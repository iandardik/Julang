package julay.compiler.analysis

import julay.compiler.ast.RootNode
import julay.compiler.prepareCheckedCompilation
import java.nio.file.Files
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AlphabetJsonTest {

    @Test
    fun termTest1JsonSeparatesExternalHiddenAndSourceInternal() {
        val projectRoot = java.io.File(".").canonicalFile
        val source = projectRoot.toPath().resolve("regression/input/basic/test1.jul")
        val checked = prepareCheckedCompilation(source)
        assertNotNull(checked)

        val scope = resolveAnalyzeScope(
            scopeNames = listOf("TermTest1"),
            procDecls = checked.procDecls,
            allPClassNames = checked.unit.allPClassNames,
            allProcAliasNames = checked.unit.allProcNames,
            librariesInUse = checked.librariesInUse,
        )
        assertNotNull(scope)

        val json = buildAlphabetJsonDocument(
            ast = checked.ast as RootNode,
            scope = scope,
            librariesInUse = checked.librariesInUse,
            procDecls = checked.procDecls,
        )

        assertTrue(json.contains("\"name\": \"TermTest1\""), json)
        // After S || T sync, increment is composition-hidden with peers S and T.
        assertTrue(json.contains("\"compositionHidden\""), json)
        assertTrue(json.contains("\"name\": \"increment\""), json)
        assertTrue(json.contains("\"pclassKey\": \"S\""), json)
        assertTrue(json.contains("\"pclassKey\": \"T\""), json)
        assertTrue(json.contains("\"channelKey\": \"TermTest1_1#increment\""), json)
        // Source-internal solo steps on S
        assertTrue(json.contains("\"sourceInternal\""), json)
        assertTrue(json.contains("\"name\": \"println\""), json)
        assertTrue(json.contains("\"name\": \"exitSystem\""), json)
        // External still has initially constructors, but not increment.
        val externalIdx = json.indexOf("\"external\"")
        val sourceInternalIdx = json.indexOf("\"sourceInternal\"")
        assertTrue(externalIdx >= 0 && sourceInternalIdx > externalIdx, json)
        val externalBlock = json.substring(externalIdx, sourceInternalIdx)
        assertTrue(
            !externalBlock.contains("\"name\": \"increment\""),
            "increment should not be external:\n$externalBlock",
        )
        assertTrue(externalBlock.contains("\"name\": \"initially\""), externalBlock)
    }

    @Test
    fun leafSShowsIncrementAsExternal() {
        val dir = Files.createTempDirectory("julay-alphabet-json")
        val file = dir.resolve("main.jul")
        file.writeText(
            """
            import julay.funlib.exitProcess
            import julay.funlib.println

            proc S {
                var x : Int
                var print : Boolean

                constructor initially(args : List<String>) {
                    transit:
                        x := 0
                        print := true
                }

                transition increment(inc : Int) {
                    guard: ~print & (x <= 10) & (inc > 0)
                    transit:
                        x := x + inc
                        print := true
                }

                internal transition println(msg : String) {
                    guard: print & (msg = x + "")
                    transit: print := false
                    after: println(msg)
                }
            }

            compile S
            """.trimIndent(),
        )
        val checked = prepareCheckedCompilation(file)
        assertNotNull(checked)
        val scope = resolveAnalyzeScope(
            scopeNames = listOf("S"),
            procDecls = checked.procDecls,
            allPClassNames = checked.unit.allPClassNames,
            allProcAliasNames = checked.unit.allProcNames,
            librariesInUse = checked.librariesInUse,
        )
        assertNotNull(scope)
        val json = buildAlphabetJsonDocument(
            checked.ast as RootNode,
            scope,
            checked.librariesInUse,
            checked.procDecls,
        )
        assertTrue(json.contains("\"name\": \"increment\""), json)
        assertTrue(json.contains("\"args\": [\"inc: Int\"]"), json)
        assertTrue(json.contains("\"modifier\": \"ordinary\""), json)
        assertTrue(json.contains("\"name\": \"println\""), json)
        assertTrue(json.contains("\"sourceInternal\": true"), json)
    }
}
