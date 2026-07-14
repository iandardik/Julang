package julay.program

import io.github.cvc5.Sort
import io.github.cvc5.TermManager
import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.withContext

/**
 * Per-Proc cache of live CVC5 datatype artifacts for one [TermManager].
 * Does not share Terms across Procs; serialized SMT-LIB stays on each [Type].
 */
class DatatypeBinder(val tm: TermManager) {
    private val mapCells = HashMap<String, MapCellMetadata>()
    private val setCells = HashMap<String, SetCellMetadata>()
    private val objClasses = HashMap<String, JulangDatatypeMetadata>()
    private val seqSorts = HashMap<String, Sort>()

    fun mapCell(cellName: String, build: () -> MapCellMetadata): MapCellMetadata =
        mapCells.getOrPut(cellName, build)

    fun setCell(cellName: String, build: () -> SetCellMetadata): SetCellMetadata =
        setCells.getOrPut(cellName, build)

    fun objClass(name: String, build: () -> JulangDatatypeMetadata): JulangDatatypeMetadata =
        objClasses.getOrPut(name, build)

    fun seqSort(key: String, build: () -> Sort): Sort =
        seqSorts.getOrPut(key, build)

    companion object {
        private val local = ThreadLocal<DatatypeBinder?>()

        /** Active binder for the current Proc coroutine, or null outside [withBinder]. */
        fun current(): DatatypeBinder? = local.get()

        /**
         * Binder for [tm]: prefer the Proc-installed binder when its native pointer matches,
         * otherwise a thread-local weak fallback so unit tests can call cellMetadata without a Proc.
         */
        fun forTm(tm: TermManager): DatatypeBinder {
            val cur = local.get()
            if (cur != null && cur.tm.getPointer() == tm.getPointer()) {
                return cur
            }
            synchronized(fallbackBinders) {
                return fallbackBinders.getOrPut(tm) { DatatypeBinder(tm) }
            }
        }

        private val fallbackBinders = java.util.WeakHashMap<TermManager, DatatypeBinder>()

        suspend fun <T> withBinder(tm: TermManager, block: suspend () -> T): T {
            val binder = DatatypeBinder(tm)
            return withContext(local.asContextElement(binder)) {
                block()
            }
        }
    }
}
