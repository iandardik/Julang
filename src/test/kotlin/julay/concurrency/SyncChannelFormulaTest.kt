package julay.concurrency

import com.microsoft.z3.BoolExpr
import com.microsoft.z3.Context
import com.microsoft.z3.Status
import kotlinx.coroutines.*
import kotlin.test.*
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class SyncChannelFormulaTest {
    private val aliveCheckTimeout = 100L
    private val notAliveCheckTimeout = 1000L

    @Test
    fun testUNSATHangs() = runBlocking {
        val incVal = AtomicInteger(0)
        val chan = createChan(incVal)
        val (t1,t2) = createUNSATThreads(chan)
        delay(aliveCheckTimeout)
        assertTrue(t1.isActive)
        assertTrue(t2.isActive)
    }

    @Test
    fun testUNSATWithoutRetryModeDoesntHang() {
        /*
        runBlocking {
            val incVal = AtomicInteger(0)
            val chan = createChan(incVal)
            val (t1,t2) = createUNSATThreads(chan, false)
            delay(notAliveCheckTimeout)
            assertTrue(!t1.isActive)
            assertTrue(!t2.isActive)
        }*/
    }

    @Test
    fun testUNSATThenSAT() {
        /*
        runBlocking {
            val incVal = AtomicInteger(0)
            val chan = createChan(incVal)
            val (t1,t2) = createUNSATThreads(chan)
            delay(aliveCheckTimeout)
            assertTrue(t1.isActive)
            assertTrue(t2.isActive)

            val (t3,t4) = createUNSATThreads(chan)
            delay(notAliveCheckTimeout)
            assertTrue(!t1.isActive)
            assertTrue(!t2.isActive)
            assertTrue(!t3.isActive)
            assertTrue(!t4.isActive)
        }*/
    }

    @Test
    fun testUNSATThenSAT2() {
        /*
        runBlocking {
            val incVal = AtomicInteger(0)
            val chan = createChan(incVal)
            val (t1,t2) = createUNSATThreads(chan)
            delay(aliveCheckTimeout)
            assertTrue(t1.isActive)
            assertTrue(t2.isActive)

            val (t3,t4) = createUNSATThreads(chan)
            delay(notAliveCheckTimeout)
            assertTrue(!t1.isActive)
            assertTrue(t2.isActive)
            assertTrue(!t3.isActive)

            delay(notAliveCheckTimeout)
            assertTrue(!t2.isActive)
            assertTrue(!t4.isActive)
        }*/
    }

    @Test
    fun testAgainstSpinWaits() {
        /*
        runBlocking {
            val incVal = AtomicInteger(0)
            val chan = createChan(incVal)
            val (t1,t2) = createUNSATThreads(chan)
            delay(aliveCheckTimeout)

            val (t3,t4) = createUNSATThreads(chan)
            delay(aliveCheckTimeout)

            // the actual number of times <compute> will be invoked is nondeterministic, but it should be relatively low,
            // e.g., under 15 times.
            assertTrue(incVal.get() <= 15)
        }*/
    }


    private suspend fun createUNSATThreads(chan : SyncChannel<Int, BoolExpr>, retry : Boolean = true) : List<Job> {
        var t1 : Job? = null
        var t2 : Job? = null
        coroutineScope {
            t1 = launch {
                val ctx = Context()
                chan.sync(ctx.mkLt(ctx.mkIntConst("x"),ctx.mkInt(0))) //, retryOnUNSAT = retry)
            }
            t2 = launch {
                val ctx = Context()
                chan.sync(ctx.mkGt(ctx.mkIntConst("x"),ctx.mkInt(0))) //, retryOnUNSAT = retry)
            }
        }
        return listOf(t1!!,t2!!)
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