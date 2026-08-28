package julay.program.type

import com.microsoft.z3.Context
import com.microsoft.z3.Expr
import com.microsoft.z3.Model
import julay.program.Value
import julay.program.Variable
import julay.tools.mkStringConst

/**
 * Reference to a procfun by name (erased to [String] at runtime).
 * Written in source as `A ~> B` or `(A, B) ~> R`.
 */
class ProcFunRefType(
    val argTypes: List<Type>,
    val returnType: Type,
) : Type {
    init {
        require(argTypes.isNotEmpty()) { "ProcFunRefType requires at least one argument type" }
    }

    override fun toZ3Expr(variable: Variable, ctx: Context): Expr<*> =
        ctx.mkStringConst(variable.name)

    override fun toZ3Expr(value: Value, ctx: Context): Expr<*> =
        ctx.mkString(value.value as String)

    override fun fromZ3Expr(expr: Expr<*>, model: Model): Any = expr.string

    override fun isOfType(obj: Any): Boolean = obj is String

    override fun toString(): String = when (argTypes.size) {
        1 -> "${argTypes.single()} ~> $returnType"
        else -> "(${argTypes.joinToString(", ")}) ~> $returnType"
    }

    override fun equals(other: Any?): Boolean =
        other is ProcFunRefType && argTypes == other.argTypes && returnType == other.returnType

    override fun hashCode(): Int = argTypes.hashCode() * 31 + returnType.hashCode()
}

fun procFunRefType(argType: Type, returnType: Type): ProcFunRefType =
    ProcFunRefType(listOf(argType), returnType)

fun procFunRefType(argTypes: List<Type>, returnType: Type): ProcFunRefType =
    ProcFunRefType(argTypes, returnType)
