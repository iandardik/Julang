package julay.program.type

import com.microsoft.z3.Context
import com.microsoft.z3.Expr
import com.microsoft.z3.Model
import julay.program.Value
import julay.program.Variable

/**
 * Finite domain declared with `sort Name := { ... }`.
 * Spec/quantifier domains only in the TLA+-first version — not executable.
 */
class SortType(
    val name: String,
    val elementType: Type,
    /** Canonical element texts for TLA+ .cfg (strings already quoted, ints/bools bare). */
    val cfgElements: List<String>,
) : Type {
    override fun toZ3Expr(variable: Variable, ctx: Context): Expr<*> {
        throw RuntimeException("sort \"$name\" is not executable")
    }

    override fun toZ3Expr(value: Value, ctx: Context): Expr<*> {
        throw RuntimeException("sort \"$name\" is not executable")
    }

    override fun fromZ3Expr(expr: Expr<*>, model: Model): Any {
        throw RuntimeException("sort \"$name\" is not executable")
    }

    override fun isOfType(obj: Any): Boolean = false

    override fun toString(): String = name

    override fun equals(other: Any?): Boolean =
        other is SortType && other.name == name &&
            other.elementType == elementType &&
            other.cfgElements == cfgElements

    override fun hashCode(): Int = name.hashCode()
}

fun Type.containsSortType(): Boolean = when (this) {
    is SortType -> true
    is ListType -> elementType.containsSortType()
    is SetType -> elementType.containsSortType()
    is MapType -> keyType.containsSortType() || valueType.containsSortType()
    is ObjClassType -> fields.any { it.type.containsSortType() }
    else -> false
}
