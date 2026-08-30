package julay.concurrency

import julay.tools.assert
import kotlinx.coroutines.CompletableDeferred
import java.lang.RuntimeException
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class Select(private vararg val cases : Case) {
    private val race = SelectRace()
    private val confirmedSignal = CompletableDeferred<Unit>()
    /** True while this Select runs [SyncChannel.runSelectCompute] — peers must not match other cases. */
    private val computeInFlight = AtomicBoolean(false)
    /** Mirror of [SelectRace] for tests / callbacks; set when race commits. */
    var winner = Optional.empty<Int>()
        private set

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

    fun canCommit(chanHash: Int): Boolean {
        val w = race.winnerHash()
        return w == null || w == chanHash
    }

    /** CAS EMPTY→COMMITTED; true if this channel owns the select (provisional until confirm). */
    fun tryRaceWin(channelHash: Int): Boolean {
        val ok = race.tryRaceWin(channelHash)
        if (ok) {
            winner = Optional.of(channelHash)
        }
        return ok
    }

    fun rollbackRaceWin(channelHash: Int) {
        race.rollbackRaceWin(channelHash)
        if (race.winnerHash() == null) {
            winner = Optional.empty()
        }
    }

    fun confirmRaceWin() {
        race.confirm()
        confirmedSignal.complete(Unit)
    }

    /** Provisional or confirmed race occupancy (for conflict detection). */
    fun hasRaceWinner(): Boolean = race.hasWinner()

    /** True only after a committed rendezvous flush. */
    fun isRaceConfirmed(): Boolean = race.isConfirmed()

    fun winnerHash(): Int? = race.winnerHash()

    fun beginCompute() {
        computeInFlight.set(true)
    }

    fun endCompute() {
        computeInFlight.set(false)
    }

    fun isComputeInFlight(): Boolean = computeInFlight.get()

    /** Completed when this Select confirms a winner — losing cases abort WAIT/FOLLOW. */
    fun confirmedSignal(): CompletableDeferred<Unit> = confirmedSignal

    fun noteChannelClosed() {
        exitDueToChannelClose = true
    }

    /**
     * Scramble / park-once / single-await via [SelectCoordinator].
     *
     * **Z3 / shared-Context invariant:** clone each case's constraints into a Case-local
     * ephemeral Context *before* constructing [SyncCase] / calling [run].
     */
    suspend fun run() {
        if (isRaceConfirmed()) {
            throw RuntimeException("Select run multiple times")
        }
        if (cases.isEmpty()) {
            return
        }
        val caseOffers = cases.map { case ->
            when (case) {
                is SyncCase<*, *> -> {
                    @Suppress("UNCHECKED_CAST")
                    val sc = case as SyncCase<Any, Any>
                    SelectCaseOffer(
                        channel = sc.getChannel(),
                        constraint = sc.getConstraint(),
                        anticonstraint = sc.getAnticonstraint(),
                        callback = { value ->
                            assert(
                                winner.get() == sc.getChannelHash(),
                                "Expected winning case to have the winning channel",
                            )
                            sc.invokeCallback(value)
                        },
                    )
                }
                else -> error("Unsupported Select case type: ${case::class}")
            }
        }
        @Suppress("UNCHECKED_CAST")
        SelectCoordinator.run(this, caseOffers as List<SelectCaseOffer<Any, Any>>)
    }

    interface Case {
        fun setSelect(s: Select)
        fun hasSelect(): Boolean
        fun getChannelHash(): Int
        suspend fun run()
    }

    class SyncCase<V : Any, C : Any>(
        private val chan: SyncChannel<C, V>,
        private val constraint: Optional<C> = Optional.empty(),
        private val anticonstraint: Optional<C> = Optional.empty(),
        private val callback: (V) -> Unit = {},
    ) : Case {
        private var selectRef = Optional.empty<Select>()
        constructor(chan: SyncChannel<C, V>, callback: (V) -> Unit = {})
            : this(chan, Optional.empty(), Optional.empty(), callback)
        constructor(
            chan: SyncChannel<C, V>,
            constraint: C,
            anticonstraint: C,
            callback: (V) -> Unit = {},
        ) : this(chan, Optional.of(constraint), Optional.of(anticonstraint), callback)

        override fun setSelect(s: Select) {
            selectRef = Optional.of(s)
        }
        override fun hasSelect() = selectRef.isPresent
        override fun getChannelHash() = chan.hashCode()

        internal fun getChannel() = chan
        internal fun getConstraint() = constraint
        internal fun getAnticonstraint() = anticonstraint
        internal fun invokeCallback(value: V) = callback.invoke(value)

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
