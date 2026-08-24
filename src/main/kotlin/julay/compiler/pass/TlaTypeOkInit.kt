package julay.compiler.pass

import julay.compiler.ast.*
import julay.compiler.decl.TransitUpdate
import julay.program.type.IntType
import julay.program.type.ListType
import julay.program.type.SetType
import julay.program.type.SortType
import julay.program.type.Type

internal data class SingletonInitHit(
    val value: String,
    val consumed: List<ExprNode>,
)

internal data class TypeOkShapePlan(
    val lenEq: Map<Pair<String, String>, String>,
    val subsetRange: Map<Pair<String, String>, String>,
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
    sorts: Map<String, SortType>,
): SingletonInitHit? {
    val listTy = type as? ListType ?: return null
    if (!tlaElemIsInt(listTy.elementType)) return null
    val lenHit = initExprs.mapNotNull { expr ->
        lengthEqSize(expr, varName, sorts)?.let { expr to it }
    }.singleOrNull() ?: return null
    val idHit = initExprs.firstOrNull { isIdentityIndexInit(it, varName) } ?: return null
    if (lenHit.second < 0) return null
    val elems = (1..lenHit.second).joinToString(", ")
    return SingletonInitHit("<<$elems>>", listOf(lenHit.first, idHit))
}

internal fun analyzeTypeOkShapes(
    leaves: List<SpecLeaf>,
    pclasses: Map<String, ProcClassNode>,
    offers: List<TlaActionOffer>,
): TypeOkShapePlan {
    val lenEq = linkedMapOf<Pair<String, String>, String>()
    val subsetRange = linkedMapOf<Pair<String, String>, String>()
    leaves.forEach { leaf ->
        val pc = pclasses[leaf.name] ?: return@forEach
        val vars = pc.localDecls().filterIsInstance<VarNode>()
        val mapSources = linkedMapOf<String, String>()
        val mapConflicted = mutableSetOf<String>()
        val declared = vars.map { it.name }.toSet()
        offers.filter { it.leaf.tlaName == leaf.tlaName }.forEach { offer ->
            offer.decl.transits.filterIsInstance<TransitUpdate.Assign>().forEach { update ->
                val src = mapSourceVar(update.expr) ?: return@forEach
                if (src !in declared) return@forEach
                if (!TlaVarProjection.get().isRelevant(leaf.name, src)) return@forEach
                val root = update.transitRootVar()
                if (!TlaVarProjection.get().isRelevant(leaf.name, root)) return@forEach
                val prev = mapSources[root]
                if (prev != null && prev != src) {
                    mapConflicted += root
                } else {
                    mapSources[root] = src
                }
            }
        }
        mapSources.keys.removeAll(mapConflicted)
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
        val clusterSources = mapSources.values.distinct()
        if (clusterSources.size != 1) return@forEach
        val sourceName = clusterSources.single()
        val sourceVn = vars.firstOrNull { it.name == sourceName } ?: return@forEach
        val sourceTy = try {
            sourceVn.type
        } catch (_: RuntimeException) {
            return@forEach
        } as? ListType ?: return@forEach
        vars.forEach { vn ->
            if (vn.name == sourceName) return@forEach
            val ty = try {
                vn.type
            } catch (_: RuntimeException) {
                return@forEach
            }
            if (ty is SetType && tlaElemIsInt(ty.elementType) && tlaElemIsInt(sourceTy.elementType)) {
                subsetRange[leaf.tlaName to vn.name] = sourceName
            }
        }
    }
    return TypeOkShapePlan(lenEq, subsetRange)
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

private fun tlaElemIsInt(type: Type): Boolean {
    if (type is IntType) return true
    if (!TlaEmitOpts.get().unwrapSingletons) return false
    val obj = type as? julay.program.type.ObjClassType ?: return false
    val single = TlaFieldProjection.get().singletonField(obj) ?: return false
    return single.type is IntType
}

private fun mapSourceVar(expr: ExprNode): String? {
    val e = unwrapInitParen(expr)
    if (e !is MethodCallExprNode || e.methodName != "map") return null
    val base = unwrapInitParen(e.baseExpr)
    return (base as? SymbolValueExprNode)?.symbol
}

private fun lengthEqSize(
    expr: ExprNode,
    varName: String,
    sorts: Map<String, SortType>,
): Int? {
    val e = unwrapInitParen(expr)
    if (e !is BinaryOpExprNode || e.op() != "=") return null
    val l = unwrapInitParen(e.lhsOperand())
    val r = unwrapInitParen(e.rhsOperand())
    fun size(lenExpr: ExprNode, other: ExprNode): Int? {
        if (!isLengthOfName(lenExpr, varName)) return null
        literalNonNegInt(other)?.let { return it }
        return sortCardinality(other, sorts)
    }
    return size(l, r) ?: size(r, l)
}

private fun sortCardinality(expr: ExprNode, sorts: Map<String, SortType>): Int? {
    val name = lengthBaseName(expr) ?: return null
    return sorts[name]?.cfgElements?.size
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
