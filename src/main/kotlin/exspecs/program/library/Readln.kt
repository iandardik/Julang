package exspecs.program.library

import com.microsoft.z3.Context
import exspecs.program.*
import exspecs.tools.mkStringConst

class ReadlnTS : TransitionSystem {
    private val ctx = Context()
    private var prompt = true
    private var msg = ""

    override fun actions() = setOf(
        SymbolicAction(
            ActionSignature("prompt", listOf()),
            ctx.mkEq(ctx.mkBoolConst("prompt"), ctx.mkBool(true))
        ),
        SymbolicAction(
            ActionSignature("readln", listOf(Variable("msg",stringType))),
            ctx.mkAnd(
                ctx.mkEq(ctx.mkBoolConst("prompt"), ctx.mkBool(false)),
                ctx.mkEq(ctx.mkStringConst("msg"), ctx.mkString(msg))
            )
        ),
    )
    override fun currentStateToZ3Expr() = ctx.mkAnd(
        ctx.mkEq(ctx.mkBoolConst("prompt"), ctx.mkBool(prompt))
    )
    override fun transit(act: ConcreteAction) {
        if (act.signature.name == "prompt") {
            prompt = false
            msg = readln()
        }
        else {
            prompt = true
        }
    }
    override fun getContext() = ctx
}

