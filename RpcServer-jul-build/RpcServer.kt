import com.microsoft.z3.*
import com.microsoft.z3.julangContext
import julay.program.*
import julay.program.type.*
import julay.program.action.*
import julay.program.library.*
import julay.program.sync.*
import julay.tools.mkStringConst
import julay.tools.mkSeqLengthAny
import julay.tools.mkSeqNthAny
import julay.tools.mkSeqConcatAny
import julay.tools.mkSeqExtractAny
import julay.tools.mkListMemberAny
import julay.tools.mkSetMemberAny
import julay.tools.mkSetUnionAny
import julay.tools.mkSetDifferenceAny
import julay.tools.mkSetAddAny
import julay.tools.setCellArrExpr
import julay.tools.setCellSizeExpr
import julay.tools.setMkCellExpr
import julay.tools.mapCellArrExpr
import julay.tools.mapCellKeysExpr
import julay.tools.mapCellSizeExpr
import julay.tools.mapSelectExpr
import julay.tools.mapStoreExpr
import julay.tools.mapSetAddExpr
import julay.tools.mapMkCellExpr

val listType_String = listType(stringType)

class Protocol(    
    private val program: Program
) : TransitionSystem {
    private lateinit var hostProc: Proc
    private var sessionPeer: Proc? = null
    private var _procFunReturn: Value? = null
    override fun bindHostProc(host: Proc) { hostProc = host }
    override fun setSessionPeer(peer: Proc?) { sessionPeer = peer }
    override fun consumeProcFunReturn(): Value? {
        val v = _procFunReturn
        _procFunReturn = null
        return v
    }
    private var _counter: Int? = null
    private var counter: Int
        get() = _counter ?: throw JulayException("State variable \"counter\" read before it was initialized")
        set(value) { _counter = value }    
    override suspend fun finishConstruction(act: ConcreteAction) {    
        when (act.symAction.name) {    
            "initially" -> {    
                val __transitRhs_0: Int = 0
                counter = __transitRhs_0
            }    
            else -> {}    
        }
    }    
    override suspend fun actions(ctx: Context): Set<TSAction> = setOf(
        TSAction(    
            SymbolicAction("increment", listOf(Variable("newVal", intType))),    
            ctx.mkEq(ctx.mkIntConst("newVal"),ctx.mkAdd(ctx.mkInt(counter),ctx.mkInt(1))),    
            TSAction.SyncRole.Provider    ,
            fastGuard = BoolExprFast.Eq(SyncTerm.Arg("newVal", SyncTerm.Arg.Sort.Int), SyncTerm.IntArith(SyncTerm.IntArith.Op.Add, SyncTerm.Local("counter"), SyncTerm.Ground(SyncGround.IntVal(1))))
        ),
        TSAction(    
            SymbolicAction("add", listOf(Variable("delta", intType), Variable("newVal", intType))),    
            ctx.mkEq(ctx.mkIntConst("newVal"),ctx.mkAdd(ctx.mkInt(counter),ctx.mkIntConst("delta"))),    
            TSAction.SyncRole.Provider    ,
            fastGuard = BoolExprFast.Eq(SyncTerm.Arg("newVal", SyncTerm.Arg.Sort.Int), SyncTerm.IntArith(SyncTerm.IntArith.Op.Add, SyncTerm.Local("counter"), SyncTerm.Arg("delta", SyncTerm.Arg.Sort.Int)))
        ),
        TSAction(    
            SymbolicAction("getCounter", listOf(Variable("counterVal", intType))),    
            ctx.mkEq(ctx.mkIntConst("counterVal"),ctx.mkInt(counter)),    
            TSAction.SyncRole.Provider    ,
            fastGuard = BoolExprFast.Eq(SyncTerm.Arg("counterVal", SyncTerm.Arg.Sort.Int), SyncTerm.Local("counter"))
        )
    )    
    override fun syncStepPlan(): SyncStepPlan {
        val __locals: Map<String, Any?> = mapOf("counter" to _counter)
        val __offers = mutableListOf<FastOffer>()
        run {
            val __g = BoolExprFast.Eq(SyncTerm.Arg("newVal", SyncTerm.Arg.Sort.Int), SyncTerm.IntArith(SyncTerm.IntArith.Op.Add, SyncTerm.Local("counter"), SyncTerm.Ground(SyncGround.IntVal(1))))
            val __grounded = SyncResolveFast.groundForOffer(__g, __locals)
            if (__grounded != null) {
                __offers.add(FastOffer(SymbolicAction("increment", listOf(Variable("newVal", intType))), __grounded, TSAction.SyncRole.Provider))
            }
        }
        run {
            val __g = BoolExprFast.Eq(SyncTerm.Arg("newVal", SyncTerm.Arg.Sort.Int), SyncTerm.IntArith(SyncTerm.IntArith.Op.Add, SyncTerm.Local("counter"), SyncTerm.Arg("delta", SyncTerm.Arg.Sort.Int)))
            val __grounded = SyncResolveFast.groundForOffer(__g, __locals)
            if (__grounded != null) {
                __offers.add(FastOffer(SymbolicAction("add", listOf(Variable("delta", intType), Variable("newVal", intType))), __grounded, TSAction.SyncRole.Provider))
            }
        }
        run {
            val __g = BoolExprFast.Eq(SyncTerm.Arg("counterVal", SyncTerm.Arg.Sort.Int), SyncTerm.Local("counter"))
            val __grounded = SyncResolveFast.groundForOffer(__g, __locals)
            if (__grounded != null) {
                __offers.add(FastOffer(SymbolicAction("getCounter", listOf(Variable("counterVal", intType))), __grounded, TSAction.SyncRole.Provider))
            }
        }
        return SyncStepPlan.FastOnly(__offers)
    }    
    override suspend fun transit(act: ConcreteAction) {    
        return when (act.symAction.name) {    
            "increment" -> {    
                val __transitRhs_0: Int = (counter + 1)
                counter = __transitRhs_0
            }
            "add" -> {    
                val __transitRhs_0: Int = (counter + (act.lookup(Variable("delta", intType)).value as Int))
                counter = __transitRhs_0
            }
            "getCounter" -> {    
    
            }    
            else -> throw RuntimeException("Action is outside my alphabet: ${act.symAction}")    
        }
    }
}

class ServerInitializer(    
    private val program: Program
) : TransitionSystem {
    private lateinit var hostProc: Proc
    private var sessionPeer: Proc? = null
    private var _procFunReturn: Value? = null
    override fun bindHostProc(host: Proc) { hostProc = host }
    override fun setSessionPeer(peer: Proc?) { sessionPeer = peer }
    override fun consumeProcFunReturn(): Value? {
        val v = _procFunReturn
        _procFunReturn = null
        return v
    }
    private var _started: Boolean? = null
    private var started: Boolean
        get() = _started ?: throw JulayException("State variable \"started\" read before it was initialized")
        set(value) { _started = value }    
    override suspend fun finishConstruction(act: ConcreteAction) {    
        when (act.symAction.name) {    
            "initially" -> {    
                val __transitRhs_0: Boolean = false
                started = __transitRhs_0
            }    
            else -> {}    
        }
    }    
    override suspend fun actions(ctx: Context): Set<TSAction> = setOf(
        TSAction(    
            SymbolicAction("listen", listOf(Variable("port", intType), Variable("handler", stringType)), isSession = true, channelKey = "RpcIn_1#listen"),    
            ctx.mkAnd(ctx.mkAnd(ctx.mkNot(ctx.mkBool(started)),ctx.mkEq(ctx.mkIntConst("port"),ctx.mkInt(8000))),ctx.mkEq(ctx.mkStringConst("handler"),ctx.mkString("handleRpc"))),    
            TSAction.SyncRole.Default    
        ),
        TSAction(    
            SymbolicAction("close", listOf(), isSession = true, channelKey = "RpcIn_1#close"),    
            ctx.mkBool(false),    
            TSAction.SyncRole.Default    ,
            fastGuard = BoolExprFast.Eq(SyncTerm.Ground(SyncGround.BoolVal(true)), SyncTerm.Ground(SyncGround.BoolVal(false)))
        )
    )    
    override fun syncStepPlan(): SyncStepPlan = SyncStepPlan.NeedsZ3    
    override suspend fun transit(act: ConcreteAction) {    
        return when (act.symAction.name) {    
            "listen" -> {    
                val __transitRhs_0: Boolean = true
                started = __transitRhs_0
            }
            "close" -> {    
    
            }    
            else -> throw RuntimeException("Action is outside my alphabet: ${act.symAction}")    
        }
    }
}

class handleRpc(    
    private val program: Program
) : TransitionSystem {
    private lateinit var hostProc: Proc
    private var sessionPeer: Proc? = null
    private var _procFunReturn: Value? = null
    override fun bindHostProc(host: Proc) { hostProc = host }
    override fun setSessionPeer(peer: Proc?) { sessionPeer = peer }
    override fun consumeProcFunReturn(): Value? {
        val v = _procFunReturn
        _procFunReturn = null
        return v
    }
    private var _req: HttpServerRequest? = null
    private var req: HttpServerRequest
        get() = _req ?: throw JulayException("State variable \"req\" read before it was initialized")
        set(value) { _req = value }
    private var _retVal: HttpServerResponse? = null
    private var retVal: HttpServerResponse
        get() = _retVal ?: throw JulayException("State variable \"retVal\" read before it was initialized")
        set(value) { _retVal = value }
    private var ___julayFuse: String? = null
    private var __julayFuse: String
        get() = ___julayFuse ?: throw JulayException("State variable \"__julayFuse\" read before it was initialized")
        set(value) { ___julayFuse = value }
    private var ___julayFuseDest: String? = null
    private var __julayFuseDest: String
        get() = ___julayFuseDest ?: throw JulayException("State variable \"__julayFuseDest\" read before it was initialized")
        set(value) { ___julayFuseDest = value }
    private var _inIncrementRPC__req: HttpServerRequest? = null
    private var inIncrementRPC__req: HttpServerRequest
        get() = _inIncrementRPC__req ?: throw JulayException("State variable \"inIncrementRPC__req\" read before it was initialized")
        set(value) { _inIncrementRPC__req = value }
    private var _inIncrementRPC__step: String? = null
    private var inIncrementRPC__step: String
        get() = _inIncrementRPC__step ?: throw JulayException("State variable \"inIncrementRPC__step\" read before it was initialized")
        set(value) { _inIncrementRPC__step = value }
    private var _inIncrementRPC__result: Int? = null
    private var inIncrementRPC__result: Int
        get() = _inIncrementRPC__result ?: throw JulayException("State variable \"inIncrementRPC__result\" read before it was initialized")
        set(value) { _inIncrementRPC__result = value }
    private var _inGetRPC__req: HttpServerRequest? = null
    private var inGetRPC__req: HttpServerRequest
        get() = _inGetRPC__req ?: throw JulayException("State variable \"inGetRPC__req\" read before it was initialized")
        set(value) { _inGetRPC__req = value }
    private var _inGetRPC__step: String? = null
    private var inGetRPC__step: String
        get() = _inGetRPC__step ?: throw JulayException("State variable \"inGetRPC__step\" read before it was initialized")
        set(value) { _inGetRPC__step = value }
    private var _inGetRPC__result: Int? = null
    private var inGetRPC__result: Int
        get() = _inGetRPC__result ?: throw JulayException("State variable \"inGetRPC__result\" read before it was initialized")
        set(value) { _inGetRPC__result = value }
    private var _inAddRPC__req: HttpServerRequest? = null
    private var inAddRPC__req: HttpServerRequest
        get() = _inAddRPC__req ?: throw JulayException("State variable \"inAddRPC__req\" read before it was initialized")
        set(value) { _inAddRPC__req = value }
    private var _inAddRPC__deltaVal: Int? = null
    private var inAddRPC__deltaVal: Int
        get() = _inAddRPC__deltaVal ?: throw JulayException("State variable \"inAddRPC__deltaVal\" read before it was initialized")
        set(value) { _inAddRPC__deltaVal = value }
    private var _inAddRPC__step: String? = null
    private var inAddRPC__step: String
        get() = _inAddRPC__step ?: throw JulayException("State variable \"inAddRPC__step\" read before it was initialized")
        set(value) { _inAddRPC__step = value }
    private var _inAddRPC__result: Int? = null
    private var inAddRPC__result: Int
        get() = _inAddRPC__result ?: throw JulayException("State variable \"inAddRPC__result\" read before it was initialized")
        set(value) { _inAddRPC__result = value }    
    override suspend fun finishConstruction(act: ConcreteAction) {    
        when (act.symAction.name) {    
            "handleRpc_call" -> {    
                val __transitRhs_0: String = ""
                __julayFuse = __transitRhs_0
                val __transitRhs_1: String = ""
                __julayFuseDest = __transitRhs_1
                val __transitRhs_2: String = "call"
                inIncrementRPC__step = __transitRhs_2
                val __transitRhs_3: Int = -1
                inIncrementRPC__result = __transitRhs_3
                val __transitRhs_4: String = "call"
                inGetRPC__step = __transitRhs_4
                val __transitRhs_5: Int = -1
                inGetRPC__result = __transitRhs_5
                val __transitRhs_6: String = "call"
                inAddRPC__step = __transitRhs_6
                val __transitRhs_7: Int = -1
                inAddRPC__result = __transitRhs_7
                val __transitRhs_8: HttpServerRequest = (act.lookup(Variable("req", httpServerRequestType)).value as HttpServerRequest)
                req = __transitRhs_8
            }    
            else -> {}    
        }
    }    
    override suspend fun actions(ctx: Context): Set<TSAction> = setOf(
        TSAction(    
            SymbolicAction("route", listOf(), isInternal = true, channelKey = "handleRpc_occ4#internal#route"),    
            if (__julayFuse == "") ctx.mkEq(ctx.mkString(__julayFuse),ctx.mkString("")) else ctx.mkFalse(),    
            TSAction.SyncRole.Internal    ,
            fastGuard = if (__julayFuse == "") BoolExprFast.Eq(SyncTerm.Local("__julayFuse"), SyncTerm.Ground(SyncGround.StringVal(""))) else null
        ),
        TSAction(    
            SymbolicAction("increment", listOf(Variable("newVal", intType))),    
            if (__julayFuse == "inIncrementRPC") ctx.mkAnd(ctx.mkEq(ctx.mkString(__julayFuse),ctx.mkString("inIncrementRPC")), ctx.mkEq(ctx.mkString(inIncrementRPC__step),ctx.mkString("call"))) else ctx.mkFalse(),    
            TSAction.SyncRole.Client    ,
            fastGuard = if (__julayFuse == "inIncrementRPC") BoolExprFast.And(listOf(BoolExprFast.Eq(SyncTerm.Local("__julayFuse"), SyncTerm.Ground(SyncGround.StringVal("inIncrementRPC"))), BoolExprFast.Eq(SyncTerm.Local("inIncrementRPC__step"), SyncTerm.Ground(SyncGround.StringVal("call"))))) else null
        ),
        TSAction(    
            SymbolicAction("respond", listOf(), isInternal = true, channelKey = "inIncrementRPC_occ7#internal#respond"),    
            if (__julayFuse == "inIncrementRPC") ctx.mkAnd(ctx.mkEq(ctx.mkString(__julayFuse),ctx.mkString("inIncrementRPC")), ctx.mkEq(ctx.mkString(inIncrementRPC__step),ctx.mkString("respond"))) else ctx.mkFalse(),    
            TSAction.SyncRole.Internal    ,
            fastGuard = if (__julayFuse == "inIncrementRPC") BoolExprFast.And(listOf(BoolExprFast.Eq(SyncTerm.Local("__julayFuse"), SyncTerm.Ground(SyncGround.StringVal("inIncrementRPC"))), BoolExprFast.Eq(SyncTerm.Local("inIncrementRPC__step"), SyncTerm.Ground(SyncGround.StringVal("respond"))))) else null
        ),
        TSAction(    
            SymbolicAction("getCounter", listOf(Variable("counterVal", intType))),    
            if (__julayFuse == "inGetRPC") ctx.mkAnd(ctx.mkEq(ctx.mkString(__julayFuse),ctx.mkString("inGetRPC")), ctx.mkEq(ctx.mkString(inGetRPC__step),ctx.mkString("call"))) else ctx.mkFalse(),    
            TSAction.SyncRole.Client    ,
            fastGuard = if (__julayFuse == "inGetRPC") BoolExprFast.And(listOf(BoolExprFast.Eq(SyncTerm.Local("__julayFuse"), SyncTerm.Ground(SyncGround.StringVal("inGetRPC"))), BoolExprFast.Eq(SyncTerm.Local("inGetRPC__step"), SyncTerm.Ground(SyncGround.StringVal("call"))))) else null
        ),
        TSAction(    
            SymbolicAction("respond", listOf(), isInternal = true, channelKey = "inGetRPC_occ6#internal#respond"),    
            if (__julayFuse == "inGetRPC") ctx.mkAnd(ctx.mkEq(ctx.mkString(__julayFuse),ctx.mkString("inGetRPC")), ctx.mkEq(ctx.mkString(inGetRPC__step),ctx.mkString("respond"))) else ctx.mkFalse(),    
            TSAction.SyncRole.Internal    ,
            fastGuard = if (__julayFuse == "inGetRPC") BoolExprFast.And(listOf(BoolExprFast.Eq(SyncTerm.Local("__julayFuse"), SyncTerm.Ground(SyncGround.StringVal("inGetRPC"))), BoolExprFast.Eq(SyncTerm.Local("inGetRPC__step"), SyncTerm.Ground(SyncGround.StringVal("respond"))))) else null
        ),
        TSAction(    
            SymbolicAction("add", listOf(Variable("delta", intType), Variable("newVal", intType))),    
            if (__julayFuse == "inAddRPC") ctx.mkAnd(ctx.mkEq(ctx.mkString(__julayFuse),ctx.mkString("inAddRPC")), ctx.mkAnd(ctx.mkEq(ctx.mkString(inAddRPC__step),ctx.mkString("call")),ctx.mkEq(ctx.mkIntConst("delta"),ctx.mkInt(inAddRPC__deltaVal)))) else ctx.mkFalse(),    
            TSAction.SyncRole.Client    ,
            fastGuard = if (__julayFuse == "inAddRPC") BoolExprFast.And(listOf(BoolExprFast.Eq(SyncTerm.Local("__julayFuse"), SyncTerm.Ground(SyncGround.StringVal("inAddRPC"))), BoolExprFast.And(listOf(BoolExprFast.Eq(SyncTerm.Local("inAddRPC__step"), SyncTerm.Ground(SyncGround.StringVal("call"))), BoolExprFast.Eq(SyncTerm.Arg("delta", SyncTerm.Arg.Sort.Int), SyncTerm.Local("inAddRPC__deltaVal")))))) else null
        ),
        TSAction(    
            SymbolicAction("respond", listOf(), isInternal = true, channelKey = "inAddRPC_occ5#internal#respond"),    
            if (__julayFuse == "inAddRPC") ctx.mkAnd(ctx.mkEq(ctx.mkString(__julayFuse),ctx.mkString("inAddRPC")), ctx.mkEq(ctx.mkString(inAddRPC__step),ctx.mkString("respond"))) else ctx.mkFalse(),    
            TSAction.SyncRole.Internal    ,
            fastGuard = if (__julayFuse == "inAddRPC") BoolExprFast.And(listOf(BoolExprFast.Eq(SyncTerm.Local("__julayFuse"), SyncTerm.Ground(SyncGround.StringVal("inAddRPC"))), BoolExprFast.Eq(SyncTerm.Local("inAddRPC__step"), SyncTerm.Ground(SyncGround.StringVal("respond"))))) else null
        )
    )    
    override fun syncStepPlan(): SyncStepPlan {
        val __locals: Map<String, Any?> = mapOf("__julayFuse" to ___julayFuse, "__julayFuseDest" to ___julayFuseDest, "inIncrementRPC__step" to _inIncrementRPC__step, "inIncrementRPC__result" to _inIncrementRPC__result, "inGetRPC__step" to _inGetRPC__step, "inGetRPC__result" to _inGetRPC__result, "inAddRPC__deltaVal" to _inAddRPC__deltaVal, "inAddRPC__step" to _inAddRPC__step, "inAddRPC__result" to _inAddRPC__result)
        val __offers = mutableListOf<FastOffer>()
        run {
            if (__julayFuse != "") {
                // Idle fused slice: skip before reading uninitialized callee locals.
            } else {
                val __g = BoolExprFast.Eq(SyncTerm.Local("__julayFuse"), SyncTerm.Ground(SyncGround.StringVal("")))
                val __grounded = SyncResolveFast.groundForOffer(__g, __locals)
                if (__grounded != null) {
                    __offers.add(FastOffer(SymbolicAction("route", listOf(), isInternal = true, channelKey = "handleRpc_occ4#internal#route"), __grounded, TSAction.SyncRole.Internal))
                }
            }
        }
        run {
            if (__julayFuse != "inIncrementRPC") {
                // Idle fused slice: skip before reading uninitialized callee locals.
            } else {
                val __g = BoolExprFast.And(listOf(BoolExprFast.Eq(SyncTerm.Local("__julayFuse"), SyncTerm.Ground(SyncGround.StringVal("inIncrementRPC"))), BoolExprFast.Eq(SyncTerm.Local("inIncrementRPC__step"), SyncTerm.Ground(SyncGround.StringVal("call")))))
                val __grounded = SyncResolveFast.groundForOffer(__g, __locals)
                if (__grounded != null) {
                    __offers.add(FastOffer(SymbolicAction("increment", listOf(Variable("newVal", intType))), __grounded, TSAction.SyncRole.Client))
                }
            }
        }
        run {
            if (__julayFuse != "inIncrementRPC") {
                // Idle fused slice: skip before reading uninitialized callee locals.
            } else {
                val __g = BoolExprFast.And(listOf(BoolExprFast.Eq(SyncTerm.Local("__julayFuse"), SyncTerm.Ground(SyncGround.StringVal("inIncrementRPC"))), BoolExprFast.Eq(SyncTerm.Local("inIncrementRPC__step"), SyncTerm.Ground(SyncGround.StringVal("respond")))))
                val __grounded = SyncResolveFast.groundForOffer(__g, __locals)
                if (__grounded != null) {
                    __offers.add(FastOffer(SymbolicAction("respond", listOf(), isInternal = true, channelKey = "inIncrementRPC_occ7#internal#respond"), __grounded, TSAction.SyncRole.Internal))
                }
            }
        }
        run {
            if (__julayFuse != "inGetRPC") {
                // Idle fused slice: skip before reading uninitialized callee locals.
            } else {
                val __g = BoolExprFast.And(listOf(BoolExprFast.Eq(SyncTerm.Local("__julayFuse"), SyncTerm.Ground(SyncGround.StringVal("inGetRPC"))), BoolExprFast.Eq(SyncTerm.Local("inGetRPC__step"), SyncTerm.Ground(SyncGround.StringVal("call")))))
                val __grounded = SyncResolveFast.groundForOffer(__g, __locals)
                if (__grounded != null) {
                    __offers.add(FastOffer(SymbolicAction("getCounter", listOf(Variable("counterVal", intType))), __grounded, TSAction.SyncRole.Client))
                }
            }
        }
        run {
            if (__julayFuse != "inGetRPC") {
                // Idle fused slice: skip before reading uninitialized callee locals.
            } else {
                val __g = BoolExprFast.And(listOf(BoolExprFast.Eq(SyncTerm.Local("__julayFuse"), SyncTerm.Ground(SyncGround.StringVal("inGetRPC"))), BoolExprFast.Eq(SyncTerm.Local("inGetRPC__step"), SyncTerm.Ground(SyncGround.StringVal("respond")))))
                val __grounded = SyncResolveFast.groundForOffer(__g, __locals)
                if (__grounded != null) {
                    __offers.add(FastOffer(SymbolicAction("respond", listOf(), isInternal = true, channelKey = "inGetRPC_occ6#internal#respond"), __grounded, TSAction.SyncRole.Internal))
                }
            }
        }
        run {
            if (__julayFuse != "inAddRPC") {
                // Idle fused slice: skip before reading uninitialized callee locals.
            } else {
                val __g = BoolExprFast.And(listOf(BoolExprFast.Eq(SyncTerm.Local("__julayFuse"), SyncTerm.Ground(SyncGround.StringVal("inAddRPC"))), BoolExprFast.And(listOf(BoolExprFast.Eq(SyncTerm.Local("inAddRPC__step"), SyncTerm.Ground(SyncGround.StringVal("call"))), BoolExprFast.Eq(SyncTerm.Arg("delta", SyncTerm.Arg.Sort.Int), SyncTerm.Local("inAddRPC__deltaVal"))))))
                val __grounded = SyncResolveFast.groundForOffer(__g, __locals)
                if (__grounded != null) {
                    __offers.add(FastOffer(SymbolicAction("add", listOf(Variable("delta", intType), Variable("newVal", intType))), __grounded, TSAction.SyncRole.Client))
                }
            }
        }
        run {
            if (__julayFuse != "inAddRPC") {
                // Idle fused slice: skip before reading uninitialized callee locals.
            } else {
                val __g = BoolExprFast.And(listOf(BoolExprFast.Eq(SyncTerm.Local("__julayFuse"), SyncTerm.Ground(SyncGround.StringVal("inAddRPC"))), BoolExprFast.Eq(SyncTerm.Local("inAddRPC__step"), SyncTerm.Ground(SyncGround.StringVal("respond")))))
                val __grounded = SyncResolveFast.groundForOffer(__g, __locals)
                if (__grounded != null) {
                    __offers.add(FastOffer(SymbolicAction("respond", listOf(), isInternal = true, channelKey = "inAddRPC_occ5#internal#respond"), __grounded, TSAction.SyncRole.Internal))
                }
            }
        }
        return SyncStepPlan.FastOnly(__offers)
    }    
    override suspend fun transit(act: ConcreteAction) {    
        return when (act.symAction.channelKey to __julayFuse) {    
            ("handleRpc_occ4#internal#route" to "") -> {    
                if ((req.path == "rpc/increment")) {
                    __julayFuse = "inIncrementRPC"
                    __julayFuseDest = "__hostRet__"
                    inIncrementRPC__req = req
                    inIncrementRPC__step = "call"
                    inIncrementRPC__result = -1
                } else {
                    if ((req.path == "rpc/get")) {
                        __julayFuse = "inGetRPC"
                        __julayFuseDest = "__hostRet__"
                        inGetRPC__req = req
                        inGetRPC__step = "call"
                        inGetRPC__result = -1
                    } else {
                        if ((req.path == "rpc/add")) {
                            __julayFuse = "inAddRPC"
                            __julayFuseDest = "__hostRet__"
                            inAddRPC__req = req
                            inAddRPC__deltaVal = inAddRPC__req.body.split("|")[(1) - 1].split("=")[(2) - 1].toInt()
                            inAddRPC__step = "call"
                            inAddRPC__result = -1
                        } else {
                            val __procFunRet: HttpServerResponse = HttpServerResponse(body = "NOT_FOUND", code = 404)
                            _procFunReturn = Value(__procFunRet, httpServerResponseType)
                        }
                    }
                }
            }
            ("increment" to "inIncrementRPC") -> {    
                val __transitRhs_0: String = "respond"
                val __transitRhs_1: Int = (act.lookup(Variable("newVal", intType)).value as Int)
                inIncrementRPC__step = __transitRhs_0
                inIncrementRPC__result = __transitRhs_1
            }
            ("inIncrementRPC_occ7#internal#respond" to "inIncrementRPC") -> {    
                val __procFunRet: HttpServerResponse = HttpServerResponse(body = ("v=" + (inIncrementRPC__result).toString()), code = 200)
                val __fuseDest = __julayFuseDest
                when (__fuseDest) {
                    "__hostRet__" -> {
                        __julayFuse = ""
                        __julayFuseDest = ""
                        _procFunReturn = Value(__procFunRet, httpServerResponseType)
                    }
                    else -> throw JulayException("procfun-fuse: unknown dest $__fuseDest")
                }
            }
            ("getCounter" to "inGetRPC") -> {    
                val __transitRhs_0: String = "respond"
                val __transitRhs_1: Int = (act.lookup(Variable("counterVal", intType)).value as Int)
                inGetRPC__step = __transitRhs_0
                inGetRPC__result = __transitRhs_1
            }
            ("inGetRPC_occ6#internal#respond" to "inGetRPC") -> {    
                val __procFunRet: HttpServerResponse = HttpServerResponse(body = ("v=" + (inGetRPC__result).toString()), code = 200)
                val __fuseDest = __julayFuseDest
                when (__fuseDest) {
                    "__hostRet__" -> {
                        __julayFuse = ""
                        __julayFuseDest = ""
                        _procFunReturn = Value(__procFunRet, httpServerResponseType)
                    }
                    else -> throw JulayException("procfun-fuse: unknown dest $__fuseDest")
                }
            }
            ("add" to "inAddRPC") -> {    
                val __transitRhs_0: String = "respond"
                val __transitRhs_1: Int = (act.lookup(Variable("newVal", intType)).value as Int)
                inAddRPC__step = __transitRhs_0
                inAddRPC__result = __transitRhs_1
            }
            ("inAddRPC_occ5#internal#respond" to "inAddRPC") -> {    
                val __procFunRet: HttpServerResponse = HttpServerResponse(body = ("v=" + (inAddRPC__result).toString()), code = 200)
                val __fuseDest = __julayFuseDest
                when (__fuseDest) {
                    "__hostRet__" -> {
                        __julayFuse = ""
                        __julayFuseDest = ""
                        _procFunReturn = Value(__procFunRet, httpServerResponseType)
                    }
                    else -> throw JulayException("procfun-fuse: unknown dest $__fuseDest")
                }
            }    
            else -> throw RuntimeException("Action is outside my alphabet: ${act.symAction} phase=$__julayFuse")    
        }
    }
}

class inIncrementRPC(    
    private val program: Program
) : TransitionSystem {
    private lateinit var hostProc: Proc
    private var sessionPeer: Proc? = null
    private var _procFunReturn: Value? = null
    override fun bindHostProc(host: Proc) { hostProc = host }
    override fun setSessionPeer(peer: Proc?) { sessionPeer = peer }
    override fun consumeProcFunReturn(): Value? {
        val v = _procFunReturn
        _procFunReturn = null
        return v
    }
    private var _req: HttpServerRequest? = null
    private var req: HttpServerRequest
        get() = _req ?: throw JulayException("State variable \"req\" read before it was initialized")
        set(value) { _req = value }
    private var _step: String? = null
    private var step: String
        get() = _step ?: throw JulayException("State variable \"step\" read before it was initialized")
        set(value) { _step = value }
    private var _result: Int? = null
    private var result: Int
        get() = _result ?: throw JulayException("State variable \"result\" read before it was initialized")
        set(value) { _result = value }
    private var _retVal: HttpServerResponse? = null
    private var retVal: HttpServerResponse
        get() = _retVal ?: throw JulayException("State variable \"retVal\" read before it was initialized")
        set(value) { _retVal = value }    
    override suspend fun finishConstruction(act: ConcreteAction) {    
        when (act.symAction.name) {    
            "inIncrementRPC_call" -> {    
                val __transitRhs_0: HttpServerRequest = (act.lookup(Variable("req", httpServerRequestType)).value as HttpServerRequest)
                req = __transitRhs_0
                val __transitRhs_1: String = "call"
                step = __transitRhs_1
                val __transitRhs_2: Int = -1
                result = __transitRhs_2
            }    
            else -> {}    
        }
    }    
    override suspend fun actions(ctx: Context): Set<TSAction> = setOf(
        TSAction(    
            SymbolicAction("increment", listOf(Variable("newVal", intType))),    
            ctx.mkEq(ctx.mkString(step),ctx.mkString("call")),    
            TSAction.SyncRole.Client    ,
            fastGuard = BoolExprFast.Eq(SyncTerm.Local("step"), SyncTerm.Ground(SyncGround.StringVal("call")))
        ),
        TSAction(    
            SymbolicAction("respond", listOf(), isInternal = true, channelKey = "inIncrementRPC_occ7#internal#respond"),    
            ctx.mkEq(ctx.mkString(step),ctx.mkString("respond")),    
            TSAction.SyncRole.Internal    ,
            fastGuard = BoolExprFast.Eq(SyncTerm.Local("step"), SyncTerm.Ground(SyncGround.StringVal("respond")))
        )
    )    
    override fun syncStepPlan(): SyncStepPlan {
        val __locals: Map<String, Any?> = mapOf("step" to _step, "result" to _result)
        val __offers = mutableListOf<FastOffer>()
        run {
            val __g = BoolExprFast.Eq(SyncTerm.Local("step"), SyncTerm.Ground(SyncGround.StringVal("call")))
            val __grounded = SyncResolveFast.groundForOffer(__g, __locals)
            if (__grounded != null) {
                __offers.add(FastOffer(SymbolicAction("increment", listOf(Variable("newVal", intType))), __grounded, TSAction.SyncRole.Client))
            }
        }
        run {
            val __g = BoolExprFast.Eq(SyncTerm.Local("step"), SyncTerm.Ground(SyncGround.StringVal("respond")))
            val __grounded = SyncResolveFast.groundForOffer(__g, __locals)
            if (__grounded != null) {
                __offers.add(FastOffer(SymbolicAction("respond", listOf(), isInternal = true, channelKey = "inIncrementRPC_occ7#internal#respond"), __grounded, TSAction.SyncRole.Internal))
            }
        }
        return SyncStepPlan.FastOnly(__offers)
    }    
    override suspend fun transit(act: ConcreteAction) {    
        return when (act.symAction.name) {    
            "increment" -> {    
                val __transitRhs_0: String = "respond"
                val __transitRhs_1: Int = (act.lookup(Variable("newVal", intType)).value as Int)
                step = __transitRhs_0
                result = __transitRhs_1
            }
            "respond" -> {    
                val __procFunRet: HttpServerResponse = HttpServerResponse(body = ("v=" + (result).toString()), code = 200)
                _procFunReturn = Value(__procFunRet, httpServerResponseType)
            }    
            else -> throw RuntimeException("Action is outside my alphabet: ${act.symAction}")    
        }
    }
}

class inGetRPC(    
    private val program: Program
) : TransitionSystem {
    private lateinit var hostProc: Proc
    private var sessionPeer: Proc? = null
    private var _procFunReturn: Value? = null
    override fun bindHostProc(host: Proc) { hostProc = host }
    override fun setSessionPeer(peer: Proc?) { sessionPeer = peer }
    override fun consumeProcFunReturn(): Value? {
        val v = _procFunReturn
        _procFunReturn = null
        return v
    }
    private var _req: HttpServerRequest? = null
    private var req: HttpServerRequest
        get() = _req ?: throw JulayException("State variable \"req\" read before it was initialized")
        set(value) { _req = value }
    private var _step: String? = null
    private var step: String
        get() = _step ?: throw JulayException("State variable \"step\" read before it was initialized")
        set(value) { _step = value }
    private var _result: Int? = null
    private var result: Int
        get() = _result ?: throw JulayException("State variable \"result\" read before it was initialized")
        set(value) { _result = value }
    private var _retVal: HttpServerResponse? = null
    private var retVal: HttpServerResponse
        get() = _retVal ?: throw JulayException("State variable \"retVal\" read before it was initialized")
        set(value) { _retVal = value }    
    override suspend fun finishConstruction(act: ConcreteAction) {    
        when (act.symAction.name) {    
            "inGetRPC_call" -> {    
                val __transitRhs_0: HttpServerRequest = (act.lookup(Variable("req", httpServerRequestType)).value as HttpServerRequest)
                req = __transitRhs_0
                val __transitRhs_1: String = "call"
                step = __transitRhs_1
                val __transitRhs_2: Int = -1
                result = __transitRhs_2
            }    
            else -> {}    
        }
    }    
    override suspend fun actions(ctx: Context): Set<TSAction> = setOf(
        TSAction(    
            SymbolicAction("getCounter", listOf(Variable("counterVal", intType))),    
            ctx.mkEq(ctx.mkString(step),ctx.mkString("call")),    
            TSAction.SyncRole.Client    ,
            fastGuard = BoolExprFast.Eq(SyncTerm.Local("step"), SyncTerm.Ground(SyncGround.StringVal("call")))
        ),
        TSAction(    
            SymbolicAction("respond", listOf(), isInternal = true, channelKey = "inGetRPC_occ6#internal#respond"),    
            ctx.mkEq(ctx.mkString(step),ctx.mkString("respond")),    
            TSAction.SyncRole.Internal    ,
            fastGuard = BoolExprFast.Eq(SyncTerm.Local("step"), SyncTerm.Ground(SyncGround.StringVal("respond")))
        )
    )    
    override fun syncStepPlan(): SyncStepPlan {
        val __locals: Map<String, Any?> = mapOf("step" to _step, "result" to _result)
        val __offers = mutableListOf<FastOffer>()
        run {
            val __g = BoolExprFast.Eq(SyncTerm.Local("step"), SyncTerm.Ground(SyncGround.StringVal("call")))
            val __grounded = SyncResolveFast.groundForOffer(__g, __locals)
            if (__grounded != null) {
                __offers.add(FastOffer(SymbolicAction("getCounter", listOf(Variable("counterVal", intType))), __grounded, TSAction.SyncRole.Client))
            }
        }
        run {
            val __g = BoolExprFast.Eq(SyncTerm.Local("step"), SyncTerm.Ground(SyncGround.StringVal("respond")))
            val __grounded = SyncResolveFast.groundForOffer(__g, __locals)
            if (__grounded != null) {
                __offers.add(FastOffer(SymbolicAction("respond", listOf(), isInternal = true, channelKey = "inGetRPC_occ6#internal#respond"), __grounded, TSAction.SyncRole.Internal))
            }
        }
        return SyncStepPlan.FastOnly(__offers)
    }    
    override suspend fun transit(act: ConcreteAction) {    
        return when (act.symAction.name) {    
            "getCounter" -> {    
                val __transitRhs_0: String = "respond"
                val __transitRhs_1: Int = (act.lookup(Variable("counterVal", intType)).value as Int)
                step = __transitRhs_0
                result = __transitRhs_1
            }
            "respond" -> {    
                val __procFunRet: HttpServerResponse = HttpServerResponse(body = ("v=" + (result).toString()), code = 200)
                _procFunReturn = Value(__procFunRet, httpServerResponseType)
            }    
            else -> throw RuntimeException("Action is outside my alphabet: ${act.symAction}")    
        }
    }
}

class inAddRPC(    
    private val program: Program
) : TransitionSystem {
    private lateinit var hostProc: Proc
    private var sessionPeer: Proc? = null
    private var _procFunReturn: Value? = null
    override fun bindHostProc(host: Proc) { hostProc = host }
    override fun setSessionPeer(peer: Proc?) { sessionPeer = peer }
    override fun consumeProcFunReturn(): Value? {
        val v = _procFunReturn
        _procFunReturn = null
        return v
    }
    private var _req: HttpServerRequest? = null
    private var req: HttpServerRequest
        get() = _req ?: throw JulayException("State variable \"req\" read before it was initialized")
        set(value) { _req = value }
    private var _deltaVal: Int? = null
    private var deltaVal: Int
        get() = _deltaVal ?: throw JulayException("State variable \"deltaVal\" read before it was initialized")
        set(value) { _deltaVal = value }
    private var _step: String? = null
    private var step: String
        get() = _step ?: throw JulayException("State variable \"step\" read before it was initialized")
        set(value) { _step = value }
    private var _result: Int? = null
    private var result: Int
        get() = _result ?: throw JulayException("State variable \"result\" read before it was initialized")
        set(value) { _result = value }
    private var _retVal: HttpServerResponse? = null
    private var retVal: HttpServerResponse
        get() = _retVal ?: throw JulayException("State variable \"retVal\" read before it was initialized")
        set(value) { _retVal = value }    
    override suspend fun finishConstruction(act: ConcreteAction) {    
        when (act.symAction.name) {    
            "inAddRPC_call" -> {    
                val __transitRhs_0: HttpServerRequest = (act.lookup(Variable("req", httpServerRequestType)).value as HttpServerRequest)
                req = __transitRhs_0
                val __transitRhs_1: Int = ((act.lookup(Variable("req", httpServerRequestType)).value as HttpServerRequest).body).split("|")[(1) - 1].split("=")[(2) - 1].toInt()
                deltaVal = __transitRhs_1
                val __transitRhs_2: String = "call"
                step = __transitRhs_2
                val __transitRhs_3: Int = -1
                result = __transitRhs_3
            }    
            else -> {}    
        }
    }    
    override suspend fun actions(ctx: Context): Set<TSAction> = setOf(
        TSAction(    
            SymbolicAction("add", listOf(Variable("delta", intType), Variable("newVal", intType))),    
            ctx.mkAnd(ctx.mkEq(ctx.mkString(step),ctx.mkString("call")),ctx.mkEq(ctx.mkIntConst("delta"),ctx.mkInt(deltaVal))),    
            TSAction.SyncRole.Client    ,
            fastGuard = BoolExprFast.And(listOf(BoolExprFast.Eq(SyncTerm.Local("step"), SyncTerm.Ground(SyncGround.StringVal("call"))), BoolExprFast.Eq(SyncTerm.Arg("delta", SyncTerm.Arg.Sort.Int), SyncTerm.Local("deltaVal"))))
        ),
        TSAction(    
            SymbolicAction("respond", listOf(), isInternal = true, channelKey = "inAddRPC_occ5#internal#respond"),    
            ctx.mkEq(ctx.mkString(step),ctx.mkString("respond")),    
            TSAction.SyncRole.Internal    ,
            fastGuard = BoolExprFast.Eq(SyncTerm.Local("step"), SyncTerm.Ground(SyncGround.StringVal("respond")))
        )
    )    
    override fun syncStepPlan(): SyncStepPlan {
        val __locals: Map<String, Any?> = mapOf("deltaVal" to _deltaVal, "step" to _step, "result" to _result)
        val __offers = mutableListOf<FastOffer>()
        run {
            val __g = BoolExprFast.And(listOf(BoolExprFast.Eq(SyncTerm.Local("step"), SyncTerm.Ground(SyncGround.StringVal("call"))), BoolExprFast.Eq(SyncTerm.Arg("delta", SyncTerm.Arg.Sort.Int), SyncTerm.Local("deltaVal"))))
            val __grounded = SyncResolveFast.groundForOffer(__g, __locals)
            if (__grounded != null) {
                __offers.add(FastOffer(SymbolicAction("add", listOf(Variable("delta", intType), Variable("newVal", intType))), __grounded, TSAction.SyncRole.Client))
            }
        }
        run {
            val __g = BoolExprFast.Eq(SyncTerm.Local("step"), SyncTerm.Ground(SyncGround.StringVal("respond")))
            val __grounded = SyncResolveFast.groundForOffer(__g, __locals)
            if (__grounded != null) {
                __offers.add(FastOffer(SymbolicAction("respond", listOf(), isInternal = true, channelKey = "inAddRPC_occ5#internal#respond"), __grounded, TSAction.SyncRole.Internal))
            }
        }
        return SyncStepPlan.FastOnly(__offers)
    }    
    override suspend fun transit(act: ConcreteAction) {    
        return when (act.symAction.name) {    
            "add" -> {    
                val __transitRhs_0: String = "respond"
                val __transitRhs_1: Int = (act.lookup(Variable("newVal", intType)).value as Int)
                step = __transitRhs_0
                result = __transitRhs_1
            }
            "respond" -> {    
                val __procFunRet: HttpServerResponse = HttpServerResponse(body = ("v=" + (result).toString()), code = 200)
                _procFunReturn = Value(__procFunRet, httpServerResponseType)
            }    
            else -> throw RuntimeException("Action is outside my alphabet: ${act.symAction}")    
        }
    }
}

suspend fun main(args : Array<String>) {    
    val tsInfo = setOf(
        TransitionSystemStaticInfo(    
            "Protocol",
            setOf(
                SymbolicAction("increment", listOf(Variable("newVal", intType))),
                SymbolicAction("add", listOf(Variable("delta", intType), Variable("newVal", intType))),
                SymbolicAction("getCounter", listOf(Variable("counterVal", intType)))
            ),
            mapOf<SymbolicAction, suspend (Program, ConcreteAction) -> TransitionSystem>(
                Pair(SymbolicAction("initially", listOf(Variable("args", listType_String))), { program, _ -> Protocol(program) })
            )
        ),
        TransitionSystemStaticInfo(    
            "ServerInitializer",
            setOf(
                SymbolicAction("listen", listOf(Variable("port", intType), Variable("handler", stringType)), isSession = true, channelKey = "RpcIn_1#listen"),
                SymbolicAction("close", listOf(), isSession = true, channelKey = "RpcIn_1#close")
            ),
            mapOf<SymbolicAction, suspend (Program, ConcreteAction) -> TransitionSystem>(
                Pair(SymbolicAction("initially", listOf(Variable("args", listType_String))), { program, _ -> ServerInitializer(program) })
            )
        ),
        JulHttpServer.staticInfo().withChannelKeys(mapOf("listen" to "RpcIn_1#listen", "close" to "RpcIn_1#close"))
    )    
    val procFunInfo = setOf(
        TransitionSystemStaticInfo(    
            "handleRpc",
            setOf(
                SymbolicAction("route", listOf(), isInternal = true, channelKey = "handleRpc_occ4#internal#route"),
                SymbolicAction("increment", listOf(Variable("newVal", intType))),
                SymbolicAction("respond", listOf(), isInternal = true, channelKey = "inIncrementRPC_occ7#internal#respond"),
                SymbolicAction("getCounter", listOf(Variable("counterVal", intType))),
                SymbolicAction("respond", listOf(), isInternal = true, channelKey = "inGetRPC_occ6#internal#respond"),
                SymbolicAction("add", listOf(Variable("delta", intType), Variable("newVal", intType))),
                SymbolicAction("respond", listOf(), isInternal = true, channelKey = "inAddRPC_occ5#internal#respond")
            ),
            mapOf<SymbolicAction, suspend (Program, ConcreteAction) -> TransitionSystem>(
                Pair(SymbolicAction("handleRpc_call", listOf(Variable("req", httpServerRequestType))), { program, _ -> handleRpc(program) })
            )
        ),
        TransitionSystemStaticInfo(    
            "inIncrementRPC",
            setOf(
                SymbolicAction("increment", listOf(Variable("newVal", intType))),
                SymbolicAction("respond", listOf(), isInternal = true, channelKey = "inIncrementRPC_occ7#internal#respond")
            ),
            mapOf<SymbolicAction, suspend (Program, ConcreteAction) -> TransitionSystem>(
                Pair(SymbolicAction("inIncrementRPC_call", listOf(Variable("req", httpServerRequestType))), { program, _ -> inIncrementRPC(program) })
            )
        ),
        TransitionSystemStaticInfo(    
            "inGetRPC",
            setOf(
                SymbolicAction("getCounter", listOf(Variable("counterVal", intType))),
                SymbolicAction("respond", listOf(), isInternal = true, channelKey = "inGetRPC_occ6#internal#respond")
            ),
            mapOf<SymbolicAction, suspend (Program, ConcreteAction) -> TransitionSystem>(
                Pair(SymbolicAction("inGetRPC_call", listOf(Variable("req", httpServerRequestType))), { program, _ -> inGetRPC(program) })
            )
        ),
        TransitionSystemStaticInfo(    
            "inAddRPC",
            setOf(
                SymbolicAction("add", listOf(Variable("delta", intType), Variable("newVal", intType))),
                SymbolicAction("respond", listOf(), isInternal = true, channelKey = "inAddRPC_occ5#internal#respond")
            ),
            mapOf<SymbolicAction, suspend (Program, ConcreteAction) -> TransitionSystem>(
                Pair(SymbolicAction("inAddRPC_call", listOf(Variable("req", httpServerRequestType))), { program, _ -> inAddRPC(program) })
            )
        )
    )    
    Program(tsInfo, args.toList(), procFunInfo, SyncResolveConfig(eqUnify=true, argOwnership=true, directedEval=true)).run()
}