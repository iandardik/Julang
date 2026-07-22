package julay.compiler

import julay.compiler.ast.*
import julay.program.library.JULAY_MODULE
import julay.program.library.JulLibrary
import julay.program.library.LibraryRegistry
import java.nio.file.Path

sealed class ResolvedSymbol {
    abstract val flatName: String

    data class Library(val lib: JulLibrary) : ResolvedSymbol() {
        override val flatName: String = lib.julName
    }

    data class FunLib(val builtin: FunBuiltin) : ResolvedSymbol() {
        override val flatName: String = builtin.name
    }

    data class LocalDecl(val decl: DeclNode, val source: Path) : ResolvedSymbol() {
        override val flatName: String = decl.name()
    }

    data class ImportedDecl(val decl: DeclNode, val modulePath: String, val source: Path) : ResolvedSymbol() {
        override val flatName: String = decl.name()
    }
}

data class ImportTable(
    val shortNames: Map<String, ResolvedSymbol>,
)

fun qualifiedKey(parts: List<String>): String = parts.joinToString(".")

fun resolveQualifiedName(parts: List<String>, moduleSymbols: Map<String, ResolvedSymbol>): ResolvedSymbol? {
    if (parts.isEmpty()) return null
    val key = qualifiedKey(parts)
    moduleSymbols[key]?.let { return it }
    FunBuiltinRegistry.resolveQualified(parts)?.let { return ResolvedSymbol.FunLib(it) }
    LibraryRegistry.resolveQualified(parts)?.let { return ResolvedSymbol.Library(it) }
    return null
}

fun resolveImportTarget(
    qn: QualifiedNameNode,
    entryPath: Path,
    moduleSymbols: Map<String, ResolvedSymbol>,
): ResolvedSymbol? {
    val parts = qn.parts()
    if (parts.size < 2) return null
    resolveQualifiedName(parts, moduleSymbols)?.let { return it }
    if (LibraryRegistry.isProclibImport(parts)) {
        LibraryRegistry.resolve(JULAY_MODULE, parts[2])?.let { return ResolvedSymbol.Library(it) }
    }
    return null
}

fun buildImportTable(
    entryRoot: RootNode,
    entryPath: Path,
    moduleSymbols: Map<String, ResolvedSymbol>,
): Pair<ImportTable, List<CompileError>> {
    val shortNames = mutableMapOf<String, ResolvedSymbol>()
    val errors = mutableListOf<CompileError>()

    entryRoot.importNodes().forEach { importNode ->
        val qn = importNode.qualifiedName()
        val parts = qn.parts()
        val symbol = parts.last()
        val resolved = resolveImportTarget(qn, entryPath, moduleSymbols)
        if (resolved == null) {
            val msg = if (parts.first() == JULAY_MODULE) {
                "Unknown library ${qualifiedKey(parts)}"
            } else {
                val modulePath = parts.dropLast(1).joinToString(".")
                val symbol = parts.last()
                val modulePrefix = "$modulePath."
                val moduleHasDecls = moduleSymbols.keys.any { it.startsWith(modulePrefix) }
                if (moduleHasDecls) {
                    "Module \"${modulePath.replace('.', '/')}.jul\" has no export named \"$symbol\""
                } else {
                    "Cannot find module \"$modulePath\" (looked for ${modulePath.replace('.', '/')}.jul on module path)"
                }
            }
            errors.add(OneLocCompileError(importNode.programLocation(), msg))
            return@forEach
        }
        if (symbol in shortNames) {
            errors.add(
                OneLocCompileError(
                    importNode.programLocation(),
                    "Duplicate import for \"$symbol\"",
                ),
            )
            return@forEach
        }
        shortNames[symbol] = resolved
    }

    return ImportTable(shortNames) to errors
}

data class ResolvedProcRef(
    val flatName: String,
    val symbol: ResolvedSymbol?,
    val isLibrary: Boolean,
)

fun resolveProcLeaf(
    node: ValueProcExprNode,
    entryDeclNames: Set<String>,
    allPClassNames: Set<String>,
    allProcNames: Set<String>,
    importTable: ImportTable,
    moduleSymbols: Map<String, ResolvedSymbol>,
): Pair<ResolvedProcRef?, CompileError?> {
    if (node.isQualified()) {
        val parts = node.qualifiedParts()!!
        val resolved = resolveQualifiedName(parts, moduleSymbols)
            ?: LibraryRegistry.resolveQualified(parts)?.let { ResolvedSymbol.Library(it) }
        if (resolved == null) {
            return null to OneLocCompileError(
                node.programLocation(),
                "Unknown process \"${qualifiedKey(parts)}\"",
            )
        }
        return ResolvedProcRef(resolved.flatName, resolved, resolved is ResolvedSymbol.Library) to null
    }

    val name = node.valueProcName()
    if (name in entryDeclNames || name in allPClassNames || name in allProcNames) {
        return ResolvedProcRef(name, null, false) to null
    }
    importTable.shortNames[name]?.let { resolved ->
        return ResolvedProcRef(resolved.flatName, resolved, resolved is ResolvedSymbol.Library) to null
    }
    if (LibraryRegistry.isKnownJulaylibSymbol(name)) {
        return null to OneLocCompileError(
            node.programLocation(),
            "Unknown process \"$name\"; did you mean to import julay.proclib.$name?",
        )
    }
    return null to OneLocCompileError(
        node.programLocation(),
        "Unknown process \"$name\"",
    )
}
