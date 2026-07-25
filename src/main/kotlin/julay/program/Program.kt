package julay.program

import com.microsoft.z3.BoolExpr
import com.microsoft.z3.Status
import julay.concurrency.SyncChannel
import julay.program.action.ConcreteAction
import julay.program.action.ProgramAction
import julay.program.action.SessionPeerMeta
import julay.program.action.SymbolicAction
import julay.program.action.SyncPayload
import julay.program.type.listType
import julay.program.type.stringType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.CompletableDeferred
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * A program represents one or more processes that interact together on a single computer.
 *
 * Process construction: constructors are not SyncChannel peers at runtime. A transition that
 * "peers" with a constructor (compiler abstraction) self-syncs (size 1) and then [spawn]s the
 * constructed process on [godScope] so children can outlive their parent. [run] boots every
 * `initially` constructor the same way, then parks.
 *
 * Session affinity and dedicated session SyncChannels are process-local (see [Proc]); this class
 * only allocates unique process ids and hosts the static per-action SyncChannel table.
 *
 * Session first contact: peers rendezvous on the static (global) action channel; that channel's
 * compute creates exactly one dedicated SyncChannel delivered via [SyncPayload.sessionToInstall].
 * Dedicated session channels never create a session to install.
 *
 * Spawn allocates an uninitialized child TS (no constructor transit/effects), installs session
 * affinity, then launches the child which runs [TransitionSystem.finishConstruction] on the child
 * coroutine. The parent does not wait for child initialization or coordinate outside SyncChannel.
 */
class Program {
    val staticChannelTable: Map<SymbolicAction, ProgramAction>
    val godScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val componentInfo: Set<TransitionSystemStaticInfo>
    private val constructorsByAction:
        Map<SymbolicAction, List<Pair<TransitionSystemStaticInfo, suspend (Program, ConcreteAction) -> TransitionSystem>>>
    private val constructorActions: Set<SymbolicAction>
    private val nextProcId = AtomicLong(1)
    private val initiallyAction: SymbolicAction
    private val initiallyConcrete: ConcreteAction

    /**
     * Session actions in the program alphabet, keyed by name (for session install during sync).
     */
    private val sessionActionsByName: Map<String, SymbolicAction>
    private val procFunByName: Map<String, TransitionSystemStaticInfo>
    private val procFunWaiters = ConcurrentHashMap<Long, CompletableDeferred<Value>>()

    constructor(
        componentInfo: Set<TransitionSystemStaticInfo>,
        cliArgs: List<String> = emptyList(),
        procFunInfo: Set<TransitionSystemStaticInfo> = emptySet(),
    ) {
        this.componentInfo = componentInfo
        this.procFunByName = procFunInfo.associateBy { it.name }

        val argsVar = Variable("args", listType(stringType))
        initiallyAction = SymbolicAction("initially", listOf(argsVar))
        initiallyConcrete = ConcreteAction(
            initiallyAction,
            mapOf(argsVar to Value(cliArgs, listType(stringType))),
        )

        val allInfo = componentInfo + procFunInfo
        constructorsByAction = allInfo
            .flatMap { info ->
                info.constructors.map { (act, ctor) -> act to (info to ctor) }
            }
            .groupBy({ it.first }, { it.second })
        constructorActions = constructorsByAction.keys

        val allActions = allInfo.flatMap { it.alphabet }.toSet()
        sessionActionsByName = allActions.filter { it.isSession }.associateBy { it.channelKey }

        val actionCounts = allActions.associateWith { setAct ->
            when {
                setAct.isInternal -> 1
                setAct in constructorActions -> 1
                else -> 2
            }
        }

        val channelTable = actionCounts.keys.associateWith { act ->
            // Global first-contact creates a session to install only for pairwise session actions.
            val installSession = act.isSession && actionCounts[act]!! >= 2
            makeSyncChannel(act, actionCounts[act]!!, installSession)
        }

        staticChannelTable = channelTable.keys.associateWith { ProgramAction(it, channelTable[it]!!) }
    }

    fun isConstructorAction(act: SymbolicAction): Boolean = act in constructorActions

    fun allocateProcId(): Long = nextProcId.getAndIncrement()

    fun sessionActions(): Collection<SymbolicAction> = sessionActionsByName.values

    fun sessionAction(name: String): SymbolicAction? =
        sessionActionsByName.values.firstOrNull { it.name == name }
            ?: sessionActionsByName[name]

    /** Dedicated session SyncChannel: payload never includes a session to install. */
    fun makeSessionChannel(act: SymbolicAction): SyncChannel<SyncPayload, Constraint> =
        makeSyncChannel(act, syncSize = 2, installSession = false)

    fun spawnProc(ts: TransitionSystem, tsInfo: TransitionSystemStaticInfo) {
        val proc = Proc(ts, tsInfo, staticChannelTable, this@Program)
        val job = godScope.launch(start = CoroutineStart.LAZY) {
            proc.run()
        }
        proc.bindRunJob(job)
        job.start()
    }

    /**
     * Spawns constructor peers for [act]. Allocates an uninitialized child TS, and when [parent]
     * is non-null and [act] is a session action, establishes mutual affinity / sessions before the
     * child starts. The child applies constructor transit and effects via
     * [TransitionSystem.finishConstruction] on its own coroutine. The parent returns immediately
     * after launch; startup dependencies must synchronize through Julay actions / SyncChannels.
     */
    suspend fun spawn(act: ConcreteAction, parent: Proc? = null) {
        val entries = constructorsByAction[act.symAction] ?: return
        for ((tsInfo, constructor) in entries) {
            // Factory must not run Julay effects; only allocate uninitialized state.
            val ts = constructor(this@Program, act)
            val child = Proc(
                ts,
                tsInfo,
                staticChannelTable,
                this@Program,
                constructorAct = act,
            )
            if (parent != null && act.symAction.isSession) {
                // Throws JulayException on live rebind before the child is launched.
                parent.establishSessionWithSpawnedChild(child, act.symAction)
            }
            val job = godScope.launch(start = CoroutineStart.LAZY) {
                child.run()
            }
            child.bindRunJob(job)
            job.start()
        }
    }

    suspend fun run() {
        spawn(initiallyConcrete, parent = null)
        awaitCancellation()
    }

    fun completeProcFunReturn(procId: Long, value: Value) {
        procFunWaiters.remove(procId)?.complete(value)
    }

    /**
     * Spawn-and-await a procfun instance: runs until a `return:` transition, then yields the value.
     * The instance participates in SyncChannels so `client` steps can meet providers.
     */
    suspend fun invokeProcFun(name: String, argValues: List<Any>): Any {
        val info = procFunByName[name]
            ?: throw JulayException("Unknown procfun \"$name\"")
        val ctorEntry = info.constructors.entries.singleOrNull()
            ?: throw JulayException("Procfun \"$name\" must have exactly one constructor")
        val ctorSym = ctorEntry.key
        val factory = ctorEntry.value
        if (ctorSym.args.size != argValues.size) {
            throw JulayException(
                "Procfun \"$name\" expects ${ctorSym.args.size} argument(s) but got ${argValues.size}",
            )
        }
        val concreteArgs = ctorSym.args.zip(argValues).associate { (v, arg) ->
            v to Value(arg, v.type)
        }
        val ctorAct = ConcreteAction(ctorSym, concreteArgs)
        val ts = factory(this, ctorAct)
        val deferred = CompletableDeferred<Value>()
        val child = Proc(ts, info, staticChannelTable, this, constructorAct = ctorAct)
        procFunWaiters[child.procId] = deferred
        val job = godScope.launch(start = CoroutineStart.LAZY) {
            child.run()
            if (!deferred.isCompleted) {
                deferred.completeExceptionally(
                    JulayException("Procfun \"$name\" exited without return"),
                )
            }
        }
        child.bindRunJob(job)
        job.start()
        return deferred.await().value
    }

    private fun constraintsSatisfiable(constraints: Set<Constraint>): Boolean =
        withEphemeralContext { ctx ->
            val solver = ctx.mkSolver()
            constraints.forEach { c -> solver.add(c.expr.translate(ctx) as BoolExpr) }
            solver.check() == Status.SATISFIABLE
        }

    private fun extractSyncPayload(
        act: SymbolicAction,
        constraints: Set<Constraint>,
        installSession: Boolean,
    ): Optional<SyncPayload> =
        withEphemeralContext { ctx ->
            val solver = ctx.mkSolver()
            constraints.forEach { c -> solver.add(c.expr.translate(ctx) as BoolExpr) }
            if (solver.check() != Status.SATISFIABLE) {
                Optional.empty()
            } else {
                val concrete = if (act.args.isEmpty()) {
                    ConcreteAction(act, emptyMap())
                } else {
                    ConcreteAction(act, ctx, solver.model)
                }
                val syncPeers = constraints
                    .filter { it.procId >= 0 }
                    .map { c ->
                        val proc = c.proc
                            ?: throw JulayException(
                                "session sync constraint for proc ${c.procId} missing Proc handle",
                            )
                        SessionPeerMeta(c.procId, c.classId, proc)
                    }
                val sessionToInstall =
                    if (installSession) {
                        val actionName = act.name
                        val channel = makeSessionChannel(act)
                        val entry: java.util.Map.Entry<String, SyncChannel<SyncPayload, Constraint>> =
                            object : java.util.Map.Entry<String, SyncChannel<SyncPayload, Constraint>> {
                                override fun getKey(): String = actionName
                                override fun getValue(): SyncChannel<SyncPayload, Constraint> = channel
                                override fun setValue(
                                    newValue: SyncChannel<SyncPayload, Constraint>,
                                ): SyncChannel<SyncPayload, Constraint> {
                                    throw UnsupportedOperationException()
                                }
                            }
                        Optional.of(entry)
                    } else {
                        Optional.empty()
                    }
                Optional.of(SyncPayload(concrete, syncPeers, sessionToInstall))
            }
        }

    private fun makeSyncChannel(
        act: SymbolicAction,
        syncSize: Int,
        installSession: Boolean,
    ): SyncChannel<SyncPayload, Constraint> {
        return SyncChannel(
            syncSize,
            satisfiable = ::constraintsSatisfiable,
            compute = { constraints -> extractSyncPayload(act, constraints, installSession) },
        )
    }
}
