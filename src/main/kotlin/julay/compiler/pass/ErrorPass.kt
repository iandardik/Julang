package julay.compiler.pass

import julay.compiler.*
import julay.compiler.ast.*
import julay.compiler.decl.*
import julay.program.*
import julay.program.library.LibraryRegistry

fun ASTNode.errorPass(procs: Set<String>, librariesInUse: Set<String> = emptySet()): List<CompileError> = when (this) {
    is RootNode -> errorPassRoot(procs, librariesInUse)
    is ProcClassNode -> errorPassProcClass(procs, librariesInUse)
    is ObjClassNode -> errorPassObjClass(procs, librariesInUse)
    is ConstructorNode -> errorPassConstructor(procs, librariesInUse)
    is TransitionNode -> errorPassTransition(procs, librariesInUse)
    else -> children.flatMap { it.errorPass(procs, librariesInUse) }
}

private fun RootNode.errorPassRoot(procs: Set<String>, librariesInUse: Set<String>): List<CompileError> =
    children.flatMap { it.errorPass(procs, librariesInUse) } +
        actionConsistencyErrors(procs, librariesInUse) +
        overlappingDeclNamesErrors()

private fun RootNode.actionConsistencyErrors(procs: Set<String>, librariesInUse: Set<String>): List<CompileError> {
    val progTransitions = declNodes()
        .flatMap { it.procClassPass(procs) }
        .flatMap { it.transitions }
    val libTransitions = librariesInUse
        .filter { it in procs }
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

private fun ProcClassNode.errorPassProcClass(procs: Set<String>, librariesInUse: Set<String>): List<CompileError> {
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
            val transitVarSet = ctorNode.transitVars().map { transitRootVar(it.first) }.toSet()
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
    return children.flatMap { it.errorPass(procs, librariesInUse) } + repeatStateVarNameErrors + ctorsCompleteAssgnErrors +
        atLeastOneConstructorErrors + ctorTransActionNotMutexErrors
}

private fun ObjClassNode.errorPassObjClass(procs: Set<String>, librariesInUse: Set<String>): List<CompileError> {
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
    return children.flatMap { it.errorPass(procs, librariesInUse) } + repeatFieldErrors
}

private fun duplicateAssignmentErrors(
    assignments: List<Pair<String, ProgramLoc>>,
    message: (String) -> String,
): List<CompileError> =
    assignments.flatMapIndexed { i, (refName, refLoc) ->
        assignments.flatMapIndexed { j, (name, loc) ->
            assertOrCompileError(
                i <= j || refName != name,
                TwoLocsCompileError(refLoc, loc, message(name)),
            )
        }
    }

private fun transitEffectOverlapErrors(
    transitAssignments: List<Pair<String, ProgramLoc>>,
    effectAssignments: List<Pair<String, ProgramLoc>>,
): List<CompileError> =
    transitAssignments.flatMap { (transitName, transitLoc) ->
        effectAssignments.flatMap { (effectName, effectLoc) ->
            assertOrCompileError(
                transitName != effectName,
                TwoLocsCompileError(
                    transitLoc,
                    effectLoc,
                    "Expected a variable not to be assigned in both transit and effect, but found assignments for \"$transitName\"",
                ),
            )
        }
    }

private fun actionBodyAssignmentErrors(body: List<ActionBodyNode>): List<CompileError> {
    val transitAssignments = body.flatMap { it.transitVars() }
    val effectAssignments = body.flatMap { it.effectAssignVars() }
    return duplicateAssignmentErrors(transitAssignments) { name ->
        "Expected at most one assignment per variable, but found multiple assignments for \"$name\""
    } + duplicateAssignmentErrors(effectAssignments) { name ->
        "Expected at most one assignment per variable in effect, but found multiple assignments for \"$name\""
    } + transitEffectOverlapErrors(transitAssignments, effectAssignments)
}

private fun ConstructorNode.errorPassConstructor(procs: Set<String>, librariesInUse: Set<String>): List<CompileError> {
    val assignmentErrors = actionBodyAssignmentErrors(body())
    val noGuardErrors = assertOrCompileError(
        body().flatMap { it.guards() }.isEmpty(),
        OneLocCompileError(programLocation(), "Expected constructors not to have guards"),
    )
    return children.flatMap { it.errorPass(procs, librariesInUse) } + assignmentErrors + noGuardErrors
}

private fun TransitionNode.errorPassTransition(procs: Set<String>, librariesInUse: Set<String>): List<CompileError> {
    val initiallyActionErrors = assertOrCompileError(
        transitionName() != "initially",
        OneLocCompileError(
            programLocation(),
            "only constructors (not transitions) can synchronize on the 'initially' action",
        ),
    )
    val assignmentErrors = actionBodyAssignmentErrors(body())
    return children.flatMap { it.errorPass(procs, librariesInUse) } + initiallyActionErrors + assignmentErrors
}
