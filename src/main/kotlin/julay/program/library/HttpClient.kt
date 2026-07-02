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
import julay.program.Variable
import julay.program.stringType
import julay.tools.mkStringConst
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse.BodyHandlers

class JulHttpClient(
    private val sendBody : String
) : TransitionSystem {
    companion object: JulLibrary {
        override val julName = "HttpClient"
        val sendBodyArg = Variable("sendBody", stringType)
        val respBodyArg = Variable("respBody", stringType)
        val sendRequestAct = SymbolicAction("sendRequest", listOf(sendBodyArg))
        val sendRequestCtor: Pair<SymbolicAction, suspend (Program, ConcreteAction) -> JulHttpClient> = Pair(
            sendRequestAct,
        ) { _, act ->
            val sendBody = act.lookup(sendBodyArg).value as String
            JulHttpClient(sendBody)
        }
        val receiveResponseAct = SymbolicAction("receiveResponse", listOf(respBodyArg))
        // the $ in the name means that programs cannot create p-classes whose names conflict with this one
        override fun staticInfo() = TransitionSystemStaticInfo(
            "JulHttpClient$",
            setOf(receiveResponseAct),
            mapOf(sendRequestCtor))
        override val actionDecls = listOf(
            ActionDecl(sendRequestAct, listOf(), mapOf(), TSAction.SyncRole.CSP, LibraryLoc(julName)),
            ActionDecl(receiveResponseAct, listOf(), mapOf(), TSAction.SyncRole.CSP, LibraryLoc(julName)),
        )
    }

    private val ctx = Context()
    private val z3True = ctx.mkTrue()
    private var recResp = true
    private var respBody : String

    init {
        val client = HttpClient.newBuilder()
            //.version(Runtime.Version.HTTP_1_1)
            //.followRedirects(Redirect.NORMAL)
            //.connectTimeout(Duration.ofSeconds(20))
            //.proxy(ProxySelector.of(new InetSocketAddress("proxy.example.com", 80)))
            //.authenticator(Authenticator.getDefault())
            .build();
        val request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:8000"))
            //.timeout(Duration.ofMinutes(2))
            //.header("Content-Type", "application/json")
            //.POST(BodyPublishers.ofFile(Paths.get("file.json")))
            .POST(BodyPublishers.ofString(sendBody))
            .build()
        val response = client.send(request, BodyHandlers.ofString())
        respBody = response.body()
        //System.out.println(response.statusCode());
        //System.out.println(response.body());
    }

    override suspend fun actions() : Set<TSAction> {
        return if (recResp) {
            recResp = false
            setOf(
                TSAction(
                    receiveResponseAct,
                    ctx.mkEq(ctx.mkStringConst(respBodyArg.name),ctx.mkString(respBody))
                )
            )
        } else {
            setOf()
        }
    }
    override suspend fun transit(act: ConcreteAction) {
        when (act.symAction) {
            sendRequestAct -> {
            }
            else -> RuntimeException("Unsupported action ${act.symAction}")
        }
    }
    override fun getContext() = ctx
}
