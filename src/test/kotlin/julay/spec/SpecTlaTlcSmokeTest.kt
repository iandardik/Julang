package julay.spec

import julay.compiler.compileJulFile
import java.io.File
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.test.fail

private val TLC_JAR: File
    get() {
        val prop = System.getProperty("tla2tools.jar")
        if (prop.isNullOrBlank()) {
            fail("System property tla2tools.jar is not set (Gradle should download and pass it)")
        }
        return File(prop)
    }
private const val TLC_TIMEOUT_SECONDS = 30L
private const val TLC_VIOLATION_TIMEOUT_SECONDS = 120L

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
                keepBuild = false
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
                keepBuild = false
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
    fun sortIndexCompilesWithExactCfgConstant() {
        assumeTlcPresent()
        val work = Files.createTempDirectory("julay-spec-sort").toFile()
        try {
            val source = File("regression/input/spec/sort-index.jul")
            assertTrue(source.exists(), "missing ${source.path}")
            compileJulFile(
                source.toPath(),
                keepBuild = false,
            )
            val tla = File("SortIndex.tla")
            val cfg = File("SortIndex.cfg")
            assertTrue(tla.exists(), "expected SortIndex.tla")
            assertTrue(cfg.exists(), "expected SortIndex.cfg")
            val tlaText = tla.readText()
            val cfgText = cfg.readText()
            assertTrue(
                tlaText.contains("CONSTANT Node"),
                "expected Node domain as CONSTANT;\n$tlaText",
            )
            assertTrue(
                tlaText.contains("[i \\in Node |->"),
                "expected Init to index Counter over Node;\n$tlaText",
            )
            assertTrue(
                cfgText.contains("CONSTANT Node = {\"n1\", \"n2\", \"n3\"}"),
                "expected exact Node assignment in cfg;\n$cfgText",
            )
            tla.copyTo(File(work, "SortIndex.tla"), overwrite = true)
            cfg.copyTo(File(work, "SortIndex.cfg"), overwrite = true)
            tla.delete()
            cfg.delete()
            assertTlcHealthyStart(work, "SortIndex")
        } finally {
            work.deleteRecursively()
            File("SortIndex.tla").delete()
            File("SortIndex.cfg").delete()
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
                keepBuild = false
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
            assertFalse(
                tlaText.contains("work(i) ==") || Regex("""\\/\s+.*\bwork\b""").containsMatchIn(tlaText),
                "guard-only work should be omitted from defs/Next;\n$tlaText",
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
                tlaText.contains("\\* initially constructor on Server") &&
                    tlaText.contains("Server_initially =="),
                "expected disambiguation comment for Server initially;\n$tlaText",
            )
            assertTrue(
                tlaText.contains("\\* initially constructor on Worker") &&
                    tlaText.contains("Worker_initially(i) =="),
                "expected disambiguation comment for Worker initially;\n$tlaText",
            )
            assertTrue(
                spawnDef.contains("\\* Server action logic") &&
                    spawnDef.contains("\\* Worker action logic") &&
                    spawnDef.indexOf("\\* Server action logic") < spawnDef.indexOf("\\* Worker action logic"),
                "expected per-proc action logic comments in spawnWorker;\n$spawnDef",
            )
            val afterInit = tlaText.substringAfter("Init ==")
            val serverInitIdx = afterInit.indexOf("Server_initially ==")
            val workerInitIdx = afterInit.indexOf("Worker_initially(i) ==")
            val spawnIdx = afterInit.indexOf("spawnWorker(i, id) ==")
            assertTrue(
                serverInitIdx >= 0 && workerInitIdx >= 0 && spawnIdx >= 0 &&
                    serverInitIdx < spawnIdx && workerInitIdx < spawnIdx,
                "initially defs should appear after Init and before spawnWorker;\n$tlaText",
            )
            val nextBody = tlaText.substringAfter("Next ==").substringBefore("\n\n")
            val nextServer = nextBody.indexOf("Server_initially")
            val nextWorker = nextBody.indexOf("Worker_initially")
            val nextSpawn = nextBody.indexOf("spawnWorker")
            assertTrue(
                nextServer >= 0 && nextWorker >= 0 && nextSpawn >= 0 &&
                    nextServer < nextSpawn && nextWorker < nextSpawn,
                "initially should lead Next before spawnWorker;\n$nextBody",
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
                keepBuild = false
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
                "proc alias System should not appear as a leaf;\n$tlaText",
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
                keepBuild = false
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
                keepBuild = false
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
    fun multiLineObjLiteralEmitsOneFieldPerLine() {
        val source = File("regression/input/spec/obj-record-layout.jul")
        assertTrue(source.exists(), "missing ${source.path}")
        try {
            compileJulFile(source.toPath(), keepBuild = false)
            val tla = File("ObjRecordLayout.tla")
            assertTrue(tla.exists(), "expected ObjRecordLayout.tla")
            val tlaText = tla.readText()
            val paintDef = tlaText.substringAfter("paint ==").substringBefore("\n\n")
            assertTrue(
                paintDef.contains("p' = [\n       x |-> 1,\n       y |-> 2\n     ]"),
                "multi-line Julay obj fields should be +2 past first non-/\\ symbol;\n$paintDef",
            )
            val initDef = tlaText.substringAfter("initially ==").substringBefore("\n\n")
                .ifEmpty { tlaText.substringAfter("Painter_initially ==").substringBefore("\n\n") }
            assertTrue(
                initDef.contains("p' = [x |-> 0, y |-> 0]") ||
                    initDef.contains("[x |-> 0, y |-> 0]"),
                "single-line Julay obj should stay compact;\n$initDef",
            )
            assertFalse(
                initDef.contains("x |-> 0,\n"),
                "single-line Julay obj should not be expanded;\n$initDef",
            )
        } finally {
            File("ObjRecordLayout.tla").delete()
            File("ObjRecordLayout.cfg").delete()
        }
    }

    @Test
    fun nestedMultiLineBoolFormatsRecursively() {
        val source = File("regression/input/spec/nested-bool-layout.jul")
        assertTrue(source.exists(), "missing ${source.path}")
        try {
            compileJulFile(source.toPath(), keepBuild = false)
            val tla = File("NestedBoolLayout.tla")
            assertTrue(tla.exists(), "expected NestedBoolLayout.tla")
            val chooseDef = tla.readText().substringAfter("choose ==").substringBefore("\n\n")
            assertTrue(
                (chooseDef.contains("\\/ /\\ ~ok") || chooseDef.contains("\\/ (/\\ ~ok")) &&
                    chooseDef.contains("/\\ n = 0") &&
                    (chooseDef.contains("\\/ /\\ ok") || chooseDef.contains("\\/ (/\\ ok")) &&
                    chooseDef.contains("/\\ n = 1"),
                "nested multi-line |/& should emit recursive \\/ and /\\ lines;\n$chooseDef",
            )
            assertFalse(
                chooseDef.contains("((~ok /\\ n = 0) \\/") ||
                    chooseDef.contains("((~(ok) /\\ (n = 0)) \\/"),
                "should not keep nested multi-line bool flat;\n$chooseDef",
            )
        } finally {
            File("NestedBoolLayout.tla").delete()
            File("NestedBoolLayout.cfg").delete()
        }
    }

    @Test
    fun invariantFormattingPreservesLayoutAndUserParens() {
        val source = File("regression/input/spec/inv-layout.jul")
        assertTrue(source.exists(), "missing ${source.path}")
        try {
            compileJulFile(source.toPath(), keepBuild = false)
            val tla = File("InvLayout.tla")
            assertTrue(tla.exists(), "expected InvLayout.tla")
            val tlaText = tla.readText()
            val invDef = tlaText.substringAfter("OneLeaderPerTerm ==").substringBefore("\n====")
            assertTrue(
                invDef.contains("\\A n1 \\in NodeSet :") &&
                    invDef.contains("\\A n2 \\in NodeSet :"),
                "multi-line quantifiers should be nested on separate lines;\n$invDef",
            )
            assertTrue(
                invDef.contains(
                    "(state[n1] = \"Leader\" /\\ state[n2] = \"Leader\" /\\ currentTerm[n1] = currentTerm[n2]) => (n1 = n2)",
                ),
                "user-written parentheses around antecedent and consequent should be preserved;\n$invDef",
            )
            assertFalse(
                invDef.contains("(\\A n1"),
                "quantifiers should not be wrapped in outer parens;\n$invDef",
            )
        } finally {
            File("InvLayout.tla").delete()
            File("InvLayout.cfg").delete()
        }
    }

    @Test
    fun ifAndLetFormattingPreservesMultiLineLayout() {
        val source = File("regression/input/spec/if-let-layout.jul")
        assertTrue(source.exists(), "missing ${source.path}")
        try {
            compileJulFile(source.toPath(), keepBuild = false)
            val tla = File("IfLetLayout.tla")
            assertTrue(tla.exists(), "expected IfLetLayout.tla")
            val tlaText = tla.readText()
            val stepDef = tlaText.substringAfter("step ==").substringBefore("\n\n")
            assertTrue(
                stepDef.contains("IF x < 1 THEN") &&
                    stepDef.contains("ELSE") &&
                    Regex("""THEN\n\s+LET n ==""").containsMatchIn(stepDef),
                "multi-line if should put THEN body / nested LET on following lines;\n$stepDef",
            )
            assertTrue(
                stepDef.contains("LET n ==") && stepDef.contains("IN"),
                "expression-level let should emit TLA LET, not inline;\n$stepDef",
            )
            assertTrue(
                stepDef.contains("LET a == 1 IN a + y"),
                "single-line let should stay compact;\n$stepDef",
            )
            assertFalse(
                stepDef.contains("IF x < 1 THEN LET n =="),
                "multi-line if should not keep THEN body on the same line;\n$stepDef",
            )
        } finally {
            File("IfLetLayout.tla").delete()
            File("IfLetLayout.cfg").delete()
        }
    }

    @Test
    fun openColumnIndentForCupAndNestedIf() {
        val source = File("regression/input/spec/open-column-indent.jul")
        assertTrue(source.exists(), "missing ${source.path}")
        try {
            compileJulFile(source.toPath(), keepBuild = false)
            val tla = File("OpenColumnIndent.tla")
            assertTrue(tla.exists(), "expected OpenColumnIndent.tla")
            val sendDef = tla.readText().substringAfter("send(n, m) ==").substringBefore("\n\n")
            assertTrue(
                sendDef.contains("msgs' = msgs \\cup {\n       [\n         src |-> n,\n         dest |-> m\n       ]\n     }"),
                "set-union record should indent from conjunct open column, not under \\cup;\n$sendDef",
            )
            assertTrue(
                sendDef.contains("longishStateVar' = IF longishStateVar < 1 THEN\n       IF n > 0 THEN\n         n\n       ELSE\n         0\n     ELSE\n       longishStateVar"),
                "nested IF under a long assign should use open-column indent, not full prefix length;\n$sendDef",
            )
            assertFalse(
                Regex("""\\cup \{\n {20,}\[""").containsMatchIn(sendDef),
                "must not hang set contents under the full LHS+\\cup prefix;\n$sendDef",
            )
        } finally {
            File("OpenColumnIndent.tla").delete()
            File("OpenColumnIndent.cfg").delete()
        }
    }

    @Test
    fun userFunsBecomeTlaOperatorsAboveInit() {
        val source = File("regression/input/spec/fun-ops.jul")
        assertTrue(source.exists(), "missing ${source.path}")
        try {
            compileJulFile(source.toPath(), keepBuild = false)
            val tla = File("FunOps.tla")
            assertTrue(tla.exists(), "expected FunOps.tla")
            val tlaText = tla.readText()
            val beforeInit = tlaText.substringBefore("Init ==")
            assertTrue(
                beforeInit.contains("\\* fun") &&
                    beforeInit.contains("entryTermAt(p_log, idx) ==") &&
                    beforeInit.contains("bumpTerm(t) =="),
                "used funs should be operators above Init; params colliding with VARIABLES are renamed;\n$beforeInit",
            )
            assertTrue(
                beforeInit.indexOf("entryTermAt(p_log, idx) ==") < beforeInit.indexOf("bumpTerm(t) =="),
                "callee fun should be defined before caller;\n$beforeInit",
            )
            val advanceDef = tlaText.substringAfter("advance(").substringBefore("\n\n")
            assertTrue(
                advanceDef.contains("entryTermAt(log, newCommitIndex) = currentTerm"),
                "call sites should keep the fun name;\n$advanceDef",
            )
            assertTrue(
                advanceDef.contains("bumpTerm(currentTerm)"),
                "transit RHS should call fun operators;\n$advanceDef",
            )
            assertFalse(
                advanceDef.contains("TRUE = currentTerm"),
                "fun calls must not degrade to TRUE;\n$advanceDef",
            )
        } finally {
            File("FunOps.tla").delete()
            File("FunOps.cfg").delete()
        }
    }

    @Test
    fun transitLetsEmitAsTlaLet() {
        val source = File("regression/input/spec/transit-let-layout.jul")
        assertTrue(source.exists(), "missing ${source.path}")
        try {
            compileJulFile(source.toPath(), keepBuild = false)
            val tla = File("TransitLetLayout.tla")
            assertTrue(tla.exists(), "expected TransitLetLayout.tla")
            val stepDef = tla.readText().substringAfter("step ==").substringBefore("\n\n")
            assertTrue(
                stepDef.contains("LET alreadyDone =="),
                "transit let should emit TLA LET;\n$stepDef",
            )
            assertTrue(
                stepDef.contains("alreadyDone") &&
                    !stepDef.contains("IF flag /\\ n > 0 THEN n ELSE n + 1"),
                "later assigns should reference the let name, not inline the init;\n$stepDef",
            )
        } finally {
            File("TransitLetLayout.tla").delete()
            File("TransitLetLayout.cfg").delete()
        }
    }

    @Test
    fun whenEmitsAsTlaCase() {
        val source = File("regression/input/spec/when-case-layout.jul")
        assertTrue(source.exists(), "missing ${source.path}")
        try {
            compileJulFile(source.toPath(), keepBuild = false)
            val tla = File("WhenCaseLayout.tla")
            assertTrue(tla.exists(), "expected WhenCaseLayout.tla")
            val stepDef = tla.readText().substringAfter("step ==").substringBefore("\n\n")
            assertTrue(
                stepDef.contains("CASE") && stepDef.contains("OTHER"),
                "when should emit TLA CASE with OTHER;\n$stepDef",
            )
            assertFalse(
                Regex("""n' = TRUE\b""").containsMatchIn(stepDef),
                "when must not degrade to TRUE;\n$stepDef",
            )
        } finally {
            File("WhenCaseLayout.tla").delete()
            File("WhenCaseLayout.cfg").delete()
        }
    }

    @Test
    fun startsWithEmitsHelperOperator() {
        val source = File("regression/input/spec/starts-with-layout.jul")
        assertTrue(source.exists(), "missing ${source.path}")
        try {
            compileJulFile(source.toPath(), keepBuild = false)
            val tla = File("StartsWithLayout.tla")
            assertTrue(tla.exists(), "expected StartsWithLayout.tla")
            val tlaText = tla.readText()
            val beforeInit = tlaText.substringBefore("Init ==")
            assertTrue(
                beforeInit.contains("\\* startsWith") && beforeInit.contains("startsWith("),
                "startsWith helper should be above Init;\n$beforeInit",
            )
            assertTrue(
                tlaText.contains("startsWith(msg, \"he\")"),
                "call site should use startsWith;\n$tlaText",
            )
        } finally {
            File("StartsWithLayout.tla").delete()
            File("StartsWithLayout.cfg").delete()
        }
    }

    @Test
    fun spliceParamsClashRenamedAgainstVariables() {
        val source = File("regression/input/spec/splice-clash-layout.jul")
        assertTrue(source.exists(), "missing ${source.path}")
        try {
            compileJulFile(source.toPath(), keepBuild = false)
            val tla = File("SpliceClashLayout.tla")
            assertTrue(tla.exists(), "expected SpliceClashLayout.tla")
            val beforeInit = tla.readText().substringBefore("Init ==")
            assertTrue(
                beforeInit.contains("\\* splice") && beforeInit.contains("splice(p_xs,"),
                "splice param xs should be renamed when VARIABLES has xs;\n$beforeInit",
            )
        } finally {
            File("SpliceClashLayout.tla").delete()
            File("SpliceClashLayout.cfg").delete()
        }
    }

    @Test
    fun stringCoerceElidesEmptyConcat() {
        val source = File("regression/input/spec/string-coerce.jul")
        assertTrue(source.exists(), "missing ${source.path}")
        try {
            compileJulFile(source.toPath(), keepBuild = false)
            val tla = File("StringCoerce.tla")
            assertTrue(tla.exists(), "expected StringCoerce.tla")
            val tlaText = tla.readText()
            assertFalse(
                tlaText.contains("\\o \"\""),
                "expected empty-string concat identity to be elided;\n$tlaText",
            )
            assertTrue(
                tlaText.contains("ToString("),
                "expected ToString for int-to-string coerce;\n$tlaText",
            )
        } finally {
            File("StringCoerce.tla").delete()
            File("StringCoerce.cfg").delete()
        }
    }

    @Test
    fun guardOnlyActionOmittedFromTla() {
        val source = File("regression/input/spec/stutter-omit.jul")
        assertTrue(source.exists(), "missing ${source.path}")
        try {
            compileJulFile(source.toPath(), keepBuild = false)
            val tla = File("StutterOmit.tla")
            assertTrue(tla.exists(), "expected StutterOmit.tla")
            val tlaText = tla.readText()
            assertTrue(
                tlaText.contains("step =="),
                "expected state-changing step action;\n$tlaText",
            )
            assertTrue(
                tlaText.contains("\\/ step"),
                "expected step in Next;\n$tlaText",
            )
            assertFalse(
                tlaText.contains("noop =="),
                "guard-only noop should be omitted from action defs;\n$tlaText",
            )
            assertFalse(
                Regex("""\\/\s+noop\b""").containsMatchIn(tlaText),
                "guard-only noop should be omitted from Next;\n$tlaText",
            )
            val stepDef = tlaText.substringAfter("step ==").substringBefore("\n\n")
            assertTrue(
                stepDef.contains("/\\ n < 3") &&
                    stepDef.contains("/\\ n >= 0") &&
                    stepDef.indexOf("/\\ n < 3") < stepDef.indexOf("/\\ n >= 0"),
                "top-level & guard conjuncts should be separate /\\ lines;\n$stepDef",
            )
            assertFalse(
                stepDef.contains("/\\ ((n < 3) /\\ (n >= 0))") ||
                    stepDef.contains("/\\ (n < 3 /\\ n >= 0)"),
                "should not nest top-level & as one conjunct;\n$stepDef",
            )
        } finally {
            File("StutterOmit.tla").delete()
            File("StutterOmit.cfg").delete()
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
                keepBuild = false
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
                !tlaText.contains("sessionException"),
                "transition-only session pair should omit sessionException;\n$tlaText",
            )
            assertTrue(
                !tlaText.contains("SessionIntegrity"),
                "transition-only session pair should omit SessionIntegrity;\n$tlaText",
            )
            val cfgText = cfg.readText()
            assertTrue(
                !cfgText.contains("INVARIANT SessionIntegrity"),
                "transition-only session pair should omit SessionIntegrity from cfg;\n$cfgText",
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
                !tlaText.contains("Alice_killed") && !tlaText.contains("Bob_killed"),
                "no killSessionPeer → omit *_killed;\n$tlaText",
            )
            assertTrue(
                tlaText.contains("\\* True when all of Alice's actions are disabled."),
                "expected Alice_dead natural-death comment;\n$tlaText",
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
                keepBuild = false
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
                keepBuild = false
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

    @Test
    fun sessionSpawnRebindViolatesSessionIntegrity() {
        assumeTlcPresent()
        val work = Files.createTempDirectory("julay-spec-session-spawn-rebind").toFile()
        try {
            val source = File("regression/input/spec/session-spawn-rebind.jul")
            assertTrue(source.exists(), "missing ${source.path}")
            compileJulFile(
                source.toPath(),
                keepBuild = false
            )
            val tla = File("SessionSpawnRebind.tla")
            val cfg = File("SessionSpawnRebind.cfg")
            assertTrue(tla.exists(), "expected SessionSpawnRebind.tla")
            assertTrue(cfg.exists(), "expected SessionSpawnRebind.cfg")
            val tlaText = tla.readText()
            assertTrue(
                tlaText.contains("sessionException"),
                "expected sessionException;\n$tlaText",
            )
            assertTrue(
                tlaText.contains("SessionIntegrity == ~sessionException"),
                "expected SessionIntegrity;\n$tlaText",
            )
            assertTrue(
                tlaText.contains("\\* Session connection semantics"),
                "expected session connection comment;\n$tlaText",
            )
            val spawnDef = tlaText.substringAfter("spawnWorker(i, id) ==").substringBefore("\n\n")
                .ifEmpty { tlaText.substringAfter("spawnWorker(").let { rest ->
                    val sigEnd = rest.indexOf(" ==")
                    if (sigEnd < 0) "" else rest.substring(sigEnd + 3).substringBefore("\n\n")
                } }
            assertTrue(
                spawnDef.contains("IF CanStartSession_Server_Worker") ||
                    spawnDef.contains("IF CanStartSession_"),
                "expected spawnWorker to gate spawn on CanStartSession IF;\n$spawnDef",
            )
            assertTrue(
                spawnDef.contains("sessionException' = TRUE"),
                "expected sessionException' on rebind ELSE;\n$spawnDef",
            )
            assertFalse(
                spawnDef.contains("session_Server_Worker \\/ CanStartSession") ||
                    spawnDef.contains("(session_Server_Worker \\/ CanStartSession"),
                "ctor-bearing action should not use sticky session \\/ CanStart;\n$spawnDef",
            )
            val cfgText = cfg.readText()
            assertTrue(
                cfgText.contains("INVARIANT SessionIntegrity"),
                "expected SessionIntegrity in cfg;\n$cfgText",
            )
            tla.copyTo(File(work, "SessionSpawnRebind.tla"), overwrite = true)
            cfg.copyTo(File(work, "SessionSpawnRebind.cfg"), overwrite = true)
            tla.delete()
            cfg.delete()
            assertTlcInvariantViolation(work, "SessionSpawnRebind", "SessionIntegrity")
        } finally {
            work.deleteRecursively()
            File("SessionSpawnRebind.tla").delete()
            File("SessionSpawnRebind.cfg").delete()
        }
    }

    @Test
    fun sessionExitEffectClearsSessionInTla() {
        assumeTlcPresent()
        val work = Files.createTempDirectory("julay-spec-session-exit").toFile()
        try {
            val source = File("regression/input/spec/session-exit.jul")
            assertTrue(source.exists(), "missing ${source.path}")
            compileJulFile(source.toPath(), keepBuild = false)
            val tla = File("SessionExit.tla")
            val cfg = File("SessionExit.cfg")
            assertTrue(tla.exists(), "expected SessionExit.tla")
            assertTrue(cfg.exists(), "expected SessionExit.cfg")
            val tlaText = tla.readText()
            assertTrue(
                !tlaText.contains("Alice_killed") && !tlaText.contains("Bob_killed"),
                "exitSession should not emit *_killed;\n$tlaText",
            )
            assertTrue(
                !tlaText.contains("sessionException"),
                "exit-only session should omit sessionException;\n$tlaText",
            )
            val meetDef = tlaText.substringAfter("meet ==").substringBefore("\n\n")
            assertTrue(
                meetDef.contains("session_Alice_Bob' = FALSE"),
                "expected exitSession to clear session;\n$meetDef",
            )
            assertTrue(
                meetDef.contains("IF session_Alice_Bob"),
                "expected exitSession no-op when session absent;\n$meetDef",
            )
            assertFalse(
                meetDef.contains("Bob_killed' = TRUE") || meetDef.contains("Alice_killed' = TRUE"),
                "exitSession must not set killed;\n$meetDef",
            )
            tla.copyTo(File(work, "SessionExit.tla"), overwrite = true)
            cfg.copyTo(File(work, "SessionExit.cfg"), overwrite = true)
            tla.delete()
            cfg.delete()
            assertTlcHealthyStart(work, "SessionExit")
        } finally {
            work.deleteRecursively()
            File("SessionExit.tla").delete()
            File("SessionExit.cfg").delete()
        }
    }

    @Test
    fun sessionKillEffectSetsPeerKilledInTla() {
        assumeTlcPresent()
        val work = Files.createTempDirectory("julay-spec-session-kill").toFile()
        try {
            val source = File("regression/input/spec/session-kill.jul")
            assertTrue(source.exists(), "missing ${source.path}")
            compileJulFile(source.toPath(), keepBuild = false)
            val tla = File("SessionKill.tla")
            val cfg = File("SessionKill.cfg")
            assertTrue(tla.exists(), "expected SessionKill.tla")
            assertTrue(cfg.exists(), "expected SessionKill.cfg")
            val tlaText = tla.readText()
            assertTrue(
                tlaText.contains("Victim_killed"),
                "expected Victim_killed for kill target;\n$tlaText",
            )
            assertTrue(
                !tlaText.contains("Keeper_killed"),
                "caller that is not a kill target should omit Keeper_killed;\n$tlaText",
            )
            val killDef = tlaText.substringAfter("killPeer ==").substringBefore("\n\n")
            assertTrue(
                killDef.contains("session_Keeper_Victim' = FALSE"),
                "expected kill to clear session;\n$killDef",
            )
            assertTrue(
                killDef.contains("IF session_Keeper_Victim"),
                "expected killSessionPeer no-op when session absent;\n$killDef",
            )
            assertTrue(
                killDef.contains("Victim_killed' = TRUE"),
                "expected killSessionPeer to set Victim_killed;\n$killDef",
            )
            tla.copyTo(File(work, "SessionKill.tla"), overwrite = true)
            cfg.copyTo(File(work, "SessionKill.cfg"), overwrite = true)
            tla.delete()
            cfg.delete()
            assertTlcHealthyStart(work, "SessionKill")
        } finally {
            work.deleteRecursively()
            File("SessionKill.tla").delete()
            File("SessionKill.cfg").delete()
        }
    }

    @Test
    fun exitProcSetsSelfKilledInTla() {
        assumeTlcPresent()
        val work = Files.createTempDirectory("julay-spec-exit-proc").toFile()
        try {
            val source = File("regression/input/spec/exit-proc.jul")
            assertTrue(source.exists(), "missing ${source.path}")
            compileJulFile(source.toPath(), keepBuild = false)
            val tla = File("ExitProcSpec.tla")
            val cfg = File("ExitProcSpec.cfg")
            assertTrue(tla.exists(), "expected ExitProcSpec.tla")
            assertTrue(cfg.exists(), "expected ExitProcSpec.cfg")
            val tlaText = tla.readText()
            assertTrue(
                tlaText.contains("Leaf_killed"),
                "expected Leaf_killed for exitProc caller;\n$tlaText",
            )
            val dieDef = tlaText.substringAfter("die ==").substringBefore("\n\n")
            assertTrue(
                dieDef.contains("Leaf_killed' = TRUE"),
                "expected exitProc action to set Leaf_killed;\n$dieDef",
            )
            assertTrue(
                tlaText.contains("~Leaf_killed"),
                "expected ~Leaf_killed enablement gates;\n$tlaText",
            )
            tla.copyTo(File(work, "ExitProcSpec.tla"), overwrite = true)
            cfg.copyTo(File(work, "ExitProcSpec.cfg"), overwrite = true)
            tla.delete()
            cfg.delete()
            assertTlcHealthyStart(work, "ExitProcSpec")
        } finally {
            work.deleteRecursively()
            File("ExitProcSpec.tla").delete()
            File("ExitProcSpec.cfg").delete()
        }
    }

    @Test
    fun procfunCountUpEmitsTerminatedAndTlcParses() {
        assumeTlcPresent()
        val work = Files.createTempDirectory("julay-spec-procfun").toFile()
        try {
            val source = File("regression/input/spec/procfun-count-up.jul")
            assertTrue(source.exists(), "missing ${source.path}")
            compileJulFile(source.toPath(), keepBuild = false)
            val tla = File("CountUpSpec.tla")
            val cfg = File("CountUpSpec.cfg")
            assertTrue(tla.exists(), "expected CountUpSpec.tla")
            assertTrue(cfg.exists(), "expected CountUpSpec.cfg")
            val tlaText = tla.readText()
            val cfgText = cfg.readText()
            assertTrue(
                tlaText.contains("countUp_terminated") || tlaText.contains("terminated"),
                "expected terminated state for procfun;\n$tlaText",
            )
            assertTrue(
                tlaText.contains("Terminates == GF("),
                "expected Terminates == GF(...);\n$tlaText",
            )
            assertTrue(
                cfgText.contains("PROPERTY") && cfgText.contains("Terminates"),
                "expected PROPERTY …Terminates in cfg;\n$cfgText",
            )
            assertTrue(tlaText.contains("initially_call"), "expected initially_call;\n$tlaText")
            assertTrue(tlaText.contains("initially_ret"), "expected initially_ret;\n$tlaText")
            assertTrue(tlaText.contains("Main_blocking"), "expected Main_blocking;\n$tlaText")
            assertTrue(tlaText.contains("returnTo_initially"), "expected returnTo_initially;\n$tlaText")
            assertTrue(tlaText.contains("call_countUp"), "expected call_countUp flag;\n$tlaText")
            assertFalse(tlaText.contains("__invoke"), "should not emit __invoke;\n$tlaText")
            assertFalse(tlaText.contains("initially_invoke"), "legacy initially_invoke should be gone;\n$tlaText")
            assertTrue(
                tlaText.contains("out' =") &&
                    (tlaText.contains("retVal") || tlaText.contains("result")),
                "expected out coupled to procfun retVal;\n$tlaText",
            )
            assertTrue(
                tlaText.contains("calls the procfun countUp before executing"),
                "expected initially_call comment;\n$tlaText",
            )
            assertTrue(
                tlaText.contains("The guards for initially appear in initially_call"),
                "expected initially_ret comment;\n$tlaText",
            )
            tla.copyTo(File(work, "CountUpSpec.tla"), overwrite = true)
            cfg.copyTo(File(work, "CountUpSpec.cfg"), overwrite = true)
            tla.delete()
            cfg.delete()
            assertTlcHealthyStart(work, "CountUpSpec")
        } finally {
            work.deleteRecursively()
            File("CountUpSpec.tla").delete()
            File("CountUpSpec.cfg").delete()
        }
    }

    @Test
    fun procfunIndexedInheritsParentIndexAndTlcParses() {
        assumeTlcPresent()
        val work = Files.createTempDirectory("julay-spec-procfun-idx").toFile()
        try {
            val source = File("regression/input/spec/procfun-count-up-indexed.jul")
            assertTrue(source.exists(), "missing ${source.path}")
            compileJulFile(source.toPath(), keepBuild = false)
            val tla = File("CountUpIndexed.tla")
            val cfg = File("CountUpIndexed.cfg")
            assertTrue(tla.exists(), "expected CountUpIndexed.tla")
            assertTrue(cfg.exists(), "expected CountUpIndexed.cfg")
            val tlaText = tla.readText()
            assertTrue(
                tlaText.contains("Terminates ==") && tlaText.contains("GF("),
                "expected indexed Terminates property;\n$tlaText",
            )
            assertTrue(
                tlaText.contains("countUp_terminated") &&
                    tlaText.contains("|-> FALSE"),
                "expected terminated indexed Init;\n$tlaText",
            )
            tla.copyTo(File(work, "CountUpIndexed.tla"), overwrite = true)
            cfg.copyTo(File(work, "CountUpIndexed.cfg"), overwrite = true)
            tla.delete()
            cfg.delete()
            assertTlcHealthyStart(work, "CountUpIndexed")
        } finally {
            work.deleteRecursively()
            File("CountUpIndexed.tla").delete()
            File("CountUpIndexed.cfg").delete()
        }
    }

    @Test
    fun leafPlainEnvCompilesAndTlcStarts() {
        assumeTlcPresent()
        val work = Files.createTempDirectory("julay-leaf-plain").toFile()
        try {
            val source = File("regression/input/spec/leaf-plain-env.jul")
            assertTrue(source.exists(), "missing ${source.path}")
            compileJulFile(source.toPath(), keepBuild = false)
            val tla = File("LeafPlainEnv.tla")
            val cfg = File("LeafPlainEnv.cfg")
            assertTrue(tla.exists(), "expected LeafPlainEnv.tla")
            assertTrue(cfg.exists(), "expected LeafPlainEnv.cfg")
            val tlaText = tla.readText()
            assertTrue(
                tlaText.contains("VARIABLE") && tlaText.contains("ready"),
                "expected Env state in TLA;\n$tlaText",
            )
            tla.copyTo(File(work, "LeafPlainEnv.tla"), overwrite = true)
            cfg.copyTo(File(work, "LeafPlainEnv.cfg"), overwrite = true)
            tla.delete()
            cfg.delete()
            assertTlcHealthyStart(work, "LeafPlainEnv")
        } finally {
            work.deleteRecursively()
            File("LeafPlainEnv.tla").delete()
            File("LeafPlainEnv.cfg").delete()
        }
    }

    @Test
    fun listHofAndBoundedSeqEmitCorrectly() {
        assumeTlcPresent()
        val work = Files.createTempDirectory("julay-list-hof").toFile()
        try {
            val source = File("regression/input/spec/list-hof-tla.jul")
            assertTrue(source.exists(), "missing ${source.path}")
            compileJulFile(source.toPath(), keepBuild = false)
            val tla = File("ListHof.tla")
            val cfg = File("ListHof.cfg")
            assertTrue(tla.exists(), "expected ListHof.tla")
            assertTrue(cfg.exists(), "expected ListHof.cfg")
            val tlaText = tla.readText()
            val cfgText = cfg.readText()
            assertTrue(
                tlaText.contains("<<>>") &&
                    tlaText.contains("[__i \\in DOMAIN") &&
                    tlaText.contains("Len(") &&
                    !tlaText.contains("nextIndex' = [nextIndex EXCEPT ![i] = TRUE]"),
                "expected list literal, map comprehension, Len — not TRUE for nextIndex;\n$tlaText",
            )
            assertTrue(
                tlaText.contains("\\* splice") &&
                    tlaText.contains("splice(xs, s, e) ==") &&
                    tlaText.contains("splice(") &&
                    tlaText.contains("SubSeq(") &&
                    !tlaText.contains("DOMAIN TRUE") &&
                    !Regex("""\be\.value\b""").containsMatchIn(tlaText),
                "expected splice operator for slices (SubSeq inside splice) and substituted map binders;\n$tlaText",
            )
            assertTrue(
                tlaText.contains("((target.id) + 1)") || tlaText.contains("(target.id) + 1"),
                "expected list index + 1;\n$tlaText",
            )
            assertTrue(
                tlaText.contains("BoundedSeq(") && tlaText.contains("MaxListLen"),
                "expected BoundedSeq / MaxListLen;\n$tlaText",
            )
            assertTrue(
                !Regex("""(?<!Bounded)(?<!Sub)Seq\(""").containsMatchIn(tlaText),
                "must not use bare Seq(...) as a domain;\n$tlaText",
            )
            assertTrue(
                cfgText.contains("MaxListLen"),
                "expected MaxListLen in cfg;\n$cfgText",
            )
            tla.copyTo(File(work, "ListHof.tla"), overwrite = true)
            cfg.copyTo(File(work, "ListHof.cfg"), overwrite = true)
            tla.delete()
            cfg.delete()
            assertTlcHealthyStart(work, "ListHof")
        } finally {
            work.deleteRecursively()
            File("ListHof.tla").delete()
            File("ListHof.cfg").delete()
        }
    }

    @Test
    fun leafParamNetCompilesAndTlcStarts() {
        assumeTlcPresent()
        val work = Files.createTempDirectory("julay-leaf-param").toFile()
        try {
            val source = File("regression/input/spec/leaf-param-net.jul")
            assertTrue(source.exists(), "missing ${source.path}")
            compileJulFile(source.toPath(), keepBuild = false)
            val tla = File("LeafParamNet.tla")
            val cfg = File("LeafParamNet.cfg")
            assertTrue(tla.exists(), "expected LeafParamNet.tla")
            assertTrue(cfg.exists(), "expected LeafParamNet.cfg")
            val tlaText = tla.readText()
            assertTrue(
                tlaText.contains("CONSTANT Node") || tlaText.contains("CONSTANT Node,"),
                "expected Node sort as CONSTANT;\n$tlaText",
            )
            assertTrue(
                tlaText.contains("\\E n \\in Node") || tlaText.contains("[n \\in Node"),
                "expected binder n over Node;\n$tlaText",
            )
            // Decl param n is an aux action arg; state stays scalar (no EXCEPT ![n]).
            assertTrue(
                tlaText.contains("lastDest") &&
                    tlaText.contains("lastDest' = n") &&
                    !tlaText.contains("EXCEPT ![n]"),
                "expected scalar lastDest' = n;\n$tlaText",
            )
            tla.copyTo(File(work, "LeafParamNet.tla"), overwrite = true)
            cfg.copyTo(File(work, "LeafParamNet.cfg"), overwrite = true)
            tla.delete()
            cfg.delete()
            assertTlcHealthyStart(work, "LeafParamNet")
        } finally {
            work.deleteRecursively()
            File("LeafParamNet.tla").delete()
            File("LeafParamNet.cfg").delete()
        }
    }

    @Test
    fun objSortFieldCompilesAndTlcStarts() {
        assumeTlcPresent()
        val work = Files.createTempDirectory("julay-obj-sort-field").toFile()
        try {
            val source = File("regression/input/spec/obj-sort-field.jul")
            assertTrue(source.exists(), "missing ${source.path}")
            compileJulFile(source.toPath(), keepBuild = false)
            val tla = File("Env.tla")
            val cfg = File("Env.cfg")
            assertTrue(tla.exists(), "expected Env.tla")
            assertTrue(cfg.exists(), "expected Env.cfg")
            val cfgText = cfg.readText()
            assertTrue(cfgText.contains("NodeSet"), "expected NodeSet in cfg;\n$cfgText")
            tla.copyTo(File(work, "Env.tla"), overwrite = true)
            cfg.copyTo(File(work, "Env.cfg"), overwrite = true)
            tla.delete()
            cfg.delete()
            assertTlcHealthyStart(work, "Env")
        } finally {
            work.deleteRecursively()
            File("Env.tla").delete()
            File("Env.cfg").delete()
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

    /**
     * Runs TLC to completion and expects an invariant violation for [invariantName].
     */
    private fun assertTlcInvariantViolation(workDir: File, module: String, invariantName: String) {
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
        val finished = proc.waitFor(TLC_VIOLATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        if (!finished) {
            proc.destroyForcibly()
            reader.join(2000)
            fail("TLC timed out waiting for invariant violation of $invariantName.\n${synchronized(output) { output.toString() }}")
        }
        reader.join(2000)
        val text = synchronized(output) { output.toString() }
        val exit = proc.exitValue()
        assertNotEquals(0, exit, "expected TLC non-zero exit on invariant violation.\n$text")
        assertTrue(
            text.contains("Invariant $invariantName is violated", ignoreCase = false) ||
                (text.contains("Invariant", ignoreCase = true) &&
                    text.contains(invariantName) &&
                    text.contains("violated", ignoreCase = true)),
            "expected TLC to report violation of $invariantName.\n$text",
        )
    }
}
