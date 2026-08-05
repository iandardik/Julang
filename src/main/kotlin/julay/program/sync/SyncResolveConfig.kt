package julay.program.sync

/**
 * Which named sync resolve optimizations are enabled for a compiled program
 * (`eq-unify` / `arg-ownership` / `directed-eval`).
 *
 * When on, Julay may use [BoolExprFast] / [SyncResolveFast] to avoid Z3 on equality-shaped
 * sync; unsupported shapes still use residual Z3. Residual Z3 is not a disableable
 * optimization id.
 */
data class SyncResolveConfig(
    val eqUnify: Boolean = true,
    val argOwnership: Boolean = true,
    val directedEval: Boolean = true,
) {
    fun anyEnabled(): Boolean = eqUnify || argOwnership || directedEval

    companion object {
        val ALL_ON = SyncResolveConfig()
        val ALL_OFF = SyncResolveConfig(
            eqUnify = false,
            argOwnership = false,
            directedEval = false,
        )

        /** Stable CLI / docs IDs. */
        val OPT_IDS: Set<String> = setOf("eq-unify", "arg-ownership", "directed-eval")

        /**
         * @param raw `null` if `--disable-opt` was not passed; `"ALL"` for a bare flag;
         *   otherwise a comma-separated list of [OPT_IDS].
         * @throws IllegalArgumentException on unknown names
         */
        fun fromDisableOptFlag(raw: String?): SyncResolveConfig {
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
            return SyncResolveConfig(
                eqUnify = "eq-unify" !in disabled,
                argOwnership = "arg-ownership" !in disabled,
                directedEval = "directed-eval" !in disabled,
            )
        }

        /** Expression embedded in generated program mains. */
        fun toKotlinExpr(config: SyncResolveConfig): String =
            "SyncResolveConfig(" +
                "eqUnify=${config.eqUnify}, " +
                "argOwnership=${config.argOwnership}, " +
                "directedEval=${config.directedEval})"
    }
}
