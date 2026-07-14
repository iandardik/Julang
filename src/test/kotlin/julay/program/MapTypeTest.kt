package julay.program

import io.github.cvc5.Kind
import io.github.cvc5.TermManager
import julay.tools.applySelector
import julay.tools.isSat
import julay.tools.mapSelectExpr
import julay.tools.mkSetMember
import julay.tools.newModelSolver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MapTypeTest {
    @Test
    fun stringIntMapMembershipInModel() {
        val tm = TermManager()
        val mt = mapType(stringType, intType)
        val solver = newModelSolver(tm)
        val v = mt.toSmtTerm(Variable("mp", mt), tm)
        solver.assertFormula(
            tm.mkTerm(Kind.EQUAL, v, mt.toSmtTerm(Value(mapOf("a" to 1, "b" to 2), mt), tm)),
        )
        assertTrue(solver.isSat())
        val meta = mt.cellMetadata(tm)
        val cell = solver.getValue(v)
        val arr = solver.getValue(applySelector(tm, meta.arrSelector, cell))
        val keys = solver.getValue(applySelector(tm, meta.keysSelector, cell))
        assertTrue(solver.getValue(tm.mkSetMember(tm.mkString("a"), keys)).booleanValue)
        assertTrue(solver.getValue(tm.mkSetMember(tm.mkString("b"), keys)).booleanValue)
        assertEquals(
            1,
            intType.fromSmtTerm(
                solver.getValue(mapSelectExpr(tm, arr, tm.mkString("a"))),
                solver,
            ),
        )
        assertEquals(
            2,
            intType.fromSmtTerm(
                solver.getValue(mapSelectExpr(tm, arr, tm.mkString("b"))),
                solver,
            ),
        )
    }

    @Test
    fun emptyStringIntMapRoundTrip() {
        val tm = TermManager()
        val mt = mapType(stringType, intType)
        val solver = newModelSolver(tm)
        val v = mt.toSmtTerm(Variable("mp", mt), tm)
        solver.assertFormula(tm.mkTerm(Kind.EQUAL, v, mt.toSmtTerm(Value(emptyMap<String, Int>(), mt), tm)))
        assertTrue(solver.isSat())
        assertEquals(emptyMap<String, Int>(), mt.fromSmtTerm(v, solver))
    }

    @Test
    fun intIntMapRoundTrip() {
        val tm = TermManager()
        val mt = mapType(intType, intType)
        val value = Value(mapOf(1 to 10, 2 to 20), mt)
        val solver = newModelSolver(tm)
        val v = mt.toSmtTerm(Variable("mp", mt), tm)
        solver.assertFormula(tm.mkTerm(Kind.EQUAL, v, mt.toSmtTerm(value, tm)))
        assertTrue(solver.isSat())
        @Suppress("UNCHECKED_CAST")
        val restored = mt.fromSmtTerm(v, solver) as Map<Int, Int>
        assertEquals(mapOf(1 to 10, 2 to 20), restored)
    }
}
