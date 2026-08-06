package julay.compiler.pass

import julay.compiler.CompileError
import julay.compiler.OneLocCompileError
import julay.compiler.ProgramLoc
import julay.compiler.ast.*
import julay.program.type.*

data class SortPassResult(
    val sorts: Map<String, SortType>,
    val errors: List<CompileError>,
)

fun RootNode.sortPass(): SortPassResult {
    val errors = mutableListOf<CompileError>()
    val sorts = linkedMapOf<String, SortType>()
    declNodes().filterIsInstance<SortDeclNode>().forEach { decl ->
        when (val built = buildSortType(decl)) {
            is SortBuildResult.Ok -> sorts[decl.name()] = built.type
            is SortBuildResult.Failed -> errors += built.errors
        }
    }
    return SortPassResult(sorts, errors)
}

fun julay.compiler.CompilationUnit.collectSorts(): SortPassResult {
    val errors = mutableListOf<CompileError>()
    val sorts = linkedMapOf<String, SortType>()
    modules.forEach { module ->
        val result = module.root.sortPass()
        errors += result.errors
        result.sorts.forEach { (name, type) ->
            if (name !in sorts) {
                sorts[name] = type
            }
        }
    }
    return SortPassResult(sorts, errors)
}

private sealed interface SortBuildResult {
    data class Ok(val type: SortType) : SortBuildResult
    data class Failed(val errors: List<CompileError>) : SortBuildResult
}

private fun buildSortType(decl: SortDeclNode): SortBuildResult {
    val loc = decl.programLocation()
    val elements = decl.elements
    if (elements.isEmpty()) {
        return SortBuildResult.Failed(
            listOf(OneLocCompileError(loc, "sort \"${decl.name()}\" must have at least one element")),
        )
    }

    val errors = mutableListOf<CompileError>()
    val elementType = elements[0].inferType(emptyMap())
    when (elementType) {
        is StringType, is IntType, is BoolType -> {}
        is RealType ->
            return SortBuildResult.Failed(
                listOf(
                    OneLocCompileError(
                        loc,
                        "sort \"${decl.name()}\" elements must be String, Int, or Boolean (got Real)",
                    ),
                ),
            )
        else ->
            return SortBuildResult.Failed(
                listOf(
                    OneLocCompileError(
                        loc,
                        "sort \"${decl.name()}\" elements must be String, Int, or Boolean (got $elementType)",
                    ),
                ),
            )
    }

    elements.forEach { lit ->
        val t = lit.inferType(emptyMap())
        if (t != elementType) {
            errors += OneLocCompileError(
                lit.programLocation(),
                "sort \"${decl.name()}\" elements must be homogeneous; expected $elementType but got $t",
            )
        }
        if (t is IntType) {
            val n = lit.literalText().toIntOrNull()
            if (n != null && n < 0) {
                errors += OneLocCompileError(
                    lit.programLocation(),
                    "sort \"${decl.name()}\" Int elements must be non-negative (TLC .cfg rejects unary minus)",
                )
            }
        }
    }

    val texts = elements.map { it.literalText() }
    val dup = texts.groupingBy { it }.eachCount().filter { it.value > 1 }.keys
    for (d in dup) {
        errors += OneLocCompileError(
            loc,
            "sort \"${decl.name()}\" has duplicate element \"$d\"",
        )
    }

    if (errors.isNotEmpty()) {
        return SortBuildResult.Failed(errors)
    }

    val cfgElements = elements.map { lit ->
        when (lit.inferType(emptyMap())) {
            is StringType -> "\"${lit.literalText()}\""
            is BoolType -> lit.literalText().uppercase() // true -> TRUE for TLA+
            else -> lit.literalText()
        }
    }

    return SortBuildResult.Ok(SortType(decl.name(), elementType, cfgElements))
}

/**
 * True when [type] is a sort domain (or a collection of such), not merely an
 * obj that nests sort-typed fields. Nested sort-bearing objs are allowed in
 * type-check and refused later for JAR targets.
 */
fun Type.isDirectSortDomain(): Boolean = when (this) {
    is SortType -> true
    is ListType -> elementType.isDirectSortDomain()
    is SetType -> elementType.isDirectSortDomain()
    is MapType -> keyType.isDirectSortDomain() || valueType.isDirectSortDomain()
    else -> false
}

fun sortDomainOnlyError(type: Type, loc: ProgramLoc): CompileError? {
    if (!type.isDirectSortDomain()) return null
    val sortName = firstDirectSortName(type) ?: type.toString()
    return OneLocCompileError(
        loc,
        "sort \"$sortName\" can only be used as a spec or quantifier domain",
    )
}

private fun firstDirectSortName(type: Type): String? = when (type) {
    is SortType -> type.name
    is ListType -> firstDirectSortName(type.elementType)
    is SetType -> firstDirectSortName(type.elementType)
    is MapType -> firstDirectSortName(type.keyType) ?: firstDirectSortName(type.valueType)
    else -> null
}
