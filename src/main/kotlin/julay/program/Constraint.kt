package julay.program

import com.microsoft.z3.BoolExpr
import com.microsoft.z3.Context
import julay.program.sync.SyncAnti
import julay.program.sync.BoolExprFast

/**
 * Sync constraint for a [julay.concurrency.SyncChannel] participant.
 *
 * At most one of these carries the “formula”:
 * - [expr] — Z3 [BoolExpr] (residual / classic path; needs a Context to build and check)
 * - [fast] — [BoolExprFast], the structured stand-in for BoolExpr when avoiding Z3
 * - [anti] — [SyncAnti] role meta (classID / provider-client), also without BoolExpr
 *
 * Process identity ([procId], [classId], [proc]) is rendezvous metadata, not part of the formula.
 */
data class Constraint(
    val expr: BoolExpr? = null,
    val fast: BoolExprFast? = null,
    val anti: SyncAnti? = null,
    val procId: Long = -1L,
    val classId: Int = -1,
    val proc: Proc? = null,
) {
    constructor(
        expr: BoolExpr,
        procId: Long = -1L,
        classId: Int = -1,
        proc: Proc? = null,
    ) : this(expr = expr, fast = null, anti = null, procId = procId, classId = classId, proc = proc)

    init {
        require(expr != null || fast != null || anti != null) {
            "Constraint needs expr, fast, and/or anti"
        }
    }

    /**
     * Clones [expr] into [ctx] via [com.microsoft.z3.Expr.translate].
     * [fast] / [anti] are Kotlin values and are copied by reference.
     */
    fun cloneInto(ctx: Context): Constraint =
        Constraint(
            expr = expr?.let { it.translate(ctx) as BoolExpr },
            fast = fast,
            anti = anti,
            procId = procId,
            classId = classId,
            proc = proc,
        )
}
