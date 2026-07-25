package julay.compiler

import java.nio.file.Path
import kotlin.io.path.name

interface CompileError {}

fun assertOrCompileError(assertion : Boolean, error : CompileError) =
    if (assertion) listOf() else listOf(error)

class OneLocCompileError(
    val loc : ProgramLoc,
    val msg : String
) : CompileError {
    override fun toString() = "$loc: $msg"
}

class TwoLocsCompileError(
    val locA : ProgramLoc,
    val locB : ProgramLoc,
    val msg : String
) : CompileError {
    override fun toString() = "$locA incompatible with $locB: $msg"
}

interface CompileWarning {}

class OneLocCompileWarning(
    val loc : ProgramLoc,
    val msg : String
) : CompileWarning {
    override fun toString() = "$loc: warning: $msg"
}

interface ProgramLoc {}

class SourceLoc(
    private val loc : Pair<Int,Int>,
    private val file : Path? = null,
) : ProgramLoc {
    val startLine: Int get() = loc.first
    val endLine: Int get() = loc.second
    val filePath: Path? get() = file

    override fun toString(): String {
        val lines = when {
            loc.first == loc.second -> "line ${loc.first}"
            else -> "lines ${loc.first}-${loc.second}"
        }
        return if (file != null) "${file.name}:$lines" else lines
    }
}

class LibraryLoc(
    val name : String
) : ProgramLoc {
    override fun toString() = "Library $name"
}
