package julay.compiler.pass

import julay.compiler.analysis.buildAlphabetJsonDocument
import julay.compiler.analysis.resolveAnalyzeScope
import julay.compiler.ast.RootNode
import julay.compiler.prepareCheckedCompilation
import julay.compiler.runErrorAndWarningPasses
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SoftSyncWarningTest {

    @Test
    fun analyzeUnilateralSessionWarnsButEmitsAlphabet() {
        val dir = Files.createTempDirectory("julay-soft-sync-analyze")
        val file = dir.resolve("solo.jul")
        file.toFile().writeText(
            """
            proc Solo {
                constructor initially(args : List<String>) { transit: }
                session transition ping() { transit: }
            }
            proc P := Solo
            """.trimIndent(),
        )

        val checked = prepareCheckedCompilation(file)
        assertNotNull(checked)

        val components = checked.unit.allPClassNames + checked.librariesInUse
        val ok = runErrorAndWarningPasses(checked.ast, components, checked.librariesInUse)
        assertTrue(ok, "missing sync peers must not abort analyze")

        val warnings = checked.ast.warningPass(components, checked.librariesInUse)
        assertTrue(
            warnings.any { it.toString().contains("exactly two sync peers") },
            "expected peer-count warning; got: $warnings",
        )

        val scope = resolveAnalyzeScope(
            scopeNames = listOf("P"),
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
        assertTrue(json.contains("\"name\": \"P\""), json)
        assertTrue(json.contains("\"name\": \"ping\""), json)
    }

    @Test
    fun jarCompileUnilateralOrdinaryWarnsButDoesNotError() {
        val dir = Files.createTempDirectory("julay-soft-sync-jar")
        val file = dir.resolve("solo.jul")
        file.toFile().writeText(
            """
            proc Solo {
                constructor initially(args : List<String>) { transit: }
                transition ping() { transit: }
            }
            proc P := Solo
            compile P
            """.trimIndent(),
        )

        val checked = prepareCheckedCompilation(file)
        assertNotNull(checked)
        assertFalse(checked.jarTargets.isEmpty())

        val program = checked.jarTargets.single()
        val components = program.allProcNames(checked.procDecls)
        val ok = runErrorAndWarningPasses(
            checked.ast,
            components,
            checked.librariesInUse,
            program.name,
            program = program,
            procDecls = checked.procDecls,
        )
        assertTrue(ok, "unsynced ordinary must not abort JAR compile checks")

        val warnings = checked.ast.warningPass(
            components,
            checked.librariesInUse,
            program,
            checked.procDecls,
        )
        assertTrue(
            warnings.any { it.toString().contains("not synchronized with any peer") },
            "expected unsynced-ordinary warning; got: $warnings",
        )
    }

    @Test
    fun providerClientsOnFoldedProcfunDoNotWarnDeadlock() {
        val dir = Files.createTempDirectory("julay-provider-procfun-clients")
        val file = dir.resolve("sync.jul")
        file.toFile().writeText(
            """
            procfun helper() : Int {
                client transition bump() {
                    return: 0
                }
            }
            proc Host {
                var out : Int
                constructor initially(args : List<String>) {
                    transit: out := helper()
                }
            }
            proc Core {
                constructor initially(args : List<String>) { transit: }
                provider transition bump() { transit: }
            }
            proc P := Host || Core
            compile P
            """.trimIndent(),
        )

        val checked = prepareCheckedCompilation(file)
        assertNotNull(checked)
        val program = checked.jarTargets.single()
        val components = program.allProcNames(checked.procDecls)
        val warnings = checked.ast.warningPass(
            components,
            checked.librariesInUse,
            program,
            checked.procDecls,
        )
        assertFalse(
            warnings.any { it.toString().contains("provider with no clients") },
            "folded procfun clients must count; got: $warnings",
        )
    }

    @Test
    fun providerWithNoClientsStillWarnsDeadlock() {
        val dir = Files.createTempDirectory("julay-provider-no-clients")
        val file = dir.resolve("solo.jul")
        file.toFile().writeText(
            """
            proc Core {
                constructor initially(args : List<String>) { transit: }
                provider transition bump() { transit: }
            }
            proc P := Core
            compile P
            """.trimIndent(),
        )

        val checked = prepareCheckedCompilation(file)
        assertNotNull(checked)
        val program = checked.jarTargets.single()
        val components = program.allProcNames(checked.procDecls)
        val warnings = checked.ast.warningPass(
            components,
            checked.librariesInUse,
            program,
            checked.procDecls,
        )
        assertTrue(
            warnings.any { it.toString().contains("provider with no clients") },
            "expected provider-deadlock warning; got: $warnings",
        )
    }
}
