package julay.program

import com.microsoft.z3.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ConstructorTransitionSystem(
    private val initiallyAction: TSAction,
    private val constructorsInfo: Set<TransitionSystemStaticInfo>,
    private val program: Program,
    private val ctx: Context,
    // TODO input the cli args into initially (accept them here as a constructor arg)
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

    private val z3True = ctx.mkTrue()
    private val nonInitiallyConstructorActions = constructorsInfo
        .asSequence()
        .flatMap { info -> info.constructors.keys }
        .filter { act -> act != initiallyAction.symAction }
        .map { act -> TSAction(act, z3True) }
        .toSet()
        .plus(TSAction(deadlockAct, ctx.mkFalse()))
    private var initially = true

    override suspend fun actions(): Set<TSAction> {
        return if (initially) {
            initially = false
            setOf(initiallyAction)
        } else {
            nonInitiallyConstructorActions
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

    override fun getContext() = ctx
}
