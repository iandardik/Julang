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
        is ParenExprNode -> ParenExprNode(
            substituteExpr(expr.innerExpr(), name, replacement),
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
                    expr.letTypeExpr(),
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
        is ThisAccessExprNode -> expr // state path is not a substitutable binder
        is FieldAccessExprNode -> substituteFieldAccess(expr, name, replacement)
        is MemberAccessExprNode -> MemberAccessExprNode(
            substituteExpr(expr.baseExpr, name, replacement),
            expr.fieldName,
            expr.programLocation(),
        ).withTypeOf(expr)
        is MethodCallExprNode -> MethodCallExprNode(
            substituteExpr(expr.baseExpr, name, replacement),
            expr.methodName,
            expr.args.map { substituteExpr(it, name, replacement) },
            expr.programLocation(),
        ).also { copy ->
            val api = expr.resolvedApiNameOrNull()
            val pf = expr.resolvedProcFunOrNull()
            if (api != null && pf != null) {
                copy.resolveApiProcFun(api, pf)
            } else {
                val body = expr.hofBodyOrNull()
                val params = expr.hofParamNamesOrNull()
                val types = expr.hofParamTypesOrNull()
                if (body != null && params != null && types != null) {
                    copy.resolveHof(substituteExpr(body, name, replacement), params, types)
                }
            }
        }.withTypeOf(expr)
        is LambdaExprNode -> LambdaExprNode(
            expr.params,
            if (expr.params.contains(name)) expr.body else substituteExpr(expr.body, name, replacement),
            expr.programLocation(),
        )
        is FieldAccessOnExprNode -> FieldAccessOnExprNode(
            substituteExpr(expr.baseExpr, name, replacement),
            expr.fieldPath,
            expr.programLocation(),
            expr.leafType,
        )
        is ObjClassLiteralExprNode -> substituteObjClassLiteral(expr, name, replacement)
        is ListLiteralExprNode -> ListLiteralExprNode(
            expr.elements.map { substituteExpr(it, name, replacement) },
            expr.programLocation(),
            expr.resolvedListTypeOrNull(),
            typeArgs = expr.typeArgs,
        ).withTypeOf(expr)
        is SetLiteralExprNode -> SetLiteralExprNode(
            expr.elements.map { substituteExpr(it, name, replacement) },
            expr.programLocation(),
            expr.resolvedSetTypeOrNull(),
            typeArgs = expr.typeArgs,
        ).withTypeOf(expr)
        is MapLiteralExprNode -> MapLiteralExprNode(
            expr.entries.map { (k, v) -> substituteExpr(k, name, replacement) to substituteExpr(v, name, replacement) },
            expr.programLocation(),
            expr.resolvedMapTypeOrNull(),
            typeArgs = expr.typeArgs,
        ).withTypeOf(expr)
        is IndexExprNode -> IndexExprNode(
            substituteExpr(expr.base, name, replacement),
            substituteExpr(expr.index, name, replacement),
            expr.programLocation(),
        ).withTypeOf(expr)
        is FunCallExprNode -> FunCallExprNode(
            expr.callName(),
            expr.callArgs().map { substituteExpr(it, name, replacement) },
            expr.programLocation(),
            expr.resolvedFunOrNull(),
            typeArgs = expr.callTypeArgs(),
        ).also { copy ->
            expr.resolvedBuiltinOrNull()?.let { copy.resolveBuiltin(it) }
            expr.resolvedProcFunOrNull()?.let { copy.resolveProcFun(it) }
            expr.specializedBodyOrNull()?.let { body ->
                copy.resolveSpecializedBody(substituteExpr(body, name, replacement))
            }
            val namedFun = expr.namedFunArgNodeOrNull()
            val namedParam = expr.namedFunParamNameOrNull()
            val namedBody = expr.namedFunBodyOrNull()
            val namedElem = expr.namedFunElemTypeOrNull()
            if (namedParam != null && namedBody != null && namedElem != null) {
                copy.resolveNamedFunArg(
                    namedFun,
                    namedParam,
                    substituteExpr(namedBody, name, replacement),
                    namedElem,
                )
            }
            try {
                copy.resolveInstantiatedReturnType(expr.getType())
            } catch (_: RuntimeException) {
            }
        }.withTypeOf(expr)
        is QuantifiedExprNode -> {
            if (expr.binderName() == name) {
                expr
            } else {
                QuantifiedExprNode(
                    expr.isUniversal(),
                    expr.binderName(),
                    expr.binderTypeExpr(),
                    substituteExpr(expr.quantifiedBody(), name, replacement),
                    expr.programLocation(),
                ).withTypeOf(expr)
            }
        }
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
        is ParenExprNode -> exprReferencesSymbol(expr.innerExpr(), symbol)
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
        // `this.x` is always process state, never a binder / action arg named x.
        is ThisAccessExprNode -> false
        is FieldAccessExprNode -> expr.baseSymbol == symbol
        is MemberAccessExprNode -> exprReferencesSymbol(expr.baseExpr, symbol)
        is MethodCallExprNode ->
            exprReferencesSymbol(expr.baseExpr, symbol) ||
                expr.args.any { exprReferencesSymbol(it, symbol) } ||
                (expr.hofBodyOrNull()?.let { exprReferencesSymbol(it, symbol) } == true)
        is LambdaExprNode ->
            if (expr.params.contains(symbol)) {
                false
            } else {
                exprReferencesSymbol(expr.body, symbol)
            }
        is FieldAccessOnExprNode -> exprReferencesSymbol(expr.baseExpr, symbol)
        is ObjClassLiteralExprNode -> expr.fieldEntries.any { exprReferencesSymbol(it.second, symbol) }
        is ListLiteralExprNode -> expr.elements.any { exprReferencesSymbol(it, symbol) }
        is SetLiteralExprNode -> expr.elements.any { exprReferencesSymbol(it, symbol) }
        is MapLiteralExprNode -> expr.entries.any { exprReferencesSymbol(it.first, symbol) || exprReferencesSymbol(it.second, symbol) }
        is IndexExprNode ->
            exprReferencesSymbol(expr.base, symbol) || exprReferencesSymbol(expr.index, symbol)
        is FunCallExprNode -> expr.callArgs().any { exprReferencesSymbol(it, symbol) }
        is QuantifiedExprNode -> {
            if (expr.binderName() == symbol) {
                false
            } else {
                exprReferencesSymbol(expr.quantifiedBody(), symbol)
            }
        }
        else -> throw RuntimeException("Unexpected expression node ${expr.javaClass.simpleName} during symbol reference check")
    }
}

fun collectFunCallNames(expr: ExprNode): Set<String> {
    return when (expr) {
        is FunCallExprNode -> setOf(expr.callName()) + expr.callArgs().flatMap { collectFunCallNames(it) }
        is UnaryOpExprNode -> collectFunCallNames(expr.operand())
        is ParenExprNode -> collectFunCallNames(expr.innerExpr())
        is BinaryOpExprNode ->
            collectFunCallNames(expr.lhsOperand()) + collectFunCallNames(expr.rhsOperand())
        is IfElseExprNode ->
            collectFunCallNames(expr.condExpr()) +
                collectFunCallNames(expr.thenExpr()) +
                collectFunCallNames(expr.elseExpr())
        is LetExprNode ->
            collectFunCallNames(expr.letInitExpr()) + collectFunCallNames(expr.bodyExpr())
        is WhenExprNode -> {
            val subjectCalls = expr.subjectExpr()?.let { collectFunCallNames(it) } ?: emptySet()
            subjectCalls + expr.arms().flatMap { arm ->
                when (arm) {
                    is WhenArm.Subject -> {
                        val patternCalls = when (val pattern = arm.pattern) {
                            is WhenPattern.Primitive -> emptySet()
                            is WhenPattern.Struct -> collectFunCallNames(pattern.literal)
                        }
                        patternCalls + collectFunCallNames(arm.expr)
                    }
                    is WhenArm.Guard -> collectFunCallNames(arm.cond) + collectFunCallNames(arm.expr)
                    is WhenArm.Else -> collectFunCallNames(arm.expr)
                }
            }
        }
        is LiteralValueExprNode -> emptySet()
        is ThisAccessExprNode -> emptySet()
        is FieldAccessExprNode -> emptySet()
        is MemberAccessExprNode -> collectFunCallNames(expr.baseExpr)
        is MethodCallExprNode ->
            collectFunCallNames(expr.baseExpr) +
                expr.args.flatMap { collectFunCallNames(it) } +
                (expr.hofBodyOrNull()?.let { collectFunCallNames(it) } ?: emptySet())
        is LambdaExprNode -> collectFunCallNames(expr.body)
        is FieldAccessOnExprNode -> collectFunCallNames(expr.baseExpr)
        is ObjClassLiteralExprNode -> expr.fieldEntries.flatMap { collectFunCallNames(it.second) }.toSet()
        is ListLiteralExprNode -> expr.elements.flatMap { collectFunCallNames(it) }.toSet()
        is SetLiteralExprNode -> expr.elements.flatMap { collectFunCallNames(it) }.toSet()
        is MapLiteralExprNode -> expr.entries.flatMap { collectFunCallNames(it.first) + collectFunCallNames(it.second) }.toSet()
        is IndexExprNode -> collectFunCallNames(expr.base) + collectFunCallNames(expr.index)
        is SymbolValueExprNode -> emptySet()
        is QuantifiedExprNode -> collectFunCallNames(expr.quantifiedBody())
        else -> throw RuntimeException("Unexpected expression node ${expr.javaClass.simpleName} during fun-call collection")
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
        expr.typeExpr,
        newFields,
        expr.programLocation(),
        expr.resolvedStructTypeOrNull(),
    ).withTypeOf(expr)
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
    leafType: julay.program.type.Type,
): ExprNode {
    if (fieldPath.isEmpty()) {
        return literal
    }
    val fieldName = fieldPath.first()
    val fieldExpr = literal.fieldAssignments[fieldName]
        ?: throw RuntimeException("Missing field \"$fieldName\" while projecting obj literal")
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
