package julay.concurrency

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.Optional
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/**
 * Select must exit promptly when a SyncCase's SyncChannel closes, so Julay procs can rebuild
 * cases (e.g. fall back from a dedicated session channel to the global first-contact channel).
 */
class SelectCloseTest {

    @Test
    fun closeWhileWaitingMultiArmExitsSelectWithoutHanging() = runBlocking {
        withContext(Dispatchers.Default) {
            val c1 = SyncChannel<Int, Int>(2) { Optional.of(1) }
            val c2 = SyncChannel<Int, Int>(2) { Optional.of(2) }
            val fired = AtomicBoolean(false)
            val selectJob = async {
                Select(
                    Select.SyncCase(c1) { fired.set(true) },
                    Select.SyncCase(c2) { fired.set(true) },
                ).run()
            }
            awaitParticipantCount(c1, 1)
            awaitParticipantCount(c2, 1)
            c1.close()
            withTimeout(5.seconds) { selectJob.await() }
            assertFalse(fired.get())
            assertEquals(0, c1.participantCountForTests())
            assertEquals(0, c2.participantCountForTests())
        }
    }

    @Test
    fun closeWhileWaitingSingleArmExitsSelect() = runBlocking {
        withContext(Dispatchers.Default) {
            val chan = SyncChannel<Int, Int>(2) { Optional.of(1) }
            val fired = AtomicBoolean(false)
            val selectJob = async {
                Select(Select.SyncCase(chan) { fired.set(true) }).run()
            }
            awaitParticipantCount(chan, 1)
            chan.close()
            withTimeout(5.seconds) { selectJob.await() }
            assertFalse(fired.get())
            assertEquals(0, chan.participantCountForTests())
        }
    }

    @Test
    fun offerOnAlreadyClosedChannelExitsSelectWithOpenOtherArm() = runBlocking {
        withContext(Dispatchers.Default) {
            val closed = SyncChannel<Int, Int>(2) { Optional.of(1) }
            closed.close()
            val never = SyncChannel<Int, Int>(2) { Optional.of(2) }
            val fired = AtomicBoolean(false)
            withTimeout(5.seconds) {
                Select(
                    Select.SyncCase(closed) { fired.set(true) },
                    Select.SyncCase(never) { fired.set(true) },
                ).run()
            }
            assertFalse(fired.get())
            assertEquals(0, never.participantCountForTests())
        }
    }

    @Test
    fun closingSharedChannelUnblocksTwoSelects() = runBlocking {
        withContext(Dispatchers.Default) {
            // Size-2 with compute always empty so two waiters never successfully rendezvous.
            val shared = SyncChannel<Int, Int>(2) { Optional.empty() }
            val fired = AtomicInteger(0)
            val j1 = async {
                Select(Select.SyncCase(shared) { fired.incrementAndGet() }).run()
            }
            val j2 = async {
                Select(Select.SyncCase(shared) { fired.incrementAndGet() }).run()
            }
            awaitParticipantCount(shared, 2)
            shared.close()
            withTimeout(5.seconds) {
                j1.await()
                j2.await()
            }
            assertEquals(0, fired.get())
            assertEquals(0, shared.participantCountForTests())
        }
    }

    @Test
    fun closingUnrelatedChannelDoesNotBreakSuccessfulSelect() = runBlocking {
        withContext(Dispatchers.Default) {
            val winner = SyncChannel<Int, Int>(1) { Optional.of(42) }
            val unrelated = SyncChannel<Int, Int>(2) { Optional.of(99) }
            unrelated.close()
            val got = AtomicInteger(-1)
            withTimeout(5.seconds) {
                Select(Select.SyncCase(winner) { v -> got.set(v) }).run()
            }
            assertEquals(42, got.get())
            assertTrue(unrelated.isClosed())
        }
    }
}
