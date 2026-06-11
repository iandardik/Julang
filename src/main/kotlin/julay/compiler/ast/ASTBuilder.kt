package julay.compiler.ast

import julay.compiler.SourceLoc
import julay.parser.JulayParser
import julay.parser.JulayParserBaseVisitor
import julay.program.TSAction
import julay.program.boolType
import julay.program.intType
import julay.program.stringType
import julay.tools.assert
import org.antlr.v4.runtime.ParserRuleContext

fun oneChoice(vararg choices : ParserRuleContext?) : ParserRuleContext {
    val nonNullChoices = choices.filterNotNull()
    assert(nonNullChoices.size == 1, "Expected one choice but got: ${nonNullChoices.size}")
    return nonNullChoices[0]
}

fun sourceLocation(ctx : ParserRuleContext) = SourceLoc(Pair(ctx.getStart().line, ctx.getStop().line))

class ASTBuilder : JulayParserBaseVisitor<ASTNode>() {

    override fun visitRoot(ctx: JulayParser.RootContext?): ASTNode {
        val declNodes = ctx!!.decl().map {
            val node = visit(it)
            if (node !is DeclNode) {
                throw RuntimeException("Expected DeclNode but got $node")
            }
            node
        }
        return RootNode(declNodes, sourceLocation(ctx))
    }

    override fun visitDecl(ctx: JulayParser.DeclContext?): ASTNode {
        val decl = oneChoice(ctx!!.pclass(), ctx.oclass(), ctx.proc(), ctx.program(), ctx.spec())
        return visit(decl)
    }

    override fun visitOclass(ctx: JulayParser.OclassContext?): ASTNode {
        val name = ctx!!.ID().text
        val fields = ctx.field()
            .map { visit(it) }
            .map {
                if (it !is FieldNode) {
                    throw RuntimeException("Expected FieldNode but got $it")
                }
                it
            }
        return ObjClassNode(name, fields, sourceLocation(ctx))
    }

    override fun visitField(ctx: JulayParser.FieldContext?): ASTNode {
        val fieldName = ctx!!.ID(0).text
        val typeName = ctx.ID(1).text
        return FieldNode(fieldName, typeName, sourceLocation(ctx))
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
        return ProcClassNode(name, localDecls, sourceLocation(ctx))
    }

    override fun visitProc(ctx: JulayParser.ProcContext?): ASTNode {
        val name = ctx!!.ID().text
        val value = visit(ctx.proc_expr())
        return ProcNode(name, value, sourceLocation(ctx))
    }

    override fun visitProgram(ctx: JulayParser.ProgramContext?): ASTNode {
        val name = ctx!!.ID().text
        val value = visit(ctx.proc_expr())
        return ProgramNode(name, value, sourceLocation(ctx))
    }

    override fun visitSpec(ctx: JulayParser.SpecContext?): ASTNode {
        val name = ctx!!.ID().text
        val value = visit(ctx.proc_expr())
        return SpecNode(name, value, sourceLocation(ctx))
    }

    override fun visitPclass_body(ctx: JulayParser.Pclass_bodyContext?): ASTNode {
        val body = oneChoice(ctx!!.`var`(), ctx.constructor(), ctx.transition())
        return visit(body)
    }

    override fun visitVar(ctx: JulayParser.VarContext?): ASTNode {
        val name = ctx!!.ID(0).text
        val typeName = ctx.ID(1).text
        return VarNode(name, typeName, sourceLocation(ctx))
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
        return ConstructorNode(name, args, body, sourceLocation(ctx))
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
        return TransitionNode(modifier, name, args, body, sourceLocation(ctx))
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
        return ArgsNode(args, sourceLocation(ctx))
    }

    override fun visitArg(ctx: JulayParser.ArgContext?): ASTNode {
        val name = ctx!!.ID(0).text
        val typeName = ctx.ID(1).text
        return ArgNode(name, typeName, sourceLocation(ctx))
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
        return GuardNode(guardExpr, sourceLocation(ctx))
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
        return TransitNode(transits, sourceLocation(ctx))
    }

    override fun visitError(ctx: JulayParser.ErrorContext?): ASTNode {
        val errExpr = visit(ctx!!.expr())
            .let {
                if (it !is ExprNode) {
                    throw RuntimeException("Expected ExprNode but got $it")
                }
                it
            }
        return ErrorNode(errExpr, sourceLocation(ctx))
    }

    override fun visitVar_transit(ctx: JulayParser.Var_transitContext?): ASTNode {
        val lhs = visit(ctx!!.field_access())
        val transit = visit(ctx.expr())
        if (transit !is ExprNode) {
            throw RuntimeException("Expected transit to be assigned an expr")
        }
        return when (lhs) {
            is FieldAccessExprNode -> VarTransitNode(lhs.baseSymbol, lhs.fieldPath, transit, sourceLocation(ctx))
            is SymbolValueExprNode -> VarTransitNode(lhs.symbol, emptyList(), transit, sourceLocation(ctx))
            else -> throw RuntimeException("Expected field access on left-hand side of transit assignment")
        }
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
                ctx.TIMES() != null -> "*"
                ctx.DIV() != null -> "/"
                ctx.MOD() != null -> "%"
                ctx.LT() != null -> "<"
                ctx.LTE() != null -> "<="
                ctx.GT() != null -> ">"
                ctx.GTE() != null -> ">="
                ctx.AND() != null -> "&"
                ctx.OR() != null -> "|"
                ctx.IMPLIES() != null -> "=>"
                ctx.PLUS() != null -> "+"
                ctx.MINUS() != null -> "-"
                else -> "N/A"
            }
        }
        val ternaryOpMapper = {
            when {
                ctx.IF() != null -> "if-else"
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
                UnaryOpExprNode(unaryOpMapper(), innerNode, sourceLocation(ctx))
            }
            binaryOpMapper() != "N/A" -> {
                val lhsNode = visit(ctx.expr(0))
                val rhsNode = visit(ctx.expr(1))
                if (lhsNode !is ExprNode || rhsNode !is ExprNode) {
                    throw RuntimeException("Expected expr children to be ExprNodes")
                }
                BinaryOpExprNode(binaryOpMapper(), lhsNode, rhsNode, sourceLocation(ctx))
            }
            ternaryOpMapper() != "N/A" -> {
                val condNode = visit(ctx.expr(0))
                val thenNode = visit(ctx.expr(1))
                val elseNode = visit(ctx.expr(2))
                if (condNode !is ExprNode || thenNode !is ExprNode || elseNode !is ExprNode) {
                    throw RuntimeException("Expected expr children to be ExprNodes")
                }
                IfElseExprNode(condNode, thenNode, elseNode, sourceLocation(ctx))
            }
            ctx.LPAREN() != null -> visit(ctx.expr(0))
            else -> throw RuntimeException("Invalid expr node: ${ctx.text}")
        }
    }

    override fun visitProc_expr(ctx: JulayParser.Proc_exprContext?): ASTNode {
        return if (ctx!!.ID() != null) {
            ValueProcExprNode(ctx.ID().text, sourceLocation(ctx))
        } else {
            val compositeProcs = ctx.proc_expr().map { visit(it) }
            CompositeProcExprNode(compositeProcs, sourceLocation(ctx))
        }
    }

    override fun visitStruct_literal(ctx: JulayParser.Struct_literalContext?): ASTNode {
        val className = ctx!!.ID().text
        val fieldEntries = ctx.struct_field_assign().map { assign ->
            val fieldName = assign.ID().text
            val expr = visit(assign.expr())
            if (expr !is ExprNode) {
                throw RuntimeException("Expected o-class literal field value to be an expression")
            }
            fieldName to expr
        }
        return ObjClassLiteralExprNode(className, fieldEntries, sourceLocation(ctx))
    }

    override fun visitField_access(ctx: JulayParser.Field_accessContext?): ASTNode {
        val ids = ctx!!.ID().map { it.text }
        if (ids.size == 1) {
            return SymbolValueExprNode(ids[0], sourceLocation(ctx))
        }
        return FieldAccessExprNode(ids[0], ids.drop(1), sourceLocation(ctx))
    }

    override fun visitValue(ctx: JulayParser.ValueContext?): ASTNode {
        return if (ctx!!.struct_literal() != null) {
            visit(ctx.struct_literal())
        } else if (ctx.field_access() != null) {
            visit(ctx.field_access())
        } else if (ctx.INT() != null) {
            LiteralValueExprNode(ctx.INT().text, intType, sourceLocation(ctx))
        } else if (ctx.TRUE() != null) {
            LiteralValueExprNode(ctx.TRUE().text, boolType, sourceLocation(ctx))
        } else if (ctx.FALSE() != null) {
            LiteralValueExprNode(ctx.FALSE().text, boolType, sourceLocation(ctx))
        } else if (ctx.STRING() != null) {
            val rawStr = ctx.STRING().text
            val unquotedStr = rawStr.substring(1,rawStr.length-1)
            LiteralValueExprNode(unquotedStr, stringType, sourceLocation(ctx))
        } else {
            throw RuntimeException("Invalid visitValue: invalid expression found: ${ctx.text}")
        }
    }
}