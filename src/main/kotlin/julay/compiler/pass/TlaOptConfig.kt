package julay.compiler.pass

/**
 * Which named TLA+ emission optimizations are enabled (`unused-fields`, …).
 *
 * Compile-time only: affects `.tla` / `.cfg` output, not generated JARs.
 * Independent of `--disable-opt` (retired JAR sync/fuse flags).
 *
 * Default-on opts are disabled with `--disable-tla-opt`. Opt-in opts (off by
 * default) are enabled with `--opt-tla` when any exist.
 */
data class TlaOptConfig(
    val unusedFields: Boolean = true,
    val unusedVars: Boolean = true,
    val determinedArgs: Boolean = true,
    val fromCollection: Boolean = true,
    val literalDomains: Boolean = true,
    val unwrapSingletons: Boolean = true,
    val unusedLets: Boolean = true,
    /**
     * When a retained obj state var is only read through one field, emit that field as
     * `var_field` (e.g. `me` + `.url` → `me_url`). Default on.
     */
    val fieldVars: Boolean = true,
    /**
     * Map IndexPut as `DOMAIN ∪ {key}` rebuild (avoids TLC EXCEPT-on-missing-field
     * warnings). Default on; disable with `--disable-tla-opt=map-index-put`.
     */
    val mapIndexPutRebuild: Boolean = true,
) {
    /**
     * Merge opt-in flags from [optIn] (OR only default-off ids).
     * Default-on flags come solely from this config (`--disable-tla-opt`).
     */
    fun withOptIn(@Suppress("UNUSED_PARAMETER") optIn: TlaOptConfig): TlaOptConfig =
        // No OPT_IN_IDS currently; map-index-put is default-on.
        this

    companion object {
        /** Default-on named opts enabled. */
        val ALL_ON = TlaOptConfig()
        val ALL_OFF = TlaOptConfig(
            unusedFields = false,
            unusedVars = false,
            determinedArgs = false,
            fromCollection = false,
            literalDomains = false,
            unwrapSingletons = false,
            unusedLets = false,
            fieldVars = false,
            mapIndexPutRebuild = false,
        )

        /** Stable CLI / docs IDs for `--disable-tla-opt` (default-on). */
        val OPT_IDS: Set<String> = setOf(
            "unused-fields",
            "unused-vars",
            "determined-args",
            "from-collection",
            "literal-domains",
            "unwrap-singletons",
            "unused-lets",
            "field-vars",
            "map-index-put",
        )

        /** Stable CLI / docs IDs for `--opt-tla` (default-off). None currently. */
        val OPT_IN_IDS: Set<String> = emptySet()

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
                unusedVars = "unused-vars" !in disabled,
                determinedArgs = "determined-args" !in disabled,
                fromCollection = "from-collection" !in disabled,
                literalDomains = "literal-domains" !in disabled,
                unwrapSingletons = "unwrap-singletons" !in disabled,
                unusedLets = "unused-lets" !in disabled,
                fieldVars = "field-vars" !in disabled,
                mapIndexPutRebuild = "map-index-put" !in disabled,
            )
        }

        /**
         * @param raw `null` if `--opt-tla` was not passed; `"ALL"` for a bare flag;
         *   otherwise a comma-separated list of [OPT_IN_IDS].
         * Legacy `map-index-put` is accepted as a no-op (now default-on).
         * @throws IllegalArgumentException on unknown names
         */
        fun fromOptTlaFlag(raw: String?): TlaOptConfig {
            if (raw == null) return TlaOptConfig()
            if (raw == "ALL" || raw.isBlank()) {
                return if (OPT_IN_IDS.isEmpty()) TlaOptConfig() else TlaOptConfig()
            }
            val names = raw.split(',')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            if (names.isEmpty()) return TlaOptConfig()
            val legacy = setOf("map-index-put")
            val unknown = names.filter { it !in OPT_IN_IDS && it !in legacy }
            require(unknown.isEmpty()) {
                val valid = (OPT_IN_IDS + legacy).joinToString(", ").ifEmpty { "(none)" }
                "Unknown TLA+ opt-in id(s): ${unknown.joinToString(", ")}. Valid: $valid"
            }
            // map-index-put is default-on; legacy --opt-tla=map-index-put is a no-op.
            return TlaOptConfig()
        }

        fun fromFlags(disableRaw: String?, optRaw: String?): TlaOptConfig =
            fromDisableTlaOptFlag(disableRaw).withOptIn(fromOptTlaFlag(optRaw))
    }
}
