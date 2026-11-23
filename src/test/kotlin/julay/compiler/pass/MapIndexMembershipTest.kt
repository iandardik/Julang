package julay.compiler.pass

import julay.compiler.prepareCheckedCompilation
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * Map reads are not compile-checked for membership; missing keys panic in Go /
 * domain-error in TLA+. These tests only assert unguarded forms type-check.
 */
class MapIndexMembershipTest {
    @Test
    fun unguardedReadCompiles() {
        val src = """
            import julay.funlib.mapOf
            static proc Box {
                var mp : Map<String, Int> := mapOf()
                constructor initially(args : List<String>) {}
                internal transition bad(k : String) {
                    guard: mp[k] = 0
                }
            }
            proc T := Box
            compile T
        """.trimIndent()
        assertNotNull(compileSnippet(src), "unguarded mp[k] must type-check")
    }

    @Test
    fun unguardedTransitReadCompiles() {
        val src = """
            import julay.funlib.mapOf
            static proc Box {
                var mp : Map<String, Int> := mapOf()
                var n : Int := 0
                constructor initially(args : List<String>) {}
                internal transition bad() {
                    guard: true
                    transit: n := mp["missing"]
                }
            }
            proc T := Box
            compile T
        """.trimIndent()
        assertNotNull(compileSnippet(src), "unguarded transit mp[k] must type-check")
    }

    @Test
    fun guardedFormsStillCompile() {
        val src = """
            import julay.funlib.mapOf
            static proc Box {
                var mp : Map<String, Int> := mapOf()
                constructor initially(args : List<String>) {}
                internal transition ok(k : String) {
                    guard: & k in mp & mp[k] = 0
                }
            }
            proc T := Box
            compile T
        """.trimIndent()
        assertNotNull(compileSnippet(src), "k in mp & mp[k] must still type-check")
    }

    private fun compileSnippet(source: String) =
        Files.createTempFile("map-memb-", ".jul").toFile().let { f ->
            f.writeText(source)
            try {
                prepareCheckedCompilation(f.toPath())
            } finally {
                f.delete()
            }
        }
}
