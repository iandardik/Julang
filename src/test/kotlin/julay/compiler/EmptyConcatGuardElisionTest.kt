package julay.compiler

import java.io.File
import java.nio.file.Files
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EmptyConcatGuardElisionTest {
    @Test
    fun emptyStringConcatElidedInGeneratedGuard() {
        val dir = Files.createTempDirectory("empty-concat-guard")
        val buildDir = File("CoerceElide-jul-build")
        try {
            val src = dir.resolve("coerce.jul")
            src.writeText(
                """
                import julay.funlib.exitProcess
                import julay.funlib.println

                proc S {
                    var n : Int
                    var done : Boolean
                    constructor initially(args : List<String>) {
                        transit:
                            n := 1
                            done := false
                    }
                    transition handoff(msg : String) {
                        guard: ~done & (msg = n + "")
                        transit:
                            done := true
                        after:
                            println(msg)
                    }
                    internal transition exitSystem() {
                        guard: done
                        after:
                            exitProcess()
                    }
                }
                proc T {
                    constructor initially(args : List<String>) {
                        transit:
                    }
                    transition handoff(msg : String) {
                        guard: msg = "1"
                    }
                }
                proc CoerceElide := S || T
                compile CoerceElide
                """.trimIndent(),
            )
            compileJulFile(src, keepBuild = true)
            assertTrue(buildDir.exists(), "expected $buildDir after keepBuild compile")
            val ktText = buildDir.listFiles()
                ?.filter { it.extension == "kt" }
                ?.joinToString("\n") { it.readText() }
                ?: ""
            assertTrue(ktText.contains("handoff"), "expected generated source;\n$ktText")
            assertFalse(
                ktText.contains("mkConcat"),
                "expected empty-string concat to be elided (no mkConcat) from guards;\n$ktText",
            )
            assertTrue(
                ktText.contains("n.toString()") || ktText.contains("SyncTerm.ToString"),
                "expected string coerce embedding;\n$ktText",
            )
        } finally {
            dir.toFile().deleteRecursively()
            buildDir.deleteRecursively()
            File("CoerceElide.jar").delete()
        }
    }
}
