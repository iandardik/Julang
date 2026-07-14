package julay.program

import io.github.cvc5.Kind
import io.github.cvc5.TermManager
import julay.tools.isSat
import julay.tools.newModelSolver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ListTypeTest {
    @Test
    fun nestedListFromSmtTermViaModel() {
        val tm = TermManager()
        val nested = listType(listType(intType))
        val value = Value(listOf(listOf(1, 2), listOf(3)), nested)
        val solver = newModelSolver(tm)
        val v = nested.toSmtTerm(Variable("xs", nested), tm)
        solver.assertFormula(tm.mkTerm(Kind.EQUAL, v, nested.toSmtTerm(value, tm)))
        assertTrue(solver.isSat())
        val restored = nested.fromSmtTerm(v, solver)
        assertEquals(listOf(listOf(1, 2), listOf(3)), restored)
    }

    @Test
    fun emptyStringListRoundTrip() {
        val tm = TermManager()
        val lt = listType(stringType)
        val solver = newModelSolver(tm)
        val v = lt.toSmtTerm(Variable("lst", lt), tm)
        solver.assertFormula(tm.mkTerm(Kind.EQUAL, v, lt.toSmtTerm(Value(emptyList<String>(), lt), tm)))
        assertTrue(solver.isSat())
        assertEquals(emptyList<String>(), lt.fromSmtTerm(v, solver))
    }

    @Test
    fun emptyIntListRoundTrip() {
        val tm = TermManager()
        val lt = listType(intType)
        val solver = newModelSolver(tm)
        val v = lt.toSmtTerm(Variable("lst", lt), tm)
        solver.assertFormula(tm.mkTerm(Kind.EQUAL, v, lt.toSmtTerm(Value(emptyList<Int>(), lt), tm)))
        assertTrue(solver.isSat())
        assertEquals(emptyList<Int>(), lt.fromSmtTerm(v, solver))
    }
}
