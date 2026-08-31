package julay.program.library

import com.microsoft.z3.Context
import julay.program.Program
import julay.program.TransitionSystem
import julay.program.TransitionSystemStaticInfo
import julay.program.Value
import julay.program.Variable
import julay.program.action.ConcreteAction
import julay.program.action.SymbolicAction
import julay.program.action.TSAction
import julay.program.sync.BoolExprFast
import julay.program.sync.FastOffer
import julay.program.sync.SyncStepPlan
import julay.program.type.intType
import julay.program.type.stringType
import kotlinx.coroutines.runBlocking
import java.net.ServerSocket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HttpServerTest {
    private val reqVar = Variable("req", httpServerRequestType)
    private val echoCtor = SymbolicAction("echoHandler", listOf(reqVar))
    private val respondAct = SymbolicAction("respond", emptyList(), isInternal = true)

    private fun echoProgram(): Program {
        val echoInfo = TransitionSystemStaticInfo(
            "echoHandler",
            setOf(respondAct),
            mapOf(echoCtor to { _, _ -> EchoHandlerTs() }),
        )
        return Program(
            setOf(JulHttpServer.staticInfo()),
            procFunInfo = setOf(echoInfo),
        )
    }

    @Test
    fun invokeProcFunBlockingReturnsEchoResponse() {
        val program = echoProgram()
        val resp = program.invokeProcFunBlocking(
            "echoHandler",
            listOf(HttpServerRequest("/ping", "hello")),
        ) as HttpServerResponse
        assertEquals(200, resp.code)
        assertEquals("echo:hello", resp.body)
    }

    @Test
    fun sequentialMultiRequestViaEmbeddedServer() = runBlocking {
        val program = echoProgram()
        val (port, server) = startEmbeddedServer(program)
        try {
            repeat(10) { i ->
                val req = HttpClientRequest("http://127.0.0.1:$port/ping", "POST", "msg$i")
                val resp = doHttpRequest(req)
                assertEquals(200, resp.code, "request $i")
                assertEquals("echo:msg$i", resp.body, "request $i")
            }
        } finally {
            server.transit(ConcreteAction(JulHttpServer.closeAct, emptyMap()))
        }
    }

    @Test
    fun concurrentRequestsViaEmbeddedServer() {
        val program = echoProgram()
        val (port, server) = runBlocking { startEmbeddedServer(program) }
        val threads = 8
        val perThread = 20
        val pool = Executors.newFixedThreadPool(threads)
        val failures = AtomicInteger(0)
        val latch = CountDownLatch(threads)
        try {
            repeat(threads) { t ->
                pool.submit {
                    try {
                        repeat(perThread) { i ->
                            val req = HttpClientRequest(
                                "http://127.0.0.1:$port/ping",
                                "POST",
                                "t${t}_$i",
                            )
                            val resp = doHttpRequest(req)
                            if (resp.code != 200 || resp.body != "echo:t${t}_$i") {
                                failures.incrementAndGet()
                            }
                        }
                    } finally {
                        latch.countDown()
                    }
                }
            }
            assertTrue(latch.await(60, TimeUnit.SECONDS), "timed out waiting for clients")
            assertEquals(0, failures.get(), "some concurrent requests failed")
        } finally {
            pool.shutdownNow()
            runBlocking {
                server.transit(ConcreteAction(JulHttpServer.closeAct, emptyMap()))
            }
        }
    }

    @Test
    fun closeShutsDownHandlerPool() = runBlocking {
        val program = echoProgram()
        val (port, server) = startEmbeddedServer(program)
        server.transit(ConcreteAction(JulHttpServer.closeAct, emptyMap()))
        val req = HttpClientRequest("http://127.0.0.1:$port/ping", "POST", "after-close")
        val resp = doHttpRequest(req)
        assertEquals(0, resp.code)
    }

    @Test
    fun handlerFailureThenRecoveryViaPool() = runBlocking {
        val throwOnce = AtomicBoolean(true)
        val reqVar = Variable("req", httpServerRequestType)
        val echoCtor = SymbolicAction("flakyHandler", listOf(reqVar))
        val respondAct = SymbolicAction("respond", emptyList(), isInternal = true)
        val info = TransitionSystemStaticInfo(
            "flakyHandler",
            setOf(respondAct),
            mapOf(
                echoCtor to { _, _ ->
                    if (throwOnce.compareAndSet(true, false)) {
                        ThrowingEchoHandlerTs()
                    } else {
                        EchoHandlerTs()
                    }
                },
            ),
        )
        val program = Program(
            setOf(JulHttpServer.staticInfo()),
            procFunInfo = setOf(info),
        )
        val (port, server) = startEmbeddedServer(program, handlerName = "flakyHandler")
        try {
            val failReq = HttpClientRequest("http://127.0.0.1:$port/ping", "POST", "boom")
            val failResp = doHttpRequest(failReq)
            assertTrue(failResp.code != 200, "expected handler failure response")
            val okReq = HttpClientRequest("http://127.0.0.1:$port/ping", "POST", "ok")
            val resp = doHttpRequest(okReq)
            assertEquals(200, resp.code)
            assertEquals("echo:ok", resp.body)
        } finally {
            server.transit(ConcreteAction(JulHttpServer.closeAct, emptyMap()))
        }
    }

    private suspend fun startEmbeddedServer(
        program: Program,
        handlerName: String = "echoHandler",
    ): Pair<Int, JulHttpServer> {
        val server = JulHttpServer(program)
        val port = ServerSocket(0).use { it.localPort }
        server.finishConstruction(
            ConcreteAction(
                JulHttpServer.listenAct,
                mapOf(
                    JulHttpServer.portArg to Value(port, intType),
                    JulHttpServer.handlerArg to Value(handlerName, stringType),
                ),
            ),
        )
        return port to server
    }

    private class ThrowingEchoHandlerTs : TransitionSystem {
        override suspend fun finishConstruction(act: ConcreteAction) {
            throw RuntimeException("simulated handler failure")
        }

        override suspend fun actions(ctx: Context): Set<TSAction> = emptySet()

        override suspend fun transit(act: ConcreteAction) {}

        override fun syncStepPlan(): SyncStepPlan = SyncStepPlan.FastOnly(emptyList())
    }

    private class EchoHandlerTs : TransitionSystem {
        private val respondAct = SymbolicAction("respond", emptyList(), isInternal = true)
        private var req: HttpServerRequest? = null
        private var procFunReturn: Value? = null
        private var done = false

        override suspend fun finishConstruction(act: ConcreteAction) {
            req = act.lookup(Variable("req", httpServerRequestType)).value as HttpServerRequest
        }

        override suspend fun actions(ctx: Context): Set<TSAction> {
            if (done) return emptySet()
            return setOf(
                TSAction(
                    respondAct,
                    ctx.mkTrue(),
                    TSAction.SyncRole.Internal,
                    fastGuard = BoolExprFast.True,
                ),
            )
        }

        override suspend fun transit(act: ConcreteAction) {
            if (act.symAction.name == respondAct.name) {
                val body = req!!.body
                procFunReturn = Value(
                    HttpServerResponse("echo:$body", 200),
                    httpServerResponseType,
                )
                done = true
            }
        }

        override fun consumeProcFunReturn(): Value? {
            val v = procFunReturn
            procFunReturn = null
            return v
        }

        override fun syncStepPlan(): SyncStepPlan {
            if (done) return SyncStepPlan.FastOnly(emptyList())
            return SyncStepPlan.FastOnly(
                listOf(FastOffer(respondAct, BoolExprFast.True, TSAction.SyncRole.Internal)),
            )
        }
    }
}
