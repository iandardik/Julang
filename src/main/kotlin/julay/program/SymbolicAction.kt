package julay.program

/**
 * Represents a symbolic action. SymbolicActions are not particular to a given transition system / proc.
 * [isInternal] is true for actions declared with the `internal` tag (sync size 1).
 */
data class SymbolicAction(
    val name : String,
    val args : List<Variable>,
    val isInternal : Boolean = false,
) {
    override fun toString(): String {
        return "$name(" + args.joinToString(", ") + ")"
    }
}
