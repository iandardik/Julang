package julay.compiler.decl

import julay.compiler.*
import julay.program.*

/**
 * A fully resolved o-class declaration, ready for code generation.
 *
 * Field types are concrete [Type] values (primitives or [ObjClassType] for nested structs).
 * Produced by [ObjClassRegistry.build] after all cross-references between o-classes have
 * been resolved. Includes nullary o-classes and monomorphized parametric instantiations.
 */
data class ObjClassDecl(
    val name: String,
    val fields: List<Variable>,
    val loc: ProgramLoc,
)

/**
 * An unresolved o-class declaration collected from the AST.
 *
 * Field types are [TypeExpr] trees. Parametric o-classes carry [typeParams]; nullary ones
 * have an empty list. Resolution / monomorphization happens in [ObjClassRegistry].
 */
data class RawObjClassDecl(
    val name: String,
    val typeParams: List<String>,
    val fields: List<Pair<String, TypeExpr>>,
    val loc: ProgramLoc,
)

sealed interface TypeResolveResult {
    data class Found(val type: Type) : TypeResolveResult
    data class Error(val message: String) : TypeResolveResult
}

internal sealed interface FieldTypeResolveResult {
    data class Success(val type: Type) : FieldTypeResolveResult
    data class Failed(val message: String) : FieldTypeResolveResult
}

internal sealed interface ObjClassResolveResult {
    data class Success(val type: ObjClassType) : ObjClassResolveResult
    data class Failed(val message: String) : ObjClassResolveResult
}

fun mangleTypeForName(type: Type): String = when (type) {
    is BoolType -> "Boolean"
    is IntType -> "Int"
    is RealType -> "Real"
    is StringType -> "String"
    is ChannelType -> "Channel_${type.actionName}"
    is ObjClassType -> type.name
    is ListType -> "List_${mangleTypeForName(type.elementType)}"
    is SetType -> "Set_${mangleTypeForName(type.elementType)}"
    is MapType -> "Map_${mangleTypeForName(type.keyType)}_${mangleTypeForName(type.valueType)}"
    is TypeVar -> type.name
    else -> throw RuntimeException("Cannot mangle type $type")
}

fun mangleInstantiation(ctor: String, argTypes: List<Type>): String =
    (listOf(ctor) + argTypes.map { mangleTypeForName(it) }).joinToString("_")

fun Type.containsTypeVar(): Boolean = when (this) {
    is TypeVar -> true
    is ObjClassType -> fields.any { it.type.containsTypeVar() }
    is ListType -> elementType.containsTypeVar()
    is SetType -> elementType.containsTypeVar()
    is MapType -> keyType.containsTypeVar() || valueType.containsTypeVar()
    else -> false
}

/**
 * Resolves [RawObjClassDecl] entries into concrete [ObjClassType] values, including
 * on-demand monomorphization of parametric templates.
 */
internal class ObjClassResolver(
    private val resolved: MutableMap<String, ObjClassType>,
    private val resolving: MutableSet<String>,
    private val declsByName: Map<String, RawObjClassDecl>,
    private val instantiationLocs: MutableMap<String, ProgramLoc>,
) {
    fun resolveTypeExpr(
        expr: TypeExpr,
        typeParamEnv: Map<String, Type> = emptyMap(),
    ): FieldTypeResolveResult {
        return when (expr) {
            is TypeExpr.Simple -> resolveSimple(expr.name, typeParamEnv)
            is TypeExpr.Parametric -> resolveParametric(expr, typeParamEnv)
        }
    }

    private fun resolveSimple(
        name: String,
        typeParamEnv: Map<String, Type>,
    ): FieldTypeResolveResult {
        typeParamEnv[name]?.let { return FieldTypeResolveResult.Success(it) }
        when (name) {
            "Boolean" -> return FieldTypeResolveResult.Success(boolType)
            "Int" -> return FieldTypeResolveResult.Success(intType)
            "Real" -> return FieldTypeResolveResult.Success(realType)
            "String" -> return FieldTypeResolveResult.Success(stringType)
            "Channel" -> return FieldTypeResolveResult.Failed("Type \"Channel\" expects 1 type argument")
            "List" -> return FieldTypeResolveResult.Failed("Type \"List\" expects 1 type argument")
            "Set" -> return FieldTypeResolveResult.Failed("Type \"Set\" expects 1 type argument")
            "Map" -> return FieldTypeResolveResult.Failed("Type \"Map\" expects 2 type arguments")
        }
        ObjClassBuiltinRegistry.lookup(name)?.let {
            return FieldTypeResolveResult.Success(it)
        }
        if (name in resolved) {
            return FieldTypeResolveResult.Success(resolved.getValue(name))
        }
        val raw = declsByName[name]
        if (raw == null) {
            return FieldTypeResolveResult.Failed("Unknown type \"$name\"")
        }
        if (raw.typeParams.isNotEmpty()) {
            return FieldTypeResolveResult.Failed(
                "Type \"$name\" expects ${raw.typeParams.size} type argument(s)",
            )
        }
        return when (val result = resolveNullaryObjClass(name)) {
            is ObjClassResolveResult.Success -> FieldTypeResolveResult.Success(result.type)
            is ObjClassResolveResult.Failed -> FieldTypeResolveResult.Failed(result.message)
        }
    }

    private fun resolveParametric(
        expr: TypeExpr.Parametric,
        typeParamEnv: Map<String, Type>,
    ): FieldTypeResolveResult {
        val ctor = expr.ctor
        if (ctor == "Channel") {
            if (expr.args.size != 1) {
                return FieldTypeResolveResult.Failed("Type \"Channel\" expects 1 type argument")
            }
            val actArg = expr.args[0]
            if (actArg !is TypeExpr.Simple) {
                return FieldTypeResolveResult.Failed(
                    "Expected Channel type argument to be an action name",
                )
            }
            return FieldTypeResolveResult.Success(channelType(actArg.name))
        }
        if (ctor == "List") {
            if (expr.args.size != 1) {
                return FieldTypeResolveResult.Failed("Type \"List\" expects 1 type argument")
            }
            return when (val argResult = resolveTypeExpr(expr.args[0], typeParamEnv)) {
                is FieldTypeResolveResult.Success ->
                    FieldTypeResolveResult.Success(listType(argResult.type))
                is FieldTypeResolveResult.Failed -> argResult
            }
        }
        if (ctor == "Set") {
            if (expr.args.size != 1) {
                return FieldTypeResolveResult.Failed("Type \"Set\" expects 1 type argument")
            }
            return when (val argResult = resolveTypeExpr(expr.args[0], typeParamEnv)) {
                is FieldTypeResolveResult.Success ->
                    FieldTypeResolveResult.Success(setType(argResult.type))
                is FieldTypeResolveResult.Failed -> argResult
            }
        }
        if (ctor == "Map") {
            if (expr.args.size != 2) {
                return FieldTypeResolveResult.Failed("Type \"Map\" expects 2 type arguments")
            }
            return when (val keyResult = resolveTypeExpr(expr.args[0], typeParamEnv)) {
                is FieldTypeResolveResult.Success -> when (
                    val valResult = resolveTypeExpr(expr.args[1], typeParamEnv)
                ) {
                    is FieldTypeResolveResult.Success ->
                        FieldTypeResolveResult.Success(mapType(keyResult.type, valResult.type))
                    is FieldTypeResolveResult.Failed -> valResult
                }
                is FieldTypeResolveResult.Failed -> keyResult
            }
        }
        val raw = declsByName[ctor]
        if (raw == null) {
            if (ctor in resolved) {
                return FieldTypeResolveResult.Failed("Type \"$ctor\" does not take type arguments")
            }
            return FieldTypeResolveResult.Failed("Unknown type \"$ctor\"")
        }
        val arity = raw.typeParams.size
        if (arity == 0) {
            return FieldTypeResolveResult.Failed("Type \"$ctor\" does not take type arguments")
        }
        val argExprs = expr.args
        if (argExprs.size != arity) {
            return FieldTypeResolveResult.Failed(
                "Type \"$ctor\" expects $arity type argument(s)",
            )
        }
        val argTypes = mutableListOf<Type>()
        for (argExpr in argExprs) {
            when (val argResult = resolveTypeExpr(argExpr, typeParamEnv)) {
                is FieldTypeResolveResult.Success -> argTypes.add(argResult.type)
                is FieldTypeResolveResult.Failed -> return argResult
            }
        }
        return when (val inst = instantiate(raw, argTypes, SourceLoc(Pair(0, 0)))) {
            is ObjClassResolveResult.Success -> FieldTypeResolveResult.Success(inst.type)
            is ObjClassResolveResult.Failed -> FieldTypeResolveResult.Failed(inst.message)
        }
    }

    fun resolveNullaryObjClass(name: String): ObjClassResolveResult {
        if (name in resolved) {
            return ObjClassResolveResult.Success(resolved.getValue(name))
        }
        val raw = declsByName[name] ?: return ObjClassResolveResult.Failed("Unknown type \"$name\"")
        if (raw.typeParams.isNotEmpty()) {
            return ObjClassResolveResult.Failed(
                "Type \"$name\" expects ${raw.typeParams.size} type argument(s)",
            )
        }
        return buildConcrete(name, raw, emptyMap(), raw.loc)
    }

    fun instantiate(
        raw: RawObjClassDecl,
        argTypes: List<Type>,
        loc: ProgramLoc,
    ): ObjClassResolveResult {
        if (argTypes.size != raw.typeParams.size) {
            return ObjClassResolveResult.Failed(
                "Type \"${raw.name}\" expects ${raw.typeParams.size} type argument(s)",
            )
        }
        val mangled = mangleInstantiation(raw.name, argTypes)
        if (mangled in resolved) {
            return ObjClassResolveResult.Success(resolved.getValue(mangled))
        }
        val typeParamEnv = raw.typeParams.zip(argTypes).toMap()
        val typeExprSubst = raw.typeParams.zip(argTypes).mapNotNull { (param, type) ->
            if (type is TypeVar) null else param to typeToTypeExpr(type)
        }.toMap()
        return buildConcrete(mangled, raw, typeParamEnv, loc, typeExprSubst)
    }

    private fun typeToTypeExpr(type: Type): TypeExpr = when (type) {
        is BoolType -> TypeExpr.Simple("Boolean")
        is IntType -> TypeExpr.Simple("Int")
        is RealType -> TypeExpr.Simple("Real")
        is StringType -> TypeExpr.Simple("String")
        is ChannelType -> TypeExpr.Parametric("Channel", listOf(TypeExpr.Simple(type.actionName)))
        is TypeVar -> TypeExpr.Simple(type.name)
        is ObjClassType -> TypeExpr.Simple(type.name)
        is ListType -> TypeExpr.Parametric("List", listOf(typeToTypeExpr(type.elementType)))
        is SetType -> TypeExpr.Parametric("Set", listOf(typeToTypeExpr(type.elementType)))
        is MapType -> TypeExpr.Parametric(
            "Map",
            listOf(typeToTypeExpr(type.keyType), typeToTypeExpr(type.valueType)),
        )
        else -> throw RuntimeException("Cannot convert type $type to TypeExpr")
    }

    private fun buildConcrete(
        concreteName: String,
        raw: RawObjClassDecl,
        typeParamEnv: Map<String, Type>,
        loc: ProgramLoc,
        typeExprSubst: Map<String, TypeExpr> = emptyMap(),
    ): ObjClassResolveResult {
        if (concreteName in resolving) {
            return ObjClassResolveResult.Failed("Cyclic o-class nesting involving \"${raw.name}\"")
        }
        resolving.add(concreteName)
        val fields = mutableListOf<Variable>()
        for ((fieldName, fieldTypeExpr) in raw.fields) {
            val substituted = if (typeExprSubst.isEmpty()) {
                fieldTypeExpr
            } else {
                substituteTypeExpr(fieldTypeExpr, typeExprSubst)
            }
            when (val fieldResult = resolveTypeExpr(substituted, typeParamEnv)) {
                is FieldTypeResolveResult.Success -> fields.add(Variable(fieldName, fieldResult.type))
                is FieldTypeResolveResult.Failed -> {
                    resolving.remove(concreteName)
                    return ObjClassResolveResult.Failed(fieldResult.message)
                }
            }
        }
        resolving.remove(concreteName)
        val objClassType = ObjClassType(
            concreteName,
            fields,
            { _, _ -> throw RuntimeException("valueToZ3 not available on compiler-resolved ObjClassType $concreteName") },
            { _, _ -> throw RuntimeException("valueFromZ3 not available on compiler-resolved ObjClassType $concreteName") },
        )
        resolved[concreteName] = objClassType
        instantiationLocs[concreteName] = loc
        return ObjClassResolveResult.Success(objClassType)
    }
}

/**
 * The compilation-unit registry of resolved o-class types (nullary + monomorphized).
 */
class ObjClassRegistry private constructor(
    private val resolvedTypes: MutableMap<String, ObjClassType>,
    val errors: List<CompileError>,
    private val resolver: ObjClassResolver?,
    private val declsByName: Map<String, RawObjClassDecl>,
    private val instantiationLocs: MutableMap<String, ProgramLoc>,
) {
    val types: Map<String, ObjClassType>
        get() = resolvedTypes.toMap()

    val decls: List<ObjClassDecl>
        get() = concreteDecls()

    fun resolveTypeName(typeName: String): TypeResolveResult =
        resolveTypeExpr(TypeExpr.Simple(typeName))

    fun resolveTypeExpr(
        expr: TypeExpr,
        typeParamEnv: Map<String, Type> = emptyMap(),
        loc: ProgramLoc = SourceLoc(Pair(0, 0)),
    ): TypeResolveResult {
        val r = resolver
        if (r == null) {
            return when (expr) {
                is TypeExpr.Simple -> when (expr.name) {
                    "Boolean" -> TypeResolveResult.Found(boolType)
                    "Int" -> TypeResolveResult.Found(intType)
                    "String" -> TypeResolveResult.Found(stringType)
                    "Channel" -> TypeResolveResult.Error("Type \"Channel\" expects 1 type argument")
                    else -> typeParamEnv[expr.name]?.let { TypeResolveResult.Found(it) }
                        ?: ObjClassBuiltinRegistry.lookup(expr.name)?.let { TypeResolveResult.Found(it) }
                        ?: resolvedTypes[expr.name]?.let { TypeResolveResult.Found(it) }
                        ?: TypeResolveResult.Error("Unknown type \"${expr.name}\"")
                }
                is TypeExpr.Parametric -> {
                    when (expr.ctor) {
                        "Channel" -> {
                            if (expr.args.size != 1) {
                                return TypeResolveResult.Error("Type \"Channel\" expects 1 type argument")
                            }
                            val actArg = expr.args[0]
                            if (actArg !is TypeExpr.Simple) {
                                return TypeResolveResult.Error(
                                    "Expected Channel type argument to be an action name",
                                )
                            }
                            return TypeResolveResult.Found(channelType(actArg.name))
                        }
                        "List" -> {
                            if (expr.args.size != 1) {
                                return TypeResolveResult.Error("Type \"List\" expects 1 type argument")
                            }
                            return when (val arg = resolveTypeExpr(expr.args[0], typeParamEnv, loc)) {
                                is TypeResolveResult.Found -> TypeResolveResult.Found(listType(arg.type))
                                is TypeResolveResult.Error -> arg
                            }
                        }
                        "Set" -> {
                            if (expr.args.size != 1) {
                                return TypeResolveResult.Error("Type \"Set\" expects 1 type argument")
                            }
                            return when (val arg = resolveTypeExpr(expr.args[0], typeParamEnv, loc)) {
                                is TypeResolveResult.Found -> TypeResolveResult.Found(setType(arg.type))
                                is TypeResolveResult.Error -> arg
                            }
                        }
                        "Map" -> {
                            if (expr.args.size != 2) {
                                return TypeResolveResult.Error("Type \"Map\" expects 2 type arguments")
                            }
                            return when (val key = resolveTypeExpr(expr.args[0], typeParamEnv, loc)) {
                                is TypeResolveResult.Found -> when (
                                    val value = resolveTypeExpr(expr.args[1], typeParamEnv, loc)
                                ) {
                                    is TypeResolveResult.Found ->
                                        TypeResolveResult.Found(mapType(key.type, value.type))
                                    is TypeResolveResult.Error -> value
                                }
                                is TypeResolveResult.Error -> key
                            }
                        }
                        else -> TypeResolveResult.Error("Cannot resolve parametric type \"$expr\" without registry resolver")
                    }
                }
            }
        }
        return when (val result = r.resolveTypeExpr(expr, typeParamEnv)) {
            is FieldTypeResolveResult.Success -> TypeResolveResult.Found(result.type)
            is FieldTypeResolveResult.Failed -> TypeResolveResult.Error(result.message)
        }
    }

    fun templateArity(name: String): Int? = declsByName[name]?.typeParams?.size

    fun rawDecl(name: String): RawObjClassDecl? = declsByName[name]

    /** Concrete decls for codegen: excludes instantiations that still contain type variables. */
    fun concreteDecls(): List<ObjClassDecl> =
        resolvedTypes.entries
            .filter { !it.value.containsTypeVar() }
            .map { (name, type) ->
                val loc = declsByName[name]?.loc
                    ?: instantiationLocs[name]
                    ?: declsByName.entries.firstOrNull { name.startsWith(it.key + "_") }?.value?.loc
                    ?: SourceLoc(Pair(0, 0))
                ObjClassDecl(name, type.fields, loc)
            }

    companion object {
        val EMPTY = ObjClassRegistry(
            mutableMapOf(),
            emptyList(),
            null,
            emptyMap(),
            mutableMapOf(),
        )

        fun build(rawDecls: List<RawObjClassDecl>): ObjClassRegistry {
            val errors = mutableListOf<CompileError>()
            for (raw in rawDecls) {
                if (ObjClassBuiltinRegistry.isBuiltin(raw.name)) {
                    errors.add(
                        OneLocCompileError(
                            raw.loc,
                            "o-class \"${raw.name}\" conflicts with a builtin o-class type",
                        ),
                    )
                }
                val dupParams = raw.typeParams.groupingBy { it }.eachCount().filter { it.value > 1 }.keys
                for (dup in dupParams) {
                    errors.add(
                        OneLocCompileError(raw.loc, "Duplicate type parameter \"$dup\" on o-class \"${raw.name}\""),
                    )
                }
            }
            val resolved = mutableMapOf<String, ObjClassType>()
            val resolving = mutableSetOf<String>()
            val declsByName = rawDecls.associateBy { it.name }
            val instantiationLocs = mutableMapOf<String, ProgramLoc>()

            val resolver = ObjClassResolver(resolved, resolving, declsByName, instantiationLocs)
            rawDecls.filter { it.typeParams.isEmpty() }.forEach { raw ->
                when (val result = resolver.resolveNullaryObjClass(raw.name)) {
                    is ObjClassResolveResult.Success -> {}
                    is ObjClassResolveResult.Failed ->
                        errors.add(OneLocCompileError(raw.loc, result.message))
                }
            }

            return ObjClassRegistry(resolved, errors, resolver, declsByName, instantiationLocs)
        }
    }
}
