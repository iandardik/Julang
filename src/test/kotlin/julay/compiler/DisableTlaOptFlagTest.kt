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
    }

    @Test
    fun unusedVarsIdDisablesOnlyThat() {
        val cfg = TlaOptConfig.fromDisableTlaOptFlag("unused-vars")
        assertEquals(true, cfg.unusedFields)
        assertEquals(false, cfg.unusedVars)
        assertEquals(true, cfg.determinedArgs)
    }

    @Test
    fun unknownNameErrors() {
        assertFailsWith<IllegalArgumentException> {
            TlaOptConfig.fromDisableTlaOptFlag("not-a-real-opt")
        }
    }
}
