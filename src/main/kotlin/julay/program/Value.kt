package julay.program

import com.microsoft.z3.Context
import com.microsoft.z3.Expr
import julay.tools.assert

data class Value(
    val value : Any,
    val type : Type
) {
    constructor(z3Value : Expr<*>, ty : Type) : this(ty.fromZ3Expr(z3Value), ty) {}

    init {
        assert(type.isOfType(value), "Value constructed with mismatched value and type: $value : $type")
    }

    fun toZ3Expr(ctx : Context) : Expr<*> {
        return type.toZ3Expr(this, ctx)
    }

    override fun toString(): String {
        if (type is StringType && value is String) {
            // Z3 replaces newlines and tabs with a special escape sequence
            return value
                .replace("\\u{a}", "\n")
                .replace("\\u{9}", "\t")
        }
        return value.toString()
    }
}