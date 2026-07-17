package julay.compiler.pass

import julay.compiler.*
import julay.compiler.ast.*
import julay.compiler.decl.ActionDecl
import julay.compiler.decl.ObjClassDecl
import julay.compiler.decl.ProcClassDecl
import julay.compiler.decl.ProcDecl
import julay.compiler.decl.TransitUpdate
import julay.program.*
import julay.program.library.LibraryRegistry

data class CodegenResult(
    val sourceText: String,
    val mainClassName: String,
)

fun codegenPass(
    ast: RootNode,
    program: ProcDecl,
    procDecls: List<ProcDecl>,
    librariesInUse: Set<String> = emptySet(),
): CodegenResult {
    val libPClassNames = librariesInUse
    val kotlinLibProcs = program.allProcNames(procDecls).filter { it in libPClassNames }
    val procsToCompile = program.allProcNames(procDecls).filter { it !in kotlinLibProcs }
    val procClasses = procsToCompile.flatMap { proc ->
        val procClass = ast.procClassPass(setOf(proc))
        julay.tools.assert(procClass.size == 1, "Expected exactly one proc class for \"$proc\" but found: ${procClass.size}")
        procClass
    }

    val servicedActionNames = (
        procClasses.flatMap { it.transitions } +
            librariesInUse.flatMap { LibraryRegistry.actionDecls(it) }
        )
        .filter { it.modifier == TSAction.SyncRole.Service }
        .map { it.action.name }
        .toSet()

    val libProcs = kotlinLibProcs
    val staticInfoLib = libProcs.map { LibraryRegistry.staticInfoCodegenExpr(it) }
    val staticInfoCompiledProcs = procClasses.map { it.kotlinStaticInfoString(servicedActionNames) }
    val staticInfoBody = (staticInfoCompiledProcs + staticInfoLib).joinToString(",\n") { it }
    val staticInfo = "val tsInfo = setOf(\n" + staticInfoBody.prependIndent() + "\n)"
    val objClassDecls = ast.resolvedObjClassDecls()
        .filterNot { ObjClassBuiltinRegistry.isBuiltin(it.name) }
    val runProgram = "Program(tsInfo, args.toList()).run()"
    val mainFunction = "suspend fun main(args : Array<String>) {" +
        "\n$staticInfo".prependIndent() +
        "\n$runProgram".prependIndent() +
        "\n}"

    val imports = "import com.microsoft.z3.*\n" +
        "import com.microsoft.z3.julangContext\n" +
        "import julay.program.*\n" +
        "import julay.program.library.*\n" +
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
    val conversionHelpers = objClassDecls.joinToString("\n\n") { it.kotlinConversionHelpersString() }
    val objClassCode = objClassDecls.joinToString("\n\n") { it.kotlinTypeValWithConvertersString() }
    val objClassSection = when {
        objClassCode.isEmpty() -> ""
        else -> buildString {
            append(conversionHelpers)
            append("\n\n")
            append(objClassCode)
            append("\n\n")
        }
    }
    val parametricTypeSection = parametricTypeValsSection(procClasses, objClassDecls)
    val mainClassName = program.name.replaceFirstChar { it.uppercase() }
    val effectImports = if (procClasses.any { it.usesEffects() }) {
        EffectBuiltinRegistry.kotlinCodegenImports().joinToString("\n") { "import $it" } + "\n"
    } else {
        ""
    }
    val sourceText = "$imports$effectImports\n" +
        dataClassSection +
        objClassSection +
        parametricTypeSection +
        procClasses.joinToString("\n\n") { it.kotlinClassString(objClassDecls, servicedActionNames) } +
        "\n\n" +
        mainFunction

    return CodegenResult(sourceText, mainClassName)
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
    constructors.any { it.effects.isNotEmpty() } || transitions.any { it.effects.isNotEmpty() }

private fun actionArgEnv(actionArgs: List<Variable>): Map<String, Type> =
    actionArgs.associate { it.name to it.type }

private fun actionArgSymbols(actionArgs: List<Variable>): Set<String> =
    actionArgs.map { it.name }.toSet()

private fun ActionDecl.kotlinEffectString(
    stateVarTypes: Map<String, Type>,
    assignPrefix: String = "",
): String {
    if (effects.isEmpty()) {
        return ""
    }
    val symbolTypes = stateVarTypes + actionArgEnv(action.args)
    val argSymbols = actionArgSymbols(action.args)
    return effects.joinToString("\n") {
        EffectBuiltinRegistry.effectStmtKotlinString(it, symbolTypes, argSymbols, assignPrefix)
    }
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
    servicedActionNames: Set<String>,
): String {
    val stateVarTypes = stateVars.associate { Pair(it.name, it.type) }
    val stateVarsStr = stateVars.joinToString(",\n") {
        "private var ${it.name.toKotlinIdent()}: ${it.type.toKotlinTypeString()}"
    }
    val registerTypes = ""
    val actionsStr = "override suspend fun actions(ctx: Context): Set<TSAction> = setOf(\n" +
        transitions.joinToString(",\n") {
            it.kotlinActionString(stateVarTypes, servicedActionNames).prependIndent()
        } +
        "\n)"
    val transitStr = "override suspend fun transit(act: ConcreteAction) {" +
        "\nreturn when (act.symAction.name) {".prependIndent() +
        transitions.joinToString("") {
            "\n\"${it.action.name}\" -> {" + "\n${it.kotlinTransitString(stateVarTypes)}".prependIndent() + "\n}"
        }.prependIndent().prependIndent() +
        "\nelse -> throw RuntimeException(\"Action is outside my alphabet: \${act.symAction}\")".prependIndent().prependIndent() +
        "\n}".prependIndent() +
        "\n}"
    val channelStateVars = stateVars.filter { it.type is ChannelType }
    val heldChannelsStr = if (channelStateVars.isEmpty()) {
        ""
    } else {
        val body = channelStateVars.joinToString(", ") { it.name.toKotlinIdent() }
        "\noverride fun heldChannels(): Set<Channel> = setOf($body)"
    }
    val allParams = if (stateVars.isEmpty()) {
        "private val program: Program"
    } else {
        "private val program: Program,\n$stateVarsStr"
    }
    return "class $name(" +
        "\n$allParams".prependIndent() +
        "\n) : TransitionSystem {" +
        registerTypes +
        "\n$actionsStr".prependIndent() +
        heldChannelsStr.prependIndent() +
        "\n$transitStr".prependIndent() +
        "\n}"
}

private fun ProcClassDecl.kotlinStaticInfoString(servicedActionNames: Set<String>): String {
    val transitionInfo = transitions.joinToString(",\n") {
        it.kotlinStaticInfoString(servicedActionNames).prependIndent()
    }
    val dynamicActs = transitions
        .filter { it.requiresDynamicChannel }
        .joinToString(",\n") {
            it.kotlinStaticInfoString(servicedActionNames).prependIndent()
        }
    val constructorPairs = constructors
        .joinToString(",\n") { ctor ->
            val actSigStr = ctor.kotlinStaticInfoString(servicedActionNames)
            val argSymbols = actionArgSymbols(ctor.action.args)
            val stateVarTypes = stateVars.associate { Pair(it.name, it.type) }
            val symbolTypes = stateVarTypes + actionArgEnv(ctor.action.args)
            val constructorArgs = ctor.transits
                .filterIsInstance<TransitUpdate.Assign>()
                .joinToString(", ") { assign ->
                    "${transitRootVar(assign.key).toKotlinIdent()} = ${assign.expr.toTransitString(symbolTypes, argSymbols)}"
                }
            val stateArgs = if (constructorArgs.isEmpty()) "" else ", $constructorArgs"
            val constructor = "$name(program$stateArgs)"
            // Error checks run before transits and effects so they see pre-state variables
            // and no effect happens upon an error.
            val errorStr = ctor.kotlinErrorString(stateVarTypes)
            val effectStr = ctor.kotlinEffectString(stateVarTypes, "result.")
            val constructStr = when {
                errorStr.isEmpty() && effectStr.isEmpty() ->
                    "{ program, act -> $constructor }"
                errorStr.isEmpty() ->
                    "{ program, act ->\nval result = $constructor\n$effectStr\nresult\n}"
                effectStr.isEmpty() ->
                    "{ program, act ->\n$errorStr\n$constructor\n}"
                else ->
                    "{ program, act ->\n$errorStr\nval result = $constructor\n$effectStr\nresult\n}"
            }
            "Pair($actSigStr, $constructStr)".prependIndent()
        }
    return "TransitionSystemStaticInfo(" +
        ("\n\"$name\"," +
            "\nsetOf(" +
            "\n$transitionInfo" +
            "\n)," +
            "\nmapOf<SymbolicAction, suspend (Program, ConcreteAction) -> TransitionSystem>(" +
            "\n$constructorPairs" +
            "\n)," +
            "\ndynamicChannelActions = setOf(" +
            "\n$dynamicActs" +
            "\n)").prependIndent() +
        "\n)"
}

private fun ActionDecl.resolvedSyncRole(servicedActionNames: Set<String>): TSAction.SyncRole =
    when {
        modifier == TSAction.SyncRole.Default && action.name in servicedActionNames ->
            TSAction.SyncRole.Consumer
        else -> modifier
    }

private fun ActionDecl.kotlinActionString(
    stateVarTypes: Map<String, Type>,
    servicedActionNames: Set<String>,
): String {
    val symbolTypes = stateVarTypes + actionArgEnv(action.args)
    val argSymbols = actionArgSymbols(action.args)

    val actionSigStr = kotlinStaticInfoString(servicedActionNames)
    val guardStr = if (guards.isEmpty()) {
        "ctx.mkTrue()"
    } else if (guards.size == 1) {
        guards[0].toZ3GuardString(symbolTypes, argSymbols)
    } else {
        val body = guards.joinToString(", ") { it.toZ3GuardString(symbolTypes, argSymbols) }
        "ctx.mkAnd($body)"
    }
    val syncRoleStr = when (resolvedSyncRole(servicedActionNames)) {
        TSAction.SyncRole.Default -> "TSAction.SyncRole.Default"
        TSAction.SyncRole.Internal -> "TSAction.SyncRole.Internal"
        TSAction.SyncRole.Service -> "TSAction.SyncRole.Service"
        TSAction.SyncRole.Consumer -> "TSAction.SyncRole.Consumer"
    }
    val channelStr = dynamicChannelVar?.let { chanVar ->
        ",\nchannel = ${chanVar.toKotlinIdent()}"
    } ?: ""
    return "TSAction(" +
        "\n$actionSigStr,".prependIndent() +
        "\n$guardStr,".prependIndent() +
        "\n$syncRoleStr$channelStr".prependIndent() +
        "\n)"
}

private fun ActionDecl.kotlinTransitString(stateVarTypes: Map<String, Type>): String {
    val symbolTypes = stateVarTypes + actionArgEnv(action.args)
    val argSymbols = actionArgSymbols(action.args)
    // Error checks run before transits and effects so they see pre-state variables
    // and no effect happens upon an error.
    val errorStr = kotlinErrorString(stateVarTypes)
    // Simultaneous assignment: evaluate every RHS against the pre-transit state, then
    // apply updates. Later lines must not observe earlier assignments in the same block
    // (e.g. `peersLeft := peersLeft - 1` then `step := if (peersLeft - 1 = 0) ...`).
    // Temps are explicitly typed so untyped builders like emptyList() still compile.
    val transitRhsSnapshots = mutableListOf<String>()
    val transitLines = transits.mapIndexed { i, update ->
        when (update) {
            is TransitUpdate.Assign -> {
                val rootVar = transitRootVar(update.key)
                val rootType = stateVarTypes.getValue(rootVar)
                val fieldPath = if (update.key.contains('.')) update.key.substringAfter('.').split('.') else emptyList()
                val rhsType = transitAssignRhsType(rootType, fieldPath)
                val temp = "__transitRhs_$i"
                val rhs = update.expr.toTransitString(symbolTypes, argSymbols)
                transitRhsSnapshots += "val $temp: ${rhsType.toKotlinTypeString()} = $rhs"
                copyAssignmentString(rootVar, rootType, fieldPath, temp)
            }
            is TransitUpdate.MapPut -> {
                val mapVar = update.mapVar.toKotlinIdent()
                val mapType = stateVarTypes.getValue(update.mapVar) as MapType
                val keyTemp = "__transitRhs_${i}_key"
                val valTemp = "__transitRhs_${i}_val"
                transitRhsSnapshots +=
                    "val $keyTemp: ${mapType.keyType.toKotlinTypeString()} = ${update.key.toTransitString(symbolTypes, argSymbols)}"
                transitRhsSnapshots +=
                    "val $valTemp: ${mapType.valueType.toKotlinTypeString()} = ${update.value.toTransitString(symbolTypes, argSymbols)}"
                // Key/value are pre-state; applying puts in order composes multiple updates
                // to the same map (TLA+-style EXCEPT with several fields).
                "$mapVar = $mapVar + ($keyTemp to $valTemp)"
            }
        }
    }
    // Snapshot all effect args from the pre-transit state before any effect runs.
    // e.g. println(step) prints the pre-transit value. Effects themselves still run
    // after transit so blocking effects like readln() see updated state.
    // Args do not see earlier assignments in the same effect block, so:
    //   effect:
    //     a := readln()
    //     println(a)
    // evaluates a in the pre-state on the second line.
    val effectArgSnapshots = mutableListOf<String>()
    val effectLines = effects.mapIndexed { i, stmt ->
        val argTemps = stmt.callArgs().mapIndexed { j, arg ->
            val temp = "__effectArg_${i}_$j"
            effectArgSnapshots += "val $temp = ${arg.toTransitString(symbolTypes, argSymbols)}"
            temp
        }
        EffectBuiltinRegistry.effectStmtKotlinString(
            stmt,
            symbolTypes,
            argSymbols,
            argStrings = argTemps,
        )
    }
    return listOfNotNull(errorStr.takeIf { it.isNotEmpty() })
        .plus(effectArgSnapshots)
        .plus(transitRhsSnapshots)
        .plus(transitLines)
        .plus(effectLines)
        .joinToString("\n")
}

private fun ActionDecl.kotlinStaticInfoString(servicedActionNames: Set<String> = emptySet()): String {
    val actionArgsStr = action.args.joinToString(", ") {
        "Variable(\"${it.name}\", ${it.type.toCodegenTypeVal()})"
    }
    val isInternal = action.isInternal || modifier == TSAction.SyncRole.Internal
    return if (isInternal) {
        "SymbolicAction(\"${action.name}\", listOf($actionArgsStr), isInternal = true)"
    } else {
        "SymbolicAction(\"${action.name}\", listOf($actionArgsStr))"
    }
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
