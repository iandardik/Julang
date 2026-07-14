package julay.program

import io.github.cvc5.Solver
import io.github.cvc5.Term
import io.github.cvc5.TermManager
import julay.tools.findDeclaredConst

class ConcreteAction {
    val symAction: SymbolicAction
    private val argAssignments: Map<Variable, Value>

    constructor(sig: SymbolicAction, solver: Solver, declaredTerms: Array<Term>) {
        symAction = sig
        argAssignments = sig.args.associateWith { v ->
            val term = findDeclaredConst(declaredTerms, v.name)
            val kotlinValue = v.type.fromSmtTerm(term, solver)
            Value(kotlinValue, v.type)
        }
    }

    constructor(sig: SymbolicAction, assignments: Map<Variable, Value>) {
        julay.tools.assert(
            sig.args.toSet() == assignments.keys,
            "ConcreteAction: assignment keys must match symbolic action args exactly",
        )
        symAction = sig
        argAssignments = assignments
    }

    fun hasArg(arg: Variable): Boolean {
        return arg in argAssignments
    }

    fun lookup(variable: Variable): Value {
        julay.tools.assert(variable in argAssignments, "ConcreteAction.lookup($variable): is not assigned a value!")
        return argAssignments[variable]!!
    }

    override fun toString(): String {
        return "ConcreteAction($symAction): $argAssignments"
    }
}
