package julay.compiler.pass

import julay.compiler.FunBuiltinRegistry
import julay.compiler.ast.*
import julay.compiler.decl.mangleTypeForName
import julay.compiler.isDiscardBinding
import julay.program.type.*

/**
 * Context for emitting Julay expressions as Go source.
 *
 * [stateVars] are fields on the proc receiver ([procRecv], usually `p`).
 * [argBindings] maps action-argument names to Go expressions (locals or message fields).
 * [letBindings] maps let-bound names to Go identifiers already in scope.
 */
data class GoEmitCtx(
    val stateVars: Set<String> = emptySet(),
    val argBindings: Map<String, String> = emptyMap(),
    val letBindings: Map<String, String> = emptyMap(),
    val procRecv: String = "p",
    val insideGuard: Boolean = false,
    /** When true, string concat of non-strings uses [julay.ToStringOf]. */
    val preferToString: Boolean = false,
) {
    fun withArgBindings(extra: Map<String, String>): GoEmitCtx =
        copy(argBindings = argBindings + extra)

    fun withLet(name: String, goIdent: String): GoEmitCtx =
        copy(letBindings = letBindings + (name to goIdent))

    fun withGuard(inside: Boolean = true): GoEmitCtx = copy(insideGuard = inside)
}

/** Map a Julay [Type] to a Go type string. */
fun Type.toGoTypeString(): String = when (this) {
    is BoolType -> "bool"
    is IntType -> "int"
    is RealType -> "float64"
    is StringType -> "string"
    is ObjClassType -> when (name) {
        "HttpServerRequest" -> "julay.HttpServerRequest"
        "HttpServerResponse" -> "julay.HttpServerResponse"
        "HttpClientRequest" -> "julay.HttpClientRequest"
        "HttpClientResponse" -> "julay.HttpClientResponse"
        else -> name
    }
    is ListType -> "julay.List[${elementType.toGoTypeString()}]"
    is SetType -> "julay.Set[${elementType.toGoTypeString()}]"
    is MapType -> "julay.Map[${keyType.toGoTypeString()}, ${valueType.toGoTypeString()}]"
    is DomainType -> when (kind) {
        DomainKind.Typedef -> carrierType.toGoTypeString()
        DomainKind.Uninterpreted ->
            throw RuntimeException("uninterpreted type \"$name\" cannot reach Go codegen")
    }
    is TypeVar -> throw RuntimeException("TypeVar \"$name\" must not reach Go codegen")
    is ProcFunRefType -> "string"
    else -> throw RuntimeException("Unsupported Go type: $this")
}

fun String.toGoExportedIdent(): String =
    if (isEmpty()) this
    else replaceFirstChar { if (it.isLowerCase()) it.titlecaseChar() else it }

fun String.toGoSafeIdent(): String {
    val sanitized = replace(Regex("[^A-Za-z0-9_]"), "_")
    return if (sanitized.isEmpty() || sanitized[0].isDigit()) "_$sanitized" else sanitized
}

fun String.escapeGoStringLiteral(): String =
    buildString {
        for (ch in this@escapeGoStringLiteral) {
            when (ch) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(ch)
            }
        }
    }

/** Emit [this] as a Go expression string under [ctx]. */
fun ExprNode.toGoString(ctx: GoEmitCtx): String = when (this) {
    is LiteralValueExprNode -> literalToGo()
    is SymbolValueExprNode -> symbolToGo(ctx)
    is UnaryOpExprNode -> unaryToGo(ctx)
    is ParenExprNode -> "(${innerExpr().toGoString(ctx)})"
    is BinaryOpExprNode -> binaryToGo(ctx)
    is IfElseExprNode -> ifElseToGo(ctx)
    is LetExprNode -> letToGo(ctx)
    is WhenExprNode -> whenToGo(ctx)
    is ObjClassLiteralExprNode -> objLiteralToGo(ctx)
    is ListLiteralExprNode -> listLiteralToGo(ctx)
    is SetLiteralExprNode -> setLiteralToGo(ctx)
    is MapLiteralExprNode -> mapLiteralToGo(ctx)
    is IndexExprNode -> indexToGo(ctx)
    is ThisAccessExprNode -> thisAccessToGo(ctx)
    is FieldAccessExprNode -> fieldAccessToGo(ctx)
    is MemberAccessExprNode -> memberAccessToGo(ctx)
    is FieldAccessOnExprNode -> fieldAccessOnExprToGo(ctx)
    is FunCallExprNode -> funCallToGo(ctx)
    is MethodCallExprNode -> methodCallToGo(ctx)
    is LambdaExprNode ->
        throw RuntimeException("Lambda cannot be emitted as a standalone Go expression at ${programLocation()}")
    is QuantifiedExprNode ->
        throw RuntimeException("Quantified expressions are not yet supported in Go codegen at ${programLocation()}")
    else ->
        throw RuntimeException("Unsupported expression for Go codegen: ${this::class.simpleName} at ${programLocation()}")
}

private fun LiteralValueExprNode.literalToGo(): String {
    val text = literalText()
    return when (getType()) {
        is StringType -> "\"${text.escapeGoStringLiteral()}\""
        is BoolType -> text // true / false
        is IntType -> text
        is RealType -> if ('.' in text) text else "$text.0"
        else -> text
    }
}

private fun SymbolValueExprNode.symbolToGo(ctx: GoEmitCtx): String {
    ctx.letBindings[symbol]?.let { return it }
    ctx.argBindings[symbol]?.let { return it }
    if (symbol in ctx.stateVars) {
        return "${ctx.procRecv}.$symbol"
    }
    try {
        if (getType() is ProcFunRefType) {
            return "\"${symbol.escapeGoStringLiteral()}\""
        }
    } catch (_: RuntimeException) {
    }
    // Fall back to bare ident (ctor args, etc.)
    return symbol.toGoSafeIdent()
}

private fun UnaryOpExprNode.unaryToGo(ctx: GoEmitCtx): String {
    val inner = operand().toGoString(ctx)
    return when (op()) {
        "~" -> "!($inner)"
        else -> throw RuntimeException("Invalid unary op \"${op()}\" at ${programLocation()}")
    }
}

private fun BinaryOpExprNode.binaryToGo(ctx: GoEmitCtx): String {
    val lhs = lhsOperand().toGoString(ctx)
    val rhs = rhsOperand().toGoString(ctx)
    val lhsType = try {
        lhsOperand().getType()
    } catch (_: RuntimeException) {
        null
    }
    val rhsType = try {
        rhsOperand().getType()
    } catch (_: RuntimeException) {
        null
    }

    return when (op()) {
        "=" -> equalityGo(lhs, rhs, lhsType, rhsType)
        "#", "~=" -> {
            val eq = equalityGo(lhs, rhs, lhsType, rhsType)
            "!($eq)"
        }
        "&" -> "(($lhs) && ($rhs))"
        "|" -> "(($lhs) || ($rhs))"
        "=>" -> "(!($lhs) || ($rhs))"
        "+" -> when {
            lhsType is StringType || rhsType is StringType -> {
                // Elide empty-string concat identity (`e + ""` / `"" + e` coerce).
                when {
                    isEmptyStringLiteral(lhsOperand()) && isEmptyStringLiteral(rhsOperand()) -> "\"\""
                    isEmptyStringLiteral(lhsOperand()) ->
                        if (rhsType is StringType) rhs else "julay.ToStringOf($rhs)"
                    isEmptyStringLiteral(rhsOperand()) ->
                        if (lhsType is StringType) lhs else "julay.ToStringOf($lhs)"
                    else -> {
                        val l = if (lhsType is StringType) lhs else "julay.ToStringOf($lhs)"
                        val r = if (rhsType is StringType) rhs else "julay.ToStringOf($rhs)"
                        "($l + $r)"
                    }
                }
            }
            lhsType is ListType && rhsType is ListType -> "($lhs).Concat($rhs)"
            lhsType is SetType && rhsType is SetType -> "($lhs).Union($rhs)"
            else -> "($lhs + $rhs)"
        }
        "-" -> when {
            lhsType is SetType && rhsType is SetType -> "($lhs).Diff($rhs)"
            else -> "($lhs - $rhs)"
        }
        "*", "/", "%", "<", "<=", ">", ">=" -> "($lhs ${op()} $rhs)"
        "in" -> membershipGo(lhs, rhs, rhsType, negated = false)
        "~in" -> membershipGo(lhs, rhs, rhsType, negated = true)
        else -> "($lhs ${op()} $rhs)"
    }
}

private fun equalityGo(lhs: String, rhs: String, lhsType: Type?, rhsType: Type?): String {
    val complex = lhsType is ObjClassType || lhsType is ListType || lhsType is SetType || lhsType is MapType ||
        rhsType is ObjClassType || rhsType is ListType || rhsType is SetType || rhsType is MapType
    return if (complex) {
        "julay.Equal($lhs, $rhs)"
    } else {
        "(($lhs) == ($rhs))"
    }
}

private fun membershipGo(elem: String, coll: String, collType: Type?, negated: Boolean): String {
    val call = when (collType) {
        is ListType, is SetType -> "($coll).Contains($elem)"
        is MapType -> "($coll).ContainsKey($elem)"
        else -> "($coll).Contains($elem)"
    }
    return if (negated) "!($call)" else call
}

private fun IfElseExprNode.ifElseToGo(ctx: GoEmitCtx): String {
    val ty = getType().toGoTypeString()
    val c = condExpr().toGoString(ctx)
    val t = thenExpr().toGoString(ctx)
    val e = elseExpr().toGoString(ctx)
    return "func() $ty { if $c { return $t }; return $e }()"
}

private fun LetExprNode.letToGo(ctx: GoEmitCtx): String {
    val ty = getType().toGoTypeString()
    val bind = if (letName().isDiscardBinding()) {
        "__discard_let"
    } else {
        "${letName().toGoSafeIdent()}__let"
    }
    val init = letInitExpr().toGoString(ctx)
    val bodyCtx = if (letName().isDiscardBinding()) ctx else ctx.withLet(letName(), bind)
    val body = bodyExpr().toGoString(bodyCtx)
    return "func() $ty { $bind := $init; return $body }()"
}

private fun WhenExprNode.whenToGo(ctx: GoEmitCtx): String {
    val ty = getType().toGoTypeString()
    val subject = subjectExpr()
    return if (subject != null) {
        val subj = subject.toGoString(ctx)
        buildString {
            append("func() $ty {\n")
            append("__subj := ($subj)\n")
            var first = true
            for (arm in arms()) {
                when (arm) {
                    is WhenArm.Subject -> {
                        val pat = when (val p = arm.pattern) {
                            is WhenPattern.Primitive -> whenLiteralToGo(p.literal)
                            is WhenPattern.Struct -> p.literal.toGoString(ctx)
                        }
                        if (first) {
                            append("if julay.Equal(__subj, $pat) {\n")
                            first = false
                        } else {
                            append("} else if julay.Equal(__subj, $pat) {\n")
                        }
                        append("return ${arm.expr.toGoString(ctx)}\n")
                    }
                    is WhenArm.Guard -> {
                        if (first) {
                            append("if ${arm.cond.toGoString(ctx)} {\n")
                            first = false
                        } else {
                            append("} else if ${arm.cond.toGoString(ctx)} {\n")
                        }
                        append("return ${arm.expr.toGoString(ctx)}\n")
                    }
                    is WhenArm.Else -> {
                        append("} else {\n")
                        append("return ${arm.expr.toGoString(ctx)}\n")
                    }
                }
            }
            if (!arms().any { it is WhenArm.Else }) {
                append("}\n")
                append("panic(\"when: no matching arm\")\n")
            } else {
                append("}\n")
            }
            append("}()")
        }
    } else {
        buildString {
            append("func() $ty {\n")
            var first = true
            for (arm in arms()) {
                when (arm) {
                    is WhenArm.Guard -> {
                        if (first) {
                            append("if ${arm.cond.toGoString(ctx)} {\n")
                            first = false
                        } else {
                            append("} else if ${arm.cond.toGoString(ctx)} {\n")
                        }
                        append("return ${arm.expr.toGoString(ctx)}\n")
                    }
                    is WhenArm.Else -> {
                        append("} else {\n")
                        append("return ${arm.expr.toGoString(ctx)}\n")
                    }
                    is WhenArm.Subject ->
                        throw RuntimeException("Subject when-arm without subject at ${programLocation()}")
                }
            }
            if (!arms().any { it is WhenArm.Else }) {
                append("}\n")
                append("panic(\"when: no matching arm\")\n")
            } else {
                append("}\n")
            }
            append("}()")
        }
    }
}

private fun whenLiteralToGo(lit: WhenLiteral): String = when (lit) {
    is WhenLiteral.IntLit -> lit.value
    is WhenLiteral.RealLit -> lit.value
    is WhenLiteral.BoolLit -> lit.value
    is WhenLiteral.StringLit -> "\"${lit.value.escapeGoStringLiteral()}\""
}

private fun ObjClassLiteralExprNode.objLiteralToGo(ctx: GoEmitCtx): String {
    val fields = structType.fields.joinToString(", ") { field ->
        val expr = fieldAssignments.getValue(field.name).toGoString(ctx)
        "${field.name.toGoExportedIdent()}: $expr"
    }
    val typeName = when (structType.name) {
        "HttpServerRequest" -> "julay.HttpServerRequest"
        "HttpServerResponse" -> "julay.HttpServerResponse"
        "HttpClientRequest" -> "julay.HttpClientRequest"
        "HttpClientResponse" -> "julay.HttpClientResponse"
        else -> structType.name
    }
    return "$typeName{$fields}"
}

private fun ListLiteralExprNode.listLiteralToGo(ctx: GoEmitCtx): String {
    val elems = elements.joinToString(", ") { it.toGoString(ctx) }
    return if (elems.isEmpty()) {
        val elemTy = (resolvedListTypeOrNull()?.elementType ?: getType().let { (it as ListType).elementType })
            .toGoTypeString()
        "julay.EmptyList[$elemTy]()"
    } else {
        "julay.ListOf($elems)"
    }
}

private fun SetLiteralExprNode.setLiteralToGo(ctx: GoEmitCtx): String {
    val elems = elements.joinToString(", ") { it.toGoString(ctx) }
    return if (elems.isEmpty()) {
        val elemTy = (resolvedSetTypeOrNull()?.elementType ?: (getType() as SetType).elementType)
            .toGoTypeString()
        "julay.EmptySet[$elemTy]()"
    } else {
        "julay.SetOf($elems)"
    }
}

private fun MapLiteralExprNode.mapLiteralToGo(ctx: GoEmitCtx): String {
    val mapType = resolvedMapTypeOrNull() ?: getType() as MapType
    val kt = mapType.keyType.toGoTypeString()
    val vt = mapType.valueType.toGoTypeString()
    if (entries.isEmpty()) {
        return "julay.EmptyMap[$kt, $vt]()"
    }
    val entryStrs = entries.joinToString(", ") { (k, v) ->
        "julay.MapEntry[$kt, $vt]{Key: ${k.toGoString(ctx)}, Value: ${v.toGoString(ctx)}}"
    }
    return "julay.MapFromEntries($entryStrs)"
}

private fun IndexExprNode.indexToGo(ctx: GoEmitCtx): String {
    val baseStr = base.toGoString(ctx)
    val indexStr = index.toGoString(ctx)
    return when (base.getType()) {
        is MapType, is ListType -> "($baseStr).Get($indexStr)"
        else -> "($baseStr).Get($indexStr)"
    }
}

private fun ThisAccessExprNode.thisAccessToGo(ctx: GoEmitCtx): String {
    val root = "${ctx.procRecv}.${stateVarName()}"
    val rest = nestedFieldPath()
    if (rest.isEmpty()) return root
    return rest.fold(root) { acc, seg -> "($acc).${seg.toGoExportedIdent()}" }
}

private fun FieldAccessExprNode.fieldAccessToGo(ctx: GoEmitCtx): String {
    val base = when {
        baseSymbol in ctx.letBindings -> ctx.letBindings.getValue(baseSymbol)
        baseSymbol in ctx.argBindings -> ctx.argBindings.getValue(baseSymbol)
        baseSymbol in ctx.stateVars -> "${ctx.procRecv}.$baseSymbol"
        else -> baseSymbol.toGoSafeIdent()
    }
    if (fieldPath.size == 1 && fieldPath[0] in setOf("length", "keys")) {
        return when (fieldPath[0]) {
            "length" -> "($base).Len()"
            "keys" -> "($base).Keys()"
            else -> error("unreachable")
        }
    }
    return fieldPath.fold(base) { acc, seg -> "($acc).${seg.toGoExportedIdent()}" }
}

private fun MemberAccessExprNode.memberAccessToGo(ctx: GoEmitCtx): String {
    val base = baseExpr.toGoString(ctx)
    return when (fieldName) {
        "length" -> "($base).Len()"
        "keys" -> "($base).Keys()"
        else -> "($base).${fieldName.toGoExportedIdent()}"
    }
}

private fun FieldAccessOnExprNode.fieldAccessOnExprToGo(ctx: GoEmitCtx): String {
    val base = baseExpr.toGoString(ctx)
    return fieldPath.fold(base) { acc, seg ->
        when (seg) {
            "length" -> "($acc).Len()"
            "keys" -> "($acc).Keys()"
            else -> "($acc).${seg.toGoExportedIdent()}"
        }
    }
}

private fun FunCallExprNode.funCallToGo(ctx: GoEmitCtx): String {
    resolvedProcFunOrNull()?.let { pf ->
        val argStrs = callArgs().map { it.toGoString(ctx) }
        val argsJoined = argStrs.joinToString(", ")
        return "run_${pf.procFunName()}($argsJoined)"
    }
    resolvedBuiltinOrNull()?.let { builtin ->
        val argStrs = callArgs().map { it.toGoString(ctx) }
        return goBuiltinCall(builtin.name, argStrs, callArgs(), ctx)
    }
    // User fun: inline via specialized body if present, else reconstruct from resolved fun
    specializedBodyOrNull()?.let { return it.toGoString(ctx) }
    resolvedFunOrNull()?.let { funNode ->
        val params = funNode.funArgs().actionArgs()
        val inlined = params.zip(callArgs()).fold(funNode.funBody()) { acc, (param, arg) ->
            substituteExpr(acc, param.name, arg)
        }
        return inlined.toGoString(ctx)
    }
    throw RuntimeException("Function call \"${callName()}\" not resolved at ${programLocation()}")
}

private fun MethodCallExprNode.methodCallToGo(ctx: GoEmitCtx): String {
    resolvedProcFunOrNull()?.let { pf ->
        val argStrs = args.map { it.toGoString(ctx) }
        return "run_${pf.procFunName()}(${argStrs.joinToString(", ")})"
    }
    val base = baseExpr.toGoString(ctx)
    when (methodName) {
        "toSet" -> return "($base).ToSet()"
        "toList" -> return "($base).ToList()"
    }
    val body = hofBodyOrNull()
        ?: throw RuntimeException("HOF body not resolved for \"$methodName\" at ${programLocation()}")
    val paramNames = hofParamNamesOrNull()
        ?: throw RuntimeException("HOF params not resolved for \"$methodName\" at ${programLocation()}")
    val paramTypes = hofParamTypesOrNull()
        ?: throw RuntimeException("HOF param types not resolved for \"$methodName\" at ${programLocation()}")
    return when (methodName) {
        "filter" -> {
            val p = paramNames.single()
            val pTy = paramTypes.single().toGoTypeString()
            val bodyStr = body.toGoString(ctx.withLet(p, p))
            "($base).Filter(func($p $pTy) bool { return $bodyStr })"
        }
        "map" -> {
            val p = paramNames.single()
            val pTy = paramTypes.single().toGoTypeString()
            val retTy = getType().let {
                when (it) {
                    is ListType -> it.elementType.toGoTypeString()
                    is SetType -> it.elementType.toGoTypeString()
                    else -> "any"
                }
            }
            val bodyStr = body.toGoString(ctx.withLet(p, p))
            when (baseExpr.getType()) {
                is ListType -> "julay.MapList($base, func($p $pTy) $retTy { return $bodyStr })"
                is SetType -> "julay.MapSet($base, func($p $pTy) $retTy { return $bodyStr })"
                else -> throw RuntimeException("map expected List or Set at ${programLocation()}")
            }
        }
        "fold" -> {
            val acc = paramNames[0]
            val elem = paramNames[1]
            val accTy = paramTypes[0].toGoTypeString()
            val elemTy = paramTypes[1].toGoTypeString()
            val init = args[0].toGoString(ctx)
            val bodyStr = body.toGoString(ctx.withLet(acc, acc).withLet(elem, elem))
            when (baseExpr.getType()) {
                is ListType ->
                    "julay.FoldList($base, $init, func($acc $accTy, $elem $elemTy) $accTy { return $bodyStr })"
                is SetType ->
                    "julay.FoldSet($base, $init, func($acc $accTy, $elem $elemTy) $accTy { return $bodyStr })"
                else -> throw RuntimeException("fold expected List or Set at ${programLocation()}")
            }
        }
        "associateWith" -> {
            val p = paramNames.single()
            val pTy = paramTypes.single().toGoTypeString()
            val retTy = (getType() as MapType).valueType.toGoTypeString()
            val bodyStr = body.toGoString(ctx.withLet(p, p))
            "julay.AssociateWith($base, func($p $pTy) $retTy { return $bodyStr })"
        }
        else -> throw RuntimeException("Unsupported method \"$methodName\" in Go codegen at ${programLocation()}")
    }
}

/**
 * Emit a builtin function/effect call. Shared by expression and statement emitters.
 */
fun goBuiltinCall(
    name: String,
    argStrs: List<String>,
    argExprs: List<ExprNode> = emptyList(),
    ctx: GoEmitCtx = GoEmitCtx(),
): String = when (name) {
    "length" -> "(${argStrs[0]}).Len()"
    "parseInt" ->
        "func() int { __v, __err := strconv.Atoi(${argStrs[0]}); if __err != nil { panic(__err) }; return __v }()"
    "split" -> "julay.ListOf(strings.Split(${argStrs[0]}, ${argStrs[1]})...)"
    "trim" -> "strings.TrimSpace(${argStrs[0]})"
    "startsWith" -> "strings.HasPrefix(${argStrs[0]}, ${argStrs[1]})"
    "println" -> "julay.Println(${argStrs[0]})"
    "exitProgram" -> "julay.ExitProgram(${argStrs[0]})"
    "exitProc" -> "return"
    "readln" -> "julay.Readln()"
    "readFile" ->
        "func() string { __b, __err := os.ReadFile(${argStrs[0]}); if __err != nil { panic(__err) }; return string(__b) }()"
    "delayMillis" -> "julay.DelayMillis(${argStrs[0]})"
    "delaySeconds" -> "julay.DelaySeconds(${argStrs[0]})"
    "doHttpRequest" -> "julay.DoHttpRequest(${argStrs[0]})"
    "splice" -> "(${argStrs[0]}).Splice(${argStrs[1]}, ${argStrs[2]})"
    "allDistinct" -> "(${argStrs[0]}).AllDistinct()"
    "portFromUrl" ->
        "func() int { __u := ${argStrs[0]}; __h := __u; if i := strings.Index(__u, \"://\"); i >= 0 { __h = __u[i+3:] }; " +
            "__p := __h[strings.LastIndex(__h, \":\")+1:]; __v, __err := strconv.Atoi(__p); if __err != nil { panic(__err) }; return __v }()"
    "setTimer" -> {
        // setTimer(name, ms) — name is timer transition name (string)
        "${ctx.procRecv}.timers.SetTimer(${argStrs[0]}, int64(${argStrs[1]}))"
    }
    "cancelTimer" -> "${ctx.procRecv}.timers.CancelTimer(${argStrs[0]})"
    "serveHttp" -> {
        // serveHttp(port, handlerName) — handler is a procfun name string
        val handler = if (argExprs.size >= 2) {
            val a = argExprs[1]
            when (a) {
                is SymbolValueExprNode -> a.symbol
                is LiteralValueExprNode -> a.literalText()
                else -> null
            }
        } else null
        val handlerFn = handler?.let { "run_$it" }
            ?: "run_${argStrs.getOrNull(1)?.trim('"') ?: "handler"}"
        "julay.ServeHttp(${argStrs[0]}, $handlerFn)"
    }
    "map" -> throw RuntimeException("map HOF requires MethodCall / named-fun path in Go codegen")
    else -> {
        // Fall back to registry kotlin name as comment + panic, or try common pattern
        FunBuiltinRegistry.lookup(name)?.let {
            "// builtin $name\n${it.kotlinCodegen(argStrs)}"
        } ?: throw RuntimeException("Unknown builtin \"$name\" for Go codegen")
    }
}

/** Statement form of a before/after call. */
fun CallStmtNode.toGoStmtString(ctx: GoEmitCtx): String {
    resolvedBuiltinOrNull()?.let { builtin ->
        val argStrs = callArgs().map { it.toGoString(ctx) }
        return goBuiltinCall(builtin.name, argStrs, callArgs(), ctx)
    }
    resolvedFunOrNull()?.let { funNode ->
        val params = funNode.funArgs().actionArgs()
        val inlined = params.zip(callArgs()).fold(funNode.funBody()) { acc, (param, arg) ->
            substituteExpr(acc, param.name, arg)
        }
        // User fun used as statement — evaluate for side effects (rare)
        return "_ = ${inlined.toGoString(ctx)}"
    }
    throw RuntimeException("Call \"${callName()}\" not resolved at ${programLocation()}")
}

/** Zero value literal for a Go type (for uninitialized state). */
fun Type.goZeroLiteral(): String = when (this) {
    is BoolType -> "false"
    is IntType -> "0"
    is RealType -> "0.0"
    is StringType -> "\"\""
    is ObjClassType -> "$name{}"
    is ListType -> "julay.EmptyList[${elementType.toGoTypeString()}]()"
    is SetType -> "julay.EmptySet[${elementType.toGoTypeString()}]()"
    is MapType -> "julay.EmptyMap[${keyType.toGoTypeString()}, ${valueType.toGoTypeString()}]()"
    is DomainType -> carrierType.goZeroLiteral()
    is ProcFunRefType -> "\"\""
    else -> "/* zero for $this */"
}

/** Stable mangled name helper (re-export for codegen). */
fun mangleGoTypeName(type: Type): String = mangleTypeForName(type)
