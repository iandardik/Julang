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
        // Top-level sync diagram: S — T labeled increment
        assertTrue(json.contains("\"compositionGraph\""), json)
        assertTrue(json.contains("\"nodes\": [\"S\", \"T\"]"), json)
        assertTrue(json.contains("\"a\": \"S\""), json)
        assertTrue(json.contains("\"b\": \"T\""), json)
        assertTrue(json.contains("\"actions\": [\"increment\"]"), json)
    }

    @Test
    fun specAliasAndLeafSpecTopLevelGraphShowsSyncEdges() {
        // Mirrors input/raft/specs/protocol.jul: ClusterSpec := Protocol[n], Sys := ClusterSpec[n] || Net[n].
        // Net is a leaf spec; omitting it from procClassPass left Sys with nodes but no edges.
        val dir = Files.createTempDirectory("julay-leaf-spec-sync-graph")
        val file = dir.resolve("main.jul")
        file.writeText(
            """
            type Node

            static proc Protocol {
                constructor initially(args : List<String>) { transit: }
                transition requestVote() { transit: }
                service transition handleVote() { transit: }
            }

            spec Net[n : Node] {
                Node := { "n1", "n2" }
                constructor initially(args : List<String>) {}
                transition requestVote() { transit: }
                client transition handleVote() { transit: }
            }

            spec ClusterSpec := Protocol[n : Node]
            spec Sys := with (n : Node) {
                ClusterSpec[n] || Net[n]
            }
            compile Sys
            """.trimIndent(),
        )
        val checked = prepareCheckedCompilation(file)
        assertNotNull(checked)
        val scope = resolveAnalyzeScope(
            scopeNames = listOf("Sys"),
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
        val graphIdx = json.indexOf("\"compositionGraph\"")
        val externalIdx = json.indexOf("\"external\"")
        assertTrue(graphIdx >= 0 && externalIdx > graphIdx, json)
        val graphBlock = json.substring(graphIdx, externalIdx)
        assertTrue(
            graphBlock.contains("\"nodes\": [\"ClusterSpec\", \"Net\"]") ||
                graphBlock.contains("\"nodes\": [\"Net\", \"ClusterSpec\"]"),
            "Sys diagram should show ClusterSpec and Net:\n$graphBlock",
        )
        assertTrue(
            graphBlock.contains("\"a\": \"ClusterSpec\"") || graphBlock.contains("\"b\": \"ClusterSpec\""),
            graphBlock,
        )
        assertTrue(
            graphBlock.contains("\"a\": \"Net\"") || graphBlock.contains("\"b\": \"Net\""),
            graphBlock,
        )
        assertTrue(
            graphBlock.contains("\"requestVote\""),
            "ordinary sync requestVote must label the ClusterSpec—Net edge:\n$graphBlock",
        )
        assertTrue(
            graphBlock.contains("\"handleVote\""),
            "service/client handleVote must label the ClusterSpec—Net edge:\n$graphBlock",
        )

        val hiddenIdx = json.indexOf("\"compositionHidden\"")
        assertTrue(hiddenIdx >= 0, json)
        val hiddenBlock = json.substring(hiddenIdx)
        assertTrue(hiddenBlock.contains("\"name\": \"requestVote\""), hiddenBlock)
        assertTrue(hiddenBlock.contains("\"name\": \"handleVote\""), hiddenBlock)
    }

    @Test
    fun leafSpecScopeListsItsActions() {
        val dir = Files.createTempDirectory("julay-leaf-spec-alphabet")
        val file = dir.resolve("main.jul")
        file.writeText(
            """
            type Node
            spec Net[n : Node] {
                Node := { "n1" }
                constructor initially(args : List<String>) {}
                transition requestVote() { transit: }
            }
            compile Net
            """.trimIndent(),
        )
        val checked = prepareCheckedCompilation(file)
        assertNotNull(checked)
        val scope = resolveAnalyzeScope(
            scopeNames = listOf("Net"),
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
        assertTrue(json.contains("\"name\": \"requestVote\""), "leaf spec actions must appear:\n$json")
    }

    @Test
    fun nestedAssembliesGraphOnlyShowsTopLevelSync() {
        val dir = Files.createTempDirectory("julay-sync-graph")
        val file = dir.resolve("main.jul")
        file.writeText(
            """
            static proc A {
                constructor initially(args : List<String>) { transit: }
                transition inner() { transit: }
                transition outer() { transit: }
            }
            static proc B {
                constructor initially(args : List<String>) { transit: }
                transition inner() { transit: }
            }
            static proc C {
                constructor initially(args : List<String>) { transit: }
                transition outer() { transit: }
            }
            proc X := A || B
            proc Y := C
            proc Z := X || Y
            compile Z
            """.trimIndent(),
        )
        val checked = prepareCheckedCompilation(file)
        assertNotNull(checked)
        val scope = resolveAnalyzeScope(
            scopeNames = listOf("Z"),
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
        // Z's top-level children are X and Y; they sync on outer (A's outer meets C).
        assertTrue(json.contains("\"nodes\": [\"X\", \"Y\"]"), json)
        assertTrue(json.contains("\"a\": \"X\""), json)
        assertTrue(json.contains("\"b\": \"Y\""), json)
        assertTrue(json.contains("\"actions\": [\"outer\"]"), json)
        // Nested sync inner (A||B) must not appear as a Z-level edge action list alone wrongly —
        // the Z graph edges should not include inner.
        val graphIdx = json.indexOf("\"compositionGraph\"")
        val externalIdx = json.indexOf("\"external\"")
        assertTrue(graphIdx >= 0 && externalIdx > graphIdx, json)
        val graphBlock = json.substring(graphIdx, externalIdx)
        assertTrue(!graphBlock.contains("\"inner\""), "nested inner must not be a Z edge:\n$graphBlock")
    }

    @Test
    fun providerClientTopLevelGraphShowsEdge() {
        val dir = Files.createTempDirectory("julay-provider-client-graph")
        val file = dir.resolve("main.jul")
        file.writeText(
            """
            static proc Hub {
                constructor initially(args : List<String>) { transit: }
                service transition w() { transit: }
            }
            static proc Cli {
                constructor initially(args : List<String>) { transit: }
                client transition w() { transit: }
            }
            proc P := Hub || Cli
            compile P
            """.trimIndent(),
        )
        val checked = prepareCheckedCompilation(file)
        assertNotNull(checked)
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
        assertTrue(json.contains("\"nodes\": [\"Hub\", \"Cli\"]"), json)
        assertTrue(json.contains("\"a\": \"Cli\""), json)
        assertTrue(json.contains("\"b\": \"Hub\""), json)
        assertTrue(json.contains("\"actions\": [\"w\"]"), json)
    }

    @Test
    fun multipleClientsDoNotSyncWithEachOtherOnProviderAction() {
        // Hub || CliA || CliB: each client meets Hub; clients must not share a graph edge
        // or a compositionHidden peer list with each other.
        val dir = Files.createTempDirectory("julay-multi-client-graph")
        val file = dir.resolve("main.jul")
        file.writeText(
            """
            static proc Hub {
                constructor initially(args : List<String>) { transit: }
                service transition staleTerm(term : Int) { transit: }
            }
            static proc CliA {
                constructor initially(args : List<String>) { transit: }
                client transition staleTerm(term : Int) { transit: }
            }
            static proc CliB {
                constructor initially(args : List<String>) { transit: }
                client transition staleTerm(term : Int) { transit: }
            }
            proc P := Hub || CliA || CliB
            compile P
            """.trimIndent(),
        )
        val checked = prepareCheckedCompilation(file)
        assertNotNull(checked)
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
        val graphIdx = json.indexOf("\"compositionGraph\"")
        val externalIdx = json.indexOf("\"external\"")
        assertTrue(graphIdx >= 0 && externalIdx > graphIdx, json)
        val graphBlock = json.substring(graphIdx, externalIdx)
        assertTrue(graphBlock.contains("\"a\": \"CliA\"") && graphBlock.contains("\"b\": \"Hub\""), graphBlock)
        assertTrue(graphBlock.contains("\"a\": \"CliB\"") && graphBlock.contains("\"b\": \"Hub\""), graphBlock)
        assertTrue(
            !Regex(""""a": "CliA",\s*"b": "CliB"""").containsMatchIn(graphBlock) &&
                !Regex(""""a": "CliB",\s*"b": "CliA"""").containsMatchIn(graphBlock),
            "clients must not share a sync edge:\n$graphBlock",
        )

        val hiddenIdx = json.indexOf("\"compositionHidden\"")
        assertTrue(hiddenIdx >= 0, json)
        val hiddenBlock = json.substring(hiddenIdx)
        // Each client pairs with Hub separately; Hub appears as a peer in each group.
        assertTrue(hiddenBlock.contains("\"role\": \"service/client\""), hiddenBlock)
        assertTrue(
            hiddenBlock.contains("\"pclassKey\": \"Hub\""),
            "service must appear in compositionHidden peers:\n$hiddenBlock",
        )
        // Clients must not be listed as the only peers of one shared group.
        assertTrue(
            !Regex(
                """"peers": \[[^\]]*\"pclassKey\": \"CliA\"[^\]]*\"pclassKey\": \"CliB\""""
            ).containsMatchIn(hiddenBlock.replace("\n", " ")),
            "clients must not share one sync group:\n$hiddenBlock",
        )
    }








    @Test
    fun leafSShowsIncrementAsExternal() {
        val dir = Files.createTempDirectory("julay-alphabet-json")
        val file = dir.resolve("main.jul")
        file.writeText(
            """
            import julay.funlib.exitProgram
            import julay.funlib.println

            static proc S {
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
