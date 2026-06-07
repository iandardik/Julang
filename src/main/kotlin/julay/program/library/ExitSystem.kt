package julay.program.library

import com.microsoft.z3.Context
import julay.ast.ActionDecl
import julay.ast.LibraryLoc
import julay.program.*
import kotlin.system.exitProcess

class ExitSystemTS : TransitionSystem {
    companion object: JulLibrary {
        override val julName = "ExitSystem"
        val exitSystemAct = SymbolicAction("exitSystem", listOf(), SymbolicAction.SyncType.P2P)
        val initiallyCtor = Pair(SymbolicAction("initially", listOf())) { _ : Program, _ : ConcreteAction -> ExitSystemTS() }
        // the $ in the name means that programs cannot create p-classes whose names conflict with this one
        override fun staticInfo() = TransitionSystemStaticInfo(
            "ExitSystemTS$",
            setOf(exitSystemAct),
            mapOf(initiallyCtor))
        override val actionDecls = listOf(
            ActionDecl(exitSystemAct, listOf(), mapOf(), TSAction.SyncRole.P2PService, LibraryLoc(julName)),
        )
    }

    private val ctx = Context()
    private val alphabet = setOf(TSAction(exitSystemAct, ctx.mkTrue(), TSAction.SyncRole.P2PService))
    override suspend fun actions() = alphabet
    override suspend fun transit(act: ConcreteAction) {
        exitProcess(0)
    }
    override fun getContext() = ctx
}
