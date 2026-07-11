package julay.program

import com.microsoft.z3.Context
import com.microsoft.z3.Status
import kotlin.test.Test
import kotlin.test.assertEquals

class ListTypeTest {
    @Test
    fun nestedListFromZ3ExprViaModel() {
        val ctx = Context()
        val nested = listType(listType(intType))
        val value = Value(listOf(listOf(1, 2), listOf(3)), nested)
        val z3 = nested.toZ3Expr(value, ctx)
        val solver = ctx.mkSolver()
        val v = nested.toZ3Expr(Variable("xs", nested), ctx)
        solver.add(ctx.mkEq(v, z3))
        assertEquals(Status.SATISFIABLE, solver.check())
        val model = solver.model
        val restored = nested.fromZ3Expr(model.eval(v, true), model)
        assertEquals(listOf(listOf(1, 2), listOf(3)), restored)
    }
}
