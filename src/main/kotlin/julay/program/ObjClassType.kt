package julay.program

import com.microsoft.z3.*

// Z3 mkDatatypeSort artifacts for one o-class in a given Context.
// sort: the record's Z3 sort; constructorDecl: mk-Name(...);
// accessors: field getters in declaration order (e.g. x, y for Point).
class JulangDatatypeMetadata(
    val sort: DatatypeSort<*>,
    val constructorDecl: FuncDecl<*>,
    val accessors: Array<FuncDecl<*>>,
)

class ObjClassType(
    val name: String,
    val fields: List<Variable>,
    private val valueToZ3: (Value, Context) -> Expr<*>,
    private val valueFromZ3: (Expr<*>) -> Any,
) : Type {
    private val objClassCtx = Context()
    private val metadata : JulangDatatypeMetadata

    init {
        val fieldNames = fields.map { it.name }.toTypedArray()
        val fieldSorts = fields.map { field -> z3SortForField(field.type, objClassCtx) }.toTypedArray()
        val constructor = objClassCtx.mkConstructor<Any>(
            "mk-$name",
            "is-$name",
            fieldNames,
            fieldSorts,
            null,
        )
        val sort: DatatypeSort<*> = objClassCtx.mkDatatypeSort(name, arrayOf(constructor))
        metadata = JulangDatatypeMetadata(
            sort = sort,
            constructorDecl = constructor.ConstructorDecl(),
            accessors = constructor.accessorDecls,
        )
    }

    override fun toZ3Expr(variable: Variable, ctx: Context): Expr<*> {
        return ctx.mkConst(variable.name, metadata.sort.translate(ctx))
    }

    override fun toZ3Expr(value: Value, ctx: Context): Expr<*> =
        valueToZ3(value, ctx)

    override fun fromZ3Expr(expr: Expr<*>): Any =
        valueFromZ3(expr)

    override fun isOfType(obj: Any): Boolean = obj.javaClass.simpleName == name

    override fun toString(): String = name

    override fun equals(other: Any?): Boolean = other is ObjClassType && other.name == name && other.fields == fields

    override fun hashCode(): Int = name.hashCode()

    fun sort(ctx: Context): DatatypeSort<*> = metadataFor(ctx).sort

    /** Constructor FuncDecl translated into [ctx] (callers apply field exprs). */
    fun constructorDecl(ctx: Context): FuncDecl<*> = metadataFor(ctx).constructorDecl

    /** Field accessor FuncDecl translated into [ctx] (callers apply the record expr). */
    fun accessor(ctx: Context, fieldIndex: Int): FuncDecl<*> = metadataFor(ctx).accessors[fieldIndex]

    /** Home-context constructor decl (for fromZ3 deconstruction without a target Context). */
    fun homeConstructorDecl(): FuncDecl<*> = metadata.constructorDecl

    /** Home-context accessor decl (for fromZ3 deconstruction without a target Context). */
    fun homeAccessor(fieldIndex: Int): FuncDecl<*> = metadata.accessors[fieldIndex]

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

    private fun metadataFor(ctx: Context): JulangDatatypeMetadata {
        if (ctx === objClassCtx) {
            return metadata
        }
        @Suppress("UNCHECKED_CAST")
        return JulangDatatypeMetadata(
            sort = metadata.sort.translate(ctx) as DatatypeSort<*>,
            constructorDecl = metadata.constructorDecl.translate(ctx),
            accessors = metadata.accessors.map { it.translate(ctx) }.toTypedArray(),
        )
    }

    private fun z3SortForField(type: Type, ctx: Context): Sort = when (type) {
        is BoolType -> ctx.boolSort
        is IntType -> ctx.intSort
        is StringType -> ctx.stringSort
        is ObjClassType -> type.metadataFor(ctx).sort
        else -> throw RuntimeException("Invalid field type for Z3 datatype: $type")
    }

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
                    throw RuntimeException("Unknown field \"$segment\" on o-class ${objType.name}")
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
    is StringType -> "String"
    is ObjClassType -> name
    else -> throw RuntimeException("Invalid type: $this")
}

fun Type.toCodegenTypeVal(): String = when (this) {
    is BoolType -> "boolType"
    is IntType -> "intType"
    is StringType -> "stringType"
    is ObjClassType -> objClassTypeValName(name)
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
