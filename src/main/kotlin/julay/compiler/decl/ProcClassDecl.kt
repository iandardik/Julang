package julay.compiler.decl

import julay.compiler.ProgramLoc
import julay.compiler.ast.EffectStmtNode
import julay.compiler.ast.ErrorArmNode
import julay.compiler.ast.ExprNode
import julay.program.*

data class ProcClassDecl(
    val name : String,
    val stateVars : List<Variable>,
    val constructors : List<ActionDecl>,
    val transitions : List<ActionDecl>,
)

data class ActionDecl(
    val action : SymbolicAction,
    val guards : List<ExprNode>,
    val transits : List<TransitUpdate>,
    val modifier: TSAction.SyncRole,
    val loc: ProgramLoc,
    val effects : List<EffectStmtNode> = emptyList(),
    val errors : List<ErrorArmNode> = emptyList(),
    /**
     * When non-null, this action requires a dynamic [Channel] bind.
     * For Julay transitions this is the state-variable name in `transition name<var>(...)`.
     * Libraries may pass `""` to mark the action dynamic-channel-only without a bind name.
     */
    val dynamicChannelVar: String? = null,
) {
    val requiresDynamicChannel: Boolean get() = dynamicChannelVar != null
}
