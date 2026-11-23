package julay.compiler.decl

import julay.compiler.ast.ExprNode
import julay.program.type.Type

sealed class TransitUpdate {
    abstract fun transitRootVar(): String

    data class Assign(val key: String, val expr: ExprNode) : TransitUpdate() {
        override fun transitRootVar(): String = julay.program.type.transitRootVar(key)
    }

    data class IndexPut(val collectionVar: String, val index: ExprNode, val value: ExprNode) : TransitUpdate() {
        override fun transitRootVar(): String = collectionVar
    }

    /** Transit-scoped temporary; evaluated against pre-state (+ earlier lets), not process state. */
    data class Let(val name: String, val type: Type, val init: ExprNode) : TransitUpdate() {
        override fun transitRootVar(): String = name
    }

    /**
     * Conditional transit block. False arm with [elseUpdates] empty is a no-op
     * (do not evaluate then-arm RHSs).
     */
    data class If(
        val cond: ExprNode,
        val thenUpdates: List<TransitUpdate>,
        val elseUpdates: List<TransitUpdate> = emptyList(),
    ) : TransitUpdate() {
        override fun transitRootVar(): String =
            (thenUpdates + elseUpdates).firstOrNull()?.transitRootVar() ?: ""
    }
}

/** Roots assigned by [update], descending into [TransitUpdate.If] arms (skips lets). */
fun TransitUpdate.assignedRoots(): Set<String> = when (this) {
    is TransitUpdate.Assign -> setOf(transitRootVar())
    is TransitUpdate.IndexPut -> setOf(collectionVar)
    is TransitUpdate.Let -> emptySet()
    is TransitUpdate.If ->
        thenUpdates.flatMap { it.assignedRoots() }.toSet() +
            elseUpdates.flatMap { it.assignedRoots() }.toSet()
}

/** Pre-order walk of leaf updates (Assign/IndexPut/Let); [If] descends into both arms. */
fun TransitUpdate.forEachLeaf(f: (TransitUpdate) -> Unit) {
    when (this) {
        is TransitUpdate.If -> {
            thenUpdates.forEach { it.forEachLeaf(f) }
            elseUpdates.forEach { it.forEachLeaf(f) }
        }
        else -> f(this)
    }
}

fun Iterable<TransitUpdate>.forEachLeaf(f: (TransitUpdate) -> Unit) =
    forEach { it.forEachLeaf(f) }

/** True if [pred] holds for this update or any nested arm of an [If]. */
fun TransitUpdate.anyMatching(pred: (TransitUpdate) -> Boolean): Boolean = when (this) {
    is TransitUpdate.If ->
        thenUpdates.any { it.anyMatching(pred) } || elseUpdates.any { it.anyMatching(pred) }
    else -> pred(this)
}

fun Iterable<TransitUpdate>.anyMatching(pred: (TransitUpdate) -> Boolean): Boolean =
    any { it.anyMatching(pred) }

/** Depth-first visit including [If] nodes themselves (then arms). */
fun TransitUpdate.forEachUpdate(f: (TransitUpdate) -> Unit) {
    f(this)
    if (this is TransitUpdate.If) {
        thenUpdates.forEach { it.forEachUpdate(f) }
        elseUpdates.forEach { it.forEachUpdate(f) }
    }
}

fun Iterable<TransitUpdate>.forEachUpdate(f: (TransitUpdate) -> Unit) =
    forEach { it.forEachUpdate(f) }
