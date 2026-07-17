package julay.program.library

import com.microsoft.z3.Context
import julay.compiler.decl.ActionDecl
import julay.compiler.LibraryLoc
import julay.program.Program
import julay.program.TransitionSystem
import julay.program.TransitionSystemStaticInfo
import julay.program.Value
import julay.program.Variable
import julay.program.action.ConcreteAction
import julay.program.action.SymbolicAction
import julay.program.action.TSAction
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse.BodyHandlers

/**
 * Long-lived outbound HTTP client. [createHttpClient] constructs one process that owns a single JDK
 * [HttpClient]; [sendRequest] / [receiveResponse] may be repeated on that process.
 * Request/response and close are session actions (affinity + sessions; no Julay Channel values).
 */
class JulHttpClient(
    private val program: Program,
) : TransitionSystem {
    companion object : JulLibrary {
        override val julName = "HttpClient"
        val reqArg = Variable("req", httpClientRequestType)
        val respArg = Variable("resp", httpClientResponseType)
        val createHttpClientAct = SymbolicAction("createHttpClient", listOf(), isSession = true)
        val sendRequestAct = SymbolicAction("sendRequest", listOf(reqArg), isSession = true)
        val receiveResponseAct = SymbolicAction("receiveResponse", listOf(respArg), isSession = true)
        val closeHttpClientAct = SymbolicAction("closeHttpClient", listOf(), isSession = true)
        val createHttpClientCtor: Pair<SymbolicAction, suspend (Program, ConcreteAction) -> JulHttpClient> = Pair(
            createHttpClientAct,
        ) { prog, _ ->
            JulHttpClient(prog)
        }
        override fun staticInfo() = TransitionSystemStaticInfo(
            "JulHttpClient$",
            setOf(sendRequestAct, receiveResponseAct, closeHttpClientAct),
            mapOf(createHttpClientCtor),
        )
        override val actionDecls = listOf(
            ActionDecl(createHttpClientAct, listOf(), emptyList(), TSAction.SyncRole.Default, LibraryLoc(julName)),
            ActionDecl(
                sendRequestAct,
                listOf(),
                emptyList(),
                TSAction.SyncRole.Default,
                LibraryLoc(julName),
            ),
            ActionDecl(
                receiveResponseAct,
                listOf(),
                emptyList(),
                TSAction.SyncRole.Default,
                LibraryLoc(julName),
            ),
            ActionDecl(
                closeHttpClientAct,
                listOf(),
                emptyList(),
                TSAction.SyncRole.Default,
                LibraryLoc(julName),
            ),
        )
    }

    private enum class Phase { Idle, HaveResponse, Closed }

    private val jdkClient = HttpClient.newBuilder().build()
    private var phase = Phase.Idle
    private var response: HttpClientResponse? = null

    override suspend fun actions(ctx: Context): Set<TSAction> {
        return when (phase) {
            Phase.Closed -> emptySet()
            Phase.Idle -> setOf(
                TSAction(sendRequestAct, ctx.mkTrue()),
                TSAction(closeHttpClientAct, ctx.mkTrue(), TSAction.SyncRole.Default),
            )
            Phase.HaveResponse -> {
                val resp = response!!
                setOf(
                    TSAction(
                        receiveResponseAct,
                        ctx.mkEq(
                            respArg.toZ3Expr(ctx),
                            httpClientResponseType.toZ3Expr(Value(resp, httpClientResponseType), ctx),
                        ),
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
            }
            closeHttpClientAct -> {
                phase = Phase.Closed
                response = null
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
