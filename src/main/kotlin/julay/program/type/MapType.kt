package julay.program.type

/**
 * Built-in parametric map type.
 */
data class MapType(val keyType: Type, val valueType: Type) : Type {
    override fun isOfType(obj: Any): Boolean {
        if (obj !is Map<*, *>) return false
        return obj.all { (k, v) ->
            k != null && v != null && keyType.isOfType(k) && valueType.isOfType(v)
        }
    }

    override fun toString(): String = "Map<${keyType}, ${valueType}>"
}

fun mapType(key: Type, value: Type): MapType = MapType(key, value)
