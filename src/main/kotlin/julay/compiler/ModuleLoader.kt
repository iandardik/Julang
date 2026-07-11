package julay.compiler

import julay.compiler.ast.*
import julay.compiler.pass.validateProcExprsInModule
import julay.parser.JulayLexer
import julay.parser.JulayParser
import julay.program.library.JULAYLIB_MODULE
import julay.program.library.LibraryRegistry
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import java.nio.file.Path
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.pathString

private data class ParseResult(val root: RootNode, val ok: Boolean)

fun loadCompilationUnit(entryPath: Path, extraLibraryPaths: List<Path> = emptyList()): Pair<CompilationUnit, List<CompileError>> {
    val searchPath = buildModuleSearchPath(entryPath, extraLibraryPaths)
    val errors = mutableListOf<CompileError>()
    val loaded = mutableMapOf<String, LoadedModule>()
    val loadingStack = mutableListOf<String>()

    fun parseFile(path: Path): ParseResult {
        val input = CharStreams.fromFileName(path.pathString)
        val lexer = JulayLexer(input)
        val tokens = CommonTokenStream(lexer)
        val parser = JulayParser(tokens)
        val parseRoot = parser.root()
        if (parser.numberOfSyntaxErrors > 0) {
            errors.add(OneLocCompileError(SourceLoc(Pair(1, 1), path), "Syntax errors in ${path.fileName}"))
            return ParseResult(emptyRootNode(path), ok = false)
        }
        return ParseResult(ASTBuilder(path).visit(parseRoot) as RootNode, ok = true)
    }

    fun registerModuleSymbols(module: LoadedModule, symbols: MutableMap<String, ResolvedSymbol>) {
        if (module.isStub) return
        module.root.declNodes().forEach { decl ->
            val key = if (module.modulePath.startsWith("$JULAYLIB_MODULE.")) {
                module.modulePath
            } else {
                qualifiedKey(listOf(module.modulePath, decl.name()))
            }
            symbols[key] = if (module.isEntry) {
                ResolvedSymbol.LocalDecl(decl, module.sourcePath)
            } else {
                ResolvedSymbol.ImportedDecl(decl, module.modulePath, module.sourcePath)
            }
        }
        LibraryRegistry.kotlinLibraries.forEach { lib ->
            symbols[LibraryRegistry.pclassModulePath(lib.julName)] = ResolvedSymbol.Library(lib)
        }
        FunBuiltinRegistry.all.forEach { builtin ->
            symbols[qualifiedKey(listOf(JULAYLIB_MODULE, "fun", builtin.name))] = ResolvedSymbol.FunLib(builtin)
        }
    }

    fun cacheStub(modulePath: String, sourcePath: Path, isEntry: Boolean): LoadedModule {
        val stub = stubLoadedModule(modulePath, sourcePath, isEntry)
        loaded[modulePath] = stub
        return stub
    }

    fun loadModule(modulePath: String, isEntry: Boolean): LoadedModule {
        if (modulePath in loaded) return loaded.getValue(modulePath)
        if (modulePath in loadingStack) {
            val cycle = (loadingStack + modulePath).joinToString(" -> ") { "$it.jul" }
            errors.add(OneLocCompileError(SourceLoc(Pair(1, 1), entryPath), "Circular import: $cycle"))
            return cacheStub(modulePath, entryPath, isEntry)
        }

        val sourcePath = if (isEntry) {
            entryPath
        } else {
            resolveModuleSourcePath(modulePath, searchPath) ?: run {
                errors.add(
                    OneLocCompileError(
                        SourceLoc(Pair(1, 1), entryPath),
                        "Cannot find module \"$modulePath\" (looked for ${moduleFileName(modulePath)} on module path)",
                    ),
                )
                return cacheStub(modulePath, entryPath, isEntry)
            }
        }

        loadingStack.add(modulePath)

        val parseResult = parseFile(sourcePath)
        if (!parseResult.ok) {
            loadingStack.removeLast()
            return cacheStub(modulePath, sourcePath, isEntry)
        }
        val root = parseResult.root

        if (!isEntry) {
            root.declNodes().filterIsInstance<ProgramNode>().forEach { program ->
                errors.add(
                    OneLocCompileError(
                        program.programLocation(),
                        "Programs are only allowed in the entry file",
                    ),
                )
            }
        }

        root.importNodes().forEach { importNode ->
            val parts = importNode.qualifiedName().parts()
            if (parts.size >= 2) {
                if (FunBuiltinRegistry.resolveQualified(parts) != null) {
                    return@forEach
                }
                if (LibraryRegistry.isPclassImport(parts) && LibraryRegistry.isKotlinLibrary(parts[2])) {
                    return@forEach
                }
                val importedModule = if (parts.first() == JULAYLIB_MODULE) {
                    parts.joinToString(".")
                } else {
                    parts.dropLast(1).joinToString(".")
                }
                loadModule(importedModule, isEntry = false)
            }
        }

        collectJulaylibStdlibModulePaths(root).forEach { modulePath ->
            loadModule(modulePath, isEntry = false)
        }

        val moduleSymbolsSnapshot = mutableMapOf<String, ResolvedSymbol>()
        loaded.values.forEach { registerModuleSymbols(it, moduleSymbolsSnapshot) }
        val (moduleImportTable, importErrors) = buildImportTable(root, sourcePath, moduleSymbolsSnapshot)
        errors.addAll(importErrors)

        val moduleDeclNames = collectDeclNames(root)
        val modulePClassNames = collectPClassNames(root)
        val moduleProcNames = collectProcAliasNames(root)
        errors.addAll(
            validateProcExprsInModule(
                root,
                moduleDeclNames,
                modulePClassNames,
                moduleProcNames,
                moduleImportTable,
                moduleSymbolsSnapshot,
            ),
        )

        val module = LoadedModule(modulePath, sourcePath, root, isEntry, moduleImportTable)
        loaded[modulePath] = module
        loadingStack.removeLast()
        return module
    }

    val entryModulePath = entryPath.nameWithoutExtension
    val entryModule = loadModule(entryModulePath, isEntry = true)
    if (entryModule.isStub) {
        return emptyCompilationUnit(entryPath) to errors
    }

    val moduleSymbols = mutableMapOf<String, ResolvedSymbol>()
    loaded.values.forEach { registerModuleSymbols(it, moduleSymbols) }

    val allDecls = loaded.values.filter { !it.isStub }.flatMap { it.root.declNodes() }
    val mergedRoot = entryModule.root.withImportsAndDecls(entryModule.root.importNodes(), allDecls)

    val entryDeclNames = collectDeclNames(entryModule.root)
    val allPClassNames = collectPClassNames(mergedRoot)
    val allProcNames = collectProcAliasNames(mergedRoot)

    return CompilationUnit(
        entryPath = entryPath,
        root = mergedRoot,
        modules = loaded.values.filter { !it.isStub }.toList(),
        importTable = entryModule.importTable,
        moduleSymbols = moduleSymbols,
        entryDeclNames = entryDeclNames,
        allPClassNames = allPClassNames,
        allProcNames = allProcNames,
    ) to errors
}

fun buildModuleSearchPath(entryPath: Path, extraLibraryPaths: List<Path>): List<Path> {
    val paths = mutableListOf<Path>()
    entryPath.parent?.let { paths.add(it) }
    extraLibraryPaths.forEach { paths.add(it) }
    System.getenv("JULAY_PATH")?.split(':')?.filter { it.isNotEmpty() }?.forEach { paths.add(Path.of(it)) }
    return paths.distinct()
}

fun collectJulaylibStdlibModulePaths(node: ASTNode): Set<String> {
    val paths = mutableSetOf<String>()
    fun walk(n: ASTNode) {
        if (n is ValueProcExprNode && n.isQualified()) {
            val parts = n.qualifiedParts()!!
            if (LibraryRegistry.isPclassImport(parts) &&
                LibraryRegistry.isJulayStdlib(parts[2])
            ) {
                paths.add(parts.joinToString("."))
            }
        }
        n.children.forEach { walk(it) }
    }
    walk(node)
    return paths
}
