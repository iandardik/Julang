package julay.compiler.pass

import julay.compiler.prepareCheckedCompilation
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ConstructorAfterStateTest {

    @Test
    fun constructorAfterReadsStateAfterSequentialAssigns() {
        val dir = Files.createTempDirectory("julay-ctor-after-state")
        val file = dir.resolve("main.jul")
        file.toFile().writeText(
            """
            import julay.funlib.exitProgram
            import julay.funlib.println

            proc S {
                var msg : String
                constructor initially(args : List<String>) {
                    transit:
                        msg := "hello"
                    after:
                        println(msg)
                        exitProgram(0)
                }
            }
            compile S
            """.trimIndent(),
        )
        try {
            val checked = prepareCheckedCompilation(file)
            assertNotNull(checked, "prepareCheckedCompilation failed")
            val codegen = codegenPass(
                checked.ast,
                checked.jarTargets.single(),
                checked.procDecls,
                checked.librariesInUse,
            )
            val finish = codegen.sourceText
                .substringAfter("finishConstruction")
                .substringBefore("override suspend fun actions")
            val assignIdx = finish.indexOf("msg = ")
            val afterArgIdx = finish.indexOf("__afterArg")
            assertTrue(assignIdx >= 0, "expected constructor assign in finishConstruction;\n$finish")
            assertTrue(afterArgIdx >= 0, "expected after: arg snapshot in finishConstruction;\n$finish")
            assertTrue(
                assignIdx < afterArgIdx,
                "constructor after: must snapshot state after sequential assigns;\n$finish",
            )
        } finally {
            dir.toFile().deleteRecursively()
        }
    }
}
