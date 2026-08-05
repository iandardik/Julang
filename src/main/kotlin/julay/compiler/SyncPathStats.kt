package julay.compiler

import julay.program.sync.SyncResolveConfig

/** Per-action fast-path classification from codegen guard lowering. */
data class SyncPathActionStat(
    val name: String,
    val hasFastGuard: Boolean,
    /** Non-null when [hasFastGuard] is false. */
    val opaqueReason: String? = null,
)

/** Per-proc FastOnly vs NeedsZ3 classification. */
data class SyncPathProcStat(
    val name: String,
    val fastOnly: Boolean,
    val actions: List<SyncPathActionStat>,
) {
    val fastGuardCount: Int get() = actions.count { it.hasFastGuard }
    val opaqueCount: Int get() = actions.count { !it.hasFastGuard }
}

/**
 * Compile-time sync-path tallies for generated Julay procs (not Kotlin stdlib libs).
 */
data class SyncPathStats(
    val procs: List<SyncPathProcStat> = emptyList(),
) {
    val fastOnlyProcCount: Int get() = procs.count { it.fastOnly }
    val needsZ3ProcCount: Int get() = procs.count { !it.fastOnly }
    val fastGuardActionCount: Int get() = procs.sumOf { it.fastGuardCount }
    val opaqueActionCount: Int get() = procs.sumOf { it.opaqueCount }

    operator fun plus(other: SyncPathStats): SyncPathStats =
        SyncPathStats(procs + other.procs)

    fun formatSummary(config: SyncResolveConfig): String = buildString {
        appendLine("=== julay sync path summary ===")
        appendLine(
            "sync opts: eq-unify=${onOff(config.eqUnify)} " +
                "arg-ownership=${onOff(config.argOwnership)} " +
                "directed-eval=${onOff(config.directedEval)}",
        )
        if (!config.anyEnabled()) {
            appendLine(
                "note: all sync opts disabled — runtime always uses NeedsZ3 even for FastOnly codegen",
            )
        }
        appendLine("procs: $fastOnlyProcCount FastOnly, $needsZ3ProcCount NeedsZ3")
        appendLine("actions: $fastGuardActionCount with fastGuard, $opaqueActionCount opaque")
        appendLine(
            "note: tallies cover generated Julay procs only (not Kotlin library TransitionSystems)",
        )
        val needsZ3 = procs.filter { !it.fastOnly }.sortedBy { it.name }
        if (needsZ3.isNotEmpty()) {
            appendLine()
            appendLine("NeedsZ3 procs:")
            for (p in needsZ3) {
                val opaque = p.actions.filter { !it.hasFastGuard }
                val opaquePart = if (opaque.isEmpty()) {
                    "no opaque guards (other action in class blocked FastOnly)"
                } else {
                    opaque.joinToString("; ") { a ->
                        "${a.name}.guard (${a.opaqueReason ?: "opaque"})"
                    }
                }
                appendLine(
                    "  ${p.name}: ${p.actions.size} actions " +
                        "(${p.fastGuardCount} fastGuard, ${p.opaqueCount} opaque) — opaque: $opaquePart",
                )
            }
        }
        val fastOnly = procs.filter { it.fastOnly }.sortedBy { it.name }
        if (fastOnly.isNotEmpty()) {
            appendLine()
            append("FastOnly procs: ")
            appendLine(fastOnly.joinToString(", ") { it.name })
        }
    }

    companion object {
        val EMPTY = SyncPathStats()
        private fun onOff(v: Boolean): String = if (v) "on" else "off"
    }
}
