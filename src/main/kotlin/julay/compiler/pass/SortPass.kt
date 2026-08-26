package julay.compiler.pass

import julay.compiler.ast.RootNode
import julay.program.type.DomainType
import julay.program.type.Type
import julay.program.type.domainOnlyError
import julay.program.type.isDirectUninterpretedDomain

/** @deprecated Use [DomainPassResult]. */
@Deprecated("Use DomainPassResult", ReplaceWith("DomainPassResult"))
typealias SortPassResult = DomainPassResult

val DomainPassResult.sorts: Map<String, DomainType> get() = domains

/** @deprecated Use [RootNode.domainPass]. */
@Deprecated("Use domainPass", ReplaceWith("domainPass()"))
fun RootNode.sortPass(): DomainPassResult = domainPass(
    declNodes().filterIsInstance<julay.compiler.ast.ObjClassNode>().map { it.name() }.toSet(),
)

/** @deprecated Use [CompilationUnit.collectDomains]. */
@Deprecated("Use collectDomains", ReplaceWith("collectDomains()"))
fun julay.compiler.CompilationUnit.collectSorts(): DomainPassResult = collectDomains()

/** @deprecated Use [domainOnlyError]. */
@Deprecated("Use domainOnlyError", ReplaceWith("domainOnlyError(type, loc)"))
fun sortDomainOnlyError(type: Type, loc: julay.compiler.ProgramLoc) =
    domainOnlyError(type, loc)

/** @deprecated Use [Type.isDirectUninterpretedDomain]. */
@Deprecated("Use isDirectUninterpretedDomain", ReplaceWith("isDirectUninterpretedDomain()"))
fun Type.isDirectSortDomain(): Boolean = isDirectUninterpretedDomain()
