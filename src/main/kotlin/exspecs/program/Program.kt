package exspecs.program

import com.microsoft.z3.BoolExpr
import com.microsoft.z3.Context
import com.microsoft.z3.Status
import exspecs.concurrency.SyncChannel
import java.util.*

/**
 * A program represents one or more processes that interact together on a single computer.
 */
class Program : Runnable {
    private val constructorProc : Proc

    /**
     * The constructor sets up a channel for each SymbolicAction so that each process that engages in the action can
     * communicate (synchronize on args) over the channel.
     */
    constructor(componentInfo : Set<TransitionSystemStaticInfo>) {
        // all action signatures that have the same name should have the same param
        // TODO add a sanity check for the above requirement
        // no transition should be for initially (only constructors)
        // TODO add a sanity check for the above requirement

        val constructorCtx = Context()
        val initiallySig = ActionSignature("initially", listOf())
        val initiallyAction = SymbolicAction(initiallySig, constructorCtx.mkTrue())

        // create a SyncChannel for each action
        val actionBag = componentInfo.flatMap { it.alphabet union it.constructors }
        val actionCounts = actionBag.toSet()
            .associateWith { setAct -> actionBag.count { bagAct -> bagAct == setAct } }
            .toMutableMap()
        // the initially action is a self-sync for the constructor proc
        actionCounts[initiallySig] = 1

        val channelTable = actionCounts.keys.associateWith { act ->
            val syncSize = actionCounts[act]!!
            val ctx = Context() // one Context per channel
            SyncChannel<ConcreteAction,BoolExpr>(syncSize) { constraints ->
                val solver = ctx.mkSolver()
                // c.translate(ctx) is key because each constraint will come from a different thread, and hence are
                // created by different Contexts.
                constraints.forEach { c -> solver.add(c.translate(ctx)) }
                if (solver.check() == Status.SATISFIABLE) {
                    Optional.of(ConcreteAction(act, ctx, solver.model))
                } else {
                    Optional.empty()
                }
            }
        }

        val constructors = componentInfo
            .flatMap { c -> c.constructors.map { act -> Pair(act, c) } }
            .toSet()
        constructorProc = Proc(ConstructorTransitionSystem(initiallyAction, constructors, channelTable, constructorCtx), channelTable)
    }

    override fun run() {
        // the constructor proc is responsible for terminating the program when all 'self terminating' procs are done
        constructorProc.run()
    }
}
