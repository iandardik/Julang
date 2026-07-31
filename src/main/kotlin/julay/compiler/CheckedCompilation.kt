package julay.compiler

import julay.compiler.ast.ASTNode
import julay.compiler.ast.CompileNode
import julay.compiler.ast.RootNode
import julay.compiler.collectProcFunNames
import julay.compiler.decl.ProcDecl
import julay.compiler.decl.ProcDeclType
import julay.compiler.pass.errorPass
import julay.compiler.pass.resolvedProcPass
import julay.compiler.pass.typePass
import julay.compiler.pass.warningPass
import java.nio.file.Path

/**
 * Compilation unit after successful load, type-check, and structural setup.
 * Callers still run error/warning passes appropriate to compile vs analyze.
 */
data class CheckedCompilation(
    val unit: CompilationUnit,
    val ast: RootNode,
    val procDecls: List<ProcDecl>,
    /** JAR roots named by `compile` directives (proc aliases or leaf proc classes). */
    val jarTargets: List<ProcDecl>,
    /** Specs named by `compile` directives. */
    val specTargets: List<ProcDecl>,
    /**
     * Procs named by `--compile-tla`: emit TLA+ as a plain system (equivalent to
     * `<true> P <true>` — no assumption, no guarantee invariants).
     */
    val tlaProcTargets: List<ProcDecl>,
    val librariesInUse: Set<String>,
)

/**
 * Resolve compile target names into JAR, spec, and standalone-procfun TLA targets.
 * When [overrideNames] is non-empty, those names are used and source `compile`
 * directives are ignored; otherwise names come from `compile` directives.
 * Returns null if any name is unknown (errors already printed).
 */
data class ResolvedCompileTargets(
    val jars: List<ProcDecl>,
    val specs: List<ProcDecl>,
    /** Procfuns named by `compile` — emit standalone TLA (not JAR). */
    val procFunTla: List<ProcDecl>,
)

fun resolveCompileTargets(
    ast: RootNode,
    unit: CompilationUnit,
    procDecls: List<ProcDecl>,
    overrideNames: List<String> = emptyList(),
): ResolvedCompileTargets? {
    val names = if (overrideNames.isNotEmpty()) {
        overrideNames.distinct()
    } else {
        ast.declNodes()
            .filterIsInstance<CompileNode>()
            .flatMap { it.compileNames() }
            .distinct()
    }
    val byName = procDecls.associateBy { it.name }
    val jars = mutableListOf<ProcDecl>()
    val specs = mutableListOf<ProcDecl>()
    val procFunTla = mutableListOf<ProcDecl>()
    val procFunNames = collectProcFunNames(ast)
    val missing = mutableListOf<String>()
    for (name in names) {
        if (name in procFunNames) {
            // Standalone TLA/analyze of the procfun — not a JAR root.
            procFunTla += ProcDecl(name, emptyList(), ProcDeclType.Proc)
            continue
        }
        val decl = byName[name]
        when {
            decl?.type == ProcDeclType.Spec -> specs += decl
            decl?.type == ProcDeclType.Proc -> jars += decl
            name in unit.allPClassNames -> jars += ProcDecl(name, emptyList(), ProcDeclType.Proc)
            else -> missing += name
        }
    }
    if (missing.isNotEmpty()) {
        println("Unknown compile name(s): ${missing.joinToString(", ")}")
        return null
    }
    return ResolvedCompileTargets(jars, specs, procFunTla)
}

/**
 * Resolve `--compile-tla` names to procs (aliases or leaf proc classes).
 * Specs are rejected. Returns null if any name is invalid (errors already printed).
 */
fun resolveCompileTlaTargets(
    unit: CompilationUnit,
    procDecls: List<ProcDecl>,
    names: List<String>,
    procFunNames: Set<String> = emptySet(),
): List<ProcDecl>? {
    if (names.isEmpty()) return emptyList()
    val byName = procDecls.associateBy { it.name }
    val procs = mutableListOf<ProcDecl>()
    val missing = mutableListOf<String>()
    val notProcs = mutableListOf<String>()
    for (name in names.distinct()) {
        when {
            name in procFunNames -> procs += ProcDecl(name, emptyList(), ProcDeclType.Proc)
            byName[name]?.type == ProcDeclType.Proc -> procs += byName.getValue(name)
            byName[name]?.type == ProcDeclType.Spec -> notProcs += name
            name in unit.allPClassNames -> procs += ProcDecl(name, emptyList(), ProcDeclType.Proc)
            else -> missing += name
        }
    }
    if (notProcs.isNotEmpty()) {
        println("--compile-tla expects a proc, not a spec: ${notProcs.joinToString(", ")}")
        return null
    }
    if (missing.isNotEmpty()) {
        println("Unknown --compile-tla name(s): ${missing.joinToString(", ")}")
        return null
    }
    return procs
}

/**
 * Load, resolve procs, and type-check. Returns null if load or type errors were printed.
 * Spec indexing warnings are printed to stderr even on success.
 */
fun prepareCheckedCompilation(
    source: Path,
    extraLibraryPaths: List<Path> = emptyList(),
    allowUnindexedSpec: Boolean = false,
    compileNames: List<String> = emptyList(),
    compileTlaNames: List<String> = emptyList(),
): CheckedCompilation? {
    val (unit, loadErrors) = loadCompilationUnit(source, extraLibraryPaths)
    if (loadErrors.isNotEmpty()) {
        loadErrors.forEach { println(it) }
        println("Found compile errors, exiting.")
        return null
    }

    val ast = unit.root
    val procDecls = ast.resolvedProcPass(unit)

    val typeResult = ast.typePass(unit, allowUnindexedSpec)
    typeResult.warnings.forEach { System.err.println(it) }
    if (typeResult.errors.isNotEmpty()) {
        typeResult.errors.forEach { println(it) }
        println("Found type errors; exiting.")
        return null
    }

    val targets = resolveCompileTargets(ast, unit, procDecls, compileNames) ?: run {
        println("Found compile errors, exiting.")
        return null
    }
    val jarTargets = targets.jars
    val specTargets = targets.specs
    val fromCompileTla = resolveCompileTlaTargets(
        unit, procDecls, compileTlaNames, collectProcFunNames(ast),
    ) ?: run {
        println("Found compile errors, exiting.")
        return null
    }
    val tlaProcTargets = (fromCompileTla + targets.procFunTla).distinctBy { it.name }

    return CheckedCompilation(
        unit = unit,
        ast = ast,
        procDecls = procDecls,
        jarTargets = jarTargets,
        specTargets = specTargets,
        tlaProcTargets = tlaProcTargets,
        librariesInUse = unit.librariesInUse(jarTargets, procDecls),
    )
}

fun runErrorAndWarningPasses(
    ast: ASTNode,
    components: Set<String>,
    librariesInUse: Set<String>,
    programName: String? = null,
    program: ProcDecl? = null,
    procDecls: List<ProcDecl> = emptyList(),
    /**
     * When false (analyze / check of intermediate assemblies), unmatched client /
     * ordinary / session sync is not required. True only for the top-level `compile` target.
     */
    requireCompleteSync: Boolean = true,
): Boolean {
    val errors = ast.errorPass(
        components,
        librariesInUse,
        program = program,
        procDecls = procDecls,
        requireCompleteSync = requireCompleteSync,
    )
    if (errors.isNotEmpty()) {
        errors.forEach { println(it) }
        if (programName != null) {
            println("Found errors while compiling \"$programName\"; exiting.")
        } else {
            println("Found errors; exiting.")
        }
        return false
    }
    ast.warningPass(
        components,
        librariesInUse,
        program,
        procDecls,
        requireCompleteSync = requireCompleteSync,
    ).forEach { System.err.println(it) }
    return true
}
