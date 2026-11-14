package julay.program.type

/**
 * Built-in parametric set type.
 */
data class SetType(val elementType: Type) : Type {
    override fun isOfType(obj: Any): Boolean {
        if (obj !is Set<*>) return false
        return obj.all { it != null && elementType.isOfType(it) }
    }

    override fun toString(): String = "Set<${elementType}>"
}

fun setType(element: Type): SetType = SetType(element)
