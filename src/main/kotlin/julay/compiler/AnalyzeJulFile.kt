package julay.compiler

import julay.compiler.analysis.AnalyzeOptions
import julay.compiler.analysis.printAnalyzeViews
import julay.compiler.analysis.resolveAnalyzeScope
import java.nio.file.Path

fun analyzeJulFile(
    source: Path,
    options: AnalyzeOptions,
    extraLibraryPaths: List<Path> = emptyList(),
) {
    val checked = prepareCheckedCompilation(source, extraLibraryPaths) ?: return
    val (unit, ast, procDecls, programs, librariesInUse) = checked

    if (programs.isEmpty()) {
        val components = unit.allPClassNames + librariesInUse
        if (!runErrorAndWarningPasses(ast, components, librariesInUse)) {
            return
        }
    } else {
        for (program in programs) {
            val components = program.allProcNames(procDecls)
            if (!runErrorAndWarningPasses(ast, components, librariesInUse, program.name)) {
                return
            }
        }
    }

    val scope = resolveAnalyzeScope(
        scopeNames = options.scopeNames,
        procDecls = procDecls,
        allPClassNames = unit.allPClassNames,
        allProcAliasNames = unit.allProcNames,
        librariesInUse = librariesInUse,
    ) ?: return

    printAnalyzeViews(
        ast = ast,
        scope = scope,
        librariesInUse = librariesInUse,
        allPClassNames = unit.allPClassNames,
        procDecls = procDecls,
        options = options,
    )
}
