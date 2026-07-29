package julay.compiler.analysis

import julay.compiler.ast.RootNode
import julay.compiler.prepareCheckedCompilation
import java.nio.file.Files
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
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
    fun compositionGraphIncludesProcFunCallEdgesWithoutActions() {
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
        assertTrue(graphBlock.contains("\"clientAppendRPC\""), graphBlock)
        assertTrue(
            graphBlock.contains("\"a\": \"RpcReqHandler\"") &&
                graphBlock.contains("\"b\": \"clientAppendRPC\"") &&
                graphBlock.contains("\"actions\": []"),
            "expected unlabeled call edge RpcReqHandler—clientAppendRPC:\n$graphBlock",
        )
        // Caller with procfuns is ordered at the end of the || spine, then callees.
        assertTrue(
            graphBlock.contains("\"nodes\": [\"Peer\", \"RpcReqHandler\", \"clientAppendRPC\"]") ||
                graphBlock.contains("\"nodes\": [\"RpcReqHandler\", \"Peer\", \"clientAppendRPC\"]"),
            "expected procfun after caller on the spine:\n$graphBlock",
        )
    }

    @Test
    fun compositionGraphOrdersSyncSpineThenProcFuns() {
        // ServerInitializer — HttpServer — RpcReqHandler — helpers minimizes crossings
        // vs composition order RpcReqHandler || ServerInitializer || HttpServer.
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
        assertEquals(
            listOf("ServerInitializer", "HttpServer", "RpcReqHandler", "helper"),
            nodes,
            "sync spine then caller then procfun:\n$graphBlock",
        )
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
