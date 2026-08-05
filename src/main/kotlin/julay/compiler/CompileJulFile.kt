package julay.compiler

import julay.compiler.ast.SpecNode
import julay.compiler.ast.ValueProcExprNode
import julay.compiler.pass.compileSpecToTla
import julay.compiler.pass.procFunCompositionErrors
import julay.compiler.pass.procFunHavocWarnings
import julay.program.sync.SyncResolveConfig
import java.nio.file.Path

fun compileJulFile(
    source: Path,
    keepBuild: Boolean,
    extraLibraryPaths: List<Path> = emptyList(),
    compilerJar: Path = resolveCompilerJar(),
    allowUnindexedSpec: Boolean = false,
    compileNames: List<String> = emptyList(),
    compileTlaNames: List<String> = emptyList(),
    syncResolveConfig: SyncResolveConfig = SyncResolveConfig.ALL_ON,
    verbose: Boolean = false,
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

    var syncPathStats = SyncPathStats.EMPTY
    jarTargets.forEach {
        val stats = compileProgram(
            it, ast, procDecls, librariesInUse, keepBuild, compilerJar,
            syncResolveConfig = syncResolveConfig,
        )
        syncPathStats += stats
    }

    if (verbose && jarTargets.isNotEmpty()) {
        print(syncPathStats.formatSummary(syncResolveConfig))
    }

    val specNodes = ast.declNodes().filterIsInstance<SpecNode>().associateBy { it.name() }
    for (specDecl in specTargets) {
        val specNode = specNodes[specDecl.name]
        if (specNode == null) {
            println("Internal error: missing SpecNode for \"${specDecl.name}\"")
            return
        }
        // Specs allow unilateral assume/system actions; only shared alphabet-integrity
        // checks (duplicate external / dual provider) must match JAR.
        if (!runSpecAlphabetIntegrityPass(ast, specDecl, procDecls, librariesInUse)) {
            return
        }
        compileSpecToTla(specNode, ast, unit)
    }

    for (proc in tlaProcTargets) {
        if (!runSpecAlphabetIntegrityPass(ast, proc, procDecls, librariesInUse)) {
            return
        }
        // Synthetic plain-system spec: equivalent to <true> P <true> (no assume, no guarantee).
        compileSpecToTla(syntheticProcSpec(proc.name), ast, unit)
    }
}

/** Alphabet integrity shared by JAR and TLA+ (not full JAR peer-count / unsynced checks). */
private fun runSpecAlphabetIntegrityPass(
    ast: julay.compiler.ast.RootNode,
    program: julay.compiler.decl.ProcDecl,
    procDecls: List<julay.compiler.decl.ProcDecl>,
    librariesInUse: Set<String>,
): Boolean {
    val compositionErrors = ast.procFunCompositionErrors(procDecls)
    if (compositionErrors.isNotEmpty()) {
        compositionErrors.forEach { System.err.println(it) }
        System.err.println("Found errors while compiling \"${program.name}\"; exiting.")
        return false
    }
    ast.procFunHavocWarnings(program, procDecls).forEach { System.err.println(it) }
    val components = program.allProcNames(procDecls)
    val leafMap = julay.compiler.pass.leafActionMap(ast, components, librariesInUse)
    val alphabet = julay.compiler.pass.computeCompositionAlphabet(
        program, procDecls, leafMap, julay.compiler.collectProcFunNames(ast), ast,
    )
    val errors = alphabet.errors + julay.compiler.pass.alphabetIntegrityErrors(
        alphabet,
        julay.compiler.collectProcFunNames(ast),
    )
    if (errors.isEmpty()) return true
    errors.forEach { System.err.println(it) }
    System.err.println("Found errors while compiling \"${program.name}\"; exiting.")
    return false
}

/** Plain `spec Name := Name` — TLA with no assumption and no guarantee invariants. */
internal fun syntheticProcSpec(procName: String): SpecNode {
    val loc = SourceLoc(0 to 0)
    return SpecNode(procName, ValueProcExprNode(procName, null, loc), loc)
}
