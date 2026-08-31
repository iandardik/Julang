package julay.program

import kotlinx.coroutines.CompletableDeferred

/** One HTTP handler invocation dispatched to a pooled worker [Proc]. */
data class HandlerWork(
    val argValues: List<Any>,
    val returnDeferred: CompletableDeferred<Value>,
)
