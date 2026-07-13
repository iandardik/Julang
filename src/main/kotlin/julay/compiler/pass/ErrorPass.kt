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
    val progClasses = declNodes().flatMap { it.procClassPass(procs) }
    val progActions = progClasses.flatMap { it.transitions + it.constructors }
    val libActions = librariesInUse
        .filter { it in procs }
        .flatMap { LibraryRegistry.actionDecls(it) }
    val allActions = progActions + libActions
    val actionOccurrences = allActions.groupBy { it.action.name }
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
            // Skip bootstrap initially: it is checked separately and must remain CSP-only.
            if (name == "initially") {
                emptyList()
            } else {
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
            val assignedVarSet = (
                ctorNode.transitVars().map { transitRootVar(it.first) } +
                    ctorNode.body().flatMap { it.effectAssignVars() }.map { transitRootVar(it.first) }
                ).toSet()
            val missingStateVars = stateVarSet.minus(assignedVarSet)
            assertOrCompileError(
                missingStateVars.isEmpty(),
                OneLocCompileError(
                    ctorNode.programLocation(),
                    "Expected each constructor to assign a value to every state variable; missing assignments to $missingStateVars",
                ),
            )
        }
    val constVarNames = localDecls
        .filterIsInstance<VarNode>()
        .filter { it.isConst }
        .map { it.name }
        .toSet()
    val constAssignInTransitionErrors = localDecls
        .filterIsInstance<TransitionNode>()
        .flatMap { transNode -> constAssignmentErrors(transNode, constVarNames) }
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
        constAssignInTransitionErrors + atLeastOneConstructorErrors + ctorTransActionNotMutexErrors
}

private fun constAssignmentErrors(transNode: TransitionNode, constVarNames: Set<String>): List<CompileError> {
    if (constVarNames.isEmpty()) return emptyList()
    val assignments = transNode.transitVars() +
        transNode.body().flatMap { it.effectAssignVars() } +
        transitAssignmentNodes(transNode.body())
            .filterIsInstance<MapIndexTransitNode>()
            .map { it.mapVar to it.programLocation() }
    return assignments.flatMap { (key, loc) ->
        val root = transitRootVar(key)
        assertOrCompileError(
            root !in constVarNames,
            OneLocCompileError(loc, "Cannot assign to const variable \"$root\""),
        )
    }
}

private fun ObjClassNode.errorPassObjClass(procs: Set<String>, librariesInUse: Set<String>): List<CompileError> {
    val dupTypeParamErrors = typeParams
        .groupingBy { it }
        .eachCount()
        .filter { it.value > 1 }
        .keys
        .map { dup ->
            OneLocCompileError(programLocation(), "Duplicate type parameter \"$dup\" on o-class \"${name()}\"")
        }
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
    return children.flatMap { it.errorPass(procs, librariesInUse) } + dupTypeParamErrors + repeatFieldErrors
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

private fun transitAssignmentNodes(body: List<ActionBodyNode>): List<ActionBodyNode> =
    body.flatMap { node ->
        when (node) {
            is TransitNode -> node.transitBodies()
            else -> emptyList()
        }
    }

private fun actionBodyAssignmentErrors(body: List<ActionBodyNode>): List<CompileError> {
    val effectAssignments = body.flatMap { it.effectAssignVars() }
    val transitNodes = transitAssignmentNodes(body)
    val varTransitAssignments = transitNodes.flatMap { it.transitVars() }
    val wholeMapAssigns = transitNodes.filterIsInstance<VarTransitNode>()
        .filter { it.fieldPath.isEmpty() }
        .map { it.varName to it.programLocation() }
    val mapPutNodes = transitNodes.filterIsInstance<MapIndexTransitNode>()
    val overlapErrors = wholeMapAssigns.flatMap { (varName, loc) ->
        mapPutNodes.filter { it.mapVar == varName }.map { put ->
            TwoLocsCompileError(
                loc,
                put.programLocation(),
                "Expected not to assign whole map \"$varName\" and update entries in the same action",
            )
        }
    }
    return duplicateAssignmentErrors(varTransitAssignments) { name ->
        "Expected at most one assignment per variable, but found multiple assignments for \"$name\""
    } + duplicateAssignmentErrors(effectAssignments) { name ->
        "Expected at most one assignment per variable in effect, but found multiple assignments for \"$name\""
    } + transitEffectOverlapErrors(varTransitAssignments, effectAssignments) + overlapErrors
}

private fun ConstructorNode.errorPassConstructor(procs: Set<String>, librariesInUse: Set<String>): List<CompileError> {
    val assignmentErrors = actionBodyAssignmentErrors(body())
    val noGuardErrors = assertOrCompileError(
        body().flatMap { it.guards() }.isEmpty(),
        OneLocCompileError(programLocation(), "Expected constructors not to have guards"),
    )
    val initiallyArgs = actionArgs()
    val expectedInitiallyArgs = listOf(Variable("args", listType(stringType)))
    val ctorName = constructors().single().action.name
    val initiallySignatureErrors = if (ctorName != "initially") {
        emptyList()
    } else {
        assertOrCompileError(
            initiallyArgs == expectedInitiallyArgs,
            OneLocCompileError(
                programLocation(),
                "Expected constructor \"initially\" to have signature initially(args : List<String>)",
            ),
        )
    }
    // initially is a program bootstrap CSP self-sync; it cannot be a p2p constructor.
    val initiallyCspErrors = if (ctorName != "initially") {
        emptyList()
    } else {
        assertOrCompileError(
            constructorModifier() == TSAction.SyncRole.CSP,
            OneLocCompileError(
                programLocation(),
                "Expected constructor \"initially\" not to have a p2p modifier",
            ),
        )
    }
    return children.flatMap { it.errorPass(procs, librariesInUse) } + assignmentErrors + noGuardErrors +
        initiallySignatureErrors + initiallyCspErrors
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
