package julay.program

import com.microsoft.z3.*

/**
 * Built-in parametric list type, backed by Z3 [SeqSort] and Kotlin [List] at runtime.
 */
data class ListType(val elementType: Type) : Type {
    private val homeCtx = Context()
    private val homeSort: SeqSort<*> by lazy {
        @Suppress("UNCHECKED_CAST")
        homeCtx.mkSeqSort(elementType.toZ3Sort(homeCtx) as Sort) as SeqSort<*>
    }

    fun sort(ctx: Context): SeqSort<*> {
        @Suppress("UNCHECKED_CAST")
        return if (ctx === homeCtx) {
            homeSort
        } else {
            homeSort.translate(ctx) as SeqSort<*>
        }
    }

    override fun toZ3Expr(variable: Variable, ctx: Context): Expr<*> {
        return ctx.mkConst(variable.name, sort(ctx))
    }

    override fun toZ3Expr(value: Value, ctx: Context): Expr<*> {
        @Suppress("UNCHECKED_CAST")
        val elements = value.value as List<Any>
        val elemSort = elementType.toZ3Sort(ctx)
        if (elements.isEmpty()) {
            return ctx.mkEmptySeq(elemSort)
        }
        val units = elements.map { elem ->
            val elemValue = Value(elem, elementType)
            @Suppress("UNCHECKED_CAST")
            ctx.mkUnit(elementType.toZ3Expr(elemValue, ctx) as Expr<Sort>)
        }
        @Suppress("UNCHECKED_CAST")
        return ctx.mkConcat(*units.toTypedArray()) as Expr<*>
    }

    override fun fromZ3Expr(expr: Expr<*>, model: Model): Any {
        val ctx = model.julangContext()
        @Suppress("UNCHECKED_CAST")
        val seq = model.eval(expr, true) as Expr<SeqSort<Sort>>
        val len = intType.fromZ3Expr(model.eval(ctx.mkLength(seq), true), model) as Int
        return (0 until len).map { i ->
            val elemExpr = model.eval(ctx.mkNth(seq, ctx.mkInt(i)), true)
            elementType.fromZ3Expr(elemExpr, model)
        }
    }

    override fun isOfType(obj: Any): Boolean {
        if (obj !is List<*>) return false
        return obj.all { it != null && elementType.isOfType(it) }
    }

    override fun toString(): String {
        val elem = elementType.toString()
        return if (elementType is ListType || ' ' in elem) {
            "List ($elem)"
        } else {
            "List $elem"
        }
    }
}

fun listType(element: Type): ListType = ListType(element)

fun Type.toZ3Sort(ctx: Context): Sort = when (this) {
    is BoolType -> ctx.boolSort
    is IntType -> ctx.intSort
    is StringType -> ctx.stringSort
    is ObjClassType -> sort(ctx)
    is ListType -> sort(ctx)
    is TypeVar -> throw RuntimeException("TypeVar \"$name\" must not reach Z3 sort construction")
    else -> throw RuntimeException("Cannot build Z3 sort for type $this")
}
