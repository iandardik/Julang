package julay.program

import com.microsoft.z3.BoolExpr

/**
 * Sync constraint for a [julay.concurrency.SyncChannel] participant: a Z3 [expr] plus the
 * live [Channel] objects this proc can resolve by id when building a [ConcreteAction].
 */
data class Constraint(
    val expr: BoolExpr,
    val channels: Set<Channel> = emptySet(),
)
