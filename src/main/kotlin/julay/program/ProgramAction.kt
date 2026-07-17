package julay.program

import julay.concurrency.SyncChannel

data class ProgramAction(
    val action: SymbolicAction,
    var channel: SyncChannel<ConcreteAction, Constraint>,
)
