package julay.program.library

import com.microsoft.z3.Context
import com.microsoft.z3.Status
import com.sun.net.httpserver.HttpServer
import julay.program.ConcreteAction
import julay.program.Value
import kotlinx.coroutines.runBlocking
import java.net.InetSocketAddress
import kotlin.test.Test

class HttpClientTest {
    @Test
    fun transitDoesNotThrowOnReceiveResponse() = runBlocking {
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
            val client = JulHttpClient(
                HttpClientRequest("http://127.0.0.1:$port/", "POST", "ping"),
            )
            val ctx = Context()
            val solver = ctx.mkSolver()
            val response = HttpClientResponse("echo:ping", 200)
            solver.add(
                ctx.mkEq(
                    JulHttpClient.respArg.toZ3Expr(ctx),
                    httpClientResponseType.toZ3Expr(Value(response, httpClientResponseType), ctx),
                ),
            )
            assert(solver.check() == Status.SATISFIABLE)
            val act = ConcreteAction(JulHttpClient.receiveResponseAct, ctx, solver.model)
            client.transit(act)
        } finally {
            server.stop(0)
        }
    }
}
