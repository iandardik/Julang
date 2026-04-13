package exspecs.program

import com.microsoft.z3.BoolExpr
import com.microsoft.z3.Context
import exspecs.concurrency.SyncChannel
import java.util.*
import java.util.concurrent.locks.ReentrantLock

class ConstructorTransitionSystem(
    private val initiallyAction : SymbolicAction,
    private val constructorsInfo : Set<TransitionSystemStaticInfo>,
    private val channelTable : Map<ActionSignature,SyncChannel<ConcreteAction, BoolExpr>>,
    private val ctx : Context,
    // TODO input the cli args into initially (accept them here as a constructor arg)
) : TransitionSystem {
    private val z3True = ctx.mkTrue()
    private val nonInitiallyConstructorActions = constructorsInfo
        .flatMap { info -> info.constructors.keys }
        .filter { sig -> sig != initiallyAction.signature }
        .map { sig -> SymbolicAction(sig, z3True) }
        .toSet()
    private val liveProcsLock = ReentrantLock()
    private val liveSelfTerminatingProcs = mutableSetOf<Thread>()
    private var initially = true

    override fun actions(): Set<SymbolicAction> {
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
                if (tsInfo.constructors.containsKey(act.signature)) {
                    val constructor = tsInfo.constructors[act.signature]!!
                    val t = Thread(Proc(constructor.invoke(act), channelTable))
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
        if (act.signature == initiallyAction.signature) {
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
                channelTable.values.forEach { it.close() }
            }.start()
        }
    }

    override fun getContext() = ctx
}