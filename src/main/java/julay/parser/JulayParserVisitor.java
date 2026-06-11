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
	 * Visit a parse tree produced by {@link JulayParser#decl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDecl(JulayParser.DeclContext ctx);
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
	 * Visit a parse tree produced by {@link JulayParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpr(JulayParser.ExprContext ctx);
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
	 * Visit a parse tree produced by {@link JulayParser#struct_literal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStruct_literal(JulayParser.Struct_literalContext ctx);
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