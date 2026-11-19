package julay.compiler.pass

import julay.compiler.CompileError
import julay.compiler.OneLocCompileError
import julay.compiler.ast.*

/**
 * A matched circular pair `<P, A> C1 <G>` / `<G> C2 <A, Safe>`.
 */
data class CircularAgPair(
    val left: SpecNode,
    val right: SpecNode,
    val pName: String?,
    val aName: String,
    val gName: String,
    val safeName: String,
)

/** Collect formula-assume invariant names for an AG node (circular-aware). */
internal fun formulaAssumeNames(
    ag: AgSpecExprNode,
    invariantNames: Set<String>,
    systemDeclNames: Set<String>,
): Pair<List<String>, List<CompileError>> {
    val errors = mutableListOf<CompileError>()
    val circ = ag.circularSlots()
    if (circ != null && circ.isLeft) {
        val names = mutableListOf<String>()
        circ.pName?.let { p ->
            if (p !in invariantNames) {
                errors += OneLocCompileError(
                    ag.programLocation(),
                    "circular P \"$p\" is not a declared invariant",
                )
            } else {
                names += p
            }
        }
        if (circ.aName !in invariantNames) {
            errors += OneLocCompileError(
                ag.programLocation(),
                "circular A \"${circ.aName}\" is not a declared invariant",
            )
        } else {
            names += circ.aName
        }
        return names to errors
    }
    if (circ != null && circ.isRight()) {
        val g = circ.gName
        if (g == null) {
            errors += OneLocCompileError(ag.programLocation(), "circular right premise missing G")
            return emptyList<String>() to errors
        }
        if (g !in invariantNames) {
            errors += OneLocCompileError(
                ag.programLocation(),
                "circular G \"$g\" is not a declared invariant",
            )
            return emptyList<String>() to errors
        }
        return listOf(g) to errors
    }
    val (one, err) = formulaAssumeInvariantName(ag.assumeExpr(), invariantNames, systemDeclNames)
    err?.let { errors += it }
    return listOfNotNull(one) to errors
}

/** Guarantee invariant names for an AG node (circular-aware). */
internal fun guaranteeInvariantNames(
    ag: AgSpecExprNode,
    invariantNames: Set<String>,
): Pair<List<String>, List<CompileError>> {
    val errors = mutableListOf<CompileError>()
    // LocalInv |= <A> C <G>: check LocalInv (implies contract G / A∧Safe).
    ag.localInvName()?.let { local ->
        if (local !in invariantNames) {
            errors += OneLocCompileError(
                ag.programLocation(),
                "local inductive invariant \"$local\" is not a declared invariant",
            )
            return emptyList<String>() to errors
        }
        // Still validate circular A/G/Safe names exist for CircSide pairing.
        val circ = ag.circularSlots()
        if (circ != null && circ.isRight()) {
            listOfNotNull(circ.aName, circ.safeName).forEach { n ->
                if (n !in invariantNames) {
                    errors += OneLocCompileError(
                        ag.programLocation(),
                        "circular guarantee \"$n\" is not a declared invariant",
                    )
                }
            }
        } else if (circ != null && circ.isLeft) {
            val g = circ.gName
            if (g == null || g !in invariantNames) {
                errors += OneLocCompileError(
                    ag.programLocation(),
                    "circular left guarantee G must be a named invariant",
                )
            }
        }
        return listOf(local) to errors
    }
    val circ = ag.circularSlots()
    if (circ != null && circ.isRight()) {
        val safe = circ.safeName
        if (safe == null) {
            errors += OneLocCompileError(ag.programLocation(), "circular right premise missing Safe")
            return emptyList<String>() to errors
        }
        listOf(circ.aName, safe).forEach { n ->
            if (n !in invariantNames) {
                errors += OneLocCompileError(
                    ag.programLocation(),
                    "circular guarantee \"$n\" is not a declared invariant",
                )
            }
        }
        return listOf(circ.aName, safe).filter { it in invariantNames } to errors
    }
    if (circ != null && circ.isLeft) {
        val g = circ.gName
        if (g == null || g !in invariantNames) {
            errors += OneLocCompileError(
                ag.programLocation(),
                "circular left guarantee G must be a named invariant",
            )
            return emptyList<String>() to errors
        }
        return listOf(g) to errors
    }
    val g = ag.guaranteeExpr() ?: return emptyList<String>() to errors
    return if (g is SymbolValueExprNode && g.symbol in invariantNames) {
        listOf(g.symbol) to errors
    } else {
        emptyList<String>() to errors // inline formula; handled elsewhere
    }
}

/**
 * Require A/G slots of circular premises to be fluent-only invariants
 * (no leaf state in the invariant closure).
 */
internal fun circularFluentSlotErrors(
    ag: AgSpecExprNode,
    invariants: Map<String, InvariantNode>,
    fluents: Map<String, FluentNode>,
): List<CompileError> {
    val circ = ag.circularSlots() ?: return emptyList()
    val errors = mutableListOf<CompileError>()
    fun checkFluentOnly(name: String, role: String) {
        val inv = invariants[name] ?: return
        val stateRefs = collectInvariantLeafBases(inv.invariantFormula())
        if (stateRefs.isNotEmpty()) {
            errors += OneLocCompileError(
                inv.programLocation(),
                "circular $role \"$name\" must be a fluent invariant (no leaf state refs: $stateRefs)",
            )
        }
        val used = fluentsUsedByInvariant(inv, invariants, fluents)
        if (used.isEmpty()) {
            errors += OneLocCompileError(
                inv.programLocation(),
                "circular $role \"$name\" must reference at least one fluent",
            )
        }
    }
    checkFluentOnly(circ.aName, "A")
    // On left premise G is the guarantee; on right, G is the assume — always check when present.
    circ.gName?.let { checkFluentOnly(it, "G") }
    return errors
}

/** Match left/right circular specs that share A and G. */
internal fun findCircularAgPairs(specs: List<SpecNode>): List<CircularAgPair> {
    val lefts = mutableListOf<SpecNode>()
    val rights = mutableListOf<SpecNode>()
    specs.forEach { sp ->
        val ag = sp.specNodeValue() as? AgSpecExprNode ?: return@forEach
        val c = ag.circularSlots() ?: return@forEach
        if (c.isLeft) lefts += sp else rights += sp
    }
    val pairs = mutableListOf<CircularAgPair>()
    lefts.forEach { left ->
        val lc = (left.specNodeValue() as AgSpecExprNode).circularSlots()!!
        val g = lc.gName ?: return@forEach
        rights.forEach { right ->
            val rc = (right.specNodeValue() as AgSpecExprNode).circularSlots()!!
            if (lc.aName == rc.aName && g == rc.gName && rc.safeName != null) {
                pairs += CircularAgPair(
                    left = left,
                    right = right,
                    pName = lc.pName,
                    aName = lc.aName,
                    gName = g,
                    safeName = rc.safeName,
                )
            }
        }
    }
    return pairs
}
