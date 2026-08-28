package julay.compiler

sealed interface TypeExpr {
    data class Simple(val name: String) : TypeExpr {
        override fun toString(): String = name
    }
    data class Parametric(val ctor: String, val args: List<TypeExpr>) : TypeExpr {
        override fun toString(): String {
            val argStr = args.joinToString(", ") { arg -> arg.toString() }
            return "$ctor<$argStr>"
        }
    }
    /** `(A, B, ...) ~> R` — multi-arg domain on the left of `~>`. */
    data class Tuple(val elements: List<TypeExpr>) : TypeExpr {
        override fun toString(): String = "(${elements.joinToString(", ")})"
    }
    data class ProcFunRef(val args: List<TypeExpr>, val ret: TypeExpr) : TypeExpr {
        override fun toString(): String {
            val domain = if (args.size == 1) args.single().toString() else Tuple(args).toString()
            return "$domain ~> $ret"
        }
    }
}

fun TypeExpr.ctorName(): String = when (this) {
    is TypeExpr.Simple -> name
    is TypeExpr.Parametric -> ctor
    is TypeExpr.Tuple -> throw RuntimeException("Tuple type has no constructor name")
    is TypeExpr.ProcFunRef -> throw RuntimeException("ProcFunRef type has no constructor name")
}

/** Peel a type application into a head constructor and its argument expressions. */
fun TypeExpr.peelApp(): Pair<String, List<TypeExpr>> = when (this) {
    is TypeExpr.Simple -> name to emptyList()
    is TypeExpr.Parametric -> ctor to args
    is TypeExpr.Tuple -> throw RuntimeException("Cannot peel tuple type")
    is TypeExpr.ProcFunRef -> throw RuntimeException("Cannot peel procfun ref type")
}

/**
 * Verify that [expr] is a parametric type with exactly [arity] arguments.
 */
fun collectTypeArgs(expr: TypeExpr, arity: Int): List<TypeExpr>? = when (expr) {
    is TypeExpr.Parametric -> if (expr.args.size == arity) expr.args else null
    is TypeExpr.Simple -> if (arity == 0) emptyList() else null
    is TypeExpr.Tuple, is TypeExpr.ProcFunRef -> null
}

fun substituteTypeExpr(expr: TypeExpr, subst: Map<String, TypeExpr>): TypeExpr = when (expr) {
    is TypeExpr.Simple -> subst[expr.name] ?: expr
    is TypeExpr.Parametric -> TypeExpr.Parametric(expr.ctor, expr.args.map { substituteTypeExpr(it, subst) })
    is TypeExpr.Tuple -> TypeExpr.Tuple(expr.elements.map { substituteTypeExpr(it, subst) })
    is TypeExpr.ProcFunRef -> TypeExpr.ProcFunRef(
        expr.args.map { substituteTypeExpr(it, subst) },
        substituteTypeExpr(expr.ret, subst),
    )
}

fun typeExprMentions(expr: TypeExpr, names: Set<String>): Set<String> = when (expr) {
    is TypeExpr.Simple -> if (expr.name in names) setOf(expr.name) else emptySet()
    is TypeExpr.Parametric -> expr.args.flatMap { typeExprMentions(it, names) }.toSet()
    is TypeExpr.Tuple -> expr.elements.flatMap { typeExprMentions(it, names) }.toSet()
    is TypeExpr.ProcFunRef ->
        (expr.args.flatMap { typeExprMentions(it, names) } + typeExprMentions(expr.ret, names)).toSet()
}
