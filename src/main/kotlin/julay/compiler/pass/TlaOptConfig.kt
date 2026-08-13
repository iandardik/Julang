package julay.compiler.pass

/**
 * Which named TLA+ emission optimizations are enabled (`unused-fields`, …).
 *
 * Compile-time only: affects `.tla` / `.cfg` output, not generated JARs.
 * Independent of [julay.program.sync.SyncResolveConfig] / `--disable-opt`.
 */
data class TlaOptConfig(
    val unusedFields: Boolean = true,
) {
    companion object {
        val ALL_ON = TlaOptConfig()
        val ALL_OFF = TlaOptConfig(unusedFields = false)

        /** Stable CLI / docs IDs. */
        val OPT_IDS: Set<String> = setOf("unused-fields")

        /**
         * @param raw `null` if `--disable-tla-opt` was not passed; `"ALL"` for a bare flag;
         *   otherwise a comma-separated list of [OPT_IDS].
         * @throws IllegalArgumentException on unknown names
         */
        fun fromDisableTlaOptFlag(raw: String?): TlaOptConfig {
            if (raw == null) return ALL_ON
            if (raw == "ALL" || raw.isBlank()) return ALL_OFF
            val names = raw.split(',')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            if (names.isEmpty()) return ALL_OFF
            val unknown = names.filter { it !in OPT_IDS }
            require(unknown.isEmpty()) {
                "Unknown TLA+ optimization id(s): ${unknown.joinToString(", ")}. " +
                    "Valid: ${OPT_IDS.joinToString(", ")}"
            }
            val disabled = names.toSet()
            return TlaOptConfig(
                unusedFields = "unused-fields" !in disabled,
            )
        }
    }
}
