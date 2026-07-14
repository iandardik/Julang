package julay.program

import io.github.cvc5.Kind
import io.github.cvc5.Solver
import io.github.cvc5.Sort
import io.github.cvc5.Term
import io.github.cvc5.TermManager
import julay.compiler.decl.mangleTypeForName
import julay.tools.applyConstructor
import julay.tools.mkSetAdd
import julay.tools.mkSetMember

class SetCellMetadata(
    val sort: Sort,
    val constructorTerm: Term,
    val arrSelector: Term,
    val sizeSelector: Term,
    val setSort: Sort,
    val domainSort: Sort,
)

data class SetType(val elementType: Type) : Type {
    val cellName: String = "SetCell_${mangleTypeForName(elementType)}"
    val serializedDatatype: SerializedDatatype = buildDatatypeDeclare(
        cellName,
        "mk-$cellName",
        listOf(
            "arr" to "(Set ${elementType.toSmtLibSort()})",
            "size" to "Int",
        ),
    )

    fun cellMetadata(tm: TermManager): SetCellMetadata =
        DatatypeBinder.forTm(tm).setCell(cellName) {
            val domain = elementType.toSmtSort(tm)
            val setSort = tm.mkSetSort(domain)
            val decl = tm.mkDatatypeDecl(cellName)
            val constructor = tm.mkDatatypeConstructorDecl("mk-$cellName")
            constructor.addSelector("arr", setSort)
            constructor.addSelector("size", tm.integerSort)
            decl.addConstructor(constructor)
            val sort = tm.mkDatatypeSort(decl)
            val dt = sort.datatype
            val ctor = dt.getConstructor(0)
            SetCellMetadata(
                sort = sort,
                constructorTerm = ctor.term,
                arrSelector = ctor.getSelector(0).term,
                sizeSelector = ctor.getSelector(1).term,
                setSort = setSort,
                domainSort = domain,
            )
        }

    override fun toSmtTerm(variable: Variable, tm: TermManager): Term =
        tm.mkConst(cellMetadata(tm).sort, variable.name)

    override fun toSmtTerm(value: Value, tm: TermManager): Term {
        @Suppress("UNCHECKED_CAST")
        val elements = value.value as Set<Any>
        val meta = cellMetadata(tm)
        var arr = tm.mkEmptySet(meta.setSort)
        for (elem in elements) {
            arr = tm.mkSetAdd(arr, elementType.toSmtTerm(Value(elem, elementType), tm))
        }
        return applyConstructor(
            tm,
            meta.constructorTerm,
            arrayOf(arr, tm.mkInteger(elements.size.toLong())),
        )
    }

    override fun fromSmtTerm(expr: Term, solver: Solver): Any {
        val tm = solver.termManager
        // Prefer constructor children: solver.termManager is a distinct Java wrapper from the
        // TermManager used to declare the datatype, so APPLY_SELECTOR via cellMetadata fails.
        val cell = solver.getValue(expr)
        val arrExpr = if (cell.kind == Kind.APPLY_CONSTRUCTOR && cell.numChildren >= 2) {
            cell.getChild(1)
        } else {
            throw RuntimeException("Set fromSmtTerm expected APPLY_CONSTRUCTOR cell, got ${cell.kind}")
        }
        return when (elementType) {
            is IntType -> {
                val result = mutableSetOf<Int>()
                for (i in -50..50) {
                    val member = solver.getValue(tm.mkSetMember(tm.mkInteger(i.toLong()), arrExpr))
                    if (member.isBooleanValue && member.booleanValue) {
                        result.add(i)
                    }
                }
                result
            }
            is StringType -> {
                // String domain is infinite; callers should assert SET_MEMBER for known literals.
                emptySet<String>()
            }
            else -> throw RuntimeException("Set fromSmtTerm not implemented for element type $elementType")
        }
    }

    override fun isOfType(obj: Any): Boolean {
        if (obj !is Set<*>) return false
        return obj.all { it != null && elementType.isOfType(it) }
    }

    override fun toString(): String = "Set<${elementType}>"
}

fun setType(element: Type): SetType = SetType(element)
