package julay.compiler.pass

import julay.compiler.*
import julay.compiler.ast.*

fun validateProcExprs(unit: CompilationUnit): List<CompileError> =
    validateProcExprsInModule(
        unit.root,
        unit.entryDeclNames,
        unit.allPClassNames,
        unit.allProcNames,
        unit.importTable,
        unit.moduleSymbols,
        unit.allLeafSpecNames,
    )

fun validateProcExprsInModule(
    root: RootNode,
    entryDeclNames: Set<String>,
    allPClassNames: Set<String>,
    allProcNames: Set<String>,
    importTable: ImportTable,
    moduleSymbols: Map<String, ResolvedSymbol>,
    leafSpecNames: Set<String> = emptySet(),
): List<CompileError> {
    val ctx = ProcExprCtx(root, entryDeclNames, allPClassNames, allProcNames, importTable, moduleSymbols, leafSpecNames)
    return root.declNodes().flatMap { decl ->
        when (decl) {
            is ProcNode -> validateProcExprsInNode(decl.procNodeValue(), ctx, banLeafSpecs = true)
            is SpecNode -> validateProcExprsInNode(decl, ctx, banLeafSpecs = false)
            else -> emptyList()
        }
    }
}

private class ProcExprCtx(
    val root: RootNode,
    val entryDeclNames: Set<String>,
    val allPClassNames: Set<String>,
    val allProcNames: Set<String>,
    val importTable: ImportTable,
    val moduleSymbols: Map<String, ResolvedSymbol>,
    val leafSpecNames: Set<String>,
) {
    fun resolve(node: ValueProcExprNode): Pair<ResolvedProcRef?, CompileError?> =
        resolveProcLeaf(node, entryDeclNames, allPClassNames, allProcNames, importTable, moduleSymbols)

    /** Declaration named by a composition leaf (local or imported), if any. */
    fun declOf(node: ValueProcExprNode): DeclNode? {
        if (node.isQualified()) {
            val resolved = resolveQualifiedName(node.qualifiedParts()!!, moduleSymbols) ?: return null
            return declFromResolvedSymbol(resolved)
        }
        val bare = node.valueProcName()
        importTable.shortNames[bare]?.let { return declFromResolvedSymbol(it) }
        return root.declNodes().firstOrNull { it.name() == bare }
    }
}

private fun validateProcExprsInNode(
    node: ASTNode,
    ctx: ProcExprCtx,
    banLeafSpecs: Boolean,
): List<CompileError> {
    return when (node) {
        is ValueProcExprNode -> {
            val bare = if (!node.isQualified()) node.valueProcName() else null
            val imported = if (bare != null) ctx.importTable.shortNames[bare] else null
            val importedDecl = imported?.let { declFromResolvedSymbol(it) }
            if (banLeafSpecs) {
                val leafSpecError = leafSpecInProcAssemblyError(
                    node, bare, importedDecl, ctx.leafSpecNames, ctx.moduleSymbols,
                )
                if (leafSpecError != null) return listOf(leafSpecError)
            }
            val (_, error) = ctx.resolve(node)
            return if (error != null) listOf(error) else emptyList()
        }
        is ChildBindingProcExprNode -> {
            val loc = node.programLocation()
            val childNode = ValueProcExprNode(node.childName(), null, loc)
            val parentNode = ValueProcExprNode(node.parentName(), null, loc)
            val errors = mutableListOf<CompileError>()
            ctx.resolve(childNode).second?.let { errors += it }
            ctx.resolve(parentNode).second?.let { errors += it }
            if (errors.isNotEmpty()) return errors
            val child = ctx.declOf(childNode)
            val parent = ctx.declOf(parentNode)
            when {
                child !is ProcClassNode ->
                    errors += OneLocCompileError(
                        loc,
                        "\"${node.childName()}\" in \"$node\" must be a proc class",
                    )
                child.isStatic ->
                    errors += OneLocCompileError(
                        loc,
                        "static proc \"${node.childName()}\" cannot be bound as ${node.childName()}<…>; " +
                            "only dynamic procs may have a parent",
                    )
            }
            if (parent !is ProcClassNode && parent !is LeafSpecNode) {
                errors += OneLocCompileError(
                    loc,
                    "parent \"${node.parentName()}\" in \"$node\" must be a proc class",
                )
            }
            if (banLeafSpecs && parent is LeafSpecNode) {
                errors += OneLocCompileError(loc, "leaf spec \"${node.parentName()}\" cannot appear in a proc assembly")
            }
            errors
        }
        else -> node.children.flatMap { validateProcExprsInNode(it, ctx, banLeafSpecs) }
    }
}

private fun leafSpecInProcAssemblyError(
    node: ValueProcExprNode,
    bare: String?,
    importedDecl: DeclNode?,
    leafSpecNames: Set<String>,
    moduleSymbols: Map<String, ResolvedSymbol>,
): CompileError? {
    fun errorFor(name: String) = OneLocCompileError(
        node.programLocation(),
        "leaf spec \"$name\" cannot appear in a proc assembly",
    )
    if (importedDecl is LeafSpecNode) return errorFor(importedDecl.leafSpecName())
    if (bare != null && bare in leafSpecNames) return errorFor(bare)
    if (node.isQualified()) {
        val parts = node.qualifiedParts()!!
        val resolved = resolveQualifiedName(parts, moduleSymbols)
        val decl = resolved?.let { declFromResolvedSymbol(it) }
        if (decl is LeafSpecNode) return errorFor(decl.leafSpecName())
    }
    return null
}
