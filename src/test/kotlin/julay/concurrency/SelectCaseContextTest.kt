package julay.concurrency

import com.microsoft.z3.BoolExpr
import com.microsoft.z3.Context
import com.microsoft.z3.Status
import julay.program.Constraint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.Optional
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Regression for the Select fan-out Z3 Context race.
 *
 * If multiple Select cases share one source Context, concurrent SyncChannels call
 * [com.microsoft.z3.Expr.translate] on that Context from different threads and can crash
 * the JVM. Julay clones each case into a Case-local Context before Select (see
 * [julay.program.Proc] and [Select.run]). Omitting that clone reintroduces the race.
 *
 * Select picks one winning arm; the other is cancelled. Peers on both channels still race
 * pairwise [translate] while both arms are registered — that is the bug window.
 */
class SelectCaseContextTest {
    private fun translateCompute(): (Set<Constraint>) -> Optional<Int> = { constraints ->
        val exprs = constraints.mapNotNull { it.expr }
        if (exprs.isEmpty()) {
            Optional.of(1)
        } else {
            Context().use { ctx ->
                val solver = ctx.mkSolver()
                exprs.forEach { e -> solver.add(e.translate(ctx) as BoolExpr) }
                if (solver.check() != Status.SATISFIABLE) {
                    Optional.empty()
                } else {
                    Optional.of(1)
                }
            }
        }
    }

    @Test
    fun multiCaseSelectWithCaseLocalContextsDoesNotCrash() = runBlocking {
        withContext(Dispatchers.Default) {
            val rounds = 100
            val wins = AtomicInteger(0)
            repeat(rounds) {
                val chan1 = SyncChannel(
                    2,
                    antisCompatible = { a, b ->
                        val aa = a.anti; val bb = b.anti
                        aa == null || bb == null ||
                            !julay.program.sync.SyncResolveFast.antiSatisfiable(listOf(aa, bb))
                    },
                    compute = translateCompute(),
                )
                val chan2 = SyncChannel(
                    2,
                    antisCompatible = { a, b ->
                        val aa = a.anti; val bb = b.anti
                        aa == null || bb == null ||
                            !julay.program.sync.SyncResolveFast.antiSatisfiable(listOf(aa, bb))
                    },
                    compute = translateCompute(),
                )

                // One shared source Context (the bug pattern) — must clone before Select.
                val source = Context()
                val g1 = source.mkEq(source.mkIntConst("x"), source.mkInt(1))
                val g2 = source.mkEq(source.mkIntConst("y"), source.mkInt(2))
                val caseCtx1 = Context()
                val caseCtx2 = Context()
                val c1 = Constraint(g1).cloneInto(caseCtx1)
                val c2 = Constraint(g2).cloneInto(caseCtx2)
                val a1 = Constraint(anti = julay.program.sync.SyncAnti.ClassId(1))
                val a2 = Constraint(anti = julay.program.sync.SyncAnti.ClassId(2))
                source.close()

                val peerCtx1 = Context()
                val peerCtx2 = Context()
                val peer1 = Constraint(peerCtx1.mkEq(peerCtx1.mkIntConst("x"), peerCtx1.mkInt(1)))
                val peerAnti1 = Constraint(anti = julay.program.sync.SyncAnti.ClassId(99))
                val peer2 = Constraint(peerCtx2.mkEq(peerCtx2.mkIntConst("y"), peerCtx2.mkInt(2)))
                val peerAnti2 = Constraint(anti = julay.program.sync.SyncAnti.ClassId(98))

                val p1 = launch {
                    try {
                        chan1.sync(Optional.of(peer1), Optional.of(peerAnti1), Optional.empty())
                    } finally {
                        peerCtx1.close()
                    }
                }
                val p2 = launch {
                    try {
                        chan2.sync(Optional.of(peer2), Optional.of(peerAnti2), Optional.empty())
                    } finally {
                        peerCtx2.close()
                    }
                }
                val selectJob = launch {
                    Select(
                        Select.SyncCase(chan1, c1, a1) { wins.incrementAndGet() },
                        Select.SyncCase(chan2, c2, a2) { wins.incrementAndGet() },
                    ).run()
                }
                selectJob.join()
                // Cancel peers before closing Case Contexts — a cancelled Select arm may leave
                // a peer still translating Case-local exprs until scrubbed/cancelled.
                p1.cancelAndJoin()
                p2.cancelAndJoin()
                julay.program.ContextLocalCache.dropContext(caseCtx1)
                julay.program.ContextLocalCache.dropContext(caseCtx2)
                caseCtx1.close()
                caseCtx2.close()
            }
            // Exactly one Select arm wins per round.
            assertEquals(rounds, wins.get())
            assertTrue(wins.get() > 0)
        }
    }
}
