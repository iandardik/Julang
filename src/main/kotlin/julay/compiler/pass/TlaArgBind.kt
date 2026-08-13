package julay.compiler.pass

import julay.compiler.ast.*
import julay.program.type.ListType
import julay.program.type.SetType
import java.util.IdentityHashMap

/**
 * Binder plan for one conjoined TLA action: determined-args + from-collection.
 *
 * Determined args and collection-bound args are omitted from type-domain `\E` params
 * (except set-membership, which keeps the arg with domain `S`). Dropped guard conjuncts
 * are tracked by identity so emit can skip the same [flattenTopLevelAnd] nodes.
 */
internal data class TlaArgBindPlan(
    val skipConjuncts: IdentityHashMap<ExprNode, Boolean>,
    val skipArgs: Set<String>,
    val determined: List<Pair<String, ExprNode>>,
    val listBinds: List<ListIndexBind>,
    val setBinds: List<SetMemberBind>,
    val structBinds: List<StructInSetBind>,
) {
    fun skipped(expr: ExprNode): Boolean = skipConjuncts.containsKey(expr)

    /** Args whose type-domain `\E` should not be collected (determined, list, struct, set-member). */
    fun omitArgTypeDomains(): Set<String> = skipArgs + setBinds.map { it.arg }.toSet()

    fun extraBinderNames(): Set<String> =
        listBinds.map { it.index }.toSet() + structBinds.map { it.tmp }.toSet()

    companion object {
        val EMPTY = TlaArgBindPlan(
            skipConjuncts = IdentityHashMap(),
            skipArgs = emptySet(),
            determined = emptyList(),
            listBinds = emptyList(),
            setBinds = emptyList(),
            structBinds = emptyList(),
        )
    }
}

internal data class ListIndexBind(
    val arg: String,
    val index: String,
    val list: ExprNode,
)

internal data class SetMemberBind(
    val arg: String,
    val set: ExprNode,
)

internal data class StructInSetBind(
    val tmp: String,
    val set: ExprNode,
    val argPaths: List<Pair<String, List<String>>>,
    val keep: List<Pair<List<String>, ExprNode>>,
)

internal fun analyzeTlaArgBind(
    offers: List<TlaActionOffer>,
    config: TlaOptConfig,
): TlaArgBindPlan {
    if (!config.determinedArgs && !config.fromCollection) return TlaArgBindPlan.EMPTY
    val argNames = linkedSetOf<String>()
    offers.forEach { offer ->
        offer.decl.action.args.forEach { arg ->
            if (offerRefsArg(offer, arg.name)) argNames += arg.name
        }
    }
    if (argNames.isEmpty()) return TlaArgBindPlan.EMPTY

    val conjuncts = offers.flatMap { offer ->
        offer.decl.guards.flatMap { flattenTopLevelAnd(it) }
    }
    val bound = linkedMapOf<String, Bound>()
    val conflicted = mutableSetOf<String>()
    val skip = IdentityHashMap<ExprNode, Boolean>()
    val structBinds = mutableListOf<StructInSetBind>()
    val takenStructNames = argNames.toMutableSet()

    fun progress(): Boolean {
        var changed = false
        if (config.determinedArgs) {
            for (c in conjuncts) {
                if (skip.containsKey(c)) continue
                val det = matchDetermined(c, argNames, bound.keys, conflicted) ?: continue
                val (arg, expr) = det
                val existing = bound[arg]
                if (existing is Bound.Determined && !sameDeterminer(existing.expr, expr)) {
                    bound.remove(arg)
                    conflicted += arg
                    continue
                }
                if (arg in conflicted) continue
                if (existing != null && existing !is Bound.Determined) continue
                bound[arg] = Bound.Determined(expr)
                skip[c] = true
                changed = true
            }
        }
        if (config.fromCollection) {
            val unbound = argNames.filter { it !in bound }.toSet()
            for (c in conjuncts) {
                if (skip.containsKey(c)) continue
                matchSetMember(c, unbound)?.let { (arg, setExpr) ->
                    bound[arg] = Bound.SetMember(setExpr)
                    skip[c] = true
                    changed = true
                    return@let
                }
                if (skip.containsKey(c)) continue
                matchListIndex(c, unbound)?.let { (arg, field, listExpr) ->
                    bound[arg] = Bound.ListIndex("${arg}_idx", listExpr)
                    skip[c] = true
                    dropImpliedBounds(conjuncts, skip, arg, field, listExpr)
                    changed = true
                    return@let
                }
                if (skip.containsKey(c)) continue
                matchStructInSet(c, unbound, takenStructNames)?.let { struct ->
                    structBinds += struct
                    struct.argPaths.forEach { (arg, path) ->
                        bound[arg] = Bound.StructField(struct.tmp, path)
                    }
                    skip[c] = true
                    changed = true
                }
            }
        }
        return changed
    }

    var steps = 0
    while (progress() && steps++ < 64) {
        // Fixpoint: determined exprs may mention args bound from collections, and vice versa.
    }

    val determined = bound.entries.mapNotNull { (n, b) ->
        (b as? Bound.Determined)?.let { n to it.expr }
    }
    val listBinds = bound.entries.mapNotNull { (n, b) ->
        (b as? Bound.ListIndex)?.let { ListIndexBind(n, it.index, it.list) }
    }
    val setBinds = bound.entries.mapNotNull { (n, b) ->
        (b as? Bound.SetMember)?.let { SetMemberBind(n, it.set) }
    }
    val skipArgs = bound.entries.mapNotNull { (n, b) ->
        if (b is Bound.SetMember) null else n
    }.toSet()

    return TlaArgBindPlan(
        skipConjuncts = skip,
        skipArgs = skipArgs,
        determined = determined,
        listBinds = listBinds,
        setBinds = setBinds,
        structBinds = structBinds,
    )
}

private sealed class Bound {
    data class Determined(val expr: ExprNode) : Bound()
    data class SetMember(val set: ExprNode) : Bound()
    data class ListIndex(val index: String, val list: ExprNode) : Bound()
    data class StructField(val tmp: String, val path: List<String>) : Bound()
}

private fun unwrapParen(expr: ExprNode): ExprNode =
    if (expr is ParenExprNode) unwrapParen(expr.innerExpr()) else expr

private fun matchDetermined(
    conjunct: ExprNode,
    argNames: Set<String>,
    alreadyBound: Set<String>,
    conflicted: Set<String>,
): Pair<String, ExprNode>? {
    val e = unwrapParen(conjunct)
    if (e !is BinaryOpExprNode) return null
    if (e.op() != "=" && e.op() != "<=>") return null
    val lhs = unwrapParen(e.lhsOperand())
    val rhs = unwrapParen(e.rhsOperand())
    fun oneSide(argSide: ExprNode, other: ExprNode): Pair<String, ExprNode>? {
        val a = (argSide as? SymbolValueExprNode)?.symbol ?: return null
        if (a !in argNames || a in conflicted) return null
        if (exprReferencesSymbol(other, a)) return null
        val unbound = argNames.filter { it != a && it !in alreadyBound }
        if (unbound.any { exprReferencesSymbol(other, it) }) return null
        return a to other
    }
    return oneSide(lhs, rhs) ?: oneSide(rhs, lhs)
}

private fun sameDeterminer(a: ExprNode, b: ExprNode): Boolean = a.toString() == b.toString()

private fun matchSetMember(conjunct: ExprNode, unbound: Set<String>): Pair<String, ExprNode>? {
    val e = unwrapParen(conjunct)
    if (e !is BinaryOpExprNode || e.op() != "in") return null
    val lhs = unwrapParen(e.lhsOperand())
    val rhs = unwrapParen(e.rhsOperand())
    val arg = (lhs as? SymbolValueExprNode)?.symbol ?: return null
    if (arg !in unbound) return null
    if (unbound.any { it != arg && exprReferencesSymbol(rhs, it) }) return null
    val rhsType = typeOf(rhs)
    if (rhsType != null && rhsType !is SetType) return null
    return arg to rhs
}

private fun matchListIndex(
    conjunct: ExprNode,
    unbound: Set<String>,
): Triple<String, String, ExprNode>? {
    val e = unwrapParen(conjunct)
    if (e !is BinaryOpExprNode || e.op() != "=") return null
    val lhs = unwrapParen(e.lhsOperand())
    val rhs = unwrapParen(e.rhsOperand())
    fun asIndex(indexExpr: ExprNode, argExpr: ExprNode): Triple<String, String, ExprNode>? {
        if (indexExpr !is IndexExprNode) return null
        val arg = (unwrapParen(argExpr) as? SymbolValueExprNode)?.symbol ?: return null
        if (arg !in unbound) return null
        val field = indexFieldOf(indexExpr.index, arg) ?: return null
        val list = unwrapParen(indexExpr.base)
        val listType = typeOf(list)
        if (listType != null && listType !is ListType) return null
        if (unbound.any { it != arg && exprReferencesSymbol(list, it) }) return null
        return Triple(arg, field, list)
    }
    return asIndex(lhs, rhs) ?: asIndex(rhs, lhs)
}

private fun indexFieldOf(index: ExprNode, arg: String): String? {
    val e = unwrapParen(index)
    return when (e) {
        is FieldAccessExprNode ->
            if (e.baseSymbol == arg && e.fieldPath.size == 1) e.fieldPath.first() else null
        is MemberAccessExprNode -> {
            val b = unwrapParen(e.baseExpr)
            if (b is SymbolValueExprNode && b.symbol == arg) e.fieldName else null
        }
        else -> null
    }
}

private fun dropImpliedBounds(
    conjuncts: List<ExprNode>,
    skip: IdentityHashMap<ExprNode, Boolean>,
    arg: String,
    field: String,
    list: ExprNode,
) {
    for (c in conjuncts) {
        if (skip.containsKey(c)) continue
        if (isLowerBound(c, arg, field) || isLenBound(c, arg, field, list)) {
            skip[c] = true
        }
    }
}

private fun isLowerBound(conjunct: ExprNode, arg: String, field: String): Boolean {
    val e = unwrapParen(conjunct)
    if (e !is BinaryOpExprNode) return false
    val l = unwrapParen(e.lhsOperand())
    val r = unwrapParen(e.rhsOperand())
    fun isOne(n: ExprNode) = n is LiteralValueExprNode && n.literalText() == "1"
    return when (e.op()) {
        ">=" -> indexFieldOf(l, arg) == field && isOne(r)
        "<=" -> isOne(l) && indexFieldOf(r, arg) == field
        else -> false
    }
}

private fun isLenBound(conjunct: ExprNode, arg: String, field: String, list: ExprNode): Boolean {
    val e = unwrapParen(conjunct)
    if (e !is BinaryOpExprNode) return false
    val l = unwrapParen(e.lhsOperand())
    val r = unwrapParen(e.rhsOperand())
    return when (e.op()) {
        "<=" -> indexFieldOf(l, arg) == field && isLengthOf(r, list)
        ">=" -> isLengthOf(l, list) && indexFieldOf(r, arg) == field
        else -> false
    }
}

private fun isLengthOf(expr: ExprNode, list: ExprNode): Boolean {
    val e = unwrapParen(expr)
    return when (e) {
        is FunCallExprNode ->
            e.callName() == "length" && e.callArgs().singleOrNull()?.let { sameStateExpr(it, list) } == true
        is MemberAccessExprNode ->
            e.fieldName == "length" && sameStateExpr(e.baseExpr, list)
        is FieldAccessExprNode ->
            e.fieldPath == listOf("length") && list is SymbolValueExprNode && e.baseSymbol == list.symbol
        else -> false
    }
}

private fun sameStateExpr(a: ExprNode, b: ExprNode): Boolean {
    val x = unwrapParen(a)
    val y = unwrapParen(b)
    return x is SymbolValueExprNode && y is SymbolValueExprNode && x.symbol == y.symbol
}

private fun matchStructInSet(
    conjunct: ExprNode,
    unbound: Set<String>,
    taken: MutableSet<String>,
): StructInSetBind? {
    val e = unwrapParen(conjunct)
    if (e !is BinaryOpExprNode || e.op() != "in") return null
    val lit = unwrapParen(e.lhsOperand()) as? ObjClassLiteralExprNode ?: return null
    val set = unwrapParen(e.rhsOperand())
    if (unbound.any { exprReferencesSymbol(set, it) }) return null
    val paths = mutableListOf<Pair<String, List<String>>>()
    collectArgPaths(lit, emptyList(), unbound, paths)
    if (paths.isEmpty()) return null
    val keep = mutableListOf<Pair<List<String>, ExprNode>>()
    collectKeep(lit, emptyList(), unbound, keep)
    val tmp = allocTlaName(collectionBinderBase(set), taken)
    return StructInSetBind(tmp, set, paths, keep)
}

internal fun collectionBinderBase(set: ExprNode): String {
    val e = unwrapParen(set)
    val raw = when (e) {
        is SymbolValueExprNode -> e.symbol
        is FieldAccessExprNode -> e.fieldPath.lastOrNull() ?: e.baseSymbol
        is MemberAccessExprNode -> e.fieldName
        else -> "elem"
    }
    return singularizeCollectionName(raw)
}

internal fun singularizeCollectionName(name: String): String = when {
    name.endsWith("Msgs") && name.length > 4 -> name.removeSuffix("s")
    name.endsWith("s") && name.length > 1 && !name.endsWith("ss") -> name.dropLast(1)
    else -> name
}

private fun collectArgPaths(
    lit: ObjClassLiteralExprNode,
    prefix: List<String>,
    unbound: Set<String>,
    out: MutableList<Pair<String, List<String>>>,
) {
    lit.fieldEntries.forEach { (name, value) ->
        val path = prefix + name
        val v = unwrapParen(value)
        when {
            v is ObjClassLiteralExprNode -> collectArgPaths(v, path, unbound, out)
            v is SymbolValueExprNode && v.symbol in unbound -> out += v.symbol to path
        }
    }
}

private fun collectKeep(
    lit: ObjClassLiteralExprNode,
    prefix: List<String>,
    unbound: Set<String>,
    out: MutableList<Pair<List<String>, ExprNode>>,
) {
    lit.fieldEntries.forEach { (name, value) ->
        val path = prefix + name
        val v = unwrapParen(value)
        when {
            v is ObjClassLiteralExprNode -> collectKeep(v, path, unbound, out)
            v is SymbolValueExprNode && v.symbol in unbound -> {}
            else -> out += path to value
        }
    }
}

private fun typeOf(expr: ExprNode): julay.program.type.Type? = try {
    expr.getType()
} catch (_: RuntimeException) {
    null
}
