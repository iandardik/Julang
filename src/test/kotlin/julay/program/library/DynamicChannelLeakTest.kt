package julay.program.library

import com.microsoft.z3.Context
import julay.program.ChannelType
import julay.program.ConcreteAction
import julay.program.Program
import julay.program.Value
import julay.program.closeChannel
import julay.regression.JulCompiler
import kotlinx.coroutines.runBlocking
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Creator-local leak checks plus integration probes against CounterServer, ListServer,
 * and Raft. Subprocess tests use `/.julay/openDynamicChannels` when started with
 * `-Djulay.channelDebug=true` (not enabled in normal production runs).
 */
class DynamicChannelLeakTest {
    private val projectRoot = File(System.getProperty("user.dir"))
    private val http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()

    companion object {
        /** Serialize HTTP servers that bind fixed ports (8000 / raft 5001-5003). */
        private val portLock = Any()
    }

    @Test
    fun httpClientClosesChannelAfterReceive() = runBlocking {
        val jdkServer = com.sun.net.httpserver.HttpServer.create(java.net.InetSocketAddress(0), 0)
        jdkServer.createContext("/") { exchange ->
            val body = exchange.requestBody.bufferedReader().use { it.readText() }
            val reply = "ok:$body".toByteArray()
            exchange.sendResponseHeaders(200, reply.size.toLong())
            exchange.responseBody.use { it.write(reply) }
        }
        jdkServer.start()
        try {
            val port = jdkServer.address.port
            val program = Program(setOf(JulHttpClient.staticInfo()))
            val closeChan = program.createDynamicChannel(JulHttpClient.closeHttpClientAct)
            val client = JulHttpClient(program, closeChan)
            repeat(50) { i ->
                val req = HttpClientRequest("http://127.0.0.1:$port/", "POST", "n=$i")
                Context().use { ctx ->
                    val acts = client.actions(ctx)
                    val sendTs = acts.first { it.symAction == JulHttpClient.sendRequestAct }
                    val solver = ctx.mkSolver()
                    solver.add(sendTs.guard)
                    solver.add(
                        ctx.mkEq(
                            JulHttpClient.reqArg.toZ3Expr(ctx),
                            httpClientRequestType.toZ3Expr(Value(req, httpClientRequestType), ctx),
                        ),
                    )
                    assert(solver.check() == com.microsoft.z3.Status.SATISFIABLE)
                    val sendAct = ChannelType.withChannelLookup(client.heldChannels().associateBy { it.id }) {
                        ConcreteAction(JulHttpClient.sendRequestAct, ctx, solver.model)
                    }
                    client.transit(sendAct)
                }
                assertEquals(1, program.openDynamicChannelCount(JulHttpClient.receiveResponseAct))
                Context().use { ctx ->
                    val acts = client.actions(ctx)
                    assertEquals(1, acts.size)
                    val solver = ctx.mkSolver()
                    solver.add(acts.first().guard)
                    assert(solver.check() == com.microsoft.z3.Status.SATISFIABLE)
                    val recvAct = ChannelType.withChannelLookup(client.heldChannels().associateBy { it.id }) {
                        ConcreteAction(JulHttpClient.receiveResponseAct, ctx, solver.model)
                    }
                    client.transit(recvAct)
                }
                assertEquals(0, program.openDynamicChannelCount(JulHttpClient.receiveResponseAct))
            }
        } finally {
            jdkServer.stop(0)
        }
    }

    @Test
    fun forgottenCloseLeavesChannelOpen() = runBlocking {
        val program = Program(setOf(JulHttpClient.staticInfo()))
        val ch = program.createDynamicChannel(JulHttpClient.receiveResponseAct)
        assertEquals(1, program.openDynamicChannelCount(JulHttpClient.receiveResponseAct))
        assertTrue(!ch.isClosed())
        assertEquals(1, program.openDynamicChannelCount(JulHttpClient.receiveResponseAct))
        closeChannel(ch)
        assertEquals(0, program.openDynamicChannelCount(JulHttpClient.receiveResponseAct))
        assertTrue(ch.isClosed())
    }

    @Test
    fun httpServerClosesChannelAfterSendResponse() = runBlocking {
        val program = Program(setOf(JulHttpServer.staticInfo()))
        val ch = program.createDynamicChannel(JulHttpServer.sendResponseAct)
        assertEquals(1, program.openDynamicChannelCount(JulHttpServer.sendResponseAct))
        closeChannel(ch)
        assertEquals(0, program.openDynamicChannelCount(JulHttpServer.sendResponseAct))
        assertTrue(ch.isClosed())
    }

    @Test
    fun counterServerIdleAndLoadNoChannelLeak() {
        synchronized(portLock) {
            runServerLeakTest(
                sourceRel = "input/inc_server/main.jul",
                jarName = "CounterServer",
                idleMs = 1500,
                loadPosts = 80,
            )
        }
    }

    @Test
    fun listServerIdleAndLoadNoChannelLeak() {
        synchronized(portLock) {
            runServerLeakTest(
                sourceRel = "input/list_server/main.jul",
                jarName = "ListServer",
                idleMs = 1500,
                loadPosts = 80,
            )
        }
    }

    @Test
    fun raftIdleAndLoadNoChannelLeak() {
        synchronized(portLock) {
            val workspace = File(projectRoot, "build/leak-test-workspace/raft").also {
                it.deleteRecursively()
                it.mkdirs()
            }
            val compile = JulCompiler.compile(projectRoot, workspace, File(projectRoot, "input/raft/main.jul"))
            val nodeJar = compile.jars["RaftNode"]
                ?: error("RaftNode.jar missing\n${compile.output}")
            val clientJar = compile.jars["RaftClient"]
                ?: error("RaftClient.jar missing\n${compile.output}")
            val conf = File(workspace, "cluster.conf")
            File(projectRoot, "input/raft/cluster.conf").copyTo(conf, overwrite = true)
            val confAbs = conf.absolutePath

            val ports = listOf(5001, 5002, 5003)
            val procs = ports.mapIndexed { id, _ ->
                ProcessBuilder(
                    "java",
                    "-Djulay.channelDebug=true",
                    "-jar", nodeJar.absolutePath,
                    confAbs, id.toString(),
                )
                    .directory(workspace)
                    .redirectErrorStream(true)
                    .start()
            }
            try {
                ports.forEach { waitForPort(it, 90_000) }
                Thread.sleep(12_000) // election settle (idle)
                // Each Raft node has a closeHttpServer channel, a closeHttpClient channel,
                // and a long-lived HttpClient that may pre-create one receiveResponse Channel
                // while Idle. Leak signal is growth above that baseline.
                val idleBaselines = ports.associateWith { fetchOpenChannelCount(it) }
                idleBaselines.forEach { (port, n) ->
                    assertTrue(n in 2..4, "Raft node :$port idle open channels out of range: $n")
                }
                repeat(15) { i ->
                    ProcessBuilder(
                        "java", "-jar", clientJar.absolutePath,
                        "http://127.0.0.1:5001", "append", "v$i",
                    )
                        .directory(workspace)
                        .redirectErrorStream(true)
                        .start()
                        .waitFor(45, TimeUnit.SECONDS)
                }
                Thread.sleep(3000)
                ports.forEach { port ->
                    val after = fetchOpenChannelCount(port)
                    val baseline = idleBaselines.getValue(port)
                    assertEquals(
                        baseline,
                        after,
                        "Raft node :$port open channels grew after load (idle=$baseline, after=$after)",
                    )
                }
            } finally {
                procs.forEach { it.destroyForcibly() }
                procs.forEach { it.waitFor(5, TimeUnit.SECONDS) }
            }
        }
    }

    private fun runServerLeakTest(
        sourceRel: String,
        jarName: String,
        idleMs: Long,
        loadPosts: Int,
    ) {
        val workspace = File(projectRoot, "build/leak-test-workspace/$jarName").also {
            it.deleteRecursively()
            it.mkdirs()
        }
        val compile = JulCompiler.compile(projectRoot, workspace, File(projectRoot, sourceRel))
        val jar = compile.jars[jarName]
            ?: error("$jarName.jar missing\n${compile.output}")
        val proc = ProcessBuilder(
            "java",
            "-Djulay.channelDebug=true",
            "-jar", jar.absolutePath,
        )
            .directory(workspace)
            .redirectErrorStream(true)
            .start()
        try {
            waitForPort(8000, 60_000)
            Thread.sleep(idleMs)
            // One closeHttpServer channel stays open while the server is running.
            assertEquals(1, fetchOpenChannelCount(8000), "$jarName idle open channels")
            repeat(loadPosts) { i ->
                post(8000, "req-$i")
            }
            Thread.sleep(1500)
            assertEquals(1, fetchOpenChannelCount(8000), "$jarName after load open channels")
        } finally {
            proc.destroyForcibly()
            proc.waitFor(5, TimeUnit.SECONDS)
        }
    }

    private fun waitForPort(port: Int, timeoutMs: Long) {
        val deadline = System.currentTimeMillis() + timeoutMs
        var last: Throwable? = null
        while (System.currentTimeMillis() < deadline) {
            try {
                java.net.Socket("localhost", port).use { }
                return
            } catch (e: Throwable) {
                last = e
                Thread.sleep(200)
            }
        }
        throw AssertionError("Port $port not open within ${timeoutMs}ms", last)
    }

    private fun fetchOpenChannelCount(port: Int): Int {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:$port/.julay/openDynamicChannels"))
            .timeout(Duration.ofSeconds(10))
            .GET()
            .build()
        val response = http.send(request, HttpResponse.BodyHandlers.ofString())
        assertEquals(200, response.statusCode())
        return response.body().trim().toInt()
    }

    private fun post(port: Int, body: String) {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:$port/"))
            .timeout(Duration.ofSeconds(30))
            .header("Content-Type", "text/plain")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()
        val response = http.send(request, HttpResponse.BodyHandlers.ofString())
        assertTrue(response.statusCode() in 200..399, "POST failed: ${response.statusCode()} ${response.body()}")
    }
}
