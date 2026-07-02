package julay.compiler

import julay.compiler.ast.RootNode
import julay.compiler.decl.ProcDecl
import julay.compiler.pass.codegenPass
import java.io.File
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

private const val GRADLE_WRAPPER_TIMEOUT_MINUTES = 5L
private const val GRADLE_BUILD_TIMEOUT_MINUTES = 10L

fun compileProgram(
    program: ProcDecl,
    ast: RootNode,
    procDecls: List<ProcDecl>,
    librariesInUse: Set<String> = emptySet(),
    keepBuild: Boolean = false,
    compilerJar: Path = resolveCompilerJar(),
) {
    val buildDir = "${program.name}-jul-build"
    if (!File(buildDir).exists() && !File(buildDir).mkdir()) {
        println("Could not create $buildDir dir")
        exitProcess(1)
    }

    // Each program gets its own generated source file; remove stale ones so Gradle
    // does not compile multiple programs' p-classes into the same module.
    File(buildDir).listFiles()?.filter { it.isFile && it.extension == "kt" }?.forEach { it.delete() }
    deleteDirectory(File("$buildDir/build"))

    val codegen = codegenPass(ast, program, procDecls, librariesInUse)
    val fileName = "${codegen.mainClassName}.kt"
    File("$buildDir/$fileName").writeText(codegen.sourceText)

    File("$buildDir/settings.gradle.kts").delete()
    File("$buildDir/build.gradle.kts").delete()

    File("$buildDir/settings.gradle.kts").writeText(gradleSettingsFileContents(program.name))
    val wrapperResult = runShellCommand(
        "cd $buildDir; gradle wrapper --gradle-version 8.5",
        GRADLE_WRAPPER_TIMEOUT_MINUTES,
    )
    if (wrapperResult.timedOut) {
        println("Gradle wrapper timed out for program \"${program.name}\"")
        return
    }
    if (wrapperResult.exitCode != 0) {
        println("Gradle wrapper failed for program \"${program.name}\" (exit ${wrapperResult.exitCode}):\n${wrapperResult.output}")
        return
    }

    File("$buildDir/build.gradle.kts").writeText(
        gradleBuildFileContents(program.name, codegen.mainClassName, compilerJar),
    )
    val gradleResult = runShellCommand(
        "cd $buildDir; ./gradlew shadowJar 2>&1",
        GRADLE_BUILD_TIMEOUT_MINUTES,
    )
    if (gradleResult.timedOut) {
        println("Gradle build timed out for program \"${program.name}\"")
        return
    }
    if (gradleResult.exitCode != 0) {
        println("Gradle build failed for program \"${program.name}\" (exit ${gradleResult.exitCode}):\n${gradleResult.output}")
        return
    }
    if (!keepBuild) {
        deleteDirectory(File(buildDir))
    }
}

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

// thank you: https://www.baeldung.com/kotlin/delete-directories-with-contents
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

private fun gradleSettingsFileContents(name : String) : String {
    return "rootProject.name = \"$name\""
}

private fun gradleBuildFileContents(name: String, mainClassName: String, compilerJar: Path): String {
    val compilerJarPath = compilerJar.toGradlePathLiteral()
    return "plugins {\n" +
            "    kotlin(\"jvm\") version \"2.1.0\"\n" +
            "    application\n" +
            "    id(\"com.github.johnrengelman.shadow\") version \"8.1.1\"\n" +
            "}\n" +
            "\n" +
            "\n" +
            "repositories {\n" +
            "    mavenCentral()\n" +
            "}\n" +
            "\n" +
            "dependencies {\n" +
            "    implementation(files(\"$compilerJarPath\"))\n" +
            "}\n" +
            "\n" +
            "application {\n" +
            "    mainClass.set(\"${mainClassName}Kt\")\n" +
            "}\n" +
            "\n" +
            "kotlin {\n" +
            "    jvmToolchain(17)\n" +
            "}\n" +
            "\n" +
            "sourceSets {\n" +
            "    main {\n" +
            "        kotlin {\n" +
            "            srcDir(\".\")\n" +
            "            include(\"${mainClassName}.kt\")\n" +
            "        }\n" +
            "    }\n" +
            "}\n" +
            "\n" +
            "tasks.shadowJar {\n" +
            "    archiveBaseName.set(\"$name\")\n" +
            "    archiveVersion.set(\"\")\n" +
            "    archiveClassifier.set(\"\")\n" +
            "    destinationDirectory.set(file(\"\$buildDir/../..\"))\n" +
            "}"
}
