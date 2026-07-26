package julay.regression

import julay.compiler.warmGradleForProgramCompile
import java.io.File
import java.nio.file.Files
import kotlin.test.assertTrue

object RegressionRunner {
    private val httpLock = Any()

    fun runAll(projectRoot: File) {
        val caseFilter = System.getenv("REGRESSION_CASE")?.trim()?.takeIf { it.isNotEmpty() }
        val cases = RegressionCases.loadAll(projectRoot).filter { case ->
            caseFilter == null || case.id == caseFilter || case.id.endsWith("/$caseFilter")
        }
        check(cases.isNotEmpty()) {
            "No regression cases matched REGRESSION_CASE=${caseFilter ?: "(unset)"}"
        }
        warmGradle(projectRoot)
        cases.forEach { case ->
            if (case.disabled) {
                println("Regression case: ${case.id} (skipped, disabled)")
                return@forEach
            }
            println("Regression case: ${case.id}")
            val block = {
                try {
                    runCase(projectRoot, case)
                    println("  passed: ${case.id}")
                } catch (e: Throwable) {
                    System.err.println("  FAILED: ${case.id}")
                    throw AssertionError("Regression case failed: ${case.id} (${case.source})", e)
                }
            }
            if ("http" in case.tags) {
                synchronized(httpLock) { block() }
            } else {
                block()
            }
        }
    }

    /** Prefetch Gradle distro/plugins/daemon so case compiles fit in [RegressionTimeouts.CASE_MS]. */
    private fun warmGradle(projectRoot: File) {
        val compilerJar = File(projectRoot, "build/libs/julayc.jar").takeIf { it.exists() }
            ?: File(projectRoot, "julayc.jar")
        check(compilerJar.exists()) {
            "julayc.jar not found for Gradle warmup. Run ./gradlew shadowJar first."
        }
        val warmupDir = File(projectRoot, "build/regression-gradle-warmup")
        println("Warming Gradle (distro + Kotlin/Shadow plugins)…")
        warmGradleForProgramCompile(warmupDir, compilerJar.toPath())
        println("  Gradle warmup done")
    }

    fun runCase(projectRoot: File, case: CaseFile) {
        val deadlineMs = System.currentTimeMillis() + RegressionTimeouts.CASE_MS
        val workspaceParent = File(projectRoot, "build/regression-workspace")
        workspaceParent.mkdirs()
        val workspace = Files.createTempDirectory(
            workspaceParent.toPath(),
            "${case.id.replace('/', '-')}-",
        ).toFile()
        var background: JulProcess.Running? = null
        try {
            val source = File(projectRoot, case.source)
            check(source.exists()) { "Missing source file: ${case.source}" }
            RegressionTimeouts.requireRemaining(case.id, deadlineMs)
            val compileResult = JulCompiler.compile(
                projectRoot,
                workspace,
                source,
                timeoutMs = RegressionTimeouts.capMs(RegressionTimeouts.CASE_MS, deadlineMs),
            )

            if (case.expectCompileFailure) {
                assertCompileFailure(case, compileResult)
                return
            }

            case.expectCompileOutputContains.forEach { needle ->
                assertTrue(
                    compileResult.output.contains(needle),
                    "Compiler output for case ${case.id} missing expected substring: $needle\n--- output ---\n${compileResult.output}",
                )
            }

            if (case.programs.isEmpty()) {
                if (case.expectJars) {
                    check(compileResult.jars.isNotEmpty()) {
                        "Expected JARs for case ${case.id} but none were produced\n--- compiler output ---\n${compileResult.output}"
                    }
                    assertTrue(
                        !compileResult.output.contains("never called by a peer"),
                        "Unexpected orphan error for case ${case.id}\n--- output ---\n${compileResult.output}",
                    )
                } else {
                    check(compileResult.jars.isNotEmpty() || case.expectCompileOutputContains.isNotEmpty()) {
                        "No program JARs produced for ${case.source}\n--- compiler output ---\n${compileResult.output}"
                    }
                }
                return
            }

            check(compileResult.jars.isNotEmpty()) {
                "No program JARs produced for ${case.source}\n--- compiler output ---\n${compileResult.output}"
            }

            stageWorkspaceFiles(projectRoot, workspace, case)

            case.programs.forEach { program ->
                RegressionTimeouts.requireRemaining(case.id, deadlineMs)
                background = runProgram(projectRoot, case, workspace, compileResult.jars, program, background, deadlineMs)
            }
        } finally {
            background?.destroy()
            workspace.deleteRecursively()
        }
    }

    /** Copy CLI arg paths that exist under the project root into the isolated workspace. */
    private fun stageWorkspaceFiles(projectRoot: File, workspace: File, case: CaseFile) {
        case.programs
            .flatMap { it.run?.args ?: emptyList() }
            .distinct()
            .forEach { arg ->
                val src = File(projectRoot, arg)
                if (!src.isFile) return@forEach
                val dest = File(workspace, arg)
                dest.parentFile.mkdirs()
                Files.copy(src.toPath(), dest.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING)
            }
    }

    private fun assertCompileFailure(case: CaseFile, compileResult: CompileResult) {
        assertTrue(
            compileResult.jars.isEmpty(),
            "Expected compile failure for case ${case.id} but JARs were produced: ${compileResult.jars.keys}",
        )
        case.expectCompileOutputContains.forEach { needle ->
            assertTrue(
                compileResult.output.contains(needle),
                "Compiler output for case ${case.id} missing expected substring: $needle\n--- output ---\n${compileResult.output}",
            )
        }
    }

    private fun cappedRun(run: RunConfig, deadlineMs: Long): RunConfig {
        val remaining = RegressionTimeouts.remainingMs(deadlineMs)
        check(remaining > 0) { "No time remaining for program run" }
        return run.copy(
            timeoutMs = minOf(run.timeoutMs, remaining),
            durationMs = run.durationMs?.let { minOf(it, remaining) },
        )
    }

    private fun runProgram(
        projectRoot: File,
        case: CaseFile,
        workspace: File,
        jars: Map<String, File>,
        program: ProgramCase,
        background: JulProcess.Running?,
        deadlineMs: Long,
    ): JulProcess.Running? {
        val jar = jars[program.name]
            ?: throw AssertionError("No JAR for program ${program.name} in case ${case.id}; have ${jars.keys}")
        val run = program.run
            ?: throw AssertionError("Program ${program.name} in case ${case.id} has no run config")
        val capped = cappedRun(run, deadlineMs)

        var activeBackground = background
        try {
            if (program.dependsOn != null) {
                val depJar = jars[program.dependsOn]
                    ?: throw AssertionError("dependsOn ${program.dependsOn} not found for ${program.name}")
                activeBackground?.destroy()
                activeBackground = JulProcess.startBackground(depJar, workspace, emptyList())
                HttpProbe.waitForPort(portWaitMs(deadlineMs))
            }

            val stdout = when {
                capped.http.isNotEmpty() -> {
                    activeBackground?.destroy()
                    activeBackground = JulProcess.startBackground(jar, workspace, capped.stdin, capped.args)
                    HttpProbe.waitForPort(portWaitMs(deadlineMs))
                    capped.http.forEach { HttpProbe.postExpectBody(it.post, it.expectBody) }
                    val captureMs = capped.durationMs ?: 3_000L
                    activeBackground!!.collect(RegressionTimeouts.capMs(captureMs, deadlineMs))
                }
                capped.background -> {
                    activeBackground?.destroy()
                    activeBackground = JulProcess.startBackground(jar, workspace, capped.stdin, capped.args)
                    if (capped.http.isNotEmpty() || case.tags.contains("http")) {
                        HttpProbe.waitForPort(portWaitMs(deadlineMs))
                    }
                    val captureMs = capped.durationMs ?: capped.timeoutMs
                    activeBackground!!.collect(RegressionTimeouts.capMs(captureMs, deadlineMs))
                }
                else -> {
                    val result = JulProcess.runToCompletion(jar, workspace, capped, capped.timeoutMs)
                    if (result.exitCode != 0) {
                        throw AssertionError(
                            "Program exited with code ${result.exitCode}\n--- stdout ---\n${result.stdout}",
                        )
                    }
                    result.stdout
                }
            }

            if (program.expectFailure) {
                throw AssertionError(
                    "Expected program ${program.name} in case ${case.id} to fail but it completed successfully",
                )
            }
            StdoutMatcher.assertMatches(projectRoot, stdout, capped)
        } catch (e: AssertionError) {
            if (!program.expectFailure) throw e
            val message = e.message ?: ""
            program.expectFailureOutputContains.forEach { needle ->
                assertTrue(
                    message.contains(needle),
                    "Failure output for ${program.name} in case ${case.id} missing expected substring: $needle\n--- message ---\n$message",
                )
            }
        }
        return activeBackground
    }

    private fun portWaitMs(deadlineMs: Long): Long =
        RegressionTimeouts.capMs(10_000L, deadlineMs)
}
