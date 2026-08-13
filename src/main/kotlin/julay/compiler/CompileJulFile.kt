package julay.compiler

import julay.compiler.ast.ApiNode
import julay.compiler.ast.LeafSpecNode
import julay.compiler.ast.ProcClassNode
import julay.compiler.ast.ProcFunNode
import julay.compiler.ast.ProcNode
import julay.compiler.ast.SpecNode
import julay.compiler.ast.ValueProcExprNode
import julay.compiler.pass.compositionLeavesOfSpec
import julay.compiler.pass.compileSpecToTla
import julay.compiler.pass.asSyntheticProcClass
import julay.compiler.pass.TlaOptConfig
import julay.compiler.pass.expandLeavesToPclasses
import julay.compiler.pass.peerCompositionErrors
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
    tlaOptConfig: TlaOptConfig = TlaOptConfig.ALL_ON,
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

    val compositionSpecs = unit.modules
        .flatMap { it.root.declNodes().filterIsInstance<SpecNode>() }
        .associateBy { it.name() }
    val leafSpecs = unit.modules
        .flatMap { it.root.declNodes().filterIsInstance<LeafSpecNode>() }
        .associateBy { it.name() }
    val pclasses = unit.modules
        .flatMap { it.root.declNodes() }
        .mapNotNull { decl ->
            when (decl) {
                is ProcClassNode -> decl.name() to decl
                is LeafSpecNode -> decl.name() to decl.asProcClass()
                else -> null
            }
        }
        .toMap()
    val procAliases = unit.modules
        .flatMap { it.root.declNodes().filterIsInstance<ProcNode>() }
        .associateBy { it.name() }
    val apiAliases = unit.modules
        .flatMap { it.root.declNodes().filterIsInstance<ApiNode>() }
        .associateBy { it.name() }
    val procFuns = unit.modules
        .flatMap { it.root.declNodes().filterIsInstance<ProcFunNode>() }
        .associate { it.name() to it.asSyntheticProcClass() }

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
        val leaves = expandLeavesToPclasses(
            compositionLeavesOfSpec(specNode),
            pclasses + procFuns,
            procAliases,
            compositionSpecs,
            apiAliases,
            leafSpecs,
        )
        val peerErrors = peerCompositionErrors(leaves, leafSpecs, pclasses)
        if (peerErrors.isNotEmpty()) {
            peerErrors.forEach { System.err.println(it) }
            System.err.println("Found errors while compiling \"${specDecl.name}\"; exiting.")
            return
        }
        compileSpecToTla(specNode, ast, unit, tlaOptConfig = tlaOptConfig)
    }

    for (proc in tlaProcTargets) {
        if (!runTlaStructuralPass(ast, proc, procDecls)) {
            return
        }
        // Synthetic plain-system spec: equivalent to <true> P <true> (no assume, no guarantee).
        compileSpecToTla(syntheticProcSpec(proc.name), ast, unit, tlaOptConfig = tlaOptConfig)
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

/** Leaf spec compile target → plain system (decl params are body binders, not create-index). */
internal fun syntheticLeafSpec(leaf: LeafSpecNode): SpecNode {
    val loc = leaf.programLocation()
    val body = ValueProcExprNode(leaf.leafSpecName(), null, loc)
    return SpecNode(leaf.leafSpecName(), body, loc)
}
