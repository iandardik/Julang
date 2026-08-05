package julay.regression

import java.io.File

data class CaseFile(
    val id: String,
    val source: String,
    val tags: List<String>,
    val disabled: Boolean,
    val expectCompileFailure: Boolean,
    val expectCompileOutputContains: List<String>,
    val programs: List<ProgramCase>,
    /** Compile must produce at least one program JAR (compile-only success). */
    val expectJars: Boolean = false,
    /** Extra args passed to `julayc` before the source path (e.g. `--disable-opt`). */
    val compileArgs: List<String> = emptyList(),
    /**
     * When true, compile and run once with default opts and once with `--disable-opt`,
     * requiring identical program stdout for each program that declares expectStdout*.
     */
    val compileTwiceWithDisableOpt: Boolean = false,
)

data class ProgramCase(
    val name: String,
    val dependsOn: String?,
    val expectFailure: Boolean,
    val expectFailureOutputContains: List<String>,
    val run: RunConfig?,
)

data class CompileResult(
    val output: String,
    val jars: Map<String, File>,
)

data class RunConfig(
    val timeoutMs: Long,
    val durationMs: Long?,
    val stdin: List<String>,
    val args: List<String>,
    val background: Boolean,
    val expectStdout: String?,
    val expectStdoutContains: List<String>,
    val expectStdoutLinesUnordered: List<String>,
    val expectStdoutMatches: String?,
    val http: List<HttpCheck>,
)

data class HttpCheck(
    val post: String,
    val expectBody: String,
)

data class ProcessResult(
    val stdout: String,
    val stderr: String,
    val exitCode: Int,
)
