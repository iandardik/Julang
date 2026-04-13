package exspecs.program

import com.microsoft.z3.BoolExpr
import com.microsoft.z3.Context

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
     */
    fun actions() : Set<SymbolicAction>

    /**
     * The transition system transits to a (potentially new) state based on the given concrete action.
     */
    fun transit(act : ConcreteAction)

    /**
     * Exactly one Context should be used, and should be available here for public use. Using just one Context is
     * important for thread safety, see: https://stackoverflow.com/questions/25542200/multi-threaded-z3
     */
    fun getContext() : Context
}