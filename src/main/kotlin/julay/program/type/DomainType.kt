package julay.program.type

import com.microsoft.z3.Context
import com.microsoft.z3.Expr
import com.microsoft.z3.Model
import julay.program.Value
import julay.program.Variable

enum class DomainKind {
    /** `type Name := Carrier` — erases to carrier in procs; separate TLA+ CONSTANT in specs. */
    Typedef,
    /** `type Name` — spec-only opaque domain; requires a delayed model when used in a spec compile. */
    Uninterpreted,
}

/**
 * Named domain from `type Name` (uninterpreted) or `type Name := Carrier` (typedef).
 * Finite set for TLC comes from a delayed model (`Name := { ... }`) when present.
 */
class DomainType(
    val name: String,
    val kind: DomainKind,
    /** Carrier / element type (from typedef alias or homogeneous model literals). */
    val carrierType: Type,
    /** Canonical element texts for TLA+ .cfg; null until a delayed model is assigned. */
    val cfgElements: List<String>? = null,
) : Type {
    val hasModel: Boolean get() = cfgElements != null

    override fun toZ3Expr(variable: Variable, ctx: Context): Expr<*> {
        throw RuntimeException("type \"$name\" is not executable")
    }

    override fun toZ3Expr(value: Value, ctx: Context): Expr<*> {
        throw RuntimeException("type \"$name\" is not executable")
    }

    override fun fromZ3Expr(expr: Expr<*>, model: Model): Any {
        throw RuntimeException("type \"$name\" is not executable")
    }

    override fun isOfType(obj: Any): Boolean = false

    override fun toString(): String = name

    fun withModel(elementType: Type, cfgElements: List<String>): DomainType =
        DomainType(name, kind, elementType, cfgElements)

    override fun equals(other: Any?): Boolean =
        other is DomainType && other.name == name &&
            other.kind == kind &&
            other.carrierType == carrierType &&
            other.cfgElements == cfgElements

    override fun hashCode(): Int = name.hashCode()
}

/** True when [type] nests an uninterpreted domain (JAR-forbidden), not a typedef. */
fun Type.containsUninterpretedType(): Boolean = when (this) {
    is DomainType -> kind == DomainKind.Uninterpreted
    is ListType -> elementType.containsUninterpretedType()
    is SetType -> elementType.containsUninterpretedType()
    is MapType -> keyType.containsUninterpretedType() || valueType.containsUninterpretedType()
    is ObjClassType -> fields.any { it.type.containsUninterpretedType() }
    else -> false
}

/** @deprecated Use [containsUninterpretedType] for JAR checks; kept for transitional call sites. */
fun Type.containsSortType(): Boolean = containsUninterpretedType()

fun Type.isDirectUninterpretedDomain(): Boolean = when (this) {
    is DomainType -> kind == DomainKind.Uninterpreted
    is ListType -> elementType.isDirectUninterpretedDomain()
    is SetType -> elementType.isDirectUninterpretedDomain()
    is MapType -> keyType.isDirectUninterpretedDomain() || valueType.isDirectUninterpretedDomain()
    else -> false
}

/** Typedef domains are JAR-legal (they erase to the carrier). */
fun Type.isDirectDomainType(): Boolean = when (this) {
    is DomainType -> true
    is ListType -> elementType.isDirectDomainType()
    is SetType -> elementType.isDirectDomainType()
    is MapType -> keyType.isDirectDomainType() || valueType.isDirectDomainType()
    else -> false
}

fun domainOnlyError(type: Type, loc: julay.compiler.ProgramLoc): julay.compiler.CompileError? {
    if (!type.isDirectUninterpretedDomain()) return null
    val domainName = firstUninterpretedName(type) ?: type.toString()
    return julay.compiler.OneLocCompileError(
        loc,
        "type \"$domainName\" can only be used as a spec or quantifier domain",
    )
}

fun firstUninterpretedName(type: Type): String? = when (type) {
    is DomainType -> if (type.kind == DomainKind.Uninterpreted) type.name else null
    is ListType -> firstUninterpretedName(type.elementType)
    is SetType -> firstUninterpretedName(type.elementType)
    is MapType -> firstUninterpretedName(type.keyType) ?: firstUninterpretedName(type.valueType)
    else -> null
}

/** Erase typedef domains to their carrier for JAR/Z3 codegen. */
fun Type.codegenErasure(): Type = when (this) {
    is DomainType -> when (kind) {
        DomainKind.Typedef -> carrierType.codegenErasure()
        DomainKind.Uninterpreted ->
            throw RuntimeException("type \"$name\" must not reach Kotlin codegen (uninterpreted types are specs/TLA+ only)")
    }
    is ListType -> listType(elementType.codegenErasure())
    is SetType -> setType(elementType.codegenErasure())
    is MapType -> mapType(keyType.codegenErasure(), valueType.codegenErasure())
    else -> this
}

/** TLA+ erasure name for a builtin carrier used in typedef cfg aliasing. */
fun carrierTlaName(type: Type): String? = when (type) {
    is StringType -> "String"
    is IntType -> "Int"
    is BoolType -> "BOOLEAN"
    else -> null
}
