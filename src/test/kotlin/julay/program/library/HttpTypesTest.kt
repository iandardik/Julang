package julay.program.library

import com.microsoft.z3.Context
import com.microsoft.z3.Status
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HttpTypesTest {
    private fun emptyModel(ctx: Context) = run {
        val solver = ctx.mkSolver()
        assertEquals(Status.SATISFIABLE, solver.check())
        solver.model
    }

    @Test
    fun roundTripHttpServerRequest() {
        val ctx = Context()
        val original = HttpServerRequest("x/y/z", "hello")
        val restored = httpServerRequestType.fromZ3Expr(
            httpServerRequestToZ3(ctx, original),
            emptyModel(ctx),
        ) as HttpServerRequest
        assertEquals(original, restored)
    }

    @Test
    fun roundTripHttpServerResponse() {
        val ctx = Context()
        val original = HttpServerResponse("ok", 200)
        val restored = httpServerResponseType.fromZ3Expr(
            httpServerResponseToZ3(ctx, original),
            emptyModel(ctx),
        ) as HttpServerResponse
        assertEquals(original, restored)
    }

    @Test
    fun roundTripHttpClientRequest() {
        val ctx = Context()
        val original = HttpClientRequest("http://localhost:8000", "POST", "body")
        val restored = httpClientRequestType.fromZ3Expr(
            httpClientRequestToZ3(ctx, original),
            emptyModel(ctx),
        ) as HttpClientRequest
        assertEquals(original, restored)
    }

    @Test
    fun roundTripHttpClientResponse() {
        val ctx = Context()
        val original = HttpClientResponse("resp", 201)
        val restored = httpClientResponseType.fromZ3Expr(
            httpClientResponseToZ3(ctx, original),
            emptyModel(ctx),
        ) as HttpClientResponse
        assertEquals(original, restored)
    }

    @Test
    fun httpPathFromUriPathStripsLeadingSlash() {
        assertEquals("x/y/z", httpPathFromUriPath("/x/y/z"))
        assertEquals("", httpPathFromUriPath("/"))
        assertEquals("a", httpPathFromUriPath("a"))
    }

    @Test
    fun isOfTypeRecognizesDataClasses() {
        assertTrue(httpServerRequestType.isOfType(HttpServerRequest("", "")))
        assertTrue(httpServerResponseType.isOfType(HttpServerResponse("", 0)))
        assertTrue(httpClientRequestType.isOfType(HttpClientRequest("", "", "")))
        assertTrue(httpClientResponseType.isOfType(HttpClientResponse("", 0)))
    }
}
