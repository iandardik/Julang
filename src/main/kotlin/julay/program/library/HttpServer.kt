package julay.program.library

import com.microsoft.z3.Context
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import julay.program.*
import julay.tools.mkStringConst
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.InetSocketAddress

class JulHttpServer(
    private val actionTable : Map<SymbolicAction,ProgramAction>
) : TransitionSystem, HttpHandler {
    companion object: StaticInfo {
        val reqBodyArg = Variable("reqBody", stringType)
        val respBodyArg = Variable("respBody", stringType)
        val receiveRequestAct = SymbolicAction("receiveRequest", listOf(reqBodyArg))
        val sendResponseAct = SymbolicAction("sendResponse", listOf(respBodyArg))
        val closeAct = SymbolicAction("close", listOf())
        val initiallyCtor = Pair(
            SymbolicAction("initially", listOf())) { prog : Program, _ : ConcreteAction -> JulHttpServer(prog.actionTable) }
        // the $ in the name means that programs cannot create p-classes whose names conflict with this one
        override fun staticInfo() = TransitionSystemStaticInfo(
            "JulHttpServer$",
            setOf(receiveRequestAct, sendResponseAct, closeAct),
            mapOf(initiallyCtor),
            setOf(closeAct),
            true)
    }

    private val ctx = Context()
    private val scope = CoroutineScope(Dispatchers.Default)
    init {
        val server = HttpServer.create(InetSocketAddress(8000), 0)
        server.createContext("/", this)
        server.start()
    }
    override suspend fun actions(): Set<TSAction> {
        return setOf(
            TSAction(closeAct, ctx.mkTrue(), true),
        )
    }
    override suspend fun transit(act: ConcreteAction) {}
    override fun getContext() = ctx
    override fun handle(exchange: HttpExchange?) {
        val resource = HttpResource(exchange!!)
        scope.launch { Proc(resource, staticInfo(), actionTable).run() }
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
        override suspend fun actions(): Set<TSAction> {
            if (initHttpReq) {
                initHttpReq = false
                return setOf(
                    TSAction(
                        receiveRequestAct,
                        ctx.mkEq(ctx.mkStringConst("reqBody"), ctx.mkString(reqBody)),
                        false
                    ),
                )
            }
            else if (finishHttpReq) {
                finishHttpReq = false
                return setOf(TSAction(sendResponseAct, ctx.mkTrue(), false))
            }
            else {
                return setOf()
            }
        }
        override suspend fun transit(act: ConcreteAction) {
            if (act.symAction == sendResponseAct) {
                val respBody = act.lookup(respBodyArg).value as String
                exchange.responseHeaders
                exchange.sendResponseHeaders(200, respBody.length.toLong())
                exchange.responseBody.writer().use { writer ->
                    writer.write(respBody)
                }
            }
        }
        override fun getContext() = ctx
    }
}
