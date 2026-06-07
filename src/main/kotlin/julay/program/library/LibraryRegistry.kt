package julay.program.library

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
    fun staticInfoCodegenExpr(julName: String) = byJulName[julName]!!.staticInfoCodegenExpr()
    fun actionDecls(julName: String) = byJulName[julName]!!.actionDecls
}
