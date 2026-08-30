package julay.concurrency

import kotlinx.coroutines.CompletableDeferred
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Shared state for one [Select] invocation: race/2PL winner + park-once completion.
 */
class SelectGroup<V : Any>(
    private val select: Select,
    private val onWin: (V) -> Unit,
) {
    private val completed = CompletableDeferred<Completion>()
    private val closedNoted = AtomicBoolean(false)
    /** Signaled when a channel may have a new peer for parked cases (re-promote). */
    @Volatile
    private var nudge = CompletableDeferred<Unit>()

    sealed class Completion {
        data class Won<V : Any>(val value: V) : Completion()
        object NoWinner : Completion()
    }

    fun tryCompleteWinner(value: V) {
        select.confirmRaceWin()
        if (completed.complete(Completion.Won(value))) {
            onWin.invoke(value)
        }
        nudge()
    }

    fun signalChannelClosed() {
        if (closedNoted.compareAndSet(false, true)) {
            select.noteChannelClosed()
        }
        if (!select.isRaceConfirmed() && !completed.isCompleted) {
            completed.complete(Completion.NoWinner)
        }
        nudge()
    }

    fun signalNoWinner() {
        if (!select.isRaceConfirmed() && !completed.isCompleted) {
            completed.complete(Completion.NoWinner)
        }
        nudge()
    }

    fun nudge() {
        nudge.complete(Unit)
    }

    suspend fun awaitCompletion(): Completion = completed.await()

    /**
     * Wait until completion or a promote nudge; returns true if Select finished.
     *
     * Must not reset a completed nudge *before* awaiting — that drops wakeups that arrived
     * between "tryLead found nothing" and park.
     */
    suspend fun awaitCompletionOrNudge(): Boolean {
        if (completed.isCompleted) return true
        val n = synchronized(this) { nudge }
        kotlinx.coroutines.selects.select {
            completed.onAwait { }
            n.onAwait { }
        }
        synchronized(this) {
            if (nudge === n && nudge.isCompleted) {
                nudge = CompletableDeferred()
            }
        }
        return completed.isCompleted
    }

    fun isDone(): Boolean = completed.isCompleted
}

/**
 * Go-style select race: CAS EMPTY → hash (provisional) until [confirm].
 * Used for single-Select commit; Select-vs-Select still uses StratifiedMutex 2PL.
 */
class SelectRace {
    private val state = AtomicReference<Int?>(null)
    private val confirmed = AtomicBoolean(false)

    fun tryRaceWin(channelHash: Int): Boolean {
        while (true) {
            val cur = state.get()
            when {
                cur == null -> {
                    if (state.compareAndSet(null, channelHash)) return true
                }
                cur == channelHash -> return true
                else -> return false
            }
        }
    }

    fun rollbackRaceWin(channelHash: Int) {
        if (confirmed.get()) return
        state.compareAndSet(channelHash, null)
    }

    fun confirm() {
        confirmed.set(true)
    }

    fun isConfirmed(): Boolean = confirmed.get()

    fun hasWinner(): Boolean = state.get() != null

    fun winnerHash(): Int? = state.get()
}
