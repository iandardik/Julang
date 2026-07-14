package julay.program

import io.github.cvc5.Kind
import io.github.cvc5.TermManager
import julay.concurrency.Select
import julay.tools.SmtConstraint
import julay.tools.isSat
import julay.tools.newModelSolver
import java.util.Optional

class Proc(
    private val transitionSystem: TransitionSystem,
    private val tsInfo: TransitionSystemStaticInfo,
    private val actionTable: Map<SymbolicAction, ProgramAction>,
) {
    suspend fun run() {
        // One TermManager per Proc (like the former per-Proc Z3 Context).
        val tm = TermManager()
        DatatypeBinder.withBinder(tm) {
            runUsingTm(tm)
        }
    }

    suspend fun runUsingTm(tm: TermManager) {
        while (true) {
            var nextAct = Optional.empty<ConcreteAction>()
            val enabledActions = transitionSystem.actions(tm).filter { act ->
                val solver = newModelSolver(tm)
                try {
                    solver.assertFormula(act.guard)
                    // deadlock is not enabled, but we let it pass on purpose to create a deadlock
                    act.symAction.name == "deadlock" || solver.isSat()
                } finally {
                    try {
                        solver.deletePointer()
                    } catch (_: Exception) {
                    }
                }
            }
            val cases = enabledActions.map { act ->
                val programAction = actionTable[act.symAction]!!
                // the first anticonstraint ensures that processes from the same p-class never sync
                // the second ensures that service transitions act like servers, and all others act like clients
                val anticonstraintTerm = when (act.syncRole) {
                    TSAction.SyncRole.CSP ->
                        tm.mkTerm(
                            Kind.EQUAL,
                            tm.mkConst(tm.integerSort, "classID"),
                            tm.mkInteger(tsInfo.classID().toLong()),
                        )
                    TSAction.SyncRole.P2PService ->
                        tm.mkTerm(
                            Kind.EQUAL,
                            tm.mkConst(tm.booleanSort, "serviceTransition"),
                            tm.mkTrue(),
                        )
                    TSAction.SyncRole.P2PConsumer ->
                        tm.mkTerm(
                            Kind.EQUAL,
                            tm.mkConst(tm.booleanSort, "serviceTransition"),
                            tm.mkFalse(),
                        )
                }
                Select.SyncCase(
                    programAction.channel,
                    SmtConstraint.from(act.guard),
                    SmtConstraint.from(anticonstraintTerm),
                ) { concAct: ConcreteAction ->
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
