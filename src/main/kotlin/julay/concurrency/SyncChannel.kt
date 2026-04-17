package julay.concurrency

import julay.tools.assert
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*
import java.util.Collections.max
import java.util.concurrent.locks.ReentrantLock

suspend fun run(name : String, num : Int, chan : SyncChannel<Int,Int>) {
    val result = chan.sync(num)
    println("$name: $result")
}

suspend fun main(args : Array<String>) {
    withContext(Dispatchers.Default) {
        val chan = SyncChannel<Int,Int>(2) { numbers ->
            Optional.of(max(numbers))
        }
        launch { run("A", 1, chan) }
        launch { run("B", 2, chan) }
        launch { run("C", 1, chan) }
        launch { run("D", 2, chan) }
    }
}

/**
 * V: The type of the value sent over the channel
 * C: The type of each constraint
 */
class SyncChannel<V : Any, C : Any>(
    private val syncSize : Int,
    private val compute : (Set<C>)->Optional<V>
) {
    val mutex = Mutex()
    private var participants = mutableSetOf<Participant<V,C>>()

    // the shared variable used for closing this channel
    private val closedLock = ReentrantLock()
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
        while (true) {
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
                    participants.forEach { p ->
                        p.compatiblePeers.remove(me)
                    }
                    participants.remove(me)
                }
                me.syncValueChan = Channel()
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
            // TODO parallelize this
            val commit = group.minus(me).all { p ->
                if (p.syncValueChan.isClosedForSend || p.syncValueChan.isClosedForReceive) {
                    return@all false
                }
                try {
                    p.syncValueChan.send(syncValue.get())
                    true
                }
                catch (_ : CancellationException) { false }
                catch (_ : ClosedSendChannelException) { false }
            }

            return if (commit) {
                SyncChannelResult.sat(syncValue.get())
            } else {
                // this clean up is very important, but right now it's not exception safe
                // TODO make exception safe
                mutex.withLock {
                    participants.addAll(group.minus(me))
                }
                SyncChannelResult.retry()
            }
        }
        else {
            // at this point, the coroutine has failed to sync so we wait for someone else to lead the sync
            assert(syncGroup.isEmpty, "")
            try {
                val syncVal = me.syncValueChan.receive()
                return SyncChannelResult.sat(syncVal)
            }
            catch (_ : CancellationException) { return SyncChannelResult.abort() }
            catch (_ : ClosedReceiveChannelException) { return SyncChannelResult.abort() }
        }
    }

    private suspend fun compatibleGroupAttempt(me : Participant<V,C>) : Triple<SyncChannelResult<V>, Optional<V>, Optional<Set<Participant<V,C>>>> {
        mutex.withLock {
            // calculate the participants who are compatible
            val compatiblePeers = participants
                .filter { p -> compatible(me, p) }
                .toSet()
            me.compatiblePeers.addAll(compatiblePeers)
            val compatibleGroup = participants
                .map { p ->
                    // we haven't added me to the set of compatible peers for each participant yet, so we calculate "setOfUs" separately
                    val intersection = me.compatiblePeers.intersect(p.compatiblePeers)
                    val setOfUs = if (p in me.compatiblePeers) setOf(me,p) else setOf()
                    intersection union setOfUs
                }
                .firstOrNull { g -> g.size >= syncSize }
            val foundCompatibleGroup = syncSize == 1 || compatibleGroup != null
            if (foundCompatibleGroup) {
                //println("will sync: $me")
                val group = if (syncSize == 1) setOf(me) else compatibleGroup!!.toSet()
                assert(group.size == syncSize, "Expected group size to be equal to the sync size")
                val selects = group
                    .filter { it.select.isPresent }
                    .map { it.select.get() }
                    .toSet()
                if (!selectsCommit(selects)) {
                    val mySelectCanCommit = false // TODO
                    val result = if (mySelectCanCommit) {
                        SyncChannelResult.retry<V>()
                    } else {
                        SyncChannelResult.abort<V>()
                    }
                    return Triple(result, Optional.empty(), Optional.empty())
                }

                participants.removeAll(group)
                val syncGroup = Optional.of(group)

                val constraints = group
                    .filter { it.constraint.isPresent }
                    .map { it.constraint.get() }
                    .toSet()
                val syncValue = compute.invoke(constraints)
                assert(syncValue.isPresent, "Expected a sync value to be present")
                return Triple(SyncChannelResult.none(), syncValue, syncGroup)
            }
            else {
                //println("didn't sync: $me")
                // at this point, the coroutine has failed to sync so we wait for someone else to lead the sync
                participants.add(me)
                return Triple(SyncChannelResult.none(), Optional.empty(), Optional.empty())
            }
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

    private fun selectsCommit(selects : Set<Select>) : Boolean {
        // request a commit from all parties--only commit if all are able to
        // 2PL on all selects
        val allLocks = selects.map { it.getPublicLock() }.sorted()
        try {
            allLocks.forEach { it.lock() }
            val allCanCommit = selects.all { it.canCommit(hashCode()) }
            if (allCanCommit) {
                selects.forEach { it.doCommit(hashCode()) }
            }
            return allCanCommit
        }
        finally {
            allLocks.forEach {
                if (it.isLocked()) {
                    it.unlock()
                }
            }
        }
    }

    fun close() {
        closedLock.lock()
        try {
            closed = true
        } finally {
            closedLock.unlock()
        }
    }

    // this function is called internally when the lobbyLock / comLock is already acquired, which is safe because we
    // don't do anything with conditions (i.e. await() / signal()) with the closedLock.
    fun isClosed() : Boolean {
        closedLock.lock()
        try {
            if (closed) {
                return true
            }
        }
        finally {
            closedLock.unlock()
        }
        return false
    }

    private class Participant<V : Any, C : Any>(
        val constraint : Optional<C>,
        val anticonstraint : Optional<C>,
        val select : Optional<Select>,
    ) {
        var syncValueChan = Channel<V>()
        // compatible peers are pairwaise satisfiable
        val compatiblePeers = mutableSetOf<Participant<V,C>>()
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
        julay.tools.assert(!isPresent || (isSAT && !isUNSAT && !isAborted && !isRetry),
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
