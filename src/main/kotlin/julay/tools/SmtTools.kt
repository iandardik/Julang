package julay.tools

import io.github.cvc5.Command
import io.github.cvc5.Datatype
import io.github.cvc5.InputParser
import io.github.cvc5.Kind
import io.github.cvc5.Solver
import io.github.cvc5.Sort
import io.github.cvc5.Term
import io.github.cvc5.TermManager
import io.github.cvc5.modes.InputLanguage
import julay.program.ConcreteAction
import julay.program.SymbolicAction
import julay.program.smtLibDeclarations
import julay.program.smtLibSymbol
import julay.program.toSmtLibSort

/**
 * Serializable SMT constraint for SyncChannel: self-contained declarations plus a boolean formula.
 * Cross-process exchange must use this (or equivalent SMT-LIB), never live [Term] handles.
 */
data class SmtConstraint(
    val declarations: List<String>,
    val formula: String,
) {
    companion object {
        fun from(term: Term): SmtConstraint {
            val decls = linkedMapOf<String, String>()
            collectDeclarations(term, decls)
            return SmtConstraint(decls.values.toList(), term.toString())
        }
    }
}

fun newModelSolver(tm: TermManager = TermManager()): Solver {
    val solver = Solver(tm)
    solver.setOption("produce-models", "true")
    solver.setOption("arrays-exp", "true")
    solver.setLogic("ALL")
    return solver
}

fun Solver.isSat(): Boolean = checkSat().isSat

/**
 * Fresh solver parse of merged SyncChannel constraints.
 * [ensureVars] are action args that must appear in the model even if absent from constraints.
 * [use] runs while the model is live; only objects owned by this solve are freed afterward
 * (no JVM-global Context snapshot / no cross-Proc lock).
 */
fun <R> withSolveConstraints(
    constraints: Collection<SmtConstraint>,
    ensureVars: List<julay.program.Variable> = emptyList(),
    use: (Solver, Array<Term>) -> R,
): R? {
    val declMap = linkedMapOf<String, String>()
    for (c in constraints) {
        for (d in c.declarations) {
            declMap.putIfAbsent(d, d)
        }
    }
    for (v in ensureVars) {
        for (d in v.type.smtLibDeclarations()) {
            declMap.putIfAbsent(d, d)
        }
        val constDecl =
            "(declare-fun ${smtLibSymbol(v.name)} () ${v.type.toSmtLibSort()})"
        declMap.putIfAbsent(constDecl, constDecl)
    }
    val script = buildString {
        appendLine("(set-logic ALL)")
        declMap.values.forEach { appendLine(it) }
        if (constraints.isEmpty()) {
            appendLine("(assert true)")
        } else {
            constraints.forEach { appendLine("(assert ${it.formula})") }
        }
    }

    val tm = TermManager()
    val solver = Solver(tm)
    val parser = InputParser(solver)
    val commands = mutableListOf<Command>()
    var declaredTerms: Array<Term> = emptyArray()
    try {
        solver.setOption("produce-models", "true")
        solver.setOption("arrays-exp", "true")
        parser.setStringInput(InputLanguage.SMT_LIB_2_6, script, "julang-constraints")
        while (true) {
            val cmd = parser.nextCommand()
            if (cmd.isNull) break
            commands.add(cmd)
            cmd.invoke(solver, parser.symbolManager)
        }
        if (!solver.checkSat().isSat) {
            return null
        }
        declaredTerms = parser.symbolManager.declaredTerms
        return use(solver, declaredTerms)
    } finally {
        disposeOwnedSolve(declaredTerms, commands, parser, solver, tm)
    }
}

/** True iff [constraints] are mutually satisfiable; always disposes the ephemeral solver. */
fun constraintsAreSat(
    constraints: Collection<SmtConstraint>,
    ensureVars: List<julay.program.Variable> = emptyList(),
): Boolean = withSolveConstraints(constraints, ensureVars) { _, _ -> true } != null

/**
 * SyncChannel solve: extract Kotlin arg values, then free this solve's owned CVC5 natives.
 */
fun solveToConcreteAction(
    constraints: Collection<SmtConstraint>,
    act: SymbolicAction,
): ConcreteAction? = withSolveConstraints(constraints, act.args) { solver, declared ->
    ConcreteAction(act, solver, declared)
}

/**
 * Free only objects this ephemeral solve created (not a JVM-global Context sweep).
 * Order: declared consts → parse commands → parser → solver → term manager.
 */
private fun disposeOwnedSolve(
    declaredTerms: Array<Term>,
    commands: List<Command>,
    parser: InputParser,
    solver: Solver,
    tm: TermManager,
) {
    for (i in declaredTerms.indices.reversed()) {
        safeDeletePointer(declaredTerms[i])
    }
    for (i in commands.indices.reversed()) {
        safeDeletePointer(commands[i])
    }
    safeDeletePointer(parser)
    safeDeletePointer(solver)
    safeDeletePointer(tm)
}

private fun safeDeletePointer(obj: Any) {
    try {
        val m = obj.javaClass.methods.find { it.name == "deletePointer" && it.parameterCount == 0 }
            ?: return
        m.invoke(obj)
    } catch (_: Exception) {
    }
}

fun findDeclaredConst(declaredTerms: Array<Term>, name: String): Term {
    for (t in declaredTerms) {
        if (t.hasSymbol() && t.symbol == name) return t
    }
    throw RuntimeException("Declared symbol not found: $name")
}

fun TermManager.mkStringConst(name: String): Term =
    mkConst(stringSort, name)

/** Build a string term from an arbitrary Kotlin string (supports newlines/tabs). */
fun TermManager.mkKotlinString(s: String): Term {
    val escaped = buildString {
        for (ch in s) {
            if (ch == '"' || ch == '\\' || ch.code < 32) {
                append("\\u{%x}".format(ch.code))
            } else {
                append(ch)
            }
        }
    }
    return mkString(escaped, true)
}

fun TermManager.mkSeqConst(name: String, elementSort: Sort): Term =
    mkConst(mkSequenceSort(elementSort), name)

fun TermManager.mkSeqLength(seq: Term): Term =
    mkTerm(Kind.SEQ_LENGTH, seq)

fun TermManager.mkSeqNth(seq: Term, index: Term): Term =
    mkTerm(Kind.SEQ_NTH, seq, index)

fun TermManager.mkSeqConcat(lhs: Term, rhs: Term): Term =
    mkTerm(Kind.SEQ_CONCAT, lhs, rhs)

fun TermManager.mkSeqConcat(parts: List<Term>): Term {
    require(parts.isNotEmpty())
    return parts.reduce { a, b -> mkSeqConcat(a, b) }
}

fun TermManager.mkSeqExtract(seq: Term, offset: Term, length: Term): Term =
    mkTerm(Kind.SEQ_EXTRACT, seq, offset, length)

fun TermManager.mkSetMember(elem: Term, set: Term): Term =
    mkTerm(Kind.SET_MEMBER, elem, set)

fun TermManager.mkSetAdd(set: Term, elem: Term): Term =
    mkTerm(Kind.SET_INSERT, elem, set)

fun TermManager.mkSetUnion(lhs: Term, rhs: Term): Term =
    mkTerm(Kind.SET_UNION, lhs, rhs)

fun TermManager.mkSetDifference(lhs: Term, rhs: Term): Term =
    mkTerm(Kind.SET_MINUS, lhs, rhs)

fun TermManager.mkListMember(elem: Term, seq: Term): Term {
    val i = mkVar(integerSort, "list_member_i")
    val body = mkTerm(
        Kind.AND,
        mkTerm(Kind.GEQ, i, mkInteger(0)),
        mkTerm(Kind.LT, i, mkSeqLength(seq)),
        mkTerm(Kind.EQUAL, mkSeqNth(seq, i), elem),
    )
    return mkTerm(Kind.EXISTS, mkTerm(Kind.VARIABLE_LIST, i), body)
}

fun applyConstructor(tm: TermManager, constructorTerm: Term, args: Array<Term>): Term {
    if (args.isEmpty()) {
        return tm.mkTerm(Kind.APPLY_CONSTRUCTOR, constructorTerm)
    }
    val children = Array(args.size + 1) { idx ->
        if (idx == 0) constructorTerm else args[idx - 1]
    }
    return tm.mkTerm(Kind.APPLY_CONSTRUCTOR, children)
}

fun applySelector(tm: TermManager, selectorTerm: Term, record: Term): Term =
    tm.mkTerm(Kind.APPLY_SELECTOR, selectorTerm, record)

fun mapCellArrExpr(tm: TermManager, cell: Term, arrSelector: Term): Term =
    applySelector(tm, arrSelector, cell)

fun mapCellKeysExpr(tm: TermManager, cell: Term, keysSelector: Term): Term =
    applySelector(tm, keysSelector, cell)

fun mapCellSizeExpr(tm: TermManager, cell: Term, sizeSelector: Term): Term =
    applySelector(tm, sizeSelector, cell)

fun mapSelectExpr(tm: TermManager, arr: Term, key: Term): Term =
    tm.mkTerm(Kind.SELECT, arr, key)

fun mapStoreExpr(tm: TermManager, arr: Term, key: Term, value: Term): Term =
    tm.mkTerm(Kind.STORE, arr, key, value)

fun mapSetAddExpr(tm: TermManager, keys: Term, key: Term): Term =
    tm.mkSetAdd(keys, key)

fun mapMkCellExpr(
    tm: TermManager,
    constructorTerm: Term,
    arr: Term,
    keys: Term,
    size: Term,
): Term = applyConstructor(tm, constructorTerm, arrayOf(arr, keys, size))

fun setCellArrExpr(tm: TermManager, cell: Term, arrSelector: Term): Term =
    applySelector(tm, arrSelector, cell)

fun setCellSizeExpr(tm: TermManager, cell: Term, sizeSelector: Term): Term =
    applySelector(tm, sizeSelector, cell)

fun setMkCellExpr(
    tm: TermManager,
    constructorTerm: Term,
    arr: Term,
    size: Term,
): Term = applyConstructor(tm, constructorTerm, arrayOf(arr, size))

private fun collectDeclarations(term: Term, decls: MutableMap<String, String>) {
    ensureSortDeclared(term.sort, decls)
    if (term.kind == Kind.CONSTANT && term.hasSymbol()) {
        val name = term.symbol
        decls.putIfAbsent(
            "const:$name",
            "(declare-fun ${smtLibSymbol(name)} () ${sortToSmtLib(term.sort)})",
        )
    }
    for (i in 0 until term.numChildren) {
        collectDeclarations(term.getChild(i), decls)
    }
}

private fun ensureSortDeclared(sort: Sort, decls: MutableMap<String, String>) {
    when {
        sort.isDatatype -> {
            val dt = sort.datatype
            val key = "dt:${dt.name}"
            if (key !in decls) {
                for (ctor in dt) {
                    for (sel in ctor) {
                        ensureSortDeclared(sel.codomainSort, decls)
                    }
                }
                decls[key] = datatypeToSmtLib(dt)
            }
        }
        sort.isSet -> ensureSortDeclared(sort.setElementSort, decls)
        sort.isSequence -> ensureSortDeclared(sort.sequenceElementSort, decls)
        sort.isArray -> {
            ensureSortDeclared(sort.arrayIndexSort, decls)
            ensureSortDeclared(sort.arrayElementSort, decls)
        }
    }
}

private fun datatypeToSmtLib(dt: Datatype): String {
    val ctors = dt.joinToString(" ") { ctor ->
        val sels = ctor.joinToString(" ") { sel ->
            "(${smtLibSymbol(sel.name)} ${sortToSmtLib(sel.codomainSort)})"
        }
        if (sels.isEmpty()) "(${smtLibSymbol(ctor.name)})" else "(${smtLibSymbol(ctor.name)} $sels)"
    }
    return "(declare-datatypes ((${smtLibSymbol(dt.name)} 0)) (($ctors)))"
}

fun sortToSmtLib(sort: Sort): String = when {
    sort.isBoolean -> "Bool"
    sort.isInteger -> "Int"
    sort.isReal -> "Real"
    sort.isString -> "String"
    sort.isSet -> "(Set ${sortToSmtLib(sort.setElementSort)})"
    sort.isSequence -> "(Seq ${sortToSmtLib(sort.sequenceElementSort)})"
    sort.isArray -> "(Array ${sortToSmtLib(sort.arrayIndexSort)} ${sortToSmtLib(sort.arrayElementSort)})"
    sort.isDatatype -> smtLibSymbol(sort.datatype.name)
    else -> sort.toString()
}
