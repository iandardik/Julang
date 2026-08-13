package julay.compiler.pass

import julay.compiler.CompilationUnit
import julay.compiler.FunBuiltinRegistry
import julay.compiler.OneLocCompileWarning
import julay.compiler.SourceLoc
import julay.compiler.TypeExpr
import julay.compiler.ast.*
import julay.compiler.decl.*
import julay.compiler.isDiscardBinding
import julay.program.action.TSAction
import julay.program.type.*
import java.io.File

data class TlaCodegenResult(
    val moduleName: String,
    val tlaText: String,
    val cfgText: String,
    val warnings: List<OneLocCompileWarning> = emptyList(),
)

internal object TlaEmitOpts {
    private val current = ThreadLocal.withInitial { TlaOptConfig.ALL_ON }
    fun get(): TlaOptConfig = current.get()
    fun set(config: TlaOptConfig) {
        current.set(config)
    }
}

internal object TlaSymbolTypes {
    private val current = ThreadLocal.withInitial { emptyMap<String, Type>() }
    fun get(): Map<String, Type> = current.get()
    fun set(types: Map<String, Type>) {
        current.set(types)
    }
}

fun compileSpecToTla(
    spec: SpecNode,
    ast: RootNode,
    unit: CompilationUnit,
    outputDir: File = File("."),
    tlaOptConfig: TlaOptConfig = TlaOptConfig.ALL_ON,
): TlaCodegenResult {
    val result = tlaCodegenPass(spec, ast, unit, tlaOptConfig)
    result.warnings.forEach { System.err.println(it) }
    File(outputDir, "${result.moduleName}.tla").writeText(result.tlaText)
    File(outputDir, "${result.moduleName}.cfg").writeText(result.cfgText)
    return result
}

fun tlaCodegenPass(
    spec: SpecNode,
    ast: RootNode,
    unit: CompilationUnit,
    tlaOptConfig: TlaOptConfig = TlaOptConfig.ALL_ON,
): TlaCodegenResult {
    val moduleName = spec.specNodeName()
    val leafSpecNodes = unit.modules
        .flatMap { it.root.declNodes().filterIsInstance<LeafSpecNode>() }
        .associateBy { it.name() }
    val pclassNodes = unit.modules
        .flatMap { it.root.declNodes().filterIsInstance<ProcClassNode>() }
        .associateBy { it.name() } + leafSpecNodes.mapValues { it.value.asProcClass() }
    val procFunNodes = unit.modules
        .flatMap { it.root.declNodes().filterIsInstance<ProcFunNode>() }
        .associateBy { it.name() }
    val procAliases = unit.modules
        .flatMap { it.root.declNodes().filterIsInstance<ProcNode>() }
        .associateBy { it.name() }
    val specAliases = unit.modules
        .flatMap { it.root.declNodes().filterIsInstance<SpecNode>() }
        .associateBy { it.name() }
    val apiAliases = unit.modules
        .flatMap { it.root.declNodes().filterIsInstance<ApiNode>() }
        .associateBy { it.name() }
    val sortModels = ast.cachedObjClassRegistry()?.sorts
        ?.mapValues { (_, sort) -> "{${sort.cfgElements.joinToString(", ")}}" }
        ?: emptyMap()

    val compositionLeaves = compositionLeavesOfSpec(spec)
    val procFunNameSet = procFunNodes.keys
    // Composition listings of procfuns are metadata (whitelist), not free-standing SyncChannel peers —
    // except when the entire system is a standalone procfun (compile F).
    val standaloneProcFun = compositionLeaves.size == 1 && compositionLeaves.single().name in procFunNameSet
    val hostLeavesRaw = expandLeavesToPclasses(
        compositionLeaves.filter { it.name !in procFunNameSet || standaloneProcFun },
        pclassNodes + procFunNodes.mapValues { (_, pf) -> pf.asSyntheticProcClass() },
        procAliases,
        specAliases,
        apiAliases,
        leafSpecNodes,
    ).map { leaf ->
        if (leaf.name in procFunNameSet) leaf.copy(isProcFun = true) else leaf
    }
    // Coupled via api calls: reachable from the spec's composition (including nested apis).
    val composedProcFuns = collectApiCallsInComposition(
        compositionLeaves.filter { it.name !in procFunNameSet || standaloneProcFun },
        apiAliases,
        procAliases,
        specAliases,
    )
    val callSiteDraftsAll = discoverProcFunCallSiteDrafts(
        hostLeavesRaw.filter { !it.isProcFun },
        pclassNodes,
    )
    val coupledDrafts = callSiteDraftsAll.filter { it.procFunName in composedProcFuns }
    val havocDrafts = callSiteDraftsAll.filter { it.procFunName !in composedProcFuns }
    val procFunLeavesRaw = procFunLeavesFromDrafts(coupledDrafts)
    val leaves = assignTlaLeafNames(hostLeavesRaw + procFunLeavesRaw)
    val callSites = resolveProcFunCallSites(coupledDrafts, leaves)
    val havocSites = resolveHavocProcFunCallSites(havocDrafts, leaves)
    val handshake = buildProcFunHandshakeVars(callSites)
    // Synthetic proc classes for procfun lookup (args + F_call + transitions + F_ret).
    val pclassesForTla = pclassNodes + procFunNodes.mapValues { (_, pf) -> pf.asSyntheticProcClass() }
    val procFunDecls = ast.procFunClassPass().associateBy { it.name }

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
    }

    val invariants = unit.modules
        .flatMap { it.root.declNodes().filterIsInstance<InvariantNode>() }
        .associateBy { it.name() }
    val ag = spec.specNodeValue() as? AgSpecExprNode
    val invNode = ag?.let { resolveGuaranteeInvariant(it, spec.specNodeName(), invariants) }
    val invClosure = if (invNode != null) {
        val invMap = if (invariants.containsKey(invNode.name())) {
            invariants
        } else {
            invariants + (invNode.name() to invNode)
        }
        topologicalInvariantClosure(invNode.name(), invMap)
    } else {
        emptyList()
    }
    invClosure.forEach { collectTypeConstants(it.invariantFormula(), constants) }

    // Finite TLC models for built-in domains used in the module (assigned in .cfg only).
    val cfgOverrides = linkedSetOf<String>()
    invClosure.forEach { collectBuiltinDomainUses(it.invariantFormula(), cfgOverrides) }
    leaves.forEach { leaf ->
        leaf.paramType?.let { typeDomainConstant(it) }?.let { name ->
            if (name in setOf("Int", "Nat", "Real")) cfgOverrides += name
        }
    }

    val intModelValues = linkedSetOf(0, 1, 2, 3, 4, 5)
    collectIntLiteralsFromLeaves(leaves, pclassesForTla, intModelValues)
    invClosure.forEach { collectIntLiteralsFromExpr(it.invariantFormula(), intModelValues) }

    val offers = collectTlaActionOffers(leaves, pclassesForTla, procFunDecls)
        // Coupled: child construct is folded into parent *_call. Standalone: keep F_call.
        .filterNot { it.isConstructor && it.leaf.isProcFun && !standaloneProcFun }
    val usedFunOpsAll = collectUserFunsUsedInOffers(offers)
    val (funlibFuns, userFuns) = usedFunOpsAll.partition { isJulayFunlibFun(it) }
    val usedFunlibOps = orderFunsForTlaEmit(funlibFuns.toSet())
    val usedUserFunOps = orderFunsForTlaEmit(userFuns.toSet())
    val usedFunOps = usedFunlibOps + usedUserFunOps
    val relevantFields = when {
        tlaOptConfig.unusedFields ->
            analyzeTlaRelevantFields(
                pclassesForTla, offers, usedFunOps, invClosure,
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
            procFunLeafNames = leaves.filter { it.isProcFun }.map { it.name }.toSet(),
            callSites = callSites,
        )
    } else {
        TlaRelevantVars.IDENTITY
    }
    TlaVarProjection.set(relevantVars)
    val literalDomains = if (tlaOptConfig.literalDomains) {
        analyzeTlaLiteralDomains(leaves, pclassesForTla, offers, usedFunOps, invClosure)
    } else {
        TlaLiteralDomains.NONE
    }

    TlaFieldProjection.set(relevantFields)
    TlaLiteralDomainProjection.set(literalDomains)
    TlaEmitOpts.set(tlaOptConfig)
    val symbolTypes = mutableMapOf<String, Type>()
    pclassesForTla.values.forEach { pc ->
        pc.localDecls().filterIsInstance<VarNode>().forEach { vn ->
            try {
                symbolTypes[vn.name] = vn.type
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
            procFunNodes = procFunNodes,
            leafSpecNodes = leafSpecNodes,
            callSites = callSites,
            havocSites = havocSites,
            handshake = handshake,
            constants = constants,
            cfgOverrides = cfgOverrides,
            intModelValues = intModelValues,
            invClosure = invClosure,
            sortModels = sortModels,
            offers = offers,
            usedFunlibOps = usedFunlibOps,
            usedUserFunOps = usedUserFunOps,
            usedFunOps = usedFunOps,
            unusedFieldWarnings = unusedFieldWarnings,
        )
    } finally {
        TlaFieldProjection.set(TlaRelevantFields.IDENTITY)
        TlaLiteralDomainProjection.set(TlaLiteralDomains.NONE)
        TlaEmitOpts.set(TlaOptConfig.ALL_ON)
        TlaSymbolTypes.set(emptyMap())
        TlaVarProjection.set(TlaRelevantVars.IDENTITY)
    }
}

private fun emitProjectedTla(
    moduleName: String,
    leaves: List<SpecLeaf>,
    pclassesForTla: Map<String, ProcClassNode>,
    procFunNodes: Map<String, ProcFunNode>,
    leafSpecNodes: Map<String, LeafSpecNode>,
    callSites: List<ProcFunCallSite>,
    havocSites: List<ProcFunCallSite>,
    handshake: ProcFunHandshakeVars,
    constants: LinkedHashSet<String>,
    cfgOverrides: LinkedHashSet<String>,
    intModelValues: LinkedHashSet<Int>,
    invClosure: List<InvariantNode>,
    sortModels: Map<String, String>,
    offers: List<TlaActionOffer>,
    usedFunlibOps: List<FunNode>,
    usedUserFunOps: List<FunNode>,
    usedFunOps: List<FunNode>,
    unusedFieldWarnings: List<OneLocCompileWarning>,
): TlaCodegenResult {
    collectProjectedSortConstants(leaves, pclassesForTla, leafSpecNodes, offers, constants)
    collectActionArgDomainModels(offers, pclassesForTla, cfgOverrides)
    collectIoHavocDomainModels(leaves, pclassesForTla, cfgOverrides)
    havocSites.forEach { site ->
        procFunNodes[site.procFunName]?.let { pf ->
            try {
                val d = typeToTlaDomain(pf.returnType)
                val base = d.trim().removePrefix("(").substringBefore(" ")
                if (base in setOf("Int", "Nat", "Real", "String")) cfgOverrides += base
            } catch (_: RuntimeException) {}
        }
    }
    if ("String" in cfgOverrides) {
        constants += "String"
    }
    if ("MaxListLen" in cfgOverrides) {
        constants += "MaxListLen"
    }

    val reservedTlaIds = constants + setOf("Int", "Nat", "Boolean", "Real") +
        actionArgNames(leaves, pclassesForTla) +
        leaves.mapNotNull { it.paramName }.toSet()
    val needsSpliceOperator =
        offersUseSlice(offers) ||
            usedFunOps.any { exprContainsSlice(it.funBody()) } ||
            invClosure.any { exprContainsSlice(it.invariantFormula()) }
    val needsStartsWithOperator =
        offersUseStartsWith(offers) ||
            usedFunOps.any { exprContainsStartsWith(it.funBody()) } ||
            invClosure.any { exprContainsStartsWith(it.invariantFormula()) }
    val unsupportedBuiltinWarnings =
        collectUnsupportedBuiltinWarnings(offers, usedFunOps, invClosure)
    val sessionPairs = detectTwoSidedSessionPairs(offers)
    val emittedOfferLists = collectEmittedOfferLists(offers)
    val killTargets = collectKillTargets(emittedOfferLists, sessionPairs)
    val needsSessionException = emittedOfferLists.any { offerList ->
        sessionPairForOffers(offerList, sessionPairs) != null && offerList.any { it.isConstructor }
    }
    val stateVarNames = buildStateVarNames(leaves, pclassesForTla, reservedTlaIds, killTargets)

    val variables = mutableListOf<String>()
    val initParts = mutableListOf<String>()
    leaves.forEach { leaf ->
        val pc = pclassesForTla[leaf.name] ?: return@forEach
        initParts += "\\* State variables for ${leaf.name}"
        val constructed = stateTlaName(leaf.tlaName, "constructed", stateVarNames)
        val hasKilled = leaf.tlaName in killTargets
        val killed = if (hasKilled) stateTlaName(leaf.tlaName, "killed", stateVarNames) else null
        val terminated = if (leaf.isProcFun) stateTlaName(leaf.tlaName, "terminated", stateVarNames) else null
        if (leaf.isParameterized) {
            val domain = typeDomainConstant(leaf.paramType!!) ?: leaf.paramType.toString()
            val bareStateVars = pc.localDecls().filterIsInstance<VarNode>().map { it.name }.toSet()
            val binder = indexBinderName(leaf, bareStateVars)
            variables += constructed
            initParts += "/\\ $constructed = [$binder \\in $domain |-> FALSE]"
            if (killed != null) {
                variables += killed
                initParts += "/\\ $killed = [$binder \\in $domain |-> FALSE]"
            }
            if (terminated != null) {
                variables += terminated
                initParts += "/\\ $terminated = [$binder \\in $domain |-> FALSE]"
            }
            pc.localDecls().filterIsInstance<VarNode>().forEach { vn ->
                if (!TlaVarProjection.get().isRelevant(leaf.name, vn.name)) return@forEach
                val v = stateTlaName(leaf.tlaName, vn.name, stateVarNames)
                variables += v
                initParts += "/\\ $v = [$binder \\in $domain |-> ${defaultTlaValue(safeType(vn), leafClass = leaf.name, varName = vn.name)}]"
            }
        } else {
            variables += constructed
            initParts += "/\\ $constructed = FALSE"
            if (killed != null) {
                variables += killed
                initParts += "/\\ $killed = FALSE"
            }
            if (terminated != null) {
                variables += terminated
                initParts += "/\\ $terminated = FALSE"
            }
            pc.localDecls().filterIsInstance<VarNode>().forEach { vn ->
                if (!TlaVarProjection.get().isRelevant(leaf.name, vn.name)) return@forEach
                val v = stateTlaName(leaf.tlaName, vn.name, stateVarNames)
                variables += v
                initParts += "/\\ $v = ${defaultTlaValue(safeType(vn), leafClass = leaf.name, varName = vn.name)}"
            }
        }
    }
    // Procfun spawn-await handshake vars
    if (handshake.allNames().isNotEmpty()) {
        initParts += "\\* Procfun call-site handshake"
        val hostByTla = leaves.associateBy { it.tlaName }
        handshake.blockingByHost.forEach { (hostTla, varName) ->
            val host = hostByTla[hostTla]
            variables += varName
            if (host != null && host.isParameterized) {
                val domain = typeDomainConstant(host.paramType!!) ?: host.paramType.toString()
                val binder = indexBinderName(host, emptySet())
                initParts += "/\\ $varName = [$binder \\in $domain |-> FALSE]"
            } else {
                initParts += "/\\ $varName = FALSE"
            }
        }
        handshake.callFlags.forEach { (occ, varName) ->
            variables += varName
            if (occ.isParameterized) {
                val domain = typeDomainConstant(occ.paramType!!) ?: occ.paramType.toString()
                val binder = indexBinderName(occ, emptySet())
                initParts += "/\\ $varName = [$binder \\in $domain |-> FALSE]"
            } else {
                initParts += "/\\ $varName = FALSE"
            }
        }
        handshake.returnToByKey.forEach { (key, varName) ->
            val (hostTla, _) = key
            val host = hostByTla[hostTla]
            variables += varName
            if (host != null && host.isParameterized) {
                val domain = typeDomainConstant(host.paramType!!) ?: host.paramType.toString()
                val binder = indexBinderName(host, emptySet())
                initParts += "/\\ $varName = [$binder \\in $domain |-> FALSE]"
            } else {
                initParts += "/\\ $varName = FALSE"
            }
        }
    }
    sessionPairs.forEach { pair ->
        variables += pair.varName
        initParts += sessionVarInit(pair)
    }
    if (needsSessionException) {
        variables += "sessionException"
        initParts += "/\\ sessionException = FALSE"
    }

    val built = buildTlaActions(
        leaves, pclassesForTla, offers, sessionPairs, stateVarNames,
        killTargets, needsSessionException, callSites, handshake, procFunNodes,
        havocSites, cfgOverrides, leafSpecNodes,
    )
    val helpers = built.helpers
    val actions = built.actions

    // Fun / helper operator params must not collide with VARIABLES / CONSTANTS / other module ops.
    val funReservedNames = (
        constants + variables + usedFunOps.map { it.name() } +
            actions.map { it.name } +
            setOf(
                "vars", "BoundedSeq", "splice", "startsWith",
                "Init", "Next", "Spec", "GF", "dummy",
            )
        ).toSet()
    val spliceOperatorDef = if (needsSpliceOperator) emitSpliceOperatorDef(funReservedNames) else null
    val startsWithOperatorDef =
        if (needsStartsWithOperator) emitStartsWithOperatorDef(funReservedNames) else null
    val funlibOperatorDefs = usedFunlibOps.map { emitFunOperatorDef(it, funReservedNames) }
    val userFunOperatorDefs = usedUserFunOps.map { emitFunOperatorDef(it, funReservedNames) }
    val hasJulayLibFuns =
        needsSpliceOperator || needsStartsWithOperator || funlibOperatorDefs.isNotEmpty()

    val invDefs = if (invClosure.isNotEmpty()) {
        invClosure.flatMap { node ->
            emitInvariantDefs(node, leaves, stateVarNames, constants)
        }
    } else {
        emptyList()
    }
    val sessionIntegrityDef =
        if (needsSessionException) "SessionIntegrity == ~sessionException" else null

    val constLine = if (constants.isEmpty()) "" else "CONSTANT ${constants.joinToString(", ")}\n\n"
    val varsLine = if (variables.isEmpty()) {
        "VARIABLES dummy\n\n"
    } else {
        "VARIABLES ${variables.joinToString(", ")}\n\n"
    }
    val varsTuple = if (variables.isEmpty()) "<<dummy>>" else "<<${variables.joinToString(", ")}>>"
    if (variables.isEmpty()) {
        initParts += "/\\ dummy = 0"
    }

    val actionDefs = actions.joinToString("\n\n") { action ->
        if (action.comment != null) "\\* ${action.comment}\n${action.def}" else action.def
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
        if ("MaxListLen" in constants || "MaxListLen" in cfgOverrides) {
            appendLine("BoundedSeq(S, N) == UNION { [1..k -> S] : k \\in 0..N }")
            appendLine()
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
        appendLine("Spec == Init /\\ [][Next]_vars")
        if (sessionIntegrityDef != null) {
            appendLine()
            appendLine(sessionIntegrityDef)
        }
        if (invDefs.isNotEmpty()) {
            appendLine()
            invDefs.forEach { appendLine(it) }
        }
        val terminatesDefs = leaves.filter { it.isProcFun }.map { leaf ->
            emitTerminatesProperty(leaf, stateVarNames, pclassesForTla)
        }
        if (terminatesDefs.isNotEmpty()) {
            appendLine()
            appendLine("GF(P) == <>[]P")
            terminatesDefs.forEach { appendLine(it) }
        }
        appendLine("====")
    }

    val terminatesPropNames = leaves.filter { it.isProcFun }.map { "${it.tlaName}Terminates" }

    val cfg = buildString {
        appendLine("SPECIFICATION Spec")
        if (needsSessionException) {
            appendLine("INVARIANT SessionIntegrity")
        }
        if (invDefs.isNotEmpty()) {
            invDefs.forEach { def ->
                val invOp = def.substringBefore(" ==")
                appendLine("INVARIANT $invOp")
            }
        }
        terminatesPropNames.forEach { name ->
            appendLine("PROPERTY $name")
        }
        appendLine("CHECK_DEADLOCK FALSE")
        fun modelFor(name: String): String = when {
            name in sortModels -> sortModels.getValue(name)
            name == "Int" || name == "Nat" || name == "Real" -> cfgIntModel(intModelValues)
            name == "String" -> cfgStringModel(intModelValues)
            else -> cfgConstantModel(name)
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

    return TlaCodegenResult(moduleName, tla, cfg, unsupportedBuiltinWarnings + unusedFieldWarnings)
}

private fun emitTerminatesProperty(
    leaf: SpecLeaf,
    stateVarNames: Map<Pair<String, String>, String>,
    pclasses: Map<String, ProcClassNode>,
): String {
    val term = stateTlaName(leaf.tlaName, "terminated", stateVarNames)
    val propName = "${leaf.tlaName}Terminates"
    return if (leaf.isParameterized) {
        val pc = pclasses[leaf.name]
        val bare = pc?.localDecls()?.filterIsInstance<VarNode>()?.map { it.name }?.toSet().orEmpty()
        val binder = indexBinderName(leaf, bare)
        val domain = typeDomainConstant(leaf.paramType!!) ?: leaf.paramType.toString()
        "$propName == \\A $binder \\in $domain : GF($term[$binder])"
    } else {
        "$propName == GF($term)"
    }
}

internal data class TlaAction(
    val name: String,
    val def: String,
    /** Index binders (first) then used action args, as (name, TLA domain). */
    val params: List<Pair<String, String>> = emptyList(),
    /** Present when [name] was renamed for disambiguation. */
    val comment: String? = null,
) {
    fun nextDisjunct(): String {
        if (params.isEmpty()) return name
        val call = "$name(${params.joinToString(", ") { it.first }})"
        return params.asReversed().fold(call) { acc, (arg, domain) ->
            "\\E $arg \\in $domain : $acc"
        }
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
 * `constructor` / `transition`, optionally prefixed with `session` / `provider` / `client` / `internal`.
 * Uses [TlaActionOffer.decl] modifier (not [TlaActionOffer.role]), since constructors are stored as
 * Internal on the offer for codegen grouping while their decl modifier remains Default.
 */
internal fun tlaTransitionTypeLabel(offer: TlaActionOffer): String {
    val kindStr = if (offer.isConstructor) "constructor" else "transition"
    return when {
        offer.decl.isSession -> "session $kindStr"
        offer.decl.modifier == TSAction.SyncRole.Provider -> "provider $kindStr"
        offer.decl.modifier == TSAction.SyncRole.Client -> "client $kindStr"
        offer.decl.modifier == TSAction.SyncRole.Internal -> "internal $kindStr"
        else -> kindStr
    }
}

/** Two SpecLeaves that both offer at least one shared session action. */
internal data class SessionLeafPair(
    val leafA: SpecLeaf,
    val leafB: SpecLeaf,
) {
    init {
        require(leafA.tlaName <= leafB.tlaName) { "leafA must be ordered before leafB by tlaName" }
    }

    val varName: String get() = "session_${leafA.tlaName}_${leafB.tlaName}"
    val canStartName: String get() = "CanStartSession_${leafA.tlaName}_${leafB.tlaName}"
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

private fun collectTlaActionOffers(
    leaves: List<SpecLeaf>,
    pclasses: Map<String, ProcClassNode>,
    procFunDecls: Map<String, julay.compiler.decl.ProcClassDecl> = emptyMap(),
): List<TlaActionOffer> {
    val offers = mutableListOf<TlaActionOffer>()
    leaves.forEach { leaf ->
        val pfDecl = procFunDecls[leaf.name]
        if (pfDecl != null && leaf.isProcFun) {
            pfDecl.constructors.forEach { ctor ->
                offers += TlaActionOffer(leaf, ctor, TSAction.SyncRole.Internal, isConstructor = true)
            }
            pfDecl.transitions.forEach { tr ->
                offers += TlaActionOffer(leaf, tr, tr.modifier, isConstructor = false)
            }
            return@forEach
        }
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

/** Session pairs where both peers appear as SpecLeaves (one-sided → empty). */
internal fun detectTwoSidedSessionPairs(offers: List<TlaActionOffer>): List<SessionLeafPair> {
    val leafByTla = offers.map { it.leaf }.associateBy { it.tlaName }
    val pairKeys = linkedSetOf<Pair<String, String>>()
    offers.groupBy { it.decl.action.name }.forEach { (_, group) ->
        val sessionOffers = group.filter { it.decl.isSession }
        if (sessionOffers.size != 2) return@forEach
        val tlaNames = sessionOffers.map { it.leaf.tlaName }.toSet()
        if (tlaNames.size != 2) return@forEach
        // Same class never sessions with itself (two occurrences of one class).
        if (sessionOffers.map { it.leaf.name }.toSet().size != 2) return@forEach
        val sorted = tlaNames.sorted()
        pairKeys += sorted[0] to sorted[1]
    }
    return pairKeys.mapNotNull { (a, b) ->
        val la = leafByTla[a] ?: return@mapNotNull null
        val lb = leafByTla[b] ?: return@mapNotNull null
        if (la.tlaName <= lb.tlaName) SessionLeafPair(la, lb) else SessionLeafPair(lb, la)
    }
}

private fun sessionPairForOffers(
    offerList: List<TlaActionOffer>,
    pairs: List<SessionLeafPair>,
): SessionLeafPair? {
    if (offerList.size != 2) return null
    if (!offerList.all { it.decl.isSession }) return null
    val names = offerList.map { it.leaf.tlaName }.sorted()
    return pairs.find { it.leafA.tlaName == names[0] && it.leafB.tlaName == names[1] }
}

/**
 * Offer lists that [buildTlaActions] would pass to [emitConjoined], in the same grouping order.
 * Used to precompute kill targets and whether sessionException is needed.
 */
internal fun collectEmittedOfferLists(offers: List<TlaActionOffer>): List<List<TlaActionOffer>> {
    val result = mutableListOf<List<TlaActionOffer>>()
    offers.groupBy { it.decl.action.name }.forEach { (_, group) ->
        val providers = group.filter { it.role == TSAction.SyncRole.Provider }
        val clients = group.filter { it.role == TSAction.SyncRole.Client }
        val constructors = group.filter { it.isConstructor }
        val internals = group.filter { it.role == TSAction.SyncRole.Internal && !it.isConstructor }
        val defaults = group.filter { it.role == TSAction.SyncRole.Default && !it.isConstructor }

        if (providers.size == 1 && clients.isNotEmpty()) {
            clients.forEach { cli -> result += listOf(providers[0], cli) }
            return@forEach
        }
        if (constructors.size == 1 && defaults.size == 1) {
            result += listOf(defaults[0], constructors[0])
            return@forEach
        }
        constructors.forEach { result += listOf(it) }
        internals.forEach { result += listOf(it) }
        when {
            defaults.size >= 2 -> result += defaults
            defaults.size == 1 -> result += defaults
        }
        if (providers.isNotEmpty() && clients.isEmpty()) {
            providers.forEach { result += listOf(it) }
        }
        if (clients.isNotEmpty() && providers.isEmpty()) {
            clients.forEach { result += listOf(it) }
        }
    }
    return result
}

/** Leaf names that are kill targets: `killSessionPeer` peers or `exitProc` callers. */
private fun collectKillTargets(
    emittedOfferLists: List<List<TlaActionOffer>>,
    sessionPairs: List<SessionLeafPair>,
): Set<String> {
    val targets = linkedSetOf<String>()
    emittedOfferLists.forEach { offerList ->
        offerList.forEach { offer ->
            if (offerHasExitProc(offer)) {
                targets += offer.leaf.tlaName
            }
        }
        if (offerList.none { "killSessionPeer" in sessionEffectNames(it) }) return@forEach
        val actionPair = sessionPairForOffers(offerList, sessionPairs)
        val effectPair = resolveSessionEffectPair(offerList, actionPair, sessionPairs) ?: return@forEach
        val caller = sessionEffectCaller(offerList, "killSessionPeer") ?: return@forEach
        targets += peerLeafOf(effectPair, caller).tlaName
    }
    return targets
}

private fun offerHasExitProc(offer: TlaActionOffer): Boolean =
    (offer.decl.befores + offer.decl.afters).any { it.callName() == "exitProc" }

private fun sessionEffectNames(offer: TlaActionOffer): List<String> =
    (offer.decl.befores + offer.decl.afters).map { it.callName() }.filter {
        it == "exitSession" || it == "killSessionPeer"
    }

/**
 * Resolve the session pair targeted by exitSession(Peer) / killSessionPeer(Peer) on [offers].
 * The peer leaf name is taken from the effect's proc-class argument.
 */
private fun resolveSessionEffectPair(
    offers: List<TlaActionOffer>,
    @Suppress("UNUSED_PARAMETER") actionSessionPair: SessionLeafPair?,
    allPairs: List<SessionLeafPair>,
): SessionLeafPair? {
    val effectNames = offers.flatMap { sessionEffectNames(it) }.toSet()
    if (effectNames.isEmpty()) return null
    val hasKill = "killSessionPeer" in effectNames
    val hasExit = "exitSession" in effectNames
    if (hasKill && hasExit) {
        throw RuntimeException(
            "TLA+: action \"${offers.first().decl.action.name}\" cannot use both " +
                "exitSession and killSessionPeer",
        )
    }
    val effectName = if (hasKill) "killSessionPeer" else "exitSession"
    val caller = sessionEffectCaller(offers, effectName)
        ?: throw RuntimeException("TLA+: $effectName missing caller leaf")
    val peerName = offers.mapNotNull { sessionEffectPeerClassName(it, effectName) }.firstOrNull()
        ?: throw RuntimeException(
            "TLA+: $effectName on \"${offers.first().decl.action.name}\" " +
                "requires a leaf proc class name argument",
        )
    if (peerName == caller.name) {
        throw RuntimeException(
            "TLA+: $effectName on \"${offers.first().decl.action.name}\" " +
                "cannot target the caller leaf \"$peerName\"",
        )
    }
    val matching = allPairs.filter { pair ->
        (pair.leafA.name == caller.name && pair.leafB.name == peerName) ||
            (pair.leafB.name == caller.name && pair.leafA.name == peerName)
    }
    return when (matching.size) {
        1 -> matching.single()
        0 -> throw RuntimeException(
            "TLA+: $effectName on \"${offers.first().decl.action.name}\" " +
                "has no two-sided session pair between ${caller.name} and $peerName",
        )
        else -> throw RuntimeException(
            "TLA+: $effectName on \"${offers.first().decl.action.name}\" " +
                "is ambiguous across ${matching.map { it.varName }}",
        )
    }
}

/** Peer proc-class name from exitSession(Peer) / killSessionPeer(Peer). */
private fun sessionEffectPeerClassName(offer: TlaActionOffer, effectName: String): String? {
    val stmt = (offer.decl.befores + offer.decl.afters).firstOrNull { it.callName() == effectName } ?: return null
    val arg = stmt.callArgs().singleOrNull() as? SymbolValueExprNode ?: return null
    return arg.symbol
}

/** Caller leaf for a kill/exit effect (first offer that declares it). */
private fun sessionEffectCaller(offers: List<TlaActionOffer>, effectName: String): SpecLeaf? =
    offers.firstOrNull { effectName in sessionEffectNames(it) }?.leaf

private fun peerLeafOf(pair: SessionLeafPair, caller: SpecLeaf): SpecLeaf =
    when (caller.name) {
        pair.leafA.name -> pair.leafB
        pair.leafB.name -> pair.leafA
        else -> throw RuntimeException(
            "TLA+: leaf ${caller.name} is not in session pair ${pair.varName}",
        )
    }

private fun killedAssignTrueExpr(
    leaf: SpecLeaf,
    binder: String?,
    stateVarNames: Map<Pair<String, String>, String>,
): String {
    val k = stateTlaName(leaf.tlaName, "killed", stateVarNames)
    return if (binder != null) {
        "$k' = [$k EXCEPT ![$binder] = TRUE]"
    } else {
        "$k' = TRUE"
    }
}

private fun sessionVarInit(pair: SessionLeafPair): String {
    val v = pair.varName
    val a = pair.leafA
    val b = pair.leafB
    return when {
        a.isParameterized && b.isParameterized -> {
            val da = typeDomainConstant(a.paramType!!) ?: a.paramType.toString()
            val db = typeDomainConstant(b.paramType!!) ?: b.paramType.toString()
            val ba = indexBinderName(a, emptySet())
            val bb = indexBinderName(b, setOf(ba))
            "/\\ $v = [$ba \\in $da |-> [$bb \\in $db |-> FALSE]]"
        }
        a.isParameterized && !b.isParameterized -> {
            val da = typeDomainConstant(a.paramType!!) ?: a.paramType.toString()
            val ba = indexBinderName(a, emptySet())
            "/\\ $v = [$ba \\in $da |-> FALSE]"
        }
        !a.isParameterized && b.isParameterized -> {
            val db = typeDomainConstant(b.paramType!!) ?: b.paramType.toString()
            val bb = indexBinderName(b, emptySet())
            "/\\ $v = [$bb \\in $db |-> FALSE]"
        }
        else -> "/\\ $v = FALSE"
    }
}

private fun canStartSessionDef(pair: SessionLeafPair): String {
    val v = pair.varName
    val a = pair.leafA
    val b = pair.leafB
    return when {
        a.isParameterized && b.isParameterized -> {
            val da = typeDomainConstant(a.paramType!!) ?: a.paramType.toString()
            val db = typeDomainConstant(b.paramType!!) ?: b.paramType.toString()
            val ba = indexBinderName(a, emptySet())
            val bb = indexBinderName(b, setOf(ba))
            val ba2 = "${ba}2"
            val bb2 = "${bb}2"
            "${pair.canStartName}($ba, $bb) ==\n" +
                "  /\\ ~\\E $bb2 \\in $db : $v[$ba][$bb2]\n" +
                "  /\\ ~\\E $ba2 \\in $da : $v[$ba2][$bb]"
        }
        a.isParameterized && !b.isParameterized -> {
            val da = typeDomainConstant(a.paramType!!) ?: a.paramType.toString()
            val ba = indexBinderName(a, emptySet())
            val ba2 = "${ba}2"
            "${pair.canStartName}($ba) ==\n  ~\\E $ba2 \\in $da : $v[$ba2]"
        }
        !a.isParameterized && b.isParameterized -> {
            val db = typeDomainConstant(b.paramType!!) ?: b.paramType.toString()
            val bb = indexBinderName(b, emptySet())
            val bb2 = "${bb}2"
            "${pair.canStartName}($bb) ==\n  ~\\E $bb2 \\in $db : $v[$bb2]"
        }
        else -> "${pair.canStartName} ==\n  ~$v"
    }
}

private fun sessionLookup(
    pair: SessionLeafPair,
    binderA: String?,
    binderB: String?,
): String {
    val v = pair.varName
    return when {
        binderA != null && binderB != null -> "$v[$binderA][$binderB]"
        binderA != null -> "$v[$binderA]"
        binderB != null -> "$v[$binderB]"
        else -> v
    }
}

/**
 * True when [caller] has any live session in [pair] (any peer index).
 * Used so teardown no-op applies only when affinity is fully absent; if some peer
 * session exists, [sessionLookup] must hold for the quantified indices.
 */
private fun anySessionWithCaller(
    pair: SessionLeafPair,
    caller: SpecLeaf,
    binderA: String?,
    binderB: String?,
): String {
    val v = pair.varName
    val a = pair.leafA
    val b = pair.leafB
    return when {
        a.isParameterized && b.isParameterized -> {
            val da = typeDomainConstant(a.paramType!!) ?: a.paramType.toString()
            val db = typeDomainConstant(b.paramType!!) ?: b.paramType.toString()
            when (caller.name) {
                a.name -> {
                    val bb = binderB ?: indexBinderName(b, emptySet())
                    val bb2 = "${bb}2"
                    val ba = binderA ?: indexBinderName(a, setOf(bb))
                    "\\E $bb2 \\in $db : $v[$ba][$bb2]"
                }
                b.name -> {
                    val ba = binderA ?: indexBinderName(a, emptySet())
                    val ba2 = "${ba}2"
                    val bb = binderB ?: indexBinderName(b, setOf(ba))
                    "\\E $ba2 \\in $da : $v[$ba2][$bb]"
                }
                else -> error("caller ${caller.name} not in pair ${pair.varName}")
            }
        }
        else -> sessionLookup(pair, binderA, binderB)
    }
}

private fun sessionAssignTrueExpr(
    pair: SessionLeafPair,
    binderA: String?,
    binderB: String?,
): String {
    val v = pair.varName
    return when {
        binderA != null && binderB != null ->
            "$v' = [$v EXCEPT ![$binderA] = [@ EXCEPT ![$binderB] = TRUE]]"
        binderA != null -> "$v' = [$v EXCEPT ![$binderA] = TRUE]"
        binderB != null -> "$v' = [$v EXCEPT ![$binderB] = TRUE]"
        else -> "$v' = TRUE"
    }
}

private fun sessionAssignFalseExpr(
    pair: SessionLeafPair,
    binderA: String?,
    binderB: String?,
): String {
    val v = pair.varName
    return when {
        binderA != null && binderB != null ->
            "$v' = [$v EXCEPT ![$binderA] = [@ EXCEPT ![$binderB] = FALSE]]"
        binderA != null -> "$v' = [$v EXCEPT ![$binderA] = FALSE]"
        binderB != null -> "$v' = [$v EXCEPT ![$binderB] = FALSE]"
        else -> "$v' = FALSE"
    }
}

private fun canStartCall(
    pair: SessionLeafPair,
    binderA: String?,
    binderB: String?,
): String {
    val name = pair.canStartName
    return when {
        binderA != null && binderB != null -> "$name($binderA, $binderB)"
        binderA != null -> "$name($binderA)"
        binderB != null -> "$name($binderB)"
        else -> name
    }
}

private fun deadOperatorName(leaf: SpecLeaf): String = "${leaf.tlaName}_dead"

private fun negateLocalGuards(
    offer: TlaActionOffer,
    self: String?,
    stateVarsByLeaf: Map<String, Set<String>>,
    stateVarNames: Map<Pair<String, String>, String>,
): String {
    val guards = offer.decl.guards
    if (guards.isEmpty()) return "FALSE"
    val plan = analyzeTlaArgBind(listOf(offer), TlaEmitOpts.get())
    offer.decl.action.args.forEach { TlaSymbolTypes.set(TlaSymbolTypes.get() + (it.name to it.type)) }
    val bindNames = linkedSetOf<String>().apply {
        addAll(plan.skipArgs)
        addAll(plan.extraBinderNames())
        plan.setBinds.forEach { add(it.arg) }
        plan.determined.forEach { add(it.first) }
        plan.listBinds.forEach { add(it.arg) }
        plan.structBinds.forEach { b -> b.argPaths.forEach { add(it.first) } }
    }
    val argNames = offer.decl.action.args.map { it.name }.toSet() + bindNames
    val leafCtx = mapOf(offer.leaf.name to offer.leaf, offer.leaf.tlaName to offer.leaf)
    val bare = stateVarsByLeaf[offer.leaf.tlaName].orEmpty()
    fun emit(expr: ExprNode, linePrefix: String = ""): String =
        exprToTla(expr, leafCtx, argNames, self, bareStateVars = bare, stateVarNames = stateVarNames, linePrefix = linePrefix)
    val guardStrs = guards.flatMap { flattenTopLevelAnd(it) }
        .filter { !plan.skipped(it) }
        .map { emit(it) }
    val keepStrs = plan.structBinds.flatMap { b ->
        val elemType = try {
            (b.set.getType() as? SetType)?.elementType
        } catch (_: RuntimeException) {
            null
        }
        b.keep.map { (path, expr) ->
            "${emitUnwrappedFieldPath(b.tmp, elemType, path)} = ${emit(expr)}"
        }
    }
    val allGuards = guardStrs + keepStrs
    val conj = when {
        allGuards.isEmpty() -> "TRUE"
        allGuards.size == 1 -> allGuards[0]
        else -> allGuards.joinToString(" /\\ ")
    }
    val letBindings = mutableListOf<Pair<String, String>>()
    plan.listBinds.forEach { b ->
        letBindings += b.arg to "${emit(b.list)}[${b.index}]"
    }
    plan.structBinds.forEach { b ->
        val elemType = try {
            (b.set.getType() as? SetType)?.elementType
        } catch (_: RuntimeException) {
            null
        }
        b.argPaths.forEach { (arg, path) ->
            letBindings += arg to emitUnwrappedFieldPath(b.tmp, elemType, path)
        }
    }
    plan.determined.forEach { (arg, expr) ->
        letBindings += arg to emit(expr)
    }
    val inner = wrapTlaLet(letBindings, conj)
    val existsParams = mutableListOf<Pair<String, String>>()
    plan.listBinds.forEach { b ->
        existsParams += b.index to "1..Len(${emit(b.list)})"
    }
    plan.setBinds.forEach { b ->
        existsParams += b.arg to emit(b.set)
    }
    plan.structBinds.forEach { b ->
        existsParams += b.tmp to emit(b.set)
    }
    val omit = plan.omitArgTypeDomains()
    offer.decl.action.args.filter { arg ->
        arg.name !in omit && guards.any { exprReferencesSymbol(it, arg.name) }
    }.forEach { arg ->
        existsParams += arg.name to (argLiteralDomain(listOf(offer), arg.name, arg.type) ?: typeToTlaDomain(arg.type))
    }
    val exists = existsParams.asReversed().fold(inner) { acc, (n, d) ->
        "\\E $n \\in $d : $acc"
    }
    return "~($exists)"
}

private fun deadOperatorDef(
    leaf: SpecLeaf,
    leafOffers: List<TlaActionOffer>,
    stateVarsByLeaf: Map<String, Set<String>>,
    stateVarNames: Map<Pair<String, String>, String>,
    killTargets: Set<String>,
): String {
    val c = stateTlaName(leaf.tlaName, "constructed", stateVarNames)
    val self = if (leaf.isParameterized) {
        indexBinderName(leaf, stateVarsByLeaf[leaf.tlaName].orEmpty())
    } else {
        null
    }
    val naturalParts = mutableListOf<String>()
    naturalParts += if (self != null) "/\\ $c[$self]" else "/\\ $c"
    leafOffers.filter { !it.isConstructor }.forEach { offer ->
        naturalParts += "/\\ ${negateLocalGuards(offer, self, stateVarsByLeaf, stateVarNames)}"
    }
    val naturalBody = naturalParts.joinToString("\n       ")
    val signature = if (self != null) "${deadOperatorName(leaf)}($self)" else deadOperatorName(leaf)
    return if (leaf.tlaName in killTargets) {
        val killed = stateTlaName(leaf.tlaName, "killed", stateVarNames)
        val killedLit = if (self != null) "$killed[$self]" else killed
        val comment =
            "\\* True when ${leaf.name} was explicitly killed or all of its actions are disabled."
        "$comment\n$signature ==\n  \\/ $killedLit\n  \\/ ($naturalBody)"
    } else {
        val comment =
            "\\* True when all of ${leaf.name}'s actions are disabled."
        val body = naturalParts.joinToString("\n  ")
        "$comment\n$signature ==\n  $body"
    }
}

private fun endSessionActionName(
    pair: SessionLeafPair,
    leaf: SpecLeaf,
    allPairs: List<SessionLeafPair>,
): String {
    val count = allPairs.count { it.leafA.tlaName == leaf.tlaName || it.leafB.tlaName == leaf.tlaName }
    return if (count == 1) {
        "EndSession_${leaf.tlaName}"
    } else {
        "EndSession_${pair.leafA.tlaName}_${pair.leafB.tlaName}_${leaf.tlaName}"
    }
}

private fun emitEndSession(
    pair: SessionLeafPair,
    exiting: SpecLeaf,
    allPairs: List<SessionLeafPair>,
    allVars: List<String>,
    stateVarsByLeaf: Map<String, Set<String>>,
): TlaAction {
    val name = endSessionActionName(pair, exiting, allPairs)
    val binderA = if (pair.leafA.isParameterized) {
        indexBinderName(pair.leafA, stateVarsByLeaf[pair.leafA.tlaName].orEmpty())
    } else null
    val binderB = if (pair.leafB.isParameterized) {
        val reserved = stateVarsByLeaf[pair.leafB.tlaName].orEmpty().toMutableSet()
        binderA?.let { reserved += it }
        indexBinderName(pair.leafB, reserved)
    } else null
    val exitingBinder = when (exiting.name) {
        pair.leafA.tlaName -> binderA
        else -> binderB
    }
    val deadCall = if (exitingBinder != null) {
        "${deadOperatorName(exiting)}($exitingBinder)"
    } else {
        deadOperatorName(exiting)
    }
    val lookup = sessionLookup(pair, binderA, binderB)
    val assign = sessionAssignFalseExpr(pair, binderA, binderB)
    val changed = setOf(pair.varName)
    val unchanged = allVars.filter { it !in changed }
    val parts = mutableListOf<String>()
    parts += "/\\ $deadCall"
    parts += "\\* Session connection semantics"
    parts += "/\\ $lookup"
    parts += "/\\ $assign"
    if (unchanged.isNotEmpty()) {
        parts += "/\\ UNCHANGED <<${unchanged.joinToString(", ")}>>"
    }
    val params = mutableListOf<Pair<String, String>>()
    if (binderA != null) {
        val domain = typeDomainConstant(pair.leafA.paramType!!) ?: pair.leafA.paramType.toString()
        params += binderA to domain
    }
    if (binderB != null) {
        val domain = typeDomainConstant(pair.leafB.paramType!!) ?: pair.leafB.paramType.toString()
        params += binderB to domain
    }
    val signature = if (params.isEmpty()) name else "$name(${params.joinToString(", ") { it.first }})"
    return TlaAction(name, "$signature ==\n  ${parts.joinToString("\n  ")}", params)
}

private fun buildTlaActions(
    leaves: List<SpecLeaf>,
    pclasses: Map<String, ProcClassNode>,
    offers: List<TlaActionOffer>,
    sessionPairs: List<SessionLeafPair>,
    stateVarNames: Map<Pair<String, String>, String>,
    killTargets: Set<String>,
    needsSessionException: Boolean,
    callSites: List<ProcFunCallSite> = emptyList(),
    handshake: ProcFunHandshakeVars = ProcFunHandshakeVars(emptyList(), emptyMap(), emptyMap()),
    procFunNodes: Map<String, ProcFunNode> = emptyMap(),
    havocSites: List<ProcFunCallSite> = emptyList(),
    cfgOverrides: MutableSet<String> = linkedSetOf(),
    leafSpecs: Map<String, LeafSpecNode> = emptyMap(),
): TlaBuildResult {
    val allVars = allTlaVars(leaves, pclasses, stateVarNames, killTargets) +
        handshake.allNames() +
        sessionPairs.map { it.varName } +
        if (needsSessionException) listOf("sessionException") else emptyList()
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
    val leafByTla = leaves.associateBy { it.tlaName }
    val callSiteByHostAction = callSites.associateBy { it.hostName to it.hostActionName }
    val havocByHostAction = havocSites.associateBy { it.hostName to it.hostActionName }
    val returnToByOccurrence = callSites.associate {
        it.occurrence.occurrenceId to handshake.returnToByKey.getValue(it.hostName to it.hostActionName)
    }
    val blockingByHost = handshake.blockingByHost
    val hostsWithBlocking = blockingByHost.keys
    val splitHostActions = callSiteByHostAction.keys + havocByHostAction.keys

    val result = mutableListOf<TlaAction>()
    val byName = offers.groupBy { it.decl.action.name }

    fun emit(
        name: String,
        offerList: List<TlaActionOffer>,
        comment: String? = null,
    ) {
        emitConjoined(
            name, offerList, allVars, stateVarsByLeaf, stateVarNames,
            sessionPairForOffers(offerList, sessionPairs), sessionPairs, killTargets, comment,
            returnToByOccurrence = returnToByOccurrence,
            blockingByHost = blockingByHost,
            hostsWithBlocking = hostsWithBlocking,
            pclasses = pclasses,
            leafSpecs = leafSpecs,
            systemLeaves = leaves,
        )?.let { result += it }
    }

    fun emitCoupled(offer: TlaActionOffer, site: ProcFunCallSite) {
        val hostLeaf = leafByTla.getValue(offer.leaf.tlaName)
        val pf = procFunNodes[site.procFunName]
            ?: error("missing procfun ${site.procFunName}")
        result += emitProcFunCallAndRet(
            site, hostLeaf, offer, pf, allVars, stateVarsByLeaf, stateVarNames, handshake,
        )
    }

    fun emitHavoc(offer: TlaActionOffer, site: ProcFunCallSite) {
        val hostLeaf = leafByTla.getValue(offer.leaf.tlaName)
        val pf = procFunNodes[site.procFunName]
            ?: error("missing procfun ${site.procFunName}")
        result += emitProcFunHavocAction(
            site, hostLeaf, offer, pf, allVars, stateVarsByLeaf, stateVarNames, cfgOverrides,
        )
    }

    byName.forEach { (actionName, group) ->
        val providers = group.filter { it.role == TSAction.SyncRole.Provider }
        val clients = group.filter { it.role == TSAction.SyncRole.Client }
        val constructors = group.filter { it.isConstructor }
        val internals = group.filter { it.role == TSAction.SyncRole.Internal && !it.isConstructor }
        val defaults = group.filter { it.role == TSAction.SyncRole.Default && !it.isConstructor }

        if (providers.size == 1 && clients.isNotEmpty()) {
            val prov = providers[0]
            val needDisambiguate = clients.size > 1
            clients.forEach { cli ->
                val name: String
                val comment: String?
                if (needDisambiguate) {
                    name = "${actionName}_${prov.leaf.tlaName}_${cli.leaf.tlaName}"
                    comment =
                        "$actionName action where ${prov.leaf.tlaName} is the provider and ${cli.leaf.tlaName} is the client"
                } else {
                    name = actionName
                    comment = null
                }
                emit(name, listOf(prov, cli), comment)
            }
            return@forEach
        }

        // 1 constructor + 1 default transition → one hybrid shared action
        if (constructors.size == 1 && defaults.size == 1) {
            emit(actionName, listOf(defaults[0], constructors[0]))
            return@forEach
        }

        fun emitSplitOrPlain(offer: TlaActionOffer): Boolean {
            val key = offer.leaf.tlaName to offer.decl.action.name
            callSiteByHostAction[key]?.let { emitCoupled(offer, it); return true }
            havocByHostAction[key]?.let { emitHavoc(offer, it); return true }
            return false
        }

        // Solo constructors (any name, including initially) — valid leaf entry
        val disambiguateCtors = constructors.size > 1
        constructors.forEach { offer ->
            if (emitSplitOrPlain(offer)) return@forEach
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

        val disambiguateInternals = internals.size > 1
        internals.forEach { offer ->
            if (emitSplitOrPlain(offer)) return@forEach
            val name: String
            val comment: String?
            if (disambiguateInternals) {
                name = "${actionName}_${offer.leaf.tlaName}"
                comment = "$actionName action on ${offer.leaf.tlaName}"
            } else {
                name = actionName
                comment = null
            }
            emit(name, listOf(offer), comment)
        }

        when {
            defaults.size >= 2 -> {
                val (coupled, plain) = defaults.partition {
                    (it.leaf.tlaName to it.decl.action.name) in splitHostActions
                }
                coupled.forEach { offer -> emitSplitOrPlain(offer) }
                if (plain.isNotEmpty()) emit(actionName, plain)
            }
            defaults.size == 1 -> {
                val offer = defaults[0]
                if (!emitSplitOrPlain(offer)) emit(actionName, defaults)
            }
        }

        if (providers.isNotEmpty() && clients.isEmpty()) {
            val disambiguate = providers.size > 1
            providers.forEach { prov ->
                val name: String
                val comment: String?
                if (disambiguate) {
                    name = "${actionName}_${prov.leaf.tlaName}"
                    comment = "$actionName action on ${prov.leaf.tlaName}"
                } else {
                    name = actionName
                    comment = null
                }
                emit(name, listOf(prov), comment)
            }
        }
        if (clients.isNotEmpty() && providers.isEmpty()) {
            val disambiguate = clients.size > 1
            clients.forEach { cli ->
                val name: String
                val comment: String?
                if (disambiguate) {
                    name = "${actionName}_${cli.leaf.tlaName}"
                    comment = "$actionName action on ${cli.leaf.tlaName}"
                } else {
                    name = actionName
                    comment = null
                }
                emit(name, listOf(cli), comment)
            }
        }
    }

    val helpers = mutableListOf<String>()
    sessionPairs.forEach { pair ->
        helpers += canStartSessionDef(pair)
    }
    val sessionLeaves = sessionPairs
        .flatMap { listOf(it.leafA, it.leafB) }
        .distinctBy { it.name }
    sessionLeaves.forEach { leaf ->
        val leafOffers = offers.filter { it.leaf.tlaName == leaf.tlaName }
        helpers += deadOperatorDef(leaf, leafOffers, stateVarsByLeaf, stateVarNames, killTargets)
    }

    sessionPairs.forEach { pair ->
        result += emitEndSession(pair, pair.leafA, sessionPairs, allVars, stateVarsByLeaf)
        result += emitEndSession(pair, pair.leafB, sessionPairs, allVars, stateVarsByLeaf)
    }

    val unique = result.distinctBy { it.name }
    val (initiallyActions, otherActions) = unique.partition { it.isInitiallyAction() }
    return TlaBuildResult(helpers, initiallyActions + otherActions)
}

/** True for solo `initially` / `Leaf_initially` constructors (defs + Next lead with these). */
private fun TlaAction.isInitiallyAction(): Boolean =
    name == "initially" || name.endsWith("_initially")

private fun allTlaVars(
    leaves: List<SpecLeaf>,
    pclasses: Map<String, ProcClassNode>,
    stateVarNames: Map<Pair<String, String>, String>,
    killTargets: Set<String>,
): List<String> =
    leaves.flatMap { leaf ->
        val pc = pclasses[leaf.name] ?: return@flatMap emptyList()
        val base = mutableListOf(stateTlaName(leaf.tlaName, "constructed", stateVarNames))
        if (leaf.tlaName in killTargets) {
            base += stateTlaName(leaf.tlaName, "killed", stateVarNames)
        }
        if (leaf.isProcFun) {
            base += stateTlaName(leaf.tlaName, "terminated", stateVarNames)
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
    sessionPair: SessionLeafPair? = null,
    allSessionPairs: List<SessionLeafPair> = emptyList(),
    killTargets: Set<String> = emptySet(),
    comment: String? = null,
    returnToByOccurrence: Map<String, String> = emptyMap(),
    blockingByHost: Map<String, String> = emptyMap(),
    hostsWithBlocking: Set<String> = emptySet(),
    pclasses: Map<String, ProcClassNode> = emptyMap(),
    leafSpecs: Map<String, LeafSpecNode> = emptyMap(),
    systemLeaves: List<SpecLeaf> = emptyList(),
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
    val bindPlan = analyzeTlaArgBind(offers, TlaEmitOpts.get())
    val symbolTypes = mutableMapOf<String, Type>()
    offers.forEach { offer ->
        offer.decl.action.args.forEach { symbolTypes[it.name] = it.type }
        pclasses[offer.leaf.name]?.localDecls()?.filterIsInstance<VarNode>()?.forEach { vn ->
            try {
                symbolTypes[vn.name] = vn.type
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
        aux.forEach { a -> auxParams += a.name to a.domain }
    }

    // Collect action arg names that appear in this conjoined action (for binder clash checks).
    val usedArgNames = mutableSetOf<String>()
    usedArgNames += auxParams.map { it.first }
    offers.forEach { offer ->
        offer.decl.action.args.filter { offerRefsArg(offer, it.name) }.forEach { usedArgNames += it.name }
    }
    usedArgNames += bindNames

    // Instance binders for parameterized leaves: paramName indexes into the type domain.
    val selfBinders = linkedMapOf<String, String>() // leafName -> binder
    val sharedWithBinders = linkedMapOf<String, String>() // withScopeId -> binder
    val reserved = usedArgNames.toMutableSet()
    offers.forEach { offer ->
        if (offer.leaf.isParameterized) {
            reserved += stateVarsByLeaf[offer.leaf.tlaName].orEmpty()
        }
    }
    // Session pair may need binders for both leaves even when indexing session after updates.
    val effectSessionPair = resolveSessionEffectPair(offers, sessionPair, allSessionPairs)
    val tearsDownSameSessionPair =
        effectSessionPair != null &&
            sessionPair != null &&
            effectSessionPair.varName == sessionPair.varName
    val pairsNeedingBinders = listOfNotNull(sessionPair, effectSessionPair).distinctBy { it.varName }
    pairsNeedingBinders.forEach { pair ->
        listOf(pair.leafA, pair.leafB).forEach { leaf ->
            if (leaf.isParameterized) {
                reserved += stateVarsByLeaf[leaf.tlaName].orEmpty()
            }
        }
    }
    fun bindLeaf(leaf: SpecLeaf) {
        // Apply-only / leaf-spec under `with`: register shared binder without lifting state.
        if (!leaf.isParameterized && leaf.withScopeId != null && leaf.paramName != null) {
            sharedWithBinders.putIfAbsent(leaf.withScopeId!!, leaf.paramName!!)
            reserved += leaf.paramName!!
            return
        }
        if (!leaf.isParameterized || leaf.tlaName in selfBinders) return
        val binder = indexBinderName(leaf, reserved, sharedWithBinders)
        selfBinders[leaf.tlaName] = binder
        reserved += binder
        leaf.withScopeId?.let { sharedWithBinders.putIfAbsent(it, binder) }
    }
    offers.forEach { offer -> bindLeaf(offer.leaf) }
    pairsNeedingBinders.forEach { pair ->
        bindLeaf(pair.leafA)
        bindLeaf(pair.leafB)
    }

    fun selfOf(leaf: SpecLeaf): String? = selfBinders[leaf.tlaName]

    fun pickOfferFor(expr: ExprNode): TlaActionOffer {
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
            linePrefix = linePrefix,
        )
    }

    bindPlan.listBinds.forEach { b ->
        argParams += b.index to "1..Len(${emitBound(b.list)})"
    }
    bindPlan.setBinds.forEach { b ->
        argParams += b.arg to emitBound(b.set)
    }
    bindPlan.structBinds.forEach { b ->
        argParams += b.tmp to emitBound(b.set)
    }

    val deferCtorSpawn = sessionPair != null && offers.any { it.isConstructor }
    val deferredSpawnParts = mutableListOf<String>()
    val deferredSpawnChanged = mutableSetOf<String>()

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
        fun emitExpr(expr: ExprNode, linePrefix: String): String =
            exprToTla(
                expr, leafCtx, effectiveArgNames(), self,
                bareStateVars = bare,
                stateVarNames = stateVarNames,
                symbolOverrides = letOverrides.toMap(),
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
                    val io = exprContainsIoHavoc(update.init)
                    val ioDomain = if (io) typeToTlaDomain(update.type) else null
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
                    targetChanged += v
                    val expr = update.expr
                    if (exprContainsIoHavoc(expr)) {
                        val domain = typeToTlaDomain(expr.getType())
                        addAssignPart(
                            if (self != null) {
                                "/\\ \\E __io \\in $domain: $v' = [$v EXCEPT ![$self] = __io]"
                            } else {
                                "/\\ $v' \\in $domain"
                            },
                        )
                    } else {
                        val assignPrefix = if (self != null) {
                            "/\\ $v' = [$v EXCEPT ![$self] = "
                        } else {
                            "/\\ $v' = "
                        }
                        val rhs = emitExpr(expr, assignPrefix)
                        addAssignPart(
                            if (self != null) {
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
                    targetChanged += v
                    val k = emitExpr(update.index, "")
                    if (exprContainsIoHavoc(update.value)) {
                        val domain = typeToTlaDomain(update.value.getType())
                        addAssignPart(
                            if (self != null) {
                                "/\\ \\E __io \\in $domain: $v' = [$v EXCEPT ![$self] = [@ EXCEPT ![$k] = __io]]"
                            } else {
                                "/\\ \\E __io \\in $domain: $v' = [$v EXCEPT ![$k] = __io]"
                            },
                        )
                    } else {
                        val putPrefix = if (self != null) {
                            "/\\ $v' = [$v EXCEPT ![$self] = [@ EXCEPT ![$k] = "
                        } else {
                            "/\\ $v' = [$v EXCEPT ![$k] = "
                        }
                        val vv = emitExpr(update.value, putPrefix)
                        addAssignPart(
                            if (self != null) {
                                "/\\ $v' = [$v EXCEPT ![$self] = [@ EXCEPT ![$k] = $vv]]"
                            } else {
                                "/\\ $v' = [$v EXCEPT ![$k] = $vv]"
                            },
                        )
                    }
                }
            }
        }
        if (offer.isConstructor) {
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
        if (offer.decl.isReturn && offer.leaf.isProcFun &&
            offer.decl.action.name == procFunRetAction(offer.leaf.name)
        ) {
            val term = stateTlaName(offer.leaf.tlaName, "terminated", stateVarNames)
            targetChanged += term
            addAssignPart(
                if (self != null) {
                    "/\\ $term' = [$term EXCEPT ![$self] = TRUE]"
                } else {
                    "/\\ $term' = TRUE"
                },
            )
            val returnTo = returnToByOccurrence[offer.leaf.occurrenceId]
            if (returnTo != null) {
                targetChanged += returnTo
                addAssignPart(
                    if (self != null) {
                        "/\\ $returnTo' = [$returnTo EXCEPT ![$self] = TRUE]"
                    } else {
                        "/\\ $returnTo' = TRUE"
                    },
                )
            }
        }

        targetParts += prefixParts
        if (bindings.isNotEmpty() && (scopedParts.isNotEmpty() || bindings.any { it.ioDomain != null })) {
            targetParts += wrapTransitLetBlock(bindings, scopedParts)
        } else {
            targetParts += scopedParts
        }
    }

    // Per-offer sections: comment + gates + guards + transits (Julay-like layout).
    offers.forEach { offer ->
        parts += "\\* ${offer.leaf.name} ${tlaTransitionTypeLabel(offer)} logic"
        val c = stateTlaName(offer.leaf.tlaName, "constructed", stateVarNames)
        val self = selfOf(offer.leaf)
        if (offer.isConstructor) {
            parts += if (self != null) "/\\ ~$c[$self]" else "/\\ ~$c"
        } else {
            parts += if (self != null) "/\\ $c[$self]" else "/\\ $c"
        }
        if (offer.leaf.tlaName in killTargets) {
            val killed = stateTlaName(offer.leaf.tlaName, "killed", stateVarNames)
            parts += if (self != null) "/\\ ~$killed[$self]" else "/\\ ~$killed"
        }
        if (offer.leaf.isProcFun) {
            val term = stateTlaName(offer.leaf.tlaName, "terminated", stateVarNames)
            parts += if (self != null) "/\\ ~$term[$self]" else "/\\ ~$term"
        }
        // Host leaf blocked while awaiting a procfun return (child steps ignore this).
        if (!offer.leaf.isProcFun && offer.leaf.tlaName in hostsWithBlocking) {
            val blocking = blockingByHost.getValue(offer.leaf.tlaName)
            parts += if (self != null) "/\\ ~$blocking[$self]" else "/\\ ~$blocking"
        }

        val auxNames = auxNamesByLeaf[offer.leaf.tlaName].orEmpty()
        val argNames = offer.decl.action.args.map { it.name }.toSet() + auxNames + bindNames
        val leafCtx = mapOf(offer.leaf.name to offer.leaf, offer.leaf.tlaName to offer.leaf)

        // Only include args that appear in guards/transits (skip unused initially args).
        offer.decl.action.args.filter { offerRefsArg(offer, it.name) }.forEach { arg ->
            if (arg.name in bindPlan.omitArgTypeDomains()) return@forEach
            val lit = argLiteralDomain(offers, arg.name, arg.type)
            argParams += arg.name to (lit ?: typeToTlaDomain(arg.type))
        }

        offer.decl.guards.forEach { g ->
            // Flatten top-level `&` so each Julay conjunct is its own `/\\` line.
            flattenTopLevelAnd(g).forEach { conjunct ->
                if (bindPlan.skipped(conjunct)) return@forEach
                parts += "/\\ ${exprToTla(
                    conjunct, leafCtx, argNames, self,
                    bareStateVars = stateVarsByLeaf[offer.leaf.tlaName].orEmpty(),
                    stateVarNames = stateVarNames,
                    linePrefix = "/\\ ",
                )}"
            }
        }

        // Defer constructor spawn updates into the CanStart THEN branch (throw-before-launch).
        if (deferCtorSpawn && offer.isConstructor) {
            emitTransitUpdates(offer, deferredSpawnParts, deferredSpawnChanged)
        } else {
            emitTransitUpdates(offer, parts, changed)
            if (offerHasExitProc(offer)) {
                val killedVar = stateTlaName(offer.leaf.tlaName, "killed", stateVarNames)
                parts += "/\\ ${killedAssignTrueExpr(offer.leaf, self, stateVarNames)}"
                changed += killedVar
            }
        }
    }

    bindPlan.structBinds.forEach { b ->
        val elemType = try {
            (b.set.getType() as? SetType)?.elementType
        } catch (_: RuntimeException) {
            null
        }
        b.keep.forEach { (path, expr) ->
            val lhs = emitUnwrappedFieldPath(b.tmp, elemType, path)
            parts += "/\\ $lhs = ${emitBound(expr)}"
        }
    }

    if (sessionPair != null) {
        val binderA = selfBinders[sessionPair.leafA.tlaName]
        val binderB = selfBinders[sessionPair.leafB.tlaName]
        val lookup = sessionLookup(sessionPair, binderA, binderB)
        val canStart = canStartCall(sessionPair, binderA, binderB)
        parts += "\\* Session connection semantics"
        if (deferCtorSpawn) {
            // sessionException mirrors runtime JulayException on session-ctor rebind
            val thenParts = mutableListOf<String>()
            thenParts += sessionAssignTrueExpr(sessionPair, binderA, binderB)
            thenParts.addAll(deferredSpawnParts.map { it.removePrefix("/\\ ") })
            thenParts += "UNCHANGED sessionException"
            val elseUnchanged = (listOf(sessionPair.varName) + deferredSpawnChanged.toList()).distinct()
            val elseParts = mutableListOf<String>()
            elseParts += "sessionException' = TRUE"
            elseParts += "UNCHANGED <<${elseUnchanged.joinToString(", ")}>>"
            val thenBody = thenParts.joinToString("\n          /\\ ")
            val elseBody = elseParts.joinToString("\n          /\\ ")
            parts += "/\\ IF $canStart"
            parts += "   THEN /\\ $thenBody"
            parts += "   ELSE /\\ $elseBody"
            changed += sessionPair.varName
            changed += deferredSpawnChanged
            changed += "sessionException"
        } else if (tearsDownSameSessionPair) {
            // Rendezvous may start or continue the session; exit/kill effect sets session' below.
            parts += "/\\ ($lookup \\/ $canStart)"
        } else {
            parts += "/\\ ($lookup \\/ $canStart)"
            parts += "/\\ ${sessionAssignTrueExpr(sessionPair, binderA, binderB)}"
            changed += sessionPair.varName
        }
    }

    if (effectSessionPair != null) {
        val hasExit = offers.any { "exitSession" in sessionEffectNames(it) }
        val hasKill = offers.any { "killSessionPeer" in sessionEffectNames(it) }
        if (hasExit && hasKill) {
            throw RuntimeException(
                "TLA+: action \"$name\" cannot use both exitSession and killSessionPeer",
            )
        }
        val effectName = if (hasKill) "killSessionPeer" else "exitSession"
        val caller = sessionEffectCaller(offers, effectName)
            ?: throw RuntimeException("TLA+: $effectName missing caller leaf")
        val binderA = selfBinders[effectSessionPair.leafA.tlaName]
        val binderB = selfBinders[effectSessionPair.leafB.tlaName]
        val lookup = sessionLookup(effectSessionPair, binderA, binderB)
        // When a session exists, require the correct peer index (lookup); when none exists, no-op.
        // Avoids cancelTimer(c, wrong_h) clearing transit while leaving the real helper session live.
        val anyLive = anySessionWithCaller(effectSessionPair, caller, binderA, binderB)
        parts += "\\* Session connection semantics"
        val thenParts = mutableListOf<String>()
        thenParts += lookup
        thenParts += sessionAssignFalseExpr(effectSessionPair, binderA, binderB)
        val elseUnchanged = mutableListOf(effectSessionPair.varName)
        if (hasKill) {
            val peer = peerLeafOf(effectSessionPair, caller)
            val peerBinder = selfBinders[peer.name]
            val killedVar = stateTlaName(peer.name, "killed", stateVarNames)
            thenParts += killedAssignTrueExpr(peer, peerBinder, stateVarNames)
            elseUnchanged += killedVar
            changed += killedVar
        }
        val thenBody = thenParts.joinToString("\n          /\\ ")
        parts += "/\\ IF $anyLive"
        parts += "   THEN /\\ $thenBody"
        parts += "   ELSE /\\ UNCHANGED <<${elseUnchanged.joinToString(", ")}>>"
        changed += effectSessionPair.varName
    }

    val unchanged = allVars.filter { it !in changed }
    // Guard-only / pure-stutter actions: no primed updates → omit (stuttering via [][Next]_vars).
    if (changed.isEmpty()) {
        return null
    }
    if (unchanged.isNotEmpty()) {
        parts += "/\\ UNCHANGED <<${unchanged.joinToString(", ")}>>"
    }

    val innerBody = parts.joinToString("\n  ")
    val letBindings = mutableListOf<Pair<String, String>>()
    bindPlan.listBinds.forEach { b ->
        letBindings += b.arg to "${emitBound(b.list)}[${b.index}]"
    }
    bindPlan.structBinds.forEach { b ->
        val elemType = try {
            (b.set.getType() as? SetType)?.elementType
        } catch (_: RuntimeException) {
            null
        }
        b.argPaths.forEach { (arg, path) ->
            letBindings += arg to emitUnwrappedFieldPath(b.tmp, elemType, path)
        }
    }
    bindPlan.determined.forEach { (arg, expr) ->
        letBindings += arg to emitBound(expr, "      $arg == ")
    }
    val body = wrapTlaLet(letBindings, innerBody)
    val indexParams = selfBinders.map { (leafName, binder) ->
        val leaf = offers.firstOrNull { it.leaf.tlaName == leafName }?.leaf
            ?: pairsNeedingBinders.firstNotNullOfOrNull { p ->
                when (leafName) {
                    p.leafA.tlaName -> p.leafA
                    p.leafB.tlaName -> p.leafB
                    else -> null
                }
            }
            ?: error("missing leaf $leafName")
        val domain = typeDomainConstant(leaf.paramType!!) ?: leaf.paramType.toString()
        binder to domain
    }
    // Aux params that are already index binders (same name) are dropped by distinctBy.
    val params = (indexParams + auxParams + argParams).distinctBy { it.first }
    val signature = if (params.isEmpty()) {
        name
    } else {
        "$name(${params.joinToString(", ") { it.first }})"
    }
    return TlaAction(name, "$signature ==\n  $body", params, comment)
}

/** Prefer [SpecLeaf.paramName] as the TLA binder; append `_<leaf>` on name clashes.
 * Leaves that share [SpecLeaf.withScopeId] reuse the same binder string.
 */
internal fun indexBinderName(leaf: SpecLeaf, reserved: Set<String>, sharedBinders: Map<String, String> = emptyMap()): String {
    val base = leaf.paramName!!
    val scope = leaf.withScopeId
    if (scope != null) {
        sharedBinders[scope]?.let { return it }
    }
    return if (base !in reserved) base else "${base}_${leaf.tlaName}"
}

/**
 * Map (leafTlaName, julayVarName) → TLA identifier.
 * `constructed` is always `Leaf_constructed`; `killed` only for [killTargets] (by tlaName);
 * other vars are bare unless duplicated or reserved.
 *
 * Future: Julay source may allow composite qualifiers such as `Y.X.n` where `proc Y := X || ...`;
 * not implemented — invariants still write `X.n` and are expanded per occurrence at emit time.
 */
internal fun buildStateVarNames(
    leaves: List<SpecLeaf>,
    pclasses: Map<String, ProcClassNode>,
    reservedIds: Set<String>,
    killTargets: Set<String> = emptySet(),
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
        if (id in killTargets || leaf.tlaName in killTargets) {
            out[id to "killed"] = "${id}_killed"
        }
        if (leaf.isProcFun) {
            out[id to "terminated"] = "${id}_terminated"
        }
        val pc = pclasses[leaf.name] ?: return@forEach
        pc.localDecls().filterIsInstance<VarNode>().forEach { vn ->
            val clash = (ownersByVar[vn.name]?.size ?: 0) > 1 || vn.name in reservedIds
            out[id to vn.name] = if (clash) "${id}_${vn.name}" else vn.name
        }
    }
    return out
}

/** Action argument names across leaves — reserved so bare state vars do not shadow params. */
internal fun actionArgNames(
    leaves: List<SpecLeaf>,
    pclasses: Map<String, ProcClassNode>,
): Set<String> {
    val names = linkedSetOf<String>()
    leaves.forEach { leaf ->
        val pc = pclasses[leaf.name] ?: return@forEach
        (pc.localDecls().flatMap { it.constructors() } + pc.localDecls().flatMap { it.transitions() })
            .forEach { action -> action.action.args.forEach { names += it.name } }
    }
    return names
}

internal fun stateTlaName(
    leaf: String,
    varName: String,
    names: Map<Pair<String, String>, String>,
): String = names[leaf to varName] ?: tlaVar(leaf, varName)

internal fun tlaVar(leaf: String, varName: String): String = "${leaf}_$varName"

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
    is SortType -> type.cfgElements.firstOrNull() ?: defaultTlaValue(type.elementType)
    is ListType -> "<<>>"
    is SetType -> "{}"
    is MapType -> "[x \\in {} |-> 0]"
    is ObjClassType -> {
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
    else -> "0"
    }
}

/** Julay leaf class names used as `Leaf.var` in an invariant formula. */
private fun collectInvariantLeafBases(expr: ExprNode): Set<String> {
    val out = linkedSetOf<String>()
    fun walk(e: ExprNode) {
        when (e) {
            is FieldAccessExprNode -> {
                // Leaf.var (single field) — not object field chains on args.
                if (e.fieldPath.size == 1) out += e.baseSymbol
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
    fun formatInv(name: String, remapped: Map<Pair<String, String>, String>): String {
        val body = exprToTla(
            node.invariantFormula(),
            leafCtx = emptyMap(),
            argNames = emptySet(),
            self = null,
            bareStateVars = emptySet(),
            reservedNames = constants,
            stateVarNames = remapped,
            linePrefix = "",
            parentPrec = PREC_BOTTOM,
        )
        return if (body.contains('\n') || isMultiLineExpr(node.invariantFormula())) {
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
}

internal fun cfgConstantModel(name: String): String = when (name) {
    "Int", "Nat", "Real" -> "{0, 1, 2, 3, 4, 5}"
    "String" -> "{\"\", \"0\", \"1\", \"2\", \"3\", \"4\", \"5\", \"a\", \"b\"}"
    "Boolean" -> "BOOLEAN"
    "MaxListLen" -> "3"
    else -> "{\"a\", \"b\"}" // parameter sets and other domains
}

internal fun cfgIntModel(values: Set<Int>): String =
    // TLC .cfg parsers reject unary-minus in set literals; negatives still appear via assignments.
    "{${values.filter { it >= 0 }.sorted().joinToString(", ")}}"

internal fun cfgStringModel(intValues: Set<Int>): String {
    val strs = linkedSetOf("", "a", "b")
    intValues.forEach { strs += it.toString() }
    return "{${strs.joinToString(", ") { "\"$it\"" }}}"
}

private fun collectIntLiteralsFromLeaves(
    leaves: List<SpecLeaf>,
    pclasses: Map<String, ProcClassNode>,
    into: MutableSet<Int>,
) {
    leaves.forEach { leaf ->
        val pc = pclasses[leaf.name] ?: return@forEach
        (pc.localDecls().flatMap { it.constructors() } + pc.localDecls().flatMap { it.transitions() })
            .forEach { action ->
                action.guards.forEach { collectIntLiteralsFromExpr(it, into) }
                action.transits.forEach { update ->
                    when (update) {
                        is TransitUpdate.Assign -> collectIntLiteralsFromExpr(update.expr, into)
                        is TransitUpdate.IndexPut -> {
                            collectIntLiteralsFromExpr(update.index, into)
                            collectIntLiteralsFromExpr(update.value, into)
                        }
                        is TransitUpdate.Let -> collectIntLiteralsFromExpr(update.init, into)
                    }
                }
            }
    }
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
): String {
    if (type is StringType || type is IntType) {
        val lits = if (fieldOfObj != null && fieldName != null) {
            TlaLiteralDomainProjection.get().objFieldSet(fieldOfObj, fieldName)
        } else {
            null
        }
        if (lits != null && lits.isNotEmpty()) {
            return TlaLiteralDomainProjection.get().render(lits)
        }
    }
    return when (type) {
    is BoolType -> "BOOLEAN"
    is IntType -> "Int"
    is RealType -> "Int"
    is StringType -> "String"
    is SortType -> type.name
    // Seq(S) is infinite; TLC needs a length-bounded set of sequences.
    is ListType -> "BoundedSeq(${typeToTlaDomain(type.elementType)}, MaxListLen)"
    is SetType -> "SUBSET ${typeToTlaDomain(type.elementType)}"
    is ObjClassType -> {
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
    else -> "Int"
    }
}

/** Collect sort CONSTANT names nested in [type]. */
internal fun collectSortConstants(type: Type, into: MutableSet<String>) {
    when (type) {
        is SortType -> into += type.name
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
        is SortType -> into += type.name
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
        val omit = analyzeTlaArgBind(group, TlaEmitOpts.get()).omitArgTypeDomains()
        group.forEach { offer ->
            offer.decl.action.args.filter { offerRefsArg(offer, it.name) && it.name !in omit }.forEach { arg ->
                collectSortConstants(arg.type, into)
            }
        }
    }
    offers.forEach { offer ->
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
    into: MutableSet<String>,
) {
    offers.groupBy { it.decl.action.name }.forEach { (_, group) ->
        val omit = analyzeTlaArgBind(group, TlaEmitOpts.get()).omitArgTypeDomains()
        group.forEach { offer ->
            offer.decl.action.args.filter { offerRefsArg(offer, it.name) && it.name !in omit }.forEach { arg ->
                if (argLiteralDomain(group, arg.name, arg.type) == null) {
                    collectDomainModelNames(arg.type, into)
                }
            }
        }
    }
    offers.forEach { offer ->
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
            try {
                collectDomainModelNames(arg.type, into)
            } catch (_: RuntimeException) {
            }
        }
    }
}

internal fun offerRefsArg(offer: TlaActionOffer, argName: String): Boolean =
    offer.decl.guards.any { exprReferencesSymbol(it, argName) } ||
        offer.decl.transits.any { update ->
            when (update) {
                is TransitUpdate.Assign -> exprReferencesSymbol(update.expr, argName)
                is TransitUpdate.IndexPut ->
                    exprReferencesSymbol(update.index, argName) || exprReferencesSymbol(update.value, argName)
                is TransitUpdate.Let -> exprReferencesSymbol(update.init, argName)
            }
        } ||
        (offer.decl.returnExpr?.let { exprReferencesSymbol(it, argName) } == true)

private fun collectIoHavocDomainModels(
    leaves: List<SpecLeaf>,
    pclasses: Map<String, ProcClassNode>,
    into: MutableSet<String>,
) {
    leaves.forEach { leaf ->
        val pc = pclasses[leaf.name] ?: return@forEach
        (pc.localDecls().flatMap { it.constructors() } + pc.localDecls().flatMap { it.transitions() })
            .forEach { action ->
                action.transits.forEach { update ->
                    when (update) {
                        is TransitUpdate.Assign -> if (exprContainsIoHavoc(update.expr)) {
                            try {
                                collectDomainModelNames(update.expr.getType(), into)
                            } catch (_: RuntimeException) {
                            }
                        }
                        is TransitUpdate.IndexPut -> if (exprContainsIoHavoc(update.value)) {
                            try {
                                collectDomainModelNames(update.value.getType(), into)
                            } catch (_: RuntimeException) {
                            }
                        }
                        is TransitUpdate.Let -> if (exprContainsIoHavoc(update.init)) {
                            try {
                                collectDomainModelNames(update.init.getType(), into)
                            } catch (_: RuntimeException) {
                            }
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
 * Resolve an AG guarantee to a root [InvariantNode], or null when there is no guarantee.
 * Named invariant symbols reuse the declared node; other formulas get a synthetic root.
 */
internal fun resolveGuaranteeInvariant(
    ag: AgSpecExprNode,
    specName: String,
    invariants: Map<String, InvariantNode>,
): InvariantNode? {
    val guarantee = ag.guaranteeExpr() ?: return null
    if (guarantee is SymbolValueExprNode && invariants.containsKey(guarantee.symbol)) {
        return invariants.getValue(guarantee.symbol)
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
    return InvariantNode(syntheticName, guarantee, ag.programLocation())
}

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
    julay.compiler.ast.isEmptyStringLiteral(expr)

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

private fun tlaLengthOf(baseRendered: String, baseExpr: ExprNode): String =
    when (try { baseExpr.getType() } catch (_: RuntimeException) { null }) {
        is ListType -> "Len($baseRendered)"
        is SetType -> "Cardinality($baseRendered)"
        is MapType -> "Cardinality($baseRendered)" // maps emit as functions; prefer keys cardinality if needed
        else -> "Len($baseRendered)"
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
 * Julay `splice(xs, s, e)` helper: 1-based inclusive with clamp, via TLA `SubSeq`.
 * `e < 1` → empty (empty prefix); else `hi = min(e, Len)`; `lo = s`; empty if `lo > hi`;
 * else `SubSeq(xs, lo, hi)`.
 * Parameter / binder names are clash-renamed against [reservedNames].
 */
internal fun emitSpliceOperatorDef(reservedNames: Set<String>): String {
    val taken = reservedNames.toMutableSet()
    val xs = allocTlaName("xs", taken)
    val s = allocTlaName("s", taken)
    val e = allocTlaName("e", taken)
    val lo = allocTlaName("lo", taken)
    val hi = allocTlaName("hi", taken)
    return "splice($xs, $s, $e) ==\n" +
        "  IF $e < 1 THEN <<>>\n" +
        "  ELSE LET $hi == IF $e > Len($xs) THEN Len($xs) ELSE $e\n" +
        "           $lo == $s\n" +
        "       IN IF $lo > $hi THEN <<>> ELSE SubSeq($xs, $lo, $hi)"
}

/** Julay `startsWith` as a TLA operator (TLC strings are sequences). */
internal fun emitStartsWithOperatorDef(reservedNames: Set<String>): String {
    val taken = reservedNames.toMutableSet()
    val str = allocTlaName("s", taken)
    val prefix = allocTlaName("p", taken)
    return "startsWith($str, $prefix) ==\n" +
        "  IF Len($prefix) > Len($str) THEN FALSE ELSE SubSeq($str, 1, Len($prefix)) = $prefix"
}

private val tlaUnsupportedBuiltins = setOf("split", "parseInt", "trim", "portFromUrl")

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

/** User funs called from Init/action guards and transit RHS (plus transitive callees). */
internal fun collectUserFunsUsedInOffers(offers: List<TlaActionOffer>): LinkedHashSet<FunNode> {
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
 * Prefer [preferred]; if taken, use `p_preferred`, then `p_preferred_2`, …
 * Adds the chosen name to [taken].
 */
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
 * Build a single action conjunct: nested `LET` (and optional IO `\\E`) around [bodyConjuncts].
 * Each body conjunct should already start with `/\\ `.
 */
internal fun wrapTransitLetBlock(
    bindings: List<TransitLetEmit>,
    bodyConjuncts: List<String>,
): String {
    require(bindings.isNotEmpty()) { "wrapTransitLetBlock requires bindings" }
    var text = if (bodyConjuncts.isEmpty()) {
        "TRUE"
    } else {
        bodyConjuncts.joinToString("\n")
    }
    for (i in bindings.indices.reversed()) {
        val b = bindings[i]
        val depth = i
        // Action defs join parts with "\n  ", so the first line of this block gets a 2-space
        // module indent; continuation lines must already be indented past that `/\`.
        // Preserve relative indents inside multi-line RHS (nested IF); flattening them to the
        // same column as `/\` makes SANY reject the junction item.
        val bodyPad = " ".repeat(4 + depth * 2)
        val letPad = " ".repeat(4 + (depth - 1).coerceAtLeast(0) * 2)
        val indentedBody = padPreservingRelative(text, bodyPad)
        text = when {
            b.ioDomain != null -> {
                val ioName = "__io_${b.tlaName}"
                if (depth == 0) {
                    "/\\ \\E $ioName \\in ${b.ioDomain}:\n" +
                        "    LET ${b.tlaName} == $ioName IN\n$indentedBody"
                } else {
                    "${letPad}\\E $ioName \\in ${b.ioDomain}:\n" +
                        "${letPad}  LET ${b.tlaName} == $ioName IN\n$indentedBody"
                }
            }
            b.initTla.contains('\n') -> {
                val initPad = " ".repeat(4 + depth * 2)
                val initIndented = padPreservingRelative(b.initTla, initPad)
                if (depth == 0) {
                    "/\\ LET ${b.tlaName} ==\n$initIndented\n    IN\n$indentedBody"
                } else {
                    "${letPad}LET ${b.tlaName} ==\n$initIndented\n${bodyPad}IN\n$indentedBody"
                }
            }
            depth == 0 -> "/\\ LET ${b.tlaName} == ${b.initTla} IN\n$indentedBody"
            else -> "${letPad}LET ${b.tlaName} == ${b.initTla} IN\n$indentedBody"
        }
    }
    return text
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
    val body = exprToTla(
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
    return if (body.contains('\n') || isMultiLineExpr(bodyExpr)) {
        "$header ==\n  $body"
    } else {
        "$header == $body"
    }
}

/**
 * Split a guard expression on top-level `&` so each Julay conjunct can be a separate TLA `/\\` line.
 * Nested `&` inside `|` / other operators is left intact.
 */
internal fun flattenTopLevelAnd(expr: ExprNode): List<ExprNode> {
    if (expr is BinaryOpExprNode && expr.op() == "&") {
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

internal fun emitUnwrappedFieldPath(base: String, type: Type?, path: List<String>): String {
    val dropped = if (type != null) TlaFieldProjection.get().dropUnwrappedPath(type, path) else path
    return if (dropped.isEmpty()) base else dropped.fold(base) { acc, f -> "$acc.$f" }
}

private fun typeOfBaseExpr(expr: ExprNode): Type? = try {
    expr.getType()
} catch (_: RuntimeException) {
    (expr as? SymbolValueExprNode)?.let { TlaSymbolTypes.get()[it.symbol] }
}

internal fun wrapTlaLet(bindings: List<Pair<String, String>>, body: String): String {
    if (bindings.isEmpty()) return body
    val defs = bindings.joinToString("\n      ") { (n, e) -> "$n == $e" }
    return if (body.contains('\n')) {
        "LET $defs\n  IN\n  $body"
    } else {
        "LET $defs IN $body"
    }
}

/**
 * Julay `let` → TLA `LET name == init IN body`. Multi-line source uses line breaks;
 * [body] / [init] should already be rendered (with let-name overrides applied to [body]).
 */
internal fun emitLetToTla(
    name: String,
    init: String,
    body: String,
    linePrefix: String,
    multiLine: Boolean,
    open: String = "",
    close: String = "",
): String {
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

private fun whenLiteralToTla(lit: WhenLiteral): String = when (lit) {
    is WhenLiteral.IntLit -> lit.value
    is WhenLiteral.RealLit -> lit.value
    is WhenLiteral.BoolLit -> if (lit.value == "true") "TRUE" else "FALSE"
    is WhenLiteral.StringLit -> "\"${lit.value}\""
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
    val andOp = "/\\ "
    val visualBefore = visualObjOpenLinePrefix(linePrefix)
    val andContIndent = " ".repeat(visualBefore.length)
    return buildString {
        conjuncts.forEachIndexed { i, c ->
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
    val fieldEntries = TlaFieldProjection.get().filterLiteralEntries(objName, expr.fieldEntries)
    if (fieldEntries.isEmpty()) {
        return "[dummy |-> 0]"
    }
    val objType = try {
        expr.structType
    } catch (_: RuntimeException) {
        null
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
): String {
    fun rec(e: ExprNode, prefix: String = linePrefix, prec: Int = parentPrec): String =
        exprToTla(
            e, leafCtx, argNames, self, bareStateVars, reservedNames, stateVarNames, symbolOverrides,
            prefix, prec,
        )
    fun recWithOverrides(e: ExprNode, extra: Map<String, String>, prefix: String = linePrefix, prec: Int = parentPrec): String =
        exprToTla(
            e, leafCtx, argNames, self, bareStateVars, reservedNames, stateVarNames,
            symbolOverrides + extra, prefix, prec,
        )
    return when (expr) {
        is LiteralValueExprNode -> literalToTla(expr)
        is SymbolValueExprNode -> {
            val sym = expr.symbol
            when {
                sym in symbolOverrides -> symbolOverrides.getValue(sym)
                sym in argNames -> sym
                sym in bareStateVars && leafCtx.size == 1 -> {
                    val leaf = leafCtx.values.first()
                    val v = stateTlaName(leaf.tlaName, sym, stateVarNames)
                    if (self != null) "$v[$self]" else v
                }
                else -> sym
            }
        }
        is ThisAccessExprNode -> {
            // Force state: never treat the root as an action arg even when names collide.
            val root = expr.stateVarName()
            val rest = expr.nestedFieldPath()
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
                symbolOverrides, linePrefix, parentPrec,
            )
        }
        is ListLiteralExprNode -> {
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
            val rawName = expr.letName()
            // Discard `let _ := e in body` → emit only body (init unused in TLA).
            if (rawName.isDiscardBinding()) {
                return rec(expr.bodyExpr(), linePrefix, parentPrec)
            }
            val needParen = PREC_IMPLIES < parentPrec
            val open = if (needParen) "(" else ""
            val close = if (needParen) ")" else ""
            val name = rawName
            val multi = isMultiLineExpr(expr)
            val initPrefix = if (multi && isMultiLineExpr(expr.letInitExpr())) {
                exprBodyIndent(linePrefix + open)
            } else {
                "$linePrefix${open}LET $name == "
            }
            val init = rec(expr.letInitExpr(), initPrefix, PREC_BOTTOM)
            val bodyPrefix = if (multi) exprBodyIndent(linePrefix + open) else "$linePrefix${open}LET $name == $init IN "
            // Bind the let name so it is not rewritten as a leaf state var / arg index.
            val body = recWithOverrides(expr.bodyExpr(), mapOf(name to name), bodyPrefix, PREC_BOTTOM)
            emitLetToTla(name, init, body, linePrefix, multi, open, close)
        }
        is FieldAccessExprNode -> {
            val base = expr.baseSymbol
            if (expr.fieldPath == listOf("length")) {
                val baseRendered = when {
                    base in symbolOverrides -> symbolOverrides.getValue(base)
                    base in argNames -> base
                    base in bareStateVars && leafCtx.size == 1 -> {
                        val leaf = leafCtx.values.first()
                        val v = stateTlaName(leaf.tlaName, base, stateVarNames)
                        if (self != null) "$v[$self]" else v
                    }
                    else -> base
                }
                // Bare `xs.length` via FieldAccess: lists are the common case (Len).
                // Prefer MemberAccess / FieldAccessOnExpr when the base expr carries a Set type.
                return "Len($baseRendered)"
            }
            // Lambda / HOF binder substitution (e.g. map(e -> e.value)).
            if (base in symbolOverrides) {
                val baseRendered = symbolOverrides.getValue(base)
                return emitUnwrappedFieldPath(baseRendered, TlaSymbolTypes.get()[base], expr.fieldPath)
            }
            val isObjectField = base in argNames || base in bareStateVars
            val leafVarName = expr.fieldPath.singleOrNull()?.let { stateVarNames[base to it] }
            if (!isObjectField && leafVarName != null) {
                // Invariant / non-param Leaf.var
                leafVarName
            } else {
                // Object field on arg or state var: base.field… (with indexing when needed)
                val baseRendered = when {
                    base in argNames -> base
                    base in bareStateVars && leafCtx.size == 1 -> {
                        val leaf = leafCtx.values.first()
                        val v = stateTlaName(leaf.tlaName, base, stateVarNames)
                        if (self != null) "$v[$self]" else v
                    }
                    else -> base
                }
                emitUnwrappedFieldPath(baseRendered, TlaSymbolTypes.get()[base], expr.fieldPath)
            }
        }
        is MemberAccessExprNode -> {
            val base = expr.baseExpr
            if (expr.fieldName == "length") {
                return tlaLengthOf(rec(base), base)
            }
            when {
                base is SymbolValueExprNode && base.symbol in symbolOverrides ->
                    emitUnwrappedFieldPath(
                        symbolOverrides.getValue(base.symbol),
                        TlaSymbolTypes.get()[base.symbol],
                        listOf(expr.fieldName),
                    )
                base is IndexExprNode && base.base is SymbolValueExprNode -> {
                    val leafName = (base.base as SymbolValueExprNode).symbol
                    val v = stateTlaName(leafName, expr.fieldName, stateVarNames)
                    "$v[${rec(base.index)}]"
                }
                base is SymbolValueExprNode && stateVarNames.containsKey(base.symbol to expr.fieldName) ->
                    stateTlaName(base.symbol, expr.fieldName, stateVarNames)
                else -> {
                    val rendered = rec(base)
                    emitUnwrappedFieldPath(rendered, typeOfBaseExpr(base), listOf(expr.fieldName))
                }
            }
        }
        is FieldAccessOnExprNode -> {
            if (expr.fieldPath == listOf("length")) {
                return tlaLengthOf(rec(expr.baseExpr), expr.baseExpr)
            }
            val base = rec(expr.baseExpr)
            emitUnwrappedFieldPath(base, typeOfBaseExpr(expr.baseExpr), expr.fieldPath)
        }
        is FunCallExprNode -> {
            when (expr.callName()) {
                "length" -> {
                    val arg = expr.callArgs().singleOrNull() ?: return "TRUE"
                    tlaLengthOf(rec(arg), arg)
                }
                "splice" -> {
                    val args = expr.callArgs()
                    if (args.size != 3) return "TRUE"
                    "splice(${rec(args[0])}, ${rec(args[1])}, ${rec(args[2])})"
                }
                "map" -> {
                    // map(xs, f) — prefer method form; support freestanding.
                    val args = expr.callArgs()
                    if (args.size < 2) return "TRUE"
                    val xs = args[0]
                    val f = args[1]
                    emitMapToTla(xs, f, ::rec, ::recWithOverrides)
                }
                else -> {
                    val userFun = expr.resolvedFunOrNull()
                    if (userFun != null) {
                        val args = expr.callArgs().joinToString(", ") { rec(it, linePrefix, PREC_BOTTOM) }
                        if (args.isEmpty()) userFun.name() else "${userFun.name()}($args)"
                    } else {
                        when (expr.resolvedBuiltinOrNull()?.name ?: expr.callName()) {
                            "startsWith" -> {
                                val args = expr.callArgs()
                                if (args.size != 2) return "TRUE"
                                "startsWith(${rec(args[0], linePrefix, PREC_BOTTOM)}, ${rec(args[1], linePrefix, PREC_BOTTOM)})"
                            }
                            else -> "TRUE"
                        }
                    }
                }
            }
        }
        is MapLiteralExprNode -> {
            if (expr.entries.isEmpty()) {
                "[x \\in {} |-> 0]"
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
                            emitMapLambdaToTla(expr.baseExpr, hofParams[0], hofBody, ::rec, ::recWithOverrides)
                        f != null ->
                            emitMapToTla(expr.baseExpr, f, ::rec, ::recWithOverrides)
                        else -> "TRUE"
                    }
                }
                "filter" -> {
                    val hofBody = expr.hofBodyOrNull()
                    val hofParams = expr.hofParamNamesOrNull()
                    if (hofBody == null || hofParams == null || hofParams.size != 1) return "TRUE"
                    val p = hofParams[0]
                    val xs = rec(expr.baseExpr)
                    when (try { expr.baseExpr.getType() } catch (_: RuntimeException) { null }) {
                        is ListType -> {
                            val pred = recWithOverrides(hofBody, mapOf(p to p))
                            "SelectSeq($xs, LAMBDA $p: $pred)"
                        }
                        is SetType -> {
                            val pred = recWithOverrides(hofBody, mapOf(p to p))
                            "{ $p \\in $xs : $pred }"
                        }
                        else -> "TRUE"
                    }
                }
                else -> "TRUE"
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
            "${rec(expr.base, linePrefix, PREC_ATOM)}[$idx]"
        }
        is UnaryOpExprNode -> {
            when (expr.op()) {
                "~" -> {
                    val o = rec(expr.operand(), linePrefix, PREC_UNARY)
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
            when (expr.op()) {
                "&" -> {
                    if (isMultiLineExpr(expr)) {
                        emitMultiLineAnd(flattenTopLevelAnd(expr), linePrefix) { e, p ->
                            rec(e, p, PREC_AND)
                        }
                    } else {
                        bin(" /\\ ", PREC_AND)
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
                "=" -> bin(" = ", PREC_REL)
                "~=" -> bin(" # ", PREC_REL)
                "<" -> bin(" < ", PREC_REL)
                "<=" -> bin(" <= ", PREC_REL)
                ">" -> bin(" > ", PREC_REL)
                ">=" -> bin(" >= ", PREC_REL)
                "in" -> bin(" \\in ", PREC_REL)
                "~in" -> bin(" \\notin ", PREC_REL)
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
            val domain = when (val t = expr.binderTypeExpr()) {
                is TypeExpr.Simple -> t.name
                else -> t.toString()
            }
            val origBinder = expr.binderName()
            val binder = if (origBinder in reservedNames) "q_$origBinder" else origBinder
            fun fixBinder(b: String): String =
                if (binder != origBinder) {
                    b.replace(Regex("\\b${Regex.escape(origBinder)}\\b"), binder)
                } else {
                    b
                }
            val q = if (expr.isUniversal()) "\\A" else "\\E"
            val header = "$q $binder \\in $domain :"
            if (isMultiLineExpr(expr)) {
                val body = fixBinder(rec(expr.quantifiedBody(), "$linePrefix  ", PREC_BOTTOM))
                "$header\n$linePrefix  $body"
            } else {
                val body = fixBinder(rec(expr.quantifiedBody(), "$linePrefix$header ", PREC_BOTTOM))
                "$header $body"
            }
        }
        is IfElseExprNode -> {
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
        is WhenExprNode -> emitWhenToTla(expr, { e, p -> rec(e, p, PREC_BOTTOM) }, linePrefix, parentPrec)
        else -> "TRUE"
    }
}

private fun emitMapLambdaToTla(
    xsExpr: ExprNode,
    param: String,
    body: ExprNode,
    rec: (ExprNode) -> String,
    recWithOverrides: (ExprNode, Map<String, String>) -> String,
): String {
    val xs = rec(xsExpr)
    return when (try { xsExpr.getType() } catch (_: RuntimeException) { null }) {
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
    return expr.toTransitString(emptyMap(), emptySet()).let { s ->
        when (s) {
            "true" -> "TRUE"
            "false" -> "FALSE"
            else -> s
        }
    }
}
