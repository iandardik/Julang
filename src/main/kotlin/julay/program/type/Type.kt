package julay.program.type

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
    fun isOfType(obj: Any): Boolean
}

class InvalidType : Type {
    constructor(name: String) {
        throw RuntimeException("Invalid type: $name")
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
    override fun isOfType(obj: Any): Boolean = obj is Boolean

    override fun toString(): String = "Boolean"

    override fun equals(other: Any?): Boolean = other is BoolType

    override fun hashCode(): Int = toString().hashCode()
}

class IntType : Type {
    override fun isOfType(obj: Any): Boolean = obj is Int

    override fun toString(): String = "Int"

    override fun equals(other: Any?): Boolean = other is IntType

    override fun hashCode(): Int = toString().hashCode()
}

class RealType : Type {
    override fun isOfType(obj: Any): Boolean = obj is Double

    override fun toString(): String = "Real"

    override fun equals(other: Any?): Boolean = other is RealType

    override fun hashCode(): Int = toString().hashCode()
}

class StringType : Type {
    override fun isOfType(obj: Any): Boolean = obj is String

    override fun toString(): String = "String"

    override fun equals(other: Any?): Boolean = other is StringType

    override fun hashCode(): Int = toString().hashCode()
}

/**
 * Rigid type parameter used while checking polymorphic function / obj template bodies.
 * Must not reach codegen.
 */
class TypeVar(val name: String) : Type {
    override fun isOfType(obj: Any): Boolean = false

    override fun toString(): String = name

    override fun equals(other: Any?): Boolean = other is TypeVar && other.name == name

    override fun hashCode(): Int = name.hashCode()
}
