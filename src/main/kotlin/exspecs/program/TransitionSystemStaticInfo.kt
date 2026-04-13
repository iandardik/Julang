package exspecs.program

data class TransitionSystemStaticInfo(
    /**
     * The alphabet, i.e. the set of all possible symbolic actions that this TS will engage in.
     */
    val alphabet : Set<ActionSignature>,

    val constructors : Set<ActionSignature>,

    /**
     * Whether the transition system will terminate itself. If true (default) then the program will not end before the
     * process terminates; otherwise, the process may be terminated early (this is desirable e.g., for library
     * processes).
     */
    val selfTerminate : Boolean,

    val construct : (ConcreteAction)->TransitionSystem
) {}
