package julay.program

import io.github.cvc5.Kind
import io.github.cvc5.Solver
import io.github.cvc5.Term
import io.github.cvc5.TermManager
import julay.tools.SmtConstraint
import julay.tools.applyConstructor
import julay.tools.applySelector
import julay.tools.findDeclaredConst
import julay.tools.isSat
import julay.tools.newModelSolver
import julay.tools.withSolveConstraints
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
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
            { value, tm -> pointToSmt(tm, value.value as Point) },
            { expr, solver -> pointFromSmt(expr, solver) },
        )
        lineType = ObjClassType(
            "Line",
            listOf(
                Variable("start", pointType),
                Variable("end", pointType),
            ),
            { value, tm -> lineToSmt(tm, value.value as Line) },
            { expr, solver -> lineFromSmt(expr, solver) },
        )
    }

    private fun emptySolver(tm: TermManager): Solver {
        val solver = newModelSolver(tm)
        assertTrue(solver.isSat())
        return solver
    }

    private fun pointMk(tm: TermManager, x: Term, y: Term): Term =
        applyConstructor(tm, pointType.constructorTerm(tm), arrayOf(x, y))

    private fun pointX(tm: TermManager, record: Term): Term =
        applySelector(tm, pointType.selector(tm, 0), record)

    private fun pointY(tm: TermManager, record: Term): Term =
        applySelector(tm, pointType.selector(tm, 1), record)

    private fun pointToSmt(tm: TermManager, value: Point): Term =
        pointMk(tm, tm.mkInteger(value.x.toLong()), tm.mkInteger(value.y.toLong()))

    private fun pointFromSmt(expr: Term, solver: Solver): Point {
        val valued = solver.getValue(expr)
        require(valued.kind == Kind.APPLY_CONSTRUCTOR && valued.numChildren >= 3)
        return Point(
            intType.fromSmtTerm(valued.getChild(1), solver) as Int,
            intType.fromSmtTerm(valued.getChild(2), solver) as Int,
        )
    }

    private fun lineMk(tm: TermManager, start: Term, end: Term): Term =
        applyConstructor(tm, lineType.constructorTerm(tm), arrayOf(start, end))

    private fun lineToSmt(tm: TermManager, value: Line): Term =
        lineMk(tm, pointToSmt(tm, value.start), pointToSmt(tm, value.end))

    private fun lineFromSmt(expr: Term, solver: Solver): Line {
        val valued = solver.getValue(expr)
        require(valued.kind == Kind.APPLY_CONSTRUCTOR && valued.numChildren >= 3)
        return Line(
            pointFromSmt(valued.getChild(1), solver),
            pointFromSmt(valued.getChild(2), solver),
        )
    }

    @Test
    fun roundTripPointThroughSmt() {
        val tm = TermManager()
        val original = Point(3, 7)
        val smtValue = pointToSmt(tm, original)
        val restored = pointType.fromSmtTerm(smtValue, emptySolver(tm)) as Point
        assertEquals(original, restored)
    }

    @Test
    fun roundTripNestedLineThroughSmt() {
        val tm = TermManager()
        val original = Line(Point(1, 2), Point(3, 4))
        val smtValue = lineToSmt(tm, original)
        val restored = lineType.fromSmtTerm(smtValue, emptySolver(tm)) as Line
        assertEquals(original, restored)
    }

    @Test
    fun smtLibRoundTripBetweenSolvers() {
        val tm = TermManager()
        val p = pointType.toSmtTerm(Variable("p", pointType), tm)
        val constraint = SmtConstraint.from(
            tm.mkTerm(Kind.EQUAL, p, pointToSmt(tm, Point(9, 10))),
        )
        val value = withSolveConstraints(listOf(constraint)) { solver, declared ->
            val restoredTerm = findDeclaredConst(declared, "p")
            pointType.fromSmtTerm(restoredTerm, solver) as Point
        }
        assertEquals(Point(9, 10), assertNotNull(value))
    }

    @Test
    fun selectorHelpersReadFields() {
        val tm = TermManager()
        val p = pointType.toSmtTerm(Variable("p", pointType), tm)
        val solver = newModelSolver(tm)
        solver.assertFormula(tm.mkTerm(Kind.EQUAL, p, pointToSmt(tm, Point(4, 5))))
        solver.assertFormula(tm.mkTerm(Kind.EQUAL, pointX(tm, p), tm.mkInteger(4)))
        solver.assertFormula(tm.mkTerm(Kind.EQUAL, pointY(tm, p), tm.mkInteger(5)))
        assertTrue(solver.isSat())
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
