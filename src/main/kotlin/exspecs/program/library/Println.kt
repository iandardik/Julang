package exspecs.program.library

import com.microsoft.z3.Context
import exspecs.program.*

class PrintlnTS : TransitionSystem {
    companion object: StaticInfo {
        val msgArg = Variable("msg", stringType)
        val printlnAct = ActionSignature("println", listOf(msgArg))
        val initiallyCtor = Pair(ActionSignature("initially", listOf())) { act : ConcreteAction -> PrintlnTS() }
        override fun staticInfo() = TransitionSystemStaticInfo(setOf(printlnAct), mapOf(initiallyCtor), false)
    }

    private val ctx = Context()
    private val alphabet = setOf(SymbolicAction(printlnAct, ctx.mkTrue()))
    override fun actions() = alphabet
    override fun transit(act: ConcreteAction) {
        val msg = act.lookup(msgArg)
        println(msg)
    }
    override fun getContext() = ctx
}
