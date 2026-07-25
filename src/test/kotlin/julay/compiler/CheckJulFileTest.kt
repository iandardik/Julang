package julay.compiler

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CheckJulFileTest {

    @Test
    fun checkJsonReportsTypeErrorWithLine() {
        val dir = Files.createTempDirectory("julay-check-error")
        val file = dir.resolve("bad.jul")
        file.toFile().writeText(
            """
            proc Bad {
                var x : NoSuchType
                constructor initially(args : List<String>) { transit: }
            }
            """.trimIndent(),
        )

        val result = checkJulFile(file)
        assertTrue(result.hasErrors, result.diagnostics.toString())
        val json = buildDiagnosticsJsonDocument(result.diagnostics)
        assertTrue(json.contains("\"severity\": \"error\""), json)
        assertTrue(json.contains("\"startLine\":"), json)
        assertTrue(
            result.diagnostics.any { it.severity == DiagnosticSeverity.Error && it.startLine >= 1 },
            result.diagnostics.toString(),
        )
    }

    @Test
    fun checkJsonReportsSoftSyncAsWarningOnly() {
        val dir = Files.createTempDirectory("julay-check-warn")
        val file = dir.resolve("solo.jul")
        file.toFile().writeText(
            """
            proc Solo {
                constructor initially(args : List<String>) { transit: }
                session transition ping() { transit: }
            }
            proc P := Solo
            """.trimIndent(),
        )

        val result = checkJulFile(file)
        assertFalse(result.hasErrors, result.diagnostics.toString())
        assertTrue(
            result.diagnostics.any {
                it.severity == DiagnosticSeverity.Warning &&
                    it.message.contains("exactly two sync peers")
            },
            result.diagnostics.toString(),
        )
        val json = buildDiagnosticsJsonDocument(result.diagnostics)
        assertTrue(json.contains("\"severity\": \"warning\""), json)
        assertFalse(json.contains("\"severity\": \"error\""), json)
    }

    @Test
    fun checkCleanFileHasEmptyDiagnostics() {
        val dir = Files.createTempDirectory("julay-check-clean")
        val file = dir.resolve("ok.jul")
        file.toFile().writeText(
            """
            proc A {
                constructor initially(args : List<String>) { transit: }
                transition ping() { transit: }
            }
            proc B {
                constructor initially(args : List<String>) { transit: }
                transition ping() { transit: }
            }
            proc P := A || B
            """.trimIndent(),
        )

        val result = checkJulFile(file)
        assertFalse(result.hasErrors)
        assertEquals(emptyList(), result.diagnostics.filter { it.severity == DiagnosticSeverity.Error })
    }
}
