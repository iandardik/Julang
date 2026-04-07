package exspecs.concurrency

import java.util.*
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

/**
 * V: The type of the value sent over the channel
 * C: The type of each constraint
 */
class SyncChannel<V : Any, C : Any>(
    private val syncSize : Int,
    private val compute : (Set<C>)->Optional<V>
) {
    // the shared variable used in the lobby
    private val lobbyLock = ReentrantLock()
    private val lobbyCond = lobbyLock.newCondition()
    private var participants = mutableSetOf<Participant<C>>()

    // the shared variables used for communication while attempting to sync
    private val comLock = ReentrantLock()
    private val comCond = comLock.newCondition()
    private var syncValue = SyncChannelResult.none<V>()
    private var commitVotes = 0
    private var aborted = false
    private var numExited = 0

    // the shared variable used for closing this channel
    private val closedLock = ReentrantLock()
    private var closed = false

    fun sync(select : Optional<Select> = Optional.empty(), retryOnUNSAT : Boolean = true) : SyncChannelResult<V> {
        return sync(Optional.empty(), select, retryOnUNSAT)
    }

    fun sync(constraint : C, select : Optional<Select> = Optional.empty(), retryOnUNSAT : Boolean = true) : SyncChannelResult<V> {
        return sync(Optional.of(constraint), select, retryOnUNSAT)
    }

    /**
     * This method will not check each constraint to see if it is satisfiable--that is up to the caller.
     */
    fun sync(constraint : Optional<C>, select : Optional<Select> = Optional.empty(), retryOnUNSAT : Boolean = true) : SyncChannelResult<V> {
        val me = Participant(constraint, select, Thread.currentThread())
        try {
            var attemptingSync = true
            var syncResult = SyncChannelResult.none<V>()
            while (attemptingSync) {
                // not the fairest policy to have each thread reenter the lobby on each retry
                val (enter, constraints, selects) = enterThroughLobby(me, retryOnUNSAT)
                if (!enter) {
                    return SyncChannelResult.abort()
                }

                // once we've made it here, attempt to sync
                val result = syncAttempt(select.isPresent, constraints, selects)

                // retry syncing under the following two conditions:
                // 1. the result is a retry
                // 2. the result is UNSAT and we're in retryOnUNSAT mode
                val retry = result.isRetry || (result.isUNSAT && retryOnUNSAT)
                if (!retry) {
                    attemptingSync = false
                    syncResult = result
                }
            }
            return syncResult
        }
        catch (e : InterruptedException) {
            handleGeneralInterrupt(me)
            return SyncChannelResult.abort()
        }
    }

    private fun enterThroughLobby(me : Participant<C>, retryOnUNSAT : Boolean) : Triple<Boolean,Set<C>,Set<Select>> {
        if (isClosed()) {
            return Triple(false, emptySet(), emptySet())
        }

        // wait to enter the channel
        lobbyLock.lock()
        try {
            // waiting in the "lobby" to get in
            while (participants.size == syncSize || (retryOnUNSAT && !satisfiableWithCurrentLobby(me))) {
                lobbyCond.await()
                if (isClosed()) {
                    return Triple(false, emptySet(), emptySet())
                }
            }

            // the thread has gotten "in", now wait until enough threads have also gotten in
            participants.add(me)
            if (participants.size == syncSize) {
                lobbyCond.signalAll()
            } else {
                lobbyCond.await()
                if (isClosed()) {
                    return Triple(false, emptySet(), emptySet())
                }
            }
            val constraints = participants
                .filter { it.constraint.isPresent }
                .map { it.constraint.get() }
                .toSet()
            val selects = participants
                .filter { it.select.isPresent }
                .map { it.select.get() }
                .toSet()
            return Triple(true, constraints, selects)
        }
        catch (e : InterruptedException) {
            // TODO clean up needs to happen here (this exception should be caught above and handled by
            //  handleGeneralInterrupt) but deleting this catch results in problems.
            return Triple(false, emptySet(), emptySet())
        }
        finally {
            lobbyLock.unlock()
        }
    }

    private fun syncAttempt(hasSelect : Boolean, constraints : Set<C>, selects : Set<Select>) : SyncChannelResult<V> {
        // the channel has been entered
        comLock.lock()
        try {
            // the first thread to enter this critical section will compute SAT on all formulas
            if (syncValue.isEmpty) {
                val computeResult = compute.invoke(constraints)
                syncValue = if (computeResult.isPresent) {
                    SyncChannelResult.sat(computeResult.get())
                } else {
                    SyncChannelResult.unsat()
                }
            }

            // attempt to commit to the value
            val commit = selectsCommit(selects)
            if (commit) {
                ++commitVotes
            }
            aborted = aborted || !commit
            // wait until an abort or enough votes to commit the value
            while (!aborted && commitVotes < syncSize) {
                // at this point, this thread is attempting to commit but doesn't have the votes yet to commit.
                // wait for the other threads to decide if they want to commit or abort.
                comCond.await()
                if (isClosed()) {
                    return SyncChannelResult.abort()
                }
            }
            comCond.signalAll()

            return if (commit && !aborted) {
                syncValue
            } else if (commit && hasSelect) {
                // never retry if there's a select--the select itself will retry
                SyncChannelResult.retry()
            } else {
                SyncChannelResult.abort()
            }
        }
        catch (e : InterruptedException) {
            aborted = true
            comCond.signalAll()
            return SyncChannelResult.abort()
        }
        finally {
            exitThroughTheLobby()
        }
    }

    /**
     * Returns whether the given constraint is mutually satisfiable with the current constraints in the lobby. Note that
     * we do not perform a check (and simply return true) if the current set of constraints is empty; this is safe based
     * on the assumption that each individual constraint is satisfiable, which we rely on for efficiency (to reduce the
     * number of calls to the SMT solver).
     */
    private fun satisfiableWithCurrentLobby(me : Participant<C>) : Boolean {
        val myConstraint = me.constraint
        val currentConstraints = participants
            .filter { it.constraint.isPresent }
            .map { it.constraint.get() }
            .toSet()
        if (myConstraint.isEmpty || currentConstraints.isEmpty()) {
            return true
        }
        return compute.invoke(currentConstraints.plus(myConstraint.get())).isPresent
    }

    /**
     * Assumes that the comLock is held and must be released
     */
    private fun exitThroughTheLobby() {
        var cleanup = false
        try {
            ++numExited
            if (numExited == syncSize) {
                // the last one out cleans up
                commitVotes = 0
                aborted = false
                syncValue = SyncChannelResult.none()
                numExited = 0
                cleanup = true
            }
        } finally {
            comLock.unlock()
        }

        // also clean up the participants data structure, which involves acquiring the lobbyLock
        if (cleanup) {
            lobbyLock.lock()
            try {
                participants = mutableSetOf()
                lobbyCond.signalAll() // tell everyone in the lobby that we're done
            }
            finally {
                lobbyLock.unlock()
            }
        }
    }

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
        lobbyLock.lock()
        try {
            lobbyCond.signalAll()
        } finally {
            lobbyLock.unlock()
        }
        comLock.lock()
        try {
            comCond.signalAll()
        } finally {
            comLock.unlock()
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

    private fun handleGeneralInterrupt(me : Participant<C>) {
        var issueAbort = false

        // remove me from the participant list if its in there
        lobbyLock.lock()
        try {
            if (me in participants) {
                if (participants.size < syncSize) {
                    // the sync attempt has not begun yet so it is safe to leave the participant list
                    participants.remove(me)
                } else if (participants.size == syncSize) {
                    // the sync attempt has begun, so simply removing me from the participant list will not stop the
                    // sync attempt. instead, we issue an abort.
                    issueAbort = true
                } else {
                    throw RuntimeException("This case is impossible")
                }
            }
        }
        finally {
            lobbyLock.unlock()
        }

        // issue an abort. essentially, this thread should have made it into a sync attempt but was interrupted, so we
        // exit the same way a sync attempt would.
        if (issueAbort) {
            comLock.lock()
            aborted = true
            exitThroughTheLobby()
        }
    }

    private data class Participant<C : Any>(
        val constraint : Optional<C>,
        val select : Optional<Select>,
        val thread : Thread,
    ) {}
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
    init {
        exspecs.tools.assert(!isPresent || (isSAT && !isUNSAT && !isAborted && !isRetry),
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
