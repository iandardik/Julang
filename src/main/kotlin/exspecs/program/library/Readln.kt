package exspecs.program.library

import com.microsoft.z3.Context
import exspecs.program.*
import exspecs.tools.mkStringConst

class ReadlnTS : TransitionSystem {
    companion object: StaticInfo {
        val msgArg = Variable("msg", stringType)
        val promptAct = SymbolicAction("prompt", listOf())
        val readlnAct = SymbolicAction("readln", listOf(msgArg))
        val initiallyCtor = Pair(SymbolicAction("initially", listOf())) { act : ConcreteAction -> ReadlnTS() }
        // the $ in the name means that programs cannot create p-classes whose names conflict with this one
        override fun staticInfo() = TransitionSystemStaticInfo("ReadlnTS$", setOf(promptAct, readlnAct), mapOf(initiallyCtor), false)
    }

    private val ctx = Context()
    private var prompt = true
    private var msg = ""

    override fun actions() = setOf(
        TSAction(
            promptAct,
            ctx.mkEq(ctx.mkBool(prompt), ctx.mkBool(true))
        ),
        TSAction(
            readlnAct,
            ctx.mkAnd(
                ctx.mkEq(ctx.mkBool(prompt), ctx.mkBool(false)),
                ctx.mkEq(ctx.mkStringConst("msg"), ctx.mkString(msg))
            )
        ),
    )
    override fun transit(act: ConcreteAction) {
        if (act.symAction == promptAct) {
            prompt = false
            msg = readln()
        }
        else {
            prompt = true
        }
    }
    override fun getContext() = ctx
}
