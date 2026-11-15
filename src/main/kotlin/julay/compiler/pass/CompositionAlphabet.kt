package julay.compiler.pass

import julay.compiler.CompileError
import julay.compiler.CompileWarning
import julay.compiler.OneLocCompileError
import julay.compiler.OneLocCompileWarning
import julay.compiler.ProgramLoc
import julay.compiler.TwoLocsCompileError
import julay.compiler.ast.ProcClassNode
import julay.compiler.ast.ProcFunNode
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
    val julay = ast.declNodes()
        .flatMap { it.procClassPass(procs) }
        .associate { pc -> pc.name to pc.toAlphabetOffers() }
    // Always index every procfun in the CU so call-site alphabet folding can stamp helpers
    // that are invoked but not listed in ||.
    val procFuns = ast.procFunClassPass()
        .associate { pc -> pc.name to pc.toAlphabetOffers() }
    // Kotlin-native libraries (HttpServer) removed; Go runtime provides ServeHttp.
    return julay + procFuns
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
 *
 * When [ast] is provided, procfuns **called** by host leaves under [root] also contribute their
 * non-synthetic alphabet — even if they are not listed in `||` (TLA coupling still requires `||`).
 */
fun computeCompositionAlphabet(
    root: ProcDecl,
    procDecls: List<ProcDecl>,
    leafOffersByPclass: Map<String, List<AlphabetOffer>>,
    procFunNames: Set<String> = emptySet(),
    ast: RootNode? = null,
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
    ): List<AlphabetOffer> {
        val occurrenceId = freshOccurrenceId(pclass)
        leafOccurrences += LeafOccurrence(pclass, occurrenceId, introducingAssembly)
        val templates = leafOffersByPclass[pclass] ?: emptyList()
        // Composed / call-folded procfuns contribute their non-synthetic alphabet to the parent;
        // *_call / *_ret stay hidden (spawn-await plumbing, not SyncChannel peers).
        // Standalone analyze/compile of the procfun alone still stamps all offers.
        val filtered = if (pclass in procFunNames && root.name != pclass) {
            templates.filterNot { offer ->
                offer.name == procFunCallCtor(pclass) ||
                    offer.name == procFunRetAction(pclass) ||
                    offer.name.startsWith("invoke_")
            }
        } else {
            templates
        }
        val offers = filtered.map { template ->
            val channelKey = when {
                template.sourceInternal -> "$occurrenceId#internal#${template.name}"
                template.isTimer -> "$occurrenceId#timer#${template.name}"
                else -> template.name
            }
            template.copy(
                occurrenceId = occurrenceId,
                introducingAssembly = introducingAssembly,
                channelKey = channelKey,
            )
        }
        recordKeys(offers)
        return offers
    }

    fun alphabetOf(pd: ProcDecl, introducingAssembly: String, scopeHint: String): List<AlphabetOffer> {
        val resolved = resolve(pd)
        if (resolved.components.isEmpty()) {
            return stampLeafOffers(resolved.name, introducingAssembly)
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

    var allOffers = alphabetOf(root, root.name, root.name)

    // Fold alphabets of procfuns called by hosts (and ~> handler refs), even when not listed in ||.
    // Transitive: handleRpc registered on listen also folds inRequestVoteRPC called from its return:.
    if (ast != null && procFunNames.isNotEmpty() && root.name !in procFunNames) {
        val pclasses = ast.declNodes().filterIsInstance<ProcClassNode>().associateBy { it.name() }
        val procFunClasses = ast.declNodes().filterIsInstance<ProcFunNode>()
            .associate { it.stableName() to it.asSyntheticProcClass() }
        val alreadyStamped = leafOccurrences.map { it.pclassName }.toSet()
        val called = calledProcFunNamesUnder(root, procDecls, procFunNames, pclasses, procFunClasses)
            .filter { it !in alreadyStamped }
            .sorted()
        for (pf in called) {
            val pfOffers = stampLeafOffers(pf, pf)
            if (pfOffers.isEmpty()) continue
            val (composed, composeErrors) = composeAlphabets(allOffers, pfOffers, freshScopeId(root.name))
            errors.addAll(composeErrors)
            allOffers = composed
            recordKeys(allOffers)
        }
    }

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
 * Procfun names reachable from non-procfun host leaves under [root]:
 * direct call sites, api-qualified calls, `~>` handler refs, and nested calls inside those procfuns.
 */
internal fun calledProcFunNamesUnder(
    root: ProcDecl,
    procDecls: List<ProcDecl>,
    procFunNames: Set<String>,
    pclasses: Map<String, ProcClassNode>,
    procFunClasses: Map<String, ProcClassNode> = emptyMap(),
): Set<String> {
    val hosts = collectLeafOccurrences(root, procDecls)
        .map { it.pclassName }
        .filter { it !in procFunNames }
        .distinct()
    return reachableProcFunNamesFromHosts(hosts, procFunNames, pclasses, procFunClasses)
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
                // Same class never syncs. Ordinary duplicates are an immediate error
                // (a later service cannot redeem them). Clients still pass through to wait for a service.
                val classes = both.map { it.pclassKey }.toSet()
                if (classes.size == 1) {
                    if (hasOrdinary) {
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
                // Ordinary: dynamic procs may not sync with each other.
                val allDynamic = both.all { !it.isStatic }
                if (allDynamic) {
                    val sample = both[0]
                    val other = both.getOrElse(1) { sample }
                    errors.add(
                        TwoLocsCompileError(
                            sample.loc,
                            other.loc,
                            "Action \"$name\" has an illegal sync shape (dynamic-to-dynamic); " +
                                "dynamic procs may only sync with a static peer (use `service`/`client` for hubs)",
                        ),
                    )
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

private fun findSignatureMismatch(
    left: List<AlphabetOffer>,
    right: List<AlphabetOffer>,
): Pair<AlphabetOffer, AlphabetOffer>? {
    val ref = left[0]
    for (o in left + right) {
        if (!ref.signatureCompatible(o)) return ref to o
    }
    // Also require a single offer per side for pairwise sync (Julay sync size 2).
    if (left.size != 1 || right.size != 1) {
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

/** JAR-target warning: leftover ordinary actions in the external alphabet are unsynced. */
fun unsyncedOrdinaryWarnings(external: List<AlphabetOffer>): List<CompileWarning> =
    external
        .filter { offer ->
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
        if (classes.size <= 1) continue // single class: OK (may warn)
        val staticCount = classes.count { c -> open.any { it.pclassKey == c && it.isStatic } }
        val dynamicCount = classes.size - staticCount
        val sample = open.first()
        val ok = when {
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
                    "exactly two static classes, or a single class",
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
 *   every external client must have a service of the same name, except when
 *   every external client offer for that name comes from a composed procfun —
 *   those stay visible on the parent until a service peer is composed.
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
    procFunNames: Set<String> = emptySet(),
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
            if (clients.all { it.pclassKey in procFunNames }) continue
            errors += OneLocCompileError(
                clients.first().loc,
                "Action \"$name\" is tagged `client` but has no `service` in the composition",
            )
        }
    }
    return errors
}
