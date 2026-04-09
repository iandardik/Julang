package exspecs.program.library

import com.microsoft.z3.Context
import exspecs.program.*

class PrintlnTS : TransitionSystem {
    private val ctx = Context()
    private val z3True = ctx.mkTrue()
    private val alphabet = setOf(
        SymbolicAction(
            ActionSignature("println", listOf(Variable("msg",stringType))),
            ctx.mkTrue(),
        ),
    )

    override fun actions() = alphabet
    override fun currentStateToZ3Expr() = z3True
    override fun transit(act: ConcreteAction) {
        val msg = act.lookup(Variable("msg", stringType))
        println(msg)
    }
    override fun getContext() = ctx
}

