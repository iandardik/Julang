package exspecs.ast

import exspecs.program.*

abstract class ASTNode(
    val children : List<ASTNode>
) {
    open fun errorPass() : List<CompileError> = children.flatMap { it.errorPass() }
    open fun procPass() : List<ProcDecl> = children.flatMap { it.procPass() }
    open fun procClassPass(pclassName : String) : List<ProcClassDecl> = children.flatMap { it.procClassPass(pclassName) }
    open fun constructors() : List<ActionDecl> = children.flatMap { it.constructors() }
    open fun transitions() : List<ActionDecl> = children.flatMap { it.transitions() }
    open fun actionArgs() : List<Variable> = children.flatMap { it.actionArgs() }
    open fun guards() : List<ASTNode> = children.flatMap { it.guards() }
    open fun transits() : Map<String,ASTNode> = children.fold(emptyMap()) { acc, astNode -> acc + astNode.transits() }
    open fun toZ3GuardString(symbolTypes : Map<String,Type>, argSymbols : Set<String>, forceString : Boolean = false) : String {
        throw RuntimeException("Unsupported")
    }
    open fun toTransitString(symbolTypes : Map<String,Type>, argSymbols : Set<String>) : String {
        throw RuntimeException("Unsupported")
    }
    open fun type(symbolTypes : Map<String,Type>) : Type {
        throw RuntimeException("type() is unsupported for: ${this.javaClass}")
    }
}

class RootNode(
    private val declNodes : List<ASTNode>
) : ASTNode(declNodes) {
    override fun toString(): String {
        return declNodes.joinToString("\n\n") { it.toString() }
    }
}

class DeclNode(
    private val declNode : ASTNode
) : ASTNode(listOf(declNode)) {
    override fun errorPass(): List<CompileError> {
        // TODO make sure the name of each decl is unique
        return declNode.errorPass()
    }
    override fun toString(): String {
        return declNode.toString()
    }
}

class ProcClassNode(
    private val name : String,
    private val localDecls : List<ASTNode>
) : ASTNode(localDecls) {
    override fun procClassPass(pclassName: String): List<ProcClassDecl> {
        if (pclassName != name) {
            return listOf()
        }
        val stateVars = localDecls.flatMap { it.children }
            .filterIsInstance<VarNode>().map { Variable(it.name,it.type) }
        val constructors = localDecls.flatMap { it.constructors() }
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
    private val value : ASTNode
) : ASTNode(listOf(value)) {
    override fun procPass(): List<ProcDecl> {
        return listOf(ProcDecl(name, value.procPass(), ProcDeclType.Proc))
    }
    override fun toString(): String {
        return "proc $name := $value"
    }
}

class ProgramNode(
    private val name : String,
    private val value : ASTNode
) : ASTNode(listOf(value)) {
    override fun procPass(): List<ProcDecl> {
        return listOf(ProcDecl(name, value.procPass(), ProcDeclType.Program))
    }
    override fun toString(): String {
        return "program $name := $value"
    }
}

class SpecNode(
    private val name : String,
    private val value : ASTNode
) : ASTNode(listOf(value)) {
    override fun procPass(): List<ProcDecl> {
        return listOf(ProcDecl(name, value.procPass(), ProcDeclType.Spec))
    }
    override fun toString(): String {
        return "spec $name := $value"
    }
}

class ProcClassBodyNode(
    private val body : ASTNode
) : ASTNode(listOf(body)) {
    override fun toString(): String {
        return body.toString()
    }
}

class VarNode(
    val name : String,
    val type : Type
) : ASTNode(listOf()) {
    override fun toString(): String {
        return "$name : $type"
    }
}

class ConstructorNode(
    private val name : String,
    private val args : ASTNode,
    private val body : List<ASTNode>
) : ASTNode(body) {
    override fun errorPass(): List<CompileError> {
        val errors = mutableListOf<CompileError>()
        errors.addAll(args.errorPass())
        errors.addAll(body.flatMap { it.errorPass() })
        // TODO ensure that each transit includes each state var exactly once
        // TODO ensure that there is no guard, since it will not be followed by the ConstructorTS
        return errors
    }
    override fun constructors(): List<ActionDecl> {
        return listOf(
            ActionDecl(
                ActionSignature(name,args.actionArgs()),
                super.guards(),
                super.transits()
            )
        )
    }
    override fun toString(): String {
        val bodyStr = body.joinToString("\n") { "$it".prependIndent() }
        return "constructor $name($args) {\n$bodyStr\n}"
    }
}

class TransitionNode(
    private val name : String,
    private val args : ASTNode,
    private val body : List<ASTNode>,
    private val loc : Pair<Int,Int>
) : ASTNode(listOf(args) + body) {
    override fun errorPass(): List<CompileError> {
        val errors = mutableListOf<CompileError>()
        errors.addAll(args.errorPass())
        errors.addAll(body.flatMap { it.errorPass() })
        if (name == "initially") {
            errors.add(CompileError(loc, "only constructors (not transitions) can synchronize on the 'initially' action"))
        }
        // TODO ensure that each transit has a unique state var
        return errors
    }
    override fun transitions(): List<ActionDecl> {
        return listOf(
            ActionDecl(
                ActionSignature(name,args.actionArgs()),
                super.guards(),
                super.transits()
            )
        )
    }
    override fun toString(): String {
        val bodyStr = body.joinToString("\n") { "$it".prependIndent() }
        return "transition $name($args) {\n$bodyStr\n}"
    }
}

class ArgsNode(
    private val args : List<ASTNode>
) : ASTNode(args) {
    override fun toString(): String {
        return args.joinToString(", ") { it.toString() }
    }
}

class ArgNode(
    private val name : String,
    private val type : Type
) : ASTNode(listOf()) {
    override fun actionArgs(): List<Variable> {
        return listOf(Variable(name,type))
    }
    override fun toString(): String {
        return "$name : $type"
    }
}

class ActionBodyNode(
    private val body : ASTNode
) : ASTNode(listOf(body)) {
    override fun toString(): String {
        return body.toString()
    }
}

class GuardNode(
    val expr : ASTNode
) : ASTNode(listOf(expr)) {
    override fun guards(): List<ASTNode> {
        return listOf(expr)
    }
    override fun toString(): String {
        val exprStr = "$expr".prependIndent()
        return "guard:\n$exprStr"
    }
}

class TransitNode(
    private val transits : List<ASTNode>
) : ASTNode(transits) {
    override fun toString(): String {
        return "transit:\n${transits.joinToString("\n") { "$it".prependIndent() }}"
    }
}

class ErrorNode(
    private val expr : ASTNode
) : ASTNode(listOf(expr)) {
    override fun toString(): String {
        val exprStr = "$expr".prependIndent()
        return "error:\n$exprStr"
    }
}

class VarTransitNode(
    val varName : String,
    val expr : ASTNode
) : ASTNode(listOf(expr)) {
    override fun transits(): Map<String, ASTNode> {
        return mapOf(Pair(varName,expr))
    }
    override fun toString(): String {
        return "$varName := $expr"
    }
}

class UnaryOpExprNode(
    private val op : String,
    private val operand : ASTNode
) : ASTNode(listOf(operand)) {
    override fun toZ3GuardString(symbolTypes : Map<String,Type>, argSymbols : Set<String>, forceString : Boolean): String {
        exspecs.tools.assert(!forceString, "Cannot force a unary boolean operator to a string")
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
    private val lhsOperand : ASTNode,
    private val rhsOperand : ASTNode
) : ASTNode(listOf(lhsOperand,rhsOperand)) {
    override fun toZ3GuardString(symbolTypes : Map<String,Type>, argSymbols : Set<String>, forceString : Boolean): String {
        val lhsType = lhsOperand.type(symbolTypes)
        val rhsType = rhsOperand.type(symbolTypes)
        val isStringConcat = op == "+" && (lhsType is StringType || rhsType is StringType)
        exspecs.tools.assert(!forceString || isStringConcat, "Cannot force a binary boolean operator to a string")

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
    private val type : Type
) : ASTNode(listOf()) {
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
        return if (type == stringType) "\"$value\"" else value
    }
}

class SymbolValueExprNode(
    private val symbol : String
) : ASTNode(listOf()) {
    override fun toZ3GuardString(symbolTypes : Map<String,Type>, argSymbols : Set<String>, forceString : Boolean): String {
        val type = symbolTypes[symbol]
        if (forceString) {
            return when (type) {
                is BoolType -> throw RuntimeException("Cannot convert a Bool to a string")
                is IntType -> {
                    if (symbol in argSymbols) {
                        "ctx.intToString(ctx.mkIntConst(\"$symbol\"))"
                    } else {
                        "ctx.ctx.mkString(${symbol}.toString())"
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
    private val name : String
) : ASTNode(listOf()) {
    override fun procPass(): List<ProcDecl> {
        return listOf(ProcDecl(name, listOf(), ProcDeclType.Proc))
    }
    override fun toString(): String {
        return name
    }
}

class CompositeProcExprNode(
    private val compositeProcs : List<ASTNode>
) : ASTNode(compositeProcs) {
    override fun toString(): String {
        return compositeProcs.joinToString(" || ") { it.toString() }
    }
}

class CompileError(
    private val loc : Pair<Int,Int>,
    private val msg : String
) {
    override fun toString(): String {
        val range = if (loc.first == loc.second) {
            "line ${loc.first}"
        } else {
            "lines ${loc.first}-${loc.second}"
        }
        return "$range: $msg"
    }
}
