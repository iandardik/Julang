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
import kotlin.test.Test
import kotlin.test.assertEquals

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
    fun handleRoundTripViaEmbeddedServer() = runBlocking {
        val program = echoProgram()
        val server = JulHttpServer(program)
        val port = ServerSocket(0).use { it.localPort }
        server.finishConstruction(
            ConcreteAction(
                JulHttpServer.listenAct,
                mapOf(
                    JulHttpServer.portArg to Value(port, intType),
                    JulHttpServer.handlerArg to Value("echoHandler", stringType),
                ),
            ),
        )
        try {
            val req = HttpClientRequest("http://127.0.0.1:$port/ping", "POST", "ping")
            val resp = doHttpRequest(req)
            assertEquals(200, resp.code)
            assertEquals("echo:ping", resp.body)
        } finally {
            server.transit(ConcreteAction(JulHttpServer.closeAct, emptyMap()))
        }
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
