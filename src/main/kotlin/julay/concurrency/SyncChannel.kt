package julay.concurrency

import julay.tools.assert
import julay.tools.subsetsOfSize
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*

/**
 * V: The type of the value sent over the channel
 * C: The type of each constraint
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
        while (true) {
            val me = Participant<V,C>(constraint, anticonstraint, select)
            try {
                val result = syncAttempt(me)
                val retry = result.isRetry && select.isEmpty // never retry if there's a select--the select itself will retry
                if (!retry) {
                    return result
                }
            }
            finally {
                me.syncValueChan.close()
                mutex.withLock {
                    if (!closed) {
                        participants.forEach { p ->
                            p.compatiblePeers.remove(me)
                        }
                        participants.remove(me)
                    }
                }
            }
        }
    }

    private suspend fun syncAttempt(me : Participant<V,C>) : SyncChannelResult<V> {
        val (groupAttemptResult, syncValue, syncGroup) = compatibleGroupAttempt(me)
        if (!groupAttemptResult.isNone) {
            return groupAttemptResult
        }
        if (syncValue.isPresent) {
            assert(syncGroup.isPresent, "")
            val group = syncGroup.get()
            // TODO parallelize this?
            val commit = group.minus(me).all { p ->
                if (p.syncValueChan.isClosedForSend || p.syncValueChan.isClosedForReceive) {
                    return@all false
                }
                try {
                    p.syncValueChan.send(syncValue.get())
                    true
                }
                //catch (_ : CancellationException) { false }
                catch (_ : ClosedSendChannelException) { false }
            }

            return if (commit) {
                SyncChannelResult.sat(syncValue.get())
            } else {
                // this clean up is very important, but right now it's not exception safe
                // TODO make exception safe
                mutex.withLock {
                    if (!closed) {
                        participants.addAll(group.minus(me))
                    }
                }
                SyncChannelResult.retry()
            }
        }
        else {
            // at this point, the coroutine has failed to sync so we wait for someone else to lead the sync
            assert(syncGroup.isEmpty, "Expected an empty sync group")
            try {
                val syncVal = me.syncValueChan.receive()
                return SyncChannelResult.sat(syncVal)
            }
            catch (_ : CancellationException) {
                return SyncChannelResult.abort()
            }
            catch (_ : ClosedReceiveChannelException) {
                return SyncChannelResult.abort()
            }
        }
    }

    private suspend fun compatibleGroupAttempt(me : Participant<V,C>) : Triple<SyncChannelResult<V>, Optional<V>, Optional<Set<Participant<V,C>>>> {
        mutex.withLock {
            if (closed) {
                return Triple(SyncChannelResult.abort(), Optional.empty(), Optional.empty())
            }

            // calculate the participants who are compatible
            val compatiblePeers = participants
                .filter { p -> compatible(me, p) }
                .toSet()
                .plus(me)
            me.compatiblePeers.addAll(compatiblePeers)
            compatiblePeers.forEach { it.compatiblePeers.add(me) }
            participants.add(me)
            val compatibleGroups = participants
                .map { p -> me.compatiblePeers.intersect(p.compatiblePeers) }
                .filter { g -> g.size >= syncSize }
                .flatMap { g -> subsetsOfSize(g, syncSize) }
                .toSet()

            for (group in compatibleGroups) {
                assert(group.size == syncSize, "Expected group size (${group.size}) to be equal to the sync size ($syncSize)")
                val allSelectsCanCommit = selectsCommit(group)
                if (allSelectsCanCommit) {
                    // found a compatible group--return the group and a satisfying value
                    val syncGroup = Optional.of(group)
                    val constraints = group
                        .filter { it.constraint.isPresent }
                        .map { it.constraint.get() }
                        .toSet()
                    val syncValue = compute.invoke(constraints)
                    assert(syncValue.isPresent, "Expected a sync value to be present")
                    participants.removeAll(group)
                    return Triple(SyncChannelResult.none(), syncValue, syncGroup)
                }
                else {
                    // at least one participant is stale (has a select that has already been won). remove the stale
                    // participants and try the next compatible group
                    val staleParticipants = participants.filter { p -> p.selectIsWon }.toSet()
                    participants.removeAll(staleParticipants)
                    if (me in staleParticipants) {
                        // if me is a stale participant then abort
                        return Triple(SyncChannelResult.abort(), Optional.empty(), Optional.empty())
                    }
                }
            }

            // if we reach this point then there are no compatible groups yet, so we wait for someone else to lead the sync
            return Triple(SyncChannelResult.none(), Optional.empty(), Optional.empty())
        }
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
                participants.removeAll(participants)
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
        // compatible peers are pairwaise satisfiable
        val compatiblePeers = mutableSetOf<Participant<V,C>>()
        var selectIsWon = false
        override fun toString(): String {
            //return "(${select.get()},${select.get().winner})" // TODO
            return "num compat: ${compatiblePeers.size}"
        }
    }
}

data class SyncChannelResult<V : Any>(
    val isSAT : Boolean,
    val isUNSAT : Boolean,
    val isAborted : Boolean,
    val isRetry : Boolean,
    val result : Optional<V>
) {
    val isEmpty = result.isEmpty
    val isPresent = result.isPresent
    val isNone = !isSAT && !isUNSAT && !isAborted && !isRetry && result.isEmpty
    init {
        assert(!isPresent || (isSAT && !isUNSAT && !isAborted && !isRetry),
            "Invalid channel result, expected: isPresent => (isSAT && !isUNSAT && !isAborted && !isRetry)")
    }
    companion object {
        fun <V : Any> sat(value : V) : SyncChannelResult<V> {
            return SyncChannelResult(true, false, false, false, Optional.of(value))
        }
        fun <V : Any> unsat() : SyncChannelResult<V> {
            return SyncChannelResult(false, true, false, false, Optional.empty())
        }
        fun <V : Any> abort() : SyncChannelResult<V> {
            return SyncChannelResult(false, false, true, false, Optional.empty())
        }
        fun <V : Any> retry() : SyncChannelResult<V> {
            return SyncChannelResult(false, false, false, true, Optional.empty())
        }
        fun <V : Any> none() : SyncChannelResult<V> {
            return SyncChannelResult(false, false, false, false, Optional.empty())
        }
    }
}
