package julay.compiler.pass

import julay.compiler.CompileError
import julay.compiler.OneLocCompileError
import julay.compiler.ProgramLoc
import julay.compiler.ast.ArgNode
import julay.compiler.ast.ArgsNode
import julay.compiler.ast.ConstructorNode
import julay.compiler.ast.ProcClassNode
import julay.compiler.ast.ProcFunNode
import julay.compiler.ast.RootNode
import julay.compiler.ast.TransitionNode
import julay.compiler.ast.VarNode
import julay.compiler.decl.ProcDecl
import julay.program.type.DomainKind
import julay.program.type.DomainType
import julay.program.type.ListType
import julay.program.type.MapType
import julay.program.type.ObjClassType
import julay.program.type.SetType
import julay.program.type.Type
import julay.program.type.containsUninterpretedType

/**
 * JAR targets must not reach uninterpreted types (including nested in records/collections).
 * Typedefs erase to their carrier and are JAR-legal. Specs / TLA+ may use uninterpreted freely.
 */
fun jarSortReachabilityErrors(
    jarTargets: List<ProcDecl>,
    ast: RootNode,
    procDecls: List<ProcDecl>,
): List<CompileError> {
    if (jarTargets.isEmpty()) return emptyList()
    val pclasses = ast.declNodes().filterIsInstance<ProcClassNode>().associateBy { it.name() }
    val procFuns = ast.declNodes().filterIsInstance<ProcFunNode>().associateBy { it.name() }
    val errors = mutableListOf<CompileError>()
    for (target in jarTargets) {
        val leafNames = target.allProcNames(procDecls)
        for (leafName in leafNames) {
            val pc = pclasses[leafName] ?: procFuns[leafName]?.asSyntheticProcClass() ?: continue
            errors += uninterpretedUsesInProcClass(pc, target.name)
        }
    }
    return errors
}

private fun uninterpretedUsesInProcClass(pc: ProcClassNode, jarTargetName: String): List<CompileError> {
    val errors = mutableListOf<CompileError>()
    for (decl in pc.localDecls()) {
        when (decl) {
            is VarNode -> {
                try {
                    reportIfUninterpreted(decl.type, decl.programLocation(), jarTargetName, "state variable \"${decl.name}\"")
                        ?.let { errors += it }
                } catch (_: RuntimeException) {
                }
            }
            is ConstructorNode ->
                errors += uninterpretedUsesInArgs(decl.constructorArgs(), jarTargetName, "constructor \"${decl.constructorName()}\"")
            is TransitionNode ->
                errors += uninterpretedUsesInArgs(decl.transitionArgs(), jarTargetName, "transition \"${decl.transitionName()}\"")
            else -> {}
        }
    }
    return errors
}

private fun uninterpretedUsesInArgs(
    args: ArgsNode,
    jarTargetName: String,
    context: String,
): List<CompileError> {
    val errors = mutableListOf<CompileError>()
    for (child in args.children) {
        if (child !is ArgNode) continue
        try {
            reportIfUninterpreted(child.type, child.programLocation(), jarTargetName, "$context argument \"${child.argName()}\"")
                ?.let { errors += it }
        } catch (_: RuntimeException) {
        }
    }
    return errors
}

private fun reportIfUninterpreted(
    type: Type,
    loc: ProgramLoc,
    jarTargetName: String,
    useSite: String,
): CompileError? {
    if (!type.containsUninterpretedType()) return null
    val detail = uninterpretedDetail(type) ?: type.toString()
    return OneLocCompileError(
        loc,
        "JAR target \"$jarTargetName\" uses uninterpreted type at $useSite ($detail); uninterpreted types are only for specs / TLA+",
    )
}

internal fun sortBearingDetail(type: Type): String? = uninterpretedDetail(type)

internal fun uninterpretedDetail(type: Type): String? = when {
    type is DomainType && type.kind == DomainKind.Uninterpreted -> "type \"${type.name}\""
    type is ObjClassType -> {
        val hit = type.fields.firstOrNull { it.type.containsUninterpretedType() }
        if (hit == null) {
            "\"${type.name}\""
        } else {
            val nested = uninterpretedDetail(hit.type) ?: hit.type.toString()
            "\"${type.name}\" (field ${hit.name} : $nested)"
        }
    }
    type is ListType -> uninterpretedDetail(type.elementType)?.let { "List<$it>" }
    type is SetType -> uninterpretedDetail(type.elementType)?.let { "Set<$it>" }
    type is MapType -> {
        val k = uninterpretedDetail(type.keyType)
        val v = uninterpretedDetail(type.valueType)
        when {
            k != null && v != null -> "Map<$k, $v>"
            k != null -> "Map<$k, …>"
            v != null -> "Map<…, $v>"
            else -> null
        }
    }
    else -> null
}
