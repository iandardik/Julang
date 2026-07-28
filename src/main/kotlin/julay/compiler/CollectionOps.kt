package julay.compiler

import julay.program.type.ListType
import julay.program.type.MapType
import julay.program.type.SetType
import julay.program.type.Type
import julay.program.type.intType
import julay.program.type.listType
import julay.program.type.setType

/** Intrinsic collection properties: `.keys`, `.length`. */
sealed interface CollectionPropResult {
    data class Resolved(val type: Type) : CollectionPropResult
    data class Error(val message: String) : CollectionPropResult
    data object NotCollectionProp : CollectionPropResult
}

fun resolveCollectionProperty(baseType: Type, propName: String): CollectionPropResult {
    return when (propName) {
        "length" -> when (baseType) {
            is ListType, is SetType, is MapType -> CollectionPropResult.Resolved(intType)
            else -> CollectionPropResult.NotCollectionProp
        }
        "keys" -> when (baseType) {
            is MapType -> CollectionPropResult.Resolved(setType(baseType.keyType))
            else -> CollectionPropResult.Error(
                "Cannot access property \"keys\" on non-Map type $baseType",
            )
        }
        else -> CollectionPropResult.NotCollectionProp
    }
}

/**
 * Resolve a path of collection properties (e.g. `keys`, `keys.length`).
 * Returns [CollectionPropResult.NotCollectionProp] if the first segment is not a collection prop
 * (so obj field resolution can run).
 */
fun resolveCollectionPropertyPath(rootType: Type, path: List<String>): CollectionPropResult {
    if (path.isEmpty()) {
        return CollectionPropResult.Error("Expected at least one property name")
    }
    var current = rootType
    for ((i, segment) in path.withIndex()) {
        when (val r = resolveCollectionProperty(current, segment)) {
            is CollectionPropResult.Resolved -> current = r.type
            is CollectionPropResult.Error -> return r
            is CollectionPropResult.NotCollectionProp -> {
                return if (i == 0) {
                    CollectionPropResult.NotCollectionProp
                } else {
                    CollectionPropResult.Error(
                        "Cannot access property \"$segment\" on type $current",
                    )
                }
            }
        }
    }
    return CollectionPropResult.Resolved(current)
}

sealed interface CollectionMethodKind {
    data object Filter : CollectionMethodKind
    data object Map : CollectionMethodKind
    data object Fold : CollectionMethodKind
}

fun collectionMethodKind(name: String): CollectionMethodKind? = when (name) {
    "filter" -> CollectionMethodKind.Filter
    "map" -> CollectionMethodKind.Map
    "fold" -> CollectionMethodKind.Fold
    else -> null
}

fun collectionElementType(collType: Type): Type? = when (collType) {
    is ListType -> collType.elementType
    is SetType -> collType.elementType
    else -> null
}

fun mapResultCollectionType(collType: Type, elemResult: Type): Type? = when (collType) {
    is ListType -> listType(elemResult)
    is SetType -> setType(elemResult)
    else -> null
}

fun sameCollectionType(collType: Type): Type? = when (collType) {
    is ListType, is SetType -> collType
    else -> null
}
