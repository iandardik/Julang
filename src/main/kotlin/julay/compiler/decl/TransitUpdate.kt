package julay.compiler.decl

import julay.compiler.ast.ExprNode

sealed class TransitUpdate {
    abstract fun transitRootVar(): String

    data class Assign(val key: String, val expr: ExprNode) : TransitUpdate() {
        override fun transitRootVar(): String = julay.program.transitRootVar(key)
    }

    data class MapPut(val mapVar: String, val key: ExprNode, val value: ExprNode) : TransitUpdate() {
        override fun transitRootVar(): String = mapVar
    }
}
