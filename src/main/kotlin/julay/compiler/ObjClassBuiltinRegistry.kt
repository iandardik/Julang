package julay.compiler

import julay.program.type.ObjClassType
import julay.program.library.httpClientRequestType
import julay.program.library.httpClientResponseType
import julay.program.library.httpServerRequestType
import julay.program.library.httpServerResponseType

/**
 * Kotlin-defined o-classes that Julay can name without a Julay `o-class` declaration.
 * Codegen must not re-emit data classes / converters for these; they live in julay.program.library.
 */
object ObjClassBuiltinRegistry {
    private val builtins: Map<String, ObjClassType> = mapOf(
        httpServerRequestType.name to httpServerRequestType,
        httpServerResponseType.name to httpServerResponseType,
        httpClientRequestType.name to httpClientRequestType,
        httpClientResponseType.name to httpClientResponseType,
    )

    fun lookup(name: String): ObjClassType? = builtins[name]

    fun isBuiltin(name: String): Boolean = name in builtins

    val all: Collection<ObjClassType> get() = builtins.values
}
