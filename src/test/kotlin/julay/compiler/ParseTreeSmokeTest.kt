package julay.compiler

import julay.parser.JulayLexer
import julay.parser.JulayParser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ParseTreeSmokeTest {
    private fun tree(input: String): String {
        val parser = JulayParser(CommonTokenStream(JulayLexer(CharStreams.fromString(input))))
        return parser.expr().toStringTree(parser)
    }

    @Test
    fun leafVarIsFieldAccessNotIndexExpr() {
        val t = tree("Counter.n")
        assertTrue(t.contains("field_access"), t)
        assertFalse(t.contains("index_expr"), t)
    }

    @Test
    fun methodAndPropStillWork() {
        assertTrue(tree("xs.filter(i -> i > 0)").contains("method_call"))
        assertTrue(tree("xs.filter(i -> i > 0).length").contains("method_prop_expr"))
        assertTrue(tree("xs.length").contains("field_access") || tree("xs.length").contains("index_expr"))
    }
}
