package julay.compiler.pass

import julay.program.Variable
import julay.program.type.ProcFunRefType
import julay.program.type.StringType
import julay.program.type.Type

/** True when action argument lists match for composition / library pairing (procfun refs erase to String). */
fun actionArgsCompatible(left: List<Variable>, right: List<Variable>): Boolean {
    if (left.size != right.size) return false
    return left.zip(right).all { (a, b) ->
        a.name == b.name && actionArgTypesCompatible(a.type, b.type)
    }
}

internal fun actionArgTypesCompatible(a: Type, b: Type): Boolean {
    if (a == b) return true
    if (a is ProcFunRefType && b is StringType) return true
    if (b is ProcFunRefType && a is StringType) return true
    if (a is ProcFunRefType && b is ProcFunRefType) return procFunRefTypesMatch(a, b)
    return false
}
