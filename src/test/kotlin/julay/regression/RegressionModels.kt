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
    /** Compile must produce at least one native program binary (compile-only success). */
    val expectBinaries: Boolean = false,
    /** Extra args passed to `julayc` before the source path (e.g. `--disable-opt`). */
    val compileArgs: List<String> = emptyList(),
    /**
     * Legacy flag from the Kotlin/JAR era. Ignored: Go codegen has no opt-parity recompile path.
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
    /** Native binaries keyed by program / compile-target name. */
    val binaries: Map<String, File>,
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
