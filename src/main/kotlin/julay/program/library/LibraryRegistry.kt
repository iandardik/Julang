package julay.program.library

const val JULAY_MODULE = "julay"
const val JULAY_PROCLIB = "proclib"
const val JULAY_FUNLIB = "funlib"
const val JULAY_OPTIONAL = "optional"

object LibraryRegistry {
    /** Former proclib modules (Timer / HttpServer) removed; use builtins instead. */
    val julayStdlibNames: Set<String> = emptySet()

    /** `.jul` funlib modules under stdlib/julay/funlib/ (may export multiple funs). */
    val julayFunlibJulModules: Set<String> = setOf("math", "http")

    /** Former Kotlin-native libraries (HttpServer) removed; Go runtime provides ServeHttp. */
    val kotlinLibraryNames: Set<String> = emptySet()

    fun isKotlinLibrary(name: String) = name in kotlinLibraryNames

    fun isJulayStdlib(name: String) = name in julayStdlibNames

    fun isKnownJulaylibSymbol(name: String) = isJulayStdlib(name) || isKotlinLibrary(name)

    fun isJulayModule(module: String) = module == JULAY_MODULE

    /** True for imports like julay.proclib.* (legacy path; Timer/HttpServer removed). */
    fun isProclibImport(parts: List<String>): Boolean =
        parts.size == 3 && parts[0] == JULAY_MODULE && parts[1] == JULAY_PROCLIB

    /** True for imports like julay.funlib.max or julay.funlib.println. */
    fun isFunlibImport(parts: List<String>): Boolean =
        parts.size == 3 && parts[0] == JULAY_MODULE && parts[1] == JULAY_FUNLIB

    /** True for imports like julay.optional.Optional / some / none. */
    fun isOptionalImport(parts: List<String>): Boolean =
        parts.size == 3 && parts[0] == JULAY_MODULE && parts[1] == JULAY_OPTIONAL

    fun proclibModulePath(name: String): String =
        listOf(JULAY_MODULE, JULAY_PROCLIB, name).joinToString(".")

    fun funlibModulePath(name: String): String =
        listOf(JULAY_MODULE, JULAY_FUNLIB, name).joinToString(".")
}
