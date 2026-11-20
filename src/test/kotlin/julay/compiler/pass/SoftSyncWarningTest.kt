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
    fun analyzeUnilateralOrdinaryEmitsAlphabetWithoutSyncRequirement() {
        val dir = Files.createTempDirectory("julay-soft-sync-analyze")
        val file = dir.resolve("solo.jul")
        file.toFile().writeText(
            """
            static proc Solo {
                constructor initially(args : List<String>) { transit: }
                transition ping() { transit: }
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
        assertTrue(ok, "incomplete ordinary sync must not abort intermediate analyze")

        val intermediateWarnings = checked.ast.warningPass(
            components,
            checked.librariesInUse,
            requireCompleteSync = false,
        )
        assertFalse(
            intermediateWarnings.any {
                it.toString().contains("not synchronized with any peer") ||
                    it.toString().contains("tag it `internal`")
            },
            "intermediate analyze must not require ordinary sync; got: $intermediateWarnings",
        )

        val compileErrors = checked.ast.errorPass(
            components,
            checked.librariesInUse,
            requireCompleteSync = true,
            program = checked.procDecls.firstOrNull { it.name == "P" },
            procDecls = checked.procDecls,
        )
        assertTrue(
            compileErrors.any {
                it.toString().contains("not synchronized with any peer") ||
                    it.toString().contains("tag it `internal`")
            },
            "top-level compile must error on unilateral ordinary; got: $compileErrors",
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
    fun jarCompileUnilateralOrdinaryErrors() {
        val dir = Files.createTempDirectory("julay-soft-sync-jar")
        val file = dir.resolve("solo.jul")
        file.toFile().writeText(
            """
            static proc Solo {
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
        assertFalse(ok, "unsynced ordinary must abort JAR compile checks")

        val errors = checked.ast.errorPass(
            components,
            checked.librariesInUse,
            program,
            checked.procDecls,
        )
        assertTrue(
            errors.any { it.toString().contains("not synchronized with any peer") },
            "expected unsynced-ordinary error; got: $errors",
        )
    }

    @Test
    fun serviceWithNoClientsStillWarnsDeadlock() {
        val dir = Files.createTempDirectory("julay-service-no-clients")
        val file = dir.resolve("solo.jul")
        file.toFile().writeText(
            """
            static proc Core {
                constructor initially(args : List<String>) { transit: }
                service transition bump() { transit: }
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
            warnings.any { it.toString().contains("service with no clients") },
            "expected service-deadlock warning; got: $warnings",
        )
    }

    @Test
    fun tlaCompileRequiresUnsyncedAllowlistForConstructor() {
        val dir = Files.createTempDirectory("julay-tla-unsynced-ctor")
        val file = dir.resolve("ctor.jul")
        file.toFile().writeText(
            """
            proc Box {
                var n : Int
                constructor start() { transit: n := 0 }
                transition tick() { transit: n := n + 1 }
                return done() { guard: n >= 0 }
            }
            spec Allowed := Box {
                unsynced start, tick, done
            }
            spec MissingCtor := Box {
                unsynced tick, done
            }
            compile Allowed, MissingCtor
            """.trimIndent(),
        )

        val cwd = java.io.File(".").canonicalFile
        val allowed = java.io.File(cwd, "Allowed.tla")
        val missing = java.io.File(cwd, "MissingCtor.tla")
        listOf(allowed, missing).forEach { it.delete() }
        try {
            julay.compiler.compileJulFile(file, keepBuild = false)
            assertTrue(allowed.exists(), "expected Allowed.tla when start listed in unsynced")
            assertFalse(missing.exists(), "MissingCtor must fail without unsynced start")
        } finally {
            listOf(allowed, missing).forEach { it.delete() }
            listOf("Allowed.cfg", "MissingCtor.cfg").forEach { java.io.File(cwd, it).delete() }
        }
    }

    @Test
    fun tlaCompileRequiresUnsyncedAllowlistForOrdinary() {
        val dir = Files.createTempDirectory("julay-tla-unsynced")
        val file = dir.resolve("partial.jul")
        file.toFile().writeText(
            """
            static proc ClientOnly {
                constructor initially(args : List<String>) { transit: }
                client transition bump() { transit: }
            }
            static proc OrdinarySolo {
                constructor initially(args : List<String>) { transit: }
                transition tick() { transit: }
            }
            spec ClientSpec := ClientOnly
            spec OrdinarySpec := OrdinarySolo {
                unsynced tick
            }
            spec OrdinaryMissing := OrdinarySolo
            compile ClientSpec, OrdinarySpec, OrdinaryMissing
            """.trimIndent(),
        )

        val cwd = java.io.File(".").canonicalFile
        val clientTla = java.io.File(cwd, "ClientSpec.tla")
        val ordinaryTla = java.io.File(cwd, "OrdinarySpec.tla")
        val missingTla = java.io.File(cwd, "OrdinaryMissing.tla")
        listOf(clientTla, ordinaryTla, missingTla).forEach { it.delete() }

        try {
            julay.compiler.compileJulFile(file, keepBuild = false)
            assertTrue(clientTla.exists(), "expected ClientSpec.tla for unmatched client")
            assertTrue(ordinaryTla.exists(), "expected OrdinarySpec.tla when listed in unsynced")
            assertFalse(missingTla.exists(), "OrdinaryMissing must fail without unsynced tick")
        } finally {
            listOf(clientTla, ordinaryTla, missingTla).forEach { it.delete() }
            listOf("ClientSpec.cfg", "OrdinarySpec.cfg", "OrdinaryMissing.cfg").forEach {
                java.io.File(cwd, it).delete()
            }
        }

        // Same unmatched client as a JAR root must still fail alphabet integrity.
        val jarFile = dir.resolve("jar.jul")
        jarFile.toFile().writeText(
            """
            static proc ClientOnly {
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
    fun tlaUnsyncedListedButSyncedIsError() {
        val dir = Files.createTempDirectory("julay-tla-unsynced-stale")
        val file = dir.resolve("stale.jul")
        file.toFile().writeText(
            """
            static proc A {
                constructor initially(args : List<String>) { transit: }
                transition ping() { transit: }
            }
            static proc B {
                constructor initially(args : List<String>) { transit: }
                transition ping() { transit: }
            }
            spec Synced := A || B {
                unsynced ping
            }
            compile Synced
            """.trimIndent(),
        )

        val cwd = java.io.File(".").canonicalFile
        val tla = java.io.File(cwd, "Synced.tla")
        val cfg = java.io.File(cwd, "Synced.cfg")
        tla.delete()
        cfg.delete()
        try {
            julay.compiler.compileJulFile(file, keepBuild = false)
            assertFalse(tla.exists(), "listed-but-synced unsynced must fail compile")
        } finally {
            tla.delete()
            cfg.delete()
        }
    }

    @Test
    fun tlaUnsyncedUnknownNameIsError() {
        val dir = Files.createTempDirectory("julay-tla-unsynced-typo")
        val file = dir.resolve("typo.jul")
        file.toFile().writeText(
            """
            static proc Solo {
                constructor initially(args : List<String>) { transit: }
                transition tick() { transit: }
            }
            spec S := Solo {
                unsynced tick, noSuchAct
            }
            compile S
            """.trimIndent(),
        )

        val cwd = java.io.File(".").canonicalFile
        val tla = java.io.File(cwd, "S.tla")
        val cfg = java.io.File(cwd, "S.cfg")
        tla.delete()
        cfg.delete()
        try {
            julay.compiler.compileJulFile(file, keepBuild = false)
            assertFalse(tla.exists(), "unknown unsynced name must fail compile")
        } finally {
            tla.delete()
            cfg.delete()
        }
    }

    @Test
    fun syncWarningsUnderlineSignatureLineOnly() {
        val dir = Files.createTempDirectory("julay-sync-warn-sig")
        val file = dir.resolve("solo.jul")
        file.toFile().writeText(
            """
            static proc Core {
                constructor initially(args : List<String>) { transit: }
                service transition bump() {
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
            it.message.contains("bump") && it.message.contains("service with no clients")
        }
        assertTrue(bump.isNotEmpty(), "expected bump warning; got: $warnings")
        assertEquals(bump[0].startLine, bump[0].endLine, "signature-only underline; got: $bump")
        assertEquals(3, bump[0].startLine, "bump signature is line 3; got: $bump")
    }
}
