package julay.spec

import julay.compiler.compileJulFile
import julay.compiler.loadCompilationUnit
import julay.compiler.pass.typePass
import java.nio.file.Files
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertTrue

class SortImportTest {

    @Test
    fun exportedSortCanBeImportedAndUsedAsIndex() {
        val root = Files.createTempDirectory("julay-sort-import")
        val modDir = root.resolve("mod")
        modDir.createDirectories()
        modDir.resolve("nodes.jul").writeText(
            """
            export sort NodeSet := { "n1", "n2" }
            """.trimIndent(),
        )
        val main = root.resolve("main.jul")
        main.writeText(
            """
            import mod.nodes.NodeSet
            proc Counter {
                var n : Int
                constructor initially(args : List<String>) { transit: n := 0 }
                transition bump() { transit: n := n + 1 }
            }
            spec S := Counter[i : NodeSet]
            """.trimIndent(),
        )
        val (unit, loadErrors) = loadCompilationUnit(main)
        assertTrue(loadErrors.isEmpty(), loadErrors.toString())
        val result = unit.root.typePass(unit)
        assertTrue(result.errors.isEmpty(), result.toString())
        assertTrue("NodeSet" in unit.importTable.shortNames, unit.importTable.toString())
    }

    @Test
    fun privateSortCannotBeImported() {
        val root = Files.createTempDirectory("julay-sort-private")
        val modDir = root.resolve("mod")
        modDir.createDirectories()
        modDir.resolve("nodes.jul").writeText(
            """
            sort NodeSet := { "n1", "n2" }
            export proc Dummy {
                constructor initially(args : List<String>) {}
            }
            """.trimIndent(),
        )
        val main = root.resolve("main.jul")
        main.writeText(
            """
            import mod.nodes.NodeSet
            proc Counter {
                var n : Int
                constructor initially(args : List<String>) { transit: n := 0 }
            }
            spec S := Counter[i : NodeSet]
            """.trimIndent(),
        )
        val (_, loadErrors) = loadCompilationUnit(main)
        assertTrue(
            loadErrors.any { it.toString().contains("no export named \"NodeSet\"") },
            loadErrors.toString(),
        )
    }

    @Test
    fun sortImportFixtureCompilesToTla() {
        val source = java.io.File("regression/input/spec/sort-import.jul")
        assertTrue(source.exists(), "missing ${source.path}")
        val cwd = java.io.File(".").absoluteFile
        val tla = java.io.File(cwd, "SortImport.tla")
        val cfg = java.io.File(cwd, "SortImport.cfg")
        tla.delete()
        cfg.delete()
        try {
            compileJulFile(
                source.toPath(),
                keepBuild = false,
                extraLibraryPaths = listOf(java.io.File("regression/input").toPath()),
            )
            assertTrue(tla.exists(), "expected SortImport.tla")
            assertTrue(cfg.exists(), "expected SortImport.cfg")
            val cfgText = cfg.readText()
            assertTrue(
                cfgText.contains("NodeSet") && cfgText.contains("n1"),
                "expected NodeSet constant assignment;\n$cfgText",
            )
        } finally {
            tla.delete()
            cfg.delete()
        }
    }
}
