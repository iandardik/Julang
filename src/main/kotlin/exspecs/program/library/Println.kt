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
    override fun transit(act: ConcreteAction) {
        val msg = act.lookup(Variable("msg", stringType))
        println(msg)
    }
    override fun getContext() = ctx
}

val printlnTSStaticInfo = TransitionSystemStaticInfo(
    setOf(ActionSignature("println", listOf(Variable("msg", stringType))),),
    setOf(ActionSignature("initially", listOf()),),
    false) { PrintlnTS() }

// TODO this is ugly af
val printlnTSStaticInfoStr = "TransitionSystemStaticInfo(" +
    "\nsetOf(ActionSignature(\"println\", listOf(Variable(\"msg\", stringType))),),".prependIndent() +
    "\nsetOf(ActionSignature(\"initially\", listOf()),),".prependIndent() +
    "false) { PrintlnTS() }"



// TODO delete in the future, this is an example for reference
/*
val tsInfo = setOf(
    TransitionSystemStaticInfo(
        setOf(
            ActionSignature("increment", listOf(Variable("inc", intType))),
            ActionSignature("println", listOf(Variable("msg", stringType))),
        ),
        setOf(
            ActionSignature("initially", listOf()),
        ),
        true) { TestTS(true, 0) },
    TransitionSystemStaticInfo(
        setOf(
            ActionSignature("println", listOf(Variable("msg", stringType))),
        ),
        setOf(
            ActionSignature("initially", listOf()),
        ),
        false) { PrintlnTS() },
)
Program(tsInfo).run()

class TestTS(
    private var print : Boolean,
    private var counter : Int
) : TransitionSystem {
    private val ctx = Context()
    override fun actions(): Set<SymbolicAction> {
        return setOf(
            SymbolicAction(
                ActionSignature("increment", listOf(Variable("inc",intType))),
                ctx.mkAnd(
                    ctx.mkGt(ctx.mkIntConst("inc"), ctx.mkInt(3)),
                    ctx.mkLe(ctx.mkIntConst("counter"), ctx.mkInt(10)),
                    ctx.mkEq(ctx.mkBoolConst("print"), ctx.mkFalse()),
                ),
            ),
            SymbolicAction(
                ActionSignature("println", listOf(Variable("msg",stringType))),
                ctx.mkAnd(
                    ctx.mkEq(ctx.mkStringConst("msg"), ctx.mkString("$counter")),
                    ctx.mkEq(ctx.mkBoolConst("print"), ctx.mkTrue()),
                ),
            ),
        )
    }
    override fun currentStateToZ3Expr() : BoolExpr {
        return ctx.mkAnd(
            ctx.mkEq(ctx.mkBoolConst("print"), ctx.mkBool(print)),
            ctx.mkEq(ctx.mkIntConst("counter"), ctx.mkInt(counter)),
        )
    }
    override fun transit(act: ConcreteAction) {
        if (act.signature.name == "increment") {
            val inc = act.lookup(Variable("inc", intType)).value as Int
            counter += inc
            print = true
        }
        else {
            print = false
        }
    }
    override fun getContext() = ctx
}
*/
