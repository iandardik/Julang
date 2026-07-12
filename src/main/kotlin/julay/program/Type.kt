package julay.program

import com.microsoft.z3.Context
import com.microsoft.z3.Expr
import com.microsoft.z3.IntNum
import com.microsoft.z3.Model
import com.microsoft.z3.RatNum
import julay.tools.mkStringConst

val boolType = BoolType()
val intType = IntType()
val realType = RealType()
val stringType = StringType()
val baseTypes = listOf(boolType, intType, realType, stringType)

fun parseType(strType : String) : Type {
    baseTypes.forEach { baseType ->
        if (strType == baseType.toString()) {
            return baseType
        }
    }
    return InvalidType(strType)
}

interface Type {
    fun toZ3Expr(variable : Variable, ctx : Context) : Expr<*>
    fun toZ3Expr(value : Value, ctx : Context) : Expr<*>
    fun fromZ3Expr(expr : Expr<*>, model : Model) : Any
    fun isOfType(obj : Any) : Boolean
}

class InvalidType : Type {
    constructor(name : String) {
        throw RuntimeException("Invalid type: $name")
    }
    override fun toZ3Expr(variable: Variable, ctx: Context): Expr<*> {
        throw RuntimeException("Invalid type")
    }

    override fun toZ3Expr(value: Value, ctx: Context): Expr<*> {
        throw RuntimeException("Invalid type")
    }

    override fun fromZ3Expr(expr: Expr<*>, model: Model): Any {
        throw RuntimeException("Invalid type")
    }

    override fun isOfType(obj: Any): Boolean {
        throw RuntimeException("Invalid type")
    }

    override fun toString(): String {
        throw RuntimeException("Invalid type")
    }

    override fun equals(other: Any?): Boolean {
        return other is InvalidType
    }

    override fun hashCode(): Int {
        return toString().hashCode()
    }
}

class BoolType : Type {
    override fun toZ3Expr(variable: Variable, ctx: Context): Expr<*> {
        return ctx.mkBoolConst(variable.name)
    }

    override fun toZ3Expr(value: Value, ctx: Context): Expr<*> {
        return ctx.mkBool(value.value as Boolean)
    }

    override fun fromZ3Expr(expr: Expr<*>, model: Model): Any {
        return expr.string.lowercase() == "true"
    }

    override fun isOfType(obj: Any): Boolean {
        return obj is Boolean
    }

    override fun toString(): String {
        return "Boolean"
    }

    override fun equals(other: Any?): Boolean {
        return other is BoolType
    }

    override fun hashCode(): Int {
        return toString().hashCode()
    }
}

class IntType : Type {
    override fun toZ3Expr(variable: Variable, ctx: Context): Expr<*> {
        return ctx.mkIntConst(variable.name)
    }

    override fun toZ3Expr(value: Value, ctx: Context): Expr<*> {
        return ctx.mkInt(value.value as Int)
    }

    override fun fromZ3Expr(expr: Expr<*>, model: Model): Any {
        if (expr is IntNum) {
            return expr.int
        }
        return Integer.parseInt(expr.toString())
    }

    override fun isOfType(obj: Any): Boolean {
        return obj is Int
    }

    override fun toString(): String {
        return "Int"
    }

    override fun equals(other: Any?): Boolean {
        return other is IntType
    }

    override fun hashCode(): Int {
        return toString().hashCode()
    }
}

class RealType : Type {
    override fun toZ3Expr(variable: Variable, ctx: Context): Expr<*> {
        return ctx.mkRealConst(variable.name)
    }

    override fun toZ3Expr(value: Value, ctx: Context): Expr<*> {
        return ctx.mkReal((value.value as Double).toString())
    }

    override fun fromZ3Expr(expr: Expr<*>, model: Model): Any {
        return when (expr) {
            is RatNum -> {
                expr.numerator.bigInteger.toDouble() / expr.denominator.bigInteger.toDouble()
            }
            is IntNum -> expr.int.toDouble()
            else -> {
                val s = expr.toString()
                if ('/' in s) {
                    val parts = s.split('/')
                    parts[0].toDouble() / parts[1].toDouble()
                } else {
                    s.toDouble()
                }
            }
        }
    }

    override fun isOfType(obj: Any): Boolean {
        return obj is Double
    }

    override fun toString(): String {
        return "Real"
    }

    override fun equals(other: Any?): Boolean {
        return other is RealType
    }

    override fun hashCode(): Int {
        return toString().hashCode()
    }
}

class StringType : Type {
    override fun toZ3Expr(variable: Variable, ctx: Context): Expr<*> {
        return ctx.mkStringConst(variable.name)
    }

    override fun toZ3Expr(value: Value, ctx: Context): Expr<*> {
        return ctx.mkString(value.value as String)
    }

    override fun fromZ3Expr(expr: Expr<*>, model: Model): Any {
        return expr.string
    }

    override fun isOfType(obj: Any): Boolean {
        return obj is String
    }

    override fun toString(): String {
        return "String"
    }

    override fun equals(other: Any?): Boolean {
        return other is StringType
    }

    override fun hashCode(): Int {
        return toString().hashCode()
    }
}

/**
 * Rigid type parameter used while checking polymorphic function / o-class template bodies.
 * Must not reach codegen or Z3 emission.
 */
class TypeVar(val name: String) : Type {
    override fun toZ3Expr(variable: Variable, ctx: Context): Expr<*> {
        throw RuntimeException("TypeVar \"$name\" must not reach Z3 codegen")
    }

    override fun toZ3Expr(value: Value, ctx: Context): Expr<*> {
        throw RuntimeException("TypeVar \"$name\" must not reach Z3 codegen")
    }

    override fun fromZ3Expr(expr: Expr<*>, model: Model): Any {
        throw RuntimeException("TypeVar \"$name\" must not reach Z3 codegen")
    }

    override fun isOfType(obj: Any): Boolean = false

    override fun toString(): String = name

    override fun equals(other: Any?): Boolean = other is TypeVar && other.name == name

    override fun hashCode(): Int = name.hashCode()
}
