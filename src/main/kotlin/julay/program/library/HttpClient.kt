package julay.program.library

import com.microsoft.z3.Context
import julay.compiler.decl.ActionDecl
import julay.compiler.LibraryLoc
import julay.program.ConcreteAction
import julay.program.Program
import julay.program.SymbolicAction
import julay.program.TSAction
import julay.program.TransitionSystem
import julay.program.TransitionSystemStaticInfo
import julay.program.Value
import julay.program.Variable
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse.BodyHandlers

/**
 * Long-lived outbound HTTP client. [createHttpClient] constructs one process that owns a single JDK
 * [HttpClient]; [sendRequest] / [receiveResponse] may be repeated on that process.
 */
class JulHttpClient : TransitionSystem {
    companion object : JulLibrary {
        override val julName = "HttpClient"
        val reqArg = Variable("req", httpClientRequestType)
        val respArg = Variable("resp", httpClientResponseType)
        val createHttpClientAct = SymbolicAction("createHttpClient", listOf())
        val sendRequestAct = SymbolicAction("sendRequest", listOf(reqArg))
        val receiveResponseAct = SymbolicAction("receiveResponse", listOf(respArg))
        val createHttpClientCtor: Pair<SymbolicAction, suspend (Program, ConcreteAction) -> JulHttpClient> = Pair(
            createHttpClientAct,
        ) { _, _ ->
            JulHttpClient()
        }
        // the $ in the name means that programs cannot create p-classes whose names conflict with this one
        override fun staticInfo() = TransitionSystemStaticInfo(
            "JulHttpClient$",
            setOf(sendRequestAct, receiveResponseAct),
            mapOf(createHttpClientCtor),
        )
        override val actionDecls = listOf(
            ActionDecl(createHttpClientAct, listOf(), emptyList(), TSAction.SyncRole.Default, LibraryLoc(julName)),
            ActionDecl(sendRequestAct, listOf(), emptyList(), TSAction.SyncRole.Default, LibraryLoc(julName)),
            ActionDecl(receiveResponseAct, listOf(), emptyList(), TSAction.SyncRole.Default, LibraryLoc(julName)),
        )
    }

    private enum class Phase { Idle, HaveResponse }

    private val jdkClient = HttpClient.newBuilder().build()
    private var phase = Phase.Idle
    private var response: HttpClientResponse? = null

    override suspend fun actions(ctx: Context): Set<TSAction> {
        return when (phase) {
            Phase.Idle -> setOf(TSAction(sendRequestAct, ctx.mkTrue()))
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
