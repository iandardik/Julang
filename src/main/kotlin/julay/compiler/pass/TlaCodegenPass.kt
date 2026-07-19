package julay.compiler.pass

import julay.compiler.CompilationUnit
import julay.compiler.TypeExpr
import julay.compiler.ast.*
import julay.compiler.decl.*
import julay.program.action.TSAction
import julay.program.type.*
import java.io.File

data class TlaCodegenResult(
    val moduleName: String,
    val tlaText: String,
    val cfgText: String,
)

fun compileSpecToTla(
    spec: SpecNode,
    ast: RootNode,
    unit: CompilationUnit,
    outputDir: File = File("."),
): TlaCodegenResult {
    val result = tlaCodegenPass(spec, ast, unit)
    File(outputDir, "${result.moduleName}.tla").writeText(result.tlaText)
    File(outputDir, "${result.moduleName}.cfg").writeText(result.cfgText)
    return result
}

fun tlaCodegenPass(
    spec: SpecNode,
    ast: RootNode,
    unit: CompilationUnit,
): TlaCodegenResult {
    val moduleName = spec.specNodeName()
    val pclassNodes = unit.modules
        .flatMap { it.root.declNodes().filterIsInstance<ProcClassNode>() }
        .associateBy { it.name() }
    val procAliases = unit.modules
        .flatMap { it.root.declNodes().filterIsInstance<ProcNode>() }
        .associateBy { it.name() }
    val programAliases = unit.modules
        .flatMap { it.root.declNodes().filterIsInstance<ProgramNode>() }
        .associateBy { it.name() }
    val specAliases = unit.modules
        .flatMap { it.root.declNodes().filterIsInstance<SpecNode>() }
        .associateBy { it.name() }

    val leaves = expandLeavesToPclasses(
        compositionLeavesOfSpec(spec),
        pclassNodes,
        procAliases,
        programAliases,
        specAliases,
    )
    val leafByName = leaves.associateBy { it.name }

    val servicedActions = leaves.flatMap { leaf ->
        val pc = pclassNodes[leaf.name] ?: return@flatMap emptyList()
        pc.localDecls().flatMap { it.transitions() }
            .filter { it.modifier == TSAction.SyncRole.Service }
            .map { it.action.name }
    }.toSet()

    val constants = linkedSetOf<String>()
    leaves.forEach { leaf ->
        if (leaf.paramName != null) {
            constants += leaf.paramName!!
            typeDomainConstant(leaf.paramType!!)?.let { name ->
                // Int/Nat/Boolean/Real are provided by EXTENDS; only declare other domains (e.g. String).
                if (name !in setOf("Int", "Nat", "Boolean", "Real")) {
                    constants += name
                }
            }
        }
    }

    val invariants = ast.declNodes().filterIsInstance<InvariantNode>().associateBy { it.name() }
    val ag = spec.specNodeValue() as? AgSpecExprNode
    val invNode = ag?.let { invariants[it.invariantRef()] }
    invNode?.let { collectTypeConstants(it.invariantFormula(), constants) }

    // Finite TLC models for built-in domains used in the module (assigned in .cfg only).
    val cfgOverrides = linkedSetOf<String>()
    if (invNode != null) {
        collectBuiltinDomainUses(invNode.invariantFormula(), cfgOverrides)
    }
    leaves.forEach { leaf ->
        leaf.paramType?.let { typeDomainConstant(it) }?.let { name ->
            if (name in setOf("Int", "Nat", "Real")) cfgOverrides += name
        }
    }
    collectActionArgDomainModels(leaves, pclassNodes, cfgOverrides)
    // String is not provided by EXTENDS; declare it CONSTANT when used as a domain.
    if ("String" in cfgOverrides) {
        constants += "String"
    }

    val intModelValues = linkedSetOf(0, 1, 2, 3, 4, 5)
    collectIntLiteralsFromLeaves(leaves, pclassNodes, intModelValues)
    if (invNode != null) {
        collectIntLiteralsFromExpr(invNode.invariantFormula(), intModelValues)
    }

    val variables = mutableListOf<String>()
    val initParts = mutableListOf<String>()
    leaves.forEach { leaf ->
        val pc = pclassNodes[leaf.name] ?: return@forEach
        val constructed = tlaVar(leaf.name, "constructed")
        if (leaf.isParameterized) {
            val i = leaf.paramName!!
            variables += constructed
            initParts += "/\\ $constructed = [x \\in $i |-> FALSE]"
            pc.localDecls().filterIsInstance<VarNode>().forEach { vn ->
                val v = tlaVar(leaf.name, vn.name)
                variables += v
                initParts += "/\\ $v = [x \\in $i |-> ${defaultTlaValue(safeType(vn))}]"
            }
        } else {
            variables += constructed
            initParts += "/\\ $constructed = FALSE"
            pc.localDecls().filterIsInstance<VarNode>().forEach { vn ->
                val v = tlaVar(leaf.name, vn.name)
                variables += v
                initParts += "/\\ $v = ${defaultTlaValue(safeType(vn))}"
            }
        }
    }

    val actions = buildTlaActions(leaves, pclassNodes, servicedActions)

    val invDef = if (invNode != null) {
        val body = exprToTla(
            invNode.invariantFormula(),
            leafCtx = emptyMap(),
            argNames = emptySet(),
            self = null,
            bareStateVars = emptySet(),
            reservedNames = constants,
        )
        "${invNode.name()} == $body"
    } else {
        null
    }

    val constLine = if (constants.isEmpty()) "" else "CONSTANT ${constants.joinToString(", ")}\n\n"
    val varsLine = if (variables.isEmpty()) {
        "VARIABLES dummy\n\n"
    } else {
        "VARIABLES ${variables.joinToString(", ")}\n\n"
    }
    val varsTuple = if (variables.isEmpty()) "<<dummy>>" else "<<${variables.joinToString(", ")}>>"
    if (variables.isEmpty()) {
        initParts += "/\\ dummy = 0"
    }

    val actionDefs = actions.joinToString("\n\n") { it.def }
    val nextBody = if (actions.isEmpty()) {
        "FALSE"
    } else {
        "\n" + actions.joinToString("\n") { "  \\/ ${it.nextDisjunct()}" }
    }

    val tla = buildString {
        appendLine("---- MODULE $moduleName ----")
        appendLine("EXTENDS Integers, Sequences, FiniteSets, TLC")
        appendLine()
        append(constLine)
        append(varsLine)
        appendLine("vars == $varsTuple")
        appendLine()
        appendLine("Init ==")
        if (initParts.isEmpty()) {
            appendLine("  /\\ TRUE")
        } else {
            initParts.forEach { appendLine("  $it") }
        }
        appendLine()
        if (actionDefs.isNotEmpty()) {
            appendLine(actionDefs)
            appendLine()
        }
        appendLine("Next ==$nextBody")
        appendLine()
        appendLine("Spec == Init /\\ [][Next]_vars")
        if (invDef != null) {
            appendLine()
            appendLine(invDef)
        }
        appendLine("====")
    }

    val cfg = buildString {
        appendLine("SPECIFICATION Spec")
        if (invNode != null) {
            appendLine("INVARIANT ${invNode.name()}")
        }
        appendLine("CHECK_DEADLOCK FALSE")
        fun modelFor(name: String): String = when (name) {
            "Int", "Nat", "Real" -> cfgIntModel(intModelValues)
            "String" -> cfgStringModel(intModelValues)
            else -> cfgConstantModel(name)
        }
        constants.forEach { c ->
            appendLine("CONSTANT $c = ${modelFor(c)}")
        }
        // Finite models for built-in TLA+ domains (Int from Integers, etc.) — cfg only, not declared CONSTANT.
        cfgOverrides.forEach { c ->
            if (c !in constants) {
                appendLine("CONSTANT $c = ${modelFor(c)}")
            }
        }
    }

    return TlaCodegenResult(moduleName, tla, cfg)
}

internal data class TlaAction(
    val name: String,
    val def: String,
    /** Used action args as (name, TLA domain), in declaration order. */
    val params: List<Pair<String, String>> = emptyList(),
) {
    fun nextDisjunct(): String {
        if (params.isEmpty()) return name
        val call = "$name(${params.joinToString(", ") { it.first }})"
        return params.asReversed().fold(call) { acc, (arg, domain) ->
            "\\E $arg \\in $domain : $acc"
        }
    }
}

internal data class TlaActionOffer(
    val leaf: SpecLeaf,
    val decl: ActionDecl,
    val role: TSAction.SyncRole,
    val isConstructor: Boolean,
)

private fun safeType(vn: VarNode): Type = try {
    vn.type
} catch (_: RuntimeException) {
    intType
}

private fun buildTlaActions(
    leaves: List<SpecLeaf>,
    pclasses: Map<String, ProcClassNode>,
    servicedActions: Set<String>,
): List<TlaAction> {
    val offers = mutableListOf<TlaActionOffer>()
    leaves.forEach { leaf ->
        val pc = pclasses[leaf.name] ?: return@forEach
        pc.localDecls().flatMap { it.constructors() }.forEach { ctor ->
            offers += TlaActionOffer(leaf, ctor, TSAction.SyncRole.Internal, isConstructor = true)
        }
        pc.localDecls().flatMap { it.transitions() }.forEach { tr ->
            val role = when {
                tr.modifier == TSAction.SyncRole.Default && tr.action.name in servicedActions ->
                    TSAction.SyncRole.Consumer
                else -> tr.modifier
            }
            offers += TlaActionOffer(leaf, tr, role, isConstructor = false)
        }
    }

    val allVars = allTlaVars(leaves, pclasses)
    val stateVarsByLeaf = leaves.associate { leaf ->
        leaf.name to (
            pclasses[leaf.name]
                ?.localDecls()
                ?.filterIsInstance<VarNode>()
                ?.map { it.name }
                ?.toSet()
                ?: emptySet()
            )
    }
    val result = mutableListOf<TlaAction>()
    val byName = offers.groupBy { it.decl.action.name }

    byName.forEach { (actionName, group) ->
        val services = group.filter { it.role == TSAction.SyncRole.Service }
        val consumers = group.filter { it.role == TSAction.SyncRole.Consumer }
        val constructors = group.filter { it.isConstructor }
        val internals = group.filter { it.role == TSAction.SyncRole.Internal && !it.isConstructor }
        val defaults = group.filter { it.role == TSAction.SyncRole.Default && !it.isConstructor }

        if (services.size == 1 && consumers.isNotEmpty()) {
            val svc = services[0]
            consumers.forEach { cons ->
                val name = "${actionName}_${svc.leaf.name}_${cons.leaf.name}"
                result += emitConjoined(name, listOf(svc, cons), allVars, stateVarsByLeaf)
            }
            return@forEach
        }

        // 1 constructor + 1 default transition → one hybrid shared action
        if (constructors.size == 1 && defaults.size == 1) {
            result += emitConjoined(
                actionName,
                listOf(defaults[0], constructors[0]),
                allVars,
                stateVarsByLeaf,
            )
            return@forEach
        }

        // Solo constructors (any name, including initially) — valid leaf entry
        constructors.forEach { offer ->
            val name = if (offer.decl.action.name == "initially") {
                "${offer.leaf.name}_initially"
            } else {
                "${offer.decl.action.name}_${offer.leaf.name}"
            }
            result += emitConjoined(name, listOf(offer), allVars, stateVarsByLeaf)
        }

        internals.forEach { offer ->
            result += emitConjoined("${actionName}_${offer.leaf.name}", listOf(offer), allVars, stateVarsByLeaf)
        }

        when {
            defaults.size >= 2 ->
                result += emitConjoined(actionName, defaults, allVars, stateVarsByLeaf)
            defaults.size == 1 ->
                result += emitConjoined(actionName, defaults, allVars, stateVarsByLeaf)
        }

        if (services.isNotEmpty() && consumers.isEmpty()) {
            services.forEach { svc ->
                result += emitConjoined("${actionName}_${svc.leaf.name}", listOf(svc), allVars, stateVarsByLeaf)
            }
        }
        if (consumers.isNotEmpty() && services.isEmpty()) {
            consumers.forEach { cons ->
                result += emitConjoined("${actionName}_${cons.leaf.name}", listOf(cons), allVars, stateVarsByLeaf)
            }
        }
    }

    return result.distinctBy { it.name }
}

private fun allTlaVars(leaves: List<SpecLeaf>, pclasses: Map<String, ProcClassNode>): List<String> =
    leaves.flatMap { leaf ->
        val pc = pclasses[leaf.name] ?: return@flatMap emptyList()
        listOf(tlaVar(leaf.name, "constructed")) +
            pc.localDecls().filterIsInstance<VarNode>().map { tlaVar(leaf.name, it.name) }
    }

private fun emitConjoined(
    name: String,
    offers: List<TlaActionOffer>,
    allVars: List<String>,
    stateVarsByLeaf: Map<String, Set<String>>,
): TlaAction {
    val parts = mutableListOf<String>()
    val changed = mutableSetOf<String>()
    val argParams = mutableListOf<Pair<String, String>>()

    // Instance binders for parameterized leaves in this action: leaf.paramName is the set CONSTANT.
    // Use a fresh self_<leaf> variable as the element.
    val selfBinders = linkedMapOf<String, String>() // leafName -> selfVar
    offers.forEach { offer ->
        if (offer.leaf.isParameterized) {
            selfBinders[offer.leaf.name] = "self_${offer.leaf.name}"
        }
    }

    fun selfOf(leaf: SpecLeaf): String? = selfBinders[leaf.name]

    // Participant-only constructed enabling (no constraints on non-offering leaves).
    offers.forEach { offer ->
        val c = tlaVar(offer.leaf.name, "constructed")
        val self = selfOf(offer.leaf)
        if (offer.isConstructor) {
            parts += if (self != null) "/\\ ~$c[$self]" else "/\\ ~$c"
        } else {
            parts += if (self != null) "/\\ $c[$self]" else "/\\ $c"
        }
    }

    offers.forEach { offer ->
        val self = selfOf(offer.leaf)
        val argNames = offer.decl.action.args.map { it.name }.toSet()
        val leafCtx = mapOf(offer.leaf.name to offer.leaf)

        // Only include args that appear in guards/transits (skip unused initially args).
        fun refsArg(name: String): Boolean {
            return offer.decl.guards.any { exprReferencesSymbol(it, name) } ||
                offer.decl.transits.any { update ->
                    when (update) {
                        is TransitUpdate.Assign -> exprReferencesSymbol(update.expr, name)
                        is TransitUpdate.MapPut ->
                            exprReferencesSymbol(update.key, name) || exprReferencesSymbol(update.value, name)
                    }
                }
        }
        offer.decl.action.args.filter { refsArg(it.name) }.forEach { arg ->
            argParams += arg.name to typeToTlaDomain(arg.type)
        }

        offer.decl.guards.forEach { g ->
            parts += "/\\ ${exprToTla(g, leafCtx, argNames, self, bareStateVars = stateVarsByLeaf[offer.leaf.name].orEmpty())}"
        }

        offer.decl.transits.forEach { update ->
            when (update) {
                is TransitUpdate.Assign -> {
                    val root = update.key.substringBefore('.')
                    val v = tlaVar(offer.leaf.name, root)
                    changed += v
                    val rhs = exprToTla(
                        update.expr, leafCtx, argNames, self,
                        bareStateVars = stateVarsByLeaf[offer.leaf.name].orEmpty(),
                    )
                    parts += if (self != null) {
                        "/\\ $v' = [$v EXCEPT ![$self] = $rhs]"
                    } else {
                        "/\\ $v' = $rhs"
                    }
                }
                is TransitUpdate.MapPut -> {
                    val v = tlaVar(offer.leaf.name, update.mapVar)
                    changed += v
                    val bare = stateVarsByLeaf[offer.leaf.name].orEmpty()
                    val k = exprToTla(update.key, leafCtx, argNames, self, bareStateVars = bare)
                    val vv = exprToTla(update.value, leafCtx, argNames, self, bareStateVars = bare)
                    parts += if (self != null) {
                        "/\\ $v' = [$v EXCEPT ![$self] = [@ EXCEPT ![$k] = $vv]]"
                    } else {
                        "/\\ $v' = [$v EXCEPT ![$k] = $vv]"
                    }
                }
            }
        }

        if (offer.isConstructor) {
            val c = tlaVar(offer.leaf.name, "constructed")
            changed += c
            parts += if (self != null) {
                "/\\ $c' = [$c EXCEPT ![$self] = TRUE]"
            } else {
                "/\\ $c' = TRUE"
            }
        }
    }

    val unchanged = allVars.filter { it !in changed }
    if (unchanged.isNotEmpty()) {
        parts += "/\\ UNCHANGED <<${unchanged.joinToString(", ")}>>"
    }

    var body = parts.joinToString("\n  ")
    selfBinders.forEach { (leafName, selfVar) ->
        val setName = offers.first { it.leaf.name == leafName }.leaf.paramName!!
        body = "\\E $selfVar \\in $setName :\n  $body"
    }

    val params = argParams.distinctBy { it.first }
    val signature = if (params.isEmpty()) {
        name
    } else {
        "$name(${params.joinToString(", ") { it.first }})"
    }
    return TlaAction(name, "$signature ==\n  $body", params)
}


internal fun tlaVar(leaf: String, varName: String): String = "${leaf}_$varName"

internal fun defaultTlaValue(type: Type): String = when (type) {
    is BoolType -> "FALSE"
    is IntType -> "0"
    is RealType -> "0"
    is StringType -> "\"\""
    is ListType -> "<<>>"
    is SetType -> "{}"
    is MapType -> "[x \\in {} |-> 0]"
    is ObjClassType -> {
        val fields = type.fields.joinToString(", ") { f ->
            "${f.name} |-> ${defaultTlaValue(f.type)}"
        }
        "[$fields]"
    }
    else -> "0"
}

internal fun typeDomainConstant(typeExpr: TypeExpr): String? = when (typeExpr) {
    is TypeExpr.Simple -> typeExpr.name
    is TypeExpr.Parametric -> null
}

internal fun cfgConstantModel(name: String): String = when (name) {
    "Int", "Nat", "Real" -> "{0, 1, 2, 3, 4, 5}"
    "String" -> "{\"\", \"0\", \"1\", \"2\", \"3\", \"4\", \"5\", \"a\", \"b\"}"
    "Boolean" -> "BOOLEAN"
    else -> "{\"a\", \"b\"}" // parameter sets and other domains
}

internal fun cfgIntModel(values: Set<Int>): String =
    // TLC .cfg parsers reject unary-minus in set literals; negatives still appear via assignments.
    "{${values.filter { it >= 0 }.sorted().joinToString(", ")}}"

internal fun cfgStringModel(intValues: Set<Int>): String {
    val strs = linkedSetOf("", "a", "b")
    intValues.forEach { strs += it.toString() }
    return "{${strs.joinToString(", ") { "\"$it\"" }}}"
}

private fun collectIntLiteralsFromLeaves(
    leaves: List<SpecLeaf>,
    pclasses: Map<String, ProcClassNode>,
    into: MutableSet<Int>,
) {
    leaves.forEach { leaf ->
        val pc = pclasses[leaf.name] ?: return@forEach
        (pc.localDecls().flatMap { it.constructors() } + pc.localDecls().flatMap { it.transitions() })
            .forEach { action ->
                action.guards.forEach { collectIntLiteralsFromExpr(it, into) }
                action.transits.forEach { update ->
                    when (update) {
                        is TransitUpdate.Assign -> collectIntLiteralsFromExpr(update.expr, into)
                        is TransitUpdate.MapPut -> {
                            collectIntLiteralsFromExpr(update.key, into)
                            collectIntLiteralsFromExpr(update.value, into)
                        }
                    }
                }
            }
    }
}

private fun collectIntLiteralsFromExpr(expr: ExprNode, into: MutableSet<Int>) {
    when (expr) {
        is LiteralValueExprNode -> {
            if (expr.inferType(emptyMap()) is IntType || expr.inferType(emptyMap()) is RealType) {
                expr.toTransitString(emptyMap(), emptySet()).toIntOrNull()?.let { into += it }
            }
        }
        is UnaryOpExprNode -> {
            if (expr.op() == "-") {
                val child = expr.operand()
                if (child is LiteralValueExprNode) {
                    child.toTransitString(emptyMap(), emptySet()).toIntOrNull()?.let { into += -it }
                }
            }
            collectIntLiteralsFromExpr(expr.operand(), into)
        }
        else -> expr.children.filterIsInstance<ExprNode>().forEach { collectIntLiteralsFromExpr(it, into) }
    }
}

internal fun typeToTlaDomain(type: Type): String = when (type) {
    is BoolType -> "BOOLEAN"
    is IntType -> "Int"
    is RealType -> "Int"
    is StringType -> "String"
    is ListType -> "Seq(${typeToTlaDomain(type.elementType)})"
    is ObjClassType -> {
        val fields = type.fields.joinToString(", ") { f ->
            "${f.name}: ${typeToTlaDomain(f.type)}"
        }
        "[$fields]"
    }
    else -> "Int"
}

/** Collect finite TLC model names (Int, String, …) needed by action argument domains. */
internal fun collectDomainModelNames(type: Type, into: MutableSet<String>) {
    when (type) {
        is IntType, is RealType -> into += "Int"
        is StringType -> into += "String"
        is ObjClassType -> type.fields.forEach { collectDomainModelNames(it.type, into) }
        is ListType -> collectDomainModelNames(type.elementType, into)
        is SetType -> collectDomainModelNames(type.elementType, into)
        is MapType -> {
            collectDomainModelNames(type.keyType, into)
            collectDomainModelNames(type.valueType, into)
        }
        else -> {}
    }
}

private fun collectActionArgDomainModels(
    leaves: List<SpecLeaf>,
    pclasses: Map<String, ProcClassNode>,
    into: MutableSet<String>,
) {
    leaves.forEach { leaf ->
        val pc = pclasses[leaf.name] ?: return@forEach
        (pc.localDecls().flatMap { it.constructors() } + pc.localDecls().flatMap { it.transitions() })
            .forEach { action ->
                action.action.args.forEach { arg ->
                    collectDomainModelNames(arg.type, into)
                }
            }
    }
}

internal fun collectTypeConstants(expr: ExprNode, into: MutableSet<String>) {
    when (expr) {
        is QuantifiedExprNode -> {
            typeDomainConstant(expr.binderTypeExpr())?.let { name ->
                // Int/Nat/Boolean/Real come from EXTENDS; String and user types need CONSTANT.
                if (name !in setOf("Int", "Nat", "Boolean", "Real")) {
                    into += name
                }
            }
            collectTypeConstants(expr.quantifiedBody(), into)
        }
        is UnaryOpExprNode -> collectTypeConstants(expr.operand(), into)
        is BinaryOpExprNode -> {
            collectTypeConstants(expr.lhsOperand(), into)
            collectTypeConstants(expr.rhsOperand(), into)
        }
        is IfElseExprNode -> {
            collectTypeConstants(expr.condExpr(), into)
            collectTypeConstants(expr.thenExpr(), into)
            collectTypeConstants(expr.elseExpr(), into)
        }
        is IndexExprNode -> {
            collectTypeConstants(expr.base, into)
            collectTypeConstants(expr.index, into)
        }
        else -> expr.children.filterIsInstance<ExprNode>().forEach { collectTypeConstants(it, into) }
    }
}

/** Built-in domains that need a finite TLC model assignment in the .cfg. */
internal fun collectBuiltinDomainUses(expr: ExprNode, into: MutableSet<String>) {
    when (expr) {
        is QuantifiedExprNode -> {
            typeDomainConstant(expr.binderTypeExpr())?.let { name ->
                if (name in setOf("Int", "Nat", "Real")) {
                    into += name
                }
            }
            collectBuiltinDomainUses(expr.quantifiedBody(), into)
        }
        is UnaryOpExprNode -> collectBuiltinDomainUses(expr.operand(), into)
        is BinaryOpExprNode -> {
            collectBuiltinDomainUses(expr.lhsOperand(), into)
            collectBuiltinDomainUses(expr.rhsOperand(), into)
        }
        is IfElseExprNode -> {
            collectBuiltinDomainUses(expr.condExpr(), into)
            collectBuiltinDomainUses(expr.thenExpr(), into)
            collectBuiltinDomainUses(expr.elseExpr(), into)
        }
        is IndexExprNode -> {
            collectBuiltinDomainUses(expr.base, into)
            collectBuiltinDomainUses(expr.index, into)
        }
        else -> expr.children.filterIsInstance<ExprNode>().forEach { collectBuiltinDomainUses(it, into) }
    }
}

private fun exprIsStringTyped(expr: ExprNode): Boolean = try {
    expr.getType() is StringType
} catch (_: RuntimeException) {
    false
}

private fun tlaStringCoerce(expr: ExprNode, rendered: String): String =
    if (exprIsStringTyped(expr)) rendered else "ToString($rendered)"

/**
 * @param leafCtx map of leaf name → SpecLeaf (for FieldAccess context; optional)
 * @param argNames action argument symbols (emitted bare)
 * @param self index variable for the current parameterized leaf, or null
 * @param bareStateVars state vars that may appear unqualified in action guards/transits
 * @param reservedNames CONSTANT names; quantifier binders that clash are renamed
 */
internal fun exprToTla(
    expr: ExprNode,
    leafCtx: Map<String, SpecLeaf>,
    argNames: Set<String>,
    self: String?,
    bareStateVars: Set<String> = emptySet(),
    reservedNames: Set<String> = emptySet(),
): String {
    fun rec(e: ExprNode): String = exprToTla(e, leafCtx, argNames, self, bareStateVars, reservedNames)
    return when (expr) {
        is LiteralValueExprNode -> literalToTla(expr)
        is SymbolValueExprNode -> {
            val sym = expr.symbol
            when {
                sym in argNames -> sym
                sym in bareStateVars && leafCtx.size == 1 -> {
                    val leaf = leafCtx.values.first()
                    val v = tlaVar(leaf.name, sym)
                    if (self != null) "$v[$self]" else v
                }
                else -> sym
            }
        }
        is FieldAccessExprNode -> {
            val varName = expr.fieldPath.firstOrNull() ?: expr.baseSymbol
            tlaVar(expr.baseSymbol, varName)
        }
        is FieldAccessOnExprNode -> {
            val base = rec(expr.baseExpr)
            val path = expr.fieldPath
            if (path.isEmpty()) base
            else path.fold(base) { acc, field -> "$acc.$field" }
        }
        is ObjClassLiteralExprNode -> {
            val fields = expr.fieldEntries.joinToString(", ") { (name, e) ->
                "$name |-> ${rec(e)}"
            }
            "[$fields]"
        }
        is IndexExprNode -> {
            val base = expr.base
            if (base is FieldAccessExprNode) {
                val v = tlaVar(base.baseSymbol, base.fieldPath.first())
                "$v[${rec(expr.index)}]"
            } else {
                "${rec(base)}[${rec(expr.index)}]"
            }
        }
        is UnaryOpExprNode -> {
            val o = rec(expr.operand())
            when (expr.op()) {
                "~" -> "~($o)"
                "-" -> "(-$o)"
                else -> "(${expr.op()} $o)"
            }
        }
        is BinaryOpExprNode -> {
            val l = rec(expr.lhsOperand())
            val r = rec(expr.rhsOperand())
            when (expr.op()) {
                "&" -> "($l /\\ $r)"
                "|" -> "($l \\/ $r)"
                "=>" -> "($l => $r)"
                "=" -> "($l = $r)"
                "~=" -> "($l # $r)"
                "<" -> "($l < $r)"
                "<=" -> "($l <= $r)"
                ">" -> "($l > $r)"
                ">=" -> "($l >= $r)"
                "+" -> {
                    val stringy = exprIsStringTyped(expr) ||
                        exprIsStringTyped(expr.lhsOperand()) ||
                        exprIsStringTyped(expr.rhsOperand())
                    if (stringy) {
                        "(${tlaStringCoerce(expr.lhsOperand(), l)} \\o ${tlaStringCoerce(expr.rhsOperand(), r)})"
                    } else {
                        "($l + $r)"
                    }
                }
                "-" -> "($l - $r)"
                "*" -> "($l * $r)"
                "/" -> "($l \\div $r)"
                else -> "($l ${expr.op()} $r)"
            }
        }
        is QuantifiedExprNode -> {
            val domain = when (val t = expr.binderTypeExpr()) {
                is TypeExpr.Simple -> t.name
                else -> t.toString()
            }
            val origBinder = expr.binderName()
            val binder = if (origBinder in reservedNames) "q_$origBinder" else origBinder
            val body = rec(expr.quantifiedBody()).let { b ->
                if (binder != origBinder) {
                    b.replace(Regex("\\b${Regex.escape(origBinder)}\\b"), binder)
                } else {
                    b
                }
            }
            val q = if (expr.isUniversal()) "\\A" else "\\E"
            "($q $binder \\in $domain : $body)"
        }
        is IfElseExprNode -> {
            "(IF ${rec(expr.condExpr())} THEN ${rec(expr.thenExpr())} ELSE ${rec(expr.elseExpr())})"
        }
        else -> "TRUE"
    }
}

private fun literalToTla(expr: LiteralValueExprNode): String {
    return expr.toTransitString(emptyMap(), emptySet()).let { s ->
        when (s) {
            "true" -> "TRUE"
            "false" -> "FALSE"
            else -> s
        }
    }
}
