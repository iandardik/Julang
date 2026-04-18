package julay.concurrency

import kotlinx.coroutines.sync.Mutex
import java.util.concurrent.atomic.AtomicInteger

class StratifiedMutex(
    val isLocked: Boolean = false,
) : Comparable<StratifiedMutex> {
    private val id = globalId.getAndIncrement()
    val mutex = Mutex(isLocked)

    fun getId() : Int = id

    override fun compareTo(other: StratifiedMutex): Int {
        return getId().compareTo(other.getId())
    }
}

val globalId = AtomicInteger(0)