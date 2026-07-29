package julay.compiler.pass

import julay.compiler.CompileError
import julay.compiler.CompileWarning
import julay.compiler.OneLocCompileError
import julay.compiler.OneLocCompileWarning
import julay.compiler.ast.*
import julay.compiler.collectApiNames
import julay.compiler.collectProcFunNames
import julay.compiler.decl.ProcDecl
import julay.compiler.decl.ProcDeclType

/**
 * Procfun names contributed by api `calls:` under a composition.
 * Call-site occurrence multiplicity is separate (from the call graph).
 */
fun composedProcFunNames(
    root: ProcDecl,
    procDecls: List<ProcDecl>,
    procFunNames: Set<String>,
): List<String> =
    collectLeafOccurrences(root, procDecls)
        .map { it.pclassName }
        .filter { it in procFunNames }

fun RootNode.procFunCompositionErrors(procDecls: List<ProcDecl>): List<CompileError> {
    // Duplicate listings inside a single api's calls: checked in ImportPass.
    // Orphan (composed but never called) does not apply: api calls are external entry points.
    // Ban on `|| procfun` is enforced in ImportPass.
    return apiQualifiedCallOccurrenceErrors(procDecls)
}

/**
 * `Api.fn(...)` requires a unique composition occurrence of [Api] when composed multiple times.
 * A locally declared api may be the compile root (zero `||` mentions) and still accept calls.
 */
private fun RootNode.apiQualifiedCallOccurrenceErrors(procDecls: List<ProcDecl>): List<CompileError> {
    val apiNames = collectApiNames(this)
    if (apiNames.isEmpty()) return emptyList()
    val refCounts = countApiCompositionRefs(this)
    val errors = mutableListOf<CompileError>()
    fun walk(node: ASTNode) {
        if (node is MethodCallExprNode) {
            val api = node.resolvedApiNameOrNull()
            if (api != null) {
                val count = refCounts[api] ?: 0
                when {
                    count > 1 -> errors += OneLocCompileError(
                        node.programLocation(),
                        "Api \"$api\" appears $count times in the composition; " +
                            "qualified calls require a unique occurrence",
                    )
                    count == 0 && api !in apiNames -> errors += OneLocCompileError(
                        node.programLocation(),
                        "Api \"$api\" is not composed in this program; cannot call $api.${node.methodName}(...)",
                    )
                }
            }
        }
        node.children.forEach { walk(it) }
    }
    walk(this)
    return errors
}

/** How many times each api name appears as a `||` / `proc:` leaf (not the api decl itself). */
private fun countApiCompositionRefs(root: RootNode): Map<String, Int> {
    val counts = mutableMapOf<String, Int>()
    fun walk(node: ASTNode) {
        when (node) {
            is ValueProcExprNode -> {
                if (!node.isQualified()) {
                    val name = node.valueProcName()
                    counts[name] = (counts[name] ?: 0) + 1
                }
            }
            else -> node.children.forEach { walk(it) }
        }
    }
    walk(root)
    return counts
}

/**
 * Warn when a spec calls a procfun that is not listed in any composed api's `calls:`
 * (return values will be havoced in TLA+).
 */
fun RootNode.procFunHavocWarnings(
    program: ProcDecl?,
    procDecls: List<ProcDecl>,
): List<CompileWarning> {
    if (program == null || program.type != ProcDeclType.Spec) return emptyList()
    val procFunNames = collectProcFunNames(this)
    if (procFunNames.isEmpty()) return emptyList()
    val composed = composedProcFunNames(program, procDecls, procFunNames).toSet()
    val hostLeaves = collectLeafOccurrences(program, procDecls)
        .map { it.pclassName }
        .filter { it !in procFunNames }
        .distinct()
    val pclasses = declNodes().filterIsInstance<ProcClassNode>().associateBy { it.name() }
    val missing = linkedSetOf<String>()
    hostLeaves.forEach { host ->
        val pc = pclasses[host] ?: return@forEach
        collectProcFunCallsInProc(pc).forEach { call ->
            val pf = call.resolvedProcFunOrNull()?.procFunName() ?: return@forEach
            if (pf in procFunNames && pf !in composed) missing += pf
        }
        collectApiQualifiedProcFunCallsInProc(pc).forEach { call ->
            val pf = call.resolvedProcFunOrNull()?.procFunName() ?: return@forEach
            if (pf in procFunNames && pf !in composed) missing += pf
        }
    }
    val loc = declNodes().find { it.name() == program.name }?.programLocation()
        ?: programLocation()
    return missing.map { pf ->
        OneLocCompileWarning(
            loc,
            "Procfun \"$pf\" is called in \"${program.name}\" but not listed in any api's calls:; " +
                "its return value will be havoced in TLA+. List it in an api's calls: to include it in the spec.",
        )
    }
}
