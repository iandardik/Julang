package julay.concurrency

import com.microsoft.z3.BoolExpr
import com.microsoft.z3.Context
import com.microsoft.z3.Status
import kotlinx.coroutines.*
import kotlin.test.*
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class SyncChannelFormulaTest {
    private val aliveCheckTimeout = 100.milliseconds
    private val gateTimeout = 5.seconds

    private fun translateCompute(): (Set<BoolExpr>) -> Optional<Int> = { constraints ->
        Context().use { ctx ->
            val solver = ctx.mkSolver()
            constraints.forEach { c ->
                solver.add(c.translate(ctx) as BoolExpr)
            }
            if (solver.check() != Status.SATISFIABLE) {
                Optional.empty()
            } else {
                Optional.of(1)
            }
        }
    }

    @Test
    fun testUNSATHangs() = runBlocking {
        withContext(Dispatchers.Default) {
            for (i in 1..10) {
                val incVal = AtomicInteger(0)
                val chan = createChan(incVal)
                val t1 = launch {
                    val ctx = Context()
                    chan.sync(ctx.mkLt(ctx.mkIntConst("x"), ctx.mkInt(0)))
                }
                val t2 = launch {
                    val ctx = Context()
                    chan.sync(ctx.mkGt(ctx.mkIntConst("x"), ctx.mkInt(0)))
                }
                withTimeoutOrNull(aliveCheckTimeout) {
                    t1.join()
                    t2.join()
                }
                assertTrue(t1.isActive)
                assertTrue(t2.isActive)
                t1.cancelAndJoin()
                t2.cancelAndJoin()
            }
        }
    }

    @Test
    fun testUNSATThenSAT() = runBlocking {
        withContext(Dispatchers.Default) {
            for (i in 1..10) {
                val incVal = AtomicInteger(0)
                val chan = createChan(incVal)
                val t1 = launch {
                    val ctx = Context()
                    chan.sync(ctx.mkLt(ctx.mkIntConst("x"), ctx.mkInt(0)))
                }
                val t2 = launch {
                    val ctx = Context()
                    chan.sync(ctx.mkGt(ctx.mkIntConst("x"), ctx.mkInt(0)))
                }
                withTimeoutOrNull(aliveCheckTimeout) {
                    t1.join()
                    t2.join()
                }
                assertTrue(t1.isActive)
                assertTrue(t2.isActive)
                awaitParticipantCount(chan, 2)

                val t3 = launch {
                    val ctx = Context()
                    chan.sync(ctx.mkLt(ctx.mkIntConst("x"), ctx.mkInt(0)))
                }
                val t4 = launch {
                    val ctx = Context()
                    chan.sync(ctx.mkGt(ctx.mkIntConst("x"), ctx.mkInt(0)))
                }
                withTimeout(gateTimeout) {
                    t1.join(); t2.join(); t3.join(); t4.join()
                }
                assertFalse(t1.isActive)
                assertFalse(t2.isActive)
                assertFalse(t3.isActive)
                assertFalse(t4.isActive)
            }
        }
    }

    @Test
    fun testUNSATThenSAT2() = runBlocking {
        withContext(Dispatchers.Default) {
            for (i in 1..10) {
                val incVal = AtomicInteger(0)
                val chan = createChan(incVal)
                val t1 = launch {
                    val ctx = Context()
                    chan.sync(ctx.mkLt(ctx.mkIntConst("x"), ctx.mkInt(0)))
                }
                val t2 = launch {
                    val ctx = Context()
                    chan.sync(ctx.mkGt(ctx.mkIntConst("x"), ctx.mkInt(0)))
                }
                withTimeoutOrNull(aliveCheckTimeout) {
                    t1.join()
                    t2.join()
                }
                assertTrue(t1.isActive)
                assertTrue(t2.isActive)
                awaitParticipantCount(chan, 2)

                val t3 = launch {
                    val ctx = Context()
                    chan.sync(ctx.mkLt(ctx.mkIntConst("x"), ctx.mkInt(0)))
                }
                withTimeout(gateTimeout) {
                    t1.join()
                    t3.join()
                }
                assertFalse(t1.isActive)
                assertTrue(t2.isActive)
                assertFalse(t3.isActive)

                val t4 = launch {
                    val ctx = Context()
                    chan.sync(ctx.mkGt(ctx.mkIntConst("x"), ctx.mkInt(0)))
                }
                withTimeout(gateTimeout) {
                    t2.join()
                    t4.join()
                }
                assertFalse(t2.isActive)
                assertFalse(t4.isActive)
            }
        }
    }

    @Test
    fun testAgainstSpinWaits() = runBlocking {
        withContext(Dispatchers.Default) {
            for (i in 1..10) {
                val incVal = AtomicInteger(0)
                val chan = createChan(incVal)
                val t1 = launch {
                    val ctx = Context()
                    chan.sync(ctx.mkLt(ctx.mkIntConst("x"), ctx.mkInt(0)))
                }
                val t2 = launch {
                    val ctx = Context()
                    chan.sync(ctx.mkGt(ctx.mkIntConst("x"), ctx.mkInt(0)))
                }
                awaitParticipantCount(chan, 2)

                val t3 = launch {
                    val ctx = Context()
                    chan.sync(ctx.mkLt(ctx.mkIntConst("x"), ctx.mkInt(0)))
                }
                val t4 = launch {
                    val ctx = Context()
                    chan.sync(ctx.mkGt(ctx.mkIntConst("x"), ctx.mkInt(0)))
                }
                withTimeout(gateTimeout) {
                    listOf(t1, t2, t3, t4).forEach { it.join() }
                }
                // Single compute per attempt; incompatible pairs still invoke compute (fused sat).
                assertTrue(incVal.get() <= 12)
            }
        }
    }

    @Test
    fun sameAnticonstraintNeverSyncs() = runBlocking {
        withContext(Dispatchers.Default) {
            val chan = SyncChannel<Int, Int>(
                2,
                compute = { Optional.of(1) },
                antisCompatible = { a, b -> a != b },
            )
            val gotSat = AtomicBoolean(false)
            val a = launch {
                val r = chan.sync(Optional.of(0), Optional.of(1), Optional.empty())
                if (r.isPresent) gotSat.set(true)
            }
            awaitParticipantCount(chan, 1)
            val b = launch {
                val r = chan.sync(Optional.of(0), Optional.of(1), Optional.empty())
                if (r.isPresent) gotSat.set(true)
            }
            awaitParticipantCount(chan, 2)
            assertTrue(a.isActive)
            assertTrue(b.isActive)
            withTimeoutOrNull(aliveCheckTimeout) {
                a.join(); b.join()
            }
            assertTrue(a.isActive)
            assertTrue(b.isActive)
            assertFalse(gotSat.get())
            a.cancelAndJoin()
            b.cancelAndJoin()
            assertEquals(0, chan.participantCountForTests())
        }
    }

    @Test
    fun emptyAnticonstraintCanSyncWithAntiPeer() = runBlocking {
        withContext(Dispatchers.Default) {
            val chan = SyncChannel<Int, Int>(
                2,
                compute = { Optional.of(7) },
                antisCompatible = { a, b -> a != b },
            )
            val a = launch {
                val r = chan.sync(Optional.of(0), Optional.of(1), Optional.empty())
                assertTrue(r.isPresent)
            }
            awaitParticipantCount(chan, 1)
            val b = launch {
                val r = chan.sync(Optional.of(0), Optional.empty(), Optional.empty())
                assertTrue(r.isPresent)
            }
            withTimeout(gateTimeout) {
                a.join(); b.join()
            }
            assertEquals(0, chan.participantCountForTests())
            assertFalse(a.isCancelled)
            assertFalse(b.isCancelled)
        }
    }


    /**
     * A and B share the same SyncAnti-style exclusivity and both wait; C (empty anti) syncs
     * with one; D syncs with the leftover. Must not leave a stuck participant.
     */
    @Test
    fun staleCompatiblePeersNotTranslatedAfterSync() = runBlocking {
        withContext(Dispatchers.Default) {
            val chan = SyncChannel<Int, Int>(
                2,
                compute = { Optional.of(1) },
                antisCompatible = { a, b -> a != b },
            )

            fun launchWithAnti(): Job = launch {
                val result = chan.sync(Optional.of(0), Optional.of(1), Optional.empty())
                assertTrue(result.isPresent)
            }

            fun launchNoAnti(): Job = launch {
                val result = chan.sync(Optional.of(0), Optional.empty(), Optional.empty())
                assertTrue(result.isPresent)
            }

            val jobA = launchWithAnti()
            awaitParticipantCount(chan, 1)
            val jobB = launchWithAnti()
            awaitParticipantCount(chan, 2)

            val jobC = launchNoAnti()
            val firstWave = listOf(jobA, jobB, jobC)
            withTimeout(gateTimeout) {
                while (firstWave.count { it.isCompleted } < 2) {
                    yield()
                }
            }
            awaitParticipantCount(chan, 1)

            val jobD = launchNoAnti()
            withTimeout(gateTimeout) {
                firstWave.filter { !it.isCompleted }.forEach { it.join() }
                jobD.join()
            }

            assertTrue(firstWave.all { it.isCompleted })
            assertTrue(firstWave.none { it.isCancelled })
            assertTrue(jobD.isCompleted)
            assertFalse(jobD.isCancelled)
        }
    }

    /**
     * cancelAndJoin of a waiter must scrub it even when another peer holds the channel mutex
     * path that parks inside [compute] (outside lock the peer is still registered; scrub waits
     * for NonCancellable cleanup). Gates only — no sleep-based synchronization.
     */
    @Test
    fun cancelWhileMutexHeldScrubsParticipant() = runBlocking {
        withContext(Dispatchers.Default) {
            val computeEntered = CompletableDeferred<Unit>()
            val computeRelease = CountDownLatch(1)
            val parkedCompute = AtomicBoolean(false)

            val chan = SyncChannel<BoolExpr, Int>(
                2,
                compute = { constraints ->
                    if (constraints.size == 2 && parkedCompute.compareAndSet(false, true)) {
                        computeEntered.complete(Unit)
                        computeRelease.await()
                        // Incompatible so H waits rather than completing with W.
                        return@SyncChannel Optional.empty()
                    }
                    if (constraints.isEmpty()) {
                        return@SyncChannel Optional.of(1)
                    }
                    Context().use { ctx ->
                        val solver = ctx.mkSolver()
                        constraints.forEach { c ->
                            solver.add(c.translate(ctx) as BoolExpr)
                        }
                        if (solver.check() != Status.SATISFIABLE) {
                            Optional.empty()
                        } else {
                            Optional.of(1)
                        }
                    }
                },
            )

            val ctxW = Context()
            val wJob = launch {
                chan.sync(ctxW.mkTrue())
            }
            awaitParticipantCount(chan, 1)

            val ctxH = Context()
            val hJob = launch {
                try {
                    chan.sync(ctxH.mkTrue())
                } finally {
                    ctxH.close()
                }
            }

            withTimeout(gateTimeout) { computeEntered.await() }

            val joinW = async { wJob.cancelAndJoin() }
            assertFalse(joinW.isCompleted)

            computeRelease.countDown()
            withTimeout(gateTimeout) { joinW.await() }

            // After empty compute, H rejects W and both may still be waiting, or W scrubbed.
            // W must be scrubbed by cancel.
            assertTrue(chan.participantCountForTests() <= 1)
            ctxW.close()

            // Ensure H can still complete with a fresh peer.
            if (chan.participantCountForTests() == 1) {
                val dJob = launch {
                    val ctxD = Context()
                    try {
                        val result = chan.sync(ctxD.mkTrue())
                        assertTrue(result.isPresent)
                    } finally {
                        ctxD.close()
                    }
                }
                withTimeout(gateTimeout) {
                    hJob.join()
                    dJob.join()
                }
                assertEquals(0, chan.participantCountForTests())
                assertFalse(hJob.isCancelled)
                assertFalse(dJob.isCancelled)
            } else {
                hJob.cancelAndJoin()
            }
        }
    }

    /**
     * Size-2 fuses sat into a single [compute] — no separate satisfiable pre-check on constraints.
     */
    @Test
    fun size2SingleComputeFusesSatAndValue() = runBlocking {
        withContext(Dispatchers.Default) {
            val satCalls = AtomicInteger(0)
            val computeCalls = AtomicInteger(0)
            val chan = SyncChannel<BoolExpr, Int>(
                2,
                satisfiable = { constraints ->
                    satCalls.incrementAndGet()
                    Context().use { ctx ->
                        val solver = ctx.mkSolver()
                        constraints.forEach { c -> solver.add(c.translate(ctx) as BoolExpr) }
                        solver.check() == Status.SATISFIABLE
                    }
                },
                compute = { constraints ->
                    computeCalls.incrementAndGet()
                    Context().use { ctx ->
                        val solver = ctx.mkSolver()
                        constraints.forEach { c -> solver.add(c.translate(ctx) as BoolExpr) }
                        if (solver.check() != Status.SATISFIABLE) {
                            Optional.empty()
                        } else {
                            Optional.of(1)
                        }
                    }
                },
            )

            val results = mutableListOf<Int>()
            val a = launch {
                val ctx = Context()
                try {
                    val r = chan.sync(ctx.mkEq(ctx.mkIntConst("x"), ctx.mkInt(7)))
                    assertTrue(r.isPresent)
                    synchronized(results) { results.add(r.result.get()) }
                } finally {
                    ctx.close()
                }
            }
            val b = launch {
                val ctx = Context()
                try {
                    val r = chan.sync(ctx.mkEq(ctx.mkIntConst("x"), ctx.mkInt(7)))
                    assertTrue(r.isPresent)
                    synchronized(results) { results.add(r.result.get()) }
                } finally {
                    ctx.close()
                }
            }
            withTimeout(gateTimeout) {
                a.join(); b.join()
            }
            assertEquals(0, satCalls.get())
            assertEquals(1, computeCalls.get())
            assertEquals(listOf(1, 1), results.sorted())
            assertEquals(0, chan.participantCountForTests())
        }
    }


    private fun createChan(incVal: AtomicInteger): SyncChannel<BoolExpr, Int> {
        fun constraintsSatisfiable(constraints: Set<BoolExpr>): Boolean =
            Context().use { ctx ->
                val solver = ctx.mkSolver()
                constraints.forEach { c -> solver.add(c.translate(ctx) as BoolExpr) }
                solver.check() == Status.SATISFIABLE
            }
        return SyncChannel(
            2,
            satisfiable = ::constraintsSatisfiable,
            compute = { constraints ->
                val i = incVal.getAndIncrement()
                Context().use { ctx ->
                    val solver = ctx.mkSolver()
                    constraints.forEach { c -> solver.add(c.translate(ctx) as BoolExpr) }
                    if (solver.check() == Status.SATISFIABLE) Optional.of(i) else Optional.empty()
                }
            },
        )
    }
}