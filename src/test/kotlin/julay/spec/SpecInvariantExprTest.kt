package julay.spec

import julay.compiler.compileJulFile
import julay.compiler.loadCompilationUnit
import julay.compiler.pass.TypePassResult
import julay.compiler.pass.typePass
import java.io.File
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail

class SpecInvariantExprTest {

    private data class Pos(val name: String, val formula: String, val imports: List<String> = emptyList())
    private data class Neg(
        val name: String,
        val formula: String,
        val needle: String,
        val imports: List<String> = emptyList(),
        val parameterized: Boolean = true,
    )

    private val positiveFormulas = listOf(
        Pos("nested-length-indexed", "forall k : N, Worker[k].log.length >= 0"),
        Pos("nested-keys-indexed", "forall k : N, Worker[k].table.keys.length >= 0"),
        Pos("obj-field", "forall k : N, Worker[k].box.x >= 0"),
        Pos(
            "list-index",
            "forall k : N, forall i : Int, (i >= 1 & i <= Worker[k].log.length) => (Worker[k].log[i] = Worker[k].log[i])",
        ),
        Pos(
            "max-min",
            "forall k : N, min(Worker[k].n, max(Worker[k].n, 0)) <= Worker[k].n",
            listOf("julay.funlib.max", "julay.funlib.min"),
        ),
        Pos(
            "allDistinct",
            "forall k : N, allDistinct(Worker[k].log)",
            listOf("julay.funlib.allDistinct"),
        ),
        Pos("in", "forall k : N, ~(\"\" in Worker[k].log) | true"),
        Pos(
            "filter-map-toSet",
            "forall k : N, Worker[k].log.filter(s -> true).length >= 0 & Worker[k].log.map(s -> s).length >= 0 & Worker[k].log.toSet().length >= 0",
        ),
        Pos(
            "let-if-when-exists",
            "forall k : N, let (x : Int := Worker[k].n) { x >= 0 } & if (Worker[k].n >= 0) (Worker[k].n >= 0) else (Worker[k].n < 0) & when (Worker[k].n) { 0 -> true else -> Worker[k].n > 0 } & exists i : Int, i = Worker[k].n",
        ),
        Pos(
            "listOf-setOf",
            "1 in listOf(1, 2) & 1 in setOf(1)",
            listOf("julay.funlib.listOf", "julay.funlib.setOf"),
        ),
    )

    private val negativeFormulas = listOf(
        Neg("missing-max-import", "forall k : N, Worker[k].n <= max(Worker[k].n, 0)", "Unknown function \"max\""),
        Neg(
            "println",
            "forall k : N, println(\"x\")",
            "println",
            listOf("julay.funlib.println"),
        ),
        Neg(
            "fold",
            "forall k : N, Worker[k].log.fold(\"\", (a, e) -> a) = \"\"",
            "fold",
        ),
        Neg("this", "forall k : N, this.n >= 0", "this"),
        Neg("non-boolean", "Worker.n", "Boolean", parameterized = false),
        Neg("bare-index", "forall k : N, Worker[k] = Worker[k]", "requires a state variable"),
        Neg("param-without-index", "Worker.n >= 0", "requires indexed access"),
        Neg("paren-unknown-len", "forall k : N, (Worker[k].log.len >= 0)", "len"),
    )

    @Test
    fun positiveInvariantFormulasTypecheck() {
        for (case in positiveFormulas) {
            val result = typeCheck(program(case.formula, case.imports))
            assertTrue(result.errors.isEmpty(), "${case.name}: ${result.errors}")
        }
        val unparamLen = typeCheck(program("Worker.log.length >= 0", parameterized = false))
        assertTrue(unparamLen.errors.isEmpty(), "unparam-length: ${unparamLen.errors}")
        val unparamKeys = typeCheck(program("Worker.table.keys.length >= 0", parameterized = false))
        assertTrue(unparamKeys.errors.isEmpty(), "unparam-keys: ${unparamKeys.errors}")
        val named = typeCheck(
            program(
                extraInvariants = """
                    invariant A := forall k : N, Worker[k].n >= 0
                    invariant B := forall k : N, Worker[k].n <= 0 | true
                """.trimIndent(),
                formula = "A & B",
                invName = "AllInvs",
            ),
        )
        assertTrue(named.errors.isEmpty(), "invariant-name-refs: ${named.errors}")
    }

    @Test
    fun negativeInvariantFormulasError() {
        for (case in negativeFormulas) {
            val result = typeCheck(program(case.formula, case.imports, parameterized = case.parameterized))
            assertTrue(
                result.errors.any { it.toString().contains(case.needle) },
                "${case.name}: expected error containing \"${case.needle}\"; got ${result.errors}",
            )
        }
    }

    @Test
    fun positiveInvariantFormulasAppearInTlaNotTrue() {
        val dir = Files.createTempDirectory("julay-inv-expr-tla")
        val file = dir.resolve("main.jul")
        file.writeText(combinedTlaProgram())
        val cwd = File(".").canonicalFile
        val artifacts = listOf("ExprCov", "PlainCov").map { File(cwd, "$it.tla") to File(cwd, "$it.cfg") }
        artifacts.forEach { (tla, cfg) ->
            tla.delete()
            cfg.delete()
        }
        try {
            compileJulFile(file, keepBuild = false)
            val indexed = File(cwd, "ExprCov.tla")
            val plain = File(cwd, "PlainCov.tla")
            assertTrue(indexed.exists(), "expected ExprCov.tla")
            assertTrue(plain.exists(), "expected PlainCov.tla")
            val indexedText = indexed.readText()
            val plainText = plain.readText()
            val fragments = listOf(
                "Len(" to "nested .length",
                "DOMAIN " to "nested .keys",
                "box[" to "obj field",
                "i >= 1" to "1-based list index",
                "max(" to "imported max",
                "min(" to "imported min",
                "allDistinct(" to "allDistinct",
                "\\in" to "in",
                "SelectSeq(" to ".filter",
                "Range(" to ".toSet",
                "LET " to "let",
                "IF " to "if",
                "CASE " to "when",
                "\\E " to "exists",
                "<<" to "listOf",
                "{" to "setOf",
            )
            for ((frag, label) in fragments) {
                assertTrue(
                    indexedText.contains(frag),
                    "$label should appear in TLA (looked for `$frag`);\n${indexedText.takeLast(2500)}",
                )
                assertFalse(
                    indexedText.contains("$frag TRUE") && frag != "{",
                    "$label must not compile as TRUE;\n$indexedText",
                )
            }
            val userInv = indexedText.substringAfter("\\* user-specified invariants", "")
            assertTrue(userInv.isNotBlank(), "missing user-specified invariants;\n$indexedText")
            assertFalse(
                Regex("""==\s*TRUE\s*$""", RegexOption.MULTILINE).containsMatchIn(userInv),
                "user invariant operators must not be TRUE;\n$userInv",
            )
            assertTrue(plainText.contains("Len("), "unparameterized .length;\n$plainText")
            assertTrue(
                plainText.contains("Cardinality(") || plainText.contains("DOMAIN "),
                "unparameterized .keys;\n$plainText",
            )
        } finally {
            artifacts.forEach { (tla, cfg) ->
                tla.delete()
                cfg.delete()
            }
            dir.toFile().deleteRecursively()
        }
    }

    @Test
    fun maxAndAllDistinctTlcCompletes() {
        assumeTlcPresent()
        val work = Files.createTempDirectory("julay-inv-max-distinct").toFile()
        val dir = Files.createTempDirectory("julay-inv-max-distinct-src")
        val file = dir.resolve("main.jul")
        file.writeText(
            """
            import julay.funlib.max
            import julay.funlib.allDistinct
            import julay.funlib.listOf
            type N
            proc Worker {
                var n : Int
                var xs : List<Int>
                constructor initially(args : List<String>) {
                    transit:
                        n := 0
                        xs := listOf()
                }
            }
            invariant Ok := forall k : N,
                Worker[k].n <= max(Worker[k].n, 0) & allDistinct(Worker[k].xs)
            spec MaxDistinct := Worker[i : N] {
                N := {"a"}
            } |= Ok
            compile MaxDistinct
            """.trimIndent(),
        )
        val cwd = File(".").canonicalFile
        val tla = File(cwd, "MaxDistinct.tla")
        val cfg = File(cwd, "MaxDistinct.cfg")
        tla.delete()
        cfg.delete()
        try {
            compileJulFile(file, keepBuild = false)
            assertTrue(tla.exists(), "expected MaxDistinct.tla")
            val tlaText = tla.readText()
            assertTrue(tlaText.contains("max(") && tlaText.contains("allDistinct("), tlaText)
            assertFalse(tlaText.contains("== TRUE"), "must not emit TRUE for max/allDistinct;\n$tlaText")
            tla.copyTo(File(work, "MaxDistinct.tla"), overwrite = true)
            cfg.copyTo(File(work, "MaxDistinct.cfg"), overwrite = true)
            assertTlcCompletesWithoutEvalError(work, "MaxDistinct")
        } finally {
            tla.delete()
            cfg.delete()
            work.deleteRecursively()
            dir.toFile().deleteRecursively()
        }
    }

    private fun program(
        formula: String,
        imports: List<String> = emptyList(),
        parameterized: Boolean = true,
        invName: String = "P",
        extraInvariants: String = "",
    ): String {
        val importBlock = (listOf("julay.funlib.listOf", "julay.funlib.mapOf") + imports)
            .distinct()
            .joinToString("\n") { "import $it" }
        val spec = if (parameterized) {
            "spec Ok := Worker[i : N] |= $invName"
        } else {
            "spec Ok := Worker |= $invName"
        }
        return """
            $importBlock
            type Point { x : Int }
            type N
            proc Worker {
                var n : Int
                var log : List<String>
                var table : Map<String, Int>
                var box : Point
                constructor initially(args : List<String>) {
                    transit:
                        n := 0
                        log := listOf()
                        table := mapOf()
                        box := Point { x := 0 }
                }
            }
            $extraInvariants
            invariant $invName := $formula
            $spec
        """.trimIndent()
    }

    private fun combinedTlaProgram(): String = """
        import julay.funlib.max
        import julay.funlib.min
        import julay.funlib.allDistinct
        import julay.funlib.listOf
        import julay.funlib.setOf
        import julay.funlib.mapOf

        type Point { x : Int }
        type N

        proc Worker {
            var n : Int
            var log : List<String>
            var table : Map<String, Int>
            var box : Point
            constructor initially(args : List<String>) {
                transit:
                    n := 0
                    log := listOf()
                    table := mapOf()
                    box := Point { x := 0 }
            }
        }

        invariant NestedLen := forall k : N, Worker[k].log.length >= 0
        invariant NestedKeys := forall k : N, Worker[k].table.keys.length >= 0
        invariant ObjField := forall k : N, Worker[k].box.x >= 0
        invariant ListIndex := forall k : N, forall i : Int,
            (i >= 1 & i <= Worker[k].log.length) => (Worker[k].log[i] = Worker[k].log[i])
        invariant MaxMin := forall k : N, min(Worker[k].n, max(Worker[k].n, 0)) <= Worker[k].n
        invariant Distinct := forall k : N, allDistinct(Worker[k].log)
        invariant Membership := forall k : N, ~("" in Worker[k].log) | true
        invariant Hof := forall k : N,
            Worker[k].log.filter(s -> true).length >= 0 &
            Worker[k].log.map(s -> s).length >= 0 &
            Worker[k].log.toSet().length >= 0
        invariant LetIfWhenExists := forall k : N,
            let (x : Int := Worker[k].n) { x >= 0 } &
            if (Worker[k].n >= 0) (Worker[k].n >= 0) else (Worker[k].n < 0) &
            when (Worker[k].n) { 0 -> true else -> Worker[k].n > 0 } &
            exists i : Int, i = Worker[k].n
        invariant Literals := 1 in listOf(1, 2) & 1 in setOf(1)
        invariant AllIndexed := NestedLen & NestedKeys & ObjField & ListIndex & MaxMin & Distinct & Membership & Hof & LetIfWhenExists & Literals

        spec ExprCov := Worker[i : N] {
            N := {"a"}
        } |= AllIndexed

        invariant PlainLen := Worker.log.length >= 0
        invariant PlainKeys := Worker.table.keys.length >= 0
        spec PlainCov := Worker |= PlainLen & PlainKeys

        compile ExprCov, PlainCov
    """.trimIndent()

    private fun typeCheck(source: String): TypePassResult {
        val dir = Files.createTempDirectory("julay-inv-expr")
        val file = dir.resolve("main.jul")
        file.writeText(source)
        try {
            val (unit, loadErrors) = loadCompilationUnit(file)
            assertTrue(loadErrors.isEmpty(), loadErrors.toString())
            return unit.root.typePass(unit)
        } finally {
            dir.toFile().deleteRecursively()
        }
    }

    private fun assumeTlcPresent() {
        val prop = System.getProperty("tla2tools.jar")
        if (prop.isNullOrBlank() || !File(prop).isFile) {
            fail("TLC jar not found (System property tla2tools.jar)")
        }
    }

    private fun assertTlcCompletesWithoutEvalError(workDir: File, module: String) {
        val jar = File(System.getProperty("tla2tools.jar"))
        val pb = ProcessBuilder(
            "java", "-XX:+UseParallelGC",
            "-cp", jar.absolutePath,
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
        val finished = proc.waitFor(30L, TimeUnit.SECONDS)
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
}
