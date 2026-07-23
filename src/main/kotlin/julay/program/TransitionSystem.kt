package julay.program

import com.microsoft.z3.Context
import julay.program.action.ConcreteAction
import julay.program.action.TSAction

/**
 * Transition systems do not worry about channels or any kind of communication--they simply deal with their own internal
 * workings. Procs, on the other hand, are what deal with communication and synchronization.
 *
 * [bindHostProc] / [setSessionPeer] exist so generated session-effect code can reach the host [Proc]
 * and current sync peer without a ThreadLocal. Library TSs that never emit those effects may leave
 * the defaults as no-ops.
 */
interface TransitionSystem {
    /**
     * The set of actions that are allowed at the current state. The caller is expected to filter the actions to ensure
     * that each one is satisfiable, so it is permissible to return actions that are not satisfiable. This is why it is
     * permissible to simply return the alphabet() here, though implementations may wish to dynamically decide which
     * actions to return.
     *
     * Guards must be built with [ctx], the single Context owned by the calling Proc (one Context per Proc for Z3 thread
     * safety; see https://stackoverflow.com/questions/25542200/multi-threaded-z3).
     */
    suspend fun actions(ctx: Context): Set<TSAction>

    /**
     * The transition system transits to a (potentially new) state based on the given concrete action.
     */
    suspend fun transit(act: ConcreteAction)

    /**
     * Completes construction for a TS allocated uninitialized by [Program.spawn]: apply constructor
     * transit and effects. Called on the child proc before its select loop so effects never run as
     * the parent. Default is a no-op for TSs that need no deferred init (e.g. [Program.spawnProc]).
     */
    suspend fun finishConstruction(act: ConcreteAction) {}

    /** Called once when a [Proc] wraps this TS; used by session-effect codegen. */
    fun bindHostProc(host: Proc) {}

    /** Current sync peer for this transit (or null); cleared by [Proc] after transit. */
    fun setSessionPeer(peer: Proc?) {}
}
