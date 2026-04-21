package julay.program

import com.microsoft.z3.BoolExpr
import julay.concurrency.SyncChannel

data class ProgramAction(
    val action : SymbolicAction,
    var channel : SyncChannel<ConcreteAction, BoolExpr>,
) {}