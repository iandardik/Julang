package julay.compiler.pass

import julay.compiler.ast.*
import julay.compiler.decl.TransitUpdate
import julay.program.type.transitRootVar

internal fun invokeFlagName(occurrence: SpecLeaf): String = "invoke_${occurrence.tlaName}"

internal fun blockingVarName(hostTlaName: String): String = "${hostTlaName}_blocking"

internal fun returnToVarName(hostActionName: String): String = "returnTo_$hostActionName"

/** Handshake booleans for procfun spawn-await coupling. */
internal data class ProcFunHandshakeVars(
    val invokeFlags: List<Pair<SpecLeaf, String>>, // occurrence leaf → invoke_* name
    val blockingByHost: Map<String, String>, // host tlaName → blocking var
    val returnToByKey: Map<Pair<String, String>, String>, // (hostTla, action) → returnTo_*
) {
    fun allNames(): List<String> =
        invokeFlags.map { it.second } +
            blockingByHost.values +
            returnToByKey.values
}

internal fun buildProcFunHandshakeVars(callSites: List<ProcFunCallSite>): ProcFunHandshakeVars {
    val invokeFlags = callSites.map { it.occurrence to invokeFlagName(it.occurrence) }.distinctBy { it.second }
    val blockingByHost = callSites.associate { it.hostName to blockingVarName(it.hostName) }
    val returnToByKey = callSites.associate {
        (it.hostName to it.hostActionName) to returnToVarName(it.hostActionName)
    }
    return ProcFunHandshakeVars(invokeFlags, blockingByHost, returnToByKey)
}

/**
 * Emit `<action>_invoke` and resume `<action>` for a whole-RHS procfun call site.
 */
internal fun emitProcFunInvokeAndResume(
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
    val invokeFlag = handshake.invokeFlags.first { it.first.occurrenceId == child.occurrenceId }.second
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

    // --- invoke action ---
    val invokeParts = mutableListOf<String>()
    val invokeChanged = mutableSetOf<String>()

    val hostConstructed = stateTlaName(hostLeaf.tlaName, "constructed", stateVarNames)
    if (site.isHostConstructor) {
        invokeParts += "/\\ ~${idx(hostConstructed, hostBinder)}"
    } else {
        invokeParts += "/\\ ${idx(hostConstructed, hostBinder)}"
    }
    invokeParts += "/\\ ~${idx(blocking, hostBinder)}"

    hostOffer.decl.guards.forEach { g ->
        invokeParts += "/\\ ${exprToTla(g, hostCtx, hostArgNames, hostBinder, hostBare, stateVarNames = stateVarNames)}"
    }

    // Call-arg → child param binds + inline inits + constructed (folded former invoke_* ctor).
    val argNodes = procFun.procFunArgs().children.filterIsInstance<ArgNode>()
    val callArgs = site.call.callArgs()
    require(argNodes.size == callArgs.size) {
        "procfun ${site.procFunName} arity mismatch at call site"
    }
    val argTla = linkedMapOf<String, String>()
    argNodes.zip(callArgs).forEach { (arg, expr) ->
        val rhs = exprToTla(expr, hostCtx, hostArgNames, hostBinder, hostBare, stateVarNames = stateVarNames)
        argTla[arg.argName()] = rhs
        val v = stateTlaName(child.tlaName, arg.argName(), stateVarNames)
        invokeParts += "/\\ ${assignVal(v, childBinder, rhs)}"
        invokeChanged += v
    }
    procFun.localDecls().filterIsInstance<VarNode>().forEach { vn ->
        val init = vn.initExpr ?: return@forEach
        val rhs = exprToTla(
            init, childCtx, argTla.keys, childBinder, childBare,
            stateVarNames = stateVarNames,
            symbolOverrides = argTla,
        )
        val v = stateTlaName(child.tlaName, vn.name, stateVarNames)
        invokeParts += "/\\ ${assignVal(v, childBinder, rhs)}"
        invokeChanged += v
    }
    // Vars without inline init: leave at Init default (user ctor folded at runtime; v1 expects inline).
    val childConstructed = stateTlaName(child.tlaName, "constructed", stateVarNames)
    invokeParts += "/\\ ${assignTrue(childConstructed, childBinder)}"
    invokeChanged += childConstructed

    invokeParts += "/\\ ${assignTrue(invokeFlag, childBinder)}"
    invokeChanged += invokeFlag
    invokeParts += "/\\ ${assignTrue(blocking, hostBinder)}   \\* so ${site.hostName} does not execute other actions in the meantime"
    invokeChanged += blocking

    val invokeUnchanged = allVars.filter { it !in invokeChanged }
    if (invokeUnchanged.isNotEmpty()) {
        invokeParts += "/\\ UNCHANGED <<${invokeUnchanged.joinToString(", ")}>>"
    }

    val invokeName = "${site.hostActionName}_invoke"
    val invokeComment = "${site.hostActionName} invokes the procfun ${site.procFunName} before executing"
    val invokeParams = mutableListOf<Pair<String, String>>()
    if (hostBinder != null) {
        val domain = typeDomainConstant(hostLeaf.paramType!!) ?: hostLeaf.paramType.toString()
        invokeParams += hostBinder to domain
    }
    // Host action args referenced by guards / call args
    fun refsArg(argName: String): Boolean =
        hostOffer.decl.guards.any { exprReferencesSymbol(it, argName) } ||
            callArgs.any { exprReferencesSymbol(it, argName) }
    hostOffer.decl.action.args.filter { refsArg(it.name) }.forEach { arg ->
        invokeParams += arg.name to typeToTlaDomain(arg.type)
    }
    val invokeSig = if (invokeParams.isEmpty()) invokeName
    else "$invokeName(${invokeParams.joinToString(", ") { it.first }})"
    val invokeAction = TlaAction(
        invokeName,
        "$invokeSig ==\n  ${invokeParts.joinToString("\n  ")}",
        invokeParams.distinctBy { it.first },
        comment = invokeComment,
    )

    // --- resume action ---
    val resumeParts = mutableListOf<String>()
    val resumeChanged = mutableSetOf<String>()
    resumeParts += "/\\ ${idx(returnTo, hostBinder)}"
    resumeParts += "/\\ ${idx(blocking, hostBinder)}"

    val returnExpr = procFun.localDecls().filterIsInstance<TransitionNode>()
        .flatMap { it.transitions() }
        .firstOrNull { it.isReturn }
        ?.returnExpr
        ?: error("procfun ${site.procFunName} has no return transition for TLA resume")

    val retRhs = exprToTla(
        returnExpr, childCtx, emptySet(), childBinder, childBare,
        stateVarNames = stateVarNames,
    )
    site.assignVars.forEach { varName ->
        val v = stateTlaName(hostLeaf.tlaName, varName, stateVarNames)
        resumeParts += "/\\ ${assignVal(v, hostBinder, retRhs)}"
        resumeChanged += v
    }

    // Non-procfun assigns from the original transit (none in v1 whole-RHS-only case, but keep others).
    hostOffer.decl.transits.forEach { update ->
        when (update) {
            is TransitUpdate.Assign -> {
                val root = transitRootVar(update.key)
                if (root in site.assignVars) return@forEach
                if (update.expr is FunCallExprNode && update.expr.resolvedProcFunOrNull() != null) return@forEach
                val v = stateTlaName(hostLeaf.tlaName, root, stateVarNames)
                val rhs = exprToTla(update.expr, hostCtx, hostArgNames, hostBinder, hostBare, stateVarNames = stateVarNames)
                resumeParts += "/\\ ${assignVal(v, hostBinder, rhs)}"
                resumeChanged += v
            }
            is TransitUpdate.MapPut -> {
                // Map puts with procfun RHS are out of v1 scope; emit normally if no procfun.
                if (update.value is FunCallExprNode && update.value.resolvedProcFunOrNull() != null) return@forEach
                val v = stateTlaName(hostLeaf.tlaName, update.mapVar, stateVarNames)
                val k = exprToTla(update.key, hostCtx, hostArgNames, hostBinder, hostBare, stateVarNames = stateVarNames)
                val vv = exprToTla(update.value, hostCtx, hostArgNames, hostBinder, hostBare, stateVarNames = stateVarNames)
                if (hostBinder != null) {
                    resumeParts += "/\\ $v' = [$v EXCEPT ![$hostBinder] = [@ EXCEPT ![$k] = $vv]]"
                } else {
                    resumeParts += "/\\ $v' = [$v EXCEPT ![$k] = $vv]"
                }
                resumeChanged += v
            }
        }
    }

    if (site.isHostConstructor) {
        resumeParts += "/\\ ${assignTrue(hostConstructed, hostBinder)}"
        resumeChanged += hostConstructed
    }
    resumeParts += "/\\ ${assignFalse(blocking, hostBinder)}"
    resumeChanged += blocking
    resumeParts += "/\\ ${assignFalse(returnTo, hostBinder)}"
    resumeChanged += returnTo

    val resumeUnchanged = allVars.filter { it !in resumeChanged }
    if (resumeUnchanged.isNotEmpty()) {
        resumeParts += "/\\ UNCHANGED <<${resumeUnchanged.joinToString(", ")}>>"
    }

    val resumeComment = "The guards for ${site.hostActionName} appear in $invokeName"
    val resumeParams = if (hostBinder != null) {
        val domain = typeDomainConstant(hostLeaf.paramType!!) ?: hostLeaf.paramType.toString()
        listOf(hostBinder to domain)
    } else emptyList()
    val resumeSig = if (resumeParams.isEmpty()) site.hostActionName
    else "${site.hostActionName}(${resumeParams.joinToString(", ") { it.first }})"
    val resumeAction = TlaAction(
        site.hostActionName,
        "$resumeSig ==\n  ${resumeParts.joinToString("\n  ")}",
        resumeParams,
        comment = resumeComment,
    )

    return listOf(invokeAction, resumeAction)
}
