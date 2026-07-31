package julay.compiler.analysis

import julay.compiler.ast.ProcClassNode
import julay.compiler.ast.RootNode
import julay.compiler.decl.ProcDecl
import julay.compiler.pass.AlphabetOffer
import julay.compiler.pass.LeafActionId
import julay.compiler.pass.collectLeafOccurrences
import julay.compiler.pass.collectProcFunCallsInProc
import julay.compiler.pass.composeAlphabets
import julay.compiler.pass.computeCompositionAlphabet

/**
 * Immediate children of [root] as diagram nodes, with edges for actions that
 * composition-hide while folding those children (not nested syncs inside a child).
 * Also includes provider↔client meetings (each client leaves the external alphabet
 * with the provider; clients never gain edges to each other).
 *
 * Called procfuns are added as nodes with unlabeled edges to their calling
 * top-level child (spawn-await, not SyncChannel peers).
 */
data class TopLevelSyncGraph(
    val nodes: List<String>,
    val edges: List<TopLevelSyncEdge>,
)

data class TopLevelSyncEdge(
    val a: String,
    val b: String,
    /** Empty for procfun call edges; otherwise composition-hidden / provider-client action names. */
    val actions: List<String>,
)

fun computeTopLevelSyncGraph(
    root: ProcDecl,
    procDecls: List<ProcDecl>,
    leafOffersByPclass: Map<String, List<AlphabetOffer>>,
    procFunNames: Set<String> = emptySet(),
    ast: RootNode? = null,
): TopLevelSyncGraph {
    val procDeclMap = procDecls.associateBy { it.name }
    fun resolve(pd: ProcDecl): ProcDecl = procDeclMap[pd.name] ?: pd

    val resolvedRoot = resolve(root)
    val childDecls: List<ProcDecl> = if (resolvedRoot.components.isEmpty()) {
        // Leaf analyze root: the root itself is the diagram host.
        listOf(resolvedRoot)
    } else {
        resolvedRoot.components.map { resolve(it) }
    }

    val rawNames = childDecls.map { it.name }
    val nameTotals = rawNames.groupingBy { it }.eachCount()
    val nameSeen = mutableMapOf<String, Int>()
    val childNodes = rawNames.map { name ->
        val n = (nameSeen[name] ?: 0) + 1
        nameSeen[name] = n
        if ((nameTotals[name] ?: 0) > 1) "${name}_$n" else name
    }

    val edgeActions = mutableMapOf<Pair<String, String>, MutableSet<String>>()

    fun canonPair(x: String, y: String): Pair<String, String> =
        if (x <= y) x to y else y to x

    fun addEdgesBetween(leftNodes: Set<String>, rightNodes: Set<String>, action: String) {
        for (a in leftNodes) {
            for (b in rightNodes) {
                if (a != b) {
                    edgeActions.getOrPut(canonPair(a, b)) { mutableSetOf() }.add(action)
                }
            }
        }
    }

    // Sync edges among || children (need at least two components).
    if (resolvedRoot.components.size >= 2) {
        val tags = mutableMapOf<LeafActionId, String>()
        val childAlphabets = resolvedRoot.components.mapIndexed { index, child ->
            val childResolved = resolve(child)
            val childAlphabet = computeCompositionAlphabet(
                childResolved, procDecls, leafOffersByPclass, procFunNames, ast,
            )
            val node = childNodes[index]
            childAlphabet.external.map { offer ->
                val uniquified = offer.copy(occurrenceId = "tl${index}_${offer.occurrenceId}")
                tags[uniquified.leafId] = node
                uniquified
            }
        }

        fun recordSyncs(
            left: List<AlphabetOffer>,
            right: List<AlphabetOffer>,
            composed: List<AlphabetOffer>,
            scopeId: String,
        ) {
            val leftIds = left.map { it.leafId }.toSet()
            val rightIds = right.map { it.leafId }.toSet()

            composed
                .filter { it.compositionHidden && it.channelKey.startsWith("$scopeId#") }
                .groupBy { it.channelKey }
                .forEach { (_, offers) ->
                    val action = offers.firstOrNull()?.name ?: return@forEach
                    if (action == "initially") return@forEach
                    val leftNodes = offers.mapNotNull { o ->
                        if (o.leafId in leftIds) tags[o.leafId] else null
                    }.toSet()
                    val rightNodes = offers.mapNotNull { o ->
                        if (o.leafId in rightIds) tags[o.leafId] else null
                    }.toSet()
                    addEdgesBetween(leftNodes, rightNodes, action)
                }

            // Provider↔client meetings only involve offers that are still external at this
            // compose step. Already-hidden clients must not gain edges to later clients
            // (clients never sync with each other — only with the provider hub).
            val leftExt = left.filter { !it.sourceInternal && !it.compositionHidden }
            val rightExt = right.filter { !it.sourceInternal && !it.compositionHidden }
            val leftByName = leftExt.groupBy { it.name }
            val rightByName = rightExt.groupBy { it.name }
            for (name in leftByName.keys.intersect(rightByName.keys)) {
                if (name == "initially") continue
                val l = leftByName.getValue(name)
                val r = rightByName.getValue(name)
                val leftProviders = l.filter { it.isProvider }.mapNotNull { tags[it.leafId] }.toSet()
                val rightProviders = r.filter { it.isProvider }.mapNotNull { tags[it.leafId] }.toSet()
                val leftClients = l.filter { it.isClient }.mapNotNull { tags[it.leafId] }.toSet()
                val rightClients = r.filter { it.isClient }.mapNotNull { tags[it.leafId] }.toSet()
                if (leftProviders.isNotEmpty() && rightClients.isNotEmpty()) {
                    addEdgesBetween(leftProviders, rightClients, name)
                }
                if (rightProviders.isNotEmpty() && leftClients.isNotEmpty()) {
                    addEdgesBetween(rightProviders, leftClients, name)
                }
            }
        }

        var acc = childAlphabets[0]
        for (i in 1 until childAlphabets.size) {
            val scopeId = "toplevel_${resolvedRoot.name}_$i"
            val right = childAlphabets[i]
            val (composed, _) = composeAlphabets(acc, right, scopeId)
            recordSyncs(acc, right, composed, scopeId)
            acc = composed
        }
    }

    // Spawn-await call edges: top-level child → called procfun (no action labels).
    val pclasses = ast?.declNodes()?.filterIsInstance<ProcClassNode>()?.associateBy { it.name() }.orEmpty()
    val extraProcFunNodes = linkedSetOf<String>()
    val callerToCallees = mutableMapOf<String, MutableSet<String>>()
    if (ast != null && procFunNames.isNotEmpty() && pclasses.isNotEmpty()) {
        childDecls.forEachIndexed { index, child ->
            val callerNode = childNodes[index]
            val called = calledProcFunNamesUnderChild(child, procDecls, procFunNames, pclasses)
            for (pf in called) {
                val calleeNode = childNodes.firstOrNull { it == pf }
                    ?: pf.also { extraProcFunNodes += it }
                if (callerNode != calleeNode) {
                    edgeActions.getOrPut(canonPair(callerNode, calleeNode)) { mutableSetOf() }
                    callerToCallees.getOrPut(callerNode) { mutableSetOf() }.add(calleeNode)
                }
            }
        }
    }

    val procFunOnly = extraProcFunNodes.filter { it !in childNodes }
    if (childNodes.size + procFunOnly.size < 2) {
        return TopLevelSyncGraph(nodes = emptyList(), edges = emptyList())
    }

    val edges = edgeActions.entries
        .map { (pair, actions) ->
            TopLevelSyncEdge(
                a = pair.first,
                b = pair.second,
                actions = actions.sorted(),
            )
        }
        .sortedWith(compareBy({ it.a }, { it.b }))

    val nodes = orderDiagramNodes(
        childNodes = childNodes,
        procFunNodes = procFunOnly,
        edges = edges,
        callerToCallees = callerToCallees,
    )

    return TopLevelSyncGraph(nodes = nodes, edges = edges)
}

/**
 * Order diagram nodes to cut edge crossings: optimally arrange || children for sync
 * edges, then append called procfuns (grouped by caller) so call edges stay short
 * and do not stretch across the composition spine.
 */
internal fun orderDiagramNodes(
    childNodes: List<String>,
    procFunNodes: List<String>,
    edges: List<TopLevelSyncEdge>,
    callerToCallees: Map<String, Set<String>>,
): List<String> {
    if (childNodes.isEmpty()) {
        return procFunNodes.sorted()
    }
    val syncPairs = edges
        .filter { it.actions.isNotEmpty() }
        .map { it.a to it.b }
        .filter { (a, b) -> a in childNodes && b in childNodes }
    val childOrder = minimizeChildOrderCrossings(childNodes, syncPairs, callerToCallees)

    val result = childOrder.toMutableList()
    val placed = childOrder.toMutableSet()
    for (child in childOrder) {
        val callees = callerToCallees[child].orEmpty()
            .filter { it in procFunNodes }
            .sorted()
        for (pf in callees) {
            if (placed.add(pf)) result += pf
        }
    }
    for (pf in procFunNodes.sorted()) {
        if (placed.add(pf)) result += pf
    }
    return result
}

/**
 * Choose a permutation of [children] that minimizes sync-edge crossings (then total
 * span, then prefers callers with procfuns toward the end so call edges stay short).
 * Brute-force for small n; otherwise iterative adjacent swaps. Ties keep the relative
 * order closest to [children].
 */
private fun minimizeChildOrderCrossings(
    children: List<String>,
    syncPairs: List<Pair<String, String>>,
    callerToCallees: Map<String, Set<String>>,
): List<String> {
    if (children.size <= 1) return children

    fun score(order: List<String>): List<Int> {
        val pos = order.withIndex().associate { it.value to it.index }
        var crossings = 0
        var span = 0
        for (i in syncPairs.indices) {
            val (a, b) = syncPairs[i]
            val a1 = minOf(pos.getValue(a), pos.getValue(b))
            val a2 = maxOf(pos.getValue(a), pos.getValue(b))
            span += a2 - a1
            for (j in i + 1 until syncPairs.size) {
                val (c, d) = syncPairs[j]
                val b1 = minOf(pos.getValue(c), pos.getValue(d))
                val b2 = maxOf(pos.getValue(c), pos.getValue(d))
                if ((a1 < b1 && b1 < a2 && a2 < b2) || (b1 < a1 && a1 < b2 && b2 < a2)) {
                    crossings++
                }
            }
        }
        // Prefer composition children that call procfuns toward the right end.
        var callerEnd = 0
        for (c in children) {
            val nCall = callerToCallees[c]?.size ?: 0
            callerEnd -= nCall * pos.getValue(c)
        }
        var drift = 0
        for (i in children.indices) {
            drift += kotlin.math.abs(pos.getValue(children[i]) - i)
        }
        return listOf(crossings, span, callerEnd, drift)
    }

    fun better(a: List<Int>, b: List<Int>): Boolean {
        for (i in a.indices) {
            if (a[i] < b[i]) return true
            if (a[i] > b[i]) return false
        }
        return false
    }

    if (syncPairs.isEmpty() && callerToCallees.values.all { it.isEmpty() }) {
        return children
    }

    if (children.size <= 7) {
        var best = children
        var bestScore = score(children)
        permute(children) { perm ->
            val s = score(perm)
            if (better(s, bestScore)) {
                bestScore = s
                best = perm
            }
        }
        return best
    }

    val order = children.toMutableList()
    var improved = true
    var guard = 0
    while (improved && guard++ < children.size * children.size) {
        improved = false
        var bestScore = score(order)
        for (i in 0 until order.lastIndex) {
            order[i] = order[i + 1].also { order[i + 1] = order[i] }
            val s = score(order)
            if (better(s, bestScore)) {
                bestScore = s
                improved = true
            } else {
                order[i] = order[i + 1].also { order[i + 1] = order[i] }
            }
        }
    }
    return order
}

/** Invoke [visit] for every permutation of [items] (including the identity). */
private fun permute(items: List<String>, visit: (List<String>) -> Unit) {
    val arr = items.toMutableList()
    fun go(k: Int) {
        if (k == arr.size) {
            visit(arr.toList())
            return
        }
        for (i in k until arr.size) {
            arr[k] = arr[i].also { arr[i] = arr[k] }
            go(k + 1)
            arr[k] = arr[i].also { arr[i] = arr[k] }
        }
    }
    go(0)
}

/** Procfun names called by non-procfun host leaves under [child]. */
private fun calledProcFunNamesUnderChild(
    child: ProcDecl,
    procDecls: List<ProcDecl>,
    procFunNames: Set<String>,
    pclasses: Map<String, ProcClassNode>,
): Set<String> {
    val hosts = collectLeafOccurrences(child, procDecls)
        .map { it.pclassName }
        .filter { it !in procFunNames }
        .distinct()
    return hosts.flatMap { host ->
        val pc = pclasses[host] ?: return@flatMap emptyList()
        collectProcFunCallsInProc(pc).mapNotNull { it.resolvedProcFunOrNull()?.procFunName() }
    }.filter { it in procFunNames }.toSet()
}
