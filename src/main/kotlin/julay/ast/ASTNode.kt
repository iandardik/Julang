package julay.ast

import julay.program.*
import julay.program.TSAction
import julay.program.library.JulHttpServer
import julay.program.library.PrintlnTS
import julay.program.library.ReadlnTS

abstract class ASTNode(
    val children : List<ASTNode>
) {
    abstract fun programLocation() : ProgramLoc
    open fun procPass() : List<ProcDecl> = children.flatMap { it.procPass() }
    open fun errorPass(procs : Set<String>) : List<CompileError> = children.flatMap { it.errorPass(procs) }
    open fun procClassPass(procs : Set<String>) : List<ProcClassDecl> = children.flatMap { it.procClassPass(procs) }
}

abstract class DeclNode(children : List<ASTNode>) : ASTNode(children) {
    abstract fun name() : String
}

abstract class ProcClassDeclNode(children : List<ASTNode>) : ASTNode(children) {
    open fun stateVariables() : List<Variable> = listOf()
    open fun constructors() : List<ActionDecl> = listOf()
    open fun transitions() : List<ActionDecl> = listOf()
}

open class ArgsNode(
    private val args : List<ArgsNode>,
    private val loc : ProgramLoc
) : ASTNode(args) {
    override fun programLocation() = loc
    open fun actionArgs() : List<Variable> = args.flatMap { it.actionArgs() }
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
}

abstract class ExprNode(children : List<ASTNode>) : ASTNode(children) {
    abstract fun toZ3GuardString(symbolTypes : Map<String,Type>, argSymbols : Set<String>, forceString : Boolean = false) : String
    abstract fun toTransitString(symbolTypes : Map<String,Type>, argSymbols : Set<String>) : String
    abstract fun type(symbolTypes : Map<String,Type>) : Type
}

class RootNode(
    private val declNodes : List<DeclNode>,
    private val loc : ProgramLoc
) : ASTNode(declNodes) {
    override fun programLocation(): ProgramLoc = loc
    override fun errorPass(procs : Set<String>): List<CompileError> {
        return actionConsistencyErrors(procs) + overlappingDeclNamesErrors()
    }
    fun actionConsistencyErrors(procs : Set<String>) : List<CompileError> {
        val progTransitions = declNodes
            .flatMap { it.procClassPass(procs) }
            .flatMap { it.transitions }
        val libTransitions = procPass()
            .flatMap { it.allProcNames() }
            .flatMap { name ->
                // TODO make this cleaner
                when (name) {
                    "Println" -> PrintlnTS.actionDecls
                    "Readln" -> ReadlnTS.actionDecls
                    "HttpServer" -> JulHttpServer.actionDecls
                    else -> listOf()
                }
            }
        val allTransitions = progTransitions + libTransitions
        val actionOccurrences = allTransitions.groupBy { it.action.name }
        return actionOccurrences.entries.flatMap { (name, actions) ->
            val refAction = actions[0]
            val argMismatches = actions.flatMap { act ->
                assertOrCompileError(refAction.action.args == act.action.args,
                    TwoLocsCompileError(refAction.loc, act.loc, "Expected action \"$name\" to have the same arguments"))
            }
            val inconsistentSyncTypes = actions.flatMap { act ->
                assertOrCompileError(refAction.action.syncType == act.action.syncType,
                    TwoLocsCompileError(refAction.loc, act.loc, "Expected action \"$name\" to have the same modifiers"))
            }
            val p2pMissingASide = actions.let { actions ->
                val isP2P = actions.any { act -> act.action.syncType == SymbolicAction.SyncType.P2P }
                val hasService = actions.any { act -> act.modifier == TSAction.SyncRole.P2PService }
                val hasConsumer = actions.any { act -> act.modifier == TSAction.SyncRole.P2PConsumer }
                val missingType = if (hasService) "p2p-consumer" else "p2p-service"
                assertOrCompileError(!isP2P || (hasService && hasConsumer),
                    SingleLocCompileError(refAction.loc, "Expected action \"$name\" to have at least one corresponding \"$missingType\" action"))
            }
            argMismatches + inconsistentSyncTypes + p2pMissingASide
        }
    }
    fun overlappingDeclNamesErrors() : List<CompileError> {
        // an n^2 algorithm isn't super efficient, but the number of decls won't be large and we do need to detect both
        // locations where the name conflict occur so we can report it
        return declNodes.flatMap { refDecl ->
            declNodes
                .filter { decl -> refDecl != decl && refDecl.name() == decl.name() }
                .map { decl -> TwoLocsCompileError(refDecl.programLocation(), decl.programLocation(),
                    "Expected each declaration to have a unique name, but found at least two named \"${decl.name()}\"") }
        }
    }
    override fun toString(): String {
        return declNodes.joinToString("\n\n") { it.toString() }
    }
}

class ProcClassNode(
    private val name : String,
    private val localDecls : List<ProcClassDeclNode>,
    private val loc : ProgramLoc
) : DeclNode(localDecls) {
    override fun programLocation() = loc
    override fun name() = name
    override fun procClassPass(procs : Set<String>): List<ProcClassDecl> {
        // TODO make sure there is at least one constructor
        if (name !in procs) {
            return listOf()
        }
        val stateVars = localDecls.flatMap { it.stateVariables() }
        // TODO make sure constructors, transitions, and services are mutually exclusive
        val constructors = localDecls.flatMap { it.constructors() } // TODO constructors cannot have repeat names
        val transitions = localDecls.flatMap { it.transitions() }
        val decl = ProcClassDecl(name, stateVars, constructors, transitions)
        return listOf(decl)
    }
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
    override fun procPass(): List<ProcDecl> {
        return listOf(ProcDecl(name, value.procPass(), ProcDeclType.Proc))
    }
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
    override fun procPass(): List<ProcDecl> {
        return listOf(ProcDecl(name, value.procPass(), ProcDeclType.Program))
    }
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
    override fun procPass(): List<ProcDecl> {
        return listOf(ProcDecl(name, value.procPass(), ProcDeclType.Spec))
    }
    override fun toString(): String {
        return "spec $name := $value"
    }
}

class VarNode(
    val name : String,
    val type : Type,
    private val loc : ProgramLoc
) : ProcClassDeclNode(listOf()) {
    override fun programLocation() = loc
    override fun stateVariables(): List<Variable> = listOf(Variable(name, type))
    override fun toString(): String {
        return "$name : $type"
    }
}

class ConstructorNode(
    private val name : String,
    private val args : ArgsNode,
    private val body : List<ActionBodyNode>,
    private val loc : ProgramLoc
) : ProcClassDeclNode(body) {
    override fun programLocation() = loc
    override fun errorPass(procs : Set<String>): List<CompileError> {
        val errors = mutableListOf<CompileError>()
        errors.addAll(args.errorPass(procs))
        errors.addAll(body.flatMap { it.errorPass(procs) })
        // TODO ensure that each transit includes each state var exactly once
        // TODO ensure that there is no guard, since it will not be followed by the ConstructorTS
        return errors
    }
    override fun constructors(): List<ActionDecl> {
        return listOf(
            ActionDecl(
                SymbolicAction(name, args.actionArgs(), SymbolicAction.SyncType.CSP),
                body.flatMap { it.guards() },
                body.fold(emptyMap()) { acc, astNode -> acc + astNode.transits() },
                TSAction.SyncRole.CSP,
                loc
            )
        )
    }
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
    override fun errorPass(procs : Set<String>): List<CompileError> {
        val errors = mutableListOf<CompileError>()
        errors.addAll(args.errorPass(procs))
        errors.addAll(body.flatMap { it.errorPass(procs) })
        if (name == "initially") {
            errors.add(SingleLocCompileError(loc, "only constructors (not transitions) can synchronize on the 'initially' action"))
        }
        // TODO ensure that each transit has a unique state var
        // TODO ensure that a modified action (in at least one place) is always modified
        return errors
    }
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
                loc
            )
        )
    }
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
    private val type : Type,
    private val loc : ProgramLoc
) : ArgsNode(listOf(), loc) {
    override fun programLocation() = loc
    override fun actionArgs(): List<Variable> {
        return listOf(Variable(name,type))
    }
    override fun toString(): String {
        return "$name : $type"
    }
}

class GuardNode(
    val expr : ExprNode,
    private val loc : ProgramLoc
) : ActionBodyNode(listOf(), listOf(expr)) {
    override fun programLocation() = loc
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

class VarTransitNode(
    val varName : String,
    val expr : ExprNode,
    private val loc : ProgramLoc
) : ActionBodyNode(listOf(), listOf(expr)) {
    override fun programLocation() = loc
    override fun transits(): Map<String, ExprNode> {
        return mapOf(Pair(varName,expr))
    }
    override fun toString(): String {
        return "$varName := $expr"
    }
}

class UnaryOpExprNode(
    private val op : String,
    private val operand : ExprNode,
    private val loc : ProgramLoc
) : ExprNode(listOf(operand)) {
    override fun programLocation() = loc
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
    override fun type(symbolTypes: Map<String, Type>): Type {
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
    override fun toZ3GuardString(symbolTypes : Map<String,Type>, argSymbols : Set<String>, forceString : Boolean): String {
        val lhsType = lhsOperand.type(symbolTypes)
        val rhsType = rhsOperand.type(symbolTypes)
        val isStringConcat = op == "+" && (lhsType is StringType || rhsType is StringType)
        julay.tools.assert(!forceString || isStringConcat, "Cannot force a binary boolean operator to a string")

        val forceStringOperands = forceString || isStringConcat
        val lhsGuardStr = lhsOperand.toZ3GuardString(symbolTypes,argSymbols, forceStringOperands)
        val rhsGuardStr = rhsOperand.toZ3GuardString(symbolTypes,argSymbols, forceStringOperands)

        return when (op) {
            "=" -> "ctx.mkEq($lhsGuardStr,$rhsGuardStr)"
            "#" -> "ctx.mkNot(ctx.mkEq($lhsGuardStr,$rhsGuardStr))"
            "<" -> "ctx.mkLt($lhsGuardStr,$rhsGuardStr)"
            "<=" -> "ctx.mkLe($lhsGuardStr,$rhsGuardStr)"
            ">" -> "ctx.mkGt($lhsGuardStr,$rhsGuardStr)"
            ">=" -> "ctx.mkGe($lhsGuardStr,$rhsGuardStr)"
            "&" -> "ctx.mkAnd($lhsGuardStr,$rhsGuardStr)"
            "|" -> "ctx.mkOr($lhsGuardStr,$rhsGuardStr)"
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
        // for readability
        val lhs = lhsOperand.toTransitString(symbolTypes, argSymbols)
        val rhs = rhsOperand.toTransitString(symbolTypes, argSymbols)
        return "($lhs $op $rhs)"
    }
    override fun type(symbolTypes: Map<String, Type>): Type {
        return when (op) {
            "=" -> boolType
            "#" -> boolType
            "<" -> boolType
            "<=" -> boolType
            ">" -> boolType
            ">=" -> boolType
            "&" -> boolType
            "|" -> boolType
            "+" -> {
                val lhsType = lhsOperand.type(symbolTypes)
                val rhsType = rhsOperand.type(symbolTypes)
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
        // for readability
        val lhs = "$lhsOperand"
        val rhs = "$rhsOperand"
        return "($lhs $op $rhs)"
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
    override fun type(symbolTypes: Map<String, Type>) = type
    override fun toString(): String {
        return if (type is StringType) "\"$value\"" else value
    }
}

class SymbolValueExprNode(
    private val symbol : String,
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
                        "ctx.intToString(ctx.mkIntConst(\"$symbol\"))"
                    } else {
                        "ctx.mkString(${symbol}.toString())"
                    }
                }
                is StringType -> {
                    if (symbol in argSymbols) {
                        "ctx.mkStringConst(\"$symbol\")"
                    } else {
                        "ctx.mkString($symbol)"
                    }
                }
                else -> throw RuntimeException("Invalid type: $type")
            }

        }
        if (symbol in argSymbols) {
            return when (type) {
                is BoolType -> "ctx.mkBoolConst(\"$symbol\")"
                is IntType -> "ctx.mkIntConst(\"$symbol\")"
                is StringType -> "ctx.mkStringConst(\"$symbol\")"
                else -> throw RuntimeException("Invalid type: $type")
            }
        }
        return when (type) {
            is BoolType -> "ctx.mkBool($symbol)"
            is IntType -> "ctx.mkInt($symbol)"
            is StringType -> "ctx.mkString($symbol)"
            else -> throw RuntimeException("Invalid type: $type")
        }
    }
    override fun toTransitString(symbolTypes: Map<String, Type>, argSymbols: Set<String>): String {
        return if (symbol in argSymbols) {
            val type = symbolTypes[symbol]
            val typeStr = when (type) {
                is BoolType -> "boolType"
                is IntType -> "intType"
                is StringType -> "stringType"
                else -> throw RuntimeException("Invalid type: $type (symbol: $symbol)")
            }
            "act.lookup(Variable(\"$symbol\", $typeStr)).value as $type"
        } else {
            symbol
        }
    }
    override fun type(symbolTypes: Map<String, Type>): Type {
        return symbolTypes[symbol]!!
    }
    override fun toString(): String {
        return symbol
    }
}

class ValueProcExprNode(
    private val name : String,
    private val loc : ProgramLoc
) : ASTNode(listOf()) {
    override fun programLocation() = loc
    override fun procPass(): List<ProcDecl> {
        return listOf(ProcDecl(name, listOf(), ProcDeclType.Proc))
    }
    override fun toString(): String {
        return name
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
