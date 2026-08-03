package julay.compiler

import julay.parser.JulayLexer
import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.Parser
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer
import org.antlr.v4.runtime.Token
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
        errors.add(
            OneLocCompileError(
                SourceLoc(Pair(line, line), path),
                rewriteSyntaxError(msg, offendingSymbol, recognizer),
            ),
        )
    }
}

internal fun rewriteSyntaxError(
    msg: String,
    offendingSymbol: Any? = null,
    recognizer: Recognizer<*, *>? = null,
): String {
    val thenAfterIf = Regex(
        """extraneous input 'then' expecting '\{'""",
        RegexOption.IGNORE_CASE,
    )
    if (thenAfterIf.containsMatchIn(msg)) {
        return "Unexpected \"then\" after if-condition; write if (cond) { ... } else { ... } " +
            "(Julay if-expressions do not use \"then\")"
    }
    // `name := ...` where grammar requires `name : Type := ...` (let / var / arg, etc.)
    if (msg.contains("mismatched input ':=' expecting ':'")) {
        val name = previousIdentifier(offendingSymbol, recognizer)
        return if (name != null) {
            "\"$name\" is missing a type declaration"
        } else {
            "Missing type declaration before \":=\"; expected \": Type\""
        }
    }
    return "Syntax error: $msg"
}

private fun previousIdentifier(
    offendingSymbol: Any?,
    recognizer: Recognizer<*, *>?,
): String? {
    val token = offendingSymbol as? Token ?: return null
    val parser = recognizer as? Parser ?: return null
    val stream = parser.tokenStream ?: return null
    var i = token.tokenIndex - 1
    while (i >= 0) {
        val prev = stream.get(i)
        if (prev.channel != Token.DEFAULT_CHANNEL) {
            i--
            continue
        }
        return if (prev.type == JulayLexer.ID) prev.text else null
    }
    return null
}
