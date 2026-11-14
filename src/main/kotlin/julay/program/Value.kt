package julay.program

import julay.program.type.StringType
import julay.program.type.Type
import julay.tools.assert

data class Value(
    val value: Any,
    val type: Type,
) {
    init {
        assert(type.isOfType(value), "Value constructed with mismatched value and type: $value : $type")
    }

    override fun toString(): String {
        if (type is StringType && value is String) {
            return value
                .replace("\\u{a}", "\n")
                .replace("\\u{9}", "\t")
        }
        return value.toString()
    }
}
