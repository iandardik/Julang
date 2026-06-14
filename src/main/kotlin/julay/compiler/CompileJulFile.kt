package julay.compiler

import julay.compiler.decl.ProcDecl
import julay.compiler.decl.ProcDeclType
import julay.compiler.pass.errorPass
import julay.compiler.pass.flattenObjClassPass
import julay.compiler.pass.resolvedObjClassRegistry
import julay.compiler.pass.resolvedProcPass
import julay.compiler.pass.typePass
import java.nio.file.Path

fun compileJulFile(source: Path, keepBuild: Boolean, extraLibraryPaths: List<Path> = emptyList()) {
    val (unit, loadErrors) = loadCompilationUnit(source, extraLibraryPaths)
    if (loadErrors.isNotEmpty()) {
        loadErrors.forEach { println(it) }
        println("Found compile errors, exiting.")
        return
    }

    val ast = unit.root
    val procDecls = ast.resolvedProcPass(unit)
    val programs = procDecls.filter { it.type == ProcDeclType.Program }

    val typeErrors = ast.typePass()
    if (typeErrors.isNotEmpty()) {
        typeErrors.forEach { println(it) }
        println("Found type errors; exiting.")
        return
    }

    val librariesInUse = unit.librariesInUse(procDecls)

    programs.forEach { program ->
        val components = program.allProcNames(procDecls)
        val errors = ast.errorPass(components, librariesInUse)
        if (errors.isNotEmpty()) {
            errors.forEach { println(it) }
            println("Found errors while compiling the program \"${program.name}\"; exiting.")
            return
        }
    }

    val flatAst = ast.flattenObjClassPass(ast.resolvedObjClassRegistry())

    programs.forEach { compileProgram(it, flatAst, procDecls, librariesInUse, keepBuild) }
}
