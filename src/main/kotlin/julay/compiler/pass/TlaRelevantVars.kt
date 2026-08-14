package julay.compiler.pass

import julay.compiler.ast.*
import julay.compiler.decl.TransitUpdate

/**
 * Per-leaf state var/const relevance for TLA+ emission (`unused-vars`).
 *
 * A var is relevant when the emitted spec reads it: the invariant closure, or
 * guards / relevant transits of offer groups that produce a TLA action.
 * Guard-only actions omitted from Next do not mark a var relevant.
 * Assignment targets alone do not mark a var relevant.
 */
class TlaRelevantVars internal constructor(
    val projecting: Boolean,
    private val relevant: Set<Pair<String, String>>,
) {
    fun isRelevant(leafClass: String, varName: String): Boolean =
        if (!projecting) true else (leafClass to varName) in relevant

    companion object {
        val IDENTITY = TlaRelevantVars(projecting = false, relevant = emptySet())
    }
}

/** Compilation-scoped projection read by TLA Init / transit emitters. */
internal object TlaVarProjection {
    private val current = ThreadLocal.withInitial { TlaRelevantVars.IDENTITY }

    fun get(): TlaRelevantVars = current.get()

    fun set(vars: TlaRelevantVars) {
        current.set(vars)
    }
}

internal fun analyzeTlaRelevantVars(
    pclasses: Map<String, ProcClassNode>,
    offers: List<TlaActionOffer>,
    usedFuns: Collection<FunNode>,
    invClosure: List<InvariantNode>,
    procFunLeafNames: Set<String> = emptySet(),
    callSites: List<ProcFunCallSite> = emptyList(),
): TlaRelevantVars {
    val varsByLeaf = pclasses.mapValues { (_, pc) ->
        pc.localDecls().filterIsInstance<VarNode>().map { it.name }.toSet()
    }
    val lists = collectEmittedOfferLists(offers)
    val relevant = mutableSetOf<Pair<String, String>>()
    // Procfun leaves are always written by *_call / *_ret handshake; keep every state var.
    procFunLeafNames.forEach { leafName ->
        varsByLeaf[leafName].orEmpty().forEach { relevant += leafName to it }
    }
    callSites.forEach { site ->
        site.assignVars.forEach { relevant += site.hostName to it }
        site.callArgs.forEach { collectStateReads(it, site.hostName, varsByLeaf, emptySet(), relevant) }
    }

    fun offerHasRelevantUpdate(offer: TlaActionOffer): Boolean {
        if (offer.isConstructor) return true
        if (offer.decl.isReturn) return true
        return offer.decl.transits.any { update ->
            when (update) {
                is TransitUpdate.Assign -> relevant.contains(offer.leaf.name to update.transitRootVar())
                is TransitUpdate.IndexPut -> relevant.contains(offer.leaf.name to update.transitRootVar())
                is TransitUpdate.Let -> false
            }
        }
    }

    fun groupHasUpdate(group: List<TlaActionOffer>, requireRelevant: Boolean): Boolean =
        group.any { offer ->
            if (offer.isConstructor || offer.decl.isReturn) return@any true
            offer.decl.transits.any { update ->
                when (update) {
                    is TransitUpdate.Assign, is TransitUpdate.IndexPut ->
                        !requireRelevant || relevant.contains(offer.leaf.name to update.transitRootVar())
                    is TransitUpdate.Let -> false
                }
            }
        }

    fun seedAndPropagate(groups: List<List<TlaActionOffer>>) {
        groups.forEach { group ->
            group.forEach { offer ->
                val bound = boundNamesOf(offer)
                offer.decl.guards.forEach { collectStateReads(it, offer.leaf.name, varsByLeaf, bound, relevant) }
                offer.decl.errors.forEach { arm ->
                    collectStateReads(arm.condExpr(), offer.leaf.name, varsByLeaf, bound, relevant)
                }
                offer.decl.returnExpr?.let { collectStateReads(it, offer.leaf.name, varsByLeaf, bound, relevant) }
            }
        }
        invClosure.forEach { collectStateReads(it.invariantFormula(), null, varsByLeaf, emptySet(), relevant) }
        usedFuns.forEach { fn ->
            val bound = fn.funArgs().actionArgs().map { it.name }.toSet()
            collectStateReads(fn.funBody(), null, varsByLeaf, bound, relevant)
        }
        var changed = true
        var steps = 0
        while (changed && steps++ < 64) {
            changed = false
            val before = relevant.size
            groups.forEach { group ->
                group.forEach { offer ->
                    if (!offerHasRelevantUpdate(offer) && !offer.isConstructor && !offer.decl.isReturn) {
                        return@forEach
                    }
                    val bound = boundNamesOf(offer).toMutableSet()
                    offer.decl.transits.forEach { update ->
                        when (update) {
                            is TransitUpdate.Let -> {
                                collectStateReads(update.init, offer.leaf.name, varsByLeaf, bound, relevant)
                                bound += update.name
                            }
                            is TransitUpdate.Assign -> {
                                if (relevant.contains(offer.leaf.name to update.transitRootVar())) {
                                    collectStateReads(update.expr, offer.leaf.name, varsByLeaf, bound, relevant)
                                }
                            }
                            is TransitUpdate.IndexPut -> {
                                if (relevant.contains(offer.leaf.name to update.transitRootVar())) {
                                    collectStateReads(update.index, offer.leaf.name, varsByLeaf, bound, relevant)
                                    collectStateReads(update.value, offer.leaf.name, varsByLeaf, bound, relevant)
                                }
                            }
                        }
                    }
                }
            }
            if (relevant.size != before) changed = true
        }
    }

    var groups = lists.filter { groupHasUpdate(it, requireRelevant = false) }
    var steps = 0
    while (steps++ < 8) {
        val snapshot = relevant.toSet()
        seedAndPropagate(groups)
        val nextGroups = lists.filter { groupHasUpdate(it, requireRelevant = true) }
        if (relevant == snapshot && nextGroups == groups) break
        groups = nextGroups
    }

    return TlaRelevantVars(projecting = true, relevant = relevant)
}

private fun boundNamesOf(offer: TlaActionOffer): Set<String> {
    val out = mutableSetOf<String>()
    offer.decl.action.args.forEach { out += it.name }
    return out
}

private fun collectStateReads(
    expr: ExprNode,
    currentLeaf: String?,
    varsByLeaf: Map<String, Set<String>>,
    bound: Set<String>,
    into: MutableSet<Pair<String, String>>,
) {
    val leaves = varsByLeaf.keys
    fun mark(leaf: String, name: String) {
        if (name in varsByLeaf[leaf].orEmpty()) into += leaf to name
    }
    fun walk(e: ExprNode, boundNow: Set<String>) {
        when (e) {
            is SymbolValueExprNode -> {
                val s = e.symbol
                if (s !in boundNow && currentLeaf != null) mark(currentLeaf, s)
            }
            is ThisAccessExprNode -> {
                if (currentLeaf != null) mark(currentLeaf, e.stateVarName())
            }
            is FieldAccessExprNode -> {
                val base = e.baseSymbol
                if (base in leaves && e.fieldPath.isNotEmpty()) {
                    mark(base, e.fieldPath.first())
                } else if (currentLeaf != null && base !in boundNow) {
                    mark(currentLeaf, base)
                }
                e.children.filterIsInstance<ExprNode>().forEach { walk(it, boundNow) }
            }
            is MemberAccessExprNode -> {
                val base = e.baseExpr
                when {
                    base is IndexExprNode && base.base is SymbolValueExprNode -> {
                        val peer = (base.base as SymbolValueExprNode).symbol
                        if (peer in leaves) mark(peer, e.fieldName)
                        walk(base.index, boundNow)
                    }
                    base is SymbolValueExprNode && base.symbol in leaves ->
                        mark(base.symbol, e.fieldName)
                    else -> walk(base, boundNow)
                }
            }
            is FieldAccessOnExprNode -> {
                walk(e.baseExpr, boundNow)
            }
            is QuantifiedExprNode -> walk(e.quantifiedBody(), boundNow + e.binderName())
            is LetExprNode -> {
                walk(e.letInitExpr(), boundNow)
                walk(e.bodyExpr(), boundNow + e.letName())
            }
            is LambdaExprNode -> walk(e.body, boundNow + e.params.toSet())
            is MethodCallExprNode -> {
                walk(e.baseExpr, boundNow)
                e.args.forEach { walk(it, boundNow) }
                val body = e.hofBodyOrNull()
                val names = e.hofParamNamesOrNull()
                if (body != null && names != null) {
                    walk(body, boundNow + names.toSet())
                }
            }
            else -> e.children.filterIsInstance<ExprNode>().forEach { walk(it, boundNow) }
        }
    }
    walk(expr, bound)
}
