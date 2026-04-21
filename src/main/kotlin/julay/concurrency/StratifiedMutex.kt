package julay.concurrency

import kotlinx.coroutines.sync.Mutex
import java.util.concurrent.atomic.AtomicLong

class StratifiedMutex(
    val isLocked: Boolean = false,
) : Comparable<StratifiedMutex> {
    private val id = globalId.getAndIncrement()
    val mutex = Mutex(isLocked)

    fun getId() : Long = id

    override fun compareTo(other: StratifiedMutex): Int {
        return getId().compareTo(other.getId())
    }
}

val globalId = AtomicLong(0)