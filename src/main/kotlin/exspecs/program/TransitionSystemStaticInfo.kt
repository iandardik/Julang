package exspecs.program

data class TransitionSystemStaticInfo(
    /**
     * The alphabet, i.e. the set of all possible symbolic actions that this TS will engage in.
     */
    val alphabet : Set<ActionSignature>,
    val constructors : Set<ActionSignature>,
    val selfTerminate : Boolean,
    val construct : ()->TransitionSystem
) {}
