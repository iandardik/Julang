package julay.program

import com.microsoft.z3.Status
import com.microsoft.z3.Context
import julay.concurrency.Select
import java.util.*

class Proc(
    private val transitionSystem : TransitionSystem,
    private val tsInfo : TransitionSystemStaticInfo,
    private val actionTable : Map<SymbolicAction,ProgramAction>
) {
    suspend fun run() {
        // Long-lived Context: SyncChannel may still hold guards/anticonstraints from this
        // Context while peer procs are waiting, so it must stay open for the process lifetime.
        Context().use { ctx ->
            runUsingCtx(ctx)
        }
    }

    private suspend fun runUsingCtx(ctx: Context) {
        // Reuse one Solver for the process lifetime (avoids mkSolver-per-check native growth).
        val solver = ctx.mkSolver()
        while (true) {
            var nextAct = Optional.empty<ConcreteAction>()
            val enabledActions = transitionSystem.actions(ctx).filter { act ->
                solver.reset()
                solver.add(act.guard)
                // deadlock is not enabled, but we let it pass on purpose to create a deadlock
                act.symAction.name == "deadlock" || solver.check() == Status.SATISFIABLE
            }
            val cases = enabledActions.map { act ->
                val programAction = actionTable[act.symAction]!!
                // the first anticonstraint ensures that processes from the same p-class never sync
                // the second ensures that service transitions act like servers, and all others act like clients
                val anticonstraint = when (act.syncRole) {
                    TSAction.SyncRole.CSP -> ctx.mkEq(ctx.mkIntConst("classID"), ctx.mkInt(tsInfo.classID()))
                    TSAction.SyncRole.P2PService -> ctx.mkEq(ctx.mkBoolConst("serviceTransition"), ctx.mkTrue())
                    TSAction.SyncRole.P2PConsumer -> ctx.mkEq(ctx.mkBoolConst("serviceTransition"), ctx.mkFalse())
                }
                Select.SyncCase(programAction.channel, act.guard, anticonstraint) { concAct : ConcreteAction ->
                    nextAct = Optional.of(concAct)
                }
            }
            Select(*cases.toTypedArray()).run()

            // check for "static" deadlocks
            if (nextAct.isEmpty) {
                return
            }

            // transit to the next state
            transitionSystem.transit(nextAct.get())
        }
    }
}
