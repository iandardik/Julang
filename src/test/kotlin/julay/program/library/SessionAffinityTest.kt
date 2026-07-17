package julay.program.library

import com.microsoft.z3.Context
import julay.concurrency.Select
import julay.concurrency.SyncChannel
import julay.program.Constraint
import julay.program.Proc
import julay.program.Program
import julay.program.TransitionSystem
import julay.program.TransitionSystemStaticInfo
import julay.program.Variable
import julay.program.action.ConcreteAction
import julay.program.action.SymbolicAction
import julay.program.action.SyncPayload
import julay.program.action.TSAction
import julay.program.type.stringType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/**
 * Session-affinity coverage: handshake install, constructor follow-ons, peer-death recovery.
 */
class SessionAffinityTest {
    @Test
    fun constructorSessionSpawnInstallsFollowOnSession() = runBlocking {
        val followOn = SymbolicAction("followOn", listOf(Variable("x", stringType)), isSession = true)
        val ctorAct = SymbolicAction("makeChild", listOf(), isSession = true)
        val parentInfo = TransitionSystemStaticInfo(
            "ParentTS$",
            setOf(followOn),
            mapOf(ctorAct to { _, _ -> EmptyTS() }),
        )
        val childInfo = TransitionSystemStaticInfo(
            "ChildTS$",
            setOf(followOn),
            emptyMap(),
        )
        val program = Program(setOf(parentInfo, childInfo))
        val parent = Proc(EmptyTS(), parentInfo, program.staticChannelTable, program)
        val child = Proc(EmptyTS(), childInfo, program.staticChannelTable, program)
        parent.establishSessionWithSpawnedChild(child, ctorAct)
        assertNotNull(program.sessionAction("followOn"))
        assertTrue(true) // session install completed without throw
    }

    @Test
    fun sessionActionsAreMarkedOnHttpLibrary() {
        assertTrue(JulHttpServer.receiveRequestAct.isSession)
        assertTrue(JulHttpServer.sendResponseAct.isSession)
        assertTrue(JulHttpClient.sendRequestAct.isSession)
        assertTrue(JulHttpClient.receiveResponseAct.isSession)
        assertTrue(JulHttpClient.closeHttpClientAct.isSession)
    }

    /**
     * Global first contact installs one dedicated session; closing that session exits a multi-arm
     * Select; a subsequent global handshake installs a fresh session (peer-death recovery).
     */
    @Test
    fun dedicatedSessionCloseExitsSelectAndAllowsGlobalRehandshake() = runBlocking {
        withContext(Dispatchers.Default) {
            val ping = SymbolicAction("ping", listOf(), isSession = true)
            val infoA = TransitionSystemStaticInfo("ATS$", setOf(ping), emptyMap())
            val infoB = TransitionSystemStaticInfo("BTS$", setOf(ping), emptyMap())
            val program = Program(setOf(infoA, infoB))
            val global = program.staticChannelTable[ping]!!.channel

            val sessionBox = arrayOfNulls<SyncChannel<SyncPayload, Constraint>>(1)
            val installCount = AtomicInteger(0)

            val ctxA = Context()
            val ctxB = Context()
            try {
                val cA = Constraint(ctxA.mkTrue(), 1L, infoA.classID())
                val aA = Constraint(ctxA.mkFalse(), 1L, infoA.classID())
                val cB = Constraint(ctxB.mkTrue(), 2L, infoB.classID())
                val aB = Constraint(ctxB.mkFalse(), 2L, infoB.classID())

                val jA = async {
                    Select(
                        Select.SyncCase(global, cA, aA) { payload ->
                            assertTrue(payload.sessionToInstall.isPresent)
                            sessionBox[0] = payload.sessionToInstall.get().value
                            installCount.incrementAndGet()
                        },
                    ).run()
                }
                val jB = async {
                    Select(
                        Select.SyncCase(global, cB, aB) { payload ->
                            assertTrue(payload.sessionToInstall.isPresent)
                            installCount.incrementAndGet()
                        },
                    ).run()
                }
                withTimeout(5.seconds) {
                    jA.await()
                    jB.await()
                }
                assertEquals(2, installCount.get())
                val session = sessionBox[0]
                assertNotNull(session)
                assertFalse(session!!.isClosed())

                // Survivor waits on dedicated session + a never-firing arm; peer closes session.
                val never = program.makeSessionChannel(ping)
                val fired = AtomicBoolean(false)
                val survivor = async {
                    Select(
                        Select.SyncCase(session) { fired.set(true) },
                        Select.SyncCase(never) { fired.set(true) },
                    ).run()
                }
                delay(50)
                session.close() // peer death
                withTimeout(5.seconds) { survivor.await() }
                assertFalse(fired.get())

                // Global re-handshake with a new peer succeeds and installs a fresh session.
                val ctxC = Context()
                try {
                    val cC = Constraint(ctxC.mkTrue(), 3L, 99)
                    val aC = Constraint(ctxC.mkFalse(), 3L, 99)
                    val cA2 = Constraint(ctxA.mkTrue(), 1L, infoA.classID())
                    val aA2 = Constraint(ctxA.mkFalse(), 1L, infoA.classID())
                    val reinstall = AtomicInteger(0)
                    val rA = async {
                        Select(
                            Select.SyncCase(global, cA2, aA2) { payload ->
                                assertTrue(payload.sessionToInstall.isPresent)
                                reinstall.incrementAndGet()
                            },
                        ).run()
                    }
                    val rC = async {
                        Select(
                            Select.SyncCase(global, cC, aC) { payload ->
                                assertTrue(payload.sessionToInstall.isPresent)
                                reinstall.incrementAndGet()
                            },
                        ).run()
                    }
                    withTimeout(5.seconds) {
                        rA.await()
                        rC.await()
                    }
                    assertEquals(2, reinstall.get())
                } finally {
                    ctxC.close()
                }
            } finally {
                ctxA.close()
                ctxB.close()
            }
        }
    }
}

private class EmptyTS : TransitionSystem {
    override suspend fun actions(ctx: Context) = emptySet<TSAction>()
    override suspend fun transit(act: ConcreteAction) {}
}
