package julay.program

import com.microsoft.z3.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ConstructorTransitionSystem(
    private val initiallyAction: SymbolicAction,
    private val constructorsInfo: Set<TransitionSystemStaticInfo>,
    private val program: Program,
) : TransitionSystem {
    companion object : StaticInfo {
        // the $ in the name means that programs cannot create p-classes whose names conflict with this one
        // the alphabet info is not strictly correct, but it does not matter since it's never used
        override fun staticInfo() = TransitionSystemStaticInfo("ConstructorTS$", setOf(), mapOf())
        // this action will never sync with any other action, meaning that this constructor proc will never deadlock and
        // terminate
        val deadlockAct = SymbolicAction("deadlock", listOf())
    }

    // in the future, we may want to switch to Dispatchers.Default or make it possible for the programmer to choose the
    // dispatch type.
    private val scope = CoroutineScope(Dispatchers.IO)

    private var initially = true
    private var nonInitiallyConstructorActions: Set<TSAction>? = null

    override suspend fun actions(ctx: Context): Set<TSAction> {
        return if (initially) {
            initially = false
            setOf(TSAction(initiallyAction, ctx.mkTrue(), TSAction.SyncRole.CSP))
        } else {
            nonInitiallyConstructorActions ?: constructorsInfo
                .asSequence()
                .flatMap { info -> info.constructors.keys }
                .filter { act -> act != initiallyAction }
                .map { act -> TSAction(act, ctx.mkTrue()) }
                .toSet()
                .plus(TSAction(deadlockAct, ctx.mkFalse()))
                .also { nonInitiallyConstructorActions = it }
        }
    }

    override suspend fun transit(act: ConcreteAction) {
        constructorsInfo
            .forEach { tsInfo ->
                if (tsInfo.constructors.containsKey(act.symAction)) {
                    val constructor = tsInfo.constructors[act.symAction]!!
                    scope.launch {
                        val ts = constructor(program, act)
                        Proc(ts, tsInfo, program.actionTable).run()
                    }
                }
            }
    }
}
