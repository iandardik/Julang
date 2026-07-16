package julay.program

import com.microsoft.z3.BoolExpr
import com.microsoft.z3.Context
import com.microsoft.z3.Status
import julay.concurrency.SyncChannel
import java.util.*

/**
 * A program represents one or more processes that interact together on a single computer.
 */
class Program {
    val actionTable : Map<SymbolicAction,ProgramAction>
    private val constructorProc : Proc

    /**
     * The constructor sets up a channel for each TSAction so that each process that engages in the action can
     * communicate (synchronize on args) over the channel.
     */
    constructor(componentInfo : Set<TransitionSystemStaticInfo>, cliArgs : List<String> = emptyList()) {
        // assumptions/requirements (enforced by the compiler):
        // - all action signatures that have the same name should have the same params
        // - no transition should be for initially (only constructors)
        // - internal actions: sync size 1; all other actions: sync size 2
        // - service actions: at most one servicer; consumers are untagged defaults
        // - a service with no consumers is a legal intentional deadlock (warned at compile time)

        val argsVar = Variable("args", listType(stringType))
        val initially = SymbolicAction("initially", listOf(argsVar))
        val initiallyConcrete = ConcreteAction(
            initially,
            mapOf(argsVar to Value(cliArgs, listType(stringType))),
        )

        // create a SyncChannel for each action
        val allActions = componentInfo
            .flatMap { it.alphabet union it.constructors.keys }
            .toSet()
        val actionCounts = allActions
            .associateWith { setAct ->
                if (setAct.isInternal) 1 else 2
            }
            .toMutableMap()
        // the initially action is a self-sync for the constructor proc
        actionCounts[initially] = 1

        val channelTable = actionCounts.keys.associateWith { act ->
            val syncSize = actionCounts[act]!!
            if (act == initially) {
                SyncChannel<ConcreteAction,BoolExpr>(syncSize) { _ ->
                    Optional.of(initiallyConcrete)
                }
            } else {
                // Ephemeral Z3 Contexts: channels live for the program lifetime, but each SAT /
                // model extraction uses a scratch Context that is closed immediately after.
                // (ConcreteAction copies assignments into plain Java Values.)
                // c.translate(ctx) is required because each constraint comes from a different
                // proc thread / Context.
                fun constraintsSatisfiable(constraints: Set<BoolExpr>): Boolean =
                    Context().use { ctx ->
                        val solver = ctx.mkSolver()
                        constraints.forEach { c -> solver.add(c.translate(ctx) as BoolExpr) }
                        solver.check() == Status.SATISFIABLE
                    }
                SyncChannel(
                    syncSize,
                    satisfiable = ::constraintsSatisfiable,
                    compute = { constraints ->
                        Context().use { ctx ->
                            val solver = ctx.mkSolver()
                            constraints.forEach { c -> solver.add(c.translate(ctx) as BoolExpr) }
                            if (solver.check() != Status.SATISFIABLE) {
                                Optional.empty()
                            } else if (act.args.isEmpty()) {
                                // Avoid allocating a Model when no args need extraction.
                                Optional.of(ConcreteAction(act, emptyMap()))
                            } else {
                                // Extract ConcreteAction (Kotlin Values only) before Context closes.
                                Optional.of(ConcreteAction(act, ctx, solver.model))
                            }
                        }
                    },
                )
            }
        }

        actionTable = channelTable.keys
            .associateWith { ProgramAction(it, channelTable[it]!!) }
        constructorProc = Proc(
            ConstructorTransitionSystem(initially, componentInfo, this),
            ConstructorTransitionSystem.staticInfo(),
            actionTable
        )
    }

    suspend fun run() {
        constructorProc.run()
    }
}
