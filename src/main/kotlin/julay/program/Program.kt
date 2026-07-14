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
        // assumpmtions/requirements:
        // - all action signatures that have the same name should have the same param
        // - no transition should be for initially (only constructors)
        // - for any serviced action, it must have at least one servicer and at least one non-servicer
        // TODO based on ^^ we should really call them client/server instead
        // - a TS who claims to be a servicer of an action must have that action in its alphabet
        // TODO add a sanity check for each of the above requirements

        val constructorCtx = Context()
        val argsVar = Variable("args", listType(stringType))
        val initially = SymbolicAction("initially", listOf(argsVar), SymbolicAction.SyncType.CSP)
        val initiallyAction = TSAction(initially, constructorCtx.mkTrue(), TSAction.SyncRole.CSP)
        val initiallyConcrete = ConcreteAction(
            initially,
            mapOf(argsVar to Value(cliArgs, listType(stringType))),
        )
        val deadlock = SymbolicAction("deadlock", listOf(), SymbolicAction.SyncType.CSP)

        // create a SyncChannel for each action
        // ConstructorTS offers each constructor action once and spawns every matching p-class,
        // so multiple constructor declarations of the same action count as 1 toward sync size.
        // CSP sync size is the full peer set (alphabet offers + one constructor offer when present).
        // Pairwise 1:1 rendezvous is the role of p2p actions, not CSP.
        val allActions = componentInfo
            .flatMap { it.alphabet union it.constructors.keys }
            .toSet()
        val actionCounts = allActions
            .associateWith { setAct ->
                if (setAct.syncType == SymbolicAction.SyncType.CSP) {
                    val transitionOffers = componentInfo.count { setAct in it.alphabet }
                    val constructorOffer = if (componentInfo.any { setAct in it.constructors }) 1 else 0
                    transitionOffers + constructorOffer
                } else {
                    2
                }
            }
            .toMutableMap()
        // the initially action is a self-sync for the constructor proc
        actionCounts[initially] = 1
        // the deadlock action should never sync
        actionCounts[deadlock] = Int.MAX_VALUE

        val channelTable = actionCounts.keys.associateWith { act ->
            val syncSize = actionCounts[act]!!
            if (act == initially) {
                SyncChannel<ConcreteAction,BoolExpr>(syncSize) { _ ->
                    Optional.of(initiallyConcrete)
                }
            } else {
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
        }

        actionTable = channelTable.keys
            .associateWith { ProgramAction(it, channelTable[it]!!) }
        constructorProc = Proc(
            ConstructorTransitionSystem(initiallyAction, componentInfo, this, constructorCtx),
            ConstructorTransitionSystem.staticInfo(),
            actionTable
        )
    }

    suspend fun run() {
        constructorProc.run()
    }
}
