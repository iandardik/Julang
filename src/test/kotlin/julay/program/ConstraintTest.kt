package julay.program

import com.microsoft.z3.Context
import com.microsoft.z3.julangContext
import kotlin.test.Test
import kotlin.test.assertEquals
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
}
