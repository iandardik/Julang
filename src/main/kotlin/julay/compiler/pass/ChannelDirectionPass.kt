package julay.compiler.pass

import julay.compiler.CompileError
import julay.compiler.OneLocCompileError
import julay.compiler.ProgramLoc
import julay.compiler.ast.BinaryOpExprNode
import julay.compiler.ast.CallStmtNode
import julay.compiler.ast.ErrorArmNode
import julay.compiler.ast.ExprNode
import julay.compiler.ast.ParenExprNode
import julay.compiler.ast.RootNode
import julay.compiler.ast.SymbolValueExprNode
import julay.compiler.ast.exprReferencesSymbol
import julay.compiler.ast.substituteExpr
import julay.compiler.decl.ActionDecl
import julay.compiler.decl.ProcClassDecl
import julay.compiler.decl.ProcDecl
import julay.compiler.decl.TransitUpdate
import java.util.IdentityHashMap

/**
 * Deduced Go channel direction for one action offer pair (or a local single-side action).
 *
 * [senderBoundArgs] are arguments the sender computes and places in the message.
 * [replyArgs] are arguments the receiver computes that the sender's body or residual
 * guard reads (reply channel; residual uses are reply commit filters on pure queries).
 */
data class ActionChannelDirection(
    val actionName: String,
    val senderClass: String,
    val receiverClass: String?,
    val senderBoundArgs: List<String>,
    val replyArgs: List<String>,
    val isLocal: Boolean,
    /** Composition alphabet key: action name for services, `$scopeId#$name` when composition-hidden. */
    val channelKey: String = actionName,
)

data class ChannelDirectionResult(
    val directions: List<ActionChannelDirection>,
    val errors: List<CompileError>,
)

/**
 * Deduce sender/receiver per action from guard binding analysis.
 *
 * [program] is the compose / JAR target when available; when null, every leaf offer in [procs]
 * is considered. [procDecls] expands named assemblies under [program].
 */
fun computeChannelDirections(
    ast: RootNode,
    procs: Set<String>,
    program: ProcDecl?,
    librariesInUse: Set<String> = emptySet(),
    procDecls: List<ProcDecl> = emptyList(),
): ChannelDirectionResult {
    val leafMap = leafActionMap(ast, procs, librariesInUse)
    val alphabet = if (program != null) {
        computeCompositionAlphabet(program, procDecls, leafMap)
    } else {
        val all = leafMap.values.flatten()
        CompositionAlphabetResult(
            external = all.filter { !it.isLocal && !it.compositionHidden },
            allOffers = all,
            channelKeys = all.associate { it.leafId to it.channelKey },
            leafOccurrences = emptyList(),
            errors = emptyList(),
        )
    }

    val pclasses = ast.declNodes()
        .flatMap { it.procClassPass(procs) }
        .associateBy { it.name }

    val directions = mutableListOf<ActionChannelDirection>()
    val errors = mutableListOf<CompileError>()

    // Group by channelKey so composition-hidden ordinary pairs get separate directions;
    // service hubs share the public action-name key with their clients.
    alphabet.allOffers
        .groupBy { it.channelKey }
        .forEach { (_, offers) ->
            val result = deduceChannelGroup(offers, pclasses)
            directions += result.directions
            errors += result.errors
        }

    return ChannelDirectionResult(directions, errors)
}

private data class Participant(
    val className: String,
    val occurrenceId: String,
    val isStatic: Boolean,
    val isService: Boolean,
    val isClient: Boolean,
    val decl: ActionDecl,
    val isConstructor: Boolean,
    val loc: ProgramLoc,
    val parentClass: String? = null,
)

private fun deduceChannelGroup(
    offers: List<AlphabetOffer>,
    pclasses: Map<String, ProcClassDecl>,
): ChannelDirectionResult {
    val participants = offers.mapNotNull { offer ->
        val pc = pclasses[offer.pclassKey] ?: return@mapNotNull null
        val decl = lookupActionDecl(pc, offer.name, offer.isConstructor) ?: return@mapNotNull null
        Participant(
            className = offer.pclassKey,
            occurrenceId = offer.occurrenceId.ifEmpty { offer.pclassKey },
            isStatic = offer.isStatic || pc.isStatic,
            isService = offer.isService,
            isClient = offer.isClient,
            decl = decl,
            isConstructor = offer.isConstructor,
            loc = offer.loc,
            parentClass = offer.parentClass,
        )
    }
    if (participants.isEmpty()) {
        return ChannelDirectionResult(emptyList(), emptyList())
    }

    // Constructor peers are spawned by the firing side, never waiting on a channel.
    if (participants.any { it.isConstructor } && participants.any { !it.isConstructor }) {
        return deduceChannelGroup(offers.filter { !it.isConstructor }, pclasses)
    }

    val actionName = offers.first().name
    val channelKey = offers.first().channelKey
    // Locals: internal/timer, or a constructor-only channel, or a single participant class.
    val localLike = offers.all { it.isLocal || it.isConstructor } ||
        participants.map { it.className }.toSet().size == 1

    if (localLike) {
        return deduceLocalGroup(actionName, channelKey, participants)
    }

    val byClass = participants.groupBy { it.className }.mapValues { (_, ps) -> ps.first() }
    val classes = byClass.values.toList()
    if (classes.size < 2) {
        return deduceLocalGroup(actionName, channelKey, participants)
    }

    val services = classes.filter { it.isService }
    val clients = classes.filter { it.isClient }
    if (services.size == 1 && clients.isNotEmpty()) {
        return deduceHub(actionName, channelKey, services.single(), clients)
    }

    val statics = classes.filter { it.isStatic }
    val dynamics = classes.filter { !it.isStatic }

    return when {
        statics.size == 1 && dynamics.isNotEmpty() ->
            deduceHub(actionName, channelKey, statics.single(), dynamics)
        classes.size == 2 ->
            deducePair(actionName, channelKey, classes[0], classes[1])
        else ->
            ChannelDirectionResult(
                emptyList(),
                listOf(
                    OneLocCompileError(
                        classes.first().loc,
                        "Cannot determine channel direction for action \"$actionName\": " +
                            "expected a pairwise sync, a service hub with clients, " +
                            "or a static hub with dynamic peers",
                    ),
                ),
            )
    }
}

private fun deduceLocalGroup(
    actionName: String,
    channelKey: String,
    participants: List<Participant>,
): ChannelDirectionResult {
    val directions = mutableListOf<ActionChannelDirection>()
    val errors = mutableListOf<CompileError>()
    // One local deduction per class (constructors / internals are per-proc).
    participants.groupBy { it.className }.forEach { (_, ps) ->
        val p = ps.first()
        val argNames = p.decl.action.args.map { it.name }
        if (p.isConstructor) {
            // Constructor arguments are supplied by the caller (main / spawner), not bound in-guard.
            directions += ActionChannelDirection(
                actionName = actionName,
                senderClass = p.className,
                receiverClass = null,
                senderBoundArgs = argNames,
                replyArgs = emptyList(),
                isLocal = true,
                channelKey = channelKey,
            )
            return@forEach
        }
        val bound = computeBoundArgs(p.decl, availableFromOther = emptySet())
        val missing = argNames.filter { it !in bound.boundArgs }
        if (missing.isNotEmpty()) {
            errors += OneLocCompileError(
                p.loc,
                "Local action \"$actionName\" on \"${p.className}\" does not bind argument \"${missing.first()}\"",
            )
        } else {
            directions += ActionChannelDirection(
                actionName = actionName,
                senderClass = p.className,
                receiverClass = null,
                senderBoundArgs = argNames,
                replyArgs = emptyList(),
                isLocal = true,
                channelKey = channelKey,
            )
        }
    }
    return ChannelDirectionResult(directions, errors)
}

private fun deducePair(
    actionName: String,
    channelKey: String,
    a: Participant,
    b: Participant,
): ChannelDirectionResult {
    val ab = tryDirection(actionName, sender = a, receiver = b, channelKey = channelKey)
    val ba = tryDirection(actionName, sender = b, receiver = a, channelKey = channelKey)
    return pickDirection(actionName, a, b, ab, ba)
}

private fun deduceHub(
    actionName: String,
    channelKey: String,
    hub: Participant,
    peers: List<Participant>,
): ChannelDirectionResult {
    val hubAsSender = peers.map { peer ->
        tryDirection(actionName, sender = hub, receiver = peer, channelKey = channelKey)
    }
    val hubAsReceiver = peers.map { peer ->
        tryDirection(actionName, sender = peer, receiver = hub, channelKey = channelKey)
    }
    val senderOk = hubAsSender.all { it is DirectionOk }
    val receiverOk = hubAsReceiver.all { it is DirectionOk }

    return when {
        senderOk && receiverOk -> {
            // Tie: dynamic sends → hub is receiver.
            ChannelDirectionResult(hubAsReceiver.map { (it as DirectionOk).direction }, emptyList())
        }
        senderOk -> ChannelDirectionResult(hubAsSender.map { (it as DirectionOk).direction }, emptyList())
        receiverOk -> ChannelDirectionResult(hubAsReceiver.map { (it as DirectionOk).direction }, emptyList())
        else -> {
            // Prefer peer→hub failures (reply-filter / unbound on the client) over hub→peer
            // “unbound” mis-reports when the hub cannot bind request args alone.
            val sample = (hubAsReceiver + hubAsSender).filterIsInstance<DirectionFail>().firstOrNull()
            ChannelDirectionResult(
                emptyList(),
                listOf(
                    sample?.error ?: OneLocCompileError(
                        hub.loc,
                        "Cannot determine channel direction for hub action \"$actionName\": " +
                            "static class \"${hub.className}\" is not uniformly sender or receiver for all peers",
                    ),
                ),
            )
        }
    }
}

private fun pickDirection(
    actionName: String,
    a: Participant,
    b: Participant,
    ab: DirectionTry,
    ba: DirectionTry,
): ChannelDirectionResult {
    val abOk = ab as? DirectionOk
    val baOk = ba as? DirectionOk
    return when {
        abOk != null && baOk != null -> {
            val chosen = breakTie(a, b, abOk, baOk)
            ChannelDirectionResult(listOf(chosen.direction), emptyList())
        }
        abOk != null -> ChannelDirectionResult(listOf(abOk.direction), emptyList())
        baOk != null -> ChannelDirectionResult(listOf(baOk.direction), emptyList())
        else -> {
            val err = (ab as? DirectionFail)?.error
                ?: (ba as? DirectionFail)?.error
                ?: OneLocCompileError(
                    a.loc,
                    "Cannot determine channel direction for action \"$actionName\": neither direction is valid",
                )
            ChannelDirectionResult(emptyList(), listOf(err))
        }
    }
}

private fun breakTie(
    a: Participant,
    b: Participant,
    ab: DirectionOk,
    ba: DirectionOk,
): DirectionOk {
    val aStatic = a.isStatic
    val bStatic = b.isStatic
    return when {
        // Child sends, so one child class keeps one direction under every parent it is bound to.
        a.parentClass == b.className -> ab
        b.parentClass == a.className -> ba
        aStatic && !bStatic -> ba // dynamic sends
        !aStatic && bStatic -> ab
        else -> {
            val abCount = ab.direction.senderBoundArgs.size
            val baCount = ba.direction.senderBoundArgs.size
            when {
                abCount > baCount -> ab
                baCount > abCount -> ba
                a.className <= b.className -> ab
                else -> ba
            }
        }
    }
}

sealed class DirectionTry
data class DirectionOk(val direction: ActionChannelDirection) : DirectionTry()
data class DirectionFail(val error: CompileError) : DirectionTry()

/** Check whether [sender] → [receiver] is a valid directed exchange for [actionName]. */
fun tryDirection(
    actionName: String,
    sender: ParticipantView,
    receiver: ParticipantView,
    channelKey: String = actionName,
): DirectionTry {
    val argNames = sender.decl.action.args.map { it.name }
    // Prefer sender's arg list; require matching names with receiver.
    val sBound = computeBoundArgs(sender.decl, availableFromOther = emptySet())
    val rBound = computeBoundArgs(receiver.decl, availableFromOther = sBound.boundArgs)

    val senderOwned = argNames.filter { it !in rBound.boundArgs }.toSet()
    val missing = senderOwned.filter { it !in sBound.boundArgs }
    if (missing.isNotEmpty()) {
        return DirectionFail(
            OneLocCompileError(
                sender.loc,
                "Cannot determine channel direction for action \"$actionName\": " +
                    "argument \"${missing.first()}\" is unbound " +
                    "(neither \"${sender.className}\" nor \"${receiver.className}\" binds it)",
            ),
        )
    }

    // Receiver may filter on args it binds; it must not filter on received (sender-owned) args.
    val recvFilter = guardRefsArgs(
        receiver.decl,
        skip = rBound.bindingConjuncts,
        args = senderOwned,
    )
    if (recvFilter != null) {
        return DirectionFail(
            OneLocCompileError(
                receiver.loc,
                "Cannot determine channel direction for action \"$actionName\": " +
                    "receiver \"${receiver.className}\" guard filters on argument \"$recvFilter\"",
            ),
        )
    }

    val receiverOnly = rBound.boundArgs - sBound.boundArgs
    val senderReplyFilter = guardRefsArgs(
        sender.decl,
        skip = sBound.bindingConjuncts,
        args = receiverOnly,
    )
    if (senderReplyFilter != null && !isPureQueryAction(receiver.decl)) {
        return DirectionFail(
            OneLocCompileError(
                sender.loc,
                "Cannot determine channel direction for action \"$actionName\": " +
                    "sender \"${sender.className}\" guard references reply argument \"$senderReplyFilter\", " +
                    "but receiver \"${receiver.className}\" is not a pure query " +
                    "(has transit, before, or after)",
            ),
        )
    }

    // Reply channel carries receiver-only args the sender body or residual guard reads.
    val replyArgs = receiverOnly.filter { arg ->
        actionBodyReferencesArg(sender.decl, arg) ||
            actionGuardReferencesArg(sender.decl, arg, skip = sBound.bindingConjuncts)
    }.sorted()
    return DirectionOk(
        ActionChannelDirection(
            actionName = actionName,
            senderClass = sender.className,
            receiverClass = receiver.className,
            senderBoundArgs = senderOwned.toList(),
            replyArgs = replyArgs,
            isLocal = false,
            channelKey = channelKey,
        ),
    )
}

/** True when the action has no fire-path effects (guard-only reply computation). */
fun isPureQueryAction(decl: ActionDecl): Boolean =
    decl.transits.isEmpty() && decl.befores.isEmpty() && decl.afters.isEmpty()

/** Lightweight view used by [tryDirection]. */
data class ParticipantView(
    val className: String,
    val isStatic: Boolean,
    val decl: ActionDecl,
    val loc: ProgramLoc,
)

private fun tryDirection(
    actionName: String,
    sender: Participant,
    receiver: Participant,
    channelKey: String = actionName,
): DirectionTry = tryDirection(
    actionName,
    ParticipantView(sender.className, sender.isStatic, sender.decl, sender.loc),
    ParticipantView(receiver.className, receiver.isStatic, receiver.decl, receiver.loc),
    channelKey = channelKey,
)

data class BoundArgAnalysis(
    val boundArgs: Set<String>,
    /** Guard conjunct exprs consumed as binding equations (equality / membership). */
    val bindingConjuncts: IdentityHashMap<ExprNode, Boolean>,
    /** Args bound by equality (vs membership). */
    val equalityBound: Set<String>,
    val membershipBound: Set<String>,
)

/**
 * Arguments [decl] can bind from its guard, given [availableFromOther] as already-known args
 * (e.g. received message fields when analyzing a receiver).
 *
 * Equality (`=` / `<=>`) wins over membership (`in`) when both are present.
 */
fun computeBoundArgs(
    decl: ActionDecl,
    availableFromOther: Set<String> = emptySet(),
): BoundArgAnalysis {
    val argNames = decl.action.args.map { it.name }.toSet()
    if (argNames.isEmpty()) {
        return BoundArgAnalysis(emptySet(), IdentityHashMap(), emptySet(), emptySet())
    }
    val conjuncts = decl.guards.flatMap { collectGuardConjuncts(it) }
    val equalityBound = linkedSetOf<String>()
    val membershipBound = linkedSetOf<String>()
    val bindingConjuncts = IdentityHashMap<ExprNode, Boolean>()
    val conflicted = mutableSetOf<String>()

    fun currentlyBound(): Set<String> = equalityBound + membershipBound + availableFromOther

    var steps = 0
    var changed = true
    while (changed && steps++ < 64) {
        changed = false
        // Pass 1: equality / iff.
        for (c in conjuncts) {
            if (bindingConjuncts.containsKey(c.expr)) continue
            val det = matchEqualityBind(c.expr, argNames, currentlyBound(), conflicted) ?: continue
            val (arg, raw) = det
            val rhs = substLetEnvLocal(raw, c.letEnv)
            if (!rhsOnlyUsesAllowed(rhs, argNames, currentlyBound() - setOf(arg) + availableFromOther)) {
                continue
            }
            if (arg in conflicted) continue
            if (arg in membershipBound) {
                // Equality wins: upgrade membership to equality; keep membership conjunct as a check.
                membershipBound -= arg
            }
            if (arg !in equalityBound) {
                equalityBound += arg
                changed = true
            }
            bindingConjuncts[c.expr] = true
        }
        // Pass 2: membership for still-unbound args.
        for (c in conjuncts) {
            if (bindingConjuncts.containsKey(c.expr)) continue
            val mem = matchMembershipBind(c.expr, argNames - equalityBound - membershipBound) ?: continue
            val (arg, rawSet) = mem
            val setExpr = substLetEnvLocal(rawSet, c.letEnv)
            if (isUnboundedTypeDomain(setExpr)) continue
            if (!rhsOnlyUsesAllowed(setExpr, argNames, currentlyBound() - setOf(arg) + availableFromOther)) {
                continue
            }
            membershipBound += arg
            bindingConjuncts[c.expr] = true
            changed = true
        }
    }

    return BoundArgAnalysis(
        boundArgs = equalityBound + membershipBound,
        bindingConjuncts = bindingConjuncts,
        equalityBound = equalityBound,
        membershipBound = membershipBound,
    )
}

private fun matchEqualityBind(
    conjunct: ExprNode,
    argNames: Set<String>,
    alreadyBound: Set<String>,
    conflicted: Set<String>,
): Pair<String, ExprNode>? {
    val e = unwrapParenLocal(conjunct)
    fun oneSide(argSide: ExprNode, other: ExprNode): Pair<String, ExprNode>? {
        val a = (argSide as? SymbolValueExprNode)?.symbol ?: return null
        if (a !in argNames || a in conflicted) return null
        if (exprReferencesSymbol(other, a)) return null
        val unbound = argNames.filter { it != a && it !in alreadyBound }
        if (unbound.any { exprReferencesSymbol(other, it) }) return null
        return a to other
    }
    if (e is BinaryOpExprNode && e.op() == "<=>") {
        val lhs = unwrapParenLocal(e.lhsOperand())
        val rhs = unwrapParenLocal(e.rhsOperand())
        return oneSide(lhs, rhs) ?: oneSide(rhs, lhs)
    }
    if (isDesugaredIff(e) && e is BinaryOpExprNode) {
        val l = unwrapParenLocal(e.lhsOperand()) as BinaryOpExprNode
        val argSide = unwrapParenLocal(l.lhsOperand())
        val other = unwrapParenLocal(l.rhsOperand())
        return oneSide(argSide, other) ?: oneSide(other, argSide)
    }
    if (e !is BinaryOpExprNode || e.op() != "=") return null
    val lhs = unwrapParenLocal(e.lhsOperand())
    val rhs = unwrapParenLocal(e.rhsOperand())
    return oneSide(lhs, rhs) ?: oneSide(rhs, lhs)
}

private fun matchMembershipBind(
    conjunct: ExprNode,
    unbound: Set<String>,
): Pair<String, ExprNode>? {
    val e = unwrapParenLocal(conjunct)
    if (e !is BinaryOpExprNode || e.op() != "in") return null
    val lhs = unwrapParenLocal(e.lhsOperand())
    val rhs = unwrapParenLocal(e.rhsOperand())
    val arg = (lhs as? SymbolValueExprNode)?.symbol ?: return null
    if (arg !in unbound) return null
    if (unbound.any { it != arg && exprReferencesSymbol(rhs, it) }) return null
    return arg to rhs
}

/** Type-name domains that are unbounded for Go random-choice binding. */
private val UNBOUNDED_TYPE_DOMAINS = setOf("Int", "String", "Nat", "Real", "Bool", "Boolean")

fun isUnboundedTypeDomain(expr: ExprNode): Boolean {
    val e = unwrapParenLocal(expr)
    return e is SymbolValueExprNode && e.symbol in UNBOUNDED_TYPE_DOMAINS
}

private fun rhsOnlyUsesAllowed(
    expr: ExprNode,
    argNames: Set<String>,
    allowedArgs: Set<String>,
): Boolean {
    for (arg in argNames) {
        if (arg in allowedArgs) continue
        if (exprReferencesSymbol(expr, arg)) return false
    }
    return true
}

private fun guardRefsArgs(
    decl: ActionDecl,
    skip: IdentityHashMap<ExprNode, Boolean>,
    args: Set<String>,
): String? {
    if (args.isEmpty()) return null
    for (g in decl.guards) {
        for (c in collectGuardConjuncts(g)) {
            if (skip.containsKey(c.expr) || skip.containsKey(unwrapParenLocal(c.expr))) continue
            for (arg in args) {
                if (exprReferencesSymbol(c.expr, arg)) return arg
            }
        }
    }
    return null
}

fun actionBodyReferencesArg(decl: ActionDecl, argName: String): Boolean {
    fun callsRef(calls: List<CallStmtNode>): Boolean =
        calls.any { call -> call.callArgs().any { exprReferencesSymbol(it, argName) } }
    if (callsRef(decl.befores) || callsRef(decl.afters)) return true
    if (decl.errors.any { arm -> errorArmRefsArg(arm, argName) }) return true
    if (decl.actionLets.any { exprReferencesSymbol(it.init, argName) }) return true
    fun walk(update: TransitUpdate): Boolean = when (update) {
        is TransitUpdate.Assign -> exprReferencesSymbol(update.expr, argName)
        is TransitUpdate.IndexPut ->
            exprReferencesSymbol(update.index, argName) || exprReferencesSymbol(update.value, argName)
        is TransitUpdate.Let -> exprReferencesSymbol(update.init, argName)
        is TransitUpdate.If ->
            exprReferencesSymbol(update.cond, argName) ||
                update.thenUpdates.any { walk(it) } ||
                update.elseUpdates.any { walk(it) }
    }
    return decl.transits.any { walk(it) }
}

/** Residual (non-binding) guard conjuncts that mention [argName]. */
fun actionGuardReferencesArg(
    decl: ActionDecl,
    argName: String,
    skip: IdentityHashMap<ExprNode, Boolean> = IdentityHashMap(),
): Boolean =
    guardRefsArgs(decl, skip = skip, args = setOf(argName)) != null

private fun errorArmRefsArg(arm: ErrorArmNode, argName: String): Boolean =
    exprReferencesSymbol(arm.condExpr(), argName)

private fun lookupActionDecl(pc: ProcClassDecl, name: String, isConstructor: Boolean): ActionDecl? =
    if (isConstructor) {
        pc.constructors.firstOrNull { it.action.name == name }
    } else {
        pc.transitions.firstOrNull { it.action.name == name }
            ?: pc.constructors.firstOrNull { it.action.name == name }
    }

private fun unwrapParenLocal(expr: ExprNode): ExprNode =
    if (expr is ParenExprNode) unwrapParenLocal(expr.innerExpr()) else expr

private fun substLetEnvLocal(expr: ExprNode, env: Map<String, ExprNode>): ExprNode {
    var out = expr
    env.forEach { (name, replacement) ->
        out = substituteExpr(out, name, replacement)
    }
    return out
}
