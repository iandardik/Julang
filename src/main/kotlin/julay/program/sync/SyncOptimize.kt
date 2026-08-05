package julay.program.sync

import com.microsoft.z3.BoolExpr
import com.microsoft.z3.Context
import com.microsoft.z3.Expr
import com.microsoft.z3.IntNum
import com.microsoft.z3.Status
import julay.program.Constraint
import julay.program.SyncOptimizeConfig
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
 * Fast-path helpers for action sync: equality unify, unilateral arg ownership,
 * and directed evaluation of determined args. Falls back when the formula shape
 * is unsupported ([trySatisfiable] / [tryConcreteAction] return null).
 *
 * Meta constraints: [META_PROVIDER_CLIENT], [META_CLASS_ID].
 */
object SyncOptimize {
    const val META_PROVIDER_CLIENT = "providerClientTransition"
    const val META_CLASS_ID = "classID"

    /**
     * @return true/false if decided; null → residual Z3
     */
    fun trySatisfiable(
        constraints: Set<Constraint>,
        config: SyncOptimizeConfig,
        ctx: Context,
    ): Boolean? {
        if (!config.anyEnabled()) return null
        val translated = constraints.map { it.expr.translate(ctx) as BoolExpr }
        val analysis = analyzeAll(translated) ?: return null
        if (!analysis.onlyEqualities) return null // mixed relational → residual Z3
        if (!config.eqUnify && !config.argOwnership && !config.directedEval) return null
        return when (val u = unify(analysis, config, ctx)) {
            is UnifyResult.Ok -> true
            is UnifyResult.Conflict -> false
            is UnifyResult.Unsupported -> null
        }
    }

    /**
     * @return null → residual Z3; [Optional.empty] → unsat; [Optional.of] → concrete args
     */
    fun tryConcreteAction(
        act: SymbolicAction,
        constraints: Set<Constraint>,
        config: SyncOptimizeConfig,
        ctx: Context,
    ): Optional<ConcreteAction>? {
        if (!config.anyEnabled()) return null
        val translated = constraints.map { it.expr.translate(ctx) as BoolExpr }
        val analysis = analyzeAll(translated) ?: return null
        if (!analysis.onlyEqualities) return null
        return when (val u = unify(analysis, config, ctx)) {
            is UnifyResult.Unsupported -> null
            is UnifyResult.Conflict -> Optional.empty()
            is UnifyResult.Ok -> {
                if (act.args.isEmpty()) {
                    Optional.of(ConcreteAction(act, emptyMap()))
                } else {
                    val assignments = LinkedHashMap<Variable, Value>()
                    for (arg in act.args) {
                        val expr = u.bindings.argGround[arg.name] ?: return null
                        val value = concreteValue(expr, arg.type, ctx) ?: return null
                        assignments[arg] = value
                    }
                    Optional.of(ConcreteAction(act, assignments))
                }
            }
        }
    }

    /** Local enablement short-circuit. */
    fun tryLocalEnablement(guard: BoolExpr, config: SyncOptimizeConfig, ctx: Context): Boolean? {
        if (!config.eqUnify) return null
        val analysis = analyzeAll(listOf(guard)) ?: return null
        if (!analysis.onlyEqualities) return null
        return when (val u = unify(analysis, config, ctx)) {
            is UnifyResult.Ok -> true
            is UnifyResult.Conflict -> false
            is UnifyResult.Unsupported -> null
        }
    }

    private data class Analysis(
        val eqs: List<Pair<Expr<*>, Expr<*>>>,
        val onlyEqualities: Boolean,
    )

    private data class Bindings(
        val argGround: Map<String, Expr<*>>,
    )

    private sealed class UnifyResult {
        data class Ok(val bindings: Bindings) : UnifyResult()
        data object Conflict : UnifyResult()
        data object Unsupported : UnifyResult()
    }

    private fun analyzeAll(exprs: List<BoolExpr>): Analysis? {
        val eqs = mutableListOf<Pair<Expr<*>, Expr<*>>>()
        var onlyEq = true
        for (e in exprs) {
            for (c in flattenAnd(e)) {
                when {
                    c.isTrue -> Unit
                    c.isEq -> eqs.add(c.args[0] to c.args[1])
                    else -> onlyEq = false
                }
            }
        }
        return Analysis(eqs, onlyEq)
    }

    private fun flattenAnd(e: BoolExpr): List<BoolExpr> {
        if (e.isTrue) return emptyList()
        if (e.isAnd) return e.args.flatMap { flattenAnd(it as BoolExpr) }
        return listOf(e)
    }

    private fun unify(analysis: Analysis, config: SyncOptimizeConfig, ctx: Context): UnifyResult {
        val argGround = mutableMapOf<String, Expr<*>>()
        val metaGround = mutableMapOf<String, Expr<*>>()
        val pendingFunctional = mutableListOf<Pair<String, Expr<*>>>()

        for ((left, right) in analysis.eqs) {
            val lMeta = metaName(left)
            val rMeta = metaName(right)
            val lArg = argConstName(left)
            val rArg = argConstName(right)
            when {
                lMeta != null && isValueGround(right) -> {
                    if (!bind(metaGround, lMeta, right)) return UnifyResult.Conflict
                }
                rMeta != null && isValueGround(left) -> {
                    if (!bind(metaGround, rMeta, left)) return UnifyResult.Conflict
                }
                lMeta != null || rMeta != null -> {
                    return UnifyResult.Unsupported
                }
                lArg != null && rArg != null -> {
                    val gl = argGround[lArg]
                    val gr = argGround[rArg]
                    when {
                        gl != null && gr != null && !groundEqual(gl, gr) -> return UnifyResult.Conflict
                        gl != null -> argGround[rArg] = gl
                        gr != null -> argGround[lArg] = gr
                        else -> {
                            pendingFunctional.add(lArg to right)
                            pendingFunctional.add(rArg to left)
                        }
                    }
                }
                lArg != null && isValueGround(right) -> {
                    if (!config.eqUnify && !config.argOwnership) return UnifyResult.Unsupported
                    // Only bind when RHS is a literal/numeral/bool — not arbitrary ground apps
                    // (e.g. string concat of collections), which need residual Z3 / model eval.
                    if (!isLiteralGround(right)) return UnifyResult.Unsupported
                    if (!bind(argGround, lArg, right)) return UnifyResult.Conflict
                }
                rArg != null && isValueGround(left) -> {
                    if (!config.eqUnify && !config.argOwnership) return UnifyResult.Unsupported
                    if (!isLiteralGround(left)) return UnifyResult.Unsupported
                    if (!bind(argGround, rArg, left)) return UnifyResult.Conflict
                }
                lArg != null -> {
                    if (!config.directedEval) return UnifyResult.Unsupported
                    // Directed-eval only for int arithmetic over args + literals
                    if (!looksLikeIntArithmetic(right)) return UnifyResult.Unsupported
                    pendingFunctional.add(lArg to right)
                }
                rArg != null -> {
                    if (!config.directedEval) return UnifyResult.Unsupported
                    if (!looksLikeIntArithmetic(left)) return UnifyResult.Unsupported
                    pendingFunctional.add(rArg to left)
                }
                isValueGround(left) && isValueGround(right) -> {
                    if (!groundEqual(left, right)) return UnifyResult.Conflict
                }
                else -> return UnifyResult.Unsupported
            }
        }

        if (config.directedEval || config.argOwnership || config.eqUnify) {
            var progress = true
            var steps = 0
            while (progress && steps++ < 64) {
                progress = false
                for ((arg, expr) in pendingFunctional) {
                    if (argGround.containsKey(arg)) {
                        val evaluated = tryEval(expr, argGround, ctx)
                        if (evaluated != null && isLiteralGround(evaluated)) {
                            if (!bind(argGround, arg, evaluated)) return UnifyResult.Conflict
                        }
                        continue
                    }
                    val evaluated = tryEval(expr, argGround, ctx) ?: continue
                    if (!isLiteralGround(evaluated)) continue
                    if (!bind(argGround, arg, evaluated)) return UnifyResult.Conflict
                    progress = true
                }
            }
            // If directed-eval left unbound functionals, do not claim success for concrete extract;
            // satisfiability may still be true (caller distinguishes via arg coverage).
            if (pendingFunctional.any { (arg, _) -> !argGround.containsKey(arg) }) {
                // Still OK for SAT if no conflicts; concrete extract will fall back.
            }
        }

        return UnifyResult.Ok(Bindings(argGround))
    }

    private fun isLiteralGround(e: Expr<*>): Boolean {
        if (e.isNumeral || e.isTrue || e.isFalse) return true
        if (e.isString) return true
        val s = e.toString()
        if (s.length >= 2 && s.startsWith("\"") && s.endsWith("\"")) return true
        return false
    }

    private fun looksLikeIntArithmetic(e: Expr<*>): Boolean {
        if (argConstName(e) != null) return true
        if (e is IntNum) return true
        if (!e.isApp || e.numArgs == 0) return false
        val name = e.funcDecl.name.toString()
        if (name !in setOf("+", "-", "*")) return false
        return e.args.all { looksLikeIntArithmetic(it) }
    }

    private fun bind(map: MutableMap<String, Expr<*>>, key: String, ground: Expr<*>): Boolean {
        val existing = map[key] ?: run {
            map[key] = ground
            return true
        }
        return groundEqual(existing, ground)
    }

    private fun metaName(e: Expr<*>): String? {
        if (!e.isConst) return null
        val name = e.funcDecl.name.toString().trim('|')
        return name.takeIf { it == META_PROVIDER_CLIENT || it == META_CLASS_ID }
    }

    private fun argConstName(e: Expr<*>): String? {
        if (!e.isConst) return null
        val name = e.funcDecl.name.toString().trim('|')
        if (name == META_PROVIDER_CLIENT || name == META_CLASS_ID) return null
        return name
    }

    private fun isValueGround(e: Expr<*>): Boolean {
        if (e.isNumeral || e.isTrue || e.isFalse) return true
        if (e.isString) return true
        if (argConstName(e) != null || metaName(e) != null) return false
        if (e.isApp && e.numArgs > 0) return e.args.all { isValueGround(it) }
        // 0-ary non-arg const (e.g. embedded datatype value) — treat as ground
        if (e.isConst) return true
        return false
    }

    private fun groundEqual(a: Expr<*>, b: Expr<*>): Boolean {
        if (a === b || a == b) return true
        if (a is IntNum && b is IntNum) return a.int == b.int
        if (a.isTrue && b.isTrue) return true
        if (a.isFalse && b.isFalse) return true
        val sa = a.simplify()
        val sb = b.simplify()
        if (sa is IntNum && sb is IntNum) return sa.int == sb.int
        if (sa.isTrue && sb.isTrue) return true
        if (sa.isFalse && sb.isFalse) return true
        return sa.toString() == sb.toString()
    }

    private fun tryEval(expr: Expr<*>, env: Map<String, Expr<*>>, ctx: Context): Expr<*>? {
        argConstName(expr)?.let { return env[it] }
        if (isValueGround(expr)) return expr
        if (!expr.isApp || expr.numArgs == 0) return null
        val args = expr.args.map { tryEval(it, env, ctx) ?: return null }
        if (args.all { it is IntNum }) {
            val ints = args.map { (it as IntNum).int }
            return when (expr.funcDecl.name.toString()) {
                "+" -> if (ints.size == 2) ctx.mkInt(ints[0] + ints[1]) else null
                "-" -> when (ints.size) {
                    1 -> ctx.mkInt(-ints[0])
                    2 -> ctx.mkInt(ints[0] - ints[1])
                    else -> null
                }
                "*" -> if (ints.size == 2) ctx.mkInt(ints[0] * ints[1]) else null
                else -> null
            }
        }
        return null
    }

    private fun concreteValue(expr: Expr<*>, type: Type, ctx: Context): Value? {
        val simplified = expr.simplify()
        return try {
            when (type) {
                is IntType -> {
                    val n = simplified as? IntNum ?: return null
                    Value(n.int, type)
                }
                is BoolType -> when {
                    simplified.isTrue -> Value(true, type)
                    simplified.isFalse -> Value(false, type)
                    else -> null
                }
                is StringType -> {
                    val v = stringFromExpr(simplified) ?: return null
                    Value(v, type)
                }
                else -> withLocalModel(ctx, simplified, type)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun stringFromExpr(expr: Expr<*>): String? {
        val s = expr.toString()
        if (s.length >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
            return s.substring(1, s.length - 1)
        }
        return null
    }

    private fun withLocalModel(ctx: Context, expr: Expr<*>, type: Type): Value? {
        return try {
            val solver = ctx.mkSolver()
            val fresh = type.toZ3Expr(Variable("__sync_opt_tmp", type), ctx)
            @Suppress("UNCHECKED_CAST")
            solver.add(
                ctx.mkEq(
                    fresh as Expr<com.microsoft.z3.Sort>,
                    expr as Expr<com.microsoft.z3.Sort>,
                ),
            )
            if (solver.check() != Status.SATISFIABLE) return null
            val model = solver.model
            Value(type.fromZ3Expr(model.eval(fresh, true), model), type)
        } catch (_: Exception) {
            null
        }
    }
}
