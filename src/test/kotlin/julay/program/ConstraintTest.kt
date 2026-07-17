package julay.program

import com.microsoft.z3.Context
import com.microsoft.z3.julangContext
import julay.program.library.JulHttpClient
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ConstraintTest {
    @Test
    fun cloneIntoMovesExprIntoTargetContextAndPreservesChannels() = runBlocking {
        val program = Program(setOf(JulHttpClient.staticInfo()))
        val ch = program.createDynamicChannel(JulHttpClient.receiveResponseAct)
        Context().use { source ->
            val original = Constraint(source.mkTrue(), setOf(ch))
            Context().use { target ->
                val cloned = original.cloneInto(target)
                assertSame(target, cloned.expr.julangContext())
                assertEquals(setOf(ch), cloned.channels)
                assertTrue(cloned.expr.isTrue)
            }
        }
        closeChannel(ch)
    }
}
