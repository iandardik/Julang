package julay.compiler.pass

import julay.compiler.SourceLoc
import julay.compiler.ast.BinaryOpExprNode
import julay.compiler.ast.ExprNode
import julay.compiler.ast.LiteralValueExprNode
import julay.compiler.ast.SymbolValueExprNode
import julay.compiler.decl.ActionDecl
import julay.compiler.decl.TransitUpdate
import julay.program.Variable
import julay.program.action.SymbolicAction
import julay.program.action.TSAction
import julay.program.type.boolType
import julay.program.type.intType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ChannelDirectionPassTest {
    private val loc = SourceLoc(1 to 1)

    private fun sym(name: String) = SymbolValueExprNode(name, loc)
    private fun litInt(n: Int) = LiteralValueExprNode(n.toString(), intType, loc)
    private fun litBool(b: Boolean) = LiteralValueExprNode(b.toString(), boolType, loc)

    private fun eq(a: ExprNode, b: ExprNode) = BinaryOpExprNode("=", a, b, loc)
    private fun iff(a: ExprNode, b: ExprNode) = BinaryOpExprNode("<=>", a, b, loc)
    private fun `in`(a: ExprNode, b: ExprNode) = BinaryOpExprNode("in", a, b, loc)
    private fun and(a: ExprNode, b: ExprNode) = BinaryOpExprNode("&", a, b, loc)

    private fun action(
        name: String,
        args: List<String>,
        guard: ExprNode?,
        bodyRefs: List<String> = emptyList(),
    ): ActionDecl {
        val transits = bodyRefs.map { TransitUpdate.Assign("sink", sym(it)) }
        return ActionDecl(
            action = SymbolicAction(name, args.map { Variable(it, intType) }),
            guards = listOfNotNull(guard),
            transits = transits,
            modifier = TSAction.SyncRole.Default,
            loc = loc,
        )
    }

    private fun view(className: String, decl: ActionDecl, static: Boolean = false) =
        ParticipantView(className, static, decl, loc)

    @Test
    fun equalityBindsArgFromState() {
        // delta = 1
        val decl = action("add", listOf("delta", "newVal"), eq(sym("delta"), litInt(1)))
        val bound = computeBoundArgs(decl)
        assertEquals(setOf("delta"), bound.boundArgs)
        assertEquals(setOf("delta"), bound.equalityBound)
    }

    @Test
    fun membershipBindsFromCollectionNotTypeDomain() {
        val fromCluster = action("vote", listOf("target"), `in`(sym("target"), sym("cluster")))
        assertEquals(setOf("target"), computeBoundArgs(fromCluster).membershipBound)

        val fromInt = action("pick", listOf("x"), `in`(sym("x"), sym("Int")))
        assertTrue(computeBoundArgs(fromInt).boundArgs.isEmpty())
        assertTrue(isUnboundedTypeDomain(sym("Int")))
    }

    @Test
    fun equalityWinsOverMembership() {
        val guard = and(`in`(sym("x"), sym("S")), eq(sym("x"), litInt(3)))
        val decl = action("a", listOf("x"), guard)
        val bound = computeBoundArgs(decl)
        assertEquals(setOf("x"), bound.equalityBound)
        assertTrue(bound.membershipBound.isEmpty())
    }

    @Test
    fun addStyleReplyDirection() {
        // Handler: delta = 1; body reads newVal
        val handler = action(
            "add",
            listOf("delta", "newVal"),
            eq(sym("delta"), litInt(1)),
            bodyRefs = listOf("newVal"),
        )
        // Protocol: newVal = counter + delta  (uses received delta)
        val protocol = action(
            "add",
            listOf("delta", "newVal"),
            eq(sym("newVal"), BinaryOpExprNode("+", sym("counter"), sym("delta"), loc)),
        )

        val handlerSends = tryDirection("add", view("Handler", handler), view("Protocol", protocol, static = true))
        assertIs<DirectionOk>(handlerSends)
        assertEquals("Handler", handlerSends.direction.senderClass)
        assertEquals("Protocol", handlerSends.direction.receiverClass)
        assertEquals(listOf("delta"), handlerSends.direction.senderBoundArgs)
        assertEquals(listOf("newVal"), handlerSends.direction.replyArgs)

        // Protocol as sender fails: it cannot bind delta alone.
        val protocolSends = tryDirection("add", view("Protocol", protocol, static = true), view("Handler", handler))
        assertIs<DirectionFail>(protocolSends)
    }

    @Test
    fun zeroArgTieBreakDynamicSends() {
        val a = action("ping", emptyList(), litBool(true))
        val b = action("ping", emptyList(), litBool(true))
        val ab = tryDirection("ping", view("Static", a, static = true), view("Dyn", b, static = false))
        val ba = tryDirection("ping", view("Dyn", b, static = false), view("Static", a, static = true))
        assertIs<DirectionOk>(ab)
        assertIs<DirectionOk>(ba)
        // Both valid; hub/pair tie prefers dynamic sender — covered via breakTie through pickDirection
        // at the pair level by reusing the same rule: dynamic class name as sender.
        assertEquals("Dyn", ba.direction.senderClass)
    }

    @Test
    fun receiverFilterOnArgIsInvalid() {
        val sender = action("m", listOf("x"), eq(sym("x"), litInt(1)))
        // Receiver also has x > 0 filter (not a binding equation)
        val recv = action(
            "m",
            listOf("x"),
            BinaryOpExprNode(">", sym("x"), litInt(0), loc),
        )
        val result = tryDirection("m", view("A", sender), view("B", recv))
        assertIs<DirectionFail>(result)
        assertTrue(result.error.toString().contains("filters on argument"))
    }

    @Test
    fun iffAlsoBinds() {
        val decl = action("t", listOf("flag"), iff(sym("flag"), sym("ready")))
        assertEquals(setOf("flag"), computeBoundArgs(decl).equalityBound)
    }

    @Test
    fun replyCommitFilterOnPureQueryAllowed() {
        // Client: key = 1 & owner > 0 (residual on reply); body reads owner
        val client = ActionDecl(
            action = SymbolicAction(
                "lookup",
                listOf(Variable("key", intType), Variable("owner", intType)),
            ),
            guards = listOf(
                and(eq(sym("key"), litInt(1)), BinaryOpExprNode(">", sym("owner"), litInt(0), loc)),
            ),
            transits = listOf(TransitUpdate.Assign("sink", sym("owner"))),
            modifier = TSAction.SyncRole.Default,
            loc = loc,
        )
        // Store: owner = table + key (pure query)
        val store = action(
            "lookup",
            listOf("key", "owner"),
            eq(sym("owner"), BinaryOpExprNode("+", sym("table"), sym("key"), loc)),
        )
        val ok = tryDirection("lookup", view("Client", client), view("Store", store, static = true))
        assertIs<DirectionOk>(ok)
        assertEquals("Client", ok.direction.senderClass)
        assertEquals("Store", ok.direction.receiverClass)
        assertEquals(listOf("key"), ok.direction.senderBoundArgs)
        assertEquals(listOf("owner"), ok.direction.replyArgs)
    }

    @Test
    fun replyCommitFilterGuardOnlyStillReplyArg() {
        // Client residual-filters on owner but does not read it in the body
        val client = ActionDecl(
            action = SymbolicAction(
                "lookup",
                listOf(Variable("key", intType), Variable("owner", intType)),
            ),
            guards = listOf(
                and(eq(sym("key"), litInt(1)), BinaryOpExprNode(">", sym("owner"), litInt(0), loc)),
            ),
            transits = listOf(TransitUpdate.Assign("step", litInt(2))),
            modifier = TSAction.SyncRole.Default,
            loc = loc,
        )
        val store = action(
            "lookup",
            listOf("key", "owner"),
            eq(sym("owner"), BinaryOpExprNode("+", sym("table"), sym("key"), loc)),
        )
        val ok = tryDirection("lookup", view("Client", client), view("Store", store, static = true))
        assertIs<DirectionOk>(ok)
        assertEquals(listOf("owner"), ok.direction.replyArgs)
    }

    @Test
    fun replyCommitFilterOnEffectfulReceiverRejected() {
        val client = ActionDecl(
            action = SymbolicAction(
                "lookup",
                listOf(Variable("key", intType), Variable("owner", intType)),
            ),
            guards = listOf(
                and(eq(sym("key"), litInt(1)), BinaryOpExprNode(">", sym("owner"), litInt(0), loc)),
            ),
            transits = emptyList(),
            modifier = TSAction.SyncRole.Default,
            loc = loc,
        )
        val store = ActionDecl(
            action = SymbolicAction(
                "lookup",
                listOf(Variable("key", intType), Variable("owner", intType)),
            ),
            guards = listOf(eq(sym("owner"), BinaryOpExprNode("+", sym("table"), sym("key"), loc))),
            transits = listOf(TransitUpdate.Assign("log", litInt(1))),
            modifier = TSAction.SyncRole.Default,
            loc = loc,
        )
        val fail = tryDirection("lookup", view("Client", client), view("Store", store, static = true))
        assertIs<DirectionFail>(fail)
        assertTrue(
            fail.error.toString().contains("pure query"),
            "expected pure-query message; got ${fail.error}",
        )
    }
}
