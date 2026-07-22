package julay.compiler.pass

import julay.compiler.TypeExpr
import julay.compiler.ast.*

/**
 * A flattened leaf in a spec system/assume expression.
 * Identity for `||` idempotence is (name, paramName, paramType).
 */
data class SpecLeaf(
    val name: String,
    val paramName: String? = null,
    val paramType: TypeExpr? = null,
) {
    val isParameterized: Boolean get() = paramName != null
    fun identityKey(): String =
        if (paramName != null) "$name[$paramName:${paramType}]" else name
}

/** Flatten a system/assume expr and dedupe identical leaves (`X || X` → `X`). */
fun flattenSpecLeaves(node: ASTNode?): List<SpecLeaf> {
    if (node == null) return emptyList()
    val out = LinkedHashMap<String, SpecLeaf>()
    fun add(leaf: SpecLeaf) {
        out.putIfAbsent(leaf.identityKey(), leaf)
    }
    fun walk(n: ASTNode) {
        when (n) {
            is ValueProcExprNode -> add(SpecLeaf(n.valueProcName()))
            is ParamProcExprNode -> {
                val paramName = n.paramName()
                val paramType = n.paramType()
                flattenSpecLeaves(n.paramBody()).forEach { child ->
                    add(
                        if (!child.isParameterized) {
                            SpecLeaf(child.name, paramName, paramType)
                        } else {
                            child
                        },
                    )
                }
            }
            is CompositeProcExprNode -> n.compositeProcChildren().forEach { walk(it) }
            is AgSpecExprNode -> {
                n.assumeExpr()?.let { walk(it) }
                walk(n.systemExpr())
            }
            else -> n.children.forEach { walk(it) }
        }
    }
    walk(node)
    return out.values.toList()
}

fun systemLeavesOfSpec(spec: SpecNode): List<SpecLeaf> {
    val value = spec.specNodeValue()
    return when (value) {
        is AgSpecExprNode -> flattenSpecLeaves(value.systemExpr())
        else -> flattenSpecLeaves(value)
    }
}

fun assumeLeavesOfSpec(spec: SpecNode): List<SpecLeaf> {
    val value = spec.specNodeValue()
    return when (value) {
        is AgSpecExprNode -> flattenSpecLeaves(value.assumeExpr())
        else -> emptyList()
    }
}

fun compositionLeavesOfSpec(spec: SpecNode): List<SpecLeaf> {
    val value = spec.specNodeValue()
    return when (value) {
        is AgSpecExprNode -> {
            if (value.assumeExpr() == null) {
                flattenSpecLeaves(value.systemExpr())
            } else {
                flattenSpecLeaves(value.assumeExpr()) + flattenSpecLeaves(value.systemExpr())
            }.let { leaves ->
                val out = LinkedHashMap<String, SpecLeaf>()
                leaves.forEach { out.putIfAbsent(it.identityKey(), it) }
                out.values.toList()
            }
        }
        else -> flattenSpecLeaves(value)
    }
}

/**
 * Expand named `proc` / `spec` aliases to their nested proc-class leaves.
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
    val out = LinkedHashMap<String, SpecLeaf>()
    val visiting = mutableSetOf<String>()

    fun add(leaf: SpecLeaf) {
        out.putIfAbsent(leaf.identityKey(), leaf)
    }

    fun pushDown(outer: SpecLeaf, child: SpecLeaf): SpecLeaf =
        if (outer.isParameterized && !child.isParameterized) {
            SpecLeaf(child.name, outer.paramName, outer.paramType)
        } else {
            child
        }

    fun childrenOfSpec(spec: SpecNode): List<SpecLeaf> {
        val value = spec.specNodeValue()
        return when (value) {
            is AgSpecExprNode -> flattenSpecLeaves(value.systemExpr())
            else -> flattenSpecLeaves(value)
        }
    }

    fun expand(leaf: SpecLeaf) {
        when {
            leaf.name in pclasses -> add(leaf)
            leaf.name in procAliases -> {
                flattenSpecLeaves(procAliases.getValue(leaf.name).procNodeValue()).forEach { child ->
                    expand(pushDown(leaf, child))
                }
            }
            leaf.name in specAliases -> {
                if (!visiting.add(leaf.name)) {
                    // Cyclic spec alias; leave unexpanded.
                    add(leaf)
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
            else -> add(leaf)
        }
    }
    leaves.forEach { expand(it) }
    return out.values.toList()
}
