package julay.ast

import julay.program.*
import julay.program.library.LibraryRegistry

fun ASTNode.errorPass(procs: Set<String>): List<CompileError> = when (this) {
    is RootNode -> errorPassRoot(procs)
    is ProcClassNode -> errorPassProcClass(procs)
    is ObjClassNode -> errorPassObjClass(procs)
    is ConstructorNode -> errorPassConstructor(procs)
    is TransitionNode -> errorPassTransition(procs)
    else -> children.flatMap { it.errorPass(procs) }
}

private fun RootNode.errorPassRoot(procs: Set<String>): List<CompileError> =
    children.flatMap { it.errorPass(procs) } +
        actionConsistencyErrors(procs) +
        overlappingDeclNamesErrors()

private fun RootNode.actionConsistencyErrors(procs: Set<String>): List<CompileError> {
    val progTransitions = declNodes()
        .flatMap { it.procClassPass(procs) }
        .flatMap { it.transitions }
    val libTransitions = procPass()
        .flatMap { it.allProcNames(procPass()) }
        .filter { it in procs && LibraryRegistry.isLibrary(it) }
        .flatMap { LibraryRegistry.actionDecls(it) }
    val allTransitions = progTransitions + libTransitions
    val actionOccurrences = allTransitions.groupBy { it.action.name }
    return actionOccurrences.entries.flatMap { (name, actions) ->
        val refAction = actions[0]
        val argMismatches = actions.flatMap { act ->
            assertOrCompileError(
                refAction.action.args == act.action.args,
                TwoLocsCompileError(
                    refAction.loc,
                    act.loc,
                    "Expected action \"$name\" to have the same arguments",
                ),
            )
        }
        val inconsistentSyncTypes = actions.flatMap { act ->
            assertOrCompileError(
                refAction.action.syncType == act.action.syncType,
                TwoLocsCompileError(
                    refAction.loc,
                    act.loc,
                    "Expected action \"$name\" to have the same modifiers",
                ),
            )
        }
        val p2pMissingASide = actions.let { actionList ->
            val isP2P = actionList.any { act -> act.action.syncType == SymbolicAction.SyncType.P2P }
            val hasService = actionList.any { act -> act.modifier == TSAction.SyncRole.P2PService }
            val hasConsumer = actionList.any { act -> act.modifier == TSAction.SyncRole.P2PConsumer }
            val missingType = if (hasService) "p2p-consumer" else "p2p-service"
            assertOrCompileError(
                !isP2P || (hasService && hasConsumer),
                OneLocCompileError(
                    refAction.loc,
                    "Expected action \"$name\" to have at least one corresponding \"$missingType\" action",
                ),
            )
        }
        argMismatches + inconsistentSyncTypes + p2pMissingASide
    }
}

private fun RootNode.overlappingDeclNamesErrors(): List<CompileError> {
    val decls = declNodes()
    return decls.flatMap { refDecl ->
        decls
            .filter { decl -> refDecl != decl && refDecl.name() == decl.name() }
            .map { decl ->
                TwoLocsCompileError(
                    refDecl.programLocation(),
                    decl.programLocation(),
                    "Expected each declaration to have a unique name, but found at least two named \"${decl.name()}\"",
                )
            }
    }
}

private fun ProcClassNode.errorPassProcClass(procs: Set<String>): List<CompileError> {
    val localDecls = localDecls()
    val repeatStateVarNameErrors = localDecls
        .filterIsInstance<VarNode>()
        .groupBy { it.name }
        .flatMap { (_, nodes) ->
            if (nodes.size == 1) emptyList()
            else listOf(
                TwoLocsCompileError(
                    nodes[0].programLocation(),
                    nodes[1].programLocation(),
                    "Expected state variables to have unique names",
                ),
            )
        }
    val stateVars = localDecls.flatMap { it.stateVariables() }.map { it.name }
    val ctorsCompleteAssgnErrors = localDecls
        .filterIsInstance<ConstructorNode>()
        .flatMap { ctorNode ->
            val stateVarSet = stateVars.toSet()
            val transitVarSet = ctorNode.transitVars().map { it.first }.toSet()
            val missingStateVars = stateVarSet.minus(transitVarSet)
            assertOrCompileError(
                missingStateVars.isEmpty(),
                OneLocCompileError(
                    ctorNode.programLocation(),
                    "Expected each constructor to assign a value to every state variable; missing assignments to $missingStateVars",
                ),
            )
        }
    val atLeastOneConstructorErrors = assertOrCompileError(
        localDecls.flatMap { it.constructors() }.isNotEmpty(),
        OneLocCompileError(programLocation(), "Expected \"${procClassNodeName()}\" to have at least one constructor"),
    )
    val constructorActions = localDecls.flatMap { it.constructors() }
    val transitionActions = localDecls.flatMap { it.transitions() }
    val ctorTransActionNotMutexErrors = constructorActions.flatMap { ctorAct ->
        transitionActions.flatMap { transAct ->
            assertOrCompileError(
                ctorAct.action.name != transAct.action.name,
                TwoLocsCompileError(
                    ctorAct.loc,
                    transAct.loc,
                    "Expected constructor names to not overlap with transition names, but found at least one overlap for the action \"${ctorAct.action.name}\"",
                ),
            )
        }
    }
    return children.flatMap { it.errorPass(procs) } + repeatStateVarNameErrors + ctorsCompleteAssgnErrors +
        atLeastOneConstructorErrors + ctorTransActionNotMutexErrors
}

private fun ObjClassNode.errorPassObjClass(procs: Set<String>): List<CompileError> {
    val repeatFieldErrors = objClassFields()
        .groupBy { it.fieldName }
        .flatMap { (_, nodes) ->
            if (nodes.size == 1) emptyList()
            else listOf(
                TwoLocsCompileError(
                    nodes[0].programLocation(),
                    nodes[1].programLocation(),
                    "Expected o-class fields to have unique names",
                ),
            )
        }
    return children.flatMap { it.errorPass(procs) } + repeatFieldErrors
}

private fun ConstructorNode.errorPassConstructor(procs: Set<String>): List<CompileError> {
    val multiVarTransitError = body()
        .flatMap { it.transitVars() }
        .let { transits ->
            transits.flatMapIndexed { i, (refName, refLoc) ->
                transits.flatMapIndexed { j, (name, loc) ->
                    assertOrCompileError(
                        i <= j || refName != name,
                        TwoLocsCompileError(
                            refLoc,
                            loc,
                            "Expected at most one assignment per variable, but found multiple assignments for \"$name\"",
                        ),
                    )
                }
            }
        }
    val noGuardErrors = assertOrCompileError(
        body().flatMap { it.guards() }.isEmpty(),
        OneLocCompileError(programLocation(), "Expected constructors not to have guards"),
    )
    return children.flatMap { it.errorPass(procs) } + multiVarTransitError + noGuardErrors
}

private fun TransitionNode.errorPassTransition(procs: Set<String>): List<CompileError> {
    val initiallyActionErrors = assertOrCompileError(
        transitionName() != "initially",
        OneLocCompileError(
            programLocation(),
            "only constructors (not transitions) can synchronize on the 'initially' action",
        ),
    )
    val multiVarTransitError = body()
        .flatMap { it.transitVars() }
        .let { transits ->
            transits.flatMapIndexed { i, (refName, refLoc) ->
                transits.flatMapIndexed { j, (name, loc) ->
                    assertOrCompileError(
                        i <= j || refName != name,
                        TwoLocsCompileError(
                            refLoc,
                            loc,
                            "Expected at most one assignment per variable, but found multiple assignments for \"$name\"",
                        ),
                    )
                }
            }
        }
    return children.flatMap { it.errorPass(procs) } + initiallyActionErrors + multiVarTransitError
}
