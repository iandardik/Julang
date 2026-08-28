package julay.compiler

import julay.program.type.ListType
import julay.program.type.MapType
import julay.program.type.ObjClassType
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

    private val doHttpRequestBuiltin = FunBuiltin(
        name = "doHttpRequest",
        arity = 1,
        returnType = julay.program.library.httpClientResponseType,
        checkArgs = { argTypes ->
            when {
                argTypes.size != 1 -> "Expected function \"doHttpRequest\" to take 1 argument(s) but got ${argTypes.size}"
                argTypes[0] !is ObjClassType || (argTypes[0] as ObjClassType).name != "HttpClientRequest" ->
                    "Expected argument of \"doHttpRequest\" to have type HttpClientRequest but got ${argTypes[0]}"
                else -> null
            }
        },
        kotlinCodegen = { args -> "julay.program.library.doHttpRequest(${args[0]})" },
        z3Codegen = { _ -> throw RuntimeException("Function \"doHttpRequest\" cannot be used in guards") },
        ioHavoc = true,
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

    private val exitProgramBuiltin = FunBuiltin(
        name = "exitProgram",
        arity = 1,
        returnType = null,
        checkArgs = { argTypes ->
            when {
                argTypes.size != 1 -> "Expected function \"exitProgram\" to take 1 argument(s) but got ${argTypes.size}"
                argTypes[0] !is IntType ->
                    "Expected argument of \"exitProgram\" to have an Int type but got ${argTypes[0]}"
                else -> null
            }
        },
        kotlinCodegen = { args -> "exitProcess(${args[0]})" },
        z3Codegen = { _ -> throw RuntimeException("Function \"exitProgram\" cannot be used in guards") },
    )

    private val exitProcBuiltin = FunBuiltin(
        name = "exitProc",
        arity = 0,
        returnType = null,
        checkArgs = { argTypes ->
            if (argTypes.isEmpty()) null
            else "Expected function \"exitProc\" to take 0 argument(s) but got ${argTypes.size}"
        },
        kotlinCodegen = { _ -> "hostProc.requestSilentKill()" },
        z3Codegen = { _ -> throw RuntimeException("Function \"exitProc\" cannot be used in guards") },
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
     * Higher-order map over List or Set: `map(xs, f)` where `f` is a unary user `fun` name or lambda.
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

    /** Import-only marker for grammar `listOf(...)` literals (not a callable FunCall). */
    private val listOfBuiltin = FunBuiltin(
        name = "listOf",
        arity = -1,
        returnType = listType(stringType),
        checkArgs = { _ -> "listOf is a collection literal, not a function call" },
        kotlinCodegen = { _ -> throw RuntimeException("listOf is a collection literal, not a call") },
        z3Codegen = { _ -> throw RuntimeException("listOf is a collection literal, not a call") },
    )

    /** Import-only marker for grammar `setOf(...)` literals (not a callable FunCall). */
    private val setOfBuiltin = FunBuiltin(
        name = "setOf",
        arity = -1,
        returnType = listType(stringType),
        checkArgs = { _ -> "setOf is a collection literal, not a function call" },
        kotlinCodegen = { _ -> throw RuntimeException("setOf is a collection literal, not a call") },
        z3Codegen = { _ -> throw RuntimeException("setOf is a collection literal, not a call") },
    )

    /** Import-only marker for grammar `mapOf(...)` literals (not a callable FunCall). */
    private val mapOfBuiltin = FunBuiltin(
        name = "mapOf",
        arity = -1,
        returnType = listType(stringType),
        checkArgs = { _ -> "mapOf is a collection literal, not a function call" },
        kotlinCodegen = { _ -> throw RuntimeException("mapOf is a collection literal, not a call") },
        z3Codegen = { _ -> throw RuntimeException("mapOf is a collection literal, not a call") },
    )

    /**
     * List slice: `splice(xs, start, end)` — 1-based inclusive (TLA SubSeq-style) with clamp.
     * `start` must be `>= 1`; `end` may be `0` for an empty prefix (e.g. Raft commitIndex).
     * `hi = min(end, length)`; `lo = start`; empty if `end < 1` or `lo > hi`; else elements lo..hi inclusive.
     * Return type is specialized from arg0's ListType in TypePass.
     * Z3 path is handled in FunCallExprNode (registry throws).
     */
    private val spliceBuiltin = FunBuiltin(
        name = "splice",
        arity = 3,
        returnType = listType(stringType), // placeholder; real return set via resolveInstantiatedReturnType
        checkArgs = { argTypes ->
            when {
                argTypes.size != 3 -> "Expected function \"splice\" to take 3 argument(s) but got ${argTypes.size}"
                argTypes[0] !is ListType ->
                    "Expected first argument of \"splice\" to have a List type but got ${argTypes[0]}"
                argTypes[1] !is IntType ->
                    "Expected second argument of \"splice\" to have an Int type but got ${argTypes[1]}"
                argTypes[2] !is IntType ->
                    "Expected third argument of \"splice\" to have an Int type but got ${argTypes[2]}"
                else -> null
            }
        },
        kotlinCodegen = { args ->
            "run { val __xs = ${args[0]}; val __s = ${args[1]}; val __e = ${args[2]}; " +
                "require(__s >= 1 && __e >= 0) { \"slice start must be >= 1 and end >= 0\" }; " +
                "if (__e < 1) emptyList() else { " +
                "val __hi = minOf(__e, __xs.size); val __lo = __s; " +
                "if (__lo > __hi) emptyList() else __xs.subList(__lo - 1, __hi).toList() } }"
        },
        z3Codegen = { _ ->
            throw RuntimeException("Function \"splice\" Z3 codegen is handled by FunCallExprNode")
        },
    )

    /**
     * Whether every element of a list is unique. Short-circuits on the first duplicate.
     * Z3 path is handled in FunCallExprNode (embed Kotlin when the list is concrete state).
     */
    private val allDistinctBuiltin = FunBuiltin(
        name = "allDistinct",
        arity = 1,
        returnType = boolType,
        checkArgs = { argTypes ->
            when {
                argTypes.size != 1 -> "Expected function \"allDistinct\" to take 1 argument(s) but got ${argTypes.size}"
                argTypes[0] !is ListType ->
                    "Expected argument of \"allDistinct\" to have a List type but got ${argTypes[0]}"
                else -> null
            }
        },
        kotlinCodegen = { args ->
            "run { val __xs = ${args[0]}; val __s = HashSet<Any?>(__xs.size); __xs.all { __s.add(it) } }"
        },
        z3Codegen = { _ ->
            throw RuntimeException("Function \"allDistinct\" Z3 codegen is handled by FunCallExprNode")
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
        doHttpRequestBuiltin.name to doHttpRequestBuiltin,
        printlnBuiltin.name to printlnBuiltin,
        exitProgramBuiltin.name to exitProgramBuiltin,
        exitProcBuiltin.name to exitProcBuiltin,
        readlnBuiltin.name to readlnBuiltin,
        delaySecondsBuiltin.name to delaySecondsBuiltin,
        exitSessionBuiltin.name to exitSessionBuiltin,
        killSessionPeerBuiltin.name to killSessionPeerBuiltin,
        mapBuiltin.name to mapBuiltin,
        listOfBuiltin.name to listOfBuiltin,
        setOfBuiltin.name to setOfBuiltin,
        mapOfBuiltin.name to mapOfBuiltin,
        spliceBuiltin.name to spliceBuiltin,
        allDistinctBuiltin.name to allDistinctBuiltin,
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
