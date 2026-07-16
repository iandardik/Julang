package julay.program.library

import com.microsoft.z3.Context
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import julay.compiler.decl.ActionDecl
import julay.compiler.LibraryLoc
import julay.program.*
import java.net.InetSocketAddress

class JulHttpServer(
    private val port: Int,
    private val program: Program,
) : TransitionSystem, HttpHandler {
    companion object : JulLibrary {
        override val julName = "HttpServer"
        val portArg = Variable("port", intType)
        val reqArg = Variable("req", httpServerRequestType)
        val sendResponseChanType = channelType("sendResponse")
        val httpChanArg = Variable("httpChan", sendResponseChanType)
        val respArg = Variable("resp", httpServerResponseType)
        val createHttpServerAct = SymbolicAction("createHttpServer", listOf(portArg))
        val receiveRequestAct = SymbolicAction("receiveRequest", listOf(reqArg, httpChanArg))
        val sendResponseAct = SymbolicAction("sendResponse", listOf(respArg))
        val closeAct = SymbolicAction("close", listOf())
        val createHttpServerCtor: Pair<SymbolicAction, suspend (Program, ConcreteAction) -> JulHttpServer> = Pair(
            createHttpServerAct,
        ) { prog, act ->
            val port = act.lookup(portArg).value as Int
            JulHttpServer(port, prog)
        }
        // the $ in the name means that programs cannot create p-classes whose names conflict with this one
        override fun staticInfo() = TransitionSystemStaticInfo(
            "JulHttpServer$",
            setOf(receiveRequestAct, sendResponseAct, closeAct),
            mapOf(createHttpServerCtor),
            dynamicChannelActions = setOf(sendResponseAct),
        )
        override val actionDecls = listOf(
            ActionDecl(createHttpServerAct, listOf(), emptyList(), TSAction.SyncRole.Default, LibraryLoc(julName)),
            ActionDecl(receiveRequestAct, listOf(), emptyList(), TSAction.SyncRole.Default, LibraryLoc(julName)),
            ActionDecl(
                sendResponseAct,
                listOf(),
                emptyList(),
                TSAction.SyncRole.Default,
                LibraryLoc(julName),
                dynamicChannelVar = "",
            ),
            ActionDecl(closeAct, listOf(), emptyList(), TSAction.SyncRole.Service, LibraryLoc(julName)),
        )
    }

    init {
        val server = HttpServer.create(InetSocketAddress(port), 0)
        server.createContext("/", this)
        if (System.getProperty("julay.channelDebug") == "true") {
            server.createContext("/.julay/openDynamicChannels") { exchange ->
                val body = program.totalOpenDynamicChannelCount().toString().toByteArray()
                exchange.sendResponseHeaders(200, body.size.toLong())
                exchange.responseBody.use { it.write(body) }
            }
        }
        server.start()
    }
    override suspend fun actions(ctx: Context): Set<TSAction> {
        return setOf(
            TSAction(closeAct, ctx.mkTrue(), TSAction.SyncRole.Service),
        )
    }
    override suspend fun transit(act: ConcreteAction) {}
    override fun handle(exchange: HttpExchange?) {
        val resource = HttpResource(exchange!!, program)
        program.spawnProc(resource, staticInfo())
    }

    class HttpResource(
        private val exchange: HttpExchange,
        private val program: Program,
    ) : TransitionSystem {
        private val request: HttpServerRequest
        private val responseChan: Channel = program.createDynamicChannel(sendResponseAct)
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
                        ctx.mkAnd(
                            ctx.mkEq(
                                reqArg.toZ3Expr(ctx),
                                httpServerRequestType.toZ3Expr(Value(request, httpServerRequestType), ctx),
                            ),
                            ctx.mkEq(
                                httpChanArg.toZ3Expr(ctx),
                                sendResponseChanType.toZ3Expr(Value(responseChan, sendResponseChanType), ctx),
                            ),
                        ),
                    ),
                )
            } else if (finishHttpReq) {
                finishHttpReq = false
                return setOf(
                    TSAction(
                        sendResponseAct,
                        ctx.mkTrue(),
                        channel = responseChan,
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
                closeChannel(responseChan)
            }
        }
    }
}
