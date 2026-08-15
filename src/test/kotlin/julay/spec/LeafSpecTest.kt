package julay.spec

import julay.compiler.OneLocCompileError
import julay.compiler.compileJulFile
import julay.compiler.loadCompilationUnit
import julay.compiler.pass.TypePassResult
import julay.compiler.pass.typePass
import java.nio.file.Files
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertTrue

class LeafSpecTest {

    @Test
    fun unparameterizedLeafSpecTypeChecks() {
        val result = typeCheck(
            """
            spec Env {
                var ready : Boolean := false
                constructor initially(args : List<String>) { transit: ready := false }
                transition mark() { transit: ready := true }
            }
            spec C := Env
            """.trimIndent(),
        )
        assertTrue(result.errors.isEmpty(), result.toString())
    }

    @Test
    fun parameterizedLeafSpecBinderInScope() {
        val result = typeCheck(
            """
            sort Node := { "n1", "n2" }
            spec Net[n : Node] {
                var lastDest : String := ""
                constructor initially(args : List<String>) {}
                transition send() {
                    transit: lastDest := n
                }
            }
            spec S := Net
            """.trimIndent(),
        )
        assertTrue(result.errors.isEmpty(), result.toString())
    }

    @Test
    fun assignToLeafSpecParamIsError() {
        val result = typeCheck(
            """
            sort Node := { "n1" }
            spec Net[n : Node] {
                constructor initially(args : List<String>) {}
                transition bump() {
                    transit: n := "n1"
                }
            }
            spec S := Net
            """.trimIndent(),
        )
        assertTrue(
            result.errors.any { it.toString().contains("Cannot assign to leaf-spec parameter") },
            result.toString(),
        )
    }

    @Test
    fun procAliasOfLeafSpecIsError() {
        val (unit, loadErrors) = loadSource(
            """
            spec A {
                constructor initially(args : List<String>) {}
            }
            proc B := A
            """.trimIndent(),
        )
        assertTrue(
            loadErrors.any { it.toString().contains("cannot appear in a proc assembly") },
            "loadErrors=$loadErrors unit=$unit",
        )
    }

    @Test
    fun procParallelOfLeafSpecsIsError() {
        val (unit, loadErrors) = loadSource(
            """
            spec A {
                constructor initially(args : List<String>) {}
            }
            spec B {
                constructor initially(args : List<String>) {}
            }
            proc D := A || B
            """.trimIndent(),
        )
        assertTrue(
            loadErrors.any { it.toString().contains("cannot appear in a proc assembly") },
            "loadErrors=$loadErrors unit=$unit",
        )
    }

    @Test
    fun procParallelLeafWithProcIsError() {
        val (unit, loadErrors) = loadSource(
            """
            spec A {
                constructor initially(args : List<String>) {}
            }
            proc Peer {
                constructor initially(args : List<String>) {}
            }
            proc E := A || Peer
            """.trimIndent(),
        )
        assertTrue(
            loadErrors.any { it.toString().contains("cannot appear in a proc assembly") },
            "loadErrors=$loadErrors unit=$unit",
        )
    }

    @Test
    fun compositionSpecMayReferenceLeafSpec() {
        val result = typeCheck(
            """
            spec A {
                var ready : Boolean := false
                constructor initially(args : List<String>) { transit: ready := false }
            }
            spec C := A
            """.trimIndent(),
        )
        assertTrue(result.errors.isEmpty(), result.toString())
    }

    @Test
    fun mismatchedReindexOnLeafSpecIsError() {
        val result = typeCheck(
            """
            sort Node := { "n1" }
            sort Other := { "x" }
            spec Net[n : Node] {
                constructor initially(args : List<String>) {}
            }
            spec Bad := Net[i : Other]
            """.trimIndent(),
        )
        assertTrue(
            result.errors.any { it.toString().contains("re-indexed with incompatible type") },
            result.toString(),
        )
    }

    @Test
    fun compileLeafSpecEmitsTlaNotJar() {
        val dir = Files.createTempDirectory("julay-leaf-compile")
        val file = dir.resolve("main.jul")
        file.writeText(
            """
            spec Env {
                var ready : Boolean := false
                constructor initially(args : List<String>) { transit: ready := false }
                transition mark() { transit: ready := true }
            }
            compile Env
            """.trimIndent(),
        )
        val cwd = java.io.File(".").absoluteFile
        val tla = java.io.File(cwd, "Env.tla")
        val cfg = java.io.File(cwd, "Env.cfg")
        val jar = java.io.File(cwd, "Env.jar")
        tla.delete()
        cfg.delete()
        jar.delete()
        try {
            compileJulFile(file, keepBuild = false)
            assertTrue(tla.exists(), "expected Env.tla")
            assertTrue(cfg.exists(), "expected Env.cfg")
            assertTrue(!jar.exists(), "leaf spec must not produce a JAR")
        } finally {
            tla.delete()
            cfg.delete()
            jar.delete()
            dir.toFile().deleteRecursively()
        }
    }

    @Test
    fun alsoArgsTypeCheckOnLeafSpec() {
        val result = typeCheck(
            """
            sort Node := { "n1" }
            proc Peer {
                var self : String := ""
                constructor initially(args : List<String>) { transit: self := "n1" }
                transition ping() { transit: self := self }
            }
            spec Net[n : Node] {
                constructor initially(args : List<String>) {}
                transition observe(target : String) also (m : Node) {
                    guard: Peer[m].self = target
                    transit:
                }
            }
            spec S := Net || Peer[i : Node]
            """.trimIndent(),
        )
        assertTrue(result.errors.isEmpty(), result.toString())
    }

    @Test
    fun alsoArgsOnProcIsError() {
        val result = typeCheck(
            """
            proc Bad {
                constructor initially(args : List<String>) {}
                transition send() also (m : Int) { transit: }
            }
            """.trimIndent(),
        )
        assertTrue(
            result.errors.any { it.toString().contains("also args are only allowed on leaf-spec actions") },
            result.toString(),
        )
    }

    @Test
    fun withApplySharesBinder() {
        val cwd = java.io.File(".").absoluteFile
        val tla = java.io.File(cwd, "Sys.tla")
        val cfg = java.io.File(cwd, "Sys.cfg")
        tla.delete()
        cfg.delete()
        val dir = Files.createTempDirectory("julay-with-share")
        val file = dir.resolve("main.jul")
        file.writeText(
            """
            sort Node := { "n1", "n2" }
            proc Counter {
                var count : Int := 0
                constructor initially(args : List<String>) { transit: count := 0 }
                transition bump() { guard: count >= 0 transit: count := count + 1 }
            }
            spec C1 := Counter[x : Node]
            spec C2 := Counter[x : Node]
            spec Sys := with (i : Node) { C1[i] || C2[i] }
            compile Sys
            """.trimIndent(),
        )
        try {
            compileJulFile(file, keepBuild = false)
            assertTrue(tla.exists(), "expected Sys.tla")
            val text = tla.readText()
            assertTrue(
                text.contains("\\E i \\in Node") || text.contains("[i \\in Node"),
                "expected shared binder i;\n$text",
            )
            assertTrue(
                !text.contains("\\E i_C") && !text.contains("i_C1") && !text.contains("i_C2"),
                "with-scope must not clash-rename binder across peers;\n$text",
            )
        } finally {
            tla.delete()
            cfg.delete()
            dir.toFile().deleteRecursively()
        }
    }

    @Test
    fun withApplyLeafSpecDeclParamSharesBinder() {
        val cwd = java.io.File(".").absoluteFile
        val tla = java.io.File(cwd, "AlsoPeerChecked.tla")
        val cfg = java.io.File(cwd, "AlsoPeerChecked.cfg")
        tla.delete()
        cfg.delete()
        try {
            compileJulFile(java.io.File("regression/input/spec/also-peer-with.jul").toPath(), keepBuild = false)
            assertTrue(tla.exists(), "expected AlsoPeerChecked.tla")
            val text = tla.readText()
            assertTrue(
                Regex("""observe\(n,\s*m""").containsMatchIn(text),
                "observe should take shared n then also-arg m, not a clash-renamed binder;\n$text",
            )
            assertTrue(
                !text.contains("n_Peer") && !text.contains("n_Net") &&
                    !text.contains("\\E n_"),
                "leaf-spec decl param n must not clash-rename the with-binder;\n$text",
            )
        } finally {
            tla.delete()
            cfg.delete()
        }
    }

    @Test
    fun createIndexInsideWithIsError() {
        val result = typeCheck(
            """
            sort Node := { "n1" }
            proc Counter {
                var n : Int := 0
                constructor initially(args : List<String>) { transit: n := 0 }
                transition bump() { transit: n := n + 1 }
            }
            spec Bad := with (i : Node) { Counter[i : Node] }
            """.trimIndent(),
        )
        assertTrue(
            result.errors.any { it.toString().contains("cannot create an index inside with") },
            result.toString(),
        )
    }

    @Test
    fun applyIndexOutsideWithIsError() {
        val result = typeCheck(
            """
            sort Node := { "n1" }
            proc Counter {
                var n : Int := 0
                constructor initially(args : List<String>) { transit: n := 0 }
                transition bump() { transit: n := n + 1 }
            }
            spec Bad := Counter[i]
            """.trimIndent(),
        )
        assertTrue(
            result.errors.any { it.toString().contains("only allowed inside with") },
            result.toString(),
        )
    }

    private fun typeCheck(source: String): TypePassResult {
        val (unit, loadErrors) = loadSource(source)
        assertTrue(loadErrors.isEmpty(), loadErrors.toString())
        return unit.root.typePass(unit)
    }

    private fun loadSource(source: String): Pair<julay.compiler.CompilationUnit, List<julay.compiler.CompileError>> {
        val dir = Files.createTempDirectory("julay-leaf-spec")
        val file = dir.resolve("main.jul")
        file.writeText(source)
        return loadCompilationUnit(file)
    }
}
