package julay.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path
import julay.ast.ASTBuilder
import julay.ast.ProcDecl
import julay.ast.ProcDeclType
import julay.ast.RootNode
import julay.parser.JulayLexer
import julay.parser.JulayParser
import julay.program.library.LibraryRegistry
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import java.io.File
import java.nio.file.Path
import kotlin.io.path.pathString
import kotlin.system.exitProcess

class Julayc : CliktCommand(name = "julayc") {
    private val keepBuild by option(
        "--keep-build",
        help = "Keep generated <program>-jul-build directories after a successful compile",
    ).flag()

    private val input by argument(
        help = "Jul source file to compile",
    ).path(mustExist = true, canBeFile = true)

    override fun run() {
        compileJulFile(input, keepBuild)
    }
}

fun main(args : Array<String>) = Julayc().main(args)

fun compileJulFile(source : Path, keepBuild : Boolean) {
    val input = CharStreams.fromFileName(source.pathString)
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

    val flatAst = ast.flattenObjClassPass(ast.resolvedObjClassRegistry())

    // compile each program
    programs.forEach { compileProgram(it, flatAst, it.name, keepBuild) }
}

fun compileProgram(program : ProcDecl, ast : RootNode, progName : String, keepBuild : Boolean = false) {
    val buildDir = "$progName-jul-build"
    if (!File(buildDir).exists() && !File(buildDir).mkdir()) {
        println("Could not create $buildDir dir")
        exitProcess(1)
    }

    // Each program gets its own generated source file; remove stale ones so Gradle
    // does not compile multiple programs' p-classes into the same module.
    File(buildDir).listFiles()?.filter { it.isFile && it.extension == "kt" }?.forEach { it.delete() }
    deleteDirectory(File("$buildDir/build"))

    val libPClassNames = LibraryRegistry.julNames

    // TODO multiple calls to ast.procPass() is not very efficient
    val procsToCompile = program.allProcNames(ast.procPass()).filter { it !in libPClassNames }
    val procClasses = procsToCompile.flatMap { proc ->
        val procClass = ast.procClassPass(setOf(proc))
        julay.tools.assert(procClass.size == 1, "Expected exactly one proc class for \"$proc\" but found: ${procClass.size}")
        procClass
    }

    // TODO multiple calls to ast.procPass() is not very efficient
    val libProcs = program.allProcNames(ast.procPass()).filter { it in libPClassNames }
    val staticInfoLib = libProcs.map { LibraryRegistry.staticInfoCodegenExpr(it) }

    val staticInfoCompiledProcs = procClasses.map { it.toKotlinStaticInfoString() }
    val staticInfoBody = (staticInfoCompiledProcs + staticInfoLib).joinToString(",\n") { it }
    val staticInfo = "val tsInfo = setOf(\n" + staticInfoBody.prependIndent() + "\n)"
    val runProgram = "Program(tsInfo).run()"
    val mainFunction = "suspend fun main(args : Array<String>) {" +
            "\n$staticInfo".prependIndent() +
            "\n$runProgram".prependIndent() +
            "\n}"

    val imports = "import com.microsoft.z3.*\n" +
            "import julay.ast.ObjClassType\n" +
            "import julay.program.*\n" +
            "import julay.program.library.*\n" +
            "import julay.tools.mkStringConst\n"
    val objClassDecls = ast.resolvedObjClassDecls()
    val objClassCode = objClassDecls.joinToString("\n\n") { it.toKotlinTypeValString() }
    val objClassSection = if (objClassCode.isEmpty()) "" else "$objClassCode\n\n"
    val programText = "$imports\n" +
            objClassSection +
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
    val gradleProc = Runtime.getRuntime().exec(arrayOf("bash", "-c", "cd $buildDir; ./gradlew shadowJar 2>&1"))
    val gradleOutput = gradleProc.inputStream.bufferedReader().readText()
    val gradleExit = gradleProc.waitFor()
    if (gradleExit != 0) {
        println("Gradle build failed for program \"$progName\" (exit $gradleExit):\n$gradleOutput")
        return
    }
    if (!keepBuild) {
        deleteDirectory(File(buildDir))
    }
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
