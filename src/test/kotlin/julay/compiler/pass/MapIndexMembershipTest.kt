package julay.compiler.pass

import julay.compiler.prepareCheckedCompilation
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MapIndexMembershipTest {
    @Test
    fun unguardedReadRejected() {
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
        assertNull(compileSnippet(src), "unguarded mp[k] must fail")
    }

    @Test
    fun conjunctMembershipAllowsRead() {
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
        assertNotNull(compileSnippet(src), "k in mp & mp[k] must be allowed")
    }

    @Test
    fun implicationAntecedentAllowsRead() {
        val src = """
            import julay.funlib.mapOf
            type N
            static proc Box {
                var mp : Map<String, Int> := mapOf()
                constructor initially(args : List<String>) {}
            }
            invariant Inv := forall n : N, forall k : String,
                (k in Box[n].mp) => (Box[n].mp[k] = 0)
            spec S := Box[n : N] { N := {"a"} } |= Inv
            compile S
        """.trimIndent()
        assertNotNull(compileSnippet(src), "(k in mp) => mp[k] must be allowed")
    }

    @Test
    fun ifMembershipAllowsRead() {
        val src = """
            import julay.funlib.mapOf
            static proc Box {
                var mp : Map<String, Int> := mapOf()
                constructor initially(args : List<String>) {}
                internal transition ok(k : String, v : Int) {
                    guard: v = if (k in mp) { mp[k] } else { 0 }
                }
            }
            proc T := Box
            compile T
        """.trimIndent()
        assertNotNull(compileSnippet(src), "if (k in mp) { mp[k] } must be allowed")
    }

    @Test
    fun guardMembershipAppliesToTransit() {
        val src = """
            import julay.funlib.mapOf
            static proc Box {
                var mp : Map<String, Int> := mapOf()
                var n : Int := 0
                constructor initially(args : List<String>) {}
                internal transition ok() {
                    guard: "a" in mp
                    transit: n := mp["a"]
                }
            }
            proc T := Box
            compile T
        """.trimIndent()
        assertNotNull(compileSnippet(src), "guard k in mp should cover transit mp[k]")
    }

    @Test
    fun keysMapAllowsRead() {
        val src = """
            import julay.funlib.mapOf
            import julay.funlib.listOf
            static proc Box {
                var mp : Map<String, Int> := mapOf()
                var xs : List<Int> := listOf()
                constructor initially(args : List<String>) {}
                internal transition collect() {
                    guard: true
                    transit:
                        xs := mp.keys.toList().map(k -> mp[k])
                }
            }
            proc T := Box
            compile T
        """.trimIndent()
        val checked = compileSnippet(src)
        assertNotNull(checked, "mp.keys…map(k -> mp[k]) must be allowed")
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
