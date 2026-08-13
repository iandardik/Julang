package julay.compiler.pass

import julay.compiler.prepareCheckedCompilation
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ParametricTypeValTest {

    @Test
    fun setLiteralInGuardEmitsSetTypeVal() {
        val dir = Files.createTempDirectory("julay-set-type-val")
        val file = dir.resolve("main.jul")
        file.toFile().writeText(
            """
            import julay.funlib.setOf

            proc Role {
                var state : String
                constructor initially(args : List<String>) {
                    transit: state := "Follower"
                }
                transition timeout() {
                    guard: state in setOf("Follower", "Candidate")
                    transit: state := "Candidate"
                }
            }
            compile Role
            """.trimIndent(),
        )
        val checked = prepareCheckedCompilation(file)
        assertNotNull(checked, "prepareCheckedCompilation failed")
        val codegen = codegenPass(
            checked.ast,
            checked.jarTargets.single(),
            checked.procDecls,
            checked.librariesInUse,
        )
        assertTrue(
            codegen.sourceText.contains("val setType_String = setType(stringType)"),
            "setOf string literal in a guard must declare setType_String;\n${codegen.sourceText.take(2500)}",
        )
        assertTrue(
            codegen.sourceText.contains("setType_String"),
            codegen.sourceText.take(2500),
        )
    }
}
