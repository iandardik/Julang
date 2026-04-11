package exspecs.cli

import exspecs.ast.ASTBuilder
import exspecs.ast.ProcDeclType
import exspecs.ast.RootNode
import exspecs.parser.JulayLexer
import exspecs.parser.JulayParser
import exspecs.program.library.printlnTSStaticInfoStr
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import java.io.File

fun main(args : Array<String>) {
    val buildDir = "jul-build"
    if (!File(buildDir).exists() && !File(buildDir).mkdir()) {
        println("Could not create $buildDir dir")
        return
    }

    if (args.size != 1) {
        println("usage: Exspec <.jul file>")
        return
    }
    val input = CharStreams.fromFileName(args[0])
    val lexer = JulayLexer(input)
    val tokens = CommonTokenStream(lexer)
    val parser = JulayParser(tokens)
    val root = parser.root()
    if (parser.numberOfSyntaxErrors > 0) {
        println("Found compile errors, exiting.")
        return
    }
    val ast = ASTBuilder().visit(root) as RootNode
    val errors = ast.errorPass()
    if (errors.isNotEmpty()) {
        errors.forEach { println(it) }
        println("Found compile errors, exiting.")
        return
    }

    val libPClassNames = setOf("Println")
    val libStaticInfoMap = mapOf(Pair("Println", printlnTSStaticInfoStr)) // TODO don't use strings

    val procDecls = ast.procPass()
    val programs = procDecls.filter { it.type == ProcDeclType.Program }
    exspecs.tools.assert(programs.size == 1, "Expected exactly one program decl, got: ${programs.size}")

    val program = programs[0]
    val procsToCompile = program.allProcNames().filter { it !in libPClassNames }
    val compiledProcs = procsToCompile.flatMap {
        val compiled = ast.procClassPass(it)
        exspecs.tools.assert(compiled.size == 1, "Expected exactly one compiled proc, got: ${compiled.size}")
        compiled
    }

    val libProcs = program.allProcNames().filter { it in libPClassNames }
    val staticInfoLib = libProcs.map { libStaticInfoMap[it]!! }

    val staticInfoCompiledProcs = compiledProcs.map { it.toKotlinStaticInfoString() }
    val staticInfoBody = (staticInfoCompiledProcs + staticInfoLib).joinToString(",\n") { it }
    val staticInfo = "val tsInfo = setOf(\n" + staticInfoBody.prependIndent() + "\n)"
    val runProgram = "Program(tsInfo).run()"
    val mainFunction = "fun main(args : Array<String>) {" +
            "\n$staticInfo".prependIndent() +
            "\n$runProgram".prependIndent() +
            "\n}"

    val imports = "import com.microsoft.z3.*\n" +
            "import exspecs.program.*\n" +
            "import exspecs.program.library.*\n" +
            "import exspecs.tools.mkStringConst\n"
    val programText = "$imports\n" +
            compiledProcs.joinToString("\n\n") { it.toKotlinClassString() } +
            "\n\n" +
            mainFunction

    val woExt = args[0].replace(Regex("\\..*$"),"") // remove the file extension
    val name = woExt.replaceFirstChar { it.uppercase() }
    val fileName = "${name}.kt"
    File("$buildDir/$fileName").writeText(programText)

    File("$buildDir/settings.gradle.kts").delete()
    File("$buildDir/build.gradle.kts").delete()

    File("$buildDir/settings.gradle.kts").writeText(gradleSettingsFileContents(name))
    Runtime.getRuntime().exec(arrayOf("bash", "-c", "cd $buildDir; gradle wrapper --gradle-version 8.5")).waitFor()
    File("$buildDir/build.gradle.kts").writeText(gradleBuildFileContents(name))
    Runtime.getRuntime().exec(arrayOf("bash", "-c", "cd $buildDir; ./gradlew shadowJar")).waitFor()
    deleteDirectory(File(buildDir))
}

// thank you: https://www.baeldung.com/kotlin/delete-directories-with-contents
fun deleteDirectory(directory: File) {
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

fun gradleSettingsFileContents(name : String) : String {
    return "rootProject.name = \"$name\""
}

fun gradleBuildFileContents(name : String) : String {
    return "plugins {\n" +
            "    kotlin(\"jvm\") version \"1.9.24\"\n" +
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
            "    implementation(files(\"../Exspecs.jar\"))\n" +
            "}\n" +
            "\n" +
            "application {\n" +
            "    mainClass.set(\"${name}Kt\")\n" +
            "}\n" +
            "\n" +
            "kotlin {\n" +
            "    jvmToolchain(17)\n" +
            "}\n" +
            "\n" +
            "sourceSets {\n" +
            "    main {\n" +
            "        kotlin.srcDirs(\".\")\n" +
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
