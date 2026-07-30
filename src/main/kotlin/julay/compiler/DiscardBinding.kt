package julay.compiler

/** Wildcard binding name: may be bound (e.g. transit `let`) but never referenced as a value. */
const val DISCARD_BINDING = "_"

fun String.isDiscardBinding(): Boolean = this == DISCARD_BINDING
