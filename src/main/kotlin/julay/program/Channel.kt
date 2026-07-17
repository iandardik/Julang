package julay.program

import com.microsoft.z3.Context
import com.microsoft.z3.Expr
import com.microsoft.z3.IntNum
import com.microsoft.z3.Model
import julay.concurrency.SyncChannel
import java.util.concurrent.atomic.AtomicLong

/**
 * First-class dynamic sync channel. Live instances wrap a [SyncChannel] for one action
 * rendezvous; [empty] is a branded sentinel for state initialization and cannot be synced on.
 *
 * Creators (Jul procs via createChannel, or library procs) should [close] / [closeChannel]
 * after the protocol finishes. Channel ids are recovered at sync time from [Constraint] bags,
 * not from a Program-wide table.
 */
class Channel private constructor(
    val id: Long,
    val actionName: String,
    val ownerAction: SymbolicAction?,
    @Volatile private var syncChannel: SyncChannel<ConcreteAction, Constraint>?,
    @Volatile private var closed: Boolean,
    private val onClose: (() -> Unit)?,
) {
    companion object {
        const val EMPTY_ID = 0L
        private val nextId = AtomicLong(1)

        fun empty(actionName: String): Channel =
            Channel(EMPTY_ID, actionName, ownerAction = null, syncChannel = null, closed = true, onClose = null)

        fun create(
            syncChannel: SyncChannel<ConcreteAction, Constraint>,
            ownerAction: SymbolicAction,
            onClose: (() -> Unit)? = null,
        ): Channel {
            val id = nextId.getAndIncrement()
            return Channel(
                id,
                ownerAction.name,
                ownerAction,
                syncChannel,
                closed = false,
                onClose,
            )
        }
    }

    fun isEmpty(): Boolean = id == EMPTY_ID

    fun isClosed(): Boolean = closed || syncChannel == null

    /**
     * Returns the underlying [SyncChannel] or throws if empty/closed.
     */
    fun requireOpenSyncChannel(): SyncChannel<ConcreteAction, Constraint> {
        if (isEmpty() || isClosed()) {
            throw IllegalStateException("Cannot sync on empty or closed Channel (id=$id)")
        }
        return syncChannel
            ?: throw IllegalStateException("Cannot sync on empty or closed Channel (id=$id)")
    }

    suspend fun close() {
        if (isEmpty() || closed) return
        closed = true
        val sc = syncChannel
        syncChannel = null
        sc?.close()
        onClose?.invoke()
    }

    override fun equals(other: Any?): Boolean = other is Channel && other.id == id

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String =
        if (isEmpty()) "Channel.empty<$actionName>" else "Channel<$actionName>(id=$id, closed=$closed)"
}

/**
 * Closes [chan]. Idempotent. Prefer this over calling [Channel.close] so Julay/Kotlin call
 * sites share one entry point.
 */
suspend fun closeChannel(chan: Channel) {
    chan.close()
}

fun channelType(actionName: String): ChannelType = ChannelType(actionName)

class ChannelType(val actionName: String) : Type {
    companion object {
        private val channelLookup = ThreadLocal<Map<Long, Channel>>()

        /**
         * Provides id→Channel recovery during [ConcreteAction] extraction from a sync model.
         */
        fun <T> withChannelLookup(channelsById: Map<Long, Channel>, block: () -> T): T {
            val prev = channelLookup.get()
            channelLookup.set(channelsById)
            try {
                return block()
            } finally {
                if (prev == null) {
                    channelLookup.remove()
                } else {
                    channelLookup.set(prev)
                }
            }
        }
    }

    override fun toZ3Expr(variable: Variable, ctx: Context): Expr<*> {
        return ctx.mkIntConst(variable.name)
    }

    override fun toZ3Expr(value: Value, ctx: Context): Expr<*> {
        return ctx.mkInt((value.value as Channel).id.toInt())
    }

    override fun fromZ3Expr(expr: Expr<*>, model: Model): Any {
        val id = when (expr) {
            is IntNum -> expr.int.toLong()
            else -> expr.toString().toLong()
        }
        if (id == Channel.EMPTY_ID) {
            return Channel.empty(actionName)
        }
        val channelsById = channelLookup.get()
            ?: throw IllegalStateException("ChannelType.fromZ3Expr requires channel lookup from Constraint bags")
        return channelsById[id]
            ?: throw IllegalStateException(
                "Unknown Channel id $id for action \"$actionName\" (not in sync Constraint bags)",
            )
    }

    override fun isOfType(obj: Any): Boolean =
        obj is Channel && (obj.isEmpty() || obj.actionName == actionName)

    override fun toString(): String = "Channel<$actionName>"

    override fun equals(other: Any?): Boolean =
        other is ChannelType && other.actionName == actionName

    override fun hashCode(): Int = actionName.hashCode()
}
