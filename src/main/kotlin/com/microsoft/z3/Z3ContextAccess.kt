package com.microsoft.z3

/**
 * Exposes package-private [Z3Object.getContext] so Julang can build
 * length/nth queries while extracting list values from a [Model].
 */
fun Z3Object.julangContext(): Context = getContext()
