package julay.spec

import julay.compiler.compileJulFile
import java.io.File
import java.nio.file.Files
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertTrue

class SpecGuaranteeFormsTest {

    @Test
    fun trueGuaranteeOmitsInvariantFromCfg() {
        withCompiledSpecs(
            """
            proc P {
                var n : Int
                constructor initially(args : List<String>) { transit: n := 0 }
            }
            spec TrueAg := <true> P <true>
            spec TrueModels := P |= true
            spec Plain := P
            compile TrueAg, TrueModels, Plain
            """.trimIndent(),
            "TrueAg", "TrueModels", "Plain",
        ) { artifacts ->
            for (name in listOf("TrueAg", "TrueModels", "Plain")) {
                val (tla, cfgFile) = artifacts.getValue(name)
                val tlaText = tla.readText()
                val cfg = cfgFile.readText()
                val invLines = cfg.lineSequence().filter { it.startsWith("INVARIANT ") }.toList()
                assertTrue(
                    invLines == listOf("INVARIANT TypeOK"),
                    "$name should list only INVARIANT TypeOK;\n$cfg",
                )
                assertTrue(
                    tlaText.contains("\\* automatically generated invariants") &&
                        tlaText.contains("TypeOK =="),
                    "$name missing TypeOK;\n$tlaText",
                )
                assertTrue(
                    !tlaText.contains("\\* user-specified invariants"),
                    "$name should omit user-specified invariants;\n$tlaText",
                )
            }
        }
    }

    @Test
    fun modelsFormMatchesNamedAgGuarantee() {
        withCompiledSpecs(
            """
            proc Counter {
                var n : Int
                constructor initially(args : List<String>) { transit: n := 0 }
                transition bump() { transit: n := n + 1 }
            }
            invariant Bound := Counter.n >= 0
            spec NamedAg := <true> Counter <Bound>
            spec Models := Counter |= Bound
            compile NamedAg, Models
            """.trimIndent(),
            "NamedAg", "Models",
        ) { artifacts ->
            for (name in listOf("NamedAg", "Models")) {
                val (tla, cfg) = artifacts.getValue(name)
                val tlaText = tla.readText()
                val cfgText = cfg.readText()
                assertTrue(tlaText.contains("Bound =="), "$name missing Bound def;\n$tlaText")
                assertTrue(
                    cfgText.contains("INVARIANT Bound"),
                    "$name missing INVARIANT Bound;\n$cfgText",
                )
                val invLines = cfgText.lineSequence().filter { it.startsWith("INVARIANT ") }.toList()
                assertTrue(
                    invLines.firstOrNull() == "INVARIANT TypeOK",
                    "$name should list INVARIANT TypeOK first;\n$cfgText",
                )
            }
        }
    }

    @Test
    fun inlineGuaranteeEmitsSyntheticInvariant() {
        withCompiledSpecs(
            """
            proc Counter {
                var n : Int
                constructor initially(args : List<String>) { transit: n := 0 }
                transition bump() { transit: n := n + 1 }
            }
            spec InlineAg := <true> Counter <Counter.n >= 0>
            spec InlineModels := Counter |= Counter.n >= 0
            compile InlineAg, InlineModels
            """.trimIndent(),
            "InlineAg", "InlineModels",
        ) { artifacts ->
            for (name in listOf("InlineAg", "InlineModels")) {
                val (tla, cfg) = artifacts.getValue(name)
                val tlaText = tla.readText()
                val cfgText = cfg.readText()
                assertTrue(
                    tlaText.contains("Guarantee =="),
                    "$name missing synthetic Guarantee;\n$tlaText",
                )
                assertTrue(
                    cfgText.contains("INVARIANT Guarantee"),
                    "$name missing INVARIANT Guarantee;\n$cfgText",
                )
            }
        }
    }

    private fun withCompiledSpecs(
        source: String,
        vararg moduleNames: String,
        body: (Map<String, Pair<File, File>>) -> Unit,
    ) {
        val dir = Files.createTempDirectory("julay-spec-guarantee-forms")
        val file = dir.resolve("main.jul")
        file.writeText(source)
        val cwd = File(".").canonicalFile
        val artifacts = moduleNames.associateWith { name ->
            File(cwd, "$name.tla") to File(cwd, "$name.cfg")
        }
        artifacts.values.forEach { (tla, cfg) ->
            tla.delete()
            cfg.delete()
        }
        try {
            compileJulFile(file, keepBuild = false)
            artifacts.forEach { (name, pair) ->
                assertTrue(pair.first.exists(), "expected $name.tla")
                assertTrue(pair.second.exists(), "expected $name.cfg")
            }
            body(artifacts)
        } finally {
            artifacts.values.forEach { (tla, cfg) ->
                tla.delete()
                cfg.delete()
            }
            dir.toFile().deleteRecursively()
        }
    }
}
