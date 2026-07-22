package julay.compiler

import julay.compiler.ast.ASTNode
import julay.compiler.ast.CompileNode
import julay.compiler.ast.RootNode
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
    val librariesInUse: Set<String>,
)

/**
 * Resolve the union of all `compile` directive names into JAR and TLA targets.
 * Returns null if any name is unknown (errors already printed).
 */
fun resolveCompileTargets(
    ast: RootNode,
    unit: CompilationUnit,
    procDecls: List<ProcDecl>,
): Pair<List<ProcDecl>, List<ProcDecl>>? {
    val names = ast.declNodes()
        .filterIsInstance<CompileNode>()
        .flatMap { it.compileNames() }
        .distinct()
    val byName = procDecls.associateBy { it.name }
    val jars = mutableListOf<ProcDecl>()
    val specs = mutableListOf<ProcDecl>()
    val missing = mutableListOf<String>()
    for (name in names) {
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
    return jars to specs
}

/**
 * Load, resolve procs, and type-check. Returns null if load or type errors were printed.
 * Spec indexing warnings are printed to stderr even on success.
 */
fun prepareCheckedCompilation(
    source: Path,
    extraLibraryPaths: List<Path> = emptyList(),
    allowUnindexedSpec: Boolean = false,
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

    val targets = resolveCompileTargets(ast, unit, procDecls) ?: run {
        println("Found compile errors, exiting.")
        return null
    }
    val (jarTargets, specTargets) = targets

    return CheckedCompilation(
        unit = unit,
        ast = ast,
        procDecls = procDecls,
        jarTargets = jarTargets,
        specTargets = specTargets,
        librariesInUse = unit.librariesInUse(jarTargets, procDecls),
    )
}

fun runErrorAndWarningPasses(
    ast: ASTNode,
    components: Set<String>,
    librariesInUse: Set<String>,
    programName: String? = null,
): Boolean {
    val errors = ast.errorPass(components, librariesInUse)
    if (errors.isNotEmpty()) {
        errors.forEach { println(it) }
        if (programName != null) {
            println("Found errors while compiling \"$programName\"; exiting.")
        } else {
            println("Found errors; exiting.")
        }
        return false
    }
    ast.warningPass(components, librariesInUse).forEach { System.err.println(it) }
    return true
}
