package julay.compiler.pass

import julay.compiler.*
import julay.compiler.ast.*
import julay.compiler.ObjClassBuiltinRegistry
import julay.program.type.*

data class DomainPassResult(
    val domains: Map<String, DomainType>,
    val errors: List<CompileError>,
)

fun RootNode.domainPass(recordNames: Set<String> = emptySet()): DomainPassResult {
    val errors = mutableListOf<CompileError>()
    val domains = linkedMapOf<String, DomainType>()

    declNodes().filterIsInstance<DomainDeclNode>().forEach { decl ->
        if (decl.name() in domains) {
            errors += OneLocCompileError(decl.programLocation(), "Duplicate type \"${decl.name()}\"")
            return@forEach
        }
        if (ObjClassBuiltinRegistry.isBuiltin(decl.name())) {
            errors += OneLocCompileError(
                decl.programLocation(),
                "type \"${decl.name()}\" conflicts with a builtin type",
            )
            return@forEach
        }
        if (decl.name() in recordNames) {
            errors += OneLocCompileError(
                decl.programLocation(),
                "type \"${decl.name()}\" conflicts with a record of the same name",
            )
            return@forEach
        }
        when (val alias = decl.aliasTypeExpr()) {
            null -> domains[decl.name()] = DomainType(
                decl.name(),
                DomainKind.Uninterpreted,
                stringType,
                cfgElements = null,
            )
            else -> {
                val carrier = builtinCarrierFromTypeExpr(alias)
                if (carrier == null) {
                    errors += OneLocCompileError(
                        decl.programLocation(),
                        "typedef \"${decl.name()}\" must alias Boolean, Int, or String (got $alias)",
                    )
                } else {
                    domains[decl.name()] = DomainType(
                        decl.name(),
                        DomainKind.Typedef,
                        carrier,
                        cfgElements = null,
                    )
                }
            }
        }
    }

    return DomainPassResult(domains, errors)
}

fun CompilationUnit.collectDomains(): DomainPassResult {
    val errors = mutableListOf<CompileError>()
    val domains = linkedMapOf<String, DomainType>()
    val recordNames = modules.flatMap { m ->
        m.root.declNodes().filterIsInstance<ObjClassNode>().map { it.name() }
    }.toSet()

    modules.forEach { module ->
        val result = module.root.domainPass(recordNames)
        errors += result.errors
        result.domains.forEach { (name, type) ->
            if (name !in domains) {
                domains[name] = type
            }
        }
    }

    // Delayed models live only inside create-index / leaf-spec bodies (not top-level decls).
    // Opaque record index-sort models are accepted later in TLA codegen (once create-index
    // sorts are known); here only domain/typedef models are merged.
    val nestedModels = modules.flatMap { it.root.collectNestedTypeModels() }
    val mergedResult = mergeSpecTypeModels(nestedModels, domains, recordNames)
    errors += mergedResult.errors
    return DomainPassResult(mergedResult.domains, errors)
}

/** Collect delayed models from create-index blocks and leaf-spec bodies. */
fun ASTNode.collectNestedTypeModels(): List<TypeModelNode> = when (this) {
    is TypeModelNode -> listOf(this)
    is LeafSpecNode -> typeModels() + localDecls().flatMap { it.collectNestedTypeModels() }
    else -> children.flatMap { it.collectNestedTypeModels() }
}

/** Collect spec-only fun overrides from create-index blocks and leaf-spec bodies. */
fun ASTNode.collectNestedFunModels(): List<FunModelNode> = when (this) {
    is FunModelNode -> listOf(this)
    is LeafSpecNode -> funModels() + localDecls().flatMap { it.collectNestedFunModels() }
    else -> children.flatMap { it.collectNestedFunModels() }
}

/**
 * Merge fun overrides by name. Conflicting exprs for the same name are errors.
 * Returns the surviving model per name (first wins when equal by string).
 */
fun mergeFunModels(
    models: List<FunModelNode>,
): Pair<Map<String, FunModelNode>, List<CompileError>> {
    val out = linkedMapOf<String, FunModelNode>()
    val errors = mutableListOf<CompileError>()
    models.forEach { model ->
        val prev = out[model.name()]
        if (prev != null && prev.overrideExpr.toString() != model.overrideExpr.toString()) {
            errors += OneLocCompileError(
                model.programLocation(),
                "Conflicting fun overrides for \"${model.name()}\"",
            )
        } else if (prev == null) {
            out[model.name()] = model
        }
    }
    return out to errors
}

sealed interface DomainModelBuildResult {
    data class Ok(val type: DomainType) : DomainModelBuildResult
    data class Failed(val errors: List<CompileError>) : DomainModelBuildResult
}

/**
 * Result of parsing delayed-model literals (shared by domain and record index-sort models).
 */
internal data class DelayedModelLiterals(
    val elementType: Type,
    val cfgElements: List<String>,
)

internal fun parseDelayedModelLiterals(model: TypeModelNode): DomainModelBuildResult {
    val loc = model.programLocation()
    val name = model.name()
    val elements = model.elements
    if (elements.isEmpty()) {
        return DomainModelBuildResult.Failed(
            listOf(OneLocCompileError(loc, "delayed model for \"$name\" must have at least one element")),
        )
    }

    val errors = mutableListOf<CompileError>()
    val elementType = elements[0].inferType(emptyMap())
    when (elementType) {
        is StringType, is IntType, is BoolType -> {}
        is RealType ->
            return DomainModelBuildResult.Failed(
                listOf(
                    OneLocCompileError(
                        loc,
                        "delayed model for \"$name\" elements must be String, Int, or Boolean (got Real)",
                    ),
                ),
            )
        else ->
            return DomainModelBuildResult.Failed(
                listOf(
                    OneLocCompileError(
                        loc,
                        "delayed model for \"$name\" elements must be String, Int, or Boolean (got $elementType)",
                    ),
                ),
            )
    }

    elements.forEach { lit ->
        val t = lit.inferType(emptyMap())
        if (t != elementType) {
            errors += OneLocCompileError(
                lit.programLocation(),
                "delayed model for \"$name\" elements must be homogeneous; expected $elementType but got $t",
            )
        }
        if (t is IntType) {
            val n = lit.literalText().toIntOrNull()
            if (n != null && n < 0) {
                errors += OneLocCompileError(
                    lit.programLocation(),
                    "delayed model \"$name\" Int elements must be non-negative (TLC .cfg rejects unary minus)",
                )
            }
        }
    }

    val texts = elements.map { it.literalText() }
    val dup = texts.groupingBy { it }.eachCount().filter { it.value > 1 }.keys
    for (d in dup) {
        errors += OneLocCompileError(loc, "delayed model for \"$name\" has duplicate element \"$d\"")
    }

    if (errors.isNotEmpty()) {
        return DomainModelBuildResult.Failed(errors)
    }

    val cfgElements = elements.map { lit ->
        when (lit.inferType(emptyMap())) {
            is StringType -> "\"${lit.literalText()}\""
            is BoolType -> lit.literalText().uppercase()
            else -> lit.literalText()
        }
    }
    // DomainModelBuildResult.Ok is DomainType-shaped; callers that only need literals use
    // [parseDelayedModelCfg] instead.
    return DomainModelBuildResult.Ok(
        DomainType(name, DomainKind.Uninterpreted, elementType, cfgElements),
    )
}

/** Parse delayed-model literals into cfg tokens without requiring a domain type. */
internal fun parseDelayedModelCfg(model: TypeModelNode): Pair<DelayedModelLiterals?, List<CompileError>> =
    when (val r = parseDelayedModelLiterals(model)) {
        is DomainModelBuildResult.Ok ->
            DelayedModelLiterals(r.type.carrierType, r.type.cfgElements!!) to emptyList()
        is DomainModelBuildResult.Failed -> null to r.errors
    }

internal fun buildDomainModel(
    model: TypeModelNode,
    existing: DomainType?,
    recordNames: Set<String>,
): DomainModelBuildResult {
    val loc = model.programLocation()
    val name = model.name()
    // Record delayed models are allowed (opaque index-sort atoms); handled in merge.
    if (name in recordNames) {
        return parseDelayedModelLiterals(model)
    }
    if (existing == null) {
        return DomainModelBuildResult.Failed(
            listOf(OneLocCompileError(loc, "Unknown type \"$name\" for delayed model")),
        )
    }

    return when (val parsed = parseDelayedModelLiterals(model)) {
        is DomainModelBuildResult.Failed -> parsed
        is DomainModelBuildResult.Ok -> {
            val elementType = parsed.type.carrierType
            if (existing.kind == DomainKind.Typedef && elementType != existing.carrierType) {
                DomainModelBuildResult.Failed(
                    listOf(
                        OneLocCompileError(
                            loc,
                            "delayed model for typedef \"$name\" must match carrier ${existing.carrierType} (got $elementType)",
                        ),
                    ),
                )
            } else {
                DomainModelBuildResult.Ok(
                    existing.withModel(elementType, parsed.type.cfgElements!!),
                )
            }
        }
    }
}

/**
 * Merge delayed models from create-index blocks onto base [domains].
 * Delayed models for record types are returned in [MergeSpecTypeModelsResult.recordIndexModels]
 * (opaque index-sort atoms); they are not DomainType entries.
 */
data class MergeSpecTypeModelsResult(
    val domains: Map<String, DomainType>,
    /** Record index-sort name → cfg atom set string like `{"n1", "n2"}`. */
    val recordIndexModels: Map<String, String>,
    val errors: List<CompileError>,
)

fun mergeSpecTypeModels(
    typeModels: List<TypeModelNode>,
    domains: Map<String, DomainType>,
    recordNames: Set<String>,
): MergeSpecTypeModelsResult {
    val out = domains.toMutableMap()
    val recordModels = linkedMapOf<String, String>()
    val errors = mutableListOf<CompileError>()
    typeModels.forEach { model ->
        // Reserved create-index concurrency bound — not a domain type.
        if (model.name() == DYN_PROC_MODEL) return@forEach
        val name = model.name()
        if (name in recordNames) {
            when (val built = buildDomainModel(model, null, recordNames)) {
                is DomainModelBuildResult.Ok -> {
                    val cfg = "{${built.type.cfgElements!!.joinToString(", ")}}"
                    val prev = recordModels[name]
                    if (prev != null && prev != cfg) {
                        errors += OneLocCompileError(
                            model.programLocation(),
                            "Conflicting delayed models for type \"$name\"",
                        )
                    } else {
                        recordModels[name] = cfg
                    }
                }
                is DomainModelBuildResult.Failed -> errors += built.errors
            }
            return@forEach
        }
        when (val built = buildDomainModel(model, out[name], recordNames)) {
            is DomainModelBuildResult.Ok -> {
                val prev = out[name]
                if (prev?.hasModel == true && prev.cfgElements != built.type.cfgElements) {
                    errors += OneLocCompileError(
                        model.programLocation(),
                        "Conflicting delayed models for type \"$name\"",
                    )
                } else {
                    out[name] = built.type
                }
            }
            is DomainModelBuildResult.Failed -> errors += built.errors
        }
    }
    return MergeSpecTypeModelsResult(out, recordModels, errors)
}

/** Collect type names used as domains in a type expression tree (for missing-model checks). */
fun collectUsedDomainNames(type: Type, into: MutableSet<String>) {
    when (type) {
        is DomainType -> into += type.name
        is ListType -> collectUsedDomainNames(type.elementType, into)
        is SetType -> collectUsedDomainNames(type.elementType, into)
        is MapType -> {
            collectUsedDomainNames(type.keyType, into)
            collectUsedDomainNames(type.valueType, into)
        }
        is ObjClassType -> type.fields.forEach { collectUsedDomainNames(it.type, into) }
        else -> {}
    }
}

fun validateSpecDomainModels(
    usedNames: Set<String>,
    domains: Map<String, DomainType>,
    loc: ProgramLoc,
): List<CompileError> {
    val errors = mutableListOf<CompileError>()
    usedNames.forEach { name ->
        val domain = domains[name] ?: return@forEach
        if (domain.kind == DomainKind.Uninterpreted && !domain.hasModel) {
            errors += OneLocCompileError(
                loc,
                "uninterpreted type \"$name\" requires a delayed model for this spec compile",
            )
        }
    }
    return errors
}

private fun builtinCarrierFromTypeExpr(expr: TypeExpr): Type? = when (expr) {
    is TypeExpr.Simple -> when (expr.name) {
        "Boolean" -> boolType
        "Int" -> intType
        "String" -> stringType
        else -> null
    }
    else -> null
}
