package julay.tools

import java.lang.RuntimeException

fun assert(expr : Boolean, msg : String = "") {
    if (!expr) {
        throw RuntimeException(msg)
    }
}

// thanks chat gpt
fun <T> subsetsOfSize(set: Set<T>, size: Int): Set<Set<T>> {
    require(size >= 0) { "Size must be non-negative" }
    if (size > set.size) return emptySet()
    if (size == 0) return setOf(emptySet())

    val elements = set.toList()
    val result = mutableSetOf<Set<T>>()

    fun backtrack(start: Int, current: MutableList<T>) {
        if (current.size == size) {
            result.add(current.toSet())
            return
        }

        for (i in start until elements.size) {
            current.add(elements[i])
            backtrack(i + 1, current)
            current.removeAt(current.lastIndex)
        }
    }

    backtrack(0, mutableListOf())
    return result
}
