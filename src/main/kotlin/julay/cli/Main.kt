package julay.cli

import julay.ast.ASTBuilder
import julay.ast.ProcDecl
import julay.ast.ProcDeclType
import julay.ast.RootNode
import julay.parser.JulayLexer
import julay.parser.JulayParser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import java.io.File
import kotlin.system.exitProcess

fun main(args : Array<String>) {
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
    val procDecls = ast.procPass()
    val programs = procDecls.filter { it.type == ProcDeclType.Program }

    val typeErrors = ast.typePass()
    if (typeErrors.isNotEmpty()) {
        typeErrors.forEach { println(it) }
        println("Found type errors; exiting.")
        return
    }

    // check for errors on each program individually
    programs.forEach { program ->
        val components = program.allProcNames(procDecls)
        val errors = ast.errorPass(components)
        if (errors.isNotEmpty()) {
            errors.forEach { println(it) }
            println("Found errors while compiling the program \"${program.name}\"; exiting.")
            return
        }
    }

    // compile each program
    programs.forEach { compileProgram(it, ast, it.name) }
}

fun compileProgram(program : ProcDecl, ast : RootNode, progName : String) {
    val buildDir = "jul-build"
    if (!File(buildDir).exists() && !File(buildDir).mkdir()) {
        println("Could not create $buildDir dir")
        exitProcess(1)
    }

    // TODO make this cleaner
    val libPClassNames = setOf("Println", "Readln", "HttpServer", "HttpClient", "Timer")
    val libStaticInfoMap = mapOf(
        Pair("Println", "PrintlnTS"),
        Pair("Readln", "ReadlnTS"),
        Pair("HttpServer", "JulHttpServer"),
        Pair("HttpClient", "JulHttpClient"),
        Pair("Timer", "TimerTS"),
    )

    // TODO multiple calls to ast.procPass() is not very efficient
    val procsToCompile = program.allProcNames(ast.procPass()).filter { it !in libPClassNames }
    val procClasses = procsToCompile.flatMap { proc ->
        val procClass = ast.procClassPass(setOf(proc))
        julay.tools.assert(procClass.size == 1, "Expected exactly one proc class for \"$proc\" but found: ${procClass.size}")
        procClass
    }

    // TODO multiple calls to ast.procPass() is not very efficient
    val libProcs = program.allProcNames(ast.procPass()).filter { it in libPClassNames }
    val staticInfoLib = libProcs.map { "${libStaticInfoMap[it]!!}.staticInfo()" }

    val staticInfoCompiledProcs = procClasses.map { it.toKotlinStaticInfoString() }
    val staticInfoBody = (staticInfoCompiledProcs + staticInfoLib).joinToString(",\n") { it }
    val staticInfo = "val tsInfo = setOf(\n" + staticInfoBody.prependIndent() + "\n)"
    val runProgram = "Program(tsInfo).run()"
    val mainFunction = "suspend fun main(args : Array<String>) {" +
            "\n$staticInfo".prependIndent() +
            "\n$runProgram".prependIndent() +
            "\n}"

    val imports = "import com.microsoft.z3.*\n" +
            "import julay.program.*\n" +
            "import julay.program.library.*\n" +
            "import julay.tools.mkStringConst\n"
    val programText = "$imports\n" +
            procClasses.joinToString("\n\n") { it.toKotlinClassString() } +
            "\n\n" +
            mainFunction

    //val name = File(inputFile).nameWithoutExtension.replaceFirstChar { it.uppercase() }
    val mainClassName = progName.replaceFirstChar { it.uppercase() }
    val fileName = "${mainClassName}.kt"
    File("$buildDir/$fileName").writeText(programText)

    File("$buildDir/settings.gradle.kts").delete()
    File("$buildDir/build.gradle.kts").delete()

    File("$buildDir/settings.gradle.kts").writeText(gradleSettingsFileContents(progName))
    Runtime.getRuntime().exec(arrayOf("bash", "-c", "cd $buildDir; gradle wrapper --gradle-version 8.5")).waitFor()
    File("$buildDir/build.gradle.kts").writeText(gradleBuildFileContents(progName, mainClassName))
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

fun gradleBuildFileContents(name : String, mainClassName : String) : String {
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
