package julay.compiler

import julay.compiler.ast.RootNode
import julay.compiler.decl.ProcDecl
import julay.compiler.pass.CompilerOptConfig
import julay.compiler.pass.goCodegenPass
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

private const val GO_BUILD_TIMEOUT_MINUTES = 2L
private const val GORUNTIME_RESOURCE_PREFIX = "goruntime/julay/"
private val GORUNTIME_FILES = listOf(
    "collections.go",
    "runtime.go",
    "http.go",
    "timer.go",
)

/**
 * No-op retained for the regression harness. Go builds do not need a Gradle daemon warmup.
 */
fun warmGradleForProgramCompile(
    @Suppress("UNUSED_PARAMETER") workDir: File,
    @Suppress("UNUSED_PARAMETER") compilerJar: java.nio.file.Path = resolveCompilerJar(),
) {
    // Intentionally empty: program compile now uses `go build`.
}

fun compileProgram(
    program: ProcDecl,
    ast: RootNode,
    procDecls: List<ProcDecl>,
    librariesInUse: Set<String> = emptySet(),
    keepBuild: Boolean = false,
    @Suppress("UNUSED_PARAMETER") compilerJar: java.nio.file.Path = resolveCompilerJar(),
    @Suppress("UNUSED_PARAMETER") compilerOptConfig: CompilerOptConfig = CompilerOptConfig.ALL_ON,
) {
    val buildDir = File("${program.name}-jul-build")
    if (!buildDir.exists() && !buildDir.mkdir()) {
        println("Could not create ${buildDir.name} dir")
        exitProcess(1)
    }

    // Clear previous generated sources
    buildDir.listFiles()?.forEach { f ->
        when {
            f.isFile && (f.name == "main.go" || f.name == "go.mod" || f.name == "go.sum") -> f.delete()
            f.isDirectory && f.name == "julay" -> deleteDirectory(f)
        }
    }

    val codegen = goCodegenPass(ast, program, procDecls, librariesInUse)

    File(buildDir, "go.mod").writeText(
        "module ${codegen.moduleName}\n\ngo 1.21\n",
    )

    val julayDir = File(buildDir, "julay")
    if (!julayDir.exists() && !julayDir.mkdirs()) {
        println("Could not create ${julayDir.path}")
        exitProcess(1)
    }
    copyGoRuntime(julayDir)

    File(buildDir, "main.go").writeText(codegen.mainGo)

    val binaryName = codegen.binaryName
    val outPath = File(buildDir.parentFile ?: File("."), binaryName).absolutePath
    val goResult = runShellCommand(
        "cd ${buildDir.absolutePath} && go mod tidy && go build -o ${shellQuote(outPath)} . 2>&1",
        GO_BUILD_TIMEOUT_MINUTES,
    )
    if (goResult.timedOut) {
        println(
            "Internal compiler error: timed out while building program \"${program.name}\" " +
                "(after ${GO_BUILD_TIMEOUT_MINUTES}m).",
        )
        exitProcess(1)
    }
    if (goResult.exitCode != 0) {
        // Never surface generated Go paths/diagnostics to users — that is a codegen bug.
        println(
            "Internal compiler error: failed to build program \"${program.name}\". " +
                "This is a compiler bug; please report it.",
        )
        if (keepBuild) {
            System.err.println(goResult.output)
        }
        exitProcess(1)
    }
    if (!keepBuild) {
        deleteDirectory(buildDir)
    }
}

private fun copyGoRuntime(destDir: File) {
    val loader = Julayc::class.java.classLoader
    for (name in GORUNTIME_FILES) {
        val resource = GORUNTIME_RESOURCE_PREFIX + name
        val stream = loader.getResourceAsStream(resource)
            ?: error("Missing classpath resource: $resource")
        val dest = File(destDir, name)
        stream.use { input ->
            Files.copy(input, dest.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }
}

private fun shellQuote(path: String): String =
    "'" + path.replace("'", "'\\''") + "'"

private data class ShellResult(val exitCode: Int, val output: String, val timedOut: Boolean)

private fun runShellCommand(command: String, timeoutMinutes: Long): ShellResult {
    val proc = Runtime.getRuntime().exec(arrayOf("bash", "-c", command))
    val output = StringBuilder()
    val reader = Thread {
        proc.inputStream.bufferedReader().use { br ->
            val buf = CharArray(4096)
            var n: Int
            while (br.read(buf).also { n = it } != -1) {
                output.append(buf, 0, n)
            }
        }
    }
    reader.start()
    val finished = proc.waitFor(timeoutMinutes, TimeUnit.MINUTES)
    if (!finished) {
        proc.destroyForcibly()
        proc.waitFor(5, TimeUnit.SECONDS)
        reader.join(2000)
        return ShellResult(-1, output.toString(), timedOut = true)
    }
    reader.join(30_000)
    return ShellResult(proc.exitValue(), output.toString(), timedOut = false)
}

private fun deleteDirectory(directory: File) {
    if (directory.exists() && directory.isDirectory) {
        directory.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                deleteDirectory(file)
            } else {
                file.delete()
            }
        }
        directory.delete()
    }
}
