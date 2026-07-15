package julay.program

import com.microsoft.z3.Context
import com.microsoft.z3.Z3Exception
import java.util.IdentityHashMap

/**
 * Per-[Context] cache that does not create a Meta→Context→Meta retention cycle
 * (unlike [java.util.WeakHashMap] keyed by Context when values hold Z3 objects).
 *
 * Entries for closed Contexts are pruned on access so the map cannot grow with every
 * ephemeral Proc/channel Context.
 */
internal class ContextLocalCache<T> {
    private val lock = Any()
    private val map = IdentityHashMap<Context, T>()

    fun getOrPut(ctx: Context, builder: () -> T): T = synchronized(lock) {
        map[ctx]?.let { return it }
        pruneClosed()
        builder().also { map[ctx] = it }
    }

    private fun pruneClosed() {
        val dead = map.keys.filter { contextIsClosed(it) }
        dead.forEach { map.remove(it) }
    }

    private fun contextIsClosed(ctx: Context): Boolean =
        try {
            ctx.boolSort
            false
        } catch (_: Z3Exception) {
            true
        } catch (_: IllegalArgumentException) {
            true
        }
}
