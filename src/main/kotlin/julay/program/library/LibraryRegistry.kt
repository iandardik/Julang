package julay.program.library

const val JULAY_MODULE = "julay"
const val JULAY_PROCLIB = "proclib"
const val JULAY_FUNLIB = "funlib"

object LibraryRegistry {
    val julayStdlibNames: Set<String> = setOf("Println", "ExitSystem", "Readln", "Timer")

    val kotlinLibraries: List<JulLibrary> = listOf(
        JulHttpServer,
        JulHttpClient,
    )

    val kotlinLibraryNames: Set<String> = kotlinLibraries.map { it.julName }.toSet()
    private val byJulName: Map<String, JulLibrary> = kotlinLibraries.associateBy { it.julName }

    fun isKotlinLibrary(name: String) = name in byJulName

    fun isJulayStdlib(name: String) = name in julayStdlibNames

    fun isKnownJulaylibSymbol(name: String) = isJulayStdlib(name) || isKotlinLibrary(name)

    fun isJulayModule(module: String) = module == JULAY_MODULE

    /** True for imports like julay.proclib.Println or julay.proclib.HttpServer. */
    fun isProclibImport(parts: List<String>): Boolean =
        parts.size == 3 && parts[0] == JULAY_MODULE && parts[1] == JULAY_PROCLIB

    fun proclibModulePath(name: String): String =
        listOf(JULAY_MODULE, JULAY_PROCLIB, name).joinToString(".")

    fun resolve(module: String, symbol: String): JulLibrary? {
        if (!isJulayModule(module)) return null
        return byJulName[symbol]
    }

    fun resolveQualified(parts: List<String>): JulLibrary? {
        if (isProclibImport(parts) && isKotlinLibrary(parts[2])) {
            return byJulName[parts[2]]
        }
        // Legacy two-part form is no longer supported for resolution of Kotlin libs.
        return null
    }

    fun staticInfoCodegenExpr(julName: String) = byJulName[julName]!!.staticInfoCodegenExpr()
    fun actionDecls(julName: String) = byJulName[julName]!!.actionDecls
    fun staticInfo(julName: String) = byJulName[julName]!!.staticInfo()
}
