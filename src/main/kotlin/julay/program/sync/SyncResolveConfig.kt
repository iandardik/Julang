package julay.program.sync

/**
 * Which named sync resolve optimizations are enabled for a compiled program
 * (`eq-unify` / `arg-ownership` / `directed-eval`).
 *
 * When on, Julay may use [BoolExprFast] / [SyncResolveFast] to avoid Z3 on equality-shaped
 * sync; unsupported shapes still use residual Z3. Residual Z3 is not a disableable
 * optimization id.
 *
 * JAR codegen opts such as `procfun-fuse` live on [julay.compiler.pass.CompilerOptConfig],
 * not here — they are not baked into [julay.program.Program].
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

        /** Sync-resolve CLI / docs IDs (subset of [julay.compiler.pass.CompilerOptConfig.OPT_IDS]). */
        val OPT_IDS: Set<String> = setOf("eq-unify", "arg-ownership", "directed-eval")

        /**
         * @param raw `null` if `--disable-opt` was not passed; `"ALL"` for a bare flag;
         *   otherwise a comma-separated list of sync ids (unknown names that are only
         *   codegen opts should be parsed via [julay.compiler.pass.CompilerOptConfig]).
         * @throws IllegalArgumentException on unknown names
         */
        fun fromDisableOptFlag(raw: String?): SyncResolveConfig =
            julay.compiler.pass.CompilerOptConfig.fromDisableOptFlag(raw).syncResolve

        /** Expression embedded in generated program mains. */
        fun toKotlinExpr(config: SyncResolveConfig): String =
            "SyncResolveConfig(" +
                "eqUnify=${config.eqUnify}, " +
                "argOwnership=${config.argOwnership}, " +
                "directedEval=${config.directedEval})"
    }
}
