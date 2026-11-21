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

/** How many slots the implicit `DynProc` model provides. */
const val DEFAULT_DYN_PROC_SLOT_COUNT = 2

/**
 * Default DynProc slot names: `dp1`..`dpN`, or `dpp1`.. / `dppp1`.. when those
 * strings are already reserved (other cfg String / domain model elements).
 */
fun defaultDynProcSlotNames(
    reserved: Set<String>,
    count: Int = DEFAULT_DYN_PROC_SLOT_COUNT,
): List<String> {
    require(count >= 1) { "DynProc slot count must be positive" }
    var prefix = "dp"
    while (true) {
        val elems = (1..count).map { "$prefix$it" }
        if (elems.none { it in reserved }) return elems
        prefix += "p"
    }
}

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
     * Create-index `global` state vars: emitted as scalar TLA VARIABLES (not functions of
     * the index). Model-only; does not affect JAR codegen.
     * Only vars listed as `global ThisLeaf.var` apply here.
     */
    val globalVars: Set<String> = emptySet(),
    /**
     * Subset of [globalVars] that are proc `const`: Init is unconstrained (`\\in` TypeOK),
     * constructors never prime the variable. Derived at materialize time (not a keyword).
     */
    val globalConstVars: Set<String> = emptySet(),
    /**
     * Qualified create-index global refs not yet bound to a concrete leaf (e.g. listed on
     * `(Store || ShardLogic)[n]` as `ClusterInfo.cluster` before `ShardLogic` expands).
     * Materialized into [globalVars] when the leaf name matches.
     */
    val pendingGlobalRefs: List<GlobalVarRef> = emptyList(),
    /**
     * Create-index `init:` Boolean exprs: extra Init conjuncts over frozen globals /
     * uninterpreted `.length`.
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
            is SpecCompositionBlockNode -> {
                // Trailing `spec A := system { models; init: … }` — push extras onto system leaves
                // only (not AG assume leaves).
                val globalRefs = n.globalVarRefs()
                val inits = n.initExprs()
                val typeModels = n.typeModels()
                val funModels = n.funModels()
                val dynProcModel = typeModels.firstOrNull { it.name() == DYN_PROC_MODEL }
                val domainTypeModels = typeModels.filter { it.name() != DYN_PROC_MODEL }
                val body = n.systemBody()
                val bodyLeaves = when (body) {
                    is AgSpecExprNode -> flattenSpecLeaves(body.systemExpr(), intro)
                    else -> flattenSpecLeaves(body, intro)
                }
                bodyLeaves.forEach { child ->
                    out += child.copy(
                        pendingGlobalRefs = child.pendingGlobalRefs + globalRefs,
                        initExprs = child.initExprs + inits,
                        typeModels = child.typeModels + domainTypeModels,
                        funModels = child.funModels + funModels,
                        dynProcModel = child.dynProcModel ?: dynProcModel,
                    )
                }
            }
            is ParamProcExprNode -> {
                val paramName = n.paramName()
                val globalRefs = n.globalVarRefs()
                val inits = n.initExprs()
                val typeModels = n.typeModels()
                val funModels = n.funModels()
                // DynProc is a reserved create-index slot set, not a domain type.
                val dynProcModel = typeModels.firstOrNull { it.name() == DYN_PROC_MODEL }
                val domainTypeModels = typeModels.filter { it.name() != DYN_PROC_MODEL }
                fun withGlobals(child: SpecLeaf): SpecLeaf {
                    // Keep all qualified refs until alias expansion (ShardLogic → ClusterInfo);
                    // materialize matching Leaf.var in expandLeavesToPclasses.
                    val hasPending = globalRefs.isNotEmpty()
                    return if (!hasPending && inits.isEmpty() && domainTypeModels.isEmpty() &&
                        funModels.isEmpty() && dynProcModel == null
                    ) {
                        child
                    } else {
                        child.copy(
                            pendingGlobalRefs = child.pendingGlobalRefs + globalRefs,
                            initExprs = child.initExprs + inits,
                            typeModels = child.typeModels + domainTypeModels,
                            funModels = child.funModels + funModels,
                            dynProcModel = child.dynProcModel ?: dynProcModel,
                        )
                    }
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
                        // Named body (e.g. VerifNode[n : NodeSet]): one shared with-scope so
                        // expanded peers reuse the same TLA binder (not n vs n_ on sync).
                        val pType = n.paramType()!!
                        val scopeId = withScopeId ?: freshWithScopeId()
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
                                withGlobals(
                                    child.copy(withScopeId = child.withScopeId ?: scopeId),
                                )
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
    val system = when (value) {
        // SpecCompositionBlockNode walk already peels AG to systemExpr only.
        is SpecCompositionBlockNode -> value
        is AgSpecExprNode -> value.systemExpr()
        else -> value
    }
    return flattenSpecLeaves(system, spec.specNodeName())
}

fun assumeLeavesOfSpec(
    spec: SpecNode,
    invariantNames: Set<String> = emptySet(),
    systemDeclNames: Set<String> = emptySet(),
): List<SpecLeaf> {
    resetSpecOccurrenceCounter()
    val value = spec.specNodeValue()
    return when (value) {
        is AgSpecExprNode -> {
            if (isFormulaAssume(value.assumeExpr(), invariantNames, systemDeclNames)) {
                emptyList()
            } else {
                flattenSpecLeaves(value.assumeExpr(), spec.specNodeName())
            }
        }
        else -> emptyList()
    }
}

fun compositionLeavesOfSpec(
    spec: SpecNode,
    invariantNames: Set<String> = emptySet(),
    systemDeclNames: Set<String> = emptySet(),
): List<SpecLeaf> {
    resetSpecOccurrenceCounter()
    val value = spec.specNodeValue()
    val assembly = spec.specNodeName()
    return when (value) {
        is AgSpecExprNode -> {
            val assume = value.assumeExpr()
            when {
                assume == null -> flattenSpecLeaves(value.systemExpr(), assembly)
                isFormulaAssume(assume, invariantNames, systemDeclNames) ->
                    flattenSpecLeaves(value.systemExpr(), assembly)
                else ->
                    flattenSpecLeaves(assume, assembly) +
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
            pendingGlobalRefs = child.pendingGlobalRefs + outer.pendingGlobalRefs,
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

    fun declaredStateVars(leafName: String): Map<String, VarNode> {
        val pc = pclasses[leafName] ?: leafSpecs[leafName]?.asProcClass() ?: return emptyMap()
        return pc.localDecls().filterIsInstance<VarNode>().associateBy { it.name }
    }

    fun materializeGlobals(leaf: SpecLeaf): SpecLeaf {
        val matched = leaf.pendingGlobalRefs.filter { it.leafName == leaf.name }.map { it.varName }.toSet()
        val withPending = leaf.copy(
            globalVars = leaf.globalVars + matched,
            pendingGlobalRefs = emptyList(),
        )
        val declaredVars = declaredStateVars(withPending.name)
        // Listed `global Leaf.var` + proc `const` → freeze. Unindexed leaves: all proc
        // `const`s are already scalars, so freeze them the same way (Init `\in`, `Leaf.var` in init:).
        val listedGlobals = if (declaredVars.isEmpty()) {
            emptySet()
        } else {
            withPending.globalVars.intersect(declaredVars.keys)
        }
        val listedFrozen = listedGlobals.filter { name -> declaredVars[name]?.isConst == true }.toSet()
        // Composition-block `init:` is pushed onto all leaves; freeze unindexed proc
        // consts so Init uses `\in` and `Leaf.var` type-checks / emits.
        val unindexedFrozen = if (
            !withPending.isParameterized &&
            withPending.initExprs.isNotEmpty() &&
            declaredVars.isNotEmpty()
        ) {
            declaredVars.filter { it.value.isConst }.keys
        } else {
            emptySet()
        }
        return withPending.copy(
            globalVars = listedGlobals,
            globalConstVars = listedFrozen + unindexedFrozen,
        )
    }

    fun expand(leaf: SpecLeaf) {
        when {
            leaf.name in leafSpecs || leaf.name in pclasses -> out += materializeGlobals(leaf)
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
            else -> out += materializeGlobals(leaf)
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
