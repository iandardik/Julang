package julay.compiler

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path

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
