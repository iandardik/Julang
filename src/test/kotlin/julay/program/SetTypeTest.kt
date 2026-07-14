package julay.program

import io.github.cvc5.Kind
import io.github.cvc5.TermManager
import julay.tools.applySelector
import julay.tools.isSat
import julay.tools.mkSetMember
import julay.tools.newModelSolver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SetTypeTest {
    @Test
    fun intSetRoundTrip() {
        val tm = TermManager()
        val st = setType(intType)
        val value = Value(setOf(1, 2, 3), st)
        val solver = newModelSolver(tm)
        val v = st.toSmtTerm(Variable("s", st), tm)
        solver.assertFormula(tm.mkTerm(Kind.EQUAL, v, st.toSmtTerm(value, tm)))
        assertTrue(solver.isSat())
        val restored = st.fromSmtTerm(v, solver) as Set<*>
        assertEquals(setOf(1, 2, 3), restored)
    }

    @Test
    fun emptyIntSetRoundTrip() {
        val tm = TermManager()
        val st = setType(intType)
        val solver = newModelSolver(tm)
        val v = st.toSmtTerm(Variable("s", st), tm)
        solver.assertFormula(tm.mkTerm(Kind.EQUAL, v, st.toSmtTerm(Value(emptySet<Int>(), st), tm)))
        assertTrue(solver.isSat())
        assertEquals(emptySet<Int>(), st.fromSmtTerm(v, solver))
    }

    @Test
    fun stringSetMembershipInModel() {
        val tm = TermManager()
        val st = setType(stringType)
        val solver = newModelSolver(tm)
        val v = st.toSmtTerm(Variable("s", st), tm)
        solver.assertFormula(tm.mkTerm(Kind.EQUAL, v, st.toSmtTerm(Value(setOf("a", "b"), st), tm)))
        assertTrue(solver.isSat())
        val meta = st.cellMetadata(tm)
        val cell = solver.getValue(v)
        val arr = solver.getValue(applySelector(tm, meta.arrSelector, cell))
        assertTrue(solver.getValue(tm.mkSetMember(tm.mkString("a"), arr)).booleanValue)
        assertTrue(solver.getValue(tm.mkSetMember(tm.mkString("b"), arr)).booleanValue)
        assertTrue(!solver.getValue(tm.mkSetMember(tm.mkString("c"), arr)).booleanValue)
    }
}
