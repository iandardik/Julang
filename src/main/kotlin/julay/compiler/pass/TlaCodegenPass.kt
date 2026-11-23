package julay.compiler.pass

import julay.compiler.CompileError
import julay.compiler.CompilationUnit
import julay.compiler.OneLocCompileError
import julay.compiler.FieldPathResult
import julay.compiler.FunBuiltinRegistry
import julay.compiler.OneLocCompileWarning
import julay.compiler.SourceLoc
import julay.compiler.TypeExpr
import julay.compiler.ast.*
import julay.compiler.decl.*
import julay.compiler.isDiscardBinding
import julay.compiler.resolveFieldPath
import julay.program.action.TSAction
import julay.program.type.*
import java.io.File

data class TlaCodegenResult(
    val moduleName: String,
    val tlaText: String,
    val cfgText: String,
    val warnings: List<OneLocCompileWarning> = emptyList(),
    /** Second cfg for CircSide SC2 (`PROPERTY SC2Prop`); null when not a CircSide emit. */
    val cfgSc2Text: String? = null,
    /** Apalache safety cfg (`INIT`/`NEXT`, no Fairness / Terminates). */
    val apalacheCfgText: String? = null,
)

internal object TlaEmitOpts {
    private val current = ThreadLocal.withInitial { TlaOptConfig.ALL_ON }
    fun get(): TlaOptConfig = current.get()
    fun set(config: TlaOptConfig) {
        current.set(config)
    }
}

/** Active fluent emit plan for action bodies / exprToTla fluent apps. */
internal object TlaFluentEmit {
    private val current = ThreadLocal<FluentEmitPlan?>()
    private val primeFluents = ThreadLocal.withInitial { false }
    fun get(): FluentEmitPlan? = current.get()
    fun primeFluents(): Boolean = primeFluents.get()
    fun <T> withPlan(plan: FluentEmitPlan?, block: () -> T): T {
        val prev = current.get()
        current.set(plan)
        try {
            return block()
        } finally {
            current.set(prev)
        }
    }
    fun <T> withPrimedFluents(block: () -> T): T {
        val prev = primeFluents.get()
        primeFluents.set(true)
        try {
            return block()
        } finally {
            primeFluents.set(prev)
        }
    }
}

internal object TlaSymbolTypes {
    private val current = ThreadLocal.withInitial { emptyMap<String, Type>() }
    fun get(): Map<String, Type> = current.get()
    fun set(types: Map<String, Type>) {
        current.set(types)
    }

    fun <T> withExtra(extra: Map<String, Type>, block: () -> T): T {
        if (extra.isEmpty()) return block()
        val prev = current.get()
        current.set(prev + extra)
        try {
            return block()
        } finally {
            current.set(prev)
        }
    }
}

fun compileSpecToTla(
    spec: SpecNode,
    ast: RootNode,
    unit: CompilationUnit,
    outputDir: File = File("."),
    tlaOptConfig: TlaOptConfig = TlaOptConfig.ALL_ON,
    circularSide: CircularAgPair? = null,
): TlaCodegenResult {
    val result = tlaCodegenPass(spec, ast, unit, tlaOptConfig, circularSide)
    result.warnings.forEach { System.err.println(it) }
    File(outputDir, "${result.moduleName}.tla").writeText(result.tlaText)
    File(outputDir, "${result.moduleName}.cfg").writeText(result.cfgText)
    result.cfgSc2Text?.let { sc2 ->
        File(outputDir, "${result.moduleName}_SC2.cfg").writeText(sc2)
    }
    result.apalacheCfgText?.let { apa ->
        File(outputDir, "${result.moduleName}.apalache.cfg").writeText(apa)
    }
    return result
}

fun tlaCodegenPass(
    spec: SpecNode,
    ast: RootNode,
    unit: CompilationUnit,
    tlaOptConfig: TlaOptConfig = TlaOptConfig.ALL_ON,
    circularSide: CircularAgPair? = null,
): TlaCodegenResult {
    val moduleName = spec.specNodeName()
    handlerResponseActions = unit.modules.flatMap { m ->
        m.root.handlerRefs().map { it.handlerClass() to it.handlerAction() }
    }.toSet()
    val leafSpecNodes = unit.modules
        .flatMap { it.root.declNodes().filterIsInstance<LeafSpecNode>() }
        .associateBy { it.name() }
    val pclassNodes = unit.modules
        .flatMap { it.root.declNodes().filterIsInstance<ProcClassNode>() }
        .associateBy { it.name() } + leafSpecNodes.mapValues { it.value.asProcClass() }
    val procAliases = unit.modules
        .flatMap { it.root.declNodes().filterIsInstance<ProcNode>() }
        .associateBy { it.name() }
    val specAliases = unit.modules
        .flatMap { it.root.declNodes().filterIsInstance<SpecNode>() }
        .associateBy { it.name() }

    val invariantsEarly = unit.modules
        .flatMap { it.root.declNodes().filterIsInstance<InvariantNode>() }
        .associateBy { it.name() }
    val allFluents = unit.modules
        .flatMap { it.root.declNodes().filterIsInstance<FluentNode>() }
        .associateBy { it.name() }
    val systemDeclNames = (
        pclassNodes.keys + leafSpecNodes.keys + procAliases.keys + specAliases.keys
        ).toSet()
    val compositionLeaves = compositionLeavesOfSpec(
        spec,
        invariantNames = invariantsEarly.keys,
        systemDeclNames = systemDeclNames,
    )
    val procDecls = unit.modules.flatMap { it.root.resolvedProcPass(unit) }
    val program = procDecls.firstOrNull { it.name == moduleName }
        ?: ProcDecl(moduleName, compositionLeaves.map { ProcDecl(it.name, emptyList(), ProcDeclType.Proc) }, ProcDeclType.Spec)
    val leafMap = leafActionMap(ast, program.allProcNames(procDecls).toSet(), emptySet())
    val alphabet = computeCompositionAlphabet(program, procDecls, leafMap)
    val leaves = expandLeavesToPclasses(
        compositionLeaves,
        pclassNodes,
        procAliases,
        specAliases,
        leafSpecNodes,
        alphabet = alphabet,
    )

    val recordNames = unit.modules.flatMap { m ->
        m.root.declNodes().filterIsInstance<ObjClassNode>().map { it.name() }
    }.toSet()
    val opaqueSorts = collectOpaqueIndexSorts(leaves, ast.cachedObjClassRegistry()
        ?: error("ObjClassRegistry required for TLA codegen"))
    TlaOpaqueIndexSort.install(opaqueSorts)
    var mergedDomains = ast.cachedObjClassRegistry()?.domains?.toMutableMap() ?: mutableMapOf()
    val recordIndexModels = linkedMapOf<String, String>()
    val domainModelErrors = mutableListOf<CompileError>()
    leaves.forEach { leaf ->
        val models = leaf.typeModels + (leafSpecNodes[leaf.name]?.typeModels() ?: emptyList())
        val mergedResult = mergeSpecTypeModels(models, mergedDomains, recordNames)
        mergedDomains = mergedResult.domains.toMutableMap()
        recordIndexModels.putAll(mergedResult.recordIndexModels)
        domainModelErrors += mergedResult.errors
    }
    // Record delayed models are only legal for create-index sorts.
    recordIndexModels.keys.forEach { name ->
        if (name !in opaqueSorts) {
            domainModelErrors += OneLocCompileError(
                SourceLoc(1 to 1, unit.entryPath),
                "delayed model for record type \"$name\" is only allowed when \"$name\" is used as a create-index sort",
            )
        }
    }
    opaqueSorts.keys.forEach { name ->
        if (name !in recordIndexModels) {
            domainModelErrors += OneLocCompileError(
                SourceLoc(1 to 1, unit.entryPath),
                "record index sort \"$name\" requires a delayed model for this spec compile",
            )
        }
    }
    if (domainModelErrors.isNotEmpty()) {
        TlaOpaqueIndexSort.clear()
        val msg = domainModelErrors.joinToString("\n") {
            (it as? OneLocCompileError)?.msg ?: it.toString()
        }
        throw RuntimeException(msg)
    }
    val domains = mergedDomains.toMap()
    val domainModels = domains.mapNotNull { (name, d) ->
        d.cfgElements?.let { name to "{${it.joinToString(", ")}}" }
    }.toMap().toMutableMap()
    recordIndexModels.forEach { (name, cfg) ->
        domainModels[name] = cfg
    }
    // Peeled DynProc models → CONSTANT + cfg (not a DomainType). Implicit default
    // (`dp1`..`dp3`, clash-renamed) is filled in after cfg string literals are known.
    leaves.mapNotNull { it.dynProcModel }.distinctBy { it.name() }.forEach { model ->
        val elems = model.elements.map { lit ->
            when (lit.inferType(emptyMap())) {
                is StringType -> "\"${escapeTlaStringLiteral(lit.literalText())}\""
                is BoolType -> lit.literalText().uppercase()
                else -> lit.literalText()
            }
        }
        domainModels[DYN_PROC_MODEL] = "{${elems.joinToString(", ")}}"
    }
    val needsDefaultDynProc =
        leaves.any { it.isDynamic } && DYN_PROC_MODEL !in domainModels
    val domainNames = domains.keys

    val pclassesForTla = pclassNodes

    val constants = linkedSetOf<String>()
    leaves.forEach { leaf ->
        if (leaf.paramType != null) {
            typeDomainConstant(leaf.paramType!!)?.let { name ->
                // Int/Nat/Boolean/Real are provided by EXTENDS; only declare other domains (e.g. String).
                if (name !in setOf("Int", "Nat", "Boolean", "Real")) {
                    constants += name
                }
            }
        }
        if (leaf.instParamType != null) {
            typeDomainConstant(leaf.instParamType!!)?.let { name ->
                if (name !in setOf("Int", "Nat", "Boolean", "Real")) {
                    constants += name
                }
            }
        }
    }
    if (DYN_PROC_MODEL in domainModels || needsDefaultDynProc) {
        constants += DYN_PROC_MODEL
    }

    val invariants = invariantsEarly
    val ag = when (val v = spec.specNodeValue()) {
        is AgSpecExprNode -> v
        is SpecCompositionBlockNode -> v.systemBody() as? AgSpecExprNode
        else -> null
    }
    val registry = ast.cachedObjClassRegistry()
        ?: error("ObjClassRegistry required for TLA codegen")
    val circSideInvNames = circularSide?.let { p ->
        listOfNotNull(p.pName, p.aName, p.gName, p.safeName)
    }.orEmpty()
    val fluentPlan = buildFluentEmitPlan(
        spec, invariants, allFluents, systemDeclNames, registry,
        extraInvariantNames = circSideInvNames,
    )
    val invRoots = buildList {
        ag?.let { addAll(resolveGuaranteeInvariants(it, spec.specNodeName(), invariants)) }
        // CircSide needs A/G (and P/Safe) operator defs for SC1/SC2, not as cfg guarantees.
        circSideInvNames.forEach { n -> invariants[n]?.let { add(it) } }
    }.distinctBy { it.name() }
    val invClosure = buildList {
        val seen = mutableSetOf<String>()
        invRoots.forEach { invNode ->
            val invMap = if (invariants.containsKey(invNode.name())) {
                invariants
            } else {
                invariants + (invNode.name() to invNode)
            }
            topologicalInvariantClosure(invNode.name(), invMap).forEach { n ->
                if (seen.add(n.name())) add(n)
            }
        }
    }
    // Formula-assume invariant ops are emitted for NameOK but not listed in cfg INVARIANT.
    val assumeInvClosure = buildList {
        val seen = invClosure.map { it.name() }.toMutableSet()
        fluentPlan.formulaAssumeNames.forEach { an ->
            if (an in invariants && seen.add(an)) {
                addAll(topologicalInvariantClosure(an, invariants))
            }
        }
    }
    invClosure.forEach { collectTypeConstants(it.invariantFormula(), constants) }
    assumeInvClosure.forEach { collectTypeConstants(it.invariantFormula(), constants) }
    val sortNames = domainNames
    leaves.forEach { leaf ->
        leaf.initExprs.forEach { expr ->
            collectTypeConstants(expr, constants)
            collectSortLengthConstants(expr, sortNames, constants)
        }
    }

    val cfgOverrides = linkedSetOf<String>()

    val offers = collectTlaActionOffers(leaves, pclassesForTla)
    val usedFunOpsAll = collectUserFunsUsedInOffers(
        offers,
        extraExprs = invClosure.map { it.invariantFormula() },
    )
    val (funlibFuns, userFuns) = usedFunOpsAll.partition { isJulayFunlibFun(it) }
    val allFunModels = leaves.flatMap { it.funModels } +
        leafSpecNodes.values.flatMap { it.funModels() }
    val (funOverrideByName, funModelMergeErrors) = mergeFunModels(allFunModels)
    if (funModelMergeErrors.isNotEmpty()) {
        val msg = funModelMergeErrors.joinToString("\n") {
            (it as? OneLocCompileError)?.msg ?: it.toString()
        }
        throw RuntimeException(msg)
    }
    val rewrittenUserFuns = userFuns.map { fn ->
        funOverrideByName[fn.name()]?.let { fn.withBody(it.overrideExpr) } ?: fn
    }
    val usedFunlibOps = orderFunsForTlaEmit(funlibFuns.toSet())
    val usedUserFunOps = orderFunsForTlaEmit(rewrittenUserFuns.toSet())
    val usedFunOps = usedFunlibOps + usedUserFunOps
    val relevantFields = when {
        tlaOptConfig.unusedFields ->
            analyzeTlaRelevantFields(
                pclassesForTla, offers, usedFunOps, invClosure,
                initExprs = leaves.flatMap { leaf -> leaf.initExprs.map { leaf.name to it } },
            ).withUnwrap(tlaOptConfig.unwrapSingletons)
        tlaOptConfig.unwrapSingletons -> TlaRelevantFields.UNWRAP_ONLY
        else -> TlaRelevantFields.IDENTITY
    }
    val unusedFieldWarnings =
        if (tlaOptConfig.unusedFields) relevantFields.comparisonWarnings() else emptyList()
    val relevantVars = if (tlaOptConfig.unusedVars) {
        analyzeTlaRelevantVars(
            pclassesForTla,
            offers,
            usedFunOps,
            invClosure,
            initExprs = leaves.flatMap { leaf -> leaf.initExprs.map { leaf.name to it } },
        )
    } else {
        TlaRelevantVars.IDENTITY
    }
    TlaVarProjection.set(relevantVars)
    val fieldVars = if (tlaOptConfig.fieldVars) {
        analyzeTlaFieldVars(
            pclassesForTla,
            offers,
            usedFunOps,
            invClosure,
            relevantVars,
            initExprs = leaves.flatMap { leaf -> leaf.initExprs.map { leaf.name to it } },
        )
    } else {
        TlaFieldVars.IDENTITY
    }
    TlaFieldVarProjection.set(fieldVars)
    val literalDomains = if (tlaOptConfig.literalDomains) {
        analyzeTlaLiteralDomains(leaves, pclassesForTla, offers, usedFunOps, invClosure)
    } else {
        TlaLiteralDomains.NONE
    }

    TlaFieldProjection.set(relevantFields)
    TlaLiteralDomainProjection.set(literalDomains)
    TlaEmitOpts.set(tlaOptConfig)
    val symbolTypes = mutableMapOf<String, Type>()
    domains.forEach { (name, domain) ->
        symbolTypes[name] = domain
    }
    pclassesForTla.forEach { (leafName, pc) ->
        pc.localDecls().filterIsInstance<VarNode>().forEach { vn ->
            try {
                symbolTypes[vn.name] = TlaFieldVarProjection.get().effectiveType(leafName, vn.name, vn.type)
            } catch (_: RuntimeException) {
            }
        }
    }
    TlaSymbolTypes.set(symbolTypes)
    try {
        return emitProjectedTla(
            moduleName = moduleName,
            leaves = leaves,
            pclassesForTla = pclassesForTla,
            leafSpecNodes = leafSpecNodes,
            constants = constants,
            cfgOverrides = cfgOverrides,
            invClosure = invClosure,
            assumeInvClosure = assumeInvClosure,
            fluentPlan = fluentPlan,
            domainModels = domainModels,
            offers = offers,
            usedFunlibOps = usedFunlibOps,
            usedUserFunOps = usedUserFunOps,
            usedFunOps = usedFunOps,
            unusedFieldWarnings = unusedFieldWarnings,
            domains = domains,
            circularSide = circularSide,
            // Only explicit `<A> Sys <G>` (and circular / LocalInv |= <…>) omit cfg liveness.
            // Trailing `Sys |= G` and plain systems keep Spec + PROPERTY *Terminates.
            omitCfgLiveness = ag?.isExplicitAngleAg() == true,
        )
    } finally {
        TlaFieldProjection.set(TlaRelevantFields.IDENTITY)
        TlaLiteralDomainProjection.set(TlaLiteralDomains.NONE)
        TlaEmitOpts.set(TlaOptConfig.ALL_ON)
        TlaSymbolTypes.set(emptyMap())
        TlaVarProjection.set(TlaRelevantVars.IDENTITY)
        TlaFieldVarProjection.set(TlaFieldVars.IDENTITY)
        TlaOpaqueIndexSort.clear()
    }
}

private fun emitProjectedTla(
    moduleName: String,
    leaves: List<SpecLeaf>,
    pclassesForTla: Map<String, ProcClassNode>,
    leafSpecNodes: Map<String, LeafSpecNode>,
    constants: LinkedHashSet<String>,
    cfgOverrides: LinkedHashSet<String>,
    invClosure: List<InvariantNode>,
    assumeInvClosure: List<InvariantNode>,
    fluentPlan: FluentEmitPlan,
    domainModels: Map<String, String>,
    offers: List<TlaActionOffer>,
    usedFunlibOps: List<FunNode>,
    usedUserFunOps: List<FunNode>,
    usedFunOps: List<FunNode>,
    unusedFieldWarnings: List<OneLocCompileWarning>,
    domains: Map<String, DomainType>,
    circularSide: CircularAgPair? = null,
    omitCfgLiveness: Boolean = false,
): TlaCodegenResult {
    val registry = fluentPlan.registry
    val cfgDomainModels = domainModels.toMutableMap()
    val emittedOffers = collectEmittedOfferLists(offers, pclassesForTla)
        .filter { offerGroupHasEmittedUpdate(it) }
        .flatten()
    invClosure.forEach { collectBuiltinDomainUses(it.invariantFormula(), cfgOverrides) }
    leaves.forEach { leaf ->
        leaf.initExprs.forEach { collectBuiltinDomainUses(it, cfgOverrides) }
    }
    leaves.forEach { leaf ->
        leaf.paramType?.let { typeDomainConstant(it) }?.let { name ->
            if (name in setOf("Int", "Nat", "Real")) cfgOverrides += name
        }
    }
    collectProjectedSortConstants(leaves, pclassesForTla, leafSpecNodes, emittedOffers, constants)
    collectActionArgDomainModels(emittedOffers, pclassesForTla, leafSpecNodes, cfgOverrides)
    collectIoHavocDomainModels(emittedOffers, cfgOverrides)
    collectTypeOkDomainModels(leaves, pclassesForTla, cfgOverrides)
    collectConstGlobalInitDomainModels(leaves, pclassesForTla, cfgOverrides)
    if ("String" in cfgOverrides) {
        constants += "String"
    }
    if ("MaxListLen" in cfgOverrides) {
        constants += "MaxListLen"
    }

    val intModelValues = linkedSetOf<Int>()
    val stringModelValues = linkedSetOf<String>()
    emittedOffers.forEach { collectCfgLiteralsFromOffer(it, intModelValues, stringModelValues) }
    invClosure.forEach { collectCfgLiteralsFromExpr(it.invariantFormula(), intModelValues, stringModelValues) }
    leaves.forEach { leaf ->
        leaf.initExprs.forEach { collectCfgLiteralsFromExpr(it, intModelValues, stringModelValues) }
    }
    usedFunOps.forEach { collectCfgLiteralsFromExpr(it.funBody(), intModelValues, stringModelValues) }
    // Delayed models bound TypeOK / `\E`; carrier literals written to those sites (e.g.
    // `value := ""` for typedef Value) must be in the CONSTANT or TypeOK is false after the write.
    widenDomainModelsFromAssignedLiterals(
        domains, cfgDomainModels, leaves, pclassesForTla, emittedOffers, stringModelValues,
    )
    // Typedef/uninterpreted domains over String (Key, NodeSet, …) have their own CONSTANT
    // models; TypeOK often types the same slots as String, so domain elements must ⊆ String.
    addStringCarrierDomainElements(domains, stringModelValues)
    addDynProcStringElements(leaves, stringModelValues)
    if (leaves.any { it.isDynamic } && DYN_PROC_MODEL !in cfgDomainModels) {
        val slots = defaultDynProcSlotNames(stringModelValues)
        cfgDomainModels[DYN_PROC_MODEL] =
            "{${slots.joinToString(", ") { "\"${escapeTlaStringLiteral(it)}\"" }}}"
        stringModelValues.addAll(slots)
    }
    val coerceIntToString =
        emittedOffers.any { offerContainsIntToStringCoerce(it) } ||
            invClosure.any { exprContainsIntToStringCoerce(it.invariantFormula()) } ||
            usedFunOps.any { exprContainsIntToStringCoerce(it.funBody()) }

    val reservedTlaIds = constants + setOf("Int", "Nat", "Boolean", "Real")
    val needsSpliceOperator =
        offersUseSlice(offers) ||
            usedFunOps.any { exprContainsSlice(it.funBody()) } ||
            invClosure.any { exprContainsSlice(it.invariantFormula()) }
    val needsStartsWithOperator =
        offersUseStartsWith(offers) ||
            usedFunOps.any { exprContainsStartsWith(it.funBody()) } ||
            invClosure.any { exprContainsStartsWith(it.invariantFormula()) }
    val typeOkShapes = analyzeTypeOkShapes(leaves, pclassesForTla, offers)
    var needsRangeOperator =
        offersUseListMembership(offers) ||
            offersUseToSet(offers) ||
            offersUseAllDistinct(offers) ||
            usedFunOps.any {
                exprContainsListMembership(it.funBody()) ||
                    exprContainsToSet(it.funBody()) ||
                    exprContainsAllDistinct(it.funBody())
            } ||
            invClosure.any {
                exprContainsListMembership(it.invariantFormula()) ||
                    exprContainsToSet(it.invariantFormula()) ||
                    exprContainsAllDistinct(it.invariantFormula())
            } ||
            typeOkShapes.usesRange ||
            // Fluent bulk updates on list `entries` may still use Range elsewhere.
            fluentPlan.fluents.any { fl ->
                fl.fluentOnClauses().any { "entries" in it.argBinders }
            }
    val needsSetToSeqOperator =
        offersUseToList(offers) ||
            usedFunOps.any { exprContainsToList(it.funBody()) } ||
            invClosure.any { exprContainsToList(it.invariantFormula()) }
    val needsAllDistinctOperator =
        offersUseAllDistinct(offers) ||
            usedFunOps.any { exprContainsAllDistinct(it.funBody()) } ||
            invClosure.any { exprContainsAllDistinct(it.invariantFormula()) }
    val unsupportedBuiltinWarnings =
        collectUnsupportedBuiltinWarnings(offers, usedFunOps, invClosure)
    val stateVarNames = applyFieldVarNames(
        buildStateVarNames(leaves, pclassesForTla, reservedTlaIds),
        leaves,
        reservedTlaIds,
    )
    val foldedCtors = foldableInitConstructors(offers, pclassesForTla)
    val foldedCtorLeaves = foldedCtors.keys

    // cfg Int/Nat may be finite \E bounds; TypeOK uses TypeOKInt (Int \cup Nat \cup extras).
    val typeOkIntRange = "TypeOKInt"
    // Opaque sentinel pins `*_id[sent] = -1`; ensure TypeOKInt covers it.
    if (TlaOpaqueIndexSort.sorts().values.any { ty -> ty.fields.any { it.type is IntType } }) {
        intModelValues += -1
    }
    val typeOkIntDef = emitTypeOkIntDef(intModelValues)

    val intConstraintRange = constraintIntRange(intModelValues)
    val opaqueAtoms: Map<String, List<String>> = buildMap {
        TlaOpaqueIndexSort.sorts().keys.forEach { name ->
            val cfg = domainModels[name] ?: return@forEach
            val atoms = parseTlaCfgSet(cfg) ?: return@forEach
            put(name, atoms)
        }
    }
    // Empty/sentinel opaque literals use an atom outside the live create-index CONSTANT.
    TlaOpaqueIndexSort.installSentinels(opaqueAtoms)
    TlaOpaqueIndexSort.sorts().keys.forEach { sort ->
        TlaOpaqueIndexSort.sentinelRaw(sort)?.let { stringModelValues += it }
    }
    val singletonConstGlobals = linkedMapOf<Pair<String, String>, String>()
    val consumedInitExprs = mutableSetOf<ExprNode>()
    // Projection Init pins from `init:` (url identity / cluster[i].id = i); default is `\in`.
    val opaqueProjectionPins = linkedMapOf<String, String>()
    leaves.forEach { leaf ->
        matchOpaqueUrlIdentityInits(leaf.initExprs).forEach { (pname, expr) ->
            val sort = TlaOpaqueIndexSort.sorts().keys.firstOrNull { s ->
                pname == TlaOpaqueIndexSort.projectionName(s, "url")
            } ?: return@forEach
            opaqueProjectionPins.putIfAbsent(pname, "[n \\in $sort |-> n]")
            consumedInitExprs += expr
        }
        val pc = pclassesForTla[leaf.name] ?: return@forEach
        pc.localDecls().filterIsInstance<VarNode>().forEach { vn ->
            if (vn.name !in leaf.globalConstVars) return@forEach
            val hit = matchSingletonConstGlobalInit(
                vn.name, safeType(vn), leaf.initExprs, domains, opaqueAtoms,
            ) ?: return@forEach
            singletonConstGlobals[leaf.tlaName to vn.name] = hit.value
            consumedInitExprs += hit.consumed
            hit.opaqueIdProjection?.let { (pname, value) ->
                opaqueProjectionPins.putIfAbsent(pname, value)
            }
        }
    }
    // Url-identity pin puts sort atoms in a String-typed projection; cfg String must cover them.
    if (opaqueProjectionPins.keys.any { it.endsWith("_url") }) {
        addOpaqueIndexSortStringAtoms(opaqueAtoms, stringModelValues)
    }
    dropConsumedOnlyTypeConstants(leaves, invClosure + assumeInvClosure, consumedInitExprs, constants)
    val variables = mutableListOf<String>()
    val stateVarApalacheTypes = linkedMapOf<String, String>()
    val initParts = mutableListOf<String>()
    val typeOkParts = mutableListOf<String>()
    val constraintParts = mutableListOf<String>()
    // Opaque index-sort field projections: VARIABLES Node_url / Node_id.
    // Domain is live sort ∪ {sentinel}; create-index still uses bare CONSTANT [sort].
    // Default Init is membership; `init:` may pin url identity or id from cluster.
    TlaOpaqueIndexSort.sorts().forEach { (sort, ty) ->
        ty.fields.forEach { field ->
            val pname = TlaOpaqueIndexSort.projectionName(sort, field.name)
            if (pname in variables || pname in constants) return@forEach
            variables += pname
            val valueDom = TlaOpaqueIndexSort.valueDomain(sort)
            val sent = TlaOpaqueIndexSort.sentinelTla(sort)
            val sentField = TlaOpaqueIndexSort.sentinelFieldTla(sort, field)
            // Init is emitted before TypeOKInt; use an inline Int∪{-1} there. TypeOK can name TypeOKInt.
            val initRange = when (field.type) {
                is IntType -> "(Int \\cup {-1})"
                else -> typeToTlaDomain(field.type)
            }
            val typeOkRange = when (field.type) {
                is IntType -> typeOkIntRange
                else -> typeToTlaDomain(field.type)
            }
            // Projection String ranges need a TLC CONSTANT; Int/Nat/Boolean come from EXTENDS.
            if (typeOkRange == "String") {
                constants += "String"
                cfgOverrides += "String"
            }
            val apaRange = julayTypeToApalache(field.type)
            stateVarApalacheTypes[pname] = "$valueDom -> $apaRange"
            val initConjunct = when {
                opaqueProjectionPins[pname] != null -> {
                    val pin = opaqueProjectionPins.getValue(pname)
                    "/\\ $pname = ($sent :> $sentField) @@ $pin"
                }
                else ->
                    "/\\ $pname \\in [$valueDom -> $initRange] /\\ $pname[$sent] = $sentField"
            }
            initParts += initConjunct
            typeOkParts += "/\\ $pname \\in [$valueDom -> $typeOkRange]"
        }
    }
    emitFluentInit(fluentPlan, variables, initParts)
    fluentPlan.fluents.forEach { fl ->
        val params = fluentParamTypes(fl, registry)
        if (params.isEmpty()) {
            typeOkParts += "/\\ ${fl.name()} \\in BOOLEAN"
        } else {
            val product = params.joinToString(" \\X ") { (_, ty) -> typeToTlaDomain(ty) }
            typeOkParts += "/\\ ${fl.name()} \\in [$product -> BOOLEAN]"
        }
    }
    leaves.forEach { leaf ->
        val pc = pclassesForTla[leaf.name] ?: return@forEach
        val foldCtor = foldedCtors[leaf.tlaName]
        initParts += if (foldCtor != null) {
            "\\* State variables for ${leaf.name} with ${foldCtor.decl.action.name} constructor logic"
        } else {
            "\\* State variables for ${leaf.name}"
        }
        unusedVarsOmittedComment(leaf, pc)?.let { initParts += it }
        TlaFieldVarProjection.get().projectionsForLeaf(leaf.name).forEach { proj ->
            initParts += proj.initComment()
        }
        val constructed = stateTlaName(leaf.tlaName, "constructed", stateVarNames)
        val retired = stateTlaName(leaf.tlaName, "retired", stateVarNames)
        val parentDomain = parentLinkDomain(leaf, leaves)
        val parentVar = if (parentDomain != null) stateTlaName(leaf.tlaName, "parent", stateVarNames) else null
        val paramDomain = leafParamDomain(leaf)
        val stateVarsByLeaf = pc.localDecls().filterIsInstance<VarNode>().map { it.name }.toSet()
        if (leaf.isParameterized) {
            val domain = paramDomain!!
            val instDomain = leafInstDomain(leaf)
            val bareStateVars = stateVarsByLeaf
            val binder = indexBinderName(leaf, bareStateVars)
            val instBinder = if (leaf.isDualIndexed) {
                val base = leaf.instParamName!!
                if (base !in bareStateVars && base != binder) base else "${base}_"
            } else {
                null
            }
            fun initMap(falseOrDefault: String): String =
                if (instBinder != null && instDomain != null) {
                    "[$binder \\in $domain |-> [$instBinder \\in $instDomain |-> $falseOrDefault]]"
                } else {
                    "[$binder \\in $domain |-> $falseOrDefault]"
                }
            if (foldCtor == null) {
                variables += constructed
                stateVarApalacheTypes[constructed] = indexedApalacheType("Bool", domain, instDomain)
                initParts += "/\\ $constructed = ${initMap("FALSE")}"
                typeOkParts += typeOkConjunct(constructed, "BOOLEAN", domain, instDomain)
                variables += retired
                stateVarApalacheTypes[retired] = indexedApalacheType("Bool", domain, instDomain)
                initParts += "/\\ $retired = ${initMap("FALSE")}"
                typeOkParts += typeOkConjunct(retired, "BOOLEAN", domain, instDomain)
            }
            if (parentVar != null) {
                variables += parentVar
                stateVarApalacheTypes[parentVar] = indexedApalacheType("Str", domain, instDomain)
                initParts += "/\\ $parentVar = ${initMap("CHOOSE p \\in $parentDomain : TRUE")}"
                typeOkParts += typeOkConjunct(parentVar, parentDomain!!, domain, instDomain)
            }
            pc.localDecls().filterIsInstance<VarNode>().forEach { vn ->
                if (!TlaVarProjection.get().isRelevant(leaf.name, vn.name)) return@forEach
                val v = stateTlaName(leaf.tlaName, vn.name, stateVarNames)
                variables += v
                val baseApa = julayTypeToApalache(effectiveVarType(leaf, vn))
                stateVarApalacheTypes[v] = if (leaf.indexesState(vn.name)) {
                    indexedApalacheType(baseApa, domain, instDomain)
                } else if (leaf.isParameterized) {
                    indexedApalacheType(baseApa, domain, null)
                } else {
                    baseApa
                }
                val folded = foldCtor?.let {
                    foldedCtorVarInit(
                        leaf, vn, it, binder, domain, stateVarNames, constants, cfgOverrides, stateVarsByLeaf,
                        leaves,
                    )
                }
                if (folded != null) {
                    initParts += folded
                } else if (leaf.indexesState(vn.name)) {
                    // Prefer a one-line literal init when present (multi-line records break
                    // Init `/\\` indentation). Open/spawn F_call re-applies inline inits.
                    val def = when (val init = vn.initExpr) {
                        is LiteralValueExprNode -> literalToTla(init)
                        else -> defaultTlaValue(effectiveVarType(leaf, vn), leafClass = leaf.name, varName = vn.name)
                    }
                    initParts += "/\\ $v = ${initMap(def)}"
                } else if (vn.name in leaf.globalConstVars) {
                    val singleton = singletonConstGlobals[leaf.tlaName to vn.name]
                    if (singleton != null) {
                        initParts += "/\\ $v = $singleton"
                    } else {
                        initParts += "/\\ $v \\in ${typeToTlaDomain(effectiveVarType(leaf, vn), leafClass = leaf.name, varName = vn.name)}"
                    }
                } else {
                    initParts += "/\\ $v = ${defaultTlaValue(effectiveVarType(leaf, vn), leafClass = leaf.name, varName = vn.name)}"
                }
                typeOkParts += typeOkForStateVar(
                    leaf, vn, v, typeOkIntRange, typeOkShapes, stateVarNames,
                    binder, domain, instDomain,
                )
                typeOkParts += typeOkShapeConjuncts(
                    leaf, vn.name, v, leaf.indexesState(vn.name), binder, domain,
                    typeOkShapes, stateVarNames,
                )
                appendStateConstraint(
                    constraintParts, v, effectiveVarType(leaf, vn),
                    indexed = leaf.indexesState(vn.name),
                    binder = binder, domain = domain,
                    instBinder = instBinder, instDomain = instDomain,
                    intRange = intConstraintRange,
                )
            }
            if (foldCtor != null) {
                emitFoldedCtorAssumptions(
                    leaf, foldCtor, binder, domain, stateVarNames, constants, cfgOverrides,
                    stateVarsByLeaf, leaves, initParts,
                )
            }
        } else {
            if (foldCtor == null) {
                variables += constructed
                stateVarApalacheTypes[constructed] = "Bool"
                initParts += "/\\ $constructed = FALSE"
                typeOkParts += typeOkConjunct(constructed, "BOOLEAN", null)
                variables += retired
                stateVarApalacheTypes[retired] = "Bool"
                initParts += "/\\ $retired = FALSE"
                typeOkParts += typeOkConjunct(retired, "BOOLEAN", null)
            }
            pc.localDecls().filterIsInstance<VarNode>().forEach { vn ->
                if (!TlaVarProjection.get().isRelevant(leaf.name, vn.name)) return@forEach
                val v = stateTlaName(leaf.tlaName, vn.name, stateVarNames)
                variables += v
                stateVarApalacheTypes[v] = julayTypeToApalache(effectiveVarType(leaf, vn))
                val folded = foldCtor?.let {
                    foldedCtorVarInit(
                        leaf, vn, it, null, null, stateVarNames, constants, cfgOverrides, stateVarsByLeaf,
                        leaves,
                    )
                }
                if (folded != null) {
                    initParts += folded
                } else if (vn.name in leaf.globalConstVars) {
                    val singleton = singletonConstGlobals[leaf.tlaName to vn.name]
                    if (singleton != null) {
                        initParts += "/\\ $v = $singleton"
                    } else {
                        initParts += "/\\ $v \\in ${typeToTlaDomain(effectiveVarType(leaf, vn), leafClass = leaf.name, varName = vn.name)}"
                    }
                } else {
                    initParts += "/\\ $v = ${defaultTlaValue(effectiveVarType(leaf, vn), leafClass = leaf.name, varName = vn.name)}"
                }
                typeOkParts += typeOkForStateVar(
                    leaf, vn, v, typeOkIntRange, typeOkShapes, stateVarNames,
                    binder = null, domain = null, instDomain = null,
                )
                typeOkParts += typeOkShapeConjuncts(
                    leaf, vn.name, v, indexed = false, binder = null, domain = null,
                    typeOkShapes, stateVarNames,
                )
                appendStateConstraint(
                    constraintParts, v, effectiveVarType(leaf, vn),
                    indexed = false, binder = null, domain = null, intRange = intConstraintRange,
                )
            }
            if (foldCtor != null) {
                emitFoldedCtorAssumptions(
                    leaf, foldCtor, null, null, stateVarNames, constants, cfgOverrides,
                    stateVarsByLeaf, leaves, initParts,
                )
            }
        }
    }
    if (constraintParts.any { "\\in Int" in it } ||
        typeOkParts.any { "SUBSET Int" in it || "[Int ->" in it }
    ) {
        cfgOverrides += "Int"
    }
    val built = TlaFluentEmit.withPlan(fluentPlan) {
        buildTlaActions(
            leaves, pclassesForTla, offers, stateVarNames,
            cfgOverrides, leafSpecNodes, foldedCtorLeaves,
            fluentVarNames = fluentPlan.fluents.map { it.name() },
        )
    }
    val helpers = built.helpers
    val actions = built.actions

    // Fun / helper operator params must not collide with VARIABLES / CONSTANTS / other module ops.
    val funReservedNames = (
        constants + variables + usedFunOps.map { it.name() } +
            actions.map { it.name } +
            setOf(
                "vars", "BoundedSeq", "Range", "SetToSeq", "splice", "startsWith", "allDistinct",
                "Init", "Next", "Spec", "SpecSafety", "TypeOK", "TypeOKInt", "StateConstraint", "GF", "dummy",
                "SC1", "SC2", "SpecSC1", "SC2Prop",
            )
        ).toSet()
    val spliceOperatorDef = if (needsSpliceOperator) emitSpliceOperatorDef(funReservedNames) else null
    val startsWithOperatorDef =
        if (needsStartsWithOperator) emitStartsWithOperatorDef(funReservedNames) else null
    val setToSeqOperatorDef = if (needsSetToSeqOperator) emitSetToSeqOperatorDef(funReservedNames) else null
    val allDistinctOperatorDef =
        if (needsAllDistinctOperator) emitAllDistinctOperatorDef(funReservedNames) else null
    val funlibOperatorDefs = usedFunlibOps.map { emitFunOperatorDef(it, funReservedNames) }
    // Callers of funs using unsupported builtins already degrade to TRUE; no operator to emit.
    val userFunOperatorDefs = usedUserFunOps
        .filter { !exprContainsUnsupportedTlaBuiltin(it.funBody()) }
        .map { emitFunOperatorDef(it, funReservedNames) }
    val hasJulayLibFuns =
        needsSpliceOperator || needsStartsWithOperator || needsAllDistinctOperator ||
            funlibOperatorDefs.isNotEmpty()
    val needsBoundedSeq = "MaxListLen" in constants || "MaxListLen" in cfgOverrides
    if (constraintParts.any { "MaxListLen" in it }) {
        constants += "MaxListLen"
        cfgOverrides += "MaxListLen"
    }

    val invDefs = TlaFluentEmit.withPlan(fluentPlan) {
        buildList {
            val emitted = mutableSetOf<String>()
            (assumeInvClosure + invClosure).forEach { node ->
                if (!emitted.add(node.name())) return@forEach
                addAll(emitInvariantDefs(node, leaves, stateVarNames, constants))
            }
        }
    }
    // Formula assume: SANY rejects Op' for defined Op; emit OpOK over primed fluents
    // before actions so action bodies can conjoin `/\\ OpOK`.
    val assumeOkDefs = TlaFluentEmit.withPlan(fluentPlan) {
        val assumeNodesByName = (assumeInvClosure + invClosure).associateBy { it.name() }
        fluentPlan.formulaAssumeNames.flatMap { an ->
            val node = assumeNodesByName[an] ?: return@flatMap emptyList()
            TlaFluentEmit.withPrimedFluents {
                emitInvariantDefs(node, leaves, stateVarNames, constants).map { def ->
                    val eq = def.indexOf(" ==")
                    if (eq < 0) def else "${an}OK${def.substring(eq)}"
                }
            }
        }
    }
    val varApalacheTypes = LinkedHashMap<String, String>().apply {
        putAll(stateVarApalacheTypes)
        // TypeOK `\in` shapes override when present (fluents, message sets, …).
        putAll(parseTypeOkConjuncts(typeOkParts))
    }
    val constLine = emitApalacheAnnotatedConstants(constants, domains)
    val varsLine = emitApalacheAnnotatedVariables(
        if (variables.isEmpty()) listOf("dummy") else variables,
        if (variables.isEmpty()) mapOf("dummy" to "Int") else varApalacheTypes,
    )
    val varsTuple = if (variables.isEmpty()) "<<dummy>>" else "<<${variables.joinToString(", ")}>>"
    if (variables.isEmpty()) {
        initParts += "/\\ dummy = 0"
        typeOkParts += typeOkConjunct("dummy", typeOkIntRange, null)
    }
    emitInitConstraintParts(leaves, stateVarNames, constants, initParts, consumedInitExprs)
    // Open `\in BoundedSeq([id:Int,url:String], …)` is infinite for Apalache and can
    // blow TLC's set limit — pin concrete Init (CircSide / System Store product).
    // Prefer singleton const-global `cluster = <<…>>` from create-index `init:` when present;
    // only empty-pin indexed (non-const-global) open membership.
    val clusterSingletonInit = initParts.any { it.trimStart().startsWith("/\\ cluster = <<") }
    // Prefer opaque record index-sort name (e.g. Node); fall back to legacy NodeSet.
    val indexSortName = TlaOpaqueIndexSort.sorts().keys.firstOrNull() ?: "NodeSet"
    val opaqueIndex = TlaOpaqueIndexSort.sorts().isNotEmpty()
    val meInPin = Regex("""^/\\ me(_[A-Za-z0-9]+)? \\in """)
    for (i in initParts.indices) {
        val line = initParts[i]
        val trimmed = line.trimStart()
        when {
            "me_url \\in" in line ->
                initParts[i] = "/\\ me_url = [n \\in $indexSortName |-> n]"
            meInPin.containsMatchIn(trimmed) && "[$indexSortName ->" in trimmed -> {
                val meVar = Regex("""^/\\ (me(?:_[A-Za-z0-9]+)?) \\in """)
                    .find(trimmed)?.groupValues?.get(1) ?: "me"
                initParts[i] = when {
                    // Opaque cluster elems are sort atoms (e.g. "n1"), not records — never .url.
                    clusterSingletonInit && opaqueIndex ->
                        "/\\ $meVar = [n \\in $indexSortName |-> " +
                            "cluster[CHOOSE i \\in DOMAIN cluster : cluster[i] = n]]"
                    clusterSingletonInit ->
                        // Record NodeInfo peers: match index atom against .url.
                        "/\\ $meVar = [n \\in $indexSortName |-> " +
                            "cluster[CHOOSE i \\in DOMAIN cluster : cluster[i].url = n]]"
                    opaqueIndex ->
                        // Opaque index-sort: each index maps to itself (atom identity).
                        "/\\ $meVar = [n \\in $indexSortName |-> n]"
                    else ->
                        "/\\ $meVar = [n \\in $indexSortName |-> [id |-> 0, url |-> n]]"
                }
            }
            "cluster \\in" in line && ("NodeSet ->" in line || "$indexSortName ->" in line) ->
                initParts[i] =
                    "/\\ cluster = [n \\in $indexSortName |-> [__j \\in {} |-> [id |-> 0, url |-> \"\"]]]"
        }
    }
    // TLC evaluates Init conjuncts in order; me's CHOOSE over cluster must follow cluster = <<…>>.
    val clusterAssignIdx = initParts.indexOfFirst { it.trimStart().startsWith("/\\ cluster = <<") }
    val meUsesClusterIdx = initParts.indexOfFirst { "CHOOSE i \\in DOMAIN cluster" in it }
    if (clusterAssignIdx >= 0 && meUsesClusterIdx >= 0 && meUsesClusterIdx < clusterAssignIdx) {
        val clusterLine = initParts.removeAt(clusterAssignIdx)
        initParts.add(meUsesClusterIdx, clusterLine)
    }

    val typeOkDef = buildString {
        appendLine("TypeOK ==")
        if (typeOkParts.isEmpty()) {
            appendLine("  /\\ TRUE")
        } else {
            typeOkParts.forEach { appendLine("  $it") }
        }
    }.trimEnd()
    val constraintDef = if (constraintParts.isEmpty()) {
        null
    } else {
        buildString {
            appendLine("StateConstraint ==")
            constraintParts.forEach { appendLine("  $it") }
        }.trimEnd()
    }

    val actionsTyped = actions.map { action ->
        val apaTypes = action.params.map { (_, dom) ->
            paramDomainToApalacheType(dom, varApalacheTypes)
        }
        action.copy(paramApalacheTypes = apaTypes)
    }
    val actionDefs = actionsTyped.joinToString("\n\n") { action ->
        val ann = emitActionTypeAnnotation(action.paramApalacheTypes)
        val body = if (action.comment != null) "\\* ${action.comment}\n${action.def}" else action.def
        if (ann != null) "$ann\n$body" else body
    }
    val nextBody = if (actions.isEmpty()) {
        "FALSE"
    } else {
        "\n" + actions.joinToString("\n") { "  \\/ ${it.nextDisjunct()}" }
    }

    val tla = buildString {
        appendLine("---- MODULE $moduleName ----")
        appendLine("EXTENDS Integers, Sequences, FiniteSets, TLC")
        appendLine()
        append(constLine)
        append(varsLine)
        appendLine("vars == $varsTuple")
        appendLine()
        val hasTlaHelpers = needsBoundedSeq || needsRangeOperator || needsSetToSeqOperator
        if (hasTlaHelpers) {
            appendLine("\\* TLA+ helpers")
            if (needsBoundedSeq) {
                appendLine(emitApalacheBoundedSeqDef())
                appendLine()
            }
            if (needsRangeOperator) {
                appendLine(emitApalacheRangeDef(funReservedNames))
                appendLine()
            }
            if (needsSetToSeqOperator) {
                appendLine(setToSeqOperatorDef!!)
                appendLine()
            }
        }
        if (hasJulayLibFuns) {
            appendLine("\\* Julay lib funs")
            if (needsSpliceOperator) {
                appendLine(spliceOperatorDef!!)
                appendLine()
            }
            if (needsStartsWithOperator) {
                appendLine(startsWithOperatorDef!!)
                appendLine()
            }
            if (needsAllDistinctOperator) {
                appendLine(allDistinctOperatorDef!!)
                appendLine()
            }
            funlibOperatorDefs.forEach { def ->
                appendLine(def)
                appendLine()
            }
        }
        if (helpers.isNotEmpty()) {
            helpers.forEach { helper ->
                appendLine(helper)
                appendLine()
            }
        }
        if (userFunOperatorDefs.isNotEmpty()) {
            appendLine("\\* user defined funs")
            userFunOperatorDefs.forEach { def ->
                appendLine(def)
                appendLine()
            }
        }
        appendLine()
        appendLine("\\* system definition")
        appendLine()
        if (assumeOkDefs.isNotEmpty()) {
            appendLine("\\* formula assume (post-state fluents)")
            assumeOkDefs.forEach { def ->
                appendLine(def)
                appendLine()
            }
        }
        appendLine("Init ==")
        if (initParts.isEmpty()) {
            appendLine("  /\\ TRUE")
        } else {
            initParts.forEach { line ->
                if (line.startsWith("\\*")) {
                    appendLine("  $line")
                } else {
                    appendLine("  $line")
                }
            }
        }
        appendLine()
        if (actionDefs.isNotEmpty()) {
            appendLine(actionDefs)
            appendLine()
        }
        appendLine("Next ==$nextBody")
        appendLine()
        val fairnessDef = emitFairnessDef(actions)
        if (fairnessDef != null) {
            appendLine(fairnessDef)
            appendLine()
            appendLine("Spec == Init /\\ [][Next]_vars /\\ Fairness")
            appendLine("\\* Apalache / safety-only (no Fairness)")
            appendLine("SpecSafety == Init /\\ [][Next]_vars")
        } else {
            appendLine("Spec == Init /\\ [][Next]_vars")
            appendLine("SpecSafety == Spec")
        }
        appendLine()
        appendLine()
        appendLine("\\* Invariants")
        appendLine()
        appendLine("\\* automatically generated invariants")
        appendLine()
        appendLine(typeOkIntDef)
        appendLine()
        appendLine(typeOkDef)
        if (constraintDef != null) {
            appendLine()
            appendLine(constraintDef)
        }
        if (invDefs.isNotEmpty()) {
            appendLine()
            appendLine("\\* user-specified invariants")
            appendLine()
            invDefs.forEachIndexed { idx, def ->
                if (idx > 0) appendLine()
                appendLine(def)
            }
        }
        val terminatesDefs = leaves.filter { it.isDynamic }.map { leaf ->
            emitTerminatesProperty(leaf, stateVarNames, pclassesForTla)
        }
        if (terminatesDefs.isNotEmpty()) {
            appendLine()
            appendLine("\\* liveness: every dynamic proc slot eventually returns")
            appendLine()
            terminatesDefs.forEach { appendLine(it) }
        }
        if (circularSide != null) {
            val a = circularSide.aName
            val g = circularSide.gName
            val p = circularSide.pName
            val sc1Body = if (p != null) "$p /\\ $a /\\ $g" else "$a /\\ $g"
            appendLine()
            appendLine("\\* Circular AG side conditions (TLC-checked)")
            appendLine("\\* SC1: Init /\\ P => A /\\ G  (checked under SpecSC1 stuttering)")
            appendLine("SC1 == $sc1Body")
            appendLine("SpecSC1 == Init /\\ [][UNCHANGED vars]_vars")
            appendLine("\\* SC2: Next => A' \\/ G'")
            appendLine("SC2 == $a' \\/ $g'")
            appendLine("SC2Prop == [][Next => SC2]_vars")
        }
        appendLine()
        appendLine("====")
    }

    val terminatesPropNames = leaves.filter { it.isDynamic }.map { "${it.tlaName}Terminates" }

    val cfg = buildString {
        if (circularSide != null) {
            // SC1: Init => A /\ G under stuttering-only behavior.
            appendLine("SPECIFICATION SpecSC1")
            appendLine("INVARIANT TypeOK")
            appendLine("INVARIANT SC1")
        } else if (omitCfgLiveness) {
            // Explicit `<A> Sys <G>` (and circular / LocalInv |= <…>): safety-only default
            // cfg. Operators stay in the .tla; docs: specifications.md § AG Obl .cfg.
            // Trailing `Sys |= G` and plain systems use Spec + *Terminates below.
            appendLine("SPECIFICATION SpecSafety")
            appendLine("INVARIANT TypeOK")
        } else {
            appendLine("SPECIFICATION Spec")
            appendLine("INVARIANT TypeOK")
        }
        if (constraintDef != null) {
            appendLine("CONSTRAINT StateConstraint")
        }
        if (invDefs.isNotEmpty() && circularSide == null) {
            val skipCfgInv = cfgSkipConjunctiveInvariants(invClosure) +
                fluentPlan.formulaAssumeNames.toSet()
            val guaranteeOps = invClosure.map { it.name() }.toSet()
            invDefs.forEach { def ->
                val invOp = def.substringBefore(" ==")
                if (invOp !in guaranteeOps || invOp in skipCfgInv) return@forEach
                appendLine("INVARIANT $invOp")
            }
        }
        // Explicit angle-bracket AG Obls omit PROPERTY *Terminates; |= and plain keep them.
        if (circularSide == null && !omitCfgLiveness) {
            terminatesPropNames.forEach { name ->
                appendLine("PROPERTY $name")
            }
        }
        appendLine("CHECK_DEADLOCK FALSE")
        fun modelFor(name: String): String {
            if (name in cfgDomainModels) return cfgDomainModels.getValue(name)
            domains[name]?.let { d ->
                if (!d.hasModel && d.kind == DomainKind.Typedef) {
                    carrierTlaName(d.carrierType)?.let { return it }
                }
            }
            return when (name) {
                "Int", "Nat", "Real" ->
                    cfgIntModel(intModelValues, DEFAULT_MAX_LIST_LEN)
                "String" -> cfgStringModel(stringModelValues, intModelValues, coerceIntToString)
                else -> cfgConstantModel(name)
            }
        }
        constants.forEach { c ->
            appendLine("CONSTANT $c = ${modelFor(c)}")
        }
        // Finite models for built-in TLA+ domains (Int from Integers, etc.) — cfg only, not declared CONSTANT.
        cfgOverrides.forEach { c ->
            if (c !in constants) {
                appendLine("CONSTANT $c = ${modelFor(c)}")
            }
        }
    }

    val cfgSc2 = if (circularSide == null) {
        null
    } else {
        buildString {
            appendLine("SPECIFICATION Spec")
            appendLine("INVARIANT TypeOK")
            if (constraintDef != null) {
                appendLine("CONSTRAINT StateConstraint")
            }
            appendLine("PROPERTY SC2Prop")
            appendLine("CHECK_DEADLOCK FALSE")
            // Reuse the same CONSTANT models as the primary cfg.
            cfg.lines().filter { it.startsWith("CONSTANT ") }.forEach { appendLine(it) }
        }
    }

    // Sibling .apalache.cfg only with --opt-tla=apalache (not CircSide).
    val apalacheCfg = if (circularSide != null || !TlaEmitOpts.get().apalache) {
        null
    } else {
        buildString {
            appendLine("\\* Apalache safety model (no Fairness / liveness)")
            appendLine("INIT Init")
            appendLine("NEXT Next")
            appendLine("INVARIANT TypeOK")
            if (invDefs.isNotEmpty()) {
                val skipCfgInv = cfgSkipConjunctiveInvariants(invClosure) +
                    fluentPlan.formulaAssumeNames.toSet()
                val guaranteeOps = invClosure.map { it.name() }.toSet()
                invDefs.forEach { def ->
                    val invOp = def.substringBefore(" ==")
                    if (invOp !in guaranteeOps || invOp in skipCfgInv) return@forEach
                    appendLine("INVARIANT $invOp")
                }
            }
            appendLine("CHECK_DEADLOCK FALSE")
            cfg.lines().filter { it.startsWith("CONSTANT ") }.forEach { appendLine(it) }
        }
    }

    return TlaCodegenResult(
        moduleName,
        tla,
        cfg,
        unsupportedBuiltinWarnings + unusedFieldWarnings,
        cfgSc2Text = cfgSc2,
        apalacheCfgText = apalacheCfg,
    )
}

private fun emitTerminatesProperty(
    leaf: SpecLeaf,
    stateVarNames: Map<Pair<String, String>, String>,
    pclasses: Map<String, ProcClassNode>,
): String {
    val constructed = stateTlaName(leaf.tlaName, "constructed", stateVarNames)
    val propName = "${leaf.tlaName}Terminates"
    val pc = pclasses[leaf.name]
    val bare = pc?.localDecls()?.filterIsInstance<VarNode>()?.map { it.name }?.toSet().orEmpty()
    val binder = indexBinderName(leaf, bare)
    val domain = typeDomainConstant(leaf.paramType!!) ?: leaf.paramType.toString()
    return if (leaf.isDualIndexed) {
        val ibase = leaf.instParamName!!
        val instBinder = if (ibase !in bare && ibase != binder) ibase else "${ibase}_"
        val instDomain = typeDomainConstant(leaf.instParamType!!) ?: leaf.instParamType.toString()
        "$propName == \\A $binder \\in $domain, $instBinder \\in $instDomain : " +
            "($constructed[$binder][$instBinder] ~> ~$constructed[$binder][$instBinder])"
    } else {
        "$propName == \\A $binder \\in $domain : ($constructed[$binder] ~> ~$constructed[$binder])"
    }
}

/**
 * `Fairness == SF_vars(Next)`: if any `Next` arm is enabled infinitely often,
 * a non-stuttering step must occur infinitely often. Same meaning as
 * `SF_vars(A1 \/ A2 \/ …)` when `Next` is that disjunction; spelling the arms
 * out again only bloats the module. (Per-action `\A … : SF_vars(Ai)` is
 * stronger and blows TLC's liveness tableau.)
 */
private fun emitFairnessDef(actions: List<TlaAction>): String? {
    if (actions.isEmpty()) return null
    return "Fairness == SF_vars(Next)"
}

/** (class, action) of `rcv` in every `serveHttp(port, A.snd ~> A.rcv)`: synced with the always-receptive runtime. */
private var handlerResponseActions: Set<Pair<String, String>> = emptySet()

internal data class TlaAction(
    val name: String,
    val def: String,
    /** Index binders (first) then used action args, as (name, TLA domain). */
    val params: List<Pair<String, String>> = emptyList(),
    /** Present when [name] was renamed for disambiguation. */
    val comment: String? = null,
    /** Leading [params] that are leaf index binders (create-index / `DynProc` slots). */
    val indexParamCount: Int = 0,
    /**
     * Formerly gated inclusion in `Fairness`; fairness is now all of `Next`.
     * Still recorded for diagnostics (sync / internal / timer / handler reply).
     */
    val systemControlled: Boolean = false,
    /** Snowcat types for [params] (same order); empty when untyped. */
    val paramApalacheTypes: List<String> = emptyList(),
) {
    fun nextDisjunct(): String {
        if (params.isEmpty()) return name
        val call = "$name(${params.joinToString(", ") { it.first }})"
        return wrapTlaExistsAvoidingBoundedSeqExpand(params, call)
    }
}

internal data class TlaActionOffer(
    val leaf: SpecLeaf,
    val decl: ActionDecl,
    val role: TSAction.SyncRole,
    val isConstructor: Boolean,
)

/**
 * Transition-type label for TLA section comments, matching analyze/`ListActions` wording:
 * `constructor` / `transition`, optionally prefixed with `timer` / `internal`.
 * Uses [TlaActionOffer.decl] modifier (not [TlaActionOffer.role]), since constructors are stored as
 * Internal on the offer for codegen grouping while their decl modifier remains Default.
 */
internal fun tlaTransitionTypeLabel(offer: TlaActionOffer): String {
    val kindStr = if (offer.isConstructor) "constructor" else "transition"
    return when {
        offer.decl.isTimer || offer.decl.modifier == TSAction.SyncRole.Timer -> "timer $kindStr"
        offer.decl.modifier == TSAction.SyncRole.Internal -> "internal $kindStr"
        offer.decl.modifier == TSAction.SyncRole.Service -> "service $kindStr"
        offer.decl.modifier == TSAction.SyncRole.Client -> "client $kindStr"
        else -> kindStr
    }
}

/** Local (non-syncing) offer: `internal`, `timer`, or Timer role. */
private fun isLocalTlaOffer(offer: TlaActionOffer): Boolean =
    !offer.isConstructor && (
        offer.role == TSAction.SyncRole.Internal ||
            offer.role == TSAction.SyncRole.Timer ||
            offer.decl.isTimer
        )

/** Sync offer: Default / Service / Client (non-timer). */
private fun isSyncableTlaOffer(offer: TlaActionOffer): Boolean =
    !offer.isConstructor &&
        !offer.decl.isTimer &&
        offer.role != TSAction.SyncRole.Internal &&
        offer.role != TSAction.SyncRole.Timer &&
        (
            offer.role == TSAction.SyncRole.Default ||
                offer.role == TSAction.SyncRole.Service ||
                offer.role == TSAction.SyncRole.Client
            )

private fun offerIsStatic(offer: TlaActionOffer, pclasses: Map<String, ProcClassNode>): Boolean =
    pclasses[offer.leaf.name]?.isStatic == true

/**
 * Pairing for syncable offers of one action name.
 * - one Service + N Clients → N pairwise lists (service with each client)
 * - one static + N dynamics (Defaults) → N pairwise lists (hub with each peer)
 * - exactly two statics / two Defaults → one pairwise list
 * - otherwise null (caller falls back to ordinary default grouping)
 */
/**
 * Parent↔child pairs for one action name: each `B<A>` child offer (constructor or sync)
 * with each syncable offer of an `A` occurrence. Null when no child meets its parent here.
 *
 * Hubs are excluded (matches Go hub-channel semantics and composition shapes):
 * - `service` + `client`: clients sync only with the service, never with each other
 * - ordinary hub (one static default + ≥1 dynamic defaults): peers sync with the static hub
 *   only — parent↔child must not fuse two dynamics that both call the same hub
 */
internal fun parentChildOfferPairs(
    group: List<TlaActionOffer>,
    pclasses: Map<String, ProcClassNode> = emptyMap(),
): List<List<TlaActionOffer>>? {
    val syncables = group.filter { isSyncableTlaOffer(it) }
    if (syncables.any { it.role == TSAction.SyncRole.Service } &&
        syncables.any { it.role == TSAction.SyncRole.Client }
    ) {
        return null
    }
    if (pclasses.isNotEmpty() && isOrdinaryStaticHubShape(syncables, pclasses)) {
        return null
    }
    val pairs = mutableListOf<List<TlaActionOffer>>()
    group.filter { it.leaf.parentClass != null }.forEach { child ->
        group.filter { p ->
            p.leaf.name == child.leaf.parentClass && p.leaf !== child.leaf && isSyncableTlaOffer(p)
        }.forEach { parent -> pairs += listOf(parent, child) }
    }
    return pairs.ifEmpty { null }
}

/** Ordinary hub shape: exactly one static default-role offer and ≥1 dynamic default offers. */
private fun isOrdinaryStaticHubShape(
    syncables: List<TlaActionOffer>,
    pclasses: Map<String, ProcClassNode>,
): Boolean {
    val defaults = syncables.filter { it.role == TSAction.SyncRole.Default }
    if (defaults.isEmpty()) return false
    val statics = defaults.filter { offerIsStatic(it, pclasses) }
    val dynamics = defaults.filter { !offerIsStatic(it, pclasses) }
    return statics.size == 1 && dynamics.isNotEmpty()
}

private fun hubPairedOfferLists(
    syncables: List<TlaActionOffer>,
    pclasses: Map<String, ProcClassNode>,
): List<List<TlaActionOffer>>? {
    if (syncables.isEmpty()) return null
    val services = syncables.filter { it.role == TSAction.SyncRole.Service }
    val clients = syncables.filter { it.role == TSAction.SyncRole.Client }
    if (services.size == 1 && clients.isNotEmpty()) {
        return clients.map { listOf(services[0], it) }
    }

    val defaults = syncables.filter { it.role == TSAction.SyncRole.Default }
    if (defaults.isEmpty()) return null
    val statics = defaults.filter { offerIsStatic(it, pclasses) }
    val dynamics = defaults.filter { !offerIsStatic(it, pclasses) }
    return when {
        statics.size == 1 && dynamics.isNotEmpty() ->
            dynamics.map { listOf(statics[0], it) }
        statics.size == 2 && dynamics.isEmpty() ->
            listOf(statics)
        defaults.size == 2 ->
            listOf(defaults)
        else -> null
    }
}

private data class TlaBuildResult(
    val helpers: List<String>,
    val actions: List<TlaAction>,
)

private fun safeType(vn: VarNode): Type = try {
    vn.type
} catch (_: RuntimeException) {
    intType
}

/**
 * Init comment listing state vars/consts dropped by `unused-vars` (declaration order).
 * Null when none were omitted or projection is off.
 */
internal fun unusedVarsOmittedComment(leaf: SpecLeaf, pc: ProcClassNode): String? {
    val proj = TlaVarProjection.get()
    if (!proj.projecting) return null
    val omitted = pc.localDecls().filterIsInstance<VarNode>()
        .map { it.name }
        .filter { !proj.isRelevant(leaf.name, it) }
    if (omitted.isEmpty()) return null
    return "\\* unused-vars: omitted ${omitted.joinToString(", ")}"
}

/**
 * Map put as an explicit domain-extending function rebuild.
 * Avoids TLC's "EXCEPT was applied to non-existing fields" warning (and matches
 * pure TLA+ better than relying on TLC extending DOMAIN via EXCEPT).
 *
 * [oldMap] is the pre-put map (`@` inside an outer EXCEPT, or the variable name).
 */
internal fun emitMapIndexPutRhs(oldMap: String, keyTla: String, valueTla: String): String =
    "LET __m == $oldMap __pk == $keyTla __pv == $valueTla IN " +
        "[__k \\in DOMAIN __m \\cup {__pk} |-> IF __k = __pk THEN __pv ELSE __m[__k]]"

private fun collectTlaActionOffers(
    leaves: List<SpecLeaf>,
    pclasses: Map<String, ProcClassNode>,
): List<TlaActionOffer> {
    val offers = mutableListOf<TlaActionOffer>()
    leaves.forEach { leaf ->
        val pc = pclasses[leaf.name] ?: return@forEach
        val inlineAssigns = pc.localDecls().filterIsInstance<VarNode>().mapNotNull { v ->
            val init = v.initExpr ?: return@mapNotNull null
            julay.compiler.decl.TransitUpdate.Assign(v.name, init)
        }
        pc.localDecls().flatMap { it.constructors() }.forEach { ctor ->
            val decl = if (inlineAssigns.isEmpty()) ctor
            else ctor.copy(transits = inlineAssigns + ctor.transits)
            offers += TlaActionOffer(leaf, decl, TSAction.SyncRole.Internal, isConstructor = true)
        }
        pc.localDecls().flatMap { it.transitions() }.forEach { tr ->
            offers += TlaActionOffer(leaf, tr, tr.modifier, isConstructor = false)
        }
    }
    return offers
}

/**
 * Offer lists that [buildTlaActions] would pass to [emitConjoined], in the same grouping order.
 * Used to precompute kill targets (`exitProc`).
 */
internal fun collectEmittedOfferLists(
    offers: List<TlaActionOffer>,
    pclasses: Map<String, ProcClassNode> = emptyMap(),
): List<List<TlaActionOffer>> {
    val result = mutableListOf<List<TlaActionOffer>>()
    offers.groupBy { it.decl.action.name }.forEach { (_, group) ->
        val constructors = group.filter { it.isConstructor }
        val locals = group.filter { isLocalTlaOffer(it) }
        val syncables = group.filter { isSyncableTlaOffer(it) }

        // Prefer service/client (and ordinary hub) pairing before parent↔child. Otherwise a
        // parent and child that both `client` the same service (e.g. ownerOf) are fused into
        // one action and the Store service arm is dropped.
        hubPairedOfferLists(syncables, pclasses)?.let { pairs ->
            pairs.forEach { result += it }
            constructors.forEach { result += listOf(it) }
            locals.forEach { result += listOf(it) }
            return@forEach
        }
        parentChildOfferPairs(group, pclasses)?.let { pairs ->
            pairs.forEach { result += it }
            val paired = pairs.flatten().toSet()
            (constructors + locals + syncables).filter { it !in paired }.forEach { result += listOf(it) }
            return@forEach
        }

        // 1 constructor + 1 default transition → one hybrid shared action
        if (constructors.size == 1 && syncables.size == 1) {
            result += listOf(syncables[0], constructors[0])
            return@forEach
        }

        constructors.forEach { result += listOf(it) }
        locals.forEach { result += listOf(it) }
        when {
            syncables.size >= 2 -> result += syncables
            syncables.size == 1 -> result += syncables
        }
    }
    return result
}

/**
 * Sole unsynced constructors of static leaves folded into Init: not a hub-paired API, not hybrid
 * ctor+default, and the leaf has no other constructor. Dynamic leaves are spawned per slot.
 */
internal fun foldableInitConstructors(
    offers: List<TlaActionOffer>,
    pclasses: Map<String, ProcClassNode> = emptyMap(),
): Map<String, TlaActionOffer> {
    val out = linkedMapOf<String, TlaActionOffer>()
    offers.filter { it.isConstructor }.groupBy { it.leaf.tlaName }.forEach { (tlaName, ctors) ->
        if (ctors.size != 1) return@forEach
        val offer = ctors.single()
        if (offer.leaf.isDynamic) return@forEach
        val group = offers.filter { it.decl.action.name == offer.decl.action.name }
        val syncables = group.filter { isSyncableTlaOffer(it) }
        if (hubPairedOfferLists(syncables, pclasses) != null) return@forEach
        val constructors = group.filter { it.isConstructor }
        if (constructors.size == 1 && syncables.size == 1) return@forEach
        // Unsupported builtins (parseInt/parseCfg, …) still fold: assigns that depend on
        // them become Init `\in` domains via foldedCtorAssignNeedsDomainInit.
        out[tlaName] = offer
    }
    return out
}

/** True when the group still primes a var after unused-vars (constructor / return always emit). */
internal fun offerGroupHasEmittedUpdate(group: List<TlaActionOffer>): Boolean =
    group.any { offer ->
        if (offer.isConstructor || offer.decl.isReturn) return@any true
        offer.decl.transits.any { update ->
            when (update) {
                is TransitUpdate.Assign, is TransitUpdate.IndexPut ->
                    TlaVarProjection.get().isRelevant(offer.leaf.name, update.transitRootVar())
                is TransitUpdate.Let -> false
            }
        }
    }

private fun leafParamDomain(leaf: SpecLeaf): String? =
    if (leaf.isParameterized) {
        typeDomainConstant(leaf.paramType!!) ?: leaf.paramType.toString()
    } else {
        null
    }

private fun leafInstDomain(leaf: SpecLeaf): String? =
    if (leaf.isDualIndexed) {
        typeDomainConstant(leaf.instParamType!!) ?: leaf.instParamType.toString()
    } else {
        null
    }

/** Upper bound for list models / `SetToSeq` CASE arms (must cover largest `.toList()`). */
internal const val DEFAULT_MAX_LIST_LEN = 3

/** Contiguous extras: lowest negative literal .. max(highest literal, MaxListLen+1). */
internal fun typeOkIntExtras(intLiterals: Set<Int>, maxListLen: Int = DEFAULT_MAX_LIST_LEN): List<Int> {
    val lo = intLiterals.minOrNull()?.coerceAtMost(0) ?: 0
    val hi = maxOf(intLiterals.maxOrNull() ?: 0, maxListLen + 1)
    return (lo..hi).toList()
}

internal fun emitTypeOkIntDef(intLiterals: Set<Int>, maxListLen: Int = DEFAULT_MAX_LIST_LEN): String {
    val extras = typeOkIntExtras(intLiterals, maxListLen)
    return "\\* cfg Int is a finite non-negative \\E bound and cannot include negatives; TypeOKInt unions Int, Nat, and that closed interval.\n" +
        "TypeOKInt == Int \\cup Nat \\cup {${extras.joinToString(", ")}}"
}

private fun typeOkVarRange(
    type: Type,
    intRange: String,
    fieldOfObj: String? = null,
    fieldName: String? = null,
    leafClass: String? = null,
    varName: String? = null,
    mapKeyUniverse: String? = null,
    setUniverse: String? = null,
    allowOpaqueSentinel: Boolean = true,
): String {
    closedLiteralDomain(type, leafClass, varName, fieldOfObj, fieldName)?.let { return it }
    return when (type) {
        is BoolType -> "BOOLEAN"
        is IntType, is RealType -> intRange
        is StringType -> "String"
        is DomainType -> type.name
        // Apalache rejects unbounded Seq(_) in TypeOK; BoundedSeq matches TLC models.
        is ListType -> "BoundedSeq(${typeOkVarRange(type.elementType, intRange, allowOpaqueSentinel = false)}, MaxListLen)"
        is SetType -> {
            val univ = setUniverse
                ?: typeOkEnumerableUniverse(type.elementType, intRange)
            "SUBSET $univ"
        }
        is MapType -> {
            val key = mapKeyUniverse ?: typeOkEnumerableUniverse(type.keyType, intRange)
            "[$key -> ${typeOkVarRange(type.valueType, intRange, allowOpaqueSentinel = false)}]"
        }
        is ObjClassType -> {
            if (TlaOpaqueIndexSort.isOpaque(type)) {
                // Top-level Node state may hold empty; nested/cluster/message fields are live-only.
                if (allowOpaqueSentinel) {
                    TlaOpaqueIndexSort.valueDomain(type.name)
                } else {
                    type.name
                }
            } else {
                val single = TlaFieldProjection.get().singletonField(type)
                if (single != null) {
                    typeOkVarRange(
                        single.type, intRange, type.name, single.name,
                        allowOpaqueSentinel = false,
                    )
                } else {
                    val fields = TlaFieldProjection.get().fieldsFor(type)
                    if (fields.isEmpty()) {
                        "[dummy: {0}]"
                    } else {
                        val rendered = fields.joinToString(", ") { f ->
                            "${f.name}: ${typeOkVarRange(
                                f.type, intRange, type.name, f.name,
                                allowOpaqueSentinel = false,
                            )}"
                        }
                        "[$rendered]"
                    }
                }
            }
        }
        else -> intRange
    }
}

/**
 * TLC can check `x \in TypeOKInt` pointwise, but `SUBSET TypeOKInt` and `[TypeOKInt -> T]`
 * require an enumerable universe. Cfg `Int` is finite; prefer a known state collection
 * ([mapKeyUniverse] / [setUniverse]) when TypeOK shapes provide one.
 */
private fun typeOkEnumerableUniverse(type: Type, intRange: String): String =
    if (tlaElemIsInt(type)) "Int" else typeOkVarRange(type, intRange, allowOpaqueSentinel = false)

private fun typeOkRangeForVar(
    leaf: SpecLeaf,
    vn: VarNode,
    intRange: String,
    shapes: TypeOkShapePlan,
    stateVarNames: Map<Pair<String, String>, String>,
    binder: String?,
): String {
    fun srcRead(src: String): String {
        val srcTla = stateTlaName(leaf.tlaName, src, stateVarNames)
        return if (leaf.indexesState(src) && binder != null) "$srcTla[$binder]" else srcTla
    }
    val mapKey = shapes.domainEq[leaf.tlaName to vn.name]?.let { srcRead(it) }
    val setUniv = shapes.subsetSrc[leaf.tlaName to vn.name]?.let { srcRead(it) }
        ?: shapes.subsetRange[leaf.tlaName to vn.name]?.let { "Range(${srcRead(it)})" }
    return typeOkVarRange(
        effectiveVarType(leaf, vn),
        intRange,
        leafClass = leaf.name,
        varName = vn.name,
        mapKeyUniverse = mapKey,
        setUniverse = setUniv,
    )
}

private fun closedLiteralDomain(
    type: Type,
    leafClass: String?,
    varName: String?,
    fieldOfObj: String?,
    fieldName: String?,
): String? {
    if (type !is StringType && type !is IntType) return null
    val lits = when {
        leafClass != null && varName != null ->
            TlaLiteralDomainProjection.get().varSet(leafClass, varName)
        fieldOfObj != null && fieldName != null ->
            TlaLiteralDomainProjection.get().objFieldSet(fieldOfObj, fieldName)
        else -> null
    }
    if (lits.isNullOrEmpty()) return null
    return TlaLiteralDomainProjection.get().render(lits)
}

private fun typeOkConjunct(
    name: String,
    range: String,
    paramDomain: String?,
    instDomain: String? = null,
): String {
    val ty = when {
        paramDomain != null && instDomain != null -> "[$paramDomain -> [$instDomain -> $range]]"
        paramDomain != null -> "[$paramDomain -> $range]"
        else -> range
    }
    return "/\\ $name \\in $ty"
}

/**
 * Sparse Julay maps: pointwise DOMAIN/range checks (TLC-cheap) instead of
 * `var \in [K -> V]` (total; rejects empty Init). When [mapKeyUniverse] is set
 * the map is total over that collection.
 */
private fun typeOkMapConjunct(
    name: String,
    mapType: MapType,
    intRange: String,
    paramDomain: String?,
    instDomain: String? = null,
    mapKeyUniverse: String? = null,
    leafClass: String? = null,
    varName: String? = null,
): String {
    val keyUniv = mapKeyUniverse ?: typeOkEnumerableUniverse(mapType.keyType, intRange)
    val valueRange = typeOkVarRange(
        mapType.valueType, intRange, leafClass = leafClass, varName = varName,
        allowOpaqueSentinel = false,
    )
    if (mapKeyUniverse != null) {
        return typeOkConjunct(name, "[$keyUniv -> $valueRange]", paramDomain, instDomain)
    }
    return when {
        paramDomain != null && instDomain != null ->
            "/\\ \\A n__tok \\in $paramDomain, i__tok \\in $instDomain :\n" +
                "     /\\ DOMAIN $name[n__tok][i__tok] \\subseteq $keyUniv\n" +
                "     /\\ \\A k__tok \\in DOMAIN $name[n__tok][i__tok] : " +
                "$name[n__tok][i__tok][k__tok] \\in $valueRange"
        paramDomain != null ->
            "/\\ \\A n__tok \\in $paramDomain :\n" +
                "     /\\ DOMAIN $name[n__tok] \\subseteq $keyUniv\n" +
                "     /\\ \\A k__tok \\in DOMAIN $name[n__tok] : " +
                "$name[n__tok][k__tok] \\in $valueRange"
        else ->
            "/\\ DOMAIN $name \\subseteq $keyUniv\n" +
                "  /\\ \\A k__tok \\in DOMAIN $name : $name[k__tok] \\in $valueRange"
    }
}

/**
 * Pointwise list TypeOK (like sparse maps). Avoids `var \in BoundedSeq(S, MaxListLen)`,
 * which TLC may fully construct and reject when |S|^MaxListLen exceeds 1e6.
 * Length is bounded by StateConstraint's `Cardinality(DOMAIN) <= MaxListLen`.
 */
private fun typeOkListConjunct(
    name: String,
    listType: ListType,
    intRange: String,
    paramDomain: String?,
    instDomain: String? = null,
    leafClass: String? = null,
    varName: String? = null,
): String {
    val elemRange = typeOkVarRange(
        listType.elementType, intRange, leafClass = leafClass, varName = varName,
        allowOpaqueSentinel = false,
    )
    return when {
        paramDomain != null && instDomain != null ->
            "/\\ \\A n__tok \\in $paramDomain, i__tok \\in $instDomain :\n" +
                "     \\A j__tok \\in DOMAIN $name[n__tok][i__tok] : " +
                "$name[n__tok][i__tok][j__tok] \\in $elemRange"
        paramDomain != null ->
            "/\\ \\A n__tok \\in $paramDomain :\n" +
                "     \\A j__tok \\in DOMAIN $name[n__tok] : " +
                "$name[n__tok][j__tok] \\in $elemRange"
        else ->
            "/\\ \\A j__tok \\in DOMAIN $name : $name[j__tok] \\in $elemRange"
    }
}

/** TypeOK conjunct for a leaf state var (maps/lists get sparse DOMAIN checks). */
private fun typeOkForStateVar(
    leaf: SpecLeaf,
    vn: VarNode,
    tlaName: String,
    intRange: String,
    shapes: TypeOkShapePlan,
    stateVarNames: Map<Pair<String, String>, String>,
    binder: String?,
    domain: String?,
    instDomain: String?,
): String {
    val ty = effectiveVarType(leaf, vn)
    val indexed = leaf.indexesState(vn.name)
    val paramDom = if (indexed) domain else null
    val instDom = if (indexed && leaf.isDualIndexed) instDomain else null
    if (ty is MapType) {
        fun srcRead(src: String): String {
            val srcTla = stateTlaName(leaf.tlaName, src, stateVarNames)
            return if (leaf.indexesState(src) && binder != null) "$srcTla[$binder]" else srcTla
        }
        val mapKey = shapes.domainEq[leaf.tlaName to vn.name]?.let { srcRead(it) }
        return typeOkMapConjunct(
            tlaName, ty, intRange, paramDom, instDom, mapKey,
            leafClass = leaf.name, varName = vn.name,
        )
    }
    if (ty is ListType) {
        return typeOkListConjunct(
            tlaName, ty, intRange, paramDom, instDom,
            leafClass = leaf.name, varName = vn.name,
        )
    }
    val range = typeOkRangeForVar(leaf, vn, intRange, shapes, stateVarNames, binder)
    return typeOkConjunct(tlaName, range, paramDom, instDom)
}

/** cfg Int plus negative literals already in the model (`votedFor = -1`). Not TypeOKInt extras. */
internal fun constraintIntRange(intLiterals: Set<Int>): String {
    val negatives = intLiterals.filter { it < 0 }.sorted()
    return if (negatives.isEmpty()) {
        "Int"
    } else {
        "Int \\cup {${negatives.joinToString(", ")}}"
    }
}

private fun appendStateConstraint(
    parts: MutableList<String>,
    varName: String,
    type: Type,
    indexed: Boolean,
    binder: String?,
    domain: String?,
    instBinder: String? = null,
    instDomain: String? = null,
    intRange: String,
) {
    val dual = indexed && binder != null && domain != null &&
        instBinder != null && instDomain != null
    val single = indexed && binder != null && domain != null
    when (type) {
        is IntType -> {
            val body = when {
                dual ->
                    "\\A $binder \\in $domain, $instBinder \\in $instDomain : " +
                        "$varName[$binder][$instBinder] \\in $intRange"
                single -> "\\A $binder \\in $domain : $varName[$binder] \\in $intRange"
                else -> "$varName \\in $intRange"
            }
            parts += "/\\ $body"
        }
        is ListType -> {
            val body = when {
                dual ->
                    "\\A $binder \\in $domain, $instBinder \\in $instDomain : " +
                        "Cardinality(DOMAIN $varName[$binder][$instBinder]) <= MaxListLen"
                single -> "\\A $binder \\in $domain : Cardinality(DOMAIN $varName[$binder]) <= MaxListLen"
                else -> "Cardinality(DOMAIN $varName) <= MaxListLen"
            }
            parts += "/\\ $body"
        }
        else -> {}
    }
}

/** Domains TypeOK mentions that must be module CONSTANTs (`String`). */
private fun collectTypeOkDomainModels(
    leaves: List<SpecLeaf>,
    pclasses: Map<String, ProcClassNode>,
    cfgOverrides: MutableSet<String>,
) {
    fun walk(type: Type) {
        when (type) {
            is StringType -> cfgOverrides += "String"
            is ListType -> walk(type.elementType)
            is SetType -> walk(type.elementType)
            is MapType -> {
                walk(type.keyType)
                walk(type.valueType)
            }
            is ObjClassType -> TlaFieldProjection.get().fieldsFor(type).forEach { walk(it.type) }
            else -> {}
        }
    }
    leaves.forEach { leaf ->
        val pc = pclasses[leaf.name] ?: return@forEach
        pc.localDecls().filterIsInstance<VarNode>().forEach { vn ->
            if (TlaVarProjection.get().isRelevant(leaf.name, vn.name)) {
                walk(effectiveVarType(leaf, vn))
            }
        }
    }
}

/** Finite TLC models for const-global Init (`BoundedSeq` / cfg Int), not TypeOK's `Seq`. */
private fun collectConstGlobalInitDomainModels(
    leaves: List<SpecLeaf>,
    pclasses: Map<String, ProcClassNode>,
    cfgOverrides: MutableSet<String>,
) {
    leaves.forEach { leaf ->
        if (leaf.globalConstVars.isEmpty()) return@forEach
        val pc = pclasses[leaf.name] ?: return@forEach
        pc.localDecls().filterIsInstance<VarNode>().forEach { vn ->
            if (vn.name !in leaf.globalConstVars) return@forEach
            if (!TlaVarProjection.get().isRelevant(leaf.name, vn.name)) return@forEach
            try {
                collectDomainModelNames(safeType(vn), cfgOverrides)
            } catch (_: RuntimeException) {
            }
        }
    }
}

private fun buildTlaActions(
    leaves: List<SpecLeaf>,
    pclasses: Map<String, ProcClassNode>,
    offers: List<TlaActionOffer>,
    stateVarNames: Map<Pair<String, String>, String>,
    @Suppress("UNUSED_PARAMETER") cfgOverrides: MutableSet<String> = linkedSetOf(),
    leafSpecs: Map<String, LeafSpecNode> = emptyMap(),
    foldedCtorLeaves: Set<String> = emptySet(),
    fluentVarNames: List<String> = emptyList(),
): TlaBuildResult {
    // Opaque index-sort projections (Node_id, Node_url, …) are VARIABLES but never primed;
    // include them in allVars so every action's UNCHANGED covers them for TLC.
    val allVars = allTlaVars(leaves, pclasses, stateVarNames, foldedCtorLeaves) +
        fluentVarNames +
        TlaOpaqueIndexSort.allProjectionNames().sorted()
    val stateVarsByLeaf = leaves.associate { leaf ->
        leaf.tlaName to (
            pclasses[leaf.name]
                ?.localDecls()
                ?.filterIsInstance<VarNode>()
                ?.map { it.name }
                ?.toSet()
                ?: emptySet()
            )
    }

    val result = mutableListOf<TlaAction>()
    val byName = offers.groupBy { it.decl.action.name }

    fun emit(
        name: String,
        offerList: List<TlaActionOffer>,
        comment: String? = null,
    ) {
        emitConjoined(
            name, offerList, allVars, stateVarsByLeaf, stateVarNames,
            comment,
            pclasses = pclasses,
            leafSpecs = leafSpecs,
            systemLeaves = leaves,
            foldedCtorLeaves = foldedCtorLeaves,
        )?.let { result += it }
    }

    byName.forEach { (actionName, group) ->
        val constructors = group.filter { it.isConstructor }
        val locals = group.filter { isLocalTlaOffer(it) }
        val syncables = group.filter { isSyncableTlaOffer(it) }

        // Prefer service/client (and ordinary hub) pairing before parent↔child — same order as
        // [collectEmittedOfferLists]. Parent+child that both client a service must not fuse.
        hubPairedOfferLists(syncables, pclasses)?.let { pairs ->
            val needDisambiguate = pairs.size > 1
            pairs.forEach { pair ->
                val name: String
                val comment: String?
                if (needDisambiguate && pair.size >= 2) {
                    val a = pair[0]
                    val b = pair[1]
                    name = "${actionName}_${a.leaf.tlaName}_${b.leaf.tlaName}"
                    comment = when {
                        pair.any { it.role == TSAction.SyncRole.Service } &&
                            pair.any { it.role == TSAction.SyncRole.Client } -> {
                            val service = pair.first { it.role == TSAction.SyncRole.Service }
                            val client = pair.first { it.role == TSAction.SyncRole.Client }
                            "$actionName action where ${service.leaf.tlaName} is the service and " +
                                "${client.leaf.tlaName} is the client"
                        }
                        pair.count { offerIsStatic(it, pclasses) } == 1 -> {
                            val hub = pair.first { offerIsStatic(it, pclasses) }
                            val peer = pair.first { !offerIsStatic(it, pclasses) }
                            "$actionName action where ${hub.leaf.tlaName} is the hub and " +
                                "${peer.leaf.tlaName} is the peer"
                        }
                        else ->
                            "$actionName action between ${a.leaf.tlaName} and ${b.leaf.tlaName}"
                    }
                } else {
                    name = actionName
                    comment = null
                }
                emit(name, pair, comment)
            }
            // Still emit constructors / locals for this action name
            val disambiguateCtors = constructors.size > 1
            constructors.forEach { offer ->
                if (offer.leaf.tlaName in foldedCtorLeaves) return@forEach
                val name = if (disambiguateCtors) {
                    if (offer.decl.action.name == "initially") "${offer.leaf.tlaName}_initially"
                    else "${actionName}_${offer.leaf.tlaName}"
                } else actionName
                val comment = if (disambiguateCtors) {
                    if (offer.decl.action.name == "initially") "initially constructor on ${offer.leaf.tlaName}"
                    else "$actionName action on ${offer.leaf.tlaName}"
                } else null
                emit(name, listOf(offer), comment)
            }
            val disambiguateLocals = locals.size > 1
            locals.forEach { offer ->
                val name = if (disambiguateLocals) "${actionName}_${offer.leaf.tlaName}" else actionName
                val comment = if (disambiguateLocals) "$actionName action on ${offer.leaf.tlaName}" else null
                emit(name, listOf(offer), comment)
            }
            return@forEach
        }

        parentChildOfferPairs(group, pclasses)?.let { pairs ->
            val needDisambiguate = pairs.size > 1
            pairs.forEach { pair ->
                val (parent, child) = pair
                if (needDisambiguate) {
                    emit(
                        "${actionName}_${parent.leaf.tlaName}_${child.leaf.tlaName}",
                        pair,
                        "$actionName action where ${parent.leaf.tlaName} is the parent and " +
                            "${child.leaf.tlaName} is the child",
                    )
                } else {
                    emit(actionName, pair)
                }
            }
            val paired = pairs.flatten().toSet()
            val rest = (constructors + locals + syncables).filter { it !in paired }
            rest.forEach { offer ->
                emit("${actionName}_${offer.leaf.tlaName}", listOf(offer), "$actionName action on ${offer.leaf.tlaName}")
            }
            return@forEach
        }

        // 1 constructor + 1 default transition → one hybrid shared action
        if (constructors.size == 1 && syncables.size == 1) {
            emit(actionName, listOf(syncables[0], constructors[0]))
            return@forEach
        }

        // Solo constructors (any name, including initially) — valid leaf entry
        val disambiguateCtors = constructors.size > 1
        constructors.forEach { offer ->
            if (offer.leaf.tlaName in foldedCtorLeaves) return@forEach
            val name: String
            val comment: String?
            if (disambiguateCtors) {
                name = if (offer.decl.action.name == "initially") {
                    "${offer.leaf.tlaName}_initially"
                } else {
                    "${actionName}_${offer.leaf.tlaName}"
                }
                comment = if (offer.decl.action.name == "initially") {
                    "initially constructor on ${offer.leaf.tlaName}"
                } else {
                    "$actionName action on ${offer.leaf.tlaName}"
                }
            } else {
                name = actionName
                comment = null
            }
            emit(name, listOf(offer), comment)
        }

        val disambiguateLocals = locals.size > 1
        locals.forEach { offer ->
            val name: String
            val comment: String?
            if (disambiguateLocals) {
                name = "${actionName}_${offer.leaf.tlaName}"
                comment = "$actionName action on ${offer.leaf.tlaName}"
            } else {
                name = actionName
                comment = null
            }
            emit(name, listOf(offer), comment)
        }

        when {
            syncables.isNotEmpty() -> emit(actionName, syncables)
        }
    }

    val unique = result.distinctBy { it.name }
    val (initiallyActions, otherActions) = unique.partition { it.isInitiallyAction() }
    return TlaBuildResult(emptyList(), initiallyActions + otherActions)
}

/** True for solo `initially` / `Leaf_initially` constructors (defs + Next lead with these). */
private fun TlaAction.isInitiallyAction(): Boolean =
    name == "initially" || name.endsWith("_initially")

private fun allTlaVars(
    leaves: List<SpecLeaf>,
    pclasses: Map<String, ProcClassNode>,
    stateVarNames: Map<Pair<String, String>, String>,
    foldedCtorLeaves: Set<String> = emptySet(),
): List<String> =
    leaves.flatMap { leaf ->
        val pc = pclasses[leaf.name] ?: return@flatMap emptyList()
        val base = mutableListOf<String>()
        if (leaf.tlaName !in foldedCtorLeaves) {
            base += stateTlaName(leaf.tlaName, "constructed", stateVarNames)
            base += stateTlaName(leaf.tlaName, "retired", stateVarNames)
        }
        if ((leaf.tlaName to "parent") in stateVarNames) {
            base += stateTlaName(leaf.tlaName, "parent", stateVarNames)
        }
        base + pc.localDecls().filterIsInstance<VarNode>().filter {
            TlaVarProjection.get().isRelevant(leaf.name, it.name)
        }.map {
            stateTlaName(leaf.tlaName, it.name, stateVarNames)
        }
    }

private fun emitConjoined(
    name: String,
    offers: List<TlaActionOffer>,
    allVars: List<String>,
    stateVarsByLeaf: Map<String, Set<String>>,
    stateVarNamesIn: Map<Pair<String, String>, String>,
    comment: String? = null,
    pclasses: Map<String, ProcClassNode> = emptyMap(),
    leafSpecs: Map<String, LeafSpecNode> = emptyMap(),
    systemLeaves: List<SpecLeaf> = emptyList(),
    foldedCtorLeaves: Set<String> = emptySet(),
): TlaAction? {
    // Peer reads use class names (Peer.self); map unique class → occurrence tlaName.
    val stateVarNames = stateVarNamesIn.toMutableMap()
    systemLeaves.groupBy { it.name }.forEach { (cls, occs) ->
        if (occs.size == 1) {
            val leaf = occs.single()
            stateVarNamesIn.forEach { (key, id) ->
                if (key.first == leaf.tlaName) {
                    stateVarNames[cls to key.second] = id
                }
            }
        }
    }
    val parts = mutableListOf<String>()
    val changed = mutableSetOf<String>()
    val argParams = mutableListOf<Pair<String, String>>()
    val auxParams = mutableListOf<Pair<String, String>>()
    val auxNamesByLeaf = mutableMapOf<String, Set<String>>()
    val extraArgNames = collectTlaExtraArgNames(offers, pclasses, leafSpecs)
    val bindPlan = analyzeTlaArgBind(offers, TlaEmitOpts.get(), extraArgNames)
    TlaSkipConjuncts.install(bindPlan.skipConjuncts)
    val symbolTypes = mutableMapOf<String, Type>()
    offers.forEach { offer ->
        offer.decl.action.args.forEach { symbolTypes[it.name] = it.type }
        pclasses[offer.leaf.name]?.localDecls()?.filterIsInstance<VarNode>()?.forEach { vn ->
            try {
                symbolTypes[vn.name] = TlaFieldVarProjection.get()
                    .effectiveType(offer.leaf.name, vn.name, vn.type)
            } catch (_: RuntimeException) {
            }
        }
    }
    TlaSymbolTypes.set(TlaSymbolTypes.get() + symbolTypes)

    val bindNames = linkedSetOf<String>().apply {
        addAll(bindPlan.skipArgs)
        addAll(bindPlan.extraBinderNames())
        bindPlan.setBinds.forEach { add(it.arg) }
        bindPlan.determined.forEach { add(it.first) }
        bindPlan.listBinds.forEach { add(it.arg) }
        bindPlan.structBinds.forEach { b -> b.argPaths.forEach { add(it.first) } }
    }

    offers.forEach { offer ->
        val aux = collectLeafSpecAuxParams(
            offer.leaf,
            offer.decl.action.name,
            offer.isConstructor,
            pclasses,
            leafSpecs,
        )
        auxNamesByLeaf[offer.leaf.tlaName] = aux.map { it.name }.toSet()
        aux.forEach { a ->
            if (a.name !in bindPlan.omitArgTypeDomains()) {
                auxParams += a.name to a.domain
            }
        }
    }

    val constGlobalArgBinds = collectGlobalConstArgBinds(offers, stateVarNames)
    val variableTlaIds = stateVarNames.values.toSet()

    // Instance binders for parameterized leaves: paramName indexes into the type domain.
    val selfBinders = linkedMapOf<String, String>() // leafName -> binder
    val sharedWithBinders = linkedMapOf<String, String>() // withScopeId -> binder
    fun registerWithBinder(leaf: SpecLeaf) {
        val scope = leaf.withScopeId ?: return
        val name = leaf.paramName ?: return
        // Keep VARIABLE `n` when a state var is named `n`; clash-rename the binder instead.
        if (name in variableTlaIds) return
        sharedWithBinders.putIfAbsent(scope, name)
    }
    // Pre-register before clash-rename: a reserved leaf-spec decl param (Net's `n`)
    // must not force `n_RaftProtocol` on a peer that shares the same `with` scope.
    offers.forEach { registerWithBinder(it.leaf) }
    // Module-level TLA ids only. Do not reserve action-arg names here: that would
    // suffix-rename every param (`target` → `target_`) because the name is taken by itself.
    // Snapshot before bindLeaf: dest LETs may share the index binder (`n`) and must
    // not be suffix-renamed just because that binder is in `reserved`.
    val moduleLevelTaken = (variableTlaIds + allVars).toMutableSet()
    val reserved = moduleLevelTaken.toMutableSet()
    val instBinders = linkedMapOf<String, String>()
    fun bindLeaf(leaf: SpecLeaf) {
        // Apply-only / leaf-spec under `with`: register shared binder without lifting state.
        if (!leaf.isParameterized && leaf.withScopeId != null && leaf.paramName != null) {
            if (leaf.paramName!! !in variableTlaIds) {
                sharedWithBinders.putIfAbsent(leaf.withScopeId!!, leaf.paramName!!)
                reserved += leaf.paramName!!
            }
            return
        }
        if (!leaf.isParameterized || leaf.tlaName in selfBinders) return
        val binder = indexBinderName(leaf, reserved, sharedWithBinders)
        selfBinders[leaf.tlaName] = binder
        reserved += binder
        leaf.withScopeId?.let { sharedWithBinders.putIfAbsent(it, binder) }
        if (leaf.isDualIndexed) {
            val base = leaf.instParamName!!
            val ib = if (base !in reserved) base else firstFreeParamTlaName(base, reserved)
            instBinders[leaf.tlaName] = ib
            reserved += ib
        }
    }
    offers.forEach { offer -> bindLeaf(offer.leaf) }

    val takenParamIds = moduleLevelTaken.toMutableSet()
    val argRenames = linkedMapOf<String, String>()
    val seenArgs = mutableSetOf<String>()
    fun renameIfTaken(name: String) {
        if (name in constGlobalArgBinds) return
        if (name in seenArgs) return
        seenArgs += name
        val tlaArg = firstFreeParamTlaName(name, takenParamIds)
        takenParamIds += tlaArg
        if (tlaArg != name) argRenames[name] = tlaArg
    }
    offers.forEach { offer ->
        offer.decl.action.args.filter { offerRefsArg(offer, it.name) }.forEach { arg ->
            renameIfTaken(arg.name)
        }
    }
    // Determined / from-collection binders still become LET / `\E` names and must
    // not collide with VARIABLES (`lastLogTerm` vs. the message-field LET).
    bindNames.forEach { renameIfTaken(it) }
    val exprOverrides = argRenames + constGlobalArgBinds
    fun argTla(name: String): String = exprOverrides[name] ?: name

    /** Index path for reads/EXCEPT: `n` or `n][i` so `$v[$path]` / `EXCEPT ![$path]` work. */
    fun selfOf(leaf: SpecLeaf): String? {
        val n = selfBinders[leaf.tlaName] ?: return null
        val i = instBinders[leaf.tlaName]
        return if (leaf.isDualIndexed && i != null) "$n][$i" else n
    }

    /** Slot of [leaf] in this action: its `DynProc` binder (null for static leaves). */
    fun slotOf(leaf: SpecLeaf): String? = when {
        !leaf.isDynamic -> null
        leaf.isDualIndexed -> instBinders[leaf.tlaName]
        else -> selfBinders[leaf.tlaName]
    }

    /**
     * Parent link for a `B<A>` child meeting its parent in this action: the child constructor
     * records the parent slot; every other parent↔child sync checks it. Pairs of (part, primed var).
     */
    fun parentLinkParts(offer: TlaActionOffer): List<Pair<String, String?>> {
        val child = offer.leaf
        if ((child.tlaName to "parent") !in stateVarNames) return emptyList()
        val parent = offers.firstOrNull { it.leaf.name == child.parentClass && it.leaf !== child }?.leaf
            ?: return emptyList()
        val pv = stateTlaName(child.tlaName, "parent", stateVarNames)
        val value = parentLinkValue(parent, child, systemLeaves, slotOf(parent))
        val self = selfOf(child)
        return if (offer.isConstructor) {
            listOf(
                (if (self != null) "/\\ $pv' = [$pv EXCEPT ![$self] = $value]" else "/\\ $pv' = $value") to pv,
            )
        } else {
            listOf((if (self != null) "/\\ $pv[$self] = $value" else "/\\ $pv = $value") to null)
        }
    }

    fun pickOfferFor(expr: ExprNode): TlaActionOffer {
        // `this.x` does not count as a symbol ref (never an arg), so match the leaf that
        // owns state x before falling back — otherwise determined-args LET RHS can bind the
        // unindexed VARIABLE map (e.g. getKey_ == getKey instead of getKey[n][i]).
        val thisVars = collectThisAccessStateVars(expr)
        if (thisVars.isNotEmpty()) {
            offers.firstOrNull { offer ->
                val bare = stateVarsByLeaf[offer.leaf.tlaName].orEmpty()
                thisVars.any { it in bare }
            }?.let { return it }
        }
        for (offer in offers) {
            val bare = stateVarsByLeaf[offer.leaf.tlaName].orEmpty()
            if (bare.any { exprReferencesSymbol(expr, it) }) return offer
            if (offer.decl.action.args.any { exprReferencesSymbol(expr, it.name) }) return offer
        }
        return offers.first()
    }

    fun emitBound(expr: ExprNode, linePrefix: String = ""): String {
        val offer = pickOfferFor(expr)
        val self = selfOf(offer.leaf)
        val auxNames = auxNamesByLeaf[offer.leaf.tlaName].orEmpty()
        val names = offer.decl.action.args.map { it.name }.toSet() + auxNames + bindNames
        val leafCtx = mapOf(offer.leaf.name to offer.leaf, offer.leaf.tlaName to offer.leaf)
        return exprToTla(
            expr, leafCtx, names, self,
            bareStateVars = stateVarsByLeaf[offer.leaf.tlaName].orEmpty(),
            stateVarNames = stateVarNames,
            symbolOverrides = exprOverrides,
            linePrefix = linePrefix,
        )
    }

    bindPlan.listBinds.forEach { b ->
        argParams += argTla(b.index) to "1..Len(${emitBound(b.list)})"
    }
    bindPlan.setBinds.forEach { b ->
        argParams += argTla(b.arg) to emitBound(b.set)
    }
    bindPlan.structBinds.forEach { b ->
        argParams += argTla(b.tmp) to emitBound(b.set)
    }


    fun emitTransitUpdates(offer: TlaActionOffer, targetParts: MutableList<String>, targetChanged: MutableSet<String>) {
        val self = selfOf(offer.leaf)
        val auxNames = auxNamesByLeaf[offer.leaf.tlaName].orEmpty()
        val baseArgNames = offer.decl.action.args.map { it.name }.toSet() + auxNames + bindNames
        val leafCtx = mapOf(offer.leaf.name to offer.leaf, offer.leaf.tlaName to offer.leaf)
        val bare = stateVarsByLeaf[offer.leaf.tlaName].orEmpty()
        val taken = mutableSetOf<String>()
        taken += baseArgNames
        taken += bare
        taken += stateVarNames.filter { it.key.first == offer.leaf.tlaName }.map { it.value }
        self?.let { taken += it }

        val letOverrides = linkedMapOf<String, String>()
        val bindings = mutableListOf<TransitLetEmit>()
        val prefixParts = mutableListOf<String>()
        val scopedParts = mutableListOf<String>()
        var seenLet = false

        fun effectiveArgNames(): Set<String> = baseArgNames + letOverrides.values
        fun indexes(varName: String): Boolean = self != null && offer.leaf.indexesState(varName)
        fun emitExpr(expr: ExprNode, linePrefix: String): String =
            exprToTla(
                expr, leafCtx, effectiveArgNames(), self,
                bareStateVars = bare,
                stateVarNames = stateVarNames,
                symbolOverrides = exprOverrides + letOverrides.toMap(),
                linePrefix = linePrefix,
            )

        fun addAssignPart(part: String) {
            if (seenLet) scopedParts += part else prefixParts += part
        }

        offer.decl.transits.forEach { update ->
            when (update) {
                is TransitUpdate.Let -> {
                    // Discard `let _ := …` has no TLA binder and is omitted from the spec.
                    if (update.name.isDiscardBinding()) return@forEach
                    seenLet = true
                    val tlaName = allocTlaName(update.name, taken)
                    val io = exprContainsIoHavoc(update.init) ||
                        exprContainsUnsupportedTlaBuiltin(update.init)
                    // IO / unsupported-builtin havoc must stay TLC-enumerable: list length 1
                    // (full MaxListLen × large element types exceeds TLC's 1e6-set limit).
                    val ioDomain = if (io) typeToTlaIoDomain(update.type) else null
                    val initTla = if (io) {
                        tlaName
                    } else {
                        emitExpr(update.init, "/\\ LET $tlaName == ")
                    }
                    bindings += TransitLetEmit(tlaName, initTla, ioDomain)
                    letOverrides[update.name] = tlaName
                }
                is TransitUpdate.Assign -> {
                    if (!TlaVarProjection.get().isRelevant(offer.leaf.name, update.transitRootVar())) {
                        return@forEach
                    }
                    val root = update.key.substringBefore('.')
                    val v = stateTlaName(offer.leaf.tlaName, root, stateVarNames)
                    val constGlobal = root in offer.leaf.globalConstVars
                    if (!constGlobal) {
                        targetChanged += v
                    }
                    val expr = update.expr
                    val argNames = offer.decl.action.args.map { it.name }.toSet()
                    if (constGlobal && isActionArgSymbol(expr, argNames)) {
                        return@forEach
                    }
                    if (exprContainsIoHavoc(expr) ||
                        exprContainsUnsupportedTlaBuiltin(expr)
                    ) {
                        val domain = typeToTlaDomain(expr.getType())
                        addAssignPart(
                            if (constGlobal) {
                                "/\\ $v \\in $domain \\* global const check"
                            } else if (indexes(root)) {
                                "/\\ \\E __io \\in $domain: $v' = [$v EXCEPT ![$self] = __io]"
                            } else {
                                "/\\ $v' \\in $domain"
                            },
                        )
                    } else if (constGlobal) {
                        val assignPrefix = "/\\ $v = "
                        val rhs = emitFieldVarProjectedRhs(
                            offer.leaf.name, root, update.key, expr, assignPrefix, ::emitExpr,
                        )
                        // Parenthesize `[1 |-> …]` so `/\\` junctions do not parse `[` as a new item.
                        addAssignPart("/\\ $v = ${parenIfBracketExpr(rhs)} \\* global const check")
                    } else {
                        val assignPrefix = if (indexes(root)) {
                            "/\\ $v' = [$v EXCEPT ![$self] = "
                        } else {
                            "/\\ $v' = "
                        }
                        val rhs = emitFieldVarProjectedRhs(
                            offer.leaf.name, root, update.key, expr, assignPrefix, ::emitExpr,
                        )
                        addAssignPart(
                            if (indexes(root)) {
                                "/\\ $v' = [$v EXCEPT ![$self] = $rhs]"
                            } else {
                                "/\\ $v' = $rhs"
                            },
                        )
                    }
                }
                is TransitUpdate.IndexPut -> {
                    if (!TlaVarProjection.get().isRelevant(offer.leaf.name, update.transitRootVar())) {
                        return@forEach
                    }
                    val v = stateTlaName(offer.leaf.tlaName, update.collectionVar, stateVarNames)
                    val constGlobal = update.collectionVar in offer.leaf.globalConstVars
                    if (!constGlobal) {
                        targetChanged += v
                    }
                    val argNames = offer.decl.action.args.map { it.name }.toSet()
                    if (constGlobal && isActionArgSymbol(update.value, argNames)) {
                        return@forEach
                    }
                    val k = emitExpr(update.index, "")
                    // Maps: DOMAIN ∪ {key} rebuild when map-index-put is on (default);
                    // nested EXCEPT when disabled. Lists always use EXCEPT.
                    val quietMapPut = TlaSymbolTypes.get()[update.collectionVar] is MapType &&
                        TlaEmitOpts.get().mapIndexPutRebuild
                    if (exprContainsIoHavoc(update.value)) {
                        val domain = typeToTlaDomain(update.value.getType())
                        addAssignPart(
                            when {
                                quietMapPut && constGlobal ->
                                    "/\\ \\E __io \\in $domain: $v = ${emitMapIndexPutRhs(v, k, "__io")} \\* global const check"
                                quietMapPut && indexes(update.collectionVar) ->
                                    "/\\ \\E __io \\in $domain: $v' = [$v EXCEPT ![$self] = ${emitMapIndexPutRhs("@", k, "__io")}]"
                                quietMapPut ->
                                    "/\\ \\E __io \\in $domain: $v' = ${emitMapIndexPutRhs(v, k, "__io")}"
                                constGlobal ->
                                    "/\\ \\E __io \\in $domain: $v = [$v EXCEPT ![$k] = __io] \\* global const check"
                                indexes(update.collectionVar) ->
                                    "/\\ \\E __io \\in $domain: $v' = [$v EXCEPT ![$self] = [@ EXCEPT ![$k] = __io]]"
                                else ->
                                    "/\\ \\E __io \\in $domain: $v' = [$v EXCEPT ![$k] = __io]"
                            },
                        )
                    } else if (quietMapPut) {
                        if (constGlobal) {
                            val putPrefix = "/\\ $v = "
                            val vv = emitExpr(update.value, putPrefix)
                            addAssignPart(
                                "$putPrefix${emitMapIndexPutRhs(v, k, vv)} \\* global const check",
                            )
                        } else if (indexes(update.collectionVar)) {
                            val putPrefix = "/\\ $v' = [$v EXCEPT ![$self] = "
                            val vv = emitExpr(update.value, putPrefix)
                            addAssignPart("$putPrefix${emitMapIndexPutRhs("@", k, vv)}]")
                        } else {
                            val putPrefix = "/\\ $v' = "
                            val vv = emitExpr(update.value, putPrefix)
                            addAssignPart("$putPrefix${emitMapIndexPutRhs(v, k, vv)}")
                        }
                    } else if (constGlobal) {
                        val putPrefix = "/\\ $v = [$v EXCEPT ![$k] = "
                        val vv = emitExpr(update.value, putPrefix)
                        addAssignPart("/\\ $v = [$v EXCEPT ![$k] = $vv] \\* global const check")
                    } else {
                        val putPrefix = if (indexes(update.collectionVar)) {
                            "/\\ $v' = [$v EXCEPT ![$self] = [@ EXCEPT ![$k] = "
                        } else {
                            "/\\ $v' = [$v EXCEPT ![$k] = "
                        }
                        val vv = emitExpr(update.value, putPrefix)
                        addAssignPart(
                            if (indexes(update.collectionVar)) {
                                "/\\ $v' = [$v EXCEPT ![$self] = [@ EXCEPT ![$k] = $vv]]"
                            } else {
                                "/\\ $v' = [$v EXCEPT ![$k] = $vv]"
                            },
                        )
                    }
                }
            }
        }
        if (offer.isConstructor && offer.leaf.tlaName !in foldedCtorLeaves) {
            val c = stateTlaName(offer.leaf.tlaName, "constructed", stateVarNames)
            targetChanged += c
            addAssignPart(
                if (self != null) {
                    "/\\ $c' = [$c EXCEPT ![$self] = TRUE]"
                } else {
                    "/\\ $c' = TRUE"
                },
            )
        }
        if (offer.decl.isReturn && offer.leaf.tlaName !in foldedCtorLeaves) {
            // A return frees the slot for concurrency but retires it for the TLC run.
            val c = stateTlaName(offer.leaf.tlaName, "constructed", stateVarNames)
            val r = stateTlaName(offer.leaf.tlaName, "retired", stateVarNames)
            targetChanged += c
            targetChanged += r
            addAssignPart(
                if (self != null) {
                    "/\\ $c' = [$c EXCEPT ![$self] = FALSE]"
                } else {
                    "/\\ $c' = FALSE"
                },
            )
            addAssignPart(
                if (self != null) {
                    "/\\ $r' = [$r EXCEPT ![$self] = TRUE]"
                } else {
                    "/\\ $r' = TRUE"
                },
            )
        }
        parentLinkParts(offer).forEach { (part, primed) ->
            if (primed != null) targetChanged += primed
            addAssignPart(part)
        }

        targetParts += prefixParts
        if (bindings.isNotEmpty() && (scopedParts.isNotEmpty() || bindings.any { it.ioDomain != null })) {
            targetParts += wrapTransitLetBlock(bindings, scopedParts)
        } else {
            targetParts += scopedParts
        }
    }

    // Per-offer sections: assumptions (from error:), then comment + gates; keep; then guards + transits.
    offers.forEach { offer ->
        val self = selfOf(offer.leaf)
        val auxNames = auxNamesByLeaf[offer.leaf.tlaName].orEmpty()
        val argNames = offer.decl.action.args.map { it.name }.toSet() + auxNames + bindNames
        val leafCtx = mapOf(offer.leaf.name to offer.leaf, offer.leaf.tlaName to offer.leaf)
        val bare = stateVarsByLeaf[offer.leaf.tlaName].orEmpty()

        if (offer.decl.errors.isNotEmpty()) {
            parts += "\\* ${offer.leaf.name} ${tlaTransitionTypeLabel(offer)} assumption"
            offer.decl.errors.forEach { arm ->
                val assumed = negateErrorCondition(arm.condExpr())
                parts += "/\\ ${exprToTla(
                    assumed, leafCtx, argNames, self,
                    bareStateVars = bare,
                    stateVarNames = stateVarNames,
                    symbolOverrides = exprOverrides,
                    linePrefix = "/\\ ",
                )}"
            }
        }

        parts += "\\* ${offer.leaf.name} ${tlaTransitionTypeLabel(offer)} logic"
        if (offer.leaf.tlaName !in foldedCtorLeaves) {
            val c = stateTlaName(offer.leaf.tlaName, "constructed", stateVarNames)
            val r = stateTlaName(offer.leaf.tlaName, "retired", stateVarNames)
            if (offer.isConstructor) {
                parts += if (self != null) "/\\ ~$c[$self]" else "/\\ ~$c"
                parts += if (self != null) "/\\ ~$r[$self]" else "/\\ ~$r"
            } else {
                parts += if (self != null) "/\\ $c[$self]" else "/\\ $c"
            }
        }

        // Only include args that appear in guards/transits (skip unused initially args).
        offer.decl.action.args.filter { offerRefsArg(offer, it.name) }.forEach { arg ->
            if (arg.name in bindPlan.omitArgTypeDomains()) return@forEach
            if (arg.name in constGlobalArgBinds) return@forEach
            val proj = bindPlan.projectedBind(arg.name)
            val domain = if (proj != null) {
                emitProjectedArgDomain(proj) { emitBound(it) }
            } else {
                argLiteralDomain(offers, arg.name, arg.type) ?: typeToTlaDomain(arg.type)
            }
            argParams += argTla(arg.name) to domain
        }
    }

    bindPlan.structBinds.forEach { b ->
        val elemType = try {
            (b.set.getType() as? SetType)?.elementType
        } catch (_: RuntimeException) {
            null
        }
        b.keep.forEach { (path, expr) ->
            val lhs = emitUnwrappedFieldPath(argTla(b.tmp), elemType, path)
            parts += "/\\ $lhs = ${emitBound(expr)}"
        }
    }

    offers.forEach { offer ->
        val self = selfOf(offer.leaf)
        val auxNames = auxNamesByLeaf[offer.leaf.tlaName].orEmpty()
        val argNames = offer.decl.action.args.map { it.name }.toSet() + auxNames + bindNames
        val leafCtx = mapOf(offer.leaf.name to offer.leaf, offer.leaf.tlaName to offer.leaf)

        offer.decl.guards.forEach { g ->
            // Flatten top-level `&` so each Julay conjunct is its own `/\\` line.
            flattenTopLevelAnd(g).forEach { conjunct ->
                if (bindPlan.skipped(conjunct)) return@forEach
                parts += "/\\ ${exprToTla(
                    conjunct, leafCtx, argNames, self,
                    bareStateVars = stateVarsByLeaf[offer.leaf.tlaName].orEmpty(),
                    stateVarNames = stateVarNames,
                    symbolOverrides = exprOverrides,
                    linePrefix = "/\\ ",
                )}"
            }
        }

        emitTransitUpdates(offer, parts, changed)
    }

    val fluentPlan = TlaFluentEmit.get()
    if (fluentPlan != null) {
        val julayAction = offers.first().decl.action.name
        val indexTla = selfBinders.values.firstOrNull()
        val argTlaByJulay = linkedMapOf<String, String>()
        offers.forEach { offer ->
            offer.decl.action.args.forEach { arg ->
                argTlaByJulay[arg.name] = argTla(arg.name)
            }
        }
        val argTypesByJulay = linkedMapOf<String, julay.program.type.Type>()
        offers.forEach { offer ->
            offer.decl.action.args.forEach { arg ->
                argTypesByJulay[arg.name] = arg.type
            }
        }
        val fluentUpdates = emitFluentUpdatesForAction(
            fluentPlan, julayAction, indexTla, argTlaByJulay, argTypesByJulay,
        )
        parts += fluentUpdates
        changed += fluentsChangedByAction(fluentPlan, julayAction)
    }

    val unchanged = allVars.filter { it !in changed }
    // Guard-only / pure-stutter actions: no primed updates → omit (stuttering via [][Next]_vars).
    if (changed.isEmpty()) {
        TlaSkipConjuncts.clear()
        return null
    }
    if (unchanged.isNotEmpty()) {
        parts += "/\\ UNCHANGED <<${unchanged.joinToString(", ")}>>"
    }
    // After fluent EXCEPT / UNCHANGED so primed fluent cells are defined for TLC.
    if (fluentPlan != null) {
        parts += emitFluentAssumePrime(fluentPlan)
    }

    val indexParams = mutableListOf<Pair<String, String>>()
    selfBinders.forEach { (leafName, binder) ->
        val leaf = offers.firstOrNull { it.leaf.tlaName == leafName }?.leaf
            ?: error("missing leaf $leafName")
        val domain = typeDomainConstant(leaf.paramType!!) ?: leaf.paramType.toString()
        indexParams += binder to domain
        val ib = instBinders[leafName]
        if (leaf.isDualIndexed && ib != null) {
            val instDomain = typeDomainConstant(leaf.instParamType!!) ?: leaf.instParamType.toString()
            indexParams += ib to instDomain
        }
    }
    val indexBinderNames = indexParams.map { it.first }.toSet()

    val letBindings = mutableListOf<Pair<String, String>>()
    // Map-index determined RHSs must sit in a nested LET *after* domain guards so TLC
    // short-circuits `/\` and does not apply `store[k]` when `k \notin DOMAIN store`.
    val mapIndexLets = mutableListOf<Pair<String, String>>()
    bindPlan.listBinds.forEach { b ->
        letBindings += argTla(b.arg) to "${emitBound(b.list)}[${argTla(b.index)}]"
    }
    bindPlan.structBinds.forEach { b ->
        val elemType = try {
            (b.set.getType() as? SetType)?.elementType
        } catch (_: RuntimeException) {
            null
        }
        b.argPaths.forEach { (arg, path) ->
            val tlaArg = argTla(arg)
            val rhs = emitUnwrappedFieldPath(argTla(b.tmp), elemType, path)
            // Index binders stay as action params (for Next `\E n \in …`); do not LET-shadow them.
            if (tlaArg in indexBinderNames) {
                parts += "/\\ $rhs = $tlaArg"
            } else if (letBindings.any { it.first == tlaArg }) {
                // Same arg on multiple struct fields (e.g. src := sender, from := sender).
                parts += "/\\ $rhs = $tlaArg"
            } else {
                letBindings += tlaArg to rhs
            }
        }
    }
    bindPlan.determined.forEach { (arg, expr) ->
        if (arg in constGlobalArgBinds) return@forEach
        val tlaArg = argTla(arg)
        if (tlaArg in indexBinderNames) {
            parts += "/\\ $tlaArg = ${emitBound(expr)}"
        } else if (collectMapIndexesInValue(expr).isNotEmpty()) {
            // Map indexes must be membership-guarded in Julay; still emit as LET for nesting.
            mapIndexLets += tlaArg to emitBound(expr, "      $tlaArg == ")
        } else {
            letBindings += tlaArg to emitBound(expr, "      $tlaArg == ")
        }
    }
    val mapLetNames = mapIndexLets.map { it.first }.toSet()
    fun usesMapLet(part: String): Boolean =
        mapLetNames.any { name ->
            Regex("(?<![A-Za-z0-9_])${Regex.escape(name)}(?![A-Za-z0-9_])").containsMatchIn(part)
        }
    val beforeMapLets = parts.filterNot { usesMapLet(it) }
    val afterMapLets = parts.filter { usesMapLet(it) }
    val afterDomainBody = if (mapIndexLets.isEmpty()) {
        parts.joinToString("\n  ")
    } else {
        val nested = wrapTlaLet(
            mapIndexLets,
            afterMapLets.joinToString("\n  ").ifEmpty { "TRUE" },
        )
        val prefix = beforeMapLets.joinToString("\n  ")
        if (prefix.isEmpty()) nested else "$prefix\n  /\\ $nested"
    }
    val body = wrapTlaLet(letBindings, afterDomainBody)
    // Aux params that are already index binders (same name) are dropped by distinctBy.
    // Determined / struct-bound also-args are omitted from type-domain `\E`, but index
    // binders must stay: set domains like `msgs[n]` still need `\E n \in NodeSet` in Next.
    val omitBinders = bindPlan.omitArgTypeDomains()
    val params = (indexParams + auxParams + argParams)
        .distinctBy { it.first }
        .filter { it.first !in omitBinders || it.first in indexBinderNames }
    val signature = if (params.isEmpty()) {
        name
    } else {
        "$name(${params.joinToString(", ") { it.first }})"
    }
    TlaSkipConjuncts.clear()
    val systemControlled = offers.size >= 2 || offers.any { o ->
        o.decl.modifier == TSAction.SyncRole.Internal ||
            o.decl.modifier == TSAction.SyncRole.Timer ||
            o.decl.isTimer ||
            (o.leaf.name to o.decl.action.name) in handlerResponseActions
    }
    return TlaAction(
        name, "$signature ==\n  $body", params, comment,
        indexParamCount = params.count { p -> indexParams.any { it.first == p.first } },
        systemControlled = systemControlled,
    )
}

/** Prefer [SpecLeaf.paramName] as the TLA binder; suffix `_` / `_2` on name clashes.
 * Leaves that share [SpecLeaf.withScopeId] reuse the same binder string.
 */
internal fun indexBinderName(leaf: SpecLeaf, reserved: Set<String>, sharedBinders: Map<String, String> = emptyMap()): String {
    val base = leaf.paramName!!
    val scope = leaf.withScopeId
    if (scope != null) {
        sharedBinders[scope]?.let { return it }
    }
    return if (base !in reserved) base else firstFreeParamTlaName(base, reserved)
}

/**
 * Map (leafTlaName, julayVarName) → TLA identifier.
 * `constructed` / `retired` are `Leaf_constructed` / `Leaf_retired` unless that leaf's sole
 * unsynced constructor was folded into Init (then both flags are omitted). `killed` only for
 * [killTargets] (by tlaName); other vars are bare unless duplicated or reserved.
 *
 * Future: Julay source may allow composite qualifiers such as `Y.X.n` where `proc Y := X || ...`;
 * not implemented — invariants still write `X.n` and are expanded per occurrence at emit time.
 */
internal fun buildStateVarNames(
    leaves: List<SpecLeaf>,
    pclasses: Map<String, ProcClassNode>,
    reservedIds: Set<String>,
): Map<Pair<String, String>, String> {
    val ownersByVar = linkedMapOf<String, MutableList<String>>()
    leaves.forEach { leaf ->
        val pc = pclasses[leaf.name] ?: return@forEach
        pc.localDecls().filterIsInstance<VarNode>().forEach { vn ->
            ownersByVar.getOrPut(vn.name) { mutableListOf() }.add(leaf.tlaName)
        }
    }
    val out = linkedMapOf<Pair<String, String>, String>()
    leaves.forEach { leaf ->
        val id = leaf.tlaName
        out[id to "constructed"] = "${id}_constructed"
        out[id to "retired"] = "${id}_retired"
        if (parentLinkDomain(leaf, leaves) != null) {
            out[id to "parent"] = "${id}_parent"
        }
        val pc = pclasses[leaf.name] ?: return@forEach
        pc.localDecls().filterIsInstance<VarNode>().forEach { vn ->
            val clash = (ownersByVar[vn.name]?.size ?: 0) > 1 || vn.name in reservedIds
            out[id to vn.name] = if (clash) "${vn.name}_$id" else vn.name
        }
    }
    return out
}

/** Rename state vars projected by `field-vars` to `var_field` (freshen on clash). */
internal fun applyFieldVarNames(
    names: Map<Pair<String, String>, String>,
    leaves: List<SpecLeaf>,
    reservedIds: Set<String>,
): Map<Pair<String, String>, String> {
    val fieldVars = TlaFieldVarProjection.get()
    if (!fieldVars.projecting) return names
    val taken = names.values.toMutableSet()
    taken += reservedIds
    val out = names.toMutableMap()
    leaves.forEach { leaf ->
        fieldVars.projectionsForLeaf(leaf.name).forEach { proj ->
            val key = leaf.tlaName to proj.varName
            if (key !in out) return@forEach
            var candidate = proj.projectedName
            if (candidate in taken && out[key] != candidate) {
                var i = 2
                while ("${candidate}_$i" in taken) i++
                candidate = "${candidate}_$i"
            }
            taken -= out.getValue(key)
            taken += candidate
            out[key] = candidate
        }
    }
    return out
}

private fun effectiveVarType(leaf: SpecLeaf, vn: VarNode): Type =
    TlaFieldVarProjection.get().effectiveType(leaf.name, vn.name, safeType(vn))

/**
 * TLA domain of `<Child>_parent` for a `B<A>` leaf, or null when no link var is needed
 * (parent is a single static occurrence, or absent).
 * One dynamic parent occurrence → its `DynProc` slot; several occurrences → `<<occ, slot>>`.
 */
internal fun parentLinkDomain(leaf: SpecLeaf, leaves: List<SpecLeaf>): String? {
    val parents = parentOccurrences(leaf, leaves)
    if (parents.isEmpty()) return null
    if (parents.size == 1) return if (parents[0].isDynamic) DYN_PROC_MODEL else null
    return parents.joinToString(" \\cup ") { p ->
        if (p.isDynamic) "({\"${p.tlaName}\"} \\X $DYN_PROC_MODEL)" else "{<<\"${p.tlaName}\", 0>>}"
    }
}

internal fun parentOccurrences(leaf: SpecLeaf, leaves: List<SpecLeaf>): List<SpecLeaf> {
    val parent = leaf.parentClass ?: return emptyList()
    return leaves.filter { it.name == parent && it !== leaf }
}

/** Slot path of a dynamic leaf inside an action: the `DynProc` binder. */
internal fun parentLinkValue(parent: SpecLeaf, child: SpecLeaf, leaves: List<SpecLeaf>, slot: String?): String {
    val multi = parentOccurrences(child, leaves).size > 1
    val s = slot ?: "0"
    return if (multi) "<<\"${parent.tlaName}\", $s>>" else s
}

internal fun stateTlaName(
    leaf: String,
    varName: String,
    names: Map<Pair<String, String>, String>,
): String = names[leaf to varName] ?: tlaVar(leaf, varName)

/** One Init conjunct. Do not prefix every pretty-printed line with `/\` — that splits `\A x \in S :` from its body. */
private fun addInitConjunct(initParts: MutableList<String>, body: String) {
    val lines = body.lineSequence().map { it.trimEnd() }.filter { it.isNotBlank() }.toList()
    if (lines.isEmpty()) return
    val first = lines.first().trimStart()
    val head = if (first.startsWith("/\\") || first.startsWith("\\/")) first else "/\\ $first"
    if (lines.size == 1) {
        initParts += head
        return
    }
    initParts += buildString {
        append(head)
        lines.drop(1).forEach { line ->
            append('\n')
            append("     ")
            append(line.trimStart())
        }
    }
}

private fun emitInitConstraintParts(
    leaves: List<SpecLeaf>,
    stateVarNames: Map<Pair<String, String>, String>,
    constants: Set<String>,
    initParts: MutableList<String>,
    skipExprs: Set<ExprNode> = emptySet(),
) {
    val withInits = leaves.filter { leaf ->
        leaf.initExprs.any { it !in skipExprs }
    }
    if (withInits.isEmpty()) return
    initParts += "\\* init constraints"
    // Create-index / composition-block inits are copied onto every child leaf; emit once.
    // Use all leaves in leafCtx and the union of frozen consts so bare names resolve
    // even when the first leaf that carries the ExprNode is not the owner (e.g. Store
    // vs ClusterInfo.cluster).
    val fullCtx = linkedMapOf<String, SpecLeaf>()
    leaves.forEach { leaf ->
        fullCtx[leaf.name] = leaf
        fullCtx[leaf.tlaName] = leaf
    }
    val allBareConsts = leaves.flatMap { it.globalConstVars }.toSet()
    val seen = mutableSetOf<ExprNode>()
    withInits.forEach { leaf ->
        leaf.initExprs.forEach { expr ->
            if (expr in skipExprs || !seen.add(expr)) return@forEach
            val body = exprToTla(
                expr,
                leafCtx = fullCtx,
                argNames = emptySet(),
                self = null,
                bareStateVars = allBareConsts,
                reservedNames = constants,
                stateVarNames = stateVarNames,
                linePrefix = "",
                parentPrec = PREC_BOTTOM,
                globalByLeaf = globalVarsByLeaf(leaves),
            )
            addInitConjunct(initParts, body)
        }
    }
}

/** Drop CONSTANT names that appear only in singleton-consumed `init:` (e.g. `exists node : Node`). */
private fun dropConsumedOnlyTypeConstants(
    leaves: List<SpecLeaf>,
    invClosure: List<InvariantNode>,
    consumedInitExprs: Set<ExprNode>,
    constants: MutableSet<String>,
) {
    if (consumedInitExprs.isEmpty()) return
    val fromConsumed = linkedSetOf<String>()
    consumedInitExprs.forEach { collectTypeConstants(it, fromConsumed) }
    val fromLive = linkedSetOf<String>()
    invClosure.forEach { collectTypeConstants(it.invariantFormula(), fromLive) }
    leaves.forEach { leaf ->
        leaf.initExprs.forEach { expr ->
            if (expr !in consumedInitExprs) collectTypeConstants(expr, fromLive)
        }
    }
    // Opaque index sorts stay as CONSTANTs (projection Init / TypeOK / binders use them
    // even when the only `init:` mention was a consumed url-identity forall).
    fromLive += TlaOpaqueIndexSort.sorts().keys
    constants.removeAll(fromConsumed - fromLive)
}

/** Last `x := arg` assign in a constructor (const-global binds handled separately). */
private fun ctorArgStateAssigns(offer: TlaActionOffer): Map<String, String> {
    val argNames = offer.decl.action.args.map { it.name }.toSet()
    val out = linkedMapOf<String, String>()
    offer.decl.transits.forEach { update ->
        if (update !is TransitUpdate.Assign) return@forEach
        if (!isActionArgSymbol(update.expr, argNames)) return@forEach
        val arg = (unwrapParens(update.expr) as SymbolValueExprNode).symbol
        out[arg] = update.transitRootVar()
    }
    return out
}

private fun lastCtorAssign(offer: TlaActionOffer, varName: String): TransitUpdate.Assign? =
    offer.decl.transits.filterIsInstance<TransitUpdate.Assign>().lastOrNull {
        it.transitRootVar() == varName
    }

/**
 * Transit lets whose init (or a prior let it references) contains IO / unsupported
 * builtins — omitted from [ctorSymbolOverrides] so fold cannot splice `TRUE.field`.
 */
private fun ctorLetsWithHavocInit(offer: TlaActionOffer): Set<String> {
    val argNames = offer.decl.action.args.map { it.name }.toSet()
    val havoc = linkedSetOf<String>()
    offer.decl.transits.forEach { update ->
        if (update !is TransitUpdate.Let || update.name.isDiscardBinding()) return@forEach
        val refsHavocLet = havoc.any { exprReferencesSymbol(update.init, it) }
        if (refsHavocLet ||
            exprContainsIoHavoc(update.init) ||
            exprContainsUnsupportedTlaBuiltin(update.init) ||
            argNames.any { exprReferencesSymbol(update.init, it) }
        ) {
            havoc += update.name
        }
    }
    return havoc
}

/** True when a folded Init assign must use `\in` domain (IO / unsupported / args / havoc lets). */
internal fun foldedCtorAssignNeedsDomainInit(
    offer: TlaActionOffer,
    assign: TransitUpdate.Assign,
): Boolean {
    val argNames = offer.decl.action.args.map { it.name }.toSet()
    if (exprContainsIoHavoc(assign.expr) ||
        exprContainsUnsupportedTlaBuiltin(assign.expr) ||
        isActionArgSymbol(assign.expr, argNames)
    ) {
        return true
    }
    val havocLets = ctorLetsWithHavocInit(offer)
    return havocLets.any { exprReferencesSymbol(assign.expr, it) }
}

private fun ctorSymbolOverrides(
    leaf: SpecLeaf,
    offer: TlaActionOffer,
    binder: String?,
    stateVarNames: Map<Pair<String, String>, String>,
    constants: Set<String>,
    stateVarsByLeaf: Set<String>,
    leaves: List<SpecLeaf>,
): Map<String, String> {
    val leafCtx = mapOf(leaf.name to leaf, leaf.tlaName to leaf)
    val argNames = offer.decl.action.args.map { it.name }.toSet()
    val constBinds = collectGlobalConstArgBinds(listOf(offer), stateVarNames)
    val overrides = linkedMapOf<String, String>()
    overrides.putAll(constBinds)
    ctorArgStateAssigns(offer).forEach { (arg, varName) ->
        if (arg in overrides) return@forEach
        val v = stateTlaName(leaf.tlaName, varName, stateVarNames)
        overrides[arg] = if (binder != null && leaf.indexesState(varName)) "$v[$binder]" else v
    }
    val skipLets = ctorLetsWithHavocInit(offer)
    offer.decl.transits.forEach { update ->
        if (update !is TransitUpdate.Let || update.name.isDiscardBinding()) return@forEach
        if (update.name in skipLets) return@forEach
        val rhs = exprToTla(
            update.init,
            leafCtx = leafCtx,
            argNames = argNames,
            self = binder,
            bareStateVars = stateVarsByLeaf,
            reservedNames = constants,
            stateVarNames = stateVarNames,
            symbolOverrides = overrides,
            linePrefix = "",
            globalByLeaf = globalVarsByLeaf(leaves),
        )
        overrides[update.name] = rhs
    }
    return overrides
}

private fun foldedCtorVarInit(
    leaf: SpecLeaf,
    vn: VarNode,
    offer: TlaActionOffer,
    binder: String?,
    domain: String?,
    stateVarNames: Map<Pair<String, String>, String>,
    constants: Set<String>,
    cfgOverrides: MutableSet<String>,
    stateVarsByLeaf: Set<String>,
    leaves: List<SpecLeaf>,
): String? {
    val assign = lastCtorAssign(offer, vn.name) ?: return null
    val v = stateTlaName(leaf.tlaName, vn.name, stateVarNames)
    val argNames = offer.decl.action.args.map { it.name }.toSet()
    val constGlobal = vn.name in leaf.globalConstVars
    if (constGlobal) {
        return null
    }
    if (foldedCtorAssignNeedsDomainInit(offer, assign)) {
        val effTy = effectiveVarType(leaf, vn)
        collectDomainModelNames(effTy, cfgOverrides)
        val ty = typeToTlaDomain(effTy)
        return if (binder != null && leaf.indexesState(vn.name)) {
            "/\\ $v \\in [$domain -> $ty]"
        } else {
            "/\\ $v \\in $ty"
        }
    }
    val leafCtx = mapOf(leaf.name to leaf, leaf.tlaName to leaf)
    val overrides = ctorSymbolOverrides(
        leaf, offer, binder, stateVarNames, constants, stateVarsByLeaf, leaves,
    )
    val indexed = binder != null && leaf.indexesState(vn.name)
    val assignPrefix = if (indexed) {
        "/\\ $v = [$binder \\in $domain |-> "
    } else {
        "/\\ $v = "
    }
    val rhs = exprToTla(
        assign.expr,
        leafCtx = leafCtx,
        argNames = argNames,
        self = binder,
        bareStateVars = stateVarsByLeaf,
        reservedNames = constants,
        stateVarNames = stateVarNames,
        symbolOverrides = overrides,
        linePrefix = assignPrefix,
        globalByLeaf = globalVarsByLeaf(leaves),
    )
    return if (indexed) {
        "/\\ $v = [$binder \\in $domain |-> $rhs]"
    } else {
        "/\\ $v = $rhs"
    }
}

private fun emitFoldedCtorAssumptions(
    leaf: SpecLeaf,
    offer: TlaActionOffer,
    binder: String?,
    domain: String?,
    stateVarNames: Map<Pair<String, String>, String>,
    constants: Set<String>,
    cfgOverrides: MutableSet<String>,
    stateVarsByLeaf: Set<String>,
    leaves: List<SpecLeaf>,
    initParts: MutableList<String>,
) {
    val argNames = offer.decl.action.args.map { it.name }.toSet()
    val constChecks = offer.decl.transits.filterIsInstance<TransitUpdate.Assign>().filter { update ->
        update.transitRootVar() in leaf.globalConstVars &&
            TlaVarProjection.get().isRelevant(leaf.name, update.transitRootVar()) &&
            !isActionArgSymbol(update.expr, argNames)
    }
    val leafCtx = mapOf(leaf.name to leaf, leaf.tlaName to leaf)
    val overrides = ctorSymbolOverrides(
        leaf, offer, binder, stateVarNames, constants, stateVarsByLeaf, leaves,
    )
    val argAssigns = ctorArgStateAssigns(offer)
    val argTypes = offer.decl.action.args.associate { it.name to it.type }
    val takenNames = (
        stateVarsByLeaf +
            constants +
            stateVarNames.values +
            overrides.keys +
            overrides.values.map { it.substringBefore('[') }
        ).toMutableSet()
    if (binder != null) takenNames += binder
    val errorFormulas = mutableListOf<String>()
    offer.decl.errors.forEach { arm ->
        val assumed = negateErrorCondition(arm.condExpr())
        val existsArgs = argTypes.keys.filter { arg ->
            arg !in overrides && exprReferencesSymbol(assumed, arg)
        }
        // `\E args : P(args)` with no state does not constrain Init — drop it.
        if (foldedCtorAssumptionIsArgsOnly(assumed, existsArgs, stateVarsByLeaf, overrides.keys)) {
            return@forEach
        }
        val quantifiedIndexed = binder != null && domain != null && argAssigns.any { (arg, varName) ->
            leaf.indexesState(varName) && exprReferencesSymbol(assumed, arg)
        }
        existsArgs.forEach { arg ->
            argTypes[arg]?.let { collectDomainModelNames(it, cfgOverrides) }
        }
        // Freshen `\E` binders that collide with VARIABLES / CONSTANTS / overrides.
        val existsArgTla = linkedMapOf<String, String>()
        existsArgs.forEach { arg ->
            val tlaArg = firstFreeParamTlaName(arg, takenNames)
            existsArgTla[arg] = tlaArg
            takenNames += tlaArg
        }
        val body = exprToTla(
            assumed,
            leafCtx = leafCtx,
            argNames = existsArgs.toSet(),
            self = binder,
            bareStateVars = stateVarsByLeaf,
            reservedNames = constants,
            stateVarNames = stateVarNames,
            symbolOverrides = overrides + existsArgTla,
            linePrefix = "/\\ ",
            globalByLeaf = globalVarsByLeaf(leaves),
        )
        val existsWrap = existsArgs.reversed().fold(body) { inner, arg ->
            val ty = argTypes[arg]?.let { typeToTlaDomain(it) } ?: "Int"
            val tlaArg = existsArgTla[arg] ?: arg
            "\\E $tlaArg \\in $ty : $inner"
        }
        errorFormulas += if (quantifiedIndexed) {
            "\\A $binder \\in $domain : $existsWrap"
        } else {
            existsWrap
        }
    }
    val liveConstChecks = constChecks.filter { update ->
        // Unsupported/IO RHS cannot constrain Init; create-index `init:` / singleton handles it.
        !foldedCtorAssignNeedsDomainInit(offer, update)
    }
    if (liveConstChecks.isEmpty() && errorFormulas.isEmpty()) return
    initParts += "\\* ${leaf.name} constructor assumption"
    liveConstChecks.forEach { update ->
        val v = stateTlaName(leaf.tlaName, update.transitRootVar(), stateVarNames)
        val rhs = exprToTla(
            update.expr,
            leafCtx = leafCtx,
            argNames = argNames,
            self = binder,
            bareStateVars = stateVarsByLeaf,
            reservedNames = constants,
            stateVarNames = stateVarNames,
            symbolOverrides = overrides,
            linePrefix = "/\\ $v = ",
            globalByLeaf = globalVarsByLeaf(leaves),
        )
        initParts += "/\\ $v = ${parenIfBracketExpr(rhs)} \\* global const check"
    }
    errorFormulas.forEach { initParts += "/\\ $it" }
}

/** Wrap TLA function/record literals so they are safe after `/\\` in a junction. */
private fun parenIfBracketExpr(tla: String): String {
    val trimmed = tla.trimStart()
    return if (trimmed.startsWith("[")) "($trimmed)" else tla
}

/**
 * True when a folded constructor `error:` assumption only mentions unbound ctor args
 * (no process state / arg→state binds). Such `\E args : P(args)` does not constrain Init.
 */
internal fun foldedCtorAssumptionIsArgsOnly(
    assumed: ExprNode,
    existsArgs: Collection<String>,
    stateVars: Set<String>,
    overriddenArgs: Set<String>,
): Boolean {
    if (existsArgs.isEmpty()) return false
    if (stateVars.any { exprReferencesSymbol(assumed, it) }) return false
    if (overriddenArgs.any { exprReferencesSymbol(assumed, it) }) return false
    return existsArgs.any { exprReferencesSymbol(assumed, it) }
}

internal fun collectSortLengthConstants(
    expr: ExprNode,
    sortNames: Set<String>,
    into: MutableSet<String>,
) {
    fun walk(e: ExprNode) {
        when (e) {
            is FieldAccessExprNode -> {
                if (e.fieldPath.lastOrNull() == "length" && e.baseSymbol in sortNames) {
                    into += e.baseSymbol
                }
            }
            is FunCallExprNode -> {
                if (e.callName() == "length") {
                    val arg = e.callArgs().singleOrNull()
                    if (arg is SymbolValueExprNode && arg.symbol in sortNames) {
                        into += arg.symbol
                    }
                }
                e.callArgs().forEach { walk(it) }
                return
            }
            else -> {}
        }
        e.children.filterIsInstance<ExprNode>().forEach { walk(it) }
    }
    walk(expr)
}

internal fun globalVarsByLeaf(leaves: List<SpecLeaf>): Map<String, Set<String>> {
    val out = mutableMapOf<String, Set<String>>()
    leaves.forEach { leaf ->
        if (leaf.globalVars.isEmpty()) return@forEach
        out[leaf.name] = leaf.globalVars
        out[leaf.tlaName] = leaf.globalVars
    }
    return out
}

internal fun tlaVar(leaf: String, varName: String): String = "${varName}_$leaf"

/** Empty/zero default for Init holes and TypeOK (opaque sorts use sentinel atoms). */
internal fun defaultTlaValue(
    type: Type,
    leafClass: String? = null,
    varName: String? = null,
    fieldOfObj: String? = null,
    fieldName: String? = null,
): String {
    if (type is StringType || type is IntType) {
        val lits = when {
            leafClass != null && varName != null ->
                TlaLiteralDomainProjection.get().varSet(leafClass, varName)
            fieldOfObj != null && fieldName != null ->
                TlaLiteralDomainProjection.get().objFieldSet(fieldOfObj, fieldName)
            else -> null
        }
        if (lits != null && lits.isNotEmpty()) return lits.sorted().first()
    }
    return when (type) {
    is BoolType -> "FALSE"
    is IntType -> "0"
    is RealType -> "0"
    is StringType -> "\"\""
    is DomainType -> type.cfgElements?.firstOrNull() ?: defaultTlaValue(type.carrierType)
    // Empty Int-> function (Apalache); body is never evaluated.
    is ListType -> "[__j \\in {} |-> ${defaultTlaValue(type.elementType, fieldOfObj, fieldName, leafClass, varName)}]"
    is SetType -> "{}"
    // Binder must not collide with a state VARIABLE named `x` (SANY multiply-defined).
    is MapType ->
        "[__k \\in {} |-> ${defaultTlaValue(type.valueType, fieldOfObj, fieldName, leafClass, varName)}]"
    is ObjClassType -> {
        if (TlaOpaqueIndexSort.isOpaque(type)) {
            // Empty/default opaque value is the sentinel outside the live create-index set.
            TlaOpaqueIndexSort.sentinelTla(type.name)
        } else {
            val single = TlaFieldProjection.get().singletonField(type)
            if (single != null) {
                defaultTlaValue(single.type, fieldOfObj = type.name, fieldName = single.name)
            } else {
                val fields = TlaFieldProjection.get().fieldsFor(type)
                if (fields.isEmpty()) {
                    "[dummy |-> 0]"
                } else {
                    val rendered = fields.joinToString(", ") { f ->
                        "${f.name} |-> ${defaultTlaValue(f.type, fieldOfObj = type.name, fieldName = f.name)}"
                    }
                    "[$rendered]"
                }
            }
        }
    }
    else -> "0"
    }
}

/**
 * Dynamic leaf field reads in comparisons imply the instance is live:
 * `Put[n][d].step = "done"` → `Put_constructed[n][d] /\ step_Put[n][d] = "done"`.
 */
private fun collectDynConstructedGuards(
    expr: ExprNode,
    leafCtx: Map<String, SpecLeaf>,
    stateVarNames: Map<Pair<String, String>, String>,
    renderIdx: (ExprNode) -> String,
): List<String> {
    val out = linkedSetOf<String>()
    fun walk(e: ExprNode) {
        when (e) {
            is MemberAccessExprNode -> {
                val base = e.baseExpr
                when {
                    base is IndexExprNode && base.base is IndexExprNode &&
                        (base.base as IndexExprNode).base is SymbolValueExprNode -> {
                        val outer = base.base as IndexExprNode
                        val leafName = (outer.base as SymbolValueExprNode).symbol
                        val peer = leafCtx[leafName] ?: return
                        if (!peer.isDynamic) return
                        val c = stateVarNames[leafName to "constructed"]
                            ?: stateVarNames[peer.tlaName to "constructed"]
                            ?: return
                        out += "$c[${renderIdx(outer.index)}][${renderIdx(base.index)}]"
                    }
                    base is IndexExprNode && base.base is SymbolValueExprNode -> {
                        val leafName = (base.base as SymbolValueExprNode).symbol
                        val peer = leafCtx[leafName] ?: return
                        if (!peer.isDynamic) return
                        val c = stateVarNames[leafName to "constructed"]
                            ?: stateVarNames[peer.tlaName to "constructed"]
                            ?: return
                        out += "$c[${renderIdx(base.index)}]"
                    }
                    else -> e.children.filterIsInstance<ExprNode>().forEach { walk(it) }
                }
            }
            else -> e.children.filterIsInstance<ExprNode>().forEach { walk(it) }
        }
    }
    walk(expr)
    return out.toList()
}

private fun wrapWithDynConstructedGuards(
    core: String,
    guards: List<String>,
    parentPrec: Int,
): String {
    if (guards.isEmpty()) return core
    val conj = (guards + listOf(core)).joinToString(" /\\ ")
    return if (PREC_AND < parentPrec) "($conj)" else conj
}

/** Julay leaf class names used as `Leaf.var` / `Leaf[i].var` / `Leaf[n][i].var` in an invariant. */
internal fun collectInvariantLeafBases(expr: ExprNode): Set<String> {
    val out = linkedSetOf<String>()
    fun peerSymbol(e: ExprNode): String? = when (e) {
        is SymbolValueExprNode -> e.symbol
        is IndexExprNode -> peerSymbol(e.base)
        else -> null
    }
    fun walk(e: ExprNode) {
        when (e) {
            is FieldAccessExprNode -> {
                // Leaf.var (single field) — not object field chains on args.
                if (e.fieldPath.size == 1) out += e.baseSymbol
            }
            is MemberAccessExprNode -> {
                peerSymbol(e.baseExpr)?.let { out += it }
            }
            else -> Unit
        }
        e.children.filterIsInstance<ExprNode>().forEach { walk(it) }
    }
    walk(expr)
    return out
}

/**
 * Emit invariant operators. Julay source keeps `X.n`; when `X` has multiple occurrences,
 * emit one operator per occurrence (named like leaf renames: `Bound_P`, `Bound_Q`, …).
 *
 * Future: composite qualifiers such as `Y.X.n` where `proc Y := X || ...` — not implemented.
 */
private fun emitInvariantDefs(
    node: InvariantNode,
    leaves: List<SpecLeaf>,
    stateVarNames: Map<Pair<String, String>, String>,
    constants: Set<String>,
): List<String> {
    val bases = collectInvariantLeafBases(node.invariantFormula())
    val byClass = leaves.groupBy { it.name }
    val multiClasses = bases.filter { (byClass[it]?.size ?: 0) > 1 }
    val fullCtx = linkedMapOf<String, SpecLeaf>()
    leaves.forEach { leaf ->
        fullCtx[leaf.name] = leaf
        fullCtx[leaf.tlaName] = leaf
    }
    fun formatInv(name: String, remapped: Map<Pair<String, String>, String>): String {
        // `mp[k] = v` → `k in mp & mp[k] = v` so TLA application is domain-guarded.
        val formula = withMapIndexDomainGuards(node.invariantFormula())
        val body = exprToTla(
            formula,
            leafCtx = fullCtx,
            argNames = emptySet(),
            self = null,
            bareStateVars = emptySet(),
            reservedNames = constants,
            stateVarNames = remapped,
            linePrefix = "",
            parentPrec = PREC_BOTTOM,
            globalByLeaf = globalVarsByLeaf(leaves),
        )
        return if (body.contains('\n') || isMultiLineExpr(formula)) {
            val indented = body.lineSequence().joinToString("\n") { "  $it" }
            "$name ==\n$indented"
        } else {
            "$name == $body"
        }
    }
    if (multiClasses.isEmpty()) {
        return listOf(formatInv(node.name(), stateVarNames))
    }
    var bindings: List<Map<String, SpecLeaf>> = listOf(emptyMap())
    for (cls in multiClasses) {
        val occs = byClass.getValue(cls)
        bindings = bindings.flatMap { partial -> occs.map { partial + (cls to it) } }
    }
    val preferred = bindings.map { binding ->
        val suffix = binding.values.joinToString("_") { it.introducingAssembly }
        "${node.name()}_$suffix" to binding
    }
    val preferredCounts = preferred.groupingBy { it.first }.eachCount()
    val seq = mutableMapOf<String, Int>()
    val used = mutableSetOf<String>()
    return preferred.map { (base, binding) ->
        val invName = if (preferredCounts.getValue(base) == 1 && base !in used) {
            base
        } else {
            val n = (seq[base] ?: 0) + 1
            seq[base] = n
            "${base}_$n"
        }
        used += invName
        val remapped = stateVarNames.toMutableMap()
        for ((cls, leaf) in binding) {
            stateVarNames.forEach { (key, tlaId) ->
                if (key.first == leaf.tlaName) {
                    remapped[cls to key.second] = tlaId
                }
            }
        }
        formatInv(invName, remapped)
    }
}

internal fun typeDomainConstant(typeExpr: TypeExpr): String? = when (typeExpr) {
    is TypeExpr.Simple -> typeExpr.name
    is TypeExpr.Parametric -> null
    is TypeExpr.Tuple -> null
}

internal fun cfgConstantModel(name: String): String = when (name) {
    "Boolean" -> "BOOLEAN"
    "MaxListLen" -> "$DEFAULT_MAX_LIST_LEN"
    else -> error("TLA+ cfg: no model for CONSTANT $name")
}

internal fun cfgIntModel(values: Set<Int>, maxListLen: Int = DEFAULT_MAX_LIST_LEN): String {
    val nums = values.filter { it >= 0 }
    val hi = maxOf(nums.maxOrNull() ?: 0, maxListLen)
    return "{${(0..hi).joinToString(", ")}}"
}

/** Escape content for a TLA+/TLC double-quoted string (`.tla` and `.cfg`). */
internal fun escapeTlaStringLiteral(s: String): String =
    buildString {
        for (ch in s) {
            when (ch) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(ch)
            }
        }
    }

internal fun cfgStringModel(
    literals: Set<String>,
    intValues: Set<Int>,
    coerceIntToString: Boolean,
): String {
    val strs = linkedSetOf<String>()
    // TLC .cfg does not accept escape sequences in strings; drop control chars from the model.
    literals.filter { s -> s.all { ch -> ch >= ' ' && ch != '"' && ch != '\\' } }
        .forEach { strs += it }
    if (coerceIntToString) {
        intValues.filter { it >= 0 }.forEach { strs += it.toString() }
    }
    if (strs.isEmpty()) strs += ""
    return "{${strs.sorted().joinToString(", ") { "\"$it\"" }}}"
}

/** Strip a TLA+ cfg string element (`"a"` / `"a\"b"`) to the raw Julang/Java string. */
internal fun rawStringFromCfgElement(elem: String): String? {
    if (elem.length < 2 || !elem.startsWith("\"") || !elem.endsWith("\"")) return null
    val body = elem.substring(1, elem.length - 1)
    return buildString {
        var i = 0
        while (i < body.length) {
            val ch = body[i]
            if (ch == '\\' && i + 1 < body.length) {
                when (body[i + 1]) {
                    'n' -> append('\n')
                    't' -> append('\t')
                    'r' -> append('\r')
                    '"' -> append('"')
                    '\\' -> append('\\')
                    else -> append(body[i + 1])
                }
                i += 2
            } else {
                append(ch)
                i++
            }
        }
    }
}

/** Union String-carrier domain models (e.g. Key, NodeSet) into the cfg String universe. */
internal fun addStringCarrierDomainElements(
    domains: Map<String, DomainType>,
    into: MutableSet<String>,
) {
    domains.values.forEach { d ->
        if (d.carrierType !is StringType) return@forEach
        d.cfgElements?.forEach { elem ->
            rawStringFromCfgElement(elem)?.let { into += it }
        }
    }
}

/**
 * Opaque index-sort delayed-model atoms (e.g. Node = {"n1","n2"}) are TLC model values
 * that also appear as String when url projections are pinned to identity.
 */
internal fun addOpaqueIndexSortStringAtoms(
    opaqueAtoms: Map<String, List<String>>,
    into: MutableSet<String>,
) {
    opaqueAtoms.values.forEach { atoms ->
        atoms.forEach { tok ->
            rawStringFromCfgElement(tok)?.let { into += it }
        }
    }
}

/**
 * Union carrier literals assigned (or packed into obj fields) at DomainType sites into the
 * delayed-model CONSTANT used by TypeOK / action `\E`. Otherwise e.g. `value := ""` with
 * `Value := {"v1","v2"}` yields a TypeOK violation on the successor.
 */
internal fun widenDomainModelsFromAssignedLiterals(
    domains: Map<String, DomainType>,
    cfgDomainModels: MutableMap<String, String>,
    leaves: List<SpecLeaf>,
    pclasses: Map<String, ProcClassNode>,
    emittedOffers: List<TlaActionOffer>,
    stringModelValues: MutableSet<String>,
) {
    val extras = linkedMapOf<String, LinkedHashSet<String>>()

    fun noteCfgToken(domain: DomainType, cfgTok: String) {
        val base = domain.cfgElements ?: return
        if (cfgTok in base) return
        if (cfgTok in (extras[domain.name] ?: emptySet())) return
        extras.getOrPut(domain.name) { linkedSetOf() }.add(cfgTok)
    }

    fun noteLiteral(type: Type, expr: ExprNode) {
        val domain = type as? DomainType ?: return
        if (!domain.hasModel || domain.name !in cfgDomainModels) return
        val lit = unwrapParens(expr)
        when {
            lit is LiteralValueExprNode -> {
                val litTy = lit.inferType(emptyMap())
                val tok = when {
                    domain.carrierType is StringType && litTy is StringType ->
                        "\"${escapeTlaStringLiteral(lit.literalText())}\""
                    domain.carrierType is IntType && (litTy is IntType || litTy is RealType) ->
                        lit.literalText()
                    domain.carrierType is BoolType && litTy is BoolType ->
                        lit.literalText().uppercase()
                    else -> return
                }
                noteCfgToken(domain, tok)
            }
            lit is UnaryOpExprNode && lit.op() == "-" &&
                domain.carrierType is IntType &&
                unwrapParens(lit.operand()) is LiteralValueExprNode -> {
                val n = (unwrapParens(lit.operand()) as LiteralValueExprNode).literalText()
                noteCfgToken(domain, "-$n")
            }
            lit is IfElseExprNode -> {
                noteLiteral(type, lit.thenExpr())
                noteLiteral(type, lit.elseExpr())
            }
            lit is WhenExprNode -> {
                lit.arms().forEach { arm ->
                    when (arm) {
                        is WhenArm.Subject -> noteLiteral(type, arm.expr)
                        is WhenArm.Guard -> noteLiteral(type, arm.expr)
                        is WhenArm.Else -> noteLiteral(type, arm.expr)
                    }
                }
            }
            else -> {}
        }
    }

    fun walkObjFields(expr: ExprNode) {
        when (val e = unwrapParens(expr)) {
            is ObjClassLiteralExprNode -> {
                val objType = try {
                    e.structType
                } catch (_: RuntimeException) {
                    null
                }
                if (objType is ObjClassType) {
                    e.fieldEntries.forEach { (fname, value) ->
                        val ft = objType.fields.firstOrNull { it.name == fname }?.type
                        if (ft != null) {
                            noteLiteral(ft, value)
                            walkObjFields(value)
                        }
                    }
                }
            }
            is IfElseExprNode -> {
                walkObjFields(e.thenExpr())
                walkObjFields(e.elseExpr())
                walkObjFields(e.condExpr())
            }
            is WhenExprNode -> {
                e.arms().forEach { arm ->
                    when (arm) {
                        is WhenArm.Subject -> walkObjFields(arm.expr)
                        is WhenArm.Guard -> {
                            walkObjFields(arm.cond)
                            walkObjFields(arm.expr)
                        }
                        is WhenArm.Else -> walkObjFields(arm.expr)
                    }
                }
            }
            else -> e.children.filterIsInstance<ExprNode>().forEach { walkObjFields(it) }
        }
    }

    fun leafVarType(leaf: SpecLeaf, name: String): Type? {
        val pc = pclasses[leaf.name] ?: return null
        val vn = pc.localDecls().filterIsInstance<VarNode>().find { it.name == name } ?: return null
        return effectiveVarType(leaf, vn)
    }

    emittedOffers.forEach { offer ->
        offer.decl.transits.forEach { update ->
            when (update) {
                is TransitUpdate.Assign -> {
                    val ty = leafVarType(offer.leaf, update.transitRootVar())
                    if (ty != null) noteLiteral(ty, update.expr)
                    walkObjFields(update.expr)
                }
                is TransitUpdate.IndexPut -> {
                    val ty = leafVarType(offer.leaf, update.collectionVar)
                    when (ty) {
                        is MapType -> {
                            noteLiteral(ty.keyType, update.index)
                            noteLiteral(ty.valueType, update.value)
                        }
                        is ListType -> noteLiteral(ty.elementType, update.value)
                        else -> {}
                    }
                    walkObjFields(update.index)
                    walkObjFields(update.value)
                }
                is TransitUpdate.Let -> {
                    noteLiteral(update.type, update.init)
                    walkObjFields(update.init)
                }
            }
        }
        offer.decl.guards.forEach { walkObjFields(it) }
    }
    leaves.forEach { leaf ->
        val pc = pclasses[leaf.name] ?: return@forEach
        pc.localDecls().filterIsInstance<VarNode>().forEach { vn ->
            val init = vn.initExpr ?: return@forEach
            noteLiteral(effectiveVarType(leaf, vn), init)
            walkObjFields(init)
        }
    }

    if (extras.isEmpty()) return
    extras.forEach { (name, toks) ->
        val domain = domains[name] ?: return@forEach
        val merged = linkedSetOf<String>()
        domain.cfgElements?.let { merged.addAll(it) }
        merged.addAll(toks)
        cfgDomainModels[name] = "{${merged.joinToString(", ")}}"
        if (domain.carrierType is StringType) {
            toks.forEach { tok -> rawStringFromCfgElement(tok)?.let { stringModelValues += it } }
        }
    }
}

/** DynProc create-index models are string literals; include them in cfg String when present. */
private fun addDynProcStringElements(leaves: List<SpecLeaf>, into: MutableSet<String>) {
    leaves.mapNotNull { it.dynProcModel }.distinctBy { it.name() }.forEach { model ->
        model.elements.forEach { lit ->
            if (lit.inferType(emptyMap()) is StringType) into += lit.literalText()
        }
    }
}

private fun collectCfgLiteralsFromOffer(
    offer: TlaActionOffer,
    ints: MutableSet<Int>,
    strs: MutableSet<String>,
) {
    offer.decl.guards.forEach { collectCfgLiteralsFromExpr(it, ints, strs) }
    offer.decl.transits.forEach { update ->
        when (update) {
            is TransitUpdate.Let -> collectCfgLiteralsFromExpr(update.init, ints, strs)
            is TransitUpdate.Assign -> {
                if (TlaVarProjection.get().isRelevant(offer.leaf.name, update.transitRootVar())) {
                    collectCfgLiteralsFromExpr(update.expr, ints, strs)
                }
            }
            is TransitUpdate.IndexPut -> {
                if (TlaVarProjection.get().isRelevant(offer.leaf.name, update.transitRootVar())) {
                    collectCfgLiteralsFromExpr(update.index, ints, strs)
                    collectCfgLiteralsFromExpr(update.value, ints, strs)
                }
            }
        }
    }
}

private fun collectCfgLiteralsFromExpr(expr: ExprNode, ints: MutableSet<Int>, strs: MutableSet<String>) {
    collectIntLiteralsFromExpr(expr, ints)
    collectStringLiteralsFromExpr(expr, strs)
}

private fun collectStringLiteralsFromExpr(expr: ExprNode, into: MutableSet<String>) {
    when (expr) {
        is LiteralValueExprNode -> {
            val t = try {
                expr.getType()
            } catch (_: RuntimeException) {
                expr.inferType(emptyMap())
            }
            if (t is StringType) into += expr.literalText()
        }
        else -> expr.children.filterIsInstance<ExprNode>().forEach { collectStringLiteralsFromExpr(it, into) }
    }
}

private fun offerContainsIntToStringCoerce(offer: TlaActionOffer): Boolean {
    if (offer.decl.guards.any { exprContainsIntToStringCoerce(it) }) return true
    return offer.decl.transits.any { update ->
        when (update) {
            is TransitUpdate.Let -> exprContainsIntToStringCoerce(update.init)
            is TransitUpdate.Assign ->
                TlaVarProjection.get().isRelevant(offer.leaf.name, update.transitRootVar()) &&
                    exprContainsIntToStringCoerce(update.expr)
            is TransitUpdate.IndexPut ->
                TlaVarProjection.get().isRelevant(offer.leaf.name, update.transitRootVar()) &&
                    (exprContainsIntToStringCoerce(update.index) || exprContainsIntToStringCoerce(update.value))
        }
    }
}

private fun exprContainsIntToStringCoerce(expr: ExprNode): Boolean {
    if (expr is BinaryOpExprNode && expr.op() == "+") {
        val lhs = expr.lhsOperand()
        val rhs = expr.rhsOperand()
        val stringOp =
            exprIsStringTyped(expr) || exprIsStringTyped(lhs) || exprIsStringTyped(rhs)
        if (stringOp && exprIsStringTyped(lhs) != exprIsStringTyped(rhs)) return true
    }
    return expr.children.filterIsInstance<ExprNode>().any { exprContainsIntToStringCoerce(it) }
}

private fun collectIntLiteralsFromExpr(expr: ExprNode, into: MutableSet<Int>) {
    when (expr) {
        is LiteralValueExprNode -> {
            if (expr.inferType(emptyMap()) is IntType || expr.inferType(emptyMap()) is RealType) {
                expr.toTransitString(emptyMap(), emptySet()).toIntOrNull()?.let { into += it }
            }
        }
        is UnaryOpExprNode -> {
            if (expr.op() == "-") {
                val child = expr.operand()
                if (child is LiteralValueExprNode) {
                    child.toTransitString(emptyMap(), emptySet()).toIntOrNull()?.let { into += -it }
                }
            }
            collectIntLiteralsFromExpr(expr.operand(), into)
        }
        else -> expr.children.filterIsInstance<ExprNode>().forEach { collectIntLiteralsFromExpr(it, into) }
    }
}

internal fun typeToTlaDomain(
    type: Type,
    fieldOfObj: String? = null,
    fieldName: String? = null,
    leafClass: String? = null,
    varName: String? = null,
): String {
    closedLiteralDomain(type, leafClass, varName, fieldOfObj, fieldName)?.let { return it }
    return when (type) {
    is BoolType -> "BOOLEAN"
    is IntType -> "Int"
    is RealType -> "Int"
    is StringType -> "String"
        is DomainType -> type.name
        // Seq(S) is infinite; TLC needs a length-bounded set of sequences.
        is ListType -> "BoundedSeq(${typeToTlaDomain(type.elementType)}, MaxListLen)"
        is SetType -> "SUBSET ${typeToTlaDomain(type.elementType)}"
        is MapType -> "[${typeToTlaDomain(type.keyType)} -> ${typeToTlaDomain(type.valueType)}]"
    is ObjClassType -> {
        if (TlaOpaqueIndexSort.isOpaque(type)) {
            // Action `\E` / structural domains: live CONSTANT only (no sentinel).
            // TypeOK scalars still use valueDomain via typeOkVarRange.
            type.name
        } else {
            val single = TlaFieldProjection.get().singletonField(type)
            if (single != null) {
                typeToTlaDomain(single.type, type.name, single.name)
            } else {
                val fields = TlaFieldProjection.get().fieldsFor(type)
                if (fields.isEmpty()) {
                    "[dummy: {0}]"
                } else {
                    val rendered = fields.joinToString(", ") { f ->
                        "${f.name}: ${typeToTlaDomain(f.type, type.name, f.name)}"
                    }
                    "[$rendered]"
                }
            }
        }
    }
    else -> "Int"
    }
}

/**
 * Domain for `\E` IO / unsupported-builtin havoc. Lists use length bound 1 so
 * TLC does not build `BoundedSeq(S, MaxListLen)` sets larger than 1e6 elements
 * (e.g. cluster configs over `[id: Int, url: String]`).
 */
internal fun typeToTlaIoDomain(type: Type): String {
    closedLiteralDomain(type, null, null, null, null)?.let { return it }
    return when (type) {
        is ListType -> "BoundedSeq(${typeToTlaIoDomain(type.elementType)}, 1)"
        is SetType -> "SUBSET ${typeToTlaIoDomain(type.elementType)}"
        is MapType -> "[${typeToTlaIoDomain(type.keyType)} -> ${typeToTlaIoDomain(type.valueType)}]"
        is ObjClassType -> {
            val single = TlaFieldProjection.get().singletonField(type)
            if (single != null) {
                typeToTlaIoDomain(single.type)
            } else {
                val fields = TlaFieldProjection.get().fieldsFor(type)
                if (fields.isEmpty()) {
                    "[dummy: {0}]"
                } else {
                    val rendered = fields.joinToString(", ") { f ->
                        "${f.name}: ${typeToTlaIoDomain(f.type)}"
                    }
                    "[$rendered]"
                }
            }
        }
        else -> typeToTlaDomain(type)
    }
}

/** Collect sort CONSTANT names nested in [type]. */
internal fun collectSortConstants(type: Type, into: MutableSet<String>) {
    when (type) {
        is DomainType -> into += type.name
        is ObjClassType -> TlaFieldProjection.get().fieldsFor(type).forEach { collectSortConstants(it.type, into) }
        is ListType -> collectSortConstants(type.elementType, into)
        is SetType -> collectSortConstants(type.elementType, into)
        is MapType -> {
            collectSortConstants(type.keyType, into)
            collectSortConstants(type.valueType, into)
        }
        else -> {}
    }
}

/** Collect finite TLC model names (Int, String, …) needed by action argument domains. */
internal fun collectDomainModelNames(type: Type, into: MutableSet<String>) {
    when (type) {
        is IntType, is RealType -> into += "Int"
        is StringType -> into += "String"
        is DomainType -> into += type.name
        is ObjClassType -> TlaFieldProjection.get().fieldsFor(type).forEach { collectDomainModelNames(it.type, into) }
        is ListType -> {
            into += "MaxListLen"
            collectDomainModelNames(type.elementType, into)
        }
        is SetType -> collectDomainModelNames(type.elementType, into)
        is MapType -> {
            collectDomainModelNames(type.keyType, into)
            collectDomainModelNames(type.valueType, into)
        }
        else -> {}
    }
}

/** True if [expr] contains IO (`readln` / `readFile`) that should havoc a transit target in TLA+. */
internal fun exprContainsIoHavoc(expr: ExprNode): Boolean =
    when (expr) {
        is FunCallExprNode ->
            expr.callName() in FunBuiltinRegistry.ioHavocEffects ||
                expr.callArgs().any { exprContainsIoHavoc(it) }
        else -> expr.children.filterIsInstance<ExprNode>().any { exprContainsIoHavoc(it) }
    }


/**
 * Sort CONSTANTs from leaf state, used action args, and also-args after unused-field projection.
 */
private fun collectProjectedSortConstants(
    leaves: List<SpecLeaf>,
    pclasses: Map<String, ProcClassNode>,
    leafSpecs: Map<String, LeafSpecNode>,
    offers: List<TlaActionOffer>,
    into: MutableSet<String>,
) {
    leaves.forEach { leaf ->
        val pc = pclasses[leaf.name] ?: return@forEach
        pc.localDecls().filterIsInstance<VarNode>().forEach { vn ->
            if (!TlaVarProjection.get().isRelevant(leaf.name, vn.name)) return@forEach
            collectSortConstants(safeType(vn), into)
        }
        leafSpecs[leaf.name]?.leafSpecParamType()?.let { te ->
            typeDomainConstant(te)?.let { name ->
                if (name !in setOf("Int", "Nat", "Boolean", "Real")) into += name
            }
        }
    }
    offers.groupBy { it.decl.action.name }.forEach { (_, group) ->
        val extra = collectTlaExtraArgNames(group, pclasses, leafSpecs)
        val omit = analyzeTlaArgBind(group, TlaEmitOpts.get(), extra).omitArgTypeDomains()
        group.forEach { offer ->
            offer.decl.action.args.filter { offerRefsArg(offer, it.name) && it.name !in omit }.forEach { arg ->
                collectSortConstants(arg.type, into)
            }
        }
    }
    offers.forEach { offer ->
        val extra = collectTlaExtraArgNames(listOf(offer), pclasses, leafSpecs)
        val omit = analyzeTlaArgBind(listOf(offer), TlaEmitOpts.get(), extra).omitArgTypeDomains()
        val pc = pclasses[offer.leaf.name] ?: return@forEach
        val also = if (offer.isConstructor) {
            pc.localDecls().filterIsInstance<ConstructorNode>()
                .firstOrNull { it.constructorName() == offer.decl.action.name }
                ?.alsoArgs()
        } else {
            pc.localDecls().filterIsInstance<TransitionNode>()
                .firstOrNull { it.transitionName() == offer.decl.action.name }
                ?.alsoArgs()
        }
        also?.children?.filterIsInstance<ArgNode>()?.forEach { arg ->
            if (arg.argName() in omit) return@forEach
            try {
                collectSortConstants(arg.type, into)
            } catch (_: RuntimeException) {
                typeDomainConstant(arg.argTypeExpr())?.let { name ->
                    if (name !in setOf("Int", "Nat", "Boolean", "Real")) into += name
                }
            }
        }
    }
}

private fun collectActionArgDomainModels(
    offers: List<TlaActionOffer>,
    pclasses: Map<String, ProcClassNode>,
    leafSpecs: Map<String, LeafSpecNode>,
    into: MutableSet<String>,
) {
    offers.groupBy { it.decl.action.name }.forEach { (_, group) ->
        val extra = collectTlaExtraArgNames(group, pclasses, leafSpecs)
        val omit = analyzeTlaArgBind(group, TlaEmitOpts.get(), extra).omitArgTypeDomains()
        group.forEach { offer ->
            offer.decl.action.args.filter { offerRefsArg(offer, it.name) && it.name !in omit }.forEach { arg ->
                if (argLiteralDomain(group, arg.name, arg.type) == null) {
                    collectDomainModelNames(arg.type, into)
                }
            }
        }
    }
    offers.forEach { offer ->
        val extra = collectTlaExtraArgNames(listOf(offer), pclasses, leafSpecs)
        val omit = analyzeTlaArgBind(listOf(offer), TlaEmitOpts.get(), extra).omitArgTypeDomains()
        val pc = pclasses[offer.leaf.name] ?: return@forEach
        val also = if (offer.isConstructor) {
            pc.localDecls().filterIsInstance<ConstructorNode>()
                .firstOrNull { it.constructorName() == offer.decl.action.name }
                ?.alsoArgs()
        } else {
            pc.localDecls().filterIsInstance<TransitionNode>()
                .firstOrNull { it.transitionName() == offer.decl.action.name }
                ?.alsoArgs()
        }
        also?.children?.filterIsInstance<ArgNode>()?.forEach { arg ->
            if (arg.argName() in omit) return@forEach
            try {
                collectDomainModelNames(arg.type, into)
            } catch (_: RuntimeException) {
            }
        }
    }
}

internal fun offerRefsArg(offer: TlaActionOffer, argName: String): Boolean =
    offer.decl.guards.any { exprReferencesSymbol(it, argName) } ||
        offer.decl.errors.any { exprReferencesSymbol(it.condExpr(), argName) } ||
        offer.decl.transits.any { update ->
            when (update) {
                is TransitUpdate.Assign -> exprReferencesSymbol(update.expr, argName)
                is TransitUpdate.IndexPut ->
                    exprReferencesSymbol(update.index, argName) || exprReferencesSymbol(update.value, argName)
                is TransitUpdate.Let -> exprReferencesSymbol(update.init, argName)
            }
        }

/**
 * Finite TLC models for `\E` domains of IO / unsupported-builtin havoc (same sites as
 * [typeToTlaIoDomain] emission). Must include unsupported builtins (e.g. parseInt via
 * user funs), not only direct IO calls — otherwise `[id: Int, …]` is not enumerable.
 */
private fun collectIoHavocDomainModels(
    offers: List<TlaActionOffer>,
    into: MutableSet<String>,
) {
    fun needsHavocDomain(expr: ExprNode): Boolean =
        exprContainsIoHavoc(expr) || exprContainsUnsupportedTlaBuiltin(expr)

    offers.forEach { offer ->
        offer.decl.transits.forEach { update ->
            when (update) {
                is TransitUpdate.Assign -> if (
                    TlaVarProjection.get().isRelevant(offer.leaf.name, update.transitRootVar()) &&
                    needsHavocDomain(update.expr)
                ) {
                    try {
                        collectDomainModelNames(update.expr.getType(), into)
                    } catch (_: RuntimeException) {
                    }
                }
                is TransitUpdate.IndexPut -> if (
                    TlaVarProjection.get().isRelevant(offer.leaf.name, update.transitRootVar()) &&
                    needsHavocDomain(update.value)
                ) {
                    try {
                        collectDomainModelNames(update.value.getType(), into)
                    } catch (_: RuntimeException) {
                    }
                }
                is TransitUpdate.Let -> if (needsHavocDomain(update.init)) {
                    try {
                        collectDomainModelNames(update.init.getType(), into)
                    } catch (_: RuntimeException) {
                    }
                }
            }
        }
    }
}

/** Invariants in the transitive closure of [rootName], dependencies before dependents. */
internal fun topologicalInvariantClosure(
    rootName: String,
    invariants: Map<String, InvariantNode>,
): List<InvariantNode> {
    val root = invariants[rootName] ?: return emptyList()
    val order = mutableListOf<InvariantNode>()
    val visiting = mutableSetOf<String>()
    val visited = mutableSetOf<String>()

    fun visit(name: String) {
        if (name in visited) return
        check(name !in visiting) { "cyclic invariant reference involving \"$name\"" }
        val node = invariants[name] ?: return
        visiting += name
        collectInvariantRefs(node.invariantFormula(), invariants).forEach { visit(it) }
        visiting -= name
        visited += name
        order += node
    }

    visit(root.name())
    return order
}

/**
 * Resolve AG guarantee roots. Circular right `<G> C <A, Safe>` yields both A and Safe.
 * Named invariant symbols reuse declared nodes; other formulas get a synthetic root.
 */
internal fun resolveGuaranteeInvariants(
    ag: AgSpecExprNode,
    specName: String,
    invariants: Map<String, InvariantNode>,
): List<InvariantNode> {
    val (names, _) = guaranteeInvariantNames(ag, invariants.keys)
    if (names.isNotEmpty()) {
        return names.mapNotNull { invariants[it] }
    }
    val guarantee = ag.guaranteeExpr() ?: return emptyList()
    if (guarantee is SymbolValueExprNode && invariants.containsKey(guarantee.symbol)) {
        return listOf(invariants.getValue(guarantee.symbol))
    }
    val syntheticName = when {
        "Guarantee" !in invariants -> "Guarantee"
        "${specName}_Inv" !in invariants -> "${specName}_Inv"
        else -> {
            var i = 2
            while ("${specName}_Inv$i" in invariants) i++
            "${specName}_Inv$i"
        }
    }
    return listOf(InvariantNode(syntheticName, guarantee, ag.programLocation()))
}

/** Compat: first guarantee root, or null. */
internal fun resolveGuaranteeInvariant(
    ag: AgSpecExprNode,
    specName: String,
    invariants: Map<String, InvariantNode>,
): InvariantNode? = resolveGuaranteeInvariants(ag, specName, invariants).firstOrNull()

/** Names of invariants referenced (as bare symbols) in [expr]. */
internal fun collectInvariantRefs(
    expr: ExprNode,
    invariants: Map<String, InvariantNode>,
): Set<String> {
    val refs = linkedSetOf<String>()
    fun walk(e: ExprNode, bound: Set<String>) {
        when (e) {
            is SymbolValueExprNode -> {
                if (e.symbol !in bound && invariants.containsKey(e.symbol)) refs += e.symbol
            }
            is QuantifiedExprNode -> walk(e.quantifiedBody(), bound + e.binderName())
            is UnaryOpExprNode -> walk(e.operand(), bound)
            is BinaryOpExprNode -> {
                walk(e.lhsOperand(), bound)
                walk(e.rhsOperand(), bound)
            }
            is IfElseExprNode -> {
                walk(e.condExpr(), bound)
                walk(e.thenExpr(), bound)
                walk(e.elseExpr(), bound)
            }
            is IndexExprNode -> {
                walk(e.base, bound)
                walk(e.index, bound)
            }
            is MemberAccessExprNode -> walk(e.baseExpr, bound)
            else -> e.children.filterIsInstance<ExprNode>().forEach { walk(it, bound) }
        }
    }
    walk(expr, emptySet())
    return refs
}

internal fun collectTypeConstants(expr: ExprNode, into: MutableSet<String>) {
    when (expr) {
        is QuantifiedExprNode -> {
            typeDomainConstant(expr.binderTypeExpr())?.let { name ->
                // Int/Nat/Boolean/Real come from EXTENDS; String and user types need CONSTANT.
                if (name !in setOf("Int", "Nat", "Boolean", "Real")) {
                    into += name
                }
            }
            collectTypeConstants(expr.quantifiedBody(), into)
        }
        is UnaryOpExprNode -> collectTypeConstants(expr.operand(), into)
        is BinaryOpExprNode -> {
            collectTypeConstants(expr.lhsOperand(), into)
            collectTypeConstants(expr.rhsOperand(), into)
        }
        is IfElseExprNode -> {
            collectTypeConstants(expr.condExpr(), into)
            collectTypeConstants(expr.thenExpr(), into)
            collectTypeConstants(expr.elseExpr(), into)
        }
        is IndexExprNode -> {
            collectTypeConstants(expr.base, into)
            collectTypeConstants(expr.index, into)
        }
        else -> expr.children.filterIsInstance<ExprNode>().forEach { collectTypeConstants(it, into) }
    }
}

/** Built-in domains that need a finite TLC model assignment in the .cfg. */
internal fun collectBuiltinDomainUses(expr: ExprNode, into: MutableSet<String>) {
    when (expr) {
        is QuantifiedExprNode -> {
            typeDomainConstant(expr.binderTypeExpr())?.let { name ->
                if (name in setOf("Int", "Nat", "Real")) {
                    into += name
                }
            }
            collectBuiltinDomainUses(expr.quantifiedBody(), into)
        }
        is UnaryOpExprNode -> collectBuiltinDomainUses(expr.operand(), into)
        is BinaryOpExprNode -> {
            collectBuiltinDomainUses(expr.lhsOperand(), into)
            collectBuiltinDomainUses(expr.rhsOperand(), into)
        }
        is IfElseExprNode -> {
            collectBuiltinDomainUses(expr.condExpr(), into)
            collectBuiltinDomainUses(expr.thenExpr(), into)
            collectBuiltinDomainUses(expr.elseExpr(), into)
        }
        is IndexExprNode -> {
            collectBuiltinDomainUses(expr.base, into)
            collectBuiltinDomainUses(expr.index, into)
        }
        else -> expr.children.filterIsInstance<ExprNode>().forEach { collectBuiltinDomainUses(it, into) }
    }
}

private fun exprIsStringTyped(expr: ExprNode): Boolean = try {
    expr.getType() is StringType
} catch (_: RuntimeException) {
    false
}

private fun exprIsSetTyped(expr: ExprNode): Boolean = try {
    expr.getType() is SetType
} catch (_: RuntimeException) {
    false
}

private fun tlaStringCoerce(expr: ExprNode, rendered: String): String =
    if (exprIsStringTyped(expr)) rendered else "ToString($rendered)"

private fun isEmptyStringLiteral(expr: ExprNode): Boolean =
    expr is LiteralValueExprNode &&
        expr.getType() is StringType &&
        expr.literalText().isEmpty()

private fun exprIsListTyped(expr: ExprNode): Boolean = try {
    expr.getType() is ListType
} catch (_: RuntimeException) {
    false
}

private fun exprIsMapTyped(expr: ExprNode): Boolean = try {
    expr.getType() is MapType
} catch (_: RuntimeException) {
    false
}

private fun tlaLengthOf(baseRendered: String, baseExpr: ExprNode): String {
    val ty = try { baseExpr.getType() } catch (_: RuntimeException) { null }
    return when {
        ty is ListType -> "Cardinality(DOMAIN $baseRendered)"
        ty is StringType -> "Len($baseRendered)"
        ty is SetType -> "Cardinality($baseRendered)"
        ty is MapType -> "Cardinality(DOMAIN $baseRendered)"
        ty is DomainType -> "Cardinality($baseRendered)"
        // Opaque index-sort record: CONSTANT set of atoms (same as DomainType).
        ty is ObjClassType && TlaOpaqueIndexSort.isOpaque(ty) -> "Cardinality($baseRendered)"
        else -> "Cardinality(DOMAIN $baseRendered)"
    }
}

private fun tlaLengthOfType(baseRendered: String, type: Type?): String =
    when {
        type is SetType || type is DomainType -> "Cardinality($baseRendered)"
        type is ObjClassType && TlaOpaqueIndexSort.isOpaque(type) -> "Cardinality($baseRendered)"
        type is MapType -> "Cardinality(DOMAIN $baseRendered)"
        type is StringType -> "Len($baseRendered)"
        else -> "Cardinality(DOMAIN $baseRendered)"
    }

/** `.length` / `.keys` / remaining obj fields after a leaf state var. */
private fun emitPeerPropPath(baseRendered: String, rootType: Type?, path: List<String>): String {
    var acc = baseRendered
    var ty = rootType
    for (seg in path) {
        when (seg) {
            "length" -> {
                acc = tlaLengthOfType(acc, ty)
                ty = intType
            }
            "keys" -> {
                acc = "DOMAIN $acc"
                ty = (ty as? MapType)?.let { setType(it.keyType) }
            }
            else -> {
                acc = emitUnwrappedFieldPath(acc, ty, listOf(seg))
                ty = when (val r = ty?.let { resolveFieldPath(it, listOf(seg)) }) {
                    is FieldPathResult.Resolved -> r.type
                    else -> null
                }
            }
        }
    }
    return acc
}

/**
 * @param leafCtx map of leaf name → SpecLeaf (for FieldAccess context; optional)
 * @param argNames action argument symbols (emitted bare)
 * @param self index variable for the current parameterized leaf, or null
 * @param bareStateVars state vars that may appear unqualified in action guards/transits
 * @param reservedNames CONSTANT names; quantifier binders that clash are renamed
 * @param stateVarNames (leaf, julayVar) → TLA identifier
 */

/**
 * Collect user [FunNode]s referenced by [expr] (resolved fun calls only; builtins ignored).
 */
internal fun collectUserFunNodesFromExpr(expr: ExprNode, into: MutableSet<FunNode>) {
    when (expr) {
        is FunCallExprNode -> {
            expr.resolvedFunOrNull()?.let { into += it }
            expr.namedFunArgNodeOrNull()?.let { into += it }
            expr.callArgs().forEach { collectUserFunNodesFromExpr(it, into) }
            expr.specializedBodyOrNull()?.let { collectUserFunNodesFromExpr(it, into) }
            expr.namedFunBodyOrNull()?.let { collectUserFunNodesFromExpr(it, into) }
        }
        is ParenExprNode -> collectUserFunNodesFromExpr(expr.innerExpr(), into)
        else -> expr.children.filterIsInstance<ExprNode>().forEach { collectUserFunNodesFromExpr(it, into) }
    }
}

/** True when [expr] contains a Julay `splice(...)` call (list slice). */
internal fun exprContainsSlice(expr: ExprNode): Boolean =
    when (expr) {
        is FunCallExprNode ->
            expr.callName() == "splice" ||
                expr.callArgs().any { exprContainsSlice(it) }
        is ParenExprNode -> exprContainsSlice(expr.innerExpr())
        else -> expr.children.filterIsInstance<ExprNode>().any { exprContainsSlice(it) }
    }

/** True when any Init/action offer guard or transit uses a list slice. */
internal fun offersUseSlice(offers: List<TlaActionOffer>): Boolean =
    offers.any { offer ->
        offer.decl.guards.any { exprContainsSlice(it) } ||
            offer.decl.transits.any { update ->
                when (update) {
                    is TransitUpdate.Assign -> exprContainsSlice(update.expr)
                    is TransitUpdate.IndexPut ->
                        exprContainsSlice(update.index) || exprContainsSlice(update.value)
                    is TransitUpdate.Let -> exprContainsSlice(update.init)
                }
            }
    }

/**
 * Julay `splice(xs, s, e)` helper: 1-based inclusive with clamp.
 * Default (TLC): `SubSeq` / `Len`. With `--opt-tla=apalache`: finite CASE on
 * result length (Apalache forbids symbolic `1..(hi-lo+1)`).
 * Parameter / binder names are clash-renamed against [reservedNames].
 */
internal fun emitSpliceOperatorDef(reservedNames: Set<String>): String {
    val taken = reservedNames.toMutableSet()
    val xs = allocTlaName("xs", taken)
    val s = allocTlaName("s", taken)
    val e = allocTlaName("e", taken)
    val lo = allocTlaName("lo", taken)
    val hi = allocTlaName("hi", taken)
    val body = if (TlaEmitOpts.get().apalache) {
        // CASE on result length (≤ MaxListLen) — Apalache forbids symbolic ranges.
        val spliceArms = (1..DEFAULT_MAX_LIST_LEN).joinToString("\n") { n ->
            val funExpr = if (n == 1) {
                "(1 :> $xs[$lo])"
            } else {
                (1..n).joinToString(" @@ ") { i ->
                    "($i :> $xs[$lo + ${i - 1}])"
                }
            }
            "             [] outLen = $n -> $funExpr"
        }
        "splice($xs, $s, $e) ==\n" +
            "  LET empty == [__j \\in {} |-> $xs[__j]]\n" +
            "  IN IF $e < 1 THEN empty\n" +
            "     ELSE LET $hi == IF $e > Cardinality(DOMAIN $xs) THEN Cardinality(DOMAIN $xs) ELSE $e\n" +
            "              $lo == $s\n" +
            "              outLen == IF $lo > $hi THEN 0 ELSE $hi - $lo + 1\n" +
            "          IN CASE outLen = 0 -> empty\n" +
            spliceArms + "\n" +
            "             [] OTHER -> empty"
    } else {
        "splice($xs, $s, $e) ==\n" +
            "  IF $e < 1 THEN <<>>\n" +
            "  ELSE LET $hi == IF $e > Len($xs) THEN Len($xs) ELSE $e\n" +
            "           $lo == $s\n" +
            "       IN IF $lo > $hi THEN <<>> ELSE SubSeq($xs, $lo, $hi)"
    }
    val ann = apalacheTypeComment("((Int -> a), Int, Int) => (Int -> a)")
    return if (ann != null) "$ann\n$body" else body
}

/**
 * Julay `startsWith`. Apalache `Str` is not a `Seq`, so avoid `Len`/`SubSeq`
 * (TLC strings are sequences; IO helpers that need true prefixes are already
 * degraded when `split`/`trim` are unsupported).
 */
internal fun emitStartsWithOperatorDef(reservedNames: Set<String>): String {
    val taken = reservedNames.toMutableSet()
    val str = allocTlaName("s", taken)
    val prefix = allocTlaName("p", taken)
    val body = "startsWith($str, $prefix) ==\n" +
        "  \\/ $prefix = \"\"\n" +
        "  \\/ $str = $prefix"
    val ann = apalacheTypeComment("(Str, Str) => Bool")
    return if (ann != null) "$ann\n$body" else body
}

private val tlaUnsupportedBuiltins = setOf("split", "parseInt", "trim", "portFromUrl")

internal fun exprContainsUnsupportedTlaBuiltin(expr: ExprNode): Boolean =
    when (expr) {
        is FunCallExprNode -> {
            val name = expr.resolvedBuiltinOrNull()?.name ?: expr.callName()
            name in tlaUnsupportedBuiltins ||
                expr.callArgs().any { exprContainsUnsupportedTlaBuiltin(it) } ||
                (expr.resolvedFunOrNull()?.let { exprContainsUnsupportedTlaBuiltin(it.funBody()) } ?: false)
        }
        is ParenExprNode -> exprContainsUnsupportedTlaBuiltin(expr.innerExpr())
        else -> expr.children.filterIsInstance<ExprNode>().any { exprContainsUnsupportedTlaBuiltin(it) }
    }

internal fun offerContainsUnsupportedTlaBuiltin(offer: TlaActionOffer): Boolean =
    offer.decl.guards.any { exprContainsUnsupportedTlaBuiltin(it) } ||
        offer.decl.transits.any { update ->
            when (update) {
                is TransitUpdate.Assign -> exprContainsUnsupportedTlaBuiltin(update.expr)
                is TransitUpdate.IndexPut ->
                    exprContainsUnsupportedTlaBuiltin(update.index) ||
                        exprContainsUnsupportedTlaBuiltin(update.value)
                is TransitUpdate.Let -> exprContainsUnsupportedTlaBuiltin(update.init)
            }
        }

/** True when [expr] calls `startsWith`. */
internal fun exprContainsStartsWith(expr: ExprNode): Boolean =
    when (expr) {
        is FunCallExprNode ->
            (expr.resolvedBuiltinOrNull()?.name == "startsWith" || expr.callName() == "startsWith") ||
                expr.callArgs().any { exprContainsStartsWith(it) }
        is ParenExprNode -> exprContainsStartsWith(expr.innerExpr())
        else -> expr.children.filterIsInstance<ExprNode>().any { exprContainsStartsWith(it) }
    }

internal fun offersUseStartsWith(offers: List<TlaActionOffer>): Boolean =
    offers.any { offer ->
        offer.decl.guards.any { exprContainsStartsWith(it) } ||
            offer.decl.transits.any { update ->
                when (update) {
                    is TransitUpdate.Assign -> exprContainsStartsWith(update.expr)
                    is TransitUpdate.IndexPut ->
                        exprContainsStartsWith(update.index) || exprContainsStartsWith(update.value)
                    is TransitUpdate.Let -> exprContainsStartsWith(update.init)
                }
            }
    }

/** True when [expr] uses Julay list `in` / `~in` (TLA sequences need `Range`). */
internal fun exprContainsListMembership(expr: ExprNode): Boolean =
    when (expr) {
        is BinaryOpExprNode -> {
            val op = expr.op()
            ((op == "in" || op == "~in") && exprIsListTyped(expr.rhsOperand())) ||
                exprContainsListMembership(expr.lhsOperand()) ||
                exprContainsListMembership(expr.rhsOperand())
        }
        is ParenExprNode -> exprContainsListMembership(expr.innerExpr())
        else -> expr.children.filterIsInstance<ExprNode>().any { exprContainsListMembership(it) }
    }

internal fun offersUseListMembership(offers: List<TlaActionOffer>): Boolean =
    offers.any { offer ->
        offer.decl.guards.any { exprContainsListMembership(it) } ||
            offer.decl.errors.any { exprContainsListMembership(it.condExpr()) } ||
            offer.decl.transits.any { update ->
                when (update) {
                    is TransitUpdate.Assign -> exprContainsListMembership(update.expr)
                    is TransitUpdate.IndexPut ->
                        exprContainsListMembership(update.index) || exprContainsListMembership(update.value)
                    is TransitUpdate.Let -> exprContainsListMembership(update.init)
                }
            }
    }

/** Sequence values: `Range(f) == { f[x] : x \in DOMAIN f }` (not in standard `TLC.tla`). */
internal fun emitRangeOperatorDef(reservedNames: Set<String>): String {
    val taken = reservedNames.toMutableSet()
    val f = allocTlaName("f", taken)
    val i = allocTlaName("__i", taken)
    return "Range($f) == { $f[$i] : $i \\in DOMAIN $f }"
}

/**
 * Julay `s.toList()`: arbitrary enumeration (order unspecified).
 * Default (TLC): recursive `CHOOSE` + `\o`. With `--opt-tla=apalache`: finite
 * CASE on Cardinality (avoids `1..Cardinality(S)` / BoundedSeq expansion).
 */
internal fun emitSetToSeqOperatorDef(reservedNames: Set<String>): String {
    val taken = reservedNames.toMutableSet()
    val s = allocTlaName("S", taken)
    val x = allocTlaName("x", taken)
    val body = if (TlaEmitOpts.get().apalache) {
        val j = allocTlaName("j", taken)
        val empty = "[$j \\in {} |-> CHOOSE $x \\in $s : TRUE]"
        val arms = (1..DEFAULT_MAX_LIST_LEN).joinToString("\n") { n ->
            val lets = (1..n).joinToString("\n             ") { i ->
                val binder = "z$i"
                val exclude =
                    if (i == 1) ""
                    else " \\ {${(1 until i).joinToString(", ") { "e$it" }}}"
                "e$i == CHOOSE $binder \\in $s$exclude : TRUE"
            }
            // Use :> / @@ so CASE does not confuse `1 |->` with arm syntax.
            val funExpr = if (n == 1) {
                "(1 :> e1)"
            } else {
                (1..n).joinToString(" @@ ") { i -> "($i :> e$i)" }
            }
            "    [] Cardinality($s) = $n ->\n" +
                "         LET $lets\n" +
                "         IN $funExpr"
        }
        "SetToSeq($s) ==\n" +
            "  CASE Cardinality($s) = 0 -> $empty\n" +
            arms + "\n" +
            "    [] OTHER -> $empty"
    } else {
        val e = allocTlaName("e", taken)
        "RECURSIVE SetToSeq(_)\n" +
            "SetToSeq($s) ==\n" +
            "  IF $s = {} THEN <<>>\n" +
            "  ELSE LET $e == CHOOSE $x \\in $s : TRUE\n" +
            "       IN <<$e>> \\o SetToSeq($s \\ {$e})"
    }
    val ann = apalacheTypeComment("(Set(a) => (Int -> a))")
    return if (ann != null) "$ann\n$body" else body
}

/** Julay `allDistinct(xs)`: every list element is unique. */
internal fun emitAllDistinctOperatorDef(reservedNames: Set<String>): String {
    val taken = reservedNames.toMutableSet()
    val xs = allocTlaName("xs", taken)
    val body = "allDistinct($xs) == Cardinality(DOMAIN $xs) = Cardinality(Range($xs))"
    val ann = apalacheTypeComment("((Int -> a) => Bool)")
    return if (ann != null) "$ann\n$body" else body
}

internal fun exprContainsToSet(expr: ExprNode): Boolean =
    when (expr) {
        is MethodCallExprNode ->
            expr.methodName == "toSet" ||
                exprContainsToSet(expr.baseExpr) ||
                expr.args.any { exprContainsToSet(it) } ||
                (expr.hofBodyOrNull()?.let { exprContainsToSet(it) } == true)
        is ParenExprNode -> exprContainsToSet(expr.innerExpr())
        else -> expr.children.filterIsInstance<ExprNode>().any { exprContainsToSet(it) }
    }

internal fun offersUseToSet(offers: List<TlaActionOffer>): Boolean =
    offersUseExpr(offers, ::exprContainsToSet)

internal fun exprContainsToList(expr: ExprNode): Boolean =
    when (expr) {
        is MethodCallExprNode ->
            expr.methodName == "toList" ||
                exprContainsToList(expr.baseExpr) ||
                expr.args.any { exprContainsToList(it) } ||
                (expr.hofBodyOrNull()?.let { exprContainsToList(it) } == true)
        is ParenExprNode -> exprContainsToList(expr.innerExpr())
        else -> expr.children.filterIsInstance<ExprNode>().any { exprContainsToList(it) }
    }

internal fun offersUseToList(offers: List<TlaActionOffer>): Boolean =
    offersUseExpr(offers, ::exprContainsToList)

internal fun exprContainsAllDistinct(expr: ExprNode): Boolean =
    when (expr) {
        is FunCallExprNode ->
            (expr.resolvedBuiltinOrNull()?.name == "allDistinct" || expr.callName() == "allDistinct") ||
                expr.callArgs().any { exprContainsAllDistinct(it) }
        is ParenExprNode -> exprContainsAllDistinct(expr.innerExpr())
        else -> expr.children.filterIsInstance<ExprNode>().any { exprContainsAllDistinct(it) }
    }

internal fun offersUseAllDistinct(offers: List<TlaActionOffer>): Boolean =
    offersUseExpr(offers, ::exprContainsAllDistinct)

private fun offersUseExpr(offers: List<TlaActionOffer>, pred: (ExprNode) -> Boolean): Boolean =
    offers.any { offer ->
        offer.decl.guards.any(pred) ||
            offer.decl.errors.any { pred(it.condExpr()) } ||
            offer.decl.transits.any { update ->
                when (update) {
                    is TransitUpdate.Assign -> pred(update.expr)
                    is TransitUpdate.IndexPut -> pred(update.index) || pred(update.value)
                    is TransitUpdate.Let -> pred(update.init)
                }
            }
    }

/** Collect unsupported funlib builtins used in TLA-emitted exprs (for compile warnings). */
internal fun collectUnsupportedBuiltinWarnings(
    offers: List<TlaActionOffer>,
    funs: Collection<FunNode>,
    invClosure: List<InvariantNode>,
): List<OneLocCompileWarning> {
    val seen = linkedSetOf<String>()
    val warnings = mutableListOf<OneLocCompileWarning>()
    fun visit(expr: ExprNode) {
        when (expr) {
            is FunCallExprNode -> {
                val name = expr.resolvedBuiltinOrNull()?.name ?: expr.callName()
                if (name in tlaUnsupportedBuiltins && name !in seen) {
                    seen += name
                    warnings += OneLocCompileWarning(
                        expr.programLocation(),
                        "Builtin \"$name\" is not supported in TLA+ emission and will degrade to TRUE",
                    )
                }
                expr.callArgs().forEach { visit(it) }
            }
            is ParenExprNode -> visit(expr.innerExpr())
            else -> expr.children.filterIsInstance<ExprNode>().forEach { visit(it) }
        }
    }
    offers.forEach { offer ->
        offer.decl.guards.forEach { visit(it) }
        offer.decl.transits.forEach { update ->
            when (update) {
                is TransitUpdate.Assign -> visit(update.expr)
                is TransitUpdate.IndexPut -> {
                    visit(update.index)
                    visit(update.value)
                }
                is TransitUpdate.Let -> visit(update.init)
            }
        }
    }
    funs.forEach { visit(it.funBody()) }
    invClosure.forEach { visit(it.invariantFormula()) }
    return warnings
}

/** User funs called from Init/action guards, transit RHS, and extra exprs (invariants), plus transitive callees. */
internal fun collectUserFunsUsedInOffers(
    offers: List<TlaActionOffer>,
    extraExprs: List<ExprNode> = emptyList(),
): LinkedHashSet<FunNode> {
    val used = linkedSetOf<FunNode>()
    offers.forEach { offer ->
        offer.decl.guards.forEach { collectUserFunNodesFromExpr(it, used) }
        offer.decl.transits.forEach { update ->
            when (update) {
                is TransitUpdate.Assign -> collectUserFunNodesFromExpr(update.expr, used)
                is TransitUpdate.IndexPut -> {
                    collectUserFunNodesFromExpr(update.index, used)
                    collectUserFunNodesFromExpr(update.value, used)
                }
                is TransitUpdate.Let -> collectUserFunNodesFromExpr(update.init, used)
            }
        }
    }
    extraExprs.forEach { collectUserFunNodesFromExpr(it, used) }
    val queue = ArrayDeque(used)
    val seen = used.map { it.name() }.toMutableSet()
    while (queue.isNotEmpty()) {
        val f = queue.removeFirst()
        val nested = linkedSetOf<FunNode>()
        collectUserFunNodesFromExpr(f.funBody(), nested)
        nested.forEach { g ->
            if (g.name() !in seen) {
                seen += g.name()
                used += g
                queue += g
            }
        }
    }
    return used
}

/** True when [funNode] comes from stdlib `julay/funlib/` (e.g. math.jul max/min). */
internal fun isJulayFunlibFun(funNode: FunNode): Boolean {
    val path = (funNode.programLocation() as? SourceLoc)?.filePath?.toString() ?: return false
    return path.replace('\\', '/').contains("julay/funlib/")
}

/** Callees before callers when possible so nested fun operators appear first. */
internal fun orderFunsForTlaEmit(funs: Set<FunNode>): List<FunNode> {
    if (funs.isEmpty()) return emptyList()
    val byName = funs.associateBy { it.name() }
    val deps = funs.associate { f ->
        val nested = linkedSetOf<FunNode>()
        collectUserFunNodesFromExpr(f.funBody(), nested)
        f.name() to nested.map { it.name() }.filter { it in byName && it != f.name() }.toMutableSet()
    }
    val remaining = funs.map { it.name() }.toMutableSet()
    val ordered = mutableListOf<FunNode>()
    while (remaining.isNotEmpty()) {
        val ready = remaining.filter { name -> deps.getValue(name).none { it in remaining } }
        val batch = if (ready.isEmpty()) listOf(remaining.first()) else ready.sorted()
        batch.forEach { name ->
            remaining -= name
            ordered += byName.getValue(name)
            deps.values.forEach { it -= name }
        }
    }
    return ordered
}

/**
 * Prefer [preferred]. On clash: `preferred_`, then `preferred_2`, `preferred_3`, …
 * Does not mutate [taken].
 */
internal fun firstFreeParamTlaName(preferred: String, taken: Set<String>): String {
    if (preferred !in taken) return preferred
    var name = "${preferred}_"
    var n = 2
    while (name in taken) {
        name = "${preferred}_$n"
        n++
    }
    return name
}

internal fun allocTlaName(preferred: String, taken: MutableSet<String>): String {
    var name = preferred
    if (name in taken) {
        name = "p_$preferred"
        var n = 2
        while (name in taken) {
            name = "p_${preferred}_$n"
            n++
        }
    }
    taken += name
    return name
}

/** One transit-scoped let to wrap around later assign conjuncts. */
internal data class TransitLetEmit(
    val tlaName: String,
    val initTla: String,
    /** When set, wrap as `\\E __io \\in domain: LET name == __io IN …`. */
    val ioDomain: String? = null,
)

/**
 * Build a single action conjunct: `LET` (and optional IO `\\E`) around [bodyConjuncts].
 * Consecutive non-IO lets share one `LET`. Each body conjunct should already start with `/\\ `.
 */
internal fun wrapTransitLetBlock(
    bindings: List<TransitLetEmit>,
    bodyConjuncts: List<String>,
): String {
    val effective = if (TlaEmitOpts.get().unusedLets) {
        pruneUnusedTransitLets(bindings, bodyConjuncts)
    } else {
        bindings
    }
    if (effective.isEmpty()) {
        return if (bodyConjuncts.isEmpty()) "TRUE" else bodyConjuncts.joinToString("\n")
    }
    require(effective.isNotEmpty()) { "wrapTransitLetBlock requires bindings" }
    var text = if (bodyConjuncts.isEmpty()) {
        "TRUE"
    } else {
        bodyConjuncts.joinToString("\n")
    }
    data class Seg(val io: TransitLetEmit?, val lets: List<TransitLetEmit>)
    val segs = mutableListOf<Seg>()
    var i = 0
    while (i < effective.size) {
        val b = effective[i]
        if (b.ioDomain != null) {
            i++
            val extra = mutableListOf<TransitLetEmit>()
            while (i < effective.size && effective[i].ioDomain == null) {
                extra += effective[i]
                i++
            }
            segs += Seg(b, extra)
        } else {
            val plain = mutableListOf<TransitLetEmit>()
            while (i < effective.size && effective[i].ioDomain == null) {
                plain += effective[i]
                i++
            }
            segs += Seg(null, plain)
        }
    }
    for (si in segs.indices.reversed()) {
        val seg = segs[si]
        val depth = si
        // Action defs join parts with "\n  ", so the first line of this block gets a 2-space
        // module indent; continuation lines must already be indented past that `/\`.
        // Preserve relative indents inside multi-line RHS (nested IF); flattening them to the
        // same column as `/\` makes SANY reject the junction item.
        val bodyPad = " ".repeat(4 + depth * 2)
        val letPad = " ".repeat(4 + (depth - 1).coerceAtLeast(0) * 2)
        val indentedBody = padPreservingRelative(text, bodyPad)
        val io = seg.io
        val defs = if (io != null) {
            listOf(TransitLetEmit(io.tlaName, "__io_${io.tlaName}")) + seg.lets
        } else {
            seg.lets
        }
        val letPrefix = when {
            io != null && depth == 0 -> "    "
            io != null -> "${letPad}  "
            depth == 0 -> "/\\ "
            else -> letPad
        }
        val letBlock = formatTransitLet(
            defs,
            indentedBody,
            firstLinePrefix = letPrefix,
            compensateModule = io == null && depth == 0,
            initPad = bodyPad,
        )
        text = if (io != null) {
            val ioName = "__io_${io.tlaName}"
            if (depth == 0) {
                "/\\ \\E $ioName \\in ${io.ioDomain}:\n$letBlock"
            } else {
                "${letPad}\\E $ioName \\in ${io.ioDomain}:\n$letBlock"
            }
        } else {
            letBlock
        }
    }
    return text
}

internal fun pruneUnusedTransitLets(
    bindings: List<TransitLetEmit>,
    bodyConjuncts: List<String>,
): List<TransitLetEmit> {
    val body = bodyConjuncts.joinToString("\n")
    val kept = mutableListOf<TransitLetEmit>()
    for (b in bindings.asReversed()) {
        val rest = buildString {
            kept.forEach { append(it.initTla).append('\n') }
            append(body)
        }
        if (tlaIdentReferenced(rest, b.tlaName)) kept.add(0, b)
    }
    return kept
}

internal fun pruneUnusedExprLets(
    chain: List<Pair<String, ExprNode>>,
    body: ExprNode,
): List<Pair<String, ExprNode>> {
    val emittedParts = flattenTopLevelAnd(body).filter { !TlaSkipConjuncts.skipped(it) }
    val kept = mutableListOf<Pair<String, ExprNode>>()
    for ((name, init) in chain.asReversed()) {
        val used = emittedParts.any { exprReferencesSymbol(it, name) } ||
            kept.any { exprReferencesSymbol(it.second, name) }
        if (used) kept.add(0, name to init)
    }
    return kept
}

private fun tlaIdentReferenced(text: String, name: String): Boolean =
    Regex("(?<![A-Za-z0-9_])${Regex.escape(name)}(?![A-Za-z0-9_])").containsMatchIn(text)

/**
 * `LET` defs plus `IN` body for [wrapTransitLetBlock]. Consecutive non-IO lets share one `LET`.
 * [firstLinePrefix] is prepended to `LET` only (`/\ ` or indent under `\\E`).
 * Continuation binding / `IN` lines use the same width in spaces so `/\` is not repeated.
 * [compensateModule] adds 2 spaces on continuation lines so they line up after the action's
 * `"\n  "` indent on the first line only.
 */
private fun formatTransitLet(
    defs: List<TransitLetEmit>,
    indentedBody: String,
    firstLinePrefix: String,
    compensateModule: Boolean,
    initPad: String,
): String {
    val extra = if (compensateModule) "  " else ""
    val prefixSpaces = " ".repeat(firstLinePrefix.length)
    val nameContPad = extra + prefixSpaces + "    "
    val inPad = extra + prefixSpaces
    val defText = defs.mapIndexed { idx, b ->
        val one = if (b.initTla.contains('\n')) {
            "${b.tlaName} ==\n${padPreservingRelative(b.initTla, initPad)}"
        } else {
            "${b.tlaName} == ${b.initTla}"
        }
        if (idx == 0) one else "$nameContPad$one"
    }.joinToString("\n")
    return if (defs.size == 1 && !defs[0].initTla.contains('\n')) {
        "${firstLinePrefix}LET $defText IN\n$indentedBody"
    } else {
        "${firstLinePrefix}LET $defText\n${inPad}IN\n$indentedBody"
    }
}

/** Indent [block] by [pad], keeping relative indentation between lines. */
internal fun padPreservingRelative(block: String, pad: String): String {
    if (pad.isEmpty()) return block
    val lines = block.lines()
    val minIndent = lines
        .filter { it.isNotEmpty() }
        .minOfOrNull { it.length - it.trimStart().length }
        ?: 0
    return lines.joinToString("\n") { line ->
        if (line.isEmpty()) {
            line
        } else {
            pad + line.substring(minIndent.coerceAtMost(line.length))
        }
    }
}

/**
 * Emit a Julay `fun` as a TLA+ operator.
 * Parameters that collide with [reservedNames] (VARIABLES, CONSTANTS, …) are renamed (`p_…`).
 */
internal fun emitFunOperatorDef(funNode: FunNode, reservedNames: Set<String> = emptySet()): String {
    val params = funNode.funArgs().actionArgs()
    val taken = reservedNames.toMutableSet()
    val renamed = linkedMapOf<String, String>()
    params.forEach { param ->
        renamed[param.name] = allocTlaName(param.name, taken)
    }
    val header = if (params.isEmpty()) {
        funNode.name()
    } else {
        "${funNode.name()}(${renamed.values.joinToString(", ")})"
    }
    val bodyExpr = funNode.funBody()
    val paramTypes = params.associate { it.name to it.type }
    val body = TlaSymbolTypes.withExtra(paramTypes) {
        exprToTla(
            bodyExpr,
            leafCtx = emptyMap(),
            argNames = renamed.values.toSet(),
            self = null,
            bareStateVars = emptySet(),
            stateVarNames = emptyMap(),
            symbolOverrides = renamed,
            linePrefix = "  ",
            parentPrec = PREC_BOTTOM,
        )
    }
    val retTy = try {
        julayTypeToApalache(funNode.returnType)
    } catch (_: RuntimeException) {
        "Bool"
    }
    val argTys = params.map { julayTypeToApalache(it.type) }
    val typeSig = if (argTys.isEmpty()) "() => $retTy" else "(${argTys.joinToString(", ")}) => $retTy"
    val ann = apalacheTypeComment(typeSig)
    val def = if (body.contains('\n') || isMultiLineExpr(bodyExpr)) {
        "$header ==\n  $body"
    } else {
        "$header == $body"
    }
    return if (ann != null) "$ann\n$def" else def
}

/**
 * Split a guard expression on top-level `&` so each Julay conjunct can be a separate TLA `/\\` line.
 * Nested `&` inside `|` / other operators is left intact.
 */
internal fun flattenTopLevelAnd(expr: ExprNode): List<ExprNode> {
    if (expr is BinaryOpExprNode && expr.op() == "&" && !isDesugaredIff(expr)) {
        return flattenTopLevelAnd(expr.lhsOperand()) + flattenTopLevelAnd(expr.rhsOperand())
    }
    return listOf(expr)
}

internal fun flattenTopLevelOr(expr: ExprNode): List<ExprNode> {
    if (expr is BinaryOpExprNode && expr.op() == "|") {
        return flattenTopLevelOr(expr.lhsOperand()) + flattenTopLevelOr(expr.rhsOperand())
    }
    return listOf(expr)
}

internal fun unwrapParens(expr: ExprNode): ExprNode {
    var e = expr
    while (e is ParenExprNode) e = e.innerExpr()
    return e
}

private fun isBoolLiteral(expr: ExprNode, text: String): Boolean {
    val e = unwrapParens(expr)
    return e is LiteralValueExprNode && e.literalText() == text
}

internal fun isActionArgSymbol(expr: ExprNode, argNames: Set<String>): Boolean {
    val inner = unwrapParens(expr)
    return inner is SymbolValueExprNode && inner.symbol in argNames
}

/**
 * Const-global assigns `x := arg` determine [arg] as the TLA state variable, so the action
 * drops that `\E` parameter and rewrites remaining references to the state name.
 */
internal fun collectGlobalConstArgBinds(
    offers: List<TlaActionOffer>,
    stateVarNames: Map<Pair<String, String>, String>,
): Map<String, String> {
    val out = linkedMapOf<String, String>()
    offers.forEach { offer ->
        val argNames = offer.decl.action.args.map { it.name }.toSet()
        offer.decl.transits.forEach { update ->
            if (update !is TransitUpdate.Assign) return@forEach
            val root = update.transitRootVar()
            if (root !in offer.leaf.globalConstVars) return@forEach
            val rhs = unwrapParens(update.expr)
            if (rhs is SymbolValueExprNode && rhs.symbol in argNames) {
                out[rhs.symbol] = stateTlaName(offer.leaf.tlaName, root, stateVarNames)
            }
        }
    }
    return out
}

/** Negate an `error:` condition for a TLA assumption: flip top-level `~in` / `~=` (`#`) / `~`, else wrap with `~`. */
internal fun negateErrorCondition(expr: ExprNode): ExprNode {
    val inner = unwrapParens(expr)
    return when {
        inner is BinaryOpExprNode && inner.op() == "~in" ->
            BinaryOpExprNode("in", inner.lhsOperand(), inner.rhsOperand(), inner.programLocation())
        inner is BinaryOpExprNode && (inner.op() == "#" || inner.op() == "~=") ->
            BinaryOpExprNode("=", inner.lhsOperand(), inner.rhsOperand(), inner.programLocation())
        inner is UnaryOpExprNode && inner.op() == "~" ->
            inner.operand()
        else -> UnaryOpExprNode("~", expr, expr.programLocation())
    }
}

/** True when a Julay `&` / `|` (or obj literal) spans more than one source line. */
internal fun isMultiLineExpr(expr: ExprNode): Boolean {
    val loc = expr.programLocation()
    return loc is SourceLoc && loc.startLine != loc.endLine
}

/** Indent for continuation body lines of a multi-line IF / LET / WHEN / list / set. */
internal fun exprBodyIndent(linePrefix: String): String = bodyColumnSpaces(linePrefix)

/** Column-aligned indent for keywords like ELSE / IN / closers under a multi-line form. */
internal fun exprKeywordIndent(linePrefix: String): String = openColumnSpaces(linePrefix)

/**
 * Spaces to the conjunct open column (first non-`/\\`/`\\/` symbol on the visual opening line).
 * Shared by IF/LET/WHEN/list/set/obj continuation layout.
 */
internal fun openColumnSpaces(linePrefix: String): String {
    val col = firstNonSlashConjunctColumn(visualObjOpenLinePrefix(linePrefix))
    return " ".repeat((col - 1).coerceAtLeast(0))
}

/** Spaces to open column + 2 (body / field content under a multi-line form). */
internal fun bodyColumnSpaces(linePrefix: String): String {
    val col = firstNonSlashConjunctColumn(visualObjOpenLinePrefix(linePrefix))
    return " ".repeat((col - 1 + 2).coerceAtLeast(0))
}

/**
 * Multi-line Julay `if` → TLA IF/THEN/ELSE with THEN and ELSE bodies on following lines.
 */
internal fun emitMultiLineIf(
    cond: String,
    thenBranch: String,
    elseBranch: String,
    linePrefix: String,
    open: String,
    close: String,
): String {
    val bodyIndent = exprBodyIndent(linePrefix + open)
    val kwIndent = exprKeywordIndent(linePrefix + open)
    return buildString {
        append(open)
        append("IF ")
        append(cond)
        append(" THEN\n")
        append(bodyIndent)
        append(thenBranch)
        append("\n")
        append(kwIndent)
        append("ELSE\n")
        append(bodyIndent)
        append(elseBranch)
        append(close)
    }
}

/**
 * Drop the kept field of a `field-vars` projection (`me.url` → path `[]` when `me` → `me_url`).
 */
internal fun dropProjectedFieldPath(leafClass: String?, varName: String, path: List<String>): List<String> {
    if (leafClass == null || path.isEmpty()) return path
    val proj = TlaFieldVarProjection.get().projection(leafClass, varName) ?: return path
    return if (path.first() == proj.keptField) path.drop(1) else path
}

/**
 * When [assignKey] is a whole-obj write to a `field-vars` projected slot (`pick := found`
 * with `pick` → `pick_url`), emit only the kept field of the RHS so TypeOK stays a scalar.
 */
internal fun emitFieldVarProjectedRhs(
    leafClass: String,
    rootVar: String,
    assignKey: String,
    expr: ExprNode,
    linePrefix: String,
    emitExpr: (ExprNode, String) -> String,
): String {
    if ('.' in assignKey) return emitExpr(expr, linePrefix)
    val proj = TlaFieldVarProjection.get().projection(leafClass, rootVar) ?: return emitExpr(expr, linePrefix)
    val kept = proj.keptField
    if (expr is ObjClassLiteralExprNode) {
        val entry = expr.fieldEntries.firstOrNull { it.first == kept }
        if (entry != null) return emitExpr(entry.second, linePrefix)
    }
    val rhs = emitExpr(expr, linePrefix)
    // unwrap-singletons already emits the kept field's type as a scalar (e.g. emptyNodeInfo == "").
    // Appending ".$kept" would become "".url and crash TLC.
    val rhsType = try {
        expr.getType()
    } catch (_: RuntimeException) {
        null
    }
    if (rhsType is ObjClassType) {
        val single = TlaFieldProjection.get().singletonField(rhsType)
        if (single != null && single.name == kept) return rhs
        // Opaque index-sort atoms: kept field is a projection VARIABLE, not a record select.
        if (TlaOpaqueIndexSort.isOpaque(rhsType)) {
            return emitUnwrappedFieldPath(rhs, rhsType, listOf(kept))
        }
    }
    return "$rhs.$kept"
}

internal fun emitUnwrappedFieldPath(
    base: String,
    type: Type?,
    path: List<String>,
    leafClass: String? = null,
    stateVarName: String? = null,
): String {
    val afterFieldVar =
        if (leafClass != null && stateVarName != null) {
            dropProjectedFieldPath(leafClass, stateVarName, path)
        } else {
            path
        }
    // Opaque index-sort atoms: field access is a projection VARIABLE application.
    if (type is ObjClassType && TlaOpaqueIndexSort.isOpaque(type) && afterFieldVar.isNotEmpty()) {
        var acc = base
        var ty: Type? = type
        for (seg in afterFieldVar) {
            val sort = (ty as? ObjClassType)?.takeIf { TlaOpaqueIndexSort.isOpaque(it) }
            if (sort != null && sort.fields.any { it.name == seg }) {
                acc = "${TlaOpaqueIndexSort.projectionName(sort.name, seg)}[$acc]"
                ty = sort.fields.first { it.name == seg }.type
            } else {
                acc = "$acc.$seg"
                ty = when (val r = ty?.let { resolveFieldPath(it, listOf(seg)) }) {
                    is FieldPathResult.Resolved -> r.type
                    else -> null
                }
            }
        }
        return acc
    }
    val dropped = if (type != null) {
        TlaFieldProjection.get().dropUnwrappedPath(type, afterFieldVar)
    } else {
        afterFieldVar
    }
    return if (dropped.isEmpty()) base else dropped.fold(base) { acc, f -> "$acc.$f" }
}

private fun typeOfBaseExpr(expr: ExprNode): Type? = try {
    expr.getType()
} catch (_: RuntimeException) {
    (expr as? SymbolValueExprNode)?.let { TlaSymbolTypes.get()[it.symbol] }
}

/**
 * Wrap [body] in `LET` binders. Multi-line form indents the `IN` body by 4 spaces so the
 * result stays valid when embedded as a junction conjunct (`/\ LET … IN /\ …`): SANY rejects
 * a `/\\` after `IN` at the same column as that conjunct's `/\\`.
 */
internal fun wrapTlaLet(bindings: List<Pair<String, String>>, body: String): String {
    if (bindings.isEmpty()) return body
    val defs = bindings.joinToString("\n      ") { (n, e) -> "$n == $e" }
    val multiLine = body.contains('\n') || defs.contains('\n')
    return if (multiLine) {
        val indentedBody = padPreservingRelative(body, "    ")
        "LET $defs\n     IN\n$indentedBody"
    } else {
        "LET $defs IN $body"
    }
}

/** `{ x.f : x \in S }` or a UNION of several, from exists-from-projection.
 * After unwrap-singletons, `x.f` may collapse to `x`; emit `S` instead of `{ x : x \in S }`.
 */
internal fun emitProjectedArgDomain(
    bind: ProjectedArgBind,
    emit: (ExprNode) -> String,
): String {
    val parts = bind.sources.map { src ->
        val env = src.param to collectionElementType(src.set)
        TlaSymbolTypes.withExtra(env.second?.let { mapOf(env.first to it) } ?: emptyMap()) {
            val projected = emit(src.projection)
            val set = emit(src.set)
            if (projected == src.param) set else "{ $projected : ${src.param} \\in $set }"
        }
    }
    return if (parts.size == 1) {
        parts.single()
    } else {
        "UNION { ${parts.joinToString(", ")} }"
    }
}

/** Consecutive binders over the same domain become one group. [params] is outermost-first. */
internal fun groupConsecutiveSameDomain(
    params: List<Pair<String, String>>,
): List<Pair<List<String>, String>> {
    val groups = mutableListOf<Pair<MutableList<String>, String>>()
    for ((name, domain) in params) {
        val last = groups.lastOrNull()
        if (last != null && last.second == domain) {
            last.first += name
        } else {
            groups += mutableListOf(name) to domain
        }
    }
    return groups.map { it.first to it.second }
}

/**
 * Consecutive binders over the same domain become `\E n, m \in D : …` (or `\A`).
 * [params] is outermost-first. Multi-line layout puts each group header on its own line.
 */
internal fun wrapTlaQuantifier(
    q: String,
    params: List<Pair<String, String>>,
    body: String,
    multiLine: Boolean = false,
    linePrefix: String = "",
): String {
    if (params.isEmpty()) return body
    val groups = groupConsecutiveSameDomain(params)
    return if (!multiLine) {
        groups.asReversed().fold(body) { acc, (names, domain) ->
            "$q ${names.joinToString(", ")} \\in $domain : $acc"
        }
    } else {
        buildString {
            groups.forEachIndexed { i, (names, domain) ->
                if (i > 0) {
                    append('\n')
                    append(linePrefix)
                    append("  ".repeat(i))
                }
                append("$q ${names.joinToString(", ")} \\in $domain :")
            }
            append('\n')
            append(linePrefix)
            append("  ".repeat(groups.size))
            append(body)
        }
    }
}

/** Consecutive `\E` binders over the same domain become `\E n, m \in D : …`. */
internal fun wrapTlaExists(params: List<Pair<String, String>>, body: String): String =
    wrapTlaQuantifier("\\E", params, body)

/**
 * Like [wrapTlaExists], but `\E x \in BoundedSeq(S, N)` becomes length-first
 * `\E __len_x \in 0..N : \E x \in [Int -> S] : DOMAIN x = 1..__len_x /\ …`
 * so Apalache does not expand `UNION { [1..k -> S] : k \in 0..N }`.
 */
internal fun wrapTlaExistsAvoidingBoundedSeqExpand(
    params: List<Pair<String, String>>,
    body: String,
): String {
    if (params.none { it.second.trim().startsWith("BoundedSeq(") }) {
        return wrapTlaExists(params, body)
    }
    var acc = body
    for ((name, domain) in params.asReversed()) {
        val trimmed = domain.trim()
        val bs = parseBoundedSeqDomain(trimmed)
        if (bs != null) {
            val (elem, _) = bs
            // Never `\E x \in BoundedSeq(...)` / `[1..k -> S]` — Apalache expands those and
            // aborts. Unroll empty + singleton only (enough for StoreAsm / short reshard).
            acc = "((LET $name == [__i \\in {} |-> CHOOSE __e \\in $elem : TRUE] IN ($acc))" +
                " \\/ (\\E __e1 \\in $elem : LET $name == [__i \\in {1} |-> __e1] IN ($acc)))"
        } else {
            acc = "\\E $name \\in $trimmed : $acc"
        }
    }
    return acc
}

/** Parse `BoundedSeq(Elem, Bound)` (Elem may contain commas inside `[…]`). */
internal fun parseBoundedSeqDomain(domain: String): Pair<String, String>? {
    val t = domain.trim()
    if (!t.startsWith("BoundedSeq(") || !t.endsWith(")")) return null
    val inner = t.removePrefix("BoundedSeq(").removeSuffix(")")
    val parts = splitTopLevelComma(inner)
    if (parts.size != 2) return null
    return parts[0].trim() to parts[1].trim()
}

/**
 * Julay `let` → TLA `LET name == init IN body`. Multi-line source uses line breaks;
 * [body] / [init] should already be rendered (with let-name overrides applied to [body]).
 * Consecutive chained lets are one `LET` with multiple bindings.
 */
internal fun emitLetToTla(
    bindings: List<Pair<String, String>>,
    body: String,
    linePrefix: String,
    multiLine: Boolean,
    open: String = "",
    close: String = "",
): String {
    require(bindings.isNotEmpty()) { "emitLetToTla requires bindings" }
    if (bindings.size == 1) {
        val (name, init) = bindings.first()
        if (!multiLine) {
            return "${open}LET $name == $init IN $body$close"
        }
        val bodyIndent = exprBodyIndent(linePrefix + open)
        val kwIndent = exprKeywordIndent(linePrefix + open)
        return if (init.contains('\n')) {
            buildString {
                append(open)
                append("LET $name ==\n")
                append(bodyIndent)
                append(init)
                append("\n")
                append(kwIndent)
                append("IN\n")
                append(bodyIndent)
                append(body)
                append(close)
            }
        } else {
            buildString {
                append(open)
                append("LET $name == $init IN\n")
                append(bodyIndent)
                append(body)
                append(close)
            }
        }
    }
    val kwIndent = exprKeywordIndent(linePrefix + open)
    val bodyIndent = exprBodyIndent(linePrefix + open)
    val nameIndent = kwIndent + "    "
    return buildString {
        append(open)
        append("LET ")
        bindings.forEachIndexed { i, (n, e) ->
            if (i > 0) {
                append("\n")
                append(nameIndent)
            }
            append("$n == $e")
        }
        append("\n")
        append(kwIndent)
        append("IN\n")
        append(bodyIndent)
        append(body)
        append(close)
    }
}

private fun whenLiteralToTla(lit: WhenLiteral): String = when (lit) {
    is WhenLiteral.IntLit -> lit.value
    is WhenLiteral.RealLit -> lit.value
    is WhenLiteral.BoolLit -> if (lit.value == "true") "TRUE" else "FALSE"
    is WhenLiteral.StringLit -> "\"${escapeTlaStringLiteral(lit.value)}\""
}

/**
 * Julay `when` → TLA `CASE` with `[]` arms and `OTHER` for the trailing else.
 */
internal fun emitWhenToTla(
    expr: WhenExprNode,
    render: (ExprNode, String) -> String,
    linePrefix: String,
    parentPrec: Int,
): String {
    val needParen = PREC_IMPLIES < parentPrec
    val open = if (needParen) "(" else ""
    val close = if (needParen) ")" else ""
    val arms = expr.arms()
    val elseArm = arms.last() as? WhenArm.Else
        ?: return "${open}TRUE$close"
    val caseArms = arms.dropLast(1)
    val multi = isMultiLineExpr(expr)
    val contIndent = exprKeywordIndent(linePrefix + open)
    val bodyIndent = exprBodyIndent(linePrefix + open)

    fun predAndBranch(arm: WhenArm): Pair<String, String> = when (arm) {
        is WhenArm.Guard -> {
            val pred = render(arm.cond, "$linePrefix${open}CASE ")
            val branch = render(arm.expr, bodyIndent)
            pred to branch
        }
        is WhenArm.Subject -> {
            val subject = expr.subjectExpr()
                ?: return "TRUE" to render(arm.expr, bodyIndent)
            val subj = render(subject, "$linePrefix${open}CASE ")
            val pat = when (val pattern = arm.pattern) {
                is WhenPattern.Primitive -> whenLiteralToTla(pattern.literal)
                is WhenPattern.Struct -> render(pattern.literal, bodyIndent)
            }
            "$subj = $pat" to render(arm.expr, bodyIndent)
        }
        is WhenArm.Else -> error("else arm not a case arm")
    }

    if (!multi && caseArms.size <= 2) {
        val parts = caseArms.map { arm ->
            val (p, b) = predAndBranch(arm)
            "$p -> $b"
        }
        val elseBody = render(elseArm.expr, linePrefix)
        return "${open}CASE ${parts.joinToString(" [] ")} [] OTHER -> $elseBody$close"
    }

    return buildString {
        append(open)
        append("CASE ")
        caseArms.forEachIndexed { i, arm ->
            val (p, b) = predAndBranch(arm)
            if (i == 0) {
                append(p)
                append(" ->\n")
                append(bodyIndent)
                append(b)
            } else {
                append("\n")
                append(contIndent)
                append("[] ")
                append(p)
                append(" ->\n")
                append(bodyIndent)
                append(b)
            }
        }
        append("\n")
        append(contIndent)
        append("[] OTHER ->\n")
        append(bodyIndent)
        append(render(elseArm.expr, bodyIndent))
        append(close)
    }
}

/**
 * Recursively format multi-line Julay `|` as TLA `\\/` branches.
 * Each branch is introduced by `\\/ `; nested multi-line `&` / `|` are formatted the same way.
 */
internal fun emitMultiLineOr(
    disjuncts: List<ExprNode>,
    linePrefix: String,
    render: (ExprNode, String) -> String,
): String {
    val orOp = "\\/ "
    val visualBefore = visualObjOpenLinePrefix(linePrefix)
    val orContIndent = " ".repeat(visualBefore.length)
    return buildString {
        disjuncts.forEachIndexed { i, d ->
            val prefix = if (i == 0) linePrefix + orOp else orContIndent + orOp
            if (i > 0) {
                append("\n")
                append(orContIndent)
            }
            append(orOp)
            append(emitBoolOperand(d, prefix, render))
        }
    }
}

/**
 * Recursively format multi-line Julay `&` as TLA `/\\` conjuncts.
 */
internal fun emitMultiLineAnd(
    conjuncts: List<ExprNode>,
    linePrefix: String,
    render: (ExprNode, String) -> String,
): String {
    val kept = conjuncts.filter { !TlaSkipConjuncts.skipped(it) }
    if (kept.isEmpty()) return "TRUE"
    val andOp = "/\\ "
    val visualBefore = visualObjOpenLinePrefix(linePrefix)
    val andContIndent = " ".repeat(visualBefore.length)
    return buildString {
        kept.forEachIndexed { i, c ->
            val prefix = if (i == 0) linePrefix + andOp else andContIndent + andOp
            if (i > 0) {
                append("\n")
                append(andContIndent)
            }
            append(andOp)
            append(emitBoolOperand(c, prefix, render))
        }
    }
}

/** Dispatch nested multi-line `&` / `|`; otherwise render as a normal TLA atom/expr. */
internal fun emitBoolOperand(
    expr: ExprNode,
    linePrefix: String,
    render: (ExprNode, String) -> String,
): String {
    if (expr is BinaryOpExprNode && expr.op() == "|" && isMultiLineExpr(expr)) {
        return emitMultiLineOr(flattenTopLevelOr(expr), linePrefix, render)
    }
    if (expr is BinaryOpExprNode && expr.op() == "&" && isMultiLineExpr(expr)) {
        return emitMultiLineAnd(flattenTopLevelAnd(expr), linePrefix, render)
    }
    return render(expr, linePrefix)
}

/** True when the Julay obj literal spans more than one source line. */
internal fun isMultiLineObjLiteral(expr: ObjClassLiteralExprNode): Boolean = isMultiLineExpr(expr)

/**
 * Column (1-based) of the first symbol on [line] that is not part of a leading `/\\` or `\\/` conjunct.
 */
internal fun firstNonSlashConjunctColumn(line: String): Int {
    var i = 0
    while (i < line.length && line[i] == ' ') i++
    when {
        line.startsWith("/\\", i) -> {
            i += 2
            while (i < line.length && line[i] == ' ') i++
            return i + 1
        }
        line.startsWith("\\/", i) -> {
            i += 2
            while (i < line.length && line[i] == ' ') i++
            return i + 1
        }
        else -> return i + 1
    }
}

/**
 * Action conjunct lines are indented two spaces in the module. [linePrefix] is the part-local
 * prefix (usually starting with `/\\ `); continuation lines already use absolute columns.
 */
internal fun visualObjOpenLinePrefix(linePrefix: String): String =
    if (linePrefix.startsWith("/\\") || linePrefix.startsWith("\\/")) "  $linePrefix" else linePrefix

/** TLA operator precedence for paren elision (higher binds tighter). */
internal const val PREC_BOTTOM = 0
internal const val PREC_IMPLIES = 10
internal const val PREC_OR = 20
internal const val PREC_AND = 30
internal const val PREC_REL = 40
internal const val PREC_ADD = 50
internal const val PREC_MUL = 60
internal const val PREC_UNARY = 70
internal const val PREC_ATOM = 100

/**
 * Emit a TLA record for an obj literal. Multi-line Julay inits put each field on its own line,
 * indented 2 spaces past the first non-`/\\`/`\\/` symbol on the opening line.
 */
internal fun emitObjClassLiteralToTla(
    expr: ObjClassLiteralExprNode,
    render: (ExprNode, String) -> String,
    linePrefix: String,
): String {
    val objName = try {
        expr.structType.name
    } catch (_: RuntimeException) {
        expr.className
    }
    val objType = try {
        expr.structType
    } catch (_: RuntimeException) {
        null
    }
    // Opaque index-sort: non-empty String field → atom label; empty/sentinel → outside-live atom.
    if (objType is ObjClassType && TlaOpaqueIndexSort.isOpaque(objType)) {
        val stringField = objType.fields.firstOrNull { it.type is StringType }?.name
        val atomEntry = expr.fieldEntries.firstOrNull { (n, _) -> n == stringField }
            ?: expr.fieldEntries.firstOrNull()
        if (atomEntry != null) {
            val v = atomEntry.second
            val isEmptyAtom = v is LiteralValueExprNode && (
                v.literalText().isEmpty() || v.literalText() == "-1"
            )
            if (!isEmptyAtom) {
                return render(v, linePrefix)
            }
        }
        return TlaOpaqueIndexSort.sentinelTla(objType.name)
    }
    val fieldEntries = TlaFieldProjection.get().filterLiteralEntries(objName, expr.fieldEntries)
    if (fieldEntries.isEmpty()) {
        return "[dummy |-> 0]"
    }
    if (fieldEntries.size == 1 && objType is ObjClassType &&
        TlaFieldProjection.get().singletonField(objType) != null
    ) {
        return render(fieldEntries.single().second, linePrefix)
    }
    if (!isMultiLineObjLiteral(expr) || fieldEntries.isEmpty()) {
        val fields = fieldEntries.joinToString(", ") { (name, e) ->
            "$name |-> ${render(e, linePrefix)}"
        }
        return "[$fields]"
    }
    val fieldIndent = bodyColumnSpaces(linePrefix)
    val closeIndent = openColumnSpaces(linePrefix)
    val fields = fieldEntries.joinToString(",\n$fieldIndent") { (name, e) ->
        val valuePrefix = fieldIndent + "$name |-> "
        "$name |-> ${render(e, valuePrefix)}"
    }
    return "[\n$fieldIndent$fields\n$closeIndent]"
}

/**
 * Julay `in` / `~in`. Sets stay `\in` / `\notin`. Lists emit as TLA sequences (functions),
 * so membership is `Range(xs)` via a generated helper. Maps emit as functions; key
 * membership is `DOMAIN`.
 */
private fun emitInToTla(
    expr: BinaryOpExprNode,
    negated: Boolean,
    linePrefix: String,
    parentPrec: Int,
    rec: (ExprNode, String, Int) -> String,
): String {
    val mid = if (negated) " \\notin " else " \\in "
    val needParen = PREC_REL < parentPrec
    val open = if (needParen) "(" else ""
    val close = if (needParen) ")" else ""
    val lhs = expr.lhsOperand()
    val rhs = expr.rhsOperand()
    val l = rec(lhs, linePrefix + open, PREC_REL)
    return when {
        exprIsListTyped(rhs) -> {
            val r = rec(rhs, linePrefix + open + l + mid + "Range(", PREC_BOTTOM)
            "$open$l${mid}Range($r)$close"
        }
        exprIsMapTyped(rhs) -> {
            val r = rec(rhs, linePrefix + open + l + mid + "DOMAIN ", PREC_UNARY)
            "$open$l${mid}DOMAIN $r$close"
        }
        else -> {
            val r = rec(rhs, linePrefix + open + l + mid, PREC_REL)
            "$open$l$mid$r$close"
        }
    }
}

/** Do not weaken formulas to TRUE when a call has no TLA+ emitter. */
private fun unresolvedTlaCall(kind: String, name: String): Nothing =
    error("internal: no TLA+ emitter for $kind \"$name\" (should have been rejected by type checking)")

internal fun exprToTla(
    expr: ExprNode,
    leafCtx: Map<String, SpecLeaf>,
    argNames: Set<String>,
    self: String?,
    bareStateVars: Set<String> = emptySet(),
    reservedNames: Set<String> = emptySet(),
    stateVarNames: Map<Pair<String, String>, String> = emptyMap(),
    symbolOverrides: Map<String, String> = emptyMap(),
    /** Text on the current TLA line before this expression (e.g. `/\\ p' = `). */
    linePrefix: String = "",
    /** Parent operator precedence; wrap this expr in parens when its prec is lower. */
    parentPrec: Int = PREC_BOTTOM,
    /** Class / TLA leaf name → `global` state vars (omit index on Peer[idx].var). */
    globalByLeaf: Map<String, Set<String>> = emptyMap(),
): String {
    if (TlaSkipConjuncts.skipped(expr)) return "TRUE"
    fun rec(e: ExprNode, prefix: String = linePrefix, prec: Int = parentPrec): String =
        exprToTla(
            e, leafCtx, argNames, self, bareStateVars, reservedNames, stateVarNames, symbolOverrides,
            prefix, prec, globalByLeaf,
        )
    fun recWithOverrides(e: ExprNode, extra: Map<String, String>, prefix: String = linePrefix, prec: Int = parentPrec): String =
        exprToTla(
            e, leafCtx, argNames, self, bareStateVars, reservedNames, stateVarNames,
            symbolOverrides + extra, prefix, prec, globalByLeaf,
        )
    fun stateRead(leaf: SpecLeaf, varName: String): String {
        val v = stateTlaName(leaf.tlaName, varName, stateVarNames)
        return if (self != null && leaf.indexesState(varName)) "$v[$self]" else v
    }
    return when (expr) {
        is LiteralValueExprNode -> literalToTla(expr)
        is SymbolValueExprNode -> {
            val sym = expr.symbol
            when {
                sym in symbolOverrides -> symbolOverrides.getValue(sym)
                sym in argNames -> sym
                sym in bareStateVars -> {
                    val owners = leafCtx.values.toSet().filter { sym in it.globalConstVars }
                    val owner = owners.singleOrNull() ?: leafCtx.values.toSet().singleOrNull()
                    if (owner != null) stateRead(owner, sym) else sym
                }
                else -> {
                    val plan = TlaFluentEmit.get()
                    if (plan != null && sym in plan.byName && TlaFluentEmit.primeFluents()) {
                        "$sym'"
                    } else {
                        sym
                    }
                }
            }
        }
        is ThisAccessExprNode -> {
            // Force state: never treat the root as an action arg even when names collide.
            val root = expr.stateVarName()
            val rest = expr.nestedFieldPath()
            val owner = leafCtx.values.toSet().firstOrNull { leaf ->
                (leaf.tlaName to root) in stateVarNames
            }
            if (owner != null && rest.isEmpty()) {
                return stateRead(owner, root)
            }
            val forcedArgs = argNames - root
            val inner: ExprNode = if (rest.isEmpty()) {
                SymbolValueExprNode(root, expr.programLocation())
            } else {
                FieldAccessExprNode(
                    root,
                    rest,
                    expr.programLocation(),
                    expr.resolvedLeafTypeOrNull(),
                    expr.resolvedRelPathOrNull(),
                )
            }
            exprToTla(
                inner, leafCtx, forcedArgs, self, bareStateVars, reservedNames, stateVarNames,
                symbolOverrides - root, linePrefix, parentPrec, globalByLeaf,
            )
        }
        is ListLiteralExprNode -> {
            // Emit TLA sequences (`<<…>>`); `[1 |-> …]` is not valid (fields must be ids).
            if (expr.elements.isEmpty()) {
                "<<>>"
            } else if (isMultiLineExpr(expr)) {
                val bodyIndent = exprBodyIndent(linePrefix)
                val closeIndent = exprKeywordIndent(linePrefix)
                buildString {
                    append("<<\n")
                    expr.elements.forEachIndexed { idx, el ->
                        append(bodyIndent)
                        append(rec(el, bodyIndent, PREC_BOTTOM))
                        if (idx < expr.elements.lastIndex) append(",")
                        append("\n")
                    }
                    append(closeIndent)
                    append(">>")
                }
            } else {
                "<<${expr.elements.joinToString(", ") { rec(it) }}>>"
            }
        }
        is LetExprNode -> {
            val needParen = PREC_IMPLIES < parentPrec
            val open = if (needParen) "(" else ""
            val close = if (needParen) ")" else ""
            val chain = mutableListOf<Pair<String, ExprNode>>()
            var cur: ExprNode = expr
            var anyMulti = false
            while (cur is LetExprNode) {
                val rawName = cur.letName()
                if (isMultiLineExpr(cur)) anyMulti = true
                if (!rawName.isDiscardBinding()) {
                    chain += rawName to cur.letInitExpr()
                    if (isMultiLineExpr(cur.letInitExpr())) anyMulti = true
                }
                cur = cur.bodyExpr()
            }
            if (isMultiLineExpr(cur)) anyMulti = true
            if (chain.isEmpty()) {
                return rec(cur, linePrefix, parentPrec)
            }
            val kept = if (TlaEmitOpts.get().unusedLets) pruneUnusedExprLets(chain, cur) else chain
            if (kept.isEmpty()) {
                return rec(cur, linePrefix, parentPrec)
            }
            val extra = mutableMapOf<String, String>()
            val rendered = mutableListOf<Pair<String, String>>()
            val kwIndent = exprKeywordIndent(linePrefix + open)
            val nameIndent = kwIndent + "    "
            kept.forEachIndexed { i, (name, initExpr) ->
                val initPrefix = when {
                    anyMulti && isMultiLineExpr(initExpr) -> exprBodyIndent(linePrefix + open)
                    i == 0 -> "$linePrefix${open}LET $name == "
                    else -> "$nameIndent$name == "
                }
                val init = recWithOverrides(initExpr, extra.toMap(), initPrefix, PREC_BOTTOM)
                rendered += name to init
                extra[name] = name
            }
            val multi = anyMulti || rendered.size > 1
            val bodyPrefix = if (multi) exprBodyIndent(linePrefix + open) else {
                val defs = rendered.joinToString(" ") { (n, e) -> "$n == $e" }
                "$linePrefix${open}LET $defs IN "
            }
            val body = recWithOverrides(cur, extra.toMap(), bodyPrefix, PREC_BOTTOM)
            emitLetToTla(rendered, body, linePrefix, multi, open, close)
        }
        is FieldAccessExprNode -> {
            val base = expr.baseSymbol
            if (expr.fieldPath == listOf("length")) {
                val baseRendered = when {
                    base in symbolOverrides -> symbolOverrides.getValue(base)
                    base in argNames -> base
                    base in bareStateVars -> {
                        val owners = leafCtx.values.toSet().filter { base in it.globalConstVars }
                        val owner = owners.singleOrNull() ?: leafCtx.values.toSet().singleOrNull()
                        if (owner != null) stateRead(owner, base) else base
                    }
                    else -> base
                }
                val ty = TlaSymbolTypes.get()[base]
                    ?: TlaOpaqueIndexSort.sorts()[base]
                return tlaLengthOfType(baseRendered, ty)
            }
            if (expr.fieldPath.isNotEmpty()) {
                val varName = expr.fieldPath[0]
                val leafVar = stateVarNames[base to varName]
                    ?: leafCtx[base]?.let { stateTlaName(it.tlaName, varName, stateVarNames) }
                if (leafVar != null && base !in argNames && base !in bareStateVars && base !in symbolOverrides) {
                    val peerLeaf = leafCtx[base]?.name ?: base
                    val rest = dropProjectedFieldPath(peerLeaf, varName, expr.fieldPath.drop(1))
                    return if (rest.isEmpty()) {
                        leafVar
                    } else {
                        emitPeerPropPath(leafVar, TlaSymbolTypes.get()[varName], rest)
                    }
                }
            }
            // Lambda / HOF binder substitution (e.g. map(e -> e.value)).
            if (base in symbolOverrides) {
                val baseRendered = symbolOverrides.getValue(base)
                return emitPeerPropPath(baseRendered, TlaSymbolTypes.get()[base], expr.fieldPath)
            }
            val isObjectField = base in argNames || base in bareStateVars
            val leafVarName = expr.fieldPath.singleOrNull()?.let { stateVarNames[base to it] }
            if (!isObjectField && leafVarName != null) {
                // Invariant / non-param Leaf.var
                leafVarName
            } else {
                // Object field on arg or state var: base.field… (with indexing when needed)
                val stateLeaf = when {
                    base in bareStateVars ->
                        leafCtx.values.toSet().filter { base in it.globalConstVars }
                            .singleOrNull()
                            ?: leafCtx.values.toSet().singleOrNull()
                    else -> leafCtx.values.singleOrNull()
                }
                val baseRendered = when {
                    base in argNames -> base
                    base in bareStateVars && stateLeaf != null -> {
                        stateRead(stateLeaf, base)
                    }
                    else -> base
                }
                val path = dropProjectedFieldPath(stateLeaf?.name, base, expr.fieldPath)
                if (path.isEmpty()) {
                    baseRendered
                } else {
                    emitPeerPropPath(baseRendered, TlaSymbolTypes.get()[base], path)
                }
            }
        }
        is MemberAccessExprNode -> {
            val base = expr.baseExpr
            if (expr.fieldName == "length") {
                return tlaLengthOf(rec(base), base)
            }
            if (expr.fieldName == "keys") {
                return "DOMAIN ${rec(base)}"
            }
            when {
                base is SymbolValueExprNode && base.symbol in symbolOverrides ->
                    emitUnwrappedFieldPath(
                        symbolOverrides.getValue(base.symbol),
                        TlaSymbolTypes.get()[base.symbol],
                        listOf(expr.fieldName),
                    )
                // Dual-index: Peer[n][d].field → var[n][d] (and constructed for dynamic leaves)
                base is IndexExprNode && base.base is IndexExprNode &&
                    (base.base as IndexExprNode).base is SymbolValueExprNode -> {
                    val outer = base.base as IndexExprNode
                    val leafName = (outer.base as SymbolValueExprNode).symbol
                    val peer = leafCtx[leafName]
                    val mapped = stateVarNames[leafName to expr.fieldName]
                        ?: peer?.let { stateVarNames[it.tlaName to expr.fieldName] }
                    if (mapped != null && leafName !in argNames && leafName !in bareStateVars) {
                        val globals = globalByLeaf[leafName].orEmpty() +
                            (peer?.let { globalByLeaf[it.tlaName].orEmpty() } ?: emptySet())
                        if (expr.fieldName in globals) {
                            mapped
                        } else {
                            "$mapped[${rec(outer.index)}][${rec(base.index)}]"
                        }
                    } else {
                        val stateLeaf = leafCtx.values.singleOrNull()
                        emitUnwrappedFieldPath(
                            rec(base),
                            typeOfBaseExpr(base),
                            listOf(expr.fieldName),
                            leafClass = stateLeaf?.name,
                            stateVarName = leafName,
                        )
                    }
                }
                base is IndexExprNode && base.base is SymbolValueExprNode -> {
                    val leafName = (base.base as SymbolValueExprNode).symbol
                    val peer = leafCtx[leafName]
                    val mapped = stateVarNames[leafName to expr.fieldName]
                        ?: peer?.let { stateVarNames[it.tlaName to expr.fieldName] }
                    if (mapped != null && leafName !in argNames && leafName !in bareStateVars) {
                        val globals = globalByLeaf[leafName].orEmpty() +
                            (peer?.let { globalByLeaf[it.tlaName].orEmpty() } ?: emptySet())
                        if (expr.fieldName in globals) {
                            mapped
                        } else {
                            "$mapped[${rec(base.index)}]"
                        }
                    } else {
                        val stateLeaf = leafCtx.values.singleOrNull()
                        val idxBase = (base.base as SymbolValueExprNode).symbol
                        emitUnwrappedFieldPath(
                            rec(base),
                            typeOfBaseExpr(base),
                            listOf(expr.fieldName),
                            leafClass = stateLeaf?.name,
                            stateVarName = idxBase,
                        )
                    }
                }
                base is SymbolValueExprNode && stateVarNames.containsKey(base.symbol to expr.fieldName) ->
                    stateTlaName(base.symbol, expr.fieldName, stateVarNames)
                else -> {
                    val rendered = rec(base)
                    val stateLeaf = leafCtx.values.singleOrNull()
                    val varName = (base as? SymbolValueExprNode)?.symbol
                    emitUnwrappedFieldPath(
                        rendered,
                        typeOfBaseExpr(base),
                        listOf(expr.fieldName),
                        leafClass = stateLeaf?.name,
                        stateVarName = varName,
                    )
                }
            }
        }
        is FieldAccessOnExprNode -> {
            if (expr.fieldPath == listOf("length")) {
                return tlaLengthOf(rec(expr.baseExpr), expr.baseExpr)
            }
            val base = rec(expr.baseExpr)
            val stateLeaf = leafCtx.values.singleOrNull()
            val varName = when (val b = expr.baseExpr) {
                is SymbolValueExprNode -> b.symbol
                is IndexExprNode -> (b.base as? SymbolValueExprNode)?.symbol
                else -> null
            }
            emitUnwrappedFieldPath(
                base,
                typeOfBaseExpr(expr.baseExpr),
                expr.fieldPath,
                leafClass = stateLeaf?.name,
                stateVarName = varName,
            )
        }
        is FunCallExprNode -> {
            when (expr.callName()) {
                "length" -> {
                    val arg = expr.callArgs().singleOrNull()
                        ?: unresolvedTlaCall("function", "length")
                    tlaLengthOf(rec(arg), arg)
                }
                "splice" -> {
                    val args = expr.callArgs()
                    if (args.size != 3) unresolvedTlaCall("function", "splice")
                    "splice(${rec(args[0])}, ${rec(args[1])}, ${rec(args[2])})"
                }
                "allDistinct" -> {
                    val arg = expr.callArgs().singleOrNull()
                        ?: unresolvedTlaCall("function", "allDistinct")
                    "allDistinct(${rec(arg)})"
                }
                "map" -> {
                    // map(xs, f) — prefer method form; support freestanding.
                    val args = expr.callArgs()
                    if (args.size < 2) unresolvedTlaCall("function", "map")
                    val xs = args[0]
                    val f = args[1]
                    emitMapToTla(xs, f, ::rec, ::recWithOverrides)
                }
                else -> {
                    val fluentPlan = TlaFluentEmit.get()
                    if (fluentPlan != null && expr.callName() in fluentPlan.byName) {
                        val args = expr.callArgs().joinToString(", ") { rec(it, linePrefix, PREC_BOTTOM) }
                        val app = if (args.isEmpty()) expr.callName() else "${expr.callName()}[$args]"
                        if (TlaFluentEmit.primeFluents()) "$app'" else app
                    } else {
                        val userFun = expr.resolvedFunOrNull()
                        if (userFun != null) {
                            val args = expr.callArgs().joinToString(", ") { rec(it, linePrefix, PREC_BOTTOM) }
                            if (args.isEmpty()) userFun.name() else "${userFun.name()}($args)"
                        } else {
                            when (expr.resolvedBuiltinOrNull()?.name ?: expr.callName()) {
                                "startsWith" -> {
                                    val args = expr.callArgs()
                                    if (args.size != 2) unresolvedTlaCall("function", "startsWith")
                                    "startsWith(${rec(args[0], linePrefix, PREC_BOTTOM)}, ${rec(args[1], linePrefix, PREC_BOTTOM)})"
                                }
                                else -> unresolvedTlaCall("function", expr.callName())
                            }
                        }
                    }
                }
            }
        }
        is MapLiteralExprNode -> {
            if (expr.entries.isEmpty()) {
                val valDefault = try {
                    when (val ty = expr.getType()) {
                        is MapType -> defaultTlaValue(ty.valueType)
                        else -> "0"
                    }
                } catch (_: RuntimeException) {
                    "0"
                }
                "[__k \\in {} |-> $valDefault]"
            } else {
                // TLC `:>` / `@@` (EXTENDS TLC).
                expr.entries.joinToString(" @@ ") { (k, v) ->
                    "(${rec(k)} :> ${rec(v)})"
                }
            }
        }
        is MethodCallExprNode -> {
            when (expr.methodName) {
                "map" -> {
                    val f = expr.args.singleOrNull()
                        ?: expr.hofBodyOrNull()?.let { /* lambda already resolved into hof */ null }
                    val hofBody = expr.hofBodyOrNull()
                    val hofParams = expr.hofParamNamesOrNull()
                    when {
                        hofBody != null && hofParams != null && hofParams.size == 1 ->
                            TlaSymbolTypes.withExtra(hofBinderTypes(listOf(hofParams[0]), expr, expr.baseExpr)) {
                                emitMapLambdaToTla(expr.baseExpr, hofParams[0], hofBody, ::rec, ::recWithOverrides)
                            }
                        f != null ->
                            emitMapToTla(expr.baseExpr, f, ::rec, ::recWithOverrides)
                        else -> unresolvedTlaCall("method", "map")
                    }
                }
                "filter" -> {
                    val hofBody = expr.hofBodyOrNull()
                    val hofParams = expr.hofParamNamesOrNull()
                    if (hofBody == null || hofParams == null || hofParams.size != 1) {
                        unresolvedTlaCall("method", "filter")
                    }
                    val p = hofParams[0]
                    val xs = rec(expr.baseExpr)
                    TlaSymbolTypes.withExtra(hofBinderTypes(listOf(p), expr, expr.baseExpr)) {
                        when (try { expr.baseExpr.getType() } catch (_: RuntimeException) { null }) {
                            is ListType -> {
                                val pred = recWithOverrides(hofBody, mapOf(p to p))
                                "SelectSeq($xs, LAMBDA $p: $pred)"
                            }
                            is SetType -> {
                                val pred = recWithOverrides(hofBody, mapOf(p to p))
                                "{ $p \\in $xs : $pred }"
                            }
                            else -> unresolvedTlaCall("method", "filter")
                        }
                    }
                }
                "toSet" -> "Range(${rec(expr.baseExpr)})"
                "toList" -> "SetToSeq(${rec(expr.baseExpr)})"
                "remove" -> {
                    val keyArg = expr.args.singleOrNull()
                    if (keyArg == null) {
                        unresolvedTlaCall("method", "remove")
                    } else {
                        val mp = rec(expr.baseExpr)
                        val key = rec(keyArg)
                        "[__k \\in DOMAIN $mp \\ {$key} |-> $mp[__k]]"
                    }
                }
                "associateWith" -> {
                    val hofBody = expr.hofBodyOrNull()
                    val hofParams = expr.hofParamNamesOrNull()
                    if (hofBody == null || hofParams == null || hofParams.size != 1) {
                        unresolvedTlaCall("method", "associateWith")
                    } else {
                        emitAssociateWithToTla(expr.baseExpr, hofParams[0], hofBody, ::rec, ::recWithOverrides)
                    }
                }
                else -> unresolvedTlaCall("method", expr.methodName)
            }
        }
        is ObjClassLiteralExprNode ->
            emitObjClassLiteralToTla(expr, { e, p -> rec(e, p, PREC_BOTTOM) }, linePrefix)
        is SetLiteralExprNode -> {
            if (expr.elements.isEmpty()) {
                "{}"
            } else if (isMultiLineExpr(expr)) {
                val bodyIndent = exprBodyIndent(linePrefix)
                val closeIndent = exprKeywordIndent(linePrefix)
                buildString {
                    append("{\n")
                    expr.elements.forEachIndexed { idx, el ->
                        append(bodyIndent)
                        append(rec(el, bodyIndent, PREC_BOTTOM))
                        if (idx < expr.elements.lastIndex) append(",")
                        append("\n")
                    }
                    append(closeIndent)
                    append("}")
                }
            } else {
                buildString {
                    append("{")
                    var before = "$linePrefix{"
                    expr.elements.forEachIndexed { idx, el ->
                        if (idx > 0) {
                            append(", ")
                            before += ", "
                        }
                        val rendered = rec(el, before, PREC_BOTTOM)
                        append(rendered)
                        before += rendered
                    }
                    append("}")
                }
            }
        }
        is IndexExprNode -> {
            val idx = rec(expr.index, linePrefix, PREC_BOTTOM)
            val baseTla = rec(expr.base, linePrefix, PREC_ATOM)
            // Map reads require explicit membership at the Julay source; emit bare apply.
            "$baseTla[$idx]"
        }
        is UnaryOpExprNode -> {
            when (expr.op()) {
                "~" -> {
                    // Include `~` in linePrefix so multi-line `\/` / `/\` continuations
                    // line up with the first junction after `~(` / `~`.
                    val o = rec(expr.operand(), "$linePrefix~", PREC_UNARY)
                    val s = "~$o"
                    if (PREC_UNARY < parentPrec) "($s)" else s
                }
                "-" -> {
                    val o = rec(expr.operand(), "$linePrefix(-", PREC_UNARY)
                    val s = "-$o"
                    if (PREC_UNARY < parentPrec) "($s)" else s
                }
                else -> {
                    val o = rec(expr.operand())
                    "(${expr.op()} $o)"
                }
            }
        }
        is ParenExprNode -> {
            val inner = rec(expr.innerExpr(), "$linePrefix(", PREC_BOTTOM)
            "($inner)"
        }
        is BinaryOpExprNode -> {
            fun bin(mid: String, prec: Int, rightPrec: Int = prec): String {
                val needParen = prec < parentPrec
                val open = if (needParen) "(" else ""
                val close = if (needParen) ")" else ""
                val l = rec(expr.lhsOperand(), linePrefix + open, prec)
                val r = rec(expr.rhsOperand(), linePrefix + open + l + mid, rightPrec)
                return "$open$l$mid$r$close"
            }
            fun binWithDynConstructed(mid: String, prec: Int, rightPrec: Int = prec): String {
                val core = bin(mid, prec, rightPrec)
                val guards = collectDynConstructedGuards(
                    expr.lhsOperand(), leafCtx, stateVarNames,
                ) { idx -> rec(idx, linePrefix, PREC_BOTTOM) } +
                    collectDynConstructedGuards(
                        expr.rhsOperand(), leafCtx, stateVarNames,
                    ) { idx -> rec(idx, linePrefix, PREC_BOTTOM) }
                return wrapWithDynConstructedGuards(core, guards, parentPrec)
            }
            when (expr.op()) {
                "&" -> {
                    val all = flattenTopLevelAnd(expr)
                    val conjuncts = all.filter { !TlaSkipConjuncts.skipped(it) }
                    when {
                        conjuncts.isEmpty() -> "TRUE"
                        conjuncts.size != all.size && conjuncts.size == 1 ->
                            rec(conjuncts.single(), linePrefix, parentPrec)
                        conjuncts.size != all.size -> {
                            if (isMultiLineExpr(expr)) {
                                emitMultiLineAnd(conjuncts, linePrefix) { e, p ->
                                    rec(e, p, PREC_AND)
                                }
                            } else {
                                conjuncts.joinToString(" /\\ ") { rec(it, linePrefix, PREC_AND) }
                            }
                        }
                        isMultiLineExpr(expr) -> {
                            emitMultiLineAnd(all, linePrefix) { e, p ->
                                rec(e, p, PREC_AND)
                            }
                        }
                        else -> bin(" /\\ ", PREC_AND)
                    }
                }
                "|" -> {
                    if (isMultiLineExpr(expr)) {
                        emitMultiLineOr(flattenTopLevelOr(expr), linePrefix) { e, p ->
                            rec(e, p, PREC_OR)
                        }
                    } else {
                        bin(" \\/ ", PREC_OR)
                    }
                }
                "=>" -> bin(" => ", PREC_IMPLIES, rightPrec = PREC_IMPLIES)
                "=" -> binWithDynConstructed(" = ", PREC_REL)
                "~=" -> binWithDynConstructed(" # ", PREC_REL)
                "<" -> binWithDynConstructed(" < ", PREC_REL)
                "<=" -> binWithDynConstructed(" <= ", PREC_REL)
                ">" -> binWithDynConstructed(" > ", PREC_REL)
                ">=" -> binWithDynConstructed(" >= ", PREC_REL)
                "in" -> {
                    val core = emitInToTla(expr, negated = false, linePrefix, parentPrec, ::rec)
                    val guards = collectDynConstructedGuards(
                        expr.lhsOperand(), leafCtx, stateVarNames,
                    ) { idx -> rec(idx, linePrefix, PREC_BOTTOM) } +
                        collectDynConstructedGuards(
                            expr.rhsOperand(), leafCtx, stateVarNames,
                        ) { idx -> rec(idx, linePrefix, PREC_BOTTOM) }
                    wrapWithDynConstructedGuards(core, guards, parentPrec)
                }
                "~in" -> {
                    val core = emitInToTla(expr, negated = true, linePrefix, parentPrec, ::rec)
                    val guards = collectDynConstructedGuards(
                        expr.lhsOperand(), leafCtx, stateVarNames,
                    ) { idx -> rec(idx, linePrefix, PREC_BOTTOM) } +
                        collectDynConstructedGuards(
                            expr.rhsOperand(), leafCtx, stateVarNames,
                        ) { idx -> rec(idx, linePrefix, PREC_BOTTOM) }
                    wrapWithDynConstructedGuards(core, guards, parentPrec)
                }
                "+" -> {
                    when {
                        exprIsSetTyped(expr) || exprIsSetTyped(expr.lhsOperand()) || exprIsSetTyped(expr.rhsOperand()) ->
                            bin(" \\cup ", PREC_ADD)
                        exprIsListTyped(expr) || exprIsListTyped(expr.lhsOperand()) || exprIsListTyped(expr.rhsOperand()) ->
                            bin(" \\o ", PREC_ADD)
                        exprIsStringTyped(expr) ||
                            exprIsStringTyped(expr.lhsOperand()) ||
                            exprIsStringTyped(expr.rhsOperand()) -> {
                            val lhs = expr.lhsOperand()
                            val rhs = expr.rhsOperand()
                            when {
                                isEmptyStringLiteral(lhs) && isEmptyStringLiteral(rhs) -> "\"\""
                                isEmptyStringLiteral(lhs) -> {
                                    val r = rec(rhs, linePrefix, PREC_BOTTOM)
                                    tlaStringCoerce(rhs, r)
                                }
                                isEmptyStringLiteral(rhs) -> {
                                    val l = rec(lhs, linePrefix, PREC_BOTTOM)
                                    tlaStringCoerce(lhs, l)
                                }
                                else -> bin(" \\o ", PREC_ADD).let {
                                    // string concat uses \\o; rebuild with coerce
                                    val needParen = PREC_ADD < parentPrec
                                    val open = if (needParen) "(" else ""
                                    val close = if (needParen) ")" else ""
                                    val l = rec(lhs, linePrefix + open, PREC_ADD)
                                    val ls = tlaStringCoerce(lhs, l)
                                    val r = rec(rhs, linePrefix + open + ls + " \\o ", PREC_ADD)
                                    "$open$ls \\o ${tlaStringCoerce(rhs, r)}$close"
                                }
                            }
                        }
                        else -> bin(" + ", PREC_ADD)
                    }
                }
                "-" -> {
                    if (exprIsSetTyped(expr) || exprIsSetTyped(expr.lhsOperand())) {
                        bin(" \\ ", PREC_ADD)
                    } else {
                        bin(" - ", PREC_ADD)
                    }
                }
                "*" -> bin(" * ", PREC_MUL)
                "/" -> bin(" \\div ", PREC_MUL)
                else -> bin(" ${expr.op()} ", PREC_REL)
            }
        }
        is QuantifiedExprNode -> {
            val universal = expr.isUniversal()
            val q = if (universal) "\\A" else "\\E"
            val binders = mutableListOf<Pair<String, String>>()
            val renames = mutableListOf<Pair<String, String>>()
            val binderTypes = linkedMapOf<String, Type>()
            var cur: ExprNode = expr
            var multiLine = false
            while (cur is QuantifiedExprNode && cur.isUniversal() == universal) {
                if (isMultiLineExpr(cur)) multiLine = true
                val domain = when (val t = cur.binderTypeExpr()) {
                    is TypeExpr.Simple -> t.name
                    else -> t.toString()
                }
                val origBinder = cur.binderName()
                val binder = if (origBinder in reservedNames) "q_$origBinder" else origBinder
                binders += binder to domain
                if (binder != origBinder) renames += origBinder to binder
                // So `n.url` on an opaque index-sort binder becomes Node_url[n].
                TlaOpaqueIndexSort.sorts()[domain]?.let { binderTypes[binder] = it }
                cur = cur.quantifiedBody()
            }
            fun fixBinders(s: String): String {
                var out = s
                for ((orig, binder) in renames) {
                    out = out.replace(Regex("\\b${Regex.escape(orig)}\\b"), binder)
                }
                return out
            }
            fun emitBody(prefix: String): String =
                TlaSymbolTypes.withExtra(binderTypes) {
                    fixBinders(rec(cur, prefix, PREC_BOTTOM))
                }
            if (multiLine) {
                val groups = groupConsecutiveSameDomain(binders)
                val bodyPrefix = linePrefix + "  ".repeat(groups.size)
                val body = emitBody(bodyPrefix)
                wrapTlaQuantifier(q, binders, body, multiLine = true, linePrefix = linePrefix)
            } else {
                val header = groupConsecutiveSameDomain(binders).joinToString(" ") { (names, domain) ->
                    "$q ${names.joinToString(", ")} \\in $domain :"
                }
                val body = emitBody("$linePrefix$header ")
                "$header $body"
            }
        }
        is IfElseExprNode -> {
            // IF P THEN FALSE ELSE TRUE → ~P; IF P THEN TRUE ELSE FALSE → P.
            when {
                isBoolLiteral(expr.thenExpr(), "false") && isBoolLiteral(expr.elseExpr(), "true") ->
                    rec(
                        UnaryOpExprNode(
                            "~",
                            // Multiline `\/` emit ignores parentPrec; parens keep `~` over the whole cond.
                            ParenExprNode(expr.condExpr(), expr.programLocation()),
                            expr.programLocation(),
                        ),
                        linePrefix,
                        parentPrec,
                    )
                isBoolLiteral(expr.thenExpr(), "true") && isBoolLiteral(expr.elseExpr(), "false") ->
                    rec(expr.condExpr(), linePrefix, parentPrec)
                else -> {
                    val needParen = PREC_IMPLIES < parentPrec // IF binds loosely; paren when nested tightly
                    val open = if (needParen) "(" else ""
                    val close = if (needParen) ")" else ""
                    if (isMultiLineExpr(expr)) {
                        val c = rec(expr.condExpr(), "$linePrefix${open}IF ", PREC_BOTTOM)
                        val bodyPrefix = exprBodyIndent(linePrefix + open)
                        val t = rec(expr.thenExpr(), bodyPrefix, PREC_BOTTOM)
                        val e = rec(expr.elseExpr(), bodyPrefix, PREC_BOTTOM)
                        emitMultiLineIf(c, t, e, linePrefix, open, close)
                    } else {
                        val c = rec(expr.condExpr(), "$linePrefix$open", PREC_BOTTOM)
                        val t = rec(expr.thenExpr(), "$linePrefix${open}IF $c THEN ", PREC_BOTTOM)
                        val e = rec(expr.elseExpr(), "$linePrefix${open}IF $c THEN $t ELSE ", PREC_BOTTOM)
                        "${open}IF $c THEN $t ELSE $e$close"
                    }
                }
            }
        }
        is WhenExprNode -> emitWhenToTla(expr, { e, p -> rec(e, p, PREC_BOTTOM) }, linePrefix, parentPrec)
        else -> "TRUE"
    }
}

private fun collectionElementType(expr: ExprNode): Type? =
    try {
        when (val t = expr.getType()) {
            is ListType -> t.elementType
            is SetType -> t.elementType
            else -> null
        }
    } catch (_: RuntimeException) {
        null
    }

private fun hofBinderTypes(params: List<String>, call: MethodCallExprNode, xsExpr: ExprNode): Map<String, Type> {
    val declared = call.hofParamTypesOrNull()
    val fallback = collectionElementType(xsExpr)
    val out = mutableMapOf<String, Type>()
    params.forEachIndexed { i, p ->
        (declared?.getOrNull(i) ?: fallback)?.let { out[p] = it }
    }
    return out
}

private fun emitMapLambdaToTla(
    xsExpr: ExprNode,
    param: String,
    body: ExprNode,
    rec: (ExprNode) -> String,
    recWithOverrides: (ExprNode, Map<String, String>) -> String,
): String {
    val xs = rec(xsExpr)
    val env = param to collectionElementType(xsExpr)
    return TlaSymbolTypes.withExtra(env.second?.let { mapOf(env.first to it) } ?: emptyMap()) {
        when (try { xsExpr.getType() } catch (_: RuntimeException) { null }) {
            is ListType -> {
                val i = "__i"
                val elem = "$xs[$i]"
                val mapped = recWithOverrides(body, mapOf(param to elem))
                "[$i \\in DOMAIN $xs |-> $mapped]"
            }
            is SetType -> {
                val mapped = recWithOverrides(body, mapOf(param to param))
                "{ $mapped : $param \\in $xs }"
            }
            else -> "TRUE"
        }
    }
}

private fun emitAssociateWithToTla(
    xsExpr: ExprNode,
    param: String,
    body: ExprNode,
    rec: (ExprNode) -> String,
    recWithOverrides: (ExprNode, Map<String, String>) -> String,
): String {
    val xs = rec(xsExpr)
    val k = "__k"
    val env = param to collectionElementType(xsExpr)
    val mapped = TlaSymbolTypes.withExtra(env.second?.let { mapOf(env.first to it) } ?: emptyMap()) {
        recWithOverrides(body, mapOf(param to k))
    }
    return "[$k \\in $xs |-> $mapped]"
}

private fun emitMapToTla(
    xsExpr: ExprNode,
    funArg: ExprNode,
    rec: (ExprNode) -> String,
    recWithOverrides: (ExprNode, Map<String, String>) -> String,
): String {
    when (funArg) {
        is LambdaExprNode -> {
            val p = funArg.params.singleOrNull() ?: return "TRUE"
            return emitMapLambdaToTla(xsExpr, p, funArg.body, rec, recWithOverrides)
        }
        else -> {
            // Named fun or other: apply as FunCall if we can resolve — fall back TRUE.
            return "TRUE"
        }
    }
}

private fun literalToTla(expr: LiteralValueExprNode): String {
    return when {
        expr.getType() is StringType -> "\"${escapeTlaStringLiteral(expr.literalText())}\""
        else -> when (val s = expr.toTransitString(emptyMap(), emptySet())) {
            "true" -> "TRUE"
            "false" -> "FALSE"
            else -> s
        }
    }
}
