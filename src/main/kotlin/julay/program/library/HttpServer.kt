package julay.program.library

import com.microsoft.z3.Context
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import julay.compiler.decl.ActionDecl
import julay.compiler.LibraryLoc
import julay.program.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.InetSocketAddress

class JulHttpServer(
    private val port: Int,
    private val actionTable: Map<SymbolicAction, ProgramAction>,
) : TransitionSystem, HttpHandler {
    companion object : JulLibrary {
        override val julName = "HttpServer"
        val portArg = Variable("port", intType)
        val reqArg = Variable("req", httpServerRequestType)
        val respArg = Variable("resp", httpServerResponseType)
        val startServerAct = SymbolicAction("startServer", listOf(portArg))
        val receiveRequestAct = SymbolicAction("receiveRequest", listOf(reqArg))
        // req correlates the response with the exchange that received it (avoids cross-wiring concurrent handlers)
        val sendResponseAct = SymbolicAction("sendResponse", listOf(reqArg, respArg))
        val closeAct = SymbolicAction("close", listOf(), SymbolicAction.SyncType.P2P)
        val startServerCtor: Pair<SymbolicAction, suspend (Program, ConcreteAction) -> JulHttpServer> = Pair(
            startServerAct,
        ) { prog, act ->
            val port = act.lookup(portArg).value as Int
            JulHttpServer(port, prog.actionTable)
        }
        // the $ in the name means that programs cannot create p-classes whose names conflict with this one
        override fun staticInfo() = TransitionSystemStaticInfo(
            "JulHttpServer$",
            setOf(receiveRequestAct, sendResponseAct, closeAct),
            mapOf(startServerCtor),
        )
        override val actionDecls = listOf(
            ActionDecl(startServerAct, listOf(), emptyList(), TSAction.SyncRole.CSP, LibraryLoc(julName)),
            ActionDecl(receiveRequestAct, listOf(), emptyList(), TSAction.SyncRole.CSP, LibraryLoc(julName)),
            ActionDecl(sendResponseAct, listOf(), emptyList(), TSAction.SyncRole.CSP, LibraryLoc(julName)),
            ActionDecl(closeAct, listOf(), emptyList(), TSAction.SyncRole.P2PService, LibraryLoc(julName)),
        )
    }

    private val ctx = Context()
    private val scope = CoroutineScope(Dispatchers.IO)
    init {
        val server = HttpServer.create(InetSocketAddress(port), 0)
        server.createContext("/", this)
        server.start()
    }
    override suspend fun actions(): Set<TSAction> {
        return setOf(
            TSAction(closeAct, ctx.mkTrue(), TSAction.SyncRole.P2PService),
        )
    }
    override suspend fun transit(act: ConcreteAction) {}
    override fun getContext() = ctx
    override fun handle(exchange: HttpExchange?) {
        val resource = HttpResource(exchange!!)
        scope.launch { Proc(resource, staticInfo(), actionTable).run() }
    }

    class HttpResource(
        private val exchange: HttpExchange,
    ) : TransitionSystem {
        private val ctx = Context()
        private val request: HttpServerRequest
        private var initHttpReq = true
        private var finishHttpReq = true
        init {
            val path = httpPathFromUriPath(exchange.requestURI.path ?: "")
            val body = exchange.requestBody.bufferedReader().use { it.readText() }
            request = HttpServerRequest(path, body)
        }
        override suspend fun actions(): Set<TSAction> {
            if (initHttpReq) {
                initHttpReq = false
                return setOf(
                    TSAction(
                        receiveRequestAct,
                        ctx.mkEq(
                            reqArg.toZ3Expr(ctx),
                            httpServerRequestType.toZ3Expr(Value(request, httpServerRequestType), ctx),
                        ),
                    ),
                )
            } else if (finishHttpReq) {
                finishHttpReq = false
                return setOf(
                    TSAction(
                        sendResponseAct,
                        ctx.mkEq(
                            reqArg.toZ3Expr(ctx),
                            httpServerRequestType.toZ3Expr(Value(request, httpServerRequestType), ctx),
                        ),
                    ),
                )
            } else {
                return setOf()
            }
        }
        override suspend fun transit(act: ConcreteAction) {
            if (act.symAction == sendResponseAct) {
                val resp = act.lookup(respArg).value as HttpServerResponse
                exchange.sendResponseHeaders(resp.code, resp.body.length.toLong())
                exchange.responseBody.writer().use { writer ->
                    writer.write(resp.body)
                }
            }
        }
        override fun getContext() = ctx
    }
}
