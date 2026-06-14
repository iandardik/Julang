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
    )

fun validateProcExprsInModule(
    root: RootNode,
    entryDeclNames: Set<String>,
    allPClassNames: Set<String>,
    allProcNames: Set<String>,
    importTable: ImportTable,
    moduleSymbols: Map<String, ResolvedSymbol>,
): List<CompileError> =
    validateProcExprsInNode(root, entryDeclNames, allPClassNames, allProcNames, importTable, moduleSymbols)

private fun validateProcExprsInNode(
    node: ASTNode,
    entryDeclNames: Set<String>,
    allPClassNames: Set<String>,
    allProcNames: Set<String>,
    importTable: ImportTable,
    moduleSymbols: Map<String, ResolvedSymbol>,
): List<CompileError> {
    return when (node) {
        is ValueProcExprNode -> {
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
            validateProcExprsInNode(it, entryDeclNames, allPClassNames, allProcNames, importTable, moduleSymbols)
        }
    }
}
