package julay.compiler

import julay.compiler.pass.CompilerOptConfig
import julay.compiler.pass.codegenPass
import julay.program.sync.SyncResolveConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class DisableOptFlagTest {
    @Test
    fun bareFlagDisablesAll() {
        assertEquals(SyncResolveConfig.ALL_OFF, SyncResolveConfig.fromDisableOptFlag("ALL"))
        assertEquals(SyncResolveConfig.ALL_OFF, SyncResolveConfig.fromDisableOptFlag(""))
        assertEquals(CompilerOptConfig.ALL_OFF, CompilerOptConfig.fromDisableOptFlag("ALL"))
        assertEquals(CompilerOptConfig.ALL_OFF, CompilerOptConfig.fromDisableOptFlag(""))
        assertFalse(CompilerOptConfig.fromDisableOptFlag("ALL").procfunFuse)
    }

    @Test
    fun absentKeepsDefaultsOn() {
        assertEquals(SyncResolveConfig.ALL_ON, SyncResolveConfig.fromDisableOptFlag(null))
        assertEquals(CompilerOptConfig.ALL_ON, CompilerOptConfig.fromDisableOptFlag(null))
        assertEquals(true, CompilerOptConfig.fromDisableOptFlag(null).procfunFuse)
    }

    @Test
    fun namedListDisablesSubset() {
        val cfg = SyncResolveConfig.fromDisableOptFlag("eq-unify,directed-eval")
        assertEquals(false, cfg.eqUnify)
        assertEquals(true, cfg.argOwnership)
        assertEquals(false, cfg.directedEval)
        assertEquals(true, CompilerOptConfig.fromDisableOptFlag("eq-unify,directed-eval").procfunFuse)
    }

    @Test
    fun procfunFuseAlone() {
        val cfg = CompilerOptConfig.fromDisableOptFlag("procfun-fuse")
        assertEquals(false, cfg.procfunFuse)
        assertEquals(SyncResolveConfig.ALL_ON, cfg.syncResolve)
    }

    @Test
    fun procfunFuseCombinedWithSyncIds() {
        val cfg = CompilerOptConfig.fromDisableOptFlag("eq-unify,procfun-fuse")
        assertEquals(false, cfg.procfunFuse)
        assertEquals(false, cfg.syncResolve.eqUnify)
        assertEquals(true, cfg.syncResolve.argOwnership)
        assertEquals(true, cfg.syncResolve.directedEval)
    }

    @Test
    fun unknownNameErrors() {
        assertFailsWith<IllegalArgumentException> {
            SyncResolveConfig.fromDisableOptFlag("not-a-real-opt")
        }
        assertFailsWith<IllegalArgumentException> {
            CompilerOptConfig.fromDisableOptFlag("not-a-real-opt")
        }
    }
}
