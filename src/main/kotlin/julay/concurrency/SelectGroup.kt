package julay.concurrency

import kotlinx.coroutines.*
import java.util.Optional
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Shared state for a single [Select] invocation. [SyncChannel] notifies this when a registered
 * arm commits or when a registered arm's channel closes.
 */
class SelectGroup<V : Any>(
    private val select: Select,
    private val onWin: (V) -> Unit,
    armCount: Int,
) {
    private val completed = CompletableDeferred<Completion>()
    private val closedNoted = AtomicBoolean(false)
    private val armsRemaining = AtomicInteger(armCount)

    sealed class Completion {
        data class Won<V : Any>(val value: V) : Completion()
        object NoWinner : Completion()
    }

    /** First successful arm wins; later calls are ignored. [Select.doCommit] runs in [SyncChannel.selectsCommit]. */
    fun tryCompleteWinner(value: V) {
        if (completed.complete(Completion.Won(value))) {
            onWin.invoke(value)
        }
    }

    /** Channel close with no winner yet may end the whole Select. */
    fun signalChannelClosed() {
        if (closedNoted.compareAndSet(false, true)) {
            select.noteChannelClosed()
        }
        if (select.winner.isEmpty && !completed.isCompleted) {
            completed.complete(Completion.NoWinner)
        }
    }

    /** An arm finished without winning (sync abort, unregister, etc.). */
    fun armFinished() {
        if (armsRemaining.decrementAndGet() == 0 && select.winner.isEmpty && !completed.isCompleted) {
            completed.complete(Completion.NoWinner)
        }
    }

    suspend fun awaitCompletion(): Completion {
        return completed.await()
    }

    fun isDone(): Boolean = completed.isCompleted
}
