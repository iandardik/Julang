package julay.program.library

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import kotlin.test.Test
import kotlin.test.assertEquals

class HttpClientTest {
    @Test
    fun doHttpRequestReturnsResponseBody() {
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
            val req = HttpClientRequest("http://127.0.0.1:$port/", "POST", "ping")
            val resp = doHttpRequest(req)
            assertEquals(200, resp.code)
            assertEquals("echo:ping", resp.body)
        } finally {
            server.stop(0)
        }
    }
}
