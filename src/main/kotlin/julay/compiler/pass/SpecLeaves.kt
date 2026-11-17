package julay.compiler.pass

import julay.compiler.TypeExpr
import julay.compiler.ast.*
import java.util.ArrayDeque

/**
 * A leaf in a spec system/assume expression.
 * [occurrenceId] distinguishes multiple occurrences of the same class; identity is not idempotent.
 */
/** Reserved create-index delayed model: slot set indexing every dynamic proc leaf. */
const val DYN_PROC_MODEL = "DynProc"

/** Slot model used when no `DynProc := { ... }` is declared. */
const val DEFAULT_DYN_PROC_MODEL = "{1, 2}"

data class SpecLeaf(
    val name: String,
    val paramName: String? = null,
    val paramType: TypeExpr? = null,
    /**
     * Slot index (`DynProc`) for a dynamic leaf under a create-index: `X[n][i]`.
     * Unindexed dynamic leaves use [paramName] / [paramType] for the slot instead (`X[i]`).
     */
    val instParamName: String? = null,
    val instParamType: TypeExpr? = null,
    val occurrenceId: String = "",
    /** Named assembly that introduced this occurrence (for TLA `{Class}_{Assembly}` renaming). */
    val introducingAssembly: String = name,
    /** Assigned TLA identifier (may differ from [name] when renaming for ties). */
    val tlaName: String = name,
    /** True for a dynamic (non-static) proc class: indexed by `DynProc` slots. */
    val isDynamic: Boolean = false,
    /** Parent proc class for a `B<A>` child occurrence. */
    val parentClass: String? = null,
    /** True when the `DynProc` slot is this leaf's only index ([paramName] is the slot binder). */
    val slotIsParam: Boolean = false,
    /**
     * When non-null, this leaf's index was applied under `with (binder)`; leaves that share the
     * same [withScopeId] use one TLA binder (no per-leaf clash rename).
     */
    val withScopeId: String? = null,
    /**
     * Create-index `global` / `const global` state vars: emitted as scalar TLA VARIABLES
     * (not functions of the index). Model-only; does not affect JAR codegen.
     */
    val globalVars: Set<String> = emptySet(),
    /**
     * Subset of [globalVars] listed as `const global`: Init is unconstrained (`\\in` TypeOK),
     * constructors never prime the variable.
     */
    val globalConstVars: Set<String> = emptySet(),
    /**
     * Create-index `init:` Boolean exprs: extra Init conjuncts (const-global constraints).
     */
    val initExprs: List<ExprNode> = emptyList(),
    /** Create-index delayed domain models (`Name := { … }`). */
    val typeModels: List<TypeModelNode> = emptyList(),
    /** Create-index / leaf-spec nullary fun overrides (`name := expr`); TLA-only. */
    val funModels: List<FunModelNode> = emptyList(),
    /**
     * Peeled `DynProc := {…}` model (not a domain type): cfg literals for the reusable
     * dynamic-proc slot set; only set when the create-index declares DynProc.
     */
    val dynProcModel: TypeModelNode? = null,
) {
    val isParameterized: Boolean get() = paramName != null && paramType != null
    val isDualIndexed: Boolean
        get() = isParameterized && instParamName != null && instParamType != null
    /** True when this leaf is create-indexed and [varName] is not a `global` model var. */
    fun indexesState(varName: String): Boolean = isParameterized && varName !in globalVars
    fun identityKey(): String {
        val base = if (paramName != null) "$name[$paramName:${paramType}]" else name
        val withInst = if (instParamName != null) "$base[${instParamName}:${instParamType}]" else base
        return if (occurrenceId.isNotEmpty()) "$withInst#$occurrenceId" else withInst
    }
}

private var withScopeCounter = 0

private fun freshWithScopeId(): String {
    withScopeCounter += 1
    return "with_$withScopeCounter"
}

private var specOccCounter = 0

private fun freshSpecOccurrenceId(pclass: String): String {
    specOccCounter += 1
    val safe = pclass.replace(Regex("[^A-Za-z0-9_]"), "_")
    return "${safe}_spec$specOccCounter"
}

fun resetSpecOccurrenceCounter() {
    specOccCounter = 0
    withScopeCounter = 0
}

/** Flatten a system/assume expr keeping every occurrence (`X || X` → two leaves). */
fun flattenSpecLeaves(node: ASTNode?, introducingAssembly: String = ""): List<SpecLeaf> {
    if (node == null) return emptyList()
    val out = mutableListOf<SpecLeaf>()
    fun walk(
        n: ASTNode,
        intro: String,
        withBinders: Map<String, TypeExpr> = emptyMap(),
        withScopeId: String? = null,
    ) {
        when (n) {
            is ValueProcExprNode -> {
                val name = n.valueProcName()
                val assembly = intro.ifEmpty { name }
                out += SpecLeaf(
                    name = name,
                    occurrenceId = freshSpecOccurrenceId(name),
                    introducingAssembly = assembly,
                    tlaName = name,
                    withScopeId = withScopeId,
                )
            }
            is WithSpecExprNode -> {
                val scopeId = freshWithScopeId()
                val binders = withBinders + (n.withBinderName() to n.withBinderType())
                walk(n.withBody(), intro, binders, scopeId)
            }
            is ParamProcExprNode -> {
                val paramName = n.paramName()
                val globals = n.globalVarNames().toSet()
                val constGlobals = n.globalConstVarNames().toSet()
                val inits = n.initExprs()
                val typeModels = n.typeModels()
                val funModels = n.funModels()
                // DynProc is a reserved create-index slot set, not a domain type.
                val dynProcModel = typeModels.firstOrNull { it.name() == DYN_PROC_MODEL }
                val domainTypeModels = typeModels.filter { it.name() != DYN_PROC_MODEL }
                fun withGlobals(child: SpecLeaf): SpecLeaf =
                    if (globals.isEmpty() && inits.isEmpty() && domainTypeModels.isEmpty() &&
                        funModels.isEmpty() && dynProcModel == null
                    ) {
                        child
                    } else {
                        child.copy(
                            globalVars = child.globalVars + globals,
                            globalConstVars = child.globalConstVars + constGlobals,
                            initExprs = child.initExprs + inits,
                            typeModels = child.typeModels + domainTypeModels,
                            funModels = child.funModels + funModels,
                            dynProcModel = child.dynProcModel ?: dynProcModel,
                        )
                    }
                when {
                    n.isApplyIndex() -> {
                        // Apply: share binder / with-scope only. Do not lift unindexed state.
                        // Create-indexed children keep lifting; binder renamed to the with-name.
                        flattenSpecLeaves(n.paramBody(), intro).forEach { child ->
                            out += if (child.isParameterized) {
                                withGlobals(child.copy(paramName = paramName, withScopeId = withScopeId))
                            } else {
                                withGlobals(
                                    child.copy(
                                        paramName = paramName,
                                        paramType = null,
                                        withScopeId = withScopeId,
                                    ),
                                )
                            }
                        }
                    }
                    // Shorthand (A || B)[n : T] → shared with-scope + create-index on each peer
                    n.paramBody() is CompositeProcExprNode -> {
                        val scopeId = freshWithScopeId()
                        val pType = n.paramType()!!
                        flattenSpecLeaves(n.paramBody(), intro).forEach { child ->
                            out += if (!child.isParameterized) {
                                withGlobals(
                                    child.copy(
                                        paramName = paramName,
                                        paramType = pType,
                                        withScopeId = scopeId,
                                    ),
                                )
                            } else {
                                withGlobals(child)
                            }
                        }
                    }
                    else -> {
                        val pType = n.paramType()!!
                        flattenSpecLeaves(n.paramBody(), intro).forEach { child ->
                            out += if (!child.isParameterized) {
                                withGlobals(child.copy(paramName = paramName, paramType = pType))
                            } else {
                                withGlobals(child)
                            }
                        }
                    }
                }
            }
            is CompositeProcExprNode ->
                n.compositeProcChildren().forEach { walk(it, intro, withBinders, withScopeId) }
            is AgSpecExprNode -> {
                n.assumeExpr()?.let { walk(it, intro, withBinders, withScopeId) }
                walk(n.systemExpr(), intro, withBinders, withScopeId)
            }
            else -> n.children.forEach { walk(it, intro, withBinders, withScopeId) }
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
 * Dynamic leaves get their `DynProc` slot index ([SpecLeaf.isDynamic]).
 */
fun expandLeavesToPclasses(
    leaves: List<SpecLeaf>,
    pclasses: Map<String, ProcClassNode>,
    procAliases: Map<String, ProcNode>,
    specAliases: Map<String, SpecNode> = emptyMap(),
    leafSpecs: Map<String, LeafSpecNode> = emptyMap(),
    alphabet: CompositionAlphabetResult? = null,
): List<SpecLeaf> {
    val out = mutableListOf<SpecLeaf>()
    val visiting = mutableSetOf<String>()

    fun pushDown(outer: SpecLeaf, child: SpecLeaf): SpecLeaf {
        var c = child.copy(
            withScopeId = outer.withScopeId ?: child.withScopeId,
            globalVars = child.globalVars + outer.globalVars,
            globalConstVars = child.globalConstVars + outer.globalConstVars,
            initExprs = child.initExprs + outer.initExprs,
            typeModels = child.typeModels + outer.typeModels,
            funModels = child.funModels + outer.funModels,
            dynProcModel = child.dynProcModel ?: outer.dynProcModel,
        )
        when {
            outer.isParameterized && !c.isParameterized ->
                c = c.copy(paramName = outer.paramName, paramType = outer.paramType)
            outer.paramName != null && c.isParameterized ->
                // Apply / shared with: rename create-index binder to the shared name.
                c = c.copy(paramName = outer.paramName)
            outer.paramName != null && !c.isParameterized && outer.withScopeId != null ->
                c = c.copy(paramName = outer.paramName, paramType = null)
        }
        return c
    }

    fun childrenOfSpec(spec: SpecNode): List<SpecLeaf> {
        val value = spec.specNodeValue()
        return when (value) {
            is AgSpecExprNode -> flattenSpecLeaves(value.systemExpr(), spec.specNodeName())
            else -> flattenSpecLeaves(value, spec.specNodeName())
        }
    }

    fun declaredStateNames(leafName: String): Set<String> {
        val pc = pclasses[leafName] ?: leafSpecs[leafName]?.asProcClass() ?: return emptySet()
        return pc.localDecls().filterIsInstance<VarNode>().map { it.name }.toSet()
    }

    fun filterGlobals(leaf: SpecLeaf): SpecLeaf {
        if (leaf.globalVars.isEmpty()) return leaf
        val declared = declaredStateNames(leaf.name)
        if (declared.isEmpty()) return leaf
        return leaf.copy(
            globalVars = leaf.globalVars.intersect(declared),
            globalConstVars = leaf.globalConstVars.intersect(declared),
        )
    }

    fun expand(leaf: SpecLeaf) {
        when {
            leaf.name in leafSpecs || leaf.name in pclasses -> out += filterGlobals(leaf)
            leaf.name in procAliases -> {
                val proc = procAliases.getValue(leaf.name)
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
            else -> out += filterGlobals(leaf)
        }
    }
    leaves.forEach { expand(it) }
    val withSlots = out.map { leaf -> if (leaf.name in leafSpecs) leaf else withDynProcSlot(leaf, pclasses[leaf.name]) }
    val withParents = if (alphabet != null) applyInferredParents(withSlots, alphabet) else withSlots
    return assignTlaLeafNames(withParents)
}

/** Give a dynamic proc leaf its `DynProc` slot: `X[i]` unindexed, `X[n][i]` under a create-index. */
private fun withDynProcSlot(leaf: SpecLeaf, pc: ProcClassNode?): SpecLeaf {
    if (pc == null || pc.isStatic) return leaf
    val slotType = TypeExpr.Simple(DYN_PROC_MODEL)
    val stateNames = pc.localDecls().filterIsInstance<VarNode>().map { it.name }.toSet()
    fun freeBinder(taken: Set<String>): String =
        listOf("i", "j", "slot").firstOrNull { it !in taken && it !in stateNames } ?: "slot_"
    return if (leaf.isParameterized) {
        leaf.copy(
            isDynamic = true,
            instParamName = freeBinder(setOf(leaf.paramName!!)),
            instParamType = slotType,
        )
    } else {
        leaf.copy(
            isDynamic = true,
            paramName = freeBinder(emptySet()),
            paramType = slotType,
            withScopeId = null,
            slotIsParam = true,
        )
    }
}

/**
 * Assign TLA leaf names: unique class stays bare; on a tie, every occurrence becomes
 * `{Class}_{IntroducingAssembly}`; if that still clashes, `{Class}_{Assembly}_1`, `_2`, …
 */
fun assignTlaLeafNames(leaves: List<SpecLeaf>): List<SpecLeaf> {
    val countByClass = leaves.groupingBy { it.name }.eachCount()
    val preferred = leaves.map { leaf ->
        val base = when {
            countByClass.getValue(leaf.name) == 1 -> leaf.name
            leaf.parentClass != null -> "${leaf.name}_${leaf.parentClass}"
            else -> "${leaf.name}_${leaf.introducingAssembly}"
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
