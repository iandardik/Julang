package julay.program.sync

import com.microsoft.z3.Context
import julay.program.Constraint
import julay.program.SyncOptimizeConfig
import julay.program.Variable
import julay.program.action.SymbolicAction
import julay.program.type.intType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SyncOptimizeTest {
    @Test
    fun eqUnifyAgreeingBindingsAreSat() {
        Context().use { ctx ->
            val x = ctx.mkIntConst("x")
            val c1 = Constraint(ctx.mkEq(x, ctx.mkInt(7)), procId = 1, classId = 1)
            val c2 = Constraint(ctx.mkEq(x, ctx.mkInt(7)), procId = 2, classId = 2)
            val sat = SyncOptimize.trySatisfiable(setOf(c1, c2), SyncOptimizeConfig.ALL_ON, ctx)
            assertEquals(true, sat)
        }
    }

    @Test
    fun eqUnifyConflictingBindingsAreUnsat() {
        Context().use { ctx ->
            val x = ctx.mkIntConst("x")
            val c1 = Constraint(ctx.mkEq(x, ctx.mkInt(1)), procId = 1, classId = 1)
            val c2 = Constraint(ctx.mkEq(x, ctx.mkInt(2)), procId = 2, classId = 2)
            val sat = SyncOptimize.trySatisfiable(setOf(c1, c2), SyncOptimizeConfig.ALL_ON, ctx)
            assertEquals(false, sat)
        }
    }

    @Test
    fun relationalFallsBackToNull() {
        Context().use { ctx ->
            val x = ctx.mkIntConst("x")
            val y = ctx.mkIntConst("y")
            val c1 = Constraint(ctx.mkLt(x, y) as com.microsoft.z3.BoolExpr, procId = 1, classId = 1)
            val sat = SyncOptimize.trySatisfiable(setOf(c1), SyncOptimizeConfig.ALL_ON, ctx)
            assertNull(sat)
        }
    }

    @Test
    fun directedEvalBindsRespFromReq() {
        Context().use { ctx ->
            val req = ctx.mkIntConst("req")
            val resp = ctx.mkIntConst("resp")
            val client = Constraint(ctx.mkEq(req, ctx.mkInt(7)), procId = 1, classId = 1)
            val server = Constraint(
                ctx.mkEq(resp, ctx.mkAdd(req, ctx.mkInt(1))),
                procId = 2,
                classId = 2,
            )
            val act = SymbolicAction(
                "echo",
                listOf(Variable("req", intType), Variable("resp", intType)),
            )
            val concrete = SyncOptimize.tryConcreteAction(
                act,
                setOf(client, server),
                SyncOptimizeConfig.ALL_ON,
                ctx,
            )
            assertNotNull(concrete)
            assertTrue(concrete!!.isPresent)
            val ca = concrete.get()
            assertEquals(7, ca.lookup(Variable("req", intType)).value)
            assertEquals(8, ca.lookup(Variable("resp", intType)).value)
        }
    }

    @Test
    fun disableAllSkipsFastPath() {
        Context().use { ctx ->
            val x = ctx.mkIntConst("x")
            val c1 = Constraint(ctx.mkEq(x, ctx.mkInt(7)), procId = 1, classId = 1)
            assertNull(SyncOptimize.trySatisfiable(setOf(c1), SyncOptimizeConfig.ALL_OFF, ctx))
        }
    }
}
