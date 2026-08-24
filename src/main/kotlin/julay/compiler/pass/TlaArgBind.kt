package julay.compiler.pass

import julay.compiler.ast.*
import julay.compiler.isDiscardBinding
import julay.program.type.ListType
import julay.program.type.SetType
import julay.program.type.boolType
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
    val projected: List<ProjectedArgBind> = emptyList(),
) {
    fun skipped(expr: ExprNode): Boolean =
        skipConjuncts.containsKey(expr) || skipConjuncts.containsKey(unwrapParen(expr))

    /** Args whose type-domain `\E` should not be collected (determined, list, struct, set-member). */
    fun omitArgTypeDomains(): Set<String> = skipArgs + setBinds.map { it.arg }.toSet()

    fun extraBinderNames(): Set<String> =
        listBinds.map { it.index }.toSet() +
            structBinds.map { it.tmp }.toSet() +
            projected.flatMap { b -> b.sources.map { it.param } }.toSet()

    fun projectedBind(arg: String): ProjectedArgBind? = projected.firstOrNull { it.arg == arg }

    companion object {
        val EMPTY = TlaArgBindPlan(
            skipConjuncts = IdentityHashMap(),
            skipArgs = emptySet(),
            determined = emptyList(),
            listBinds = emptyList(),
            setBinds = emptyList(),
            structBinds = emptyList(),
            projected = emptyList(),
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

/** `\E a \in { x.f : x \in S }` from `S.filter(x -> x.f = a).length > 0`. */
internal data class ProjectedArgBind(
    val arg: String,
    val sources: List<ProjectedFilterSource>,
)

internal data class ProjectedFilterSource(
    val set: ExprNode,
    val param: String,
    val projection: ExprNode,
)

/** Skip-by-identity guard conjuncts while emitting TLA (determined-args / from-collection). */
internal object TlaSkipConjuncts {
    private val current = ThreadLocal.withInitial { IdentityHashMap<ExprNode, Boolean>() }

    fun skipped(expr: ExprNode): Boolean {
        val skip = current.get()
        return skip.containsKey(expr) || skip.containsKey(unwrapParen(expr))
    }

    fun install(skip: IdentityHashMap<ExprNode, Boolean>) {
        current.set(skip)
    }

    fun clear() {
        current.set(IdentityHashMap())
    }

    fun <T> with(skip: IdentityHashMap<ExprNode, Boolean>, block: () -> T): T {
        val prev = current.get()
        current.set(skip)
        try {
            return block()
        } finally {
            current.set(prev)
        }
    }
}

/** Index / `also` names that from-collection may bind from a struct-in-set guard. */
internal fun collectTlaExtraArgNames(
    offers: List<TlaActionOffer>,
    pclasses: Map<String, ProcClassNode> = emptyMap(),
    leafSpecs: Map<String, LeafSpecNode> = emptyMap(),
): Set<String> {
    val names = linkedSetOf<String>()
    offers.forEach { offer ->
        offer.leaf.paramName?.let { names += it }
        collectLeafSpecAuxParams(
            offer.leaf,
            offer.decl.action.name,
            offer.isConstructor,
            pclasses,
            leafSpecs,
        ).forEach { names += it.name }
    }
    return names
}

internal data class GuardConjunct(
    val expr: ExprNode,
    val letEnv: Map<String, ExprNode>,
)

internal fun analyzeTlaArgBind(
    offers: List<TlaActionOffer>,
    config: TlaOptConfig,
    extraArgNames: Set<String> = emptySet(),
): TlaArgBindPlan {
    if (!config.determinedArgs && !config.fromCollection) return TlaArgBindPlan.EMPTY
    val argNames = linkedSetOf<String>()
    offers.forEach { offer ->
        offer.decl.action.args.forEach { arg ->
            if (offerRefsArg(offer, arg.name)) argNames += arg.name
        }
    }
    extraArgNames.forEach { argNames += it }
    if (argNames.isEmpty()) return TlaArgBindPlan.EMPTY

    val conjuncts = offers.flatMap { offer ->
        offer.decl.guards.flatMap { collectGuardConjuncts(it) }
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
                if (skip.containsKey(c.expr)) continue
                val det = matchDetermined(c.expr, argNames, bound.keys, conflicted, extraArgNames)
                if (det != null) {
                    val (arg, raw) = det
                    val expr = substLetEnv(raw, c.letEnv)
                    val existing = bound[arg]
                    if (existing is Bound.Determined && !sameDeterminer(existing.expr, expr)) {
                        bound.remove(arg)
                        conflicted += arg
                        continue
                    }
                    if (arg in conflicted) continue
                    if (existing != null && existing !is Bound.Determined) continue
                    bound[arg] = Bound.Determined(expr)
                    skip[c.expr] = true
                    changed = true
                    continue
                }
                for (arg in argNames) {
                    if (arg in bound || arg in conflicted) continue
                    val hit = matchCaseDetermined(c.expr, arg, extraArgNames) ?: continue
                    val expr = substLetEnv(hit.expr, c.letEnv)
                    val existing = bound[arg]
                    if (existing is Bound.Determined && !sameDeterminer(existing.expr, expr)) {
                        bound.remove(arg)
                        conflicted += arg
                        continue
                    }
                    if (existing != null && existing !is Bound.Determined) continue
                    bound[arg] = Bound.Determined(expr)
                    hit.skipNodes.forEach { skip[it] = true }
                    changed = true
                }
            }
        }
        if (config.fromCollection) {
            val unbound = argNames.filter { it !in bound }.toSet()
            for (c in conjuncts) {
                if (skip.containsKey(c.expr)) continue
                matchSetMember(c.expr, unbound)?.let { (arg, setExpr) ->
                    bound[arg] = Bound.SetMember(substLetEnv(setExpr, c.letEnv))
                    skip[c.expr] = true
                    changed = true
                    return@let
                }
                if (skip.containsKey(c.expr)) continue
                matchListIndex(c.expr, unbound)?.let { (arg, field, listExpr) ->
                    bound[arg] = Bound.ListIndex("${arg}_idx", substLetEnv(listExpr, c.letEnv))
                    skip[c.expr] = true
                    dropImpliedBounds(conjuncts, skip, arg, field, listExpr)
                    changed = true
                    return@let
                }
                if (skip.containsKey(c.expr)) continue
                matchStructInSet(c.expr, unbound, takenStructNames)?.let { struct ->
                    structBinds += struct
                    struct.argPaths.forEach { (arg, path) ->
                        bound[arg] = Bound.StructField(struct.tmp, path)
                    }
                    skip[c.expr] = true
                    changed = true
                    return@let
                }
                matchProjectedFilters(c.expr, unbound, extraArgNames)?.let { (arg, sources) ->
                    val subst = sources.map {
                        ProjectedFilterSource(
                            substLetEnv(it.set, c.letEnv),
                            it.param,
                            substLetEnv(it.projection, c.letEnv),
                        )
                    }
                    val existing = bound[arg]
                    if (existing is Bound.Projected) {
                        bound[arg] = Bound.Projected(existing.sources + subst)
                    } else if (existing == null) {
                        bound[arg] = Bound.Projected(subst)
                    } else {
                        return@let
                    }
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
        if (b is Bound.SetMember || b is Bound.Projected) null else n
    }.toSet()
    val projectedBinds = bound.entries.mapNotNull { (n, b) ->
        (b as? Bound.Projected)?.let { ProjectedArgBind(n, it.sources) }
    }

    return TlaArgBindPlan(
        skipConjuncts = skip,
        skipArgs = skipArgs,
        determined = determined,
        listBinds = listBinds,
        setBinds = setBinds,
        structBinds = structBinds,
        projected = projectedBinds,
    )
}

private sealed class Bound {
    data class Determined(val expr: ExprNode) : Bound()
    data class SetMember(val set: ExprNode) : Bound()
    data class ListIndex(val index: String, val list: ExprNode) : Bound()
    data class StructField(val tmp: String, val path: List<String>) : Bound()
    data class Projected(val sources: List<ProjectedFilterSource>) : Bound()
}

private data class CaseDeterminedHit(
    val expr: ExprNode,
    val skipNodes: List<ExprNode>,
)

private fun unwrapParen(expr: ExprNode): ExprNode =
    if (expr is ParenExprNode) unwrapParen(expr.innerExpr()) else expr

internal fun isDesugaredIff(expr: ExprNode): Boolean {
    val e = unwrapParen(expr)
    if (e !is BinaryOpExprNode || e.op() != "&") return false
    val l = unwrapParen(e.lhsOperand()) as? BinaryOpExprNode ?: return false
    val r = unwrapParen(e.rhsOperand()) as? BinaryOpExprNode ?: return false
    if (l.op() != "=>" || r.op() != "=>") return false
    return sameDeterminer(unwrapParen(l.lhsOperand()), unwrapParen(r.rhsOperand())) &&
        sameDeterminer(unwrapParen(l.rhsOperand()), unwrapParen(r.lhsOperand()))
}

private fun matchDetermined(
    conjunct: ExprNode,
    argNames: Set<String>,
    alreadyBound: Set<String>,
    conflicted: Set<String>,
    extraArgNames: Set<String>,
): Pair<String, ExprNode>? {
    val e = unwrapParen(conjunct)
    fun oneSide(argSide: ExprNode, other: ExprNode): Pair<String, ExprNode>? {
        val a = (argSide as? SymbolValueExprNode)?.symbol ?: return null
        if (a !in argNames || a in conflicted) return null
        if (exprReferencesSymbol(other, a)) return null
        val unbound = argNames.filter { it != a && it !in alreadyBound && it !in extraArgNames }
        if (unbound.any { exprReferencesSymbol(other, it) }) return null
        return a to other
    }
    if (e is BinaryOpExprNode && e.op() == "<=>") {
        val lhs = unwrapParen(e.lhsOperand())
        val rhs = unwrapParen(e.rhsOperand())
        return oneSide(lhs, rhs) ?: oneSide(rhs, lhs)
    }
    if (isDesugaredIff(e) && e is BinaryOpExprNode) {
        val l = unwrapParen(e.lhsOperand()) as BinaryOpExprNode
        val argSide = unwrapParen(l.lhsOperand())
        val other = unwrapParen(l.rhsOperand())
        return oneSide(argSide, other) ?: oneSide(other, argSide)
    }
    if (e !is BinaryOpExprNode) return null
    if (e.op() != "=") return null
    val lhs = unwrapParen(e.lhsOperand())
    val rhs = unwrapParen(e.rhsOperand())
    return oneSide(lhs, rhs) ?: oneSide(rhs, lhs)
}

private fun matchCaseDetermined(
    conjunct: ExprNode,
    arg: String,
    extraArgNames: Set<String>,
): CaseDeterminedHit? {
    val leaves = orLeaves(conjunct)
    if (leaves.size < 2) return null
    val arms = leaves.map { branchDetermines(it, arg, extraArgNames) ?: return null }
    val skipNodes = arms.flatMap { it.skip }
    val conds = arms.map { stripDeterminerConjuncts(it.branch, setOf(arg), extraArgNames) }
    if (conds.dropLast(1).any { isTrueLiteral(it) }) return null
    if (conds.any { exprReferencesSymbol(it, arg) }) return null
    if (arms.any { exprReferencesSymbol(it.value, arg) }) return null
    if (conds.dropLast(1).distinctBy { it.toString() }.size != conds.dropLast(1).size) return null
    var expr = arms.last().value
    val loc = conjunct.programLocation()
    for (i in arms.size - 2 downTo 0) {
        expr = IfElseExprNode(conds[i], arms[i].value, expr, loc)
    }
    return CaseDeterminedHit(expr, skipNodes)
}

private data class BranchDeterminer(
    val branch: ExprNode,
    val value: ExprNode,
    val skip: List<ExprNode>,
)

private fun branchDetermines(
    branch: ExprNode,
    arg: String,
    extraArgNames: Set<String>,
): BranchDeterminer? {
    val conjuncts = andLeaves(branch)
    for (c in conjuncts) {
        booleanDeterminer(c, arg)?.let { value ->
            return BranchDeterminer(branch, value, listOf(c))
        }
        val det = matchDetermined(c, setOf(arg), emptySet(), emptySet(), extraArgNames) ?: continue
        if (det.first != arg) continue
        return BranchDeterminer(branch, det.second, listOf(c))
    }
    for (c in conjuncts) {
        val inner = unwrapBoolUnary(unwrapParen(c))
        if (inner !is BinaryOpExprNode || inner.op() != "|") continue
        val nested = matchCaseDetermined(inner, arg, extraArgNames) ?: continue
        return BranchDeterminer(branch, nested.expr, nested.skipNodes)
    }
    return null
}

private fun booleanDeterminer(conjunct: ExprNode, arg: String): ExprNode? {
    val e = unwrapBoolUnary(unwrapParen(conjunct))
    val loc = e.programLocation()
    if (e is SymbolValueExprNode && e.symbol == arg) {
        return LiteralValueExprNode("true", boolType, loc)
    }
    if (e is UnaryOpExprNode && e.op() == "~") {
        val inner = unwrapParen(e.operand())
        if (inner is SymbolValueExprNode && inner.symbol == arg) {
            return LiteralValueExprNode("false", boolType, loc)
        }
    }
    return null
}

private fun stripDeterminerConjuncts(
    expr: ExprNode,
    args: Set<String>,
    extraArgNames: Set<String>,
): ExprNode {
    val e = unwrapBoolUnary(unwrapParen(expr))
    if (isDeterminerOf(e, args, extraArgNames)) {
        return LiteralValueExprNode("true", boolType, e.programLocation())
    }
    if (e is BinaryOpExprNode && e.op() == "&" && !isDesugaredIff(e)) {
        val kept = andLeaves(e).map { stripDeterminerConjuncts(it, args, extraArgNames) }
            .filter { !isTrueLiteral(it) }
        return andFold(kept, e.programLocation())
    }
    if (e is BinaryOpExprNode && e.op() == "|") {
        val parts = orLeaves(e).map { stripDeterminerConjuncts(it, args, extraArgNames) }
        return orFold(parts, e.programLocation())
    }
    return e
}

private fun isDeterminerOf(expr: ExprNode, args: Set<String>, extraArgNames: Set<String>): Boolean {
    val e = unwrapBoolUnary(unwrapParen(expr))
    if (args.any { booleanDeterminer(e, it) != null }) return true
    val det = matchDetermined(e, args, emptySet(), emptySet(), extraArgNames) ?: return false
    return det.first in args
}

private fun isTrueLiteral(expr: ExprNode): Boolean {
    val e = unwrapParen(expr)
    return e is LiteralValueExprNode && e.literalText() == "true"
}

private fun andFold(parts: List<ExprNode>, loc: julay.compiler.ProgramLoc): ExprNode = when {
    parts.isEmpty() -> LiteralValueExprNode("true", boolType, loc)
    parts.size == 1 -> parts.single()
    else -> parts.reduce { l, r -> BinaryOpExprNode("&", l, r, loc) }
}

private fun orFold(parts: List<ExprNode>, loc: julay.compiler.ProgramLoc): ExprNode = when {
    parts.isEmpty() -> LiteralValueExprNode("true", boolType, loc)
    parts.size == 1 -> parts.single()
    else -> parts.reduce { l, r -> BinaryOpExprNode("|", l, r, loc) }
}

private fun andLeaves(expr: ExprNode): List<ExprNode> {
    val e = unwrapBoolUnary(unwrapParen(expr))
    return if (e is BinaryOpExprNode && e.op() == "&" && !isDesugaredIff(e)) {
        andLeaves(e.lhsOperand()) + andLeaves(e.rhsOperand())
    } else {
        listOf(e)
    }
}

private fun orLeaves(expr: ExprNode): List<ExprNode> {
    val e = unwrapBoolUnary(unwrapParen(expr))
    return if (e is BinaryOpExprNode && e.op() == "|") {
        orLeaves(e.lhsOperand()) + orLeaves(e.rhsOperand())
    } else {
        listOf(e)
    }
}

/** Leading `&` / `|` in a Julay guard is a unary op wrapping the formula. */
private fun unwrapBoolUnary(expr: ExprNode): ExprNode {
    val e = unwrapParen(expr)
    return if (e is UnaryOpExprNode && (e.op() == "&" || e.op() == "|")) {
        unwrapBoolUnary(e.operand())
    } else {
        e
    }
}

private fun matchProjectedFilters(
    expr: ExprNode,
    unbound: Set<String>,
    extraArgNames: Set<String>,
): Pair<String, List<ProjectedFilterSource>>? {
    val leaves = orLeaves(expr)
    val hits = leaves.map { matchFilterLengthProjection(it, unbound, extraArgNames) ?: return null }
    val arg = hits.map { it.first }.distinct().singleOrNull() ?: return null
    return arg to hits.map { it.second }
}

private fun matchFilterLengthProjection(
    expr: ExprNode,
    unbound: Set<String>,
    extraArgNames: Set<String>,
): Pair<String, ProjectedFilterSource>? {
    val e = unwrapParen(expr)
    if (e !is BinaryOpExprNode) return null
    val op = e.op()
    val lhs = unwrapParen(e.lhsOperand())
    val rhs = unwrapParen(e.rhsOperand())
    fun isZero(n: ExprNode) = n is LiteralValueExprNode && n.literalText() == "0"
    fun isOne(n: ExprNode) = n is LiteralValueExprNode && n.literalText() == "1"
    val filterCall = when {
        (op == ">" && isZero(rhs)) || (op == ">=" && isOne(rhs)) -> lengthFilterCall(lhs)
        (op == "<" && isZero(lhs)) || (op == "<=" && isOne(lhs)) -> lengthFilterCall(rhs)
        else -> null
    } ?: return null
    if (filterCall.methodName != "filter") return null
    val param = filterCall.hofParamNamesOrNull()?.singleOrNull() ?: return null
    val body = filterCall.hofBodyOrNull() ?: return null
    val set = unwrapParen(filterCall.baseExpr)
    var arg: String? = null
    var projection: ExprNode? = null
    for (c in andLeaves(body)) {
        val hit = equalityArgAndField(c, param, unbound) ?: continue
        if (hit.first in extraArgNames) continue
        if (arg != null && arg != hit.first) return null
        arg = hit.first
        projection = hit.second
    }
    if (arg == null || projection == null) return null
    if (unbound.any { it != arg && exprReferencesSymbol(set, it) }) return null
    if (unbound.any { it != arg && exprReferencesSymbol(projection!!, it) }) return null
    return arg to ProjectedFilterSource(set, param, projection)
}

private fun equalityArgAndField(
    conjunct: ExprNode,
    param: String,
    unbound: Set<String>,
): Pair<String, ExprNode>? {
    val e = unwrapParen(conjunct)
    if (e !is BinaryOpExprNode || e.op() != "=") return null
    val l = unwrapParen(e.lhsOperand())
    val r = unwrapParen(e.rhsOperand())
    fun hit(fieldSide: ExprNode, argSide: ExprNode): Pair<String, ExprNode>? {
        val a = (argSide as? SymbolValueExprNode)?.symbol ?: return null
        if (a !in unbound) return null
        if (!fieldStartsWithParam(fieldSide, param)) return null
        if (exprReferencesSymbol(fieldSide, a)) return null
        return a to fieldSide
    }
    return hit(l, r) ?: hit(r, l)
}

private fun fieldStartsWithParam(expr: ExprNode, param: String): Boolean {
    var cur = unwrapParen(expr)
    while (true) {
        cur = when (cur) {
            is MemberAccessExprNode -> unwrapParen(cur.baseExpr)
            is FieldAccessOnExprNode -> unwrapParen(cur.baseExpr)
            is FieldAccessExprNode -> return cur.baseSymbol == param
            is SymbolValueExprNode -> return cur.symbol == param
            else -> return false
        }
    }
}

private fun lengthFilterCall(expr: ExprNode): MethodCallExprNode? {
    val e = unwrapParen(expr)
    return when (e) {
        is MemberAccessExprNode ->
            if (e.fieldName == "length") unwrapParen(e.baseExpr) as? MethodCallExprNode else null
        is FieldAccessExprNode -> null
        is FunCallExprNode ->
            if (e.callName() == "length") {
                e.callArgs().singleOrNull()?.let { unwrapParen(it) as? MethodCallExprNode }
            } else {
                null
            }
        else -> null
    }
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
    conjuncts: List<GuardConjunct>,
    skip: IdentityHashMap<ExprNode, Boolean>,
    arg: String,
    field: String,
    list: ExprNode,
) {
    for (c in conjuncts) {
        if (skip.containsKey(c.expr)) continue
        if (isLowerBound(c.expr, arg, field) || isLenBound(c.expr, arg, field, list)) {
            skip[c.expr] = true
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

/** Walk into `let` / parens / top-level `&` so determined-args can see nested `arg = E`. */
internal fun collectGuardConjuncts(
    expr: ExprNode,
    env: Map<String, ExprNode> = emptyMap(),
): List<GuardConjunct> {
    val e = unwrapParen(expr)
    return when {
        e is LetExprNode -> {
            val name = e.letName()
            val init = substLetEnv(e.letInitExpr(), env)
            val newEnv = if (name.isDiscardBinding()) env else env + (name to init)
            collectGuardConjuncts(e.bodyExpr(), newEnv)
        }
        e is UnaryOpExprNode && (e.op() == "&" || e.op() == "|") ->
            collectGuardConjuncts(e.operand(), env)
        e is BinaryOpExprNode && e.op() == "&" && !isDesugaredIff(e) ->
            collectGuardConjuncts(e.lhsOperand(), env) + collectGuardConjuncts(e.rhsOperand(), env)
        else -> listOf(GuardConjunct(e, env))
    }
}

private fun substLetEnv(expr: ExprNode, env: Map<String, ExprNode>): ExprNode {
    var out = expr
    env.forEach { (name, replacement) ->
        out = substituteExpr(out, name, replacement)
    }
    return out
}
