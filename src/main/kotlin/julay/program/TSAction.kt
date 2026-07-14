package julay.program

import io.github.cvc5.Term
import julay.tools.assert

/**
 * Represents a symbolic action for a given transition system / proc.
 * This class is particular to a transition system / proc because it dictates when it's enabled (via the guard). Usually
 * a symbolic action also describes how the transition system transits to a new state, but we do not include this here
 * (each TransitionSystem class decides how the transit should happen).
 */
data class TSAction(
    val symAction: SymbolicAction,
    val guard: Term,
    val syncRole: SyncRole = SyncRole.CSP,
) {
    enum class SyncRole { CSP, P2PService, P2PConsumer }

    init {
        assert(
            !(syncRole == SyncRole.CSP) || (symAction.syncType == SymbolicAction.SyncType.CSP),
            "Expected (syncRole == SyncRole.CSP) => (symAction.syncType == SymbolicAction.SyncType.CSP)",
        )
        assert(
            !(syncRole == SyncRole.P2PService) || (symAction.syncType == SymbolicAction.SyncType.P2P),
            "Expected (syncRole == SyncRole.P2PService) => (symAction.syncType == SymbolicAction.SyncType.P2P)",
        )
        assert(
            !(syncRole == SyncRole.P2PConsumer) || (symAction.syncType == SymbolicAction.SyncType.P2P),
            "Expected (syncRole == SyncRole.P2PConsumer) => (symAction.syncType == SymbolicAction.SyncType.P2P)",
        )
    }
}
