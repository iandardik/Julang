package julay.bench

import julay.concurrency.SelectCaseOffer
import julay.concurrency.SelectCoordinator
import julay.concurrency.SyncChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.Optional
import java.util.concurrent.atomic.AtomicLong
import kotlin.system.exitProcess

/**
 * No-HTTP SyncChannel / Select rendezvous load for async-profiler.
 *
 * Modes:
 *   syncfast — two coroutines rendezvous on one size-2 channel via [SyncChannel.syncFast]
 *   select3  — Protocol-shaped: [SelectCoordinator.runOffers] on 3 size-2 channels vs one sync peer
 *
 * Usage:
 *   ./gradlew -q rendezvousMicrobench --args='--mode syncfast --seconds 30'
 *   ./gradlew -q rendezvousMicrobench --args='--mode select3 --seconds 30'
 */
fun main(args: Array<String>) {
    var mode = "syncfast"
    var seconds = 30
    var i = 0
    while (i < args.size) {
        when (args[i]) {
            "--mode" -> {
                mode = args.getOrNull(++i) ?: usage()
            }
            "--seconds" -> {
                seconds = args.getOrNull(++i)?.toIntOrNull() ?: usage()
            }
            "-h", "--help" -> usage()
            else -> usage()
        }
        i++
    }
    when (mode) {
        "syncfast" -> runSyncFast(seconds)
        "select3" -> runSelect3(seconds)
        else -> usage()
    }
}

private fun usage(): Nothing {
    System.err.println(
        "usage: RendezvousMicrobench --mode syncfast|select3 [--seconds N]",
    )
    exitProcess(2)
}

private fun runSyncFast(seconds: Int) {
    val ops = AtomicLong(0)
    // antisCompatible always true: Int anticonstraints are role tags in Julay, not formulas.
    val chan = SyncChannel<Int, Int>(
        2,
        antisCompatible = { _, _ -> true },
    ) { Optional.of(1) }
    val deadline = System.nanoTime() + seconds * 1_000_000_000L
    println("rendezvous_microbench mode=syncfast seconds=$seconds pid=${ProcessHandle.current().pid()}")
    System.out.flush()
    runBlocking {
        withContext(Dispatchers.Default) {
            val a = launch {
                while (System.nanoTime() < deadline) {
                    chan.syncFast(0, 0)
                    ops.incrementAndGet()
                }
            }
            val b = launch {
                while (System.nanoTime() < deadline) {
                    chan.syncFast(1, 1)
                    ops.incrementAndGet()
                }
            }
            a.join()
            b.join()
        }
    }
    println("ops_total=${ops.get()}  approx_rps=${ops.get() / seconds.toDouble()}")
}

private fun runSelect3(seconds: Int) {
    val ops = AtomicLong(0)
    val c1 = SyncChannel<Int, Int>(2) { Optional.of(1) }
    val c2 = SyncChannel<Int, Int>(2) { Optional.of(2) }
    val c3 = SyncChannel<Int, Int>(2) { Optional.of(3) }
    val channels = arrayOf(c1, c2, c3)
    val deadline = System.nanoTime() + seconds * 1_000_000_000L
    println("rendezvous_microbench mode=select3 seconds=$seconds pid=${ProcessHandle.current().pid()}")
    System.out.flush()
    runBlocking {
        withContext(Dispatchers.Default) {
            val provider = launch {
                val slots = listOf(
                    SelectCaseOffer<Int, Int>(),
                    SelectCaseOffer(),
                    SelectCaseOffer(),
                )
                while (System.nanoTime() < deadline) {
                    slots[0].fill(c1, Optional.empty(), Optional.empty()) {}
                    slots[1].fill(c2, Optional.empty(), Optional.empty()) {}
                    slots[2].fill(c3, Optional.empty(), Optional.empty()) {}
                    SelectCoordinator.runOffers(slots)
                    ops.incrementAndGet()
                }
            }
            val clients = List(3) { idx ->
                launch {
                    val chan = channels[idx]
                    while (System.nanoTime() < deadline) {
                        chan.sync()
                        ops.incrementAndGet()
                    }
                }
            }
            provider.join()
            clients.forEach { it.join() }
        }
    }
    println("ops_total=${ops.get()}  approx_rps=${ops.get() / seconds.toDouble()}")
}
