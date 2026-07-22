package julay.compiler.analysis

import julay.compiler.ast.ASTNode
import julay.compiler.ast.RootNode
import julay.compiler.decl.ActionDecl
import julay.compiler.decl.ProcDecl
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
)

fun printAnalyzeViews(
    ast: ASTNode,
    scope: ResolvedAnalyzeScope,
    librariesInUse: Set<String>,
    allPClassNames: Set<String>,
    procDecls: List<ProcDecl>,
    options: AnalyzeOptions,
) {
    require(ast is RootNode)

    if (options.showTree) {
        printCompositionTrees(
            rootNames = scope.rootNames,
            procDecls = procDecls,
            allPClassNames = allPClassNames,
            librariesInUse = librariesInUse,
        )
    }
    if (options.showActionView) {
        printActionView(ast, scope, librariesInUse, allPClassNames, options)
    }
    if (options.showProcView) {
        printPclassView(ast, scope.leafComponents, librariesInUse, options)
    }
}

fun printActionView(
    ast: ASTNode,
    scope: ResolvedAnalyzeScope,
    librariesInUse: Set<String>,
    allPClassNames: Set<String>,
    options: AnalyzeOptions,
) {
    val unionLeaves = scope.leafComponents
    var offers = collectListedActionOffers(ast, unionLeaves, librariesInUse)
    val useAlphabetIntersect =
        (options.scopeIntersect || options.scopeMutual) &&
            options.scopeNames.size > 1 &&
            scope.leafSets.size > 1
    if (useAlphabetIntersect) {
        val alphabets = scope.leafSets.map { leaves ->
            collectListedActionOffers(ast, leaves, librariesInUse).map { it.actionName }.toSet()
        }
        val shared = alphabets.reduce { a, b -> a intersect b }
        offers = offers.filter { it.actionName in shared }
    }
    if (options.scopeMutual && options.scopeNames.size > 1 && scope.leafSets.size > 1) {
        // Serviced status is program-wide: the service offer may be outside the selected scopes.
        val servicedNames = collectListedActionOffers(
            ast,
            allPClassNames + librariesInUse,
            librariesInUse,
        )
            .filter { it.modifier == TSAction.SyncRole.Service }
            .map { it.actionName }
            .toSet()
        val offersByAction = offers.groupBy { it.actionName }
        val mutualNames = offersByAction.keys.filter { actionName ->
            scopesMutuallySyncOn(actionName, scope.leafSets, offersByAction[actionName].orEmpty(), servicedNames)
        }.toSet()
        offers = offers.filter { it.actionName in mutualNames }
    }
    printActions(filterOffers(offers, options), detail = options.actionsDetail)
}

/**
 * True when every pair of selected scopes can sync on [actionName].
 * Consumers of a serviced action do not sync with each other.
 */
private fun scopesMutuallySyncOn(
    actionName: String,
    leafSets: List<Set<String>>,
    actionOffers: List<ListedActionOffer>,
    servicedNames: Set<String>,
): Boolean {
    val rolesPerScope = leafSets.map { leaves ->
        actionOffers
            .filter { it.pclassName in leaves }
            .map { it.resolvedRole(servicedNames) }
            .toSet()
    }
    for (i in rolesPerScope.indices) {
        for (j in i + 1 until rolesPerScope.size) {
            if (!scopesSyncOn(rolesPerScope[i], rolesPerScope[j], actionName in servicedNames)) {
                return false
            }
        }
    }
    return true
}

private fun ListedActionOffer.resolvedRole(servicedNames: Set<String>): TSAction.SyncRole =
    when {
        modifier == TSAction.SyncRole.Default && actionName in servicedNames ->
            TSAction.SyncRole.Consumer
        else -> modifier
    }

/** Whether two scopes' role sets for one action form a valid sync pair. */
private fun scopesSyncOn(
    rolesA: Set<TSAction.SyncRole>,
    rolesB: Set<TSAction.SyncRole>,
    serviced: Boolean,
): Boolean {
    if (serviced) {
        val aService = TSAction.SyncRole.Service in rolesA
        val bService = TSAction.SyncRole.Service in rolesB
        val aConsumer = TSAction.SyncRole.Consumer in rolesA
        val bConsumer = TSAction.SyncRole.Consumer in rolesB
        return (aService && bConsumer) || (bService && aConsumer)
    }
    // Non-serviced pairwise rendezvous: Default (or session, still Default role) peers.
    return TSAction.SyncRole.Default in rolesA && TSAction.SyncRole.Default in rolesB
}

fun printPclassView(
    ast: ASTNode,
    procs: Set<String>,
    librariesInUse: Set<String>,
    options: AnalyzeOptions,
) {
    require(ast is RootNode)
    val offers = filterOffers(collectListedActionOffers(ast, procs, librariesInUse), options)
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

private fun collectListedActionOffers(
    ast: ASTNode,
    procs: Set<String>,
    librariesInUse: Set<String>,
): List<ListedActionOffer> {
    require(ast is RootNode)
    val progOffers = ast.declNodes()
        .flatMap { it.procClassPass(procs) }
        .flatMap { pc ->
            pc.transitions.map { it.toListedOffer(pc.name, OfferKind.Transition) } +
                pc.constructors.map { it.toListedOffer(pc.name, OfferKind.Constructor) }
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
                    decl.action in ctorActs -> decl.toListedOffer(key, OfferKind.Constructor)
                    decl.action in alphabet -> decl.toListedOffer(key, OfferKind.Transition)
                    else -> null
                }
            }
        }
    return progOffers + libOffers
}

private fun ActionDecl.toListedOffer(pclassName: String, kind: OfferKind): ListedActionOffer =
    ListedActionOffer(
        actionName = action.name,
        pclassName = pclassName,
        kind = kind,
        modifier = modifier,
        modifierLabel = modifierLabel(kind),
        isInternal = modifier == TSAction.SyncRole.Internal,
    )

private fun ActionDecl.modifierLabel(kind: OfferKind): String {
    val kindStr = when (kind) {
        OfferKind.Constructor -> "constructor"
        OfferKind.Transition -> "transition"
    }
    return when {
        isSession -> "session $kindStr"
        modifier == TSAction.SyncRole.Service -> "service $kindStr"
        modifier == TSAction.SyncRole.Internal -> "internal $kindStr"
        else -> kindStr
    }
}

private fun filterOffers(
    offers: List<ListedActionOffer>,
    options: AnalyzeOptions,
): List<ListedActionOffer> {
    val withoutInternal = if (options.includeInternal) {
        offers
    } else {
        offers.filterNot { it.isInternal }
    }
    val exactNames = options.actionNames.toSet()
    val regex = options.actionRegex?.let { Regex(it) }
    if (exactNames.isEmpty() && regex == null) {
        return withoutInternal
    }
    return withoutInternal.filter { offer ->
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
            .sortedWith(compareBy({ it.pclassName }, { it.kind.name }, { it.modifierLabel }))
            .forEach { offer ->
                println("  ${offer.pclassName}: ${offer.modifierLabel}")
            }
    }
}

private fun printPclassDetail(
    offers: List<ListedActionOffer>,
    pclassNames: List<String>,
) {
    val byPclass = offers.groupBy { it.pclassName }
    pclassNames.forEach { pclassName ->
        println(pclassName)
        val pclassOffers = byPclass[pclassName].orEmpty()
            .sortedWith(compareBy({ it.actionName }, { it.kind.name }, { it.modifierLabel }))
        pclassOffers.forEach { offer ->
            println("  ${offer.actionName}: ${offer.modifierLabel}")
        }
    }
}
