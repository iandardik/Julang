package julay.compiler.pass

import julay.compiler.ast.*
import julay.compiler.decl.ActionDecl
import julay.compiler.decl.ProcClassDecl
import julay.compiler.decl.TransitUpdate
import julay.program.Variable
import julay.program.action.SymbolicAction
import julay.program.action.TSAction

/** Synthetic constructor name for a procfun: `invoke_<procfunName>`. */
fun procFunInvokeCtor(name: String) = "invoke_$name"

/** Lower each [ProcFunNode] to a [ProcClassDecl] with synthetic `invoke_<name>` constructor. */
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
    val stateVars = argVars + userVars

    val argAssigns = argVars.map { v ->
        TransitUpdate.Assign(v.name, SymbolValueExprNode(v.name, programLocation()).also {
            // Type already known from arg; inferType needs env — set directly for codegen.
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

    val invokeCtor = ActionDecl(
        SymbolicAction(procFunInvokeCtor(name()), argVars),
        guards = emptyList(),
        transits = argAssigns + inlineAssigns + userCtorTransits,
        modifier = TSAction.SyncRole.Default,
        loc = programLocation(),
        befores = userCtorBefores,
        afters = userCtorAfters,
        errors = userCtorErrors,
    )
    val transitions = localDecls().flatMap { it.transitions() }
    return ProcClassDecl(name(), stateVars, listOf(invokeCtor), transitions)
}
