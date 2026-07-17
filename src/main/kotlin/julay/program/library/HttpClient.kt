package julay.program.library

import com.microsoft.z3.Context
import julay.compiler.decl.ActionDecl
import julay.compiler.LibraryLoc
import julay.program.Channel
import julay.program.Program
import julay.program.TransitionSystem
import julay.program.TransitionSystemStaticInfo
import julay.program.Value
import julay.program.Variable
import julay.program.action.ConcreteAction
import julay.program.action.SymbolicAction
import julay.program.action.TSAction
import julay.program.channelType
import julay.program.closeChannel
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse.BodyHandlers

/**
 * Long-lived outbound HTTP client. [createHttpClient] constructs one process that owns a single JDK
 * [HttpClient]; [sendRequest] / [receiveResponse] may be repeated on that process.
 * Each request allocates a dynamic [Channel] for the response; the client closes it after receive.
 * [closeHttpClient] tears down the client on the Jul-provided close channel.
 */
class JulHttpClient(
    private val program: Program,
    private val closeChan: Channel,
) : TransitionSystem {
    companion object : JulLibrary {
        override val julName = "HttpClient"
        val closeHttpClientChanType = channelType("closeHttpClient")
        val closeChanArg = Variable("closeChan", closeHttpClientChanType)
        val reqArg = Variable("req", httpClientRequestType)
        val receiveResponseChanType = channelType("receiveResponse")
        val httpChanArg = Variable("httpChan", receiveResponseChanType)
        val respArg = Variable("resp", httpClientResponseType)
        val createHttpClientAct = SymbolicAction("createHttpClient", listOf(closeChanArg))
        val sendRequestAct = SymbolicAction("sendRequest", listOf(reqArg, httpChanArg))
        val receiveResponseAct = SymbolicAction("receiveResponse", listOf(respArg))
        val closeHttpClientAct = SymbolicAction("closeHttpClient", listOf())
        val createHttpClientCtor: Pair<SymbolicAction, suspend (Program, ConcreteAction) -> JulHttpClient> = Pair(
            createHttpClientAct,
        ) { prog, act ->
            val closeChan = act.lookup(closeChanArg).value as Channel
            JulHttpClient(prog, closeChan)
        }
        // the $ in the name means that programs cannot create p-classes whose names conflict with this one
        override fun staticInfo() = TransitionSystemStaticInfo(
            "JulHttpClient$",
            setOf(sendRequestAct, receiveResponseAct, closeHttpClientAct),
            mapOf(createHttpClientCtor),
            dynamicChannelActions = setOf(receiveResponseAct, closeHttpClientAct),
        )
        override val actionDecls = listOf(
            ActionDecl(createHttpClientAct, listOf(), emptyList(), TSAction.SyncRole.Default, LibraryLoc(julName)),
            ActionDecl(
                sendRequestAct,
                listOf(),
                emptyList(),
                TSAction.SyncRole.Default,
                LibraryLoc(julName),
                constrainedChannelArgs = setOf("httpChan"),
            ),
            ActionDecl(
                receiveResponseAct,
                listOf(),
                emptyList(),
                TSAction.SyncRole.Default,
                LibraryLoc(julName),
                dynamicChannelVar = "",
            ),
            ActionDecl(
                closeHttpClientAct,
                listOf(),
                emptyList(),
                TSAction.SyncRole.Default,
                LibraryLoc(julName),
                dynamicChannelVar = "",
            ),
        )
    }

    private enum class Phase { Idle, HaveResponse, Closed }

    private val jdkClient = HttpClient.newBuilder().build()
    private var phase = Phase.Idle
    private var response: HttpClientResponse? = null
    private var responseChan: Channel? = null

    override fun heldChannels(): Set<Channel> = buildSet {
        if (!closeChan.isEmpty() && phase != Phase.Closed) {
            add(closeChan)
        }
        responseChan?.let { add(it) }
    }

    override suspend fun actions(ctx: Context): Set<TSAction> {
        return when (phase) {
            Phase.Closed -> emptySet()
            Phase.Idle -> {
                val chan = responseChan ?: program.createDynamicChannel(receiveResponseAct).also {
                    responseChan = it
                }
                buildSet {
                    add(
                        TSAction(
                            sendRequestAct,
                            ctx.mkEq(
                                httpChanArg.toZ3Expr(ctx),
                                receiveResponseChanType.toZ3Expr(Value(chan, receiveResponseChanType), ctx),
                            ),
                        ),
                    )
                    if (!closeChan.isEmpty()) {
                        add(
                            TSAction(
                                closeHttpClientAct,
                                ctx.mkTrue(),
                                TSAction.SyncRole.Default,
                                channel = closeChan,
                            ),
                        )
                    }
                }
            }
            Phase.HaveResponse -> {
                val resp = response!!
                val chan = responseChan!!
                setOf(
                    TSAction(
                        receiveResponseAct,
                        ctx.mkEq(
                            respArg.toZ3Expr(ctx),
                            httpClientResponseType.toZ3Expr(Value(resp, httpClientResponseType), ctx),
                        ),
                        channel = chan,
                    ),
                )
            }
        }
    }

    override suspend fun transit(act: ConcreteAction) {
        when (act.symAction) {
            sendRequestAct -> {
                val request = act.lookup(reqArg).value as HttpClientRequest
                response = send(request)
                phase = Phase.HaveResponse
            }
            receiveResponseAct -> {
                response = null
                phase = Phase.Idle
                val chan = responseChan
                responseChan = null
                if (chan != null) {
                    closeChannel(chan)
                }
            }
            closeHttpClientAct -> {
                phase = Phase.Closed
                val pending = responseChan
                responseChan = null
                response = null
                if (pending != null) {
                    closeChannel(pending)
                }
                if (!closeChan.isEmpty()) {
                    closeChannel(closeChan)
                }
            }
        }
    }

    private fun send(request: HttpClientRequest): HttpClientResponse {
        val builder = HttpRequest.newBuilder().uri(URI.create(request.url))
        val method = request.method.uppercase()
        val jdkRequest = when (method) {
            "GET", "HEAD" -> builder.method(method, BodyPublishers.noBody()).build()
            else -> builder.method(method, BodyPublishers.ofString(request.body)).build()
        }
        val jdkResponse = jdkClient.send(jdkRequest, BodyHandlers.ofString())
        return HttpClientResponse(jdkResponse.body(), jdkResponse.statusCode())
    }
}
