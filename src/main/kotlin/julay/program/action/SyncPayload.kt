package julay.program.action

import julay.concurrency.SyncChannel
import julay.program.Constraint
import julay.program.Proc
import java.util.Optional

/**
 * Value delivered to every participant of a successful SyncChannel rendezvous.
 * [syncPeers] and [sessionToInstall] carry session affinity metadata established only through sync.
 *
 * [sessionToInstall] is empty or a singleton:
 * - empty for non-session actions, dedicated session SyncChannels (channel already installed),
 *   and constructor self-syncs (follow-on channels are installed by the spawning parent);
 * - present for a global first-contact handshake on a session action, carrying one freshly
 *   created dedicated SyncChannel for the action currently synchronizing.
 */
data class SyncPayload(
    val action: ConcreteAction,
    val syncPeers: List<SessionPeerMeta> = emptyList(),
    val sessionToInstall: Optional<java.util.Map.Entry<String, SyncChannel<Constraint, SyncPayload>>> =
        Optional.empty(),
)

data class SessionPeerMeta(
    val procId: Long,
    val classId: Int,
    val proc: Proc,
)
