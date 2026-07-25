package julay.compiler

import julay.compiler.ast.CompileNode
import julay.compiler.ast.RootNode
import julay.compiler.decl.ProcDecl
import julay.compiler.decl.ProcDeclType
import julay.compiler.pass.errorPass
import julay.compiler.pass.resolvedProcPass
import julay.compiler.pass.typePass
import julay.compiler.pass.warningPass
import java.nio.file.Path
import kotlin.io.path.absolute
import kotlin.system.exitProcess

data class CheckResult(
    val diagnostics: List<StructuredDiagnostic>,
) {
    val hasErrors: Boolean get() = diagnostics.any { it.severity == DiagnosticSeverity.Error }
}

/**
 * Type-check [source] (and imports) without codegen. Collects load, type, and
 * structural diagnostics for IDE / `julayc check --json`.
 */
fun checkJulFile(
    source: Path,
    extraLibraryPaths: List<Path> = emptyList(),
    allowUnindexedSpec: Boolean = false,
): CheckResult {
    val entry = source.absolute()
    val diagnostics = mutableListOf<StructuredDiagnostic>()

    val (unit, loadErrors) = loadCompilationUnit(entry, extraLibraryPaths)
    diagnostics += loadErrors.map { it.toStructuredDiagnostic(entry) }
    if (loadErrors.isNotEmpty()) {
        return CheckResult(diagnostics)
    }

    val ast = unit.root
    val procDecls = ast.resolvedProcPass(unit)

    val typeResult = ast.typePass(unit, allowUnindexedSpec)
    diagnostics += typeResult.errors.map { it.toStructuredDiagnostic(entry) }
    diagnostics += typeResult.warnings.map { it.toStructuredDiagnostic(entry) }
    if (typeResult.errors.isNotEmpty()) {
        return CheckResult(diagnostics)
    }

    val (jarTargets, _, targetErrors) = resolveCompileTargetsCollecting(ast, unit, procDecls)
    diagnostics += targetErrors.map { it.toStructuredDiagnostic(entry) }
    if (targetErrors.isNotEmpty()) {
        return CheckResult(diagnostics)
    }

    val librariesInUse = unit.librariesInUse(jarTargets, procDecls)

    if (jarTargets.isEmpty()) {
        val components = unit.allPClassNames + librariesInUse
        collectPassDiagnostics(ast, components, librariesInUse, null, procDecls, entry, diagnostics)
    } else {
        for (program in jarTargets) {
            val components = program.allProcNames(procDecls)
            collectPassDiagnostics(ast, components, librariesInUse, program, procDecls, entry, diagnostics)
        }
    }

    return CheckResult(diagnostics)
}

private fun collectPassDiagnostics(
    ast: RootNode,
    components: Set<String>,
    librariesInUse: Set<String>,
    program: ProcDecl?,
    procDecls: List<ProcDecl>,
    entry: Path,
    out: MutableList<StructuredDiagnostic>,
) {
    val errors = ast.errorPass(
        components,
        librariesInUse,
        program = program,
        procDecls = procDecls,
    )
    out += errors.map { it.toStructuredDiagnostic(entry) }
    if (errors.isNotEmpty()) {
        return
    }
    val warnings = ast.warningPass(components, librariesInUse, program, procDecls)
    out += warnings.map { it.toStructuredDiagnostic(entry) }
}

private data class CompileTargetsCollect(
    val jarTargets: List<ProcDecl>,
    val specTargets: List<ProcDecl>,
    val errors: List<CompileError>,
)

private fun resolveCompileTargetsCollecting(
    ast: RootNode,
    unit: CompilationUnit,
    procDecls: List<ProcDecl>,
): CompileTargetsCollect {
    val names = ast.declNodes()
        .filterIsInstance<CompileNode>()
        .flatMap { it.compileNames() }
        .distinct()
    if (names.isEmpty()) {
        return CompileTargetsCollect(emptyList(), emptyList(), emptyList())
    }
    val byName = procDecls.associateBy { it.name }
    val jars = mutableListOf<ProcDecl>()
    val specs = mutableListOf<ProcDecl>()
    val missing = mutableListOf<String>()
    for (name in names) {
        val decl = byName[name]
        when {
            decl?.type == ProcDeclType.Spec -> specs += decl
            decl?.type == ProcDeclType.Proc -> jars += decl
            name in unit.allPClassNames -> jars += ProcDecl(name, emptyList(), ProcDeclType.Proc)
            else -> missing += name
        }
    }
    if (missing.isNotEmpty()) {
        val msg = "Unknown compile name(s): ${missing.joinToString(", ")}"
        return CompileTargetsCollect(
            emptyList(),
            emptyList(),
            listOf(OneLocCompileError(SourceLoc(1 to 1, unit.entryPath), msg)),
        )
    }
    return CompileTargetsCollect(jars, specs, emptyList())
}

fun printCheckResult(result: CheckResult, asJson: Boolean) {
    if (asJson) {
        print(buildDiagnosticsJsonDocument(result.diagnostics))
    } else {
        result.diagnostics.forEach { d ->
            val stream = if (d.severity == DiagnosticSeverity.Warning) System.err else System.out
            val fileName = d.file?.fileName?.toString()
            val loc = if (fileName != null) {
                if (d.startLine == d.endLine) "$fileName:line ${d.startLine}"
                else "$fileName:lines ${d.startLine}-${d.endLine}"
            } else {
                if (d.startLine == d.endLine) "line ${d.startLine}"
                else "lines ${d.startLine}-${d.endLine}"
            }
            val prefix = if (d.severity == DiagnosticSeverity.Warning) "warning: " else ""
            stream.println("$loc: $prefix${d.message}")
        }
        if (result.hasErrors) {
            println("Found compile errors, exiting.")
        }
    }
}

fun checkJulFileAndExit(
    source: Path,
    extraLibraryPaths: List<Path> = emptyList(),
    asJson: Boolean = false,
    allowUnindexedSpec: Boolean = false,
) {
    val result = checkJulFile(source, extraLibraryPaths, allowUnindexedSpec)
    printCheckResult(result, asJson)
    if (result.hasErrors) {
        exitProcess(1)
    }
}
