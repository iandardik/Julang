package julay.concurrency

import java.util.Optional
import java.util.concurrent.ThreadLocalRandom

/**
 * Go-style Select: scramble case order, park-once [SyncChannel.selectOffer], at most one
 * [tryLeadParked] / [SyncChannel.runSelectCompute] per wake, then [SelectGroup.awaitCompletionOrNudge].
 * No per-case coroutines. Select-vs-Select commit stays StratifiedMutex 2PL inside SyncChannel.
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
                var computed = false
                for (handle in scramble(handles.toList())) {
                    if (group.isDone()) break
                    if (select.hasRaceWinner() && !select.canCommit(handle.channel.hashCode())) {
                        handle.channel.unregisterSelectCase(handle)
                        handles.removeAll { it.participant === handle.participant }
                        continue
                    }
                    val lead = handle.channel.tryLeadParked(handle) ?: continue
                    computed = true
                    val ret = handle.channel.runSelectCompute(lead)
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
                // Size-1 self-commit (or non-park lead).
                val ret = caseOffer.channel.runSelectCompute(offer)
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
