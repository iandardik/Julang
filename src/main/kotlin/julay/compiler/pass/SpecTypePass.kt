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
            if (!leaf.isParameterized) {
                leaf.copy(paramName = ls.leafSpecParamName(), paramType = ls.leafSpecParamType())
            } else {
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
                leaf
            }
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
                // Decl-parameterized leaf specs are intentionally parameterized.
                if (ls.isParameterized()) return@forEach
                // Unparameterized leaf specs: same initially-only heuristic as procs.
            }
            val pc = pclassNodes[leaf.name] ?: return@forEach
            if (pc.isInitiallyOnly()) {
                if (leaf.isParameterized && ls?.isParameterized() != true) {
                    warnings += OneLocCompileWarning(
                        spec.programLocation(),
                        "proc \"${leaf.name}\" only has constructor initially, so indexing is unnecessary",
                    )
                }
            } else if (!leaf.isParameterized) {
                val kind = if (ls != null) "leaf spec" else "proc"
                val msg =
                    "$kind \"${leaf.name}\" can have multiple instances and must be indexed in this spec " +
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
            val assumeLeaves = annotateDeclParams(flattenSpecLeaves(value.assumeExpr()))
            val systemLeaves = annotateDeclParams(flattenSpecLeaves(value.systemExpr()))
            checkLeaves(assumeLeaves, "assumption")
            checkLeaves(systemLeaves, "system")
            checkIndexing(assumeLeaves)
            checkIndexing(systemLeaves)

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
            val systemLeaves = annotateDeclParams(flattenSpecLeaves(value))
            checkLeaves(systemLeaves, "system")
            checkIndexing(systemLeaves)
        }
    }
    return SpecTypePassResult(errors, warnings)
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
                if (indexed !is IndexExprNode || indexed.base !is SymbolValueExprNode) {
                    errors += OneLocCompileError(
                        expr.programLocation(),
                        "invariant member access must be Leaf[i].var",
                    )
                    return
                }
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
