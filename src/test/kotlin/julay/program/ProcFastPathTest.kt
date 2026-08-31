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

    /** Protocol-shaped FastOnly path: Proc reuses Select shell across 3-offer multi-offer steps. */
    @Test
    fun multiOfferFastOnlyProcReusesSelectShell() = runBlocking {
        withContext(Dispatchers.Default) {
            val act1 = SymbolicAction("offer1", emptyList())
            val act2 = SymbolicAction("offer2", emptyList())
            val act3 = SymbolicAction("offer3", emptyList())
            val serverInfo = TransitionSystemStaticInfo(
                "Server",
                setOf(act1, act2, act3),
                emptyMap(),
            )
            val clientInfo = TransitionSystemStaticInfo("Client", setOf(act1), emptyMap())
            val prog = Program(setOf(serverInfo, clientInfo))
            val table = prog.staticChannelTable
            ContextAllocationCounter.reset()
            val rounds = 25
            val server = ThreeOfferServerTs(act1, act2, act3, act1, maxRounds = rounds)
            val client = SingleOfferClientTs(act1, maxRounds = rounds)
            val start = ContextAllocationCounter.get()
            val procServer = Proc(server, serverInfo, table, prog)
            val procClient = Proc(client, clientInfo, table, prog)
            withTimeout(15_000) {
                val js = async { procServer.run() }
                val jc = async { procClient.run() }
                js.await()
                jc.await()
            }
            val allocated = ContextAllocationCounter.get() - start
            assertEquals(0L, allocated, "expected no ephemeral Context allocations, got $allocated")
            assertEquals(rounds, server.transits)
            assertEquals(rounds, client.transits)
        }
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

/** Server TS: three always-enabled FastOnly offers (Protocol-shaped idle loop). */
private class ThreeOfferServerTs(
    private val act1: SymbolicAction,
    private val act2: SymbolicAction,
    private val act3: SymbolicAction,
    /** Client offer synced against this action's channel. */
    private val clientAct: SymbolicAction,
    private val maxRounds: Int,
) : TransitionSystem {
    var transits = 0

    override suspend fun actions(ctx: Context): Set<TSAction> =
        error("Z3 actions should not be called on FastOnly path")

    override fun syncStepPlan(): SyncStepPlan {
        if (transits >= maxRounds) {
            return SyncStepPlan.FastOnly(emptyList())
        }
        return SyncStepPlan.FastOnly(
            listOf(
                FastOffer(act1, BoolExprFast.True),
                FastOffer(act2, BoolExprFast.True),
                FastOffer(act3, BoolExprFast.True),
            ),
        )
    }

    override suspend fun transit(act: ConcreteAction) {
        assertEquals(clientAct, act.symAction)
        transits++
    }
}

private class SingleOfferClientTs(
    private val act: SymbolicAction,
    private val maxRounds: Int,
) : TransitionSystem {
    var transits = 0

    override suspend fun actions(ctx: Context): Set<TSAction> =
        error("Z3 actions should not be called on FastOnly path")

    override fun syncStepPlan(): SyncStepPlan {
        if (transits >= maxRounds) {
            return SyncStepPlan.FastOnly(emptyList())
        }
        return SyncStepPlan.FastOnly(listOf(FastOffer(act, BoolExprFast.True)))
    }

    override suspend fun transit(act: ConcreteAction) {
        transits++
    }
}
