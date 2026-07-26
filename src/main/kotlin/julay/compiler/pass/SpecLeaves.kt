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

/**
 * A whole-RHS procfun call in a host action transit (`x := countUp(2)`).
 * [occurrence] is filled after [assignTlaLeafNames].
 */
data class ProcFunCallSite(
    val hostName: String,
    val hostActionName: String,
    val isHostConstructor: Boolean,
    val procFunName: String,
    val call: FunCallExprNode,
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
    val call: FunCallExprNode,
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
            val pfName = hit.call.resolvedProcFunOrNull()?.procFunName() ?: return@forEach
            out += ProcFunCallSiteDraft(
                host = host,
                hostActionName = hit.actionName,
                isHostConstructor = hit.isCtor,
                procFunName = pfName,
                call = hit.call,
                assignVars = hit.assignVars,
                occurrenceId = freshSpecOccurrenceId(pfName),
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
            call = draft.call,
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
            call = draft.call,
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
        wholeRhsProcFunCall(decl)?.let { (call, vars) ->
            out += WholeRhsHit(decl.action.name, true, call, vars)
        }
    }
    pc.localDecls().filterIsInstance<TransitionNode>().forEach { tr ->
        val decl = tr.transitions().single()
        wholeRhsProcFunCall(decl)?.let { (call, vars) ->
            out += WholeRhsHit(decl.action.name, false, call, vars)
        }
    }
    return out
}

internal data class WholeRhsHit(
    val actionName: String,
    val isCtor: Boolean,
    val call: FunCallExprNode,
    val assignVars: List<String>,
)

/**
 * All resolved procfun calls appearing anywhere in constructor/transition transit exprs
 * (including nested under `when` / `if` / etc.). Used for orphan and havoc warnings.
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
                is julay.compiler.decl.TransitUpdate.MapPut -> {
                    walkExpr(update.key)
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
): Pair<FunCallExprNode, List<String>>? {
    val hits = decl.transits.mapNotNull { update ->
        when (update) {
            is julay.compiler.decl.TransitUpdate.Assign -> {
                val call = update.expr as? FunCallExprNode ?: return@mapNotNull null
                if (call.resolvedProcFunOrNull() == null) return@mapNotNull null
                julay.program.type.transitRootVar(update.key) to call
            }
            else -> null
        }
    }
    if (hits.isEmpty()) return null
    val calls = hits.map { it.second }.distinct()
    // v1: exactly one distinct procfun call in the action
    if (calls.size != 1) return null
    return calls.single() to hits.map { it.first }
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
