package julay.compiler.pass

import julay.program.type.*

/**
 * Snowcat / Apalache `@type` strings derived from Julay [Type] and from TypeOK-style
 * TLA membership RHS. Comments are ignored by TLC.
 */

/**
 * Wrap a value Apalache type under create-index / DynProc binders.
 * Domains are string-literal sorts in emitted models (`NodeSet`, `DynProc`, …).
 */
internal fun indexedApalacheType(
    valueType: String,
    paramDomain: String?,
    instDomain: String? = null,
): String {
    val v = parenIfArrow(valueType)
    return when {
        paramDomain != null && instDomain != null -> "Str -> (Str -> $v)"
        paramDomain != null -> "Str -> $v"
        else -> valueType
    }
}

/** Apalache type for a Julay [Type] (values / params). */
internal fun julayTypeToApalache(type: Type): String = when (type) {
    is BoolType -> "Bool"
    is IntType, is RealType -> "Int"
    is StringType -> "Str"
    is DomainType -> if (tlaElemIsInt(type.carrierType)) "Int" else "Str"
    // Julay list map/comprehension emits `[i \in DOMAIN xs |-> …]` (Int ->), not Seq.
    is ListType -> {
        val elem = parenIfArrow(julayTypeToApalache(type.elementType))
        "Int -> $elem"
    }
    is SetType -> "Set(${julayTypeToApalache(type.elementType)})"
    is MapType -> {
        val k = julayTypeToApalache(type.keyType)
        val v = parenIfArrow(julayTypeToApalache(type.valueType))
        "$k -> $v"
    }
    is ObjClassType -> {
        val proj = TlaFieldProjection.get()
        val single = proj.singletonField(type)
        if (single != null) {
            julayTypeToApalache(single.type)
        } else {
            val fields = proj.fieldsFor(type)
            if (fields.isEmpty()) {
                "{dummy: Int}"
            } else {
                val rendered = fields.joinToString(", ") { f ->
                    "${f.name}: ${julayTypeToApalache(f.type)}"
                }
                "{$rendered}"
            }
        }
    }
    else -> "Str"
}

/** Apalache type for a module CONSTANT (the constant's value, e.g. `Set(Str)`). */
internal fun constantToApalacheType(name: String, domains: Map<String, DomainType>): String {
    if (name == "MaxListLen") return "Int"
    if (name == DYN_PROC_MODEL) return "Set(Str)"
    domains[name]?.let { d ->
        return when {
            tlaElemIsInt(d.carrierType) -> "Set(Int)"
            d.carrierType is BoolType -> "Set(Bool)"
            else -> "Set(Str)"
        }
    }
    // Typedef sorts modeled as string literals (Key, NodeSet, String, Value, …).
    return "Set(Str)"
}

/** Apalache type for a state/fluent variable from its TypeOK membership RHS. */
internal fun typeOkRhsToApalacheType(rhs: String): String {
    val t = rhs.trim()
    return when {
        t == "BOOLEAN" -> "Bool"
        t == "String" || t == "DynProc" -> "Str"
        t == "Int" || t == "Nat" || t == "TypeOKInt" || t == "Real" -> "Int"
        t.startsWith("SUBSET ") -> "Set(${typeOkRhsToApalacheType(t.removePrefix("SUBSET ").trim())})"
        t.startsWith("BoundedSeq(") && t.endsWith(")") -> {
            val inner = t.removePrefix("BoundedSeq(").removeSuffix(")")
            val elem = splitTopLevelComma(inner).firstOrNull()?.trim() ?: "Str"
            val elemT = parenIfArrow(typeOkRhsToApalacheType(elem))
            "Int -> $elemT"
        }
        t.startsWith("Seq(") && t.endsWith(")") -> {
            val elemT = parenIfArrow(
                typeOkRhsToApalacheType(t.removePrefix("Seq(").removeSuffix(")")),
            )
            "Int -> $elemT"
        }
        t.startsWith("[") && t.endsWith("]") && " -> " in t && ":" !in t.substringBefore(" -> ") -> {
            val inner = t.removePrefix("[").removeSuffix("]")
            val (dom, cod) = splitArrow(inner)
            val domT = domainProductToApalache(dom.trim())
            val codT = parenIfArrow(typeOkRhsToApalacheType(cod.trim()))
            "$domT -> $codT"
        }
        t.startsWith("[") && t.endsWith("]") && ":" in t -> {
            val fields = splitTopLevelComma(t.removePrefix("[").removeSuffix("]"))
            val rendered = fields.joinToString(", ") { f ->
                val (name, ty) = f.split(":", limit = 2)
                "${name.trim()}: ${typeOkRhsToApalacheType(ty.trim())}"
            }
            "{$rendered}"
        }
        // Named sort used as element type (NodeSet, Key, Value, …).
        else -> "Str"
    }
}

/**
 * Apalache type for an action/operator parameter from its TLA `\E` domain string.
 * [varApalacheTypes] maps VARIABLES to their Apalache types so `\E m \in claimMsgs`
 * resolves to the set element record type.
 */
internal fun paramDomainToApalacheType(
    domain: String,
    varApalacheTypes: Map<String, String> = emptyMap(),
): String {
    val d = domain.trim()
    if (d.startsWith("1..") || d.startsWith("0..")) return "Int"
    // `\E entries \in BoundedSeq(...)` — element is Int -> a (function encoding).
    if (d.startsWith("BoundedSeq(")) {
        val inner = d.removePrefix("BoundedSeq(").removeSuffix(")")
        val elem = splitTopLevelComma(inner).firstOrNull()?.trim() ?: "Str"
        val elemT = parenIfArrow(typeOkRhsToApalacheType(elem))
        return "Int -> $elemT"
    }
    if (d.startsWith("SUBSET ")) {
        return typeOkRhsToApalacheType(d)
    }
    varApalacheTypes[d]?.let { vt ->
        unwrapSetElemType(vt)?.let { return it }
    }
    // Record / function domain spelled like TypeOK RHS.
    if (d.startsWith("[") || d.startsWith("Seq(")) {
        return typeOkRhsToApalacheType(d)
    }
    return typeOkRhsToApalacheType(d)
}

private fun unwrapSetElemType(apalacheType: String): String? {
    val t = apalacheType.trim()
    if (!t.startsWith("Set(") || !t.endsWith(")")) return null
    return t.removePrefix("Set(").removeSuffix(")")
}

/** Snowcat annotation line, or null when `--opt-tla=apalache` is off. */
internal fun apalacheTypeComment(typeSig: String, annotate: Boolean = TlaEmitOpts.get().apalache): String? =
    if (annotate) "\\* @type: $typeSig;" else null

internal fun emitApalacheAnnotatedConstants(
    constants: Collection<String>,
    domains: Map<String, DomainType>,
    annotate: Boolean = TlaEmitOpts.get().apalache,
): String {
    if (constants.isEmpty()) return ""
    val list = constants.toList()
    return buildString {
        appendLine("CONSTANT")
        list.forEachIndexed { i, c ->
            if (annotate) {
                appendLine("  \\* @type: ${constantToApalacheType(c, domains)};")
            }
            appendLine(if (i < list.lastIndex) "  $c," else "  $c")
        }
        appendLine()
    }
}

internal fun emitApalacheAnnotatedVariables(
    variables: List<String>,
    typeByVar: Map<String, String>,
    annotate: Boolean = TlaEmitOpts.get().apalache,
): String {
    if (variables.isEmpty()) {
        return if (annotate) {
            "VARIABLES\n  \\* @type: Int;\n  dummy\n\n"
        } else {
            "VARIABLES\n  dummy\n\n"
        }
    }
    return buildString {
        appendLine("VARIABLES")
        variables.forEachIndexed { i, v ->
            if (annotate) {
                val ty = typeByVar[v] ?: "Str"
                appendLine("  \\* @type: $ty;")
            }
            appendLine(if (i < variables.lastIndex) "  $v," else "  $v")
        }
        appendLine()
    }
}

/**
 * Bounded sequences as functions `1..k -> S` (TLC+Apalache). Prefer this over
 * Append-expanded `Seq` so list `map` comprehensions (`[i \in DOMAIN xs |-> …]`) unify.
 */
internal fun emitApalacheBoundedSeqDef(annotate: Boolean = TlaEmitOpts.get().apalache): String {
    val ann = apalacheTypeComment("(Set(a), Int) => Set(Int -> a)", annotate)
    val body = "BoundedSeq(S, N) == UNION { [1..k -> S] : k \\in 0..N }"
    return if (ann != null) "$ann\n$body" else body
}

internal fun emitApalacheRangeDef(
    reservedNames: Set<String>,
    annotate: Boolean = TlaEmitOpts.get().apalache,
): String {
    val taken = reservedNames.toMutableSet()
    val f = allocTlaName("f", taken)
    val i = allocTlaName("__i", taken)
    val body = "Range($f) == { $f[$i] : $i \\in DOMAIN $f }"
    val ann = apalacheTypeComment("((Int -> a) => Set(a))", annotate)
    return if (ann != null) "$ann\n$body" else body
}

/** `@type: (T1, T2, ...) => Bool;` line for an action operator (null if Apalache off). */
internal fun emitActionTypeAnnotation(
    paramApalacheTypes: List<String>,
    annotate: Boolean = TlaEmitOpts.get().apalache,
): String? {
    if (!annotate || paramApalacheTypes.isEmpty()) return null
    val args = paramApalacheTypes.joinToString(", ")
    return "\\* @type: ($args) => Bool;"
}

internal fun parseTypeOkConjuncts(typeOkParts: List<String>): Map<String, String> {
    val out = linkedMapOf<String, String>()
    typeOkParts.forEach { raw ->
        val line = raw.trim().removePrefix("/\\ ").trim()
        val idx = line.indexOf(" \\in ")
        if (idx < 0) return@forEach
        val name = line.substring(0, idx).trim()
        val rhs = line.substring(idx + 5).trim()
        if (name.isNotEmpty()) out[name] = typeOkRhsToApalacheType(rhs)
    }
    return out
}

private fun domainProductToApalache(dom: String): String {
    if ("\\X" in dom) {
        val parts = dom.split("\\X").map { typeOkRhsToApalacheType(it.trim()) }
        return "<<${parts.joinToString(", ")}>>"
    }
    return typeOkRhsToApalacheType(dom)
}

private fun parenIfArrow(t: String): String =
    if (" -> " in t && !t.startsWith("(") && !t.startsWith("{") && !t.startsWith("<<") &&
        !t.startsWith("Set(") && !t.startsWith("Seq(")
    ) {
        "($t)"
    } else {
        t
    }

private fun splitArrow(s: String): Pair<String, String> {
    var depth = 0
    for (i in 0 until s.length - 3) {
        when (s[i]) {
            '[', '(', '{' -> depth++
            ']', ')', '}' -> depth--
        }
        if (depth == 0 && s.startsWith(" -> ", i)) {
            return s.substring(0, i) to s.substring(i + 4)
        }
    }
    error("no arrow in $s")
}

internal fun splitTopLevelComma(s: String): List<String> {
    val parts = mutableListOf<String>()
    val cur = StringBuilder()
    var depth = 0
    for (ch in s) {
        when (ch) {
            '[', '(', '{' -> {
                depth++
                cur.append(ch)
            }
            ']', ')', '}' -> {
                depth--
                cur.append(ch)
            }
            ',' -> if (depth == 0) {
                parts += cur.toString().trim()
                cur.clear()
            } else {
                cur.append(ch)
            }
            else -> cur.append(ch)
        }
    }
    if (cur.isNotEmpty()) parts += cur.toString().trim()
    return parts.filter { it.isNotEmpty() }
}
