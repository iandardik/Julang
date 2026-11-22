package julay.compiler.pass

import julay.compiler.ProgramLoc
import julay.compiler.ast.*
import julay.program.type.MapType
import julay.program.type.boolType

/**
 * Invariant desugar for TLA+: map reads in comparisons need a domain guard so
 * function application is only evaluated when the key is in `DOMAIN`.
 *
 * `mp[k] = v` → `k in mp & mp[k] = v` (likewise `~=` / `#` and other comparisons;
 * for `in` / `~in`, only the element side is scanned).
 */
internal fun withMapIndexDomainGuards(expr: ExprNode): ExprNode = rewriteMapIndexDomain(expr)

private val mapIndexComparisonOps = setOf("=", "#", "<", "<=", ">", ">=")
private val mapIndexMembershipOps = setOf("in", "~in")

private fun rewriteMapIndexDomain(expr: ExprNode): ExprNode = when (expr) {
    is BinaryOpExprNode -> {
        val lhs = rewriteMapIndexDomain(expr.lhsOperand())
        val rhs = rewriteMapIndexDomain(expr.rhsOperand())
        val rebuilt =
            if (lhs === expr.lhsOperand() && rhs === expr.rhsOperand()) expr
            else typedBoolBinary(expr.op(), lhs, rhs, expr.programLocation())
        when (expr.op()) {
            in mapIndexComparisonOps ->
                conjoinMapDomainGuards(rebuilt, collectMapIndexesInValue(lhs) + collectMapIndexesInValue(rhs))
            in mapIndexMembershipOps ->
                conjoinMapDomainGuards(rebuilt, collectMapIndexesInValue(lhs))
            else -> rebuilt
        }
    }
    is UnaryOpExprNode -> {
        val inner = rewriteMapIndexDomain(expr.operand())
        if (inner === expr.operand()) expr
        else UnaryOpExprNode(expr.op(), inner, expr.programLocation()).also {
            it.setInferredType(TypePassType.Inferred(boolType))
        }
    }
    is ParenExprNode -> {
        val inner = rewriteMapIndexDomain(expr.innerExpr())
        if (inner === expr.innerExpr()) expr
        else ParenExprNode(inner, expr.programLocation()).also {
            it.setInferredType(TypePassType.Inferred(inner.getType()))
        }
    }
    is QuantifiedExprNode -> {
        val body = rewriteMapIndexDomain(expr.quantifiedBody())
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
        val cond = rewriteMapIndexDomain(expr.condExpr())
        val thenE = rewriteMapIndexDomain(expr.thenExpr())
        val elseE = rewriteMapIndexDomain(expr.elseExpr())
        if (cond === expr.condExpr() && thenE === expr.thenExpr() && elseE === expr.elseExpr()) expr
        else IfElseExprNode(cond, thenE, elseE, expr.programLocation()).also {
            it.setInferredType(TypePassType.Inferred(thenE.getType()))
        }
    }
    is LetExprNode -> {
        val init = rewriteMapIndexDomain(expr.letInitExpr())
        val body = rewriteMapIndexDomain(expr.bodyExpr())
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
        // Arms are rewritten below; subject is a value (may contain map indexes used in
        // comparisons inside arms only — rewrite arms' exprs).
        val subject = expr.subjectExpr()?.let { rewriteMapIndexDomain(it) }
        var changed = subject !== expr.subjectExpr()
        val arms = expr.arms().map { arm ->
            when (arm) {
                is WhenArm.Subject -> {
                    val e = rewriteMapIndexDomain(arm.expr)
                    if (e !== arm.expr) changed = true
                    WhenArm.Subject(arm.pattern, e)
                }
                is WhenArm.Guard -> {
                    val c = rewriteMapIndexDomain(arm.cond)
                    val e = rewriteMapIndexDomain(arm.expr)
                    if (c !== arm.cond || e !== arm.expr) changed = true
                    WhenArm.Guard(c, e)
                }
                is WhenArm.Else -> {
                    val e = rewriteMapIndexDomain(arm.expr)
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
 * Like [collectMapIndexesInValue], but omits indexes already guarded by an enclosing
 * `key in map` (under `&`) on an `if` condition. Used for determined-arg action DOMAIN
 * guards: `owner = if (key in owners) { owners[key] } else { empty }` must stay enabled
 * when the map is empty — the emitted `IF … \in DOMAIN …` already protects the read.
 */
internal fun collectUnguardedMapIndexesInValue(expr: ExprNode): List<IndexExprNode> =
    collectUnguardedMapIndexesInValue(expr, emptySet())

private fun collectUnguardedMapIndexesInValue(
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
        collectUnguardedMapIndexesInValue(expr.thenExpr(), thenAssumed) +
            collectUnguardedMapIndexesInValue(expr.elseExpr(), assumedIn)
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
