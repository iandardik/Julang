package julay.parser;
// Generated from JulayParser.g4 by ANTLR 4.13.2
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link JulayParser}.
 */
public interface JulayParserListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link JulayParser#root}.
	 * @param ctx the parse tree
	 */
	void enterRoot(JulayParser.RootContext ctx);
	/**
	 * Exit a parse tree produced by {@link JulayParser#root}.
	 * @param ctx the parse tree
	 */
	void exitRoot(JulayParser.RootContext ctx);
	/**
	 * Enter a parse tree produced by {@link JulayParser#import_stmt}.
	 * @param ctx the parse tree
	 */
	void enterImport_stmt(JulayParser.Import_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link JulayParser#import_stmt}.
	 * @param ctx the parse tree
	 */
	void exitImport_stmt(JulayParser.Import_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link JulayParser#qualified_name}.
	 * @param ctx the parse tree
	 */
	void enterQualified_name(JulayParser.Qualified_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link JulayParser#qualified_name}.
	 * @param ctx the parse tree
	 */
	void exitQualified_name(JulayParser.Qualified_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link JulayParser#decl}.
	 * @param ctx the parse tree
	 */
	void enterDecl(JulayParser.DeclContext ctx);
	/**
	 * Exit a parse tree produced by {@link JulayParser#decl}.
	 * @param ctx the parse tree
	 */
	void exitDecl(JulayParser.DeclContext ctx);
	/**
	 * Enter a parse tree produced by {@link JulayParser#pclass}.
	 * @param ctx the parse tree
	 */
	void enterPclass(JulayParser.PclassContext ctx);
	/**
	 * Exit a parse tree produced by {@link JulayParser#pclass}.
	 * @param ctx the parse tree
	 */
	void exitPclass(JulayParser.PclassContext ctx);
	/**
	 * Enter a parse tree produced by {@link JulayParser#oclass}.
	 * @param ctx the parse tree
	 */
	void enterOclass(JulayParser.OclassContext ctx);
	/**
	 * Exit a parse tree produced by {@link JulayParser#oclass}.
	 * @param ctx the parse tree
	 */
	void exitOclass(JulayParser.OclassContext ctx);
	/**
	 * Enter a parse tree produced by {@link JulayParser#proc}.
	 * @param ctx the parse tree
	 */
	void enterProc(JulayParser.ProcContext ctx);
	/**
	 * Exit a parse tree produced by {@link JulayParser#proc}.
	 * @param ctx the parse tree
	 */
	void exitProc(JulayParser.ProcContext ctx);
	/**
	 * Enter a parse tree produced by {@link JulayParser#program}.
	 * @param ctx the parse tree
	 */
	void enterProgram(JulayParser.ProgramContext ctx);
	/**
	 * Exit a parse tree produced by {@link JulayParser#program}.
	 * @param ctx the parse tree
	 */
	void exitProgram(JulayParser.ProgramContext ctx);
	/**
	 * Enter a parse tree produced by {@link JulayParser#spec}.
	 * @param ctx the parse tree
	 */
	void enterSpec(JulayParser.SpecContext ctx);
	/**
	 * Exit a parse tree produced by {@link JulayParser#spec}.
	 * @param ctx the parse tree
	 */
	void exitSpec(JulayParser.SpecContext ctx);
	/**
	 * Enter a parse tree produced by {@link JulayParser#pclass_body}.
	 * @param ctx the parse tree
	 */
	void enterPclass_body(JulayParser.Pclass_bodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link JulayParser#pclass_body}.
	 * @param ctx the parse tree
	 */
	void exitPclass_body(JulayParser.Pclass_bodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link JulayParser#field}.
	 * @param ctx the parse tree
	 */
	void enterField(JulayParser.FieldContext ctx);
	/**
	 * Exit a parse tree produced by {@link JulayParser#field}.
	 * @param ctx the parse tree
	 */
	void exitField(JulayParser.FieldContext ctx);
	/**
	 * Enter a parse tree produced by {@link JulayParser#var}.
	 * @param ctx the parse tree
	 */
	void enterVar(JulayParser.VarContext ctx);
	/**
	 * Exit a parse tree produced by {@link JulayParser#var}.
	 * @param ctx the parse tree
	 */
	void exitVar(JulayParser.VarContext ctx);
	/**
	 * Enter a parse tree produced by {@link JulayParser#constructor}.
	 * @param ctx the parse tree
	 */
	void enterConstructor(JulayParser.ConstructorContext ctx);
	/**
	 * Exit a parse tree produced by {@link JulayParser#constructor}.
	 * @param ctx the parse tree
	 */
	void exitConstructor(JulayParser.ConstructorContext ctx);
	/**
	 * Enter a parse tree produced by {@link JulayParser#transition}.
	 * @param ctx the parse tree
	 */
	void enterTransition(JulayParser.TransitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link JulayParser#transition}.
	 * @param ctx the parse tree
	 */
	void exitTransition(JulayParser.TransitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link JulayParser#args}.
	 * @param ctx the parse tree
	 */
	void enterArgs(JulayParser.ArgsContext ctx);
	/**
	 * Exit a parse tree produced by {@link JulayParser#args}.
	 * @param ctx the parse tree
	 */
	void exitArgs(JulayParser.ArgsContext ctx);
	/**
	 * Enter a parse tree produced by {@link JulayParser#arg}.
	 * @param ctx the parse tree
	 */
	void enterArg(JulayParser.ArgContext ctx);
	/**
	 * Exit a parse tree produced by {@link JulayParser#arg}.
	 * @param ctx the parse tree
	 */
	void exitArg(JulayParser.ArgContext ctx);
	/**
	 * Enter a parse tree produced by {@link JulayParser#action_body}.
	 * @param ctx the parse tree
	 */
	void enterAction_body(JulayParser.Action_bodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link JulayParser#action_body}.
	 * @param ctx the parse tree
	 */
	void exitAction_body(JulayParser.Action_bodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link JulayParser#guard}.
	 * @param ctx the parse tree
	 */
	void enterGuard(JulayParser.GuardContext ctx);
	/**
	 * Exit a parse tree produced by {@link JulayParser#guard}.
	 * @param ctx the parse tree
	 */
	void exitGuard(JulayParser.GuardContext ctx);
	/**
	 * Enter a parse tree produced by {@link JulayParser#transit}.
	 * @param ctx the parse tree
	 */
	void enterTransit(JulayParser.TransitContext ctx);
	/**
	 * Exit a parse tree produced by {@link JulayParser#transit}.
	 * @param ctx the parse tree
	 */
	void exitTransit(JulayParser.TransitContext ctx);
	/**
	 * Enter a parse tree produced by {@link JulayParser#error}.
	 * @param ctx the parse tree
	 */
	void enterError(JulayParser.ErrorContext ctx);
	/**
	 * Exit a parse tree produced by {@link JulayParser#error}.
	 * @param ctx the parse tree
	 */
	void exitError(JulayParser.ErrorContext ctx);
	/**
	 * Enter a parse tree produced by {@link JulayParser#var_transit}.
	 * @param ctx the parse tree
	 */
	void enterVar_transit(JulayParser.Var_transitContext ctx);
	/**
	 * Exit a parse tree produced by {@link JulayParser#var_transit}.
	 * @param ctx the parse tree
	 */
	void exitVar_transit(JulayParser.Var_transitContext ctx);
	/**
	 * Enter a parse tree produced by {@link JulayParser#effect}.
	 * @param ctx the parse tree
	 */
	void enterEffect(JulayParser.EffectContext ctx);
	/**
	 * Exit a parse tree produced by {@link JulayParser#effect}.
	 * @param ctx the parse tree
	 */
	void exitEffect(JulayParser.EffectContext ctx);
	/**
	 * Enter a parse tree produced by {@link JulayParser#effect_stmt}.
	 * @param ctx the parse tree
	 */
	void enterEffect_stmt(JulayParser.Effect_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link JulayParser#effect_stmt}.
	 * @param ctx the parse tree
	 */
	void exitEffect_stmt(JulayParser.Effect_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link JulayParser#effect_call}.
	 * @param ctx the parse tree
	 */
	void enterEffect_call(JulayParser.Effect_callContext ctx);
	/**
	 * Exit a parse tree produced by {@link JulayParser#effect_call}.
	 * @param ctx the parse tree
	 */
	void exitEffect_call(JulayParser.Effect_callContext ctx);
	/**
	 * Enter a parse tree produced by {@link JulayParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterExpr(JulayParser.ExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link JulayParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitExpr(JulayParser.ExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link JulayParser#when_subject_arm}.
	 * @param ctx the parse tree
	 */
	void enterWhen_subject_arm(JulayParser.When_subject_armContext ctx);
	/**
	 * Exit a parse tree produced by {@link JulayParser#when_subject_arm}.
	 * @param ctx the parse tree
	 */
	void exitWhen_subject_arm(JulayParser.When_subject_armContext ctx);
	/**
	 * Enter a parse tree produced by {@link JulayParser#when_guard_arm}.
	 * @param ctx the parse tree
	 */
	void enterWhen_guard_arm(JulayParser.When_guard_armContext ctx);
	/**
	 * Exit a parse tree produced by {@link JulayParser#when_guard_arm}.
	 * @param ctx the parse tree
	 */
	void exitWhen_guard_arm(JulayParser.When_guard_armContext ctx);
	/**
	 * Enter a parse tree produced by {@link JulayParser#when_literal}.
	 * @param ctx the parse tree
	 */
	void enterWhen_literal(JulayParser.When_literalContext ctx);
	/**
	 * Exit a parse tree produced by {@link JulayParser#when_literal}.
	 * @param ctx the parse tree
	 */
	void exitWhen_literal(JulayParser.When_literalContext ctx);
	/**
	 * Enter a parse tree produced by {@link JulayParser#proc_expr}.
	 * @param ctx the parse tree
	 */
	void enterProc_expr(JulayParser.Proc_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link JulayParser#proc_expr}.
	 * @param ctx the parse tree
	 */
	void exitProc_expr(JulayParser.Proc_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link JulayParser#value}.
	 * @param ctx the parse tree
	 */
	void enterValue(JulayParser.ValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link JulayParser#value}.
	 * @param ctx the parse tree
	 */
	void exitValue(JulayParser.ValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link JulayParser#struct_literal}.
	 * @param ctx the parse tree
	 */
	void enterStruct_literal(JulayParser.Struct_literalContext ctx);
	/**
	 * Exit a parse tree produced by {@link JulayParser#struct_literal}.
	 * @param ctx the parse tree
	 */
	void exitStruct_literal(JulayParser.Struct_literalContext ctx);
	/**
	 * Enter a parse tree produced by {@link JulayParser#struct_field_assign}.
	 * @param ctx the parse tree
	 */
	void enterStruct_field_assign(JulayParser.Struct_field_assignContext ctx);
	/**
	 * Exit a parse tree produced by {@link JulayParser#struct_field_assign}.
	 * @param ctx the parse tree
	 */
	void exitStruct_field_assign(JulayParser.Struct_field_assignContext ctx);
	/**
	 * Enter a parse tree produced by {@link JulayParser#field_access}.
	 * @param ctx the parse tree
	 */
	void enterField_access(JulayParser.Field_accessContext ctx);
	/**
	 * Exit a parse tree produced by {@link JulayParser#field_access}.
	 * @param ctx the parse tree
	 */
	void exitField_access(JulayParser.Field_accessContext ctx);
}