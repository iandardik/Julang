package julay.program

import com.microsoft.z3.Context
import com.microsoft.z3.Status
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

data class Point(val x: Int, val y: Int)

data class Line(val start: Point, val end: Point)

class ObjClassTypeTest {
    private val pointType = ObjClassType(
        "Point",
        listOf(
            Variable("x", intType),
            Variable("y", intType),
        ),
    )

    private val lineType = ObjClassType(
        "Line",
        listOf(
            Variable("start", pointType),
            Variable("end", pointType),
        ),
    )

    @Test
    fun roundTripPointThroughZ3() {
        val ctx = Context()
        val original = Point(3, 7)
        val z3Value = pointType.stateToZ3(ctx, original)
        val restored = pointType.fromZ3ExprWithContext(z3Value, ctx) as Point
        assertEquals(original, restored)
    }

    @Test
    fun roundTripNestedLineThroughZ3() {
        val ctx = Context()
        val original = Line(Point(1, 2), Point(3, 4))
        val z3Value = lineType.stateToZ3(ctx, original)
        val restored = lineType.fromZ3ExprWithContext(z3Value, ctx) as Line
        assertEquals(original, restored)
    }

    @Test
    fun translateBetweenContexts() {
        val ctx1 = Context()
        val ctx2 = Context()

        val p1 = ctx1.mkConst("p", pointType.sort(ctx1))
        val p2 = ctx2.mkConst("p", pointType.sort(ctx2))
        val solver = ctx2.mkSolver()
        solver.add(ctx1.mkEq(p1, pointType.stateToZ3(ctx1, Point(9, 10))).translate(ctx2))
        solver.add(ctx2.mkEq(p2, pointType.stateToZ3(ctx2, Point(9, 10))))
        assertEquals(Status.SATISFIABLE, solver.check())
        val model = solver.model
        val value = pointType.fromZ3ExprWithContext(model.eval(p2, true), ctx1) as Point
        assertEquals(Point(9, 10), value)
    }

    @Test
    fun copyAssignmentStringNested() {
        val result = copyAssignmentString(
            "segment",
            lineType,
            listOf("start", "x"),
            "expr",
        )
        assertEquals(
            "segment = segment.copy(start = segment.start.copy(x = expr))",
            result,
        )
    }

    @Test
    fun isOfTypeRecognizesDataClass() {
        assertTrue(pointType.isOfType(Point(0, 0)))
    }
}
