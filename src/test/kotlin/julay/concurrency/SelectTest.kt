package julay.concurrency

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.test.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.seconds

class SelectTest {

    @Test
    fun test1Case1Sync() {
        businessLogic1Case(1, 2_000)
    }

    @Test
    fun test1Case2Sync() {
        businessLogic1Case(2, 2_000)
    }



    private fun businessLogic1Case(syncSize : Int, numThreads : Int) {
        // if this is false then some threads will hang
        julay.tools.assert(numThreads % syncSize == 0)

        val results = ConcurrentHashMap<Int,Int>() // value -> count
        val chan = SyncChannel<Int,Int>(syncSize) { Optional.of(1) }
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

        assertEquals(numThreads, results.values.sum())
    }

    @Test
    fun test2Case1Sync() {
        businessLogic2Cases(1, 2_000)
    }

    @Test
    fun test2Case2Sync() {
        businessLogic2Cases(2, 2_000)
    }



    private fun businessLogic2Cases(syncSize : Int, numThreads : Int) {
        // if this is false then some threads will hang
        julay.tools.assert(numThreads % syncSize == 0)

        val results = ConcurrentHashMap<Int,Int>() // value -> count
        val chan1 = SyncChannel<Int,Int>(syncSize) { Optional.of(1) }
        val chan2 = SyncChannel<Int,Int>(syncSize) { Optional.of(1) }
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

        assertEquals(numThreads, results.values.sum())
    }

    @Test
    fun test1and2Case1Sync() {
        businessLogic1and2Cases(1, 2_000)
    }

    @Test
    fun test1and2Case2Sync() {
        businessLogic1and2Cases(2, 2_000)
    }



    private fun businessLogic1and2Cases(syncSize : Int, numThreads : Int) {
        // if this is false then some threads will hang
        julay.tools.assert(numThreads % syncSize == 0)

        val results = ConcurrentHashMap<Int,Int>() // value -> count
        val chan1 = SyncChannel<Int,Int>(syncSize) { Optional.of(1) }
        val chan2 = SyncChannel<Int,Int>(syncSize) { Optional.of(1) }
        runBlocking {
            withContext(Dispatchers.Default) {
                for (i in 1..numThreads) {
                    launch {
                        Select(
                            Select.SyncCase(chan1) { syncResult -> results.compute(syncResult, chmResultUpdate) },
                        ).run()
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

        assertEquals(2 * numThreads, results.values.sum())
    }

    @Test
    fun test3and4Case1Sync() {
        businessLogic3and4Cases(1, 2_000)
    }

    @Test
    fun test3and4Case2Sync() {
        businessLogic3and4Cases(2, 2_000)
    }



    private fun businessLogic3and4Cases(syncSize : Int, numThreads : Int) {
        // if this is false then some threads will hang
        julay.tools.assert(numThreads % syncSize == 0)

        val results = ConcurrentHashMap<Int,Int>() // value -> count
        val chan1 = SyncChannel<Int,Int>(syncSize) { Optional.of(1) }
        val chan2 = SyncChannel<Int,Int>(syncSize) { Optional.of(1) }
        val chan3 = SyncChannel<Int,Int>(syncSize) { Optional.of(1) }
        val chan4 = SyncChannel<Int,Int>(syncSize) { Optional.of(1) }
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

        assertEquals(2 * numThreads, results.values.sum())
    }

    @Test
    fun testChanCase1Sync() {
        businessLogicChanCase(1, 2_000)
    }

    @Test
    fun testChanCase2Sync() {
        businessLogicChanCase(2, 2_000)
    }



    private fun businessLogicChanCase(syncSize : Int, numThreads : Int) {
        // if this is false then some threads will hang
        julay.tools.assert(numThreads % syncSize == 0)

        val results = ConcurrentHashMap<Int,Int>() // value -> count
        val chan1 = SyncChannel<Int,Int>(syncSize) { Optional.of(1) }
        val chan2 = SyncChannel<Int,Int>(syncSize) { Optional.of(1) }
        runBlocking {
            withContext(Dispatchers.Default) {
                for (i in 1..numThreads) {
                    launch {
                        val syncResult = chan1.sync()
                        results.compute(syncResult.result.get(), chmResultUpdate)
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

        assertEquals(2 * numThreads, results.values.sum())
    }

    @Test
    fun testSanityCheckReuseChannel() {
        val chan = SyncChannel<Int,Int>(2) { Optional.of(1) }
        try {
            Select(
                Select.SyncCase(chan) {},
                Select.SyncCase(chan) {},
            )
            // an exception should have been thrown before we reach this point
            assertTrue(false, "an exception should have been thrown by the Select")
        }
        catch (_ : RuntimeException) {}
    }

    @Test
    fun testSanityCheckReuseCase() {
        val chan = SyncChannel<Int,Int>(2) { Optional.of(1) }
        val case = Select.SyncCase(chan) {}
        Select(case)
        try {
            Select(case)
            // an exception should have been thrown before we reach this point
            assertTrue(false, "an exception should have been thrown by the Select")
        }
        catch (_ : RuntimeException) {}
    }

    @Test
    fun testSanityCheckRerunCase() {
        val chan = SyncChannel<Int,Int>(1) { Optional.of(1) }
        val select = Select(
            Select.SyncCase(chan) {}
        )
        runBlocking { select.run() }
        // the select should only be able to be run once
        try {
            runBlocking { select.run() }
            // an exception should have been thrown before we reach this point
            assertTrue(false, "an exception should have been thrown by the Select")
        }
        catch (_ : RuntimeException) {}
    }

    /**
     * Loser case blocks on a size-2 channel with no peer until Select cancels it.
     * After run() returns (post cancelAndJoin), the loser must not remain a participant.
     */
    @Test
    fun cancelledLoserCaseRemovedFromChannel() = runBlocking {
        val chanLoser = SyncChannel<Int, Int>(2) { Optional.of(1) }
        val chanWinner = SyncChannel<Int, Int>(1) { Optional.of(1) }
        Select(
            Select.SyncCase(chanLoser) {},
            Select.SyncCase(chanWinner) {},
        ).run()
        assertEquals(0, chanLoser.participantCountForTests())
        assertEquals(0, chanWinner.participantCountForTests())
    }

    /**
     * When Select wins on a size-1 channel, the loser case on a size-2 channel must scrub.
     * A waiter already on that channel (pairwise-incompatible with the loser) must remain
     * and still sync with a fresh compatible peer.
     */
    @Test
    fun staleSelectAbortAllowsRemainingPeersToSync() = runBlocking {
        withContext(Dispatchers.Default) {
            // compute receives a Set, so identical peer constraints collapse to size 1.
            // Distinct constraints ("w" vs "loser") yield size 2 → UNSAT, so W never syncs with Select.
            val chanA = SyncChannel<String, Int>(2) { cs ->
                if (cs.size == 1) Optional.of(7) else Optional.empty()
            }
            val chanWinner = SyncChannel<Int, Int>(1) { Optional.of(1) }

            var wGot: Int? = null
            val w = launch {
                val r = chanA.sync("w")
                assertTrue(r.isPresent)
                wGot = r.result.get()
            }
            awaitParticipantCount(chanA, 1)

            Select(
                Select.SyncCase(chanA, "loser", "loser") {},
                Select.SyncCase(chanWinner) {},
            ).run()

            // Loser scrubbed; W remains.
            assertEquals(1, chanA.participantCountForTests())
            assertEquals(0, chanWinner.participantCountForTests())

            var pGot: Int? = null
            val p = launch {
                val r = chanA.sync("w")
                assertTrue(r.isPresent)
                pGot = r.result.get()
            }
            withTimeout(5.seconds) {
                w.join(); p.join()
            }
            assertEquals(7, wGot)
            assertEquals(7, pGot)
            assertEquals(0, chanA.participantCountForTests())
        }
    }

    /** Two single-case Selects on the same size-2 channel must rendezvous and clear the channel. */
    @Test
    fun twoSingleCaseSelectsSyncOnOneChannel() = runBlocking {
        withContext(Dispatchers.Default) {
            val shared = AtomicInteger(0)
            val chan = SyncChannel<Int, Int>(2) {
                Optional.of(shared.incrementAndGet())
            }
            var v1 = -1
            var v2 = -1
            val j1 = launch {
                Select(Select.SyncCase(chan) { v -> v1 = v }).run()
            }
            val j2 = launch {
                Select(Select.SyncCase(chan) { v -> v2 = v }).run()
            }
            withTimeout(5.seconds) {
                j1.join(); j2.join()
            }
            assertEquals(v1, v2)
            assertTrue(v1 > 0)
            assertEquals(0, chan.participantCountForTests())
        }
    }

    private val chmResultUpdate : (Int, Int?)->Int? = {
            _, curVal ->
        if (curVal == null) {
            1
        } else {
            curVal + 1
        }
    }
}