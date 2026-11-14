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
        internal: Boolean = false,
        static: Boolean = false,
        timer: Boolean = false,
    ): AlphabetOffer {
        val mod = when {
            internal -> TSAction.SyncRole.Internal
            timer -> TSAction.SyncRole.Timer
            else -> TSAction.SyncRole.Default
        }
        return AlphabetOffer(
            pclassKey = pclass,
            occurrenceId = "",
            introducingAssembly = pclass,
            name = action,
            args = emptyList(),
            modifier = mod,
            isTimer = timer,
            isConstructor = false,
            loc = loc,
            channelKey = if (internal) "$pclass#internal#$action" else action,
            sourceInternal = internal,
            isStatic = static,
        )
    }

    @Test
    fun twoOccurrencesOfXKeepDistinctHiddenChannels() {
        // A/B are static hubs; X is dynamic (two occurrences). Ordinary sync is
        // composition-hidden per pair — dynamic-to-dynamic would be rejected.
        val leafMap = mapOf(
            "A" to listOf(offer("A", "w", static = true)),
            "B" to listOf(offer("B", "w", static = true)),
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
    fun danglingWOnTwoXCopiesIsComposeError() {
        val leafMap = mapOf(
            "A" to listOf(offer("A", "other", static = true)),
            "B" to listOf(offer("B", "other", static = true)),
            "X" to listOf(offer("X", "w")),
        )
        val ax = ProcDecl("AX", listOf(ProcDecl("A", emptyList(), ProcDeclType.Proc), ProcDecl("X", emptyList(), ProcDeclType.Proc)), ProcDeclType.Proc)
        val bx = ProcDecl("BX", listOf(ProcDecl("B", emptyList(), ProcDeclType.Proc), ProcDecl("X", emptyList(), ProcDeclType.Proc)), ProcDeclType.Proc)
        val root = ProcDecl("P", listOf(ax, bx), ProcDeclType.Proc)
        val result = computeCompositionAlphabet(root, listOf(root, ax, bx), leafMap)
        assertTrue(
            result.errors.any { it.toString().contains("Multiple occurrences of \"X\"") },
            "expected same-class ordinary error; got ${result.errors}",
        )
    }

    @Test
    fun sameClassOrdinaryIsComposeError() {
        val left = listOf(offer("X", "w").copy(occurrenceId = "x1"))
        val right = listOf(offer("X", "w").copy(occurrenceId = "x2"))
        val (composed, errs) = composeAlphabets(left, right, "scope")
        assertTrue(errs.any { it.toString().contains("Multiple occurrences of \"X\"") })
        assertEquals(2, composed.size)
        assertTrue(composed.none { it.compositionHidden })
    }

    @Test
    fun hubStaticWithDynamicsIsOk() {
        val leafMap = mapOf(
            "Hub" to listOf(offer("Hub", "w", static = true)),
            "A" to listOf(offer("A", "w")),
            "B" to listOf(offer("B", "w")),
        )
        val root = ProcDecl(
            "P",
            listOf(
                ProcDecl("Hub", emptyList(), ProcDeclType.Proc),
                ProcDecl("A", emptyList(), ProcDeclType.Proc),
                ProcDecl("B", emptyList(), ProcDeclType.Proc),
            ),
            ProcDeclType.Proc,
        )
        val result = computeCompositionAlphabet(root, listOf(root), leafMap)
        assertTrue(result.errors.isEmpty(), result.errors.toString())
        assertTrue(actionShapeErrors(result.allOffers).isEmpty(), actionShapeErrors(result.allOffers).toString())
    }

    @Test
    fun composedProcFunFoldsNonSyntheticOffersIntoParent() {
        val leafMap = mapOf(
            "Handler" to listOf(offer("Handler", "dispatch")),
            "clientAppendRPC" to listOf(
                offer("clientAppendRPC", "clientAppendRPC_call").copy(isConstructor = true),
                offer("clientAppendRPC", "noLeader"),
                offer("clientAppendRPC", "committed"),
                offer("clientAppendRPC", "clientAppendRPC_ret"),
                offer("clientAppendRPC", "step", internal = true),
            ),
        )
        val root = ProcDecl(
            "RpcIn",
            listOf(
                ProcDecl("Handler", emptyList(), ProcDeclType.Proc),
                ProcDecl("clientAppendRPC", emptyList(), ProcDeclType.Proc),
            ),
            ProcDeclType.Proc,
        )
        val result = computeCompositionAlphabet(
            root,
            listOf(root),
            leafMap,
            procFunNames = setOf("clientAppendRPC"),
        )
        assertTrue(result.errors.isEmpty(), result.errors.toString())
        assertTrue(result.external.any { it.name == "noLeader" })
        assertTrue(result.external.any { it.name == "committed" })
        assertTrue(result.external.none { it.name == "clientAppendRPC_call" })
        assertTrue(result.external.none { it.name == "clientAppendRPC_ret" })
        assertTrue(result.allOffers.none { it.name == "clientAppendRPC_call" })
        assertTrue(result.allOffers.none { it.name == "clientAppendRPC_ret" })
        assertTrue(result.allOffers.any { it.name == "step" && it.sourceInternal })
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
