package julay.concurrency

import com.microsoft.z3.BoolExpr
import com.microsoft.z3.Context
import com.microsoft.z3.Status
import kotlinx.coroutines.*
import kotlin.test.*
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class SyncChannelFormulaTest {
    private val aliveCheckTimeout = 100.milliseconds
    private val notAliveCheckTimeout = 100.milliseconds
    private val gateTimeout = 5.seconds

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
                t1.cancel()
                t2.cancel()
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
                // two UNSAT threads should still be active
                assertTrue(t1.isActive)
                assertTrue(t2.isActive)

                val t3 = launch {
                    val ctx = Context()
                    chan.sync(ctx.mkLt(ctx.mkIntConst("x"), ctx.mkInt(0)))
                }
                val t4 = launch {
                    val ctx = Context()
                    chan.sync(ctx.mkGt(ctx.mkIntConst("x"), ctx.mkInt(0)))
                }
                delay(notAliveCheckTimeout)
                // the threads should have synced
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

                val t3 = launch {
                    val ctx = Context()
                    chan.sync(ctx.mkLt(ctx.mkIntConst("x"), ctx.mkInt(0)))
                }
                delay(notAliveCheckTimeout)
                assertFalse(t1.isActive)
                assertTrue(t2.isActive)
                assertFalse(t3.isActive)

                val t4 = launch {
                    val ctx = Context()
                    chan.sync(ctx.mkGt(ctx.mkIntConst("x"), ctx.mkInt(0)))
                }
                delay(notAliveCheckTimeout)
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
                delay(aliveCheckTimeout)

                val t3 = launch {
                    val ctx = Context()
                    chan.sync(ctx.mkLt(ctx.mkIntConst("x"), ctx.mkInt(0)))
                }
                val t4 = launch {
                    val ctx = Context()
                    chan.sync(ctx.mkGt(ctx.mkIntConst("x"), ctx.mkInt(0)))
                }
                delay(aliveCheckTimeout)

                // the actual number of times <compute> will be invoked is nondeterministic, but it should be relatively low,
                // e.g., under 15 times.
                assertTrue(incVal.get() <= 15)

                t1.cancel()
                t2.cancel()
                t3.cancel()
                t4.cancel()
            }
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
            val chan = SyncChannel<Int, BoolExpr>(2) { constraints ->
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

    private suspend fun awaitParticipantCount(
        chan: SyncChannel<*, *>,
        expected: Int,
        timeout: Duration = gateTimeout,
    ) {
        withTimeout(timeout) {
            while (chan.participantCountForTests() != expected) {
                yield()
            }
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