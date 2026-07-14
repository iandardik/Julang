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

    val libProcs = kotlinLibProcs
    val staticInfoLib = libProcs.map { LibraryRegistry.staticInfoCodegenExpr(it) }
    val staticInfoCompiledProcs = procClasses.map { it.kotlinStaticInfoString() }
    val staticInfoBody = (staticInfoCompiledProcs + staticInfoLib).joinToString(",\n") { it }
    val staticInfo = "val tsInfo = setOf(\n" + staticInfoBody.prependIndent() + "\n)"
    val objClassDecls = ast.resolvedObjClassDecls()
        .filterNot { ObjClassBuiltinRegistry.isBuiltin(it.name) }
    val runProgram = "Program(tsInfo, args.toList()).run()"
    val mainFunction = "suspend fun main(args : Array<String>) {" +
        "\n$staticInfo".prependIndent() +
        "\n$runProgram".prependIndent() +
        "\n}"

    // Avoid `import io.github.cvc5.*` — cvc5.Pair clashes with kotlin.Pair in generated mapOf(Pair(...)).
    val imports = "import io.github.cvc5.Kind\n" +
        "import io.github.cvc5.Solver\n" +
        "import io.github.cvc5.Term\n" +
        "import io.github.cvc5.TermManager\n" +
        "import julay.program.*\n" +
        "import julay.program.library.*\n" +
        "import julay.tools.mkStringConst\n" +
        "import julay.tools.mkKotlinString\n" +
        "import julay.tools.mkSeqLength\n" +
        "import julay.tools.mkSeqNth\n" +
        "import julay.tools.mkSeqConcat\n" +
        "import julay.tools.mkListMember\n" +
        "import julay.tools.mkSetMember\n" +
        "import julay.tools.mkSetUnion\n" +
        "import julay.tools.mkSetDifference\n" +
        "import julay.tools.mkSetAdd\n" +
        "import julay.tools.setCellArrExpr\n" +
        "import julay.tools.setCellSizeExpr\n" +
        "import julay.tools.setMkCellExpr\n" +
        "import julay.tools.mapCellArrExpr\n" +
        "import julay.tools.mapCellKeysExpr\n" +
        "import julay.tools.mapCellSizeExpr\n" +
        "import julay.tools.mapSelectExpr\n" +
        "import julay.tools.mapStoreExpr\n" +
        "import julay.tools.mapSetAddExpr\n" +
        "import julay.tools.mapMkCellExpr\n" +
        "import julay.tools.applyConstructor\n" +
        "import julay.tools.applySelector\n"
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
    val mainClassName = program.name.replaceFirstChar { it.uppercase() }
    val effectImports = if (procClasses.any { it.usesEffects() }) {
        EffectBuiltinRegistry.kotlinCodegenImports().joinToString("\n") { "import $it" } + "\n"
    } else {
        ""
    }
    val sourceText = "$imports$effectImports\n" +
        dataClassSection +
        objClassSection +
        procClasses.joinToString("\n\n") { it.kotlinClassString(objClassDecls) } +
        "\n\n" +
        mainFunction

    return CodegenResult(sourceText, mainClassName)
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
    val toSmtFun = objClassToSmtFunName(name)
    val fromSmtFun = objClassFromSmtFunName(name)
    return """
        |val $typeVal = ObjClassType(
        |    "$name",
        |    listOf($fieldsStr),
        |    { value, tm -> $toSmtFun(tm, value.value as $name) },
        |    { expr, solver -> $fromSmtFun(expr, solver) },
        |)
    """.trimMargin()
}

private fun ObjClassDecl.kotlinConversionHelpersString(): String {
    val typeVal = objClassTypeValName(name)
    val mkFun = objClassMkFunName(name)
    val toSmtFun = objClassToSmtFunName(name)
    val fromSmtFun = objClassFromSmtFunName(name)
    val mkParams = fields.joinToString(", ") { field ->
        "${field.name}: Term"
    }
    val mkArgs = fields.joinToString(", ") { it.name }
    val accessorFuns = fields.mapIndexed { index, field ->
        val accFun = objClassAccessorFunName(name, field.name)
        """
            |fun $accFun(tm: TermManager, record: Term): Term =
            |    applySelector(tm, $typeVal.selector(tm, $index), record)
        """.trimMargin()
    }.joinToString("\n\n")
    val toSmtArgs = fields.joinToString(", ") { field ->
        fieldToSmtExprString("value.${field.name}", field.type)
    }
    val fromSmtArgs = fields.mapIndexed { index, field ->
        // APPLY_CONSTRUCTOR children: [ctor, field0, field1, ...] — avoid re-declaring
        // datatypes on the solver TM via selectors after SMT-LIB parse.
        fieldFromSmtExprString("valued.getChild(${index + 1})", field.type)
    }.joinToString(",\n")
    return """
        |fun $mkFun(tm: TermManager, $mkParams): Term =
        |    applyConstructor(tm, $typeVal.constructorTerm(tm), arrayOf($mkArgs))
        |
        |$accessorFuns
        |
        |fun $toSmtFun(tm: TermManager, value: $name): Term =
        |    $mkFun(tm, $toSmtArgs)
        |
        |fun $fromSmtFun(expr: Term, solver: Solver): $name {
        |    val valued = solver.getValue(expr)
        |    return $name(
        |${fromSmtArgs.prependIndent("        ")}
        |    )
        |}
    """.trimMargin()
}

private fun fieldToSmtExprString(valueExpr: String, type: Type): String = when (type) {
    is BoolType -> "tm.mkBoolean($valueExpr)"
    is IntType -> "tm.mkInteger($valueExpr.toLong())"
    is RealType -> "tm.mkReal($valueExpr.toString())"
    is StringType -> "tm.mkKotlinString($valueExpr)"
    is ObjClassType -> "${objClassToSmtFunName(type.name)}(tm, $valueExpr)"
    is ListType -> "${type.toCodegenTypeVal()}.toSmtTerm(Value($valueExpr, ${type.toCodegenTypeVal()}), tm)"
    else -> throw RuntimeException("Invalid field type for SMT conversion: $type")
}

private fun fieldFromSmtExprString(exprStr: String, type: Type): String = when (type) {
    is BoolType -> "boolType.fromSmtTerm($exprStr, solver) as Boolean"
    is IntType -> "intType.fromSmtTerm($exprStr, solver) as Int"
    is RealType -> "realType.fromSmtTerm($exprStr, solver) as Double"
    is StringType -> "stringType.fromSmtTerm($exprStr, solver) as String"
    is ObjClassType -> "${objClassFromSmtFunName(type.name)}($exprStr, solver)"
    is ListType ->
        "@Suppress(\"UNCHECKED_CAST\") (${type.toCodegenTypeVal()}.fromSmtTerm($exprStr, solver) as ${type.toKotlinTypeString()})"
    else -> throw RuntimeException("Invalid field type for SMT conversion: $type")
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

private fun ProcClassDecl.kotlinClassString(objClassDecls: List<ObjClassDecl>): String {
    val stateVarTypes = stateVars.associate { Pair(it.name, it.type) }
    val stateVarsStr = stateVars.joinToString(",\n") {
        "private var ${it.name.toKotlinIdent()}: ${it.type.toKotlinTypeString()}"
    }
    val registerTypes = ""
    val actionsStr = "override suspend fun actions(tm: TermManager): Set<TSAction> = setOf(\n" +
        transitions.joinToString(",\n") { it.kotlinActionString(stateVarTypes).prependIndent() } +
        "\n)"
    val transitStr = "override suspend fun transit(act: ConcreteAction) {" +
        "\nreturn when (act.symAction.name) {".prependIndent() +
        transitions.joinToString("") {
            "\n\"${it.action.name}\" -> {" + "\n${it.kotlinTransitString(stateVarTypes)}".prependIndent() + "\n}"
        }.prependIndent().prependIndent() +
        "\nelse -> throw RuntimeException(\"Action is outside my alphabet: \${act.symAction}\")".prependIndent().prependIndent() +
        "\n}".prependIndent() +
        "\n}"
    return "class $name(" +
        "\n$stateVarsStr".prependIndent() +
        "\n) : TransitionSystem {" +
        registerTypes +
        "\n$actionsStr".prependIndent() +
        "\n$transitStr".prependIndent() +
        "\n}"
}

private fun ProcClassDecl.kotlinStaticInfoString(): String {
    val transitionInfo = transitions.joinToString(",\n") { it.kotlinStaticInfoString().prependIndent() }
    val constructorPairs = constructors
        .joinToString(",\n") { ctor ->
            val actSigStr = ctor.kotlinStaticInfoString()
            val argSymbols = actionArgSymbols(ctor.action.args)
            val stateVarTypes = stateVars.associate { Pair(it.name, it.type) }
            val symbolTypes = stateVarTypes + actionArgEnv(ctor.action.args)
            val constructorArgs = ctor.transits
                .filterIsInstance<TransitUpdate.Assign>()
                .joinToString(", ") { assign ->
                    "${transitRootVar(assign.key).toKotlinIdent()} = ${assign.expr.toTransitString(symbolTypes, argSymbols)}"
                }
            val constructor = "$name($constructorArgs)"
            // Error checks run before transits and effects so they see pre-state variables
            // and no effect happens upon an error.
            val errorStr = ctor.kotlinErrorString(stateVarTypes)
            val effectStr = ctor.kotlinEffectString(stateVarTypes, "result.")
            val constructStr = when {
                errorStr.isEmpty() && effectStr.isEmpty() ->
                    "{ _,act -> $constructor }"
                errorStr.isEmpty() ->
                    "{ _,act ->\nval result = $constructor\n$effectStr\nresult\n}"
                effectStr.isEmpty() ->
                    "{ _,act ->\n$errorStr\n$constructor\n}"
                else ->
                    "{ _,act ->\n$errorStr\nval result = $constructor\n$effectStr\nresult\n}"
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
            "\n))").prependIndent()
}

private fun ActionDecl.kotlinActionString(stateVarTypes: Map<String, Type>): String {
    val symbolTypes = stateVarTypes + actionArgEnv(action.args)
    val argSymbols = actionArgSymbols(action.args)

    val actionArgsStr = action.args.joinToString(", ") {
        "Variable(\"${it.name}\", ${it.type.toCodegenTypeVal()})"
    }
    val syncTypeStr = when (action.syncType) {
        SymbolicAction.SyncType.CSP -> "SymbolicAction.SyncType.CSP"
        SymbolicAction.SyncType.P2P -> "SymbolicAction.SyncType.P2P"
    }
    val actionSigStr = "SymbolicAction(\"${action.name}\", listOf($actionArgsStr), $syncTypeStr)"
    val guardStr = when {
        guards.isEmpty() -> "tm.mkTrue()"
        guards.size == 1 -> guards[0].toSmtGuardString(symbolTypes, argSymbols)
        else -> {
            val body = guards.joinToString(", ") { it.toSmtGuardString(symbolTypes, argSymbols) }
            "tm.mkTerm(Kind.AND, $body)"
        }
    }
    val syncRoleStr = when (modifier) {
        TSAction.SyncRole.CSP -> "TSAction.SyncRole.CSP"
        TSAction.SyncRole.P2PService -> "TSAction.SyncRole.P2PService"
        TSAction.SyncRole.P2PConsumer -> "TSAction.SyncRole.P2PConsumer"
    }
    return "TSAction(" +
        "\n$actionSigStr,".prependIndent() +
        "\n$guardStr,".prependIndent() +
        "\n$syncRoleStr".prependIndent() +
        "\n)"
}

private fun ActionDecl.kotlinTransitString(stateVarTypes: Map<String, Type>): String {
    val symbolTypes = stateVarTypes + actionArgEnv(action.args)
    val argSymbols = actionArgSymbols(action.args)
    // Error checks run before transits and effects so they see pre-state variables
    // and no effect happens upon an error.
    val errorStr = kotlinErrorString(stateVarTypes)
    val transitLines = transits.map { update ->
        when (update) {
            is TransitUpdate.Assign -> {
                val rootVar = transitRootVar(update.key)
                val rootType = stateVarTypes.getValue(rootVar)
                val fieldPath = if (update.key.contains('.')) update.key.substringAfter('.').split('.') else emptyList()
                val rhs = update.expr.toTransitString(symbolTypes, argSymbols)
                copyAssignmentString(rootVar, rootType, fieldPath, rhs)
            }
            is TransitUpdate.MapPut -> {
                val mapVar = update.mapVar.toKotlinIdent()
                val keyStr = update.key.toTransitString(symbolTypes, argSymbols)
                val valStr = update.value.toTransitString(symbolTypes, argSymbols)
                "$mapVar = $mapVar + ($keyStr to $valStr)"
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
        .plus(transitLines)
        .plus(effectLines)
        .joinToString("\n")
}

private fun ActionDecl.kotlinStaticInfoString(): String {
    val actionArgsStr = action.args.joinToString(", ") {
        "Variable(\"${it.name}\", ${it.type.toCodegenTypeVal()})"
    }
    val syncTypeStr = when (action.syncType) {
        SymbolicAction.SyncType.CSP -> "SymbolicAction.SyncType.CSP"
        SymbolicAction.SyncType.P2P -> "SymbolicAction.SyncType.P2P"
    }
    return "SymbolicAction(\"${action.name}\", listOf($actionArgsStr), $syncTypeStr)"
}
