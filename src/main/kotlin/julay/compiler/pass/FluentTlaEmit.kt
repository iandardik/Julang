package julay.compiler.pass

import julay.compiler.ast.*
import julay.compiler.decl.ObjClassRegistry
import julay.compiler.decl.TypeResolveResult
import julay.program.type.ListType
import julay.program.type.ObjClassType
import julay.program.type.Type

/** Fluents + formula-assume names needed while emitting one AG spec. */
internal data class FluentEmitPlan(
    val fluents: List<FluentNode>,
    /** Invariant names whose `NameOK` (primed fluents) are conjoined on actions. */
    val formulaAssumeNames: List<String>,
    val registry: ObjClassRegistry,
) {
    val byName: Map<String, FluentNode> = fluents.associateBy { it.name() }
    /** First assume name (compat); null if none. */
    val formulaAssumeName: String? get() = formulaAssumeNames.firstOrNull()
}

internal fun buildFluentEmitPlan(
    spec: SpecNode,
    invariants: Map<String, InvariantNode>,
    allFluents: Map<String, FluentNode>,
    systemDeclNames: Set<String>,
    registry: ObjClassRegistry,
    /** Extra invariant names to pull fluents from (e.g. CircSide A/G). */
    extraInvariantNames: List<String> = emptyList(),
): FluentEmitPlan {
    val ag = spec.specNodeValue() as? AgSpecExprNode
    val (formulaAssumes, _) = if (ag != null) {
        formulaAssumeNames(ag, invariants.keys, systemDeclNames)
    } else {
        emptyList<String>() to emptyList()
    }
    val used = linkedSetOf<String>()
    fun absorbInv(name: String?) {
        if (name == null) return
        val inv = invariants[name] ?: return
        used += fluentsUsedByInvariant(inv, invariants, allFluents)
    }
    formulaAssumes.forEach { absorbInv(it) }
    val (guarNames, _) = if (ag != null) {
        guaranteeInvariantNames(ag, invariants.keys)
    } else {
        emptyList<String>() to emptyList()
    }
    if (guarNames.isNotEmpty()) {
        guarNames.forEach { absorbInv(it) }
    } else {
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
    }
    extraInvariantNames.forEach { absorbInv(it) }
    return FluentEmitPlan(used.mapNotNull { allFluents[it] }, formulaAssumes, registry)
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
            // Prefix binders so they never clash with VARIABLES (e.g. ApplyClaim.owner).
            val domains = params.joinToString(", ") { (n, ty) ->
                "__$n \\in ${typeToTlaDomain(ty)}"
            }
            initParts += "/\\ $name = [$domains |-> $initTla]"
        } else {
            val binders = params.joinToString(", ") { (n, ty) ->
                "__$n \\in ${typeToTlaDomain(ty)}"
            }
            val apps = params.joinToString(", ") { "__${it.first}" }
            initParts += "/\\ \\A $binders : $name[$apps] <=> $initTla"
        }
    }
}

/**
 * TLA conjuncts updating fluents for [actionName].
 * [indexTla] is the create-index binder in the action signature when present.
 * [argTlaByJulay] maps Julay action-arg names to TLA binders.
 *
 * List-arg bulk updates (SKV `dropReshardBatch` / `entries`): when a clause binds a
 * list/seq arg named `entries` (typically `List<KvPair>`) and a Key fluent axis is
 * otherwise unbound,
 * - `:= false` clears the fluent for every key in `{ entries[i].k : i \in DOMAIN entries }`
 *   (optional index axis);
 * - `:= true` with a String owner binder replaces the owner for those keys.
 */
internal fun emitFluentUpdatesForAction(
    plan: FluentEmitPlan,
    actionName: String,
    indexTla: String?,
    argTlaByJulay: Map<String, String>,
    argTypesByJulay: Map<String, Type> = emptyMap(),
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
                return@forEach
            }
            val bulk = emitBulkEntriesUpdate(
                flName = fl.name(),
                params = params,
                clause = clause,
                targetTla = targetTla,
                indexTla = indexTla,
                argTlaByJulay = argTlaByJulay,
                entriesElemType = (argTypesByJulay["entries"] as? ListType)?.elementType,
            )
            if (bulk != null) {
                parts += bulk
                return@forEach
            }
            val keys = alignFluentExceptKeys(
                params = params,
                clause = clause,
                matchedBinders = matchedBinders,
                indexTla = indexTla,
                argTlaByJulay = argTlaByJulay,
            ) ?: return@forEach
            parts += "/\\ ${fl.name()}' = [${fl.name()} EXCEPT ![$keys] = $targetTla]"
        }
    }
    return parts
}

/**
 * Build EXCEPT index values in fluent-param order.
 * Create-index binder fills a same-named param, or (when still open) a String/domain
 * param so `on putLocal[n](putKey, _) := true` can set `ClaimSeen(putKey, n)`.
 */
private fun alignFluentExceptKeys(
    params: List<Pair<String, Type>>,
    clause: FluentOnClause,
    matchedBinders: List<String>,
    indexTla: String?,
    argTlaByJulay: Map<String, String>,
): String? {
    val binderQueue = matchedBinders.toMutableList()
    var indexUsed = false
    val keys = mutableListOf<String>()
    params.forEach { (pname, _) ->
        when {
            clause.indexBinder != null && indexTla != null && pname == clause.indexBinder -> {
                keys += indexTla
                indexUsed = true
            }
            binderQueue.isNotEmpty() && binderQueue.first() == pname -> {
                val b = binderQueue.removeAt(0)
                keys += argTlaByJulay[b] ?: b
            }
            binderQueue.isNotEmpty() -> {
                val b = binderQueue.removeAt(0)
                keys += argTlaByJulay[b] ?: b
            }
            !indexUsed && clause.indexBinder != null && indexTla != null -> {
                keys += indexTla
                indexUsed = true
            }
            else -> return null
        }
    }
    if (binderQueue.isNotEmpty()) return null
    return keys.joinToString(", ")
}

/**
 * Bulk clear/replace for SKV `dropReshardBatch`.
 * When field projection keeps KvPair as a record, keys are `entries[i].k`;
 * when the singleton `k` field is unwrapped, `entries` is already a seq of keys.
 */
private fun emitBulkEntriesUpdate(
    flName: String,
    params: List<Pair<String, Type>>,
    clause: FluentOnClause,
    targetTla: String,
    indexTla: String?,
    argTlaByJulay: Map<String, String>,
    entriesElemType: Type?,
): String? {
    if ("entries" !in clause.argBinders) return null
    if (clause.target !is FluentTarget.True && clause.target !is FluentTarget.False) return null
    val entriesTla = argTlaByJulay["entries"] ?: "entries"
    val keySet = entriesKeySetExpr(entriesTla, entriesElemType)
    val keyParamIdx = params.indexOfFirst { (_, ty) ->
        typeToTlaDomain(ty) == "Key"
    }
    if (keyParamIdx < 0) return null
    val ownerBinder = clause.argBinders.firstOrNull { it != "_" && it != "entries" && it in argTlaByJulay }
    val ownerTla = ownerBinder?.let { argTlaByJulay[it] }
    val binders = params.mapIndexed { i, (pname, _) ->
        val v = "__p$i"
        val dom = typeToTlaDomain(params[i].second)
        Triple(pname, v, dom)
    }
    val quant = binders.joinToString(", ") { (_, v, dom) -> "$v \\in $dom" }
    val args = binders.joinToString(", ") { it.second }
    val keyVar = binders[keyParamIdx].second
    val indexParamIdx = params.indexOfFirst { it.first == clause.indexBinder }
    val body = when {
        // Held(n,k): clear cells for this index and keys in entries
        clause.target is FluentTarget.False && indexParamIdx >= 0 && indexTla != null -> {
            val nVar = binders[indexParamIdx].second
            "IF $nVar = $indexTla /\\ $keyVar \\in $keySet THEN FALSE ELSE $flName[$args]"
        }
        // ClaimSeen(k, owner): replace owner for keys in entries
        clause.target is FluentTarget.True && ownerTla != null && params.size == 2 -> {
            val ownerVar = binders.first { it.first != params[keyParamIdx].first }.second
            "IF $keyVar \\in $keySet THEN ($ownerVar = $ownerTla) ELSE $flName[$args]"
        }
        clause.target is FluentTarget.False && params.size == 2 && keyParamIdx >= 0 -> {
            "IF $keyVar \\in $keySet THEN FALSE ELSE $flName[$args]"
        }
        else -> return null
    }
    return "/\\ $flName' = [$quant |-> $body]"
}

/** Key set from a seq arg named `entries` (KvPair records or unwrapped keys). */
private fun entriesKeySetExpr(entriesTla: String, elemType: Type?): String {
    if (elemType is ObjClassType) {
        val proj = TlaFieldProjection.get()
        val single = proj.singletonField(elemType)
        if (single != null && single.name == "k") {
            return "Range($entriesTla)"
        }
        if (proj.fieldsFor(elemType).any { it.name == "k" }) {
            return "{ $entriesTla[__ei].k : __ei \\in DOMAIN $entriesTla }"
        }
    }
    return "Range($entriesTla)"
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
 * Post-state assume conjuncts. SANY does not reliably accept `Op'` for a defined
 * operator `Op == …`, so we emit `OpOK == <formula with fluent primes>` per assume
 * and conjoin each `OpOK` on actions.
 */
internal fun emitFluentAssumePrime(plan: FluentEmitPlan): List<String> =
    plan.formulaAssumeNames.map { "/\\ ${it}OK" }

internal fun assumeOkOperatorName(assumeName: String): String = "${assumeName}OK"
