package julay.compiler.pass

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TlaApalacheTypesTest {
    @Test
    fun typeOkRhsMapsCommonShapes() {
        assertEquals("Bool", typeOkRhsToApalacheType("BOOLEAN"))
        assertEquals("Str", typeOkRhsToApalacheType("NodeSet"))
        assertEquals(
            "Str -> (Str -> Str)",
            typeOkRhsToApalacheType("[NodeSet -> [DynProc -> String]]"),
        )
        assertEquals(
            "Int -> {k: Str, v: Str}",
            typeOkRhsToApalacheType("BoundedSeq([k: Key, v: Value], MaxListLen)"),
        )
        assertEquals(
            "Set({src: Str, dest: Str, count: Int})",
            typeOkRhsToApalacheType("SUBSET [src: NodeSet, dest: NodeSet, count: TypeOKInt]"),
        )
    }

    @Test
    fun paramDomainResolvesMessageSets() {
        val vars = mapOf(
            "claimMsgs" to "Set({src: Str, dest: Str, k: Str, owner: Str})",
        )
        assertEquals(
            "{src: Str, dest: Str, k: Str, owner: Str}",
            paramDomainToApalacheType("claimMsgs", vars),
        )
        assertEquals(
            "Int -> {k: Str, v: Str}",
            paramDomainToApalacheType("BoundedSeq([k: Key, v: Value], MaxListLen)", vars),
        )
    }

    @Test
    fun annotatedConstantsAndVariablesEmitSnowcatComments() {
        val consts = emitApalacheAnnotatedConstants(
            listOf("NodeSet", "MaxListLen"),
            emptyMap(),
            annotate = true,
        )
        assertTrue(consts.contains("@type: Set(Str);"))
        assertTrue(consts.contains("@type: Int;"))
        val vars = emitApalacheAnnotatedVariables(
            listOf("store", "IfaceBroken"),
            mapOf("store" to "Str -> (Str -> Str)", "IfaceBroken" to "Bool"),
            annotate = true,
        )
        assertTrue(vars.contains("@type: Str -> (Str -> Str);"))
        assertTrue(vars.contains("@type: Bool;"))
    }

    @Test
    fun plainConstantsAndVariablesOmitSnowcatWhenApalacheOff() {
        val consts = emitApalacheAnnotatedConstants(
            listOf("NodeSet", "MaxListLen"),
            emptyMap(),
            annotate = false,
        )
        assertTrue(!consts.contains("@type"))
        val vars = emitApalacheAnnotatedVariables(
            listOf("store"),
            mapOf("store" to "Str -> Str"),
            annotate = false,
        )
        assertTrue(!vars.contains("@type"))
    }

    @Test
    fun boundedSeqAndRangeDefsAreTyped() {
        assertTrue(emitApalacheBoundedSeqDef(annotate = true).contains("@type: (Set(a), Int) => Set(Int -> a);"))
        assertTrue(emitApalacheRangeDef(emptySet(), annotate = true).contains("@type: ((Int -> a) => Set(a));"))
        assertTrue(!emitApalacheBoundedSeqDef(annotate = false).contains("@type"))
        assertTrue(!emitApalacheRangeDef(emptySet(), annotate = false).contains("@type"))
    }
}
