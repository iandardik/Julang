package julay.compiler

import julay.program.*
import julay.program.type.*
import julay.program.action.*

sealed interface FieldPathResult {
    data class Resolved(val type: Type, val relPath: String) : FieldPathResult
    data class Error(val message: String) : FieldPathResult
}

fun resolveFieldPath(rootType: Type, path: List<String>): FieldPathResult {
    if (path.isEmpty()) {
        return FieldPathResult.Error("Expected at least one field name in field access")
    }
    var current: Type = rootType
    val pathParts = mutableListOf<String>()
    for (segment in path) {
        if (current !is ObjClassType) {
            return FieldPathResult.Error("Cannot access field \"$segment\" on non o-class type $current")
        }
        val field = current.fields.find { it.name == segment }
        if (field == null) {
            return FieldPathResult.Error("Unknown field \"$segment\" on o-class ${current.name}")
        }
        pathParts.add(segment)
        current = field.type
    }
    return FieldPathResult.Resolved(current, pathParts.joinToString("."))
}
