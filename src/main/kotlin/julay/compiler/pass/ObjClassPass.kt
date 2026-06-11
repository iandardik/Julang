package julay.compiler.pass

import julay.compiler.*
import julay.compiler.ast.*
import julay.compiler.decl.*

fun ASTNode.objClassPass(): List<RawObjClassDecl> = when (this) {
    is ObjClassNode -> listOf(
        RawObjClassDecl(objClassNodeName(), objClassFields().map { it.fieldName to it.typeName }, programLocation()),
    )
    else -> children.flatMap { it.objClassPass() }
}

fun RootNode.resolvedObjClassDecls(): List<ObjClassDecl> = resolvedObjClassRegistry().decls

fun RootNode.resolvedObjClassRegistry(): ObjClassRegistry =
    ObjClassRegistry.build(declNodes().flatMap { it.objClassPass() })

fun RootNode.flattenObjClassPass(registry: ObjClassRegistry): RootNode = flattenObjClassPass(this, registry)
