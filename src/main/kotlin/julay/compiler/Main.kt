package julay.compiler

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.optionalValue
import com.github.ajalt.clikt.parameters.types.path
import julay.compiler.analysis.AnalyzeOptions

class Julayc : CliktCommand(name = "julayc") {
    init {
        subcommands(AnalyzeCommand())
    }

    /**
     * Allow `julayc file.jul` (compile) while still supporting `julayc analyze ...`.
     * Without this, Clikt prints help whenever no subcommand is given.
     */
    override val invokeWithoutSubcommand: Boolean = true

    override fun helpEpilog(context: Context) = """
        Environment variables:
          JULAY_PATH  Colon-separated list of directories searched for imported .jul modules
                      (after the entry file's directory, any -L paths, and the embedded stdlib).

        Stdlib: julaylib.pclass.Println, ExitSystem, Readln, and Timer are Julay modules shipped in the
        compiler jar. julaylib.pclass.HttpServer and julaylib.pclass.HttpClient remain Kotlin-native libraries
        (with builtin HttpServerRequest/Response and HttpClientRequest/Response o-classes).

        Use `julayc analyze --help` to inspect program structure without codegen.
    """.trimIndent()

    private val keepBuild by option(
        "--keep-build",
        help = "Keep generated <program>-jul-build directories after a successful compile",
    ).flag()

    private val libraryPaths by option(
        "-L",
        help = "Add a directory to the module search path",
    ).path(mustExist = true, canBeDir = true).multiple()

    /**
     * null = flag absent; "" = `--program` with no name (all programs);
     * non-empty = exactly that program.
     * Value must be `--program=Name` (not `--program Name`) so the input file is not consumed.
     */
    private val programOpt by option(
        "--program",
        metavar = "NAME",
        help = "Compile programs only; optional NAME (--program=Name) selects exactly one program",
    ).optionalValue("", acceptsUnattachedValue = false)

    private val specOpt by option(
        "--spec",
        metavar = "NAME",
        help = "Compile specs only; optional NAME (--spec=Name) selects exactly one spec",
    ).optionalValue("", acceptsUnattachedValue = false)

    private val input by argument(
        help = "Jul source file to compile",
    ).path(mustExist = true, canBeFile = true).optional()

    override fun run() {
        if (currentContext.invokedSubcommand != null) {
            return
        }
        val file = input ?: throw UsageError("Missing argument \"<input>\"")
        val neither = programOpt == null && specOpt == null
        val targets = CompileTargets(
            compilePrograms = neither || programOpt != null,
            programNames = programOpt?.takeIf { it.isNotEmpty() }?.let { setOf(it) },
            compileSpecs = neither || specOpt != null,
            specNames = specOpt?.takeIf { it.isNotEmpty() }?.let { setOf(it) },
        )
        compileJulFile(file, keepBuild, libraryPaths, targets = targets)
    }
}

class AnalyzeCommand : CliktCommand(name = "analyze") {
    override fun help(context: Context) =
        "Inspect composition and actions without compiling (default view: --tree)"

    override fun helpEpilog(context: Context) = """
        Examples:
          julayc analyze input/raft/main.jul
          julayc analyze -s NodeLogic --actions input/raft/main.jul
          julayc analyze -s NodeLogic --actions-detail --action-regex '^handle' input/raft/main.jul
          julayc analyze -s Raft -s Client --pclasses input/raft/main.jul
          julayc analyze -s RaftCore --pclasses-detail input/raft/main.jul
          julayc analyze -s NodeLogic --tree --actions input/raft/main.jul
    """.trimIndent()

    private val libraryPaths by option(
        "-L",
        help = "Add a directory to the module search path",
    ).path(mustExist = true, canBeDir = true).multiple()

    private val scopeNames by option(
        "-s", "--scope",
        metavar = "NAME",
        help = "Restrict to proc/program/spec/pclass NAME (repeatable; action views use union unless --intersect)",
    ).multiple()

    private val scopeIntersect by option(
        "--intersect",
        help = "With multiple -s, list only actions that appear in every scope (default: union)",
    ).flag()

    private val showTree by option(
        "--tree",
        help = "Print composition tree for the scope",
    ).flag()

    private val showActions by option(
        "--actions",
        help = "List action names in the scope (internal actions omitted unless --include-internal)",
    ).flag()

    private val actionsDetail by option(
        "--actions-detail",
        help = "Per action: offering pclasses and modifiers (internal omitted unless --include-internal)",
    ).flag()

    private val showPclasses by option(
        "--pclasses",
        help = "List pclass names in the scope",
    ).flag()

    private val pclassesDetail by option(
        "--pclasses-detail",
        help = "Per pclass: its actions and modifiers (internal omitted unless --include-internal)",
    ).flag()

    private val actionNames by option(
        "--action",
        help = "Filter to this exact action name (repeatable)",
    ).multiple()

    private val actionRegex by option(
        "--action-regex",
        help = "Filter actions to names matching this regex",
    )

    private val includeInternal by option(
        "--include-internal",
        help = "Include internal actions in action listings (hidden by default)",
    ).flag()

    private val input by argument(
        help = "Jul source file to analyze",
    ).path(mustExist = true, canBeFile = true)

    override fun run() {
        val anyView = showTree || showActions || actionsDetail || showPclasses || pclassesDetail
        val options = AnalyzeOptions(
            scopeNames = scopeNames,
            showTree = if (anyView) showTree else true,
            showActions = showActions,
            actionsDetail = actionsDetail,
            showPclasses = showPclasses,
            pclassesDetail = pclassesDetail,
            actionNames = actionNames,
            actionRegex = actionRegex,
            includeInternal = includeInternal,
            scopeIntersect = scopeIntersect,
        )
        analyzeJulFile(input, options, libraryPaths)
    }
}

fun main(args: Array<String>) = Julayc().main(args)
