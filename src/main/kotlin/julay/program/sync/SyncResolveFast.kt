package julay.program.sync

import julay.program.Constraint
import julay.program.Value
import julay.program.Variable
import julay.program.action.ConcreteAction
import julay.program.action.SymbolicAction
import julay.program.type.BoolType
import julay.program.type.IntType
import julay.program.type.StringType
import julay.program.type.Type
import java.util.Optional

/**
 * Sat / commit helpers that operate on [BoolExprFast] and [SyncAnti] **without** a Z3
 * [com.microsoft.z3.Context] — the avoid-Z3 optimization used when [Constraint.fast] (or
 * anti-only meta) is present.
 *
 * Returns null when the IR shape is unsupported so the caller can use residual Z3
 * ([SyncResolveZ3] on [com.microsoft.z3.BoolExpr], or [toZ3] when mixing IR offers with BoolExpr peers).
 *
 * Prefer calling this before [SyncResolveZ3] when opts are on; [SyncResolveZ3] itself also
 * tries this first when given a Context.
 */
object SyncResolveFast {
    /**
     * Pairwise / group satisfiability over IR (or [SyncAnti]-only sets).
     * @return true/false if decided; null → caller should use residual Z3
     */
    fun trySatisfiable(
        constraints: Set<Constraint>,
        config: SyncResolveConfig,
    ): Boolean? {
        if (!config.anyEnabled()) return null
        if (constraints.isEmpty()) return true
        if (constraints.all { it.anti != null && it.fast == null }) {
            return antiSatisfiable(constraints.mapNotNull { it.anti })
        }
        if (constraints.any { it.fast == null }) return null
        val guards = constraints.map { it.fast!! }
        return when (val u = unify(guards, config)) {
            is FastUnify.Ok -> true
            is FastUnify.Conflict -> false
            is FastUnify.Unsupported -> null
        }
    }

    /**
     * Bind action args from IR equalities / directed eval (no Z3 model).
     * @return null → residual Z3; [Optional.empty] → unsat; [Optional.of] → concrete args
     */
    fun tryConcreteAction(
        act: SymbolicAction,
        constraints: Set<Constraint>,
        config: SyncResolveConfig,
    ): Optional<ConcreteAction>? {
        if (!config.anyEnabled()) return null
        if (constraints.any { it.fast == null }) return null
        val guards = constraints.map { it.fast!! }
        return when (val u = unify(guards, config)) {
            is FastUnify.Unsupported -> null
            is FastUnify.Conflict -> Optional.empty()
            is FastUnify.Ok -> {
                if (act.args.isEmpty()) {
                    Optional.of(ConcreteAction(act, emptyMap()))
                } else {
                    val assignments = LinkedHashMap<Variable, Value>()
                    for (arg in act.args) {
                        val g = u.argGround[arg.name] ?: return null
                        val value = groundToValue(g, arg.type) ?: return null
                        assignments[arg] = value
                    }
                    Optional.of(ConcreteAction(act, assignments))
                }
            }
        }
    }

    /**
     * Whether [guard] is enabled given Kotlin [locals] (state var name → value).
     * Used for IR enablement checks; does not allocate Z3.
     * @return true/false if decided; null if unsupported (missing local / opaque shape)
     */
    fun enableFast(
        guard: BoolExprFast,
        locals: Map<String, Any?>,
        config: SyncResolveConfig,
    ): Boolean? {
        if (!config.anyEnabled()) return null
        return try {
            evalEnable(guard, locals)
        } catch (_: UnsupportedFast) {
            null
        }
    }

    /**
     * Prepare an IR guard for a SyncChannel offer: substitute [SyncTerm.Local] from [locals],
     * drop enablement-only atoms that hold, return null if enablement fails or a local is missing.
     */
    fun groundForOffer(
        guard: BoolExprFast,
        locals: Map<String, Any?>,
    ): BoolExprFast? {
        return try {
            groundGuard(guard, locals)
        } catch (_: EnablementFalse) {
            null
        } catch (_: UnsupportedFast) {
            null
        }
    }

    /**
     * Whether a set of [SyncAnti] constraints is jointly satisfiable (same semantics as
     * AND-ing the corresponding Z3 meta equalities).
     */
    fun antiSatisfiable(antis: List<SyncAnti>): Boolean {
        if (antis.isEmpty()) return true
        var classId: Int? = null
        var providerClient: Boolean? = null
        for (a in antis) {
            when (a) {
                is SyncAnti.ClassId -> {
                    if (classId != null && classId != a.classId) return false
                    classId = a.classId
                }
                is SyncAnti.ProviderClient -> {
                    if (providerClient != null && providerClient != a.isProvider) return false
                    providerClient = a.isProvider
                }
            }
        }
        return true
    }

    private sealed class FastUnify {
        data class Ok(val argGround: Map<String, SyncGround>) : FastUnify()
        data object Conflict : FastUnify()
        data object Unsupported : FastUnify()
    }

    private class UnsupportedFast : Exception()
    private class EnablementFalse : Exception()

    private fun unify(guards: List<BoolExprFast>, config: SyncResolveConfig): FastUnify {
        val eqs = mutableListOf<Pair<SyncTerm, SyncTerm>>()
        for (g in guards) {
            collectEqs(g, eqs) ?: return FastUnify.Unsupported
        }
        val argGround = mutableMapOf<String, SyncGround>()
        val pending = mutableListOf<Pair<String, SyncTerm>>()

        for ((left, right) in eqs) {
            when (val step = bindEq(left, right, argGround, pending, config)) {
                BindStep.Conflict -> return FastUnify.Conflict
                BindStep.Unsupported -> return FastUnify.Unsupported
                BindStep.Ok -> Unit
            }
        }

        var progress = true
        var steps = 0
        while (progress && steps++ < 64) {
            progress = false
            for ((arg, term) in pending) {
                if (argGround.containsKey(arg)) continue
                val g = evalTerm(term, argGround) ?: continue
                if (!bindGround(argGround, arg, g)) return FastUnify.Conflict
                progress = true
            }
        }
        return FastUnify.Ok(argGround)
    }

    private enum class BindStep { Ok, Conflict, Unsupported }

    private fun bindEq(
        left: SyncTerm,
        right: SyncTerm,
        argGround: MutableMap<String, SyncGround>,
        pending: MutableList<Pair<String, SyncTerm>>,
        config: SyncResolveConfig,
    ): BindStep {
        val lArg = (left as? SyncTerm.Arg)?.name
        val rArg = (right as? SyncTerm.Arg)?.name
        val lG = (left as? SyncTerm.Ground)?.value
        val rG = (right as? SyncTerm.Ground)?.value
        return when {
            lArg != null && rArg != null -> {
                val gl = argGround[lArg]
                val gr = argGround[rArg]
                when {
                    gl != null && gr != null && gl != gr -> BindStep.Conflict
                    gl != null -> {
                        argGround[rArg] = gl
                        BindStep.Ok
                    }
                    gr != null -> {
                        argGround[lArg] = gr
                        BindStep.Ok
                    }
                    else -> {
                        pending.add(lArg to right)
                        pending.add(rArg to left)
                        BindStep.Ok
                    }
                }
            }
            lArg != null && rG != null -> {
                if (!config.eqUnify && !config.argOwnership) return BindStep.Unsupported
                if (!bindGround(argGround, lArg, rG)) BindStep.Conflict else BindStep.Ok
            }
            rArg != null && lG != null -> {
                if (!config.eqUnify && !config.argOwnership) return BindStep.Unsupported
                if (!bindGround(argGround, rArg, lG)) BindStep.Conflict else BindStep.Ok
            }
            lArg != null -> {
                if (!config.directedEval) return BindStep.Unsupported
                if (!looksLikeIntArith(right)) return BindStep.Unsupported
                pending.add(lArg to right)
                BindStep.Ok
            }
            rArg != null -> {
                if (!config.directedEval) return BindStep.Unsupported
                if (!looksLikeIntArith(left)) return BindStep.Unsupported
                pending.add(rArg to left)
                BindStep.Ok
            }
            lG != null && rG != null -> if (lG == rG) BindStep.Ok else BindStep.Conflict
            else -> BindStep.Unsupported
        }
    }

    private fun collectEqs(
        g: BoolExprFast,
        out: MutableList<Pair<SyncTerm, SyncTerm>>,
    ): Unit? {
        when (g) {
            is BoolExprFast.True -> Unit
            is BoolExprFast.And -> g.parts.forEach { collectEqs(it, out) ?: return null }
            is BoolExprFast.Eq -> out.add(g.left to g.right)
            is BoolExprFast.NotLocalBool, is BoolExprFast.LocalBool ->
                // Enablement-only atoms should be grounded away before channel offer.
                return null
        }
        return Unit
    }

    private fun looksLikeIntArith(t: SyncTerm): Boolean = when (t) {
        is SyncTerm.Arg -> true
        is SyncTerm.Ground -> t.value is SyncGround.IntVal
        is SyncTerm.IntArith -> looksLikeIntArith(t.left) && looksLikeIntArith(t.right)
        is SyncTerm.Local, is SyncTerm.ToString -> false
    }

    private fun bindGround(map: MutableMap<String, SyncGround>, key: String, g: SyncGround): Boolean {
        val existing = map[key]
        return if (existing == null) {
            map[key] = g
            true
        } else {
            existing == g
        }
    }

    private fun evalTerm(term: SyncTerm, env: Map<String, SyncGround>): SyncGround? = when (term) {
        is SyncTerm.Arg -> env[term.name]
        is SyncTerm.Ground -> term.value
        is SyncTerm.Local -> null
        is SyncTerm.ToString -> {
            val inner = evalTerm(term.inner, env) ?: return null
            SyncGround.StringVal(inner.asKotlin().toString())
        }
        is SyncTerm.IntArith -> {
            val l = evalTerm(term.left, env) as? SyncGround.IntVal ?: return null
            val r = evalTerm(term.right, env) as? SyncGround.IntVal ?: return null
            val v = when (term.op) {
                SyncTerm.IntArith.Op.Add -> l.v + r.v
                SyncTerm.IntArith.Op.Sub -> l.v - r.v
                SyncTerm.IntArith.Op.Mul -> l.v * r.v
            }
            SyncGround.IntVal(v)
        }
    }

    private fun evalEnable(guard: BoolExprFast, locals: Map<String, Any?>): Boolean = when (guard) {
        is BoolExprFast.True -> true
        is BoolExprFast.And -> guard.parts.all { evalEnable(it, locals) }
        is BoolExprFast.NotLocalBool -> {
            val v = locals[guard.name] as? Boolean ?: throw UnsupportedFast()
            !v
        }
        is BoolExprFast.LocalBool -> {
            val v = locals[guard.name] as? Boolean ?: throw UnsupportedFast()
            v
        }
        is BoolExprFast.Eq -> {
            val l = evalTermLocals(guard.left, locals)
            val r = evalTermLocals(guard.right, locals)
            when {
                l is SyncTerm.Arg || r is SyncTerm.Arg -> true // arg constraints don't disable locally
                l is SyncTerm.Ground && r is SyncTerm.Ground -> l.value == r.value
                else -> throw UnsupportedFast()
            }
        }
    }

    private fun evalTermLocals(term: SyncTerm, locals: Map<String, Any?>): SyncTerm = when (term) {
        is SyncTerm.Arg -> term
        is SyncTerm.Ground -> term
        is SyncTerm.Local -> {
            val v = locals[term.name] ?: throw UnsupportedFast()
            SyncTerm.Ground(kotlinToGround(v))
        }
        is SyncTerm.ToString -> {
            val inner = evalTermLocals(term.inner, locals)
            when (inner) {
                is SyncTerm.Ground -> SyncTerm.Ground(SyncGround.StringVal(inner.value.asKotlin().toString()))
                else -> SyncTerm.ToString(inner)
            }
        }
        is SyncTerm.IntArith -> {
            val l = evalTermLocals(term.left, locals)
            val r = evalTermLocals(term.right, locals)
            if (l is SyncTerm.Ground && r is SyncTerm.Ground &&
                l.value is SyncGround.IntVal && r.value is SyncGround.IntVal
            ) {
                val lv = (l.value as SyncGround.IntVal).v
                val rv = (r.value as SyncGround.IntVal).v
                val v = when (term.op) {
                    SyncTerm.IntArith.Op.Add -> lv + rv
                    SyncTerm.IntArith.Op.Sub -> lv - rv
                    SyncTerm.IntArith.Op.Mul -> lv * rv
                }
                SyncTerm.Ground(SyncGround.IntVal(v))
            } else {
                SyncTerm.IntArith(term.op, l, r)
            }
        }
    }

    private fun groundGuard(guard: BoolExprFast, locals: Map<String, Any?>): BoolExprFast = when (guard) {
        is BoolExprFast.True -> BoolExprFast.True
        is BoolExprFast.And -> {
            val parts = guard.parts.mapNotNull { p ->
                when (val g = groundGuard(p, locals)) {
                    is BoolExprFast.True -> null
                    else -> g
                }
            }
            when {
                parts.isEmpty() -> BoolExprFast.True
                parts.size == 1 -> parts[0]
                else -> BoolExprFast.And(parts)
            }
        }
        is BoolExprFast.NotLocalBool -> {
            val v = locals[guard.name] as? Boolean ?: throw UnsupportedFast()
            if (v) throw EnablementFalse()
            BoolExprFast.True
        }
        is BoolExprFast.LocalBool -> {
            val v = locals[guard.name] as? Boolean ?: throw UnsupportedFast()
            if (!v) throw EnablementFalse()
            BoolExprFast.True
        }
        is BoolExprFast.Eq -> {
            val l = evalTermLocals(guard.left, locals)
            val r = evalTermLocals(guard.right, locals)
            when {
                l is SyncTerm.Ground && r is SyncTerm.Ground ->
                    if (l.value == r.value) BoolExprFast.True else throw EnablementFalse()
                l is SyncTerm.Arg || r is SyncTerm.Arg ||
                    l is SyncTerm.IntArith || r is SyncTerm.IntArith ||
                    l is SyncTerm.ToString || r is SyncTerm.ToString ->
                    BoolExprFast.Eq(l, r)
                else -> throw UnsupportedFast()
            }
        }
    }

    private fun kotlinToGround(v: Any?): SyncGround = when (v) {
        is Int -> SyncGround.IntVal(v)
        is Boolean -> SyncGround.BoolVal(v)
        is String -> SyncGround.StringVal(v)
        else -> throw UnsupportedFast()
    }

    private fun groundToValue(g: SyncGround, type: Type): Value? = try {
        when (type) {
            is IntType -> {
                val v = (g as? SyncGround.IntVal)?.v ?: return null
                Value(v, type)
            }
            is BoolType -> {
                val v = (g as? SyncGround.BoolVal)?.v ?: return null
                Value(v, type)
            }
            is StringType -> when (g) {
                is SyncGround.StringVal -> Value(g.v, type)
                else -> Value(g.asKotlin().toString(), type)
            }
            else -> null
        }
    } catch (_: Exception) {
        null
    }

    /**
     * Lower a **grounded** [BoolExprFast] (no [SyncTerm.Local] / enablement-only atoms) to a
     * Z3 [BoolExpr] so an IR-only offer can join residual BoolExpr peers in one solver check.
     * Returns null if the IR cannot be lowered.
     */
    fun toZ3(guard: BoolExprFast, ctx: com.microsoft.z3.Context): com.microsoft.z3.BoolExpr? {
        return try {
            guardToZ3(guard, ctx)
        } catch (_: UnsupportedFast) {
            null
        }
    }

    private fun guardToZ3(guard: BoolExprFast, ctx: com.microsoft.z3.Context): com.microsoft.z3.BoolExpr =
        when (guard) {
            is BoolExprFast.True -> ctx.mkTrue()
            is BoolExprFast.And -> {
                val parts = guard.parts.map { guardToZ3(it, ctx) }
                if (parts.isEmpty()) ctx.mkTrue()
                else if (parts.size == 1) parts[0]
                else ctx.mkAnd(*parts.toTypedArray())
            }
            is BoolExprFast.Eq -> {
                val l = termToZ3(guard.left, ctx)
                val r = termToZ3(guard.right, ctx)
                @Suppress("UNCHECKED_CAST")
                ctx.mkEq(
                    l as com.microsoft.z3.Expr<com.microsoft.z3.Sort>,
                    r as com.microsoft.z3.Expr<com.microsoft.z3.Sort>,
                )
            }
            is BoolExprFast.NotLocalBool, is BoolExprFast.LocalBool ->
                throw UnsupportedFast()
        }

    private fun termToZ3(term: SyncTerm, ctx: com.microsoft.z3.Context): com.microsoft.z3.Expr<*> =
        when (term) {
            is SyncTerm.Arg -> when (term.sort) {
                SyncTerm.Arg.Sort.Int -> ctx.mkIntConst(term.name)
                SyncTerm.Arg.Sort.Bool -> ctx.mkBoolConst(term.name)
                SyncTerm.Arg.Sort.String -> ctx.mkConst(term.name, ctx.stringSort)
            }
            is SyncTerm.Ground -> when (val g = term.value) {
                is SyncGround.IntVal -> ctx.mkInt(g.v)
                is SyncGround.BoolVal -> ctx.mkBool(g.v)
                is SyncGround.StringVal -> ctx.mkString(g.v)
            }
            is SyncTerm.IntArith -> {
                val l = termToZ3(term.left, ctx) as com.microsoft.z3.IntExpr
                val r = termToZ3(term.right, ctx) as com.microsoft.z3.IntExpr
                when (term.op) {
                    SyncTerm.IntArith.Op.Add -> ctx.mkAdd(l, r)
                    SyncTerm.IntArith.Op.Sub -> ctx.mkSub(l, r)
                    SyncTerm.IntArith.Op.Mul -> ctx.mkMul(l, r)
                }
            }
            is SyncTerm.ToString -> {
                val inner = termToZ3(term.inner, ctx)
                when {
                    inner is com.microsoft.z3.IntNum -> ctx.mkString(inner.int.toString())
                    inner.isString -> inner
                    else -> throw UnsupportedFast()
                }
            }
            is SyncTerm.Local -> throw UnsupportedFast()
        }
}
