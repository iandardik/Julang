package julay.ast

fun ASTNode.procClassPass(procs: Set<String>): List<ProcClassDecl> = when (this) {
    is ProcClassNode -> procClassPassNode(procs)
    else -> children.flatMap { it.procClassPass(procs) }
}

private fun ProcClassNode.procClassPassNode(procs: Set<String>): List<ProcClassDecl> {
    if (procClassNodeName() !in procs) {
        return listOf()
    }
    val stateVars = localDecls().flatMap { it.stateVariables() }
    val constructors = localDecls().flatMap { it.constructors() }
    val transitions = localDecls().flatMap { it.transitions() }
    return listOf(ProcClassDecl(procClassNodeName(), stateVars, constructors, transitions))
}
