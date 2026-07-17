package julay.compiler.decl

import julay.compiler.ProgramLoc
import julay.compiler.ast.EffectStmtNode
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
    val effects: List<EffectStmtNode> = emptyList(),
    val errors: List<ErrorArmNode> = emptyList(),
) {
    val isSession: Boolean get() = action.isSession
}
