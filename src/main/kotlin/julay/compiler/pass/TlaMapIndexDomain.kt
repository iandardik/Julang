package julay.compiler.pass

import julay.compiler.ProgramLoc
import julay.compiler.ast.*
import julay.program.type.MapType
import julay.program.type.boolType

/**
 * Invariant desugar for TLA+: unguarded map reads in comparisons need a domain
 * guard so function application is only evaluated when the key is in `DOMAIN`.
 *
 * `mp[k] = v` → `k in mp & mp[k] = v` (likewise `~=` / `#` and other comparisons;
 * for `in` / `~in`, only the element side is scanned).
 *
 * Membership is paired only with **unguarded** reads: under `if (k in mp) { mp[k] = v }`
 * the then-arm does not gain a redundant outer `k in mp` (the `if` already guards).
 * (List indexes are not included: Julay `x in xs` is element membership, not index-in-DOMAIN.)
 */
internal fun withMapIndexDomainGuards(expr: ExprNode): ExprNode =
    rewriteMapIndexDomain(expr, emptySet())

private val mapIndexComparisonOps = setOf("=", "#", "<", "<=", ">", ">=")
private val mapIndexMembershipOps = setOf("in", "~in")

private fun rewriteMapIndexDomain(expr: ExprNode, assumedIn: Set<String>): ExprNode = when (expr) {
    is BinaryOpExprNode -> {
        val unguarded = when (expr.op()) {
            in mapIndexComparisonOps ->
                collectUnguardedMapIndexesInValue(expr.lhsOperand(), assumedIn) +
                    collectUnguardedMapIndexesInValue(expr.rhsOperand(), assumedIn)
            in mapIndexMembershipOps ->
                collectUnguardedMapIndexesInValue(expr.lhsOperand(), assumedIn)
            else -> emptyList()
        }
        val lhs = rewriteMapIndexDomain(expr.lhsOperand(), assumedIn)
        val rhs = rewriteMapIndexDomain(expr.rhsOperand(), assumedIn)
        val rebuilt =
            if (lhs === expr.lhsOperand() && rhs === expr.rhsOperand()) expr
            else typedBoolBinary(expr.op(), lhs, rhs, expr.programLocation())
        if (expr.op() in mapIndexComparisonOps || expr.op() in mapIndexMembershipOps) {
            conjoinMapDomainGuards(rebuilt, unguarded)
        } else {
            rebuilt
        }
    }
    is UnaryOpExprNode -> {
        val inner = rewriteMapIndexDomain(expr.operand(), assumedIn)
        if (inner === expr.operand()) expr
        else UnaryOpExprNode(expr.op(), inner, expr.programLocation()).also {
            it.setInferredType(TypePassType.Inferred(boolType))
        }
    }
    is ParenExprNode -> {
        val inner = rewriteMapIndexDomain(expr.innerExpr(), assumedIn)
        if (inner === expr.innerExpr()) expr
        else ParenExprNode(inner, expr.programLocation()).also {
            it.setInferredType(TypePassType.Inferred(inner.getType()))
        }
    }
    is QuantifiedExprNode -> {
        val body = rewriteMapIndexDomain(expr.quantifiedBody(), assumedIn)
        if (body === expr.quantifiedBody()) expr
        else QuantifiedExprNode(
            universal = expr.isUniversal(),
            binder = expr.binderName(),
            binderType = expr.binderTypeExpr(),
            body = body,
            loc = expr.programLocation(),
        ).also { it.setInferredType(TypePassType.Inferred(boolType)) }
    }
    is IfElseExprNode -> {
        val cond = rewriteMapIndexDomain(expr.condExpr(), assumedIn)
        val thenAssumed = assumedIn + membershipInShapes(expr.condExpr())
        val elseAssumed = assumedIn + membershipNotInShapes(expr.condExpr())
        val thenE = rewriteMapIndexDomain(expr.thenExpr(), thenAssumed)
        val elseE = rewriteMapIndexDomain(expr.elseExpr(), elseAssumed)
        if (cond === expr.condExpr() && thenE === expr.thenExpr() && elseE === expr.elseExpr()) expr
        else IfElseExprNode(cond, thenE, elseE, expr.programLocation()).also {
            it.setInferredType(TypePassType.Inferred(thenE.getType()))
        }
    }
    is LetExprNode -> {
        val init = rewriteMapIndexDomain(expr.letInitExpr(), assumedIn)
        val body = rewriteMapIndexDomain(expr.bodyExpr(), assumedIn)
        if (init === expr.letInitExpr() && body === expr.bodyExpr()) expr
        else LetExprNode(
            expr.letName(),
            expr.letTypeExpr(),
            init,
            body,
            expr.programLocation(),
            expr.resolvedLetTypeOrNull(),
        ).also { it.setInferredType(TypePassType.Inferred(body.getType())) }
    }
    is WhenExprNode -> {
        val subject = expr.subjectExpr()?.let { rewriteMapIndexDomain(it, assumedIn) }
        var changed = subject !== expr.subjectExpr()
        val arms = expr.arms().map { arm ->
            when (arm) {
                is WhenArm.Subject -> {
                    val e = rewriteMapIndexDomain(arm.expr, assumedIn)
                    if (e !== arm.expr) changed = true
                    WhenArm.Subject(arm.pattern, e)
                }
                is WhenArm.Guard -> {
                    val c = rewriteMapIndexDomain(arm.cond, assumedIn)
                    val armAssumed = assumedIn + membershipInShapes(arm.cond)
                    val e = rewriteMapIndexDomain(arm.expr, armAssumed)
                    if (c !== arm.cond || e !== arm.expr) changed = true
                    WhenArm.Guard(c, e)
                }
                is WhenArm.Else -> {
                    val e = rewriteMapIndexDomain(arm.expr, assumedIn)
                    if (e !== arm.expr) changed = true
                    WhenArm.Else(e)
                }
            }
        }
        if (!changed) expr
        else WhenExprNode(subject, arms, expr.programLocation()).also {
            val ty = arms.first().let { a ->
                when (a) {
                    is WhenArm.Subject -> a.expr.getType()
                    is WhenArm.Guard -> a.expr.getType()
                    is WhenArm.Else -> a.expr.getType()
                }
            }
            it.setInferredType(TypePassType.Inferred(ty))
        }
    }
    else -> expr
}

private fun conjoinMapDomainGuards(comparison: ExprNode, indexes: List<IndexExprNode>): ExprNode {
    if (indexes.isEmpty()) return comparison
    // Stable order: left-to-right appearance; guards precede the comparison.
    val unique = indexes.distinctBy { it.toString() }
    return unique.foldRight(comparison) { idx, acc ->
        typedBoolBinary(
            "&",
            typedBoolBinary("in", idx.index, idx.base, idx.programLocation()),
            acc,
            comparison.programLocation(),
        )
    }
}

/**
 * Map indexes that appear in a *value* position (not under boolean connectives —
 * those are handled when their enclosing comparison is rewritten).
 */
internal fun collectMapIndexesInValue(expr: ExprNode): List<IndexExprNode> = when (expr) {
    is IndexExprNode -> {
        val nested = collectMapIndexesInValue(expr.base) + collectMapIndexesInValue(expr.index)
        val self = try {
            if (expr.base.getType() is MapType) listOf(expr) else emptyList()
        } catch (_: RuntimeException) {
            emptyList()
        }
        nested + self
    }
    is BinaryOpExprNode -> when (expr.op()) {
        "&", "|", "=>", "<=>", "in", "~in" -> emptyList()
        else -> collectMapIndexesInValue(expr.lhsOperand()) + collectMapIndexesInValue(expr.rhsOperand())
    }
    is UnaryOpExprNode -> when (expr.op()) {
        "~" -> emptyList()
        else -> collectMapIndexesInValue(expr.operand())
    }
    is QuantifiedExprNode -> emptyList()
    is ParenExprNode -> collectMapIndexesInValue(expr.innerExpr())
    is IfElseExprNode ->
        collectMapIndexesInValue(expr.thenExpr()) + collectMapIndexesInValue(expr.elseExpr())
    is LetExprNode -> collectMapIndexesInValue(expr.bodyExpr())
    else -> expr.children.filterIsInstance<ExprNode>().flatMap { collectMapIndexesInValue(it) }
}

/**
 * Like [collectMapIndexesInValue], but omits indexes already guarded by enclosing
 * `key in map` (positive `if` then-arm, inverted `if` else-arm, `when` guard arms).
 * Used for determined-arg action DOMAIN guards and invariant desugar.
 */
internal fun collectUnguardedMapIndexesInValue(expr: ExprNode): List<IndexExprNode> =
    collectUnguardedMapIndexesInValue(expr, emptySet())

internal fun collectUnguardedMapIndexesInValue(
    expr: ExprNode,
    assumedIn: Set<String>,
): List<IndexExprNode> = when (expr) {
    is IndexExprNode -> {
        val nested =
            collectUnguardedMapIndexesInValue(expr.base, assumedIn) +
                collectUnguardedMapIndexesInValue(expr.index, assumedIn)
        val self = try {
            if (expr.base.getType() is MapType) {
                val shape = mapIndexShape(expr)
                if (shape in assumedIn) emptyList() else listOf(expr)
            } else {
                emptyList()
            }
        } catch (_: RuntimeException) {
            emptyList()
        }
        nested + self
    }
    is BinaryOpExprNode -> when (expr.op()) {
        "&", "|", "=>", "<=>", "in", "~in" -> emptyList()
        else ->
            collectUnguardedMapIndexesInValue(expr.lhsOperand(), assumedIn) +
                collectUnguardedMapIndexesInValue(expr.rhsOperand(), assumedIn)
    }
    is UnaryOpExprNode -> when (expr.op()) {
        "~" -> emptyList()
        else -> collectUnguardedMapIndexesInValue(expr.operand(), assumedIn)
    }
    is QuantifiedExprNode -> emptyList()
    is ParenExprNode -> collectUnguardedMapIndexesInValue(expr.innerExpr(), assumedIn)
    is IfElseExprNode -> {
        val thenAssumed = assumedIn + membershipInShapes(expr.condExpr())
        val elseAssumed = assumedIn + membershipNotInShapes(expr.condExpr())
        collectUnguardedMapIndexesInValue(expr.thenExpr(), thenAssumed) +
            collectUnguardedMapIndexesInValue(expr.elseExpr(), elseAssumed)
    }
    is WhenExprNode -> {
        expr.arms().flatMap { arm ->
            when (arm) {
                is WhenArm.Guard ->
                    collectUnguardedMapIndexesInValue(
                        arm.expr,
                        assumedIn + membershipInShapes(arm.cond),
                    )
                is WhenArm.Subject ->
                    collectUnguardedMapIndexesInValue(arm.expr, assumedIn)
                is WhenArm.Else ->
                    collectUnguardedMapIndexesInValue(arm.expr, assumedIn)
            }
        }
    }
    is LetExprNode -> collectUnguardedMapIndexesInValue(expr.bodyExpr(), assumedIn)
    else -> expr.children.filterIsInstance<ExprNode>().flatMap {
        collectUnguardedMapIndexesInValue(it, assumedIn)
    }
}

/** `key in map` conjuncts as map-index shapes (`map[key]` string form). */
private fun membershipInShapes(cond: ExprNode): Set<String> {
    val e = unwrapParensForDomain(cond)
    return when {
        e is BinaryOpExprNode && e.op() == "&" ->
            membershipInShapes(e.lhsOperand()) + membershipInShapes(e.rhsOperand())
        e is BinaryOpExprNode && e.op() == "in" ->
            setOf("${exprShape(e.rhsOperand())}[${exprShape(e.lhsOperand())}]")
        else -> emptySet()
    }
}

/**
 * Shapes for which [cond] entails `~(key in map)` (so the **else** of `if (cond)` may
 * assume `key in map`). Recognizes `~(k in m)` and `&` of such conjuncts.
 */
private fun membershipNotInShapes(cond: ExprNode): Set<String> {
    val e = unwrapParensForDomain(cond)
    return when {
        e is UnaryOpExprNode && e.op() == "~" -> membershipInShapes(e.operand())
        e is BinaryOpExprNode && e.op() == "&" ->
            membershipNotInShapes(e.lhsOperand()) + membershipNotInShapes(e.rhsOperand())
        else -> emptySet()
    }
}

private fun mapIndexShape(idx: IndexExprNode): String =
    "${exprShape(idx.base)}[${exprShape(idx.index)}]"

/** Stable shape for matching `key in map` against `map[key]` (not AST toString). */
private fun exprShape(expr: ExprNode): String {
    val e = unwrapParensForDomain(expr)
    return when (e) {
        is SymbolValueExprNode -> e.symbol
        is IndexExprNode -> "${exprShape(e.base)}[${exprShape(e.index)}]"
        is FieldAccessExprNode ->
            e.baseSymbol + e.fieldPath.joinToString("") { ".$it" }
        else -> e.toString()
    }
}

private fun unwrapParensForDomain(expr: ExprNode): ExprNode {
    var e = expr
    while (e is ParenExprNode) e = e.innerExpr()
    return e
}

private fun typedBoolBinary(op: String, lhs: ExprNode, rhs: ExprNode, loc: ProgramLoc): BinaryOpExprNode =
    BinaryOpExprNode(op, lhs, rhs, loc).also {
        it.setInferredType(TypePassType.Inferred(boolType))
    }
