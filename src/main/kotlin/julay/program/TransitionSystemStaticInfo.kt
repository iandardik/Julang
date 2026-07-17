package julay.program

import julay.program.action.ConcreteAction
import julay.program.action.SymbolicAction

data class TransitionSystemStaticInfo(
    /**
     * A name that uniquely describes the process class.
     */
    val name: String,

    /**
     * The alphabet, i.e. the set of all possible symbolic actions that this TS will engage in.
     */
    val alphabet: Set<SymbolicAction>,

    /**
     * For each constructor action, a function that constructs a new TransitionSystem.
     */
    val constructors: Map<SymbolicAction, suspend (Program, ConcreteAction) -> TransitionSystem>,

    /**
     * Alphabet actions that rendezvous only on dynamic [Channel]s (no static Program.actionTable entry).
     */
    val dynamicChannelActions: Set<SymbolicAction> = emptySet(),
) {
    fun classID() = hashCode()
}

interface StaticInfo {
    fun staticInfo(): TransitionSystemStaticInfo
}
