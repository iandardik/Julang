// ObjClassType lives in julay.compiler (not julay.program) because o-class types are used only
// for static type checking and codegen. At runtime, state and action values are desugared to
// primitive types; julay.program is reserved for runtime support code.
package julay.compiler

import com.microsoft.z3.Context
import com.microsoft.z3.Expr
import com.microsoft.z3.Model
import julay.program.*

fun objClassTypeValName(className: String): String =
    className.replaceFirstChar { it.lowercase() } + "Type"

fun String.toKotlinIdent(): String =
    if (contains('$')) "`$this`" else this

fun String.escapeKotlinStringLiteral(): String =
    replace("\\", "\\\\").replace("$", "\\$")

fun Type.toKotlinTypeString(): String = when (this) {
    is BoolType -> "Boolean"
    is IntType -> "Int"
    is StringType -> "String"
    is ObjClassType -> name
    else -> throw RuntimeException("Invalid type: $this")
}

sealed interface FieldPathResult {
    data class Resolved(val type: Type, val relPath: String) : FieldPathResult
    data class Error(val message: String) : FieldPathResult
}

fun resolveFieldPath(rootType: Type, path: List<String>): FieldPathResult {
    if (path.isEmpty()) {
        return FieldPathResult.Error("Expected at least one field name in field access")
    }
    var current: Type = rootType
    val pathParts = mutableListOf<String>()
    for (segment in path) {
        if (current !is ObjClassType) {
            return FieldPathResult.Error("Cannot access field \"$segment\" on non o-class type $current")
        }
        val field = current.fields.find { it.name == segment }
        if (field == null) {
            return FieldPathResult.Error("Unknown field \"$segment\" on o-class ${current.name}")
        }
        pathParts.add(segment)
        current = field.type
    }
    return FieldPathResult.Resolved(current, pathParts.joinToString("$"))
}

fun Type.toCodegenTypeVal(): String = when (this) {
    is BoolType -> "boolType"
    is IntType -> "intType"
    is StringType -> "stringType"
    is ObjClassType -> objClassTypeValName(name)
    else -> throw RuntimeException("Invalid type: $this")
}

data class ObjClassType(
    val name: String,
    val fields: List<Variable>,
) : Type {
    fun collectRelativeFlatFields(prefix: String = ""): List<Pair<String, Type>> =
        fields.flatMap { field ->
            val path = if (prefix.isEmpty()) field.name else "$prefix$${field.name}"
            when (val fieldType = field.type) {
                is ObjClassType -> fieldType.collectRelativeFlatFields(path)
                else -> listOf(path to fieldType)
            }
        }

    fun flatVarName(varName: String, relPath: String): String = combineRelPath(varName, relPath)

    fun fieldZ3Guard(varName: String, relPath: String, fieldType: Type, isArg: Boolean): String {
        val flatName = flatVarName(varName, relPath)
        if (isArg) {
            val escaped = flatName.escapeKotlinStringLiteral()
            return when (fieldType) {
                is BoolType -> "ctx.mkBoolConst(\"$escaped\")"
                is IntType -> "ctx.mkIntConst(\"$escaped\")"
                is StringType -> "ctx.mkStringConst(\"$escaped\")"
                else -> throw RuntimeException("Invalid field type in o-class guard: $fieldType")
            }
        }
        return when (fieldType) {
            is BoolType -> "ctx.mkBool(${flatName.toKotlinIdent()})"
            is IntType -> "ctx.mkInt(${flatName.toKotlinIdent()})"
            is StringType -> "ctx.mkString(${flatName.toKotlinIdent()})"
            else -> throw RuntimeException("Invalid field type in o-class guard: $fieldType")
        }
    }

    fun objClassEqualityZ3(
        lhsSymbol: String,
        rhsSymbol: String,
        lhsIsArg: Boolean,
        rhsIsArg: Boolean,
        lhsRelPath: String = "",
        rhsRelPath: String = "",
    ): String {
        val eqs = collectRelativeFlatFields().map { (fieldRelPath, fieldType) ->
            val lhsPath = combineRelPath(lhsRelPath, fieldRelPath)
            val rhsPath = combineRelPath(rhsRelPath, fieldRelPath)
            val lhsZ3 = fieldZ3Guard(lhsSymbol, lhsPath, fieldType, lhsIsArg)
            val rhsZ3 = fieldZ3Guard(rhsSymbol, rhsPath, fieldType, rhsIsArg)
            "ctx.mkEq($lhsZ3,$rhsZ3)"
        }
        return if (eqs.size == 1) eqs[0] else "ctx.mkAnd(${eqs.joinToString(", ")})"
    }

    fun combineRelPath(prefix: String, suffix: String): String =
        if (prefix.isEmpty()) suffix else "$prefix$$suffix"

    fun flattenArgAssignments(
        argName: String,
        model: Model,
        ctx: Context,
    ): Map<Variable, Value> =
        collectRelativeFlatFields().associate { (relPath, fieldType) ->
            val flatName = flatVarName(argName, relPath)
            val z3Var = Variable(flatName, fieldType)
            val z3Value = model.eval(fieldType.toZ3Expr(z3Var, ctx), true)
            z3Var to Value(z3Value, fieldType)
        }

    override fun toZ3Expr(variable: Variable, ctx: Context): Expr<*> {
        throw RuntimeException("Cannot create a single Z3 expression for o-class variable ${variable.name}")
    }

    override fun toZ3Expr(value: Value, ctx: Context): Expr<*> {
        throw RuntimeException("Cannot create a single Z3 expression for o-class value")
    }

    override fun fromZ3Expr(expr: Expr<*>): Any {
        throw RuntimeException("fromZ3Expr is not supported for o-class type $name")
    }

    override fun isOfType(obj: Any): Boolean = false

    override fun toString(): String = name

    override fun equals(other: Any?): Boolean = other is ObjClassType && other.name == name && other.fields == fields

    override fun hashCode(): Int = name.hashCode()
}

fun Type.flattenStateVariables(varName: String): List<Variable> = when (this) {
    is ObjClassType -> collectRelativeFlatFields().map { (relPath, fieldType) ->
        Variable(flatVarName(varName, relPath), fieldType)
    }
    else -> listOf(Variable(varName, this))
}
