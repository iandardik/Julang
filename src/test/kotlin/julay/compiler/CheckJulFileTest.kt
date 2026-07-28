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

    @Test
    fun directMissingImportAttributedToImportLine() {
        val dir = Files.createTempDirectory("julay-check-direct-missing")
        val entry = dir.resolve("entry.jul")
        entry.toFile().writeText(
            """
            import gone.X
            proc P {
                constructor initially(args : List<String>) { transit: }
            }
            """.trimIndent(),
        )

        val result = checkJulFile(entry)
        assertTrue(result.hasErrors, result.diagnostics.toString())
        val missing = result.diagnostics.filter {
            it.severity == DiagnosticSeverity.Error && it.message.contains("Cannot find module \"gone\"")
        }
        assertEquals(1, missing.size, result.diagnostics.toString())
        assertEquals(entry.toAbsolutePath().normalize(), missing[0].file?.toAbsolutePath()?.normalize())
        assertEquals(1, missing[0].startLine)
    }

    @Test
    fun transitiveMissingBubblesToEntryImportWithRelated() {
        val dir = Files.createTempDirectory("julay-check-transitive-missing")
        val midDir = dir.resolve("midpkg")
        Files.createDirectories(midDir)
        midDir.resolve("mid.jul").toFile().writeText(
            """
            import gone.Y
            export proc X {
                constructor initially(args : List<String>) { transit: }
            }
            """.trimIndent(),
        )
        val entry = dir.resolve("entry.jul")
        entry.toFile().writeText(
            """
            import midpkg.mid.X
            proc P := X
            """.trimIndent(),
        )

        val result = checkJulFile(entry)
        assertTrue(result.hasErrors, result.diagnostics.toString())

        val leaf = result.diagnostics.filter {
            it.severity == DiagnosticSeverity.Error && it.message.contains("Cannot find module \"gone\"")
        }
        assertEquals(1, leaf.size, result.diagnostics.toString())
        assertTrue(
            leaf[0].file!!.toAbsolutePath().normalize().endsWith("midpkg/mid.jul") ||
                leaf[0].file!!.toAbsolutePath().normalize().endsWith("midpkg\\mid.jul"),
            leaf[0].file.toString(),
        )
        assertEquals(1, leaf[0].startLine)

        val bubble = result.diagnostics.filter {
            it.severity == DiagnosticSeverity.Error &&
                it.message.contains("Module \"midpkg.mid\" has load errors")
        }
        assertEquals(1, bubble.size, result.diagnostics.toString())
        assertEquals(entry.toAbsolutePath().normalize(), bubble[0].file?.toAbsolutePath()?.normalize())
        assertEquals(1, bubble[0].startLine)
        assertTrue(bubble[0].related.isNotEmpty(), bubble[0].toString())
        assertEquals(1, bubble[0].related[0].startLine)
    }

    @Test
    fun cleanTwoModuleImportHasNoErrors() {
        val dir = Files.createTempDirectory("julay-check-clean-import")
        val midDir = dir.resolve("midpkg")
        Files.createDirectories(midDir)
        midDir.resolve("mid.jul").toFile().writeText(
            """
            export proc X {
                constructor initially(args : List<String>) { transit: }
                transition ping() { transit: }
            }
            """.trimIndent(),
        )
        val entry = dir.resolve("entry.jul")
        entry.toFile().writeText(
            """
            import midpkg.mid.X
            proc P := X
            """.trimIndent(),
        )

        val result = checkJulFile(entry)
        assertFalse(result.hasErrors, result.diagnostics.toString())
        assertEquals(
            emptyList(),
            result.diagnostics.filter { it.severity == DiagnosticSeverity.Error },
        )
    }

    @Test
    fun missingModuleNotDuplicatedAtSameImportSite() {
        val dir = Files.createTempDirectory("julay-check-dedupe-missing")
        val entry = dir.resolve("entry.jul")
        entry.toFile().writeText(
            """
            import gone.X
            proc P {
                constructor initially(args : List<String>) { transit: }
            }
            """.trimIndent(),
        )

        val result = checkJulFile(entry)
        val missingMsgs = result.diagnostics.filter {
            it.message.contains("Cannot find module \"gone\"")
        }
        assertEquals(1, missingMsgs.size, result.diagnostics.toString())
    }
}
