package julay.compiler.decl

import julay.compiler.ast.ExprNode
import julay.program.type.Type

sealed class TransitUpdate {
    abstract fun transitRootVar(): String

    data class Assign(val key: String, val expr: ExprNode) : TransitUpdate() {
        override fun transitRootVar(): String = julay.program.type.transitRootVar(key)
    }

    data class IndexPut(val collectionVar: String, val index: ExprNode, val value: ExprNode) : TransitUpdate() {
        override fun transitRootVar(): String = collectionVar
    }

    /** Transit-scoped temporary; evaluated against pre-state (+ earlier lets), not process state. */
    data class Let(val name: String, val type: Type, val init: ExprNode) : TransitUpdate() {
        override fun transitRootVar(): String = name
    }
}
