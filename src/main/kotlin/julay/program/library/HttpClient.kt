package julay.program.library

import io.github.cvc5.Kind
import io.github.cvc5.TermManager
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

class JulHttpClient(
    private val request: HttpClientRequest,
) : TransitionSystem {
    companion object : JulLibrary {
        override val julName = "HttpClient"
        val reqArg = Variable("req", httpClientRequestType)
        val respArg = Variable("resp", httpClientResponseType)
        val sendRequestAct = SymbolicAction("sendRequest", listOf(reqArg))
        val sendRequestCtor: Pair<SymbolicAction, suspend (Program, ConcreteAction) -> JulHttpClient> = Pair(
            sendRequestAct,
        ) { _, act ->
            val req = act.lookup(reqArg).value as HttpClientRequest
            JulHttpClient(req)
        }
        val receiveResponseAct = SymbolicAction("receiveResponse", listOf(respArg))
        // the $ in the name means that programs cannot create p-classes whose names conflict with this one
        override fun staticInfo() = TransitionSystemStaticInfo(
            "JulHttpClient$",
            setOf(receiveResponseAct),
            mapOf(sendRequestCtor),
        )
        override val actionDecls = listOf(
            ActionDecl(sendRequestAct, listOf(), emptyList(), TSAction.SyncRole.CSP, LibraryLoc(julName)),
            ActionDecl(receiveResponseAct, listOf(), emptyList(), TSAction.SyncRole.CSP, LibraryLoc(julName)),
        )
    }

    private var recResp = true
    private var response: HttpClientResponse

    init {
        val client = HttpClient.newBuilder().build()
        val builder = HttpRequest.newBuilder().uri(URI.create(request.url))
        val method = request.method.uppercase()
        val jdkRequest = when (method) {
            "GET", "HEAD" -> builder.method(method, BodyPublishers.noBody()).build()
            else -> builder.method(method, BodyPublishers.ofString(request.body)).build()
        }
        val jdkResponse = client.send(jdkRequest, BodyHandlers.ofString())
        response = HttpClientResponse(jdkResponse.body(), jdkResponse.statusCode())
    }

    override suspend fun actions(tm: TermManager): Set<TSAction> {
        return if (recResp) {
            recResp = false
            setOf(
                TSAction(
                    receiveResponseAct,
                    tm.mkTerm(
                        Kind.EQUAL,
                        respArg.toSmtTerm(tm),
                        httpClientResponseType.toSmtTerm(Value(response, httpClientResponseType), tm),
                    ),
                ),
            )
        } else {
            setOf()
        }
    }
    override suspend fun transit(act: ConcreteAction) {
        // Response is already materialized in init; sync only delivers it to peers.
    }
}
