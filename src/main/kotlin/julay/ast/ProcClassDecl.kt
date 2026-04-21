package julay.ast

import julay.program.*

data class ProcClassDecl(
    val name : String,
    val stateVars : List<Variable>,
    val constructors : List<ActionDecl>,
    val transitions : List<ActionDecl>,
) {

    fun toKotlinClassString(): String {
        val stateVarTypes = stateVars.associate { Pair(it.name,it.type) }
        val stateVarsStr = stateVars.joinToString(",\n") { "private var $it" }
        val actionsStr = "override suspend fun actions(): Set<TSAction> = setOf(\n" +
                transitions.joinToString(",\n") { it.toActionString(stateVarTypes).prependIndent() } +
                "\n)"
        val transitStr = "override suspend fun transit(act: ConcreteAction) {" +
                "\nreturn when (act.symAction.name) {".prependIndent() +
                transitions.joinToString("") {
                    "\n\"${it.action.name}\" -> {" + "\n${it.toTransitString(stateVarTypes)}".prependIndent() + "\n}"
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

    fun toKotlinStaticInfoString(): String {
        val transitionInfo = transitions.joinToString(",\n") { it.toStaticInfoString().prependIndent() }
        val constructorPairs = constructors
            .joinToString(",\n") { ctor ->
                val actSigStr = ctor.toStaticInfoString()
                val constructorArgs = ctor.transits.values
                    .map { v ->
                        val argTypes = ctor.action.args.associate { Pair(it.name,it.type) }
                        val argSymbols = ctor.action.args.map { it.name }.toSet()
                        v.toTransitString(argTypes, argSymbols)
                    }
                    .joinToString(", ") { it }
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
                "\n)," +
                "\ntrue)").prependIndent()
    }
}

data class ActionDecl(
    val action : SymbolicAction,
    val guards : List<ASTNode>,
    val transits : Map<String,ASTNode>,
    val modifier: TSAction.SyncRole,
) {
    fun toActionString(stateVarTypes : Map<String,Type>) : String {
        val argTypes = action.args.associate { Pair(it.name,it.type) }
        val symbolTypes = stateVarTypes + argTypes // action args are more tightly scoped than state vars
        val argSymbols = action.args.map { it.name }.toSet()

        val actionArgsStr = action.args.joinToString(", ") {
            val typeStr = when (it.type) {
                boolType -> "boolType"
                intType -> "intType"
                stringType -> "stringType"
                else -> throw RuntimeException("Invalid type: ${it.type}")
            }
            "Variable(\"${it.name}\", $typeStr)"
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

    fun toTransitString(stateVarTypes : Map<String,Type>) : String {
        val argTypes = action.args.associate { Pair(it.name,it.type) }
        val symbolTypes = stateVarTypes + argTypes // action args are more tightly scoped than state vars
        val argSymbols = action.args.map { it.name }.toSet()
        return transits.map { (v,e) -> "$v = ${e.toTransitString(symbolTypes,argSymbols)}" }.joinToString("\n")
    }

    fun toStaticInfoString(): String {
        val actionArgsStr = action.args.joinToString(", ") {
            val typeStr = when (it.type) {
                boolType -> "boolType"
                intType -> "intType"
                stringType -> "stringType"
                else -> throw RuntimeException("Invalid type: ${it.type}")
            }
            "Variable(\"${it.name}\", $typeStr)"
        }
        val syncTypeStr = when (action.syncType) {
            SymbolicAction.SyncType.CSP -> "SymbolicAction.SyncType.CSP"
            SymbolicAction.SyncType.P2P -> "SymbolicAction.SyncType.P2P"
        }
        return "SymbolicAction(\"${action.name}\", listOf($actionArgsStr), $syncTypeStr)"
    }
}
