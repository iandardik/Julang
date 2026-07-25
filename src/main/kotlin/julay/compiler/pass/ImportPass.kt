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
    )

fun validateProcExprsInModule(
    root: RootNode,
    entryDeclNames: Set<String>,
    allPClassNames: Set<String>,
    allProcNames: Set<String>,
    importTable: ImportTable,
    moduleSymbols: Map<String, ResolvedSymbol>,
    procFunNames: Set<String> = emptySet(),
): List<CompileError> =
    validateProcExprsInNode(
        root, entryDeclNames, allPClassNames, allProcNames, importTable, moduleSymbols, procFunNames,
    )

private fun validateProcExprsInNode(
    node: ASTNode,
    entryDeclNames: Set<String>,
    allPClassNames: Set<String>,
    allProcNames: Set<String>,
    importTable: ImportTable,
    moduleSymbols: Map<String, ResolvedSymbol>,
    procFunNames: Set<String>,
): List<CompileError> {
    return when (node) {
        is ValueProcExprNode -> {
            val bare = if (!node.isQualified()) node.valueProcName() else null
            if (bare != null && bare in procFunNames) {
                return listOf(
                    OneLocCompileError(
                        node.programLocation(),
                        "Procfun \"$bare\" cannot appear in parallel composition; call it as a function instead",
                    ),
                )
            }
            val imported = if (bare != null) importTable.shortNames[bare] else null
            val importedDecl = imported?.let { declFromResolvedSymbol(it) }
            if (importedDecl is ProcFunNode) {
                return listOf(
                    OneLocCompileError(
                        node.programLocation(),
                        "Procfun \"${importedDecl.name()}\" cannot appear in parallel composition; call it as a function instead",
                    ),
                )
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
        else -> node.children.flatMap {
            validateProcExprsInNode(
                it, entryDeclNames, allPClassNames, allProcNames, importTable, moduleSymbols, procFunNames,
            )
        }
    }
}
