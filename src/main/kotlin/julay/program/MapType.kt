package julay.program

import io.github.cvc5.Kind
import io.github.cvc5.Solver
import io.github.cvc5.Sort
import io.github.cvc5.Term
import io.github.cvc5.TermManager
import julay.compiler.decl.mangleTypeForName
import julay.tools.applyConstructor
import julay.tools.mapSelectExpr
import julay.tools.mkSetAdd
import julay.tools.mkSetMember

class MapCellMetadata(
    val sort: Sort,
    val constructorTerm: Term,
    val arrSelector: Term,
    val keysSelector: Term,
    val sizeSelector: Term,
    val arraySort: Sort,
    val keySetSort: Sort,
    val domainSort: Sort,
)

data class MapType(val keyType: Type, val valueType: Type) : Type {
    val cellName: String = "MapCell_${mangleTypeForName(keyType)}_${mangleTypeForName(valueType)}"
    val serializedDatatype: SerializedDatatype = buildDatatypeDeclare(
        cellName,
        "mk-$cellName",
        listOf(
            "arr" to "(Array ${keyType.toSmtLibSort()} ${valueType.toSmtLibSort()})",
            "keys" to "(Set ${keyType.toSmtLibSort()})",
            "size" to "Int",
        ),
    )

    fun cellMetadata(tm: TermManager): MapCellMetadata =
        DatatypeBinder.forTm(tm).mapCell(cellName) {
            val keySort = keyType.toSmtSort(tm)
            val valueSort = valueType.toSmtSort(tm)
            val arraySort = tm.mkArraySort(keySort, valueSort)
            val keySetSort = tm.mkSetSort(keySort)
            val decl = tm.mkDatatypeDecl(cellName)
            val constructor = tm.mkDatatypeConstructorDecl("mk-$cellName")
            constructor.addSelector("arr", arraySort)
            constructor.addSelector("keys", keySetSort)
            constructor.addSelector("size", tm.integerSort)
            decl.addConstructor(constructor)
            val sort = tm.mkDatatypeSort(decl)
            val dt = sort.datatype
            val ctor = dt.getConstructor(0)
            MapCellMetadata(
                sort = sort,
                constructorTerm = ctor.term,
                arrSelector = ctor.getSelector(0).term,
                keysSelector = ctor.getSelector(1).term,
                sizeSelector = ctor.getSelector(2).term,
                arraySort = arraySort,
                keySetSort = keySetSort,
                domainSort = keySort,
            )
        }

    override fun toSmtTerm(variable: Variable, tm: TermManager): Term =
        tm.mkConst(cellMetadata(tm).sort, variable.name)

    override fun toSmtTerm(value: Value, tm: TermManager): Term {
        @Suppress("UNCHECKED_CAST")
        val map = value.value as Map<Any, Any>
        val meta = cellMetadata(tm)
        var arr = tm.mkConstArray(meta.arraySort, defaultValueTerm(tm))
        var keys = tm.mkEmptySet(meta.keySetSort)
        for ((k, v) in map) {
            val keyExpr = keyType.toSmtTerm(Value(k, keyType), tm)
            val valExpr = valueType.toSmtTerm(Value(v, valueType), tm)
            arr = tm.mkTerm(Kind.STORE, arr, keyExpr, valExpr)
            keys = tm.mkSetAdd(keys, keyExpr)
        }
        return applyConstructor(
            tm,
            meta.constructorTerm,
            arrayOf(arr, keys, tm.mkInteger(map.size.toLong())),
        )
    }

    override fun fromSmtTerm(expr: Term, solver: Solver): Any {
        val tm = solver.termManager
        // Prefer constructor children: solver.termManager is a distinct Java wrapper from the
        // TermManager used to declare the datatype, so APPLY_SELECTOR via cellMetadata fails.
        val cell = solver.getValue(expr)
        if (cell.kind != Kind.APPLY_CONSTRUCTOR || cell.numChildren < 3) {
            throw RuntimeException("Map fromSmtTerm expected APPLY_CONSTRUCTOR cell, got ${cell.kind}")
        }
        val arrExpr = cell.getChild(1)
        val keysExpr = cell.getChild(2)
        val result = mutableMapOf<Any, Any>()
        when (keyType) {
            is StringType -> {
                // String key domain is infinite; callers should assert SELECT for known keys.
            }
            is IntType -> {
                for (i in -50..50) {
                    val keyExpr = tm.mkInteger(i.toLong())
                    val member = solver.getValue(tm.mkSetMember(keyExpr, keysExpr))
                    if (member.isBooleanValue && member.booleanValue) {
                        val valExpr = solver.getValue(mapSelectExpr(tm, arrExpr, keyExpr))
                        result[i] = valueType.fromSmtTerm(valExpr, solver)
                    }
                }
            }
            else -> throw RuntimeException("Map fromSmtTerm not implemented for key type $keyType")
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

    private fun defaultValueTerm(tm: TermManager): Term = when (valueType) {
        is IntType -> tm.mkInteger(0)
        is BoolType -> tm.mkFalse()
        is RealType -> tm.mkReal(0)
        is StringType -> tm.mkString("")
        is ListType -> valueType.toSmtTerm(Value(emptyList<Any>(), valueType), tm)
        is SetType -> valueType.toSmtTerm(Value(emptySet<Any>(), valueType), tm)
        is MapType -> valueType.toSmtTerm(Value(emptyMap<Any, Any>(), valueType), tm)
        is ObjClassType -> {
            val fields = valueType.fields.map { field ->
                field.type.toSmtTerm(Value(defaultForType(field.type), field.type), tm)
            }
            applyConstructor(tm, valueType.constructorTerm(tm), fields.toTypedArray())
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
