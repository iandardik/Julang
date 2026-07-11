package julay.compiler

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path

class Julayc : CliktCommand(name = "julayc") {
    override fun helpEpilog(context: Context) = """
        Environment variables:
          JULAY_PATH  Colon-separated list of directories searched for imported .jul modules
                      (after the entry file's directory, any -L paths, and the embedded stdlib).

        Stdlib: julaylib.pclass.Println, ExitSystem, Readln, and Timer are Julay modules shipped in the
        compiler jar. julaylib.pclass.HttpServer and julaylib.pclass.HttpClient remain Kotlin-native libraries.
    """.trimIndent()

    private val keepBuild by option(
        "--keep-build",
        help = "Keep generated <program>-jul-build directories after a successful compile",
    ).flag()

    private val libraryPaths by option(
        "-L",
        help = "Add a directory to the module search path",
    ).path(mustExist = true, canBeDir = true).multiple()

    private val input by argument(
        help = "Jul source file to compile",
    ).path(mustExist = true, canBeFile = true)

    override fun run() {
        compileJulFile(input, keepBuild, libraryPaths)
    }
}

fun main(args : Array<String>) = Julayc().main(args)
