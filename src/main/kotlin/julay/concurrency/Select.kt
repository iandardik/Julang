package julay.concurrency

import julay.tools.assert
import java.lang.RuntimeException
import java.util.*

class Select(private vararg val cases : Case) {
    private val winnerMutex = StratifiedMutex()
    var winner = Optional.empty<Int>() // TODO make private
    /** Set when a SyncCase's SyncChannel closes; Select exits without waiting for other arms. */
    @Volatile
    internal var exitDueToChannelClose = false

    init {
        val numCases = cases.size
        val numChannels = cases.map { it.getChannelHash() }.toSet().size
        if (numCases != numChannels) {
            throw RuntimeException("Each Case in a Select must use a unique channel")
        }

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

    /** Called when a SyncCase's channel closes so Select can exit remaining arms promptly. */
    fun noteChannelClosed() {
        exitDueToChannelClose = true
    }

    /**
     * Registers all arms on their channels from one coroutine ([SelectCoordinator]), waits once
     * for a winner or close, then unregisters losers.
     *
     * **Z3 / shared-Context invariant:** If a case's constraint type [C] embeds live Z3 ASTs,
     * clone each case's constraints into a Case-local ephemeral Context *before* constructing
     * [SyncCase] / calling [run]. Julay does this in [julay.program.Proc.runOneStep].
     */
    suspend fun run() {
        if (winner.isPresent) {
            throw RuntimeException("Select run multiple times")
        }
        if (cases.isEmpty()) {
            return
        }

        val arms = cases.map { case ->
            when (case) {
                is SyncCase<*, *> -> {
                    @Suppress("UNCHECKED_CAST")
                    val sc = case as SyncCase<Any, Any>
                    SelectArm(
                        channel = sc.getChannel(),
                        constraint = sc.getConstraint(),
                        anticonstraint = sc.getAnticonstraint(),
                        callback = { value ->
                            assert(winner.get() == sc.getChannelHash(), "Expected winning case to have the winning channel")
                            sc.invokeCallback(value)
                        },
                    )
                }
                else -> error("Unsupported Select case type: ${case::class}")
            }
        }
        @Suppress("UNCHECKED_CAST")
        SelectCoordinator.run(this, arms as List<SelectArm<Any, Any>>)
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
        private val chan : SyncChannel<C, V>,
        private val constraint : Optional<C> = Optional.empty(),
        private val anticonstraint : Optional<C> = Optional.empty(),
        private val callback : (V)->Unit = {}
    ) : Case {
        private var selectRef = Optional.empty<Select>()
        constructor(chan : SyncChannel<C, V>, callback : (V)->Unit = {})
            : this(chan, Optional.empty(), Optional.empty(), callback) {}
        constructor(chan : SyncChannel<C, V>, constraint : C, anticonstraint : C, callback : (V)->Unit = {})
                : this(chan, Optional.of(constraint), Optional.of(anticonstraint), callback) {}
        override fun setSelect(s : Select) {
            selectRef = Optional.of(s)
        }
        override fun hasSelect() = selectRef.isPresent
        override fun getChannelHash() = chan.hashCode()

        internal fun getChannel() = chan
        internal fun getConstraint() = constraint
        internal fun getAnticonstraint() = anticonstraint
        internal fun invokeCallback(value: V) = callback.invoke(value)

        /**
         * Direct sync without a [Select] (single-offer Proc steps). Uses [SyncChannel.syncFast]
         * when both constraints are present; otherwise falls back to empty-Select [SyncChannel.sync].
         * Does not require [setSelect].
         */
        suspend fun syncDirect(onSat: (V) -> Unit = callback) {
            val ret = if (constraint.isPresent && anticonstraint.isPresent) {
                chan.syncFast(constraint.get(), anticonstraint.get())
            } else {
                chan.sync(constraint, anticonstraint, Optional.empty())
            }
            if (ret.isPresent) {
                onSat.invoke(ret.result.get())
            }
        }

        @Deprecated("Use SelectCoordinator via Select.run", level = DeprecationLevel.HIDDEN)
        override suspend fun run() {
            val select = selectRef.get()
            val ret = chan.sync(constraint, anticonstraint, selectRef)
            if (ret.isPresent) {
                assert(select.winner.get() == chan.hashCode(), "Expected winning case to have the winning channel")
                callback.invoke(ret.result.get())
            } else if (chan.isClosed()) {
                select.noteChannelClosed()
            }
        }
    }
}
