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
            static proc Worker {
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
            static proc Worker {
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
    fun initiallyOnlyIndexedIsOk() {
        val result = typeCheck(
            """
            static proc Counter {
                var n : Int
                constructor initially(args : List<String>) { transit: n := 0 }
                transition bump() { transit: n := n + 1 }
            }
            spec IndexedOk := Counter[i : Int]
            """.trimIndent(),
        )
        assertTrue(result.errors.isEmpty(), result.toString())
        assertTrue(
            result.warnings.none { it.toString().contains("indexing is unnecessary") },
            result.toString(),
        )
    }

    @Test
    fun initiallyOnlyUnindexedIsOk() {
        val result = typeCheck(
            """
            static proc Counter {
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
            static proc A {
                var x : Int
                constructor initially(args : List<String>) { transit: x := 0 }
                constructor makeA(x : Int) { transit: x := x }
            }
            static proc B {
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
            static proc Worker {
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
    fun globalKeywordIsParseError() {
        val dir = Files.createTempDirectory("julay-spec-indexing-parse")
        val file = dir.resolve("main.jul")
        file.writeText(
            """
            type N
            static proc Worker {
                var id : Int
                constructor initially(args : List<String>) { transit: id := 0 }
            }
            spec Bad := Worker[i : N] {
              global Worker.id
            }
            """.trimIndent(),
        )
        try {
            val (_, loadErrors) = loadCompilationUnit(file)
            assertTrue(loadErrors.isNotEmpty(), loadErrors.toString())
        } finally {
            dir.toFile().deleteRecursively()
        }
    }

    @Test
    fun initFrozenConstLengthEqualsSortLengthIsOk() {
        val result = typeCheck(
            """
            type N
            static proc Topology {
                const cluster : List<String>
                constructor initially(cluster : List<String>) { transit: cluster := cluster }
            }
            static proc Worker {
                var flag : Boolean := false
                constructor initially() { transit: flag := false }
            }
            spec Ok := Worker[i : N] || Topology {
              N := {"a", "b"}
              init: Topology.cluster.length = N.length
            }
            """.trimIndent(),
        )
        assertTrue(result.errors.isEmpty(), result.toString())
    }

    @Test
    fun initLengthFunOnSortIsOk() {
        val result = typeCheck(
            """
            type N
            static proc Topology {
                const cluster : List<String>
                constructor initially(cluster : List<String>) { transit: cluster := cluster }
            }
            static proc Worker {
                var flag : Boolean := false
                constructor initially() { transit: flag := false }
            }
            spec Ok := Worker[i : N] || Topology {
              N := {"a", "b"}
              init: length(Topology.cluster) = length(N)
            }
            """.trimIndent(),
        )
        assertTrue(result.errors.isEmpty(), result.toString())
    }

    @Test
    fun initNonBooleanIsError() {
        val result = typeCheck(
            """
            type N
            static proc Topology {
                const cluster : List<String>
                constructor initially(cluster : List<String>) { transit: cluster := cluster }
            }
            static proc Worker {
                var flag : Boolean := false
                constructor initially() { transit: flag := false }
            }
            spec Bad := Worker[i : N] || Topology {
              N := {"a", "b"}
              init: Topology.cluster.length
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
            type N
            static proc Topology {
                const cluster : List<String>
                var extra : Int
                constructor initially(cluster : List<String>) {
                    transit:
                        cluster := cluster
                        extra := 0
                }
            }
            static proc Worker {
                var flag : Boolean := false
                constructor initially() { transit: flag := false }
            }
            spec Bad := Worker[i : N] || Topology {
              N := {"a", "b"}
              init: Topology.extra = 0
            }
            """.trimIndent(),
        )
        assertTrue(
            result.errors.any {
                it.toString().contains("frozen") ||
                    it.toString().contains("not a frozen const")
            },
            result.toString(),
        )
    }

    @Test
    fun initSortLargerThanMaxListLenIsError() {
        val result = typeCheck(
            """
            type N
            static proc Topology {
                const cluster : List<String>
                constructor initially(cluster : List<String>) { transit: cluster := cluster }
            }
            static proc Worker {
                var flag : Boolean := false
                constructor initially() { transit: flag := false }
            }
            spec Bad := Worker[i : N] || Topology {
              N := {"a", "b", "c", "d"}
              init: Topology.cluster.length = N.length
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
    fun initIndexedFrozenConstIsError() {
        val result = typeCheck(
            """
            type N
            static proc Topology {
                const cluster : List<String>
                constructor initially(cluster : List<String>) { transit: cluster := cluster }
            }
            static proc Worker {
                var flag : Boolean := false
                constructor initially() { transit: flag := false }
            }
            spec Bad := Worker[i : N] || Topology {
              N := {"a", "b"}
              init: Topology[i].cluster.length = N.length
            }
            """.trimIndent(),
        )
        assertTrue(
            result.errors.any {
                it.toString().contains("indexed") || it.toString().contains("not parameterized")
            },
            result.toString(),
        )
    }

    @Test
    fun compositionBlockInitBareConstRequiresLeaf() {
        val result = typeCheck(
            """
            type N
            static proc Topology {
                const cluster : List<String>
                constructor initially(cluster : List<String>) { transit: cluster := cluster }
            }
            static proc Worker {
                var flag : Boolean := false
                constructor initially() { transit: flag := false }
            }
            spec Bad := Worker[i : N] || Topology {
              N := {"a", "b"}
              init: cluster.length = N.length
            }
            """.trimIndent(),
        )
        assertTrue(
            result.errors.any {
                it.toString().contains("unbound symbol \"cluster\"") ||
                    it.toString().contains("Leaf.var")
            },
            result.toString(),
        )
    }

    @Test
    fun compositionBlockInitLeafVarIsOk() {
        val result = typeCheck(
            """
            type N
            static proc Topology {
                const cluster : List<String>
                constructor initially(cluster : List<String>) { transit: cluster := cluster }
            }
            static proc Worker {
                var flag : Boolean := false
                constructor initially() { transit: flag := false }
            }
            spec Ok := Worker[i : N] || Topology {
              N := {"a", "b"}
              init: Topology.cluster.length = N.length
            }
            """.trimIndent(),
        )
        assertTrue(result.errors.isEmpty(), result.toString())
    }

    @Test
    fun initIndexedConstInjectiveIsOk() {
        val result = typeCheck(
            """
            type N
            static proc Topology {
                const cluster : List<String>
                constructor initially(cluster : List<String>) { transit: cluster := cluster }
            }
            static proc Worker {
                const me : String
                constructor initially(args : List<String>) {
                    transit:
                        me := args[1]
                }
            }
            spec Ok := Worker[i : N] || Topology {
              N := {"a", "b"}
              init: Topology.cluster.length = N.length
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
            type N
            static proc Topology {
                const cluster : List<String>
                constructor initially(cluster : List<String>) { transit: cluster := cluster }
            }
            static proc Worker {
                var extra : Int
                constructor initially(args : List<String>) {
                    transit:
                        extra := 0
                }
            }
            spec Bad := Worker[i : N] || Topology {
              N := {"a", "b"}
              init: Topology.cluster.length = N.length
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
            type N
            static proc Worker {
                var log : List<String>
                constructor initially(args : List<String>) { transit: log := args }
            }
            invariant SameLog := forall n1 : N, forall n2 : N, forall i : Int,
                (i >= 1 & i <= Worker[n1].log.length & i <= Worker[n2].log.length) => (Worker[n1].log[i] = Worker[n2].log[i])
            spec Ok := Worker[n : N] {
                N := {"a", "b"}
            } |= SameLog
            """.trimIndent(),
        )
        assertTrue(result.errors.isEmpty(), result.toString())
    }

    @Test
    fun invariantImportedMaxIsOk() {
        val result = typeCheck(
            """
            import julay.funlib.max
            type N
            static proc Worker {
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
            spec Ok := Worker[n : N] {
                N := {"a", "b"}
            } |= SameLog
            """.trimIndent(),
        )
        assertTrue(result.errors.isEmpty(), result.toString())
    }

    @Test
    fun invariantMaxWithoutImportIsError() {
        val result = typeCheck(
            """
            type N
            static proc Worker {
                var n : Int
                constructor initially(args : List<String>) { transit: n := 0 }
            }
            invariant Bound := forall k : N, Worker[k].n <= max(Worker[k].n, Worker[k].n)
            spec Bad := Worker[a : N] {
                N := {"a"}
            } |= Bound
            """.trimIndent(),
        )
        assertTrue(
            result.errors.any { it.toString().contains("Unknown function \"max\"") },
            result.toString(),
        )
    }

    @Test
    fun sharedGuaranteeInvariantTypeErrorReportedOnce() {
        // Two specs share one guarantee; the invariant body must not be reported twice.
        val result = typeCheck(
            """
            type Node {
                id : Int
            }
            type N
            static proc Box {
                var owner : Node
                constructor initially(args : List<String>) {
                    transit: owner := Node { id := 0 }
                }
            }
            invariant BadEq := forall n : N, Box[n].owner = n
            invariant G := BadEq
            spec SysA := Box[i : N] {
                N := {"a", "b"}
            }
            spec SysB := Box[i : N] {
                N := {"a", "b"}
            }
            spec A := <true> SysA <G>
            spec B := <true> SysB <G>
            """.trimIndent(),
        )
        val eqErrors = result.errors.filter {
            it.toString().contains("same obj type")
        }
        assertTrue(eqErrors.isNotEmpty(), result.toString())
        assertEquals(1, eqErrors.size, "duplicate invariant diagnostics:\n$result")
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
