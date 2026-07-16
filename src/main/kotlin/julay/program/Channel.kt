package julay.program

import com.microsoft.z3.BoolExpr
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
 * Only the creator (e.g. HttpServer / HttpClient) should [close] / [closeChannel] a live channel.
 */
class Channel private constructor(
    val id: Long,
    val actionName: String,
    val ownerAction: SymbolicAction?,
    @Volatile private var syncChannel: SyncChannel<ConcreteAction, BoolExpr>?,
    @Volatile private var closed: Boolean,
    private val table: DynamicChannelTable?,
) {
    companion object {
        const val EMPTY_ID = 0L
        private val nextId = AtomicLong(1)

        fun empty(actionName: String): Channel =
            Channel(EMPTY_ID, actionName, ownerAction = null, syncChannel = null, closed = true, table = null)

        fun create(
            syncChannel: SyncChannel<ConcreteAction, BoolExpr>,
            ownerAction: SymbolicAction,
            table: DynamicChannelTable,
        ): Channel {
            val id = nextId.getAndIncrement()
            val channel = Channel(
                id,
                ownerAction.name,
                ownerAction,
                syncChannel,
                closed = false,
                table,
            )
            table.register(id, channel)
            return channel
        }
    }

    fun isEmpty(): Boolean = id == EMPTY_ID

    fun isClosed(): Boolean = closed || syncChannel == null

    /**
     * Returns the underlying [SyncChannel] or throws if empty/closed.
     */
    fun requireOpenSyncChannel(): SyncChannel<ConcreteAction, BoolExpr> {
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
        table?.unregister(id)
    }

    override fun equals(other: Any?): Boolean = other is Channel && other.id == id

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String =
        if (isEmpty()) "Channel.empty<$actionName>" else "Channel<$actionName>(id=$id, closed=$closed)"
}

/**
 * Closes [chan] and unregisters it from its owner table. Idempotent. Prefer this over
 * calling [Channel.close] so Julay/Kotlin call sites share one entry point.
 */
suspend fun closeChannel(chan: Channel) {
    chan.close()
}

fun channelType(actionName: String): ChannelType = ChannelType(actionName)

class ChannelType(val actionName: String) : Type {
    companion object {
        private val programLookup = ThreadLocal<Program>()

        fun <T> withProgramLookup(program: Program, block: () -> T): T {
            val prev = programLookup.get()
            programLookup.set(program)
            try {
                return block()
            } finally {
                if (prev == null) {
                    programLookup.remove()
                } else {
                    programLookup.set(prev)
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
        val program = programLookup.get()
            ?: throw IllegalStateException("ChannelType.fromZ3Expr requires Program lookup context")
        return program.lookupDynamicChannel(actionName, id)
    }

    override fun isOfType(obj: Any): Boolean =
        obj is Channel && (obj.isEmpty() || obj.actionName == actionName)

    override fun toString(): String = "Channel<$actionName>"

    override fun equals(other: Any?): Boolean =
        other is ChannelType && other.actionName == actionName

    override fun hashCode(): Int = actionName.hashCode()
}
