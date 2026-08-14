package julay.spec

import julay.compiler.OneLocCompileError
import julay.compiler.OneLocCompileWarning
import julay.compiler.loadCompilationUnit
import julay.compiler.pass.TypePassResult
import julay.compiler.pass.typePass
import java.nio.file.Files
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SpecIndexingTest {

    @Test
    fun multiInstanceUnindexedIsError() {
        val result = typeCheck(
            """
            proc Worker {
                var id : Int
                constructor initially(args : List<String>) { transit: id := 0 }
                constructor spawnWorker(id : Int) { transit: id := id }
            }
            spec Bad := Worker
            """.trimIndent(),
        )
        assertTrue(result.errors.any { it.toString().contains("must be indexed") }, result.toString())
        assertEquals(0, result.warnings.size, result.toString())
    }

    @Test
    fun multiInstanceUnindexedWarnsWithFlag() {
        val result = typeCheck(
            """
            proc Worker {
                var id : Int
                constructor initially(args : List<String>) { transit: id := 0 }
                constructor spawnWorker(id : Int) { transit: id := id }
            }
            spec Bad := Worker
            """.trimIndent(),
            allowUnindexedSpec = true,
        )
        assertTrue(result.errors.isEmpty(), result.toString())
        assertTrue(result.warnings.any { it.toString().contains("must be indexed") }, result.toString())
    }

    @Test
    fun initiallyOnlyIndexedWarns() {
        val result = typeCheck(
            """
            proc Counter {
                var n : Int
                constructor initially(args : List<String>) { transit: n := 0 }
                transition bump() { transit: n := n + 1 }
            }
            spec Needless := Counter[i : Int]
            """.trimIndent(),
        )
        assertTrue(result.errors.isEmpty(), result.toString())
        assertTrue(
            result.warnings.any { it.toString().contains("indexing is unnecessary") },
            result.toString(),
        )
    }

    @Test
    fun initiallyOnlyUnindexedIsOk() {
        val result = typeCheck(
            """
            proc Counter {
                var n : Int
                constructor initially(args : List<String>) { transit: n := 0 }
                transition bump() { transit: n := n + 1 }
            }
            spec Ok := Counter
            """.trimIndent(),
        )
        assertTrue(result.errors.isEmpty(), result.toString())
        assertTrue(result.warnings.none { it is OneLocCompileWarning && it.toString().contains("indexed") }, result.toString())
    }

    @Test
    fun compositionIndexCoversNestedLeaves() {
        val result = typeCheck(
            """
            proc A {
                var x : Int
                constructor initially(args : List<String>) { transit: x := 0 }
                constructor makeA(x : Int) { transit: x := x }
            }
            proc B {
                var y : Int
                constructor initially(args : List<String>) { transit: y := 0 }
                constructor makeB(y : Int) { transit: y := y }
            }
            spec Ok := (A || B)[i : Int]
            """.trimIndent(),
        )
        assertTrue(result.errors.isEmpty(), result.toString())
        assertTrue(
            result.errors.none { it is OneLocCompileError && it.toString().contains("must be indexed") },
            result.toString(),
        )
    }

    @Test
    fun multiInstanceIndexedIsOk() {
        val result = typeCheck(
            """
            proc Worker {
                var id : Int
                constructor initially(args : List<String>) { transit: id := 0 }
                constructor spawnWorker(id : Int) { transit: id := id }
            }
            spec Ok := Worker[i : Int]
            """.trimIndent(),
        )
        assertTrue(result.errors.isEmpty(), result.toString())
        assertTrue(result.warnings.isEmpty(), result.toString())
    }

    @Test
    fun unknownGlobalVarIsError() {
        val result = typeCheck(
            """
            sort N := {"a"}
            proc Worker {
                var id : Int
                constructor initially(args : List<String>) { transit: id := 0 }
                constructor spawnWorker(id : Int) { transit: id := id }
            }
            spec Bad := Worker[i : N] {
              global nope
            }
            """.trimIndent(),
        )
        assertTrue(
            result.errors.any { it.toString().contains("global \"nope\"") },
            result.toString(),
        )
    }

    @Test
    fun duplicateGlobalVarIsError() {
        val result = typeCheck(
            """
            sort N := {"a"}
            proc Worker {
                var id : Int
                constructor initially(args : List<String>) { transit: id := 0 }
                constructor spawnWorker(id : Int) { transit: id := id }
            }
            spec Bad := Worker[i : N] {
              global id, id
            }
            """.trimIndent(),
        )
        assertTrue(
            result.errors.any { it.toString().contains("duplicate global variable \"id\"") },
            result.toString(),
        )
    }

    @Test
    fun globalConstructedIsError() {
        val result = typeCheck(
            """
            sort N := {"a"}
            proc Worker {
                var id : Int
                constructor initially(args : List<String>) { transit: id := 0 }
                constructor spawnWorker(id : Int) { transit: id := id }
            }
            spec Bad := Worker[i : N] {
              global constructed
            }
            """.trimIndent(),
        )
        assertTrue(
            result.errors.any { it.toString().contains("synthetic variable \"constructed\"") },
            result.toString(),
        )
    }

    private fun typeCheck(source: String, allowUnindexedSpec: Boolean = false): TypePassResult {
        val dir = Files.createTempDirectory("julay-spec-indexing")
        val file = dir.resolve("main.jul")
        file.writeText(source)
        try {
            val (unit, loadErrors) = loadCompilationUnit(file)
            assertTrue(loadErrors.isEmpty(), loadErrors.toString())
            return unit.root.typePass(unit, allowUnindexedSpec)
        } finally {
            dir.toFile().deleteRecursively()
        }
    }
}
