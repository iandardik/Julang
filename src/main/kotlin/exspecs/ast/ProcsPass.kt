package exspecs.ast

data class ProcDecl(
    val name : String,
    val components : List<ProcDecl>,
    val type : ProcDeclType
) {
    fun allProcNames() : Set<String> {
        return if (components.isEmpty()) {
            setOf(name)
        } else {
            components.flatMap { it.allProcNames() }.toSet()
        }
    }
}

enum class ProcDeclType {
    Proc, Program, Spec
}