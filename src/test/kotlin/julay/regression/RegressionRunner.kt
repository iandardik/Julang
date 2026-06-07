package julay.regression

import java.io.File
import java.nio.file.Files
import kotlin.test.assertTrue

object RegressionRunner {
    private val httpLock = Any()

    fun runAll(projectRoot: File) {
        RegressionCases.loadAll(projectRoot).forEach { case ->
            val block = { runCase(projectRoot, case) }
            if ("http" in case.tags) {
                synchronized(httpLock) { block() }
            } else {
                block()
            }
        }
    }

    private fun runCase(projectRoot: File, case: CaseFile) {
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
            val compileResult = JulCompiler.compile(projectRoot, workspace, source)

            if (case.expectCompileFailure) {
                assertCompileFailure(case, compileResult)
                return
            }

            check(compileResult.jars.isNotEmpty()) {
                "No program JARs produced for ${case.source}\n--- compiler output ---\n${compileResult.output}"
            }

            case.programs.forEach { program ->
                background = runProgram(projectRoot, case, workspace, compileResult.jars, program, background)
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

    private fun runProgram(
        projectRoot: File,
        case: CaseFile,
        workspace: File,
        jars: Map<String, File>,
        program: ProgramCase,
        background: JulProcess.Running?,
    ): JulProcess.Running? {
        val jar = jars[program.name]
            ?: throw AssertionError("No JAR for program ${program.name} in case ${case.id}; have ${jars.keys}")
        val run = program.run
            ?: throw AssertionError("Program ${program.name} in case ${case.id} has no run config")

        var activeBackground = background
        try {
            if (program.dependsOn != null) {
                val depJar = jars[program.dependsOn]
                    ?: throw AssertionError("dependsOn ${program.dependsOn} not found for ${program.name}")
                activeBackground?.destroy()
                activeBackground = JulProcess.startBackground(depJar, workspace, emptyList())
                HttpProbe.waitForPort()
            }

            val stdout = when {
                run.http.isNotEmpty() -> {
                    activeBackground?.destroy()
                    activeBackground = JulProcess.startBackground(jar, workspace, run.stdin)
                    HttpProbe.waitForPort()
                    run.http.forEach { HttpProbe.postExpectBody(it.post, it.expectBody) }
                    val captureMs = run.durationMs ?: 3_000L
                    activeBackground!!.collect(captureMs)
                }
                run.background -> {
                    activeBackground?.destroy()
                    activeBackground = JulProcess.startBackground(jar, workspace, run.stdin)
                    if (run.http.isNotEmpty() || case.tags.contains("http")) {
                        HttpProbe.waitForPort()
                    }
                    val captureMs = run.durationMs ?: run.timeoutMs
                    activeBackground!!.collect(captureMs)
                }
                else -> {
                    val result = JulProcess.runToCompletion(jar, workspace, run)
                    result.stdout
                }
            }

            if (program.expectFailure) {
                throw AssertionError(
                    "Expected program ${program.name} in case ${case.id} to fail but it completed successfully",
                )
            }
            StdoutMatcher.assertMatches(projectRoot, stdout, run)
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
}
