package julay.program.library

import com.microsoft.z3.Context
import julay.ast.ActionDecl
import julay.ast.LibraryLoc
import julay.program.ConcreteAction
import julay.program.Program
import julay.program.StaticInfo
import julay.program.SymbolicAction
import julay.program.TSAction
import julay.program.TransitionSystem
import julay.program.TransitionSystemStaticInfo
import julay.program.Variable
import julay.program.intType
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

class TimerTS : TransitionSystem {
    companion object: StaticInfo {
        val timeArg = Variable("time", intType)
        val timerAct = SymbolicAction("timer", listOf(timeArg))
        val timeoutAct = SymbolicAction("timeout", listOf())
        val initiallyCtor = Pair(SymbolicAction("initially", listOf())) { _ : Program, _ : ConcreteAction -> TimerTS() }
        // the $ in the name means that programs cannot create p-classes whose names conflict with this one
        override fun staticInfo() = TransitionSystemStaticInfo(
            "TimerTS$",
            setOf(timerAct, timeoutAct),
            mapOf(initiallyCtor))
        val actionDecls = listOf(
            ActionDecl(timerAct, listOf(), mapOf(), TSAction.SyncRole.CSP, LibraryLoc("Timer")),
            ActionDecl(timeoutAct, listOf(), mapOf(), TSAction.SyncRole.CSP, LibraryLoc("Timer")),
        )
    }

    private val ctx = Context()
    private val z3True = ctx.mkTrue()
    private var timing = false

    override suspend fun actions() : Set<TSAction> {
        return if (timing) {
            setOf(TSAction(timeoutAct, z3True))
        } else {
            setOf(TSAction(timerAct, z3True))
        }
    }
    override suspend fun transit(act: ConcreteAction) {
        when (act.symAction) {
            timerAct -> {
                timing = true
                val time = act.lookup(timeArg).value as Int
                delay(time.seconds)
            }
            timeoutAct -> timing = false
            else -> RuntimeException("Unsupported action ${act.symAction}")
        }
    }
    override fun getContext() = ctx
}
