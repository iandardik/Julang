package julay.compiler.pass

import julay.compiler.ast.*
import julay.compiler.decl.TransitUpdate
import julay.program.type.IntType
import julay.program.type.MapType
import julay.program.type.ObjClassType
import julay.program.type.SetType
import julay.program.type.StringType
import julay.program.type.Type

/**
 * Per-site finite String/Int domains inferred from closed literal sets.
 * Does not replace the module-global `String` / `Int` CONSTANTs.
 */
internal class TlaLiteralDomains(
    private val enabled: Boolean,
    private val varDomain: Map<Pair<String, String>, Set<String>>,
    private val objFieldDomain: Map<Pair<String, String>, Set<String>>,
) {
    fun varSet(leafClass: String, varName: String): Set<String>? =
        if (enabled) varDomain[leafClass to varName] else null

    fun objFieldSet(objName: String, field: String): Set<String>? =
        if (enabled) objFieldDomain[objName to field] else null

    fun render(lits: Set<String>): String =
        "{${lits.sorted().joinToString(", ")}}"

    companion object {
        val NONE = TlaLiteralDomains(false, emptyMap(), emptyMap())
    }
}

internal object TlaLiteralDomainProjection {
    private val current = ThreadLocal.withInitial { TlaLiteralDomains.NONE }

    fun get(): TlaLiteralDomains = current.get()

    fun set(d: TlaLiteralDomains) {
        current.set(d)
    }
}

internal fun analyzeTlaLiteralDomains(
    leaves: List<SpecLeaf>,
    pclasses: Map<String, ProcClassNode>,
    offers: List<TlaActionOffer>,
    usedFuns: Collection<FunNode>,
    invClosure: List<InvariantNode>,
): TlaLiteralDomains {
    val vars = mutableMapOf<Pair<String, String>, SiteLits>()
    val fields = mutableMapOf<Pair<String, String>, SiteLits>()

    fun varSite(leaf: String, name: String) = vars.getOrPut(leaf to name) { SiteLits() }
    fun fieldSite(obj: String, name: String) = fields.getOrPut(obj to name) { SiteLits() }

    leaves.forEach { leaf ->
        val pc = pclasses[leaf.name] ?: return@forEach
        pc.localDecls().filterIsInstance<VarNode>().forEach { vn ->
            val t = try {
                vn.type
            } catch (_: RuntimeException) {
                return@forEach
            }
            if (t is StringType || t is IntType) {
                if (TlaVarProjection.get().isRelevant(leaf.name, vn.name)) {
                    varSite(leaf.name, vn.name)
                }
            }
        }
    }

    offers.forEach { offer ->
        val argNames = offer.decl.action.args.map { it.name }.toSet()
        offer.decl.guards.forEach { walkLits(it, offer.leaf.name, ::varSite, ::fieldSite, inWrite = false) }
        offer.decl.errors.forEach { arm ->
            walkLits(arm.condExpr(), offer.leaf.name, ::varSite, ::fieldSite, inWrite = false)
        }
        offer.decl.transits.forEach { update ->
            when (update) {
                is TransitUpdate.Assign -> {
                    if (TlaVarProjection.get().isRelevant(offer.leaf.name, update.transitRootVar())) {
                        noteWrite(update.transitRootVar(), update.expr, offer.leaf.name, ::varSite, ::fieldSite, argNames)
                        walkLits(update.expr, offer.leaf.name, ::varSite, ::fieldSite, inWrite = true)
                    }
                }
                is TransitUpdate.IndexPut -> {
                    if (TlaVarProjection.get().isRelevant(offer.leaf.name, update.transitRootVar())) {
                        walkLits(update.index, offer.leaf.name, ::varSite, ::fieldSite, inWrite = false)
                        walkLits(update.value, offer.leaf.name, ::varSite, ::fieldSite, inWrite = true)
                    }
                }
                is TransitUpdate.Let ->
                    walkLits(update.init, offer.leaf.name, ::varSite, ::fieldSite, inWrite = false)
            }
        }
        offer.decl.returnExpr?.let { walkLits(it, offer.leaf.name, ::varSite, ::fieldSite, inWrite = false) }
    }
    usedFuns.forEach { walkLits(it.funBody(), "", ::varSite, ::fieldSite, inWrite = false) }
    invClosure.forEach { walkLits(it.invariantFormula(), "", ::varSite, ::fieldSite, inWrite = false) }
    // Placeholder obj literals (e.g. Optional none) must not close Int fields of types that
    // also appear as set elements / map keys — cluster ids come from init, not those literals.
    leaves.forEach { leaf ->
        val pc = pclasses[leaf.name] ?: return@forEach
        pc.localDecls().filterIsInstance<VarNode>().forEach { vn ->
            if (!TlaVarProjection.get().isRelevant(leaf.name, vn.name)) return@forEach
            val t = try {
                vn.type
            } catch (_: RuntimeException) {
                return@forEach
            }
            fun openIntFields(obj: ObjClassType) {
                obj.fields.forEach { f ->
                    if (f.type is IntType) fieldSite(obj.name, f.name).open = true
                }
            }
            when (t) {
                is SetType -> (t.elementType as? ObjClassType)?.let { openIntFields(it) }
                is MapType -> {
                    (t.keyType as? ObjClassType)?.let { openIntFields(it) }
                    (t.valueType as? ObjClassType)?.let { openIntFields(it) }
                }
                else -> {}
            }
        }
    }

    fun closed(map: Map<Pair<String, String>, SiteLits>) =
        map.mapNotNull { (k, v) ->
            if (!v.open && v.lits.isNotEmpty()) k to v.lits.toSet() else null
        }.toMap()

    return TlaLiteralDomains(true, closed(vars), closed(fields))
}

/** Finite domain for a remaining action arg from literal equalities / membership in its guards. */
internal fun argLiteralDomain(
    offers: List<TlaActionOffer>,
    argName: String,
    type: Type,
): String? {
    if (!TlaEmitOpts.get().literalDomains) return null
    if (type !is StringType && type !is IntType) return null
    val lits = linkedSetOf<String>()
    var constrained = false
    offers.forEach { offer ->
        offer.decl.guards.flatMap { flattenTopLevelAnd(it) }.forEach { c ->
            if (!exprReferencesSymbol(c, argName)) return@forEach
            val collected = literalsConstrainingArg(c, argName, type) ?: run {
                return null
            }
            constrained = true
            lits += collected
        }
    }
    if (!constrained || lits.isEmpty()) return null
    return "{${lits.sorted().joinToString(", ")}}"
}

private class SiteLits {
    val lits = linkedSetOf<String>()
    var open = false
}

private fun noteWrite(
    name: String,
    expr: ExprNode,
    leaf: String,
    varSite: (String, String) -> SiteLits,
    fieldSite: (String, String) -> SiteLits,
    argNames: Set<String> = emptySet(),
) {
    val site = varSite(leaf, name)
    val lit = literalToken(expr)
    when {
        lit != null -> site.lits += lit
        expr is SymbolValueExprNode && expr.symbol in argNames -> site.open = true
        isSelfOrIfSelf(expr, name) -> {
            collectLiteralsFromIf(expr, name, site)
        }
        else -> site.open = true
    }
    noteObjLiteralWrites(expr, fieldSite)
}

private fun isSelfOrIfSelf(expr: ExprNode, name: String): Boolean {
    val e = if (expr is ParenExprNode) expr.innerExpr() else expr
    return when (e) {
        is SymbolValueExprNode -> e.symbol == name
        is IfElseExprNode ->
            isSelfOrIfSelf(e.thenExpr(), name) && isSelfOrIfSelf(e.elseExpr(), name) ||
                (literalToken(e.thenExpr()) != null && isSelfOrIfSelf(e.elseExpr(), name)) ||
                (isSelfOrIfSelf(e.thenExpr(), name) && literalToken(e.elseExpr()) != null) ||
                (literalToken(e.thenExpr()) != null && literalToken(e.elseExpr()) != null)
        else -> false
    }
}

private fun collectLiteralsFromIf(expr: ExprNode, name: String, site: SiteLits) {
    val e = if (expr is ParenExprNode) expr.innerExpr() else expr
    when (e) {
        is IfElseExprNode -> {
            literalToken(e.thenExpr())?.let { site.lits += it } ?: collectLiteralsFromIf(e.thenExpr(), name, site)
            literalToken(e.elseExpr())?.let { site.lits += it } ?: collectLiteralsFromIf(e.elseExpr(), name, site)
        }
        else -> {}
    }
}

private fun noteObjLiteralWrites(
    expr: ExprNode,
    fieldSite: (String, String) -> SiteLits,
) {
    when (expr) {
        is ObjClassLiteralExprNode -> {
            val obj = try {
                expr.structType.name
            } catch (_: RuntimeException) {
                expr.className
            }
            expr.fieldEntries.forEach { (fname, value) ->
                val site = fieldSite(obj, fname)
                val lit = literalToken(value)
                if (lit != null) site.lits += lit
                else if (value !is ObjClassLiteralExprNode) site.open = true
                noteObjLiteralWrites(value, fieldSite)
            }
        }
        is IfElseExprNode -> {
            noteObjLiteralWrites(expr.thenExpr(), fieldSite)
            noteObjLiteralWrites(expr.elseExpr(), fieldSite)
            noteObjLiteralWrites(expr.condExpr(), fieldSite)
        }
        else -> expr.children.filterIsInstance<ExprNode>().forEach { noteObjLiteralWrites(it, fieldSite) }
    }
}

private fun walkLits(
    expr: ExprNode,
    leaf: String,
    varSite: (String, String) -> SiteLits,
    fieldSite: (String, String) -> SiteLits,
    inWrite: Boolean,
) {
    when (expr) {
        is BinaryOpExprNode -> {
            val op = expr.op()
            if (op == "=" || op == "~=" || op == "in" || op == "~in") {
                noteCompare(expr.lhsOperand(), expr.rhsOperand(), leaf, varSite, fieldSite)
                noteCompare(expr.rhsOperand(), expr.lhsOperand(), leaf, varSite, fieldSite)
            }
            walkLits(expr.lhsOperand(), leaf, varSite, fieldSite, inWrite)
            walkLits(expr.rhsOperand(), leaf, varSite, fieldSite, inWrite)
        }
        is ObjClassLiteralExprNode -> noteObjLiteralWrites(expr, fieldSite)
        else -> expr.children.filterIsInstance<ExprNode>().forEach {
            walkLits(it, leaf, varSite, fieldSite, inWrite)
        }
    }
}

private fun noteCompare(
    siteExpr: ExprNode,
    other: ExprNode,
    leaf: String,
    varSite: (String, String) -> SiteLits,
    fieldSite: (String, String) -> SiteLits,
) {
    val tokens = literalToken(other)?.let { listOf(it) } ?: setOfLiteralList(other) ?: return
    when (val s = if (siteExpr is ParenExprNode) siteExpr.innerExpr() else siteExpr) {
        is SymbolValueExprNode -> tokens.forEach { varSite(leaf, s.symbol).lits += it }
        is FieldAccessExprNode -> {
            if (s.fieldPath.size == 1) {
                tokens.forEach { varSite(s.baseSymbol, s.fieldPath.first()).lits += it }
            }
        }
        is MemberAccessExprNode -> {
            val base = s.baseExpr
            val obj = try {
                base.getType()
            } catch (_: RuntimeException) {
                null
            }
            if (obj is julay.program.type.ObjClassType) {
                tokens.forEach { fieldSite(obj.name, s.fieldName).lits += it }
            }
        }
        else -> {}
    }
}

private fun literalsConstrainingArg(conjunct: ExprNode, arg: String, type: Type): Set<String>? {
    val pieces = flattenTopLevelOr(conjunct)
    val out = linkedSetOf<String>()
    for (p in pieces) {
        val e = if (p is ParenExprNode) p.innerExpr() else p
        if (e !is BinaryOpExprNode) return null
        when (e.op()) {
            "=" -> {
                val lit = literalEqArg(e, arg) ?: return null
                out += lit
            }
            "in" -> {
                val lhs = if (e.lhsOperand() is ParenExprNode) (e.lhsOperand() as ParenExprNode).innerExpr() else e.lhsOperand()
                if (lhs !is SymbolValueExprNode || lhs.symbol != arg) return null
                val lits = setOfLiteralList(e.rhsOperand()) ?: return null
                out += lits
            }
            else -> return null
        }
    }
    return out
}

private fun literalEqArg(e: BinaryOpExprNode, arg: String): String? {
    val l = if (e.lhsOperand() is ParenExprNode) (e.lhsOperand() as ParenExprNode).innerExpr() else e.lhsOperand()
    val r = if (e.rhsOperand() is ParenExprNode) (e.rhsOperand() as ParenExprNode).innerExpr() else e.rhsOperand()
    return when {
        l is SymbolValueExprNode && l.symbol == arg -> literalToken(r)
        r is SymbolValueExprNode && r.symbol == arg -> literalToken(l)
        else -> null
    }
}

private fun literalToken(expr: ExprNode): String? {
    val e = if (expr is ParenExprNode) expr.innerExpr() else expr
    if (e !is LiteralValueExprNode) return null
    return when (e.getType()) {
        is StringType -> "\"${e.literalText()}\""
        is IntType -> e.literalText()
        else -> null
    }
}

private fun setOfLiteralList(expr: ExprNode): List<String>? {
    val e = if (expr is ParenExprNode) expr.innerExpr() else expr
    when (e) {
        is SetLiteralExprNode -> {
            val toks = e.elements.map { literalToken(it) }
            if (toks.any { it == null }) return null
            return toks.filterNotNull()
        }
        is FunCallExprNode -> {
            if (e.callName() != "setOf") return null
            val toks = e.callArgs().map { literalToken(it) }
            if (toks.any { it == null }) return null
            return toks.filterNotNull()
        }
        else -> return null
    }
}
