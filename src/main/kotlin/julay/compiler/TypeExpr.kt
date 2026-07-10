package julay.compiler

sealed interface TypeExpr {
    data class Simple(val name: String) : TypeExpr {
        override fun toString(): String = name
    }
    data class Parametric(val ctor: String, val arg: TypeExpr) : TypeExpr {
        override fun toString(): String {
            val argStr = when (arg) {
                is Simple -> arg.toString()
                is Parametric -> "($arg)"
            }
            return "$ctor $argStr"
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
    is TypeExpr.Parametric -> ctor to listOf(arg)
}

/**
 * Given the argument spine of a type application (right-associated `ID typeExpr` nesting),
 * collect exactly [arity] argument [TypeExpr]s.
 *
 * For arity 1, the whole spine is one argument (so `Box Int` and `Box (Pair Int Boolean)` work).
 * For arity > 1, a spine `A B C` parsed as `Parametric(A, Parametric(B, C))` becomes
 * `[Simple(A), Simple(B), Simple(C)]` when collecting 3 args (nullary args in the spine).
 */
fun collectTypeArgs(spine: TypeExpr, arity: Int): List<TypeExpr>? {
    if (arity < 1) return null
    if (arity == 1) return listOf(spine)
    return when (spine) {
        is TypeExpr.Parametric -> {
            val rest = collectTypeArgs(spine.arg, arity - 1) ?: return null
            listOf(TypeExpr.Simple(spine.ctor)) + rest
        }
        is TypeExpr.Simple -> null
    }
}

fun substituteTypeExpr(expr: TypeExpr, subst: Map<String, TypeExpr>): TypeExpr = when (expr) {
    is TypeExpr.Simple -> subst[expr.name] ?: expr
    is TypeExpr.Parametric -> TypeExpr.Parametric(expr.ctor, substituteTypeExpr(expr.arg, subst))
}

fun typeExprMentions(expr: TypeExpr, names: Set<String>): Set<String> = when (expr) {
    is TypeExpr.Simple -> if (expr.name in names) setOf(expr.name) else emptySet()
    is TypeExpr.Parametric -> typeExprMentions(expr.arg, names)
}
