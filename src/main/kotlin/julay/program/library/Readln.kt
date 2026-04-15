package julay.program.library

import com.microsoft.z3.Context
import julay.program.*
import julay.tools.mkStringConst

class ReadlnTS : TransitionSystem {
    companion object: StaticInfo {
        val msgArg = Variable("msg", stringType)
        val promptAct = SymbolicAction("prompt", listOf())
        val readlnAct = SymbolicAction("readln", listOf(msgArg))
        val initiallyCtor = Pair(SymbolicAction("initially", listOf())) { _ : Program, _ : ConcreteAction -> ReadlnTS() }
        // the $ in the name means that programs cannot create p-classes whose names conflict with this one
        override fun staticInfo() = TransitionSystemStaticInfo(
            "ReadlnTS$",
            setOf(promptAct, readlnAct),
            mapOf(initiallyCtor),
            setOf(readlnAct),
            false)
    }

    private val ctx = Context()
    private var prompt = true
    private var msg = ""

    override fun actions() = setOf(
        TSAction(
            promptAct,
            ctx.mkEq(ctx.mkBool(prompt), ctx.mkBool(true)),
            false
        ),
        TSAction(
            readlnAct,
            ctx.mkAnd(
                ctx.mkEq(ctx.mkBool(prompt), ctx.mkBool(false)),
                ctx.mkEq(ctx.mkStringConst("msg"), ctx.mkString(msg))
            ),
            true
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
