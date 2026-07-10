package julay.compiler.ast

import julay.compiler.decl.ActionDecl
import julay.compiler.ProgramLoc
import julay.compiler.*
import julay.compiler.pass.TypePassType
import julay.program.*
import julay.program.TSAction

abstract class ASTNode(
    val children : List<ASTNode>
) {
    abstract fun programLocation() : ProgramLoc
}

abstract class DeclNode(children : List<ASTNode>) : ASTNode(children) {
    abstract fun name() : String
}

abstract class ProcClassDeclNode(children : List<ASTNode>) : ASTNode(children) {
    open fun stateVariables() : List<Variable> = listOf()
    open fun constructors() : List<ActionDecl> = listOf()
    open fun transitions() : List<ActionDecl> = listOf()
    open fun transitVars() : List<Pair<String, ProgramLoc>> = listOf()
}

open class ArgsNode(
    private val args : List<ArgsNode>,
    private val loc : ProgramLoc
) : ASTNode(args) {
    override fun programLocation() = loc
    open fun actionArgs() : List<Variable> = args.flatMap { it.actionArgs() }
    fun argsTypeMap() : Map<String, Type> = actionArgs().associate { it.name to it.type }
    override fun toString(): String {
        return children.joinToString(", ") { it.toString() }
    }
}

abstract class ActionBodyNode(
    private val body : List<ActionBodyNode>,
    exprs : List<ExprNode>
) : ASTNode(body + exprs) {
    open fun guards() : List<ExprNode> = body.flatMap { it.guards() }
    open fun transits() : Map<String,ExprNode> = body.fold(emptyMap()) { acc, astNode -> acc + astNode.transits() }
    open fun transitVars() : List<Pair<String, ProgramLoc>> = body.flatMap { it.transitVars() }
    open fun effects() : List<EffectStmtNode> = body.flatMap { it.effects() }
    open fun effectAssignVars() : List<Pair<String, ProgramLoc>> = body.flatMap { it.effectAssignVars() }
}

abstract class ExprNode(children : List<ASTNode>) : ASTNode(children) {
    private var myType : TypePassType = TypePassType.Uninferred
    internal fun setInferredType(type : TypePassType) { myType = type }
    fun getType() : Type = when (val ts = myType) {
        is TypePassType.Inferred -> ts.type
        is TypePassType.Uninferred ->
            throw RuntimeException("Type not inferred for expression at ${programLocation()}")
    }
    internal abstract fun inferType(symbolEnv : Map<String, Type>) : Type
    abstract fun toZ3GuardString(symbolTypes : Map<String,Type>, argSymbols : Set<String>, forceString : Boolean = false) : String
    abstract fun toTransitString(symbolTypes : Map<String,Type>, argSymbols : Set<String>) : String
}

class ImportNode(
    private val qualifiedName : QualifiedNameNode,
    private val loc : ProgramLoc
) : ASTNode(listOf(qualifiedName)) {
    override fun programLocation() = loc
    fun qualifiedName() = qualifiedName
    override fun toString(): String {
        return "import $qualifiedName"
    }
}

class QualifiedNameNode(
    private val parts : List<String>,
    private val loc : ProgramLoc
) : ASTNode(listOf()) {
    override fun programLocation() = loc
    fun parts() = parts
    fun modulePath() = parts.dropLast(1).joinToString(".")
    fun symbol() = parts.last()
    override fun toString(): String {
        return parts.joinToString(".")
    }
}

class RootNode(
    private val importNodes : List<ImportNode>,
    private val declNodes : List<DeclNode>,
    private val loc : ProgramLoc
) : ASTNode(importNodes + declNodes) {
    override fun programLocation(): ProgramLoc = loc
    internal fun importNodes(): List<ImportNode> = importNodes
    internal fun declNodes(): List<DeclNode> = declNodes
    fun withDeclNodes(decls: List<DeclNode>): RootNode = RootNode(importNodes, decls, programLocation())
    fun withImportsAndDecls(imports: List<ImportNode>, decls: List<DeclNode>): RootNode =
        RootNode(imports, decls, programLocation())
    override fun toString(): String {
        return (importNodes + declNodes).joinToString("\n\n") { it.toString() }
    }
}

class ProcClassNode(
    private val name : String,
    private val localDecls : List<ProcClassDeclNode>,
    private val loc : ProgramLoc
) : DeclNode(localDecls) {
    override fun programLocation() = loc
    override fun name() = name
    internal fun procClassNodeName() = name
    internal fun localDecls(): List<ProcClassDeclNode> = localDecls
    internal fun withLocalDecls(decls: List<ProcClassDeclNode>): ProcClassNode =
        ProcClassNode(name, decls, programLocation())
    override fun toString(): String {
        val body = localDecls.joinToString("\n") { "$it".prependIndent() }
        return "p-class $name {\n$body\n}"
    }
}

class ProcNode(
    private val name : String,
    private val value : ASTNode,
    private val loc : ProgramLoc
) : DeclNode(listOf(value)) {
    override fun programLocation() = loc
    override fun name() = name
    internal fun procNodeName() = name
    internal fun procNodeValue() = value
    override fun toString(): String {
        return "proc $name := $value"
    }
}

class ProgramNode(
    private val name : String,
    private val value : ASTNode,
    private val loc : ProgramLoc
) : DeclNode(listOf(value)) {
    override fun programLocation() = loc
    override fun name() = name
    internal fun programNodeName() = name
    internal fun programNodeValue() = value
    override fun toString(): String {
        return "program $name := $value"
    }
}

class SpecNode(
    private val name : String,
    private val value : ASTNode,
    private val loc : ProgramLoc
) : DeclNode(listOf(value)) {
    override fun programLocation() = loc
    override fun name() = name
    internal fun specNodeName() = name
    internal fun specNodeValue() = value
    override fun toString(): String {
        return "spec $name := $value"
    }
}

class ObjClassNode(
    private val name: String,
    val fields: List<FieldNode>,
    private val loc: ProgramLoc,
) : DeclNode(fields) {
    override fun programLocation() = loc
    override fun name() = name
    internal fun objClassNodeName() = name
    internal fun objClassFields(): List<FieldNode> = fields
    override fun toString(): String {
        val body = fields.joinToString("\n") { "$it".prependIndent() }
        return "o-class $name {\n$body\n}"
    }
}

class FieldNode(
    val fieldName: String,
    val typeName: String,
    private val loc: ProgramLoc,
) : ASTNode(listOf()) {
    override fun programLocation() = loc
    override fun toString(): String = "$fieldName : $typeName"
}

private sealed interface TypeNameResolution {
    data object Unresolved : TypeNameResolution
    data class Resolved(val type: Type) : TypeNameResolution
}

class VarNode(
    val name : String,
    val typeName : String,
    private val loc : ProgramLoc
) : ProcClassDeclNode(listOf()) {
    private var typeResolution : TypeNameResolution = TypeNameResolution.Unresolved
    val type : Type
        get() = when (val resolution = typeResolution) {
            is TypeNameResolution.Resolved -> resolution.type
            is TypeNameResolution.Unresolved ->
                throw RuntimeException("Type not resolved for state variable \"$name\"")
        }
    override fun programLocation() = loc
    internal fun resolveType(type: Type) {
        typeResolution = TypeNameResolution.Resolved(type)
    }
    override fun stateVariables(): List<Variable> = listOf(Variable(name, type))
    companion object {
        fun primitive(name: String, type: Type, loc: ProgramLoc): VarNode {
            val node = VarNode(name, primitiveTypeName(type), loc)
            node.typeResolution = TypeNameResolution.Resolved(type)
            return node
        }

        private fun primitiveTypeName(type: Type): String = when (type) {
            is BoolType -> "Boolean"
            is IntType -> "Int"
            is StringType -> "String"
            else -> throw RuntimeException("Cannot create flattened VarNode for type $type")
        }
    }
    override fun toString(): String {
        val displayType = when (val resolution = typeResolution) {
            is TypeNameResolution.Resolved -> resolution.type
            is TypeNameResolution.Unresolved -> typeName
        }
        return "$name : $displayType"
    }
}

class ConstructorNode(
    private val name : String,
    private val args : ArgsNode,
    private val body : List<ActionBodyNode>,
    private val loc : ProgramLoc
) : ProcClassDeclNode(listOf(args) + body) {
    override fun programLocation() = loc
    override fun transitVars() = body.flatMap { it.transitVars() }
    override fun constructors(): List<ActionDecl> {
        return listOf(
            ActionDecl(
                SymbolicAction(name, args.actionArgs(), SymbolicAction.SyncType.CSP),
                body.flatMap { it.guards() },
                body.fold(emptyMap()) { acc, astNode -> acc + astNode.transits() },
                TSAction.SyncRole.CSP,
                loc,
                body.flatMap { it.effects() },
            )
        )
    }
    internal fun body(): List<ActionBodyNode> = body
    internal fun constructorArgs(): ArgsNode = args
    internal fun actionArgs(): List<Variable> = args.actionArgs()
    internal fun withBody(newBody: List<ActionBodyNode>): ConstructorNode =
        ConstructorNode(name, args, newBody, programLocation())
    override fun toString(): String {
        val bodyStr = body.joinToString("\n") { "$it".prependIndent() }
        return "constructor $name($args) {\n$bodyStr\n}"
    }
}

class TransitionNode(
    private val modifier : TSAction.SyncRole,
    private val name : String,
    private val args : ArgsNode,
    private val body : List<ActionBodyNode>,
    private val loc : ProgramLoc
) : ProcClassDeclNode(listOf(args) + body) {
    override fun programLocation() = loc
    override fun transitVars() = body.flatMap { it.transitVars() }
    override fun transitions(): List<ActionDecl> {
        val syncType = when (modifier) {
            TSAction.SyncRole.CSP -> SymbolicAction.SyncType.CSP
            TSAction.SyncRole.P2PService -> SymbolicAction.SyncType.P2P
            TSAction.SyncRole.P2PConsumer -> SymbolicAction.SyncType.P2P
        }
        return listOf(
            ActionDecl(
                SymbolicAction(name, args.actionArgs(), syncType),
                body.flatMap { it.guards() },
                body.fold(emptyMap()) { acc, astNode -> acc + astNode.transits() },
                modifier,
                loc,
                body.flatMap { it.effects() },
            )
        )
    }
    internal fun transitionName() = name
    internal fun body(): List<ActionBodyNode> = body
    internal fun transitionArgs(): ArgsNode = args
    internal fun actionArgs(): List<Variable> = args.actionArgs()
    internal fun withBody(newBody: List<ActionBodyNode>): TransitionNode =
        TransitionNode(modifier, name, args, newBody, programLocation())
    override fun toString(): String {
        val modifierStr = when (modifier) {
            TSAction.SyncRole.CSP -> ""
            TSAction.SyncRole.P2PService -> "p2p-service "
            TSAction.SyncRole.P2PConsumer -> "p2p-consumer "
        }
        val bodyStr = body.joinToString("\n") { "$it".prependIndent() }
        return "${modifierStr}transition $name($args) {\n$bodyStr\n}"
    }
}

class ArgNode(
    private val name : String,
    private val typeName : String,
    private val loc : ProgramLoc
) : ArgsNode(listOf(), loc) {
    private var typeResolution : TypeNameResolution = TypeNameResolution.Unresolved
    val type : Type
        get() = when (val resolution = typeResolution) {
            is TypeNameResolution.Resolved -> resolution.type
            is TypeNameResolution.Unresolved ->
                throw RuntimeException("Type not resolved for action argument \"$name\"")
        }
    override fun programLocation() = loc
    internal fun argName() = name
    internal fun argTypeName() = typeName
    internal fun resolveArgType(type: Type) {
        typeResolution = TypeNameResolution.Resolved(type)
    }
    override fun actionArgs(): List<Variable> {
        return listOf(Variable(name, type))
    }
    override fun toString(): String {
        val displayType = when (val resolution = typeResolution) {
            is TypeNameResolution.Resolved -> resolution.type
            is TypeNameResolution.Unresolved -> typeName
        }
        return "$name : $displayType"
    }
}

class GuardNode(
    private val expr : ExprNode,
    private val loc : ProgramLoc
) : ActionBodyNode(listOf(), listOf(expr)) {
    override fun programLocation() = loc
    internal fun guardExpr() = expr
    override fun guards(): List<ExprNode> {
        return listOf(expr)
    }
    override fun toString(): String {
        val exprStr = "$expr".prependIndent()
        return "guard:\n$exprStr"
    }
}

class TransitNode(
    private val transits : List<ActionBodyNode>,
    private val loc : ProgramLoc
) : ActionBodyNode(transits, listOf()) {
    override fun programLocation() = loc
    internal fun transitBodies(): List<ActionBodyNode> = transits
    override fun toString(): String {
        return "transit:\n${transits.joinToString("\n") { "$it".prependIndent() }}"
    }
}

class ErrorNode(
    private val expr : ExprNode,
    private val loc : ProgramLoc
) : ActionBodyNode(listOf(), listOf(expr)) {
    override fun programLocation() = loc
    override fun toString(): String {
        val exprStr = "$expr".prependIndent()
        return "error:\n$exprStr"
    }
}

sealed class EffectStmtNode(children : List<ASTNode>) : ASTNode(children) {
    abstract fun callName(): String
    abstract fun callArgs(): List<ExprNode>
    open fun effectAssignVars(): List<Pair<String, ProgramLoc>> = emptyList()
}

class EffectCallNode(
    private val name : String,
    private val args : List<ExprNode>,
    private val loc : ProgramLoc
) : EffectStmtNode(args) {
    override fun programLocation() = loc
    override fun callName() = name
    override fun callArgs() = args
    override fun toString(): String {
        val argStr = args.joinToString(", ") { "$it" }
        return if (args.isEmpty()) "$name()" else "$name($argStr)"
    }
}

class EffectAssignNode(
    val varName : String,
    val fieldPath : List<String> = emptyList(),
    private val callName : String,
    private val callArgs : List<ExprNode>,
    private val loc : ProgramLoc
) : EffectStmtNode(callArgs) {
    override fun programLocation() = loc
    override fun callName() = callName
    override fun callArgs() = callArgs

    internal fun assignKey(): String =
        if (fieldPath.isEmpty()) varName else "$varName.${fieldPath.joinToString(".")}"

    override fun effectAssignVars() = listOf(Pair(assignKey(), programLocation()))

    override fun toString(): String {
        val argStr = callArgs.joinToString(", ") { "$it" }
        val callStr = if (callArgs.isEmpty()) "$callName()" else "$callName($argStr)"
        return "${assignKey()} := $callStr"
    }
}

class EffectNode(
    private val stmts : List<EffectStmtNode>,
    private val loc : ProgramLoc
) : ActionBodyNode(emptyList(), listOf()) {
    override fun programLocation() = loc
    override fun effects(): List<EffectStmtNode> = stmts
    override fun effectAssignVars(): List<Pair<String, ProgramLoc>> = stmts.flatMap { it.effectAssignVars() }
    override fun toString(): String {
        return "effect:\n${stmts.joinToString("\n") { "$it".prependIndent() }}"
    }
}

class VarTransitNode(
    val varName : String,
    val fieldPath : List<String> = emptyList(),
    val expr : ExprNode,
    private val loc : ProgramLoc
) : ActionBodyNode(listOf(), listOf(expr)) {
    override fun programLocation() = loc

    internal fun transitKey(): String =
        if (fieldPath.isEmpty()) varName else "$varName.${fieldPath.joinToString(".")}"

    internal fun transitExpr() = expr

    override fun transitVars() = listOf(Pair(transitKey(), loc))
    override fun transits(): Map<String, ExprNode> {
        return mapOf(Pair(transitKey(), expr))
    }
    override fun toString(): String {
        return "${transitKey()} := $expr"
    }
}

class UnaryOpExprNode(
    private val op : String,
    private val operand : ExprNode,
    private val loc : ProgramLoc
) : ExprNode(listOf(operand)) {
    override fun programLocation() = loc
    internal fun op(): String = op
    internal fun operand(): ExprNode = operand
    override fun toZ3GuardString(symbolTypes : Map<String,Type>, argSymbols : Set<String>, forceString : Boolean): String {
        julay.tools.assert(!forceString, "Cannot force a unary boolean operator to a string")
        return when (op) {
            "~" -> "ctx.mkNot(${operand.toZ3GuardString(symbolTypes, argSymbols)})"
            else -> throw RuntimeException("Invalid unary op: $op")
        }
    }
    override fun toTransitString(symbolTypes: Map<String, Type>, argSymbols: Set<String>): String {
        val transitStr = operand.toTransitString(symbolTypes, argSymbols)
        return "($op $transitStr)"
    }
    override fun inferType(symbolEnv: Map<String, Type>): Type {
        return when (op) {
            "~" -> boolType
            else -> throw RuntimeException("Invalid unary op: $op")
        }
    }
    override fun toString(): String {
        return "($op $operand)"
    }
}

class BinaryOpExprNode(
    private val op : String,
    private val lhsOperand : ExprNode,
    private val rhsOperand : ExprNode,
    private val loc : ProgramLoc
) : ExprNode(listOf(lhsOperand,rhsOperand)) {
    override fun programLocation() = loc
    internal fun op(): String = op
    internal fun lhsOperand(): ExprNode = lhsOperand
    internal fun rhsOperand(): ExprNode = rhsOperand
    override fun toZ3GuardString(symbolTypes : Map<String,Type>, argSymbols : Set<String>, forceString : Boolean): String {
        val lhsType = lhsOperand.getType()
        val rhsType = rhsOperand.getType()
        val isStringConcat = op == "+" && (lhsType is StringType || rhsType is StringType)
        julay.tools.assert(!forceString || isStringConcat, "Cannot force a binary boolean operator to a string")

        val forceStringOperands = forceString || isStringConcat
        val lhsGuardStr = lhsOperand.toZ3GuardString(symbolTypes, argSymbols, forceStringOperands)
        val rhsGuardStr = rhsOperand.toZ3GuardString(symbolTypes, argSymbols, forceStringOperands)

        return when (op) {
            "*" -> "ctx.mkMul($lhsGuardStr,$rhsGuardStr)"
            "/" -> "ctx.mkDiv($lhsGuardStr,$rhsGuardStr)"
            "%" -> "ctx.mkMod($lhsGuardStr,$rhsGuardStr)"
            "<" -> "ctx.mkLt($lhsGuardStr,$rhsGuardStr)"
            "<=" -> "ctx.mkLe($lhsGuardStr,$rhsGuardStr)"
            ">" -> "ctx.mkGt($lhsGuardStr,$rhsGuardStr)"
            ">=" -> "ctx.mkGe($lhsGuardStr,$rhsGuardStr)"
            "=" -> "ctx.mkEq($lhsGuardStr,$rhsGuardStr)"
            "#" -> "ctx.mkNot(ctx.mkEq($lhsGuardStr,$rhsGuardStr))"
            "&" -> "ctx.mkAnd($lhsGuardStr,$rhsGuardStr)"
            "|" -> "ctx.mkOr($lhsGuardStr,$rhsGuardStr)"
            "=>" -> "ctx.mkImplies($lhsGuardStr,$rhsGuardStr)"
            "+" -> {
                when {
                    lhsType is IntType && rhsType is IntType -> "ctx.mkAdd($lhsGuardStr,$rhsGuardStr)"
                    lhsType is StringType || rhsType is StringType -> "ctx.mkConcat($lhsGuardStr,$rhsGuardStr)"
                    else -> throw RuntimeException("Cannot add types: $lhsType and $rhsType")
                }
            }
            "-" -> "ctx.mkMinus($lhsGuardStr,$rhsGuardStr)"
            else -> throw RuntimeException("Invalid binary op: $op")
        }
    }
    override fun toTransitString(symbolTypes: Map<String, Type>, argSymbols: Set<String>): String {
        val lhs = lhsOperand.toTransitString(symbolTypes, argSymbols)
        val rhs = rhsOperand.toTransitString(symbolTypes, argSymbols)
        return when (op) {
            "=" -> "($lhs == $rhs)"
            "#" -> "($lhs != $rhs)"
            "&" -> "($lhs && $rhs)"
            "|" -> "($lhs || $rhs)"
            "=>" -> "(!($lhs) || $rhs)"
            else -> "($lhs $op $rhs)"
        }
    }
    override fun inferType(symbolEnv: Map<String, Type>): Type {
        return when (op) {
            "=" -> boolType
            "#" -> boolType
            "*" -> intType
            "/" -> intType
            "%" -> intType
            "<" -> boolType
            "<=" -> boolType
            ">" -> boolType
            ">=" -> boolType
            "&" -> boolType
            "|" -> boolType
            "=>" -> boolType
            "+" -> {
                val lhsType = lhsOperand.getType()
                val rhsType = rhsOperand.getType()
                when {
                    lhsType is IntType && rhsType is IntType -> intType
                    lhsType is StringType || rhsType is StringType -> stringType
                    else -> throw RuntimeException("Cannot add types: $lhsType and $rhsType")
                }
            }
            "-" -> intType
            else -> throw RuntimeException("Invalid binary op: $op")
        }
    }
    override fun toString(): String {
        val lhs = "$lhsOperand"
        val rhs = "$rhsOperand"
        return "($lhs $op $rhs)"
    }
}

class IfElseExprNode(
    private val condExpr : ExprNode,
    private val thenExpr : ExprNode,
    private val elseExpr : ExprNode,
    private val loc : ProgramLoc
) : ExprNode(listOf(condExpr,thenExpr,elseExpr)) {
    override fun programLocation() = loc
    internal fun condExpr(): ExprNode = condExpr
    internal fun thenExpr(): ExprNode = thenExpr
    internal fun elseExpr(): ExprNode = elseExpr
    override fun toZ3GuardString(symbolTypes : Map<String,Type>, argSymbols : Set<String>, forceString : Boolean): String {
        val condGuardStr = condExpr.toZ3GuardString(symbolTypes,argSymbols)
        val thenGuardStr = thenExpr.toZ3GuardString(symbolTypes,argSymbols)
        val elseGuardStr = elseExpr.toZ3GuardString(symbolTypes,argSymbols)
        return "ctx.mkITE<BoolSort>($condGuardStr,$thenGuardStr,$elseGuardStr) as BoolExpr"
    }
    override fun toTransitString(symbolTypes: Map<String, Type>, argSymbols: Set<String>): String {
        val condTransitStr = condExpr.toTransitString(symbolTypes,argSymbols)
        val thenTransitStr = thenExpr.toTransitString(symbolTypes,argSymbols)
        val elseTransitStr = elseExpr.toTransitString(symbolTypes,argSymbols)
        return "if ($condTransitStr) {$thenTransitStr} else {$elseTransitStr}"
    }
    override fun inferType(symbolEnv: Map<String, Type>): Type {
        return thenExpr.getType()
    }
    override fun toString(): String {
        return "if ($condExpr) {$thenExpr} else {$elseExpr}"
    }
}

sealed interface WhenLiteral {
    data class IntLit(val value: String) : WhenLiteral
    data class StringLit(val value: String) : WhenLiteral
    data class BoolLit(val value: String) : WhenLiteral
}

sealed interface WhenArm {
    data class Subject(val literal: WhenLiteral, val expr: ExprNode) : WhenArm
    data class Guard(val cond: ExprNode, val expr: ExprNode) : WhenArm
    data class Else(val expr: ExprNode) : WhenArm
}

class LetExprNode(
    private val name: String,
    private val typeName: String,
    private val letInitExpr: ExprNode,
    private val bodyExpr: ExprNode,
    private val loc: ProgramLoc,
    resolvedType: Type? = null,
) : ExprNode(listOf(letInitExpr, bodyExpr)) {
    private var letTypeResolution: Type? = resolvedType

    val resolvedLetType: Type
        get() = letTypeResolution
            ?: throw RuntimeException("Type not resolved for let binding \"$name\" at $loc")

    override fun programLocation() = loc
    internal fun letName(): String = name
    internal fun letTypeName(): String = typeName
    internal fun letInitExpr(): ExprNode = letInitExpr
    internal fun bodyExpr(): ExprNode = bodyExpr
    internal fun resolvedLetTypeOrNull(): Type? = letTypeResolution

    internal fun resolveLetType(type: Type) {
        letTypeResolution = type
    }

    override fun toZ3GuardString(
        symbolTypes: Map<String, Type>,
        argSymbols: Set<String>,
        forceString: Boolean,
    ): String {
        return substituteExpr(bodyExpr, name, letInitExpr)
            .toZ3GuardString(symbolTypes, argSymbols, forceString)
    }

    override fun toTransitString(symbolTypes: Map<String, Type>, argSymbols: Set<String>): String {
        val localBind = "${name.toKotlinIdent()}__let"
        val initStr = letInitExpr.toTransitString(symbolTypes, argSymbols)
        val localBody = substituteExpr(bodyExpr, name, SymbolValueExprNode(localBind, programLocation()))
        val bodyStr = localBody.toTransitString(
            symbolTypes + (localBind to resolvedLetType),
            argSymbols - name,
        )
        return "run { val $localBind = $initStr; $bodyStr }"
    }

    override fun inferType(symbolEnv: Map<String, Type>): Type = bodyExpr.getType()

    override fun toString(): String {
        return "let ($name : $typeName := $letInitExpr) { $bodyExpr }"
    }
}

class WhenExprNode(
    private val subjectExpr: ExprNode?,
    private val arms: List<WhenArm>,
    private val loc: ProgramLoc,
) : ExprNode(
    (subjectExpr?.let { listOf(it) } ?: emptyList()) +
        arms.flatMap { arm ->
            when (arm) {
                is WhenArm.Subject -> listOf(arm.expr)
                is WhenArm.Guard -> listOf(arm.cond, arm.expr)
                is WhenArm.Else -> listOf(arm.expr)
            }
        },
) {
    override fun programLocation() = loc
    internal fun subjectExpr(): ExprNode? = subjectExpr
    internal fun arms(): List<WhenArm> = arms

    override fun toZ3GuardString(
        symbolTypes: Map<String, Type>,
        argSymbols: Set<String>,
        forceString: Boolean,
    ): String {
        julay.tools.assert(!forceString, "Cannot force a when expression to a string")
        return buildNestedZ3ITE(symbolTypes, argSymbols)
    }

    override fun toTransitString(symbolTypes: Map<String, Type>, argSymbols: Set<String>): String {
        return buildNestedTransitIf(symbolTypes, argSymbols)
    }

    override fun inferType(symbolEnv: Map<String, Type>): Type {
        val firstNonElse = arms.firstOrNull { it !is WhenArm.Else }
            ?: throw RuntimeException("When expression at $loc has no non-else arms")
        return when (firstNonElse) {
            is WhenArm.Subject -> firstNonElse.expr.getType()
            is WhenArm.Guard -> firstNonElse.expr.getType()
            is WhenArm.Else -> firstNonElse.expr.getType()
        }
    }

    private fun buildNestedZ3ITE(symbolTypes: Map<String, Type>, argSymbols: Set<String>): String {
        val elseArm = arms.last() as WhenArm.Else
        var result = elseArm.expr.toZ3GuardString(symbolTypes, argSymbols)
        for (arm in arms.dropLast(1).reversed()) {
            val (condStr, branchStr) = when (arm) {
                is WhenArm.Subject -> subjectMatchZ3String(arm.literal, symbolTypes, argSymbols) to
                    arm.expr.toZ3GuardString(symbolTypes, argSymbols)
                is WhenArm.Guard -> {
                    arm.cond.toZ3GuardString(symbolTypes, argSymbols) to
                        arm.expr.toZ3GuardString(symbolTypes, argSymbols)
                }
                is WhenArm.Else -> throw RuntimeException("Unexpected else arm before final position")
            }
            result = "ctx.mkITE<BoolSort>($condStr,$branchStr,$result) as BoolExpr"
        }
        return result
    }

    private fun buildNestedTransitIf(symbolTypes: Map<String, Type>, argSymbols: Set<String>): String {
        val elseArm = arms.last() as WhenArm.Else
        var result = elseArm.expr.toTransitString(symbolTypes, argSymbols)
        for (arm in arms.dropLast(1).reversed()) {
            val (condStr, branchStr) = when (arm) {
                is WhenArm.Subject -> subjectMatchTransitString(arm.literal, symbolTypes, argSymbols) to
                    arm.expr.toTransitString(symbolTypes, argSymbols)
                is WhenArm.Guard -> {
                    arm.cond.toTransitString(symbolTypes, argSymbols) to
                        arm.expr.toTransitString(symbolTypes, argSymbols)
                }
                is WhenArm.Else -> throw RuntimeException("Unexpected else arm before final position")
            }
            result = "if ($condStr) {$branchStr} else {$result}"
        }
        return result
    }

    private fun subjectMatchZ3String(
        literal: WhenLiteral,
        symbolTypes: Map<String, Type>,
        argSymbols: Set<String>,
    ): String {
        val subject = subjectExpr ?: throw RuntimeException("Subject when at $loc has no subject expression")
        val literalExpr = literal.toLiteralExprNode(subject.programLocation())
        val lhsStr = subject.toZ3GuardString(symbolTypes, argSymbols)
        val rhsStr = literalExpr.toZ3GuardString(symbolTypes, argSymbols)
        return "ctx.mkEq($lhsStr,$rhsStr)"
    }

    private fun subjectMatchTransitString(
        literal: WhenLiteral,
        symbolTypes: Map<String, Type>,
        argSymbols: Set<String>,
    ): String {
        val subject = subjectExpr ?: throw RuntimeException("Subject when at $loc has no subject expression")
        val literalExpr = literal.toLiteralExprNode(subject.programLocation())
        val lhsStr = subject.toTransitString(symbolTypes, argSymbols)
        val rhsStr = literalExpr.toTransitString(symbolTypes, argSymbols)
        return "($lhsStr == $rhsStr)"
    }

    override fun toString(): String {
        return if (subjectExpr != null) {
            val armStrs = arms.joinToString("\n") { arm -> "    $arm" }
            "when ($subjectExpr) {\n$armStrs\n}"
        } else {
            val armStrs = arms.joinToString("\n") { arm -> "    $arm" }
            "when {\n$armStrs\n}"
        }
    }
}

private fun WhenLiteral.toLiteralExprNode(loc: ProgramLoc): LiteralValueExprNode = when (this) {
    is WhenLiteral.IntLit -> LiteralValueExprNode(value, intType, loc)
    is WhenLiteral.StringLit -> LiteralValueExprNode(value, stringType, loc)
    is WhenLiteral.BoolLit -> LiteralValueExprNode(value, boolType, loc)
}

class ObjClassLiteralExprNode(
    val className: String,
    val fieldEntries: List<Pair<String, ExprNode>>,
    private val loc: ProgramLoc,
) : ExprNode(fieldEntries.map { it.second }) {
    private sealed interface ObjClassLiteralResolution {
        data object Unresolved : ObjClassLiteralResolution
        data class Resolved(val structType: ObjClassType) : ObjClassLiteralResolution
    }

    private var objClassLiteralResolution: ObjClassLiteralResolution = ObjClassLiteralResolution.Unresolved

    val structType: ObjClassType
        get() = when (val resolution = objClassLiteralResolution) {
            is ObjClassLiteralResolution.Resolved -> resolution.structType
            is ObjClassLiteralResolution.Unresolved ->
                throw RuntimeException("O-class literal type not resolved at $loc")
        }

    val fieldAssignments: Map<String, ExprNode> = fieldEntries.toMap()

    override fun programLocation() = loc

    internal fun resolveLiteralType(type: ObjClassType) {
        objClassLiteralResolution = ObjClassLiteralResolution.Resolved(type)
    }

    override fun toZ3GuardString(
        symbolTypes: Map<String, Type>,
        argSymbols: Set<String>,
        forceString: Boolean,
    ): String {
        val fieldExprs = fieldEntries.map { it.second.toZ3GuardString(symbolTypes, argSymbols) }
        return structType.literalToZ3Codegen(fieldExprs)
    }

    override fun toTransitString(symbolTypes: Map<String, Type>, argSymbols: Set<String>): String {
        val fieldExprs = fieldEntries.map { it.second.toTransitString(symbolTypes, argSymbols) }
        return structType.literalToTransit(fieldExprs)
    }

    override fun inferType(symbolEnv: Map<String, Type>): Type = structType

    override fun toString(): String {
        val fields = fieldEntries.joinToString(", ") { (name, expr) -> "$name := $expr" }
        return "$className { $fields }"
    }
}

class LiteralValueExprNode(
    private val value : String,
    private val type : Type,
    private val loc : ProgramLoc
) : ExprNode(listOf()) {
    override fun programLocation() = loc
    override fun toZ3GuardString(symbolTypes : Map<String,Type>, argSymbols : Set<String>, forceString : Boolean): String {
        if (forceString) {
            return "ctx.mkString(\"$value\")"
        }
        return when (type) {
            is BoolType -> "ctx.mkBool($value)"
            is IntType -> "ctx.mkInt($value)"
            is StringType -> "ctx.mkString(\"$value\")"
            else -> throw RuntimeException("Invalid type: $type")
        }
    }
    override fun toTransitString(symbolTypes: Map<String, Type>, argSymbols: Set<String>): String {
        return if (type is StringType) {
            "\"$value\""
        } else {
            value
        }
    }
    override fun inferType(symbolEnv: Map<String, Type>) = type
    override fun toString(): String {
        return if (type is StringType) "\"$value\"" else value
    }
}

class FieldAccessExprNode(
    val baseSymbol: String,
    val fieldPath: List<String>,
    private val loc: ProgramLoc,
) : ExprNode(listOf()) {
    private sealed interface FieldAccessResolution {
        data object Unresolved : FieldAccessResolution
        data class Resolved(val leafType: Type, val relPath: String) : FieldAccessResolution
    }

    private var fieldResolution: FieldAccessResolution = FieldAccessResolution.Unresolved

    override fun programLocation() = loc

    internal fun resolveFieldAccess(leafType: Type, relPath: String) {
        fieldResolution = FieldAccessResolution.Resolved(leafType, relPath)
    }

    override fun toZ3GuardString(
        symbolTypes: Map<String, Type>,
        argSymbols: Set<String>,
        forceString: Boolean,
    ): String {
        val resolution = fieldResolution as FieldAccessResolution.Resolved
        val baseType = symbolTypes.getValue(baseSymbol) as ObjClassType
        val baseZ3 = recordZ3Expr(baseSymbol, baseType, argSymbols)
        val fieldZ3 = ObjClassType.fieldAccessZ3Codegen(baseType, baseZ3, fieldPath)
        if (forceString) {
            return when (resolution.leafType) {
                is BoolType -> throw RuntimeException("Cannot convert a Bool to a string")
                is IntType -> "ctx.intToString($fieldZ3 as IntExpr)"
                is StringType -> fieldZ3
                is ObjClassType -> throw RuntimeException("Cannot convert o-class field to string")
                else -> throw RuntimeException("Invalid field type: ${resolution.leafType}")
            }
        }
        if (resolution.leafType is ObjClassType) {
            return fieldZ3
        }
        return when (resolution.leafType) {
            is BoolType -> "$fieldZ3 as BoolExpr"
            is IntType -> "$fieldZ3 as IntExpr"
            is StringType -> fieldZ3
            else -> throw RuntimeException("Invalid field type: ${resolution.leafType}")
        }
    }

    override fun toTransitString(symbolTypes: Map<String, Type>, argSymbols: Set<String>): String {
        return ObjClassType.fieldAccessTransitString(baseSymbol, fieldPath, symbolTypes, argSymbols)
    }

    private fun recordZ3Expr(baseSymbol: String, baseType: ObjClassType, argSymbols: Set<String>): String {
        val typeVal = objClassTypeValName(baseType.name)
        return if (baseSymbol in argSymbols) {
            ObjClassType.z3ConstString(baseSymbol, typeVal)
        } else {
            ObjClassType.stateToZ3String(typeVal, baseSymbol)
        }
    }

    override fun inferType(symbolEnv: Map<String, Type>): Type {
        return when (val resolution = fieldResolution) {
            is FieldAccessResolution.Resolved -> resolution.leafType
            is FieldAccessResolution.Unresolved ->
                throw RuntimeException("Field access not resolved at $loc")
        }
    }

    override fun toString(): String = fieldPath.joinToString(".", prefix = "$baseSymbol.")
}

class SymbolValueExprNode(
    val symbol : String,
    private val loc : ProgramLoc
) : ExprNode(listOf()) {
    override fun programLocation() = loc
    override fun toZ3GuardString(symbolTypes : Map<String,Type>, argSymbols : Set<String>, forceString : Boolean): String {
        val type = symbolTypes[symbol]
        if (forceString) {
            return when (type) {
                is BoolType -> throw RuntimeException("Cannot convert a Bool to a string")
                is IntType -> {
                    if (symbol in argSymbols) {
                        "ctx.intToString(ctx.mkIntConst(\"${symbol.escapeKotlinStringLiteral()}\"))"
                    } else {
                        "ctx.mkString(${symbol.toKotlinIdent()}.toString())"
                    }
                }
                is StringType -> {
                    if (symbol in argSymbols) {
                        "ctx.mkStringConst(\"${symbol.escapeKotlinStringLiteral()}\")"
                    } else {
                        "ctx.mkString(${symbol.toKotlinIdent()})"
                    }
                }
                is ObjClassType -> throw RuntimeException("Cannot convert o-class type $type to string")
                else -> throw RuntimeException("Invalid type: $type")
            }

        }
        if (type is ObjClassType) {
            val typeVal = objClassTypeValName(type.name)
            return if (symbol in argSymbols) {
                ObjClassType.z3ConstString(symbol, typeVal)
            } else {
                ObjClassType.stateToZ3String(typeVal, symbol)
            }
        }
        if (symbol in argSymbols) {
            return when (type) {
                is BoolType -> "ctx.mkBoolConst(\"${symbol.escapeKotlinStringLiteral()}\")"
                is IntType -> "ctx.mkIntConst(\"${symbol.escapeKotlinStringLiteral()}\")"
                is StringType -> "ctx.mkStringConst(\"${symbol.escapeKotlinStringLiteral()}\")"
                else -> throw RuntimeException("Invalid type: $type")
            }
        }
        return when (type) {
            is BoolType -> "ctx.mkBool(${symbol.toKotlinIdent()})"
            is IntType -> "ctx.mkInt(${symbol.toKotlinIdent()})"
            is StringType -> "ctx.mkString(${symbol.toKotlinIdent()})"
            else -> throw RuntimeException("Invalid type: $type")
        }
    }
    override fun toTransitString(symbolTypes: Map<String, Type>, argSymbols: Set<String>): String {
        val type = symbolTypes.getValue(symbol)
        return if (symbol in argSymbols) {
            val typeStr = type.toCodegenTypeVal()
            "(act.lookup(Variable(\"${symbol.escapeKotlinStringLiteral()}\", $typeStr)).value as ${type.toKotlinTypeString()})"
        } else {
            symbol.toKotlinIdent()
        }
    }
    override fun inferType(symbolEnv: Map<String, Type>): Type {
        return symbolEnv[symbol] ?: throw RuntimeException("Found unexpected free variable $symbol at $loc")
    }
    override fun toString(): String {
        return symbol
    }
}

class ValueProcExprNode(
    private val name : String,
    private val qualifiedParts : List<String>?,
    private val loc : ProgramLoc
) : ASTNode(listOf()) {
    override fun programLocation() = loc
    internal fun valueProcName() = name
    internal fun qualifiedParts() = qualifiedParts
    internal fun isQualified() = qualifiedParts != null
    internal fun fullQualifiedName() = qualifiedParts?.joinToString(".") ?: name
    override fun toString(): String {
        return fullQualifiedName()
    }
}

class CompositeProcExprNode(
    private val compositeProcs : List<ASTNode>,
    private val loc : ProgramLoc
) : ASTNode(compositeProcs) {
    override fun programLocation() = loc
    override fun toString(): String {
        return compositeProcs.joinToString(" || ") { it.toString() }
    }
}
