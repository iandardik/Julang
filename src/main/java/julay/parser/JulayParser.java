package julay.parser;
// Generated from JulayParser.g4 by ANTLR 4.13.2
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue", "this-escape"})
public class JulayParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.13.2", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		COMMA=1, DOT=2, COLON=3, LPAREN=4, RPAREN=5, LBRACK=6, RBRACK=7, LCURLY=8, 
		RCURLY=9, PARALLEL=10, TRUE=11, FALSE=12, AND=13, MODELS=14, OR=15, PROCFUN_ARROW=16, 
		NOT=17, TIMES=18, DIV=19, MOD=20, PLUS=21, MINUS=22, LT=23, LTE=24, GT=25, 
		GTE=26, EQ=27, NEQ=28, NIN=29, ASGN_EQ=30, IMPLIES=31, IFF=32, IF=33, 
		ELSE=34, LET=35, WHEN=36, IN=37, TO=38, ARROW=39, IMPORT=40, EXPORT=41, 
		LISTOF=42, SETOF=43, MAPOF=44, TYPE=45, STATIC=46, PROC=47, COMPILE=48, 
		SPEC=49, INVARIANT=50, FLUENT=51, ON=52, INITIALLY=53, FORALL=54, EXISTS=55, 
		VAR=56, CONST=57, CONSTRUCTOR=58, TRANSITION=59, INTERNAL=60, TIMER=61, 
		SERVICE=62, CLIENT=63, GUARD=64, TRANSIT=65, ERROR=66, BEFORE=67, AFTER=68, 
		FUN=69, RETURN=70, ALSO=71, WITH=72, THIS=73, GLOBAL=74, INIT=75, REAL=76, 
		INT=77, ID=78, STRING=79, WS=80, COMMENT=81, LINE_COMMENT=82;
	public static final int
		RULE_root = 0, RULE_import_stmt = 1, RULE_qualified_name = 2, RULE_name_id = 3, 
		RULE_decl = 4, RULE_typeExpr = 5, RULE_typeDomain = 6, RULE_typeAtom = 7, 
		RULE_typeArgs = 8, RULE_typeParams = 9, RULE_fun_decl = 10, RULE_proc = 11, 
		RULE_type_decl = 12, RULE_type_model = 13, RULE_fun_model = 14, RULE_compile_decl = 15, 
		RULE_spec = 16, RULE_leaf_spec_item = 17, RULE_ag_spec = 18, RULE_assume_expr = 19, 
		RULE_guarantee_expr = 20, RULE_system_expr = 21, RULE_with_expr = 22, 
		RULE_system_atom = 23, RULE_create_index_item = 24, RULE_global_decl = 25, 
		RULE_init_clause = 26, RULE_system_primary = 27, RULE_system_leaf = 28, 
		RULE_invariant_decl = 29, RULE_fluent_decl = 30, RULE_fluent_params = 31, 
		RULE_fluent_param = 32, RULE_fluent_item = 33, RULE_on_arg_list = 34, 
		RULE_on_arg = 35, RULE_fluent_target = 36, RULE_proc_body = 37, RULE_field = 38, 
		RULE_var = 39, RULE_constructor = 40, RULE_transition = 41, RULE_return_action = 42, 
		RULE_return_body = 43, RULE_args = 44, RULE_arg = 45, RULE_constructor_body = 46, 
		RULE_action_body = 47, RULE_guard = 48, RULE_transit = 49, RULE_error = 50, 
		RULE_error_arm = 51, RULE_var_transit = 52, RULE_before = 53, RULE_after = 54, 
		RULE_call_stmt = 55, RULE_expr = 56, RULE_when_subject_arm = 57, RULE_when_guard_arm = 58, 
		RULE_when_pattern = 59, RULE_proc_expr = 60, RULE_handler_ref = 61, RULE_literal = 62, 
		RULE_collection_literal = 63, RULE_list_literal = 64, RULE_set_literal = 65, 
		RULE_map_literal = 66, RULE_map_entry = 67, RULE_index_expr = 68, RULE_method_prop_expr = 69, 
		RULE_method_call = 70, RULE_fun_call = 71, RULE_call_arg = 72, RULE_lambda_expr = 73, 
		RULE_record_literal = 74, RULE_record_field_assign = 75, RULE_field_access = 76;
	private static String[] makeRuleNames() {
		return new String[] {
			"root", "import_stmt", "qualified_name", "name_id", "decl", "typeExpr", 
			"typeDomain", "typeAtom", "typeArgs", "typeParams", "fun_decl", "proc", 
			"type_decl", "type_model", "fun_model", "compile_decl", "spec", "leaf_spec_item", 
			"ag_spec", "assume_expr", "guarantee_expr", "system_expr", "with_expr", 
			"system_atom", "create_index_item", "global_decl", "init_clause", "system_primary", 
			"system_leaf", "invariant_decl", "fluent_decl", "fluent_params", "fluent_param", 
			"fluent_item", "on_arg_list", "on_arg", "fluent_target", "proc_body", 
			"field", "var", "constructor", "transition", "return_action", "return_body", 
			"args", "arg", "constructor_body", "action_body", "guard", "transit", 
			"error", "error_arm", "var_transit", "before", "after", "call_stmt", 
			"expr", "when_subject_arm", "when_guard_arm", "when_pattern", "proc_expr", 
			"handler_ref", "literal", "collection_literal", "list_literal", "set_literal", 
			"map_literal", "map_entry", "index_expr", "method_prop_expr", "method_call", 
			"fun_call", "call_arg", "lambda_expr", "record_literal", "record_field_assign", 
			"field_access"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "','", "'.'", "':'", "'('", "')'", "'['", "']'", "'{'", "'}'", 
			"'||'", "'true'", "'false'", "'&'", "'|='", "'|'", "'~>'", "'~'", "'*'", 
			"'/'", "'%'", "'+'", "'-'", "'<'", "'<='", "'>'", "'>='", "'='", "'~='", 
			"'~in'", "':='", "'=>'", "'<=>'", "'if'", "'else'", "'let'", "'when'", 
			"'in'", "'to'", "'->'", "'import'", "'export'", "'listOf'", "'setOf'", 
			"'mapOf'", "'type'", "'static'", "'proc'", "'compile'", "'spec'", "'invariant'", 
			"'fluent'", "'on'", "'initially'", "'forall'", "'exists'", "'var'", "'const'", 
			"'constructor'", "'transition'", "'internal'", "'timer'", "'service'", 
			"'client'", "'guard'", "'transit'", "'error'", "'before'", "'after'", 
			"'fun'", "'return'", "'also'", "'with'", "'this'", "'global'", "'init'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "COMMA", "DOT", "COLON", "LPAREN", "RPAREN", "LBRACK", "RBRACK", 
			"LCURLY", "RCURLY", "PARALLEL", "TRUE", "FALSE", "AND", "MODELS", "OR", 
			"PROCFUN_ARROW", "NOT", "TIMES", "DIV", "MOD", "PLUS", "MINUS", "LT", 
			"LTE", "GT", "GTE", "EQ", "NEQ", "NIN", "ASGN_EQ", "IMPLIES", "IFF", 
			"IF", "ELSE", "LET", "WHEN", "IN", "TO", "ARROW", "IMPORT", "EXPORT", 
			"LISTOF", "SETOF", "MAPOF", "TYPE", "STATIC", "PROC", "COMPILE", "SPEC", 
			"INVARIANT", "FLUENT", "ON", "INITIALLY", "FORALL", "EXISTS", "VAR", 
			"CONST", "CONSTRUCTOR", "TRANSITION", "INTERNAL", "TIMER", "SERVICE", 
			"CLIENT", "GUARD", "TRANSIT", "ERROR", "BEFORE", "AFTER", "FUN", "RETURN", 
			"ALSO", "WITH", "THIS", "GLOBAL", "INIT", "REAL", "INT", "ID", "STRING", 
			"WS", "COMMENT", "LINE_COMMENT"
		};
	}
	private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}

	@Override
	public String getGrammarFileName() { return "JulayParser.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public JulayParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@SuppressWarnings("CheckReturnValue")
	public static class RootContext extends ParserRuleContext {
		public TerminalNode EOF() { return getToken(JulayParser.EOF, 0); }
		public List<Import_stmtContext> import_stmt() {
			return getRuleContexts(Import_stmtContext.class);
		}
		public Import_stmtContext import_stmt(int i) {
			return getRuleContext(Import_stmtContext.class,i);
		}
		public List<DeclContext> decl() {
			return getRuleContexts(DeclContext.class);
		}
		public DeclContext decl(int i) {
			return getRuleContext(DeclContext.class,i);
		}
		public RootContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_root; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterRoot(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitRoot(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitRoot(this);
			else return visitor.visitChildren(this);
		}
	}

	public final RootContext root() throws RecognitionException {
		RootContext _localctx = new RootContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_root);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(158);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (((((_la - 40)) & ~0x3f) == 0 && ((1L << (_la - 40)) & 536874979L) != 0)) {
				{
				setState(156);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case IMPORT:
					{
					setState(154);
					import_stmt();
					}
					break;
				case EXPORT:
				case TYPE:
				case STATIC:
				case PROC:
				case COMPILE:
				case SPEC:
				case INVARIANT:
				case FLUENT:
				case FUN:
					{
					setState(155);
					decl();
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				}
				setState(160);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(161);
			match(EOF);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Import_stmtContext extends ParserRuleContext {
		public TerminalNode IMPORT() { return getToken(JulayParser.IMPORT, 0); }
		public Qualified_nameContext qualified_name() {
			return getRuleContext(Qualified_nameContext.class,0);
		}
		public Import_stmtContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_import_stmt; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterImport_stmt(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitImport_stmt(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitImport_stmt(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Import_stmtContext import_stmt() throws RecognitionException {
		Import_stmtContext _localctx = new Import_stmtContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_import_stmt);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(163);
			match(IMPORT);
			setState(164);
			qualified_name();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Qualified_nameContext extends ParserRuleContext {
		public List<Name_idContext> name_id() {
			return getRuleContexts(Name_idContext.class);
		}
		public Name_idContext name_id(int i) {
			return getRuleContext(Name_idContext.class,i);
		}
		public List<TerminalNode> DOT() { return getTokens(JulayParser.DOT); }
		public TerminalNode DOT(int i) {
			return getToken(JulayParser.DOT, i);
		}
		public Qualified_nameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_qualified_name; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterQualified_name(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitQualified_name(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitQualified_name(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Qualified_nameContext qualified_name() throws RecognitionException {
		Qualified_nameContext _localctx = new Qualified_nameContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_qualified_name);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(166);
			name_id();
			setState(169); 
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					{
					setState(167);
					match(DOT);
					setState(168);
					name_id();
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(171); 
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,2,_ctx);
			} while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER );
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Name_idContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(JulayParser.ID, 0); }
		public TerminalNode INTERNAL() { return getToken(JulayParser.INTERNAL, 0); }
		public TerminalNode TIMER() { return getToken(JulayParser.TIMER, 0); }
		public TerminalNode SERVICE() { return getToken(JulayParser.SERVICE, 0); }
		public TerminalNode CLIENT() { return getToken(JulayParser.CLIENT, 0); }
		public TerminalNode STATIC() { return getToken(JulayParser.STATIC, 0); }
		public TerminalNode LISTOF() { return getToken(JulayParser.LISTOF, 0); }
		public TerminalNode SETOF() { return getToken(JulayParser.SETOF, 0); }
		public TerminalNode MAPOF() { return getToken(JulayParser.MAPOF, 0); }
		public Name_idContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_name_id; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterName_id(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitName_id(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitName_id(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Name_idContext name_id() throws RecognitionException {
		Name_idContext _localctx = new Name_idContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_name_id);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(173);
			_la = _input.LA(1);
			if ( !(((((_la - 42)) & ~0x3f) == 0 && ((1L << (_la - 42)) & 68723408919L) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class DeclContext extends ParserRuleContext {
		public ProcContext proc() {
			return getRuleContext(ProcContext.class,0);
		}
		public TerminalNode EXPORT() { return getToken(JulayParser.EXPORT, 0); }
		public Type_declContext type_decl() {
			return getRuleContext(Type_declContext.class,0);
		}
		public Compile_declContext compile_decl() {
			return getRuleContext(Compile_declContext.class,0);
		}
		public SpecContext spec() {
			return getRuleContext(SpecContext.class,0);
		}
		public Invariant_declContext invariant_decl() {
			return getRuleContext(Invariant_declContext.class,0);
		}
		public Fluent_declContext fluent_decl() {
			return getRuleContext(Fluent_declContext.class,0);
		}
		public Fun_declContext fun_decl() {
			return getRuleContext(Fun_declContext.class,0);
		}
		public DeclContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_decl; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterDecl(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitDecl(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitDecl(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DeclContext decl() throws RecognitionException {
		DeclContext _localctx = new DeclContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_decl);
		int _la;
		try {
			setState(200);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,9,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(176);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==EXPORT) {
					{
					setState(175);
					match(EXPORT);
					}
				}

				setState(178);
				proc();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(180);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==EXPORT) {
					{
					setState(179);
					match(EXPORT);
					}
				}

				setState(182);
				type_decl();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(183);
				compile_decl();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(185);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==EXPORT) {
					{
					setState(184);
					match(EXPORT);
					}
				}

				setState(187);
				spec();
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(189);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==EXPORT) {
					{
					setState(188);
					match(EXPORT);
					}
				}

				setState(191);
				invariant_decl();
				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(193);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==EXPORT) {
					{
					setState(192);
					match(EXPORT);
					}
				}

				setState(195);
				fluent_decl();
				}
				break;
			case 7:
				enterOuterAlt(_localctx, 7);
				{
				setState(197);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==EXPORT) {
					{
					setState(196);
					match(EXPORT);
					}
				}

				setState(199);
				fun_decl();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class TypeExprContext extends ParserRuleContext {
		public TypeDomainContext typeDomain() {
			return getRuleContext(TypeDomainContext.class,0);
		}
		public TypeExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typeExpr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterTypeExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitTypeExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitTypeExpr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TypeExprContext typeExpr() throws RecognitionException {
		TypeExprContext _localctx = new TypeExprContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_typeExpr);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(202);
			typeDomain();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class TypeDomainContext extends ParserRuleContext {
		public TerminalNode LPAREN() { return getToken(JulayParser.LPAREN, 0); }
		public List<TypeExprContext> typeExpr() {
			return getRuleContexts(TypeExprContext.class);
		}
		public TypeExprContext typeExpr(int i) {
			return getRuleContext(TypeExprContext.class,i);
		}
		public TerminalNode RPAREN() { return getToken(JulayParser.RPAREN, 0); }
		public List<TerminalNode> COMMA() { return getTokens(JulayParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(JulayParser.COMMA, i);
		}
		public TypeAtomContext typeAtom() {
			return getRuleContext(TypeAtomContext.class,0);
		}
		public TypeDomainContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typeDomain; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterTypeDomain(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitTypeDomain(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitTypeDomain(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TypeDomainContext typeDomain() throws RecognitionException {
		TypeDomainContext _localctx = new TypeDomainContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_typeDomain);
		int _la;
		try {
			setState(215);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,11,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(204);
				match(LPAREN);
				setState(205);
				typeExpr();
				setState(208); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(206);
					match(COMMA);
					setState(207);
					typeExpr();
					}
					}
					setState(210); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( _la==COMMA );
				setState(212);
				match(RPAREN);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(214);
				typeAtom();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class TypeAtomContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(JulayParser.ID, 0); }
		public TypeArgsContext typeArgs() {
			return getRuleContext(TypeArgsContext.class,0);
		}
		public TerminalNode LPAREN() { return getToken(JulayParser.LPAREN, 0); }
		public TypeExprContext typeExpr() {
			return getRuleContext(TypeExprContext.class,0);
		}
		public TerminalNode RPAREN() { return getToken(JulayParser.RPAREN, 0); }
		public TypeAtomContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typeAtom; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterTypeAtom(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitTypeAtom(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitTypeAtom(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TypeAtomContext typeAtom() throws RecognitionException {
		TypeAtomContext _localctx = new TypeAtomContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_typeAtom);
		int _la;
		try {
			setState(225);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ID:
				enterOuterAlt(_localctx, 1);
				{
				setState(217);
				match(ID);
				setState(219);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==LT) {
					{
					setState(218);
					typeArgs();
					}
				}

				}
				break;
			case LPAREN:
				enterOuterAlt(_localctx, 2);
				{
				setState(221);
				match(LPAREN);
				setState(222);
				typeExpr();
				setState(223);
				match(RPAREN);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class TypeArgsContext extends ParserRuleContext {
		public TerminalNode LT() { return getToken(JulayParser.LT, 0); }
		public List<TypeExprContext> typeExpr() {
			return getRuleContexts(TypeExprContext.class);
		}
		public TypeExprContext typeExpr(int i) {
			return getRuleContext(TypeExprContext.class,i);
		}
		public TerminalNode GT() { return getToken(JulayParser.GT, 0); }
		public List<TerminalNode> COMMA() { return getTokens(JulayParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(JulayParser.COMMA, i);
		}
		public TypeArgsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typeArgs; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterTypeArgs(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitTypeArgs(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitTypeArgs(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TypeArgsContext typeArgs() throws RecognitionException {
		TypeArgsContext _localctx = new TypeArgsContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_typeArgs);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(227);
			match(LT);
			setState(228);
			typeExpr();
			setState(233);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(229);
				match(COMMA);
				setState(230);
				typeExpr();
				}
				}
				setState(235);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(236);
			match(GT);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class TypeParamsContext extends ParserRuleContext {
		public TerminalNode LT() { return getToken(JulayParser.LT, 0); }
		public List<TerminalNode> ID() { return getTokens(JulayParser.ID); }
		public TerminalNode ID(int i) {
			return getToken(JulayParser.ID, i);
		}
		public TerminalNode GT() { return getToken(JulayParser.GT, 0); }
		public List<TerminalNode> COMMA() { return getTokens(JulayParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(JulayParser.COMMA, i);
		}
		public TypeParamsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typeParams; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterTypeParams(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitTypeParams(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitTypeParams(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TypeParamsContext typeParams() throws RecognitionException {
		TypeParamsContext _localctx = new TypeParamsContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_typeParams);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(238);
			match(LT);
			setState(239);
			match(ID);
			setState(244);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(240);
				match(COMMA);
				setState(241);
				match(ID);
				}
				}
				setState(246);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(247);
			match(GT);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Fun_declContext extends ParserRuleContext {
		public TerminalNode FUN() { return getToken(JulayParser.FUN, 0); }
		public TerminalNode ID() { return getToken(JulayParser.ID, 0); }
		public ArgsContext args() {
			return getRuleContext(ArgsContext.class,0);
		}
		public TerminalNode COLON() { return getToken(JulayParser.COLON, 0); }
		public TypeExprContext typeExpr() {
			return getRuleContext(TypeExprContext.class,0);
		}
		public TerminalNode EQ() { return getToken(JulayParser.EQ, 0); }
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public TypeParamsContext typeParams() {
			return getRuleContext(TypeParamsContext.class,0);
		}
		public Fun_declContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fun_decl; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterFun_decl(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitFun_decl(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitFun_decl(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Fun_declContext fun_decl() throws RecognitionException {
		Fun_declContext _localctx = new Fun_declContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_fun_decl);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(249);
			match(FUN);
			setState(250);
			match(ID);
			setState(252);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LT) {
				{
				setState(251);
				typeParams();
				}
			}

			setState(254);
			args();
			setState(255);
			match(COLON);
			setState(256);
			typeExpr();
			setState(257);
			match(EQ);
			setState(258);
			expr(0);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ProcContext extends ParserRuleContext {
		public TerminalNode PROC() { return getToken(JulayParser.PROC, 0); }
		public TerminalNode ID() { return getToken(JulayParser.ID, 0); }
		public TerminalNode LCURLY() { return getToken(JulayParser.LCURLY, 0); }
		public TerminalNode RCURLY() { return getToken(JulayParser.RCURLY, 0); }
		public TerminalNode STATIC() { return getToken(JulayParser.STATIC, 0); }
		public List<Proc_bodyContext> proc_body() {
			return getRuleContexts(Proc_bodyContext.class);
		}
		public Proc_bodyContext proc_body(int i) {
			return getRuleContext(Proc_bodyContext.class,i);
		}
		public TerminalNode ASGN_EQ() { return getToken(JulayParser.ASGN_EQ, 0); }
		public Proc_exprContext proc_expr() {
			return getRuleContext(Proc_exprContext.class,0);
		}
		public ProcContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_proc; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterProc(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitProc(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitProc(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ProcContext proc() throws RecognitionException {
		ProcContext _localctx = new ProcContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_proc);
		int _la;
		try {
			setState(277);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,19,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(261);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==STATIC) {
					{
					setState(260);
					match(STATIC);
					}
				}

				setState(263);
				match(PROC);
				setState(264);
				match(ID);
				setState(265);
				match(LCURLY);
				setState(269);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (((((_la - 56)) & ~0x3f) == 0 && ((1L << (_la - 56)) & 16639L) != 0)) {
					{
					{
					setState(266);
					proc_body();
					}
					}
					setState(271);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(272);
				match(RCURLY);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(273);
				match(PROC);
				setState(274);
				match(ID);
				setState(275);
				match(ASGN_EQ);
				setState(276);
				proc_expr(0);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Type_declContext extends ParserRuleContext {
		public TerminalNode TYPE() { return getToken(JulayParser.TYPE, 0); }
		public TerminalNode ID() { return getToken(JulayParser.ID, 0); }
		public TerminalNode LCURLY() { return getToken(JulayParser.LCURLY, 0); }
		public TerminalNode RCURLY() { return getToken(JulayParser.RCURLY, 0); }
		public TypeParamsContext typeParams() {
			return getRuleContext(TypeParamsContext.class,0);
		}
		public List<FieldContext> field() {
			return getRuleContexts(FieldContext.class);
		}
		public FieldContext field(int i) {
			return getRuleContext(FieldContext.class,i);
		}
		public TerminalNode ASGN_EQ() { return getToken(JulayParser.ASGN_EQ, 0); }
		public TypeExprContext typeExpr() {
			return getRuleContext(TypeExprContext.class,0);
		}
		public Type_declContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_type_decl; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterType_decl(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitType_decl(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitType_decl(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Type_declContext type_decl() throws RecognitionException {
		Type_declContext _localctx = new Type_declContext(_ctx, getState());
		enterRule(_localctx, 24, RULE_type_decl);
		int _la;
		try {
			setState(298);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,22,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(279);
				match(TYPE);
				setState(280);
				match(ID);
				setState(282);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==LT) {
					{
					setState(281);
					typeParams();
					}
				}

				setState(284);
				match(LCURLY);
				setState(288);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==ID) {
					{
					{
					setState(285);
					field();
					}
					}
					setState(290);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(291);
				match(RCURLY);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(292);
				match(TYPE);
				setState(293);
				match(ID);
				setState(294);
				match(ASGN_EQ);
				setState(295);
				typeExpr();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(296);
				match(TYPE);
				setState(297);
				match(ID);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Type_modelContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(JulayParser.ID, 0); }
		public TerminalNode ASGN_EQ() { return getToken(JulayParser.ASGN_EQ, 0); }
		public TerminalNode LCURLY() { return getToken(JulayParser.LCURLY, 0); }
		public List<LiteralContext> literal() {
			return getRuleContexts(LiteralContext.class);
		}
		public LiteralContext literal(int i) {
			return getRuleContext(LiteralContext.class,i);
		}
		public TerminalNode RCURLY() { return getToken(JulayParser.RCURLY, 0); }
		public List<TerminalNode> COMMA() { return getTokens(JulayParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(JulayParser.COMMA, i);
		}
		public Type_modelContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_type_model; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterType_model(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitType_model(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitType_model(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Type_modelContext type_model() throws RecognitionException {
		Type_modelContext _localctx = new Type_modelContext(_ctx, getState());
		enterRule(_localctx, 26, RULE_type_model);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(300);
			match(ID);
			setState(301);
			match(ASGN_EQ);
			setState(302);
			match(LCURLY);
			setState(303);
			literal();
			setState(308);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(304);
				match(COMMA);
				setState(305);
				literal();
				}
				}
				setState(310);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(311);
			match(RCURLY);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Fun_modelContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(JulayParser.ID, 0); }
		public TerminalNode ASGN_EQ() { return getToken(JulayParser.ASGN_EQ, 0); }
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public Fun_modelContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fun_model; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterFun_model(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitFun_model(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitFun_model(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Fun_modelContext fun_model() throws RecognitionException {
		Fun_modelContext _localctx = new Fun_modelContext(_ctx, getState());
		enterRule(_localctx, 28, RULE_fun_model);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(313);
			match(ID);
			setState(314);
			match(ASGN_EQ);
			setState(315);
			expr(0);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Compile_declContext extends ParserRuleContext {
		public TerminalNode COMPILE() { return getToken(JulayParser.COMPILE, 0); }
		public List<TerminalNode> ID() { return getTokens(JulayParser.ID); }
		public TerminalNode ID(int i) {
			return getToken(JulayParser.ID, i);
		}
		public List<TerminalNode> COMMA() { return getTokens(JulayParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(JulayParser.COMMA, i);
		}
		public Compile_declContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_compile_decl; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterCompile_decl(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitCompile_decl(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitCompile_decl(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Compile_declContext compile_decl() throws RecognitionException {
		Compile_declContext _localctx = new Compile_declContext(_ctx, getState());
		enterRule(_localctx, 30, RULE_compile_decl);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(317);
			match(COMPILE);
			setState(318);
			match(ID);
			setState(323);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(319);
				match(COMMA);
				setState(320);
				match(ID);
				}
				}
				setState(325);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class SpecContext extends ParserRuleContext {
		public TerminalNode SPEC() { return getToken(JulayParser.SPEC, 0); }
		public List<TerminalNode> ID() { return getTokens(JulayParser.ID); }
		public TerminalNode ID(int i) {
			return getToken(JulayParser.ID, i);
		}
		public TerminalNode LCURLY() { return getToken(JulayParser.LCURLY, 0); }
		public TerminalNode RCURLY() { return getToken(JulayParser.RCURLY, 0); }
		public TerminalNode STATIC() { return getToken(JulayParser.STATIC, 0); }
		public TerminalNode LBRACK() { return getToken(JulayParser.LBRACK, 0); }
		public TerminalNode COLON() { return getToken(JulayParser.COLON, 0); }
		public TypeExprContext typeExpr() {
			return getRuleContext(TypeExprContext.class,0);
		}
		public TerminalNode RBRACK() { return getToken(JulayParser.RBRACK, 0); }
		public List<Leaf_spec_itemContext> leaf_spec_item() {
			return getRuleContexts(Leaf_spec_itemContext.class);
		}
		public Leaf_spec_itemContext leaf_spec_item(int i) {
			return getRuleContext(Leaf_spec_itemContext.class,i);
		}
		public TerminalNode ASGN_EQ() { return getToken(JulayParser.ASGN_EQ, 0); }
		public TerminalNode MODELS() { return getToken(JulayParser.MODELS, 0); }
		public Ag_specContext ag_spec() {
			return getRuleContext(Ag_specContext.class,0);
		}
		public System_exprContext system_expr() {
			return getRuleContext(System_exprContext.class,0);
		}
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public SpecContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_spec; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterSpec(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitSpec(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitSpec(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SpecContext spec() throws RecognitionException {
		SpecContext _localctx = new SpecContext(_ctx, getState());
		enterRule(_localctx, 32, RULE_spec);
		int _la;
		try {
			setState(368);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,28,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(327);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==STATIC) {
					{
					setState(326);
					match(STATIC);
					}
				}

				setState(329);
				match(SPEC);
				setState(330);
				match(ID);
				setState(337);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==LBRACK) {
					{
					setState(331);
					match(LBRACK);
					setState(332);
					match(ID);
					setState(333);
					match(COLON);
					setState(334);
					typeExpr();
					setState(335);
					match(RBRACK);
					}
				}

				setState(339);
				match(LCURLY);
				setState(343);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (((((_la - 56)) & ~0x3f) == 0 && ((1L << (_la - 56)) & 4210943L) != 0)) {
					{
					{
					setState(340);
					leaf_spec_item();
					}
					}
					setState(345);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(346);
				match(RCURLY);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(347);
				match(SPEC);
				setState(348);
				match(ID);
				setState(349);
				match(ASGN_EQ);
				setState(350);
				match(ID);
				setState(351);
				match(MODELS);
				setState(352);
				ag_spec();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(353);
				match(SPEC);
				setState(354);
				match(ID);
				setState(355);
				match(ASGN_EQ);
				setState(356);
				ag_spec();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(357);
				match(SPEC);
				setState(358);
				match(ID);
				setState(359);
				match(ASGN_EQ);
				setState(360);
				system_expr(0);
				setState(361);
				match(MODELS);
				setState(362);
				expr(0);
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(364);
				match(SPEC);
				setState(365);
				match(ID);
				setState(366);
				match(ASGN_EQ);
				setState(367);
				system_expr(0);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Leaf_spec_itemContext extends ParserRuleContext {
		public Proc_bodyContext proc_body() {
			return getRuleContext(Proc_bodyContext.class,0);
		}
		public Type_modelContext type_model() {
			return getRuleContext(Type_modelContext.class,0);
		}
		public Fun_modelContext fun_model() {
			return getRuleContext(Fun_modelContext.class,0);
		}
		public Leaf_spec_itemContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_leaf_spec_item; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterLeaf_spec_item(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitLeaf_spec_item(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitLeaf_spec_item(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Leaf_spec_itemContext leaf_spec_item() throws RecognitionException {
		Leaf_spec_itemContext _localctx = new Leaf_spec_itemContext(_ctx, getState());
		enterRule(_localctx, 34, RULE_leaf_spec_item);
		try {
			setState(373);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,29,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(370);
				proc_body();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(371);
				type_model();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(372);
				fun_model();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Ag_specContext extends ParserRuleContext {
		public List<TerminalNode> LT() { return getTokens(JulayParser.LT); }
		public TerminalNode LT(int i) {
			return getToken(JulayParser.LT, i);
		}
		public Assume_exprContext assume_expr() {
			return getRuleContext(Assume_exprContext.class,0);
		}
		public List<TerminalNode> GT() { return getTokens(JulayParser.GT); }
		public TerminalNode GT(int i) {
			return getToken(JulayParser.GT, i);
		}
		public System_exprContext system_expr() {
			return getRuleContext(System_exprContext.class,0);
		}
		public Guarantee_exprContext guarantee_expr() {
			return getRuleContext(Guarantee_exprContext.class,0);
		}
		public Ag_specContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_ag_spec; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterAg_spec(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitAg_spec(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitAg_spec(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Ag_specContext ag_spec() throws RecognitionException {
		Ag_specContext _localctx = new Ag_specContext(_ctx, getState());
		enterRule(_localctx, 36, RULE_ag_spec);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(375);
			match(LT);
			setState(376);
			assume_expr();
			setState(377);
			match(GT);
			setState(378);
			system_expr(0);
			setState(379);
			match(LT);
			setState(380);
			guarantee_expr();
			setState(381);
			match(GT);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Assume_exprContext extends ParserRuleContext {
		public TerminalNode TRUE() { return getToken(JulayParser.TRUE, 0); }
		public TerminalNode COMMA() { return getToken(JulayParser.COMMA, 0); }
		public List<TerminalNode> ID() { return getTokens(JulayParser.ID); }
		public TerminalNode ID(int i) {
			return getToken(JulayParser.ID, i);
		}
		public System_exprContext system_expr() {
			return getRuleContext(System_exprContext.class,0);
		}
		public Assume_exprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_assume_expr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterAssume_expr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitAssume_expr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitAssume_expr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Assume_exprContext assume_expr() throws RecognitionException {
		Assume_exprContext _localctx = new Assume_exprContext(_ctx, getState());
		enterRule(_localctx, 38, RULE_assume_expr);
		int _la;
		try {
			setState(392);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,31,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(383);
				match(TRUE);
				setState(386);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==COMMA) {
					{
					setState(384);
					match(COMMA);
					setState(385);
					match(ID);
					}
				}

				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(388);
				match(ID);
				setState(389);
				match(COMMA);
				setState(390);
				match(ID);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(391);
				system_expr(0);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Guarantee_exprContext extends ParserRuleContext {
		public List<TerminalNode> ID() { return getTokens(JulayParser.ID); }
		public TerminalNode ID(int i) {
			return getToken(JulayParser.ID, i);
		}
		public TerminalNode COMMA() { return getToken(JulayParser.COMMA, 0); }
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public Guarantee_exprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_guarantee_expr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterGuarantee_expr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitGuarantee_expr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitGuarantee_expr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Guarantee_exprContext guarantee_expr() throws RecognitionException {
		Guarantee_exprContext _localctx = new Guarantee_exprContext(_ctx, getState());
		enterRule(_localctx, 40, RULE_guarantee_expr);
		try {
			setState(398);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,32,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(394);
				match(ID);
				setState(395);
				match(COMMA);
				setState(396);
				match(ID);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(397);
				expr(0);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class System_exprContext extends ParserRuleContext {
		public With_exprContext with_expr() {
			return getRuleContext(With_exprContext.class,0);
		}
		public System_atomContext system_atom() {
			return getRuleContext(System_atomContext.class,0);
		}
		public List<System_exprContext> system_expr() {
			return getRuleContexts(System_exprContext.class);
		}
		public System_exprContext system_expr(int i) {
			return getRuleContext(System_exprContext.class,i);
		}
		public TerminalNode PARALLEL() { return getToken(JulayParser.PARALLEL, 0); }
		public System_exprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_system_expr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterSystem_expr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitSystem_expr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitSystem_expr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final System_exprContext system_expr() throws RecognitionException {
		return system_expr(0);
	}

	private System_exprContext system_expr(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		System_exprContext _localctx = new System_exprContext(_ctx, _parentState);
		System_exprContext _prevctx = _localctx;
		int _startState = 42;
		enterRecursionRule(_localctx, 42, RULE_system_expr, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(403);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case WITH:
				{
				setState(401);
				with_expr();
				}
				break;
			case LPAREN:
			case LISTOF:
			case SETOF:
			case MAPOF:
			case STATIC:
			case INTERNAL:
			case TIMER:
			case SERVICE:
			case CLIENT:
			case ID:
				{
				setState(402);
				system_atom();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			_ctx.stop = _input.LT(-1);
			setState(410);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,34,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					{
					_localctx = new System_exprContext(_parentctx, _parentState);
					pushNewRecursionContext(_localctx, _startState, RULE_system_expr);
					setState(405);
					if (!(precpred(_ctx, 3))) throw new FailedPredicateException(this, "precpred(_ctx, 3)");
					setState(406);
					match(PARALLEL);
					setState(407);
					system_expr(4);
					}
					} 
				}
				setState(412);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,34,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class With_exprContext extends ParserRuleContext {
		public TerminalNode WITH() { return getToken(JulayParser.WITH, 0); }
		public TerminalNode LPAREN() { return getToken(JulayParser.LPAREN, 0); }
		public TerminalNode ID() { return getToken(JulayParser.ID, 0); }
		public TerminalNode COLON() { return getToken(JulayParser.COLON, 0); }
		public TypeExprContext typeExpr() {
			return getRuleContext(TypeExprContext.class,0);
		}
		public TerminalNode RPAREN() { return getToken(JulayParser.RPAREN, 0); }
		public TerminalNode LCURLY() { return getToken(JulayParser.LCURLY, 0); }
		public System_exprContext system_expr() {
			return getRuleContext(System_exprContext.class,0);
		}
		public TerminalNode RCURLY() { return getToken(JulayParser.RCURLY, 0); }
		public With_exprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_with_expr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterWith_expr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitWith_expr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitWith_expr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final With_exprContext with_expr() throws RecognitionException {
		With_exprContext _localctx = new With_exprContext(_ctx, getState());
		enterRule(_localctx, 44, RULE_with_expr);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(413);
			match(WITH);
			setState(414);
			match(LPAREN);
			setState(415);
			match(ID);
			setState(416);
			match(COLON);
			setState(417);
			typeExpr();
			setState(418);
			match(RPAREN);
			setState(419);
			match(LCURLY);
			setState(420);
			system_expr(0);
			setState(421);
			match(RCURLY);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class System_atomContext extends ParserRuleContext {
		public System_primaryContext system_primary() {
			return getRuleContext(System_primaryContext.class,0);
		}
		public TerminalNode LBRACK() { return getToken(JulayParser.LBRACK, 0); }
		public TerminalNode ID() { return getToken(JulayParser.ID, 0); }
		public TerminalNode COLON() { return getToken(JulayParser.COLON, 0); }
		public TypeExprContext typeExpr() {
			return getRuleContext(TypeExprContext.class,0);
		}
		public TerminalNode RBRACK() { return getToken(JulayParser.RBRACK, 0); }
		public TerminalNode LCURLY() { return getToken(JulayParser.LCURLY, 0); }
		public TerminalNode RCURLY() { return getToken(JulayParser.RCURLY, 0); }
		public List<Create_index_itemContext> create_index_item() {
			return getRuleContexts(Create_index_itemContext.class);
		}
		public Create_index_itemContext create_index_item(int i) {
			return getRuleContext(Create_index_itemContext.class,i);
		}
		public System_atomContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_system_atom; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterSystem_atom(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitSystem_atom(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitSystem_atom(this);
			else return visitor.visitChildren(this);
		}
	}

	public final System_atomContext system_atom() throws RecognitionException {
		System_atomContext _localctx = new System_atomContext(_ctx, getState());
		enterRule(_localctx, 46, RULE_system_atom);
		int _la;
		try {
			setState(445);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,37,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(423);
				system_primary();
				setState(424);
				match(LBRACK);
				setState(425);
				match(ID);
				setState(426);
				match(COLON);
				setState(427);
				typeExpr();
				setState(428);
				match(RBRACK);
				setState(437);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,36,_ctx) ) {
				case 1:
					{
					setState(429);
					match(LCURLY);
					setState(433);
					_errHandler.sync(this);
					_la = _input.LA(1);
					while (((((_la - 57)) & ~0x3f) == 0 && ((1L << (_la - 57)) & 2490369L) != 0)) {
						{
						{
						setState(430);
						create_index_item();
						}
						}
						setState(435);
						_errHandler.sync(this);
						_la = _input.LA(1);
					}
					setState(436);
					match(RCURLY);
					}
					break;
				}
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(439);
				system_primary();
				setState(440);
				match(LBRACK);
				setState(441);
				match(ID);
				setState(442);
				match(RBRACK);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(444);
				system_primary();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Create_index_itemContext extends ParserRuleContext {
		public Global_declContext global_decl() {
			return getRuleContext(Global_declContext.class,0);
		}
		public Init_clauseContext init_clause() {
			return getRuleContext(Init_clauseContext.class,0);
		}
		public Type_modelContext type_model() {
			return getRuleContext(Type_modelContext.class,0);
		}
		public Fun_modelContext fun_model() {
			return getRuleContext(Fun_modelContext.class,0);
		}
		public Create_index_itemContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_create_index_item; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterCreate_index_item(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitCreate_index_item(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitCreate_index_item(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Create_index_itemContext create_index_item() throws RecognitionException {
		Create_index_itemContext _localctx = new Create_index_itemContext(_ctx, getState());
		enterRule(_localctx, 48, RULE_create_index_item);
		try {
			setState(451);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,38,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(447);
				global_decl();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(448);
				init_clause();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(449);
				type_model();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(450);
				fun_model();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Global_declContext extends ParserRuleContext {
		public TerminalNode GLOBAL() { return getToken(JulayParser.GLOBAL, 0); }
		public List<TerminalNode> ID() { return getTokens(JulayParser.ID); }
		public TerminalNode ID(int i) {
			return getToken(JulayParser.ID, i);
		}
		public TerminalNode CONST() { return getToken(JulayParser.CONST, 0); }
		public List<TerminalNode> COMMA() { return getTokens(JulayParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(JulayParser.COMMA, i);
		}
		public Global_declContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_global_decl; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterGlobal_decl(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitGlobal_decl(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitGlobal_decl(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Global_declContext global_decl() throws RecognitionException {
		Global_declContext _localctx = new Global_declContext(_ctx, getState());
		enterRule(_localctx, 50, RULE_global_decl);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(454);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==CONST) {
				{
				setState(453);
				match(CONST);
				}
			}

			setState(456);
			match(GLOBAL);
			setState(457);
			match(ID);
			setState(462);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(458);
				match(COMMA);
				setState(459);
				match(ID);
				}
				}
				setState(464);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Init_clauseContext extends ParserRuleContext {
		public TerminalNode INIT() { return getToken(JulayParser.INIT, 0); }
		public TerminalNode COLON() { return getToken(JulayParser.COLON, 0); }
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public Init_clauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_init_clause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterInit_clause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitInit_clause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitInit_clause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Init_clauseContext init_clause() throws RecognitionException {
		Init_clauseContext _localctx = new Init_clauseContext(_ctx, getState());
		enterRule(_localctx, 52, RULE_init_clause);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(465);
			match(INIT);
			setState(466);
			match(COLON);
			setState(467);
			expr(0);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class System_primaryContext extends ParserRuleContext {
		public System_leafContext system_leaf() {
			return getRuleContext(System_leafContext.class,0);
		}
		public TerminalNode LPAREN() { return getToken(JulayParser.LPAREN, 0); }
		public System_exprContext system_expr() {
			return getRuleContext(System_exprContext.class,0);
		}
		public TerminalNode RPAREN() { return getToken(JulayParser.RPAREN, 0); }
		public System_primaryContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_system_primary; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterSystem_primary(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitSystem_primary(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitSystem_primary(this);
			else return visitor.visitChildren(this);
		}
	}

	public final System_primaryContext system_primary() throws RecognitionException {
		System_primaryContext _localctx = new System_primaryContext(_ctx, getState());
		enterRule(_localctx, 54, RULE_system_primary);
		try {
			setState(474);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case LISTOF:
			case SETOF:
			case MAPOF:
			case STATIC:
			case INTERNAL:
			case TIMER:
			case SERVICE:
			case CLIENT:
			case ID:
				enterOuterAlt(_localctx, 1);
				{
				setState(469);
				system_leaf();
				}
				break;
			case LPAREN:
				enterOuterAlt(_localctx, 2);
				{
				setState(470);
				match(LPAREN);
				setState(471);
				system_expr(0);
				setState(472);
				match(RPAREN);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class System_leafContext extends ParserRuleContext {
		public Qualified_nameContext qualified_name() {
			return getRuleContext(Qualified_nameContext.class,0);
		}
		public TerminalNode ID() { return getToken(JulayParser.ID, 0); }
		public System_leafContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_system_leaf; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterSystem_leaf(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitSystem_leaf(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitSystem_leaf(this);
			else return visitor.visitChildren(this);
		}
	}

	public final System_leafContext system_leaf() throws RecognitionException {
		System_leafContext _localctx = new System_leafContext(_ctx, getState());
		enterRule(_localctx, 56, RULE_system_leaf);
		try {
			setState(478);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,42,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(476);
				qualified_name();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(477);
				match(ID);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Invariant_declContext extends ParserRuleContext {
		public TerminalNode INVARIANT() { return getToken(JulayParser.INVARIANT, 0); }
		public TerminalNode ID() { return getToken(JulayParser.ID, 0); }
		public TerminalNode ASGN_EQ() { return getToken(JulayParser.ASGN_EQ, 0); }
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public Invariant_declContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_invariant_decl; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterInvariant_decl(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitInvariant_decl(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitInvariant_decl(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Invariant_declContext invariant_decl() throws RecognitionException {
		Invariant_declContext _localctx = new Invariant_declContext(_ctx, getState());
		enterRule(_localctx, 58, RULE_invariant_decl);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(480);
			match(INVARIANT);
			setState(481);
			match(ID);
			setState(482);
			match(ASGN_EQ);
			setState(483);
			expr(0);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Fluent_declContext extends ParserRuleContext {
		public TerminalNode FLUENT() { return getToken(JulayParser.FLUENT, 0); }
		public TerminalNode ID() { return getToken(JulayParser.ID, 0); }
		public TerminalNode LCURLY() { return getToken(JulayParser.LCURLY, 0); }
		public TerminalNode RCURLY() { return getToken(JulayParser.RCURLY, 0); }
		public Fluent_paramsContext fluent_params() {
			return getRuleContext(Fluent_paramsContext.class,0);
		}
		public List<Fluent_itemContext> fluent_item() {
			return getRuleContexts(Fluent_itemContext.class);
		}
		public Fluent_itemContext fluent_item(int i) {
			return getRuleContext(Fluent_itemContext.class,i);
		}
		public Fluent_declContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fluent_decl; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterFluent_decl(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitFluent_decl(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitFluent_decl(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Fluent_declContext fluent_decl() throws RecognitionException {
		Fluent_declContext _localctx = new Fluent_declContext(_ctx, getState());
		enterRule(_localctx, 60, RULE_fluent_decl);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(485);
			match(FLUENT);
			setState(486);
			match(ID);
			setState(488);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LPAREN) {
				{
				setState(487);
				fluent_params();
				}
			}

			setState(490);
			match(LCURLY);
			setState(492); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(491);
				fluent_item();
				}
				}
				setState(494); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==ON || _la==INITIALLY );
			setState(496);
			match(RCURLY);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Fluent_paramsContext extends ParserRuleContext {
		public TerminalNode LPAREN() { return getToken(JulayParser.LPAREN, 0); }
		public List<Fluent_paramContext> fluent_param() {
			return getRuleContexts(Fluent_paramContext.class);
		}
		public Fluent_paramContext fluent_param(int i) {
			return getRuleContext(Fluent_paramContext.class,i);
		}
		public TerminalNode RPAREN() { return getToken(JulayParser.RPAREN, 0); }
		public List<TerminalNode> COMMA() { return getTokens(JulayParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(JulayParser.COMMA, i);
		}
		public Fluent_paramsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fluent_params; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterFluent_params(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitFluent_params(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitFluent_params(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Fluent_paramsContext fluent_params() throws RecognitionException {
		Fluent_paramsContext _localctx = new Fluent_paramsContext(_ctx, getState());
		enterRule(_localctx, 62, RULE_fluent_params);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(498);
			match(LPAREN);
			setState(499);
			fluent_param();
			setState(504);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(500);
				match(COMMA);
				setState(501);
				fluent_param();
				}
				}
				setState(506);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(507);
			match(RPAREN);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Fluent_paramContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(JulayParser.ID, 0); }
		public TerminalNode COLON() { return getToken(JulayParser.COLON, 0); }
		public TypeExprContext typeExpr() {
			return getRuleContext(TypeExprContext.class,0);
		}
		public Fluent_paramContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fluent_param; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterFluent_param(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitFluent_param(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitFluent_param(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Fluent_paramContext fluent_param() throws RecognitionException {
		Fluent_paramContext _localctx = new Fluent_paramContext(_ctx, getState());
		enterRule(_localctx, 64, RULE_fluent_param);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(509);
			match(ID);
			setState(510);
			match(COLON);
			setState(511);
			typeExpr();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Fluent_itemContext extends ParserRuleContext {
		public TerminalNode INITIALLY() { return getToken(JulayParser.INITIALLY, 0); }
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public TerminalNode ON() { return getToken(JulayParser.ON, 0); }
		public List<TerminalNode> ID() { return getTokens(JulayParser.ID); }
		public TerminalNode ID(int i) {
			return getToken(JulayParser.ID, i);
		}
		public TerminalNode LPAREN() { return getToken(JulayParser.LPAREN, 0); }
		public On_arg_listContext on_arg_list() {
			return getRuleContext(On_arg_listContext.class,0);
		}
		public TerminalNode RPAREN() { return getToken(JulayParser.RPAREN, 0); }
		public TerminalNode ASGN_EQ() { return getToken(JulayParser.ASGN_EQ, 0); }
		public Fluent_targetContext fluent_target() {
			return getRuleContext(Fluent_targetContext.class,0);
		}
		public TerminalNode LBRACK() { return getToken(JulayParser.LBRACK, 0); }
		public TerminalNode RBRACK() { return getToken(JulayParser.RBRACK, 0); }
		public Fluent_itemContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fluent_item; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterFluent_item(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitFluent_item(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitFluent_item(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Fluent_itemContext fluent_item() throws RecognitionException {
		Fluent_itemContext _localctx = new Fluent_itemContext(_ctx, getState());
		enterRule(_localctx, 66, RULE_fluent_item);
		int _la;
		try {
			setState(528);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case INITIALLY:
				enterOuterAlt(_localctx, 1);
				{
				setState(513);
				match(INITIALLY);
				setState(514);
				expr(0);
				}
				break;
			case ON:
				enterOuterAlt(_localctx, 2);
				{
				setState(515);
				match(ON);
				setState(516);
				match(ID);
				setState(520);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==LBRACK) {
					{
					setState(517);
					match(LBRACK);
					setState(518);
					match(ID);
					setState(519);
					match(RBRACK);
					}
				}

				setState(522);
				match(LPAREN);
				setState(523);
				on_arg_list();
				setState(524);
				match(RPAREN);
				setState(525);
				match(ASGN_EQ);
				setState(526);
				fluent_target();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class On_arg_listContext extends ParserRuleContext {
		public List<On_argContext> on_arg() {
			return getRuleContexts(On_argContext.class);
		}
		public On_argContext on_arg(int i) {
			return getRuleContext(On_argContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(JulayParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(JulayParser.COMMA, i);
		}
		public On_arg_listContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_on_arg_list; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterOn_arg_list(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitOn_arg_list(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitOn_arg_list(this);
			else return visitor.visitChildren(this);
		}
	}

	public final On_arg_listContext on_arg_list() throws RecognitionException {
		On_arg_listContext _localctx = new On_arg_listContext(_ctx, getState());
		enterRule(_localctx, 68, RULE_on_arg_list);
		int _la;
		try {
			setState(539);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case RPAREN:
				enterOuterAlt(_localctx, 1);
				{
				}
				break;
			case ID:
				enterOuterAlt(_localctx, 2);
				{
				setState(531);
				on_arg();
				setState(536);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(532);
					match(COMMA);
					setState(533);
					on_arg();
					}
					}
					setState(538);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class On_argContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(JulayParser.ID, 0); }
		public On_argContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_on_arg; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterOn_arg(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitOn_arg(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitOn_arg(this);
			else return visitor.visitChildren(this);
		}
	}

	public final On_argContext on_arg() throws RecognitionException {
		On_argContext _localctx = new On_argContext(_ctx, getState());
		enterRule(_localctx, 70, RULE_on_arg);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(541);
			match(ID);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Fluent_targetContext extends ParserRuleContext {
		public TerminalNode TRUE() { return getToken(JulayParser.TRUE, 0); }
		public TerminalNode FALSE() { return getToken(JulayParser.FALSE, 0); }
		public TerminalNode NOT() { return getToken(JulayParser.NOT, 0); }
		public TerminalNode ID() { return getToken(JulayParser.ID, 0); }
		public TerminalNode LPAREN() { return getToken(JulayParser.LPAREN, 0); }
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public TerminalNode RPAREN() { return getToken(JulayParser.RPAREN, 0); }
		public List<TerminalNode> COMMA() { return getTokens(JulayParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(JulayParser.COMMA, i);
		}
		public Fluent_targetContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fluent_target; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterFluent_target(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitFluent_target(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitFluent_target(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Fluent_targetContext fluent_target() throws RecognitionException {
		Fluent_targetContext _localctx = new Fluent_targetContext(_ctx, getState());
		enterRule(_localctx, 72, RULE_fluent_target);
		int _la;
		try {
			setState(574);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case TRUE:
				enterOuterAlt(_localctx, 1);
				{
				setState(543);
				match(TRUE);
				}
				break;
			case FALSE:
				enterOuterAlt(_localctx, 2);
				{
				setState(544);
				match(FALSE);
				}
				break;
			case NOT:
				enterOuterAlt(_localctx, 3);
				{
				setState(545);
				match(NOT);
				setState(546);
				match(ID);
				setState(558);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==LPAREN) {
					{
					setState(547);
					match(LPAREN);
					setState(548);
					expr(0);
					setState(553);
					_errHandler.sync(this);
					_la = _input.LA(1);
					while (_la==COMMA) {
						{
						{
						setState(549);
						match(COMMA);
						setState(550);
						expr(0);
						}
						}
						setState(555);
						_errHandler.sync(this);
						_la = _input.LA(1);
					}
					setState(556);
					match(RPAREN);
					}
				}

				}
				break;
			case ID:
				enterOuterAlt(_localctx, 4);
				{
				setState(560);
				match(ID);
				setState(572);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==LPAREN) {
					{
					setState(561);
					match(LPAREN);
					setState(562);
					expr(0);
					setState(567);
					_errHandler.sync(this);
					_la = _input.LA(1);
					while (_la==COMMA) {
						{
						{
						setState(563);
						match(COMMA);
						setState(564);
						expr(0);
						}
						}
						setState(569);
						_errHandler.sync(this);
						_la = _input.LA(1);
					}
					setState(570);
					match(RPAREN);
					}
				}

				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Proc_bodyContext extends ParserRuleContext {
		public VarContext var() {
			return getRuleContext(VarContext.class,0);
		}
		public ConstructorContext constructor() {
			return getRuleContext(ConstructorContext.class,0);
		}
		public TransitionContext transition() {
			return getRuleContext(TransitionContext.class,0);
		}
		public Return_actionContext return_action() {
			return getRuleContext(Return_actionContext.class,0);
		}
		public Proc_bodyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_proc_body; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterProc_body(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitProc_body(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitProc_body(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Proc_bodyContext proc_body() throws RecognitionException {
		Proc_bodyContext _localctx = new Proc_bodyContext(_ctx, getState());
		enterRule(_localctx, 74, RULE_proc_body);
		try {
			setState(580);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,55,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(576);
				var();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(577);
				constructor();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(578);
				transition();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(579);
				return_action();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class FieldContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(JulayParser.ID, 0); }
		public TerminalNode COLON() { return getToken(JulayParser.COLON, 0); }
		public TypeExprContext typeExpr() {
			return getRuleContext(TypeExprContext.class,0);
		}
		public FieldContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_field; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterField(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitField(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitField(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FieldContext field() throws RecognitionException {
		FieldContext _localctx = new FieldContext(_ctx, getState());
		enterRule(_localctx, 76, RULE_field);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(582);
			match(ID);
			setState(583);
			match(COLON);
			setState(584);
			typeExpr();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class VarContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(JulayParser.ID, 0); }
		public TerminalNode COLON() { return getToken(JulayParser.COLON, 0); }
		public TypeExprContext typeExpr() {
			return getRuleContext(TypeExprContext.class,0);
		}
		public TerminalNode VAR() { return getToken(JulayParser.VAR, 0); }
		public TerminalNode CONST() { return getToken(JulayParser.CONST, 0); }
		public TerminalNode ASGN_EQ() { return getToken(JulayParser.ASGN_EQ, 0); }
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public VarContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_var; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterVar(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitVar(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitVar(this);
			else return visitor.visitChildren(this);
		}
	}

	public final VarContext var() throws RecognitionException {
		VarContext _localctx = new VarContext(_ctx, getState());
		enterRule(_localctx, 78, RULE_var);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(586);
			_la = _input.LA(1);
			if ( !(_la==VAR || _la==CONST) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			setState(587);
			match(ID);
			setState(588);
			match(COLON);
			setState(589);
			typeExpr();
			setState(592);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ASGN_EQ) {
				{
				setState(590);
				match(ASGN_EQ);
				setState(591);
				expr(0);
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ConstructorContext extends ParserRuleContext {
		public TerminalNode CONSTRUCTOR() { return getToken(JulayParser.CONSTRUCTOR, 0); }
		public List<ArgsContext> args() {
			return getRuleContexts(ArgsContext.class);
		}
		public ArgsContext args(int i) {
			return getRuleContext(ArgsContext.class,i);
		}
		public TerminalNode LCURLY() { return getToken(JulayParser.LCURLY, 0); }
		public TerminalNode RCURLY() { return getToken(JulayParser.RCURLY, 0); }
		public TerminalNode ID() { return getToken(JulayParser.ID, 0); }
		public TerminalNode INITIALLY() { return getToken(JulayParser.INITIALLY, 0); }
		public TerminalNode ALSO() { return getToken(JulayParser.ALSO, 0); }
		public List<Constructor_bodyContext> constructor_body() {
			return getRuleContexts(Constructor_bodyContext.class);
		}
		public Constructor_bodyContext constructor_body(int i) {
			return getRuleContext(Constructor_bodyContext.class,i);
		}
		public ConstructorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_constructor; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterConstructor(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitConstructor(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitConstructor(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ConstructorContext constructor() throws RecognitionException {
		ConstructorContext _localctx = new ConstructorContext(_ctx, getState());
		enterRule(_localctx, 80, RULE_constructor);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(594);
			match(CONSTRUCTOR);
			setState(595);
			_la = _input.LA(1);
			if ( !(_la==INITIALLY || _la==ID) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			setState(596);
			args();
			setState(599);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ALSO) {
				{
				setState(597);
				match(ALSO);
				setState(598);
				args();
				}
			}

			setState(601);
			match(LCURLY);
			setState(605);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (((((_la - 65)) & ~0x3f) == 0 && ((1L << (_la - 65)) & 15L) != 0)) {
				{
				{
				setState(602);
				constructor_body();
				}
				}
				setState(607);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(608);
			match(RCURLY);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class TransitionContext extends ParserRuleContext {
		public TerminalNode TRANSITION() { return getToken(JulayParser.TRANSITION, 0); }
		public TerminalNode ID() { return getToken(JulayParser.ID, 0); }
		public List<ArgsContext> args() {
			return getRuleContexts(ArgsContext.class);
		}
		public ArgsContext args(int i) {
			return getRuleContext(ArgsContext.class,i);
		}
		public TerminalNode LCURLY() { return getToken(JulayParser.LCURLY, 0); }
		public TerminalNode RCURLY() { return getToken(JulayParser.RCURLY, 0); }
		public TerminalNode ALSO() { return getToken(JulayParser.ALSO, 0); }
		public List<Action_bodyContext> action_body() {
			return getRuleContexts(Action_bodyContext.class);
		}
		public Action_bodyContext action_body(int i) {
			return getRuleContext(Action_bodyContext.class,i);
		}
		public TerminalNode INTERNAL() { return getToken(JulayParser.INTERNAL, 0); }
		public TerminalNode TIMER() { return getToken(JulayParser.TIMER, 0); }
		public TerminalNode SERVICE() { return getToken(JulayParser.SERVICE, 0); }
		public TerminalNode CLIENT() { return getToken(JulayParser.CLIENT, 0); }
		public TransitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_transition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterTransition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitTransition(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitTransition(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TransitionContext transition() throws RecognitionException {
		TransitionContext _localctx = new TransitionContext(_ctx, getState());
		enterRule(_localctx, 82, RULE_transition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(611);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & -1152921504606846976L) != 0)) {
				{
				setState(610);
				_la = _input.LA(1);
				if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & -1152921504606846976L) != 0)) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
			}

			setState(613);
			match(TRANSITION);
			setState(614);
			match(ID);
			setState(615);
			args();
			setState(618);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ALSO) {
				{
				setState(616);
				match(ALSO);
				setState(617);
				args();
				}
			}

			setState(620);
			match(LCURLY);
			setState(624);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & 31L) != 0)) {
				{
				{
				setState(621);
				action_body();
				}
				}
				setState(626);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(627);
			match(RCURLY);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Return_actionContext extends ParserRuleContext {
		public TerminalNode RETURN() { return getToken(JulayParser.RETURN, 0); }
		public TerminalNode ID() { return getToken(JulayParser.ID, 0); }
		public ArgsContext args() {
			return getRuleContext(ArgsContext.class,0);
		}
		public TerminalNode LCURLY() { return getToken(JulayParser.LCURLY, 0); }
		public TerminalNode RCURLY() { return getToken(JulayParser.RCURLY, 0); }
		public List<Return_bodyContext> return_body() {
			return getRuleContexts(Return_bodyContext.class);
		}
		public Return_bodyContext return_body(int i) {
			return getRuleContext(Return_bodyContext.class,i);
		}
		public TerminalNode INTERNAL() { return getToken(JulayParser.INTERNAL, 0); }
		public TerminalNode SERVICE() { return getToken(JulayParser.SERVICE, 0); }
		public TerminalNode CLIENT() { return getToken(JulayParser.CLIENT, 0); }
		public Return_actionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_return_action; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterReturn_action(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitReturn_action(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitReturn_action(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Return_actionContext return_action() throws RecognitionException {
		Return_actionContext _localctx = new Return_actionContext(_ctx, getState());
		enterRule(_localctx, 84, RULE_return_action);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(630);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & -3458764513820540928L) != 0)) {
				{
				setState(629);
				_la = _input.LA(1);
				if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & -3458764513820540928L) != 0)) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
			}

			setState(632);
			match(RETURN);
			setState(633);
			match(ID);
			setState(634);
			args();
			setState(635);
			match(LCURLY);
			setState(639);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==GUARD || _la==BEFORE) {
				{
				{
				setState(636);
				return_body();
				}
				}
				setState(641);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(642);
			match(RCURLY);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Return_bodyContext extends ParserRuleContext {
		public GuardContext guard() {
			return getRuleContext(GuardContext.class,0);
		}
		public BeforeContext before() {
			return getRuleContext(BeforeContext.class,0);
		}
		public Return_bodyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_return_body; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterReturn_body(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitReturn_body(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitReturn_body(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Return_bodyContext return_body() throws RecognitionException {
		Return_bodyContext _localctx = new Return_bodyContext(_ctx, getState());
		enterRule(_localctx, 86, RULE_return_body);
		try {
			setState(646);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case GUARD:
				enterOuterAlt(_localctx, 1);
				{
				setState(644);
				guard();
				}
				break;
			case BEFORE:
				enterOuterAlt(_localctx, 2);
				{
				setState(645);
				before();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ArgsContext extends ParserRuleContext {
		public TerminalNode LPAREN() { return getToken(JulayParser.LPAREN, 0); }
		public TerminalNode RPAREN() { return getToken(JulayParser.RPAREN, 0); }
		public List<ArgContext> arg() {
			return getRuleContexts(ArgContext.class);
		}
		public ArgContext arg(int i) {
			return getRuleContext(ArgContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(JulayParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(JulayParser.COMMA, i);
		}
		public ArgsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_args; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterArgs(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitArgs(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitArgs(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ArgsContext args() throws RecognitionException {
		ArgsContext _localctx = new ArgsContext(_ctx, getState());
		enterRule(_localctx, 88, RULE_args);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(648);
			match(LPAREN);
			setState(650);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ID) {
				{
				setState(649);
				arg();
				}
			}

			setState(656);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(652);
				match(COMMA);
				setState(653);
				arg();
				}
				}
				setState(658);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(659);
			match(RPAREN);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ArgContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(JulayParser.ID, 0); }
		public TerminalNode COLON() { return getToken(JulayParser.COLON, 0); }
		public TypeExprContext typeExpr() {
			return getRuleContext(TypeExprContext.class,0);
		}
		public ArgContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_arg; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterArg(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitArg(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitArg(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ArgContext arg() throws RecognitionException {
		ArgContext _localctx = new ArgContext(_ctx, getState());
		enterRule(_localctx, 90, RULE_arg);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(661);
			match(ID);
			setState(662);
			match(COLON);
			setState(663);
			typeExpr();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Constructor_bodyContext extends ParserRuleContext {
		public BeforeContext before() {
			return getRuleContext(BeforeContext.class,0);
		}
		public TransitContext transit() {
			return getRuleContext(TransitContext.class,0);
		}
		public ErrorContext error() {
			return getRuleContext(ErrorContext.class,0);
		}
		public AfterContext after() {
			return getRuleContext(AfterContext.class,0);
		}
		public Constructor_bodyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_constructor_body; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterConstructor_body(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitConstructor_body(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitConstructor_body(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Constructor_bodyContext constructor_body() throws RecognitionException {
		Constructor_bodyContext _localctx = new Constructor_bodyContext(_ctx, getState());
		enterRule(_localctx, 92, RULE_constructor_body);
		try {
			setState(669);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case BEFORE:
				enterOuterAlt(_localctx, 1);
				{
				setState(665);
				before();
				}
				break;
			case TRANSIT:
				enterOuterAlt(_localctx, 2);
				{
				setState(666);
				transit();
				}
				break;
			case ERROR:
				enterOuterAlt(_localctx, 3);
				{
				setState(667);
				error();
				}
				break;
			case AFTER:
				enterOuterAlt(_localctx, 4);
				{
				setState(668);
				after();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Action_bodyContext extends ParserRuleContext {
		public GuardContext guard() {
			return getRuleContext(GuardContext.class,0);
		}
		public BeforeContext before() {
			return getRuleContext(BeforeContext.class,0);
		}
		public TransitContext transit() {
			return getRuleContext(TransitContext.class,0);
		}
		public ErrorContext error() {
			return getRuleContext(ErrorContext.class,0);
		}
		public AfterContext after() {
			return getRuleContext(AfterContext.class,0);
		}
		public Action_bodyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_action_body; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterAction_body(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitAction_body(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitAction_body(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Action_bodyContext action_body() throws RecognitionException {
		Action_bodyContext _localctx = new Action_bodyContext(_ctx, getState());
		enterRule(_localctx, 94, RULE_action_body);
		try {
			setState(676);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case GUARD:
				enterOuterAlt(_localctx, 1);
				{
				setState(671);
				guard();
				}
				break;
			case BEFORE:
				enterOuterAlt(_localctx, 2);
				{
				setState(672);
				before();
				}
				break;
			case TRANSIT:
				enterOuterAlt(_localctx, 3);
				{
				setState(673);
				transit();
				}
				break;
			case ERROR:
				enterOuterAlt(_localctx, 4);
				{
				setState(674);
				error();
				}
				break;
			case AFTER:
				enterOuterAlt(_localctx, 5);
				{
				setState(675);
				after();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class GuardContext extends ParserRuleContext {
		public TerminalNode GUARD() { return getToken(JulayParser.GUARD, 0); }
		public TerminalNode COLON() { return getToken(JulayParser.COLON, 0); }
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public GuardContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_guard; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterGuard(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitGuard(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitGuard(this);
			else return visitor.visitChildren(this);
		}
	}

	public final GuardContext guard() throws RecognitionException {
		GuardContext _localctx = new GuardContext(_ctx, getState());
		enterRule(_localctx, 96, RULE_guard);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(678);
			match(GUARD);
			setState(679);
			match(COLON);
			setState(680);
			expr(0);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class TransitContext extends ParserRuleContext {
		public TerminalNode TRANSIT() { return getToken(JulayParser.TRANSIT, 0); }
		public TerminalNode COLON() { return getToken(JulayParser.COLON, 0); }
		public List<Var_transitContext> var_transit() {
			return getRuleContexts(Var_transitContext.class);
		}
		public Var_transitContext var_transit(int i) {
			return getRuleContext(Var_transitContext.class,i);
		}
		public TransitContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_transit; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterTransit(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitTransit(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitTransit(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TransitContext transit() throws RecognitionException {
		TransitContext _localctx = new TransitContext(_ctx, getState());
		enterRule(_localctx, 98, RULE_transit);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(682);
			match(TRANSIT);
			setState(683);
			match(COLON);
			setState(687);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (((((_la - 35)) & ~0x3f) == 0 && ((1L << (_la - 35)) & 9070970929153L) != 0)) {
				{
				{
				setState(684);
				var_transit();
				}
				}
				setState(689);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ErrorContext extends ParserRuleContext {
		public TerminalNode ERROR() { return getToken(JulayParser.ERROR, 0); }
		public TerminalNode COLON() { return getToken(JulayParser.COLON, 0); }
		public List<Error_armContext> error_arm() {
			return getRuleContexts(Error_armContext.class);
		}
		public Error_armContext error_arm(int i) {
			return getRuleContext(Error_armContext.class,i);
		}
		public ErrorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_error; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterError(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitError(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitError(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ErrorContext error() throws RecognitionException {
		ErrorContext _localctx = new ErrorContext(_ctx, getState());
		enterRule(_localctx, 100, RULE_error);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(690);
			match(ERROR);
			setState(691);
			match(COLON);
			setState(693); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(692);
				error_arm();
				}
				}
				setState(695); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & 54074093523351568L) != 0) || ((((_la - 73)) & ~0x3f) == 0 && ((1L << (_la - 73)) & 121L) != 0) );
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Error_armContext extends ParserRuleContext {
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public TerminalNode ARROW() { return getToken(JulayParser.ARROW, 0); }
		public Error_armContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_error_arm; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterError_arm(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitError_arm(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitError_arm(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Error_armContext error_arm() throws RecognitionException {
		Error_armContext _localctx = new Error_armContext(_ctx, getState());
		enterRule(_localctx, 102, RULE_error_arm);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(697);
			expr(0);
			setState(698);
			match(ARROW);
			setState(699);
			expr(0);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Var_transitContext extends ParserRuleContext {
		public Field_accessContext field_access() {
			return getRuleContext(Field_accessContext.class,0);
		}
		public TerminalNode ASGN_EQ() { return getToken(JulayParser.ASGN_EQ, 0); }
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public TerminalNode THIS() { return getToken(JulayParser.THIS, 0); }
		public TerminalNode DOT() { return getToken(JulayParser.DOT, 0); }
		public TerminalNode ID() { return getToken(JulayParser.ID, 0); }
		public TerminalNode LBRACK() { return getToken(JulayParser.LBRACK, 0); }
		public TerminalNode RBRACK() { return getToken(JulayParser.RBRACK, 0); }
		public TerminalNode LET() { return getToken(JulayParser.LET, 0); }
		public TerminalNode COLON() { return getToken(JulayParser.COLON, 0); }
		public TypeExprContext typeExpr() {
			return getRuleContext(TypeExprContext.class,0);
		}
		public Var_transitContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_var_transit; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterVar_transit(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitVar_transit(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitVar_transit(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Var_transitContext var_transit() throws RecognitionException {
		Var_transitContext _localctx = new Var_transitContext(_ctx, getState());
		enterRule(_localctx, 104, RULE_var_transit);
		try {
			setState(728);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,71,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(701);
				field_access();
				setState(702);
				match(ASGN_EQ);
				setState(703);
				expr(0);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(705);
				match(THIS);
				setState(706);
				match(DOT);
				setState(707);
				match(ID);
				setState(708);
				match(LBRACK);
				setState(709);
				expr(0);
				setState(710);
				match(RBRACK);
				setState(711);
				match(ASGN_EQ);
				setState(712);
				expr(0);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(714);
				match(ID);
				setState(715);
				match(LBRACK);
				setState(716);
				expr(0);
				setState(717);
				match(RBRACK);
				setState(718);
				match(ASGN_EQ);
				setState(719);
				expr(0);
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(721);
				match(LET);
				setState(722);
				match(ID);
				setState(723);
				match(COLON);
				setState(724);
				typeExpr();
				setState(725);
				match(ASGN_EQ);
				setState(726);
				expr(0);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class BeforeContext extends ParserRuleContext {
		public TerminalNode BEFORE() { return getToken(JulayParser.BEFORE, 0); }
		public TerminalNode COLON() { return getToken(JulayParser.COLON, 0); }
		public List<Call_stmtContext> call_stmt() {
			return getRuleContexts(Call_stmtContext.class);
		}
		public Call_stmtContext call_stmt(int i) {
			return getRuleContext(Call_stmtContext.class,i);
		}
		public BeforeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_before; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterBefore(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitBefore(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitBefore(this);
			else return visitor.visitChildren(this);
		}
	}

	public final BeforeContext before() throws RecognitionException {
		BeforeContext _localctx = new BeforeContext(_ctx, getState());
		enterRule(_localctx, 106, RULE_before);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(730);
			match(BEFORE);
			setState(731);
			match(COLON);
			setState(733); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(732);
				call_stmt();
				}
				}
				setState(735); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==ID );
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class AfterContext extends ParserRuleContext {
		public TerminalNode AFTER() { return getToken(JulayParser.AFTER, 0); }
		public TerminalNode COLON() { return getToken(JulayParser.COLON, 0); }
		public List<Call_stmtContext> call_stmt() {
			return getRuleContexts(Call_stmtContext.class);
		}
		public Call_stmtContext call_stmt(int i) {
			return getRuleContext(Call_stmtContext.class,i);
		}
		public AfterContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_after; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterAfter(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitAfter(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitAfter(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AfterContext after() throws RecognitionException {
		AfterContext _localctx = new AfterContext(_ctx, getState());
		enterRule(_localctx, 108, RULE_after);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(737);
			match(AFTER);
			setState(738);
			match(COLON);
			setState(740); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(739);
				call_stmt();
				}
				}
				setState(742); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==ID );
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Call_stmtContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(JulayParser.ID, 0); }
		public TerminalNode LPAREN() { return getToken(JulayParser.LPAREN, 0); }
		public TerminalNode RPAREN() { return getToken(JulayParser.RPAREN, 0); }
		public TypeArgsContext typeArgs() {
			return getRuleContext(TypeArgsContext.class,0);
		}
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(JulayParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(JulayParser.COMMA, i);
		}
		public Call_stmtContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_call_stmt; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterCall_stmt(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitCall_stmt(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitCall_stmt(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Call_stmtContext call_stmt() throws RecognitionException {
		Call_stmtContext _localctx = new Call_stmtContext(_ctx, getState());
		enterRule(_localctx, 110, RULE_call_stmt);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(744);
			match(ID);
			setState(746);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LT) {
				{
				setState(745);
				typeArgs();
				}
			}

			setState(748);
			match(LPAREN);
			setState(757);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 54074093523351568L) != 0) || ((((_la - 73)) & ~0x3f) == 0 && ((1L << (_la - 73)) & 121L) != 0)) {
				{
				setState(749);
				expr(0);
				setState(754);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(750);
					match(COMMA);
					setState(751);
					expr(0);
					}
					}
					setState(756);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
			}

			setState(759);
			match(RPAREN);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ExprContext extends ParserRuleContext {
		public LiteralContext literal() {
			return getRuleContext(LiteralContext.class,0);
		}
		public TerminalNode LPAREN() { return getToken(JulayParser.LPAREN, 0); }
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public TerminalNode RPAREN() { return getToken(JulayParser.RPAREN, 0); }
		public Collection_literalContext collection_literal() {
			return getRuleContext(Collection_literalContext.class,0);
		}
		public Method_prop_exprContext method_prop_expr() {
			return getRuleContext(Method_prop_exprContext.class,0);
		}
		public Index_exprContext index_expr() {
			return getRuleContext(Index_exprContext.class,0);
		}
		public Field_accessContext field_access() {
			return getRuleContext(Field_accessContext.class,0);
		}
		public Record_literalContext record_literal() {
			return getRuleContext(Record_literalContext.class,0);
		}
		public Fun_callContext fun_call() {
			return getRuleContext(Fun_callContext.class,0);
		}
		public Handler_refContext handler_ref() {
			return getRuleContext(Handler_refContext.class,0);
		}
		public TerminalNode NOT() { return getToken(JulayParser.NOT, 0); }
		public TerminalNode AND() { return getToken(JulayParser.AND, 0); }
		public TerminalNode OR() { return getToken(JulayParser.OR, 0); }
		public TerminalNode IF() { return getToken(JulayParser.IF, 0); }
		public TerminalNode ELSE() { return getToken(JulayParser.ELSE, 0); }
		public List<TerminalNode> LCURLY() { return getTokens(JulayParser.LCURLY); }
		public TerminalNode LCURLY(int i) {
			return getToken(JulayParser.LCURLY, i);
		}
		public List<TerminalNode> RCURLY() { return getTokens(JulayParser.RCURLY); }
		public TerminalNode RCURLY(int i) {
			return getToken(JulayParser.RCURLY, i);
		}
		public TerminalNode LET() { return getToken(JulayParser.LET, 0); }
		public List<TerminalNode> ID() { return getTokens(JulayParser.ID); }
		public TerminalNode ID(int i) {
			return getToken(JulayParser.ID, i);
		}
		public TerminalNode COLON() { return getToken(JulayParser.COLON, 0); }
		public TypeExprContext typeExpr() {
			return getRuleContext(TypeExprContext.class,0);
		}
		public TerminalNode ASGN_EQ() { return getToken(JulayParser.ASGN_EQ, 0); }
		public TerminalNode WHEN() { return getToken(JulayParser.WHEN, 0); }
		public List<When_subject_armContext> when_subject_arm() {
			return getRuleContexts(When_subject_armContext.class);
		}
		public When_subject_armContext when_subject_arm(int i) {
			return getRuleContext(When_subject_armContext.class,i);
		}
		public List<When_guard_armContext> when_guard_arm() {
			return getRuleContexts(When_guard_armContext.class);
		}
		public When_guard_armContext when_guard_arm(int i) {
			return getRuleContext(When_guard_armContext.class,i);
		}
		public TerminalNode FORALL() { return getToken(JulayParser.FORALL, 0); }
		public List<TerminalNode> COMMA() { return getTokens(JulayParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(JulayParser.COMMA, i);
		}
		public TerminalNode EXISTS() { return getToken(JulayParser.EXISTS, 0); }
		public TerminalNode TIMES() { return getToken(JulayParser.TIMES, 0); }
		public TerminalNode DIV() { return getToken(JulayParser.DIV, 0); }
		public TerminalNode MOD() { return getToken(JulayParser.MOD, 0); }
		public TerminalNode PLUS() { return getToken(JulayParser.PLUS, 0); }
		public TerminalNode MINUS() { return getToken(JulayParser.MINUS, 0); }
		public TerminalNode LT() { return getToken(JulayParser.LT, 0); }
		public TerminalNode LTE() { return getToken(JulayParser.LTE, 0); }
		public TerminalNode GT() { return getToken(JulayParser.GT, 0); }
		public TerminalNode GTE() { return getToken(JulayParser.GTE, 0); }
		public TerminalNode IN() { return getToken(JulayParser.IN, 0); }
		public TerminalNode NIN() { return getToken(JulayParser.NIN, 0); }
		public TerminalNode EQ() { return getToken(JulayParser.EQ, 0); }
		public TerminalNode NEQ() { return getToken(JulayParser.NEQ, 0); }
		public TerminalNode IMPLIES() { return getToken(JulayParser.IMPLIES, 0); }
		public TerminalNode IFF() { return getToken(JulayParser.IFF, 0); }
		public ExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitExpr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExprContext expr() throws RecognitionException {
		return expr(0);
	}

	private ExprContext expr(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		ExprContext _localctx = new ExprContext(_ctx, _parentState);
		ExprContext _prevctx = _localctx;
		int _startState = 112;
		enterRecursionRule(_localctx, 112, RULE_expr, _p);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(863);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,84,_ctx) ) {
			case 1:
				{
				setState(762);
				literal();
				}
				break;
			case 2:
				{
				setState(763);
				match(LPAREN);
				setState(764);
				expr(0);
				setState(765);
				match(RPAREN);
				}
				break;
			case 3:
				{
				setState(767);
				collection_literal();
				}
				break;
			case 4:
				{
				setState(768);
				method_prop_expr();
				}
				break;
			case 5:
				{
				setState(769);
				index_expr(0);
				}
				break;
			case 6:
				{
				setState(770);
				field_access();
				}
				break;
			case 7:
				{
				setState(771);
				record_literal();
				}
				break;
			case 8:
				{
				setState(772);
				fun_call();
				}
				break;
			case 9:
				{
				setState(773);
				handler_ref();
				}
				break;
			case 10:
				{
				setState(774);
				match(NOT);
				setState(775);
				expr(26);
				}
				break;
			case 11:
				{
				setState(776);
				match(AND);
				setState(777);
				expr(25);
				}
				break;
			case 12:
				{
				setState(778);
				match(OR);
				setState(779);
				expr(24);
				}
				break;
			case 13:
				{
				setState(780);
				match(IF);
				setState(781);
				match(LPAREN);
				setState(782);
				expr(0);
				setState(783);
				match(RPAREN);
				setState(789);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case LCURLY:
					{
					setState(784);
					match(LCURLY);
					setState(785);
					expr(0);
					setState(786);
					match(RCURLY);
					}
					break;
				case LPAREN:
				case TRUE:
				case FALSE:
				case AND:
				case OR:
				case NOT:
				case IF:
				case LET:
				case WHEN:
				case LISTOF:
				case SETOF:
				case MAPOF:
				case FORALL:
				case EXISTS:
				case THIS:
				case REAL:
				case INT:
				case ID:
				case STRING:
					{
					setState(788);
					expr(0);
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(791);
				match(ELSE);
				setState(797);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case LCURLY:
					{
					setState(792);
					match(LCURLY);
					setState(793);
					expr(0);
					setState(794);
					match(RCURLY);
					}
					break;
				case LPAREN:
				case TRUE:
				case FALSE:
				case AND:
				case OR:
				case NOT:
				case IF:
				case LET:
				case WHEN:
				case LISTOF:
				case SETOF:
				case MAPOF:
				case FORALL:
				case EXISTS:
				case THIS:
				case REAL:
				case INT:
				case ID:
				case STRING:
					{
					setState(796);
					expr(0);
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				}
				break;
			case 14:
				{
				setState(799);
				match(LET);
				setState(800);
				match(LPAREN);
				setState(801);
				match(ID);
				setState(802);
				match(COLON);
				setState(803);
				typeExpr();
				setState(804);
				match(ASGN_EQ);
				setState(805);
				expr(0);
				setState(806);
				match(RPAREN);
				setState(812);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case LCURLY:
					{
					setState(807);
					match(LCURLY);
					setState(808);
					expr(0);
					setState(809);
					match(RCURLY);
					}
					break;
				case LPAREN:
				case TRUE:
				case FALSE:
				case AND:
				case OR:
				case NOT:
				case IF:
				case LET:
				case WHEN:
				case LISTOF:
				case SETOF:
				case MAPOF:
				case FORALL:
				case EXISTS:
				case THIS:
				case REAL:
				case INT:
				case ID:
				case STRING:
					{
					setState(811);
					expr(0);
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				}
				break;
			case 15:
				{
				setState(814);
				match(WHEN);
				setState(815);
				match(LPAREN);
				setState(816);
				expr(0);
				setState(817);
				match(RPAREN);
				setState(818);
				match(LCURLY);
				setState(820); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(819);
					when_subject_arm();
					}
					}
					setState(822); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & 17179875344L) != 0) || ((((_la - 76)) & ~0x3f) == 0 && ((1L << (_la - 76)) & 15L) != 0) );
				setState(824);
				match(RCURLY);
				}
				break;
			case 16:
				{
				setState(826);
				match(WHEN);
				setState(827);
				match(LCURLY);
				setState(829); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(828);
					when_guard_arm();
					}
					}
					setState(831); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & 54074110703220752L) != 0) || ((((_la - 73)) & ~0x3f) == 0 && ((1L << (_la - 73)) & 121L) != 0) );
				setState(833);
				match(RCURLY);
				}
				break;
			case 17:
				{
				setState(835);
				match(FORALL);
				setState(836);
				match(ID);
				setState(841);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(837);
					match(COMMA);
					setState(838);
					match(ID);
					}
					}
					setState(843);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(844);
				match(COLON);
				setState(845);
				typeExpr();
				setState(846);
				match(COMMA);
				setState(847);
				expr(2);
				}
				break;
			case 18:
				{
				setState(849);
				match(EXISTS);
				setState(850);
				match(ID);
				setState(855);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(851);
					match(COMMA);
					setState(852);
					match(ID);
					}
					}
					setState(857);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(858);
				match(COLON);
				setState(859);
				typeExpr();
				setState(860);
				match(COMMA);
				setState(861);
				expr(1);
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(918);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,86,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(916);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,85,_ctx) ) {
					case 1:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(865);
						if (!(precpred(_ctx, 23))) throw new FailedPredicateException(this, "precpred(_ctx, 23)");
						setState(866);
						match(TIMES);
						setState(867);
						expr(24);
						}
						break;
					case 2:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(868);
						if (!(precpred(_ctx, 22))) throw new FailedPredicateException(this, "precpred(_ctx, 22)");
						setState(869);
						match(DIV);
						setState(870);
						expr(23);
						}
						break;
					case 3:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(871);
						if (!(precpred(_ctx, 21))) throw new FailedPredicateException(this, "precpred(_ctx, 21)");
						setState(872);
						match(MOD);
						setState(873);
						expr(22);
						}
						break;
					case 4:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(874);
						if (!(precpred(_ctx, 20))) throw new FailedPredicateException(this, "precpred(_ctx, 20)");
						setState(875);
						match(PLUS);
						setState(876);
						expr(21);
						}
						break;
					case 5:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(877);
						if (!(precpred(_ctx, 19))) throw new FailedPredicateException(this, "precpred(_ctx, 19)");
						setState(878);
						match(MINUS);
						setState(879);
						expr(20);
						}
						break;
					case 6:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(880);
						if (!(precpred(_ctx, 18))) throw new FailedPredicateException(this, "precpred(_ctx, 18)");
						setState(881);
						match(LT);
						setState(882);
						expr(19);
						}
						break;
					case 7:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(883);
						if (!(precpred(_ctx, 17))) throw new FailedPredicateException(this, "precpred(_ctx, 17)");
						setState(884);
						match(LTE);
						setState(885);
						expr(18);
						}
						break;
					case 8:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(886);
						if (!(precpred(_ctx, 16))) throw new FailedPredicateException(this, "precpred(_ctx, 16)");
						setState(887);
						match(GT);
						setState(888);
						expr(17);
						}
						break;
					case 9:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(889);
						if (!(precpred(_ctx, 15))) throw new FailedPredicateException(this, "precpred(_ctx, 15)");
						setState(890);
						match(GTE);
						setState(891);
						expr(16);
						}
						break;
					case 10:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(892);
						if (!(precpred(_ctx, 14))) throw new FailedPredicateException(this, "precpred(_ctx, 14)");
						setState(893);
						match(IN);
						setState(894);
						expr(15);
						}
						break;
					case 11:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(895);
						if (!(precpred(_ctx, 13))) throw new FailedPredicateException(this, "precpred(_ctx, 13)");
						setState(896);
						match(NIN);
						setState(897);
						expr(14);
						}
						break;
					case 12:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(898);
						if (!(precpred(_ctx, 12))) throw new FailedPredicateException(this, "precpred(_ctx, 12)");
						setState(899);
						match(EQ);
						setState(900);
						expr(13);
						}
						break;
					case 13:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(901);
						if (!(precpred(_ctx, 11))) throw new FailedPredicateException(this, "precpred(_ctx, 11)");
						setState(902);
						match(NEQ);
						setState(903);
						expr(12);
						}
						break;
					case 14:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(904);
						if (!(precpred(_ctx, 10))) throw new FailedPredicateException(this, "precpred(_ctx, 10)");
						setState(905);
						match(AND);
						setState(906);
						expr(11);
						}
						break;
					case 15:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(907);
						if (!(precpred(_ctx, 9))) throw new FailedPredicateException(this, "precpred(_ctx, 9)");
						setState(908);
						match(OR);
						setState(909);
						expr(10);
						}
						break;
					case 16:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(910);
						if (!(precpred(_ctx, 8))) throw new FailedPredicateException(this, "precpred(_ctx, 8)");
						setState(911);
						match(IMPLIES);
						setState(912);
						expr(8);
						}
						break;
					case 17:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(913);
						if (!(precpred(_ctx, 7))) throw new FailedPredicateException(this, "precpred(_ctx, 7)");
						setState(914);
						match(IFF);
						setState(915);
						expr(8);
						}
						break;
					}
					} 
				}
				setState(920);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,86,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class When_subject_armContext extends ParserRuleContext {
		public When_patternContext when_pattern() {
			return getRuleContext(When_patternContext.class,0);
		}
		public TerminalNode ARROW() { return getToken(JulayParser.ARROW, 0); }
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public TerminalNode ELSE() { return getToken(JulayParser.ELSE, 0); }
		public When_subject_armContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_when_subject_arm; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterWhen_subject_arm(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitWhen_subject_arm(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitWhen_subject_arm(this);
			else return visitor.visitChildren(this);
		}
	}

	public final When_subject_armContext when_subject_arm() throws RecognitionException {
		When_subject_armContext _localctx = new When_subject_armContext(_ctx, getState());
		enterRule(_localctx, 114, RULE_when_subject_arm);
		try {
			setState(928);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case LPAREN:
			case TRUE:
			case FALSE:
			case REAL:
			case INT:
			case ID:
			case STRING:
				enterOuterAlt(_localctx, 1);
				{
				setState(921);
				when_pattern();
				setState(922);
				match(ARROW);
				setState(923);
				expr(0);
				}
				break;
			case ELSE:
				enterOuterAlt(_localctx, 2);
				{
				setState(925);
				match(ELSE);
				setState(926);
				match(ARROW);
				setState(927);
				expr(0);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class When_guard_armContext extends ParserRuleContext {
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public TerminalNode ARROW() { return getToken(JulayParser.ARROW, 0); }
		public TerminalNode ELSE() { return getToken(JulayParser.ELSE, 0); }
		public When_guard_armContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_when_guard_arm; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterWhen_guard_arm(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitWhen_guard_arm(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitWhen_guard_arm(this);
			else return visitor.visitChildren(this);
		}
	}

	public final When_guard_armContext when_guard_arm() throws RecognitionException {
		When_guard_armContext _localctx = new When_guard_armContext(_ctx, getState());
		enterRule(_localctx, 116, RULE_when_guard_arm);
		try {
			setState(937);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case LPAREN:
			case TRUE:
			case FALSE:
			case AND:
			case OR:
			case NOT:
			case IF:
			case LET:
			case WHEN:
			case LISTOF:
			case SETOF:
			case MAPOF:
			case FORALL:
			case EXISTS:
			case THIS:
			case REAL:
			case INT:
			case ID:
			case STRING:
				enterOuterAlt(_localctx, 1);
				{
				setState(930);
				expr(0);
				setState(931);
				match(ARROW);
				setState(932);
				expr(0);
				}
				break;
			case ELSE:
				enterOuterAlt(_localctx, 2);
				{
				setState(934);
				match(ELSE);
				setState(935);
				match(ARROW);
				setState(936);
				expr(0);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class When_patternContext extends ParserRuleContext {
		public LiteralContext literal() {
			return getRuleContext(LiteralContext.class,0);
		}
		public Record_literalContext record_literal() {
			return getRuleContext(Record_literalContext.class,0);
		}
		public When_patternContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_when_pattern; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterWhen_pattern(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitWhen_pattern(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitWhen_pattern(this);
			else return visitor.visitChildren(this);
		}
	}

	public final When_patternContext when_pattern() throws RecognitionException {
		When_patternContext _localctx = new When_patternContext(_ctx, getState());
		enterRule(_localctx, 118, RULE_when_pattern);
		try {
			setState(941);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case TRUE:
			case FALSE:
			case REAL:
			case INT:
			case STRING:
				enterOuterAlt(_localctx, 1);
				{
				setState(939);
				literal();
				}
				break;
			case LPAREN:
			case ID:
				enterOuterAlt(_localctx, 2);
				{
				setState(940);
				record_literal();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Proc_exprContext extends ParserRuleContext {
		public Qualified_nameContext qualified_name() {
			return getRuleContext(Qualified_nameContext.class,0);
		}
		public TerminalNode ID() { return getToken(JulayParser.ID, 0); }
		public TerminalNode LPAREN() { return getToken(JulayParser.LPAREN, 0); }
		public List<Proc_exprContext> proc_expr() {
			return getRuleContexts(Proc_exprContext.class);
		}
		public Proc_exprContext proc_expr(int i) {
			return getRuleContext(Proc_exprContext.class,i);
		}
		public TerminalNode RPAREN() { return getToken(JulayParser.RPAREN, 0); }
		public TerminalNode PARALLEL() { return getToken(JulayParser.PARALLEL, 0); }
		public Proc_exprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_proc_expr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterProc_expr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitProc_expr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitProc_expr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Proc_exprContext proc_expr() throws RecognitionException {
		return proc_expr(0);
	}

	private Proc_exprContext proc_expr(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		Proc_exprContext _localctx = new Proc_exprContext(_ctx, _parentState);
		Proc_exprContext _prevctx = _localctx;
		int _startState = 120;
		enterRecursionRule(_localctx, 120, RULE_proc_expr, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(950);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,90,_ctx) ) {
			case 1:
				{
				setState(944);
				qualified_name();
				}
				break;
			case 2:
				{
				setState(945);
				match(ID);
				}
				break;
			case 3:
				{
				setState(946);
				match(LPAREN);
				setState(947);
				proc_expr(0);
				setState(948);
				match(RPAREN);
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(957);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,91,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					{
					_localctx = new Proc_exprContext(_parentctx, _parentState);
					pushNewRecursionContext(_localctx, _startState, RULE_proc_expr);
					setState(952);
					if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
					setState(953);
					match(PARALLEL);
					setState(954);
					proc_expr(2);
					}
					} 
				}
				setState(959);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,91,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Handler_refContext extends ParserRuleContext {
		public List<TerminalNode> ID() { return getTokens(JulayParser.ID); }
		public TerminalNode ID(int i) {
			return getToken(JulayParser.ID, i);
		}
		public List<TerminalNode> DOT() { return getTokens(JulayParser.DOT); }
		public TerminalNode DOT(int i) {
			return getToken(JulayParser.DOT, i);
		}
		public TerminalNode PROCFUN_ARROW() { return getToken(JulayParser.PROCFUN_ARROW, 0); }
		public Handler_refContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_handler_ref; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterHandler_ref(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitHandler_ref(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitHandler_ref(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Handler_refContext handler_ref() throws RecognitionException {
		Handler_refContext _localctx = new Handler_refContext(_ctx, getState());
		enterRule(_localctx, 122, RULE_handler_ref);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(960);
			match(ID);
			setState(961);
			match(DOT);
			setState(962);
			match(ID);
			setState(963);
			match(PROCFUN_ARROW);
			setState(964);
			match(ID);
			setState(965);
			match(DOT);
			setState(966);
			match(ID);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class LiteralContext extends ParserRuleContext {
		public TerminalNode INT() { return getToken(JulayParser.INT, 0); }
		public TerminalNode REAL() { return getToken(JulayParser.REAL, 0); }
		public TerminalNode TRUE() { return getToken(JulayParser.TRUE, 0); }
		public TerminalNode FALSE() { return getToken(JulayParser.FALSE, 0); }
		public TerminalNode STRING() { return getToken(JulayParser.STRING, 0); }
		public LiteralContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_literal; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitLiteral(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LiteralContext literal() throws RecognitionException {
		LiteralContext _localctx = new LiteralContext(_ctx, getState());
		enterRule(_localctx, 124, RULE_literal);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(968);
			_la = _input.LA(1);
			if ( !(_la==TRUE || _la==FALSE || ((((_la - 76)) & ~0x3f) == 0 && ((1L << (_la - 76)) & 11L) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Collection_literalContext extends ParserRuleContext {
		public List_literalContext list_literal() {
			return getRuleContext(List_literalContext.class,0);
		}
		public Set_literalContext set_literal() {
			return getRuleContext(Set_literalContext.class,0);
		}
		public Map_literalContext map_literal() {
			return getRuleContext(Map_literalContext.class,0);
		}
		public Collection_literalContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_collection_literal; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterCollection_literal(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitCollection_literal(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitCollection_literal(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Collection_literalContext collection_literal() throws RecognitionException {
		Collection_literalContext _localctx = new Collection_literalContext(_ctx, getState());
		enterRule(_localctx, 126, RULE_collection_literal);
		try {
			setState(973);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case LISTOF:
				enterOuterAlt(_localctx, 1);
				{
				setState(970);
				list_literal();
				}
				break;
			case SETOF:
				enterOuterAlt(_localctx, 2);
				{
				setState(971);
				set_literal();
				}
				break;
			case MAPOF:
				enterOuterAlt(_localctx, 3);
				{
				setState(972);
				map_literal();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class List_literalContext extends ParserRuleContext {
		public TerminalNode LISTOF() { return getToken(JulayParser.LISTOF, 0); }
		public TerminalNode LPAREN() { return getToken(JulayParser.LPAREN, 0); }
		public TerminalNode RPAREN() { return getToken(JulayParser.RPAREN, 0); }
		public TypeArgsContext typeArgs() {
			return getRuleContext(TypeArgsContext.class,0);
		}
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(JulayParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(JulayParser.COMMA, i);
		}
		public List_literalContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_list_literal; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterList_literal(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitList_literal(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitList_literal(this);
			else return visitor.visitChildren(this);
		}
	}

	public final List_literalContext list_literal() throws RecognitionException {
		List_literalContext _localctx = new List_literalContext(_ctx, getState());
		enterRule(_localctx, 128, RULE_list_literal);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(975);
			match(LISTOF);
			setState(977);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LT) {
				{
				setState(976);
				typeArgs();
				}
			}

			setState(979);
			match(LPAREN);
			setState(988);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 54074093523351568L) != 0) || ((((_la - 73)) & ~0x3f) == 0 && ((1L << (_la - 73)) & 121L) != 0)) {
				{
				setState(980);
				expr(0);
				setState(985);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(981);
					match(COMMA);
					setState(982);
					expr(0);
					}
					}
					setState(987);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
			}

			setState(990);
			match(RPAREN);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Set_literalContext extends ParserRuleContext {
		public TerminalNode SETOF() { return getToken(JulayParser.SETOF, 0); }
		public TerminalNode LPAREN() { return getToken(JulayParser.LPAREN, 0); }
		public TerminalNode RPAREN() { return getToken(JulayParser.RPAREN, 0); }
		public TypeArgsContext typeArgs() {
			return getRuleContext(TypeArgsContext.class,0);
		}
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(JulayParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(JulayParser.COMMA, i);
		}
		public Set_literalContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_set_literal; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterSet_literal(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitSet_literal(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitSet_literal(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Set_literalContext set_literal() throws RecognitionException {
		Set_literalContext _localctx = new Set_literalContext(_ctx, getState());
		enterRule(_localctx, 130, RULE_set_literal);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(992);
			match(SETOF);
			setState(994);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LT) {
				{
				setState(993);
				typeArgs();
				}
			}

			setState(996);
			match(LPAREN);
			setState(1005);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 54074093523351568L) != 0) || ((((_la - 73)) & ~0x3f) == 0 && ((1L << (_la - 73)) & 121L) != 0)) {
				{
				setState(997);
				expr(0);
				setState(1002);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(998);
					match(COMMA);
					setState(999);
					expr(0);
					}
					}
					setState(1004);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
			}

			setState(1007);
			match(RPAREN);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Map_literalContext extends ParserRuleContext {
		public TerminalNode MAPOF() { return getToken(JulayParser.MAPOF, 0); }
		public TerminalNode LPAREN() { return getToken(JulayParser.LPAREN, 0); }
		public TerminalNode RPAREN() { return getToken(JulayParser.RPAREN, 0); }
		public TypeArgsContext typeArgs() {
			return getRuleContext(TypeArgsContext.class,0);
		}
		public List<Map_entryContext> map_entry() {
			return getRuleContexts(Map_entryContext.class);
		}
		public Map_entryContext map_entry(int i) {
			return getRuleContext(Map_entryContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(JulayParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(JulayParser.COMMA, i);
		}
		public Map_literalContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_map_literal; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterMap_literal(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitMap_literal(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitMap_literal(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Map_literalContext map_literal() throws RecognitionException {
		Map_literalContext _localctx = new Map_literalContext(_ctx, getState());
		enterRule(_localctx, 132, RULE_map_literal);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1009);
			match(MAPOF);
			setState(1011);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LT) {
				{
				setState(1010);
				typeArgs();
				}
			}

			setState(1013);
			match(LPAREN);
			setState(1022);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 54074093523351568L) != 0) || ((((_la - 73)) & ~0x3f) == 0 && ((1L << (_la - 73)) & 121L) != 0)) {
				{
				setState(1014);
				map_entry();
				setState(1019);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(1015);
					match(COMMA);
					setState(1016);
					map_entry();
					}
					}
					setState(1021);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
			}

			setState(1024);
			match(RPAREN);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Map_entryContext extends ParserRuleContext {
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public TerminalNode TO() { return getToken(JulayParser.TO, 0); }
		public Map_entryContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_map_entry; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterMap_entry(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitMap_entry(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitMap_entry(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Map_entryContext map_entry() throws RecognitionException {
		Map_entryContext _localctx = new Map_entryContext(_ctx, getState());
		enterRule(_localctx, 134, RULE_map_entry);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1026);
			expr(0);
			setState(1027);
			match(TO);
			setState(1028);
			expr(0);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Index_exprContext extends ParserRuleContext {
		public TerminalNode LBRACK() { return getToken(JulayParser.LBRACK, 0); }
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public TerminalNode RBRACK() { return getToken(JulayParser.RBRACK, 0); }
		public Fun_callContext fun_call() {
			return getRuleContext(Fun_callContext.class,0);
		}
		public Field_accessContext field_access() {
			return getRuleContext(Field_accessContext.class,0);
		}
		public Collection_literalContext collection_literal() {
			return getRuleContext(Collection_literalContext.class,0);
		}
		public List<TerminalNode> LPAREN() { return getTokens(JulayParser.LPAREN); }
		public TerminalNode LPAREN(int i) {
			return getToken(JulayParser.LPAREN, i);
		}
		public List<TerminalNode> RPAREN() { return getTokens(JulayParser.RPAREN); }
		public TerminalNode RPAREN(int i) {
			return getToken(JulayParser.RPAREN, i);
		}
		public TerminalNode DOT() { return getToken(JulayParser.DOT, 0); }
		public TerminalNode ID() { return getToken(JulayParser.ID, 0); }
		public List<Call_argContext> call_arg() {
			return getRuleContexts(Call_argContext.class);
		}
		public Call_argContext call_arg(int i) {
			return getRuleContext(Call_argContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(JulayParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(JulayParser.COMMA, i);
		}
		public Index_exprContext index_expr() {
			return getRuleContext(Index_exprContext.class,0);
		}
		public Index_exprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_index_expr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterIndex_expr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitIndex_expr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitIndex_expr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Index_exprContext index_expr() throws RecognitionException {
		return index_expr(0);
	}

	private Index_exprContext index_expr(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		Index_exprContext _localctx = new Index_exprContext(_ctx, _parentState);
		Index_exprContext _prevctx = _localctx;
		int _startState = 136;
		enterRecursionRule(_localctx, 136, RULE_index_expr, _p);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(1079);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,107,_ctx) ) {
			case 1:
				{
				setState(1038);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,102,_ctx) ) {
				case 1:
					{
					setState(1031);
					fun_call();
					}
					break;
				case 2:
					{
					setState(1032);
					field_access();
					}
					break;
				case 3:
					{
					setState(1033);
					collection_literal();
					}
					break;
				case 4:
					{
					setState(1034);
					match(LPAREN);
					setState(1035);
					expr(0);
					setState(1036);
					match(RPAREN);
					}
					break;
				}
				setState(1040);
				match(LBRACK);
				setState(1041);
				expr(0);
				setState(1042);
				match(RBRACK);
				}
				break;
			case 2:
				{
				setState(1051);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,103,_ctx) ) {
				case 1:
					{
					setState(1044);
					fun_call();
					}
					break;
				case 2:
					{
					setState(1045);
					field_access();
					}
					break;
				case 3:
					{
					setState(1046);
					collection_literal();
					}
					break;
				case 4:
					{
					setState(1047);
					match(LPAREN);
					setState(1048);
					expr(0);
					setState(1049);
					match(RPAREN);
					}
					break;
				}
				setState(1053);
				match(DOT);
				setState(1054);
				match(ID);
				setState(1055);
				match(LPAREN);
				setState(1064);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 54074093523351568L) != 0) || ((((_la - 73)) & ~0x3f) == 0 && ((1L << (_la - 73)) & 121L) != 0)) {
					{
					setState(1056);
					call_arg();
					setState(1061);
					_errHandler.sync(this);
					_la = _input.LA(1);
					while (_la==COMMA) {
						{
						{
						setState(1057);
						match(COMMA);
						setState(1058);
						call_arg();
						}
						}
						setState(1063);
						_errHandler.sync(this);
						_la = _input.LA(1);
					}
					}
				}

				setState(1066);
				match(RPAREN);
				}
				break;
			case 3:
				{
				setState(1074);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case ID:
					{
					setState(1068);
					fun_call();
					}
					break;
				case LISTOF:
				case SETOF:
				case MAPOF:
					{
					setState(1069);
					collection_literal();
					}
					break;
				case LPAREN:
					{
					setState(1070);
					match(LPAREN);
					setState(1071);
					expr(0);
					setState(1072);
					match(RPAREN);
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(1076);
				match(DOT);
				setState(1077);
				match(ID);
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(1106);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,111,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(1104);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,110,_ctx) ) {
					case 1:
						{
						_localctx = new Index_exprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_index_expr);
						setState(1081);
						if (!(precpred(_ctx, 6))) throw new FailedPredicateException(this, "precpred(_ctx, 6)");
						setState(1082);
						match(LBRACK);
						setState(1083);
						expr(0);
						setState(1084);
						match(RBRACK);
						}
						break;
					case 2:
						{
						_localctx = new Index_exprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_index_expr);
						setState(1086);
						if (!(precpred(_ctx, 5))) throw new FailedPredicateException(this, "precpred(_ctx, 5)");
						setState(1087);
						match(DOT);
						setState(1088);
						match(ID);
						setState(1089);
						match(LPAREN);
						setState(1098);
						_errHandler.sync(this);
						_la = _input.LA(1);
						if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 54074093523351568L) != 0) || ((((_la - 73)) & ~0x3f) == 0 && ((1L << (_la - 73)) & 121L) != 0)) {
							{
							setState(1090);
							call_arg();
							setState(1095);
							_errHandler.sync(this);
							_la = _input.LA(1);
							while (_la==COMMA) {
								{
								{
								setState(1091);
								match(COMMA);
								setState(1092);
								call_arg();
								}
								}
								setState(1097);
								_errHandler.sync(this);
								_la = _input.LA(1);
							}
							}
						}

						setState(1100);
						match(RPAREN);
						}
						break;
					case 3:
						{
						_localctx = new Index_exprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_index_expr);
						setState(1101);
						if (!(precpred(_ctx, 4))) throw new FailedPredicateException(this, "precpred(_ctx, 4)");
						setState(1102);
						match(DOT);
						setState(1103);
						match(ID);
						}
						break;
					}
					} 
				}
				setState(1108);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,111,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Method_prop_exprContext extends ParserRuleContext {
		public Method_callContext method_call() {
			return getRuleContext(Method_callContext.class,0);
		}
		public List<TerminalNode> DOT() { return getTokens(JulayParser.DOT); }
		public TerminalNode DOT(int i) {
			return getToken(JulayParser.DOT, i);
		}
		public List<TerminalNode> ID() { return getTokens(JulayParser.ID); }
		public TerminalNode ID(int i) {
			return getToken(JulayParser.ID, i);
		}
		public Method_prop_exprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_method_prop_expr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterMethod_prop_expr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitMethod_prop_expr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitMethod_prop_expr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Method_prop_exprContext method_prop_expr() throws RecognitionException {
		Method_prop_exprContext _localctx = new Method_prop_exprContext(_ctx, getState());
		enterRule(_localctx, 138, RULE_method_prop_expr);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(1109);
			method_call();
			setState(1114);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,112,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(1110);
					match(DOT);
					setState(1111);
					match(ID);
					}
					} 
				}
				setState(1116);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,112,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Method_callContext extends ParserRuleContext {
		public TerminalNode THIS() { return getToken(JulayParser.THIS, 0); }
		public TerminalNode LPAREN() { return getToken(JulayParser.LPAREN, 0); }
		public TerminalNode RPAREN() { return getToken(JulayParser.RPAREN, 0); }
		public List<TerminalNode> DOT() { return getTokens(JulayParser.DOT); }
		public TerminalNode DOT(int i) {
			return getToken(JulayParser.DOT, i);
		}
		public List<TerminalNode> ID() { return getTokens(JulayParser.ID); }
		public TerminalNode ID(int i) {
			return getToken(JulayParser.ID, i);
		}
		public List<Call_argContext> call_arg() {
			return getRuleContexts(Call_argContext.class);
		}
		public Call_argContext call_arg(int i) {
			return getRuleContext(Call_argContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(JulayParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(JulayParser.COMMA, i);
		}
		public Method_callContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_method_call; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterMethod_call(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitMethod_call(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitMethod_call(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Method_callContext method_call() throws RecognitionException {
		Method_callContext _localctx = new Method_callContext(_ctx, getState());
		enterRule(_localctx, 140, RULE_method_call);
		int _la;
		try {
			setState(1155);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case THIS:
				enterOuterAlt(_localctx, 1);
				{
				setState(1117);
				match(THIS);
				setState(1120); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(1118);
					match(DOT);
					setState(1119);
					match(ID);
					}
					}
					setState(1122); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( _la==DOT );
				setState(1124);
				match(LPAREN);
				setState(1133);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 54074093523351568L) != 0) || ((((_la - 73)) & ~0x3f) == 0 && ((1L << (_la - 73)) & 121L) != 0)) {
					{
					setState(1125);
					call_arg();
					setState(1130);
					_errHandler.sync(this);
					_la = _input.LA(1);
					while (_la==COMMA) {
						{
						{
						setState(1126);
						match(COMMA);
						setState(1127);
						call_arg();
						}
						}
						setState(1132);
						_errHandler.sync(this);
						_la = _input.LA(1);
					}
					}
				}

				setState(1135);
				match(RPAREN);
				}
				break;
			case ID:
				enterOuterAlt(_localctx, 2);
				{
				setState(1136);
				match(ID);
				setState(1139); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(1137);
					match(DOT);
					setState(1138);
					match(ID);
					}
					}
					setState(1141); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( _la==DOT );
				setState(1143);
				match(LPAREN);
				setState(1152);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 54074093523351568L) != 0) || ((((_la - 73)) & ~0x3f) == 0 && ((1L << (_la - 73)) & 121L) != 0)) {
					{
					setState(1144);
					call_arg();
					setState(1149);
					_errHandler.sync(this);
					_la = _input.LA(1);
					while (_la==COMMA) {
						{
						{
						setState(1145);
						match(COMMA);
						setState(1146);
						call_arg();
						}
						}
						setState(1151);
						_errHandler.sync(this);
						_la = _input.LA(1);
					}
					}
				}

				setState(1154);
				match(RPAREN);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Fun_callContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(JulayParser.ID, 0); }
		public TerminalNode LPAREN() { return getToken(JulayParser.LPAREN, 0); }
		public TerminalNode RPAREN() { return getToken(JulayParser.RPAREN, 0); }
		public TypeArgsContext typeArgs() {
			return getRuleContext(TypeArgsContext.class,0);
		}
		public List<Call_argContext> call_arg() {
			return getRuleContexts(Call_argContext.class);
		}
		public Call_argContext call_arg(int i) {
			return getRuleContext(Call_argContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(JulayParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(JulayParser.COMMA, i);
		}
		public Fun_callContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fun_call; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterFun_call(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitFun_call(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitFun_call(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Fun_callContext fun_call() throws RecognitionException {
		Fun_callContext _localctx = new Fun_callContext(_ctx, getState());
		enterRule(_localctx, 142, RULE_fun_call);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1157);
			match(ID);
			setState(1159);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LT) {
				{
				setState(1158);
				typeArgs();
				}
			}

			setState(1161);
			match(LPAREN);
			setState(1170);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 54074093523351568L) != 0) || ((((_la - 73)) & ~0x3f) == 0 && ((1L << (_la - 73)) & 121L) != 0)) {
				{
				setState(1162);
				call_arg();
				setState(1167);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(1163);
					match(COMMA);
					setState(1164);
					call_arg();
					}
					}
					setState(1169);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
			}

			setState(1172);
			match(RPAREN);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Call_argContext extends ParserRuleContext {
		public Lambda_exprContext lambda_expr() {
			return getRuleContext(Lambda_exprContext.class,0);
		}
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public Call_argContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_call_arg; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterCall_arg(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitCall_arg(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitCall_arg(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Call_argContext call_arg() throws RecognitionException {
		Call_argContext _localctx = new Call_argContext(_ctx, getState());
		enterRule(_localctx, 144, RULE_call_arg);
		try {
			setState(1176);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,123,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1174);
				lambda_expr();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1175);
				expr(0);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Lambda_exprContext extends ParserRuleContext {
		public List<TerminalNode> ID() { return getTokens(JulayParser.ID); }
		public TerminalNode ID(int i) {
			return getToken(JulayParser.ID, i);
		}
		public TerminalNode ARROW() { return getToken(JulayParser.ARROW, 0); }
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public TerminalNode LPAREN() { return getToken(JulayParser.LPAREN, 0); }
		public TerminalNode COMMA() { return getToken(JulayParser.COMMA, 0); }
		public TerminalNode RPAREN() { return getToken(JulayParser.RPAREN, 0); }
		public Lambda_exprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_lambda_expr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterLambda_expr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitLambda_expr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitLambda_expr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Lambda_exprContext lambda_expr() throws RecognitionException {
		Lambda_exprContext _localctx = new Lambda_exprContext(_ctx, getState());
		enterRule(_localctx, 146, RULE_lambda_expr);
		try {
			setState(1188);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ID:
				enterOuterAlt(_localctx, 1);
				{
				setState(1178);
				match(ID);
				setState(1179);
				match(ARROW);
				setState(1180);
				expr(0);
				}
				break;
			case LPAREN:
				enterOuterAlt(_localctx, 2);
				{
				setState(1181);
				match(LPAREN);
				setState(1182);
				match(ID);
				setState(1183);
				match(COMMA);
				setState(1184);
				match(ID);
				setState(1185);
				match(RPAREN);
				setState(1186);
				match(ARROW);
				setState(1187);
				expr(0);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Record_literalContext extends ParserRuleContext {
		public TypeExprContext typeExpr() {
			return getRuleContext(TypeExprContext.class,0);
		}
		public TerminalNode LCURLY() { return getToken(JulayParser.LCURLY, 0); }
		public List<Record_field_assignContext> record_field_assign() {
			return getRuleContexts(Record_field_assignContext.class);
		}
		public Record_field_assignContext record_field_assign(int i) {
			return getRuleContext(Record_field_assignContext.class,i);
		}
		public TerminalNode RCURLY() { return getToken(JulayParser.RCURLY, 0); }
		public List<TerminalNode> COMMA() { return getTokens(JulayParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(JulayParser.COMMA, i);
		}
		public Record_literalContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_record_literal; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterRecord_literal(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitRecord_literal(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitRecord_literal(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Record_literalContext record_literal() throws RecognitionException {
		Record_literalContext _localctx = new Record_literalContext(_ctx, getState());
		enterRule(_localctx, 148, RULE_record_literal);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1190);
			typeExpr();
			setState(1191);
			match(LCURLY);
			setState(1192);
			record_field_assign();
			setState(1197);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(1193);
				match(COMMA);
				setState(1194);
				record_field_assign();
				}
				}
				setState(1199);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(1200);
			match(RCURLY);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Record_field_assignContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(JulayParser.ID, 0); }
		public TerminalNode ASGN_EQ() { return getToken(JulayParser.ASGN_EQ, 0); }
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public Record_field_assignContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_record_field_assign; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterRecord_field_assign(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitRecord_field_assign(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitRecord_field_assign(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Record_field_assignContext record_field_assign() throws RecognitionException {
		Record_field_assignContext _localctx = new Record_field_assignContext(_ctx, getState());
		enterRule(_localctx, 150, RULE_record_field_assign);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1202);
			match(ID);
			setState(1203);
			match(ASGN_EQ);
			setState(1204);
			expr(0);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Field_accessContext extends ParserRuleContext {
		public TerminalNode THIS() { return getToken(JulayParser.THIS, 0); }
		public List<TerminalNode> DOT() { return getTokens(JulayParser.DOT); }
		public TerminalNode DOT(int i) {
			return getToken(JulayParser.DOT, i);
		}
		public List<TerminalNode> ID() { return getTokens(JulayParser.ID); }
		public TerminalNode ID(int i) {
			return getToken(JulayParser.ID, i);
		}
		public Field_accessContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_field_access; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterField_access(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitField_access(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitField_access(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Field_accessContext field_access() throws RecognitionException {
		Field_accessContext _localctx = new Field_accessContext(_ctx, getState());
		enterRule(_localctx, 152, RULE_field_access);
		try {
			int _alt;
			setState(1224);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case THIS:
				enterOuterAlt(_localctx, 1);
				{
				setState(1206);
				match(THIS);
				setState(1207);
				match(DOT);
				setState(1208);
				match(ID);
				setState(1213);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,126,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(1209);
						match(DOT);
						setState(1210);
						match(ID);
						}
						} 
					}
					setState(1215);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,126,_ctx);
				}
				}
				break;
			case ID:
				enterOuterAlt(_localctx, 2);
				{
				setState(1216);
				match(ID);
				setState(1221);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,127,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(1217);
						match(DOT);
						setState(1218);
						match(ID);
						}
						} 
					}
					setState(1223);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,127,_ctx);
				}
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public boolean sempred(RuleContext _localctx, int ruleIndex, int predIndex) {
		switch (ruleIndex) {
		case 21:
			return system_expr_sempred((System_exprContext)_localctx, predIndex);
		case 56:
			return expr_sempred((ExprContext)_localctx, predIndex);
		case 60:
			return proc_expr_sempred((Proc_exprContext)_localctx, predIndex);
		case 68:
			return index_expr_sempred((Index_exprContext)_localctx, predIndex);
		}
		return true;
	}
	private boolean system_expr_sempred(System_exprContext _localctx, int predIndex) {
		switch (predIndex) {
		case 0:
			return precpred(_ctx, 3);
		}
		return true;
	}
	private boolean expr_sempred(ExprContext _localctx, int predIndex) {
		switch (predIndex) {
		case 1:
			return precpred(_ctx, 23);
		case 2:
			return precpred(_ctx, 22);
		case 3:
			return precpred(_ctx, 21);
		case 4:
			return precpred(_ctx, 20);
		case 5:
			return precpred(_ctx, 19);
		case 6:
			return precpred(_ctx, 18);
		case 7:
			return precpred(_ctx, 17);
		case 8:
			return precpred(_ctx, 16);
		case 9:
			return precpred(_ctx, 15);
		case 10:
			return precpred(_ctx, 14);
		case 11:
			return precpred(_ctx, 13);
		case 12:
			return precpred(_ctx, 12);
		case 13:
			return precpred(_ctx, 11);
		case 14:
			return precpred(_ctx, 10);
		case 15:
			return precpred(_ctx, 9);
		case 16:
			return precpred(_ctx, 8);
		case 17:
			return precpred(_ctx, 7);
		}
		return true;
	}
	private boolean proc_expr_sempred(Proc_exprContext _localctx, int predIndex) {
		switch (predIndex) {
		case 18:
			return precpred(_ctx, 1);
		}
		return true;
	}
	private boolean index_expr_sempred(Index_exprContext _localctx, int predIndex) {
		switch (predIndex) {
		case 19:
			return precpred(_ctx, 6);
		case 20:
			return precpred(_ctx, 5);
		case 21:
			return precpred(_ctx, 4);
		}
		return true;
	}

	public static final String _serializedATN =
		"\u0004\u0001R\u04cb\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001\u0002"+
		"\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004\u0002"+
		"\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007\u0007\u0007\u0002"+
		"\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0002\u000b\u0007\u000b\u0002"+
		"\f\u0007\f\u0002\r\u0007\r\u0002\u000e\u0007\u000e\u0002\u000f\u0007\u000f"+
		"\u0002\u0010\u0007\u0010\u0002\u0011\u0007\u0011\u0002\u0012\u0007\u0012"+
		"\u0002\u0013\u0007\u0013\u0002\u0014\u0007\u0014\u0002\u0015\u0007\u0015"+
		"\u0002\u0016\u0007\u0016\u0002\u0017\u0007\u0017\u0002\u0018\u0007\u0018"+
		"\u0002\u0019\u0007\u0019\u0002\u001a\u0007\u001a\u0002\u001b\u0007\u001b"+
		"\u0002\u001c\u0007\u001c\u0002\u001d\u0007\u001d\u0002\u001e\u0007\u001e"+
		"\u0002\u001f\u0007\u001f\u0002 \u0007 \u0002!\u0007!\u0002\"\u0007\"\u0002"+
		"#\u0007#\u0002$\u0007$\u0002%\u0007%\u0002&\u0007&\u0002\'\u0007\'\u0002"+
		"(\u0007(\u0002)\u0007)\u0002*\u0007*\u0002+\u0007+\u0002,\u0007,\u0002"+
		"-\u0007-\u0002.\u0007.\u0002/\u0007/\u00020\u00070\u00021\u00071\u0002"+
		"2\u00072\u00023\u00073\u00024\u00074\u00025\u00075\u00026\u00076\u0002"+
		"7\u00077\u00028\u00078\u00029\u00079\u0002:\u0007:\u0002;\u0007;\u0002"+
		"<\u0007<\u0002=\u0007=\u0002>\u0007>\u0002?\u0007?\u0002@\u0007@\u0002"+
		"A\u0007A\u0002B\u0007B\u0002C\u0007C\u0002D\u0007D\u0002E\u0007E\u0002"+
		"F\u0007F\u0002G\u0007G\u0002H\u0007H\u0002I\u0007I\u0002J\u0007J\u0002"+
		"K\u0007K\u0002L\u0007L\u0001\u0000\u0001\u0000\u0005\u0000\u009d\b\u0000"+
		"\n\u0000\f\u0000\u00a0\t\u0000\u0001\u0000\u0001\u0000\u0001\u0001\u0001"+
		"\u0001\u0001\u0001\u0001\u0002\u0001\u0002\u0001\u0002\u0004\u0002\u00aa"+
		"\b\u0002\u000b\u0002\f\u0002\u00ab\u0001\u0003\u0001\u0003\u0001\u0004"+
		"\u0003\u0004\u00b1\b\u0004\u0001\u0004\u0001\u0004\u0003\u0004\u00b5\b"+
		"\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0003\u0004\u00ba\b\u0004\u0001"+
		"\u0004\u0001\u0004\u0003\u0004\u00be\b\u0004\u0001\u0004\u0001\u0004\u0003"+
		"\u0004\u00c2\b\u0004\u0001\u0004\u0001\u0004\u0003\u0004\u00c6\b\u0004"+
		"\u0001\u0004\u0003\u0004\u00c9\b\u0004\u0001\u0005\u0001\u0005\u0001\u0006"+
		"\u0001\u0006\u0001\u0006\u0001\u0006\u0004\u0006\u00d1\b\u0006\u000b\u0006"+
		"\f\u0006\u00d2\u0001\u0006\u0001\u0006\u0001\u0006\u0003\u0006\u00d8\b"+
		"\u0006\u0001\u0007\u0001\u0007\u0003\u0007\u00dc\b\u0007\u0001\u0007\u0001"+
		"\u0007\u0001\u0007\u0001\u0007\u0003\u0007\u00e2\b\u0007\u0001\b\u0001"+
		"\b\u0001\b\u0001\b\u0005\b\u00e8\b\b\n\b\f\b\u00eb\t\b\u0001\b\u0001\b"+
		"\u0001\t\u0001\t\u0001\t\u0001\t\u0005\t\u00f3\b\t\n\t\f\t\u00f6\t\t\u0001"+
		"\t\u0001\t\u0001\n\u0001\n\u0001\n\u0003\n\u00fd\b\n\u0001\n\u0001\n\u0001"+
		"\n\u0001\n\u0001\n\u0001\n\u0001\u000b\u0003\u000b\u0106\b\u000b\u0001"+
		"\u000b\u0001\u000b\u0001\u000b\u0001\u000b\u0005\u000b\u010c\b\u000b\n"+
		"\u000b\f\u000b\u010f\t\u000b\u0001\u000b\u0001\u000b\u0001\u000b\u0001"+
		"\u000b\u0001\u000b\u0003\u000b\u0116\b\u000b\u0001\f\u0001\f\u0001\f\u0003"+
		"\f\u011b\b\f\u0001\f\u0001\f\u0005\f\u011f\b\f\n\f\f\f\u0122\t\f\u0001"+
		"\f\u0001\f\u0001\f\u0001\f\u0001\f\u0001\f\u0001\f\u0003\f\u012b\b\f\u0001"+
		"\r\u0001\r\u0001\r\u0001\r\u0001\r\u0001\r\u0005\r\u0133\b\r\n\r\f\r\u0136"+
		"\t\r\u0001\r\u0001\r\u0001\u000e\u0001\u000e\u0001\u000e\u0001\u000e\u0001"+
		"\u000f\u0001\u000f\u0001\u000f\u0001\u000f\u0005\u000f\u0142\b\u000f\n"+
		"\u000f\f\u000f\u0145\t\u000f\u0001\u0010\u0003\u0010\u0148\b\u0010\u0001"+
		"\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0001"+
		"\u0010\u0001\u0010\u0003\u0010\u0152\b\u0010\u0001\u0010\u0001\u0010\u0005"+
		"\u0010\u0156\b\u0010\n\u0010\f\u0010\u0159\t\u0010\u0001\u0010\u0001\u0010"+
		"\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010"+
		"\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010"+
		"\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010"+
		"\u0001\u0010\u0001\u0010\u0003\u0010\u0171\b\u0010\u0001\u0011\u0001\u0011"+
		"\u0001\u0011\u0003\u0011\u0176\b\u0011\u0001\u0012\u0001\u0012\u0001\u0012"+
		"\u0001\u0012\u0001\u0012\u0001\u0012\u0001\u0012\u0001\u0012\u0001\u0013"+
		"\u0001\u0013\u0001\u0013\u0003\u0013\u0183\b\u0013\u0001\u0013\u0001\u0013"+
		"\u0001\u0013\u0001\u0013\u0003\u0013\u0189\b\u0013\u0001\u0014\u0001\u0014"+
		"\u0001\u0014\u0001\u0014\u0003\u0014\u018f\b\u0014\u0001\u0015\u0001\u0015"+
		"\u0001\u0015\u0003\u0015\u0194\b\u0015\u0001\u0015\u0001\u0015\u0001\u0015"+
		"\u0005\u0015\u0199\b\u0015\n\u0015\f\u0015\u019c\t\u0015\u0001\u0016\u0001"+
		"\u0016\u0001\u0016\u0001\u0016\u0001\u0016\u0001\u0016\u0001\u0016\u0001"+
		"\u0016\u0001\u0016\u0001\u0016\u0001\u0017\u0001\u0017\u0001\u0017\u0001"+
		"\u0017\u0001\u0017\u0001\u0017\u0001\u0017\u0001\u0017\u0005\u0017\u01b0"+
		"\b\u0017\n\u0017\f\u0017\u01b3\t\u0017\u0001\u0017\u0003\u0017\u01b6\b"+
		"\u0017\u0001\u0017\u0001\u0017\u0001\u0017\u0001\u0017\u0001\u0017\u0001"+
		"\u0017\u0003\u0017\u01be\b\u0017\u0001\u0018\u0001\u0018\u0001\u0018\u0001"+
		"\u0018\u0003\u0018\u01c4\b\u0018\u0001\u0019\u0003\u0019\u01c7\b\u0019"+
		"\u0001\u0019\u0001\u0019\u0001\u0019\u0001\u0019\u0005\u0019\u01cd\b\u0019"+
		"\n\u0019\f\u0019\u01d0\t\u0019\u0001\u001a\u0001\u001a\u0001\u001a\u0001"+
		"\u001a\u0001\u001b\u0001\u001b\u0001\u001b\u0001\u001b\u0001\u001b\u0003"+
		"\u001b\u01db\b\u001b\u0001\u001c\u0001\u001c\u0003\u001c\u01df\b\u001c"+
		"\u0001\u001d\u0001\u001d\u0001\u001d\u0001\u001d\u0001\u001d\u0001\u001e"+
		"\u0001\u001e\u0001\u001e\u0003\u001e\u01e9\b\u001e\u0001\u001e\u0001\u001e"+
		"\u0004\u001e\u01ed\b\u001e\u000b\u001e\f\u001e\u01ee\u0001\u001e\u0001"+
		"\u001e\u0001\u001f\u0001\u001f\u0001\u001f\u0001\u001f\u0005\u001f\u01f7"+
		"\b\u001f\n\u001f\f\u001f\u01fa\t\u001f\u0001\u001f\u0001\u001f\u0001 "+
		"\u0001 \u0001 \u0001 \u0001!\u0001!\u0001!\u0001!\u0001!\u0001!\u0001"+
		"!\u0003!\u0209\b!\u0001!\u0001!\u0001!\u0001!\u0001!\u0001!\u0003!\u0211"+
		"\b!\u0001\"\u0001\"\u0001\"\u0001\"\u0005\"\u0217\b\"\n\"\f\"\u021a\t"+
		"\"\u0003\"\u021c\b\"\u0001#\u0001#\u0001$\u0001$\u0001$\u0001$\u0001$"+
		"\u0001$\u0001$\u0001$\u0005$\u0228\b$\n$\f$\u022b\t$\u0001$\u0001$\u0003"+
		"$\u022f\b$\u0001$\u0001$\u0001$\u0001$\u0001$\u0005$\u0236\b$\n$\f$\u0239"+
		"\t$\u0001$\u0001$\u0003$\u023d\b$\u0003$\u023f\b$\u0001%\u0001%\u0001"+
		"%\u0001%\u0003%\u0245\b%\u0001&\u0001&\u0001&\u0001&\u0001\'\u0001\'\u0001"+
		"\'\u0001\'\u0001\'\u0001\'\u0003\'\u0251\b\'\u0001(\u0001(\u0001(\u0001"+
		"(\u0001(\u0003(\u0258\b(\u0001(\u0001(\u0005(\u025c\b(\n(\f(\u025f\t("+
		"\u0001(\u0001(\u0001)\u0003)\u0264\b)\u0001)\u0001)\u0001)\u0001)\u0001"+
		")\u0003)\u026b\b)\u0001)\u0001)\u0005)\u026f\b)\n)\f)\u0272\t)\u0001)"+
		"\u0001)\u0001*\u0003*\u0277\b*\u0001*\u0001*\u0001*\u0001*\u0001*\u0005"+
		"*\u027e\b*\n*\f*\u0281\t*\u0001*\u0001*\u0001+\u0001+\u0003+\u0287\b+"+
		"\u0001,\u0001,\u0003,\u028b\b,\u0001,\u0001,\u0005,\u028f\b,\n,\f,\u0292"+
		"\t,\u0001,\u0001,\u0001-\u0001-\u0001-\u0001-\u0001.\u0001.\u0001.\u0001"+
		".\u0003.\u029e\b.\u0001/\u0001/\u0001/\u0001/\u0001/\u0003/\u02a5\b/\u0001"+
		"0\u00010\u00010\u00010\u00011\u00011\u00011\u00051\u02ae\b1\n1\f1\u02b1"+
		"\t1\u00012\u00012\u00012\u00042\u02b6\b2\u000b2\f2\u02b7\u00013\u0001"+
		"3\u00013\u00013\u00014\u00014\u00014\u00014\u00014\u00014\u00014\u0001"+
		"4\u00014\u00014\u00014\u00014\u00014\u00014\u00014\u00014\u00014\u0001"+
		"4\u00014\u00014\u00014\u00014\u00014\u00014\u00014\u00014\u00014\u0003"+
		"4\u02d9\b4\u00015\u00015\u00015\u00045\u02de\b5\u000b5\f5\u02df\u0001"+
		"6\u00016\u00016\u00046\u02e5\b6\u000b6\f6\u02e6\u00017\u00017\u00037\u02eb"+
		"\b7\u00017\u00017\u00017\u00017\u00057\u02f1\b7\n7\f7\u02f4\t7\u00037"+
		"\u02f6\b7\u00017\u00017\u00018\u00018\u00018\u00018\u00018\u00018\u0001"+
		"8\u00018\u00018\u00018\u00018\u00018\u00018\u00018\u00018\u00018\u0001"+
		"8\u00018\u00018\u00018\u00018\u00018\u00018\u00018\u00018\u00018\u0001"+
		"8\u00018\u00038\u0316\b8\u00018\u00018\u00018\u00018\u00018\u00018\u0003"+
		"8\u031e\b8\u00018\u00018\u00018\u00018\u00018\u00018\u00018\u00018\u0001"+
		"8\u00018\u00018\u00018\u00018\u00038\u032d\b8\u00018\u00018\u00018\u0001"+
		"8\u00018\u00018\u00048\u0335\b8\u000b8\f8\u0336\u00018\u00018\u00018\u0001"+
		"8\u00018\u00048\u033e\b8\u000b8\f8\u033f\u00018\u00018\u00018\u00018\u0001"+
		"8\u00018\u00058\u0348\b8\n8\f8\u034b\t8\u00018\u00018\u00018\u00018\u0001"+
		"8\u00018\u00018\u00018\u00018\u00058\u0356\b8\n8\f8\u0359\t8\u00018\u0001"+
		"8\u00018\u00018\u00018\u00038\u0360\b8\u00018\u00018\u00018\u00018\u0001"+
		"8\u00018\u00018\u00018\u00018\u00018\u00018\u00018\u00018\u00018\u0001"+
		"8\u00018\u00018\u00018\u00018\u00018\u00018\u00018\u00018\u00018\u0001"+
		"8\u00018\u00018\u00018\u00018\u00018\u00018\u00018\u00018\u00018\u0001"+
		"8\u00018\u00018\u00018\u00018\u00018\u00018\u00018\u00018\u00018\u0001"+
		"8\u00018\u00018\u00018\u00018\u00018\u00018\u00058\u0395\b8\n8\f8\u0398"+
		"\t8\u00019\u00019\u00019\u00019\u00019\u00019\u00019\u00039\u03a1\b9\u0001"+
		":\u0001:\u0001:\u0001:\u0001:\u0001:\u0001:\u0003:\u03aa\b:\u0001;\u0001"+
		";\u0003;\u03ae\b;\u0001<\u0001<\u0001<\u0001<\u0001<\u0001<\u0001<\u0003"+
		"<\u03b7\b<\u0001<\u0001<\u0001<\u0005<\u03bc\b<\n<\f<\u03bf\t<\u0001="+
		"\u0001=\u0001=\u0001=\u0001=\u0001=\u0001=\u0001=\u0001>\u0001>\u0001"+
		"?\u0001?\u0001?\u0003?\u03ce\b?\u0001@\u0001@\u0003@\u03d2\b@\u0001@\u0001"+
		"@\u0001@\u0001@\u0005@\u03d8\b@\n@\f@\u03db\t@\u0003@\u03dd\b@\u0001@"+
		"\u0001@\u0001A\u0001A\u0003A\u03e3\bA\u0001A\u0001A\u0001A\u0001A\u0005"+
		"A\u03e9\bA\nA\fA\u03ec\tA\u0003A\u03ee\bA\u0001A\u0001A\u0001B\u0001B"+
		"\u0003B\u03f4\bB\u0001B\u0001B\u0001B\u0001B\u0005B\u03fa\bB\nB\fB\u03fd"+
		"\tB\u0003B\u03ff\bB\u0001B\u0001B\u0001C\u0001C\u0001C\u0001C\u0001D\u0001"+
		"D\u0001D\u0001D\u0001D\u0001D\u0001D\u0001D\u0003D\u040f\bD\u0001D\u0001"+
		"D\u0001D\u0001D\u0001D\u0001D\u0001D\u0001D\u0001D\u0001D\u0001D\u0003"+
		"D\u041c\bD\u0001D\u0001D\u0001D\u0001D\u0001D\u0001D\u0005D\u0424\bD\n"+
		"D\fD\u0427\tD\u0003D\u0429\bD\u0001D\u0001D\u0001D\u0001D\u0001D\u0001"+
		"D\u0001D\u0001D\u0003D\u0433\bD\u0001D\u0001D\u0001D\u0003D\u0438\bD\u0001"+
		"D\u0001D\u0001D\u0001D\u0001D\u0001D\u0001D\u0001D\u0001D\u0001D\u0001"+
		"D\u0001D\u0005D\u0446\bD\nD\fD\u0449\tD\u0003D\u044b\bD\u0001D\u0001D"+
		"\u0001D\u0001D\u0005D\u0451\bD\nD\fD\u0454\tD\u0001E\u0001E\u0001E\u0005"+
		"E\u0459\bE\nE\fE\u045c\tE\u0001F\u0001F\u0001F\u0004F\u0461\bF\u000bF"+
		"\fF\u0462\u0001F\u0001F\u0001F\u0001F\u0005F\u0469\bF\nF\fF\u046c\tF\u0003"+
		"F\u046e\bF\u0001F\u0001F\u0001F\u0001F\u0004F\u0474\bF\u000bF\fF\u0475"+
		"\u0001F\u0001F\u0001F\u0001F\u0005F\u047c\bF\nF\fF\u047f\tF\u0003F\u0481"+
		"\bF\u0001F\u0003F\u0484\bF\u0001G\u0001G\u0003G\u0488\bG\u0001G\u0001"+
		"G\u0001G\u0001G\u0005G\u048e\bG\nG\fG\u0491\tG\u0003G\u0493\bG\u0001G"+
		"\u0001G\u0001H\u0001H\u0003H\u0499\bH\u0001I\u0001I\u0001I\u0001I\u0001"+
		"I\u0001I\u0001I\u0001I\u0001I\u0001I\u0003I\u04a5\bI\u0001J\u0001J\u0001"+
		"J\u0001J\u0001J\u0005J\u04ac\bJ\nJ\fJ\u04af\tJ\u0001J\u0001J\u0001K\u0001"+
		"K\u0001K\u0001K\u0001L\u0001L\u0001L\u0001L\u0001L\u0005L\u04bc\bL\nL"+
		"\fL\u04bf\tL\u0001L\u0001L\u0001L\u0005L\u04c4\bL\nL\fL\u04c7\tL\u0003"+
		"L\u04c9\bL\u0001L\u0000\u0004*px\u0088M\u0000\u0002\u0004\u0006\b\n\f"+
		"\u000e\u0010\u0012\u0014\u0016\u0018\u001a\u001c\u001e \"$&(*,.02468:"+
		"<>@BDFHJLNPRTVXZ\\^`bdfhjlnprtvxz|~\u0080\u0082\u0084\u0086\u0088\u008a"+
		"\u008c\u008e\u0090\u0092\u0094\u0096\u0098\u0000\u0006\u0004\u0000*,."+
		".<?NN\u0001\u000089\u0002\u000055NN\u0001\u0000<?\u0002\u0000<<>?\u0003"+
		"\u0000\u000b\fLMOO\u053f\u0000\u009e\u0001\u0000\u0000\u0000\u0002\u00a3"+
		"\u0001\u0000\u0000\u0000\u0004\u00a6\u0001\u0000\u0000\u0000\u0006\u00ad"+
		"\u0001\u0000\u0000\u0000\b\u00c8\u0001\u0000\u0000\u0000\n\u00ca\u0001"+
		"\u0000\u0000\u0000\f\u00d7\u0001\u0000\u0000\u0000\u000e\u00e1\u0001\u0000"+
		"\u0000\u0000\u0010\u00e3\u0001\u0000\u0000\u0000\u0012\u00ee\u0001\u0000"+
		"\u0000\u0000\u0014\u00f9\u0001\u0000\u0000\u0000\u0016\u0115\u0001\u0000"+
		"\u0000\u0000\u0018\u012a\u0001\u0000\u0000\u0000\u001a\u012c\u0001\u0000"+
		"\u0000\u0000\u001c\u0139\u0001\u0000\u0000\u0000\u001e\u013d\u0001\u0000"+
		"\u0000\u0000 \u0170\u0001\u0000\u0000\u0000\"\u0175\u0001\u0000\u0000"+
		"\u0000$\u0177\u0001\u0000\u0000\u0000&\u0188\u0001\u0000\u0000\u0000("+
		"\u018e\u0001\u0000\u0000\u0000*\u0193\u0001\u0000\u0000\u0000,\u019d\u0001"+
		"\u0000\u0000\u0000.\u01bd\u0001\u0000\u0000\u00000\u01c3\u0001\u0000\u0000"+
		"\u00002\u01c6\u0001\u0000\u0000\u00004\u01d1\u0001\u0000\u0000\u00006"+
		"\u01da\u0001\u0000\u0000\u00008\u01de\u0001\u0000\u0000\u0000:\u01e0\u0001"+
		"\u0000\u0000\u0000<\u01e5\u0001\u0000\u0000\u0000>\u01f2\u0001\u0000\u0000"+
		"\u0000@\u01fd\u0001\u0000\u0000\u0000B\u0210\u0001\u0000\u0000\u0000D"+
		"\u021b\u0001\u0000\u0000\u0000F\u021d\u0001\u0000\u0000\u0000H\u023e\u0001"+
		"\u0000\u0000\u0000J\u0244\u0001\u0000\u0000\u0000L\u0246\u0001\u0000\u0000"+
		"\u0000N\u024a\u0001\u0000\u0000\u0000P\u0252\u0001\u0000\u0000\u0000R"+
		"\u0263\u0001\u0000\u0000\u0000T\u0276\u0001\u0000\u0000\u0000V\u0286\u0001"+
		"\u0000\u0000\u0000X\u0288\u0001\u0000\u0000\u0000Z\u0295\u0001\u0000\u0000"+
		"\u0000\\\u029d\u0001\u0000\u0000\u0000^\u02a4\u0001\u0000\u0000\u0000"+
		"`\u02a6\u0001\u0000\u0000\u0000b\u02aa\u0001\u0000\u0000\u0000d\u02b2"+
		"\u0001\u0000\u0000\u0000f\u02b9\u0001\u0000\u0000\u0000h\u02d8\u0001\u0000"+
		"\u0000\u0000j\u02da\u0001\u0000\u0000\u0000l\u02e1\u0001\u0000\u0000\u0000"+
		"n\u02e8\u0001\u0000\u0000\u0000p\u035f\u0001\u0000\u0000\u0000r\u03a0"+
		"\u0001\u0000\u0000\u0000t\u03a9\u0001\u0000\u0000\u0000v\u03ad\u0001\u0000"+
		"\u0000\u0000x\u03b6\u0001\u0000\u0000\u0000z\u03c0\u0001\u0000\u0000\u0000"+
		"|\u03c8\u0001\u0000\u0000\u0000~\u03cd\u0001\u0000\u0000\u0000\u0080\u03cf"+
		"\u0001\u0000\u0000\u0000\u0082\u03e0\u0001\u0000\u0000\u0000\u0084\u03f1"+
		"\u0001\u0000\u0000\u0000\u0086\u0402\u0001\u0000\u0000\u0000\u0088\u0437"+
		"\u0001\u0000\u0000\u0000\u008a\u0455\u0001\u0000\u0000\u0000\u008c\u0483"+
		"\u0001\u0000\u0000\u0000\u008e\u0485\u0001\u0000\u0000\u0000\u0090\u0498"+
		"\u0001\u0000\u0000\u0000\u0092\u04a4\u0001\u0000\u0000\u0000\u0094\u04a6"+
		"\u0001\u0000\u0000\u0000\u0096\u04b2\u0001\u0000\u0000\u0000\u0098\u04c8"+
		"\u0001\u0000\u0000\u0000\u009a\u009d\u0003\u0002\u0001\u0000\u009b\u009d"+
		"\u0003\b\u0004\u0000\u009c\u009a\u0001\u0000\u0000\u0000\u009c\u009b\u0001"+
		"\u0000\u0000\u0000\u009d\u00a0\u0001\u0000\u0000\u0000\u009e\u009c\u0001"+
		"\u0000\u0000\u0000\u009e\u009f\u0001\u0000\u0000\u0000\u009f\u00a1\u0001"+
		"\u0000\u0000\u0000\u00a0\u009e\u0001\u0000\u0000\u0000\u00a1\u00a2\u0005"+
		"\u0000\u0000\u0001\u00a2\u0001\u0001\u0000\u0000\u0000\u00a3\u00a4\u0005"+
		"(\u0000\u0000\u00a4\u00a5\u0003\u0004\u0002\u0000\u00a5\u0003\u0001\u0000"+
		"\u0000\u0000\u00a6\u00a9\u0003\u0006\u0003\u0000\u00a7\u00a8\u0005\u0002"+
		"\u0000\u0000\u00a8\u00aa\u0003\u0006\u0003\u0000\u00a9\u00a7\u0001\u0000"+
		"\u0000\u0000\u00aa\u00ab\u0001\u0000\u0000\u0000\u00ab\u00a9\u0001\u0000"+
		"\u0000\u0000\u00ab\u00ac\u0001\u0000\u0000\u0000\u00ac\u0005\u0001\u0000"+
		"\u0000\u0000\u00ad\u00ae\u0007\u0000\u0000\u0000\u00ae\u0007\u0001\u0000"+
		"\u0000\u0000\u00af\u00b1\u0005)\u0000\u0000\u00b0\u00af\u0001\u0000\u0000"+
		"\u0000\u00b0\u00b1\u0001\u0000\u0000\u0000\u00b1\u00b2\u0001\u0000\u0000"+
		"\u0000\u00b2\u00c9\u0003\u0016\u000b\u0000\u00b3\u00b5\u0005)\u0000\u0000"+
		"\u00b4\u00b3\u0001\u0000\u0000\u0000\u00b4\u00b5\u0001\u0000\u0000\u0000"+
		"\u00b5\u00b6\u0001\u0000\u0000\u0000\u00b6\u00c9\u0003\u0018\f\u0000\u00b7"+
		"\u00c9\u0003\u001e\u000f\u0000\u00b8\u00ba\u0005)\u0000\u0000\u00b9\u00b8"+
		"\u0001\u0000\u0000\u0000\u00b9\u00ba\u0001\u0000\u0000\u0000\u00ba\u00bb"+
		"\u0001\u0000\u0000\u0000\u00bb\u00c9\u0003 \u0010\u0000\u00bc\u00be\u0005"+
		")\u0000\u0000\u00bd\u00bc\u0001\u0000\u0000\u0000\u00bd\u00be\u0001\u0000"+
		"\u0000\u0000\u00be\u00bf\u0001\u0000\u0000\u0000\u00bf\u00c9\u0003:\u001d"+
		"\u0000\u00c0\u00c2\u0005)\u0000\u0000\u00c1\u00c0\u0001\u0000\u0000\u0000"+
		"\u00c1\u00c2\u0001\u0000\u0000\u0000\u00c2\u00c3\u0001\u0000\u0000\u0000"+
		"\u00c3\u00c9\u0003<\u001e\u0000\u00c4\u00c6\u0005)\u0000\u0000\u00c5\u00c4"+
		"\u0001\u0000\u0000\u0000\u00c5\u00c6\u0001\u0000\u0000\u0000\u00c6\u00c7"+
		"\u0001\u0000\u0000\u0000\u00c7\u00c9\u0003\u0014\n\u0000\u00c8\u00b0\u0001"+
		"\u0000\u0000\u0000\u00c8\u00b4\u0001\u0000\u0000\u0000\u00c8\u00b7\u0001"+
		"\u0000\u0000\u0000\u00c8\u00b9\u0001\u0000\u0000\u0000\u00c8\u00bd\u0001"+
		"\u0000\u0000\u0000\u00c8\u00c1\u0001\u0000\u0000\u0000\u00c8\u00c5\u0001"+
		"\u0000\u0000\u0000\u00c9\t\u0001\u0000\u0000\u0000\u00ca\u00cb\u0003\f"+
		"\u0006\u0000\u00cb\u000b\u0001\u0000\u0000\u0000\u00cc\u00cd\u0005\u0004"+
		"\u0000\u0000\u00cd\u00d0\u0003\n\u0005\u0000\u00ce\u00cf\u0005\u0001\u0000"+
		"\u0000\u00cf\u00d1\u0003\n\u0005\u0000\u00d0\u00ce\u0001\u0000\u0000\u0000"+
		"\u00d1\u00d2\u0001\u0000\u0000\u0000\u00d2\u00d0\u0001\u0000\u0000\u0000"+
		"\u00d2\u00d3\u0001\u0000\u0000\u0000\u00d3\u00d4\u0001\u0000\u0000\u0000"+
		"\u00d4\u00d5\u0005\u0005\u0000\u0000\u00d5\u00d8\u0001\u0000\u0000\u0000"+
		"\u00d6\u00d8\u0003\u000e\u0007\u0000\u00d7\u00cc\u0001\u0000\u0000\u0000"+
		"\u00d7\u00d6\u0001\u0000\u0000\u0000\u00d8\r\u0001\u0000\u0000\u0000\u00d9"+
		"\u00db\u0005N\u0000\u0000\u00da\u00dc\u0003\u0010\b\u0000\u00db\u00da"+
		"\u0001\u0000\u0000\u0000\u00db\u00dc\u0001\u0000\u0000\u0000\u00dc\u00e2"+
		"\u0001\u0000\u0000\u0000\u00dd\u00de\u0005\u0004\u0000\u0000\u00de\u00df"+
		"\u0003\n\u0005\u0000\u00df\u00e0\u0005\u0005\u0000\u0000\u00e0\u00e2\u0001"+
		"\u0000\u0000\u0000\u00e1\u00d9\u0001\u0000\u0000\u0000\u00e1\u00dd\u0001"+
		"\u0000\u0000\u0000\u00e2\u000f\u0001\u0000\u0000\u0000\u00e3\u00e4\u0005"+
		"\u0017\u0000\u0000\u00e4\u00e9\u0003\n\u0005\u0000\u00e5\u00e6\u0005\u0001"+
		"\u0000\u0000\u00e6\u00e8\u0003\n\u0005\u0000\u00e7\u00e5\u0001\u0000\u0000"+
		"\u0000\u00e8\u00eb\u0001\u0000\u0000\u0000\u00e9\u00e7\u0001\u0000\u0000"+
		"\u0000\u00e9\u00ea\u0001\u0000\u0000\u0000\u00ea\u00ec\u0001\u0000\u0000"+
		"\u0000\u00eb\u00e9\u0001\u0000\u0000\u0000\u00ec\u00ed\u0005\u0019\u0000"+
		"\u0000\u00ed\u0011\u0001\u0000\u0000\u0000\u00ee\u00ef\u0005\u0017\u0000"+
		"\u0000\u00ef\u00f4\u0005N\u0000\u0000\u00f0\u00f1\u0005\u0001\u0000\u0000"+
		"\u00f1\u00f3\u0005N\u0000\u0000\u00f2\u00f0\u0001\u0000\u0000\u0000\u00f3"+
		"\u00f6\u0001\u0000\u0000\u0000\u00f4\u00f2\u0001\u0000\u0000\u0000\u00f4"+
		"\u00f5\u0001\u0000\u0000\u0000\u00f5\u00f7\u0001\u0000\u0000\u0000\u00f6"+
		"\u00f4\u0001\u0000\u0000\u0000\u00f7\u00f8\u0005\u0019\u0000\u0000\u00f8"+
		"\u0013\u0001\u0000\u0000\u0000\u00f9\u00fa\u0005E\u0000\u0000\u00fa\u00fc"+
		"\u0005N\u0000\u0000\u00fb\u00fd\u0003\u0012\t\u0000\u00fc\u00fb\u0001"+
		"\u0000\u0000\u0000\u00fc\u00fd\u0001\u0000\u0000\u0000\u00fd\u00fe\u0001"+
		"\u0000\u0000\u0000\u00fe\u00ff\u0003X,\u0000\u00ff\u0100\u0005\u0003\u0000"+
		"\u0000\u0100\u0101\u0003\n\u0005\u0000\u0101\u0102\u0005\u001b\u0000\u0000"+
		"\u0102\u0103\u0003p8\u0000\u0103\u0015\u0001\u0000\u0000\u0000\u0104\u0106"+
		"\u0005.\u0000\u0000\u0105\u0104\u0001\u0000\u0000\u0000\u0105\u0106\u0001"+
		"\u0000\u0000\u0000\u0106\u0107\u0001\u0000\u0000\u0000\u0107\u0108\u0005"+
		"/\u0000\u0000\u0108\u0109\u0005N\u0000\u0000\u0109\u010d\u0005\b\u0000"+
		"\u0000\u010a\u010c\u0003J%\u0000\u010b\u010a\u0001\u0000\u0000\u0000\u010c"+
		"\u010f\u0001\u0000\u0000\u0000\u010d\u010b\u0001\u0000\u0000\u0000\u010d"+
		"\u010e\u0001\u0000\u0000\u0000\u010e\u0110\u0001\u0000\u0000\u0000\u010f"+
		"\u010d\u0001\u0000\u0000\u0000\u0110\u0116\u0005\t\u0000\u0000\u0111\u0112"+
		"\u0005/\u0000\u0000\u0112\u0113\u0005N\u0000\u0000\u0113\u0114\u0005\u001e"+
		"\u0000\u0000\u0114\u0116\u0003x<\u0000\u0115\u0105\u0001\u0000\u0000\u0000"+
		"\u0115\u0111\u0001\u0000\u0000\u0000\u0116\u0017\u0001\u0000\u0000\u0000"+
		"\u0117\u0118\u0005-\u0000\u0000\u0118\u011a\u0005N\u0000\u0000\u0119\u011b"+
		"\u0003\u0012\t\u0000\u011a\u0119\u0001\u0000\u0000\u0000\u011a\u011b\u0001"+
		"\u0000\u0000\u0000\u011b\u011c\u0001\u0000\u0000\u0000\u011c\u0120\u0005"+
		"\b\u0000\u0000\u011d\u011f\u0003L&\u0000\u011e\u011d\u0001\u0000\u0000"+
		"\u0000\u011f\u0122\u0001\u0000\u0000\u0000\u0120\u011e\u0001\u0000\u0000"+
		"\u0000\u0120\u0121\u0001\u0000\u0000\u0000\u0121\u0123\u0001\u0000\u0000"+
		"\u0000\u0122\u0120\u0001\u0000\u0000\u0000\u0123\u012b\u0005\t\u0000\u0000"+
		"\u0124\u0125\u0005-\u0000\u0000\u0125\u0126\u0005N\u0000\u0000\u0126\u0127"+
		"\u0005\u001e\u0000\u0000\u0127\u012b\u0003\n\u0005\u0000\u0128\u0129\u0005"+
		"-\u0000\u0000\u0129\u012b\u0005N\u0000\u0000\u012a\u0117\u0001\u0000\u0000"+
		"\u0000\u012a\u0124\u0001\u0000\u0000\u0000\u012a\u0128\u0001\u0000\u0000"+
		"\u0000\u012b\u0019\u0001\u0000\u0000\u0000\u012c\u012d\u0005N\u0000\u0000"+
		"\u012d\u012e\u0005\u001e\u0000\u0000\u012e\u012f\u0005\b\u0000\u0000\u012f"+
		"\u0134\u0003|>\u0000\u0130\u0131\u0005\u0001\u0000\u0000\u0131\u0133\u0003"+
		"|>\u0000\u0132\u0130\u0001\u0000\u0000\u0000\u0133\u0136\u0001\u0000\u0000"+
		"\u0000\u0134\u0132\u0001\u0000\u0000\u0000\u0134\u0135\u0001\u0000\u0000"+
		"\u0000\u0135\u0137\u0001\u0000\u0000\u0000\u0136\u0134\u0001\u0000\u0000"+
		"\u0000\u0137\u0138\u0005\t\u0000\u0000\u0138\u001b\u0001\u0000\u0000\u0000"+
		"\u0139\u013a\u0005N\u0000\u0000\u013a\u013b\u0005\u001e\u0000\u0000\u013b"+
		"\u013c\u0003p8\u0000\u013c\u001d\u0001\u0000\u0000\u0000\u013d\u013e\u0005"+
		"0\u0000\u0000\u013e\u0143\u0005N\u0000\u0000\u013f\u0140\u0005\u0001\u0000"+
		"\u0000\u0140\u0142\u0005N\u0000\u0000\u0141\u013f\u0001\u0000\u0000\u0000"+
		"\u0142\u0145\u0001\u0000\u0000\u0000\u0143\u0141\u0001\u0000\u0000\u0000"+
		"\u0143\u0144\u0001\u0000\u0000\u0000\u0144\u001f\u0001\u0000\u0000\u0000"+
		"\u0145\u0143\u0001\u0000\u0000\u0000\u0146\u0148\u0005.\u0000\u0000\u0147"+
		"\u0146\u0001\u0000\u0000\u0000\u0147\u0148\u0001\u0000\u0000\u0000\u0148"+
		"\u0149\u0001\u0000\u0000\u0000\u0149\u014a\u00051\u0000\u0000\u014a\u0151"+
		"\u0005N\u0000\u0000\u014b\u014c\u0005\u0006\u0000\u0000\u014c\u014d\u0005"+
		"N\u0000\u0000\u014d\u014e\u0005\u0003\u0000\u0000\u014e\u014f\u0003\n"+
		"\u0005\u0000\u014f\u0150\u0005\u0007\u0000\u0000\u0150\u0152\u0001\u0000"+
		"\u0000\u0000\u0151\u014b\u0001\u0000\u0000\u0000\u0151\u0152\u0001\u0000"+
		"\u0000\u0000\u0152\u0153\u0001\u0000\u0000\u0000\u0153\u0157\u0005\b\u0000"+
		"\u0000\u0154\u0156\u0003\"\u0011\u0000\u0155\u0154\u0001\u0000\u0000\u0000"+
		"\u0156\u0159\u0001\u0000\u0000\u0000\u0157\u0155\u0001\u0000\u0000\u0000"+
		"\u0157\u0158\u0001\u0000\u0000\u0000\u0158\u015a\u0001\u0000\u0000\u0000"+
		"\u0159\u0157\u0001\u0000\u0000\u0000\u015a\u0171\u0005\t\u0000\u0000\u015b"+
		"\u015c\u00051\u0000\u0000\u015c\u015d\u0005N\u0000\u0000\u015d\u015e\u0005"+
		"\u001e\u0000\u0000\u015e\u015f\u0005N\u0000\u0000\u015f\u0160\u0005\u000e"+
		"\u0000\u0000\u0160\u0171\u0003$\u0012\u0000\u0161\u0162\u00051\u0000\u0000"+
		"\u0162\u0163\u0005N\u0000\u0000\u0163\u0164\u0005\u001e\u0000\u0000\u0164"+
		"\u0171\u0003$\u0012\u0000\u0165\u0166\u00051\u0000\u0000\u0166\u0167\u0005"+
		"N\u0000\u0000\u0167\u0168\u0005\u001e\u0000\u0000\u0168\u0169\u0003*\u0015"+
		"\u0000\u0169\u016a\u0005\u000e\u0000\u0000\u016a\u016b\u0003p8\u0000\u016b"+
		"\u0171\u0001\u0000\u0000\u0000\u016c\u016d\u00051\u0000\u0000\u016d\u016e"+
		"\u0005N\u0000\u0000\u016e\u016f\u0005\u001e\u0000\u0000\u016f\u0171\u0003"+
		"*\u0015\u0000\u0170\u0147\u0001\u0000\u0000\u0000\u0170\u015b\u0001\u0000"+
		"\u0000\u0000\u0170\u0161\u0001\u0000\u0000\u0000\u0170\u0165\u0001\u0000"+
		"\u0000\u0000\u0170\u016c\u0001\u0000\u0000\u0000\u0171!\u0001\u0000\u0000"+
		"\u0000\u0172\u0176\u0003J%\u0000\u0173\u0176\u0003\u001a\r\u0000\u0174"+
		"\u0176\u0003\u001c\u000e\u0000\u0175\u0172\u0001\u0000\u0000\u0000\u0175"+
		"\u0173\u0001\u0000\u0000\u0000\u0175\u0174\u0001\u0000\u0000\u0000\u0176"+
		"#\u0001\u0000\u0000\u0000\u0177\u0178\u0005\u0017\u0000\u0000\u0178\u0179"+
		"\u0003&\u0013\u0000\u0179\u017a\u0005\u0019\u0000\u0000\u017a\u017b\u0003"+
		"*\u0015\u0000\u017b\u017c\u0005\u0017\u0000\u0000\u017c\u017d\u0003(\u0014"+
		"\u0000\u017d\u017e\u0005\u0019\u0000\u0000\u017e%\u0001\u0000\u0000\u0000"+
		"\u017f\u0182\u0005\u000b\u0000\u0000\u0180\u0181\u0005\u0001\u0000\u0000"+
		"\u0181\u0183\u0005N\u0000\u0000\u0182\u0180\u0001\u0000\u0000\u0000\u0182"+
		"\u0183\u0001\u0000\u0000\u0000\u0183\u0189\u0001\u0000\u0000\u0000\u0184"+
		"\u0185\u0005N\u0000\u0000\u0185\u0186\u0005\u0001\u0000\u0000\u0186\u0189"+
		"\u0005N\u0000\u0000\u0187\u0189\u0003*\u0015\u0000\u0188\u017f\u0001\u0000"+
		"\u0000\u0000\u0188\u0184\u0001\u0000\u0000\u0000\u0188\u0187\u0001\u0000"+
		"\u0000\u0000\u0189\'\u0001\u0000\u0000\u0000\u018a\u018b\u0005N\u0000"+
		"\u0000\u018b\u018c\u0005\u0001\u0000\u0000\u018c\u018f\u0005N\u0000\u0000"+
		"\u018d\u018f\u0003p8\u0000\u018e\u018a\u0001\u0000\u0000\u0000\u018e\u018d"+
		"\u0001\u0000\u0000\u0000\u018f)\u0001\u0000\u0000\u0000\u0190\u0191\u0006"+
		"\u0015\uffff\uffff\u0000\u0191\u0194\u0003,\u0016\u0000\u0192\u0194\u0003"+
		".\u0017\u0000\u0193\u0190\u0001\u0000\u0000\u0000\u0193\u0192\u0001\u0000"+
		"\u0000\u0000\u0194\u019a\u0001\u0000\u0000\u0000\u0195\u0196\n\u0003\u0000"+
		"\u0000\u0196\u0197\u0005\n\u0000\u0000\u0197\u0199\u0003*\u0015\u0004"+
		"\u0198\u0195\u0001\u0000\u0000\u0000\u0199\u019c\u0001\u0000\u0000\u0000"+
		"\u019a\u0198\u0001\u0000\u0000\u0000\u019a\u019b\u0001\u0000\u0000\u0000"+
		"\u019b+\u0001\u0000\u0000\u0000\u019c\u019a\u0001\u0000\u0000\u0000\u019d"+
		"\u019e\u0005H\u0000\u0000\u019e\u019f\u0005\u0004\u0000\u0000\u019f\u01a0"+
		"\u0005N\u0000\u0000\u01a0\u01a1\u0005\u0003\u0000\u0000\u01a1\u01a2\u0003"+
		"\n\u0005\u0000\u01a2\u01a3\u0005\u0005\u0000\u0000\u01a3\u01a4\u0005\b"+
		"\u0000\u0000\u01a4\u01a5\u0003*\u0015\u0000\u01a5\u01a6\u0005\t\u0000"+
		"\u0000\u01a6-\u0001\u0000\u0000\u0000\u01a7\u01a8\u00036\u001b\u0000\u01a8"+
		"\u01a9\u0005\u0006\u0000\u0000\u01a9\u01aa\u0005N\u0000\u0000\u01aa\u01ab"+
		"\u0005\u0003\u0000\u0000\u01ab\u01ac\u0003\n\u0005\u0000\u01ac\u01b5\u0005"+
		"\u0007\u0000\u0000\u01ad\u01b1\u0005\b\u0000\u0000\u01ae\u01b0\u00030"+
		"\u0018\u0000\u01af\u01ae\u0001\u0000\u0000\u0000\u01b0\u01b3\u0001\u0000"+
		"\u0000\u0000\u01b1\u01af\u0001\u0000\u0000\u0000\u01b1\u01b2\u0001\u0000"+
		"\u0000\u0000\u01b2\u01b4\u0001\u0000\u0000\u0000\u01b3\u01b1\u0001\u0000"+
		"\u0000\u0000\u01b4\u01b6\u0005\t\u0000\u0000\u01b5\u01ad\u0001\u0000\u0000"+
		"\u0000\u01b5\u01b6\u0001\u0000\u0000\u0000\u01b6\u01be\u0001\u0000\u0000"+
		"\u0000\u01b7\u01b8\u00036\u001b\u0000\u01b8\u01b9\u0005\u0006\u0000\u0000"+
		"\u01b9\u01ba\u0005N\u0000\u0000\u01ba\u01bb\u0005\u0007\u0000\u0000\u01bb"+
		"\u01be\u0001\u0000\u0000\u0000\u01bc\u01be\u00036\u001b\u0000\u01bd\u01a7"+
		"\u0001\u0000\u0000\u0000\u01bd\u01b7\u0001\u0000\u0000\u0000\u01bd\u01bc"+
		"\u0001\u0000\u0000\u0000\u01be/\u0001\u0000\u0000\u0000\u01bf\u01c4\u0003"+
		"2\u0019\u0000\u01c0\u01c4\u00034\u001a\u0000\u01c1\u01c4\u0003\u001a\r"+
		"\u0000\u01c2\u01c4\u0003\u001c\u000e\u0000\u01c3\u01bf\u0001\u0000\u0000"+
		"\u0000\u01c3\u01c0\u0001\u0000\u0000\u0000\u01c3\u01c1\u0001\u0000\u0000"+
		"\u0000\u01c3\u01c2\u0001\u0000\u0000\u0000\u01c41\u0001\u0000\u0000\u0000"+
		"\u01c5\u01c7\u00059\u0000\u0000\u01c6\u01c5\u0001\u0000\u0000\u0000\u01c6"+
		"\u01c7\u0001\u0000\u0000\u0000\u01c7\u01c8\u0001\u0000\u0000\u0000\u01c8"+
		"\u01c9\u0005J\u0000\u0000\u01c9\u01ce\u0005N\u0000\u0000\u01ca\u01cb\u0005"+
		"\u0001\u0000\u0000\u01cb\u01cd\u0005N\u0000\u0000\u01cc\u01ca\u0001\u0000"+
		"\u0000\u0000\u01cd\u01d0\u0001\u0000\u0000\u0000\u01ce\u01cc\u0001\u0000"+
		"\u0000\u0000\u01ce\u01cf\u0001\u0000\u0000\u0000\u01cf3\u0001\u0000\u0000"+
		"\u0000\u01d0\u01ce\u0001\u0000\u0000\u0000\u01d1\u01d2\u0005K\u0000\u0000"+
		"\u01d2\u01d3\u0005\u0003\u0000\u0000\u01d3\u01d4\u0003p8\u0000\u01d45"+
		"\u0001\u0000\u0000\u0000\u01d5\u01db\u00038\u001c\u0000\u01d6\u01d7\u0005"+
		"\u0004\u0000\u0000\u01d7\u01d8\u0003*\u0015\u0000\u01d8\u01d9\u0005\u0005"+
		"\u0000\u0000\u01d9\u01db\u0001\u0000\u0000\u0000\u01da\u01d5\u0001\u0000"+
		"\u0000\u0000\u01da\u01d6\u0001\u0000\u0000\u0000\u01db7\u0001\u0000\u0000"+
		"\u0000\u01dc\u01df\u0003\u0004\u0002\u0000\u01dd\u01df\u0005N\u0000\u0000"+
		"\u01de\u01dc\u0001\u0000\u0000\u0000\u01de\u01dd\u0001\u0000\u0000\u0000"+
		"\u01df9\u0001\u0000\u0000\u0000\u01e0\u01e1\u00052\u0000\u0000\u01e1\u01e2"+
		"\u0005N\u0000\u0000\u01e2\u01e3\u0005\u001e\u0000\u0000\u01e3\u01e4\u0003"+
		"p8\u0000\u01e4;\u0001\u0000\u0000\u0000\u01e5\u01e6\u00053\u0000\u0000"+
		"\u01e6\u01e8\u0005N\u0000\u0000\u01e7\u01e9\u0003>\u001f\u0000\u01e8\u01e7"+
		"\u0001\u0000\u0000\u0000\u01e8\u01e9\u0001\u0000\u0000\u0000\u01e9\u01ea"+
		"\u0001\u0000\u0000\u0000\u01ea\u01ec\u0005\b\u0000\u0000\u01eb\u01ed\u0003"+
		"B!\u0000\u01ec\u01eb\u0001\u0000\u0000\u0000\u01ed\u01ee\u0001\u0000\u0000"+
		"\u0000\u01ee\u01ec\u0001\u0000\u0000\u0000\u01ee\u01ef\u0001\u0000\u0000"+
		"\u0000\u01ef\u01f0\u0001\u0000\u0000\u0000\u01f0\u01f1\u0005\t\u0000\u0000"+
		"\u01f1=\u0001\u0000\u0000\u0000\u01f2\u01f3\u0005\u0004\u0000\u0000\u01f3"+
		"\u01f8\u0003@ \u0000\u01f4\u01f5\u0005\u0001\u0000\u0000\u01f5\u01f7\u0003"+
		"@ \u0000\u01f6\u01f4\u0001\u0000\u0000\u0000\u01f7\u01fa\u0001\u0000\u0000"+
		"\u0000\u01f8\u01f6\u0001\u0000\u0000\u0000\u01f8\u01f9\u0001\u0000\u0000"+
		"\u0000\u01f9\u01fb\u0001\u0000\u0000\u0000\u01fa\u01f8\u0001\u0000\u0000"+
		"\u0000\u01fb\u01fc\u0005\u0005\u0000\u0000\u01fc?\u0001\u0000\u0000\u0000"+
		"\u01fd\u01fe\u0005N\u0000\u0000\u01fe\u01ff\u0005\u0003\u0000\u0000\u01ff"+
		"\u0200\u0003\n\u0005\u0000\u0200A\u0001\u0000\u0000\u0000\u0201\u0202"+
		"\u00055\u0000\u0000\u0202\u0211\u0003p8\u0000\u0203\u0204\u00054\u0000"+
		"\u0000\u0204\u0208\u0005N\u0000\u0000\u0205\u0206\u0005\u0006\u0000\u0000"+
		"\u0206\u0207\u0005N\u0000\u0000\u0207\u0209\u0005\u0007\u0000\u0000\u0208"+
		"\u0205\u0001\u0000\u0000\u0000\u0208\u0209\u0001\u0000\u0000\u0000\u0209"+
		"\u020a\u0001\u0000\u0000\u0000\u020a\u020b\u0005\u0004\u0000\u0000\u020b"+
		"\u020c\u0003D\"\u0000\u020c\u020d\u0005\u0005\u0000\u0000\u020d\u020e"+
		"\u0005\u001e\u0000\u0000\u020e\u020f\u0003H$\u0000\u020f\u0211\u0001\u0000"+
		"\u0000\u0000\u0210\u0201\u0001\u0000\u0000\u0000\u0210\u0203\u0001\u0000"+
		"\u0000\u0000\u0211C\u0001\u0000\u0000\u0000\u0212\u021c\u0001\u0000\u0000"+
		"\u0000\u0213\u0218\u0003F#\u0000\u0214\u0215\u0005\u0001\u0000\u0000\u0215"+
		"\u0217\u0003F#\u0000\u0216\u0214\u0001\u0000\u0000\u0000\u0217\u021a\u0001"+
		"\u0000\u0000\u0000\u0218\u0216\u0001\u0000\u0000\u0000\u0218\u0219\u0001"+
		"\u0000\u0000\u0000\u0219\u021c\u0001\u0000\u0000\u0000\u021a\u0218\u0001"+
		"\u0000\u0000\u0000\u021b\u0212\u0001\u0000\u0000\u0000\u021b\u0213\u0001"+
		"\u0000\u0000\u0000\u021cE\u0001\u0000\u0000\u0000\u021d\u021e\u0005N\u0000"+
		"\u0000\u021eG\u0001\u0000\u0000\u0000\u021f\u023f\u0005\u000b\u0000\u0000"+
		"\u0220\u023f\u0005\f\u0000\u0000\u0221\u0222\u0005\u0011\u0000\u0000\u0222"+
		"\u022e\u0005N\u0000\u0000\u0223\u0224\u0005\u0004\u0000\u0000\u0224\u0229"+
		"\u0003p8\u0000\u0225\u0226\u0005\u0001\u0000\u0000\u0226\u0228\u0003p"+
		"8\u0000\u0227\u0225\u0001\u0000\u0000\u0000\u0228\u022b\u0001\u0000\u0000"+
		"\u0000\u0229\u0227\u0001\u0000\u0000\u0000\u0229\u022a\u0001\u0000\u0000"+
		"\u0000\u022a\u022c\u0001\u0000\u0000\u0000\u022b\u0229\u0001\u0000\u0000"+
		"\u0000\u022c\u022d\u0005\u0005\u0000\u0000\u022d\u022f\u0001\u0000\u0000"+
		"\u0000\u022e\u0223\u0001\u0000\u0000\u0000\u022e\u022f\u0001\u0000\u0000"+
		"\u0000\u022f\u023f\u0001\u0000\u0000\u0000\u0230\u023c\u0005N\u0000\u0000"+
		"\u0231\u0232\u0005\u0004\u0000\u0000\u0232\u0237\u0003p8\u0000\u0233\u0234"+
		"\u0005\u0001\u0000\u0000\u0234\u0236\u0003p8\u0000\u0235\u0233\u0001\u0000"+
		"\u0000\u0000\u0236\u0239\u0001\u0000\u0000\u0000\u0237\u0235\u0001\u0000"+
		"\u0000\u0000\u0237\u0238\u0001\u0000\u0000\u0000\u0238\u023a\u0001\u0000"+
		"\u0000\u0000\u0239\u0237\u0001\u0000\u0000\u0000\u023a\u023b\u0005\u0005"+
		"\u0000\u0000\u023b\u023d\u0001\u0000\u0000\u0000\u023c\u0231\u0001\u0000"+
		"\u0000\u0000\u023c\u023d\u0001\u0000\u0000\u0000\u023d\u023f\u0001\u0000"+
		"\u0000\u0000\u023e\u021f\u0001\u0000\u0000\u0000\u023e\u0220\u0001\u0000"+
		"\u0000\u0000\u023e\u0221\u0001\u0000\u0000\u0000\u023e\u0230\u0001\u0000"+
		"\u0000\u0000\u023fI\u0001\u0000\u0000\u0000\u0240\u0245\u0003N\'\u0000"+
		"\u0241\u0245\u0003P(\u0000\u0242\u0245\u0003R)\u0000\u0243\u0245\u0003"+
		"T*\u0000\u0244\u0240\u0001\u0000\u0000\u0000\u0244\u0241\u0001\u0000\u0000"+
		"\u0000\u0244\u0242\u0001\u0000\u0000\u0000\u0244\u0243\u0001\u0000\u0000"+
		"\u0000\u0245K\u0001\u0000\u0000\u0000\u0246\u0247\u0005N\u0000\u0000\u0247"+
		"\u0248\u0005\u0003\u0000\u0000\u0248\u0249\u0003\n\u0005\u0000\u0249M"+
		"\u0001\u0000\u0000\u0000\u024a\u024b\u0007\u0001\u0000\u0000\u024b\u024c"+
		"\u0005N\u0000\u0000\u024c\u024d\u0005\u0003\u0000\u0000\u024d\u0250\u0003"+
		"\n\u0005\u0000\u024e\u024f\u0005\u001e\u0000\u0000\u024f\u0251\u0003p"+
		"8\u0000\u0250\u024e\u0001\u0000\u0000\u0000\u0250\u0251\u0001\u0000\u0000"+
		"\u0000\u0251O\u0001\u0000\u0000\u0000\u0252\u0253\u0005:\u0000\u0000\u0253"+
		"\u0254\u0007\u0002\u0000\u0000\u0254\u0257\u0003X,\u0000\u0255\u0256\u0005"+
		"G\u0000\u0000\u0256\u0258\u0003X,\u0000\u0257\u0255\u0001\u0000\u0000"+
		"\u0000\u0257\u0258\u0001\u0000\u0000\u0000\u0258\u0259\u0001\u0000\u0000"+
		"\u0000\u0259\u025d\u0005\b\u0000\u0000\u025a\u025c\u0003\\.\u0000\u025b"+
		"\u025a\u0001\u0000\u0000\u0000\u025c\u025f\u0001\u0000\u0000\u0000\u025d"+
		"\u025b\u0001\u0000\u0000\u0000\u025d\u025e\u0001\u0000\u0000\u0000\u025e"+
		"\u0260\u0001\u0000\u0000\u0000\u025f\u025d\u0001\u0000\u0000\u0000\u0260"+
		"\u0261\u0005\t\u0000\u0000\u0261Q\u0001\u0000\u0000\u0000\u0262\u0264"+
		"\u0007\u0003\u0000\u0000\u0263\u0262\u0001\u0000\u0000\u0000\u0263\u0264"+
		"\u0001\u0000\u0000\u0000\u0264\u0265\u0001\u0000\u0000\u0000\u0265\u0266"+
		"\u0005;\u0000\u0000\u0266\u0267\u0005N\u0000\u0000\u0267\u026a\u0003X"+
		",\u0000\u0268\u0269\u0005G\u0000\u0000\u0269\u026b\u0003X,\u0000\u026a"+
		"\u0268\u0001\u0000\u0000\u0000\u026a\u026b\u0001\u0000\u0000\u0000\u026b"+
		"\u026c\u0001\u0000\u0000\u0000\u026c\u0270\u0005\b\u0000\u0000\u026d\u026f"+
		"\u0003^/\u0000\u026e\u026d\u0001\u0000\u0000\u0000\u026f\u0272\u0001\u0000"+
		"\u0000\u0000\u0270\u026e\u0001\u0000\u0000\u0000\u0270\u0271\u0001\u0000"+
		"\u0000\u0000\u0271\u0273\u0001\u0000\u0000\u0000\u0272\u0270\u0001\u0000"+
		"\u0000\u0000\u0273\u0274\u0005\t\u0000\u0000\u0274S\u0001\u0000\u0000"+
		"\u0000\u0275\u0277\u0007\u0004\u0000\u0000\u0276\u0275\u0001\u0000\u0000"+
		"\u0000\u0276\u0277\u0001\u0000\u0000\u0000\u0277\u0278\u0001\u0000\u0000"+
		"\u0000\u0278\u0279\u0005F\u0000\u0000\u0279\u027a\u0005N\u0000\u0000\u027a"+
		"\u027b\u0003X,\u0000\u027b\u027f\u0005\b\u0000\u0000\u027c\u027e\u0003"+
		"V+\u0000\u027d\u027c\u0001\u0000\u0000\u0000\u027e\u0281\u0001\u0000\u0000"+
		"\u0000\u027f\u027d\u0001\u0000\u0000\u0000\u027f\u0280\u0001\u0000\u0000"+
		"\u0000\u0280\u0282\u0001\u0000\u0000\u0000\u0281\u027f\u0001\u0000\u0000"+
		"\u0000\u0282\u0283\u0005\t\u0000\u0000\u0283U\u0001\u0000\u0000\u0000"+
		"\u0284\u0287\u0003`0\u0000\u0285\u0287\u0003j5\u0000\u0286\u0284\u0001"+
		"\u0000\u0000\u0000\u0286\u0285\u0001\u0000\u0000\u0000\u0287W\u0001\u0000"+
		"\u0000\u0000\u0288\u028a\u0005\u0004\u0000\u0000\u0289\u028b\u0003Z-\u0000"+
		"\u028a\u0289\u0001\u0000\u0000\u0000\u028a\u028b\u0001\u0000\u0000\u0000"+
		"\u028b\u0290\u0001\u0000\u0000\u0000\u028c\u028d\u0005\u0001\u0000\u0000"+
		"\u028d\u028f\u0003Z-\u0000\u028e\u028c\u0001\u0000\u0000\u0000\u028f\u0292"+
		"\u0001\u0000\u0000\u0000\u0290\u028e\u0001\u0000\u0000\u0000\u0290\u0291"+
		"\u0001\u0000\u0000\u0000\u0291\u0293\u0001\u0000\u0000\u0000\u0292\u0290"+
		"\u0001\u0000\u0000\u0000\u0293\u0294\u0005\u0005\u0000\u0000\u0294Y\u0001"+
		"\u0000\u0000\u0000\u0295\u0296\u0005N\u0000\u0000\u0296\u0297\u0005\u0003"+
		"\u0000\u0000\u0297\u0298\u0003\n\u0005\u0000\u0298[\u0001\u0000\u0000"+
		"\u0000\u0299\u029e\u0003j5\u0000\u029a\u029e\u0003b1\u0000\u029b\u029e"+
		"\u0003d2\u0000\u029c\u029e\u0003l6\u0000\u029d\u0299\u0001\u0000\u0000"+
		"\u0000\u029d\u029a\u0001\u0000\u0000\u0000\u029d\u029b\u0001\u0000\u0000"+
		"\u0000\u029d\u029c\u0001\u0000\u0000\u0000\u029e]\u0001\u0000\u0000\u0000"+
		"\u029f\u02a5\u0003`0\u0000\u02a0\u02a5\u0003j5\u0000\u02a1\u02a5\u0003"+
		"b1\u0000\u02a2\u02a5\u0003d2\u0000\u02a3\u02a5\u0003l6\u0000\u02a4\u029f"+
		"\u0001\u0000\u0000\u0000\u02a4\u02a0\u0001\u0000\u0000\u0000\u02a4\u02a1"+
		"\u0001\u0000\u0000\u0000\u02a4\u02a2\u0001\u0000\u0000\u0000\u02a4\u02a3"+
		"\u0001\u0000\u0000\u0000\u02a5_\u0001\u0000\u0000\u0000\u02a6\u02a7\u0005"+
		"@\u0000\u0000\u02a7\u02a8\u0005\u0003\u0000\u0000\u02a8\u02a9\u0003p8"+
		"\u0000\u02a9a\u0001\u0000\u0000\u0000\u02aa\u02ab\u0005A\u0000\u0000\u02ab"+
		"\u02af\u0005\u0003\u0000\u0000\u02ac\u02ae\u0003h4\u0000\u02ad\u02ac\u0001"+
		"\u0000\u0000\u0000\u02ae\u02b1\u0001\u0000\u0000\u0000\u02af\u02ad\u0001"+
		"\u0000\u0000\u0000\u02af\u02b0\u0001\u0000\u0000\u0000\u02b0c\u0001\u0000"+
		"\u0000\u0000\u02b1\u02af\u0001\u0000\u0000\u0000\u02b2\u02b3\u0005B\u0000"+
		"\u0000\u02b3\u02b5\u0005\u0003\u0000\u0000\u02b4\u02b6\u0003f3\u0000\u02b5"+
		"\u02b4\u0001\u0000\u0000\u0000\u02b6\u02b7\u0001\u0000\u0000\u0000\u02b7"+
		"\u02b5\u0001\u0000\u0000\u0000\u02b7\u02b8\u0001\u0000\u0000\u0000\u02b8"+
		"e\u0001\u0000\u0000\u0000\u02b9\u02ba\u0003p8\u0000\u02ba\u02bb\u0005"+
		"\'\u0000\u0000\u02bb\u02bc\u0003p8\u0000\u02bcg\u0001\u0000\u0000\u0000"+
		"\u02bd\u02be\u0003\u0098L\u0000\u02be\u02bf\u0005\u001e\u0000\u0000\u02bf"+
		"\u02c0\u0003p8\u0000\u02c0\u02d9\u0001\u0000\u0000\u0000\u02c1\u02c2\u0005"+
		"I\u0000\u0000\u02c2\u02c3\u0005\u0002\u0000\u0000\u02c3\u02c4\u0005N\u0000"+
		"\u0000\u02c4\u02c5\u0005\u0006\u0000\u0000\u02c5\u02c6\u0003p8\u0000\u02c6"+
		"\u02c7\u0005\u0007\u0000\u0000\u02c7\u02c8\u0005\u001e\u0000\u0000\u02c8"+
		"\u02c9\u0003p8\u0000\u02c9\u02d9\u0001\u0000\u0000\u0000\u02ca\u02cb\u0005"+
		"N\u0000\u0000\u02cb\u02cc\u0005\u0006\u0000\u0000\u02cc\u02cd\u0003p8"+
		"\u0000\u02cd\u02ce\u0005\u0007\u0000\u0000\u02ce\u02cf\u0005\u001e\u0000"+
		"\u0000\u02cf\u02d0\u0003p8\u0000\u02d0\u02d9\u0001\u0000\u0000\u0000\u02d1"+
		"\u02d2\u0005#\u0000\u0000\u02d2\u02d3\u0005N\u0000\u0000\u02d3\u02d4\u0005"+
		"\u0003\u0000\u0000\u02d4\u02d5\u0003\n\u0005\u0000\u02d5\u02d6\u0005\u001e"+
		"\u0000\u0000\u02d6\u02d7\u0003p8\u0000\u02d7\u02d9\u0001\u0000\u0000\u0000"+
		"\u02d8\u02bd\u0001\u0000\u0000\u0000\u02d8\u02c1\u0001\u0000\u0000\u0000"+
		"\u02d8\u02ca\u0001\u0000\u0000\u0000\u02d8\u02d1\u0001\u0000\u0000\u0000"+
		"\u02d9i\u0001\u0000\u0000\u0000\u02da\u02db\u0005C\u0000\u0000\u02db\u02dd"+
		"\u0005\u0003\u0000\u0000\u02dc\u02de\u0003n7\u0000\u02dd\u02dc\u0001\u0000"+
		"\u0000\u0000\u02de\u02df\u0001\u0000\u0000\u0000\u02df\u02dd\u0001\u0000"+
		"\u0000\u0000\u02df\u02e0\u0001\u0000\u0000\u0000\u02e0k\u0001\u0000\u0000"+
		"\u0000\u02e1\u02e2\u0005D\u0000\u0000\u02e2\u02e4\u0005\u0003\u0000\u0000"+
		"\u02e3\u02e5\u0003n7\u0000\u02e4\u02e3\u0001\u0000\u0000\u0000\u02e5\u02e6"+
		"\u0001\u0000\u0000\u0000\u02e6\u02e4\u0001\u0000\u0000\u0000\u02e6\u02e7"+
		"\u0001\u0000\u0000\u0000\u02e7m\u0001\u0000\u0000\u0000\u02e8\u02ea\u0005"+
		"N\u0000\u0000\u02e9\u02eb\u0003\u0010\b\u0000\u02ea\u02e9\u0001\u0000"+
		"\u0000\u0000\u02ea\u02eb\u0001\u0000\u0000\u0000\u02eb\u02ec\u0001\u0000"+
		"\u0000\u0000\u02ec\u02f5\u0005\u0004\u0000\u0000\u02ed\u02f2\u0003p8\u0000"+
		"\u02ee\u02ef\u0005\u0001\u0000\u0000\u02ef\u02f1\u0003p8\u0000\u02f0\u02ee"+
		"\u0001\u0000\u0000\u0000\u02f1\u02f4\u0001\u0000\u0000\u0000\u02f2\u02f0"+
		"\u0001\u0000\u0000\u0000\u02f2\u02f3\u0001\u0000\u0000\u0000\u02f3\u02f6"+
		"\u0001\u0000\u0000\u0000\u02f4\u02f2\u0001\u0000\u0000\u0000\u02f5\u02ed"+
		"\u0001\u0000\u0000\u0000\u02f5\u02f6\u0001\u0000\u0000\u0000\u02f6\u02f7"+
		"\u0001\u0000\u0000\u0000\u02f7\u02f8\u0005\u0005\u0000\u0000\u02f8o\u0001"+
		"\u0000\u0000\u0000\u02f9\u02fa\u00068\uffff\uffff\u0000\u02fa\u0360\u0003"+
		"|>\u0000\u02fb\u02fc\u0005\u0004\u0000\u0000\u02fc\u02fd\u0003p8\u0000"+
		"\u02fd\u02fe\u0005\u0005\u0000\u0000\u02fe\u0360\u0001\u0000\u0000\u0000"+
		"\u02ff\u0360\u0003~?\u0000\u0300\u0360\u0003\u008aE\u0000\u0301\u0360"+
		"\u0003\u0088D\u0000\u0302\u0360\u0003\u0098L\u0000\u0303\u0360\u0003\u0094"+
		"J\u0000\u0304\u0360\u0003\u008eG\u0000\u0305\u0360\u0003z=\u0000\u0306"+
		"\u0307\u0005\u0011\u0000\u0000\u0307\u0360\u0003p8\u001a\u0308\u0309\u0005"+
		"\r\u0000\u0000\u0309\u0360\u0003p8\u0019\u030a\u030b\u0005\u000f\u0000"+
		"\u0000\u030b\u0360\u0003p8\u0018\u030c\u030d\u0005!\u0000\u0000\u030d"+
		"\u030e\u0005\u0004\u0000\u0000\u030e\u030f\u0003p8\u0000\u030f\u0315\u0005"+
		"\u0005\u0000\u0000\u0310\u0311\u0005\b\u0000\u0000\u0311\u0312\u0003p"+
		"8\u0000\u0312\u0313\u0005\t\u0000\u0000\u0313\u0316\u0001\u0000\u0000"+
		"\u0000\u0314\u0316\u0003p8\u0000\u0315\u0310\u0001\u0000\u0000\u0000\u0315"+
		"\u0314\u0001\u0000\u0000\u0000\u0316\u0317\u0001\u0000\u0000\u0000\u0317"+
		"\u031d\u0005\"\u0000\u0000\u0318\u0319\u0005\b\u0000\u0000\u0319\u031a"+
		"\u0003p8\u0000\u031a\u031b\u0005\t\u0000\u0000\u031b\u031e\u0001\u0000"+
		"\u0000\u0000\u031c\u031e\u0003p8\u0000\u031d\u0318\u0001\u0000\u0000\u0000"+
		"\u031d\u031c\u0001\u0000\u0000\u0000\u031e\u0360\u0001\u0000\u0000\u0000"+
		"\u031f\u0320\u0005#\u0000\u0000\u0320\u0321\u0005\u0004\u0000\u0000\u0321"+
		"\u0322\u0005N\u0000\u0000\u0322\u0323\u0005\u0003\u0000\u0000\u0323\u0324"+
		"\u0003\n\u0005\u0000\u0324\u0325\u0005\u001e\u0000\u0000\u0325\u0326\u0003"+
		"p8\u0000\u0326\u032c\u0005\u0005\u0000\u0000\u0327\u0328\u0005\b\u0000"+
		"\u0000\u0328\u0329\u0003p8\u0000\u0329\u032a\u0005\t\u0000\u0000\u032a"+
		"\u032d\u0001\u0000\u0000\u0000\u032b\u032d\u0003p8\u0000\u032c\u0327\u0001"+
		"\u0000\u0000\u0000\u032c\u032b\u0001\u0000\u0000\u0000\u032d\u0360\u0001"+
		"\u0000\u0000\u0000\u032e\u032f\u0005$\u0000\u0000\u032f\u0330\u0005\u0004"+
		"\u0000\u0000\u0330\u0331\u0003p8\u0000\u0331\u0332\u0005\u0005\u0000\u0000"+
		"\u0332\u0334\u0005\b\u0000\u0000\u0333\u0335\u0003r9\u0000\u0334\u0333"+
		"\u0001\u0000\u0000\u0000\u0335\u0336\u0001\u0000\u0000\u0000\u0336\u0334"+
		"\u0001\u0000\u0000\u0000\u0336\u0337\u0001\u0000\u0000\u0000\u0337\u0338"+
		"\u0001\u0000\u0000\u0000\u0338\u0339\u0005\t\u0000\u0000\u0339\u0360\u0001"+
		"\u0000\u0000\u0000\u033a\u033b\u0005$\u0000\u0000\u033b\u033d\u0005\b"+
		"\u0000\u0000\u033c\u033e\u0003t:\u0000\u033d\u033c\u0001\u0000\u0000\u0000"+
		"\u033e\u033f\u0001\u0000\u0000\u0000\u033f\u033d\u0001\u0000\u0000\u0000"+
		"\u033f\u0340\u0001\u0000\u0000\u0000\u0340\u0341\u0001\u0000\u0000\u0000"+
		"\u0341\u0342\u0005\t\u0000\u0000\u0342\u0360\u0001\u0000\u0000\u0000\u0343"+
		"\u0344\u00056\u0000\u0000\u0344\u0349\u0005N\u0000\u0000\u0345\u0346\u0005"+
		"\u0001\u0000\u0000\u0346\u0348\u0005N\u0000\u0000\u0347\u0345\u0001\u0000"+
		"\u0000\u0000\u0348\u034b\u0001\u0000\u0000\u0000\u0349\u0347\u0001\u0000"+
		"\u0000\u0000\u0349\u034a\u0001\u0000\u0000\u0000\u034a\u034c\u0001\u0000"+
		"\u0000\u0000\u034b\u0349\u0001\u0000\u0000\u0000\u034c\u034d\u0005\u0003"+
		"\u0000\u0000\u034d\u034e\u0003\n\u0005\u0000\u034e\u034f\u0005\u0001\u0000"+
		"\u0000\u034f\u0350\u0003p8\u0002\u0350\u0360\u0001\u0000\u0000\u0000\u0351"+
		"\u0352\u00057\u0000\u0000\u0352\u0357\u0005N\u0000\u0000\u0353\u0354\u0005"+
		"\u0001\u0000\u0000\u0354\u0356\u0005N\u0000\u0000\u0355\u0353\u0001\u0000"+
		"\u0000\u0000\u0356\u0359\u0001\u0000\u0000\u0000\u0357\u0355\u0001\u0000"+
		"\u0000\u0000\u0357\u0358\u0001\u0000\u0000\u0000\u0358\u035a\u0001\u0000"+
		"\u0000\u0000\u0359\u0357\u0001\u0000\u0000\u0000\u035a\u035b\u0005\u0003"+
		"\u0000\u0000\u035b\u035c\u0003\n\u0005\u0000\u035c\u035d\u0005\u0001\u0000"+
		"\u0000\u035d\u035e\u0003p8\u0001\u035e\u0360\u0001\u0000\u0000\u0000\u035f"+
		"\u02f9\u0001\u0000\u0000\u0000\u035f\u02fb\u0001\u0000\u0000\u0000\u035f"+
		"\u02ff\u0001\u0000\u0000\u0000\u035f\u0300\u0001\u0000\u0000\u0000\u035f"+
		"\u0301\u0001\u0000\u0000\u0000\u035f\u0302\u0001\u0000\u0000\u0000\u035f"+
		"\u0303\u0001\u0000\u0000\u0000\u035f\u0304\u0001\u0000\u0000\u0000\u035f"+
		"\u0305\u0001\u0000\u0000\u0000\u035f\u0306\u0001\u0000\u0000\u0000\u035f"+
		"\u0308\u0001\u0000\u0000\u0000\u035f\u030a\u0001\u0000\u0000\u0000\u035f"+
		"\u030c\u0001\u0000\u0000\u0000\u035f\u031f\u0001\u0000\u0000\u0000\u035f"+
		"\u032e\u0001\u0000\u0000\u0000\u035f\u033a\u0001\u0000\u0000\u0000\u035f"+
		"\u0343\u0001\u0000\u0000\u0000\u035f\u0351\u0001\u0000\u0000\u0000\u0360"+
		"\u0396\u0001\u0000\u0000\u0000\u0361\u0362\n\u0017\u0000\u0000\u0362\u0363"+
		"\u0005\u0012\u0000\u0000\u0363\u0395\u0003p8\u0018\u0364\u0365\n\u0016"+
		"\u0000\u0000\u0365\u0366\u0005\u0013\u0000\u0000\u0366\u0395\u0003p8\u0017"+
		"\u0367\u0368\n\u0015\u0000\u0000\u0368\u0369\u0005\u0014\u0000\u0000\u0369"+
		"\u0395\u0003p8\u0016\u036a\u036b\n\u0014\u0000\u0000\u036b\u036c\u0005"+
		"\u0015\u0000\u0000\u036c\u0395\u0003p8\u0015\u036d\u036e\n\u0013\u0000"+
		"\u0000\u036e\u036f\u0005\u0016\u0000\u0000\u036f\u0395\u0003p8\u0014\u0370"+
		"\u0371\n\u0012\u0000\u0000\u0371\u0372\u0005\u0017\u0000\u0000\u0372\u0395"+
		"\u0003p8\u0013\u0373\u0374\n\u0011\u0000\u0000\u0374\u0375\u0005\u0018"+
		"\u0000\u0000\u0375\u0395\u0003p8\u0012\u0376\u0377\n\u0010\u0000\u0000"+
		"\u0377\u0378\u0005\u0019\u0000\u0000\u0378\u0395\u0003p8\u0011\u0379\u037a"+
		"\n\u000f\u0000\u0000\u037a\u037b\u0005\u001a\u0000\u0000\u037b\u0395\u0003"+
		"p8\u0010\u037c\u037d\n\u000e\u0000\u0000\u037d\u037e\u0005%\u0000\u0000"+
		"\u037e\u0395\u0003p8\u000f\u037f\u0380\n\r\u0000\u0000\u0380\u0381\u0005"+
		"\u001d\u0000\u0000\u0381\u0395\u0003p8\u000e\u0382\u0383\n\f\u0000\u0000"+
		"\u0383\u0384\u0005\u001b\u0000\u0000\u0384\u0395\u0003p8\r\u0385\u0386"+
		"\n\u000b\u0000\u0000\u0386\u0387\u0005\u001c\u0000\u0000\u0387\u0395\u0003"+
		"p8\f\u0388\u0389\n\n\u0000\u0000\u0389\u038a\u0005\r\u0000\u0000\u038a"+
		"\u0395\u0003p8\u000b\u038b\u038c\n\t\u0000\u0000\u038c\u038d\u0005\u000f"+
		"\u0000\u0000\u038d\u0395\u0003p8\n\u038e\u038f\n\b\u0000\u0000\u038f\u0390"+
		"\u0005\u001f\u0000\u0000\u0390\u0395\u0003p8\b\u0391\u0392\n\u0007\u0000"+
		"\u0000\u0392\u0393\u0005 \u0000\u0000\u0393\u0395\u0003p8\b\u0394\u0361"+
		"\u0001\u0000\u0000\u0000\u0394\u0364\u0001\u0000\u0000\u0000\u0394\u0367"+
		"\u0001\u0000\u0000\u0000\u0394\u036a\u0001\u0000\u0000\u0000\u0394\u036d"+
		"\u0001\u0000\u0000\u0000\u0394\u0370\u0001\u0000\u0000\u0000\u0394\u0373"+
		"\u0001\u0000\u0000\u0000\u0394\u0376\u0001\u0000\u0000\u0000\u0394\u0379"+
		"\u0001\u0000\u0000\u0000\u0394\u037c\u0001\u0000\u0000\u0000\u0394\u037f"+
		"\u0001\u0000\u0000\u0000\u0394\u0382\u0001\u0000\u0000\u0000\u0394\u0385"+
		"\u0001\u0000\u0000\u0000\u0394\u0388\u0001\u0000\u0000\u0000\u0394\u038b"+
		"\u0001\u0000\u0000\u0000\u0394\u038e\u0001\u0000\u0000\u0000\u0394\u0391"+
		"\u0001\u0000\u0000\u0000\u0395\u0398\u0001\u0000\u0000\u0000\u0396\u0394"+
		"\u0001\u0000\u0000\u0000\u0396\u0397\u0001\u0000\u0000\u0000\u0397q\u0001"+
		"\u0000\u0000\u0000\u0398\u0396\u0001\u0000\u0000\u0000\u0399\u039a\u0003"+
		"v;\u0000\u039a\u039b\u0005\'\u0000\u0000\u039b\u039c\u0003p8\u0000\u039c"+
		"\u03a1\u0001\u0000\u0000\u0000\u039d\u039e\u0005\"\u0000\u0000\u039e\u039f"+
		"\u0005\'\u0000\u0000\u039f\u03a1\u0003p8\u0000\u03a0\u0399\u0001\u0000"+
		"\u0000\u0000\u03a0\u039d\u0001\u0000\u0000\u0000\u03a1s\u0001\u0000\u0000"+
		"\u0000\u03a2\u03a3\u0003p8\u0000\u03a3\u03a4\u0005\'\u0000\u0000\u03a4"+
		"\u03a5\u0003p8\u0000\u03a5\u03aa\u0001\u0000\u0000\u0000\u03a6\u03a7\u0005"+
		"\"\u0000\u0000\u03a7\u03a8\u0005\'\u0000\u0000\u03a8\u03aa\u0003p8\u0000"+
		"\u03a9\u03a2\u0001\u0000\u0000\u0000\u03a9\u03a6\u0001\u0000\u0000\u0000"+
		"\u03aau\u0001\u0000\u0000\u0000\u03ab\u03ae\u0003|>\u0000\u03ac\u03ae"+
		"\u0003\u0094J\u0000\u03ad\u03ab\u0001\u0000\u0000\u0000\u03ad\u03ac\u0001"+
		"\u0000\u0000\u0000\u03aew\u0001\u0000\u0000\u0000\u03af\u03b0\u0006<\uffff"+
		"\uffff\u0000\u03b0\u03b7\u0003\u0004\u0002\u0000\u03b1\u03b7\u0005N\u0000"+
		"\u0000\u03b2\u03b3\u0005\u0004\u0000\u0000\u03b3\u03b4\u0003x<\u0000\u03b4"+
		"\u03b5\u0005\u0005\u0000\u0000\u03b5\u03b7\u0001\u0000\u0000\u0000\u03b6"+
		"\u03af\u0001\u0000\u0000\u0000\u03b6\u03b1\u0001\u0000\u0000\u0000\u03b6"+
		"\u03b2\u0001\u0000\u0000\u0000\u03b7\u03bd\u0001\u0000\u0000\u0000\u03b8"+
		"\u03b9\n\u0001\u0000\u0000\u03b9\u03ba\u0005\n\u0000\u0000\u03ba\u03bc"+
		"\u0003x<\u0002\u03bb\u03b8\u0001\u0000\u0000\u0000\u03bc\u03bf\u0001\u0000"+
		"\u0000\u0000\u03bd\u03bb\u0001\u0000\u0000\u0000\u03bd\u03be\u0001\u0000"+
		"\u0000\u0000\u03bey\u0001\u0000\u0000\u0000\u03bf\u03bd\u0001\u0000\u0000"+
		"\u0000\u03c0\u03c1\u0005N\u0000\u0000\u03c1\u03c2\u0005\u0002\u0000\u0000"+
		"\u03c2\u03c3\u0005N\u0000\u0000\u03c3\u03c4\u0005\u0010\u0000\u0000\u03c4"+
		"\u03c5\u0005N\u0000\u0000\u03c5\u03c6\u0005\u0002\u0000\u0000\u03c6\u03c7"+
		"\u0005N\u0000\u0000\u03c7{\u0001\u0000\u0000\u0000\u03c8\u03c9\u0007\u0005"+
		"\u0000\u0000\u03c9}\u0001\u0000\u0000\u0000\u03ca\u03ce\u0003\u0080@\u0000"+
		"\u03cb\u03ce\u0003\u0082A\u0000\u03cc\u03ce\u0003\u0084B\u0000\u03cd\u03ca"+
		"\u0001\u0000\u0000\u0000\u03cd\u03cb\u0001\u0000\u0000\u0000\u03cd\u03cc"+
		"\u0001\u0000\u0000\u0000\u03ce\u007f\u0001\u0000\u0000\u0000\u03cf\u03d1"+
		"\u0005*\u0000\u0000\u03d0\u03d2\u0003\u0010\b\u0000\u03d1\u03d0\u0001"+
		"\u0000\u0000\u0000\u03d1\u03d2\u0001\u0000\u0000\u0000\u03d2\u03d3\u0001"+
		"\u0000\u0000\u0000\u03d3\u03dc\u0005\u0004\u0000\u0000\u03d4\u03d9\u0003"+
		"p8\u0000\u03d5\u03d6\u0005\u0001\u0000\u0000\u03d6\u03d8\u0003p8\u0000"+
		"\u03d7\u03d5\u0001\u0000\u0000\u0000\u03d8\u03db\u0001\u0000\u0000\u0000"+
		"\u03d9\u03d7\u0001\u0000\u0000\u0000\u03d9\u03da\u0001\u0000\u0000\u0000"+
		"\u03da\u03dd\u0001\u0000\u0000\u0000\u03db\u03d9\u0001\u0000\u0000\u0000"+
		"\u03dc\u03d4\u0001\u0000\u0000\u0000\u03dc\u03dd\u0001\u0000\u0000\u0000"+
		"\u03dd\u03de\u0001\u0000\u0000\u0000\u03de\u03df\u0005\u0005\u0000\u0000"+
		"\u03df\u0081\u0001\u0000\u0000\u0000\u03e0\u03e2\u0005+\u0000\u0000\u03e1"+
		"\u03e3\u0003\u0010\b\u0000\u03e2\u03e1\u0001\u0000\u0000\u0000\u03e2\u03e3"+
		"\u0001\u0000\u0000\u0000\u03e3\u03e4\u0001\u0000\u0000\u0000\u03e4\u03ed"+
		"\u0005\u0004\u0000\u0000\u03e5\u03ea\u0003p8\u0000\u03e6\u03e7\u0005\u0001"+
		"\u0000\u0000\u03e7\u03e9\u0003p8\u0000\u03e8\u03e6\u0001\u0000\u0000\u0000"+
		"\u03e9\u03ec\u0001\u0000\u0000\u0000\u03ea\u03e8\u0001\u0000\u0000\u0000"+
		"\u03ea\u03eb\u0001\u0000\u0000\u0000\u03eb\u03ee\u0001\u0000\u0000\u0000"+
		"\u03ec\u03ea\u0001\u0000\u0000\u0000\u03ed\u03e5\u0001\u0000\u0000\u0000"+
		"\u03ed\u03ee\u0001\u0000\u0000\u0000\u03ee\u03ef\u0001\u0000\u0000\u0000"+
		"\u03ef\u03f0\u0005\u0005\u0000\u0000\u03f0\u0083\u0001\u0000\u0000\u0000"+
		"\u03f1\u03f3\u0005,\u0000\u0000\u03f2\u03f4\u0003\u0010\b\u0000\u03f3"+
		"\u03f2\u0001\u0000\u0000\u0000\u03f3\u03f4\u0001\u0000\u0000\u0000\u03f4"+
		"\u03f5\u0001\u0000\u0000\u0000\u03f5\u03fe\u0005\u0004\u0000\u0000\u03f6"+
		"\u03fb\u0003\u0086C\u0000\u03f7\u03f8\u0005\u0001\u0000\u0000\u03f8\u03fa"+
		"\u0003\u0086C\u0000\u03f9\u03f7\u0001\u0000\u0000\u0000\u03fa\u03fd\u0001"+
		"\u0000\u0000\u0000\u03fb\u03f9\u0001\u0000\u0000\u0000\u03fb\u03fc\u0001"+
		"\u0000\u0000\u0000\u03fc\u03ff\u0001\u0000\u0000\u0000\u03fd\u03fb\u0001"+
		"\u0000\u0000\u0000\u03fe\u03f6\u0001\u0000\u0000\u0000\u03fe\u03ff\u0001"+
		"\u0000\u0000\u0000\u03ff\u0400\u0001\u0000\u0000\u0000\u0400\u0401\u0005"+
		"\u0005\u0000\u0000\u0401\u0085\u0001\u0000\u0000\u0000\u0402\u0403\u0003"+
		"p8\u0000\u0403\u0404\u0005&\u0000\u0000\u0404\u0405\u0003p8\u0000\u0405"+
		"\u0087\u0001\u0000\u0000\u0000\u0406\u040e\u0006D\uffff\uffff\u0000\u0407"+
		"\u040f\u0003\u008eG\u0000\u0408\u040f\u0003\u0098L\u0000\u0409\u040f\u0003"+
		"~?\u0000\u040a\u040b\u0005\u0004\u0000\u0000\u040b\u040c\u0003p8\u0000"+
		"\u040c\u040d\u0005\u0005\u0000\u0000\u040d\u040f\u0001\u0000\u0000\u0000"+
		"\u040e\u0407\u0001\u0000\u0000\u0000\u040e\u0408\u0001\u0000\u0000\u0000"+
		"\u040e\u0409\u0001\u0000\u0000\u0000\u040e\u040a\u0001\u0000\u0000\u0000"+
		"\u040f\u0410\u0001\u0000\u0000\u0000\u0410\u0411\u0005\u0006\u0000\u0000"+
		"\u0411\u0412\u0003p8\u0000\u0412\u0413\u0005\u0007\u0000\u0000\u0413\u0438"+
		"\u0001\u0000\u0000\u0000\u0414\u041c\u0003\u008eG\u0000\u0415\u041c\u0003"+
		"\u0098L\u0000\u0416\u041c\u0003~?\u0000\u0417\u0418\u0005\u0004\u0000"+
		"\u0000\u0418\u0419\u0003p8\u0000\u0419\u041a\u0005\u0005\u0000\u0000\u041a"+
		"\u041c\u0001\u0000\u0000\u0000\u041b\u0414\u0001\u0000\u0000\u0000\u041b"+
		"\u0415\u0001\u0000\u0000\u0000\u041b\u0416\u0001\u0000\u0000\u0000\u041b"+
		"\u0417\u0001\u0000\u0000\u0000\u041c\u041d\u0001\u0000\u0000\u0000\u041d"+
		"\u041e\u0005\u0002\u0000\u0000\u041e\u041f\u0005N\u0000\u0000\u041f\u0428"+
		"\u0005\u0004\u0000\u0000\u0420\u0425\u0003\u0090H\u0000\u0421\u0422\u0005"+
		"\u0001\u0000\u0000\u0422\u0424\u0003\u0090H\u0000\u0423\u0421\u0001\u0000"+
		"\u0000\u0000\u0424\u0427\u0001\u0000\u0000\u0000\u0425\u0423\u0001\u0000"+
		"\u0000\u0000\u0425\u0426\u0001\u0000\u0000\u0000\u0426\u0429\u0001\u0000"+
		"\u0000\u0000\u0427\u0425\u0001\u0000\u0000\u0000\u0428\u0420\u0001\u0000"+
		"\u0000\u0000\u0428\u0429\u0001\u0000\u0000\u0000\u0429\u042a\u0001\u0000"+
		"\u0000\u0000\u042a\u042b\u0005\u0005\u0000\u0000\u042b\u0438\u0001\u0000"+
		"\u0000\u0000\u042c\u0433\u0003\u008eG\u0000\u042d\u0433\u0003~?\u0000"+
		"\u042e\u042f\u0005\u0004\u0000\u0000\u042f\u0430\u0003p8\u0000\u0430\u0431"+
		"\u0005\u0005\u0000\u0000\u0431\u0433\u0001\u0000\u0000\u0000\u0432\u042c"+
		"\u0001\u0000\u0000\u0000\u0432\u042d\u0001\u0000\u0000\u0000\u0432\u042e"+
		"\u0001\u0000\u0000\u0000\u0433\u0434\u0001\u0000\u0000\u0000\u0434\u0435"+
		"\u0005\u0002\u0000\u0000\u0435\u0436\u0005N\u0000\u0000\u0436\u0438\u0001"+
		"\u0000\u0000\u0000\u0437\u0406\u0001\u0000\u0000\u0000\u0437\u041b\u0001"+
		"\u0000\u0000\u0000\u0437\u0432\u0001\u0000\u0000\u0000\u0438\u0452\u0001"+
		"\u0000\u0000\u0000\u0439\u043a\n\u0006\u0000\u0000\u043a\u043b\u0005\u0006"+
		"\u0000\u0000\u043b\u043c\u0003p8\u0000\u043c\u043d\u0005\u0007\u0000\u0000"+
		"\u043d\u0451\u0001\u0000\u0000\u0000\u043e\u043f\n\u0005\u0000\u0000\u043f"+
		"\u0440\u0005\u0002\u0000\u0000\u0440\u0441\u0005N\u0000\u0000\u0441\u044a"+
		"\u0005\u0004\u0000\u0000\u0442\u0447\u0003\u0090H\u0000\u0443\u0444\u0005"+
		"\u0001\u0000\u0000\u0444\u0446\u0003\u0090H\u0000\u0445\u0443\u0001\u0000"+
		"\u0000\u0000\u0446\u0449\u0001\u0000\u0000\u0000\u0447\u0445\u0001\u0000"+
		"\u0000\u0000\u0447\u0448\u0001\u0000\u0000\u0000\u0448\u044b\u0001\u0000"+
		"\u0000\u0000\u0449\u0447\u0001\u0000\u0000\u0000\u044a\u0442\u0001\u0000"+
		"\u0000\u0000\u044a\u044b\u0001\u0000\u0000\u0000\u044b\u044c\u0001\u0000"+
		"\u0000\u0000\u044c\u0451\u0005\u0005\u0000\u0000\u044d\u044e\n\u0004\u0000"+
		"\u0000\u044e\u044f\u0005\u0002\u0000\u0000\u044f\u0451\u0005N\u0000\u0000"+
		"\u0450\u0439\u0001\u0000\u0000\u0000\u0450\u043e\u0001\u0000\u0000\u0000"+
		"\u0450\u044d\u0001\u0000\u0000\u0000\u0451\u0454\u0001\u0000\u0000\u0000"+
		"\u0452\u0450\u0001\u0000\u0000\u0000\u0452\u0453\u0001\u0000\u0000\u0000"+
		"\u0453\u0089\u0001\u0000\u0000\u0000\u0454\u0452\u0001\u0000\u0000\u0000"+
		"\u0455\u045a\u0003\u008cF\u0000\u0456\u0457\u0005\u0002\u0000\u0000\u0457"+
		"\u0459\u0005N\u0000\u0000\u0458\u0456\u0001\u0000\u0000\u0000\u0459\u045c"+
		"\u0001\u0000\u0000\u0000\u045a\u0458\u0001\u0000\u0000\u0000\u045a\u045b"+
		"\u0001\u0000\u0000\u0000\u045b\u008b\u0001\u0000\u0000\u0000\u045c\u045a"+
		"\u0001\u0000\u0000\u0000\u045d\u0460\u0005I\u0000\u0000\u045e\u045f\u0005"+
		"\u0002\u0000\u0000\u045f\u0461\u0005N\u0000\u0000\u0460\u045e\u0001\u0000"+
		"\u0000\u0000\u0461\u0462\u0001\u0000\u0000\u0000\u0462\u0460\u0001\u0000"+
		"\u0000\u0000\u0462\u0463\u0001\u0000\u0000\u0000\u0463\u0464\u0001\u0000"+
		"\u0000\u0000\u0464\u046d\u0005\u0004\u0000\u0000\u0465\u046a\u0003\u0090"+
		"H\u0000\u0466\u0467\u0005\u0001\u0000\u0000\u0467\u0469\u0003\u0090H\u0000"+
		"\u0468\u0466\u0001\u0000\u0000\u0000\u0469\u046c\u0001\u0000\u0000\u0000"+
		"\u046a\u0468\u0001\u0000\u0000\u0000\u046a\u046b\u0001\u0000\u0000\u0000"+
		"\u046b\u046e\u0001\u0000\u0000\u0000\u046c\u046a\u0001\u0000\u0000\u0000"+
		"\u046d\u0465\u0001\u0000\u0000\u0000\u046d\u046e\u0001\u0000\u0000\u0000"+
		"\u046e\u046f\u0001\u0000\u0000\u0000\u046f\u0484\u0005\u0005\u0000\u0000"+
		"\u0470\u0473\u0005N\u0000\u0000\u0471\u0472\u0005\u0002\u0000\u0000\u0472"+
		"\u0474\u0005N\u0000\u0000\u0473\u0471\u0001\u0000\u0000\u0000\u0474\u0475"+
		"\u0001\u0000\u0000\u0000\u0475\u0473\u0001\u0000\u0000\u0000\u0475\u0476"+
		"\u0001\u0000\u0000\u0000\u0476\u0477\u0001\u0000\u0000\u0000\u0477\u0480"+
		"\u0005\u0004\u0000\u0000\u0478\u047d\u0003\u0090H\u0000\u0479\u047a\u0005"+
		"\u0001\u0000\u0000\u047a\u047c\u0003\u0090H\u0000\u047b\u0479\u0001\u0000"+
		"\u0000\u0000\u047c\u047f\u0001\u0000\u0000\u0000\u047d\u047b\u0001\u0000"+
		"\u0000\u0000\u047d\u047e\u0001\u0000\u0000\u0000\u047e\u0481\u0001\u0000"+
		"\u0000\u0000\u047f\u047d\u0001\u0000\u0000\u0000\u0480\u0478\u0001\u0000"+
		"\u0000\u0000\u0480\u0481\u0001\u0000\u0000\u0000\u0481\u0482\u0001\u0000"+
		"\u0000\u0000\u0482\u0484\u0005\u0005\u0000\u0000\u0483\u045d\u0001\u0000"+
		"\u0000\u0000\u0483\u0470\u0001\u0000\u0000\u0000\u0484\u008d\u0001\u0000"+
		"\u0000\u0000\u0485\u0487\u0005N\u0000\u0000\u0486\u0488\u0003\u0010\b"+
		"\u0000\u0487\u0486\u0001\u0000\u0000\u0000\u0487\u0488\u0001\u0000\u0000"+
		"\u0000\u0488\u0489\u0001\u0000\u0000\u0000\u0489\u0492\u0005\u0004\u0000"+
		"\u0000\u048a\u048f\u0003\u0090H\u0000\u048b\u048c\u0005\u0001\u0000\u0000"+
		"\u048c\u048e\u0003\u0090H\u0000\u048d\u048b\u0001\u0000\u0000\u0000\u048e"+
		"\u0491\u0001\u0000\u0000\u0000\u048f\u048d\u0001\u0000\u0000\u0000\u048f"+
		"\u0490\u0001\u0000\u0000\u0000\u0490\u0493\u0001\u0000\u0000\u0000\u0491"+
		"\u048f\u0001\u0000\u0000\u0000\u0492\u048a\u0001\u0000\u0000\u0000\u0492"+
		"\u0493\u0001\u0000\u0000\u0000\u0493\u0494\u0001\u0000\u0000\u0000\u0494"+
		"\u0495\u0005\u0005\u0000\u0000\u0495\u008f\u0001\u0000\u0000\u0000\u0496"+
		"\u0499\u0003\u0092I\u0000\u0497\u0499\u0003p8\u0000\u0498\u0496\u0001"+
		"\u0000\u0000\u0000\u0498\u0497\u0001\u0000\u0000\u0000\u0499\u0091\u0001"+
		"\u0000\u0000\u0000\u049a\u049b\u0005N\u0000\u0000\u049b\u049c\u0005\'"+
		"\u0000\u0000\u049c\u04a5\u0003p8\u0000\u049d\u049e\u0005\u0004\u0000\u0000"+
		"\u049e\u049f\u0005N\u0000\u0000\u049f\u04a0\u0005\u0001\u0000\u0000\u04a0"+
		"\u04a1\u0005N\u0000\u0000\u04a1\u04a2\u0005\u0005\u0000\u0000\u04a2\u04a3"+
		"\u0005\'\u0000\u0000\u04a3\u04a5\u0003p8\u0000\u04a4\u049a\u0001\u0000"+
		"\u0000\u0000\u04a4\u049d\u0001\u0000\u0000\u0000\u04a5\u0093\u0001\u0000"+
		"\u0000\u0000\u04a6\u04a7\u0003\n\u0005\u0000\u04a7\u04a8\u0005\b\u0000"+
		"\u0000\u04a8\u04ad\u0003\u0096K\u0000\u04a9\u04aa\u0005\u0001\u0000\u0000"+
		"\u04aa\u04ac\u0003\u0096K\u0000\u04ab\u04a9\u0001\u0000\u0000\u0000\u04ac"+
		"\u04af\u0001\u0000\u0000\u0000\u04ad\u04ab\u0001\u0000\u0000\u0000\u04ad"+
		"\u04ae\u0001\u0000\u0000\u0000\u04ae\u04b0\u0001\u0000\u0000\u0000\u04af"+
		"\u04ad\u0001\u0000\u0000\u0000\u04b0\u04b1\u0005\t\u0000\u0000\u04b1\u0095"+
		"\u0001\u0000\u0000\u0000\u04b2\u04b3\u0005N\u0000\u0000\u04b3\u04b4\u0005"+
		"\u001e\u0000\u0000\u04b4\u04b5\u0003p8\u0000\u04b5\u0097\u0001\u0000\u0000"+
		"\u0000\u04b6\u04b7\u0005I\u0000\u0000\u04b7\u04b8\u0005\u0002\u0000\u0000"+
		"\u04b8\u04bd\u0005N\u0000\u0000\u04b9\u04ba\u0005\u0002\u0000\u0000\u04ba"+
		"\u04bc\u0005N\u0000\u0000\u04bb\u04b9\u0001\u0000\u0000\u0000\u04bc\u04bf"+
		"\u0001\u0000\u0000\u0000\u04bd\u04bb\u0001\u0000\u0000\u0000\u04bd\u04be"+
		"\u0001\u0000\u0000\u0000\u04be\u04c9\u0001\u0000\u0000\u0000\u04bf\u04bd"+
		"\u0001\u0000\u0000\u0000\u04c0\u04c5\u0005N\u0000\u0000\u04c1\u04c2\u0005"+
		"\u0002\u0000\u0000\u04c2\u04c4\u0005N\u0000\u0000\u04c3\u04c1\u0001\u0000"+
		"\u0000\u0000\u04c4\u04c7\u0001\u0000\u0000\u0000\u04c5\u04c3\u0001\u0000"+
		"\u0000\u0000\u04c5\u04c6\u0001\u0000\u0000\u0000\u04c6\u04c9\u0001\u0000"+
		"\u0000\u0000\u04c7\u04c5\u0001\u0000\u0000\u0000\u04c8\u04b6\u0001\u0000"+
		"\u0000\u0000\u04c8\u04c0\u0001\u0000\u0000\u0000\u04c9\u0099\u0001\u0000"+
		"\u0000\u0000\u0081\u009c\u009e\u00ab\u00b0\u00b4\u00b9\u00bd\u00c1\u00c5"+
		"\u00c8\u00d2\u00d7\u00db\u00e1\u00e9\u00f4\u00fc\u0105\u010d\u0115\u011a"+
		"\u0120\u012a\u0134\u0143\u0147\u0151\u0157\u0170\u0175\u0182\u0188\u018e"+
		"\u0193\u019a\u01b1\u01b5\u01bd\u01c3\u01c6\u01ce\u01da\u01de\u01e8\u01ee"+
		"\u01f8\u0208\u0210\u0218\u021b\u0229\u022e\u0237\u023c\u023e\u0244\u0250"+
		"\u0257\u025d\u0263\u026a\u0270\u0276\u027f\u0286\u028a\u0290\u029d\u02a4"+
		"\u02af\u02b7\u02d8\u02df\u02e6\u02ea\u02f2\u02f5\u0315\u031d\u032c\u0336"+
		"\u033f\u0349\u0357\u035f\u0394\u0396\u03a0\u03a9\u03ad\u03b6\u03bd\u03cd"+
		"\u03d1\u03d9\u03dc\u03e2\u03ea\u03ed\u03f3\u03fb\u03fe\u040e\u041b\u0425"+
		"\u0428\u0432\u0437\u0447\u044a\u0450\u0452\u045a\u0462\u046a\u046d\u0475"+
		"\u047d\u0480\u0483\u0487\u048f\u0492\u0498\u04a4\u04ad\u04bd\u04c5\u04c8";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}