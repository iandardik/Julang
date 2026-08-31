package julay.concurrency

import java.util.Optional
import java.util.concurrent.ThreadLocalRandom

/**
 * Go-style Select: scramble case order, park-once [SyncChannel.selectOffer], at most one
 * [tryLeadParked] / [SyncChannel.runSelectCompute] per wake, then [SelectGroup.awaitCompletionOrNudge].
 * No per-case coroutines. Commit is lock-free [SelectRace] inside SyncChannel (never provisional-complete).
 *
 * While computing, other cases are unregistered and [Select.isComputeInFlight] is set so peers cannot
 * reserve this Select on another channel (avoids dual-channel race conflicts).
 */
object SelectCoordinator {
    /**
     * Run a multi-offer select using a fresh [Select.forCoordinator] shell (no [Select.SyncCase]
     * wrappers). Prefer this from [julay.program.Proc] FastOnly multi-offer steps.
     */
    suspend fun <V : Any, C : Any> runOffers(caseOffers: List<SelectCaseOffer<V, C>>) {
        if (caseOffers.isEmpty()) return
        run(Select.forCoordinator(), caseOffers, group = null)
    }

    suspend fun <V : Any, C : Any> run(
        select: Select,
        caseOffers: List<SelectCaseOffer<V, C>>,
        group: SelectGroup<V>? = null,
    ) {
        if (select.isRaceConfirmed()) {
            throw RuntimeException("Select run multiple times")
        }
        if (caseOffers.isEmpty()) {
            return
        }
        val channelHashes = HashSet<Int>(caseOffers.size)
        for (offer in caseOffers) {
            if (!channelHashes.add(offer.channel.hashCode())) {
                throw RuntimeException("Each Case in a Select must use a unique channel")
            }
        }

        val activeGroup = group ?: SelectGroup(select) { value ->
            val hash = select.winnerHash()
                ?: error("Select winner missing after commit")
            caseOffers.first { it.channel.hashCode() == hash }.callback(value)
        }

        val handles = ArrayList<SyncChannel.SelectCaseHandle<V, C>>(caseOffers.size)
        val order = ArrayList(caseOffers)
        scrambleInPlace(order)

        try {
            parkPass(order, select, activeGroup, handles)

            while (!activeGroup.isDone()) {
                pruneDeadHandlesInPlace(handles)
                reparkMissing(order, select, activeGroup, handles)
                if (activeGroup.isDone()) break

                pruneDeadHandlesInPlace(handles)
                // Peer reserved one of our cases — wait for commit or release; do not lead elsewhere.
                if (handles.any { it.participant.pairing || it.participant.followerValue.get() }) {
                    // Timed: stuck pairing with no nudge must not deadlock the Select.
                    activeGroup.awaitCompletionOrNudge(timeoutMs = 50)
                    continue
                }

                var computed = false
                // Iterate a scrambled copy so removeAll on [handles] is safe.
                val leadOrder = ArrayList(handles)
                scrambleInPlace(leadOrder)
                for (handle in leadOrder) {
                    if (activeGroup.isDone()) break
                    if (select.hasRaceWinner() && !select.canCommit(handle.channel.hashCode())) {
                        // Won elsewhere (provisional or confirmed) — leave so peers are not blocked.
                        // After rollbackRaceWin, hasRaceWinner clears and repark can re-offer.
                        handle.channel.unregisterSelectCase(handle)
                        handles.removeAll { it.participant === handle.participant }
                        activeGroup.nudge()
                        continue
                    }
                    val lead = handle.channel.tryLeadParked(handle) ?: continue
                    computed = true
                    val ret = runComputeExclusive(select, handles, handle, lead)
                    if (ret.isPresent || activeGroup.isDone()) break
                    if (!handle.channel.isSelectCaseRegistered(handle)) {
                        handles.removeAll { it.participant === handle.participant }
                    }
                    break // at most one compute per wake
                }
                if (activeGroup.isDone()) break

                pruneDeadHandlesInPlace(handles)
                if (handles.isEmpty()) {
                    // Stranded with a provisional win and no parks — clear so rematching works.
                    if (select.hasRaceWinner() && !select.isRaceConfirmed()) {
                        select.winnerHash()?.let { select.rollbackRaceWin(it) }
                        if (select.hasRaceWinner() && !select.isRaceConfirmed()) {
                            select.clearProvisionalRace()
                        }
                    }
                    kotlinx.coroutines.delay(1)
                    continue
                }
                if (!computed && select.hasRaceWinner() && !select.isRaceConfirmed()) {
                    // Parked only on the winning channel (or none) while peers wait elsewhere.
                    select.winnerHash()?.let { select.rollbackRaceWin(it) }
                    if (select.hasRaceWinner() && !select.isRaceConfirmed()) {
                        select.clearProvisionalRace()
                    }
                    // Avoid a tight yield spin when rematch still cannot form a pair.
                    kotlinx.coroutines.delay(1)
                    continue
                }
                if (select.isRaceConfirmed() && !activeGroup.isDone()) {
                    // Commit confirmed; wait briefly for tryCompleteWinner flush.
                    activeGroup.awaitCompletionOrNudge(timeoutMs = 50)
                    continue
                }
                if (computed) {
                    // Back off after race-fail RETRY / empty compute so peers can re-lead.
                    kotlinx.coroutines.yield()
                    if (activeGroup.isDone()) break
                    // Timed: a RETRY with no peer left on this channel must not await forever.
                    activeGroup.awaitCompletionOrNudge(timeoutMs = 5)
                } else {
                    // No lead possible (e.g. alone on each channel). Peer arrival on *this*
                    // channel nudges; a peer parked only on another channel does not. Timed
                    // wait recovers so reparkMissing can rematch across channels.
                    activeGroup.awaitCompletionOrNudge(timeoutMs = 5)
                }
            }
        } finally {
            for (handle in handles.sortedBy { it.channel.channelId }) {
                handle.channel.unregisterSelectCase(handle)
            }
            if (!activeGroup.isDone()) {
                activeGroup.signalNoWinner()
            }
        }
    }

    /**
     * Run compute with other cases unregistered so this Select cannot be matched on two channels.
     */
    private suspend fun <V : Any, C : Any> runComputeExclusive(
        select: Select,
        handles: MutableList<SyncChannel.SelectCaseHandle<V, C>>,
        active: SyncChannel.SelectCaseHandle<V, C>,
        lead: SyncChannel.SelectOfferResult.NeedCompute<V, C>,
    ): SyncChannelResult<V> {
        select.beginCompute()
        try {
            val others = handles.filter { it !== active }.sortedBy { it.channel.channelId }
            for (h in others) {
                h.channel.unregisterSelectCase(h)
            }
            handles.removeAll { it !== active }
            return active.channel.runSelectCompute(lead)
        } finally {
            select.endCompute()
        }
    }

    /**
     * First scrambled size-2 case uses [parkOnly]=false so an already-waiting peer can yield
     * [NeedCompute] immediately; remaining size-2 cases stay park-only (dual-channel safety).
     * Size-1 channels always try lead (unchanged).
     *
     * Opportunistic lead applies only when **every** case is size-2 (e.g. Protocol’s three RPC
     * channels). Mixed size-1 + size-2 Selects keep park-only on size-2 so size-1 self-commit
     * during parkPass is not starved by an accidental size-2 rendezvous.
     */
    private suspend fun <V : Any, C : Any> parkPass(
        order: List<SelectCaseOffer<V, C>>,
        select: Select,
        group: SelectGroup<V>,
        handles: MutableList<SyncChannel.SelectCaseHandle<V, C>>,
    ) {
        val allSize2 = order.all { it.channel.internalSyncSize != 1 }
        var triedOpportunisticLead = false
        for (caseOffer in order) {
            if (group.isDone()) break
            if (handles.any { it.channel === caseOffer.channel }) continue
            val parkOnly = when {
                caseOffer.channel.internalSyncSize == 1 -> false
                allSize2 && !triedOpportunisticLead -> {
                    triedOpportunisticLead = true
                    false
                }
                else -> true
            }
            offerOne(caseOffer, select, group, handles, parkOnly = parkOnly)
        }
    }

    private suspend fun <V : Any, C : Any> reparkMissing(
        order: List<SelectCaseOffer<V, C>>,
        select: Select,
        group: SelectGroup<V>,
        handles: MutableList<SyncChannel.SelectCaseHandle<V, C>>,
    ) {
        for (caseOffer in order) {
            if (group.isDone()) break
            if (handles.any { it.channel === caseOffer.channel }) continue
            if (select.hasRaceWinner() && !select.canCommit(caseOffer.channel.hashCode())) continue
            offerOne(caseOffer, select, group, handles, parkOnly = true)
        }
    }

    private suspend fun <V : Any, C : Any> offerOne(
        caseOffer: SelectCaseOffer<V, C>,
        select: Select,
        group: SelectGroup<V>,
        handles: MutableList<SyncChannel.SelectCaseHandle<V, C>>,
        parkOnly: Boolean,
    ) {
        when (
            val offer = caseOffer.channel.selectOffer(
                caseOffer.constraint,
                caseOffer.anticonstraint,
                select,
                group,
                parkOnly = parkOnly,
            )
        ) {
            is SyncChannel.SelectOfferResult.Closed -> {
                group.signalChannelClosed()
            }
            is SyncChannel.SelectOfferResult.Rejected -> {
                group.nudge()
            }
            is SyncChannel.SelectOfferResult.Parked -> {
                handles.add(offer.handle)
            }
            is SyncChannel.SelectOfferResult.NeedCompute -> {
                // Size-1 self-commit (or non-park lead): drop other parks for exclusive compute.
                select.beginCompute()
                val ret = try {
                    val others = handles.toList().sortedBy { it.channel.channelId }
                    for (h in others) {
                        h.channel.unregisterSelectCase(h)
                    }
                    handles.clear()
                    caseOffer.channel.runSelectCompute(offer)
                } finally {
                    select.endCompute()
                }
                if (ret.isPresent || group.isDone()) return
                val h = SyncChannel.SelectCaseHandle(caseOffer.channel, offer.me)
                if (caseOffer.channel.isSelectCaseRegistered(h)) {
                    handles.add(h)
                }
            }
        }
    }

    private suspend fun <V : Any, C : Any> pruneDeadHandlesInPlace(
        handles: MutableList<SyncChannel.SelectCaseHandle<V, C>>,
    ) {
        var w = 0
        for (i in handles.indices) {
            val h = handles[i]
            if (h.channel.isSelectCaseRegistered(h)) {
                if (w != i) {
                    handles[w] = h
                }
                w++
            }
        }
        while (handles.size > w) {
            handles.removeAt(handles.lastIndex)
        }
    }

    private fun <T> scrambleInPlace(items: MutableList<T>) {
        if (items.size <= 1) return
        val rnd = ThreadLocalRandom.current()
        for (i in items.lastIndex downTo 1) {
            val j = rnd.nextInt(i + 1)
            val tmp = items[i]
            items[i] = items[j]
            items[j] = tmp
        }
    }
}

/**
 * One select case for [SelectCoordinator]. Mutable so a long-lived [julay.program.Proc] can
 * recycle slots across FastOnly multi-offer steps instead of allocating wrappers each time.
 */
class SelectCaseOffer<V : Any, C : Any> {
    private var channelRef: SyncChannel<C, V>? = null
    var constraint: Optional<C> = Optional.empty()
        private set
    var anticonstraint: Optional<C> = Optional.empty()
        private set
    var callback: (V) -> Unit = {}
        private set

    val channel: SyncChannel<C, V>
        get() = channelRef ?: error("SelectCaseOffer used before fill")

    constructor()

    constructor(
        channel: SyncChannel<C, V>,
        constraint: Optional<C>,
        anticonstraint: Optional<C>,
        callback: (V) -> Unit = {},
    ) {
        fill(channel, constraint, anticonstraint, callback)
    }

    fun fill(
        channel: SyncChannel<C, V>,
        constraint: Optional<C>,
        anticonstraint: Optional<C>,
        callback: (V) -> Unit,
    ) {
        this.channelRef = channel
        this.constraint = constraint
        this.anticonstraint = anticonstraint
        this.callback = callback
    }
}
