package julay.compiler.pass

import julay.compiler.CompilationUnit
import julay.compiler.FunBuiltinRegistry
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
)

fun compileSpecToTla(
    spec: SpecNode,
    ast: RootNode,
    unit: CompilationUnit,
    outputDir: File = File("."),
): TlaCodegenResult {
    val result = tlaCodegenPass(spec, ast, unit)
    File(outputDir, "${result.moduleName}.tla").writeText(result.tlaText)
    File(outputDir, "${result.moduleName}.cfg").writeText(result.cfgText)
    return result
}

fun tlaCodegenPass(
    spec: SpecNode,
    ast: RootNode,
    unit: CompilationUnit,
): TlaCodegenResult {
    val moduleName = spec.specNodeName()
    val pclassNodes = unit.modules
        .flatMap { it.root.declNodes().filterIsInstance<ProcClassNode>() }
        .associateBy { it.name() }
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

    val invariants = ast.declNodes().filterIsInstance<InvariantNode>().associateBy { it.name() }
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
    collectActionArgDomainModels(leaves, pclassesForTla, cfgOverrides)
    collectIoHavocDomainModels(leaves, pclassesForTla, cfgOverrides)
    // Collect domains for havoc'd procfun returns.
    havocSites.forEach { site ->
        procFunNodes[site.procFunName]?.let { pf ->
            try {
                val d = typeToTlaDomain(pf.returnType)
                val base = d.trim().removePrefix("(").substringBefore(" ")
                if (base in setOf("Int", "Nat", "Real", "String")) cfgOverrides += base
            } catch (_: RuntimeException) {}
        }
    }
    // String is not provided by EXTENDS; declare it CONSTANT when used as a domain.
    if ("String" in cfgOverrides) {
        constants += "String"
    }

    val intModelValues = linkedSetOf(0, 1, 2, 3, 4, 5)
    collectIntLiteralsFromLeaves(leaves, pclassesForTla, intModelValues)
    invClosure.forEach { collectIntLiteralsFromExpr(it.invariantFormula(), intModelValues) }

    val reservedTlaIds = constants + setOf("Int", "Nat", "Boolean", "Real") +
        actionArgNames(leaves, pclassesForTla) +
        leaves.mapNotNull { it.paramName }.toSet()

    val offers = collectTlaActionOffers(leaves, pclassesForTla, procFunDecls)
        // Coupled: child construct is folded into parent *_call. Standalone: keep F_call.
        .filterNot { it.isConstructor && it.leaf.isProcFun && !standaloneProcFun }
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
                val v = stateTlaName(leaf.tlaName, vn.name, stateVarNames)
                variables += v
                initParts += "/\\ $v = [$binder \\in $domain |-> ${defaultTlaValue(safeType(vn))}]"
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
                val v = stateTlaName(leaf.tlaName, vn.name, stateVarNames)
                variables += v
                initParts += "/\\ $v = ${defaultTlaValue(safeType(vn))}"
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
        havocSites, cfgOverrides,
    )
    val helpers = built.helpers
    val actions = built.actions

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
        if (helpers.isNotEmpty()) {
            helpers.forEach { helper ->
                appendLine(helper)
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

    return TlaCodegenResult(moduleName, tla, cfg)
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
private fun collectEmittedOfferLists(offers: List<TlaActionOffer>): List<List<TlaActionOffer>> {
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

/** Leaf names that are `killSessionPeer` targets somewhere in the module. */
private fun collectKillTargets(
    emittedOfferLists: List<List<TlaActionOffer>>,
    sessionPairs: List<SessionLeafPair>,
): Set<String> {
    val targets = linkedSetOf<String>()
    emittedOfferLists.forEach { offerList ->
        if (offerList.none { "killSessionPeer" in sessionEffectNames(it) }) return@forEach
        val actionPair = sessionPairForOffers(offerList, sessionPairs)
        val effectPair = resolveSessionEffectPair(offerList, actionPair, sessionPairs) ?: return@forEach
        val caller = sessionEffectCaller(offerList, "killSessionPeer") ?: return@forEach
        targets += peerLeafOf(effectPair, caller).tlaName
    }
    return targets
}

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
    val argNames = offer.decl.action.args.map { it.name }.toSet()
    val leafCtx = mapOf(offer.leaf.name to offer.leaf, offer.leaf.tlaName to offer.leaf)
    val bare = stateVarsByLeaf[offer.leaf.tlaName].orEmpty()
    val guardStrs = guards.map {
        exprToTla(it, leafCtx, argNames, self, bareStateVars = bare, stateVarNames = stateVarNames)
    }
    val conj = if (guardStrs.size == 1) guardStrs[0] else guardStrs.joinToString(" /\\ ")
    val usedArgs = offer.decl.action.args.filter { arg ->
        guards.any { exprReferencesSymbol(it, arg.name) }
    }
    return if (usedArgs.isEmpty()) {
        "~($conj)"
    } else {
        val exists = usedArgs.asReversed().fold(conj) { acc, arg ->
            "\\E ${arg.name} \\in ${typeToTlaDomain(arg.type)} : $acc"
        }
        "~($exists)"
    }
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
    ) = emitConjoined(
        name, offerList, allVars, stateVarsByLeaf, stateVarNames,
        sessionPairForOffers(offerList, sessionPairs), sessionPairs, killTargets, comment,
        returnToByOccurrence = returnToByOccurrence,
        blockingByHost = blockingByHost,
        hostsWithBlocking = hostsWithBlocking,
    )

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
                result += emit(name, listOf(prov, cli), comment)
            }
            return@forEach
        }

        // 1 constructor + 1 default transition → one hybrid shared action
        if (constructors.size == 1 && defaults.size == 1) {
            result += emit(actionName, listOf(defaults[0], constructors[0]))
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
            result += emit(name, listOf(offer), comment)
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
            result += emit(name, listOf(offer), comment)
        }

        when {
            defaults.size >= 2 -> {
                val (coupled, plain) = defaults.partition {
                    (it.leaf.tlaName to it.decl.action.name) in splitHostActions
                }
                coupled.forEach { offer -> emitSplitOrPlain(offer) }
                if (plain.isNotEmpty()) result += emit(actionName, plain)
            }
            defaults.size == 1 -> {
                val offer = defaults[0]
                if (!emitSplitOrPlain(offer)) result += emit(actionName, defaults)
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
                result += emit(name, listOf(prov), comment)
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
                result += emit(name, listOf(cli), comment)
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

    return TlaBuildResult(helpers, result.distinctBy { it.name })
}

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
        base + pc.localDecls().filterIsInstance<VarNode>().map {
            stateTlaName(leaf.tlaName, it.name, stateVarNames)
        }
    }

private fun emitConjoined(
    name: String,
    offers: List<TlaActionOffer>,
    allVars: List<String>,
    stateVarsByLeaf: Map<String, Set<String>>,
    stateVarNames: Map<Pair<String, String>, String>,
    sessionPair: SessionLeafPair? = null,
    allSessionPairs: List<SessionLeafPair> = emptyList(),
    killTargets: Set<String> = emptySet(),
    comment: String? = null,
    returnToByOccurrence: Map<String, String> = emptyMap(),
    blockingByHost: Map<String, String> = emptyMap(),
    hostsWithBlocking: Set<String> = emptySet(),
): TlaAction {
    val parts = mutableListOf<String>()
    val changed = mutableSetOf<String>()
    val argParams = mutableListOf<Pair<String, String>>()

    // Collect action arg names that appear in this conjoined action (for binder clash checks).
    val usedArgNames = mutableSetOf<String>()
    offers.forEach { offer ->
        fun refsArg(argName: String): Boolean {
            return offer.decl.guards.any { exprReferencesSymbol(it, argName) } ||
                offer.decl.transits.any { update ->
                    when (update) {
                        is TransitUpdate.Assign -> exprReferencesSymbol(update.expr, argName)
                        is TransitUpdate.IndexPut ->
                            exprReferencesSymbol(update.index, argName) || exprReferencesSymbol(update.value, argName)
                        is TransitUpdate.Let -> exprReferencesSymbol(update.init, argName)
                    }
                }
        }
        offer.decl.action.args.filter { refsArg(it.name) }.forEach { usedArgNames += it.name }
    }

    // Instance binders for parameterized leaves: paramName indexes into the type domain.
    val selfBinders = linkedMapOf<String, String>() // leafName -> binder
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
    offers.forEach { offer ->
        if (offer.leaf.isParameterized) {
            val binder = indexBinderName(offer.leaf, reserved)
            selfBinders[offer.leaf.tlaName] = binder
            reserved += binder
        }
    }
    pairsNeedingBinders.forEach { pair ->
        listOf(pair.leafA, pair.leafB).forEach { leaf ->
            if (leaf.isParameterized && leaf.tlaName !in selfBinders) {
                val binder = indexBinderName(leaf, reserved)
                selfBinders[leaf.tlaName] = binder
                reserved += binder
            }
        }
    }

    fun selfOf(leaf: SpecLeaf): String? = selfBinders[leaf.tlaName]

    val deferCtorSpawn = sessionPair != null && offers.any { it.isConstructor }
    val deferredSpawnParts = mutableListOf<String>()
    val deferredSpawnChanged = mutableSetOf<String>()

    fun emitTransitUpdates(offer: TlaActionOffer, targetParts: MutableList<String>, targetChanged: MutableSet<String>) {
        val self = selfOf(offer.leaf)
        val argNames = offer.decl.action.args.map { it.name }.toSet()
        val leafCtx = mapOf(offer.leaf.name to offer.leaf, offer.leaf.tlaName to offer.leaf)
        var letBindings = emptyMap<String, ExprNode>()
        fun substLets(expr: ExprNode): ExprNode {
            var result = expr
            for ((name, init) in letBindings) {
                result = substituteExpr(result, name, init)
            }
            return result
        }
        offer.decl.transits.forEach { update ->
            when (update) {
                is TransitUpdate.Let -> {
                    // Discard `_` is never substituted into later transit expressions.
                    if (!update.name.isDiscardBinding()) {
                        letBindings = letBindings + (update.name to substLets(update.init))
                    }
                }
                is TransitUpdate.Assign -> {
                    val root = update.key.substringBefore('.')
                    val v = stateTlaName(offer.leaf.tlaName, root, stateVarNames)
                    targetChanged += v
                    val expr = substLets(update.expr)
                    if (exprContainsIoHavoc(expr)) {
                        val domain = typeToTlaDomain(expr.getType())
                        targetParts += if (self != null) {
                            "/\\ \\E __io \\in $domain: $v' = [$v EXCEPT ![$self] = __io]"
                        } else {
                            "/\\ $v' \\in $domain"
                        }
                    } else {
                        val rhs = exprToTla(
                            expr, leafCtx, argNames, self,
                            bareStateVars = stateVarsByLeaf[offer.leaf.tlaName].orEmpty(),
                            stateVarNames = stateVarNames,
                        )
                        targetParts += if (self != null) {
                            "/\\ $v' = [$v EXCEPT ![$self] = $rhs]"
                        } else {
                            "/\\ $v' = $rhs"
                        }
                    }
                }
                is TransitUpdate.IndexPut -> {
                    val v = stateTlaName(offer.leaf.tlaName, update.collectionVar, stateVarNames)
                    targetChanged += v
                    val bare = stateVarsByLeaf[offer.leaf.tlaName].orEmpty()
                    val keyExpr = substLets(update.index)
                    val valueExpr = substLets(update.value)
                    val k = exprToTla(keyExpr, leafCtx, argNames, self, bareStateVars = bare, stateVarNames = stateVarNames)
                    if (exprContainsIoHavoc(valueExpr)) {
                        val domain = typeToTlaDomain(valueExpr.getType())
                        targetParts += if (self != null) {
                            "/\\ \\E __io \\in $domain: $v' = [$v EXCEPT ![$self] = [@ EXCEPT ![$k] = __io]]"
                        } else {
                            "/\\ \\E __io \\in $domain: $v' = [$v EXCEPT ![$k] = __io]"
                        }
                    } else {
                        val vv = exprToTla(valueExpr, leafCtx, argNames, self, bareStateVars = bare, stateVarNames = stateVarNames)
                        targetParts += if (self != null) {
                            "/\\ $v' = [$v EXCEPT ![$self] = [@ EXCEPT ![$k] = $vv]]"
                        } else {
                            "/\\ $v' = [$v EXCEPT ![$k] = $vv]"
                        }
                    }
                }
            }
        }
        if (offer.isConstructor) {
            val c = stateTlaName(offer.leaf.tlaName, "constructed", stateVarNames)
            targetChanged += c
            targetParts += if (self != null) {
                "/\\ $c' = [$c EXCEPT ![$self] = TRUE]"
            } else {
                "/\\ $c' = TRUE"
            }
        }
        if (offer.decl.isReturn && offer.leaf.isProcFun &&
            offer.decl.action.name == procFunRetAction(offer.leaf.name)
        ) {
            val term = stateTlaName(offer.leaf.tlaName, "terminated", stateVarNames)
            targetChanged += term
            targetParts += if (self != null) {
                "/\\ $term' = [$term EXCEPT ![$self] = TRUE]"
            } else {
                "/\\ $term' = TRUE"
            }
            val returnTo = returnToByOccurrence[offer.leaf.occurrenceId]
            if (returnTo != null) {
                targetChanged += returnTo
                targetParts += if (self != null) {
                    "/\\ $returnTo' = [$returnTo EXCEPT ![$self] = TRUE]"
                } else {
                    "/\\ $returnTo' = TRUE"
                }
            }
        }
    }

    // Participant-only constructed / killed / terminated enabling (no constraints on non-offering leaves).
    offers.forEach { offer ->
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
    }

    offers.forEach { offer ->
        val self = selfOf(offer.leaf)
        val argNames = offer.decl.action.args.map { it.name }.toSet()
        val leafCtx = mapOf(offer.leaf.name to offer.leaf, offer.leaf.tlaName to offer.leaf)

        // Only include args that appear in guards/transits (skip unused initially args).
        fun refsArg(argName: String): Boolean {
            return offer.decl.guards.any { exprReferencesSymbol(it, argName) } ||
                offer.decl.transits.any { update ->
                    when (update) {
                        is TransitUpdate.Assign -> exprReferencesSymbol(update.expr, argName)
                        is TransitUpdate.IndexPut ->
                            exprReferencesSymbol(update.index, argName) || exprReferencesSymbol(update.value, argName)
                        is TransitUpdate.Let -> exprReferencesSymbol(update.init, argName)
                    }
                }
        }
        offer.decl.action.args.filter { refsArg(it.name) }.forEach { arg ->
            argParams += arg.name to typeToTlaDomain(arg.type)
        }

        offer.decl.guards.forEach { g ->
            parts += "/\\ ${exprToTla(
                g, leafCtx, argNames, self,
                bareStateVars = stateVarsByLeaf[offer.leaf.tlaName].orEmpty(),
                stateVarNames = stateVarNames,
            )}"
        }

        // Defer constructor spawn updates into the CanStart THEN branch (throw-before-launch).
        if (deferCtorSpawn && offer.isConstructor) {
            emitTransitUpdates(offer, deferredSpawnParts, deferredSpawnChanged)
        } else {
            emitTransitUpdates(offer, parts, changed)
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
    if (unchanged.isNotEmpty()) {
        parts += "/\\ UNCHANGED <<${unchanged.joinToString(", ")}>>"
    }

    val body = parts.joinToString("\n  ")
    val indexParams = selfBinders.map { (leafName, binder) ->
        val leaf = offers.firstOrNull { it.leaf.name == leafName }?.leaf
            ?: pairsNeedingBinders.firstNotNullOfOrNull { p ->
                when (leafName) {
                    p.leafA.name -> p.leafA
                    p.leafB.name -> p.leafB
                    else -> null
                }
            }
            ?: error("missing leaf $leafName")
        val domain = typeDomainConstant(leaf.paramType!!) ?: leaf.paramType.toString()
        binder to domain
    }
    val params = (indexParams + argParams).distinctBy { it.first }
    val signature = if (params.isEmpty()) {
        name
    } else {
        "$name(${params.joinToString(", ") { it.first }})"
    }
    return TlaAction(name, "$signature ==\n  $body", params, comment)
}

/** Prefer [SpecLeaf.paramName] as the TLA binder; append `_<leaf>` on name clashes. */
internal fun indexBinderName(leaf: SpecLeaf, reserved: Set<String>): String {
    val base = leaf.paramName!!
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

internal fun defaultTlaValue(type: Type): String = when (type) {
    is BoolType -> "FALSE"
    is IntType -> "0"
    is RealType -> "0"
    is StringType -> "\"\""
    is ListType -> "<<>>"
    is SetType -> "{}"
    is MapType -> "[x \\in {} |-> 0]"
    is ObjClassType -> {
        val fields = type.fields.joinToString(", ") { f ->
            "${f.name} |-> ${defaultTlaValue(f.type)}"
        }
        "[$fields]"
    }
    else -> "0"
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
    if (multiClasses.isEmpty()) {
        val body = exprToTla(
            node.invariantFormula(),
            leafCtx = emptyMap(),
            argNames = emptySet(),
            self = null,
            bareStateVars = emptySet(),
            reservedNames = constants,
            stateVarNames = stateVarNames,
        )
        return listOf("${node.name()} == $body")
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
        val body = exprToTla(
            node.invariantFormula(),
            leafCtx = emptyMap(),
            argNames = emptySet(),
            self = null,
            bareStateVars = emptySet(),
            reservedNames = constants,
            stateVarNames = remapped,
        )
        "$invName == $body"
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

internal fun typeToTlaDomain(type: Type): String = when (type) {
    is BoolType -> "BOOLEAN"
    is IntType -> "Int"
    is RealType -> "Int"
    is StringType -> "String"
    is ListType -> "Seq(${typeToTlaDomain(type.elementType)})"
    is ObjClassType -> {
        val fields = type.fields.joinToString(", ") { f ->
            "${f.name}: ${typeToTlaDomain(f.type)}"
        }
        "[$fields]"
    }
    else -> "Int"
}

/** True if [expr] contains IO (`readln` / `readFile`) that should havoc a transit target in TLA+. */
internal fun exprContainsIoHavoc(expr: ExprNode): Boolean =
    when (expr) {
        is FunCallExprNode ->
            expr.callName() in FunBuiltinRegistry.ioHavocEffects ||
                expr.callArgs().any { exprContainsIoHavoc(it) }
        else -> expr.children.filterIsInstance<ExprNode>().any { exprContainsIoHavoc(it) }
    }

/** Collect finite TLC model names (Int, String, …) needed by action argument domains. */
internal fun collectDomainModelNames(type: Type, into: MutableSet<String>) {
    when (type) {
        is IntType, is RealType -> into += "Int"
        is StringType -> into += "String"
        is ObjClassType -> type.fields.forEach { collectDomainModelNames(it.type, into) }
        is ListType -> collectDomainModelNames(type.elementType, into)
        is SetType -> collectDomainModelNames(type.elementType, into)
        is MapType -> {
            collectDomainModelNames(type.keyType, into)
            collectDomainModelNames(type.valueType, into)
        }
        else -> {}
    }
}

private fun collectActionArgDomainModels(
    leaves: List<SpecLeaf>,
    pclasses: Map<String, ProcClassNode>,
    into: MutableSet<String>,
) {
    leaves.forEach { leaf ->
        val pc = pclasses[leaf.name] ?: return@forEach
        (pc.localDecls().flatMap { it.constructors() } + pc.localDecls().flatMap { it.transitions() })
            .forEach { action ->
                action.action.args.forEach { arg ->
                    collectDomainModelNames(arg.type, into)
                }
            }
    }
}

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

private fun tlaStringCoerce(expr: ExprNode, rendered: String): String =
    if (exprIsStringTyped(expr)) rendered else "ToString($rendered)"

private fun isEmptyStringLiteral(expr: ExprNode): Boolean =
    expr is LiteralValueExprNode &&
        expr.getType() is StringType &&
        expr.literalText().isEmpty()

/**
 * @param leafCtx map of leaf name → SpecLeaf (for FieldAccess context; optional)
 * @param argNames action argument symbols (emitted bare)
 * @param self index variable for the current parameterized leaf, or null
 * @param bareStateVars state vars that may appear unqualified in action guards/transits
 * @param reservedNames CONSTANT names; quantifier binders that clash are renamed
 * @param stateVarNames (leaf, julayVar) → TLA identifier
 */
internal fun exprToTla(
    expr: ExprNode,
    leafCtx: Map<String, SpecLeaf>,
    argNames: Set<String>,
    self: String?,
    bareStateVars: Set<String> = emptySet(),
    reservedNames: Set<String> = emptySet(),
    stateVarNames: Map<Pair<String, String>, String> = emptyMap(),
    symbolOverrides: Map<String, String> = emptyMap(),
): String {
    fun rec(e: ExprNode): String =
        exprToTla(e, leafCtx, argNames, self, bareStateVars, reservedNames, stateVarNames, symbolOverrides)
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
        is FieldAccessExprNode -> {
            val base = expr.baseSymbol
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
                if (expr.fieldPath.isEmpty()) baseRendered
                else expr.fieldPath.fold(baseRendered) { acc, field -> "$acc.$field" }
            }
        }
        is MemberAccessExprNode -> {
            val base = expr.baseExpr
            if (base is IndexExprNode && base.base is SymbolValueExprNode) {
                val leafName = (base.base as SymbolValueExprNode).symbol
                val v = stateTlaName(leafName, expr.fieldName, stateVarNames)
                "$v[${rec(base.index)}]"
            } else {
                val rendered = rec(base)
                "$rendered.${expr.fieldName}"
            }
        }
        is FieldAccessOnExprNode -> {
            val base = rec(expr.baseExpr)
            val path = expr.fieldPath
            if (path.isEmpty()) base
            else path.fold(base) { acc, field -> "$acc.$field" }
        }
        is ObjClassLiteralExprNode -> {
            val fields = expr.fieldEntries.joinToString(", ") { (name, e) ->
                "$name |-> ${rec(e)}"
            }
            "[$fields]"
        }
        is IndexExprNode -> {
            "${rec(expr.base)}[${rec(expr.index)}]"
        }
        is UnaryOpExprNode -> {
            val o = rec(expr.operand())
            when (expr.op()) {
                "~" -> "~($o)"
                "-" -> "(-$o)"
                else -> "(${expr.op()} $o)"
            }
        }
        is BinaryOpExprNode -> {
            val l = rec(expr.lhsOperand())
            val r = rec(expr.rhsOperand())
            when (expr.op()) {
                "&" -> "($l /\\ $r)"
                "|" -> "($l \\/ $r)"
                "=>" -> "($l => $r)"
                "=" -> "($l = $r)"
                "~=" -> "($l # $r)"
                "<" -> "($l < $r)"
                "<=" -> "($l <= $r)"
                ">" -> "($l > $r)"
                ">=" -> "($l >= $r)"
                "+" -> {
                    val stringy = exprIsStringTyped(expr) ||
                        exprIsStringTyped(expr.lhsOperand()) ||
                        exprIsStringTyped(expr.rhsOperand())
                    if (stringy) {
                        val lhs = expr.lhsOperand()
                        val rhs = expr.rhsOperand()
                        when {
                            isEmptyStringLiteral(lhs) && isEmptyStringLiteral(rhs) -> "\"\""
                            isEmptyStringLiteral(lhs) -> tlaStringCoerce(rhs, r)
                            isEmptyStringLiteral(rhs) -> tlaStringCoerce(lhs, l)
                            else -> "(${tlaStringCoerce(lhs, l)} \\o ${tlaStringCoerce(rhs, r)})"
                        }
                    } else {
                        "($l + $r)"
                    }
                }
                "-" -> "($l - $r)"
                "*" -> "($l * $r)"
                "/" -> "($l \\div $r)"
                else -> "($l ${expr.op()} $r)"
            }
        }
        is QuantifiedExprNode -> {
            val domain = when (val t = expr.binderTypeExpr()) {
                is TypeExpr.Simple -> t.name
                else -> t.toString()
            }
            val origBinder = expr.binderName()
            val binder = if (origBinder in reservedNames) "q_$origBinder" else origBinder
            val body = rec(expr.quantifiedBody()).let { b ->
                if (binder != origBinder) {
                    b.replace(Regex("\\b${Regex.escape(origBinder)}\\b"), binder)
                } else {
                    b
                }
            }
            val q = if (expr.isUniversal()) "\\A" else "\\E"
            "($q $binder \\in $domain : $body)"
        }
        is IfElseExprNode -> {
            "(IF ${rec(expr.condExpr())} THEN ${rec(expr.thenExpr())} ELSE ${rec(expr.elseExpr())})"
        }
        else -> "TRUE"
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
