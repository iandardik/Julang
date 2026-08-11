package julay.compiler.pass

import julay.compiler.*
import julay.compiler.ast.*
import julay.compiler.decl.ActionDecl
import julay.compiler.decl.ObjClassDecl
import julay.compiler.decl.ProcClassDecl
import julay.compiler.decl.ProcDecl
import julay.compiler.decl.TransitUpdate
import julay.program.*
import julay.program.type.*
import julay.program.action.*
import julay.program.library.LibraryRegistry
import julay.program.sync.SyncResolveConfig

data class CodegenResult(
    val sourceText: String,
    val mainClassName: String,
    val syncPathStats: SyncPathStats = SyncPathStats.EMPTY,
)

fun codegenPass(
    ast: RootNode,
    program: ProcDecl,
    procDecls: List<ProcDecl>,
    librariesInUse: Set<String> = emptySet(),
    syncResolveConfig: SyncResolveConfig = SyncResolveConfig.ALL_ON,
): CodegenResult {
    val libPClassNames = librariesInUse
    val leafMap = leafActionMap(ast, program.allProcNames(procDecls), librariesInUse)
    val alphabet = computeCompositionAlphabet(
        program, procDecls, leafMap, collectProcFunNames(ast), ast,
    )
    val channelKeys = alphabet.channelKeys
    val procFunNames = collectProcFunNames(ast)
    // Procfuns in || are spec metadata only — do not SyncChannel-start them as JAR peers.
    // Their leaf occurrences still carry composition channel keys for procFunInfo / class bodies.
    val occurrences = alphabet.leafOccurrences.filter { it.pclassName !in procFunNames }
    val procFunOccurrences = alphabet.leafOccurrences.filter { it.pclassName in procFunNames }
    val procFunOccByName = procFunOccurrences.associateBy { it.pclassName }

    val distinctPclasses = occurrences.map { it.pclassName }.toSet()
    val kotlinLibProcs = distinctPclasses.filter { it in libPClassNames && LibraryRegistry.isKotlinLibrary(it) }
    val procsToCompile = distinctPclasses.filter { it !in kotlinLibProcs }
    val procClasses = procsToCompile.flatMap { proc ->
        val procClass = ast.procClassPass(setOf(proc))
        julay.tools.assert(procClass.size == 1, "Expected exactly one proc class for \"$proc\" but found: ${procClass.size}")
        procClass
    }
    val procFunClasses = ast.procFunClassPass()
    val procClassByName = procClasses.associateBy { it.name }

    // One StaticInfo per leaf occurrence (Julay and Kotlin libraries), with occurrence channel keys.
    val staticInfoExprs = occurrences.map { occ ->
        occurrenceStaticInfoExpr(occ, channelKeys, procClassByName, libPClassNames)
    }
    val staticInfoBody = staticInfoExprs.joinToString(",\n") { it }
    val staticInfo = "val tsInfo = setOf(\n" + staticInfoBody.prependIndent() + "\n)"
    val procFunStaticInfos = procFunClasses.joinToString(",\n") { pc ->
        val occId = procFunOccByName[pc.name]?.occurrenceId ?: pc.name
        pc.kotlinStaticInfoString(occurrenceId = occId, channelKeys = channelKeys)
    }
    val procFunInfoBlock = if (procFunClasses.isEmpty()) {
        "emptySet<TransitionSystemStaticInfo>()"
    } else {
        "setOf(\n${procFunStaticInfos.prependIndent()}\n)"
    }
    val procFunInfo = "val procFunInfo = $procFunInfoBlock"
    val objClassDecls = ast.resolvedObjClassDecls()
        .filterNot { ObjClassBuiltinRegistry.isBuiltin(it.name) }
        // Specs may define sort-bearing objs in the same CU; JAR codegen must not emit them.
        .filterNot { decl -> decl.fields.any { it.type.containsSortType() } }
    val runProgram =
        "Program(tsInfo, args.toList(), procFunInfo, ${SyncResolveConfig.toKotlinExpr(syncResolveConfig)}).run()"
    val mainFunction = "suspend fun main(args : Array<String>) {" +
        "\n$staticInfo".prependIndent() +
        "\n$procFunInfo".prependIndent() +
        "\n$runProgram".prependIndent() +
        "\n}"

    val imports = "import com.microsoft.z3.*\n" +
        "import com.microsoft.z3.julangContext\n" +
        "import julay.program.*\n" +
        "import julay.program.type.*\n" +
        "import julay.program.action.*\n" +
        "import julay.program.library.*\n" +
        "import julay.program.sync.*\n" +
        "import julay.tools.mkStringConst\n" +
        "import julay.tools.mkSeqLengthAny\n" +
        "import julay.tools.mkSeqNthAny\n" +
        "import julay.tools.mkSeqConcatAny\n" +
        "import julay.tools.mkListMemberAny\n" +
        "import julay.tools.mkSetMemberAny\n" +
        "import julay.tools.mkSetUnionAny\n" +
        "import julay.tools.mkSetDifferenceAny\n" +
        "import julay.tools.mkSetAddAny\n" +
        "import julay.tools.setCellArrExpr\n" +
        "import julay.tools.setCellSizeExpr\n" +
        "import julay.tools.setMkCellExpr\n" +
        "import julay.tools.mapCellArrExpr\n" +
        "import julay.tools.mapCellKeysExpr\n" +
        "import julay.tools.mapCellSizeExpr\n" +
        "import julay.tools.mapSelectExpr\n" +
        "import julay.tools.mapStoreExpr\n" +
        "import julay.tools.mapSetAddExpr\n" +
        "import julay.tools.mapMkCellExpr\n"
    val dataClassCode = objClassDecls.joinToString("\n\n") { it.kotlinDataClassString() }
    val dataClassSection = if (dataClassCode.isEmpty()) "" else "$dataClassCode\n\n"
    val (simpleObjDecls, complexObjDecls) = objClassDecls.partition { decl ->
        decl.fields.none { field ->
            field.type is ListType || field.type is SetType || field.type is MapType
        }
    }
    val simpleHelpers = simpleObjDecls.joinToString("\n\n") { it.kotlinConversionHelpersString() }
    val simpleObjCode = simpleObjDecls.joinToString("\n\n") { it.kotlinTypeValWithConvertersString() }
    val parametricTypeSection = parametricTypeValsSection(procClasses + procFunClasses, objClassDecls)
    val complexHelpers = complexObjDecls.joinToString("\n\n") { it.kotlinConversionHelpersString() }
    val complexObjCode = complexObjDecls.joinToString("\n\n") { it.kotlinTypeValWithConvertersString() }
    val objClassSection = buildString {
        if (simpleHelpers.isNotEmpty()) {
            append(simpleHelpers)
            append("\n\n")
        }
        if (simpleObjCode.isNotEmpty()) {
            append(simpleObjCode)
            append("\n\n")
        }
        if (parametricTypeSection.isNotEmpty()) {
            append(parametricTypeSection)
        }
        if (complexHelpers.isNotEmpty()) {
            append(complexHelpers)
            append("\n\n")
        }
        if (complexObjCode.isNotEmpty()) {
            append(complexObjCode)
            append("\n\n")
        }
    }
    val mainClassName = program.name.replaceFirstChar { it.uppercase() }
    val effectImports = if ((procClasses + procFunClasses).any { it.usesEffects() }) {
        EffectBuiltinRegistry.kotlinCodegenImports().joinToString("\n") { "import $it" } + "\n"
    } else {
        ""
    }
    // Class bodies use channel keys from the first occurrence of each class (remap still
    // handles further occurrences via StaticInfo.resolveAction). Include folded procfuns so
    // their actions share composition-hidden keys with leaf peers (e.g. startElectionTimeout).
    val classBodyKeys = LinkedHashMap<LeafActionId, String>()
    val seenClass = mutableSetOf<String>()
    for (occ in occurrences + procFunOccurrences) {
        if (!seenClass.add(occ.pclassName)) continue
        channelKeys.forEach { (id, key) ->
            if (id.occurrenceId == occ.occurrenceId && id.pclassKey == occ.pclassName) {
                // Class body lookup uses empty occurrenceId sentinel.
                classBodyKeys[LeafActionId(id.pclassKey, "", id.actionName, id.isConstructor)] = key
            }
        }
    }
    val allClasses = procClasses + procFunClasses
    val sourceText = "$imports$effectImports\n" +
        dataClassSection +
        objClassSection +
        allClasses.joinToString("\n\n") { it.kotlinClassString(objClassDecls, classBodyKeys) } +
        "\n\n" +
        mainFunction

    val syncPathStats = SyncPathStats(
        (procClasses + procFunClasses).map { it.syncPathProcStat() },
    )
    return CodegenResult(sourceText, mainClassName, syncPathStats)
}

private fun occurrenceStaticInfoExpr(
    occ: LeafOccurrence,
    channelKeys: Map<LeafActionId, String>,
    procClassByName: Map<String, ProcClassDecl>,
    libPClassNames: Set<String>,
): String {
    val overrides = channelKeys.entries
        .filter { it.key.occurrenceId == occ.occurrenceId && it.key.pclassKey == occ.pclassName }
        .filter { it.value != it.key.actionName }
        .associate { it.key.actionName to it.value }
    return if (occ.pclassName in libPClassNames && LibraryRegistry.isKotlinLibrary(occ.pclassName)) {
        val base = LibraryRegistry.staticInfoCodegenExpr(occ.pclassName)
        if (overrides.isEmpty()) {
            base
        } else {
            val mapEntries = overrides.entries.joinToString(", ") { (k, v) ->
                "\"${k.escapeKotlinStringLiteral()}\" to \"${v.escapeKotlinStringLiteral()}\""
            }
            "$base.withChannelKeys(mapOf($mapEntries))"
        }
    } else {
        val pc = procClassByName.getValue(occ.pclassName)
        pc.kotlinStaticInfoString(occ.occurrenceId, channelKeys)
    }
}

/**
 * File-level vals for monomorphized List/Set/Map types used by this compilation unit,
 * so generated actions/transit reuse one instance instead of calling listType()/setType()/mapType()
 * on every evaluation.
 */
private fun parametricTypeValsSection(
    procClasses: List<ProcClassDecl>,
    objClassDecls: List<ObjClassDecl>,
): String {
    val roots = mutableListOf<Type>()
    procClasses.forEach { pc ->
        pc.stateVars.forEach { roots.add(it.type) }
        (pc.constructors + pc.transitions).forEach { action ->
            action.action.args.forEach { roots.add(it.type) }
        }
    }
    objClassDecls.forEach { decl ->
        decl.fields.forEach { roots.add(it.type) }
    }
    // Program initially always uses List<String> in generated static info constructors.
    roots.add(listType(stringType))

    val collected = linkedSetOf<Type>()
    roots.forEach { collectParametricTypes(it, collected) }
    if (collected.isEmpty()) return ""

    val ordered = collected.sortedWith(
        compareBy<Type> { it.parametricNestingDepth() }
            .thenBy { it.toCodegenTypeVal() },
    )
    val decls = ordered.joinToString("\n") { ty ->
        val name = ty.toCodegenTypeVal()
        val rhs = when (ty) {
            is ListType -> "listType(${ty.elementType.toCodegenTypeVal()})"
            is SetType -> "setType(${ty.elementType.toCodegenTypeVal()})"
            is MapType -> "mapType(${ty.keyType.toCodegenTypeVal()}, ${ty.valueType.toCodegenTypeVal()})"
            else -> throw RuntimeException("Not a parametric type for codegen val: $ty")
        }
        "val $name = $rhs"
    }
    return "$decls\n\n"
}

private fun collectParametricTypes(type: Type, out: MutableSet<Type>) {
    when (type) {
        is ListType -> {
            collectParametricTypes(type.elementType, out)
            out.add(type)
        }
        is SetType -> {
            collectParametricTypes(type.elementType, out)
            out.add(type)
        }
        is MapType -> {
            collectParametricTypes(type.keyType, out)
            collectParametricTypes(type.valueType, out)
            out.add(type)
        }
        is ObjClassType -> type.fields.forEach { collectParametricTypes(it.type, out) }
        else -> {}
    }
}

private fun Type.parametricNestingDepth(): Int = when (this) {
    is ListType -> 1 + elementType.parametricNestingDepth()
    is SetType -> 1 + elementType.parametricNestingDepth()
    is MapType -> 1 + maxOf(keyType.parametricNestingDepth(), valueType.parametricNestingDepth())
    is ObjClassType -> fields.maxOfOrNull { it.type.parametricNestingDepth() } ?: 0
    else -> 0
}

private fun ObjClassDecl.kotlinDataClassString(): String {
    val fieldsStr = fields.joinToString(", ") {
        "val ${it.name}: ${it.type.toKotlinTypeString()}"
    }
    return "data class $name($fieldsStr)"
}

private fun ObjClassDecl.kotlinTypeValWithConvertersString(): String {
    val fieldsStr = fields.joinToString(", ") {
        "Variable(\"${it.name}\", ${it.type.toCodegenTypeVal()})"
    }
    val typeVal = objClassTypeValName(name)
    val toZ3Fun = objClassToZ3FunName(name)
    val fromZ3Fun = objClassFromZ3FunName(name)
    return """
        |val $typeVal = ObjClassType(
        |    "$name",
        |    listOf($fieldsStr),
        |    { value, ctx -> $toZ3Fun(ctx, value.value as $name) },
        |    { expr, model -> $fromZ3Fun(expr, model) },
        |)
    """.trimMargin()
}

private fun ObjClassDecl.kotlinConversionHelpersString(): String {
    val typeVal = objClassTypeValName(name)
    val mkFun = objClassMkFunName(name)
    val toZ3Fun = objClassToZ3FunName(name)
    val fromZ3Fun = objClassFromZ3FunName(name)
    val mkParams = fields.joinToString(", ") { field ->
        "${field.name}: Expr<*>"
    }
    val mkArgs = fields.joinToString(", ") { it.name }
    val accessorFuns = fields.mapIndexed { index, field ->
        val accFun = objClassAccessorFunName(name, field.name)
        val returnType = field.type.toZ3ExprTypeString()
        """
            |fun $accFun(ctx: Context, record: Expr<*>): $returnType =
            |    $typeVal.accessor(ctx, $index).apply(record) as $returnType
        """.trimMargin()
    }.joinToString("\n\n")
    val toZ3Args = fields.joinToString(", ") { field ->
        fieldToZ3ExprString("value.${field.name}", field.type)
    }
    val fromZ3FieldExprs = fields.mapIndexed { index, _ ->
        "$typeVal.accessor(modelCtx, $index).apply(expr) as Expr<*>"
    }.joinToString(",\n")
    val fromZ3Args = fields.mapIndexed { index, field ->
        fieldFromZ3ExprString("fieldExprs[$index]", field.type)
    }.joinToString(",\n")
    return """
        |fun $mkFun(ctx: Context, $mkParams): Expr<*> =
        |    $typeVal.constructorDecl(ctx).apply($mkArgs) as Expr<*>
        |
        |$accessorFuns
        |
        |fun $toZ3Fun(ctx: Context, value: $name): Expr<*> =
        |    $mkFun(ctx, $toZ3Args)
        |
        |fun $fromZ3Fun(expr: Expr<*>, model: Model): $name {
        |    val fieldExprs = if (expr.isApp && expr.funcDecl.name.toString() == $typeVal.constructorName) {
        |        expr.args
        |    } else {
        |        val modelCtx = model.julangContext()
        |        arrayOf(
        |${fromZ3FieldExprs.prependIndent("            ")}
        |        )
        |    }
        |    return $name(
        |${fromZ3Args.prependIndent("        ")}
        |    )
        |}
    """.trimMargin()
}

private fun fieldToZ3ExprString(valueExpr: String, type: Type): String = when (type) {
    is BoolType -> "ctx.mkBool($valueExpr)"
    is IntType -> "ctx.mkInt($valueExpr)"
    is RealType -> "ctx.mkReal($valueExpr.toString())"
    is StringType -> "ctx.mkString($valueExpr)"
    is ObjClassType -> "${objClassToZ3FunName(type.name)}(ctx, $valueExpr)"
    is ListType -> "${type.toCodegenTypeVal()}.toZ3Expr(Value($valueExpr, ${type.toCodegenTypeVal()}), ctx)"
    else -> throw RuntimeException("Invalid field type for Z3 conversion: $type")
}

private fun fieldFromZ3ExprString(exprStr: String, type: Type): String = when (type) {
    is BoolType -> "boolType.fromZ3Expr($exprStr, model) as Boolean"
    is IntType -> "intType.fromZ3Expr($exprStr, model) as Int"
    is RealType -> "realType.fromZ3Expr($exprStr, model) as Double"
    is StringType -> "stringType.fromZ3Expr($exprStr, model) as String"
    is ObjClassType -> "${objClassFromZ3FunName(type.name)}($exprStr, model)"
    is ListType ->
        "@Suppress(\"UNCHECKED_CAST\") (${type.toCodegenTypeVal()}.fromZ3Expr($exprStr, model) as ${type.toKotlinTypeString()})"
    else -> throw RuntimeException("Invalid field type for Z3 conversion: $type")
}

private fun Type.toZ3ExprTypeString(): String = when (this) {
    is BoolType -> "BoolExpr"
    is IntType -> "IntExpr"
    is RealType -> "RealExpr"
    is StringType -> "Expr<SeqSort<CharSort>>"
    is ObjClassType -> "Expr<*>"
    is ListType -> "Expr<*>"
    else -> throw RuntimeException("Invalid field type for Z3 expr: $this")
}

private fun ProcClassDecl.usesEffects(): Boolean =
    constructors.any { it.befores.isNotEmpty() || it.afters.isNotEmpty() } ||
        transitions.any { it.befores.isNotEmpty() || it.afters.isNotEmpty() }

private fun actionArgEnv(actionArgs: List<Variable>): Map<String, Type> =
    actionArgs.associate { it.name to it.type }

private fun actionArgSymbols(actionArgs: List<Variable>): Set<String> =
    actionArgs.map { it.name }.toSet()

private fun ActionDecl.kotlinCallStmtsString(
    stmts: List<CallStmtNode>,
    stateVarTypes: Map<String, Type>,
    argPrefix: String,
): Pair<List<String>, List<String>> {
    if (stmts.isEmpty()) {
        return emptyList<String>() to emptyList()
    }
    val symbolTypes = stateVarTypes + actionArgEnv(action.args)
    val argSymbols = actionArgSymbols(action.args)
    val argSnapshots = mutableListOf<String>()
    val lines = stmts.mapIndexed { i, stmt ->
        val argTemps = stmt.callArgs().mapIndexed { j, arg ->
            if (stmt.callName() in EffectBuiltinRegistry.sessionPeerClassNameEffects) {
                val peerName = (arg as? SymbolValueExprNode)?.symbol
                    ?: throw RuntimeException(
                        "Expected \"${stmt.callName()}\" argument to be a leaf proc class name",
                    )
                "\"${peerName.escapeKotlinStringLiteral()}\""
            } else {
                val temp = "${argPrefix}_${i}_$j"
                argSnapshots += "val $temp = ${arg.toTransitString(symbolTypes, argSymbols)}"
                temp
            }
        }
        EffectBuiltinRegistry.callStmtKotlinString(
            stmt,
            symbolTypes,
            argSymbols,
            argStrings = argTemps,
        )
    }
    return argSnapshots to lines
}

private fun ActionDecl.kotlinErrorString(
    stateVarTypes: Map<String, Type>,
): String {
    if (errors.isEmpty()) {
        return ""
    }
    val symbolTypes = stateVarTypes + actionArgEnv(action.args)
    val argSymbols = actionArgSymbols(action.args)
    // Error checks run before transits and effects so they see pre-state variables
    // and no effect happens upon an error.
    return errors.joinToString("\n") { arm ->
        val cond = arm.condExpr().toTransitString(symbolTypes, argSymbols)
        val msg = arm.msgExpr().toTransitString(symbolTypes, argSymbols)
        "if ($cond) throw JulayException(($msg).toString())"
    }
}

private fun ProcClassDecl.kotlinClassString(
    objClassDecls: List<ObjClassDecl>,
    channelKeys: Map<LeafActionId, String>,
): String {
    val stateVarTypes = stateVars.associate { Pair(it.name, it.type) }
    // Nullable backing fields start as null; property accessors throw until finishConstruction.
    // Synthetic F_ret is TLA/alphabet-only; runtime completes on return: via returnExpr.
    val runtimeTransitions = transitions.filterNot { it.action.name == procFunRetAction(name) }
    val stateFieldsStr = stateVars.joinToString("\n") {
        val ident = it.name.toKotlinIdent()
        val ty = it.type.toKotlinTypeString()
        "private var _$ident: $ty? = null\n" +
            "private var $ident: $ty\n" +
            "    get() = _$ident!!\n" +
            "    set(value) { _$ident = value }"
    }
    val registerTypes = ""
    val actionsStr = "override suspend fun actions(ctx: Context): Set<TSAction> = setOf(\n" +
        runtimeTransitions.joinToString(",\n") {
            it.kotlinActionString(stateVarTypes, name, channelKeys).prependIndent()
        } +
        "\n)"
    val syncStepPlanStr = kotlinSyncStepPlanString(runtimeTransitions, stateVarTypes, name, channelKeys)
    val transitStr = "override suspend fun transit(act: ConcreteAction) {" +
        "\nreturn when (act.symAction.name) {".prependIndent() +
        runtimeTransitions.joinToString("") {
            "\n\"${it.action.name}\" -> {" + "\n${it.kotlinTransitString(stateVarTypes)}".prependIndent() + "\n}"
        }.prependIndent().prependIndent() +
        "\nelse -> throw RuntimeException(\"Action is outside my alphabet: \${act.symAction}\")".prependIndent().prependIndent() +
        "\n}".prependIndent() +
        "\n}"
    val finishConstructionBody = if (constructors.isEmpty()) {
        ""
    } else {
        constructors.joinToString("") { ctor ->
            "\n\"${ctor.action.name}\" -> {" +
                "\n${ctor.kotlinTransitString(stateVarTypes)}".prependIndent() +
                "\n}"
        }
    }
    val finishConstructionStr = "override suspend fun finishConstruction(act: ConcreteAction) {" +
        "\nwhen (act.symAction.name) {".prependIndent() +
        finishConstructionBody.prependIndent().prependIndent() +
        "\nelse -> {}".prependIndent().prependIndent() +
        "\n}".prependIndent() +
        "\n}"
    val hostBindStr =
        "private lateinit var hostProc: Proc\n" +
            "private var sessionPeer: Proc? = null\n" +
            "private var _procFunReturn: Value? = null\n" +
            "override fun bindHostProc(host: Proc) { hostProc = host }\n" +
            "override fun setSessionPeer(peer: Proc?) { sessionPeer = peer }\n" +
            "override fun consumeProcFunReturn(): Value? {\n" +
            "    val v = _procFunReturn\n" +
            "    _procFunReturn = null\n" +
            "    return v\n" +
            "}"
    return "class $name(" +
        "\nprivate val program: Program".prependIndent() +
        "\n) : TransitionSystem {" +
        "\n${hostBindStr.prependIndent()}" +
        (if (stateFieldsStr.isEmpty()) "" else "\n${stateFieldsStr.prependIndent()}") +
        registerTypes +
        "\n$finishConstructionStr".prependIndent() +
        "\n$actionsStr".prependIndent() +
        "\n$syncStepPlanStr".prependIndent() +
        "\n$transitStr".prependIndent() +
        "\n}"
}

private fun ProcClassDecl.runtimeTransitionsForStaticInfo(): List<ActionDecl> =
    transitions.filterNot { it.action.name == procFunRetAction(name) }

/** Compile-time FastOnly/NeedsZ3 + per-action fastGuard classification (same as codegen emit). */
private fun ProcClassDecl.syncPathProcStat(): SyncPathProcStat {
    val stateVarTypes = stateVars.associate { it.name to it.type }
    val actions = runtimeTransitionsForStaticInfo().map { it.syncPathActionStat(stateVarTypes) }
    // Match kotlinSyncStepPlanString: empty transition set → FastOnly; else all guards lower.
    val fastOnly = actions.all { it.hasFastGuard }
    return SyncPathProcStat(
        name = name,
        fastOnly = fastOnly,
        actions = actions,
    )
}

private fun ActionDecl.syncPathActionStat(stateVarTypes: Map<String, Type>): SyncPathActionStat {
    val symbolTypes = stateVarTypes + actionArgEnv(action.args)
    val argSymbols = actionArgSymbols(action.args)
    val fast = guards.toCombinedBoolExprFastOrNull(symbolTypes, argSymbols)
    return if (fast != null) {
        SyncPathActionStat(name = action.name, hasFastGuard = true)
    } else {
        SyncPathActionStat(
            name = action.name,
            hasFastGuard = false,
            opaqueReason = guards.opaqueFastGuardReason(symbolTypes, argSymbols),
        )
    }
}

private fun ProcClassDecl.kotlinStaticInfoString(
    occurrenceId: String,
    channelKeys: Map<LeafActionId, String>,
): String {
    val transitionInfo = runtimeTransitionsForStaticInfo().joinToString(",\n") {
        it.kotlinStaticInfoString(
            name,
            occurrenceId,
            channelKeys,
            isConstructor = false,
        ).prependIndent()
    }
    // Factory only allocates an uninitialized instance (null state). Constructor transit and
    // effects run later on the child proc via TransitionSystem.finishConstruction.
    val constructorPairs = constructors
        .joinToString(",\n") { ctor ->
            val actSigStr = ctor.kotlinStaticInfoString(
                name,
                occurrenceId,
                channelKeys,
                isConstructor = true,
            )
            val constructStr = "{ program, _ -> $name(program) }"
            "Pair($actSigStr, $constructStr)".prependIndent()
        }
    return "TransitionSystemStaticInfo(" +
        ("\n\"$name\"," +
            "\nsetOf(" +
            "\n$transitionInfo" +
            "\n)," +
            "\nmapOf<SymbolicAction, suspend (Program, ConcreteAction) -> TransitionSystem>(" +
            "\n$constructorPairs" +
            "\n)").prependIndent() +
        "\n)"
}

/**
 * Sync role for Kotlin [TSAction] / SyncChannel sizing is always [ActionDecl.modifier].
 * Codegen never rewrites tags for `return:` (or any other) steps. Synthetic `_ret` remains
 * TLA/alphabet-only; runtime delivers via [returnExpr] when the source-tagged step fires.
 * Solo completion under spawn-and-await requires an explicit `internal` (or a real peer).
 */
private fun ActionDecl.kotlinActionString(
    stateVarTypes: Map<String, Type>,
    pclassName: String,
    channelKeys: Map<LeafActionId, String>,
): String {
    val symbolTypes = stateVarTypes + actionArgEnv(action.args)
    val argSymbols = actionArgSymbols(action.args)

    val actionSigStr = kotlinStaticInfoString(
        pclassName,
        occurrenceId = "",
        channelKeys,
        isConstructor = false,
    )
    val guardStr = if (guards.isEmpty()) {
        "ctx.mkTrue()"
    } else if (guards.size == 1) {
        guards[0].toZ3GuardString(symbolTypes, argSymbols)
    } else {
        val body = guards.joinToString(", ") { it.toZ3GuardString(symbolTypes, argSymbols) }
        "ctx.mkAnd($body)"
    }
    val syncRoleStr = when (modifier) {
        TSAction.SyncRole.Default -> "TSAction.SyncRole.Default"
        TSAction.SyncRole.Internal -> "TSAction.SyncRole.Internal"
        TSAction.SyncRole.Provider -> "TSAction.SyncRole.Provider"
        TSAction.SyncRole.Client -> "TSAction.SyncRole.Client"
    }
    val fastIr = guards.toCombinedBoolExprFastOrNull(symbolTypes, argSymbols)
    val fastParam = if (fastIr != null) {
        ",\nfastGuard = $fastIr"
    } else {
        ""
    }
    return "TSAction(" +
        "\n$actionSigStr,".prependIndent() +
        "\n$guardStr,".prependIndent() +
        "\n$syncRoleStr".prependIndent() +
        fastParam.prependIndent() +
        "\n)"
}

/**
 * Emit [SyncStepPlan]: if every transition guard lowers to [BoolExprFast], [FastOnly] so Proc
 * can skip Z3 Contexts on that step; otherwise [NeedsZ3] (residual BoolExpr path).
 */
private fun kotlinSyncStepPlanString(
    runtimeTransitions: List<ActionDecl>,
    stateVarTypes: Map<String, Type>,
    pclassName: String,
    channelKeys: Map<LeafActionId, String>,
): String {
    val classified = runtimeTransitions.map { decl ->
        val symbolTypes = stateVarTypes + actionArgEnv(decl.action.args)
        val argSymbols = actionArgSymbols(decl.action.args)
        val ir = decl.guards.toCombinedBoolExprFastOrNull(symbolTypes, argSymbols)
        decl to ir
    }
    if (classified.any { it.second == null }) {
        return "override fun syncStepPlan(): SyncStepPlan = SyncStepPlan.NeedsZ3"
    }
    val localEntries = stateVarTypes.entries
        .filter { (_, ty) ->
            ty is julay.program.type.IntType ||
                ty is julay.program.type.BoolType ||
                ty is julay.program.type.StringType
        }
        .joinToString(", ") { (name, _) ->
            // Use nullable backing fields so syncStepPlan is safe before/without init.
            "\"${name.escapeKotlinStringLiteral()}\" to _${name.toKotlinIdent()}"
        }
    val offerBlocks = classified.joinToString("\n") { (decl, ir) ->
        val actionSigStr = decl.kotlinStaticInfoString(
            pclassName,
            occurrenceId = "",
            channelKeys,
            isConstructor = false,
        )
        val syncRoleStr = when (decl.modifier) {
            TSAction.SyncRole.Default -> "TSAction.SyncRole.Default"
            TSAction.SyncRole.Internal -> "TSAction.SyncRole.Internal"
            TSAction.SyncRole.Provider -> "TSAction.SyncRole.Provider"
            TSAction.SyncRole.Client -> "TSAction.SyncRole.Client"
        }
        """
        run {
            val __g = $ir
            val __grounded = SyncResolveFast.groundForOffer(__g, __locals)
            if (__grounded != null) {
                __offers.add(FastOffer($actionSigStr, __grounded, $syncRoleStr))
            }
        }
        """.trimIndent()
    }
    return """
override fun syncStepPlan(): SyncStepPlan {
    val __locals: Map<String, Any?> = mapOf($localEntries)
    val __offers = mutableListOf<FastOffer>()
${offerBlocks.prependIndent()}
    return SyncStepPlan.FastOnly(__offers)
}
""".trimIndent()
}

private fun ActionDecl.kotlinTransitString(stateVarTypes: Map<String, Type>): String {
    val symbolTypes = stateVarTypes + actionArgEnv(action.args)
    val argSymbols = actionArgSymbols(action.args)
    // Error checks run before before/transit/after so they see pre-state variables
    // and no side effects happen upon an error.
    val errorStr = kotlinErrorString(stateVarTypes)
    val (beforeArgSnapshots, beforeLines) = kotlinCallStmtsString(befores, stateVarTypes, "__beforeArg")
    if (returnExpr != null) {
        val retType = returnExpr.getType()
        val retStr = returnExpr.toTransitString(symbolTypes, argSymbols)
        val (afterArgSnapshots, afterLines) = kotlinCallStmtsString(afters, stateVarTypes, "__afterArg")
        return listOfNotNull(errorStr.takeIf { it.isNotEmpty() })
            .plus(beforeArgSnapshots)
            .plus(beforeLines)
            .plus(afterArgSnapshots)
            .plus("val __procFunRet: ${retType.toKotlinTypeString()} = $retStr")
            .plus("_procFunReturn = Value(__procFunRet, ${retType.toCodegenTypeVal()})")
            .plus(afterLines)
            .joinToString("\n")
    }
    // Simultaneous assignment: evaluate every RHS against the pre-transit state, then
    // apply updates. Later lines must not observe earlier assignments in the same block
    // (e.g. `peersLeft := peersLeft - 1` then `step := if (peersLeft - 1 = 0) ...`).
    // Transit lets are evaluated in order against pre-state (+ earlier lets) and are in
    // scope for assignment RHSs textually below them. Temps are explicitly typed so
    // untyped builders like emptyList() still compile.
    var transitSymbolTypes = symbolTypes
    var transitArgSymbols = argSymbols
    var letReplacements = emptyMap<String, ExprNode>()
    fun substTransitLets(expr: ExprNode): ExprNode {
        var result = expr
        for ((name, replacement) in letReplacements) {
            result = substituteExpr(result, name, replacement)
        }
        return result
    }

    val transitEvalSnapshots = mutableListOf<String>()
    val transitApplyLines = mutableListOf<String>()
    var assignIndex = 0
    var discardIndex = 0
    for (update in transits) {
        when (update) {
            is TransitUpdate.Let -> {
                // Discard bindings (`_`) may repeat; give each a unique Kotlin temp and never
                // register them for later substitution (they cannot be referenced).
                val localBind = if (update.name.isDiscardBinding()) {
                    "__discard_${discardIndex++}__transitLet"
                } else {
                    "${update.name.toKotlinIdent()}__transitLet"
                }
                val init = substTransitLets(update.init)
                val initStr = init.toTransitString(transitSymbolTypes, transitArgSymbols)
                transitEvalSnapshots +=
                    "val $localBind: ${update.type.toKotlinTypeString()} = $initStr"
                transitSymbolTypes = transitSymbolTypes + (localBind to update.type)
                if (!update.name.isDiscardBinding()) {
                    val replacement = SymbolValueExprNode(localBind, update.init.programLocation()).also {
                        it.setInferredType(TypePassType.Inferred(update.type))
                    }
                    letReplacements = letReplacements + (update.name to replacement)
                    transitArgSymbols = transitArgSymbols - update.name
                }
            }
            is TransitUpdate.Assign -> {
                val rootVar = transitRootVar(update.key)
                val rootType = stateVarTypes.getValue(rootVar)
                val fieldPath = if (update.key.contains('.')) update.key.substringAfter('.').split('.') else emptyList()
                val rhsType = transitAssignRhsType(rootType, fieldPath)
                val temp = "__transitRhs_$assignIndex"
                val rhs = substTransitLets(update.expr).toTransitString(transitSymbolTypes, transitArgSymbols)
                transitEvalSnapshots += "val $temp: ${rhsType.toKotlinTypeString()} = $rhs"
                transitApplyLines += copyAssignmentString(rootVar, rootType, fieldPath, temp)
                assignIndex++
            }
            is TransitUpdate.IndexPut -> {
                val collectionVar = update.collectionVar.toKotlinIdent()
                val collectionType = stateVarTypes.getValue(update.collectionVar)
                val idxTemp = "__transitRhs_${assignIndex}_key"
                val valTemp = "__transitRhs_${assignIndex}_val"
                when (collectionType) {
                    is MapType -> {
                        transitEvalSnapshots +=
                            "val $idxTemp: ${collectionType.keyType.toKotlinTypeString()} = ${substTransitLets(update.index).toTransitString(transitSymbolTypes, transitArgSymbols)}"
                        transitEvalSnapshots +=
                            "val $valTemp: ${collectionType.valueType.toKotlinTypeString()} = ${substTransitLets(update.value).toTransitString(transitSymbolTypes, transitArgSymbols)}"
                        // Index/value are pre-state; applying puts in order composes multiple updates
                        // to the same map (TLA+-style EXCEPT with several fields).
                        transitApplyLines += "$collectionVar = $collectionVar + ($idxTemp to $valTemp)"
                    }
                    is ListType -> {
                        transitEvalSnapshots +=
                            "val $idxTemp: Int = ${substTransitLets(update.index).toTransitString(transitSymbolTypes, transitArgSymbols)}"
                        transitEvalSnapshots +=
                            "val $valTemp: ${collectionType.elementType.toKotlinTypeString()} = ${substTransitLets(update.value).toTransitString(transitSymbolTypes, transitArgSymbols)}"
                        // Index/value are pre-state; applying sets in order composes multiple updates
                        // to the same list (TLA+-style EXCEPT with several fields).
                        // Julay lists are 1-based; Kotlin List is 0-based.
                        transitApplyLines +=
                            "$collectionVar = $collectionVar.toMutableList().also { it[($idxTemp) - 1] = $valTemp }"
                    }
                    else -> throw RuntimeException(
                        "IndexPut expected map or list state var but \"${update.collectionVar}\" has type $collectionType",
                    )
                }
                assignIndex++
            }
        }
    }
    // Snapshot all after args from the pre-transit state (same as historical effect args).
    val (afterArgSnapshots, afterLines) = kotlinCallStmtsString(afters, stateVarTypes, "__afterArg")
    return listOfNotNull(errorStr.takeIf { it.isNotEmpty() })
        .plus(beforeArgSnapshots)
        .plus(beforeLines)
        .plus(afterArgSnapshots)
        .plus(transitEvalSnapshots)
        .plus(transitApplyLines)
        .plus(afterLines)
        .joinToString("\n")
}

private fun ActionDecl.kotlinStaticInfoString(
    pclassName: String = "",
    occurrenceId: String = "",
    channelKeys: Map<LeafActionId, String> = emptyMap(),
    isConstructor: Boolean = false,
): String {
    val actionArgsStr = action.args.joinToString(", ") {
        "Variable(\"${it.name}\", ${it.type.toCodegenTypeVal()})"
    }
    val resolvedKey = if (occurrenceId.isNotEmpty()) {
        channelKeys[LeafActionId(pclassName, occurrenceId, action.name, isConstructor)]
            ?: action.channelKey
    } else {
        channelKeys[LeafActionId(pclassName, "", action.name, isConstructor)]
            ?: action.channelKey
    }
    val flags = buildList {
        if (action.isInternal || modifier == TSAction.SyncRole.Internal) {
            add("isInternal = true")
        }
        if (action.isSession) add("isSession = true")
        if (resolvedKey != action.name) add("channelKey = \"${resolvedKey.escapeKotlinStringLiteral()}\"")
    }
    val flagStr = if (flags.isEmpty()) "" else ", " + flags.joinToString(", ")
    return "SymbolicAction(\"${action.name}\", listOf($actionArgsStr)$flagStr)"
}

/** Type of the value written by `root.fieldPath := …` (leaf field, or the root itself). */
private fun transitAssignRhsType(rootType: Type, fieldPath: List<String>): Type {
    if (fieldPath.isEmpty()) {
        return rootType
    }
    var current = rootType as ObjClassType
    for ((index, fieldName) in fieldPath.withIndex()) {
        val fieldType = current.fields.first { it.name == fieldName }.type
        if (index == fieldPath.lastIndex) {
            return fieldType
        }
        current = fieldType as ObjClassType
    }
    return rootType
}
