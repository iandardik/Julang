package julay.compiler

import java.io.File
import java.nio.file.Files
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Regressions for Go backend bugs that passed Julay typechecking but failed `go build`:
 * - obj field `key` clashing with generated `Key()` (Keyer)
 * - duplicate `arg :=` across actions in one `run()` (Go switch shares scope)
 * - unused hub-bound locals when a service has no clients
 */
class GoCodegenCollisionTest {

    @Test
    fun objFieldNamedKeyDoesNotClashWithKeyMethod() {
        val dir = Files.createTempDirectory("go-key-field")
        val buildDir = File("KeyFieldObj-jul-build")
        try {
            val src = dir.resolve("key_field.jul")
            src.writeText(
                """
                import julay.funlib.exitProgram

                type Entry {
                    key : String
                    n : Int
                }

                static proc S {
                    var e : Entry
                    constructor initially(args : List<String>) {
                        transit:
                            e := Entry { key := "a", n := 1 }
                    }
                    internal transition done() {
                        guard: e.key = "a"
                        after: exitProgram(0)
                    }
                }
                proc KeyFieldObj := S
                compile KeyFieldObj
                """.trimIndent(),
            )
            compileJulFile(src, keepBuild = true)
            assertTrue(File("KeyFieldObj").exists(), "expected KeyFieldObj binary")
            val goText = readGo(buildDir)
            assertTrue(
                goText.contains("Key_ string") || goText.contains("\tKey_ "),
                "field key should mangle to Key_ to avoid Key() clash;\n$goText",
            )
            assertTrue(
                Regex("""func \(v Entry\) Key\(\) string""").containsMatchIn(goText),
                "Entry should still implement Key();\n$goText",
            )
            assertTrue(
                goText.contains("v.Key_") || goText.contains("(e).Key_") || goText.contains("e.Key_"),
                "field reads should use Key_;\n$goText",
            )
            assertFalse(
                Regex("""type Entry struct \{[^}]*\bKey string""").containsMatchIn(goText),
                "must not emit field Key alongside Key() method;\n$goText",
            )
        } finally {
            dir.toFile().deleteRecursively()
            buildDir.deleteRecursively()
            File("KeyFieldObj").delete()
            File("KeyFieldObj.jar").delete()
        }
    }

    @Test
    fun duplicateArgNameAcrossActionsCompiles() {
        val dir = Files.createTempDirectory("go-dup-arg")
        val buildDir = File("DupArgBind-jul-build")
        try {
            val src = dir.resolve("dup_arg.jul")
            src.writeText(
                """
                import julay.funlib.exitProgram

                static proc Hub {
                    var a : String
                    var b : String
                    constructor initially(args : List<String>) {
                        transit:
                            a := "x"
                            b := "y"
                    }
                    service transition first(key : String) {
                        guard: key = a
                    }
                    service transition second(key : String) {
                        guard: key = b
                    }
                }
                static proc Client {
                    var step : String
                    constructor initially(args : List<String>) {
                        transit: step := "one"
                    }
                    client transition first(key : String) {
                        guard: step = "one" & key = "x"
                        transit: step := "two"
                    }
                    client transition second(key : String) {
                        guard: step = "two" & key = "y"
                        transit: step := "done"
                    }
                    internal transition exitSystem() {
                        guard: step = "done"
                        after: exitProgram(0)
                    }
                }
                proc DupArgBind := Hub || Client
                compile DupArgBind
                """.trimIndent(),
            )
            compileJulFile(src, keepBuild = true)
            assertTrue(File("DupArgBind").exists(), "expected DupArgBind binary")
            val goText = readGo(buildDir)
            // Scoped locals: key_first / key_second (not two bare `key :=` in one run).
            assertTrue(
                goText.contains("key_first") && goText.contains("key_second"),
                "expected action-scoped arg locals;\n$goText",
            )
        } finally {
            dir.toFile().deleteRecursively()
            buildDir.deleteRecursively()
            File("DupArgBind").delete()
            File("DupArgBind.jar").delete()
        }
    }

    @Test
    fun clientlessServiceBoundArgIsNotUnusedGoLocal() {
        val dir = Files.createTempDirectory("go-unused-svc")
        val buildDir = File("UnusedSvcArg-jul-build")
        try {
            val src = dir.resolve("unused_svc.jul")
            src.writeText(
                """
                import julay.funlib.exitProgram

                static proc Hub {
                    var n : Int
                    constructor initially(args : List<String>) {
                        transit: n := 0
                    }
                    service transition report(count : Int) {
                        guard: count = n
                    }
                    internal transition exitSystem() {
                        guard: true
                        after: exitProgram(0)
                    }
                }
                proc UnusedSvcArg := Hub
                compile UnusedSvcArg
                """.trimIndent(),
            )
            // Must succeed go build (unused count previously broke compilation).
            compileJulFile(src, keepBuild = true)
            assertTrue(File("UnusedSvcArg").exists(), "expected UnusedSvcArg binary")
            val goText = readGo(buildDir)
            assertTrue(
                goText.contains("_ = count_report") || goText.contains("_ = count"),
                "hub-bound service arg should be referenced or discarded;\n$goText",
            )
        } finally {
            dir.toFile().deleteRecursively()
            buildDir.deleteRecursively()
            File("UnusedSvcArg").delete()
            File("UnusedSvcArg.jar").delete()
        }
    }

    private fun readGo(buildDir: File): String {
        if (!buildDir.exists()) fail("missing $buildDir")
        return buildDir.listFiles()?.filter { it.extension == "go" }
            ?.joinToString("\n") { it.readText() }
            ?: fail("no .go files in $buildDir")
    }
}
