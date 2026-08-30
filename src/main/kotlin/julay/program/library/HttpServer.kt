package julay.program.library

import com.microsoft.z3.Context
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import julay.compiler.LibraryLoc
import julay.compiler.decl.ActionDecl
import julay.program.Program
import julay.program.Proc
import julay.program.TransitionSystem
import julay.program.TransitionSystemStaticInfo
import julay.program.Variable
import julay.program.action.ConcreteAction
import julay.program.action.SymbolicAction
import julay.program.action.TSAction
import julay.program.sync.FastOffer
import julay.program.sync.BoolExprFast
import julay.program.sync.SyncStepPlan
import julay.program.type.intType
import julay.program.type.stringType
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets.UTF_8

/**
 * HTTP server library. [listen] registers a procfun handler; each JDK request invokes
 * [Program.invokeProcFun] directly. Lifecycle actions are session syncs (startup pairing).
 */
class JulHttpServer(
    private val program: Program,
) : TransitionSystem, HttpHandler {
    companion object : JulLibrary {
        override val julName = "HttpServer"
        val portArg = Variable("port", intType)
        // Procfun refs erase to String at runtime; must match Jul codegen SymbolicAction args.
        val handlerArg = Variable("handler", stringType)
        val listenAct = SymbolicAction("listen", listOf(portArg, handlerArg), isSession = true)
        val closeAct = SymbolicAction("close", listOf(), isSession = true)
        val listenCtor: Pair<SymbolicAction, suspend (Program, ConcreteAction) -> JulHttpServer> = Pair(
            listenAct,
        ) { prog, _ ->
            JulHttpServer(prog)
        }
        override fun staticInfo() = TransitionSystemStaticInfo(
            "JulHttpServer$",
            setOf(closeAct),
            mapOf(listenCtor),
        )
        override val actionDecls = listOf(
            ActionDecl(listenAct, listOf(), emptyList(), TSAction.SyncRole.Default, LibraryLoc(julName)),
            ActionDecl(
                closeAct,
                listOf(),
                emptyList(),
                TSAction.SyncRole.Default,
                LibraryLoc(julName),
            ),
        )
    }

    private var handlerName: String? = null
    private var jdkServer: HttpServer? = null
    private var closed = false

    override suspend fun finishConstruction(act: ConcreteAction) {
        val listenPort = act.lookup(portArg).value as Int
        handlerName = act.lookup(handlerArg).value as String
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
            TSAction(closeAct, ctx.mkTrue(), TSAction.SyncRole.Default, fastGuard = BoolExprFast.True),
        )
    }

    override fun syncStepPlan(): SyncStepPlan {
        if (closed) {
            return SyncStepPlan.FastOnly(emptyList())
        }
        return SyncStepPlan.FastOnly(
            listOf(FastOffer(closeAct, BoolExprFast.True, TSAction.SyncRole.Default)),
        )
    }

    override suspend fun transit(act: ConcreteAction) {
        if (act.symAction.name == closeAct.name) {
            closed = true
            jdkServer?.stop(0)
        }
    }

    override fun handle(exchange: HttpExchange?) {
        val name = handlerName
            ?: throw IllegalStateException("JulHttpServer handle before listen")
        val path = httpPathFromUriPath(exchange!!.requestURI.path ?: "")
        val body = String(exchange.requestBody.readAllBytes(), UTF_8)
        val req = HttpServerRequest(path, body)
        val resp = program.invokeProcFunBlocking(name, listOf(req)) as HttpServerResponse
        val respBytes = resp.body.toByteArray(UTF_8)
        exchange.sendResponseHeaders(resp.code, respBytes.size.toLong())
        exchange.responseBody.use { it.write(respBytes) }
    }
}
