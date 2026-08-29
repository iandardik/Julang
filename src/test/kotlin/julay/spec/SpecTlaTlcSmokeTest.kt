package julay.spec

import julay.compiler.ast.SpecNode
import julay.compiler.compileJulFile
import julay.compiler.prepareCheckedCompilation
import julay.compiler.pass.TlaOptConfig
import julay.compiler.pass.tlaCodegenPass
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

/** Text after a line-leading `Next ==` (avoids false hits inside LET names like `newNext ==`). */
private fun tlaAfterNext(tlaText: String): String {
    val m = Regex("(?m)^Next ==").find(tlaText)
        ?: error("missing line-leading Next == in TLA:\n${tlaText.take(500)}")
    return tlaText.substring(m.range.last + 1)
}

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
                tlaText.contains("\\* State variables for Counter with initially constructor logic"),
                "expected Init comment for Counter state with folded initially;\n$tlaText",
            )
            assertTrue(
                !tlaText.contains("Counter_constructed") && tlaText.contains("/\\ n = [i \\in String |->"),
                "expected bare state var n without Leaf_constructed (ctor folded into Init);\n$tlaText",
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
            val cfgText = cfg.readText()
            assertTrue(
                cfgText.contains("CONSTANT String = {\"\"}"),
                "String model should be used literals only (none → empty string);\n$cfgText",
            )
            assertTrue(
                !cfgText.contains("\"a\"") && !cfgText.contains("\"1\""),
                "String model should not inject a, b, or int-as-string;\n$cfgText",
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
                tlaText.contains("spawnWorker(i, id_) =="),
                "expected spawnWorker(i, id_) operator with index first;\n$tlaText",
            )
            assertTrue(
                tlaText.contains("~Worker_constructed[i]"),
                "expected parameterized ctor enabling on Worker for spawnWorker;\n$tlaText",
            )
            val spawnDef = tlaText.substringAfter("spawnWorker(i, id_) ==").substringBefore("\n\n")
            assertTrue(
                !spawnDef.trimStart().startsWith("\\E i \\in Int :"),
                "index quantification should be in Next, not inside spawnWorker;\n$tlaText",
            )
            assertTrue(
                tlaText.contains("Worker_constructed' = [Worker_constructed EXCEPT ![i] = TRUE]"),
                "expected Worker_constructed' flip in spawnWorker;\n$tlaText",
            )
            assertTrue(
                tlaText.contains("\\E i, id_ \\in Int : spawnWorker(i, id_)"),
                "expected Next to quantify index then args;\n$tlaText",
            )
            assertFalse(
                tlaText.contains("work(i) ==") || Regex("""\\/\s+.*\bwork\b""").containsMatchIn(tlaText),
                "guard-only work should be omitted from defs/Next;\n$tlaText",
            )
            assertTrue(
                tlaText.contains("\\* State variables for Server with initially constructor logic") &&
                    tlaText.contains("\\* State variables for Worker"),
                "expected Init state-var comments; Server initially is folded, Worker keeps Next ctors;\n$tlaText",
            )
            assertTrue(
                tlaText.contains("/\\ id = [i \\in Int |-> 0]") &&
                    tlaText.contains("/\\ ready = FALSE") &&
                    !tlaText.contains("Worker_id"),
                "id is a unique state var so stays bare (indexed Init); spawn arg is id_; ready is bare;\n$tlaText",
            )
            assertTrue(
                !tlaText.contains("Server_constructed") &&
                    !tlaText.contains("Server_initially =="),
                "Server's sole unsynced initially is folded into Init;\n$tlaText",
            )
            assertTrue(
                tlaText.contains("\\* initially constructor on Worker") &&
                    tlaText.contains("Worker_initially(i) =="),
                "expected disambiguation comment for Worker initially;\n$tlaText",
            )
            assertTrue(
                spawnDef.contains("\\* Server transition logic") &&
                    spawnDef.contains("\\* Worker constructor logic") &&
                    spawnDef.indexOf("\\* Server transition logic") <
                    spawnDef.indexOf("\\* Worker constructor logic"),
                "expected per-proc transition-type logic comments in spawnWorker;\n$spawnDef",
            )
            val afterInit = tlaText.substringAfter("Init ==")
            val workerInitIdx = afterInit.indexOf("Worker_initially(i) ==")
            val spawnIdx = afterInit.indexOf("spawnWorker(i, id_) ==")
            assertTrue(
                workerInitIdx >= 0 && spawnIdx >= 0 && workerInitIdx < spawnIdx,
                "Worker initially def should appear after Init and before spawnWorker;\n$tlaText",
            )
            val nextBody = tlaAfterNext(tlaText).substringBefore("\n\n")
            val nextWorker = nextBody.indexOf("Worker_initially")
            val nextSpawn = nextBody.indexOf("spawnWorker")
            assertTrue(
                nextWorker >= 0 && nextSpawn >= 0 && nextWorker < nextSpawn,
                "Worker initially should lead Next before spawnWorker;\n$nextBody",
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
                !tlaText.contains("Counter_constructed") &&
                    tlaText.contains("\\* State variables for Counter with initially constructor logic") &&
                    tlaText.contains("/\\ n = 0"),
                "expected Counter initially folded into Init with bare n;\n$tlaText",
            )
            assertTrue(
                !tlaText.contains("Env_constructed") &&
                    tlaText.contains("\\* State variables for Env with initially constructor logic") &&
                    tlaText.contains("/\\ ready = FALSE"),
                "expected Env initially folded into Init with bare ready;\n$tlaText",
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
                !tlaText.contains("Counter_constructed") &&
                    tlaText.contains("\\* State variables for Counter with initially constructor logic") &&
                    !tlaText.contains("Counter_n"),
                "expected Counter initially folded into Init with bare n;\n$tlaText",
            )
            assertTrue(
                !tlaText.contains("Handler_constructed") &&
                    tlaText.contains("\\* State variables for Handler with initially constructor logic") &&
                    !tlaText.contains("Handler_step") &&
                    tlaText.contains("step"),
                "expected Handler initially folded into Init with bare step;\n$tlaText",
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
                tlaText.contains("peerArg") && !tlaText.contains("peerArg.id"),
                "unwrap-singletons: Peer arg should emit as Int without .id;\n$tlaText",
            )
            assertTrue(
                !tlaText.contains("peerArg_id"),
                "action-arg field must not use underscore peerArg_id;\n$tlaText",
            )
            assertTrue(
                tlaText.contains("peer[i]") && !tlaText.contains("peer[i].id"),
                "unwrap-singletons: parameterized Peer state should emit without .id;\n$tlaText",
            )
            assertTrue(
                !tlaText.contains("peer_id"),
                "state-var field must not use underscore peer_id;\n$tlaText",
            )
            assertTrue(
                !tlaText.contains("url"),
                "unread Peer.url should be omitted from TLA;\n$tlaText",
            )
            assertTrue(
                !tlaText.contains("[id: Int]") && !tlaText.contains("[id |->"),
                "unwrapped Peer should be Int, not a one-field record;\n$tlaText",
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
    fun objFieldAccessOmitsUrlWithoutComparisonWarning() {
        val source = File("regression/input/spec/obj-field-access.jul")
        val (tlaText, warnings) = compileSpecTla(source, "ObjFieldAccess")
        assertTrue(!tlaText.contains("url"), "unread url should be omitted;\n$tlaText")
        assertTrue(
            warnings.none { it.contains("unused-fields") },
            "no whole-record comparison, so no unused-fields warning;\n$warnings",
        )
    }

    @Test
    fun unusedFieldsDisableKeepsUrl() {
        val source = File("regression/input/spec/obj-field-access.jul")
        val configs = listOf(
            TlaOptConfig.fromDisableTlaOptFlag("unused-fields"),
            TlaOptConfig.fromDisableTlaOptFlag("ALL"),
        )
        for (cfg in configs) {
            try {
                compileJulFile(
                    source.toPath(),
                    keepBuild = false,
                    tlaOptConfig = cfg,
                )
                val tlaText = File("ObjFieldAccess.tla").readText()
                assertTrue(
                    tlaText.contains("url"),
                    "url should remain when unused-fields is disabled ($cfg);\n$tlaText",
                )
            } finally {
                File("ObjFieldAccess.tla").delete()
                File("ObjFieldAccess.cfg").delete()
            }
        }
    }

    @Test
    fun unusedFieldsKeptWhenRead() {
        val source = File("regression/input/spec/obj-field-keep-url.jul")
        val (tlaText, warnings) = compileSpecTla(source, "ObjFieldKeepUrl")
        assertTrue(
            warnings.none { it.contains("unused-fields") },
            "no unused-fields warning when url is read;\n$warnings",
        )
        assertTrue(
            tlaText.contains("peerArg") && !tlaText.contains("peerArg.id"),
            "url-only Peer unwraps to String; id must stay omitted;\n$tlaText",
        )
        val (unwrappedOff, _) = compileSpecTla(
            source,
            "ObjFieldKeepUrl",
            tlaOptConfig = TlaOptConfig.fromDisableTlaOptFlag("unwrap-singletons"),
        )
        assertTrue(
            unwrappedOff.contains("url"),
            "read url field must stay when unwrap is off;\n$unwrappedOff",
        )
        File("ObjFieldKeepUrl.tla").delete()
        File("ObjFieldKeepUrl.cfg").delete()
    }

    @Test
    fun unusedFieldsWarnsOnRecordEquality() {
        val source = File("regression/input/spec/obj-field-eq-unused.jul")
        val (tlaText, warnings) = compileSpecTla(source, "ObjFieldEq")
        assertTrue(!tlaText.contains("url"), "unread url should be omitted;\n$tlaText")
        val hit = warnings.firstOrNull { it.contains("unused-fields") && it.contains("\"url\"") }
        assertTrue(hit != null, "expected unused-fields warning for url;\n$warnings")
        assertTrue(
            hit!!.contains("diverge from the actual semantics"),
            "warning should mention semantic divergence;\n$hit",
        )
        assertTrue(
            hit.contains("--disable-tla-opt=unused-fields"),
            "warning should mention disable flag;\n$hit",
        )
        File("ObjFieldEq.tla").delete()
        File("ObjFieldEq.cfg").delete()
    }

    @Test
    fun inferredStateConstraintBindsIntVars() {
        val source = File("regression/input/spec/tla-state-constraint.jul")
        val emit = compileSpecEmit(source, "StateConstraintInc")
        val tlaText = emit.tlaText
        val cfgText = emit.cfgText
        assertTrue(
            cfgText.contains("CONSTRAINT StateConstraint") &&
                !cfgText.contains("INVARIANT StateConstraint"),
            "StateConstraint should be a TLC CONSTRAINT, not an INVARIANT;\n$cfgText",
        )
        val constraintDef = tlaText.substringAfter("StateConstraint ==").substringBefore("\n\n")
        assertTrue(
            constraintDef.contains("n \\in Int") &&
                !constraintDef.contains("TypeOKInt"),
            "Int state vars should inhabit cfg Int;\n$constraintDef",
        )
        assertTrue(
            cfgText.contains("CONSTANT Int"),
            "CONSTRAINT n \\in Int needs cfg Int;\n$cfgText",
        )
        File("StateConstraintInc.tla").delete()
        File("StateConstraintInc.cfg").delete()
    }

    @Test
    fun determinedArgsSubstitutesPayloadLet() {
        val source = File("regression/input/spec/tla-determined-args.jul")
        val (tlaText, _) = compileSpecTla(source, "DeterminedArgs")
        assertTrue(
            !tlaText.contains("\\E payload \\in"),
            "determined-args should omit payload exists;\n$tlaText",
        )
        assertTrue(
            tlaText.contains("LET payload ==") &&
                (tlaText.contains("[x |-> n") || tlaText.contains("[x |-> Box_n")),
            "expected LET payload == record;\n$tlaText",
        )
        val (offText, _) = compileSpecTla(
            source,
            "DeterminedArgs",
            tlaOptConfig = TlaOptConfig.fromDisableTlaOptFlag("determined-args"),
        )
        assertTrue(
            offText.contains("\\E payload \\in"),
            "disabling determined-args should restore payload exists;\n$offText",
        )
    }

    @Test
    fun determinedArgsWalksGuardLets() {
        val source = File("regression/input/spec/tla-determined-args-let.jul")
        val (tlaText, _) = compileSpecTla(source, "DeterminedArgsLet")
        assertTrue(
            !Regex("""\\E g \\in BOOLEAN""").containsMatchIn(tlaText),
            "determined-args should omit g exists through let;\n$tlaText",
        )
        assertTrue(
            !Regex("""\\E out \\in Int""").containsMatchIn(tlaText),
            "determined-args should omit out type-domain exists;\n$tlaText",
        )
        assertTrue(
            (tlaText.contains("g ==") && tlaText.contains("out ==")),
            "expected LET g and LET out;\n$tlaText",
        )
        assertTrue(
            !Regex("""LET g == c\b""").containsMatchIn(tlaText),
            "determiner should inline the let-bound name c;\n$tlaText",
        )
    }

    @Test
    fun determinedArgsBindsCaseBranches() {
        val source = File("regression/input/spec/tla-case-determined.jul")
        val (tlaText, _) = compileSpecTla(source, "CaseDetermined")
        assertTrue(
            !Regex("""\\E x \\in Int""").containsMatchIn(tlaText),
            "CASE determined-args should omit x exists;\n$tlaText",
        )
        assertTrue(
            tlaText.contains("LET x ==") &&
                tlaText.contains("IF") &&
                (tlaText.contains("THEN 0") || tlaText.contains("THEN  0")),
            "expected LET x == IF ... THEN 0;\n$tlaText",
        )
        val (offText, _) = compileSpecTla(
            source,
            "CaseDetermined",
            tlaOptConfig = TlaOptConfig.fromDisableTlaOptFlag("determined-args"),
        )
        assertTrue(
            Regex("""\\E x \\in Int""").containsMatchIn(offText),
            "disabling determined-args should restore x exists;\n$offText",
        )
    }

    @Test
    fun boolIteIfFalseElseTrueEmitsNegation() {
        val source = File("regression/input/spec/tla-bool-ite.jul")
        val (tlaText, _) = compileSpecTla(source, "BoolIte")
        val goDef = tlaText.substringAfter("go ==").substringBefore("\n\n")
        assertTrue(
            Regex("""ok ==\s*~""").containsMatchIn(goDef) &&
                !Regex("""ok ==\s*IF""").containsMatchIn(goDef) &&
                !goDef.contains("THEN FALSE") &&
                !goDef.contains("ELSE TRUE"),
            "IF P THEN FALSE ELSE TRUE should emit ~P;\n$goDef",
        )
        val okInit = goDef.lineSequence()
            .dropWhile { !it.contains("ok ==") }
            .takeWhile { !it.trimStart().startsWith("IN") }
            .toList()
        val andCols = okInit.mapNotNull { line ->
            val i = line.indexOf("/\\")
            if (i >= 0) i else null
        }
        assertTrue(
            andCols.size >= 2 && andCols[0] == andCols[1],
            "multi-line ~P should align /\\ under ~(\n${okInit.joinToString("\n")}",
        )
    }

    @Test
    fun fromCollectionBindsListIndex() {
        val source = File("regression/input/spec/tla-from-collection.jul")
        val (tlaText, _) = compileSpecTla(source, "FromCollection")
        assertTrue(
            !tlaText.contains("\\E target \\in [id: Int]"),
            "from-collection should not quantify target over [id: Int];\n$tlaText",
        )
        assertTrue(
            tlaText.contains("target_idx") && tlaText.contains("LET target =="),
            "expected index binder and LET target;\n$tlaText",
        )
        val (offText, _) = compileSpecTla(
            source,
            "FromCollection",
            tlaOptConfig = TlaOptConfig.fromDisableTlaOptFlag("from-collection"),
        )
        assertTrue(
            offText.contains("\\E target \\in") && !offText.contains("target_idx"),
            "disabling from-collection should restore type-domain exists for target;\n$offText",
        )
    }

    @Test
    fun fromCollectionBindsStructInSet() {
        val source = File("regression/input/spec/tla-from-collection-struct.jul")
        val (tlaText, _) = compileSpecTla(source, "FromCollectionStruct")
        assertTrue(
            tlaText.contains("\\E msg \\in") && tlaText.contains("LET x =="),
            "expected struct-in-set collection binder and LET x;\n$tlaText",
        )
        assertTrue(
            !tlaText.contains("__s0"),
            "struct-in-set binder should not be __s0;\n$tlaText",
        )
        assertTrue(
            !Regex("""\\E x \\in Int""").containsMatchIn(tlaText),
            "x should be bound from the set element, not Int;\n$tlaText",
        )
        val (offText, _) = compileSpecTla(
            source,
            "FromCollectionStruct",
            tlaOptConfig = TlaOptConfig.fromDisableTlaOptFlag("from-collection"),
        )
        assertTrue(
            Regex("""\\E x \\in Int""").containsMatchIn(offText) && !offText.contains("__s0"),
            "disabling from-collection should restore \\E x \\in Int;\n$offText",
        )
    }

    @Test
    fun fromCollectionBindsAlsoAndIndex() {
        val source = File("regression/input/spec/tla-from-collection-also.jul")
        val (tlaText, _) = compileSpecTla(source, "AlsoStructChecked")
        val recvDef = tlaText.substringAfter("recv(").substringBefore("\n\n")
        assertTrue(
            (recvDef.contains("LET n ==") || recvDef.contains("n ==")) &&
                (recvDef.contains("LET m ==") || recvDef.contains("m ==")),
            "also-arg m and index n should be LET-bound from the struct;\n$recvDef",
        )
        assertTrue(
            !Regex("""\\E n, m \\in NodeSet""").containsMatchIn(tlaText) &&
                !Regex("""\\E n \\in NodeSet""").containsMatchIn(recvDef) &&
                !Regex("""\\E m \\in NodeSet""").containsMatchIn(recvDef),
            "n and m should not be type-domain exists on recv;\n$tlaText",
        )
        assertTrue(
            Regex("""\\E \w+ \\in """).containsMatchIn(recvDef) ||
                Regex("""\\E \w+ \\in """).containsMatchIn(tlaAfterNext(tlaText)),
            "collection binder exists should remain;\n$tlaText",
        )
    }

    @Test
    fun fromCollectionProjectsFilterField() {
        val source = File("regression/input/spec/tla-from-collection-project.jul")
        val (tlaText, _) = compileSpecTla(source, "FromCollectionProject")
        assertTrue(
            !Regex("""\\E t \\in Int""").containsMatchIn(tlaText),
            "from-collection projection should not quantify t over Int;\n$tlaText",
        )
        assertTrue(
            Regex("""\\E t \\in msgs""").containsMatchIn(tlaText) ||
                Regex("""\\E t \\in \{[^}]*term""").containsMatchIn(tlaText) ||
                tlaText.contains("{ m.term : m \\in") ||
                tlaText.contains("{m.term : m \\in"),
            "t should be quantified from the filter projection (unwrapped set or m.term);\n$tlaText",
        )
        val (offText, _) = compileSpecTla(
            source,
            "FromCollectionProject",
            tlaOptConfig = TlaOptConfig.fromDisableTlaOptFlag("from-collection"),
        )
        assertTrue(
            Regex("""\\E t \\in Int""").containsMatchIn(offText),
            "disabling from-collection should restore \\E t \\in Int;\n$offText",
        )
    }

    @Test
    fun literalDomainsShrinkArgNotGlobalString() {
        val source = File("regression/input/spec/tla-literal-domains.jul")
        val emit = compileSpecEmit(source, "LiteralDomains")
        val tlaText = emit.tlaText
        assertTrue(
            tlaText.contains("\\E mode \\in {\"a\", \"b\"}") ||
                tlaText.contains("\\E mode \\in {\"b\", \"a\"}"),
            "mode should use a finite literal set;\n$tlaText",
        )
        assertTrue(
            tlaText.contains("String"),
            "open payload String site should keep the global String model;\n$tlaText",
        )
        val typeOk = tlaText.substringAfter("TypeOK ==").substringBefore("\n\n")
        assertTrue(
            typeOk.contains("role \\in {\"idle\", \"run\"}") ||
                typeOk.contains("role \\in {\"run\", \"idle\"}"),
            "TypeOK should use the closed role-string set;\n$typeOk",
        )
        val stringConst = emit.cfgText.lineSequence().firstOrNull { it.startsWith("CONSTANT String") }
        assertTrue(
            stringConst != null,
            "open note site should keep CONSTANT String in cfg;\n${emit.cfgText}",
        )
        assertTrue(
            stringConst!!.contains("\"seed\"") &&
                stringConst.contains("\"a\"") &&
                stringConst.contains("\"b\"") &&
                stringConst.contains("\"\""),
            "String model should be used literals, including seed/a/b/empty;\n$stringConst",
        )
        assertTrue(
            !stringConst.contains("\"0\"") && !stringConst.contains("\"1\""),
            "String model should not inject int-as-string;\n$stringConst",
        )
        val offText = compileSpecTla(
            source,
            "LiteralDomains",
            tlaOptConfig = TlaOptConfig.fromDisableTlaOptFlag("literal-domains"),
        ).first
        assertTrue(
            offText.contains("\\E mode, note \\in String") ||
                offText.contains("\\E mode \\in String"),
            "disabling literal-domains should restore String for mode;\n$offText",
        )
    }

    @Test
    fun singletonInitEmitsSequenceLiteral() {
        val source = File("regression/input/spec/tla-singleton-init.jul")
        val (tlaText, _) = compileSpecTla(source, "SingletonInit")
        val initBlock = tlaText.substringAfter("Init ==").substringBefore("\n\n")
        assertTrue(
            initBlock.contains("xs = <<1, 2>>") || initBlock.contains("Box_xs = <<1, 2>>"),
            "singleton init should assign the identity sequence;\n$initBlock",
        )
        assertTrue(
            !initBlock.contains("xs \\in BoundedSeq") && !initBlock.contains("Box_xs \\in BoundedSeq"),
            "singleton init should drop BoundedSeq membership;\n$initBlock",
        )
    }

    @Test
    fun singletonSetInitEmitsSetLiteral() {
        val source = File("regression/input/spec/tla-singleton-set-init.jul")
        val (tlaText, _) = compileSpecTla(source, "SingletonSetInit")
        val initBlock = tlaText.substringAfter("Init ==").substringBefore("\n\n")
        assertTrue(
            initBlock.contains("xs = {1, 2}") || initBlock.contains("Box_xs = {1, 2}"),
            "singleton set init should assign {1, 2};\n$initBlock",
        )
        assertTrue(
            !initBlock.contains("xs \\in SUBSET") && !initBlock.contains("Box_xs \\in SUBSET"),
            "singleton set init should drop SUBSET membership;\n$initBlock",
        )
        assertTrue(
            !Regex("""\\A i \\in Int :""").containsMatchIn(initBlock),
            "singleton set init should drop covering filters;\n$initBlock",
        )
    }

    @Test
    fun associateWithEmitsFunctionComprehension() {
        val source = File("regression/input/spec/set-associate-with-tla.jul")
        val (tlaText, _) = compileSpecTla(source, "AssocMap")
        assertTrue(
            Regex("""\[__k \\in [^\]]*cluster[^\]]* \|-> 1]""").containsMatchIn(tlaText) ||
                tlaText.contains("[__k \\in cluster |-> 1]") ||
                tlaText.contains("[__k \\in Box_cluster |-> 1]"),
            "associateWith should emit [__k \\in cluster |-> 1];\n$tlaText",
        )
        val typeOkBlock = tlaText.substringAfter("TypeOK ==").substringBefore("\n\n")
        assertTrue(
            typeOkBlock.contains("DOMAIN nextIndex[i] = cluster") ||
                typeOkBlock.contains("DOMAIN nextIndex[i] = Box_cluster") ||
                Regex("""DOMAIN \S*nextIndex\[[^\]]+\] = \S*cluster""").containsMatchIn(typeOkBlock),
            "TypeOK should pin DOMAIN nextIndex to cluster;\n$typeOkBlock",
        )
    }

    @Test
    fun optionalRecordEmitsPresentValue() {
        val source = File("regression/input/spec/tla-optional.jul")
        val (tlaText, _) = compileSpecTla(source, "OptionalSpec")
        assertTrue(
            tlaText.contains("present:") &&
                tlaText.contains("value:") &&
                (tlaText.contains("present |-> FALSE") || tlaText.contains("present |-> false") ||
                    tlaText.contains("[present |-> FALSE, value |-> 0]") ||
                    tlaText.contains("present |-> FALSE")),
            "Optional Init should be a present/value record;\n${tlaText.substringAfter("Init ==").take(2000)}",
        )
        val typeOkBlock = tlaText.substringAfter("TypeOK ==").substringBefore("\n\n")
        assertTrue(
            typeOkBlock.contains("present: BOOLEAN") && typeOkBlock.contains("value:"),
            "TypeOK should type Optional as a present/value record;\n$typeOkBlock",
        )
        assertTrue(
            !tlaText.contains("votedFor = -1") && !Regex("""opt = -1""").containsMatchIn(tlaText),
            "Optional should not use Int -1 sentinels;\n$tlaText",
        )
    }

    @Test
    fun cfgSkipsConjunctiveInvariant() {
        val source = File("regression/input/spec/tla-cfg-skip-conjunction.jul")
        val emit = compileSpecEmit(source, "ConjInv")
        assertTrue(
            emit.tlaText.contains("C == A /\\ B") || emit.tlaText.contains("C == A /\\ B\n"),
            "TLA should keep the conjunctive operator;\n${emit.tlaText.substringAfter("user-specified invariants")}",
        )
        assertTrue(
            emit.cfgText.contains("INVARIANT A") && emit.cfgText.contains("INVARIANT B"),
            "cfg should list the leaf invariants;\n${emit.cfgText}",
        )
        assertTrue(
            !emit.cfgText.contains("INVARIANT C"),
            "cfg should omit the conjunctive name;\n${emit.cfgText}",
        )
    }

    @Test
    fun unwrapSingletonsDropsIdOnHofBinders() {
        val source = File("regression/input/spec/tla-unwrap-hof-field.jul")
        val (tlaText, _) = compileSpecTla(source, "UnwrapHofField")
        val initBlock = tlaText.substringAfter("Init ==").substringBefore("\n\n")
        assertTrue(
            initBlock.contains("cluster = {1, 2, 3}") || initBlock.contains("Box_cluster = {1, 2, 3}"),
            "unwrapped Node set init should be {1, 2, 3};\n$initBlock",
        )
        assertTrue(
            !Regex("""\w+\.id""").containsMatchIn(tlaText),
            "HOF/fun binders should unwrap Node.id;\n$tlaText",
        )
        assertTrue(
            Regex("""\{ x \\in \S*cluster : \(x = id\) \}""").containsMatchIn(tlaText) ||
                tlaText.contains("{ x \\in cluster : (x = id) }") ||
                tlaText.contains("{ x \\in Box_cluster : (x = id) }"),
            "lookup filter should compare the unwrapped binder, not x.id;\n$tlaText",
        )
    }

    @Test
    fun unwrapSingletonsEmitsFieldType() {
        val source = File("regression/input/spec/obj-field-access.jul")
        val (tlaText, _) = compileSpecTla(source, "ObjFieldAccess")
        assertTrue(
            !tlaText.contains("[id: Int]") && !tlaText.contains("peerArg.id") && !tlaText.contains("peer[i].id"),
            "unwrap-singletons should drop .id and [id: Int];\n$tlaText",
        )
        val (offText, _) = compileSpecTla(
            source,
            "ObjFieldAccess",
            tlaOptConfig = TlaOptConfig.fromDisableTlaOptFlag("unwrap-singletons"),
        )
        assertTrue(
            (offText.contains("[id: Int]") || offText.contains("[id |->")) &&
                (offText.contains("peerArg.id") || offText.contains("peer[i].id")),
            "disabling unwrap-singletons should keep [id: Int] / .id;\n$offText",
        )
        assertTrue(
            !offText.contains("url"),
            "unused-fields should still omit url when only unwrap is disabled;\n$offText",
        )
    }

    @Test
    fun unusedVarsOmitsWriteOnlyGhost() {
        val source = File("regression/input/spec/tla-unused-vars-write.jul")
        val (tlaText, _) = compileSpecTla(source, "UnusedVarsWrite")
        assertTrue(
            !tlaText.contains("ghost"),
            "write-only ghost should be omitted;\n$tlaText",
        )
        val (offText, _) = compileSpecTla(
            source,
            "UnusedVarsWrite",
            tlaOptConfig = TlaOptConfig.fromDisableTlaOptFlag("unused-vars"),
        )
        assertTrue(
            offText.contains("ghost"),
            "disabling unused-vars should keep ghost;\n$offText",
        )
    }

    @Test
    fun unusedVarsOmitsGuardOnlyReads() {
        val source = File("regression/input/spec/tla-unused-vars-guard-only.jul")
        val (tlaText, _) = compileSpecTla(source, "UnusedVarsGuardOnly")
        assertTrue(
            !tlaText.contains("secret"),
            "var read only in a guard-only action should be omitted;\n$tlaText",
        )
        val (offText, _) = compileSpecTla(
            source,
            "UnusedVarsGuardOnly",
            tlaOptConfig = TlaOptConfig.fromDisableTlaOptFlag("unused-vars"),
        )
        assertTrue(
            offText.contains("secret"),
            "disabling unused-vars should keep secret;\n$offText",
        )
    }

    @Test
    fun unusedVarsKeepsInvariantReads() {
        val source = File("regression/input/spec/tla-unused-vars-inv.jul")
        val (tlaText, _) = compileSpecTla(source, "UnusedVarsInv")
        assertTrue(
            tlaText.contains("watched"),
            "var read in the guarantee should stay;\n$tlaText",
        )
    }

    @Test
    fun unusedLetsDropsUnreferencedBindings() {
        val source = File("regression/input/spec/tla-unused-lets.jul")
        val (tlaText, _) = compileSpecTla(source, "UnusedLets")
        assertTrue(
            !tlaText.contains("a ==") && !tlaText.contains("b =="),
            "unused-lets should drop both unused let names;\n$tlaText",
        )
        val (offText, _) = compileSpecTla(
            source,
            "UnusedLets",
            tlaOptConfig = TlaOptConfig.fromDisableTlaOptFlag("unused-lets"),
        )
        assertTrue(
            offText.contains("a ==") && offText.contains("b =="),
            "disabling unused-lets should keep a and b;\n$offText",
        )
    }

    @Test
    fun raftNodeSpecOmitsUrlAndWarns() {
        val source = File("input/raft/sys.jul")
        assertTrue(source.exists(), "missing ${source.path}")
        val emit = compileSpecEmit(source, "RaftNodeSpec", compileNames = listOf("RaftNodeSpec"))
        val tlaText = emit.tlaText
        val warnings = emit.warnings
        assertTrue(
            !tlaText.contains("url"),
            "Node.url should be omitted from RaftNodeSpec TLA;\n${tlaText.take(2000)}",
        )
        val hit = warnings.firstOrNull { it.contains("unused-fields") && it.contains("\"url\"") }
        assertTrue(hit != null, "expected unused-fields warning for Node.url;\n$warnings")
        assertTrue(
            !tlaText.contains("\\E payload \\in [term:"),
            "determined-args should drop payload type-domain exists;\n${tlaText.take(4000)}",
        )
        assertTrue(
            !tlaText.contains("\\E target \\in [id: Int]"),
            "target should not be quantified over [id: Int];\n${tlaText.take(4000)}",
        )
        assertTrue(
            !Regex("""RaftProtocol_state\s*=\s*\[.*\|->\s*""\]""").containsMatchIn(tlaText) &&
                (tlaText.contains("\"Follower\"") || tlaText.contains("\"Candidate\"") || tlaText.contains("\"Leader\"")),
            "state should use the closed role-string set, not the empty-string default;\n${tlaText.take(4000)}",
        )
        assertTrue(
            !tlaText.contains("n_RaftProtocol") &&
                !Regex("""requestVote\([^)]*n_Raft""").containsMatchIn(tlaText),
            "with (n) should share one binder for RaftProtocol and Net, not n_RaftProtocol;\n${tlaText.take(4000)}",
        )
        assertTrue(
            Regex("""self\[n1] = self\[n2]""").containsMatchIn(tlaText),
            "Init should require injective self;\n${tlaText.substringAfter("Init ==").take(2500)}",
        )
        assertTrue(
            !tlaText.contains("cluster \\in BoundedSeq") &&
                !tlaText.contains("RaftProtocol_cluster \\in BoundedSeq") &&
                (tlaText.contains("cluster = {1, 2, 3}") || tlaText.contains("RaftProtocol_cluster = {1, 2, 3}")),
            "length+covering init should emit the singleton cluster set;\n${tlaText.substringAfter("Init ==").take(2500)}",
        )
        assertTrue(
            !tlaText.contains("voteCondition") && !tlaText.contains("logOk"),
            "unused-lets should drop voteCondition and then logOk;\n${tlaText.substringAfter("handleRequestVoteRequest").take(1500)}",
        )
        assertTrue(
            tlaText.contains("\\E n, m \\in NodeSet :") &&
                Regex("""requestVote\(n, m""").containsMatchIn(tlaText) &&
                !tlaText.contains("\\E n \\in NodeSet : \\E m \\in NodeSet"),
            "consecutive \\E over NodeSet should combine;\n${tlaAfterNext(tlaText).take(2000)}",
        )
        assertTrue(
            !tlaText.contains("RaftProtocol_constructed") &&
                !tlaText.contains("Net_constructed") &&
                !tlaText.contains("startRaftCore(") &&
                !Regex("""(?m)^initially ==""").containsMatchIn(tlaText),
            "sole unsynced ctors should fold into Init; omit *_constructed and Next ctors;\n${tlaText.take(4000)}",
        )
        assertTrue(
            tlaText.contains("\\* State variables for RaftProtocol with startRaftCore constructor logic") &&
                tlaText.contains("\\* State variables for Net with initially constructor logic") &&
                tlaText.contains("\\* RaftProtocol constructor assumption") &&
                tlaText.contains("\\* init constraints"),
            "Init comments should name folded ctors, assumptions, then init constraints;\n${tlaText.substringAfter("Init ==").take(2500)}",
        )
        val initBlock = tlaText.substringAfter("Init ==").substringBefore("\n\n")
        assertTrue(
            !Regex("""\\A i \\in Int :""").containsMatchIn(initBlock),
            "singleton cluster init should drop covering-membership filters;\n$initBlock",
        )
        assertTrue(
            !Regex("""\w+\.id""").containsMatchIn(tlaText),
            "unwrap-singletons should drop .id on Node, including HOF binders;\n$tlaText",
        )
        assertTrue(
            !tlaText.contains(".keys") &&
                (
                    tlaText.contains("DOMAIN matchIndex[n]") ||
                        tlaText.contains("{ k \\in DOMAIN matchIndex")
                    ),
            "map .keys should emit DOMAIN, not record field keys;\n${tlaText.substringAfter("advanceCommitIndex(").take(800)}",
        )
        val updateTermDef = tlaText.substringAfter("updateTerm(").substringBefore("\n\n")
        assertTrue(
            !tlaText.contains("updateTerm_RaftProtocol_Net") &&
                Regex("""(?m)^updateTerm\(""").containsMatchIn(tlaText) &&
                (
                    updateTermDef.contains("voteRequestMsgs") &&
                        updateTermDef.contains("voteResponseMsgs") &&
                        updateTermDef.contains("appendEntriesRequestMsgs") &&
                        updateTermDef.contains("appendEntriesResponseMsgs")
                    ) &&
                (updateTermDef.contains("Cardinality") || updateTermDef.contains("filter")) &&
                (
                    updateTermDef.contains("inTerm > currentTerm") ||
                        updateTermDef.contains("inTerm > RaftProtocol_currentTerm")
                    ),
            "updateTerm should be one paired action whose guard mentions all four bags and inTerm > currentTerm;\n$updateTermDef",
        )
        assertTrue(
            !Regex("""\\E inTerm \\in Int""").containsMatchIn(tlaText),
            "exists-from-projection should drop \\E inTerm \\in Int;\n${tlaAfterNext(tlaText).take(2500)}",
        )
        assertTrue(
            !Regex("""\\E matchIdx \\in Int""").containsMatchIn(tlaText) &&
                !Regex("""\\E success \\in BOOLEAN""").containsMatchIn(tlaText),
            "CASE determined-args should drop matchIdx and success exists;\n${tlaText.substringAfter("handleAppendEntriesRequest").take(2500)}",
        )
        val handleAppendEntriesRequestDef = tlaText.substringAfter("handleAppendEntriesRequest(").substringBefore("\n\n")
        assertTrue(
            Regex("""success ==\s*~""").containsMatchIn(handleAppendEntriesRequestDef) &&
                !Regex("""success ==\s*IF""").containsMatchIn(handleAppendEntriesRequestDef) &&
                !handleAppendEntriesRequestDef.contains("THEN FALSE"),
            "success LET should be ~P, not IF P THEN FALSE ELSE TRUE;\n$handleAppendEntriesRequestDef",
        )
        val successLet = handleAppendEntriesRequestDef.lineSequence()
            .dropWhile { !it.contains("success ==") }
            .takeWhile { !it.contains("matchIdx") }
            .toList()
        val successOrCols = successLet.mapNotNull { line ->
            val i = line.indexOf("\\/")
            if (i >= 0) i else null
        }
        assertTrue(
            successOrCols.size >= 2 && successOrCols[0] == successOrCols[1],
            "success ~(\\/ …) should align top-level \\/;\n${successLet.joinToString("\n")}",
        )
        val handleRvDef = tlaText.substringAfter("handleRequestVoteRequest(").substringBefore("\n\n")
        assertTrue(
            handleRvDef.contains("voteRequestMsgs' =") &&
                (handleRvDef.contains("voteRequestMsgs \\ {") || handleRvDef.contains("voteRequestMsgs\\ {")),
            "handleRequestVoteRequest should subtract the matched request;\n$handleRvDef",
        )
        assertTrue(
            !Regex("""\\E outTerm \\in Int""").containsMatchIn(tlaText) &&
                !Regex("""\\E voteGranted \\in BOOLEAN""").containsMatchIn(tlaText),
            "determined-args through let should drop outTerm and voteGranted exists;\n$handleRvDef",
        )
        assertTrue(
            handleRvDef.contains("lastLogTerm_") &&
                !Regex("""(?m)^\s+lastLogTerm == """).containsMatchIn(handleRvDef),
            "VARIABLE lastLogTerm stays bare; the message-field LET is lastLogTerm_;\n$handleRvDef",
        )
        assertTrue(
            handleRvDef.contains("lastLogTerm_ > lastLogTerm[") &&
                handleRvDef.contains("lastLogTerm_ = lastLogTerm["),
            "vote logOk should compare the candidate term to state lastLogTerm[n], not the arg to itself;\n$handleRvDef",
        )
        val nextBlock = tlaAfterNext(tlaText).substringBefore("\n\n")
        assertTrue(
            !Regex("""\\E n, m \\in NodeSet :[^\n]*handleRequestVoteRequest""").containsMatchIn(nextBlock) &&
                !Regex("""\\E n, m \\in NodeSet :[^\n]*handleAppendEntriesResponse""").containsMatchIn(nextBlock),
            "handle Next should not wrap bag exists with \\E n, m \\in NodeSet;\n$nextBlock",
        )
        assertTrue(
            !nextBlock.contains("\\E peer \\in Int : handleRequestVoteResponse") &&
                !nextBlock.contains("\\E peer \\in Int : handleAppendEntriesResponse"),
            "response handlers should bind peer from the message, not \\E peer \\in Int;\n$nextBlock",
        )
        assertTrue(
            nextBlock.contains("dropStaleResponse"),
            "Next should include dropStaleResponse;\n$nextBlock",
        )
        val dropStaleDef = tlaText.substringAfter("dropStaleResponse(").substringBefore("\n\n")
        assertTrue(
            (
                dropStaleDef.contains("voteResponseMsgs") &&
                    dropStaleDef.contains("appendEntriesResponseMsgs")
                ) &&
                (
                    dropStaleDef.contains("SelectSeq") ||
                        dropStaleDef.contains("{") ||
                        dropStaleDef.contains("filter")
                    ) &&
                dropStaleDef.contains("voteResponseMsgs'") &&
                dropStaleDef.contains("appendEntriesResponseMsgs'"),
            "dropStaleResponse should shrink stale vote/AE response bags;\n$dropStaleDef",
        )
        val handleAeDef = tlaText.substringAfter("handleAppendEntriesResponse(").substringBefore("\n\n")
        val keepTermAt = listOf(".term =", ".dest =", ".src =")
            .map { handleAeDef.indexOf(it) }
            .filter { it >= 0 }
            .minOrNull() ?: -1
        val nextIndexAt = handleAeDef.indexOf("nextIndex'")
        assertTrue(
            keepTermAt >= 0 && nextIndexAt >= 0 && keepTermAt < nextIndexAt,
            "struct keep equalities should precede primed nextIndex;\n$handleAeDef",
        )
        val cfgText = emit.cfgText
        assertTrue(
            cfgText.contains("CONSTANT Int = {0, 1, 2, 3"),
            "cfg Int should pad through MaxListLen so node id 3 is in the universe;\n$cfgText",
        )
        assertTrue(
            cfgText.contains("INVARIANT OneLeaderPerTerm") &&
                cfgText.contains("INVARIANT StateMachineSafety") &&
                !cfgText.contains("INVARIANT AllInvariants"),
            "cfg should check leaf invariants, not AllInvariants;\n$cfgText",
        )
        assertTrue(
            cfgText.contains("CONSTRAINT StateConstraint") &&
                !cfgText.contains("INVARIANT StateConstraint"),
            "inferred StateConstraint should be a TLC CONSTRAINT;\n$cfgText",
        )
        val constraintDef = tlaText.substringAfter("StateConstraint ==").substringBefore("\n\n")
        assertTrue(
            (
                constraintDef.contains("currentTerm[n] \\in Int") ||
                    constraintDef.contains("RaftProtocol_currentTerm[n] \\in Int")
                ) &&
                (
                    constraintDef.contains("commitIndex[n] \\in Int") ||
                        constraintDef.contains("RaftProtocol_commitIndex[n] \\in Int")
                    ) &&
                (
                    Regex("""Len\([^)]*log\[n\]\)\s*<=\s*MaxListLen""").containsMatchIn(constraintDef) ||
                        Regex("""Len\(RaftProtocol_log\[n\]\)\s*<=\s*MaxListLen""").containsMatchIn(constraintDef)
                    ),
            "CONSTRAINT should bind Int vars to cfg Int and list Len to MaxListLen;\n$constraintDef",
        )
        assertTrue(
            tlaText.contains("self \\in [NodeSet -> TypeOKInt]"),
            "TypeOK should type self as [NodeSet -> TypeOKInt];\n${tlaText.substringAfter("TypeOK ==").take(1500)}",
        )
        val typeOkBlock = tlaText.substringAfter("TypeOK ==").substringBefore("\n\n")
        assertTrue(
            Regex("""state \\in \[NodeSet -> \{[^]]*Follower[^]]*\}]""").containsMatchIn(typeOkBlock) ||
                (
                    typeOkBlock.contains("state \\in [NodeSet -> {") &&
                        typeOkBlock.contains("\"Follower\"") &&
                        typeOkBlock.contains("\"Candidate\"") &&
                        typeOkBlock.contains("\"Leader\"") &&
                        !typeOkBlock.contains("state \\in [NodeSet -> String]")
                    ),
            "TypeOK state should use the three roles, not String;\n$typeOkBlock",
        )
        assertTrue(
            Regex("""DOMAIN [^\n]*nextIndex\[n\]\s*=\s*[^\n]*cluster""").containsMatchIn(typeOkBlock) ||
                typeOkBlock.contains("DOMAIN nextIndex[n] = cluster") ||
                typeOkBlock.contains("DOMAIN nextIndex[n] = RaftProtocol_cluster"),
            "TypeOK should pin nextIndex domain to cluster;\n$typeOkBlock",
        )
        assertTrue(
            typeOkBlock.contains("[RaftProtocol_cluster -> TypeOKInt]") ||
                typeOkBlock.contains("[cluster -> TypeOKInt]") ||
                Regex("""nextIndex \\in \[NodeSet -> \[[^\]]*cluster[^\]]* -> TypeOKInt]]""").containsMatchIn(typeOkBlock),
            "TypeOK nextIndex must use enumerable cluster as the map domain, not TypeOKInt;\n$typeOkBlock",
        )
        assertTrue(
            !typeOkBlock.contains("SUBSET TypeOKInt") && !typeOkBlock.contains("[TypeOKInt -> TypeOKInt]"),
            "TypeOK must not ask TLC to enumerate TypeOKInt as a set/function domain;\n$typeOkBlock",
        )
        assertTrue(
            (
                typeOkBlock.contains("votesGranted[n] \\subseteq cluster") ||
                    typeOkBlock.contains("votesGranted[n] \\subseteq RaftProtocol_cluster")
                ) &&
                !typeOkBlock.contains("votesGranted[n] \\subseteq Range("),
            "TypeOK should pin votesGranted to cluster;\n$typeOkBlock",
        )
        assertTrue(
            tlaText.contains("\\A n1, n2 \\in NodeSet :") &&
                !tlaText.contains("\\A n1 \\in NodeSet :"),
            "consecutive \\A over NodeSet should combine;\n${tlaText.substringAfter("OneLeaderPerTerm ==").take(500)}",
        )
        assertTrue(
            tlaText.contains("StateMachineSafety") &&
                (tlaText.contains("1 <= i") || tlaText.contains("i >= 1")) &&
                (tlaText.contains("min(") || tlaText.contains("max(")) &&
                (
                    tlaText.contains("commitIndex[n1]") ||
                        tlaText.contains("RaftProtocol_commitIndex") ||
                        tlaText.contains("Len(log[n1])") ||
                        tlaText.contains("Len(RaftProtocol_log[n1])")
                    ),
            "StateMachineSafety should constrain overlapping log indices;\n${tlaText.substringAfter("StateMachineSafety").take(800)}",
        )
        assertTrue(
            Regex("""=> \(n1 = n2\)\n\nStateMachineSafety ==""").containsMatchIn(tlaText) &&
                Regex("""\n\nAllInvariants == OneLeaderPerTerm /\\ StateMachineSafety""").containsMatchIn(tlaText),
            "user-specified invariants should be separated by a blank line;\n${tlaText.substringAfter("user-specified invariants").take(1200)}",
        )
        assertTrue(
            (
                tlaText.contains("votedFor \\in [NodeSet -> [present: BOOLEAN, value: TypeOKInt]]") ||
                    tlaText.contains("votedFor \\in [NodeSet -> [present: BOOLEAN, value: TypeOKInt]") ||
                    Regex("""votedFor \\in \[NodeSet -> \[present: BOOLEAN, value: TypeOKInt]]""").containsMatchIn(tlaText)
                ) &&
                !tlaText.contains("votedFor \\in [NodeSet -> TypeOKInt]") &&
                tlaText.contains("TypeOKInt == Int \\cup Nat \\cup {") &&
                Regex("""\\* cfg Int[^\n]+\nTypeOKInt ==""").containsMatchIn(tlaText),
            "votedFor should be Optional records; TypeOKInt should union Int and Nat;\n${tlaText.substringAfter("automatically generated invariants").take(2500)}",
        )
        assertTrue(
            !Regex("""LET nextLogIndex ==[^\n]* IN\s+LET prevLogIndex""").containsMatchIn(tlaText) &&
                Regex("""LET nextLogIndex ==.*prevLogIndex ==.*prevLogTerm ==""", RegexOption.DOT_MATCHES_ALL)
                    .containsMatchIn(tlaText),
            "chained expression lets should be one LET;\n${tlaText.substringAfter("appendEntries").take(1500)}",
        )
        assertTrue(
            !Regex("""LET advNext ==[^\n]* IN\s+LET newMatch""").containsMatchIn(tlaText) &&
                Regex("""LET advNext ==.*newMatch ==.*updatedMatch ==""", RegexOption.DOT_MATCHES_ALL)
                    .containsMatchIn(tlaText),
            "back-to-back transit lets should be one LET;\n${tlaText.substringAfter("handleAppendEntriesResponse").take(2000)}",
        )
        val (offVars, offWarn) = compileSpecTla(
            source,
            "RaftNodeSpec",
            compileNames = listOf("RaftNodeSpec"),
            tlaOptConfig = TlaOptConfig.fromDisableTlaOptFlag("unused-vars"),
        )
        assertTrue(
            offVars.contains("knownLeader"),
            "disabling unused-vars should restore knownLeader;\n${offVars.take(4000)}",
        )
        assertTrue(
            offWarn.any { it.contains("unused-fields") && it.contains("\"url\"") },
            "unused-fields url warning should still fire;\n$offWarn",
        )
        File("RaftNodeSpec.tla").delete()
        File("RaftNodeSpec.cfg").delete()
    }

    @Test
    fun raftNodeSpecCompilesAndTlcRuns() {
        assumeTlcPresent()
        val work = Files.createTempDirectory("julay-spec-raft-tlc").toFile()
        try {
            val source = File("input/raft/sys.jul")
            assertTrue(source.exists(), "missing ${source.path}")
            compileJulFile(source.toPath(), keepBuild = false)
            val tla = File("RaftNodeSpec.tla")
            val cfg = File("RaftNodeSpec.cfg")
            assertTrue(tla.exists(), "expected RaftNodeSpec.tla")
            assertTrue(cfg.exists(), "expected RaftNodeSpec.cfg")
            tla.copyTo(File(work, "RaftNodeSpec.tla"), overwrite = true)
            cfg.copyTo(File(work, "RaftNodeSpec.cfg"), overwrite = true)
            assertTlcHealthyStart(work, "RaftNodeSpec")
        } finally {
            work.deleteRecursively()
            File("RaftNodeSpec.tla").delete()
            File("RaftNodeSpec.cfg").delete()
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
            val initDef = tlaText.substringAfter("Init ==").substringBefore("\n\n")
            assertTrue(
                initDef.contains("p = [x |-> 0, y |-> 0]") ||
                    initDef.contains("[x |-> 0, y |-> 0]"),
                "single-line Julay obj should stay compact in folded Init;\n$initDef",
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
            val cfgText = File("InvLayout.cfg").readText()
            assertTrue(
                tlaText.contains("\n\n\\* system definition\n\nInit =="),
                "system definition should sit two blanks after funs/vars and one blank before Init;\n$tlaText",
            )
            assertTrue(
                tlaText.contains(
                    "Spec == Init /\\ [][Next]_vars\n\n\n\\* Invariants\n\n" +
                        "\\* automatically generated invariants\n\n",
                ) &&
                    tlaText.contains("\\* automatically generated invariants\n\n\\* cfg Int") &&
                    tlaText.contains("TypeOKInt == Int \\cup Nat \\cup {") &&
                    Regex("""TypeOKInt ==[^\n]+\n\nTypeOK ==""").containsMatchIn(tlaText),
                "TypeOKInt should sit immediately above TypeOK under automatically generated invariants;\n$tlaText",
            )
            assertTrue(
                tlaText.contains("currentTerm \\in [NodeSet -> TypeOKInt]"),
                "TypeOK should type parametric Int vars as [NodeSet -> TypeOKInt];\n$tlaText",
            )
            val invLines = cfgText.lineSequence().filter { it.startsWith("INVARIANT ") }.toList()
            assertTrue(
                invLines.firstOrNull() == "INVARIANT TypeOK" &&
                    invLines.contains("INVARIANT OneLeaderPerTerm"),
                "cfg should list TypeOK first then OneLeaderPerTerm;\n$cfgText",
            )
            val invDef = tlaText.substringAfter("OneLeaderPerTerm ==").substringBefore("\n====")
            assertTrue(
                invDef.contains("\\A n1, n2 \\in NodeSet :") &&
                    !invDef.contains("\\A n1 \\in NodeSet :") &&
                    !invDef.contains("\\A n2 \\in NodeSet :"),
                "consecutive \\A over NodeSet should combine;\n$invDef",
            )
            assertTrue(
                tlaText.contains("\\* user-specified invariants\n\nOneLeaderPerTerm =="),
                "user-specified comment should precede OneLeaderPerTerm;\n$tlaText",
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
    fun listIndexInvariantDoesNotApplyEmptySeqAtZero() {
        assumeTlcPresent()
        val work = Files.createTempDirectory("julay-spec-list-index-inv").toFile()
        try {
            val source = File("regression/input/spec/list-index-inv.jul")
            assertTrue(source.exists(), "missing ${source.path}")
            compileJulFile(source.toPath(), keepBuild = false)
            val tla = File("ListIndexInv.tla")
            val cfg = File("ListIndexInv.cfg")
            assertTrue(tla.exists(), "expected ListIndexInv.tla")
            val tlaText = tla.readText()
            assertTrue(
                tlaText.contains("i >= 1") &&
                    tlaText.contains("Len(log[n1])") &&
                    tlaText.contains("log[n1][i]"),
                "SameLog should guard 1-based indices before list apply;\n${tlaText.substringAfter("SameLog").take(600)}",
            )
            tla.copyTo(File(work, "ListIndexInv.tla"), overwrite = true)
            cfg.copyTo(File(work, "ListIndexInv.cfg"), overwrite = true)
            tla.delete()
            cfg.delete()
            assertTlcCompletesWithoutEvalError(work, "ListIndexInv")
        } finally {
            work.deleteRecursively()
            File("ListIndexInv.tla").delete()
            File("ListIndexInv.cfg").delete()
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
                beforeInit.contains("\\* user defined funs") &&
                    beforeInit.contains("entryTermAt(p_log, idx) ==") &&
                    beforeInit.contains("bumpTerm(t) =="),
                "used funs should be operators above Init under user defined funs; params colliding with VARIABLES are renamed;\n$beforeInit",
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
                beforeInit.contains("\\* Julay lib funs") && beforeInit.contains("startsWith("),
                "startsWith helper should be above Init under Julay lib funs;\n$beforeInit",
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
                beforeInit.contains("\\* Julay lib funs") && beforeInit.contains("splice(p_xs,"),
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
                sessionCommentIdx >= 0 && (unchangedIdx < 0 || sessionCommentIdx < unchangedIdx),
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
                tlaText.contains("\\E a, b \\in Int : meet(a, b)"),
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
            val spawnDef = tlaText.substringAfter("spawnWorker(i, id_) ==").substringBefore("\n\n")
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
                tlaText.contains("Terminates ==") && tlaText.contains("~>"),
                "expected Terminates == (... ~> ...);\n$tlaText",
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
                tlaText.contains("Terminates ==") && tlaText.contains("~>"),
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
                tlaText.contains("\\* Julay lib funs") &&
                    tlaText.contains("splice(xs, s, e) ==") &&
                    tlaText.contains("splice(") &&
                    tlaText.contains("SubSeq(") &&
                    !tlaText.contains("DOMAIN TRUE") &&
                    !Regex("""\be\.value\b""").containsMatchIn(tlaText),
                "expected splice operator for slices (SubSeq inside splice) and substituted map binders;\n$tlaText",
            )
            assertTrue(
                (tlaText.contains("[target_idx]") || tlaText.contains("[target.id]") ||
                    tlaText.contains("cluster[i][target]")) &&
                    !tlaText.contains("((target.id) + 1)") &&
                    !tlaText.contains("[(target.id) + 1]") &&
                    !tlaText.contains("[((target.id) + 1)]"),
                "expected 1-based list indexes without +1 shift;\n$tlaText",
            )
            assertTrue(
                tlaText.contains("SubSeq(") &&
                    !tlaText.contains("(lo) + 1") &&
                    !tlaText.contains("(lo)+1") &&
                    (
                        Regex("""IF \w+ > \w+ THEN <<>> ELSE SubSeq""").containsMatchIn(tlaText) ||
                            Regex("""IF \w+ < 1 THEN <<>>""").containsMatchIn(tlaText)
                    ),
                "expected inclusive SubSeq splice helper;\n$tlaText",
            )
            assertTrue(
                tlaText.contains("\\* TLA+ helpers") &&
                    tlaText.contains("BoundedSeq(") &&
                    tlaText.contains("MaxListLen"),
                "expected BoundedSeq under TLA+ helpers / MaxListLen;\n$tlaText",
            )
            assertTrue(
                tlaText.contains("Range(") &&
                    tlaText.contains("SetToSeq(") &&
                    tlaText.contains("RECURSIVE SetToSeq") &&
                    tlaText.contains("allDistinct(") &&
                    tlaText.contains("Cardinality(Range("),
                "expected Range / SetToSeq / allDistinct conversion helpers;\n$tlaText",
            )
            val nextBody = tlaAfterNext(tlaText).substringBefore("Spec ==")
            assertTrue(
                !Regex("""(?<!Bounded)(?<!Sub)Seq\(""").containsMatchIn(nextBody),
                "must not use bare Seq(...) as a Next/action domain;\n$nextBody",
            )
            assertTrue(
                cfgText.contains("MaxListLen") && cfgText.contains("CONSTANT MaxListLen = 3"),
                "expected MaxListLen = 3 in cfg;\n$cfgText",
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

    @Test
    fun globalIndexVarsStayScalarInTla() {
        assumeTlcPresent()
        val work = Files.createTempDirectory("julay-spec-global").toFile()
        try {
            val source = File("regression/input/spec/global-index-var.jul")
            assertTrue(source.exists(), "missing ${source.path}")
            compileJulFile(source.toPath(), keepBuild = false)
            val tla = File("Indexed.tla")
            val cfg = File("Indexed.cfg")
            assertTrue(tla.exists(), "expected Indexed.tla")
            assertTrue(cfg.exists(), "expected Indexed.cfg")
            val tlaText = tla.readText()
            assertTrue(
                tlaText.contains("/\\ cluster \\in BoundedSeq("),
                "expected const-global Init as \\\\in BoundedSeq (enumerable);\n$tlaText",
            )
            assertTrue(
                !tlaText.contains("cluster = [n \\in") &&
                    !tlaText.contains("/\\ cluster = <<>>"),
                "global cluster must not be a function of n or default Init;\n$tlaText",
            )
            assertTrue(
                tlaText.contains("/\\ extra = 0"),
                "expected scalar Init for global extra;\n$tlaText",
            )
            assertTrue(
                tlaText.contains("state = [n \\in Node |->"),
                "expected indexed Init for per-instance state;\n$tlaText",
            )
            assertTrue(
                !tlaText.contains("cluster' =") &&
                    !tlaText.contains("cluster'"),
                "const-global cluster must not be primed;\n$tlaText",
            )
            assertTrue(
                !tlaText.contains("start(n, me) ==") &&
                    !tlaText.contains("Peer_constructed"),
                "sole unsynced start ctor should fold into Init;\n$tlaText",
            )
            assertTrue(
                tlaText.contains("extra' = extra + 1") ||
                    tlaText.contains("extra' = 0"),
                "expected scalar extra write;\n$tlaText",
            )
            assertTrue(
                !tlaText.contains("extra' = [extra EXCEPT ![n]"),
                "global extra must not use EXCEPT ![n];\n$tlaText",
            )
            assertTrue(
                tlaText.contains("state' = [state EXCEPT ![n]"),
                "expected EXCEPT ![n] for per-instance state;\n$tlaText",
            )
            assertTrue(
                (tlaText.contains("Len(cluster)") || tlaText.contains("Len(cluster)")) &&
                    !tlaText.contains("cluster[i]"),
                "expected unindexed Peer[i].cluster in invariant;\n$tlaText",
            )
            assertTrue(
                tlaText.contains("state[i]"),
                "expected indexed Peer[i].state in invariant;\n$tlaText",
            )
            val initDef = tlaText.substringAfter("Init ==").substringBefore("\n\n")
            assertTrue(
                tlaText.contains("\\* Peer constructor assumption") &&
                    initDef.contains("\\E me \\in String : me \\in Range(cluster)") &&
                    rangeHelperUnderTlaHelpers(tlaText),
                "expected folded ~in error as Range membership with unbound me existential;\n$initDef",
            )
            val bumpDef = tlaText.substringAfter("bump(n) ==").substringBefore("\n\n")
                .ifEmpty { tlaText.substringAfter("bump(").let { rest ->
                    val sigEnd = rest.indexOf(" ==")
                    if (sigEnd < 0) "" else rest.substring(sigEnd + 3).substringBefore("\n\n")
                } }
            assertTrue(
                bumpDef.contains("UNCHANGED") && bumpDef.contains("cluster"),
                "const-global cluster must stay in UNCHANGED;\n$bumpDef",
            )
            tla.copyTo(File(work, "Indexed.tla"), overwrite = true)
            cfg.copyTo(File(work, "Indexed.cfg"), overwrite = true)
            tla.delete()
            cfg.delete()
            assertTlcHealthyStart(work, "Indexed")
        } finally {
            work.deleteRecursively()
            File("Indexed.tla").delete()
            File("Indexed.cfg").delete()
        }
    }

    @Test
    fun initClauseConjoinsLenEqualsCardinality() {
        assumeTlcPresent()
        val work = Files.createTempDirectory("julay-spec-init-clause").toFile()
        try {
            val source = File("regression/input/spec/init-const-global.jul")
            assertTrue(source.exists(), "missing ${source.path}")
            compileJulFile(source.toPath(), keepBuild = false)
            val tla = File("Indexed.tla")
            val cfg = File("Indexed.cfg")
            assertTrue(tla.exists(), "expected Indexed.tla")
            val tlaText = tla.readText()
            assertTrue(
                tlaText.contains("/\\ cluster \\in BoundedSeq(") ||
                    tlaText.contains("/\\ cluster \\in BoundedSeq("),
                "expected const-global Init as BoundedSeq;\n$tlaText",
            )
            val initBlock = tlaText.substringAfter("Init ==").substringBefore("\n\n")
            val constraint = listOf(
                "Len(cluster) = Cardinality(Node)",
                "Len(cluster) = Cardinality(Node)",
            ).firstOrNull { it in initBlock }
            assertTrue(
                constraint != null,
                "expected init: cluster.length = Node.length in Init;\n$initBlock",
            )
            val trimmed = initBlock.trimEnd()
            val commentIdx = trimmed.lastIndexOf("\\* init constraints")
            val constraintIdx = trimmed.lastIndexOf(constraint!!)
            assertTrue(
                commentIdx >= 0 && commentIdx < constraintIdx &&
                    (trimmed.endsWith(constraint) || trimmed.endsWith("/\\ $constraint")),
                "init constraints comment and conjuncts should be last in Init;\n$initBlock",
            )
            tla.copyTo(File(work, "Indexed.tla"), overwrite = true)
            cfg.copyTo(File(work, "Indexed.cfg"), overwrite = true)
            tla.delete()
            cfg.delete()
            assertTlcHealthyStart(work, "Indexed")
        } finally {
            work.deleteRecursively()
            File("Indexed.tla").delete()
            File("Indexed.cfg").delete()
        }
    }

    @Test
    fun constGlobalAssignEmitsEqualityCheck() {
        assumeTlcPresent()
        val work = Files.createTempDirectory("julay-spec-const-global-check").toFile()
        try {
            val source = File("regression/input/spec/const-global-check.jul")
            assertTrue(source.exists(), "missing ${source.path}")
            compileJulFile(source.toPath(), keepBuild = false)
            val tla = File("ConstCheck.tla")
            val cfg = File("ConstCheck.cfg")
            assertTrue(tla.exists(), "expected ConstCheck.tla")
            val tlaText = tla.readText()
            assertTrue(
                tlaText.contains("/\\ cluster \\in BoundedSeq(") ||
                    tlaText.contains("/\\ cluster \\in BoundedSeq("),
                "expected const-global Init as \\\\in BoundedSeq (enumerable);\n$tlaText",
            )
            assertTrue(
                (tlaText.contains("/\\ cluster = <<\"a\">> \\* global const check") ||
                    tlaText.contains("/\\ cluster = <<\"a\">> \\* global const check")) &&
                    !tlaText.contains("cluster'") &&
                    !tlaText.contains("cluster'"),
                "expected unprimed equality check for non-arg RHS;\n$tlaText",
            )
            tla.copyTo(File(work, "ConstCheck.tla"), overwrite = true)
            cfg.copyTo(File(work, "ConstCheck.cfg"), overwrite = true)
            tla.delete()
            cfg.delete()
            assertTlcHealthyStart(work, "ConstCheck")
        } finally {
            work.deleteRecursively()
            File("ConstCheck.tla").delete()
            File("ConstCheck.cfg").delete()
        }
    }

    @Test
    fun ctorErrorAssumptionsFlipNotinAndNeq() {
        assumeTlcPresent()
        val work = Files.createTempDirectory("julay-spec-ctor-err").toFile()
        try {
            val source = File("regression/input/spec/ctor-error-assumption.jul")
            assertTrue(source.exists(), "missing ${source.path}")
            compileJulFile(source.toPath(), keepBuild = false)
            val tla = File("CtorErr.tla")
            val cfg = File("CtorErr.cfg")
            assertTrue(tla.exists(), "expected CtorErr.tla")
            val tlaText = tla.readText()
            val startDef = tlaText.substringAfter("start(n,").substringBefore("\n\n")
            assertTrue(
                startDef.contains("\\* P constructor assumption"),
                "expected constructor assumption comment;\n$tlaText",
            )
            assertTrue(
                startDef.contains("me \\in Range(cluster)"),
                "expected ~in flipped to \\\\in Range;\n$tlaText",
            )
            assertTrue(
                rangeHelperUnderTlaHelpers(tlaText),
                "expected Range helper under TLA+ helpers;\n$tlaText",
            )
            assertTrue(
                startDef.contains("k = 1"),
                "expected ~= flipped to =;\n$tlaText",
            )
            assertTrue(
                startDef.contains("allDistinct(cluster)") &&
                    !startDef.contains("~~allDistinct"),
                "expected ~ flipped off allDistinct, not double negation;\n$tlaText",
            )
            assertTrue(
                startDef.contains("~(port <= 0)") || startDef.contains("~(port <= 0)"),
                "expected wrapped non-flipped error cond;\n$tlaText",
            )
            assertTrue(
                startDef.indexOf("constructor assumption") < startDef.indexOf("constructor logic"),
                "assumption must precede logic;\n$tlaText",
            )
            assertTrue(
                tlaText.endsWith("\n\n====\n"),
                "expected blank line before closing ====;\n$tlaText",
            )
            assertTrue(
                startDef.contains("~P_constructed[n]") || startDef.contains("~P_constructed[n]"),
                "expected constructor logic constructed gate;\n$tlaText",
            )
            tla.copyTo(File(work, "CtorErr.tla"), overwrite = true)
            cfg.copyTo(File(work, "CtorErr.cfg"), overwrite = true)
            tla.delete()
            cfg.delete()
            assertTlcHealthyStart(work, "CtorErr")
        } finally {
            work.deleteRecursively()
            File("CtorErr.tla").delete()
            File("CtorErr.cfg").delete()
        }
    }

    @Test
    fun transitionErrorBecomesAssumption() {
        assumeTlcPresent()
        val work = Files.createTempDirectory("julay-spec-trans-err").toFile()
        try {
            val source = File("regression/input/spec/transition-error-assumption.jul")
            assertTrue(source.exists(), "missing ${source.path}")
            compileJulFile(source.toPath(), keepBuild = false)
            val tla = File("TransErr.tla")
            val cfg = File("TransErr.cfg")
            assertTrue(tla.exists(), "expected TransErr.tla")
            val tlaText = tla.readText()
            val tickDef = tlaText.substringAfter("tick ==").substringBefore("\n\n")
            assertTrue(
                tickDef.contains("\\* P internal transition assumption"),
                "expected transition assumption comment;\n$tlaText",
            )
            assertTrue(
                tickDef.contains("~(x = 0)"),
                "expected negated error condition;\n$tlaText",
            )
            assertTrue(
                tickDef.indexOf("transition assumption") < tickDef.indexOf("transition logic"),
                "assumption must precede logic;\n$tlaText",
            )
            assertTrue(
                tickDef.contains("x < 5"),
                "expected original guard still present;\n$tlaText",
            )
            tla.copyTo(File(work, "TransErr.tla"), overwrite = true)
            cfg.copyTo(File(work, "TransErr.cfg"), overwrite = true)
            tla.delete()
            cfg.delete()
            assertTlcHealthyStart(work, "TransErr")
        } finally {
            work.deleteRecursively()
            File("TransErr.tla").delete()
            File("TransErr.cfg").delete()
        }
    }

    private fun assumeTlcPresent() {
        if (!TLC_JAR.isFile) {
            fail("TLC jar not found at ${TLC_JAR.path}")
        }
    }

    /** `Range` belongs with `BoundedSeq` under `\* TLA+ helpers`, not `\* Julay lib funs`. */
    private fun rangeHelperUnderTlaHelpers(tlaText: String): Boolean {
        if (!tlaText.contains("\\* TLA+ helpers")) return false
        if (!Regex("""Range\(\w+\) == \{ \w+\[\w+\] : \w+ \\in DOMAIN \w+ \}""").containsMatchIn(tlaText)) {
            return false
        }
        val afterHelpers = tlaText.substringAfter("\\* TLA+ helpers")
        val helperSec = afterHelpers
            .substringBefore("\\* Julay lib funs")
            .substringBefore("\\* user defined funs")
            .substringBefore("\\* system definition")
        val libSec = if (tlaText.contains("\\* Julay lib funs")) {
            tlaText.substringAfter("\\* Julay lib funs")
                .substringBefore("\\* user defined funs")
                .substringBefore("\\* system definition")
        } else {
            ""
        }
        return helperSec.contains("Range(") &&
            !Regex("""Range\(\w+\) == """).containsMatchIn(libSec)
    }

    /**
     * Success = TLC finishes with no evaluation errors (empty-seq `<<>>[0]`, fingerprint, …).
     * Use for small specs that should complete.
     */
    private fun assertTlcCompletesWithoutEvalError(workDir: File, module: String) {
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
        assertTrue(finished, "TLC timed out on $module.\n$text")
        assertTrue(
            !text.contains("out of domain", ignoreCase = true) &&
                !text.contains("unable to fingerprint", ignoreCase = true),
            "TLC evaluation error on $module.\n$text",
        )
        assertTrue(
            proc.exitValue() == 0 &&
                text.contains("Model checking completed. No error has been found."),
            "expected TLC to complete without error on $module.\n$text",
        )
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

    private fun compileSpecTla(
        source: File,
        specName: String,
        compileNames: List<String> = emptyList(),
        tlaOptConfig: TlaOptConfig = TlaOptConfig.ALL_ON,
    ): Pair<String, List<String>> {
        val emit = compileSpecEmit(source, specName, compileNames, tlaOptConfig)
        return emit.tlaText to emit.warnings
    }

    private fun compileSpecEmit(
        source: File,
        specName: String,
        compileNames: List<String> = emptyList(),
        tlaOptConfig: TlaOptConfig = TlaOptConfig.ALL_ON,
    ): SpecTlaEmit {
        val checked = prepareCheckedCompilation(
            source.toPath(),
            compileNames = compileNames,
        ) ?: fail("prepareCheckedCompilation failed for ${source.path}")
        val spec = checked.unit.modules
            .flatMap { it.root.declNodes().filterIsInstance<SpecNode>() }
            .firstOrNull { it.specNodeName() == specName }
            ?: fail("spec $specName not found in ${source.path}")
        val result = tlaCodegenPass(spec, checked.ast, checked.unit, tlaOptConfig)
        return SpecTlaEmit(result.tlaText, result.cfgText, result.warnings.map { it.toString() })
    }
}

private data class SpecTlaEmit(
    val tlaText: String,
    val cfgText: String,
    val warnings: List<String>,
)
