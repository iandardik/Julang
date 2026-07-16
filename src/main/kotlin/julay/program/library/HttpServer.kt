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
        val createHttpServerAct = SymbolicAction("createHttpServer", listOf(portArg))
        val receiveRequestAct = SymbolicAction("receiveRequest", listOf(reqArg))
        // req correlates the response with the exchange that received it (avoids cross-wiring concurrent handlers)
        val sendResponseAct = SymbolicAction("sendResponse", listOf(reqArg, respArg))
        val closeAct = SymbolicAction("close", listOf())
        val createHttpServerCtor: Pair<SymbolicAction, suspend (Program, ConcreteAction) -> JulHttpServer> = Pair(
            createHttpServerAct,
        ) { prog, act ->
            val port = act.lookup(portArg).value as Int
            JulHttpServer(port, prog.actionTable)
        }
        // the $ in the name means that programs cannot create p-classes whose names conflict with this one
        override fun staticInfo() = TransitionSystemStaticInfo(
            "JulHttpServer$",
            setOf(receiveRequestAct, sendResponseAct, closeAct),
            mapOf(createHttpServerCtor),
        )
        override val actionDecls = listOf(
            ActionDecl(createHttpServerAct, listOf(), emptyList(), TSAction.SyncRole.Default, LibraryLoc(julName)),
            ActionDecl(receiveRequestAct, listOf(), emptyList(), TSAction.SyncRole.Default, LibraryLoc(julName)),
            ActionDecl(sendResponseAct, listOf(), emptyList(), TSAction.SyncRole.Default, LibraryLoc(julName)),
            ActionDecl(closeAct, listOf(), emptyList(), TSAction.SyncRole.Service, LibraryLoc(julName)),
        )
    }

    private val scope = CoroutineScope(Dispatchers.IO)
    init {
        val server = HttpServer.create(InetSocketAddress(port), 0)
        server.createContext("/", this)
        server.start()
    }
    override suspend fun actions(ctx: Context): Set<TSAction> {
        return setOf(
            TSAction(closeAct, ctx.mkTrue(), TSAction.SyncRole.Service),
        )
    }
    override suspend fun transit(act: ConcreteAction) {}
    override fun handle(exchange: HttpExchange?) {
        val resource = HttpResource(exchange!!)
        scope.launch { Proc(resource, staticInfo(), actionTable).run() }
    }

    class HttpResource(
        private val exchange: HttpExchange,
    ) : TransitionSystem {
        private val request: HttpServerRequest
        private var initHttpReq = true
        private var finishHttpReq = true
        init {
            val path = httpPathFromUriPath(exchange.requestURI.path ?: "")
            val body = exchange.requestBody.bufferedReader().use { it.readText() }
            request = HttpServerRequest(path, body)
        }
        override suspend fun actions(ctx: Context): Set<TSAction> {
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
    }
}
