package julay.program.type

/**
 * Built-in parametric list type.
 */
data class ListType(val elementType: Type) : Type {
    override fun isOfType(obj: Any): Boolean {
        if (obj !is List<*>) return false
        return obj.all { it != null && elementType.isOfType(it) }
    }

    override fun toString(): String = "List<${elementType}>"
}

fun listType(element: Type): ListType = ListType(element)
