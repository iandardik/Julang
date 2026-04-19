package julay.concurrency

import julay.tools.assert
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import java.lang.RuntimeException
import java.util.*

class Select(private vararg val cases : Case) {
    private val caseDoneChan = Channel<Case>()
    private val winnerMutex = StratifiedMutex()
    var winner = Optional.empty<Int>() // TODO make private

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
            while (!winnerDone && doneCases != allCases) {
                val case = caseDoneChan.receive()
                doneCases.add(case)
                if (winnerCopy.isEmpty) {
                    winnerMutex.mutex.withLock { winnerCopy = winner }
                }
                winnerDone = winnerCopy.isPresent && winnerCopy.get() == case.getChannelHash()
            }
            // It's possible to reach here and have winnerDone equal false. This would most likely be because all
            // channels have been closed. In this case, no case would fire; however--for now--we will not consider that
            // a bug.
            //assert(winnerDone, "Select $this expected a winner")
            jobs.forEach { it.cancel() }
            caseDoneChan.close()
        }
    }


    interface Case {
        fun setSelect(s : Select)
        fun hasSelect() : Boolean
        fun getChannelHash() : Int
        suspend fun run()
    }
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
            assert((ret.isPresent && ret.isSAT) || (!ret.isPresent && !ret.isSAT), "Expected ret.isPresent <=> ret.isSAT")
            if (ret.isPresent && ret.isSAT) {
                assert(select.winner.get() == chan.hashCode(), "Expected winning case to have the winning channel")
                callback.invoke(ret.result.get())
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

