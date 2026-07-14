package julay.program

import io.github.cvc5.Solver
import io.github.cvc5.Sort
import io.github.cvc5.Term
import io.github.cvc5.TermManager
import julay.tools.applySelector

// CVC5 datatype artifacts for one o-class in a given TermManager.
class JulangDatatypeMetadata(
    val sort: Sort,
    val constructorTerm: Term,
    val selectors: Array<Term>,
)

class ObjClassType(
    val name: String,
    val fields: List<Variable>,
    private val valueToSmt: (Value, TermManager) -> Term,
    private val valueFromSmt: (Term, Solver) -> Any,
) : Type {
    /**
     * SMT-LIB datatype for this o-class. Null when fields still contain [TypeVar]
     * (polymorphic templates during type checking).
     */
    val serializedDatatype: SerializedDatatype? =
        if (fields.any { fieldContainsTypeVar(it.type) }) {
            null
        } else {
            buildDatatypeDeclare(
                name,
                "mk-$name",
                fields.map { it.name to it.type.toSmtLibSort() },
            )
        }

    // Polymorphic o-class instantiations (fields still typed as TypeVar) can be
    // constructed during type checking without declaring sorts; concrete types force this.
    private fun metadataFor(tm: TermManager): JulangDatatypeMetadata =
        DatatypeBinder.forTm(tm).objClass(name) {
            val decl = tm.mkDatatypeDecl(name)
            val constructor = tm.mkDatatypeConstructorDecl("mk-$name")
            for (field in fields) {
                constructor.addSelector(field.name, field.type.toSmtSort(tm))
            }
            decl.addConstructor(constructor)
            val sort = tm.mkDatatypeSort(decl)
            val dt = sort.datatype
            val ctor = dt.getConstructor(0)
            JulangDatatypeMetadata(
                sort = sort,
                constructorTerm = ctor.term,
                selectors = Array(fields.size) { i -> ctor.getSelector(i).term },
            )
        }

    override fun toSmtTerm(variable: Variable, tm: TermManager): Term =
        tm.mkConst(metadataFor(tm).sort, variable.name)

    override fun toSmtTerm(value: Value, tm: TermManager): Term =
        valueToSmt(value, tm)

    override fun fromSmtTerm(expr: Term, solver: Solver): Any =
        valueFromSmt(expr, solver)

    override fun isOfType(obj: Any): Boolean = obj.javaClass.simpleName == name

    override fun toString(): String = name

    override fun equals(other: Any?): Boolean = other is ObjClassType && other.name == name && other.fields == fields

    override fun hashCode(): Int = name.hashCode()

    fun sort(tm: TermManager): Sort = metadataFor(tm).sort

    fun constructorTerm(tm: TermManager): Term = metadataFor(tm).constructorTerm

    fun selector(tm: TermManager, fieldIndex: Int): Term = metadataFor(tm).selectors[fieldIndex]

    fun literalToSmtCodegen(fieldExprStrs: List<String>): String {
        val args = fieldExprStrs.joinToString(", ")
        return "${objClassMkFunName(name)}(tm, $args)"
    }

    fun literalToTransit(fieldExprStrs: List<String>): String {
        val args = fields.zip(fieldExprStrs).joinToString(", ") { (field, exprStr) ->
            "${field.name} = $exprStr"
        }
        return "$name($args)"
    }

    companion object {
        fun smtConstString(symbol: String, typeValName: String): String {
            val escaped = symbol.escapeKotlinStringLiteral()
            return "tm.mkConst($typeValName.sort(tm), \"$escaped\")"
        }

        fun kotlinObjClassToSmtString(className: String, varName: String): String {
            return "${objClassToSmtFunName(className)}(tm, $varName)"
        }

        fun fieldAccessSmtCodegen(rootType: ObjClassType, recordExpr: String, fieldPath: List<String>): String {
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
                expr = "${objClassAccessorFunName(objType.name, field.name)}(tm, $expr)"
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

fun objClassToSmtFunName(className: String): String =
    className.replaceFirstChar { it.lowercase() } + "ToSmt"

fun objClassFromSmtFunName(className: String): String =
    className.replaceFirstChar { it.lowercase() } + "FromSmt"

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
    else -> throw RuntimeException("Invalid type: $this")
}

fun Type.toCodegenTypeVal(): String = when (this) {
    is BoolType -> "boolType"
    is IntType -> "intType"
    is RealType -> "realType"
    is StringType -> "stringType"
    is ObjClassType -> objClassTypeValName(name)
    is ListType -> "listType(${elementType.toCodegenTypeVal()})"
    is SetType -> "setType(${elementType.toCodegenTypeVal()})"
    is MapType -> "mapType(${keyType.toCodegenTypeVal()}, ${valueType.toCodegenTypeVal()})"
    is TypeVar -> throw RuntimeException("TypeVar \"$name\" must not reach Kotlin codegen")
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

private fun fieldContainsTypeVar(type: Type): Boolean = when (type) {
    is TypeVar -> true
    is ListType -> fieldContainsTypeVar(type.elementType)
    is SetType -> fieldContainsTypeVar(type.elementType)
    is MapType -> fieldContainsTypeVar(type.keyType) || fieldContainsTypeVar(type.valueType)
    is ObjClassType -> type.fields.any { fieldContainsTypeVar(it.type) }
    else -> false
}
