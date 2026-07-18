package julay.compiler

import julay.compiler.ast.ASTNode
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
    val programs: List<ProcDecl>,
    val librariesInUse: Set<String>,
)

/**
 * Load, resolve procs, and type-check. Returns null if load or type errors were printed.
 */
fun prepareCheckedCompilation(
    source: Path,
    extraLibraryPaths: List<Path> = emptyList(),
): CheckedCompilation? {
    val (unit, loadErrors) = loadCompilationUnit(source, extraLibraryPaths)
    if (loadErrors.isNotEmpty()) {
        loadErrors.forEach { println(it) }
        println("Found compile errors, exiting.")
        return null
    }

    val ast = unit.root
    val procDecls = ast.resolvedProcPass(unit)
    val programs = procDecls.filter { it.type == ProcDeclType.Program }

    val typeErrors = ast.typePass(unit)
    if (typeErrors.isNotEmpty()) {
        typeErrors.forEach { println(it) }
        println("Found type errors; exiting.")
        return null
    }

    return CheckedCompilation(
        unit = unit,
        ast = ast,
        procDecls = procDecls,
        programs = programs,
        librariesInUse = unit.librariesInUse(procDecls),
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
            println("Found errors while compiling the program \"$programName\"; exiting.")
        } else {
            println("Found errors; exiting.")
        }
        return false
    }
    ast.warningPass(components, librariesInUse).forEach { System.err.println(it) }
    return true
}
