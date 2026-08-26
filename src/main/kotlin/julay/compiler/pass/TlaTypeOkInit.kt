package julay.compiler.pass

import julay.compiler.ast.*
import julay.compiler.decl.TransitUpdate
import julay.program.type.IntType
import julay.program.type.ListType
import julay.program.type.MapType
import julay.program.type.SetType
import julay.program.type.DomainType
import julay.program.type.Type

internal data class SingletonInitHit(
    val value: String,
    val consumed: List<ExprNode>,
)

internal data class TypeOkShapePlan(
    val lenEq: Map<Pair<String, String>, String>,
    val domainEq: Map<Pair<String, String>, String>,
    val subsetRange: Map<Pair<String, String>, String>,
    val subsetSrc: Map<Pair<String, String>, String>,
) {
    val usesRange: Boolean get() = subsetRange.isNotEmpty()
}

/**
 * `cluster = <<1, 2, …, n>>` when `init:` pins length and identity `xs[i] = i`.
 * Does not pick a canonical `self`.
 */
internal fun matchSingletonConstGlobalInit(
    varName: String,
    type: Type,
    initExprs: List<ExprNode>,
    domains: Map<String, DomainType>,
): SingletonInitHit? {
    val lenHit = initExprs.mapNotNull { expr ->
        lengthEqSize(expr, varName, domains)?.let { expr to it }
    }.singleOrNull() ?: return null
    if (lenHit.second < 0) return null
    val elems = (1..lenHit.second).joinToString(", ")
    return when (type) {
        is ListType -> {
            if (!tlaElemIsInt(type.elementType)) return null
            val idHit = initExprs.firstOrNull { isIdentityIndexInit(it, varName) } ?: return null
            SingletonInitHit("<<$elems>>", listOf(lenHit.first, idHit))
        }
        is SetType -> {
            if (!tlaElemIsInt(type.elementType)) return null
            val coverHit = initExprs.firstOrNull { isCoveringMembershipInit(it, varName) } ?: return null
            SingletonInitHit("{$elems}", listOf(lenHit.first, coverHit))
        }
        else -> null
    }
}

internal fun analyzeTypeOkShapes(
    leaves: List<SpecLeaf>,
    pclasses: Map<String, ProcClassNode>,
    offers: List<TlaActionOffer>,
): TypeOkShapePlan {
    val lenEq = linkedMapOf<Pair<String, String>, String>()
    val domainEq = linkedMapOf<Pair<String, String>, String>()
    val subsetRange = linkedMapOf<Pair<String, String>, String>()
    val subsetSrc = linkedMapOf<Pair<String, String>, String>()
    leaves.forEach { leaf ->
        val pc = pclasses[leaf.name] ?: return@forEach
        val vars = pc.localDecls().filterIsInstance<VarNode>()
        val mapSources = linkedMapOf<String, String>()
        val assocSources = linkedMapOf<String, String>()
        val mapConflicted = mutableSetOf<String>()
        val assocConflicted = mutableSetOf<String>()
        val declared = vars.map { it.name }.toSet()
        offers.filter { it.leaf.tlaName == leaf.tlaName }.forEach { offer ->
            offer.decl.transits.filterIsInstance<TransitUpdate.Assign>().forEach { update ->
                val hof = hofSourceVar(update.expr) ?: return@forEach
                if (hof.src !in declared) return@forEach
                if (!TlaVarProjection.get().isRelevant(leaf.name, hof.src)) return@forEach
                val root = update.transitRootVar()
                if (!TlaVarProjection.get().isRelevant(leaf.name, root)) return@forEach
                val dest = if (hof.method == "associateWith") assocSources else mapSources
                val conflicted = if (hof.method == "associateWith") assocConflicted else mapConflicted
                val prev = dest[root]
                if (prev != null && prev != hof.src) {
                    conflicted += root
                } else {
                    dest[root] = hof.src
                }
            }
        }
        mapSources.keys.removeAll(mapConflicted)
        assocSources.keys.removeAll(assocConflicted)
        mapSources.forEach { (listVar, src) ->
            val vn = vars.firstOrNull { it.name == listVar } ?: return@forEach
            val ty = try {
                vn.type
            } catch (_: RuntimeException) {
                return@forEach
            }
            if (ty is ListType) {
                lenEq[leaf.tlaName to listVar] = src
            }
        }
        assocSources.forEach { (mapVar, src) ->
            val vn = vars.firstOrNull { it.name == mapVar } ?: return@forEach
            val ty = try {
                vn.type
            } catch (_: RuntimeException) {
                return@forEach
            }
            if (ty is MapType) {
                domainEq[leaf.tlaName to mapVar] = src
            }
        }
        val clusterSources = (mapSources.values + assocSources.values).distinct()
        if (clusterSources.size != 1) return@forEach
        val sourceName = clusterSources.single()
        val sourceVn = vars.firstOrNull { it.name == sourceName } ?: return@forEach
        val sourceTy = try {
            sourceVn.type
        } catch (_: RuntimeException) {
            return@forEach
        }
        vars.forEach { vn ->
            if (vn.name == sourceName) return@forEach
            val ty = try {
                vn.type
            } catch (_: RuntimeException) {
                return@forEach
            }
            if (ty !is SetType || !tlaElemIsInt(ty.elementType)) return@forEach
            when (sourceTy) {
                is ListType -> {
                    if (tlaElemIsInt(sourceTy.elementType)) {
                        subsetRange[leaf.tlaName to vn.name] = sourceName
                    }
                }
                is SetType -> {
                    if (tlaElemIsInt(sourceTy.elementType)) {
                        subsetSrc[leaf.tlaName to vn.name] = sourceName
                    }
                }
                else -> {}
            }
        }
    }
    return TypeOkShapePlan(lenEq, domainEq, subsetRange, subsetSrc)
}

internal fun cfgSkipConjunctiveInvariants(invClosure: List<InvariantNode>): Set<String> {
    val names = invClosure.map { it.name() }.toSet()
    return invClosure.mapNotNull { node ->
        if (isNamedConjunctionOfOthers(node, names)) node.name() else null
    }.toSet()
}

internal fun typeOkShapeConjuncts(
    leaf: SpecLeaf,
    varName: String,
    tlaName: String,
    indexed: Boolean,
    binder: String?,
    domain: String?,
    shapes: TypeOkShapePlan,
    stateVarNames: Map<Pair<String, String>, String>,
): List<String> {
    val parts = mutableListOf<String>()
    shapes.lenEq[leaf.tlaName to varName]?.let { src ->
        val srcTla = stateTlaName(leaf.tlaName, src, stateVarNames)
        val srcRead = if (leaf.indexesState(src) && binder != null) "$srcTla[$binder]" else srcTla
        val lhs = if (indexed && binder != null) "Len($tlaName[$binder])" else "Len($tlaName)"
        val body = "$lhs = Len($srcRead)"
        parts += if (indexed && binder != null && domain != null) {
            "/\\ \\A $binder \\in $domain : $body"
        } else {
            "/\\ $body"
        }
    }
    shapes.domainEq[leaf.tlaName to varName]?.let { src ->
        val srcTla = stateTlaName(leaf.tlaName, src, stateVarNames)
        val srcRead = if (leaf.indexesState(src) && binder != null) "$srcTla[$binder]" else srcTla
        val lhs = if (indexed && binder != null) "DOMAIN $tlaName[$binder]" else "DOMAIN $tlaName"
        val body = "$lhs = $srcRead"
        parts += if (indexed && binder != null && domain != null) {
            "/\\ \\A $binder \\in $domain : $body"
        } else {
            "/\\ $body"
        }
    }
    shapes.subsetRange[leaf.tlaName to varName]?.let { src ->
        val srcTla = stateTlaName(leaf.tlaName, src, stateVarNames)
        val srcRead = if (leaf.indexesState(src) && binder != null) "$srcTla[$binder]" else srcTla
        val lhs = if (indexed && binder != null) "$tlaName[$binder]" else tlaName
        val body = "$lhs \\subseteq Range($srcRead)"
        parts += if (indexed && binder != null && domain != null) {
            "/\\ \\A $binder \\in $domain : $body"
        } else {
            "/\\ $body"
        }
    }
    shapes.subsetSrc[leaf.tlaName to varName]?.let { src ->
        val srcTla = stateTlaName(leaf.tlaName, src, stateVarNames)
        val srcRead = if (leaf.indexesState(src) && binder != null) "$srcTla[$binder]" else srcTla
        val lhs = if (indexed && binder != null) "$tlaName[$binder]" else tlaName
        val body = "$lhs \\subseteq $srcRead"
        parts += if (indexed && binder != null && domain != null) {
            "/\\ \\A $binder \\in $domain : $body"
        } else {
            "/\\ $body"
        }
    }
    return parts
}

private fun isNamedConjunctionOfOthers(node: InvariantNode, names: Set<String>): Boolean {
    val others = names - node.name()
    val parts = andLeavesInit(node.invariantFormula())
    if (parts.size < 2) return false
    return parts.all { p ->
        val s = unwrapInitParen(p)
        s is SymbolValueExprNode && s.symbol in others
    }
}

internal fun tlaElemIsInt(type: Type): Boolean {
    if (type is IntType) return true
    if (!TlaEmitOpts.get().unwrapSingletons) return false
    val obj = type as? julay.program.type.ObjClassType ?: return false
    val single = TlaFieldProjection.get().singletonField(obj) ?: return false
    return single.type is IntType
}

private data class HofSource(val method: String, val src: String)

private fun hofSourceVar(expr: ExprNode): HofSource? {
    val e = unwrapInitParen(expr)
    if (e !is MethodCallExprNode) return null
    if (e.methodName != "map" && e.methodName != "associateWith") return null
    val base = unwrapInitParen(e.baseExpr)
    val src = (base as? SymbolValueExprNode)?.symbol ?: return null
    return HofSource(e.methodName, src)
}

private fun lengthEqSize(
    expr: ExprNode,
    varName: String,
    domains: Map<String, DomainType>,
): Int? {
    val e = unwrapInitParen(expr)
    if (e !is BinaryOpExprNode || e.op() != "=") return null
    val l = unwrapInitParen(e.lhsOperand())
    val r = unwrapInitParen(e.rhsOperand())
    fun size(lenExpr: ExprNode, other: ExprNode): Int? {
        if (!isLengthOfName(lenExpr, varName)) return null
        literalNonNegInt(other)?.let { return it }
        return sortCardinality(other, domains)
    }
    return size(l, r) ?: size(r, l)
}

private fun sortCardinality(expr: ExprNode, domains: Map<String, DomainType>): Int? {
    val name = lengthBaseName(expr) ?: return null
    return domains[name]?.cfgElements?.size
}

private fun isLengthOfName(expr: ExprNode, name: String): Boolean =
    lengthBaseName(expr) == name

private fun lengthBaseName(expr: ExprNode): String? {
    val e = unwrapInitParen(expr)
    return when (e) {
        is MemberAccessExprNode ->
            if (e.fieldName == "length") {
                (unwrapInitParen(e.baseExpr) as? SymbolValueExprNode)?.symbol
            } else {
                null
            }
        is FieldAccessExprNode ->
            if (e.fieldPath == listOf("length")) e.baseSymbol else null
        is FunCallExprNode ->
            if (e.callName() == "length") {
                (e.callArgs().singleOrNull()?.let { unwrapInitParen(it) } as? SymbolValueExprNode)?.symbol
            } else {
                null
            }
        else -> null
    }
}

private fun literalNonNegInt(expr: ExprNode): Int? {
    val e = unwrapInitParen(expr)
    if (e !is LiteralValueExprNode) return null
    return e.literalText().toIntOrNull()?.takeIf { it >= 0 }
}

private fun isIdentityIndexInit(expr: ExprNode, varName: String): Boolean {
    val q = unwrapInitParen(expr) as? QuantifiedExprNode ?: return false
    if (!q.isUniversal()) return false
    val binder = q.binderName()
    val body = unwrapInitParen(q.quantifiedBody())
    if (body !is BinaryOpExprNode || body.op() != "=>") return false
    if (!isIndexRange(body.lhsOperand(), binder, varName)) return false
    return isIdentityIndexEq(body.rhsOperand(), binder, varName)
}

/** `forall i, (1 <= i <= |xs|) => i in xs` or `exists n: n in xs & n.id = i`. */
private fun isCoveringMembershipInit(expr: ExprNode, varName: String): Boolean {
    val q = unwrapInitParen(expr) as? QuantifiedExprNode ?: return false
    if (!q.isUniversal()) return false
    val binder = q.binderName()
    val body = unwrapInitParen(q.quantifiedBody())
    if (body !is BinaryOpExprNode || body.op() != "=>") return false
    if (!isIndexRange(body.lhsOperand(), binder, varName)) return false
    return isCoveringMembership(body.rhsOperand(), binder, varName)
}

private fun isCoveringMembership(expr: ExprNode, binder: String, varName: String): Boolean {
    if (isBinderInVar(expr, binder, varName)) return true
    val q = unwrapInitParen(expr) as? QuantifiedExprNode ?: return false
    if (q.isUniversal()) return false
    val nodeBinder = q.binderName()
    val parts = andLeavesInit(q.quantifiedBody())
    val hasMem = parts.any { isBinderInVar(it, nodeBinder, varName) }
    val hasId = parts.any { isIdOfBinderEquals(it, nodeBinder, binder) }
    return hasMem && hasId
}

private fun isBinderInVar(expr: ExprNode, binder: String, varName: String): Boolean {
    val e = unwrapInitParen(expr)
    if (e !is BinaryOpExprNode || e.op() != "in") return false
    val l = unwrapInitParen(e.lhsOperand())
    val r = unwrapInitParen(e.rhsOperand())
    val isBinder = l is SymbolValueExprNode && l.symbol == binder
    val isVar = r is SymbolValueExprNode && r.symbol == varName
    return isBinder && isVar
}

private fun isIdOfBinderEquals(expr: ExprNode, nodeBinder: String, intBinder: String): Boolean {
    val e = unwrapInitParen(expr)
    if (e !is BinaryOpExprNode || e.op() != "=") return false
    val l = unwrapInitParen(e.lhsOperand())
    val r = unwrapInitParen(e.rhsOperand())
    fun isIntBinder(n: ExprNode) = n is SymbolValueExprNode && n.symbol == intBinder
    fun isNodeId(n: ExprNode): Boolean {
        val x = unwrapInitParen(n)
        val baseOk: (ExprNode) -> Boolean = { b ->
            val bb = unwrapInitParen(b)
            bb is SymbolValueExprNode && bb.symbol == nodeBinder
        }
        return when (x) {
            is MemberAccessExprNode -> x.fieldName == "id" && baseOk(x.baseExpr)
            is FieldAccessOnExprNode -> x.fieldPath == listOf("id") && baseOk(x.baseExpr)
            is FieldAccessExprNode -> x.fieldPath == listOf("id") && x.baseSymbol == nodeBinder
            is SymbolValueExprNode -> x.symbol == nodeBinder
            else -> false
        }
    }
    return (isNodeId(l) && isIntBinder(r)) || (isNodeId(r) && isIntBinder(l))
}

private fun isIndexRange(expr: ExprNode, binder: String, varName: String): Boolean {
    val parts = andLeavesInit(expr)
    var lo = false
    var hi = false
    parts.forEach { p ->
        val e = unwrapInitParen(p)
        if (e !is BinaryOpExprNode) return@forEach
        val l = unwrapInitParen(e.lhsOperand())
        val r = unwrapInitParen(e.rhsOperand())
        fun isBinder(n: ExprNode) = n is SymbolValueExprNode && n.symbol == binder
        fun isOne(n: ExprNode) = n is LiteralValueExprNode && n.literalText() == "1"
        when (e.op()) {
            ">=" -> {
                if (isBinder(l) && isOne(r)) lo = true
                if (isLengthOfName(l, varName) && isBinder(r)) hi = true
            }
            "<=" -> {
                if (isOne(l) && isBinder(r)) lo = true
                if (isBinder(l) && isLengthOfName(r, varName)) hi = true
            }
            else -> {}
        }
    }
    return lo && hi
}

private fun isIdentityIndexEq(expr: ExprNode, binder: String, varName: String): Boolean {
    val e = unwrapInitParen(expr)
    if (e !is BinaryOpExprNode || e.op() != "=") return false
    val l = unwrapInitParen(e.lhsOperand())
    val r = unwrapInitParen(e.rhsOperand())
    fun isBinder(n: ExprNode) = n is SymbolValueExprNode && n.symbol == binder
    fun isIndexedVar(n: ExprNode): Boolean = asIndexOf(n, varName, binder)
    return (isIndexedVar(l) && isBinder(r)) || (isIndexedVar(r) && isBinder(l))
}

/** `xs[i]` or `xs[i].id` (unwrap-singletons). */
private fun asIndexOf(expr: ExprNode, varName: String, binder: String): Boolean {
    fun indexMatches(n: ExprNode): Boolean {
        val e = unwrapInitParen(n) as? IndexExprNode ?: return false
        val base = unwrapInitParen(e.base) as? SymbolValueExprNode ?: return false
        val idx = unwrapInitParen(e.index) as? SymbolValueExprNode ?: return false
        return base.symbol == varName && idx.symbol == binder
    }
    val e = unwrapInitParen(expr)
    return when {
        indexMatches(e) -> true
        e is MemberAccessExprNode && indexMatches(e.baseExpr) -> true
        e is FieldAccessOnExprNode && indexMatches(e.baseExpr) -> true
        else -> false
    }
}

private fun andLeavesInit(expr: ExprNode): List<ExprNode> {
    val e = unwrapInitParen(expr)
    return if (e is BinaryOpExprNode && e.op() == "&" && !isDesugaredIff(e)) {
        andLeavesInit(e.lhsOperand()) + andLeavesInit(e.rhsOperand())
    } else {
        listOf(e)
    }
}

private fun unwrapInitParen(expr: ExprNode): ExprNode =
    if (expr is ParenExprNode) unwrapInitParen(expr.innerExpr()) else expr
