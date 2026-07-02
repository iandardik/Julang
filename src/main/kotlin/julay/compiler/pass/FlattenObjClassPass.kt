package julay.compiler.pass

import julay.compiler.*
import julay.compiler.ast.*
import julay.compiler.decl.*
import julay.program.*

fun flattenObjClassPass(root: RootNode, registry: ObjClassRegistry): RootNode {
    val flatDecls = root.children.filterIsInstance<DeclNode>().map { decl ->
        when (decl) {
            is ProcClassNode -> decl.flatten(registry)
            else -> decl
        }
    }
    return root.withDeclNodes(flatDecls)
}

private fun ProcClassNode.flatten(registry: ObjClassRegistry): ProcClassNode {
    val logicalStateEnv = localDecls().filterIsInstance<VarNode>().associate { it.name to it.type }
    val flatVarNodes = localDecls().filterIsInstance<VarNode>().flatMap { varNode ->
        varNode.type.flattenStateVariables(varNode.name).map { variable ->
            VarNode.primitive(variable.name, variable.type, varNode.programLocation())
        }
    }
    val flatStateEnv = flatVarNodes.associate { it.name to it.type }
    val flatOtherDecls = localDecls().filterNot { it is VarNode }.map { decl ->
        when (decl) {
            is ConstructorNode -> decl.flatten(logicalStateEnv, flatStateEnv)
            is TransitionNode -> decl.flatten(logicalStateEnv, flatStateEnv)
            else -> decl
        }
    }
    val flatNode = withLocalDecls(flatVarNodes + flatOtherDecls)
    flatNode.typePass(emptyMap(), registry)
    return flatNode
}

private fun ConstructorNode.flatten(
    logicalStateEnv: Map<String, Type>,
    flatStateEnv: Map<String, Type>,
): ConstructorNode =
    withBody(flattenActionBody(body(), logicalStateEnv, flatStateEnv, actionArgs()))

private fun TransitionNode.flatten(
    logicalStateEnv: Map<String, Type>,
    flatStateEnv: Map<String, Type>,
): TransitionNode =
    withBody(flattenActionBody(body(), logicalStateEnv, flatStateEnv, actionArgs()))

private fun logicalEnv(
    logicalStateEnv: Map<String, Type>,
    actionArgs: List<Variable>,
): Map<String, Type> = logicalStateEnv + actionArgs.associate { it.name to it.type }

private fun flattenActionBody(
    body: List<ActionBodyNode>,
    logicalStateEnv: Map<String, Type>,
    flatStateEnv: Map<String, Type>,
    actionArgs: List<Variable>,
): List<ActionBodyNode> {
    val logicalSymbolEnv = logicalEnv(logicalStateEnv, actionArgs)
    val fullEnv = flatStateEnv + flattenActionArgEnv(actionArgs)
    val argSymbols = flattenedArgSymbols(actionArgs)
    return body.flatMap { node ->
        when (node) {
            is GuardNode -> listOf(
                GuardNode(flattenExpr(node.guardExpr(), logicalSymbolEnv, fullEnv, argSymbols), node.programLocation()),
            )
            is TransitNode -> listOf(TransitNode(node.transitBodies().flatMap { transitBody ->
                when (transitBody) {
                    is VarTransitNode -> flattenVarTransit(transitBody, logicalStateEnv, logicalSymbolEnv, fullEnv, argSymbols)
                    else -> listOf(transitBody)
                }
            }, node.programLocation()))
            is EffectNode -> listOf(EffectNode(node.effects().map { effect ->
                flattenEffectStmt(effect, logicalStateEnv, logicalSymbolEnv, fullEnv, argSymbols)
            }, node.programLocation()))
            else -> listOf(node)
        }
    }
}

private fun flattenEffectStmt(
    stmt: EffectStmtNode,
    logicalStateEnv: Map<String, Type>,
    logicalSymbolEnv: Map<String, Type>,
    fullEnv: Map<String, Type>,
    argSymbols: Set<String>,
): EffectStmtNode {
    return when (stmt) {
        is EffectCallNode -> EffectCallNode(
            stmt.callName(),
            stmt.callArgs().map { flattenExpr(it, logicalSymbolEnv, fullEnv, argSymbols) },
            stmt.programLocation(),
        )
        is EffectAssignNode -> {
            if (stmt.fieldPath.isNotEmpty()) {
                val baseType = logicalStateEnv.getValue(stmt.varName)
                when (val result = resolveFieldPath(baseType, stmt.fieldPath)) {
                    is FieldPathResult.Error -> throw RuntimeException(result.message)
                    is FieldPathResult.Resolved -> {
                        if (result.type is ObjClassType) {
                            throw RuntimeException(
                                "Cannot assign effect result to o-class field \"${stmt.assignKey()}\" at ${stmt.programLocation()}",
                            )
                        }
                        val objClassType = baseType as ObjClassType
                        val flatName = objClassType.flatVarName(stmt.varName, result.relPath)
                        EffectAssignNode(
                            flatName,
                            emptyList(),
                            stmt.callName(),
                            stmt.callArgs().map { flattenExpr(it, logicalSymbolEnv, fullEnv, argSymbols) },
                            stmt.programLocation(),
                        )
                    }
                }
            } else {
                EffectAssignNode(
                    stmt.varName,
                    emptyList(),
                    stmt.callName(),
                    stmt.callArgs().map { flattenExpr(it, logicalSymbolEnv, fullEnv, argSymbols) },
                    stmt.programLocation(),
                )
            }
        }
    }
}

private fun flattenVarTransit(
    transit: VarTransitNode,
    logicalStateEnv: Map<String, Type>,
    logicalSymbolEnv: Map<String, Type>,
    fullEnv: Map<String, Type>,
    argSymbols: Set<String>,
): List<VarTransitNode> {
    if (transit.fieldPath.isNotEmpty()) {
        val baseType = logicalStateEnv.getValue(transit.varName)
        when (val result = resolveFieldPath(baseType, transit.fieldPath)) {
            is FieldPathResult.Error -> throw RuntimeException(result.message)
            is FieldPathResult.Resolved -> {
                if (result.type is ObjClassType) {
                    throw RuntimeException(
                        "Cannot partially assign to o-class field \"${transit.varName}.${transit.fieldPath.joinToString(".")}\" at ${transit.programLocation()}",
                    )
                }
                val objClassType = baseType as ObjClassType
                val lhs = objClassType.flatVarName(transit.varName, result.relPath)
                val rhs = flattenExpr(transit.expr, logicalSymbolEnv, fullEnv, argSymbols)
                return listOf(VarTransitNode(lhs, emptyList(), rhs, transit.programLocation()))
            }
        }
    }
    val logicalType = logicalStateEnv[transit.varName]
    if (logicalType !is ObjClassType) {
        return listOf(
            VarTransitNode(
                transit.varName,
                emptyList(),
                flattenExpr(transit.expr, logicalSymbolEnv, fullEnv, argSymbols),
                transit.programLocation(),
            ),
        )
    }
    return when (val expr = transit.expr) {
        is ObjClassLiteralExprNode -> logicalType.collectRelativeFlatFields().map { (relPath, _) ->
            val lhs = logicalType.flatVarName(transit.varName, relPath)
            val rhs = fieldExprForLiteralPath(logicalType, expr.fieldAssignments, relPath)
            VarTransitNode(lhs, emptyList(), rhs, transit.programLocation())
        }
        is SymbolValueExprNode -> logicalType.collectRelativeFlatFields().map { (relPath, _) ->
            val lhs = logicalType.flatVarName(transit.varName, relPath)
            val rhs = SymbolValueExprNode(logicalType.flatVarName(expr.symbol, relPath), transit.programLocation())
            VarTransitNode(lhs, emptyList(), rhs, transit.programLocation())
        }
        else -> throw RuntimeException(
            "Cannot assign ${expr.javaClass.simpleName} to o-class variable \"${transit.varName}\" at ${transit.programLocation()}",
        )
    }
}

private fun flattenExpr(
    expr: ExprNode,
    logicalSymbolEnv: Map<String, Type>,
    flatSymbolEnv: Map<String, Type>,
    argSymbols: Set<String>,
): ExprNode {
    return when (expr) {
        is FieldAccessExprNode -> flattenFieldAccess(expr, logicalSymbolEnv)
        is ObjClassLiteralExprNode ->
            throw RuntimeException("O-class literal at ${expr.programLocation()} must appear in an assignment or equality")
        is SymbolValueExprNode -> {
            if (logicalSymbolEnv[expr.symbol] is ObjClassType) {
                throw RuntimeException(
                    "O-class symbol ${expr.symbol} must appear in an assignment or equality at ${expr.programLocation()}",
                )
            }
            expr
        }
        is BinaryOpExprNode -> {
            val lhs = expr.lhsOperand()
            val rhs = expr.rhsOperand()
            if ((expr.op() == "=" || expr.op() == "#") && isObjClassEquality(lhs, rhs, logicalSymbolEnv)) {
                flattenObjClassEquality(expr.op(), lhs, rhs, logicalSymbolEnv, expr.programLocation())
            } else {
                BinaryOpExprNode(
                    expr.op(),
                    flattenExpr(lhs, logicalSymbolEnv, flatSymbolEnv, argSymbols),
                    flattenExpr(rhs, logicalSymbolEnv, flatSymbolEnv, argSymbols),
                    expr.programLocation(),
                )
            }
        }
        is UnaryOpExprNode -> UnaryOpExprNode(
            expr.op(),
            flattenExpr(expr.operand(), logicalSymbolEnv, flatSymbolEnv, argSymbols),
            expr.programLocation(),
        )
        is IfElseExprNode -> IfElseExprNode(
            flattenExpr(expr.condExpr(), logicalSymbolEnv, flatSymbolEnv, argSymbols),
            flattenExpr(expr.thenExpr(), logicalSymbolEnv, flatSymbolEnv, argSymbols),
            flattenExpr(expr.elseExpr(), logicalSymbolEnv, flatSymbolEnv, argSymbols),
            expr.programLocation(),
        )
        is LiteralValueExprNode -> expr
        else -> throw RuntimeException("Unexpected expression node ${expr.javaClass.simpleName} at ${expr.programLocation()}")
    }
}

private fun flattenFieldAccess(expr: FieldAccessExprNode, logicalSymbolEnv: Map<String, Type>): SymbolValueExprNode {
    val baseType = logicalSymbolEnv.getValue(expr.baseSymbol) as ObjClassType
    val result = resolveFieldPath(baseType, expr.fieldPath)
    if (result is FieldPathResult.Error) {
        throw RuntimeException(result.message)
    }
    val resolved = result as FieldPathResult.Resolved
    if (resolved.type is ObjClassType) {
        throw RuntimeException(
            "O-class field ${expr.baseSymbol}.${expr.fieldPath.joinToString(".")} must be used in whole-value equality",
        )
    }
    return SymbolValueExprNode(baseType.flatVarName(expr.baseSymbol, resolved.relPath), expr.programLocation())
}

private fun isObjClassEquality(lhs: ExprNode, rhs: ExprNode, logicalSymbolEnv: Map<String, Type>): Boolean {
    val lhsType = equalityOperandObjClassType(lhs, logicalSymbolEnv)
    val rhsType = equalityOperandObjClassType(rhs, logicalSymbolEnv)
    return lhsType != null && lhsType == rhsType
}

private fun equalityOperandObjClassType(expr: ExprNode, logicalSymbolEnv: Map<String, Type>): ObjClassType? =
    when (expr) {
        is ObjClassLiteralExprNode -> expr.structType
        is SymbolValueExprNode -> logicalSymbolEnv[expr.symbol] as? ObjClassType
        is FieldAccessExprNode -> {
            val baseType = logicalSymbolEnv[expr.baseSymbol] as? ObjClassType ?: return null
            when (val result = resolveFieldPath(baseType, expr.fieldPath)) {
                is FieldPathResult.Resolved -> result.type as? ObjClassType
                is FieldPathResult.Error -> null
            }
        }
        else -> null
    }

private fun flattenObjClassEquality(
    op: String,
    lhs: ExprNode,
    rhs: ExprNode,
    logicalSymbolEnv: Map<String, Type>,
    loc: ProgramLoc,
): ExprNode {
    val lhsSide = classifyEqualitySide(lhs, logicalSymbolEnv)
    val rhsSide = classifyEqualitySide(rhs, logicalSymbolEnv)
    val objClassType = equalitySideObjClassType(lhsSide, rhsSide)
    val fieldEqualities = objClassType.collectRelativeFlatFields().map { (fieldRelPath, _) ->
        val lhsExpr = equalitySideFieldExpr(lhsSide, objClassType, fieldRelPath, loc)
        val rhsExpr = equalitySideFieldExpr(rhsSide, objClassType, fieldRelPath, loc)
        BinaryOpExprNode("=", lhsExpr, rhsExpr, loc)
    }
    val conjunction = fieldEqualities.reduce { acc, eq -> BinaryOpExprNode("&", acc, eq, loc) }
    return if (op == "#") UnaryOpExprNode("~", conjunction, loc) else conjunction
}

private sealed interface EqualitySide {
    data class SymbolRef(
        val baseSymbol: String,
        val relPath: String,
        val objClassType: ObjClassType,
    ) : EqualitySide

    data class LiteralRef(val lit: ObjClassLiteralExprNode) : EqualitySide
}

private fun classifyEqualitySide(
    expr: ExprNode,
    logicalSymbolEnv: Map<String, Type>,
): EqualitySide {
    when (expr) {
        is ObjClassLiteralExprNode -> return EqualitySide.LiteralRef(expr)
        is SymbolValueExprNode -> {
            val objClassType = logicalSymbolEnv[expr.symbol] as? ObjClassType
                ?: throw RuntimeException("Expected o-class symbol at ${expr.programLocation()}")
            return EqualitySide.SymbolRef(expr.symbol, "", objClassType)
        }
        is FieldAccessExprNode -> {
            val baseType = logicalSymbolEnv.getValue(expr.baseSymbol) as ObjClassType
            when (val result = resolveFieldPath(baseType, expr.fieldPath)) {
                is FieldPathResult.Resolved -> {
                    val leafType = result.type as? ObjClassType
                        ?: throw RuntimeException("Expected o-class field access at ${expr.programLocation()}")
                    return EqualitySide.SymbolRef(expr.baseSymbol, result.relPath, leafType)
                }
                is FieldPathResult.Error -> throw RuntimeException(result.message)
            }
        }
        else -> throw RuntimeException("Expected o-class equality operand at ${expr.programLocation()}")
    }
}

private fun equalitySideObjClassType(lhs: EqualitySide, rhs: EqualitySide): ObjClassType {
    val lhsType = when (lhs) {
        is EqualitySide.SymbolRef -> lhs.objClassType
        is EqualitySide.LiteralRef -> lhs.lit.structType
    }
    val rhsType = when (rhs) {
        is EqualitySide.SymbolRef -> rhs.objClassType
        is EqualitySide.LiteralRef -> rhs.lit.structType
    }
    if (lhsType != rhsType) {
        throw RuntimeException("Expected both sides of o-class equality to have the same type, got $lhsType and $rhsType")
    }
    return lhsType
}

private fun equalitySideFieldExpr(
    side: EqualitySide,
    objClassType: ObjClassType,
    fieldRelPath: String,
    loc: ProgramLoc,
): ExprNode {
    return when (side) {
        is EqualitySide.SymbolRef -> {
            val refPath = objClassType.combineRelPath(side.relPath, fieldRelPath)
            SymbolValueExprNode(objClassType.flatVarName(side.baseSymbol, refPath), loc)
        }
        is EqualitySide.LiteralRef ->
            fieldExprForLiteralPath(objClassType, side.lit.fieldAssignments, fieldRelPath)
    }
}

internal fun flattenActionArgEnv(actionArgs: List<Variable>): Map<String, Type> =
    actionArgs.flatMap { arg ->
        when (val ty = arg.type) {
            is ObjClassType -> ty.collectRelativeFlatFields().map { (relPath, fieldType) ->
                ty.flatVarName(arg.name, relPath) to fieldType
            }
            else -> listOf(arg.name to ty)
        }
    }.toMap()

internal fun flattenedArgSymbols(actionArgs: List<Variable>): Set<String> =
    actionArgs.flatMap { arg ->
        when (val ty = arg.type) {
            is ObjClassType -> ty.collectRelativeFlatFields().map { (relPath, _) ->
                ty.flatVarName(arg.name, relPath)
            }
            else -> listOf(arg.name)
        }
    }.toSet()

internal fun fieldExprForLiteralPath(
    objClassType: ObjClassType,
    assignments: Map<String, ExprNode>,
    relPath: String,
): ExprNode {
    val segments = relPath.split("$")
    var currentAssignments = assignments
    var currentType: Type = objClassType
    for (i in segments.indices) {
        val segment = segments[i]
        val expr = currentAssignments.getValue(segment)
        if (i < segments.lastIndex) {
            currentType = (currentType as ObjClassType).fields.first { it.name == segment }.type
            currentAssignments = (expr as ObjClassLiteralExprNode).fieldAssignments
        } else {
            return expr
        }
    }
    throw RuntimeException("Empty relative path in o-class literal field lookup")
}
