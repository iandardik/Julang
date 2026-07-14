package julay.program.library

import com.sun.net.httpserver.HttpServer
import io.github.cvc5.Kind
import io.github.cvc5.TermManager
import julay.program.ConcreteAction
import julay.program.Value
import julay.tools.isSat
import julay.tools.newModelSolver
import kotlinx.coroutines.runBlocking
import java.net.InetSocketAddress
import kotlin.test.Test
import kotlin.test.assertTrue

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
            val tm = TermManager()
            val solver = newModelSolver(tm)
            val response = HttpClientResponse("echo:ping", 200)
            solver.assertFormula(
                tm.mkTerm(
                    Kind.EQUAL,
                    JulHttpClient.respArg.toSmtTerm(tm),
                    httpClientResponseType.toSmtTerm(Value(response, httpClientResponseType), tm),
                ),
            )
            assertTrue(solver.isSat())
            val act = ConcreteAction(
                JulHttpClient.receiveResponseAct,
                mapOf(JulHttpClient.respArg to Value(response, httpClientResponseType)),
            )
            client.transit(act)
        } finally {
            server.stop(0)
        }
    }
}
