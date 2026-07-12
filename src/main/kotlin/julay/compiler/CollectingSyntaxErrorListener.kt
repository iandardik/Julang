package julay.compiler

import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer
import java.nio.file.Path

/**
 * Collects ANTLR syntax errors as [CompileError]s, with friendlier wording for
 * common mistakes (e.g. writing `then` in an if-expression).
 */
class CollectingSyntaxErrorListener(
    private val path: Path,
    private val errors: MutableList<CompileError>,
) : BaseErrorListener() {
    override fun syntaxError(
        recognizer: Recognizer<*, *>?,
        offendingSymbol: Any?,
        line: Int,
        charPositionInLine: Int,
        msg: String,
        e: RecognitionException?,
    ) {
        errors.add(OneLocCompileError(SourceLoc(Pair(line, line), path), rewriteSyntaxError(msg)))
    }
}

internal fun rewriteSyntaxError(msg: String): String {
    val thenAfterIf = Regex(
        """extraneous input 'then' expecting '\{'""",
        RegexOption.IGNORE_CASE,
    )
    if (thenAfterIf.containsMatchIn(msg)) {
        return "Unexpected \"then\" after if-condition; write if (cond) { ... } else { ... } " +
            "(Julay if-expressions do not use \"then\")"
    }
    return "Syntax error: $msg"
}
