package julay.compiler

import julay.compiler.pass.TlaOptConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DisableTlaOptFlagTest {
    @Test
    fun bareFlagDisablesAll() {
        assertEquals(TlaOptConfig.ALL_OFF, TlaOptConfig.fromDisableTlaOptFlag("ALL"))
        assertEquals(TlaOptConfig.ALL_OFF, TlaOptConfig.fromDisableTlaOptFlag(""))
    }

    @Test
    fun absentKeepsDefaultsOn() {
        assertEquals(TlaOptConfig.ALL_ON, TlaOptConfig.fromDisableTlaOptFlag(null))
    }

    @Test
    fun namedListDisablesSubset() {
        val cfg = TlaOptConfig.fromDisableTlaOptFlag("unused-fields")
        assertEquals(false, cfg.unusedFields)
        assertEquals(true, cfg.unusedVars)
        assertEquals(true, cfg.determinedArgs)
        assertEquals(true, cfg.fromCollection)
        assertEquals(true, cfg.literalDomains)
        assertEquals(true, cfg.unwrapSingletons)
        assertEquals(true, cfg.unusedLets)
    }

    @Test
    fun commaSeparatedDisablesSeveral() {
        val cfg = TlaOptConfig.fromDisableTlaOptFlag("determined-args,from-collection")
        assertEquals(true, cfg.unusedFields)
        assertEquals(true, cfg.unusedVars)
        assertEquals(false, cfg.determinedArgs)
        assertEquals(false, cfg.fromCollection)
        assertEquals(true, cfg.literalDomains)
        assertEquals(true, cfg.unwrapSingletons)
        assertEquals(true, cfg.unusedLets)
    }

    @Test
    fun unusedVarsIdDisablesOnlyThat() {
        val cfg = TlaOptConfig.fromDisableTlaOptFlag("unused-vars")
        assertEquals(true, cfg.unusedFields)
        assertEquals(false, cfg.unusedVars)
        assertEquals(true, cfg.determinedArgs)
        assertEquals(true, cfg.unusedLets)
    }

    @Test
    fun unusedLetsIdDisablesOnlyThat() {
        val cfg = TlaOptConfig.fromDisableTlaOptFlag("unused-lets")
        assertEquals(true, cfg.unusedFields)
        assertEquals(true, cfg.unusedVars)
        assertEquals(true, cfg.determinedArgs)
        assertEquals(false, cfg.unusedLets)
    }

    @Test
    fun fieldVarsIdDisablesOnlyThat() {
        val cfg = TlaOptConfig.fromDisableTlaOptFlag("field-vars")
        assertEquals(true, cfg.unusedFields)
        assertEquals(false, cfg.fieldVars)
        assertEquals(true, cfg.mapIndexPutRebuild)
    }

    @Test
    fun mapIndexPutIdDisablesOnlyThat() {
        val cfg = TlaOptConfig.fromDisableTlaOptFlag("map-index-put")
        assertEquals(false, cfg.mapIndexPutRebuild)
        assertEquals(true, cfg.fieldVars)
        assertEquals(true, cfg.unusedFields)
    }

    @Test
    fun unknownNameErrors() {
        assertFailsWith<IllegalArgumentException> {
            TlaOptConfig.fromDisableTlaOptFlag("not-a-real-opt")
        }
    }

    @Test
    fun mapIndexPutDefaultOn() {
        assertEquals(true, TlaOptConfig.ALL_ON.mapIndexPutRebuild)
        assertEquals(true, TlaOptConfig.fromOptTlaFlag(null).mapIndexPutRebuild)
    }

    @Test
    fun apalacheDefaultOff() {
        assertEquals(false, TlaOptConfig.ALL_ON.apalache)
        assertEquals(false, TlaOptConfig.fromOptTlaFlag(null).apalache)
    }

    @Test
    fun optTlaApalacheEnables() {
        assertEquals(true, TlaOptConfig.fromOptTlaFlag("apalache").apalache)
        assertEquals(true, TlaOptConfig.fromOptTlaFlag("ALL").apalache)
        assertEquals(true, TlaOptConfig.fromFlags(null, "apalache").apalache)
        assertEquals(false, TlaOptConfig.fromFlags("ALL", null).apalache)
    }

    @Test
    fun optTlaLegacyMapIndexPutIsNoOp() {
        val cfg = TlaOptConfig.fromOptTlaFlag("map-index-put")
        assertEquals(true, cfg.mapIndexPutRebuild)
        assertEquals(false, cfg.apalache)
    }

    @Test
    fun fromFlagsDisableMapIndexPut() {
        val cfg = TlaOptConfig.fromFlags("map-index-put", null)
        assertEquals(false, cfg.mapIndexPutRebuild)
        assertEquals(true, cfg.unusedFields)
    }

    @Test
    fun optTlaUnknownNameErrors() {
        assertFailsWith<IllegalArgumentException> {
            TlaOptConfig.fromOptTlaFlag("not-a-real-opt")
        }
    }
}
