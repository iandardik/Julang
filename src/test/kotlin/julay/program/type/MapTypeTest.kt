package julay.program.type

import julay.program.Value
import julay.program.Variable
import com.microsoft.z3.Context
import com.microsoft.z3.Status
import julay.tools.mapSelectExpr
import julay.tools.mkSetMemberAny
import kotlin.test.Test
import kotlin.test.assertEquals

class MapTypeTest {
    @Test
    fun stringIntMapMembershipInModel() {
        val ctx = Context()
        val mt = mapType(stringType, intType)
        val z3 = mt.toZ3Expr(Value(mapOf("a" to 1, "b" to 2), mt), ctx)
        val solver = ctx.mkSolver()
        val v = mt.toZ3Expr(Variable("mp", mt), ctx)
        solver.add(ctx.mkEq(v, z3))
        assertEquals(Status.SATISFIABLE, solver.check())
        val model = solver.model
        val cell = model.eval(v, true)
        val meta = mt.cellMetadata(ctx)
        val arr = model.eval(ctx.mkApp(meta.arrAccessor, cell), true)
        val keys = model.eval(ctx.mkApp(meta.keysAccessor, cell), true)
        kotlin.test.assertTrue(
            model.eval(ctx.mkSetMemberAny(ctx.mkString("a"), keys), true).isTrue,
        )
        assertEquals(
            1,
            intType.fromZ3Expr(
                model.eval(mapSelectExpr(ctx, arr, ctx.mkString("a")), true),
                model,
            ),
        )
    }

    @Test
    fun emptyStringIntMapRoundTrip() {
        val ctx = Context()
        val mt = mapType(stringType, intType)
        val z3 = mt.toZ3Expr(Value(emptyMap<String, Int>(), mt), ctx)
        val solver = ctx.mkSolver()
        val v = mt.toZ3Expr(Variable("mp", mt), ctx)
        solver.add(ctx.mkEq(v, z3))
        assertEquals(Status.SATISFIABLE, solver.check())
        assertEquals(emptyMap<String, Int>(), mt.fromZ3Expr(solver.model.eval(v, true), solver.model))
    }

    @Test
    fun intIntMapRoundTrip() {
        val ctx = Context()
        val mt = mapType(intType, intType)
        val value = Value(mapOf(1 to 10, 2 to 20), mt)
        val z3 = mt.toZ3Expr(value, ctx)
        val solver = ctx.mkSolver()
        val v = mt.toZ3Expr(Variable("mp", mt), ctx)
        solver.add(ctx.mkEq(v, z3))
        assertEquals(Status.SATISFIABLE, solver.check())
        @Suppress("UNCHECKED_CAST")
        val restored = mt.fromZ3Expr(solver.model.eval(v, true), solver.model) as Map<Int, Int>
        assertEquals(mapOf(1 to 10, 2 to 20), restored)
    }
}
