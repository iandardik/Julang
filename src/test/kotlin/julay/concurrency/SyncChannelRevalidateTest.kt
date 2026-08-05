package julay.concurrency

import julay.program.Constraint
import julay.program.sync.BoolExprFast
import julay.program.sync.SyncAnti
import julay.program.sync.SyncResolveFast
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/**
 * Edge cases for size-1/2 SyncChannel: compute-outside-mutex, SyncAnti, empty compute.
 */
class SyncChannelRevalidateTest {

    private val gateTimeout = 5.seconds

    private fun antiOk(a: Constraint, b: Constraint): Boolean {
        val aa = a.anti
        val bb = b.anti
        return aa == null || bb == null || !SyncResolveFast.antiSatisfiable(listOf(aa, bb))
    }

    @Test
    fun size1SelfCommit_success() = runBlocking {
        val chan = SyncChannel<Int, Int>(1) { Optional.of(99) }
        val r = chan.sync()
        assertTrue(r.isPresent)
        assertEquals(99, r.result.get())
        assertEquals(0, chan.participantCountForTests())
    }

    @Test
    fun size1SelfCommit_mutexReleasedDuringCompute() = runBlocking {
        withContext(Dispatchers.Default) {
            val entered = CompletableDeferred<Unit>()
            val release = CountDownLatch(1)
            val sawMutexFree = AtomicInteger(0)
            val chan = SyncChannel<Int, Int>(1) {
                entered.complete(Unit)
                release.await()
                Optional.of(1)
            }
            val job = launch { chan.sync() }
            entered.await()
            // compute runs outside the channel mutex
            if (chan.mutexAvailableForTests()) {
                sawMutexFree.incrementAndGet()
            }
            release.countDown()
            withTimeout(gateTimeout) { job.join() }
            assertEquals(1, sawMutexFree.get())
            assertEquals(0, chan.participantCountForTests())
        }
    }

    @Test
    fun size1SelfCommit_computeEmptyAborts() = runBlocking {
        val chan = SyncChannel<Int, Int>(1) { Optional.empty() }
        val r = chan.sync()
        assertTrue(r.isEmpty)
        assertEquals(0, chan.participantCountForTests())
    }

    @Test
    fun size2ComputeEmptyDoesNotCommit() = runBlocking {
        withContext(Dispatchers.Default) {
            val computeCalls = AtomicInteger(0)
            val chan = SyncChannel<Int, Int>(2) {
                computeCalls.incrementAndGet()
                Optional.empty()
            }
            val a = launch { chan.sync() }
            val b = launch { chan.sync() }
            awaitParticipantCount(chan, 2)
            delay(50)
            assertTrue(a.isActive)
            assertTrue(b.isActive)
            assertTrue(computeCalls.get() >= 1)
            a.cancelAndJoin()
            b.cancelAndJoin()
            assertEquals(0, chan.participantCountForTests())
        }
    }

    @Test
    fun size2AnticonstraintFilterSkipsCompute() = runBlocking {
        withContext(Dispatchers.Default) {
            val computeCalls = AtomicInteger(0)
            val chan = SyncChannel(
                2,
                compute = { computeCalls.incrementAndGet(); Optional.of(1) },
                antisCompatible = ::antiOk,
            )
            val anti = Constraint(anti = SyncAnti.ClassId(1))
            val guard = Constraint(fast = BoolExprFast.True)
            val a = launch {
                chan.sync(Optional.of(guard), Optional.of(anti), Optional.empty())
            }
            val b = launch {
                chan.sync(Optional.of(guard), Optional.of(anti), Optional.empty())
            }
            awaitParticipantCount(chan, 2)
            delay(50)
            assertEquals(0, computeCalls.get())
            a.cancelAndJoin()
            b.cancelAndJoin()
        }
    }

    @Test
    fun syncAnti_classIdDifferentCanSync() = runBlocking {
        val chan = SyncChannel(
            2,
            compute = { Optional.of(1) },
            antisCompatible = ::antiOk,
        )
        val a = async {
            chan.sync(
                Optional.of(Constraint(fast = BoolExprFast.True)),
                Optional.of(Constraint(anti = SyncAnti.ClassId(1))),
                Optional.empty(),
            )
        }
        val b = async {
            chan.sync(
                Optional.of(Constraint(fast = BoolExprFast.True)),
                Optional.of(Constraint(anti = SyncAnti.ClassId(2))),
                Optional.empty(),
            )
        }
        withTimeout(gateTimeout) {
            assertTrue(a.await().isPresent)
            assertTrue(b.await().isPresent)
        }
    }

    @Test
    fun syncAnti_providerClientPairCanSync() = runBlocking {
        val chan = SyncChannel(
            2,
            compute = { Optional.of(1) },
            antisCompatible = ::antiOk,
        )
        val a = async {
            chan.sync(
                Optional.of(Constraint(fast = BoolExprFast.True)),
                Optional.of(Constraint(anti = SyncAnti.ProviderClient(true))),
                Optional.empty(),
            )
        }
        val b = async {
            chan.sync(
                Optional.of(Constraint(fast = BoolExprFast.True)),
                Optional.of(Constraint(anti = SyncAnti.ProviderClient(false))),
                Optional.empty(),
            )
        }
        withTimeout(gateTimeout) {
            assertTrue(a.await().isPresent)
            assertTrue(b.await().isPresent)
        }
    }

    @Test
    fun syncAnti_twoProvidersNeverSync() = runBlocking {
        withContext(Dispatchers.Default) {
            val chan = SyncChannel(
                2,
                compute = { Optional.of(1) },
                antisCompatible = ::antiOk,
            )
            val anti = Constraint(anti = SyncAnti.ProviderClient(true))
            val a = launch {
                chan.sync(Optional.of(Constraint(fast = BoolExprFast.True)), Optional.of(anti), Optional.empty())
            }
            val b = launch {
                chan.sync(Optional.of(Constraint(fast = BoolExprFast.True)), Optional.of(anti), Optional.empty())
            }
            awaitParticipantCount(chan, 2)
            delay(50)
            assertTrue(a.isActive && b.isActive)
            a.cancelAndJoin()
            b.cancelAndJoin()
        }
    }

    @Test
    fun size2ComputeOutside_channelClosedDuringCompute() = runBlocking {
        withContext(Dispatchers.Default) {
            val entered = CompletableDeferred<Unit>()
            val release = CountDownLatch(1)
            val chan = SyncChannel<Int, Int>(2) {
                entered.complete(Unit)
                release.await()
                Optional.of(1)
            }
            val a = launch { chan.sync() }
            awaitParticipantCount(chan, 1)
            val b = launch { chan.sync() }
            entered.await()
            chan.close()
            release.countDown()
            withTimeout(gateTimeout) {
                a.join(); b.join()
            }
            assertEquals(0, chan.participantCountForTests())
        }
    }

    @Test
    fun size2DirectSyncVsSelectPeer() = runBlocking {
        withContext(Dispatchers.Default) {
            val chan = SyncChannel<Int, Int>(2) { Optional.of(42) }
            val got = AtomicInteger(-1)
            val direct = launch {
                val r = chan.sync(Optional.empty(), Optional.empty(), Optional.empty())
                assertTrue(r.isPresent)
                got.compareAndSet(-1, r.result.get())
            }
            val selectJob = launch {
                Select(Select.SyncCase(chan) { v -> got.compareAndSet(-1, v) }).run()
            }
            withTimeout(gateTimeout) {
                direct.join()
                selectJob.join()
            }
            assertEquals(42, got.get())
            assertEquals(0, chan.participantCountForTests())
        }
    }

    @Test
    fun selectEmptyCasesReturnsImmediately() = runBlocking {
        Select().run()
    }
}
