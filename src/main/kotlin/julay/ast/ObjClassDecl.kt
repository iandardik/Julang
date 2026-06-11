package julay.ast

import julay.program.*

/**
 * A fully resolved o-class declaration, ready for code generation.
 *
 * Field types are concrete [Type] values (primitives or [ObjClassType] for nested structs).
 * Produced by [ObjClassRegistry.build] after all cross-references between o-classes have
 * been resolved.
 */
data class ObjClassDecl(
    val name: String,
    val fields: List<Variable>,
    val loc: ProgramLoc,
)

/**
 * An unresolved o-class declaration collected from the AST.
 *
 * Field types are stored as strings (e.g. `"Int"`, `"Point"`) rather than [Type] objects
 * because the AST is built in a single pass and o-classes may reference one another in
 * any order — including forward references to o-classes defined later in the file.
 *
 * We need both [RawObjClassDecl] and [ObjClassDecl] because resolution is a separate phase:
 * [RawObjClassDecl] is what [ObjClassNode.objClassPass] emits from the parse tree, while
 * [ObjClassDecl] is the output of [ObjClassRegistry.build] once every field type name has
 * been looked up, nested structs expanded, and errors (unknown types, cycles) collected.
 * Type checking and codegen consume [ObjClassDecl]; they must not run on raw string types.
 */
data class RawObjClassDecl(
    val name: String,
    val fields: List<Pair<String, String>>,
    val loc: ProgramLoc,
)

sealed interface TypeResolveResult {
    data class Found(val type: Type) : TypeResolveResult
    data object NotFound : TypeResolveResult
}

private sealed interface FieldTypeResolveResult {
    data class Success(val type: Type) : FieldTypeResolveResult
    data object Failed : FieldTypeResolveResult
}

private sealed interface ObjClassResolveResult {
    data class Success(val type: ObjClassType) : ObjClassResolveResult
    data object Failed : ObjClassResolveResult
}

/**
 * Resolves [RawObjClassDecl] entries into [ObjClassType] values.
 *
 * O-classes can nest and refer to each other, so field-type resolution is mutually
 * recursive ([resolveObjClass] calls [resolveFieldType] and vice versa). This helper
 * class exists because Kotlin does not allow forward references between local functions.
 * It also tracks the [resolving] set to detect cyclic nesting (e.g. A contains B, B
 * contains A).
 */
private class ObjClassResolver(
    private val errors: MutableList<CompileError>,
    private val resolved: MutableMap<String, ObjClassType>,
    private val resolving: MutableSet<String>,
    private val declsByName: Map<String, RawObjClassDecl>,
) {
    fun resolveFieldType(typeName: String, loc: ProgramLoc): FieldTypeResolveResult {
        when (typeName) {
            "Boolean" -> return FieldTypeResolveResult.Success(boolType)
            "Int" -> return FieldTypeResolveResult.Success(intType)
            "String" -> return FieldTypeResolveResult.Success(stringType)
        }
        if (typeName in resolved) {
            return FieldTypeResolveResult.Success(resolved.getValue(typeName))
        }
        if (typeName !in declsByName) {
            errors.add(OneLocCompileError(loc, "Unknown type \"$typeName\""))
            return FieldTypeResolveResult.Failed
        }
        return when (val result = resolveObjClass(typeName)) {
            is ObjClassResolveResult.Success -> FieldTypeResolveResult.Success(result.type)
            is ObjClassResolveResult.Failed -> FieldTypeResolveResult.Failed
        }
    }

    fun resolveObjClass(name: String): ObjClassResolveResult {
        if (name in resolved) {
            return ObjClassResolveResult.Success(resolved.getValue(name))
        }
        if (name !in declsByName) {
            return ObjClassResolveResult.Failed
        }
        val raw = declsByName.getValue(name)
        if (name in resolving) {
            errors.add(OneLocCompileError(raw.loc, "Cyclic o-class nesting involving \"$name\""))
            return ObjClassResolveResult.Failed
        }
        resolving.add(name)
        val fields = mutableListOf<Variable>()
        var failed = false
        for ((fieldName, typeName) in raw.fields) {
            when (val fieldResult = resolveFieldType(typeName, raw.loc)) {
                is FieldTypeResolveResult.Success -> fields.add(Variable(fieldName, fieldResult.type))
                is FieldTypeResolveResult.Failed -> failed = true
            }
        }
        resolving.remove(name)
        if (failed) {
            return ObjClassResolveResult.Failed
        }
        val objClassType = ObjClassType(name, fields)
        resolved[name] = objClassType
        return ObjClassResolveResult.Success(objClassType)
    }
}

/**
 * The file-scoped registry of resolved o-class types.
 *
 * Built once per compilation unit from a list of [RawObjClassDecl] via [build]. The
 * registry is threaded through [typePass] so that [VarNode], [ArgNode], struct
 * literals, and field accesses can resolve type names like `"Point"` to [ObjClassType].
 * It also exposes [ObjClassDecl] list for o-class type val codegen in [CodegenPass].
 */
class ObjClassRegistry(
    val types: Map<String, ObjClassType>,
    val decls: List<ObjClassDecl>,
    val errors: List<CompileError>,
) {
    fun resolveTypeName(typeName: String): TypeResolveResult = when (typeName) {
        "Boolean" -> TypeResolveResult.Found(boolType)
        "Int" -> TypeResolveResult.Found(intType)
        "String" -> TypeResolveResult.Found(stringType)
        else -> if (typeName in types) {
            TypeResolveResult.Found(types.getValue(typeName))
        } else {
            TypeResolveResult.NotFound
        }
    }

    companion object {
        val EMPTY = ObjClassRegistry(emptyMap(), emptyList(), emptyList())

        fun build(rawDecls: List<RawObjClassDecl>): ObjClassRegistry {
            val errors = mutableListOf<CompileError>()
            val resolved = mutableMapOf<String, ObjClassType>()
            val resolving = mutableSetOf<String>()
            val declsByName = rawDecls.associateBy { it.name }

            val resolver = ObjClassResolver(errors, resolved, resolving, declsByName)
            rawDecls.forEach { raw ->
                resolver.resolveObjClass(raw.name)
            }

            val decls = rawDecls.flatMap { raw ->
                if (raw.name in resolved) {
                    val type = resolved.getValue(raw.name)
                    listOf(ObjClassDecl(raw.name, type.fields, raw.loc))
                } else {
                    emptyList()
                }
            }
            return ObjClassRegistry(resolved, decls, errors)
        }
    }
}
