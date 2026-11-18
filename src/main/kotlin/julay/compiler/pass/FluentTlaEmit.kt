package julay.compiler.pass

import julay.compiler.ast.*
import julay.compiler.decl.ObjClassRegistry
import julay.compiler.decl.TypeResolveResult
import julay.program.type.Type

/** Fluents + formula-assume name needed while emitting one AG spec. */
internal data class FluentEmitPlan(
    val fluents: List<FluentNode>,
    /** Invariant name whose `NameOK` (primed fluents) is conjoined on actions; null if none. */
    val formulaAssumeName: String?,
    val registry: ObjClassRegistry,
) {
    val byName: Map<String, FluentNode> = fluents.associateBy { it.name() }
}

internal fun buildFluentEmitPlan(
    spec: SpecNode,
    invariants: Map<String, InvariantNode>,
    allFluents: Map<String, FluentNode>,
    systemDeclNames: Set<String>,
    registry: ObjClassRegistry,
): FluentEmitPlan {
    val ag = spec.specNodeValue() as? AgSpecExprNode
    val (formulaAssume, _) = formulaAssumeInvariantName(
        ag?.assumeExpr(),
        invariants.keys,
        systemDeclNames,
    )
    val used = linkedSetOf<String>()
    fun absorbInv(name: String?) {
        if (name == null) return
        val inv = invariants[name] ?: return
        used += fluentsUsedByInvariant(inv, invariants, allFluents)
    }
    absorbInv(formulaAssume)
    val guarantee = ag?.guaranteeExpr()
    if (guarantee is SymbolValueExprNode) {
        absorbInv(guarantee.symbol)
    } else if (guarantee != null) {
        val refs = collectFluentRefs(guarantee, allFluents.keys)
        val flSeen = mutableSetOf<String>()
        fun walkFl(n: String) {
            if (!flSeen.add(n)) return
            used += n
            allFluents[n]?.fluentOnClauses()?.forEach { c ->
                val t = c.target
                if (t is FluentTarget.FluentRef && t.name in allFluents) walkFl(t.name)
            }
        }
        refs.forEach { walkFl(it) }
    }
    return FluentEmitPlan(used.mapNotNull { allFluents[it] }, formulaAssume, registry)
}

internal fun fluentParamTypes(fluent: FluentNode, registry: ObjClassRegistry): List<Pair<String, Type>> =
    fluent.fluentParams().map { p ->
        val ty = when (val r = registry.resolveTypeExpr(p.typeExpr, emptyMap())) {
            is TypeResolveResult.Found -> r.type
            is TypeResolveResult.Error -> error("unresolved fluent param ${p.name}")
        }
        p.name to ty
    }

private fun initLiteralBool(expr: ExprNode): String? = when {
    expr is LiteralValueExprNode && expr.literalText().equals("false", ignoreCase = true) -> "FALSE"
    expr is LiteralValueExprNode && expr.literalText().equals("true", ignoreCase = true) -> "TRUE"
    else -> null
}

/** Init conjuncts for fluents; appends VARIABLE names to [variables]. */
internal fun emitFluentInit(
    plan: FluentEmitPlan,
    variables: MutableList<String>,
    initParts: MutableList<String>,
) {
    if (plan.fluents.isEmpty()) return
    initParts += "\\* fluent history variables"
    plan.fluents.forEach { fl ->
        val name = fl.name()
        variables += name
        val params = fluentParamTypes(fl, plan.registry)
        val initExpr = fl.fluentInitially()
        val lit = initLiteralBool(initExpr)
        val initTla = lit ?: try {
            exprToTla(
                initExpr,
                leafCtx = emptyMap(),
                argNames = params.map { it.first }.toSet(),
                self = null,
                bareStateVars = emptySet(),
                reservedNames = emptySet(),
                stateVarNames = emptyMap(),
                symbolOverrides = emptyMap(),
                linePrefix = "",
            )
        } catch (_: RuntimeException) {
            "FALSE"
        }
        if (params.isEmpty()) {
            initParts += "/\\ $name = $initTla"
        } else if (lit == "FALSE" || lit == "TRUE") {
            val domains = params.joinToString(", ") { (n, ty) ->
                "$n \\in ${typeToTlaDomain(ty)}"
            }
            initParts += "/\\ $name = [$domains |-> $initTla]"
        } else {
            val binders = params.joinToString(", ") { (n, ty) ->
                "$n \\in ${typeToTlaDomain(ty)}"
            }
            val apps = params.joinToString(", ") { it.first }
            initParts += "/\\ \\A $binders : $name[$apps] <=> $initTla"
        }
    }
}

/**
 * TLA conjuncts updating fluents for [actionName].
 * [indexTla] is the create-index binder in the action signature when present.
 * [argTlaByJulay] maps Julay action-arg names to TLA binders.
 */
internal fun emitFluentUpdatesForAction(
    plan: FluentEmitPlan,
    actionName: String,
    indexTla: String?,
    argTlaByJulay: Map<String, String>,
): List<String> {
    val parts = mutableListOf<String>()
    plan.fluents.forEach { fl ->
        val clauses = fl.fluentOnClauses().filter { clause ->
            clause.actionName == actionName &&
                when {
                    clause.indexBinder != null -> indexTla != null
                    else -> true
                }
        }
        if (clauses.isEmpty()) return@forEach
        val params = fluentParamTypes(fl, plan.registry)
        clauses.forEach { clause ->
            val matchedBinders = clause.argBinders.filter { it != "_" }
            val cellArgs = matchedBinders.map { b -> argTlaByJulay[b] ?: b }
            val targetTla = when (val t = clause.target) {
                FluentTarget.True -> "TRUE"
                FluentTarget.False -> "FALSE"
                is FluentTarget.FluentRef -> {
                    val innerArgs = t.args.map { argExpr ->
                        fluentTargetArgToTla(argExpr, argTlaByJulay)
                    }
                    val app = if (innerArgs.isEmpty()) {
                        t.name
                    } else {
                        "${t.name}[${innerArgs.joinToString(", ")}]"
                    }
                    if (t.negated) "~$app" else app
                }
            }
            if (params.isEmpty()) {
                parts += "/\\ ${fl.name()}' = $targetTla"
            } else {
                val keys = cellArgs.joinToString(", ")
                parts += "/\\ ${fl.name()}' = [${fl.name()} EXCEPT ![$keys] = $targetTla]"
            }
        }
    }
    return parts
}

private fun fluentTargetArgToTla(expr: ExprNode, argTlaByJulay: Map<String, String>): String =
    when (expr) {
        is SymbolValueExprNode -> argTlaByJulay[expr.symbol] ?: expr.symbol
        else -> try {
            exprToTla(
                expr,
                leafCtx = emptyMap(),
                argNames = argTlaByJulay.keys,
                self = null,
                bareStateVars = emptySet(),
                reservedNames = emptySet(),
                stateVarNames = emptyMap(),
                symbolOverrides = argTlaByJulay,
                linePrefix = "",
            )
        } catch (_: RuntimeException) {
            expr.toString()
        }
    }

internal fun fluentsChangedByAction(plan: FluentEmitPlan, actionName: String): Set<String> =
    plan.fluents.filter { fl -> fl.fluentOnClauses().any { it.actionName == actionName } }
        .map { it.name() }
        .toSet()

/**
 * Post-state assume conjunct. SANY does not reliably accept `Op'` for a defined
 * operator `Op == …`, so we emit a separate `OpOK == <formula with fluent primes>`
 * and conjoin `OpOK` on actions.
 */
internal fun emitFluentAssumePrime(plan: FluentEmitPlan): String? =
    plan.formulaAssumeName?.let { "/\\ ${it}OK" }

internal fun assumeOkOperatorName(assumeName: String): String = "${assumeName}OK"
