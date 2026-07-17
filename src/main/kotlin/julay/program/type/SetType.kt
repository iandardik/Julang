package julay.program.type

import com.microsoft.z3.*
import julay.compiler.decl.mangleTypeForName
import julay.program.ContextLocalCache
import julay.program.Value
import julay.program.Variable
import julay.tools.*

class SetCellMetadata(
    val sort: DatatypeSort<*>,
    val constructorDecl: FuncDecl<*>,
    val arrAccessor: FuncDecl<*>,
    val sizeAccessor: FuncDecl<*>,
    val arraySort: ArraySort<Sort, BoolSort>,
    val domainSort: Sort,
)

/**
 * Built-in parametric set type. Cell datatype metadata is built directly in the caller's
 * [Context] (no per-instance home Context); results are cached per live Context so the
 * datatype is not redefined on every use, without retaining closed Contexts.
 */
data class SetType(val elementType: Type) : Type {
    private val cellName = "SetCell_${mangleTypeForName(elementType)}"
    // TODO: this shared per-Context cache breaks the rule that interprocess communication
    // must go only through SyncChannel (procs can observe/reuse metadata across contexts).
    // Fix later; kept for now so fixed-name mkDatatypeSort is not redefined within a Context.
    private val metaByCtx = ContextLocalCache<SetCellMetadata>()

    fun cellMetadata(ctx: Context): SetCellMetadata =
        metaByCtx.getOrPut(ctx) { buildMetadata(ctx) }

    private fun buildMetadata(ctx: Context): SetCellMetadata {
        val domain = elementType.toZ3Sort(ctx)
        @Suppress("UNCHECKED_CAST")
        val arraySort = ctx.mkSetSort(domain) as ArraySort<Sort, BoolSort>
        val constructor = ctx.mkConstructor<Any>(
            "mk-$cellName",
            "is-$cellName",
            arrayOf("arr", "size"),
            arrayOf(arraySort, ctx.intSort),
            null,
        )
        val sort = ctx.mkDatatypeSort(cellName, arrayOf(constructor))
        return SetCellMetadata(
            sort = sort,
            constructorDecl = constructor.ConstructorDecl(),
            arrAccessor = constructor.accessorDecls[0],
            sizeAccessor = constructor.accessorDecls[1],
            arraySort = arraySort,
            domainSort = domain,
        )
    }

    override fun toZ3Expr(variable: Variable, ctx: Context): Expr<*> {
        return ctx.mkConst(variable.name, cellMetadata(ctx).sort)
    }

    override fun toZ3Expr(value: Value, ctx: Context): Expr<*> {
        @Suppress("UNCHECKED_CAST")
        val elements = value.value as Set<Any>
        val meta = cellMetadata(ctx)
        val domain = meta.domainSort
        @Suppress("UNCHECKED_CAST")
        var arr = ctx.mkEmptySet(domain) as ArrayExpr<Sort, BoolSort>
        for (elem in elements) {
            val elemValue = Value(elem, elementType)
            @Suppress("UNCHECKED_CAST")
            arr = ctx.mkSetAdd(arr, elementType.toZ3Expr(elemValue, ctx) as Expr<Sort>)
        }
        return ctx.mkApp(meta.constructorDecl, arr, ctx.mkInt(elements.size))
    }

    override fun fromZ3Expr(expr: Expr<*>, model: Model): Any {
        val ctx = model.julangContext()
        val meta = cellMetadata(ctx)
        val cell = model.eval(expr, true)
        @Suppress("UNCHECKED_CAST")
        val arrExpr = model.eval(ctx.mkApp(meta.arrAccessor, cell), true) as ArrayExpr<Sort, BoolSort>
        return when (elementType) {
            is IntType -> {
                val result = mutableSetOf<Int>()
                for (i in -50..50) {
                    if (model.eval(ctx.mkSetMemberAny(ctx.mkInt(i), arrExpr), true).isTrue) {
                        result.add(i)
                    }
                }
                result
            }
            is StringType -> {
                val result = mutableSetOf<String>()
                for (decl in model.constDecls) {
                    if (decl.range == ctx.stringSort) {
                        val candidate = decl.name.toString().trim('"')
                        if (model.eval(ctx.mkSetMemberAny(ctx.mkString(candidate), arrExpr), true).isTrue) {
                            result.add(candidate)
                        }
                    }
                }
                result
            }
            else -> throw RuntimeException("Set fromZ3Expr not implemented for element type $elementType")
        }
    }

    override fun isOfType(obj: Any): Boolean {
        if (obj !is Set<*>) return false
        return obj.all { it != null && elementType.isOfType(it) }
    }

    override fun toString(): String = "Set<${elementType}>"
}

fun setType(element: Type): SetType = SetType(element)
