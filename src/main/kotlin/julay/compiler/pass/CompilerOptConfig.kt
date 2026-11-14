package julay.compiler.pass

/**
 * Compile-time JAR optimization flags.
 *
 * Sync resolve / procfun-fuse options are retired with the Kotlin runtime; `--disable-opt`
 * still accepts the old IDs as no-ops for CLI compatibility.
 */
data class CompilerOptConfig(
    /** Reserved no-op (former SyncChannel fast-path). */
    val syncResolveNoop: Boolean = true,
    /** Reserved no-op (former Kotlin procfun fuse). */
    val procfunFuse: Boolean = true,
) {
    companion object {
        val ALL_ON = CompilerOptConfig()
        val ALL_OFF = CompilerOptConfig(syncResolveNoop = false, procfunFuse = false)

        /** Former CLI / docs IDs retained as accepted no-ops. */
        val OPT_IDS: Set<String> = setOf(
            "eq-unify",
            "arg-ownership",
            "directed-eval",
            "procfun-fuse",
        )

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
                syncResolveNoop = "eq-unify" !in disabled &&
                    "arg-ownership" !in disabled &&
                    "directed-eval" !in disabled,
                procfunFuse = "procfun-fuse" !in disabled,
            )
        }
    }
}
