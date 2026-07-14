package julay.concurrency

import io.github.cvc5.Kind
import io.github.cvc5.TermManager
import julay.tools.SmtConstraint
import julay.tools.constraintsAreSat
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
        withContext(Dispatchers.Default) {
            for (i in 1..10) {
                val incVal = AtomicInteger(0)
                val chan = createChan(incVal)
                val t1 = launch {
                    chan.sync(mkLtX(0))
                }
                val t2 = launch {
                    chan.sync(mkGtX(0))
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
                    chan.sync(mkLtX(0))
                }
                val t2 = launch {
                    chan.sync(mkGtX(0))
                }
                withTimeoutOrNull(aliveCheckTimeout) {
                    t1.join()
                    t2.join()
                }
                // two UNSAT threads should still be active
                assertTrue(t1.isActive)
                assertTrue(t2.isActive)

                val t3 = launch {
                    chan.sync(mkLtX(0))
                }
                val t4 = launch {
                    chan.sync(mkGtX(0))
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
                    chan.sync(mkLtX(0))
                }
                val t2 = launch {
                    chan.sync(mkGtX(0))
                }
                withTimeoutOrNull(aliveCheckTimeout) {
                    t1.join()
                    t2.join()
                }
                assertTrue(t1.isActive)
                assertTrue(t2.isActive)

                val t3 = launch {
                    chan.sync(mkLtX(0))
                }
                delay(notAliveCheckTimeout)
                assertFalse(t1.isActive)
                assertTrue(t2.isActive)
                assertFalse(t3.isActive)

                val t4 = launch {
                    chan.sync(mkGtX(0))
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
                    chan.sync(mkLtX(0))
                }
                val t2 = launch {
                    chan.sync(mkGtX(0))
                }
                delay(aliveCheckTimeout)

                val t3 = launch {
                    chan.sync(mkLtX(0))
                }
                val t4 = launch {
                    chan.sync(mkGtX(0))
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

    private fun mkLtX(n: Long): SmtConstraint {
        val tm = TermManager()
        val x = tm.mkConst(tm.integerSort, "x")
        return SmtConstraint.from(tm.mkTerm(Kind.LT, x, tm.mkInteger(n)))
    }

    private fun mkGtX(n: Long): SmtConstraint {
        val tm = TermManager()
        val x = tm.mkConst(tm.integerSort, "x")
        return SmtConstraint.from(tm.mkTerm(Kind.GT, x, tm.mkInteger(n)))
    }

    private fun createChan(incVal: AtomicInteger): SyncChannel<Int, SmtConstraint> {
        return SyncChannel(2) { constraints ->
            val i = incVal.getAndIncrement()
            if (constraintsAreSat(constraints)) {
                Optional.of(i)
            } else {
                Optional.empty()
            }
        }
    }
}
