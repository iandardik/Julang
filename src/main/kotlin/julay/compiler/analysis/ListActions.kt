package julay.compiler.analysis

import julay.compiler.ast.ASTNode
import julay.compiler.ast.RootNode
import julay.compiler.decl.ActionDecl
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
        // Serviced status is program-wide: the service offer may be outside the selected scopes.
        val servicedNames = collectListedActionOffers(
            ast,
            allPClassNames + librariesInUse,
            librariesInUse,
            procDecls,
            scope,
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

private fun compositionHiddenIds(
    ast: RootNode,
    procDecls: List<ProcDecl>,
    scope: ResolvedAnalyzeScope,
    librariesInUse: Set<String>,
): Set<Pair<String, String>> {
    val hidden = mutableSetOf<Pair<String, String>>()
    scope.rootNames.forEachIndexed { i, rootName ->
        val leaves = scope.leafSets.getOrElse(i) { scope.leafComponents }
        val pd = procDecls.firstOrNull { it.name == rootName }
            ?: ProcDecl(rootName, emptyList(), ProcDeclType.Proc)
        val leafMap = leafActionMap(ast, leaves, librariesInUse)
        computeCompositionAlphabet(pd, procDecls, leafMap).allOffers
            .filter { it.compositionHidden }
            .forEach { hidden.add(it.pclassKey to it.name) }
    }
    return hidden
}

private fun collectListedActionOffers(
    ast: ASTNode,
    procs: Set<String>,
    librariesInUse: Set<String>,
    procDecls: List<ProcDecl>,
    scope: ResolvedAnalyzeScope,
): List<ListedActionOffer> {
    require(ast is RootNode)
    val hidden = compositionHiddenIds(ast, procDecls, scope, librariesInUse)
    val progOffers = ast.declNodes()
        .flatMap { it.procClassPass(procs) }
        .flatMap { pc ->
            pc.transitions.map { it.toListedOffer(pc.name, OfferKind.Transition, hidden) } +
                pc.constructors.map { it.toListedOffer(pc.name, OfferKind.Constructor, hidden) }
        }
    val libOffers = librariesInUse
        .filter { it in procs && LibraryRegistry.isKotlinLibrary(it) }
        .flatMap { libName ->
            val info = LibraryRegistry.staticInfo(libName)
            val ctorActs = info.constructors.keys
            val alphabet = info.alphabet
            LibraryRegistry.actionDecls(libName).mapNotNull { decl ->
                when {
                    decl.action in ctorActs -> decl.toListedOffer(libName, OfferKind.Constructor, hidden)
                    decl.action in alphabet -> decl.toListedOffer(libName, OfferKind.Transition, hidden)
                    else -> null
                }
            }
        }
    return progOffers + libOffers
}

private fun ActionDecl.toListedOffer(
    pclassName: String,
    kind: OfferKind,
    hidden: Set<Pair<String, String>>,
): ListedActionOffer =
    ListedActionOffer(
        actionName = action.name,
        pclassName = pclassName,
        kind = kind,
        modifier = modifier,
        modifierLabel = modifierLabel(kind),
        isInternal = modifier == TSAction.SyncRole.Internal,
        isCompositionHidden = (pclassName to action.name) in hidden,
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
            .sortedWith(compareBy({ it.pclassName }, { it.kind.name }, { it.modifierLabel }))
            .forEach { offer ->
                println("  ${offer.pclassName}: ${offer.modifierLabel}")
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
