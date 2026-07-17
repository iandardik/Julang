package julay.program

import com.microsoft.z3.BoolExpr
import com.microsoft.z3.Context

/**
 * Sync constraint for a [julay.concurrency.SyncChannel] participant: a Z3 [expr] plus
 * process identity metadata contributed only for this rendezvous (not a global registry).
 */
data class Constraint(
    val expr: BoolExpr,
    val procId: Long = -1L,
    val classId: Int = -1,
) {
    /**
     * Clones [expr] into [ctx] via [com.microsoft.z3.Expr.translate], preserving process metadata.
     */
    fun cloneInto(ctx: Context): Constraint =
        Constraint(expr.translate(ctx) as BoolExpr, procId, classId)
}
