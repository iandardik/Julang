package julay.program.library

import com.microsoft.z3.Context
import com.microsoft.z3.Status
import com.sun.net.httpserver.HttpServer
import julay.program.Program
import julay.program.Value
import julay.program.action.ConcreteAction
import kotlinx.coroutines.runBlocking
import java.net.InetSocketAddress
import kotlin.test.Test
import kotlin.test.assertEquals

class HttpClientTest {
    @Test
    fun sendReceiveCanBeRepeatedOnSameClient() = runBlocking {
        val server = HttpServer.create(InetSocketAddress(0), 0)
        server.createContext("/") { exchange ->
            val body = exchange.requestBody.bufferedReader().use { it.readText() }
            val reply = "echo:$body".toByteArray()
            exchange.sendResponseHeaders(200, reply.size.toLong())
            exchange.responseBody.use { it.write(reply) }
        }
        server.start()
        try {
            val port = server.address.port
            val program = Program(setOf(JulHttpClient.staticInfo()))
            val client = JulHttpClient(program)
            for (payload in listOf("ping", "pong")) {
                val req = HttpClientRequest("http://127.0.0.1:$port/", "POST", payload)
                val ctxSend = Context()
                try {
                    val acts = client.actions(ctxSend)
                    val sendTs = acts.first { it.symAction == JulHttpClient.sendRequestAct }
                    val solver = ctxSend.mkSolver()
                    solver.add(sendTs.guard)
                    solver.add(
                        ctxSend.mkEq(
                            JulHttpClient.reqArg.toZ3Expr(ctxSend),
                            httpClientRequestType.toZ3Expr(Value(req, httpClientRequestType), ctxSend),
                        ),
                    )
                    assert(solver.check() == Status.SATISFIABLE)
                    val sendAct = ConcreteAction(JulHttpClient.sendRequestAct, ctxSend, solver.model)
                    client.transit(sendAct)
                } finally {
                    ctxSend.close()
                }

                val ctxRecv = Context()
                try {
                    val acts = client.actions(ctxRecv)
                    assertEquals(1, acts.size)
                    val expected = HttpClientResponse("echo:$payload", 200)
                    val solver = ctxRecv.mkSolver()
                    solver.add(acts.first().guard)
                    solver.add(
                        ctxRecv.mkEq(
                            JulHttpClient.respArg.toZ3Expr(ctxRecv),
                            httpClientResponseType.toZ3Expr(Value(expected, httpClientResponseType), ctxRecv),
                        ),
                    )
                    assert(solver.check() == Status.SATISFIABLE)
                    val recvAct = ConcreteAction(JulHttpClient.receiveResponseAct, ctxRecv, solver.model)
                    client.transit(recvAct)
                } finally {
                    ctxRecv.close()
                }
            }
        } finally {
            server.stop(0)
        }
    }
}
