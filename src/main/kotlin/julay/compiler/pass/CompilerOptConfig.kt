package julay.compiler.pass

import julay.program.sync.SyncResolveConfig

/**
 * Compile-time JAR optimization flags.
 *
 * [syncResolve] is baked into generated [julay.program.Program] (runtime sync path).
 * [procfunFuse] affects codegen only (nested procfun inlining); it is not a Program field.
 *
 * Parsed from `--disable-opt` / `--disable-opt=ID,...` (see [fromDisableOptFlag]).
 */
data class CompilerOptConfig(
    val syncResolve: SyncResolveConfig = SyncResolveConfig.ALL_ON,
    /** When true, nested procfun calls are fused into the caller TransitionSystem. */
    val procfunFuse: Boolean = true,
    // Reserved for later: val procFuse: Boolean = false,
) {
    companion object {
        val ALL_ON = CompilerOptConfig()
        val ALL_OFF = CompilerOptConfig(
            syncResolve = SyncResolveConfig.ALL_OFF,
            procfunFuse = false,
        )

        /** Stable CLI / docs IDs (sync resolve + JAR codegen). */
        val OPT_IDS: Set<String> =
            SyncResolveConfig.OPT_IDS + setOf("procfun-fuse")

        /**
         * @param raw `null` if `--disable-opt` was not passed; `"ALL"` for a bare flag;
         *   otherwise a comma-separated list of [OPT_IDS].
         * @throws IllegalArgumentException on unknown names
         */
        fun fromDisableOptFlag(raw: String?): CompilerOptConfig {
            if (raw == null) return ALL_ON
            if (raw == "ALL" || raw.isBlank()) return ALL_OFF
            val names = raw.split(',')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            if (names.isEmpty()) return ALL_OFF
            val unknown = names.filter { it !in OPT_IDS }
            require(unknown.isEmpty()) {
                "Unknown optimization id(s): ${unknown.joinToString(", ")}. " +
                    "Valid: ${OPT_IDS.joinToString(", ")}"
            }
            val disabled = names.toSet()
            return CompilerOptConfig(
                syncResolve = SyncResolveConfig(
                    eqUnify = "eq-unify" !in disabled,
                    argOwnership = "arg-ownership" !in disabled,
                    directedEval = "directed-eval" !in disabled,
                ),
                procfunFuse = "procfun-fuse" !in disabled,
            )
        }
    }
}
