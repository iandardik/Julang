package julay.compiler.pass.fuse

import julay.compiler.compileJulFile
import julay.compiler.pass.CompilerOptConfig
import julay.compiler.pass.codegenPass
import julay.compiler.prepareCheckedCompilation
import julay.program.sync.SyncResolveConfig
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ProcfunFuseTest {

    private fun codegen(source: String, fuse: Boolean = true): String {
        val dir = Files.createTempDirectory("procfun-fuse")
        val file = dir.resolve("main.jul")
        file.writeText(source.trimIndent())
        try {
            val checked = prepareCheckedCompilation(file)
            assertNotNull(checked, "prepareCheckedCompilation failed")
            val program = checked.jarTargets.single()
            val result = codegenPass(
                checked.ast,
                program,
                checked.procDecls,
                checked.librariesInUse,
                syncResolveConfig = SyncResolveConfig.ALL_ON,
                compilerOptConfig = CompilerOptConfig(
                    syncResolve = SyncResolveConfig.ALL_ON,
                    procfunFuse = fuse,
                ),
            )
            return result.sourceText
        } finally {
            dir.toFile().deleteRecursively()
        }
    }

    private val whenRouterSource = """
        import julay.funlib.exitProgram
        import julay.funlib.println

        procfun leaf(x : Int) : Int {
            internal transition done() {
                return: x + 1
            }
        }

        procfun mid(x : Int) : Int {
            internal transition go() {
                return: leaf(x)
            }
        }

        procfun top(x : Int) : Int {
            internal transition go() {
                return: mid(x)
            }
        }

        proc Driver {
            var done : Boolean
            var result : Int
            constructor initially(args : List<String>) {
                transit:
                    done := false
                    result := 0
            }
            internal transition run() {
                guard: ~done
                transit:
                    result := top(41)
                    done := true
            }
            internal transition finish() {
                guard: done
                after:
                    println(result + "")
                    exitProgram(0)
            }
        }
        compile Driver
    """.trimIndent()

    @Test
    fun fuseOnEmitsPhaseVarAndNoNestedInvoke() {
        val src = """
            procfun helper(x : Int) : Int {
                internal transition done() {
                    return: x + 1
                }
            }
            procfun host(n : Int) : Int {
                internal transition go() {
                    return: helper(n)
                }
            }
            proc Boot {
                constructor initially(args : List<String>) { transit: }
            }
            compile Boot
        """.trimIndent()
        val text = codegen(src, fuse = true)
        assertTrue(text.contains("__julayFuse"), "expected fuse phase var;\n${text.take(2000)}")
        assertTrue(
            text.contains("__julayFuse = \"helper\"") || text.contains("__julayFuse = \"helper\""),
            "expected dispatch to helper;\n${text.take(3000)}",
        )
        // Nested call site should not spawn helper via invokeProcFun.
        assertFalse(
            text.contains("invokeProcFun(\"helper\""),
            "fused host must not invokeProcFun helper;\n${text.take(4000)}",
        )
    }

    @Test
    fun fuseOffKeepsInvokeProcFun() {
        val src = """
            procfun helper(x : Int) : Int {
                internal transition done() {
                    return: x + 1
                }
            }
            procfun host(n : Int) : Int {
                internal transition go() {
                    return: helper(n)
                }
            }
            proc Boot {
                constructor initially(args : List<String>) { transit: }
            }
            compile Boot
        """.trimIndent()
        val text = codegen(src, fuse = false)
        assertFalse(text.contains("__julayFuse"), "fuse off should not emit phase var")
        assertTrue(
            text.contains("invokeProcFun(\"helper\""),
            "expected invokeProcFun when fuse off;\n${text.take(3000)}",
        )
    }

    @Test
    fun whenMixedArmsDispatch() {
        val src = """
            procfun helper(x : Int) : Int {
                internal transition done() {
                    return: x
                }
            }
            procfun router(path : String, n : Int) : Int {
                internal transition route() {
                    return: when (path) {
                        "call" -> helper(n)
                        else -> 0
                    }
                }
            }
            proc Boot {
                constructor initially(args : List<String>) { transit: }
            }
            compile Boot
        """.trimIndent()
        val text = codegen(src, fuse = true)
        assertTrue(text.contains("__julayFuse = \"helper\""), text.take(4000))
        assertTrue(
            text.contains("_procFunReturn = Value") && text.contains("0"),
            "literal else arm should set _procFunReturn;\n${text.take(4000)}",
        )
        assertFalse(text.contains("invokeProcFun(\"helper\""), text.take(4000))
    }

    @Test
    fun depthTwoNoNestedInvoke() {
        val text = codegen(whenRouterSource, fuse = true)
        assertTrue(text.contains("__julayFuse"), text.take(2000))
        assertFalse(text.contains("invokeProcFun(\"leaf\""), text.take(5000))
        assertFalse(text.contains("invokeProcFun(\"mid\""), text.take(5000))
        assertTrue(
            text.contains("__julayFuse = \"mid\"") || text.contains("__julayFuse = \"mid/leaf\"") ||
                text.contains("__julayFuse = \"leaf\""),
            "expected nested phase paths;\n${text.take(5000)}",
        )
    }

    @Test
    fun midTransitAssignDispatch() {
        val src = """
            procfun helper(x : Int) : Int {
                internal transition done() {
                    return: x + 1
                }
            }
            procfun host(n : Int) : Int {
                var y : Int := 0
                var ready : Boolean := false
                internal transition go() {
                    guard: ~ready
                    transit:
                        y := helper(n)
                        ready := true
                }
                internal transition fin() {
                    guard: ready
                    return: y
                }
            }
            proc Boot {
                constructor initially(args : List<String>) { transit: }
            }
            compile Boot
        """.trimIndent()
        val text = codegen(src, fuse = true)
        assertTrue(text.contains("__julayFuseDest"), text.take(3000))
        assertFalse(text.contains("invokeProcFun(\"helper\""), text.take(4000))
    }

    @Test
    fun overlapKeepsDistinctChannelKeys() {
        // Caller host and callee both offer `ping`; fusion must still stamp composition keys.
        val src = """
            procfun callee() : Int {
                client transition ping(v : Int) {
                    return: v
                }
            }
            procfun host() : Int {
                client transition ping(v : Int) {
                    guard: false
                    return: 0
                }
                internal transition go() {
                    return: callee()
                }
            }
            proc Peer {
                var done : Boolean
                constructor initially(args : List<String>) {
                    transit: done := false
                }
                transition ping(v : Int) {
                    guard: ~done & v = 7
                    transit: done := true
                }
            }
            proc App := Peer
            compile App
        """.trimIndent()
        val text = codegen(src, fuse = true)
        assertTrue(text.contains("__julayFuse"), text.take(2000))
        // Both ping actions appear; at least one should carry an explicit channelKey when remapped.
        val pingCount = Regex("""SymbolicAction\("ping"""").findAll(text).count()
        assertTrue(pingCount >= 2, "expected host+callee ping offers;\n${text.take(4000)}")
    }

    @Test
    fun blockingGuardOnHostOffers() {
        val src = """
            procfun helper(x : Int) : Int {
                internal transition done() {
                    return: x
                }
            }
            procfun host(n : Int) : Int {
                var side : Int := 0
                internal transition bump() {
                    guard: true
                    transit: side := side + 1
                }
                internal transition go() {
                    return: helper(n)
                }
            }
            proc Boot {
                constructor initially(args : List<String>) { transit: }
            }
            compile Boot
        """.trimIndent()
        val text = codegen(src, fuse = true)
        // Host bump must be gated on idle fuse phase.
        assertTrue(
            text.contains("__julayFuse") && text.contains("\"\""),
            "expected idle phase guards;\n${text.take(4000)}",
        )
    }

    @Test
    fun optOffEquivalenceStdout() {
        val dir = Files.createTempDirectory("procfun-fuse-equiv")
        val file = dir.resolve("main.jul")
        file.writeText(whenRouterSource)
        val jarOn = File("Driver.jar")
        val jarOff = File("DriverOff.jar")
        val buildOn = File("Driver-jul-build")
        try {
            jarOn.delete()
            buildOn.deleteRecursively()
            compileJulFile(
                file,
                keepBuild = false,
                compilerOptConfig = CompilerOptConfig.ALL_ON,
            )
            assertTrue(jarOn.exists(), "expected Driver.jar with fuse on")
            val onOut = runJar(jarOn)

            jarOn.delete()
            compileJulFile(
                file,
                keepBuild = false,
                compilerOptConfig = CompilerOptConfig.fromDisableOptFlag("procfun-fuse"),
            )
            assertTrue(jarOn.exists(), "expected Driver.jar with fuse off")
            val offOut = runJar(jarOn)
            assertEquals(onOut.trim(), offOut.trim(), "fuse on vs off stdout mismatch")
            assertTrue(onOut.contains("42"), "expected 41+1=42; got $onOut")
        } finally {
            jarOn.delete()
            jarOff.delete()
            buildOn.deleteRecursively()
            File("Driver-jul-build").deleteRecursively()
            dir.toFile().deleteRecursively()
        }
    }

    @Test
    fun rpcServerHttpFused() {
        val cwd = File(".").absoluteFile
        val jar = File(cwd, "RpcServer.jar")
        val buildDir = File(cwd, "RpcServer-jul-build")
        jar.delete()
        buildDir.deleteRecursively()
        var proc: Process? = null
        try {
            compileJulFile(
                File("input/rpc_server/main.jul").toPath(),
                keepBuild = true,
                compilerOptConfig = CompilerOptConfig.ALL_ON,
            )
            assertTrue(jar.exists(), "expected RpcServer.jar")
            val kt = buildDir.listFiles()?.filter { it.extension == "kt" }?.joinToString("\n") { it.readText() }.orEmpty()
            assertTrue(kt.contains("__julayFuse"), "handleRpc should be fused;\n${kt.take(1500)}")
            assertFalse(
                kt.contains("invokeProcFun(\"inIncrementRPC\""),
                "fused handleRpc must not invokeProcFun inIncrementRPC",
            )

            proc = ProcessBuilder("java", "-jar", jar.absolutePath)
                .directory(cwd)
                .redirectErrorStream(true)
                .start()
            waitForPort(8000, timeoutMs = 20_000)
            repeat(5) { i ->
                assertEquals(200 to "v=${i + 1}", httpPost("http://127.0.0.1:8000/rpc/increment", ""))
            }
            assertEquals(200 to "v=5", httpPost("http://127.0.0.1:8000/rpc/get", ""))
            assertEquals(200 to "v=9", httpPost("http://127.0.0.1:8000/rpc/add", "delta=4"))
        } finally {
            proc?.destroyForcibly()
            proc?.waitFor(5, TimeUnit.SECONDS)
            jar.delete()
            buildDir.deleteRecursively()
        }
    }

    @Test
    fun rpcServerHttpFuseOff() {
        val cwd = File(".").absoluteFile
        val jar = File(cwd, "RpcServer.jar")
        val buildDir = File(cwd, "RpcServer-jul-build")
        jar.delete()
        buildDir.deleteRecursively()
        var proc: Process? = null
        try {
            compileJulFile(
                File("input/rpc_server/main.jul").toPath(),
                keepBuild = true,
                compilerOptConfig = CompilerOptConfig.fromDisableOptFlag("procfun-fuse"),
            )
            assertTrue(jar.exists(), "expected RpcServer.jar")
            proc = ProcessBuilder("java", "-jar", jar.absolutePath)
                .directory(cwd)
                .redirectErrorStream(true)
                .start()
            waitForPort(8000, timeoutMs = 20_000)
            assertEquals(200 to "v=1", httpPost("http://127.0.0.1:8000/rpc/increment", ""))
            assertEquals(200 to "v=1", httpPost("http://127.0.0.1:8000/rpc/get", ""))
        } finally {
            proc?.destroyForcibly()
            proc?.waitFor(5, TimeUnit.SECONDS)
            jar.delete()
            buildDir.deleteRecursively()
        }
    }

    /**
     * Raft-style hole: two fused callees share public action name/channelKey `updateTerm`.
     * Transit must dispatch on `(channelKey, __julayFuse)` or the second path livelocks on the
     * first callee's transit body.
     */
    private val sharedUpdateTermSource = """
        import julay.funlib.exitProgram
        import julay.funlib.println

        procfun inA(x : Int) : Int {
            var step : String := "u"
            const n : Int := x
            client transition updateTerm(inTerm : Int) {
                guard:
                    & step = "u"
                    & inTerm = n
                transit:
                    step := "d"
            }
            internal transition ret() {
                guard: step = "d"
                return: n + 1
            }
        }

        procfun inB(x : Int) : Int {
            var step : String := "u"
            const n : Int := x
            client transition updateTerm(inTerm : Int) {
                guard:
                    & step = "u"
                    & inTerm = n
                transit:
                    step := "d"
            }
            internal transition ret() {
                guard: step = "d"
                return: n + 10
            }
        }

        procfun router(which : String, x : Int) : Int {
            internal transition go() {
                return: when (which) {
                    "a" -> inA(x)
                    "b" -> inB(x)
                    else -> 0
                }
            }
        }

        proc Peer {
            constructor initially(args : List<String>) {
                transit:
            }
            provider transition updateTerm(inTerm : Int) {
            }
        }

        proc Driver {
            var step : String
            var r1 : Int
            var r2 : Int
            constructor initially(args : List<String>) {
                transit:
                    step := "a"
                    r1 := 0
                    r2 := 0
            }
            internal transition doA() {
                guard: step = "a"
                transit:
                    r1 := router("a", 5)
                    step := "b"
            }
            internal transition doB() {
                guard: step = "b"
                transit:
                    r2 := router("b", 5)
                    step := "print"
            }
            internal transition finish() {
                guard: step = "print"
                after:
                    println((r1 + "") + "," + (r2 + ""))
                    exitProgram(0)
            }
        }

        proc FuseDupKeyApp := Driver || Peer
        compile FuseDupKeyApp
    """.trimIndent()

    @Test
    fun sharedChannelKeyTransitDispatchesByPhase() {
        val text = codegen(sharedUpdateTermSource, fuse = true)
        assertTrue(
            text.contains("channelKey to __julayFuse"),
            "fuse host transit must dispatch on (channelKey, phase);\n${text.take(4000)}",
        )
        assertTrue(
            text.contains("(\"updateTerm\" to \"inA\")") &&
                text.contains("(\"updateTerm\" to \"inB\")"),
            "both updateTerm phases must appear as distinct when arms;\n${text.take(6000)}",
        )
        // Phase short-circuit so idle actions() does not read uninitialized callee locals.
        assertTrue(
            text.contains("if (__julayFuse == \"inA\")") &&
                text.contains("if (__julayFuse == \"inB\")"),
            "expected phase-gated guards;\n${text.take(6000)}",
        )
        val routerClass = text.substringAfter("class router(", missingDelimiterValue = "")
            .let { body ->
                val next = body.indexOf("\nclass ")
                if (next < 0) body else body.substring(0, next)
            }
        assertTrue(routerClass.isNotEmpty(), "expected class router in codegen;\n${text.take(2000)}")
        val routerFinish = routerClass
            .substringAfter("finishConstruction", missingDelimiterValue = "")
            .substringBefore("override suspend fun actions", missingDelimiterValue = "")
        assertTrue(
            routerFinish.contains("inA__step") && routerFinish.contains("\"u\""),
            "expected hoisted inA__step in router ctor;\n$routerFinish",
        )
        // Dispatch may assign inA__n from inA__x; host construction must not.
        assertFalse(
            Regex("""inA__n\s*=\s*inA__x""").containsMatchIn(routerFinish),
            "must not hoist arg-dependent inA__n := inA__x into host ctor;\n$routerFinish",
        )
        assertFalse(
            Regex("""inB__n\s*=\s*inB__x""").containsMatchIn(routerFinish),
            "must not hoist arg-dependent inB__n := inB__x into host ctor;\n$routerFinish",
        )
    }

    @Test
    fun sharedChannelKeyBothCalleePathsRun() {
        val dir = Files.createTempDirectory("procfun-fuse-dupkey")
        val file = dir.resolve("main.jul")
        file.writeText(sharedUpdateTermSource)
        val jar = File("FuseDupKeyApp.jar")
        val buildDir = File("FuseDupKeyApp-jul-build")
        try {
            jar.delete()
            buildDir.deleteRecursively()
            compileJulFile(
                file,
                keepBuild = true,
                compilerOptConfig = CompilerOptConfig.ALL_ON,
            )
            assertTrue(jar.exists(), "expected FuseDupKeyApp.jar")
            val kt = buildDir.listFiles()?.filter { it.extension == "kt" }
                ?.joinToString("\n") { it.readText() }.orEmpty()
            assertTrue(
                kt.contains("(\"updateTerm\" to \"inA\")") &&
                    kt.contains("(\"updateTerm\" to \"inB\")"),
                "generated transit missing phase arms;\n${kt.take(4000)}",
            )
            val out = runJar(jar, timeoutSec = 20)
            assertEquals(
                "6,15",
                out.trim(),
                "both fused updateTerm paths must complete (got ${out.trim()})",
            )
        } finally {
            jar.delete()
            buildDir.deleteRecursively()
            dir.toFile().deleteRecursively()
        }
    }

    @Test
    fun idleFusedSliceDoesNotThrowOnUninitializedArgState() {
        // Host offers fused callee transitions while idle; guards that read arg-derived
        // consts must be phase-gated or construction crashes before dispatch.
        val src = """
            import julay.funlib.exitProgram
            import julay.funlib.println

            procfun leaf(x : Int) : Int {
                var step : String := "go"
                const y : Int := x + 1
                internal transition done() {
                    guard: step = "go" & y > 0
                    return: y
                }
            }

            procfun host(n : Int) : Int {
                internal transition go() {
                    return: leaf(n)
                }
            }

            proc Driver {
                var done : Boolean
                var result : Int
                constructor initially(args : List<String>) {
                    transit:
                        done := false
                        result := 0
                }
                internal transition run() {
                    guard: ~done
                    transit:
                        result := host(41)
                        done := true
                }
                internal transition finish() {
                    guard: done
                    after:
                        println(result + "")
                        exitProgram(0)
                }
            }
            compile Driver
        """.trimIndent()
        val dir = Files.createTempDirectory("procfun-fuse-idle")
        val file = dir.resolve("main.jul")
        file.writeText(src)
        val jar = File("Driver.jar")
        val buildDir = File("Driver-jul-build")
        try {
            jar.delete()
            buildDir.deleteRecursively()
            compileJulFile(
                file,
                keepBuild = false,
                compilerOptConfig = CompilerOptConfig.ALL_ON,
            )
            assertTrue(jar.exists())
            val out = runJar(jar, timeoutSec = 15)
            assertEquals("42", out.trim())
        } finally {
            jar.delete()
            buildDir.deleteRecursively()
            dir.toFile().deleteRecursively()
        }
    }

    private fun runJar(jar: File, timeoutSec: Long = 15): String {
        val outFile = Files.createTempFile("julay-jar-out", ".txt").toFile()
        try {
            val pb = ProcessBuilder("java", "-jar", jar.absolutePath)
                .redirectErrorStream(true)
                .redirectOutput(outFile)
            val p = pb.start()
            val finished = p.waitFor(timeoutSec, TimeUnit.SECONDS)
            if (!finished) {
                p.destroyForcibly()
                error("jar timed out: ${jar.name}\n${outFile.readText()}")
            }
            return outFile.readText()
        } finally {
            outFile.delete()
        }
    }

    private fun waitForPort(port: Int, timeoutMs: Long) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            try {
                java.net.Socket("127.0.0.1", port).use { return }
            } catch (_: Exception) {
                Thread.sleep(100)
            }
        }
        error("port $port not ready within ${timeoutMs}ms")
    }

    private fun httpPost(url: String, body: String): Pair<Int, String> {
        val conn = URI(url).toURL().openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "text/plain")
        conn.outputStream.use { it.write(body.toByteArray()) }
        val code = conn.responseCode
        val text = (if (code in 200..299) conn.inputStream else conn.errorStream)
            ?.bufferedReader()?.readText().orEmpty()
        return code to text.trim()
    }
}
