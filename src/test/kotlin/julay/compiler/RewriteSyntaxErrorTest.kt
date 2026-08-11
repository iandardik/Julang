package julay.compiler

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RewriteSyntaxErrorTest {
    @Test
    fun thenAfterIf() {
        val msg = rewriteSyntaxError("extraneous input 'then' expecting '{'")
        assertTrue(msg.contains("Unexpected \"then\""), msg)
        assertTrue(msg.contains("do not use \"then\""), msg)
    }

    @Test
    fun missingTypeBeforeAssign() {
        assertEquals(
            "Missing type declaration before \":=\"; expected \": Type\"",
            rewriteSyntaxError("mismatched input ':=' expecting ':'"),
        )
    }

    @Test
    fun objectLiteralWithParensInsideSet() {
        val msg = rewriteSyntaxError(
            "no viable alternative at input '{Message(\\n                msgType :='",
        )
        assertEquals(
            "Object values use braces: Message { field := ... }, not Message(...)",
            msg,
        )
    }

    @Test
    fun objectLiteralWithParensNoSetBrace() {
        val msg = rewriteSyntaxError(
            "no viable alternative at input 'Point(x :='",
        )
        assertEquals(
            "Object values use braces: Point { field := ... }, not Point(...)",
            msg,
        )
    }

    @Test
    fun tupleParensSuggestListOf() {
        val msg = rewriteSyntaxError("no viable alternative at input '(1,'")
        assertEquals(
            "Unexpected \"(...)\"; list values use listOf(a, b), not (a, b)",
            msg,
        )
    }

    @Test
    fun oldBracketLiteralSuggestsListOf() {
        val msg = rewriteSyntaxError("no viable alternative at input '[1, 2]'")
        assertEquals(
            "Unexpected \"[...]\"; list values use listOf(...), map values use mapOf(k to v)",
            msg,
        )
    }

    @Test
    fun trailingCommaInCollectionCall() {
        val msg = rewriteSyntaxError("no viable alternative at input 'listOf(1,)'")
        assertEquals("Unexpected trailing comma in collection literal", msg)
    }

    @Test
    fun unknownAntlrFallsThrough() {
        val raw = "missing '{' at 'proc'"
        assertEquals("Syntax error: $raw", rewriteSyntaxError(raw))
    }
}
