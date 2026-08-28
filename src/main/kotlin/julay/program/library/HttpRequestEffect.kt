package julay.program.library

import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse.BodyHandlers

/** One fresh JDK connection per call (no shared pool). */
fun doHttpRequest(request: HttpClientRequest): HttpClientResponse {
    val client = java.net.http.HttpClient.newBuilder().build()
    val builder = HttpRequest.newBuilder().uri(URI.create(request.url))
    val method = request.method.uppercase()
    val jdkRequest = when (method) {
        "GET", "HEAD" -> builder.method(method, BodyPublishers.noBody()).build()
        else -> builder.method(method, BodyPublishers.ofString(request.body)).build()
    }
    return try {
        val jdkResponse = client.send(jdkRequest, BodyHandlers.ofString())
        HttpClientResponse(jdkResponse.body(), jdkResponse.statusCode())
    } catch (_: Exception) {
        // Raft and other networked procs treat non-200 responses as RPC failure; avoid
        // killing the caller coroutine on transient connect/refused errors at startup.
        HttpClientResponse("", 0)
    }
}
