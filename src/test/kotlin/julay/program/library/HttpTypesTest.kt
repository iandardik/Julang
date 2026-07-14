package julay.program.library

import io.github.cvc5.TermManager
import julay.tools.isSat
import julay.tools.newModelSolver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HttpTypesTest {
    private fun emptySolver(tm: TermManager) = run {
        val solver = newModelSolver(tm)
        assertTrue(solver.isSat())
        solver
    }

    @Test
    fun roundTripHttpServerRequest() {
        val tm = TermManager()
        val original = HttpServerRequest("x/y/z", "hello")
        val restored = httpServerRequestType.fromSmtTerm(
            httpServerRequestToSmt(tm, original),
            emptySolver(tm),
        ) as HttpServerRequest
        assertEquals(original, restored)
    }

    @Test
    fun roundTripHttpServerResponse() {
        val tm = TermManager()
        val original = HttpServerResponse("ok", 200)
        val restored = httpServerResponseType.fromSmtTerm(
            httpServerResponseToSmt(tm, original),
            emptySolver(tm),
        ) as HttpServerResponse
        assertEquals(original, restored)
    }

    @Test
    fun roundTripHttpClientRequest() {
        val tm = TermManager()
        val original = HttpClientRequest("http://localhost:8000", "POST", "body")
        val restored = httpClientRequestType.fromSmtTerm(
            httpClientRequestToSmt(tm, original),
            emptySolver(tm),
        ) as HttpClientRequest
        assertEquals(original, restored)
    }

    @Test
    fun roundTripHttpClientResponse() {
        val tm = TermManager()
        val original = HttpClientResponse("resp", 201)
        val restored = httpClientResponseType.fromSmtTerm(
            httpClientResponseToSmt(tm, original),
            emptySolver(tm),
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
