package julay.regression

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.TimeUnit

object JulCompiler {
    fun compile(projectRoot: File, workspace: File, sourceJul: File): Map<String, File> {
        val compilerJar = resolveCompilerJar(projectRoot)
        val dest = File(workspace, "julayc.jar")
        Files.copy(compilerJar.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING)

        val proc = ProcessBuilder(
            "java", "-jar", dest.absolutePath, sourceJul.absolutePath,
        )
            .directory(workspace)
            .redirectErrorStream(true)
            .start()

        val output = proc.inputStream.bufferedReader().readText()
        val finished = proc.waitFor(15, TimeUnit.MINUTES)
        check(finished) { "Compiler timed out for ${sourceJul.path}\n$output" }

        val jars = workspace.listFiles { f ->
            f.isFile && f.extension == "jar" && f.name != "julayc.jar"
        }?.associateBy { it.nameWithoutExtension }
            ?: emptyMap()

        check(jars.isNotEmpty()) {
            "No program JARs produced in ${workspace.absolutePath}\n$output"
        }
        return jars
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
}
