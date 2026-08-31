package julay.program

import com.microsoft.z3.Context
import julay.program.action.ConcreteAction
import julay.program.action.TSAction
import julay.program.sync.SyncStepPlan

/** Inert TS for long-lived pooled handler [Proc] shells before each request replaces it. */
internal object PooledWorkerPlaceholder : TransitionSystem {
    override suspend fun actions(ctx: Context): Set<TSAction> = emptySet()

    override suspend fun transit(act: ConcreteAction) {}

    override fun syncStepPlan(): SyncStepPlan = SyncStepPlan.FastOnly(emptyList())
}
