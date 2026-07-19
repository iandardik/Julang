package julay.compiler

import julay.compiler.ast.SpecNode
import julay.compiler.decl.ProcDecl
import julay.compiler.decl.ProcDeclType
import julay.compiler.pass.compileSpecToTla
import java.nio.file.Path

/**
 * @param programNames null = all programs (when [compilePrograms]); otherwise the named subset.
 * @param specNames null = all specs (when [compileSpecs]); otherwise the named subset.
 */
data class CompileTargets(
    val compilePrograms: Boolean = true,
    val programNames: Set<String>? = null,
    val compileSpecs: Boolean = true,
    val specNames: Set<String>? = null,
)

fun compileJulFile(
    source: Path,
    keepBuild: Boolean,
    extraLibraryPaths: List<Path> = emptyList(),
    compilerJar: Path = resolveCompilerJar(),
    targets: CompileTargets = CompileTargets(),
) {
    val checked = prepareCheckedCompilation(source, extraLibraryPaths) ?: return
    val (unit, ast, procDecls, programs, librariesInUse) = checked

    val specs = procDecls.filter { it.type == ProcDeclType.Spec }

    val selectedPrograms = if (targets.compilePrograms) {
        filterByName(programs, targets.programNames, "program") ?: return
    } else {
        emptyList()
    }
    val selectedSpecs = if (targets.compileSpecs) {
        filterByName(specs, targets.specNames, "spec") ?: return
    } else {
        emptyList()
    }

    if (selectedPrograms.isEmpty() && selectedSpecs.isEmpty()) {
        println("No programs or specs to compile.")
        return
    }

    for (program in selectedPrograms) {
        val components = program.allProcNames(procDecls)
        if (!runErrorAndWarningPasses(ast, components, librariesInUse, program.name)) {
            return
        }
    }

    selectedPrograms.forEach {
        compileProgram(it, ast, procDecls, librariesInUse, keepBuild, compilerJar)
    }

    val specNodes = ast.declNodes().filterIsInstance<SpecNode>().associateBy { it.name() }
    for (specDecl in selectedSpecs) {
        val specNode = specNodes[specDecl.name]
        if (specNode == null) {
            println("Internal error: missing SpecNode for \"${specDecl.name}\"")
            return
        }
        // Specs intentionally allow unshared actions (assumption/system roles);
        // skip program-oriented sync errorPass. Typechecking already ran in prepareCheckedCompilation.
        compileSpecToTla(specNode, ast, unit)
    }
}

private fun filterByName(
    all: List<ProcDecl>,
    filter: Set<String>?,
    kind: String,
): List<ProcDecl>? {
    if (filter == null) return all
    val byName = all.associateBy { it.name }
    val missing = filter.filter { it !in byName }
    if (missing.isNotEmpty()) {
        println("Unknown $kind name(s): ${missing.joinToString(", ")}")
        return null
    }
    return filter.map { byName.getValue(it) }
}
