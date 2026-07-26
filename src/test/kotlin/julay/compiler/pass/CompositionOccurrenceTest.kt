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
        provider: Boolean = false,
        client: Boolean = false,
        internal: Boolean = false,
    ): AlphabetOffer {
        val mod = when {
            internal -> TSAction.SyncRole.Internal
            provider -> TSAction.SyncRole.Provider
            client -> TSAction.SyncRole.Client
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
    fun danglingWOnTwoXCopiesIsComposeError() {
        val leafMap = mapOf(
            "A" to listOf(offer("A", "other")),
            "B" to listOf(offer("B", "other")),
            "X" to listOf(offer("X", "w")),
        )
        val ax = ProcDecl("AX", listOf(ProcDecl("A", emptyList(), ProcDeclType.Proc), ProcDecl("X", emptyList(), ProcDeclType.Proc)), ProcDeclType.Proc)
        val bx = ProcDecl("BX", listOf(ProcDecl("B", emptyList(), ProcDeclType.Proc), ProcDecl("X", emptyList(), ProcDeclType.Proc)), ProcDeclType.Proc)
        val root = ProcDecl("P", listOf(ax, bx), ProcDeclType.Proc)
        val result = computeCompositionAlphabet(root, listOf(root, ax, bx), leafMap)
        assertTrue(result.errors.any { it.toString().contains("Multiple occurrences of \"X\"") })
        assertTrue(alphabetIntegrityErrors(result).isEmpty())
    }

    @Test
    fun providerWithClientOccurrencesOk() {
        // Two client offers of w (same class) must not hide with each other; provider resolves the hub.
        val leafMap = mapOf(
            "A" to listOf(offer("A", "other")),
            "B" to listOf(offer("B", "other")),
            "X" to listOf(offer("X", "w", client = true)),
            "S" to listOf(offer("S", "w", provider = true)),
        )
        val ax = ProcDecl("AX", listOf(ProcDecl("A", emptyList(), ProcDeclType.Proc), ProcDecl("X", emptyList(), ProcDeclType.Proc)), ProcDeclType.Proc)
        val bx = ProcDecl("BX", listOf(ProcDecl("B", emptyList(), ProcDeclType.Proc), ProcDecl("X", emptyList(), ProcDeclType.Proc)), ProcDeclType.Proc)
        val p = ProcDecl("P", listOf(ax, bx), ProcDeclType.Proc)
        val q = ProcDecl("Q", listOf(p, ProcDecl("S", emptyList(), ProcDeclType.Proc)), ProcDeclType.Proc)
        val result = computeCompositionAlphabet(q, listOf(q, p, ax, bx), leafMap)
        assertTrue(result.errors.isEmpty(), result.errors.toString())
        assertTrue(alphabetIntegrityErrors(result).isEmpty())
        val externalW = result.external.filter { it.name == "w" }
        assertEquals(1, externalW.size)
        assertTrue(externalW.single().isProvider)
        val clientW = result.allOffers.filter { it.name == "w" && it.isClient }
        assertEquals(2, clientW.size)
        assertTrue(clientW.all { it.compositionHidden })
    }

    @Test
    fun providerHidesSyncedClientsFromExternal() {
        val left = listOf(offer("S", "w", provider = true).copy(occurrenceId = "s1"))
        val right = listOf(offer("C", "w", client = true).copy(occurrenceId = "c1"))
        val (composed, errs) = composeAlphabets(left, right, "scope")
        assertTrue(errs.isEmpty(), errs.toString())
        assertEquals(2, composed.size)
        val provider = composed.single { it.isProvider }
        val client = composed.single { it.isClient }
        assertTrue(!provider.compositionHidden)
        assertTrue(client.compositionHidden)
        assertEquals("w", provider.channelKey)
        assertEquals("w", client.channelKey)
        val external = composed.filter { !it.sourceInternal && !it.compositionHidden }
        assertEquals(listOf(provider), external)
    }

    @Test
    fun twoProvidersError() {
        val leafMap = mapOf(
            "X" to listOf(offer("X", "w", provider = true)),
            "Y" to listOf(offer("Y", "w", provider = true)),
        )
        val root = ProcDecl(
            "P",
            listOf(ProcDecl("X", emptyList(), ProcDeclType.Proc), ProcDecl("Y", emptyList(), ProcDeclType.Proc)),
            ProcDeclType.Proc,
        )
        val result = computeCompositionAlphabet(root, listOf(root), leafMap)
        assertTrue(alphabetIntegrityErrors(result).any { it.toString().contains("more than one provider") })
    }

    @Test
    fun clientsDoNotHideWithEachOther() {
        val left = listOf(offer("A", "w", client = true).copy(occurrenceId = "a1"))
        val right = listOf(offer("B", "w", client = true).copy(occurrenceId = "b1"))
        val (composed, errs) = composeAlphabets(left, right, "scope")
        assertTrue(errs.isEmpty())
        assertEquals(2, composed.size)
        assertTrue(composed.none { it.compositionHidden })
    }

    @Test
    fun ordinaryWithProviderIsError() {
        val left = listOf(offer("A", "w").copy(occurrenceId = "a1"))
        val right = listOf(offer("S", "w", provider = true).copy(occurrenceId = "s1"))
        val (_, errs) = composeAlphabets(left, right, "scope")
        assertTrue(errs.any { it.toString().contains("untagged") })
    }

    @Test
    fun ordinaryWithClientIsError() {
        val left = listOf(offer("A", "w").copy(occurrenceId = "a1"))
        val right = listOf(offer("C", "w", client = true).copy(occurrenceId = "c1"))
        val (_, errs) = composeAlphabets(left, right, "scope")
        assertTrue(errs.any { it.toString().contains("untagged") })
    }

    @Test
    fun danglingClientFromProcFunIsNotIntegrityError() {
        val leafMap = mapOf(
            "Handler" to listOf(offer("Handler", "dispatch")),
            "clientAppendRPC" to listOf(offer("clientAppendRPC", "noLeader", client = true)),
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
        assertTrue(result.external.any { it.name == "noLeader" })
        assertTrue(
            alphabetIntegrityErrors(result, setOf("clientAppendRPC")).isEmpty(),
            alphabetIntegrityErrors(result, setOf("clientAppendRPC")).toString(),
        )
    }

    @Test
    fun danglingClientIsIntegrityError() {
        val leafMap = mapOf(
            "C" to listOf(offer("C", "w", client = true)),
        )
        val root = ProcDecl("P", listOf(ProcDecl("C", emptyList(), ProcDeclType.Proc)), ProcDeclType.Proc)
        val result = computeCompositionAlphabet(root, listOf(root), leafMap)
        assertTrue(alphabetIntegrityErrors(result).any { it.toString().contains("no `provider`") })
    }

    @Test
    fun ordinaryHideThenProviderClientCompiles() {
        val leafMap = mapOf(
            "A" to listOf(offer("A", "w")),
            "B" to listOf(offer("B", "w")),
            "Prov" to listOf(offer("Prov", "w", provider = true)),
            "Cli" to listOf(offer("Cli", "w", client = true)),
        )
        val ab = ProcDecl(
            "AB",
            listOf(ProcDecl("A", emptyList(), ProcDeclType.Proc), ProcDecl("B", emptyList(), ProcDeclType.Proc)),
            ProcDeclType.Proc,
        )
        val root = ProcDecl(
            "Root",
            listOf(
                ab,
                ProcDecl("Prov", emptyList(), ProcDeclType.Proc),
                ProcDecl("Cli", emptyList(), ProcDeclType.Proc),
            ),
            ProcDeclType.Proc,
        )
        val result = computeCompositionAlphabet(root, listOf(root, ab), leafMap)
        assertTrue(result.errors.isEmpty(), result.errors.toString())
        assertTrue(alphabetIntegrityErrors(result).isEmpty(), alphabetIntegrityErrors(result).toString())
        val externalW = result.external.filter { it.name == "w" }
        assertEquals(1, externalW.size)
        assertTrue(externalW.single().isProvider)
        assertTrue(result.allOffers.filter { it.name == "w" && it.isClient }.all { it.compositionHidden })
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
    fun sameClassClientsDoNotComposeError() {
        val left = listOf(offer("X", "w", client = true).copy(occurrenceId = "x1"))
        val right = listOf(offer("X", "w", client = true).copy(occurrenceId = "x2"))
        val (composed, errs) = composeAlphabets(left, right, "scope")
        assertTrue(errs.isEmpty(), errs.toString())
        assertEquals(2, composed.size)
        assertTrue(composed.none { it.compositionHidden })
    }

    @Test
    fun composedProcFunFoldsNonSyntheticOffersIntoParent() {
        val leafMap = mapOf(
            "Handler" to listOf(offer("Handler", "dispatch")),
            "clientAppendRPC" to listOf(
                offer("clientAppendRPC", "clientAppendRPC_call").copy(isConstructor = true),
                offer("clientAppendRPC", "noLeader", client = true),
                offer("clientAppendRPC", "committed", client = true),
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
        assertTrue(result.external.any { it.name == "noLeader" && it.isClient })
        assertTrue(result.external.any { it.name == "committed" && it.isClient })
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
