package julay.compiler.pass

import julay.compiler.*
import julay.compiler.ast.*
import julay.compiler.decl.*
import julay.program.*

sealed interface TypePassType {
    data object Uninferred : TypePassType
    data class Inferred(val type: Type) : TypePassType
}

fun RootNode.typePass(unit: CompilationUnit): List<CompileError> {
    val built = ObjClassRegistry.build(declNodes().flatMap { it.objClassPass() })
    if (built.errors.isNotEmpty()) {
        return built.errors
    }
    val allFuns = declNodes().filterIsInstance<FunNode>().associateBy { it.name() }
    val signatureErrors = allFuns.values.flatMap { it.typePassFunSignature(built) }
    val recursionErrors = funRecursionErrors(allFuns)
    val funBodyErrors = unit.modules.flatMap { module ->
        val callable = callableFuns(module)
        module.root.declNodes().filterIsInstance<FunNode>().flatMap { funNode ->
            funNode.typePassFunBody(callable, built)
        }
    }
    val entryModule = unit.modules.firstOrNull { it.isEntry }
        ?: return built.errors + signatureErrors + recursionErrors + funBodyErrors
    val entryCallable = callableFuns(entryModule)
    val otherErrors = declNodes()
        .filter { it !is FunNode }
        .flatMap { it.typePass(emptyMap(), built, entryCallable) }
    return built.errors + signatureErrors + recursionErrors + funBodyErrors + otherErrors
}

fun ASTNode.typePass(
    symbolEnv: Map<String, Type> = emptyMap(),
    registry: ObjClassRegistry = ObjClassRegistry.EMPTY,
    funEnv: Map<String, FunNode> = emptyMap(),
): List<CompileError> = when (this) {
    is ProcClassNode -> typePassProcClass(symbolEnv, registry, funEnv)
    is VarNode -> typePassVarNode(symbolEnv, registry, funEnv)
    is ConstructorNode -> typePassConstructor(symbolEnv, registry, funEnv)
    is TransitionNode -> typePassTransition(symbolEnv, registry, funEnv)
    is ArgNode -> typePassArgNode(symbolEnv, registry, funEnv)
    is GuardNode -> typePassGuard(symbolEnv, registry, funEnv)
    is VarTransitNode -> typePassVarTransit(symbolEnv, registry, funEnv)
    is EffectNode -> typePassEffect(symbolEnv, registry, funEnv)
    is EffectCallNode -> typePassEffectCall(symbolEnv, registry, funEnv)
    is EffectAssignNode -> typePassEffectAssign(symbolEnv, registry, funEnv)
    is BinaryOpExprNode -> typePassBinaryOp(symbolEnv, registry, funEnv)
    is IfElseExprNode -> typePassIfElse(symbolEnv, registry, funEnv)
    is LetExprNode -> typePassLet(symbolEnv, registry, funEnv)
    is WhenExprNode -> typePassWhen(symbolEnv, registry, funEnv)
    is ObjClassLiteralExprNode -> typePassObjClassLiteral(symbolEnv, registry, funEnv)
    is FieldAccessExprNode -> typePassFieldAccess(symbolEnv, registry, funEnv)
    is FunCallExprNode -> typePassFunCall(symbolEnv, registry, funEnv)
    is SymbolValueExprNode -> typePassSymbol(symbolEnv, registry, funEnv)
    is ExprNode -> typePassExpr(symbolEnv, registry, funEnv)
    else -> children.flatMap { it.typePass(symbolEnv, registry, funEnv) }
}

private fun ProcClassNode.typePassProcClass(
    symbolEnv: Map<String, Type>,
    registry: ObjClassRegistry,
    funEnv: Map<String, FunNode>,
): List<CompileError> {
    val localDecls = localDecls()
    val varErrors = localDecls
        .filterIsInstance<VarNode>()
        .flatMap { it.typePass(symbolEnv, registry, funEnv) }
    val localSymbolEnv = localDecls
        .filterIsInstance<VarNode>()
        .associate { it.name to it.type }
    return varErrors + localDecls.flatMap { decl ->
        if (decl is VarNode) emptyList() else decl.typePass(localSymbolEnv, registry, funEnv)
    }
}

private fun VarNode.typePassVarNode(symbolEnv: Map<String, Type>, registry: ObjClassRegistry, funEnv: Map<String, FunNode>): List<CompileError> =
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
    funEnv: Map<String, FunNode>,
): List<CompileError> {
    val argErrors = constructorArgs().typePass(symbolEnv, registry, funEnv)
    val actionEnv = symbolEnv + constructorArgs().argsTypeMap() + constructorArgs().actionArgs().associate { it.name to it.type }
    return argErrors + body().flatMap { it.typePass(actionEnv, registry, funEnv) }
}

private fun TransitionNode.typePassTransition(
    symbolEnv: Map<String, Type>,
    registry: ObjClassRegistry,
    funEnv: Map<String, FunNode>,
): List<CompileError> {
    val argErrors = transitionArgs().typePass(symbolEnv, registry, funEnv)
    val actionEnv = symbolEnv + transitionArgs().argsTypeMap() + transitionArgs().actionArgs().associate { it.name to it.type }
    return argErrors + body().flatMap { it.typePass(actionEnv, registry, funEnv) }
}

private fun ArgNode.typePassArgNode(symbolEnv: Map<String, Type>, registry: ObjClassRegistry, funEnv: Map<String, FunNode>): List<CompileError> =
    when (val result = registry.resolveTypeName(argTypeName())) {
        is TypeResolveResult.Found -> {
            resolveArgType(result.type)
            emptyList()
        }
        is TypeResolveResult.NotFound ->
            listOf(OneLocCompileError(programLocation(), "Unknown type \"${argTypeName()}\" for action argument \"${argName()}\""))
    }

private fun GuardNode.typePassGuard(symbolEnv: Map<String, Type>, registry: ObjClassRegistry, funEnv: Map<String, FunNode>): List<CompileError> {
    val childrenErrors = children.flatMap { it.typePass(symbolEnv, registry, funEnv) }
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
    funEnv: Map<String, FunNode>,
): List<CompileError> {
    val childrenErrors = children.flatMap { it.typePass(symbolEnv, registry, funEnv) }
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

private fun EffectNode.typePassEffect(symbolEnv: Map<String, Type>, registry: ObjClassRegistry, funEnv: Map<String, FunNode>): List<CompileError> =
    children.flatMap { it.typePass(symbolEnv, registry, funEnv) }

private fun EffectCallNode.typePassEffectCall(
    symbolEnv: Map<String, Type>,
    registry: ObjClassRegistry,
    funEnv: Map<String, FunNode>,
): List<CompileError> {
    val childrenErrors = children.flatMap { it.typePass(symbolEnv, registry, funEnv) }
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
    funEnv: Map<String, FunNode>,
): List<CompileError> {
    val childrenErrors = children.flatMap { it.typePass(symbolEnv, registry, funEnv) }
    if (childrenErrors.isNotEmpty()) {
        return childrenErrors
    }
    val callErrors = EffectCallNode(callName(), callArgs(), programLocation()).typePassEffectCall(symbolEnv, registry, funEnv)
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
    funEnv: Map<String, FunNode>,
): List<CompileError> {
    val childErrors = children.flatMap { it.typePass(symbolEnv, registry, funEnv) }
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

private fun IfElseExprNode.typePassIfElse(symbolEnv: Map<String, Type>, registry: ObjClassRegistry, funEnv: Map<String, FunNode>): List<CompileError> {
    val childErrors = children.flatMap { it.typePass(symbolEnv, registry, funEnv) }
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

private fun LetExprNode.typePassLet(symbolEnv: Map<String, Type>, registry: ObjClassRegistry, funEnv: Map<String, FunNode>): List<CompileError> {
    val typeErrors = when (val result = registry.resolveTypeName(letTypeName())) {
        is TypeResolveResult.Found -> {
            resolveLetType(result.type)
            emptyList()
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

    val initErrors = letInitExpr().typePass(symbolEnv, registry, funEnv)
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
    val bodyErrors = bodyExpr().typePass(bodyEnv, registry, funEnv)
    inferExprType(bodyEnv)
    return bodyErrors
}

private fun WhenExprNode.typePassWhen(symbolEnv: Map<String, Type>, registry: ObjClassRegistry, funEnv: Map<String, FunNode>): List<CompileError> {
    val subjectErrors = subjectExpr()?.typePass(symbolEnv, registry, funEnv) ?: emptyList()
    if (subjectErrors.isNotEmpty()) {
        return subjectErrors
    }

    val structureErrors = whenStructureErrors()
    if (structureErrors.isNotEmpty()) {
        return structureErrors
    }

    val armErrors = arms().flatMap { arm ->
        when (arm) {
            is WhenArm.Subject -> {
                val patternErrors = when (val pattern = arm.pattern) {
                    is WhenPattern.Primitive -> emptyList()
                    is WhenPattern.Struct -> pattern.literal.typePass(symbolEnv, registry, funEnv)
                }
                patternErrors + arm.expr.typePass(symbolEnv, registry, funEnv)
            }
            is WhenArm.Guard -> arm.cond.typePass(symbolEnv, registry, funEnv) + arm.expr.typePass(symbolEnv, registry, funEnv)
            is WhenArm.Else -> arm.expr.typePass(symbolEnv, registry, funEnv)
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

    arms().filterIsInstance<WhenArm.Guard>().forEach { arm ->
        if (arm.cond.getType() !is BoolType) {
            errors.add(
                OneLocCompileError(arm.cond.programLocation(), "Expected when guard condition to be Boolean"),
            )
        }
    }

    subjectExpr()?.let { subject ->
        val subjectType = subject.getType()
        val subjectArms = arms().filterIsInstance<WhenArm.Subject>()
        subjectArms.forEach { arm ->
            when (val pattern = arm.pattern) {
                is WhenPattern.Primitive -> {
                    val literalType = pattern.literal.whenLiteralType()
                    if (literalType != subjectType) {
                        errors.add(
                            OneLocCompileError(
                                arm.expr.programLocation(),
                                "Expected when arm literal to match subject type $subjectType but got $literalType",
                            ),
                        )
                    }
                }
                is WhenPattern.Struct -> {
                    if (subjectType !is ObjClassType) {
                        errors.add(
                            OneLocCompileError(
                                pattern.literal.programLocation(),
                                "Expected when subject to be an o-class type for struct pattern but got $subjectType",
                            ),
                        )
                    } else if (pattern.literal.structType != subjectType) {
                        errors.add(
                            OneLocCompileError(
                                pattern.literal.programLocation(),
                                "Expected when arm struct pattern to match subject type $subjectType but got ${pattern.literal.structType}",
                            ),
                        )
                    }
                }
            }
        }
        val duplicatePatterns = subjectArms
            .groupBy { it.pattern.duplicateKey() }
            .filter { it.value.size > 1 }
        duplicatePatterns.forEach { (_, entries) ->
            errors.add(
                TwoLocsCompileError(
                    entries[0].expr.programLocation(),
                    entries[1].expr.programLocation(),
                    "Expected when subject arms to have unique patterns, but found duplicate ${entries[0].pattern}",
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

private fun WhenPattern.duplicateKey(): Any = when (this) {
    is WhenPattern.Primitive -> literal
    is WhenPattern.Struct ->
        literal.className to literal.fieldEntries.map { (name, expr) -> name to expr.toString() }
}

private fun ObjClassLiteralExprNode.typePassObjClassLiteral(
    symbolEnv: Map<String, Type>,
    registry: ObjClassRegistry,
    funEnv: Map<String, FunNode>,
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

    val childErrors = children.flatMap { it.typePass(symbolEnv, registry, funEnv) }
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
    funEnv: Map<String, FunNode>,
): List<CompileError> {
    val baseType = symbolEnv[baseSymbol]
    if (baseType == null) {
        return listOf(OneLocCompileError(programLocation(), "Unknown variable \"$baseSymbol\" in field access"))
    }
    return when (val result = resolveFieldPath(baseType, fieldPath)) {
        is FieldPathResult.Error -> listOf(OneLocCompileError(programLocation(), result.message))
        is FieldPathResult.Resolved -> {
            resolveFieldAccess(result.type, result.relPath)
            val childErrors = children.flatMap { it.typePass(symbolEnv, registry, funEnv) }
            inferExprType(symbolEnv)
            childErrors
        }
    }
}

private fun ExprNode.typePassExpr(symbolEnv: Map<String, Type>, registry: ObjClassRegistry, funEnv: Map<String, FunNode>): List<CompileError> {
    val childrenErrors = children.flatMap { it.typePass(symbolEnv, registry, funEnv) }
    inferExprType(symbolEnv)
    return childrenErrors
}

private fun SymbolValueExprNode.typePassSymbol(
    symbolEnv: Map<String, Type>,
    registry: ObjClassRegistry,
    funEnv: Map<String, FunNode>,
): List<CompileError> {
    if (symbol !in symbolEnv) {
        return listOf(OneLocCompileError(programLocation(), "Unknown variable \"$symbol\""))
    }
    inferExprType(symbolEnv)
    return emptyList()
}

private fun FunCallExprNode.typePassFunCall(
    symbolEnv: Map<String, Type>,
    registry: ObjClassRegistry,
    funEnv: Map<String, FunNode>,
): List<CompileError> {
    val argErrors = callArgs().flatMap { it.typePass(symbolEnv, registry, funEnv) }
    if (argErrors.isNotEmpty()) {
        return argErrors
    }
    val funNode = funEnv[callName()]
        ?: return listOf(OneLocCompileError(programLocation(), "Unknown function \"${callName()}\""))
    resolveFun(funNode)
    val params = funNode.funArgs().actionArgs()
    if (params.size != callArgs().size) {
        return listOf(
            OneLocCompileError(
                programLocation(),
                "Expected function \"${callName()}\" to take ${params.size} argument(s) but got ${callArgs().size}",
            ),
        )
    }
    val typeMismatchErrors = callArgs().zip(params).flatMap { (arg, param) ->
        assertOrCompileError(
            arg.getType() == param.type,
            OneLocCompileError(
                arg.programLocation(),
                "Expected argument \"${param.name}\" of \"${callName()}\" to have type ${param.type} but got ${arg.getType()}",
            ),
        )
    }
    if (typeMismatchErrors.isEmpty()) {
        try {
            inferExprType(symbolEnv)
        } catch (_: RuntimeException) {
            return listOf(
                OneLocCompileError(
                    programLocation(),
                    "Return type not resolved for function \"${callName()}\"",
                ),
            )
        }
    }
    return typeMismatchErrors
}

private fun FunNode.typePassFunSignature(registry: ObjClassRegistry): List<CompileError> {
    val argErrors = funArgs().typePass(emptyMap(), registry, emptyMap())
    val returnErrors = when (val result = registry.resolveTypeName(funReturnTypeName())) {
        is TypeResolveResult.Found -> {
            resolveReturnType(result.type)
            emptyList()
        }
        is TypeResolveResult.NotFound ->
            listOf(
                OneLocCompileError(
                    programLocation(),
                    "Unknown return type \"${funReturnTypeName()}\" for function \"${name()}\"",
                ),
            )
    }
    return argErrors + returnErrors
}

private fun FunNode.typePassFunBody(
    funEnv: Map<String, FunNode>,
    registry: ObjClassRegistry,
): List<CompileError> {
    val params = try {
        funArgs().actionArgs()
    } catch (_: RuntimeException) {
        return emptyList()
    }
    val returnType = try {
        returnType
    } catch (_: RuntimeException) {
        return emptyList()
    }
    val paramEnv = params.associate { it.name to it.type }
    val bodyErrors = funBody().typePass(paramEnv, registry, funEnv)
    if (bodyErrors.isNotEmpty()) {
        return bodyErrors
    }
    return assertOrCompileError(
        funBody().getType() == returnType,
        OneLocCompileError(
            funBody().programLocation(),
            "Expected function \"${name()}\" to return $returnType but body has type ${funBody().getType()}",
        ),
    )
}

private fun funRecursionErrors(allFuns: Map<String, FunNode>): List<CompileError> {
    val callGraph = allFuns.mapValues { (_, funNode) ->
        collectFunCallNames(funNode.funBody()).filter { it in allFuns }.toSet()
    }
    val errors = mutableListOf<CompileError>()
    val visiting = mutableSetOf<String>()
    val visited = mutableSetOf<String>()

    fun dfs(name: String, path: List<String>) {
        if (name in visited) return
        if (name in visiting) {
            val cycleStart = path.indexOf(name)
            val cycle = (path.drop(cycleStart) + name).joinToString(" -> ")
            val funNode = allFuns.getValue(name)
            errors.add(
                OneLocCompileError(
                    funNode.programLocation(),
                    "Recursive function calls are not allowed: $cycle",
                ),
            )
            return
        }
        visiting.add(name)
        callGraph[name].orEmpty().forEach { callee ->
            dfs(callee, path + name)
        }
        visiting.remove(name)
        visited.add(name)
    }

    allFuns.keys.forEach { dfs(it, emptyList()) }
    return errors.distinctBy { it.toString() }
}

private fun ExprNode.inferExprType(symbolEnv: Map<String, Type>) {
    setInferredType(TypePassType.Inferred(inferType(symbolEnv)))
}
