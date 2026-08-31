package julay.compiler.pass.fuse

import julay.compiler.LibraryLoc
import julay.compiler.ast.BinaryOpExprNode
import julay.compiler.ast.ExprNode
import julay.compiler.ast.FunCallExprNode
import julay.compiler.ast.IfElseExprNode
import julay.compiler.ast.LetExprNode
import julay.compiler.ast.LiteralValueExprNode
import julay.compiler.ast.ParenExprNode
import julay.compiler.ast.SymbolValueExprNode
import julay.compiler.ast.UnaryOpExprNode
import julay.compiler.ast.WhenArm
import julay.compiler.ast.WhenExprNode
import julay.compiler.ast.substituteExpr
import julay.compiler.decl.ActionDecl
import julay.compiler.decl.ProcClassDecl
import julay.compiler.decl.TransitUpdate
import julay.compiler.ast.exprReferencesSymbol
import julay.compiler.pass.LeafActionId
import julay.compiler.pass.PROC_FUN_RET_VAL
import julay.compiler.pass.TypePassType
import julay.compiler.pass.procFunCallCtor
import julay.compiler.pass.procFunRetAction
import julay.program.Variable
import julay.program.type.Type
import julay.program.type.boolType
import julay.program.type.intType
import julay.program.type.realType
import julay.program.type.stringType
import julay.program.type.toCodegenTypeVal
import julay.program.type.toKotlinIdent
import julay.program.type.toKotlinTypeString

/** Host state: empty = idle; otherwise `/`-separated active fused callee path (e.g. `B` or `B/C`). */
const val FUSE_PHASE_VAR = "__julayFuse"

/**
 * Destination for the fused callee’s return value:
 * - [FUSE_DEST_HOST_RET] → set host `_procFunReturn`
 * - otherwise a host state var name to assign
 */
const val FUSE_DEST_VAR = "__julayFuseDest"
const val FUSE_DEST_HOST_RET = "__hostRet__"

data class FusePlan(
    val hostName: String,
    val calleeSlices: Map<String, PrefixedCallee>,
    val dispatchActionNames: Set<String>,
)

data class PrefixedCallee(
    val originalName: String,
    val prefix: String,
    val stateVars: List<Variable>,
    val transitions: List<ActionDecl>,
    val initAssigns: List<TransitUpdate.Assign>,
    val ctorArgNames: List<String>,
)

data class FuseSiteInfo(
    val calleeInits: Map<String, List<TransitUpdate.Assign>>,
    val assignDests: Set<String>,
)

data class ProcFunFuseResult(
    val decls: List<ProcClassDecl>,
    val siteInfoByHost: Map<String, FuseSiteInfo>,
)

fun fuseProcFunDecls(
    decls: List<ProcClassDecl>,
    channelKeys: Map<LeafActionId, String>,
): ProcFunFuseResult {
    val byName = decls.associateBy { it.name }.toMutableMap()
    val callGraph = decls.associate { d ->
        d.name to findDirectCalleeNames(d).filter { it in byName }.toSet()
    }
    val order = topoCalleesFirst(decls.map { it.name }, callGraph)
    val siteInfoByHost = mutableMapOf<String, FuseSiteInfo>()
    for (name in order) {
        val host = byName.getValue(name)
        val plan = buildFusePlan(host, byName, channelKeys) ?: continue
        byName[name] = applyFusePlan(host, plan)
        val assignDests = linkedSetOf<String>()
        for (tr in host.transitions) {
            for (u in tr.transits.filterIsInstance<TransitUpdate.Assign>()) {
                if (u.key == PROC_FUN_RET_VAL) continue
                if (collectProcFunCalls(u.expr).any { it in plan.calleeSlices }) {
                    assignDests.add(u.key)
                }
            }
        }
        val mergedInits = mutableMapOf<String, List<TransitUpdate.Assign>>()
        for ((calleeName, slice) in plan.calleeSlices) {
            mergedInits[calleeName] = slice.initAssigns
            val nested = siteInfoByHost[calleeName] ?: continue
            val nestPrefix = slice.prefix
            for ((deepName, assigns) in nested.calleeInits) {
                mergedInits[deepName] = assigns.map { a ->
                    TransitUpdate.Assign(nestPrefix + a.key, a.expr)
                }
            }
        }
        siteInfoByHost[name] = FuseSiteInfo(
            calleeInits = mergedInits,
            assignDests = assignDests,
        )
    }
    return ProcFunFuseResult(
        decls = decls.map { byName.getValue(it.name) },
        siteInfoByHost = siteInfoByHost,
    )
}

fun buildFusePlan(
    host: ProcClassDecl,
    calleesByName: Map<String, ProcClassDecl>,
    channelKeys: Map<LeafActionId, String>,
): FusePlan? {
    val calleeNames = linkedSetOf<String>()
    val dispatchActions = linkedSetOf<String>()
    for (tr in host.transitions) {
        val fromReturn = tr.returnExpr?.let { collectProcFunCalls(it) }.orEmpty()
        val fromTransit = tr.transits.filterIsInstance<TransitUpdate.Assign>()
            .flatMap { collectProcFunCalls(it.expr) }
            .toSet()
        val known = (fromReturn + fromTransit).filter { it in calleesByName }.toSet()
        if (known.isEmpty()) continue
        calleeNames.addAll(known)
        dispatchActions.add(tr.action.name)
    }
    if (calleeNames.isEmpty()) return null
    val slices = calleeNames.associateWith { name ->
        prefixCallee(calleesByName.getValue(name), prefix = "${name}__", channelKeys)
    }
    return FusePlan(host.name, slices, dispatchActions)
}

fun applyFusePlan(host: ProcClassDecl, plan: FusePlan): ProcClassDecl {
    val alreadyFuse = host.isFuseHost()
    val fuseVar = Variable(FUSE_PHASE_VAR, stringType)
    val fuseDest = Variable(FUSE_DEST_VAR, stringType)
    val idleGuard = eqString(FUSE_PHASE_VAR, "")
    val extraState = buildList {
        if (!alreadyFuse) {
            add(fuseVar)
            add(fuseDest)
        }
        addAll(plan.calleeSlices.values.flatMap { it.stateVars })
    }
    val newState = host.stateVars + extraState
    val rewrittenHost = host.transitions.map { tr ->
        when {
            tr.action.name == procFunRetAction(host.name) -> tr
            else -> tr.copy(guards = listOf(idleGuard) + tr.guards)
        }
    }
    // Callee slices already carry exact phase-path guards from [prefixCallee].
    val calleeTransitions = plan.calleeSlices.values.flatMap { it.transitions }
    val sliceInits = plan.calleeSlices.values.flatMap { slice ->
        // Only hoist inits that do not read call args (e.g. step := "call").
        // Arg-dependent consts (rvTerm from req) stay dispatch-time only.
        // initAssigns are already renamed, so args appear as "${prefix}${arg}".
        slice.initAssigns.filter { assign ->
            slice.ctorArgNames.none { arg ->
                exprReferencesSymbol(assign.expr, arg) ||
                    exprReferencesSymbol(assign.expr, slice.prefix + arg)
            }
        }
    }
    val newCtors = host.constructors.map { ctor ->
        val initFuse = if (alreadyFuse) {
            emptyList()
        } else {
            listOf(
                TransitUpdate.Assign(
                    FUSE_PHASE_VAR,
                    LiteralValueExprNode("", stringType, ctor.loc),
                ),
                TransitUpdate.Assign(
                    FUSE_DEST_VAR,
                    LiteralValueExprNode("", stringType, ctor.loc),
                ),
            )
        }
        // Safe field inits for inlined callees so idle actions()/guards can read them.
        ctor.copy(transits = initFuse + sliceInits + ctor.transits)
    }
    return ProcClassDecl(
        host.name,
        newState,
        newCtors,
        rewrittenHost + calleeTransitions,
    )
}

fun ProcClassDecl.isFuseHost(): Boolean =
    stateVars.any { it.name == FUSE_PHASE_VAR }

/** Exact `__julayFuse = "…"` literal required for a fused-in transition, if any. */
fun ActionDecl.requiredFusePhaseLiteral(): String? {
    for (g in guards) {
        if (g !is BinaryOpExprNode || g.op() != "=") continue
        val left = g.lhsOperand()
        val right = g.rhsOperand()
        if (left is SymbolValueExprNode && left.symbol == FUSE_PHASE_VAR &&
            right is LiteralValueExprNode
        ) {
            return right.literalText()
        }
    }
    return null
}

fun emitFusedReturnDispatchKotlin(
    returnExpr: ExprNode,
    symbolTypes: Map<String, Type>,
    argSymbols: Set<String>,
    calleeInits: Map<String, List<TransitUpdate.Assign>>,
    /** Current phase path prefix when dispatching from a nested fused frame (e.g. `"B"`). */
    phasePrefix: String = "",
): String? {
    if (collectProcFunCalls(returnExpr).isEmpty()) return null
    return emitDispatchExpr(
        returnExpr, symbolTypes, argSymbols, calleeInits,
        dest = FUSE_DEST_HOST_RET, phasePrefix = phasePrefix,
    )
}

fun emitFusedAssignDispatchKotlin(
    destVar: String,
    expr: ExprNode,
    symbolTypes: Map<String, Type>,
    argSymbols: Set<String>,
    calleeInits: Map<String, List<TransitUpdate.Assign>>,
    phasePrefix: String = "",
): String? {
    if (expr !is FunCallExprNode) return null
    if (expr.resolvedProcFunOrNull() == null) return null
    if (collectProcFunCalls(expr).isEmpty()) return null
    return emitDispatchExpr(expr, symbolTypes, argSymbols, calleeInits, dest = destVar, phasePrefix)
}

/**
 * Complete a fused callee frame: pop one path segment, deliver value if outermost host ret.
 */
fun emitFusedCalleeReturnKotlin(
    returnExpr: ExprNode,
    symbolTypes: Map<String, Type>,
    argSymbols: Set<String>,
    assignDests: Set<String>,
): String {
    val retTy = returnExpr.getType()
    val retStr = returnExpr.toTransitString(symbolTypes, argSymbols)
    val popPhase = buildString {
        appendLine("run {")
        appendLine("    val __p = $FUSE_PHASE_VAR")
        appendLine("    val __slash = __p.lastIndexOf('/')")
        appendLine("    $FUSE_PHASE_VAR = if (__slash < 0) \"\" else __p.substring(0, __slash)")
        appendLine("}")
        appendLine("$FUSE_DEST_VAR = \"\"")
    }.trimEnd()
    val assignBranches = assignDests.joinToString("\n") { dest ->
        val ident = dest.toKotlinIdent()
        "    \"$dest\" -> {\n${popPhase.prependIndent("        ")}\n        $ident = __procFunRet\n    }"
    }
    return buildString {
        appendLine("val __procFunRet: ${retTy.toKotlinTypeString()} = $retStr")
        appendLine("val __fuseDest = $FUSE_DEST_VAR")
        appendLine("when (__fuseDest) {")
        appendLine("    \"$FUSE_DEST_HOST_RET\" -> {")
        appendLine("        $FUSE_PHASE_VAR = \"\"")
        appendLine("        $FUSE_DEST_VAR = \"\"")
        appendLine("        _procFunReturn = Value(__procFunRet, ${retTy.toCodegenTypeVal()})")
        appendLine("    }")
        if (assignBranches.isNotEmpty()) {
            appendLine(assignBranches)
        }
        appendLine("    else -> throw JulayException(\"procfun-fuse: unknown dest \$__fuseDest\")")
        appendLine("}")
    }.trimEnd()
}

fun collectProcFunCalls(expr: ExprNode): Set<String> {
    val out = linkedSetOf<String>()
    fun walk(e: ExprNode) {
        when (e) {
            is FunCallExprNode -> {
                if (e.resolvedProcFunOrNull() != null) out.add(e.callName())
                e.callArgs().forEach { walk(it) }
            }
            is WhenExprNode -> {
                e.subjectExpr()?.let { walk(it) }
                e.arms().forEach { arm ->
                    when (arm) {
                        is WhenArm.Subject -> walk(arm.expr)
                        is WhenArm.Guard -> {
                            walk(arm.cond)
                            walk(arm.expr)
                        }
                        is WhenArm.Else -> walk(arm.expr)
                    }
                }
            }
            is IfElseExprNode -> {
                walk(e.condExpr())
                walk(e.thenExpr())
                walk(e.elseExpr())
            }
            is BinaryOpExprNode -> {
                walk(e.lhsOperand())
                walk(e.rhsOperand())
            }
            is UnaryOpExprNode -> walk(e.operand())
            is ParenExprNode -> walk(e.innerExpr())
            is LetExprNode -> {
                walk(e.letInitExpr())
                walk(e.bodyExpr())
            }
            else -> {}
        }
    }
    walk(expr)
    return out
}

// --- emit helpers ---

private fun emitDispatchExpr(
    expr: ExprNode,
    symbolTypes: Map<String, Type>,
    argSymbols: Set<String>,
    calleeInits: Map<String, List<TransitUpdate.Assign>>,
    dest: String,
    phasePrefix: String,
): String {
    return when (expr) {
        is FunCallExprNode -> {
            val pf = expr.resolvedProcFunOrNull()
                ?: return "throw RuntimeException(\"procfun-fuse: expected procfun call\")"
            val name = pf.procFunName()
            val phasePath = if (phasePrefix.isEmpty()) name else "$phasePrefix/$name"
            val args = try {
                pf.procFunArgs().actionArgs()
            } catch (_: RuntimeException) {
                emptyList()
            }
            val argAssigns = args.zip(expr.callArgs()).joinToString("\n") { (v, argExpr) ->
                val rhs = argExpr.toTransitString(symbolTypes, argSymbols)
                val storagePrefix = if (phasePrefix.isEmpty()) {
                    "${name}__"
                } else {
                    "${phasePrefix.replace("/", "__")}__${name}__"
                }
                "${storagePrefix}${v.name.toKotlinIdent()} = $rhs"
            }
            val initLines = calleeInits[name].orEmpty().joinToString("\n") { assign ->
                val rhs = assign.expr.toTransitString(symbolTypes, argSymbols)
                "${assign.key.toKotlinIdent()} = $rhs"
            }
            buildString {
                appendLine("$FUSE_PHASE_VAR = \"$phasePath\"")
                // Only outermost dispatch sets host-ret dest; nested keeps prior dest.
                if (phasePrefix.isEmpty()) {
                    appendLine("$FUSE_DEST_VAR = \"$dest\"")
                }
                if (argAssigns.isNotEmpty()) appendLine(argAssigns)
                if (initLines.isNotEmpty()) appendLine(initLines)
            }.trimEnd()
        }
        is WhenExprNode -> emitWhenDispatch(expr, symbolTypes, argSymbols, calleeInits, dest, phasePrefix)
        is IfElseExprNode -> {
            val c = expr.condExpr().toTransitString(symbolTypes, argSymbols)
            val t = emitDispatchExpr(expr.thenExpr(), symbolTypes, argSymbols, calleeInits, dest, phasePrefix)
            val el = emitDispatchExpr(expr.elseExpr(), symbolTypes, argSymbols, calleeInits, dest, phasePrefix)
            "if ($c) {\n${t.prependIndent()}\n} else {\n${el.prependIndent()}\n}"
        }
        else -> {
            val retTy = expr.getType()
            val retStr = expr.toTransitString(symbolTypes, argSymbols)
            "val __procFunRet: ${retTy.toKotlinTypeString()} = $retStr\n" +
                "_procFunReturn = Value(__procFunRet, ${retTy.toCodegenTypeVal()})"
        }
    }
}

private fun emitWhenDispatch(
    whenExpr: WhenExprNode,
    symbolTypes: Map<String, Type>,
    argSymbols: Set<String>,
    calleeInits: Map<String, List<TransitUpdate.Assign>>,
    dest: String,
    phasePrefix: String,
): String {
    val elseArm = whenExpr.arms().last() as WhenArm.Else
    var result = emitDispatchExpr(elseArm.expr, symbolTypes, argSymbols, calleeInits, dest, phasePrefix)
    for (arm in whenExpr.arms().dropLast(1).reversed()) {
        val (condStr, branchStr) = when (arm) {
            is WhenArm.Subject -> {
                val subject = whenExpr.subjectExpr()
                    ?: throw RuntimeException("when subject arm without subject")
                val cond = when (val p = arm.pattern) {
                    is julay.compiler.ast.WhenPattern.Primitive -> {
                        val lit = when (val lit = p.literal) {
                            is julay.compiler.ast.WhenLiteral.StringLit ->
                                LiteralValueExprNode(lit.value, stringType, whenExpr.programLocation())
                            is julay.compiler.ast.WhenLiteral.IntLit ->
                                LiteralValueExprNode(lit.value, intType, whenExpr.programLocation())
                            is julay.compiler.ast.WhenLiteral.BoolLit ->
                                LiteralValueExprNode(lit.value, boolType, whenExpr.programLocation())
                            is julay.compiler.ast.WhenLiteral.RealLit ->
                                LiteralValueExprNode(lit.value, realType, whenExpr.programLocation())
                        }
                        BinaryOpExprNode("=", subject, lit, whenExpr.programLocation()).also {
                            it.setInferredType(TypePassType.Inferred(boolType))
                        }.toTransitString(symbolTypes, argSymbols)
                    }
                    else -> "true"
                }
                cond to emitDispatchExpr(arm.expr, symbolTypes, argSymbols, calleeInits, dest, phasePrefix)
            }
            is WhenArm.Guard ->
                arm.cond.toTransitString(symbolTypes, argSymbols) to
                    emitDispatchExpr(arm.expr, symbolTypes, argSymbols, calleeInits, dest, phasePrefix)
            is WhenArm.Else -> error("else not in non-final position")
        }
        result = "if ($condStr) {\n${branchStr.prependIndent()}\n} else {\n${result.prependIndent()}\n}"
    }
    return result
}

// --- planning helpers ---

private fun findDirectCalleeNames(decl: ProcClassDecl): Set<String> {
    val out = linkedSetOf<String>()
    for (tr in decl.transitions) {
        tr.returnExpr?.let { out.addAll(collectProcFunCalls(it)) }
        for (u in tr.transits) {
            if (u is TransitUpdate.Assign) out.addAll(collectProcFunCalls(u.expr))
        }
    }
    return out
}

private fun prefixCallee(
    callee: ProcClassDecl,
    prefix: String,
    channelKeys: Map<LeafActionId, String>,
): PrefixedCallee {
    val skip = setOf(procFunCallCtor(callee.name), procFunRetAction(callee.name))
    // Share host fuse phase/dest vars; do not duplicate them under the prefix.
    val userVars = callee.stateVars.filter {
        it.name != PROC_FUN_RET_VAL &&
            it.name != FUSE_PHASE_VAR &&
            it.name != FUSE_DEST_VAR
    }
    val renamedVars = userVars.map { Variable(prefix + it.name, it.type) }
    val nameMap = userVars.associate { it.name to (prefix + it.name) }
    fun renameExpr(e: ExprNode): ExprNode {
        val rootTy = try {
            e.getType()
        } catch (_: RuntimeException) {
            null
        }
        var result = e
        for ((old, new) in nameMap) {
            val ty = userVars.firstOrNull { it.name == old }?.type
            val repl = SymbolValueExprNode(new, e.programLocation()).also { node ->
                if (ty != null) node.setInferredType(TypePassType.Inferred(ty))
            }
            result = substituteExpr(result, old, repl)
        }
        if (rootTy != null) {
            try {
                result.getType()
            } catch (_: RuntimeException) {
                result.setInferredType(TypePassType.Inferred(rootTy))
            }
        }
        return result
    }
    fun rebasePhaseGuard(e: ExprNode): ExprNode {
        if (e !is BinaryOpExprNode || e.op() != "=") return e
        val left = e.lhsOperand()
        val right = e.rhsOperand()
        if (left !is SymbolValueExprNode || left.symbol != FUSE_PHASE_VAR) return e
        if (right !is LiteralValueExprNode) return e
        val old = right.literalText()
        val newPath = if (old.isEmpty()) callee.name else "${callee.name}/$old"
        val newRight = LiteralValueExprNode(newPath, stringType, right.programLocation())
        return BinaryOpExprNode("=", left, newRight, e.programLocation()).also {
            it.setInferredType(TypePassType.Inferred(boolType))
        }
    }
    fun resolveChannelKey(actionName: String): String {
        val match = channelKeys.entries.firstOrNull { (id, _) ->
            id.pclassKey == callee.name &&
                id.actionName == actionName &&
                !id.isConstructor
        }
        return match?.value ?: actionName
    }
    fun renameAction(tr: ActionDecl): ActionDecl {
        val newGuards = tr.guards.map { g -> rebasePhaseGuard(renameExpr(g)) }
        val newTransits = tr.transits.mapNotNull { u ->
            when (u) {
                is TransitUpdate.Assign -> {
                    if (u.key == PROC_FUN_RET_VAL) return@mapNotNull null
                    if (u.key == FUSE_PHASE_VAR || u.key == FUSE_DEST_VAR) return@mapNotNull null
                    TransitUpdate.Assign(
                        nameMap[u.key] ?: u.key,
                        renameExpr(u.expr),
                    )
                }
                is TransitUpdate.Let -> TransitUpdate.Let(u.name, u.type, renameExpr(u.init))
                is TransitUpdate.IndexPut -> TransitUpdate.IndexPut(
                    nameMap[u.collectionVar] ?: u.collectionVar,
                    renameExpr(u.index),
                    renameExpr(u.value),
                )
            }
        }
        val newReturn = tr.returnExpr?.let { renameExpr(it) }
        // Phase guard first so FastOnly grounding / actions() can short-circuit before
        // reading uninitialized arg-dependent callee state.
        val phaseGuard = eqString(FUSE_PHASE_VAR, callee.name)
        val guardsWithPhase = if (tr.guards.any { mentionsFusePhase(it) }) {
            // Already has a (possibly rebased) phase literal — keep it first.
            val (phaseGuards, rest) = newGuards.partition { mentionsFusePhase(it) }
            phaseGuards + rest
        } else {
            listOf(phaseGuard) + newGuards
        }
        val key = resolveChannelKey(tr.action.name)
        val tagged = tr.action.copy(channelKey = key)
        return ActionDecl(
            tagged,
            guardsWithPhase,
            newTransits,
            tr.modifier,
            tr.loc,
            tr.befores,
            tr.afters,
            tr.errors,
            returnExpr = newReturn,
            fuseOriginPclass = tr.fuseOriginPclass ?: callee.name,
        )
    }
    val ctor = callee.constructors.singleOrNull()
    val ctorArgNames = ctor?.action?.args?.map { it.name } ?: emptyList()
    val argNameSet = ctorArgNames.toSet()
    val initAssigns = ctor?.transits.orEmpty().mapNotNull { u ->
        if (u !is TransitUpdate.Assign) return@mapNotNull null
        if (u.key in argNameSet) return@mapNotNull null
        if (u.key == PROC_FUN_RET_VAL) return@mapNotNull null
        if (u.key == FUSE_PHASE_VAR || u.key == FUSE_DEST_VAR) return@mapNotNull null
        TransitUpdate.Assign(prefix + u.key, renameExpr(u.expr))
    }
    val transitions = callee.transitions
        .filter { it.action.name !in skip }
        .map { renameAction(it) }
    return PrefixedCallee(callee.name, prefix, renamedVars, transitions, initAssigns, ctorArgNames)
}

private fun mentionsFusePhase(e: ExprNode): Boolean {
    return when (e) {
        is SymbolValueExprNode -> e.symbol == FUSE_PHASE_VAR
        is BinaryOpExprNode -> mentionsFusePhase(e.lhsOperand()) || mentionsFusePhase(e.rhsOperand())
        is UnaryOpExprNode -> mentionsFusePhase(e.operand())
        is ParenExprNode -> mentionsFusePhase(e.innerExpr())
        else -> false
    }
}

private fun eqString(varName: String, lit: String): ExprNode {
    val loc = LibraryLoc("procfun-fuse")
    val left = SymbolValueExprNode(varName, loc).also {
        it.setInferredType(TypePassType.Inferred(stringType))
    }
    val right = LiteralValueExprNode(lit, stringType, loc)
    return BinaryOpExprNode("=", left, right, loc).also {
        it.setInferredType(TypePassType.Inferred(boolType))
    }
}

private fun topoCalleesFirst(names: List<String>, edges: Map<String, Set<String>>): List<String> {
    val result = mutableListOf<String>()
    val visiting = mutableSetOf<String>()
    val done = mutableSetOf<String>()
    fun dfs(n: String) {
        if (n in done) return
        if (n in visiting) return
        visiting.add(n)
        for (c in edges[n].orEmpty()) dfs(c)
        visiting.remove(n)
        done.add(n)
        result.add(n)
    }
    names.forEach { dfs(it) }
    return result
}
