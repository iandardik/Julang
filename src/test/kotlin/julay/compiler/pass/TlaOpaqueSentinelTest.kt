package julay.compiler.pass

import kotlin.test.Test
import kotlin.test.assertEquals

class TlaOpaqueSentinelTest {
    @Test
    fun pickOpaqueSentinelPrefersEmptyString() {
        assertEquals("", pickOpaqueSentinelAtom(setOf("n1", "n2")))
    }

    @Test
    fun pickOpaqueSentinelSkipsLiveAtoms() {
        assertEquals("#", pickOpaqueSentinelAtom(setOf("", "n1")))
        assertEquals("__none", pickOpaqueSentinelAtom(setOf("", "#", "n1")))
        assertEquals(
            "__empty_0",
            pickOpaqueSentinelAtom(setOf("", "#", "__none", "__empty", "None")),
        )
    }
}
