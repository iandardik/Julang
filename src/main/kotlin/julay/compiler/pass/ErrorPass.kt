package julay.compiler.pass

import julay.compiler.*
import julay.compiler.ast.*
import julay.compiler.decl.*
import julay.program.*
import julay.program.type.*
import julay.program.action.*
import julay.program.library.LibraryRegistry

fun ASTNode.errorPass(
    procs: Set<String>,
    librariesInUse: Set<String> = emptySet(),
    program: ProcDecl? = null,
    procDecls: List<ProcDecl> = emptyList(),
    jarUnsyncedCheck: Boolean = false,
): List<CompileError> = when (this) {
    is RootNode -> errorPassRoot(procs, librariesInUse, program, procDecls, jarUnsyncedCheck)
    is ProcClassNode -> errorPassProcClass(procs, librariesInUse)
    is ObjClassNode -> errorPassObjClass(procs, librariesInUse)
    is ConstructorNode -> errorPassConstructor(procs, librariesInUse)
    is TransitionNode -> errorPassTransition(procs, librariesInUse)
    else -> children.flatMap { it.errorPass(procs, librariesInUse, program, procDecls, jarUnsyncedCheck) }
}

fun ASTNode.warningPass(
    procs: Set<String>,
    librariesInUse: Set<String> = emptySet(),
): List<CompileWarning> =
    if (this is RootNode) actionConsistencyWarnings(procs, librariesInUse) else emptyList()

private data class ActionOffer(
    val pclassKey: String,
    val decl: ActionDecl,
    val isConstructor: Boolean,
    val channelKey: String = decl.action.channelKey,
    val compositionHidden: Boolean = false,
    val sourceInternal: Boolean = decl.modifier == TSAction.SyncRole.Internal,
)

private fun RootNode.collectActionOffers(procs: Set<String>, librariesInUse: Set<String>): List<ActionOffer> {
    val progOffers = declNodes()
        .flatMap { it.procClassPass(procs) }
        .flatMap { pc ->
            pc.transitions.map { ActionOffer(pc.name, it, isConstructor = false) } +
                pc.constructors.map { ActionOffer(pc.name, it, isConstructor = true) }
        }
    val libOffers = librariesInUse
        .filter { it in procs && LibraryRegistry.isKotlinLibrary(it) }
        .flatMap { libName ->
            val info = LibraryRegistry.staticInfo(libName)
            val key = info.name
            val ctorActs = info.constructors.keys
            val alphabet = info.alphabet
            LibraryRegistry.actionDecls(libName).mapNotNull { decl ->
                when {
                    decl.action in ctorActs -> ActionOffer(key, decl, isConstructor = true)
                    decl.action in alphabet -> ActionOffer(key, decl, isConstructor = false)
                    else -> null
                }
            }
        }
    return progOffers + libOffers
}

private fun RootNode.errorPassRoot(
    procs: Set<String>,
    librariesInUse: Set<String>,
    program: ProcDecl? = null,
    procDecls: List<ProcDecl> = emptyList(),
    jarUnsyncedCheck: Boolean = false,
): List<CompileError> =
    children.flatMap { it.errorPass(procs, librariesInUse) } +
        actionConsistencyErrors(procs, librariesInUse, program, procDecls, jarUnsyncedCheck) +
        overlappingDeclNamesErrors()

private fun RootNode.actionConsistencyErrors(
    procs: Set<String>,
    librariesInUse: Set<String>,
    program: ProcDecl?,
    procDecls: List<ProcDecl>,
    jarUnsyncedCheck: Boolean,
): List<CompileError> {
    val leafMap = leafActionMap(this, procs, librariesInUse)
    val alphabetResult = if (program != null) {
        computeCompositionAlphabet(program, procDecls, leafMap)
    } else {
        // No composition root (e.g. analyze whole CU): unique keys for internals only.
        val all = leafMap.values.flatten()
        CompositionAlphabetResult(
            external = all.filter { !it.sourceInternal },
            allOffers = all,
            channelKeys = all.associate { it.leafId to it.channelKey },
            leafOccurrences = emptyList(),
            errors = emptyList(),
        )
    }

    val offers = alphabetResult.allOffers.map { offer ->
        ActionOffer(
            pclassKey = offer.pclassKey,
            decl = ActionDecl(
                action = offer.toSymbolicAction(),
                guards = emptyList(),
                transits = emptyList(),
                modifier = offer.modifier,
                loc = offer.loc,
            ),
            isConstructor = offer.isConstructor,
            channelKey = offer.channelKey,
            compositionHidden = offer.compositionHidden,
            sourceInternal = offer.sourceInternal,
        )
    }

    // Source-internal names must not also appear as ordinary/service/session offers in the same program.
    val internalMixErrors = alphabetResult.allOffers.groupBy { it.name }.entries.flatMap { (name, named) ->
        val internals = named.filter { it.sourceInternal }
        val others = named.filter { !it.sourceInternal }
        if (internals.isEmpty() || others.isEmpty()) {
            emptyList()
        } else {
            listOf(
                OneLocCompileError(
                    internals[0].loc,
                    "Expected action \"$name\" not to mix internal with other transition tags",
                ),
            )
        }
    }

    val consistencyErrors = offers.groupBy { it.channelKey }.entries.flatMap { (channelKey, namedOffers) ->
        val name = namedOffers[0].decl.action.name
        if (name == "initially") {
            return@flatMap initiallyConsistencyErrors(namedOffers)
        }
        // Source-internal: each proc has its own channel; no cross-proc uniqueness check.
        if (namedOffers.all { it.sourceInternal }) {
            return@flatMap emptyList()
        }
        val decls = namedOffers.map { it.decl }
        val refAction = decls[0]
        val argMismatches = decls.flatMap { act ->
            assertOrCompileError(
                refAction.action.args == act.action.args,
                TwoLocsCompileError(
                    refAction.loc,
                    act.loc,
                    "Expected action \"$name\" to have the same arguments",
                ),
            )
        }
        val transitions = namedOffers.filter { !it.isConstructor }
        val constructors = namedOffers.filter { it.isConstructor }
        val services = transitions.filter { it.decl.modifier == TSAction.SyncRole.Service }
        val internals = transitions.filter { it.decl.modifier == TSAction.SyncRole.Internal }
        val defaults = transitions.filter {
            it.decl.modifier == TSAction.SyncRole.Default || it.decl.modifier == TSAction.SyncRole.Consumer
        }

        val sessionFlags = namedOffers.map { it.decl.isSession }
        val isSession = sessionFlags.any { it }
        val sessionMixErrors = if (isSession && sessionFlags.any { !it }) {
            val withSession = namedOffers.first { it.decl.isSession }
            val withoutSession = namedOffers.first { !it.decl.isSession }
            listOf(
                TwoLocsCompileError(
                    withSession.decl.loc,
                    withoutSession.decl.loc,
                    "Expected action \"$name\" to have matching session tags on every offer",
                ),
            )
        } else {
            emptyList()
        }
        val sessionTagErrors = if (isSession) {
            buildList {
                if (services.isNotEmpty()) {
                    add(
                        OneLocCompileError(
                            services[0].decl.loc,
                            "Expected session action \"$name\" not to use the service tag",
                        ),
                    )
                }
                if (internals.isNotEmpty()) {
                    add(
                        OneLocCompileError(
                            internals[0].decl.loc,
                            "Expected session action \"$name\" not to use the internal tag",
                        ),
                    )
                }
            }
        } else {
            emptyList()
        }

        val tagMixErrors = buildList {
            if (internals.isNotEmpty() && (services.isNotEmpty() || defaults.isNotEmpty())) {
                add(
                    OneLocCompileError(
                        internals[0].decl.loc,
                        "Expected action \"$name\" not to mix internal with other transition tags",
                    ),
                )
            }
            if (services.size > 1) {
                add(
                    TwoLocsCompileError(
                        services[0].decl.loc,
                        services[1].decl.loc,
                        "Expected at most one proc to declare service for action \"$name\"",
                    ),
                )
            }
            if (services.isNotEmpty()) {
                transitions.filter { it.decl.modifier == TSAction.SyncRole.Internal }.forEach { offer ->
                    add(
                        TwoLocsCompileError(
                            services[0].decl.loc,
                            offer.decl.loc,
                            "Expected action \"$name\" not to mix service with internal",
                        ),
                    )
                }
            }
        }

        val constructorErrors = buildList {
            if (constructors.size > 1) {
                add(
                    TwoLocsCompileError(
                        constructors[0].decl.loc,
                        constructors[1].decl.loc,
                        "Expected at most one constructor for action \"$name\"",
                    ),
                )
            }
            if (constructors.isNotEmpty() && services.isNotEmpty()) {
                add(
                    TwoLocsCompileError(
                        constructors[0].decl.loc,
                        services[0].decl.loc,
                        "Expected constructors not to use an action serviced by another proc (\"$name\")",
                    ),
                )
            }
            if (constructors.isNotEmpty() && internals.isNotEmpty()) {
                add(
                    TwoLocsCompileError(
                        constructors[0].decl.loc,
                        internals[0].decl.loc,
                        "Expected internal action \"$name\" not to have a constructor",
                    ),
                )
            }
        }

        val peerErrors = when {
            // Source-internal already handled above; composition-hidden pairs are sized below.
            internals.isNotEmpty() -> emptyList()
            services.isNotEmpty() -> emptyList() // any number of default consumers OK
            else -> {
                val t = transitions.map { it.pclassKey }.toSet().size
                val c = if (constructors.isNotEmpty()) 1 else 0
                when {
                    t + c == 2 -> emptyList()
                    // JAR compile uses unsyncedNonServiceErrors for a clearer message.
                    jarUnsyncedCheck && t + c < 2 -> emptyList()
                    else -> assertOrCompileError(
                        false,
                        OneLocCompileError(
                            refAction.loc,
                            "Expected default/session action \"$name\" to have exactly two sync peers " +
                                "(two transitioning procs, or one transition and one constructor), but found " +
                                "$t transitioning proc(s) and $c constructor offer(s)",
                        ),
                    )
                }
            }
        }

        argMismatches + sessionMixErrors + sessionTagErrors + tagMixErrors +
            constructorErrors + peerErrors
    }

    val unsynced = if (jarUnsyncedCheck && program != null) {
        unsyncedNonServiceErrors(alphabetResult.external)
    } else {
        emptyList()
    }

    val integrity = if (program != null) {
        alphabetIntegrityErrors(alphabetResult)
    } else {
        emptyList()
    }

    return alphabetResult.errors + internalMixErrors + consistencyErrors + unsynced + integrity
}

private fun initiallyConsistencyErrors(offers: List<ActionOffer>): List<CompileError> {
    val transitions = offers.filter { !it.isConstructor }
    val transitionErrors = transitions.map {
        OneLocCompileError(
            it.decl.loc,
            "only constructors (not transitions) can synchronize on the 'initially' action",
        )
    }
    // Multiple initially constructors are allowed (all are spawned at Program.run).
    return transitionErrors
}

private fun RootNode.actionConsistencyWarnings(
    procs: Set<String>,
    librariesInUse: Set<String>,
): List<CompileWarning> {
    val offers = collectActionOffers(procs, librariesInUse)
    return offers.groupBy { it.decl.action.name }.entries.flatMap { (name, namedOffers) ->
        if (name == "initially") return@flatMap emptyList()
        val transitions = namedOffers.filter { !it.isConstructor }
        val services = transitions.filter { it.decl.modifier == TSAction.SyncRole.Service }
        val consumers = transitions.filter { it.decl.modifier != TSAction.SyncRole.Service }
        if (services.size == 1 && consumers.isEmpty()) {
            listOf(
                OneLocCompileWarning(
                    services[0].decl.loc,
                    "action \"$name\" is a service with no consumers and will never synchronize (intentional deadlock)",
                ),
            )
        } else {
            emptyList()
        }
    }
}

private fun RootNode.overlappingDeclNamesErrors(): List<CompileError> {
    val decls = declNodes().filter { it !is CompileNode }
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
    val selfName = procClassNodeName()
    val sessionPeerNameErrors = localDecls
        .filterIsInstance<TransitionNode>()
        .flatMap { trans -> sessionPeerClassNameErrors(trans, selfName, procs) }
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
            val assignedVarSet = ctorNode.transitVars().map { transitRootVar(it.first) }.toSet()
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
    return children.flatMap { it.errorPass(procs, librariesInUse) } + sessionPeerNameErrors +
        repeatStateVarNameErrors + ctorsCompleteAssgnErrors +
        constAssignInTransitionErrors + atLeastOneConstructorErrors + ctorTransActionNotMutexErrors
}

/** Validate exitSession(Peer) / killSessionPeer(Peer): leaf proc class, not self. */
private fun sessionPeerClassNameErrors(
    transNode: TransitionNode,
    selfName: String,
    leafProcNames: Set<String>,
): List<CompileError> =
    transNode.body().flatMap { it.befores() + it.afters() }.flatMap { stmt ->
        val effectName = stmt.callName()
        if (effectName !in EffectBuiltinRegistry.sessionPeerClassNameEffects) {
            return@flatMap emptyList()
        }
        val arg = stmt.callArgs().singleOrNull()
            ?: return@flatMap listOf(
                OneLocCompileError(
                    stmt.programLocation(),
                    "Expected effect \"$effectName\" to take 1 argument",
                ),
            )
        if (arg !is SymbolValueExprNode) {
            return@flatMap listOf(
                OneLocCompileError(
                    arg.programLocation(),
                    "Expected \"$effectName\" argument to be a leaf proc class name",
                ),
            )
        }
        val peerName = arg.symbol
        assertOrCompileError(
            peerName != selfName,
            OneLocCompileError(
                arg.programLocation(),
                "Expected \"$effectName\" peer to be a different proc class, not \"$selfName\"",
            ),
        ) + assertOrCompileError(
            peerName in leafProcNames,
            OneLocCompileError(
                arg.programLocation(),
                "Expected \"$effectName\" argument \"$peerName\" to name a leaf proc class in this program",
            ),
        )
    }

private fun constAssignmentErrors(transNode: TransitionNode, constVarNames: Set<String>): List<CompileError> {
    if (constVarNames.isEmpty()) return emptyList()
    val assignments = transNode.transitVars() +
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
            OneLocCompileError(programLocation(), "Duplicate type parameter \"$dup\" on obj \"${name()}\"")
        }
    val repeatFieldErrors = objClassFields()
        .groupBy { it.fieldName }
        .flatMap { (_, nodes) ->
            if (nodes.size == 1) emptyList()
            else listOf(
                TwoLocsCompileError(
                    nodes[0].programLocation(),
                    nodes[1].programLocation(),
                    "Expected obj fields to have unique names",
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

private fun transitAssignmentNodes(body: List<ActionBodyNode>): List<ActionBodyNode> =
    body.flatMap { node ->
        when (node) {
            is TransitNode -> node.transitBodies()
            else -> emptyList()
        }
    }

private fun actionBodyAssignmentErrors(body: List<ActionBodyNode>): List<CompileError> {
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
    } + overlapErrors
}

private fun ConstructorNode.errorPassConstructor(procs: Set<String>, librariesInUse: Set<String>): List<CompileError> {
    val assignmentErrors = actionBodyAssignmentErrors(body())
    val initiallyArgs = actionArgs()
    val expectedInitiallyArgs = listOf(Variable("args", listType(stringType)))
    val initiallySignatureErrors = if (constructors().single().action.name != "initially") {
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
    val transitionOnlyEffectErrors = body().flatMap { it.befores() + it.afters() }.flatMap { stmt ->
        val name = stmt.callName()
        assertOrCompileError(
            name !in EffectBuiltinRegistry.transitionOnlyEffects,
            OneLocCompileError(
                stmt.programLocation(),
                "Effect \"$name\" can only be used in a transition, not a constructor",
            ),
        )
    }
    return children.flatMap { it.errorPass(procs, librariesInUse) } + assignmentErrors +
        initiallySignatureErrors + transitionOnlyEffectErrors
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
    val sessionEffectNames = body().flatMap { it.befores() + it.afters() }.map { it.callName() }.filter {
        it in EffectBuiltinRegistry.transitionOnlyEffects
    }
    val bothSessionEffectsError = assertOrCompileError(
        !(sessionEffectNames.contains("exitSession") && sessionEffectNames.contains("killSessionPeer")),
        OneLocCompileError(
            programLocation(),
            "Expected at most one of exitSession(Peer) or killSessionPeer(Peer) in the same transition",
        ),
    )
    return children.flatMap { it.errorPass(procs, librariesInUse) } + initiallyActionErrors +
        assignmentErrors + bothSessionEffectsError
}
