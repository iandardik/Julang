package julay.program

import com.microsoft.z3.Context
import com.microsoft.z3.Model

class ConcreteAction {
    val symAction : SymbolicAction
    private val argAssignments : Map<Variable,Value>

    constructor(sig : SymbolicAction, ctx : Context, model : Model) {
        symAction = sig
        argAssignments = sig.args.associateWith { v ->
            val z3Value = model.eval(v.toZ3Expr(ctx), true)
            val kotlinValue = v.type.fromZ3Expr(z3Value)
            Value(kotlinValue, v.type)
        }
    }

    fun hasArg(arg : Variable) : Boolean {
        return arg in argAssignments
    }

    fun lookup(variable : Variable) : Value {
        julay.tools.assert(variable in argAssignments, "ConcreteAction.lookup($variable): is not assigned a value!")
        return argAssignments[variable]!!
    }

    override fun toString() : String {
        return "ConcreteAction($symAction): $argAssignments"
    }
}
