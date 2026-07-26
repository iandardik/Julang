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
    val (unit, ast, procDecls, jarTargets, _, _, librariesInUse) = checked

    if (jarTargets.isEmpty()) {
        val components = unit.allPClassNames + librariesInUse
        if (!runErrorAndWarningPasses(ast, components, librariesInUse)) {
            return
        }
    } else {
        for (program in jarTargets) {
            val components = program.allProcNames(procDecls)
            if (!runErrorAndWarningPasses(
                    ast,
                    components,
                    librariesInUse,
                    program.name,
                    program = program,
                    procDecls = procDecls,
                )
            ) {
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
        procFunNames = collectProcFunNames(ast),
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
