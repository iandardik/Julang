package julay.program

import julay.concurrency.SyncChannel
import julay.tools.SmtConstraint

data class ProgramAction(
    val action: SymbolicAction,
    var channel: SyncChannel<ConcreteAction, SmtConstraint>,
)
