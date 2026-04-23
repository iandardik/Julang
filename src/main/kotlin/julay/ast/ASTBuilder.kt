package julay.ast

import julay.parser.JulayParser
import julay.parser.JulayParserBaseVisitor
import julay.program.SymbolicAction
import julay.program.TSAction
import julay.program.boolType
import julay.program.intType
import julay.program.parseType
import julay.program.stringType
import julay.tools.assert
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
        val localDecls = ctx.pclass_body()
            .map { visit(it) }
            .map {
                if (it !is ProcClassDeclNode) {
                    throw RuntimeException("Expected ProcClassDeclNode but got $it")
                }
                it
            }
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
        return visit(body)
    }

    override fun visitVar(ctx: JulayParser.VarContext?): ASTNode {
        val name = ctx!!.ID(0).text
        val type = ctx!!.ID(1).text
        return VarNode(name, parseType(type))
    }

    override fun visitConstructor(ctx: JulayParser.ConstructorContext?): ASTNode {
        val name = ctx!!.ID().text
        val args = visit(ctx.args()).let { argsNode ->
            if (argsNode !is ArgsNode) {
                throw RuntimeException("Expected ArgsNode but got $argsNode")
            }
            argsNode
        }
        val body = ctx.action_body()
            .map { visit(it) }
            .map {
                if (it !is ActionBodyNode) {
                    throw RuntimeException("Expected ActionBody but got $it")
                }
                it
            }
        val loc = Pair(ctx.getStart().line, ctx.getStart().line)
        return ConstructorNode(name, args, body, SourceLoc(loc))
    }

    override fun visitTransition(ctx: JulayParser.TransitionContext?): ASTNode {
        val isService = ctx!!.SERVICE() != null
        val isConsumer = ctx.CONSUMER() != null
        assert(!isService || !isConsumer, "A transition cannot be both a service and consumer")
        val modifier = when {
            isService -> TSAction.SyncRole.P2PService
            isConsumer -> TSAction.SyncRole.P2PConsumer
            else -> TSAction.SyncRole.CSP
        }
        val name = ctx.ID().text
        val args = visit(ctx.args()).let { argsNode ->
            if (argsNode !is ArgsNode) {
                throw RuntimeException("Expected ArgsNode but got $argsNode")
            }
            argsNode
        }
        val body = ctx.action_body()
            .map { visit(it) }
            .map {
                if (it !is ActionBodyNode) {
                    throw RuntimeException("Expected ActionBody but got $it")
                }
                it
            }
        val loc = Pair(ctx.getStart().line, ctx.getStart().line)
        return TransitionNode(modifier, name, args, body, SourceLoc(loc))
    }

    override fun visitArgs(ctx: JulayParser.ArgsContext?): ASTNode {
        val args = ctx!!.arg()
            .map { visit(it) }
            .map { argsNode ->
                if (argsNode !is ArgsNode) {
                    throw RuntimeException("Expected ArgsNode but got $argsNode")
                }
                argsNode
            }
        return ArgsNode(args)
    }

    override fun visitArg(ctx: JulayParser.ArgContext?): ASTNode {
        val name = ctx!!.ID(0).text
        val type = ctx!!.ID(1).text
        return ArgNode(name, parseType(type))
    }

    override fun visitAction_body(ctx: JulayParser.Action_bodyContext?): ASTNode {
        val body = oneChoice(ctx!!.guard(), ctx.transit(), ctx.error())
        return visit(body)
    }

    override fun visitGuard(ctx: JulayParser.GuardContext?): ASTNode {
        val guardExpr = visit(ctx!!.expr())
        if (guardExpr !is ExprNode) {
            throw RuntimeException("Expected every guard to be an expr")
        }
        return GuardNode(guardExpr)
    }

    override fun visitTransit(ctx: JulayParser.TransitContext?): ASTNode {
        val transits = ctx!!.var_transit()
            .map { visit(it) }
            .map {
                if (it !is ActionBodyNode) {
                    throw RuntimeException("Expected ActionBody but got $it")
                }
                it
            }
        return TransitNode(transits)
    }

    override fun visitError(ctx: JulayParser.ErrorContext?): ASTNode {
        val errExpr = visit(ctx!!.expr())
            .let {
                if (it !is ExprNode) {
                    throw RuntimeException("Expected ExprNode but got $it")
                }
                it
            }
        return ErrorNode(errExpr)
    }

    override fun visitVar_transit(ctx: JulayParser.Var_transitContext?): ASTNode {
        val varName = ctx!!.ID().text
        val transit = visit(ctx.expr())
        if (transit !is ExprNode) {
            throw RuntimeException("Expected transit to be assigned an expr")
        }
        return VarTransitNode(varName, transit)
    }

    override fun visitExpr(ctx: JulayParser.ExprContext?): ASTNode {
        ctx!!
        val unaryOpMapper = {
            when {
                ctx.NOT() != null -> "~"
                ctx.BANG() != null -> "~"
                else -> "N/A"
            }
        }
        val binaryOpMapper = {
            when {
                ctx.EQ() != null -> "="
                ctx.NEQ() != null -> "#"
                ctx.BANG_NEQ() != null -> "#"
                ctx.LT() != null -> "<"
                ctx.LTE() != null -> "<="
                ctx.GT() != null -> ">"
                ctx.GTE() != null -> ">="
                ctx.AND() != null -> "&"
                ctx.OR() != null -> "|"
                ctx.PLUS() != null -> "+"
                ctx.MINUS() != null -> "-"
                else -> "N/A"
            }
        }
        return when {
            ctx.value() != null -> {
                val valueNode = visit(ctx.value())
                assert(valueNode is ExprNode, "Expected expr children to be ExprNodes")
                valueNode
            }
            unaryOpMapper() != "N/A" -> {
                val innerNode = visit(ctx.expr(0))
                if (innerNode !is ExprNode) {
                    throw RuntimeException("Expected expr children to be ExprNodes")
                }
                UnaryOpExprNode(unaryOpMapper(), innerNode)
            }
            binaryOpMapper() != "N/A" -> {
                val lhsNode = visit(ctx.expr(0))
                val rhsNode = visit(ctx.expr(1))
                if (lhsNode !is ExprNode || rhsNode !is ExprNode) {
                    throw RuntimeException("Expected expr children to be ExprNodes")
                }
                BinaryOpExprNode(binaryOpMapper(), lhsNode, rhsNode)
            }
            ctx.LPAREN() != null -> visit(ctx.expr(0))
            else -> throw RuntimeException("Invalid expr node: ${ctx.text}")
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