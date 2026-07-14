package julay.program

/**
 * Immutable SMT-LIB datatype declaration owned by a [Type] (not keyed by TermManager).
 */
data class SerializedDatatype(
    val name: String,
    val constructorName: String,
    val declareSmtLib: String,
)

/** Escape an SMT-LIB symbol (simple or quoted). */
fun smtLibSymbol(name: String): String =
    if (name.matches(Regex("[a-zA-Z~!@$%^&*_+=<>.?/-][0-9a-zA-Z~!@$%^&*_+=<>.?/-]*"))) {
        name
    } else {
        "|${name.replace("|", "\\|")}|"
    }

/** SMT-LIB sort expression for this type (no TermManager). */
fun Type.toSmtLibSort(): String = when (this) {
    is BoolType -> "Bool"
    is IntType -> "Int"
    is RealType -> "Real"
    is StringType -> "String"
    is ObjClassType -> smtLibSymbol(name)
    is ListType -> "(Seq ${elementType.toSmtLibSort()})"
    is SetType -> smtLibSymbol(cellName)
    is MapType -> smtLibSymbol(cellName)
    is TypeVar -> throw RuntimeException("TypeVar \"$name\" must not reach SMT-LIB sort emission")
    else -> throw RuntimeException("Cannot emit SMT-LIB sort for type $this")
}

/** Ordered datatype declare strings for this type and nested datatypes (deduped). */
fun Type.smtLibDeclarations(): List<String> {
    val decls = linkedMapOf<String, String>()
    collectSmtLibDeclarations(decls)
    return decls.values.toList()
}

fun Type.collectSmtLibDeclarations(decls: MutableMap<String, String>) {
    when (this) {
        is MapType -> {
            keyType.collectSmtLibDeclarations(decls)
            valueType.collectSmtLibDeclarations(decls)
            decls.putIfAbsent(cellName, serializedDatatype.declareSmtLib)
        }
        is SetType -> {
            elementType.collectSmtLibDeclarations(decls)
            decls.putIfAbsent(cellName, serializedDatatype.declareSmtLib)
        }
        is ObjClassType -> {
            val schema = serializedDatatype ?: return
            for (field in fields) {
                field.type.collectSmtLibDeclarations(decls)
            }
            decls.putIfAbsent(name, schema.declareSmtLib)
        }
        is ListType -> elementType.collectSmtLibDeclarations(decls)
        else -> {}
    }
}

internal fun buildDatatypeDeclare(
    name: String,
    constructorName: String,
    selectors: List<Pair<String, String>>,
): SerializedDatatype {
    val sels = selectors.joinToString(" ") { (selName, sort) ->
        "(${smtLibSymbol(selName)} $sort)"
    }
    val ctorBody = if (sels.isEmpty()) {
        "(${smtLibSymbol(constructorName)})"
    } else {
        "(${smtLibSymbol(constructorName)} $sels)"
    }
    val declare = "(declare-datatypes ((${smtLibSymbol(name)} 0)) (($ctorBody)))"
    return SerializedDatatype(name, constructorName, declare)
}
