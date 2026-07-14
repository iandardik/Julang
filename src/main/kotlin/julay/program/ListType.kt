package julay.program

import io.github.cvc5.Kind
import io.github.cvc5.Solver
import io.github.cvc5.Sort
import io.github.cvc5.Term
import io.github.cvc5.TermManager
import julay.tools.mkSeqLength
import julay.tools.mkSeqNth

/**
 * Built-in parametric list type, backed by CVC5 sequences and Kotlin [List] at runtime.
 */
data class ListType(val elementType: Type) : Type {
    fun sort(tm: TermManager): Sort =
        DatatypeBinder.forTm(tm).seqSort("Seq_${elementType}") {
            tm.mkSequenceSort(elementType.toSmtSort(tm))
        }

    override fun toSmtTerm(variable: Variable, tm: TermManager): Term =
        tm.mkConst(sort(tm), variable.name)

    override fun toSmtTerm(value: Value, tm: TermManager): Term {
        @Suppress("UNCHECKED_CAST")
        val elements = value.value as List<Any>
        if (elements.isEmpty()) {
            // mkEmptySequence takes the *element* sort, not the sequence sort.
            return tm.mkEmptySequence(elementType.toSmtSort(tm))
        }
        val units = elements.map { elem ->
            tm.mkTerm(Kind.SEQ_UNIT, elementType.toSmtTerm(Value(elem, elementType), tm))
        }
        return units.reduce { a, b -> tm.mkTerm(Kind.SEQ_CONCAT, a, b) }
    }

    override fun fromSmtTerm(expr: Term, solver: Solver): Any {
        val tm = solver.termManager
        val seq = solver.getValue(expr)
        val len = intType.fromSmtTerm(tm.mkSeqLength(seq), solver) as Int
        return (0 until len).map { i ->
            val elemExpr = solver.getValue(tm.mkSeqNth(seq, tm.mkInteger(i.toLong())))
            elementType.fromSmtTerm(elemExpr, solver)
        }
    }

    override fun isOfType(obj: Any): Boolean {
        if (obj !is List<*>) return false
        return obj.all { it != null && elementType.isOfType(it) }
    }

    override fun toString(): String = "List<${elementType}>"
}

fun listType(element: Type): ListType = ListType(element)

fun Type.toSmtSort(tm: TermManager): Sort = when (this) {
    is BoolType -> tm.booleanSort
    is IntType -> tm.integerSort
    is RealType -> tm.realSort
    is StringType -> tm.stringSort
    is ObjClassType -> sort(tm)
    is ListType -> sort(tm)
    is SetType -> cellMetadata(tm).sort
    is MapType -> cellMetadata(tm).sort
    is TypeVar -> throw RuntimeException("TypeVar \"$name\" must not reach SMT sort construction")
    else -> throw RuntimeException("Cannot build SMT sort for type $this")
}
