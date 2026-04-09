package exspecs.cli

import com.microsoft.z3.BoolExpr
import com.microsoft.z3.Context
import exspecs.ast.ASTBuilder
import exspecs.ast.RootNode
import exspecs.parser.JulayLexer
import exspecs.parser.JulayParser
import exspecs.program.*
import exspecs.program.library.PrintlnTS
import exspecs.tools.mkStringConst
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import java.util.*

class TestTS : TransitionSystem {
    private val ctx = Context()
    private var print = true
    private var counter = 0

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

fun main(args : Array<String>) {
    val tsInfo = setOf(
        TransitionSystemStaticInfo(
            setOf(
                ActionSignature("increment", listOf(Variable("inc", intType))),
                ActionSignature("println", listOf(Variable("msg", stringType))),
            ),
            setOf(
                ActionSignature("initially", listOf()),
            ),
            true) { TestTS() },
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

    /*
    if (args.size != 1) {
        println("usage: Exspec <.jul file>")
        return
    }
    val input = CharStreams.fromFileName(args[0])
    val lexer = JulayLexer(input)
    val tokens = CommonTokenStream(lexer)
    val parser = JulayParser(tokens)
    val root = parser.root()
    if (parser.numberOfSyntaxErrors > 0) {
        println("Found compile errors, exiting.")
        return
    }
    val ast = ASTBuilder().visit(root) as RootNode
    val errors = ast.errorPass()
    if (errors.isNotEmpty()) {
        errors.forEach { println(it) }
        println("Found compile errors, exiting.")
        return
    }
    println(ast.toKotlin())
    //val programAST = ast as RootNode
    //val typedAST = programAST.toTypedAST()
    //val prog = typedAST.toProgram()
    //prog.run()
     */
}
