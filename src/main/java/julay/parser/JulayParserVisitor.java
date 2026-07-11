package julay.parser;
// Generated from JulayParser.g4 by ANTLR 4.13.2
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link JulayParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface JulayParserVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link JulayParser#root}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRoot(JulayParser.RootContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#import_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitImport_stmt(JulayParser.Import_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#qualified_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQualified_name(JulayParser.Qualified_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#qual_segment}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQual_segment(JulayParser.Qual_segmentContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#decl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDecl(JulayParser.DeclContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#typeExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeExpr(JulayParser.TypeExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#typeParams}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeParams(JulayParser.TypeParamsContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#fun_decl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFun_decl(JulayParser.Fun_declContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#pclass}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPclass(JulayParser.PclassContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#oclass}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOclass(JulayParser.OclassContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#proc}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProc(JulayParser.ProcContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#program}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProgram(JulayParser.ProgramContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#spec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSpec(JulayParser.SpecContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#pclass_body}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPclass_body(JulayParser.Pclass_bodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#field}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitField(JulayParser.FieldContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#var}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVar(JulayParser.VarContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#constructor}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstructor(JulayParser.ConstructorContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#transition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTransition(JulayParser.TransitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#args}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArgs(JulayParser.ArgsContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#arg}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArg(JulayParser.ArgContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#action_body}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAction_body(JulayParser.Action_bodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#guard}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGuard(JulayParser.GuardContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#transit}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTransit(JulayParser.TransitContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#error}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitError(JulayParser.ErrorContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#var_transit}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVar_transit(JulayParser.Var_transitContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#effect}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEffect(JulayParser.EffectContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#effect_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEffect_stmt(JulayParser.Effect_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#effect_call}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEffect_call(JulayParser.Effect_callContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpr(JulayParser.ExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#when_subject_arm}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWhen_subject_arm(JulayParser.When_subject_armContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#when_guard_arm}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWhen_guard_arm(JulayParser.When_guard_armContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#when_pattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWhen_pattern(JulayParser.When_patternContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#when_literal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWhen_literal(JulayParser.When_literalContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#proc_expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProc_expr(JulayParser.Proc_exprContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#value}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitValue(JulayParser.ValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#list_literal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitList_literal(JulayParser.List_literalContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#index_expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIndex_expr(JulayParser.Index_exprContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#fun_call}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFun_call(JulayParser.Fun_callContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#oclass_literal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOclass_literal(JulayParser.Oclass_literalContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#struct_field_assign}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStruct_field_assign(JulayParser.Struct_field_assignContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#field_access}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitField_access(JulayParser.Field_accessContext ctx);
}