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
                // the actual number of times <compute> will be invoked is nondeterministic, but it should be relatively low,
                // e.g., under 15 times.
                assertTrue(incVal.get() <= 15)
            }
        }
    }

    @Test
    fun sameAnticonstraintNeverSyncs() = runBlocking {
        withContext(Dispatchers.Default) {
            val chan = SyncChannel(2, translateCompute())
            fun classAnti(ctx: Context) =
                ctx.mkEq(ctx.mkIntConst("classID"), ctx.mkInt(1))

            val ctxA = Context()
            val ctxB = Context()
            val gotSat = AtomicBoolean(false)
            val a = launch {
                try {
                    val r = chan.sync(
                        Optional.of(ctxA.mkTrue()),
                        Optional.of(classAnti(ctxA)),
                        Optional.empty(),
                    )
                    if (r.isPresent) gotSat.set(true)
                } finally {
                    ctxA.close()
                }
            }
            awaitParticipantCount(chan, 1)
            val b = launch {
                try {
                    val r = chan.sync(
                        Optional.of(ctxB.mkTrue()),
                        Optional.of(classAnti(ctxB)),
                        Optional.empty(),
                    )
                    if (r.isPresent) gotSat.set(true)
                } finally {
                    ctxB.close()
                }
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
            val chan = SyncChannel(2, translateCompute())
            val ctxA = Context()
            val anti = ctxA.mkEq(ctxA.mkIntConst("classID"), ctxA.mkInt(1))
            val a = launch {
                try {
                    val r = chan.sync(
                        Optional.of(ctxA.mkTrue()),
                        Optional.of(anti),
                        Optional.empty(),
                    )
                    assertTrue(r.isPresent)
                } finally {
                    ctxA.close()
                }
            }
            awaitParticipantCount(chan, 1)
            val b = launch {
                val ctxB = Context()
                try {
                    val r = chan.sync(ctxB.mkTrue())
                    assertTrue(r.isPresent)
                } finally {
                    ctxB.close()
                }
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
     * removeParticipants must scrub departed peers from leftover waiters' compatiblePeers.
     *
     * Size-2 + mutually SAT waiters can never stack (the 2nd arrival syncs immediately).
     * Instead A and B share a SAT anticonstraint so they are incompatible and both wait;
     * C (empty anti) is compatible with both, syncs with one, and leaves the other holding
     * C in compatiblePeers if cleanup is buggy. After A/C Contexts close, D syncing with the
     * leftover must not translate those closed ASTs.
     */
    @Test
    fun staleCompatiblePeersNotTranslatedAfterSync() = runBlocking {
        withContext(Dispatchers.Default) {
            val chan = SyncChannel(2, translateCompute())

            fun classAnti(ctx: Context): BoolExpr =
                ctx.mkEq(ctx.mkIntConst("classID"), ctx.mkInt(1))

            fun launchWithAnti(): Job = launch {
                val ctx = Context()
                try {
                    val result = chan.sync(
                        Optional.of(ctx.mkTrue()),
                        Optional.of(classAnti(ctx)),
                        Optional.empty(),
                    )
                    assertTrue(result.isPresent)
                } finally {
                    ctx.close()
                }
            }

            fun launchNoAnti(): Job = launch {
                val ctx = Context()
                try {
                    val result = chan.sync(ctx.mkTrue())
                    assertTrue(result.isPresent)
                } finally {
                    ctx.close()
                }
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
     * inside compute (pairwiseSatisfiable). Without NonCancellable [removeSelfAfterAbort],
     * cleanup can be skipped while waiting for the lock; after Context.close a later peer
     * then translates a dead AST (Z3 "Context closed" / invalid ast).
     *
     * Gates only — no sleep-based synchronization.
     */
    @Test
    fun cancelWhileMutexHeldScrubsParticipant() = runBlocking {
        withContext(Dispatchers.Default) {
            val computeEntered = CompletableDeferred<Unit>()
            // Latch (not Deferred.await): compute is a blocking callback, not a suspend function.
            val computeRelease = CountDownLatch(1)
            val parkedPairwise = AtomicBoolean(false)

            val chan = SyncChannel<Int, BoolExpr>(2) { constraints ->
                if (constraints.size == 2 && parkedPairwise.compareAndSet(false, true)) {
                    computeEntered.complete(Unit)
                    computeRelease.await()
                    // Incompatible so H waits rather than leading a sync that removes W itself.
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
            }

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
            // NonCancellable scrub waits on the mutex H still holds inside compute.
            // Do not call participantCountForTests here — it takes the same mutex and would deadlock
            // with compute parked under withLock.
            assertFalse(joinW.isCompleted)

            computeRelease.countDown()
            withTimeout(gateTimeout) { joinW.await() }

            // W scrubbed; H remains as the lone waiter.
            // Without NonCancellable cleanup, W would still be registered (count == 2).
            assertEquals(1, chan.participantCountForTests())
            ctxW.close()

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
        }
    }

    private fun createChan(incVal : AtomicInteger) : SyncChannel<Int, BoolExpr> {
        val chanCtx = Context()
        val chan = SyncChannel<Int, BoolExpr>(2) { constraints ->
            val i = incVal.getAndIncrement()
            val solver = chanCtx.mkSolver()
            constraints.forEach { c -> solver.add(c.translate(chanCtx)) }
            if (solver.check() == Status.SATISFIABLE) {
                Optional.of(i)
            } else {
                Optional.empty()
            }
        }
        return chan
    }
}