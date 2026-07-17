package julay.program.action

import julay.concurrency.SyncChannel
import julay.program.Constraint

data class ProgramAction(
    val action: SymbolicAction,
    var channel: SyncChannel<SyncPayload, Constraint>,
)
