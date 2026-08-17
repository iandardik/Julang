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

    @Test
    fun plainGlobalOnConstIsError() {
        val result = typeCheck(
            """
            sort N := {"a"}
            proc Worker {
                const cluster : List<String>
                constructor initially(args : List<String>) { transit: cluster := args }
            }
            spec Bad := Worker[i : N] {
              global cluster
            }
            """.trimIndent(),
        )
        assertTrue(
            result.errors.any {
                it.toString().contains(
                    "\"cluster\" may change without declaring it \"const global cluster\", so either make it \"const global cluster\" or change the state var to be \"var cluster\" instead of \"const cluster\"",
                )
            },
            result.toString(),
        )
    }

    @Test
    fun constGlobalOnVarIsError() {
        val result = typeCheck(
            """
            sort N := {"a"}
            proc Worker {
                var cluster : List<String>
                constructor initially(args : List<String>) { transit: cluster := args }
            }
            spec Bad := Worker[i : N] {
              const global cluster
            }
            """.trimIndent(),
        )
        assertTrue(
            result.errors.any {
                it.toString().contains("\"cluster\" is a var") &&
                    it.toString().contains("drop \"const\"") &&
                    it.toString().contains("const cluster")
            },
            result.toString(),
        )
    }

    @Test
    fun constGlobalOnConstIsOk() {
        val result = typeCheck(
            """
            sort N := {"a"}
            proc Worker {
                const cluster : List<String>
                constructor initially(args : List<String>) { transit: cluster := args }
            }
            spec Ok := Worker[i : N] {
              const global cluster
            }
            """.trimIndent(),
        )
        assertTrue(result.errors.isEmpty(), result.toString())
    }

    @Test
    fun initConstGlobalLengthEqualsSortLengthIsOk() {
        val result = typeCheck(
            """
            sort N := {"a", "b"}
            proc Worker {
                const cluster : List<String>
                constructor initially(args : List<String>) { transit: cluster := args }
            }
            spec Ok := Worker[i : N] {
              const global cluster
              init: cluster.length = N.length
            }
            """.trimIndent(),
        )
        assertTrue(result.errors.isEmpty(), result.toString())
    }

    @Test
    fun initLengthFunOnSortIsOk() {
        val result = typeCheck(
            """
            sort N := {"a", "b"}
            proc Worker {
                const cluster : List<String>
                constructor initially(args : List<String>) { transit: cluster := args }
            }
            spec Ok := Worker[i : N] {
              const global cluster
              init: length(cluster) = length(N)
            }
            """.trimIndent(),
        )
        assertTrue(result.errors.isEmpty(), result.toString())
    }

    @Test
    fun initNonBooleanIsError() {
        val result = typeCheck(
            """
            sort N := {"a"}
            proc Worker {
                const cluster : List<String>
                constructor initially(args : List<String>) { transit: cluster := args }
            }
            spec Bad := Worker[i : N] {
              const global cluster
              init: cluster.length
            }
            """.trimIndent(),
        )
        assertTrue(
            result.errors.any { it.toString().contains("must be Boolean") },
            result.toString(),
        )
    }

    @Test
    fun initMutableVarIsError() {
        val result = typeCheck(
            """
            sort N := {"a"}
            proc Worker {
                const cluster : List<String>
                var extra : Int
                constructor initially(args : List<String>) {
                    transit:
                        cluster := args
                        extra := 0
                }
            }
            spec Bad := Worker[i : N] {
              const global cluster
              global extra
              init: extra = 0
            }
            """.trimIndent(),
        )
        assertTrue(
            result.errors.any {
                it.toString().contains("const-global") || it.toString().contains("unbound symbol \"extra\"")
            },
            result.toString(),
        )
    }

    @Test
    fun initSortLargerThanMaxListLenIsError() {
        val result = typeCheck(
            """
            sort N := {"a", "b", "c", "d"}
            proc Worker {
                const cluster : List<String>
                constructor initially(args : List<String>) { transit: cluster := args }
            }
            spec Bad := Worker[i : N] {
              const global cluster
              init: cluster.length = N.length
            }
            """.trimIndent(),
        )
        assertTrue(
            result.errors.any {
                it.toString().contains("MaxListLen") && it.toString().contains("4")
            },
            result.toString(),
        )
    }

    @Test
    fun initIndexedConstGlobalIsError() {
        val result = typeCheck(
            """
            sort N := {"a"}
            proc Worker {
                const cluster : List<String>
                constructor initially(args : List<String>) { transit: cluster := args }
            }
            spec Bad := Worker[i : N] {
              const global cluster
              init: Worker[i].cluster.length = N.length
            }
            """.trimIndent(),
        )
        assertTrue(
            result.errors.any { it.toString().contains("indexed access") },
            result.toString(),
        )
    }

    @Test
    fun initIndexedConstInjectiveIsOk() {
        val result = typeCheck(
            """
            sort N := {"a", "b"}
            proc Worker {
                const me : String
                const cluster : List<String>
                constructor initially(args : List<String>) {
                    transit:
                        me := args[1]
                        cluster := args
                }
            }
            spec Ok := Worker[i : N] {
              const global cluster
              init: forall n1 : N, forall n2 : N, (Worker[n1].me = Worker[n2].me) => (n1 = n2)
            }
            """.trimIndent(),
        )
        assertTrue(result.errors.isEmpty(), result.toString())
    }

    @Test
    fun initIndexedVarIsError() {
        val result = typeCheck(
            """
            sort N := {"a"}
            proc Worker {
                const cluster : List<String>
                var extra : Int
                constructor initially(args : List<String>) {
                    transit:
                        cluster := args
                        extra := 0
                }
            }
            spec Bad := Worker[i : N] {
              const global cluster
              init: Worker[i].extra = 0
            }
            """.trimIndent(),
        )
        assertTrue(
            result.errors.any {
                it.toString().contains("const") || it.toString().contains("var")
            },
            result.toString(),
        )
    }

    @Test
    fun invariantIndexedListLengthAndIndexIsOk() {
        val result = typeCheck(
            """
            sort N := {"a", "b"}
            proc Worker {
                var log : List<String>
                constructor initially(args : List<String>) { transit: log := args }
            }
            invariant SameLog := forall n1 : N, forall n2 : N, forall i : Int,
                (i >= 1 & i <= Worker[n1].log.length & i <= Worker[n2].log.length) => (Worker[n1].log[i] = Worker[n2].log[i])
            spec Ok := Worker[n : N] |= SameLog
            """.trimIndent(),
        )
        assertTrue(result.errors.isEmpty(), result.toString())
    }

    @Test
    fun invariantImportedMaxIsOk() {
        val result = typeCheck(
            """
            import julay.funlib.max
            sort N := {"a", "b"}
            proc Worker {
                var log : List<String>
                var commitIndex : Int
                constructor initially(args : List<String>) {
                    transit:
                        log := args
                        commitIndex := 0
                }
            }
            invariant SameLog := forall n1 : N, forall n2 : N, forall i : Int,
                (1 <= i & i <= max(Worker[n1].commitIndex, Worker[n2].commitIndex)) => (Worker[n1].log[i] = Worker[n2].log[i])
            spec Ok := Worker[n : N] |= SameLog
            """.trimIndent(),
        )
        assertTrue(result.errors.isEmpty(), result.toString())
    }

    @Test
    fun invariantMaxWithoutImportIsError() {
        val result = typeCheck(
            """
            sort N := {"a"}
            proc Worker {
                var n : Int
                constructor initially(args : List<String>) { transit: n := 0 }
            }
            invariant Bound := forall k : N, Worker[k].n <= max(Worker[k].n, Worker[k].n)
            spec Bad := Worker[a : N] |= Bound
            """.trimIndent(),
        )
        assertTrue(
            result.errors.any { it.toString().contains("Unknown function \"max\"") },
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
