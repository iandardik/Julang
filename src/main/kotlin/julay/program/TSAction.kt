package julay.program

import com.microsoft.z3.BoolExpr

/**
 * Represents a symbolic action for a given transition system / proc.
 * This class is particular to a transition system / proc because it dictates when it's enabled (via the guard). Usually
 * a symbolic action also describes how the transition system transits to a new state, but we do not include this here
 * (each TransitionSystem class decides how the transit should happen).
 */
data class TSAction(
    val symAction : SymbolicAction,
    val guard : BoolExpr,
    val isServicer : Boolean,
) {}
