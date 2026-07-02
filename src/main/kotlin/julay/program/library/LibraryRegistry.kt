package julay.program.library

const val JULAYLIB_MODULE = "julaylib"

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

    fun resolve(module: String, symbol: String): JulLibrary? {
        if (!isJulaylibModule(module)) return null
        return byJulName[symbol]
    }

    fun resolveQualified(parts: List<String>): JulLibrary? {
        if (parts.size < 2 || !isJulaylibModule(parts.first())) return null
        return byJulName[parts.last()]
    }

    fun staticInfoCodegenExpr(julName: String) = byJulName[julName]!!.staticInfoCodegenExpr()
    fun actionDecls(julName: String) = byJulName[julName]!!.actionDecls
}
