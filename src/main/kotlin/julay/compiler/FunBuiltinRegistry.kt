package julay.compiler

import julay.program.ListType
import julay.program.StringType
import julay.program.Type
import julay.program.intType

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
                argTypes[0] !is ListType -> "Expected argument of \"length\" to have a List type but got ${argTypes[0]}"
                else -> null
            }
        },
        kotlinCodegen = { args -> "${args[0]}.size" },
        z3Codegen = { args -> "ctx.mkSeqLengthAny(${args[0]})" },
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

    private val builtins = mapOf(
        lengthBuiltin.name to lengthBuiltin,
        parseIntBuiltin.name to parseIntBuiltin,
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
