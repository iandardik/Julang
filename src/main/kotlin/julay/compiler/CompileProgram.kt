package julay.compiler

import julay.compiler.ast.RootNode
import julay.compiler.decl.ProcDecl
import julay.compiler.pass.codegenPass
import java.io.File
import kotlin.system.exitProcess

fun compileProgram(program : ProcDecl, ast : RootNode, procDecls : List<ProcDecl>, keepBuild : Boolean = false) {
    val buildDir = "${program.name}-jul-build"
    if (!File(buildDir).exists() && !File(buildDir).mkdir()) {
        println("Could not create $buildDir dir")
        exitProcess(1)
    }

    // Each program gets its own generated source file; remove stale ones so Gradle
    // does not compile multiple programs' p-classes into the same module.
    File(buildDir).listFiles()?.filter { it.isFile && it.extension == "kt" }?.forEach { it.delete() }
    deleteDirectory(File("$buildDir/build"))

    val codegen = codegenPass(ast, program, procDecls)
    val fileName = "${codegen.mainClassName}.kt"
    File("$buildDir/$fileName").writeText(codegen.sourceText)

    File("$buildDir/settings.gradle.kts").delete()
    File("$buildDir/build.gradle.kts").delete()

    File("$buildDir/settings.gradle.kts").writeText(gradleSettingsFileContents(program.name))
    Runtime.getRuntime().exec(arrayOf("bash", "-c", "cd $buildDir; gradle wrapper --gradle-version 8.5")).waitFor()
    File("$buildDir/build.gradle.kts").writeText(gradleBuildFileContents(program.name, codegen.mainClassName))
    val gradleProc = Runtime.getRuntime().exec(arrayOf("bash", "-c", "cd $buildDir; ./gradlew shadowJar 2>&1"))
    val gradleOutput = gradleProc.inputStream.bufferedReader().readText()
    val gradleExit = gradleProc.waitFor()
    if (gradleExit != 0) {
        println("Gradle build failed for program \"${program.name}\" (exit $gradleExit):\n$gradleOutput")
        return
    }
    if (!keepBuild) {
        deleteDirectory(File(buildDir))
    }
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

private fun gradleBuildFileContents(name : String, mainClassName : String) : String {
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
            "    implementation(files(\"../julayc.jar\"))\n" +
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
