package julay.compiler.ast

import julay.compiler.SourceLoc
import julay.compiler.TypeExpr
import julay.parser.JulayParser
import julay.parser.JulayParserBaseVisitor
import julay.program.action.TSAction
import julay.program.type.boolType
import julay.program.type.intType
import julay.program.type.realType
import julay.program.type.stringType
import julay.tools.assert
import org.antlr.v4.runtime.ParserRuleContext
import java.nio.file.Path

fun oneChoice(vararg choices : ParserRuleContext?) : ParserRuleContext {
    val nonNullChoices = choices.filterNotNull()
    assert(nonNullChoices.size == 1, "Expected one choice but got: ${nonNullChoices.size}")
    return nonNullChoices[0]
}

/** Literal `true` in a guarantee means no guarantee (same as omitting it). */
private fun normalizeGuarantee(expr: ExprNode): ExprNode? =
    if (expr is LiteralValueExprNode && expr.isTrueLiteral()) null else expr

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

    /** Location of the header through `args` (not the `{ ... }` body). */
    private fun signatureLocation(ctx: ParserRuleContext, argsCtx: ParserRuleContext) =
        SourceLoc(Pair(ctx.getStart().line, argsCtx.getStop().line), sourcePath)

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
        val parts = ctx!!.name_id().map { it.text }
        return QualifiedNameNode(parts, sourceLocation(ctx))
    }

    override fun visitDecl(ctx: JulayParser.DeclContext?): ASTNode {
        val decl = oneChoice(
            ctx!!.proc(),
            ctx.api_decl(),
            ctx.obj(),
            ctx.sort_decl(),
            ctx.compile_decl(),
            ctx.spec(),
            ctx.invariant_decl(),
            ctx.fun_decl(),
            ctx.procfun_decl(),
        )
        val node = visit(decl)
        if (node is DeclNode && ctx.EXPORT() != null) {
            node.visibility = DeclVisibility.Export
        }
        return node
    }

    override fun visitSort_decl(ctx: JulayParser.Sort_declContext?): ASTNode {
        val name = ctx!!.ID().text
        val elements = ctx.literal().map { litCtx ->
            val node = visit(litCtx)
            if (node !is LiteralValueExprNode) {
                throw RuntimeException("Expected LiteralValueExprNode but got $node")
            }
            node
        }
        return SortDeclNode(name, elements, sourceLocation(ctx))
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

    override fun visitProcfun_decl(ctx: JulayParser.Procfun_declContext?): ASTNode {
        val name = ctx!!.ID().text
        val args = visit(ctx.args()).let { argsNode ->
            if (argsNode !is ArgsNode) {
                throw RuntimeException("Expected ArgsNode but got $argsNode")
            }
            argsNode
        }
        val returnType = parseTypeExpr(ctx.typeExpr())
        val localDecls = ctx.procfun_body()
            .map { visit(it) }
            .map {
                if (it !is ProcClassDeclNode) {
                    throw RuntimeException("Expected ProcClassDeclNode but got $it")
                }
                it
            }
        return ProcFunNode(name, args, returnType, localDecls, sourceLocation(ctx))
    }

    override fun visitProcfun_body(ctx: JulayParser.Procfun_bodyContext?): ASTNode {
        val body = oneChoice(ctx!!.`var`(), ctx.constructor(), ctx.transition())
        return visit(body)
    }

    override fun visitFun_call(ctx: JulayParser.Fun_callContext?): ASTNode {
        val name = ctx!!.ID().text
        val typeArgs = ctx.typeArgs()?.typeExpr()?.map { parseTypeExpr(it) } ?: emptyList()
        val args = ctx.call_arg().map { visitCallArg(it) }
        return FunCallExprNode(name, args, sourceLocation(ctx), typeArgs = typeArgs)
    }

    override fun visitMethod_call(ctx: JulayParser.Method_callContext?): ASTNode {
        val ids = ctx!!.ID().map { it.text }
        val args = ctx.call_arg().map { visitCallArg(it) }
        if (ctx.THIS() != null) {
            // this.xs.filter(...) — at least one ID after THIS (receiver + method).
            require(ids.size >= 2) { "method_call on this requires a dotted receiver at ${ctx.text}" }
            val methodName = ids.last()
            val basePath = ids.dropLast(1)
            val base = ThisAccessExprNode(basePath, sourceLocation(ctx))
            return MethodCallExprNode(base, methodName, args, sourceLocation(ctx))
        }
        require(ids.size >= 2) { "method_call requires a dotted receiver at ${ctx.text}" }
        val methodName = ids.last()
        val baseIds = ids.dropLast(1)
        val base: ExprNode = if (baseIds.size == 1) {
            SymbolValueExprNode(baseIds[0], sourceLocation(ctx))
        } else {
            FieldAccessExprNode(baseIds[0], baseIds.drop(1), sourceLocation(ctx))
        }
        return MethodCallExprNode(base, methodName, args, sourceLocation(ctx))
    }

    override fun visitLambda_expr(ctx: JulayParser.Lambda_exprContext?): ASTNode {
        val params = ctx!!.ID().map { it.text }
        val body = visit(ctx.expr()) as ExprNode
        return LambdaExprNode(params, body, sourceLocation(ctx))
    }

    private fun visitCallArg(ctx: JulayParser.Call_argContext): ExprNode {
        if (ctx.lambda_expr() != null) {
            return visit(ctx.lambda_expr()) as ExprNode
        }
        val e = visit(ctx.expr())
        if (e !is ExprNode) {
            throw RuntimeException("Expected call argument to be an expression")
        }
        return e
    }

    override fun visitObj(ctx: JulayParser.ObjContext?): ASTNode {
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

    override fun visitProc(ctx: JulayParser.ProcContext?): ASTNode {
        val name = ctx!!.ID().text
        if (ctx.LCURLY() != null) {
            val localDecls = ctx.proc_body()
                .map { visit(it) }
                .map {
                    if (it !is ProcClassDeclNode) {
                        throw RuntimeException("Expected ProcClassDeclNode but got $it")
                    }
                    it
                }
            return ProcClassNode(name, localDecls, sourceLocation(ctx))
        }
        val value = visit(ctx.proc_expr())
        return ProcNode(name, value, sourceLocation(ctx))
    }

    override fun visitApi_decl(ctx: JulayParser.Api_declContext?): ASTNode {
        val name = ctx!!.ID().text
        val procExpr = visit(ctx.proc_expr())
        val callNames = ctx.api_call_list()?.ID()?.map { it.text } ?: emptyList()
        return ApiNode(name, procExpr, callNames, sourceLocation(ctx))
    }

    override fun visitCompile_decl(ctx: JulayParser.Compile_declContext?): ASTNode {
        val names = ctx!!.ID().map { it.text }
        return CompileNode(names, sourceLocation(ctx))
    }

    override fun visitSpec(ctx: JulayParser.SpecContext?): ASTNode {
        val name = ctx!!.ID(0).text
        // Leaf form: spec Name [p : T]? { ... }
        if (ctx.LCURLY() != null) {
            val localDecls = ctx.proc_body()
                .map { visit(it) }
                .map {
                    if (it !is ProcClassDeclNode) {
                        throw RuntimeException("Expected ProcClassDeclNode but got $it")
                    }
                    it
                }
            val paramName = if (ctx.LBRACK() != null) ctx.ID(1).text else null
            val paramType = if (ctx.LBRACK() != null) parseTypeExpr(ctx.typeExpr()) else null
            return LeafSpecNode(name, paramName, paramType, localDecls, sourceLocation(ctx))
        }
        val value = when {
            ctx.ag_spec() != null -> visit(ctx.ag_spec())
            ctx.MODELS() != null -> {
                val system = visit(ctx.system_expr())
                val guaranteeRaw = visit(ctx.expr())
                if (guaranteeRaw !is ExprNode) {
                    throw RuntimeException("Expected guarantee to be an expression")
                }
                AgSpecExprNode(
                    assume = null,
                    system = system,
                    guarantee = normalizeGuarantee(guaranteeRaw),
                    loc = sourceLocation(ctx),
                )
            }
            ctx.system_expr() != null -> visit(ctx.system_expr())
            else -> throw RuntimeException("Invalid spec body")
        }
        return SpecNode(name, value, sourceLocation(ctx))
    }

    override fun visitAg_spec(ctx: JulayParser.Ag_specContext?): ASTNode {
        val assumeCtx = ctx!!.assume_expr()
        val assume: ASTNode? = if (assumeCtx.TRUE() != null) {
            null
        } else {
            visit(assumeCtx.system_expr())
        }
        val system = visit(ctx.system_expr())
        val guaranteeRaw = visit(ctx.expr())
        if (guaranteeRaw !is ExprNode) {
            throw RuntimeException("Expected guarantee to be an expression")
        }
        return AgSpecExprNode(assume, system, normalizeGuarantee(guaranteeRaw), sourceLocation(ctx))
    }

    override fun visitSystem_expr(ctx: JulayParser.System_exprContext?): ASTNode {
        return when {
            ctx!!.PARALLEL() != null -> {
                val left = visit(ctx.system_expr(0))
                val right = visit(ctx.system_expr(1))
                CompositeProcExprNode(listOf(left, right), sourceLocation(ctx))
            }
            ctx.with_expr() != null -> visit(ctx.with_expr())
            else -> visit(ctx.system_atom())
        }
    }

    override fun visitWith_expr(ctx: JulayParser.With_exprContext?): ASTNode {
        val binderName = ctx!!.ID().text
        val binderType = parseTypeExpr(ctx.typeExpr())
        val body = visit(ctx.system_expr())
        return WithSpecExprNode(binderName, binderType, body, sourceLocation(ctx))
    }

    override fun visitSystem_atom(ctx: JulayParser.System_atomContext?): ASTNode {
        val primary = visit(ctx!!.system_primary())
        return when {
            ctx.COLON() != null -> {
                val paramName = ctx.ID().text
                val paramType = parseTypeExpr(ctx.typeExpr())
                val globalDecls = ctx.global_decl().map { decl ->
                    GlobalDeclNames(
                        names = decl.ID().map { it.text },
                        isConst = decl.CONST() != null,
                        loc = sourceLocation(decl),
                    )
                }
                ParamProcExprNode(primary, paramName, paramType, sourceLocation(ctx), globalDecls)
            }
            ctx.LBRACK() != null -> {
                val paramName = ctx.ID().text
                ParamProcExprNode(primary, paramName, null, sourceLocation(ctx))
            }
            else -> primary
        }
    }

    override fun visitSystem_primary(ctx: JulayParser.System_primaryContext?): ASTNode {
        return when {
            ctx!!.LPAREN() != null -> visit(ctx.system_expr())
            else -> visit(ctx.system_leaf())
        }
    }

    override fun visitSystem_leaf(ctx: JulayParser.System_leafContext?): ASTNode {
        return when {
            ctx!!.qualified_name() != null -> {
                val qn = visit(ctx.qualified_name()) as QualifiedNameNode
                val parts = qn.parts()
                ValueProcExprNode(parts.last(), parts, sourceLocation(ctx))
            }
            else -> ValueProcExprNode(ctx.ID().text, null, sourceLocation(ctx))
        }
    }

    override fun visitInvariant_decl(ctx: JulayParser.Invariant_declContext?): ASTNode {
        val name = ctx!!.ID().text
        val formula = visit(ctx.expr())
        if (formula !is ExprNode) {
            throw RuntimeException("Expected invariant formula to be an expression")
        }
        return InvariantNode(name, formula, sourceLocation(ctx))
    }

    override fun visitProc_body(ctx: JulayParser.Proc_bodyContext?): ASTNode {
        val body = oneChoice(ctx!!.`var`(), ctx.constructor(), ctx.transition())
        return visit(body)
    }

    override fun visitVar(ctx: JulayParser.VarContext?): ASTNode {
        val name = ctx!!.ID().text
        val typeExpr = parseTypeExpr(ctx.typeExpr())
        val isConst = ctx.CONST() != null
        val initExpr = ctx.expr()?.let { visit(it) }?.also {
            if (it !is ExprNode) {
                throw RuntimeException("Expected init expression but got $it")
            }
        } as ExprNode?
        return VarNode(name, typeExpr, sourceLocation(ctx), isConst = isConst, initExpr = initExpr)
    }

    override fun visitConstructor(ctx: JulayParser.ConstructorContext?): ASTNode {
        val name = ctx!!.ID().text
        val isSession = ctx.SESSION() != null
        val alsoArgs = if (ctx.ALSO() != null) {
            val allArgs = ctx.args()
            val alsoNode = visit(allArgs[allArgs.size - 1])
            if (alsoNode !is ArgsNode) throw RuntimeException("Expected ArgsNode for also")
            alsoNode
        } else {
            null
        }
        val syncArgsCtx = ctx.args(0)
        val args = visit(syncArgsCtx).let { argsNode ->
            if (argsNode !is ArgsNode) {
                throw RuntimeException("Expected ArgsNode but got $argsNode")
            }
            argsNode
        }
        val body = ctx.constructor_body()
            .map { visit(it) }
            .map {
                if (it !is ActionBodyNode) {
                    throw RuntimeException("Expected ActionBody but got $it")
                }
                it
            }
        return ConstructorNode(name, args, body, signatureLocation(ctx, syncArgsCtx), isSession, alsoArgs)
    }

    override fun visitTransition(ctx: JulayParser.TransitionContext?): ASTNode {
        val isProvider = ctx!!.PROVIDER() != null
        val isClient = ctx.CLIENT() != null
        val isInternal = ctx.INTERNAL() != null
        val isSession = ctx.SESSION() != null
        assert(!isProvider || !isInternal, "A transition cannot be both provider and internal")
        assert(!isClient || !isInternal, "A transition cannot be both client and internal")
        assert(!isProvider || !isClient, "A transition cannot be both provider and client")
        assert(!isSession || !isProvider, "A transition cannot be both session and provider")
        assert(!isSession || !isClient, "A transition cannot be both session and client")
        assert(!isSession || !isInternal, "A transition cannot be both session and internal")
        val modifier = when {
            isProvider -> TSAction.SyncRole.Provider
            isClient -> TSAction.SyncRole.Client
            isInternal -> TSAction.SyncRole.Internal
            else -> TSAction.SyncRole.Default
        }
        val name = ctx.ID().text
        val alsoArgs = if (ctx.ALSO() != null) {
            val allArgs = ctx.args()
            val alsoNode = visit(allArgs[allArgs.size - 1])
            if (alsoNode !is ArgsNode) throw RuntimeException("Expected ArgsNode for also")
            alsoNode
        } else {
            null
        }
        val syncArgsCtx = ctx.args(0)
        val args = visit(syncArgsCtx).let { argsNode ->
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
        return TransitionNode(modifier, name, args, body, signatureLocation(ctx, syncArgsCtx), isSession, alsoArgs)
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
        val body = oneChoice(
            ctx!!.guard(),
            ctx.before(),
            ctx.transit(),
            ctx.error(),
            ctx.after(),
            ctx.return_clause(),
        )
        return visit(body)
    }

    override fun visitReturn_clause(ctx: JulayParser.Return_clauseContext?): ASTNode {
        val expr = visit(ctx!!.expr())
        if (expr !is ExprNode) {
            throw RuntimeException("Expected return expression")
        }
        return ReturnNode(expr, sourceLocation(ctx))
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

    override fun visitBefore(ctx: JulayParser.BeforeContext?): ASTNode {
        val stmts = ctx!!.call_stmt()
            .map { visit(it) }
            .map {
                if (it !is CallStmtNode) {
                    throw RuntimeException("Expected CallStmtNode but got $it")
                }
                it
            }
        return BeforeNode(stmts, sourceLocation(ctx))
    }

    override fun visitAfter(ctx: JulayParser.AfterContext?): ASTNode {
        val stmts = ctx!!.call_stmt()
            .map { visit(it) }
            .map {
                if (it !is CallStmtNode) {
                    throw RuntimeException("Expected CallStmtNode but got $it")
                }
                it
            }
        return AfterNode(stmts, sourceLocation(ctx))
    }

    override fun visitCall_stmt(ctx: JulayParser.Call_stmtContext?): ASTNode {
        val name = ctx!!.ID().text
        val typeArgs = ctx.typeArgs()?.typeExpr()?.map { parseTypeExpr(it) } ?: emptyList()
        val args = ctx.expr().map { visit(it) }.map {
            if (it !is ExprNode) {
                throw RuntimeException("Expected call statement arguments to be expressions")
            }
            it
        }
        return CallStmtNode(name, args, sourceLocation(ctx), typeArgs)
    }

    override fun visitVar_transit(ctx: JulayParser.Var_transitContext?): ASTNode {
        if (ctx!!.LET() != null) {
            val name = ctx.ID().text
            val typeExpr = parseTypeExpr(ctx.typeExpr())
            val init = visit(ctx.expr(0))
            if (init !is ExprNode) {
                throw RuntimeException("Expected transit let initializer to be an expression")
            }
            return LetTransitNode(name, typeExpr, init, sourceLocation(ctx))
        }
        if (ctx.LBRACK() != null) {
            // xs[i] := … or this.xs[i] := … (both mean state collection update)
            val collectionVar = ctx.ID().text
            val index = visit(ctx.expr(0))
            val value = visit(ctx.expr(1))
            if (index !is ExprNode || value !is ExprNode) {
                throw RuntimeException("Expected expressions in index transit assignment")
            }
            return IndexTransitNode(collectionVar, index, value, sourceLocation(ctx))
        }
        val lhs = visit(ctx.field_access())
        val transit = visit(ctx.expr(0))
        if (transit !is ExprNode) {
            throw RuntimeException("Expected transit to be assigned an expr")
        }
        return when (lhs) {
            is ThisAccessExprNode ->
                VarTransitNode(lhs.stateVarName(), lhs.nestedFieldPath(), transit, sourceLocation(ctx))
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
                else -> "N/A"
            }
        }
        val binaryOpMapper = {
            when {
                ctx.EQ() != null -> "="
                ctx.NEQ() != null -> "#"
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
                ctx.IFF() != null -> "<=>"
                ctx.IN() != null -> "in"
                ctx.NIN() != null -> "~in"
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
            ctx.collection_literal() != null -> visit(ctx.collection_literal())
            ctx.method_prop_expr() != null -> visit(ctx.method_prop_expr())
            ctx.index_expr() != null -> visit(ctx.index_expr())
            ctx.field_access() != null -> visit(ctx.field_access())
            ctx.obj_literal() != null -> visit(ctx.obj_literal())
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
            binaryOpMapper() == "<=>" -> {
                // a <=> b ≡ (a => b) & (b => a)
                val lhsNode = visit(ctx.expr(0))
                val rhsNode = visit(ctx.expr(1))
                if (lhsNode !is ExprNode || rhsNode !is ExprNode) {
                    throw RuntimeException("Expected expr children to be ExprNodes")
                }
                val loc = sourceLocation(ctx)
                val forward = BinaryOpExprNode("=>", lhsNode, rhsNode, loc)
                val backward = BinaryOpExprNode("=>", rhsNode, lhsNode, loc)
                BinaryOpExprNode("&", forward, backward, loc)
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
            ctx.FORALL() != null || ctx.EXISTS() != null -> {
                val binder = ctx.ID().text
                val binderType = parseTypeExpr(ctx.typeExpr())
                val bodyNode = visit(ctx.expr(0))
                if (bodyNode !is ExprNode) {
                    throw RuntimeException("Expected quantified body to be an expression")
                }
                QuantifiedExprNode(
                    universal = ctx.FORALL() != null,
                    binder = binder,
                    binderType = binderType,
                    body = bodyNode,
                    loc = sourceLocation(ctx),
                )
            }
            ctx.LPAREN() != null -> {
                val innerNode = visit(ctx.expr(0))
                if (innerNode !is ExprNode) {
                    throw RuntimeException("Expected expr children to be ExprNodes")
                }
                ParenExprNode(innerNode, sourceLocation(ctx))
            }
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
            ctx.obj_literal() != null -> {
                val literal = visit(ctx.obj_literal())
                if (literal !is ObjClassLiteralExprNode) {
                    throw RuntimeException("Expected obj literal in when pattern")
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

    override fun visitObj_literal(ctx: JulayParser.Obj_literalContext?): ASTNode {
        val typeExpr = parseTypeExpr(ctx!!.typeExpr())
        val fieldEntries = ctx.obj_field_assign().map { assign ->
            val fieldName = assign.ID().text
            val expr = visit(assign.expr())
            if (expr !is ExprNode) {
                throw RuntimeException("Expected obj literal field value to be an expression")
            }
            fieldName to expr
        }
        return ObjClassLiteralExprNode(typeExpr, fieldEntries, sourceLocation(ctx))
    }

    override fun visitField_access(ctx: JulayParser.Field_accessContext?): ASTNode {
        val ids = ctx!!.ID().map { it.text }
        if (ctx.THIS() != null) {
            require(ids.isNotEmpty()) { "this access requires a state field at ${ctx.text}" }
            return ThisAccessExprNode(ids, sourceLocation(ctx))
        }
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

    override fun visitCollection_literal(ctx: JulayParser.Collection_literalContext?): ASTNode {
        return when {
            ctx!!.list_literal() != null -> visit(ctx.list_literal())
            ctx.set_literal() != null -> visit(ctx.set_literal())
            ctx.map_literal() != null -> visit(ctx.map_literal())
            else -> throw RuntimeException("Invalid collection_literal: ${ctx.text}")
        }
    }

    override fun visitList_literal(ctx: JulayParser.List_literalContext?): ASTNode {
        val elements = ctx!!.expr().map { visit(it) as ExprNode }
        return ListLiteralExprNode(elements, sourceLocation(ctx))
    }

    override fun visitSet_literal(ctx: JulayParser.Set_literalContext?): ASTNode {
        val elements = ctx!!.expr().map { visit(it) as ExprNode }
        return SetLiteralExprNode(elements, sourceLocation(ctx))
    }

    override fun visitMap_literal(ctx: JulayParser.Map_literalContext?): ASTNode {
        val entries = ctx!!.map_entry().map { entry ->
            val key = visit(entry.expr(0)) as ExprNode
            val value = visit(entry.expr(1)) as ExprNode
            key to value
        }
        return MapLiteralExprNode(entries, sourceLocation(ctx))
    }

    override fun visitMethod_prop_expr(ctx: JulayParser.Method_prop_exprContext?): ASTNode {
        var expr: ExprNode = visit(ctx!!.method_call()) as ExprNode
        for (id in ctx.ID()) {
            expr = MemberAccessExprNode(expr, id.text, sourceLocation(ctx))
        }
        return expr
    }

    override fun visitIndex_expr(ctx: JulayParser.Index_exprContext?): ASTNode {
        // Postfix method call: base DOT ID LPAREN ...
        // Note: LPAREN() returns List<TerminalNode> (never null); check isNotEmpty().
        if (ctx!!.DOT() != null && ctx.ID() != null && ctx.LPAREN().isNotEmpty()) {
            val base: ExprNode = when {
                ctx.index_expr() != null -> visit(ctx.index_expr()) as ExprNode
                ctx.fun_call() != null -> visit(ctx.fun_call()) as ExprNode
                ctx.field_access() != null -> visit(ctx.field_access()) as ExprNode
                ctx.collection_literal() != null -> visit(ctx.collection_literal()) as ExprNode
                ctx.expr().isNotEmpty() -> visit(ctx.expr(0)) as ExprNode
                else -> throw RuntimeException("Invalid method call base at ${ctx.text}")
            }
            val args = ctx.call_arg().map { visitCallArg(it) }
            return MethodCallExprNode(base, ctx.ID().text, args, sourceLocation(ctx))
        }
        // Postfix .field: ... DOT ID
        if (ctx.DOT() != null && ctx.ID() != null) {
            val base: ExprNode = when {
                ctx.index_expr() != null -> visit(ctx.index_expr()) as ExprNode
                ctx.fun_call() != null -> visit(ctx.fun_call()) as ExprNode
                ctx.field_access() != null -> visit(ctx.field_access()) as ExprNode
                ctx.collection_literal() != null -> visit(ctx.collection_literal()) as ExprNode
                ctx.expr().isNotEmpty() -> visit(ctx.expr(0)) as ExprNode
                else -> throw RuntimeException("Invalid member-access base at ${ctx.text}")
            }
            return MemberAccessExprNode(base, ctx.ID().text, sourceLocation(ctx))
        }
        // Indexing: base [ expr ]
        val exprs = ctx.expr()
        if (exprs.isEmpty()) {
            throw RuntimeException("Invalid index_expr at ${ctx.text}")
        }
        val indexExpr = visit(exprs[exprs.size - 1]) as ExprNode
        val base: ExprNode = when {
            ctx.index_expr() != null -> visit(ctx.index_expr()) as ExprNode
            ctx.fun_call() != null -> visit(ctx.fun_call()) as ExprNode
            ctx.field_access() != null -> visit(ctx.field_access()) as ExprNode
            ctx.collection_literal() != null -> visit(ctx.collection_literal()) as ExprNode
            exprs.size >= 2 -> visit(exprs[0]) as ExprNode
            else -> throw RuntimeException("Invalid index_expr at ${ctx.text}")
        }
        return IndexExprNode(base, indexExpr, sourceLocation(ctx))
    }
}
