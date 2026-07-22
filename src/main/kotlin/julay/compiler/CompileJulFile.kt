package julay.compiler

import julay.compiler.ast.SpecNode
import julay.compiler.pass.compileSpecToTla
import java.nio.file.Path

fun compileJulFile(
    source: Path,
    keepBuild: Boolean,
    extraLibraryPaths: List<Path> = emptyList(),
    compilerJar: Path = resolveCompilerJar(),
    allowUnindexedSpec: Boolean = false,
) {
    val checked = prepareCheckedCompilation(
        source,
        extraLibraryPaths,
        allowUnindexedSpec = allowUnindexedSpec,
    ) ?: return
    val (unit, ast, procDecls, jarTargets, specTargets, librariesInUse) = checked

    if (jarTargets.isEmpty() && specTargets.isEmpty()) {
        println("No procs or specs to compile.")
        return
    }

    for (program in jarTargets) {
        val components = program.allProcNames(procDecls)
        if (!runErrorAndWarningPasses(ast, components, librariesInUse, program.name)) {
            return
        }
    }

    jarTargets.forEach {
        compileProgram(it, ast, procDecls, librariesInUse, keepBuild, compilerJar)
    }

    val specNodes = ast.declNodes().filterIsInstance<SpecNode>().associateBy { it.name() }
    for (specDecl in specTargets) {
        val specNode = specNodes[specDecl.name]
        if (specNode == null) {
            println("Internal error: missing SpecNode for \"${specDecl.name}\"")
            return
        }
        // Specs intentionally allow unshared actions (assumption/system roles);
        // skip JAR-oriented sync errorPass. Typechecking already ran in prepareCheckedCompilation.
        compileSpecToTla(specNode, ast, unit)
    }
}
