package julay.spec

import julay.compiler.checkJulFile
import julay.compiler.compileJulFile
import julay.compiler.loadCompilationUnit
import julay.compiler.pass.TypePassResult
import julay.compiler.pass.sortBearingDetail
import julay.compiler.pass.typePass
import julay.program.type.ObjClassType
import julay.program.type.SetType
import julay.program.type.DomainKind
import julay.program.type.DomainType
import julay.program.type.containsUninterpretedType
import julay.program.type.stringType
import julay.program.Variable
import java.io.File
import java.nio.file.Files
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ObjSortFieldTest {

    @Test
    fun objWithSortFieldTypeChecksAlone() {
        val result = typeCheck(
            """
            type NodeSet
            type VoteRequestMsg {
                dest : NodeSet
                msg : String
            }
            """.trimIndent(),
        )
        assertTrue(result.errors.isEmpty(), result.toString())
    }

    @Test
    fun leafSpecSortStateTypeChecks() {
        val result = typeCheck(
            """
            type Node
            spec E {
                Node := { "n1", "n2" }
                var d : Node := "n1"
                constructor initially(args : List<String>) { transit: d := "n1" }
            }
            """.trimIndent(),
        )
        assertTrue(result.errors.isEmpty(), result.toString())
    }

    @Test
    fun leafSpecSortFieldObjTypeChecks() {
        val result = typeCheck(
            """
            import julay.funlib.setOf
            type NodeSet
            type VoteRequestMsg {
                dest : NodeSet
                msg : String
            }
            spec Env {
                NodeSet := { "n1", "n2" }
                var msgs : Set<VoteRequestMsg> := setOf()
                constructor initially(args : List<String>) { transit: msgs := setOf() }
            }
            """.trimIndent(),
        )
        assertTrue(result.errors.isEmpty(), result.toString())
    }

    @Test
    fun procDirectSortStateStillErrors() {
        val result = typeCheck(
            """
            type Node
            proc P {
                var d : Node := "n1"
                constructor initially(args : List<String>) { transit: d := "n1" }
            }
            """.trimIndent(),
        )
        assertTrue(
            result.errors.any { it.toString().contains("can only be used as a spec or quantifier domain") },
            result.toString(),
        )
    }

    @Test
    fun jarIgnoresSortBearingObjsUsedOnlyBySpecs() {
        // Same shape as Raft sys.jul: JAR target + imported leaf-spec that pulls in
        // sort-bearing objs. Codegen must not try to emit those objs as Kotlin.
        val root = Files.createTempDirectory("julay-mixed-sort-objs")
        val net = root.resolve("net.jul")
        net.writeText(
            """
            import julay.funlib.setOf
            export type NodeSet
            type VoteRequestMsg {
                dest : NodeSet
                msg : String
            }
            export spec Net {
                NodeSet := { "n1", "n2" }
                var msgs : Set<VoteRequestMsg> := setOf()
                constructor initially(args : List<String>) {}
            }
            """.trimIndent(),
        )
        val protocol = root.resolve("protocol.jul")
        protocol.writeText(
            """
            import net.Net
            export spec Sys := Net
            """.trimIndent(),
        )
        val main = root.resolve("main.jul")
        main.writeText(
            """
            import protocol.Sys
            proc Peer {
                var ack : Boolean := false
                constructor initially(args : List<String>) {}
                transition ping() { transit: ack := true }
            }
            proc App := Peer
            compile App, Sys
            """.trimIndent(),
        )
        val cwd = java.io.File(".").absoluteFile
        val jar = java.io.File(cwd, "App.jar")
        val tla = java.io.File(cwd, "Sys.tla")
        val cfg = java.io.File(cwd, "Sys.cfg")
        jar.delete()
        tla.delete()
        cfg.delete()
        try {
            compileJulFile(main, keepBuild = false)
            assertTrue(jar.exists(), "JAR App should compile despite sort-bearing objs used only by specs")
            assertTrue(tla.exists() && cfg.exists(), "spec Sys should emit TLA")
        } finally {
            jar.delete()
            tla.delete()
            cfg.delete()
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun mixedJarSortObjsRegressionFixtureCompiles() {
        val source = File("regression/input/spec/mixedjarsort/main.jul")
        assertTrue(source.exists(), "missing ${source.path}")
        val cwd = java.io.File(".").absoluteFile
        val jar = java.io.File(cwd, "PeerApp.jar")
        val tla = java.io.File(cwd, "Sys.tla")
        val cfg = java.io.File(cwd, "Sys.cfg")
        jar.delete()
        tla.delete()
        cfg.delete()
        try {
            compileJulFile(source.toPath(), keepBuild = false)
            assertTrue(jar.exists(), "expected PeerApp.jar from mixedjarsort fixture")
            assertTrue(tla.exists() && cfg.exists(), "expected Sys.tla/cfg from mixedjarsort fixture")
        } finally {
            jar.delete()
            tla.delete()
            cfg.delete()
        }
    }

    @Test
    fun procSortBearingObjTypeChecksButJarRefused() {
        val source = """
            type NodeSet
            type VoteRequestMsg {
                dest : NodeSet
                msg : String
            }
            proc Bad {
                var m : VoteRequestMsg
                constructor initially(args : List<String>) {
                    transit: m := VoteRequestMsg { dest := "n1", msg := "x" }
                }
            }
            compile Bad
        """.trimIndent()
        val result = typeCheck(source)
        assertTrue(result.errors.isEmpty(), "type-check should allow sort-bearing obj on proc; got $result")

        val dir = Files.createTempDirectory("julay-obj-sort-jar")
        val file = dir.resolve("main.jul")
        file.writeText(source)
        val check = checkJulFile(file)
        assertTrue(
            check.diagnostics.any { it.message.contains("uninterpreted") && it.message.contains("Bad") },
            check.diagnostics.toString(),
        )

        val cwd = java.io.File(".").absoluteFile
        val jar = java.io.File(cwd, "Bad.jar")
        jar.delete()
        try {
            compileJulFile(file, keepBuild = false)
            assertFalse(jar.exists(), "JAR must not be produced for sort-bearing target")
        } finally {
            jar.delete()
            dir.toFile().deleteRecursively()
        }
    }

    @Test
    fun nestedReachabilityRefused() {
        val source = File("regression/input/spec/obj-sort-field-jar-refuse.jul")
        assertTrue(source.exists(), "missing ${source.path}")
        val check = checkJulFile(source.toPath())
        assertTrue(
            check.diagnostics.any {
                it.message.contains("uninterpreted") &&
                    (it.message.contains("Outer") || it.message.contains("Inner") || it.message.contains("Node"))
            },
            check.diagnostics.toString(),
        )
    }

    @Test
    fun specAndJarSameFile() {
        val source = """
            type NodeSet
            type Msg { dest : NodeSet }
            spec Good {
                NodeSet := { "n1" }
                var m : Msg
                constructor initially(args : List<String>) {
                    transit: m := Msg { dest := "n1" }
                }
            }
            proc Bad {
                var m : Msg
                constructor initially(args : List<String>) {
                    transit: m := Msg { dest := "n1" }
                }
            }
            compile Good, Bad
        """.trimIndent()
        val dir = Files.createTempDirectory("julay-obj-sort-mixed")
        val file = dir.resolve("main.jul")
        file.writeText(source)
        val cwd = java.io.File(".").absoluteFile
        val tla = java.io.File(cwd, "Good.tla")
        val cfg = java.io.File(cwd, "Good.cfg")
        val jar = java.io.File(cwd, "Bad.jar")
        listOf(tla, cfg, jar).forEach { it.delete() }
        try {
            compileJulFile(file, keepBuild = false)
            // prepareCheckedCompilation fails entirely if any jar target is sort-bearing,
            // so neither TLA nor JAR should be emitted when Bad is co-targeted.
            assertFalse(jar.exists(), "Bad.jar must not exist")
            // Spec-only compile of Good still works when Bad is not a compile target:
            file.writeText(
                """
                type NodeSet
                type Msg { dest : NodeSet }
                spec Good {
                    NodeSet := { "n1" }
                    var m : Msg
                    constructor initially(args : List<String>) {
                        transit: m := Msg { dest := "n1" }
                    }
                }
                compile Good
                """.trimIndent(),
            )
            compileJulFile(file, keepBuild = false)
            assertTrue(tla.exists(), "expected Good.tla")
            assertTrue(cfg.exists(), "expected Good.cfg")
        } finally {
            listOf(tla, cfg, jar).forEach { it.delete() }
            dir.toFile().deleteRecursively()
        }
    }

    @Test
    fun leafSpecParamIntoSortField() {
        val result = typeCheck(
            """
            type Node
            type Vote { dest : Node }
            spec Net[n : Node] {
                Node := { "n1", "n2" }
                var last : Vote
                constructor initially(args : List<String>) {
                    transit: last := Vote { dest := n }
                }
            }
            """.trimIndent(),
        )
        assertTrue(result.errors.isEmpty(), result.toString())
    }

    @Test
    fun fixtureCompilesToTla() {
        val source = java.io.File("regression/input/spec/obj-sort-field.jul")
        assertTrue(source.exists(), "missing ${source.path}")
        val cwd = java.io.File(".").absoluteFile
        val tla = java.io.File(cwd, "Env.tla")
        val cfg = java.io.File(cwd, "Env.cfg")
        val jar = java.io.File(cwd, "Env.jar")
        listOf(tla, cfg, jar).forEach { it.delete() }
        try {
            compileJulFile(source.toPath(), keepBuild = false)
            assertTrue(tla.exists(), "expected Env.tla")
            assertTrue(cfg.exists(), "expected Env.cfg")
            assertFalse(jar.exists(), "spec must not produce JAR")
            val cfgText = cfg.readText()
            assertTrue(cfgText.contains("NodeSet"), "expected NodeSet in cfg;\n$cfgText")
        } finally {
            listOf(tla, cfg, jar).forEach { it.delete() }
        }
    }

    @Test
    fun sortBearingDetailNamesFieldAndSort() {
        val node = DomainType("Node", DomainKind.Uninterpreted, stringType, listOf("\"n1\""))
        val inner = ObjClassType(
            "Inner",
            listOf(Variable("d", node)),
            { _, _ -> throw UnsupportedOperationException() },
            { _, _ -> throw UnsupportedOperationException() },
        )
        assertTrue(inner.containsUninterpretedType())
        val detail = sortBearingDetail(inner)
        assertTrue(detail != null && detail.contains("d") && detail.contains("Node"), detail)
        val setDetail = sortBearingDetail(SetType(inner))
        assertTrue(setDetail != null && setDetail.startsWith("Set<"), setDetail)
    }

    private fun typeCheck(source: String): TypePassResult {
        val (unit, loadErrors) = loadSource(source)
        assertTrue(loadErrors.isEmpty(), loadErrors.toString())
        return unit.root.typePass(unit)
    }

    private fun loadSource(source: String): Pair<julay.compiler.CompilationUnit, List<julay.compiler.CompileError>> {
        val dir = Files.createTempDirectory("julay-obj-sort")
        val file = dir.resolve("main.jul")
        file.writeText(source)
        return loadCompilationUnit(file)
    }
}
