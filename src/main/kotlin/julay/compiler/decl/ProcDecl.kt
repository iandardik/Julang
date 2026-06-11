package julay.compiler.decl

data class ProcDecl(
    val name : String,
    val components : List<ProcDecl>,
    val type : ProcDeclType
) {
    fun allProcNames(procDecls : List<ProcDecl>) : Set<String> {
        val procDeclNames = procDecls.associateBy { it.name }
        val subComponents = components.map { cmpt ->
            procDeclNames.getOrElse(cmpt.name) { cmpt }
        }
        return if (subComponents.isEmpty()) {
            setOf(name)
        } else {
            subComponents.flatMap { it.allProcNames(procDecls) }.toSet()
        }
    }
}

enum class ProcDeclType {
    Proc, Program, Spec
}
