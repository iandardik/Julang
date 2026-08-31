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
 *   run Select race (CAS), remove the pair, and the leader resumes the follower's [Participant.valueCont].
 * - [syncSize] **1** (special): self-commit with the same compute-outside pattern (no peer).
 *
 * Callers that cannot form a group [WAIT] on [wakeWaiters] until registration/pairing/close
 * signals them. A peer reserved by a leader [FOLLOW]s: it awaits `pairGate`, then receives [V]
 * (or retries if the reservation was released without a value).
 *
 * Never hold the channel mutex across [compute]. Select commitment uses lock-free race CAS,
 * not StratifiedMutex 2PL.
 *
 * [syncFast] is the no-Select entry used by single-offer Proc steps; it shares the participant
 * table with [sync] so Select cases and syncFast callers can rendezvous.
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
    private var wakeFlushBatch: MutableList<() -> Unit>? = null
    @Volatile
    private var closed = false

    init {
        assert(
            syncSize == 1 || syncSize == 2,
            "SyncChannel syncSize must be 1 or 2 (got $syncSize)",
        )
    }

    private suspend inline fun <T> withChannelLock(block: suspend () -> T): T {
        val batch = mutableListOf<() -> Unit>()
        val result = mutex.withLock {
            wakeFlushBatch = batch
            try {
                block()
            } finally {
                wakeFlushBatch = null
            }
        }
        batch.forEach { it.invoke() }
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
     * [julay.program.Proc] FastOnly). Same participant table as [sync]; avoids allocating empty
     * [Optional] Select wrappers on the hot path.
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
        val decision = DecisionBuf<V, C>()
        try {
            while (true) {
                if (me.followerValue.get()) {
                    return waitForLeaderOrAbort(me)
                }
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
                        val wake = withContext(NonCancellable) {
                            withChannelLock {
                                if (closed || me !in participants || me.followerValue.get() || me.pairing) {
                                    return@withChannelLock null
                                }
                                if (me.select.isPresent && !me.select.get().canCommit(hashCode())) {
                                    return@withChannelLock null
                                }
                                val w = CompletableDeferred<Unit>()
                                wakeWaiters.add(w)
                                if (closed || me.followerValue.get() || me.pairing) {
                                    wakeWaiters.remove(w)
                                    return@withChannelLock null
                                }
                                if (me.select.isPresent && !me.select.get().canCommit(hashCode())) {
                                    wakeWaiters.remove(w)
                                    return@withChannelLock null
                                }
                                w
                            }
                        }
                        if (wake == null) {
                            if (closed || (me.select.isPresent && !me.select.get().canCommit(hashCode()))) {
                                removeSelfAfterAbort(me)
                                return SyncChannelResult.abort()
                            }
                            if (me.followerValue.get()) {
                                return waitForLeaderOrAbort(me)
                            }
                            continue
                        }
                        try {
                            val sel = me.select.orElse(null)
                            if (sel != null) {
                                var lost = false
                                kotlinx.coroutines.selects.select {
                                    wake.onAwait { }
                                    sel.confirmedSignal().onAwait { lost = true }
                                }
                                if (lost || !sel.canCommit(hashCode())) {
                                    removeSelfAfterAbort(me)
                                    return SyncChannelResult.abort()
                                }
                            } else {
                                wake.await()
                            }
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
                                for (p in decision.group) {
                                    if (p !== me) {
                                        p.completeValue(value)
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
            return SyncChannelResult.sat(me.awaitValue())
        } catch (_: CancellationException) {
        } catch (_: Exception) {
        }
        removeSelfAfterAbort(me)
        return SyncChannelResult.abort()
    }

    private suspend fun removeSelfAfterAbort(me: Participant<V, C>) {
        withContext(NonCancellable) {
            me.cancelValue()
            withChannelLock {
                if (!closed) {
                    releasePairingLocked(setOf(me))
                    removeParticipants(setOf(me))
                    signalWaitersLocked()
                }
            }
        }
    }

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
        val myHash = hashCode()
            if (me.select.isPresent) {
                val sel = me.select.get()
                if (!sel.canCommit(myHash) || sel.isComputeInFlight()) {
                    out.setAbort()
                    return
                }
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
            // Skip Selects already committed on another channel or mid-compute elsewhere.
            if (peer.select.isPresent) {
                val sel = peer.select.get()
                if (!sel.canCommit(myHash) || sel.isComputeInFlight()) continue
            }
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
        enqueueFlush { gates.forEach { it.complete(Unit) } }
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
            // Abort the whole reserved group — leaving a peer registered after close hangs it.
            for (p in group) {
                p.cancelValue()
            }
            releasePairingLocked(group)
            removeParticipants(group.filter { it in participants }.toSet())
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
                p.cancelValue()
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
        enqueueFlush { gates.forEach { it.complete(Unit) } }
        signalWaitersLocked()
        out.setSat(syncValue.get(), group)
        // Defer SelectGroup wake until after channel unlock — completing under the lock
        // deadlocks if the coordinator resumes and unregisterSelectCase re-enters the mutex.
        val value = syncValue.get()
        val groupsToComplete = group.mapNotNull { it.selectGroup }
        enqueueFlush {
            for (g in groupsToComplete) {
                g.tryCompleteWinner(value)
            }
        }
    }

    private fun enqueueFlush(action: () -> Unit) {
        val batch = wakeFlushBatch
        if (batch != null) {
            batch.add(action)
        } else {
            action()
        }
    }

    private fun signalWaitersLocked() {
        if (wakeWaiters.isNotEmpty()) {
            val copy = wakeWaiters.toList()
            wakeWaiters.clear()
            enqueueFlush { copy.forEach { it.complete(Unit) } }
        }
        // Wake SelectCoordinators parked in awaitCompletionOrNudge so they can tryLeadParked.
        val toNudge = participants.mapNotNull { it.selectGroup }.toSet()
        if (toNudge.isNotEmpty()) {
            enqueueFlush { toNudge.forEach { it.nudge() } }
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

    /**
     * Select commitment via ordered lock-free race (CAS EMPTY→channel hash, then confirm).
     * On conflict: rollback provisional winners; mark [Participant.selectIsWon] only for Selects
     * that already hold a *different* channel (won elsewhere) so finish can scrub them without
     * aborting plain sync peers or Selects that merely lost a dual-CAS after rollback.
     */
    private fun selectsCommit(group: Set<Participant<V, C>>): Boolean {
        val myHash = hashCode()
        val withSelect = group.filter { it.select.isPresent }
            .sortedBy { System.identityHashCode(it.select.get()) }
        if (withSelect.isEmpty()) return true
        val won = mutableListOf<Select>()
        for (p in withSelect) {
            p.selectIsWon = false
            val select = p.select.get()
            if (select.tryRaceWin(myHash)) {
                won.add(select)
            } else {
                // tryRaceWin fails only when this Select already owns another channel.
                p.selectIsWon = !select.canCommit(myHash)
                for (s in won) {
                    s.rollbackRaceWin(myHash)
                }
                return false
            }
        }
        for (s in won) {
            s.confirmRaceWin()
        }
        return true
    }

    suspend fun close() {
        withChannelLock {
            if (!closed) {
                closed = true
                val groupsToClose = mutableListOf<SelectGroup<V>>()
                participants.forEach {
                    // Cancel everyone still registered, including followers waiting on
                    // awaitValue(): skipping followers races with finishAfterComputeLocked
                    // (followerValue set, completeValue not yet) and leaves them hung.
                    it.cancelValue()
                    it.selectGroup?.let { g -> groupsToClose.add(g) }
                    it.pairGate?.complete(Unit)
                    it.pairGate = null
                    it.pairing = false
                }
                removeParticipants(participants.toSet())
                signalWaitersLocked()
                enqueueFlush {
                    for (g in groupsToClose) {
                        g.signalChannelClosed()
                    }
                }
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

    /** Stable id for cross-channel lock ordering in [SelectCoordinator]. */
    internal val channelId: Long = nextChannelId.getAndIncrement()

    internal val internalSyncSize: Int get() = syncSize

    /**
     * Go-style select offer: register under the channel lock.
     *
     * @param parkOnly when true (size-2), only register — do not reserve a peer. The coordinator
     *   leads via [tryLeadParked] one case at a time so a Select cannot NeedCompute on two
     *   channels at once (avoids dual-channel mutual race livelock).
     */
    internal suspend fun selectOffer(
        constraint: Optional<C>,
        anticonstraint: Optional<C>,
        select: Select,
        group: SelectGroup<V>,
        parkOnly: Boolean = false,
    ): SelectOfferResult<V, C> {
        return withChannelLock {
            selectOfferLocked(constraint, anticonstraint, select, group, parkOnly)
        }
    }

    private fun selectOfferLocked(
        constraint: Optional<C>,
        anticonstraint: Optional<C>,
        select: Select,
        group: SelectGroup<V>,
        parkOnly: Boolean,
    ): SelectOfferResult<V, C> {
        if (closed) {
            return SelectOfferResult.Closed()
        }
        val me = Participant(constraint, anticonstraint, Optional.of(select), group)
        me.everRegistered = true
        participants.add(me)
        signalWaitersLocked()
        if (syncSize == 1) {
            me.pairing = true
            me.pairGate = CompletableDeferred()
            val raw = constraintsOf(me, null)
            val (constraints, cleanup) = snapshotForCompute?.invoke(raw) ?: (raw to {})
            return SelectOfferResult.NeedCompute(me, setOf(me), constraints, cleanup)
        }
        if (parkOnly) {
            return SelectOfferResult.Parked(SelectCaseHandle(this, me))
        }
        val myHash = hashCode()
        if (me.select.isPresent && !me.select.get().canCommit(myHash)) {
            removeParticipants(setOf(me))
            return SelectOfferResult.Closed() // treat as done-elsewhere; coordinator checks race
        }
        for (peer in participants) {
            if (peer === me) continue
            if (peer.pairing || peer.followerValue.get()) continue
            if (me.isRejected(peer)) continue
            if (!antiOk(me, peer)) continue
            if (peer.select.isPresent) {
                val sel = peer.select.get()
                if (!sel.canCommit(myHash) || sel.isComputeInFlight()) continue
            }
            me.pairing = true
            peer.pairing = true
            val gate = CompletableDeferred<Unit>()
            me.pairGate = gate
            peer.pairGate = gate
            signalWaitersLocked()
            val raw = constraintsOf(me, peer)
            val (constraints, cleanup) = snapshotForCompute?.invoke(raw) ?: (raw to {})
            return SelectOfferResult.NeedCompute(me, setOf(me, peer), constraints, cleanup)
        }
        return SelectOfferResult.Parked(SelectCaseHandle(this, me))
    }

    /**
     * After [selectOffer] returned [SelectOfferResult.NeedCompute]: run [compute] outside the
     * lock, then finish (Select race + deliver).
     */
    internal suspend fun runSelectCompute(
        offer: SelectOfferResult.NeedCompute<V, C>,
    ): SyncChannelResult<V> {
        val syncValue = try {
            compute.invoke(offer.constraints)
        } catch (e: CancellationException) {
            offer.computeCleanup()
            withContext(NonCancellable) {
                withChannelLock { releasePairingLocked(offer.group) }
            }
            removeSelfAfterAbort(offer.me)
            throw e
        } catch (e: Throwable) {
            offer.computeCleanup()
            throw e
        }
        offer.computeCleanup()
        val decision = DecisionBuf<V, C>()
        withContext(NonCancellable) {
            withChannelLock {
                finishAfterComputeLocked(offer.me, offer.group, syncValue, decision)
            }
        }
        return when (decision.kind) {
            DecisionKind.SAT -> {
                val value = decision.value!!
                for (p in decision.group) {
                    if (p !== offer.me) {
                        p.completeValue(value)
                    }
                }
                SyncChannelResult.sat(value)
            }
            DecisionKind.ABORT -> {
                removeSelfAfterAbort(offer.me)
                SyncChannelResult.abort()
            }
            DecisionKind.FOLLOW -> {
                val gate = offer.me.pairGate
                if (gate != null && !gate.isCompleted) {
                    try { gate.await() } catch (_: CancellationException) {}
                }
                waitForLeaderOrAbort(offer.me)
            }
            DecisionKind.RETRY -> {
                // Still registered; caller treats as parked.
                SyncChannelResult.abort()
            }
            else -> error("unreachable finish kind: ${decision.kind}")
        }
    }

    /**
     * If [handle]'s participant is still free and an eligible peer exists, reserve a pair
     * (or size-1 self) and return [SelectOfferResult.NeedCompute]; else null.
     */
    internal suspend fun tryLeadParked(
        handle: SelectCaseHandle<V, C>,
    ): SelectOfferResult.NeedCompute<V, C>? {
        return withChannelLock {
            val me = handle.participant
            if (closed || me !in participants || me.pairing || me.followerValue.get()) {
                return@withChannelLock null
            }
            val myHash = hashCode()
            if (me.select.isPresent) {
                val sel = me.select.get()
                if (!sel.canCommit(myHash) || sel.isComputeInFlight()) {
                    return@withChannelLock null
                }
            }
            if (syncSize == 1) {
                me.pairing = true
                me.pairGate = CompletableDeferred()
                val raw = constraintsOf(me, null)
                val (constraints, cleanup) = snapshotForCompute?.invoke(raw) ?: (raw to {})
                return@withChannelLock SelectOfferResult.NeedCompute(me, setOf(me), constraints, cleanup)
            }
            for (peer in participants) {
                if (peer === me) continue
                if (peer.pairing || peer.followerValue.get()) continue
                if (me.isRejected(peer)) continue
                if (!antiOk(me, peer)) continue
                if (peer.select.isPresent) {
                    val sel = peer.select.get()
                    if (!sel.canCommit(myHash) || sel.isComputeInFlight()) continue
                }
                me.pairing = true
                peer.pairing = true
                val gate = CompletableDeferred<Unit>()
                me.pairGate = gate
                peer.pairGate = gate
                signalWaitersLocked()
                val raw = constraintsOf(me, peer)
                val (constraints, cleanup) = snapshotForCompute?.invoke(raw) ?: (raw to {})
                return@withChannelLock SelectOfferResult.NeedCompute(me, setOf(me, peer), constraints, cleanup)
            }
            null
        }
    }

    internal suspend fun isSelectCaseRegistered(handle: SelectCaseHandle<V, C>): Boolean =
        withChannelLock { handle.participant in participants }

    internal suspend fun unregisterSelectCase(handle: SelectCaseHandle<V, C>) {
        withContext(NonCancellable) {
            handle.participant.cancelValue()
            withChannelLock {
                if (!closed && handle.participant in participants) {
                    releasePairingLocked(setOf(handle.participant))
                    removeParticipants(setOf(handle.participant))
                    signalWaitersLocked()
                }
            }
        }
    }

    internal sealed class SelectOfferResult<V : Any, C : Any> {
        class Closed<V : Any, C : Any> : SelectOfferResult<V, C>()
        class Parked<V : Any, C : Any>(val handle: SelectCaseHandle<V, C>) : SelectOfferResult<V, C>()
        class NeedCompute<V : Any, C : Any>(
            val me: Participant<V, C>,
            val group: Set<Participant<V, C>>,
            val constraints: Set<C>,
            val computeCleanup: () -> Unit,
        ) : SelectOfferResult<V, C>()
    }

    internal class SelectCaseHandle<V : Any, C : Any>(
        val channel: SyncChannel<C, V>,
        val participant: Participant<V, C>,
    )

    internal class Participant<V : Any, C : Any>(
        val constraint: Optional<C>,
        val anticonstraint: Optional<C>,
        val select: Optional<Select>,
        val selectGroup: SelectGroup<V>? = null,
    ) {
        @Volatile
        private var pendingValue: V? = null
        @Volatile
        private var valueCont: CancellableContinuation<V>? = null
        /** Sticky: [cancelValue] may race before [awaitValue] suspends. */
        @Volatile
        private var valueCancelled = false

        suspend fun awaitValue(): V = suspendCancellableCoroutine { cont ->
            val early: V? = synchronized(this) {
                if (valueCancelled) {
                    cont.cancel(CancellationException())
                    return@suspendCancellableCoroutine
                }
                val pending = pendingValue
                if (pending != null) {
                    pendingValue = null
                    pending
                } else {
                    valueCont = cont
                    null
                }
            }
            if (early != null) {
                cont.resumeWith(Result.success(early))
                return@suspendCancellableCoroutine
            }
            cont.invokeOnCancellation {
                synchronized(this) {
                    if (valueCont === cont) {
                        valueCont = null
                    }
                }
            }
        }

        fun completeValue(value: V) {
            val cont = synchronized(this) {
                if (valueCancelled) {
                    return
                }
                val c = valueCont
                if (c != null) {
                    valueCont = null
                    c
                } else {
                    pendingValue = value
                    null
                }
            }
            cont?.resumeWith(Result.success(value))
        }

        fun cancelValue() {
            val cont = synchronized(this) {
                valueCancelled = true
                pendingValue = null
                val c = valueCont
                valueCont = null
                c
            }
            cont?.cancel(CancellationException())
        }

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

    companion object {
        private val nextChannelId = java.util.concurrent.atomic.AtomicLong(0)
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
