package julay.program

import io.github.cvc5.TermManager

/**
 * Transition systems do not worry about channels or any kind of communication--they simply deal with their own internal
 * workings. Procs, on the other hand, are what deal with communication and synchronization.
 */
interface TransitionSystem {
    /**
     * The set of actions that are allowed at the current state. The caller is expected to filter the actions to ensure
     * that each one is satisfiable, so it is permissible to return actions that are not satisfiable. This is why it is
     * permissible to simply return the alphabet() here, though implementations may wish to dynamically decide which
     * actions to return.
     *
     * Guards must be built with [tm], the single TermManager owned by the calling Proc (one TermManager per Proc for
     * CVC5 thread safety). Cross-process formula exchange uses SMT-LIB via SyncChannel, not shared TermManagers.
     */
    suspend fun actions(tm: TermManager): Set<TSAction>

    /**
     * The transition system transits to a (potentially new) state based on the given concrete action.
     */
    suspend fun transit(act: ConcreteAction)
}
