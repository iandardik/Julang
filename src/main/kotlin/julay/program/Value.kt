package julay.program

import io.github.cvc5.Term
import io.github.cvc5.TermManager
import julay.tools.assert

data class Value(
    val value: Any,
    val type: Type,
) {
    init {
        assert(type.isOfType(value), "Value constructed with mismatched value and type: $value : $type")
    }

    fun toSmtTerm(tm: TermManager): Term = type.toSmtTerm(this, tm)

    override fun toString(): String {
        if (type is StringType && value is String) {
            return value
                .replace("\\u{a}", "\n")
                .replace("\\u{9}", "\t")
        }
        return value.toString()
    }
}
