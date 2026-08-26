package julay.compiler.ast

import julay.compiler.decl.ActionDecl
import julay.compiler.decl.TransitUpdate
import julay.compiler.ProgramLoc
import julay.compiler.*
import julay.compiler.pass.TypePassType
import julay.program.*
import julay.program.type.*
import julay.program.action.*

abstract class ASTNode(
    val children : List<ASTNode>
) {
    abstract fun programLocation() : ProgramLoc
}

/** Top-level declaration visibility: file-private by default, or [Export] for `import`. */
enum class DeclVisibility {
    File,
    Export,
}

abstract class DeclNode(children : List<ASTNode>) : ASTNode(children) {
    abstract fun name() : String
    var visibility: DeclVisibility = DeclVisibility.File
    val isExported: Boolean get() = visibility == DeclVisibility.Export
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
    fun argsTypeMap() : Map<String, Type> =
        actionArgs().filter { !it.name.isDiscardBinding() }.associate { it.name to it.type }
    override fun toString(): String {
        return children.joinToString(", ") { it.toString() }
    }
}

abstract class ActionBodyNode(
    private val body : List<ActionBodyNode>,
    exprs : List<ExprNode>
) : ASTNode(body + exprs) {
    open fun guards() : List<ExprNode> = body.flatMap { it.guards() }
    open fun transits() : List<TransitUpdate> = body.flatMap { it.transits() }
    open fun transitVars() : List<Pair<String, ProgramLoc>> = body.flatMap { it.transitVars() }
    open fun befores() : List<CallStmtNode> = body.flatMap { it.befores() }
    open fun afters() : List<CallStmtNode> = body.flatMap { it.afters() }
    open fun errors() : List<ErrorArmNode> = body.flatMap { it.errors() }
    open fun returns() : List<ExprNode> = body.flatMap { it.returns() }
}

abstract class ExprNode(children : List<ASTNode>) : ASTNode(children) {
    private var myType : TypePassType = TypePassType.Uninferred
    internal fun setInferredType(type : TypePassType) { myType = type }
    fun getType() : Type = when (val ts = myType) {
        is TypePassType.Inferred -> ts.type
        is TypePassType.Uninferred ->
            throw RuntimeException("Type not inferred for expression at ${programLocation()}")
    }
    internal abstract fun inferType(symbolEnv : Map<String, Type>) : Type
    abstract fun toZ3GuardString(symbolTypes : Map<String,Type>, argSymbols : Set<String>, forceString : Boolean = false) : String
    abstract fun toTransitString(symbolTypes : Map<String,Type>, argSymbols : Set<String>) : String
}

class ImportNode(
    private val qualifiedName : QualifiedNameNode,
    private val loc : ProgramLoc
) : ASTNode(listOf(qualifiedName)) {
    override fun programLocation() = loc
    fun qualifiedName() = qualifiedName
    override fun toString(): String {
        return "import $qualifiedName"
    }
}

class QualifiedNameNode(
    private val parts : List<String>,
    private val loc : ProgramLoc
) : ASTNode(listOf()) {
    override fun programLocation() = loc
    fun parts() = parts
    fun modulePath() = parts.dropLast(1).joinToString(".")
    fun symbol() = parts.last()
    override fun toString(): String {
        return parts.joinToString(".")
    }
}

class RootNode(
    private val importNodes : List<ImportNode>,
    private val declNodes : List<DeclNode>,
    private val loc : ProgramLoc
) : ASTNode(importNodes + declNodes) {
    private var cachedObjClassRegistry: julay.compiler.decl.ObjClassRegistry? = null

    override fun programLocation(): ProgramLoc = loc
    internal fun importNodes(): List<ImportNode> = importNodes
    internal fun declNodes(): List<DeclNode> = declNodes
    fun withDeclNodes(decls: List<DeclNode>): RootNode = RootNode(importNodes, decls, programLocation())
    fun withImportsAndDecls(imports: List<ImportNode>, decls: List<DeclNode>): RootNode =
        RootNode(imports, decls, programLocation())
    fun cacheObjClassRegistry(registry: julay.compiler.decl.ObjClassRegistry) {
        cachedObjClassRegistry = registry
    }
    fun cachedObjClassRegistry(): julay.compiler.decl.ObjClassRegistry? = cachedObjClassRegistry
    override fun toString(): String {
        return (importNodes + declNodes).joinToString("\n\n") { it.toString() }
    }
}

class ProcClassNode(
    private val name : String,
    private val localDecls : List<ProcClassDeclNode>,
    private val loc : ProgramLoc
) : DeclNode(localDecls) {
    override fun programLocation() = loc
    override fun name() = name
    internal fun procClassNodeName() = name
    internal fun localDecls(): List<ProcClassDeclNode> = localDecls
    internal fun withLocalDecls(decls: List<ProcClassDeclNode>): ProcClassNode =
        ProcClassNode(name, decls, programLocation()).also { it.visibility = this.visibility }
    override fun toString(): String {
        val body = localDecls.joinToString("\n") { "$it".prependIndent() }
        val export = if (isExported) "export " else ""
        return "${export}proc $name {\n$body\n}"
    }
}

class ProcNode(
    private val name : String,
    private val value : ASTNode,
    private val loc : ProgramLoc
) : DeclNode(listOf(value)) {
    override fun programLocation() = loc
    override fun name() = name
    internal fun procNodeName() = name
    internal fun procNodeValue() = value
    override fun toString(): String {
        val export = if (isExported) "export " else ""
        return "${export}proc $name := $value"
    }
}

/**
 * Composition unit: resident [procExpr] peers plus optional [callNames] (procfuns).
 * Conceptually `proc Name := procExpr || call1 || …`; calls are not SyncChannel-started.
 */
class ApiNode(
    private val name: String,
    private val procExpr: ASTNode,
    private val callNames: List<String>,
    private val loc: ProgramLoc,
) : DeclNode(listOf(procExpr)) {
    override fun programLocation() = loc
    override fun name() = name
    internal fun apiName() = name
    internal fun apiProcExpr() = procExpr
    internal fun apiCallNames() = callNames
    override fun toString(): String {
        val export = if (isExported) "export " else ""
        val calls = if (callNames.isEmpty()) "" else {
            "\n    calls: ${callNames.joinToString(", ")}"
        }
        return "${export}api $name {\n    proc: $procExpr$calls\n}"
    }
}

class CompileNode(
    private val names: List<String>,
    private val loc: ProgramLoc,
) : DeclNode(listOf()) {
    override fun programLocation() = loc
    override fun name() = names.joinToString(", ")
    internal fun compileNames() = names
    override fun toString(): String = "compile ${names.joinToString(", ")}"
}

class SpecNode(
    private val name : String,
    private val value : ASTNode,
    private val loc : ProgramLoc
) : DeclNode(listOf(value)) {
    override fun programLocation() = loc
    override fun name() = name
    internal fun specNodeName() = name
    internal fun specNodeValue() = value
    override fun toString(): String {
        val export = if (isExported) "export " else ""
        return "${export}spec $name := $value"
    }
}

/**
 * Leaf spec: proc-class-shaped body that compiles only to TLA+ (never a JAR).
 * Optional declaration parameter `[paramName : paramType]` is an immutable binder
 * in scope in the body and becomes the TLA instance index when present.
 */
class LeafSpecNode(
    private val name: String,
    private val paramName: String?,
    private val paramType: TypeExpr?,
    private val localDecls: List<ProcClassDeclNode>,
    private val loc: ProgramLoc,
) : DeclNode(localDecls) {
    override fun programLocation() = loc
    override fun name() = name
    internal fun leafSpecName() = name
    internal fun leafSpecParamName() = paramName
    internal fun leafSpecParamType() = paramType
    internal fun localDecls(): List<ProcClassDeclNode> = localDecls
    internal fun isParameterized(): Boolean = paramName != null
    /** View as a [ProcClassNode] for TLA / alphabet reuse. */
    internal fun asProcClass(): ProcClassNode =
        ProcClassNode(name, localDecls, loc).also { it.visibility = this.visibility }
    override fun toString(): String {
        val body = localDecls.joinToString("\n") { "$it".prependIndent() }
        val export = if (isExported) "export " else ""
        val params = if (paramName != null && paramType != null) "[$paramName : $paramType]" else ""
        return "${export}spec $name$params {\n$body\n}"
    }
}

class InvariantNode(
    private val name: String,
    private val formula: ExprNode,
    private val loc: ProgramLoc,
) : DeclNode(listOf(formula)) {
    override fun programLocation() = loc
    override fun name() = name
    internal fun invariantName() = name
    internal fun invariantFormula() = formula
    override fun toString(): String = "invariant $name := $formula"
}

class AgSpecExprNode(
    /** null means `<true>` (no assumption). */
    private val assume: ASTNode?,
    private val system: ASTNode,
    /** null means `<true>` (no guarantee). */
    private val guarantee: ExprNode?,
    private val loc: ProgramLoc,
) : ASTNode(listOfNotNull(assume, system, guarantee)) {
    override fun programLocation() = loc
    internal fun assumeExpr() = assume
    internal fun systemExpr() = system
    internal fun guaranteeExpr() = guarantee
    override fun toString(): String {
        val a = assume?.toString() ?: "true"
        val g = guarantee?.toString() ?: "true"
        return "<$a> $system <$g>"
    }
}

/** Create-index `{ global x }` / `{ const global x }` names with source location. */
data class GlobalDeclNames(
    val names: List<String>,
    val isConst: Boolean,
    val loc: ProgramLoc,
)

class ParamProcExprNode(
    private val body: ASTNode,
    private val paramName: String,
    /** Null when this is an apply-index `Name[n]` (type comes from enclosing `with`). */
    private val paramType: TypeExpr?,
    private val loc: ProgramLoc,
    /** Create-index `global` / `const global` decls; empty for apply-index. */
    private val globalDecls: List<GlobalDeclNames> = emptyList(),
    /** Create-index `init:` Boolean exprs; empty for apply-index. */
    private val initExprs: List<ExprNode> = emptyList(),
    /** Create-index delayed domain models (`Name := { … }`). */
    private val typeModels: List<TypeModelNode> = emptyList(),
) : ASTNode(listOf(body) + initExprs + typeModels) {
    override fun programLocation() = loc
    internal fun paramBody() = body
    internal fun paramName() = paramName
    internal fun paramType(): TypeExpr? = paramType
    internal fun isApplyIndex(): Boolean = paramType == null
    internal fun globalDecls(): List<GlobalDeclNames> = globalDecls
    internal fun globalVarNames(): List<String> = globalDecls.flatMap { it.names }
    internal fun globalConstVarNames(): List<String> =
        globalDecls.filter { it.isConst }.flatMap { it.names }
    internal fun initExprs(): List<ExprNode> = initExprs
    internal fun typeModels(): List<TypeModelNode> = typeModels
    override fun toString(): String {
        val index = if (paramType == null) "$body[$paramName]" else "$body[$paramName : $paramType]"
        if (globalDecls.isEmpty() && initExprs.isEmpty() && typeModels.isEmpty()) return index
        val lines = globalDecls.map { decl ->
            val names = decl.names.joinToString(", ")
            val kw = if (decl.isConst) "const global" else "global"
            "  $kw $names"
        } + typeModels.map { it.toString().prependIndent("  ") } +
            initExprs.map { expr ->
                "  init: $expr"
            }
        return "$index {\n${lines.joinToString("\n")}\n}"
    }
}

/** `with (n : T) { system }` — introduces binder [n] for apply-index forms inside. */
class WithSpecExprNode(
    private val binderName: String,
    private val binderType: TypeExpr,
    private val body: ASTNode,
    private val loc: ProgramLoc,
) : ASTNode(listOf(body)) {
    override fun programLocation() = loc
    internal fun withBinderName() = binderName
    internal fun withBinderType() = binderType
    internal fun withBody() = body
    override fun toString(): String = "with ($binderName : $binderType) { $body }"
}

class ObjClassNode(
    private val name: String,
    val typeParams: List<String>,
    val fields: List<FieldNode>,
    private val loc: ProgramLoc,
) : DeclNode(fields) {
    override fun programLocation() = loc
    override fun name() = name
    internal fun objClassNodeName() = name
    internal fun objClassTypeParams(): List<String> = typeParams
    internal fun objClassFields(): List<FieldNode> = fields
    override fun toString(): String {
        val params = if (typeParams.isEmpty()) "" else typeParams.joinToString(", ", "<", ">")
        val body = fields.joinToString("\n") { "$it".prependIndent() }
        return "type $name$params {\n$body\n}"
    }
}

/** `type Name` (uninterpreted) or `type Name := Carrier` (typedef). */
class DomainDeclNode(
    private val name: String,
    private val aliasTypeExpr: TypeExpr?,
    private val loc: ProgramLoc,
) : DeclNode(emptyList()) {
    override fun programLocation() = loc
    override fun name() = name
    internal fun aliasTypeExpr(): TypeExpr? = aliasTypeExpr
    internal fun isTypedef(): Boolean = aliasTypeExpr != null
    override fun toString(): String {
        val export = if (isExported) "export " else ""
        return if (aliasTypeExpr != null) {
            "${export}type $name := $aliasTypeExpr"
        } else {
            "${export}type $name"
        }
    }
}

/** Delayed model `Name := { lit, ... }` for uninterpreted/typedef domains. */
class TypeModelNode(
    private val name: String,
    val elements: List<LiteralValueExprNode>,
    private val loc: ProgramLoc,
) : DeclNode(elements) {
    override fun programLocation() = loc
    override fun name(): String = name
    override fun toString(): String = "$name := {${elements.joinToString(", ")}}"
}

class FieldNode(
    val fieldName: String,
    val typeExpr: TypeExpr,
    private val loc: ProgramLoc,
) : ASTNode(listOf()) {
    override fun programLocation() = loc
    @Deprecated("Use typeExpr", ReplaceWith("typeExpr.toString()"))
    val typeName: String get() = typeExpr.toString()
    override fun toString(): String = "$fieldName : $typeExpr"
}

class FunNode(
    private val name: String,
    val typeParams: List<String>,
    private val args: ArgsNode,
    private val returnTypeExpr: TypeExpr,
    private val body: ExprNode,
    private val loc: ProgramLoc,
) : DeclNode(listOf(args, body)) {
    private var returnTypeResolution: TypeNameResolution = TypeNameResolution.Unresolved

    val returnType: Type
        get() = when (val resolution = returnTypeResolution) {
            is TypeNameResolution.Resolved -> resolution.type
            is TypeNameResolution.Unresolved ->
                throw RuntimeException("Return type not resolved for function \"$name\"")
        }

    override fun programLocation() = loc
    override fun name() = name
    internal fun funTypeParams(): List<String> = typeParams
    internal fun funArgs(): ArgsNode = args
    internal fun funReturnTypeExpr(): TypeExpr = returnTypeExpr
    internal fun funReturnTypeName(): String = returnTypeExpr.toString()
    internal fun funBody(): ExprNode = body
    internal fun resolveReturnType(type: Type) {
        returnTypeResolution = TypeNameResolution.Resolved(type)
    }
    override fun toString(): String {
        val params = if (typeParams.isEmpty()) "" else typeParams.joinToString(", ", "<", ">")
        val displayReturn = when (val resolution = returnTypeResolution) {
            is TypeNameResolution.Resolved -> resolution.type
            is TypeNameResolution.Unresolved -> returnTypeExpr
        }
        return "fun $name$params($args) : $displayReturn = $body"
    }
}

/**
 * Process-backed blocking function: args become consts, body has state + internal/client
 * steps and untagged `return:` transitions. Invoked via call expressions (not `||`).
 */
class ProcFunNode(
    private val name: String,
    private val args: ArgsNode,
    private val returnTypeExpr: TypeExpr,
    private val localDecls: List<ProcClassDeclNode>,
    private val loc: ProgramLoc,
) : DeclNode(listOf(args) + localDecls) {
    private var returnTypeResolution: TypeNameResolution = TypeNameResolution.Unresolved

    val returnType: Type
        get() = when (val resolution = returnTypeResolution) {
            is TypeNameResolution.Resolved -> resolution.type
            is TypeNameResolution.Unresolved ->
                throw RuntimeException("Return type not resolved for procfun \"$name\"")
        }

    override fun programLocation() = loc
    override fun name() = name
    internal fun procFunName() = name
    internal fun procFunArgs(): ArgsNode = args
    internal fun procFunReturnTypeExpr(): TypeExpr = returnTypeExpr
    internal fun localDecls(): List<ProcClassDeclNode> = localDecls
    internal fun withLocalDecls(decls: List<ProcClassDeclNode>): ProcFunNode =
        ProcFunNode(name, args, returnTypeExpr, decls, loc).also {
            it.visibility = this.visibility
            when (val r = returnTypeResolution) {
                is TypeNameResolution.Resolved -> it.resolveReturnType(r.type)
                else -> {}
            }
        }
    internal fun resolveReturnType(type: Type) {
        returnTypeResolution = TypeNameResolution.Resolved(type)
    }
    override fun toString(): String {
        val displayReturn = when (val resolution = returnTypeResolution) {
            is TypeNameResolution.Resolved -> resolution.type
            is TypeNameResolution.Unresolved -> returnTypeExpr
        }
        val body = localDecls.joinToString("\n") { "$it".prependIndent() }
        val export = if (isExported) "export " else ""
        return "${export}procfun $name($args) : $displayReturn {\n$body\n}"
    }
}

class FunCallExprNode(
    private val name: String,
    private val args: List<ExprNode>,
    private val loc: ProgramLoc,
    resolved: FunNode? = null,
    private var instantiatedReturnType: Type? = null,
    private val typeArgs: List<TypeExpr> = emptyList(),
) : ExprNode(args) {
    private var resolvedFun: FunNode? = resolved
    private var resolvedBuiltin: FunBuiltin? = null
    private var resolvedProcFun: ProcFunNode? = null
    private var specializedBody: ExprNode? = null
    /** For named-fun HOFs like map: the unary user fun being applied. */
    private var namedFunArgNode: FunNode? = null
    private var namedFunParamName: String? = null
    private var namedFunBody: ExprNode? = null
    private var namedFunElemType: Type? = null

    override fun programLocation() = loc
    fun callName(): String = name
    fun callArgs(): List<ExprNode> = args
    fun callTypeArgs(): List<TypeExpr> = typeArgs
    internal fun resolvedFunOrNull(): FunNode? = resolvedFun
    internal fun resolvedBuiltinOrNull(): FunBuiltin? = resolvedBuiltin
    internal fun resolvedProcFunOrNull(): ProcFunNode? = resolvedProcFun
    internal fun namedFunArgNodeOrNull(): FunNode? = namedFunArgNode
    internal fun namedFunParamNameOrNull(): String? = namedFunParamName
    internal fun namedFunBodyOrNull(): ExprNode? = namedFunBody
    internal fun namedFunElemTypeOrNull(): Type? = namedFunElemType
    internal fun resolveFun(funNode: FunNode) {
        resolvedFun = funNode
        resolvedBuiltin = null
        resolvedProcFun = null
    }
    internal fun resolveBuiltin(builtin: FunBuiltin) {
        resolvedBuiltin = builtin
        resolvedFun = null
        resolvedProcFun = null
    }
    internal fun resolveProcFun(procFun: ProcFunNode) {
        resolvedProcFun = procFun
        resolvedFun = null
        resolvedBuiltin = null
    }
    internal fun resolveNamedFunArg(funNode: FunNode?, paramName: String, body: ExprNode, elemType: Type) {
        namedFunArgNode = funNode
        namedFunParamName = paramName
        namedFunBody = body
        namedFunElemType = elemType
    }
    internal fun resolveInstantiatedReturnType(type: Type) {
        instantiatedReturnType = type
    }
    internal fun resolveSpecializedBody(body: ExprNode) {
        specializedBody = body
    }
    internal fun specializedBodyOrNull(): ExprNode? = specializedBody

    private fun inlinedBody(): ExprNode {
        specializedBody?.let { return it }
        val funNode = resolvedFun
            ?: throw RuntimeException("Function call \"$name\" not resolved at $loc")
        val params = funNode.funArgs().actionArgs()
        return params.zip(args).fold(funNode.funBody()) { acc, (param, arg) ->
            substituteExpr(acc, param.name, arg)
        }
    }

    private fun mapKotlinCodegen(symbolTypes: Map<String, Type>, argSymbols: Set<String>): String {
        val coll = args[0].toTransitString(symbolTypes, argSymbols)
        val paramName = namedFunParamName
            ?: throw RuntimeException("map named-fun param not resolved at $loc")
        val body = namedFunBody
            ?: throw RuntimeException("map named-fun body not resolved at $loc")
        val elemType = namedFunElemType
            ?: throw RuntimeException("map element type not resolved at $loc")
        val elemIdent = "__julay_map_elem"
        val elemSym = SymbolValueExprNode(elemIdent, loc)
        val inlined = substituteExpr(body, paramName, elemSym)
        val bodyStr = inlined.toTransitString(symbolTypes + (elemIdent to elemType), argSymbols)
        return when (val collType = args[0].getType()) {
            is ListType -> "$coll.map { $elemIdent -> $bodyStr }"
            is SetType -> "$coll.map { $elemIdent -> $bodyStr }.toSet()"
            else -> throw RuntimeException("map expected List or Set at $loc but got $collType")
        }
    }

    override fun toZ3GuardString(
        symbolTypes: Map<String, Type>,
        argSymbols: Set<String>,
        forceString: Boolean,
    ): String {
        if (resolvedProcFun != null) {
            throw RuntimeException("Procfun \"$name\" cannot be used in guards at $loc")
        }
        resolvedBuiltin?.let { builtin ->
            if (builtin.namedFunArg) {
                throw RuntimeException("Function \"${builtin.name}\" cannot be used in guards")
            }
            if (builtin.returnType == null) {
                throw RuntimeException("Function \"${builtin.name}\" cannot be used in guards")
            }
            if (builtin.name == "allDistinct") {
                if (exprReferencesAnyArg(this, argSymbols)) {
                    throw RuntimeException(
                        "Function \"allDistinct\" cannot be used in guards when it depends on action arguments at $loc",
                    )
                }
                return embedKotlinValueAsZ3(toTransitString(symbolTypes, argSymbols), boolType, forceString, loc)
            }
            val argStrs = args.map { it.toZ3GuardString(symbolTypes, argSymbols, forceString) }
            if (builtin.name == "length" && args.isNotEmpty()) {
                return when (val argType = args[0].getType()) {
                    is ListType -> "ctx.mkSeqLengthAny(${argStrs[0]})"
                    is SetType -> {
                        val meta = "${argType.toCodegenTypeVal()}.cellMetadata(ctx)"
                        "setCellSizeExpr(ctx, ${argStrs[0]}, $meta.sizeAccessor)"
                    }
                    is MapType -> {
                        val meta = "${argType.toCodegenTypeVal()}.cellMetadata(ctx)"
                        "mapCellSizeExpr(ctx, ${argStrs[0]}, $meta.sizeAccessor)"
                    }
                    else -> builtin.z3Codegen(argStrs)
                }
            }
            if (builtin.name == "splice" && args.size == 3) {
                if (forceString) {
                    return "ctx.mkString(${toTransitString(symbolTypes, argSymbols)}.toString())"
                }
                val baseType = args[0].getType()
                if (baseType !is ListType) {
                    throw RuntimeException("Cannot splice non-list type $baseType at $loc")
                }
                // 1-based inclusive: end < 1 → empty; else lo = start; hi = min(end, Len);
                // empty if lo > hi; else extract at offset lo-1 with length hi-lo+1.
                val empty = "ctx.mkEmptySeq(${baseType.toCodegenTypeVal()}.sort(ctx))"
                val xs = argStrs[0]
                val s = argStrs[1]
                val e = argStrs[2]
                val len = "ctx.mkSeqLengthAny($xs)"
                return "run { " +
                    "val __empty = $empty; " +
                    "ctx.mkITE(ctx.mkLt($e, ctx.mkInt(1)), __empty, run { " +
                    "val __hi = ctx.mkITE(ctx.mkGt($e, $len), $len, $e); " +
                    "val __lo = $s; " +
                    "val __off = ctx.mkSub(__lo, ctx.mkInt(1)); " +
                    "val __n = ctx.mkAdd(ctx.mkSub(__hi, __lo), ctx.mkInt(1)); " +
                    "ctx.mkITE(ctx.mkGt(__lo, __hi), __empty, ctx.mkSeqExtractAny($xs, __off, __n)) " +
                    "}) }"
            }
            return builtin.z3Codegen(argStrs)
        }
        return inlinedBody().toZ3GuardString(symbolTypes, argSymbols, forceString)
    }

    override fun toTransitString(symbolTypes: Map<String, Type>, argSymbols: Set<String>): String {
        resolvedProcFun?.let { pf ->
            val argStrs = args.map { it.toTransitString(symbolTypes, argSymbols) }
            val argsList = if (argStrs.isEmpty()) {
                "emptyList()"
            } else {
                "listOf(${argStrs.joinToString(", ")})"
            }
            val retTy = pf.returnType.toKotlinTypeString()
            return "(hostProc.invokeProcFun(\"${pf.procFunName()}\", $argsList) as $retTy)"
        }
        resolvedBuiltin?.let { builtin ->
            if (builtin.namedFunArg) {
                return mapKotlinCodegen(symbolTypes, argSymbols)
            }
            val argStrs = args.map { it.toTransitString(symbolTypes, argSymbols) }
            return builtin.kotlinCodegen(argStrs)
        }
        return inlinedBody().toTransitString(symbolTypes, argSymbols)
    }

    override fun inferType(symbolEnv: Map<String, Type>): Type {
        instantiatedReturnType?.let { return it }
        resolvedProcFun?.let { return it.returnType }
        resolvedBuiltin?.let { builtin ->
            return builtin.returnType
                ?: throw RuntimeException("Function \"${builtin.name}\" returns no value at $loc")
        }
        val funNode = resolvedFun
            ?: throw RuntimeException("Function call \"$name\" not resolved at $loc")
        return funNode.returnType
    }

    override fun toString(): String {
        val typeStr = if (typeArgs.isEmpty()) "" else "<${typeArgs.joinToString(", ")}>"
        val argsStr = args.joinToString(", ")
        return "$name$typeStr($argsStr)"
    }
}

private sealed interface TypeNameResolution {
    data object Unresolved : TypeNameResolution
    data class Resolved(val type: Type) : TypeNameResolution
}

class VarNode(
    val name : String,
    val typeExpr : TypeExpr,
    private val loc : ProgramLoc,
    val isConst : Boolean = false,
    val initExpr : ExprNode? = null,
) : ProcClassDeclNode(listOfNotNull(initExpr)) {
    private var typeResolution : TypeNameResolution = TypeNameResolution.Unresolved
    val type : Type
        get() = when (val resolution = typeResolution) {
            is TypeNameResolution.Resolved -> resolution.type
            is TypeNameResolution.Unresolved ->
                throw RuntimeException("Type not resolved for state variable \"$name\"")
        }
    val typeName: String get() = typeExpr.toString()
    override fun programLocation() = loc
    internal fun resolveType(type: Type) {
        typeResolution = TypeNameResolution.Resolved(type)
    }
    override fun stateVariables(): List<Variable> = listOf(Variable(name, type))
    companion object {
        fun primitive(name: String, type: Type, loc: ProgramLoc): VarNode {
            val node = VarNode(name, TypeExpr.Simple(primitiveTypeName(type)), loc)
            node.typeResolution = TypeNameResolution.Resolved(type)
            return node
        }

        private fun primitiveTypeName(type: Type): String = when (type) {
            is BoolType -> "Boolean"
            is IntType -> "Int"
            is RealType -> "Real"
            is StringType -> "String"
            else -> throw RuntimeException("Cannot create flattened VarNode for type $type")
        }
    }
    override fun toString(): String {
        val displayType = when (val resolution = typeResolution) {
            is TypeNameResolution.Resolved -> resolution.type
            is TypeNameResolution.Unresolved -> typeExpr
        }
        val keyword = if (isConst) "const" else "var"
        val init = initExpr?.let { " := $it" } ?: ""
        return "$keyword $name : $displayType$init"
    }
}

class ConstructorNode(
    private val name: String,
    private val args: ArgsNode,
    private val body: List<ActionBodyNode>,
    private val loc: ProgramLoc,
    private val isSession: Boolean = false,
    private val alsoArgs: ArgsNode? = null,
) : ProcClassDeclNode(listOfNotNull(args, alsoArgs) + body) {
    override fun programLocation() = loc
    override fun transitVars() = body.flatMap { it.transitVars() }
    override fun constructors(): List<ActionDecl> {
        val actionArgs = args.actionArgs()
        val guards = body.flatMap { it.guards() }
        return listOf(
            ActionDecl(
                SymbolicAction(name, actionArgs, isSession = isSession),
                guards,
                body.flatMap { it.transits() },
                TSAction.SyncRole.Default,
                loc,
                body.flatMap { it.befores() },
                body.flatMap { it.afters() },
                body.flatMap { it.errors() },
            )
        )
    }
    internal fun body(): List<ActionBodyNode> = body
    internal fun constructorName(): String = name
    internal fun constructorArgs(): ArgsNode = args
    internal fun alsoArgs(): ArgsNode? = alsoArgs
    internal fun actionArgs(): List<Variable> = args.actionArgs()
    internal fun withBody(newBody: List<ActionBodyNode>): ConstructorNode =
        ConstructorNode(name, args, newBody, programLocation(), isSession, alsoArgs)
    override fun toString(): String {
        val sessionStr = if (isSession) "session " else ""
        val alsoStr = alsoArgs?.let { " also $it" } ?: ""
        val bodyStr = body.joinToString("\n") { "$it".prependIndent() }
        return "${sessionStr}constructor $name($args)$alsoStr {\n$bodyStr\n}"
    }
}

class TransitionNode(
    private val modifier: TSAction.SyncRole,
    private val name: String,
    private val args: ArgsNode,
    private val body: List<ActionBodyNode>,
    private val loc: ProgramLoc,
    private val isSession: Boolean = false,
    private val alsoArgs: ArgsNode? = null,
) : ProcClassDeclNode(listOfNotNull(args, alsoArgs) + body) {
    override fun programLocation() = loc
    override fun transitVars() = body.flatMap { it.transitVars() }
    override fun transitions(): List<ActionDecl> {
        val actionArgs = args.actionArgs()
        val guards = body.flatMap { it.guards() }
        val returnExprs = body.flatMap { it.returns() }
        val returnExpr = when (returnExprs.size) {
            0 -> null
            1 -> returnExprs[0]
            else -> throw RuntimeException("Transition \"$name\" has multiple return: clauses at $loc")
        }
        return listOf(
            ActionDecl(
                SymbolicAction(
                    name,
                    actionArgs,
                    isInternal = modifier == TSAction.SyncRole.Internal,
                    isSession = isSession,
                ),
                guards,
                body.flatMap { it.transits() },
                modifier,
                loc,
                body.flatMap { it.befores() },
                body.flatMap { it.afters() },
                body.flatMap { it.errors() },
                returnExpr = returnExpr,
            )
        )
    }
    internal fun transitionName() = name
    internal fun body(): List<ActionBodyNode> = body
    internal fun transitionArgs(): ArgsNode = args
    internal fun alsoArgs(): ArgsNode? = alsoArgs
    internal fun actionArgs(): List<Variable> = args.actionArgs()
    internal fun modifier(): TSAction.SyncRole = modifier
    internal fun isSessionTransition(): Boolean = isSession
    internal fun withBody(newBody: List<ActionBodyNode>): TransitionNode =
        TransitionNode(modifier, name, args, newBody, programLocation(), isSession, alsoArgs)
    override fun toString(): String {
        val modifierStr = when {
            isSession -> "session "
            modifier == TSAction.SyncRole.Provider -> "provider "
            modifier == TSAction.SyncRole.Client -> "client "
            modifier == TSAction.SyncRole.Internal -> "internal "
            else -> ""
        }
        val alsoStr = alsoArgs?.let { " also $it" } ?: ""
        val bodyStr = body.joinToString("\n") { "$it".prependIndent() }
        return "${modifierStr}transition $name($args)$alsoStr {\n$bodyStr\n}"
    }
}

class ArgNode(
    private val name : String,
    private val typeExpr : TypeExpr,
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
    internal fun argName() = name
    internal fun argTypeExpr() = typeExpr
    internal fun argTypeName() = typeExpr.toString()
    internal fun resolveArgType(type: Type) {
        typeResolution = TypeNameResolution.Resolved(type)
    }
    override fun actionArgs(): List<Variable> {
        return listOf(Variable(name, type))
    }
    override fun toString(): String {
        val displayType = when (val resolution = typeResolution) {
            is TypeNameResolution.Resolved -> resolution.type
            is TypeNameResolution.Unresolved -> typeExpr
        }
        return "$name : $displayType"
    }
}

class GuardNode(
    private val expr : ExprNode,
    private val loc : ProgramLoc
) : ActionBodyNode(listOf(), listOf(expr)) {
    override fun programLocation() = loc
    internal fun guardExpr() = expr
    override fun guards(): List<ExprNode> {
        return listOf(expr)
    }
    override fun toString(): String {
        val exprStr = "$expr".prependIndent()
        return "guard:\n$exprStr"
    }
}

class ReturnNode(
    private val expr: ExprNode,
    private val loc: ProgramLoc,
) : ActionBodyNode(listOf(), listOf(expr)) {
    override fun programLocation() = loc
    internal fun returnExpr() = expr
    override fun returns(): List<ExprNode> = listOf(expr)
    override fun toString(): String {
        val exprStr = "$expr".prependIndent()
        return "return:\n$exprStr"
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

class ErrorArmNode(
    private val cond : ExprNode,
    private val msg : ExprNode,
    private val loc : ProgramLoc
) : ASTNode(listOf(cond, msg)) {
    override fun programLocation() = loc
    fun condExpr() = cond
    fun msgExpr() = msg
    override fun toString(): String = "$cond -> $msg"
}

class ErrorNode(
    private val arms : List<ErrorArmNode>,
    private val loc : ProgramLoc
) : ActionBodyNode(listOf(), arms.flatMap { listOf(it.condExpr(), it.msgExpr()) }) {
    override fun programLocation() = loc
    override fun errors(): List<ErrorArmNode> = arms
    override fun toString(): String {
        return "error:\n${arms.joinToString("\n") { "$it".prependIndent() }}"
    }
}

class CallStmtNode(
    private val name : String,
    private val args : List<ExprNode>,
    private val loc : ProgramLoc,
    private val typeArgs: List<TypeExpr> = emptyList(),
) : ASTNode(args) {
    private var resolvedBuiltin: FunBuiltin? = null
    private var resolvedFun: FunNode? = null

    override fun programLocation() = loc
    fun callName() = name
    fun callArgs() = args
    fun callTypeArgs(): List<TypeExpr> = typeArgs
    internal fun resolvedBuiltinOrNull() = resolvedBuiltin
    internal fun resolvedFunOrNull() = resolvedFun
    internal fun resolveBuiltin(builtin: FunBuiltin) {
        resolvedBuiltin = builtin
        resolvedFun = null
    }
    internal fun resolveFun(funNode: FunNode) {
        resolvedFun = funNode
        resolvedBuiltin = null
    }
    override fun toString(): String {
        val typeStr = if (typeArgs.isEmpty()) "" else "<${typeArgs.joinToString(", ")}>"
        val argStr = args.joinToString(", ") { "$it" }
        return if (args.isEmpty()) "$name$typeStr()" else "$name$typeStr($argStr)"
    }
}

class BeforeNode(
    private val stmts : List<CallStmtNode>,
    private val loc : ProgramLoc
) : ActionBodyNode(emptyList(), listOf()) {
    override fun programLocation() = loc
    override fun befores(): List<CallStmtNode> = stmts
    override fun toString(): String {
        return "before:\n${stmts.joinToString("\n") { "$it".prependIndent() }}"
    }
}

class AfterNode(
    private val stmts : List<CallStmtNode>,
    private val loc : ProgramLoc
) : ActionBodyNode(emptyList(), listOf()) {
    override fun programLocation() = loc
    override fun afters(): List<CallStmtNode> = stmts
    override fun toString(): String {
        return "after:\n${stmts.joinToString("\n") { "$it".prependIndent() }}"
    }
}

class IndexTransitNode(
    val collectionVar: String,
    val index: ExprNode,
    val value: ExprNode,
    private val loc: ProgramLoc,
) : ActionBodyNode(listOf(), listOf(index, value)) {
    override fun programLocation() = loc

    override fun transitVars(): List<Pair<String, ProgramLoc>> = emptyList()

    override fun transits(): List<TransitUpdate> =
        listOf(TransitUpdate.IndexPut(collectionVar, index, value))

    override fun toString(): String = "$collectionVar[$index] := $value"
}

class LetTransitNode(
    private val name: String,
    private val typeExpr: TypeExpr,
    private val initExpr: ExprNode,
    private val loc: ProgramLoc,
    resolvedType: Type? = null,
) : ActionBodyNode(listOf(), listOf(initExpr)) {
    private var letTypeResolution: Type? = resolvedType

    val resolvedLetType: Type
        get() = letTypeResolution
            ?: throw RuntimeException("Type not resolved for transit let binding \"$name\" at $loc")

    override fun programLocation() = loc
    internal fun letName(): String = name
    internal fun letTypeExpr(): TypeExpr = typeExpr
    internal fun letTypeName(): String = typeExpr.toString()
    internal fun letInitExpr(): ExprNode = initExpr
    internal fun resolvedLetTypeOrNull(): Type? = letTypeResolution

    internal fun resolveLetType(type: Type) {
        letTypeResolution = type
    }

    override fun transitVars(): List<Pair<String, ProgramLoc>> = emptyList()

    override fun transits(): List<TransitUpdate> =
        listOf(TransitUpdate.Let(name, resolvedLetType, initExpr))

    override fun toString(): String = "let $name : $typeExpr := $initExpr"
}

class VarTransitNode(
    val varName : String,
    val fieldPath : List<String> = emptyList(),
    val expr : ExprNode,
    private val loc : ProgramLoc
) : ActionBodyNode(listOf(), listOf(expr)) {
    override fun programLocation() = loc

    internal fun transitKey(): String =
        if (fieldPath.isEmpty()) varName else "$varName.${fieldPath.joinToString(".")}"

    internal fun transitExpr() = expr

    override fun transitVars() = listOf(Pair(transitKey(), loc))
    override fun transits(): List<TransitUpdate> {
        return listOf(TransitUpdate.Assign(transitKey(), expr))
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
        return when (op) {
            "~" -> "(!($transitStr))"
            else -> "($op $transitStr)"
        }
    }
    override fun inferType(symbolEnv: Map<String, Type>): Type {
        return when (op) {
            "~" -> {
                val operandType = operand.getType()
                if (operandType !is BoolType) {
                    throw RuntimeException("Cannot apply \"~\" to type $operandType")
                }
                boolType
            }
            else -> throw RuntimeException("Invalid unary op: $op")
        }
    }
    override fun toString(): String {
        return "($op $operand)"
    }
}

/** Explicit `(…)` written in Julay source; preserved when emitting TLA+. */
class ParenExprNode(
    private val inner: ExprNode,
    private val loc: ProgramLoc,
) : ExprNode(listOf(inner)) {
    override fun programLocation() = loc
    fun innerExpr(): ExprNode = inner
    override fun toZ3GuardString(symbolTypes: Map<String, Type>, argSymbols: Set<String>, forceString: Boolean): String =
        inner.toZ3GuardString(symbolTypes, argSymbols, forceString)
    override fun toTransitString(symbolTypes: Map<String, Type>, argSymbols: Set<String>): String =
        "(${inner.toTransitString(symbolTypes, argSymbols)})"
    override fun inferType(symbolEnv: Map<String, Type>): Type = inner.getType()
    override fun toString(): String = "($inner)"
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
        // String coerce via `e + ""` / `"" + e` sets forceString on the non-string side.
        // Nested arithmetic (e.g. `(n+1) + ""`) is not itself string-concat; stringify the
        // normally-emitted Z3 term (same path as castFieldZ3 for Int fields).
        if (forceString && !isStringConcat) {
            val inner = toZ3GuardString(symbolTypes, argSymbols, forceString = false)
            return when (val resultType = getType()) {
                is IntType -> "ctx.intToString($inner as IntExpr)"
                is BoolType ->
                    throw RuntimeException("Cannot force a binary boolean operator to a string")
                is RealType ->
                    throw RuntimeException("Cannot convert a Real expression to a string")
                is StringType -> inner
                else ->
                    "ctx.mkString((${toTransitString(symbolTypes, argSymbols)}).toString())"
            }
        }

        val forceStringOperands = forceString || isStringConcat
        val lhsGuardStr = lhsOperand.toZ3GuardString(symbolTypes, argSymbols, forceStringOperands)
        val rhsGuardStr = rhsOperand.toZ3GuardString(symbolTypes, argSymbols, forceStringOperands)

        fun numericZ3(mkOp: (String, String) -> String): String {
            val promoted = promoteNumeric(lhsType, rhsType)
                ?: throw RuntimeException("Cannot apply \"$op\" to types $lhsType and $rhsType")
            return if (promoted is RealType) {
                mkOp(asZ3Real(lhsGuardStr, lhsType), asZ3Real(rhsGuardStr, rhsType))
            } else {
                mkOp(lhsGuardStr, rhsGuardStr)
            }
        }

        return when (op) {
            "*" -> numericZ3 { l, r -> "ctx.mkMul($l,$r)" }
            "/" -> numericZ3 { l, r -> "ctx.mkDiv($l,$r)" }
            "%" -> {
                if (lhsType !is IntType || rhsType !is IntType) {
                    throw RuntimeException("Cannot apply \"%\" to types $lhsType and $rhsType")
                }
                "ctx.mkMod($lhsGuardStr,$rhsGuardStr)"
            }
            "<" -> numericZ3 { l, r -> "ctx.mkLt($l,$r)" }
            "<=" -> numericZ3 { l, r -> "ctx.mkLe($l,$r)" }
            ">" -> numericZ3 { l, r -> "ctx.mkGt($l,$r)" }
            ">=" -> numericZ3 { l, r -> "ctx.mkGe($l,$r)" }
            "=" -> {
                val promoted = promoteNumeric(lhsType, rhsType)
                if (promoted is RealType) {
                    "ctx.mkEq(${asZ3Real(lhsGuardStr, lhsType)},${asZ3Real(rhsGuardStr, rhsType)})"
                } else {
                    "ctx.mkEq($lhsGuardStr,$rhsGuardStr)"
                }
            }
            "#" -> {
                val promoted = promoteNumeric(lhsType, rhsType)
                if (promoted is RealType) {
                    "ctx.mkNot(ctx.mkEq(${asZ3Real(lhsGuardStr, lhsType)},${asZ3Real(rhsGuardStr, rhsType)}))"
                } else {
                    "ctx.mkNot(ctx.mkEq($lhsGuardStr,$rhsGuardStr))"
                }
            }
            "&" -> "ctx.mkAnd($lhsGuardStr,$rhsGuardStr)"
            "|" -> "ctx.mkOr($lhsGuardStr,$rhsGuardStr)"
            "=>" -> "ctx.mkImplies($lhsGuardStr,$rhsGuardStr)"
            "+" -> {
                when {
                    lhsType is IntType && rhsType is IntType -> "ctx.mkAdd($lhsGuardStr,$rhsGuardStr)"
                    promoteNumeric(lhsType, rhsType) is RealType ->
                        "ctx.mkAdd(${asZ3Real(lhsGuardStr, lhsType)},${asZ3Real(rhsGuardStr, rhsType)})"
                    lhsType is ListType && rhsType is ListType ->
                        "ctx.mkSeqConcatAny($lhsGuardStr, $rhsGuardStr)"
                    lhsType is SetType && rhsType is SetType -> {
                        val setVal = lhsType.toCodegenTypeVal()
                        val meta = "$setVal.cellMetadata(ctx)"
                        "run { val __l = $lhsGuardStr; val __r = $rhsGuardStr; " +
                            "val __la = setCellArrExpr(ctx, __l, $meta.arrAccessor); " +
                            "val __ra = setCellArrExpr(ctx, __r, $meta.arrAccessor); " +
                            "val __arr = ctx.mkSetUnionAny(__la, __ra); " +
                            "val __sz = ctx.mkAdd(setCellSizeExpr(ctx, __l, $meta.sizeAccessor), setCellSizeExpr(ctx, __r, $meta.sizeAccessor)); " +
                            "setMkCellExpr(ctx, $meta.constructorDecl, __arr, __sz) }"
                    }
                    lhsType is StringType || rhsType is StringType -> {
                        // Elide empty-string concat identity (int/other → string coerce via `+ ""`).
                        when {
                            isEmptyStringLiteral(lhsOperand) && isEmptyStringLiteral(rhsOperand) ->
                                "ctx.mkString(\"\")"
                            isEmptyStringLiteral(lhsOperand) -> rhsGuardStr
                            isEmptyStringLiteral(rhsOperand) -> lhsGuardStr
                            else -> "ctx.mkConcat($lhsGuardStr,$rhsGuardStr)"
                        }
                    }
                    else -> throw RuntimeException("Cannot add types: $lhsType and $rhsType")
                }
            }
            "-" -> {
                when {
                    lhsType is SetType && rhsType is SetType -> {
                        val setVal = lhsType.toCodegenTypeVal()
                        val meta = "$setVal.cellMetadata(ctx)"
                        "run { val __l = $lhsGuardStr; val __r = $rhsGuardStr; " +
                            "val __la = setCellArrExpr(ctx, __l, $meta.arrAccessor); " +
                            "val __ra = setCellArrExpr(ctx, __r, $meta.arrAccessor); " +
                            "val __arr = ctx.mkSetDifferenceAny(__la, __ra); " +
                            "val __sz = ctx.mkSub(setCellSizeExpr(ctx, __l, $meta.sizeAccessor), setCellSizeExpr(ctx, __r, $meta.sizeAccessor)); " +
                            "setMkCellExpr(ctx, $meta.constructorDecl, __arr, __sz) }"
                    }
                    else -> numericZ3 { l, r -> "ctx.mkSub($l,$r)" }
                }
            }
            "in" -> when (rhsType) {
                is ListType -> "ctx.mkListMemberAny($lhsGuardStr, $rhsGuardStr)"
                is SetType -> "ctx.mkSetMemberAny($lhsGuardStr, setCellArrExpr(ctx, $rhsGuardStr, ${rhsType.toCodegenTypeVal()}.cellMetadata(ctx).arrAccessor))"
                is MapType -> {
                    val mapVal = rhsType.toCodegenTypeVal()
                    "ctx.mkSetMemberAny($lhsGuardStr, mapCellKeysExpr(ctx, $rhsGuardStr, $mapVal.cellMetadata(ctx).keysAccessor))"
                }
                else -> throw RuntimeException("Cannot apply \"in\" to type $rhsType")
            }
            "~in" -> {
                val member = when (rhsType) {
                    is ListType -> "ctx.mkListMemberAny($lhsGuardStr, $rhsGuardStr)"
                    is SetType -> "ctx.mkSetMemberAny($lhsGuardStr, setCellArrExpr(ctx, $rhsGuardStr, ${rhsType.toCodegenTypeVal()}.cellMetadata(ctx).arrAccessor))"
                    is MapType -> {
                        val mapVal = rhsType.toCodegenTypeVal()
                        "ctx.mkSetMemberAny($lhsGuardStr, mapCellKeysExpr(ctx, $rhsGuardStr, $mapVal.cellMetadata(ctx).keysAccessor))"
                    }
                    else -> throw RuntimeException("Cannot apply \"~in\" to type $rhsType")
                }
                "ctx.mkNot($member)"
            }
            else -> throw RuntimeException("Invalid binary op: $op")
        }
    }
    override fun toTransitString(symbolTypes: Map<String, Type>, argSymbols: Set<String>): String {
        val lhs = lhsOperand.toTransitString(symbolTypes, argSymbols)
        val rhs = rhsOperand.toTransitString(symbolTypes, argSymbols)
        val lhsType = typeForTransit(lhsOperand, symbolTypes)
        val rhsType = typeForTransit(rhsOperand, symbolTypes)

        fun numericTransit(kotlinOp: String): String {
            if (lhsType != null && rhsType != null && promoteNumeric(lhsType, rhsType) is RealType) {
                return "(${asKotlinDouble(lhs, lhsType)} $kotlinOp ${asKotlinDouble(rhs, rhsType)})"
            }
            return "($lhs $kotlinOp $rhs)"
        }

        return when (op) {
            "=" -> {
                if (lhsType != null && rhsType != null && promoteNumeric(lhsType, rhsType) is RealType) {
                    "(${asKotlinDouble(lhs, lhsType)} == ${asKotlinDouble(rhs, rhsType)})"
                } else {
                    "($lhs == $rhs)"
                }
            }
            "#" -> {
                if (lhsType != null && rhsType != null && promoteNumeric(lhsType, rhsType) is RealType) {
                    "(${asKotlinDouble(lhs, lhsType)} != ${asKotlinDouble(rhs, rhsType)})"
                } else {
                    "($lhs != $rhs)"
                }
            }
            "&" -> "($lhs && $rhs)"
            "|" -> "($lhs || $rhs)"
            "=>" -> "(!($lhs) || $rhs)"
            "+" -> {
                when {
                    lhsType is StringType || rhsType is StringType -> {
                        val lhsStr = if (lhsType is StringType) lhs else "($lhs).toString()"
                        val rhsStr = if (rhsType is StringType) rhs else "($rhs).toString()"
                        "($lhsStr + $rhsStr)"
                    }
                    lhsType != null && rhsType != null && promoteNumeric(lhsType, rhsType) is RealType ->
                        "(${asKotlinDouble(lhs, lhsType)} + ${asKotlinDouble(rhs, rhsType)})"
                    else -> "($lhs + $rhs)"
                }
            }
            "*" -> numericTransit("*")
            "/" -> numericTransit("/")
            "%" -> "($lhs % $rhs)"
            "-" -> {
                when {
                    lhsType is SetType && rhsType is SetType -> "($lhs - $rhs)"
                    else -> numericTransit("-")
                }
            }
            "in" -> "($lhs in $rhs)"
            "~in" -> "($lhs !in $rhs)"
            "<" -> numericTransit("<")
            "<=" -> numericTransit("<=")
            ">" -> numericTransit(">")
            ">=" -> numericTransit(">=")
            else -> "($lhs $op $rhs)"
        }
    }
    override fun inferType(symbolEnv: Map<String, Type>): Type {
        val lhsType = lhsOperand.getType()
        val rhsType = rhsOperand.getType()
        return when (op) {
            "=" -> boolType
            "#" -> boolType
            "*" -> promoteNumeric(lhsType, rhsType)
                ?: throw RuntimeException("Cannot apply \"*\" to types $lhsType and $rhsType")
            "/" -> promoteNumeric(lhsType, rhsType)
                ?: throw RuntimeException("Cannot apply \"/\" to types $lhsType and $rhsType")
            "%" -> {
                if (lhsType !is IntType || rhsType !is IntType) {
                    throw RuntimeException("Cannot apply \"%\" to types $lhsType and $rhsType")
                }
                intType
            }
            "<", "<=", ">", ">=" -> {
                promoteNumeric(lhsType, rhsType)
                    ?: throw RuntimeException("Cannot apply \"$op\" to types $lhsType and $rhsType")
                boolType
            }
            "&", "|", "=>" -> {
                if (lhsType !is BoolType || rhsType !is BoolType) {
                    val hint = if (op == "=>") {
                        " (\"=>\" is Boolean implication; for map entries use \"to\", e.g. mapOf(\"k\" to v))"
                    } else {
                        ""
                    }
                    throw RuntimeException("Cannot apply \"$op\" to types $lhsType and $rhsType$hint")
                }
                boolType
            }
            "+" -> {
                when {
                    lhsType is IntType && rhsType is IntType -> intType
                    promoteNumeric(lhsType, rhsType) is RealType -> realType
                    lhsType is ListType && rhsType is ListType && lhsType == rhsType -> lhsType
                    lhsType is SetType && rhsType is SetType && lhsType == rhsType -> lhsType
                    lhsType is StringType || rhsType is StringType -> stringType
                    else -> throw RuntimeException("Cannot add types: $lhsType and $rhsType")
                }
            }
            "-" -> {
                when {
                    lhsType is SetType && rhsType is SetType && lhsType == rhsType -> lhsType
                    else -> promoteNumeric(lhsType, rhsType)
                        ?: throw RuntimeException("Cannot apply \"-\" to types $lhsType and $rhsType")
                }
            }
            "in", "~in" -> boolType
            else -> throw RuntimeException("Invalid binary op: $op")
        }
    }
    override fun toString(): String {
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
    override fun toZ3GuardString(symbolTypes : Map<String,Type>, argSymbols : Set<String>, forceString : Boolean): String {
        val condGuardStr = condExpr.toZ3GuardString(symbolTypes, argSymbols)
        val thenGuardStr = thenExpr.toZ3GuardString(symbolTypes, argSymbols, forceString)
        val elseGuardStr = elseExpr.toZ3GuardString(symbolTypes, argSymbols, forceString)
        return mkIteGuardString(condGuardStr, thenGuardStr, elseGuardStr, getType(), forceString)
    }
    override fun toTransitString(symbolTypes: Map<String, Type>, argSymbols: Set<String>): String {
        val condTransitStr = condExpr.toTransitString(symbolTypes,argSymbols)
        val thenTransitStr = thenExpr.toTransitString(symbolTypes,argSymbols)
        val elseTransitStr = elseExpr.toTransitString(symbolTypes,argSymbols)
        return "if ($condTransitStr) {$thenTransitStr} else {$elseTransitStr}"
    }
    override fun inferType(symbolEnv: Map<String, Type>): Type {
        return thenExpr.getType()
    }
    override fun toString(): String {
        return "if ($condExpr) {$thenExpr} else {$elseExpr}"
    }
}

sealed interface WhenLiteral {
    data class IntLit(val value: String) : WhenLiteral
    data class RealLit(val value: String) : WhenLiteral
    data class StringLit(val value: String) : WhenLiteral
    data class BoolLit(val value: String) : WhenLiteral
}

sealed interface WhenPattern {
    data class Primitive(val literal: WhenLiteral) : WhenPattern
    data class Struct(val literal: ObjClassLiteralExprNode) : WhenPattern
}

sealed interface WhenArm {
    data class Subject(val pattern: WhenPattern, val expr: ExprNode) : WhenArm
    data class Guard(val cond: ExprNode, val expr: ExprNode) : WhenArm
    data class Else(val expr: ExprNode) : WhenArm
}

class LetExprNode(
    private val name: String,
    private val typeExpr: TypeExpr,
    private val letInitExpr: ExprNode,
    private val bodyExpr: ExprNode,
    private val loc: ProgramLoc,
    resolvedType: Type? = null,
) : ExprNode(listOf(letInitExpr, bodyExpr)) {
    private var letTypeResolution: Type? = resolvedType

    val resolvedLetType: Type
        get() = letTypeResolution
            ?: throw RuntimeException("Type not resolved for let binding \"$name\" at $loc")

    override fun programLocation() = loc
    internal fun letName(): String = name
    internal fun letTypeExpr(): TypeExpr = typeExpr
    internal fun letTypeName(): String = typeExpr.toString()
    internal fun letInitExpr(): ExprNode = letInitExpr
    internal fun bodyExpr(): ExprNode = bodyExpr
    internal fun resolvedLetTypeOrNull(): Type? = letTypeResolution

    internal fun resolveLetType(type: Type) {
        letTypeResolution = type
    }

    override fun toZ3GuardString(
        symbolTypes: Map<String, Type>,
        argSymbols: Set<String>,
        forceString: Boolean,
    ): String {
        return substituteExpr(bodyExpr, name, letInitExpr)
            .toZ3GuardString(symbolTypes, argSymbols, forceString)
    }

    override fun toTransitString(symbolTypes: Map<String, Type>, argSymbols: Set<String>): String {
        val localBind = if (name.isDiscardBinding()) {
            "__discard__let"
        } else {
            "${name.toKotlinIdent()}__let"
        }
        val initStr = letInitExpr.toTransitString(symbolTypes, argSymbols)
        val localBody = if (name.isDiscardBinding()) {
            bodyExpr
        } else {
            val replacement = SymbolValueExprNode(localBind, programLocation()).also {
                it.setInferredType(TypePassType.Inferred(resolvedLetType))
            }
            substituteExpr(bodyExpr, name, replacement)
        }
        val bodyStr = localBody.toTransitString(
            symbolTypes + (localBind to resolvedLetType),
            if (name.isDiscardBinding()) argSymbols else argSymbols - name,
        )
        return "run { val $localBind = $initStr; $bodyStr }"
    }

    override fun inferType(symbolEnv: Map<String, Type>): Type = bodyExpr.getType()

    override fun toString(): String {
        return "let ($name : $typeExpr := $letInitExpr) { $bodyExpr }"
    }
}

class WhenExprNode(
    private val subjectExpr: ExprNode?,
    private val arms: List<WhenArm>,
    private val loc: ProgramLoc,
) : ExprNode(
    (subjectExpr?.let { listOf(it) } ?: emptyList()) +
        arms.flatMap { arm ->
            when (arm) {
                is WhenArm.Subject -> when (val pattern = arm.pattern) {
                    is WhenPattern.Primitive -> listOf(arm.expr)
                    is WhenPattern.Struct -> listOf(pattern.literal) + listOf(arm.expr)
                }
                is WhenArm.Guard -> listOf(arm.cond, arm.expr)
                is WhenArm.Else -> listOf(arm.expr)
            }
        },
) {
    override fun programLocation() = loc
    internal fun subjectExpr(): ExprNode? = subjectExpr
    internal fun arms(): List<WhenArm> = arms

    override fun toZ3GuardString(
        symbolTypes: Map<String, Type>,
        argSymbols: Set<String>,
        forceString: Boolean,
    ): String {
        julay.tools.assert(!forceString, "Cannot force a when expression to a string")
        return buildNestedZ3ITE(symbolTypes, argSymbols)
    }

    override fun toTransitString(symbolTypes: Map<String, Type>, argSymbols: Set<String>): String {
        return buildNestedTransitIf(symbolTypes, argSymbols)
    }

    override fun inferType(symbolEnv: Map<String, Type>): Type {
        val firstNonElse = arms.firstOrNull { it !is WhenArm.Else }
            ?: throw RuntimeException("When expression at $loc has no non-else arms")
        return when (firstNonElse) {
            is WhenArm.Subject -> firstNonElse.expr.getType()
            is WhenArm.Guard -> firstNonElse.expr.getType()
            is WhenArm.Else -> firstNonElse.expr.getType()
        }
    }

    private fun buildNestedZ3ITE(symbolTypes: Map<String, Type>, argSymbols: Set<String>): String {
        val resultType = getType()
        val elseArm = arms.last() as WhenArm.Else
        var result = elseArm.expr.toZ3GuardString(symbolTypes, argSymbols)
        for (arm in arms.dropLast(1).reversed()) {
            val (condStr, branchStr) = when (arm) {
                is WhenArm.Subject -> subjectMatchZ3String(arm.pattern, symbolTypes, argSymbols) to
                    arm.expr.toZ3GuardString(symbolTypes, argSymbols)
                is WhenArm.Guard -> {
                    arm.cond.toZ3GuardString(symbolTypes, argSymbols) to
                        arm.expr.toZ3GuardString(symbolTypes, argSymbols)
                }
                is WhenArm.Else -> throw RuntimeException("Unexpected else arm before final position")
            }
            result = mkIteGuardString(condStr, branchStr, result, resultType, forceString = false)
        }
        return result
    }

    private fun buildNestedTransitIf(symbolTypes: Map<String, Type>, argSymbols: Set<String>): String {
        val elseArm = arms.last() as WhenArm.Else
        var result = elseArm.expr.toTransitString(symbolTypes, argSymbols)
        for (arm in arms.dropLast(1).reversed()) {
            val (condStr, branchStr) = when (arm) {
                is WhenArm.Subject -> subjectMatchTransitString(arm.pattern, symbolTypes, argSymbols) to
                    arm.expr.toTransitString(symbolTypes, argSymbols)
                is WhenArm.Guard -> {
                    arm.cond.toTransitString(symbolTypes, argSymbols) to
                        arm.expr.toTransitString(symbolTypes, argSymbols)
                }
                is WhenArm.Else -> throw RuntimeException("Unexpected else arm before final position")
            }
            result = "if ($condStr) {$branchStr} else {$result}"
        }
        return result
    }

    private fun subjectMatchZ3String(
        pattern: WhenPattern,
        symbolTypes: Map<String, Type>,
        argSymbols: Set<String>,
    ): String {
        val subject = subjectExpr ?: throw RuntimeException("Subject when at $loc has no subject expression")
        val lhsStr = subject.toZ3GuardString(symbolTypes, argSymbols)
        val rhsStr = when (pattern) {
            is WhenPattern.Primitive ->
                pattern.literal.toLiteralExprNode(subject.programLocation())
                    .toZ3GuardString(symbolTypes, argSymbols)
            is WhenPattern.Struct ->
                pattern.literal.toZ3GuardString(symbolTypes, argSymbols)
        }
        return "ctx.mkEq($lhsStr,$rhsStr)"
    }

    private fun subjectMatchTransitString(
        pattern: WhenPattern,
        symbolTypes: Map<String, Type>,
        argSymbols: Set<String>,
    ): String {
        val subject = subjectExpr ?: throw RuntimeException("Subject when at $loc has no subject expression")
        val lhsStr = subject.toTransitString(symbolTypes, argSymbols)
        val rhsStr = when (pattern) {
            is WhenPattern.Primitive ->
                pattern.literal.toLiteralExprNode(subject.programLocation())
                    .toTransitString(symbolTypes, argSymbols)
            is WhenPattern.Struct ->
                pattern.literal.toTransitString(symbolTypes, argSymbols)
        }
        return "($lhsStr == $rhsStr)"
    }

    override fun toString(): String {
        return if (subjectExpr != null) {
            val armStrs = arms.joinToString("\n") { arm -> "    $arm" }
            "when ($subjectExpr) {\n$armStrs\n}"
        } else {
            val armStrs = arms.joinToString("\n") { arm -> "    $arm" }
            "when {\n$armStrs\n}"
        }
    }
}

private fun WhenLiteral.toLiteralExprNode(loc: ProgramLoc): LiteralValueExprNode = when (this) {
    is WhenLiteral.IntLit -> LiteralValueExprNode(value, intType, loc)
    is WhenLiteral.RealLit -> LiteralValueExprNode(value, realType, loc)
    is WhenLiteral.StringLit -> LiteralValueExprNode(value, stringType, loc)
    is WhenLiteral.BoolLit -> LiteralValueExprNode(value, boolType, loc)
}

class ObjClassLiteralExprNode(
    val typeExpr: TypeExpr,
    val fieldEntries: List<Pair<String, ExprNode>>,
    private val loc: ProgramLoc,
    resolvedType: ObjClassType? = null,
) : ExprNode(fieldEntries.map { it.second }) {
    private sealed interface ObjClassLiteralResolution {
        data object Unresolved : ObjClassLiteralResolution
        data class Resolved(val structType: ObjClassType) : ObjClassLiteralResolution
    }

    private var objClassLiteralResolution: ObjClassLiteralResolution =
        if (resolvedType != null) ObjClassLiteralResolution.Resolved(resolvedType)
        else ObjClassLiteralResolution.Unresolved

    val className: String get() = typeExpr.ctorName()

    val structType: ObjClassType
        get() = when (val resolution = objClassLiteralResolution) {
            is ObjClassLiteralResolution.Resolved -> resolution.structType
            is ObjClassLiteralResolution.Unresolved ->
                throw RuntimeException("Obj literal type not resolved at $loc")
        }

    val fieldAssignments: Map<String, ExprNode> = fieldEntries.toMap()

    override fun programLocation() = loc

    internal fun resolvedStructTypeOrNull(): ObjClassType? =
        (objClassLiteralResolution as? ObjClassLiteralResolution.Resolved)?.structType

    internal fun resolveLiteralType(type: ObjClassType) {
        objClassLiteralResolution = ObjClassLiteralResolution.Resolved(type)
    }

    override fun toZ3GuardString(
        symbolTypes: Map<String, Type>,
        argSymbols: Set<String>,
        forceString: Boolean,
    ): String {
        val fieldExprs = structType.fields.map { field ->
            fieldAssignments.getValue(field.name).toZ3GuardString(symbolTypes, argSymbols)
        }
        return structType.literalToZ3Codegen(fieldExprs)
    }

    override fun toTransitString(symbolTypes: Map<String, Type>, argSymbols: Set<String>): String {
        val fieldExprs = structType.fields.map { field ->
            fieldAssignments.getValue(field.name).toTransitString(symbolTypes, argSymbols)
        }
        return structType.literalToTransit(fieldExprs)
    }

    override fun inferType(symbolEnv: Map<String, Type>): Type = structType

    override fun toString(): String {
        val fields = fieldEntries.joinToString(", ") { (name, expr) -> "$name := $expr" }
        return "$typeExpr { $fields }"
    }
}

class ListLiteralExprNode(
    val elements: List<ExprNode>,
    private val loc: ProgramLoc,
    resolvedType: ListType? = null,
    val typeArgs: List<TypeExpr> = emptyList(),
) : ExprNode(elements) {
    private var listType: ListType? = resolvedType

    override fun programLocation() = loc

    internal fun resolveListType(type: ListType) {
        listType = type
    }

    internal fun resolvedListTypeOrNull(): ListType? = listType

    override fun toZ3GuardString(
        symbolTypes: Map<String, Type>,
        argSymbols: Set<String>,
        forceString: Boolean,
    ): String {
        if (forceString) {
            return "ctx.mkString(${toTransitString(symbolTypes, argSymbols)}.toString())"
        }
        val ty = listType ?: throw RuntimeException("List literal type not resolved at $loc")
        if (elements.isEmpty()) {
            return "ctx.mkEmptySeq(${ty.toCodegenTypeVal()}.sort(ctx))"
        }
        val units = elements.map { elem ->
            "ctx.mkUnit(${elem.toZ3GuardString(symbolTypes, argSymbols)})"
        }
        return if (units.size == 1) {
            units[0]
        } else {
            "ctx.mkConcat(${units.joinToString(", ")})"
        }
    }

    override fun toTransitString(symbolTypes: Map<String, Type>, argSymbols: Set<String>): String {
        if (elements.isEmpty()) {
            val elem = listType?.elementType?.toKotlinTypeString()
            return if (elem != null) "emptyList<$elem>()" else "emptyList()"
        }
        val elems = elements.joinToString(", ") { it.toTransitString(symbolTypes, argSymbols) }
        return "listOf($elems)"
    }

    override fun inferType(symbolEnv: Map<String, Type>): Type {
        return listType ?: throw RuntimeException("List literal type not resolved at $loc")
    }

    override fun toString(): String {
        val ann = if (typeArgs.isEmpty()) "" else "<${typeArgs.joinToString(", ")}>"
        return if (elements.isEmpty()) "listOf$ann()"
        else elements.joinToString(", ", prefix = "listOf$ann(", postfix = ")")
    }
}

class MapLiteralExprNode(
    val entries: List<Pair<ExprNode, ExprNode>>,
    private val loc: ProgramLoc,
    resolvedType: MapType? = null,
    val typeArgs: List<TypeExpr> = emptyList(),
) : ExprNode(entries.flatMap { listOf(it.first, it.second) }) {
    private var mapType: MapType? = resolvedType

    override fun programLocation() = loc

    internal fun resolveMapType(type: MapType) {
        mapType = type
    }

    internal fun resolvedMapTypeOrNull(): MapType? = mapType

    override fun toZ3GuardString(
        symbolTypes: Map<String, Type>,
        argSymbols: Set<String>,
        forceString: Boolean,
    ): String {
        if (forceString) {
            return "ctx.mkString(${toTransitString(symbolTypes, argSymbols)}.toString())"
        }
        val ty = mapType ?: throw RuntimeException("Map literal type not resolved at $loc")
        val mapVal = ty.toCodegenTypeVal()
        val meta = "$mapVal.cellMetadata(ctx)"
        val domain = "${mapVal}.cellMetadata(ctx).domainSort"
        if (entries.isEmpty()) {
            return "mapMkCellExpr(ctx, $meta.constructorDecl, ctx.mkConstArray($domain, ctx.mkInt(0)), ctx.mkEmptySet($domain), ctx.mkInt(0))"
        }
        var arr = "ctx.mkConstArray($domain, ctx.mkInt(0))"
        var keys = "ctx.mkEmptySet($domain)"
        var size = "ctx.mkInt(0)"
        for ((k, v) in entries) {
            val keyStr = k.toZ3GuardString(symbolTypes, argSymbols)
            val valStr = v.toZ3GuardString(symbolTypes, argSymbols)
            val wasMember = "ctx.mkSetMemberAny($keyStr, $keys)"
            arr = "mapStoreExpr(ctx, $arr, $keyStr, $valStr)"
            keys = "mapSetAddExpr(ctx, $keys, $keyStr)"
            size = "ctx.mkITE($wasMember, $size, ctx.mkAdd($size, ctx.mkInt(1)))"
        }
        return "mapMkCellExpr(ctx, $meta.constructorDecl, $arr, $keys, $size)"
    }

    override fun toTransitString(symbolTypes: Map<String, Type>, argSymbols: Set<String>): String {
        if (entries.isEmpty()) {
            val kt = mapType?.keyType?.toKotlinTypeString()
            val vt = mapType?.valueType?.toKotlinTypeString()
            return if (kt != null && vt != null) "emptyMap<$kt, $vt>()" else "emptyMap()"
        }
        val pairs = entries.joinToString(", ") { (k, v) ->
            "${k.toTransitString(symbolTypes, argSymbols)} to ${v.toTransitString(symbolTypes, argSymbols)}"
        }
        return "mapOf($pairs)"
    }

    override fun inferType(symbolEnv: Map<String, Type>): Type {
        return mapType ?: throw RuntimeException("Map literal type not resolved at $loc")
    }

    override fun toString(): String {
        val ann = if (typeArgs.isEmpty()) "" else "<${typeArgs.joinToString(", ")}>"
        return if (entries.isEmpty()) "mapOf$ann()"
        else entries.joinToString(", ", prefix = "mapOf$ann(", postfix = ")") { (k, v) -> "$k to $v" }
    }
}

class SetLiteralExprNode(
    val elements: List<ExprNode>,
    private val loc: ProgramLoc,
    resolvedType: SetType? = null,
    val typeArgs: List<TypeExpr> = emptyList(),
) : ExprNode(elements) {
    private var setType: SetType? = resolvedType

    override fun programLocation() = loc

    internal fun resolveSetType(type: SetType) {
        setType = type
    }

    internal fun resolvedSetTypeOrNull(): SetType? = setType

    override fun toZ3GuardString(
        symbolTypes: Map<String, Type>,
        argSymbols: Set<String>,
        forceString: Boolean,
    ): String {
        if (forceString) {
            return "ctx.mkString(${toTransitString(symbolTypes, argSymbols)}.toString())"
        }
        val ty = setType ?: throw RuntimeException("Set literal type not resolved at $loc")
        val setVal = ty.toCodegenTypeVal()
        val meta = "$setVal.cellMetadata(ctx)"
        val domain = "$setVal.cellMetadata(ctx).domainSort"
        if (elements.isEmpty()) {
            return "setMkCellExpr(ctx, $meta.constructorDecl, ctx.mkEmptySet($domain), ctx.mkInt(0))"
        }
        var arr = "ctx.mkEmptySet($domain)"
        for (elem in elements) {
            val elemStr = elem.toZ3GuardString(symbolTypes, argSymbols)
            arr = "ctx.mkSetAddAny($arr, $elemStr)"
        }
        return "setMkCellExpr(ctx, $meta.constructorDecl, $arr, ctx.mkInt(${elements.size}))"
    }

    override fun toTransitString(symbolTypes: Map<String, Type>, argSymbols: Set<String>): String {
        if (elements.isEmpty()) {
            val elem = setType?.elementType?.toKotlinTypeString()
            return if (elem != null) "emptySet<$elem>()" else "emptySet()"
        }
        val elems = elements.joinToString(", ") { it.toTransitString(symbolTypes, argSymbols) }
        return "setOf($elems)"
    }

    override fun inferType(symbolEnv: Map<String, Type>): Type {
        return setType ?: throw RuntimeException("Set literal type not resolved at $loc")
    }

    override fun toString(): String {
        val ann = if (typeArgs.isEmpty()) "" else "<${typeArgs.joinToString(", ")}>"
        return if (elements.isEmpty()) "setOf$ann()"
        else elements.joinToString(", ", prefix = "setOf$ann(", postfix = ")")
    }
}

class IndexExprNode(
    val base: ExprNode,
    val index: ExprNode,
    private val loc: ProgramLoc,
) : ExprNode(listOf(base, index)) {
    override fun programLocation() = loc

    override fun toZ3GuardString(
        symbolTypes: Map<String, Type>,
        argSymbols: Set<String>,
        forceString: Boolean,
    ): String {
        val baseStr = base.toZ3GuardString(symbolTypes, argSymbols)
        val indexStr = index.toZ3GuardString(symbolTypes, argSymbols)
        return when (val baseType = base.getType()) {
            is ListType -> {
                // Julay lists are 1-based; Z3 Seq is 0-based.
                val nth = "ctx.mkSeqNthAny($baseStr, ctx.mkSub($indexStr, ctx.mkInt(1)))"
                castFieldZ3(nth, baseType.elementType, forceString)
            }
            is MapType -> {
                val mapVal = baseType.toCodegenTypeVal()
                val meta = "$mapVal.cellMetadata(ctx)"
                val selected = "run { val __cell = $baseStr; val __keys = mapCellKeysExpr(ctx, __cell, $meta.keysAccessor); " +
                    "val __arr = mapCellArrExpr(ctx, __cell, $meta.arrAccessor); " +
                    "ctx.mkITE(ctx.mkSetMemberAny($indexStr, __keys), mapSelectExpr(ctx, __arr, $indexStr), " +
                    "${baseType.valueType.toCodegenTypeVal()}.toZ3Expr(Value(0, ${baseType.valueType.toCodegenTypeVal()}), ctx)) }"
                castFieldZ3(selected, baseType.valueType, forceString)
            }
            else -> throw RuntimeException("Cannot index type $baseType at $loc")
        }
    }

    override fun toTransitString(symbolTypes: Map<String, Type>, argSymbols: Set<String>): String {
        val baseStr = base.toTransitString(symbolTypes, argSymbols)
        val indexStr = index.toTransitString(symbolTypes, argSymbols)
        return when (typeForTransit(base, symbolTypes)) {
            is MapType -> "($baseStr.getValue($indexStr))"
            // Julay lists are 1-based; Kotlin List is 0-based.
            is ListType -> "$baseStr[($indexStr) - 1]"
            else -> "$baseStr[($indexStr) - 1]"
        }
    }

    override fun inferType(symbolEnv: Map<String, Type>): Type {
        return when (val baseType = base.getType()) {
            is ListType -> baseType.elementType
            is MapType -> baseType.valueType
            else -> throw RuntimeException("Cannot index type $baseType at $loc")
        }
    }

    override fun toString(): String = "$base[$index]"
}

class LiteralValueExprNode(
    private val value : String,
    private val type : Type,
    private val loc : ProgramLoc
) : ExprNode(listOf()) {
    init {
        // Literals know their type at construction; keep it available for transit codegen
        // even when typeForTransit falls back (e.g. after let-subst recreates unbound nodes).
        setInferredType(TypePassType.Inferred(type))
    }
    override fun programLocation() = loc
    internal fun literalText() = value
    internal fun isTrueLiteral() = type is BoolType && value == "true"
    override fun toZ3GuardString(symbolTypes : Map<String,Type>, argSymbols : Set<String>, forceString : Boolean): String {
        if (forceString) {
            return "ctx.mkString(\"$value\")"
        }
        return when (type) {
            is BoolType -> "ctx.mkBool($value)"
            is IntType -> "ctx.mkInt($value)"
            is RealType -> "ctx.mkReal(\"$value\")"
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

/**
 * Explicit proc-state access: `this.x` / `this.obj.f`.
 * Always refers to enclosing leaf state, never action args of the same name.
 */
class ThisAccessExprNode(
    val fieldPath: List<String>,
    private val loc: ProgramLoc,
    resolvedLeafType: Type? = null,
    resolvedRelPath: String? = null,
    resolvedRootType: Type? = null,
) : ExprNode(listOf()) {
    private sealed interface ThisAccessResolution {
        data object Unresolved : ThisAccessResolution
        data class Resolved(val rootType: Type, val leafType: Type, val relPath: String) : ThisAccessResolution
    }

    private var resolution: ThisAccessResolution =
        if (resolvedLeafType != null && resolvedRelPath != null && resolvedRootType != null) {
            ThisAccessResolution.Resolved(resolvedRootType, resolvedLeafType, resolvedRelPath)
        } else {
            ThisAccessResolution.Unresolved
        }

    init {
        require(fieldPath.isNotEmpty()) { "this access requires at least one field at $loc" }
        if (resolvedLeafType != null) {
            setInferredType(TypePassType.Inferred(resolvedLeafType))
        }
    }

    override fun programLocation() = loc

    fun stateVarName(): String = fieldPath[0]

    fun nestedFieldPath(): List<String> = fieldPath.drop(1)

    internal fun resolveThisAccess(rootType: Type, leafType: Type, relPath: String) {
        resolution = ThisAccessResolution.Resolved(rootType, leafType, relPath)
        setInferredType(TypePassType.Inferred(leafType))
    }

    internal fun resolvedRootTypeOrNull(): Type? =
        (resolution as? ThisAccessResolution.Resolved)?.rootType

    internal fun resolvedLeafTypeOrNull(): Type? =
        (resolution as? ThisAccessResolution.Resolved)?.leafType

    internal fun resolvedRelPathOrNull(): String? =
        (resolution as? ThisAccessResolution.Resolved)?.relPath

    /** State-only view: root is never treated as an action arg. */
    private fun stateForced(
        symbolTypes: Map<String, Type>,
        argSymbols: Set<String>,
    ): Pair<Map<String, Type>, Set<String>> {
        val root = stateVarName()
        val rootType = resolvedRootTypeOrNull() ?: symbolTypes[root]
            ?: throw RuntimeException("Unresolved this.${fieldPath.joinToString(".")} at $loc")
        return (symbolTypes + (root to rootType)) to (argSymbols - root)
    }

    private fun asFieldOrSymbol(): ExprNode {
        val root = stateVarName()
        val rest = nestedFieldPath()
        val resolved = resolution as? ThisAccessResolution.Resolved
        return if (rest.isEmpty()) {
            SymbolValueExprNode(root, loc).also {
                if (resolved != null) {
                    it.setInferredType(TypePassType.Inferred(resolved.leafType))
                }
            }
        } else {
            FieldAccessExprNode(
                root,
                rest,
                loc,
                resolved?.leafType,
                resolved?.relPath,
            )
        }
    }

    override fun toZ3GuardString(
        symbolTypes: Map<String, Type>,
        argSymbols: Set<String>,
        forceString: Boolean,
    ): String {
        val (types, args) = stateForced(symbolTypes, argSymbols)
        return asFieldOrSymbol().toZ3GuardString(types, args, forceString)
    }

    override fun toTransitString(symbolTypes: Map<String, Type>, argSymbols: Set<String>): String {
        val (types, args) = stateForced(symbolTypes, argSymbols)
        val inner = asFieldOrSymbol().toTransitString(types, args)
        // Qualify with this. so Kotlin field wins if a same-named local ever appears.
        return if (inner.startsWith("this.")) inner else "this.$inner"
    }

    override fun inferType(symbolEnv: Map<String, Type>): Type {
        val resolved = resolution as? ThisAccessResolution.Resolved
            ?: throw RuntimeException("Unresolved this.${fieldPath.joinToString(".")} at $loc")
        return resolved.leafType
    }

    override fun toString(): String = "this.${fieldPath.joinToString(".")}"
}

class FieldAccessExprNode(
    val baseSymbol: String,
    val fieldPath: List<String>,
    private val loc: ProgramLoc,
    resolvedLeafType: Type? = null,
    resolvedRelPath: String? = null,
) : ExprNode(listOf()) {
    private sealed interface FieldAccessResolution {
        data object Unresolved : FieldAccessResolution
        data class Resolved(val leafType: Type, val relPath: String) : FieldAccessResolution
    }

    private var fieldResolution: FieldAccessResolution =
        if (resolvedLeafType != null && resolvedRelPath != null) {
            FieldAccessResolution.Resolved(resolvedLeafType, resolvedRelPath)
        } else {
            FieldAccessResolution.Unresolved
        }

    init {
        if (resolvedLeafType != null) {
            setInferredType(TypePassType.Inferred(resolvedLeafType))
        }
    }

    override fun programLocation() = loc

    internal fun resolveFieldAccess(leafType: Type, relPath: String) {
        fieldResolution = FieldAccessResolution.Resolved(leafType, relPath)
        setInferredType(TypePassType.Inferred(leafType))
    }

    internal fun resolvedLeafTypeOrNull(): Type? =
        (fieldResolution as? FieldAccessResolution.Resolved)?.leafType

    internal fun resolvedRelPathOrNull(): String? =
        (fieldResolution as? FieldAccessResolution.Resolved)?.relPath

    internal fun withBaseSymbol(newBase: String): FieldAccessExprNode {
        val resolved = fieldResolution as? FieldAccessResolution.Resolved
        return FieldAccessExprNode(
            newBase,
            fieldPath,
            loc,
            resolved?.leafType,
            resolved?.relPath,
        )
    }

    override fun toZ3GuardString(
        symbolTypes: Map<String, Type>,
        argSymbols: Set<String>,
        forceString: Boolean,
    ): String {
        val resolution = fieldResolution as FieldAccessResolution.Resolved
        val rootType = symbolTypes[baseSymbol]
        if (rootType is ListType || rootType is SetType || rootType is MapType) {
            return collectionPropToZ3(symbolTypes, argSymbols, forceString, resolution.leafType)
        }
        if (forceString && (resolution.leafType is ListType || resolution.leafType is ObjClassType)) {
            if (baseSymbol in argSymbols) {
                throw RuntimeException("Cannot convert symbolic ${resolution.leafType} to string")
            }
            return "ctx.mkString((${toTransitString(symbolTypes, argSymbols)}).toString())"
        }
        val baseType = symbolTypes.getValue(baseSymbol) as ObjClassType
        val baseZ3 = recordZ3Expr(baseSymbol, baseType, argSymbols)
        val fieldZ3 = ObjClassType.fieldAccessZ3Codegen(baseType, baseZ3, fieldPath)
        return castFieldZ3(fieldZ3, resolution.leafType, forceString)
    }

    override fun toTransitString(symbolTypes: Map<String, Type>, argSymbols: Set<String>): String {
        val rootType = symbolTypes[baseSymbol]
        if (rootType is ListType || rootType is SetType || rootType is MapType) {
            return collectionPropToTransit(symbolTypes, argSymbols)
        }
        return ObjClassType.fieldAccessTransitString(baseSymbol, fieldPath, symbolTypes, argSymbols)
    }

    private fun collectionPropToTransit(symbolTypes: Map<String, Type>, argSymbols: Set<String>): String {
        var expr = SymbolValueExprNode(baseSymbol, loc).toTransitString(symbolTypes, argSymbols)
        for (seg in fieldPath) {
            expr = when (seg) {
                "keys" -> "($expr).keys"
                "length" -> "($expr).size"
                else -> throw RuntimeException("Unknown collection property \"$seg\" at $loc")
            }
        }
        return expr
    }

    private fun collectionPropToZ3(
        symbolTypes: Map<String, Type>,
        argSymbols: Set<String>,
        forceString: Boolean,
        leafType: Type,
    ): String {
        if (baseSymbol in argSymbols) {
            throw RuntimeException("Collection property access on symbolic argument \"$baseSymbol\" is not supported in guards at $loc")
        }
        val kotlinExpr = collectionPropToTransit(symbolTypes, argSymbols)
        return embedKotlinValueAsZ3(kotlinExpr, leafType, forceString, loc)
    }

    private fun recordZ3Expr(baseSymbol: String, baseType: ObjClassType, argSymbols: Set<String>): String {
        val typeVal = objClassTypeValName(baseType.name)
        return if (baseSymbol in argSymbols) {
            ObjClassType.z3ConstString(baseSymbol, typeVal)
        } else {
            ObjClassType.kotlinObjClassToZ3String(baseType.name, baseSymbol)
        }
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

/** Postfix `.field` on an arbitrary expression (e.g. `xs[i].f` or `Proc[i].sv`). */
class MemberAccessExprNode(
    val baseExpr: ExprNode,
    val fieldName: String,
    private val loc: ProgramLoc,
) : ExprNode(listOf(baseExpr)) {
    override fun programLocation() = loc

    override fun toZ3GuardString(
        symbolTypes: Map<String, Type>,
        argSymbols: Set<String>,
        forceString: Boolean,
    ): String {
        val leafType = getType()
        val baseType = baseExpr.getType()
        if (baseType is ListType || baseType is SetType || baseType is MapType) {
            val kotlinExpr = toTransitString(symbolTypes, argSymbols)
            if (exprReferencesAnyArg(baseExpr, argSymbols)) {
                throw RuntimeException("Collection property \"$fieldName\" on symbolic value is not supported in guards at $loc")
            }
            return embedKotlinValueAsZ3(kotlinExpr, leafType, forceString, loc)
        }
        if (forceString && (leafType is ListType || leafType is ObjClassType)) {
            return "ctx.mkString((${toTransitString(symbolTypes, argSymbols)}).toString())"
        }
        val objType = baseType as ObjClassType
        val baseZ3 = baseExpr.toZ3GuardString(symbolTypes, argSymbols)
        val fieldZ3 = ObjClassType.fieldAccessZ3Codegen(objType, baseZ3, listOf(fieldName))
        return castFieldZ3(fieldZ3, leafType, forceString)
    }

    override fun toTransitString(symbolTypes: Map<String, Type>, argSymbols: Set<String>): String {
        val base = baseExpr.toTransitString(symbolTypes, argSymbols)
        return when (fieldName) {
            "keys" -> "($base).keys"
            "length" -> "($base).size"
            else -> "($base).$fieldName"
        }
    }

    override fun inferType(symbolEnv: Map<String, Type>): Type {
        return try {
            getType()
        } catch (_: RuntimeException) {
            throw RuntimeException("Member access not typed at $loc")
        }
    }

    override fun toString(): String = "$baseExpr.$fieldName"
}

/**
 * Inline lambda used only as a higher-order function argument: `x -> e` or `(acc, x) -> e`.
 * Not a first-class value; typing happens in the enclosing HOF call.
 */
class LambdaExprNode(
    val params: List<String>,
    val body: ExprNode,
    private val loc: ProgramLoc,
) : ExprNode(listOf(body)) {
    override fun programLocation() = loc

    override fun toZ3GuardString(
        symbolTypes: Map<String, Type>,
        argSymbols: Set<String>,
        forceString: Boolean,
    ): String {
        throw RuntimeException("Lambda cannot be used as a standalone expression in guards at $loc")
    }

    override fun toTransitString(symbolTypes: Map<String, Type>, argSymbols: Set<String>): String {
        throw RuntimeException("Lambda cannot be used as a standalone expression at $loc")
    }

    override fun inferType(symbolEnv: Map<String, Type>): Type {
        throw RuntimeException("Lambda type is determined by the enclosing higher-order call at $loc")
    }

    override fun toString(): String = when (params.size) {
        1 -> "${params[0]} -> $body"
        else -> "(${params.joinToString(", ")}) -> $body"
    }
}

/** Intrinsic collection method call or api-qualified procfun call: `xs.filter(...)` / `RpcOut.fn(...)`. */
class MethodCallExprNode(
    val baseExpr: ExprNode,
    val methodName: String,
    val args: List<ExprNode>,
    private val loc: ProgramLoc,
) : ExprNode(listOf(baseExpr) + args) {
    private var hofBody: ExprNode? = null
    private var hofParamNames: List<String>? = null
    private var hofParamTypes: List<Type>? = null
    private var resolvedProcFun: ProcFunNode? = null
    private var resolvedApiName: String? = null

    override fun programLocation() = loc

    internal fun resolveHof(body: ExprNode, paramNames: List<String>, paramTypes: List<Type>) {
        hofBody = body
        hofParamNames = paramNames
        hofParamTypes = paramTypes
        resolvedProcFun = null
        resolvedApiName = null
    }

    internal fun resolveApiProcFun(apiName: String, procFun: ProcFunNode) {
        resolvedApiName = apiName
        resolvedProcFun = procFun
        hofBody = null
        hofParamNames = null
        hofParamTypes = null
    }

    internal fun hofBodyOrNull(): ExprNode? = hofBody
    internal fun hofParamNamesOrNull(): List<String>? = hofParamNames
    internal fun hofParamTypesOrNull(): List<Type>? = hofParamTypes
    internal fun resolvedProcFunOrNull(): ProcFunNode? = resolvedProcFun
    internal fun resolvedApiNameOrNull(): String? = resolvedApiName

    override fun toZ3GuardString(
        symbolTypes: Map<String, Type>,
        argSymbols: Set<String>,
        forceString: Boolean,
    ): String {
        if (resolvedProcFun != null) {
            throw RuntimeException(
                "Api procfun call \"$resolvedApiName.$methodName\" cannot be used in guards at $loc",
            )
        }
        if (exprReferencesAnyArg(this, argSymbols)) {
            throw RuntimeException(
                "Method \"$methodName\" cannot be used in guards when it depends on action arguments at $loc",
            )
        }
        val kotlinExpr = toTransitString(symbolTypes, argSymbols)
        return embedKotlinValueAsZ3(kotlinExpr, getType(), forceString, loc)
    }

    override fun toTransitString(symbolTypes: Map<String, Type>, argSymbols: Set<String>): String {
        resolvedProcFun?.let { pf ->
            val argStrs = args.map { it.toTransitString(symbolTypes, argSymbols) }
            val argsList = if (argStrs.isEmpty()) {
                "emptyList()"
            } else {
                "listOf(${argStrs.joinToString(", ")})"
            }
            val retTy = pf.returnType.toKotlinTypeString()
            return "(hostProc.invokeProcFun(\"${pf.procFunName()}\", $argsList) as $retTy)"
        }
        val base = baseExpr.toTransitString(symbolTypes, argSymbols)
        when (methodName) {
            "toSet" -> return "$base.toSet()"
            "toList" -> return "$base.toList()"
        }
        val body = hofBody ?: throw RuntimeException("HOF body not resolved for \"$methodName\" at $loc")
        val paramNames = hofParamNames ?: throw RuntimeException("HOF params not resolved for \"$methodName\" at $loc")
        val paramTypes = hofParamTypes ?: throw RuntimeException("HOF param types not resolved for \"$methodName\" at $loc")
        val extendedTypes = symbolTypes + paramNames.zip(paramTypes).toMap()
        return when (methodName) {
            "filter" -> {
                val p = paramNames.single()
                val bodyStr = body.toTransitString(extendedTypes, argSymbols)
                when (baseExpr.getType()) {
                    is ListType -> "$base.filter { $p -> $bodyStr }"
                    is SetType -> "$base.filter { $p -> $bodyStr }.toSet()"
                    else -> throw RuntimeException("filter expected List or Set at $loc")
                }
            }
            "map" -> {
                val p = paramNames.single()
                val bodyStr = body.toTransitString(extendedTypes, argSymbols)
                when (baseExpr.getType()) {
                    is ListType -> "$base.map { $p -> $bodyStr }"
                    is SetType -> "$base.map { $p -> $bodyStr }.toSet()"
                    else -> throw RuntimeException("map expected List or Set at $loc")
                }
            }
            "associateWith" -> {
                val p = paramNames.single()
                val bodyStr = body.toTransitString(extendedTypes, argSymbols)
                when (baseExpr.getType()) {
                    is SetType -> "$base.associateWith { $p -> $bodyStr }"
                    else -> throw RuntimeException("associateWith expected Set at $loc")
                }
            }
            "fold" -> {
                val init = args[0].toTransitString(symbolTypes, argSymbols)
                val acc = paramNames[0]
                val elem = paramNames[1]
                val bodyStr = body.toTransitString(extendedTypes, argSymbols)
                "$base.fold($init) { $acc, $elem -> $bodyStr }"
            }
            else -> throw RuntimeException("Unknown collection method \"$methodName\" at $loc")
        }
    }

    override fun inferType(symbolEnv: Map<String, Type>): Type {
        resolvedProcFun?.let { return it.returnType }
        // HOF path — type should already be set via setInferredType during typePass
        return getType()
    }

    override fun toString(): String {
        val argStr = args.joinToString(", ")
        return when (val api = resolvedApiName) {
            null -> "$baseExpr.$methodName($argStr)"
            else -> "$api.$methodName($argStr)"
        }
    }
}

internal fun embedKotlinValueAsZ3(
    kotlinExpr: String,
    type: Type,
    forceString: Boolean,
    loc: ProgramLoc,
): String {
    if (forceString) {
        return "ctx.mkString(($kotlinExpr).toString())"
    }
    return when (type) {
        is BoolType -> "ctx.mkBool($kotlinExpr)"
        is IntType -> "ctx.mkInt($kotlinExpr)"
        is RealType -> "ctx.mkReal(($kotlinExpr).toString())"
        is StringType -> "ctx.mkString($kotlinExpr)"
        is ListType -> {
            val tv = type.toCodegenTypeVal()
            "$tv.toZ3Expr(Value($kotlinExpr, $tv), ctx)"
        }
        is SetType -> {
            val tv = type.toCodegenTypeVal()
            "$tv.toZ3Expr(Value($kotlinExpr, $tv), ctx)"
        }
        is MapType -> {
            val tv = type.toCodegenTypeVal()
            "$tv.toZ3Expr(Value($kotlinExpr, $tv), ctx)"
        }
        else -> throw RuntimeException("Cannot embed type $type as Z3 at $loc")
    }
}

internal fun exprReferencesAnyArg(expr: ExprNode, argSymbols: Set<String>): Boolean =
    argSymbols.any { exprReferencesSymbol(expr, it) }

class FieldAccessOnExprNode(
    val baseExpr: ExprNode,
    val fieldPath: List<String>,
    private val loc: ProgramLoc,
    val leafType: Type,
) : ExprNode(listOf(baseExpr)) {
    init {
        setInferredType(TypePassType.Inferred(leafType))
    }

    override fun programLocation() = loc

    override fun toZ3GuardString(
        symbolTypes: Map<String, Type>,
        argSymbols: Set<String>,
        forceString: Boolean,
    ): String {
        if (forceString && (leafType is ListType || leafType is ObjClassType)) {
            return "ctx.mkString((${toTransitString(symbolTypes, argSymbols)}).toString())"
        }
        val baseType = baseExpr.getType() as ObjClassType
        val baseZ3 = baseExpr.toZ3GuardString(symbolTypes, argSymbols)
        val fieldZ3 = ObjClassType.fieldAccessZ3Codegen(baseType, baseZ3, fieldPath)
        return castFieldZ3(fieldZ3, leafType, forceString)
    }

    override fun toTransitString(symbolTypes: Map<String, Type>, argSymbols: Set<String>): String {
        val base = baseExpr.toTransitString(symbolTypes, argSymbols)
        return "($base).${fieldPath.joinToString(".")}"
    }

    override fun inferType(symbolEnv: Map<String, Type>): Type = leafType

    override fun toString(): String = fieldPath.joinToString(".", prefix = "($baseExpr).")
}

private fun typeForTransit(expr: ExprNode, symbolTypes: Map<String, Type>): Type? {
    try {
        return expr.getType()
    } catch (_: RuntimeException) {
        // Substituted let bindings may lack inferred types; fall back to the symbol env
        // (symbols) or the literal's inherent type (Int/String/Bool/... literals).
    }
    return when (expr) {
        is SymbolValueExprNode -> symbolTypes[expr.symbol]
        is LiteralValueExprNode -> expr.inferType(emptyMap())
        else -> null
    }
}

private fun Type.isNumeric(): Boolean = this is IntType || this is RealType

private fun promoteNumeric(lhs: Type, rhs: Type): Type? = when {
    lhs is IntType && rhs is IntType -> intType
    lhs.isNumeric() && rhs.isNumeric() -> realType
    else -> null
}

private fun asZ3Real(guardStr: String, type: Type): String =
    if (type is IntType) "ctx.mkInt2Real($guardStr)" else guardStr

private fun asKotlinDouble(exprStr: String, type: Type): String =
    if (type is IntType) "($exprStr).toDouble()" else exprStr

private fun mkIteGuardString(
    condStr: String,
    thenStr: String,
    elseStr: String,
    resultType: Type,
    forceString: Boolean,
): String {
    val ite = "ctx.mkITE($condStr, $thenStr, $elseStr)"
    return castFieldZ3(ite, resultType, forceString)
}

private fun castFieldZ3(fieldZ3: String, leafType: Type, forceString: Boolean): String {
    val type = leafType.codegenErasure()
    if (forceString) {
        return when (type) {
            is BoolType -> throw RuntimeException("Cannot convert a Bool to a string")
            is IntType -> "ctx.intToString($fieldZ3 as IntExpr)"
            is RealType -> throw RuntimeException("Cannot convert a symbolic Real to a string")
            is StringType -> "$fieldZ3 as Expr<SeqSort<CharSort>>"
            is ObjClassType, is ListType ->
                throw RuntimeException("Cannot convert symbolic $leafType field to string")
            else -> throw RuntimeException("Invalid field type: $leafType")
        }
    }
    if (type is ObjClassType) {
        return fieldZ3
    }
    return when (type) {
        is BoolType -> "$fieldZ3 as BoolExpr"
        is IntType -> "$fieldZ3 as IntExpr"
        is RealType -> "$fieldZ3 as RealExpr"
        is StringType -> "$fieldZ3 as Expr<SeqSort<CharSort>>"
        is ListType, is SetType, is MapType -> fieldZ3
        else -> throw RuntimeException("Invalid field type: $leafType")
    }
}

class SymbolValueExprNode(
    val symbol : String,
    private val loc : ProgramLoc
) : ExprNode(listOf()) {
    override fun programLocation() = loc
    override fun toZ3GuardString(symbolTypes : Map<String,Type>, argSymbols : Set<String>, forceString : Boolean): String {
        val type = symbolTypes[symbol]?.codegenErasure()
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
                is RealType -> {
                    if (symbol in argSymbols) {
                        throw RuntimeException("Cannot convert a symbolic Real to a string")
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
                is ObjClassType, is ListType, is SetType, is MapType -> {
                    if (symbol in argSymbols) {
                        throw RuntimeException("Cannot convert symbolic $type to string")
                    } else {
                        "ctx.mkString(${symbol.toKotlinIdent()}.toString())"
                    }
                }
                else -> throw RuntimeException("Invalid type: $type")
            }

        }
        if (type is ObjClassType) {
            val typeVal = objClassTypeValName(type.name)
            return if (symbol in argSymbols) {
                ObjClassType.z3ConstString(symbol, typeVal)
            } else {
                ObjClassType.kotlinObjClassToZ3String(type.name, symbol)
            }
        }
        if (type is ListType) {
            val typeVal = type.toCodegenTypeVal()
            return if (symbol in argSymbols) {
                "ctx.mkConst(\"${symbol.escapeKotlinStringLiteral()}\", $typeVal.sort(ctx))"
            } else {
                "$typeVal.toZ3Expr(Value(${symbol.toKotlinIdent()}, $typeVal), ctx)"
            }
        }
        if (type is SetType) {
            val typeVal = type.toCodegenTypeVal()
            return if (symbol in argSymbols) {
                "ctx.mkConst(\"${symbol.escapeKotlinStringLiteral()}\", $typeVal.cellMetadata(ctx).sort)"
            } else {
                "$typeVal.toZ3Expr(Value(${symbol.toKotlinIdent()}, $typeVal), ctx)"
            }
        }
        if (type is MapType) {
            val typeVal = type.toCodegenTypeVal()
            return if (symbol in argSymbols) {
                "ctx.mkConst(\"${symbol.escapeKotlinStringLiteral()}\", $typeVal.cellMetadata(ctx).sort)"
            } else {
                "$typeVal.toZ3Expr(Value(${symbol.toKotlinIdent()}, $typeVal), ctx)"
            }
        }
        if (symbol in argSymbols) {
            return when (type) {
                is BoolType -> "ctx.mkBoolConst(\"${symbol.escapeKotlinStringLiteral()}\")"
                is IntType -> "ctx.mkIntConst(\"${symbol.escapeKotlinStringLiteral()}\")"
                is RealType -> "ctx.mkRealConst(\"${symbol.escapeKotlinStringLiteral()}\")"
                is StringType -> "ctx.mkStringConst(\"${symbol.escapeKotlinStringLiteral()}\")"
                else -> throw RuntimeException("Invalid type: $type")
            }
        }
        return when (type) {
            is BoolType -> "ctx.mkBool(${symbol.toKotlinIdent()})"
            is IntType -> "ctx.mkInt(${symbol.toKotlinIdent()})"
            is RealType -> "ctx.mkReal(${symbol.toKotlinIdent()}.toString())"
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
    private val qualifiedParts : List<String>?,
    private val loc : ProgramLoc
) : ASTNode(listOf()) {
    override fun programLocation() = loc
    internal fun valueProcName() = name
    internal fun qualifiedParts() = qualifiedParts
    internal fun isQualified() = qualifiedParts != null
    internal fun fullQualifiedName() = qualifiedParts?.joinToString(".") ?: name
    override fun toString(): String {
        return fullQualifiedName()
    }
}

class CompositeProcExprNode(
    private val compositeProcs : List<ASTNode>,
    private val loc : ProgramLoc
) : ASTNode(compositeProcs) {
    override fun programLocation() = loc
    internal fun compositeProcChildren() = compositeProcs
    override fun toString(): String {
        return compositeProcs.joinToString(" || ") { it.toString() }
    }
}

/**
 * First-order quantifier for invariant formulas: `forall x : T, body` / `exists x : T, body`.
 */
class QuantifiedExprNode(
    private val universal: Boolean,
    private val binder: String,
    private val binderType: TypeExpr,
    private val body: ExprNode,
    private val loc: ProgramLoc,
) : ExprNode(listOf(body)) {
    override fun programLocation() = loc
    internal fun isUniversal() = universal
    internal fun binderName() = binder
    internal fun binderTypeExpr() = binderType
    internal fun quantifiedBody() = body

    override fun toZ3GuardString(
        symbolTypes: Map<String, Type>,
        argSymbols: Set<String>,
        forceString: Boolean,
    ): String {
        throw RuntimeException("Quantified formulas are not allowed in guards at $loc")
    }

    override fun toTransitString(symbolTypes: Map<String, Type>, argSymbols: Set<String>): String {
        throw RuntimeException("Quantified formulas are not allowed in transits at $loc")
    }

    override fun inferType(symbolEnv: Map<String, Type>): Type = boolType

    override fun toString(): String {
        val q = if (universal) "forall" else "exists"
        return "$q $binder : $binderType, $body"
    }
}

