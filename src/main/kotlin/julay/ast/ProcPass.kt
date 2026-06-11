package julay.ast

fun ASTNode.procPass(): List<ProcDecl> = when (this) {
    is ProcNode -> listOf(ProcDecl(procNodeName(), procNodeValue().procPass(), ProcDeclType.Proc))
    is ProgramNode -> listOf(ProcDecl(programNodeName(), programNodeValue().procPass(), ProcDeclType.Program))
    is SpecNode -> listOf(ProcDecl(specNodeName(), specNodeValue().procPass(), ProcDeclType.Spec))
    is ValueProcExprNode -> listOf(ProcDecl(valueProcName(), listOf(), ProcDeclType.Proc))
    else -> children.flatMap { it.procPass() }
}
