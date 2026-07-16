package julay.program

import com.microsoft.z3.BoolExpr
import com.microsoft.z3.Context

data class TransitionSystemStaticInfo(
    /**
     * A name that uniquely describes the process class.
     */
    val name : String,

    /**
     * The alphabet, i.e. the set of all possible symbolic actions that this TS will engage in.
     */
    val alphabet : Set<SymbolicAction>,

    /**
     * For each constructor action, a function that constructs a new TransitionSystem.
     */
    val constructors : Map<SymbolicAction, suspend (Program,ConcreteAction)->TransitionSystem>,

    /**
     * Optional Z3 guards for constructor offers (and for filtering `initially` spawns).
     * Keys should be a subset of [constructors]. Missing entries are treated as true.
     */
    val constructorGuards : Map<SymbolicAction, (Context) -> BoolExpr> = emptyMap(),
) {
    fun classID() = hashCode()
}

interface StaticInfo {
    fun staticInfo() : TransitionSystemStaticInfo
}
