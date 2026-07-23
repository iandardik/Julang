package julay.program.action

import julay.program.Variable

/**
 * Represents a symbolic action. SymbolicActions are not particular to a given transition system / proc.
 * [isInternal] is true for actions declared with the `internal` tag (sync size 1).
 * [isSession] is true for actions declared with the `session` tag (exclusive affinity).
 */
data class SymbolicAction(
    val name: String,
    val args: List<Variable>,
    val isInternal: Boolean = false,
    val isSession: Boolean = false,
    /** Channel identity; defaults to [name]. Composition-hidden and per-proc internals use distinct keys. */
    val channelKey: String = name,
) {
    override fun toString(): String {
        return "$name(" + args.joinToString(", ") + ")"
    }
}
