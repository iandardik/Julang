package julay.spec

import julay.compiler.CompileTargets
import julay.compiler.compileJulFile
import java.io.File
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

private val TLC_JAR = File("/Users/idardik/bin/tla2tools.jar")
private const val TLC_TIMEOUT_SECONDS = 30L

class SpecTlaTlcSmokeTest {

    @Test
    fun safeIncCompilesAndTlcStarts() {
        assumeTlcPresent()
        val work = Files.createTempDirectory("julay-spec-safeinc").toFile()
        try {
            val source = File("regression/input/spec/safe-inc.jul")
            assertTrue(source.exists(), "missing ${source.path}")
            compileJulFile(
                source.toPath(),
                keepBuild = false,
                targets = CompileTargets(compilePrograms = false, compileSpecs = true),
            )
            // compileJulFile writes to CWD; move artifacts into work dir
            val tla = File("SafeInc.tla")
            val cfg = File("SafeInc.cfg")
            assertTrue(tla.exists(), "expected SafeInc.tla")
            assertTrue(cfg.exists(), "expected SafeInc.cfg")
            tla.copyTo(File(work, "SafeInc.tla"), overwrite = true)
            cfg.copyTo(File(work, "SafeInc.cfg"), overwrite = true)
            tla.delete()
            cfg.delete()
            assertTlcHealthyStart(work, "SafeInc")
        } finally {
            work.deleteRecursively()
            File("SafeInc.tla").delete()
            File("SafeInc.cfg").delete()
        }
    }

    @Test
    fun paramCountersCompilesAndTlcStarts() {
        assumeTlcPresent()
        val work = Files.createTempDirectory("julay-spec-param").toFile()
        try {
            val source = File("regression/input/spec/param-counters.jul")
            assertTrue(source.exists(), "missing ${source.path}")
            compileJulFile(
                source.toPath(),
                keepBuild = false,
                targets = CompileTargets(compilePrograms = false, compileSpecs = true),
            )
            val tla = File("ParamCounters.tla")
            val cfg = File("ParamCounters.cfg")
            assertTrue(tla.exists(), "expected ParamCounters.tla")
            assertTrue(cfg.exists(), "expected ParamCounters.cfg")
            tla.copyTo(File(work, "ParamCounters.tla"), overwrite = true)
            cfg.copyTo(File(work, "ParamCounters.cfg"), overwrite = true)
            tla.delete()
            cfg.delete()
            assertTlcHealthyStart(work, "ParamCounters")
        } finally {
            work.deleteRecursively()
            File("ParamCounters.tla").delete()
            File("ParamCounters.cfg").delete()
        }
    }

    @Test
    fun spawnWorkerCtorSyncCompilesAndTlcStarts() {
        assumeTlcPresent()
        val work = Files.createTempDirectory("julay-spec-spawn").toFile()
        try {
            val source = File("regression/input/spec/spawn-worker.jul")
            assertTrue(source.exists(), "missing ${source.path}")
            compileJulFile(
                source.toPath(),
                keepBuild = false,
                targets = CompileTargets(compilePrograms = false, compileSpecs = true),
            )
            val tla = File("SpawnWorker.tla")
            val cfg = File("SpawnWorker.cfg")
            assertTrue(tla.exists(), "expected SpawnWorker.tla")
            assertTrue(cfg.exists(), "expected SpawnWorker.cfg")
            val tlaText = tla.readText()
            assertTrue(
                tlaText.contains("spawnWorker(id) =="),
                "expected spawnWorker(id) operator;\n$tlaText",
            )
            assertTrue(
                tlaText.contains("~Worker_constructed"),
                "expected ctor enabling on Worker for spawnWorker;\n$tlaText",
            )
            assertTrue(
                tlaText.contains("Worker_constructed' = TRUE"),
                "expected Worker_constructed' flip in spawnWorker;\n$tlaText",
            )
            assertTrue(
                tlaText.contains("\\E id \\in Int : spawnWorker(id)"),
                "expected Next to quantify spawnWorker args;\n$tlaText",
            )
            val workBody = tlaText.substringAfter("work ==").substringBefore("\n\n")
            assertTrue(
                workBody.contains("/\\ Worker_constructed"),
                "work should require Worker_constructed;\n$tlaText",
            )
            assertTrue(
                workBody.lines().none { it.trim() == "/\\ Server_constructed" },
                "work should not require Server_constructed;\n$tlaText",
            )
            tla.copyTo(File(work, "SpawnWorker.tla"), overwrite = true)
            cfg.copyTo(File(work, "SpawnWorker.cfg"), overwrite = true)
            tla.delete()
            cfg.delete()
            assertTlcHealthyStart(work, "SpawnWorker")
        } finally {
            work.deleteRecursively()
            File("SpawnWorker.tla").delete()
            File("SpawnWorker.cfg").delete()
        }
    }

    @Test
    fun composedProgramSystemExpandsForInvariantAndTla() {
        assumeTlcPresent()
        val work = Files.createTempDirectory("julay-spec-composed").toFile()
        try {
            val source = File("regression/input/spec/composed-system.jul")
            assertTrue(source.exists(), "missing ${source.path}")
            compileJulFile(
                source.toPath(),
                keepBuild = false,
                targets = CompileTargets(compilePrograms = false, compileSpecs = true),
            )
            val tla = File("ComposedSystem.tla")
            val cfg = File("ComposedSystem.cfg")
            assertTrue(tla.exists(), "expected ComposedSystem.tla")
            assertTrue(cfg.exists(), "expected ComposedSystem.cfg")
            val tlaText = tla.readText()
            assertTrue(
                tlaText.contains("Counter_n") && tlaText.contains("Counter_constructed"),
                "expected expanded Counter state in TLA;\n$tlaText",
            )
            assertTrue(
                tlaText.contains("Env_constructed"),
                "expected expanded Env state in TLA;\n$tlaText",
            )
            assertTrue(
                !tlaText.contains("System_constructed"),
                "program alias System should not appear as a leaf;\n$tlaText",
            )
            tla.copyTo(File(work, "ComposedSystem.tla"), overwrite = true)
            cfg.copyTo(File(work, "ComposedSystem.cfg"), overwrite = true)
            tla.delete()
            cfg.delete()
            assertTlcHealthyStart(work, "ComposedSystem")
        } finally {
            work.deleteRecursively()
            File("ComposedSystem.tla").delete()
            File("ComposedSystem.cfg").delete()
        }
    }

    @Test
    fun nestedSpecAliasExpandsLikeInlinedComposition() {
        assumeTlcPresent()
        val work = Files.createTempDirectory("julay-spec-nested").toFile()
        try {
            val source = File("regression/input/spec/nested-spec.jul")
            assertTrue(source.exists(), "missing ${source.path}")
            compileJulFile(
                source.toPath(),
                keepBuild = false,
                targets = CompileTargets(compilePrograms = false, compileSpecs = true),
            )
            // Only NestedSpec is AG; HandlerSpec is a bare alias used inside it.
            val tla = File("NestedSpec.tla")
            val cfg = File("NestedSpec.cfg")
            assertTrue(tla.exists(), "expected NestedSpec.tla")
            assertTrue(cfg.exists(), "expected NestedSpec.cfg")
            val tlaText = tla.readText()
            assertTrue(
                tlaText.contains("Counter_constructed") && tlaText.contains("Counter_n"),
                "expected Counter state in TLA;\n$tlaText",
            )
            assertTrue(
                tlaText.contains("Handler_constructed") && tlaText.contains("Handler_step"),
                "expected nested HandlerSpec expanded to Handler;\n$tlaText",
            )
            assertTrue(
                tlaText.contains("CONSTANT t") || tlaText.contains("CONSTANT t,"),
                "expected parameter t from Handler[t : Int];\n$tlaText",
            )
            assertTrue(
                !tlaText.contains("HandlerSpec_constructed"),
                "spec alias HandlerSpec should not appear as a leaf;\n$tlaText",
            )
            tla.copyTo(File(work, "NestedSpec.tla"), overwrite = true)
            cfg.copyTo(File(work, "NestedSpec.cfg"), overwrite = true)
            tla.delete()
            cfg.delete()
            // HandlerSpec.tla may also be emitted if all specs compile; clean up.
            File("HandlerSpec.tla").delete()
            File("HandlerSpec.cfg").delete()
            assertTlcHealthyStart(work, "NestedSpec")
        } finally {
            work.deleteRecursively()
            File("NestedSpec.tla").delete()
            File("NestedSpec.cfg").delete()
            File("HandlerSpec.tla").delete()
            File("HandlerSpec.cfg").delete()
        }
    }

    private fun assumeTlcPresent() {
        if (!TLC_JAR.isFile) {
            fail("TLC jar not found at ${TLC_JAR.path}")
        }
    }

    /**
     * Success = TLC parses the module and begins model checking without early errors.
     * Timeout after a healthy start is OK (TLC often does not terminate quickly).
     */
    private fun assertTlcHealthyStart(workDir: File, module: String) {
        val pb = ProcessBuilder(
            "java", "-XX:+UseParallelGC",
            "-cp", TLC_JAR.absolutePath,
            "tlc2.TLC",
            "-config", "$module.cfg",
            "$module.tla",
        ).directory(workDir).redirectErrorStream(true)
        val proc = pb.start()
        val output = StringBuilder()
        val reader = Thread {
            proc.inputStream.bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    synchronized(output) { output.appendLine(line) }
                }
            }
        }
        reader.start()
        val finished = proc.waitFor(TLC_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        if (!finished) {
            proc.destroyForcibly()
        }
        reader.join(2000)
        val text = synchronized(output) { output.toString() }
        val started = text.contains("Parsing file") && (
            text.contains("Semantic processing of module $module") ||
                text.contains(module)
            )
        val parseFail = text.contains("Parse error", ignoreCase = true) ||
            text.contains("Semantic errors", ignoreCase = true) ||
            text.contains("Unknown operator", ignoreCase = true) ||
            text.contains("is not declared in the module", ignoreCase = true) ||
            text.contains("non-enumerable quantifier bound", ignoreCase = true)
        assertTrue(started, "TLC did not parse module $module.\n$text")
        assertTrue(!parseFail, "TLC reported parse/config errors.\n$text")
        // Timeout after a healthy start is OK; deadlock / completed search also OK.
    }
}
