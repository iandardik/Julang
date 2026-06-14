package julay.regression

import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object JulProcess {
    data class Running(
        val process: Process,
        val stdout: StringBuilder,
        val reader: Thread,
    ) {
        fun collect(millis: Long): String {
            Thread.sleep(millis)
            reader.join(500)
            return stdout.toString()
        }

        fun destroy() {
            process.destroyForcibly()
            process.waitFor(5, TimeUnit.SECONDS)
            reader.join(2000)
        }
    }

    fun startBackground(jar: File, workspace: File, stdin: List<String>): Running {
        val pb = ProcessBuilder("java", "-jar", jar.absolutePath)
            .directory(workspace)
            .redirectErrorStream(true)
        val process = pb.start()
        writeStdin(process, stdin)
        val stdout = StringBuilder()
        val reader = Thread {
            process.inputStream.bufferedReader().use { br ->
                val buf = CharArray(4096)
                var n: Int
                while (br.read(buf).also { n = it } != -1) {
                    stdout.append(buf, 0, n)
                }
            }
        }
        reader.isDaemon = true
        reader.start()
        return Running(process, stdout, reader)
    }

    fun runToCompletion(jar: File, workspace: File, run: RunConfig, timeoutMs: Long = run.timeoutMs): ProcessResult {
        val pb = ProcessBuilder("java", "-jar", jar.absolutePath)
            .directory(workspace)
            .redirectErrorStream(true)
        val process = pb.start()
        writeStdin(process, run.stdin)
        val output = readProcessOutput(process, timeoutMs)
        return ProcessResult(output, "", process.exitValue())
    }

    private fun readProcessOutput(process: Process, timeoutMs: Long): String {
        val output = StringBuilder()
        val reader = Executors.newSingleThreadExecutor()
        val readFuture = reader.submit {
            process.inputStream.bufferedReader().use { br ->
                val buf = CharArray(4096)
                var n: Int
                while (br.read(buf).also { n = it } != -1) {
                    output.append(buf, 0, n)
                }
            }
        }
        val finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
        if (!finished) {
            process.destroyForcibly()
            process.waitFor(5, TimeUnit.SECONDS)
            readFuture.get(2, TimeUnit.SECONDS)
            reader.shutdownNow()
            throw AssertionError(
                "Program timed out after ${timeoutMs}ms\n--- partial stdout ---\n$output",
            )
        }
        readFuture.get(5, TimeUnit.SECONDS)
        reader.shutdown()
        return output.toString()
    }

    private fun writeStdin(process: Process, stdin: List<String>) {
        if (stdin.isEmpty()) return
        Thread {
            process.outputStream.bufferedWriter().use { w ->
                stdin.forEach { line ->
                    w.write(line)
                    if (!line.endsWith("\n")) w.newLine()
                    w.flush()
                }
            }
        }.start()
    }
}
