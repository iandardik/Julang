package julay.ast

import julay.parser.JulayParser
import julay.parser.JulayParserBaseVisitor
import julay.program.boolType
import julay.program.intType
import julay.program.parseType
import julay.program.stringType
import org.antlr.v4.runtime.ParserRuleContext

fun oneChoice(vararg choices : ParserRuleContext?) : ParserRuleContext {
    val nonNullChoices = choices.filterNotNull()
    julay.tools.assert(nonNullChoices.size == 1, "Expected one choice but got: ${nonNullChoices.size}")
    return nonNullChoices[0]
}

class ASTBuilder : JulayParserBaseVisitor<ASTNode>() {

    override fun visitRoot(ctx: JulayParser.RootContext?): ASTNode {
        val declNodes = ctx!!.decl().map { visit(it) }
        return RootNode(declNodes)
    }

    override fun visitDecl(ctx: JulayParser.DeclContext?): ASTNode {
        val decl = oneChoice(ctx!!.pclass(), ctx.proc(), ctx.program(), ctx.spec())
        return DeclNode(visit(decl))
    }

    override fun visitPclass(ctx: JulayParser.PclassContext?): ASTNode {
        val name = ctx!!.ID().text
        val localDecls = ctx.pclass_body().map { visit(it) }
        return ProcClassNode(name, localDecls)
    }

    override fun visitProc(ctx: JulayParser.ProcContext?): ASTNode {
        val name = ctx!!.ID().text
        val value = visit(ctx.proc_expr())
        return ProcNode(name, value)
    }

    override fun visitProgram(ctx: JulayParser.ProgramContext?): ASTNode {
        val name = ctx!!.ID().text
        val value = visit(ctx.proc_expr())
        return ProgramNode(name, value)
    }

    override fun visitSpec(ctx: JulayParser.SpecContext?): ASTNode {
        val name = ctx!!.ID().text
        val value = visit(ctx.proc_expr())
        return SpecNode(name, value)
    }

    override fun visitPclass_body(ctx: JulayParser.Pclass_bodyContext?): ASTNode {
        val body = oneChoice(ctx!!.`var`(), ctx.constructor(), ctx.transition())
        return ProcClassBodyNode(visit(body))
    }

    override fun visitVar(ctx: JulayParser.VarContext?): ASTNode {
        val name = ctx!!.ID(0).text
        val type = ctx!!.ID(1).text
        return VarNode(name, parseType(type))
    }

    override fun visitConstructor(ctx: JulayParser.ConstructorContext?): ASTNode {
        val name = ctx!!.ID().text
        val args = visit(ctx.args())
        val body = ctx.action_body().map { visit(it) }
        return ConstructorNode(name, args, body)
    }

    override fun visitTransition(ctx: JulayParser.TransitionContext?): ASTNode {
        val name = ctx!!.ID().text
        val args = visit(ctx.args())
        val body = ctx.action_body().map { visit(it) }
        val loc = Pair(ctx.getStart().line, ctx.getStart().line)
        return TransitionNode(name, args, body, loc)
    }

    override fun visitService(ctx: JulayParser.ServiceContext?): ASTNode {
        val name = ctx!!.ID().text
        val args = visit(ctx.args())
        val body = ctx.action_body().map { visit(it) }
        val loc = Pair(ctx.getStart().line, ctx.getStart().line)
        return ServiceNode(name, args, body, loc)
    }

    override fun visitArgs(ctx: JulayParser.ArgsContext?): ASTNode {
        val args = ctx!!.arg().map { visit(it) }
        return ArgsNode(args)
    }

    override fun visitArg(ctx: JulayParser.ArgContext?): ASTNode {
        val name = ctx!!.ID(0).text
        val type = ctx!!.ID(1).text
        return ArgNode(name, parseType(type))
    }

    override fun visitAction_body(ctx: JulayParser.Action_bodyContext?): ASTNode {
        val body = oneChoice(ctx!!.guard(), ctx.transit(), ctx.error())
        return ActionBodyNode(visit(body))
    }

    override fun visitGuard(ctx: JulayParser.GuardContext?): ASTNode {
        val guardExpr = visit(ctx!!.expr())
        return GuardNode(guardExpr)
    }

    override fun visitTransit(ctx: JulayParser.TransitContext?): ASTNode {
        val transits = ctx!!.var_transit().map { visit(it) }
        return TransitNode(transits)
    }

    override fun visitError(ctx: JulayParser.ErrorContext?): ASTNode {
        val errExpr = visit(ctx!!.expr())
        return ErrorNode(errExpr)
    }

    override fun visitVar_transit(ctx: JulayParser.Var_transitContext?): ASTNode {
        val varName = ctx!!.ID().text
        val transit = visit(ctx.expr())
        return VarTransitNode(varName, transit)
    }

    override fun visitExpr(ctx: JulayParser.ExprContext?): ASTNode {
       return if (ctx!!.EQ() != null) {
           BinaryOpExprNode("=", visit(ctx.expr(0)), visit(ctx.expr(1)))
       } else if (ctx.NEQ() != null || ctx.BANG_NEQ() != null) {
           BinaryOpExprNode("#", visit(ctx.expr(0)), visit(ctx.expr(1)))
       } else if (ctx.LT() != null) {
           BinaryOpExprNode("<", visit(ctx.expr(0)), visit(ctx.expr(1)))
       } else if (ctx.LTE() != null) {
           BinaryOpExprNode("<=", visit(ctx.expr(0)), visit(ctx.expr(1)))
       } else if (ctx.GT() != null) {
           BinaryOpExprNode(">", visit(ctx.expr(0)), visit(ctx.expr(1)))
       } else if (ctx.GTE() != null) {
           BinaryOpExprNode(">=", visit(ctx.expr(0)), visit(ctx.expr(1)))
       } else if (ctx.AND() != null) {
           BinaryOpExprNode("&", visit(ctx.expr(0)), visit(ctx.expr(1)))
       } else if (ctx.OR() != null) {
           BinaryOpExprNode("|", visit(ctx.expr(0)), visit(ctx.expr(1)))
       } else if (ctx.NOT() != null || ctx.BANG() != null) {
           UnaryOpExprNode("~", visit(ctx.expr(0)))
       } else if (ctx.PLUS() != null) {
           BinaryOpExprNode("+", visit(ctx.expr(0)), visit(ctx.expr(1)))
       } else if (ctx.MINUS() != null) {
           BinaryOpExprNode("-", visit(ctx.expr(0)), visit(ctx.expr(1)))
       } else if (ctx.LPAREN() != null) {
           visit(ctx.expr(0))
       } else if (ctx.value() != null) {
           visit(ctx.value())
       } else {
           throw RuntimeException("Invalid visitExpr: invalid expression found: ${ctx.text}")
       }
    }

    override fun visitProc_expr(ctx: JulayParser.Proc_exprContext?): ASTNode {
        return if (ctx!!.ID() != null) {
            ValueProcExprNode(ctx.ID().text)
        } else {
            val compositeProcs = ctx.proc_expr().map { visit(it) }
            CompositeProcExprNode(compositeProcs)
        }
    }

    override fun visitValue(ctx: JulayParser.ValueContext?): ASTNode {
        return if (ctx!!.ID() != null) {
            SymbolValueExprNode(ctx.ID().text)
        } else if (ctx.INT() != null) {
            LiteralValueExprNode(ctx.INT().text, intType)
        } else if (ctx.TRUE() != null) {
            LiteralValueExprNode(ctx.TRUE().text, boolType)
        } else if (ctx.FALSE() != null) {
            LiteralValueExprNode(ctx.FALSE().text, boolType)
        } else if (ctx.STRING() != null) {
            val rawStr = ctx.STRING().text
            val unquotedStr = rawStr.substring(1,rawStr.length-1)
            LiteralValueExprNode(unquotedStr, stringType)
        } else {
            throw RuntimeException("Invalid visitValue: invalid expression found: ${ctx.text}")
        }
    }
}