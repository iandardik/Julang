package julay.compiler.pass

import julay.compiler.decl.ActionDecl
import julay.compiler.decl.TransitUpdate
import julay.compiler.isDiscardBinding
import julay.program.type.MapType
import julay.program.type.Type

/**
 * Emit action-level lets as Go locals; returns updated emit context.
 * Used at the start of fire bodies (and can be used before enablement checks).
 */
internal fun emitActionLetBindings(
    decl: ActionDecl,
    ctx: GoEmitCtx,
    indent: String = "",
): Pair<String, GoEmitCtx> {
    if (decl.actionLets.isEmpty()) return "" to ctx
    // Idempotent: callers may emit once before equality/reply binds, then again from fire.
    if (decl.actionLets.all { it.name.isDiscardBinding() || it.name in ctx.letBindings }) {
        return "" to ctx
    }
    val sb = StringBuilder()
    var local = ctx
    decl.actionLets.forEachIndexed { i, update ->
        if (!update.name.isDiscardBinding() && update.name in local.letBindings) {
            return@forEachIndexed
        }
        val bind =
            if (update.name.isDiscardBinding()) {
                "__discard_al$i"
            } else {
                "${update.name.toGoSafeIdent()}__al"
            }
        sb.appendLine("$indent$bind := ${update.init.toGoString(local)}")
        if (update.name.isDiscardBinding()) {
            sb.appendLine("${indent}_ = $bind")
        } else {
            local = local.withLet(update.name, bind)
        }
    }
    return sb.toString() to local
}

/**
 * Emit a list of transit updates (including nested [TransitUpdate.If]) into [sb].
 * Non-sequential: temps then apply within each executed arm.
 */
internal fun emitTransitUpdatesGo(
    updates: List<TransitUpdate>,
    ctx: GoEmitCtx,
    sequential: Boolean,
    stateVarTypes: Map<String, Type>,
    sb: StringBuilder,
    indent: String = "",
    tempPrefix: String = "",
): GoEmitCtx {
    var localCtx = ctx
    if (sequential) {
        updates.forEachIndexed { i, update ->
            when (update) {
                is TransitUpdate.Let -> {
                    val bind =
                        if (update.name.isDiscardBinding()) {
                            "${tempPrefix}__discard_t$i"
                        } else {
                            "${update.name.toGoSafeIdent()}__t"
                        }
                    sb.appendLine("$indent$bind := ${update.init.toGoString(localCtx)}")
                    if (update.name.isDiscardBinding()) {
                        sb.appendLine("${indent}_ = $bind")
                    } else {
                        localCtx = localCtx.withLet(update.name, bind)
                    }
                }
                is TransitUpdate.Assign -> {
                    sb.appendLine(
                        "$indent${assignTargetGo(update.key, localCtx)} = ${update.expr.toGoString(localCtx)}",
                    )
                }
                is TransitUpdate.IndexPut -> {
                    val base = stateOrLocalGo(update.collectionVar, localCtx)
                    val idx = update.index.toGoString(localCtx)
                    val value = update.value.toGoString(localCtx)
                    val ty = stateVarTypes[update.collectionVar]
                    sb.appendLine(
                        "$indent${assignTargetGo(update.collectionVar, localCtx)} = " +
                            indexPutGoExpr(base, idx, value, ty),
                    )
                }
                is TransitUpdate.If -> {
                    sb.appendLine("$indent" + "if ${update.cond.toGoString(localCtx)} {")
                    emitTransitUpdatesGo(
                        update.thenUpdates, localCtx, true, stateVarTypes, sb, "$indent\t", "${tempPrefix}t${i}_",
                    )
                    if (update.elseUpdates.isNotEmpty()) {
                        sb.appendLine("$indent} else {")
                        emitTransitUpdatesGo(
                            update.elseUpdates, localCtx, true, stateVarTypes, sb, "$indent\t", "${tempPrefix}e${i}_",
                        )
                    }
                    sb.appendLine("$indent}")
                }
            }
        }
        return localCtx
    }

    data class Pending(val key: String, val temp: String, val kind: String)
    val pending = mutableListOf<Pending>()
    updates.forEachIndexed { i, update ->
        when (update) {
            is TransitUpdate.Let -> {
                val bind =
                    if (update.name.isDiscardBinding()) {
                        "${tempPrefix}__discard_t$i"
                    } else {
                        "${update.name.toGoSafeIdent()}__t"
                    }
                sb.appendLine("$indent$bind := ${update.init.toGoString(localCtx)}")
                if (update.name.isDiscardBinding()) {
                    sb.appendLine("${indent}_ = $bind")
                } else {
                    localCtx = localCtx.withLet(update.name, bind)
                }
            }
            is TransitUpdate.Assign -> {
                val temp = "${tempPrefix}__t$i"
                sb.appendLine("$indent$temp := ${update.expr.toGoString(localCtx)}")
                pending += Pending(update.key, temp, "assign")
            }
            is TransitUpdate.IndexPut -> {
                val idxTemp = "${tempPrefix}__idx$i"
                val valTemp = "${tempPrefix}__val$i"
                sb.appendLine("$indent$idxTemp := ${update.index.toGoString(localCtx)}")
                sb.appendLine("$indent$valTemp := ${update.value.toGoString(localCtx)}")
                pending += Pending(update.collectionVar, "$idxTemp|$valTemp", "index")
            }
            is TransitUpdate.If -> {
                // Nested if: evaluate arm with its own simultaneous temps.
                sb.appendLine("$indent" + "if ${update.cond.toGoString(localCtx)} {")
                emitTransitUpdatesGo(
                    update.thenUpdates, localCtx, false, stateVarTypes, sb, "$indent\t", "${tempPrefix}t${i}_",
                )
                if (update.elseUpdates.isNotEmpty()) {
                    sb.appendLine("$indent} else {")
                    emitTransitUpdatesGo(
                        update.elseUpdates, localCtx, false, stateVarTypes, sb, "$indent\t", "${tempPrefix}e${i}_",
                    )
                }
                sb.appendLine("$indent}")
            }
        }
    }
    for (p in pending) {
        when (p.kind) {
            "assign" -> sb.appendLine("$indent${assignTargetGo(p.key, localCtx)} = ${p.temp}")
            "index" -> {
                val (idx, value) = p.temp.split("|")
                val base = stateOrLocalGo(p.key, localCtx)
                sb.appendLine(
                    "$indent${assignTargetGo(p.key, localCtx)} = " +
                        indexPutGoExpr(base, idx, value, stateVarTypes[p.key]),
                )
            }
        }
    }
    return localCtx
}

private fun indexPutGoExpr(base: String, idx: String, value: String, collectionType: Type?): String =
    if (collectionType is MapType) {
        "($base).Put($idx, $value)"
    } else {
        "($base).SetAt($idx, $value)"
    }

private fun assignTargetGo(key: String, ctx: GoEmitCtx): String {
    val parts = key.split(".")
    val root = parts[0]
    val rootGo = when {
        root in ctx.letBindings -> ctx.letBindings.getValue(root)
        root in ctx.stateVars -> "${ctx.procRecv}.$root"
        else -> "${ctx.procRecv}.$root"
    }
    if (parts.size == 1) return rootGo
    return parts.drop(1).fold(rootGo) { acc, seg -> "$acc.${seg.toGoObjFieldIdent()}" }
}

private fun stateOrLocalGo(name: String, ctx: GoEmitCtx): String = when {
    name in ctx.letBindings -> ctx.letBindings.getValue(name)
    name in ctx.stateVars -> "${ctx.procRecv}.$name"
    else -> "${ctx.procRecv}.$name"
}
