package julay.program

import com.microsoft.z3.*
import julay.compiler.decl.mangleTypeForName
import julay.tools.*

class SetCellMetadata(
    val sort: DatatypeSort<*>,
    val constructorDecl: FuncDecl<*>,
    val arrAccessor: FuncDecl<*>,
    val sizeAccessor: FuncDecl<*>,
    val arraySort: ArraySort<Sort, BoolSort>,
    val domainSort: Sort,
)

data class SetType(val elementType: Type) : Type {
    private val homeCtx = Context()
    private val cellName = "SetCell_${mangleTypeForName(elementType)}"

    private val metadata: SetCellMetadata by lazy {
        val domain = elementType.toZ3Sort(homeCtx)
        @Suppress("UNCHECKED_CAST")
        val arraySort = homeCtx.mkSetSort(domain) as ArraySort<Sort, BoolSort>
        val constructor = homeCtx.mkConstructor<Any>(
            "mk-$cellName",
            "is-$cellName",
            arrayOf("arr", "size"),
            arrayOf(arraySort, homeCtx.intSort),
            null,
        )
        val sort = homeCtx.mkDatatypeSort(cellName, arrayOf(constructor))
        SetCellMetadata(
            sort = sort,
            constructorDecl = constructor.ConstructorDecl(),
            arrAccessor = constructor.accessorDecls[0],
            sizeAccessor = constructor.accessorDecls[1],
            arraySort = arraySort,
            domainSort = domain,
        )
    }

    fun cellMetadata(ctx: Context): SetCellMetadata =
        if (ctx === homeCtx) {
            metadata
        } else {
            @Suppress("UNCHECKED_CAST")
            val translatedSort = metadata.sort.translate(ctx) as DatatypeSort<*>
            @Suppress("UNCHECKED_CAST")
            val translatedArraySort = metadata.arraySort.translate(ctx) as ArraySort<Sort, BoolSort>
            SetCellMetadata(
                sort = translatedSort,
                constructorDecl = metadata.constructorDecl.translate(ctx),
                arrAccessor = metadata.arrAccessor.translate(ctx),
                sizeAccessor = metadata.sizeAccessor.translate(ctx),
                arraySort = translatedArraySort,
                domainSort = metadata.domainSort.translate(ctx),
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
