package julay.program.library

import julay.program.Variable
import julay.program.type.ObjClassType
import julay.program.type.intType
import julay.program.type.stringType

data class HttpServerRequest(val path: String, val body: String)

data class HttpServerResponse(val body: String, val code: Int)

data class HttpClientRequest(val url: String, val method: String, val body: String)

data class HttpClientResponse(val body: String, val code: Int)

val httpServerRequestType = ObjClassType(
    "HttpServerRequest",
    listOf(
        Variable("path", stringType),
        Variable("body", stringType),
    ),
)

val httpServerResponseType = ObjClassType(
    "HttpServerResponse",
    listOf(
        Variable("body", stringType),
        Variable("code", intType),
    ),
)

val httpClientRequestType = ObjClassType(
    "HttpClientRequest",
    listOf(
        Variable("url", stringType),
        Variable("method", stringType),
        Variable("body", stringType),
    ),
)

val httpClientResponseType = ObjClassType(
    "HttpClientResponse",
    listOf(
        Variable("body", stringType),
        Variable("code", intType),
    ),
)

/** Strip a leading `/` so `http://a.com/x/y/z` yields path `x/y/z`. */
fun httpPathFromUriPath(uriPath: String): String =
    uriPath.removePrefix("/")
