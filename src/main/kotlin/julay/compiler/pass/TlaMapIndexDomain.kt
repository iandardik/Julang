package julay.compiler.pass

import julay.compiler.OneLocCompileError
import julay.compiler.CompileError
import julay.compiler.ProgramLoc
import julay.compiler.ast.*
import julay.program.type.MapType
import julay.program.type.boolType

/**
 * Map-index membership: unguarded `m[k]` is a compile error.
 * Guarding forms: `if (k in m)`, inverted `if`, `when` guards, `&` / `=>` local
 * membership, and keys-iteration HOFs (`m.keys…map/filter/associateWith`).
 *
 * [withMapIndexDomainGuards] remains an identity (no auto DOMAIN inject).
 */
internal fun withMapIndexDomainGuards(expr: ExprNode): ExprNode = expr

private val mapIndexComparisonOps = setOf("=", "#", "<", "<=", ">", ">=")

/**
 * Report every map read whose key is not locally proven present.
 * Call after type inference so [IndexExprNode] bases have [MapType].
 */
internal fun mapIndexMembershipErrors(
    expr: ExprNode,
    assumedIn: Set<String> = emptySet(),
): List<CompileError> =
    collectUnguardedMapIndexesInValue(expr, assumedIn).map { idx ->
        OneLocCompileError(
            idx.programLocation(),
            "map read requires explicit membership: use \"k in m\" before m[k]",
        )
    }

/**
 * Like collecting all map indexes in a value position, but omits indexes already
 * guarded by enclosing membership assumptions.
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
        "&" -> {
            val left = collectUnguardedMapIndexesInValue(expr.lhsOperand(), assumedIn)
            val right = collectUnguardedMapIndexesInValue(
                expr.rhsOperand(),
                assumedIn + membershipInShapes(expr.lhsOperand()),
            )
            left + right
        }
        "|" ->
            collectUnguardedMapIndexesInValue(expr.lhsOperand(), assumedIn) +
                collectUnguardedMapIndexesInValue(expr.rhsOperand(), assumedIn)
        "=>" -> {
            val ant = collectUnguardedMapIndexesInValue(expr.lhsOperand(), assumedIn)
            val cons = collectUnguardedMapIndexesInValue(
                expr.rhsOperand(),
                assumedIn + membershipInShapes(expr.lhsOperand()),
            )
            ant + cons
        }
        "<=>" ->
            collectUnguardedMapIndexesInValue(expr.lhsOperand(), assumedIn) +
                collectUnguardedMapIndexesInValue(expr.rhsOperand(), assumedIn)
        "in", "~in" ->
            // Membership itself is not a map value-read; still scan the key expr.
            collectUnguardedMapIndexesInValue(expr.lhsOperand(), assumedIn) +
                collectUnguardedMapIndexesInValue(expr.rhsOperand(), assumedIn)
        in mapIndexComparisonOps ->
            collectUnguardedMapIndexesInValue(expr.lhsOperand(), assumedIn) +
                collectUnguardedMapIndexesInValue(expr.rhsOperand(), assumedIn)
        else ->
            collectUnguardedMapIndexesInValue(expr.lhsOperand(), assumedIn) +
                collectUnguardedMapIndexesInValue(expr.rhsOperand(), assumedIn)
    }
    is UnaryOpExprNode ->
        collectUnguardedMapIndexesInValue(expr.operand(), assumedIn)
    is QuantifiedExprNode ->
        collectUnguardedMapIndexesInValue(expr.quantifiedBody(), assumedIn)
    is ParenExprNode ->
        collectUnguardedMapIndexesInValue(expr.innerExpr(), assumedIn)
    is IfElseExprNode -> {
        val condHits = collectUnguardedMapIndexesInValue(expr.condExpr(), assumedIn)
        val thenAssumed = assumedIn + membershipInShapes(expr.condExpr())
        val elseAssumed = assumedIn + membershipNotInShapes(expr.condExpr())
        condHits +
            collectUnguardedMapIndexesInValue(expr.thenExpr(), thenAssumed) +
            collectUnguardedMapIndexesInValue(expr.elseExpr(), elseAssumed)
    }
    is WhenExprNode -> {
        val subjectHits = expr.subjectExpr()?.let {
            collectUnguardedMapIndexesInValue(it, assumedIn)
        }.orEmpty()
        subjectHits + expr.arms().flatMap { arm ->
            when (arm) {
                is WhenArm.Guard -> {
                    val condHits = collectUnguardedMapIndexesInValue(arm.cond, assumedIn)
                    val armAssumed = assumedIn + membershipInShapes(arm.cond)
                    condHits + collectUnguardedMapIndexesInValue(arm.expr, armAssumed)
                }
                is WhenArm.Subject ->
                    collectUnguardedMapIndexesInValue(arm.expr, assumedIn)
                is WhenArm.Else ->
                    collectUnguardedMapIndexesInValue(arm.expr, assumedIn)
            }
        }
    }
    is LetExprNode ->
        collectUnguardedMapIndexesInValue(expr.letInitExpr(), assumedIn) +
            collectUnguardedMapIndexesInValue(expr.bodyExpr(), assumedIn)
    is MethodCallExprNode -> {
        val baseHits = collectUnguardedMapIndexesInValue(expr.baseExpr, assumedIn)
        val argHits = expr.args.flatMap { arg ->
            if (arg is LambdaExprNode) emptyList()
            else collectUnguardedMapIndexesInValue(arg, assumedIn)
        }
        val hofBody = expr.hofBodyOrNull()
        val hofParams = expr.hofParamNamesOrNull()
        val hofHits = if (hofBody != null && hofParams != null) {
            val keysAssumed = keysIterationAssumptions(expr.baseExpr, hofParams)
            collectUnguardedMapIndexesInValue(hofBody, assumedIn + keysAssumed)
        } else {
            emptyList()
        }
        baseHits + argHits + hofHits
    }
    is LambdaExprNode ->
        // Standalone lambda should not appear; HOF path handles bodies.
        collectUnguardedMapIndexesInValue(expr.body, assumedIn)
    is ObjClassLiteralExprNode ->
        expr.fieldEntries.flatMap { (_, v) ->
            collectUnguardedMapIndexesInValue(v, assumedIn)
        }
    is MemberAccessExprNode ->
        collectUnguardedMapIndexesInValue(expr.baseExpr, assumedIn)
    else -> expr.children.filterIsInstance<ExprNode>().flatMap {
        collectUnguardedMapIndexesInValue(it, assumedIn)
    }
}

/**
 * Map indexes that appear in a *value* position (legacy helper for TLA emit paths).
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
        "&", "|", "=>", "<=>", "in", "~in" ->
            collectMapIndexesInValue(expr.lhsOperand()) + collectMapIndexesInValue(expr.rhsOperand())
        else -> collectMapIndexesInValue(expr.lhsOperand()) + collectMapIndexesInValue(expr.rhsOperand())
    }
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

/**
 * If [base] is `m.keys` (optionally `.toList()` / `.toSet()` / `.filter`), and the HOF
 * binds [params] as the element key, assume `m[param]` is present.
 */
private fun keysIterationAssumptions(base: ExprNode, params: List<String>): Set<String> {
    val mapShape = keysSourceMapShape(base) ?: return emptySet()
    // map/filter/associateWith use the element param; fold uses (acc, elem) — last is elem.
    val keyParam = when (params.size) {
        1 -> params[0]
        else -> params.lastOrNull() ?: return emptySet()
    }
    return setOf("$mapShape[$keyParam]")
}

/** Unwrap `m.keys`, `m.keys.toList()`, `m.keys.filter(...)`, etc. → shape of `m`. */
private fun keysSourceMapShape(expr: ExprNode): String? {
    val e = unwrapParensForDomain(expr)
    return when (e) {
        is MemberAccessExprNode ->
            if (e.fieldName == "keys") exprShape(e.baseExpr) else null
        is FieldAccessExprNode ->
            if (e.fieldPath.lastOrNull() == "keys") {
                e.baseSymbol + e.fieldPath.dropLast(1).joinToString("") { ".$it" }
            } else {
                null
            }
        is MethodCallExprNode -> when (e.methodName) {
            "toList", "toSet", "filter" -> keysSourceMapShape(e.baseExpr)
            else -> null
        }
        else -> null
    }
}

/** `key in map` conjuncts as map-index shapes (`map[key]` string form). */
internal fun membershipInShapes(cond: ExprNode): Set<String> {
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
internal fun membershipNotInShapes(cond: ExprNode): Set<String> {
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
internal fun exprShape(expr: ExprNode): String {
    val e = unwrapParensForDomain(expr)
    return when (e) {
        is SymbolValueExprNode -> e.symbol
        is IndexExprNode -> "${exprShape(e.base)}[${exprShape(e.index)}]"
        is FieldAccessExprNode ->
            e.baseSymbol + e.fieldPath.joinToString("") { ".$it" }
        is ThisAccessExprNode ->
            "this." + e.fieldPath.joinToString(".")
        is MemberAccessExprNode ->
            "${exprShape(e.baseExpr)}.${e.fieldName}"
        else -> e.toString()
    }
}

private fun unwrapParensForDomain(expr: ExprNode): ExprNode {
    var e = expr
    while (e is ParenExprNode) e = e.innerExpr()
    return e
}

// Keep typedBoolBinary available if any residual tooling rebuilds guards.
@Suppress("unused")
private fun typedBoolBinary(op: String, lhs: ExprNode, rhs: ExprNode, loc: ProgramLoc): BinaryOpExprNode =
    BinaryOpExprNode(op, lhs, rhs, loc).also {
        it.setInferredType(TypePassType.Inferred(boolType))
    }
