package julay.compiler

import julay.compiler.ast.SpecNode
import julay.compiler.ast.ValueProcExprNode
import julay.compiler.pass.compileSpecToTla
import java.nio.file.Path

fun compileJulFile(
    source: Path,
    keepBuild: Boolean,
    extraLibraryPaths: List<Path> = emptyList(),
    compilerJar: Path = resolveCompilerJar(),
    allowUnindexedSpec: Boolean = false,
    compileNames: List<String> = emptyList(),
    compileTlaNames: List<String> = emptyList(),
) {
    val checked = prepareCheckedCompilation(
        source,
        extraLibraryPaths,
        allowUnindexedSpec = allowUnindexedSpec,
        compileNames = compileNames,
        compileTlaNames = compileTlaNames,
    ) ?: return
    val (unit, ast, procDecls, jarTargets, specTargets, tlaProcTargets, librariesInUse) = checked

    if (jarTargets.isEmpty() && specTargets.isEmpty() && tlaProcTargets.isEmpty()) {
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

    for (proc in tlaProcTargets) {
        // Synthetic plain-system spec: equivalent to <true> P <true> (no assume, no guarantee).
        compileSpecToTla(syntheticProcSpec(proc.name), ast, unit)
    }
}

/** Plain `spec Name := Name` — TLA with no assumption and no guarantee invariants. */
internal fun syntheticProcSpec(procName: String): SpecNode {
    val loc = SourceLoc(0 to 0)
    return SpecNode(procName, ValueProcExprNode(procName, null, loc), loc)
}
