package julay.program

/**
 * Thread-local host for Julay effect builtins that need the current [Proc]
 * (session exit / peer kill). [Proc] installs this around transit / finishConstruction.
 */
object ProcEffectContext {
    private val currentProc = ThreadLocal<Proc?>()
    private val syncPeerProcId = ThreadLocal<Long?>()

    suspend fun <T> withProc(
        proc: Proc,
        syncPeerId: Long? = null,
        block: suspend () -> T,
    ): T {
        val prevProc = currentProc.get()
        val prevPeer = syncPeerProcId.get()
        currentProc.set(proc)
        syncPeerProcId.set(syncPeerId)
        try {
            return block()
        } finally {
            currentProc.set(prevProc)
            syncPeerProcId.set(prevPeer)
        }
    }

    suspend fun exitSession() {
        val proc = requireProc("exitSession")
        val peerId = resolveExitPeer(proc)
        proc.exitSessionWith(peerId)
    }

    suspend fun killSessionPeer() {
        val proc = requireProc("killSessionPeer")
        val peerId = resolveKillPeer(proc)
        proc.exitSessionWith(peerId)
        val peer = proc.program.lookupProc(peerId)
            ?: throw JulayException(
                "killSessionPeer: peer proc $peerId is not registered (already exited?)",
            )
        peer.requestSilentKill()
    }

    private fun requireProc(effectName: String): Proc =
        currentProc.get()
            ?: throw JulayException("$effectName called outside of a proc transit/effect context")

    /**
     * Prefer the sync peer of the current session action; else the unique affinity peer.
     */
    private fun resolveExitPeer(proc: Proc): Long {
        syncPeerProcId.get()?.let { return it }
        return uniqueAffinityPeer(proc, "exitSession")
    }

    /**
     * Prefer an affinity peer that is not the current sync peer (e.g. Timer killing TimerHelper
     * while cancelTimer syncs with the client). Else sync peer, else unique affinity peer.
     */
    private fun resolveKillPeer(proc: Proc): Long {
        val syncPeer = syncPeerProcId.get()
        val affinityPeers = proc.affinityPeerIds()
        val nonSync = affinityPeers.filter { it != syncPeer }
        when {
            nonSync.size == 1 -> return nonSync.single()
            syncPeer != null && syncPeer in affinityPeers -> return syncPeer
            syncPeer != null && affinityPeers.isEmpty() -> return syncPeer
            affinityPeers.size == 1 -> return affinityPeers.single()
            affinityPeers.isEmpty() && syncPeer != null -> return syncPeer
            affinityPeers.isEmpty() -> throw JulayException("killSessionPeer: no session peer to kill")
            else -> throw JulayException(
                "killSessionPeer: ambiguous peers affinity=$affinityPeers syncPeer=$syncPeer",
            )
        }
    }

    private fun uniqueAffinityPeer(proc: Proc, effectName: String): Long {
        val peers = proc.affinityPeerIds()
        return when (peers.size) {
            1 -> peers.single()
            0 -> throw JulayException("$effectName: no session affinity peer to target")
            else -> throw JulayException(
                "$effectName: ambiguous session affinity peers $peers",
            )
        }
    }
}
