package julay.program.action

import com.microsoft.z3.BoolExpr
import julay.program.Channel
import julay.tools.assert

/**
 * Represents a symbolic action for a given transition system / proc.
 * This class is particular to a transition system / proc because it dictates when it's enabled (via the guard). Usually
 * a symbolic action also describes how the transition system transits to a new state, but we do not include this here
 * (each TransitionSystem class decides how the transit should happen).
 */
data class TSAction(
    val symAction : SymbolicAction,
    val guard : BoolExpr,
    val syncRole : SyncRole = SyncRole.Default,
    /**
     * When non-null, sync on this dynamic [Channel] instead of the static [Program.actionTable] channel.
     */
    val channel : Channel? = null,
) {
    /**
     * [Default] / [Internal] come from source tags (untagged / `internal`).
     * [Service] comes from the `service` tag.
     * [Consumer] is assigned by the compiler to untagged transitions on a serviced action
     * (there is no consumer source tag).
     */
    enum class SyncRole { Default, Internal, Service, Consumer }

    init {
        assert(
            !(syncRole == SyncRole.Internal) || symAction.isInternal,
            "Expected Internal role => SymbolicAction.isInternal",
        )
        assert(
            !(syncRole != SyncRole.Internal) || !symAction.isInternal,
            "Expected non-Internal role => !SymbolicAction.isInternal",
        )
    }
}
