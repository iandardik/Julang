package julay.compiler.pass

import julay.compiler.TypeExpr
import julay.compiler.decl.ObjClassRegistry
import julay.compiler.decl.TypeResolveResult
import julay.program.Variable
import julay.program.type.IntType
import julay.program.type.ObjClassType
import julay.program.type.StringType
import julay.program.type.Type

/**
 * Record types used as create-index sorts: TLA encodes values as opaque CONSTANT atoms
 * with per-field projection VARIABLES (`Node_url`, `Node_id`), not as TLA records.
 * JAR/Go still use full structs.
 *
 * Empty / sentinel literals (e.g. `emptyNode()` with `url := ""`) emit a distinguished
 * atom that is **not** in the create-index CONSTANT [sort], so `peer = emptyNode` never
 * skips a live replica. Top-level Node-typed state uses [valueDomain]; nested / `\E`
 * domains stay on the live CONSTANT. Projections are defined on live atoms plus the sentinel.
 */
internal object TlaOpaqueIndexSort {
    private var byName: Map<String, ObjClassType> = emptyMap()
    /** sort → raw (unquoted) sentinel string atom, disjoint from the live cfg model. */
    private var sentinels: Map<String, String> = emptyMap()

    fun install(sorts: Map<String, ObjClassType>) {
        byName = sorts
        sentinels = emptyMap()
    }

    /**
     * Pick a sentinel atom per opaque sort from its delayed-model cfg elements
     * (quoted tokens like `"n1"`). Call after domain models are known.
     */
    fun installSentinels(opaqueAtoms: Map<String, List<String>>) {
        sentinels = byName.keys.associateWith { sort ->
            val live = opaqueAtoms[sort].orEmpty()
                .mapNotNull { rawStringFromCfgElement(it) }
                .toSet()
            pickOpaqueSentinelAtom(live)
        }
    }

    fun clear() {
        byName = emptyMap()
        sentinels = emptyMap()
    }

    fun isOpaque(name: String): Boolean = name in byName

    fun isOpaque(type: Type): Boolean =
        type is ObjClassType && type.name in byName

    fun sorts(): Map<String, ObjClassType> = byName

    fun fields(name: String): List<Variable> =
        byName[name]?.fields.orEmpty()

    /** TLA VARIABLE / operator name for a field projection over [sort]. */
    fun projectionName(sort: String, field: String): String = "${sort}_$field"

    fun allProjectionNames(): Set<String> =
        byName.flatMap { (sort, ty) ->
            ty.fields.map { projectionName(sort, it.name) }
        }.toSet()

    fun sentinelRaw(sort: String): String? = sentinels[sort]

    /** TLA string literal for the sentinel (e.g. `""` or `"#"`). */
    fun sentinelTla(sort: String): String {
        val raw = sentinels[sort] ?: pickOpaqueSentinelAtom(emptySet())
        return "\"${escapeTlaStringLiteral(raw)}\""
    }

    /**
     * Domain for top-level Node-typed *values* that may be empty (scalars in TypeOK).
     * Nested fields / list-map elements / action `\E` use the bare CONSTANT [sort].
     */
    fun valueDomain(sort: String): String {
        if (sort !in byName) return sort
        return "($sort \\cup {${sentinelTla(sort)}})"
    }

    /** Projection / Init value of the sentinel for [field] (url → sentinel atom, id → -1). */
    fun sentinelFieldTla(sort: String, field: Variable): String {
        val sent = sentinelTla(sort)
        return when (field.type) {
            is StringType -> sent
            is IntType -> "-1"
            else -> sent
        }
    }
}

/**
 * Choose a string atom not among [liveAtoms] (raw, unquoted). Prefers `""`, then a few
 * fixed alternatives, then `__empty_0`, `__empty_1`, …
 */
internal fun pickOpaqueSentinelAtom(liveAtoms: Set<String>): String {
    val candidates = listOf("", "#", "__none", "__empty", "None")
    for (c in candidates) {
        if (c !in liveAtoms) return c
    }
    var i = 0
    while (true) {
        val c = "__empty_$i"
        if (c !in liveAtoms) return c
        i++
    }
}

/** Record names that appear as create-index / instance-index parameter types. */
internal fun collectOpaqueIndexSorts(
    leaves: List<SpecLeaf>,
    registry: ObjClassRegistry,
): Map<String, ObjClassType> {
    val out = linkedMapOf<String, ObjClassType>()
    fun note(te: TypeExpr?) {
        if (te == null) return
        when (val r = registry.resolveTypeExpr(te)) {
            is TypeResolveResult.Found -> {
                val ty = r.type
                if (ty is ObjClassType) out[ty.name] = ty
            }
            else -> {}
        }
    }
    leaves.forEach { leaf ->
        note(leaf.paramType)
        note(leaf.instParamType)
    }
    return out
}
