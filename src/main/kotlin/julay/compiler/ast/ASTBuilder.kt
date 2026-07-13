package julay.compiler.ast

import julay.compiler.SourceLoc
import julay.compiler.TypeExpr
import julay.parser.JulayParser
import julay.parser.JulayParserBaseVisitor
import julay.program.TSAction
import julay.program.boolType
import julay.program.intType
import julay.program.realType
import julay.program.stringType
import julay.tools.assert
import org.antlr.v4.runtime.ParserRuleContext
import java.nio.file.Path

fun oneChoice(vararg choices : ParserRuleContext?) : ParserRuleContext {
    val nonNullChoices = choices.filterNotNull()
    assert(nonNullChoices.size == 1, "Expected one choice but got: ${nonNullChoices.size}")
    return nonNullChoices[0]
}

private fun parseTypeExpr(ctx: JulayParser.TypeExprContext): TypeExpr {
    return when {
        ctx.LPAREN() != null -> parseTypeExpr(ctx.typeExpr())
        ctx.typeArgs() != null -> {
            val args = ctx.typeArgs().typeExpr().map { parseTypeExpr(it) }
            TypeExpr.Parametric(ctx.ID().text, args)
        }
        else -> TypeExpr.Simple(ctx.ID().text)
    }
}

private fun parseTypeParams(ctx: JulayParser.TypeParamsContext?): List<String> =
    ctx?.ID()?.map { it.text } ?: emptyList()

class ASTBuilder(private val sourcePath: Path) : JulayParserBaseVisitor<ASTNode>() {

    private fun sourceLocation(ctx: ParserRuleContext) =
        SourceLoc(Pair(ctx.getStart().line, ctx.getStop().line), sourcePath)

    override fun visitRoot(ctx: JulayParser.RootContext?): ASTNode {
        val importNodes = ctx!!.import_stmt().map {
            val node = visit(it)
            if (node !is ImportNode) {
                throw RuntimeException("Expected ImportNode but got $node")
            }
            node
        }
        val declNodes = ctx.decl().map {
            val node = visit(it)
            if (node !is DeclNode) {
                throw RuntimeException("Expected DeclNode but got $node")
            }
            node
        }
        return RootNode(importNodes, declNodes, sourceLocation(ctx))
    }

    override fun visitImport_stmt(ctx: JulayParser.Import_stmtContext?): ASTNode {
        val qualifiedName = visit(ctx!!.qualified_name())
        if (qualifiedName !is QualifiedNameNode) {
            throw RuntimeException("Expected QualifiedNameNode but got $qualifiedName")
        }
        return ImportNode(qualifiedName, sourceLocation(ctx))
    }

    override fun visitQualified_name(ctx: JulayParser.Qualified_nameContext?): ASTNode {
        val parts = mutableListOf(ctx!!.ID().text)
        ctx.qual_segment().forEach { seg ->
            parts.add(seg.ID()?.text ?: seg.FUN().text)
        }
        return QualifiedNameNode(parts, sourceLocation(ctx))
    }

    override fun visitDecl(ctx: JulayParser.DeclContext?): ASTNode {
        val decl = oneChoice(
            ctx!!.pclass(),
            ctx.oclass(),
            ctx.proc(),
            ctx.program(),
            ctx.spec(),
            ctx.fun_decl(),
        )
        return visit(decl)
    }

    override fun visitFun_decl(ctx: JulayParser.Fun_declContext?): ASTNode {
        val name = ctx!!.ID().text
        val typeParams = parseTypeParams(ctx.typeParams())
        val args = visit(ctx.args()).let { argsNode ->
            if (argsNode !is ArgsNode) {
                throw RuntimeException("Expected ArgsNode but got $argsNode")
            }
            argsNode
        }
        val returnType = parseTypeExpr(ctx.typeExpr())
        val body = visit(ctx.expr())
        if (body !is ExprNode) {
            throw RuntimeException("Expected function body to be an expression")
        }
        return FunNode(name, typeParams, args, returnType, body, sourceLocation(ctx))
    }

    override fun visitFun_call(ctx: JulayParser.Fun_callContext?): ASTNode {
        val name = ctx!!.ID().text
        val args = ctx.expr().map { visit(it) }.map {
            if (it !is ExprNode) {
                throw RuntimeException("Expected function call arguments to be expressions")
            }
            it
        }
        return FunCallExprNode(name, args, sourceLocation(ctx))
    }

    override fun visitOclass(ctx: JulayParser.OclassContext?): ASTNode {
        val name = ctx!!.ID().text
        val typeParams = parseTypeParams(ctx.typeParams())
        val fields = ctx.field()
            .map { visit(it) }
            .map {
                if (it !is FieldNode) {
                    throw RuntimeException("Expected FieldNode but got $it")
                }
                it
            }
        return ObjClassNode(name, typeParams, fields, sourceLocation(ctx))
    }

    override fun visitField(ctx: JulayParser.FieldContext?): ASTNode {
        val fieldName = ctx!!.ID().text
        val typeExpr = parseTypeExpr(ctx.typeExpr())
        return FieldNode(fieldName, typeExpr, sourceLocation(ctx))
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
        val name = ctx!!.ID().text
        val typeExpr = parseTypeExpr(ctx.typeExpr())
        return VarNode(name, typeExpr, sourceLocation(ctx))
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
        val name = ctx!!.ID().text
        val typeExpr = parseTypeExpr(ctx.typeExpr())
        return ArgNode(name, typeExpr, sourceLocation(ctx))
    }

    override fun visitAction_body(ctx: JulayParser.Action_bodyContext?): ASTNode {
        val body = oneChoice(ctx!!.guard(), ctx.transit(), ctx.error(), ctx.effect())
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
        val arms = ctx!!.error_arm()
            .map { visit(it) }
            .map {
                if (it !is ErrorArmNode) {
                    throw RuntimeException("Expected ErrorArmNode but got $it")
                }
                it
            }
        return ErrorNode(arms, sourceLocation(ctx))
    }

    override fun visitError_arm(ctx: JulayParser.Error_armContext?): ASTNode {
        val exprs = ctx!!.expr()
        val cond = visit(exprs[0]).let {
            if (it !is ExprNode) {
                throw RuntimeException("Expected ExprNode but got $it")
            }
            it
        }
        val msg = visit(exprs[1]).let {
            if (it !is ExprNode) {
                throw RuntimeException("Expected ExprNode but got $it")
            }
            it
        }
        return ErrorArmNode(cond, msg, sourceLocation(ctx))
    }

    override fun visitEffect(ctx: JulayParser.EffectContext?): ASTNode {
        val stmts = ctx!!.effect_stmt()
            .map { visit(it) }
            .map {
                if (it !is EffectStmtNode) {
                    throw RuntimeException("Expected EffectStmtNode but got $it")
                }
                it
            }
        return EffectNode(stmts, sourceLocation(ctx))
    }

    override fun visitEffect_stmt(ctx: JulayParser.Effect_stmtContext?): ASTNode {
        if (ctx!!.effect_call() != null) {
            val call = visit(ctx.effect_call())
            if (call !is EffectCallNode) {
                throw RuntimeException("Expected EffectCallNode but got $call")
            }
            if (ctx.field_access() != null) {
                val lhs = visit(ctx.field_access())
                return when (lhs) {
                    is FieldAccessExprNode -> EffectAssignNode(
                        lhs.baseSymbol,
                        lhs.fieldPath,
                        call.callName(),
                        call.callArgs(),
                        sourceLocation(ctx),
                    )
                    is SymbolValueExprNode -> EffectAssignNode(
                        lhs.symbol,
                        emptyList(),
                        call.callName(),
                        call.callArgs(),
                        sourceLocation(ctx),
                    )
                    else -> throw RuntimeException("Expected field access on left-hand side of effect assignment")
                }
            }
            return call
        }
        throw RuntimeException("Invalid effect statement: ${ctx.text}")
    }

    override fun visitEffect_call(ctx: JulayParser.Effect_callContext?): ASTNode {
        val name = ctx!!.ID().text
        val args = ctx.expr().map { visit(it) }.map {
            if (it !is ExprNode) {
                throw RuntimeException("Expected effect call arguments to be expressions")
            }
            it
        }
        return EffectCallNode(name, args, sourceLocation(ctx))
    }

    override fun visitVar_transit(ctx: JulayParser.Var_transitContext?): ASTNode {
        if (ctx!!.ID() != null && ctx.LBRACK() != null) {
            val mapVar = ctx.ID().text
            val key = visit(ctx.expr(0))
            val value = visit(ctx.expr(1))
            if (key !is ExprNode || value !is ExprNode) {
                throw RuntimeException("Expected expressions in map transit assignment")
            }
            return MapIndexTransitNode(mapVar, key, value, sourceLocation(ctx))
        }
        val lhs = visit(ctx.field_access())
        val transit = visit(ctx.expr(0))
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
                // Binary & / | require two operands; prefix forms are handled below
                ctx.AND() != null && ctx.expr().size == 2 -> "&"
                ctx.OR() != null && ctx.expr().size == 2 -> "|"
                ctx.IMPLIES() != null -> "=>"
                ctx.IN() != null -> "in"
                ctx.PLUS() != null -> "+"
                ctx.MINUS() != null -> "-"
                else -> "N/A"
            }
        }
        val ternaryOpMapper = {
            when {
                ctx.IF() != null -> "if-else"
                ctx.LET() != null -> "let"
                ctx.WHEN() != null -> "when"
                else -> "N/A"
            }
        }
        return when {
            ctx.literal() != null -> {
                val valueNode = visit(ctx.literal())
                assert(valueNode is ExprNode, "Expected expr children to be ExprNodes")
                valueNode
            }
            ctx.bracket_literal() != null -> visit(ctx.bracket_literal())
            ctx.set_literal() != null -> visit(ctx.set_literal())
            ctx.index_expr() != null -> visit(ctx.index_expr())
            ctx.field_access() != null -> visit(ctx.field_access())
            ctx.oclass_literal() != null -> visit(ctx.oclass_literal())
            ctx.fun_call() != null -> visit(ctx.fun_call())
            // Prefix & / | have no semantic effect (TLA+ style guard formatting)
            (ctx.AND() != null || ctx.OR() != null) && ctx.expr().size == 1 -> {
                val innerNode = visit(ctx.expr(0))
                if (innerNode !is ExprNode) {
                    throw RuntimeException("Expected expr children to be ExprNodes")
                }
                innerNode
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
            ternaryOpMapper() == "if-else" -> {
                val condNode = visit(ctx.expr(0))
                val thenNode = visit(ctx.expr(1))
                val elseNode = visit(ctx.expr(2))
                if (condNode !is ExprNode || thenNode !is ExprNode || elseNode !is ExprNode) {
                    throw RuntimeException("Expected expr children to be ExprNodes")
                }
                IfElseExprNode(condNode, thenNode, elseNode, sourceLocation(ctx))
            }
            ternaryOpMapper() == "let" -> {
                val name = ctx.ID().text
                val typeExpr = parseTypeExpr(ctx.typeExpr())
                val letInitNode = visit(ctx.expr(0))
                val bodyNode = visit(ctx.expr(1))
                if (letInitNode !is ExprNode || bodyNode !is ExprNode) {
                    throw RuntimeException("Expected expr children to be ExprNodes")
                }
                LetExprNode(name, typeExpr, letInitNode, bodyNode, sourceLocation(ctx))
            }
            ternaryOpMapper() == "when" -> {
                if (ctx.LPAREN() != null) {
                    val subjectNode = visit(ctx.expr(0))
                    if (subjectNode !is ExprNode) {
                        throw RuntimeException("Expected when subject to be an expression")
                    }
                    val arms = ctx.when_subject_arm().map { parseWhenSubjectArm(it) }
                    WhenExprNode(subjectNode, arms, sourceLocation(ctx))
                } else {
                    val arms = ctx.when_guard_arm().map { parseWhenGuardArm(it) }
                    WhenExprNode(null, arms, sourceLocation(ctx))
                }
            }
            ctx.LPAREN() != null -> visit(ctx.expr(0))
            else -> throw RuntimeException("Invalid expr node: ${ctx.text}")
        }
    }

    private fun parseWhenSubjectArm(ctx: JulayParser.When_subject_armContext): WhenArm {
        return if (ctx.ELSE() != null) {
            val expr = visit(ctx.expr())
            if (expr !is ExprNode) {
                throw RuntimeException("Expected when arm expression to be an ExprNode")
            }
            WhenArm.Else(expr)
        } else {
            val pattern = parseWhenPattern(ctx.when_pattern())
            val expr = visit(ctx.expr())
            if (expr !is ExprNode) {
                throw RuntimeException("Expected when arm expression to be an ExprNode")
            }
            WhenArm.Subject(pattern, expr)
        }
    }

    private fun parseWhenPattern(ctx: JulayParser.When_patternContext): WhenPattern {
        return when {
            ctx.literal() != null -> WhenPattern.Primitive(parseWhenLiteral(ctx.literal()))
            ctx.oclass_literal() != null -> {
                val literal = visit(ctx.oclass_literal())
                if (literal !is ObjClassLiteralExprNode) {
                    throw RuntimeException("Expected o-class literal in when pattern")
                }
                WhenPattern.Struct(literal)
            }
            else -> throw RuntimeException("Invalid when pattern: ${ctx.text}")
        }
    }

    private fun parseWhenGuardArm(ctx: JulayParser.When_guard_armContext): WhenArm {
        return if (ctx.ELSE() != null) {
            val expr = visit(ctx.expr(0))
            if (expr !is ExprNode) {
                throw RuntimeException("Expected when arm expression to be an ExprNode")
            }
            WhenArm.Else(expr)
        } else {
            val cond = visit(ctx.expr(0))
            val expr = visit(ctx.expr(1))
            if (cond !is ExprNode || expr !is ExprNode) {
                throw RuntimeException("Expected when arm expressions to be ExprNodes")
            }
            WhenArm.Guard(cond, expr)
        }
    }

    private fun parseWhenLiteral(ctx: JulayParser.LiteralContext): WhenLiteral {
        return when {
            ctx.INT() != null -> WhenLiteral.IntLit(ctx.INT().text)
            ctx.REAL() != null -> WhenLiteral.RealLit(ctx.REAL().text)
            ctx.STRING() != null -> {
                val rawStr = ctx.STRING().text
                WhenLiteral.StringLit(rawStr.substring(1, rawStr.length - 1))
            }
            ctx.TRUE() != null -> WhenLiteral.BoolLit(ctx.TRUE().text)
            ctx.FALSE() != null -> WhenLiteral.BoolLit(ctx.FALSE().text)
            else -> throw RuntimeException("Invalid when literal: ${ctx.text}")
        }
    }

    override fun visitProc_expr(ctx: JulayParser.Proc_exprContext?): ASTNode {
        return when {
            ctx!!.qualified_name() != null -> {
                val qn = visit(ctx.qualified_name()) as QualifiedNameNode
                val parts = qn.parts()
                ValueProcExprNode(parts.last(), parts, sourceLocation(ctx))
            }
            ctx.ID() != null -> {
                ValueProcExprNode(ctx.ID().text, null, sourceLocation(ctx))
            }
            else -> {
                val compositeProcs = ctx.proc_expr().map { visit(it) }
                CompositeProcExprNode(compositeProcs, sourceLocation(ctx))
            }
        }
    }

    override fun visitOclass_literal(ctx: JulayParser.Oclass_literalContext?): ASTNode {
        val typeExpr = parseTypeExpr(ctx!!.typeExpr())
        val fieldEntries = ctx.oclass_field_assign().map { assign ->
            val fieldName = assign.ID().text
            val expr = visit(assign.expr())
            if (expr !is ExprNode) {
                throw RuntimeException("Expected o-class literal field value to be an expression")
            }
            fieldName to expr
        }
        return ObjClassLiteralExprNode(typeExpr, fieldEntries, sourceLocation(ctx))
    }

    override fun visitField_access(ctx: JulayParser.Field_accessContext?): ASTNode {
        val ids = ctx!!.ID().map { it.text }
        if (ids.size == 1) {
            return SymbolValueExprNode(ids[0], sourceLocation(ctx))
        }
        return FieldAccessExprNode(ids[0], ids.drop(1), sourceLocation(ctx))
    }

    override fun visitLiteral(ctx: JulayParser.LiteralContext?): ASTNode {
        return if (ctx!!.INT() != null) {
            LiteralValueExprNode(ctx.INT().text, intType, sourceLocation(ctx))
        } else if (ctx.REAL() != null) {
            LiteralValueExprNode(ctx.REAL().text, realType, sourceLocation(ctx))
        } else if (ctx.TRUE() != null) {
            LiteralValueExprNode(ctx.TRUE().text, boolType, sourceLocation(ctx))
        } else if (ctx.FALSE() != null) {
            LiteralValueExprNode(ctx.FALSE().text, boolType, sourceLocation(ctx))
        } else if (ctx.STRING() != null) {
            val rawStr = ctx.STRING().text
            val unquotedStr = rawStr.substring(1,rawStr.length-1)
            LiteralValueExprNode(unquotedStr, stringType, sourceLocation(ctx))
        } else {
            throw RuntimeException("Invalid visitLiteral: invalid expression found: ${ctx.text}")
        }
    }

    override fun visitBracket_literal(ctx: JulayParser.Bracket_literalContext?): ASTNode {
        if (ctx!!.map_entry().isNotEmpty()) {
            val entries = ctx.map_entry().map { entry ->
                val key = visit(entry.expr(0)) as ExprNode
                val value = visit(entry.expr(1)) as ExprNode
                key to value
            }
            return MapLiteralExprNode(entries, sourceLocation(ctx))
        }
        if (ctx.expr().isEmpty()) {
            return EmptyBracketLiteralExprNode(sourceLocation(ctx))
        }
        val elements = ctx.expr().map { visit(it) as ExprNode }
        return ListLiteralExprNode(elements, sourceLocation(ctx))
    }

    override fun visitSet_literal(ctx: JulayParser.Set_literalContext?): ASTNode {
        val elements = ctx!!.expr().map { visit(it) as ExprNode }
        return SetLiteralExprNode(elements, sourceLocation(ctx))
    }

    override fun visitIndex_expr(ctx: JulayParser.Index_exprContext?): ASTNode {
        val indexOrSlice = ctx!!.index_or_slice()
        val base: ExprNode = when {
            ctx.index_expr() != null -> visit(ctx.index_expr()) as ExprNode
            ctx.fun_call() != null -> visit(ctx.fun_call()) as ExprNode
            ctx.field_access() != null -> visit(ctx.field_access()) as ExprNode
            ctx.bracket_literal() != null -> visit(ctx.bracket_literal()) as ExprNode
            ctx.set_literal() != null -> visit(ctx.set_literal()) as ExprNode
            ctx.expr() != null -> visit(ctx.expr()) as ExprNode
            else -> throw RuntimeException("Invalid index_expr at ${ctx.text}")
        }
        return if (indexOrSlice.COLON() != null) {
            SliceExprNode(
                base,
                visit(indexOrSlice.expr(0)) as ExprNode,
                visit(indexOrSlice.expr(1)) as ExprNode,
                sourceLocation(ctx),
            )
        } else {
            IndexExprNode(base, visit(indexOrSlice.expr(0)) as ExprNode, sourceLocation(ctx))
        }
    }
}
