package julay.regression

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import kotlin.test.assertEquals

object HttpProbe {
    private const val BASE = "http://localhost:8000"

    fun waitForPort(timeoutMs: Long = 60_000) {
        val deadline = System.currentTimeMillis() + timeoutMs
        var lastError: Throwable? = null
        while (System.currentTimeMillis() < deadline) {
            try {
                java.net.Socket("localhost", 8000).use { }
                return
            } catch (e: Throwable) {
                lastError = e
                Thread.sleep(200)
            }
        }
        throw AssertionError("Port 8000 not open within ${timeoutMs}ms", lastError)
    }

    fun postExpectBody(body: String, expectBody: String) {
        val client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()
        val request = HttpRequest.newBuilder()
            .uri(URI.create(BASE))
            .timeout(Duration.ofSeconds(30))
            .header("Content-Type", "text/plain")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        assertEquals(200, response.statusCode(), "HTTP status for POST '$body'")
        assertEquals(expectBody, response.body(), "HTTP response body for POST '$body'")
    }
}
