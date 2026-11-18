package julay.compiler.pass

import julay.compiler.*
import julay.compiler.ast.*
import julay.compiler.decl.*
import julay.program.*
import julay.program.type.*
import julay.program.action.*
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
    is LeafSpecNode -> errorPassLeafSpec(procs, librariesInUse)
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
    return progOffers
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
        computeChannelDirections(
            this, procs, program, librariesInUse, procDecls,
        ).errors +
        overlappingDeclNamesErrors()

private fun RootNode.actionConsistencyErrors(
    procs: Set<String>,
    librariesInUse: Set<String>,
    program: ProcDecl?,
    procDecls: List<ProcDecl>,
    requireCompleteSync: Boolean,
): List<CompileError> {
    val leafMap = leafActionMap(this, procs, librariesInUse)
    val alphabetResult = if (program != null) {
        computeCompositionAlphabet(program, procDecls, leafMap)
    } else {
        // No composition root (e.g. analyze whole CU): unique keys for internals only.
        val all = leafMap.values.flatten()
        CompositionAlphabetResult(
            external = all.filter { !it.isLocal && !it.compositionHidden },
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

    // Local (`internal` / `timer`) names must not also appear as ordinary offers.
    val localMixErrors = alphabetResult.allOffers.groupBy { it.name }.entries.flatMap { (name, named) ->
        val locals = named.filter { it.sourceInternal || it.isTimer }
        val others = named.filter { !it.sourceInternal && !it.isTimer }
        if (locals.isEmpty() || others.isEmpty()) {
            emptyList()
        } else {
            val tag = when {
                locals.any { it.sourceInternal } && locals.any { it.isTimer } -> "internal/timer"
                locals.any { it.sourceInternal } -> "internal"
                else -> "timer"
            }
            listOf(
                OneLocCompileError(
                    locals[0].loc,
                    "Expected action \"$name\" not to mix $tag with other transition tags",
                ),
            )
        }
    }

    val consistencyErrors = offers.groupBy { it.channelKey }.entries.flatMap { (_, namedOffers) ->
        val name = namedOffers[0].decl.action.name
        if (name == "initially") {
            return@flatMap initiallyConsistencyErrors(namedOffers)
        }
        // Local (internal/timer): each occurrence has its own channel; no cross-proc check.
        if (namedOffers.all { it.sourceInternal || it.decl.isTimer }) {
            return@flatMap emptyList()
        }
        val decls = namedOffers.map { it.decl }
        val refAction = decls[0]
        val argMismatches = decls.flatMap { act ->
            assertOrCompileError(
                actionArgsCompatible(refAction.action.args, act.action.args),
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
        val clients = transitions.filter { it.decl.modifier == TSAction.SyncRole.Client }
        val internals = namedOffers.filter {
            !it.isConstructor && it.decl.modifier == TSAction.SyncRole.Internal
        }
        val defaults = transitions.filter { it.decl.modifier == TSAction.SyncRole.Default }

        val tagMixErrors = buildList {
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
                internals.forEach { offer ->
                    add(
                        TwoLocsCompileError(
                            services[0].decl.loc,
                            offer.decl.loc,
                            "Expected action \"$name\" not to mix service with internal",
                        ),
                    )
                }
                defaults.forEach { offer ->
                    add(
                        TwoLocsCompileError(
                            services[0].decl.loc,
                            offer.decl.loc,
                            "Action \"$name\" cannot mix an untagged transition with a `service` " +
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
            if (constructors.isNotEmpty() && services.isNotEmpty()) {
                add(
                    TwoLocsCompileError(
                        constructors[0].decl.loc,
                        services[0].decl.loc,
                        "Expected constructors not to use an action served by another proc (\"$name\")",
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

        argMismatches + tagMixErrors + constructorErrors
    }

    val integrity = alphabetIntegrityErrors(
        alphabetResult,
        requireCompleteSync = requireCompleteSync,
        leafOffersByPclass = leafMap,
    )
    // Parent/child pairing rules always apply; parent presence only on compiled artifacts.
    val childErrors = childSyncErrors(alphabetResult) +
        if (program != null && requireCompleteSync) childParentPresenceErrors(alphabetResult, program.name) else emptyList()

    val spawnErrors = spawningStaticErrors(procs)

    return alphabetResult.errors + localMixErrors + consistencyErrors + integrity + childErrors + spawnErrors
}

/**
 * A transition whose name matches another class's constructor spawns that class.
 * Spawning a static class is an error.
 */
private fun RootNode.spawningStaticErrors(procs: Set<String>): List<CompileError> {
    val procClasses = declNodes()
        .flatMap { it.procClassPass(procs) }
    val ctorByName = mutableMapOf<String, MutableList<ProcClassDecl>>()
    for (pc in procClasses) {
        for (ctor in pc.constructors) {
            ctorByName.getOrPut(ctor.action.name) { mutableListOf() }.add(pc)
        }
    }
    val errors = mutableListOf<CompileError>()
    for (pc in procClasses) {
        for (trans in pc.transitions) {
            val name = trans.action.name
            val targets = ctorByName[name].orEmpty().filter { it.name != pc.name }
            for (target in targets) {
                if (target.isStatic) {
                    errors += OneLocCompileError(
                        trans.loc,
                        "Cannot spawn static proc \"${target.name}\" via transition \"$name\"; " +
                            "only dynamic procs may be spawned",
                    )
                }
            }
        }
    }
    return errors
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
    val alphabetResult = if (program != null) {
        computeCompositionAlphabet(program, procDecls, leafMap)
    } else {
        null
    }

    // Sync-completeness (client / ordinary peer wiring, lone services) is
    // only required on the top-level `compile` target — not intermediate procs.
    if (!requireCompleteSync) {
        return emptyList()
    }

    val serviceDeadlock = if (alphabetResult != null) {
        alphabetResult.allOffers
            .filter { !it.isConstructor && !it.isLocal }
            .groupBy { it.name }
            .entries
            .flatMap { (name, named) ->
                if (name == "initially") return@flatMap emptyList()
                val services = named.filter { it.isService }
                val clients = named.filter { it.isClient }
                if (services.size == 1 && clients.isEmpty()) {
                    listOf(
                        OneLocCompileWarning(
                            services[0].loc,
                            "action \"$name\" is a service with no clients and will never synchronize (intentional deadlock)",
                        ),
                    )
                } else {
                    emptyList()
                }
            }
    } else {
        emptyList()
    }

    val syncWarnings = if (alphabetResult != null) {
        unsyncedOrdinaryWarnings(alphabetResult.external, handlerRuntimeActions())
    } else {
        unsyncedOrdinaryWarnings(
            collectActionOffers(procs, librariesInUse).map { offer ->
                AlphabetOffer(
                    pclassKey = offer.pclassKey,
                    occurrenceId = "",
                    introducingAssembly = offer.pclassKey,
                    name = offer.decl.action.name,
                    args = offer.decl.action.args,
                    modifier = offer.decl.modifier,
                    isTimer = offer.decl.isTimer,
                    isConstructor = offer.isConstructor,
                    loc = offer.decl.loc,
                    channelKey = offer.channelKey,
                    compositionHidden = offer.compositionHidden,
                    sourceInternal = offer.sourceInternal,
                )
            },
        )
    }

    return serviceDeadlock + syncWarnings
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

private fun LeafSpecNode.errorPassLeafSpec(
    procs: Set<String>,
    librariesInUse: Set<String>,
): List<CompileError> {
    val pcErrors = asProcClass().errorPassProcClass(procs, librariesInUse, isLeafSpec = true)
    val paramName = leafSpecParamName() ?: return pcErrors
    val forbidden = setOf(paramName)
    val transitionAssign = localDecls()
        .filterIsInstance<TransitionNode>()
        .flatMap { constAssignmentErrors(it, forbidden) }
        .map { err ->
            when (err) {
                is OneLocCompileError ->
                    OneLocCompileError(err.loc, "Cannot assign to leaf-spec parameter \"$paramName\"")
                else -> err
            }
        }
    val ctorAssign = localDecls()
        .filterIsInstance<ConstructorNode>()
        .flatMap { ctor ->
            ctor.transitVars().mapNotNull { (key, loc) ->
                if (transitRootVar(key) == paramName) {
                    OneLocCompileError(loc, "Cannot assign to leaf-spec parameter \"$paramName\"")
                } else {
                    null
                }
            }
        }
    return pcErrors + transitionAssign + ctorAssign
}

private fun ProcClassNode.errorPassProcClass(
    procs: Set<String>,
    librariesInUse: Set<String>,
    isLeafSpec: Boolean = false,
): List<CompileError> {
    val entity = if (isLeafSpec) "spec" else "proc"
    val localDecls = localDecls()
    val staticCtorErrors = staticConstructorErrors(entity)
    val lifecycleErrors = lifecycleErrors(entity)
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
    val atLeastOneConstructorErrors = assertOrCompileError(
        localDecls.flatMap { it.constructors() }.isNotEmpty(),
        OneLocCompileError(programLocation(), "Expected \"${procClassNodeName()}\" to have at least one constructor"),
    )
    val userStateVarNames = localDecls.filterIsInstance<VarNode>().map { it.name }.toSet()
    val constructorBeforeErrors = localDecls.filterIsInstance<ConstructorNode>().flatMap { ctor ->
        constructorBeforeBlockErrors(ctor)
    }
    val stateInAssignErrors =
        inlineInitStateRefErrors(localDecls.filterIsInstance<VarNode>(), userStateVarNames, emptySet()) +
            localDecls.filterIsInstance<ConstructorNode>().flatMap { ctor ->
                val argNames = ctor.actionArgs().map { it.name }.toSet()
                actionBodyConstructorStateInAssignErrors(ctor.body(), userStateVarNames, argNames)
            }
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
    return children.flatMap { it.errorPass(procs, librariesInUse) } + staticCtorErrors +
        repeatStateVarNameErrors + ctorsCompleteAssgnErrors +
        constAssignInTransitionErrors + lifecycleErrors + atLeastOneConstructorErrors +
        constructorBeforeErrors + stateInAssignErrors + ctorTransActionNotMutexErrors
}

/** Static procs/specs must have exactly one constructor, and it must be named `initially`. */
private fun ProcClassNode.staticConstructorErrors(entity: String = "proc"): List<CompileError> {
    if (!isStatic) return emptyList()
    val name = procClassNodeName()
    val label = if (entity == "spec") "Static spec" else "Static proc"
    val ctors = localDecls().filterIsInstance<ConstructorNode>()
    return when {
        ctors.size != 1 -> listOf(
            OneLocCompileError(
                programLocation(),
                "$label \"$name\" must have exactly one constructor named \"initially\"",
            ),
        )
        ctors.single().constructorName() != "initially" -> listOf(
            OneLocCompileError(
                ctors.single().programLocation(),
                "$label \"$name\" constructor must be named \"initially\", " +
                    "found \"${ctors.single().constructorName()}\"",
            ),
        )
        else -> emptyList()
    }
}

/**
 * Static vs dynamic lifecycle: only static declare `initially`; dynamic need a `return`;
 * static have none. Same rules for `proc` / `static proc` and leaf `spec` / `static spec`.
 */
internal fun ProcClassNode.lifecycleErrors(entity: String = "proc"): List<CompileError> {
    val name = procClassNodeName()
    val returns = localDecls().filterIsInstance<ReturnActionNode>()
    val errors = mutableListOf<CompileError>()
    val staticLabel = if (entity == "spec") "Static spec" else "Static proc"
    val dynamicLabel = if (entity == "spec") "Dynamic spec" else "Dynamic proc"
    val staticKeyword = if (entity == "spec") "static spec" else "static proc"
    if (isStatic) {
        returns.forEach {
            errors += OneLocCompileError(
                it.programLocation(),
                "$staticLabel \"$name\" cannot have a return action \"${it.returnActionName()}\"",
            )
        }
    } else {
        localDecls().filterIsInstance<ConstructorNode>()
            .filter { it.constructorName() == "initially" }
            .forEach {
                errors += OneLocCompileError(
                    it.programLocation(),
                    "Only ${entity}s marked static may declare constructor \"initially\"; " +
                        "declare \"$name\" as `$staticKeyword` or give it a spawning constructor",
                )
            }
        if (returns.isEmpty()) {
            errors += OneLocCompileError(
                programLocation(),
                "$dynamicLabel \"$name\" must have at least one return action",
            )
        }
    }
    return errors
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

private fun constructorBeforeBlockErrors(ctor: ConstructorNode): List<CompileError> {
    val befores = ctor.body().flatMap { it.befores() }
    if (befores.isEmpty()) return emptyList()
    return listOf(
        OneLocCompileError(
            befores.first().programLocation(),
            "Constructors cannot have a before: block",
        ),
    )
}

/**
 * Inline `var`/`const` inits are folded into the constructor. An init RHS must not
 * reference any user state variable (args and pure expressions are fine).
 */
private fun inlineInitStateRefErrors(
    varNodes: List<VarNode>,
    stateVars: Set<String>,
    argNames: Set<String>,
): List<CompileError> =
    varNodes.mapNotNull { vn ->
        val init = vn.initExpr ?: return@mapNotNull null
        val refs = stateVarRefsInExpr(init, stateVars, argNames)
        if (refs.isEmpty()) null
        else OneLocCompileError(
            vn.programLocation(),
            "Inline initializer for \"${vn.name}\" cannot reference state variable(s) ${refs.sorted()}; " +
                "bind them with a transit let in a constructor first",
        )
    }

/**
 * Constructor transit only: let initializers and assignment / index-put RHSs must not
 * mention any user state variable (including self-updates). Action args and earlier
 * transit lets may appear. Ordinary transitions are unchecked.
 */
private fun actionBodyConstructorStateInAssignErrors(
    body: List<ActionBodyNode>,
    stateVars: Set<String>,
    argNames: Set<String>,
): List<CompileError> {
    if (stateVars.isEmpty()) return emptyList()
    val transitNodes = transitAssignmentNodes(body)
    val seenLets = mutableSetOf<String>()
    val errors = mutableListOf<CompileError>()
    for (node in transitNodes) {
        when (node) {
            is LetTransitNode -> {
                val refs = stateVarRefsInExpr(node.letInitExpr(), stateVars, seenLets + argNames)
                if (refs.isNotEmpty()) {
                    errors += OneLocCompileError(
                        node.programLocation(),
                        "Constructor let \"${node.letName()}\" cannot reference state variable(s) ${refs.sorted()}; " +
                            "use constructor args or earlier lets that do not read state",
                    )
                }
                seenLets += node.letName()
            }
            is VarTransitNode -> {
                val refs = stateVarRefsInExpr(node.transitExpr(), stateVars, seenLets + argNames)
                if (refs.isNotEmpty()) {
                    errors += OneLocCompileError(
                        node.programLocation(),
                        "Constructor assignment to \"${node.transitKey()}\" cannot reference state variable(s) ${refs.sorted()}; " +
                            "bind them with a transit let first",
                    )
                }
            }
            is IndexTransitNode -> {
                val refs = stateVarRefsInExpr(node.value, stateVars, seenLets + argNames) +
                    stateVarRefsInExpr(node.index, stateVars, seenLets + argNames)
                if (refs.isNotEmpty()) {
                    errors += OneLocCompileError(
                        node.programLocation(),
                        "Constructor assignment to \"${node.collectionVar}[...]\" cannot reference state variable(s) ${refs.sorted()}; " +
                            "bind them with a transit let first",
                    )
                }
            }
            else -> {}
        }
    }
    return errors
}

/**
 * Free user-state names in [expr]. [bound] includes transit/expression lets, lambda binders,
 * and constructor/procfun args (which shadow state of the same name).
 */
private fun stateVarRefsInExpr(
    expr: ExprNode,
    stateVars: Set<String>,
    bound: Set<String> = emptySet(),
): Set<String> {
    val found = mutableSetOf<String>()
    fun walk(e: ExprNode, boundNow: Set<String>) {
        when (e) {
            is SymbolValueExprNode -> {
                val s = e.symbol
                if (s in stateVars && s !in boundNow) found += s
            }
            is ThisAccessExprNode -> {
                val s = e.stateVarName()
                if (s in stateVars && s !in boundNow) found += s
            }
            is QuantifiedExprNode -> walk(e.quantifiedBody(), boundNow + e.binderName())
            is LetExprNode -> {
                walk(e.letInitExpr(), boundNow)
                walk(e.bodyExpr(), boundNow + e.letName())
            }
            is LambdaExprNode -> walk(e.body, boundNow + e.params.toSet())
            is MethodCallExprNode -> {
                walk(e.baseExpr, boundNow)
                e.args.forEach { walk(it, boundNow) }
                val hofBody = e.hofBodyOrNull()
                val hofNames = e.hofParamNamesOrNull()
                if (hofBody != null && hofNames != null) {
                    walk(hofBody, boundNow + hofNames.toSet())
                }
            }
            else -> e.children.filterIsInstance<ExprNode>().forEach { walk(it, boundNow) }
        }
    }
    walk(expr, bound)
    return found
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
    return children.flatMap { it.errorPass(procs, librariesInUse) } + initiallyActionErrors +
        assignmentErrors
}
