package julay.program

import com.microsoft.z3.BoolExpr
import com.microsoft.z3.Context
import com.microsoft.z3.Status
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
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
    }

    // in the future, we may want to switch to Dispatchers.Default or make it possible for the programmer to choose the
    // dispatch type.
    private val scope = CoroutineScope(Dispatchers.IO)

    private var initially = true
    // Cache symbolic actions only — BoolExpr guards must be rebuilt each step because
    // Proc uses a fresh Z3 Context per step (caching TSAction caused Context mismatch).
    private var nonInitiallyConstructorEntries: List<Pair<SymbolicAction, TransitionSystemStaticInfo>>? = null

    override suspend fun actions(ctx: Context): Set<TSAction> {
        return if (initially) {
            initially = false
            setOf(TSAction(initiallyAction, ctx.mkTrue(), TSAction.SyncRole.Default))
        } else {
            val entries = nonInitiallyConstructorEntries ?: constructorsInfo
                .asSequence()
                .flatMap { info ->
                    info.constructors.keys
                        .filter { act -> act != initiallyAction }
                        .map { act -> act to info }
                }
                .toList()
                .also { nonInitiallyConstructorEntries = it }
            if (entries.isEmpty()) {
                // No further constructor offers: park so Program.run (this proc) does not return.
                // Child procs run on Dispatchers.IO (daemon threads); exiting main tears them down.
                // Intentional process end uses exitProcess from a child (e.g. ExitSystem).
                awaitCancellation()
            }
            entries
                .map { (act, info) ->
                    val guard = info.constructorGuards[act]?.invoke(ctx) ?: ctx.mkTrue()
                    TSAction(act, guard, TSAction.SyncRole.Default)
                }
                .toSet()
        }
    }

    override suspend fun transit(act: ConcreteAction) {
        constructorsInfo
            .forEach { tsInfo ->
                if (!tsInfo.constructors.containsKey(act.symAction)) {
                    return@forEach
                }
                // initially: channel guard is always true; filter spawns by each p-class's constructor guard
                if (act.symAction == initiallyAction && !constructorGuardSatisfied(tsInfo, act)) {
                    return@forEach
                }
                val constructor = tsInfo.constructors[act.symAction]!!
                scope.launch {
                    val ts = constructor(program, act)
                    Proc(ts, tsInfo, program.actionTable).run()
                }
            }
    }

    private fun constructorGuardSatisfied(tsInfo: TransitionSystemStaticInfo, act: ConcreteAction): Boolean {
        val guardBuilder = tsInfo.constructorGuards[act.symAction] ?: return true
        return Context().use { ctx ->
            val solver = ctx.mkSolver()
            solver.add(guardBuilder(ctx))
            act.symAction.args.forEach { v ->
                val value = act.lookup(v)
                solver.add(ctx.mkEq(v.toZ3Expr(ctx), value.toZ3Expr(ctx)) as BoolExpr)
            }
            solver.check() == Status.SATISFIABLE
        }
    }
}
