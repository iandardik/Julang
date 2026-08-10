package julay.compiler.ast

import julay.program.type.BoolType
import julay.program.type.IntType
import julay.program.type.StringType
import julay.program.type.Type
import julay.program.type.escapeKotlinStringLiteral

/** True when [expr] is the string literal `""`. */
fun isEmptyStringLiteral(expr: ExprNode): Boolean =
    expr is LiteralValueExprNode &&
        expr.getType() is StringType &&
        expr.literalText().isEmpty()

/**
 * Emit a Kotlin expression of type [julay.program.sync.BoolExprFast], or null if opaque.
 */
fun ExprNode.toBoolExprFastOrNull(
    symbolTypes: Map<String, Type>,
    argSymbols: Set<String>,
): String? = when (this) {
    is LiteralValueExprNode -> when {
        isTrueLiteral() -> "BoolExprFast.True"
        getType() is BoolType && literalText() == "false" ->
            "BoolExprFast.Eq(SyncTerm.Ground(SyncGround.BoolVal(true)), SyncTerm.Ground(SyncGround.BoolVal(false)))"
        else -> null
    }
    is UnaryOpExprNode -> when (op()) {
        "~" -> {
            val inner = operand()
            if (inner is SymbolValueExprNode &&
                inner.symbol !in argSymbols &&
                symbolTypes[inner.symbol] is BoolType
            ) {
                "BoolExprFast.NotLocalBool(\"${inner.symbol.escapeKotlinStringLiteral()}\")"
            } else {
                null
            }
        }
        else -> null
    }
    is ParenExprNode -> innerExpr().toBoolExprFastOrNull(symbolTypes, argSymbols)
    is BinaryOpExprNode -> binaryToBoolExprFast(symbolTypes, argSymbols)
    is SymbolValueExprNode -> {
        if (symbol !in argSymbols && symbolTypes[symbol] is BoolType) {
            "BoolExprFast.LocalBool(\"${symbol.escapeKotlinStringLiteral()}\")"
        } else {
            null
        }
    }
    else -> null
}

/**
 * Emit a Kotlin [julay.program.sync.SyncTerm] expression, or null if opaque.
 */
fun ExprNode.toSyncTermOrNull(
    symbolTypes: Map<String, Type>,
    argSymbols: Set<String>,
    forceString: Boolean = false,
): String? {
    if (forceString) {
        val inner = toSyncTermOrNull(symbolTypes, argSymbols, forceString = false) ?: return null
        if (this is LiteralValueExprNode && getType() is StringType) return inner
        if (this is SymbolValueExprNode && symbolTypes[symbol] is StringType) return inner
        return "SyncTerm.ToString($inner)"
    }
    return when (this) {
        is LiteralValueExprNode -> {
            val g = when (getType()) {
                is IntType -> "SyncGround.IntVal(${literalText()})"
                is BoolType -> "SyncGround.BoolVal(${literalText()})"
                is StringType ->
                    "SyncGround.StringVal(\"${literalText().escapeKotlinStringLiteral()}\")"
                else -> return null
            }
            "SyncTerm.Ground($g)"
        }
        is SymbolValueExprNode -> {
            val ty = symbolTypes[symbol] ?: return null
            when {
                symbol in argSymbols -> {
                    val sort = when (ty) {
                        is IntType -> "SyncTerm.Arg.Sort.Int"
                        is BoolType -> "SyncTerm.Arg.Sort.Bool"
                        is StringType -> "SyncTerm.Arg.Sort.String"
                        else -> return null
                    }
                    "SyncTerm.Arg(\"${symbol.escapeKotlinStringLiteral()}\", $sort)"
                }
                ty is IntType || ty is BoolType || ty is StringType ->
                    "SyncTerm.Local(\"${symbol.escapeKotlinStringLiteral()}\")"
                else -> null
            }
        }
        is BinaryOpExprNode -> {
            val lhsType = lhsOperand().getType()
            val rhsType = rhsOperand().getType()
            when (op()) {
                "+" -> when {
                    lhsType is StringType || rhsType is StringType -> {
                        when {
                            isEmptyStringLiteral(lhsOperand()) && isEmptyStringLiteral(rhsOperand()) ->
                                "SyncTerm.Ground(SyncGround.StringVal(\"\"))"
                            isEmptyStringLiteral(lhsOperand()) ->
                                rhsOperand().toSyncTermOrNull(symbolTypes, argSymbols, forceString = true)
                            isEmptyStringLiteral(rhsOperand()) ->
                                lhsOperand().toSyncTermOrNull(symbolTypes, argSymbols, forceString = true)
                            else -> null
                        }
                    }
                    lhsType is IntType && rhsType is IntType -> {
                        val l = lhsOperand().toSyncTermOrNull(symbolTypes, argSymbols) ?: return null
                        val r = rhsOperand().toSyncTermOrNull(symbolTypes, argSymbols) ?: return null
                        "SyncTerm.IntArith(SyncTerm.IntArith.Op.Add, $l, $r)"
                    }
                    else -> null
                }
                "-" -> {
                    if (lhsType !is IntType || rhsType !is IntType) return null
                    val l = lhsOperand().toSyncTermOrNull(symbolTypes, argSymbols) ?: return null
                    val r = rhsOperand().toSyncTermOrNull(symbolTypes, argSymbols) ?: return null
                    "SyncTerm.IntArith(SyncTerm.IntArith.Op.Sub, $l, $r)"
                }
                "*" -> {
                    if (lhsType !is IntType || rhsType !is IntType) return null
                    val l = lhsOperand().toSyncTermOrNull(symbolTypes, argSymbols) ?: return null
                    val r = rhsOperand().toSyncTermOrNull(symbolTypes, argSymbols) ?: return null
                    "SyncTerm.IntArith(SyncTerm.IntArith.Op.Mul, $l, $r)"
                }
                else -> null
            }
        }
        is ParenExprNode -> innerExpr().toSyncTermOrNull(symbolTypes, argSymbols, forceString = false)
        else -> null
    }
}

private fun BinaryOpExprNode.binaryToBoolExprFast(
    symbolTypes: Map<String, Type>,
    argSymbols: Set<String>,
): String? {
    return when (op()) {
        "&" -> {
            val l = lhsOperand().toBoolExprFastOrNull(symbolTypes, argSymbols) ?: return null
            val r = rhsOperand().toBoolExprFastOrNull(symbolTypes, argSymbols) ?: return null
            "BoolExprFast.And(listOf($l, $r))"
        }
        "=" -> {
            val l = lhsOperand().toSyncTermOrNull(symbolTypes, argSymbols) ?: return null
            val r = rhsOperand().toSyncTermOrNull(symbolTypes, argSymbols) ?: return null
            "BoolExprFast.Eq($l, $r)"
        }
        else -> null
    }
}

/** Combine action guard exprs into one BoolExprFast Kotlin expression, or null if any is opaque. */
fun List<ExprNode>.toCombinedBoolExprFastOrNull(
    symbolTypes: Map<String, Type>,
    argSymbols: Set<String>,
): String? {
    if (isEmpty()) return "BoolExprFast.True"
    if (size == 1) return this[0].toBoolExprFastOrNull(symbolTypes, argSymbols)
    val parts = map { it.toBoolExprFastOrNull(symbolTypes, argSymbols) ?: return null }
    return "BoolExprFast.And(listOf(${parts.joinToString(", ")}))"
}

/**
 * Short reason why a guard list cannot lower to [julay.program.sync.BoolExprFast].
 * Call only when [toCombinedBoolExprFastOrNull] returned null.
 */
fun List<ExprNode>.opaqueFastGuardReason(
    symbolTypes: Map<String, Type>,
    argSymbols: Set<String>,
): String {
    if (isEmpty()) return "empty" // should not happen when combined is null
    for (e in this) {
        if (e.toBoolExprFastOrNull(symbolTypes, argSymbols) == null) {
            return e.opaqueFastGuardKind(symbolTypes, argSymbols)
        }
    }
    return "opaque"
}

private fun ExprNode.opaqueFastGuardKind(
    symbolTypes: Map<String, Type>,
    argSymbols: Set<String>,
): String = when (this) {
    is BinaryOpExprNode -> when (val op = op()) {
        "<", ">", "<=", ">=" -> "relational"
        "&" -> {
            val l = lhsOperand().toBoolExprFastOrNull(symbolTypes, argSymbols)
            val r = rhsOperand().toBoolExprFastOrNull(symbolTypes, argSymbols)
            when {
                l == null -> lhsOperand().opaqueFastGuardKind(symbolTypes, argSymbols)
                r == null -> rhsOperand().opaqueFastGuardKind(symbolTypes, argSymbols)
                else -> "opaque &"
            }
        }
        "=" -> {
            when {
                lhsOperand().toSyncTermOrNull(symbolTypes, argSymbols) == null ->
                    "equality (opaque left term)"
                rhsOperand().toSyncTermOrNull(symbolTypes, argSymbols) == null ->
                    "equality (opaque right term)"
                else -> "equality"
            }
        }
        else -> "op '$op'"
    }
    is UnaryOpExprNode -> when (val op = op()) {
        "~" -> {
            if (operand().toBoolExprFastOrNull(symbolTypes, argSymbols) == null) {
                operand().opaqueFastGuardKind(symbolTypes, argSymbols)
            } else {
                "unary ~"
            }
        }
        else -> "unary '$op'"
    }
    is ParenExprNode -> innerExpr().opaqueFastGuardKind(symbolTypes, argSymbols)
    is MethodCallExprNode -> "method call"
    is FunCallExprNode -> "function call"
    is QuantifiedExprNode -> "quantifier"
    is IfElseExprNode -> "if-else"
    is WhenExprNode -> "when"
    is LetExprNode -> "let"
    is IndexExprNode -> "index"
    is SliceExprNode -> "slice"
    is FieldAccessExprNode, is FieldAccessOnExprNode, is MemberAccessExprNode -> "field access"
    is ListLiteralExprNode, is MapLiteralExprNode, is SetLiteralExprNode -> "collection literal"
    is LambdaExprNode -> "lambda"
    else -> this::class.simpleName?.removeSuffix("ExprNode")?.lowercase() ?: "opaque"
}
