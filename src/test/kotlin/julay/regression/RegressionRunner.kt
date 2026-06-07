package julay.regression

import java.io.File
import java.nio.file.Files

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
            "${case.id}-",
        ).toFile()
        var background: JulProcess.Running? = null
        try {
            val source = File(projectRoot, case.source)
            check(source.exists()) { "Missing source file: ${case.source}" }
            val jars = JulCompiler.compile(projectRoot, workspace, source)

            case.programs.forEach { program ->
                val jar = jars[program.name]
                    ?: throw AssertionError("No JAR for program ${program.name} in case ${case.id}; have ${jars.keys}")
                val run = program.run
                    ?: throw AssertionError("Program ${program.name} in case ${case.id} has no run config")

                if (program.dependsOn != null) {
                    val depJar = jars[program.dependsOn]
                        ?: throw AssertionError("dependsOn ${program.dependsOn} not found for ${program.name}")
                    background?.destroy()
                    background = JulProcess.startBackground(depJar, workspace, emptyList())
                    HttpProbe.waitForPort()
                }

                val stdout = when {
                    run.http.isNotEmpty() -> {
                        background?.destroy()
                        background = JulProcess.startBackground(jar, workspace, run.stdin)
                        HttpProbe.waitForPort()
                        run.http.forEach { HttpProbe.postExpectBody(it.post, it.expectBody) }
                        val captureMs = run.durationMs ?: 3_000L
                        background!!.collect(captureMs)
                    }
                    run.background -> {
                        background?.destroy()
                        background = JulProcess.startBackground(jar, workspace, run.stdin)
                        if (run.http.isNotEmpty() || case.tags.contains("http")) {
                            HttpProbe.waitForPort()
                        }
                        val captureMs = run.durationMs ?: run.timeoutMs
                        background!!.collect(captureMs)
                    }
                    else -> {
                        val result = JulProcess.runToCompletion(jar, workspace, run)
                        result.stdout
                    }
                }

                StdoutMatcher.assertMatches(projectRoot, stdout, run)
            }
        } finally {
            background?.destroy()
            workspace.deleteRecursively()
        }
    }
}
