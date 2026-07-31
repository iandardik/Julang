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
    val (unit, ast, procDecls, jarTargets, _, _, _) = checked
    val checkRoots = jarTargets.ifEmpty {
        maximalCompositionRoots(procDecls, unit.entryDeclNames)
    }
    val librariesInUse = unit.librariesInUse(checkRoots, procDecls)
    // Alphabet viewing must not hard-fail (or warn) on incomplete client / ordinary /
    // session sync in intermediate assemblies; that applies only to `compile` targets.
    val requireCompleteSync = false

    if (checkRoots.isEmpty()) {
        val components = unit.allPClassNames + librariesInUse
        if (!runErrorAndWarningPasses(
                ast,
                components,
                librariesInUse,
                requireCompleteSync = requireCompleteSync,
            )
        ) {
            return
        }
    } else {
        for (program in checkRoots) {
            val components = program.allProcNames(procDecls)
            if (!runErrorAndWarningPasses(
                    ast,
                    components,
                    librariesInUse,
                    program.name,
                    program = program,
                    procDecls = procDecls,
                    requireCompleteSync = requireCompleteSync,
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
