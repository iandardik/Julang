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
            sort Node := { "n1", "n2" }

            proc Protocol {
                constructor initially(args : List<String>) { transit: }
                transition requestVote() { transit: }
                provider transition handleVote() { transit: }
            }

            spec Net[n : Node] {
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
            "provider/client handleVote must label the ClusterSpec—Net edge:\n$graphBlock",
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
            sort Node := { "n1" }
            spec Net[n : Node] {
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
            proc A {
                constructor initially(args : List<String>) { transit: }
                transition inner() { transit: }
                transition outer() { transit: }
            }
            proc B {
                constructor initially(args : List<String>) { transit: }
                transition inner() { transit: }
            }
            proc C {
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
            proc Hub {
                constructor initially(args : List<String>) { transit: }
                provider transition w() { transit: }
            }
            proc Cli {
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
            proc Hub {
                constructor initially(args : List<String>) { transit: }
                provider transition staleTerm(term : Int) { transit: }
            }
            proc CliA {
                constructor initially(args : List<String>) { transit: }
                client transition staleTerm(term : Int) { transit: }
            }
            proc CliB {
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
        assertTrue(hiddenBlock.contains("\"role\": \"provider/client\""), hiddenBlock)
        assertTrue(
            hiddenBlock.contains("\"pclassKey\": \"Hub\""),
            "provider must appear in compositionHidden peers:\n$hiddenBlock",
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
    fun compositionGraphOmitsNestedProcFunCallsOnComposite() {
        val dir = Files.createTempDirectory("julay-procfun-graph")
        val file = dir.resolve("main.jul")
        file.writeText(
            """
            procfun clientAppendRPC(n : Int) : Int {
                client transition noLeader() {
                    return: 0
                }
                transition done() {
                    return: n
                }
            }

            proc RpcReqHandler {
                var out : Int
                constructor initially(args : List<String>) {
                    transit: out := clientAppendRPC(1)
                }
            }

            proc Peer {
                constructor initially(args : List<String>) { transit: }
                transition tick() { transit: }
            }

            proc RpcIn := RpcReqHandler || Peer
            compile RpcIn
            """.trimIndent(),
        )
        val checked = prepareCheckedCompilation(file)
        assertNotNull(checked)
        val scope = resolveAnalyzeScope(
            scopeNames = listOf("RpcIn"),
            procDecls = checked.procDecls,
            allPClassNames = checked.unit.allPClassNames,
            allProcAliasNames = checked.unit.allProcNames,
            librariesInUse = checked.librariesInUse,
            procFunNames = julay.compiler.collectProcFunNames(checked.ast as RootNode),
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
        assertTrue(graphBlock.contains("\"RpcReqHandler\""), graphBlock)
        assertTrue(graphBlock.contains("\"Peer\""), graphBlock)
        assertTrue(
            !graphBlock.contains("\"clientAppendRPC\""),
            "composite must not lift nested procfun call nodes:\n$graphBlock",
        )
        assertTrue(
            !graphBlock.contains("\"actions\": []"),
            "composite must not emit call edges:\n$graphBlock",
        )
    }

    @Test
    fun leafCompositionGraphIncludesBareProcFunCallEdge() {
        val dir = Files.createTempDirectory("julay-leaf-procfun-graph")
        val file = dir.resolve("main.jul")
        file.writeText(
            """
            procfun clientAppendRPC(n : Int) : Int {
                client transition noLeader() {
                    return: 0
                }
                transition done() {
                    return: n
                }
            }

            proc RpcReqHandler {
                var out : Int
                constructor initially(args : List<String>) {
                    transit: out := clientAppendRPC(1)
                }
            }

            compile RpcReqHandler
            """.trimIndent(),
        )
        val checked = prepareCheckedCompilation(file)
        assertNotNull(checked)
        val scope = resolveAnalyzeScope(
            scopeNames = listOf("RpcReqHandler"),
            procDecls = checked.procDecls,
            allPClassNames = checked.unit.allPClassNames,
            allProcAliasNames = checked.unit.allProcNames,
            librariesInUse = checked.librariesInUse,
            procFunNames = julay.compiler.collectProcFunNames(checked.ast as RootNode),
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
            graphBlock.contains("\"nodes\": [\"RpcReqHandler\", \"clientAppendRPC\"]"),
            "leaf diagram should be caller then callee:\n$graphBlock",
        )
        assertTrue(
            graphBlock.contains("\"a\": \"RpcReqHandler\"") &&
                graphBlock.contains("\"b\": \"clientAppendRPC\"") &&
                graphBlock.contains("\"actions\": []"),
            "expected directed call edge RpcReqHandler→clientAppendRPC:\n$graphBlock",
        )
    }

    @Test
    fun leafCompositionGraphIncludesApiQualifiedProcFunCallEdge() {
        val dir = Files.createTempDirectory("julay-leaf-api-call-graph")
        val file = dir.resolve("main.jul")
        file.writeText(
            """
            procfun startTimeout() : Boolean {
                transition startTimeout() {
                    return: true
                }
            }

            proc TimeoutImpl {
                constructor startTimeout() { transit: }
            }

            export api Timeout {
                proc: TimeoutImpl
                calls: startTimeout
            }

            proc Protocol {
                var ok : Boolean
                constructor initially(args : List<String>) {
                    transit: ok := Timeout.startTimeout()
                }
            }

            compile Protocol
            """.trimIndent(),
        )
        val checked = prepareCheckedCompilation(file)
        assertNotNull(checked)
        val scope = resolveAnalyzeScope(
            scopeNames = listOf("Protocol"),
            procDecls = checked.procDecls,
            allPClassNames = checked.unit.allPClassNames,
            allProcAliasNames = checked.unit.allProcNames,
            librariesInUse = checked.librariesInUse,
            procFunNames = julay.compiler.collectProcFunNames(checked.ast as RootNode),
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
        assertTrue(graphBlock.contains("\"Protocol\""), graphBlock)
        assertTrue(graphBlock.contains("\"startTimeout\""), graphBlock)
        assertTrue(
            graphBlock.contains("\"a\": \"Protocol\"") &&
                graphBlock.contains("\"b\": \"startTimeout\"") &&
                graphBlock.contains("\"actions\": []"),
            "expected directed api-qualified call edge Protocol→startTimeout:\n$graphBlock",
        )
    }

    @Test
    fun compositionGraphCompositeShowsOnlySyncSpine() {
        // Nested helper under RpcReqHandler must not appear on the composite diagram.
        val dir = Files.createTempDirectory("julay-graph-order")
        val file = dir.resolve("main.jul")
        file.writeText(
            """
            procfun helper(n : Int) : Int {
                transition done() { return: n }
            }

            proc RpcReqHandler {
                var out : Int
                constructor initially(args : List<String>) {
                    transit: out := helper(1)
                }
                session transition ping() { transit: }
            }
            proc ServerInitializer {
                constructor initially(args : List<String>) { transit: }
                session transition setup() { transit: }
            }
            proc HttpServer {
                constructor initially(args : List<String>) { transit: }
                session transition setup() { transit: }
                session transition ping() { transit: }
            }

            proc RpcIn := RpcReqHandler || ServerInitializer || HttpServer
            compile RpcIn
            """.trimIndent(),
        )
        val checked = prepareCheckedCompilation(file)
        assertNotNull(checked)
        val scope = resolveAnalyzeScope(
            scopeNames = listOf("RpcIn"),
            procDecls = checked.procDecls,
            allPClassNames = checked.unit.allPClassNames,
            allProcAliasNames = checked.unit.allProcNames,
            librariesInUse = checked.librariesInUse,
            procFunNames = julay.compiler.collectProcFunNames(checked.ast as RootNode),
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
        val nodesMatch = Regex(""""nodes": \[([^\]]+)\]""").find(graphBlock)
        assertNotNull(nodesMatch, graphBlock)
        val nodes = nodesMatch!!.groupValues[1]
            .split(",")
            .map { it.trim().removeSurrounding("\"") }
        assertTrue(
            nodes.toSet() == setOf("ServerInitializer", "HttpServer", "RpcReqHandler"),
            "composite sync spine only (no nested procfun):\n$graphBlock",
        )
        assertTrue(!nodes.contains("helper"), graphBlock)
    }

    @Test
    fun calledProcFunFoldsIntoParentWithoutExplicitComposition() {
        val dir = Files.createTempDirectory("julay-procfun-call-fold")
        val file = dir.resolve("main.jul")
        file.writeText(
            """
            procfun clientAppendRPC(n : Int) : Int {
                client transition noLeader() {
                    return: 0
                }
                transition done() {
                    return: n
                }
            }

            proc Handler {
                var out : Int
                constructor initially(args : List<String>) {
                    transit: out := clientAppendRPC(1)
                }
            }

            proc RpcIn := Handler
            compile RpcIn
            """.trimIndent(),
        )
        val checked = prepareCheckedCompilation(file)
        assertNotNull(checked)
        val scope = resolveAnalyzeScope(
            scopeNames = listOf("RpcIn"),
            procDecls = checked.procDecls,
            allPClassNames = checked.unit.allPClassNames,
            allProcAliasNames = checked.unit.allProcNames,
            librariesInUse = checked.librariesInUse,
            procFunNames = julay.compiler.collectProcFunNames(checked.ast as RootNode),
        )
        assertNotNull(scope)
        val json = buildAlphabetJsonDocument(
            checked.ast as RootNode,
            scope,
            checked.librariesInUse,
            checked.procDecls,
        )
        assertTrue(json.contains("\"name\": \"noLeader\""), "called procfun should fold into parent:\n$json")
        assertTrue(!json.contains("\"name\": \"clientAppendRPC_call\""), json)
        assertTrue(!json.contains("\"name\": \"clientAppendRPC_ret\""), json)
    }

    @Test
    fun standaloneProcFunShowsUserActionsInExternalAlphabet() {
        val dir = Files.createTempDirectory("julay-procfun-alphabet")
        val file = dir.resolve("main.jul")
        file.writeText(
            """
            procfun countUp(n : Int) : Int {
                var i : Int := 0
                var result : Int := 0

                internal transition step() {
                    guard: i < n
                    transit:
                        result := i + 1
                        i := i + 1
                }

                transition done() {
                    guard: i = n
                    return: result
                }
            }

            compile countUp
            """.trimIndent(),
        )
        val checked = prepareCheckedCompilation(file)
        assertNotNull(checked)
        val scope = resolveAnalyzeScope(
            scopeNames = listOf("countUp"),
            procDecls = checked.procDecls,
            allPClassNames = checked.unit.allPClassNames,
            allProcAliasNames = checked.unit.allProcNames,
            librariesInUse = checked.librariesInUse,
            procFunNames = julay.compiler.collectProcFunNames(checked.ast as RootNode),
        )
        assertNotNull(scope)
        val json = buildAlphabetJsonDocument(
            checked.ast as RootNode,
            scope,
            checked.librariesInUse,
            checked.procDecls,
        )
        assertTrue(json.contains("\"name\": \"step\""), json)
        assertTrue(json.contains("\"name\": \"done\""), json)
        // Bare return must stay ordinary in the IR alphabet (not rewritten to source-internal).
        assertTrue(
            json.contains(Regex("""\"name\": \"done\"[\s\S]*?\"modifier\": \"ordinary\"""")),
            "bare return done() must remain ordinary:\n$json",
        )
        assertTrue(!json.contains("\"name\": \"countUp_call\""), json)
        assertTrue(!json.contains("\"name\": \"countUp_ret\""), json)
    }

    @Test
    fun whenNestedProcFunCallDoesNotOrphanAndFoldsNoLeader() {
        val dir = Files.createTempDirectory("julay-procfun-fold")
        val file = dir.resolve("main.jul")
        file.writeText(
            """
            procfun clientAppendRPC(n : Int) : Int {
                client transition noLeader() {
                    return: 0
                }
                transition done() {
                    return: n
                }
            }

            proc Handler {
                var out : Int
                var path : String

                constructor initially(args : List<String>) {
                    transit:
                        path := "a"
                        out := 0
                }

                transition dispatch() {
                    transit:
            out := when (path) {
                "a" -> RpcIn.clientAppendRPC(1)
                else -> 0
            }
                }
            }

            api RpcIn {
                proc: Handler
                calls: clientAppendRPC
            }
            compile RpcIn
            """.trimIndent(),
        )
        val checked = prepareCheckedCompilation(file)
        assertNotNull(checked)

        // Api-listed calls need not be called by the interior (external entry points).
        val components = checked.unit.allPClassNames + checked.librariesInUse
        val ok = julay.compiler.runErrorAndWarningPasses(
            checked.ast,
            components,
            checked.librariesInUse,
            programName = null,
            program = null,
            procDecls = checked.procDecls,
        )
        assertTrue(ok, "api-listed procfun must not fail composition checks")

        val scope = resolveAnalyzeScope(
            scopeNames = listOf("RpcIn"),
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
        assertTrue(json.contains("\"name\": \"noLeader\""), "noLeader should appear in alphabet:\n$json")
        assertTrue(
            !json.contains("\"name\": \"clientAppendRPC_call\""),
            "synthetic call must stay out of IDE alphabet:\n$json",
        )
        assertTrue(
            !json.contains("\"name\": \"clientAppendRPC_ret\""),
            "synthetic ret must stay out of IDE alphabet:\n$json",
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
