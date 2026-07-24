package julay.compiler.analysis

import julay.compiler.ast.RootNode
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
    val scopesJson = scope.rootNames.mapIndexed { i, rootName ->
        val leaves = scope.leafSets.getOrElse(i) { scope.leafComponents }
        val pd = procDecls.firstOrNull { it.name == rootName }
            ?: ProcDecl(rootName, emptyList(), ProcDeclType.Proc)
        val leafMap = leafActionMap(ast, leaves, librariesInUse)
        val alphabet = computeCompositionAlphabet(pd, procDecls, leafMap)
        scopeAlphabetJson(rootName, alphabet)
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

private fun scopeAlphabetJson(name: String, alphabet: CompositionAlphabetResult): String {
    val external = alphabet.external
    val sourceInternal = alphabet.allOffers.filter { it.sourceInternal }
    val hidden = alphabet.allOffers.filter { it.compositionHidden && !it.sourceInternal }
    val syncGroups = hidden.groupBy { it.channelKey }.entries.sortedBy { it.key }

    return buildString {
        append("{\n")
        append("  \"name\": ").append(jsonString(name)).append(",\n")
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

private fun syncGroupJson(channelKey: String, offers: List<AlphabetOffer>): String {
    val sorted = offers.sortedWith(
        compareBy({ it.pclassKey }, { it.occurrenceId }, { it.introducingAssembly }),
    )
    val actionName = sorted.firstOrNull()?.name ?: ""
    val sample = sorted.firstOrNull()
    return buildString {
        append("{\n")
        append("  \"name\": ").append(jsonString(actionName)).append(",\n")
        append("  \"channelKey\": ").append(jsonString(channelKey)).append(",\n")
        if (sample != null) {
            append("  \"args\": ").append(argsJson(sample)).append(",\n")
            append("  \"isSession\": ").append(sample.isSession).append(",\n")
            append("  \"isConstructor\": ").append(sample.isConstructor).append(",\n")
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
