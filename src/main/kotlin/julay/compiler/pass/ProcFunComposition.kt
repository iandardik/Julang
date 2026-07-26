package julay.compiler.pass

import julay.compiler.CompileError
import julay.compiler.CompileWarning
import julay.compiler.OneLocCompileError
import julay.compiler.OneLocCompileWarning
import julay.compiler.ast.*
import julay.compiler.collectProcFunNames
import julay.compiler.decl.ProcDecl
import julay.compiler.decl.ProcDeclType

/**
 * Procfun names listed in a composition (`||`) — spec/analyze metadata only.
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
    val procFunNames = collectProcFunNames(this)
    if (procFunNames.isEmpty()) return emptyList()
    val pclasses = declNodes().filterIsInstance<ProcClassNode>().associateBy { it.name() }
    return declNodes().filter { it is ProcNode || it is SpecNode }.flatMap { decl ->
        val pd = procDecls.find { it.name == decl.name() } ?: return@flatMap emptyList()
        val composed = composedProcFunNames(pd, procDecls, procFunNames)
        val dupErrors = composed.groupingBy { it }.eachCount().filter { it.value > 1 }.map { (pf, n) ->
            OneLocCompileError(
                decl.programLocation(),
                "Procfun \"$pf\" may appear at most once in composition (listed $n times in \"${decl.name()}\")",
            )
        }
        val hostLeaves = collectLeafOccurrences(pd, procDecls)
            .map { it.pclassName }
            .filter { it !in procFunNames }
            .distinct()
        val called = hostLeaves.flatMap { host ->
            val pc = pclasses[host] ?: return@flatMap emptyList()
            collectWholeRhsProcFunCalls(pc).mapNotNull { it.call.resolvedProcFunOrNull()?.procFunName() }
        }.toSet()
        val orphanErrors = composed.distinct().filter { it !in called }.map { pf ->
            OneLocCompileError(
                decl.programLocation(),
                "Procfun \"$pf\" is composed in \"${decl.name()}\" but never called by a peer in that composition",
            )
        }
        dupErrors + orphanErrors
    }
}

/**
 * Warn when a spec calls a procfun that is not listed in `||`
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
        collectWholeRhsProcFunCalls(pc).forEach { hit ->
            val pf = hit.call.resolvedProcFunOrNull()?.procFunName() ?: return@forEach
            if (pf in procFunNames && pf !in composed) missing += pf
        }
    }
    val loc = declNodes().find { it.name() == program.name }?.programLocation()
        ?: programLocation()
    return missing.map { pf ->
        OneLocCompileWarning(
            loc,
            "Procfun \"$pf\" is called in \"${program.name}\" but not composed via ||; " +
                "its return value will be havoced in TLA+. Add \"|| $pf\" to include it in the spec.",
        )
    }
}
