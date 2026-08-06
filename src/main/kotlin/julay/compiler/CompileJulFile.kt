package julay.compiler

import julay.compiler.ast.LeafSpecNode
import julay.compiler.ast.ParamProcExprNode
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

    val compositionSpecs = ast.declNodes().filterIsInstance<SpecNode>().associateBy { it.name() }
    val leafSpecs = ast.declNodes().filterIsInstance<LeafSpecNode>().associateBy { it.name() }
    for (specDecl in specTargets) {
        val specNode = compositionSpecs[specDecl.name]
            ?: leafSpecs[specDecl.name]?.let { syntheticLeafSpec(it) }
        if (specNode == null) {
            println("Internal error: missing SpecNode for \"${specDecl.name}\"")
            return
        }
        // TLA+ allows incomplete alphabets (unmatched clients, unilateral ordinary/session,
        // dual providers, etc.). SyncChannel completeness is JAR-only.
        if (!runTlaStructuralPass(ast, specDecl, procDecls)) {
            return
        }
        compileSpecToTla(specNode, ast, unit)
    }

    for (proc in tlaProcTargets) {
        if (!runTlaStructuralPass(ast, proc, procDecls)) {
            return
        }
        // Synthetic plain-system spec: equivalent to <true> P <true> (no assume, no guarantee).
        compileSpecToTla(syntheticProcSpec(proc.name), ast, unit)
    }
}

/**
 * Non-alphabet preflight for TLA+ targets: procfuns must not appear in `||`, and
 * call-site havoc warnings still print. Composition alphabet integrity is skipped.
 */
private fun runTlaStructuralPass(
    ast: julay.compiler.ast.RootNode,
    program: julay.compiler.decl.ProcDecl,
    procDecls: List<julay.compiler.decl.ProcDecl>,
): Boolean {
    val compositionErrors = ast.procFunCompositionErrors(procDecls)
    if (compositionErrors.isNotEmpty()) {
        compositionErrors.forEach { System.err.println(it) }
        System.err.println("Found errors while compiling \"${program.name}\"; exiting.")
        return false
    }
    ast.procFunHavocWarnings(program, procDecls).forEach { System.err.println(it) }
    return true
}

/** Plain `spec Name := Name` — TLA with no assumption and no guarantee invariants. */
internal fun syntheticProcSpec(procName: String): SpecNode {
    val loc = SourceLoc(0 to 0)
    return SpecNode(procName, ValueProcExprNode(procName, null, loc), loc)
}

/** Leaf spec compile target → plain system (with declaration params when present). */
internal fun syntheticLeafSpec(leaf: LeafSpecNode): SpecNode {
    val loc = leaf.programLocation()
    val body: julay.compiler.ast.ASTNode = ValueProcExprNode(leaf.leafSpecName(), null, loc)
    val system = if (leaf.isParameterized()) {
        ParamProcExprNode(body, leaf.leafSpecParamName()!!, leaf.leafSpecParamType()!!, loc)
    } else {
        body
    }
    return SpecNode(leaf.leafSpecName(), system, loc)
}
