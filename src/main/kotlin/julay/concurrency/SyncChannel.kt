package julay.concurrency

import julay.tools.assert
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ClosedSendChannelException
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
 *   run Select 2PL, remove the pair, and the leader sends [V] on the follower's channel.
 * - [syncSize] **1** (special): self-commit with the same compute-outside pattern (no peer).
 *
 * Callers that cannot form a group [WAIT] on [wakeWaiters] until registration/pairing/close
 * signals them. A peer reserved by a leader [FOLLOW]s: it awaits `pairGate`, then receives [V]
 * (or retries if the reservation was released without a value).
 *
 * Lock order when Select is involved: channel [mutex] outer, Select [StratifiedMutex]es inner
 * (sorted). Never hold the channel mutex across [compute].
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
    /** Waiters blocked in [SyncDecision.Kind.WAIT]; completed after unlock via [wakeFlushBatch]. */
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
     * Offer this call as a participant and block until rendezvous succeeds or aborts.
     *
     * Loop: under the mutex, [pickCandidateLocked] returns a [SyncDecision]; we act on it
     * (wait / follow / compute+finish / abort). Cancel and close scrub [me] from [participants].
     */
    suspend fun sync(
        constraint: Optional<C>,
        anticonstraint: Optional<C>,
        select: Optional<Select> = Optional.empty(),
    ): SyncChannelResult<V> {
        val me = Participant<V, C>(constraint, anticonstraint, select)
        try {
            while (true) {
                // Another leader already committed us as follower while we were unlocked.
                if (me.followerValue.get()) {
                    return waitForLeaderOrAbort(me)
                }
                val nextDecision = withContext(NonCancellable) {
                    withChannelLock { pickCandidateLocked(me) }
                }
                when (nextDecision.kind) {
                    SyncDecision.Kind.ABORT -> {
                        // Channel closed, we were scrubbed, or we must leave without a value.
                        removeSelfAfterAbort(me)
                        return SyncChannelResult.abort()
                    }
                    SyncDecision.Kind.FOLLOW -> {
                        // Another participant reserved us as its peer (or we were already marked
                        // for delivery). Do not compute; wait for the leader's outcome on pairGate.
                        val gate = nextDecision.pairGate!!
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
                        // Pairing released without delivery — retry.
                        continue
                    }
                    SyncDecision.Kind.WAIT -> {
                        // No eligible peer yet: park until someone registers, pairs, or closes.
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
                            // pairing set while registering — loop to FOLLOW
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
                    SyncDecision.Kind.TRY -> {
                        // We are the leader for [nextDecision.group]: compute outside the mutex,
                        // then relock to revalidate and commit (or retry/abort).
                        val syncValue = try {
                            compute.invoke(nextDecision.constraints)
                        } catch (e: CancellationException) {
                            nextDecision.computeCleanup()
                            withContext(NonCancellable) {
                                withChannelLock {
                                    releasePairingLocked(nextDecision.group)
                                }
                            }
                            removeSelfAfterAbort(me)
                            throw e
                        } catch (e: Throwable) {
                            nextDecision.computeCleanup()
                            throw e
                        }
                        nextDecision.computeCleanup()
                        val finishDecision = withContext(NonCancellable) {
                            withChannelLock {
                                finishAfterComputeLocked(me, nextDecision.group, syncValue)
                            }
                        }
                        when (finishDecision.kind) {
                            SyncDecision.Kind.ABORT -> {
                                // Closed during compute, Select loser, or size-1 empty compute.
                                removeSelfAfterAbort(me)
                                return SyncChannelResult.abort()
                            }
                            SyncDecision.Kind.FOLLOW -> {
                                // Became a follower of another commit while we computed.
                                val gate = me.pairGate
                                if (gate != null && !gate.isCompleted) {
                                    try { gate.await() } catch (_: CancellationException) {}
                                }
                                return waitForLeaderOrAbort(me)
                            }
                            SyncDecision.Kind.RETRY -> {
                                // Peer gone, empty compute (peer rejected), or Select raced — try again.
                                yield()
                                continue
                            }
                            SyncDecision.Kind.SAT -> {
                                // Group removed under lock; deliver [V] to followers, return as leader.
                                val value = finishDecision.value!!
                                withContext(NonCancellable) {
                                    for (p in finishDecision.group) {
                                        if (p !== me) {
                                            try {
                                                p.syncValueChan.send(value)
                                            } catch (_: ClosedSendChannelException) {
                                                // Peer cancelled/closed between commit and delivery.
                                            }
                                        }
                                    }
                                }
                                return SyncChannelResult.sat(value)
                            }
                            SyncDecision.Kind.WAIT, SyncDecision.Kind.TRY -> error("unreachable finish kind: ${finishDecision.kind}")
                        }
                    }
                    SyncDecision.Kind.RETRY, SyncDecision.Kind.SAT -> error("unreachable pick kind: ${nextDecision.kind}")
                }
            }
        } catch (_: CancellationException) {
            removeSelfAfterAbort(me)
            return SyncChannelResult.abort()
        }
    }

    private suspend fun waitForLeaderOrAbort(me: Participant<V, C>): SyncChannelResult<V> {
        try {
            val syncVal = me.syncValueChan.receive()
            return SyncChannelResult.sat(syncVal)
        } catch (_: CancellationException) {
        } catch (_: ClosedReceiveChannelException) {
        }
        removeSelfAfterAbort(me)
        return SyncChannelResult.abort()
    }

    private suspend fun removeSelfAfterAbort(me: Participant<V, C>) {
        withContext(NonCancellable) {
            me.syncValueChan.close()
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
     * Under [mutex]: register [me] if needed, then decide the next [SyncDecision] for the sync loop.
     *
     * @param me this call's participant (may already be in [participants]).
     * @return ABORT / WAIT / FOLLOW / TRY (never RETRY or SAT — those come from finish).
     *   TRY carries a constraint snapshot + reserved [group] (+ cleanup for the snapshot).
     */
    private fun pickCandidateLocked(me: Participant<V, C>): SyncDecision<V, C> {
        // Committed followers must receive their value even if the peer already closed the
        // channel (e.g. TimerHelper exits and clearAffinity closes the session SyncChannel
        // before TimerController's Select arm finishes). Check followerValue before closed.
        if (me.followerValue.get()) {
            return SyncDecision(SyncDecision.Kind.FOLLOW, pairGate = me.pairGate ?: completedGate())
        }
        if (closed) return SyncDecision(SyncDecision.Kind.ABORT)
        if (me !in participants) {
            // Never re-register after scrub/commit/abort removal.
            if (me.everRegistered) return SyncDecision(SyncDecision.Kind.ABORT)
            me.everRegistered = true
            participants.add(me)
            signalWaitersLocked()
        }
        // Already reserved or value pending — do not become a competing leader.
        if (me.pairing) {
            return SyncDecision(SyncDecision.Kind.FOLLOW, pairGate = me.pairGate ?: completedGate())
        }
        if (syncSize == 1) {
            // Self-commit: reserve ourselves and leave the lock to compute.
            me.pairing = true
            me.pairGate = CompletableDeferred()
            val raw = constraintsOf(setOf(me))
            val (constraints, cleanup) = snapshotForCompute?.invoke(raw) ?: (raw to {})
            return SyncDecision(SyncDecision.Kind.TRY, constraints, setOf(me), computeCleanup = cleanup)
        }
        // Size 2: first eligible free peer (anti-ok, not rejected, not already pairing).
        for (peer in participants.toList()) {
            if (peer === me) continue
            if (peer.pairing || peer.followerValue.get()) continue
            if (peer in me.rejectedPeers) continue
            if (!antiOk(me, peer)) continue
            me.pairing = true
            peer.pairing = true
            val gate = CompletableDeferred<Unit>()
            me.pairGate = gate
            peer.pairGate = gate
            signalWaitersLocked()
            val raw = constraintsOf(setOf(me, peer))
            val (constraints, cleanup) = snapshotForCompute?.invoke(raw) ?: (raw to {})
            return SyncDecision(SyncDecision.Kind.TRY, constraints, setOf(me, peer), pairGate = gate, computeCleanup = cleanup)
        }
        return SyncDecision(SyncDecision.Kind.WAIT)
    }

    private fun completedGate(): CompletableDeferred<Unit> =
        CompletableDeferred<Unit>().also { it.complete(Unit) }

    /** Clear reservation and resume any peer waiting on [Participant.pairGate]. */
    private fun releasePairingLocked(group: Set<Participant<V, C>>) {
        val gates = mutableListOf<CompletableDeferred<Unit>>()
        for (p in group) {
            p.pairing = false
            p.pairGate?.let { gates.add(it) }
            p.pairGate = null
        }
        // Complete after pairing flags cleared; flush with wake batch if under withChannelLock.
        val batch = wakeFlushBatch
        if (batch != null) {
            // pairGates are not wakeWaiters — complete after unlock via a side list on batch by
            // piggybacking: complete now only if not holding re-entry risk. Gates don't re-enter
            // the channel mutex on resume until the await returns to the sync loop, so immediate
            // complete after unlock is safer — push onto wakeFlushBatch as Runnable-equivalent
            // by using wakeWaiters style: store gates in wakeFlushBatch only works for Unit
            // deferreds — pairGates are also CompletableDeferred<Unit>, so:
            batch.addAll(gates)
        } else {
            gates.forEach { it.complete(Unit) }
        }
        signalWaitersLocked()
    }

    /**
     * Under [mutex], after the leader's [compute] returned [syncValue]: revalidate the reserved
     * [group], run Select 2PL if needed, then commit (SAT) or back out (RETRY / ABORT / FOLLOW).
     *
     * @param me the leader that ran compute.
     * @param group participants reserved at TRY time (size 1: `{me}`; size 2: `{me, peer}`).
     * @param syncValue result of [compute] (empty ⇒ unsat for this group).
     * @return SAT with [SyncDecision.value]; RETRY to pick again; ABORT to leave; FOLLOW if we
     *   were removed as someone else's follower while computing.
     */
    private suspend fun finishAfterComputeLocked(
        me: Participant<V, C>,
        group: Set<Participant<V, C>>,
        syncValue: Optional<V>,
    ): SyncDecision<V, C> {
        // Scrubbed or closed while compute ran outside the lock.
        if (me !in participants) {
            releasePairingLocked(group)
            return if (me.followerValue.get()) {
                SyncDecision(SyncDecision.Kind.FOLLOW, pairGate = me.pairGate ?: completedGate())
            } else {
                SyncDecision(SyncDecision.Kind.ABORT)
            }
        }
        if (closed) {
            releasePairingLocked(group)
            removeParticipants(setOf(me))
            return SyncDecision(SyncDecision.Kind.ABORT)
        }
        val peers = group.filter { it !== me }
        // Peer cancelled/left during compute — release and try another peer.
        if (peers.any { it !in participants }) {
            releasePairingLocked(group)
            return SyncDecision(SyncDecision.Kind.RETRY)
        }
        if (syncValue.isEmpty) {
            if (syncSize == 1) {
                releasePairingLocked(group)
                removeParticipants(setOf(me))
                return SyncDecision(SyncDecision.Kind.ABORT)
            }
            // Remember unsat peer so we do not livelock retrying the same pair.
            peers.forEach { me.rejectedPeers.add(it) }
            releasePairingLocked(group)
            return SyncDecision(SyncDecision.Kind.RETRY)
        }
        // Select 2PL: all arms with a Select must still be able to win this channel.
        if (!selectsCommit(group)) {
            val stale = participants.filter { it.selectIsWon }.toSet()
            releasePairingLocked(group)
            for (p in stale) {
                p.syncValueChan.close()
            }
            removeParticipants(stale)
            signalWaitersLocked()
            return if (me in stale) SyncDecision(SyncDecision.Kind.ABORT) else SyncDecision(SyncDecision.Kind.RETRY)
        }
        // Commit: mark followers, drop the group from [participants], wake waiters.
        for (p in peers) {
            p.followerValue.set(true)
        }
        // Resume pairGate waiters so they move to value receive; keep gates completed.
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
        return SyncDecision(SyncDecision.Kind.SAT, emptySet(), group, syncValue.get())
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

    private fun constraintsOf(group: Set<Participant<V, C>>): Set<C> =
        group.filter { it.constraint.isPresent }.map { it.constraint.get() }.toSet()

    private fun removeParticipants(toRemove: Set<Participant<V, C>>) {
        if (toRemove.isEmpty()) return
        for (r in toRemove) {
            r.rejectedPeers.clear()
            r.pairing = false
        }
        for (p in participants) {
            p.rejectedPeers.removeAll(toRemove)
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
                    // Do not close a committed follower's delivery channel — they may still
                    // be in waitForLeaderOrAbort and must receive the buffered sync value.
                    if (!it.followerValue.get()) {
                        it.syncValueChan.close()
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
        /** Capacity 1 so the leader never blocks on follower scheduling. */
        var syncValueChan = Channel<V>(1)
        /** Peers for which this participant already saw empty [compute] (avoid livelock). */
        val rejectedPeers = mutableSetOf<Participant<V, C>>()
        var selectIsWon = false
        /** True while reserved in an in-flight TRY (leader or peer). */
        var pairing = false
        /** Completed when the leader finishes compute (success or release of the reservation). */
        var pairGate: CompletableDeferred<Unit>? = null
        var everRegistered = false
        /** Set under lock when a leader commits us as follower before remove. */
        val followerValue = AtomicBoolean(false)
    }

    /** Payload returned by pick/finish to drive the [sync] loop. */
    private class SyncDecision<V : Any, C : Any>(
        val kind: Kind,
        val constraints: Set<C> = emptySet(),
        val group: Set<Participant<V, C>> = emptySet(),
        val value: V? = null,
        val pairGate: CompletableDeferred<Unit>? = null,
        val computeCleanup: () -> Unit = {},
    ) {
        enum class Kind {
            /** Leave the channel (closed, scrubbed, Select loser, size-1 empty compute). */
            ABORT,
            /** No peer yet — park on the wake list. */
            WAIT,
            /** Leader: run compute outside the lock (carries snapshot constraints + group + cleanup). */
            TRY,
            /** Compute/revalidate failed for this peer — clear reservation and try again. */
            RETRY,
            /** Commit succeeded — deliver value (carries value + group). */
            SAT,
            /** Reserved as peer — await pairGate, then take the leader's value. */
            FOLLOW,
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
