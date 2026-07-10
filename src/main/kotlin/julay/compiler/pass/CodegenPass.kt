package julay.compiler.pass

import julay.compiler.*
import julay.compiler.ast.*
import julay.compiler.decl.*
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
    val runProgram = "Program(tsInfo).run()"
    val mainFunction = "suspend fun main(args : Array<String>) {" +
        "\n$staticInfo".prependIndent() +
        "\n$runProgram".prependIndent() +
        "\n}"

    val imports = "import com.microsoft.z3.*\n" +
        "import julay.program.*\n" +
        "import julay.program.library.*\n" +
        "import julay.tools.mkStringConst\n"
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
    val toZ3Fun = objClassToZ3FunName(name)
    val fromZ3Fun = objClassFromZ3FunName(name)
    return """
        |val $typeVal = ObjClassType(
        |    "$name",
        |    listOf($fieldsStr),
        |    { value, ctx -> $toZ3Fun(ctx, value.value as $name) },
        |    { expr -> $fromZ3Fun(expr) },
        |)
    """.trimMargin()
}

private fun ObjClassDecl.kotlinConversionHelpersString(): String {
    val typeVal = objClassTypeValName(name)
    val toZ3Fun = objClassToZ3FunName(name)
    val fromZ3Fun = objClassFromZ3FunName(name)
    val toZ3Args = fields.joinToString(", ") { field ->
        fieldToZ3ExprString("value.${field.name}", field.type)
    }
    val fromZ3Args = fields.mapIndexed { index, field ->
        fieldFromZ3ExprString("fieldExprs[$index]", field.type)
    }.joinToString(",\n")
    return """
        |fun $toZ3Fun(ctx: Context, value: $name): Expr<*> =
        |    $typeVal.mkConstructorZ3(ctx, $toZ3Args)
        |
        |fun $fromZ3Fun(expr: Expr<*>): $name {
        |    val fieldExprs = $typeVal.fieldExprsFromZ3(expr)
        |    return $name(
        |${fromZ3Args.prependIndent("        ")}
        |    )
        |}
    """.trimMargin()
}

private fun fieldToZ3ExprString(valueExpr: String, type: Type): String = when (type) {
    is BoolType -> "ctx.mkBool($valueExpr)"
    is IntType -> "ctx.mkInt($valueExpr)"
    is StringType -> "ctx.mkString($valueExpr)"
    is ObjClassType -> "${objClassToZ3FunName(type.name)}(ctx, $valueExpr)"
    else -> throw RuntimeException("Invalid field type for Z3 conversion: $type")
}

private fun fieldFromZ3ExprString(exprStr: String, type: Type): String = when (type) {
    is BoolType -> "boolType.fromZ3Expr($exprStr) as Boolean"
    is IntType -> "intType.fromZ3Expr($exprStr) as Int"
    is StringType -> "stringType.fromZ3Expr($exprStr) as String"
    is ObjClassType -> "${objClassFromZ3FunName(type.name)}($exprStr)"
    else -> throw RuntimeException("Invalid field type for Z3 conversion: $type")
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

private fun ProcClassDecl.kotlinClassString(objClassDecls: List<ObjClassDecl>): String {
    val stateVarTypes = stateVars.associate { Pair(it.name, it.type) }
    val stateVarsStr = stateVars.joinToString(",\n") {
        "private var ${it.name.toKotlinIdent()}: ${it.type.toKotlinTypeString()}"
    }
    val registerTypes = ""
    val actionsStr = "override suspend fun actions(): Set<TSAction> = setOf(\n" +
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
        "\nprivate val ctx = Context()".prependIndent() +
        registerTypes +
        "\n$actionsStr".prependIndent() +
        "\n$transitStr".prependIndent() +
        "\noverride fun getContext() = ctx".prependIndent() +
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
            val constructorArgs = ctor.transits.entries
                .joinToString(", ") { (k, v) ->
                    "${transitRootVar(k).toKotlinIdent()} = ${v.toTransitString(symbolTypes, argSymbols)}"
                }
            val constructor = "$name($constructorArgs)"
            val effectStr = ctor.kotlinEffectString(stateVarTypes, "result.")
            val constructStr = if (effectStr.isEmpty()) {
                "{ _,act -> $constructor }"
            } else {
                "{ _,act ->\nval result = $constructor\n$effectStr\nresult\n}"
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
    val guardStr = if (guards.size == 1) {
        val guard = guards[0]
        guard.toZ3GuardString(symbolTypes, argSymbols)
    } else {
        val body = guards.joinToString(", ") { it.toZ3GuardString(symbolTypes, argSymbols) }
        "ctx.mkAnd($body)"
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
    val transitLines = transits.map { (k, e) ->
        val rootVar = transitRootVar(k)
        val rootType = stateVarTypes.getValue(rootVar)
        val fieldPath = if (k.contains('.')) k.substringAfter('.').split('.') else emptyList()
        val rhs = e.toTransitString(symbolTypes, argSymbols)
        copyAssignmentString(rootVar, rootType, fieldPath, rhs)
    }
    val effectLines = effects.map {
        EffectBuiltinRegistry.effectStmtKotlinString(it, symbolTypes, argSymbols)
    }
    return (transitLines + effectLines).joinToString("\n")
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
