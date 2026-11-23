package julay.compiler.pass

import julay.compiler.ast.*
import julay.program.type.MapType

/**
 * Collect map indexes that appear in a *value* position.
 * Used by TLA action emit so determined RHSs with `m[k]` nest in LETs after
 * prior `/\` conjuncts (TLC short-circuits and does not apply `m[k]` when a
 * preceding `k \in DOMAIN m` fails).
 *
 * Missing-key behavior is hard on both backends: Go `Get` panics; TLA bare
 * `m[k]` is a domain error. There is no compile-time membership checker and
 * no soft-default rewrite.
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
    is BinaryOpExprNode ->
        collectMapIndexesInValue(expr.lhsOperand()) + collectMapIndexesInValue(expr.rhsOperand())
    is UnaryOpExprNode -> collectMapIndexesInValue(expr.operand())
    is QuantifiedExprNode -> collectMapIndexesInValue(expr.quantifiedBody())
    is ParenExprNode -> collectMapIndexesInValue(expr.innerExpr())
    is IfElseExprNode ->
        collectMapIndexesInValue(expr.condExpr()) +
            collectMapIndexesInValue(expr.thenExpr()) +
            collectMapIndexesInValue(expr.elseExpr())
    is LetExprNode ->
        collectMapIndexesInValue(expr.letInitExpr()) + collectMapIndexesInValue(expr.bodyExpr())
    is MethodCallExprNode -> {
        val hof = expr.hofBodyOrNull()
        collectMapIndexesInValue(expr.baseExpr) +
            expr.args.filter { it !is LambdaExprNode }.flatMap { collectMapIndexesInValue(it) } +
            (hof?.let { collectMapIndexesInValue(it) } ?: emptyList())
    }
    else -> expr.children.filterIsInstance<ExprNode>().flatMap { collectMapIndexesInValue(it) }
}
