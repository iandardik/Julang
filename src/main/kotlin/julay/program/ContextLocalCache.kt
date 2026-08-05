package julay.program

import com.microsoft.z3.Context
import java.util.Collections
import java.util.IdentityHashMap

/**
 * Per-[Context] cache that does not create a Meta→Context→Meta retention cycle
 * (unlike [java.util.WeakHashMap] keyed by Context when values hold Z3 objects).
 *
 * Entries must be removed via [dropContext] when a [Context] is closed. Do **not** probe
 * other Contexts with Z3 APIs from here: [Context] is not thread-safe, and calling
 * e.g. [Context.boolSort] on a Context another thread is closing SIGSEGVs in native code
 * (seen under raft / multi-proc load after o-class metadata used this cache).
 */
internal class ContextLocalCache<T> {
    private val lock = Any()
    private val map = IdentityHashMap<Context, T>()

    init {
        register(this)
    }

    fun getOrPut(ctx: Context, builder: () -> T): T = synchronized(lock) {
        map[ctx]?.let { return it }
        builder().also { map[ctx] = it }
    }

    private fun remove(ctx: Context) {
        synchronized(lock) { map.remove(ctx) }
    }

    companion object {
        private val allCaches =
            Collections.synchronizedList(mutableListOf<ContextLocalCache<*>>())

        private fun register(cache: ContextLocalCache<*>) {
            allCaches.add(cache)
        }

        /**
         * Drop cached metadata for [ctx]. Call from a `finally` immediately before/after
         * closing an ephemeral Context (Proc step, Select Case, Program scratch SAT).
         */
        fun dropContext(ctx: Context) {
            // Snapshot under the list lock so we do not iterate a live synchronized list
            // while holding per-cache locks (avoid lock-order surprises).
            val caches = synchronized(allCaches) { allCaches.toList() }
            caches.forEach { it.remove(ctx) }
        }
    }
}

/**
 * Runs [block] with a fresh Z3 [Context], then drops all [ContextLocalCache] entries for it
 * before close so prune-via-probe is unnecessary.
 */
internal inline fun <T> withEphemeralContext(block: (Context) -> T): T {
    ContextAllocationCounter.increment()
    val ctx = Context()
    try {
        return block(ctx)
    } finally {
        ContextLocalCache.dropContext(ctx)
        ctx.close()
    }
}

/** Suspend variant of [withEphemeralContext] for [Proc] step loops. */
internal suspend inline fun <T> withEphemeralContextSuspend(block: suspend (Context) -> T): T {
    ContextAllocationCounter.increment()
    val ctx = Context()
    try {
        return block(ctx)
    } finally {
        ContextLocalCache.dropContext(ctx)
        ctx.close()
    }
}

/** Test hook: counts Contexts created via [withEphemeralContext] / [withEphemeralContextSuspend]. */
object ContextAllocationCounter {
    private val count = java.util.concurrent.atomic.AtomicLong(0)

    fun increment() {
        count.incrementAndGet()
    }

    fun get(): Long = count.get()

    fun reset() {
        count.set(0)
    }
}
