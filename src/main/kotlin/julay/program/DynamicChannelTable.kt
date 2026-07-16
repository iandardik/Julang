package julay.program

import java.util.concurrent.ConcurrentHashMap

/**
 * Per-action registry of live dynamic [Channel]s for Z3 id → object recovery.
 * Owned by [Program]'s immutable index; each dynamic [SymbolicAction] gets one table.
 */
class DynamicChannelTable {
    private val byId = ConcurrentHashMap<Long, Channel>()

    fun register(id: Long, channel: Channel) {
        byId[id] = channel
    }

    fun lookup(id: Long): Channel =
        byId[id] ?: throw IllegalStateException("Unknown Channel id $id")

    fun unregister(id: Long) {
        byId.remove(id)
    }

    /** Number of channels still registered (open until [Channel.close] unregisters). */
    fun size(): Int = byId.size
}
