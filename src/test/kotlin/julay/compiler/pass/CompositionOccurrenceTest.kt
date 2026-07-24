package julay.compiler.pass

import julay.compiler.SourceLoc
import julay.compiler.decl.ProcDecl
import julay.compiler.decl.ProcDeclType
import julay.program.action.TSAction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CompositionOccurrenceTest {
    private val loc = SourceLoc(1 to 1)

    private fun offer(
        pclass: String,
        action: String,
        service: Boolean = false,
        internal: Boolean = false,
    ): AlphabetOffer {
        val mod = when {
            internal -> TSAction.SyncRole.Internal
            service -> TSAction.SyncRole.Service
            else -> TSAction.SyncRole.Default
        }
        return AlphabetOffer(
            pclassKey = pclass,
            occurrenceId = "",
            introducingAssembly = pclass,
            name = action,
            args = emptyList(),
            modifier = mod,
            isSession = false,
            isConstructor = false,
            loc = loc,
            channelKey = if (internal) "$pclass#internal#$action" else action,
            sourceInternal = internal,
        )
    }

    @Test
    fun twoOccurrencesOfXKeepDistinctHiddenChannels() {
        val leafMap = mapOf(
            "A" to listOf(offer("A", "w")),
            "B" to listOf(offer("B", "w")),
            "X" to listOf(offer("X", "w")),
        )
        // (A || X) || (B || X)
        val ax = ProcDecl("AX", listOf(ProcDecl("A", emptyList(), ProcDeclType.Proc), ProcDecl("X", emptyList(), ProcDeclType.Proc)), ProcDeclType.Proc)
        val bx = ProcDecl("BX", listOf(ProcDecl("B", emptyList(), ProcDeclType.Proc), ProcDecl("X", emptyList(), ProcDeclType.Proc)), ProcDeclType.Proc)
        val root = ProcDecl("P", listOf(ax, bx), ProcDeclType.Proc)
        val decls = listOf(root, ax, bx)
        val result = computeCompositionAlphabet(root, decls, leafMap)
        assertEquals(4, result.leafOccurrences.size)
        assertEquals(2, result.leafOccurrences.count { it.pclassName == "X" })
        val hiddenW = result.allOffers.filter { it.name == "w" && it.compositionHidden }
        assertEquals(4, hiddenW.size)
        val keys = hiddenW.map { it.channelKey }.toSet()
        assertEquals(2, keys.size, "expected two distinct hidden channels for the two X syncs, got $keys")
        assertTrue(result.external.none { it.name == "w" })
        assertTrue(alphabetIntegrityErrors(result).isEmpty())
    }

    @Test
    fun danglingWOnTwoXCopiesIsIntegrityError() {
        val leafMap = mapOf(
            "A" to listOf(offer("A", "other")),
            "B" to listOf(offer("B", "other")),
            "X" to listOf(offer("X", "w")),
        )
        val ax = ProcDecl("AX", listOf(ProcDecl("A", emptyList(), ProcDeclType.Proc), ProcDecl("X", emptyList(), ProcDeclType.Proc)), ProcDeclType.Proc)
        val bx = ProcDecl("BX", listOf(ProcDecl("B", emptyList(), ProcDeclType.Proc), ProcDecl("X", emptyList(), ProcDeclType.Proc)), ProcDeclType.Proc)
        val root = ProcDecl("P", listOf(ax, bx), ProcDeclType.Proc)
        val result = computeCompositionAlphabet(root, listOf(root, ax, bx), leafMap)
        val errs = alphabetIntegrityErrors(result)
        assertTrue(errs.any { it.toString().contains("Multiple occurrences of \"X\"") })
    }

    @Test
    fun serviceResolvesDuplicateExternalW() {
        val leafMap = mapOf(
            "A" to listOf(offer("A", "other")),
            "B" to listOf(offer("B", "other")),
            "X" to listOf(offer("X", "w")),
            "S" to listOf(offer("S", "w", service = true)),
        )
        val ax = ProcDecl("AX", listOf(ProcDecl("A", emptyList(), ProcDeclType.Proc), ProcDecl("X", emptyList(), ProcDeclType.Proc)), ProcDeclType.Proc)
        val bx = ProcDecl("BX", listOf(ProcDecl("B", emptyList(), ProcDeclType.Proc), ProcDecl("X", emptyList(), ProcDeclType.Proc)), ProcDeclType.Proc)
        val p = ProcDecl("P", listOf(ax, bx), ProcDeclType.Proc)
        val q = ProcDecl("Q", listOf(p, ProcDecl("S", emptyList(), ProcDeclType.Proc)), ProcDeclType.Proc)
        val result = computeCompositionAlphabet(q, listOf(q, p, ax, bx), leafMap)
        assertTrue(alphabetIntegrityErrors(result).isEmpty())
    }

    @Test
    fun twoServiceProvidersError() {
        val leafMap = mapOf(
            "X" to listOf(offer("X", "w", service = true)),
            "Y" to listOf(offer("Y", "w", service = true)),
        )
        val root = ProcDecl(
            "P",
            listOf(ProcDecl("X", emptyList(), ProcDeclType.Proc), ProcDecl("Y", emptyList(), ProcDeclType.Proc)),
            ProcDeclType.Proc,
        )
        val result = computeCompositionAlphabet(root, listOf(root), leafMap)
        assertTrue(alphabetIntegrityErrors(result).any { it.toString().contains("more than one service provider") })
    }

    @Test
    fun sameClassNeverSyncsOnCompose() {
        val left = listOf(offer("X", "w").copy(occurrenceId = "x1"))
        val right = listOf(offer("X", "w").copy(occurrenceId = "x2"))
        val (composed, errs) = composeAlphabets(left, right, "scope")
        assertTrue(errs.isEmpty())
        assertEquals(2, composed.size)
        assertTrue(composed.none { it.compositionHidden })
    }

    @Test
    fun tlaNamesUseIntroducingAssembly() {
        val leaves = listOf(
            SpecLeaf("X", occurrenceId = "1", introducingAssembly = "P"),
            SpecLeaf("X", occurrenceId = "2", introducingAssembly = "Q"),
            SpecLeaf("A", occurrenceId = "3", introducingAssembly = "P"),
        )
        val named = assignTlaLeafNames(leaves)
        assertEquals("X_P", named[0].tlaName)
        assertEquals("X_Q", named[1].tlaName)
        assertEquals("A", named[2].tlaName)
    }

    @Test
    fun tlaNamesNumberSameParentTie() {
        val leaves = listOf(
            SpecLeaf("X", occurrenceId = "1", introducingAssembly = "P"),
            SpecLeaf("X", occurrenceId = "2", introducingAssembly = "P"),
        )
        val named = assignTlaLeafNames(leaves)
        assertEquals(setOf("X_P_1", "X_P_2"), named.map { it.tlaName }.toSet())
    }
}
