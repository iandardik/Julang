package julay.program

import com.microsoft.z3.Status
import com.microsoft.z3.Context
import julay.concurrency.Select
import java.util.*

class Proc(
    private val transitionSystem: TransitionSystem,
    private val tsInfo: TransitionSystemStaticInfo,
    private val actionTable: Map<SymbolicAction, ProgramAction>,
    private val program: Program,
) {
    suspend fun run() {
        while (true) {
            // Fresh Context per step so AST/Solver native refs are force-cleared on close.
            // Safe once Select waits for loser-case SyncChannel cleanup (cancelAndJoin).
            val cont = withEphemeralContextSuspend { ctx ->
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
        val tsChannels = transitionSystem.heldChannels()
        // Z3 Contexts are not thread-safe. Select launches one coroutine per case on different
        // SyncChannels; those threads call BoolExpr.translate into scratch contexts during
        // pairwise SAT. If every case shared this step [ctx], concurrent translates race on
        // native state and can crash the JVM (intermittent raft / multi-case Select failures).
        // Clone each case's constraint+anticonstraint into a Case-local Context *before*
        // Select construction / coroutine launch (see Select.run docs). Close case Contexts
        // only after Select.run returns (cancelAndJoin has scrubbed waiters).
        val caseCtxs = mutableListOf<Context>()
        try {
            val cases = enabledActions.map { act ->
                val syncChannel = if (act.channel != null) {
                    act.channel.requireOpenSyncChannel()
                } else {
                    actionTable[act.symAction]!!.channel
                }
                val caseCtx = Context().also { caseCtxs.add(it) }
                val constraint = buildSet {
                    addAll(tsChannels)
                    act.channel?.let { add(it) }
                }.let { Constraint(act.guard, it).cloneInto(caseCtx) }
                // Default/Internal: processes from the same p-class never sync.
                // Service/Consumer: service vs client rendezvous (serviceTransition).
                val anticonstraintExpr = when (act.syncRole) {
                    TSAction.SyncRole.Default, TSAction.SyncRole.Internal ->
                        ctx.mkEq(ctx.mkIntConst("classID"), ctx.mkInt(tsInfo.classID()))
                    TSAction.SyncRole.Service ->
                        ctx.mkEq(ctx.mkBoolConst("serviceTransition"), ctx.mkTrue())
                    TSAction.SyncRole.Consumer ->
                        ctx.mkEq(ctx.mkBoolConst("serviceTransition"), ctx.mkFalse())
                }
                val anticonstraint = Constraint(anticonstraintExpr).cloneInto(caseCtx)
                Select.SyncCase(syncChannel, constraint, anticonstraint) { concAct: ConcreteAction ->
                    nextAct = Optional.of(concAct)
                }
            }
            Select(*cases.toTypedArray()).run()
        } finally {
            caseCtxs.forEach { caseCtx ->
                ContextLocalCache.dropContext(caseCtx)
                caseCtx.close()
            }
        }

        // check for "static" deadlocks
        if (nextAct.isEmpty) {
            return false
        }

        val act = nextAct.get()
        // transit to the next state
        transitionSystem.transit(act)
        // Constructor peer is a compile-time abstraction: spawn locally after the transition.
        if (program.isConstructorAction(act.symAction)) {
            program.spawn(act)
        }
        return true
    }
}
