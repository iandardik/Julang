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
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException

/**
 * Process wrapper around a [TransitionSystem]. Owns process-local session affinity and dedicated
 * session SyncChannels. Affinity/sessions are never looked up on [Program] — only updated from
 * successful session sync / spawn.
 *
 * Peer-exit / session lifetime:
 * - An exiting proc closes every dedicated session SyncChannel in [clearAffinityAndCloseSessions].
 * - Both peers hold the same SyncChannel reference, so close immediately wakes any waiter and
 *   causes that Select arm to abort; Select then exits (see [Select]) so other arms do not hang.
 * - The surviving peer lazily removes closed session entries and the corresponding affinity in
 *   [scrubClosedSessionsAndAffinity] at the start of its next transition step, then rebuilds
 *   Select so session actions fall back to the global first-contact channel.
 * - [exitSessionWith] / [requestSilentKill] end a session mid-life without waiting for natural death.
 */
class Proc(
    private val transitionSystem: TransitionSystem,
    private val tsInfo: TransitionSystemStaticInfo,
    private val staticChannelTable: Map<SymbolicAction, ProgramAction>,
    val program: Program,
    /** When non-null, [TransitionSystem.finishConstruction] runs once before the select loop. */
    private val constructorAct: ConcreteAction? = null,
) {
    val procId: Long = program.allocateProcId()
    val classId: Int = tsInfo.classID()

    /** peer classID → locked peer Proc (session affinity + kill handle). */
    private val affinity = mutableMapOf<Int, Proc>()

    /** (peerProcId, actionName) → dedicated session SyncChannel. */
    private val sessionChannelTable = mutableMapOf<Pair<Long, String>, SyncChannel<SyncPayload, Constraint>>()

    private val silentlyKilled = AtomicBoolean(false)
    @Volatile
    private var runJob: Job? = null

    init {
        transitionSystem.bindHostProc(this)
    }

    fun bindRunJob(job: Job) {
        runJob = job
    }

    fun affinityPeerIds(): List<Long> = affinity.values.map { it.procId }

    fun affinityPeers(): List<Proc> = affinity.values.toList()

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
     * Prefer the sync peer of the current session action; else the unique affinity peer.
     */
    suspend fun exitSession(syncPeer: Proc?) {
        val peer = resolveExitPeer(syncPeer)
        exitSessionWith(peer.procId)
    }

    /**
     * Prefer an affinity peer that is not the current sync peer (e.g. Timer killing TimerHelper
     * while cancelTimer syncs with the client). Else sync peer, else unique affinity peer.
     * Clears the session then silently cancels the peer's run job.
     */
    suspend fun killSessionPeer(syncPeer: Proc?) {
        val peer = resolveKillPeer(syncPeer)
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
                    val cont = withEphemeralContextSuspend { ctx ->
                        runOneStep(ctx)
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
            val act = program.sessionAction(name) ?: continue
            val session = program.makeSessionChannel(act)
            installSession(child.procId, name, session)
            child.installSession(procId, name, session)
        }
    }

    private fun installSession(
        peerProcId: Long,
        actionName: String,
        session: SyncChannel<SyncPayload, Constraint>,
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
     * Runs one select/transit step using [ctx].
     * @return false only when no actions are enabled (Proc exits); true to continue the loop,
     * including after Select finishes without a winner (e.g. dedicated session channel closed).
     */
    private suspend fun runOneStep(ctx: Context): Boolean {
        scrubClosedSessionsAndAffinity()
        val solver = ctx.mkSolver()
        var nextPayload = Optional.empty<SyncPayload>()
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
            val cases = enabledActions.map { act ->
                val syncChannel = resolveSyncChannel(act)
                val caseCtx = Context().also { caseCtxs.add(it) }
                val constraint = Constraint(
                    act.guard,
                    procId = procId,
                    classId = classId,
                    proc = this,
                ).cloneInto(caseCtx)
                val anticonstraintExpr = when (act.syncRole) {
                    TSAction.SyncRole.Default, TSAction.SyncRole.Internal ->
                        ctx.mkEq(ctx.mkIntConst("classID"), ctx.mkInt(tsInfo.classID()))
                    TSAction.SyncRole.Service ->
                        ctx.mkEq(ctx.mkBoolConst("serviceTransition"), ctx.mkTrue())
                    TSAction.SyncRole.Consumer ->
                        ctx.mkEq(ctx.mkBoolConst("serviceTransition"), ctx.mkFalse())
                }
                // Affinity exclusivity is enforced by routing onto the dedicated session SyncChannel
                // once affinity exists (see resolveSyncChannel). First contact may use the static channel.
                val anticonstraint = Constraint(
                    anticonstraintExpr,
                    procId = procId,
                    classId = classId,
                    proc = this,
                ).cloneInto(caseCtx)
                Select.SyncCase(syncChannel, constraint, anticonstraint) { payload: SyncPayload ->
                    nextPayload = Optional.of(payload)
                }
            }
            Select(*cases.toTypedArray()).run()
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

        val payload = nextPayload.get()
        applySessionPayload(payload)
        val act = payload.action
        val syncPeer = payload.syncPeers.firstOrNull { it.procId != procId }?.proc
        withSessionPeer(syncPeer) {
            transitionSystem.transit(act)
        }
        if (program.isConstructorAction(act.symAction)) {
            program.spawn(act, parent = this)
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
     * Called at step start and after a Select that finished without installing a payload.
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
    private suspend fun resolveSyncChannel(act: TSAction): SyncChannel<SyncPayload, Constraint> {
        act.syncChannel?.let { return it }
        if (act.symAction.isSession) {
            for ((_, peer) in affinity) {
                val session = sessionChannelTable[peer.procId to act.symAction.name]
                if (session != null && !session.isClosed()) {
                    return session
                }
            }
        }
        return staticChannelTable[act.symAction]!!.channel
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

    private fun resolveExitPeer(syncPeer: Proc?): Proc {
        syncPeer?.let { return it }
        return uniqueAffinityPeer("exitSession")
    }

    private fun resolveKillPeer(syncPeer: Proc?): Proc {
        val affinityPeers = affinityPeers()
        val nonSync = affinityPeers.filter { it !== syncPeer && it.procId != syncPeer?.procId }
        when {
            nonSync.size == 1 -> return nonSync.single()
            syncPeer != null && affinityPeers.any { it.procId == syncPeer.procId } -> return syncPeer
            syncPeer != null && affinityPeers.isEmpty() -> return syncPeer
            affinityPeers.size == 1 -> return affinityPeers.single()
            affinityPeers.isEmpty() && syncPeer != null -> return syncPeer
            affinityPeers.isEmpty() -> throw JulayException("killSessionPeer: no session peer to kill")
            else -> throw JulayException(
                "killSessionPeer: ambiguous peers affinity=${affinityPeers.map { it.procId }} " +
                    "syncPeer=${syncPeer?.procId}",
            )
        }
    }

    private fun uniqueAffinityPeer(effectName: String): Proc {
        val peers = affinityPeers()
        return when (peers.size) {
            1 -> peers.single()
            0 -> throw JulayException("$effectName: no session affinity peer to target")
            else -> throw JulayException(
                "$effectName: ambiguous session affinity peers ${peers.map { it.procId }}",
            )
        }
    }
}
