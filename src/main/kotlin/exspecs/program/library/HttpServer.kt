package exspecs.program.library

import com.microsoft.z3.Context
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import exspecs.program.*
import exspecs.tools.mkStringConst
import java.net.InetSocketAddress

class JulHttpServer : TransitionSystem, HttpHandler {
    private val ctx = Context()
    init {
        val server = HttpServer.create(InetSocketAddress(8000), 0)
        server.createContext("/", this)
        server.start()
    }
    override fun actions(): Set<SymbolicAction> {
        Thread.sleep(9999999L) // TODO this is temporary because no one calls close, so a deadlock happens immediately
        return setOf(
            SymbolicAction(
                ActionSignature("closeHttpServer", listOf()),
                ctx.mkTrue()
            ),
        )
    }
    override fun transit(act: ConcreteAction) {}
    override fun getContext() = ctx
    override fun handle(exchange: HttpExchange?) {
        val resource = HttpResource(exchange!!)
        Thread(Proc(resource, Program.channelTable)).start()
    }
}

class HttpResource(
    private val exchange : HttpExchange
) : TransitionSystem {
    private val ctx = Context()
    private val reqBody : String
    private var initHttpReq = true
    private var finishHttpReq = true
    init {
        // first half of a typical HttpExchange life cycle:
        // https://docs.oracle.com/en/java/javase/22/docs/api/jdk.httpserver/com/sun/net/httpserver/HttpExchange.html
        val reqMethod = exchange!!.requestMethod
        val reqHeaders = exchange.requestHeaders
        reqBody = exchange.requestBody.bufferedReader().use { it.readText() }
    }
    override fun actions(): Set<SymbolicAction> {
        if (initHttpReq) {
            initHttpReq = false
            return setOf(
                SymbolicAction(
                    ActionSignature("receiveRequest", listOf(Variable("reqBody", stringType))),
                    ctx.mkEq(ctx.mkStringConst("reqBody"), ctx.mkString(reqBody))
                ),
            )
        }
        else if (finishHttpReq) {
            finishHttpReq = false
            return setOf(
                SymbolicAction(
                    ActionSignature("sendResponse", listOf(Variable("respBody", stringType))),
                    ctx.mkTrue()
                ),
            )
        }
        else {
            return setOf()
        }
    }
    override fun transit(act: ConcreteAction) {
        if (act.signature.name == "sendResponse") {
            val respBody = act.lookup(Variable("respBody", stringType)).value as String
            exchange.responseHeaders
            exchange.sendResponseHeaders(200, respBody.length.toLong())
            exchange.responseBody.writer().use { writer ->
                writer.write(respBody)
            }
        }
    }
    override fun getContext() = ctx
}

val httpServerTSStaticInfo = TransitionSystemStaticInfo(
    setOf(
        ActionSignature("receiveRequest", listOf(Variable("reqBody", stringType))),
        ActionSignature("sendResponse", listOf(Variable("respBody", stringType))),
        //ActionSignature("close", listOf()), // TODO mark this (and others) as 'service' or something so syncNum = 2
    ),
    mapOf(
        Pair(ActionSignature("initially", listOf())) { JulHttpServer() },
    ),
    true)
