package julay.compiler.decl

import julay.compiler.ProgramLoc
import julay.compiler.ast.CallStmtNode
import julay.compiler.ast.ErrorArmNode
import julay.compiler.ast.ExprNode
import julay.program.*
import julay.program.type.*
import julay.program.action.*

data class ProcClassDecl(
    val name: String,
    val stateVars: List<Variable>,
    val constructors: List<ActionDecl>,
    val transitions: List<ActionDecl>,
)

data class ActionDecl(
    val action: SymbolicAction,
    val guards: List<ExprNode>,
    val transits: List<TransitUpdate>,
    val modifier: TSAction.SyncRole,
    val loc: ProgramLoc,
    val befores: List<CallStmtNode> = emptyList(),
    val afters: List<CallStmtNode> = emptyList(),
    val errors: List<ErrorArmNode> = emptyList(),
    /** When non-null, this transition is a procfun return edge (mutually exclusive with [transits]). */
    val returnExpr: ExprNode? = null,
) {
    val isSession: Boolean get() = action.isSession
    val isReturn: Boolean get() = returnExpr != null
}
