package julay.compiler

import java.nio.file.Path
import kotlin.io.path.name

interface CompileError {}

fun assertOrCompileError(assertion : Boolean, error : CompileError) =
    if (assertion) listOf() else listOf(error)

class OneLocCompileError(
    private val loc : ProgramLoc,
    private val msg : String
) : CompileError {
    override fun toString() = "$loc: $msg"
}

class TwoLocsCompileError(
    private val locA : ProgramLoc,
    private val locB : ProgramLoc,
    private val msg : String
) : CompileError {
    override fun toString() = "$locA incompatible with $locB: $msg"
}

interface ProgramLoc {}

class SourceLoc(
    private val loc : Pair<Int,Int>,
    private val file : Path? = null,
) : ProgramLoc {
    override fun toString(): String {
        val lines = when {
            loc.first == loc.second -> "line ${loc.first}"
            else -> "lines ${loc.first}-${loc.second}"
        }
        return if (file != null) "${file.name}:$lines" else lines
    }
}

class LibraryLoc(
    private val name : String
) : ProgramLoc {
    override fun toString() = "Library $name"
}