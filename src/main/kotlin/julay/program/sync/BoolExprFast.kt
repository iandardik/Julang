package julay.program.sync

import julay.program.action.SymbolicAction
import julay.program.action.TSAction
import julay.concurrency.SyncChannel
import julay.program.Constraint
import julay.program.action.SyncPayload

/**
 * Fast-path analogue of Z3's [com.microsoft.z3.BoolExpr]: a Kotlin-structured action guard used
 * when Julay can decide enablement / sync compatibility / arg binding **without** allocating a
 * Z3 [com.microsoft.z3.Context] or calling the solver.
 *
 * That optimization (see [SyncStepPlan.FastOnly], [SyncResolveFast], [Constraint.fast]) only
 * covers equality-shaped / directed-eval guards. Relational, HOF, collection, and other opaque
 * shapes still use residual Z3 [com.microsoft.z3.BoolExpr]s on [Constraint.expr].
 *
 * Lifecycle: codegen/libraries build a form that may mention [SyncTerm.Local] state vars;
 * [SyncResolveFast.groundForOffer] evaluates locals and drops enablement-only atoms so the
 * value offered on a [julay.concurrency.SyncChannel] is arg/ground equations only.
 */
sealed class BoolExprFast {
    data object True : BoolExprFast()

    data class And(val parts: List<BoolExprFast>) : BoolExprFast() {
        init {
            require(parts.isNotEmpty()) { "And requires at least one part" }
        }
    }

    data class Eq(val left: SyncTerm, val right: SyncTerm) : BoolExprFast()

    /**
     * Local enablement only (`~done`): must be false for the action to be offered.
     * Grounded away before the constraint is placed on a SyncChannel.
     */
    data class NotLocalBool(val name: String) : BoolExprFast()

    /**
     * Local enablement only (`done`): must be true for the action to be offered.
     * Grounded away before the constraint is placed on a SyncChannel.
     */
    data class LocalBool(val name: String) : BoolExprFast()
}

/** Terms inside [BoolExprFast.Eq] / arithmetic — Kotlin values, not Z3 ASTs. */
sealed class SyncTerm {
    data class Arg(val name: String, val sort: Sort = Sort.Int) : SyncTerm() {
        enum class Sort { Int, Bool, String }
    }
    /** Proc state variable; substituted to [Ground] at offer time. */
    data class Local(val name: String) : SyncTerm()
    data class Ground(val value: SyncGround) : SyncTerm()
    data class IntArith(val op: Op, val left: SyncTerm, val right: SyncTerm) : SyncTerm() {
        enum class Op { Add, Sub, Mul }
    }
    /** Stringify [inner] (the `e + ""` / `"" + e` coerce after empty-concat elision). */
    data class ToString(val inner: SyncTerm) : SyncTerm()
}

/** Concrete literal carried by [SyncTerm.Ground]. */
sealed class SyncGround {
    data class IntVal(val v: Int) : SyncGround()
    data class BoolVal(val v: Boolean) : SyncGround()
    data class StringVal(val v: String) : SyncGround()

    fun asKotlin(): Any = when (this) {
        is IntVal -> v
        is BoolVal -> v
        is StringVal -> v
    }
}

/**
 * Sync-role exclusivity without a Z3 [BoolExpr] anticonstraint
 * (`classID = …` / `providerClientTransition = …` in the residual path).
 *
 * [julay.concurrency.SyncChannel] still treats peers as compatible when their antis are
 * **not** jointly satisfiable (`!antiSatisfiable`), matching the BoolExpr encoding.
 */
sealed class SyncAnti {
    data class ClassId(val classId: Int) : SyncAnti()
    data class ProviderClient(val isProvider: Boolean) : SyncAnti()

    companion object {
        fun fromRole(role: TSAction.SyncRole, classId: Int): SyncAnti = when (role) {
            TSAction.SyncRole.Default, TSAction.SyncRole.Internal -> ClassId(classId)
            TSAction.SyncRole.Provider -> ProviderClient(isProvider = true)
            TSAction.SyncRole.Client -> ProviderClient(isProvider = false)
        }
    }
}

/**
 * One enabled action ready for Select on the no-Z3 step path ([SyncStepPlan.FastOnly]):
 * [guard] is already locally grounded ([SyncResolveFast.groundForOffer]).
 */
data class FastOffer(
    val symAction: SymbolicAction,
    val guard: BoolExprFast,
    val syncRole: TSAction.SyncRole = TSAction.SyncRole.Default,
    val syncChannel: SyncChannel<SyncPayload, Constraint>? = null,
)

/**
 * How [julay.program.Proc] should run the next transition step.
 *
 * - [FastOnly]: every offered action has a [BoolExprFast]; Proc skips Z3 Context allocation
 *   for that step (enablement already done; constraints carry [Constraint.fast] only).
 * - [NeedsZ3]: fall back to building [com.microsoft.z3.BoolExpr] guards in a step Context (classic path).
 */
sealed class SyncStepPlan {
    data class FastOnly(val offers: List<FastOffer>) : SyncStepPlan()
    data object NeedsZ3 : SyncStepPlan()
}
