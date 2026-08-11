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
    if (looksLikeThenAfterIf(offendingSymbol, recognizer) ||
        Regex("""extraneous input 'then' expecting""", RegexOption.IGNORE_CASE).containsMatchIn(msg) ||
        Regex("""mismatched input 'then'""", RegexOption.IGNORE_CASE).containsMatchIn(msg)
    ) {
        return "Unexpected \"then\" after if-condition; write if (cond) expr else expr " +
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
    // (inside a set) or `setOf(Message(\n  field :='`. Prefer the innermost type name.
    if (msg.contains("no viable alternative at input") && msg.contains(":=")) {
        val names = Regex("""([A-Za-z_][A-Za-z0-9_]*)\(""").findAll(msg).map { it.groupValues[1] }.toList()
        val name = names.lastOrNull { it !in setOf("listOf", "setOf", "mapOf") }
        if (name != null && msg.substringAfter("$name(").contains(":=")) {
            return "Object values use braces: $name { field := ... }, not $name(...)"
        }
    }

    // `pos := Point(x := 0)` — parser finishes `Point` as an expression, then `(` is
    // "extraneous" at statement level. Same shape for invalid named call args.
    if ((msg.contains("extraneous input '('") || msg.contains("mismatched input '('")) &&
        looksLikeObjFieldAssignAfterParen(offendingSymbol, recognizer)
    ) {
        val name = previousIdentifier(offendingSymbol, recognizer) ?: "Name"
        return "Julay does not support $name(field := ...) with parentheses; " +
            "for object values write $name { field := ... }"
    }

    // `(1, 2)` mistaken for a list / tuple
    if (Regex("""no viable alternative at input '\([^']*,""").containsMatchIn(msg)) {
        return "Unexpected \"(...)\"; list values use listOf(a, b), not (a, b)"
    }

    // Old bracket / brace value literals
    if (
        Regex("""no viable alternative at input '\[[^\]]*\]?'""").containsMatchIn(msg) ||
        msg.contains("extraneous input '['") ||
        msg.contains("mismatched input '['")
    ) {
        return "Unexpected \"[...]\"; list values use listOf(...), map values use mapOf(k to v)"
    }
    if (
        Regex("""no viable alternative at input '\{[^}]*\}?'""").containsMatchIn(msg) ||
        msg.contains("extraneous input '{'")
    ) {
        // Keep obj-literal and sort-domain braces distinguishable when possible
        if (!msg.contains(":=")) {
            return "Unexpected \"{...}\" value; set values use setOf(...)"
        }
    }

    // Trailing comma in listOf(1,) / setOf(1,) / mapOf(...)
    if (Regex("""no viable alternative at input '[^']*,\s*\)'""").containsMatchIn(msg)) {
        return "Unexpected trailing comma in collection literal"
    }

    return "Syntax error: $msg"
}

/**
 * Detect `if (...) then` — `then` is parsed as an identifier expression, so the
 * first syntax error may land later (e.g. on the then-branch body).
 */
private fun looksLikeThenAfterIf(
    offendingSymbol: Any?,
    recognizer: Recognizer<*, *>?,
): Boolean {
    val token = offendingSymbol as? Token ?: return false
    val parser = recognizer as? Parser ?: return false
    val stream = parser.tokenStream ?: return false
    // Collect recent default-channel tokens up to and including a nearby "then" ID.
    val recent = mutableListOf<Token>()
    var i = token.tokenIndex
    while (i >= 0 && recent.size < 40) {
        val t = stream.get(i)
        if (t.channel == Token.DEFAULT_CHANNEL) {
            recent.add(t)
        }
        i--
    }
    recent.reverse()
    // Find ID "then" followed eventually by our error region; require IF ... RPAREN before it.
    for (idx in recent.indices) {
        val t = recent[idx]
        if (t.type != JulayLexer.ID || t.text != "then") continue
        // Look left for IF ... RPAREN immediately before this then
        var j = idx - 1
        while (j >= 0 && recent[j].channel != Token.DEFAULT_CHANNEL) j--
        if (j < 0 || recent[j].type != JulayLexer.RPAREN) continue
        var k = j - 1
        var depth = 1
        while (k >= 0 && depth > 0) {
            when (recent[k].type) {
                JulayLexer.RPAREN -> depth++
                JulayLexer.LPAREN -> depth--
            }
            k--
        }
        // k now before matching LPAREN; previous should be IF
        while (k >= 0 && recent[k].channel != Token.DEFAULT_CHANNEL) k--
        if (k >= 0 && recent[k].type == JulayLexer.IF) return true
    }
    return false
}

/**
 * After an offending `(`, look ahead for `ID :=` (object field assign syntax).
 */
private fun looksLikeObjFieldAssignAfterParen(
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
