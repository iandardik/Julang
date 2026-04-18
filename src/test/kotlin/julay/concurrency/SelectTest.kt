package julay.concurrency

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.test.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class SelectTest {

    @Test
    fun test1Case1Sync() {
        businessLogic1Case(1, 10000)
    }

    @Test
    fun test1Case2Sync() {
        businessLogic1Case(2, 10000)
    }

    @Test
    fun test1Case3Sync() {
        businessLogic1Case(3, 9999)
    }

    @Test
    fun test1Case4Sync() {
        businessLogic1Case(4, 10000)
    }

    private fun businessLogic1Case(syncSize : Int, numThreads : Int) {
        // if this is false then some threads will hang
        julay.tools.assert(numThreads % syncSize == 0)

        val incVal = AtomicInteger(1)
        val results = ConcurrentHashMap<Int,Int>() // value -> count
        val chan = SyncChannel<Int,Int>(syncSize) { Optional.of(incVal.getAndIncrement()) }
        runBlocking {
            withContext(Dispatchers.Default) {
                for (i in 1..numThreads) {
                    launch {
                        Select(
                            Select.SyncCase(chan) { syncResult -> results.compute(syncResult, chmResultUpdate) },
                        ).run()
                    }
                }
            }
        }

        // testing
        val maxKey = numThreads / syncSize
        assertEquals(maxKey, results.size)
        for (i in 1..maxKey) {
            assertTrue(results.containsKey(i))
            assertEquals(syncSize, results[i])
        }
    }

    @Test
    fun test2Case1Sync() {
        businessLogic2Cases(1, 10000)
    }

    @Test
    fun test2Case2Sync() {
        businessLogic2Cases(2, 10000)
    }

    @Test
    fun test2Case3Sync() {
        businessLogic2Cases(3, 9999)
    }

    @Test
    fun test2Case4Sync() {
        businessLogic2Cases(4, 10000)
    }

    private fun businessLogic2Cases(syncSize : Int, numThreads : Int) {
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
                        Select(
                            Select.SyncCase(chan1) { syncResult -> results.compute(syncResult, chmResultUpdate) },
                            Select.SyncCase(chan2) { syncResult -> results.compute(syncResult, chmResultUpdate) },
                        ).run()
                    }
                }
            }
        }

        // testing
        val maxKey = numThreads / syncSize
        assertEquals(maxKey, results.size)
        for (k in results.keys()) {
            assertEquals(syncSize, results[k])
        }
    }

    @Test
    fun test1and2Case1Sync() {
        businessLogic1and2Cases(1, 10000)
    }

    @Test
    fun test1and2Case2Sync() {
        businessLogic1and2Cases(2, 10000)
    }

    @Test
    fun test1and2Case3Sync() {
        businessLogic1and2Cases(3, 9999)
    }

    @Test
    fun test1and2Case4Sync() {
        businessLogic1and2Cases(4, 10000)
    }

    private fun businessLogic1and2Cases(syncSize : Int, numThreads : Int) {
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
                        Select(
                            Select.SyncCase(chan1) { syncResult -> results.compute(syncResult, chmResultUpdate) },
                        ).run()
                        // TODO make a test case for raw channel v selects too
                        //val syncResult = chan1.sync()
                        //results.compute(syncResult.result.get(), chmResultUpdate)
                    }
                    launch {
                        Select(
                            Select.SyncCase(chan1) { syncResult -> results.compute(syncResult, chmResultUpdate) },
                            Select.SyncCase(chan2) { syncResult -> results.compute(syncResult, chmResultUpdate) },
                        ).run()
                    }
                }
            }
        }

        // testing
        val maxKey = 2*numThreads / syncSize
        assertEquals(maxKey, results.size)
        for (k in results.keys()) {
            assertEquals(syncSize, results[k])
        }
    }

    @Test
    fun test3and4Case1Sync() {
        businessLogic3and4Cases(1, 10_000)
    }

    @Test
    fun test3and4Case2Sync() {
        businessLogic3and4Cases(2, 10_000)
    }

    @Test
    fun test3and4Case3Sync() {
        businessLogic3and4Cases(3, 9_999)
    }

    @Test
    fun test3and4Case4Sync() {
        businessLogic3and4Cases(4, 10_000)
    }

    private fun businessLogic3and4Cases(syncSize : Int, numThreads : Int) {
        // if this is false then some threads will hang
        julay.tools.assert(numThreads % syncSize == 0)

        val incVal = AtomicInteger(1)
        val results = ConcurrentHashMap<Int,Int>() // value -> count
        val chan1 = SyncChannel<Int,Int>(syncSize) { Optional.of(incVal.getAndIncrement()) }
        val chan2 = SyncChannel<Int,Int>(syncSize) { Optional.of(incVal.getAndIncrement()) }
        val chan3 = SyncChannel<Int,Int>(syncSize) { Optional.of(incVal.getAndIncrement()) }
        val chan4 = SyncChannel<Int,Int>(syncSize) { Optional.of(incVal.getAndIncrement()) }
        runBlocking {
            withContext(Dispatchers.Default) {
                for (i in 1..numThreads) {
                    launch {
                        Select(
                            Select.SyncCase(chan1) { syncResult -> results.compute(syncResult, chmResultUpdate) },
                            Select.SyncCase(chan2) { syncResult -> results.compute(syncResult, chmResultUpdate) },
                            Select.SyncCase(chan3) { syncResult -> results.compute(syncResult, chmResultUpdate) },
                        ).run()
                    }
                    launch {
                        Select(
                            Select.SyncCase(chan1) { syncResult -> results.compute(syncResult, chmResultUpdate) },
                            Select.SyncCase(chan2) { syncResult -> results.compute(syncResult, chmResultUpdate) },
                            Select.SyncCase(chan3) { syncResult -> results.compute(syncResult, chmResultUpdate) },
                            Select.SyncCase(chan4) { syncResult -> results.compute(syncResult, chmResultUpdate) },
                        ).run()
                    }
                }
            }
        }

        // testing
        val maxKey = 2*numThreads / syncSize
        assertEquals(maxKey, results.size)
        for (k in results.keys()) {
            assertEquals(syncSize, results[k])
        }
    }

    /*
    @Test
    fun testSanityCheckReuseChannel() {
        val chan = SyncChannel<Int,Int>(2) { Optional.of(1) }
        assertThrows {
            Select(
                Select.SyncCase(chan) {},
                Select.SyncCase(chan) {},
            )
        }
    }

    @Test
    fun testSanityCheckReuseCase() {
        val chan = SyncChannel<Int,Int>(2) { Optional.of(1) }
        val case = Select.SyncCase(chan) {}
        Select(case)
        assertThrows {
            Select(case)
        }
    }

    @Test
    fun testSanityCheckRerunCase() {
        val chan = SyncChannel<Int,Int>(1) { Optional.of(1) }
        val select = Select(
            Select.SyncCase(chan) {}
        )
        select.run()
        // the select should only be able to be run once
        assertThrows {
            select.run()
        }
    }
     */

    private val chmResultUpdate : (Int, Int?)->Int? = {
            _, curVal ->
        if (curVal == null) {
            1
        } else {
            curVal + 1
        }
    }
}