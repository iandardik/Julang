package julay.compiler.pass

import julay.compiler.*
import julay.compiler.ast.*
import julay.compiler.decl.*
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
    is EffectNode -> typePassEffect(symbolEnv, registry)
    is EffectCallNode -> typePassEffectCall(symbolEnv, registry)
    is EffectAssignNode -> typePassEffectAssign(symbolEnv, registry)
    is BinaryOpExprNode -> typePassBinaryOp(symbolEnv, registry)
    is IfElseExprNode -> typePassIfElse(symbolEnv, registry)
    is LetExprNode -> typePassLet(symbolEnv, registry)
    is WhenExprNode -> typePassWhen(symbolEnv, registry)
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

private fun EffectNode.typePassEffect(symbolEnv: Map<String, Type>, registry: ObjClassRegistry): List<CompileError> =
    children.flatMap { it.typePass(symbolEnv, registry) }

private fun EffectCallNode.typePassEffectCall(
    symbolEnv: Map<String, Type>,
    registry: ObjClassRegistry,
): List<CompileError> {
    val childrenErrors = children.flatMap { it.typePass(symbolEnv, registry) }
    if (childrenErrors.isNotEmpty()) {
        return childrenErrors
    }
    val builtin = EffectBuiltinRegistry.lookup(callName())
    if (builtin == null) {
        return listOf(OneLocCompileError(programLocation(), "Unknown effect builtin \"${callName()}\""))
    }
    if (builtin.paramTypes.size != callArgs().size) {
        return listOf(
            OneLocCompileError(
                programLocation(),
                "Expected effect \"${callName()}\" to take ${builtin.paramTypes.size} argument(s) but got ${callArgs().size}",
            ),
        )
    }
    return callArgs().zip(builtin.paramTypes).flatMap { (arg, expectedType) ->
        assertOrCompileError(
            arg.getType() == expectedType,
            OneLocCompileError(
                arg.programLocation(),
                "Expected argument to \"${callName()}\" to have type $expectedType but got ${arg.getType()}",
            ),
        )
    }
}

private fun EffectAssignNode.typePassEffectAssign(
    symbolEnv: Map<String, Type>,
    registry: ObjClassRegistry,
): List<CompileError> {
    val childrenErrors = children.flatMap { it.typePass(symbolEnv, registry) }
    if (childrenErrors.isNotEmpty()) {
        return childrenErrors
    }
    val callErrors = EffectCallNode(callName(), callArgs(), programLocation()).typePassEffectCall(symbolEnv, registry)
    if (callErrors.isNotEmpty()) {
        return callErrors
    }
    val builtin = EffectBuiltinRegistry.lookup(callName())!!
    val returnType = builtin.returnType
        ?: return listOf(
            OneLocCompileError(
                programLocation(),
                "Effect \"${callName()}\" cannot be used in an assignment because it returns no value",
            ),
        )
    val varType = symbolEnv[varName]
    val varErrors = if (varType == null) {
        assertOrCompileError(
            false,
            OneLocCompileError(programLocation(), "Unknown variable \"$varName\" in effect assignment"),
        )
    } else if (fieldPath.isEmpty()) {
        assertOrCompileError(
            returnType == varType,
            OneLocCompileError(
                programLocation(),
                "Expected assignment to \"$varName\" ($varType) but got effect returning $returnType",
            ),
        )
    } else {
        when (val result = resolveFieldPath(varType, fieldPath)) {
            is FieldPathResult.Error -> listOf(OneLocCompileError(programLocation(), result.message))
            is FieldPathResult.Resolved -> {
                assertOrCompileError(
                    returnType == result.type,
                    OneLocCompileError(
                        programLocation(),
                        "Expected assignment to \"${assignKey()}\" (${result.type}) but got effect returning $returnType",
                    ),
                )
            }
        }
    }
    return childrenErrors + callErrors + varErrors
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

private fun LetExprNode.typePassLet(symbolEnv: Map<String, Type>, registry: ObjClassRegistry): List<CompileError> {
    val typeErrors = when (val result = registry.resolveTypeName(letTypeName())) {
        is TypeResolveResult.Found -> {
            if (result.type is ObjClassType) {
                listOf(
                    OneLocCompileError(
                        programLocation(),
                        "let bindings support only primitive types",
                    ),
                )
            } else {
                resolveLetType(result.type)
                emptyList()
            }
        }
        is TypeResolveResult.NotFound ->
            listOf(
                OneLocCompileError(
                    programLocation(),
                    "Unknown type \"${letTypeName()}\" for let binding \"${letName()}\"",
                ),
            )
    }
    if (typeErrors.isNotEmpty()) {
        return typeErrors
    }

    val selfRefErrors = assertOrCompileError(
        letName() in symbolEnv || !exprReferencesSymbol(letInitExpr(), letName()),
        OneLocCompileError(
            letInitExpr().programLocation(),
            "let initializer for \"${letName()}\" cannot reference the bound name",
        ),
    )
    if (selfRefErrors.isNotEmpty()) {
        return selfRefErrors
    }

    val initErrors = letInitExpr().typePass(symbolEnv, registry)
    if (initErrors.isNotEmpty()) {
        return initErrors
    }

    val declaredType = resolvedLetType
    val initTypeErrors = assertOrCompileError(
        letInitExpr().getType() == declaredType,
        OneLocCompileError(
            letInitExpr().programLocation(),
            "Expected let initializer for \"${letName()}\" to have type $declaredType but got ${letInitExpr().getType()}",
        ),
    )
    if (initTypeErrors.isNotEmpty()) {
        return initTypeErrors
    }

    val bodyEnv = symbolEnv + (letName() to declaredType)
    val bodyErrors = bodyExpr().typePass(bodyEnv, registry)
    inferExprType(bodyEnv)
    return bodyErrors
}

private fun WhenExprNode.typePassWhen(symbolEnv: Map<String, Type>, registry: ObjClassRegistry): List<CompileError> {
    val subjectErrors = subjectExpr()?.typePass(symbolEnv, registry) ?: emptyList()
    if (subjectErrors.isNotEmpty()) {
        return subjectErrors
    }

    val structureErrors = whenStructureErrors()
    if (structureErrors.isNotEmpty()) {
        return structureErrors
    }

    val armErrors = arms().flatMap { arm ->
        when (arm) {
            is WhenArm.Subject -> arm.expr.typePass(symbolEnv, registry)
            is WhenArm.Guard -> arm.cond.typePass(symbolEnv, registry) + arm.expr.typePass(symbolEnv, registry)
            is WhenArm.Else -> arm.expr.typePass(symbolEnv, registry)
        }
    }
    if (armErrors.isNotEmpty()) {
        return armErrors
    }

    inferExprType(symbolEnv)
    return whenTypeErrors()
}

private fun WhenExprNode.whenStructureErrors(): List<CompileError> {
    val errors = mutableListOf<CompileError>()
    if (arms().isEmpty()) {
        errors.add(OneLocCompileError(programLocation(), "Expected when expression to have at least one arm"))
        return errors
    }
    val elseArms = arms().filterIsInstance<WhenArm.Else>()
    if (elseArms.size != 1) {
        errors.add(OneLocCompileError(programLocation(), "Expected when expression to have exactly one else arm"))
    }
    if (arms().last() !is WhenArm.Else) {
        errors.add(OneLocCompileError(programLocation(), "Expected else arm to be the last arm in when expression"))
    }
    if (arms().count { it !is WhenArm.Else } == 0) {
        errors.add(OneLocCompileError(programLocation(), "Expected when expression to have at least one non-else arm"))
    }
    if (subjectExpr() != null && arms().any { it is WhenArm.Guard }) {
        errors.add(OneLocCompileError(programLocation(), "Expected subject when arms to use literals, not guard conditions"))
    }
    if (subjectExpr() == null && arms().any { it is WhenArm.Subject }) {
        errors.add(OneLocCompileError(programLocation(), "Expected guard when arms to use conditions, not literals"))
    }
    return errors
}

private fun WhenExprNode.whenTypeErrors(): List<CompileError> {
    val errors = mutableListOf<CompileError>()

    subjectExpr()?.let { subject ->
        if (subject.getType() is ObjClassType) {
            errors.add(OneLocCompileError(subject.programLocation(), "Expected when subject to be a primitive type"))
        }
    }

    arms().filterIsInstance<WhenArm.Guard>().forEach { arm ->
        if (arm.cond.getType() !is BoolType) {
            errors.add(
                OneLocCompileError(arm.cond.programLocation(), "Expected when guard condition to be Boolean"),
            )
        }
    }

    subjectExpr()?.let { subject ->
        val subjectType = subject.getType()
        val literalArms = arms().filterIsInstance<WhenArm.Subject>()
        literalArms.forEach { arm ->
            val literalType = arm.literal.whenLiteralType()
            if (literalType != subjectType) {
                errors.add(
                    OneLocCompileError(
                        arm.expr.programLocation(),
                        "Expected when arm literal to match subject type $subjectType but got $literalType",
                    ),
                )
            }
        }
        val duplicateLiterals = literalArms
            .groupBy { it.literal }
            .filter { it.value.size > 1 }
        duplicateLiterals.forEach { (literal, entries) ->
            errors.add(
                TwoLocsCompileError(
                    entries[0].expr.programLocation(),
                    entries[1].expr.programLocation(),
                    "Expected when subject arms to have unique literals, but found duplicate $literal",
                ),
            )
        }
    }

    val branchTypes = arms().map { arm ->
        when (arm) {
            is WhenArm.Subject -> arm.expr.getType()
            is WhenArm.Guard -> arm.expr.getType()
            is WhenArm.Else -> arm.expr.getType()
        }
    }
    val expectedType = branchTypes.first()
    branchTypes.drop(1).forEachIndexed { index, branchType ->
        if (branchType != expectedType) {
            val arm = arms()[index + 1]
            val armLoc = when (arm) {
                is WhenArm.Subject -> arm.expr.programLocation()
                is WhenArm.Guard -> arm.expr.programLocation()
                is WhenArm.Else -> arm.expr.programLocation()
            }
            errors.add(
                TwoLocsCompileError(
                    arms()[0].branchExpr().programLocation(),
                    armLoc,
                    "Expected when branches to have the same type, got $expectedType and $branchType",
                ),
            )
        }
    }

    return errors
}

private fun WhenArm.branchExpr(): ExprNode = when (this) {
    is WhenArm.Subject -> expr
    is WhenArm.Guard -> expr
    is WhenArm.Else -> expr
}

private fun WhenLiteral.whenLiteralType(): Type = when (this) {
    is WhenLiteral.IntLit -> intType
    is WhenLiteral.StringLit -> stringType
    is WhenLiteral.BoolLit -> boolType
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
