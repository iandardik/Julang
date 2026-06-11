package julay.compiler.decl

import julay.compiler.ProgramLoc
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
    val transits : Map<String,ExprNode>,
    val modifier: TSAction.SyncRole,
    val loc: ProgramLoc
)
