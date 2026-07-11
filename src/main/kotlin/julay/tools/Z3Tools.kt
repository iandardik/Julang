package julay.tools

import com.microsoft.z3.*

fun Context.mkStringConst(name : String) : Expr<SeqSort<CharSort>> {
    return this.mkConst(this.mkSymbol(name), this.mkStringSort())
}

fun Context.mkSeqConst(name: String, elementSort: Sort): Expr<*> {
    return this.mkConst(this.mkSymbol(name), this.mkSeqSort(elementSort))
}

@Suppress("UNCHECKED_CAST")
fun Context.mkSeqLengthAny(seq: Expr<*>): IntExpr =
    mkLength(seq as Expr<SeqSort<Sort>>)

@Suppress("UNCHECKED_CAST")
fun Context.mkSeqNthAny(seq: Expr<*>, index: Expr<*>): Expr<*> =
    mkNth(seq as Expr<SeqSort<Sort>>, index as Expr<IntSort>)

@Suppress("UNCHECKED_CAST")
fun Context.mkSeqConcatAny(lhs: Expr<*>, rhs: Expr<*>): Expr<*> =
    mkConcat(lhs as Expr<SeqSort<Sort>>, rhs as Expr<SeqSort<Sort>>)

