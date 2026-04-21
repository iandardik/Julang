package julay.program.library

import com.microsoft.z3.Context
import julay.program.*

class PrintlnTS : TransitionSystem {
    companion object: StaticInfo {
        val msgArg = Variable("msg", stringType)
        val printlnAct = SymbolicAction("println", listOf(msgArg), SymbolicAction.SyncType.P2P)
        val initiallyCtor = Pair(SymbolicAction("initially", listOf()))
            { _ : Program, _ : ConcreteAction -> PrintlnTS() }
        // the $ in the name means that programs cannot create p-classes whose names conflict with this one
        override fun staticInfo() = TransitionSystemStaticInfo(
            "PrintlnTS$",
            setOf(printlnAct),
            mapOf(initiallyCtor),
            false)
    }

    private val ctx = Context()
    private val alphabet = setOf(TSAction(printlnAct, ctx.mkTrue(), TSAction.SyncRole.P2PService))
    override suspend fun actions() = alphabet
    override suspend fun transit(act: ConcreteAction) {
        val msg = act.lookup(msgArg)
        println(msg)
    }
    override fun getContext() = ctx
}
