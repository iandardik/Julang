package julay.compiler

import julay.compiler.ast.EffectAssignNode
import julay.compiler.ast.EffectCallNode
import julay.compiler.ast.EffectStmtNode
import julay.compiler.ast.ExprNode
import julay.program.*

data class EffectBuiltin(
    val name: String,
    val paramTypes: List<Type>,
    val returnType: Type?,
    val kotlinCodegen: (List<String>) -> String,
)

object EffectBuiltinRegistry {
    private val printlnBuiltin = EffectBuiltin(
        name = "println",
        paramTypes = listOf(stringType),
        returnType = null,
        kotlinCodegen = { args -> "println(${args[0]})" },
    )

    private val exitProcessBuiltin = EffectBuiltin(
        name = "exitProcess",
        paramTypes = emptyList(),
        returnType = null,
        kotlinCodegen = { _ -> "exitProcess(0)" },
    )

    private val readlnBuiltin = EffectBuiltin(
        name = "readln",
        paramTypes = emptyList(),
        returnType = stringType,
        kotlinCodegen = { _ -> "readln()" },
    )

    private val delaySecondsBuiltin = EffectBuiltin(
        name = "delaySeconds",
        paramTypes = listOf(intType),
        returnType = null,
        kotlinCodegen = { args -> "delay(${args[0]}.seconds)" },
    )

    private val builtins = mapOf(
        printlnBuiltin.name to printlnBuiltin,
        exitProcessBuiltin.name to exitProcessBuiltin,
        readlnBuiltin.name to readlnBuiltin,
        delaySecondsBuiltin.name to delaySecondsBuiltin,
    )

    fun lookup(name: String): EffectBuiltin? = builtins[name]

    fun kotlinCodegenImports(): Set<String> = setOf(
        "kotlin.system.exitProcess",
        "kotlin.time.Duration.Companion.seconds",
        "kotlinx.coroutines.delay",
    )

    fun effectStmtKotlinString(
        stmt: EffectStmtNode,
        symbolTypes: Map<String, Type>,
        argSymbols: Set<String>,
        assignPrefix: String = "",
    ): String = when (stmt) {
        is EffectCallNode -> callKotlinString(stmt.callName(), stmt.callArgs(), symbolTypes, argSymbols)
        is EffectAssignNode -> {
            val rhs = callKotlinString(stmt.callName(), stmt.callArgs(), symbolTypes, argSymbols)
            "${assignPrefix}${stmt.assignKey().toKotlinIdent()} = $rhs"
        }
    }

    private fun callKotlinString(
        name: String,
        args: List<ExprNode>,
        symbolTypes: Map<String, Type>,
        argSymbols: Set<String>,
    ): String {
        val builtin = lookup(name)
            ?: throw RuntimeException("Unknown effect builtin \"$name\"")
        if (builtin.paramTypes.size != args.size) {
            throw RuntimeException(
                "Expected effect \"$name\" to take ${builtin.paramTypes.size} argument(s) but got ${args.size}",
            )
        }
        val argStrings = args.map { it.toTransitString(symbolTypes, argSymbols) }
        return builtin.kotlinCodegen(argStrings)
    }
}
