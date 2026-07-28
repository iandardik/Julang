package julay.compiler.pass

import julay.compiler.*
import julay.compiler.ast.*
import julay.compiler.decl.*

fun ASTNode.procClassPass(procs: Set<String>): List<ProcClassDecl> = when (this) {
    is ProcClassNode -> procClassPassNode(procs)
    else -> children.flatMap { it.procClassPass(procs) }
}

private fun ProcClassNode.procClassPassNode(procs: Set<String>): List<ProcClassDecl> {
    if (procClassNodeName() !in procs) {
        return listOf()
    }
    val varNodes = localDecls().filterIsInstance<VarNode>()
    val stateVars = varNodes.flatMap { it.stateVariables() }
    // Inline `var x : T := e` counts as constructor assignment (ErrorPass); fold into each ctor
    // so Kotlin finishConstruction / runtime match that contract (same as procfun call-ctors).
    val inlineAssigns = varNodes.mapNotNull { v ->
        val init = v.initExpr ?: return@mapNotNull null
        TransitUpdate.Assign(v.name, init)
    }
    val constructors = localDecls().flatMap { it.constructors() }.map { ctor ->
        if (inlineAssigns.isEmpty()) ctor
        else ctor.copy(transits = inlineAssigns + ctor.transits)
    }
    val transitions = localDecls().flatMap { it.transitions() }
    return listOf(ProcClassDecl(procClassNodeName(), stateVars, constructors, transitions))
}
