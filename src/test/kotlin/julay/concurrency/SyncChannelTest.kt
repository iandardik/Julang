package julay.concurrency

import kotlinx.coroutines.*
import kotlin.test.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds

class SyncChannelTest {

    @Test
    fun test1Channel1Sync() {
        businessLogic1Channel(1, 10_000)
    }

    @Test
    fun test1Channel2Sync() {
        businessLogic1Channel(2, 10_000)
    }

    @Test
    fun test1Channel3Sync() {
        businessLogic1Channel(3, 9_999)
    }

    @Test
    fun test1Channel4Sync() {
        businessLogic1Channel(4, 10_000)
    }

    private fun businessLogic1Channel(syncSize : Int, numThreads : Int) {
        // if this is false then some threads will hang
        julay.tools.assert(numThreads % syncSize == 0)

        val incVal = AtomicInteger(1)
        val results = ConcurrentHashMap<Int,Int>() // value -> count
        val chan = SyncChannel<Int,Int>(syncSize) { Optional.of(incVal.getAndIncrement()) }
        runBlocking {
            withContext(Dispatchers.Default) {
                for (i in 1.. numThreads) {
                    launch {
                        val syncResult = chan.sync()
                        // keep track of how many threads have sync'ed on this value
                        results.compute(syncResult.result.get(), chmResultUpdate)
                    }
                }
            }
        }

        // testing
        val maxKey = numThreads / syncSize
        assertEquals(results.size, maxKey)
        for (i in 1..maxKey) {
            assertTrue(results.containsKey(i))
            assertEquals(results[i], syncSize)
        }
    }

    @Test
    fun test2Channels1Sync() {
        businessLogic2Channels(1, 10_000)
    }

    @Test
    fun test2Channels2Sync() {
        businessLogic2Channels(2, 10_000)
    }

    @Test
    fun test2Channels3Sync() {
        businessLogic2Channels(3, 9_999)
    }

    @Test
    fun test2Channels4Sync() {
        businessLogic2Channels(4, 10_000)
    }

    private fun businessLogic2Channels(syncSize : Int, numThreads : Int) {
        // if this is false then some threads will hang
        julay.tools.assert(numThreads % syncSize == 0)

        val incVal = AtomicInteger(1)
        val results = ConcurrentHashMap<Int,Int>() // value -> count
        val chan1 = SyncChannel<Int,Int>(syncSize) { Optional.of(incVal.getAndIncrement()) }
        val chan2 = SyncChannel<Int,Int>(syncSize) { Optional.of(incVal.getAndIncrement()) }
        runBlocking {
            withContext(Dispatchers.Default) {
                for (i in 1..numThreads) {
                    launch {
                        val syncResult = chan1.sync()
                        results.compute(syncResult.result.get(), chmResultUpdate)
                    }
                    launch {
                        val syncResult = chan2.sync()
                        results.compute(syncResult.result.get(), chmResultUpdate)
                    }
                }
            }
        }

        // testing
        val maxKey = 2*numThreads / syncSize
        assertEquals(results.size, maxKey)
        for (i in 1..maxKey) {
            assertTrue(results.containsKey(i))
            assertEquals(results[i], syncSize)
        }
    }

    private val chmResultUpdate : (Int,Int?)->Int? = {
        _, curVal ->
            if (curVal == null) {
                1
            } else {
                curVal + 1
            }
    }

    @Test
    fun testClose1() = runBlocking {
        withContext(Dispatchers.Default) {
            for (i in 1..10_000) {
                val chan = SyncChannel<Unit, Unit>(2) { Optional.empty() }
                val t1 = launch { chan.sync() }
                assertTrue(t1.isActive)
                chan.close()
                withTimeout(100.milliseconds) {
                    t1.join()
                }
                assertFalse(t1.isActive)
            }
        }
    }

    @Test
    fun testClose2() = runBlocking {
        withContext(Dispatchers.Default) {
            for (i in 1..100) {
                val chan = SyncChannel<Int, Int>(2) { Optional.of(0) }
                val t1 = launch { chan.sync() }
                val t2 = launch { chan.sync() }
                val t3 = launch { chan.sync() }
                delay(2)
                // two threads will have synced, so exactly one must be alive
                assertTrue(t1.isActive || t2.isActive || t3.isActive)
                assertTrue((!t1.isActive && !t2.isActive) || (!t1.isActive && !t3.isActive) || (!t2.isActive && !t3.isActive))

                chan.close()
                delay(2)
                assertFalse(t1.isActive)
                assertFalse(t2.isActive)
                assertFalse(t3.isActive)
            }
        }
    }

    @Test
    fun testCancel1() = runBlocking {
        withContext(Dispatchers.Default) {
            for (i in 1..100) {
                val chan = SyncChannel<Unit, Unit>(2) { Optional.empty() }
                val t1 = launch { chan.sync() }
                assertTrue(t1.isActive)
                t1.cancel()
                assertFalse(t1.isActive)
            }
        }
    }

    @Test
    fun testCancel2() = runBlocking {
        withContext(Dispatchers.Default) {
            for (i in 1..100) {
                val chan = SyncChannel<Int, Int>(2) { Optional.of(0) }
                val t1 = launch { chan.sync() }
                val t2 = launch { chan.sync() }
                val t3 = launch { chan.sync() }
                delay(4.milliseconds)
                // two threads will have synced, so exactly one must be alive
                assertTrue(t1.isActive || t2.isActive || t3.isActive)
                assertTrue((!t1.isActive && !t2.isActive) || (!t1.isActive && !t3.isActive) || (!t2.isActive && !t3.isActive))

                t1.cancel()
                t2.cancel()
                t3.cancel()
                delay(4.milliseconds)
                assertFalse(t1.isActive)
                assertFalse(t2.isActive)
                assertFalse(t3.isActive)
            }
        }
    }
}