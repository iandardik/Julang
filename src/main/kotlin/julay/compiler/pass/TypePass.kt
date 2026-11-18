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

/**
 * Validate create-index / leaf-spec `name := expr` fun overrides: known nullary user fun,
 * return-type match, no conflicting definitions.
 */
fun validateSpecFunModels(
    models: List<FunModelNode>,
    funEnv: Map<String, FunNode>,
    registry: ObjClassRegistry,
    funBuiltinEnv: Map<String, FunBuiltin> = emptyMap(),
): List<CompileError> {
    val (merged, conflictErrors) = mergeFunModels(models)
    val errors = conflictErrors.toMutableList()
    merged.values.forEach { model ->
        val name = model.name()
        val funNode = funEnv[name]
        if (funNode == null) {
            errors += OneLocCompileError(
                model.programLocation(),
                "Unknown function \"$name\" for fun override",
            )
            return@forEach
        }
        if (funNode.funArgs().actionArgs().isNotEmpty()) {
            errors += OneLocCompileError(
                model.programLocation(),
                "Fun override \"$name\" is only allowed for nullary functions",
            )
            return@forEach
        }
        val exprErrors = model.overrideExpr.typePass(
            emptyMap(), registry, funEnv, emptyMap(), funBuiltinEnv,
        )
        errors += exprErrors
        if (exprErrors.isEmpty()) {
            try {
                val got = model.overrideExpr.getType()
                val expected = funNode.returnType
                if (got != expected) {
                    errors += OneLocCompileError(
                        model.programLocation(),
                        "Fun override \"$name\" has type $got but function returns $expected",
                    )
                }
            } catch (_: RuntimeException) {
            }
        }
    }
    return errors
}

private var typePassModulesByPath: Map<String, LoadedModule> = emptyMap()
private var typePassCurrentModule: LoadedModule? = null
/** True while typing leaf-spec bodies: sort types OK in state/args (specs/TLA only). */
private var typePassAllowSortDomains: Boolean = false
/** Proc/leaf-spec classes visible for peer state reads in leaf-spec bodies. */
private var typePassPeerClasses: Map<String, ProcClassNode> = emptyMap()
/**
 * While typing invariant/`init:` formulas: unparameterized leaves (`Leaf.var` only).
 * Empty outside [typePassSpecFormula].
 */
private var typePassUnindexedPeerClasses: Map<String, ProcClassNode> = emptyMap()
/**
 * While typing invariant/`init:` formulas: parameterized leaves (`Leaf[i].var` only).
 * Empty outside [typePassSpecFormula].
 */
private var typePassIndexedPeerClasses: Map<String, ProcClassNode> = emptyMap()
/** Expected index types for [typePassIndexedPeerClasses], when known. */
private var typePassIndexedPeerParamTypes: Map<String, Type> = emptyMap()
/**
 * Non-null while typing `init:`: leaf name → const-global var names. Indexed
 * `Leaf[i].var` is rejected for those names (scalars) and for mutable vars;
 * indexed `const` state is allowed.
 */
private var typePassInitConstGlobals: Map<String, Set<String>>? = null
/** True while typing invariant/`init:` formulas (tighter peer indexing, no effects). */
private var typePassSpecFormula: Boolean = false
/** Reject IO/session/transition-only builtins and `.fold` (spec formulas). */
private var typePassForbidEffects: Boolean = false
/**
 * State var/const types for the enclosing proc/procfun/leaf-spec body.
 * Null outside those bodies (`this` illegal). Empty map means a leaf with no state.
 */
private var typePassStateEnv: Map<String, Type>? = null

/**
 * Type an invariant or `init:` formula with the same [ASTNode.typePass] used for
 * proc / leaf-spec bodies, plus spec-only flags (sort domains, peer maps, no `this`).
 */
internal fun typePassSpecFormula(
    expr: ExprNode,
    symbolEnv: Map<String, Type>,
    registry: ObjClassRegistry,
    funEnv: Map<String, FunNode>,
    funBuiltinEnv: Map<String, FunBuiltin>,
    unindexedPeers: Map<String, ProcClassNode>,
    indexedPeers: Map<String, ProcClassNode>,
    indexedParamTypes: Map<String, Type> = emptyMap(),
    initConstGlobals: Map<String, Set<String>>? = null,
): List<CompileError> {
    val prevAllow = typePassAllowSortDomains
    val prevPeers = typePassPeerClasses
    val prevUnindexed = typePassUnindexedPeerClasses
    val prevIndexed = typePassIndexedPeerClasses
    val prevParamTypes = typePassIndexedPeerParamTypes
    val prevInit = typePassInitConstGlobals
    val prevSpec = typePassSpecFormula
    val prevForbid = typePassForbidEffects
    val prevState = typePassStateEnv
    typePassAllowSortDomains = true
    typePassSpecFormula = true
    typePassForbidEffects = true
    typePassUnindexedPeerClasses = unindexedPeers
    typePassIndexedPeerClasses = indexedPeers
    typePassIndexedPeerParamTypes = indexedParamTypes
    typePassInitConstGlobals = initConstGlobals
    typePassPeerClasses = unindexedPeers + indexedPeers
    typePassStateEnv = null
    val builtins = funBuiltinEnv.toMutableMap()
    FunBuiltinRegistry.lookup("length")?.let { builtins.putIfAbsent("length", it) }
    try {
        return expr.typePass(symbolEnv, registry, funEnv, emptyMap(), builtins)
    } finally {
        typePassAllowSortDomains = prevAllow
        typePassPeerClasses = prevPeers
        typePassUnindexedPeerClasses = prevUnindexed
        typePassIndexedPeerClasses = prevIndexed
        typePassIndexedPeerParamTypes = prevParamTypes
        typePassInitConstGlobals = prevInit
        typePassSpecFormula = prevSpec
        typePassForbidEffects = prevForbid
        typePassStateEnv = prevState
    }
}

private fun unindexedPeerClass(name: String): ProcClassNode? =
    if (typePassSpecFormula) typePassUnindexedPeerClasses[name] else typePassPeerClasses[name]

private fun indexedPeerClass(name: String): ProcClassNode? =
    if (typePassSpecFormula) typePassIndexedPeerClasses[name] else typePassPeerClasses[name]

private fun isSpecPeerName(name: String): Boolean =
    name in typePassUnindexedPeerClasses ||
        name in typePassIndexedPeerClasses ||
        name in typePassPeerClasses

/** Walk `.length` / `.keys` / obj fields after a peer state var. */
private fun resolveAccessPath(rootType: Type, path: List<String>): Pair<Type?, String?> {
    if (path.isEmpty()) return rootType to null
    var current = rootType
    var i = 0
    while (i < path.size) {
        val remaining = path.subList(i, path.size)
        when (val coll = resolveCollectionPropertyPath(current, remaining)) {
            is CollectionPropResult.Resolved -> return coll.type to null
            is CollectionPropResult.Error -> return null to coll.message
            is CollectionPropResult.NotCollectionProp -> {}
        }
        when (val result = resolveFieldPath(current, listOf(path[i]))) {
            is FieldPathResult.Error -> return null to result.message
            is FieldPathResult.Resolved -> {
                current = result.type
                i++
            }
        }
    }
    return current to null
}

/** Prefer an obj-literal hint when `Name(...)` names a known obj type rather than a function. */
internal fun unknownFunctionMessage(name: String, registry: ObjClassRegistry): String =
    if (registry.rawDecl(name) != null || ObjClassBuiltinRegistry.isBuiltin(name)) {
        "\"$name\" is a type, not a function; write $name { field := ... }, not $name(...)"
    } else {
        "Unknown function \"$name\""
    }

private fun sortDomainBan(type: Type, loc: ProgramLoc): CompileError? =
    if (typePassAllowSortDomains) null else domainOnlyError(type, loc)

/** Value-level view: domain types use their carrier / element type in specs. */
private fun valueView(type: Type): Type = when (type) {
    is DomainType -> type.carrierType
    is ListType -> listType(valueView(type.elementType))
    is SetType -> setType(valueView(type.elementType))
    is MapType -> mapType(valueView(type.keyType), valueView(type.valueType))
    else -> type
}

/** Proc/JAR view: typedefs erase to carrier; uninterpreted stays (and is banned). */
private fun procErasure(type: Type): Type = when (type) {
    is DomainType -> if (type.kind == DomainKind.Typedef) type.carrierType else type
    is ListType -> listType(procErasure(type.elementType))
    is SetType -> setType(procErasure(type.elementType))
    is MapType -> mapType(procErasure(type.keyType), procErasure(type.valueType))
    else -> type
}

/** Typing view: specs use carrier for domains; procs erase typedefs to carrier. */
private fun typingView(type: Type): Type =
    if (typePassAllowSortDomains) valueView(type) else procErasure(type)

fun RootNode.typePass(
    unit: CompilationUnit,
    allowUnindexedSpec: Boolean = false,
): TypePassResult {
    val domainResult = unit.collectDomains()
    if (domainResult.errors.isNotEmpty()) {
        return TypePassResult(domainResult.errors)
    }
    val allRawObjClasses = unit.modules.flatMap { module ->
        module.root.declNodes().flatMap { it.objClassPass() }
    }
    val built = ObjClassRegistry.build(allRawObjClasses, domainResult.domains)
    cacheObjClassRegistry(built)
    unit.modules.forEach { it.root.cacheObjClassRegistry(built) }
    if (built.errors.isNotEmpty()) {
        return TypePassResult(built.errors)
    }
    val prevPeers = typePassPeerClasses
    try {
        val modulesByPath = unit.modules.associateBy { it.modulePath }
        typePassModulesByPath = modulesByPath
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
        val signatureErrors = allFuns.values.flatMap { it.typePassFunSignature(built) }
        val recursionErrors = funRecursionErrors(allFuns)
        val funBodyErrors = unit.modules.flatMap { module ->
            typePassCurrentModule = module
            val callable = callableFuns(module)
            val builtins = callableFunBuiltins(module)
            module.root.declNodes().filterIsInstance<FunNode>().flatMap { funNode ->
                funNode.typePassFunBody(callable, built, builtins)
            }
        }
        val funModelErrors = validateSpecFunModels(
            unit.modules.flatMap { it.root.collectNestedFunModels() },
            allFuns,
            built,
        )
        // Typecheck each module with that module's imports so julay.funlib.* (and imported
        // user funs) resolve in dependency modules, not only in the entry file.
        val otherErrors = unit.modules.flatMap { module ->
            typePassCurrentModule = module
            val callable = callableFuns(module)
            val builtins = callableFunBuiltins(module)
            module.root.declNodes()
                .filter {
                    it !is FunNode && it !is SpecNode && it !is InvariantNode
                }
                .flatMap { decl ->
                    when (decl) {
                        is LeafSpecNode -> decl.typePassLeafSpec(
                            emptyMap(), built, callable, emptyMap(), builtins,
                        )
                        else -> decl.typePass(emptyMap(), built, callable, emptyMap(), builtins)
                    }
                }
        }
        val specResult = unit.root.specTypePass(unit, allowUnindexedSpec)
        return TypePassResult(
            errors = built.errors + signatureErrors + recursionErrors +
                funBodyErrors + funModelErrors + otherErrors + specResult.errors,
            warnings = specResult.warnings,
        )
    } finally {
        typePassPeerClasses = prevPeers
    }
}

fun ASTNode.typePass(
    symbolEnv: Map<String, Type> = emptyMap(),
    registry: ObjClassRegistry = ObjClassRegistry.EMPTY,
    funEnv: Map<String, FunNode> = emptyMap(),
    typeParamEnv: Map<String, Type> = emptyMap(),
    funBuiltinEnv: Map<String, FunBuiltin> = emptyMap(),
): List<CompileError> = when (this) {
    is ProcClassNode -> typePassProcClass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
    is LeafSpecNode -> typePassLeafSpec(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
    is VarNode -> typePassVarNode(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
    is ConstructorNode -> typePassConstructor(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
    is TransitionNode -> typePassTransition(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
    is ArgNode -> typePassArgNode(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
    is GuardNode -> typePassGuard(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
    is ReturnActionNode -> typePassReturnAction(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
    is HandlerRefExprNode -> typePassHandlerRef()
    is TransitNode -> typePassTransit(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
    is LetTransitNode -> typePassLetTransit(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
    is VarTransitNode -> typePassVarTransit(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
    is IndexTransitNode -> typePassIndexTransit(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
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
    is ThisAccessExprNode -> typePassThisAccess(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
    is MemberAccessExprNode -> typePassMemberAccess(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
    is MethodCallExprNode -> typePassMethodCall(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
    is LambdaExprNode -> listOf(
        OneLocCompileError(
            programLocation(),
            "Lambda may only appear as an argument to a higher-order function",
        ),
    )
    is ListLiteralExprNode -> typePassListLiteral(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
    is SetLiteralExprNode -> typePassSetLiteral(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
    is MapLiteralExprNode -> typePassMapLiteral(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
    is IndexExprNode -> typePassIndex(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
    is FunCallExprNode -> typePassFunCall(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
    is SymbolValueExprNode -> typePassSymbol(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
    is QuantifiedExprNode -> typePassQuantified(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
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
    val stateEnv = localDecls
        .filterIsInstance<VarNode>()
        .associate { it.name to typingView(it.type) }
    val localSymbolEnv = symbolEnv + stateEnv
    val prevState = typePassStateEnv
    typePassStateEnv = stateEnv
    try {
        return varErrors + localDecls.flatMap { decl ->
            if (decl is VarNode) emptyList() else decl.typePass(localSymbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
        }
    } finally {
        typePassStateEnv = prevState
    }
}

private fun LeafSpecNode.typePassLeafSpec(
    symbolEnv: Map<String, Type>,
    registry: ObjClassRegistry,
    funEnv: Map<String, FunNode>,
    typeParamEnv: Map<String, Type>,
    funBuiltinEnv: Map<String, FunBuiltin>,
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
                    is DomainType -> t.carrierType
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
        val bodyErrors = asProcClass().typePassProcClass(env, registry, funEnv, typeParamEnv, funBuiltinEnv)
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
): List<CompileError> {
    return when (val result = registry.resolveTypeExpr(typeExpr, typeParamEnv, programLocation())) {
        is TypeResolveResult.Found -> {
            resolveType(result.type)
            val sortErr = listOfNotNull(sortDomainBan(result.type, programLocation()))
            val init = initExpr ?: return sortErr
            val expectedInitType = typingView(result.type)
            // Empty listOf()/setOf()/mapOf() need the declared type before typePass (same as transit assigns).
            applyExpectedCollectionType(init, expectedInitType)
            val initErrors = init.typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
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
): List<CompileError> {
    val alsoErr = alsoArgsOutsideLeafError()
    if (alsoErr != null) return listOf(alsoErr)
    val argErrors = constructorArgs().typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
    val alsoErrors = alsoArgs()?.typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv).orEmpty()
    val alsoEnv = alsoArgs()?.argsTypeMap()?.mapValues { (_, t) -> typingView(t) }.orEmpty() +
        alsoArgs()?.actionArgs()?.filter { !it.name.isDiscardBinding() }
            ?.associate { it.name to typingView(it.type) }.orEmpty()
    val actionEnv = symbolEnv + constructorArgs().argsTypeMap().mapValues { (_, t) -> typingView(t) } +
        constructorArgs().actionArgs().filter { !it.name.isDiscardBinding() }
            .associate { it.name to typingView(it.type) } + alsoEnv
    return argErrors + alsoErrors + body().flatMap { it.typePass(actionEnv, registry, funEnv, typeParamEnv, funBuiltinEnv) }
}

private fun TransitionNode.typePassTransition(
    symbolEnv: Map<String, Type>,
    registry: ObjClassRegistry,
    funEnv: Map<String, FunNode>,
    typeParamEnv: Map<String, Type>,
    funBuiltinEnv: Map<String, FunBuiltin>,
): List<CompileError> {
    val alsoErr = alsoArgsOutsideLeafError()
    if (alsoErr != null) return listOf(alsoErr)
    val argErrors = transitionArgs().typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
    val alsoErrors = alsoArgs()?.typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv).orEmpty()
    val alsoEnv = alsoArgs()?.argsTypeMap()?.mapValues { (_, t) -> typingView(t) }.orEmpty() +
        alsoArgs()?.actionArgs()?.filter { !it.name.isDiscardBinding() }
            ?.associate { it.name to typingView(it.type) }.orEmpty()
    val actionEnv = symbolEnv + transitionArgs().argsTypeMap().mapValues { (_, t) -> typingView(t) } +
        transitionArgs().actionArgs().filter { !it.name.isDiscardBinding() }
            .associate { it.name to typingView(it.type) } + alsoEnv
    return argErrors + alsoErrors + body().flatMap { it.typePass(actionEnv, registry, funEnv, typeParamEnv, funBuiltinEnv) }
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
    funBuiltinEnv: Map<String, FunBuiltin>): List<CompileError> =
    when (val result = registry.resolveTypeExpr(argTypeExpr(), typeParamEnv, programLocation())) {
        is TypeResolveResult.Found -> {
            // Keep typedef/uninterpreted DomainType on the arg (like state vars) so TLA+
            // `\E` binders use CONSTANT Key/Value/… . Expression typing still applies
            // typingView when building the action env.
            resolveArgType(result.type)
            listOfNotNull(sortDomainBan(result.type, programLocation()))
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

private fun TransitNode.typePassTransit(
    symbolEnv: Map<String, Type>,
    registry: ObjClassRegistry,
    funEnv: Map<String, FunNode>,
    typeParamEnv: Map<String, Type>,
    funBuiltinEnv: Map<String, FunBuiltin>,
): List<CompileError> {
    var letEnv = emptyMap<String, Type>()
    val errors = mutableListOf<CompileError>()
    for (item in transitBodies()) {
        val rhsEnv = symbolEnv + letEnv
        when (item) {
            is LetTransitNode -> {
                val itemErrors = item.typePassLetTransit(rhsEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
                errors += itemErrors
                // Discard `_` may be rebound freely but is never in scope for later expressions.
                if (itemErrors.isEmpty() && !item.letName().isDiscardBinding()) {
                    letEnv = letEnv + (item.letName() to item.resolvedLetType)
                }
            }
            is VarTransitNode -> {
                errors += item.typePassVarTransit(
                    symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, rhsEnv,
                )
            }
            is IndexTransitNode -> {
                errors += item.typePassIndexTransit(
                    symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, rhsEnv,
                )
            }
            else -> {
                errors += item.typePass(rhsEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
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
): List<CompileError> {
    val typeErrors = when (val result = registry.resolveTypeExpr(letTypeExpr(), typeParamEnv, programLocation())) {
        is TypeResolveResult.Found -> {
            resolveLetType(typingView(result.type))
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
    val initErrors = letInitExpr().typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
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
    rhsEnv: Map<String, Type> = symbolEnv,
): List<CompileError> {
    // Transit targets are always process state (never action args), even when names collide.
    val lhsEnv = typePassStateEnv ?: symbolEnv
    val varType = lhsEnv[varName]
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
    val childrenErrors = children.flatMap { it.typePass(rhsEnv, registry, funEnv, typeParamEnv, funBuiltinEnv) }
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
    // Membership is about the RHS collection; handle before obj/list/set/map branches
    // so obj elements/keys (e.g. Node in Set<Node>) are not rejected as invalid obj ops.
    val structOpErrors = if (op() == "in" || op() == "~in") {
        val membershipOp = op()
        when (rhsType) {
            is ListType -> assertOrCompileError(
                lhsType == rhsType.elementType,
                OneLocCompileError(
                    programLocation(),
                    "Expected \"$membershipOp\" list element type $lhsType to match ${rhsType.elementType}",
                ),
            )
            is SetType -> assertOrCompileError(
                lhsType == rhsType.elementType,
                OneLocCompileError(
                    programLocation(),
                    "Expected \"$membershipOp\" set element type $lhsType to match ${rhsType.elementType}",
                ),
            )
            is MapType -> assertOrCompileError(
                lhsType == rhsType.keyType,
                OneLocCompileError(
                    programLocation(),
                    "Expected \"$membershipOp\" map key type $lhsType to match ${rhsType.keyType}",
                ),
            )
            else -> listOf(OneLocCompileError(programLocation(), "Cannot apply \"$membershipOp\" to types $lhsType and $rhsType"))
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
            resolveLetType(typingView(result.type))
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

    val bodyEnv = if (letName().isDiscardBinding()) {
        symbolEnv
    } else {
        symbolEnv + (letName() to declaredType)
    }
    val bodyErrors = bodyExpr().typePass(bodyEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
    if (bodyErrors.isNotEmpty()) {
        return bodyErrors
    }
    inferExprType(bodyEnv)
    return emptyList()
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
            add(OneLocCompileError(programLocation(), "Obj literal for \"$className\" is missing fields: $missingFields"))
        }
        if (extraFields.isNotEmpty()) {
            add(OneLocCompileError(programLocation(), "Obj literal for \"$className\" has unknown fields: $extraFields"))
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
): List<CompileError> {
    if (typePassAllowSortDomains && fieldPath == listOf("length") && baseSymbol !in symbolEnv &&
        registry.domains.containsKey(baseSymbol)
    ) {
        resolveFieldAccess(intType, "length")
        inferExprType(symbolEnv)
        return emptyList()
    }
    if (typePassAllowSortDomains && fieldPath.isNotEmpty() && baseSymbol !in symbolEnv) {
        val unindexed = unindexedPeerClass(baseSymbol)
        val indexed = indexedPeerClass(baseSymbol)
        val pc = when {
            unindexed != null -> unindexed
            indexed != null && typePassSpecFormula -> {
                val varName = fieldPath[0]
                return listOf(
                    OneLocCompileError(
                        programLocation(),
                        "parameterized component \"$baseSymbol\" requires indexed access \"$baseSymbol[i].$varName\"",
                    ),
                )
            }
            indexed != null -> indexed
            else -> null
        }
        if (pc != null) {
            val varName = fieldPath[0]
            val vn = pc.localDecls().filterIsInstance<VarNode>().firstOrNull { it.name == varName }
            if (vn == null) {
                return listOf(
                    OneLocCompileError(
                        programLocation(),
                        "unknown state variable \"$baseSymbol.$varName\"",
                    ),
                )
            }
            val initConsts = typePassInitConstGlobals
            if (initConsts != null && varName !in initConsts[baseSymbol].orEmpty()) {
                return listOf(
                    OneLocCompileError(
                        programLocation(),
                        "\"init:\" may only mention const-global state; \"$baseSymbol.$varName\" is not const global",
                    ),
                )
            }
            val varType = try {
                valueView(vn.type)
            } catch (_: RuntimeException) {
                return listOf(OneLocCompileError(programLocation(), "unresolved type for \"$baseSymbol.$varName\""))
            }
            val rest = fieldPath.drop(1)
            val (leafType, pathErr) = resolveAccessPath(varType, rest)
            if (pathErr != null) {
                return listOf(OneLocCompileError(programLocation(), pathErr))
            }
            if (leafType == null) {
                return listOf(OneLocCompileError(programLocation(), "unresolved type for \"$baseSymbol.${fieldPath.joinToString(".")}\""))
            }
            resolveFieldAccess(leafType, fieldPath.joinToString("."))
            inferExprType(symbolEnv)
            return emptyList()
        }
    }
    val baseType = symbolEnv[baseSymbol]
    if (baseType == null) {
        if (typePassInitConstGlobals != null) {
            return listOf(
                OneLocCompileError(
                    programLocation(),
                    "unbound \"$baseSymbol.${fieldPath.joinToString(".")}\" in init: " +
                        "(use a const-global name, Leaf.var, or Sort.length)",
                ),
            )
        }
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
            val childErrors = children.flatMap { it.typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv) }
            inferExprType(symbolEnv)
            childErrors
        }
    }
}

private fun ThisAccessExprNode.typePassThisAccess(
    symbolEnv: Map<String, Type>,
    registry: ObjClassRegistry,
    funEnv: Map<String, FunNode>,
    typeParamEnv: Map<String, Type>,
    funBuiltinEnv: Map<String, FunBuiltin>,
): List<CompileError> {
    val stateEnv = typePassStateEnv
        ?: return listOf(
            OneLocCompileError(programLocation(), "\"this\" is only allowed in proc action bodies"),
        )
    val root = stateVarName()
    val rest = nestedFieldPath()
    val rootType = stateEnv[root]
        ?: return listOf(
            OneLocCompileError(programLocation(), "unknown state variable \"this.$root\""),
        )
    if (rest.isEmpty()) {
        resolveThisAccess(rootType, valueView(rootType), root)
        inferExprType(symbolEnv)
        return emptyList()
    }
    when (val coll = resolveCollectionPropertyPath(rootType, rest)) {
        is CollectionPropResult.Resolved -> {
            resolveThisAccess(rootType, valueView(coll.type), rest.joinToString("."))
            inferExprType(symbolEnv)
            return emptyList()
        }
        is CollectionPropResult.Error ->
            return listOf(OneLocCompileError(programLocation(), coll.message))
        is CollectionPropResult.NotCollectionProp -> {}
    }
    return when (val result = resolveFieldPath(rootType, rest)) {
        is FieldPathResult.Error -> listOf(OneLocCompileError(programLocation(), result.message))
        is FieldPathResult.Resolved -> {
            resolveThisAccess(rootType, valueView(result.type), result.relPath)
            inferExprType(symbolEnv)
            emptyList()
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
): PeerRefTypeResult? {
    when (baseExpr) {
        is IndexExprNode -> {
            val peerSym = (baseExpr.base as? SymbolValueExprNode)?.symbol ?: return null
            if (peerSym in symbolEnv) return null
            val pc = indexedPeerClass(peerSym)
            if (pc == null) {
                if (!typePassSpecFormula || !isSpecPeerName(peerSym)) return null
                val loc = baseExpr.programLocation()
                val fieldErr = if (typePassInitConstGlobals != null) {
                    OneLocCompileError(
                        loc,
                        "\"init:\" cannot use indexed access \"$peerSym[i].$fieldName\"; " +
                            "const-globals are scalars (write $peerSym.$fieldName or a bare name)",
                    )
                } else {
                    OneLocCompileError(
                        loc,
                        "component \"$peerSym\" is not parameterized; use \"$peerSym.$fieldName\"",
                    )
                }
                return PeerRefTypeResult(null, listOf(fieldErr))
            }
            val idxErrors = baseExpr.index.typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
            val vn = pc.localDecls().filterIsInstance<VarNode>().firstOrNull { it.name == fieldName }
                ?: return PeerRefTypeResult(
                    null,
                    listOf(OneLocCompileError(baseExpr.programLocation(), "unknown state variable \"$peerSym.$fieldName\"")),
                )
            val initConsts = typePassInitConstGlobals
            if (initConsts != null) {
                val loc = baseExpr.programLocation()
                if (fieldName in initConsts[peerSym].orEmpty()) {
                    return PeerRefTypeResult(
                        null,
                        listOf(
                            OneLocCompileError(
                                loc,
                                "\"init:\" cannot use indexed access \"$peerSym[i].$fieldName\"; " +
                                    "const-globals are scalars (write $peerSym.$fieldName or a bare name)",
                            ),
                        ),
                    )
                }
                if (!vn.isConst) {
                    return PeerRefTypeResult(
                        null,
                        listOf(
                            OneLocCompileError(
                                loc,
                                "\"init:\" may only mention const state; \"$peerSym.$fieldName\" is a var",
                            ),
                        ),
                    )
                }
            }
            val t = try {
                valueView(vn.type)
            } catch (_: RuntimeException) {
                return PeerRefTypeResult(
                    null,
                    listOf(OneLocCompileError(baseExpr.programLocation(), "unresolved type for \"$peerSym.$fieldName\"")),
                )
            }
            val expected = typePassIndexedPeerParamTypes[peerSym]
            val typeErrs = mutableListOf<CompileError>()
            if (expected != null && idxErrors.isEmpty()) {
                try {
                    val actualIdx = baseExpr.index.getType()
                    if (typingView(actualIdx) != typingView(expected)) {
                        typeErrs += OneLocCompileError(
                            baseExpr.programLocation(),
                            "index type $actualIdx does not match parameter type $expected",
                        )
                    }
                } catch (_: RuntimeException) {
                }
            }
            return PeerRefTypeResult(t, idxErrors + typeErrs)
        }
        is SymbolValueExprNode -> {
            val pc = unindexedPeerClass(baseExpr.symbol) ?: return null
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
): List<CompileError> {
    // Leaf-spec peer read: Peer[idx].var or Peer.var
    if (typePassAllowSortDomains) {
        val peerRef = peerStateRefType(baseExpr, fieldName, symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
        if (peerRef != null) {
            if (peerRef.errors.isNotEmpty()) return peerRef.errors
            setInferredType(TypePassType.Inferred(peerRef.type!!))
            return emptyList()
        }
    }
    val baseErrors = baseExpr.typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
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
): List<CompileError> {
    val kind = collectionMethodKind(methodName)
        ?: return listOf(
            OneLocCompileError(
                programLocation(),
                "Unknown method \"$methodName\"",
            ),
        )
    if (typePassForbidEffects && kind == CollectionMethodKind.Fold) {
        return listOf(
            OneLocCompileError(
                programLocation(),
                "Method \"fold\" cannot be used in a spec formula",
            ),
        )
    }
    val baseErrors = baseExpr.typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
    if (baseErrors.isNotEmpty()) {
        return baseErrors
    }
    val collType = try {
        baseExpr.getType()
    } catch (_: RuntimeException) {
        return listOf(OneLocCompileError(programLocation(), "Cannot resolve type of method-call receiver"))
    }
    return when (kind) {
        CollectionMethodKind.Filter -> typePassFilter(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, collType)
        CollectionMethodKind.Map -> typePassMapMethod(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, collType)
        CollectionMethodKind.AssociateWith -> typePassAssociateWith(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, collType)
        CollectionMethodKind.Fold -> typePassFold(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, collType)
        CollectionMethodKind.ToSet -> typePassToSet(collType)
        CollectionMethodKind.ToList -> typePassToList(collType)
    }
}

private fun MethodCallExprNode.typePassToSet(collType: Type): List<CompileError> {
    val listTy = collType as? ListType
        ?: return listOf(
            OneLocCompileError(
                programLocation(),
                "Expected receiver of \"toSet\" to have a List type but got $collType",
            ),
        )
    if (args.isNotEmpty()) {
        return listOf(
            OneLocCompileError(
                programLocation(),
                "Expected method \"toSet\" to take 0 argument(s) but got ${args.size}",
            ),
        )
    }
    setInferredType(TypePassType.Inferred(setType(listTy.elementType)))
    return emptyList()
}

private fun MethodCallExprNode.typePassToList(collType: Type): List<CompileError> {
    val setTy = collType as? SetType
        ?: return listOf(
            OneLocCompileError(
                programLocation(),
                "Expected receiver of \"toList\" to have a Set type but got $collType",
            ),
        )
    if (args.isNotEmpty()) {
        return listOf(
            OneLocCompileError(
                programLocation(),
                "Expected method \"toList\" to take 0 argument(s) but got ${args.size}",
            ),
        )
    }
    setInferredType(TypePassType.Inferred(listType(setTy.elementType)))
    return emptyList()
}

private fun MethodCallExprNode.typePassFilter(
    symbolEnv: Map<String, Type>,
    registry: ObjClassRegistry,
    funEnv: Map<String, FunNode>,
    typeParamEnv: Map<String, Type>,
    funBuiltinEnv: Map<String, FunBuiltin>,
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
        symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv,
        args[0], elemType, expectedBodyType = boolType, resultType = resultType,
    )
}

private fun MethodCallExprNode.typePassMapMethod(
    symbolEnv: Map<String, Type>,
    registry: ObjClassRegistry,
    funEnv: Map<String, FunNode>,
    typeParamEnv: Map<String, Type>,
    funBuiltinEnv: Map<String, FunBuiltin>,
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
        symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv,
        args[0], elemType, expectedBodyType = null, resultType = null, mapCollType = collType,
    )
}

private fun MethodCallExprNode.typePassAssociateWith(
    symbolEnv: Map<String, Type>,
    registry: ObjClassRegistry,
    funEnv: Map<String, FunNode>,
    typeParamEnv: Map<String, Type>,
    funBuiltinEnv: Map<String, FunBuiltin>,
    collType: Type,
): List<CompileError> {
    val setTy = collType as? SetType
        ?: return listOf(
            OneLocCompileError(
                programLocation(),
                "Expected receiver of \"associateWith\" to have a Set type but got $collType",
            ),
        )
    if (args.size != 1) {
        return listOf(
            OneLocCompileError(
                programLocation(),
                "Expected method \"associateWith\" to take 1 argument(s) but got ${args.size}",
            ),
        )
    }
    return typePassUnaryHofArg(
        symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv,
        args[0], setTy.elementType, expectedBodyType = null, resultType = null,
        mapCollType = setTy, toMapFromSet = true,
    )
}

private fun MethodCallExprNode.typePassFold(
    symbolEnv: Map<String, Type>,
    registry: ObjClassRegistry,
    funEnv: Map<String, FunNode>,
    typeParamEnv: Map<String, Type>,
    funBuiltinEnv: Map<String, FunBuiltin>,
    collType: Type,
): List<CompileError> {
    val elemType = collectionElementType(collType)
        ?: return listOf(
            OneLocCompileError(programLocation(), "Expected receiver of \"fold\" to have a List or Set type but got $collType"),
        )
    if (args.size != 2) {
        return listOf(OneLocCompileError(programLocation(), "Expected method \"fold\" to take 2 argument(s) but got ${args.size}"))
    }
    val initErrors = args[0].typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
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
    val bodyErrors = lambda.body.typePass(bodyEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
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
    funArg: ExprNode,
    elemType: Type,
    expectedBodyType: Type?,
    resultType: Type?,
    mapCollType: Type? = null,
    toMapFromSet: Boolean = false,
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
            val bodyErrors = funArg.body.typePass(bodyEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
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
                toMapFromSet -> {
                    val st = mapCollType as? SetType
                        ?: return listOf(
                            OneLocCompileError(programLocation(), "associateWith expected a Set receiver"),
                        )
                    mapType(st.elementType, bodyType)
                }
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
                        toMapFromSet -> {
                            val st = mapCollType as? SetType
                                ?: return listOf(
                                    OneLocCompileError(programLocation(), "associateWith expected a Set receiver"),
                                )
                            mapType(st.elementType, ret.type)
                        }
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

private fun QuantifiedExprNode.typePassQuantified(
    symbolEnv: Map<String, Type>,
    registry: ObjClassRegistry,
    funEnv: Map<String, FunNode>,
    typeParamEnv: Map<String, Type>,
    funBuiltinEnv: Map<String, FunBuiltin>,
): List<CompileError> {
    val resolved = registry.resolveTypeExpr(binderTypeExpr(), typeParamEnv, programLocation())
    val binderType = when (resolved) {
        is TypeResolveResult.Error -> {
            return listOf(
                OneLocCompileError(
                    programLocation(),
                    "unknown type \"${binderTypeExpr()}\" in quantifier",
                ),
            )
        }
        is TypeResolveResult.Found -> resolved.type
    }
    sortDomainBan(binderType, programLocation())?.let { return listOf(it) }
    val bodyType = when (binderType) {
        is DomainType -> binderType.carrierType
        else -> binderType
    }
    val bodyErrors = quantifiedBody().typePass(
        symbolEnv + (binderName() to bodyType),
        registry,
        funEnv,
        typeParamEnv,
        funBuiltinEnv,
    )
    if (bodyErrors.isNotEmpty()) return bodyErrors
    try {
        if (quantifiedBody().getType() !is BoolType) {
            return listOf(
                OneLocCompileError(
                    programLocation(),
                    "quantifier body must be Boolean but got ${quantifiedBody().getType()}",
                ),
            )
        }
    } catch (_: RuntimeException) {
        return listOf(OneLocCompileError(programLocation(), "quantifier body must be Boolean"))
    }
    setInferredType(TypePassType.Inferred(boolType))
    return emptyList()
}

private fun ExprNode.typePassExpr(symbolEnv: Map<String, Type>, registry: ObjClassRegistry, funEnv: Map<String, FunNode>, typeParamEnv: Map<String, Type>,
    funBuiltinEnv: Map<String, FunBuiltin>): List<CompileError> {
    val childrenErrors = children.flatMap { it.typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv) }
    if (childrenErrors.isNotEmpty()) {
        return childrenErrors
    }
    try {
        inferExprType(symbolEnv)
    } catch (e: RuntimeException) {
        return listOf(OneLocCompileError(programLocation(), e.message ?: "Type error"))
    }
    return emptyList()
}

private fun SymbolValueExprNode.typePassSymbol(
    symbolEnv: Map<String, Type>,
    registry: ObjClassRegistry,
    funEnv: Map<String, FunNode>,
    typeParamEnv: Map<String, Type>,
    funBuiltinEnv: Map<String, FunBuiltin>,
): List<CompileError> {
    if (symbol.isDiscardBinding()) {
        return listOf(
            OneLocCompileError(programLocation(), "Cannot reference discard binding \"_\""),
        )
    }
    if (symbol !in symbolEnv) {
        if (typePassAllowSortDomains) {
            registry.domains[symbol]?.let { domain ->
                setInferredType(TypePassType.Inferred(domain))
                return emptyList()
            }
        }
        if (typePassInitConstGlobals != null) {
            return listOf(
                OneLocCompileError(
                    programLocation(),
                    "unbound symbol \"$symbol\" in init: (use a const-global name, a sort name, or Sort.length)",
                ),
            )
        }
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
    val collErrors = collArg.typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
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
        val bodyErrors = funArg.body.typePass(bodyEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
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
): List<CompileError> {
    if (typePassAllowSortDomains && callName() == "length" && callArgs().size == 1) {
        val arg = callArgs().single()
        if (arg is SymbolValueExprNode && arg.symbol !in symbolEnv &&
            registry.domains.containsKey(arg.symbol)
        ) {
            arg.setInferredType(TypePassType.Inferred(registry.domains.getValue(arg.symbol)))
            setInferredType(TypePassType.Inferred(intType))
            return emptyList()
        }
    }
    funBuiltinEnv[callName()]?.let { builtin ->
        if (builtin.namedFunArg) {
            return typePassNamedFunMap(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv, builtin)
        }
    }
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
        if (typePassForbidEffects && (builtin.ioHavoc || builtin.transitionOnly || builtin.sessionPeerClassArg)) {
            return listOf(
                OneLocCompileError(
                    programLocation(),
                    "Function \"${callName()}\" cannot be used in a spec formula",
                ),
            )
        }
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
                // Re-type the inlined body under the concrete type substitution so obj
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
                arg.resolveArgType(typingView(result.type))
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
            resolveReturnType(typingView(result.type))
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
    when (expr) {
        is ListLiteralExprNode ->
            if (expr.elements.isEmpty() && expected is ListType) expr.resolveListType(expected)
        is SetLiteralExprNode ->
            if (expr.elements.isEmpty() && expected is SetType) expr.resolveSetType(expected)
        is MapLiteralExprNode ->
            if (expr.entries.isEmpty() && expected is MapType) expr.resolveMapType(expected)
        is ParenExprNode -> applyExpectedCollectionType(expr.innerExpr(), expected)
        is IfElseExprNode -> {
            applyExpectedCollectionType(expr.thenExpr(), expected)
            applyExpectedCollectionType(expr.elseExpr(), expected)
        }
        is LetExprNode -> applyExpectedCollectionType(expr.bodyExpr(), expected)
        is WhenExprNode -> expr.arms().forEach { arm ->
            when (arm) {
                is WhenArm.Subject -> applyExpectedCollectionType(arm.expr, expected)
                is WhenArm.Guard -> applyExpectedCollectionType(arm.expr, expected)
                is WhenArm.Else -> applyExpectedCollectionType(arm.expr, expected)
            }
        }
    }
}

private fun resolveLiteralTypeArg(
    typeExpr: TypeExpr,
    registry: ObjClassRegistry,
    typeParamEnv: Map<String, Type>,
    loc: ProgramLoc,
): Pair<Type?, CompileError?> =
    when (val result = registry.resolveTypeExpr(typeExpr, typeParamEnv, loc)) {
        is TypeResolveResult.Found -> typingView(result.type) to null
        is TypeResolveResult.Error -> null to OneLocCompileError(loc, result.message)
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
): List<CompileError> {
    requireCollectionFunlib("listOf", funBuiltinEnv, programLocation())?.let { return listOf(it) }
    val childErrors = elements.flatMap { it.typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv) }
    if (childErrors.isNotEmpty()) {
        return childErrors
    }
    if (typeArgs.isNotEmpty()) {
        if (typeArgs.size != 1) {
            return listOf(
                OneLocCompileError(
                    programLocation(),
                    "listOf type annotation expects 1 type argument but got ${typeArgs.size}",
                ),
            )
        }
        val (elemTy, err) = resolveLiteralTypeArg(typeArgs[0], registry, typeParamEnv, programLocation())
        if (err != null) return listOf(err)
        val annotated = listType(elemTy!!)
        resolvedListTypeOrNull()?.let { existing ->
            if (existing != annotated) {
                return listOf(
                    OneLocCompileError(
                        programLocation(),
                        "listOf type annotation $annotated does not match target type $existing",
                    ),
                )
            }
        }
        resolveListType(annotated)
    }
    if (elements.isEmpty()) {
        if (resolvedListTypeOrNull() != null) {
            inferExprType(symbolEnv)
            return emptyList()
        }
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
    val resolved = resolvedListTypeOrNull()
    if (resolved != null) {
        if (elemType != resolved.elementType) {
            return listOf(
                OneLocCompileError(
                    programLocation(),
                    "Expected list elements to have type ${resolved.elementType} but got $elemType",
                ),
            )
        }
    } else {
        resolveListType(listType(elemType))
    }
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
    if (typePassSpecFormula) {
        val baseSym = (base as? SymbolValueExprNode)?.symbol
        if (baseSym != null && baseSym !in symbolEnv && isSpecPeerName(baseSym)) {
            val msg = if (typePassInitConstGlobals != null) {
                "\"init:\" cannot index component \"$baseSym\"; const-globals are unindexed"
            } else {
                "indexed component \"$baseSym\" requires a state variable: \"$baseSym[i].var\""
            }
            return listOf(OneLocCompileError(programLocation(), msg))
        }
    }
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

private fun IndexTransitNode.typePassIndexTransit(
    symbolEnv: Map<String, Type>,
    registry: ObjClassRegistry,
    funEnv: Map<String, FunNode>,
    typeParamEnv: Map<String, Type>,
    funBuiltinEnv: Map<String, FunBuiltin>,
    rhsEnv: Map<String, Type> = symbolEnv,
): List<CompileError> {
    // Index assignment targets are always process state (never action args).
    val lhsEnv = typePassStateEnv ?: symbolEnv
    val collectionType = lhsEnv[collectionVar]
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
    val childErrors = children.flatMap { it.typePass(rhsEnv, registry, funEnv, typeParamEnv, funBuiltinEnv) }
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
): List<CompileError> {
    requireCollectionFunlib("setOf", funBuiltinEnv, programLocation())?.let { return listOf(it) }
    val childErrors = elements.flatMap { it.typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv) }
    if (childErrors.isNotEmpty()) {
        return childErrors
    }
    if (typeArgs.isNotEmpty()) {
        if (typeArgs.size != 1) {
            return listOf(
                OneLocCompileError(
                    programLocation(),
                    "setOf type annotation expects 1 type argument but got ${typeArgs.size}",
                ),
            )
        }
        val (elemTy, err) = resolveLiteralTypeArg(typeArgs[0], registry, typeParamEnv, programLocation())
        if (err != null) return listOf(err)
        val annotated = setType(elemTy!!)
        resolvedSetTypeOrNull()?.let { existing ->
            if (existing != annotated) {
                return listOf(
                    OneLocCompileError(
                        programLocation(),
                        "setOf type annotation $annotated does not match target type $existing",
                    ),
                )
            }
        }
        resolveSetType(annotated)
    }
    if (elements.isEmpty()) {
        if (resolvedSetTypeOrNull() != null) {
            inferExprType(symbolEnv)
            return emptyList()
        }
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
    val resolved = resolvedSetTypeOrNull()
    if (resolved != null) {
        if (elemType != resolved.elementType) {
            return listOf(
                OneLocCompileError(
                    programLocation(),
                    "Expected set elements to have type ${resolved.elementType} but got $elemType",
                ),
            )
        }
    } else {
        resolveSetType(setType(elemType))
    }
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
    requireCollectionFunlib("mapOf", funBuiltinEnv, programLocation())?.let { return listOf(it) }
    val childErrors = entries.flatMap { (k, v) ->
        k.typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv) +
            v.typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
    }
    if (childErrors.isNotEmpty()) {
        return childErrors
    }
    if (typeArgs.isNotEmpty()) {
        if (typeArgs.size != 2) {
            return listOf(
                OneLocCompileError(
                    programLocation(),
                    "mapOf type annotation expects 2 type arguments but got ${typeArgs.size}",
                ),
            )
        }
        val (keyTy, keyErr) = resolveLiteralTypeArg(typeArgs[0], registry, typeParamEnv, programLocation())
        if (keyErr != null) return listOf(keyErr)
        val (valTy, valErr) = resolveLiteralTypeArg(typeArgs[1], registry, typeParamEnv, programLocation())
        if (valErr != null) return listOf(valErr)
        val annotated = mapType(keyTy!!, valTy!!)
        resolvedMapTypeOrNull()?.let { existing ->
            if (existing != annotated) {
                return listOf(
                    OneLocCompileError(
                        programLocation(),
                        "mapOf type annotation $annotated does not match target type $existing",
                    ),
                )
            }
        }
        resolveMapType(annotated)
    }
    if (entries.isEmpty()) {
        if (resolvedMapTypeOrNull() != null) {
            inferExprType(symbolEnv)
            return emptyList()
        }
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
    val resolved = resolvedMapTypeOrNull()
    if (resolved != null) {
        if (keyType != resolved.keyType) {
            return listOf(
                OneLocCompileError(
                    programLocation(),
                    "Expected map keys to have type ${resolved.keyType} but got $keyType",
                ),
            )
        }
        if (valueType != resolved.valueType) {
            return listOf(
                OneLocCompileError(
                    programLocation(),
                    "Expected map values to have type ${resolved.valueType} but got $valueType",
                ),
            )
        }
    } else {
        resolveMapType(mapType(keyType, valueType))
    }
    inferExprType(symbolEnv)
    return emptyList()
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
    val paramEnv = params.filter { !it.name.isDiscardBinding() }.associate { it.name to it.type }
    val typeParamEnv: Map<String, Type> = typeParams.associateWith { TypeVar(it) }
    val bodyErrors = funBody().typePass(paramEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
    if (bodyErrors.isNotEmpty()) {
        return bodyErrors
    }
    val expectedReturn = typingView(returnType)
    return assertOrCompileError(
        funBody().getType() == expectedReturn,
        OneLocCompileError(
            funBody().programLocation(),
            "Expected function \"${name()}\" to return $returnType but body has type ${funBody().getType()}",
        ),
    )
}

private fun ReturnActionNode.typePassReturnAction(
    symbolEnv: Map<String, Type>,
    registry: ObjClassRegistry,
    funEnv: Map<String, FunNode>,
    typeParamEnv: Map<String, Type>,
    funBuiltinEnv: Map<String, FunBuiltin>,
): List<CompileError> {
    val argErrors = returnArgs().typePass(symbolEnv, registry, funEnv, typeParamEnv, funBuiltinEnv)
    val actionEnv = symbolEnv + returnArgs().argsTypeMap().mapValues { (_, t) -> typingView(t) } +
        returnArgs().actionArgs().filter { !it.name.isDiscardBinding() }
            .associate { it.name to typingView(it.type) }
    return argErrors + body().flatMap { it.typePass(actionEnv, registry, funEnv, typeParamEnv, funBuiltinEnv) }
}

/** `A.snd ~> A.rcv`: both actions must exist on a dynamic proc class. */
private fun HandlerRefExprNode.typePassHandlerRef(): List<CompileError> {
    setInferredType(TypePassType.Inferred(HandlerRefType))
    if (handlerClass() != handlerRespClass()) {
        return listOf(
            OneLocCompileError(
                programLocation(),
                "Handler \"$this\" must name the same proc class on both sides of ~>",
            ),
        )
    }
    val pc = typePassPeerClasses[handlerClass()]
        ?: return listOf(OneLocCompileError(programLocation(), "Unknown proc class \"${handlerClass()}\" in handler"))
    val ctor = pc.localDecls().filterIsInstance<ConstructorNode>().firstOrNull { it.constructorName() == handlerCtor() }
        ?: return listOf(
            OneLocCompileError(
                programLocation(),
                "Handler request \"${handlerClass()}.${handlerCtor()}\" must be a constructor of \"${handlerClass()}\"",
            ),
        )
    fun argTypeNames(args: ArgsNode): List<String> =
        generateSequence(listOf<ASTNode>(args)) { level -> level.flatMap { it.children }.ifEmpty { null } }
            .flatten().filterIsInstance<ArgNode>().map { it.argTypeName() }.toList()
    val rcvArgs = pc.localDecls().firstNotNullOfOrNull { d ->
        when {
            d is TransitionNode && d.transitionName() == handlerAction() -> argTypeNames(d.transitionArgs())
            d is ReturnActionNode && d.returnActionName() == handlerAction() -> argTypeNames(d.returnArgs())
            else -> null
        }
    }
        ?: return listOf(
            OneLocCompileError(
                programLocation(),
                "Handler response \"${handlerClass()}.${handlerAction()}\" must be an action of \"${handlerClass()}\"",
            ),
        )
    val errors = mutableListOf<CompileError>()
    val reqArgs = argTypeNames(ctor.constructorArgs())
    if (reqArgs.size != 1 || reqArgs[0] != "HttpServerRequest") {
        errors += OneLocCompileError(
            programLocation(),
            "Handler request \"${handlerClass()}.${handlerCtor()}\" must take exactly one HttpServerRequest argument",
        )
    }
    if (rcvArgs.size != 1 || rcvArgs[0] != "HttpServerResponse") {
        errors += OneLocCompileError(
            programLocation(),
            "Handler response \"${handlerClass()}.${handlerAction()}\" must take exactly one HttpServerResponse argument",
        )
    }
    return errors
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
