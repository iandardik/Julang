package julay.program

import com.microsoft.z3.Context
import com.microsoft.z3.Model
import julay.tools.assert

class ConcreteAction(
    val symAction : SymbolicAction,
    private val argAssignments : Map<Variable,Value>
) {
    constructor(sig : SymbolicAction, ctx : Context, model : Model)
        : this(sig, sig.args.associateWith { v ->
            val z3Value = model.eval(v.type.toZ3Expr(v,ctx), true)
            Value(z3Value, v.type)
        }) {}

    fun hasArg(arg : Variable) : Boolean {
        return arg in argAssignments
    }

    fun lookup(variable : Variable) : Value {
        assert(variable in argAssignments, "ConcreteAction.lookup($variable): is not assigned a value!")
        return argAssignments[variable]!!
    }

    override fun toString() : String {
        return "ConcreteAction($symAction): $argAssignments"
    }
}

fun emptyConcreteAction() : ConcreteAction {
    return ConcreteAction(SymbolicAction("",emptyList()), emptyMap())
}