package julay.program

import io.github.cvc5.Term
import io.github.cvc5.TermManager

/**
 * Represents a typed variable, including state variables and action arguments.
 */
data class Variable(
    val name: String,
    val type: Type,
) {
    fun toSmtTerm(tm: TermManager): Term = type.toSmtTerm(this, tm)

    override fun toString(): String = "$name : $type"
}
