package julay.regression

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object JulCompiler {
    fun compile(
        projectRoot: File,
        workspace: File,
        sourceJul: File,
        timeoutMs: Long = RegressionTimeouts.CASE_MS,
        compileArgs: List<String> = emptyList(),
    ): CompileResult {
        val compilerJar = resolveCompilerJar(projectRoot)
        val dest = File(workspace, "julayc.jar")
        Files.copy(compilerJar.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING)

        val cmd = mutableListOf("java", "-jar", dest.absolutePath)
        cmd.addAll(compileArgs)
        cmd.add(sourceJul.absolutePath)
        val proc = ProcessBuilder(cmd)
            .directory(workspace)
            .redirectErrorStream(true)
            .start()

        val output = readProcessOutput(proc, timeoutMs, TimeUnit.MILLISECONDS)

        val jars = workspace.listFiles { f ->
            f.isFile && f.extension == "jar" && f.name != "julayc.jar"
        }?.associateBy { it.nameWithoutExtension }
            ?: emptyMap()

        return CompileResult(output, jars)
    }

    private fun resolveCompilerJar(projectRoot: File): File {
        val libs = File(projectRoot, "build/libs/julayc.jar")
        if (libs.exists()) return libs
        val root = File(projectRoot, "julayc.jar")
        check(root.exists()) {
            "julayc.jar not found. Run ./gradlew shadowJar first."
        }
        return root
    }

    private fun readProcessOutput(proc: Process, timeout: Long, unit: TimeUnit): String {
        val output = StringBuilder()
        val reader = Executors.newSingleThreadExecutor()
        val readFuture = reader.submit {
            proc.inputStream.bufferedReader().use { br ->
                val buf = CharArray(4096)
                var n: Int
                while (br.read(buf).also { n = it } != -1) {
                    output.append(buf, 0, n)
                }
            }
        }
        val finished = proc.waitFor(timeout, unit)
        if (!finished) {
            proc.destroyForcibly()
            proc.waitFor(5, TimeUnit.SECONDS)
            readFuture.get(2, TimeUnit.SECONDS)
            reader.shutdownNow()
            val timeoutLabel = if (unit == TimeUnit.MILLISECONDS) "${timeout}ms" else "${timeout} ${unit.name.lowercase()}"
            throw AssertionError(
                "Compiler timed out after $timeoutLabel\n--- partial output ---\n$output",
            )
        }
        readFuture.get(30, TimeUnit.SECONDS)
        reader.shutdown()
        return output.toString()
    }
}
