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
    /** True when this leaf is a procfun call-site occurrence (not a `||` peer). */
    val isProcFun: Boolean = false,
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
) {
    val isParameterized: Boolean get() = paramName != null && paramType != null
    /** True when this leaf is create-indexed and [varName] is not a `global` model var. */
    fun indexesState(varName: String): Boolean = isParameterized && varName !in globalVars
    fun identityKey(): String {
        val base = if (paramName != null) "$name[$paramName:${paramType}]" else name
        return if (occurrenceId.isNotEmpty()) "$base#$occurrenceId" else base
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
                fun withGlobals(child: SpecLeaf): SpecLeaf =
                    if (globals.isEmpty()) {
                        child
                    } else {
                        child.copy(
                            globalVars = child.globalVars + globals,
                            globalConstVars = child.globalConstVars + constGlobals,
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
 * Expand named `proc` / `spec` / `api` aliases to their nested proc-class leaves (by occurrence).
 * Parameterization on an outer leaf is pushed down onto non-parameterized children.
 *
 * Nested AG specs contribute only their system expression (not assume/guarantee).
 * Api `calls:` are not expanded as SpecLeaves (call-site occurrences come from the call graph).
 */
fun expandLeavesToPclasses(
    leaves: List<SpecLeaf>,
    pclasses: Map<String, ProcClassNode>,
    procAliases: Map<String, ProcNode>,
    specAliases: Map<String, SpecNode> = emptyMap(),
    apiAliases: Map<String, ApiNode> = emptyMap(),
    leafSpecs: Map<String, LeafSpecNode> = emptyMap(),
): List<SpecLeaf> {
    val out = mutableListOf<SpecLeaf>()
    val visiting = mutableSetOf<String>()

    fun pushDown(outer: SpecLeaf, child: SpecLeaf): SpecLeaf {
        var c = child.copy(
            withScopeId = outer.withScopeId ?: child.withScopeId,
            globalVars = child.globalVars + outer.globalVars,
            globalConstVars = child.globalConstVars + outer.globalConstVars,
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
            leaf.name in apiAliases -> {
                val api = apiAliases.getValue(leaf.name)
                flattenSpecLeaves(api.apiProcExpr(), leaf.name).forEach { child ->
                    expand(pushDown(leaf, child.copy(introducingAssembly = leaf.name)))
                }
            }
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
    return assignTlaLeafNames(out)
}

/** Union of `calls:` from every api reachable by expanding [leaves]. */
fun collectApiCallsInComposition(
    leaves: List<SpecLeaf>,
    apiAliases: Map<String, ApiNode>,
    procAliases: Map<String, ProcNode>,
    specAliases: Map<String, SpecNode>,
): Set<String> {
    val calls = linkedSetOf<String>()
    val visiting = mutableSetOf<String>()
    fun walk(leaf: SpecLeaf) {
        when {
            leaf.name in apiAliases -> {
                val api = apiAliases.getValue(leaf.name)
                calls += api.apiCallNames()
                flattenSpecLeaves(api.apiProcExpr(), leaf.name).forEach { walk(it) }
            }
            leaf.name in procAliases -> {
                flattenSpecLeaves(procAliases.getValue(leaf.name).procNodeValue(), leaf.name)
                    .forEach { walk(it) }
            }
            leaf.name in specAliases -> {
                if (!visiting.add(leaf.name)) return
                try {
                    val spec = specAliases.getValue(leaf.name)
                    val value = spec.specNodeValue()
                    val children = when (value) {
                        is AgSpecExprNode -> flattenSpecLeaves(value.systemExpr(), spec.specNodeName())
                        else -> flattenSpecLeaves(value, spec.specNodeName())
                    }
                    children.forEach { walk(it) }
                } finally {
                    visiting.remove(leaf.name)
                }
            }
        }
    }
    leaves.forEach { walk(it) }
    return calls
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

/**
 * A whole-RHS procfun call in a host action transit (`x := countUp(2)`).
 * [occurrence] is filled after [assignTlaLeafNames].
 */
data class ProcFunCallSite(
    val hostName: String,
    val hostActionName: String,
    val isHostConstructor: Boolean,
    val procFunName: String,
    val callArgs: List<ExprNode>,
    val assignVars: List<String>,
    val occurrence: SpecLeaf,
)

/**
 * Raw call site before leaf naming; [occurrenceId] ties to the matching [SpecLeaf].
 */
data class ProcFunCallSiteDraft(
    val host: SpecLeaf,
    val hostActionName: String,
    val isHostConstructor: Boolean,
    val procFunName: String,
    val callArgs: List<ExprNode>,
    val assignVars: List<String>,
    val occurrenceId: String,
)

/**
 * Discover whole-RHS procfun call sites under host leaves (one draft per call).
 */
fun discoverProcFunCallSiteDrafts(
    hostLeaves: List<SpecLeaf>,
    pclasses: Map<String, ProcClassNode>,
): List<ProcFunCallSiteDraft> {
    val out = mutableListOf<ProcFunCallSiteDraft>()
    hostLeaves.forEach { host ->
        val pc = pclasses[host.name] ?: return@forEach
        collectWholeRhsProcFunCalls(pc).forEach { hit ->
            out += ProcFunCallSiteDraft(
                host = host,
                hostActionName = hit.actionName,
                isHostConstructor = hit.isCtor,
                procFunName = hit.procFunName,
                callArgs = hit.callArgs,
                assignVars = hit.assignVars,
                occurrenceId = freshSpecOccurrenceId(hit.procFunName),
            )
        }
    }
    return out
}

/** SpecLeaves for [drafts], inheriting each host's index binders. */
fun procFunLeavesFromDrafts(drafts: List<ProcFunCallSiteDraft>): List<SpecLeaf> =
    drafts.map { draft ->
        SpecLeaf(
            name = draft.procFunName,
            paramName = draft.host.paramName,
            paramType = draft.host.paramType,
            occurrenceId = draft.occurrenceId,
            introducingAssembly = draft.host.tlaName,
            tlaName = draft.procFunName,
            isProcFun = true,
        )
    }

/**
 * Each whole-RHS textual procfun call site under a host leaf becomes its own SpecLeaf occurrence,
 * inheriting the host's index binders.
 */
fun discoverProcFunOccurrenceLeaves(
    hostLeaves: List<SpecLeaf>,
    pclasses: Map<String, ProcClassNode>,
): List<SpecLeaf> =
    procFunLeavesFromDrafts(discoverProcFunCallSiteDrafts(hostLeaves, pclasses))

/**
 * Build named [ProcFunCallSite]s after [assignTlaLeafNames] on host+procfun leaves.
 */
fun resolveProcFunCallSites(
    drafts: List<ProcFunCallSiteDraft>,
    namedLeaves: List<SpecLeaf>,
): List<ProcFunCallSite> {
    val byOccId = namedLeaves.filter { it.isProcFun }.associateBy { it.occurrenceId }
    val hostsByKey = namedLeaves.filter { !it.isProcFun }.associateBy { it.identityKey() }
    return drafts.mapNotNull { draft ->
        val occ = byOccId[draft.occurrenceId] ?: return@mapNotNull null
        val host = hostsByKey[draft.host.identityKey()]
            ?: namedLeaves.firstOrNull { !it.isProcFun && it.name == draft.host.name }
            ?: return@mapNotNull null
        ProcFunCallSite(
            hostName = host.tlaName,
            hostActionName = draft.hostActionName,
            isHostConstructor = draft.isHostConstructor,
            procFunName = draft.procFunName,
            callArgs = draft.callArgs,
            assignVars = draft.assignVars,
            occurrence = occ,
        )
    }
}

/**
 * Resolve havoc call sites (procfun not in composition) — no child occurrence leaf.
 */
fun resolveHavocProcFunCallSites(
    drafts: List<ProcFunCallSiteDraft>,
    namedHostLeaves: List<SpecLeaf>,
): List<ProcFunCallSite> {
    val hostsByKey = namedHostLeaves.filter { !it.isProcFun }.associateBy { it.identityKey() }
    return drafts.mapNotNull { draft ->
        val host = hostsByKey[draft.host.identityKey()]
            ?: namedHostLeaves.firstOrNull { !it.isProcFun && it.name == draft.host.name }
            ?: return@mapNotNull null
        ProcFunCallSite(
            hostName = host.tlaName,
            hostActionName = draft.hostActionName,
            isHostConstructor = draft.isHostConstructor,
            procFunName = draft.procFunName,
            callArgs = draft.callArgs,
            assignVars = draft.assignVars,
            occurrence = SpecLeaf(
                name = draft.procFunName,
                occurrenceId = draft.occurrenceId,
                introducingAssembly = host.tlaName,
                isProcFun = true,
            ),
        )
    }
}

/**
 * Collect actions whose transit has exactly one whole-RHS procfun call (v1 coupling shape).
 * Used for TLA+ spawn-await coupling only — not for orphan / alphabet call detection.
 */
internal fun collectWholeRhsProcFunCalls(pc: ProcClassNode): List<WholeRhsHit> {
    val out = mutableListOf<WholeRhsHit>()
    pc.localDecls().filterIsInstance<ConstructorNode>().forEach { ctor ->
        val decl = ctor.constructors().single()
        wholeRhsProcFunCall(decl)?.let { hit ->
            out += hit.copy(actionName = decl.action.name, isCtor = true)
        }
    }
    pc.localDecls().filterIsInstance<TransitionNode>().forEach { tr ->
        val decl = tr.transitions().single()
        wholeRhsProcFunCall(decl)?.let { hit ->
            out += hit.copy(actionName = decl.action.name, isCtor = false)
        }
    }
    return out
}

internal data class WholeRhsHit(
    val actionName: String,
    val isCtor: Boolean,
    val procFunName: String,
    val callArgs: List<ExprNode>,
    val assignVars: List<String>,
)

/**
 * All resolved bare procfun calls appearing anywhere in constructor/transition transit exprs.
 */
internal fun collectProcFunCallsInProc(pc: ProcClassNode): List<FunCallExprNode> {
    val out = mutableListOf<FunCallExprNode>()
    fun walkExpr(expr: ExprNode) {
        if (expr is FunCallExprNode && expr.resolvedProcFunOrNull() != null) {
            out += expr
        }
        expr.children.filterIsInstance<ExprNode>().forEach { walkExpr(it) }
    }
    fun walkDecl(decl: julay.compiler.decl.ActionDecl) {
        decl.transits.forEach { update ->
            when (update) {
                is julay.compiler.decl.TransitUpdate.Assign -> walkExpr(update.expr)
                is julay.compiler.decl.TransitUpdate.IndexPut -> {
                    walkExpr(update.index)
                    walkExpr(update.value)
                }
                is julay.compiler.decl.TransitUpdate.Let -> walkExpr(update.init)
            }
        }
    }
    pc.localDecls().filterIsInstance<ConstructorNode>().forEach { walkDecl(it.constructors().single()) }
    pc.localDecls().filterIsInstance<TransitionNode>().forEach { walkDecl(it.transitions().single()) }
    return out
}

/** Api-qualified procfun calls (`Api.fn(...)`) in a proc. */
internal fun collectApiQualifiedProcFunCallsInProc(pc: ProcClassNode): List<MethodCallExprNode> {
    val out = mutableListOf<MethodCallExprNode>()
    fun walkExpr(expr: ExprNode) {
        if (expr is MethodCallExprNode && expr.resolvedProcFunOrNull() != null) {
            out += expr
        }
        expr.children.filterIsInstance<ExprNode>().forEach { walkExpr(it) }
    }
    fun walkDecl(decl: julay.compiler.decl.ActionDecl) {
        decl.transits.forEach { update ->
            when (update) {
                is julay.compiler.decl.TransitUpdate.Assign -> walkExpr(update.expr)
                is julay.compiler.decl.TransitUpdate.IndexPut -> {
                    walkExpr(update.index)
                    walkExpr(update.value)
                }
                is julay.compiler.decl.TransitUpdate.Let -> walkExpr(update.init)
            }
        }
    }
    pc.localDecls().filterIsInstance<ConstructorNode>().forEach { walkDecl(it.constructors().single()) }
    pc.localDecls().filterIsInstance<TransitionNode>().forEach { walkDecl(it.transitions().single()) }
    return out
}

private fun wholeRhsProcFunCall(
    decl: julay.compiler.decl.ActionDecl,
): WholeRhsHit? {
    val hits = decl.transits.mapNotNull { update ->
        when (update) {
            is julay.compiler.decl.TransitUpdate.Assign -> {
                when (val expr = update.expr) {
                    is FunCallExprNode -> {
                        val pf = expr.resolvedProcFunOrNull() ?: return@mapNotNull null
                        Triple(
                            pf.procFunName(),
                            expr.callArgs(),
                            julay.program.type.transitRootVar(update.key),
                        )
                    }
                    is MethodCallExprNode -> {
                        val pf = expr.resolvedProcFunOrNull() ?: return@mapNotNull null
                        Triple(
                            pf.procFunName(),
                            expr.args,
                            julay.program.type.transitRootVar(update.key),
                        )
                    }
                    else -> null
                }
            }
            else -> null
        }
    }
    if (hits.isEmpty()) return null
    val names = hits.map { it.first }.distinct()
    if (names.size != 1) return null
    return WholeRhsHit(
        actionName = "",
        isCtor = false,
        procFunName = names.single(),
        callArgs = hits.first().second,
        assignVars = hits.map { it.third },
    )
}

/** Build a synthetic [ProcClassNode] view of a procfun for TLA leaf lookup. */
fun ProcFunNode.asSyntheticProcClass(): ProcClassNode {
    val argNodes = procFunArgs().children.filterIsInstance<ArgNode>()
    val argVars = argNodes.map { arg ->
        val vn = VarNode(arg.argName(), arg.argTypeExpr(), programLocation(), isConst = true)
        try {
            vn.resolveType(arg.type)
        } catch (_: RuntimeException) {}
        vn
    }
    val retVal = VarNode(PROC_FUN_RET_VAL, procFunReturnTypeExpr(), programLocation())
    try {
        retVal.resolveType(returnType)
    } catch (_: RuntimeException) {}
    val callCtor = ConstructorNode(
        procFunCallCtor(name()),
        procFunArgs(),
        emptyList(),
        programLocation(),
    )
    val body = argVars + listOf(retVal) + listOf(callCtor) + localDecls().filter { it !is ConstructorNode }
    return ProcClassNode(name(), body, programLocation())
}
