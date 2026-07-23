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

/** Identity of a leaf action offer for channel-key assignment. */
data class LeafActionId(
    val pclassKey: String,
    val actionName: String,
    val isConstructor: Boolean,
)

/**
 * One action offer in a (possibly derived) alphabet.
 * [compositionHidden] means the offer was internalized by `||` matching (not source `internal`).
 */
data class AlphabetOffer(
    val pclassKey: String,
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
    val isService: Boolean get() = modifier == TSAction.SyncRole.Service
    val leafId: LeafActionId get() = LeafActionId(pclassKey, name, isConstructor)

    fun signatureCompatible(other: AlphabetOffer): Boolean =
        args == other.args && isSession == other.isSession &&
            sourceInternal == other.sourceInternal &&
            // Non-service ordinary/session must agree on modifier; service↔consumer may differ.
            (isService || other.isService || modifier == other.modifier)

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
    val errors: List<CompileError>,
)

private var scopeCounter = 0

private fun freshScopeId(hint: String): String {
    scopeCounter += 1
    val safe = hint.ifEmpty { "comp" }.replace(Regex("[^A-Za-z0-9_]"), "_")
    return "${safe}_$scopeCounter"
}

/** Reset between compilations so codegen strings stay stable within a run when desired. */
fun resetCompositionScopeCounter() {
    scopeCounter = 0
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

private fun ActionDecl.toAlphabetOffer(pclassKey: String, isConstructor: Boolean): AlphabetOffer {
    val sourceInternal = modifier == TSAction.SyncRole.Internal
    val channelKey = if (sourceInternal) {
        "$pclassKey#internal#${action.name}"
    } else {
        action.name
    }
    return AlphabetOffer(
        pclassKey = pclassKey,
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
 * Compute the inductive alphabet of [root] (a JAR/analyze target), assigning private channel keys
 * to composition-hidden syncs so nested assemblies do not cross-sync on the same surface name.
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

    fun recordKeys(offers: List<AlphabetOffer>) {
        offers.forEach { offer ->
            if (!offer.sourceInternal || offer.compositionHidden) {
                // Always record; source-internal already has a unique key.
            }
            channelKeys[offer.leafId] = offer.channelKey
        }
    }

    fun resolve(pd: ProcDecl): ProcDecl = procDeclMap[pd.name] ?: pd

    fun alphabetOf(pd: ProcDecl, scopeHint: String): List<AlphabetOffer> {
        val resolved = resolve(pd)
        if (resolved.components.isEmpty()) {
            val offers = leafOffersByPclass[resolved.name]
                ?: leafOffersByPclass[pd.name]
                ?: emptyList()
            recordKeys(offers)
            return offers
        }
        val childAlphabets = resolved.components.map { child ->
            alphabetOf(child, child.name)
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

    val allOffers = alphabetOf(root, root.name)
    val external = allOffers.filter { !it.sourceInternal && !it.compositionHidden }
    return CompositionAlphabetResult(external, allOffers, channelKeys.toMap(), errors)
}

/**
 * Compose two alphabets under [scopeId]. Matching non-service (incl. session) pairs become
 * composition-hidden with a private [scopeId]-scoped channel key. Services always escape.
 */
fun composeAlphabets(
    left: List<AlphabetOffer>,
    right: List<AlphabetOffer>,
    scopeId: String,
): Pair<List<AlphabetOffer>, List<CompileError>> {
    // Source-internal never participates in cross-proc sync; always keep as-is (unique channels).
    val leftInternal = left.filter { it.sourceInternal }
    val rightInternal = right.filter { it.sourceInternal }
    val leftExt = left.filter { !it.sourceInternal }
    val rightExt = right.filter { !it.sourceInternal }

    val errors = mutableListOf<CompileError>()
    val result = mutableListOf<AlphabetOffer>()
    result.addAll(leftInternal)
    result.addAll(rightInternal)

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
                val anyService = (l + r).any { it.isService }
                if (anyService) {
                    // Services escape; provider/consumer modifier mismatch is allowed.
                    result.addAll(l)
                    result.addAll(r)
                    continue
                }
                // Non-service (ordinary or session): require matching signatures, then hide.
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
                // Kotlin libraries ship fixed SymbolicAction channel keys (= name). Keep the
                // public name when either peer is a Kotlin lib so channels still match.
                val touchesKotlinLib =
                    (l + r).any { LibraryRegistry.isKotlinLibrary(it.pclassKey) }
                val hiddenKey = if (touchesKotlinLib) name else "$scopeId#$name"
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

/** JAR-target check: leftover non-service actions in the external alphabet are unsynced. */
fun unsyncedNonServiceErrors(external: List<AlphabetOffer>): List<CompileError> {
    val servicedNames = external.filter { it.isService }.map { it.name }.toSet()
    return external
        .filter { offer ->
            offer.name != "initially" &&
                !offer.isService &&
                offer.name !in servicedNames
        }
        .distinctBy { it.channelKey }
        .map { o ->
            OneLocCompileError(
                o.loc,
                "Action \"${o.name}\" is non-service but not synchronized with any peer; " +
                    "tag it `internal` if a solo step is intentional",
            )
        }
}
