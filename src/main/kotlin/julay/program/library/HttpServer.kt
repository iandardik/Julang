package julay.program.library

import com.microsoft.z3.Context
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import julay.compiler.decl.ActionDecl
import julay.compiler.LibraryLoc
import julay.program.*
import julay.program.action.*
import julay.program.type.intType
import java.net.InetSocketAddress

/**
 * HTTP server library. Request/response and close are [session] actions: sticky pairing uses
 * process-local affinity and SyncChannel sessions (no Julay Channel values).
 *
 * The JDK server is created and started in [finishConstruction] on the child proc, not during
 * the parent's spawn allocation.
 */
class JulHttpServer(
    private val program: Program,
) : TransitionSystem, HttpHandler {
    companion object : JulLibrary {
        override val julName = "HttpServer"
        val portArg = Variable("port", intType)
        val reqArg = Variable("req", httpServerRequestType)
        val respArg = Variable("resp", httpServerResponseType)
        val createHttpServerAct = SymbolicAction("createHttpServer", listOf(portArg), isSession = true)
        val receiveRequestAct = SymbolicAction("receiveRequest", listOf(reqArg), isSession = true)
        val sendResponseAct = SymbolicAction("sendResponse", listOf(respArg), isSession = true)
        val closeHttpServerAct = SymbolicAction("closeHttpServer", listOf(), isSession = true)
        val createHttpServerCtor: Pair<SymbolicAction, suspend (Program, ConcreteAction) -> JulHttpServer> = Pair(
            createHttpServerAct,
        ) { prog, _ ->
            JulHttpServer(prog)
        }
        override fun staticInfo() = TransitionSystemStaticInfo(
            "JulHttpServer$",
            setOf(receiveRequestAct, sendResponseAct, closeHttpServerAct),
            mapOf(createHttpServerCtor),
        )
        override val actionDecls = listOf(
            ActionDecl(createHttpServerAct, listOf(), emptyList(), TSAction.SyncRole.Default, LibraryLoc(julName)),
            ActionDecl(
                receiveRequestAct,
                listOf(),
                emptyList(),
                TSAction.SyncRole.Default,
                LibraryLoc(julName),
            ),
            ActionDecl(
                sendResponseAct,
                listOf(),
                emptyList(),
                TSAction.SyncRole.Default,
                LibraryLoc(julName),
            ),
            ActionDecl(
                closeHttpServerAct,
                listOf(),
                emptyList(),
                TSAction.SyncRole.Default,
                LibraryLoc(julName),
            ),
        )
    }

    private var port: Int? = null
    private var jdkServer: HttpServer? = null
    private var closed = false

    override suspend fun finishConstruction(act: ConcreteAction) {
        val listenPort = act.lookup(portArg).value as Int
        port = listenPort
        val server = HttpServer.create(InetSocketAddress(listenPort), 0)
        server.createContext("/", this)
        server.start()
        jdkServer = server
    }

    override suspend fun actions(ctx: Context): Set<TSAction> {
        if (closed) {
            return emptySet()
        }
        return setOf(
            TSAction(closeHttpServerAct, ctx.mkTrue(), TSAction.SyncRole.Default),
        )
    }

    override suspend fun transit(act: ConcreteAction) {
        if (act.symAction == closeHttpServerAct) {
            closed = true
            jdkServer?.stop(0)
        }
    }

    override fun handle(exchange: HttpExchange?) {
        val resource = HttpResource(exchange!!, program)
        program.spawnProc(resource, staticInfo())
    }

    class HttpResource(
        private val exchange: HttpExchange,
        private val program: Program,
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
                        ctx.mkTrue(),
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
