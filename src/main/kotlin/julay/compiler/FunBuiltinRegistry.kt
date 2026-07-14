package julay.compiler

import julay.program.ListType
import julay.program.MapType
import julay.program.SetType
import julay.program.StringType
import julay.program.Type
import julay.program.boolType
import julay.program.intType
import julay.program.listType
import julay.program.stringType

/**
 * Kotlin-backed expression function (julaylib.fun.*), analogous to effect builtins
 * but usable in guards/transit via [julay.compiler.ast.FunCallExprNode].
 */
data class FunBuiltin(
    val name: String,
    val arity: Int,
    val returnType: Type,
    val checkArgs: (List<Type>) -> String?,
    val kotlinCodegen: (List<String>) -> String,
    val smtCodegen: (List<String>) -> String,
)

object FunBuiltinRegistry {
    private val lengthBuiltin = FunBuiltin(
        name = "length",
        arity = 1,
        returnType = intType,
        checkArgs = { argTypes ->
            when {
                argTypes.size != 1 -> "Expected function \"length\" to take 1 argument(s) but got ${argTypes.size}"
                argTypes[0] !is ListType && argTypes[0] !is SetType && argTypes[0] !is MapType ->
                    "Expected argument of \"length\" to have a List, Set, or Map type but got ${argTypes[0]}"
                else -> null
            }
        },
        kotlinCodegen = { args -> "${args[0]}.size" },
        smtCodegen = { args ->
            // Actual SMT codegen is overridden in FunCallExprNode using argument type
            "tm.mkSeqLength(${args[0]})"
        },
    )

    private val parseIntBuiltin = FunBuiltin(
        name = "parseInt",
        arity = 1,
        returnType = intType,
        checkArgs = { argTypes ->
            when {
                argTypes.size != 1 -> "Expected function \"parseInt\" to take 1 argument(s) but got ${argTypes.size}"
                argTypes[0] !is StringType -> "Expected argument of \"parseInt\" to have a String type but got ${argTypes[0]}"
                else -> null
            }
        },
        kotlinCodegen = { args -> "${args[0]}.toInt()" },
        smtCodegen = { args -> "tm.mkTerm(Kind.STRING_TO_INT, ${args[0]})" },
    )

    private val readFileBuiltin = FunBuiltin(
        name = "readFile",
        arity = 1,
        returnType = stringType,
        checkArgs = { argTypes ->
            when {
                argTypes.size != 1 -> "Expected function \"readFile\" to take 1 argument(s) but got ${argTypes.size}"
                argTypes[0] !is StringType -> "Expected argument of \"readFile\" to have a String type but got ${argTypes[0]}"
                else -> null
            }
        },
        kotlinCodegen = { args -> "java.io.File(${args[0]}).readText()" },
        smtCodegen = { _ ->
            throw RuntimeException("Function \"readFile\" cannot be used in guards")
        },
    )

    private val splitBuiltin = FunBuiltin(
        name = "split",
        arity = 2,
        returnType = listType(stringType),
        checkArgs = { argTypes ->
            when {
                argTypes.size != 2 -> "Expected function \"split\" to take 2 argument(s) but got ${argTypes.size}"
                argTypes[0] !is StringType -> "Expected first argument of \"split\" to have a String type but got ${argTypes[0]}"
                argTypes[1] !is StringType -> "Expected second argument of \"split\" to have a String type but got ${argTypes[1]}"
                else -> null
            }
        },
        kotlinCodegen = { args -> "${args[0]}.split(${args[1]})" },
        smtCodegen = { _ ->
            throw RuntimeException("Function \"split\" cannot be used in guards")
        },
    )

    private val trimBuiltin = FunBuiltin(
        name = "trim",
        arity = 1,
        returnType = stringType,
        checkArgs = { argTypes ->
            when {
                argTypes.size != 1 -> "Expected function \"trim\" to take 1 argument(s) but got ${argTypes.size}"
                argTypes[0] !is StringType -> "Expected argument of \"trim\" to have a String type but got ${argTypes[0]}"
                else -> null
            }
        },
        kotlinCodegen = { args -> "${args[0]}.trim()" },
        smtCodegen = { _ ->
            throw RuntimeException("Function \"trim\" cannot be used in guards")
        },
    )

    private val portFromUrlBuiltin = FunBuiltin(
        name = "portFromUrl",
        arity = 1,
        returnType = intType,
        checkArgs = { argTypes ->
            when {
                argTypes.size != 1 -> "Expected function \"portFromUrl\" to take 1 argument(s) but got ${argTypes.size}"
                argTypes[0] !is StringType -> "Expected argument of \"portFromUrl\" to have a String type but got ${argTypes[0]}"
                else -> null
            }
        },
        kotlinCodegen = { args ->
            "run { val _u = ${args[0]}; val _h = _u.substringAfter(\"://\", _u); _h.substringAfterLast(':').toInt() }"
        },
        smtCodegen = { _ ->
            throw RuntimeException("Function \"portFromUrl\" cannot be used in guards")
        },
    )

    private val startsWithBuiltin = FunBuiltin(
        name = "startsWith",
        arity = 2,
        returnType = boolType,
        checkArgs = { argTypes ->
            when {
                argTypes.size != 2 -> "Expected function \"startsWith\" to take 2 argument(s) but got ${argTypes.size}"
                argTypes[0] !is StringType -> "Expected first argument of \"startsWith\" to have a String type but got ${argTypes[0]}"
                argTypes[1] !is StringType -> "Expected second argument of \"startsWith\" to have a String type but got ${argTypes[1]}"
                else -> null
            }
        },
        kotlinCodegen = { args -> "${args[0]}.startsWith(${args[1]})" },
        smtCodegen = { args ->
            "tm.mkTerm(Kind.STRING_PREFIX, ${args[1]}, ${args[0]})"
        },
    )

    private val builtins = mapOf(
        lengthBuiltin.name to lengthBuiltin,
        parseIntBuiltin.name to parseIntBuiltin,
        readFileBuiltin.name to readFileBuiltin,
        splitBuiltin.name to splitBuiltin,
        trimBuiltin.name to trimBuiltin,
        portFromUrlBuiltin.name to portFromUrlBuiltin,
        startsWithBuiltin.name to startsWithBuiltin,
    )

    val all: Collection<FunBuiltin> get() = builtins.values

    fun lookup(name: String): FunBuiltin? = builtins[name]

    fun isFunBuiltin(name: String): Boolean = name in builtins

    /** Qualified import path parts, e.g. ["julaylib", "fun", "length"]. */
    fun resolveQualified(parts: List<String>): FunBuiltin? {
        if (parts.size != 3) return null
        if (parts[0] != "julaylib" || parts[1] != "fun") return null
        return lookup(parts[2])
    }
}
