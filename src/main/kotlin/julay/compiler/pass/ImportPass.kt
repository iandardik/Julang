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
        collectProcFunNames(unit.root),
        unit.allLeafSpecNames,
    )

fun validateProcExprsInModule(
    root: RootNode,
    entryDeclNames: Set<String>,
    allPClassNames: Set<String>,
    allProcNames: Set<String>,
    importTable: ImportTable,
    moduleSymbols: Map<String, ResolvedSymbol>,
    procFunNames: Set<String> = emptySet(),
    leafSpecNames: Set<String> = emptySet(),
): List<CompileError> =
    root.declNodes().flatMap { decl ->
        when (decl) {
            is ProcNode -> validateProcExprsInNode(
                decl.procNodeValue(),
                entryDeclNames,
                allPClassNames,
                allProcNames,
                importTable,
                moduleSymbols,
                procFunNames,
                banLeafSpecs = true,
                leafSpecNames,
            )
            is ApiNode -> validateProcExprsInNode(
                decl,
                entryDeclNames,
                allPClassNames,
                allProcNames,
                importTable,
                moduleSymbols,
                procFunNames,
                banLeafSpecs = true,
                leafSpecNames,
            )
            is SpecNode -> validateProcExprsInNode(
                decl,
                entryDeclNames,
                allPClassNames,
                allProcNames,
                importTable,
                moduleSymbols,
                procFunNames,
                banLeafSpecs = false,
                leafSpecNames,
            )
            else -> emptyList()
        }
    }

private fun validateProcExprsInNode(
    node: ASTNode,
    entryDeclNames: Set<String>,
    allPClassNames: Set<String>,
    allProcNames: Set<String>,
    importTable: ImportTable,
    moduleSymbols: Map<String, ResolvedSymbol>,
    procFunNames: Set<String>,
    banLeafSpecs: Boolean,
    leafSpecNames: Set<String>,
): List<CompileError> {
    return when (node) {
        is ValueProcExprNode -> {
            val bare = if (!node.isQualified()) node.valueProcName() else null
            // Procfuns may not appear in || — use an api's calls: for TLA coupling.
            // Indexed spec leaves (banLeafSpecs = false) may reference procfuns directly.
            if (banLeafSpecs) {
                if (bare != null && bare in procFunNames) {
                    return listOf(
                        OneLocCompileError(
                            node.programLocation(),
                            "Procfun \"$bare\" cannot appear in parallel composition; " +
                                "list it in an api's calls: instead",
                        ),
                    )
                }
                val imported = if (bare != null) importTable.shortNames[bare] else null
                val importedDecl = imported?.let { declFromResolvedSymbol(it) }
                if (importedDecl is ProcFunNode) {
                    return listOf(
                        OneLocCompileError(
                            node.programLocation(),
                            "Procfun \"${importedDecl.procFunName()}\" cannot appear in parallel composition; " +
                                "list it in an api's calls: instead",
                        ),
                    )
                }
            }
            val imported = if (bare != null) importTable.shortNames[bare] else null
            val importedDecl = imported?.let { declFromResolvedSymbol(it) }
            if (banLeafSpecs) {
                val leafSpecError = leafSpecInProcAssemblyError(
                    node, bare, importedDecl, leafSpecNames, moduleSymbols,
                )
                if (leafSpecError != null) return listOf(leafSpecError)
            }
            val (_, error) = resolveProcLeaf(
                node,
                entryDeclNames,
                allPClassNames,
                allProcNames,
                importTable,
                moduleSymbols,
            )
            if (error != null) listOf(error) else emptyList()
        }
        is ApiNode -> {
            val procErrors = validateProcExprsInNode(
                node.apiProcExpr(),
                entryDeclNames, allPClassNames, allProcNames, importTable, moduleSymbols, procFunNames,
                banLeafSpecs, leafSpecNames,
            )
            val callErrors = node.apiCallNames().flatMap { callName ->
                val localPf = callName in procFunNames
                val importedPf = importTable.shortNames[callName]?.let { declFromResolvedSymbol(it) } is ProcFunNode
                when {
                    localPf || importedPf -> emptyList()
                    else -> listOf(
                        OneLocCompileError(
                            node.programLocation(),
                            "Unknown procfun \"$callName\" in api \"${node.apiName()}\" calls:",
                        ),
                    )
                }
            }
            val dupCalls = node.apiCallNames().groupingBy { it }.eachCount().filter { it.value > 1 }
            val dupErrors = dupCalls.map { (name, n) ->
                OneLocCompileError(
                    node.programLocation(),
                    "Procfun \"$name\" listed $n times in api \"${node.apiName()}\" calls:",
                )
            }
            procErrors + callErrors + dupErrors
        }
        else -> node.children.flatMap {
            validateProcExprsInNode(
                it, entryDeclNames, allPClassNames, allProcNames, importTable, moduleSymbols, procFunNames,
                banLeafSpecs, leafSpecNames,
            )
        }
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
