package julay.compiler

import julay.compiler.decl.containsTypeVar
import julay.program.*

sealed interface UnifyResult {
    data class Ok(val subst: Map<String, Type>) : UnifyResult
    data class Fail(val message: String) : UnifyResult
}

/**
 * Unify [schema] (may contain [TypeVar]s) against [concrete] (should be closed),
 * producing a substitution for type parameters.
 */
fun unifyTypes(schema: Type, concrete: Type, subst: MutableMap<String, Type> = mutableMapOf()): UnifyResult {
    when (schema) {
        is TypeVar -> {
            val existing = subst[schema.name]
            if (existing != null) {
                return if (existing == concrete) {
                    UnifyResult.Ok(subst)
                } else {
                    UnifyResult.Fail(
                        "Type parameter \"${schema.name}\" cannot be both $existing and $concrete",
                    )
                }
            }
            if (concrete.containsTypeVarRef(schema.name)) {
                return UnifyResult.Fail("Occurs check failed for type parameter \"${schema.name}\"")
            }
            subst[schema.name] = concrete
            return UnifyResult.Ok(subst)
        }
        is BoolType, is IntType, is RealType, is StringType -> {
            return if (schema == concrete) {
                UnifyResult.Ok(subst)
            } else {
                UnifyResult.Fail("Expected $schema but got $concrete")
            }
        }
        is ObjClassType -> {
            if (concrete !is ObjClassType) {
                return UnifyResult.Fail("Expected $schema but got $concrete")
            }
            if (schema.containsTypeVar()) {
                if (schema.fields.size != concrete.fields.size) {
                    return UnifyResult.Fail("Expected $schema but got $concrete")
                }
                schema.fields.zip(concrete.fields).forEach { (sf, cf) ->
                    if (sf.name != cf.name) {
                        return UnifyResult.Fail("Expected field \"${sf.name}\" but got \"${cf.name}\"")
                    }
                    when (val r = unifyTypes(sf.type, cf.type, subst)) {
                        is UnifyResult.Fail -> return r
                        is UnifyResult.Ok -> {}
                    }
                }
                return UnifyResult.Ok(subst)
            }
            if (schema.name != concrete.name) {
                return UnifyResult.Fail("Expected $schema but got $concrete")
            }
            return UnifyResult.Ok(subst)
        }
        is ListType -> {
            if (concrete !is ListType) {
                return UnifyResult.Fail("Expected $schema but got $concrete")
            }
            return unifyTypes(schema.elementType, concrete.elementType, subst)
        }
        else -> return UnifyResult.Fail("Cannot unify $schema with $concrete")
    }
}

private fun Type.containsTypeVarRef(name: String): Boolean = when (this) {
    is TypeVar -> this.name == name
    is ObjClassType -> fields.any { it.type.containsTypeVarRef(name) }
    is ListType -> elementType.containsTypeVarRef(name)
    else -> false
}
