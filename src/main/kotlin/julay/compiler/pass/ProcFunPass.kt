package julay.compiler.pass

import julay.compiler.ast.*
import julay.compiler.decl.ActionDecl
import julay.compiler.decl.ProcClassDecl
import julay.compiler.decl.TransitUpdate
import julay.program.Variable
import julay.program.action.SymbolicAction
import julay.program.action.TSAction
import julay.program.type.Type
import julay.program.type.boolType
import julay.program.type.intType

/** Synthetic constructor name for a procfun: `<procfunName>_call`. */
fun procFunCallCtor(name: String) = "${name}_call"

/** Synthetic return transition name for a procfun: `<procfunName>_ret`. */
fun procFunRetAction(name: String) = "${name}_ret"

/** Synthetic state var holding the value yielded by `return:`. */
const val PROC_FUN_RET_VAL = "retVal"

/** @deprecated Use [procFunCallCtor]; kept for call-site renames during migration. */
@Deprecated("Use procFunCallCtor", ReplaceWith("procFunCallCtor(name)"))
fun procFunInvokeCtor(name: String) = procFunCallCtor(name)

/** Lower each [ProcFunNode] to a [ProcClassDecl] with synthetic `_call` / `_ret`. */
fun RootNode.procFunClassPass(): List<ProcClassDecl> =
    declNodes().filterIsInstance<ProcFunNode>().map { it.toProcClassDecl() }

internal fun ProcFunNode.toProcClassDecl(): ProcClassDecl {
    val argVars = try {
        procFunArgs().actionArgs()
    } catch (_: RuntimeException) {
        emptyList()
    }
    val userVarNodes = localDecls().filterIsInstance<VarNode>()
    val userVars = userVarNodes.flatMap { it.stateVariables() }
    val retType = try {
        returnType
    } catch (_: RuntimeException) {
        // Type pass failed; emit a stub so later errors can still report.
        intType
    }
    val retValVar = Variable(PROC_FUN_RET_VAL, retType)
    val stateVars = argVars + userVars + retValVar

    val callCtor = buildCallCtor(argVars, userVarNodes)
    val userTransitions = localDecls().flatMap { decl ->
        if (decl !is TransitionNode) return@flatMap emptyList()
        listOf(rewriteReturnToRetVal(decl, retType))
    }
    val retTransition = procFunRetActionDecl(name(), retType, programLocation())
    return ProcClassDecl(name(), stateVars, listOf(callCtor), userTransitions + retTransition)
}

private fun ProcFunNode.buildCallCtor(
    argVars: List<Variable>,
    userVarNodes: List<VarNode>,
): ActionDecl {
    val argAssigns = argVars.map { v ->
        TransitUpdate.Assign(v.name, SymbolValueExprNode(v.name, programLocation()).also {
            it.setInferredType(TypePassType.Inferred(v.type))
        })
    }
    val inlineAssigns = userVarNodes.mapNotNull { v ->
        val init = v.initExpr ?: return@mapNotNull null
        TransitUpdate.Assign(v.name, init)
    }
    val userCtor = localDecls().filterIsInstance<ConstructorNode>().singleOrNull()
    val userCtorTransits = userCtor?.body()?.flatMap { it.transits() } ?: emptyList()
    val userCtorBefores = userCtor?.body()?.flatMap { it.befores() } ?: emptyList()
    val userCtorAfters = userCtor?.body()?.flatMap { it.afters() } ?: emptyList()
    val userCtorErrors = userCtor?.body()?.flatMap { it.errors() } ?: emptyList()

    return ActionDecl(
        SymbolicAction(procFunCallCtor(name()), argVars),
        guards = emptyList(),
        transits = argAssigns + inlineAssigns + userCtorTransits,
        modifier = TSAction.SyncRole.Default,
        loc = programLocation(),
        befores = userCtorBefores,
        afters = userCtorAfters,
        errors = userCtorErrors,
    )
}

/**
 * `return: e` becomes `transit: retVal := e` (modifier preserved except bare → internal).
 * Bare `return:` fires solo under spawn-and-await (no SyncChannel peer); client/session keep tags.
 * [returnExpr] is kept so runtime still completes on this step (one logical return);
 * synthetic `_ret` is for TLA/alphabet and is omitted from Kotlin offers.
 */
private fun rewriteReturnToRetVal(trans: TransitionNode, retType: Type): ActionDecl {
    val decl = trans.transitions().single()
    val returnExpr = decl.returnExpr ?: return decl
    val retAssign = TransitUpdate.Assign(
        PROC_FUN_RET_VAL,
        returnExpr.also {
            try {
                it.getType()
            } catch (_: RuntimeException) {
                it.setInferredType(TypePassType.Inferred(retType))
            }
        },
    )
    // Untagged return edges complete alone; tagged client/session/internal keep their sync role.
    val effectiveModifier =
        if (decl.modifier == TSAction.SyncRole.Default && !decl.isSession) {
            TSAction.SyncRole.Internal
        } else {
            decl.modifier
        }
    return ActionDecl(
        SymbolicAction(
            decl.action.name,
            decl.action.args,
            isInternal = effectiveModifier == TSAction.SyncRole.Internal,
            isSession = decl.isSession,
        ),
        decl.guards,
        decl.transits + retAssign,
        effectiveModifier,
        decl.loc,
        decl.befores,
        decl.afters,
        decl.errors,
        returnExpr = returnExpr,
    )
}

/**
 * Synthetic completion edge: `transition F_ret(ret : Ret) { guard: ret = retVal }` with
 * [ActionDecl.returnExpr] set so runtime still completes via `_procFunReturn` after this step.
 * Marked internal so it can fire without a SyncChannel peer under spawn-and-await.
 */
private fun procFunRetActionDecl(
    procFunName: String,
    retType: Type,
    loc: julay.compiler.ProgramLoc,
): ActionDecl {
    val retArg = Variable("ret", retType)
    val retSym = SymbolValueExprNode("ret", loc).also {
        it.setInferredType(TypePassType.Inferred(retType))
    }
    val retValSym = SymbolValueExprNode(PROC_FUN_RET_VAL, loc).also {
        it.setInferredType(TypePassType.Inferred(retType))
    }
    val eqGuard = BinaryOpExprNode("=", retSym, retValSym, loc).also {
        it.setInferredType(TypePassType.Inferred(boolType))
    }
    return ActionDecl(
        SymbolicAction(procFunRetAction(procFunName), listOf(retArg), isInternal = true),
        guards = listOf(eqGuard),
        transits = emptyList(),
        modifier = TSAction.SyncRole.Internal,
        loc = loc,
        returnExpr = retValSym,
    )
}
