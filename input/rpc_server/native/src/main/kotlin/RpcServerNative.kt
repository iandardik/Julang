/**
 * Kotlin-native twin of Julay `input/rpc_server` for localhost RPS/latency comparison.
 *
 * Same wire API:
 *   POST /rpc/increment  → v=<n>
 *   POST /rpc/get        → v=<n>
 *   POST /rpc/add        body delta=<int> → v=<n>
 *   else                 → 404 NOT_FOUND
 *
 * Uses the same JDK [com.sun.net.httpserver.HttpServer] stack as Julay's HttpServer
 * proclib, with a mutex-protected counter instead of SyncChannel.
 */
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.util.concurrent.Executors
import kotlin.concurrent.withLock
import java.util.concurrent.locks.ReentrantLock

private const val PORT = 8000

private class Protocol {
    private val lock = ReentrantLock()
    private var counter = 0

    fun increment(): Int = lock.withLock {
        counter += 1
        counter
    }

    fun add(delta: Int): Int = lock.withLock {
        counter += delta
        counter
    }

    fun get(): Int = lock.withLock { counter }
}

private fun fieldValue(line: String): String {
    val parts = line.split("=", limit = 2)
    require(parts.size == 2) { "expected key=value, got: $line" }
    return parts[1]
}

/** Match Julay [httpPathFromUriPath]: strip a single leading '/'. */
private fun httpPathFromUriPath(uriPath: String): String =
    if (uriPath.startsWith("/")) uriPath.substring(1) else uriPath

private fun handle(protocol: Protocol, exchange: HttpExchange) {
    val path = httpPathFromUriPath(exchange.requestURI.path ?: "")
    val body = exchange.requestBody.bufferedReader().use { it.readText() }

    val (code, respBody) = try {
        when (path) {
            "rpc/increment" -> 200 to "v=${protocol.increment()}"
            "rpc/get" -> 200 to "v=${protocol.get()}"
            "rpc/add" -> {
                // Same as Julay: split on '|', take first field, then value after '='.
                val first = body.split("|").firstOrNull() ?: ""
                val delta = fieldValue(first).toInt()
                200 to "v=${protocol.add(delta)}"
            }
            else -> 404 to "NOT_FOUND"
        }
    } catch (e: Exception) {
        400 to "BAD_REQUEST ${e.message}"
    }

    val bytes = respBody.toByteArray(Charsets.UTF_8)
    exchange.sendResponseHeaders(code, bytes.size.toLong())
    exchange.responseBody.use { it.write(bytes) }
}

fun main(args: Array<String>) {
    val port = args.firstOrNull()?.toIntOrNull() ?: PORT
    val protocol = Protocol()
    // backlog 0 matches Julay JulHttpServer; fixed pool avoids unbounded thread spawn noise.
    val server = HttpServer.create(InetSocketAddress(port), 0)
    server.executor = Executors.newCachedThreadPool()
    server.createContext("/") { exchange -> handle(protocol, exchange) }
    server.start()
    // Stay alive; bench harness kills the process.
    Thread.currentThread().join()
}
