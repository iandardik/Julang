package julay.regression

import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.FileInputStream

object RegressionCases {
    fun loadAll(projectRoot: File): List<CaseFile> {
        val dir = File(projectRoot, "regression/cases")
        require(dir.isDirectory) { "Missing regression/cases directory: ${dir.absolutePath}" }
        return dir.walkTopDown()
            .filter { it.isFile && (it.extension == "yaml" || it.extension == "yml") }
            .sortedBy { dir.toPath().relativize(it.toPath()).toString() }
            .map { loadCase(projectRoot, dir, it) }
            .toList()
    }

    private fun caseId(casesDir: File, file: File): String {
        val relative = casesDir.toPath().relativize(file.toPath()).toString()
        return relative.removeSuffix(".yaml").removeSuffix(".yml")
    }

    private fun loadCase(projectRoot: File, casesDir: File, file: File): CaseFile {
        @Suppress("UNCHECKED_CAST")
        val root = Yaml().load<Map<String, Any>>(FileInputStream(file))
        val source = root["source"] as String
        val tags = (root["tags"] as? List<*>)?.map { it.toString() } ?: emptyList()
        val expectCompileFailure = root["expectCompileFailure"] as? Boolean ?: false
        val disabled = root["disabled"] as? Boolean ?: false
        val expectCompileOutputContains =
            (root["expectCompileOutputContains"] as? List<*>)?.map { it.toString() } ?: emptyList()
        @Suppress("UNCHECKED_CAST")
        val programsRaw = root["programs"] as? List<Map<String, Any>> ?: emptyList()
        val programs = programsRaw.map { parseProgram(it) }
        if (!disabled) {
            check(expectCompileFailure || programs.isNotEmpty()) {
                "Case ${file.name} must set expectCompileFailure: true or declare at least one program"
            }
        }
        return CaseFile(
            caseId(casesDir, file), source, tags, disabled,
            expectCompileFailure, expectCompileOutputContains, programs,
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseProgram(map: Map<String, Any>): ProgramCase {
        val name = map["name"] as String
        val dependsOn = map["dependsOn"] as? String
        val expectFailure = map["expectFailure"] as? Boolean ?: false
        val expectFailureOutputContains =
            (map["expectFailureOutputContains"] as? List<*>)?.map { it.toString() } ?: emptyList()
        val runMap = map["run"] as? Map<String, Any>
        val run = runMap?.let { parseRun(it) }
        if (!expectFailure) {
            check(run != null) { "Program $name must have a run block unless expectFailure: true" }
        }
        return ProgramCase(name, dependsOn, expectFailure, expectFailureOutputContains, run)
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseRun(map: Map<String, Any>): RunConfig {
        val timeoutMs = ((map["timeoutMs"] as? Number)?.toLong() ?: RegressionTimeouts.CASE_MS)
            .coerceAtMost(RegressionTimeouts.CASE_MS)
        val durationMs = (map["durationMs"] as? Number)?.toLong()
            ?.coerceAtMost(RegressionTimeouts.CASE_MS)
        val stdin = (map["stdin"] as? List<*>)?.map { it.toString() } ?: emptyList()
        val background = map["background"] as? Boolean ?: false
        val expectStdout = map["expectStdout"] as? String
        val expectStdoutContains = (map["expectStdoutContains"] as? List<*>)?.map { it.toString() } ?: emptyList()
        val expectStdoutLinesUnordered = (map["expectStdoutLinesUnordered"] as? List<*>)?.map { it.toString() } ?: emptyList()
        val expectStdoutMatches = map["expectStdoutMatches"] as? String
        val http = (map["http"] as? List<Map<String, Any>>)?.map { m ->
            HttpCheck(m["post"] as String, m["expectBody"] as String)
        } ?: emptyList()
        return RunConfig(
            timeoutMs, durationMs, stdin, background, expectStdout,
            expectStdoutContains, expectStdoutLinesUnordered, expectStdoutMatches, http,
        )
    }
}
