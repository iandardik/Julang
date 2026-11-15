package julay.compiler

import julay.compiler.ast.ApiNode
import julay.compiler.ast.ProcFunNode
import julay.compiler.ast.RootNode

/** Make a stable procfun id safe for Go/TLA identifiers (e.g. module `count-up` → `count_up`). */
fun sanitizeProcFunStableIdent(raw: String): String {
    val mapped = raw.map { ch ->
        if (ch.isLetterOrDigit() || ch == '_') ch else '_'
    }.joinToString("")
    return if (mapped.isNotEmpty() && mapped.first().isDigit()) "_$mapped" else mapped
}

/**
 * Stamp [ProcFunNode] defining-module + owning-api fields, and return ownership conflicts
 * (same home-module procfun listed in more than one api's `calls:`).
 */
fun stampProcFunIdentity(modules: List<LoadedModule>): List<CompileError> {
    val errors = mutableListOf<CompileError>()
    modules.forEach { mod ->
        mod.root.declNodes().filterIsInstance<ProcFunNode>().forEach { pf ->
            pf.setDefiningModulePath(mod.modulePath)
        }
        val ownerByBare = mutableMapOf<String, Pair<String, ApiNode>>()
        mod.root.declNodes().filterIsInstance<ApiNode>().forEach { api ->
            api.apiCallNames().forEach { call ->
                val prev = ownerByBare[call]
                if (prev != null && prev.first != api.apiName()) {
                    errors += TwoLocsCompileError(
                        api.programLocation(),
                        prev.second.programLocation(),
                        "Procfun \"$call\" is listed in more than one api's calls: " +
                            "\"${prev.first}\" and \"${api.apiName()}\"",
                    )
                } else {
                    ownerByBare[call] = api.apiName() to api
                }
            }
        }
        mod.root.declNodes().filterIsInstance<ProcFunNode>().forEach { pf ->
            pf.setOwningApiName(ownerByBare[pf.name()]?.first)
        }
    }
    return errors
}

/** All procfuns in [root], keyed by [ProcFunNode.stableName]. */
fun collectProcFunsByStableName(root: RootNode): Map<String, ProcFunNode> =
    root.declNodes().filterIsInstance<ProcFunNode>().associateBy { it.stableName() }

/** Stable ids for every procfun in [root]. */
fun collectProcFunStableNames(root: RootNode): Set<String> =
    root.declNodes().filterIsInstance<ProcFunNode>().map { it.stableName() }.toSet()

/** Per-module duplicate bare procfun names. */
fun duplicateProcFunNameErrors(modules: List<LoadedModule>): List<CompileError> {
    val errors = mutableListOf<CompileError>()
    modules.forEach { mod ->
        val byBare = mod.root.declNodes().filterIsInstance<ProcFunNode>().groupBy { it.name() }
        byBare.forEach { (bare, pfs) ->
            if (pfs.size < 2) return@forEach
            val first = pfs[0]
            pfs.drop(1).forEach { other ->
                errors += TwoLocsCompileError(
                    first.programLocation(),
                    other.programLocation(),
                    "Expected each procfun to have a unique name within module " +
                        "\"${mod.modulePath}\", but found at least two named \"$bare\"",
                )
            }
        }
    }
    return errors
}
