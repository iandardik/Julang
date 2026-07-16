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
        while (true) {
            // Fresh Context per step so AST/Solver native refs are force-cleared on close.
            // Safe once Select waits for loser-case SyncChannel cleanup (cancelAndJoin).
            val cont = Context().use { ctx ->
                runOneStep(ctx)
            }
            if (!cont) return
        }
    }

    /**
     * Runs one select/transit step using [ctx].
     * @return true to continue the process loop, false on deadlock / empty select.
     */
    private suspend fun runOneStep(ctx: Context): Boolean {
        // One Solver per step (avoids mkSolver-per-check native growth); reset between checks.
        val solver = ctx.mkSolver()
        var nextAct = Optional.empty<ConcreteAction>()
        val enabledActions = transitionSystem.actions(ctx).filter { act ->
            solver.reset()
            solver.add(act.guard)
            solver.check() == Status.SATISFIABLE
        }
        val cases = enabledActions.map { act ->
            val programAction = actionTable[act.symAction]!!
            // Default/Internal: processes from the same p-class never sync.
            // Service/Consumer: service vs client rendezvous (serviceTransition).
            val anticonstraint = when (act.syncRole) {
                TSAction.SyncRole.Default, TSAction.SyncRole.Internal ->
                    ctx.mkEq(ctx.mkIntConst("classID"), ctx.mkInt(tsInfo.classID()))
                TSAction.SyncRole.Service ->
                    ctx.mkEq(ctx.mkBoolConst("serviceTransition"), ctx.mkTrue())
                TSAction.SyncRole.Consumer ->
                    ctx.mkEq(ctx.mkBoolConst("serviceTransition"), ctx.mkFalse())
            }
            Select.SyncCase(programAction.channel, act.guard, anticonstraint) { concAct : ConcreteAction ->
                nextAct = Optional.of(concAct)
            }
        }
        Select(*cases.toTypedArray()).run()

        // check for "static" deadlocks
        if (nextAct.isEmpty) {
            return false
        }

        // transit to the next state
        transitionSystem.transit(nextAct.get())
        return true
    }
}
