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
                import julay.funlib.exitProgram
                import julay.funlib.println

                static proc S {
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
                            exitProgram(0)
                    }
                }
                static proc T {
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
            val goText = buildDir.listFiles()
                ?.filter { it.extension == "go" }
                ?.joinToString("\n") { it.readText() }
                ?: ""
            assertTrue(goText.contains("handoff"), "expected generated source;\n$goText")
            assertFalse(
                goText.contains("+ \"\"") || goText.contains("+\"\""),
                "expected empty-string concat to be elided from guards;\n$goText",
            )
            assertTrue(
                goText.contains("julay.ToStringOf") ||
                    goText.contains("strconv.") ||
                    goText.contains("fmt."),
                "expected Go string coerce embedding;\n$goText",
            )
        } finally {
            dir.toFile().deleteRecursively()
            buildDir.deleteRecursively()
            File("CoerceElide.jar").delete()
            File("CoerceElide").delete()
        }
    }

    @Test
    fun nestedArithmeticEmptyConcatInGuard() {
        val dir = Files.createTempDirectory("nested-arith-empty-concat-guard")
        val buildDir = File("NestedArithCoerce-jul-build")
        try {
            val src = dir.resolve("nested.jul")
            src.writeText(
                """
                import julay.funlib.exitProgram
                import julay.funlib.println

                static proc S {
                    var n : Int
                    var done : Boolean
                    constructor initially(args : List<String>) {
                        transit:
                            n := 1
                            done := false
                    }
                    transition handoff(msg : String) {
                        guard: ~done & (msg = (n + 100) + "")
                        transit:
                            done := true
                        after:
                            println(msg)
                    }
                    internal transition exitSystem() {
                        guard: done
                        after:
                            exitProgram(0)
                    }
                }
                static proc T {
                    constructor initially(args : List<String>) {
                        transit:
                    }
                    transition handoff(msg : String) {
                        guard: msg = "101"
                    }
                }
                proc NestedArithCoerce := S || T
                compile NestedArithCoerce
                """.trimIndent(),
            )
            compileJulFile(src, keepBuild = true)
            assertTrue(buildDir.exists(), "expected $buildDir after keepBuild compile")
            val goText = buildDir.listFiles()
                ?.filter { it.extension == "go" }
                ?.joinToString("\n") { it.readText() }
                ?: ""
            assertTrue(goText.contains("handoff"), "expected generated source;\n$goText")
            assertFalse(
                goText.contains("+ \"\"") || goText.contains("+\"\""),
                "expected empty-string concat to be elided from guards;\n$goText",
            )
            assertTrue(
                goText.contains("julay.ToStringOf") ||
                    goText.contains("strconv.") ||
                    goText.contains("fmt."),
                "expected nested arithmetic Go string coerce;\n$goText",
            )
        } finally {
            dir.toFile().deleteRecursively()
            buildDir.deleteRecursively()
            File("NestedArithCoerce.jar").delete()
            File("NestedArithCoerce").delete()
        }
    }
}
