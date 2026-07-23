package julay.compiler

import julay.compiler.ast.CallStmtNode
import julay.compiler.ast.substituteExpr
import julay.program.*
import julay.program.type.*
import julay.program.action.*

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

    private val exitSessionBuiltin = EffectBuiltin(
        name = "exitSession",
        // Peer proc-class name is validated specially (not an ordinary typed expr).
        paramTypes = listOf(stringType),
        returnType = null,
        kotlinCodegen = { args -> "hostProc.exitSession(${args[0]})" },
    )

    private val killSessionPeerBuiltin = EffectBuiltin(
        name = "killSessionPeer",
        paramTypes = listOf(stringType),
        returnType = null,
        kotlinCodegen = { args -> "hostProc.killSessionPeer(${args[0]})" },
    )

    private val builtins = mapOf(
        printlnBuiltin.name to printlnBuiltin,
        exitProcessBuiltin.name to exitProcessBuiltin,
        readlnBuiltin.name to readlnBuiltin,
        delaySecondsBuiltin.name to delaySecondsBuiltin,
        exitSessionBuiltin.name to exitSessionBuiltin,
        killSessionPeerBuiltin.name to killSessionPeerBuiltin,
    )

    /** Effects that may only appear on transitions (not constructors). */
    val transitionOnlyEffects: Set<String> = setOf(
        exitSessionBuiltin.name,
        killSessionPeerBuiltin.name,
    )

    /**
     * Session teardown effects whose single argument is a bare leaf proc-class identifier
     * (emitted as a string literal), not a value expression.
     */
    val sessionPeerClassNameEffects: Set<String> = transitionOnlyEffects

    /** IO builtins whose transit RHS is havoc'd (nondet domain membership) in TLA+. */
    val ioHavocEffects: Set<String> = setOf(readlnBuiltin.name)

    fun lookup(name: String): EffectBuiltin? = builtins[name]

    fun kotlinCodegenImports(): Set<String> = setOf(
        "kotlin.system.exitProcess",
        "kotlin.time.Duration.Companion.seconds",
        "kotlinx.coroutines.delay",
    )

    fun callStmtKotlinString(
        stmt: CallStmtNode,
        symbolTypes: Map<String, Type>,
        argSymbols: Set<String>,
        argStrings: List<String>? = null,
    ): String {
        val resolvedArgs = argStrings
            ?: stmt.callArgs().map { it.toTransitString(symbolTypes, argSymbols) }
        stmt.resolvedEffectOrNull()?.let { builtin ->
            return builtin.kotlinCodegen(resolvedArgs)
        }
        stmt.resolvedBuiltinOrNull()?.let { builtin ->
            return builtin.kotlinCodegen(resolvedArgs)
        }
        val funNode = stmt.resolvedFunOrNull()
            ?: throw RuntimeException("Call \"${stmt.callName()}\" not resolved")
        val params = funNode.funArgs().actionArgs()
        val inlined = params.zip(stmt.callArgs()).fold(funNode.funBody()) { acc, (param, arg) ->
            substituteExpr(acc, param.name, arg)
        }
        return inlined.toTransitString(symbolTypes, argSymbols)
    }
}
