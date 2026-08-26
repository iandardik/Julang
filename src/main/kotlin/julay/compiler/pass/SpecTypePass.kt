package julay.compiler.pass

import julay.compiler.*
import julay.compiler.ast.*
import julay.compiler.decl.*
import julay.program.type.*

data class SpecTypePassResult(
    val errors: List<CompileError>,
    val warnings: List<CompileWarning> = emptyList(),
)

fun RootNode.specTypePass(
    unit: CompilationUnit,
    allowUnindexedSpec: Boolean = false,
): SpecTypePassResult {
    val registry = cachedObjClassRegistry() ?: return SpecTypePassResult(emptyList())
    val invariants = unit.modules
        .flatMap { it.root.declNodes().filterIsInstance<InvariantNode>() }
        .associateBy { it.name() }
    val funEnvByInv = mutableMapOf<String, Map<String, FunNode>>()
    val funBuiltinEnvByInv = mutableMapOf<String, Map<String, FunBuiltin>>()
    unit.modules.forEach { mod ->
        val env = callableFuns(mod)
        val builtins = callableFunBuiltins(mod)
        mod.root.declNodes().filterIsInstance<InvariantNode>().forEach { inv ->
            funEnvByInv[inv.name()] = env
            funBuiltinEnvByInv[inv.name()] = builtins
        }
    }
    val leafSpecNodes = unit.modules
        .flatMap { it.root.declNodes().filterIsInstance<LeafSpecNode>() }
        .associateBy { it.name() }
    val pclassNodes = unit.modules
        .flatMap { it.root.declNodes().filterIsInstance<ProcClassNode>() }
        .associateBy { it.name() } + leafSpecNodes.mapValues { it.value.asProcClass() }
    val procAliases = unit.modules
        .flatMap { it.root.declNodes().filterIsInstance<ProcNode>() }
        .associateBy { it.name() }
    val specAliases = unit.modules
        .flatMap { it.root.declNodes().filterIsInstance<SpecNode>() }
        .associateBy { it.name() }
    val apiAliases = unit.modules
        .flatMap { it.root.declNodes().filterIsInstance<ApiNode>() }
        .associateBy { it.name() }

    val errors = mutableListOf<CompileError>()
    val warnings = mutableListOf<CompileWarning>()
    unit.modules.forEach { mod ->
        val moduleFunEnv = callableFuns(mod)
        val moduleFunBuiltinEnv = callableFunBuiltins(mod)
        mod.root.declNodes().filterIsInstance<SpecNode>().forEach { spec ->
            val result = typePassSpec(
                spec,
                invariants,
                pclassNodes,
                leafSpecNodes,
                procAliases,
                specAliases,
                apiAliases,
                unit,
                registry,
                allowUnindexedSpec,
                funEnvByInv,
                moduleFunEnv,
                funBuiltinEnvByInv,
                moduleFunBuiltinEnv,
            )
            errors += result.errors
            warnings += result.warnings
        }
    }
    return SpecTypePassResult(errors, warnings)
}

private fun ProcClassNode.isInitiallyOnly(): Boolean {
    val ctors = localDecls().flatMap { it.constructors() }
    return ctors.isNotEmpty() && ctors.all { it.action.name == "initially" }
}

private fun resolveType(
    registry: ObjClassRegistry,
    typeExpr: TypeExpr,
): Type? = when (val r = registry.resolveTypeExpr(typeExpr, emptyMap())) {
    is TypeResolveResult.Found -> r.type
    is TypeResolveResult.Error -> null
}

private fun typePassSpec(
    spec: SpecNode,
    invariants: Map<String, InvariantNode>,
    pclassNodes: Map<String, ProcClassNode>,
    leafSpecNodes: Map<String, LeafSpecNode>,
    procAliases: Map<String, ProcNode>,
    specAliases: Map<String, SpecNode>,
    apiAliases: Map<String, ApiNode>,
    unit: CompilationUnit,
    registry: ObjClassRegistry,
    allowUnindexedSpec: Boolean,
    funEnvByInv: Map<String, Map<String, FunNode>>,
    moduleFunEnv: Map<String, FunNode>,
    funBuiltinEnvByInv: Map<String, Map<String, FunBuiltin>>,
    moduleFunBuiltinEnv: Map<String, FunBuiltin>,
): SpecTypePassResult {
    val errors = mutableListOf<CompileError>()
    val warnings = mutableListOf<CompileWarning>()
    val value = spec.specNodeValue()

    fun annotateDeclParams(leaves: List<SpecLeaf>): List<SpecLeaf> =
        leaves.map { leaf ->
            val ls = leafSpecNodes[leaf.name] ?: return@map leaf
            if (!ls.isParameterized()) return@map leaf
            // Leaf-decl [n : T] is a body binder only — do not lift state via SpecLeaf params.
            // Create-index Name[n : T] may still parameterize the leaf; check domain compatibility.
            if (leaf.isParameterized) {
                val declType = ls.leafSpecParamType()
                val useType = leaf.paramType
                if (declType != null && useType != null &&
                    resolveType(registry, declType) != null &&
                    resolveType(registry, useType) != null &&
                    resolveType(registry, declType) != resolveType(registry, useType)
                ) {
                    errors += OneLocCompileError(
                        spec.programLocation(),
                        "leaf spec \"${leaf.name}\" is parameterized as [${ls.leafSpecParamName()} : $declType] " +
                            "but re-indexed with incompatible type $useType",
                    )
                }
            }
            leaf
        }

    fun checkLeaves(leaves: List<SpecLeaf>, role: String) {
        val procFunNames = collectProcFunNames(unit.root)
        leaves.forEach { leaf ->
            val known = leaf.name in pclassNodes ||
                leaf.name in leafSpecNodes ||
                leaf.name in unit.allPClassNames ||
                leaf.name in unit.allLeafSpecNames ||
                leaf.name in unit.allProcNames ||
                leaf.name in unit.entryDeclNames ||
                leaf.name in procFunNames ||
                unit.importTable.shortNames.containsKey(leaf.name)
            if (!known) {
                errors += OneLocCompileError(
                    spec.programLocation(),
                    "unknown $role component \"${leaf.name}\"",
                )
            }
            if (leaf.paramType != null && resolveType(registry, leaf.paramType) == null) {
                errors += OneLocCompileError(
                    spec.programLocation(),
                    "unknown parameter type \"${leaf.paramType}\" on ${leaf.name}",
                )
            }
        }
    }

    fun checkIndexing(leaves: List<SpecLeaf>) {
        val expanded = expandLeavesToPclasses(
            leaves,
            pclassNodes,
            procAliases,
            specAliases,
            apiAliases,
            leafSpecNodes,
        )
        expanded.forEach { leaf ->
            val ls = leafSpecNodes[leaf.name]
            if (ls != null) {
                // Leaf specs are never required to be create-indexed (decl params do not lift
                // state; singleton / global-state stubs are normal).
                return@forEach
            }
            val pc = pclassNodes[leaf.name] ?: return@forEach
            if (pc.isInitiallyOnly()) {
                if (leaf.isParameterized) {
                    warnings += OneLocCompileWarning(
                        spec.programLocation(),
                        "proc \"${leaf.name}\" only has constructor initially, so indexing is unnecessary",
                    )
                }
            } else if (!leaf.isParameterized) {
                val msg =
                    "proc \"${leaf.name}\" can have multiple instances and must be indexed in this spec " +
                        "(e.g. ${leaf.name}[i : Type]); pass --allow-unindexed-spec to warn instead"
                if (allowUnindexedSpec) {
                    warnings += OneLocCompileWarning(spec.programLocation(), msg)
                } else {
                    errors += OneLocCompileError(spec.programLocation(), msg)
                }
            }
        }
    }

    when (value) {
        is AgSpecExprNode -> {
            errors += withIndexStructureErrors(value)
            val assumeLeaves = annotateDeclParams(flattenSpecLeaves(value.assumeExpr()))
            val systemLeaves = annotateDeclParams(flattenSpecLeaves(value.systemExpr()))
            checkLeaves(assumeLeaves, "assumption")
            checkLeaves(systemLeaves, "system")
            checkIndexing(assumeLeaves)
            checkIndexing(systemLeaves)
            errors += globalDeclErrors(value, pclassNodes, leafSpecNodes, procAliases, specAliases, apiAliases)
            errors += initClauseErrors(
                value, pclassNodes, leafSpecNodes, procAliases, specAliases, apiAliases, registry,
                moduleFunEnv, moduleFunBuiltinEnv,
            )

            val guarantee = value.guaranteeExpr()
            if (guarantee != null) {
                val expandedSystem = expandLeavesToPclasses(
                    systemLeaves,
                    pclassNodes,
                    procAliases,
                    specAliases,
                    apiAliases,
                    leafSpecNodes,
                )
                val systemPclasses = expandedSystem.mapNotNull { leaf ->
                    pclassNodes[leaf.name]?.let { leaf.name to it }
                }.toMap()
                when {
                    guarantee is SymbolValueExprNode && invariants.containsKey(guarantee.symbol) -> {
                        errors += typePassInvariantNamed(
                            guarantee.symbol,
                            invariants,
                            expandedSystem,
                            systemPclasses,
                            registry,
                            funEnvByInv,
                            moduleFunEnv,
                            funBuiltinEnvByInv,
                            moduleFunBuiltinEnv,
                        )
                    }
                    else -> {
                        errors += typePassInvariantFormula(
                            guarantee,
                            expandedSystem,
                            systemPclasses,
                            registry,
                            invariants,
                            checking = mutableSetOf(),
                            checked = mutableSetOf(),
                            funEnvByInv,
                            moduleFunEnv,
                            funBuiltinEnvByInv,
                            moduleFunBuiltinEnv,
                            guarantee.programLocation(),
                        )
                    }
                }
            }
        }
        else -> {
            errors += withIndexStructureErrors(value)
            val systemLeaves = annotateDeclParams(flattenSpecLeaves(value))
            checkLeaves(systemLeaves, "system")
            checkIndexing(systemLeaves)
            errors += globalDeclErrors(value, pclassNodes, leafSpecNodes, procAliases, specAliases, apiAliases)
            errors += initClauseErrors(
                value, pclassNodes, leafSpecNodes, procAliases, specAliases, apiAliases, registry,
                moduleFunEnv, moduleFunBuiltinEnv,
            )
        }
    }
    return SpecTypePassResult(errors, warnings)
}

private val SYNTHETIC_GLOBAL_NAMES = setOf("constructed", "killed", "terminated")

private fun declaredStateVars(
    leafName: String,
    pclassNodes: Map<String, ProcClassNode>,
    leafSpecNodes: Map<String, LeafSpecNode>,
): Map<String, VarNode> {
    val pc = pclassNodes[leafName] ?: leafSpecNodes[leafName]?.asProcClass() ?: return emptyMap()
    return pc.localDecls().filterIsInstance<VarNode>().associateBy { it.name }
}

private fun declaredStateNames(
    leafName: String,
    pclassNodes: Map<String, ProcClassNode>,
    leafSpecNodes: Map<String, LeafSpecNode>,
): Set<String> = declaredStateVars(leafName, pclassNodes, leafSpecNodes).keys

private fun globalDeclErrors(
    node: ASTNode,
    pclassNodes: Map<String, ProcClassNode>,
    leafSpecNodes: Map<String, LeafSpecNode>,
    procAliases: Map<String, ProcNode>,
    specAliases: Map<String, SpecNode>,
    apiAliases: Map<String, ApiNode>,
): List<CompileError> {
    val errors = mutableListOf<CompileError>()
    fun walk(n: ASTNode) {
        if (n is ParamProcExprNode && !n.isApplyIndex() && n.globalVarNames().isNotEmpty()) {
            val names = n.globalVarNames()
            val seen = mutableSetOf<String>()
            names.forEach { name ->
                if (!seen.add(name)) {
                    errors += OneLocCompileError(
                        n.programLocation(),
                        "duplicate global variable \"$name\"",
                    )
                }
                if (name in SYNTHETIC_GLOBAL_NAMES) {
                    errors += OneLocCompileError(
                        n.programLocation(),
                        "cannot mark synthetic variable \"$name\" as global",
                    )
                }
            }
            val unique = names.distinct().filter { it !in SYNTHETIC_GLOBAL_NAMES }
            if (unique.isNotEmpty()) {
                val expanded = expandLeavesToPclasses(
                    flattenSpecLeaves(n),
                    pclassNodes,
                    procAliases,
                    specAliases,
                    apiAliases,
                    leafSpecNodes,
                )
                val declared = expanded.flatMap { leaf ->
                    declaredStateNames(leaf.name, pclassNodes, leafSpecNodes)
                }.toSet()
                unique.forEach { name ->
                    if (name !in declared) {
                        errors += OneLocCompileError(
                            n.programLocation(),
                            "global \"$name\" is not a state variable of any indexed leaf in this spec",
                        )
                    }
                }
                n.globalDecls().forEach { decl ->
                    decl.names.distinct().forEach { name ->
                        if (name in SYNTHETIC_GLOBAL_NAMES || name !in declared) return@forEach
                        val vars = expanded.mapNotNull { leaf ->
                            declaredStateVars(leaf.name, pclassNodes, leafSpecNodes)[name]
                        }
                        if (!decl.isConst && vars.any { it.isConst }) {
                            errors += OneLocCompileError(
                                decl.loc,
                                "\"$name\" may change without declaring it \"const global $name\", so either make it \"const global $name\" or change the state var to be \"var $name\" instead of \"const $name\"",
                            )
                        }
                        if (decl.isConst && vars.any { !it.isConst }) {
                            errors += OneLocCompileError(
                                decl.loc,
                                "\"$name\" is a var, so either drop \"const\" (use \"global $name\") or declare \"const $name\" on the proc instead of \"var $name\"",
                            )
                        }
                    }
                }
            }
        }
        n.children.forEach { walk(it) }
    }
    walk(node)
    return errors
}

private fun typePassInvariantNamed(
    invName: String,
    invariants: Map<String, InvariantNode>,
    systemLeaves: List<SpecLeaf>,
    pclasses: Map<String, ProcClassNode>,
    registry: ObjClassRegistry,
    funEnvByInv: Map<String, Map<String, FunNode>>,
    defaultFunEnv: Map<String, FunNode>,
    funBuiltinEnvByInv: Map<String, Map<String, FunBuiltin>>,
    defaultFunBuiltinEnv: Map<String, FunBuiltin>,
    checking: MutableSet<String> = mutableSetOf(),
    checked: MutableSet<String> = mutableSetOf(),
): List<CompileError> {
    if (invName in checked) return emptyList()
    val inv = invariants[invName]
        ?: error("internal: invariant \"$invName\" missing from map")
    if (invName in checking) {
        return listOf(
            OneLocCompileError(
                inv.programLocation(),
                "cyclic invariant reference involving \"$invName\"",
            ),
        )
    }
    checking += invName
    val errors = typePassInvariantFormula(
        inv.invariantFormula(),
        systemLeaves,
        pclasses,
        registry,
        invariants,
        checking,
        checked,
        funEnvByInv,
        funEnvByInv[invName] ?: defaultFunEnv,
        funBuiltinEnvByInv,
        funBuiltinEnvByInv[invName] ?: defaultFunBuiltinEnv,
        inv.programLocation(),
    )
    checking -= invName
    checked += invName
    return errors
}


private fun typePassInvariantFormula(
    formula: ExprNode,
    systemLeaves: List<SpecLeaf>,
    pclasses: Map<String, ProcClassNode>,
    registry: ObjClassRegistry,
    invariants: Map<String, InvariantNode>,
    checking: MutableSet<String>,
    checked: MutableSet<String>,
    funEnvByInv: Map<String, Map<String, FunNode>>,
    funEnv: Map<String, FunNode>,
    funBuiltinEnvByInv: Map<String, Map<String, FunBuiltin>>,
    funBuiltinEnv: Map<String, FunBuiltin>,
    loc: julay.compiler.ProgramLoc,
): List<CompileError> {
    val refs = collectInvariantRefs(formula, invariants.keys)
    val errors = mutableListOf<CompileError>()
    val env = mutableMapOf<String, Type>()
    refs.forEach { name ->
        errors += typePassInvariantNamed(
            name,
            invariants,
            systemLeaves,
            pclasses,
            registry,
            funEnvByInv,
            funEnv,
            funBuiltinEnvByInv,
            funBuiltinEnv,
            checking,
            checked,
        )
        env[name] = boolType
    }
    val (unindexed, indexed, paramTypes) = specPeerMaps(systemLeaves, pclasses, registry)
    errors += typePassSpecFormula(
        formula,
        env,
        registry,
        funEnv,
        funBuiltinEnv,
        unindexed,
        indexed,
        paramTypes,
    )
    if (errors.isEmpty()) {
        try {
            if (formula.getType() !is BoolType) {
                errors += OneLocCompileError(
                    loc,
                    "invariant formula must be Boolean but got ${formula.getType()}",
                )
            }
        } catch (_: RuntimeException) {
        }
    }
    return errors
}

private fun specPeerMaps(
    systemLeaves: List<SpecLeaf>,
    pclasses: Map<String, ProcClassNode>,
    registry: ObjClassRegistry,
): Triple<Map<String, ProcClassNode>, Map<String, ProcClassNode>, Map<String, Type>> {
    val unindexed = linkedMapOf<String, ProcClassNode>()
    val indexed = linkedMapOf<String, ProcClassNode>()
    val paramTypes = linkedMapOf<String, Type>()
    systemLeaves.forEach { leaf ->
        val pc = pclasses[leaf.name] ?: return@forEach
        if (leaf.isParameterized) {
            indexed[leaf.name] = pc
            leaf.paramType?.let { pt ->
                when (val r = registry.resolveTypeExpr(pt)) {
                    is TypeResolveResult.Found -> paramTypes[leaf.name] = r.type
                    is TypeResolveResult.Error -> {}
                }
            }
        } else {
            unindexed[leaf.name] = pc
        }
    }
    return Triple(unindexed, indexed, paramTypes)
}

private fun collectInvariantRefs(
    expr: ExprNode,
    invariantNames: Set<String>,
    bound: Set<String> = emptySet(),
): Set<String> = when (expr) {
    is QuantifiedExprNode ->
        collectInvariantRefs(expr.quantifiedBody(), invariantNames, bound + expr.binderName())
    is LetExprNode ->
        collectInvariantRefs(expr.letInitExpr(), invariantNames, bound) +
            collectInvariantRefs(expr.bodyExpr(), invariantNames, bound + expr.letName())
    is LambdaExprNode ->
        collectInvariantRefs(expr.body, invariantNames, bound + expr.params)
    is SymbolValueExprNode ->
        if (expr.symbol in invariantNames && expr.symbol !in bound) setOf(expr.symbol) else emptySet()
    else -> expr.children.filterIsInstance<ExprNode>().flatMap {
        collectInvariantRefs(it, invariantNames, bound)
    }.toSet()
}


/** Default TLC MaxListLen; `init:` length constraints larger than this make Init empty. */
private const val INIT_MAX_LIST_LEN = 3

private fun initClauseErrors(
    node: ASTNode,
    pclassNodes: Map<String, ProcClassNode>,
    leafSpecNodes: Map<String, LeafSpecNode>,
    procAliases: Map<String, ProcNode>,
    specAliases: Map<String, SpecNode>,
    apiAliases: Map<String, ApiNode>,
    registry: ObjClassRegistry,
    funEnv: Map<String, FunNode>,
    funBuiltinEnv: Map<String, FunBuiltin>,
): List<CompileError> {
    val errors = mutableListOf<CompileError>()
    fun walk(n: ASTNode) {
        if (n is ParamProcExprNode && !n.isApplyIndex() && n.initExprs().isNotEmpty()) {
            val expanded = expandLeavesToPclasses(
                flattenSpecLeaves(n),
                pclassNodes,
                procAliases,
                specAliases,
                apiAliases,
                leafSpecNodes,
            )
            val constNames = n.globalConstVarNames().toSet()
            val constTypes = linkedMapOf<String, Type>()
            constNames.forEach { name ->
                expanded.forEach { leaf ->
                    val vn = declaredStateVars(leaf.name, pclassNodes, leafSpecNodes)[name] ?: return@forEach
                    val t = try {
                        vn.type
                    } catch (_: RuntimeException) {
                        resolveType(registry, vn.typeExpr)
                    } ?: return@forEach
                    constTypes.putIfAbsent(name, t)
                }
            }
            val leafConst = expanded.associate { leaf ->
                leaf.name to leaf.globalConstVars
            }
            val unindexed = expanded.mapNotNull { leaf ->
                val pc = pclassNodes[leaf.name] ?: leafSpecNodes[leaf.name]?.asProcClass()
                pc?.let { leaf.name to it }
            }.toMap()
            val indexed = linkedMapOf<String, ProcClassNode>()
            val paramTypes = linkedMapOf<String, Type>()
            n.paramType()?.let { pt ->
                val indexType = when (val r = registry.resolveTypeExpr(pt)) {
                    is TypeResolveResult.Found -> r.type
                    is TypeResolveResult.Error -> null
                }
                if (indexType != null) {
                    expanded.forEach { leaf ->
                        val pc = pclassNodes[leaf.name] ?: leafSpecNodes[leaf.name]?.asProcClass() ?: return@forEach
                        indexed[leaf.name] = pc
                        paramTypes[leaf.name] = indexType
                    }
                }
            }
            n.initExprs().forEach { expr ->
                val initErrs = typePassSpecFormula(
                    expr,
                    constTypes,
                    registry,
                    funEnv,
                    funBuiltinEnv,
                    unindexed,
                    indexedPeers = indexed,
                    indexedParamTypes = paramTypes,
                    initConstGlobals = leafConst,
                )
                errors += initErrs
                if (initErrs.isEmpty()) {
                    try {
                        if (expr.getType() !is BoolType) {
                            errors += OneLocCompileError(
                                expr.programLocation(),
                                "\"init:\" constraint must be Boolean but got ${expr.getType()}",
                            )
                        }
                    } catch (_: RuntimeException) {
                    }
                    errors += sortLengthMaxListLenErrors(expr, registry.domains)
                }
            }
        }
        n.children.forEach { walk(it) }
    }
    walk(node)
    return errors
}

private fun sortLengthMaxListLenErrors(
    expr: ExprNode,
    domains: Map<String, DomainType>,
): List<CompileError> {
    val errors = mutableListOf<CompileError>()
    fun checkSort(name: String, loc: julay.compiler.ProgramLoc) {
        val domain = domains[name] ?: return
        val size = domain.cfgElements?.size ?: return
        if (size > INIT_MAX_LIST_LEN) {
            errors += OneLocCompileError(
                loc,
                "\"init:\" constraint uses type \"$name\" of size $size, " +
                    "which exceeds MaxListLen ($INIT_MAX_LIST_LEN)",
            )
        }
    }
    fun walk(e: ExprNode) {
        when (e) {
            is FieldAccessExprNode -> {
                if (e.fieldPath.lastOrNull() == "length") {
                    checkSort(e.baseSymbol, e.programLocation())
                }
            }
            is FunCallExprNode -> {
                if (e.callName() == "length") {
                    val arg = e.callArgs().singleOrNull()
                    if (arg is SymbolValueExprNode) {
                        checkSort(arg.symbol, e.programLocation())
                    }
                }
                e.callArgs().forEach { walk(it) }
                return
            }
            else -> {}
        }
        e.children.filterIsInstance<ExprNode>().forEach { walk(it) }
    }
    walk(expr)
    return errors
}

