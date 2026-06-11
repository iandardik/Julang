package julay.program

import com.microsoft.z3.Context
import com.microsoft.z3.Model
import julay.ast.ObjClassType
import julay.tools.assert

class ConcreteAction {
    val symAction : SymbolicAction
    private val argAssignments : Map<Variable,Value>

    constructor(sig : SymbolicAction, ctx : Context, model : Model) {
        symAction = sig
        // The reason for the flatMap: primitive args correspond to a single variable, and are
        // therefore associated with a single concrete value. However, o-class args desguar into
        // multiple variables, and therefore may be associated with multiple concrete values.
        argAssignments = sig.args.flatMap { v ->
            when (val ty = v.type) {
                is ObjClassType -> ty.flattenArgAssignments(v.name, model, ctx).toList()
                else -> {
                    val z3Value = model.eval(ty.toZ3Expr(v, ctx), true)
                    listOf(v to Value(z3Value, ty))
                }
            }
        }.toMap()
    }

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
