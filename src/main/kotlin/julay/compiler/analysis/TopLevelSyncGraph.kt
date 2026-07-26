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
 * Also includes provider↔client meetings (clients leave the external alphabet;
 * providers keep a public action name as channel key).
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

            val leftByName = left.groupBy { it.name }
            val rightByName = right.groupBy { it.name }
            for (name in leftByName.keys.intersect(rightByName.keys)) {
                if (name == "initially") continue
                val l = leftByName.getValue(name)
                val r = rightByName.getValue(name)
                val leftHasProvider = l.any { it.isProvider }
                val rightHasProvider = r.any { it.isProvider }
                val leftHasClient = l.any { it.isClient }
                val rightHasClient = r.any { it.isClient }
                if (!((leftHasProvider && rightHasClient) || (rightHasProvider && leftHasClient))) {
                    continue
                }
                val leftNodes = l.mapNotNull { tags[it.leafId] }.toSet()
                val rightNodes = r.mapNotNull { tags[it.leafId] }.toSet()
                addEdgesBetween(leftNodes, rightNodes, name)
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
    if (ast != null && procFunNames.isNotEmpty() && pclasses.isNotEmpty()) {
        childDecls.forEachIndexed { index, child ->
            val callerNode = childNodes[index]
            val called = calledProcFunNamesUnderChild(child, procDecls, procFunNames, pclasses)
            for (pf in called) {
                val calleeNode = childNodes.firstOrNull { it == pf }
                    ?: pf.also { extraProcFunNodes += it }
                if (callerNode != calleeNode) {
                    edgeActions.getOrPut(canonPair(callerNode, calleeNode)) { mutableSetOf() }
                }
            }
        }
    }

    val nodes = childNodes + extraProcFunNodes.filter { it !in childNodes }.sorted()
    if (nodes.size < 2) {
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

    return TopLevelSyncGraph(nodes = nodes, edges = edges)
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
