package julay.compiler.pass

import julay.compiler.ObjClassBuiltinRegistry
import julay.compiler.ast.*
import julay.compiler.decl.ActionDecl
import julay.compiler.decl.ProcClassDecl
import julay.compiler.decl.ProcDecl
import julay.compiler.decl.TransitUpdate
import julay.compiler.isDiscardBinding
import julay.program.action.TSAction
import julay.program.type.*

data class GoCodegenResult(
    val mainGo: String,
    val moduleName: String,
    val binaryName: String,
)

fun goCodegenPass(
    ast: RootNode,
    program: ProcDecl,
    procDecls: List<ProcDecl>,
    librariesInUse: Set<String> = emptySet(),
): GoCodegenResult {
    val leafMap = leafActionMap(ast, program.allProcNames(procDecls), librariesInUse)
    val alphabet = computeCompositionAlphabet(program, procDecls, leafMap)
    val occurrences = alphabet.leafOccurrences
    val distinctPclasses = occurrences.map { it.pclassName }.toSet()

    val procClasses = distinctPclasses.flatMap { proc ->
        ast.procClassPass(setOf(proc))
    }
    val procClassByName = procClasses.associateBy { it.name }
    val parentClassesOf: Map<String, Set<String>> = occurrences
        .filter { it.parentClass != null }
        .groupBy({ it.pclassName }, { it.parentClass!! })
        .mapValues { it.value.toSet() }

    val directionResult = computeChannelDirections(
        ast, program.allProcNames(procDecls), program, librariesInUse, procDecls,
    )
    // Parent↔child syncs use one channel per parent instance (canonical key per child class + action).
    val parentChildKeys = parentChildChannelKeys(alphabet)
    val directions = directionResult.directions
        .map { d -> parentChildKeys[d.channelKey]?.let { d.copy(channelKey = it.key) } ?: d }
        .distinctBy { Triple(it.channelKey, it.senderClass, it.receiverClass) }
    val parentChannels = parentChildKeys.values.distinct()
    // One Go channel per distinct composition channelKey (not merely per action name).
    val directionsByChannelKey = directions.groupBy { it.channelKey }
    val channelKeysByLeaf = alphabet.channelKeys.mapValues { (_, k) -> parentChildKeys[k]?.key ?: k }
    val handlerReplies = collectHandlerReplies(ast, distinctPclasses)

    val objClassDecls = ast.resolvedObjClassDecls()
        .filterNot { ObjClassBuiltinRegistry.isBuiltin(it.name) }
        .filterNot { decl -> decl.fields.any { it.type.containsUninterpretedType() } }

    val dynamicCtorNames = procClasses
        .filter { !it.isStatic }
        .flatMap { pc -> pc.constructors.map { it.action.name to pc.name } }
        .groupBy({ it.first }, { it.second })

    val sb = StringBuilder()
    // Body first so we can decide which std imports are needed.
    val bodySb = StringBuilder()

    // --- channel types & vars (non-local sync actions), one per channelKey ---
    val syncDirections = directions.filter { !it.isLocal }
    val emittedChannels = linkedSetOf<String>()
    for (dir in syncDirections) {
        val key = dir.channelKey
        if (!emittedChannels.add(key)) continue
        val an = key.toGoSafeIdent()
        val allForKey = directionsByChannelKey.getValue(key)
        val senderBound = allForKey.flatMap { it.senderBoundArgs }.distinct()
        val replyArgs = allForKey.flatMap { it.replyArgs }.distinct()
        val argTypes = resolveActionArgTypes(dir.actionName, procClassByName)

        if (replyArgs.isNotEmpty()) {
            bodySb.appendLine("type reply_$an struct {")
            for (arg in replyArgs) {
                val ty = argTypes[arg]?.toGoTypeString() ?: "any"
                bodySb.appendLine("\t${arg.toGoExportedIdent()} $ty")
            }
            bodySb.appendLine("}")
            bodySb.appendLine()
        }
        bodySb.appendLine("type msg_$an struct {")
        for (arg in senderBound) {
            val ty = argTypes[arg]?.toGoTypeString() ?: "any"
            bodySb.appendLine("\t${arg.toGoExportedIdent()} $ty")
        }
        if (replyArgs.isNotEmpty()) {
            bodySb.appendLine("\tReply chan reply_$an")
        }
        bodySb.appendLine("}")
        if (parentChannels.none { it.key == key }) {
            bodySb.appendLine("var ch_$an = make(chan msg_$an)")
        }
        bodySb.appendLine()
    }

    // --- obj structs ---
    for (obj in objClassDecls) {
        bodySb.appendLine("type ${obj.name} struct {")
        for (f in obj.fields) {
            bodySb.appendLine("\t${f.name.toGoExportedIdent()} ${f.type.toGoTypeString()}")
        }
        bodySb.appendLine("}")
        bodySb.appendLine()
        bodySb.appendLine("func (v ${obj.name}) ToString() string {")
        bodySb.appendLine("\treturn julay.ToStringOf(v)")
        bodySb.appendLine("}")
        bodySb.appendLine()
        bodySb.appendLine("func (v ${obj.name}) Key() string {")
        val keyParts = obj.fields.joinToString(" + \"|\" + ") { f ->
            "julay.KeyOf(v.${f.name.toGoExportedIdent()})"
        }.ifEmpty { "\"\"" }
        bodySb.appendLine("\treturn \"${obj.name}|\" + $keyParts")
        bodySb.appendLine("}")
        bodySb.appendLine()
    }

    // --- proc classes ---
    val emittedKeys = emittedChannels.toSet()
    val linkage = GoLinkage(
        parentChannels.filter { it.key in emittedKeys },
        parentClassesOf,
        handlerReplies,
    )
    for (pc in procClasses) {
        bodySb.append(
            emitProcClass(pc, directions, channelKeysByLeaf, dynamicCtorNames, procClassByName, linkage),
        )
        bodySb.appendLine()
    }

    // --- main ---
    bodySb.appendLine("func main() {")
    bodySb.appendLine("\targs := julay.ListOf(os.Args[1:]...)")
    val started = linkedSetOf<String>()
    for (occ in occurrences) {
        val pc = procClassByName[occ.pclassName] ?: continue
        if (!pc.isStatic) continue
        val initially = pc.constructors.firstOrNull { it.action.name == "initially" } ?: continue
        if (!started.add(pc.name)) continue
        bodySb.appendLine("\tjulay.AddProc()")
        bodySb.appendLine("\tgo new${pc.name}_${initially.action.name.toGoSafeIdent()}(args).run()")
    }
    bodySb.appendLine("\tjulay.WaitAll()")
    bodySb.appendLine("}")

    val body = bodySb.toString()
    val moduleName = program.name.toGoSafeIdent().lowercase().ifEmpty { "julayprog" }
    sb.appendLine("package main")
    sb.appendLine()
    sb.appendLine("import (")
    sb.appendLine("\t\"os\"")
    if ("strconv." in body) sb.appendLine("\t\"strconv\"")
    if ("strings." in body) sb.appendLine("\t\"strings\"")
    sb.appendLine()
    sb.appendLine("\t\"$moduleName/julay\"")
    sb.appendLine(")")
    sb.appendLine()
    sb.append(body)

    val mainGo = sb.toString()
    return GoCodegenResult(
        mainGo = mainGo,
        moduleName = moduleName,
        binaryName = program.name,
    )
}

private enum class CaseRole { Local, Timer, Sender, Receiver }

/** A parent↔child channel: owned by each [parentClass] instance, shared with its [childClass] children. */
private data class ParentChildChannel(val key: String, val parentClass: String, val childClass: String)

/** Program-wide wiring for parent/child channels and HTTP handler replies. */
private data class GoLinkage(
    val parentChannels: List<ParentChildChannel>,
    /** Child class → parent classes it is bound to (`B<A>`). */
    val parentClassesOf: Map<String, Set<String>>,
    /** Handler class → response action names (`A.snd ~> A.rcv`). */
    val handlerReplies: Map<String, Set<String>>,
) {
    fun ownedBy(pc: String) = parentChannels.filter { it.parentClass == pc }
    fun sharedTo(pc: String) = parentChannels.filter { it.childClass == pc }
    fun isParentChild(key: String) = parentChannels.any { it.key == key }
}

/**
 * Composition channel keys where a `B<A>` child meets its parent `A`, mapped to one canonical
 * key per (child, action) so `B<A> || B<C>` share one child field and Go message type; each
 * child instance has exactly one parent, which hands over its own channel at spawn.
 */
private fun parentChildChannelKeys(alphabet: CompositionAlphabetResult): Map<String, ParentChildChannel> {
    val out = linkedMapOf<String, ParentChildChannel>()
    alphabet.allOffers.filter { it.compositionHidden && !it.isLocal }.groupBy { it.channelKey }
        .forEach { (key, offers) ->
            val child = offers.firstOrNull { o ->
                o.parentClass != null && offers.any { it.pclassKey == o.parentClass && it.pclassKey != o.pclassKey }
            } ?: return@forEach
            val parent = child.parentClass!!
            out[key] = ParentChildChannel("pc#${child.pclassKey}#${child.name}", parent, child.pclassKey)
        }
    return out
}

/** `serveHttp(port, A.snd ~> A.rcv)` targets among [classes]: class → rcv actions. */
private fun collectHandlerReplies(ast: RootNode, classes: Set<String>): Map<String, Set<String>> =
    ast.handlerRefs().filter { it.handlerClass() in classes }
        .groupBy({ it.handlerClass() }, { it.handlerAction() })
        .mapValues { it.value.toSet() }

/** Go expression for channel [key] inside a proc method (`p.ch_X` for parent↔child, else global). */
private fun goChannelRef(key: String, linkage: GoLinkage): String {
    val id = key.toGoSafeIdent()
    return if (linkage.isParentChild(key)) "p.ch_$id" else "ch_$id"
}

private data class CaseEmit(
    val name: String,
    val decl: ActionDecl,
    val role: CaseRole,
    val dir: ActionChannelDirection?,
)

private fun resolveActionArgTypes(
    actionName: String,
    procClassByName: Map<String, ProcClassDecl>,
): Map<String, Type> {
    for (pc in procClassByName.values) {
        val decl = pc.transitions.firstOrNull { it.action.name == actionName }
            ?: pc.constructors.firstOrNull { it.action.name == actionName }
        if (decl != null) {
            return decl.action.args.associate { it.name to it.type }
        }
    }
    return emptyMap()
}

private fun emitProcClass(
    pc: ProcClassDecl,
    directions: List<ActionChannelDirection>,
    channelKeysByLeaf: Map<LeafActionId, String>,
    dynamicCtorNames: Map<String, List<String>>,
    procClassByName: Map<String, ProcClassDecl>,
    linkage: GoLinkage,
): String {
    val hasTimers = pc.transitions.any {
        it.isTimer || it.modifier == TSAction.SyncRole.Timer
    }
    val replyOwned = directions.filter {
        it.senderClass == pc.name && it.replyArgs.isNotEmpty()
    }.map { it.channelKey }.distinct()

    val sb = StringBuilder()
    sb.appendLine("type ${pc.name} struct {")
    for (v in pc.stateVars) {
        sb.appendLine("\t${v.name} ${v.type.toGoTypeString()}")
    }
    if (hasTimers) {
        sb.appendLine("\ttimers *julay.ProcTimers")
    }
    for (key in replyOwned) {
        val safe = key.toGoSafeIdent()
        sb.appendLine("\treply_$safe chan reply_$safe")
    }
    val channelFields = (linkage.ownedBy(pc.name) + linkage.sharedTo(pc.name)).map { it.key }.distinct()
    for (key in channelFields) {
        val safe = key.toGoSafeIdent()
        sb.appendLine("\tch_$safe chan msg_$safe")
    }
    for (action in linkage.handlerReplies[pc.name].orEmpty()) {
        sb.appendLine("\t${goHttpReplyField(action)} chan julay.HttpServerResponse")
    }
    sb.appendLine("}")
    sb.appendLine()

    // constructors
    for (ctor in pc.constructors) {
        sb.append(emitConstructor(pc, ctor, hasTimers, replyOwned, linkage.ownedBy(pc.name).map { it.key }.distinct()))
        sb.appendLine()
    }

    // run loop
    sb.append(emitRunLoop(pc, directions, channelKeysByLeaf, dynamicCtorNames, procClassByName, hasTimers, linkage))
    return sb.toString()
}

private fun emitConstructor(
    pc: ProcClassDecl,
    ctor: ActionDecl,
    hasTimers: Boolean,
    replyOwned: List<String>,
    ownedChannels: List<String>,
): String {
    val stateVars = pc.stateVars.map { it.name }.toSet()
    val argBindings = ctor.action.args.associate { it.name to it.name.toGoSafeIdent() }
    val ctx = GoEmitCtx(stateVars = stateVars, argBindings = argBindings)

    val params = ctor.action.args.joinToString(", ") { arg ->
        "${arg.name.toGoSafeIdent()} ${arg.type.toGoTypeString()}"
    }
    val sb = StringBuilder()
    sb.appendLine("func new${pc.name}_${ctor.action.name.toGoSafeIdent()}($params) *${pc.name} {")
    sb.appendLine("\tp := &${pc.name}{")
    if (hasTimers) {
        sb.appendLine("\t\ttimers: julay.NewProcTimers(),")
    }
    for (key in replyOwned) {
        val safe = key.toGoSafeIdent()
        sb.appendLine("\t\treply_$safe: make(chan reply_$safe, 1),")
    }
    for (key in ownedChannels) {
        val safe = key.toGoSafeIdent()
        sb.appendLine("\t\tch_$safe: make(chan msg_$safe),")
    }
    sb.appendLine("\t}")
    // binding for constructor args that come from equality in guard (usually equality to param)
    // For initially(args), args is already a param — transit uses state only typically.
    val bound = computeBoundArgs(ctor)
    for (arg in ctor.action.args) {
        if (arg.name in bound.equalityBound) {
            val rhs = findEqualityRhs(ctor, arg.name) ?: continue
            // Materialize if needed — usually the param itself
            val rhsGo = rhs.toGoString(ctx)
            if (rhsGo != arg.name.toGoSafeIdent()) {
                sb.appendLine("\t${arg.name.toGoSafeIdent()} := $rhsGo")
            }
        }
    }
    for (arm in ctor.errors) {
        val cond = arm.condExpr().toGoString(ctx)
        val msg = arm.msgExpr().toGoString(ctx)
        sb.appendLine("\tif $cond { julay.Fail(julay.ToStringOf($msg)) }")
    }
    val stateVarTypes = pc.stateVars.associate { it.name to it.type }
    val ctorTransit = emitTransitBody(ctor, ctx, sequential = true, stateVarTypes).trimEnd()
    if (ctorTransit.isNotEmpty()) {
        sb.appendLine(ctorTransit.prependIndent("\t"))
    }
    for (stmt in ctor.afters) {
        sb.appendLine("\t${stmt.toGoStmtString(ctx)}")
    }
    sb.appendLine("\treturn p")
    sb.appendLine("}")
    return sb.toString()
}

private fun channelKeyForOffer(
    pcName: String,
    actionName: String,
    channelKeysByLeaf: Map<LeafActionId, String>,
    isConstructor: Boolean = false,
): String {
    val match = channelKeysByLeaf.entries.firstOrNull { (id, _) ->
        id.pclassKey == pcName && id.actionName == actionName && id.isConstructor == isConstructor
    }
    return match?.value ?: actionName
}

private fun emitRunLoop(
    pc: ProcClassDecl,
    directions: List<ActionChannelDirection>,
    channelKeysByLeaf: Map<LeafActionId, String>,
    dynamicCtorNames: Map<String, List<String>>,
    procClassByName: Map<String, ProcClassDecl>,
    @Suppress("UNUSED_PARAMETER") hasTimers: Boolean,
    linkage: GoLinkage,
): String {
    val stateVars = pc.stateVars.map { it.name }.toSet()
    val runtimeTransitions = pc.transitions

    val sb = StringBuilder()
    sb.appendLine("func (p *${pc.name}) run() {")
    sb.appendLine("\tdefer julay.DoneProc()")
    sb.appendLine("\tfor {")

    val cases = mutableListOf<CaseEmit>()
    for (tr in runtimeTransitions) {
        val isTimer = tr.isTimer || tr.modifier == TSAction.SyncRole.Timer
        val isInternal = tr.modifier == TSAction.SyncRole.Internal
        val offerKey = channelKeyForOffer(pc.name, tr.action.name, channelKeysByLeaf)
        val dirs = directions.filter { it.actionName == tr.action.name }
        val asSender = dirs.firstOrNull {
            it.senderClass == pc.name && !it.isLocal && it.channelKey == offerKey
        } ?: dirs.firstOrNull { it.senderClass == pc.name && !it.isLocal }
        val asReceiver = dirs.firstOrNull {
            it.receiverClass == pc.name && !it.isLocal && it.channelKey == offerKey
        } ?: dirs.firstOrNull { it.receiverClass == pc.name && !it.isLocal }
        val asLocal = dirs.firstOrNull { it.isLocal && it.senderClass == pc.name }
            ?: if (isInternal || isTimer) {
                ActionChannelDirection(
                    actionName = tr.action.name,
                    senderClass = pc.name,
                    receiverClass = null,
                    senderBoundArgs = tr.action.args.map { it.name },
                    replyArgs = emptyList(),
                    isLocal = true,
                    channelKey = offerKey,
                )
            } else null

        when {
            isTimer -> cases += CaseEmit(tr.action.name, tr, CaseRole.Timer, asLocal)
            isInternal || asLocal != null && asSender == null && asReceiver == null ->
                cases += CaseEmit(tr.action.name, tr, CaseRole.Local, asLocal)
            asSender != null -> cases += CaseEmit(tr.action.name, tr, CaseRole.Sender, asSender)
            asReceiver != null -> cases += CaseEmit(tr.action.name, tr, CaseRole.Receiver, asReceiver)
            else -> cases += CaseEmit(tr.action.name, tr, CaseRole.Local, asLocal)
        }
    }

    sb.appendLine("\t\tenabledAny := false")

    for (c in cases) {
        val safe = c.name.toGoSafeIdent()
        val decl = c.decl
        val baseCtx = GoEmitCtx(stateVars = stateVars).withGuard(true)
        when (c.role) {
            CaseRole.Local, CaseRole.Timer -> {
                val bound = computeBoundArgs(decl)
                // Emit membership choices + residual guard
                sb.append(emitEnablementLocals(safe, decl, bound, baseCtx, c.role == CaseRole.Timer))
            }
            CaseRole.Sender -> {
                val dir = c.dir!!
                val bound = computeBoundArgs(decl)
                sb.append(emitSenderEnablement(safe, decl, dir, bound, baseCtx, linkage))
            }
            CaseRole.Receiver -> {
                // Receiver enablement: residual guard only (no sender-owned args yet)
                val dir = c.dir!!
                val bound = computeBoundArgs(decl, availableFromOther = dir.senderBoundArgs.toSet())
                sb.append(emitReceiverEnablement(safe, decl, dir, bound, baseCtx, linkage))
            }
        }
    }

    sb.appendLine("\t\tif !enabledAny {")
    sb.appendLine("\t\t\treturn")
    sb.appendLine("\t\t}")
    sb.appendLine()
    sb.appendLine("\t\tselect {")

    for (c in cases) {
        val safe = c.name.toGoSafeIdent()
        val decl = c.decl
        when (c.role) {
            CaseRole.Local -> {
                sb.appendLine("\t\tcase <-case_$safe:")
                sb.append(emitLocalArgRebind(decl, safe, stateVars))
                sb.append(emitFireBody(pc, decl, c.dir, CaseRole.Local, stateVars, dynamicCtorNames, procClassByName, linkage))
            }
            CaseRole.Timer -> {
                sb.appendLine("\t\tcase <-case_$safe:")
                sb.appendLine("\t\t\tp.timers.ClearFired(\"${c.name.escapeGoStringLiteral()}\")")
                sb.append(emitLocalArgRebind(decl, safe, stateVars))
                sb.append(emitFireBody(pc, decl, c.dir, CaseRole.Timer, stateVars, dynamicCtorNames, procClassByName, linkage))
            }
            CaseRole.Sender -> {
                val dir = c.dir!!
                val chanId = dir.channelKey.toGoSafeIdent()
                // payload_* avoids shadowing the msg_* type name in Go.
                sb.appendLine("\t\tcase ch_send_$safe <- payload_$safe:")
                if (dir.replyArgs.isNotEmpty()) {
                    sb.appendLine("\t\t\treplyMsg_$safe := <-p.reply_$chanId")
                    for (arg in dir.replyArgs) {
                        sb.appendLine("\t\t\t${arg.toGoSafeIdent()} := replyMsg_$safe.${arg.toGoExportedIdent()}")
                    }
                }
                sb.append(emitFireBody(pc, decl, dir, CaseRole.Sender, stateVars, dynamicCtorNames, procClassByName, linkage))
            }
            CaseRole.Receiver -> {
                val dir = c.dir!!
                val chanId = dir.channelKey.toGoSafeIdent()
                if (dir.senderBoundArgs.isEmpty() && dir.replyArgs.isEmpty()) {
                    sb.appendLine("\t\tcase <-case_$safe:")
                } else {
                    sb.appendLine("\t\tcase payload_$safe := <-case_$safe:")
                }
                for (arg in dir.senderBoundArgs) {
                    sb.appendLine("\t\t\t${arg.toGoSafeIdent()} := payload_$safe.${arg.toGoExportedIdent()}")
                    sb.appendLine("\t\t\t_ = ${arg.toGoSafeIdent()}")
                }
                // Materialize receiver-bound args (including reply args) for body + reply channel.
                val bound = computeBoundArgs(decl, availableFromOther = dir.senderBoundArgs.toSet())
                val recvCtx = GoEmitCtx(
                    stateVars = stateVars,
                    argBindings = (dir.senderBoundArgs + bound.boundArgs).associateWith { it.toGoSafeIdent() },
                )
                val receiverOwned = (bound.boundArgs - dir.senderBoundArgs.toSet()).toList()
                for (arg in receiverOwned) {
                    when {
                        arg in bound.equalityBound -> {
                            val rhs = findEqualityRhs(decl, arg) ?: continue
                            sb.appendLine("\t\t\t${arg.toGoSafeIdent()} := ${rhs.toGoString(recvCtx)}")
                            sb.appendLine("\t\t\t_ = ${arg.toGoSafeIdent()}")
                        }
                        arg in bound.membershipBound -> {
                            val setExpr = findMembershipSet(decl, arg) ?: continue
                            val elemTy = try {
                                setExpr.getType().let {
                                    when (it) {
                                        is SetType -> it.elementType
                                        is ListType -> it.elementType
                                        is MapType -> it.keyType
                                        else -> intType
                                    }
                                }.toGoTypeString()
                            } catch (_: RuntimeException) {
                                "any"
                            }
                            sb.appendLine(
                                "\t\t\t${arg.toGoSafeIdent()}, _ := julay.ChooseUniformFiltered(" +
                                    "(${setExpr.toGoString(recvCtx)}).Elements(), " +
                                    "func(__cand $elemTy) bool { return true })",
                            )
                        }
                    }
                }
                if (dir.replyArgs.isNotEmpty()) {
                    val fields = dir.replyArgs.joinToString(", ") { arg ->
                        "${arg.toGoExportedIdent()}: ${arg.toGoSafeIdent()}"
                    }
                    sb.appendLine("\t\t\tpayload_$safe.Reply <- reply_$chanId{$fields}")
                }
                sb.append(emitFireBody(pc, decl, dir, CaseRole.Receiver, stateVars, dynamicCtorNames, procClassByName, linkage))
            }
        }
    }

    sb.appendLine("\t\t}")
    sb.appendLine("\t}")
    sb.appendLine("}")
    return sb.toString()
}

private fun emitEnablementLocals(
    safe: String,
    decl: ActionDecl,
    bound: BoundArgAnalysis,
    ctx: GoEmitCtx,
    isTimer: Boolean,
): String {
    val sb = StringBuilder()
    // Membership-bound args: choose or disable
    val memArgs = decl.action.args.map { it.name }.filter { it in bound.membershipBound }
    val eqArgs = decl.action.args.map { it.name }.filter { it in bound.equalityBound }

    sb.appendLine("\t\tvar case_$safe <-chan struct{} = nil")
    if (memArgs.isEmpty() && eqArgs.isEmpty() && decl.action.args.isEmpty()) {
        val guard = residualGuardGo(decl, bound, ctx)
        if (isTimer) {
            // Wait on an armed timer even before it fires (FiredChan is non-nil while armed).
            sb.appendLine("\t\tif ($guard) {")
            sb.appendLine("\t\t\tcase_$safe = p.timers.FiredChan(\"${decl.action.name.escapeGoStringLiteral()}\")")
            sb.appendLine("\t\t\tif case_$safe != nil { enabledAny = true }")
            sb.appendLine("\t\t}")
        } else {
            sb.appendLine("\t\ten_$safe := ($guard)")
            sb.appendLine("\t\tif en_$safe {")
            sb.appendLine("\t\t\tenabledAny = true")
            sb.appendLine("\t\t\t__ch_$safe := make(chan struct{}, 1)")
            sb.appendLine("\t\t\t__ch_$safe <- struct{}{}")
            sb.appendLine("\t\t\tcase_$safe = __ch_$safe")
            sb.appendLine("\t\t}")
        }
        return sb.toString()
    }

    // Bind equality args first (suffix with action name to avoid redeclarations across cases).
    var bindCtx = ctx
    for (arg in eqArgs) {
        val rhs = findEqualityRhs(decl, arg) ?: continue
        val local = "${arg.toGoSafeIdent()}_$safe"
        sb.appendLine("\t\t$local := ${rhs.toGoString(bindCtx)}")
        sb.appendLine("\t\t_ = $local") // may be unused if residual skips the binding conjunct
        bindCtx = bindCtx.withArgBindings(mapOf(arg to local))
    }
    // Membership: nested ChooseUniformFiltered (single arg first; multi-arg cartesian later)
    if (memArgs.size == 1) {
        val arg = memArgs.single()
        val setExpr = findMembershipSet(decl, arg)!!
        val setGo = setExpr.toGoString(bindCtx)
        val elemTy = elementTypeOfCollection(setExpr)?.toGoTypeString() ?: "any"
        val predCtx = bindCtx.withArgBindings(mapOf(arg to "__cand"))
        val predGuard = residualGuardGo(decl, bound, predCtx, skipMembershipOf = setOf(arg))
        sb.appendLine("\t\t${arg.toGoSafeIdent()}, ok_$safe := julay.ChooseUniformFiltered(($setGo).Elements(), func(__cand $elemTy) bool {")
        sb.appendLine("\t\t\treturn ($predGuard)")
        sb.appendLine("\t\t})")
        sb.appendLine("\t\tif ok_$safe {")
        val extraGuard = residualGuardGo(
            decl, bound,
            bindCtx.withArgBindings(mapOf(arg to arg.toGoSafeIdent())),
            skipMembershipOf = setOf(arg),
        )
        // pred already checked residual that mentions arg; still need residual without mem
        sb.appendLine("\t\t\tif ($extraGuard) {")
        if (isTimer) {
            sb.appendLine("\t\t\t\tcase_$safe = p.timers.FiredChan(\"${decl.action.name.escapeGoStringLiteral()}\")")
            sb.appendLine("\t\t\t\tif case_$safe != nil { enabledAny = true }")
        } else {
            sb.appendLine("\t\t\t\tenabledAny = true")
            sb.appendLine("\t\t\t\t__ch_$safe := make(chan struct{}, 1)")
            sb.appendLine("\t\t\t\t__ch_$safe <- struct{}{}")
            sb.appendLine("\t\t\t\tcase_$safe = __ch_$safe")
        }
        sb.appendLine("\t\t\t}")
        sb.appendLine("\t\t}")
    } else {
        // Multi membership or only equality: evaluate residual
        val guard = residualGuardGo(decl, bound, bindCtx)
        sb.appendLine("\t\tif ($guard) {")
        if (isTimer) {
            sb.appendLine("\t\t\tcase_$safe = p.timers.FiredChan(\"${decl.action.name.escapeGoStringLiteral()}\")")
            sb.appendLine("\t\t\tif case_$safe != nil { enabledAny = true }")
        } else {
            sb.appendLine("\t\t\tenabledAny = true")
            sb.appendLine("\t\t\t__ch_$safe := make(chan struct{}, 1)")
            sb.appendLine("\t\t\t__ch_$safe <- struct{}{}")
            sb.appendLine("\t\t\tcase_$safe = __ch_$safe")
        }
        sb.appendLine("\t\t}")
    }
    return sb.toString()
}

private fun emitSenderEnablement(
    safe: String,
    decl: ActionDecl,
    dir: ActionChannelDirection,
    bound: BoundArgAnalysis,
    ctx: GoEmitCtx,
    linkage: GoLinkage,
): String {
    val chanId = dir.channelKey.toGoSafeIdent()
    val sb = StringBuilder()
    sb.appendLine("\t\tvar ch_send_$safe chan msg_$chanId = nil")
    sb.appendLine("\t\tvar payload_$safe msg_$chanId")
    var bindCtx = ctx
    for (arg in dir.senderBoundArgs) {
        when {
            arg in bound.equalityBound -> {
                val rhs = findEqualityRhs(decl, arg)!!
                sb.appendLine("\t\t${arg.toGoSafeIdent()} := ${rhs.toGoString(bindCtx)}")
                bindCtx = bindCtx.withArgBindings(mapOf(arg to arg.toGoSafeIdent()))
            }
            arg in bound.membershipBound -> {
                val setExpr = findMembershipSet(decl, arg)!!
                val setGo = setExpr.toGoString(bindCtx)
                val elemTy = elementTypeOfCollection(setExpr)?.toGoTypeString() ?: "any"
                val predCtx = bindCtx.withArgBindings(mapOf(arg to "__cand"))
                val pred = residualGuardGo(decl, bound, predCtx, skipMembershipOf = setOf(arg))
                sb.appendLine("\t\t${arg.toGoSafeIdent()}, ok_${safe}_$arg := julay.ChooseUniformFiltered(($setGo).Elements(), func(__cand $elemTy) bool {")
                sb.appendLine("\t\t\treturn ($pred)")
                sb.appendLine("\t\t})")
                sb.appendLine("\t\t_ = ok_${safe}_$arg")
                bindCtx = bindCtx.withArgBindings(mapOf(arg to arg.toGoSafeIdent()))
            }
        }
    }
    // Args both sides bind by equality stay local to each side; the body may still read them.
    val localEq = decl.action.args.map { it.name }
        .filter { it !in dir.senderBoundArgs && it !in dir.replyArgs && it in bound.equalityBound }
    for (arg in localEq) {
        val rhs = findEqualityRhs(decl, arg) ?: continue
        sb.appendLine("\t\t${arg.toGoSafeIdent()} := ${rhs.toGoString(bindCtx)}")
        sb.appendLine("\t\t_ = ${arg.toGoSafeIdent()}")
        bindCtx = bindCtx.withArgBindings(mapOf(arg to arg.toGoSafeIdent()))
    }
    val guard = residualGuardGo(decl, bound, bindCtx)
    val memOk = dir.senderBoundArgs.filter { it in bound.membershipBound }
        .joinToString(" && ") { "ok_${safe}_$it" }
        .ifEmpty { "true" }
    sb.appendLine("\t\tif ($memOk) && ($guard) {")
    sb.appendLine("\t\t\tenabledAny = true")
    val fields = buildString {
        for (arg in dir.senderBoundArgs) {
            append("${arg.toGoExportedIdent()}: ${arg.toGoSafeIdent()}, ")
        }
        if (dir.replyArgs.isNotEmpty()) {
            append("Reply: p.reply_$chanId, ")
        }
    }
    sb.appendLine("\t\t\tpayload_$safe = msg_$chanId{$fields}")
    sb.appendLine("\t\t\tch_send_$safe = ${goChannelRef(dir.channelKey, linkage)}")
    sb.appendLine("\t\t}")
    return sb.toString()
}

private fun emitReceiverEnablement(
    safe: String,
    decl: ActionDecl,
    dir: ActionChannelDirection,
    bound: BoundArgAnalysis,
    ctx: GoEmitCtx,
    linkage: GoLinkage,
): String {
    val chanId = dir.channelKey.toGoSafeIdent()
    val sb = StringBuilder()
    sb.appendLine("\t\tvar case_$safe chan msg_$chanId = nil")
    // Receiver residual must not reference sender-owned args
    val guard = residualGuardGo(decl, bound, ctx, skipArgs = dir.senderBoundArgs.toSet())
    sb.appendLine("\t\tif ($guard) {")
    sb.appendLine("\t\t\tenabledAny = true")
    sb.appendLine("\t\t\tcase_$safe = ${goChannelRef(dir.channelKey, linkage)}")
    sb.appendLine("\t\t}")
    return sb.toString()
}

/** Re-materialize equality-bound args inside a local/timer select case body. */
private fun emitLocalArgRebind(decl: ActionDecl, @Suppress("UNUSED_PARAMETER") safe: String, stateVars: Set<String>): String {
    val bound = computeBoundArgs(decl)
    if (bound.equalityBound.isEmpty()) return ""
    val sb = StringBuilder()
    var ctx = GoEmitCtx(stateVars = stateVars)
    for (arg in decl.action.args.map { it.name }) {
        if (arg !in bound.equalityBound) continue
        val rhs = findEqualityRhs(decl, arg) ?: continue
        sb.appendLine("\t\t\t${arg.toGoSafeIdent()} := ${rhs.toGoString(ctx)}")
        ctx = ctx.withArgBindings(mapOf(arg to arg.toGoSafeIdent()))
    }
    return sb.toString()
}

private fun emitFireBody(
    pc: ProcClassDecl,
    decl: ActionDecl,
    dir: ActionChannelDirection?,
    @Suppress("UNUSED_PARAMETER") role: CaseRole,
    stateVars: Set<String>,
    dynamicCtorNames: Map<String, List<String>>,
    procClassByName: Map<String, ProcClassDecl>,
    linkage: GoLinkage,
): String {
    val argNames = decl.action.args.map { it.name }
    val argBindings = argNames.associateWith { it.toGoSafeIdent() }
    // Also include reply args for sender after reply received
    val replyBindings = dir?.replyArgs?.associateWith { it.toGoSafeIdent() }.orEmpty()
    val ctx = GoEmitCtx(stateVars = stateVars, argBindings = argBindings + replyBindings)

    val sb = StringBuilder()
    // errors
    for (arm in decl.errors) {
        val cond = arm.condExpr().toGoString(ctx)
        val msg = arm.msgExpr().toGoString(ctx)
        sb.appendLine("\t\t\tif $cond { julay.Fail(julay.ToStringOf($msg)) }")
    }
    // before
    for (stmt in decl.befores) {
        sb.appendLine("\t\t\t${stmt.toGoStmtString(ctx)}")
    }
    // transit
    val stateVarTypes = pc.stateVars.associate { it.name to it.type }
    val transit = emitTransitBody(decl, ctx, sequential = false, stateVarTypes).trimEnd()
    if (transit.isNotEmpty()) {
        sb.appendLine(transit.prependIndent("\t\t\t"))
    }
    // after
    for (stmt in decl.afters) {
        sb.appendLine("\t\t\t${stmt.toGoStmtString(ctx)}")
    }
    // spawn if transition name matches a dynamic ctor (children only from their parents)
    val spawnTargets = dynamicCtorNames[decl.action.name].orEmpty()
        .filter { it != pc.name || !pc.isStatic }
        .filter { target ->
            val parents = linkage.parentClassesOf[target]
            // Parent-bound procs (`B<A>`) are only spawned by a declared parent;
            // unbound dynamics may be spawned by any peer that syncs their ctor.
            parents == null || parents.isEmpty() || pc.name in parents
        }
    for (childClass in spawnTargets) {
        val child = procClassByName[childClass] ?: continue
        val ctor = child.constructors.firstOrNull { it.action.name == decl.action.name } ?: continue
        val args = ctor.action.args.joinToString(", ") { it.name.toGoSafeIdent() }
        val shared = linkage.ownedBy(pc.name).filter { it.childClass == childClass }.map { it.key }.distinct()
        sb.appendLine("\t\t\tjulay.AddProc()")
        if (shared.isEmpty()) {
            sb.appendLine("\t\t\tgo new${childClass}_${ctor.action.name.toGoSafeIdent()}($args).run()")
        } else {
            val v = "__child_${childClass.toGoSafeIdent()}"
            sb.appendLine("\t\t\t$v := new${childClass}_${ctor.action.name.toGoSafeIdent()}($args)")
            for (key in shared) {
                val safe = key.toGoSafeIdent()
                sb.appendLine("\t\t\t$v.ch_$safe = p.ch_$safe")
            }
            sb.appendLine("\t\t\tgo $v.run()")
        }
    }
    if (decl.action.name in linkage.handlerReplies[pc.name].orEmpty()) {
        val resp = decl.action.args.firstOrNull()?.name?.toGoSafeIdent()
        if (resp != null) sb.appendLine("\t\t\tp.${goHttpReplyField(decl.action.name)} <- $resp")
    }
    if (decl.isReturn) {
        sb.appendLine("\t\t\treturn")
    }
    return sb.toString()
}

private fun indexPutGo(base: String, idx: String, value: String, collectionType: Type?): String =
    if (collectionType is MapType) {
        "($base).Put($idx, $value)"
    } else {
        "($base).SetAt($idx, $value)"
    }

private fun emitTransitBody(
    decl: ActionDecl,
    ctx: GoEmitCtx,
    sequential: Boolean,
    stateVarTypes: Map<String, Type> = emptyMap(),
): String {
    if (decl.transits.isEmpty()) return ""
    val sb = StringBuilder()
    var localCtx = ctx
    if (sequential) {
        for ((i, update) in decl.transits.withIndex()) {
            when (update) {
                is TransitUpdate.Let -> {
                    val bind = if (update.name.isDiscardBinding()) "__discard_t$i" else "${update.name.toGoSafeIdent()}__t"
                    sb.appendLine("$bind := ${update.init.toGoString(localCtx)}")
                    if (update.name.isDiscardBinding()) {
                        sb.appendLine("_ = $bind")
                    } else {
                        localCtx = localCtx.withLet(update.name, bind)
                    }
                }
                is TransitUpdate.Assign -> {
                    sb.appendLine("${assignTarget(update.key, localCtx)} = ${update.expr.toGoString(localCtx)}")
                }
                is TransitUpdate.IndexPut -> {
                    val base = stateOrLocal(update.collectionVar, localCtx)
                    val idx = update.index.toGoString(localCtx)
                    val value = update.value.toGoString(localCtx)
                    val ty = stateVarTypes[update.collectionVar]
                    sb.appendLine(
                        "${assignTarget(update.collectionVar, localCtx)} = " +
                            indexPutGo(base, idx, value, ty),
                    )
                }
            }
        }
        return sb.toString()
    }

    data class Pending(val key: String, val temp: String, val kind: String)
    val pending = mutableListOf<Pending>()
    for ((i, update) in decl.transits.withIndex()) {
        when (update) {
            is TransitUpdate.Let -> {
                val bind = if (update.name.isDiscardBinding()) "__discard_t$i" else "${update.name.toGoSafeIdent()}__t"
                sb.appendLine("$bind := ${update.init.toGoString(localCtx)}")
                if (update.name.isDiscardBinding()) {
                    sb.appendLine("_ = $bind")
                } else {
                    localCtx = localCtx.withLet(update.name, bind)
                }
            }
            is TransitUpdate.Assign -> {
                val temp = "__t$i"
                sb.appendLine("$temp := ${update.expr.toGoString(localCtx)}")
                pending += Pending(update.key, temp, "assign")
            }
            is TransitUpdate.IndexPut -> {
                val idxTemp = "__idx$i"
                val valTemp = "__val$i"
                sb.appendLine("$idxTemp := ${update.index.toGoString(localCtx)}")
                sb.appendLine("$valTemp := ${update.value.toGoString(localCtx)}")
                pending += Pending(update.collectionVar, "$idxTemp|$valTemp", "index")
            }
        }
    }
    for (p in pending) {
        when (p.kind) {
            "assign" -> sb.appendLine("${assignTarget(p.key, localCtx)} = ${p.temp}")
            "index" -> {
                val (idx, value) = p.temp.split("|")
                val base = stateOrLocal(p.key, localCtx)
                sb.appendLine(
                    "${assignTarget(p.key, localCtx)} = " +
                        indexPutGo(base, idx, value, stateVarTypes[p.key]),
                )
            }
        }
    }
    return sb.toString()
}

private fun assignTarget(key: String, ctx: GoEmitCtx): String {
    val parts = key.split(".")
    val root = parts[0]
    val rootGo = when {
        root in ctx.letBindings -> ctx.letBindings.getValue(root)
        root in ctx.stateVars -> "${ctx.procRecv}.$root"
        else -> "${ctx.procRecv}.$root"
    }
    if (parts.size == 1) return rootGo
    // Nested field update: for simplicity assign whole root with copy — field path
    // Go structs are values; nested updates need rebuild. Simple path: p.obj.Field = ...
    return parts.drop(1).fold(rootGo) { acc, seg -> "$acc.${seg.toGoExportedIdent()}" }
}

private fun stateOrLocal(name: String, ctx: GoEmitCtx): String = when {
    name in ctx.letBindings -> ctx.letBindings.getValue(name)
    name in ctx.stateVars -> "${ctx.procRecv}.$name"
    else -> "${ctx.procRecv}.$name"
}

private fun residualGuardGo(
    decl: ActionDecl,
    bound: BoundArgAnalysis,
    ctx: GoEmitCtx,
    skipMembershipOf: Set<String> = emptySet(),
    skipArgs: Set<String> = emptySet(),
): String {
    if (decl.guards.isEmpty()) return "true"
    val parts = mutableListOf<String>()
    for (g in decl.guards) {
        for (c in collectGuardConjuncts(g)) {
            if (bound.bindingConjuncts.containsKey(c.expr)) {
                // Still emit membership-as-check when equality won, or skip binding conjuncts
                val e = unwrapParenGo(c.expr)
                if (e is BinaryOpExprNode && e.op() == "in") {
                    val arg = (unwrapParenGo(e.lhsOperand()) as? SymbolValueExprNode)?.symbol
                    if (arg != null && arg in skipMembershipOf) continue
                    if (arg != null && arg in bound.membershipBound) continue
                } else {
                    continue // equality binding — skip
                }
            }
            // Skip conjuncts that reference skipArgs
            if (skipArgs.any { exprReferencesSymbol(c.expr, it) }) continue
            val subst = c.letEnv.entries.fold(c.expr) { acc, (n, r) -> substituteExpr(acc, n, r) }
            parts += "(${subst.toGoString(ctx)})"
        }
    }
    return if (parts.isEmpty()) "true" else parts.joinToString(" && ")
}

private fun unwrapParenGo(expr: ExprNode): ExprNode =
    if (expr is ParenExprNode) unwrapParenGo(expr.innerExpr()) else expr

private fun findEqualityRhs(decl: ActionDecl, arg: String): ExprNode? {
    for (g in decl.guards) {
        for (c in collectGuardConjuncts(g)) {
            val e = unwrapParenGo(c.expr)
            if (e is BinaryOpExprNode && e.op() == "=") {
                val lhs = unwrapParenGo(e.lhsOperand())
                val rhs = unwrapParenGo(e.rhsOperand())
                if ((lhs as? SymbolValueExprNode)?.symbol == arg) {
                    return c.letEnv.entries.fold(rhs) { acc, (n, r) -> substituteExpr(acc, n, r) }
                }
                if ((rhs as? SymbolValueExprNode)?.symbol == arg) {
                    return c.letEnv.entries.fold(lhs) { acc, (n, r) -> substituteExpr(acc, n, r) }
                }
            }
        }
    }
    return null
}

private fun findMembershipSet(decl: ActionDecl, arg: String): ExprNode? {
    for (g in decl.guards) {
        for (c in collectGuardConjuncts(g)) {
            val e = unwrapParenGo(c.expr)
            if (e is BinaryOpExprNode && e.op() == "in") {
                val lhs = unwrapParenGo(e.lhsOperand())
                val rhs = unwrapParenGo(e.rhsOperand())
                if ((lhs as? SymbolValueExprNode)?.symbol == arg) {
                    return c.letEnv.entries.fold(rhs) { acc, (n, r) -> substituteExpr(acc, n, r) }
                }
            }
        }
    }
    return null
}

private fun elementTypeOfCollection(expr: ExprNode): Type? = try {
    when (val t = expr.getType()) {
        is SetType -> t.elementType
        is ListType -> t.elementType
        is MapType -> t.keyType
        else -> null
    }
} catch (_: RuntimeException) {
    null
}
