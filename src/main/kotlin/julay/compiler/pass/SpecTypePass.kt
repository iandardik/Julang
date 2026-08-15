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
    val invariants = declNodes().filterIsInstance<InvariantNode>().associateBy { it.name() }
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
    declNodes().filterIsInstance<SpecNode>().forEach { spec ->
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
        )
        errors += result.errors
        warnings += result.warnings
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
): List<CompileError> {
    val leafByName = systemLeaves.associateBy { it.name }
    val errors = mutableListOf<CompileError>()

    fun stateVarType(leafName: String, varName: String): Type? {
        val pc = pclasses[leafName] ?: return null
        val vn = pc.localDecls().filterIsInstance<VarNode>().firstOrNull { it.name == varName }
            ?: return null
        return try {
            vn.type
        } catch (_: RuntimeException) {
            resolveType(registry, vn.typeExpr)
        }
    }

    fun check(expr: ExprNode, env: Map<String, Type>) {
        when (expr) {
            is QuantifiedExprNode -> {
                val t = resolveType(registry, expr.binderTypeExpr())
                if (t == null) {
                    errors += OneLocCompileError(
                        expr.programLocation(),
                        "unknown type \"${expr.binderTypeExpr()}\" in quantifier",
                    )
                    return
                }
                check(expr.quantifiedBody(), env + (expr.binderName() to t))
                expr.setInferredType(TypePassType.Inferred(boolType))
            }
            is FieldAccessExprNode -> {
                if (expr.fieldPath == listOf("length") && registry.sorts.containsKey(expr.baseSymbol)) {
                    expr.resolveFieldAccess(intType, "length")
                    return
                }
                val leaf = leafByName[expr.baseSymbol]
                if (leaf == null) {
                    errors += OneLocCompileError(
                        expr.programLocation(),
                        "invariant may only reference system components; unknown \"${expr.baseSymbol}\"",
                    )
                    return
                }
                if (expr.fieldPath.size != 1) {
                    errors += OneLocCompileError(
                        expr.programLocation(),
                        "invariant state reference must be Leaf.var",
                    )
                    return
                }
                val varName = expr.fieldPath[0]
                val vt = stateVarType(expr.baseSymbol, varName)
                if (vt == null) {
                    errors += OneLocCompileError(
                        expr.programLocation(),
                        "unknown state variable \"${expr.baseSymbol}.$varName\"",
                    )
                    return
                }
                if (leaf.isParameterized) {
                    errors += OneLocCompileError(
                        expr.programLocation(),
                        "parameterized component \"${expr.baseSymbol}\" requires indexed access \"${expr.baseSymbol}[i].$varName\"",
                    )
                    return
                }
                expr.resolveFieldAccess(vt, varName)
            }
            is MemberAccessExprNode -> {
                val indexed = expr.baseExpr
                if (indexed is IndexExprNode && indexed.base is SymbolValueExprNode) {
                    val leafName = (indexed.base as SymbolValueExprNode).symbol
                    val leaf = leafByName[leafName]
                    if (leaf == null) {
                        errors += OneLocCompileError(
                            expr.programLocation(),
                            "invariant may only reference system components; unknown \"$leafName\"",
                        )
                        return
                    }
                    if (!leaf.isParameterized) {
                        errors += OneLocCompileError(
                            expr.programLocation(),
                            "component \"$leafName\" is not parameterized; use \"$leafName.${expr.fieldName}\"",
                        )
                        return
                    }
                    val vt = stateVarType(leafName, expr.fieldName)
                    if (vt == null) {
                        errors += OneLocCompileError(
                            expr.programLocation(),
                            "unknown state variable \"$leafName.${expr.fieldName}\"",
                        )
                        return
                    }
                    check(indexed.index, env)
                    try {
                        val actualIdx = indexed.index.getType()
                        val expected = resolveType(registry, leaf.paramType!!)
                        if (expected != null && actualIdx != expected) {
                            errors += OneLocCompileError(
                                expr.programLocation(),
                                "index type $actualIdx does not match parameter type $expected",
                            )
                        }
                    } catch (_: RuntimeException) {
                    }
                    expr.setInferredType(TypePassType.Inferred(vt))
                    return
                }
                // Nested: Leaf[i].log.length, Leaf[i].self.id, …
                check(expr.baseExpr, env)
                val baseType = try {
                    expr.baseExpr.getType()
                } catch (_: RuntimeException) {
                    return
                }
                when (val coll = resolveCollectionProperty(baseType, expr.fieldName)) {
                    is CollectionPropResult.Resolved -> {
                        expr.setInferredType(TypePassType.Inferred(coll.type))
                        return
                    }
                    is CollectionPropResult.Error -> {
                        errors += OneLocCompileError(expr.programLocation(), coll.message)
                        return
                    }
                    is CollectionPropResult.NotCollectionProp -> {}
                }
                when (val result = resolveFieldPath(baseType, listOf(expr.fieldName))) {
                    is FieldPathResult.Resolved -> {
                        expr.setInferredType(TypePassType.Inferred(result.type))
                    }
                    is FieldPathResult.Error -> {
                        errors += OneLocCompileError(expr.programLocation(), result.message)
                    }
                }
            }
            is IndexExprNode -> {
                val base = expr.base
                // Bare Leaf[i] (missing .var)
                if (base is SymbolValueExprNode && leafByName.containsKey(base.symbol)) {
                    errors += OneLocCompileError(
                        expr.programLocation(),
                        "indexed component \"${base.symbol}\" requires a state variable: \"${base.symbol}[i].var\"",
                    )
                    return
                }
                // Leaf.var[j] on a parameterized leaf — instance access is Leaf[i].var
                if (base is FieldAccessExprNode && base.fieldPath.size == 1) {
                    val leaf = leafByName[base.baseSymbol]
                    if (leaf != null && leaf.isParameterized) {
                        val varName = base.fieldPath[0]
                        errors += OneLocCompileError(
                            expr.programLocation(),
                            "parameterized component \"${base.baseSymbol}\" requires instance access \"${base.baseSymbol}[i].$varName\" " +
                                "(list/map index: \"${base.baseSymbol}[i].$varName[j]\")",
                        )
                        return
                    }
                }
                check(base, env)
                try {
                    when (val baseType = base.getType()) {
                        is ListType -> {
                            check(expr.index, env)
                            try {
                                if (expr.index.getType() !is IntType) {
                                    errors += OneLocCompileError(
                                        expr.index.programLocation(),
                                        "Expected Int index but got ${expr.index.getType()}",
                                    )
                                }
                            } catch (_: RuntimeException) {
                            }
                            expr.setInferredType(TypePassType.Inferred(baseType.elementType))
                        }
                        is MapType -> {
                            check(expr.index, env)
                            try {
                                if (expr.index.getType() != baseType.keyType) {
                                    errors += OneLocCompileError(
                                        expr.index.programLocation(),
                                        "Expected map key type ${baseType.keyType} but got ${expr.index.getType()}",
                                    )
                                }
                            } catch (_: RuntimeException) {
                            }
                            expr.setInferredType(TypePassType.Inferred(baseType.valueType))
                        }
                        else -> {
                            errors += OneLocCompileError(
                                expr.programLocation(),
                                "Expected list or map type for index base but got $baseType",
                            )
                        }
                    }
                } catch (_: RuntimeException) {
                    // Base failed to type; error already recorded.
                }
            }
            is SymbolValueExprNode -> {
                val t = env[expr.symbol]
                when {
                    t != null -> expr.setInferredType(TypePassType.Inferred(t))
                    invariants.containsKey(expr.symbol) -> {
                        errors += typePassInvariantNamed(
                            expr.symbol,
                            invariants,
                            systemLeaves,
                            pclasses,
                            registry,
                            checking,
                            checked,
                        )
                        expr.setInferredType(TypePassType.Inferred(boolType))
                    }
                    else -> {
                        errors += OneLocCompileError(
                            expr.programLocation(),
                            "unbound symbol \"${expr.symbol}\" in invariant (use Leaf.var for state)",
                        )
                    }
                }
            }
            is FunCallExprNode -> {
                if (expr.callName() == "length" && expr.callArgs().size == 1) {
                    val arg = expr.callArgs().single()
                    if (arg is SymbolValueExprNode && registry.sorts.containsKey(arg.symbol)) {
                        arg.setInferredType(TypePassType.Inferred(registry.sorts.getValue(arg.symbol)))
                        expr.setInferredType(TypePassType.Inferred(intType))
                        return
                    }
                }
                expr.children.filterIsInstance<ExprNode>().forEach { check(it, env) }
                if (errors.isEmpty() && expr.callName() == "length" && expr.callArgs().size == 1) {
                    when (val t = try { expr.callArgs().single().getType() } catch (_: RuntimeException) { null }) {
                        is ListType, is SetType, is MapType ->
                            expr.setInferredType(TypePassType.Inferred(intType))
                        else -> {
                            if (t != null) {
                                errors += OneLocCompileError(
                                    expr.programLocation(),
                                    "Expected argument of \"length\" to have a List, Set, Map, or sort type but got $t",
                                )
                            }
                        }
                    }
                }
            }
            is LiteralValueExprNode -> {
                expr.setInferredType(TypePassType.Inferred(expr.inferType(env)))
            }
            is UnaryOpExprNode -> {
                check(expr.operand(), env)
                if (errors.isEmpty()) {
                    try {
                        expr.setInferredType(TypePassType.Inferred(expr.inferType(env)))
                    } catch (e: RuntimeException) {
                        errors += OneLocCompileError(expr.programLocation(), e.message ?: "type error")
                    }
                }
            }
            is ParenExprNode -> {
                check(expr.innerExpr(), env)
                if (errors.isEmpty()) {
                    try {
                        expr.setInferredType(TypePassType.Inferred(expr.inferType(env)))
                    } catch (e: RuntimeException) {
                        errors += OneLocCompileError(expr.programLocation(), e.message ?: "type error")
                    }
                }
            }
            is BinaryOpExprNode -> {
                check(expr.lhsOperand(), env)
                check(expr.rhsOperand(), env)
                if (errors.isEmpty()) {
                    try {
                        expr.setInferredType(TypePassType.Inferred(expr.inferType(env)))
                    } catch (e: RuntimeException) {
                        errors += OneLocCompileError(expr.programLocation(), e.message ?: "type error")
                    }
                }
            }
            is IfElseExprNode -> {
                check(expr.condExpr(), env)
                check(expr.thenExpr(), env)
                check(expr.elseExpr(), env)
                if (errors.isEmpty()) {
                    try {
                        expr.setInferredType(TypePassType.Inferred(expr.inferType(env)))
                    } catch (e: RuntimeException) {
                        errors += OneLocCompileError(expr.programLocation(), e.message ?: "type error")
                    }
                }
            }
            else -> {
                expr.children.filterIsInstance<ExprNode>().forEach { check(it, env) }
            }
        }
    }

    check(formula, emptyMap())
    return errors
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
            n.initExprs().forEach { expr ->
                val initErrs = typePassInitFormula(
                    expr, expanded, pclassNodes, leafSpecNodes, registry, constTypes, leafConst,
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
                    errors += sortLengthMaxListLenErrors(expr, registry.sorts)
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
    sorts: Map<String, SortType>,
): List<CompileError> {
    val errors = mutableListOf<CompileError>()
    fun checkSort(name: String, loc: julay.compiler.ProgramLoc) {
        val sort = sorts[name] ?: return
        if (sort.cfgElements.size > INIT_MAX_LIST_LEN) {
            errors += OneLocCompileError(
                loc,
                "\"init:\" constraint uses sort \"$name\" of size ${sort.cfgElements.size}, " +
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

private fun typePassInitFormula(
    formula: ExprNode,
    expanded: List<SpecLeaf>,
    pclassNodes: Map<String, ProcClassNode>,
    leafSpecNodes: Map<String, LeafSpecNode>,
    registry: ObjClassRegistry,
    constGlobalTypes: Map<String, Type>,
    leafConstGlobals: Map<String, Set<String>>,
): List<CompileError> {
    val leafByName = expanded.associateBy { it.name }
    val errors = mutableListOf<CompileError>()

    fun stateVarType(leafName: String, varName: String): Type? {
        val vn = declaredStateVars(leafName, pclassNodes, leafSpecNodes)[varName] ?: return null
        return try {
            vn.type
        } catch (_: RuntimeException) {
            resolveType(registry, vn.typeExpr)
        }
    }

    fun collectionLengthType(baseType: Type, loc: julay.compiler.ProgramLoc): Type? {
        return when (val r = resolveCollectionProperty(baseType, "length")) {
            is CollectionPropResult.Resolved -> r.type
            is CollectionPropResult.Error -> {
                errors += OneLocCompileError(loc, r.message)
                null
            }
            is CollectionPropResult.NotCollectionProp -> {
                errors += OneLocCompileError(loc, "Cannot access property \"length\" on type $baseType")
                null
            }
        }
    }

    fun requireConstGlobal(leafName: String, varName: String, loc: julay.compiler.ProgramLoc): Boolean {
        val consts = leafConstGlobals[leafName].orEmpty()
        if (varName in consts) return true
        errors += OneLocCompileError(
            loc,
            "\"init:\" may only mention const-global state; \"$leafName.$varName\" is not const global",
        )
        return false
    }

    fun check(expr: ExprNode, env: Map<String, Type>) {
        when (expr) {
            is QuantifiedExprNode -> {
                val t = resolveType(registry, expr.binderTypeExpr())
                if (t == null) {
                    errors += OneLocCompileError(
                        expr.programLocation(),
                        "unknown type \"${expr.binderTypeExpr()}\" in quantifier",
                    )
                    return
                }
                check(expr.quantifiedBody(), env + (expr.binderName() to t))
                expr.setInferredType(TypePassType.Inferred(boolType))
            }
            is FieldAccessExprNode -> {
                val path = expr.fieldPath
                if (path == listOf("length") && registry.sorts.containsKey(expr.baseSymbol) &&
                    expr.baseSymbol !in constGlobalTypes && expr.baseSymbol !in env
                ) {
                    expr.resolveFieldAccess(intType, "length")
                    return
                }
                if (path.firstOrNull() == "length" || path.lastOrNull() == "length") {
                    val constTy = constGlobalTypes[expr.baseSymbol]
                    if (constTy != null && path == listOf("length")) {
                        val lt = collectionLengthType(constTy, expr.programLocation()) ?: return
                        expr.resolveFieldAccess(lt, "length")
                        return
                    }
                }
                val leaf = leafByName[expr.baseSymbol]
                if (leaf != null) {
                    if (path.isEmpty()) {
                        errors += OneLocCompileError(expr.programLocation(), "\"init:\" state reference must be Leaf.var")
                        return
                    }
                    val varName = path[0]
                    if (!requireConstGlobal(leaf.name, varName, expr.programLocation())) return
                    val vt = stateVarType(leaf.name, varName)
                    if (vt == null) {
                        errors += OneLocCompileError(
                            expr.programLocation(),
                            "unknown state variable \"${expr.baseSymbol}.$varName\"",
                        )
                        return
                    }
                    if (path.size == 1) {
                        expr.resolveFieldAccess(vt, varName)
                        return
                    }
                    if (path == listOf(varName, "length")) {
                        val lt = collectionLengthType(vt, expr.programLocation()) ?: return
                        expr.resolveFieldAccess(lt, path.joinToString("."))
                        return
                    }
                    errors += OneLocCompileError(
                        expr.programLocation(),
                        "\"init:\" state reference must be Leaf.var or Leaf.var.length",
                    )
                    return
                }
                val constTy = constGlobalTypes[expr.baseSymbol]
                if (constTy != null) {
                    when (val coll = resolveCollectionPropertyPath(constTy, path)) {
                        is CollectionPropResult.Resolved -> {
                            expr.resolveFieldAccess(coll.type, path.joinToString("."))
                            return
                        }
                        is CollectionPropResult.Error -> {
                            errors += OneLocCompileError(expr.programLocation(), coll.message)
                            return
                        }
                        is CollectionPropResult.NotCollectionProp -> {}
                    }
                }
                errors += OneLocCompileError(
                    expr.programLocation(),
                    "unbound \"${expr.baseSymbol}.${path.joinToString(".")}\" in init: " +
                        "(use a const-global name, Leaf.var, or Sort.length)",
                )
            }
            is MemberAccessExprNode -> {
                if (expr.fieldName == "length") {
                    val base = expr.baseExpr
                    if (base is SymbolValueExprNode && registry.sorts.containsKey(base.symbol) &&
                        base.symbol !in constGlobalTypes && base.symbol !in env
                    ) {
                        base.setInferredType(TypePassType.Inferred(registry.sorts.getValue(base.symbol)))
                        expr.setInferredType(TypePassType.Inferred(intType))
                        return
                    }
                    check(base, env)
                    if (errors.isNotEmpty()) return
                    val baseType = try { base.getType() } catch (_: RuntimeException) { return }
                    val lt = collectionLengthType(baseType, expr.programLocation()) ?: return
                    expr.setInferredType(TypePassType.Inferred(lt))
                    return
                }
                val indexed = expr.baseExpr
                if (indexed is IndexExprNode && indexed.base is SymbolValueExprNode) {
                    val leafName = (indexed.base as SymbolValueExprNode).symbol
                    errors += OneLocCompileError(
                        expr.programLocation(),
                        "\"init:\" cannot use indexed access \"${leafName}[i].${expr.fieldName}\"; " +
                            "const-globals are scalars (write ${leafName}.${expr.fieldName} or a bare name)",
                    )
                    return
                }
                errors += OneLocCompileError(
                    expr.programLocation(),
                    "\"init:\" member access must be expr.length or Leaf.var",
                )
            }
            is FunCallExprNode -> {
                if (expr.callName() == "length" && expr.callArgs().size == 1) {
                    val arg = expr.callArgs().single()
                    if (arg is SymbolValueExprNode && registry.sorts.containsKey(arg.symbol) &&
                        arg.symbol !in constGlobalTypes && arg.symbol !in env
                    ) {
                        arg.setInferredType(TypePassType.Inferred(registry.sorts.getValue(arg.symbol)))
                        expr.setInferredType(TypePassType.Inferred(intType))
                        return
                    }
                    check(arg, env)
                    if (errors.isNotEmpty()) return
                    val t = try { arg.getType() } catch (_: RuntimeException) { return }
                    if (t is SortType) {
                        expr.setInferredType(TypePassType.Inferred(intType))
                        return
                    }
                    val lt = collectionLengthType(t, expr.programLocation()) ?: return
                    expr.setInferredType(TypePassType.Inferred(lt))
                    return
                }
                errors += OneLocCompileError(
                    expr.programLocation(),
                    "unknown function \"${expr.callName()}\" in init:",
                )
            }
            is IndexExprNode -> {
                val base = expr.base
                if (base is SymbolValueExprNode && leafByName.containsKey(base.symbol)) {
                    errors += OneLocCompileError(
                        expr.programLocation(),
                        "\"init:\" cannot index component \"${base.symbol}\"; const-globals are unindexed",
                    )
                    return
                }
                check(base, env)
                try {
                    when (val baseType = base.getType()) {
                        is ListType -> {
                            check(expr.index, env)
                            expr.setInferredType(TypePassType.Inferred(baseType.elementType))
                        }
                        is MapType -> {
                            check(expr.index, env)
                            expr.setInferredType(TypePassType.Inferred(baseType.valueType))
                        }
                        else -> {
                            errors += OneLocCompileError(
                                expr.programLocation(),
                                "Expected list or map type for index base but got $baseType",
                            )
                        }
                    }
                } catch (_: RuntimeException) {
                }
            }
            is SymbolValueExprNode -> {
                val t = env[expr.symbol] ?: constGlobalTypes[expr.symbol]
                if (t != null) {
                    expr.setInferredType(TypePassType.Inferred(t))
                } else {
                    errors += OneLocCompileError(
                        expr.programLocation(),
                        "unbound symbol \"${expr.symbol}\" in init: (use a const-global name or Sort.length)",
                    )
                }
            }
            is LiteralValueExprNode -> {
                expr.setInferredType(TypePassType.Inferred(expr.inferType(env)))
            }
            is UnaryOpExprNode -> {
                check(expr.operand(), env)
                if (errors.isEmpty()) {
                    try {
                        expr.setInferredType(TypePassType.Inferred(expr.inferType(env)))
                    } catch (e: RuntimeException) {
                        errors += OneLocCompileError(expr.programLocation(), e.message ?: "type error")
                    }
                }
            }
            is ParenExprNode -> {
                check(expr.innerExpr(), env)
                if (errors.isEmpty()) {
                    try {
                        expr.setInferredType(TypePassType.Inferred(expr.inferType(env)))
                    } catch (e: RuntimeException) {
                        errors += OneLocCompileError(expr.programLocation(), e.message ?: "type error")
                    }
                }
            }
            is BinaryOpExprNode -> {
                check(expr.lhsOperand(), env)
                check(expr.rhsOperand(), env)
                if (errors.isEmpty()) {
                    try {
                        expr.setInferredType(TypePassType.Inferred(expr.inferType(env)))
                    } catch (e: RuntimeException) {
                        errors += OneLocCompileError(expr.programLocation(), e.message ?: "type error")
                    }
                }
            }
            is IfElseExprNode -> {
                check(expr.condExpr(), env)
                check(expr.thenExpr(), env)
                check(expr.elseExpr(), env)
                if (errors.isEmpty()) {
                    try {
                        expr.setInferredType(TypePassType.Inferred(expr.inferType(env)))
                    } catch (e: RuntimeException) {
                        errors += OneLocCompileError(expr.programLocation(), e.message ?: "type error")
                    }
                }
            }
            else -> {
                expr.children.filterIsInstance<ExprNode>().forEach { check(it, env) }
            }
        }
    }

    check(formula, emptyMap())
    return errors
}
