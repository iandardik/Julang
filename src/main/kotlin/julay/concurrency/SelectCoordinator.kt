package julay.concurrency

import kotlinx.coroutines.*
import java.util.Optional
import kotlin.coroutines.coroutineContext

/**
 * Multi-channel [Select] via [SelectGroup]: one coroutine per arm (active [sync]), a single
 * wait on the group, and no [kotlinx.coroutines.channels.Channel] / [cancelAndJoin] teardown.
 *
 * Size-2 multi-arm procs (e.g. Protocol) still register passively when [usePassiveSize2Arms]
 * is enabled; default uses active [sync] for correctness with all pairing edge cases.
 */
object SelectCoordinator {
    /** Passive registration for size-2 multi-arm selects (fewer coroutines when safe). */
    var usePassiveSize2Arms: Boolean = false

    suspend fun <V : Any, C : Any> run(
        select: Select,
        arms: List<SelectArm<V, C>>,
    ) {
        if (select.winner.isPresent) {
            throw RuntimeException("Select run multiple times")
        }
        if (arms.isEmpty()) {
            return
        }

        val group = SelectGroup<V>(select, { value ->
            arms.first { it.channel.hashCode() == select.winner.get() }
                .callback(value)
        }, arms.size)

        val passive = mutableListOf<SyncChannel.SelectArmHandle<V, C>>()
        val activeJobs = mutableListOf<Job>()
        val supervisor = SupervisorJob()
        val scope = CoroutineScope(coroutineContext + supervisor)

        try {
            for (arm in arms.sortedBy { it.channel.channelId }) {
                val usePassive = usePassiveSize2Arms &&
                    arm.channel.internalSyncSize == 2 &&
                    arms.size > 1
                if (usePassive) {
                    val handle = arm.channel.registerSelectArm(
                        arm.constraint,
                        arm.anticonstraint,
                        select,
                        group,
                    )
                    if (handle == null) {
                        group.signalChannelClosed()
                        group.armFinished()
                    } else {
                        passive.add(handle)
                    }
                } else {
                    activeJobs.add(
                        scope.launch {
                            runActiveArm(arm, select, group)
                        },
                    )
                }
            }

            group.awaitCompletion()
        } finally {
            activeJobs.forEach { it.cancelAndJoin() }
            supervisor.cancel()
            for (handle in passive) {
                handle.channel.unregisterSelectArm(handle)
                group.armFinished()
            }
        }
    }

    private suspend fun <V : Any, C : Any> runActiveArm(
        arm: SelectArm<V, C>,
        select: Select,
        group: SelectGroup<V>,
    ) {
        try {
            val ret = arm.channel.sync(arm.constraint, arm.anticonstraint, Optional.of(select))
            if (ret.isPresent) {
                group.tryCompleteWinner(ret.result.get())
            } else if (arm.channel.isClosed()) {
                group.signalChannelClosed()
            }
        } finally {
            group.armFinished()
        }
    }
}

data class SelectArm<V : Any, C : Any>(
    val channel: SyncChannel<C, V>,
    val constraint: Optional<C>,
    val anticonstraint: Optional<C>,
    val callback: (V) -> Unit = {},
)
