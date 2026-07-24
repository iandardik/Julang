package julay.compiler.analysis

import julay.compiler.decl.ProcDecl
import julay.compiler.decl.ProcDeclType
import julay.program.library.LibraryRegistry

fun printCompositionTrees(
    rootNames: List<String>,
    procDecls: List<ProcDecl>,
    allPClassNames: Set<String>,
    librariesInUse: Set<String>,
) {
    val byName = procDecls.associateBy { it.name }
    rootNames.forEach { root ->
        printTreeNode(
            name = root,
            byName = byName,
            allPClassNames = allPClassNames,
            librariesInUse = librariesInUse,
            prefix = "",
            isLast = true,
            path = mutableSetOf(),
            isRoot = true,
        )
    }
}

private fun printTreeNode(
    name: String,
    byName: Map<String, ProcDecl>,
    allPClassNames: Set<String>,
    librariesInUse: Set<String>,
    prefix: String,
    isLast: Boolean,
    path: MutableSet<String>,
    isRoot: Boolean,
) {
    val connector = when {
        isRoot -> ""
        isLast -> "└── "
        else -> "├── "
    }
    val label = nodeLabel(name, byName[name], allPClassNames, librariesInUse)

    if (name in path) {
        println("$prefix$connector$label … (cycle)")
        return
    }

    println("$prefix$connector$label")

    val decl = byName[name]
    // Preserve duplicate component occurrences (occurrence-based ||).
    val childNames = decl?.components?.map { cmpt ->
        byName[cmpt.name]?.name ?: cmpt.name
    }.orEmpty()
    if (childNames.isEmpty()) {
        return
    }

    path.add(name)
    val childPrefix = when {
        isRoot -> ""
        isLast -> "$prefix    "
        else -> "$prefix│   "
    }
    childNames.forEachIndexed { index, child ->
        printTreeNode(
            name = child,
            byName = byName,
            allPClassNames = allPClassNames,
            librariesInUse = librariesInUse,
            prefix = childPrefix,
            isLast = index == childNames.lastIndex,
            path = path,
            isRoot = false,
        )
    }
    path.remove(name)
}

private fun nodeLabel(
    name: String,
    decl: ProcDecl?,
    allPClassNames: Set<String>,
    librariesInUse: Set<String>,
): String {
    if (decl != null && decl.components.isNotEmpty()) {
        val kind = when (decl.type) {
            ProcDeclType.Spec -> "spec"
            ProcDeclType.Proc -> "proc"
        }
        return "$kind $name"
    }
    if (decl != null && decl.type == ProcDeclType.Spec) {
        return "spec $name"
    }
    if (name in librariesInUse && LibraryRegistry.isKotlinLibrary(name)) {
        return "lib $name"
    }
    if (LibraryRegistry.isKotlinLibrary(name)) {
        return "lib $name"
    }
    if (name in allPClassNames) {
        return "proc $name"
    }
    if (decl != null) {
        return "proc $name"
    }
    return name
}
