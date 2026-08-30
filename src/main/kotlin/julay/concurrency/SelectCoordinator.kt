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
    suspend fun <V : Any, C : Any> run(
        select: Select,
        caseOffers: List<SelectCaseOffer<V, C>>,
    ) {
        if (select.isRaceConfirmed()) {
            throw RuntimeException("Select run multiple times")
        }
        if (caseOffers.isEmpty()) {
            return
        }

        val group = SelectGroup<V>(select) { value ->
            val hash = select.winner.get()
            caseOffers.first { it.channel.hashCode() == hash }.callback(value)
        }

        val handles = mutableListOf<SyncChannel.SelectCaseHandle<V, C>>()
        val order = scramble(caseOffers)

        try {
            parkPass(order, select, group, handles)

            while (!group.isDone()) {
                pruneDeadHandles(handles)
                reparkMissing(order, select, group, handles)
                if (group.isDone()) break

                pruneDeadHandles(handles)
                // Peer reserved one of our cases — wait for commit or release; do not lead elsewhere.
                if (handles.any { it.participant.pairing || it.participant.followerValue.get() }) {
                    group.awaitCompletionOrNudge()
                    continue
                }

                var computed = false
                for (handle in scramble(handles.toList())) {
                    if (group.isDone()) break
                    if (select.hasRaceWinner() && !select.canCommit(handle.channel.hashCode())) {
                        // Won elsewhere (provisional or confirmed) — leave so peers are not blocked.
                        // After rollbackRaceWin, hasRaceWinner clears and repark can re-offer.
                        handle.channel.unregisterSelectCase(handle)
                        handles.removeAll { it.participant === handle.participant }
                        continue
                    }
                    val lead = handle.channel.tryLeadParked(handle) ?: continue
                    computed = true
                    val ret = runComputeExclusive(select, handles, handle, lead)
                    if (ret.isPresent || group.isDone()) break
                    if (!handle.channel.isSelectCaseRegistered(handle)) {
                        handles.removeAll { it.participant === handle.participant }
                    }
                    break // at most one compute per wake
                }
                if (group.isDone()) break

                pruneDeadHandles(handles)
                if (handles.isEmpty()) {
                    kotlinx.coroutines.yield()
                    continue
                }
                if (computed) {
                    // Back off after race-fail RETRY / empty compute so peers can re-lead.
                    kotlinx.coroutines.yield()
                    if (group.isDone()) break
                }
                group.awaitCompletionOrNudge()
            }
        } finally {
            for (handle in handles.sortedBy { it.channel.channelId }) {
                handle.channel.unregisterSelectCase(handle)
            }
            if (!group.isDone() && !select.isRaceConfirmed()) {
                group.signalNoWinner()
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

    private suspend fun <V : Any, C : Any> parkPass(
        order: List<SelectCaseOffer<V, C>>,
        select: Select,
        group: SelectGroup<V>,
        handles: MutableList<SyncChannel.SelectCaseHandle<V, C>>,
    ) {
        for (caseOffer in order) {
            if (group.isDone()) break
            if (handles.any { it.channel === caseOffer.channel }) continue
            offerOne(caseOffer, select, group, handles, parkOnly = caseOffer.channel.internalSyncSize != 1)
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

    private suspend fun <V : Any, C : Any> pruneDeadHandles(
        handles: MutableList<SyncChannel.SelectCaseHandle<V, C>>,
    ) {
        val live = ArrayList<SyncChannel.SelectCaseHandle<V, C>>(handles.size)
        for (h in handles) {
            if (h.channel.isSelectCaseRegistered(h)) {
                live.add(h)
            }
        }
        handles.clear()
        handles.addAll(live)
    }

    private fun <T> scramble(items: List<T>): List<T> {
        if (items.size <= 1) return items
        val copy = items.toMutableList()
        val rnd = ThreadLocalRandom.current()
        for (i in copy.lastIndex downTo 1) {
            val j = rnd.nextInt(i + 1)
            val tmp = copy[i]
            copy[i] = copy[j]
            copy[j] = tmp
        }
        return copy
    }
}

data class SelectCaseOffer<V : Any, C : Any>(
    val channel: SyncChannel<C, V>,
    val constraint: Optional<C>,
    val anticonstraint: Optional<C>,
    val callback: (V) -> Unit = {},
)
