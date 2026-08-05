package julay.compiler

import julay.program.sync.SyncResolveConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DisableOptFlagTest {
    @Test
    fun bareFlagDisablesAll() {
        assertEquals(SyncResolveConfig.ALL_OFF, SyncResolveConfig.fromDisableOptFlag("ALL"))
        assertEquals(SyncResolveConfig.ALL_OFF, SyncResolveConfig.fromDisableOptFlag(""))
    }

    @Test
    fun absentKeepsDefaultsOn() {
        assertEquals(SyncResolveConfig.ALL_ON, SyncResolveConfig.fromDisableOptFlag(null))
    }

    @Test
    fun namedListDisablesSubset() {
        val cfg = SyncResolveConfig.fromDisableOptFlag("eq-unify,directed-eval")
        assertEquals(false, cfg.eqUnify)
        assertEquals(true, cfg.argOwnership)
        assertEquals(false, cfg.directedEval)
    }

    @Test
    fun unknownNameErrors() {
        assertFailsWith<IllegalArgumentException> {
            SyncResolveConfig.fromDisableOptFlag("not-a-real-opt")
        }
    }
}
