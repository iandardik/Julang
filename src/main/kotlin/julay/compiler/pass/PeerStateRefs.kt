package julay.compiler.pass

import julay.compiler.CompileError
import julay.compiler.OneLocCompileError
import julay.compiler.TypeExpr
import julay.compiler.ast.*
import julay.compiler.decl.TransitUpdate

/** Errors for `with` / create-index / apply-index misuse in a system expression. */
fun withIndexStructureErrors(node: ASTNode?, insideWith: Boolean = false): List<CompileError> {
    if (node == null) return emptyList()
    return when (node) {
        is WithSpecExprNode ->
            withIndexStructureErrors(node.withBody(), insideWith = true)
        is ParamProcExprNode -> {
            val errs = mutableListOf<CompileError>()
            when {
                node.isApplyIndex() && !insideWith ->
                    errs += OneLocCompileError(
                        node.programLocation(),
                        "apply-index \"${node.paramBody()}[${node.paramName()}]\" is only allowed inside with (...)",
                    )
                !node.isApplyIndex() && insideWith ->
                    errs += OneLocCompileError(
                        node.programLocation(),
                        "cannot create an index inside with; create outside, then apply with Name[${node.paramName()}]",
                    )
            }
            errs + withIndexStructureErrors(node.paramBody(), insideWith)
        }
        is CompositeProcExprNode ->
            node.compositeProcChildren().flatMap { withIndexStructureErrors(it, insideWith) }
        is AgSpecExprNode ->
            listOfNotNull(node.assumeExpr()).flatMap { withIndexStructureErrors(it, insideWith) } +
                withIndexStructureErrors(node.systemExpr(), insideWith)
        else -> node.children.flatMap { withIndexStructureErrors(it, insideWith) }
    }
}

data class PeerStateRef(
    val peerName: String,
    val varName: String,
    val indexed: Boolean,
    val loc: julay.compiler.ProgramLoc,
)

/** Collect P.var / P[idx].var reads from leaf-spec action bodies. */
fun collectPeerStateRefs(pc: ProcClassNode, knownPeers: Set<String>): List<PeerStateRef> {
    val out = mutableListOf<PeerStateRef>()
    fun walkExpr(expr: ExprNode) {
        when (expr) {
            is MemberAccessExprNode -> {
                val base = expr.baseExpr
                if (base is IndexExprNode && base.base is SymbolValueExprNode) {
                    val peer = (base.base as SymbolValueExprNode).symbol
                    if (peer in knownPeers) {
                        out += PeerStateRef(peer, expr.fieldName, indexed = true, expr.programLocation())
                    }
                } else if (base is SymbolValueExprNode && base.symbol in knownPeers) {
                    out += PeerStateRef(base.symbol, expr.fieldName, indexed = false, expr.programLocation())
                }
                walkExpr(base)
            }
            is FieldAccessExprNode -> {
                if (expr.baseSymbol in knownPeers && expr.fieldPath.size == 1) {
                    out += PeerStateRef(expr.baseSymbol, expr.fieldPath[0], indexed = false, expr.programLocation())
                }
            }
            else -> expr.children.filterIsInstance<ExprNode>().forEach { walkExpr(it) }
        }
    }
    pc.localDecls().forEach { decl ->
        when (decl) {
            is TransitionNode -> {
                decl.body().forEach { b ->
                    b.guards().forEach { walkExpr(it) }
                    b.transits().forEach { u ->
                        when (u) {
                            is TransitUpdate.Assign -> walkExpr(u.expr)
                            is TransitUpdate.IndexPut -> {
                                walkExpr(u.index)
                                walkExpr(u.value)
                            }
                            is TransitUpdate.Let -> walkExpr(u.init)
                        }
                    }
                }
            }
            is ConstructorNode -> {
                decl.body().forEach { b ->
                    b.guards().forEach { walkExpr(it) }
                    b.transits().forEach { u ->
                        when (u) {
                            is TransitUpdate.Assign -> walkExpr(u.expr)
                            is TransitUpdate.IndexPut -> {
                                walkExpr(u.index)
                                walkExpr(u.value)
                            }
                            is TransitUpdate.Let -> walkExpr(u.init)
                        }
                    }
                }
            }
            else -> {}
        }
    }
    return out
}

/**
 * For a compiled system, ensure each peer state reference targets a composed leaf
 * with matching indexing (indexed ref ⇒ create-indexed peer).
 */
fun peerCompositionErrors(
    systemLeaves: List<SpecLeaf>,
    leafSpecs: Map<String, LeafSpecNode>,
    pclasses: Map<String, ProcClassNode>,
): List<CompileError> {
    val byName = systemLeaves.groupBy { it.name }
    val known = byName.keys + pclasses.keys + leafSpecs.keys
    val errors = mutableListOf<CompileError>()
    systemLeaves.forEach { leaf ->
        val ls = leafSpecs[leaf.name] ?: return@forEach
        val pc = pclasses[leaf.name] ?: ls.asProcClass()
        collectPeerStateRefs(pc, known).forEach { ref ->
            val peers = byName[ref.peerName]
            if (peers.isNullOrEmpty()) {
                errors += OneLocCompileError(
                    ref.loc,
                    "leaf spec \"${leaf.name}\" references ${ref.peerName}.${ref.varName}, " +
                        "but this system does not compose ${ref.peerName}",
                )
                return@forEach
            }
            val indexedPeer = peers.any { it.isParameterized }
            when {
                ref.indexed && !indexedPeer ->
                    errors += OneLocCompileError(
                        ref.loc,
                        "leaf spec \"${leaf.name}\" references ${ref.peerName}[…].${ref.varName}, " +
                            "but this system does not compose an indexed ${ref.peerName}",
                    )
                !ref.indexed && indexedPeer && peers.all { it.isParameterized } ->
                    errors += OneLocCompileError(
                        ref.loc,
                        "leaf spec \"${leaf.name}\" references ${ref.peerName}.${ref.varName} without an index, " +
                            "but ${ref.peerName} is indexed in this system",
                    )
            }
        }
    }
    return errors
}
