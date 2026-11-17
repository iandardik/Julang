package julay.compiler

import julay.compiler.ast.LeafSpecNode
import julay.compiler.ast.ProcClassNode
import julay.compiler.ast.ProcNode
import julay.compiler.ast.SpecNode
import julay.compiler.ast.ValueProcExprNode
import julay.compiler.pass.compositionLeavesOfSpec
import julay.compiler.pass.compileSpecToTla
import julay.compiler.pass.CompilerOptConfig
import julay.compiler.pass.TlaOptConfig
import julay.compiler.pass.childParentPresenceErrors
import julay.compiler.pass.childSyncErrors
import julay.compiler.pass.handlerRuntimeActions
import julay.compiler.pass.lifecycleErrors
import julay.compiler.pass.computeCompositionAlphabet
import julay.compiler.pass.expandLeavesToPclasses
import julay.compiler.pass.leafActionMap
import julay.compiler.pass.peerCompositionErrors
import julay.compiler.pass.unboundExternalActionsWarning
import java.nio.file.Path

fun compileJulFile(
    source: Path,
    keepBuild: Boolean,
    extraLibraryPaths: List<Path> = emptyList(),
    compilerJar: Path = resolveCompilerJar(),
    allowUnindexedSpec: Boolean = false,
    compileNames: List<String> = emptyList(),
    compileTlaNames: List<String> = emptyList(),
    @Suppress("UNUSED_PARAMETER") compilerOptConfig: CompilerOptConfig = CompilerOptConfig.ALL_ON,
    tlaOptConfig: TlaOptConfig = TlaOptConfig.ALL_ON,
    @Suppress("UNUSED_PARAMETER") verbose: Boolean = false,
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

    jarTargets.forEach {
        compileProgram(
            it, ast, procDecls, librariesInUse, keepBuild, compilerJar,
            compilerOptConfig = compilerOptConfig,
        )
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

    for (specDecl in specTargets) {
        val specNode = compositionSpecs[specDecl.name]
            ?: leafSpecs[specDecl.name]?.let { syntheticLeafSpec(it) }
        if (specNode == null) {
            println("Internal error: missing SpecNode for \"${specDecl.name}\"")
            return
        }
        // TLA+ allows incomplete alphabets (unmatched clients, unilateral ordinary/session,
        // dual providers, etc.). SyncChannel completeness is JAR-only.
        if (!runTlaStructuralPass(ast, specDecl, procDecls, librariesInUse)) {
            return
        }
        val leaves = expandLeavesToPclasses(
            compositionLeavesOfSpec(specNode),
            pclasses,
            procAliases,
            compositionSpecs,
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
        if (!runTlaStructuralPass(ast, proc, procDecls, librariesInUse)) {
            return
        }
        // Synthetic plain-system spec: equivalent to <true> P <true> (no assume, no guarantee).
        compileSpecToTla(syntheticProcSpec(proc.name), ast, unit, tlaOptConfig = tlaOptConfig)
    }
}

/**
 * Preflight for TLA+ targets: parent/child rules are enforced, and one
 * unbound-external-actions warning lists leftover env alphabet names. Other composition
 * alphabet integrity (hard errors) is skipped — partial specs are allowed for TLA+.
 */
private fun runTlaStructuralPass(
    ast: julay.compiler.ast.RootNode,
    program: julay.compiler.decl.ProcDecl,
    procDecls: List<julay.compiler.decl.ProcDecl>,
    librariesInUse: Set<String> = emptySet(),
): Boolean {
    val leafMap = leafActionMap(ast, program.allProcNames(procDecls).toSet(), librariesInUse)
    val alphabet = computeCompositionAlphabet(program, procDecls, leafMap)
    val components = program.allProcNames(procDecls).toSet()
    val lifecycle = ast.declNodes().filterIsInstance<julay.compiler.ast.ProcClassNode>()
        .filter { it.name() in components }
        .flatMap { it.lifecycleErrors() }
    val childErrors = lifecycle + childSyncErrors(alphabet) + childParentPresenceErrors(alphabet, program.name)
    if (childErrors.isNotEmpty()) {
        childErrors.forEach { System.err.println(it) }
        System.err.println("Found errors while compiling \"${program.name}\"; exiting.")
        return false
    }
    unboundExternalActionsWarning(alphabet, ast.handlerRuntimeActions())?.let { System.err.println(it) }
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
