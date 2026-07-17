package julay.program

import com.microsoft.z3.Context
import julay.program.library.JulHttpClient
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ChannelTest {
    @Test
    fun emptyChannelCannotSync() {
        val empty = Channel.empty("receiveResponse")
        assertTrue(empty.isEmpty())
        assertTrue(empty.isClosed())
        assertEquals("receiveResponse", empty.actionName)
        assertFailsWith<IllegalStateException> {
            empty.requireOpenSyncChannel()
        }
    }

    @Test
    fun closedChannelThrowsOnSync() = runBlocking {
        val program = Program(setOf(JulHttpClient.staticInfo()))
        val ch = program.createDynamicChannel(JulHttpClient.receiveResponseAct)
        assertEquals(1, program.openDynamicChannelCount(JulHttpClient.receiveResponseAct))
        closeChannel(ch)
        assertEquals(0, program.openDynamicChannelCount(JulHttpClient.receiveResponseAct))
        assertFailsWith<IllegalStateException> {
            ch.requireOpenSyncChannel()
        }
        Unit
    }

    @Test
    fun constraintBagDecodesChannelId() {
        val program = Program(setOf(JulHttpClient.staticInfo()))
        val ch = program.createDynamicChannel(JulHttpClient.receiveResponseAct)
        val byId = mapOf(ch.id to ch)
        Context().use { ctx ->
            val expr = ctx.mkInt(ch.id.toInt())
            // Dummy model — ChannelType.fromZ3Expr only needs the IntNum expr for live ids.
            val model = ctx.mkSolver().let { s ->
                s.add(ctx.mkTrue())
                check(s.check() == com.microsoft.z3.Status.SATISFIABLE)
                s.model
            }
            ChannelType.withChannelLookup(byId) {
                val decoded = channelType("receiveResponse").fromZ3Expr(expr, model)
                assertEquals(ch, decoded)
            }
        }
        assertEquals(Channel.EMPTY_ID, Channel.empty("receiveResponse").id)
        runBlocking { closeChannel(ch) }
    }

    @Test
    fun createChannelByName() = runBlocking {
        val program = Program(setOf(JulHttpClient.staticInfo()))
        val ch = program.createDynamicChannel("closeHttpClient")
        assertEquals("closeHttpClient", ch.actionName)
        assertEquals(1, program.openDynamicChannelCount(JulHttpClient.closeHttpClientAct))
        closeChannel(ch)
        assertEquals(0, program.openDynamicChannelCount(JulHttpClient.closeHttpClientAct))
    }

    @Test
    fun channelTypeBrandsDiffer() {
        assertTrue(channelType("sendResponse") != channelType("receiveResponse"))
        assertEquals(channelType("sendResponse"), channelType("sendResponse"))
    }
}
