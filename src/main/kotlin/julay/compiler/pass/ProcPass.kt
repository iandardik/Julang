package julay.compiler.pass

import julay.compiler.*
import julay.compiler.ast.*
import julay.compiler.decl.*

fun ASTNode.procPass(): List<ProcDecl> = when (this) {
    is ProcNode -> listOf(ProcDecl(procNodeName(), procNodeValue().procPass(), ProcDeclType.Proc))
    is SpecNode -> listOf(ProcDecl(specNodeName(), specNodeValue().procPass(), ProcDeclType.Spec))
    is LeafSpecNode -> listOf(ProcDecl(leafSpecName(), emptyList(), ProcDeclType.Spec))
    is AgSpecExprNode -> {
        val assumeParts = assumeExpr()?.procPass() ?: emptyList()
        val systemParts = systemExpr().procPass()
        assumeParts + systemParts
    }
    is ParamProcExprNode -> paramBody().procPass()
    is ValueProcExprNode -> listOf(ProcDecl(valueProcName(), listOf(), ProcDeclType.Proc))
    is ChildBindingProcExprNode -> listOf(ProcDecl(childName(), listOf(), ProcDeclType.Proc, parentClass = parentName()))
    is CompositeProcExprNode -> children.flatMap { it.procPass() }
    else -> children.flatMap { it.procPass() }
}

fun ASTNode.resolvedProcPass(unit: CompilationUnit): List<ProcDecl> = when (this) {
    is ProcNode -> listOf(
        ProcDecl(procNodeName(), procNodeValue().resolvedProcPass(unit), ProcDeclType.Proc),
    )
    is SpecNode -> listOf(
        ProcDecl(specNodeName(), specNodeValue().resolvedProcPass(unit), ProcDeclType.Spec),
    )
    is LeafSpecNode -> listOf(ProcDecl(leafSpecName(), emptyList(), ProcDeclType.Spec))
    is AgSpecExprNode -> {
        val assumeParts = assumeExpr()?.resolvedProcPass(unit) ?: emptyList()
        val systemParts = systemExpr().resolvedProcPass(unit)
        assumeParts + systemParts
    }
    is ParamProcExprNode -> paramBody().resolvedProcPass(unit)
    is ValueProcExprNode -> {
        val (resolved, _) = resolveProcLeaf(
            this,
            unit.entryDeclNames,
            unit.allPClassNames,
            unit.allProcNames,
            unit.importTable,
            unit.moduleSymbols,
        )
        val flatName = resolved?.flatName ?: valueProcName()
        listOf(ProcDecl(flatName, listOf(), ProcDeclType.Proc))
    }
    is ChildBindingProcExprNode -> {
        fun flat(name: String): String = resolveProcLeaf(
            ValueProcExprNode(name, null, programLocation()),
            unit.entryDeclNames,
            unit.allPClassNames,
            unit.allProcNames,
            unit.importTable,
            unit.moduleSymbols,
        ).first?.flatName ?: name
        listOf(ProcDecl(flat(childName()), listOf(), ProcDeclType.Proc, parentClass = flat(parentName())))
    }
    is CompositeProcExprNode -> children.flatMap { it.resolvedProcPass(unit) }
    else -> children.flatMap { it.resolvedProcPass(unit) }
}
