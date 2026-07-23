package julay.compiler.ast

import julay.compiler.decl.ActionDecl
import julay.compiler.decl.TransitUpdate
import julay.compiler.ProgramLoc
import julay.compiler.*
import julay.compiler.pass.TypePassType
import julay.program.*
import julay.program.type.*
import julay.program.action.*

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
    open fun transits() : List<TransitUpdate> = body.flatMap { it.transits() }
    open fun transitVars() : List<Pair<String, ProgramLoc>> = body.flatMap { it.transitVars() }
    open fun effects() : List<EffectStmtNode> = body.flatMap { it.effects() }
    open fun effectAssignVars() : List<Pair<String, ProgramLoc>> = body.flatMap { it.effectAssignVars() }
    open fun errors() : List<ErrorArmNode> = body.flatMap { it.errors() }
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
    private var cachedObjClassRegistry: julay.compiler.decl.ObjClassRegistry? = null

    override fun programLocation(): ProgramLoc = loc
    internal fun importNodes(): List<ImportNode> = importNodes
    internal fun declNodes(): List<DeclNode> = declNodes
    fun withDeclNodes(decls: List<DeclNode>): RootNode = RootNode(importNodes, decls, programLocation())
    fun withImportsAndDecls(imports: List<ImportNode>, decls: List<DeclNode>): RootNode =
        RootNode(imports, decls, programLocation())
    fun cacheObjClassRegistry(registry: julay.compiler.decl.ObjClassRegistry) {
        cachedObjClassRegistry = registry
    }
    fun cachedObjClassRegistry(): julay.compiler.decl.ObjClassRegistry? = cachedObjClassRegistry
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
        return "proc $name {\n$body\n}"
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

class CompileNode(
    private val names: List<String>,
    private val loc: ProgramLoc,
) : DeclNode(listOf()) {
    override fun programLocation() = loc
    override fun name() = names.joinToString(", ")
    internal fun compileNames() = names
    override fun toString(): String = "compile ${names.joinToString(", ")}"
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

class InvariantNode(
    private val name: String,
    private val formula: ExprNode,
    private val loc: ProgramLoc,
) : DeclNode(listOf(formula)) {
    override fun programLocation() = loc
    override fun name() = name
    internal fun invariantName() = name
    internal fun invariantFormula() = formula
    override fun toString(): String = "invariant $name := $formula"
}

class AgSpecExprNode(
    /** null means `<true>` (no assumption). */
    private val assume: ASTNode?,
    private val system: ASTNode,
    /** null means `<true>` (no guarantee). */
    private val guarantee: ExprNode?,
    private val loc: ProgramLoc,
) : ASTNode(listOfNotNull(assume, system, guarantee)) {
    override fun programLocation() = loc
    internal fun assumeExpr() = assume
    internal fun systemExpr() = system
    internal fun guaranteeExpr() = guarantee
    override fun toString(): String {
        val a = assume?.toString() ?: "true"
        val g = guarantee?.toString() ?: "true"
        return "<$a> $system <$g>"
    }
}

class ParamProcExprNode(
    private val body: ASTNode,
    private val paramName: String,
    private val paramType: TypeExpr,
    private val loc: ProgramLoc,
) : ASTNode(listOf(body)) {
    override fun programLocation() = loc
    internal fun paramBody() = body
    internal fun paramName() = paramName
    internal fun paramType() = paramType
    override fun toString(): String = "$body[$paramName : $paramType]"
}

class ObjClassNode(
    private val name: String,
    val typeParams: List<String>,
    val fields: List<FieldNode>,
    private val loc: ProgramLoc,
) : DeclNode(fields) {
    override fun programLocation() = loc
    override fun name() = name
    internal fun objClassNodeName() = name
    internal fun objClassTypeParams(): List<String> = typeParams
    internal fun objClassFields(): List<FieldNode> = fields
    override fun toString(): String {
        val params = if (typeParams.isEmpty()) "" else typeParams.joinToString(", ", "<", ">")
        val body = fields.joinToString("\n") { "$it".prependIndent() }
        return "obj $name$params {\n$body\n}"
    }
}

class FieldNode(
    val fieldName: String,
    val typeExpr: TypeExpr,
    private val loc: ProgramLoc,
) : ASTNode(listOf()) {
    override fun programLocation() = loc
    @Deprecated("Use typeExpr", ReplaceWith("typeExpr.toString()"))
    val typeName: String get() = typeExpr.toString()
    override fun toString(): String = "$fieldName : $typeExpr"
}

class FunNode(
    private val name: String,
    val typeParams: List<String>,
    private val args: ArgsNode,
    private val returnTypeExpr: TypeExpr,
    private val body: ExprNode,
    private val loc: ProgramLoc,
) : DeclNode(listOf(args, body)) {
    private var returnTypeResolution: TypeNameResolution = TypeNameResolution.Unresolved

    val returnType: Type
        get() = when (val resolution = returnTypeResolution) {
            is TypeNameResolution.Resolved -> resolution.type
            is TypeNameResolution.Unresolved ->
                throw RuntimeException("Return type not resolved for function \"$name\"")
        }

    override fun programLocation() = loc
    override fun name() = name
    internal fun funTypeParams(): List<String> = typeParams
    internal fun funArgs(): ArgsNode = args
    internal fun funReturnTypeExpr(): TypeExpr = returnTypeExpr
    internal fun funReturnTypeName(): String = returnTypeExpr.toString()
    internal fun funBody(): ExprNode = body
    internal fun resolveReturnType(type: Type) {
        returnTypeResolution = TypeNameResolution.Resolved(type)
    }
    override fun toString(): String {
        val params = if (typeParams.isEmpty()) "" else typeParams.joinToString(", ", "<", ">")
        val displayReturn = when (val resolution = returnTypeResolution) {
            is TypeNameResolution.Resolved -> resolution.type
            is TypeNameResolution.Unresolved -> returnTypeExpr
        }
        return "fun $name$params($args) : $displayReturn = $body"
    }
}

class FunCallExprNode(
    private val name: String,
    private val args: List<ExprNode>,
    private val loc: ProgramLoc,
    resolved: FunNode? = null,
    private var instantiatedReturnType: Type? = null,
    private val typeArgs: List<TypeExpr> = emptyList(),
) : ExprNode(args) {
    private var resolvedFun: FunNode? = resolved
    private var resolvedBuiltin: FunBuiltin? = null
    private var specializedBody: ExprNode? = null

    override fun programLocation() = loc
    fun callName(): String = name
    fun callArgs(): List<ExprNode> = args
    fun callTypeArgs(): List<TypeExpr> = typeArgs
    internal fun resolvedFunOrNull(): FunNode? = resolvedFun
    internal fun resolvedBuiltinOrNull(): FunBuiltin? = resolvedBuiltin
    internal fun resolveFun(funNode: FunNode) {
        resolvedFun = funNode
        resolvedBuiltin = null
    }
    internal fun resolveBuiltin(builtin: FunBuiltin) {
        resolvedBuiltin = builtin
        resolvedFun = null
    }
    internal fun resolveInstantiatedReturnType(type: Type) {
        instantiatedReturnType = type
    }
    internal fun resolveSpecializedBody(body: ExprNode) {
        specializedBody = body
    }
    internal fun specializedBodyOrNull(): ExprNode? = specializedBody

    private fun inlinedBody(): ExprNode {
        specializedBody?.let { return it }
        val funNode = resolvedFun
            ?: throw RuntimeException("Function call \"$name\" not resolved at $loc")
        val params = funNode.funArgs().actionArgs()
        return params.zip(args).fold(funNode.funBody()) { acc, (param, arg) ->
            substituteExpr(acc, param.name, arg)
        }
    }

    override fun toZ3GuardString(
        symbolTypes: Map<String, Type>,
        argSymbols: Set<String>,
        forceString: Boolean,
    ): String {
        resolvedBuiltin?.let { builtin ->
            val argStrs = args.map { it.toZ3GuardString(symbolTypes, argSymbols, forceString) }
            if (builtin.name == "length" && args.isNotEmpty()) {
                return when (val argType = args[0].getType()) {
                    is ListType -> "ctx.mkSeqLengthAny(${argStrs[0]})"
                    is SetType -> {
                        val meta = "${argType.toCodegenTypeVal()}.cellMetadata(ctx)"
                        "setCellSizeExpr(ctx, ${argStrs[0]}, $meta.sizeAccessor)"
                    }
                    is MapType -> {
                        val meta = "${argType.toCodegenTypeVal()}.cellMetadata(ctx)"
                        "mapCellSizeExpr(ctx, ${argStrs[0]}, $meta.sizeAccessor)"
                    }
                    else -> builtin.z3Codegen(argStrs)
                }
            }
            return builtin.z3Codegen(argStrs)
        }
        return inlinedBody().toZ3GuardString(symbolTypes, argSymbols, forceString)
    }

    override fun toTransitString(symbolTypes: Map<String, Type>, argSymbols: Set<String>): String {
        resolvedBuiltin?.let { builtin ->
            val argStrs = args.map { it.toTransitString(symbolTypes, argSymbols) }
            return builtin.kotlinCodegen(argStrs)
        }
        return inlinedBody().toTransitString(symbolTypes, argSymbols)
    }

    override fun inferType(symbolEnv: Map<String, Type>): Type {
        instantiatedReturnType?.let { return it }
        resolvedBuiltin?.let { return it.returnType }
        val funNode = resolvedFun
            ?: throw RuntimeException("Function call \"$name\" not resolved at $loc")
        return funNode.returnType
    }

    override fun toString(): String {
        val typeStr = if (typeArgs.isEmpty()) "" else "<${typeArgs.joinToString(", ")}>"
        val argsStr = args.joinToString(", ")
        return "$name$typeStr($argsStr)"
    }
}

private sealed interface TypeNameResolution {
    data object Unresolved : TypeNameResolution
    data class Resolved(val type: Type) : TypeNameResolution
}

class VarNode(
    val name : String,
    val typeExpr : TypeExpr,
    private val loc : ProgramLoc,
    val isConst : Boolean = false,
) : ProcClassDeclNode(listOf()) {
    private var typeResolution : TypeNameResolution = TypeNameResolution.Unresolved
    val type : Type
        get() = when (val resolution = typeResolution) {
            is TypeNameResolution.Resolved -> resolution.type
            is TypeNameResolution.Unresolved ->
                throw RuntimeException("Type not resolved for state variable \"$name\"")
        }
    val typeName: String get() = typeExpr.toString()
    override fun programLocation() = loc
    internal fun resolveType(type: Type) {
        typeResolution = TypeNameResolution.Resolved(type)
    }
    override fun stateVariables(): List<Variable> = listOf(Variable(name, type))
    companion object {
        fun primitive(name: String, type: Type, loc: ProgramLoc): VarNode {
            val node = VarNode(name, TypeExpr.Simple(primitiveTypeName(type)), loc)
            node.typeResolution = TypeNameResolution.Resolved(type)
            return node
        }

        private fun primitiveTypeName(type: Type): String = when (type) {
            is BoolType -> "Boolean"
            is IntType -> "Int"
            is RealType -> "Real"
            is StringType -> "String"
            else -> throw RuntimeException("Cannot create flattened VarNode for type $type")
        }
    }
    override fun toString(): String {
        val displayType = when (val resolution = typeResolution) {
            is TypeNameResolution.Resolved -> resolution.type
            is TypeNameResolution.Unresolved -> typeExpr
        }
        val keyword = if (isConst) "const" else "var"
        return "$keyword $name : $displayType"
    }
}

class ConstructorNode(
    private val name: String,
    private val args: ArgsNode,
    private val body: List<ActionBodyNode>,
    private val loc: ProgramLoc,
    private val isSession: Boolean = false,
) : ProcClassDeclNode(listOf(args) + body) {
    override fun programLocation() = loc
    override fun transitVars() = body.flatMap { it.transitVars() }
    override fun constructors(): List<ActionDecl> {
        val actionArgs = args.actionArgs()
        val guards = body.flatMap { it.guards() }
        return listOf(
            ActionDecl(
                SymbolicAction(name, actionArgs, isSession = isSession),
                guards,
                body.flatMap { it.transits() },
                TSAction.SyncRole.Default,
                loc,
                body.flatMap { it.effects() },
                body.flatMap { it.errors() },
            )
        )
    }
    internal fun body(): List<ActionBodyNode> = body
    internal fun constructorArgs(): ArgsNode = args
    internal fun actionArgs(): List<Variable> = args.actionArgs()
    internal fun withBody(newBody: List<ActionBodyNode>): ConstructorNode =
        ConstructorNode(name, args, newBody, programLocation(), isSession)
    override fun toString(): String {
        val sessionStr = if (isSession) "session " else ""
        val bodyStr = body.joinToString("\n") { "$it".prependIndent() }
        return "${sessionStr}constructor $name($args) {\n$bodyStr\n}"
    }
}

class TransitionNode(
    private val modifier: TSAction.SyncRole,
    private val name: String,
    private val args: ArgsNode,
    private val body: List<ActionBodyNode>,
    private val loc: ProgramLoc,
    private val isSession: Boolean = false,
) : ProcClassDeclNode(listOf(args) + body) {
    override fun programLocation() = loc
    override fun transitVars() = body.flatMap { it.transitVars() }
    override fun transitions(): List<ActionDecl> {
        val actionArgs = args.actionArgs()
        val guards = body.flatMap { it.guards() }
        return listOf(
            ActionDecl(
                SymbolicAction(
                    name,
                    actionArgs,
                    isInternal = modifier == TSAction.SyncRole.Internal,
                    isSession = isSession,
                ),
                guards,
                body.flatMap { it.transits() },
                modifier,
                loc,
                body.flatMap { it.effects() },
                body.flatMap { it.errors() },
            )
        )
    }
    internal fun transitionName() = name
    internal fun body(): List<ActionBodyNode> = body
    internal fun transitionArgs(): ArgsNode = args
    internal fun actionArgs(): List<Variable> = args.actionArgs()
    internal fun withBody(newBody: List<ActionBodyNode>): TransitionNode =
        TransitionNode(modifier, name, args, newBody, programLocation(), isSession)
    override fun toString(): String {
        val modifierStr = when {
            isSession -> "session "
            modifier == TSAction.SyncRole.Service -> "service "
            modifier == TSAction.SyncRole.Internal -> "internal "
            else -> ""
        }
        val bodyStr = body.joinToString("\n") { "$it".prependIndent() }
        return "${modifierStr}transition $name($args) {\n$bodyStr\n}"
    }
}

class ArgNode(
    private val name : String,
    private val typeExpr : TypeExpr,
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
    internal fun argTypeExpr() = typeExpr
    internal fun argTypeName() = typeExpr.toString()
    internal fun resolveArgType(type: Type) {
        typeResolution = TypeNameResolution.Resolved(type)
    }
    override fun actionArgs(): List<Variable> {
        return listOf(Variable(name, type))
    }
    override fun toString(): String {
        val displayType = when (val resolution = typeResolution) {
            is TypeNameResolution.Resolved -> resolution.type
            is TypeNameResolution.Unresolved -> typeExpr
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

class ErrorArmNode(
    private val cond : ExprNode,
    private val msg : ExprNode,
    private val loc : ProgramLoc
) : ASTNode(listOf(cond, msg)) {
    override fun programLocation() = loc
    fun condExpr() = cond
    fun msgExpr() = msg
    override fun toString(): String = "$cond -> $msg"
}

class ErrorNode(
    private val arms : List<ErrorArmNode>,
    private val loc : ProgramLoc
) : ActionBodyNode(listOf(), arms.flatMap { listOf(it.condExpr(), it.msgExpr()) }) {
    override fun programLocation() = loc
    override fun errors(): List<ErrorArmNode> = arms
    override fun toString(): String {
        return "error:\n${arms.joinToString("\n") { "$it".prependIndent() }}"
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
    private val loc : ProgramLoc,
    private val typeArgs: List<TypeExpr> = emptyList(),
) : EffectStmtNode(args) {
    override fun programLocation() = loc
    override fun callName() = name
    override fun callArgs() = args
    fun callTypeArgs(): List<TypeExpr> = typeArgs
    override fun toString(): String {
        val typeStr = if (typeArgs.isEmpty()) "" else "<${typeArgs.joinToString(", ")}>"
        val argStr = args.joinToString(", ") { "$it" }
        return if (args.isEmpty()) "$name$typeStr()" else "$name$typeStr($argStr)"
    }
}

class EffectAssignNode(
    val varName : String,
    val fieldPath : List<String> = emptyList(),
    private val callName : String,
    private val callArgs : List<ExprNode>,
    private val loc : ProgramLoc,
    private val typeArgs: List<TypeExpr> = emptyList(),
) : EffectStmtNode(callArgs) {
    override fun programLocation() = loc
    override fun callName() = callName
    override fun callArgs() = callArgs
    fun callTypeArgs(): List<TypeExpr> = typeArgs

    internal fun assignKey(): String =
        if (fieldPath.isEmpty()) varName else "$varName.${fieldPath.joinToString(".")}"

    override fun effectAssignVars() = listOf(Pair(assignKey(), programLocation()))

    override fun toString(): String {
        val typeStr = if (typeArgs.isEmpty()) "" else "<${typeArgs.joinToString(", ")}>"
        val argStr = callArgs.joinToString(", ") { "$it" }
        val callStr = if (callArgs.isEmpty()) "$callName$typeStr()" else "$callName$typeStr($argStr)"
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

class MapIndexTransitNode(
    val mapVar: String,
    val key: ExprNode,
    val value: ExprNode,
    private val loc: ProgramLoc,
) : ActionBodyNode(listOf(), listOf(key, value)) {
    override fun programLocation() = loc

    override fun transitVars(): List<Pair<String, ProgramLoc>> = emptyList()

    override fun transits(): List<TransitUpdate> =
        listOf(TransitUpdate.MapPut(mapVar, key, value))

    override fun toString(): String = "$mapVar[$key] := $value"
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
    override fun transits(): List<TransitUpdate> {
        return listOf(TransitUpdate.Assign(transitKey(), expr))
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
        return when (op) {
            "~" -> "(!($transitStr))"
            else -> "($op $transitStr)"
        }
    }
    override fun inferType(symbolEnv: Map<String, Type>): Type {
        return when (op) {
            "~" -> {
                val operandType = operand.getType()
                if (operandType !is BoolType) {
                    throw RuntimeException("Cannot apply \"~\" to type $operandType")
                }
                boolType
            }
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

        fun numericZ3(mkOp: (String, String) -> String): String {
            val promoted = promoteNumeric(lhsType, rhsType)
                ?: throw RuntimeException("Cannot apply \"$op\" to types $lhsType and $rhsType")
            return if (promoted is RealType) {
                mkOp(asZ3Real(lhsGuardStr, lhsType), asZ3Real(rhsGuardStr, rhsType))
            } else {
                mkOp(lhsGuardStr, rhsGuardStr)
            }
        }

        return when (op) {
            "*" -> numericZ3 { l, r -> "ctx.mkMul($l,$r)" }
            "/" -> numericZ3 { l, r -> "ctx.mkDiv($l,$r)" }
            "%" -> {
                if (lhsType !is IntType || rhsType !is IntType) {
                    throw RuntimeException("Cannot apply \"%\" to types $lhsType and $rhsType")
                }
                "ctx.mkMod($lhsGuardStr,$rhsGuardStr)"
            }
            "<" -> numericZ3 { l, r -> "ctx.mkLt($l,$r)" }
            "<=" -> numericZ3 { l, r -> "ctx.mkLe($l,$r)" }
            ">" -> numericZ3 { l, r -> "ctx.mkGt($l,$r)" }
            ">=" -> numericZ3 { l, r -> "ctx.mkGe($l,$r)" }
            "=" -> {
                val promoted = promoteNumeric(lhsType, rhsType)
                if (promoted is RealType) {
                    "ctx.mkEq(${asZ3Real(lhsGuardStr, lhsType)},${asZ3Real(rhsGuardStr, rhsType)})"
                } else {
                    "ctx.mkEq($lhsGuardStr,$rhsGuardStr)"
                }
            }
            "#" -> {
                val promoted = promoteNumeric(lhsType, rhsType)
                if (promoted is RealType) {
                    "ctx.mkNot(ctx.mkEq(${asZ3Real(lhsGuardStr, lhsType)},${asZ3Real(rhsGuardStr, rhsType)}))"
                } else {
                    "ctx.mkNot(ctx.mkEq($lhsGuardStr,$rhsGuardStr))"
                }
            }
            "&" -> "ctx.mkAnd($lhsGuardStr,$rhsGuardStr)"
            "|" -> "ctx.mkOr($lhsGuardStr,$rhsGuardStr)"
            "=>" -> "ctx.mkImplies($lhsGuardStr,$rhsGuardStr)"
            "+" -> {
                when {
                    lhsType is IntType && rhsType is IntType -> "ctx.mkAdd($lhsGuardStr,$rhsGuardStr)"
                    promoteNumeric(lhsType, rhsType) is RealType ->
                        "ctx.mkAdd(${asZ3Real(lhsGuardStr, lhsType)},${asZ3Real(rhsGuardStr, rhsType)})"
                    lhsType is ListType && rhsType is ListType ->
                        "ctx.mkSeqConcatAny($lhsGuardStr, $rhsGuardStr)"
                    lhsType is SetType && rhsType is SetType -> {
                        val setVal = lhsType.toCodegenTypeVal()
                        val meta = "$setVal.cellMetadata(ctx)"
                        "run { val __l = $lhsGuardStr; val __r = $rhsGuardStr; " +
                            "val __la = setCellArrExpr(ctx, __l, $meta.arrAccessor); " +
                            "val __ra = setCellArrExpr(ctx, __r, $meta.arrAccessor); " +
                            "val __arr = ctx.mkSetUnionAny(__la, __ra); " +
                            "val __sz = ctx.mkAdd(setCellSizeExpr(ctx, __l, $meta.sizeAccessor), setCellSizeExpr(ctx, __r, $meta.sizeAccessor)); " +
                            "setMkCellExpr(ctx, $meta.constructorDecl, __arr, __sz) }"
                    }
                    lhsType is StringType || rhsType is StringType -> "ctx.mkConcat($lhsGuardStr,$rhsGuardStr)"
                    else -> throw RuntimeException("Cannot add types: $lhsType and $rhsType")
                }
            }
            "-" -> {
                when {
                    lhsType is SetType && rhsType is SetType -> {
                        val setVal = lhsType.toCodegenTypeVal()
                        val meta = "$setVal.cellMetadata(ctx)"
                        "run { val __l = $lhsGuardStr; val __r = $rhsGuardStr; " +
                            "val __la = setCellArrExpr(ctx, __l, $meta.arrAccessor); " +
                            "val __ra = setCellArrExpr(ctx, __r, $meta.arrAccessor); " +
                            "val __arr = ctx.mkSetDifferenceAny(__la, __ra); " +
                            "val __sz = ctx.mkSub(setCellSizeExpr(ctx, __l, $meta.sizeAccessor), setCellSizeExpr(ctx, __r, $meta.sizeAccessor)); " +
                            "setMkCellExpr(ctx, $meta.constructorDecl, __arr, __sz) }"
                    }
                    else -> numericZ3 { l, r -> "ctx.mkSub($l,$r)" }
                }
            }
            "in" -> when (rhsType) {
                is ListType -> "ctx.mkListMemberAny($lhsGuardStr, $rhsGuardStr)"
                is SetType -> "ctx.mkSetMemberAny($lhsGuardStr, setCellArrExpr(ctx, $rhsGuardStr, ${rhsType.toCodegenTypeVal()}.cellMetadata(ctx).arrAccessor))"
                is MapType -> {
                    val mapVal = rhsType.toCodegenTypeVal()
                    "ctx.mkSetMemberAny($lhsGuardStr, mapCellKeysExpr(ctx, $rhsGuardStr, $mapVal.cellMetadata(ctx).keysAccessor))"
                }
                else -> throw RuntimeException("Cannot apply \"in\" to type $rhsType")
            }
            else -> throw RuntimeException("Invalid binary op: $op")
        }
    }
    override fun toTransitString(symbolTypes: Map<String, Type>, argSymbols: Set<String>): String {
        val lhs = lhsOperand.toTransitString(symbolTypes, argSymbols)
        val rhs = rhsOperand.toTransitString(symbolTypes, argSymbols)
        val lhsType = typeForTransit(lhsOperand, symbolTypes)
        val rhsType = typeForTransit(rhsOperand, symbolTypes)

        fun numericTransit(kotlinOp: String): String {
            if (lhsType != null && rhsType != null && promoteNumeric(lhsType, rhsType) is RealType) {
                return "(${asKotlinDouble(lhs, lhsType)} $kotlinOp ${asKotlinDouble(rhs, rhsType)})"
            }
            return "($lhs $kotlinOp $rhs)"
        }

        return when (op) {
            "=" -> {
                if (lhsType != null && rhsType != null && promoteNumeric(lhsType, rhsType) is RealType) {
                    "(${asKotlinDouble(lhs, lhsType)} == ${asKotlinDouble(rhs, rhsType)})"
                } else {
                    "($lhs == $rhs)"
                }
            }
            "#" -> {
                if (lhsType != null && rhsType != null && promoteNumeric(lhsType, rhsType) is RealType) {
                    "(${asKotlinDouble(lhs, lhsType)} != ${asKotlinDouble(rhs, rhsType)})"
                } else {
                    "($lhs != $rhs)"
                }
            }
            "&" -> "($lhs && $rhs)"
            "|" -> "($lhs || $rhs)"
            "=>" -> "(!($lhs) || $rhs)"
            "+" -> {
                when {
                    lhsType is StringType || rhsType is StringType -> {
                        val lhsStr = if (lhsType is StringType) lhs else "($lhs).toString()"
                        val rhsStr = if (rhsType is StringType) rhs else "($rhs).toString()"
                        "($lhsStr + $rhsStr)"
                    }
                    lhsType != null && rhsType != null && promoteNumeric(lhsType, rhsType) is RealType ->
                        "(${asKotlinDouble(lhs, lhsType)} + ${asKotlinDouble(rhs, rhsType)})"
                    else -> "($lhs + $rhs)"
                }
            }
            "*" -> numericTransit("*")
            "/" -> numericTransit("/")
            "%" -> "($lhs % $rhs)"
            "-" -> {
                when {
                    lhsType is SetType && rhsType is SetType -> "($lhs - $rhs)"
                    else -> numericTransit("-")
                }
            }
            "in" -> "($lhs in $rhs)"
            "<" -> numericTransit("<")
            "<=" -> numericTransit("<=")
            ">" -> numericTransit(">")
            ">=" -> numericTransit(">=")
            else -> "($lhs $op $rhs)"
        }
    }
    override fun inferType(symbolEnv: Map<String, Type>): Type {
        val lhsType = lhsOperand.getType()
        val rhsType = rhsOperand.getType()
        return when (op) {
            "=" -> boolType
            "#" -> boolType
            "*" -> promoteNumeric(lhsType, rhsType)
                ?: throw RuntimeException("Cannot apply \"*\" to types $lhsType and $rhsType")
            "/" -> promoteNumeric(lhsType, rhsType)
                ?: throw RuntimeException("Cannot apply \"/\" to types $lhsType and $rhsType")
            "%" -> {
                if (lhsType !is IntType || rhsType !is IntType) {
                    throw RuntimeException("Cannot apply \"%\" to types $lhsType and $rhsType")
                }
                intType
            }
            "<", "<=", ">", ">=" -> {
                promoteNumeric(lhsType, rhsType)
                    ?: throw RuntimeException("Cannot apply \"$op\" to types $lhsType and $rhsType")
                boolType
            }
            "&", "|", "=>" -> {
                if (lhsType !is BoolType || rhsType !is BoolType) {
                    throw RuntimeException("Cannot apply \"$op\" to types $lhsType and $rhsType")
                }
                boolType
            }
            "+" -> {
                when {
                    lhsType is IntType && rhsType is IntType -> intType
                    promoteNumeric(lhsType, rhsType) is RealType -> realType
                    lhsType is ListType && rhsType is ListType && lhsType == rhsType -> lhsType
                    lhsType is SetType && rhsType is SetType && lhsType == rhsType -> lhsType
                    lhsType is StringType || rhsType is StringType -> stringType
                    else -> throw RuntimeException("Cannot add types: $lhsType and $rhsType")
                }
            }
            "-" -> {
                when {
                    lhsType is SetType && rhsType is SetType && lhsType == rhsType -> lhsType
                    else -> promoteNumeric(lhsType, rhsType)
                        ?: throw RuntimeException("Cannot apply \"-\" to types $lhsType and $rhsType")
                }
            }
            "in" -> boolType
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
    data class RealLit(val value: String) : WhenLiteral
    data class StringLit(val value: String) : WhenLiteral
    data class BoolLit(val value: String) : WhenLiteral
}

sealed interface WhenPattern {
    data class Primitive(val literal: WhenLiteral) : WhenPattern
    data class Struct(val literal: ObjClassLiteralExprNode) : WhenPattern
}

sealed interface WhenArm {
    data class Subject(val pattern: WhenPattern, val expr: ExprNode) : WhenArm
    data class Guard(val cond: ExprNode, val expr: ExprNode) : WhenArm
    data class Else(val expr: ExprNode) : WhenArm
}

class LetExprNode(
    private val name: String,
    private val typeExpr: TypeExpr,
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
    internal fun letTypeExpr(): TypeExpr = typeExpr
    internal fun letTypeName(): String = typeExpr.toString()
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
        val replacement = SymbolValueExprNode(localBind, programLocation()).also {
            it.setInferredType(TypePassType.Inferred(resolvedLetType))
        }
        val localBody = substituteExpr(bodyExpr, name, replacement)
        val bodyStr = localBody.toTransitString(
            symbolTypes + (localBind to resolvedLetType),
            argSymbols - name,
        )
        return "run { val $localBind = $initStr; $bodyStr }"
    }

    override fun inferType(symbolEnv: Map<String, Type>): Type = bodyExpr.getType()

    override fun toString(): String {
        return "let ($name : $typeExpr := $letInitExpr) { $bodyExpr }"
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
                is WhenArm.Subject -> when (val pattern = arm.pattern) {
                    is WhenPattern.Primitive -> listOf(arm.expr)
                    is WhenPattern.Struct -> listOf(pattern.literal) + listOf(arm.expr)
                }
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
                is WhenArm.Subject -> subjectMatchZ3String(arm.pattern, symbolTypes, argSymbols) to
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
                is WhenArm.Subject -> subjectMatchTransitString(arm.pattern, symbolTypes, argSymbols) to
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
        pattern: WhenPattern,
        symbolTypes: Map<String, Type>,
        argSymbols: Set<String>,
    ): String {
        val subject = subjectExpr ?: throw RuntimeException("Subject when at $loc has no subject expression")
        val lhsStr = subject.toZ3GuardString(symbolTypes, argSymbols)
        val rhsStr = when (pattern) {
            is WhenPattern.Primitive ->
                pattern.literal.toLiteralExprNode(subject.programLocation())
                    .toZ3GuardString(symbolTypes, argSymbols)
            is WhenPattern.Struct ->
                pattern.literal.toZ3GuardString(symbolTypes, argSymbols)
        }
        return "ctx.mkEq($lhsStr,$rhsStr)"
    }

    private fun subjectMatchTransitString(
        pattern: WhenPattern,
        symbolTypes: Map<String, Type>,
        argSymbols: Set<String>,
    ): String {
        val subject = subjectExpr ?: throw RuntimeException("Subject when at $loc has no subject expression")
        val lhsStr = subject.toTransitString(symbolTypes, argSymbols)
        val rhsStr = when (pattern) {
            is WhenPattern.Primitive ->
                pattern.literal.toLiteralExprNode(subject.programLocation())
                    .toTransitString(symbolTypes, argSymbols)
            is WhenPattern.Struct ->
                pattern.literal.toTransitString(symbolTypes, argSymbols)
        }
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
    is WhenLiteral.RealLit -> LiteralValueExprNode(value, realType, loc)
    is WhenLiteral.StringLit -> LiteralValueExprNode(value, stringType, loc)
    is WhenLiteral.BoolLit -> LiteralValueExprNode(value, boolType, loc)
}

class ObjClassLiteralExprNode(
    val typeExpr: TypeExpr,
    val fieldEntries: List<Pair<String, ExprNode>>,
    private val loc: ProgramLoc,
    resolvedType: ObjClassType? = null,
) : ExprNode(fieldEntries.map { it.second }) {
    private sealed interface ObjClassLiteralResolution {
        data object Unresolved : ObjClassLiteralResolution
        data class Resolved(val structType: ObjClassType) : ObjClassLiteralResolution
    }

    private var objClassLiteralResolution: ObjClassLiteralResolution =
        if (resolvedType != null) ObjClassLiteralResolution.Resolved(resolvedType)
        else ObjClassLiteralResolution.Unresolved

    val className: String get() = typeExpr.ctorName()

    val structType: ObjClassType
        get() = when (val resolution = objClassLiteralResolution) {
            is ObjClassLiteralResolution.Resolved -> resolution.structType
            is ObjClassLiteralResolution.Unresolved ->
                throw RuntimeException("O-class literal type not resolved at $loc")
        }

    val fieldAssignments: Map<String, ExprNode> = fieldEntries.toMap()

    override fun programLocation() = loc

    internal fun resolvedStructTypeOrNull(): ObjClassType? =
        (objClassLiteralResolution as? ObjClassLiteralResolution.Resolved)?.structType

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
        return "$typeExpr { $fields }"
    }
}

class ListLiteralExprNode(
    val elements: List<ExprNode>,
    private val loc: ProgramLoc,
    resolvedType: ListType? = null,
) : ExprNode(elements) {
    private var listType: ListType? = resolvedType

    override fun programLocation() = loc

    internal fun resolveListType(type: ListType) {
        listType = type
    }

    internal fun resolvedListTypeOrNull(): ListType? = listType

    override fun toZ3GuardString(
        symbolTypes: Map<String, Type>,
        argSymbols: Set<String>,
        forceString: Boolean,
    ): String {
        if (forceString) {
            return "ctx.mkString(${toTransitString(symbolTypes, argSymbols)}.toString())"
        }
        val ty = listType ?: throw RuntimeException("List literal type not resolved at $loc")
        if (elements.isEmpty()) {
            return "ctx.mkEmptySeq(${ty.toCodegenTypeVal()}.sort(ctx))"
        }
        val units = elements.map { elem ->
            "ctx.mkUnit(${elem.toZ3GuardString(symbolTypes, argSymbols)})"
        }
        return if (units.size == 1) {
            units[0]
        } else {
            "ctx.mkConcat(${units.joinToString(", ")})"
        }
    }

    override fun toTransitString(symbolTypes: Map<String, Type>, argSymbols: Set<String>): String {
        if (elements.isEmpty()) {
            return "emptyList()"
        }
        val elems = elements.joinToString(", ") { it.toTransitString(symbolTypes, argSymbols) }
        return "listOf($elems)"
    }

    override fun inferType(symbolEnv: Map<String, Type>): Type {
        return listType ?: throw RuntimeException("List literal type not resolved at $loc")
    }

    override fun toString(): String = elements.joinToString(", ", prefix = "[", postfix = "]")
}

class EmptyBracketLiteralExprNode(
    private val loc: ProgramLoc,
) : ExprNode(emptyList()) {
    private var listType: ListType? = null
    private var mapType: MapType? = null

    override fun programLocation() = loc

    internal fun resolveListType(type: ListType) {
        listType = type
        mapType = null
    }

    internal fun resolveMapType(type: MapType) {
        mapType = type
        listType = null
    }

    internal fun resolvedListTypeOrNull(): ListType? = listType
    internal fun resolvedMapTypeOrNull(): MapType? = mapType

    override fun toZ3GuardString(
        symbolTypes: Map<String, Type>,
        argSymbols: Set<String>,
        forceString: Boolean,
    ): String {
        if (forceString) {
            return "ctx.mkString(${toTransitString(symbolTypes, argSymbols)}.toString())"
        }
        mapType?.let { ty ->
            return MapLiteralExprNode(emptyList(), loc, ty).toZ3GuardString(symbolTypes, argSymbols, forceString)
        }
        listType?.let { ty ->
            return ListLiteralExprNode(emptyList(), loc, ty).toZ3GuardString(symbolTypes, argSymbols, forceString)
        }
        throw RuntimeException("Empty bracket literal type not resolved at $loc")
    }

    override fun toTransitString(symbolTypes: Map<String, Type>, argSymbols: Set<String>): String {
        return when {
            mapType != null -> "emptyMap()"
            else -> "emptyList()"
        }
    }

    override fun inferType(symbolEnv: Map<String, Type>): Type {
        return mapType ?: listType ?: throw RuntimeException("Empty bracket literal type not resolved at $loc")
    }

    override fun toString(): String = "[]"
}

class MapLiteralExprNode(
    val entries: List<Pair<ExprNode, ExprNode>>,
    private val loc: ProgramLoc,
    resolvedType: MapType? = null,
) : ExprNode(entries.flatMap { listOf(it.first, it.second) }) {
    private var mapType: MapType? = resolvedType

    override fun programLocation() = loc

    internal fun resolveMapType(type: MapType) {
        mapType = type
    }

    internal fun resolvedMapTypeOrNull(): MapType? = mapType

    override fun toZ3GuardString(
        symbolTypes: Map<String, Type>,
        argSymbols: Set<String>,
        forceString: Boolean,
    ): String {
        if (forceString) {
            return "ctx.mkString(${toTransitString(symbolTypes, argSymbols)}.toString())"
        }
        val ty = mapType ?: throw RuntimeException("Map literal type not resolved at $loc")
        val mapVal = ty.toCodegenTypeVal()
        val meta = "$mapVal.cellMetadata(ctx)"
        val domain = "${mapVal}.cellMetadata(ctx).domainSort"
        if (entries.isEmpty()) {
            return "mapMkCellExpr(ctx, $meta.constructorDecl, ctx.mkConstArray($domain, ctx.mkInt(0)), ctx.mkEmptySet($domain), ctx.mkInt(0))"
        }
        var arr = "ctx.mkConstArray($domain, ctx.mkInt(0))"
        var keys = "ctx.mkEmptySet($domain)"
        var size = "ctx.mkInt(0)"
        for ((k, v) in entries) {
            val keyStr = k.toZ3GuardString(symbolTypes, argSymbols)
            val valStr = v.toZ3GuardString(symbolTypes, argSymbols)
            val wasMember = "ctx.mkSetMemberAny($keyStr, $keys)"
            arr = "mapStoreExpr(ctx, $arr, $keyStr, $valStr)"
            keys = "mapSetAddExpr(ctx, $keys, $keyStr)"
            size = "ctx.mkITE($wasMember, $size, ctx.mkAdd($size, ctx.mkInt(1)))"
        }
        return "mapMkCellExpr(ctx, $meta.constructorDecl, $arr, $keys, $size)"
    }

    override fun toTransitString(symbolTypes: Map<String, Type>, argSymbols: Set<String>): String {
        if (entries.isEmpty()) {
            return "emptyMap()"
        }
        val pairs = entries.joinToString(", ") { (k, v) ->
            "${k.toTransitString(symbolTypes, argSymbols)} to ${v.toTransitString(symbolTypes, argSymbols)}"
        }
        return "mapOf($pairs)"
    }

    override fun inferType(symbolEnv: Map<String, Type>): Type {
        return mapType ?: throw RuntimeException("Map literal type not resolved at $loc")
    }

    override fun toString(): String =
        entries.joinToString(", ", prefix = "[", postfix = "]") { (k, v) -> "$k -> $v" }
}

class SetLiteralExprNode(
    val elements: List<ExprNode>,
    private val loc: ProgramLoc,
    resolvedType: SetType? = null,
) : ExprNode(elements) {
    private var setType: SetType? = resolvedType

    override fun programLocation() = loc

    internal fun resolveSetType(type: SetType) {
        setType = type
    }

    internal fun resolvedSetTypeOrNull(): SetType? = setType

    override fun toZ3GuardString(
        symbolTypes: Map<String, Type>,
        argSymbols: Set<String>,
        forceString: Boolean,
    ): String {
        if (forceString) {
            return "ctx.mkString(${toTransitString(symbolTypes, argSymbols)}.toString())"
        }
        val ty = setType ?: throw RuntimeException("Set literal type not resolved at $loc")
        val setVal = ty.toCodegenTypeVal()
        val meta = "$setVal.cellMetadata(ctx)"
        val domain = "$setVal.cellMetadata(ctx).domainSort"
        if (elements.isEmpty()) {
            return "setMkCellExpr(ctx, $meta.constructorDecl, ctx.mkEmptySet($domain), ctx.mkInt(0))"
        }
        var arr = "ctx.mkEmptySet($domain)"
        for (elem in elements) {
            val elemStr = elem.toZ3GuardString(symbolTypes, argSymbols)
            arr = "ctx.mkSetAddAny($arr, $elemStr)"
        }
        return "setMkCellExpr(ctx, $meta.constructorDecl, $arr, ctx.mkInt(${elements.size}))"
    }

    override fun toTransitString(symbolTypes: Map<String, Type>, argSymbols: Set<String>): String {
        if (elements.isEmpty()) {
            return "emptySet()"
        }
        val elems = elements.joinToString(", ") { it.toTransitString(symbolTypes, argSymbols) }
        return "setOf($elems)"
    }

    override fun inferType(symbolEnv: Map<String, Type>): Type {
        return setType ?: throw RuntimeException("Set literal type not resolved at $loc")
    }

    override fun toString(): String = elements.joinToString(", ", prefix = "{", postfix = "}")
}

class IndexExprNode(
    val base: ExprNode,
    val index: ExprNode,
    private val loc: ProgramLoc,
) : ExprNode(listOf(base, index)) {
    override fun programLocation() = loc

    override fun toZ3GuardString(
        symbolTypes: Map<String, Type>,
        argSymbols: Set<String>,
        forceString: Boolean,
    ): String {
        val baseStr = base.toZ3GuardString(symbolTypes, argSymbols)
        val indexStr = index.toZ3GuardString(symbolTypes, argSymbols)
        return when (val baseType = base.getType()) {
            is ListType -> "ctx.mkSeqNthAny($baseStr, $indexStr)"
            is MapType -> {
                val mapVal = baseType.toCodegenTypeVal()
                val meta = "$mapVal.cellMetadata(ctx)"
                "run { val __cell = $baseStr; val __keys = mapCellKeysExpr(ctx, __cell, $meta.keysAccessor); " +
                    "val __arr = mapCellArrExpr(ctx, __cell, $meta.arrAccessor); " +
                    "ctx.mkITE(ctx.mkSetMemberAny($indexStr, __keys), mapSelectExpr(ctx, __arr, $indexStr), " +
                    "${baseType.valueType.toCodegenTypeVal()}.toZ3Expr(Value(0, ${baseType.valueType.toCodegenTypeVal()}), ctx)) }"
            }
            else -> throw RuntimeException("Cannot index type $baseType at $loc")
        }
    }

    override fun toTransitString(symbolTypes: Map<String, Type>, argSymbols: Set<String>): String {
        val baseStr = base.toTransitString(symbolTypes, argSymbols)
        val indexStr = index.toTransitString(symbolTypes, argSymbols)
        return when (typeForTransit(base, symbolTypes)) {
            is MapType -> "($baseStr.getValue($indexStr))"
            else -> "$baseStr[$indexStr]"
        }
    }

    override fun inferType(symbolEnv: Map<String, Type>): Type {
        return when (val baseType = base.getType()) {
            is ListType -> baseType.elementType
            is MapType -> baseType.valueType
            else -> throw RuntimeException("Cannot index type $baseType at $loc")
        }
    }

    override fun toString(): String = "$base[$index]"
}

class SliceExprNode(
    val base: ExprNode,
    val start: ExprNode,
    val end: ExprNode,
    private val loc: ProgramLoc,
) : ExprNode(listOf(base, start, end)) {
    override fun programLocation() = loc

    override fun toZ3GuardString(
        symbolTypes: Map<String, Type>,
        argSymbols: Set<String>,
        forceString: Boolean,
    ): String {
        if (forceString) {
            return "ctx.mkString(${toTransitString(symbolTypes, argSymbols)}.toString())"
        }
        val baseType = base.getType()
        if (baseType !is ListType) {
            throw RuntimeException("Cannot slice non-list type $baseType at $loc")
        }
        val baseStr = base.toZ3GuardString(symbolTypes, argSymbols)
        val startStr = start.toZ3GuardString(symbolTypes, argSymbols)
        val endStr = end.toZ3GuardString(symbolTypes, argSymbols)
        val empty = "ctx.mkEmptySeq(${baseType.toCodegenTypeVal()}.sort(ctx))"
        val extract = "ctx.mkSeqExtractAny($baseStr, $startStr, ctx.mkSub($endStr, $startStr))"
        return "ctx.mkITE(ctx.mkGt($endStr, $startStr), $extract, $empty)"
    }

    override fun toTransitString(symbolTypes: Map<String, Type>, argSymbols: Set<String>): String {
        val baseStr = base.toTransitString(symbolTypes, argSymbols)
        val startStr = start.toTransitString(symbolTypes, argSymbols)
        val endStr = end.toTransitString(symbolTypes, argSymbols)
        return "run { val __xs = $baseStr; val __s = $startStr; val __e = $endStr; " +
            "require(__s >= 0 && __e >= 0) { \"slice bounds must be non-negative\" }; " +
            "val __lo = minOf(__s, __xs.size); val __hi = minOf(__e, __xs.size); " +
            "if (__lo >= __hi) emptyList() else __xs.subList(__lo, __hi).toList() }"
    }

    override fun inferType(symbolEnv: Map<String, Type>): Type {
        val baseType = base.getType()
        if (baseType !is ListType) {
            throw RuntimeException("Cannot slice non-list type $baseType at $loc")
        }
        return baseType
    }

    override fun toString(): String = "$base[$start:$end]"
}

class LiteralValueExprNode(
    private val value : String,
    private val type : Type,
    private val loc : ProgramLoc
) : ExprNode(listOf()) {
    init {
        // Literals know their type at construction; keep it available for transit codegen
        // even when typeForTransit falls back (e.g. after let-subst recreates unbound nodes).
        setInferredType(TypePassType.Inferred(type))
    }
    override fun programLocation() = loc
    internal fun literalText() = value
    internal fun isTrueLiteral() = type is BoolType && value == "true"
    override fun toZ3GuardString(symbolTypes : Map<String,Type>, argSymbols : Set<String>, forceString : Boolean): String {
        if (forceString) {
            return "ctx.mkString(\"$value\")"
        }
        return when (type) {
            is BoolType -> "ctx.mkBool($value)"
            is IntType -> "ctx.mkInt($value)"
            is RealType -> "ctx.mkReal(\"$value\")"
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
    resolvedLeafType: Type? = null,
    resolvedRelPath: String? = null,
) : ExprNode(listOf()) {
    private sealed interface FieldAccessResolution {
        data object Unresolved : FieldAccessResolution
        data class Resolved(val leafType: Type, val relPath: String) : FieldAccessResolution
    }

    private var fieldResolution: FieldAccessResolution =
        if (resolvedLeafType != null && resolvedRelPath != null) {
            FieldAccessResolution.Resolved(resolvedLeafType, resolvedRelPath)
        } else {
            FieldAccessResolution.Unresolved
        }

    init {
        if (resolvedLeafType != null) {
            setInferredType(TypePassType.Inferred(resolvedLeafType))
        }
    }

    override fun programLocation() = loc

    internal fun resolveFieldAccess(leafType: Type, relPath: String) {
        fieldResolution = FieldAccessResolution.Resolved(leafType, relPath)
        setInferredType(TypePassType.Inferred(leafType))
    }

    internal fun resolvedLeafTypeOrNull(): Type? =
        (fieldResolution as? FieldAccessResolution.Resolved)?.leafType

    internal fun resolvedRelPathOrNull(): String? =
        (fieldResolution as? FieldAccessResolution.Resolved)?.relPath

    internal fun withBaseSymbol(newBase: String): FieldAccessExprNode {
        val resolved = fieldResolution as? FieldAccessResolution.Resolved
        return FieldAccessExprNode(
            newBase,
            fieldPath,
            loc,
            resolved?.leafType,
            resolved?.relPath,
        )
    }

    override fun toZ3GuardString(
        symbolTypes: Map<String, Type>,
        argSymbols: Set<String>,
        forceString: Boolean,
    ): String {
        val resolution = fieldResolution as FieldAccessResolution.Resolved
        if (forceString && (resolution.leafType is ListType || resolution.leafType is ObjClassType)) {
            if (baseSymbol in argSymbols) {
                throw RuntimeException("Cannot convert symbolic ${resolution.leafType} to string")
            }
            return "ctx.mkString((${toTransitString(symbolTypes, argSymbols)}).toString())"
        }
        val baseType = symbolTypes.getValue(baseSymbol) as ObjClassType
        val baseZ3 = recordZ3Expr(baseSymbol, baseType, argSymbols)
        val fieldZ3 = ObjClassType.fieldAccessZ3Codegen(baseType, baseZ3, fieldPath)
        return castFieldZ3(fieldZ3, resolution.leafType, forceString)
    }

    override fun toTransitString(symbolTypes: Map<String, Type>, argSymbols: Set<String>): String {
        return ObjClassType.fieldAccessTransitString(baseSymbol, fieldPath, symbolTypes, argSymbols)
    }

    private fun recordZ3Expr(baseSymbol: String, baseType: ObjClassType, argSymbols: Set<String>): String {
        val typeVal = objClassTypeValName(baseType.name)
        return if (baseSymbol in argSymbols) {
            ObjClassType.z3ConstString(baseSymbol, typeVal)
        } else {
            ObjClassType.kotlinObjClassToZ3String(baseType.name, baseSymbol)
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

/** Postfix `.field` on an arbitrary expression (e.g. `xs[i].f` or `Pclass[i].sv`). */
class MemberAccessExprNode(
    val baseExpr: ExprNode,
    val fieldName: String,
    private val loc: ProgramLoc,
) : ExprNode(listOf(baseExpr)) {
    override fun programLocation() = loc

    override fun toZ3GuardString(
        symbolTypes: Map<String, Type>,
        argSymbols: Set<String>,
        forceString: Boolean,
    ): String {
        val leafType = getType()
        if (forceString && (leafType is ListType || leafType is ObjClassType)) {
            return "ctx.mkString((${toTransitString(symbolTypes, argSymbols)}).toString())"
        }
        val baseType = baseExpr.getType() as ObjClassType
        val baseZ3 = baseExpr.toZ3GuardString(symbolTypes, argSymbols)
        val fieldZ3 = ObjClassType.fieldAccessZ3Codegen(baseType, baseZ3, listOf(fieldName))
        return castFieldZ3(fieldZ3, leafType, forceString)
    }

    override fun toTransitString(symbolTypes: Map<String, Type>, argSymbols: Set<String>): String {
        val base = baseExpr.toTransitString(symbolTypes, argSymbols)
        return "($base).$fieldName"
    }

    override fun inferType(symbolEnv: Map<String, Type>): Type {
        return try {
            getType()
        } catch (_: RuntimeException) {
            throw RuntimeException("Member access not typed at $loc")
        }
    }

    override fun toString(): String = "$baseExpr.$fieldName"
}

class FieldAccessOnExprNode(
    val baseExpr: ExprNode,
    val fieldPath: List<String>,
    private val loc: ProgramLoc,
    val leafType: Type,
) : ExprNode(listOf(baseExpr)) {
    init {
        setInferredType(TypePassType.Inferred(leafType))
    }

    override fun programLocation() = loc

    override fun toZ3GuardString(
        symbolTypes: Map<String, Type>,
        argSymbols: Set<String>,
        forceString: Boolean,
    ): String {
        if (forceString && (leafType is ListType || leafType is ObjClassType)) {
            return "ctx.mkString((${toTransitString(symbolTypes, argSymbols)}).toString())"
        }
        val baseType = baseExpr.getType() as ObjClassType
        val baseZ3 = baseExpr.toZ3GuardString(symbolTypes, argSymbols)
        val fieldZ3 = ObjClassType.fieldAccessZ3Codegen(baseType, baseZ3, fieldPath)
        return castFieldZ3(fieldZ3, leafType, forceString)
    }

    override fun toTransitString(symbolTypes: Map<String, Type>, argSymbols: Set<String>): String {
        val base = baseExpr.toTransitString(symbolTypes, argSymbols)
        return "($base).${fieldPath.joinToString(".")}"
    }

    override fun inferType(symbolEnv: Map<String, Type>): Type = leafType

    override fun toString(): String = fieldPath.joinToString(".", prefix = "($baseExpr).")
}

private fun typeForTransit(expr: ExprNode, symbolTypes: Map<String, Type>): Type? {
    try {
        return expr.getType()
    } catch (_: RuntimeException) {
        // Substituted let bindings may lack inferred types; fall back to the symbol env
        // (symbols) or the literal's inherent type (Int/String/Bool/... literals).
    }
    return when (expr) {
        is SymbolValueExprNode -> symbolTypes[expr.symbol]
        is LiteralValueExprNode -> expr.inferType(emptyMap())
        else -> null
    }
}

private fun Type.isNumeric(): Boolean = this is IntType || this is RealType

private fun promoteNumeric(lhs: Type, rhs: Type): Type? = when {
    lhs is IntType && rhs is IntType -> intType
    lhs.isNumeric() && rhs.isNumeric() -> realType
    else -> null
}

private fun asZ3Real(guardStr: String, type: Type): String =
    if (type is IntType) "ctx.mkInt2Real($guardStr)" else guardStr

private fun asKotlinDouble(exprStr: String, type: Type): String =
    if (type is IntType) "($exprStr).toDouble()" else exprStr

private fun castFieldZ3(fieldZ3: String, leafType: Type, forceString: Boolean): String {
    if (forceString) {
        return when (leafType) {
            is BoolType -> throw RuntimeException("Cannot convert a Bool to a string")
            is IntType -> "ctx.intToString($fieldZ3 as IntExpr)"
            is RealType -> throw RuntimeException("Cannot convert a symbolic Real to a string")
            is StringType -> "$fieldZ3 as Expr<SeqSort<CharSort>>"
            is ObjClassType, is ListType ->
                throw RuntimeException("Cannot convert symbolic $leafType field to string")
            else -> throw RuntimeException("Invalid field type: $leafType")
        }
    }
    if (leafType is ObjClassType) {
        return fieldZ3
    }
    return when (leafType) {
        is BoolType -> "$fieldZ3 as BoolExpr"
        is IntType -> "$fieldZ3 as IntExpr"
        is RealType -> "$fieldZ3 as RealExpr"
        is StringType -> "$fieldZ3 as Expr<SeqSort<CharSort>>"
        is ListType -> fieldZ3
        else -> throw RuntimeException("Invalid field type: $leafType")
    }
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
                is RealType -> {
                    if (symbol in argSymbols) {
                        throw RuntimeException("Cannot convert a symbolic Real to a string")
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
                is ObjClassType, is ListType, is SetType, is MapType -> {
                    if (symbol in argSymbols) {
                        throw RuntimeException("Cannot convert symbolic $type to string")
                    } else {
                        "ctx.mkString(${symbol.toKotlinIdent()}.toString())"
                    }
                }
                else -> throw RuntimeException("Invalid type: $type")
            }

        }
        if (type is ObjClassType) {
            val typeVal = objClassTypeValName(type.name)
            return if (symbol in argSymbols) {
                ObjClassType.z3ConstString(symbol, typeVal)
            } else {
                ObjClassType.kotlinObjClassToZ3String(type.name, symbol)
            }
        }
        if (type is ListType) {
            val typeVal = type.toCodegenTypeVal()
            return if (symbol in argSymbols) {
                "ctx.mkConst(\"${symbol.escapeKotlinStringLiteral()}\", $typeVal.sort(ctx))"
            } else {
                "$typeVal.toZ3Expr(Value(${symbol.toKotlinIdent()}, $typeVal), ctx)"
            }
        }
        if (type is SetType) {
            val typeVal = type.toCodegenTypeVal()
            return if (symbol in argSymbols) {
                "ctx.mkConst(\"${symbol.escapeKotlinStringLiteral()}\", $typeVal.cellMetadata(ctx).sort)"
            } else {
                "$typeVal.toZ3Expr(Value(${symbol.toKotlinIdent()}, $typeVal), ctx)"
            }
        }
        if (type is MapType) {
            val typeVal = type.toCodegenTypeVal()
            return if (symbol in argSymbols) {
                "ctx.mkConst(\"${symbol.escapeKotlinStringLiteral()}\", $typeVal.cellMetadata(ctx).sort)"
            } else {
                "$typeVal.toZ3Expr(Value(${symbol.toKotlinIdent()}, $typeVal), ctx)"
            }
        }
        if (symbol in argSymbols) {
            return when (type) {
                is BoolType -> "ctx.mkBoolConst(\"${symbol.escapeKotlinStringLiteral()}\")"
                is IntType -> "ctx.mkIntConst(\"${symbol.escapeKotlinStringLiteral()}\")"
                is RealType -> "ctx.mkRealConst(\"${symbol.escapeKotlinStringLiteral()}\")"
                is StringType -> "ctx.mkStringConst(\"${symbol.escapeKotlinStringLiteral()}\")"
                else -> throw RuntimeException("Invalid type: $type")
            }
        }
        return when (type) {
            is BoolType -> "ctx.mkBool(${symbol.toKotlinIdent()})"
            is IntType -> "ctx.mkInt(${symbol.toKotlinIdent()})"
            is RealType -> "ctx.mkReal(${symbol.toKotlinIdent()}.toString())"
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
    internal fun compositeProcChildren() = compositeProcs
    override fun toString(): String {
        return compositeProcs.joinToString(" || ") { it.toString() }
    }
}

/**
 * First-order quantifier for invariant formulas: `all x : T, body` / `exists x : T, body`.
 */
class QuantifiedExprNode(
    private val universal: Boolean,
    private val binder: String,
    private val binderType: TypeExpr,
    private val body: ExprNode,
    private val loc: ProgramLoc,
) : ExprNode(listOf(body)) {
    override fun programLocation() = loc
    internal fun isUniversal() = universal
    internal fun binderName() = binder
    internal fun binderTypeExpr() = binderType
    internal fun quantifiedBody() = body

    override fun toZ3GuardString(
        symbolTypes: Map<String, Type>,
        argSymbols: Set<String>,
        forceString: Boolean,
    ): String {
        throw RuntimeException("Quantified formulas are not allowed in guards at $loc")
    }

    override fun toTransitString(symbolTypes: Map<String, Type>, argSymbols: Set<String>): String {
        throw RuntimeException("Quantified formulas are not allowed in transits at $loc")
    }

    override fun inferType(symbolEnv: Map<String, Type>): Type = boolType

    override fun toString(): String {
        val q = if (universal) "all" else "exists"
        return "$q $binder : $binderType, $body"
    }
}

