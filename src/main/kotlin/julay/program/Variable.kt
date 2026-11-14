package julay.program

import julay.program.type.Type

/**
 * Represents a typed variable, including state variables and action arguments.
 */
data class Variable(
    val name: String,
    val type: Type,
) {
    override fun toString(): String {
        return "$name : $type"
    }
}
