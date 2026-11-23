package julay.compiler.pass

import julay.compiler.ast.*
import julay.compiler.decl.TransitUpdate
import julay.compiler.decl.assignedRoots
import julay.program.type.ObjClassType
import julay.program.type.Type

/**
 * Per-state-var field projection (`field-vars`): when a retained obj state var is only
 * read through a single field, emit that field as `var_field` (e.g. `me` + `.url` → `me_url`).
 */
data class FieldVarProjection(
    val leafClass: String,
    val varName: String,
    val keptField: String,
    val fieldType: Type,
    /** Declared fields other than [keptField], declaration order. */
    val omittedFields: List<String>,
) {
    val projectedName: String get() = "${varName}_$keptField"

    fun initComment(): String =
        "\\* field-vars: $varName → $projectedName (omitted ${omittedFields.joinToString(", ")})"
}

class TlaFieldVars internal constructor(
    val projecting: Boolean,
    private val byLeafVar: Map<Pair<String, String>, FieldVarProjection>,
) {
    fun projection(leafClass: String, varName: String): FieldVarProjection? =
        if (!projecting) null else byLeafVar[leafClass to varName]

    fun projectionsForLeaf(leafClass: String): List<FieldVarProjection> =
        if (!projecting) {
            emptyList()
        } else {
            byLeafVar.values.filter { it.leafClass == leafClass }
        }

    fun effectiveType(leafClass: String, varName: String, original: Type): Type =
        projection(leafClass, varName)?.fieldType ?: original

    companion object {
        val IDENTITY = TlaFieldVars(projecting = false, byLeafVar = emptyMap())
    }
}

/** Compilation-scoped field-var projection for Init / expr emit. */
internal object TlaFieldVarProjection {
    private val current = ThreadLocal.withInitial { TlaFieldVars.IDENTITY }

    fun get(): TlaFieldVars = current.get()

    fun set(vars: TlaFieldVars) {
        current.set(vars)
    }
}

/**
 * Analyze sole-field projections for relevant obj state vars.
 * Uses the same emitted-offer relevance as [analyzeTlaRelevantVars].
 */
internal fun analyzeTlaFieldVars(
    pclasses: Map<String, ProcClassNode>,
    offers: List<TlaActionOffer>,
    usedFuns: Collection<FunNode>,
    invClosure: List<InvariantNode>,
    relevantVars: TlaRelevantVars,
    initExprs: List<Pair<String, ExprNode>> = emptyList(),
): TlaFieldVars {
    val varsByLeaf = pclasses.mapValues { (_, pc) ->
        pc.localDecls().filterIsInstance<VarNode>().map { it.name }.toSet()
    }
    val varType = mutableMapOf<Pair<String, String>, Type>()
    pclasses.forEach { (leafName, pc) ->
        pc.localDecls().filterIsInstance<VarNode>().forEach { vn ->
            try {
                varType[leafName to vn.name] = vn.type
            } catch (_: RuntimeException) {
            }
        }
    }
    val fieldReads = mutableMapOf<Pair<String, String>, MutableSet<String>>()
    val wholeReads = mutableSetOf<Pair<String, String>>()

    fun offerHasRelevantUpdate(offer: TlaActionOffer, relevant: Set<Pair<String, String>>): Boolean {
        if (offer.isConstructor) return true
        if (offer.decl.isReturn) return true
        return offer.decl.transits.any { update ->
            update.assignedRoots().any { root -> relevant.contains(offer.leaf.name to root) }
        }
    }

    fun groupHasUpdate(
        group: List<TlaActionOffer>,
        requireRelevant: Boolean,
        relevant: Set<Pair<String, String>>,
    ): Boolean =
        group.any { offer ->
            if (offer.isConstructor || offer.decl.isReturn) return@any true
            offer.decl.transits.any { update ->
                val roots = update.assignedRoots()
                roots.isNotEmpty() && (!requireRelevant || roots.any { relevant.contains(offer.leaf.name to it) })
            }
        }

    // Reuse unused-vars relevance seed so we walk the same offer groups.
    val relevant = mutableSetOf<Pair<String, String>>()
    pclasses.keys.forEach { leaf ->
        varsByLeaf[leaf].orEmpty().forEach { v ->
            if (relevantVars.isRelevant(leaf, v)) relevant += leaf to v
        }
    }

    fun seedAndPropagate(groups: List<List<TlaActionOffer>>) {
        fun walkReads(expr: ExprNode, leaf: String?, bound: Set<String>) {
            collectObjFieldReads(expr, leaf, varsByLeaf, varType, bound, fieldReads, wholeReads)
        }
        groups.forEach { group ->
            group.forEach { offer ->
                val bound = offer.decl.action.args.map { it.name }.toSet()
                offer.decl.guards.forEach { walkReads(it, offer.leaf.name, bound) }
                offer.decl.errors.forEach { arm -> walkReads(arm.condExpr(), offer.leaf.name, bound) }
            }
        }
        invClosure.forEach { walkReads(it.invariantFormula(), null, emptySet()) }
        initExprs.forEach { (leafName, expr) -> walkReads(expr, leafName, emptySet()) }
        usedFuns.forEach { fn ->
            val bound = fn.funArgs().actionArgs().map { it.name }.toSet()
            walkReads(fn.funBody(), null, bound)
        }
        var changed = true
        var steps = 0
        while (changed && steps++ < 64) {
            changed = false
            val before = fieldReads.values.sumOf { it.size } + wholeReads.size
            groups.forEach { group ->
                group.forEach { offer ->
                    if (!offerHasRelevantUpdate(offer, relevant) &&
                        !offer.isConstructor &&
                        !offer.decl.isReturn
                    ) {
                        return@forEach
                    }
                    val bound = offer.decl.action.args.map { it.name }.toMutableSet()
                    offer.decl.actionLets.forEach { al ->
                        walkReads(al.init, offer.leaf.name, bound)
                        bound += al.name
                    }
                    fun walkUpdate(update: TransitUpdate) {
                        when (update) {
                            is TransitUpdate.Let -> {
                                walkReads(update.init, offer.leaf.name, bound)
                                bound += update.name
                            }
                            is TransitUpdate.Assign -> {
                                if (relevant.contains(offer.leaf.name to update.transitRootVar())) {
                                    // RHS may read other vars' fields; do not count as field read on LHS.
                                    walkReads(update.expr, offer.leaf.name, bound)
                                }
                            }
                            is TransitUpdate.IndexPut -> {
                                if (relevant.contains(offer.leaf.name to update.transitRootVar())) {
                                    walkReads(update.index, offer.leaf.name, bound)
                                    walkReads(update.value, offer.leaf.name, bound)
                                }
                            }
                            is TransitUpdate.If -> {
                                walkReads(update.cond, offer.leaf.name, bound)
                                update.thenUpdates.forEach { walkUpdate(it) }
                                update.elseUpdates.forEach { walkUpdate(it) }
                            }
                        }
                    }
                    offer.decl.transits.forEach { walkUpdate(it) }
                }
            }
            if (fieldReads.values.sumOf { it.size } + wholeReads.size != before) changed = true
        }
    }

    val lists = collectEmittedOfferLists(offers, pclasses)
    var groups = lists.filter { groupHasUpdate(it, requireRelevant = false, relevant) }
    var steps = 0
    while (steps++ < 8) {
        val snapFields = fieldReads.mapValues { it.value.toSet() }
        val snapWhole = wholeReads.toSet()
        seedAndPropagate(groups)
        val nextGroups = lists.filter { groupHasUpdate(it, requireRelevant = true, relevant) }
        if (fieldReads.mapValues { it.value.toSet() } == snapFields &&
            wholeReads == snapWhole &&
            nextGroups == groups
        ) {
            break
        }
        groups = nextGroups
    }

    val out = linkedMapOf<Pair<String, String>, FieldVarProjection>()
    relevant.forEach { (leaf, varName) ->
        if (leaf to varName in wholeReads) return@forEach
        val ty = varType[leaf to varName] as? ObjClassType ?: return@forEach
        val used = fieldReads[leaf to varName].orEmpty()
        if (used.size != 1) return@forEach
        val kept = used.single()
        val fieldTy = ty.fields.firstOrNull { it.name == kept }?.type ?: return@forEach
        val omitted = ty.fields.map { it.name }.filter { it != kept }
        if (omitted.isEmpty()) return@forEach // already a single-field obj; unwrap-singletons handles
        out[leaf to varName] = FieldVarProjection(
            leafClass = leaf,
            varName = varName,
            keptField = kept,
            fieldType = fieldTy,
            omittedFields = omitted,
        )
    }
    return TlaFieldVars(projecting = true, byLeafVar = out)
}

/**
 * Record obj-field projections and whole-var reads of obj state.
 * [currentLeaf] null in invariants: resolve `Leaf.var` / binder bases via [varsByLeaf] keys.
 */
private fun collectObjFieldReads(
    expr: ExprNode,
    currentLeaf: String?,
    varsByLeaf: Map<String, Set<String>>,
    varType: Map<Pair<String, String>, Type>,
    bound: Set<String>,
    fieldReads: MutableMap<Pair<String, String>, MutableSet<String>>,
    wholeReads: MutableSet<Pair<String, String>>,
) {
    val leaves = varsByLeaf.keys
    fun markField(leaf: String, varName: String, field: String) {
        if (varName !in varsByLeaf[leaf].orEmpty()) return
        if (varType[leaf to varName] !is ObjClassType) return
        fieldReads.getOrPut(leaf to varName) { mutableSetOf() }.add(field)
    }
    fun markWhole(leaf: String, varName: String) {
        if (varName !in varsByLeaf[leaf].orEmpty()) return
        if (varType[leaf to varName] !is ObjClassType) return
        wholeReads += leaf to varName
    }
    fun walk(e: ExprNode, boundNow: Set<String>) {
        when (e) {
            is SymbolValueExprNode -> {
                val s = e.symbol
                if (s !in boundNow && currentLeaf != null) markWhole(currentLeaf, s)
            }
            is ThisAccessExprNode -> {
                if (currentLeaf == null) return
                val rest = e.nestedFieldPath()
                if (rest.isEmpty()) {
                    markWhole(currentLeaf, e.stateVarName())
                } else {
                    markField(currentLeaf, e.stateVarName(), rest.first())
                }
            }
            is FieldAccessExprNode -> {
                val base = e.baseSymbol
                val path = e.fieldPath
                when {
                    base in leaves && path.isNotEmpty() -> {
                        // Peer.leafVar.field… or Peer.var
                        val varName = path.first()
                        if (path.size == 1) {
                            markWhole(base, varName)
                        } else {
                            markField(base, varName, path[1])
                        }
                    }
                    currentLeaf != null && base !in boundNow && path.isNotEmpty() ->
                        markField(currentLeaf, base, path.first())
                    currentLeaf != null && base !in boundNow ->
                        markWhole(currentLeaf, base)
                }
                e.children.filterIsInstance<ExprNode>().forEach { walk(it, boundNow) }
            }
            is MemberAccessExprNode -> {
                when (val base = e.baseExpr) {
                    is SymbolValueExprNode -> {
                        val s = base.symbol
                        when {
                            s in leaves -> markWhole(s, e.fieldName)
                            currentLeaf != null && s !in boundNow ->
                                markField(currentLeaf, s, e.fieldName)
                            else -> walk(base, boundNow)
                        }
                    }
                    is IndexExprNode -> {
                        val idxBase = base.base
                        if (idxBase is IndexExprNode && idxBase.base is SymbolValueExprNode) {
                            val s = (idxBase.base as SymbolValueExprNode).symbol
                            when {
                                s in leaves -> {
                                    markWhole(s, e.fieldName)
                                    walk(idxBase.index, boundNow)
                                    walk(base.index, boundNow)
                                }
                                currentLeaf != null && s !in boundNow -> {
                                    markField(currentLeaf, s, e.fieldName)
                                    walk(idxBase.index, boundNow)
                                    walk(base.index, boundNow)
                                }
                                else -> walk(base, boundNow)
                            }
                        } else if (idxBase is SymbolValueExprNode) {
                            val s = idxBase.symbol
                            when {
                                s in leaves -> {
                                    // Leaf[i].stateVar — whole state var on peer leaf
                                    markWhole(s, e.fieldName)
                                    walk(base.index, boundNow)
                                }
                                currentLeaf != null && s !in boundNow -> {
                                    markField(currentLeaf, s, e.fieldName)
                                    walk(base.index, boundNow)
                                }
                                else -> walk(base, boundNow)
                            }
                        } else {
                            walk(base, boundNow)
                        }
                    }
                    else -> walk(base, boundNow)
                }
            }
            is FieldAccessOnExprNode -> {
                when (val base = e.baseExpr) {
                    is SymbolValueExprNode -> {
                        val s = base.symbol
                        if (currentLeaf != null && s !in boundNow && e.fieldPath.isNotEmpty()) {
                            markField(currentLeaf, s, e.fieldPath.first())
                        } else {
                            walk(base, boundNow)
                        }
                    }
                    is IndexExprNode -> {
                        val idxBase = base.base
                        if (idxBase is SymbolValueExprNode) {
                            val s = idxBase.symbol
                            if (currentLeaf != null && s !in boundNow && e.fieldPath.isNotEmpty()) {
                                markField(currentLeaf, s, e.fieldPath.first())
                                walk(base.index, boundNow)
                            } else {
                                walk(base, boundNow)
                            }
                        } else {
                            walk(base, boundNow)
                        }
                    }
                    else -> walk(base, boundNow)
                }
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
                val body = e.hofBodyOrNull()
                val names = e.hofParamNamesOrNull()
                if (body != null && names != null) {
                    walk(body, boundNow + names.toSet())
                }
            }
            else -> e.children.filterIsInstance<ExprNode>().forEach { walk(it, boundNow) }
        }
    }
    walk(expr, bound)
}
