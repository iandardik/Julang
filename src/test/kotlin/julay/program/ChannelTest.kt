package julay.program

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
    fun lookupByIdRoundTrip() = runBlocking {
        val program = Program(setOf(JulHttpClient.staticInfo()))
        val ch = program.createDynamicChannel(JulHttpClient.receiveResponseAct)
        assertEquals(ch, program.lookupDynamicChannel("receiveResponse", ch.id))
        assertEquals(Channel.EMPTY_ID, Channel.empty("receiveResponse").id)
        closeChannel(ch)
        Unit
    }

    @Test
    fun channelTypeBrandsDiffer() {
        assertTrue(channelType("sendResponse") != channelType("receiveResponse"))
        assertEquals(channelType("sendResponse"), channelType("sendResponse"))
    }
}
