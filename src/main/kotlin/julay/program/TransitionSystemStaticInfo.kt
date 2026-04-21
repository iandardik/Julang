package julay.program

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
    val constructors : Map<SymbolicAction, (Program,ConcreteAction)->TransitionSystem>,

    /**
     * Whether the transition system will terminate itself. If true (default) then the program will not end before the
     * process terminates; otherwise, the process may be terminated early (this is desirable e.g., for library
     * processes).
     */
    val selfTerminate : Boolean,
) {
    fun classID() = hashCode()
}

interface StaticInfo {
    fun staticInfo() : TransitionSystemStaticInfo
}