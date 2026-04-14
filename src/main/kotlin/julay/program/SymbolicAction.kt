package julay.program

/**
 * Represents a symbolic action (in particular, its signature), excluding its types (for now).
 * SymbolicActions are not particular to a given transition system / proc.
 */
data class SymbolicAction(
    val name : String,
    val args : List<Variable>
) {
    override fun toString(): String {
        return "$name(" + args.joinToString(", ") + ")"
    }
}
