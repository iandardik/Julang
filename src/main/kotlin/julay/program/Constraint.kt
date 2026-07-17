package julay.program

import com.microsoft.z3.BoolExpr
import com.microsoft.z3.Context

/**
 * Sync constraint for a [julay.concurrency.SyncChannel] participant: a Z3 [expr] plus the
 * live [Channel] objects this proc can resolve by id when building a [ConcreteAction].
 */
data class Constraint(
    val expr: BoolExpr,
    val channels: Set<Channel> = emptySet(),
) {
    /**
     * Clones [expr] into [ctx] via [com.microsoft.z3.Expr.translate], preserving [channels].
     * Used to give each Select case its own Context so concurrent SyncChannels do not race
     * on a shared Proc step Context (see [julay.concurrency.Select] and [Proc.runOneStep]).
     */
    fun cloneInto(ctx: Context): Constraint =
        Constraint(expr.translate(ctx) as BoolExpr, channels)
}
