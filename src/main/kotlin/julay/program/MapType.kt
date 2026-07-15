package julay.program

import com.microsoft.z3.*
import julay.compiler.decl.mangleTypeForName
import julay.tools.*
import java.util.Collections
import java.util.WeakHashMap

class MapCellMetadata(
    val sort: DatatypeSort<*>,
    val constructorDecl: FuncDecl<*>,
    val arrAccessor: FuncDecl<*>,
    val keysAccessor: FuncDecl<*>,
    val sizeAccessor: FuncDecl<*>,
    val arraySort: ArraySort<*, *>,
    val keyArraySort: ArraySort<Sort, BoolSort>,
    val domainSort: Sort,
)

/**
 * Built-in parametric map type. Cell datatype metadata is built directly in the caller's
 * [Context] (no per-instance home Context); results are cached per Context so the datatype
 * is not redefined on every use.
 */
data class MapType(val keyType: Type, val valueType: Type) : Type {
    private val cellName = "MapCell_${mangleTypeForName(keyType)}_${mangleTypeForName(valueType)}"
    // TODO: this shared per-Context cache breaks the rule that interprocess communication
    // must go only through SyncChannel (procs can observe/reuse metadata across contexts).
    // Fix later; kept for now so fixed-name mkDatatypeSort is not redefined within a Context.
    private val metaByCtx: MutableMap<Context, MapCellMetadata> =
        Collections.synchronizedMap(WeakHashMap())

    fun cellMetadata(ctx: Context): MapCellMetadata =
        metaByCtx.getOrPut(ctx) { buildMetadata(ctx) }

    private fun buildMetadata(ctx: Context): MapCellMetadata {
        val keySort = keyType.toZ3Sort(ctx)
        val valueSort = valueType.toZ3Sort(ctx)
        @Suppress("UNCHECKED_CAST")
        val arraySort = ctx.mkArraySort(keySort, valueSort) as ArraySort<*, *>
        @Suppress("UNCHECKED_CAST")
        val keyArraySort = ctx.mkSetSort(keySort) as ArraySort<Sort, BoolSort>
        val constructor = ctx.mkConstructor<Any>(
            "mk-$cellName",
            "is-$cellName",
            arrayOf("arr", "keys", "size"),
            arrayOf(arraySort, keyArraySort, ctx.intSort),
            null,
        )
        val sort = ctx.mkDatatypeSort(cellName, arrayOf(constructor))
        return MapCellMetadata(
            sort = sort,
            constructorDecl = constructor.ConstructorDecl(),
            arrAccessor = constructor.accessorDecls[0],
            keysAccessor = constructor.accessorDecls[1],
            sizeAccessor = constructor.accessorDecls[2],
            arraySort = arraySort,
            keyArraySort = keyArraySort,
            domainSort = keySort,
        )
    }

    override fun toZ3Expr(variable: Variable, ctx: Context): Expr<*> {
        return ctx.mkConst(variable.name, cellMetadata(ctx).sort)
    }

    override fun toZ3Expr(value: Value, ctx: Context): Expr<*> {
        @Suppress("UNCHECKED_CAST")
        val map = value.value as Map<Any, Any>
        val meta = cellMetadata(ctx)
        val domain = meta.domainSort
        @Suppress("UNCHECKED_CAST")
        var arr = ctx.mkConstArray(
            domain as Sort,
            defaultValueExpr(ctx),
        ) as ArrayExpr<Sort, Sort>
        @Suppress("UNCHECKED_CAST")
        var keys = ctx.mkEmptySet(domain) as ArrayExpr<Sort, BoolSort>
        for ((k, v) in map) {
            @Suppress("UNCHECKED_CAST")
            val keyExpr = keyType.toZ3Expr(Value(k, keyType), ctx) as Expr<Sort>
            @Suppress("UNCHECKED_CAST")
            val valExpr = valueType.toZ3Expr(Value(v, valueType), ctx) as Expr<Sort>
            @Suppress("UNCHECKED_CAST")
            arr = ctx.mkStore(arr, keyExpr, valExpr) as ArrayExpr<Sort, Sort>
            @Suppress("UNCHECKED_CAST")
            keys = ctx.mkSetAdd(keys, keyExpr) as ArrayExpr<Sort, BoolSort>
        }
        return ctx.mkApp(meta.constructorDecl, arr, keys, ctx.mkInt(map.size))
    }

    override fun fromZ3Expr(expr: Expr<*>, model: Model): Any {
        val ctx = model.julangContext()
        val meta = cellMetadata(ctx)
        val cell = model.eval(expr, true)
        val arrExpr = model.eval(ctx.mkApp(meta.arrAccessor, cell), true)
        @Suppress("UNCHECKED_CAST")
        val keysExpr = model.eval(ctx.mkApp(meta.keysAccessor, cell), true) as ArrayExpr<Sort, BoolSort>
        val result = mutableMapOf<Any, Any>()
        when (keyType) {
            is StringType -> {
                for (decl in model.constDecls) {
                    if (decl.range == ctx.stringSort) {
                        val candidate = decl.name.toString().trim('"')
                        val keyExpr = ctx.mkString(candidate)
                        if (model.eval(ctx.mkSetMemberAny(keyExpr, keysExpr), true).isTrue) {
                            val valExpr = model.eval(
                                mapSelectExpr(ctx, arrExpr, keyExpr),
                                true,
                            )
                            result[candidate] = valueType.fromZ3Expr(valExpr, model)
                        }
                    }
                }
            }
            is IntType -> {
                for (i in -50..50) {
                    val keyExpr = ctx.mkInt(i)
                    if (model.eval(ctx.mkSetMemberAny(keyExpr, keysExpr), true).isTrue) {
                        val valExpr = model.eval(
                            mapSelectExpr(ctx, arrExpr, keyExpr),
                            true,
                        )
                        result[i] = valueType.fromZ3Expr(valExpr, model)
                    }
                }
            }
            else -> throw RuntimeException("Map fromZ3Expr not implemented for key type $keyType")
        }
        return result
    }

    override fun isOfType(obj: Any): Boolean {
        if (obj !is Map<*, *>) return false
        return obj.all { (k, v) ->
            k != null && v != null && keyType.isOfType(k) && valueType.isOfType(v)
        }
    }

    override fun toString(): String = "Map<${keyType}, ${valueType}>"

    private fun defaultValueExpr(ctx: Context): Expr<*> = when (valueType) {
        is IntType -> ctx.mkInt(0)
        is BoolType -> ctx.mkFalse()
        is RealType -> ctx.mkReal(0)
        is StringType -> ctx.mkString("")
        is ListType -> valueType.toZ3Expr(Value(emptyList<Any>(), valueType), ctx)
        is SetType -> valueType.toZ3Expr(Value(emptySet<Any>(), valueType), ctx)
        is MapType -> valueType.toZ3Expr(Value(emptyMap<Any, Any>(), valueType), ctx)
        is ObjClassType -> {
            val fields = valueType.fields.map { field ->
                field.type.toZ3Expr(Value(defaultForType(field.type), field.type), ctx)
            }
            ctx.mkApp(valueType.constructorDecl(ctx), *fields.toTypedArray())
        }
        is TypeVar -> throw RuntimeException("TypeVar in map default value")
        else -> throw RuntimeException("Cannot build default for type $valueType")
    }

    private fun defaultForType(type: Type): Any = when (type) {
        is IntType -> 0
        is BoolType -> false
        is RealType -> 0.0
        is StringType -> ""
        is ListType -> emptyList<Any>()
        is SetType -> emptySet<Any>()
        is MapType -> emptyMap<Any, Any>()
        is ObjClassType -> throw RuntimeException("Cannot default o-class $type")
        is TypeVar -> throw RuntimeException("TypeVar default")
        else -> throw RuntimeException("Cannot default $type")
    }
}

fun mapType(key: Type, value: Type): MapType = MapType(key, value)
