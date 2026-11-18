package julay.compiler.pass

import julay.compiler.CompileError
import julay.compiler.OneLocCompileError
import julay.compiler.ast.*
import julay.compiler.decl.ActionDecl
import julay.compiler.decl.ObjClassRegistry
import julay.compiler.decl.TypeResolveResult
import julay.program.type.Type

/**
 * If [assume] is a bare name that denotes an invariant (and not a system decl),
 * return that invariant name. Ambiguous names yield an error.
 */
internal fun formulaAssumeInvariantName(
    assume: ASTNode?,
    invariantNames: Set<String>,
    systemDeclNames: Set<String>,
): Pair<String?, CompileError?> {
    if (assume == null) return null to null
    if (assume is AssumeInvariantExprNode) {
        val name = assume.assumeInvariantName()
        return if (name in invariantNames) {
            name to null
        } else {
            null to OneLocCompileError(
                assume.programLocation(),
                "assumption \"$name\" is not a declared invariant",
            )
        }
    }
    if (assume !is ValueProcExprNode || assume.isQualified()) return null to null
    val name = assume.valueProcName()
    val isInv = name in invariantNames
    val isSys = name in systemDeclNames
    return when {
        isInv && isSys ->
            null to OneLocCompileError(
                assume.programLocation(),
                "assumption \"$name\" is ambiguous (both an invariant and a system name); rename one",
            )
        isInv -> name to null
        else -> null to null
    }
}

internal fun isFormulaAssume(
    assume: ASTNode?,
    invariantNames: Set<String>,
    systemDeclNames: Set<String>,
): Boolean = formulaAssumeInvariantName(assume, invariantNames, systemDeclNames).first != null

/** Collect fluent names referenced from an expression (calls and bare symbols). */
internal fun collectFluentRefs(expr: ExprNode, fluentNames: Set<String>): Set<String> {
    val out = linkedSetOf<String>()
    fun walk(n: ASTNode) {
        when (n) {
            is FunCallExprNode -> {
                val name = n.callName()
                if (name in fluentNames) out += name
                n.callArgs().forEach { walk(it) }
            }
            is SymbolValueExprNode -> {
                if (n.symbol in fluentNames) out += n.symbol
            }
            else -> n.children.forEach { walk(it) }
        }
    }
    walk(expr)
    return out
}

internal fun exprMentionsFluent(expr: ExprNode, fluentNames: Set<String>): Boolean =
    collectFluentRefs(expr, fluentNames).isNotEmpty()

/**
 * Fluents mentioned by an invariant closure rooted at [root], including nested
 * invariant refs and fluent `on` targets of those fluents.
 */
internal fun fluentsUsedByInvariant(
    root: InvariantNode,
    invariants: Map<String, InvariantNode>,
    fluents: Map<String, FluentNode>,
): Set<String> {
    val fluentNames = fluents.keys
    val out = linkedSetOf<String>()
    val invSeen = mutableSetOf<String>()
    val flSeen = mutableSetOf<String>()
    fun walkFluent(name: String) {
        if (!flSeen.add(name)) return
        out += name
        val fl = fluents[name] ?: return
        fl.fluentOnClauses().forEach { clause ->
            val t = clause.target
            if (t is FluentTarget.FluentRef && t.name in fluentNames) walkFluent(t.name)
        }
    }
    fun walkInv(name: String) {
        if (!invSeen.add(name)) return
        val inv = invariants[name] ?: return
        collectFluentRefs(inv.invariantFormula(), fluentNames).forEach { walkFluent(it) }
        collectInvariantRefs(inv.invariantFormula(), invariants.keys).forEach { walkInv(it) }
    }
    walkInv(root.name())
    return out
}

/** Resolve fluent param types; return errors. */
internal fun typePassFluentDecl(
    fluent: FluentNode,
    registry: ObjClassRegistry,
    allFluentNames: Set<String>,
): List<CompileError> {
    val errors = mutableListOf<CompileError>()
    val paramNames = mutableSetOf<String>()
    fluent.fluentParams().forEach { p ->
        if (!paramNames.add(p.name)) {
            errors += OneLocCompileError(
                fluent.programLocation(),
                "duplicate fluent parameter \"${p.name}\" in fluent \"${fluent.name()}\"",
            )
        }
        when (val r = registry.resolveTypeExpr(p.typeExpr, emptyMap())) {
            is TypeResolveResult.Error ->
                errors += OneLocCompileError(
                    fluent.programLocation(),
                    "unknown type \"${p.typeExpr}\" for fluent parameter \"${p.name}\"",
                )
            is TypeResolveResult.Found -> {}
        }
    }
    if (exprMentionsFluent(fluent.fluentInitially(), allFluentNames)) {
        errors += OneLocCompileError(
            fluent.fluentInitially().programLocation(),
            "fluent \"${fluent.name()}\" initially formula may not reference fluents",
        )
    }
    fluent.fluentOnClauses().forEach { clause ->
        when (val t = clause.target) {
            is FluentTarget.True, is FluentTarget.False -> {}
            is FluentTarget.FluentRef -> {
                if (t.name !in allFluentNames) {
                    errors += OneLocCompileError(
                        clause.loc,
                        "unknown fluent \"${t.name}\" in on-clause target",
                    )
                }
            }
        }
    }
    return errors
}

/**
 * Check that each `on` clause matches the AG system's alphabet and create-index shape.
 */
internal fun typePassFluentOnAgainstSystem(
    fluent: FluentNode,
    systemLeaves: List<SpecLeaf>,
    pclasses: Map<String, ProcClassNode>,
): List<CompileError> {
    val errors = mutableListOf<CompileError>()
    // action name -> offers (leaf + decl)
    data class Offer(val leaf: SpecLeaf, val decl: ActionDecl)
    val byName = linkedMapOf<String, MutableList<Offer>>()
    systemLeaves.forEach { leaf ->
        val pc = pclasses[leaf.name] ?: return@forEach
        pc.localDecls().flatMap { it.transitions() }.forEach { decl ->
            byName.getOrPut(decl.action.name) { mutableListOf() } += Offer(leaf, decl)
        }
        // service/client constructors rarely update fluents; still allow by name
        pc.localDecls().flatMap { it.constructors() }.forEach { decl ->
            byName.getOrPut(decl.action.name) { mutableListOf() } += Offer(leaf, decl)
        }
    }
    fluent.fluentOnClauses().forEach { clause ->
        val offers = byName[clause.actionName]
        if (offers.isNullOrEmpty()) {
            errors += OneLocCompileError(
                clause.loc,
                "fluent \"${fluent.name()}\" on-clause action \"${clause.actionName}\" " +
                    "is not in the system alphabet",
            )
            return@forEach
        }
        val indexed = offers.any { it.leaf.isParameterized }
        val unindexed = offers.any { !it.leaf.isParameterized }
        when {
            clause.indexBinder != null && !indexed ->
                errors += OneLocCompileError(
                    clause.loc,
                    "fluent \"${fluent.name()}\" uses on ${clause.actionName}[${clause.indexBinder}](...), " +
                        "but \"${clause.actionName}\" is not create-indexed in this system",
                )
            clause.indexBinder == null && indexed && !unindexed ->
                errors += OneLocCompileError(
                    clause.loc,
                    "fluent \"${fluent.name()}\" uses on ${clause.actionName}(...), " +
                        "but \"${clause.actionName}\" is create-indexed in this system " +
                        "(write on ${clause.actionName}[n](...))",
                )
            clause.indexBinder != null && indexed -> {
                val leaf = offers.first { it.leaf.isParameterized }.leaf
                val expected = leaf.paramType
                // binder is only a name; type must match create-index domain when present on fluent params
                val flParam = fluent.fluentParams().find { it.name == clause.indexBinder }
                if (flParam != null && expected != null) {
                    val gotName = typeDomainConstant(flParam.typeExpr)
                    val expName = typeDomainConstant(expected)
                    if (gotName != null && expName != null && gotName != expName) {
                        errors += OneLocCompileError(
                            clause.loc,
                            "fluent index binder \"${clause.indexBinder}\" has type $gotName " +
                                "but action is indexed by $expName",
                        )
                    }
                }
            }
        }
        // Arg binders must be action arg names or _
        val sample = offers.first().decl.action.args.map { it.name }.toSet()
        clause.argBinders.forEach { b ->
            if (b != "_" && b !in sample) {
                errors += OneLocCompileError(
                    clause.loc,
                    "fluent on-clause binder \"$b\" is not an argument of \"${clause.actionName}\" " +
                        "(expected one of ${sample.sorted().joinToString(", ")})",
                )
            }
        }
        if (clause.argBinders.filter { it != "_" }.size > sample.size) {
            errors += OneLocCompileError(
                clause.loc,
                "fluent on-clause for \"${clause.actionName}\" has too many binders",
            )
        }
    }
    return errors
}

internal fun resolveFluentParamTypes(
    fluent: FluentNode,
    registry: ObjClassRegistry,
): List<Type> =
    fluent.fluentParams().map { p ->
        when (val r = registry.resolveTypeExpr(p.typeExpr, emptyMap())) {
            is TypeResolveResult.Found -> r.type
            is TypeResolveResult.Error -> error("fluent param type unresolved: ${p.typeExpr}")
        }
    }

internal fun fluentTlaDomain(type: Type): String = typeToTlaDomain(type)
