package julay.compiler.analysis

import julay.compiler.decl.ProcDecl
import julay.compiler.decl.ProcDeclType
import julay.program.library.LibraryRegistry

/**
 * Options for `julayc analyze`.
 *
 * If no view flag is set, [showTree] defaults to true at the CLI layer.
 */
data class AnalyzeOptions(
    val scopeNames: List<String> = emptyList(),
    val showTree: Boolean = false,
    val showActions: Boolean = false,
    val actionsDetail: Boolean = false,
    val showProcs: Boolean = false,
    val procsDetail: Boolean = false,
    val actionNames: List<String> = emptyList(),
    val actionRegex: String? = null,
    val includeInternal: Boolean = false,
    /** With multiple -s, restrict action views to ∩ of each scope's alphabet. */
    val scopeIntersect: Boolean = false,
    /**
     * With multiple -s, restrict action views to ∩ of each scope's alphabet and
     * further omit actions those scopes do not sync on (e.g. provider actions when
     * every selected scope is only a client).
     */
    val scopeMutual: Boolean = false,
    /** Emit machine-readable alphabet JSON (IDE / tooling). Suppresses human text views. */
    val json: Boolean = false,
) {
    val showActionView: Boolean
        get() = showActions || actionsDetail

    val showProcView: Boolean
        get() = showProcs || procsDetail
}

/**
 * Resolved analyze scope: tree roots plus per-root leaf pclass/lib sets.
 * Null if any scope name was unknown (errors already printed).
 */
data class ResolvedAnalyzeScope(
    val rootNames: List<String>,
    /** One leaf set per [rootNames] entry (same order). */
    val leafSets: List<Set<String>>,
) {
    /** Union of all leaf sets (pclass views, default action views). */
    val leafComponents: Set<String>
        get() = leafSets.flatten().toSet()
}

fun resolveAnalyzeScope(
    scopeNames: List<String>,
    procDecls: List<ProcDecl>,
    allPClassNames: Set<String>,
    allProcAliasNames: Set<String>,
    librariesInUse: Set<String>,
): ResolvedAnalyzeScope? {
    val roots = if (scopeNames.isEmpty()) {
        defaultScopeRoots(procDecls, allProcAliasNames, allPClassNames)
    } else {
        val unknown = scopeNames.filter { name ->
            procDecls.none { it.name == name } &&
                name !in allPClassNames &&
                !(name in librariesInUse && LibraryRegistry.isKotlinLibrary(name))
        }
        if (unknown.isNotEmpty()) {
            unknown.forEach { println("Unknown proc or spec: \"$it\"") }
            return null
        }
        scopeNames.distinct()
    }

    val leafSets = roots.map { root ->
        resolveNameToLeaves(root, procDecls, allPClassNames, librariesInUse)
    }

    return ResolvedAnalyzeScope(rootNames = roots, leafSets = leafSets)
}

fun defaultScopeRoots(
    procDecls: List<ProcDecl>,
    allProcAliasNames: Set<String>,
    allPClassNames: Set<String>,
): List<String> {
    val specs = procDecls.filter { it.type == ProcDeclType.Spec }.map { it.name }
    val procs = procDecls.filter { it.type == ProcDeclType.Proc }.map { it.name }
    return (specs + procs + allProcAliasNames + allPClassNames).distinct().sorted()
}

private fun resolveNameToLeaves(
    name: String,
    procDecls: List<ProcDecl>,
    allPClassNames: Set<String>,
    librariesInUse: Set<String>,
): Set<String> {
    val decl = procDecls.firstOrNull { it.name == name }
    return when {
        decl != null -> decl.allProcNames(procDecls)
        name in allPClassNames ||
            (name in librariesInUse && LibraryRegistry.isKotlinLibrary(name)) ->
            setOf(name)
        else -> emptySet()
    }
}
