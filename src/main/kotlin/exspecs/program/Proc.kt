package exspecs.program

import com.microsoft.z3.Status
import com.microsoft.z3.BoolExpr
import exspecs.concurrency.Select
import exspecs.concurrency.SyncChannel
import java.util.*

class Proc(
    private val transitionSystem : TransitionSystem,
    private val tsInfo : TransitionSystemStaticInfo,
    private val channelTable : Map<SymbolicAction, SyncChannel<ConcreteAction, BoolExpr>>
) : Runnable {
    private val ctx = transitionSystem.getContext()

    override fun run() {
        while (true) {
            var nextAct = Optional.empty<ConcreteAction>()
            val enabledActions = transitionSystem.actions().filter {
                val solver = ctx.mkSolver()
                solver.add(it.guard)
                solver.check() == Status.SATISFIABLE
            }
            val cases = enabledActions.map { symAct ->
                val channel = channelTable[symAct.symAction]!!
                // the anticonstraint ensures that processes from the same p-class never sync
                val anticonstraint = ctx.mkEq(ctx.mkIntConst("classID"), ctx.mkInt(tsInfo.classID()))
                Select.SyncCase(channel, symAct.guard, anticonstraint) { concAct : ConcreteAction ->
                    nextAct = Optional.of(concAct)
                }
            }
            Select(*cases.toTypedArray()).run()

            // check for deadlocks
            if (nextAct.isEmpty) {
                return
            }

            // transit to the next state
            transitionSystem.transit(nextAct.get())
        }
    }
}
