package julay.program

import com.microsoft.z3.Context
import com.microsoft.z3.Expr
import com.microsoft.z3.Status
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

data class Point(val x: Int, val y: Int)

data class Line(val start: Point, val end: Point)

class ObjClassTypeTest {
    private lateinit var pointType: ObjClassType
    private lateinit var lineType: ObjClassType

    init {
        pointType = ObjClassType(
            "Point",
            listOf(
                Variable("x", intType),
                Variable("y", intType),
            ),
            { value, ctx -> pointToZ3(ctx, value.value as Point) },
            { expr -> pointFromZ3(expr) },
        )
        lineType = ObjClassType(
            "Line",
            listOf(
                Variable("start", pointType),
                Variable("end", pointType),
            ),
            { value, ctx -> lineToZ3(ctx, value.value as Line) },
            { expr -> lineFromZ3(expr) },
        )
    }

    private fun pointToZ3(ctx: Context, value: Point): Expr<*> =
        pointType.mkConstructorZ3(ctx, ctx.mkInt(value.x), ctx.mkInt(value.y))

    private fun pointFromZ3(expr: Expr<*>): Point {
        val fieldExprs = pointType.fieldExprsFromZ3(expr)
        return Point(
            intType.fromZ3Expr(fieldExprs[0]) as Int,
            intType.fromZ3Expr(fieldExprs[1]) as Int,
        )
    }

    private fun lineToZ3(ctx: Context, value: Line): Expr<*> =
        lineType.mkConstructorZ3(ctx, pointToZ3(ctx, value.start), pointToZ3(ctx, value.end))

    private fun lineFromZ3(expr: Expr<*>): Line {
        val fieldExprs = lineType.fieldExprsFromZ3(expr)
        return Line(
            pointFromZ3(fieldExprs[0]),
            pointFromZ3(fieldExprs[1]),
        )
    }

    @Test
    fun roundTripPointThroughZ3() {
        val ctx = Context()
        val original = Point(3, 7)
        val z3Value = pointToZ3(ctx, original)
        val restored = pointType.fromZ3Expr(z3Value) as Point
        assertEquals(original, restored)
    }

    @Test
    fun roundTripNestedLineThroughZ3() {
        val ctx = Context()
        val original = Line(Point(1, 2), Point(3, 4))
        val z3Value = lineToZ3(ctx, original)
        val restored = lineType.fromZ3Expr(z3Value) as Line
        assertEquals(original, restored)
    }

    @Test
    fun translateBetweenContexts() {
        val ctx1 = Context()
        val ctx2 = Context()

        val p1 = ctx1.mkConst("p", pointType.sort(ctx1))
        val p2 = ctx2.mkConst("p", pointType.sort(ctx2))
        val solver = ctx2.mkSolver()
        solver.add(ctx1.mkEq(p1, pointToZ3(ctx1, Point(9, 10))).translate(ctx2))
        solver.add(ctx2.mkEq(p2, pointToZ3(ctx2, Point(9, 10))))
        assertEquals(Status.SATISFIABLE, solver.check())
        val model = solver.model
        val value = pointType.fromZ3Expr(model.eval(p2, true)) as Point
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
