package julay.program

import com.microsoft.z3.Status
import com.microsoft.z3.BoolExpr
import julay.concurrency.Select
import julay.concurrency.SyncChannel
import java.util.*

class Proc(
    private val transitionSystem : TransitionSystem,
    private val tsInfo : TransitionSystemStaticInfo,
    private val actionTable : Map<SymbolicAction,ProgramAction>
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
            val cases = enabledActions.map { act ->
                val programAction = actionTable[act.symAction]!!
                // the first anticonstraint ensures that processes from the same p-class never sync
                // the second ensures that service transitions act like servers, and all others act like clients
                val anticonstraint = if (programAction.isServiced) {
                    ctx.mkEq(ctx.mkBoolConst("serviceTransition"), ctx.mkBool(act.isServicer))
                } else {
                    ctx.mkEq(ctx.mkIntConst("classID"), ctx.mkInt(tsInfo.classID()))
                }
                Select.SyncCase(programAction.channel, act.guard, anticonstraint) { concAct : ConcreteAction ->
                    nextAct = Optional.of(concAct)
                }
            }
            // TODO
            //Select(*cases.toTypedArray()).run()

            // check for deadlocks
            if (nextAct.isEmpty) {
                return
            }

            // transit to the next state
            transitionSystem.transit(nextAct.get())
        }
    }
}
