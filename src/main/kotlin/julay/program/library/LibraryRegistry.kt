package julay.program.library

const val JULAYLIB_MODULE = "julaylib"
const val JULAYLIB_PCLASS = "pclass"
const val JULAYLIB_FUN = "fun"

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

    fun isJulaylibModule(module: String) = module == JULAYLIB_MODULE

    /** True for imports like julaylib.pclass.Println or julaylib.pclass.HttpServer. */
    fun isPclassImport(parts: List<String>): Boolean =
        parts.size == 3 && parts[0] == JULAYLIB_MODULE && parts[1] == JULAYLIB_PCLASS

    fun pclassModulePath(name: String): String =
        listOf(JULAYLIB_MODULE, JULAYLIB_PCLASS, name).joinToString(".")

    fun resolve(module: String, symbol: String): JulLibrary? {
        if (!isJulaylibModule(module)) return null
        return byJulName[symbol]
    }

    fun resolveQualified(parts: List<String>): JulLibrary? {
        if (isPclassImport(parts) && isKotlinLibrary(parts[2])) {
            return byJulName[parts[2]]
        }
        // Legacy two-part form is no longer supported for resolution of Kotlin libs.
        return null
    }

    fun staticInfoCodegenExpr(julName: String) = byJulName[julName]!!.staticInfoCodegenExpr()
    fun actionDecls(julName: String) = byJulName[julName]!!.actionDecls
    fun staticInfo(julName: String) = byJulName[julName]!!.staticInfo()
}
