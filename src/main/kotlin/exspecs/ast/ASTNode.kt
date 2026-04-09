package exspecs.ast

import exspecs.program.*

interface ASTNode {
    fun errorPass() : List<CompileError>
    fun toKotlin() : String {
        return this.toString()
    }
}

class RootNode(
    private val declNodes : List<ASTNode>
) : ASTNode {
    override fun errorPass(): List<CompileError> {
        return declNodes.flatMap { it.errorPass() }
    }
    override fun toString(): String {
        return declNodes.joinToString("\n\n") { it.toString() }
    }
}

class DeclNode(
    private val declNode : ASTNode
) : ASTNode {
    override fun errorPass(): List<CompileError> {
        return declNode.errorPass()
    }
    override fun toString(): String {
        return declNode.toString()
    }
}

class ProcClassNode(
    private val name : String,
    private val localDecls : List<ASTNode>
) : ASTNode {
    /*fun toTypedAST() : TypedProcClassNode {
        val varDecls = localDecls.filterIsInstance<VarDeclNode>()
        val actionDecls = localDecls.filterIsInstance<ActionDeclNode>()
        localDecls
            .filter { it !in varDecls && it !in actionDecls }
            .forEach { throw RuntimeException("Unexpected p-class declaration: $it") }
        return TypedProcClassNode(name, varDecls.map { it.toTypedAST() }, actionDecls.map { it.toTypedAST() })
    }*/
    override fun errorPass(): List<CompileError> {
        return localDecls.flatMap { it.errorPass() }
    }
    override fun toString(): String {
        val body = localDecls.joinToString("\n") { "$it".prependIndent() }
        return "p-class $name {\n$body\n}"
    }
}

class ProcNode(
    private val name : String,
    private val value : ASTNode
) : ASTNode {
    override fun errorPass(): List<CompileError> {
        return value.errorPass()
    }
    override fun toString(): String {
        return "proc $name := $value"
    }
}

class ProgramNode(
    private val name : String,
    private val value : ASTNode
) : ASTNode {
    override fun errorPass(): List<CompileError> {
        return value.errorPass()
    }
    override fun toString(): String {
        return "program $name := $value"
    }
}

class SpecNode(
    private val name : String,
    private val value : ASTNode
) : ASTNode {
    override fun errorPass(): List<CompileError> {
        return value.errorPass()
    }
    override fun toString(): String {
        return "spec $name := $value"
    }
}

class ProcClassBodyNode(
    private val body : ASTNode
) : ASTNode {
    override fun errorPass(): List<CompileError> {
        return body.errorPass()
    }
    override fun toString(): String {
        return body.toString()
    }
}

class VarNode(
    private val name : String,
    private val type : String
) : ASTNode {
    override fun errorPass(): List<CompileError> {
        return listOf()
    }
    override fun toString(): String {
        return "$name : $type"
    }
}

class ConstructorNode(
    private val name : String,
    private val args : ASTNode,
    private val body : List<ASTNode>
) : ASTNode {
    override fun errorPass(): List<CompileError> {
        val errors = mutableListOf<CompileError>()
        errors.addAll(args.errorPass())
        errors.addAll(body.flatMap { it.errorPass() })
        // TODO ensure that each transit includes each state var exactly once
        // TODO ensure that there is no guard, since it will not be followed by the ConstructorTS
        return errors
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
) : ASTNode {
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
    override fun toString(): String {
        val bodyStr = body.joinToString("\n") { "$it".prependIndent() }
        return "transition $name($args) {\n$bodyStr\n}"
    }
}

class ArgsNode(
    private val args : List<ASTNode>
) : ASTNode {
    fun toTypedAST() : TypedActionArgsNode {
        val typedArgs = args.map { arg ->
            if (arg !is ArgNode) {
                throw RuntimeException("Expected ActionArgNode")
            }
            arg.toTypedAST()
        }
        return TypedActionArgsNode(typedArgs)
    }
    override fun errorPass(): List<CompileError> {
        return args.flatMap { it.errorPass() }
    }
    override fun toString(): String {
        return args.joinToString(", ") { it.toString() }
    }
}

class ArgNode(
    private val name : String,
    private val type : String
) : ASTNode {
    fun toTypedAST() : TypedActionArgNode {
        return TypedActionArgNode(name, parseType(type))
    }
    override fun errorPass(): List<CompileError> {
        return listOf()
    }
    override fun toString(): String {
        return "$name : $type"
    }
}

class ActionBodyNode(
    private val body : ASTNode
) : ASTNode {
    override fun errorPass(): List<CompileError> {
        return body.errorPass()
    }
    override fun toString(): String {
        return body.toString()
    }
}

class GuardNode(
    private val expr : ASTNode
) : ASTNode {
    fun toTypedAST() : TypedGuardNode {
        if (expr !is ExprNode) {
            throw RuntimeException("Expected TypedExprNode")
        }
        val typedGuardExpr = expr.toTypedAST()
        return TypedGuardNode(typedGuardExpr)
    }
    override fun errorPass(): List<CompileError> {
        return expr.errorPass()
    }
    override fun toString(): String {
        val exprStr = "$expr".prependIndent()
        return "guard:\n$exprStr"
    }
}

class TransitNode(
    private val transits : List<ASTNode>
) : ASTNode {
    /*fun toTypedAST() : TypedUpdateNode {
        val typedUpdates = transits.map { transit ->
            if (transit !is VarUpdateNode) {
                throw RuntimeException("Expected UpdateNode") // TODO
            }
            transit.toTypedAST()
        }
        return TypedUpdateNode(typedUpdates)
    }*/
    override fun errorPass(): List<CompileError> {
        return transits.flatMap { it.errorPass() }
    }
    override fun toString(): String {
        return "transit:\n${transits.joinToString("\n") { "$it".prependIndent() }}"
    }
}

class ErrorNode(
    private val expr : ASTNode
) : ASTNode {
    override fun errorPass(): List<CompileError> {
        return expr.errorPass()
    }
    override fun toString(): String {
        val exprStr = "$expr".prependIndent()
        return "error:\n$exprStr"
    }
}

class VarTransitNode(
    private val varName : String,
    private val expr : ASTNode
) : ASTNode {
    override fun errorPass(): List<CompileError> {
        return expr.errorPass()
    }
    override fun toString(): String {
        return "$varName := $expr"
    }
}

interface ExprNode : ASTNode {
    fun toTypedAST() : TypedExprNode
}

class UnaryOpExprNode(
    private val op : String,
    private val operand : ASTNode
) : ExprNode {
    override fun toTypedAST(): TypedExprNode {
        if (operand !is ExprNode) {
            throw RuntimeException("Expected ExprNode")
        }
        return TypedUnaryOpExprNode(op, operand.toTypedAST())
    }
    override fun errorPass(): List<CompileError> {
        return operand.errorPass()
    }
    override fun toString(): String {
        return "$op $operand"
    }
}

class BinaryOpExprNode(
    private val op : String,
    private val lhsOperand : ASTNode,
    private val rhsOperand : ASTNode
) : ExprNode {
    override fun toTypedAST(): TypedExprNode {
        if (lhsOperand !is ExprNode || rhsOperand !is ExprNode) {
            throw RuntimeException("Expected ExprNode")
        }
        return TypedBinaryOpExprNode(op, lhsOperand.toTypedAST(), rhsOperand.toTypedAST())
    }
    override fun errorPass(): List<CompileError> {
        return lhsOperand.errorPass() + rhsOperand.errorPass()
    }
    override fun toString(): String {
        // for readability
        val lhs = if (lhsOperand is BinaryOpExprNode && lhsOperand.op != "=") "($lhsOperand)" else "$lhsOperand"
        val rhs = if (rhsOperand is BinaryOpExprNode && rhsOperand.op != "=") "($rhsOperand)" else "$rhsOperand"
        return "$lhs $op $rhs"
    }
}

class LiteralValueExprNode(
    private val value : String,
    private val type : Type
) : ExprNode {
    override fun toTypedAST(): TypedExprNode {
        return TypedLiteralValueExprNode(value,type)
    }
    override fun errorPass(): List<CompileError> {
        return listOf()
    }
    override fun toString(): String {
        return if (type == stringType) "\"$value\"" else value
    }
}

class SymbolValueExprNode(
    private val symbol : String
) : ExprNode {
    override fun toTypedAST(): TypedExprNode {
        return TypedSymbolValueExprNode(symbol)
    }
    override fun errorPass(): List<CompileError> {
        return listOf()
    }
    override fun toString(): String {
        return symbol
    }
}

interface ProcExprNode : ASTNode {}

class ValueProcExprNode(
    private val name : String
) : ProcExprNode {
    override fun errorPass(): List<CompileError> {
        return listOf()
    }
    override fun toString(): String {
        return name
    }
}

class CompositeProcExprNode(
    private val compositeProcs : List<ASTNode>
) : ProcExprNode {
    override fun errorPass(): List<CompileError> {
        return compositeProcs.flatMap { it.errorPass() }
    }
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
