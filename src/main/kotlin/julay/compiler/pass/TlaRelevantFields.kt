package julay.compiler.pass

import julay.compiler.OneLocCompileWarning
import julay.compiler.ProgramLoc
import julay.compiler.ast.*
import julay.compiler.decl.TransitUpdate
import julay.program.Variable
import julay.program.type.*

/**
 * Per-obj field relevance for TLA+ emission, plus whole-record comparison sites
 * (`=` / `~=` / `in` / `~in`) used for unused-field warnings.
 *
 * A field is relevant when the TLA-relevant fragment projects it (field access,
 * struct `when` patterns) or names it in an obj literal that is an operand of
 * whole-record comparison / membership. Assignment literals do not keep unread
 * fields. Equality itself does not keep unread fields.
 */
class TlaRelevantFields internal constructor(
    val projecting: Boolean,
    val unwrapSingletons: Boolean,
    private val relevantByObj: Map<String, Set<String>>,
    private val declaredFieldsByObj: Map<String, List<String>>,
    private val comparisonLocByObj: Map<String, ProgramLoc>,
) {
    fun fieldsFor(type: ObjClassType): List<Variable> {
        if (!projecting) return type.fields
        val keep = relevantByObj[type.name] ?: return type.fields
        return type.fields.filter { it.name in keep }
    }

    fun singletonField(type: ObjClassType): Variable? {
        if (!unwrapSingletons) return null
        return fieldsFor(type).singleOrNull()
    }

    fun dropUnwrappedPath(type: Type, path: List<String>): List<String> {
        if (!unwrapSingletons || path.isEmpty()) return path
        if (type !is ObjClassType) return path
        val head = path.first()
        val fieldType = type.fields.firstOrNull { it.name == head }?.type
        val rest = if (fieldType != null) dropUnwrappedPath(fieldType, path.drop(1)) else path.drop(1)
        val single = singletonField(type)
        return if (single != null && head == single.name) rest else listOf(head) + rest
    }

    fun filterLiteralEntries(
        objName: String,
        entries: List<Pair<String, ExprNode>>,
    ): List<Pair<String, ExprNode>> {
        if (!projecting) return entries
        val keep = relevantByObj[objName] ?: return entries
        return entries.filter { it.first in keep }
    }

    fun comparisonWarnings(): List<OneLocCompileWarning> {
        if (!projecting) return emptyList()
        val out = mutableListOf<OneLocCompileWarning>()
        for ((objName, declared) in declaredFieldsByObj) {
            val loc = comparisonLocByObj[objName] ?: continue
            val keep = relevantByObj[objName] ?: emptySet()
            for (field in declared) {
                if (field !in keep) {
                    out += OneLocCompileWarning(
                        loc,
                        "TLA+ unused-fields: omitting field \"$field\" from obj $objName " +
                            "may cause the semantics of equality and set containment for $objName " +
                            "instances to diverge from the actual semantics. " +
                            "Disable with --disable-tla-opt=unused-fields",
                    )
                }
            }
        }
        return out
    }

    fun withUnwrap(on: Boolean): TlaRelevantFields = TlaRelevantFields(
        projecting = projecting,
        unwrapSingletons = on,
        relevantByObj = relevantByObj,
        declaredFieldsByObj = declaredFieldsByObj,
        comparisonLocByObj = comparisonLocByObj,
    )

    companion object {
        val IDENTITY = TlaRelevantFields(
            projecting = false,
            unwrapSingletons = false,
            relevantByObj = emptyMap(),
            declaredFieldsByObj = emptyMap(),
            comparisonLocByObj = emptyMap(),
        )

        val UNWRAP_ONLY = TlaRelevantFields(
            projecting = false,
            unwrapSingletons = true,
            relevantByObj = emptyMap(),
            declaredFieldsByObj = emptyMap(),
            comparisonLocByObj = emptyMap(),
        )
    }
}

/** Compilation-scoped projection read by TLA domain / default / literal emitters. */
internal object TlaFieldProjection {
    private val current = ThreadLocal.withInitial { TlaRelevantFields.IDENTITY }

    fun get(): TlaRelevantFields = current.get()

    fun set(fields: TlaRelevantFields) {
        current.set(fields)
    }

    fun <T> with(fields: TlaRelevantFields, block: () -> T): T {
        val prev = current.get()
        current.set(fields)
        try {
            return block()
        } finally {
            current.set(prev)
        }
    }
}

internal fun analyzeTlaRelevantFields(
    pclasses: Map<String, ProcClassNode>,
    offers: List<TlaActionOffer>,
    usedFuns: Collection<FunNode>,
    invClosure: List<InvariantNode>,
): TlaRelevantFields {
    val analysis = RelevantFieldCollector(pclasses)
    offers.forEach { analysis.walkOffer(it) }
    usedFuns.forEach { analysis.walkFun(it) }
    invClosure.forEach { analysis.walkExpr(it.invariantFormula(), emptyMap(), inComparison = false) }
    return analysis.build()
}

private class RelevantFieldCollector(
    private val pclasses: Map<String, ProcClassNode>,
) {
    private val relevant = mutableMapOf<String, MutableSet<String>>()
    private val declared = mutableMapOf<String, List<String>>()
    private val comparisonLoc = linkedMapOf<String, ProgramLoc>()

    fun build(): TlaRelevantFields = TlaRelevantFields(
        projecting = true,
        unwrapSingletons = false,
        relevantByObj = relevant.mapValues { it.value.toSet() },
        declaredFieldsByObj = declared.toMap(),
        comparisonLocByObj = comparisonLoc.toMap(),
    )

    fun walkOffer(offer: TlaActionOffer) {
        val env = mutableMapOf<String, Type>()
        val pc = pclasses[offer.leaf.name]
        pc?.localDecls()?.filterIsInstance<VarNode>()?.forEach { vn ->
            env[vn.name] = safeTypeOf(vn)
            noteType(safeTypeOf(vn))
        }
        offer.decl.action.args.forEach { arg ->
            env[arg.name] = arg.type
            noteType(arg.type)
        }
        alsoArgsOf(offer)?.children?.filterIsInstance<ArgNode>()?.forEach { arg ->
            try {
                env[arg.argName()] = arg.type
                noteType(arg.type)
            } catch (_: RuntimeException) {
            }
        }
        offer.decl.guards.forEach { walkExpr(it, env, inComparison = false) }
        offer.decl.transits.forEach { update ->
            when (update) {
                is TransitUpdate.Assign -> walkExpr(update.expr, env, inComparison = false)
                is TransitUpdate.IndexPut -> {
                    walkExpr(update.index, env, inComparison = false)
                    walkExpr(update.value, env, inComparison = false)
                }
                is TransitUpdate.Let -> {
                    walkExpr(update.init, env, inComparison = false)
                    env[update.name] = update.type
                    noteType(update.type)
                }
            }
        }
        offer.decl.returnExpr?.let { walkExpr(it, env, inComparison = false) }
    }

    fun walkFun(funNode: FunNode) {
        val env = mutableMapOf<String, Type>()
        funNode.funArgs().actionArgs().forEach { arg ->
            env[arg.name] = arg.type
            noteType(arg.type)
        }
        walkExpr(funNode.funBody(), env, inComparison = false)
    }

    fun walkExpr(expr: ExprNode, env: Map<String, Type>, inComparison: Boolean) {
        when (expr) {
            is BinaryOpExprNode -> {
                val op = expr.op()
                val cmp = op == "=" || op == "~=" || op == "in" || op == "~in"
                if (cmp) {
                    recordComparison(expr.lhsOperand(), expr.programLocation())
                    recordComparison(expr.rhsOperand(), expr.programLocation())
                }
                val nested = inComparison || cmp
                walkExpr(expr.lhsOperand(), env, nested)
                walkExpr(expr.rhsOperand(), env, nested)
            }
            is FieldAccessExprNode -> {
                markFieldAccess(expr.baseSymbol, expr.fieldPath, env)
                expr.children.filterIsInstance<ExprNode>().forEach { walkExpr(it, env, inComparison) }
            }
            is FieldAccessOnExprNode -> {
                walkExpr(expr.baseExpr, env, inComparison)
                typeOf(expr.baseExpr)?.let { markPath(it, expr.fieldPath) }
            }
            is MemberAccessExprNode -> {
                walkExpr(expr.baseExpr, env, inComparison)
                val baseType = typeOf(expr.baseExpr)
                if (baseType is ObjClassType) {
                    markPath(baseType, listOf(expr.fieldName))
                }
            }
            is ThisAccessExprNode -> {
                val rest = expr.nestedFieldPath()
                if (rest.isNotEmpty()) {
                    env[expr.stateVarName()]?.let { markPath(it, rest) }
                }
            }
            is ObjClassLiteralExprNode -> {
                if (inComparison) {
                    markLiteralFields(expr)
                }
                expr.fieldEntries.forEach { (_, e) -> walkExpr(e, env, inComparison) }
            }
            is LetExprNode -> {
                walkExpr(expr.letInitExpr(), env, inComparison)
                val letType = expr.resolvedLetTypeOrNull()
                val inner = if (letType != null) {
                    noteType(letType)
                    env + (expr.letName() to letType)
                } else {
                    env
                }
                walkExpr(expr.bodyExpr(), inner, inComparison)
            }
            is WhenExprNode -> {
                expr.subjectExpr()?.let { walkExpr(it, env, inComparison) }
                expr.arms().forEach { arm ->
                    when (arm) {
                        is WhenArm.Subject -> {
                            when (val pattern = arm.pattern) {
                                is WhenPattern.Struct -> markLiteralFields(pattern.literal)
                                is WhenPattern.Primitive -> {}
                            }
                            walkExpr(arm.expr, env, inComparison)
                        }
                        is WhenArm.Guard -> {
                            walkExpr(arm.cond, env, inComparison)
                            walkExpr(arm.expr, env, inComparison)
                        }
                        is WhenArm.Else -> walkExpr(arm.expr, env, inComparison)
                    }
                }
            }
            is MethodCallExprNode -> {
                walkExpr(expr.baseExpr, env, inComparison)
                expr.args.forEach { walkExpr(it, env, inComparison) }
                val body = expr.hofBodyOrNull()
                val names = expr.hofParamNamesOrNull()
                val types = expr.hofParamTypesOrNull()
                if (body != null && names != null && types != null) {
                    val inner = env.toMutableMap()
                    names.zip(types).forEach { (n, t) ->
                        inner[n] = t
                        noteType(t)
                    }
                    walkExpr(body, inner, inComparison)
                }
            }
            is LambdaExprNode -> walkExpr(expr.body, env, inComparison)
            is FunCallExprNode -> {
                expr.callArgs().forEach { walkExpr(it, env, inComparison) }
                val n = expr.namedFunParamNameOrNull()
                val body = expr.namedFunBodyOrNull()
                val elem = expr.namedFunElemTypeOrNull()
                if (n != null && body != null && elem != null) {
                    noteType(elem)
                    walkExpr(body, env + (n to elem), inComparison)
                }
            }
            is QuantifiedExprNode -> walkExpr(expr.quantifiedBody(), env, inComparison)
            is IfElseExprNode -> {
                walkExpr(expr.condExpr(), env, inComparison)
                walkExpr(expr.thenExpr(), env, inComparison)
                walkExpr(expr.elseExpr(), env, inComparison)
            }
            is ParenExprNode -> walkExpr(expr.innerExpr(), env, inComparison)
            is UnaryOpExprNode -> walkExpr(expr.operand(), env, inComparison)
            is IndexExprNode -> {
                walkExpr(expr.base, env, inComparison)
                walkExpr(expr.index, env, inComparison)
            }
            else -> expr.children.filterIsInstance<ExprNode>().forEach {
                walkExpr(it, env, inComparison)
            }
        }
    }

    private fun markLiteralFields(expr: ObjClassLiteralExprNode) {
        val objName = try {
            expr.structType.name
        } catch (_: RuntimeException) {
            expr.className
        }
        try {
            noteType(expr.structType)
        } catch (_: RuntimeException) {
        }
        val keep = relevant.getOrPut(objName) { mutableSetOf() }
        expr.fieldEntries.forEach { (name, value) ->
            keep += name
            if (value is ObjClassLiteralExprNode) {
                markLiteralFields(value)
            }
        }
    }

    private fun markFieldAccess(baseSymbol: String, fieldPath: List<String>, env: Map<String, Type>) {
        val envType = env[baseSymbol]
        if (envType != null) {
            markPath(envType, fieldPath)
            return
        }
        val pc = pclasses[baseSymbol] ?: return
        if (fieldPath.isEmpty()) return
        val vn = pc.localDecls().filterIsInstance<VarNode>().firstOrNull { it.name == fieldPath.first() }
            ?: return
        markPath(safeTypeOf(vn), fieldPath.drop(1))
    }

    private fun markPath(type: Type, path: List<String>) {
        if (path.isEmpty()) return
        val head = path.first()
        if (head == "length" || head == "keys") return
        when (type) {
            is ObjClassType -> {
                noteType(type)
                relevant.getOrPut(type.name) { mutableSetOf() }.add(head)
                val fieldType = type.fields.firstOrNull { it.name == head }?.type ?: return
                markPath(fieldType, path.drop(1))
            }
            is ListType -> markPath(type.elementType, path)
            is SetType -> markPath(type.elementType, path)
            is MapType -> {
                markPath(type.valueType, path)
                markPath(type.keyType, path)
            }
            else -> {}
        }
    }

    private fun recordComparison(operand: ExprNode, loc: ProgramLoc) {
        val t = typeOf(operand) ?: return
        objsIn(t).forEach { obj ->
            noteType(obj)
            comparisonLoc.putIfAbsent(obj.name, loc)
        }
    }

    private fun noteType(type: Type) {
        when (type) {
            is ObjClassType -> {
                declared.putIfAbsent(type.name, type.fields.map { it.name })
                relevant.putIfAbsent(type.name, mutableSetOf())
            }
            is ListType -> noteType(type.elementType)
            is SetType -> noteType(type.elementType)
            is MapType -> {
                noteType(type.keyType)
                noteType(type.valueType)
            }
            else -> {}
        }
    }

    private fun alsoArgsOf(offer: TlaActionOffer): ArgsNode? {
        val pc = pclasses[offer.leaf.name] ?: return null
        return if (offer.isConstructor) {
            pc.localDecls().filterIsInstance<ConstructorNode>()
                .firstOrNull { it.constructorName() == offer.decl.action.name }
                ?.alsoArgs()
        } else {
            pc.localDecls().filterIsInstance<TransitionNode>()
                .firstOrNull { it.transitionName() == offer.decl.action.name }
                ?.alsoArgs()
        }
    }
}

private fun objsIn(type: Type): List<ObjClassType> = when (type) {
    is ObjClassType -> listOf(type)
    is ListType -> objsIn(type.elementType)
    is SetType -> objsIn(type.elementType)
    is MapType -> objsIn(type.keyType) + objsIn(type.valueType)
    else -> emptyList()
}

private fun typeOf(expr: ExprNode): Type? = try {
    expr.getType()
} catch (_: RuntimeException) {
    null
}

private fun safeTypeOf(vn: VarNode): Type = try {
    vn.type
} catch (_: RuntimeException) {
    intType
}
