package julay.compiler

import julay.program.type.ListType
import julay.program.type.MapType
import julay.program.type.SetType
import julay.program.type.StringType
import julay.program.type.IntType
import julay.program.type.Type
import julay.program.type.boolType
import julay.program.type.intType
import julay.program.type.listType
import julay.program.type.stringType
import julay.program.library.JULAY_FUNLIB
import julay.program.library.JULAY_MODULE

/**
 * Kotlin-backed function from julay.funlib.* (pure helpers and effectful builtins).
 * [returnType] is null for void effects usable only as bare `before`/`after` statements.
 */
data class FunBuiltin(
    val name: String,
    val arity: Int,
    val returnType: Type?,
    val checkArgs: (List<Type>) -> String?,
    val kotlinCodegen: (List<String>) -> String,
    val z3Codegen: (List<String>) -> String,
    /** Session teardown: argument is a bare leaf proc-class name, not a typed value. */
    val sessionPeerClassArg: Boolean = false,
    /**
     * Second argument is a bare unary user-fun name (named-fun HOF), not a typed value.
     * Typing and codegen are handled specially in TypePass / FunCallExprNode.
     */
    val namedFunArg: Boolean = false,
    /** May only appear on transitions (not constructors). */
    val transitionOnly: Boolean = false,
    /** Transit RHS involving this call is havoc'd in TLA+. */
    val ioHavoc: Boolean = false,
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
        ioHavoc = true,
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

    private val printlnBuiltin = FunBuiltin(
        name = "println",
        arity = 1,
        returnType = null,
        checkArgs = { argTypes ->
            when {
                argTypes.size != 1 -> "Expected function \"println\" to take 1 argument(s) but got ${argTypes.size}"
                argTypes[0] !is StringType -> "Expected argument of \"println\" to have a String type but got ${argTypes[0]}"
                else -> null
            }
        },
        kotlinCodegen = { args -> "println(${args[0]})" },
        z3Codegen = { _ -> throw RuntimeException("Function \"println\" cannot be used in guards") },
    )

    private val exitProcessBuiltin = FunBuiltin(
        name = "exitProcess",
        arity = 0,
        returnType = null,
        checkArgs = { argTypes ->
            if (argTypes.isEmpty()) null
            else "Expected function \"exitProcess\" to take 0 argument(s) but got ${argTypes.size}"
        },
        kotlinCodegen = { _ -> "exitProcess(0)" },
        z3Codegen = { _ -> throw RuntimeException("Function \"exitProcess\" cannot be used in guards") },
    )

    private val readlnBuiltin = FunBuiltin(
        name = "readln",
        arity = 0,
        returnType = stringType,
        checkArgs = { argTypes ->
            if (argTypes.isEmpty()) null
            else "Expected function \"readln\" to take 0 argument(s) but got ${argTypes.size}"
        },
        kotlinCodegen = { _ -> "readln()" },
        z3Codegen = { _ -> throw RuntimeException("Function \"readln\" cannot be used in guards") },
        ioHavoc = true,
    )

    private val delaySecondsBuiltin = FunBuiltin(
        name = "delaySeconds",
        arity = 1,
        returnType = null,
        checkArgs = { argTypes ->
            when {
                argTypes.size != 1 -> "Expected function \"delaySeconds\" to take 1 argument(s) but got ${argTypes.size}"
                argTypes[0] !is IntType ->
                    "Expected argument of \"delaySeconds\" to have an Int type but got ${argTypes[0]}"
                else -> null
            }
        },
        kotlinCodegen = { args -> "delay(${args[0]}.seconds)" },
        z3Codegen = { _ -> throw RuntimeException("Function \"delaySeconds\" cannot be used in guards") },
    )

    private val exitSessionBuiltin = FunBuiltin(
        name = "exitSession",
        arity = 1,
        returnType = null,
        checkArgs = { argTypes ->
            if (argTypes.size == 1) null
            else "Expected function \"exitSession\" to take 1 argument(s) but got ${argTypes.size}"
        },
        kotlinCodegen = { args -> "hostProc.exitSession(${args[0]})" },
        z3Codegen = { _ -> throw RuntimeException("Function \"exitSession\" cannot be used in guards") },
        sessionPeerClassArg = true,
        transitionOnly = true,
    )

    private val killSessionPeerBuiltin = FunBuiltin(
        name = "killSessionPeer",
        arity = 1,
        returnType = null,
        checkArgs = { argTypes ->
            if (argTypes.size == 1) null
            else "Expected function \"killSessionPeer\" to take 1 argument(s) but got ${argTypes.size}"
        },
        kotlinCodegen = { args -> "hostProc.killSessionPeer(${args[0]})" },
        z3Codegen = { _ -> throw RuntimeException("Function \"killSessionPeer\" cannot be used in guards") },
        sessionPeerClassArg = true,
        transitionOnly = true,
    )

    /**
     * Higher-order map over List or Set: `map(xs, f)` where `f` is a unary user `fun` name.
     * Return type and codegen are specialized in TypePass / FunCallExprNode.
     */
    private val mapBuiltin = FunBuiltin(
        name = "map",
        arity = 2,
        returnType = listType(stringType), // placeholder; real return set via resolveInstantiatedReturnType
        checkArgs = { argTypes ->
            when {
                argTypes.size != 2 -> "Expected function \"map\" to take 2 argument(s) but got ${argTypes.size}"
                else -> null
            }
        },
        kotlinCodegen = { _ ->
            throw RuntimeException("Function \"map\" requires named-fun codegen")
        },
        z3Codegen = { _ ->
            throw RuntimeException("Function \"map\" cannot be used in guards")
        },
        namedFunArg = true,
    )

    private val builtins = mapOf(
        lengthBuiltin.name to lengthBuiltin,
        parseIntBuiltin.name to parseIntBuiltin,
        readFileBuiltin.name to readFileBuiltin,
        splitBuiltin.name to splitBuiltin,
        trimBuiltin.name to trimBuiltin,
        portFromUrlBuiltin.name to portFromUrlBuiltin,
        startsWithBuiltin.name to startsWithBuiltin,
        printlnBuiltin.name to printlnBuiltin,
        exitProcessBuiltin.name to exitProcessBuiltin,
        readlnBuiltin.name to readlnBuiltin,
        delaySecondsBuiltin.name to delaySecondsBuiltin,
        exitSessionBuiltin.name to exitSessionBuiltin,
        killSessionPeerBuiltin.name to killSessionPeerBuiltin,
        mapBuiltin.name to mapBuiltin,
    )

    val namedFunArgEffects: Set<String> =
        builtins.values.filter { it.namedFunArg }.map { it.name }.toSet()

    val all: Collection<FunBuiltin> get() = builtins.values

    val transitionOnlyEffects: Set<String> =
        builtins.values.filter { it.transitionOnly }.map { it.name }.toSet()

    val sessionPeerClassNameEffects: Set<String> =
        builtins.values.filter { it.sessionPeerClassArg }.map { it.name }.toSet()

    val ioHavocEffects: Set<String> =
        builtins.values.filter { it.ioHavoc }.map { it.name }.toSet()

    fun lookup(name: String): FunBuiltin? = builtins[name]

    fun isFunBuiltin(name: String): Boolean = name in builtins

    /** Qualified import path parts, e.g. ["julay", "funlib", "length"]. */
    fun resolveQualified(parts: List<String>): FunBuiltin? {
        if (parts.size != 3) return null
        if (parts[0] != JULAY_MODULE || parts[1] != JULAY_FUNLIB) return null
        return lookup(parts[2])
    }

    fun kotlinCodegenImports(): Set<String> = setOf(
        "kotlin.system.exitProcess",
        "kotlin.time.Duration.Companion.seconds",
        "kotlinx.coroutines.delay",
    )
}
