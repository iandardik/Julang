package julay.program.action

import com.microsoft.z3.BoolExpr
import julay.concurrency.SyncChannel
import julay.program.Constraint
import julay.program.sync.BoolExprFast
import julay.tools.assert

/**
 * Represents a symbolic action for a given transition system / proc.
 * This class is particular to a transition system / proc because it dictates when it's enabled (via the guard). Usually
 * a symbolic action also describes how the transition system transits to a new state, but we do not include this here
 * (each TransitionSystem class decides how the transit should happen).
 */
data class TSAction(
    val symAction: SymbolicAction,
    val guard: BoolExpr,
    val syncRole: SyncRole = SyncRole.Default,
    /**
     * When non-null, sync on this dedicated session SyncChannel instead of the static
     * [Program.actionTable] channel.
     */
    val syncChannel: SyncChannel<Constraint, SyncPayload>? = null,
    /**
     * Optional [BoolExprFast] for this action (codegen / libraries). When non-null, Julay may
     * use it instead of [guard] to avoid Z3 on sat/commit; null means BoolExpr/[guard] only.
     */
    val fastGuard: BoolExprFast? = null,
) {
    /**
     * [Default] / [Internal] come from source tags (untagged / `internal`).
     * [Provider] comes from the `provider` tag (one hub per action name).
     * [Client] comes from the `client` tag (only syncs with a provider, not other clients).
     * Session is tracked on [SymbolicAction.isSession], not as a sync role.
     */
    enum class SyncRole { Default, Internal, Provider, Client }

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
            !(symAction.isSession && syncRole == SyncRole.Provider),
            "Session actions cannot use Provider sync role",
        )
        assert(
            !(symAction.isSession && syncRole == SyncRole.Client),
            "Session actions cannot use Client sync role",
        )
        assert(
            !(symAction.isSession && syncRole == SyncRole.Internal),
            "Session actions cannot use Internal sync role",
        )
    }
}
