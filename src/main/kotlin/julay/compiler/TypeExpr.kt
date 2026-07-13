package julay.compiler

sealed interface TypeExpr {
    data class Simple(val name: String) : TypeExpr {
        override fun toString(): String = name
    }
    data class Parametric(val ctor: String, val args: List<TypeExpr>) : TypeExpr {
        override fun toString(): String {
            val argStr = args.joinToString(", ") { arg ->
                when (arg) {
                    is Simple -> arg.toString()
                    is Parametric -> arg.toString()
                }
            }
            return "$ctor<$argStr>"
        }
    }
}

fun TypeExpr.ctorName(): String = when (this) {
    is TypeExpr.Simple -> name
    is TypeExpr.Parametric -> ctor
}

/** Peel a type application into a head constructor and its argument expressions. */
fun TypeExpr.peelApp(): Pair<String, List<TypeExpr>> = when (this) {
    is TypeExpr.Simple -> name to emptyList()
    is TypeExpr.Parametric -> ctor to args
}

/**
 * Verify that [expr] is a parametric type with exactly [arity] arguments.
 */
fun collectTypeArgs(expr: TypeExpr, arity: Int): List<TypeExpr>? = when (expr) {
    is TypeExpr.Parametric -> if (expr.args.size == arity) expr.args else null
    is TypeExpr.Simple -> if (arity == 0) emptyList() else null
}

fun substituteTypeExpr(expr: TypeExpr, subst: Map<String, TypeExpr>): TypeExpr = when (expr) {
    is TypeExpr.Simple -> subst[expr.name] ?: expr
    is TypeExpr.Parametric -> TypeExpr.Parametric(expr.ctor, expr.args.map { substituteTypeExpr(it, subst) })
}

fun typeExprMentions(expr: TypeExpr, names: Set<String>): Set<String> = when (expr) {
    is TypeExpr.Simple -> if (expr.name in names) setOf(expr.name) else emptySet()
    is TypeExpr.Parametric -> expr.args.flatMap { typeExprMentions(it, names) }.toSet()
}
