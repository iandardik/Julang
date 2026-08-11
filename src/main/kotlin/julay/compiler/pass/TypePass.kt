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

/** Set for the duration of [RootNode.typePass] — api-qualified procfun resolution. */
private var typePassApiEnv: Map<String, ApiNode> = emptyMap()
/** procfun name → api that lists it in calls: (first wins if overlapping). */
private var typePassCallToApi: Map<String, String> = emptyMap()
private var typePassInsideProcFun: Boolean = false
/** True while typing leaf-spec bodies: sort types OK in state/args (specs/TLA only). */
private var typePassAllowSortDomains: Boolean = false
/** Proc/leaf-spec classes visible for peer state reads in leaf-spec bodies. */
private var typePassPeerClasses: Map<String, ProcClassNode> = emptyMap()

/** Prefer an obj-literal hint when `Name(...)` names a known obj type rather than a function. */
internal fun unknownFunctionMessage(name: String, registry: ObjClassRegistry): String =
    if (registry.rawDecl(name) != null || ObjClassBuiltinRegistry.isBuiltin(name)) {
        "\"$name\" is an obj type, not a function; write $name { field := ... }, not $name(...)"
    } else {
        "Unknown function \"$name\""
    }

private fun sortDomainBan(type: Type, loc: ProgramLoc): CompileError? =
    if (typePassAllowSortDomains) null else sortDomainOnlyError(type, loc)

/** Value-level view: a sort domain is inhabited by its element type. */
private fun valueView(type: Type): Type =
    if (type is SortType) type.elementType else type

/** Typing view of a type in leaf-spec bodies: sort domains behave as their element type. */
private fun typingView(type: Type): Type =
    if (typePassAllowSortDomains) valueView(type) else type

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
    val prevApiEnv = typePassApiEnv
    val prevCallToApi = typePassCallToApi
    val prevInside = typePassInsideProcFun
    val prevPeers = typePassPeerClasses
    try {
        typePassInsideProcFun = false
        val modulesByPath = unit.modules.associateBy { it.modulePath }
        typePassPeerClasses = unit.modules
            .flatMap { it.root.declNodes() }
            .mapNotNull { decl ->
                when (decl) {
                    is ProcClassNode -> decl.name() to decl
                    is LeafSpecNode -> decl.name() to decl.asProcClass()
                    else -> null
                }
            }
            .toMap()

        val allFuns = unit.modules
            .flatMap { it.root.declNodes().filterIsInstance<FunNode>() }
            .associateBy { it.name() }
        val allProcFuns = unit.modules
            .flatMap { it.root.declNodes().filterIsInstance<ProcFunNode>() }
            .associateBy { it.name() }
        val signatureErrors = allFuns.values.flatMap { it.typePassFunSignature(built) }
        val procFunSigErrors = allProcFuns.values.flatMap { it.typePassProcFunSignature(built) }
        val recursionErrors = funRecursionErrors(allFuns) + procFunRecursionErrors(allProcFuns)
        val funBodyErrors = unit.modules.flatMap { module ->
            val moduleApis = callableApis(module)
            typePassApiEnv = moduleApis
            typePassCallToApi = buildMap {
                moduleApis.values.forEach { api ->
                    api.apiCallNames().forEach { call ->
                        putIfAbsent(call, api.apiName())
                    }
                }
            }
            val callable = callableFuns(module)
            val builtins = callableFunBuiltins(module)
            val procFuns = callableProcFuns(module, modulesByPath)
            module.root.declNodes().filterIsInstance<FunNode>().flatMap { funNode ->
                funNode.typePassFunBody(callable, built, builtins, procFuns)
            }
        }
        // Typecheck each module with that module's imports so julay.funlib.* (and imported
        // user funs) resolve in dependency modules, not only in the entry file.
        // Api.fn(...) only resolves for local/imported apis (not every transitively loaded api).
        val otherErrors = unit.modules.flatMap { module ->
            val moduleApis = callableApis(module)
            typePassApiEnv = moduleApis
            typePassCallToApi = buildMap {
                moduleApis.values.forEach { api ->
                    api.apiCallNames().forEach { call ->
                        putIfAbsent(call, api.apiName())
                    }
                }
            }
            val callable = callableFuns(module)
            val builtins = callableFunBuiltins(module)
            val procFuns = callableProcFuns(module, modulesByPath)
            module.root.declNodes()
                .filter {
                    it !is FunNode && it !is SpecNode && it !is InvariantNode && it !is ApiNode
                }
                .flatMap { decl ->
                    when (decl) {
                        is LeafSpecNode -> decl.typePassLeafSpec(
                            emptyMap(), built, callable, emptyMap(), builtins, procFuns,
                        )
                        else -> decl.typePass(emptyMap(), built, callable, emptyMap(), builtins, procFuns)
                    }
                }
        }
        val specResult = unit.root.specTypePass(unit, allowUnindexedSpec)
        return TypePassResult(
            errors = built.errors + signatureErrors + procFunSigErrors + recursionErrors + funBodyErrors + otherErrors + specResult.errors,
            warnings = specResult.warnings,
        )
    } finally {
        typePassApiEnv = prevApiEnv
        typePassCallToApi = prevCallToApi
        typePassInsideProcFun = prevInside
        typePassPeerClasses = prevPeers
    }
}

fun ASTNode.typePass(
    symbolEnv: Map<String, Type> = emptyMap(),
    registry: ObjClassRegistry = ObjClassRegistry.EMPTY,
    funEnv: Map<String, FunNode> = emptyMap(),
    typeParamEnv: Map<String, Type> = emptyMap(),
    funBuiltinEnv: Map<String, FunBuiltin> = emptyMap(),
    procFunEnv: Map<String, ProcFunNode> = emptyMap(),
): List<CompileError> = when (this) {
    is ProcClassNode -> typePassProcClass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv)
    is LeafSpecNode -> typePassLeafSpec(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv)
    is ProcFunNode -> typePassProcFun(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv)
    is VarNode -> typePassVarNode(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv)
    is ConstructorNode -> typePassConstructor(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv)
    is TransitionNode -> typePassTransition(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv)
    is ArgNode -> typePassArgNode(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv)
    is GuardNode -> typePassGuard(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv)
    is ReturnNode -> typePassReturn(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv)
    is TransitNode -> typePassTransit(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv)
    is LetTransitNode -> typePassLetTransit(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv)
    is VarTransitNode -> typePassVarTransit(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv)
    is IndexTransitNode -> typePassIndexTransit(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv)
    is ErrorNode -> typePassError(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv)
    is BeforeNode -> typePassBefore(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv)
    is AfterNode -> typePassAfter(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv)
    is CallStmtNode -> typePassCallStmt(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv)
    is UnaryOpExprNode -> typePassUnaryOp(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv)
    is BinaryOpExprNode -> typePassBinaryOp(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv)
    is IfElseExprNode -> typePassIfElse(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv)
    is LetExprNode -> typePassLet(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv)
    is WhenExprNode -> typePassWhen(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv)
    is ObjClassLiteralExprNode -> typePassObjClassLiteral(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv)
    is FieldAccessExprNode -> typePassFieldAccess(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv)
    is MemberAccessExprNode -> typePassMemberAccess(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv)
    is MethodCallExprNode -> typePassMethodCall(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv)
    is LambdaExprNode -> listOf(
        OneLocCompileError(
            programLocation(),
            "Lambda may only appear as an argument to a higher-order function",
        ),
    )
    is ListLiteralExprNode -> typePassListLiteral(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv)
    is SetLiteralExprNode -> typePassSetLiteral(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv)
    is MapLiteralExprNode -> typePassMapLiteral(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv)
    is IndexExprNode -> typePassIndex(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv)
    is FunCallExprNode -> typePassFunCall(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv)
    is SymbolValueExprNode -> typePassSymbol(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv)
    is ExprNode -> typePassExpr(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv)
    else -> children.flatMap { it.typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv) }
}

private fun ProcClassNode.typePassProcClass(
    symbolEnv: Map<String, Type>,
    registry: ObjClassRegistry,
    funEnv: Map<String, FunNode>,
    typeParamEnv: Map<String, Type>,
    funBuiltinEnv: Map<String, FunBuiltin>,
    procFunEnv: Map<String, ProcFunNode> = emptyMap(),
): List<CompileError> {
    val localDecls = localDecls()
    val varErrors = localDecls
        .filterIsInstance<VarNode>()
        .flatMap { it.typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv) }
    if (varErrors.isNotEmpty()) {
        return varErrors
    }
    val localSymbolEnv = symbolEnv + localDecls
        .filterIsInstance<VarNode>()
        .associate { it.name to typingView(it.type) }
    return varErrors + localDecls.flatMap { decl ->
        if (decl is VarNode) emptyList() else decl.typePass(localSymbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv)
    }
}

private fun LeafSpecNode.typePassLeafSpec(
    symbolEnv: Map<String, Type>,
    registry: ObjClassRegistry,
    funEnv: Map<String, FunNode>,
    typeParamEnv: Map<String, Type>,
    funBuiltinEnv: Map<String, FunBuiltin>,
    procFunEnv: Map<String, ProcFunNode> = emptyMap(),
): List<CompileError> {
    val paramErrors = mutableListOf<CompileError>()
    var env = symbolEnv
    val pName = leafSpecParamName()
    val pTypeExpr = leafSpecParamType()
    if (pName != null && pTypeExpr != null) {
        when (val result = registry.resolveTypeExpr(pTypeExpr, typeParamEnv, programLocation())) {
            is TypeResolveResult.Found -> {
                // Sort params are domain binders; in the body they behave as the sort's element type.
                val bodyType = when (val t = result.type) {
                    is SortType -> t.elementType
                    else -> t
                }
                env = env + (pName to bodyType)
            }
            is TypeResolveResult.Error -> {
                paramErrors += OneLocCompileError(
                    programLocation(),
                    "Unknown parameter type \"$pTypeExpr\" on leaf spec \"${leafSpecName()}\"",
                )
            }
        }
    }
    if (paramErrors.isNotEmpty()) return paramErrors
    val prevAllow = typePassAllowSortDomains
    typePassAllowSortDomains = true
    try {
        val bodyErrors = asProcClass().typePassProcClass(env, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv)
        val assignErrors = if (pName != null) leafSpecParamAssignmentErrors(pName) else emptyList()
        return bodyErrors + assignErrors
    } finally {
        typePassAllowSortDomains = prevAllow
    }
}

private fun LeafSpecNode.leafSpecParamAssignmentErrors(paramName: String): List<CompileError> {
    fun checkKey(key: String, loc: ProgramLoc): CompileError? =
        if (julay.program.type.transitRootVar(key) == paramName) {
            OneLocCompileError(loc, "Cannot assign to leaf-spec parameter \"$paramName\"")
        } else {
            null
        }
    val fromTransitions = localDecls()
        .filterIsInstance<TransitionNode>()
        .flatMap { trans ->
            trans.transitVars().mapNotNull { (key, loc) -> checkKey(key, loc) }
        }
    val fromCtors = localDecls()
        .filterIsInstance<ConstructorNode>()
        .flatMap { ctor ->
            ctor.transitVars().mapNotNull { (key, loc) -> checkKey(key, loc) }
        }
    return fromTransitions + fromCtors
}

private fun VarNode.typePassVarNode(
    symbolEnv: Map<String, Type>,
    registry: ObjClassRegistry,
    funEnv: Map<String, FunNode>,
    typeParamEnv: Map<String, Type>,
    funBuiltinEnv: Map<String, FunBuiltin>,
    procFunEnv: Map<String, ProcFunNode> = emptyMap(),
): List<CompileError> {
    return when (val result = registry.resolveTypeExpr(typeExpr, typeParamEnv, programLocation())) {
        is TypeResolveResult.Found -> {
            resolveType(result.type)
            val sortErr = listOfNotNull(sortDomainBan(result.type, programLocation()))
            val init = initExpr ?: return sortErr
            val expectedInitType = typingView(result.type)
            // Empty listOf()/setOf()/mapOf() need the declared type before typePass (same as transit assigns).
            applyExpectedCollectionType(init, expectedInitType)
            val initErrors = init.typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv)
            if (initErrors.isNotEmpty()) return sortErr + initErrors
            applyExpectedCollectionType(init, expectedInitType)
            sortErr + assertOrCompileError(
                init.getType() == expectedInitType,
                OneLocCompileError(
                    init.programLocation(),
                    "Expected init of \"$name\" to have type $expectedInitType but got ${init.getType()}",
                ),
            )
        }
        is TypeResolveResult.Error ->
            listOf(OneLocCompileError(programLocation(), "${result.message} for state variable \"$name\""))
    }
}

private fun ConstructorNode.typePassConstructor(
    symbolEnv: Map<String, Type>,
    registry: ObjClassRegistry,
    funEnv: Map<String, FunNode>,
    typeParamEnv: Map<String, Type>,
    funBuiltinEnv: Map<String, FunBuiltin>,
    procFunEnv: Map<String, ProcFunNode> = emptyMap(),
): List<CompileError> {
    val alsoErr = alsoArgsOutsideLeafError()
    if (alsoErr != null) return listOf(alsoErr)
    val argErrors = constructorArgs().typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv)
    val alsoErrors = alsoArgs()?.typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv).orEmpty()
    val alsoEnv = alsoArgs()?.argsTypeMap()?.mapValues { (_, t) -> typingView(t) }.orEmpty() +
        alsoArgs()?.actionArgs()?.filter { !it.name.isDiscardBinding() }
            ?.associate { it.name to typingView(it.type) }.orEmpty()
    val actionEnv = symbolEnv + constructorArgs().argsTypeMap().mapValues { (_, t) -> typingView(t) } +
        constructorArgs().actionArgs().filter { !it.name.isDiscardBinding() }
            .associate { it.name to typingView(it.type) } + alsoEnv
    return argErrors + alsoErrors + body().flatMap { it.typePass(actionEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv) }
}

private fun TransitionNode.typePassTransition(
    symbolEnv: Map<String, Type>,
    registry: ObjClassRegistry,
    funEnv: Map<String, FunNode>,
    typeParamEnv: Map<String, Type>,
    funBuiltinEnv: Map<String, FunBuiltin>,
    procFunEnv: Map<String, ProcFunNode> = emptyMap(),
): List<CompileError> {
    val alsoErr = alsoArgsOutsideLeafError()
    if (alsoErr != null) return listOf(alsoErr)
    val argErrors = transitionArgs().typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv)
    val alsoErrors = alsoArgs()?.typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv).orEmpty()
    val alsoEnv = alsoArgs()?.argsTypeMap()?.mapValues { (_, t) -> typingView(t) }.orEmpty() +
        alsoArgs()?.actionArgs()?.filter { !it.name.isDiscardBinding() }
            ?.associate { it.name to typingView(it.type) }.orEmpty()
    val actionEnv = symbolEnv + transitionArgs().argsTypeMap().mapValues { (_, t) -> typingView(t) } +
        transitionArgs().actionArgs().filter { !it.name.isDiscardBinding() }
            .associate { it.name to typingView(it.type) } + alsoEnv
    return argErrors + alsoErrors + body().flatMap { it.typePass(actionEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv) }
}

private fun ConstructorNode.alsoArgsOutsideLeafError(): CompileError? =
    if (alsoArgs() != null && !typePassAllowSortDomains) {
        OneLocCompileError(programLocation(), "also args are only allowed on leaf-spec actions")
    } else {
        null
    }

private fun TransitionNode.alsoArgsOutsideLeafError(): CompileError? =
    if (alsoArgs() != null && !typePassAllowSortDomains) {
        OneLocCompileError(programLocation(), "also args are only allowed on leaf-spec actions")
    } else {
        null
    }

private fun ArgNode.typePassArgNode(symbolEnv: Map<String, Type>, registry: ObjClassRegistry, funEnv: Map<String, FunNode>, typeParamEnv: Map<String, Type>,
    funBuiltinEnv: Map<String, FunBuiltin>,
    procFunEnv: Map<String, ProcFunNode> = emptyMap()): List<CompileError> =
    when (val result = registry.resolveTypeExpr(argTypeExpr(), typeParamEnv, programLocation())) {
        is TypeResolveResult.Found -> {
            resolveArgType(result.type)
            listOfNotNull(sortDomainBan(result.type, programLocation()))
        }
        is TypeResolveResult.Error ->
            listOf(OneLocCompileError(programLocation(), "Unknown type \"${argTypeName()}\" for action argument \"${argName()}\""))
    }

private fun GuardNode.typePassGuard(symbolEnv: Map<String, Type>, registry: ObjClassRegistry, funEnv: Map<String, FunNode>, typeParamEnv: Map<String, Type>,
    funBuiltinEnv: Map<String, FunBuiltin>,
    procFunEnv: Map<String, ProcFunNode> = emptyMap()): List<CompileError> {
    val childrenErrors = children.flatMap { it.typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv) }
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
    procFunEnv: Map<String, ProcFunNode> = emptyMap(),
): List<CompileError> {
    val childrenErrors = children.flatMap { it.typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv) }
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

private fun TransitNode.typePassTransit(
    symbolEnv: Map<String, Type>,
    registry: ObjClassRegistry,
    funEnv: Map<String, FunNode>,
    typeParamEnv: Map<String, Type>,
    funBuiltinEnv: Map<String, FunBuiltin>,
    procFunEnv: Map<String, ProcFunNode> = emptyMap(),
): List<CompileError> {
    var letEnv = emptyMap<String, Type>()
    val errors = mutableListOf<CompileError>()
    for (item in transitBodies()) {
        val rhsEnv = symbolEnv + letEnv
        when (item) {
            is LetTransitNode -> {
                val itemErrors = item.typePassLetTransit(rhsEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv)
                errors += itemErrors
                // Discard `_` may be rebound freely but is never in scope for later expressions.
                if (itemErrors.isEmpty() && !item.letName().isDiscardBinding()) {
                    letEnv = letEnv + (item.letName() to item.resolvedLetType)
                }
            }
            is VarTransitNode -> {
                errors += item.typePassVarTransit(
                    symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv, rhsEnv,
                )
            }
            is IndexTransitNode -> {
                errors += item.typePassIndexTransit(
                    symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv, rhsEnv,
                )
            }
            else -> {
                errors += item.typePass(rhsEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv)
            }
        }
    }
    return errors
}

private fun LetTransitNode.typePassLetTransit(
    symbolEnv: Map<String, Type>,
    registry: ObjClassRegistry,
    funEnv: Map<String, FunNode>,
    typeParamEnv: Map<String, Type>,
    funBuiltinEnv: Map<String, FunBuiltin>,
    procFunEnv: Map<String, ProcFunNode> = emptyMap(),
): List<CompileError> {
    val typeErrors = when (val result = registry.resolveTypeExpr(letTypeExpr(), typeParamEnv, programLocation())) {
        is TypeResolveResult.Found -> {
            resolveLetType(result.type)
            listOfNotNull(sortDomainBan(result.type, programLocation()))
        }
        is TypeResolveResult.Error ->
            listOf(
                OneLocCompileError(
                    programLocation(),
                    "Unknown type \"${letTypeName()}\" for transit let binding \"${letName()}\"",
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
    val initErrors = letInitExpr().typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv)
    if (initErrors.isNotEmpty()) {
        return initErrors
    }

    return assertOrCompileError(
        letInitExpr().getType() == declaredType,
        OneLocCompileError(
            letInitExpr().programLocation(),
            "Expected let initializer for \"${letName()}\" to have type $declaredType but got ${letInitExpr().getType()}",
        ),
    )
}

private fun VarTransitNode.typePassVarTransit(
    symbolEnv: Map<String, Type>,
    registry: ObjClassRegistry,
    funEnv: Map<String, FunNode>,
    typeParamEnv: Map<String, Type>,
    funBuiltinEnv: Map<String, FunBuiltin>,
    procFunEnv: Map<String, ProcFunNode> = emptyMap(),
    rhsEnv: Map<String, Type> = symbolEnv,
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
            is FieldPathResult.Resolved -> valueView(result.type)
            is FieldPathResult.Error -> null
        }
    }
    expectedType?.let { applyExpectedCollectionType(transitExpr(), it) }
    val childrenErrors = children.flatMap { it.typePass(rhsEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv) }
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
                val fieldExpected = valueView(result.type)
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
                        transitExpr().getType() == fieldExpected,
                        OneLocCompileError(
                            programLocation(),
                            "Expected assignment to \"${transitKey()}\" ($fieldExpected) but got expression of type ${transitExpr().getType()}",
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
    procFunEnv: Map<String, ProcFunNode> = emptyMap(),
): List<CompileError> =
    befores().flatMap { it.typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv) }

private fun AfterNode.typePassAfter(
    symbolEnv: Map<String, Type>,
    registry: ObjClassRegistry,
    funEnv: Map<String, FunNode>,
    typeParamEnv: Map<String, Type>,
    funBuiltinEnv: Map<String, FunBuiltin>,
    procFunEnv: Map<String, ProcFunNode> = emptyMap(),
): List<CompileError> =
    afters().flatMap { it.typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv) }

private fun CallStmtNode.typePassCallStmt(
    symbolEnv: Map<String, Type>,
    registry: ObjClassRegistry,
    funEnv: Map<String, FunNode>,
    typeParamEnv: Map<String, Type>,
    funBuiltinEnv: Map<String, FunBuiltin>,
    procFunEnv: Map<String, ProcFunNode> = emptyMap(),
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
        val childrenErrors = callArgs().flatMap { it.typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv) }
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
    val errors = asExpr.typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv)
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
    procFunEnv: Map<String, ProcFunNode> = emptyMap(),
): List<CompileError> {
    val childErrors = children.flatMap { it.typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv) }
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
    procFunEnv: Map<String, ProcFunNode> = emptyMap(),
): List<CompileError> {
    val childErrors = children.flatMap { it.typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv) }
    if (childErrors.isNotEmpty()) {
        return childErrors
    }
    val lhsType = lhsOperand().getType()
    val rhsType = rhsOperand().getType()
    // Membership is about the RHS collection; handle before obj/list/set/map branches
    // so obj elements/keys (e.g. Node in Set<Node>) are not rejected as invalid obj ops.
    val structOpErrors = if (op() == "in") {
        when (rhsType) {
            is ListType -> assertOrCompileError(
                lhsType == rhsType.elementType,
                OneLocCompileError(
                    programLocation(),
                    "Expected \"in\" list element type $lhsType to match ${rhsType.elementType}",
                ),
            )
            is SetType -> assertOrCompileError(
                lhsType == rhsType.elementType,
                OneLocCompileError(
                    programLocation(),
                    "Expected \"in\" set element type $lhsType to match ${rhsType.elementType}",
                ),
            )
            is MapType -> assertOrCompileError(
                lhsType == rhsType.keyType,
                OneLocCompileError(
                    programLocation(),
                    "Expected \"in\" map key type $lhsType to match ${rhsType.keyType}",
                ),
            )
            else -> listOf(OneLocCompileError(programLocation(), "Cannot apply \"in\" to types $lhsType and $rhsType"))
        }
    } else if (op() == "+" && (lhsType is StringType || rhsType is StringType)) {
        // If either side of "+" is String, coerce the other via toString (string concat).
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
            else -> listOf(OneLocCompileError(programLocation(), "Cannot apply \"${op()}\" to map type $lhsType"))
        }
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
    funBuiltinEnv: Map<String, FunBuiltin>,
    procFunEnv: Map<String, ProcFunNode> = emptyMap()): List<CompileError> {
    val childErrors = children.flatMap { it.typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv) }
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
    funBuiltinEnv: Map<String, FunBuiltin>,
    procFunEnv: Map<String, ProcFunNode> = emptyMap()): List<CompileError> {
    val typeErrors = when (val result = registry.resolveTypeExpr(letTypeExpr(), typeParamEnv, programLocation())) {
        is TypeResolveResult.Found -> {
            resolveLetType(result.type)
            listOfNotNull(sortDomainBan(result.type, programLocation()))
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
    val initErrors = letInitExpr().typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv)
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

    val bodyEnv = if (letName().isDiscardBinding()) {
        symbolEnv
    } else {
        symbolEnv + (letName() to declaredType)
    }
    val bodyErrors = bodyExpr().typePass(bodyEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv)
    if (bodyErrors.isNotEmpty()) {
        return bodyErrors
    }
    inferExprType(bodyEnv)
    return emptyList()
}

private fun WhenExprNode.typePassWhen(symbolEnv: Map<String, Type>, registry: ObjClassRegistry, funEnv: Map<String, FunNode>, typeParamEnv: Map<String, Type>,
    funBuiltinEnv: Map<String, FunBuiltin>,
    procFunEnv: Map<String, ProcFunNode> = emptyMap()): List<CompileError> {
    val subjectErrors = subjectExpr()?.typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv) ?: emptyList()
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
                    is WhenPattern.Struct -> pattern.literal.typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv)
                }
                patternErrors + arm.expr.typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv)
            }
            is WhenArm.Guard -> arm.cond.typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv) + arm.expr.typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv)
            is WhenArm.Else -> arm.expr.typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv)
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
    procFunEnv: Map<String, ProcFunNode> = emptyMap(),
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

    val childErrors = children.flatMap { it.typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv) }
    if (childErrors.isNotEmpty()) {
        return childErrors
    }

    val matchErrors = resolvedType.fields.flatMap { field ->
        val expr = fieldAssignments.getValue(field.name)
        val expected = valueView(field.type)
        assertOrCompileError(
            expr.getType() == expected,
            OneLocCompileError(
                expr.programLocation(),
                "Expected field \"${field.name}\" of \"$className\" to have type $expected but got ${expr.getType()}",
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
    procFunEnv: Map<String, ProcFunNode> = emptyMap(),
): List<CompileError> {
    if (typePassAllowSortDomains && fieldPath.size == 1) {
        val pc = typePassPeerClasses[baseSymbol]
        if (pc != null && baseSymbol !in symbolEnv) {
            val vn = pc.localDecls().filterIsInstance<VarNode>().firstOrNull { it.name == fieldPath[0] }
            if (vn == null) {
                return listOf(
                    OneLocCompileError(
                        programLocation(),
                        "unknown state variable \"$baseSymbol.${fieldPath[0]}\"",
                    ),
                )
            }
            val t = try {
                valueView(vn.type)
            } catch (_: RuntimeException) {
                return listOf(OneLocCompileError(programLocation(), "unresolved type for \"$baseSymbol.${fieldPath[0]}\""))
            }
            resolveFieldAccess(t, fieldPath[0])
            inferExprType(symbolEnv)
            return emptyList()
        }
    }
    val baseType = symbolEnv[baseSymbol]
    if (baseType == null) {
        return listOf(OneLocCompileError(programLocation(), "Unknown variable \"$baseSymbol\" in field access"))
    }
    when (val coll = resolveCollectionPropertyPath(baseType, fieldPath)) {
        is CollectionPropResult.Resolved -> {
            resolveFieldAccess(valueView(coll.type), fieldPath.joinToString("."))
            inferExprType(symbolEnv)
            return emptyList()
        }
        is CollectionPropResult.Error ->
            return listOf(OneLocCompileError(programLocation(), coll.message))
        is CollectionPropResult.NotCollectionProp -> {}
    }
    return when (val result = resolveFieldPath(baseType, fieldPath)) {
        is FieldPathResult.Error -> listOf(OneLocCompileError(programLocation(), result.message))
        is FieldPathResult.Resolved -> {
            resolveFieldAccess(valueView(result.type), result.relPath)
            val childErrors = children.flatMap { it.typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv) }
            inferExprType(symbolEnv)
            childErrors
        }
    }
}

private data class PeerRefTypeResult(val type: Type?, val errors: List<CompileError>)

/** Resolve Peer[idx].field / Peer.field in leaf-spec bodies; null if not a peer ref. */
private fun peerStateRefType(
    baseExpr: ExprNode,
    fieldName: String,
    symbolEnv: Map<String, Type>,
    registry: ObjClassRegistry,
    funEnv: Map<String, FunNode>,
    typeParamEnv: Map<String, Type>,
    funBuiltinEnv: Map<String, FunBuiltin>,
    procFunEnv: Map<String, ProcFunNode>,
): PeerRefTypeResult? {
    when (baseExpr) {
        is IndexExprNode -> {
            val peerSym = (baseExpr.base as? SymbolValueExprNode)?.symbol ?: return null
            val pc = typePassPeerClasses[peerSym] ?: return null
            if (peerSym in symbolEnv) return null
            val idxErrors = baseExpr.index.typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv)
            val vn = pc.localDecls().filterIsInstance<VarNode>().firstOrNull { it.name == fieldName }
                ?: return PeerRefTypeResult(
                    null,
                    listOf(OneLocCompileError(baseExpr.programLocation(), "unknown state variable \"$peerSym.$fieldName\"")),
                )
            val t = try {
                valueView(vn.type)
            } catch (_: RuntimeException) {
                return PeerRefTypeResult(
                    null,
                    listOf(OneLocCompileError(baseExpr.programLocation(), "unresolved type for \"$peerSym.$fieldName\"")),
                )
            }
            return PeerRefTypeResult(t, idxErrors)
        }
        is SymbolValueExprNode -> {
            val pc = typePassPeerClasses[baseExpr.symbol] ?: return null
            if (baseExpr.symbol in symbolEnv) return null
            val vn = pc.localDecls().filterIsInstance<VarNode>().firstOrNull { it.name == fieldName }
                ?: return PeerRefTypeResult(
                    null,
                    listOf(OneLocCompileError(baseExpr.programLocation(), "unknown state variable \"${baseExpr.symbol}.$fieldName\"")),
                )
            val t = try {
                valueView(vn.type)
            } catch (_: RuntimeException) {
                return PeerRefTypeResult(
                    null,
                    listOf(OneLocCompileError(baseExpr.programLocation(), "unresolved type for \"${baseExpr.symbol}.$fieldName\"")),
                )
            }
            return PeerRefTypeResult(t, emptyList())
        }
        else -> return null
    }
}

private fun MemberAccessExprNode.typePassMemberAccess(
    symbolEnv: Map<String, Type>,
    registry: ObjClassRegistry,
    funEnv: Map<String, FunNode>,
    typeParamEnv: Map<String, Type>,
    funBuiltinEnv: Map<String, FunBuiltin>,
    procFunEnv: Map<String, ProcFunNode> = emptyMap(),
): List<CompileError> {
    // Leaf-spec peer read: Peer[idx].var or Peer.var
    if (typePassAllowSortDomains) {
        val peerRef = peerStateRefType(baseExpr, fieldName, symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv)
        if (peerRef != null) {
            if (peerRef.errors.isNotEmpty()) return peerRef.errors
            setInferredType(TypePassType.Inferred(peerRef.type!!))
            return emptyList()
        }
    }
    val baseErrors = baseExpr.typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv)
    if (baseErrors.isNotEmpty()) {
        return baseErrors
    }
    val baseType = try {
        baseExpr.getType()
    } catch (_: RuntimeException) {
        return listOf(OneLocCompileError(programLocation(), "Cannot resolve type of member-access base"))
    }
    when (val coll = resolveCollectionProperty(baseType, fieldName)) {
        is CollectionPropResult.Resolved -> {
            setInferredType(TypePassType.Inferred(valueView(coll.type)))
            return emptyList()
        }
        is CollectionPropResult.Error ->
            return listOf(OneLocCompileError(programLocation(), coll.message))
        is CollectionPropResult.NotCollectionProp -> {}
    }
    return when (val result = resolveFieldPath(baseType, listOf(fieldName))) {
        is FieldPathResult.Error -> listOf(OneLocCompileError(programLocation(), result.message))
        is FieldPathResult.Resolved -> {
            setInferredType(TypePassType.Inferred(valueView(result.type)))
            emptyList()
        }
    }
}

private fun MethodCallExprNode.typePassMethodCall(
    symbolEnv: Map<String, Type>,
    registry: ObjClassRegistry,
    funEnv: Map<String, FunNode>,
    typeParamEnv: Map<String, Type>,
    funBuiltinEnv: Map<String, FunBuiltin>,
    procFunEnv: Map<String, ProcFunNode> = emptyMap(),
): List<CompileError> {
    // Api-qualified procfun: ApiName.fn(...)
    val apiBase = (baseExpr as? SymbolValueExprNode)?.symbol
    if (apiBase != null) {
        val api = typePassApiEnv[apiBase]
        if (api != null) {
            if (methodName !in api.apiCallNames()) {
                return listOf(
                    OneLocCompileError(
                        programLocation(),
                        "Procfun \"$methodName\" is not listed in api \"$apiBase\" calls:",
                    ),
                )
            }
            val procFun = procFunEnv[methodName]
                ?: return listOf(
                    OneLocCompileError(
                        programLocation(),
                        "Unknown procfun \"$methodName\"",
                    ),
                )
            val argErrors = args.flatMap { it.typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv) }
            if (argErrors.isNotEmpty()) return argErrors
            resolveApiProcFun(apiBase, procFun)
            val params = try {
                procFun.procFunArgs().actionArgs()
            } catch (_: RuntimeException) {
                return listOf(
                    OneLocCompileError(
                        programLocation(),
                        "Parameter types not resolved for procfun \"$methodName\"",
                    ),
                )
            }
            if (params.size != args.size) {
                return listOf(
                    OneLocCompileError(
                        programLocation(),
                        "Expected procfun \"$methodName\" to take ${params.size} argument(s) but got ${args.size}",
                    ),
                )
            }
            val typeMismatchErrors = args.zip(params).flatMap { (arg, param) ->
                assertOrCompileError(
                    arg.getType() == param.type,
                    OneLocCompileError(
                        arg.programLocation(),
                        "Expected argument of type ${param.type} but got ${arg.getType()}",
                    ),
                )
            }
            if (typeMismatchErrors.isNotEmpty()) return typeMismatchErrors
            setInferredType(TypePassType.Inferred(procFun.returnType))
            return emptyList()
        }
    }

    val kind = collectionMethodKind(methodName)
        ?: return listOf(
            OneLocCompileError(
                programLocation(),
                "Unknown method \"$methodName\"",
            ),
        )
    val baseErrors = baseExpr.typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv)
    if (baseErrors.isNotEmpty()) {
        return baseErrors
    }
    val collType = try {
        baseExpr.getType()
    } catch (_: RuntimeException) {
        return listOf(OneLocCompileError(programLocation(), "Cannot resolve type of method-call receiver"))
    }
    return when (kind) {
        CollectionMethodKind.Filter -> typePassFilter(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv, collType)
        CollectionMethodKind.Map -> typePassMapMethod(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv, collType)
        CollectionMethodKind.Fold -> typePassFold(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv, collType)
    }
}

private fun MethodCallExprNode.typePassFilter(
    symbolEnv: Map<String, Type>,
    registry: ObjClassRegistry,
    funEnv: Map<String, FunNode>,
    typeParamEnv: Map<String, Type>,
    funBuiltinEnv: Map<String, FunBuiltin>,
    procFunEnv: Map<String, ProcFunNode>,
    collType: Type,
): List<CompileError> {
    val elemType = collectionElementType(collType)
        ?: return listOf(
            OneLocCompileError(programLocation(), "Expected receiver of \"filter\" to have a List or Set type but got $collType"),
        )
    if (args.size != 1) {
        return listOf(OneLocCompileError(programLocation(), "Expected method \"filter\" to take 1 argument(s) but got ${args.size}"))
    }
    val resultType = sameCollectionType(collType)!!
    return typePassUnaryHofArg(
        symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv,
        args[0], elemType, expectedBodyType = boolType, resultType = resultType,
    )
}

private fun MethodCallExprNode.typePassMapMethod(
    symbolEnv: Map<String, Type>,
    registry: ObjClassRegistry,
    funEnv: Map<String, FunNode>,
    typeParamEnv: Map<String, Type>,
    funBuiltinEnv: Map<String, FunBuiltin>,
    procFunEnv: Map<String, ProcFunNode>,
    collType: Type,
): List<CompileError> {
    val elemType = collectionElementType(collType)
        ?: return listOf(
            OneLocCompileError(programLocation(), "Expected receiver of \"map\" to have a List or Set type but got $collType"),
        )
    if (args.size != 1) {
        return listOf(OneLocCompileError(programLocation(), "Expected method \"map\" to take 1 argument(s) but got ${args.size}"))
    }
    return typePassUnaryHofArg(
        symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv,
        args[0], elemType, expectedBodyType = null, resultType = null, mapCollType = collType,
    )
}

private fun MethodCallExprNode.typePassFold(
    symbolEnv: Map<String, Type>,
    registry: ObjClassRegistry,
    funEnv: Map<String, FunNode>,
    typeParamEnv: Map<String, Type>,
    funBuiltinEnv: Map<String, FunBuiltin>,
    procFunEnv: Map<String, ProcFunNode>,
    collType: Type,
): List<CompileError> {
    val elemType = collectionElementType(collType)
        ?: return listOf(
            OneLocCompileError(programLocation(), "Expected receiver of \"fold\" to have a List or Set type but got $collType"),
        )
    if (args.size != 2) {
        return listOf(OneLocCompileError(programLocation(), "Expected method \"fold\" to take 2 argument(s) but got ${args.size}"))
    }
    val initErrors = args[0].typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv)
    if (initErrors.isNotEmpty()) {
        return initErrors
    }
    val accType = args[0].getType()
    val lambda = args[1]
    if (lambda !is LambdaExprNode) {
        return listOf(
            OneLocCompileError(
                lambda.programLocation(),
                "Expected second argument of \"fold\" to be a binary lambda (acc, elem) -> expr",
            ),
        )
    }
    if (lambda.params.size != 2) {
        return listOf(
            OneLocCompileError(
                lambda.programLocation(),
                "Expected fold lambda to take 2 parameter(s) but got ${lambda.params.size}",
            ),
        )
    }
    val (accName, elemName) = lambda.params
    val bodyEnv = symbolEnv + listOf(accName to accType, elemName to elemType).filter { !it.first.isDiscardBinding() }
    val bodyErrors = lambda.body.typePass(bodyEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv)
    if (bodyErrors.isNotEmpty()) {
        return bodyErrors
    }
    val bodyType = lambda.body.getType()
    if (bodyType != accType) {
        return listOf(
            OneLocCompileError(
                lambda.body.programLocation(),
                "Expected fold lambda body to have type $accType but got $bodyType",
            ),
        )
    }
    resolveHof(lambda.body, listOf(accName, elemName), listOf(accType, elemType))
    setInferredType(TypePassType.Inferred(accType))
    return emptyList()
}

/**
 * @param expectedBodyType if non-null, body must have this type (filter → Boolean)
 * @param resultType if non-null, method result type (filter)
 * @param mapCollType if non-null, result is mapped collection of body type
 */
private fun MethodCallExprNode.typePassUnaryHofArg(
    symbolEnv: Map<String, Type>,
    registry: ObjClassRegistry,
    funEnv: Map<String, FunNode>,
    typeParamEnv: Map<String, Type>,
    funBuiltinEnv: Map<String, FunBuiltin>,
    procFunEnv: Map<String, ProcFunNode>,
    funArg: ExprNode,
    elemType: Type,
    expectedBodyType: Type?,
    resultType: Type?,
    mapCollType: Type? = null,
): List<CompileError> {
    return when (funArg) {
        is LambdaExprNode -> {
            if (funArg.params.size != 1) {
                return listOf(
                    OneLocCompileError(
                        funArg.programLocation(),
                        "Expected unary lambda but got ${funArg.params.size} parameter(s)",
                    ),
                )
            }
            val param = funArg.params.single()
            val bodyEnv = if (param.isDiscardBinding()) symbolEnv else symbolEnv + (param to elemType)
            val bodyErrors = funArg.body.typePass(bodyEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv)
            if (bodyErrors.isNotEmpty()) {
                return bodyErrors
            }
            val bodyType = funArg.body.getType()
            if (expectedBodyType != null && bodyType != expectedBodyType) {
                return listOf(
                    OneLocCompileError(
                        funArg.body.programLocation(),
                        "Expected lambda body to have type $expectedBodyType but got $bodyType",
                    ),
                )
            }
            val outType = when {
                resultType != null -> resultType
                mapCollType != null -> mapResultCollectionType(mapCollType, bodyType)
                    ?: return listOf(OneLocCompileError(programLocation(), "map expected List or Set"))
                else -> error("unreachable")
            }
            resolveHof(funArg.body, listOf(param), listOf(elemType))
            setInferredType(TypePassType.Inferred(outType))
            emptyList()
        }
        is SymbolValueExprNode -> {
            // Named unary fun, same as freestanding map(xs, f)
            val mappedFun = funEnv[funArg.symbol]
                ?: return listOf(OneLocCompileError(funArg.programLocation(), "Unknown function \"${funArg.symbol}\""))
            val params = try {
                mappedFun.funArgs().actionArgs()
            } catch (_: RuntimeException) {
                return listOf(
                    OneLocCompileError(funArg.programLocation(), "Parameter types not resolved for function \"${funArg.symbol}\""),
                )
            }
            if (params.size != 1) {
                return listOf(
                    OneLocCompileError(
                        funArg.programLocation(),
                        "Expected function \"${funArg.symbol}\" to take 1 argument(s) but got ${params.size}",
                    ),
                )
            }
            val param = params.single()
            val subst = mutableMapOf<String, Type>()
            when (val unify = unifyTypes(param.type, elemType, subst)) {
                is UnifyResult.Fail ->
                    return listOf(
                        OneLocCompileError(
                            funArg.programLocation(),
                            "Expected argument \"${param.name}\" of \"${funArg.symbol}\" to have type ${param.type} but got $elemType: ${unify.message}",
                        ),
                    )
                is UnifyResult.Ok -> {}
            }
            when (val ret = registry.resolveTypeExpr(mappedFun.funReturnTypeExpr(), subst, programLocation())) {
                is TypeResolveResult.Found -> {
                    if (expectedBodyType != null && ret.type != expectedBodyType) {
                        return listOf(
                            OneLocCompileError(
                                funArg.programLocation(),
                                "Expected function \"${funArg.symbol}\" to return $expectedBodyType but got ${ret.type}",
                            ),
                        )
                    }
                    val outType = when {
                        resultType != null -> resultType
                        mapCollType != null -> mapResultCollectionType(mapCollType, ret.type)
                            ?: return listOf(OneLocCompileError(programLocation(), "map expected List or Set"))
                        else -> error("unreachable")
                    }
                    val body = mappedFun.funBody()
                    resolveHof(body, listOf(param.name), listOf(elemType))
                    setInferredType(TypePassType.Inferred(outType))
                    emptyList()
                }
                is TypeResolveResult.Error -> listOf(OneLocCompileError(programLocation(), ret.message))
            }
        }
        else -> listOf(
            OneLocCompileError(
                funArg.programLocation(),
                "Expected a unary lambda or function name",
            ),
        )
    }
}

private fun ExprNode.typePassExpr(symbolEnv: Map<String, Type>, registry: ObjClassRegistry, funEnv: Map<String, FunNode>, typeParamEnv: Map<String, Type>,
    funBuiltinEnv: Map<String, FunBuiltin>,
    procFunEnv: Map<String, ProcFunNode> = emptyMap()): List<CompileError> {
    val childrenErrors = children.flatMap { it.typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv) }
    inferExprType(symbolEnv)
    return childrenErrors
}

private fun SymbolValueExprNode.typePassSymbol(
    symbolEnv: Map<String, Type>,
    registry: ObjClassRegistry,
    funEnv: Map<String, FunNode>,
    typeParamEnv: Map<String, Type>,
    funBuiltinEnv: Map<String, FunBuiltin>,
    procFunEnv: Map<String, ProcFunNode> = emptyMap(),
): List<CompileError> {
    if (symbol.isDiscardBinding()) {
        return listOf(
            OneLocCompileError(programLocation(), "Cannot reference discard binding \"_\""),
        )
    }
    if (symbol !in symbolEnv) {
        return listOf(OneLocCompileError(programLocation(), "Unknown variable \"$symbol\""))
    }
    inferExprType(symbolEnv)
    return emptyList()
}

/**
 * Type `map(xs, f)` where `f` is a bare unary user-fun name (not a value expression).
 */
private fun FunCallExprNode.typePassNamedFunMap(
    symbolEnv: Map<String, Type>,
    registry: ObjClassRegistry,
    funEnv: Map<String, FunNode>,
    typeParamEnv: Map<String, Type>,
    funBuiltinEnv: Map<String, FunBuiltin>,
    procFunEnv: Map<String, ProcFunNode>,
    builtin: FunBuiltin,
): List<CompileError> {
    resolveBuiltin(builtin)
    if (callTypeArgs().isNotEmpty()) {
        return listOf(
            OneLocCompileError(
                programLocation(),
                "Expected function \"${callName()}\" not to take type arguments",
            ),
        )
    }
    if (callArgs().size != 2) {
        return listOf(
            OneLocCompileError(
                programLocation(),
                "Expected function \"map\" to take 2 argument(s) but got ${callArgs().size}",
            ),
        )
    }
    val collArg = callArgs()[0]
    val funArg = callArgs()[1]
    val collErrors = collArg.typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv)
    if (collErrors.isNotEmpty()) {
        return collErrors
    }
    val collType = collArg.getType()
    val elemType = when (collType) {
        is ListType -> collType.elementType
        is SetType -> collType.elementType
        else -> return listOf(
            OneLocCompileError(
                collArg.programLocation(),
                "Expected first argument of \"map\" to have a List or Set type but got $collType",
            ),
        )
    }
    if (funArg is LambdaExprNode) {
        if (funArg.params.size != 1) {
            return listOf(
                OneLocCompileError(
                    funArg.programLocation(),
                    "Expected unary lambda but got ${funArg.params.size} parameter(s)",
                ),
            )
        }
        val param = funArg.params.single()
        val bodyEnv = if (param.isDiscardBinding()) symbolEnv else symbolEnv + (param to elemType)
        val bodyErrors = funArg.body.typePass(bodyEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv)
        if (bodyErrors.isNotEmpty()) {
            return bodyErrors
        }
        val resultType = when (collType) {
            is ListType -> listType(funArg.body.getType())
            is SetType -> setType(funArg.body.getType())
            else -> error("unreachable")
        }
        resolveNamedFunArg(null, param, funArg.body, elemType)
        resolveInstantiatedReturnType(resultType)
        inferExprType(symbolEnv)
        return emptyList()
    }
    if (funArg !is SymbolValueExprNode) {
        return listOf(
            OneLocCompileError(
                funArg.programLocation(),
                "Expected second argument of \"map\" to be a unary function name or lambda",
            ),
        )
    }
    val mappedFun = funEnv[funArg.symbol]
        ?: return listOf(
            OneLocCompileError(
                funArg.programLocation(),
                "Unknown function \"${funArg.symbol}\"",
            ),
        )
    val params = try {
        mappedFun.funArgs().actionArgs()
    } catch (_: RuntimeException) {
        return listOf(
            OneLocCompileError(
                funArg.programLocation(),
                "Parameter types not resolved for function \"${funArg.symbol}\"",
            ),
        )
    }
    if (params.size != 1) {
        return listOf(
            OneLocCompileError(
                funArg.programLocation(),
                "Expected function \"${funArg.symbol}\" to take 1 argument(s) but got ${params.size}",
            ),
        )
    }
    val param = params.single()
    val subst = mutableMapOf<String, Type>()
    when (val unify = unifyTypes(param.type, elemType, subst)) {
        is UnifyResult.Fail ->
            return listOf(
                OneLocCompileError(
                    funArg.programLocation(),
                    "Expected argument \"${param.name}\" of \"${funArg.symbol}\" to have type ${param.type} but got $elemType: ${unify.message}",
                ),
            )
        is UnifyResult.Ok -> {}
    }
    if (mappedFun.typeParams.isNotEmpty()) {
        val unbound = mappedFun.typeParams.filter { it !in subst }
        if (unbound.isNotEmpty()) {
            return listOf(
                OneLocCompileError(
                    funArg.programLocation(),
                    "Cannot infer type parameter(s) ${unbound.joinToString(", ")} for function \"${funArg.symbol}\"",
                ),
            )
        }
    }
    return when (val ret = registry.resolveTypeExpr(mappedFun.funReturnTypeExpr(), subst, programLocation())) {
        is TypeResolveResult.Found -> {
            val resultType = when (collType) {
                is ListType -> listType(ret.type)
                is SetType -> setType(ret.type)
                else -> error("unreachable")
            }
            val specialized = if (mappedFun.typeParams.isNotEmpty()) {
                val valueInlined = mappedFun.funBody()
                val specializeErrors = valueInlined.typePass(symbolEnv + (param.name to elemType), registry, funEnv, subst)
                if (specializeErrors.isNotEmpty()) {
                    return specializeErrors
                }
                valueInlined
            } else {
                mappedFun.funBody()
            }
            resolveNamedFunArg(mappedFun, param.name, specialized, elemType)
            resolveInstantiatedReturnType(resultType)
            inferExprType(symbolEnv)
            emptyList()
        }
        is TypeResolveResult.Error ->
            listOf(OneLocCompileError(programLocation(), ret.message))
    }
}

private fun FunCallExprNode.typePassFunCall(
    symbolEnv: Map<String, Type>,
    registry: ObjClassRegistry,
    funEnv: Map<String, FunNode>,
    typeParamEnv: Map<String, Type>,
    funBuiltinEnv: Map<String, FunBuiltin>,
    procFunEnv: Map<String, ProcFunNode> = emptyMap(),
): List<CompileError> {
    funBuiltinEnv[callName()]?.let { builtin ->
        if (builtin.namedFunArg) {
            return typePassNamedFunMap(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv, builtin)
        }
    }
    val argErrors = callArgs().flatMap { it.typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv) }
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
        if (builtin.name == "splice") {
            val listTy = argTypes[0] as? ListType
                ?: return listOf(
                    OneLocCompileError(
                        programLocation(),
                        "Expected first argument of \"splice\" to have a List type but got ${argTypes[0]}",
                    ),
                )
            resolveInstantiatedReturnType(listTy)
        } else {
            resolveInstantiatedReturnType(returnType)
        }
        inferExprType(symbolEnv)
        return emptyList()
    }
    procFunEnv[callName()]?.let { procFun ->
        resolveProcFun(procFun)
        typePassCallToApi[callName()]?.let { apiName ->
            if (!typePassInsideProcFun) {
                return listOf(
                    OneLocCompileError(
                        programLocation(),
                        "Procfun \"${callName()}\" is listed in api \"$apiName\" calls:; " +
                            "call it as $apiName.${callName()}(...)",
                    ),
                )
            }
        }
        if (callTypeArgs().isNotEmpty()) {
            return listOf(
                OneLocCompileError(
                    programLocation(),
                    "Expected procfun \"${callName()}\" not to take type arguments",
                ),
            )
        }
        val params = try {
            procFun.procFunArgs().actionArgs()
        } catch (_: RuntimeException) {
            return listOf(
                OneLocCompileError(
                    programLocation(),
                    "Parameter types not resolved for procfun \"${callName()}\"",
                ),
            )
        }
        if (params.size != callArgs().size) {
            return listOf(
                OneLocCompileError(
                    programLocation(),
                    "Expected procfun \"${callName()}\" to take ${params.size} argument(s) but got ${callArgs().size}",
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
                        "Return type not resolved for procfun \"${callName()}\"",
                    ),
                )
            }
        }
        return typeMismatchErrors
    }
    val funNode = funEnv[callName()]
        ?: return listOf(
            OneLocCompileError(
                programLocation(),
                unknownFunctionMessage(callName(), registry),
            ),
        )
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
                listOfNotNull(sortDomainBan(result.type, arg.programLocation()))
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
            listOfNotNull(sortDomainBan(result.type, programLocation()))
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
        expr is SetLiteralExprNode && expr.elements.isEmpty() && expected is SetType ->
            expr.resolveSetType(expected)
        expr is MapLiteralExprNode && expr.entries.isEmpty() && expected is MapType ->
            expr.resolveMapType(expected)
    }
}

private fun requireCollectionFunlib(
    name: String,
    funBuiltinEnv: Map<String, FunBuiltin>,
    loc: ProgramLoc,
): CompileError? =
    if (funBuiltinEnv.containsKey(name)) {
        null
    } else {
        OneLocCompileError(loc, "Unknown function \"$name\"")
    }

private fun ListLiteralExprNode.typePassListLiteral(
    symbolEnv: Map<String, Type>,
    registry: ObjClassRegistry,
    funEnv: Map<String, FunNode>,
    typeParamEnv: Map<String, Type>,
    funBuiltinEnv: Map<String, FunBuiltin>,
    procFunEnv: Map<String, ProcFunNode> = emptyMap(),
): List<CompileError> {
    requireCollectionFunlib("listOf", funBuiltinEnv, programLocation())?.let { return listOf(it) }
    val childErrors = elements.flatMap { it.typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv) }
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
                "Empty listOf() requires a known List target type",
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
    procFunEnv: Map<String, ProcFunNode> = emptyMap(),
): List<CompileError> {
    val childErrors = children.flatMap { it.typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv) }
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

private fun IndexTransitNode.typePassIndexTransit(
    symbolEnv: Map<String, Type>,
    registry: ObjClassRegistry,
    funEnv: Map<String, FunNode>,
    typeParamEnv: Map<String, Type>,
    funBuiltinEnv: Map<String, FunBuiltin>,
    procFunEnv: Map<String, ProcFunNode> = emptyMap(),
    rhsEnv: Map<String, Type> = symbolEnv,
): List<CompileError> {
    val collectionType = symbolEnv[collectionVar]
    if (collectionType == null) {
        return listOf(OneLocCompileError(programLocation(), "Unknown variable \"$collectionVar\" in transit assignment"))
    }
    when (collectionType) {
        is MapType, is ListType -> {}
        else -> {
            return listOf(
                OneLocCompileError(
                    programLocation(),
                    "Expected map or list variable for index assignment but \"$collectionVar\" has type $collectionType",
                ),
            )
        }
    }
    val childErrors = children.flatMap { it.typePass(rhsEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv) }
    if (childErrors.isNotEmpty()) {
        return childErrors
    }
    val errors = mutableListOf<CompileError>()
    when (collectionType) {
        is MapType -> {
            if (index.getType() != collectionType.keyType) {
                errors.add(
                    OneLocCompileError(
                        index.programLocation(),
                        "Expected map key type ${collectionType.keyType} but got ${index.getType()}",
                    ),
                )
            }
            if (value.getType() != collectionType.valueType) {
                errors.add(
                    OneLocCompileError(
                        value.programLocation(),
                        "Expected map value type ${collectionType.valueType} but got ${value.getType()}",
                    ),
                )
            }
        }
        is ListType -> {
            if (index.getType() !is IntType) {
                errors.add(
                    OneLocCompileError(
                        index.programLocation(),
                        "Expected Int index but got ${index.getType()}",
                    ),
                )
            }
            if (value.getType() != collectionType.elementType) {
                errors.add(
                    OneLocCompileError(
                        value.programLocation(),
                        "Expected list element type ${collectionType.elementType} but got ${value.getType()}",
                    ),
                )
            }
        }
        else -> {}
    }
    return errors
}

private fun SetLiteralExprNode.typePassSetLiteral(
    symbolEnv: Map<String, Type>,
    registry: ObjClassRegistry,
    funEnv: Map<String, FunNode>,
    typeParamEnv: Map<String, Type>,
    funBuiltinEnv: Map<String, FunBuiltin>,
    procFunEnv: Map<String, ProcFunNode> = emptyMap(),
): List<CompileError> {
    requireCollectionFunlib("setOf", funBuiltinEnv, programLocation())?.let { return listOf(it) }
    val childErrors = elements.flatMap { it.typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv) }
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
                "Empty setOf() requires a known Set target type",
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
    procFunEnv: Map<String, ProcFunNode> = emptyMap(),
): List<CompileError> {
    requireCollectionFunlib("mapOf", funBuiltinEnv, programLocation())?.let { return listOf(it) }
    val childErrors = entries.flatMap { (k, v) ->
        k.typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv) +
            v.typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv)
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
                "Empty mapOf() requires a known Map target type",
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

private fun FunNode.typePassFunBody(
    funEnv: Map<String, FunNode>,
    registry: ObjClassRegistry,
    funBuiltinEnv: Map<String, FunBuiltin> = emptyMap(),
    procFunEnv: Map<String, ProcFunNode> = emptyMap(),
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
    val paramEnv = params.filter { !it.name.isDiscardBinding() }.associate { it.name to it.type }
    val typeParamEnv: Map<String, Type> = typeParams.associateWith { TypeVar(it) }
    val bodyErrors = funBody().typePass(paramEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv)
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

private fun ProcFunNode.typePassProcFunSignature(registry: ObjClassRegistry): List<CompileError> {
    val argNodes = procFunArgs().children.filterIsInstance<ArgNode>()
    val argErrors = argNodes.flatMap { arg ->
        when (val result = registry.resolveTypeExpr(arg.argTypeExpr(), emptyMap(), arg.programLocation())) {
            is TypeResolveResult.Found -> {
                arg.resolveArgType(result.type)
                listOfNotNull(sortDomainBan(result.type, arg.programLocation()))
            }
            is TypeResolveResult.Error ->
                listOf(
                    OneLocCompileError(
                        arg.programLocation(),
                        "${result.message} for procfun argument \"${arg.argName()}\"",
                    ),
                )
        }
    }
    val returnErrors = when (val result = registry.resolveTypeExpr(procFunReturnTypeExpr(), emptyMap(), programLocation())) {
        is TypeResolveResult.Found -> {
            resolveReturnType(result.type)
            listOfNotNull(sortDomainBan(result.type, programLocation()))
        }
        is TypeResolveResult.Error ->
            listOf(
                OneLocCompileError(
                    programLocation(),
                    "${result.message} for return type of procfun \"${name()}\"",
                ),
            )
    }
    return argErrors + returnErrors
}

private fun ProcFunNode.typePassProcFun(
    symbolEnv: Map<String, Type>,
    registry: ObjClassRegistry,
    funEnv: Map<String, FunNode>,
    typeParamEnv: Map<String, Type>,
    funBuiltinEnv: Map<String, FunBuiltin>,
    procFunEnv: Map<String, ProcFunNode>,
): List<CompileError> {
    val prevInside = typePassInsideProcFun
    typePassInsideProcFun = true
    try {
        return typePassProcFunBody(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv)
    } finally {
        typePassInsideProcFun = prevInside
    }
}

private fun ProcFunNode.typePassProcFunBody(
    symbolEnv: Map<String, Type>,
    registry: ObjClassRegistry,
    funEnv: Map<String, FunNode>,
    typeParamEnv: Map<String, Type>,
    funBuiltinEnv: Map<String, FunBuiltin>,
    procFunEnv: Map<String, ProcFunNode>,
): List<CompileError> {
    val argErrors = procFunArgs().typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv)
    if (argErrors.isNotEmpty()) return argErrors
    val argEnv = try {
        procFunArgs().actionArgs().associate { it.name to it.type }
    } catch (_: RuntimeException) {
        return emptyList()
    }
    // Type vars in declaration order so later inits can see earlier ones + args.
    val vars = localDecls().filterIsInstance<VarNode>()
    val initEnv = argEnv.toMutableMap()
    val varErrors = mutableListOf<CompileError>()
    for (v in vars) {
        varErrors += v.typePass(initEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv)
        if (varErrors.isEmpty() || v.name in initEnv || runCatching { v.type }.isSuccess) {
            try {
                initEnv[v.name] = v.type
            } catch (_: RuntimeException) {}
        }
    }
    if (varErrors.isNotEmpty()) return varErrors
    val localSymbolEnv = initEnv.toMap()
    val returnType = try {
        returnType
    } catch (_: RuntimeException) {
        return emptyList()
    }
    val bodyErrors = localDecls().flatMap { decl ->
        when (decl) {
            is VarNode -> emptyList()
            is TransitionNode -> {
                val transErrors = decl.typePass(localSymbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv)
                val returnExprs = decl.body().flatMap { it.returns() }
                val retTypeErrors = returnExprs.flatMap { expr ->
                    assertOrCompileError(
                        expr.getType() == returnType,
                        OneLocCompileError(
                            expr.programLocation(),
                            "Expected return of procfun \"${name()}\" to have type $returnType but got ${expr.getType()}",
                        ),
                    )
                }
                transErrors + retTypeErrors
            }
            else -> decl.typePass(localSymbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv)
        }
    }
    return bodyErrors
}

private fun ReturnNode.typePassReturn(
    symbolEnv: Map<String, Type>,
    registry: ObjClassRegistry,
    funEnv: Map<String, FunNode>,
    typeParamEnv: Map<String, Type>,
    funBuiltinEnv: Map<String, FunBuiltin>,
    procFunEnv: Map<String, ProcFunNode>,
): List<CompileError> =
    children.flatMap { it.typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, procFunEnv) }

private fun procFunRecursionErrors(allProcFuns: Map<String, ProcFunNode>): List<CompileError> {
    if (allProcFuns.isEmpty()) return emptyList()
    val callGraph = allProcFuns.mapValues { (_, pf) ->
        pf.localDecls().flatMap { collectFunCallNamesFromNode(it) }.filter { it in allProcFuns }.toSet()
    }
    val errors = mutableListOf<CompileError>()
    val visiting = mutableSetOf<String>()
    val visited = mutableSetOf<String>()

    fun dfs(name: String, path: List<String>) {
        if (name in visited) return
        if (name in visiting) {
            val cycleStart = path.indexOf(name)
            val cycle = (path.drop(cycleStart) + name).joinToString(" -> ")
            errors.add(
                OneLocCompileError(
                    allProcFuns.getValue(name).programLocation(),
                    "Recursive procfun calls are not allowed: $cycle",
                ),
            )
            return
        }
        visiting.add(name)
        callGraph[name].orEmpty().forEach { callee -> dfs(callee, path + name) }
        visiting.remove(name)
        visited.add(name)
    }
    allProcFuns.keys.forEach { dfs(it, emptyList()) }
    return errors.distinctBy { it.toString() }
}

private fun collectFunCallNamesFromNode(node: ASTNode): Set<String> = when (node) {
    is FunCallExprNode -> setOf(node.callName()) + node.children.flatMap { collectFunCallNamesFromNode(it) }
    else -> node.children.flatMap { collectFunCallNamesFromNode(it) }.toSet()
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
