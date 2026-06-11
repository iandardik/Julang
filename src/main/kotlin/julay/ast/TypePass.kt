package julay.ast

import julay.program.*

sealed interface TypePassType {
    data object Uninferred : TypePassType
    data class Inferred(val type: Type) : TypePassType
}

fun ASTNode.typePass(
    symbolEnv: Map<String, Type> = emptyMap(),
    registry: ObjClassRegistry = ObjClassRegistry.EMPTY,
): List<CompileError> = when (this) {
    is RootNode -> typePassRoot(symbolEnv, registry)
    is ProcClassNode -> typePassProcClass(symbolEnv, registry)
    is VarNode -> typePassVarNode(symbolEnv, registry)
    is ConstructorNode -> typePassConstructor(symbolEnv, registry)
    is TransitionNode -> typePassTransition(symbolEnv, registry)
    is ArgNode -> typePassArgNode(symbolEnv, registry)
    is GuardNode -> typePassGuard(symbolEnv, registry)
    is VarTransitNode -> typePassVarTransit(symbolEnv, registry)
    is BinaryOpExprNode -> typePassBinaryOp(symbolEnv, registry)
    is IfElseExprNode -> typePassIfElse(symbolEnv, registry)
    is ObjClassLiteralExprNode -> typePassObjClassLiteral(symbolEnv, registry)
    is FieldAccessExprNode -> typePassFieldAccess(symbolEnv, registry)
    is ExprNode -> typePassExpr(symbolEnv, registry)
    else -> children.flatMap { it.typePass(symbolEnv, registry) }
}

private fun RootNode.typePassRoot(symbolEnv: Map<String, Type>, registry: ObjClassRegistry): List<CompileError> {
    val built = ObjClassRegistry.build(declNodes().flatMap { it.objClassPass() })
    return built.errors + declNodes().flatMap { it.typePass(symbolEnv, built) }
}

private fun ProcClassNode.typePassProcClass(
    symbolEnv: Map<String, Type>,
    registry: ObjClassRegistry,
): List<CompileError> {
    val localDecls = localDecls()
    val varErrors = localDecls
        .filterIsInstance<VarNode>()
        .flatMap { it.typePass(symbolEnv, registry) }
    val localSymbolEnv = localDecls
        .filterIsInstance<VarNode>()
        .associate { it.name to it.type }
    return varErrors + localDecls.flatMap { decl ->
        if (decl is VarNode) emptyList() else decl.typePass(localSymbolEnv, registry)
    }
}

private fun VarNode.typePassVarNode(symbolEnv: Map<String, Type>, registry: ObjClassRegistry): List<CompileError> =
    when (val result = registry.resolveTypeName(typeName)) {
        is TypeResolveResult.Found -> {
            resolveType(result.type)
            emptyList()
        }
        is TypeResolveResult.NotFound ->
            listOf(OneLocCompileError(programLocation(), "Unknown type \"$typeName\" for state variable \"$name\""))
    }

private fun ConstructorNode.typePassConstructor(
    symbolEnv: Map<String, Type>,
    registry: ObjClassRegistry,
): List<CompileError> {
    val argErrors = constructorArgs().typePass(symbolEnv, registry)
    val actionEnv = symbolEnv + constructorArgs().argsTypeMap() + flattenActionArgEnv(constructorArgs().actionArgs())
    return argErrors + body().flatMap { it.typePass(actionEnv, registry) }
}

private fun TransitionNode.typePassTransition(
    symbolEnv: Map<String, Type>,
    registry: ObjClassRegistry,
): List<CompileError> {
    val argErrors = transitionArgs().typePass(symbolEnv, registry)
    val actionEnv = symbolEnv + transitionArgs().argsTypeMap() + flattenActionArgEnv(transitionArgs().actionArgs())
    return argErrors + body().flatMap { it.typePass(actionEnv, registry) }
}

private fun ArgNode.typePassArgNode(symbolEnv: Map<String, Type>, registry: ObjClassRegistry): List<CompileError> =
    when (val result = registry.resolveTypeName(argTypeName())) {
        is TypeResolveResult.Found -> {
            resolveArgType(result.type)
            emptyList()
        }
        is TypeResolveResult.NotFound ->
            listOf(OneLocCompileError(programLocation(), "Unknown type \"${argTypeName()}\" for action argument \"${argName()}\""))
    }

private fun GuardNode.typePassGuard(symbolEnv: Map<String, Type>, registry: ObjClassRegistry): List<CompileError> {
    val childrenErrors = children.flatMap { it.typePass(symbolEnv, registry) }
    if (childrenErrors.isNotEmpty()) {
        return childrenErrors
    }
    val guardTypeErrors = assertOrCompileError(
        guardExpr().getType() is BoolType,
        OneLocCompileError(programLocation(), "Expected guards to be Boolean-valued expressions"),
    )
    return childrenErrors + guardTypeErrors
}

private fun VarTransitNode.typePassVarTransit(
    symbolEnv: Map<String, Type>,
    registry: ObjClassRegistry,
): List<CompileError> {
    val childrenErrors = children.flatMap { it.typePass(symbolEnv, registry) }
    if (childrenErrors.isNotEmpty()) {
        return childrenErrors
    }
    val varType = symbolEnv[varName]
    val varErrors = if (varType == null) {
        assertOrCompileError(
            false,
            OneLocCompileError(programLocation(), "Unknown variable \"$varName\" in transit assignment"),
        )
    } else if (fieldPath.isEmpty()) {
        assertOrCompileError(
            transitExpr().getType() == varType,
            OneLocCompileError(
                programLocation(),
                "Expected assignment to \"$varName\" ($varType) but got expression of type ${transitExpr().getType()}",
            ),
        )
    } else {
        when (val result = resolveFieldPath(varType, fieldPath)) {
            is FieldPathResult.Error -> listOf(OneLocCompileError(programLocation(), result.message))
            is FieldPathResult.Resolved -> {
                if (result.type is ObjClassType) {
                    assertOrCompileError(
                        false,
                        OneLocCompileError(
                            programLocation(),
                            "Cannot assign a scalar to o-class field \"${transitKey()}\"; assign the whole value instead",
                        ),
                    )
                } else {
                    assertOrCompileError(
                        transitExpr().getType() == result.type,
                        OneLocCompileError(
                            programLocation(),
                            "Expected assignment to \"${transitKey()}\" (${result.type}) but got expression of type ${transitExpr().getType()}",
                        ),
                    )
                }
            }
        }
    }
    return childrenErrors + varErrors
}

private fun BinaryOpExprNode.typePassBinaryOp(
    symbolEnv: Map<String, Type>,
    registry: ObjClassRegistry,
): List<CompileError> {
    val childErrors = children.flatMap { it.typePass(symbolEnv, registry) }
    if (childErrors.isNotEmpty()) {
        return childErrors
    }
    inferExprType(symbolEnv)
    val lhsType = lhsOperand().getType()
    val rhsType = rhsOperand().getType()
    val structOpErrors = if (lhsType is ObjClassType || rhsType is ObjClassType) {
        when (op()) {
            "=", "#" -> assertOrCompileError(
                lhsType == rhsType,
                OneLocCompileError(
                    programLocation(),
                    "Expected both sides of \"${op()}\" to have the same o-class type, got $lhsType and $rhsType",
                ),
            )
            else -> listOf(OneLocCompileError(programLocation(), "Cannot apply \"${op()}\" to o-class type $lhsType"))
        }
    } else {
        emptyList()
    }
    return childErrors + structOpErrors
}

private fun IfElseExprNode.typePassIfElse(symbolEnv: Map<String, Type>, registry: ObjClassRegistry): List<CompileError> {
    val childErrors = children.flatMap { it.typePass(symbolEnv, registry) }
    inferExprType(symbolEnv)
    return childErrors + ifElseTypeErrors()
}

private fun IfElseExprNode.ifElseTypeErrors(): List<CompileError> {
    val errors = mutableListOf<CompileError>()
    if (condExpr().getType() !is BoolType) {
        errors.add(OneLocCompileError(programLocation(), "Expected if-condition to be Boolean"))
    }
    val thenT = thenExpr().getType()
    val elseT = elseExpr().getType()
    if (thenT != elseT) {
        errors.add(
            TwoLocsCompileError(
                thenExpr().programLocation(),
                elseExpr().programLocation(),
                "Expected if-branches to have the same type, got $thenT and $elseT",
            ),
        )
    }
    return errors
}

private fun ObjClassLiteralExprNode.typePassObjClassLiteral(
    symbolEnv: Map<String, Type>,
    registry: ObjClassRegistry,
): List<CompileError> {
    val classErrors = when (val classResult = registry.resolveTypeName(className)) {
        is TypeResolveResult.NotFound ->
            listOf(OneLocCompileError(programLocation(), "Unknown o-class \"$className\" in o-class literal"))
        is TypeResolveResult.Found -> {
            if (classResult.type !is ObjClassType) {
                listOf(OneLocCompileError(programLocation(), "\"$className\" is not an o-class type"))
            } else {
                resolveLiteralType(classResult.type)
                emptyList()
            }
        }
    }
    if (classErrors.isNotEmpty()) {
        return classErrors
    }

    val resolvedType = structType
    val duplicateFieldErrors = fieldEntries
        .groupBy { it.first }
        .flatMap { (name, entries) ->
            if (entries.size == 1) emptyList()
            else listOf(
                TwoLocsCompileError(
                    entries[0].second.programLocation(),
                    entries[1].second.programLocation(),
                    "Expected o-class literal fields to have unique names, but found duplicate \"$name\"",
                ),
            )
        }
    val providedFields = fieldEntries.map { it.first }.toSet()
    val expectedFields = resolvedType.fields.map { it.name }.toSet()
    val missingFields = expectedFields - providedFields
    val extraFields = providedFields - expectedFields
    val fieldSetErrors = buildList {
        if (missingFields.isNotEmpty()) {
            add(OneLocCompileError(programLocation(), "O-class literal for \"$className\" is missing fields: $missingFields"))
        }
        if (extraFields.isNotEmpty()) {
            add(OneLocCompileError(programLocation(), "O-class literal for \"$className\" has unknown fields: $extraFields"))
        }
    }
    if (duplicateFieldErrors.isNotEmpty() || fieldSetErrors.isNotEmpty()) {
        return classErrors + duplicateFieldErrors + fieldSetErrors
    }

    val childErrors = children.flatMap { it.typePass(symbolEnv, registry) }
    if (childErrors.isNotEmpty()) {
        return childErrors
    }

    val matchErrors = resolvedType.fields.flatMap { field ->
        val expr = fieldAssignments.getValue(field.name)
        assertOrCompileError(
            expr.getType() == field.type,
            OneLocCompileError(
                expr.programLocation(),
                "Expected field \"${field.name}\" of \"$className\" to have type ${field.type} but got ${expr.getType()}",
            ),
        )
    }
    inferExprType(symbolEnv)
    return matchErrors
}

private fun FieldAccessExprNode.typePassFieldAccess(
    symbolEnv: Map<String, Type>,
    registry: ObjClassRegistry,
): List<CompileError> {
    val baseType = symbolEnv[baseSymbol]
    if (baseType == null) {
        return listOf(OneLocCompileError(programLocation(), "Unknown variable \"$baseSymbol\" in field access"))
    }
    return when (val result = resolveFieldPath(baseType, fieldPath)) {
        is FieldPathResult.Error -> listOf(OneLocCompileError(programLocation(), result.message))
        is FieldPathResult.Resolved -> {
            resolveFieldAccess(result.type, result.relPath)
            val childErrors = children.flatMap { it.typePass(symbolEnv, registry) }
            inferExprType(symbolEnv)
            childErrors
        }
    }
}

private fun ExprNode.typePassExpr(symbolEnv: Map<String, Type>, registry: ObjClassRegistry): List<CompileError> {
    val childrenErrors = children.flatMap { it.typePass(symbolEnv, registry) }
    inferExprType(symbolEnv)
    return childrenErrors
}

private fun ExprNode.inferExprType(symbolEnv: Map<String, Type>) {
    setInferredType(TypePassType.Inferred(inferType(symbolEnv)))
}
