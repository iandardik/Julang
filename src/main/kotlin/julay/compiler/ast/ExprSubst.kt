package julay.compiler.ast

fun substituteExpr(expr: ExprNode, name: String, replacement: ExprNode): ExprNode {
    return when (expr) {
        is SymbolValueExprNode -> if (expr.symbol == name) replacement else expr
        is UnaryOpExprNode -> UnaryOpExprNode(
            expr.op(),
            substituteExpr(expr.operand(), name, replacement),
            expr.programLocation(),
        )
        is BinaryOpExprNode -> BinaryOpExprNode(
            expr.op(),
            substituteExpr(expr.lhsOperand(), name, replacement),
            substituteExpr(expr.rhsOperand(), name, replacement),
            expr.programLocation(),
        )
        is IfElseExprNode -> IfElseExprNode(
            substituteExpr(expr.condExpr(), name, replacement),
            substituteExpr(expr.thenExpr(), name, replacement),
            substituteExpr(expr.elseExpr(), name, replacement),
            expr.programLocation(),
        )
        is LetExprNode -> {
            if (expr.letName() == name) {
                expr
            } else {
                LetExprNode(
                    expr.letName(),
                    expr.letTypeName(),
                    substituteExpr(expr.letInitExpr(), name, replacement),
                    substituteExpr(expr.bodyExpr(), name, replacement),
                    expr.programLocation(),
                    expr.resolvedLetTypeOrNull(),
                )
            }
        }
        is WhenExprNode -> WhenExprNode(
            expr.subjectExpr()?.let { substituteExpr(it, name, replacement) },
            expr.arms().map { arm ->
                when (arm) {
                    is WhenArm.Subject -> WhenArm.Subject(arm.literal, substituteExpr(arm.expr, name, replacement))
                    is WhenArm.Guard -> WhenArm.Guard(
                        substituteExpr(arm.cond, name, replacement),
                        substituteExpr(arm.expr, name, replacement),
                    )
                    is WhenArm.Else -> WhenArm.Else(substituteExpr(arm.expr, name, replacement))
                }
            },
            expr.programLocation(),
        )
        is LiteralValueExprNode -> expr
        is FieldAccessExprNode -> expr
        is ObjClassLiteralExprNode -> expr
        else -> throw RuntimeException("Unexpected expression node ${expr.javaClass.simpleName} during substitution")
    }
}

fun exprReferencesSymbol(expr: ExprNode, symbol: String): Boolean {
    return when (expr) {
        is SymbolValueExprNode -> expr.symbol == symbol
        is UnaryOpExprNode -> exprReferencesSymbol(expr.operand(), symbol)
        is BinaryOpExprNode ->
            exprReferencesSymbol(expr.lhsOperand(), symbol) || exprReferencesSymbol(expr.rhsOperand(), symbol)
        is IfElseExprNode ->
            exprReferencesSymbol(expr.condExpr(), symbol) ||
                exprReferencesSymbol(expr.thenExpr(), symbol) ||
                exprReferencesSymbol(expr.elseExpr(), symbol)
        is LetExprNode -> {
            if (expr.letName() == symbol) {
                exprReferencesSymbol(expr.letInitExpr(), symbol)
            } else {
                exprReferencesSymbol(expr.letInitExpr(), symbol) ||
                    exprReferencesSymbol(expr.bodyExpr(), symbol)
            }
        }
        is WhenExprNode -> {
            val subjectRefs = expr.subjectExpr()?.let { exprReferencesSymbol(it, symbol) } == true
            subjectRefs || expr.arms().any { arm ->
                when (arm) {
                    is WhenArm.Subject -> exprReferencesSymbol(arm.expr, symbol)
                    is WhenArm.Guard ->
                        exprReferencesSymbol(arm.cond, symbol) || exprReferencesSymbol(arm.expr, symbol)
                    is WhenArm.Else -> exprReferencesSymbol(arm.expr, symbol)
                }
            }
        }
        is LiteralValueExprNode -> false
        is FieldAccessExprNode -> expr.baseSymbol == symbol
        is ObjClassLiteralExprNode -> expr.fieldEntries.any { exprReferencesSymbol(it.second, symbol) }
        else -> throw RuntimeException("Unexpected expression node ${expr.javaClass.simpleName} during symbol reference check")
    }
}
