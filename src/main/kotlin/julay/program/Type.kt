package julay.program

import io.github.cvc5.Solver
import io.github.cvc5.Term
import io.github.cvc5.TermManager
import julay.tools.mkKotlinString
import julay.tools.mkStringConst

val boolType = BoolType()
val intType = IntType()
val realType = RealType()
val stringType = StringType()
val baseTypes = listOf(boolType, intType, realType, stringType)

fun parseType(strType: String): Type {
    baseTypes.forEach { baseType ->
        if (strType == baseType.toString()) {
            return baseType
        }
    }
    return InvalidType(strType)
}

interface Type {
    fun toSmtTerm(variable: Variable, tm: TermManager): Term
    fun toSmtTerm(value: Value, tm: TermManager): Term
    fun fromSmtTerm(expr: Term, solver: Solver): Any
    fun isOfType(obj: Any): Boolean
}

class InvalidType : Type {
    constructor(name: String) {
        throw RuntimeException("Invalid type: $name")
    }

    override fun toSmtTerm(variable: Variable, tm: TermManager): Term {
        throw RuntimeException("Invalid type")
    }

    override fun toSmtTerm(value: Value, tm: TermManager): Term {
        throw RuntimeException("Invalid type")
    }

    override fun fromSmtTerm(expr: Term, solver: Solver): Any {
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
    override fun toSmtTerm(variable: Variable, tm: TermManager): Term =
        tm.mkConst(tm.booleanSort, variable.name)

    override fun toSmtTerm(value: Value, tm: TermManager): Term =
        tm.mkBoolean(value.value as Boolean)

    override fun fromSmtTerm(expr: Term, solver: Solver): Any {
        val evaluated = solver.getValue(expr)
        return when {
            evaluated.isBooleanValue -> evaluated.booleanValue
            else -> evaluated.toString().lowercase() == "true"
        }
    }

    override fun isOfType(obj: Any): Boolean = obj is Boolean

    override fun toString(): String = "Boolean"

    override fun equals(other: Any?): Boolean = other is BoolType

    override fun hashCode(): Int = toString().hashCode()
}

class IntType : Type {
    override fun toSmtTerm(variable: Variable, tm: TermManager): Term =
        tm.mkConst(tm.integerSort, variable.name)

    override fun toSmtTerm(value: Value, tm: TermManager): Term =
        tm.mkInteger((value.value as Int).toLong())

    override fun fromSmtTerm(expr: Term, solver: Solver): Any {
        val evaluated = solver.getValue(expr)
        return evaluated.integerValue.toInt()
    }

    override fun isOfType(obj: Any): Boolean = obj is Int

    override fun toString(): String = "Int"

    override fun equals(other: Any?): Boolean = other is IntType

    override fun hashCode(): Int = toString().hashCode()
}

class RealType : Type {
    override fun toSmtTerm(variable: Variable, tm: TermManager): Term =
        tm.mkConst(tm.realSort, variable.name)

    override fun toSmtTerm(value: Value, tm: TermManager): Term =
        tm.mkReal((value.value as Double).toString())

    override fun fromSmtTerm(expr: Term, solver: Solver): Any {
        val evaluated = solver.getValue(expr)
        return try {
            val pair = evaluated.realValue
            pair.first.toDouble() / pair.second.toDouble()
        } catch (_: Exception) {
            val s = evaluated.toString()
            if ('/' in s) {
                val parts = s.split('/')
                parts[0].toDouble() / parts[1].toDouble()
            } else {
                s.toDouble()
            }
        }
    }

    override fun isOfType(obj: Any): Boolean = obj is Double

    override fun toString(): String = "Real"

    override fun equals(other: Any?): Boolean = other is RealType

    override fun hashCode(): Int = toString().hashCode()
}

class StringType : Type {
    override fun toSmtTerm(variable: Variable, tm: TermManager): Term =
        tm.mkStringConst(variable.name)

    override fun toSmtTerm(value: Value, tm: TermManager): Term =
        tm.mkKotlinString(value.value as String)

    override fun fromSmtTerm(expr: Term, solver: Solver): Any {
        val evaluated = solver.getValue(expr)
        return evaluated.stringValue
    }

    override fun isOfType(obj: Any): Boolean = obj is String

    override fun toString(): String = "String"

    override fun equals(other: Any?): Boolean = other is StringType

    override fun hashCode(): Int = toString().hashCode()
}

/**
 * Rigid type parameter used while checking polymorphic function / o-class template bodies.
 * Must not reach codegen or SMT emission.
 */
class TypeVar(val name: String) : Type {
    override fun toSmtTerm(variable: Variable, tm: TermManager): Term {
        throw RuntimeException("TypeVar \"$name\" must not reach SMT codegen")
    }

    override fun toSmtTerm(value: Value, tm: TermManager): Term {
        throw RuntimeException("TypeVar \"$name\" must not reach SMT codegen")
    }

    override fun fromSmtTerm(expr: Term, solver: Solver): Any {
        throw RuntimeException("TypeVar \"$name\" must not reach SMT codegen")
    }

    override fun isOfType(obj: Any): Boolean = false

    override fun toString(): String = name

    override fun equals(other: Any?): Boolean = other is TypeVar && other.name == name

    override fun hashCode(): Int = name.hashCode()
}
