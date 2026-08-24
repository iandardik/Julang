package julay.program.type

import julay.program.Value
import julay.program.Variable
import com.microsoft.z3.Context
import com.microsoft.z3.Expr
import com.microsoft.z3.Status
import com.microsoft.z3.julangContext
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
        @Suppress("UNCHECKED_CAST")
        val restored = st.fromZ3Expr(model.eval(v, true), model) as Set<String>
        assertEquals(setOf("a", "b"), restored)
    }

    @Test
    fun objSetRoundTrip() {
        val ctx = Context()
        lateinit var pointType: ObjClassType
        pointType = ObjClassType(
            "Point",
            listOf(Variable("x", intType), Variable("y", intType)),
            { value, c ->
                val p = value.value as Point
                pointType.constructorDecl(c).apply(c.mkInt(p.x), c.mkInt(p.y)) as com.microsoft.z3.Expr<*>
            },
            { expr, model ->
                val fieldExprs = if (expr.isApp && expr.funcDecl.name.toString() == pointType.constructorName) {
                    expr.args
                } else {
                    val c = model.julangContext()
                    arrayOf(
                        pointType.accessor(c, 0).apply(expr) as Expr<*>,
                        pointType.accessor(c, 1).apply(expr) as Expr<*>,
                    )
                }
                Point(
                    intType.fromZ3Expr(fieldExprs[0], model) as Int,
                    intType.fromZ3Expr(fieldExprs[1], model) as Int,
                )
            },
        )
        val st = setType(pointType)
        val value = Value(setOf(Point(1, 2), Point(3, 4)), st)
        val z3 = st.toZ3Expr(value, ctx)
        val solver = ctx.mkSolver()
        val v = st.toZ3Expr(Variable("s", st), ctx)
        solver.add(ctx.mkEq(v, z3))
        assertEquals(Status.SATISFIABLE, solver.check())
        @Suppress("UNCHECKED_CAST")
        val restored = st.fromZ3Expr(solver.model.eval(v, true), solver.model) as Set<Point>
        assertEquals(setOf(Point(1, 2), Point(3, 4)), restored)
    }
}
