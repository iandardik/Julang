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
    private val closeChan: Channel,
) : TransitionSystem, HttpHandler {
    companion object : JulLibrary {
        override val julName = "HttpServer"
        val portArg = Variable("port", intType)
        val closeHttpServerChanType = channelType("closeHttpServer")
        val closeChanArg = Variable("closeChan", closeHttpServerChanType)
        val reqArg = Variable("req", httpServerRequestType)
        val sendResponseChanType = channelType("sendResponse")
        val httpChanArg = Variable("httpChan", sendResponseChanType)
        val respArg = Variable("resp", httpServerResponseType)
        val createHttpServerAct = SymbolicAction("createHttpServer", listOf(portArg, closeChanArg))
        val receiveRequestAct = SymbolicAction("receiveRequest", listOf(reqArg, httpChanArg))
        val sendResponseAct = SymbolicAction("sendResponse", listOf(respArg))
        val closeHttpServerAct = SymbolicAction("closeHttpServer", listOf())
        val createHttpServerCtor: Pair<SymbolicAction, suspend (Program, ConcreteAction) -> JulHttpServer> = Pair(
            createHttpServerAct,
        ) { prog, act ->
            val port = act.lookup(portArg).value as Int
            val closeChan = act.lookup(closeChanArg).value as Channel
            JulHttpServer(port, prog, closeChan)
        }
        // the $ in the name means that programs cannot create p-classes whose names conflict with this one
        override fun staticInfo() = TransitionSystemStaticInfo(
            "JulHttpServer$",
            setOf(receiveRequestAct, sendResponseAct, closeHttpServerAct),
            mapOf(createHttpServerCtor),
            dynamicChannelActions = setOf(sendResponseAct, closeHttpServerAct),
        )
        override val actionDecls = listOf(
            ActionDecl(createHttpServerAct, listOf(), emptyList(), TSAction.SyncRole.Default, LibraryLoc(julName)),
            ActionDecl(
                receiveRequestAct,
                listOf(),
                emptyList(),
                TSAction.SyncRole.Default,
                LibraryLoc(julName),
                constrainedChannelArgs = setOf("httpChan"),
            ),
            ActionDecl(
                sendResponseAct,
                listOf(),
                emptyList(),
                TSAction.SyncRole.Default,
                LibraryLoc(julName),
                dynamicChannelVar = "",
            ),
            ActionDecl(
                closeHttpServerAct,
                listOf(),
                emptyList(),
                TSAction.SyncRole.Default,
                LibraryLoc(julName),
                dynamicChannelVar = "",
            ),
        )
    }

    private val jdkServer = HttpServer.create(InetSocketAddress(port), 0)
    private var closed = false

    init {
        jdkServer.createContext("/", this)
        if (System.getProperty("julay.channelDebug") == "true") {
            jdkServer.createContext("/.julay/openDynamicChannels") { exchange ->
                val body = program.totalOpenDynamicChannelCount().toString().toByteArray()
                exchange.sendResponseHeaders(200, body.size.toLong())
                exchange.responseBody.use { it.write(body) }
            }
        }
        jdkServer.start()
    }

    override fun heldChannels(): Set<Channel> =
        if (closed || closeChan.isEmpty()) emptySet() else setOf(closeChan)

    override suspend fun actions(ctx: Context): Set<TSAction> {
        if (closed || closeChan.isEmpty()) {
            return emptySet()
        }
        return setOf(
            TSAction(closeHttpServerAct, ctx.mkTrue(), TSAction.SyncRole.Default, channel = closeChan),
        )
    }

    override suspend fun transit(act: ConcreteAction) {
        if (act.symAction == closeHttpServerAct) {
            closed = true
            jdkServer.stop(0)
            closeChannel(closeChan)
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
        private val responseChan: Channel = program.createDynamicChannel(sendResponseAct)
        private var initHttpReq = true
        private var finishHttpReq = true
        init {
            val path = httpPathFromUriPath(exchange.requestURI.path ?: "")
            val body = exchange.requestBody.bufferedReader().use { it.readText() }
            request = HttpServerRequest(path, body)
        }

        override fun heldChannels(): Set<Channel> = setOf(responseChan)

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
