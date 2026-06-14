package julay.program.library

const val JULAYLIB_MODULE = "julaylib"

object LibraryRegistry {
    val all: List<JulLibrary> = listOf(
        PrintlnTS,
        ReadlnTS,
        JulHttpServer,
        JulHttpClient,
        TimerTS,
        ExitSystemTS,
    )

    val julNames: Set<String> = all.map { it.julName }.toSet()
    private val byJulName: Map<String, JulLibrary> = all.associateBy { it.julName }

    fun isLibrary(name: String) = name in byJulName

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
