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
import com.github.ajalt.clikt.parameters.types.path
import com.github.ajalt.clikt.parameters.options.optionalValue
import julay.compiler.analysis.AnalyzeOptions
import julay.compiler.pass.TlaOptConfig
import julay.program.sync.SyncResolveConfig

class Julayc : CliktCommand(name = "julayc") {
    init {
        subcommands(AnalyzeCommand(), CheckCommand())
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

        Stdlib: julay.proclib.Timer is a Julay module shipped in the compiler jar.
        julay.proclib.HttpServer remains a Kotlin-native library
        (with builtin HttpServerRequest/Response and HttpClientRequest/Response obj types).
        Outbound HTTP uses the julay.funlib.httpRequest procfun.

        Use `julayc analyze --help` to inspect program structure without codegen.
        Use `julayc check --help` to type-check and emit diagnostics without codegen.
    """.trimIndent()

    private val keepBuild by option(
        "--keep-build",
        help = "Keep generated <name>-jul-build directories after a successful compile",
    ).flag()

    private val allowUnindexedSpec by option(
        "--allow-unindexed-spec",
        help = "Warn instead of error when a multi-instance proc appears unindexed in a spec",
    ).flag()

    private val libraryPaths by option(
        "-L",
        help = "Add a directory to the module search path",
    ).path(mustExist = true, canBeDir = true).multiple()

    private val compileNames by option(
        "--compile",
        metavar = "NAME",
        help = "Compile proc/spec NAME (repeatable; ignores source compile directives)",
    ).multiple()

    private val compileTlaNames by option(
        "--compile-tla",
        metavar = "NAME",
        help = "Emit TLA+ for proc NAME as <true> NAME <true> (repeatable; no extra invariants)",
    ).multiple()

    private val disableOpt by option(
        "--disable-opt",
        metavar = "OPT,...",
        help = "Disable sync optimizations. Use bare --disable-opt to disable all " +
            "(eq-unify, arg-ownership, directed-eval), or --disable-opt=ID,ID for a subset. " +
            "The '=' form is required when passing ids so the input path is not consumed. " +
            "See docs/language/compiler-optimizations.md",
    ).optionalValue("ALL", acceptsUnattachedValue = false)

    private val disableTlaOpt by option(
        "--disable-tla-opt",
        metavar = "OPT,...",
        help = "Disable TLA+ emission optimizations. Use bare --disable-tla-opt to disable all " +
            "(unused-fields, unused-vars, determined-args, from-collection, literal-domains, unwrap-singletons, unused-lets), " +
            "or --disable-tla-opt=ID,ID for a subset. " +
            "The '=' form is required when passing ids so the input path is not consumed. " +
            "See docs/language/compiler-optimizations.md",
    ).optionalValue("ALL", acceptsUnattachedValue = false)

    private val verbose by option(
        "--verbose",
        help = "Print sync-path summary (FastOnly vs NeedsZ3).",
    ).flag()

    private val input by argument(
        help = "Jul source file to compile",
    ).path(mustExist = true, canBeFile = true).optional()

    override fun run() {
        if (currentContext.invokedSubcommand != null) {
            return
        }
        val file = input ?: throw UsageError("Missing argument \"<input>\"")
        val syncResolveConfig = try {
            SyncResolveConfig.fromDisableOptFlag(disableOpt)
        } catch (e: IllegalArgumentException) {
            throw UsageError(e.message ?: "Invalid --disable-opt")
        }
        val tlaOptConfig = try {
            TlaOptConfig.fromDisableTlaOptFlag(disableTlaOpt)
        } catch (e: IllegalArgumentException) {
            throw UsageError(e.message ?: "Invalid --disable-tla-opt")
        }
        compileJulFile(
            file,
            keepBuild,
            libraryPaths,
            allowUnindexedSpec = allowUnindexedSpec,
            compileNames = compileNames,
            compileTlaNames = compileTlaNames,
            syncResolveConfig = syncResolveConfig,
            tlaOptConfig = tlaOptConfig,
            verbose = verbose,
        )
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
          julayc analyze -s Raft -s Client --procs input/raft/main.jul
          julayc analyze -s RaftCore --procs-detail input/raft/main.jul
          julayc analyze -s NodeLogic --tree --actions input/raft/main.jul
          julayc analyze -s TermTest1 --json regression/input/basic/test1.jul
    """.trimIndent()

    private val libraryPaths by option(
        "-L",
        help = "Add a directory to the module search path",
    ).path(mustExist = true, canBeDir = true).multiple()

    private val scopeNames by option(
        "-s", "--scope",
        metavar = "NAME",
        help = "Restrict to proc/spec NAME (repeatable; action views use union unless --intersect/--mutual)",
    ).multiple()

    private val scopeIntersect by option(
        "--intersect",
        help = "With multiple -s, list only actions that appear in every scope (default: union)",
    ).flag()

    private val scopeMutual by option(
        "--mutual",
        help = "With multiple -s, list only actions that appear in every scope and that those scopes sync on (default: union)",
    ).flag()

    private val showTree by option(
        "--tree",
        help = "Print composition tree for the scope",
    ).flag()

    private val showActions by option(
        "--actions",
        help = "List action names in the scope (internal and composition-hidden syncs omitted unless --include-internal)",
    ).flag()

    private val actionsDetail by option(
        "--actions-detail",
        help = "Per action: offering procs and modifiers (internal and composition-hidden omitted unless --include-internal)",
    ).flag()

    private val showProcs by option(
        "--procs",
        help = "List proc class names in the scope",
    ).flag()

    private val procsDetail by option(
        "--procs-detail",
        help = "Per proc class: its actions and modifiers (internal and composition-hidden omitted unless --include-internal)",
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
        help = "Include internal and composition-hidden synced actions in listings (hidden by default)",
    ).flag()

    private val json by option(
        "--json",
        help = "Emit machine-readable alphabet JSON for the scope (external, source-internal, composition-hidden sync groups)",
    ).flag()

    private val input by argument(
        help = "Jul source file to analyze",
    ).path(mustExist = true, canBeFile = true)

    override fun run() {
        val anyView = showTree || showActions || actionsDetail || showProcs || procsDetail || json
        val options = AnalyzeOptions(
            scopeNames = scopeNames,
            showTree = if (anyView) showTree else true,
            showActions = showActions,
            actionsDetail = actionsDetail,
            showProcs = showProcs,
            procsDetail = procsDetail,
            actionNames = actionNames,
            actionRegex = actionRegex,
            includeInternal = includeInternal,
            scopeIntersect = scopeIntersect,
            scopeMutual = scopeMutual,
            json = json,
        )
        analyzeJulFile(input, options, libraryPaths)
    }
}

class CheckCommand : CliktCommand(name = "check") {
    override fun help(context: Context) =
        "Type-check without compiling; emit diagnostics for IDE squiggles"

    override fun helpEpilog(context: Context) = """
        Examples:
          julayc check path/to/file.jul
          julayc check --json path/to/file.jul
    """.trimIndent()

    private val libraryPaths by option(
        "-L",
        help = "Add a directory to the module search path",
    ).path(mustExist = true, canBeDir = true).multiple()

    private val allowUnindexedSpec by option(
        "--allow-unindexed-spec",
        help = "Warn instead of error when a multi-instance proc appears unindexed in a spec",
    ).flag()

    private val json by option(
        "--json",
        help = "Emit machine-readable diagnostics JSON (errors and warnings)",
    ).flag()

    private val input by argument(
        help = "Jul source file to check",
    ).path(mustExist = true, canBeFile = true)

    override fun run() {
        checkJulFileAndExit(
            source = input,
            extraLibraryPaths = libraryPaths,
            asJson = json,
            allowUnindexedSpec = allowUnindexedSpec,
        )
    }
}

fun main(args: Array<String>) = Julayc().main(args)
