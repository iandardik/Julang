package julay.regression

import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.FileInputStream

object RegressionCases {
    fun loadAll(projectRoot: File): List<CaseFile> {
        val dir = File(projectRoot, "regression/cases")
        require(dir.isDirectory) { "Missing regression/cases directory: ${dir.absolutePath}" }
        return dir.listFiles { f -> f.extension == "yaml" || f.extension == "yml" }
            ?.sortedBy { it.name }
            ?.map { loadCase(projectRoot, it) }
            ?: emptyList()
    }

    private fun loadCase(projectRoot: File, file: File): CaseFile {
        @Suppress("UNCHECKED_CAST")
        val root = Yaml().load<Map<String, Any>>(FileInputStream(file))
        val source = root["source"] as String
        val tags = (root["tags"] as? List<*>)?.map { it.toString() } ?: emptyList()
        @Suppress("UNCHECKED_CAST")
        val programsRaw = root["programs"] as List<Map<String, Any>>
        val programs = programsRaw.map { parseProgram(it) }
        return CaseFile(file.nameWithoutExtension, source, tags, programs)
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseProgram(map: Map<String, Any>): ProgramCase {
        val name = map["name"] as String
        val dependsOn = map["dependsOn"] as? String
        val runMap = map["run"] as? Map<String, Any>
        val run = runMap?.let { parseRun(it) }
        return ProgramCase(name, dependsOn, run)
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseRun(map: Map<String, Any>): RunConfig {
        val timeoutMs = (map["timeoutMs"] as? Number)?.toLong() ?: 30_000L
        val durationMs = (map["durationMs"] as? Number)?.toLong()
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
