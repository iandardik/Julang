package julay.program

import julay.program.action.ConcreteAction
import julay.program.action.SymbolicAction
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.launch

fun defaultHandlerPoolSize(): Int =
    maxOf(2, Runtime.getRuntime().availableProcessors())

/** Resolved procfun metadata for a pooled HTTP handler. */
data class ProcFunHandlerSpec(
    val name: String,
    val info: TransitionSystemStaticInfo,
    val ctorSym: SymbolicAction,
    val factory: suspend (Program, ConcreteAction) -> TransitionSystem,
)

/**
 * Fixed pool of long-lived handler [Proc]s. Each worker loops on [HandlerWork] items instead of
 * spawning a new proc per HTTP request.
 */
class ProcFunHandlerPool private constructor(
    private val program: Program,
    private val spec: ProcFunHandlerSpec,
    poolSize: Int,
) {
    private class Slot(
        val proc: Proc,
        val work: Channel<HandlerWork>,
        var job: Job,
    )

    private val available = ArrayBlockingQueue<Slot>(poolSize)
    @Volatile
    private var shutdown = false

    init {
        repeat(poolSize) {
            available.offer(spawnSlot())
        }
    }

    fun serveBlocking(argValues: List<Any>): Any {
        if (shutdown) {
            throw IllegalStateException("ProcFunHandlerPool for \"${spec.name}\" is shut down")
        }
        var slot = available.take()
        try {
            val future = CompletableFuture<Any>()
            program.godScope.launch {
                try {
                    val workDeferred = CompletableDeferred<Value>()
                    slot.work.send(HandlerWork(argValues, workDeferred))
                    future.complete(workDeferred.await().value)
                } catch (e: CancellationException) {
                    future.completeExceptionally(e)
                } catch (e: Throwable) {
                    future.completeExceptionally(e)
                }
            }
            return try {
                future.get()
            } catch (e: ExecutionException) {
                throw e.cause ?: e
            }
        } catch (e: Throwable) {
            if (!shutdown && !slot.job.isActive) {
                closeSlot(slot)
                slot = spawnSlot()
            }
            throw e
        } finally {
            if (!shutdown) {
                if (slot.job.isActive) {
                    available.offer(slot)
                } else {
                    available.offer(spawnSlot())
                }
            }
        }
    }

    fun shutdown() {
        shutdown = true
        while (true) {
            val slot = available.poll() ?: break
            closeSlot(slot)
        }
    }

    private fun spawnSlot(): Slot {
        val work = Channel<HandlerWork>(Channel.UNLIMITED)
        val proc = Proc(
            PooledWorkerPlaceholder,
            spec.info,
            program.staticChannelTable,
            program,
        )
        lateinit var slot: Slot
        val job = program.godScope.launch {
            try {
                proc.runHttpHandlerLoop(work, spec.factory, spec.ctorSym)
            } catch (_: ClosedReceiveChannelException) {
                // pool shutdown closed the channel
            } catch (e: CancellationException) {
                if (!shutdown) {
                    throw e
                }
            }
        }
        slot = Slot(proc, work, job)
        return slot
    }

    private fun closeSlot(slot: Slot) {
        slot.proc.requestSilentKill()
        slot.job.cancel()
        slot.work.close()
    }

    companion object {
        fun create(program: Program, name: String, poolSize: Int = defaultHandlerPoolSize()): ProcFunHandlerPool {
            val spec = program.resolveProcFunHandler(name)
            return ProcFunHandlerPool(program, spec, poolSize)
        }
    }
}
