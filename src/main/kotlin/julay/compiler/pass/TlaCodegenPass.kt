package julay.compiler.pass

import julay.compiler.CompilationUnit
import julay.compiler.TypeExpr
import julay.compiler.ast.*
import julay.compiler.decl.*
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
    val procAliases = unit.modules
        .flatMap { it.root.declNodes().filterIsInstance<ProcNode>() }
        .associateBy { it.name() }
    val specAliases = unit.modules
        .flatMap { it.root.declNodes().filterIsInstance<SpecNode>() }
        .associateBy { it.name() }

    val leaves = expandLeavesToPclasses(
        compositionLeavesOfSpec(spec),
        pclassNodes,
        procAliases,
        specAliases,
    )

    val servicedActions = leaves.flatMap { leaf ->
        val pc = pclassNodes[leaf.name] ?: return@flatMap emptyList()
        pc.localDecls().flatMap { it.transitions() }
            .filter { it.modifier == TSAction.SyncRole.Service }
            .map { it.action.name }
    }.toSet()

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
    val invNode = ag?.let { invariants[it.invariantRef()] }
    val invClosure = if (invNode != null) {
        topologicalInvariantClosure(invNode.name(), invariants)
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
    collectActionArgDomainModels(leaves, pclassNodes, cfgOverrides)
    // String is not provided by EXTENDS; declare it CONSTANT when used as a domain.
    if ("String" in cfgOverrides) {
        constants += "String"
    }

    val intModelValues = linkedSetOf(0, 1, 2, 3, 4, 5)
    collectIntLiteralsFromLeaves(leaves, pclassNodes, intModelValues)
    invClosure.forEach { collectIntLiteralsFromExpr(it.invariantFormula(), intModelValues) }

    val reservedTlaIds = constants + setOf("Int", "Nat", "Boolean", "Real") +
        actionArgNames(leaves, pclassNodes)

    val offers = collectTlaActionOffers(leaves, pclassNodes, servicedActions)
    val sessionPairs = detectTwoSidedSessionPairs(offers)
    val emittedOfferLists = collectEmittedOfferLists(offers)
    val killTargets = collectKillTargets(emittedOfferLists, sessionPairs)
    val needsSessionException = emittedOfferLists.any { offerList ->
        sessionPairForOffers(offerList, sessionPairs) != null && offerList.any { it.isConstructor }
    }
    val stateVarNames = buildStateVarNames(leaves, pclassNodes, reservedTlaIds, killTargets)

    val variables = mutableListOf<String>()
    val initParts = mutableListOf<String>()
    leaves.forEach { leaf ->
        val pc = pclassNodes[leaf.name] ?: return@forEach
        initParts += "\\* State variables for ${leaf.name}"
        val constructed = stateTlaName(leaf.name, "constructed", stateVarNames)
        val hasKilled = leaf.name in killTargets
        val killed = if (hasKilled) stateTlaName(leaf.name, "killed", stateVarNames) else null
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
            pc.localDecls().filterIsInstance<VarNode>().forEach { vn ->
                val v = stateTlaName(leaf.name, vn.name, stateVarNames)
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
            pc.localDecls().filterIsInstance<VarNode>().forEach { vn ->
                val v = stateTlaName(leaf.name, vn.name, stateVarNames)
                variables += v
                initParts += "/\\ $v = ${defaultTlaValue(safeType(vn))}"
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
        leaves, pclassNodes, offers, sessionPairs, stateVarNames,
        killTargets, needsSessionException,
    )
    val helpers = built.helpers
    val actions = built.actions

    val invDefs = if (invClosure.isNotEmpty()) {
        invClosure.map { node ->
            val body = exprToTla(
                node.invariantFormula(),
                leafCtx = emptyMap(),
                argNames = emptySet(),
                self = null,
                bareStateVars = emptySet(),
                reservedNames = constants,
                stateVarNames = stateVarNames,
            )
            "${node.name()} == $body"
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
        appendLine("====")
    }

    val cfg = buildString {
        appendLine("SPECIFICATION Spec")
        if (needsSessionException) {
            appendLine("INVARIANT SessionIntegrity")
        }
        if (invNode != null) {
            appendLine("INVARIANT ${invNode.name()}")
        }
        appendLine("CHECK_DEADLOCK FALSE")
        fun modelFor(name: String): String = when (name) {
            "Int", "Nat", "Real" -> cfgIntModel(intModelValues)
            "String" -> cfgStringModel(intModelValues)
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
        require(leafA.name <= leafB.name) { "leafA must be ordered before leafB by name" }
    }

    val varName: String get() = "session_${leafA.name}_${leafB.name}"
    val canStartName: String get() = "CanStartSession_${leafA.name}_${leafB.name}"
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
    servicedActions: Set<String>,
): List<TlaActionOffer> {
    val offers = mutableListOf<TlaActionOffer>()
    leaves.forEach { leaf ->
        val pc = pclasses[leaf.name] ?: return@forEach
        pc.localDecls().flatMap { it.constructors() }.forEach { ctor ->
            offers += TlaActionOffer(leaf, ctor, TSAction.SyncRole.Internal, isConstructor = true)
        }
        pc.localDecls().flatMap { it.transitions() }.forEach { tr ->
            val role = when {
                tr.modifier == TSAction.SyncRole.Default && tr.action.name in servicedActions ->
                    TSAction.SyncRole.Consumer
                else -> tr.modifier
            }
            offers += TlaActionOffer(leaf, tr, role, isConstructor = false)
        }
    }
    return offers
}

/** Session pairs where both peers appear as SpecLeaves (one-sided → empty). */
internal fun detectTwoSidedSessionPairs(offers: List<TlaActionOffer>): List<SessionLeafPair> {
    val leafByName = offers.map { it.leaf }.associateBy { it.name }
    val pairKeys = linkedSetOf<Pair<String, String>>()
    offers.groupBy { it.decl.action.name }.forEach { (_, group) ->
        val sessionOffers = group.filter { it.decl.isSession }
        if (sessionOffers.size != 2) return@forEach
        val names = sessionOffers.map { it.leaf.name }.toSet()
        if (names.size != 2) return@forEach
        val sorted = names.sorted()
        pairKeys += sorted[0] to sorted[1]
    }
    return pairKeys.mapNotNull { (a, b) ->
        val la = leafByName[a] ?: return@mapNotNull null
        val lb = leafByName[b] ?: return@mapNotNull null
        SessionLeafPair(la, lb)
    }
}

private fun sessionPairForOffers(
    offerList: List<TlaActionOffer>,
    pairs: List<SessionLeafPair>,
): SessionLeafPair? {
    if (offerList.size != 2) return null
    if (!offerList.all { it.decl.isSession }) return null
    val names = offerList.map { it.leaf.name }.sorted()
    return pairs.find { it.leafA.name == names[0] && it.leafB.name == names[1] }
}

/**
 * Offer lists that [buildTlaActions] would pass to [emitConjoined], in the same grouping order.
 * Used to precompute kill targets and whether sessionException is needed.
 */
private fun collectEmittedOfferLists(offers: List<TlaActionOffer>): List<List<TlaActionOffer>> {
    val result = mutableListOf<List<TlaActionOffer>>()
    offers.groupBy { it.decl.action.name }.forEach { (_, group) ->
        val services = group.filter { it.role == TSAction.SyncRole.Service }
        val consumers = group.filter { it.role == TSAction.SyncRole.Consumer }
        val constructors = group.filter { it.isConstructor }
        val internals = group.filter { it.role == TSAction.SyncRole.Internal && !it.isConstructor }
        val defaults = group.filter { it.role == TSAction.SyncRole.Default && !it.isConstructor }

        if (services.size == 1 && consumers.isNotEmpty()) {
            consumers.forEach { cons -> result += listOf(services[0], cons) }
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
        if (services.isNotEmpty() && consumers.isEmpty()) {
            services.forEach { result += listOf(it) }
        }
        if (consumers.isNotEmpty() && services.isEmpty()) {
            consumers.forEach { result += listOf(it) }
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
        targets += peerLeafOf(effectPair, caller).name
    }
    return targets
}

private fun sessionEffectNames(offer: TlaActionOffer): List<String> =
    offer.decl.effects.map { it.callName() }.filter {
        it == "exitSession" || it == "killSessionPeer"
    }

/**
 * Resolve the session pair targeted by exitSession/killSessionPeer on [offers].
 * - exitSession: action session pair, else unique pair involving an offering leaf.
 * - killSessionPeer: prefer a session pair involving the caller that is *not* the
 *   action's sync pair (e.g. Timer→Helper while cancelTimer syncs with Client);
 *   else the action pair / unique pair involving the caller.
 */
private fun resolveSessionEffectPair(
    offers: List<TlaActionOffer>,
    actionSessionPair: SessionLeafPair?,
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
    if (hasExit) {
        if (actionSessionPair != null) return actionSessionPair
        val leafNames = offers.map { it.leaf.name }.toSet()
        val matching = allPairs.filter { pair ->
            pair.leafA.name in leafNames || pair.leafB.name in leafNames
        }
        return when (matching.size) {
            1 -> matching.single()
            0 -> throw RuntimeException(
                "TLA+: exitSession on \"${offers.first().decl.action.name}\" " +
                    "has no two-sided session pair among SpecLeaves",
            )
            else -> throw RuntimeException(
                "TLA+: exitSession on \"${offers.first().decl.action.name}\" " +
                    "is ambiguous across ${matching.map { it.varName }}",
            )
        }
    }
    // killSessionPeer
    val caller = sessionEffectCaller(offers, "killSessionPeer")
        ?: throw RuntimeException("TLA+: killSessionPeer missing caller")
    val involving = allPairs.filter {
        it.leafA.name == caller.name || it.leafB.name == caller.name
    }
    val nonSync = involving.filter { it.varName != actionSessionPair?.varName }
    return when {
        nonSync.size == 1 -> nonSync.single()
        involving.size == 1 -> involving.single()
        actionSessionPair != null && involving.any { it.varName == actionSessionPair.varName } &&
            nonSync.isEmpty() -> actionSessionPair
        involving.isEmpty() -> throw RuntimeException(
            "TLA+: killSessionPeer on \"${offers.first().decl.action.name}\" " +
                "has no session pair involving ${caller.name}",
        )
        else -> throw RuntimeException(
            "TLA+: killSessionPeer on \"${offers.first().decl.action.name}\" " +
                "is ambiguous across ${involving.map { it.varName }}",
        )
    }
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
    val k = stateTlaName(leaf.name, "killed", stateVarNames)
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

private fun deadOperatorName(leaf: SpecLeaf): String = "${leaf.name}_dead"

private fun negateLocalGuards(
    offer: TlaActionOffer,
    self: String?,
    stateVarsByLeaf: Map<String, Set<String>>,
    stateVarNames: Map<Pair<String, String>, String>,
): String {
    val guards = offer.decl.guards
    if (guards.isEmpty()) return "FALSE"
    val argNames = offer.decl.action.args.map { it.name }.toSet()
    val leafCtx = mapOf(offer.leaf.name to offer.leaf)
    val bare = stateVarsByLeaf[offer.leaf.name].orEmpty()
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
    val c = stateTlaName(leaf.name, "constructed", stateVarNames)
    val self = if (leaf.isParameterized) {
        indexBinderName(leaf, stateVarsByLeaf[leaf.name].orEmpty())
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
    return if (leaf.name in killTargets) {
        val killed = stateTlaName(leaf.name, "killed", stateVarNames)
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
    val count = allPairs.count { it.leafA.name == leaf.name || it.leafB.name == leaf.name }
    return if (count == 1) {
        "EndSession_${leaf.name}"
    } else {
        "EndSession_${pair.leafA.name}_${pair.leafB.name}_${leaf.name}"
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
        indexBinderName(pair.leafA, stateVarsByLeaf[pair.leafA.name].orEmpty())
    } else null
    val binderB = if (pair.leafB.isParameterized) {
        val reserved = stateVarsByLeaf[pair.leafB.name].orEmpty().toMutableSet()
        binderA?.let { reserved += it }
        indexBinderName(pair.leafB, reserved)
    } else null
    val exitingBinder = when (exiting.name) {
        pair.leafA.name -> binderA
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
): TlaBuildResult {
    val allVars = allTlaVars(leaves, pclasses, stateVarNames, killTargets) +
        sessionPairs.map { it.varName } +
        if (needsSessionException) listOf("sessionException") else emptyList()
    val stateVarsByLeaf = leaves.associate { leaf ->
        leaf.name to (
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
    ) = emitConjoined(
        name, offerList, allVars, stateVarsByLeaf, stateVarNames,
        sessionPairForOffers(offerList, sessionPairs), sessionPairs, killTargets, comment,
    )

    byName.forEach { (actionName, group) ->
        val services = group.filter { it.role == TSAction.SyncRole.Service }
        val consumers = group.filter { it.role == TSAction.SyncRole.Consumer }
        val constructors = group.filter { it.isConstructor }
        val internals = group.filter { it.role == TSAction.SyncRole.Internal && !it.isConstructor }
        val defaults = group.filter { it.role == TSAction.SyncRole.Default && !it.isConstructor }

        if (services.size == 1 && consumers.isNotEmpty()) {
            val svc = services[0]
            val needDisambiguate = consumers.size > 1
            consumers.forEach { cons ->
                val name: String
                val comment: String?
                if (needDisambiguate) {
                    name = "${actionName}_${svc.leaf.name}_${cons.leaf.name}"
                    comment =
                        "$actionName action where ${svc.leaf.name} is the servicer and ${cons.leaf.name} is the consumer"
                } else {
                    name = actionName
                    comment = null
                }
                result += emit(name, listOf(svc, cons), comment)
            }
            return@forEach
        }

        // 1 constructor + 1 default transition → one hybrid shared action
        if (constructors.size == 1 && defaults.size == 1) {
            result += emit(actionName, listOf(defaults[0], constructors[0]))
            return@forEach
        }

        // Solo constructors (any name, including initially) — valid leaf entry
        val disambiguateCtors = constructors.size > 1
        constructors.forEach { offer ->
            val name: String
            val comment: String?
            if (disambiguateCtors) {
                name = if (offer.decl.action.name == "initially") {
                    "${offer.leaf.name}_initially"
                } else {
                    "${actionName}_${offer.leaf.name}"
                }
                comment = if (offer.decl.action.name == "initially") {
                    "initially constructor on ${offer.leaf.name}"
                } else {
                    "$actionName action on ${offer.leaf.name}"
                }
            } else {
                name = actionName
                comment = null
            }
            result += emit(name, listOf(offer), comment)
        }

        val disambiguateInternals = internals.size > 1
        internals.forEach { offer ->
            val name: String
            val comment: String?
            if (disambiguateInternals) {
                name = "${actionName}_${offer.leaf.name}"
                comment = "$actionName action on ${offer.leaf.name}"
            } else {
                name = actionName
                comment = null
            }
            result += emit(name, listOf(offer), comment)
        }

        when {
            defaults.size >= 2 -> result += emit(actionName, defaults)
            defaults.size == 1 -> result += emit(actionName, defaults)
        }

        if (services.isNotEmpty() && consumers.isEmpty()) {
            val disambiguate = services.size > 1
            services.forEach { svc ->
                val name: String
                val comment: String?
                if (disambiguate) {
                    name = "${actionName}_${svc.leaf.name}"
                    comment = "$actionName action on ${svc.leaf.name}"
                } else {
                    name = actionName
                    comment = null
                }
                result += emit(name, listOf(svc), comment)
            }
        }
        if (consumers.isNotEmpty() && services.isEmpty()) {
            val disambiguate = consumers.size > 1
            consumers.forEach { cons ->
                val name: String
                val comment: String?
                if (disambiguate) {
                    name = "${actionName}_${cons.leaf.name}"
                    comment = "$actionName action on ${cons.leaf.name}"
                } else {
                    name = actionName
                    comment = null
                }
                result += emit(name, listOf(cons), comment)
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
        val leafOffers = offers.filter { it.leaf.name == leaf.name }
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
        val base = mutableListOf(stateTlaName(leaf.name, "constructed", stateVarNames))
        if (leaf.name in killTargets) {
            base += stateTlaName(leaf.name, "killed", stateVarNames)
        }
        base + pc.localDecls().filterIsInstance<VarNode>().map {
            stateTlaName(leaf.name, it.name, stateVarNames)
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
                        is TransitUpdate.MapPut ->
                            exprReferencesSymbol(update.key, argName) || exprReferencesSymbol(update.value, argName)
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
            reserved += stateVarsByLeaf[offer.leaf.name].orEmpty()
        }
    }
    // Session pair may need binders for both leaves even when indexing session after updates.
    val effectSessionPair = resolveSessionEffectPair(offers, sessionPair, allSessionPairs)
    val pairsNeedingBinders = listOfNotNull(sessionPair, effectSessionPair).distinctBy { it.varName }
    pairsNeedingBinders.forEach { pair ->
        listOf(pair.leafA, pair.leafB).forEach { leaf ->
            if (leaf.isParameterized) {
                reserved += stateVarsByLeaf[leaf.name].orEmpty()
            }
        }
    }
    offers.forEach { offer ->
        if (offer.leaf.isParameterized) {
            val binder = indexBinderName(offer.leaf, reserved)
            selfBinders[offer.leaf.name] = binder
            reserved += binder
        }
    }
    pairsNeedingBinders.forEach { pair ->
        listOf(pair.leafA, pair.leafB).forEach { leaf ->
            if (leaf.isParameterized && leaf.name !in selfBinders) {
                val binder = indexBinderName(leaf, reserved)
                selfBinders[leaf.name] = binder
                reserved += binder
            }
        }
    }

    fun selfOf(leaf: SpecLeaf): String? = selfBinders[leaf.name]

    val deferCtorSpawn = sessionPair != null && offers.any { it.isConstructor }
    val deferredSpawnParts = mutableListOf<String>()
    val deferredSpawnChanged = mutableSetOf<String>()

    fun emitTransitUpdates(offer: TlaActionOffer, targetParts: MutableList<String>, targetChanged: MutableSet<String>) {
        val self = selfOf(offer.leaf)
        val argNames = offer.decl.action.args.map { it.name }.toSet()
        val leafCtx = mapOf(offer.leaf.name to offer.leaf)
        offer.decl.transits.forEach { update ->
            when (update) {
                is TransitUpdate.Assign -> {
                    val root = update.key.substringBefore('.')
                    val v = stateTlaName(offer.leaf.name, root, stateVarNames)
                    targetChanged += v
                    val rhs = exprToTla(
                        update.expr, leafCtx, argNames, self,
                        bareStateVars = stateVarsByLeaf[offer.leaf.name].orEmpty(),
                        stateVarNames = stateVarNames,
                    )
                    targetParts += if (self != null) {
                        "/\\ $v' = [$v EXCEPT ![$self] = $rhs]"
                    } else {
                        "/\\ $v' = $rhs"
                    }
                }
                is TransitUpdate.MapPut -> {
                    val v = stateTlaName(offer.leaf.name, update.mapVar, stateVarNames)
                    targetChanged += v
                    val bare = stateVarsByLeaf[offer.leaf.name].orEmpty()
                    val k = exprToTla(update.key, leafCtx, argNames, self, bareStateVars = bare, stateVarNames = stateVarNames)
                    val vv = exprToTla(update.value, leafCtx, argNames, self, bareStateVars = bare, stateVarNames = stateVarNames)
                    targetParts += if (self != null) {
                        "/\\ $v' = [$v EXCEPT ![$self] = [@ EXCEPT ![$k] = $vv]]"
                    } else {
                        "/\\ $v' = [$v EXCEPT ![$k] = $vv]"
                    }
                }
            }
        }
        if (offer.isConstructor) {
            val c = stateTlaName(offer.leaf.name, "constructed", stateVarNames)
            targetChanged += c
            targetParts += if (self != null) {
                "/\\ $c' = [$c EXCEPT ![$self] = TRUE]"
            } else {
                "/\\ $c' = TRUE"
            }
        }
    }

    // Participant-only constructed / killed enabling (no constraints on non-offering leaves).
    offers.forEach { offer ->
        val c = stateTlaName(offer.leaf.name, "constructed", stateVarNames)
        val self = selfOf(offer.leaf)
        if (offer.isConstructor) {
            parts += if (self != null) "/\\ ~$c[$self]" else "/\\ ~$c"
        } else {
            parts += if (self != null) "/\\ $c[$self]" else "/\\ $c"
        }
        if (offer.leaf.name in killTargets) {
            val killed = stateTlaName(offer.leaf.name, "killed", stateVarNames)
            parts += if (self != null) "/\\ ~$killed[$self]" else "/\\ ~$killed"
        }
    }

    offers.forEach { offer ->
        val self = selfOf(offer.leaf)
        val argNames = offer.decl.action.args.map { it.name }.toSet()
        val leafCtx = mapOf(offer.leaf.name to offer.leaf)

        // Only include args that appear in guards/transits (skip unused initially args).
        fun refsArg(argName: String): Boolean {
            return offer.decl.guards.any { exprReferencesSymbol(it, argName) } ||
                offer.decl.transits.any { update ->
                    when (update) {
                        is TransitUpdate.Assign -> exprReferencesSymbol(update.expr, argName)
                        is TransitUpdate.MapPut ->
                            exprReferencesSymbol(update.key, argName) || exprReferencesSymbol(update.value, argName)
                    }
                }
        }
        offer.decl.action.args.filter { refsArg(it.name) }.forEach { arg ->
            argParams += arg.name to typeToTlaDomain(arg.type)
        }

        offer.decl.guards.forEach { g ->
            parts += "/\\ ${exprToTla(
                g, leafCtx, argNames, self,
                bareStateVars = stateVarsByLeaf[offer.leaf.name].orEmpty(),
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
        val binderA = selfBinders[sessionPair.leafA.name]
        val binderB = selfBinders[sessionPair.leafB.name]
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
        val binderA = selfBinders[effectSessionPair.leafA.name]
        val binderB = selfBinders[effectSessionPair.leafB.name]
        val lookup = sessionLookup(effectSessionPair, binderA, binderB)
        parts += "\\* Session connection semantics"
        parts += "/\\ $lookup"
        parts += "/\\ ${sessionAssignFalseExpr(effectSessionPair, binderA, binderB)}"
        changed += effectSessionPair.varName
        if (hasKill) {
            val caller = sessionEffectCaller(offers, "killSessionPeer")
                ?: throw RuntimeException("TLA+: killSessionPeer missing caller leaf")
            val peer = peerLeafOf(effectSessionPair, caller)
            val peerBinder = selfBinders[peer.name]
            parts += "/\\ ${killedAssignTrueExpr(peer, peerBinder, stateVarNames)}"
            changed += stateTlaName(peer.name, "killed", stateVarNames)
        }
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
    return if (base !in reserved) base else "${base}_${leaf.name}"
}

/**
 * Map (leafName, julayVarName) → TLA identifier.
 * `constructed` is always `Leaf_constructed`; `killed` only for [killTargets];
 * other vars are bare unless duplicated or reserved.
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
            ownersByVar.getOrPut(vn.name) { mutableListOf() }.add(leaf.name)
        }
    }
    val out = linkedMapOf<Pair<String, String>, String>()
    leaves.forEach { leaf ->
        out[leaf.name to "constructed"] = "${leaf.name}_constructed"
        if (leaf.name in killTargets) {
            out[leaf.name to "killed"] = "${leaf.name}_killed"
        }
        val pc = pclasses[leaf.name] ?: return@forEach
        pc.localDecls().filterIsInstance<VarNode>().forEach { vn ->
            val clash = (ownersByVar[vn.name]?.size ?: 0) > 1 || vn.name in reservedIds
            out[leaf.name to vn.name] = if (clash) "${leaf.name}_${vn.name}" else vn.name
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
                        is TransitUpdate.MapPut -> {
                            collectIntLiteralsFromExpr(update.key, into)
                            collectIntLiteralsFromExpr(update.value, into)
                        }
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
): String {
    fun rec(e: ExprNode): String =
        exprToTla(e, leafCtx, argNames, self, bareStateVars, reservedNames, stateVarNames)
    return when (expr) {
        is LiteralValueExprNode -> literalToTla(expr)
        is SymbolValueExprNode -> {
            val sym = expr.symbol
            when {
                sym in argNames -> sym
                sym in bareStateVars && leafCtx.size == 1 -> {
                    val leaf = leafCtx.values.first()
                    val v = stateTlaName(leaf.name, sym, stateVarNames)
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
                        val v = stateTlaName(leaf.name, base, stateVarNames)
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
                        "(${tlaStringCoerce(expr.lhsOperand(), l)} \\o ${tlaStringCoerce(expr.rhsOperand(), r)})"
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
