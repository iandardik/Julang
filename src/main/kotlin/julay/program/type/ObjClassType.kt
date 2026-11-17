package julay.program.type

import julay.program.Variable

/**
 * Built-in / user obj type (structural fields for typing and emission helpers).
 */
class ObjClassType(
    val name: String,
    val fields: List<Variable>,
) : Type {
    val constructorName: String = "mk-$name"

    override fun isOfType(obj: Any): Boolean = obj.javaClass.simpleName == name

    override fun toString(): String = name

    override fun equals(other: Any?): Boolean =
        other is ObjClassType && other.name == name && other.fields == fields

    override fun hashCode(): Int = name.hashCode()

    /** Legacy Kotlin-codegen helper retained for unused Z3 AST string paths. */
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
