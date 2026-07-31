package julay.compiler.analysis

import julay.compiler.ast.RootNode
import julay.compiler.collectProcFunNames
import julay.compiler.decl.ProcDecl
import julay.compiler.decl.ProcDeclType
import julay.compiler.pass.AlphabetOffer
import julay.compiler.pass.CompositionAlphabetResult
import julay.compiler.pass.computeCompositionAlphabet
import julay.compiler.pass.leafActionMap
import julay.program.action.TSAction

/**
 * Build the machine-readable alphabet JSON for [scope] roots.
 * Always includes external, source-internal, and composition-hidden sync groups
 * so IDE panels can toggle without a second analyze pass.
 */
fun buildAlphabetJsonDocument(
    ast: RootNode,
    scope: ResolvedAnalyzeScope,
    librariesInUse: Set<String>,
    procDecls: List<ProcDecl>,
): String {
    val procFunNames = collectProcFunNames(ast)
    val scopesJson = scope.rootNames.mapIndexed { i, rootName ->
        val leaves = scope.leafSets.getOrElse(i) { scope.leafComponents }
        val pd = procDecls.firstOrNull { it.name == rootName }
            ?: ProcDecl(rootName, emptyList(), ProcDeclType.Proc)
        val leafMap = leafActionMap(ast, leaves, librariesInUse)
        val alphabet = computeCompositionAlphabet(pd, procDecls, leafMap, procFunNames, ast)
        val graph = computeTopLevelSyncGraph(pd, procDecls, leafMap, procFunNames, ast)
        scopeAlphabetJson(rootName, alphabet, graph, procFunNames)
    }
    return buildString {
        append("{\n  \"scopes\": [\n")
        scopesJson.forEachIndexed { index, block ->
            if (index > 0) append(",\n")
            append(indent(block, 4))
        }
        append("\n  ]\n}\n")
    }
}

fun printAlphabetJson(
    ast: RootNode,
    scope: ResolvedAnalyzeScope,
    librariesInUse: Set<String>,
    procDecls: List<ProcDecl>,
) {
    print(buildAlphabetJsonDocument(ast, scope, librariesInUse, procDecls))
}

private fun scopeAlphabetJson(
    name: String,
    alphabet: CompositionAlphabetResult,
    graph: TopLevelSyncGraph,
    procFunNames: Set<String>,
): String {
    // Hide synthetic procfun call/ret plumbing from IDE external alphabet by default.
    fun isSyntheticProcFunOffer(o: AlphabetOffer): Boolean {
        val n = o.name
        return n == "${o.pclassKey}_call" || n == "${o.pclassKey}_ret" || n.startsWith("invoke_")
    }
    // Standalone procfun analyze: show the helper's full useful surface as "external"
    // (including source-internal / bare-return steps). Synthetics stay hidden.
    // Parent assemblies keep the usual external vs source-internal split.
    val external: List<AlphabetOffer>
    val sourceInternal: List<AlphabetOffer>
    if (name in procFunNames) {
        external = alphabet.allOffers.filterNot {
            isSyntheticProcFunOffer(it) || it.compositionHidden
        }
        sourceInternal = emptyList()
    } else {
        external = alphabet.external.filterNot(::isSyntheticProcFunOffer)
        sourceInternal = alphabet.allOffers.filter { it.sourceInternal && !isSyntheticProcFunOffer(it) }
    }
    val syncGroups = compositionHiddenSyncGroups(alphabet)

    return buildString {
        append("{\n")
        append("  \"name\": ").append(jsonString(name)).append(",\n")
        append("  \"compositionGraph\": ").append(compositionGraphJson(graph)).append(",\n")
        append("  \"external\": ").append(offerArrayJson(external)).append(",\n")
        append("  \"sourceInternal\": ").append(offerArrayJson(sourceInternal)).append(",\n")
        append("  \"compositionHidden\": [\n")
        syncGroups.forEachIndexed { index, (channelKey, offers) ->
            if (index > 0) append(",\n")
            append(indent(syncGroupJson(channelKey, offers), 4))
        }
        if (syncGroups.isNotEmpty()) append("\n")
        append("  ]\n")
        append("}")
    }
}

/**
 * Ordinary/session syncs share a private [AlphabetOffer.channelKey] and group together.
 * Provider↔client meetings keep the public action name as channel key and hide only the
 * clients; grouping those clients alone would wrongly look like they sync with each other.
 * Emit one group per client that includes the matching provider peer.
 */
internal fun compositionHiddenSyncGroups(
    alphabet: CompositionAlphabetResult,
): List<Pair<String, List<AlphabetOffer>>> {
    val hidden = alphabet.allOffers.filter { it.compositionHidden && !it.sourceInternal }
    val providerByName = alphabet.allOffers
        .filter { it.isProvider && !it.sourceInternal }
        .groupBy { it.name }
        .mapValues { (_, offers) -> offers.first() }

    val providerClientClients = mutableListOf<AlphabetOffer>()
    val ordinaryHidden = mutableListOf<AlphabetOffer>()
    for (offer in hidden) {
        val provider = providerByName[offer.name]
        if (offer.isClient && provider != null && offer.channelKey == provider.channelKey) {
            providerClientClients += offer
        } else {
            ordinaryHidden += offer
        }
    }

    val ordinaryGroups = ordinaryHidden
        .groupBy { it.channelKey }
        .entries
        .map { (key, offers) -> key to offers }

    val providerClientGroups = providerClientClients
        .sortedWith(compareBy({ it.name }, { it.pclassKey }, { it.occurrenceId }))
        .map { client ->
            val provider = providerByName.getValue(client.name)
            // Distinct key so multiple clients of the same provider are separate list entries.
            "${client.channelKey}@${client.occurrenceId}" to listOf(provider, client)
        }

    return (ordinaryGroups + providerClientGroups).sortedBy { it.first }
}

private fun compositionGraphJson(graph: TopLevelSyncGraph): String {
    // Call edges use empty actions with a=caller, b=callee (directed).
    // Sync edges use non-empty actions; a/b are an undirected canonical pair.
    if (graph.nodes.isEmpty()) {
        return """{"nodes": [], "edges": []}"""
    }
    return buildString {
        append("{\n")
        append("  \"nodes\": [")
        append(graph.nodes.joinToString(", ") { jsonString(it) })
        append("],\n")
        append("  \"edges\": [\n")
        graph.edges.forEachIndexed { index, edge ->
            if (index > 0) append(",\n")
            append("    {\n")
            append("      \"a\": ").append(jsonString(edge.a)).append(",\n")
            append("      \"b\": ").append(jsonString(edge.b)).append(",\n")
            append("      \"actions\": [")
            append(edge.actions.joinToString(", ") { jsonString(it) })
            append("]\n")
            append("    }")
        }
        if (graph.edges.isNotEmpty()) append("\n")
        append("  ]\n")
        append("}")
    }
}

private fun syncGroupJson(channelKey: String, offers: List<AlphabetOffer>): String {
    // Provider before clients so hub‖client listings read naturally in the IDE panel.
    val sorted = offers.sortedWith(
        compareBy(
            { if (it.isProvider) 0 else 1 },
            { it.pclassKey },
            { it.occurrenceId },
            { it.introducingAssembly },
        ),
    )
    val actionName = sorted.firstOrNull()?.name ?: ""
    val sample = sorted.firstOrNull()
    val hasProvider = sorted.any { it.isProvider }
    val hasClient = sorted.any { it.isClient }
    val roleLabel = when {
        sample == null -> "ordinary"
        sample.isConstructor -> "constructor"
        sample.isSession -> "session"
        hasProvider && hasClient -> "provider/client"
        else -> modifierLabel(sample)
    }
    return buildString {
        append("{\n")
        append("  \"name\": ").append(jsonString(actionName)).append(",\n")
        append("  \"channelKey\": ").append(jsonString(channelKey)).append(",\n")
        if (sample != null) {
            append("  \"args\": ").append(argsJson(sample)).append(",\n")
            append("  \"isSession\": ").append(sample.isSession).append(",\n")
            append("  \"isConstructor\": ").append(sample.isConstructor).append(",\n")
            append("  \"role\": ").append(jsonString(roleLabel)).append(",\n")
        }
        append("  \"peers\": [\n")
        sorted.forEachIndexed { i, o ->
            if (i > 0) append(",\n")
            append("    {\n")
            append("      \"pclassKey\": ").append(jsonString(o.pclassKey)).append(",\n")
            append("      \"occurrenceId\": ").append(jsonString(o.occurrenceId)).append(",\n")
            append("      \"introducingAssembly\": ").append(jsonString(o.introducingAssembly)).append("\n")
            append("    }")
        }
        if (sorted.isNotEmpty()) append("\n")
        append("  ],\n")
        append("  \"offers\": ").append(offerArrayJson(sorted)).append("\n")
        append("}")
    }
}

private fun offerArrayJson(offers: List<AlphabetOffer>): String {
    if (offers.isEmpty()) return "[]"
    val sorted = offers.sortedWith(
        compareBy(
            { it.name },
            { it.pclassKey },
            { it.occurrenceId },
            { it.isConstructor },
        ),
    )
    return buildString {
        append("[\n")
        sorted.forEachIndexed { index, offer ->
            if (index > 0) append(",\n")
            append(indent(offerJson(offer), 2))
        }
        append("\n]")
    }
}

private fun offerJson(offer: AlphabetOffer): String = buildString {
    append("{\n")
    append("  \"name\": ").append(jsonString(offer.name)).append(",\n")
    append("  \"args\": ").append(argsJson(offer)).append(",\n")
    append("  \"modifier\": ").append(jsonString(modifierLabel(offer))).append(",\n")
    append("  \"isSession\": ").append(offer.isSession).append(",\n")
    append("  \"isConstructor\": ").append(offer.isConstructor).append(",\n")
    append("  \"pclassKey\": ").append(jsonString(offer.pclassKey)).append(",\n")
    append("  \"occurrenceId\": ").append(jsonString(offer.occurrenceId)).append(",\n")
    append("  \"introducingAssembly\": ").append(jsonString(offer.introducingAssembly)).append(",\n")
    append("  \"channelKey\": ").append(jsonString(offer.channelKey)).append(",\n")
    append("  \"compositionHidden\": ").append(offer.compositionHidden).append(",\n")
    append("  \"sourceInternal\": ").append(offer.sourceInternal).append("\n")
    append("}")
}

private fun argsJson(offer: AlphabetOffer): String {
    if (offer.args.isEmpty()) return "[]"
    return offer.args.joinToString(prefix = "[", postfix = "]") { arg ->
        jsonString("${arg.name}: ${arg.type}")
    }
}

private fun modifierLabel(offer: AlphabetOffer): String = when {
    offer.isSession -> "session"
    offer.modifier == TSAction.SyncRole.Provider -> "provider"
    offer.modifier == TSAction.SyncRole.Client -> "client"
    offer.modifier == TSAction.SyncRole.Internal || offer.sourceInternal -> "internal"
    else -> "ordinary"
}

internal fun jsonString(value: String): String = buildString {
    append('"')
    for (c in value) {
        when (c) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> append(c)
        }
    }
    append('"')
}

private fun indent(block: String, spaces: Int): String {
    val pad = " ".repeat(spaces)
    return block.lineSequence().joinToString("\n") { line ->
        if (line.isEmpty()) line else pad + line
    }
}
