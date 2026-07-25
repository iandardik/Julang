package julay.compiler

import julay.compiler.analysis.jsonString
import java.nio.file.Path
import kotlin.io.path.absolute
import kotlin.io.path.pathString

enum class DiagnosticSeverity {
    Error,
    Warning,
}

data class RelatedDiagnosticLoc(
    val file: Path?,
    val startLine: Int,
    val endLine: Int,
    val message: String,
)

data class StructuredDiagnostic(
    val severity: DiagnosticSeverity,
    val message: String,
    val file: Path?,
    val startLine: Int,
    val endLine: Int,
    val related: List<RelatedDiagnosticLoc> = emptyList(),
)

/**
 * Convert a [ProgramLoc] into file/line fields. [LibraryLoc] and missing paths fall back to
 * [entryFile] at line 1 so the IDE always has somewhere to attach a squiggle.
 */
fun ProgramLoc.toFileLine(
    entryFile: Path,
): Triple<Path, Int, Int> = when (this) {
    is SourceLoc -> {
        val path = filePath?.absolute() ?: entryFile.absolute()
        Triple(path, startLine.coerceAtLeast(1), endLine.coerceAtLeast(startLine.coerceAtLeast(1)))
    }
    is LibraryLoc -> Triple(entryFile.absolute(), 1, 1)
    else -> Triple(entryFile.absolute(), 1, 1)
}

fun CompileError.toStructuredDiagnostic(entryFile: Path): StructuredDiagnostic = when (this) {
    is OneLocCompileError -> {
        val (file, start, end) = loc.toFileLine(entryFile)
        val message = when (loc) {
            is LibraryLoc -> "Library ${loc.name}: $msg"
            else -> msg
        }
        StructuredDiagnostic(DiagnosticSeverity.Error, message, file, start, end)
    }
    is TwoLocsCompileError -> {
        val (fileA, startA, endA) = locA.toFileLine(entryFile)
        val (fileB, startB, endB) = locB.toFileLine(entryFile)
        StructuredDiagnostic(
            severity = DiagnosticSeverity.Error,
            message = msg,
            file = fileA,
            startLine = startA,
            endLine = endA,
            related = listOf(
                RelatedDiagnosticLoc(fileB, startB, endB, "related location"),
            ),
        )
    }
    else -> StructuredDiagnostic(
        DiagnosticSeverity.Error,
        toString(),
        entryFile.absolute(),
        1,
        1,
    )
}

fun CompileWarning.toStructuredDiagnostic(entryFile: Path): StructuredDiagnostic = when (this) {
    is OneLocCompileWarning -> {
        val (file, start, end) = loc.toFileLine(entryFile)
        val message = when (loc) {
            is LibraryLoc -> "Library ${loc.name}: $msg"
            else -> msg
        }
        StructuredDiagnostic(DiagnosticSeverity.Warning, message, file, start, end)
    }
    else -> StructuredDiagnostic(
        DiagnosticSeverity.Warning,
        toString(),
        entryFile.absolute(),
        1,
        1,
    )
}

fun buildDiagnosticsJsonDocument(diagnostics: List<StructuredDiagnostic>): String = buildString {
    append("{\n  \"diagnostics\": [\n")
    diagnostics.forEachIndexed { index, d ->
        if (index > 0) append(",\n")
        append(indent(diagnosticJson(d), 4))
    }
    if (diagnostics.isNotEmpty()) append("\n")
    append("  ]\n}\n")
}

private fun diagnosticJson(d: StructuredDiagnostic): String = buildString {
    append("{\n")
    append("  \"severity\": ").append(jsonString(d.severity.name.lowercase())).append(",\n")
    append("  \"message\": ").append(jsonString(d.message)).append(",\n")
    append("  \"file\": ").append(jsonString(d.file?.pathString ?: "")).append(",\n")
    append("  \"startLine\": ").append(d.startLine).append(",\n")
    append("  \"endLine\": ").append(d.endLine).append(",\n")
    append("  \"related\": [\n")
    d.related.forEachIndexed { index, r ->
        if (index > 0) append(",\n")
        append(indent(relatedJson(r), 4))
    }
    if (d.related.isNotEmpty()) append("\n")
    append("  ]\n")
    append("}")
}

private fun relatedJson(r: RelatedDiagnosticLoc): String = buildString {
    append("{\n")
    append("  \"file\": ").append(jsonString(r.file?.pathString ?: "")).append(",\n")
    append("  \"startLine\": ").append(r.startLine).append(",\n")
    append("  \"endLine\": ").append(r.endLine).append(",\n")
    append("  \"message\": ").append(jsonString(r.message)).append("\n")
    append("}")
}

private fun indent(block: String, spaces: Int): String {
    val pad = " ".repeat(spaces)
    return block.lineSequence().joinToString("\n") { line ->
        if (line.isEmpty()) line else pad + line
    }
}
