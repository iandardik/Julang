package julay.compiler.pass

import julay.compiler.*
import julay.compiler.ast.*
import julay.compiler.decl.*

fun ASTNode.objClassPass(): List<RawObjClassDecl> = when (this) {
    is ObjClassNode -> listOf(
        RawObjClassDecl(
            objClassNodeName(),
            objClassTypeParams(),
            objClassFields().map { it.fieldName to it.typeExpr },
            programLocation(),
        ),
    )
    else -> children.flatMap { it.objClassPass() }
}

fun RootNode.resolvedObjClassDecls(): List<ObjClassDecl> =
    cachedObjClassRegistry()?.concreteDecls()
        ?: resolvedObjClassRegistry().concreteDecls()

fun RootNode.resolvedObjClassRegistry(): ObjClassRegistry =
    cachedObjClassRegistry()
        ?: ObjClassRegistry.build(declNodes().flatMap { it.objClassPass() })
