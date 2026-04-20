package julay.program

import com.microsoft.z3.Context
import julay.program.library.JulHttpServer.Companion.reqBodyArg
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.*
import java.util.*

class ConstructorTransitionSystem(
    private val initiallyAction : TSAction,
    private val constructorsInfo : Set<TransitionSystemStaticInfo>,
    private val program : Program,
    private val ctx : Context,
    // TODO input the cli args into initially (accept them here as a constructor arg)
) : TransitionSystem {
    companion object: StaticInfo {
        // the $ in the name means that programs cannot create p-classes whose names conflict with this one
        // the alphabet info is not strictly correct, but it does not matter since it's never used
        override fun staticInfo() = TransitionSystemStaticInfo("ConstructorTS$", setOf(), mapOf(), setOf(), false)
        // this action will never sync with any other action, meaning that this constructor proc will never deadlock and
        // terminate
        val deadlockAct = SymbolicAction("deadlock", listOf())
    }

    // in the future, we may want to switch to Dispatchers.Default or make it possible for the programmer to choose the
    // dispatch type.
    private val scope = CoroutineScope(Dispatchers.IO)

    private val z3True = ctx.mkTrue()
    private val nonInitiallyConstructorActions = constructorsInfo
        .asSequence()
        .flatMap { info -> info.constructors.keys }
        .filter { act -> act != initiallyAction.symAction }
        .map { act -> TSAction(act, z3True, false) }
        .toSet()
        .plus(TSAction(deadlockAct, ctx.mkFalse(), true))
    private val liveProcsMutex = Mutex()
    private val liveSelfTerminatingProcs = mutableSetOf<Job>()
    private var initially = true

    override suspend fun actions(): Set<TSAction> {
        return if (initially) {
            initially = false
            setOf(initiallyAction)
        }
        else {
            nonInitiallyConstructorActions
        }
    }

    override suspend fun transit(act: ConcreteAction) {
        constructorsInfo
            .forEach { tsInfo ->
                if (tsInfo.constructors.containsKey(act.symAction)) {
                    val constructor = tsInfo.constructors[act.symAction]!!
                    val proc = Proc(constructor.invoke(program,act), tsInfo, program.actionTable)
                    val job = scope.launch { proc.run() }
                    if (tsInfo.selfTerminate) {
                        liveProcsMutex.withLock {
                            liveSelfTerminatingProcs.add(job)
                        }
                    }
                }
            }

        // after the initially action, start monitoring for whether the program has ended in a new thread
        if (act.symAction == initiallyAction.symAction) {
            scope.launch {
                var running = true
                while (running) {
                    var nextLiveProc = Optional.empty<Job>()
                    liveProcsMutex.withLock {
                        if (liveSelfTerminatingProcs.isNotEmpty()) {
                            nextLiveProc = Optional.of(liveSelfTerminatingProcs.random())
                            liveSelfTerminatingProcs.remove(nextLiveProc.get())
                        }
                    }
                    // wait for the proc to terminate
                    if (nextLiveProc.isPresent) {
                        nextLiveProc.get().join()
                    }
                    else {
                        running = false
                    }
                }
                // close all channels to end the program
                program.actionTable.values.forEach { it.channel.close() }
            }
        }
    }

    override fun getContext() = ctx
}
