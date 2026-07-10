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

    override fun toZ3Expr(value: Value, ctx: Context): Expr<*> {
        return kotlinObjClassToZ3(ctx, value.value)
    }

    override fun fromZ3Expr(expr: Expr<*>): Any {
        specializedFromZ3?.let { return it(expr) }
        val fieldExprs = deconstructFieldExprs(expr, metadata)
        val fieldValues = fields.mapIndexed { index, field ->
            when (val fieldType = field.type) {
                is ObjClassType -> fieldType.fromZ3Expr(fieldExprs[index])
                else -> fieldType.fromZ3Expr(fieldExprs[index])
            }
        }
        return instantiateDataClass(fieldValues)
    }

    override fun isOfType(obj: Any): Boolean = obj.javaClass.simpleName == name

    override fun toString(): String = name

    override fun equals(other: Any?): Boolean = other is ObjClassType && other.name == name && other.fields == fields

    override fun hashCode(): Int = name.hashCode()

    fun sort(ctx: Context): DatatypeSort<*> = metadataFor(ctx).sort

    fun mkConstructorZ3(ctx: Context, vararg fieldZ3: Expr<*>): Expr<*> {
        val meta = metadataFor(ctx)
        return meta.constructorDecl.apply(*fieldZ3) as Expr<*>
    }

    fun fieldAccessZ3(ctx: Context, recordExpr: Expr<*>, fieldIndex: Int): Expr<*> {
        val meta = metadataFor(ctx)
        return meta.accessors[fieldIndex].apply(recordExpr) as Expr<*>
    }

    // Optional codegen-installed converters that avoid reflection for known o-classes.
    private var specializedToZ3: ((Context, Any) -> Expr<*>)? = null
    private var specializedFromZ3: ((Expr<*>) -> Any)? = null

    fun installConverters(
        toZ3: (Context, Any) -> Expr<*>,
        fromZ3: (Expr<*>) -> Any,
    ) {
        specializedToZ3 = toZ3
        specializedFromZ3 = fromZ3
    }

    // Expects value to be a Kotlin data class instance for this o-class (e.g. Point(3, 7) for Point).
    fun kotlinObjClassToZ3(ctx: Context, value: Any): Expr<*> {
        specializedToZ3?.let { return it(ctx, value) }
        val meta = metadataFor(ctx)
        val fieldExprs = fields.map { field ->
            val fieldValue = readFieldValue(value, field.name)
            field.type.toZ3Expr(Value(fieldValue, field.type), ctx)
        }
        return meta.constructorDecl.apply(*fieldExprs.toTypedArray()) as Expr<*>
    }

    // Unpack a Z3 record into one expr per field (constructor args when possible).
    fun fieldExprsFromZ3(expr: Expr<*>): Array<out Expr<*>> =
        deconstructFieldExprs(expr, metadata)

    fun literalToZ3Codegen(fieldExprStrs: List<String>): String {
        val args = fieldExprStrs.joinToString(", ")
        return "${objClassTypeValName(name)}.mkConstructorZ3(ctx, $args)"
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

    private fun deconstructFieldExprs(localExpr: Expr<*>, meta: JulangDatatypeMetadata): Array<out Expr<*>> {
        // Prefer constructor args when the term is already (mk-Name ...)
        if (localExpr.isApp && localExpr.funcDecl.name == meta.constructorDecl.name) {
            return localExpr.args
        }
        // otherwise use accessors for symbolic values. Accessors on ground constructors may not simplify to plain numerals.
        return Array(fields.size) { index ->
            meta.accessors[index].apply(localExpr) as Expr<*>
        }
    }

    // Expects value to be a Kotlin data class instance; reads a field via its getter.
    private fun readFieldValue(value: Any, fieldName: String): Any {
        val getter = value.javaClass.methods.first { it.name == "get${fieldName.replaceFirstChar { c -> c.uppercase() }}" }
        return getter.invoke(value)
    }

    private fun instantiateDataClass(fieldValues: List<Any>): Any {
        val clazz = loadDataClass(name)
        val paramTypes = fields.map { field -> kotlinTypeToJavaClass(field.type) }.toTypedArray()
        return clazz.getConstructor(*paramTypes).newInstance(*fieldValues.toTypedArray())
    }

    private fun loadDataClass(className: String): Class<*> = try {
        Class.forName(className)
    } catch (_: ClassNotFoundException) {
        Class.forName("julay.program.$className")
    }

    private fun kotlinTypeToJavaClass(type: Type): Class<*> = when (type) {
        is BoolType -> Boolean::class.javaPrimitiveType!!
        is IntType -> Int::class.javaPrimitiveType!!
        is StringType -> String::class.java
        is ObjClassType -> loadDataClass(type.name)
        else -> throw RuntimeException("Invalid field type: $type")
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
                val typeVal = objClassTypeValName(objType.name)
                expr = "$typeVal.fieldAccessZ3(ctx, $expr, $fieldIndex)"
                currentType = objType.fields[fieldIndex].type
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
