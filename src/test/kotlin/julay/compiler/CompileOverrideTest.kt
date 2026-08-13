package julay.compiler

import java.io.File
import java.nio.file.Files
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CompileOverrideTest {

    @Test
    fun cliCompileNamesOverrideSourceDirectives() {
        val checked = prepare(
            """
            proc A {
                constructor initially(args : List<String>) {}
            }
            proc B {
                constructor initially(args : List<String>) {}
            }
            compile B
            """.trimIndent(),
            compileNames = listOf("A"),
        )
        assertEquals(listOf("A"), checked.jarTargets.map { it.name })
        assertTrue(checked.specTargets.isEmpty())
        assertTrue(checked.tlaProcTargets.isEmpty())
    }

    @Test
    fun repeatedCliCompileNamesSelectMultipleTargets() {
        val checked = prepare(
            """
            proc A {
                constructor initially(args : List<String>) {}
            }
            proc B {
                constructor initially(args : List<String>) {}
            }
            invariant Inv := true
            spec S := <true> A <Inv>
            compile A
            """.trimIndent(),
            compileNames = listOf("B", "S"),
        )
        assertEquals(listOf("B"), checked.jarTargets.map { it.name })
        assertEquals(listOf("S"), checked.specTargets.map { it.name })
    }

    @Test
    fun cliCompileWorksWithoutSourceCompileDirective() {
        val checked = prepare(
            """
            proc Solo {
                constructor initially(args : List<String>) {}
            }
            """.trimIndent(),
            compileNames = listOf("Solo"),
        )
        assertEquals(listOf("Solo"), checked.jarTargets.map { it.name })
    }

    @Test
    fun withoutCliCompileUsesSourceDirectives() {
        val checked = prepare(
            """
            proc A {
                constructor initially(args : List<String>) {}
            }
            proc B {
                constructor initially(args : List<String>) {}
            }
            compile A, B
            """.trimIndent(),
        )
        assertEquals(listOf("A", "B"), checked.jarTargets.map { it.name })
    }

    @Test
    fun compileTlaAddsTlaTargetAlongsideSourceCompile() {
        val checked = prepare(
            """
            proc Solo {
                constructor initially(args : List<String>) {}
            }
            compile Solo
            """.trimIndent(),
            compileTlaNames = listOf("Solo"),
        )
        // Source compile still selects JAR; --compile-tla adds a TLA target.
        assertEquals(listOf("Solo"), checked.jarTargets.map { it.name })
        assertEquals(listOf("Solo"), checked.tlaProcTargets.map { it.name })
    }

    @Test
    fun compileTlaAloneWithoutSourceCompile() {
        val checked = prepare(
            """
            proc Solo {
                constructor initially(args : List<String>) {}
            }
            """.trimIndent(),
            compileTlaNames = listOf("Solo"),
        )
        assertTrue(checked.jarTargets.isEmpty())
        assertTrue(checked.specTargets.isEmpty())
        assertEquals(listOf("Solo"), checked.tlaProcTargets.map { it.name })
    }

    @Test
    fun compileTlaRejectsSpecNames() {
        val dir = Files.createTempDirectory("julay-compile-tla-reject")
        val file = dir.resolve("main.jul")
        file.writeText(
            """
            proc A {
                constructor initially(args : List<String>) {}
            }
            invariant Inv := true
            spec S := <true> A <Inv>
            """.trimIndent(),
        )
        assertNull(prepareCheckedCompilation(file, compileTlaNames = listOf("S")))
    }

    @Test
    fun compileTlaEmitsTlaWithoutInvariant() {
        val dir = Files.createTempDirectory("julay-compile-tla-emit")
        val file = dir.resolve("main.jul")
        file.writeText(
            """
            proc Solo {
                var n : Int
                constructor initially(args : List<String>) { transit: n := 0 }
                transition bump() { transit: n := n + 1 }
            }
            """.trimIndent(),
        )
        val cwd = File(".").canonicalFile
        val tla = File(cwd, "Solo.tla")
        val cfg = File(cwd, "Solo.cfg")
        tla.delete()
        cfg.delete()
        try {
            compileJulFile(file, keepBuild = false, compileTlaNames = listOf("Solo"))
            assertTrue(tla.exists(), "expected Solo.tla")
            assertTrue(cfg.exists(), "expected Solo.cfg")
            assertFalse(File(cwd, "Solo.jar").exists(), "should not emit JAR for --compile-tla alone")
            val cfgText = cfg.readText()
            val invLines = cfgText.lineSequence().filter { it.startsWith("INVARIANT ") }.toList()
            assertTrue(
                invLines == listOf("INVARIANT TypeOK"),
                "plain <true> P <true> should list only INVARIANT TypeOK;\n$cfgText",
            )
        } finally {
            tla.delete()
            cfg.delete()
        }
    }

    private fun prepare(
        source: String,
        compileNames: List<String> = emptyList(),
        compileTlaNames: List<String> = emptyList(),
    ): CheckedCompilation {
        val dir = Files.createTempDirectory("julay-compile-override")
        val file = dir.resolve("main.jul")
        file.writeText(source)
        val checked = prepareCheckedCompilation(
            file,
            compileNames = compileNames,
            compileTlaNames = compileTlaNames,
        )
        assertNotNull(checked, "prepareCheckedCompilation failed for:\n$source")
        return checked
    }
}
