package julay.concurrency

import julay.tools.assert
import julay.tools.subsetsOfSize
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*

/**
 * The sole mechanism for inter-process communication in Julay.
 *
 * All coordination between transition systems / procs must go through [SyncChannel]
 * (CSP or p2p rendezvous on program actions). Do not introduce shared caches, mutable
 * statics, or other shortcuts that let processes observe or affect each other outside
 * of sync. Pairwise 1:1 rendezvous is expressed with p2p actions (sync size 2); CSP
 * sync size is the full set of alphabet/constructor peers for that action.
 *
 * @param V The type of the value sent over the channel
 * @param C The type of each constraint
 */
class SyncChannel<V : Any, C : Any>(
    private val syncSize : Int,
    private val compute : (Set<C>)->Optional<V>
) {
    private val mutex = Mutex()
    private var participants = mutableSetOf<Participant<V,C>>()
    private var closed = false

    init {
        assert(syncSize > 0, "SyncChannel expected a positive number for the syncSize")
    }

    suspend fun sync(select : Optional<Select> = Optional.empty()) : SyncChannelResult<V> {
        return sync(Optional.empty(), Optional.empty(), select)
    }

    suspend fun sync(constraint : C, select : Optional<Select> = Optional.empty()) : SyncChannelResult<V> {
        return sync(Optional.of(constraint), Optional.empty(), select)
    }

    /**
     * This method will not check each constraint to see if it is satisfiable--that is up to the caller.
     */
    suspend fun sync(
        constraint : Optional<C>,
        anticonstraint : Optional<C>,
        select : Optional<Select> = Optional.empty()
    ) : SyncChannelResult<V> {
        val me = Participant<V,C>(constraint, anticonstraint, select)
        val (validResult, syncInfo) = withContext(NonCancellable) {
            // findCompatibleGroup() also performs important clean up of the participants list which should not be
            // canceled part way through. we also call this function with the mutex because it interacts with the
            // participants list, which is a shared data structure.
            mutex.withLock { findCompatibleGroup(me) }
        }
        if (!validResult) {
            return SyncChannelResult.abort()
        }
        if (syncInfo.isPresent) {
            // we found a big enough compatible group to sync so send everyone in the group that value
            val (syncValue, syncGroup) = syncInfo.get()
            syncGroup.minus(me).forEach { p -> p.syncValueChan.send(syncValue) } // TODO parallelize this?
            return SyncChannelResult.sat(syncValue)
        }
        else {
            // we didn't find a big enough compatible group to sync so we wait for someone else to lead the sync
            try {
                val syncVal = me.syncValueChan.receive()
                // Leader already removeParticipants(group) before send; do not clean up on success.
                return SyncChannelResult.sat(syncVal)
            }
            catch (_ : CancellationException) {
                // Select cancel of loser cases: convert to abort so SyncCase can finish cleanly.
                // (Rethrowing would skip abort return and confuse Select's cancelAndJoin path.)
            }
            catch (_ : ClosedReceiveChannelException) { }
            me.syncValueChan.close()
            mutex.withLock {
                if (!closed) {
                    removeParticipants(setOf(me))
                }
            }
            return SyncChannelResult.abort()
        }
    }

    /**
     * This function must be called with the mutex acquired.
     */
    private suspend fun findCompatibleGroup(me : Participant<V,C>) : Pair<Boolean, Optional<Pair<V, Set<Participant<V,C>>>>> {
        if (closed) {
            return Pair(false, Optional.empty())
        }

        // calculate the participants who are compatible
        val compatiblePeers = participants
            .filter { p -> compatible(me, p) }
            .toSet()
            .plus(me)
        me.compatiblePeers.addAll(compatiblePeers)
        compatiblePeers.forEach { it.compatiblePeers.add(me) }
        participants.add(me)
        // Only form groups from participants still waiting on this channel (compatiblePeers can
        // briefly disagree during cleanup; never sync against peers already removed).
        val compatibleGroups = participants
            .map { p ->
                me.compatiblePeers.intersect(p.compatiblePeers).intersect(participants)
            }
            .filter { g -> g.size >= syncSize }
            .flatMap { g -> subsetsOfSize(g, syncSize) }
            .toSet()

        for (group in compatibleGroups) {
            assert(group.size == syncSize, "Expected group size (${group.size}) to be equal to the sync size ($syncSize)")
            val allSelectsCanCommit = selectsCommit(group)
            if (allSelectsCanCommit) {
                // found a compatible group--return the group and a satisfying value
                val constraints = group
                    .filter { it.constraint.isPresent }
                    .map { it.constraint.get() }
                    .toSet()
                val syncValue = compute.invoke(constraints)
                assert(syncValue.isPresent, "Expected a sync value to be present")
                removeParticipants(group)
                return Pair(true, Optional.of(Pair(syncValue.get(), group)))
            }
            else {
                // at least one participant is stale (has a select that has already been won). remove the stale
                // participants and try the next compatible group
                val staleParticipants = participants.filter { p -> p.selectIsWon }.toSet()
                removeParticipants(staleParticipants)
                if (me in staleParticipants) {
                    // if me is a stale participant then abort
                    return Pair(false, Optional.empty())
                }
            }
        }

        // if we reach this point then there are no compatible groups yet, so we wait for someone else to lead the sync
        return Pair(true, Optional.empty())
    }

    /**
     * Drop [toRemove] from [participants] and from every remaining peer's [Participant.compatiblePeers].
     * Plain removeAll leaves stale peers in compatiblePeers; those can still be picked into a sync
     * group and translated after their owning Proc Context has closed.
     */
    private fun removeParticipants(toRemove: Set<Participant<V,C>>) {
        if (toRemove.isEmpty()) return
        for (p in participants) {
            p.compatiblePeers.removeAll(toRemove)
        }
        for (r in toRemove) {
            r.compatiblePeers.clear()
        }
        participants.removeAll(toRemove)
    }

    private fun compatible(p1 : Participant<V,C>, p2 : Participant<V,C>) : Boolean {
        // empty constraints are treated as TRUE
        val satConstraints = p1.constraint.isEmpty || p2.constraint.isEmpty ||
                pairwiseSatisfiable(p1.constraint.get(), p2.constraint.get())
        // empty anticonstraints are treated as FALSE
        val unsatAnticonstraints = p1.anticonstraint.isEmpty || p2.anticonstraint.isEmpty ||
                !pairwiseSatisfiable(p1.anticonstraint.get(), p2.anticonstraint.get())
        return satConstraints && unsatAnticonstraints
    }
    private fun pairwiseSatisfiable(c1 : C, c2 : C) = compute.invoke(setOf(c1, c2)).isPresent

    private suspend fun selectsCommit(group : Set<Participant<V,C>>) : Boolean {
        // request a commit from all parties--only commit if all are able to 2PL on all selects
        val myHash = hashCode()
        var allCanCommit : Boolean
        val selectGroup = group.filter { p -> p.select.isPresent }
        val allMutexes = selectGroup.map { p -> p.select.get().getWinnerMutex() }.sorted()
        var lockedMutexes = emptyList<StratifiedMutex>()
        try {
            lockedMutexes = allMutexes.map {
                it.mutex.lock()
                it
            }
            assert(lockedMutexes == allMutexes, "Expected all locked mutexes")
            allCanCommit = selectGroup.all { p ->
                val select = p.select.get()
                val selectCanCommit = select.canCommit(myHash)
                p.selectIsWon = !selectCanCommit
                selectCanCommit
            }
            if (allCanCommit) {
                selectGroup.forEach { it.select.get().doCommit(myHash) }
            }
        }
        finally {
            lockedMutexes.forEach { it.mutex.unlock() }
        }
        return allCanCommit
    }

    suspend fun close() {
        mutex.withLock {
            if (!closed) {
                closed = true
                participants.forEach { it.syncValueChan.close() }
                removeParticipants(participants.toSet())
            }
        }
    }

    suspend fun isClosed() : Boolean {
        mutex.withLock {
            return closed
        }
    }

    private class Participant<V : Any, C : Any>(
        val constraint : Optional<C>,
        val anticonstraint : Optional<C>,
        val select : Optional<Select>,
    ) {
        var syncValueChan = Channel<V>()
        // compatible peers are pairwise satisfiable
        val compatiblePeers = mutableSetOf<Participant<V,C>>()
        var selectIsWon = false
    }
}

data class SyncChannelResult<V : Any>(
    val result : Optional<V>
) {
    val isEmpty = result.isEmpty
    val isPresent = result.isPresent
    companion object {
        fun <V : Any> sat(value : V) : SyncChannelResult<V> {
            return SyncChannelResult(Optional.of(value))
        }
        fun <V : Any> abort() : SyncChannelResult<V> {
            return SyncChannelResult(Optional.empty())
        }
    }
}
