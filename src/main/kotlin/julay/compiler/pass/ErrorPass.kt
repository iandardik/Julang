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
    /** True only for the top-level `compile` / TLA target. */
    requireCompleteSync: Boolean = true,
): List<CompileError> = when (this) {
    is RootNode -> errorPassRoot(procs, librariesInUse, program, procDecls, requireCompleteSync)
    is ProcClassNode -> errorPassProcClass(procs, librariesInUse)
    is ProcFunNode -> errorPassProcFun(procs, librariesInUse)
    is ObjClassNode -> errorPassObjClass(procs, librariesInUse)
    is ConstructorNode -> errorPassConstructor(procs, librariesInUse)
    is TransitionNode -> errorPassTransition(procs, librariesInUse)
    else -> children.flatMap {
        it.errorPass(procs, librariesInUse, program, procDecls, requireCompleteSync)
    }
}

fun ASTNode.warningPass(
    procs: Set<String>,
    librariesInUse: Set<String> = emptySet(),
    program: ProcDecl? = null,
    procDecls: List<ProcDecl> = emptyList(),
    /** True only for the top-level `compile` / TLA target. */
    requireCompleteSync: Boolean = true,
): List<CompileWarning> =
    if (this is RootNode) {
        actionConsistencyWarnings(
            procs, librariesInUse, program, procDecls, requireCompleteSync,
        )
    } else {
        emptyList()
    }

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
    requireCompleteSync: Boolean = true,
): List<CompileError> =
    children.flatMap { it.errorPass(procs, librariesInUse) } +
        actionConsistencyErrors(
            procs, librariesInUse, program, procDecls, requireCompleteSync,
        ) +
        overlappingDeclNamesErrors() +
        procFunCompositionErrors(procDecls)

private fun RootNode.actionConsistencyErrors(
    procs: Set<String>,
    librariesInUse: Set<String>,
    program: ProcDecl?,
    procDecls: List<ProcDecl>,
    requireCompleteSync: Boolean,
): List<CompileError> {
    val leafMap = leafActionMap(this, procs, librariesInUse)
    val alphabetResult = if (program != null) {
        computeCompositionAlphabet(program, procDecls, leafMap, collectProcFunNames(this), this)
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

    // Source-internal names must not also appear as ordinary/provider/session offers in the same program.
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
        val providers = transitions.filter { it.decl.modifier == TSAction.SyncRole.Provider }
        val clients = transitions.filter { it.decl.modifier == TSAction.SyncRole.Client }
        val internals = transitions.filter { it.decl.modifier == TSAction.SyncRole.Internal }
        val defaults = transitions.filter { it.decl.modifier == TSAction.SyncRole.Default }

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
                if (providers.isNotEmpty()) {
                    add(
                        OneLocCompileError(
                            providers[0].decl.loc,
                            "Expected session action \"$name\" not to use the provider tag",
                        ),
                    )
                }
                if (clients.isNotEmpty()) {
                    add(
                        OneLocCompileError(
                            clients[0].decl.loc,
                            "Expected session action \"$name\" not to use the client tag",
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
            if (internals.isNotEmpty() &&
                (providers.isNotEmpty() || clients.isNotEmpty() || defaults.isNotEmpty())
            ) {
                add(
                    OneLocCompileError(
                        internals[0].decl.loc,
                        "Expected action \"$name\" not to mix internal with other transition tags",
                    ),
                )
            }
            if (providers.size > 1) {
                add(
                    TwoLocsCompileError(
                        providers[0].decl.loc,
                        providers[1].decl.loc,
                        "Expected at most one proc to declare provider for action \"$name\"",
                    ),
                )
            }
            if (providers.isNotEmpty()) {
                transitions.filter { it.decl.modifier == TSAction.SyncRole.Internal }.forEach { offer ->
                    add(
                        TwoLocsCompileError(
                            providers[0].decl.loc,
                            offer.decl.loc,
                            "Expected action \"$name\" not to mix provider with internal",
                        ),
                    )
                }
                defaults.forEach { offer ->
                    add(
                        TwoLocsCompileError(
                            providers[0].decl.loc,
                            offer.decl.loc,
                            "Action \"$name\" cannot mix an untagged transition with a `provider` " +
                                "transition; tag the client as `client` (or hide ordinary peers first)",
                        ),
                    )
                }
            }
            if (clients.isNotEmpty() && defaults.isNotEmpty()) {
                add(
                    TwoLocsCompileError(
                        defaults[0].decl.loc,
                        clients[0].decl.loc,
                        "Action \"$name\" cannot mix an untagged transition with a `client` " +
                            "transition; tag the client as `client` (or hide ordinary peers first)",
                    ),
                )
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
            if (constructors.isNotEmpty() && providers.isNotEmpty()) {
                add(
                    TwoLocsCompileError(
                        constructors[0].decl.loc,
                        providers[0].decl.loc,
                        "Expected constructors not to use an action provided by another proc (\"$name\")",
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

        argMismatches + sessionMixErrors + sessionTagErrors + tagMixErrors + constructorErrors
    }

    val sessionOrdinaryPairErrors = sessionOrdinaryPairMixErrors(offers)

    val integrity = if (program != null) {
        alphabetIntegrityErrors(
            alphabetResult,
            collectProcFunNames(this),
            requireCompleteSync = requireCompleteSync,
        )
    } else {
        emptyList()
    }

    return alphabetResult.errors + internalMixErrors + consistencyErrors +
        sessionOrdinaryPairErrors + integrity
}

/**
 * If two proc classes share any session action, they must not also share an ordinary
 * (untagged) action. Provider/client APIs and ordinary sync with a *different* peer class
 * remain allowed.
 */
private fun sessionOrdinaryPairMixErrors(offers: List<ActionOffer>): List<CompileError> {
    val byChannel = offers.groupBy { it.channelKey }

    val sessionPairs = linkedSetOf<Pair<String, String>>()
    byChannel.values.forEach { named ->
        if (named.any { it.sourceInternal }) return@forEach
        if (named.any { it.decl.action.name == "initially" }) return@forEach
        if (!named.all { it.decl.isSession }) return@forEach
        val classes = named.map { it.pclassKey }.toSet()
        if (classes.size != 2) return@forEach
        val sorted = classes.sorted()
        sessionPairs += sorted[0] to sorted[1]
    }
    if (sessionPairs.isEmpty()) return emptyList()

    return byChannel.entries.flatMap { (_, named) ->
        if (named.any { it.sourceInternal }) return@flatMap emptyList()
        if (named.any { it.decl.action.name == "initially" }) return@flatMap emptyList()
        if (named.any { it.decl.isSession }) return@flatMap emptyList()
        // Ordinary rendezvous only (not provider/client).
        if (!named.all { it.decl.modifier == TSAction.SyncRole.Default }) {
            return@flatMap emptyList()
        }
        val classes = named.map { it.pclassKey }.toSet()
        if (classes.size != 2) return@flatMap emptyList()
        val sorted = classes.sorted()
        val pair = sorted[0] to sorted[1]
        if (pair !in sessionPairs) return@flatMap emptyList()
        val name = named[0].decl.action.name
        val offerA = named.first { it.pclassKey == sorted[0] }
        val offerB = named.first { it.pclassKey == sorted[1] }
        listOf(
            TwoLocsCompileError(
                offerA.decl.loc,
                offerB.decl.loc,
                "Procs \"${sorted[0]}\" and \"${sorted[1]}\" share a session protocol but also share " +
                    "ordinary action \"$name\"; tag \"$name\" as `session` or communicate with a " +
                    "different peer class",
            ),
        )
    }
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
    program: ProcDecl?,
    procDecls: List<ProcDecl>,
    requireCompleteSync: Boolean,
): List<CompileWarning> {
    val leafMap = leafActionMap(this, procs, librariesInUse)
    val procFunNames = collectProcFunNames(this)
    val alphabetResult = if (program != null) {
        computeCompositionAlphabet(program, procDecls, leafMap, procFunNames, this)
    } else {
        null
    }

    // Sync-completeness (client / ordinary / session peer wiring, lone providers) is
    // only required on the top-level `compile` target — not intermediate apis/procs.
    if (!requireCompleteSync) {
        return procFunHavocWarnings(program, procDecls)
    }

    // Prefer composition alphabet (includes call-folded procfun clients). Flat leaf scans
    // miss rpc_in/rpc_out helpers and falsely warn that providers have no clients.
    val providerDeadlock = if (alphabetResult != null) {
        alphabetResult.allOffers
            .filter { !it.isConstructor && !it.sourceInternal }
            .groupBy { it.name }
            .entries
            .flatMap { (name, named) ->
                if (name == "initially") return@flatMap emptyList()
                val providers = named.filter { it.isProvider }
                val clients = named.filter { it.isClient }
                if (providers.size == 1 && clients.isEmpty()) {
                    listOf(
                        OneLocCompileWarning(
                            providers[0].loc,
                            "action \"$name\" is a provider with no clients and will never synchronize (intentional deadlock)",
                        ),
                    )
                } else {
                    emptyList()
                }
            }
    } else {
        val offers = collectActionOffers(procs, librariesInUse)
        offers.groupBy { it.decl.action.name }.entries.flatMap { (name, namedOffers) ->
            if (name == "initially") return@flatMap emptyList()
            val transitions = namedOffers.filter { !it.isConstructor }
            val providers = transitions.filter { it.decl.modifier == TSAction.SyncRole.Provider }
            val clients = transitions.filter { it.decl.modifier == TSAction.SyncRole.Client }
            if (providers.size == 1 && clients.isEmpty()) {
                listOf(
                    OneLocCompileWarning(
                        providers[0].decl.loc,
                        "action \"$name\" is a provider with no clients and will never synchronize (intentional deadlock)",
                    ),
                )
            } else {
                emptyList()
            }
        }
    }

    // With a composition root, prefer the external-alphabet unsynced message.
    // Without one, warn on peer-count mismatches across the CU.
    val syncWarnings = if (alphabetResult != null) {
        unsyncedOrdinaryWarnings(alphabetResult.external)
    } else {
        missingSyncPeerWarnings(collectActionOffers(procs, librariesInUse))
    }

    return providerDeadlock + syncWarnings + procFunHavocWarnings(program, procDecls)
}

/** Warn when a default/session action does not have exactly two sync peers. */
private fun missingSyncPeerWarnings(offers: List<ActionOffer>): List<CompileWarning> =
    offers.groupBy { it.channelKey }.entries.flatMap { (_, namedOffers) ->
        val name = namedOffers[0].decl.action.name
        if (name == "initially") return@flatMap emptyList()
        if (namedOffers.all { it.sourceInternal }) return@flatMap emptyList()
        val transitions = namedOffers.filter { !it.isConstructor }
        val constructors = namedOffers.filter { it.isConstructor }
        val providers = transitions.filter { it.decl.modifier == TSAction.SyncRole.Provider }
        val clients = transitions.filter { it.decl.modifier == TSAction.SyncRole.Client }
        val internals = transitions.filter { it.decl.modifier == TSAction.SyncRole.Internal }
        if (internals.isNotEmpty() || providers.isNotEmpty() || clients.isNotEmpty()) {
            return@flatMap emptyList()
        }
        val t = transitions.map { it.pclassKey }.toSet().size
        val c = if (constructors.isNotEmpty()) 1 else 0
        if (t + c == 2) {
            emptyList()
        } else {
            listOf(
                OneLocCompileWarning(
                    namedOffers[0].decl.loc,
                    "default/session action \"$name\" does not have exactly two sync peers " +
                        "(two transitioning procs, or one transition and one constructor); found " +
                        "$t transitioning proc(s) and $c constructor offer(s)",
                ),
            )
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
    val inlinedVars = localDecls
        .filterIsInstance<VarNode>()
        .filter { it.initExpr != null }
        .map { it.name }
        .toSet()
    val ctorsCompleteAssgnErrors = localDecls
        .filterIsInstance<ConstructorNode>()
        .flatMap { ctorNode ->
            val stateVarSet = stateVars.toSet()
            val assignedVarSet = ctorNode.transitVars().map { transitRootVar(it.first) }.toSet() + inlinedVars
            val missingStateVars = stateVarSet.minus(assignedVarSet)
            val doubleInits = ctorNode.transitVars()
                .map { transitRootVar(it.first) }
                .filter { it in inlinedVars }
                .distinct()
            assertOrCompileError(
                missingStateVars.isEmpty(),
                OneLocCompileError(
                    ctorNode.programLocation(),
                    "Expected each constructor to assign a value to every state variable; missing assignments to $missingStateVars",
                ),
            ) + doubleInits.flatMap { name ->
                assertOrCompileError(
                    false,
                    OneLocCompileError(
                        ctorNode.programLocation(),
                        "Variable \"$name\" is initialized inline and cannot also be assigned in the constructor",
                    ),
                )
            }
        }
    val constVarNames = localDecls
        .filterIsInstance<VarNode>()
        .filter { it.isConst }
        .map { it.name }
        .toSet()
    val constAssignInTransitionErrors = localDecls
        .filterIsInstance<TransitionNode>()
        .flatMap { transNode -> constAssignmentErrors(transNode, constVarNames) }
    val returnInProcErrors = localDecls
        .filterIsInstance<TransitionNode>()
        .filter { it.body().any { b -> b.returns().isNotEmpty() } }
        .map {
            OneLocCompileError(
                it.programLocation(),
                "return: is only allowed inside a procfun",
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
    return children.flatMap { it.errorPass(procs, librariesInUse) } + sessionPeerNameErrors +
        repeatStateVarNameErrors + ctorsCompleteAssgnErrors +
        constAssignInTransitionErrors + returnInProcErrors + atLeastOneConstructorErrors + ctorTransActionNotMutexErrors
}

private fun ProcFunNode.errorPassProcFun(procs: Set<String>, librariesInUse: Set<String>): List<CompileError> {
    val localDecls = localDecls()
    val argNames = try {
        procFunArgs().actionArgs().map { it.name }.toSet()
    } catch (_: RuntimeException) {
        emptySet()
    }
    val varNodes = localDecls.filterIsInstance<VarNode>()
    val redeclArgErrors = varNodes.filter { it.name in argNames }.map {
        OneLocCompileError(
            it.programLocation(),
            "Procfun parameter \"${it.name}\" cannot be redeclared as a state variable",
        )
    }
    val repeatStateVarNameErrors = varNodes
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
    val transitions = localDecls.filterIsInstance<TransitionNode>()
    val ctors = localDecls.filterIsInstance<ConstructorNode>()
    val reservedCall = procFunCallCtor(name())
    val reservedRet = procFunRetAction(name())
    val reservedNames = setOf("initially", reservedCall, reservedRet, PROC_FUN_RET_VAL)
    val reservedNameClash = transitions.filter { it.transitionName() in reservedNames }.map {
        OneLocCompileError(
            it.programLocation(),
            "Name \"${it.transitionName()}\" is reserved in a procfun",
        )
    } + ctors.filter { it.constructorName() in reservedNames }.map {
        OneLocCompileError(
            it.programLocation(),
            "Name \"${it.constructorName()}\" is reserved in a procfun",
        )
    } + varNodes.filter { it.name in reservedNames }.map {
        OneLocCompileError(
            it.programLocation(),
            "Name \"${it.name}\" is reserved in a procfun",
        )
    }
    val returnTransitions = transitions.filter { it.body().any { b -> b.returns().isNotEmpty() } }
    val needReturn = assertOrCompileError(
        returnTransitions.isNotEmpty(),
        OneLocCompileError(programLocation(), "Expected procfun \"${name()}\" to have at least one return transition"),
    )
    // Any modifier except provider; session is allowed. return: may appear on any such transition.
    val modifierErrors = transitions.flatMap { trans ->
        val mod = trans.modifier()
        assertOrCompileError(
            mod != TSAction.SyncRole.Provider,
            OneLocCompileError(
                trans.programLocation(),
                "Procfun transitions cannot be tagged provider",
            ),
        )
    }
    val returnClauseErrors = returnTransitions.flatMap { trans ->
        val hasTransit = trans.body().any { it.transits().isNotEmpty() || it is TransitNode }
        val hasError = trans.body().any { it.errors().isNotEmpty() }
        assertOrCompileError(
            !hasTransit,
            OneLocCompileError(
                trans.programLocation(),
                "Return transitions cannot have a transit: block",
            ),
        ) + assertOrCompileError(
            !hasError,
            OneLocCompileError(
                trans.programLocation(),
                "Return transitions cannot have an error: block",
            ),
        )
    }
    val returnOutsideProcFunOnOrdinary = emptyList<CompileError>() // checked: return only on procfun via structure
    val inlinedVars = varNodes.filter { it.initExpr != null }.map { it.name }.toSet()
    val stateVarNames = varNodes.map { it.name }.toSet()
    // Args are initialized by the call; inline inits cover some vars; optional ctor covers the rest.
    val ctorErrors = when {
        ctors.size > 1 -> listOf(
            OneLocCompileError(programLocation(), "Procfun \"${name()}\" may have at most one constructor"),
        )
        else -> ctors.flatMap { ctor ->
            val assigned = ctor.transitVars().map { transitRootVar(it.first) }.toSet()
            val double = assigned.intersect(inlinedVars)
            val missing = stateVarNames - inlinedVars - assigned
            double.map {
                OneLocCompileError(
                    ctor.programLocation(),
                    "Variable \"$it\" is initialized inline and cannot also be assigned in the constructor",
                )
            } + assertOrCompileError(
                missing.isEmpty(),
                OneLocCompileError(
                    ctor.programLocation(),
                    "Expected constructor to assign every non-inline state variable; missing $missing",
                ),
            )
        }
    }
    val noCtorMissing = if (ctors.isEmpty()) {
        val missing = stateVarNames - inlinedVars
        assertOrCompileError(
            missing.isEmpty(),
            OneLocCompileError(
                programLocation(),
                "Procfun \"${name()}\" state variables must be initialized inline or in a constructor; missing $missing",
            ),
        )
    } else emptyList()
    val constVarNames = varNodes.filter { it.isConst }.map { it.name }.toSet() + argNames
    val constAssignErrors = transitions.flatMap { constAssignmentErrors(it, constVarNames) }
    val returnInOrdinaryProc = emptyList<CompileError>()
    return children.flatMap { it.errorPass(procs, librariesInUse) } + redeclArgErrors +
        repeatStateVarNameErrors + reservedNameClash + needReturn + modifierErrors + returnClauseErrors +
        ctorErrors + noCtorMissing + constAssignErrors + returnOutsideProcFunOnOrdinary + returnInOrdinaryProc
}

private fun sessionPeerClassNameErrors(
    transNode: TransitionNode,
    selfName: String,
    leafProcNames: Set<String>,
): List<CompileError> {
    // Skip when this proc is not a leaf of the current compile target (e.g. Timer
    // loaded transitively while compiling an unrelated JAR root).
    if (selfName !in leafProcNames) {
        return emptyList()
    }
    return transNode.body().flatMap { it.befores() + it.afters() }.flatMap { stmt ->
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
}

private fun constAssignmentErrors(transNode: TransitionNode, constVarNames: Set<String>): List<CompileError> {
    if (constVarNames.isEmpty()) return emptyList()
    val assignments = transNode.transitVars() +
        transitAssignmentNodes(transNode.body())
            .filterIsInstance<IndexTransitNode>()
            .map { it.collectionVar to it.programLocation() }
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
    val seenLets = mutableSetOf<String>()
    val assignToLetErrors = mutableListOf<CompileError>()
    for (node in transitNodes) {
        when (node) {
            is LetTransitNode -> seenLets += node.letName()
            is VarTransitNode -> {
                if (node.varName in seenLets) {
                    assignToLetErrors += OneLocCompileError(
                        node.programLocation(),
                        "Cannot assign to transit let binding \"${node.varName}\"",
                    )
                }
            }
            is IndexTransitNode -> {
                if (node.collectionVar in seenLets) {
                    assignToLetErrors += OneLocCompileError(
                        node.programLocation(),
                        "Cannot assign to transit let binding \"${node.collectionVar}\"",
                    )
                }
            }
            else -> {}
        }
    }
    val varTransitAssignments = transitNodes.flatMap { it.transitVars() }
    val wholeCollectionAssigns = transitNodes.filterIsInstance<VarTransitNode>()
        .filter { it.fieldPath.isEmpty() }
        .map { it.varName to it.programLocation() }
    val indexPutNodes = transitNodes.filterIsInstance<IndexTransitNode>()
    val overlapErrors = wholeCollectionAssigns.flatMap { (varName, loc) ->
        indexPutNodes.filter { it.collectionVar == varName }.map { put ->
            TwoLocsCompileError(
                loc,
                put.programLocation(),
                "Expected not to assign whole collection \"$varName\" and update entries in the same action",
            )
        }
    }
    return assignToLetErrors + duplicateAssignmentErrors(varTransitAssignments) { name ->
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
