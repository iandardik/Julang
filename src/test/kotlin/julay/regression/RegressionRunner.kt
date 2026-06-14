package julay.regression

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

            check(compileResult.jars.isNotEmpty()) {
                "No program JARs produced for ${case.source}\n--- compiler output ---\n${compileResult.output}"
            }

            case.programs.forEach { program ->
                RegressionTimeouts.requireRemaining(case.id, deadlineMs)
                background = runProgram(projectRoot, case, workspace, compileResult.jars, program, background, deadlineMs)
            }
        } finally {
            background?.destroy()
            workspace.deleteRecursively()
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
                    activeBackground = JulProcess.startBackground(jar, workspace, capped.stdin)
                    HttpProbe.waitForPort(portWaitMs(deadlineMs))
                    capped.http.forEach { HttpProbe.postExpectBody(it.post, it.expectBody) }
                    val captureMs = capped.durationMs ?: 3_000L
                    activeBackground!!.collect(RegressionTimeouts.capMs(captureMs, deadlineMs))
                }
                capped.background -> {
                    activeBackground?.destroy()
                    activeBackground = JulProcess.startBackground(jar, workspace, capped.stdin)
                    if (capped.http.isNotEmpty() || case.tags.contains("http")) {
                        HttpProbe.waitForPort(portWaitMs(deadlineMs))
                    }
                    val captureMs = capped.durationMs ?: capped.timeoutMs
                    activeBackground!!.collect(RegressionTimeouts.capMs(captureMs, deadlineMs))
                }
                else -> {
                    val result = JulProcess.runToCompletion(jar, workspace, capped, capped.timeoutMs)
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
