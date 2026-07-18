package julay.compiler

import java.nio.file.Path

fun compileJulFile(
    source: Path,
    keepBuild: Boolean,
    extraLibraryPaths: List<Path> = emptyList(),
    compilerJar: Path = resolveCompilerJar(),
) {
    val checked = prepareCheckedCompilation(source, extraLibraryPaths) ?: return
    val (_, ast, procDecls, programs, librariesInUse) = checked

    if (programs.isEmpty()) {
        return
    }

    for (program in programs) {
        val components = program.allProcNames(procDecls)
        if (!runErrorAndWarningPasses(ast, components, librariesInUse, program.name)) {
            return
        }
    }

    programs.forEach { compileProgram(it, ast, procDecls, librariesInUse, keepBuild, compilerJar) }
}
