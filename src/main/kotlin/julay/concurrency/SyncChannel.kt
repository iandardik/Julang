package julay.concurrency

import julay.tools.assert
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The sole mechanism for inter-process communication in Julay.
 *
 * ## Participants and rendezvous
 *
 * Each [sync] call creates one [Participant] (constraint + optional anticonstraint + optional
 * [Select]) and registers it in [participants] under [mutex]. A successful rendezvous removes
 * exactly [syncSize] participants and delivers the same [V] to all of them.
 *
 * - [syncSize] **2** (main): pairwise rendezvous. Under the mutex we filter peers (anti /
 *   [rejectedPeers]), reserve a pair (`pairing` + shared `pairGate`), snapshot constraints, then
 *   **unlock**. The leader runs [compute] outside the lock; on success we relock, revalidate,
 *   run Select 2PL, remove the pair, and the leader completes the follower's [Participant.valueGate].
 * - [syncSize] **1** (special): self-commit with the same compute-outside pattern (no peer).
 *
 * Callers that cannot form a group [WAIT] on [wakeWaiters] until registration/pairing/close
 * signals them. A peer reserved by a leader [FOLLOW]s: it awaits `pairGate`, then receives [V]
 * (or retries if the reservation was released without a value).
 *
 * Lock order when Select is involved: channel [mutex] outer, Select [StratifiedMutex]es inner
 * (sorted). Never hold the channel mutex across [compute].
 *
 * [syncFast] is the no-Select entry used by single-offer Proc steps; it shares the participant
 * table with [sync] so Select arms and syncFast callers can rendezvous.
 *
 * @param antisCompatible When both peers have anticonstraints, return true if they may sync
 *   on the anti dimension. Julay wires SyncAnti equality. Empty anticonstraint = no exclusivity.
 */
class SyncChannel<C : Any, V : Any>(
    private val syncSize: Int,
    private val satisfiable: ((Set<C>) -> Boolean)? = null,
    private val antisCompatible: ((C, C) -> Boolean)? = null,
    /**
     * Optional under-lock snapshot before [compute] runs outside the mutex.
     * Returns (constraints for compute, cleanup after compute). Used to clone Z3 ASTs
     * so peer Case Contexts may close without racing translate.
     */
    private val snapshotForCompute: ((Set<C>) -> Pair<Set<C>, () -> Unit>)? = null,
    private val compute: (Set<C>) -> Optional<V>,
) {
    private val mutex = Mutex()
    /** Live offers currently registered on this channel (waiting, pairing, or mid-commit). */
    private val participants = mutableSetOf<Participant<V, C>>()
    /** Waiters blocked in [DecisionKind.WAIT]; completed after unlock via [wakeFlushBatch]. */
    private val wakeWaiters = mutableListOf<CompletableDeferred<Unit>>()
    /** Completions deferred until after [withChannelLock] releases [mutex] (avoid lock re-entry). */
    private var wakeFlushBatch: MutableList<CompletableDeferred<Unit>>? = null
    private var closed = false

    init {
        assert(
            syncSize == 1 || syncSize == 2,
            "SyncChannel syncSize must be 1 or 2 (got $syncSize)",
        )
    }

    private suspend inline fun <T> withChannelLock(block: () -> T): T {
        val batch = mutableListOf<CompletableDeferred<Unit>>()
        val result = mutex.withLock {
            wakeFlushBatch = batch
            try {
                block()
            } finally {
                wakeFlushBatch = null
            }
        }
        batch.forEach { it.complete(Unit) }
        return result
    }

    suspend fun sync(select: Optional<Select> = Optional.empty()): SyncChannelResult<V> {
        return sync(Optional.empty(), Optional.empty(), select)
    }

    suspend fun sync(constraint: C, select: Optional<Select> = Optional.empty()): SyncChannelResult<V> {
        return sync(Optional.of(constraint), Optional.empty(), select)
    }

    /**
     * No-Select rendezvous for single-offer Proc steps ([Select.SyncCase.syncDirect] /
     * [julay.program.Proc] FastOnly). Same participant table as [sync]; skips Select 2PL and
     * avoids allocating empty [Optional] Select wrappers on the hot path.
     */
    suspend fun syncFast(constraint: C, anticonstraint: C): SyncChannelResult<V> {
        return sync(Optional.of(constraint), Optional.of(anticonstraint), Optional.empty())
    }

    /**
     * Offer this call as a participant and block until rendezvous succeeds or aborts.
     *
     * Loop: under the mutex, [pickCandidateLocked] returns a decision; we act on it
     * (wait / follow / compute+finish / abort). Cancel and close scrub [me] from [participants].
     */
    suspend fun sync(
        constraint: Optional<C>,
        anticonstraint: Optional<C>,
        select: Optional<Select> = Optional.empty(),
    ): SyncChannelResult<V> {
        val me = Participant<V, C>(constraint, anticonstraint, select)
        // Reused across loop iterations to avoid SyncDecision allocations on the hot path.
        val decision = DecisionBuf<V, C>()
        try {
            while (true) {
                // Another leader already committed us as follower while we were unlocked.
                if (me.followerValue.get()) {
                    return waitForLeaderOrAbort(me)
                }
                // Pick under NonCancellable so we never leave pairing=true half-updated.
                withContext(NonCancellable) {
                    withChannelLock { pickCandidateLocked(me, decision) }
                }
                when (decision.kind) {
                    DecisionKind.ABORT -> {
                        removeSelfAfterAbort(me)
                        return SyncChannelResult.abort()
                    }
                    DecisionKind.FOLLOW -> {
                        val gate = decision.pairGate!!
                        try {
                            gate.await()
                        } catch (_: CancellationException) {
                            removeSelfAfterAbort(me)
                            throw CancellationException()
                        }
                        if (me.followerValue.get()) {
                            return waitForLeaderOrAbort(me)
                        }
                        if (closed) {
                            removeSelfAfterAbort(me)
                            return SyncChannelResult.abort()
                        }
                        continue
                    }
                    DecisionKind.WAIT -> {
                        // Re-check under lock before awaiting to avoid lost wakeups.
                        val wake = withContext(NonCancellable) {
                            withChannelLock {
                                if (closed || me !in participants || me.followerValue.get() || me.pairing) {
                                    return@withChannelLock null
                                }
                                val w = CompletableDeferred<Unit>()
                                wakeWaiters.add(w)
                                if (closed || me.followerValue.get() || me.pairing) {
                                    wakeWaiters.remove(w)
                                    return@withChannelLock null
                                }
                                w
                            }
                        }
                        if (wake == null) {
                            if (closed) {
                                removeSelfAfterAbort(me)
                                return SyncChannelResult.abort()
                            }
                            if (me.followerValue.get()) {
                                return waitForLeaderOrAbort(me)
                            }
                            continue
                        }
                        try {
                            wake.await()
                        } catch (_: CancellationException) {
                            withContext(NonCancellable) {
                                withChannelLock { wakeWaiters.remove(wake) }
                            }
                            throw CancellationException()
                        }
                        continue
                    }
                    DecisionKind.TRY -> {
                        val group = decision.group
                        val constraints = decision.constraints
                        val computeCleanup = decision.computeCleanup
                        val syncValue = try {
                            compute.invoke(constraints)
                        } catch (e: CancellationException) {
                            computeCleanup()
                            withContext(NonCancellable) {
                                withChannelLock {
                                    releasePairingLocked(group)
                                }
                            }
                            removeSelfAfterAbort(me)
                            throw e
                        } catch (e: Throwable) {
                            computeCleanup()
                            throw e
                        }
                        computeCleanup()
                        // NonCancellable: must finish revalidate/Select 2PL or release pairing;
                        // cancel mid-selectsCommit would leak pairing=true (deadlock).
                        withContext(NonCancellable) {
                            withChannelLock {
                                finishAfterComputeLocked(me, group, syncValue, decision)
                            }
                        }
                        when (decision.kind) {
                            DecisionKind.ABORT -> {
                                removeSelfAfterAbort(me)
                                return SyncChannelResult.abort()
                            }
                            DecisionKind.FOLLOW -> {
                                val gate = me.pairGate
                                if (gate != null && !gate.isCompleted) {
                                    try { gate.await() } catch (_: CancellationException) {}
                                }
                                return waitForLeaderOrAbort(me)
                            }
                            DecisionKind.RETRY -> {
                                yield()
                                continue
                            }
                            DecisionKind.SAT -> {
                                val value = decision.value!!
                                // valueGate.complete is non-suspending — no NonCancellable needed.
                                for (p in decision.group) {
                                    if (p !== me) {
                                        p.valueGate.complete(value)
                                    }
                                }
                                return SyncChannelResult.sat(value)
                            }
                            DecisionKind.WAIT, DecisionKind.TRY ->
                                error("unreachable finish kind: ${decision.kind}")
                        }
                    }
                    DecisionKind.RETRY, DecisionKind.SAT ->
                        error("unreachable pick kind: ${decision.kind}")
                }
            }
        } catch (_: CancellationException) {
            removeSelfAfterAbort(me)
            return SyncChannelResult.abort()
        }
    }

    private suspend fun waitForLeaderOrAbort(me: Participant<V, C>): SyncChannelResult<V> {
        try {
            return SyncChannelResult.sat(me.valueGate.await())
        } catch (_: CancellationException) {
        } catch (_: Exception) {
        }
        removeSelfAfterAbort(me)
        return SyncChannelResult.abort()
    }

    private suspend fun removeSelfAfterAbort(me: Participant<V, C>) {
        withContext(NonCancellable) {
            me.valueGate.cancel()
            withChannelLock {
                if (!closed) {
                    releasePairingLocked(setOf(me))
                    removeParticipants(setOf(me))
                    signalWaitersLocked()
                }
            }
        }
    }

    /**
     * Under [mutex]: register [me] if needed, then fill [out] with the next decision.
     */
    private fun pickCandidateLocked(me: Participant<V, C>, out: DecisionBuf<V, C>) {
        if (me.followerValue.get()) {
            out.setFollow(me.pairGate ?: completedGate())
            return
        }
        if (closed) {
            out.setAbort()
            return
        }
        if (me !in participants) {
            if (me.everRegistered) {
                out.setAbort()
                return
            }
            me.everRegistered = true
            participants.add(me)
            signalWaitersLocked()
        }
        if (me.pairing) {
            out.setFollow(me.pairGate ?: completedGate())
            return
        }
        if (syncSize == 1) {
            me.pairing = true
            me.pairGate = CompletableDeferred()
            val raw = constraintsOf(me, null)
            val (constraints, cleanup) = snapshotForCompute?.invoke(raw) ?: (raw to {})
            out.setTry(constraints, setOf(me), pairGate = null, computeCleanup = cleanup)
            return
        }
        for (peer in participants) {
            if (peer === me) continue
            if (peer.pairing || peer.followerValue.get()) continue
            if (me.isRejected(peer)) continue
            if (!antiOk(me, peer)) continue
            me.pairing = true
            peer.pairing = true
            val gate = CompletableDeferred<Unit>()
            me.pairGate = gate
            peer.pairGate = gate
            signalWaitersLocked()
            val raw = constraintsOf(me, peer)
            val (constraints, cleanup) = snapshotForCompute?.invoke(raw) ?: (raw to {})
            out.setTry(constraints, setOf(me, peer), pairGate = gate, computeCleanup = cleanup)
            return
        }
        out.setWait()
    }

    private fun completedGate(): CompletableDeferred<Unit> =
        CompletableDeferred<Unit>().also { it.complete(Unit) }

    private fun releasePairingLocked(group: Set<Participant<V, C>>) {
        val gates = mutableListOf<CompletableDeferred<Unit>>()
        for (p in group) {
            p.pairing = false
            p.pairGate?.let { gates.add(it) }
            p.pairGate = null
        }
        val batch = wakeFlushBatch
        if (batch != null) {
            batch.addAll(gates)
        } else {
            gates.forEach { it.complete(Unit) }
        }
        signalWaitersLocked()
    }

    private suspend fun finishAfterComputeLocked(
        me: Participant<V, C>,
        group: Set<Participant<V, C>>,
        syncValue: Optional<V>,
        out: DecisionBuf<V, C>,
    ) {
        if (me !in participants) {
            releasePairingLocked(group)
            if (me.followerValue.get()) {
                out.setFollow(me.pairGate ?: completedGate())
            } else {
                out.setAbort()
            }
            return
        }
        if (closed) {
            releasePairingLocked(group)
            removeParticipants(setOf(me))
            out.setAbort()
            return
        }
        val peers = group.filter { it !== me }
        if (peers.any { it !in participants }) {
            releasePairingLocked(group)
            out.setRetry()
            return
        }
        if (syncValue.isEmpty) {
            if (syncSize == 1) {
                releasePairingLocked(group)
                removeParticipants(setOf(me))
                out.setAbort()
                return
            }
            peers.forEach { me.addRejected(it) }
            releasePairingLocked(group)
            out.setRetry()
            return
        }
        if (!selectsCommit(group)) {
            val stale = participants.filter { it.selectIsWon }.toSet()
            releasePairingLocked(group)
            for (p in stale) {
                p.valueGate.cancel()
            }
            removeParticipants(stale)
            signalWaitersLocked()
            if (me in stale) out.setAbort() else out.setRetry()
            return
        }
        for (p in peers) {
            p.followerValue.set(true)
        }
        val gates = group.mapNotNull { it.pairGate }
        for (p in group) {
            p.pairGate = null
            p.pairing = false
        }
        removeParticipants(group)
        val batch = wakeFlushBatch
        if (batch != null) {
            batch.addAll(gates)
        } else {
            gates.forEach { it.complete(Unit) }
        }
        signalWaitersLocked()
        out.setSat(syncValue.get(), group)
    }

    private fun signalWaitersLocked() {
        if (wakeWaiters.isEmpty()) return
        val copy = wakeWaiters.toList()
        wakeWaiters.clear()
        val batch = wakeFlushBatch
        if (batch != null) {
            batch.addAll(copy)
        } else {
            copy.forEach { it.complete(Unit) }
        }
    }

    private fun constraintsOf(a: Participant<V, C>, b: Participant<V, C>?): Set<C> {
        val hasA = a.constraint.isPresent
        val hasB = b != null && b.constraint.isPresent
        return when {
            hasA && hasB -> setOf(a.constraint.get(), b!!.constraint.get())
            hasA -> setOf(a.constraint.get())
            hasB -> setOf(b!!.constraint.get())
            else -> emptySet()
        }
    }

    private fun removeParticipants(toRemove: Set<Participant<V, C>>) {
        if (toRemove.isEmpty()) return
        for (r in toRemove) {
            r.clearRejected()
            r.pairing = false
        }
        for (p in participants) {
            p.removeRejectedAll(toRemove)
        }
        participants.removeAll(toRemove)
    }

    private fun antiOk(p1: Participant<V, C>, p2: Participant<V, C>): Boolean {
        if (p1.anticonstraint.isEmpty || p2.anticonstraint.isEmpty) return true
        val a = p1.anticonstraint.get()
        val b = p2.anticonstraint.get()
        return antisCompatible?.invoke(a, b)
            ?: !pairwiseSatisfiable(a, b)
    }

    private fun pairwiseSatisfiable(c1: C, c2: C): Boolean {
        val constraints = setOf(c1, c2)
        return satisfiable?.invoke(constraints) ?: compute.invoke(constraints).isPresent
    }

    private suspend fun selectsCommit(group: Set<Participant<V, C>>): Boolean {
        val myHash = hashCode()
        val selectGroup = group.filter { p -> p.select.isPresent }
        if (selectGroup.isEmpty()) return true
        val allMutexes = selectGroup.map { p -> p.select.get().getWinnerMutex() }.sorted()
        var lockedMutexes = emptyList<StratifiedMutex>()
        try {
            lockedMutexes = allMutexes.map {
                it.mutex.lock()
                it
            }
            assert(lockedMutexes == allMutexes, "Expected all locked mutexes")
            val allCanCommit = selectGroup.all { p ->
                val select = p.select.get()
                val selectCanCommit = select.canCommit(myHash)
                p.selectIsWon = !selectCanCommit
                selectCanCommit
            }
            if (allCanCommit) {
                selectGroup.forEach { it.select.get().doCommit(myHash) }
            }
            return allCanCommit
        } finally {
            lockedMutexes.forEach { it.mutex.unlock() }
        }
    }

    suspend fun close() {
        withChannelLock {
            if (!closed) {
                closed = true
                participants.forEach {
                    if (!it.followerValue.get()) {
                        it.valueGate.cancel()
                    }
                    it.pairGate?.complete(Unit)
                    it.pairGate = null
                    it.pairing = false
                }
                removeParticipants(participants.toSet())
                signalWaitersLocked()
            }
        }
    }

    suspend fun isClosed(): Boolean {
        withChannelLock {
            return closed
        }
    }

    internal suspend fun participantCountForTests(): Int =
        withChannelLock { participants.size }

    internal fun mutexAvailableForTests(): Boolean = !mutex.isLocked

    private class Participant<V : Any, C : Any>(
        val constraint: Optional<C>,
        val anticonstraint: Optional<C>,
        val select: Optional<Select>,
    ) {
        /** Leader completes this for followers; capacity-1 Channel replaced to cut alloc. */
        val valueGate = CompletableDeferred<V>()
        /** Peers for which this participant already saw empty [compute] (avoid livelock). */
        private var rejectedPeers: MutableSet<Participant<V, C>>? = null
        var selectIsWon = false
        var pairing = false
        var pairGate: CompletableDeferred<Unit>? = null
        var everRegistered = false
        val followerValue = AtomicBoolean(false)

        fun isRejected(peer: Participant<V, C>): Boolean =
            rejectedPeers?.contains(peer) == true

        fun addRejected(peer: Participant<V, C>) {
            val set = rejectedPeers
            if (set != null) {
                set.add(peer)
            } else {
                rejectedPeers = mutableSetOf(peer)
            }
        }

        fun clearRejected() {
            rejectedPeers?.clear()
            rejectedPeers = null
        }

        fun removeRejectedAll(toRemove: Set<Participant<V, C>>) {
            rejectedPeers?.removeAll(toRemove)
        }
    }

    private enum class DecisionKind {
        ABORT, WAIT, TRY, RETRY, SAT, FOLLOW,
    }

    /** Mutable decision buffer reused across pick/finish within one [sync] call. */
    private class DecisionBuf<V : Any, C : Any> {
        var kind: DecisionKind = DecisionKind.WAIT
        var constraints: Set<C> = emptySet()
        var group: Set<Participant<V, C>> = emptySet()
        var value: V? = null
        var pairGate: CompletableDeferred<Unit>? = null
        var computeCleanup: () -> Unit = {}

        fun setAbort() {
            kind = DecisionKind.ABORT
            clearPayload()
        }

        fun setWait() {
            kind = DecisionKind.WAIT
            clearPayload()
        }

        fun setRetry() {
            kind = DecisionKind.RETRY
            clearPayload()
        }

        fun setFollow(gate: CompletableDeferred<Unit>) {
            kind = DecisionKind.FOLLOW
            constraints = emptySet()
            group = emptySet()
            value = null
            pairGate = gate
            computeCleanup = {}
        }

        fun setTry(
            constraints: Set<C>,
            group: Set<Participant<V, C>>,
            pairGate: CompletableDeferred<Unit>?,
            computeCleanup: () -> Unit,
        ) {
            kind = DecisionKind.TRY
            this.constraints = constraints
            this.group = group
            this.value = null
            this.pairGate = pairGate
            this.computeCleanup = computeCleanup
        }

        fun setSat(value: V, group: Set<Participant<V, C>>) {
            kind = DecisionKind.SAT
            this.constraints = emptySet()
            this.group = group
            this.value = value
            this.pairGate = null
            this.computeCleanup = {}
        }

        private fun clearPayload() {
            constraints = emptySet()
            group = emptySet()
            value = null
            pairGate = null
            computeCleanup = {}
        }
    }
}

data class SyncChannelResult<V : Any>(
    val result: Optional<V>,
) {
    val isEmpty = result.isEmpty
    val isPresent = result.isPresent

    companion object {
        fun <V : Any> sat(value: V): SyncChannelResult<V> {
            return SyncChannelResult(Optional.of(value))
        }

        fun <V : Any> abort(): SyncChannelResult<V> {
            return SyncChannelResult(Optional.empty())
        }
    }
}
