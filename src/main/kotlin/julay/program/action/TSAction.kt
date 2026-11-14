package julay.program.action

import julay.tools.assert

/**
 * Symbolic action role metadata used by the compiler (alphabet / modifiers).
 * Runtime SyncChannel / Z3 guards have been removed; Go codegen is the JAR backend.
 */
data class TSAction(
    val symAction: SymbolicAction,
    val syncRole: SyncRole = SyncRole.Default,
) {
    /**
     * [Default] comes from untagged actions (composition-private when paired).
     * [Internal] comes from the `internal` tag (local).
     * [Timer] comes from the `timer` tag (local like Internal).
     * [Service] comes from the `service` tag (one hub per action name; stays external).
     * [Client] comes from the `client` tag (only syncs with a service).
     */
    enum class SyncRole { Default, Internal, Timer, Service, Client }

    init {
        assert(
            !(syncRole == SyncRole.Internal) || symAction.isInternal,
            "Expected Internal role => SymbolicAction.isInternal",
        )
        assert(
            !(syncRole != SyncRole.Internal) || !symAction.isInternal,
            "Expected non-Internal role => !SymbolicAction.isInternal",
        )
        assert(
            !(syncRole == SyncRole.Timer) || symAction.isTimer,
            "Expected Timer role => SymbolicAction.isTimer",
        )
        assert(
            !(syncRole != SyncRole.Timer) || !symAction.isTimer,
            "Expected non-Timer role => !SymbolicAction.isTimer",
        )
    }
}
