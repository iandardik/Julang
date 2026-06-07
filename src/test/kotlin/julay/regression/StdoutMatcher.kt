package julay.regression

import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

object StdoutMatcher {
    fun assertMatches(projectRoot: File, stdout: String, run: RunConfig) {
        run.expectStdout?.let { path ->
            val expected = File(projectRoot, path).readText()
            assertEquals(normalize(expected), normalize(stdout), "stdout mismatch (golden file $path)")
        }
        run.expectStdoutContains.forEach { needle ->
            assertTrue(stdout.contains(needle), "stdout missing expected substring: $needle\n--- stdout ---\n$stdout")
        }
        run.expectStdoutLinesUnordered.takeIf { it.isNotEmpty() }?.let { expectedLines ->
            val actualLines = stdout.lines().filter { it.isNotBlank() }.toSet()
            val expected = expectedLines.toSet()
            assertEquals(expected, actualLines, "stdout lines (unordered) mismatch")
        }
        run.expectStdoutMatches?.let { pattern ->
            assertTrue(Regex(pattern).containsMatchIn(stdout), "stdout did not match regex $pattern\n--- stdout ---\n$stdout")
        }
    }

    private fun normalize(s: String) = s.replace("\r\n", "\n").trimEnd()
}
