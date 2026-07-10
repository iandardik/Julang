package julay.compiler.ast

import julay.compiler.pass.TypePassType

fun substituteExpr(expr: ExprNode, name: String, replacement: ExprNode): ExprNode {
    return when (expr) {
        is SymbolValueExprNode -> if (expr.symbol == name) replacement else expr
        is UnaryOpExprNode -> UnaryOpExprNode(
            expr.op(),
            substituteExpr(expr.operand(), name, replacement),
            expr.programLocation(),
        ).withTypeOf(expr)
        is BinaryOpExprNode -> BinaryOpExprNode(
            expr.op(),
            substituteExpr(expr.lhsOperand(), name, replacement),
            substituteExpr(expr.rhsOperand(), name, replacement),
            expr.programLocation(),
        ).withTypeOf(expr)
        is IfElseExprNode -> IfElseExprNode(
            substituteExpr(expr.condExpr(), name, replacement),
            substituteExpr(expr.thenExpr(), name, replacement),
            substituteExpr(expr.elseExpr(), name, replacement),
            expr.programLocation(),
        ).withTypeOf(expr)
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
                ).withTypeOf(expr)
            }
        }
        is WhenExprNode -> WhenExprNode(
            expr.subjectExpr()?.let { substituteExpr(it, name, replacement) },
            expr.arms().map { arm ->
                when (arm) {
                    is WhenArm.Subject -> WhenArm.Subject(
                        substituteWhenPattern(arm.pattern, name, replacement),
                        substituteExpr(arm.expr, name, replacement),
                    )
                    is WhenArm.Guard -> WhenArm.Guard(
                        substituteExpr(arm.cond, name, replacement),
                        substituteExpr(arm.expr, name, replacement),
                    )
                    is WhenArm.Else -> WhenArm.Else(substituteExpr(arm.expr, name, replacement))
                }
            },
            expr.programLocation(),
        ).withTypeOf(expr)
        is LiteralValueExprNode -> expr
        is FieldAccessExprNode -> substituteFieldAccess(expr, name, replacement)
        is FieldAccessOnExprNode -> FieldAccessOnExprNode(
            substituteExpr(expr.baseExpr, name, replacement),
            expr.fieldPath,
            expr.programLocation(),
            expr.leafType,
        )
        is ObjClassLiteralExprNode -> substituteObjClassLiteral(expr, name, replacement)
        else -> throw RuntimeException("Unexpected expression node ${expr.javaClass.simpleName} during substitution")
    }
}

private fun <T : ExprNode> T.withTypeOf(original: ExprNode): T {
    try {
        setInferredType(TypePassType.Inferred(original.getType()))
    } catch (_: RuntimeException) {
        // Original may be untyped during early passes; leave as-is.
    }
    return this
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
                    is WhenArm.Subject ->
                        whenPatternReferencesSymbol(arm.pattern, symbol) ||
                            exprReferencesSymbol(arm.expr, symbol)
                    is WhenArm.Guard ->
                        exprReferencesSymbol(arm.cond, symbol) || exprReferencesSymbol(arm.expr, symbol)
                    is WhenArm.Else -> exprReferencesSymbol(arm.expr, symbol)
                }
            }
        }
        is LiteralValueExprNode -> false
        is FieldAccessExprNode -> expr.baseSymbol == symbol
        is FieldAccessOnExprNode -> exprReferencesSymbol(expr.baseExpr, symbol)
        is ObjClassLiteralExprNode -> expr.fieldEntries.any { exprReferencesSymbol(it.second, symbol) }
        else -> throw RuntimeException("Unexpected expression node ${expr.javaClass.simpleName} during symbol reference check")
    }
}

private fun substituteWhenPattern(pattern: WhenPattern, name: String, replacement: ExprNode): WhenPattern {
    return when (pattern) {
        is WhenPattern.Primitive -> pattern
        is WhenPattern.Struct -> WhenPattern.Struct(substituteObjClassLiteral(pattern.literal, name, replacement))
    }
}

private fun whenPatternReferencesSymbol(pattern: WhenPattern, symbol: String): Boolean {
    return when (pattern) {
        is WhenPattern.Primitive -> false
        is WhenPattern.Struct -> exprReferencesSymbol(pattern.literal, symbol)
    }
}

private fun substituteObjClassLiteral(
    expr: ObjClassLiteralExprNode,
    name: String,
    replacement: ExprNode,
): ObjClassLiteralExprNode {
    val newFields = expr.fieldEntries.map { (fieldName, fieldExpr) ->
        fieldName to substituteExpr(fieldExpr, name, replacement)
    }
    return ObjClassLiteralExprNode(
        expr.className,
        newFields,
        expr.programLocation(),
        expr.resolvedStructTypeOrNull(),
    )
}

private fun substituteFieldAccess(
    expr: FieldAccessExprNode,
    name: String,
    replacement: ExprNode,
): ExprNode {
    if (expr.baseSymbol != name) {
        return expr
    }
    val leafType = expr.resolvedLeafTypeOrNull()
        ?: throw RuntimeException("Field access not resolved before substitution at ${expr.programLocation()}")
    return when (replacement) {
        is SymbolValueExprNode -> expr.withBaseSymbol(replacement.symbol)
        is FieldAccessExprNode -> {
            FieldAccessExprNode(
                replacement.baseSymbol,
                replacement.fieldPath + expr.fieldPath,
                expr.programLocation(),
                leafType,
                (replacement.fieldPath + expr.fieldPath).joinToString("."),
            )
        }
        is ObjClassLiteralExprNode -> projectLiteralField(replacement, expr.fieldPath, leafType)
        else -> FieldAccessOnExprNode(replacement, expr.fieldPath, expr.programLocation(), leafType)
    }
}

private fun projectLiteralField(
    literal: ObjClassLiteralExprNode,
    fieldPath: List<String>,
    leafType: julay.program.Type,
): ExprNode {
    if (fieldPath.isEmpty()) {
        return literal
    }
    val fieldName = fieldPath.first()
    val fieldExpr = literal.fieldAssignments[fieldName]
        ?: throw RuntimeException("Missing field \"$fieldName\" while projecting o-class literal")
    val rest = fieldPath.drop(1)
    if (rest.isEmpty()) {
        return fieldExpr
    }
    return when (fieldExpr) {
        is ObjClassLiteralExprNode -> projectLiteralField(fieldExpr, rest, leafType)
        is SymbolValueExprNode -> FieldAccessExprNode(
            fieldExpr.symbol,
            rest,
            fieldExpr.programLocation(),
            leafType,
            rest.joinToString("."),
        )
        is FieldAccessExprNode -> FieldAccessExprNode(
            fieldExpr.baseSymbol,
            fieldExpr.fieldPath + rest,
            fieldExpr.programLocation(),
            leafType,
            (fieldExpr.fieldPath + rest).joinToString("."),
        )
        else -> FieldAccessOnExprNode(fieldExpr, rest, fieldExpr.programLocation(), leafType)
    }
}
