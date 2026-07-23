package julay.compiler.pass

import julay.compiler.*
import julay.compiler.ast.*
import julay.compiler.decl.*
import julay.program.*
import julay.program.type.*
import julay.program.action.*

sealed interface TypePassType {
    data object Uninferred : TypePassType
    data class Inferred(val type: Type) : TypePassType
}

data class TypePassResult(
    val errors: List<CompileError>,
    val warnings: List<CompileWarning> = emptyList(),
)

fun RootNode.typePass(
    unit: CompilationUnit,
    allowUnindexedSpec: Boolean = false,
): TypePassResult {
    val sortResult = unit.collectSorts()
    if (sortResult.errors.isNotEmpty()) {
        return TypePassResult(sortResult.errors)
    }
    val allRawObjClasses = unit.modules.flatMap { module ->
        module.root.declNodes().flatMap { it.objClassPass() }
    }
    val built = ObjClassRegistry.build(allRawObjClasses, sortResult.sorts)
    cacheObjClassRegistry(built)
    unit.modules.forEach { it.root.cacheObjClassRegistry(built) }
    if (built.errors.isNotEmpty()) {
        return TypePassResult(built.errors)
    }
    val allFuns = unit.modules
        .flatMap { it.root.declNodes().filterIsInstance<FunNode>() }
        .associateBy { it.name() }
    val signatureErrors = allFuns.values.flatMap { it.typePassFunSignature(built) }
    val recursionErrors = funRecursionErrors(allFuns)
    val funBodyErrors = unit.modules.flatMap { module ->
        val callable = callableFuns(module)
        val builtins = callableFunBuiltins(module)
        module.root.declNodes().filterIsInstance<FunNode>().flatMap { funNode ->
            funNode.typePassFunBody(callable, built, builtins)
        }
    }
    // Typecheck each module with that module's imports so julay.funlib.* (and imported
    // user funs) resolve in dependency modules, not only in the entry file.
    val otherErrors = unit.modules.flatMap { module ->
        val callable = callableFuns(module)
        val builtins = callableFunBuiltins(module)
        module.root.declNodes()
            .filter { it !is FunNode && it !is SpecNode && it !is InvariantNode }
            .flatMap { it.typePass(emptyMap(), built, callable, emptyMap(), builtins) }
    }
    val specResult = unit.root.specTypePass(unit, allowUnindexedSpec)
    return TypePassResult(
        errors = built.errors + signatureErrors + recursionErrors + funBodyErrors + otherErrors + specResult.errors,
        warnings = specResult.warnings,
    )
}

fun ASTNode.typePass(
    symbolEnv: Map<String, Type> = emptyMap(),
    registry: ObjClassRegistry = ObjClassRegistry.EMPTY,
    funEnv: Map<String, FunNode> = emptyMap(),
    typeParamEnv: Map<String, Type> = emptyMap(),
    funBuiltinEnv: Map<String, FunBuiltin> = emptyMap(),
): List<CompileError> = when (this) {
    is ProcClassNode -> typePassProcClass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
    is VarNode -> typePassVarNode(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
    is ConstructorNode -> typePassConstructor(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
    is TransitionNode -> typePassTransition(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
    is ArgNode -> typePassArgNode(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
    is GuardNode -> typePassGuard(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
    is VarTransitNode -> typePassVarTransit(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
    is MapIndexTransitNode -> typePassMapIndexTransit(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
    is ErrorNode -> typePassError(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
    is BeforeNode -> typePassBefore(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
    is AfterNode -> typePassAfter(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
    is CallStmtNode -> typePassCallStmt(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
    is UnaryOpExprNode -> typePassUnaryOp(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
    is BinaryOpExprNode -> typePassBinaryOp(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
    is IfElseExprNode -> typePassIfElse(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
    is LetExprNode -> typePassLet(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
    is WhenExprNode -> typePassWhen(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
    is ObjClassLiteralExprNode -> typePassObjClassLiteral(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
    is FieldAccessExprNode -> typePassFieldAccess(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
    is MemberAccessExprNode -> typePassMemberAccess(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
    is ListLiteralExprNode -> typePassListLiteral(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
    is EmptyBracketLiteralExprNode -> typePassEmptyBracketLiteral(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
    is SetLiteralExprNode -> typePassSetLiteral(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
    is MapLiteralExprNode -> typePassMapLiteral(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
    is IndexExprNode -> typePassIndex(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
    is SliceExprNode -> typePassSlice(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
    is FunCallExprNode -> typePassFunCall(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
    is SymbolValueExprNode -> typePassSymbol(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
    is ExprNode -> typePassExpr(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
    else -> children.flatMap { it.typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv) }
}

private fun ProcClassNode.typePassProcClass(
    symbolEnv: Map<String, Type>,
    registry: ObjClassRegistry,
    funEnv: Map<String, FunNode>,
    typeParamEnv: Map<String, Type>,
    funBuiltinEnv: Map<String, FunBuiltin>,
): List<CompileError> {
    val localDecls = localDecls()
    val varErrors = localDecls
        .filterIsInstance<VarNode>()
        .flatMap { it.typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv) }
    if (varErrors.isNotEmpty()) {
        return varErrors
    }
    val localSymbolEnv = localDecls
        .filterIsInstance<VarNode>()
        .associate { it.name to it.type }
    return varErrors + localDecls.flatMap { decl ->
        if (decl is VarNode) emptyList() else decl.typePass(localSymbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
    }
}

private fun VarNode.typePassVarNode(symbolEnv: Map<String, Type>, registry: ObjClassRegistry, funEnv: Map<String, FunNode>, typeParamEnv: Map<String, Type>,
    funBuiltinEnv: Map<String, FunBuiltin>): List<CompileError> =
    when (val result = registry.resolveTypeExpr(typeExpr, typeParamEnv, programLocation())) {
        is TypeResolveResult.Found -> {
            resolveType(result.type)
            listOfNotNull(sortDomainOnlyError(result.type, programLocation()))
        }
        is TypeResolveResult.Error ->
            listOf(OneLocCompileError(programLocation(), "${result.message} for state variable \"$name\""))
    }

private fun ConstructorNode.typePassConstructor(
    symbolEnv: Map<String, Type>,
    registry: ObjClassRegistry,
    funEnv: Map<String, FunNode>,
    typeParamEnv: Map<String, Type>,
    funBuiltinEnv: Map<String, FunBuiltin>,
): List<CompileError> {
    val argErrors = constructorArgs().typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
    val actionEnv = symbolEnv + constructorArgs().argsTypeMap() + constructorArgs().actionArgs().associate { it.name to it.type }
    return argErrors + body().flatMap { it.typePass(actionEnv, registry, funEnv, typeParamEnv, funBuiltinEnv) }
}

private fun TransitionNode.typePassTransition(
    symbolEnv: Map<String, Type>,
    registry: ObjClassRegistry,
    funEnv: Map<String, FunNode>,
    typeParamEnv: Map<String, Type>,
    funBuiltinEnv: Map<String, FunBuiltin>,
): List<CompileError> {
    val argErrors = transitionArgs().typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
    val actionEnv = symbolEnv + transitionArgs().argsTypeMap() + transitionArgs().actionArgs().associate { it.name to it.type }
    return argErrors + body().flatMap { it.typePass(actionEnv, registry, funEnv, typeParamEnv, funBuiltinEnv) }
}

private fun ArgNode.typePassArgNode(symbolEnv: Map<String, Type>, registry: ObjClassRegistry, funEnv: Map<String, FunNode>, typeParamEnv: Map<String, Type>,
    funBuiltinEnv: Map<String, FunBuiltin>): List<CompileError> =
    when (val result = registry.resolveTypeExpr(argTypeExpr(), typeParamEnv, programLocation())) {
        is TypeResolveResult.Found -> {
            resolveArgType(result.type)
            listOfNotNull(sortDomainOnlyError(result.type, programLocation()))
        }
        is TypeResolveResult.Error ->
            listOf(OneLocCompileError(programLocation(), "Unknown type \"${argTypeName()}\" for action argument \"${argName()}\""))
    }

private fun GuardNode.typePassGuard(symbolEnv: Map<String, Type>, registry: ObjClassRegistry, funEnv: Map<String, FunNode>, typeParamEnv: Map<String, Type>,
    funBuiltinEnv: Map<String, FunBuiltin>): List<CompileError> {
    val childrenErrors = children.flatMap { it.typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv) }
    if (childrenErrors.isNotEmpty()) {
        return childrenErrors
    }
    val guardTypeErrors = assertOrCompileError(
        guardExpr().getType() is BoolType,
        OneLocCompileError(programLocation(), "Expected guards to be Boolean-valued expressions"),
    )
    return childrenErrors + guardTypeErrors
}

private fun ErrorNode.typePassError(
    symbolEnv: Map<String, Type>,
    registry: ObjClassRegistry,
    funEnv: Map<String, FunNode>,
    typeParamEnv: Map<String, Type>,
    funBuiltinEnv: Map<String, FunBuiltin>,
): List<CompileError> {
    val childrenErrors = children.flatMap { it.typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv) }
    if (childrenErrors.isNotEmpty()) {
        return childrenErrors
    }
    val condTypeErrors = errors().flatMap { arm ->
        assertOrCompileError(
            arm.condExpr().getType() is BoolType,
            OneLocCompileError(arm.programLocation(), "Expected error conditions to be Boolean-valued expressions"),
        )
    }
    return childrenErrors + condTypeErrors
}

private fun VarTransitNode.typePassVarTransit(
    symbolEnv: Map<String, Type>,
    registry: ObjClassRegistry,
    funEnv: Map<String, FunNode>,
    typeParamEnv: Map<String, Type>,
    funBuiltinEnv: Map<String, FunBuiltin>,
): List<CompileError> {
    val varType = symbolEnv[varName]
    // Undeclared targets take priority over RHS errors (e.g. untyped empty list literals).
    if (varType == null) {
        return listOf(
            OneLocCompileError(programLocation(), "Unknown variable \"$varName\" in transit assignment"),
        )
    }
    val expectedType: Type? = when {
        fieldPath.isEmpty() -> varType
        else -> when (val result = resolveFieldPath(varType, fieldPath)) {
            is FieldPathResult.Resolved -> result.type
            is FieldPathResult.Error -> null
        }
    }
    expectedType?.let { applyExpectedCollectionType(transitExpr(), it) }
    val childrenErrors = children.flatMap { it.typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv) }
    if (childrenErrors.isNotEmpty()) {
        return childrenErrors
    }
    val varErrors = if (fieldPath.isEmpty()) {
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
                            "Cannot assign a scalar to obj field \"${transitKey()}\"; assign the whole value instead",
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

private fun BeforeNode.typePassBefore(
    symbolEnv: Map<String, Type>,
    registry: ObjClassRegistry,
    funEnv: Map<String, FunNode>,
    typeParamEnv: Map<String, Type>,
    funBuiltinEnv: Map<String, FunBuiltin>,
): List<CompileError> =
    befores().flatMap { it.typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv) }

private fun AfterNode.typePassAfter(
    symbolEnv: Map<String, Type>,
    registry: ObjClassRegistry,
    funEnv: Map<String, FunNode>,
    typeParamEnv: Map<String, Type>,
    funBuiltinEnv: Map<String, FunBuiltin>,
): List<CompileError> =
    afters().flatMap { it.typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv) }

private fun CallStmtNode.typePassCallStmt(
    symbolEnv: Map<String, Type>,
    registry: ObjClassRegistry,
    funEnv: Map<String, FunNode>,
    typeParamEnv: Map<String, Type>,
    funBuiltinEnv: Map<String, FunBuiltin>,
): List<CompileError> {
    funBuiltinEnv[callName()]?.let { builtin ->
        resolveBuiltin(builtin)
        if (callTypeArgs().isNotEmpty()) {
            return listOf(
                OneLocCompileError(
                    programLocation(),
                    "Expected function \"${callName()}\" not to take type arguments",
                ),
            )
        }
        if (builtin.sessionPeerClassArg) {
            if (builtin.arity != callArgs().size) {
                return listOf(
                    OneLocCompileError(
                        programLocation(),
                        "Expected function \"${callName()}\" to take ${builtin.arity} argument(s) but got ${callArgs().size}",
                    ),
                )
            }
            val arg = callArgs().single()
            return assertOrCompileError(
                arg is SymbolValueExprNode,
                OneLocCompileError(
                    arg.programLocation(),
                    "Expected \"${callName()}\" argument to be a leaf proc class name",
                ),
            )
        }
        val childrenErrors = callArgs().flatMap { it.typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv) }
        if (childrenErrors.isNotEmpty()) {
            return childrenErrors
        }
        val argTypes = callArgs().map { it.getType() }
        builtin.checkArgs(argTypes)?.let { msg ->
            return listOf(OneLocCompileError(programLocation(), msg))
        }
        return emptyList()
    }
    // User fun: reuse FunCallExprNode typing, then copy resolution onto this stmt.
    val asExpr = FunCallExprNode(callName(), callArgs(), programLocation(), typeArgs = callTypeArgs())
    val errors = asExpr.typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
    if (errors.isNotEmpty()) {
        return errors
    }
    asExpr.resolvedBuiltinOrNull()?.let { resolveBuiltin(it) }
    asExpr.resolvedFunOrNull()?.let { resolveFun(it) }
    return emptyList()
}

private fun UnaryOpExprNode.typePassUnaryOp(
    symbolEnv: Map<String, Type>,
    registry: ObjClassRegistry,
    funEnv: Map<String, FunNode>,
    typeParamEnv: Map<String, Type>,
    funBuiltinEnv: Map<String, FunBuiltin>,
): List<CompileError> {
    val childErrors = children.flatMap { it.typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv) }
    if (childErrors.isNotEmpty()) {
        return childErrors
    }
    try {
        inferExprType(symbolEnv)
    } catch (e: RuntimeException) {
        return childErrors + listOf(OneLocCompileError(programLocation(), e.message ?: "Type error"))
    }
    return childErrors
}

private fun BinaryOpExprNode.typePassBinaryOp(
    symbolEnv: Map<String, Type>,
    registry: ObjClassRegistry,
    funEnv: Map<String, FunNode>,
    typeParamEnv: Map<String, Type>,
    funBuiltinEnv: Map<String, FunBuiltin>,
): List<CompileError> {
    val childErrors = children.flatMap { it.typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv) }
    if (childErrors.isNotEmpty()) {
        return childErrors
    }
    val lhsType = lhsOperand().getType()
    val rhsType = rhsOperand().getType()
    // If either side of "+" is String, coerce the other via toString (string concat).
    val structOpErrors = if (op() == "+" && (lhsType is StringType || rhsType is StringType)) {
        emptyList()
    } else if (lhsType is ObjClassType || rhsType is ObjClassType) {
        when (op()) {
            "=", "#" -> assertOrCompileError(
                lhsType == rhsType,
                OneLocCompileError(
                    programLocation(),
                    "Expected both sides of \"${op()}\" to have the same obj type, got $lhsType and $rhsType",
                ),
            )
            else -> listOf(OneLocCompileError(programLocation(), "Cannot apply \"${op()}\" to obj type $lhsType"))
        }
    } else if (lhsType is ListType || rhsType is ListType) {
        when (op()) {
            "=", "#" -> assertOrCompileError(
                lhsType == rhsType,
                OneLocCompileError(
                    programLocation(),
                    "Expected both sides of \"${op()}\" to have the same list type, got $lhsType and $rhsType",
                ),
            )
            "+" -> assertOrCompileError(
                lhsType is ListType && rhsType is ListType && lhsType == rhsType,
                OneLocCompileError(
                    programLocation(),
                    "Expected both sides of \"+\" to have the same list type, got $lhsType and $rhsType",
                ),
            )
            "in" -> assertOrCompileError(
                rhsType is ListType && lhsType == (rhsType as ListType).elementType,
                OneLocCompileError(
                    programLocation(),
                    "Expected \"in\" list element type $lhsType to match ${(rhsType as ListType).elementType}",
                ),
            )
            else -> listOf(OneLocCompileError(programLocation(), "Cannot apply \"${op()}\" to list type $lhsType"))
        }
    } else if (lhsType is SetType || rhsType is SetType) {
        when (op()) {
            "=", "#" -> assertOrCompileError(
                lhsType == rhsType,
                OneLocCompileError(
                    programLocation(),
                    "Expected both sides of \"${op()}\" to have the same set type, got $lhsType and $rhsType",
                ),
            )
            "+", "-" -> assertOrCompileError(
                lhsType is SetType && rhsType is SetType && lhsType == rhsType,
                OneLocCompileError(
                    programLocation(),
                    "Expected both sides of \"${op()}\" to have the same set type, got $lhsType and $rhsType",
                ),
            )
            "in" -> assertOrCompileError(
                rhsType is SetType && lhsType == (rhsType as SetType).elementType,
                OneLocCompileError(
                    programLocation(),
                    "Expected \"in\" set element type $lhsType to match ${(rhsType as SetType).elementType}",
                ),
            )
            else -> listOf(OneLocCompileError(programLocation(), "Cannot apply \"${op()}\" to set type $lhsType"))
        }
    } else if (lhsType is MapType || rhsType is MapType) {
        when (op()) {
            "=", "#" -> assertOrCompileError(
                lhsType == rhsType,
                OneLocCompileError(
                    programLocation(),
                    "Expected both sides of \"${op()}\" to have the same map type, got $lhsType and $rhsType",
                ),
            )
            "in" -> assertOrCompileError(
                rhsType is MapType && lhsType == (rhsType as MapType).keyType,
                OneLocCompileError(
                    programLocation(),
                    "Expected \"in\" map key type $lhsType to match ${(rhsType as MapType).keyType}",
                ),
            )
            else -> listOf(OneLocCompileError(programLocation(), "Cannot apply \"${op()}\" to map type $lhsType"))
        }
    } else if (op() == "in") {
        listOf(OneLocCompileError(programLocation(), "Cannot apply \"in\" to types $lhsType and $rhsType"))
    } else {
        emptyList()
    }
    if (structOpErrors.isNotEmpty()) {
        return childErrors + structOpErrors
    }
    try {
        inferExprType(symbolEnv)
    } catch (e: RuntimeException) {
        return childErrors + listOf(OneLocCompileError(programLocation(), e.message ?: "Type error"))
    }
    return childErrors
}

private fun IfElseExprNode.typePassIfElse(symbolEnv: Map<String, Type>, registry: ObjClassRegistry, funEnv: Map<String, FunNode>, typeParamEnv: Map<String, Type>,
    funBuiltinEnv: Map<String, FunBuiltin>): List<CompileError> {
    val childErrors = children.flatMap { it.typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv) }
    if (childErrors.isNotEmpty()) {
        return childErrors
    }
    inferExprType(symbolEnv)
    return ifElseTypeErrors()
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

private fun LetExprNode.typePassLet(symbolEnv: Map<String, Type>, registry: ObjClassRegistry, funEnv: Map<String, FunNode>, typeParamEnv: Map<String, Type>,
    funBuiltinEnv: Map<String, FunBuiltin>): List<CompileError> {
    val typeErrors = when (val result = registry.resolveTypeExpr(letTypeExpr(), typeParamEnv, programLocation())) {
        is TypeResolveResult.Found -> {
            resolveLetType(result.type)
            listOfNotNull(sortDomainOnlyError(result.type, programLocation()))
        }
        is TypeResolveResult.Error ->
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

    val declaredType = resolvedLetType
    applyExpectedCollectionType(letInitExpr(), declaredType)
    val initErrors = letInitExpr().typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
    if (initErrors.isNotEmpty()) {
        return initErrors
    }

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
    val bodyErrors = bodyExpr().typePass(bodyEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
    inferExprType(bodyEnv)
    return bodyErrors
}

private fun WhenExprNode.typePassWhen(symbolEnv: Map<String, Type>, registry: ObjClassRegistry, funEnv: Map<String, FunNode>, typeParamEnv: Map<String, Type>,
    funBuiltinEnv: Map<String, FunBuiltin>): List<CompileError> {
    val subjectErrors = subjectExpr()?.typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv) ?: emptyList()
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
                    is WhenPattern.Struct -> pattern.literal.typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
                }
                patternErrors + arm.expr.typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
            }
            is WhenArm.Guard -> arm.cond.typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv) + arm.expr.typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
            is WhenArm.Else -> arm.expr.typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
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
                                "Expected when subject to be an obj type for struct pattern but got $subjectType",
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
    is WhenLiteral.RealLit -> realType
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
    typeParamEnv: Map<String, Type>,
    funBuiltinEnv: Map<String, FunBuiltin>,
): List<CompileError> {
    val classErrors = when (val classResult = registry.resolveTypeExpr(typeExpr, typeParamEnv, programLocation())) {
        is TypeResolveResult.Error ->
            listOf(OneLocCompileError(programLocation(), "Unknown obj \"$typeExpr\" in obj literal"))
        is TypeResolveResult.Found -> {
            if (classResult.type !is ObjClassType) {
                listOf(OneLocCompileError(programLocation(), "\"$typeExpr\" is not an obj type"))
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
                    "Expected obj literal fields to have unique names, but found duplicate \"$name\"",
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

    val childErrors = children.flatMap { it.typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv) }
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
    typeParamEnv: Map<String, Type>,
    funBuiltinEnv: Map<String, FunBuiltin>,
): List<CompileError> {
    val baseType = symbolEnv[baseSymbol]
    if (baseType == null) {
        return listOf(OneLocCompileError(programLocation(), "Unknown variable \"$baseSymbol\" in field access"))
    }
    return when (val result = resolveFieldPath(baseType, fieldPath)) {
        is FieldPathResult.Error -> listOf(OneLocCompileError(programLocation(), result.message))
        is FieldPathResult.Resolved -> {
            resolveFieldAccess(result.type, result.relPath)
            val childErrors = children.flatMap { it.typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv) }
            inferExprType(symbolEnv)
            childErrors
        }
    }
}

private fun MemberAccessExprNode.typePassMemberAccess(
    symbolEnv: Map<String, Type>,
    registry: ObjClassRegistry,
    funEnv: Map<String, FunNode>,
    typeParamEnv: Map<String, Type>,
    funBuiltinEnv: Map<String, FunBuiltin>,
): List<CompileError> {
    val baseErrors = baseExpr.typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
    if (baseErrors.isNotEmpty()) {
        return baseErrors
    }
    val baseType = try {
        baseExpr.getType()
    } catch (_: RuntimeException) {
        return listOf(OneLocCompileError(programLocation(), "Cannot resolve type of member-access base"))
    }
    return when (val result = resolveFieldPath(baseType, listOf(fieldName))) {
        is FieldPathResult.Error -> listOf(OneLocCompileError(programLocation(), result.message))
        is FieldPathResult.Resolved -> {
            setInferredType(TypePassType.Inferred(result.type))
            emptyList()
        }
    }
}

private fun ExprNode.typePassExpr(symbolEnv: Map<String, Type>, registry: ObjClassRegistry, funEnv: Map<String, FunNode>, typeParamEnv: Map<String, Type>,
    funBuiltinEnv: Map<String, FunBuiltin>): List<CompileError> {
    val childrenErrors = children.flatMap { it.typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv) }
    inferExprType(symbolEnv)
    return childrenErrors
}

private fun SymbolValueExprNode.typePassSymbol(
    symbolEnv: Map<String, Type>,
    registry: ObjClassRegistry,
    funEnv: Map<String, FunNode>,
    typeParamEnv: Map<String, Type>,
    funBuiltinEnv: Map<String, FunBuiltin>,
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
    typeParamEnv: Map<String, Type>,
    funBuiltinEnv: Map<String, FunBuiltin>,
): List<CompileError> {
    val argErrors = callArgs().flatMap { it.typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv) }
    if (argErrors.isNotEmpty()) {
        return argErrors
    }
    funBuiltinEnv[callName()]?.let { builtin ->
        resolveBuiltin(builtin)
        if (callTypeArgs().isNotEmpty()) {
            return listOf(
                OneLocCompileError(
                    programLocation(),
                    "Expected function \"${callName()}\" not to take type arguments",
                ),
            )
        }
        val returnType = builtin.returnType
            ?: return listOf(
                OneLocCompileError(
                    programLocation(),
                    "Function \"${callName()}\" cannot be used in an expression because it returns no value",
                ),
            )
        if (builtin.sessionPeerClassArg) {
            return listOf(
                OneLocCompileError(
                    programLocation(),
                    "Function \"${callName()}\" cannot be used in an expression",
                ),
            )
        }
        val argTypes = callArgs().map { it.getType() }
        builtin.checkArgs(argTypes)?.let { msg ->
            return listOf(OneLocCompileError(programLocation(), msg))
        }
        resolveInstantiatedReturnType(returnType)
        inferExprType(symbolEnv)
        return emptyList()
    }
    val funNode = funEnv[callName()]
        ?: return listOf(OneLocCompileError(programLocation(), "Unknown function \"${callName()}\""))
    resolveFun(funNode)
    val params = try {
        funNode.funArgs().actionArgs()
    } catch (_: RuntimeException) {
        return listOf(
            OneLocCompileError(
                programLocation(),
                "Parameter types not resolved for function \"${callName()}\"",
            ),
        )
    }
    if (params.size != callArgs().size) {
        return listOf(
            OneLocCompileError(
                programLocation(),
                "Expected function \"${callName()}\" to take ${params.size} argument(s) but got ${callArgs().size}",
            ),
        )
    }

    if (funNode.typeParams.isNotEmpty()) {
        val subst = mutableMapOf<String, Type>()
        for ((arg, param) in callArgs().zip(params)) {
            when (val unify = unifyTypes(param.type, arg.getType(), subst)) {
                is UnifyResult.Fail ->
                    return listOf(
                        OneLocCompileError(
                            arg.programLocation(),
                            "Expected argument \"${param.name}\" of \"${callName()}\" to have type ${param.type} but got ${arg.getType()}: ${unify.message}",
                        ),
                    )
                is UnifyResult.Ok -> {}
            }
        }
        val unbound = funNode.typeParams.filter { it !in subst }
        if (unbound.isNotEmpty()) {
            return listOf(
                OneLocCompileError(
                    programLocation(),
                    "Cannot infer type parameter(s) ${unbound.joinToString(", ")} for function \"${callName()}\"",
                ),
            )
        }
        return when (val ret = registry.resolveTypeExpr(funNode.funReturnTypeExpr(), subst, programLocation())) {
            is TypeResolveResult.Found -> {
                resolveInstantiatedReturnType(ret.type)
                val paramsForSubst = funNode.funArgs().actionArgs()
                val valueInlined = paramsForSubst.zip(callArgs()).fold(funNode.funBody()) { acc, (param, arg) ->
                    substituteExpr(acc, param.name, arg)
                }
                // Re-type the inlined body under the concrete type substitution so o-class
                // literals like `Box T { ... }` become `Box_Int` rather than schema `Box_T`.
                val specializeErrors = valueInlined.typePass(symbolEnv, registry, funEnv, subst)
                if (specializeErrors.isNotEmpty()) {
                    return specializeErrors
                }
                resolveSpecializedBody(valueInlined)
                inferExprType(symbolEnv)
                emptyList()
            }
            is TypeResolveResult.Error ->
                listOf(OneLocCompileError(programLocation(), ret.message))
        }
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
    val dupParamErrors = typeParams
        .groupingBy { it }
        .eachCount()
        .filter { it.value > 1 }
        .keys
        .map { dup ->
            OneLocCompileError(programLocation(), "Duplicate type parameter \"$dup\" on function \"${name()}\"")
        }
    if (dupParamErrors.isNotEmpty()) {
        return dupParamErrors
    }

    val typeParamEnv: Map<String, Type> = typeParams.associateWith { TypeVar(it) }
    val argNodes = funArgs().children.filterIsInstance<ArgNode>().ifEmpty {
        // ArgsNode may nest ArgNodes
        fun collectArgs(node: ASTNode): List<ArgNode> = when (node) {
            is ArgNode -> listOf(node)
            else -> node.children.flatMap { collectArgs(it) }
        }
        collectArgs(funArgs())
    }

    val mentionedInParams = argNodes
        .flatMap { typeExprMentions(it.argTypeExpr(), typeParams.toSet()) }
        .toSet()
    val returnOnlyParams = typeParams.filter { it !in mentionedInParams }
    val returnOnlyErrors = returnOnlyParams.map { tp ->
        OneLocCompileError(
            programLocation(),
            "Type parameter \"$tp\" of function \"${name()}\" must appear in a value-parameter type",
        )
    }

    val argErrors = argNodes.flatMap { arg ->
        when (val result = registry.resolveTypeExpr(arg.argTypeExpr(), typeParamEnv, arg.programLocation())) {
            is TypeResolveResult.Found -> {
                arg.resolveArgType(result.type)
                listOfNotNull(sortDomainOnlyError(result.type, arg.programLocation()))
            }
            is TypeResolveResult.Error ->
                listOf(
                    OneLocCompileError(
                        arg.programLocation(),
                        "${result.message} for action argument \"${arg.argName()}\"",
                    ),
                )
        }
    }
    val returnErrors = when (val result = registry.resolveTypeExpr(funReturnTypeExpr(), typeParamEnv, programLocation())) {
        is TypeResolveResult.Found -> {
            resolveReturnType(result.type)
            listOfNotNull(sortDomainOnlyError(result.type, programLocation()))
        }
        is TypeResolveResult.Error ->
            listOf(
                OneLocCompileError(
                    programLocation(),
                    "${result.message} for return type of function \"${name()}\"",
                ),
            )
    }
    return returnOnlyErrors + argErrors + returnErrors
}

private fun applyExpectedCollectionType(expr: ExprNode, expected: Type) {
    when {
        expr is ListLiteralExprNode && expr.elements.isEmpty() && expected is ListType ->
            expr.resolveListType(expected)
        expr is EmptyBracketLiteralExprNode && expected is ListType ->
            expr.resolveListType(expected)
        expr is EmptyBracketLiteralExprNode && expected is MapType ->
            expr.resolveMapType(expected)
        expr is SetLiteralExprNode && expr.elements.isEmpty() && expected is SetType ->
            expr.resolveSetType(expected)
        expr is MapLiteralExprNode && expr.entries.isEmpty() && expected is MapType ->
            expr.resolveMapType(expected)
    }
}

private fun EmptyBracketLiteralExprNode.typePassEmptyBracketLiteral(
    symbolEnv: Map<String, Type>,
    registry: ObjClassRegistry,
    funEnv: Map<String, FunNode>,
    typeParamEnv: Map<String, Type>,
    funBuiltinEnv: Map<String, FunBuiltin>,
): List<CompileError> {
    resolvedListTypeOrNull()?.let {
        inferExprType(symbolEnv)
        return emptyList()
    }
    resolvedMapTypeOrNull()?.let {
        inferExprType(symbolEnv)
        return emptyList()
    }
    return listOf(
        OneLocCompileError(
            programLocation(),
            "Empty [] literal requires a known List or Map target type",
        ),
    )
}

private fun ListLiteralExprNode.typePassListLiteral(
    symbolEnv: Map<String, Type>,
    registry: ObjClassRegistry,
    funEnv: Map<String, FunNode>,
    typeParamEnv: Map<String, Type>,
    funBuiltinEnv: Map<String, FunBuiltin>,
): List<CompileError> {
    val childErrors = elements.flatMap { it.typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv) }
    if (childErrors.isNotEmpty()) {
        return childErrors
    }
    resolvedListTypeOrNull()?.let {
        inferExprType(symbolEnv)
        return emptyList()
    }
    if (elements.isEmpty()) {
        return listOf(
            OneLocCompileError(
                programLocation(),
                "Empty list literal requires a known List target type",
            ),
        )
    }
    val elemType = elements[0].getType()
    val mismatch = elements.drop(1).filter { it.getType() != elemType }
    if (mismatch.isNotEmpty()) {
        return mismatch.map {
            OneLocCompileError(
                it.programLocation(),
                "Expected list elements to have type $elemType but got ${it.getType()}",
            )
        }
    }
    resolveListType(listType(elemType))
    inferExprType(symbolEnv)
    return emptyList()
}

private fun IndexExprNode.typePassIndex(
    symbolEnv: Map<String, Type>,
    registry: ObjClassRegistry,
    funEnv: Map<String, FunNode>,
    typeParamEnv: Map<String, Type>,
    funBuiltinEnv: Map<String, FunBuiltin>,
): List<CompileError> {
    val childErrors = children.flatMap { it.typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv) }
    if (childErrors.isNotEmpty()) {
        return childErrors
    }
    val errors = mutableListOf<CompileError>()
    val baseType = base.getType()
    when (baseType) {
        is ListType -> {
            if (index.getType() !is IntType) {
                errors.add(OneLocCompileError(index.programLocation(), "Expected Int index but got ${index.getType()}"))
            }
        }
        is MapType -> {
            if (index.getType() != baseType.keyType) {
                errors.add(
                    OneLocCompileError(
                        index.programLocation(),
                        "Expected map key type ${baseType.keyType} but got ${index.getType()}",
                    ),
                )
            }
        }
        else -> errors.add(OneLocCompileError(base.programLocation(), "Expected list or map type for index base but got $baseType"))
    }
    if (errors.isEmpty()) {
        inferExprType(symbolEnv)
    }
    return errors
}

private fun MapIndexTransitNode.typePassMapIndexTransit(
    symbolEnv: Map<String, Type>,
    registry: ObjClassRegistry,
    funEnv: Map<String, FunNode>,
    typeParamEnv: Map<String, Type>,
    funBuiltinEnv: Map<String, FunBuiltin>,
): List<CompileError> {
    val mapVarType = symbolEnv[mapVar]
    if (mapVarType == null) {
        return listOf(OneLocCompileError(programLocation(), "Unknown variable \"$mapVar\" in transit assignment"))
    }
    if (mapVarType !is MapType) {
        return listOf(
            OneLocCompileError(
                programLocation(),
                "Expected map variable for index assignment but \"$mapVar\" has type $mapVarType",
            ),
        )
    }
    val childErrors = children.flatMap { it.typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv) }
    if (childErrors.isNotEmpty()) {
        return childErrors
    }
    val errors = mutableListOf<CompileError>()
    if (key.getType() != mapVarType.keyType) {
        errors.add(
            OneLocCompileError(
                key.programLocation(),
                "Expected map key type ${mapVarType.keyType} but got ${key.getType()}",
            ),
        )
    }
    if (value.getType() != mapVarType.valueType) {
        errors.add(
            OneLocCompileError(
                value.programLocation(),
                "Expected map value type ${mapVarType.valueType} but got ${value.getType()}",
            ),
        )
    }
    return errors
}

private fun SetLiteralExprNode.typePassSetLiteral(
    symbolEnv: Map<String, Type>,
    registry: ObjClassRegistry,
    funEnv: Map<String, FunNode>,
    typeParamEnv: Map<String, Type>,
    funBuiltinEnv: Map<String, FunBuiltin>,
): List<CompileError> {
    val childErrors = elements.flatMap { it.typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv) }
    if (childErrors.isNotEmpty()) {
        return childErrors
    }
    resolvedSetTypeOrNull()?.let {
        inferExprType(symbolEnv)
        return emptyList()
    }
    if (elements.isEmpty()) {
        return listOf(
            OneLocCompileError(
                programLocation(),
                "Empty set literal requires a known Set target type",
            ),
        )
    }
    val elemType = elements[0].getType()
    val mismatch = elements.drop(1).filter { it.getType() != elemType }
    if (mismatch.isNotEmpty()) {
        return mismatch.map {
            OneLocCompileError(
                it.programLocation(),
                "Expected set elements to have type $elemType but got ${it.getType()}",
            )
        }
    }
    resolveSetType(setType(elemType))
    inferExprType(symbolEnv)
    return emptyList()
}

private fun MapLiteralExprNode.typePassMapLiteral(
    symbolEnv: Map<String, Type>,
    registry: ObjClassRegistry,
    funEnv: Map<String, FunNode>,
    typeParamEnv: Map<String, Type>,
    funBuiltinEnv: Map<String, FunBuiltin>,
): List<CompileError> {
    val childErrors = entries.flatMap { (k, v) ->
        k.typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv) +
            v.typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
    }
    if (childErrors.isNotEmpty()) {
        return childErrors
    }
    resolvedMapTypeOrNull()?.let {
        inferExprType(symbolEnv)
        return emptyList()
    }
    if (entries.isEmpty()) {
        return listOf(
            OneLocCompileError(
                programLocation(),
                "Empty map literal requires a known Map target type",
            ),
        )
    }
    val keyType = entries[0].first.getType()
    val valueType = entries[0].second.getType()
    val keyMismatch = entries.drop(1).filter { it.first.getType() != keyType }
    if (keyMismatch.isNotEmpty()) {
        return keyMismatch.map {
            OneLocCompileError(
                it.first.programLocation(),
                "Expected map keys to have type $keyType but got ${it.first.getType()}",
            )
        }
    }
    val valueMismatch = entries.filter { it.second.getType() != valueType }
    if (valueMismatch.isNotEmpty()) {
        return valueMismatch.map {
            OneLocCompileError(
                it.second.programLocation(),
                "Expected map values to have type $valueType but got ${it.second.getType()}",
            )
        }
    }
    resolveMapType(mapType(keyType, valueType))
    inferExprType(symbolEnv)
    return emptyList()
}

private fun SliceExprNode.typePassSlice(
    symbolEnv: Map<String, Type>,
    registry: ObjClassRegistry,
    funEnv: Map<String, FunNode>,
    typeParamEnv: Map<String, Type>,
    funBuiltinEnv: Map<String, FunBuiltin>,
): List<CompileError> {
    val childErrors = children.flatMap { it.typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv) }
    if (childErrors.isNotEmpty()) {
        return childErrors
    }
    val errors = mutableListOf<CompileError>()
    val baseType = base.getType()
    if (baseType !is ListType) {
        errors.add(OneLocCompileError(base.programLocation(), "Expected list type for slice base but got $baseType"))
    }
    if (start.getType() !is IntType) {
        errors.add(OneLocCompileError(start.programLocation(), "Expected Int slice start but got ${start.getType()}"))
    }
    if (end.getType() !is IntType) {
        errors.add(OneLocCompileError(end.programLocation(), "Expected Int slice end but got ${end.getType()}"))
    }
    if (errors.isEmpty()) {
        inferExprType(symbolEnv)
    }
    return errors
}

private fun FunNode.typePassFunBody(
    funEnv: Map<String, FunNode>,
    registry: ObjClassRegistry,
    funBuiltinEnv: Map<String, FunBuiltin> = emptyMap(),
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
    val typeParamEnv: Map<String, Type> = typeParams.associateWith { TypeVar(it) }
    val bodyErrors = funBody().typePass(paramEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
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
