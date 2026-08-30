package julay.program

import com.microsoft.z3.Context
import julay.concurrency.SyncChannel
import julay.program.action.ConcreteAction
import julay.program.action.ProgramAction
import julay.program.action.SymbolicAction
import julay.program.action.SyncPayload
import julay.program.action.TSAction
import julay.program.sync.FastOffer
import julay.program.sync.BoolExprFast
import julay.program.sync.SyncGround
import julay.program.sync.SyncResolveConfig
import julay.program.sync.SyncResolveFast
import julay.program.sync.SyncStepPlan
import julay.program.sync.SyncTerm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.Optional
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Hand-written pure-IR TransitionSystems: Proc steps must not allocate ephemeral Contexts.
 */
class ProcFastPathTest {
    @Test
    fun pureIrPeerSyncAllocatesNoEphemeralContext() = runBlocking {
        withContext(Dispatchers.Default) {
            ContextAllocationCounter.reset()
            val before = ContextAllocationCounter.get()
            val handoff = SymbolicAction("handoff", emptyList())
            val program = Program(
                setOf(
                    TransitionSystemStaticInfo("A", setOf(handoff), emptyMap()),
                    TransitionSystemStaticInfo("B", setOf(handoff), emptyMap()),
                ),
                emptyList(),
            )
            // Program constructor registers channels; may allocate. Reset after setup.
            // Re-build with explicit channel table via spawnProc after reset is hard;
            // instead count only during run by resetting after Program init.
            ContextAllocationCounter.reset()

            val a = PureIrTs(handoff, offer = true)
            val b = PureIrTs(handoff, offer = true)
            val infoA = TransitionSystemStaticInfo("A", setOf(handoff), emptyMap())
            val infoB = TransitionSystemStaticInfo("B", setOf(handoff), emptyMap())
            // Use a dedicated Program with known sync channel
            val chan = SyncChannel<Constraint, SyncPayload>(
                2,
                satisfiable = { cs ->
                    julay.program.sync.SyncResolveFast.trySatisfiable(cs, SyncResolveConfig.ALL_ON)
                        ?: false
                },
                compute = { cs ->
                    val concrete = julay.program.sync.SyncResolveFast.tryConcreteAction(
                        handoff,
                        cs,
                        SyncResolveConfig.ALL_ON,
                    )
                    if (concrete == null || concrete.isEmpty) {
                        Optional.empty()
                    } else {
                        Optional.of(
                            SyncPayload(
                                concrete.get(),
                                cs.filter { it.procId >= 0 }.map {
                                    julay.program.action.SessionPeerMeta(it.procId, it.classId, it.proc!!)
                                },
                                Optional.empty(),
                            ),
                        )
                    }
                },
            )
            val staticTable = mapOf(
                handoff to ProgramAction(handoff, chan),
            )
            // Minimal Program for Proc — need allocateProcId. Use real Program.
            val prog = Program(
                setOf(infoA, infoB),
                emptyList(),
            )
            ContextAllocationCounter.reset()
            val start = ContextAllocationCounter.get()
            val procA = Proc(a, infoA, staticTable, prog)
            val procB = Proc(b, infoB, staticTable, prog)
            withTimeout(10_000) {
                val ja = async { procA.run() }
                val jb = async { procB.run() }
                ja.await()
                jb.await()
            }
            val allocated = ContextAllocationCounter.get() - start
            assertEquals(0L, allocated, "expected no ephemeral Context allocations, got $allocated")
            assertTrue(a.transited && b.transited)
            // silence unused
            assertTrue(before >= 0)
        }
    }

    @Test
    fun syncStepPlanSkipsDisabledOffersWithoutThrowing() = runBlocking {
        val handoff = SymbolicAction("handoff", emptyList())
        val info = TransitionSystemStaticInfo("A", setOf(handoff), emptyMap())
        val prog = Program(setOf(info), emptyList())
        val ts = MultiOfferIrTs(handoff, step = "call")
        val plan = ts.syncStepPlan()
        assertTrue(plan is SyncStepPlan.FastOnly)
        assertEquals(1, (plan as SyncStepPlan.FastOnly).offers.size)
        assertEquals(handoff, plan.offers[0].symAction)
    }
}

private class PureIrTs(
    private val act: SymbolicAction,
    private val offer: Boolean,
) : TransitionSystem {
    var transited = false

    override suspend fun actions(ctx: Context): Set<TSAction> =
        error("Z3 actions should not be called on FastOnly path")

    override fun syncStepPlan(): SyncStepPlan {
        if (transited || !offer) {
            return SyncStepPlan.FastOnly(emptyList())
        }
        return SyncStepPlan.FastOnly(listOf(FastOffer(act, BoolExprFast.True)))
    }

    override suspend fun transit(act: ConcreteAction) {
        transited = true
    }
}

/** Mirrors generated syncStepPlan: ground each offer; skip disabled guards (no exceptions). */
private class MultiOfferIrTs(
    private val enabledAct: SymbolicAction,
    private val step: String,
) : TransitionSystem {
    private val disabledAct = SymbolicAction("other", emptyList())

    override suspend fun actions(ctx: Context): Set<TSAction> =
        error("Z3 actions should not be called on FastOnly path")

    override fun syncStepPlan(): SyncStepPlan {
        val locals = mapOf("step" to step)
        val offers = mutableListOf<FastOffer>()
        val enabledGuard = BoolExprFast.Eq(
            SyncTerm.Local("step"),
            SyncTerm.Ground(SyncGround.StringVal("call")),
        )
        val disabledGuard = BoolExprFast.Eq(
            SyncTerm.Local("step"),
            SyncTerm.Ground(SyncGround.StringVal("respond")),
        )
        SyncResolveFast.groundForOffer(enabledGuard, locals)?.let {
            offers.add(FastOffer(enabledAct, it, TSAction.SyncRole.Default))
        }
        SyncResolveFast.groundForOffer(disabledGuard, locals)?.let {
            offers.add(FastOffer(disabledAct, it, TSAction.SyncRole.Default))
        }
        return SyncStepPlan.FastOnly(offers)
    }

    override suspend fun transit(act: ConcreteAction) {}
}
