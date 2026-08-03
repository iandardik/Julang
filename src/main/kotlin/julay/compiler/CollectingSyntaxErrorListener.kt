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

    // `TypeName(field := ...)` — object literals use braces, not call parentheses.
    // ANTLR often reports this as "no viable alternative at input '{Message(\n  field :='"
    // (inside a set) or similar snippets that include both `Name(` and `:=`.
    val oclassParensWithFields = Regex(
        """no viable alternative at input '(?:\{)?([A-Za-z_][A-Za-z0-9_]*)\([\s\S]*?:=""",
    )
    oclassParensWithFields.find(msg)?.let { match ->
        val name = match.groupValues[1]
        return "Object values use braces: $name { field := ... }, not $name(...)"
    }

    // `pos := Point(x := 0)` — parser finishes `Point` as an expression, then `(` is
    // "extraneous" at statement level. Same shape for invalid named call args.
    if ((msg.contains("extraneous input '('") || msg.contains("mismatched input '('")) &&
        looksLikeOclassFieldAssignAfterParen(offendingSymbol, recognizer)
    ) {
        val name = previousIdentifier(offendingSymbol, recognizer) ?: "Name"
        return "Julay does not support $name(field := ...) with parentheses; " +
            "for object values write $name { field := ... }"
    }

    // `(1, 2)` mistaken for a list / tuple
    if (Regex("""no viable alternative at input '\([^']*,""").containsMatchIn(msg)) {
        return "Unexpected \"(...)\"; list values use brackets: [a, b], not (a, b)"
    }

    // Trailing comma in `{1,}` / `[1,]` / etc.
    if (Regex("""no viable alternative at input '[\[{][^']*,\s*[\]}]'""").containsMatchIn(msg)) {
        return "Unexpected trailing comma in collection literal"
    }

    return "Syntax error: $msg"
}

/**
 * After an offending `(`, look ahead for `ID :=` (object field assign syntax).
 */
private fun looksLikeOclassFieldAssignAfterParen(
    offendingSymbol: Any?,
    recognizer: Recognizer<*, *>?,
): Boolean {
    val token = offendingSymbol as? Token ?: return false
    val parser = recognizer as? Parser ?: return false
    val stream = parser.tokenStream ?: return false
    var i = token.tokenIndex + 1
    var sawId = false
    while (i < stream.size()) {
        val next = stream.get(i)
        if (next.channel != Token.DEFAULT_CHANNEL) {
            i++
            continue
        }
        if (next.type == Token.EOF) return false
        if (!sawId) {
            if (next.type != JulayLexer.ID) return false
            sawId = true
            i++
            continue
        }
        return next.type == JulayLexer.ASGN_EQ
    }
    return false
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
