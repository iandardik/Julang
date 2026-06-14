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
