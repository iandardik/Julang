package exspecs.program.library

import com.microsoft.z3.Context
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import exspecs.program.*
import exspecs.tools.mkStringConst
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.locks.ReentrantLock

class JulHttpServer : TransitionSystem, HttpHandler {
    private val ctx = Context()
    private val requests = ConcurrentLinkedQueue<String>()
    private val reqLock = ReentrantLock()
    private val reqCond = reqLock.newCondition()

    init {
        val server = HttpServer.create(InetSocketAddress(8000), 0)
        server.createContext("/", this)
        server.setExecutor(null) // creates a default executor
        server.start()
    }

    override fun actions(): Set<SymbolicAction> {
        var bodyVal = ""
        reqLock.lock()
        try {
            while (requests.isEmpty()) {
                reqCond.await()
            }
            bodyVal = requests.remove()!!
        }
        finally {
            reqLock.unlock()
        }
        return setOf(
            SymbolicAction(
                ActionSignature("handleHttpReq", listOf(Variable("reqBody", stringType))),
                ctx.mkAnd(
                    ctx.mkEq(ctx.mkStringConst("reqBody"), ctx.mkString(bodyVal))
                )
            ),
        )
    }
    override fun currentStateToZ3Expr() = ctx.mkTrue()
    override fun transit(act: ConcreteAction) {}
    override fun getContext() = ctx

    override fun handle(exchange: HttpExchange?) {
        // implements typical life cycle: https://docs.oracle.com/en/java/javase/22/docs/api/jdk.httpserver/com/sun/net/httpserver/HttpExchange.html
        val reqMethod = exchange!!.requestMethod
        val reqHeaders = exchange.requestHeaders
        val reqBody = exchange.requestBody.bufferedReader().use { it.readText() }
        reqLock.lock()
        try {
            requests.add(reqBody)
            reqCond.signalAll()
        }
        finally {
            reqLock.unlock()
        }

        // TODO get response from the handler
        val resp = "ok"
        exchange.responseHeaders
        exchange.sendResponseHeaders(200, resp.length.toLong())
        exchange.responseBody.writer().use { writer ->
            writer.write(resp)
        }
    }
}

// TODO this is ugly af
val httpServerTSStaticInfoStr = "TransitionSystemStaticInfo(" +
        "\nsetOf(" +
        "\nActionSignature(\"handleHttpReq\", listOf(Variable(\"reqBody\", stringType)))" +
        "\n)," +
        "\nsetOf(ActionSignature(\"initially\", listOf()),)," +
        "\ntrue) { JulHttpServer() }"
