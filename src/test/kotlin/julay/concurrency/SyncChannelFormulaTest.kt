package julay.concurrency

import com.microsoft.z3.BoolExpr
import com.microsoft.z3.Context
import com.microsoft.z3.Status
import kotlinx.coroutines.*
import kotlin.test.*
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds

class SyncChannelFormulaTest {
    private val aliveCheckTimeout = 100.milliseconds
    private val notAliveCheckTimeout = 100.milliseconds

    @Test
    fun testUNSATHangs() = runBlocking {
        val incVal = AtomicInteger(0)
        val chan = createChan(incVal)
        val t1 = launch {
            val ctx = Context()
            chan.sync(ctx.mkLt(ctx.mkIntConst("x"),ctx.mkInt(0)))
        }
        val t2 = launch {
            val ctx = Context()
            chan.sync(ctx.mkGt(ctx.mkIntConst("x"),ctx.mkInt(0)))
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

    @Test
    fun testUNSATThenSAT() = runBlocking {
        val incVal = AtomicInteger(0)
        val chan = createChan(incVal)
        val t1 = launch {
            val ctx = Context()
            chan.sync(ctx.mkLt(ctx.mkIntConst("x"),ctx.mkInt(0)))
        }
        val t2 = launch {
            val ctx = Context()
            chan.sync(ctx.mkGt(ctx.mkIntConst("x"),ctx.mkInt(0)))
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
            chan.sync(ctx.mkLt(ctx.mkIntConst("x"),ctx.mkInt(0)))
        }
        val t4 = launch {
            val ctx = Context()
            chan.sync(ctx.mkGt(ctx.mkIntConst("x"),ctx.mkInt(0)))
        }
        delay(notAliveCheckTimeout)
        // the threads should have synced
        assertFalse(t1.isActive)
        assertFalse(t2.isActive)
        assertFalse(t3.isActive)
        assertFalse(t4.isActive)
    }

    @Test
    fun testUNSATThenSAT2() = runBlocking {
        val incVal = AtomicInteger(0)
        val chan = createChan(incVal)
        val t1 = launch {
            val ctx = Context()
            chan.sync(ctx.mkLt(ctx.mkIntConst("x"),ctx.mkInt(0))) //, retryOnUNSAT = retry)
        }
        val t2 = launch {
            val ctx = Context()
            chan.sync(ctx.mkGt(ctx.mkIntConst("x"),ctx.mkInt(0))) //, retryOnUNSAT = retry)
        }
        withTimeoutOrNull(aliveCheckTimeout) {
            t1.join()
            t2.join()
        }
        assertTrue(t1.isActive)
        assertTrue(t2.isActive)

        val t3 = launch {
            val ctx = Context()
            chan.sync(ctx.mkLt(ctx.mkIntConst("x"),ctx.mkInt(0))) //, retryOnUNSAT = retry)
        }
        delay(notAliveCheckTimeout)
        assertFalse(t1.isActive)
        assertTrue(t2.isActive)
        assertFalse(t3.isActive)

        val t4 = launch {
            val ctx = Context()
            chan.sync(ctx.mkGt(ctx.mkIntConst("x"),ctx.mkInt(0))) //, retryOnUNSAT = retry)
        }
        delay(notAliveCheckTimeout)
        assertFalse(t2.isActive)
        assertFalse(t4.isActive)
    }

    @Test
    fun testAgainstSpinWaits() = runBlocking {
        val incVal = AtomicInteger(0)
        val chan = createChan(incVal)
        val t1 = launch {
            val ctx = Context()
            chan.sync(ctx.mkLt(ctx.mkIntConst("x"),ctx.mkInt(0))) //, retryOnUNSAT = retry)
        }
        val t2 = launch {
            val ctx = Context()
            chan.sync(ctx.mkGt(ctx.mkIntConst("x"),ctx.mkInt(0))) //, retryOnUNSAT = retry)
        }
        delay(aliveCheckTimeout)

        val t3 = launch {
            val ctx = Context()
            chan.sync(ctx.mkLt(ctx.mkIntConst("x"),ctx.mkInt(0))) //, retryOnUNSAT = retry)
        }
        val t4 = launch {
            val ctx = Context()
            chan.sync(ctx.mkGt(ctx.mkIntConst("x"),ctx.mkInt(0))) //, retryOnUNSAT = retry)
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