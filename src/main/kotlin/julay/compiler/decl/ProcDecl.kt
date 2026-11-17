package julay.compiler.decl

data class ProcDecl(
    val name: String,
    val components: List<ProcDecl>,
    val type: ProcDeclType,
) {
    /** Distinct leaf proc-class names under this assembly (order not significant). */
    fun allProcNames(procDecls: List<ProcDecl>): Set<String> =
        allLeafOccurrences(procDecls).map { it.pclassName }.toSet()

    /**
     * Leaf occurrences under this assembly, expanding named procs independently
     * (`M || M` yields two expansions). Delegates to [julay.compiler.pass.collectLeafOccurrences].
     */
    fun allLeafOccurrences(procDecls: List<ProcDecl>): List<julay.compiler.pass.LeafOccurrence> =
        julay.compiler.pass.collectLeafOccurrences(this, procDecls)
}

enum class ProcDeclType {
    Proc, Spec
}
