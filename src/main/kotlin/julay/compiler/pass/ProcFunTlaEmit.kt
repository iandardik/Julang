package julay.compiler.pass

import julay.compiler.ast.*
import julay.compiler.decl.TransitUpdate
import julay.program.type.transitRootVar

internal fun callFlagName(occurrence: SpecLeaf): String = "call_${occurrence.tlaName}"

internal fun blockingVarName(hostTlaName: String): String = "${hostTlaName}_blocking"

internal fun returnToVarName(hostActionName: String): String = "returnTo_$hostActionName"

/** Handshake booleans for procfun spawn-await coupling. */
internal data class ProcFunHandshakeVars(
    val callFlags: List<Pair<SpecLeaf, String>>, // occurrence leaf → call_* name
    val blockingByHost: Map<String, String>, // host tlaName → blocking var
    val returnToByKey: Map<Pair<String, String>, String>, // (hostTla, action) → returnTo_*
) {
    fun allNames(): List<String> =
        callFlags.map { it.second } +
            blockingByHost.values +
            returnToByKey.values
}

internal fun buildProcFunHandshakeVars(callSites: List<ProcFunCallSite>): ProcFunHandshakeVars {
    val callFlags = callSites.map { it.occurrence to callFlagName(it.occurrence) }.distinctBy { it.second }
    val blockingByHost = callSites.associate { it.hostName to blockingVarName(it.hostName) }
    val returnToByKey = callSites.associate {
        (it.hostName to it.hostActionName) to returnToVarName(it.hostActionName)
    }
    return ProcFunHandshakeVars(callFlags, blockingByHost, returnToByKey)
}

/**
 * Emit `<action>_call` and `<action>_ret` for a whole-RHS procfun call site.
 */
internal fun emitProcFunCallAndRet(
    site: ProcFunCallSite,
    hostLeaf: SpecLeaf,
    hostOffer: TlaActionOffer,
    procFun: ProcFunNode,
    allVars: List<String>,
    stateVarsByLeaf: Map<String, Set<String>>,
    stateVarNames: Map<Pair<String, String>, String>,
    handshake: ProcFunHandshakeVars,
): List<TlaAction> {
    val child = site.occurrence
    val callFlag = handshake.callFlags.first { it.first.occurrenceId == child.occurrenceId }.second
    val blocking = handshake.blockingByHost.getValue(site.hostName)
    val returnTo = handshake.returnToByKey.getValue(site.hostName to site.hostActionName)

    val hostBare = stateVarsByLeaf[hostLeaf.tlaName].orEmpty()
    val childBare = stateVarsByLeaf[child.tlaName].orEmpty()
    val reserved = mutableSetOf<String>()
    reserved += hostBare
    reserved += childBare

    val hostBinder = if (hostLeaf.isParameterized) {
        indexBinderName(hostLeaf, reserved).also { reserved += it }
    } else null
    // Occurrence inherits host index; same binder name when parameterized.
    val childBinder = if (child.isParameterized) hostBinder else null

    fun idx(v: String, binder: String?): String = if (binder != null) "$v[$binder]" else v
    fun assignTrue(v: String, binder: String?): String =
        if (binder != null) "$v' = [$v EXCEPT ![$binder] = TRUE]" else "$v' = TRUE"
    fun assignFalse(v: String, binder: String?): String =
        if (binder != null) "$v' = [$v EXCEPT ![$binder] = FALSE]" else "$v' = FALSE"
    fun assignVal(v: String, binder: String?, rhs: String): String =
        if (binder != null) "$v' = [$v EXCEPT ![$binder] = $rhs]" else "$v' = $rhs"

    val hostCtx = mapOf(hostLeaf.name to hostLeaf, hostLeaf.tlaName to hostLeaf)
    val childCtx = mapOf(child.name to child, child.tlaName to child)
    val hostArgNames = hostOffer.decl.action.args.map { it.name }.toSet()

    // --- call action ---
    val callParts = mutableListOf<String>()
    val callChanged = mutableSetOf<String>()

    val hostConstructed = stateTlaName(hostLeaf.tlaName, "constructed", stateVarNames)
    if (site.isHostConstructor) {
        callParts += "/\\ ~${idx(hostConstructed, hostBinder)}"
    } else {
        callParts += "/\\ ${idx(hostConstructed, hostBinder)}"
    }
    callParts += "/\\ ~${idx(blocking, hostBinder)}"

    hostOffer.decl.guards.forEach { g ->
        callParts += "/\\ ${exprToTla(g, hostCtx, hostArgNames, hostBinder, hostBare, stateVarNames = stateVarNames)}"
    }

    // Call-arg → child param binds + inline inits + constructed (folded former F_call ctor).
    val argNodes = procFun.procFunArgs().children.filterIsInstance<ArgNode>()
    val callArgs = site.callArgs
    require(argNodes.size == callArgs.size) {
        "procfun ${site.procFunName} arity mismatch at call site"
    }
    val argTla = linkedMapOf<String, String>()
    argNodes.zip(callArgs).forEach { (arg, expr) ->
        val rhs = exprToTla(expr, hostCtx, hostArgNames, hostBinder, hostBare, stateVarNames = stateVarNames)
        argTla[arg.argName()] = rhs
        val v = stateTlaName(child.tlaName, arg.argName(), stateVarNames)
        callParts += "/\\ ${assignVal(v, childBinder, rhs)}"
        callChanged += v
    }
    procFun.localDecls().filterIsInstance<VarNode>().forEach { vn ->
        val init = vn.initExpr ?: return@forEach
        val rhs = exprToTla(
            init, childCtx, argTla.keys, childBinder, childBare,
            stateVarNames = stateVarNames,
            symbolOverrides = argTla,
        )
        val v = stateTlaName(child.tlaName, vn.name, stateVarNames)
        callParts += "/\\ ${assignVal(v, childBinder, rhs)}"
        callChanged += v
    }
    val childConstructed = stateTlaName(child.tlaName, "constructed", stateVarNames)
    callParts += "/\\ ${assignTrue(childConstructed, childBinder)}"
    callChanged += childConstructed

    callParts += "/\\ ${assignTrue(callFlag, childBinder)}"
    callChanged += callFlag
    callParts += "/\\ ${assignTrue(blocking, hostBinder)}   \\* so ${site.hostName} does not execute other actions in the meantime"
    callChanged += blocking

    val callUnchanged = allVars.filter { it !in callChanged }
    if (callUnchanged.isNotEmpty()) {
        callParts += "/\\ UNCHANGED <<${callUnchanged.joinToString(", ")}>>"
    }

    val callName = "${site.hostActionName}_call"
    val callComment = "${site.hostActionName} calls the procfun ${site.procFunName} before executing"
    val callParams = mutableListOf<Pair<String, String>>()
    if (hostBinder != null) {
        val domain = typeDomainConstant(hostLeaf.paramType!!) ?: hostLeaf.paramType.toString()
        callParams += hostBinder to domain
    }
    fun refsArg(argName: String): Boolean =
        hostOffer.decl.guards.any { exprReferencesSymbol(it, argName) } ||
            callArgs.any { exprReferencesSymbol(it, argName) }
    hostOffer.decl.action.args.filter { refsArg(it.name) }.forEach { arg ->
        callParams += arg.name to typeToTlaDomain(arg.type)
    }
    val callSig = if (callParams.isEmpty()) callName
    else "$callName(${callParams.joinToString(", ") { it.first }})"
    val callAction = TlaAction(
        callName,
        "$callSig ==\n  ${callParts.joinToString("\n  ")}",
        callParams.distinctBy { it.first },
        comment = callComment,
    )

    // --- ret action ---
    val retParts = mutableListOf<String>()
    val retChanged = mutableSetOf<String>()
    retParts += "/\\ ${idx(returnTo, hostBinder)}"
    retParts += "/\\ ${idx(blocking, hostBinder)}"

    val retValTla = stateTlaName(child.tlaName, PROC_FUN_RET_VAL, stateVarNames)
    val retRhs = idx(retValTla, childBinder)
    site.assignVars.forEach { varName ->
        val v = stateTlaName(hostLeaf.tlaName, varName, stateVarNames)
        retParts += "/\\ ${assignVal(v, hostBinder, retRhs)}"
        retChanged += v
    }

    var letBindings = emptyMap<String, ExprNode>()
    fun substLets(expr: ExprNode): ExprNode {
        var result = expr
        for ((name, init) in letBindings) {
            result = substituteExpr(result, name, init)
        }
        return result
    }
    hostOffer.decl.transits.forEach { update ->
        when (update) {
            is TransitUpdate.Let -> {
                letBindings = letBindings + (update.name to substLets(update.init))
            }
            is TransitUpdate.Assign -> {
                val root = transitRootVar(update.key)
                if (root in site.assignVars) return@forEach
                val expr = substLets(update.expr)
                if (expr is FunCallExprNode && expr.resolvedProcFunOrNull() != null) return@forEach
                val v = stateTlaName(hostLeaf.tlaName, root, stateVarNames)
                val rhs = exprToTla(expr, hostCtx, hostArgNames, hostBinder, hostBare, stateVarNames = stateVarNames)
                retParts += "/\\ ${assignVal(v, hostBinder, rhs)}"
                retChanged += v
            }
            is TransitUpdate.IndexPut -> {
                val valueExpr = substLets(update.value)
                if (valueExpr is FunCallExprNode && valueExpr.resolvedProcFunOrNull() != null) return@forEach
                val v = stateTlaName(hostLeaf.tlaName, update.collectionVar, stateVarNames)
                val k = exprToTla(substLets(update.index), hostCtx, hostArgNames, hostBinder, hostBare, stateVarNames = stateVarNames)
                val vv = exprToTla(valueExpr, hostCtx, hostArgNames, hostBinder, hostBare, stateVarNames = stateVarNames)
                if (hostBinder != null) {
                    retParts += "/\\ $v' = [$v EXCEPT ![$hostBinder] = [@ EXCEPT ![$k] = $vv]]"
                } else {
                    retParts += "/\\ $v' = [$v EXCEPT ![$k] = $vv]"
                }
                retChanged += v
            }
        }
    }

    if (site.isHostConstructor) {
        retParts += "/\\ ${assignTrue(hostConstructed, hostBinder)}"
        retChanged += hostConstructed
    }
    retParts += "/\\ ${assignFalse(blocking, hostBinder)}"
    retChanged += blocking
    retParts += "/\\ ${assignFalse(returnTo, hostBinder)}"
    retChanged += returnTo

    val retUnchanged = allVars.filter { it !in retChanged }
    if (retUnchanged.isNotEmpty()) {
        retParts += "/\\ UNCHANGED <<${retUnchanged.joinToString(", ")}>>"
    }

    val retName = "${site.hostActionName}_ret"
    val retComment = "The guards for ${site.hostActionName} appear in $callName"
    val retParams = if (hostBinder != null) {
        val domain = typeDomainConstant(hostLeaf.paramType!!) ?: hostLeaf.paramType.toString()
        listOf(hostBinder to domain)
    } else emptyList()
    val retSig = if (retParams.isEmpty()) retName
    else "$retName(${retParams.joinToString(", ") { it.first }})"
    val retAction = TlaAction(
        retName,
        "$retSig ==\n  ${retParts.joinToString("\n  ")}",
        retParams,
        comment = retComment,
    )

    return listOf(callAction, retAction)
}

/**
 * Collapse a host action with an uncomposed procfun call to a single step that
 * havocs the assigned vars (`var' \\in Domain`).
 */
internal fun emitProcFunHavocAction(
    site: ProcFunCallSite,
    hostLeaf: SpecLeaf,
    hostOffer: TlaActionOffer,
    procFun: ProcFunNode,
    allVars: List<String>,
    stateVarsByLeaf: Map<String, Set<String>>,
    stateVarNames: Map<Pair<String, String>, String>,
    cfgOverrides: MutableSet<String>,
): TlaAction {
    val hostBare = stateVarsByLeaf[hostLeaf.tlaName].orEmpty()
    val reserved = mutableSetOf<String>()
    reserved += hostBare
    val hostBinder = if (hostLeaf.isParameterized) {
        indexBinderName(hostLeaf, reserved).also { reserved += it }
    } else null

    fun idx(v: String, binder: String?): String = if (binder != null) "$v[$binder]" else v
    fun assignTrue(v: String, binder: String?): String =
        if (binder != null) "$v' = [$v EXCEPT ![$binder] = TRUE]" else "$v' = TRUE"

    val hostCtx = mapOf(hostLeaf.name to hostLeaf, hostLeaf.tlaName to hostLeaf)
    val hostArgNames = hostOffer.decl.action.args.map { it.name }.toSet()
    val parts = mutableListOf<String>()
    val changed = mutableSetOf<String>()

    val hostConstructed = stateTlaName(hostLeaf.tlaName, "constructed", stateVarNames)
    if (site.isHostConstructor) {
        parts += "/\\ ~${idx(hostConstructed, hostBinder)}"
    } else {
        parts += "/\\ ${idx(hostConstructed, hostBinder)}"
    }
    hostOffer.decl.guards.forEach { g ->
        parts += "/\\ ${exprToTla(g, hostCtx, hostArgNames, hostBinder, hostBare, stateVarNames = stateVarNames)}"
    }

    val retDomain = typeToTlaDomain(procFun.returnType)
    collectDomainModelName(retDomain, cfgOverrides)
    site.assignVars.forEach { varName ->
        val v = stateTlaName(hostLeaf.tlaName, varName, stateVarNames)
        if (hostBinder != null) {
            parts += "/\\ \\E __pf \\in $retDomain: $v' = [$v EXCEPT ![$hostBinder] = __pf]"
        } else {
            parts += "/\\ $v' \\in $retDomain"
        }
        changed += v
    }

    if (site.isHostConstructor) {
        parts += "/\\ ${assignTrue(hostConstructed, hostBinder)}"
        changed += hostConstructed
    }

    val unchanged = allVars.filter { it !in changed }
    if (unchanged.isNotEmpty()) {
        parts += "/\\ UNCHANGED <<${unchanged.joinToString(", ")}>>"
    }

    val params = mutableListOf<Pair<String, String>>()
    if (hostBinder != null) {
        val domain = typeDomainConstant(hostLeaf.paramType!!) ?: hostLeaf.paramType.toString()
        params += hostBinder to domain
    }
    fun refsArg(argName: String): Boolean =
        hostOffer.decl.guards.any { exprReferencesSymbol(it, argName) }
    hostOffer.decl.action.args.filter { refsArg(it.name) }.forEach { arg ->
        params += arg.name to typeToTlaDomain(arg.type)
    }
    val sig = if (params.isEmpty()) site.hostActionName
    else "${site.hostActionName}(${params.joinToString(", ") { it.first }})"
    return TlaAction(
        site.hostActionName,
        "$sig ==\n  ${parts.joinToString("\n  ")}",
        params.distinctBy { it.first },
        comment = "${site.hostActionName} havocs return of uncomposed procfun ${site.procFunName}",
    )
}

private fun collectDomainModelName(domain: String, into: MutableSet<String>) {
    // Simple domain names that need .cfg models (e.g. String, Int).
    val base = domain.trim().removePrefix("(").substringBefore(" ")
    if (base in setOf("Int", "Nat", "Real", "String")) into += base
}
