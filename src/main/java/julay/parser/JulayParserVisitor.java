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
	 * Visit a parse tree produced by {@link JulayParser#name_id}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitName_id(JulayParser.Name_idContext ctx);
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
	 * Visit a parse tree produced by {@link JulayParser#typeArgs}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeArgs(JulayParser.TypeArgsContext ctx);
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
	 * Visit a parse tree produced by {@link JulayParser#procfun_decl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProcfun_decl(JulayParser.Procfun_declContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#procfun_body}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProcfun_body(JulayParser.Procfun_bodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#api_decl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitApi_decl(JulayParser.Api_declContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#api_call_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitApi_call_list(JulayParser.Api_call_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#proc}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProc(JulayParser.ProcContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#obj}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitObj(JulayParser.ObjContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#sort_decl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSort_decl(JulayParser.Sort_declContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#compile_decl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCompile_decl(JulayParser.Compile_declContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#spec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSpec(JulayParser.SpecContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#ag_spec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAg_spec(JulayParser.Ag_specContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#assume_expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssume_expr(JulayParser.Assume_exprContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#system_expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSystem_expr(JulayParser.System_exprContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#with_expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWith_expr(JulayParser.With_exprContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#system_atom}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSystem_atom(JulayParser.System_atomContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#create_index_item}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_index_item(JulayParser.Create_index_itemContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#global_decl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGlobal_decl(JulayParser.Global_declContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#init_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInit_clause(JulayParser.Init_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#system_primary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSystem_primary(JulayParser.System_primaryContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#system_leaf}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSystem_leaf(JulayParser.System_leafContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#invariant_decl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInvariant_decl(JulayParser.Invariant_declContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#proc_body}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProc_body(JulayParser.Proc_bodyContext ctx);
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
	 * Visit a parse tree produced by {@link JulayParser#constructor_body}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstructor_body(JulayParser.Constructor_bodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#action_body}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAction_body(JulayParser.Action_bodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#return_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReturn_clause(JulayParser.Return_clauseContext ctx);
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
	 * Visit a parse tree produced by {@link JulayParser#error_arm}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitError_arm(JulayParser.Error_armContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#var_transit}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVar_transit(JulayParser.Var_transitContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#before}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBefore(JulayParser.BeforeContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#after}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAfter(JulayParser.AfterContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#call_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCall_stmt(JulayParser.Call_stmtContext ctx);
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
	 * Visit a parse tree produced by {@link JulayParser#proc_expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProc_expr(JulayParser.Proc_exprContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#literal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLiteral(JulayParser.LiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#collection_literal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCollection_literal(JulayParser.Collection_literalContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#list_literal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitList_literal(JulayParser.List_literalContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#set_literal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSet_literal(JulayParser.Set_literalContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#map_literal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMap_literal(JulayParser.Map_literalContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#map_entry}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMap_entry(JulayParser.Map_entryContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#index_expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIndex_expr(JulayParser.Index_exprContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#method_prop_expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMethod_prop_expr(JulayParser.Method_prop_exprContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#method_call}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMethod_call(JulayParser.Method_callContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#fun_call}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFun_call(JulayParser.Fun_callContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#call_arg}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCall_arg(JulayParser.Call_argContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#lambda_expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLambda_expr(JulayParser.Lambda_exprContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#obj_literal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitObj_literal(JulayParser.Obj_literalContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#obj_field_assign}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitObj_field_assign(JulayParser.Obj_field_assignContext ctx);
	/**
	 * Visit a parse tree produced by {@link JulayParser#field_access}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitField_access(JulayParser.Field_accessContext ctx);
}