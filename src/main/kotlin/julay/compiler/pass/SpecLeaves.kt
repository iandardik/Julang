package julay.compiler.pass

import julay.compiler.TypeExpr
import julay.compiler.ast.*

/**
 * A leaf in a spec system/assume expression.
 * [occurrenceId] distinguishes multiple occurrences of the same class; identity is not idempotent.
 */
data class SpecLeaf(
    val name: String,
    val paramName: String? = null,
    val paramType: TypeExpr? = null,
    val occurrenceId: String = "",
    /** Named assembly that introduced this occurrence (for TLA `{Class}_{Assembly}` renaming). */
    val introducingAssembly: String = name,
    /** Assigned TLA identifier (may differ from [name] when renaming for ties). */
    val tlaName: String = name,
) {
    val isParameterized: Boolean get() = paramName != null
    fun identityKey(): String {
        val base = if (paramName != null) "$name[$paramName:${paramType}]" else name
        return if (occurrenceId.isNotEmpty()) "$base#$occurrenceId" else base
    }
}

private var specOccCounter = 0

private fun freshSpecOccurrenceId(pclass: String): String {
    specOccCounter += 1
    val safe = pclass.replace(Regex("[^A-Za-z0-9_]"), "_")
    return "${safe}_spec$specOccCounter"
}

fun resetSpecOccurrenceCounter() {
    specOccCounter = 0
}

/** Flatten a system/assume expr keeping every occurrence (`X || X` → two leaves). */
fun flattenSpecLeaves(node: ASTNode?, introducingAssembly: String = ""): List<SpecLeaf> {
    if (node == null) return emptyList()
    val out = mutableListOf<SpecLeaf>()
    fun walk(n: ASTNode, intro: String) {
        when (n) {
            is ValueProcExprNode -> {
                val name = n.valueProcName()
                val assembly = intro.ifEmpty { name }
                out += SpecLeaf(
                    name = name,
                    occurrenceId = freshSpecOccurrenceId(name),
                    introducingAssembly = assembly,
                    tlaName = name,
                )
            }
            is ParamProcExprNode -> {
                val paramName = n.paramName()
                val paramType = n.paramType()
                flattenSpecLeaves(n.paramBody(), intro).forEach { child ->
                    out += if (!child.isParameterized) {
                        child.copy(paramName = paramName, paramType = paramType)
                    } else {
                        child
                    }
                }
            }
            is CompositeProcExprNode -> n.compositeProcChildren().forEach { walk(it, intro) }
            is AgSpecExprNode -> {
                n.assumeExpr()?.let { walk(it, intro) }
                walk(n.systemExpr(), intro)
            }
            else -> n.children.forEach { walk(it, intro) }
        }
    }
    walk(node, introducingAssembly)
    return out
}

fun systemLeavesOfSpec(spec: SpecNode): List<SpecLeaf> {
    resetSpecOccurrenceCounter()
    val value = spec.specNodeValue()
    return when (value) {
        is AgSpecExprNode -> flattenSpecLeaves(value.systemExpr(), spec.specNodeName())
        else -> flattenSpecLeaves(value, spec.specNodeName())
    }
}

fun assumeLeavesOfSpec(spec: SpecNode): List<SpecLeaf> {
    resetSpecOccurrenceCounter()
    val value = spec.specNodeValue()
    return when (value) {
        is AgSpecExprNode -> flattenSpecLeaves(value.assumeExpr(), spec.specNodeName())
        else -> emptyList()
    }
}

fun compositionLeavesOfSpec(spec: SpecNode): List<SpecLeaf> {
    resetSpecOccurrenceCounter()
    val value = spec.specNodeValue()
    val assembly = spec.specNodeName()
    return when (value) {
        is AgSpecExprNode -> {
            if (value.assumeExpr() == null) {
                flattenSpecLeaves(value.systemExpr(), assembly)
            } else {
                flattenSpecLeaves(value.assumeExpr(), assembly) +
                    flattenSpecLeaves(value.systemExpr(), assembly)
            }
        }
        else -> flattenSpecLeaves(value, assembly)
    }
}

/**
 * Expand named `proc` / `spec` aliases to their nested proc-class leaves (by occurrence).
 * Parameterization on an outer leaf is pushed down onto non-parameterized children.
 *
 * Nested AG specs contribute only their system expression (not assume/guarantee).
 */
fun expandLeavesToPclasses(
    leaves: List<SpecLeaf>,
    pclasses: Map<String, ProcClassNode>,
    procAliases: Map<String, ProcNode>,
    specAliases: Map<String, SpecNode> = emptyMap(),
): List<SpecLeaf> {
    val out = mutableListOf<SpecLeaf>()
    val visiting = mutableSetOf<String>()

    fun pushDown(outer: SpecLeaf, child: SpecLeaf): SpecLeaf =
        if (outer.isParameterized && !child.isParameterized) {
            child.copy(paramName = outer.paramName, paramType = outer.paramType)
        } else {
            child
        }

    fun childrenOfSpec(spec: SpecNode): List<SpecLeaf> {
        val value = spec.specNodeValue()
        return when (value) {
            is AgSpecExprNode -> flattenSpecLeaves(value.systemExpr(), spec.specNodeName())
            else -> flattenSpecLeaves(value, spec.specNodeName())
        }
    }

    fun expand(leaf: SpecLeaf) {
        when {
            leaf.name in pclasses -> out += leaf
            leaf.name in procAliases -> {
                val proc = procAliases.getValue(leaf.name)
                // Expand the alias body with introducing assembly = this proc's name.
                flattenSpecLeaves(proc.procNodeValue(), leaf.name).forEach { child ->
                    expand(pushDown(leaf, child.copy(introducingAssembly = leaf.name)))
                }
            }
            leaf.name in specAliases -> {
                if (!visiting.add(leaf.name)) {
                    out += leaf
                    return
                }
                try {
                    childrenOfSpec(specAliases.getValue(leaf.name)).forEach { child ->
                        expand(pushDown(leaf, child))
                    }
                } finally {
                    visiting.remove(leaf.name)
                }
            }
            else -> out += leaf
        }
    }
    leaves.forEach { expand(it) }
    return assignTlaLeafNames(out)
}

/**
 * Assign TLA leaf names: unique class stays bare; on a tie, every occurrence becomes
 * `{Class}_{IntroducingAssembly}`; if that still clashes, `{Class}_{Assembly}_1`, `_2`, …
 */
fun assignTlaLeafNames(leaves: List<SpecLeaf>): List<SpecLeaf> {
    val countByClass = leaves.groupingBy { it.name }.eachCount()
    val preferred = leaves.map { leaf ->
        val base = if (countByClass.getValue(leaf.name) == 1) {
            leaf.name
        } else {
            "${leaf.name}_${leaf.introducingAssembly}"
        }
        leaf to base
    }
    val preferredCounts = preferred.groupingBy { it.second }.eachCount()
    val used = mutableSetOf<String>()
    val seq = mutableMapOf<String, Int>()
    return preferred.map { (leaf, base) ->
        val tlaName = if (preferredCounts.getValue(base) == 1 && base !in used) {
            base
        } else {
            val n = (seq[base] ?: 0) + 1
            seq[base] = n
            var candidate = "${base}_$n"
            while (candidate in used) {
                val next = (seq[base] ?: 0) + 1
                seq[base] = next
                candidate = "${base}_$next"
            }
            candidate
        }
        used += tlaName
        leaf.copy(tlaName = tlaName)
    }
}
