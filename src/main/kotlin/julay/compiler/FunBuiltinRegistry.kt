package julay.compiler

import julay.program.ListType
import julay.program.MapType
import julay.program.SetType
import julay.program.StringType
import julay.program.Type
import julay.program.boolType
import julay.program.Channel
import julay.program.channelType
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
    val z3Codegen: (List<String>) -> String,
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
        z3Codegen = { args ->
            val argType = args[0] // placeholder - resolved at call site via type on FunCallExprNode
            // Actual Z3 codegen is overridden in FunCallExprNode using argument type
            "ctx.mkSeqLengthAny(${args[0]})"
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
        z3Codegen = { args -> "ctx.stringToInt(${args[0]} as Expr<SeqSort<CharSort>>)" },
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
        z3Codegen = { _ ->
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
        z3Codegen = { _ ->
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
        z3Codegen = { _ ->
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
        z3Codegen = { _ ->
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
        z3Codegen = { args ->
            "ctx.mkPrefixOf(${args[1]} as Expr<SeqSort<CharSort>>, ${args[0]} as Expr<SeqSort<CharSort>>)"
        },
    )

    private val createEmptyChannelBuiltin = FunBuiltin(
        name = "createEmptyChannel",
        arity = 0,
        // Placeholder; TypePass sets ChannelType from createEmptyChannel<ActName>() type arg.
        returnType = channelType("_"),
        checkArgs = { argTypes ->
            when {
                argTypes.isNotEmpty() ->
                    "Expected function \"createEmptyChannel\" to take 0 argument(s) but got ${argTypes.size}"
                else -> null
            }
        },
        kotlinCodegen = { _ ->
            throw RuntimeException("createEmptyChannel codegen requires a type argument")
        },
        z3Codegen = { _ -> "ctx.mkInt(${Channel.EMPTY_ID})" },
    )

    private val createChannelBuiltin = FunBuiltin(
        name = "createChannel",
        arity = 0,
        // Placeholder; TypePass sets ChannelType from createChannel<ActName>() type arg.
        returnType = channelType("_"),
        checkArgs = { argTypes ->
            when {
                argTypes.isNotEmpty() ->
                    "Expected function \"createChannel\" to take 0 argument(s) but got ${argTypes.size}"
                else -> null
            }
        },
        kotlinCodegen = { _ ->
            throw RuntimeException("createChannel codegen requires a type argument")
        },
        // Must not appear in guards (allocates); TypePass rejects guard use.
        z3Codegen = { _ ->
            throw RuntimeException("createChannel cannot be used in a guard")
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
        createEmptyChannelBuiltin.name to createEmptyChannelBuiltin,
        createChannelBuiltin.name to createChannelBuiltin,
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
