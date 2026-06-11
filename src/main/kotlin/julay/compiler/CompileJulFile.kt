package julay.compiler

import julay.compiler.ast.ASTBuilder
import julay.compiler.ast.RootNode
import julay.compiler.decl.ProcDecl
import julay.compiler.decl.ProcDeclType
import julay.compiler.pass.errorPass
import julay.compiler.pass.flattenObjClassPass
import julay.compiler.pass.procPass
import julay.compiler.pass.resolvedObjClassRegistry
import julay.compiler.pass.typePass
import julay.parser.JulayLexer
import julay.parser.JulayParser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import java.nio.file.Path
import kotlin.io.path.pathString

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
    programs.forEach { compileProgram(it, flatAst, procDecls, keepBuild) }
}
