package julay.program

import com.microsoft.z3.Context
import com.microsoft.z3.Expr
import com.microsoft.z3.Model
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
            { expr, model -> pointFromZ3(expr, model) },
        )
        lineType = ObjClassType(
            "Line",
            listOf(
                Variable("start", pointType),
                Variable("end", pointType),
            ),
            { value, ctx -> lineToZ3(ctx, value.value as Line) },
            { expr, model -> lineFromZ3(expr, model) },
        )
    }

    private fun emptyModel(ctx: Context): Model {
        val solver = ctx.mkSolver()
        assertEquals(Status.SATISFIABLE, solver.check())
        return solver.model
    }

    private fun pointMk(ctx: Context, x: Expr<*>, y: Expr<*>): Expr<*> =
        pointType.constructorDecl(ctx).apply(x, y) as Expr<*>

    private fun pointX(ctx: Context, record: Expr<*>): Expr<*> =
        pointType.accessor(ctx, 0).apply(record) as Expr<*>

    private fun pointY(ctx: Context, record: Expr<*>): Expr<*> =
        pointType.accessor(ctx, 1).apply(record) as Expr<*>

    private fun pointToZ3(ctx: Context, value: Point): Expr<*> =
        pointMk(ctx, ctx.mkInt(value.x), ctx.mkInt(value.y))

    private fun pointFromZ3(expr: Expr<*>, model: Model): Point {
        val fieldExprs = if (expr.isApp && expr.funcDecl.name == pointType.homeConstructorDecl().name) {
            expr.args
        } else {
            arrayOf(
                pointType.homeAccessor(0).apply(expr) as Expr<*>,
                pointType.homeAccessor(1).apply(expr) as Expr<*>,
            )
        }
        return Point(
            intType.fromZ3Expr(fieldExprs[0], model) as Int,
            intType.fromZ3Expr(fieldExprs[1], model) as Int,
        )
    }

    private fun lineMk(ctx: Context, start: Expr<*>, end: Expr<*>): Expr<*> =
        lineType.constructorDecl(ctx).apply(start, end) as Expr<*>

    private fun lineToZ3(ctx: Context, value: Line): Expr<*> =
        lineMk(ctx, pointToZ3(ctx, value.start), pointToZ3(ctx, value.end))

    private fun lineFromZ3(expr: Expr<*>, model: Model): Line {
        val fieldExprs = if (expr.isApp && expr.funcDecl.name == lineType.homeConstructorDecl().name) {
            expr.args
        } else {
            arrayOf(
                lineType.homeAccessor(0).apply(expr) as Expr<*>,
                lineType.homeAccessor(1).apply(expr) as Expr<*>,
            )
        }
        return Line(
            pointFromZ3(fieldExprs[0], model),
            pointFromZ3(fieldExprs[1], model),
        )
    }

    @Test
    fun roundTripPointThroughZ3() {
        val ctx = Context()
        val original = Point(3, 7)
        val z3Value = pointToZ3(ctx, original)
        val restored = pointType.fromZ3Expr(z3Value, emptyModel(ctx)) as Point
        assertEquals(original, restored)
    }

    @Test
    fun roundTripNestedLineThroughZ3() {
        val ctx = Context()
        val original = Line(Point(1, 2), Point(3, 4))
        val z3Value = lineToZ3(ctx, original)
        val restored = lineType.fromZ3Expr(z3Value, emptyModel(ctx)) as Line
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
        val value = pointType.fromZ3Expr(model.eval(p2, true), model) as Point
        assertEquals(Point(9, 10), value)
    }

    @Test
    fun accessorHelpersReadFields() {
        val ctx = Context()
        val p = ctx.mkConst("p", pointType.sort(ctx))
        val solver = ctx.mkSolver()
        solver.add(ctx.mkEq(p, pointToZ3(ctx, Point(4, 5))))
        solver.add(ctx.mkEq(pointX(ctx, p) as com.microsoft.z3.IntExpr, ctx.mkInt(4)))
        solver.add(ctx.mkEq(pointY(ctx, p) as com.microsoft.z3.IntExpr, ctx.mkInt(5)))
        assertEquals(Status.SATISFIABLE, solver.check())
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
