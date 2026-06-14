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
import kotlin.io.path.isRegularFile
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.pathString

private data class ModuleFileLookup(val path: Path, val found: Boolean)

private data class ParseResult(val root: RootNode, val ok: Boolean)

fun loadCompilationUnit(entryPath: Path, extraLibraryPaths: List<Path> = emptyList()): Pair<CompilationUnit, List<CompileError>> {
    val searchPath = buildModuleSearchPath(entryPath, extraLibraryPaths)
    val errors = mutableListOf<CompileError>()
    val loaded = mutableMapOf<String, LoadedModule>()
    val loadingStack = mutableListOf<String>()

    fun moduleFileName(modulePath: String): String = modulePath.replace('.', '/') + ".jul"

    fun findModuleFile(modulePath: String): ModuleFileLookup {
        val relPath = moduleFileName(modulePath)
        for (base in searchPath) {
            val candidate = base.resolve(relPath)
            if (candidate.isRegularFile()) {
                return ModuleFileLookup(candidate, found = true)
            }
        }
        return ModuleFileLookup(entryPath, found = false)
    }

    fun parseFile(path: Path): ParseResult {
        val input = CharStreams.fromFileName(path.pathString)
        val lexer = JulayLexer(input)
        val tokens = CommonTokenStream(lexer)
        val parser = JulayParser(tokens)
        val parseRoot = parser.root()
        if (parser.numberOfSyntaxErrors > 0) {
            errors.add(OneLocCompileError(SourceLoc(Pair(1, 1)), "Syntax errors in ${path.fileName}"))
            return ParseResult(emptyRootNode(), ok = false)
        }
        return ParseResult(ASTBuilder().visit(parseRoot) as RootNode, ok = true)
    }

    fun registerModuleSymbols(module: LoadedModule, symbols: MutableMap<String, ResolvedSymbol>) {
        if (module.isStub) return
        module.root.declNodes().forEach { decl ->
            val key = qualifiedKey(listOf(module.modulePath, decl.name()))
            symbols[key] = if (module.isEntry) {
                ResolvedSymbol.LocalDecl(decl, module.sourcePath)
            } else {
                ResolvedSymbol.ImportedDecl(decl, module.modulePath, module.sourcePath)
            }
        }
        LibraryRegistry.all.forEach { lib ->
            symbols[qualifiedKey(listOf(JULAYLIB_MODULE, lib.julName))] = ResolvedSymbol.Library(lib)
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
            errors.add(OneLocCompileError(SourceLoc(Pair(1, 1)), "Circular import: $cycle"))
            return cacheStub(modulePath, entryPath, isEntry)
        }

        val sourcePath = if (isEntry) {
            entryPath
        } else {
            val lookup = findModuleFile(modulePath)
            if (!lookup.found) {
                errors.add(
                    OneLocCompileError(
                        SourceLoc(Pair(1, 1)),
                        "Cannot find module \"$modulePath\" (looked for ${moduleFileName(modulePath)} on module path)",
                    ),
                )
                return cacheStub(modulePath, entryPath, isEntry)
            }
            lookup.path
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
            if (parts.first() != JULAYLIB_MODULE && parts.size >= 2) {
                val importedModule = parts.dropLast(1).joinToString(".")
                loadModule(importedModule, isEntry = false)
            }
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
