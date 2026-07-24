package julay.compiler.analysis

import julay.compiler.ast.ASTNode
import julay.compiler.ast.RootNode
import julay.compiler.decl.ProcDecl
import julay.compiler.decl.ProcDeclType
import julay.compiler.pass.computeCompositionAlphabet
import julay.compiler.pass.leafActionMap
import julay.compiler.pass.procClassPass
import julay.program.action.TSAction
import julay.program.library.LibraryRegistry

private enum class OfferKind { Constructor, Transition }

private data class ListedActionOffer(
    val actionName: String,
    val pclassName: String,
    val kind: OfferKind,
    val modifier: TSAction.SyncRole,
    val modifierLabel: String,
    val isInternal: Boolean,
    val isCompositionHidden: Boolean = false,
    /** Occurrence id when listed from the composition alphabet (empty if class-collapsed). */
    val occurrenceId: String = "",
    /** Composition channel key (shows distinct hidden scopes in --actions-detail). */
    val channelKey: String = "",
    val introducingAssembly: String = "",
) {
    val hideByDefault: Boolean get() = isInternal || isCompositionHidden
}

fun printAnalyzeViews(
    ast: ASTNode,
    scope: ResolvedAnalyzeScope,
    librariesInUse: Set<String>,
    allPClassNames: Set<String>,
    procDecls: List<ProcDecl>,
    options: AnalyzeOptions,
) {
    require(ast is RootNode)

    if (options.json) {
        printAlphabetJson(ast, scope, librariesInUse, procDecls)
        return
    }

    if (options.showTree) {
        printCompositionTrees(
            rootNames = scope.rootNames,
            procDecls = procDecls,
            allPClassNames = allPClassNames,
            librariesInUse = librariesInUse,
        )
    }
    if (options.showActionView) {
        printActionView(ast, scope, librariesInUse, allPClassNames, procDecls, options)
    }
    if (options.showProcView) {
        printPclassView(ast, scope.leafComponents, librariesInUse, procDecls, scope, options)
    }
}

fun printActionView(
    ast: ASTNode,
    scope: ResolvedAnalyzeScope,
    librariesInUse: Set<String>,
    allPClassNames: Set<String>,
    procDecls: List<ProcDecl>,
    options: AnalyzeOptions,
) {
    val unionLeaves = scope.leafComponents
    var offers = collectListedActionOffers(ast, unionLeaves, librariesInUse, procDecls, scope)
    val useAlphabetIntersect =
        (options.scopeIntersect || options.scopeMutual) &&
            options.scopeNames.size > 1 &&
            scope.leafSets.size > 1
    if (useAlphabetIntersect) {
        val root = ast as RootNode
        val alphabets = scope.rootNames.mapIndexed { i, rootName ->
            externalActionNames(root, rootName, scope.leafSets[i], librariesInUse, procDecls)
        }
        val shared = alphabets.reduce { a, b -> a intersect b }
        offers = offers.filter { it.actionName in shared }
    }
    if (options.scopeMutual && options.scopeNames.size > 1 && scope.leafSets.size > 1) {
        // Provider status is program-wide: the provider offer may be outside the selected scopes.
        val providerNames = collectListedActionOffers(
            ast,
            allPClassNames + librariesInUse,
            librariesInUse,
            procDecls,
            scope,
        )
            .filter { it.modifier == TSAction.SyncRole.Provider }
            .map { it.actionName }
            .toSet()
        val offersByAction = offers.groupBy { it.actionName }
        val mutualNames = offersByAction.keys.filter { actionName ->
            scopesMutuallySyncOn(actionName, scope.leafSets, offersByAction[actionName].orEmpty(), providerNames)
        }.toSet()
        offers = offers.filter { it.actionName in mutualNames }
    }
    printActions(filterOffers(offers, options), detail = options.actionsDetail)
}

private fun externalActionNames(
    ast: RootNode,
    rootName: String,
    leaves: Set<String>,
    librariesInUse: Set<String>,
    procDecls: List<ProcDecl>,
): Set<String> {
    val pd = procDecls.firstOrNull { it.name == rootName }
        ?: ProcDecl(rootName, emptyList(), ProcDeclType.Proc)
    val leafMap = leafActionMap(ast, leaves, librariesInUse)
    return computeCompositionAlphabet(pd, procDecls, leafMap).external.map { it.name }.toSet()
}

/**
 * True when every pair of selected scopes can sync on [actionName].
 * Clients of a provider action do not sync with each other.
 */
private fun scopesMutuallySyncOn(
    actionName: String,
    leafSets: List<Set<String>>,
    actionOffers: List<ListedActionOffer>,
    providerNames: Set<String>,
): Boolean {
    val rolesPerScope = leafSets.map { leaves ->
        actionOffers
            .filter { it.pclassName in leaves }
            .map { it.modifier }
            .toSet()
    }
    for (i in rolesPerScope.indices) {
        for (j in i + 1 until rolesPerScope.size) {
            if (!scopesSyncOn(rolesPerScope[i], rolesPerScope[j], actionName in providerNames)) {
                return false
            }
        }
    }
    return true
}

/** Whether two scopes' role sets for one action form a valid sync pair. */
private fun scopesSyncOn(
    rolesA: Set<TSAction.SyncRole>,
    rolesB: Set<TSAction.SyncRole>,
    hasProvider: Boolean,
): Boolean {
    if (hasProvider) {
        val aProvider = TSAction.SyncRole.Provider in rolesA
        val bProvider = TSAction.SyncRole.Provider in rolesB
        val aClient = TSAction.SyncRole.Client in rolesA
        val bClient = TSAction.SyncRole.Client in rolesB
        return (aProvider && bClient) || (bProvider && aClient)
    }
    // Ordinary pairwise rendezvous: Default (or session, still Default role) peers.
    return TSAction.SyncRole.Default in rolesA && TSAction.SyncRole.Default in rolesB
}

fun printPclassView(
    ast: ASTNode,
    procs: Set<String>,
    librariesInUse: Set<String>,
    procDecls: List<ProcDecl>,
    scope: ResolvedAnalyzeScope,
    options: AnalyzeOptions,
) {
    require(ast is RootNode)
    val offers = filterOffers(
        collectListedActionOffers(ast, procs, librariesInUse, procDecls, scope),
        options,
    )
    val pclassNames = collectPclassNames(ast, procs, librariesInUse)
    if (options.procsDetail) {
        printPclassDetail(offers, pclassNames)
    } else {
        pclassNames.forEach { println(it) }
    }
}

private fun collectPclassNames(
    ast: RootNode,
    procs: Set<String>,
    librariesInUse: Set<String>,
): List<String> {
    val julayNames = ast.declNodes()
        .flatMap { it.procClassPass(procs) }
        .map { it.name }
    val libNames = librariesInUse
        .filter { it in procs && LibraryRegistry.isKotlinLibrary(it) }
        .map { LibraryRegistry.staticInfo(it).name }
    return (julayNames + libNames).toSortedSet().toList()
}

/**
 * List action offers from the composition alphabet so each leaf occurrence appears separately
 * (with distinct composition-hidden channel keys when scopes differ).
 */
private fun collectListedActionOffers(
    ast: ASTNode,
    procs: Set<String>,
    librariesInUse: Set<String>,
    procDecls: List<ProcDecl>,
    scope: ResolvedAnalyzeScope,
): List<ListedActionOffer> {
    require(ast is RootNode)
    val offers = mutableListOf<ListedActionOffer>()
    val seen = mutableSetOf<String>() // occurrenceId + action + ctor bit
    scope.rootNames.forEachIndexed { i, rootName ->
        val leaves = scope.leafSets.getOrElse(i) { scope.leafComponents }
        val pd = procDecls.firstOrNull { it.name == rootName }
            ?: ProcDecl(rootName, emptyList(), ProcDeclType.Proc)
        val leafMap = leafActionMap(ast, leaves, librariesInUse)
        val alphabet = computeCompositionAlphabet(pd, procDecls, leafMap)
        for (o in alphabet.allOffers) {
            if (o.pclassKey !in procs && o.pclassKey !in librariesInUse) continue
            val key = "${o.occurrenceId}\u0000${o.name}\u0000${o.isConstructor}"
            if (!seen.add(key)) continue
            val kind = if (o.isConstructor) OfferKind.Constructor else OfferKind.Transition
            offers += ListedActionOffer(
                actionName = o.name,
                pclassName = o.pclassKey,
                kind = kind,
                modifier = o.modifier,
                modifierLabel = o.modifierLabel(kind),
                isInternal = o.sourceInternal,
                isCompositionHidden = o.compositionHidden,
                occurrenceId = o.occurrenceId,
                channelKey = o.channelKey,
                introducingAssembly = o.introducingAssembly,
            )
        }
    }
    return offers
}

private fun julay.compiler.pass.AlphabetOffer.modifierLabel(kind: OfferKind): String {
    val kindStr = when (kind) {
        OfferKind.Constructor -> "constructor"
        OfferKind.Transition -> "transition"
    }
    return when {
        isSession -> "session $kindStr"
        modifier == TSAction.SyncRole.Provider -> "provider $kindStr"
        modifier == TSAction.SyncRole.Client -> "client $kindStr"
        modifier == TSAction.SyncRole.Internal || sourceInternal -> "internal $kindStr"
        else -> kindStr
    }
}

private fun filterOffers(
    offers: List<ListedActionOffer>,
    options: AnalyzeOptions,
): List<ListedActionOffer> {
    val withoutHidden = if (options.includeInternal) {
        offers
    } else {
        offers.filterNot { it.hideByDefault }
    }
    val exactNames = options.actionNames.toSet()
    val regex = options.actionRegex?.let { Regex(it) }
    if (exactNames.isEmpty() && regex == null) {
        return withoutHidden
    }
    return withoutHidden.filter { offer ->
        offer.actionName in exactNames || (regex != null && regex.containsMatchIn(offer.actionName))
    }
}

private fun printActions(offers: List<ListedActionOffer>, detail: Boolean) {
    val byAction = offers.groupBy { it.actionName }.toSortedMap()
    if (!detail) {
        byAction.keys.forEach { println(it) }
        return
    }
    byAction.forEach { (actionName, actionOffers) ->
        println(actionName)
        actionOffers
            .sortedWith(
                compareBy(
                    { it.pclassName },
                    { it.introducingAssembly },
                    { it.occurrenceId },
                    { it.kind.name },
                    { it.modifierLabel },
                    { it.channelKey },
                ),
            )
            .forEach { offer ->
                val occ =
                    if (offer.introducingAssembly.isNotEmpty() &&
                        offer.introducingAssembly != offer.pclassName
                    ) {
                        " (from ${offer.introducingAssembly})"
                    } else {
                        ""
                    }
                val scope = when {
                    offer.isCompositionHidden &&
                        offer.channelKey.isNotEmpty() &&
                        offer.channelKey != offer.actionName ->
                        " [composition-hidden scope=${offer.channelKey}]"
                    offer.isCompositionHidden -> " [composition-hidden]"
                    else -> ""
                }
                println("  ${offer.pclassName}$occ: ${offer.modifierLabel}$scope")
            }
    }
}

private fun printPclassDetail(offers: List<ListedActionOffer>, pclassNames: List<String>) {
    val byPclass = offers.groupBy { it.pclassName }
    pclassNames.forEach { pclass ->
        println(pclass)
        byPclass[pclass].orEmpty()
            .sortedWith(compareBy({ it.actionName }, { it.kind.name }, { it.modifierLabel }))
            .forEach { offer ->
                println("  ${offer.actionName}: ${offer.modifierLabel}")
            }
    }
}
