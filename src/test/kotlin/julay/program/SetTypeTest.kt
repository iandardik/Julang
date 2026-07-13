package julay.program

import com.microsoft.z3.Context
import com.microsoft.z3.Status
import julay.tools.mkSetMemberAny
import kotlin.test.Test
import kotlin.test.assertEquals

class SetTypeTest {
    @Test
    fun intSetRoundTrip() {
        val ctx = Context()
        val st = setType(intType)
        val value = Value(setOf(1, 2, 3), st)
        val z3 = st.toZ3Expr(value, ctx)
        val solver = ctx.mkSolver()
        val v = st.toZ3Expr(Variable("s", st), ctx)
        solver.add(ctx.mkEq(v, z3))
        assertEquals(Status.SATISFIABLE, solver.check())
        val restored = st.fromZ3Expr(solver.model.eval(v, true), solver.model) as Set<*>
        assertEquals(setOf(1, 2, 3), restored)
    }

    @Test
    fun emptyIntSetRoundTrip() {
        val ctx = Context()
        val st = setType(intType)
        val z3 = st.toZ3Expr(Value(emptySet<Int>(), st), ctx)
        val solver = ctx.mkSolver()
        val v = st.toZ3Expr(Variable("s", st), ctx)
        solver.add(ctx.mkEq(v, z3))
        assertEquals(Status.SATISFIABLE, solver.check())
        assertEquals(emptySet<Int>(), st.fromZ3Expr(solver.model.eval(v, true), solver.model))
    }

    @Test
    fun stringSetMembershipInModel() {
        val ctx = Context()
        val st = setType(stringType)
        val z3 = st.toZ3Expr(Value(setOf("a", "b"), st), ctx)
        val solver = ctx.mkSolver()
        val v = st.toZ3Expr(Variable("s", st), ctx)
        solver.add(ctx.mkEq(v, z3))
        assertEquals(Status.SATISFIABLE, solver.check())
        val model = solver.model
        val cell = model.eval(v, true)
        val meta = st.cellMetadata(ctx)
        val arr = model.eval(ctx.mkApp(meta.arrAccessor, cell), true)
        kotlin.test.assertTrue(
            model.eval(ctx.mkSetMemberAny(ctx.mkString("a"), arr), true).isTrue,
        )
        kotlin.test.assertTrue(
            model.eval(ctx.mkSetMemberAny(ctx.mkString("b"), arr), true).isTrue,
        )
    }
}
