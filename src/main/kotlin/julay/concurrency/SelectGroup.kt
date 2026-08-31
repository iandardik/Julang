package julay.concurrency

import kotlinx.coroutines.CompletableDeferred
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Shared state for one [Select] invocation: race winner + park-once completion.
 */
class SelectGroup<V : Any>(
    private var select: Select,
    onWin: (V) -> Unit = {},
) {
    private var onWin: (V) -> Unit = onWin
    @Volatile
    private var completed = CompletableDeferred<Completion>()
    private val closedNoted = AtomicBoolean(false)
    /** Signaled when a channel may have a new peer for parked cases (re-promote). */
    @Volatile
    private var nudgeSignal = CompletableDeferred<Unit>()

    sealed class Completion {
        data class Won<V : Any>(val value: V) : Completion()
        object NoWinner : Completion()
    }

    fun tryCompleteWinner(value: V, channelHash: Int? = null) {
        if (channelHash != null) {
            select.forceWinnerHash(channelHash)
        }
        select.confirmRaceWin()
        try {
            if (completed.complete(Completion.Won(value))) {
                onWin.invoke(value)
            }
        } finally {
            nudge()
        }
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
        nudgeSignal.complete(Unit)
    }

    suspend fun awaitCompletion(): Completion = completed.await()

    /**
     * Wait until completion or a promote nudge; returns true if Select finished.
     *
     * Must not reset a completed nudge *before* awaiting — that drops wakeups that arrived
     * between "tryLead found nothing" and park.
     *
     * @param timeoutMs if non-null, return after timeout even if neither fired (caller rematches).
     */
    suspend fun awaitCompletionOrNudge(timeoutMs: Long? = null): Boolean {
        if (completed.isCompleted) return true
        val (c, n) = synchronized(this) { completed to nudgeSignal }
        if (c.isCompleted) return true
        if (timeoutMs == null) {
            kotlinx.coroutines.selects.select {
                c.onAwait { }
                n.onAwait { }
            }
        } else {
            kotlinx.coroutines.withTimeoutOrNull(timeoutMs) {
                kotlinx.coroutines.selects.select {
                    c.onAwait { }
                    n.onAwait { }
                }
            }
        }
        synchronized(this) {
            if (nudgeSignal === n && nudgeSignal.isCompleted) {
                nudgeSignal = CompletableDeferred()
            }
        }
        return completed.isCompleted
    }

    fun isDone(): Boolean = completed.isCompleted

    /** Prepare for another [SelectCoordinator.run] on the same long-lived shell. */
    fun reset(select: Select, onWin: (V) -> Unit) {
        this.select = select
        this.onWin = onWin
        closedNoted.set(false)
        completed = CompletableDeferred()
        nudgeSignal = CompletableDeferred()
    }
}

/**
 * Go-style select race: CAS EMPTY → hash (provisional) until [confirm].
 * Used for all Select commits (including Select-vs-Select); never treat provisional as done.
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

    /** Set hash even if a peer cleared provisional state before confirm flush. */
    fun forceHash(channelHash: Int) {
        state.set(channelHash)
    }

    fun reset() {
        state.set(null)
        confirmed.set(false)
    }
}
