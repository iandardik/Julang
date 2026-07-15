package julay.program.library

import com.microsoft.z3.Context
import com.microsoft.z3.Status
import com.sun.net.httpserver.HttpServer
import julay.program.ConcreteAction
import julay.program.Value
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
            val client = JulHttpClient()
            for (payload in listOf("ping", "pong")) {
                val req = HttpClientRequest("http://127.0.0.1:$port/", "POST", payload)
                val ctxSend = Context()
                try {
                    val solver = ctxSend.mkSolver()
                    solver.add(
                        ctxSend.mkEq(
                            JulHttpClient.reqArg.toZ3Expr(ctxSend),
                            httpClientRequestType.toZ3Expr(Value(req, httpClientRequestType), ctxSend),
                        ),
                    )
                    assert(solver.check() == Status.SATISFIABLE)
                    client.transit(ConcreteAction(JulHttpClient.sendRequestAct, ctxSend, solver.model))
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
                    client.transit(ConcreteAction(JulHttpClient.receiveResponseAct, ctxRecv, solver.model))
                } finally {
                    ctxRecv.close()
                }
            }
        } finally {
            server.stop(0)
        }
    }
}
