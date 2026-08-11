package julay.compiler

import julay.compiler.pass.codegenPass
import julay.program.sync.SyncResolveConfig
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SyncPathVerboseSummaryTest {
    @Test
    fun verboseSummaryReportsProcAndActionLayers() {
        val dir = Files.createTempDirectory("julay-sync-path-verbose")
        val file = dir.resolve("sync-path.jul")
        file.toFile().writeText(
            """
            import julay.funlib.exitProgram

            proc FastPeer {
                var done : Boolean
                constructor initially(args : List<String>) {
                    transit:
                        done := false
                }
                transition handoff(n : Int) {
                    guard: ~done & n = 1
                    transit:
                        done := true
                }
                internal transition exitSystem() {
                    guard: done
                    after:
                        exitProgram(0)
                }
            }
            proc RelPeer {
                var done : Boolean
                constructor initially(args : List<String>) {
                    transit:
                        done := false
                }
                transition handoff(n : Int) {
                    guard: ~done & n < 2
                    transit:
                        done := true
                }
                internal transition exitSystem() {
                    guard: done
                    after:
                        exitProgram(0)
                }
            }
            proc SyncPathDemo := FastPeer || RelPeer
            compile SyncPathDemo
            """.trimIndent(),
        )

        val checked = prepareCheckedCompilation(file)
        assertNotNull(checked)
        val program = checked.jarTargets.single()
        val codegen = codegenPass(
            checked.ast,
            program,
            checked.procDecls,
            checked.librariesInUse,
            SyncResolveConfig.ALL_ON,
        )
        val summary = codegen.syncPathStats.formatSummary(SyncResolveConfig.ALL_ON)

        assertTrue(summary.contains("=== julay sync path summary ==="), summary)
        assertTrue(summary.contains("eq-unify=on"), summary)
        assertTrue(summary.contains("arg-ownership=on"), summary)
        assertTrue(summary.contains("directed-eval=on"), summary)
        assertTrue(
            Regex("""procs:\s+\d+\s+FastOnly,\s+\d+\s+NeedsZ3""").containsMatchIn(summary),
            summary,
        )
        assertTrue(
            Regex("""actions:\s+\d+\s+with fastGuard,\s+\d+\s+opaque""").containsMatchIn(summary),
            summary,
        )
        assertTrue(summary.contains("FastOnly procs:"), summary)
        assertTrue(summary.contains("FastPeer"), summary)
        assertTrue(summary.contains("NeedsZ3 procs:"), summary)
        assertTrue(summary.contains("RelPeer"), summary)
        assertTrue(summary.contains("handoff.guard (relational)"), summary)
        assertTrue(
            summary.contains("generated Julay procs only"),
            summary,
        )

        // Mirror compileJulFile --verbose: summary is printed to stdout.
        val captured = ByteArrayOutputStream()
        val prev = System.out
        try {
            System.setOut(PrintStream(captured))
            print(summary)
        } finally {
            System.setOut(prev)
        }
        val stdout = captured.toString()
        assertTrue(stdout.contains("procs:"), stdout)
        assertTrue(stdout.contains("actions:"), stdout)
        assertTrue(stdout.contains("FastOnly"), stdout)
        assertTrue(stdout.contains("NeedsZ3"), stdout)
        assertTrue(stdout.contains("fastGuard"), stdout)
        assertTrue(stdout.contains("opaque"), stdout)

        dir.toFile().deleteRecursively()
    }

    @Test
    fun allOptsOffNoteAppearsInSummary() {
        val stats = SyncPathStats.EMPTY
        val text = stats.formatSummary(SyncResolveConfig.ALL_OFF)
        assertTrue(text.contains("eq-unify=off"), text)
        assertTrue(text.contains("all sync opts disabled"), text)
    }
}
