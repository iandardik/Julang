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
 */
class Proc(
    private val transitionSystem: TransitionSystem,
    private val tsInfo: TransitionSystemStaticInfo,
    private val staticChannelTable: Map<SymbolicAction, ProgramAction>,
    private val program: Program,
    /** When non-null, [TransitionSystem.finishConstruction] runs once before the select loop. */
    private val constructorAct: ConcreteAction? = null,
) {
    val procId: Long = program.allocateProcId()
    val classId: Int = tsInfo.classID()

    /** peer classID → locked peer procId (session affinity). */
    private val affinity = mutableMapOf<Int, Long>()

    /** (peerProcId, actionName) → dedicated session SyncChannel. */
    private val sessionChannelTable = mutableMapOf<Pair<Long, String>, SyncChannel<SyncPayload, Constraint>>()

    suspend fun run() {
        try {
            constructorAct?.let { transitionSystem.finishConstruction(it) }
            while (true) {
                val cont = withEphemeralContextSuspend { ctx ->
                    runOneStep(ctx)
                }
                // The Proc exits when no actions are enabled.
                if (!cont) return
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
        val existingPeerId = affinity[child.classId]
        if (existingPeerId != null && existingPeerId != child.procId) {
            throw JulayException(
                "Session constructor \"${constructorAct.name}\" cannot rebind: " +
                    "proc $procId already has affinity to peer $existingPeerId " +
                    "of class ${child.classId} (attempted peer ${child.procId})",
            )
        }
        affinity[child.classId] = child.procId
        child.affinity[classId] = procId
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
                val anticonstraint = Constraint(anticonstraintExpr, procId, classId).cloneInto(caseCtx)
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
        transitionSystem.transit(act)
        if (program.isConstructorAction(act.symAction)) {
            program.spawn(act, parent = this)
        }
        return true
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
        affinity.entries.removeAll { (_, peerId) -> peerId in peersWithClosedSessions }
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
            for ((_, peerId) in affinity) {
                val session = sessionChannelTable[peerId to act.symAction.name]
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
        for (peer in peers) {
            val locked = affinity[peer.classId]
            if (locked == null) {
                affinity[peer.classId] = peer.procId
            } else if (locked != peer.procId) {
                // Should not match a different peer when session routing works; ignore/stale.
                continue
            }
            if (sessionEntry != null) {
                installSession(peer.procId, sessionEntry.key, sessionEntry.value)
            }
        }
    }
}
