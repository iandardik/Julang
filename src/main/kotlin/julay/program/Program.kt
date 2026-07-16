package julay.program

import com.microsoft.z3.BoolExpr
import com.microsoft.z3.Context
import com.microsoft.z3.Status
import julay.concurrency.SyncChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import java.util.*

/**
 * A program represents one or more processes that interact together on a single computer.
 *
 * Process construction: constructors are not SyncChannel peers at runtime. A transition that
 * "peers" with a constructor (compiler abstraction) self-syncs (size 1) and then [spawn]s the
 * constructed process on [godScope] so children can outlive their parent. [run] boots every
 * `initially` constructor the same way, then parks.
 */
class Program {
    val actionTable: Map<SymbolicAction, ProgramAction>
    val godScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val componentInfo: Set<TransitionSystemStaticInfo>
    private val constructorsByAction:
        Map<SymbolicAction, List<Pair<TransitionSystemStaticInfo, suspend (Program, ConcreteAction) -> TransitionSystem>>>
    private val constructorActions: Set<SymbolicAction>
    private val initiallyAction: SymbolicAction
    private val initiallyConcrete: ConcreteAction

    /**
     * The constructor sets up a channel for each alphabet action so that processes that engage
     * in the action can communicate (synchronize on args) over the channel.
     */
    constructor(componentInfo: Set<TransitionSystemStaticInfo>, cliArgs: List<String> = emptyList()) {
        // assumptions/requirements (enforced by the compiler):
        // - all action signatures that have the same name should have the same params
        // - no transition should be for initially (only constructors)
        // - internal actions and constructor-paired actions: sync size 1; other defaults: sync size 2
        // - service actions: at most one servicer; consumers are untagged defaults
        // - a service with no consumers is a legal intentional deadlock (warned at compile time)
        // - constructor peers are a compile-time sync abstraction; runtime spawns locally via spawn()

        this.componentInfo = componentInfo

        val argsVar = Variable("args", listType(stringType))
        initiallyAction = SymbolicAction("initially", listOf(argsVar))
        initiallyConcrete = ConcreteAction(
            initiallyAction,
            mapOf(argsVar to Value(cliArgs, listType(stringType))),
        )

        constructorsByAction = componentInfo
            .flatMap { info ->
                info.constructors.map { (act, ctor) -> act to (info to ctor) }
            }
            .groupBy({ it.first }, { it.second })
        constructorActions = constructorsByAction.keys

        // Channels for live alphabets only (constructors are not offered on a channel).
        val allActions = componentInfo.flatMap { it.alphabet }.toSet()
        val actionCounts = allActions.associateWith { setAct ->
            when {
                setAct.isInternal -> 1
                setAct in constructorActions -> 1
                else -> 2
            }
        }

        val channelTable = actionCounts.keys.associateWith { act ->
            val syncSize = actionCounts[act]!!
            // Ephemeral Z3 Contexts: channels live for the program lifetime, but each SAT /
            // model extraction uses a scratch Context that is closed immediately after.
            // (ConcreteAction copies assignments into plain Java Values.)
            // c.translate(ctx) is required because each constraint comes from a different
            // proc thread / Context.
            fun constraintsSatisfiable(constraints: Set<BoolExpr>): Boolean =
                Context().use { ctx ->
                    val solver = ctx.mkSolver()
                    constraints.forEach { c -> solver.add(c.translate(ctx) as BoolExpr) }
                    solver.check() == Status.SATISFIABLE
                }
            SyncChannel(
                syncSize,
                satisfiable = ::constraintsSatisfiable,
                compute = { constraints ->
                    Context().use { ctx ->
                        val solver = ctx.mkSolver()
                        constraints.forEach { c -> solver.add(c.translate(ctx) as BoolExpr) }
                        if (solver.check() != Status.SATISFIABLE) {
                            Optional.empty()
                        } else if (act.args.isEmpty()) {
                            // Avoid allocating a Model when no args need extraction.
                            Optional.of(ConcreteAction(act, emptyMap()))
                        } else {
                            // Extract ConcreteAction (Kotlin Values only) before Context closes.
                            Optional.of(ConcreteAction(act, ctx, solver.model))
                        }
                    }
                },
            )
        }

        actionTable = channelTable.keys.associateWith { ProgramAction(it, channelTable[it]!!) }
    }

    fun isConstructorAction(act: SymbolicAction): Boolean = act in constructorActions

    fun spawnProc(ts: TransitionSystem, tsInfo: TransitionSystemStaticInfo) {
        godScope.launch {
            Proc(ts, tsInfo, actionTable, this@Program).run()
        }
    }

    fun spawn(act: ConcreteAction) {
        val entries = constructorsByAction[act.symAction] ?: return
        entries.forEach { (tsInfo, constructor) ->
            godScope.launch {
                val ts = constructor(this@Program, act)
                Proc(ts, tsInfo, actionTable, this@Program).run()
            }
        }
    }

    suspend fun run() {
        // Spawn every p-class / library with an initially constructor (once per registry entry).
        spawn(initiallyConcrete)
        // Park so main does not return while godScope children run on Dispatchers.IO.
        // Intentional process end uses exitProcess from a child (e.g. ExitSystem).
        awaitCancellation()
    }
}
