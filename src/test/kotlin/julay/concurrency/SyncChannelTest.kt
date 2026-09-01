package julay.concurrency

import kotlinx.coroutines.*
import kotlin.test.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

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
    fun constructorRejectsSyncSizeOutside1Or2() {
        assertFails { SyncChannel<Int, Int>(0) { Optional.of(1) } }
        assertFails { SyncChannel<Int, Int>(3) { Optional.of(1) } }
        assertFails { SyncChannel<Int, Int>(-1) { Optional.of(1) } }
    }


    private fun businessLogic1Channel(syncSize : Int, numThreads : Int) {
        // if this is false then some threads will hang
        julay.tools.assert(numThreads % syncSize == 0)

        val results = ConcurrentHashMap<Int,Int>() // value -> count
        // Constant value: compute may run and be discarded under races; do not key on increments.
        val chan = SyncChannel<Int,Int>(syncSize) { Optional.of(1) }
        runBlocking {
            withContext(Dispatchers.Default) {
                for (i in 1.. numThreads) {
                    launch {
                        val syncResult = chan.sync()
                        results.compute(syncResult.result.get(), chmResultUpdate)
                    }
                }
            }
        }

        assertEquals(1, results.size)
        assertEquals(numThreads, results[1])
    }

    @Test
    fun test2Channels1Sync() {
        businessLogic2Channels(1, 10_000)
    }

    @Test
    fun test2Channels2Sync() {
        businessLogic2Channels(2, 10_000)
    }


    private fun businessLogic2Channels(syncSize : Int, numThreads : Int) {
        // if this is false then some threads will hang
        julay.tools.assert(numThreads % syncSize == 0)

        val results1 = ConcurrentHashMap<Int,Int>()
        val results2 = ConcurrentHashMap<Int,Int>()
        val chan1 = SyncChannel<Int,Int>(syncSize) { Optional.of(1) }
        val chan2 = SyncChannel<Int,Int>(syncSize) { Optional.of(2) }
        runBlocking {
            withContext(Dispatchers.Default) {
                for (i in 1..numThreads) {
                    launch {
                        val syncResult = chan1.sync()
                        results1.compute(syncResult.result.get(), chmResultUpdate)
                    }
                    launch {
                        val syncResult = chan2.sync()
                        results2.compute(syncResult.result.get(), chmResultUpdate)
                    }
                }
            }
        }

        assertEquals(numThreads, results1[1])
        assertEquals(numThreads, results2[2])
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
            for (i in 1..2_000) {
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
                val jobs = listOf(t1, t2, t3)

                // Let a pair form; one waiter remains (or all still starting).
                awaitParticipantCount(chan, 1, timeout = 5.seconds)

                chan.close()
                withTimeout(5.seconds) {
                    jobs.forEach { it.join() }
                }
                assertTrue(jobs.none { it.isActive })
                assertEquals(0, chan.participantCountForTests())
            }
        }
    }

    @Test
    fun syncAfterCloseAborts() = runBlocking {
        withContext(Dispatchers.Default) {
            val chan = SyncChannel<Int, Int>(2) { Optional.of(0) }
            val waiter = launch { chan.sync() }
            awaitParticipantCount(chan, 1)

            chan.close()
            withTimeout(5.seconds) { waiter.join() }
            assertTrue(chan.isClosed())
            assertEquals(0, chan.participantCountForTests())

            val late = chan.sync()
            assertTrue(late.isEmpty)
            assertEquals(0, chan.participantCountForTests())

            chan.close() // idempotent
            assertTrue(chan.isClosed())
        }
    }

    @Test
    fun successfulSyncLeavesChannelEmpty() = runBlocking {
        withContext(Dispatchers.Default) {
            for (syncSize in listOf(1, 2)) {
                val chan = SyncChannel<Int, Int>(syncSize) { Optional.of(42) }
                val results = mutableListOf<Int>()
                val jobs = (1..syncSize).map {
                    launch {
                        val r = chan.sync()
                        assertTrue(r.isPresent)
                        synchronized(results) { results.add(r.result.get()) }
                    }
                }
                withTimeout(5.seconds) { jobs.forEach { it.join() } }
                assertEquals(0, chan.participantCountForTests())
                assertEquals(syncSize, results.size)
                assertTrue(results.all { it == 42 })
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
                val jobs = listOf(t1, t2, t3)

                awaitParticipantCount(chan, 1, timeout = 5.seconds)

                withTimeout(5.seconds) {
                    jobs.forEach { it.cancelAndJoin() }
                }
                assertTrue(jobs.none { it.isActive })
                assertEquals(0, chan.participantCountForTests())
            }
        }
    }

    @Test
    fun syncFastSize2Rendezvous() = runBlocking {
        withContext(Dispatchers.Default) {
            // antisCompatible always true: Int anticonstraints are role tags in Julay, not formulas.
            val chan = SyncChannel<Int, Int>(
                2,
                antisCompatible = { _, _ -> true },
            ) { cs -> Optional.of(cs.sum()) }
            val a = async { chan.syncFast(3, 10) }
            val b = async { chan.syncFast(7, 20) }
            withTimeout(5.seconds) {
                assertEquals(10, a.await().result.get())
                assertEquals(10, b.await().result.get())
            }
            assertEquals(0, chan.participantCountForTests())
        }
    }

    @Test
    fun syncFastInteropsWithSync() = runBlocking {
        withContext(Dispatchers.Default) {
            val chan = SyncChannel<Int, Int>(2) { Optional.of(99) }
            val viaSync = async { chan.sync(1) }
            val viaFast = async { chan.syncFast(2, 3) }
            withTimeout(5.seconds) {
                assertEquals(99, viaSync.await().result.get())
                assertEquals(99, viaFast.await().result.get())
            }
            assertEquals(0, chan.participantCountForTests())
        }
    }

    @Test
    fun syncFastSize1SelfCommit() = runBlocking {
        withContext(Dispatchers.Default) {
            val chan = SyncChannel<Int, Int>(1) { Optional.of(7) }
            val r = chan.syncFast(1, 2)
            assertTrue(r.isPresent)
            assertEquals(7, r.result.get())
            assertEquals(0, chan.participantCountForTests())
        }
    }

    @Test
    fun reusedParticipantShellManyRendezvous() = runBlocking {
        withContext(Dispatchers.Default) {
            val chan = SyncChannel<Int, Int>(
                2,
                antisCompatible = { _, _ -> true },
            ) { cs -> Optional.of(cs.sum()) }
            val left = SyncChannel.Participant<Int, Int>()
            val right = SyncChannel.Participant<Int, Int>()
            val leftDec = SyncChannel.DecisionBuf<Int, Int>()
            val rightDec = SyncChannel.DecisionBuf<Int, Int>()
            val n = 80
            withTimeout(10.seconds) {
                coroutineScope {
                    launch {
                        repeat(n) {
                            left.fillForSyncFast(1, 10)
                            try {
                                val r = chan.syncFast(left, leftDec)
                                assertTrue(r.isPresent)
                            } finally {
                                left.resetAfterSync()
                            }
                        }
                    }
                    launch {
                        repeat(n) {
                            right.fillForSyncFast(2, 20)
                            try {
                                val r = chan.syncFast(right, rightDec)
                                assertTrue(r.isPresent)
                            } finally {
                                right.resetAfterSync()
                            }
                        }
                    }
                }
            }
            assertEquals(0, chan.participantCountForTests())
            assertTrue(chan.mutexAvailableForTests())
        }
    }

    @Test
    fun reusedParticipantShellRecoversAfterCloseAbort() = runBlocking {
        withContext(Dispatchers.Default) {
            val shell = SyncChannel.Participant<Int, Int>()
            val dec = SyncChannel.DecisionBuf<Int, Int>()

            // Lone sync() on size-2 blocks until close aborts it (do not add a second peer).
            val chan = SyncChannel<Int, Int>(
                2,
                antisCompatible = { _, _ -> true },
            ) { Optional.of(1) }
            val waiter = async { chan.sync() }
            awaitParticipantCount(chan, 1)
            chan.close()
            assertTrue(waiter.await().isEmpty)
            assertEquals(0, chan.participantCountForTests())

            // Lone syncFast shell on size-2 blocks until close; resetAfterSync must clear flags.
            val chanAbort = SyncChannel<Int, Int>(
                2,
                antisCompatible = { _, _ -> true },
            ) { Optional.of(1) }
            shell.fillForSyncFast(10, 20)
            val fast = async { chanAbort.syncFast(shell, dec) }
            awaitParticipantCount(chanAbort, 1)
            chanAbort.close()
            assertTrue(fast.await().isEmpty)
            shell.resetAfterSync()
            assertEquals(0, chanAbort.participantCountForTests())

            val chan2 = SyncChannel<Int, Int>(
                2,
                antisCompatible = { _, _ -> true },
            ) { Optional.of(99) }
            val peer = async { chan2.syncFast(1, 2) }
            shell.fillForSyncFast(3, 4)
            val r = chan2.syncFast(shell, dec)
            assertTrue(r.isPresent)
            assertEquals(99, r.result.get())
            shell.resetAfterSync()
            assertTrue(peer.await().isPresent)
            assertEquals(0, chan2.participantCountForTests())
            assertTrue(chan2.mutexAvailableForTests())
        }
    }
}