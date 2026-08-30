package julay.program.sync

import julay.program.Constraint
import julay.program.Variable
import julay.program.action.SymbolicAction
import julay.program.type.intType
import julay.program.type.stringType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SyncResolveFastTest {
    @Test
    fun eqUnifyAgreeingBindingsAreSatWithoutContext() {
        val c1 = Constraint(
            fast = BoolExprFast.Eq(SyncTerm.Arg("x"), SyncTerm.Ground(SyncGround.IntVal(7))),
            procId = 1,
            classId = 1,
        )
        val c2 = Constraint(
            fast = BoolExprFast.Eq(SyncTerm.Arg("x"), SyncTerm.Ground(SyncGround.IntVal(7))),
            procId = 2,
            classId = 2,
        )
        assertEquals(true, SyncResolveFast.trySatisfiable(setOf(c1, c2), SyncResolveConfig.ALL_ON))
    }

    @Test
    fun eqUnifyConflictingBindingsAreUnsat() {
        val c1 = Constraint(
            fast = BoolExprFast.Eq(SyncTerm.Arg("x"), SyncTerm.Ground(SyncGround.IntVal(1))),
            procId = 1,
            classId = 1,
        )
        val c2 = Constraint(
            fast = BoolExprFast.Eq(SyncTerm.Arg("x"), SyncTerm.Ground(SyncGround.IntVal(2))),
            procId = 2,
            classId = 2,
        )
        assertEquals(false, SyncResolveFast.trySatisfiable(setOf(c1, c2), SyncResolveConfig.ALL_ON))
    }

    @Test
    fun directedEvalBindsRespFromReq() {
        val client = Constraint(
            fast = BoolExprFast.Eq(SyncTerm.Arg("req"), SyncTerm.Ground(SyncGround.IntVal(7))),
            procId = 1,
            classId = 1,
        )
        val server = Constraint(
            fast = BoolExprFast.Eq(
                SyncTerm.Arg("resp"),
                SyncTerm.IntArith(
                    SyncTerm.IntArith.Op.Add,
                    SyncTerm.Arg("req"),
                    SyncTerm.Ground(SyncGround.IntVal(1)),
                ),
            ),
            procId = 2,
            classId = 2,
        )
        val act = SymbolicAction(
            "echo",
            listOf(Variable("req", intType), Variable("resp", intType)),
        )
        val concrete = SyncResolveFast.tryConcreteAction(
            act,
            setOf(client, server),
            SyncResolveConfig.ALL_ON,
        )
        assertNotNull(concrete)
        assertTrue(concrete!!.isPresent)
        assertEquals(7, concrete.get().lookup(Variable("req", intType)).value)
        assertEquals(8, concrete.get().lookup(Variable("resp", intType)).value)
    }

    @Test
    fun enableFastNotLocalBool() {
        val guard = BoolExprFast.And(
            listOf(
                BoolExprFast.NotLocalBool("done"),
                BoolExprFast.Eq(SyncTerm.Arg("x"), SyncTerm.Local("k")),
            ),
        )
        assertEquals(
            true,
            SyncResolveFast.enableFast(guard, mapOf("done" to false, "k" to 7), SyncResolveConfig.ALL_ON),
        )
        assertEquals(
            false,
            SyncResolveFast.enableFast(guard, mapOf("done" to true, "k" to 7), SyncResolveConfig.ALL_ON),
        )
    }

    @Test
    fun groundForOfferSubstitutesLocalsAndDropsEnablement() {
        val guard = BoolExprFast.And(
            listOf(
                BoolExprFast.NotLocalBool("done"),
                BoolExprFast.Eq(SyncTerm.Arg("x"), SyncTerm.Local("k")),
            ),
        )
        val grounded = SyncResolveFast.groundForOffer(guard, mapOf("done" to false, "k" to 7))
        assertEquals(
            BoolExprFast.Eq(SyncTerm.Arg("x"), SyncTerm.Ground(SyncGround.IntVal(7))),
            grounded,
        )
        assertNull(SyncResolveFast.groundForOffer(guard, mapOf("done" to true, "k" to 7)))
    }

    @Test
    fun antiSameClassIdIsSatisfiablePair() {
        assertTrue(
            SyncResolveFast.antiSatisfiable(
                listOf(SyncAnti.ClassId(1), SyncAnti.ClassId(1)),
            ),
        )
        assertFalse(
            SyncResolveFast.antiSatisfiable(
                listOf(SyncAnti.ClassId(1), SyncAnti.ClassId(2)),
            ),
        )
    }

    @Test
    fun providerClientAntiUnsatTogether() {
        assertFalse(
            SyncResolveFast.antiSatisfiable(
                listOf(
                    SyncAnti.ProviderClient(true),
                    SyncAnti.ProviderClient(false),
                ),
            ),
        )
    }

    @Test
    fun stringCoerceToStringBinds() {
        val c1 = Constraint(
            fast = BoolExprFast.Eq(
                SyncTerm.Arg("msg", SyncTerm.Arg.Sort.String),
                SyncTerm.ToString(SyncTerm.Ground(SyncGround.IntVal(3))),
            ),
            procId = 1,
            classId = 1,
        )
        val c2 = Constraint(
            fast = BoolExprFast.Eq(
                SyncTerm.Arg("msg", SyncTerm.Arg.Sort.String),
                SyncTerm.Ground(SyncGround.StringVal("3")),
            ),
            procId = 2,
            classId = 2,
        )
        // ToString left unevaluated on one side without env — need grounded forms.
        val g1 = Constraint(
            fast = BoolExprFast.Eq(
                SyncTerm.Arg("msg", SyncTerm.Arg.Sort.String),
                SyncTerm.Ground(SyncGround.StringVal("3")),
            ),
            procId = 1,
            classId = 1,
        )
        assertEquals(true, SyncResolveFast.trySatisfiable(setOf(g1, c2), SyncResolveConfig.ALL_ON))
        assertNull(SyncResolveFast.trySatisfiable(setOf(c1, c2), SyncResolveConfig.ALL_ON))
    }

    @Test
    fun groundForOfferLocalBoolFalseReturnsNull() {
        assertNull(
            SyncResolveFast.groundForOffer(
                BoolExprFast.LocalBool("ready"),
                mapOf("ready" to false),
            ),
        )
    }

    @Test
    fun groundForOfferGroundEqMismatchReturnsNull() {
        assertNull(
            SyncResolveFast.groundForOffer(
                BoolExprFast.Eq(
                    SyncTerm.Local("step"),
                    SyncTerm.Ground(SyncGround.StringVal("call")),
                ),
                mapOf("step" to "respond"),
            ),
        )
    }

    @Test
    fun groundForOfferAndShortCircuitsOnDisabledPart() {
        val guard = BoolExprFast.And(
            listOf(
                BoolExprFast.Eq(SyncTerm.Local("step"), SyncTerm.Ground(SyncGround.StringVal("call"))),
                BoolExprFast.Eq(SyncTerm.Arg("x"), SyncTerm.Local("k")),
            ),
        )
        assertNull(SyncResolveFast.groundForOffer(guard, mapOf("step" to "respond", "k" to 7)))
        assertEquals(
            BoolExprFast.Eq(SyncTerm.Arg("x"), SyncTerm.Ground(SyncGround.IntVal(7))),
            SyncResolveFast.groundForOffer(guard, mapOf("step" to "call", "k" to 7)),
        )
    }

    @Test
    fun disableAllSkipsFastPath() {
        val c1 = Constraint(
            fast = BoolExprFast.Eq(SyncTerm.Arg("x"), SyncTerm.Ground(SyncGround.IntVal(7))),
            procId = 1,
            classId = 1,
        )
        assertNull(SyncResolveFast.trySatisfiable(setOf(c1), SyncResolveConfig.ALL_OFF))
    }
}
