package julay.compiler.pass

import julay.compiler.CompileError
import julay.compiler.OneLocCompileError
import julay.compiler.ProgramLoc
import julay.compiler.TwoLocsCompileError
import julay.compiler.ast.RootNode
import julay.compiler.decl.ActionDecl
import julay.compiler.decl.ProcClassDecl
import julay.compiler.decl.ProcDecl
import julay.program.Variable
import julay.program.action.SymbolicAction
import julay.program.action.TSAction
import julay.program.library.LibraryRegistry

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
 * [occurrenceId] distinguishes multiple occurrences of the same proc class.
 * [introducingAssembly] is the named proc/assembly that introduced this leaf (for TLA naming).
 */
data class AlphabetOffer(
    val pclassKey: String,
    val occurrenceId: String,
    val introducingAssembly: String,
    val name: String,
    val args: List<Variable>,
    val modifier: TSAction.SyncRole,
    val isSession: Boolean,
    val isConstructor: Boolean,
    val loc: ProgramLoc,
    val channelKey: String,
    val compositionHidden: Boolean = false,
    val sourceInternal: Boolean = false,
) {
    val isProvider: Boolean get() = modifier == TSAction.SyncRole.Provider
    val isClient: Boolean get() = modifier == TSAction.SyncRole.Client
    val leafId: LeafActionId get() = LeafActionId(pclassKey, occurrenceId, name, isConstructor)

    fun signatureCompatible(other: AlphabetOffer): Boolean =
        args == other.args && isSession == other.isSession &&
            sourceInternal == other.sourceInternal &&
            // Ordinary/session must agree on modifier; provider↔client may differ.
            (isProvider || other.isProvider || isClient || other.isClient || modifier == other.modifier)

    fun toSymbolicAction(): SymbolicAction =
        SymbolicAction(
            name = name,
            args = args,
            isInternal = sourceInternal,
            isSession = isSession,
            channelKey = channelKey,
        )
}

data class CompositionAlphabetResult(
    /** External alphabet of the composition (not source-internal, not composition-hidden). */
    val external: List<AlphabetOffer>,
    /** All offers including hidden and source-internal (for channel binding / analyze --include-internal). */
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
    val libs = librariesInUse
        .filter { it in procs && LibraryRegistry.isKotlinLibrary(it) }
        .associate { libName ->
            val info = LibraryRegistry.staticInfo(libName)
            // Key by julName (e.g. HttpServer) to match ProcDecl leaves; staticInfo.name may differ.
            val ctorActs = info.constructors.keys
            val alphabet = info.alphabet
            val offers = LibraryRegistry.actionDecls(libName).mapNotNull { decl ->
                when {
                    decl.action in ctorActs -> decl.toAlphabetOffer(libName, isConstructor = true)
                    decl.action in alphabet -> decl.toAlphabetOffer(libName, isConstructor = false)
                    else -> null
                }
            }
            libName to offers
        }
    return julay + libs
}

private fun ProcClassDecl.toAlphabetOffers(): List<AlphabetOffer> =
    transitions.map { it.toAlphabetOffer(name, isConstructor = false) } +
        constructors.map { it.toAlphabetOffer(name, isConstructor = true) }

/** Template offers (no occurrence id yet) for a leaf pclass. */
private fun ActionDecl.toAlphabetOffer(pclassKey: String, isConstructor: Boolean): AlphabetOffer {
    val sourceInternal = modifier == TSAction.SyncRole.Internal
    val channelKey = if (sourceInternal) {
        "$pclassKey#internal#${action.name}"
    } else {
        action.name
    }
    return AlphabetOffer(
        pclassKey = pclassKey,
        occurrenceId = "",
        introducingAssembly = pclassKey,
        name = action.name,
        args = action.args,
        modifier = modifier,
        isSession = isSession,
        isConstructor = isConstructor,
        loc = loc,
        channelKey = channelKey,
        compositionHidden = false,
        sourceInternal = sourceInternal,
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
 * keys to composition-hidden syncs so nested assemblies do not cross-sync on the same surface name.
 * Expands named assemblies by occurrence (`M || M` → two independent expansions).
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
    ): List<AlphabetOffer> {
        val occurrenceId = freshOccurrenceId(pclass)
        leafOccurrences += LeafOccurrence(pclass, occurrenceId, introducingAssembly)
        val templates = leafOffersByPclass[pclass] ?: emptyList()
        val offers = templates.map { template ->
            val channelKey = if (template.sourceInternal) {
                "$occurrenceId#internal#${template.name}"
            } else {
                template.channelKey
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

    val allOffers = alphabetOf(root, root.name, root.name)
    val external = allOffers.filter { !it.sourceInternal && !it.compositionHidden }
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
 * with a private [scopeId]-scoped channel key. Provider/client rules are local to this step.
 * Same proc-class on both sides never syncs (same-class rule).
 */
fun composeAlphabets(
    left: List<AlphabetOffer>,
    right: List<AlphabetOffer>,
    scopeId: String,
): Pair<List<AlphabetOffer>, List<CompileError>> {
    // Source-internal and already composition-hidden offers never re-sync at an outer step;
    // they pass through with their channel keys (binary sync scoping).
    val leftSettled = left.filter { it.sourceInternal || it.compositionHidden }
    val rightSettled = right.filter { it.sourceInternal || it.compositionHidden }
    val leftExt = left.filter { !it.sourceInternal && !it.compositionHidden }
    val rightExt = right.filter { !it.sourceInternal && !it.compositionHidden }

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
                val hasProvider = both.any { it.isProvider }
                val hasClient = both.any { it.isClient }
                if (hasOrdinary && (hasProvider || hasClient)) {
                    val ord = both.first { it.modifier == TSAction.SyncRole.Default }
                    val tagged = both.first { it.isProvider || it.isClient }
                    val tag = if (tagged.isProvider) "provider" else "client"
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
                if (hasProvider) {
                    // Provider stays external (further clients can still sync); clients that meet
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
                // Same class never syncs. Ordinary/session duplicates are an immediate error
                // (a later provider cannot redeem them). Clients still pass through to wait for a provider.
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
                                    "or tag callers `client` with a single `provider` for \"$name\"",
                            ),
                        )
                    }
                    result.addAll(l)
                    result.addAll(r)
                    continue
                }
                // Ordinary (or session): require matching signatures, then hide.
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

/** JAR-target check: leftover ordinary actions in the external alphabet are unsynced. */
fun unsyncedOrdinaryErrors(external: List<AlphabetOffer>): List<CompileError> =
    external
        .filter { offer ->
            offer.name != "initially" &&
                !offer.isProvider &&
                !offer.isClient
        }
        .distinctBy { it.channelKey }
        .map { o ->
            OneLocCompileError(
                o.loc,
                "Action \"${o.name}\" is not synchronized with any peer; " +
                    "tag it `internal` if a solo step is intentional",
            )
        }

/**
 * Alphabet integrity for JAR and TLA+ targets (same checks):
 * - at most one provider per action name
 * - every external client must have a provider of the same name
 *
 * Same-class ordinary duplicates are reported eagerly in [composeAlphabets].
 */
fun alphabetIntegrityErrors(
    alphabet: CompositionAlphabetResult,
): List<CompileError> {
    val external = alphabet.external
    val errors = mutableListOf<CompileError>()

    val providerCountByName = external.filter { it.isProvider }.groupBy { it.name }
    for ((name, offers) in providerCountByName) {
        val providers = offers.distinctBy { it.occurrenceId }
        if (providers.size >= 2) {
            val locs = providers.map { it.loc }
            errors += if (locs.size >= 2) {
                TwoLocsCompileError(
                    locs[0],
                    locs[1],
                    "Action \"$name\" has more than one provider in the composition",
                )
            } else {
                OneLocCompileError(
                    locs[0],
                    "Action \"$name\" has more than one provider in the composition",
                )
            }
        }
    }

    val providerNames = providerCountByName.keys
    for ((name, clients) in external.filter { it.isClient }.groupBy { it.name }) {
        if (name !in providerNames) {
            errors += OneLocCompileError(
                clients.first().loc,
                "Action \"$name\" is tagged `client` but has no `provider` in the composition",
            )
        }
    }
    return errors
}
