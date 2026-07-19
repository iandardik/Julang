package julay.compiler.pass

import julay.compiler.*
import julay.compiler.ast.*
import julay.compiler.decl.*
import julay.program.type.*

fun RootNode.specTypePass(unit: CompilationUnit): List<CompileError> {
    val registry = cachedObjClassRegistry() ?: return emptyList()
    val invariants = declNodes().filterIsInstance<InvariantNode>().associateBy { it.name() }
    val pclassNodes = unit.modules
        .flatMap { it.root.declNodes().filterIsInstance<ProcClassNode>() }
        .associateBy { it.name() }

    return declNodes().filterIsInstance<SpecNode>().flatMap { spec ->
        typePassSpec(spec, invariants, pclassNodes, unit, registry)
    }
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
    unit: CompilationUnit,
    registry: ObjClassRegistry,
): List<CompileError> {
    val errors = mutableListOf<CompileError>()
    val value = spec.specNodeValue()

    fun checkLeaves(leaves: List<SpecLeaf>, role: String) {
        leaves.forEach { leaf ->
            val known = leaf.name in pclassNodes ||
                leaf.name in unit.allPClassNames ||
                leaf.name in unit.allProcNames ||
                leaf.name in unit.entryDeclNames ||
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

    when (value) {
        is AgSpecExprNode -> {
            val assumeLeaves = flattenSpecLeaves(value.assumeExpr())
            val systemLeaves = flattenSpecLeaves(value.systemExpr())
            checkLeaves(assumeLeaves, "assumption")
            checkLeaves(systemLeaves, "system")

            val invName = value.invariantRef()
            val inv = invariants[invName]
            if (inv == null) {
                errors += OneLocCompileError(
                    spec.programLocation(),
                    "unknown invariant \"$invName\"",
                )
            } else {
                val procAliases = unit.modules
                    .flatMap { it.root.declNodes().filterIsInstance<ProcNode>() }
                    .associateBy { it.name() }
                val programAliases = unit.modules
                    .flatMap { it.root.declNodes().filterIsInstance<ProgramNode>() }
                    .associateBy { it.name() }
                val specAliases = unit.modules
                    .flatMap { it.root.declNodes().filterIsInstance<SpecNode>() }
                    .associateBy { it.name() }
                val expandedSystem = expandLeavesToPclasses(
                    systemLeaves,
                    pclassNodes,
                    procAliases,
                    programAliases,
                    specAliases,
                )
                val systemPclasses = expandedSystem.mapNotNull { leaf ->
                    pclassNodes[leaf.name]?.let { leaf.name to it }
                }.toMap()
                errors += typePassInvariantFormula(
                    inv.invariantFormula(),
                    expandedSystem,
                    systemPclasses,
                    registry,
                )
            }
        }
        else -> checkLeaves(flattenSpecLeaves(value), "system")
    }
    return errors
}

private fun typePassInvariantFormula(
    formula: ExprNode,
    systemLeaves: List<SpecLeaf>,
    pclasses: Map<String, ProcClassNode>,
    registry: ObjClassRegistry,
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
                        "parameterized component \"${expr.baseSymbol}\" requires indexed access \"${expr.baseSymbol}.$varName[i]\"",
                    )
                    return
                }
                expr.resolveFieldAccess(vt, varName)
            }
            is IndexExprNode -> {
                val base = expr.base
                if (base !is FieldAccessExprNode || base.fieldPath.size != 1) {
                    errors += OneLocCompileError(
                        expr.programLocation(),
                        "indexed invariant ref must be Leaf.var[index]",
                    )
                    return
                }
                val leaf = leafByName[base.baseSymbol]
                if (leaf == null) {
                    errors += OneLocCompileError(
                        expr.programLocation(),
                        "invariant may only reference system components; unknown \"${base.baseSymbol}\"",
                    )
                    return
                }
                if (!leaf.isParameterized) {
                    errors += OneLocCompileError(
                        expr.programLocation(),
                        "component \"${base.baseSymbol}\" is not parameterized; use \"${base.baseSymbol}.${base.fieldPath[0]}\"",
                    )
                    return
                }
                val varName = base.fieldPath[0]
                val vt = stateVarType(base.baseSymbol, varName)
                if (vt == null) {
                    errors += OneLocCompileError(
                        expr.programLocation(),
                        "unknown state variable \"${base.baseSymbol}.$varName\"",
                    )
                    return
                }
                errors += expr.index.typePass(env, registry)
                try {
                    val actualIdx = expr.index.getType()
                    val expected = resolveType(registry, leaf.paramType!!)
                    if (expected != null && actualIdx != expected) {
                        errors += OneLocCompileError(
                            expr.programLocation(),
                            "index type $actualIdx does not match parameter type $expected",
                        )
                    }
                } catch (_: RuntimeException) {
                }
                base.resolveFieldAccess(vt, varName)
                expr.setInferredType(TypePassType.Inferred(vt))
            }
            is SymbolValueExprNode -> {
                val t = env[expr.symbol]
                if (t == null) {
                    errors += OneLocCompileError(
                        expr.programLocation(),
                        "unbound symbol \"${expr.symbol}\" in invariant (use Leaf.var for state)",
                    )
                } else {
                    expr.setInferredType(TypePassType.Inferred(t))
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
