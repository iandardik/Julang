package julay.program

import com.microsoft.z3.Context
import com.microsoft.z3.julangContext
import julay.program.sync.BoolExprFast
import julay.program.sync.SyncAnti
import julay.program.sync.SyncGround
import julay.program.sync.SyncTerm
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ConstraintTest {
    @Test
    fun cloneIntoMovesExprIntoTargetContextAndPreservesProcessMetadata() {
        Context().use { source ->
            val original = Constraint(source.mkTrue(), procId = 7L, classId = 42, proc = null)
            Context().use { target ->
                val cloned = original.cloneInto(target)
                assertSame(target, cloned.expr!!.julangContext())
                assertEquals(7L, cloned.procId)
                assertEquals(42, cloned.classId)
                assertEquals(null, cloned.proc)
                assertTrue(cloned.expr!!.isTrue)
            }
        }
    }

    @Test
    fun fillFastAndFillAntiReuseShell() {
        val shell = Constraint(fast = BoolExprFast.True)
        val guard = BoolExprFast.Eq(
            SyncTerm.Arg("x", SyncTerm.Arg.Sort.Int),
            SyncTerm.Ground(SyncGround.IntVal(1)),
        )
        shell.fillFast(guard, procId = 3L, classId = 9, proc = null)
        assertSame(guard, shell.fast)
        assertNull(shell.expr)
        assertNull(shell.anti)
        assertEquals(3L, shell.procId)
        assertEquals(9, shell.classId)

        val anti = SyncAnti.ClassId(9)
        shell.fillAnti(anti, procId = 3L, classId = 9, proc = null)
        assertSame(anti, shell.anti)
        assertNull(shell.fast)
        assertNull(shell.expr)
        assertEquals(3L, shell.procId)
    }
}
