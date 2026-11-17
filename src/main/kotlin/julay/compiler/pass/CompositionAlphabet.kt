package julay.compiler.pass

import julay.compiler.CompileError
import julay.compiler.CompileWarning
import julay.compiler.OneLocCompileError
import julay.compiler.OneLocCompileWarning
import julay.compiler.ProgramLoc
import julay.compiler.TwoLocsCompileError
import julay.compiler.ast.ASTNode
import julay.compiler.ast.HandlerRefExprNode
import julay.compiler.ast.ProcClassNode
import julay.compiler.ast.RootNode
import julay.compiler.decl.ActionDecl
import julay.compiler.decl.ProcClassDecl
import julay.compiler.decl.ProcDecl
import julay.program.Variable
import julay.program.action.SymbolicAction
import julay.program.action.TSAction
/** Identity of a leaf action offer for channel-key assignment (occurrence-scoped). */
data class LeafActionId(
    val pclassKey: String,
    val occurrenceId: String,
    val actionName: String,
    val isConstructor: Boolean,
)

/**
 * One action offer in a (possibly derived) alphabet.
 * [compositionHidden] means the offer was internalized by `||` matching (not source `internal`).
 * Ordinary peers get a private [channelKey]; service hubs keep the public action name while
 * meeting clients are marked composition-hidden.
 * [occurrenceId] distinguishes multiple occurrences of the same proc class.
 * [introducingAssembly] is the named proc/assembly that introduced this leaf (for TLA naming).
 * [isStatic] is true for `static proc` classes (hub / static-static sync shapes).
 * [parentClass] is set on offers of a `B<A>` child occurrence (the parent class `A`).
 */
data class AlphabetOffer(
    val pclassKey: String,
    val occurrenceId: String,
    val introducingAssembly: String,
    val name: String,
    val args: List<Variable>,
    val modifier: TSAction.SyncRole,
    val isTimer: Boolean,
    val isConstructor: Boolean,
    val loc: ProgramLoc,
    val channelKey: String,
    val compositionHidden: Boolean = false,
    val sourceInternal: Boolean = false,
    val isStatic: Boolean = false,
    val parentClass: String? = null,
    val isReturn: Boolean = false,
) {
    val isService: Boolean get() = modifier == TSAction.SyncRole.Service
    val isClient: Boolean get() = modifier == TSAction.SyncRole.Client
    @Deprecated("Use isService", ReplaceWith("isService"))
    val isProvider: Boolean get() = isService
    val leafId: LeafActionId get() = LeafActionId(pclassKey, occurrenceId, name, isConstructor)

    /** Local to one occurrence: `internal` or `timer` (does not sync across classes). */
    val isLocal: Boolean get() = sourceInternal || isTimer

    fun signatureCompatible(other: AlphabetOffer): Boolean =
        actionArgsCompatible(args, other.args) && isTimer == other.isTimer &&
            sourceInternal == other.sourceInternal &&
            // Ordinary must agree on modifier; service↔client may differ.
            (isService || other.isService || isClient || other.isClient || modifier == other.modifier)

    fun toSymbolicAction(): SymbolicAction =
        SymbolicAction(
            name = name,
            args = args,
            isInternal = sourceInternal,
            isTimer = isTimer,
            channelKey = channelKey,
        )
}

data class CompositionAlphabetResult(
    /** External alphabet (not source-internal, not timer, not composition-hidden). */
    val external: List<AlphabetOffer>,
    /** All offers including hidden, source-internal, and timer (for channel binding / analyze). */
    val allOffers: List<AlphabetOffer>,
    val channelKeys: Map<LeafActionId, String>,
    /** Distinct leaf occurrences in left-to-right composition order. */
    val leafOccurrences: List<LeafOccurrence>,
    val errors: List<CompileError>,
)

/** One leaf proc-class occurrence in a composition (JAR / analyze / TLA). */
data class LeafOccurrence(
    val pclassName: String,
    val occurrenceId: String,
    /** Named assembly that introduced this occurrence (for TLA `{Class}_{Assembly}` renaming). */
    val introducingAssembly: String,
    /** Parent proc class for a `B<A>` child occurrence. */
    val parentClass: String? = null,
)

private var scopeCounter = 0
private var occurrenceCounter = 0

private fun freshScopeId(hint: String): String {
    scopeCounter += 1
    val safe = hint.ifEmpty { "comp" }.replace(Regex("[^A-Za-z0-9_]"), "_")
    return "${safe}_$scopeCounter"
}

private fun freshOccurrenceId(pclass: String): String {
    occurrenceCounter += 1
    val safe = pclass.replace(Regex("[^A-Za-z0-9_]"), "_")
    return "${safe}_occ$occurrenceCounter"
}

/** Reset between compilations so codegen strings stay stable within a run when desired. */
fun resetCompositionScopeCounter() {
    scopeCounter = 0
    occurrenceCounter = 0
}

fun leafActionMap(ast: RootNode, procs: Set<String>, librariesInUse: Set<String>): Map<String, List<AlphabetOffer>> {
    return ast.declNodes()
        .flatMap { it.procClassPass(procs) }
        .associate { pc -> pc.name to pc.toAlphabetOffers() }
}

private fun ProcClassDecl.toAlphabetOffers(): List<AlphabetOffer> =
    transitions.map { it.toAlphabetOffer(name, isConstructor = false, isStatic = isStatic) } +
        constructors.map { it.toAlphabetOffer(name, isConstructor = true, isStatic = isStatic) }

/** Template offers (no occurrence id yet) for a leaf pclass. */
private fun ActionDecl.toAlphabetOffer(
    pclassKey: String,
    isConstructor: Boolean,
    isStatic: Boolean,
): AlphabetOffer {
    val sourceInternal = modifier == TSAction.SyncRole.Internal
    val timer = this.isTimer || modifier == TSAction.SyncRole.Timer
    val channelKey = when {
        sourceInternal -> "$pclassKey#internal#${action.name}"
        timer -> "$pclassKey#timer#${action.name}"
        else -> action.name
    }
    return AlphabetOffer(
        pclassKey = pclassKey,
        occurrenceId = "",
        introducingAssembly = pclassKey,
        name = action.name,
        args = action.args,
        modifier = modifier,
        isTimer = timer,
        isConstructor = isConstructor,
        loc = loc,
        channelKey = channelKey,
        compositionHidden = false,
        sourceInternal = sourceInternal,
        isStatic = isStatic,
        isReturn = isReturnAction,
    )
}

/**
 * Collect leaf occurrences under [root] by expanding named assemblies independently
 * (`M || M` yields two expansions). Left-to-right, occurrence-preserving.
 */
fun collectLeafOccurrences(
    root: ProcDecl,
    procDecls: List<ProcDecl>,
): List<LeafOccurrence> {
    resetCompositionScopeCounter()
    val procDeclMap = procDecls.associateBy { it.name }
    val out = mutableListOf<LeafOccurrence>()

    fun resolve(pd: ProcDecl): ProcDecl = procDeclMap[pd.name] ?: pd

    fun walk(pd: ProcDecl, introducingAssembly: String) {
        val resolved = resolve(pd)
        if (resolved.components.isEmpty()) {
            val pclass = resolved.name
            out += LeafOccurrence(
                pclassName = pclass,
                occurrenceId = freshOccurrenceId(pclass),
                introducingAssembly = introducingAssembly,
                parentClass = pd.parentClass ?: resolved.parentClass,
            )
            return
        }
        // Each child component is an independent expansion; the assembly name for nested
        // named procs is that child's name when it has its own declaration.
        for (child in resolved.components) {
            val childResolved = resolve(child)
            val childIntro = if (childResolved.components.isNotEmpty() || child.name in procDeclMap) {
                child.name
            } else {
                introducingAssembly
            }
            walk(child, childIntro)
        }
    }

    walk(root, root.name)
    return out
}

/**
 * Compute the inductive alphabet of [root] (a JAR/analyze/TLA target), assigning private channel
 * keys to composition-hidden ordinary syncs so nested assemblies do not cross-sync on the same
 * surface name. Expands named assemblies by occurrence (`M || M` → two independent expansions).
 */
fun computeCompositionAlphabet(
    root: ProcDecl,
    procDecls: List<ProcDecl>,
    leafOffersByPclass: Map<String, List<AlphabetOffer>>,
): CompositionAlphabetResult {
    resetCompositionScopeCounter()
    val procDeclMap = procDecls.associateBy { it.name }
    val channelKeys = mutableMapOf<LeafActionId, String>()
    val errors = mutableListOf<CompileError>()
    val leafOccurrences = mutableListOf<LeafOccurrence>()

    fun recordKeys(offers: List<AlphabetOffer>) {
        offers.forEach { offer ->
            channelKeys[offer.leafId] = offer.channelKey
        }
    }

    fun resolve(pd: ProcDecl): ProcDecl = procDeclMap[pd.name] ?: pd

    fun stampLeafOffers(
        pclass: String,
        introducingAssembly: String,
        parentClass: String?,
    ): List<AlphabetOffer> {
        val occurrenceId = freshOccurrenceId(pclass)
        leafOccurrences += LeafOccurrence(pclass, occurrenceId, introducingAssembly, parentClass)
        val templates = leafOffersByPclass[pclass] ?: emptyList()
        val offers = templates.map { template ->
            val channelKey = when {
                template.sourceInternal -> "$occurrenceId#internal#${template.name}"
                template.isTimer -> "$occurrenceId#timer#${template.name}"
                else -> template.name
            }
            template.copy(
                occurrenceId = occurrenceId,
                introducingAssembly = introducingAssembly,
                channelKey = channelKey,
                parentClass = parentClass,
            )
        }
        recordKeys(offers)
        return offers
    }

    fun alphabetOf(pd: ProcDecl, introducingAssembly: String, scopeHint: String): List<AlphabetOffer> {
        val resolved = resolve(pd)
        if (resolved.components.isEmpty()) {
            return stampLeafOffers(resolved.name, introducingAssembly, pd.parentClass ?: resolved.parentClass)
        }
        val childAlphabets = resolved.components.map { child ->
            val childResolved = resolve(child)
            val childIntro = if (childResolved.components.isNotEmpty() || child.name in procDeclMap) {
                child.name
            } else {
                introducingAssembly
            }
            alphabetOf(child, childIntro, child.name)
        }
        if (childAlphabets.isEmpty()) return emptyList()
        var acc = childAlphabets[0]
        for (i in 1 until childAlphabets.size) {
            val scopeId = freshScopeId(if (i == 1) scopeHint.ifEmpty { resolved.name } else resolved.name)
            val (composed, composeErrors) = composeAlphabets(acc, childAlphabets[i], scopeId)
            errors.addAll(composeErrors)
            acc = composed
        }
        recordKeys(acc)
        return acc
    }

    val allOffers = alphabetOf(root, root.name, root.name)

    val external = allOffers.filter { !it.isLocal && !it.compositionHidden }
    return CompositionAlphabetResult(
        external = external,
        allOffers = allOffers,
        channelKeys = channelKeys.toMap(),
        leafOccurrences = leafOccurrences.toList(),
        errors = errors,
    )
}

/**
 * Compose two alphabets under [scopeId]. Matching ordinary pairs become composition-hidden
 * with a private [scopeId]-scoped channel key. Service/client rules are local to this step.
 * Same proc-class on both sides never syncs (same-class rule).
 * Timers and source-internals are treated as settled/local and never re-sync.
 */
fun composeAlphabets(
    left: List<AlphabetOffer>,
    right: List<AlphabetOffer>,
    scopeId: String,
): Pair<List<AlphabetOffer>, List<CompileError>> {
    // Local (internal/timer) and already composition-hidden offers never re-sync at an outer step;
    // they pass through with their channel keys (binary sync scoping).
    val leftSettled = left.filter { it.isLocal || it.compositionHidden }
    val rightSettled = right.filter { it.isLocal || it.compositionHidden }
    val leftExt = left.filter { !it.isLocal && !it.compositionHidden }
    val rightExt = right.filter { !it.isLocal && !it.compositionHidden }

    val errors = mutableListOf<CompileError>()
    val result = mutableListOf<AlphabetOffer>()
    result.addAll(leftSettled)
    result.addAll(rightSettled)

    val leftByName = leftExt.groupBy { it.name }
    val rightByName = rightExt.groupBy { it.name }
    val allNames = leftByName.keys + rightByName.keys

    for (name in allNames) {
        val l = leftByName[name].orEmpty()
        val r = rightByName[name].orEmpty()
        when {
            l.isEmpty() -> result.addAll(r)
            r.isEmpty() -> result.addAll(l)
            name == "initially" -> {
                // Multiple initially constructors are independent boots, not a sync pair.
                result.addAll(l)
                result.addAll(r)
            }
            else -> {
                val both = l + r
                val hasOrdinary = both.any {
                    it.modifier == TSAction.SyncRole.Default
                }
                val hasService = both.any { it.isService }
                val hasClient = both.any { it.isClient }
                if (hasOrdinary && (hasService || hasClient)) {
                    val ord = both.first { it.modifier == TSAction.SyncRole.Default }
                    val tagged = both.first { it.isService || it.isClient }
                    val tag = if (tagged.isService) "service" else "client"
                    errors.add(
                        TwoLocsCompileError(
                            ord.loc,
                            tagged.loc,
                            "Action \"$name\" cannot mix an untagged transition with a `$tag` " +
                                "transition; tag the client as `client` (or hide ordinary peers first)",
                        ),
                    )
                    result.addAll(l)
                    result.addAll(r)
                    continue
                }
                if (hasService) {
                    // Service stays external (further clients can still sync); clients that meet
                    // it leave the external alphabet. Keep the public channelKey (action name).
                    result.addAll(
                        (l + r).map { offer ->
                            if (offer.isClient) offer.copy(compositionHidden = true) else offer
                        },
                    )
                    continue
                }
                if (hasClient) {
                    // Clients never pairwise-hide with each other.
                    result.addAll(l)
                    result.addAll(r)
                    continue
                }
                val parentChild = pairParentChild(name, l, r, scopeId, errors)
                if (parentChild != null) {
                    result.addAll(parentChild)
                    continue
                }
                // Same class never syncs. Static ordinary duplicates are an immediate error
                // (a later service cannot redeem them). Dynamic duplicates may still meet a
                // child bound to their class (`C<B>` covers every `B` occurrence), so they are
                // checked once the composition is closed ([actionShapeErrors]).
                val classes = both.map { it.pclassKey }.toSet()
                if (classes.size == 1) {
                    if (hasOrdinary && both.any { it.isStatic }) {
                        val pclass = classes.first()
                        val sample = both[0]
                        val other = both.getOrElse(1) { sample }
                        errors.add(
                            TwoLocsCompileError(
                                sample.loc,
                                other.loc,
                                "Multiple occurrences of \"$pclass\" expose unsynced action \"$name\" in the composition; " +
                                    "sync each occurrence with a different-class peer, tag the action `internal`, " +
                                    "or tag callers `client` with a single `service` for \"$name\"",
                            ),
                        )
                    }
                    result.addAll(l)
                    result.addAll(r)
                    continue
                }
                // Ordinary: dynamic procs only sync parent↔child (paired above). Unrelated
                // dynamics stay open: each may still meet its own child later; leftovers are
                // reported by [actionShapeErrors].
                if (both.all { !it.isStatic }) {
                    result.addAll(l)
                    result.addAll(r)
                    continue
                }
                val ctorErr = childCtorPeerError(name, both)
                if (ctorErr != null) {
                    errors.add(ctorErr)
                    result.addAll(l)
                    result.addAll(r)
                    continue
                }
                // Ordinary: require matching signatures, then hide.
                val mismatch = findSignatureMismatch(l, r)
                if (mismatch != null) {
                    errors.add(
                        TwoLocsCompileError(
                            mismatch.first.loc,
                            mismatch.second.loc,
                            "Expected action \"$name\" to have the same signature on both sides of composition",
                        ),
                    )
                    result.addAll(l)
                    result.addAll(r)
                    continue
                }
                val hiddenKey = "$scopeId#$name"
                result.addAll(l.map { it.copy(channelKey = hiddenKey, compositionHidden = true) })
                result.addAll(r.map { it.copy(channelKey = hiddenKey, compositionHidden = true) })
            }
        }
    }
    return result to errors
}

/**
 * Pair each `B<A>` child offer on one side with every offer of class `A` on the other side.
 * One hidden key per (parent class, child class), so `C<B>` joins every `B` occurrence on one
 * channel while `B<A>` and `B<C>` stay separate. Unpaired offers pass through unchanged.
 * Returns null when no parent-child pair exists.
 */
private fun pairParentChild(
    name: String,
    left: List<AlphabetOffer>,
    right: List<AlphabetOffer>,
    scopeId: String,
    errors: MutableList<CompileError>,
): List<AlphabetOffer>? {
    fun isChildOf(child: AlphabetOffer, parent: AlphabetOffer) =
        child.parentClass != null && child.parentClass == parent.pclassKey && child.pclassKey != parent.pclassKey
    val pairs = left.flatMap { a ->
        right.mapNotNull { b ->
            when {
                isChildOf(b, a) -> a to b
                isChildOf(a, b) -> b to a
                else -> null
            }
        }
    }
    if (pairs.isEmpty()) return null
    val keyed = mutableMapOf<LeafActionId, AlphabetOffer>()
    for ((parent, child) in pairs) {
        if (!parent.signatureCompatible(child) || parent.isConstructor) {
            errors.add(
                TwoLocsCompileError(
                    parent.loc,
                    child.loc,
                    "Expected action \"$name\" to have the same signature on both sides of composition",
                ),
            )
            continue
        }
        val key = "$scopeId#$name#${child.pclassKey}<${parent.pclassKey}>"
        keyed[parent.leafId] = parent.copy(channelKey = key, compositionHidden = true)
        keyed[child.leafId] = child.copy(channelKey = key, compositionHidden = true)
    }
    return (left + right).map { keyed[it.leafId] ?: it }
}

/** Exactly two classes where one is a `B<A>` child occurrence and the other is its parent `A`. */
private fun isParentChildPair(offers: List<AlphabetOffer>): Boolean {
    val classes = offers.map { it.pclassKey }.distinct()
    if (classes.size != 2) return false
    return offers.any { o -> o.parentClass != null && o.parentClass in classes && o.parentClass != o.pclassKey }
}

/** A child constructor may only sync with its parent. */
private fun childCtorPeerError(name: String, offers: List<AlphabetOffer>): CompileError? {
    val ctor = offers.firstOrNull { it.isConstructor && it.parentClass != null } ?: return null
    val peer = offers.firstOrNull { it.pclassKey != ctor.pclassKey && it.pclassKey != ctor.parentClass } ?: return null
    return TwoLocsCompileError(
        ctor.loc,
        peer.loc,
        "Child constructor \"$name\" of \"${ctor.pclassKey}\" may only sync with its parent \"${ctor.parentClass}\", " +
            "not \"${peer.pclassKey}\"",
    )
}

private fun findSignatureMismatch(
    left: List<AlphabetOffer>,
    right: List<AlphabetOffer>,
): Pair<AlphabetOffer, AlphabetOffer>? {
    val ref = left[0]
    for (o in left + right) {
        if (!ref.signatureCompatible(o)) return ref to o
    }
    // Also require a single offer per side for pairwise sync (Julay sync size 2), except a
    // static hub meeting several dynamic offers.
    val hub = (left + right).count { it.isStatic } == 1
    if (!hub && (left.size != 1 || right.size != 1)) {
        return left[0] to right[0]
    }
    return null
}

private fun findSignatureMismatch(offers: List<AlphabetOffer>): Pair<AlphabetOffer, AlphabetOffer>? {
    if (offers.isEmpty()) return null
    val ref = offers[0]
    for (o in offers) {
        if (!ref.signatureCompatible(o)) return ref to o
    }
    return null
}

/** Every `A.snd ~> A.rcv` handler reference in a before/after of a proc class. */
fun RootNode.handlerRefs(): List<HandlerRefExprNode> {
    val out = mutableListOf<HandlerRefExprNode>()
    fun walk(n: ASTNode) {
        if (n is HandlerRefExprNode) out += n
        n.children.forEach { walk(it) }
    }
    declNodes().filterIsInstance<ProcClassNode>()
        .flatMap { pc -> pc.localDecls().flatMap { it.constructors() + it.transitions() } }
        .flatMap { it.befores + it.afters }
        .forEach { walk(it) }
    return out
}

/** (class, action) pairs synced with the HTTP runtime: handler request ctors and response actions. */
fun RootNode.handlerRuntimeActions(): Set<Pair<String, String>> =
    handlerRefs().flatMap { listOf(it.handlerClass() to it.handlerCtor(), it.handlerClass() to it.handlerAction()) }.toSet()

/** JAR-target warning: leftover ordinary actions in the external alphabet are unsynced. */
fun unsyncedOrdinaryWarnings(
    external: List<AlphabetOffer>,
    runtimeSynced: Set<Pair<String, String>> = emptySet(),
): List<CompileWarning> =
    external
        .filter { offer ->
            (offer.pclassKey to offer.name) !in runtimeSynced &&
            offer.name != "initially" &&
                !offer.isService &&
                !offer.isClient &&
                !offer.isLocal
        }
        .distinctBy { it.channelKey }
        .map { o ->
            OneLocCompileWarning(
                o.loc,
                "Action \"${o.name}\" is not synchronized with any peer; " +
                    "tag it `internal` if a solo step is intentional",
            )
        }

/**
 * TLA-target warning: one message listing every unbound (still-external) action name
 * after composition. Not an error — open env alphabet is expected for TLC.
 */
fun unboundExternalActionsWarning(
    alphabet: CompositionAlphabetResult,
    runtimeSynced: Set<Pair<String, String>> = emptySet(),
): CompileWarning? {
    val names = alphabet.external
        .filter { offer ->
            (offer.pclassKey to offer.name) !in runtimeSynced &&
            !offer.isConstructor &&
                !offer.isService &&
                offer.name != "initially" &&
                !offer.isLocal
        }
        .map { it.name }
        .distinct()
        .sorted()
    if (names.isEmpty()) return null
    val loc = alphabet.external.firstOrNull()?.loc ?: return null
    return OneLocCompileWarning(
        loc,
        "unbound actions in TLA composition: ${names.joinToString(", ")}",
    )
}

/**
 * Static procs may appear at most once in the top-level composition.
 */
fun staticOccurrenceErrors(
    leafOccurrences: List<LeafOccurrence>,
    leafOffersByPclass: Map<String, List<AlphabetOffer>>,
): List<CompileError> {
    val staticClasses = leafOffersByPclass
        .filter { (_, offers) -> offers.any { it.isStatic } }
        .keys
    val errors = mutableListOf<CompileError>()
    for ((pclass, occs) in leafOccurrences.groupBy { it.pclassName }) {
        if (pclass !in staticClasses) continue
        if (occs.size <= 1) continue
        val sampleLoc = leafOffersByPclass[pclass]?.firstOrNull()?.loc
        if (sampleLoc != null) {
            errors += OneLocCompileError(
                sampleLoc,
                "Static proc \"$pclass\" may appear at most once in the composition; found ${occs.size} occurrences",
            )
        }
    }
    return errors
}

/**
 * Action-shape rules for ordinary (non-service/client) multi-class offers that were not
 * composition-hidden by [composeAlphabets]:
 * - one static + any number of dynamics (hub)
 * - exactly two statics
 * - a single class (local / unsynced — allowed; warned separately)
 *
 * Names with service/client offers are skipped (compose handles them). Composition-hidden
 * ordinary pairs are also skipped.
 */
fun actionShapeErrors(allOffers: List<AlphabetOffer>): List<CompileError> {
    val errors = mutableListOf<CompileError>()
    val byName = allOffers
        .filter { !it.isLocal && it.name != "initially" }
        .groupBy { it.name }
    for ((name, offers) in byName) {
        if (offers.any { it.isService || it.isClient }) continue
        // Composition-hidden ordinary pairs were validated by composeAlphabets.
        val open = offers.filter { !it.compositionHidden }
        if (open.isEmpty()) continue
        val classes = open.map { it.pclassKey }.toSet()
        if (classes.size == 1) {
            val occs = open.distinctBy { it.occurrenceId }
            if (occs.size >= 2) {
                errors += TwoLocsCompileError(
                    occs[0].loc,
                    occs[1].loc,
                    "Multiple occurrences of \"${classes.first()}\" expose unsynced action \"$name\" in the composition; " +
                        "sync each occurrence with a different-class peer, tag the action `internal`, " +
                        "or tag callers `client` with a single `service` for \"$name\"",
                )
            }
            continue
        }
        if (classes.size == 0) continue
        val staticCount = classes.count { c -> open.any { it.pclassKey == c && it.isStatic } }
        val dynamicCount = classes.size - staticCount
        val sample = open.first()
        val ok = when {
            isParentChildPair(open) -> true
            staticCount == 1 -> true // hub: 1 static + any dynamics
            staticCount == 2 && dynamicCount == 0 -> true
            else -> false
        }
        if (!ok) {
            val shape = when {
                staticCount == 0 -> "dynamic-to-dynamic"
                staticCount >= 3 -> "three or more static classes"
                staticCount >= 2 && dynamicCount > 0 -> "two or more static classes plus a dynamic class"
                else -> "invalid participant set"
            }
            errors += OneLocCompileError(
                sample.loc,
                "Action \"$name\" has an illegal sync shape ($shape); " +
                    "allowed shapes are one static hub with any number of dynamics, " +
                    "exactly two static classes, a parent and its child, or a single class",
            )
        }
        val mismatch = findSignatureMismatch(open)
        if (mismatch != null) {
            errors += TwoLocsCompileError(
                mismatch.first.loc,
                mismatch.second.loc,
                "Expected action \"$name\" to have the same signature on every offering class",
            )
        }
    }
    return errors
}

/**
 * Alphabet integrity for JAR / analyze / check composition roots:
 * - static single-occurrence ([staticOccurrenceErrors])
 * - ordinary multi-class shape checks ([actionShapeErrors]) for non-hidden leftovers
 * - at most one service per action name (always when this pass runs)
 * - when [requireCompleteSync] is true (top-level JAR `compile` target only):
 *   every external client must have a service of the same name.
 *
 * Incomplete sync of `client` and ordinary (default) actions is allowed on
 * intermediate assemblies and on all TLA+ targets; only the compiled top-level JAR
 * process must be fully synced (dangling clients → error; leftover ordinary →
 * warning via [unsyncedOrdinaryWarnings]).
 *
 * Same-class ordinary duplicates are reported eagerly in [composeAlphabets] (JAR path).
 */
fun alphabetIntegrityErrors(
    alphabet: CompositionAlphabetResult,
    requireCompleteSync: Boolean = true,
    leafOffersByPclass: Map<String, List<AlphabetOffer>> = emptyMap(),
): List<CompileError> {
    val staticErrs = if (leafOffersByPclass.isNotEmpty()) {
        staticOccurrenceErrors(alphabet.leafOccurrences, leafOffersByPclass)
    } else {
        emptyList()
    }
    val shapeErrs = actionShapeErrors(alphabet.allOffers)
    val errors = mutableListOf<CompileError>()
    errors += staticErrs
    errors += shapeErrs

    val external = alphabet.external
    val serviceCountByName = external.filter { it.isService }.groupBy { it.name }
    for ((name, offers) in serviceCountByName) {
        val services = offers.distinctBy { it.occurrenceId }
        if (services.size >= 2) {
            val locs = services.map { it.loc }
            errors += if (locs.size >= 2) {
                TwoLocsCompileError(
                    locs[0],
                    locs[1],
                    "Action \"$name\" has more than one service in the composition",
                )
            } else {
                OneLocCompileError(
                    locs[0],
                    "Action \"$name\" has more than one service in the composition",
                )
            }
        }
    }

    if (!requireCompleteSync) return errors

    val serviceNames = serviceCountByName.keys
    for ((name, clients) in external.filter { it.isClient }.groupBy { it.name }) {
        if (name !in serviceNames) {
            errors += OneLocCompileError(
                clients.first().loc,
                "Action \"$name\" is tagged `client` but has no `service` in the composition",
            )
        }
    }
    return errors
}

/**
 * Parent/child rules on a whole composition alphabet:
 * - ambiguity: a parent action that could pair with two child classes, or with a child
 *   and a non-child class
 *
 * Child constructors syncing with a non-parent are reported while composing ([composeAlphabets]).
 */
fun childSyncErrors(alphabet: CompositionAlphabetResult): List<CompileError> {
    val errors = mutableListOf<CompileError>()
    val syncable = alphabet.allOffers.filter { !it.isLocal && it.name != "initially" }
    val byName = syncable.groupBy { it.name }
    for ((name, offers) in byName) {
        val parents = offers.filter { o -> offers.any { it.parentClass == o.pclassKey && it.pclassKey != o.pclassKey } }
            .map { it.pclassKey }.distinct()
        for (parent in parents) {
            val parentOffers = offers.filter { it.pclassKey == parent }
            val parentParents = parentOffers.mapNotNull { it.parentClass }.toSet()
            val peers = offers.filter { o ->
                o.pclassKey != parent &&
                    parentOffers.any { p -> canPair(p, o) } &&
                    (o.isStatic || o.parentClass == parent || o.pclassKey in parentParents)
            }.distinctBy { it.pclassKey }
            if (peers.size >= 2 && peers.any { it.parentClass == parent }) {
                errors += TwoLocsCompileError(
                    peers[0].loc,
                    peers[1].loc,
                    "Action \"$name\" of parent \"$parent\" is ambiguous: it could pair with " +
                        peers.joinToString(" and ") { "\"${it.pclassKey}\"" } + "; rename one side",
                )
            }
        }
    }
    return errors.distinctBy { it.toString() }
}

/** Service pairs with client, ordinary with ordinary; two clients (or two services) never sync. */
private fun canPair(a: AlphabetOffer, b: AlphabetOffer): Boolean = when {
    a.isService -> b.isClient
    a.isClient -> b.isService
    else -> !b.isService && !b.isClient
}

/**
 * Compiled-artifact rule: every `B<A>` occurrence needs at least one occurrence of `A`
 * in the flattened composition.
 */
fun childParentPresenceErrors(alphabet: CompositionAlphabetResult, rootName: String): List<CompileError> {
    val classes = alphabet.leafOccurrences.map { it.pclassName }.toSet()
    return alphabet.leafOccurrences
        .filter { it.parentClass != null && it.parentClass !in classes }
        .distinctBy { it.pclassName to it.parentClass }
        .map { occ ->
            val loc = alphabet.allOffers.firstOrNull { it.occurrenceId == occ.occurrenceId }?.loc
                ?: alphabet.allOffers.firstOrNull()?.loc
                ?: julay.compiler.SourceLoc(0 to 0)
            OneLocCompileError(
                loc,
                "Child \"${occ.pclassName}<${occ.parentClass}>\" in \"$rootName\" requires its parent " +
                    "\"${occ.parentClass}\" in the same composition",
            )
        }
}
