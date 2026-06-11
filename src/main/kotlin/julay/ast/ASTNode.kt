package julay.ast

import julay.program.*
import julay.program.TSAction
import julay.program.library.LibraryRegistry

abstract class ASTNode(
    val children : List<ASTNode>
) {
    abstract fun programLocation() : ProgramLoc
    open fun procPass() : List<ProcDecl> = children.flatMap { it.procPass() }
    open fun errorPass(procs : Set<String>) : List<CompileError> = children.flatMap { it.errorPass(procs) }
    open fun procClassPass(procs : Set<String>) : List<ProcClassDecl> = children.flatMap { it.procClassPass(procs) }
    open fun objClassPass() : List<RawObjClassDecl> = children.flatMap { it.objClassPass() }
    open fun typePass(symbolEnv : Map<String, Type> = emptyMap(), registry : ObjClassRegistry = ObjClassRegistry.EMPTY) : List<CompileError> =
        children.flatMap { it.typePass(symbolEnv, registry) }
}

abstract class DeclNode(children : List<ASTNode>) : ASTNode(children) {
    abstract fun name() : String
}

abstract class ProcClassDeclNode(children : List<ASTNode>) : ASTNode(children) {
    open fun stateVariables() : List<Variable> = listOf()
    open fun constructors() : List<ActionDecl> = listOf()
    open fun transitions() : List<ActionDecl> = listOf()
    open fun transitVars() : List<Pair<String, ProgramLoc>> = listOf()
}

open class ArgsNode(
    private val args : List<ArgsNode>,
    private val loc : ProgramLoc
) : ASTNode(args) {
    override fun programLocation() = loc
    open fun actionArgs() : List<Variable> = args.flatMap { it.actionArgs() }
    fun argsTypeMap() : Map<String, Type> = actionArgs().associate { it.name to it.type }
    override fun toString(): String {
        return children.joinToString(", ") { it.toString() }
    }
}

abstract class ActionBodyNode(
    private val body : List<ActionBodyNode>,
    exprs : List<ExprNode>
) : ASTNode(body + exprs) {
    open fun guards() : List<ExprNode> = body.flatMap { it.guards() }
    open fun transits() : Map<String,ExprNode> = body.fold(emptyMap()) { acc, astNode -> acc + astNode.transits() }
    open fun transitVars() : List<Pair<String, ProgramLoc>> = body.flatMap { it.transitVars() }
}

sealed interface TypePassType {
    data object Uninferred : TypePassType
    data class Inferred(val type : Type) : TypePassType
}

abstract class ExprNode(children : List<ASTNode>) : ASTNode(children) {
    private var myType : TypePassType = TypePassType.Uninferred
    override fun typePass(symbolEnv : Map<String, Type>, registry: ObjClassRegistry) : List<CompileError> {
        val childrenErrors = children.flatMap { it.typePass(symbolEnv, registry) }
        myType = TypePassType.Inferred(inferType(symbolEnv))
        return childrenErrors
    }
    fun getType() : Type = when (val ts = myType) {
        is TypePassType.Inferred -> ts.type
        is TypePassType.Uninferred ->
            throw RuntimeException("Type not inferred for expression at ${programLocation()}")
    }
    protected abstract fun inferType(symbolEnv : Map<String, Type>) : Type
    abstract fun toZ3GuardString(symbolTypes : Map<String,Type>, argSymbols : Set<String>, forceString : Boolean = false) : String
    abstract fun toTransitString(symbolTypes : Map<String,Type>, argSymbols : Set<String>) : String
}

class RootNode(
    private val declNodes : List<DeclNode>,
    private val loc : ProgramLoc
) : ASTNode(declNodes) {
    override fun programLocation(): ProgramLoc = loc
    override fun errorPass(procs : Set<String>): List<CompileError> {
        return super.errorPass(procs) + actionConsistencyErrors(procs) + overlappingDeclNamesErrors()
    }
    fun actionConsistencyErrors(procs : Set<String>) : List<CompileError> {
        val progTransitions = declNodes
            .flatMap { it.procClassPass(procs) }
            .flatMap { it.transitions }
        val libTransitions = procPass()
            .flatMap { it.allProcNames(procPass()) }
            .filter { it in procs && LibraryRegistry.isLibrary(it) }
            .flatMap { LibraryRegistry.actionDecls(it) }
        val allTransitions = progTransitions + libTransitions
        val actionOccurrences = allTransitions.groupBy { it.action.name }
        return actionOccurrences.entries.flatMap { (name, actions) ->
            val refAction = actions[0]
            val argMismatches = actions.flatMap { act ->
                assertOrCompileError(refAction.action.args == act.action.args,
                    TwoLocsCompileError(refAction.loc, act.loc, "Expected action \"$name\" to have the same arguments"))
            }
            val inconsistentSyncTypes = actions.flatMap { act ->
                assertOrCompileError(refAction.action.syncType == act.action.syncType,
                    TwoLocsCompileError(refAction.loc, act.loc, "Expected action \"$name\" to have the same modifiers"))
            }
            val p2pMissingASide = actions.let { actions ->
                val isP2P = actions.any { act -> act.action.syncType == SymbolicAction.SyncType.P2P }
                val hasService = actions.any { act -> act.modifier == TSAction.SyncRole.P2PService }
                val hasConsumer = actions.any { act -> act.modifier == TSAction.SyncRole.P2PConsumer }
                val missingType = if (hasService) "p2p-consumer" else "p2p-service"
                assertOrCompileError(!isP2P || (hasService && hasConsumer),
                    OneLocCompileError(refAction.loc, "Expected action \"$name\" to have at least one corresponding \"$missingType\" action"))
            }
            argMismatches + inconsistentSyncTypes + p2pMissingASide
        }
    }
    fun overlappingDeclNamesErrors() : List<CompileError> {
        // an n^2 algorithm isn't super efficient, but the number of decls won't be large and we do need to detect both
        // locations where the name conflict occur so we can report it
        return declNodes.flatMap { refDecl ->
            declNodes
                .filter { decl -> refDecl != decl && refDecl.name() == decl.name() }
                .map { decl -> TwoLocsCompileError(refDecl.programLocation(), decl.programLocation(),
                    "Expected each declaration to have a unique name, but found at least two named \"${decl.name()}\"") }
        }
    }
    override fun typePass(symbolEnv: Map<String, Type>, registry: ObjClassRegistry): List<CompileError> {
        val built = ObjClassRegistry.build(declNodes.flatMap { it.objClassPass() })
        return built.errors + declNodes.flatMap { it.typePass(symbolEnv, built) }
    }

    fun resolvedObjClassDecls(): List<ObjClassDecl> = resolvedObjClassRegistry().decls

    fun resolvedObjClassRegistry(): ObjClassRegistry = ObjClassRegistry.build(declNodes.flatMap { it.objClassPass() })

    fun withDeclNodes(decls: List<DeclNode>): RootNode = RootNode(decls, programLocation())

    fun flattenObjClassPass(registry: ObjClassRegistry): RootNode = flattenObjClassPass(this, registry)

    override fun toString(): String {
        return declNodes.joinToString("\n\n") { it.toString() }
    }
}

class ProcClassNode(
    private val name : String,
    private val localDecls : List<ProcClassDeclNode>,
    private val loc : ProgramLoc
) : DeclNode(localDecls) {
    override fun programLocation() = loc
    override fun name() = name
    override fun procClassPass(procs : Set<String>): List<ProcClassDecl> {
        if (name !in procs) {
            return listOf()
        }
        val stateVars = localDecls.flatMap { it.stateVariables() }
        val constructors = localDecls.flatMap { it.constructors() }
        val transitions = localDecls.flatMap { it.transitions() }
        val decl = ProcClassDecl(name, stateVars, constructors, transitions)
        return listOf(decl)
    }

    internal fun localDecls(): List<ProcClassDeclNode> = localDecls

    internal fun withLocalDecls(decls: List<ProcClassDeclNode>): ProcClassNode =
        ProcClassNode(name, decls, programLocation())

    override fun errorPass(procs: Set<String>): List<CompileError> {
        // no repeat state var names
        val repeatStateVarNameErrors = localDecls
            .filterIsInstance<VarNode>()
            .groupBy { it.name }
            .flatMap { (_, nodes) ->
                if (nodes.size == 1) emptyList()
                // we have an error (repeat state var names) if there are more than one VarNode's with the same name
                else listOf(
                    TwoLocsCompileError(
                        nodes[0].programLocation(),
                        nodes[1].programLocation(),
                        "Expected state variables to have unique names"
                    )
                )
            }
        // ensure that each constructor assigns a value to every state var exactly once
        val stateVars = localDecls
            .flatMap { it.stateVariables() }
            .map { it.name }
        val ctorsCompleteAssgnErrors = localDecls
            .filterIsInstance<ConstructorNode>()
            .flatMap { ctorNode ->
                val stateVarSet = stateVars.toSet()
                val transitVarSet = ctorNode.transitVars().map { it.first }.toSet()
                val missingStateVars = stateVarSet.minus(transitVarSet)
                assertOrCompileError(missingStateVars.isEmpty(), OneLocCompileError(ctorNode.programLocation(),
                        "Expected each constructor to assign a value to every state variable; missing assignments to $missingStateVars"))
            }
        // make sure there is at least one constructor
        val atLeastOneConstructorErrors = assertOrCompileError(localDecls.flatMap { it.constructors() }.isNotEmpty(),
            OneLocCompileError(loc, "Expected \"$name\" to have at least one constructor"))
        // constructors actions cannot intersect with transition actions
        val constructorActions = localDecls.flatMap { it.constructors() }
        val transitionActions = localDecls.flatMap { it.transitions() }
        val ctorTransActionNotMutexErrors = constructorActions.flatMap { ctorAct ->
            transitionActions.flatMap { transAct ->
                assertOrCompileError(ctorAct.action.name != transAct.action.name,
                    TwoLocsCompileError(ctorAct.loc, transAct.loc,
                        "Expected constructor names to not overlap with transition names, but found at least one overlap for the action \"${ctorAct.action.name}\""))
            }
        }
        return super.errorPass(procs) + repeatStateVarNameErrors + ctorsCompleteAssgnErrors +
                atLeastOneConstructorErrors + ctorTransActionNotMutexErrors
    }
    override fun typePass(symbolEnv : Map<String, Type>, registry: ObjClassRegistry) : List<CompileError> {
        val varErrors = localDecls
            .filterIsInstance<VarNode>()
            .flatMap { it.typePass(symbolEnv, registry) }
        val localSymbolEnv = localDecls
            .filterIsInstance<VarNode>()
            .associate { it.name to it.type }
        return varErrors + localDecls.flatMap { decl ->
            if (decl is VarNode) emptyList() else decl.typePass(localSymbolEnv, registry)
        }
    }
    override fun toString(): String {
        val body = localDecls.joinToString("\n") { "$it".prependIndent() }
        return "p-class $name {\n$body\n}"
    }
}

class ProcNode(
    private val name : String,
    private val value : ASTNode,
    private val loc : ProgramLoc
) : DeclNode(listOf(value)) {
    override fun programLocation() = loc
    override fun name() = name
    override fun procPass(): List<ProcDecl> {
        return listOf(ProcDecl(name, value.procPass(), ProcDeclType.Proc))
    }
    override fun toString(): String {
        return "proc $name := $value"
    }
}

class ProgramNode(
    private val name : String,
    private val value : ASTNode,
    private val loc : ProgramLoc
) : DeclNode(listOf(value)) {
    override fun programLocation() = loc
    override fun name() = name
    override fun procPass(): List<ProcDecl> {
        return listOf(ProcDecl(name, value.procPass(), ProcDeclType.Program))
    }
    override fun toString(): String {
        return "program $name := $value"
    }
}

class SpecNode(
    private val name : String,
    private val value : ASTNode,
    private val loc : ProgramLoc
) : DeclNode(listOf(value)) {
    override fun programLocation() = loc
    override fun name() = name
    override fun procPass(): List<ProcDecl> {
        return listOf(ProcDecl(name, value.procPass(), ProcDeclType.Spec))
    }
    override fun toString(): String {
        return "spec $name := $value"
    }
}

class ObjClassNode(
    private val name: String,
    val fields: List<FieldNode>,
    private val loc: ProgramLoc,
) : DeclNode(fields) {
    override fun programLocation() = loc
    override fun name() = name
    override fun objClassPass(): List<RawObjClassDecl> = listOf(
        RawObjClassDecl(name, fields.map { it.fieldName to it.typeName }, loc)
    )
    override fun errorPass(procs: Set<String>): List<CompileError> {
        val repeatFieldErrors = fields
            .groupBy { it.fieldName }
            .flatMap { (_, nodes) ->
                if (nodes.size == 1) emptyList()
                else listOf(
                    TwoLocsCompileError(
                        nodes[0].programLocation(),
                        nodes[1].programLocation(),
                        "Expected o-class fields to have unique names",
                    )
                )
            }
        return super.errorPass(procs) + repeatFieldErrors
    }
    override fun toString(): String {
        val body = fields.joinToString("\n") { "$it".prependIndent() }
        return "o-class $name {\n$body\n}"
    }
}

class FieldNode(
    val fieldName: String,
    val typeName: String,
    private val loc: ProgramLoc,
) : ASTNode(listOf()) {
    override fun programLocation() = loc
    override fun toString(): String = "$fieldName : $typeName"
}

private sealed interface TypeNameResolution {
    data object Unresolved : TypeNameResolution
    data class Resolved(val type: Type) : TypeNameResolution
}

class VarNode(
    val name : String,
    val typeName : String,
    private val loc : ProgramLoc
) : ProcClassDeclNode(listOf()) {
    private var typeResolution : TypeNameResolution = TypeNameResolution.Unresolved
    val type : Type
        get() = when (val resolution = typeResolution) {
            is TypeNameResolution.Resolved -> resolution.type
            is TypeNameResolution.Unresolved ->
                throw RuntimeException("Type not resolved for state variable \"$name\"")
        }
    override fun programLocation() = loc
    override fun typePass(symbolEnv: Map<String, Type>, registry: ObjClassRegistry): List<CompileError> {
        return when (val result = registry.resolveTypeName(typeName)) {
            is TypeResolveResult.Found -> {
                typeResolution = TypeNameResolution.Resolved(result.type)
                emptyList()
            }
            is TypeResolveResult.NotFound ->
                listOf(OneLocCompileError(loc, "Unknown type \"$typeName\" for state variable \"$name\""))
        }
    }
    override fun stateVariables(): List<Variable> = listOf(Variable(name, type))
    companion object {
        fun primitive(name: String, type: Type, loc: ProgramLoc): VarNode {
            val node = VarNode(name, primitiveTypeName(type), loc)
            node.typeResolution = TypeNameResolution.Resolved(type)
            return node
        }

        private fun primitiveTypeName(type: Type): String = when (type) {
            is BoolType -> "Boolean"
            is IntType -> "Int"
            is StringType -> "String"
            else -> throw RuntimeException("Cannot create flattened VarNode for type $type")
        }
    }
    override fun toString(): String {
        val displayType = when (val resolution = typeResolution) {
            is TypeNameResolution.Resolved -> resolution.type
            is TypeNameResolution.Unresolved -> typeName
        }
        return "$name : $displayType"
    }
}

class ConstructorNode(
    private val name : String,
    private val args : ArgsNode,
    private val body : List<ActionBodyNode>,
    private val loc : ProgramLoc
) : ProcClassDeclNode(listOf(args) + body) {
    override fun programLocation() = loc
    override fun transitVars() = body.flatMap { it.transitVars() }
    override fun errorPass(procs : Set<String>): List<CompileError> {
        // TODO get rid of the copy pasta
        val multiVarTransitError = body
            .flatMap { it.transitVars() }
            .let { transits ->
                transits.flatMapIndexed { i, (refName,refLoc) ->
                    transits.flatMapIndexed { j, (name,loc) ->
                        assertOrCompileError(i <= j || refName != name, TwoLocsCompileError(refLoc, loc,
                            "Expected at most one assignment per variable, but found multiple assignments for \"$name\""))
                    }
                }
            }
        // ensure that there is no guard, since it will not be followed by the ConstructorTS
        val noGuardErrors = assertOrCompileError(body.flatMap { it.guards() }.isEmpty(),
            OneLocCompileError(loc, "Expected constructors not to have guards"))
        return super.errorPass(procs) + multiVarTransitError + noGuardErrors
    }
    override fun typePass(symbolEnv : Map<String, Type>, registry: ObjClassRegistry) : List<CompileError> {
        val argErrors = args.typePass(symbolEnv, registry)
        val actionEnv = symbolEnv + args.argsTypeMap() + flattenActionArgEnv(args.actionArgs())
        return argErrors + body.flatMap { it.typePass(actionEnv, registry) }
    }
    override fun constructors(): List<ActionDecl> {
        return listOf(
            ActionDecl(
                SymbolicAction(name, args.actionArgs(), SymbolicAction.SyncType.CSP),
                body.flatMap { it.guards() },
                body.fold(emptyMap()) { acc, astNode -> acc + astNode.transits() },
                TSAction.SyncRole.CSP,
                loc
            )
        )
    }
    internal fun body(): List<ActionBodyNode> = body
    internal fun actionArgs(): List<Variable> = args.actionArgs()
    internal fun withBody(newBody: List<ActionBodyNode>): ConstructorNode =
        ConstructorNode(name, args, newBody, programLocation())
    override fun toString(): String {
        val bodyStr = body.joinToString("\n") { "$it".prependIndent() }
        return "constructor $name($args) {\n$bodyStr\n}"
    }
}

class TransitionNode(
    private val modifier : TSAction.SyncRole,
    private val name : String,
    private val args : ArgsNode,
    private val body : List<ActionBodyNode>,
    private val loc : ProgramLoc
) : ProcClassDeclNode(listOf(args) + body) {
    override fun programLocation() = loc
    override fun transitVars() = body.flatMap { it.transitVars() }
    override fun errorPass(procs : Set<String>): List<CompileError> {
        val initiallyActionErrors = assertOrCompileError(name != "initially", OneLocCompileError(loc,
            "only constructors (not transitions) can synchronize on the 'initially' action"))
        // ensure that each transit has a unique state var
        // TODO get rid of the copy pasta
        val multiVarTransitError = body
            .flatMap { it.transitVars() }
            .let { transits ->
                transits.flatMapIndexed { i, (refName,refLoc) ->
                    transits.flatMapIndexed { j, (name,loc) ->
                        assertOrCompileError(i <= j || refName != name, TwoLocsCompileError(refLoc, loc,
                            "Expected at most one assignment per variable, but found multiple assignments for \"$name\""))
                    }
                }
            }
        return super.errorPass(procs) + initiallyActionErrors + multiVarTransitError
    }
    override fun typePass(symbolEnv : Map<String, Type>, registry: ObjClassRegistry) : List<CompileError> {
        val argErrors = args.typePass(symbolEnv, registry)
        val actionEnv = symbolEnv + args.argsTypeMap() + flattenActionArgEnv(args.actionArgs())
        return argErrors + body.flatMap { it.typePass(actionEnv, registry) }
    }
    override fun transitions(): List<ActionDecl> {
        val syncType = when (modifier) {
            TSAction.SyncRole.CSP -> SymbolicAction.SyncType.CSP
            TSAction.SyncRole.P2PService -> SymbolicAction.SyncType.P2P
            TSAction.SyncRole.P2PConsumer -> SymbolicAction.SyncType.P2P
        }
        return listOf(
            ActionDecl(
                SymbolicAction(name, args.actionArgs(), syncType),
                body.flatMap { it.guards() },
                body.fold(emptyMap()) { acc, astNode -> acc + astNode.transits() },
                modifier,
                loc
            )
        )
    }
    internal fun body(): List<ActionBodyNode> = body
    internal fun actionArgs(): List<Variable> = args.actionArgs()
    internal fun withBody(newBody: List<ActionBodyNode>): TransitionNode =
        TransitionNode(modifier, name, args, newBody, programLocation())
    override fun toString(): String {
        val modifierStr = when (modifier) {
            TSAction.SyncRole.CSP -> ""
            TSAction.SyncRole.P2PService -> "p2p-service "
            TSAction.SyncRole.P2PConsumer -> "p2p-consumer "
        }
        val bodyStr = body.joinToString("\n") { "$it".prependIndent() }
        return "${modifierStr}transition $name($args) {\n$bodyStr\n}"
    }
}

class ArgNode(
    private val name : String,
    private val typeName : String,
    private val loc : ProgramLoc
) : ArgsNode(listOf(), loc) {
    private var typeResolution : TypeNameResolution = TypeNameResolution.Unresolved
    val type : Type
        get() = when (val resolution = typeResolution) {
            is TypeNameResolution.Resolved -> resolution.type
            is TypeNameResolution.Unresolved ->
                throw RuntimeException("Type not resolved for action argument \"$name\"")
        }
    override fun programLocation() = loc
    override fun typePass(symbolEnv: Map<String, Type>, registry: ObjClassRegistry): List<CompileError> {
        return when (val result = registry.resolveTypeName(typeName)) {
            is TypeResolveResult.Found -> {
                typeResolution = TypeNameResolution.Resolved(result.type)
                emptyList()
            }
            is TypeResolveResult.NotFound ->
                listOf(OneLocCompileError(loc, "Unknown type \"$typeName\" for action argument \"$name\""))
        }
    }
    override fun actionArgs(): List<Variable> {
        return listOf(Variable(name, type))
    }
    override fun toString(): String {
        val displayType = when (val resolution = typeResolution) {
            is TypeNameResolution.Resolved -> resolution.type
            is TypeNameResolution.Unresolved -> typeName
        }
        return "$name : $displayType"
    }
}

class GuardNode(
    val expr : ExprNode,
    private val loc : ProgramLoc
) : ActionBodyNode(listOf(), listOf(expr)) {
    override fun programLocation() = loc
    override fun typePass(symbolEnv : Map<String, Type>, registry: ObjClassRegistry) : List<CompileError> {
        val childrenErrors = super.typePass(symbolEnv, registry)
        val guardTypeErrors = assertOrCompileError(
            expr.getType() is BoolType,
            OneLocCompileError(loc, "Expected guards to be Boolean-valued expressions")
        )
        return childrenErrors + guardTypeErrors
    }
    override fun guards(): List<ExprNode> {
        return listOf(expr)
    }
    override fun toString(): String {
        val exprStr = "$expr".prependIndent()
        return "guard:\n$exprStr"
    }
}

class TransitNode(
    private val transits : List<ActionBodyNode>,
    private val loc : ProgramLoc
) : ActionBodyNode(transits, listOf()) {
    override fun programLocation() = loc
    internal fun transitBodies(): List<ActionBodyNode> = transits
    override fun toString(): String {
        return "transit:\n${transits.joinToString("\n") { "$it".prependIndent() }}"
    }
}

class ErrorNode(
    private val expr : ExprNode,
    private val loc : ProgramLoc
) : ActionBodyNode(listOf(), listOf(expr)) {
    override fun programLocation() = loc
    override fun toString(): String {
        val exprStr = "$expr".prependIndent()
        return "error:\n$exprStr"
    }
}

class VarTransitNode(
    val varName : String,
    val fieldPath : List<String> = emptyList(),
    val expr : ExprNode,
    private val loc : ProgramLoc
) : ActionBodyNode(listOf(), listOf(expr)) {
    override fun programLocation() = loc

    private fun transitKey(): String =
        if (fieldPath.isEmpty()) varName else "$varName.${fieldPath.joinToString(".")}"

    override fun transitVars() = listOf(Pair(transitKey(), loc))
        override fun transits(): Map<String, ExprNode> {
        return mapOf(Pair(transitKey(), expr))
    }
    override fun typePass(symbolEnv : Map<String, Type>, registry: ObjClassRegistry) : List<CompileError> {
        val childrenErrors = super.typePass(symbolEnv, registry)
        if (childrenErrors.isNotEmpty()) {
            return childrenErrors
        }
        val varType = symbolEnv[varName]
        val varErrors = if (varType == null) {
            assertOrCompileError(
                false,
                OneLocCompileError(loc, "Unknown variable \"$varName\" in transit assignment"),
            )
        } else if (fieldPath.isEmpty()) {
            assertOrCompileError(
                expr.getType() == varType,
                OneLocCompileError(
                    loc,
                    "Expected assignment to \"$varName\" ($varType) but got expression of type ${expr.getType()}",
                ),
            )
        } else {
            when (val result = resolveFieldPath(varType, fieldPath)) {
                is FieldPathResult.Error -> listOf(OneLocCompileError(loc, result.message))
                is FieldPathResult.Resolved -> {
                    if (result.type is ObjClassType) {
                        assertOrCompileError(
                            false,
                            OneLocCompileError(
                                loc,
                                "Cannot assign a scalar to o-class field \"${transitKey()}\"; assign the whole value instead",
                            ),
                        )
                    } else {
                        assertOrCompileError(
                            expr.getType() == result.type,
                            OneLocCompileError(
                                loc,
                                "Expected assignment to \"${transitKey()}\" (${result.type}) but got expression of type ${expr.getType()}",
                            ),
                        )
                    }
                }
            }
        }
        return childrenErrors + varErrors
    }
    override fun toString(): String {
        return "${transitKey()} := $expr"
    }
}

class UnaryOpExprNode(
    private val op : String,
    private val operand : ExprNode,
    private val loc : ProgramLoc
) : ExprNode(listOf(operand)) {
    override fun programLocation() = loc
    internal fun op(): String = op
    internal fun operand(): ExprNode = operand
    override fun toZ3GuardString(symbolTypes : Map<String,Type>, argSymbols : Set<String>, forceString : Boolean): String {
        julay.tools.assert(!forceString, "Cannot force a unary boolean operator to a string")
        return when (op) {
            "~" -> "ctx.mkNot(${operand.toZ3GuardString(symbolTypes, argSymbols)})"
            else -> throw RuntimeException("Invalid unary op: $op")
        }
    }
    override fun toTransitString(symbolTypes: Map<String, Type>, argSymbols: Set<String>): String {
        val transitStr = operand.toTransitString(symbolTypes, argSymbols)
        return "($op $transitStr)"
    }
    override fun inferType(symbolEnv: Map<String, Type>): Type {
        return when (op) {
            "~" -> boolType
            else -> throw RuntimeException("Invalid unary op: $op")
        }
    }
    override fun toString(): String {
        return "($op $operand)"
    }
}

class BinaryOpExprNode(
    private val op : String,
    private val lhsOperand : ExprNode,
    private val rhsOperand : ExprNode,
    private val loc : ProgramLoc
) : ExprNode(listOf(lhsOperand,rhsOperand)) {
    override fun programLocation() = loc
    internal fun op(): String = op
    internal fun lhsOperand(): ExprNode = lhsOperand
    internal fun rhsOperand(): ExprNode = rhsOperand
    override fun toZ3GuardString(symbolTypes : Map<String,Type>, argSymbols : Set<String>, forceString : Boolean): String {
        val lhsType = lhsOperand.getType()
        val rhsType = rhsOperand.getType()
        val isStringConcat = op == "+" && (lhsType is StringType || rhsType is StringType)
        julay.tools.assert(!forceString || isStringConcat, "Cannot force a binary boolean operator to a string")

        val forceStringOperands = forceString || isStringConcat
        val lhsGuardStr = lhsOperand.toZ3GuardString(symbolTypes, argSymbols, forceStringOperands)
        val rhsGuardStr = rhsOperand.toZ3GuardString(symbolTypes, argSymbols, forceStringOperands)

        return when (op) {
            "*" -> "ctx.mkMul($lhsGuardStr,$rhsGuardStr)"
            "/" -> "ctx.mkDiv($lhsGuardStr,$rhsGuardStr)"
            "%" -> "ctx.mkMod($lhsGuardStr,$rhsGuardStr)"
            "<" -> "ctx.mkLt($lhsGuardStr,$rhsGuardStr)"
            "<=" -> "ctx.mkLe($lhsGuardStr,$rhsGuardStr)"
            ">" -> "ctx.mkGt($lhsGuardStr,$rhsGuardStr)"
            ">=" -> "ctx.mkGe($lhsGuardStr,$rhsGuardStr)"
            "=" -> "ctx.mkEq($lhsGuardStr,$rhsGuardStr)"
            "#" -> "ctx.mkNot(ctx.mkEq($lhsGuardStr,$rhsGuardStr))"
            "&" -> "ctx.mkAnd($lhsGuardStr,$rhsGuardStr)"
            "|" -> "ctx.mkOr($lhsGuardStr,$rhsGuardStr)"
            "=>" -> "ctx.mkImplies($lhsGuardStr,$rhsGuardStr)"
            "+" -> {
                when {
                    lhsType is IntType && rhsType is IntType -> "ctx.mkAdd($lhsGuardStr,$rhsGuardStr)"
                    lhsType is StringType || rhsType is StringType -> "ctx.mkConcat($lhsGuardStr,$rhsGuardStr)"
                    else -> throw RuntimeException("Cannot add types: $lhsType and $rhsType")
                }
            }
            "-" -> "ctx.mkMinus($lhsGuardStr,$rhsGuardStr)"
            else -> throw RuntimeException("Invalid binary op: $op")
        }
    }
    override fun toTransitString(symbolTypes: Map<String, Type>, argSymbols: Set<String>): String {
        // for readability
        val lhs = lhsOperand.toTransitString(symbolTypes, argSymbols)
        val rhs = rhsOperand.toTransitString(symbolTypes, argSymbols)
        return when (op) {
            "=" -> "($lhs == $rhs)"
            "#" -> "($lhs != $rhs)"
            "&" -> "($lhs && $rhs)"
            "|" -> "($lhs || $rhs)"
            "=>" -> "(!($lhs) || $rhs)"
            else -> "($lhs $op $rhs)"
        }
    }
    override fun typePass(symbolEnv: Map<String, Type>, registry: ObjClassRegistry): List<CompileError> {
        val childErrors = super.typePass(symbolEnv, registry)
        if (childErrors.isNotEmpty()) {
            return childErrors
        }
        val lhsType = lhsOperand.getType()
        val rhsType = rhsOperand.getType()
        val structOpErrors = if (lhsType is ObjClassType || rhsType is ObjClassType) {
            when (op) {
                "=", "#" -> assertOrCompileError(
                    lhsType == rhsType,
                    OneLocCompileError(loc, "Expected both sides of \"$op\" to have the same o-class type, got $lhsType and $rhsType"),
                )
                else -> listOf(OneLocCompileError(loc, "Cannot apply \"$op\" to o-class type $lhsType"))
            }
        } else {
            emptyList()
        }
        return childErrors + structOpErrors
    }
    override fun inferType(symbolEnv: Map<String, Type>): Type {
        return when (op) {
            "=" -> boolType
            "#" -> boolType
            "*" -> intType
            "/" -> intType
            "%" -> intType
            "<" -> boolType
            "<=" -> boolType
            ">" -> boolType
            ">=" -> boolType
            "&" -> boolType
            "|" -> boolType
            "=>" -> boolType
            "+" -> {
                val lhsType = lhsOperand.getType()
                val rhsType = rhsOperand.getType()
                when {
                    lhsType is IntType && rhsType is IntType -> intType
                    lhsType is StringType || rhsType is StringType -> stringType
                    else -> throw RuntimeException("Cannot add types: $lhsType and $rhsType")
                }
            }
            "-" -> intType
            else -> throw RuntimeException("Invalid binary op: $op")
        }
    }
    override fun toString(): String {
        // for readability
        val lhs = "$lhsOperand"
        val rhs = "$rhsOperand"
        return "($lhs $op $rhs)"
    }
}

class IfElseExprNode(
    private val condExpr : ExprNode,
    private val thenExpr : ExprNode,
    private val elseExpr : ExprNode,
    private val loc : ProgramLoc
) : ExprNode(listOf(condExpr,thenExpr,elseExpr)) {
    override fun programLocation() = loc
    internal fun condExpr(): ExprNode = condExpr
    internal fun thenExpr(): ExprNode = thenExpr
    internal fun elseExpr(): ExprNode = elseExpr
    override fun typePass(symbolEnv : Map<String, Type>, registry: ObjClassRegistry) : List<CompileError> =
        super.typePass(symbolEnv, registry) + ifElseTypeErrors()
    private fun ifElseTypeErrors() : List<CompileError> {
        val errors = mutableListOf<CompileError>()
        if (condExpr.getType() !is BoolType) {
            errors.add(
                OneLocCompileError(programLocation(), "Expected if-condition to be Boolean")
            )
        }
        val thenT = thenExpr.getType()
        val elseT = elseExpr.getType()
        if (thenT != elseT) {
            errors.add(
                TwoLocsCompileError(
                    thenExpr.programLocation(),
                    elseExpr.programLocation(),
                    "Expected if-branches to have the same type, got $thenT and $elseT"
                )
            )
        }
        return errors
    }
    override fun toZ3GuardString(symbolTypes : Map<String,Type>, argSymbols : Set<String>, forceString : Boolean): String {
        val condGuardStr = condExpr.toZ3GuardString(symbolTypes,argSymbols)
        val thenGuardStr = thenExpr.toZ3GuardString(symbolTypes,argSymbols)
        val elseGuardStr = elseExpr.toZ3GuardString(symbolTypes,argSymbols)
        return "ctx.mkITE<BoolSort>($condGuardStr,$thenGuardStr,$elseGuardStr) as BoolExpr"
    }
    override fun toTransitString(symbolTypes: Map<String, Type>, argSymbols: Set<String>): String {
        val condTransitStr = condExpr.toTransitString(symbolTypes,argSymbols)
        val thenTransitStr = thenExpr.toTransitString(symbolTypes,argSymbols)
        val elseTransitStr = elseExpr.toTransitString(symbolTypes,argSymbols)
        return "if ($condTransitStr) {$thenTransitStr} else {$elseTransitStr}"
    }
    override fun inferType(symbolEnv: Map<String, Type>): Type {
        // typePass() ensures that elseExpr has the same type
        return thenExpr.getType()
    }
    override fun toString(): String {
        return "if ($condExpr) {$thenExpr} else {$elseExpr)"
    }
}

class ObjClassLiteralExprNode(
    val className: String,
    val fieldEntries: List<Pair<String, ExprNode>>,
    private val loc: ProgramLoc,
) : ExprNode(fieldEntries.map { it.second }) {
    private sealed interface ObjClassLiteralResolution {
        data object Unresolved : ObjClassLiteralResolution
        data class Resolved(val structType: ObjClassType) : ObjClassLiteralResolution
    }

    private var objClassLiteralResolution: ObjClassLiteralResolution = ObjClassLiteralResolution.Unresolved

    val structType: ObjClassType
        get() = when (val resolution = objClassLiteralResolution) {
            is ObjClassLiteralResolution.Resolved -> resolution.structType
            is ObjClassLiteralResolution.Unresolved ->
                throw RuntimeException("O-class literal type not resolved at $loc")
        }

    val fieldAssignments: Map<String, ExprNode> = fieldEntries.toMap()

    override fun programLocation() = loc

    override fun typePass(symbolEnv: Map<String, Type>, registry: ObjClassRegistry): List<CompileError> {
        val classErrors = when (val classResult = registry.resolveTypeName(className)) {
            is TypeResolveResult.NotFound ->
                listOf(OneLocCompileError(loc, "Unknown o-class \"$className\" in o-class literal"))
            is TypeResolveResult.Found -> {
                if (classResult.type !is ObjClassType) {
                    listOf(OneLocCompileError(loc, "\"$className\" is not an o-class type"))
                } else {
                    objClassLiteralResolution = ObjClassLiteralResolution.Resolved(classResult.type)
                    emptyList()
                }
            }
        }
        if (classErrors.isNotEmpty()) {
            return classErrors
        }

        val resolvedType = structType
        val duplicateFieldErrors = fieldEntries
            .groupBy { it.first }
            .flatMap { (name, entries) ->
                if (entries.size == 1) emptyList()
                else listOf(
                    TwoLocsCompileError(
                        entries[0].second.programLocation(),
                        entries[1].second.programLocation(),
                        "Expected o-class literal fields to have unique names, but found duplicate \"$name\"",
                    )
                )
            }
        val providedFields = fieldEntries.map { it.first }.toSet()
        val expectedFields = resolvedType.fields.map { it.name }.toSet()
        val missingFields = expectedFields - providedFields
        val extraFields = providedFields - expectedFields
        val fieldSetErrors = buildList {
            if (missingFields.isNotEmpty()) {
                add(OneLocCompileError(loc, "O-class literal for \"$className\" is missing fields: $missingFields"))
            }
            if (extraFields.isNotEmpty()) {
                add(OneLocCompileError(loc, "O-class literal for \"$className\" has unknown fields: $extraFields"))
            }
        }
        if (duplicateFieldErrors.isNotEmpty() || fieldSetErrors.isNotEmpty()) {
            return classErrors + duplicateFieldErrors + fieldSetErrors
        }

        val childErrors = super.typePass(symbolEnv, registry)
        if (childErrors.isNotEmpty()) {
            return childErrors
        }

        val matchErrors = resolvedType.fields.flatMap { field ->
            val expr = fieldAssignments.getValue(field.name)
            assertOrCompileError(
                expr.getType() == field.type,
                OneLocCompileError(
                    expr.programLocation(),
                    "Expected field \"${field.name}\" of \"$className\" to have type ${field.type} but got ${expr.getType()}",
                ),
            )
        }
        return matchErrors
    }

    override fun toZ3GuardString(
        symbolTypes: Map<String, Type>,
        argSymbols: Set<String>,
        forceString: Boolean,
    ): String {
        throw RuntimeException("O-class literal at $loc must appear in an equality comparison, not as a scalar expression")
    }

    override fun toTransitString(symbolTypes: Map<String, Type>, argSymbols: Set<String>): String {
        throw RuntimeException("O-class literal at $loc must be assigned via flattened primitive fields")
    }

    override fun inferType(symbolEnv: Map<String, Type>): Type = structType

    override fun toString(): String {
        val fields = fieldEntries.joinToString(", ") { (name, expr) -> "$name := $expr" }
        return "$className { $fields }"
    }
}

class LiteralValueExprNode(
    private val value : String,
    private val type : Type,
    private val loc : ProgramLoc
) : ExprNode(listOf()) {
    override fun programLocation() = loc
    override fun toZ3GuardString(symbolTypes : Map<String,Type>, argSymbols : Set<String>, forceString : Boolean): String {
        if (forceString) {
            return "ctx.mkString(\"$value\")"
        }
        return when (type) {
            is BoolType -> "ctx.mkBool($value)"
            is IntType -> "ctx.mkInt($value)"
            is StringType -> "ctx.mkString(\"$value\")"
            else -> throw RuntimeException("Invalid type: $type")
        }
    }
    override fun toTransitString(symbolTypes: Map<String, Type>, argSymbols: Set<String>): String {
        return if (type is StringType) {
            "\"$value\""
        } else {
            value
        }
    }
    override fun inferType(symbolEnv: Map<String, Type>) = type
    override fun toString(): String {
        return if (type is StringType) "\"$value\"" else value
    }
}

class FieldAccessExprNode(
    val baseSymbol: String,
    val fieldPath: List<String>,
    private val loc: ProgramLoc,
) : ExprNode(listOf()) {
    private sealed interface FieldAccessResolution {
        data object Unresolved : FieldAccessResolution
        data class Resolved(val leafType: Type, val relPath: String) : FieldAccessResolution
    }

    private var fieldResolution: FieldAccessResolution = FieldAccessResolution.Unresolved

    override fun programLocation() = loc

    override fun typePass(symbolEnv: Map<String, Type>, registry: ObjClassRegistry): List<CompileError> {
        val baseType = symbolEnv[baseSymbol]
        if (baseType == null) {
            return listOf(OneLocCompileError(loc, "Unknown variable \"$baseSymbol\" in field access"))
        }
        return when (val result = resolveFieldPath(baseType, fieldPath)) {
            is FieldPathResult.Error -> listOf(OneLocCompileError(loc, result.message))
            is FieldPathResult.Resolved -> {
                fieldResolution = FieldAccessResolution.Resolved(result.type, result.relPath)
                super.typePass(symbolEnv, registry)
            }
        }
    }

    override fun toZ3GuardString(
        symbolTypes: Map<String, Type>,
        argSymbols: Set<String>,
        forceString: Boolean,
    ): String {
        val resolution = fieldResolution as FieldAccessResolution.Resolved
        val baseType = symbolTypes.getValue(baseSymbol) as ObjClassType
        val isArg = baseSymbol in argSymbols
        if (forceString) {
            return when (resolution.leafType) {
                is BoolType -> throw RuntimeException("Cannot convert a Bool to a string")
                is IntType -> {
                    if (isArg) {
                        "ctx.intToString(${baseType.fieldZ3Guard(baseSymbol, resolution.relPath, intType, true)})"
                    } else {
                        val flatName = baseType.flatVarName(baseSymbol, resolution.relPath)
                        "ctx.mkString(${flatName.toKotlinIdent()}.toString())"
                    }
                }
                is StringType -> baseType.fieldZ3Guard(baseSymbol, resolution.relPath, stringType, isArg)
                is ObjClassType -> throw RuntimeException("Cannot convert o-class field to string")
                else -> throw RuntimeException("Invalid field type: ${resolution.leafType}")
            }
        }
        if (resolution.leafType is ObjClassType) {
            throw RuntimeException("O-class field $baseSymbol.${fieldPath.joinToString(".")} must be used in whole-value equality")
        }
        return baseType.fieldZ3Guard(baseSymbol, resolution.relPath, resolution.leafType, isArg)
    }

    override fun toTransitString(symbolTypes: Map<String, Type>, argSymbols: Set<String>): String {
        val resolution = fieldResolution as FieldAccessResolution.Resolved
        val baseType = symbolTypes.getValue(baseSymbol) as ObjClassType
        val flatName = baseType.flatVarName(baseSymbol, resolution.relPath)
        if (baseSymbol in argSymbols) {
            val typeStr = resolution.leafType.toCodegenTypeVal()
            return "(act.lookup(Variable(\"${flatName.escapeKotlinStringLiteral()}\", $typeStr)).value as ${resolution.leafType.toKotlinTypeString()})"
        }
        return flatName.toKotlinIdent()
    }

    override fun inferType(symbolEnv: Map<String, Type>): Type {
        return when (val resolution = fieldResolution) {
            is FieldAccessResolution.Resolved -> resolution.leafType
            is FieldAccessResolution.Unresolved ->
                throw RuntimeException("Field access not resolved at $loc")
        }
    }

    override fun toString(): String = fieldPath.joinToString(".", prefix = "$baseSymbol.")
}

class SymbolValueExprNode(
    val symbol : String,
    private val loc : ProgramLoc
) : ExprNode(listOf()) {
    override fun programLocation() = loc
    override fun toZ3GuardString(symbolTypes : Map<String,Type>, argSymbols : Set<String>, forceString : Boolean): String {
        val type = symbolTypes[symbol]
        if (forceString) {
            return when (type) {
                is BoolType -> throw RuntimeException("Cannot convert a Bool to a string")
                is IntType -> {
                    if (symbol in argSymbols) {
                        "ctx.intToString(ctx.mkIntConst(\"${symbol.escapeKotlinStringLiteral()}\"))"
                    } else {
                        "ctx.mkString(${symbol.toKotlinIdent()}.toString())"
                    }
                }
                is StringType -> {
                    if (symbol in argSymbols) {
                        "ctx.mkStringConst(\"${symbol.escapeKotlinStringLiteral()}\")"
                    } else {
                        "ctx.mkString(${symbol.toKotlinIdent()})"
                    }
                }
                is ObjClassType -> throw RuntimeException("Cannot convert o-class type $type to string")
                else -> throw RuntimeException("Invalid type: $type")
            }

        }
        if (type is ObjClassType) {
            throw RuntimeException("O-class symbol $symbol must be used in whole-value equality, not as a scalar guard expression")
        }
        if (symbol in argSymbols) {
            return when (type) {
                is BoolType -> "ctx.mkBoolConst(\"${symbol.escapeKotlinStringLiteral()}\")"
                is IntType -> "ctx.mkIntConst(\"${symbol.escapeKotlinStringLiteral()}\")"
                is StringType -> "ctx.mkStringConst(\"${symbol.escapeKotlinStringLiteral()}\")"
                else -> throw RuntimeException("Invalid type: $type")
            }
        }
        return when (type) {
            is BoolType -> "ctx.mkBool(${symbol.toKotlinIdent()})"
            is IntType -> "ctx.mkInt(${symbol.toKotlinIdent()})"
            is StringType -> "ctx.mkString(${symbol.toKotlinIdent()})"
            else -> throw RuntimeException("Invalid type: $type")
        }
    }
    override fun toTransitString(symbolTypes: Map<String, Type>, argSymbols: Set<String>): String {
        val type = symbolTypes.getValue(symbol)
        return if (symbol in argSymbols) {
            val typeStr = type.toCodegenTypeVal()
            "(act.lookup(Variable(\"${symbol.escapeKotlinStringLiteral()}\", $typeStr)).value as ${type.toKotlinTypeString()})"
        } else {
            symbol.toKotlinIdent()
        }
    }
    override fun inferType(symbolEnv: Map<String, Type>): Type {
        return symbolEnv[symbol] ?: throw RuntimeException("Found unexpected free variable $symbol at $loc")
    }
    override fun toString(): String {
        return symbol
    }
}

class ValueProcExprNode(
    private val name : String,
    private val loc : ProgramLoc
) : ASTNode(listOf()) {
    override fun programLocation() = loc
    override fun procPass(): List<ProcDecl> {
        return listOf(ProcDecl(name, listOf(), ProcDeclType.Proc))
    }
    override fun toString(): String {
        return name
    }
}

class CompositeProcExprNode(
    private val compositeProcs : List<ASTNode>,
    private val loc : ProgramLoc
) : ASTNode(compositeProcs) {
    override fun programLocation() = loc
    override fun toString(): String {
        return compositeProcs.joinToString(" || ") { it.toString() }
    }
}
