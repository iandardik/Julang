package julay.program.library

import com.microsoft.z3.Context
import julay.ast.ActionDecl
import julay.ast.LibraryLoc
import julay.program.*
import julay.tools.mkStringConst

class ReadlnTS : TransitionSystem {
    companion object: StaticInfo {
        val msgArg = Variable("msg", stringType)
        val promptAct = SymbolicAction("prompt", listOf(), SymbolicAction.SyncType.P2P)
        val readlnAct = SymbolicAction("readln", listOf(msgArg), SymbolicAction.SyncType.P2P)
        val initiallyCtor = Pair(SymbolicAction("initially", listOf())) { _ : Program, _ : ConcreteAction -> ReadlnTS() }
        // the $ in the name means that programs cannot create p-classes whose names conflict with this one
        override fun staticInfo() = TransitionSystemStaticInfo(
            "ReadlnTS$",
            setOf(promptAct, readlnAct),
            mapOf(initiallyCtor))
        val actionDecls = listOf(
            ActionDecl(readlnAct, listOf(), mapOf(), TSAction.SyncRole.P2PService, LibraryLoc("Readln")),
            ActionDecl(promptAct, listOf(), mapOf(), TSAction.SyncRole.P2PService, LibraryLoc("Readln")),
        )
    }

    private val ctx = Context()
    private var prompt = true
    private var msg = ""

    override suspend fun actions() = setOf(
        TSAction(
            promptAct,
            ctx.mkEq(ctx.mkBool(prompt), ctx.mkBool(true)),
            TSAction.SyncRole.P2PService
        ),
        TSAction(
            readlnAct,
            ctx.mkAnd(
                ctx.mkEq(ctx.mkBool(prompt), ctx.mkBool(false)),
                ctx.mkEq(ctx.mkStringConst("msg"), ctx.mkString(msg))
            ),
            TSAction.SyncRole.P2PService
        ),
    )
    override suspend fun transit(act: ConcreteAction) {
        if (act.symAction == promptAct) {
            prompt = false
            msg = readln()
        }
        else {
            prompt = true
        }
    }
    override fun getContext() = ctx
}
