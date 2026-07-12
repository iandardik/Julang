package julay.program.library

import com.microsoft.z3.Context
import com.microsoft.z3.Expr
import com.microsoft.z3.IntExpr
import com.microsoft.z3.Model
import com.microsoft.z3.SeqExpr
import julay.program.ObjClassType
import julay.program.Variable
import julay.program.intType
import julay.program.stringType

data class HttpServerRequest(val path: String, val body: String)

data class HttpServerResponse(val body: String, val code: Int)

data class HttpClientRequest(val url: String, val method: String, val body: String)

data class HttpClientResponse(val body: String, val code: Int)

fun httpServerRequestMk(ctx: Context, path: Expr<*>, body: Expr<*>): Expr<*> =
    httpServerRequestType.constructorDecl(ctx).apply(path, body) as Expr<*>

fun httpServerRequestPath(ctx: Context, record: Expr<*>): SeqExpr<*> =
    httpServerRequestType.accessor(ctx, 0).apply(record) as SeqExpr<*>

fun httpServerRequestBody(ctx: Context, record: Expr<*>): SeqExpr<*> =
    httpServerRequestType.accessor(ctx, 1).apply(record) as SeqExpr<*>

fun httpServerRequestToZ3(ctx: Context, value: HttpServerRequest): Expr<*> =
    httpServerRequestMk(ctx, ctx.mkString(value.path), ctx.mkString(value.body))

fun httpServerRequestFromZ3(expr: Expr<*>, model: Model): HttpServerRequest {
    val fieldExprs = if (expr.isApp && expr.funcDecl.name == httpServerRequestType.homeConstructorDecl().name) {
        expr.args
    } else {
        arrayOf(
            httpServerRequestType.homeAccessor(0).apply(expr) as Expr<*>,
            httpServerRequestType.homeAccessor(1).apply(expr) as Expr<*>,
        )
    }
    return HttpServerRequest(
        stringType.fromZ3Expr(fieldExprs[0], model) as String,
        stringType.fromZ3Expr(fieldExprs[1], model) as String,
    )
}

val httpServerRequestType = ObjClassType(
    "HttpServerRequest",
    listOf(
        Variable("path", stringType),
        Variable("body", stringType),
    ),
    { value, ctx -> httpServerRequestToZ3(ctx, value.value as HttpServerRequest) },
    { expr, model -> httpServerRequestFromZ3(expr, model) },
)

fun httpServerResponseMk(ctx: Context, body: Expr<*>, code: Expr<*>): Expr<*> =
    httpServerResponseType.constructorDecl(ctx).apply(body, code) as Expr<*>

fun httpServerResponseBody(ctx: Context, record: Expr<*>): SeqExpr<*> =
    httpServerResponseType.accessor(ctx, 0).apply(record) as SeqExpr<*>

fun httpServerResponseCode(ctx: Context, record: Expr<*>): IntExpr =
    httpServerResponseType.accessor(ctx, 1).apply(record) as IntExpr

fun httpServerResponseToZ3(ctx: Context, value: HttpServerResponse): Expr<*> =
    httpServerResponseMk(ctx, ctx.mkString(value.body), ctx.mkInt(value.code))

fun httpServerResponseFromZ3(expr: Expr<*>, model: Model): HttpServerResponse {
    val fieldExprs = if (expr.isApp && expr.funcDecl.name == httpServerResponseType.homeConstructorDecl().name) {
        expr.args
    } else {
        arrayOf(
            httpServerResponseType.homeAccessor(0).apply(expr) as Expr<*>,
            httpServerResponseType.homeAccessor(1).apply(expr) as Expr<*>,
        )
    }
    return HttpServerResponse(
        stringType.fromZ3Expr(fieldExprs[0], model) as String,
        intType.fromZ3Expr(fieldExprs[1], model) as Int,
    )
}

val httpServerResponseType = ObjClassType(
    "HttpServerResponse",
    listOf(
        Variable("body", stringType),
        Variable("code", intType),
    ),
    { value, ctx -> httpServerResponseToZ3(ctx, value.value as HttpServerResponse) },
    { expr, model -> httpServerResponseFromZ3(expr, model) },
)

fun httpClientRequestMk(ctx: Context, url: Expr<*>, method: Expr<*>, body: Expr<*>): Expr<*> =
    httpClientRequestType.constructorDecl(ctx).apply(url, method, body) as Expr<*>

fun httpClientRequestUrl(ctx: Context, record: Expr<*>): SeqExpr<*> =
    httpClientRequestType.accessor(ctx, 0).apply(record) as SeqExpr<*>

fun httpClientRequestMethod(ctx: Context, record: Expr<*>): SeqExpr<*> =
    httpClientRequestType.accessor(ctx, 1).apply(record) as SeqExpr<*>

fun httpClientRequestBody(ctx: Context, record: Expr<*>): SeqExpr<*> =
    httpClientRequestType.accessor(ctx, 2).apply(record) as SeqExpr<*>

fun httpClientRequestToZ3(ctx: Context, value: HttpClientRequest): Expr<*> =
    httpClientRequestMk(
        ctx,
        ctx.mkString(value.url),
        ctx.mkString(value.method),
        ctx.mkString(value.body),
    )

fun httpClientRequestFromZ3(expr: Expr<*>, model: Model): HttpClientRequest {
    val fieldExprs = if (expr.isApp && expr.funcDecl.name == httpClientRequestType.homeConstructorDecl().name) {
        expr.args
    } else {
        arrayOf(
            httpClientRequestType.homeAccessor(0).apply(expr) as Expr<*>,
            httpClientRequestType.homeAccessor(1).apply(expr) as Expr<*>,
            httpClientRequestType.homeAccessor(2).apply(expr) as Expr<*>,
        )
    }
    return HttpClientRequest(
        stringType.fromZ3Expr(fieldExprs[0], model) as String,
        stringType.fromZ3Expr(fieldExprs[1], model) as String,
        stringType.fromZ3Expr(fieldExprs[2], model) as String,
    )
}

val httpClientRequestType = ObjClassType(
    "HttpClientRequest",
    listOf(
        Variable("url", stringType),
        Variable("method", stringType),
        Variable("body", stringType),
    ),
    { value, ctx -> httpClientRequestToZ3(ctx, value.value as HttpClientRequest) },
    { expr, model -> httpClientRequestFromZ3(expr, model) },
)

fun httpClientResponseMk(ctx: Context, body: Expr<*>, code: Expr<*>): Expr<*> =
    httpClientResponseType.constructorDecl(ctx).apply(body, code) as Expr<*>

fun httpClientResponseBody(ctx: Context, record: Expr<*>): SeqExpr<*> =
    httpClientResponseType.accessor(ctx, 0).apply(record) as SeqExpr<*>

fun httpClientResponseCode(ctx: Context, record: Expr<*>): IntExpr =
    httpClientResponseType.accessor(ctx, 1).apply(record) as IntExpr

fun httpClientResponseToZ3(ctx: Context, value: HttpClientResponse): Expr<*> =
    httpClientResponseMk(ctx, ctx.mkString(value.body), ctx.mkInt(value.code))

fun httpClientResponseFromZ3(expr: Expr<*>, model: Model): HttpClientResponse {
    val fieldExprs = if (expr.isApp && expr.funcDecl.name == httpClientResponseType.homeConstructorDecl().name) {
        expr.args
    } else {
        arrayOf(
            httpClientResponseType.homeAccessor(0).apply(expr) as Expr<*>,
            httpClientResponseType.homeAccessor(1).apply(expr) as Expr<*>,
        )
    }
    return HttpClientResponse(
        stringType.fromZ3Expr(fieldExprs[0], model) as String,
        intType.fromZ3Expr(fieldExprs[1], model) as Int,
    )
}

val httpClientResponseType = ObjClassType(
    "HttpClientResponse",
    listOf(
        Variable("body", stringType),
        Variable("code", intType),
    ),
    { value, ctx -> httpClientResponseToZ3(ctx, value.value as HttpClientResponse) },
    { expr, model -> httpClientResponseFromZ3(expr, model) },
)

/** Strip a leading `/` so `http://a.com/x/y/z` yields path `x/y/z`. */
fun httpPathFromUriPath(uriPath: String): String =
    uriPath.removePrefix("/")
