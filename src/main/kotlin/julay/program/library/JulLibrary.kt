package julay.program.library

import julay.compiler.decl.ActionDecl
import julay.program.StaticInfo

interface JulLibrary : StaticInfo {
    val julName: String
    val actionDecls: List<ActionDecl>
}

fun JulLibrary.staticInfoCodegenExpr(): String {
    val clazz = this::class.java.enclosingClass ?: this::class.java
    return "${clazz.simpleName}.staticInfo()"
}
