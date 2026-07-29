package julay.compiler

import julay.compiler.ast.*
import julay.compiler.decl.ProcDecl
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

    fun librariesInUse(jarRoots: List<ProcDecl>, procDecls: List<ProcDecl>): Set<String> {
        val programLibs = jarRoots
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

fun emptyRootNode(sourcePath: Path? = null): RootNode =
    RootNode(emptyList(), emptyList(), SourceLoc(Pair(1, 1), sourcePath))

fun stubLoadedModule(modulePath: String, sourcePath: Path, isEntry: Boolean): LoadedModule =
    LoadedModule(
        modulePath = modulePath,
        sourcePath = sourcePath,
        root = emptyRootNode(sourcePath),
        isEntry = isEntry,
        importTable = ImportTable(emptyMap()),
        isStub = true,
    )

fun emptyCompilationUnit(entryPath: Path): CompilationUnit {
    val emptyRoot = emptyRootNode(entryPath)
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
    root.declNodes()
        .filter { it !is FunNode && it !is ProcFunNode && it !is CompileNode }
        .map { it.name() }
        .toSet()

fun collectPClassNames(root: RootNode): Set<String> =
    root.declNodes().filterIsInstance<ProcClassNode>().map { it.name() }.toSet()

fun collectProcAliasNames(root: RootNode): Set<String> =
    root.declNodes()
        .filter { it is ProcNode || it is SpecNode || it is ApiNode }
        .map { it.name() }
        .toSet()

fun collectApiNames(root: RootNode): Set<String> =
    root.declNodes().filterIsInstance<ApiNode>().map { it.name() }.toSet()

fun collectProcFunNames(root: RootNode): Set<String> =
    root.declNodes().filterIsInstance<ProcFunNode>().map { it.name() }.toSet()

/** Map api name → listed call (procfun) names. */
fun collectApiCalls(root: RootNode): Map<String, List<String>> =
    root.declNodes().filterIsInstance<ApiNode>().associate { it.apiName() to it.apiCallNames() }

fun callableApis(module: LoadedModule): Map<String, ApiNode> {
    val local = module.root.declNodes().filterIsInstance<ApiNode>().associateBy { it.name() }
    val imported = module.importTable.shortNames.mapNotNull { (name, symbol) ->
        val decl = declFromResolvedSymbol(symbol)
        if (decl is ApiNode) name to decl else null
    }.toMap()
    return local + imported
}
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

fun callableProcFuns(module: LoadedModule): Map<String, ProcFunNode> {
    val local = module.root.declNodes().filterIsInstance<ProcFunNode>().associateBy { it.name() }
    val imported = module.importTable.shortNames.mapNotNull { (name, symbol) ->
        val decl = declFromResolvedSymbol(symbol)
        if (decl is ProcFunNode) name to decl else null
    }.toMap()
    return local + imported
}

fun callableFunBuiltins(module: LoadedModule): Map<String, FunBuiltin> =
    module.importTable.shortNames.mapNotNull { (name, symbol) ->
        if (symbol is ResolvedSymbol.FunLib) name to symbol.builtin else null
    }.toMap()

