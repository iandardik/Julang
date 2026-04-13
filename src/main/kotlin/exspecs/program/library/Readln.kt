package exspecs.program.library

import com.microsoft.z3.Context
import exspecs.program.*
import exspecs.tools.mkStringConst

class ReadlnTS : TransitionSystem {
    companion object: StaticInfo {
        val msgArg = Variable("msg", stringType)
        val promptAct = ActionSignature("prompt", listOf())
        val readlnAct = ActionSignature("readln", listOf(msgArg))
        val initiallyCtor = Pair(ActionSignature("initially", listOf())) { act : ConcreteAction -> ReadlnTS() }
        override fun staticInfo() = TransitionSystemStaticInfo(setOf(promptAct, readlnAct), mapOf(initiallyCtor), false)
    }

    private val ctx = Context()
    private var prompt = true
    private var msg = ""

    override fun actions() = setOf(
        SymbolicAction(
            promptAct,
            ctx.mkEq(ctx.mkBool(prompt), ctx.mkBool(true))
        ),
        SymbolicAction(
            readlnAct,
            ctx.mkAnd(
                ctx.mkEq(ctx.mkBool(prompt), ctx.mkBool(false)),
                ctx.mkEq(ctx.mkStringConst("msg"), ctx.mkString(msg))
            )
        ),
    )
    override fun transit(act: ConcreteAction) {
        if (act.signature == promptAct) {
            prompt = false
            msg = readln()
        }
        else {
            prompt = true
        }
    }
    override fun getContext() = ctx
}
