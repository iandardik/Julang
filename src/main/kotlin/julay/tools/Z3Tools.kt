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

@Suppress("UNCHECKED_CAST")
fun Context.mkSeqExtractAny(seq: Expr<*>, offset: Expr<*>, length: Expr<*>): Expr<*> =
    mkExtract(seq as Expr<SeqSort<Sort>>, offset as IntExpr, length as IntExpr)

@Suppress("UNCHECKED_CAST")
fun Context.mkSetMemberAny(elem: Expr<*>, set: Expr<*>): BoolExpr =
    mkSetMembership(elem as Expr<Sort>, set as ArrayExpr<Sort, BoolSort>)

@Suppress("UNCHECKED_CAST")
fun Context.mkSetAddAny(set: Expr<*>, elem: Expr<*>): Expr<*> =
    mkSetAdd(set as ArrayExpr<Sort, BoolSort>, elem as Expr<Sort>)

@Suppress("UNCHECKED_CAST")
fun Context.mkSetUnionAny(lhs: Expr<*>, rhs: Expr<*>): Expr<*> =
    mkSetUnion(lhs as ArrayExpr<Sort, BoolSort>, rhs as ArrayExpr<Sort, BoolSort>)

@Suppress("UNCHECKED_CAST")
fun Context.mkSetDifferenceAny(lhs: Expr<*>, rhs: Expr<*>): Expr<*> =
    mkSetDifference(lhs as ArrayExpr<Sort, BoolSort>, rhs as ArrayExpr<Sort, BoolSort>)

@Suppress("UNCHECKED_CAST")
fun Context.mkListMemberAny(elem: Expr<*>, seq: Expr<*>): BoolExpr {
    val seqExpr = seq as Expr<SeqSort<Sort>>
    val i = mkIntConst("list_member_i")
    return mkExists(
        arrayOf(intSort),
        arrayOf(mkSymbol("list_member_i")),
        mkAnd(
            mkGe(i, mkInt(0)),
            mkLt(i, mkLength(seqExpr)),
            mkEq(mkNth(seqExpr, i), elem as Expr<Sort>),
        ),
        1,
        null,
        null,
        mkSymbol("Q"),
        mkSymbol(""),
    )
}

fun mapCellArrExpr(ctx: Context, cell: Expr<*>, arrAccessor: FuncDecl<*>): Expr<*> =
    ctx.mkApp(arrAccessor, cell)

fun mapCellKeysExpr(ctx: Context, cell: Expr<*>, keysAccessor: FuncDecl<*>): Expr<*> =
    ctx.mkApp(keysAccessor, cell)

fun mapCellSizeExpr(ctx: Context, cell: Expr<*>, sizeAccessor: FuncDecl<*>): IntExpr =
    ctx.mkApp(sizeAccessor, cell) as IntExpr

@Suppress("UNCHECKED_CAST")
fun mapSelectExpr(ctx: Context, arr: Expr<*>, key: Expr<*>): Expr<*> =
    ctx.mkSelect(arr as ArrayExpr<Sort, Sort>, key as Expr<Sort>)

@Suppress("UNCHECKED_CAST")
fun mapStoreExpr(ctx: Context, arr: Expr<*>, key: Expr<*>, value: Expr<*>): Expr<*> =
    ctx.mkStore(arr as ArrayExpr<Sort, Sort>, key as Expr<Sort>, value as Expr<Sort>)

@Suppress("UNCHECKED_CAST")
fun mapSetAddExpr(ctx: Context, keys: Expr<*>, key: Expr<*>): ArrayExpr<Sort, BoolSort> =
    ctx.mkSetAdd(keys as ArrayExpr<Sort, BoolSort>, key as Expr<Sort>)

fun mapMkCellExpr(
    ctx: Context,
    constructorDecl: FuncDecl<*>,
    arr: Expr<*>,
    keys: Expr<*>,
    size: Expr<*>,
): Expr<*> = ctx.mkApp(constructorDecl, arr, keys, size)

fun setCellArrExpr(ctx: Context, cell: Expr<*>, arrAccessor: FuncDecl<*>): Expr<*> =
    ctx.mkApp(arrAccessor, cell)

fun setCellSizeExpr(ctx: Context, cell: Expr<*>, sizeAccessor: FuncDecl<*>): IntExpr =
    ctx.mkApp(sizeAccessor, cell) as IntExpr

fun setMkCellExpr(
    ctx: Context,
    constructorDecl: FuncDecl<*>,
    arr: Expr<*>,
    size: Expr<*>,
): Expr<*> = ctx.mkApp(constructorDecl, arr, size)
