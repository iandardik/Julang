package julay.program.type

import com.microsoft.z3.*
import julay.program.ContextLocalCache
import julay.program.Value
import julay.program.Variable

// Z3 mkDatatypeSort artifacts for one obj in a given Context.
// sort: the record's Z3 sort; constructorDecl: mk-Name(...);
// accessors: field getters in declaration order (e.g. x, y for Point).
class JulangDatatypeMetadata(
    val sort: DatatypeSort<*>,
    val constructorDecl: FuncDecl<*>,
    val accessors: Array<FuncDecl<*>>,
)

/**
 * Built-in obj type. Datatype metadata is built directly in the caller's [Context]
 * (no per-instance home Context); results are cached per live Context so the datatype is not
 * redefined on every use, without retaining closed Contexts — same pattern as [SetType] / [MapType].
 */
class ObjClassType(
    val name: String,
    val fields: List<Variable>,
    private val valueToZ3: (Value, Context) -> Expr<*>,
    private val valueFromZ3: (Expr<*>, Model) -> Any,
) : Type {
    /**
     * Z3 constructor symbol name (`mk-$name`). Stable across Contexts, so constructor-application
     * matching in `fromZ3` can compare against this string without reading decls from another Context.
     */
    val constructorName: String = "mk-$name"

    // TODO: this shared per-Context cache breaks the rule that interprocess communication
    // must go only through SyncChannel (procs can observe/reuse metadata across contexts).
    // Fix later; kept for now so fixed-name mkDatatypeSort is not redefined within a Context.
    private val metaByCtx = ContextLocalCache<JulangDatatypeMetadata>()

    private fun metadataFor(ctx: Context): JulangDatatypeMetadata =
        metaByCtx.getOrPut(ctx) { buildMetadata(ctx) }

    private fun buildMetadata(ctx: Context): JulangDatatypeMetadata {
        // Polymorphic obj instantiations (fields still typed as TypeVar) must not reach here;
        // concrete monomorphized types force this on first Z3 use.
        val fieldNames = fields.map { it.name }.toTypedArray()
        val fieldSorts = fields.map { field -> z3SortForField(field.type, ctx) }.toTypedArray()
        val constructor = ctx.mkConstructor<Any>(
            constructorName,
            "is-$name",
            fieldNames,
            fieldSorts,
            null,
        )
        val sort: DatatypeSort<*> = ctx.mkDatatypeSort(name, arrayOf(constructor))
        return JulangDatatypeMetadata(
            sort = sort,
            constructorDecl = constructor.ConstructorDecl(),
            accessors = constructor.accessorDecls,
        )
    }

    override fun toZ3Expr(variable: Variable, ctx: Context): Expr<*> {
        return ctx.mkConst(variable.name, metadataFor(ctx).sort)
    }

    override fun toZ3Expr(value: Value, ctx: Context): Expr<*> =
        valueToZ3(value, ctx)

    override fun fromZ3Expr(expr: Expr<*>, model: Model): Any =
        valueFromZ3(expr, model)

    override fun isOfType(obj: Any): Boolean = obj.javaClass.simpleName == name

    override fun toString(): String = name

    override fun equals(other: Any?): Boolean = other is ObjClassType && other.name == name && other.fields == fields

    override fun hashCode(): Int = name.hashCode()

    fun sort(ctx: Context): DatatypeSort<*> = metadataFor(ctx).sort

    /** Constructor FuncDecl in [ctx] (callers apply field exprs). */
    fun constructorDecl(ctx: Context): FuncDecl<*> = metadataFor(ctx).constructorDecl

    /** Field accessor FuncDecl in [ctx] (callers apply the record expr). */
    fun accessor(ctx: Context, fieldIndex: Int): FuncDecl<*> = metadataFor(ctx).accessors[fieldIndex]

    fun literalToZ3Codegen(fieldExprStrs: List<String>): String {
        val args = fieldExprStrs.joinToString(", ")
        return "${objClassMkFunName(name)}(ctx, $args)"
    }

    fun literalToTransit(fieldExprStrs: List<String>): String {
        val args = fields.zip(fieldExprStrs).joinToString(", ") { (field, exprStr) ->
            "${field.name} = $exprStr"
        }
        return "$name($args)"
    }

    private fun z3SortForField(type: Type, ctx: Context): Sort = type.toZ3Sort(ctx)

    companion object {
        fun z3ConstString(symbol: String, typeValName: String): String {
            val escaped = symbol.escapeKotlinStringLiteral()
            return "ctx.mkConst(\"$escaped\", $typeValName.sort(ctx))"
        }

        fun kotlinObjClassToZ3String(className: String, varName: String): String {
            return "${objClassToZ3FunName(className)}(ctx, $varName)"
        }

        fun fieldAccessZ3Codegen(rootType: ObjClassType, recordExpr: String, fieldPath: List<String>): String {
            if (fieldPath.isEmpty()) {
                return recordExpr
            }
            var currentType: Type = rootType
            var expr = recordExpr
            for (segment in fieldPath) {
                val objType = currentType as ObjClassType
                val fieldIndex = objType.fields.indexOfFirst { it.name == segment }
                if (fieldIndex < 0) {
                    throw RuntimeException("Unknown field \"$segment\" on type ${objType.name}")
                }
                val field = objType.fields[fieldIndex]
                expr = "${objClassAccessorFunName(objType.name, field.name)}(ctx, $expr)"
                currentType = field.type
            }
            return expr
        }

        fun fieldAccessTransitString(
            baseSymbol: String,
            fieldPath: List<String>,
            symbolTypes: Map<String, Type>,
            argSymbols: Set<String>,
        ): String {
            val path = fieldPath.joinToString(".")
            if (baseSymbol in argSymbols) {
                val baseType = symbolTypes.getValue(baseSymbol) as ObjClassType
                val typeStr = baseType.toCodegenTypeVal()
                return "((act.lookup(Variable(\"${baseSymbol.escapeKotlinStringLiteral()}\", $typeStr)).value as ${baseType.name}).$path)"
            }
            return "$baseSymbol.$path"
        }
    }
}

fun objClassTypeValName(className: String): String =
    className.replaceFirstChar { it.lowercase() } + "Type"

fun listTypeValName(elementType: Type): String =
    "listType_${julay.compiler.decl.mangleTypeForName(elementType)}"

fun setTypeValName(elementType: Type): String =
    "setType_${julay.compiler.decl.mangleTypeForName(elementType)}"

fun mapTypeValName(keyType: Type, valueType: Type): String =
    "mapType_${julay.compiler.decl.mangleTypeForName(keyType)}_${julay.compiler.decl.mangleTypeForName(valueType)}"

fun objClassToZ3FunName(className: String): String =
    className.replaceFirstChar { it.lowercase() } + "ToZ3"

fun objClassFromZ3FunName(className: String): String =
    className.replaceFirstChar { it.lowercase() } + "FromZ3"

fun objClassMkFunName(className: String): String =
    className.replaceFirstChar { it.lowercase() } + "Mk"

fun objClassAccessorFunName(className: String, fieldName: String): String =
    className.replaceFirstChar { it.lowercase() } + fieldName.replaceFirstChar { it.uppercase() }

fun String.toKotlinIdent(): String = this

fun String.escapeKotlinStringLiteral(): String =
    replace("\\", "\\\\").replace("\"", "\\\"")

fun Type.toKotlinTypeString(): String = when (this) {
    is BoolType -> "Boolean"
    is IntType -> "Int"
    is RealType -> "Double"
    is StringType -> "String"
    is ObjClassType -> name
    is ListType -> "List<${elementType.toKotlinTypeString()}>"
    is SetType -> "Set<${elementType.toKotlinTypeString()}>"
    is MapType -> "Map<${keyType.toKotlinTypeString()}, ${valueType.toKotlinTypeString()}>"
    is TypeVar -> throw RuntimeException("TypeVar \"$name\" must not reach Kotlin codegen")
    is DomainType -> when (kind) {
        DomainKind.Typedef -> carrierType.toKotlinTypeString()
        DomainKind.Uninterpreted -> throw RuntimeException(
            "type \"$name\" must not reach Kotlin codegen (uninterpreted types are specs/TLA+ only)",
        )
    }
    is ProcFunRefType -> "String"
    else -> throw RuntimeException("Invalid type: $this")
}

fun Type.toCodegenTypeVal(): String = when (this) {
    is BoolType -> "boolType"
    is IntType -> "intType"
    is RealType -> "realType"
    is StringType -> "stringType"
    is ObjClassType -> objClassTypeValName(name)
    is ListType -> listTypeValName(elementType)
    is SetType -> setTypeValName(elementType)
    is MapType -> mapTypeValName(keyType, valueType)
    is TypeVar -> throw RuntimeException("TypeVar \"$name\" must not reach Kotlin codegen")
    is DomainType -> when (kind) {
        DomainKind.Typedef -> carrierType.toCodegenTypeVal()
        DomainKind.Uninterpreted -> throw RuntimeException(
            "type \"$name\" must not reach Kotlin codegen (uninterpreted types are specs/TLA+ only)",
        )
    }
    is ProcFunRefType -> "stringType"
    else -> throw RuntimeException("Invalid type: $this")
}

fun copyAssignmentString(
    rootVar: String,
    rootType: Type,
    fieldPath: List<String>,
    rhs: String,
): String {
    if (fieldPath.isEmpty()) {
        return "$rootVar = $rhs"
    }
    val objType = rootType as ObjClassType
    return "$rootVar = ${copyExprString(rootVar, objType, fieldPath, rhs)}"
}

private fun copyExprString(
    currentVar: String,
    currentType: ObjClassType,
    fieldPath: List<String>,
    rhs: String,
): String {
    val fieldName = fieldPath.first()
    if (fieldPath.size == 1) {
        return "$currentVar.copy($fieldName = $rhs)"
    }
    val fieldType = currentType.fields.first { it.name == fieldName }.type as ObjClassType
    val inner = copyExprString("$currentVar.$fieldName", fieldType, fieldPath.drop(1), rhs)
    return "$currentVar.copy($fieldName = $inner)"
}

fun transitRootVar(transitKey: String): String = transitKey.substringBefore('.')
