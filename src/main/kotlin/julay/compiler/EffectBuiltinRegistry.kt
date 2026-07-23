package julay.compiler

import julay.compiler.ast.CallStmtNode
import julay.compiler.ast.substituteExpr
import julay.program.type.Type

/**
 * Helpers for emitting before/after call statements. Effectful builtins live in
 * [FunBuiltinRegistry] under julay.funlib.
 */
object EffectBuiltinRegistry {
    val transitionOnlyEffects: Set<String> get() = FunBuiltinRegistry.transitionOnlyEffects
    val sessionPeerClassNameEffects: Set<String> get() = FunBuiltinRegistry.sessionPeerClassNameEffects
    val ioHavocEffects: Set<String> get() = FunBuiltinRegistry.ioHavocEffects

    fun kotlinCodegenImports(): Set<String> = FunBuiltinRegistry.kotlinCodegenImports()

    fun callStmtKotlinString(
        stmt: CallStmtNode,
        symbolTypes: Map<String, Type>,
        argSymbols: Set<String>,
        argStrings: List<String>? = null,
    ): String {
        val resolvedArgs = argStrings
            ?: stmt.callArgs().map { it.toTransitString(symbolTypes, argSymbols) }
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
