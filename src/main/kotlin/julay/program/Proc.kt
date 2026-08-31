package julay.program

import com.microsoft.z3.Status
import com.microsoft.z3.Context
import julay.concurrency.Select
import julay.concurrency.SyncChannel
import julay.program.action.ConcreteAction
import julay.program.action.ProgramAction
import julay.program.action.SymbolicAction
import julay.program.action.SyncPayload
import julay.program.action.TSAction
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.ReceiveChannel

/**
 * Process wrapper around a [TransitionSystem]. Owns process-local session affinity and dedicated
 * session SyncChannels. Affinity/sessions are never looked up on [Program] — only updated from
 * successful session sync / spawn.
 *
 * Peer-exit / session lifetime:
 * - An exiting proc closes every dedicated session SyncChannel in [clearAffinityAndCloseSessions].
 * - Both peers hold the same SyncChannel reference, so close immediately wakes any waiter and
 *   causes that Select case to abort; Select then exits (see [Select]) so other cases do not hang.
 * - The surviving peer lazily removes closed session entries and the corresponding affinity in
 *   [scrubClosedSessionsAndAffinity] at the start of its next transition step, then rebuilds
 *   Select so session actions fall back to the global first-contact channel.
 * - [exitSession] / [killSessionPeer] end a session mid-life without waiting for natural death;
 *   both take a peer proc-class name and are no-ops when that affinity is absent.
 */
class Proc(
    transitionSystem: TransitionSystem,
    private val tsInfo: TransitionSystemStaticInfo,
    private val staticChannelTable: Map<SymbolicAction, ProgramAction>,
    val program: Program,
    /** When non-null, [TransitionSystem.finishConstruction] runs once before the select loop. */
    private val constructorAct: ConcreteAction? = null,
    /** When non-null, completed on `return:` for spawn-and-await procfun callers. */
    private val procFunReturnDeferred: CompletableDeferred<Value>? = null,
) {
    val procId: Long = program.allocateProcId()
    val classId: Int = tsInfo.classID()

    /** Julay leaf proc-class name (matches [TransitionSystemStaticInfo.name]). */
    val className: String = tsInfo.name

    /** peer classID → locked peer Proc (session affinity + kill handle). */
    private val affinity = mutableMapOf<Int, Proc>()

    /** (peerProcId, actionName) → dedicated session SyncChannel. */
    private val sessionChannelTable = mutableMapOf<Pair<Long, String>, SyncChannel<Constraint, SyncPayload>>()

    /** Reused across select steps to avoid per-step [ArrayList] / [Select.SyncCase] churn. */
    private val stepSyncCases = ArrayList<Select.SyncCase<SyncPayload, Constraint>>(8)

    private val silentlyKilled = AtomicBoolean(false)
    @Volatile
    private var runJob: Job? = null

    private var transitionSystem: TransitionSystem = transitionSystem

    /** Set while a pooled HTTP worker serves one request; completes instead of [procFunReturnDeferred]. */
    @Volatile
    private var activeWorkReturn: CompletableDeferred<Value>? = null

    init {
        this.transitionSystem.bindHostProc(this)
    }

    fun replaceTransitionSystem(ts: TransitionSystem) {
        transitionSystem = ts
        ts.bindHostProc(this)
    }

    fun bindRunJob(job: Job) {
        runJob = job
    }

    fun affinityPeerIds(): List<Long> = affinity.values.map { it.procId }

    fun affinityPeers(): List<Proc> = affinity.values.toList()

    /** This occurrence's StaticInfo (shared with nested library resources when needed). */
    fun occurrenceStaticInfo(): TransitionSystemStaticInfo = tsInfo

    /**
     * Map a TS-offered [SymbolicAction] onto this occurrence's StaticInfo alphabet / constructors.
     */
    fun resolveSymbolicAction(act: SymbolicAction): SymbolicAction = tsInfo.resolveAction(act)

    /** Blocking call into a registered procfun (spawn-and-await until `return:`). */
    suspend fun invokeProcFun(name: String, args: List<Any>): Any =
        program.invokeProcFun(name, args)

    /**
     * Close dedicated sessions with [peerProcId] and clear affinity to that peer.
     * Does not kill either proc.
     */
    suspend fun exitSessionWith(peerProcId: Long) {
        val toClose = sessionChannelTable.entries
            .filter { it.key.first == peerProcId }
            .map { it.key to it.value }
        toClose.forEach { (key, _) -> sessionChannelTable.remove(key) }
        affinity.entries.removeAll { (_, peer) -> peer.procId == peerProcId }
        toClose.forEach { (_, channel) -> channel.close() }
    }

    /**
     * End session affinity with the peer whose proc-class name is [peerClassName].
     * No-op if there is no live affinity to that class. Both procs keep running.
     */
    suspend fun exitSession(peerClassName: String) {
        val peer = affinityPeerByClassName(peerClassName) ?: return
        exitSessionWith(peer.procId)
    }

    /**
     * End session affinity with the named peer proc class and silently cancel that peer.
     * No-op if there is no live affinity to that class.
     */
    suspend fun killSessionPeer(peerClassName: String) {
        val peer = affinityPeerByClassName(peerClassName) ?: return
        exitSessionWith(peer.procId)
        peer.requestSilentKill()
    }

    /** Mark this proc for silent death and cancel its run [Job] if bound. */
    fun requestSilentKill() {
        silentlyKilled.set(true)
        runJob?.cancel()
    }

    suspend fun run() {
        try {
            try {
                constructorAct?.let { act ->
                    withSessionPeer(null) {
                        transitionSystem.finishConstruction(act)
                    }
                }
                while (true) {
                    if (silentlyKilled.get()) return
                    scrubClosedSessionsAndAffinity()
                    val cont = when {
                        program.syncResolveConfig.anyEnabled() -> {
                            when (val plan = transitionSystem.syncStepPlan()) {
                                is julay.program.sync.SyncStepPlan.FastOnly ->
                                    runOneStepFast(plan.offers)
                                is julay.program.sync.SyncStepPlan.NeedsZ3 ->
                                    withEphemeralContextSuspend { ctx -> runOneStep(ctx) }
                            }
                        }
                        else -> withEphemeralContextSuspend { ctx -> runOneStep(ctx) }
                    }
                    // The Proc exits when no actions are enabled.
                    if (!cont) return
                }
            } catch (e: CancellationException) {
                if (silentlyKilled.get()) {
                    return
                }
                throw e
            }
        } finally {
            clearAffinityAndCloseSessions()
        }
    }

    /**
     * Long-lived pooled HTTP handler loop: receive work, mint a fresh TS per request, run until
     * `return:`, complete [HandlerWork.returnDeferred], repeat.
     */
    suspend fun runHttpHandlerLoop(
        work: ReceiveChannel<HandlerWork>,
        factory: suspend (Program, ConcreteAction) -> TransitionSystem,
        ctorSym: SymbolicAction,
    ) {
        try {
            for (item in work) {
                serveOneHandlerRequest(item, factory, ctorSym)
            }
        } catch (e: CancellationException) {
            if (!silentlyKilled.get()) {
                throw e
            }
        } finally {
            clearAffinityAndCloseSessions()
        }
    }

    private suspend fun serveOneHandlerRequest(
        item: HandlerWork,
        factory: suspend (Program, ConcreteAction) -> TransitionSystem,
        ctorSym: SymbolicAction,
    ) {
        if (ctorSym.args.size != item.argValues.size) {
            item.returnDeferred.completeExceptionally(
                JulayException(
                    "Procfun \"${tsInfo.name}\" expects ${ctorSym.args.size} argument(s) " +
                        "but got ${item.argValues.size}",
                ),
            )
            return
        }
        try {
            val concreteArgs = ctorSym.args.zip(item.argValues).associate { (v, arg) ->
                v to Value(arg, v.type)
            }
            val ctorAct = ConcreteAction(ctorSym, concreteArgs)
            val ts = factory(program, ctorAct)
            replaceTransitionSystem(ts)
            activeWorkReturn = item.returnDeferred
            withSessionPeer(null) {
                transitionSystem.finishConstruction(ctorAct)
            }
            while (true) {
                if (silentlyKilled.get()) {
                    if (!item.returnDeferred.isCompleted) {
                        item.returnDeferred.completeExceptionally(
                            JulayException("Pooled handler proc $procId silently killed"),
                        )
                    }
                    return
                }
                scrubClosedSessionsAndAffinity()
                val cont = when {
                    program.syncResolveConfig.anyEnabled() -> {
                        when (val plan = transitionSystem.syncStepPlan()) {
                            is julay.program.sync.SyncStepPlan.FastOnly ->
                                runOneStepFast(plan.offers)
                            is julay.program.sync.SyncStepPlan.NeedsZ3 ->
                                withEphemeralContextSuspend { ctx -> runOneStep(ctx) }
                        }
                    }
                    else -> withEphemeralContextSuspend { ctx -> runOneStep(ctx) }
                }
                if (!cont) {
                    if (!item.returnDeferred.isCompleted) {
                        item.returnDeferred.completeExceptionally(
                            JulayException("Procfun \"${tsInfo.name}\" exited without return"),
                        )
                    }
                    return
                }
            }
        } catch (e: Throwable) {
            if (!item.returnDeferred.isCompleted) {
                item.returnDeferred.completeExceptionally(e)
            }
            throw e
        } finally {
            activeWorkReturn = null
        }
    }

    /**
     * Called by [Program.spawn] when this proc's session constructor action spawned [child].
     * Establishes mutual affinity and installs sessions for other shared session actions.
     *
     * Throws [JulayException] if this proc already has live affinity to another peer of
     * [child]'s class (session-ctor rebind). After that peer exits and sessions are scrubbed,
     * a later spawn may establish again.
     */
    suspend fun establishSessionWithSpawnedChild(child: Proc, constructorAct: SymbolicAction) {
        scrubClosedSessionsAndAffinity()
        val existingPeer = affinity[child.classId]
        if (existingPeer != null && existingPeer.procId != child.procId) {
            throw JulayException(
                "Session constructor \"${constructorAct.name}\" cannot rebind: " +
                    "proc $procId already has affinity to peer ${existingPeer.procId} " +
                    "of class ${child.classId} (attempted peer ${child.procId})",
            )
        }
        affinity[child.classId] = child
        child.affinity[classId] = this
        val parentSession = tsInfo.alphabet.filter { it.isSession && it.name != constructorAct.name }
        val childSession = child.tsInfo.alphabet.filter { it.isSession && it.name != constructorAct.name }
        val sharedNames = parentSession.map { it.name }.toSet().intersect(childSession.map { it.name }.toSet())
        for (name in sharedNames) {
            val act = parentSession.firstOrNull { it.name == name } ?: continue
            val session = program.makeSessionChannel(act)
            installSession(child.procId, name, session)
            child.installSession(procId, name, session)
        }
    }

    private fun affinityPeerByClassName(peerClassName: String): Proc? =
        affinity.values.firstOrNull { it.className == peerClassName }

    private fun installSession(
        peerProcId: Long,
        actionName: String,
        session: SyncChannel<Constraint, SyncPayload>,
    ) {
        sessionChannelTable[peerProcId to actionName] = session
    }

    /**
     * Close all dedicated sessions this proc holds. Shared SyncChannels wake peer waiters;
     * the peer scrubs closed entries and affinity on its next step.
     */
    private suspend fun clearAffinityAndCloseSessions() {
        val toClose = sessionChannelTable.values.toList()
        sessionChannelTable.clear()
        affinity.clear()
        toClose.forEach { it.close() }
    }

    /**
     * Select/transit for [julay.program.sync.SyncStepPlan.FastOnly]: offers already use
     * grounded [BoolExprFast] on [Constraint.fast], so this step allocates no Z3 Context.
     */
    private suspend fun runOneStepFast(offers: List<julay.program.sync.FastOffer>): Boolean {
        if (offers.isEmpty()) {
            return false
        }
        // Single offer: syncFast directly — no Select.SyncCase list/map/callback churn.
        if (offers.size == 1) {
            val offer = offers[0]
            val syncChannel = resolveSyncChannel(offer)
            val constraint = Constraint(
                fast = offer.guard,
                procId = procId,
                classId = classId,
                proc = this,
            )
            val anticonstraint = Constraint(
                anti = julay.program.sync.SyncAnti.fromRole(offer.syncRole, tsInfo.classID()),
                procId = procId,
                classId = classId,
                proc = this,
            )
            val ret = syncChannel.syncFast(constraint, anticonstraint)
            if (ret.isEmpty) {
                scrubClosedSessionsAndAffinity()
                return true
            }
            return applySyncPayload(ret.result.get())
        }
        var nextPayload = Optional.empty<SyncPayload>()
        stepSyncCases.clear()
        for (offer in offers) {
            val syncChannel = resolveSyncChannel(offer)
            val constraint = Constraint(
                fast = offer.guard,
                procId = procId,
                classId = classId,
                proc = this,
            )
            val anticonstraint = Constraint(
                anti = julay.program.sync.SyncAnti.fromRole(offer.syncRole, tsInfo.classID()),
                procId = procId,
                classId = classId,
                proc = this,
            )
            stepSyncCases.add(
                Select.SyncCase(syncChannel, constraint, anticonstraint) { payload: SyncPayload ->
                    nextPayload = Optional.of(payload)
                },
            )
        }
        runSyncCases(stepSyncCases) { payload -> nextPayload = Optional.of(payload) }

        if (nextPayload.isEmpty) {
            scrubClosedSessionsAndAffinity()
            return true
        }

        return applySyncPayload(nextPayload.get())
    }

    /**
     * Runs one select/transit step using [ctx].
     * @return false only when no actions are enabled (Proc exits); true to continue the loop,
     * including after Select finishes without a winner (e.g. dedicated session channel closed).
     */
    private suspend fun runOneStep(ctx: Context): Boolean {
        scrubClosedSessionsAndAffinity()
        val solver = ctx.mkSolver()
        var nextPayload = Optional.empty<SyncPayload>()
        // Enablement stays on Z3: local guards often mix equalities with embeddings
        // (string concat, collection ops) where a wrong fast-path unsat skips needed steps.
        val enabledActions = transitionSystem.actions(ctx).filter { act ->
            solver.reset()
            solver.add(act.guard)
            solver.check() == Status.SATISFIABLE
        }
        // The Proc exits when no actions are enabled.
        if (enabledActions.isEmpty()) {
            return false
        }
        val caseCtxs = mutableListOf<Context>()
        try {
            stepSyncCases.clear()
            for (act in enabledActions) {
                val syncChannel = resolveSyncChannel(act)
                val caseCtx = Context().also { caseCtxs.add(it) }
                val constraint = Constraint(
                    expr = act.guard,
                    fast = act.fastGuard,
                    procId = procId,
                    classId = classId,
                    proc = this,
                ).cloneInto(caseCtx)
                val anticonstraint = Constraint(
                    anti = julay.program.sync.SyncAnti.fromRole(act.syncRole, tsInfo.classID()),
                    procId = procId,
                    classId = classId,
                    proc = this,
                )
                stepSyncCases.add(
                    Select.SyncCase(syncChannel, constraint, anticonstraint) { payload: SyncPayload ->
                        nextPayload = Optional.of(payload)
                    },
                )
            }
            runSyncCases(stepSyncCases) { payload -> nextPayload = Optional.of(payload) }
        } finally {
            caseCtxs.forEach { caseCtx ->
                ContextLocalCache.dropContext(caseCtx)
                caseCtx.close()
            }
        }

        if (nextPayload.isEmpty) {
            // Select finished without a winner (e.g. dedicated session closed by a dying peer).
            // Scrub and continue so the next step can offer session actions on the global channel.
            scrubClosedSessionsAndAffinity()
            return true
        }

        return applySyncPayload(nextPayload.get())
    }

    /**
     * One enabled offer: [Select.SyncCase.syncDirect] (no Select). Multiple offers: [Select.run].
     */
    private suspend fun runSyncCases(
        cases: List<Select.SyncCase<SyncPayload, Constraint>>,
        onSat: (SyncPayload) -> Unit,
    ) {
        if (cases.isEmpty()) return
        if (cases.size == 1) {
            cases[0].syncDirect(onSat)
            return
        }
        Select(*cases.toTypedArray()).run()
    }

    private suspend fun applySyncPayload(payload: SyncPayload): Boolean {
        applySessionPayload(payload)
        val act = payload.action
        val syncPeer = payload.syncPeers.firstOrNull { it.procId != procId }?.proc
        withSessionPeer(syncPeer) {
            transitionSystem.transit(act)
        }
        if (program.isConstructorAction(act.symAction)) {
            program.spawn(act, parent = this)
        }
        transitionSystem.consumeProcFunReturn()?.let { value ->
            val workDeferred = activeWorkReturn
            if (workDeferred != null) {
                workDeferred.complete(value)
            } else {
                procFunReturnDeferred?.complete(value)
            }
            return false
        }
        return !silentlyKilled.get()
    }

    private suspend fun withSessionPeer(syncPeer: Proc?, block: suspend () -> Unit) {
        transitionSystem.setSessionPeer(syncPeer)
        try {
            block()
        } finally {
            transitionSystem.setSessionPeer(null)
        }
    }

    /**
     * Remove closed dedicated sessions and clear affinity to peers that owned them.
     * Called at step start and after a Select that finished without a payload.
     */
    private suspend fun scrubClosedSessionsAndAffinity() {
        val closedKeys = sessionChannelTable.entries.mapNotNull { (key, session) ->
            if (session.isClosed()) key else null
        }
        closedKeys.forEach { sessionChannelTable.remove(it) }
        // Clear affinity for peers whose sessions were closed (peer exited).
        val peersWithClosedSessions = closedKeys.map { it.first }.toSet()
        affinity.entries.removeAll { (_, peer) -> peer.procId in peersWithClosedSessions }
    }

    /**
     * A session action uses the global action channel for first contact; [Program] creates one
     * dedicated SyncChannel delivered via [SyncPayload.sessionToInstall]. Subsequent occurrences
     * use that dedicated channel.
     *
     * A proc created by a session constructor is a special case: the constructor action itself
     * uses the global channel because the child does not exist yet. Constructor channels have
     * sync size 1, so construction requires no peer rendezvous and remains inexpensive.
     * [Program.spawn] then installs dedicated channels for all shared follow-on session actions
     * before the child starts.
     */
    private suspend fun resolveSyncChannel(offer: julay.program.sync.FastOffer): SyncChannel<Constraint, SyncPayload> =
        resolveSyncChannelForSym(offer.symAction, offer.syncChannel)

    private suspend fun resolveSyncChannelForSym(
        symAction: SymbolicAction,
        syncChannel: SyncChannel<Constraint, SyncPayload>?,
    ): SyncChannel<Constraint, SyncPayload> {
        syncChannel?.let { return it }
        val resolvedSym = resolveSymbolicAction(symAction)
        if (resolvedSym.isSession) {
            for ((_, peer) in affinity) {
                val session = sessionChannelTable[peer.procId to resolvedSym.name]
                if (session != null && !session.isClosed()) {
                    return session
                }
            }
        }
        val entry = staticChannelTable[resolvedSym]
            ?: staticChannelTable.entries.firstOrNull { (k, _) ->
                k.name == resolvedSym.name &&
                    k.isSession == resolvedSym.isSession &&
                    k.isInternal == resolvedSym.isInternal &&
                    k.args == resolvedSym.args &&
                    k.channelKey == resolvedSym.channelKey
            }?.value
            ?: staticChannelTable.entries.firstOrNull { (k, _) ->
                k.name == resolvedSym.name &&
                    k.isSession == resolvedSym.isSession &&
                    k.isInternal == resolvedSym.isInternal &&
                    k.args == resolvedSym.args
            }?.value
        return entry?.channel
            ?: error(
                "No SyncChannel for ${resolvedSym.name} (channelKey=${resolvedSym.channelKey}) " +
                    "on $className/$procId",
            )
    }

    private suspend fun resolveSyncChannel(act: TSAction): SyncChannel<Constraint, SyncPayload> {
        return resolveSyncChannelForSym(act.symAction, act.syncChannel)
    }

    private fun applySessionPayload(payload: SyncPayload) {
        if (!payload.action.symAction.isSession) {
            return
        }
        val peers = payload.syncPeers.filter { it.procId != procId }
        val sessionEntry = payload.sessionToInstall.orElse(null)
        for (peerMeta in peers) {
            val locked = affinity[peerMeta.classId]
            if (locked == null) {
                affinity[peerMeta.classId] = peerMeta.proc
            } else if (locked.procId != peerMeta.procId) {
                // Should not match a different peer when session routing works; ignore/stale.
                continue
            }
            if (sessionEntry != null) {
                installSession(peerMeta.procId, sessionEntry.key, sessionEntry.value)
            }
        }
    }
}
