package julay.program

import com.microsoft.z3.BoolExpr
import com.microsoft.z3.Context
import julay.concurrency.SyncChannel
import java.util.*
import java.util.concurrent.locks.ReentrantLock

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
    }

    private val z3True = ctx.mkTrue()
    private val nonInitiallyConstructorActions = constructorsInfo
        .flatMap { info -> info.constructors.keys }
        .filter { act -> act != initiallyAction.symAction }
        .map { act -> TSAction(act, z3True, false) }
        .toSet()
    private val liveProcsLock = ReentrantLock()
    private val liveSelfTerminatingProcs = mutableSetOf<Thread>()
    private var initially = true

    override fun actions(): Set<TSAction> {
        return if (initially) {
            initially = false
            setOf(initiallyAction)
        }
        else {
            nonInitiallyConstructorActions
        }
    }

    override fun transit(act: ConcreteAction) {
        constructorsInfo
            .forEach { tsInfo ->
                if (tsInfo.constructors.containsKey(act.symAction)) {
                    val constructor = tsInfo.constructors[act.symAction]!!
                    val t = Thread(Proc(constructor.invoke(program,act), tsInfo, program.actionTable))
                    if (tsInfo.selfTerminate) {
                        liveProcsLock.lock()
                        try {
                            liveSelfTerminatingProcs.add(t)
                        } finally {
                            liveProcsLock.unlock()
                        }
                    }
                    t.start()
                }
            }

        // after the initially action, start monitoring for whether the program has ended in a new thread
        if (act.symAction == initiallyAction.symAction) {
            Thread {
                var running = true
                while (running) {
                    var nextLiveProc = Optional.empty<Thread>()
                    liveProcsLock.lock()
                    try {
                        if (liveSelfTerminatingProcs.isNotEmpty()) {
                            nextLiveProc = Optional.of(liveSelfTerminatingProcs.random())
                            liveSelfTerminatingProcs.remove(nextLiveProc.get())
                        }
                    } finally {
                        liveProcsLock.unlock()
                    }
                    // wait for the proc to terminate
                    if (nextLiveProc.isPresent) {
                        nextLiveProc.get().join()
                    }
                    else {
                        running = false
                    }
                }
                // TODO
                //program.actionTable.values.forEach { it.channel.close() }
            }.start()
        }
    }

    override fun getContext() = ctx
}