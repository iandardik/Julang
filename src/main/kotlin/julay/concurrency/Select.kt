package julay.concurrency

import julay.tools.assert
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import java.lang.RuntimeException
import java.util.*

class Select(private vararg val cases : Case) {
    private val caseDoneChan = Channel<Case>()
    private val winnerMutex = StratifiedMutex()
    var winner = Optional.empty<Int>() // TODO make private
    /** Set when a SyncCase's SyncChannel closes; Select exits without waiting for other arms. */
    @Volatile
    private var exitDueToChannelClose = false

    init {
        // check to make sure no two cases use the same channel
        val numCases = cases.size
        val numChannels = cases.map { it.getChannelHash() }.toSet().size
        if (numCases != numChannels) {
            throw RuntimeException("Each Case in a Select must use a unique channel")
        }

        // make sure that each cases isn't already associated with a select
        cases.forEach {
            if (it.hasSelect()) {
                throw RuntimeException("A Case must only be associated with a single Select")
            }
        }

        cases.forEach { it.setSelect(this) }
    }

    fun getWinnerMutex() = winnerMutex
    fun canCommit(chanHash : Int) : Boolean {
        return winner.isEmpty || winner.get() == chanHash
    }
    fun doCommit(chanHash : Int) {
        julay.tools.assert(canCommit(chanHash))
        winner = Optional.of(chanHash)
    }

    /** Called by [SyncCase] when its channel closes so Select cancels remaining arms promptly. */
    fun noteChannelClosed() {
        exitDueToChannelClose = true
    }

    /**
     * Runs all [cases] concurrently (one coroutine per case).
     *
     * **Z3 / shared-Context invariant:** If a case's constraint type [C] embeds live Z3 ASTs
     * (e.g. Julay [julay.program.Constraint] with a [com.microsoft.z3.BoolExpr]), those ASTs must
     * **not** share one Context across multiple cases. [run] launches cases on different
     * [SyncChannel]s in parallel; each channel may [com.microsoft.z3.Expr.translate] peer
     * constraints into a scratch Context. Concurrent translates from the same source Context
     * race on Z3 native state and can crash the JVM. Callers must clone each case's constraints
     * into a Case-local ephemeral Context *before* constructing [SyncCase] / calling [run].
     * Julay does this in [julay.program.Proc.runOneStep].
     */
    suspend fun run() {
        // make sure that run() is only ever run once
        if (winner.isPresent) {
            throw RuntimeException("Select run multiple times")
        }
        if (cases.isEmpty()) {
            return
        }

        // launch a coroutine for each case and listen on the channel. each routine attempts to "win" the select statement
        // by communicating with its channel first.
        coroutineScope {
            val jobs = cases.map { launch { it.run() } }
            val allCases = cases.toSet()
            val doneCases = mutableSetOf<Case>()
            var winnerCopy = Optional.empty<Int>()
            var winnerDone = false
            while (!winnerDone && !exitDueToChannelClose && doneCases != allCases) {
                val case = caseDoneChan.receive()
                doneCases.add(case)
                if (winnerCopy.isEmpty) {
                    winnerMutex.mutex.withLock { winnerCopy = winner }
                }
                winnerDone = winnerCopy.isPresent && winnerCopy.get() == case.getChannelHash()
            }
            // No winner is OK when a case's SyncChannel closed (exitDueToChannelClose) or all
            // arms aborted. Callers (e.g. Proc) rebuild Select after scrubbing closed sessions.
            // Cancel remaining arms, then join so SyncChannel participant cleanup finishes before
            // callers (e.g. Proc) close Z3 Contexts that own those constraints.
            jobs.forEach { it.cancelAndJoin() }
            caseDoneChan.close()
        }
    }


    interface Case {
        fun setSelect(s : Select)
        fun hasSelect() : Boolean
        fun getChannelHash() : Int
        suspend fun run()
    }
    /**
     * One Select arm on [chan]. For Z3-backed [C], pass constraints already cloned into a
     * Case-local Context (see [Select.run]); do not share one Context across multiple cases.
     */
    class SyncCase<V : Any, C : Any>(
        private val chan : SyncChannel<V, C>,
        private val constraint : Optional<C> = Optional.empty(),
        private val anticonstraint : Optional<C> = Optional.empty(),
        private val callback : (V)->Unit = {}
    ) : Case {
        private var selectRef = Optional.empty<Select>()
        constructor(chan : SyncChannel<V, C>, callback : (V)->Unit = {})
            : this(chan, Optional.empty(), Optional.empty(), callback) {}
        constructor(chan : SyncChannel<V, C>, constraint : C, anticonstraint : C, callback : (V)->Unit = {})
                : this(chan, Optional.of(constraint), Optional.of(anticonstraint), callback) {}
        override fun setSelect(s : Select) {
            selectRef = Optional.of(s)
        }
        override fun hasSelect() = selectRef.isPresent
        override fun getChannelHash() = chan.hashCode()
        override suspend fun run() {
            val select = selectRef.get()
            val ret = chan.sync(constraint, anticonstraint, selectRef)
            if (ret.isPresent) {
                assert(select.winner.get() == chan.hashCode(), "Expected winning case to have the winning channel")
                callback.invoke(ret.result.get())
            } else if (chan.isClosed()) {
                // Exit the whole Select so callers can rebuild cases (e.g. fall back to a
                // global session channel after a peer dies). Do not leave other arms waiting.
                select.noteChannelClosed()
            }
            try {
                select.caseDoneChan.send(this)
            }
            catch (_ : CancellationException) {}
            catch (_ : ClosedReceiveChannelException) {}
            catch (_ : ClosedSendChannelException) {}
        }
    }
}

