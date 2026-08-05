package julay.concurrency

import julay.program.Constraint
import julay.program.action.TSAction
import julay.program.sync.BoolExprFast
import julay.program.sync.SyncAnti
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/** Select / mixed-size / race edge cases for the compute-outside SyncChannel protocol. */
class SelectEdgeTest {

    private val gateTimeout = 5.seconds

    @Test
    fun selectWinnerDuringSlowPeerCompute() = runBlocking {
        withContext(Dispatchers.Default) {
            // Size-2 arm never commits (empty compute); size-1 self-commits and wins Select.
            val slow = SyncChannel<Int, Int>(2) { Optional.empty() }
            val fast = SyncChannel<Int, Int>(1) { Optional.of(42) }
            val got = AtomicInteger(-1)
            val peer = launch { slow.sync() }
            awaitParticipantCount(slow, 1)
            withTimeout(gateTimeout) {
                Select(
                    Select.SyncCase(slow) { got.set(it) },
                    Select.SyncCase(fast) { got.set(it) },
                ).run()
            }
            assertEquals(42, got.get())
            withTimeout(gateTimeout) { peer.cancelAndJoin() }
            assertEquals(0, slow.participantCountForTests())
        }
    }

    @Test
    fun selectAllArmsAbortWithoutWinner() = runBlocking {
        withContext(Dispatchers.Default) {
            val c1 = SyncChannel<Int, Int>(2) { Optional.of(1) }
            val c2 = SyncChannel<Int, Int>(2) { Optional.of(2) }
            val fired = AtomicInteger(0)
            val selectJob = async {
                Select(
                    Select.SyncCase(c1) { fired.incrementAndGet() },
                    Select.SyncCase(c2) { fired.incrementAndGet() },
                ).run()
            }
            awaitParticipantCount(c1, 1)
            awaitParticipantCount(c2, 1)
            c1.close()
            c2.close()
            withTimeout(gateTimeout) { selectJob.await() }
            assertEquals(0, fired.get())
        }
    }

    @Test
    fun selectTwoSingleCaseSelectsOnSize2() = runBlocking {
        withContext(Dispatchers.Default) {
            val chan = SyncChannel<Int, Int>(2) { Optional.of(7) }
            val a = AtomicInteger(0)
            val b = AtomicInteger(0)
            val j1 = launch { Select(Select.SyncCase(chan) { a.set(it) }).run() }
            val j2 = launch { Select(Select.SyncCase(chan) { b.set(it) }).run() }
            withTimeout(gateTimeout) {
                j1.join(); j2.join()
            }
            assertEquals(7, a.get())
            assertEquals(7, b.get())
            assertEquals(0, chan.participantCountForTests())
        }
    }

    @Test
    fun selectSize1ArmWinsSize2LoserScrubs() = runBlocking {
        withContext(Dispatchers.Default) {
            val size1 = SyncChannel<Int, Int>(1) { Optional.of(1) }
            // Empty compute so the size-2 arm cannot beat size-1 via peer rendezvous.
            val size2 = SyncChannel<Int, Int>(2) { Optional.empty() }
            val got = AtomicInteger(-1)
            val peer = launch { size2.sync() }
            awaitParticipantCount(size2, 1)
            withTimeout(gateTimeout) {
                Select(
                    Select.SyncCase(size1) { got.set(it) },
                    Select.SyncCase(size2) { got.set(it) },
                ).run()
            }
            assertEquals(1, got.get())
            peer.cancelAndJoin()
            assertEquals(0, size2.participantCountForTests())
        }
    }

    @Test
    fun singleOfferDirectSync_pairsWithSelectPeer() = runBlocking {
        withContext(Dispatchers.Default) {
            val chan = SyncChannel<Int, Int>(2) { Optional.of(5) }
            val directGot = AtomicInteger(-1)
            val selectGot = AtomicInteger(-1)
            val direct = launch {
                Select.SyncCase(chan) { directGot.set(it) }.syncDirect()
            }
            val selectJob = launch {
                Select(Select.SyncCase(chan) { selectGot.set(it) }).run()
            }
            withTimeout(gateTimeout) {
                direct.join(); selectJob.join()
            }
            assertTrue(directGot.get() == 5 || selectGot.get() == 5)
            assertEquals(0, chan.participantCountForTests())
        }
    }

    @Test
    fun multiOfferStillUsesSelect_loserScrubs() = runBlocking {
        withContext(Dispatchers.Default) {
            val win = SyncChannel<Int, Int>(1) { Optional.of(1) }
            val lose = SyncChannel<Int, Int>(2) { Optional.of(2) }
            val peer = launch { lose.sync() }
            awaitParticipantCount(lose, 1)
            val fired = AtomicInteger(0)
            withTimeout(gateTimeout) {
                Select(
                    Select.SyncCase(win) { fired.incrementAndGet() },
                    Select.SyncCase(lose) { fired.incrementAndGet() },
                ).run()
            }
            assertEquals(1, fired.get())
            peer.cancelAndJoin()
            assertEquals(0, lose.participantCountForTests())
        }
    }

    @Test
    fun size2TwoLeadersRaceComputeOneWins() = runBlocking {
        withContext(Dispatchers.Default) {
            val computeCalls = AtomicInteger(0)
            val chan = SyncChannel<Int, Int>(2) {
                computeCalls.incrementAndGet()
                Optional.of(1)
            }
            val results = Collections.synchronizedList(mutableListOf<Boolean>())
            val jobs = (1..4).map {
                launch {
                    results.add(chan.sync().isPresent)
                }
            }
            withTimeout(gateTimeout) { jobs.forEach { it.join() } }
            assertEquals(4, results.count { it })
            assertEquals(0, chan.participantCountForTests())
            // Two successful pairs → two computes (fused sat+value).
            assertEquals(2, computeCalls.get())
        }
    }

    @Test
    fun size2NextPeerAfterFailedCompute() = runBlocking {
        withContext(Dispatchers.Default) {
            val chan = SyncChannel<Int, Int>(2) { constraints ->
                // Pair (1,2) is never sat; (2,3) is.
                if (constraints == setOf(2, 3)) Optional.of(9) else Optional.empty()
            }
            val stuck = launch { chan.sync(1) }
            awaitParticipantCount(chan, 1)
            val leader = async { chan.sync(2) }
            delay(30)
            val good = async { chan.sync(3) }
            withTimeout(gateTimeout) {
                assertTrue(leader.await().isPresent)
                assertTrue(good.await().isPresent)
            }
            stuck.cancelAndJoin()
            assertEquals(0, chan.participantCountForTests())
        }
    }

    @Test
    fun size2ComputeOutside_peerCancelsDuringCompute() = runBlocking {
        withContext(Dispatchers.Default) {
            val entered = CompletableDeferred<Unit>()
            val release = CountDownLatch(1)
            val chan = SyncChannel<Int, Int>(2) {
                entered.complete(Unit)
                release.await()
                Optional.of(1)
            }
            val peer = launch { chan.sync() }
            awaitParticipantCount(chan, 1)
            val leader = launch { chan.sync() }
            entered.await()
            peer.cancelAndJoin()
            release.countDown()
            // Leader's revalidate fails; left waiting alone — scrub via close.
            withTimeout(gateTimeout) {
                chan.close()
                leader.join()
            }
            assertEquals(0, chan.participantCountForTests())
        }
    }

    @Test
    fun closeIdempotentDuringSuccessfulSyncRace() = runBlocking {
        withContext(Dispatchers.Default) {
            repeat(50) {
                val chan = SyncChannel<Int, Int>(2) { Optional.of(1) }
                val a = launch { chan.sync() }
                val b = launch { chan.sync() }
                launch { chan.close() }
                withTimeout(gateTimeout) {
                    a.join(); b.join()
                    chan.close()
                }
                assertTrue(chan.isClosed())
                assertEquals(0, chan.participantCountForTests())
            }
        }
    }

    @Test
    fun syncAnti_emptyWithAntiPeerCanSync() = runBlocking {
        val chan = SyncChannel<Constraint, Int>(
            2,
            compute = { Optional.of(1) },
            antisCompatible = { a, b ->
                val aa = a.anti
                val bb = b.anti
                aa == null || bb == null || aa != bb
            },
        )
        val withAnti = async {
            chan.sync(
                Optional.of(Constraint(fast = BoolExprFast.True)),
                Optional.of(Constraint(anti = SyncAnti.ClassId(1))),
                Optional.empty(),
            )
        }
        val noAnti = async {
            chan.sync(
                Optional.of(Constraint(fast = BoolExprFast.True)),
                Optional.empty(),
                Optional.empty(),
            )
        }
        withTimeout(gateTimeout) {
            assertTrue(withAnti.await().isPresent)
            assertTrue(noAnti.await().isPresent)
        }
    }

    @Test
    fun needsZ3Path_antiHasNoBoolExpr() {
        // Mirrors Proc NeedsZ3 anticonstraint construction: SyncAnti only, expr null.
        val anti = Constraint(
            anti = SyncAnti.fromRole(TSAction.SyncRole.Default, 7),
        )
        assertEquals(null, anti.expr)
        assertTrue(anti.anti is SyncAnti.ClassId)
    }
}
