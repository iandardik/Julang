package julay.compiler.pass

import julay.compiler.TypeExpr
import julay.compiler.ast.*
import julay.compiler.decl.TransitUpdate
import julay.program.type.DomainType
import julay.program.type.Type

/**
 * Auxiliary action parameters for a leaf-spec action: declaration binder (if used) then
 * explicit `also` args, in source order.
 */
data class LeafSpecAuxParam(
    val name: String,
    val domain: String,
)

fun collectLeafSpecAuxParams(
    leaf: SpecLeaf,
    actionName: String,
    isConstructor: Boolean,
    pclasses: Map<String, ProcClassNode>,
    leafSpecs: Map<String, LeafSpecNode>,
): List<LeafSpecAuxParam> {
    val ls = leafSpecs[leaf.name] ?: return emptyList()
    val pc = pclasses[leaf.name] ?: ls.asProcClass()
    val declParam = ls.leafSpecParamName()
    val declType = ls.leafSpecParamType()
    val (guards, transits, alsoArgs) = if (isConstructor) {
        val ctor = pc.localDecls().filterIsInstance<ConstructorNode>()
            .firstOrNull { it.constructorName() == actionName }
            ?: return emptyList()
        Triple(
            ctor.body().flatMap { it.guards() },
            ctor.body().flatMap { it.transits() },
            ctor.alsoArgs(),
        )
    } else {
        val tr = pc.localDecls().filterIsInstance<TransitionNode>()
            .firstOrNull { it.transitionName() == actionName }
            ?: return emptyList()
        Triple(
            tr.body().flatMap { it.guards() },
            tr.body().flatMap { it.transits() },
            tr.alsoArgs(),
        )
    }

    fun refs(name: String): Boolean =
        guards.any { exprReferencesSymbol(it, name) } ||
            transits.any { update ->
                when (update) {
                    is TransitUpdate.Assign -> exprReferencesSymbol(update.expr, name)
                    is TransitUpdate.IndexPut ->
                        exprReferencesSymbol(update.index, name) || exprReferencesSymbol(update.value, name)
                    is TransitUpdate.Let -> exprReferencesSymbol(update.init, name)
                }
            }

    val out = mutableListOf<LeafSpecAuxParam>()
    if (declParam != null && declType != null && refs(declParam)) {
        // Prefer create-index domain when this leaf is composition-indexed under the same name.
        val domain = when {
            leaf.isParameterized && leaf.paramName == declParam ->
                typeDomainConstant(leaf.paramType!!) ?: leaf.paramType.toString()
            else -> typeDomainConstant(declType) ?: declType.toString()
        }
        out += LeafSpecAuxParam(declParam, domain)
    }
    alsoArgs?.children?.filterIsInstance<ArgNode>()?.forEach { arg ->
        try {
            val t = arg.type
            val domain = when (t) {
                is DomainType -> t.name
                else -> typeToTlaDomain(t)
            }
            out += LeafSpecAuxParam(arg.argName(), domain)
        } catch (_: RuntimeException) {
            val te = arg.argTypeExpr()
            out += LeafSpecAuxParam(arg.argName(), typeDomainConstant(te) ?: te.toString())
        }
    }
    return out.distinctBy { it.name }
}

/** Surface type of a sort aux/decl param in leaf-spec bodies. */
fun leafSpecBinderSurfaceType(type: Type): Type =
    if (type is DomainType) type.carrierType else type
