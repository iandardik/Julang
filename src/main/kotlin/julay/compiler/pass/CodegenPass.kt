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
    val procsToCompile = program.allProcNames(procDecls).filter { it !in libPClassNames }
    val procClasses = procsToCompile.flatMap { proc ->
        val procClass = ast.procClassPass(setOf(proc))
        julay.tools.assert(procClass.size == 1, "Expected exactly one proc class for \"$proc\" but found: ${procClass.size}")
        procClass
    }

    val libProcs = program.allProcNames(procDecls).filter { it in libPClassNames }
    val staticInfoLib = libProcs.map { LibraryRegistry.staticInfoCodegenExpr(it) }
    val staticInfoCompiledProcs = procClasses.map { it.kotlinStaticInfoString() }
    val staticInfoBody = (staticInfoCompiledProcs + staticInfoLib).joinToString(",\n") { it }
    val staticInfo = "val tsInfo = setOf(\n" + staticInfoBody.prependIndent() + "\n)"
    val runProgram = "Program(tsInfo).run()"
    val mainFunction = "suspend fun main(args : Array<String>) {" +
        "\n$staticInfo".prependIndent() +
        "\n$runProgram".prependIndent() +
        "\n}"

    val imports = "import com.microsoft.z3.*\n" +
        "import julay.compiler.ObjClassType\n" +
        "import julay.program.*\n" +
        "import julay.program.library.*\n" +
        "import julay.tools.mkStringConst\n"
    val objClassDecls = ast.resolvedObjClassDecls()
    val objClassCode = objClassDecls.joinToString("\n\n") { it.kotlinTypeValString() }
    val objClassSection = if (objClassCode.isEmpty()) "" else "$objClassCode\n\n"
    val mainClassName = program.name.replaceFirstChar { it.uppercase() }
    val sourceText = "$imports\n" +
        objClassSection +
        procClasses.joinToString("\n\n") { it.kotlinClassString() } +
        "\n\n" +
        mainFunction

    return CodegenResult(sourceText, mainClassName)
}

private fun ObjClassDecl.kotlinTypeValString(): String {
    val fieldsStr = fields.joinToString(", ") {
        "Variable(\"${it.name}\", ${it.type.toCodegenTypeVal()})"
    }
    return "val ${objClassTypeValName(name)} = ObjClassType(\"$name\", listOf($fieldsStr))"
}

private fun ProcClassDecl.kotlinClassString(): String {
    val stateVarTypes = stateVars.associate { Pair(it.name, it.type) }
    val stateVarsStr = stateVars.joinToString(",\n") {
        "private var ${it.name.toKotlinIdent()}: ${it.type.toKotlinTypeString()}"
    }
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
            val argSymbols = flattenedArgSymbols(ctor.action.args)
            val stateVarTypes = stateVars.associate { Pair(it.name, it.type) }
            val symbolTypes = stateVarTypes + flattenActionArgEnv(ctor.action.args)
            val constructorArgs = ctor.transits.entries
                .joinToString(", ") { (k, v) ->
                    "${k.toKotlinIdent()} = ${v.toTransitString(symbolTypes, argSymbols)}"
                }
            val constructor = "$name($constructorArgs)"
            val constructStr = "{ _,act -> $constructor }"
            "Pair($actSigStr, $constructStr)".prependIndent()
        }
    return "TransitionSystemStaticInfo(" +
        ("\n\"$name\"," +
            "\nsetOf(" +
            "\n$transitionInfo" +
            "\n)," +
            "\nmapOf(" +
            "\n$constructorPairs" +
            "\n))").prependIndent()
}

private fun ActionDecl.kotlinActionString(stateVarTypes: Map<String, Type>): String {
    val symbolTypes = stateVarTypes + flattenActionArgEnv(action.args)
    val argSymbols = flattenedArgSymbols(action.args)

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
    val symbolTypes = stateVarTypes + flattenActionArgEnv(action.args)
    val argSymbols = flattenedArgSymbols(action.args)
    return transits.map { (v, e) ->
        "${v.toKotlinIdent()} = ${e.toTransitString(symbolTypes, argSymbols)}"
    }.joinToString("\n")
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
