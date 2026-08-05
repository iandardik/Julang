package julay.program

/**
 * Which sync fast paths are enabled for a compiled program.
 *
 * Residual Z3 always remains available when a shape is unsupported or an opt is off.
 * Named IDs match `--disable-opt` (see [fromDisableOptFlag]).
 */
data class SyncOptimizeConfig(
    val eqUnify: Boolean = true,
    val argOwnership: Boolean = true,
    val directedEval: Boolean = true,
) {
    fun anyEnabled(): Boolean = eqUnify || argOwnership || directedEval

    companion object {
        val ALL_ON = SyncOptimizeConfig()
        val ALL_OFF = SyncOptimizeConfig(
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
        fun fromDisableOptFlag(raw: String?): SyncOptimizeConfig {
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
            return SyncOptimizeConfig(
                eqUnify = "eq-unify" !in disabled,
                argOwnership = "arg-ownership" !in disabled,
                directedEval = "directed-eval" !in disabled,
            )
        }

        /** Expression embedded in generated program mains. */
        fun toKotlinExpr(config: SyncOptimizeConfig): String =
            "SyncOptimizeConfig(" +
                "eqUnify=${config.eqUnify}, " +
                "argOwnership=${config.argOwnership}, " +
                "directedEval=${config.directedEval})"
    }
}
