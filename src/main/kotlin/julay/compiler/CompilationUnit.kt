package julay.compiler

import julay.compiler.ast.*
import julay.compiler.decl.ProcDecl
import julay.compiler.decl.ProcDeclType
import julay.program.library.LibraryRegistry
import java.nio.file.Path

data class LoadedModule(
    val modulePath: String,
    val sourcePath: Path,
    val root: RootNode,
    val isEntry: Boolean,
    val importTable: ImportTable,
    val isStub: Boolean = false,
)

data class CompilationUnit(
    val entryPath: Path,
    val root: RootNode,
    val modules: List<LoadedModule>,
    val importTable: ImportTable,
    val moduleSymbols: Map<String, ResolvedSymbol>,
    val entryDeclNames: Set<String>,
    val allPClassNames: Set<String>,
    val allProcNames: Set<String>,
) {
    fun isLibraryInUse(flatName: String): Boolean =
        importTable.shortNames.values.any { it is ResolvedSymbol.Library && it.flatName == flatName } ||
            moduleSymbols.values.any { it is ResolvedSymbol.Library && it.flatName == flatName }

    fun librariesInUse(procDecls: List<ProcDecl>): Set<String> {
        val programLibs = procDecls
            .filter { it.type == ProcDeclType.Program }
            .flatMap { it.allProcNames(procDecls) }
            .filter { LibraryRegistry.isKotlinLibrary(it) }
            .toSet()
        val importedLibs = importTable.shortNames.values
            .filterIsInstance<ResolvedSymbol.Library>()
            .map { it.flatName }
            .toSet()
        return programLibs + importedLibs
    }
}

fun emptyRootNode(): RootNode = RootNode(emptyList(), emptyList(), SourceLoc(Pair(1, 1)))

fun stubLoadedModule(modulePath: String, sourcePath: Path, isEntry: Boolean): LoadedModule =
    LoadedModule(
        modulePath = modulePath,
        sourcePath = sourcePath,
        root = emptyRootNode(),
        isEntry = isEntry,
        importTable = ImportTable(emptyMap()),
        isStub = true,
    )

fun emptyCompilationUnit(entryPath: Path): CompilationUnit {
    val emptyRoot = emptyRootNode()
    return CompilationUnit(
        entryPath = entryPath,
        root = emptyRoot,
        modules = emptyList(),
        importTable = ImportTable(emptyMap()),
        moduleSymbols = emptyMap(),
        entryDeclNames = emptySet(),
        allPClassNames = emptySet(),
        allProcNames = emptySet(),
    )
}

fun collectDeclNames(root: RootNode): Set<String> =
    root.declNodes().filter { it !is FunNode }.map { it.name() }.toSet()

fun collectPClassNames(root: RootNode): Set<String> =
    root.declNodes().filterIsInstance<ProcClassNode>().map { it.name() }.toSet()

fun collectProcAliasNames(root: RootNode): Set<String> =
    root.declNodes()
        .filter { it is ProcNode || it is SpecNode }
        .map { it.name() }
        .toSet()

fun declFromResolvedSymbol(symbol: ResolvedSymbol): DeclNode? = when (symbol) {
    is ResolvedSymbol.LocalDecl -> symbol.decl
    is ResolvedSymbol.ImportedDecl -> symbol.decl
    is ResolvedSymbol.Library -> null
    is ResolvedSymbol.FunLib -> null
}

fun callableFuns(module: LoadedModule): Map<String, FunNode> {
    val local = module.root.declNodes().filterIsInstance<FunNode>().associateBy { it.name() }
    val imported = module.importTable.shortNames.mapNotNull { (name, symbol) ->
        val decl = declFromResolvedSymbol(symbol)
        if (decl is FunNode) name to decl else null
    }.toMap()
    return local + imported
}

fun callableFunBuiltins(module: LoadedModule): Map<String, FunBuiltin> =
    module.importTable.shortNames.mapNotNull { (name, symbol) ->
        if (symbol is ResolvedSymbol.FunLib) name to symbol.builtin else null
    }.toMap()

