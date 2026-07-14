package julay.program.library

import io.github.cvc5.Kind
import io.github.cvc5.Solver
import io.github.cvc5.Term
import io.github.cvc5.TermManager
import julay.program.ObjClassType
import julay.program.Variable
import julay.program.intType
import julay.program.stringType
import julay.tools.applyConstructor
import julay.tools.applySelector
import julay.tools.mkKotlinString

data class HttpServerRequest(val path: String, val body: String)

data class HttpServerResponse(val body: String, val code: Int)

data class HttpClientRequest(val url: String, val method: String, val body: String)

data class HttpClientResponse(val body: String, val code: Int)

fun httpServerRequestMk(tm: TermManager, path: Term, body: Term): Term =
    applyConstructor(tm, httpServerRequestType.constructorTerm(tm), arrayOf(path, body))

fun httpServerRequestPath(tm: TermManager, record: Term): Term =
    applySelector(tm, httpServerRequestType.selector(tm, 0), record)

fun httpServerRequestBody(tm: TermManager, record: Term): Term =
    applySelector(tm, httpServerRequestType.selector(tm, 1), record)

fun httpServerRequestToSmt(tm: TermManager, value: HttpServerRequest): Term =
    httpServerRequestMk(tm, tm.mkKotlinString(value.path), tm.mkKotlinString(value.body))

fun httpServerRequestFromSmt(expr: Term, solver: Solver): HttpServerRequest {
    val valued = solver.getValue(expr)
    require(valued.kind == Kind.APPLY_CONSTRUCTOR && valued.numChildren >= 3) {
        "expected HttpServerRequest constructor term"
    }
    return HttpServerRequest(
        stringType.fromSmtTerm(valued.getChild(1), solver) as String,
        stringType.fromSmtTerm(valued.getChild(2), solver) as String,
    )
}

val httpServerRequestType = ObjClassType(
    "HttpServerRequest",
    listOf(
        Variable("path", stringType),
        Variable("body", stringType),
    ),
    { value, tm -> httpServerRequestToSmt(tm, value.value as HttpServerRequest) },
    { expr, solver -> httpServerRequestFromSmt(expr, solver) },
)

fun httpServerResponseMk(tm: TermManager, body: Term, code: Term): Term =
    applyConstructor(tm, httpServerResponseType.constructorTerm(tm), arrayOf(body, code))

fun httpServerResponseBody(tm: TermManager, record: Term): Term =
    applySelector(tm, httpServerResponseType.selector(tm, 0), record)

fun httpServerResponseCode(tm: TermManager, record: Term): Term =
    applySelector(tm, httpServerResponseType.selector(tm, 1), record)

fun httpServerResponseToSmt(tm: TermManager, value: HttpServerResponse): Term =
    httpServerResponseMk(tm, tm.mkKotlinString(value.body), tm.mkInteger(value.code.toLong()))

fun httpServerResponseFromSmt(expr: Term, solver: Solver): HttpServerResponse {
    val valued = solver.getValue(expr)
    require(valued.kind == Kind.APPLY_CONSTRUCTOR && valued.numChildren >= 3) {
        "expected HttpServerResponse constructor term"
    }
    return HttpServerResponse(
        stringType.fromSmtTerm(valued.getChild(1), solver) as String,
        intType.fromSmtTerm(valued.getChild(2), solver) as Int,
    )
}

val httpServerResponseType = ObjClassType(
    "HttpServerResponse",
    listOf(
        Variable("body", stringType),
        Variable("code", intType),
    ),
    { value, tm -> httpServerResponseToSmt(tm, value.value as HttpServerResponse) },
    { expr, solver -> httpServerResponseFromSmt(expr, solver) },
)

fun httpClientRequestMk(tm: TermManager, url: Term, method: Term, body: Term): Term =
    applyConstructor(tm, httpClientRequestType.constructorTerm(tm), arrayOf(url, method, body))

fun httpClientRequestUrl(tm: TermManager, record: Term): Term =
    applySelector(tm, httpClientRequestType.selector(tm, 0), record)

fun httpClientRequestMethod(tm: TermManager, record: Term): Term =
    applySelector(tm, httpClientRequestType.selector(tm, 1), record)

fun httpClientRequestBody(tm: TermManager, record: Term): Term =
    applySelector(tm, httpClientRequestType.selector(tm, 2), record)

fun httpClientRequestToSmt(tm: TermManager, value: HttpClientRequest): Term =
    httpClientRequestMk(
        tm,
        tm.mkKotlinString(value.url),
        tm.mkKotlinString(value.method),
        tm.mkKotlinString(value.body),
    )

fun httpClientRequestFromSmt(expr: Term, solver: Solver): HttpClientRequest {
    val valued = solver.getValue(expr)
    require(valued.kind == Kind.APPLY_CONSTRUCTOR && valued.numChildren >= 4) {
        "expected HttpClientRequest constructor term"
    }
    return HttpClientRequest(
        stringType.fromSmtTerm(valued.getChild(1), solver) as String,
        stringType.fromSmtTerm(valued.getChild(2), solver) as String,
        stringType.fromSmtTerm(valued.getChild(3), solver) as String,
    )
}

val httpClientRequestType = ObjClassType(
    "HttpClientRequest",
    listOf(
        Variable("url", stringType),
        Variable("method", stringType),
        Variable("body", stringType),
    ),
    { value, tm -> httpClientRequestToSmt(tm, value.value as HttpClientRequest) },
    { expr, solver -> httpClientRequestFromSmt(expr, solver) },
)

fun httpClientResponseMk(tm: TermManager, body: Term, code: Term): Term =
    applyConstructor(tm, httpClientResponseType.constructorTerm(tm), arrayOf(body, code))

fun httpClientResponseBody(tm: TermManager, record: Term): Term =
    applySelector(tm, httpClientResponseType.selector(tm, 0), record)

fun httpClientResponseCode(tm: TermManager, record: Term): Term =
    applySelector(tm, httpClientResponseType.selector(tm, 1), record)

fun httpClientResponseToSmt(tm: TermManager, value: HttpClientResponse): Term =
    httpClientResponseMk(tm, tm.mkKotlinString(value.body), tm.mkInteger(value.code.toLong()))

fun httpClientResponseFromSmt(expr: Term, solver: Solver): HttpClientResponse {
    val valued = solver.getValue(expr)
    require(valued.kind == Kind.APPLY_CONSTRUCTOR && valued.numChildren >= 3) {
        "expected HttpClientResponse constructor term"
    }
    return HttpClientResponse(
        stringType.fromSmtTerm(valued.getChild(1), solver) as String,
        intType.fromSmtTerm(valued.getChild(2), solver) as Int,
    )
}

val httpClientResponseType = ObjClassType(
    "HttpClientResponse",
    listOf(
        Variable("body", stringType),
        Variable("code", intType),
    ),
    { value, tm -> httpClientResponseToSmt(tm, value.value as HttpClientResponse) },
    { expr, solver -> httpClientResponseFromSmt(expr, solver) },
)

/** Strip a leading `/` so `http://a.com/x/y/z` yields path `x/y/z`. */
fun httpPathFromUriPath(uriPath: String): String =
    uriPath.removePrefix("/")
