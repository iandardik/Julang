package julay.program

import julay.tools.SmtConstraint
import julay.tools.solveToConcreteAction
import julay.concurrency.SyncChannel
import java.util.Optional

/**
 * A program represents one or more processes that interact together on a single computer.
 */
class Program {
    val actionTable: Map<SymbolicAction, ProgramAction>
    private val constructorProc: Proc

    /**
     * The constructor sets up a channel for each TSAction so that each process that engages in the action can
     * communicate (synchronize on args) over the channel.
     */
    constructor(componentInfo: Set<TransitionSystemStaticInfo>, cliArgs: List<String> = emptyList()) {
        // assumpmtions/requirements:
        // - all action signatures that have the same name should have the same param
        // - no transition should be for initially (only constructors)
        // - for any serviced action, it must have at least one servicer and at least one non-servicer
        // TODO based on ^^ we should really call them client/server instead
        // - a TS who claims to be a servicer of an action must have that action in its alphabet
        // TODO add a sanity check for each of the above requirements

        val argsVar = Variable("args", listType(stringType))
        val initially = SymbolicAction("initially", listOf(argsVar), SymbolicAction.SyncType.CSP)
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
                SyncChannel<ConcreteAction, SmtConstraint>(syncSize) { _ ->
                    Optional.of(initiallyConcrete)
                }
            } else {
                // Fresh TermManager per sync; disposed inside solveToConcreteAction after model extract.
                SyncChannel<ConcreteAction, SmtConstraint>(syncSize) { constraints ->
                    Optional.ofNullable(solveToConcreteAction(constraints, act))
                }
            }
        }

        actionTable = channelTable.keys
            .associateWith { ProgramAction(it, channelTable[it]!!) }
        constructorProc = Proc(
            ConstructorTransitionSystem(initially, componentInfo, this),
            ConstructorTransitionSystem.staticInfo(),
            actionTable,
        )
    }

    suspend fun run() {
        constructorProc.run()
    }
}
