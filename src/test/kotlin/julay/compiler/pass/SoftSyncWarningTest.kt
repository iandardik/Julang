package julay.compiler.pass

import julay.compiler.analysis.buildAlphabetJsonDocument
import julay.compiler.analysis.resolveAnalyzeScope
import julay.compiler.ast.RootNode
import julay.compiler.prepareCheckedCompilation
import julay.compiler.runErrorAndWarningPasses
import julay.compiler.toStructuredDiagnostic
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SoftSyncWarningTest {

    @Test
    fun analyzeUnilateralSessionEmitsAlphabetWithoutSyncRequirement() {
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
        val ok = runErrorAndWarningPasses(
            checked.ast,
            components,
            checked.librariesInUse,
            requireCompleteSync = false,
        )
        assertTrue(ok, "incomplete session sync must not abort intermediate analyze")

        val intermediateWarnings = checked.ast.warningPass(
            components,
            checked.librariesInUse,
            requireCompleteSync = false,
        )
        assertFalse(
            intermediateWarnings.any { it.toString().contains("exactly two sync peers") },
            "intermediate analyze must not require session sync; got: $intermediateWarnings",
        )

        val compileWarnings = checked.ast.warningPass(
            components,
            checked.librariesInUse,
            requireCompleteSync = true,
        )
        assertTrue(
            compileWarnings.any { it.toString().contains("exactly two sync peers") },
            "top-level compile must still warn on unilateral session; got: $compileWarnings",
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

    @Test
    fun intermediateApiDanglingClientAnalyzesAlphabet() {
        val dir = Files.createTempDirectory("julay-intermediate-client")
        val file = dir.resolve("hb.jul")
        file.toFile().writeText(
            """
            proc HeartbeatImpl {
                constructor start() { transit: }
                client transition staleTerm(term : Int) { transit: }
            }
            api Heartbeat {
                proc: HeartbeatImpl
            }
            """.trimIndent(),
        )

        val checked = prepareCheckedCompilation(file)
        assertNotNull(checked)
        assertTrue(checked.jarTargets.isEmpty(), "no compile directive")

        val roots = julay.compiler.maximalCompositionRoots(
            checked.procDecls,
            checked.unit.entryDeclNames,
        )
        assertEquals(listOf("Heartbeat"), roots.map { it.name })
        val program = roots.single()
        val components = program.allProcNames(checked.procDecls)
        val ok = runErrorAndWarningPasses(
            checked.ast,
            components,
            checked.librariesInUse,
            program.name,
            program = program,
            procDecls = checked.procDecls,
            requireCompleteSync = false,
        )
        assertTrue(ok, "unmatched client must not abort intermediate analyze")

        val intermediateWarnings = checked.ast.warningPass(
            components,
            checked.librariesInUse,
            program,
            checked.procDecls,
            requireCompleteSync = false,
        )
        assertTrue(
            intermediateWarnings.none {
                it.toString().contains("not synchronized") ||
                    it.toString().contains("exactly two sync peers") ||
                    it.toString().contains("no `provider`")
            },
            "intermediate must not require complete sync; got: $intermediateWarnings",
        )

        val compileOk = runErrorAndWarningPasses(
            checked.ast,
            components,
            checked.librariesInUse,
            program.name,
            program = program,
            procDecls = checked.procDecls,
            requireCompleteSync = true,
        )
        assertFalse(compileOk, "direct compile must still reject unmatched client")

        val scope = resolveAnalyzeScope(
            scopeNames = listOf("Heartbeat"),
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
        assertTrue(json.contains("\"name\": \"staleTerm\""), json)
        assertTrue(json.contains("\"modifier\": \"client\""), json)
    }

    @Test
    fun tlaCompileAllowsPartialAlphabetsJarStillRejectsUnmatchedClient() {
        val dir = Files.createTempDirectory("julay-tla-partial-alphabet")
        val file = dir.resolve("partial.jul")
        file.toFile().writeText(
            """
            proc ClientOnly {
                constructor initially(args : List<String>) { transit: }
                client transition bump() { transit: }
            }
            proc OrdinarySolo {
                constructor initially(args : List<String>) { transit: }
                transition tick() { transit: }
            }
            spec ClientSpec := ClientOnly
            spec OrdinarySpec := OrdinarySolo
            compile ClientSpec, OrdinarySpec
            """.trimIndent(),
        )

        val cwd = java.io.File(".").canonicalFile
        val clientTla = java.io.File(cwd, "ClientSpec.tla")
        val clientCfg = java.io.File(cwd, "ClientSpec.cfg")
        val ordinaryTla = java.io.File(cwd, "OrdinarySpec.tla")
        val ordinaryCfg = java.io.File(cwd, "OrdinarySpec.cfg")
        listOf(clientTla, clientCfg, ordinaryTla, ordinaryCfg).forEach { it.delete() }

        try {
            julay.compiler.compileJulFile(file, keepBuild = false)
            assertTrue(clientTla.exists(), "expected ClientSpec.tla for unmatched client")
            assertTrue(clientCfg.exists(), "expected ClientSpec.cfg")
            assertTrue(ordinaryTla.exists(), "expected OrdinarySpec.tla for unsynced ordinary")
            assertTrue(ordinaryCfg.exists(), "expected OrdinarySpec.cfg")
        } finally {
            listOf(clientTla, clientCfg, ordinaryTla, ordinaryCfg).forEach { it.delete() }
        }

        // Same unmatched client as a JAR root must still fail alphabet integrity.
        val jarFile = dir.resolve("jar.jul")
        jarFile.toFile().writeText(
            """
            proc ClientOnly {
                constructor initially(args : List<String>) { transit: }
                client transition bump() { transit: }
            }
            proc P := ClientOnly
            compile P
            """.trimIndent(),
        )
        val checked = prepareCheckedCompilation(jarFile)
        assertNotNull(checked)
        val program = checked.jarTargets.single()
        val ok = runErrorAndWarningPasses(
            checked.ast,
            program.allProcNames(checked.procDecls),
            checked.librariesInUse,
            program.name,
            program = program,
            procDecls = checked.procDecls,
            requireCompleteSync = true,
        )
        assertFalse(ok, "JAR compile must still reject unmatched client")
    }

    @Test
    fun syncWarningsUnderlineSignatureLineOnly() {
        val dir = Files.createTempDirectory("julay-sync-warn-sig")
        val file = dir.resolve("solo.jul")
        file.toFile().writeText(
            """
            proc Core {
                constructor initially(args : List<String>) { transit: }
                provider transition bump() {
                    guard: true
                }
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
        val bump = warnings.map { it.toStructuredDiagnostic(file) }.filter {
            it.message.contains("bump") && it.message.contains("provider with no clients")
        }
        assertTrue(bump.isNotEmpty(), "expected bump warning; got: $warnings")
        assertEquals(bump[0].startLine, bump[0].endLine, "signature-only underline; got: $bump")
        assertEquals(3, bump[0].startLine, "bump signature is line 3; got: $bump")
    }
}
