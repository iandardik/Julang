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
            val tlaText = tla.readText()
            assertTrue(
                tlaText.contains("CONSTANT String"),
                "expected String domain as CONSTANT;\n$tlaText",
            )
            assertTrue(
                !tlaText.contains("CONSTANT i") && !tlaText.contains("CONSTANT i,"),
                "index name i should be a binder, not a CONSTANT;\n$tlaText",
            )
            assertTrue(
                tlaText.contains("[i \\in String |->"),
                "expected Init to index Counter over String with binder i;\n$tlaText",
            )
            assertTrue(
                tlaText.contains("\\* State variables for Counter"),
                "expected Init comment for Counter state;\n$tlaText",
            )
            assertTrue(
                tlaText.contains("Counter_constructed") && tlaText.contains("/\\ n = [i \\in String |->"),
                "expected bare state var n with Leaf_constructed;\n$tlaText",
            )
            assertTrue(
                tlaText.contains("\\E i \\in String :"),
                "expected Next to quantify binder i over String;\n$tlaText",
            )
            assertTrue(
                tlaText.contains("bump(i) =="),
                "expected bump(i) operator with index as parameter;\n$tlaText",
            )
            val bumpDef = tlaText.substringAfter("bump(i) ==").substringBefore("\n\n")
            assertTrue(
                !bumpDef.trimStart().startsWith("\\E i \\in String :"),
                "index quantification should be in Next, not inside bump;\n$tlaText",
            )
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
                tlaText.contains("spawnWorker(i, id) =="),
                "expected spawnWorker(i, id) operator with index first;\n$tlaText",
            )
            assertTrue(
                tlaText.contains("~Worker_constructed[i]"),
                "expected parameterized ctor enabling on Worker for spawnWorker;\n$tlaText",
            )
            val spawnDef = tlaText.substringAfter("spawnWorker(i, id) ==").substringBefore("\n\n")
            assertTrue(
                !spawnDef.trimStart().startsWith("\\E i \\in Int :"),
                "index quantification should be in Next, not inside spawnWorker;\n$tlaText",
            )
            assertTrue(
                tlaText.contains("Worker_constructed' = [Worker_constructed EXCEPT ![i] = TRUE]"),
                "expected Worker_constructed' flip in spawnWorker;\n$tlaText",
            )
            assertTrue(
                tlaText.contains("\\E i \\in Int : \\E id \\in Int : spawnWorker(i, id)"),
                "expected Next to quantify index then args;\n$tlaText",
            )
            assertTrue(
                tlaText.contains("work(i) =="),
                "expected work(i) operator;\n$tlaText",
            )
            val workBody = tlaText.substringAfter("work(i) ==").substringBefore("\n\n")
            assertTrue(
                workBody.contains("/\\ Worker_constructed[i]"),
                "work should require Worker_constructed[i];\n$tlaText",
            )
            assertTrue(
                workBody.lines().none { it.trim() == "/\\ Server_constructed" },
                "work should not require Server_constructed;\n$tlaText",
            )
            assertTrue(
                tlaText.contains("\\* State variables for Server") &&
                    tlaText.contains("\\* State variables for Worker"),
                "expected Init state-var comments;\n$tlaText",
            )
            assertTrue(
                tlaText.contains("Worker_id") && tlaText.contains("/\\ ready = FALSE"),
                "id clashes with action arg so stays Worker_id; ready is bare;\n$tlaText",
            )
            assertTrue(
                workBody.contains("/\\ (Worker_id[i] >= 0)"),
                "work guard should use Worker_id[i];\n$tlaText",
            )
            assertTrue(
                tlaText.contains("\\* initially constructor on Server") &&
                    tlaText.contains("Server_initially =="),
                "expected disambiguation comment for Server initially;\n$tlaText",
            )
            assertTrue(
                tlaText.contains("\\* initially constructor on Worker") &&
                    tlaText.contains("Worker_initially(i) =="),
                "expected disambiguation comment for Worker initially;\n$tlaText",
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
                tlaText.contains("Counter_constructed") &&
                    tlaText.contains("\\* State variables for Counter") &&
                    tlaText.contains("/\\ n = 0"),
                "expected Counter_constructed and bare n;\n$tlaText",
            )
            assertTrue(
                tlaText.contains("Env_constructed") &&
                    tlaText.contains("\\* State variables for Env") &&
                    tlaText.contains("/\\ ready = FALSE"),
                "expected Env_constructed and bare ready;\n$tlaText",
            )
            assertTrue(
                !tlaText.contains("System_constructed"),
                "program alias System should not appear as a leaf;\n$tlaText",
            )
            assertTrue(
                !tlaText.contains("Counter_n") && !tlaText.contains("Env_ready"),
                "unique state vars should not be leaf-prefixed;\n$tlaText",
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
                tlaText.contains("Counter_constructed") &&
                    tlaText.contains("\\* State variables for Counter") &&
                    !tlaText.contains("Counter_n"),
                "expected Counter_constructed with bare n;\n$tlaText",
            )
            assertTrue(
                tlaText.contains("Handler_constructed") &&
                    tlaText.contains("\\* State variables for Handler") &&
                    !tlaText.contains("Handler_step") &&
                    tlaText.contains("step"),
                "expected Handler_constructed with bare step;\n$tlaText",
            )
            assertTrue(
                !tlaText.contains("CONSTANT t") && !tlaText.contains("CONSTANT t,"),
                "parameter name t should be a binder, not a CONSTANT;\n$tlaText",
            )
            assertTrue(
                tlaText.contains("[t \\in Int |->"),
                "expected Init to index Handler over Int with binder t;\n$tlaText",
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

    @Test
    fun objFieldAccessEmitsDotNotUnderscore() {
        assumeTlcPresent()
        val work = Files.createTempDirectory("julay-spec-objfield").toFile()
        try {
            val source = File("regression/input/spec/obj-field-access.jul")
            assertTrue(source.exists(), "missing ${source.path}")
            compileJulFile(
                source.toPath(),
                keepBuild = false,
                targets = CompileTargets(compilePrograms = false, compileSpecs = true),
            )
            val tla = File("ObjFieldAccess.tla")
            val cfg = File("ObjFieldAccess.cfg")
            assertTrue(tla.exists(), "expected ObjFieldAccess.tla")
            assertTrue(cfg.exists(), "expected ObjFieldAccess.cfg")
            val tlaText = tla.readText()
            assertTrue(
                tlaText.contains("peerArg.id"),
                "expected action-arg record field as peerArg.id;\n$tlaText",
            )
            assertTrue(
                !tlaText.contains("peerArg_id"),
                "action-arg field must not use underscore peerArg_id;\n$tlaText",
            )
            assertTrue(
                tlaText.contains("peer[i].id"),
                "expected parameterized state-var field as peer[i].id;\n$tlaText",
            )
            assertTrue(
                !tlaText.contains("peer_id"),
                "state-var field must not use underscore peer_id;\n$tlaText",
            )
            tla.copyTo(File(work, "ObjFieldAccess.tla"), overwrite = true)
            cfg.copyTo(File(work, "ObjFieldAccess.cfg"), overwrite = true)
            tla.delete()
            cfg.delete()
            assertTlcHealthyStart(work, "ObjFieldAccess")
        } finally {
            work.deleteRecursively()
            File("ObjFieldAccess.tla").delete()
            File("ObjFieldAccess.cfg").delete()
        }
    }

    @Test
    fun sessionPairBothSidesEmitsAffinityAndEndSession() {
        assumeTlcPresent()
        val work = Files.createTempDirectory("julay-spec-session-pair").toFile()
        try {
            val source = File("regression/input/spec/session-pair.jul")
            assertTrue(source.exists(), "missing ${source.path}")
            compileJulFile(
                source.toPath(),
                keepBuild = false,
                targets = CompileTargets(compilePrograms = false, compileSpecs = true),
            )
            val tla = File("SessionPair.tla")
            val cfg = File("SessionPair.cfg")
            assertTrue(tla.exists(), "expected SessionPair.tla")
            assertTrue(cfg.exists(), "expected SessionPair.cfg")
            val tlaText = tla.readText()
            assertTrue(
                tlaText.contains("session_Alice_Bob"),
                "expected session_Alice_Bob variable;\n$tlaText",
            )
            assertTrue(
                tlaText.contains("CanStartSession_Alice_Bob =="),
                "expected CanStartSession helper before Init;\n$tlaText",
            )
            val canStartIdx = tlaText.indexOf("CanStartSession_Alice_Bob ==")
            val initIdx = tlaText.indexOf("Init ==")
            assertTrue(
                canStartIdx in 0 until initIdx,
                "CanStartSession should appear before Init;\n$tlaText",
            )
            assertTrue(
                tlaText.contains("Alice_dead ==") && tlaText.contains("Bob_dead =="),
                "expected Alice_dead and Bob_dead;\n$tlaText",
            )
            assertTrue(
                tlaText.contains(
                    "\\* True exactly when all of Alice's actions are no longer enabled, in which case Alice dies.",
                ),
                "expected Alice_dead comment;\n$tlaText",
            )
            assertTrue(
                tlaText.contains("\\* Session connection semantics"),
                "expected session connection comment in actions;\n$tlaText",
            )
            val meetDef = tlaText.substringAfter("meet ==").substringBefore("\n\n")
            assertTrue(
                meetDef.contains("session_Alice_Bob \\/ CanStartSession_Alice_Bob") ||
                    meetDef.contains("(session_Alice_Bob \\/ CanStartSession_Alice_Bob)"),
                "expected meet to gate on session or CanStartSession;\n$meetDef",
            )
            assertTrue(
                meetDef.contains("session_Alice_Bob' = TRUE"),
                "expected meet to set session TRUE;\n$meetDef",
            )
            val sessionCommentIdx = meetDef.indexOf("\\* Session connection semantics")
            val unchangedIdx = meetDef.indexOf("UNCHANGED")
            assertTrue(
                sessionCommentIdx in 0 until unchangedIdx,
                "session block should be last before UNCHANGED in meet;\n$meetDef",
            )
            assertTrue(
                tlaText.contains("EndSession_Alice ==") && tlaText.contains("EndSession_Bob =="),
                "expected EndSession_Alice and EndSession_Bob;\n$tlaText",
            )
            tla.copyTo(File(work, "SessionPair.tla"), overwrite = true)
            cfg.copyTo(File(work, "SessionPair.cfg"), overwrite = true)
            tla.delete()
            cfg.delete()
            File("SessionAliceOnly.tla").delete()
            File("SessionAliceOnly.cfg").delete()
            assertTlcHealthyStart(work, "SessionPair")
        } finally {
            work.deleteRecursively()
            File("SessionPair.tla").delete()
            File("SessionPair.cfg").delete()
            File("SessionAliceOnly.tla").delete()
            File("SessionAliceOnly.cfg").delete()
        }
    }

    @Test
    fun sessionPairOneSideOmitsAffinity() {
        assumeTlcPresent()
        val work = Files.createTempDirectory("julay-spec-session-onesided").toFile()
        try {
            val source = File("regression/input/spec/session-pair.jul")
            assertTrue(source.exists(), "missing ${source.path}")
            compileJulFile(
                source.toPath(),
                keepBuild = false,
                targets = CompileTargets(compilePrograms = false, compileSpecs = true),
            )
            val tla = File("SessionAliceOnly.tla")
            val cfg = File("SessionAliceOnly.cfg")
            assertTrue(tla.exists(), "expected SessionAliceOnly.tla")
            assertTrue(cfg.exists(), "expected SessionAliceOnly.cfg")
            val tlaText = tla.readText()
            assertTrue(
                !tlaText.contains("session_"),
                "one-sided spec should not emit session_ vars;\n$tlaText",
            )
            assertTrue(
                !tlaText.contains("CanStartSession_") && !tlaText.contains("EndSession_"),
                "one-sided spec should not emit session helpers/EndSession;\n$tlaText",
            )
            assertTrue(
                !tlaText.contains("_dead =="),
                "one-sided spec should not emit *_dead operators;\n$tlaText",
            )
            assertTrue(
                tlaText.contains("meet =="),
                "expected meet action without session gating;\n$tlaText",
            )
            tla.copyTo(File(work, "SessionAliceOnly.tla"), overwrite = true)
            cfg.copyTo(File(work, "SessionAliceOnly.cfg"), overwrite = true)
            tla.delete()
            cfg.delete()
            File("SessionPair.tla").delete()
            File("SessionPair.cfg").delete()
            assertTlcHealthyStart(work, "SessionAliceOnly")
        } finally {
            work.deleteRecursively()
            File("SessionPair.tla").delete()
            File("SessionPair.cfg").delete()
            File("SessionAliceOnly.tla").delete()
            File("SessionAliceOnly.cfg").delete()
        }
    }

    @Test
    fun sessionPairParamBothSidesEmitsIndexedAffinity() {
        assumeTlcPresent()
        val work = Files.createTempDirectory("julay-spec-session-pair-param").toFile()
        try {
            val source = File("regression/input/spec/session-pair-param.jul")
            assertTrue(source.exists(), "missing ${source.path}")
            compileJulFile(
                source.toPath(),
                keepBuild = false,
                targets = CompileTargets(compilePrograms = false, compileSpecs = true),
            )
            val tla = File("SessionPairParam.tla")
            val cfg = File("SessionPairParam.cfg")
            assertTrue(tla.exists(), "expected SessionPairParam.tla")
            assertTrue(cfg.exists(), "expected SessionPairParam.cfg")
            val tlaText = tla.readText()
            assertTrue(
                tlaText.contains("session_Alice_Bob = [a \\in Int |-> [b \\in Int |-> FALSE]]"),
                "expected indexed session_Alice_Bob init;\n$tlaText",
            )
            assertTrue(
                tlaText.contains("CanStartSession_Alice_Bob(a, b) =="),
                "expected parameterized CanStartSession;\n$tlaText",
            )
            assertTrue(
                tlaText.contains("~\\E b2 \\in Int : session_Alice_Bob[a][b2]") &&
                    tlaText.contains("~\\E a2 \\in Int : session_Alice_Bob[a2][b]"),
                "expected CanStartSession exclusivity quantifiers;\n$tlaText",
            )
            assertTrue(
                tlaText.contains("Alice_dead(a) ==") && tlaText.contains("Bob_dead(b) =="),
                "expected parameterized *_dead operators;\n$tlaText",
            )
            assertTrue(
                tlaText.contains("meet(a, b) ==") &&
                    tlaText.contains("session_Alice_Bob[a][b] \\/ CanStartSession_Alice_Bob(a, b)"),
                "expected meet(a, b) with session gate;\n$tlaText",
            )
            assertTrue(
                tlaText.contains("EndSession_Alice(a, b) ==") &&
                    tlaText.contains("EndSession_Bob(a, b) =="),
                "expected parameterized EndSession operators;\n$tlaText",
            )
            assertTrue(
                tlaText.contains("\\E a \\in Int : \\E b \\in Int : meet(a, b)"),
                "expected Next to quantify meet over a and b;\n$tlaText",
            )
            tla.copyTo(File(work, "SessionPairParam.tla"), overwrite = true)
            cfg.copyTo(File(work, "SessionPairParam.cfg"), overwrite = true)
            tla.delete()
            cfg.delete()
            File("SessionAliceOnlyParam.tla").delete()
            File("SessionAliceOnlyParam.cfg").delete()
            assertTlcHealthyStart(work, "SessionPairParam")
        } finally {
            work.deleteRecursively()
            File("SessionPairParam.tla").delete()
            File("SessionPairParam.cfg").delete()
            File("SessionAliceOnlyParam.tla").delete()
            File("SessionAliceOnlyParam.cfg").delete()
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
