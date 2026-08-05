package julay.compiler

import julay.program.SyncOptimizeConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DisableOptFlagTest {
    @Test
    fun bareFlagDisablesAll() {
        assertEquals(SyncOptimizeConfig.ALL_OFF, SyncOptimizeConfig.fromDisableOptFlag("ALL"))
        assertEquals(SyncOptimizeConfig.ALL_OFF, SyncOptimizeConfig.fromDisableOptFlag(""))
    }

    @Test
    fun absentKeepsDefaultsOn() {
        assertEquals(SyncOptimizeConfig.ALL_ON, SyncOptimizeConfig.fromDisableOptFlag(null))
    }

    @Test
    fun namedListDisablesSubset() {
        val cfg = SyncOptimizeConfig.fromDisableOptFlag("eq-unify,directed-eval")
        assertEquals(false, cfg.eqUnify)
        assertEquals(true, cfg.argOwnership)
        assertEquals(false, cfg.directedEval)
    }

    @Test
    fun unknownNameErrors() {
        assertFailsWith<IllegalArgumentException> {
            SyncOptimizeConfig.fromDisableOptFlag("not-a-real-opt")
        }
    }
}
