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
		RCURLY=9, PARALLEL=10, TRUE=11, FALSE=12, AND=13, MODELS=14, OR=15, NOT=16, 
		TIMES=17, DIV=18, MOD=19, PLUS=20, MINUS=21, LT=22, LTE=23, GT=24, GTE=25, 
		EQ=26, NEQ=27, ASGN_EQ=28, IMPLIES=29, IFF=30, IF=31, ELSE=32, LET=33, 
		WHEN=34, IN=35, ARROW=36, IMPORT=37, EXPORT=38, OBJ=39, SORT=40, PROC=41, 
		COMPILE=42, SPEC=43, INVARIANT=44, ALL=45, EXISTS=46, VAR=47, CONST=48, 
		CONSTRUCTOR=49, TRANSITION=50, INTERNAL=51, PROVIDER=52, CLIENT=53, SESSION=54, 
		GUARD=55, TRANSIT=56, ERROR=57, BEFORE=58, AFTER=59, FUN=60, PROCFUN=61, 
		RETURN=62, REAL=63, INT=64, ID=65, STRING=66, WS=67, COMMENT=68, LINE_COMMENT=69;
	public static final int
		RULE_root = 0, RULE_import_stmt = 1, RULE_qualified_name = 2, RULE_name_id = 3, 
		RULE_decl = 4, RULE_typeExpr = 5, RULE_typeArgs = 6, RULE_typeParams = 7, 
		RULE_fun_decl = 8, RULE_procfun_decl = 9, RULE_procfun_body = 10, RULE_proc = 11, 
		RULE_obj = 12, RULE_sort_decl = 13, RULE_compile_decl = 14, RULE_spec = 15, 
		RULE_ag_spec = 16, RULE_assume_expr = 17, RULE_system_expr = 18, RULE_system_atom = 19, 
		RULE_system_primary = 20, RULE_system_leaf = 21, RULE_invariant_decl = 22, 
		RULE_pclass_body = 23, RULE_field = 24, RULE_var = 25, RULE_constructor = 26, 
		RULE_transition = 27, RULE_args = 28, RULE_arg = 29, RULE_constructor_body = 30, 
		RULE_action_body = 31, RULE_return_clause = 32, RULE_guard = 33, RULE_transit = 34, 
		RULE_error = 35, RULE_error_arm = 36, RULE_var_transit = 37, RULE_before = 38, 
		RULE_after = 39, RULE_call_stmt = 40, RULE_expr = 41, RULE_when_subject_arm = 42, 
		RULE_when_guard_arm = 43, RULE_when_pattern = 44, RULE_proc_expr = 45, 
		RULE_literal = 46, RULE_bracket_literal = 47, RULE_map_entry = 48, RULE_set_literal = 49, 
		RULE_index_expr = 50, RULE_method_prop_expr = 51, RULE_index_or_slice = 52, 
		RULE_method_call = 53, RULE_fun_call = 54, RULE_call_arg = 55, RULE_lambda_expr = 56, 
		RULE_oclass_literal = 57, RULE_oclass_field_assign = 58, RULE_field_access = 59;
	private static String[] makeRuleNames() {
		return new String[] {
			"root", "import_stmt", "qualified_name", "name_id", "decl", "typeExpr", 
			"typeArgs", "typeParams", "fun_decl", "procfun_decl", "procfun_body", 
			"proc", "obj", "sort_decl", "compile_decl", "spec", "ag_spec", "assume_expr", 
			"system_expr", "system_atom", "system_primary", "system_leaf", "invariant_decl", 
			"pclass_body", "field", "var", "constructor", "transition", "args", "arg", 
			"constructor_body", "action_body", "return_clause", "guard", "transit", 
			"error", "error_arm", "var_transit", "before", "after", "call_stmt", 
			"expr", "when_subject_arm", "when_guard_arm", "when_pattern", "proc_expr", 
			"literal", "bracket_literal", "map_entry", "set_literal", "index_expr", 
			"method_prop_expr", "index_or_slice", "method_call", "fun_call", "call_arg", 
			"lambda_expr", "oclass_literal", "oclass_field_assign", "field_access"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "','", "'.'", "':'", "'('", "')'", "'['", "']'", "'{'", "'}'", 
			"'||'", "'true'", "'false'", "'&'", "'|='", "'|'", "'~'", "'*'", "'/'", 
			"'%'", "'+'", "'-'", "'<'", "'<='", "'>'", "'>='", "'='", "'~='", "':='", 
			"'=>'", "'<=>'", "'if'", "'else'", "'let'", "'when'", "'in'", "'->'", 
			"'import'", "'export'", "'obj'", "'sort'", "'proc'", "'compile'", "'spec'", 
			"'invariant'", "'all'", "'exists'", "'var'", "'const'", "'constructor'", 
			"'transition'", "'internal'", "'provider'", "'client'", "'session'", 
			"'guard'", "'transit'", "'error'", "'before'", "'after'", "'fun'", "'procfun'", 
			"'return'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "COMMA", "DOT", "COLON", "LPAREN", "RPAREN", "LBRACK", "RBRACK", 
			"LCURLY", "RCURLY", "PARALLEL", "TRUE", "FALSE", "AND", "MODELS", "OR", 
			"NOT", "TIMES", "DIV", "MOD", "PLUS", "MINUS", "LT", "LTE", "GT", "GTE", 
			"EQ", "NEQ", "ASGN_EQ", "IMPLIES", "IFF", "IF", "ELSE", "LET", "WHEN", 
			"IN", "ARROW", "IMPORT", "EXPORT", "OBJ", "SORT", "PROC", "COMPILE", 
			"SPEC", "INVARIANT", "ALL", "EXISTS", "VAR", "CONST", "CONSTRUCTOR", 
			"TRANSITION", "INTERNAL", "PROVIDER", "CLIENT", "SESSION", "GUARD", "TRANSIT", 
			"ERROR", "BEFORE", "AFTER", "FUN", "PROCFUN", "RETURN", "REAL", "INT", 
			"ID", "STRING", "WS", "COMMENT", "LINE_COMMENT"
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
			setState(124);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 3458799560753676288L) != 0)) {
				{
				setState(122);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case IMPORT:
					{
					setState(120);
					import_stmt();
					}
					break;
				case EXPORT:
				case OBJ:
				case SORT:
				case PROC:
				case COMPILE:
				case SPEC:
				case INVARIANT:
				case FUN:
				case PROCFUN:
					{
					setState(121);
					decl();
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				}
				setState(126);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(127);
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
			setState(129);
			match(IMPORT);
			setState(130);
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
			setState(132);
			name_id();
			setState(135); 
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					{
					setState(133);
					match(DOT);
					setState(134);
					name_id();
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(137); 
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
		public TerminalNode CLIENT() { return getToken(JulayParser.CLIENT, 0); }
		public TerminalNode PROVIDER() { return getToken(JulayParser.PROVIDER, 0); }
		public TerminalNode INTERNAL() { return getToken(JulayParser.INTERNAL, 0); }
		public TerminalNode SESSION() { return getToken(JulayParser.SESSION, 0); }
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
			setState(139);
			_la = _input.LA(1);
			if ( !(((((_la - 51)) & ~0x3f) == 0 && ((1L << (_la - 51)) & 16399L) != 0)) ) {
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
		public ObjContext obj() {
			return getRuleContext(ObjContext.class,0);
		}
		public Sort_declContext sort_decl() {
			return getRuleContext(Sort_declContext.class,0);
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
		public Fun_declContext fun_decl() {
			return getRuleContext(Fun_declContext.class,0);
		}
		public Procfun_declContext procfun_decl() {
			return getRuleContext(Procfun_declContext.class,0);
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
			setState(170);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,10,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(142);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==EXPORT) {
					{
					setState(141);
					match(EXPORT);
					}
				}

				setState(144);
				proc();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(146);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==EXPORT) {
					{
					setState(145);
					match(EXPORT);
					}
				}

				setState(148);
				obj();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(150);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==EXPORT) {
					{
					setState(149);
					match(EXPORT);
					}
				}

				setState(152);
				sort_decl();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(153);
				compile_decl();
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(155);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==EXPORT) {
					{
					setState(154);
					match(EXPORT);
					}
				}

				setState(157);
				spec();
				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(159);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==EXPORT) {
					{
					setState(158);
					match(EXPORT);
					}
				}

				setState(161);
				invariant_decl();
				}
				break;
			case 7:
				enterOuterAlt(_localctx, 7);
				{
				setState(163);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==EXPORT) {
					{
					setState(162);
					match(EXPORT);
					}
				}

				setState(165);
				fun_decl();
				}
				break;
			case 8:
				enterOuterAlt(_localctx, 8);
				{
				setState(167);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==EXPORT) {
					{
					setState(166);
					match(EXPORT);
					}
				}

				setState(169);
				procfun_decl();
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
		public TerminalNode ID() { return getToken(JulayParser.ID, 0); }
		public TypeArgsContext typeArgs() {
			return getRuleContext(TypeArgsContext.class,0);
		}
		public TerminalNode LPAREN() { return getToken(JulayParser.LPAREN, 0); }
		public TypeExprContext typeExpr() {
			return getRuleContext(TypeExprContext.class,0);
		}
		public TerminalNode RPAREN() { return getToken(JulayParser.RPAREN, 0); }
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
		int _la;
		try {
			setState(180);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ID:
				enterOuterAlt(_localctx, 1);
				{
				setState(172);
				match(ID);
				setState(174);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==LT) {
					{
					setState(173);
					typeArgs();
					}
				}

				}
				break;
			case LPAREN:
				enterOuterAlt(_localctx, 2);
				{
				setState(176);
				match(LPAREN);
				setState(177);
				typeExpr();
				setState(178);
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
		enterRule(_localctx, 12, RULE_typeArgs);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(182);
			match(LT);
			setState(183);
			typeExpr();
			setState(188);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(184);
				match(COMMA);
				setState(185);
				typeExpr();
				}
				}
				setState(190);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(191);
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
		enterRule(_localctx, 14, RULE_typeParams);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(193);
			match(LT);
			setState(194);
			match(ID);
			setState(199);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(195);
				match(COMMA);
				setState(196);
				match(ID);
				}
				}
				setState(201);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(202);
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
		enterRule(_localctx, 16, RULE_fun_decl);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(204);
			match(FUN);
			setState(205);
			match(ID);
			setState(207);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LT) {
				{
				setState(206);
				typeParams();
				}
			}

			setState(209);
			args();
			setState(210);
			match(COLON);
			setState(211);
			typeExpr();
			setState(212);
			match(EQ);
			setState(213);
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
	public static class Procfun_declContext extends ParserRuleContext {
		public TerminalNode PROCFUN() { return getToken(JulayParser.PROCFUN, 0); }
		public TerminalNode ID() { return getToken(JulayParser.ID, 0); }
		public ArgsContext args() {
			return getRuleContext(ArgsContext.class,0);
		}
		public TerminalNode COLON() { return getToken(JulayParser.COLON, 0); }
		public TypeExprContext typeExpr() {
			return getRuleContext(TypeExprContext.class,0);
		}
		public TerminalNode LCURLY() { return getToken(JulayParser.LCURLY, 0); }
		public TerminalNode RCURLY() { return getToken(JulayParser.RCURLY, 0); }
		public List<Procfun_bodyContext> procfun_body() {
			return getRuleContexts(Procfun_bodyContext.class);
		}
		public Procfun_bodyContext procfun_body(int i) {
			return getRuleContext(Procfun_bodyContext.class,i);
		}
		public Procfun_declContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_procfun_decl; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterProcfun_decl(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitProcfun_decl(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitProcfun_decl(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Procfun_declContext procfun_decl() throws RecognitionException {
		Procfun_declContext _localctx = new Procfun_declContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_procfun_decl);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(215);
			match(PROCFUN);
			setState(216);
			match(ID);
			setState(217);
			args();
			setState(218);
			match(COLON);
			setState(219);
			typeExpr();
			setState(220);
			match(LCURLY);
			setState(224);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 35888059530608640L) != 0)) {
				{
				{
				setState(221);
				procfun_body();
				}
				}
				setState(226);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(227);
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
	public static class Procfun_bodyContext extends ParserRuleContext {
		public VarContext var() {
			return getRuleContext(VarContext.class,0);
		}
		public ConstructorContext constructor() {
			return getRuleContext(ConstructorContext.class,0);
		}
		public TransitionContext transition() {
			return getRuleContext(TransitionContext.class,0);
		}
		public Procfun_bodyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_procfun_body; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterProcfun_body(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitProcfun_body(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitProcfun_body(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Procfun_bodyContext procfun_body() throws RecognitionException {
		Procfun_bodyContext _localctx = new Procfun_bodyContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_procfun_body);
		try {
			setState(232);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,17,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(229);
				var();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(230);
				constructor();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(231);
				transition();
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
	public static class ProcContext extends ParserRuleContext {
		public TerminalNode PROC() { return getToken(JulayParser.PROC, 0); }
		public TerminalNode ID() { return getToken(JulayParser.ID, 0); }
		public TerminalNode LCURLY() { return getToken(JulayParser.LCURLY, 0); }
		public TerminalNode RCURLY() { return getToken(JulayParser.RCURLY, 0); }
		public List<Pclass_bodyContext> pclass_body() {
			return getRuleContexts(Pclass_bodyContext.class);
		}
		public Pclass_bodyContext pclass_body(int i) {
			return getRuleContext(Pclass_bodyContext.class,i);
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
			setState(248);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,19,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(234);
				match(PROC);
				setState(235);
				match(ID);
				setState(236);
				match(LCURLY);
				setState(240);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 35888059530608640L) != 0)) {
					{
					{
					setState(237);
					pclass_body();
					}
					}
					setState(242);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(243);
				match(RCURLY);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(244);
				match(PROC);
				setState(245);
				match(ID);
				setState(246);
				match(ASGN_EQ);
				setState(247);
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
	public static class ObjContext extends ParserRuleContext {
		public TerminalNode OBJ() { return getToken(JulayParser.OBJ, 0); }
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
		public ObjContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_obj; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterObj(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitObj(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitObj(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ObjContext obj() throws RecognitionException {
		ObjContext _localctx = new ObjContext(_ctx, getState());
		enterRule(_localctx, 24, RULE_obj);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(250);
			match(OBJ);
			setState(251);
			match(ID);
			setState(253);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LT) {
				{
				setState(252);
				typeParams();
				}
			}

			setState(255);
			match(LCURLY);
			setState(259);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==ID) {
				{
				{
				setState(256);
				field();
				}
				}
				setState(261);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(262);
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
	public static class Sort_declContext extends ParserRuleContext {
		public TerminalNode SORT() { return getToken(JulayParser.SORT, 0); }
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
		public Sort_declContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_sort_decl; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterSort_decl(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitSort_decl(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitSort_decl(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Sort_declContext sort_decl() throws RecognitionException {
		Sort_declContext _localctx = new Sort_declContext(_ctx, getState());
		enterRule(_localctx, 26, RULE_sort_decl);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(264);
			match(SORT);
			setState(265);
			match(ID);
			setState(266);
			match(ASGN_EQ);
			setState(267);
			match(LCURLY);
			setState(268);
			literal();
			setState(273);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(269);
				match(COMMA);
				setState(270);
				literal();
				}
				}
				setState(275);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(276);
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
		enterRule(_localctx, 28, RULE_compile_decl);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(278);
			match(COMPILE);
			setState(279);
			match(ID);
			setState(284);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(280);
				match(COMMA);
				setState(281);
				match(ID);
				}
				}
				setState(286);
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
		public TerminalNode ID() { return getToken(JulayParser.ID, 0); }
		public TerminalNode ASGN_EQ() { return getToken(JulayParser.ASGN_EQ, 0); }
		public Ag_specContext ag_spec() {
			return getRuleContext(Ag_specContext.class,0);
		}
		public System_exprContext system_expr() {
			return getRuleContext(System_exprContext.class,0);
		}
		public TerminalNode MODELS() { return getToken(JulayParser.MODELS, 0); }
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
		enterRule(_localctx, 30, RULE_spec);
		try {
			setState(302);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,24,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(287);
				match(SPEC);
				setState(288);
				match(ID);
				setState(289);
				match(ASGN_EQ);
				setState(290);
				ag_spec();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(291);
				match(SPEC);
				setState(292);
				match(ID);
				setState(293);
				match(ASGN_EQ);
				setState(294);
				system_expr(0);
				setState(295);
				match(MODELS);
				setState(296);
				expr(0);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(298);
				match(SPEC);
				setState(299);
				match(ID);
				setState(300);
				match(ASGN_EQ);
				setState(301);
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
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
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
		enterRule(_localctx, 32, RULE_ag_spec);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(304);
			match(LT);
			setState(305);
			assume_expr();
			setState(306);
			match(GT);
			setState(307);
			system_expr(0);
			setState(308);
			match(LT);
			setState(309);
			expr(0);
			setState(310);
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
		enterRule(_localctx, 34, RULE_assume_expr);
		try {
			setState(314);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case TRUE:
				enterOuterAlt(_localctx, 1);
				{
				setState(312);
				match(TRUE);
				}
				break;
			case LPAREN:
			case INTERNAL:
			case PROVIDER:
			case CLIENT:
			case SESSION:
			case ID:
				enterOuterAlt(_localctx, 2);
				{
				setState(313);
				system_expr(0);
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
	public static class System_exprContext extends ParserRuleContext {
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
		int _startState = 36;
		enterRecursionRule(_localctx, 36, RULE_system_expr, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			{
			setState(317);
			system_atom();
			}
			_ctx.stop = _input.LT(-1);
			setState(324);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,26,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					{
					_localctx = new System_exprContext(_parentctx, _parentState);
					pushNewRecursionContext(_localctx, _startState, RULE_system_expr);
					setState(319);
					if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
					setState(320);
					match(PARALLEL);
					setState(321);
					system_expr(3);
					}
					} 
				}
				setState(326);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,26,_ctx);
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
		enterRule(_localctx, 38, RULE_system_atom);
		try {
			setState(335);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,27,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(327);
				system_primary();
				setState(328);
				match(LBRACK);
				setState(329);
				match(ID);
				setState(330);
				match(COLON);
				setState(331);
				typeExpr();
				setState(332);
				match(RBRACK);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(334);
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
		enterRule(_localctx, 40, RULE_system_primary);
		try {
			setState(342);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case INTERNAL:
			case PROVIDER:
			case CLIENT:
			case SESSION:
			case ID:
				enterOuterAlt(_localctx, 1);
				{
				setState(337);
				system_leaf();
				}
				break;
			case LPAREN:
				enterOuterAlt(_localctx, 2);
				{
				setState(338);
				match(LPAREN);
				setState(339);
				system_expr(0);
				setState(340);
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
		enterRule(_localctx, 42, RULE_system_leaf);
		try {
			setState(346);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,29,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(344);
				qualified_name();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(345);
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
		enterRule(_localctx, 44, RULE_invariant_decl);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(348);
			match(INVARIANT);
			setState(349);
			match(ID);
			setState(350);
			match(ASGN_EQ);
			setState(351);
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
	public static class Pclass_bodyContext extends ParserRuleContext {
		public VarContext var() {
			return getRuleContext(VarContext.class,0);
		}
		public ConstructorContext constructor() {
			return getRuleContext(ConstructorContext.class,0);
		}
		public TransitionContext transition() {
			return getRuleContext(TransitionContext.class,0);
		}
		public Pclass_bodyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_pclass_body; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterPclass_body(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitPclass_body(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitPclass_body(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Pclass_bodyContext pclass_body() throws RecognitionException {
		Pclass_bodyContext _localctx = new Pclass_bodyContext(_ctx, getState());
		enterRule(_localctx, 46, RULE_pclass_body);
		try {
			setState(356);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,30,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(353);
				var();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(354);
				constructor();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(355);
				transition();
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
		enterRule(_localctx, 48, RULE_field);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(358);
			match(ID);
			setState(359);
			match(COLON);
			setState(360);
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
		enterRule(_localctx, 50, RULE_var);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(362);
			_la = _input.LA(1);
			if ( !(_la==VAR || _la==CONST) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			setState(363);
			match(ID);
			setState(364);
			match(COLON);
			setState(365);
			typeExpr();
			setState(368);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ASGN_EQ) {
				{
				setState(366);
				match(ASGN_EQ);
				setState(367);
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
		public TerminalNode ID() { return getToken(JulayParser.ID, 0); }
		public ArgsContext args() {
			return getRuleContext(ArgsContext.class,0);
		}
		public TerminalNode LCURLY() { return getToken(JulayParser.LCURLY, 0); }
		public TerminalNode RCURLY() { return getToken(JulayParser.RCURLY, 0); }
		public TerminalNode SESSION() { return getToken(JulayParser.SESSION, 0); }
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
		enterRule(_localctx, 52, RULE_constructor);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(371);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==SESSION) {
				{
				setState(370);
				match(SESSION);
				}
			}

			setState(373);
			match(CONSTRUCTOR);
			setState(374);
			match(ID);
			setState(375);
			args();
			setState(376);
			match(LCURLY);
			setState(380);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 1080863910568919040L) != 0)) {
				{
				{
				setState(377);
				constructor_body();
				}
				}
				setState(382);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(383);
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
		public ArgsContext args() {
			return getRuleContext(ArgsContext.class,0);
		}
		public TerminalNode LCURLY() { return getToken(JulayParser.LCURLY, 0); }
		public TerminalNode RCURLY() { return getToken(JulayParser.RCURLY, 0); }
		public List<Action_bodyContext> action_body() {
			return getRuleContexts(Action_bodyContext.class);
		}
		public Action_bodyContext action_body(int i) {
			return getRuleContext(Action_bodyContext.class,i);
		}
		public TerminalNode INTERNAL() { return getToken(JulayParser.INTERNAL, 0); }
		public TerminalNode PROVIDER() { return getToken(JulayParser.PROVIDER, 0); }
		public TerminalNode CLIENT() { return getToken(JulayParser.CLIENT, 0); }
		public TerminalNode SESSION() { return getToken(JulayParser.SESSION, 0); }
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
		enterRule(_localctx, 54, RULE_transition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(386);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 33776997205278720L) != 0)) {
				{
				setState(385);
				_la = _input.LA(1);
				if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 33776997205278720L) != 0)) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
			}

			setState(388);
			match(TRANSITION);
			setState(389);
			match(ID);
			setState(390);
			args();
			setState(391);
			match(LCURLY);
			setState(395);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 5728578726015270912L) != 0)) {
				{
				{
				setState(392);
				action_body();
				}
				}
				setState(397);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(398);
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
		enterRule(_localctx, 56, RULE_args);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(400);
			match(LPAREN);
			setState(402);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ID) {
				{
				setState(401);
				arg();
				}
			}

			setState(408);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(404);
				match(COMMA);
				setState(405);
				arg();
				}
				}
				setState(410);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(411);
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
		enterRule(_localctx, 58, RULE_arg);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(413);
			match(ID);
			setState(414);
			match(COLON);
			setState(415);
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
		enterRule(_localctx, 60, RULE_constructor_body);
		try {
			setState(421);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case BEFORE:
				enterOuterAlt(_localctx, 1);
				{
				setState(417);
				before();
				}
				break;
			case TRANSIT:
				enterOuterAlt(_localctx, 2);
				{
				setState(418);
				transit();
				}
				break;
			case ERROR:
				enterOuterAlt(_localctx, 3);
				{
				setState(419);
				error();
				}
				break;
			case AFTER:
				enterOuterAlt(_localctx, 4);
				{
				setState(420);
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
		public Return_clauseContext return_clause() {
			return getRuleContext(Return_clauseContext.class,0);
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
		enterRule(_localctx, 62, RULE_action_body);
		try {
			setState(429);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case GUARD:
				enterOuterAlt(_localctx, 1);
				{
				setState(423);
				guard();
				}
				break;
			case BEFORE:
				enterOuterAlt(_localctx, 2);
				{
				setState(424);
				before();
				}
				break;
			case TRANSIT:
				enterOuterAlt(_localctx, 3);
				{
				setState(425);
				transit();
				}
				break;
			case ERROR:
				enterOuterAlt(_localctx, 4);
				{
				setState(426);
				error();
				}
				break;
			case AFTER:
				enterOuterAlt(_localctx, 5);
				{
				setState(427);
				after();
				}
				break;
			case RETURN:
				enterOuterAlt(_localctx, 6);
				{
				setState(428);
				return_clause();
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
	public static class Return_clauseContext extends ParserRuleContext {
		public TerminalNode RETURN() { return getToken(JulayParser.RETURN, 0); }
		public TerminalNode COLON() { return getToken(JulayParser.COLON, 0); }
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public Return_clauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_return_clause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterReturn_clause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitReturn_clause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitReturn_clause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Return_clauseContext return_clause() throws RecognitionException {
		Return_clauseContext _localctx = new Return_clauseContext(_ctx, getState());
		enterRule(_localctx, 64, RULE_return_clause);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(431);
			match(RETURN);
			setState(432);
			match(COLON);
			setState(433);
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
		enterRule(_localctx, 66, RULE_guard);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(435);
			match(GUARD);
			setState(436);
			match(COLON);
			setState(437);
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
		enterRule(_localctx, 68, RULE_transit);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(439);
			match(TRANSIT);
			setState(440);
			match(COLON);
			setState(444);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==LET || _la==ID) {
				{
				{
				setState(441);
				var_transit();
				}
				}
				setState(446);
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
		enterRule(_localctx, 70, RULE_error);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(447);
			match(ERROR);
			setState(448);
			match(COLON);
			setState(450); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(449);
				error_arm();
				}
				}
				setState(452); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( ((((_la - 4)) & ~0x3f) == 0 && ((1L << (_la - 4)) & 8646917883365956501L) != 0) );
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
		enterRule(_localctx, 72, RULE_error_arm);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(454);
			expr(0);
			setState(455);
			match(ARROW);
			setState(456);
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
		enterRule(_localctx, 74, RULE_var_transit);
		try {
			setState(476);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,42,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(458);
				field_access();
				setState(459);
				match(ASGN_EQ);
				setState(460);
				expr(0);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(462);
				match(ID);
				setState(463);
				match(LBRACK);
				setState(464);
				expr(0);
				setState(465);
				match(RBRACK);
				setState(466);
				match(ASGN_EQ);
				setState(467);
				expr(0);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(469);
				match(LET);
				setState(470);
				match(ID);
				setState(471);
				match(COLON);
				setState(472);
				typeExpr();
				setState(473);
				match(ASGN_EQ);
				setState(474);
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
		enterRule(_localctx, 76, RULE_before);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(478);
			match(BEFORE);
			setState(479);
			match(COLON);
			setState(481); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(480);
				call_stmt();
				}
				}
				setState(483); 
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
		enterRule(_localctx, 78, RULE_after);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(485);
			match(AFTER);
			setState(486);
			match(COLON);
			setState(488); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(487);
				call_stmt();
				}
				}
				setState(490); 
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
		enterRule(_localctx, 80, RULE_call_stmt);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(492);
			match(ID);
			setState(494);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LT) {
				{
				setState(493);
				typeArgs();
				}
			}

			setState(496);
			match(LPAREN);
			setState(505);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (((((_la - 4)) & ~0x3f) == 0 && ((1L << (_la - 4)) & 8646917883365956501L) != 0)) {
				{
				setState(497);
				expr(0);
				setState(502);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(498);
					match(COMMA);
					setState(499);
					expr(0);
					}
					}
					setState(504);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
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
		public Bracket_literalContext bracket_literal() {
			return getRuleContext(Bracket_literalContext.class,0);
		}
		public Set_literalContext set_literal() {
			return getRuleContext(Set_literalContext.class,0);
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
		public Oclass_literalContext oclass_literal() {
			return getRuleContext(Oclass_literalContext.class,0);
		}
		public Fun_callContext fun_call() {
			return getRuleContext(Fun_callContext.class,0);
		}
		public TerminalNode NOT() { return getToken(JulayParser.NOT, 0); }
		public TerminalNode AND() { return getToken(JulayParser.AND, 0); }
		public TerminalNode OR() { return getToken(JulayParser.OR, 0); }
		public TerminalNode IF() { return getToken(JulayParser.IF, 0); }
		public List<TerminalNode> LCURLY() { return getTokens(JulayParser.LCURLY); }
		public TerminalNode LCURLY(int i) {
			return getToken(JulayParser.LCURLY, i);
		}
		public List<TerminalNode> RCURLY() { return getTokens(JulayParser.RCURLY); }
		public TerminalNode RCURLY(int i) {
			return getToken(JulayParser.RCURLY, i);
		}
		public TerminalNode ELSE() { return getToken(JulayParser.ELSE, 0); }
		public TerminalNode LET() { return getToken(JulayParser.LET, 0); }
		public TerminalNode ID() { return getToken(JulayParser.ID, 0); }
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
		public TerminalNode ALL() { return getToken(JulayParser.ALL, 0); }
		public TerminalNode COMMA() { return getToken(JulayParser.COMMA, 0); }
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
		int _startState = 82;
		enterRecursionRule(_localctx, 82, RULE_expr, _p);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(587);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,50,_ctx) ) {
			case 1:
				{
				setState(510);
				literal();
				}
				break;
			case 2:
				{
				setState(511);
				match(LPAREN);
				setState(512);
				expr(0);
				setState(513);
				match(RPAREN);
				}
				break;
			case 3:
				{
				setState(515);
				bracket_literal();
				}
				break;
			case 4:
				{
				setState(516);
				set_literal();
				}
				break;
			case 5:
				{
				setState(517);
				method_prop_expr();
				}
				break;
			case 6:
				{
				setState(518);
				index_expr(0);
				}
				break;
			case 7:
				{
				setState(519);
				field_access();
				}
				break;
			case 8:
				{
				setState(520);
				oclass_literal();
				}
				break;
			case 9:
				{
				setState(521);
				fun_call();
				}
				break;
			case 10:
				{
				setState(522);
				match(NOT);
				setState(523);
				expr(25);
				}
				break;
			case 11:
				{
				setState(524);
				match(AND);
				setState(525);
				expr(24);
				}
				break;
			case 12:
				{
				setState(526);
				match(OR);
				setState(527);
				expr(23);
				}
				break;
			case 13:
				{
				setState(528);
				match(IF);
				setState(529);
				match(LPAREN);
				setState(530);
				expr(0);
				setState(531);
				match(RPAREN);
				setState(532);
				match(LCURLY);
				setState(533);
				expr(0);
				setState(534);
				match(RCURLY);
				setState(535);
				match(ELSE);
				setState(536);
				match(LCURLY);
				setState(537);
				expr(0);
				setState(538);
				match(RCURLY);
				}
				break;
			case 14:
				{
				setState(540);
				match(LET);
				setState(541);
				match(LPAREN);
				setState(542);
				match(ID);
				setState(543);
				match(COLON);
				setState(544);
				typeExpr();
				setState(545);
				match(ASGN_EQ);
				setState(546);
				expr(0);
				setState(547);
				match(RPAREN);
				setState(548);
				match(LCURLY);
				setState(549);
				expr(0);
				setState(550);
				match(RCURLY);
				}
				break;
			case 15:
				{
				setState(552);
				match(WHEN);
				setState(553);
				match(LPAREN);
				setState(554);
				expr(0);
				setState(555);
				match(RPAREN);
				setState(556);
				match(LCURLY);
				setState(558); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(557);
					when_subject_arm();
					}
					}
					setState(560); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( ((((_la - 4)) & ~0x3f) == 0 && ((1L << (_la - 4)) & 8646911284819788161L) != 0) );
				setState(562);
				match(RCURLY);
				}
				break;
			case 16:
				{
				setState(564);
				match(WHEN);
				setState(565);
				match(LCURLY);
				setState(567); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(566);
					when_guard_arm();
					}
					}
					setState(569); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( ((((_la - 4)) & ~0x3f) == 0 && ((1L << (_la - 4)) & 8646917883634391957L) != 0) );
				setState(571);
				match(RCURLY);
				}
				break;
			case 17:
				{
				setState(573);
				match(ALL);
				setState(574);
				match(ID);
				setState(575);
				match(COLON);
				setState(576);
				typeExpr();
				setState(577);
				match(COMMA);
				setState(578);
				expr(2);
				}
				break;
			case 18:
				{
				setState(580);
				match(EXISTS);
				setState(581);
				match(ID);
				setState(582);
				match(COLON);
				setState(583);
				typeExpr();
				setState(584);
				match(COMMA);
				setState(585);
				expr(1);
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(639);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,52,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(637);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,51,_ctx) ) {
					case 1:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(589);
						if (!(precpred(_ctx, 22))) throw new FailedPredicateException(this, "precpred(_ctx, 22)");
						setState(590);
						match(TIMES);
						setState(591);
						expr(23);
						}
						break;
					case 2:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(592);
						if (!(precpred(_ctx, 21))) throw new FailedPredicateException(this, "precpred(_ctx, 21)");
						setState(593);
						match(DIV);
						setState(594);
						expr(22);
						}
						break;
					case 3:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(595);
						if (!(precpred(_ctx, 20))) throw new FailedPredicateException(this, "precpred(_ctx, 20)");
						setState(596);
						match(MOD);
						setState(597);
						expr(21);
						}
						break;
					case 4:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(598);
						if (!(precpred(_ctx, 19))) throw new FailedPredicateException(this, "precpred(_ctx, 19)");
						setState(599);
						match(PLUS);
						setState(600);
						expr(20);
						}
						break;
					case 5:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(601);
						if (!(precpred(_ctx, 18))) throw new FailedPredicateException(this, "precpred(_ctx, 18)");
						setState(602);
						match(MINUS);
						setState(603);
						expr(19);
						}
						break;
					case 6:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(604);
						if (!(precpred(_ctx, 17))) throw new FailedPredicateException(this, "precpred(_ctx, 17)");
						setState(605);
						match(LT);
						setState(606);
						expr(18);
						}
						break;
					case 7:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(607);
						if (!(precpred(_ctx, 16))) throw new FailedPredicateException(this, "precpred(_ctx, 16)");
						setState(608);
						match(LTE);
						setState(609);
						expr(17);
						}
						break;
					case 8:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(610);
						if (!(precpred(_ctx, 15))) throw new FailedPredicateException(this, "precpred(_ctx, 15)");
						setState(611);
						match(GT);
						setState(612);
						expr(16);
						}
						break;
					case 9:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(613);
						if (!(precpred(_ctx, 14))) throw new FailedPredicateException(this, "precpred(_ctx, 14)");
						setState(614);
						match(GTE);
						setState(615);
						expr(15);
						}
						break;
					case 10:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(616);
						if (!(precpred(_ctx, 13))) throw new FailedPredicateException(this, "precpred(_ctx, 13)");
						setState(617);
						match(IN);
						setState(618);
						expr(14);
						}
						break;
					case 11:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(619);
						if (!(precpred(_ctx, 12))) throw new FailedPredicateException(this, "precpred(_ctx, 12)");
						setState(620);
						match(EQ);
						setState(621);
						expr(13);
						}
						break;
					case 12:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(622);
						if (!(precpred(_ctx, 11))) throw new FailedPredicateException(this, "precpred(_ctx, 11)");
						setState(623);
						match(NEQ);
						setState(624);
						expr(12);
						}
						break;
					case 13:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(625);
						if (!(precpred(_ctx, 10))) throw new FailedPredicateException(this, "precpred(_ctx, 10)");
						setState(626);
						match(AND);
						setState(627);
						expr(11);
						}
						break;
					case 14:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(628);
						if (!(precpred(_ctx, 9))) throw new FailedPredicateException(this, "precpred(_ctx, 9)");
						setState(629);
						match(OR);
						setState(630);
						expr(10);
						}
						break;
					case 15:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(631);
						if (!(precpred(_ctx, 8))) throw new FailedPredicateException(this, "precpred(_ctx, 8)");
						setState(632);
						match(IMPLIES);
						setState(633);
						expr(8);
						}
						break;
					case 16:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(634);
						if (!(precpred(_ctx, 7))) throw new FailedPredicateException(this, "precpred(_ctx, 7)");
						setState(635);
						match(IFF);
						setState(636);
						expr(8);
						}
						break;
					}
					} 
				}
				setState(641);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,52,_ctx);
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
		enterRule(_localctx, 84, RULE_when_subject_arm);
		try {
			setState(649);
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
				setState(642);
				when_pattern();
				setState(643);
				match(ARROW);
				setState(644);
				expr(0);
				}
				break;
			case ELSE:
				enterOuterAlt(_localctx, 2);
				{
				setState(646);
				match(ELSE);
				setState(647);
				match(ARROW);
				setState(648);
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
		enterRule(_localctx, 86, RULE_when_guard_arm);
		try {
			setState(658);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case LPAREN:
			case LBRACK:
			case LCURLY:
			case TRUE:
			case FALSE:
			case AND:
			case OR:
			case NOT:
			case IF:
			case LET:
			case WHEN:
			case ALL:
			case EXISTS:
			case REAL:
			case INT:
			case ID:
			case STRING:
				enterOuterAlt(_localctx, 1);
				{
				setState(651);
				expr(0);
				setState(652);
				match(ARROW);
				setState(653);
				expr(0);
				}
				break;
			case ELSE:
				enterOuterAlt(_localctx, 2);
				{
				setState(655);
				match(ELSE);
				setState(656);
				match(ARROW);
				setState(657);
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
		public Oclass_literalContext oclass_literal() {
			return getRuleContext(Oclass_literalContext.class,0);
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
		enterRule(_localctx, 88, RULE_when_pattern);
		try {
			setState(662);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case TRUE:
			case FALSE:
			case REAL:
			case INT:
			case STRING:
				enterOuterAlt(_localctx, 1);
				{
				setState(660);
				literal();
				}
				break;
			case LPAREN:
			case ID:
				enterOuterAlt(_localctx, 2);
				{
				setState(661);
				oclass_literal();
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
		int _startState = 90;
		enterRecursionRule(_localctx, 90, RULE_proc_expr, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(671);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,56,_ctx) ) {
			case 1:
				{
				setState(665);
				qualified_name();
				}
				break;
			case 2:
				{
				setState(666);
				match(ID);
				}
				break;
			case 3:
				{
				setState(667);
				match(LPAREN);
				setState(668);
				proc_expr(0);
				setState(669);
				match(RPAREN);
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(678);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,57,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					{
					_localctx = new Proc_exprContext(_parentctx, _parentState);
					pushNewRecursionContext(_localctx, _startState, RULE_proc_expr);
					setState(673);
					if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
					setState(674);
					match(PARALLEL);
					setState(675);
					proc_expr(2);
					}
					} 
				}
				setState(680);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,57,_ctx);
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
		enterRule(_localctx, 92, RULE_literal);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(681);
			_la = _input.LA(1);
			if ( !(((((_la - 11)) & ~0x3f) == 0 && ((1L << (_la - 11)) & 49539595901075459L) != 0)) ) {
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
	public static class Bracket_literalContext extends ParserRuleContext {
		public TerminalNode LBRACK() { return getToken(JulayParser.LBRACK, 0); }
		public TerminalNode RBRACK() { return getToken(JulayParser.RBRACK, 0); }
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
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public Bracket_literalContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_bracket_literal; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterBracket_literal(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitBracket_literal(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitBracket_literal(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Bracket_literalContext bracket_literal() throws RecognitionException {
		Bracket_literalContext _localctx = new Bracket_literalContext(_ctx, getState());
		enterRule(_localctx, 94, RULE_bracket_literal);
		int _la;
		try {
			setState(707);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,60,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(683);
				match(LBRACK);
				setState(684);
				match(RBRACK);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(685);
				match(LBRACK);
				setState(686);
				map_entry();
				setState(691);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(687);
					match(COMMA);
					setState(688);
					map_entry();
					}
					}
					setState(693);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(694);
				match(RBRACK);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(696);
				match(LBRACK);
				setState(697);
				expr(0);
				setState(702);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(698);
					match(COMMA);
					setState(699);
					expr(0);
					}
					}
					setState(704);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(705);
				match(RBRACK);
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
	public static class Map_entryContext extends ParserRuleContext {
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public TerminalNode ARROW() { return getToken(JulayParser.ARROW, 0); }
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
		enterRule(_localctx, 96, RULE_map_entry);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(709);
			expr(0);
			setState(710);
			match(ARROW);
			setState(711);
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
	public static class Set_literalContext extends ParserRuleContext {
		public TerminalNode LCURLY() { return getToken(JulayParser.LCURLY, 0); }
		public TerminalNode RCURLY() { return getToken(JulayParser.RCURLY, 0); }
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
		enterRule(_localctx, 98, RULE_set_literal);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(713);
			match(LCURLY);
			setState(722);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (((((_la - 4)) & ~0x3f) == 0 && ((1L << (_la - 4)) & 8646917883365956501L) != 0)) {
				{
				setState(714);
				expr(0);
				setState(719);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(715);
					match(COMMA);
					setState(716);
					expr(0);
					}
					}
					setState(721);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
			}

			setState(724);
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
	public static class Index_exprContext extends ParserRuleContext {
		public TerminalNode LBRACK() { return getToken(JulayParser.LBRACK, 0); }
		public Index_or_sliceContext index_or_slice() {
			return getRuleContext(Index_or_sliceContext.class,0);
		}
		public TerminalNode RBRACK() { return getToken(JulayParser.RBRACK, 0); }
		public Fun_callContext fun_call() {
			return getRuleContext(Fun_callContext.class,0);
		}
		public Field_accessContext field_access() {
			return getRuleContext(Field_accessContext.class,0);
		}
		public Bracket_literalContext bracket_literal() {
			return getRuleContext(Bracket_literalContext.class,0);
		}
		public Set_literalContext set_literal() {
			return getRuleContext(Set_literalContext.class,0);
		}
		public List<TerminalNode> LPAREN() { return getTokens(JulayParser.LPAREN); }
		public TerminalNode LPAREN(int i) {
			return getToken(JulayParser.LPAREN, i);
		}
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
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
		int _startState = 100;
		enterRecursionRule(_localctx, 100, RULE_index_expr, _p);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(778);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,68,_ctx) ) {
			case 1:
				{
				setState(735);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,63,_ctx) ) {
				case 1:
					{
					setState(727);
					fun_call();
					}
					break;
				case 2:
					{
					setState(728);
					field_access();
					}
					break;
				case 3:
					{
					setState(729);
					bracket_literal();
					}
					break;
				case 4:
					{
					setState(730);
					set_literal();
					}
					break;
				case 5:
					{
					setState(731);
					match(LPAREN);
					setState(732);
					expr(0);
					setState(733);
					match(RPAREN);
					}
					break;
				}
				setState(737);
				match(LBRACK);
				setState(738);
				index_or_slice();
				setState(739);
				match(RBRACK);
				}
				break;
			case 2:
				{
				setState(749);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,64,_ctx) ) {
				case 1:
					{
					setState(741);
					fun_call();
					}
					break;
				case 2:
					{
					setState(742);
					field_access();
					}
					break;
				case 3:
					{
					setState(743);
					bracket_literal();
					}
					break;
				case 4:
					{
					setState(744);
					set_literal();
					}
					break;
				case 5:
					{
					setState(745);
					match(LPAREN);
					setState(746);
					expr(0);
					setState(747);
					match(RPAREN);
					}
					break;
				}
				setState(751);
				match(DOT);
				setState(752);
				match(ID);
				setState(753);
				match(LPAREN);
				setState(762);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (((((_la - 4)) & ~0x3f) == 0 && ((1L << (_la - 4)) & 8646917883365956501L) != 0)) {
					{
					setState(754);
					call_arg();
					setState(759);
					_errHandler.sync(this);
					_la = _input.LA(1);
					while (_la==COMMA) {
						{
						{
						setState(755);
						match(COMMA);
						setState(756);
						call_arg();
						}
						}
						setState(761);
						_errHandler.sync(this);
						_la = _input.LA(1);
					}
					}
				}

				setState(764);
				match(RPAREN);
				}
				break;
			case 3:
				{
				setState(773);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case ID:
					{
					setState(766);
					fun_call();
					}
					break;
				case LBRACK:
					{
					setState(767);
					bracket_literal();
					}
					break;
				case LCURLY:
					{
					setState(768);
					set_literal();
					}
					break;
				case LPAREN:
					{
					setState(769);
					match(LPAREN);
					setState(770);
					expr(0);
					setState(771);
					match(RPAREN);
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(775);
				match(DOT);
				setState(776);
				match(ID);
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(805);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,72,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(803);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,71,_ctx) ) {
					case 1:
						{
						_localctx = new Index_exprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_index_expr);
						setState(780);
						if (!(precpred(_ctx, 6))) throw new FailedPredicateException(this, "precpred(_ctx, 6)");
						setState(781);
						match(LBRACK);
						setState(782);
						index_or_slice();
						setState(783);
						match(RBRACK);
						}
						break;
					case 2:
						{
						_localctx = new Index_exprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_index_expr);
						setState(785);
						if (!(precpred(_ctx, 5))) throw new FailedPredicateException(this, "precpred(_ctx, 5)");
						setState(786);
						match(DOT);
						setState(787);
						match(ID);
						setState(788);
						match(LPAREN);
						setState(797);
						_errHandler.sync(this);
						_la = _input.LA(1);
						if (((((_la - 4)) & ~0x3f) == 0 && ((1L << (_la - 4)) & 8646917883365956501L) != 0)) {
							{
							setState(789);
							call_arg();
							setState(794);
							_errHandler.sync(this);
							_la = _input.LA(1);
							while (_la==COMMA) {
								{
								{
								setState(790);
								match(COMMA);
								setState(791);
								call_arg();
								}
								}
								setState(796);
								_errHandler.sync(this);
								_la = _input.LA(1);
							}
							}
						}

						setState(799);
						match(RPAREN);
						}
						break;
					case 3:
						{
						_localctx = new Index_exprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_index_expr);
						setState(800);
						if (!(precpred(_ctx, 4))) throw new FailedPredicateException(this, "precpred(_ctx, 4)");
						setState(801);
						match(DOT);
						setState(802);
						match(ID);
						}
						break;
					}
					} 
				}
				setState(807);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,72,_ctx);
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
		enterRule(_localctx, 102, RULE_method_prop_expr);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(808);
			method_call();
			setState(813);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,73,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(809);
					match(DOT);
					setState(810);
					match(ID);
					}
					} 
				}
				setState(815);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,73,_ctx);
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
	public static class Index_or_sliceContext extends ParserRuleContext {
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public TerminalNode COLON() { return getToken(JulayParser.COLON, 0); }
		public Index_or_sliceContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_index_or_slice; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterIndex_or_slice(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitIndex_or_slice(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitIndex_or_slice(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Index_or_sliceContext index_or_slice() throws RecognitionException {
		Index_or_sliceContext _localctx = new Index_or_sliceContext(_ctx, getState());
		enterRule(_localctx, 104, RULE_index_or_slice);
		try {
			setState(821);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,74,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(816);
				expr(0);
				setState(817);
				match(COLON);
				setState(818);
				expr(0);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(820);
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
	public static class Method_callContext extends ParserRuleContext {
		public List<TerminalNode> ID() { return getTokens(JulayParser.ID); }
		public TerminalNode ID(int i) {
			return getToken(JulayParser.ID, i);
		}
		public TerminalNode LPAREN() { return getToken(JulayParser.LPAREN, 0); }
		public TerminalNode RPAREN() { return getToken(JulayParser.RPAREN, 0); }
		public List<TerminalNode> DOT() { return getTokens(JulayParser.DOT); }
		public TerminalNode DOT(int i) {
			return getToken(JulayParser.DOT, i);
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
		enterRule(_localctx, 106, RULE_method_call);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(823);
			match(ID);
			setState(826); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(824);
				match(DOT);
				setState(825);
				match(ID);
				}
				}
				setState(828); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==DOT );
			setState(830);
			match(LPAREN);
			setState(839);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (((((_la - 4)) & ~0x3f) == 0 && ((1L << (_la - 4)) & 8646917883365956501L) != 0)) {
				{
				setState(831);
				call_arg();
				setState(836);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(832);
					match(COMMA);
					setState(833);
					call_arg();
					}
					}
					setState(838);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
			}

			setState(841);
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
		enterRule(_localctx, 108, RULE_fun_call);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(843);
			match(ID);
			setState(845);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LT) {
				{
				setState(844);
				typeArgs();
				}
			}

			setState(847);
			match(LPAREN);
			setState(856);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (((((_la - 4)) & ~0x3f) == 0 && ((1L << (_la - 4)) & 8646917883365956501L) != 0)) {
				{
				setState(848);
				call_arg();
				setState(853);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(849);
					match(COMMA);
					setState(850);
					call_arg();
					}
					}
					setState(855);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
			}

			setState(858);
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
		enterRule(_localctx, 110, RULE_call_arg);
		try {
			setState(862);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,81,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(860);
				lambda_expr();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(861);
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
		enterRule(_localctx, 112, RULE_lambda_expr);
		try {
			setState(874);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ID:
				enterOuterAlt(_localctx, 1);
				{
				setState(864);
				match(ID);
				setState(865);
				match(ARROW);
				setState(866);
				expr(0);
				}
				break;
			case LPAREN:
				enterOuterAlt(_localctx, 2);
				{
				setState(867);
				match(LPAREN);
				setState(868);
				match(ID);
				setState(869);
				match(COMMA);
				setState(870);
				match(ID);
				setState(871);
				match(RPAREN);
				setState(872);
				match(ARROW);
				setState(873);
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
	public static class Oclass_literalContext extends ParserRuleContext {
		public TypeExprContext typeExpr() {
			return getRuleContext(TypeExprContext.class,0);
		}
		public TerminalNode LCURLY() { return getToken(JulayParser.LCURLY, 0); }
		public List<Oclass_field_assignContext> oclass_field_assign() {
			return getRuleContexts(Oclass_field_assignContext.class);
		}
		public Oclass_field_assignContext oclass_field_assign(int i) {
			return getRuleContext(Oclass_field_assignContext.class,i);
		}
		public TerminalNode RCURLY() { return getToken(JulayParser.RCURLY, 0); }
		public List<TerminalNode> COMMA() { return getTokens(JulayParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(JulayParser.COMMA, i);
		}
		public Oclass_literalContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_oclass_literal; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterOclass_literal(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitOclass_literal(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitOclass_literal(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Oclass_literalContext oclass_literal() throws RecognitionException {
		Oclass_literalContext _localctx = new Oclass_literalContext(_ctx, getState());
		enterRule(_localctx, 114, RULE_oclass_literal);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(876);
			typeExpr();
			setState(877);
			match(LCURLY);
			setState(878);
			oclass_field_assign();
			setState(883);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(879);
				match(COMMA);
				setState(880);
				oclass_field_assign();
				}
				}
				setState(885);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(886);
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
	public static class Oclass_field_assignContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(JulayParser.ID, 0); }
		public TerminalNode ASGN_EQ() { return getToken(JulayParser.ASGN_EQ, 0); }
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public Oclass_field_assignContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_oclass_field_assign; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterOclass_field_assign(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitOclass_field_assign(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitOclass_field_assign(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Oclass_field_assignContext oclass_field_assign() throws RecognitionException {
		Oclass_field_assignContext _localctx = new Oclass_field_assignContext(_ctx, getState());
		enterRule(_localctx, 116, RULE_oclass_field_assign);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(888);
			match(ID);
			setState(889);
			match(ASGN_EQ);
			setState(890);
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
		public List<TerminalNode> ID() { return getTokens(JulayParser.ID); }
		public TerminalNode ID(int i) {
			return getToken(JulayParser.ID, i);
		}
		public List<TerminalNode> DOT() { return getTokens(JulayParser.DOT); }
		public TerminalNode DOT(int i) {
			return getToken(JulayParser.DOT, i);
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
		enterRule(_localctx, 118, RULE_field_access);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(892);
			match(ID);
			setState(897);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,84,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(893);
					match(DOT);
					setState(894);
					match(ID);
					}
					} 
				}
				setState(899);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,84,_ctx);
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

	public boolean sempred(RuleContext _localctx, int ruleIndex, int predIndex) {
		switch (ruleIndex) {
		case 18:
			return system_expr_sempred((System_exprContext)_localctx, predIndex);
		case 41:
			return expr_sempred((ExprContext)_localctx, predIndex);
		case 45:
			return proc_expr_sempred((Proc_exprContext)_localctx, predIndex);
		case 50:
			return index_expr_sempred((Index_exprContext)_localctx, predIndex);
		}
		return true;
	}
	private boolean system_expr_sempred(System_exprContext _localctx, int predIndex) {
		switch (predIndex) {
		case 0:
			return precpred(_ctx, 2);
		}
		return true;
	}
	private boolean expr_sempred(ExprContext _localctx, int predIndex) {
		switch (predIndex) {
		case 1:
			return precpred(_ctx, 22);
		case 2:
			return precpred(_ctx, 21);
		case 3:
			return precpred(_ctx, 20);
		case 4:
			return precpred(_ctx, 19);
		case 5:
			return precpred(_ctx, 18);
		case 6:
			return precpred(_ctx, 17);
		case 7:
			return precpred(_ctx, 16);
		case 8:
			return precpred(_ctx, 15);
		case 9:
			return precpred(_ctx, 14);
		case 10:
			return precpred(_ctx, 13);
		case 11:
			return precpred(_ctx, 12);
		case 12:
			return precpred(_ctx, 11);
		case 13:
			return precpred(_ctx, 10);
		case 14:
			return precpred(_ctx, 9);
		case 15:
			return precpred(_ctx, 8);
		case 16:
			return precpred(_ctx, 7);
		}
		return true;
	}
	private boolean proc_expr_sempred(Proc_exprContext _localctx, int predIndex) {
		switch (predIndex) {
		case 17:
			return precpred(_ctx, 1);
		}
		return true;
	}
	private boolean index_expr_sempred(Index_exprContext _localctx, int predIndex) {
		switch (predIndex) {
		case 18:
			return precpred(_ctx, 6);
		case 19:
			return precpred(_ctx, 5);
		case 20:
			return precpred(_ctx, 4);
		}
		return true;
	}

	public static final String _serializedATN =
		"\u0004\u0001E\u0385\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001\u0002"+
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
		"7\u00077\u00028\u00078\u00029\u00079\u0002:\u0007:\u0002;\u0007;\u0001"+
		"\u0000\u0001\u0000\u0005\u0000{\b\u0000\n\u0000\f\u0000~\t\u0000\u0001"+
		"\u0000\u0001\u0000\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0002\u0001"+
		"\u0002\u0001\u0002\u0004\u0002\u0088\b\u0002\u000b\u0002\f\u0002\u0089"+
		"\u0001\u0003\u0001\u0003\u0001\u0004\u0003\u0004\u008f\b\u0004\u0001\u0004"+
		"\u0001\u0004\u0003\u0004\u0093\b\u0004\u0001\u0004\u0001\u0004\u0003\u0004"+
		"\u0097\b\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0003\u0004\u009c\b"+
		"\u0004\u0001\u0004\u0001\u0004\u0003\u0004\u00a0\b\u0004\u0001\u0004\u0001"+
		"\u0004\u0003\u0004\u00a4\b\u0004\u0001\u0004\u0001\u0004\u0003\u0004\u00a8"+
		"\b\u0004\u0001\u0004\u0003\u0004\u00ab\b\u0004\u0001\u0005\u0001\u0005"+
		"\u0003\u0005\u00af\b\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005"+
		"\u0003\u0005\u00b5\b\u0005\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006"+
		"\u0005\u0006\u00bb\b\u0006\n\u0006\f\u0006\u00be\t\u0006\u0001\u0006\u0001"+
		"\u0006\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0005\u0007\u00c6"+
		"\b\u0007\n\u0007\f\u0007\u00c9\t\u0007\u0001\u0007\u0001\u0007\u0001\b"+
		"\u0001\b\u0001\b\u0003\b\u00d0\b\b\u0001\b\u0001\b\u0001\b\u0001\b\u0001"+
		"\b\u0001\b\u0001\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001\t\u0005"+
		"\t\u00df\b\t\n\t\f\t\u00e2\t\t\u0001\t\u0001\t\u0001\n\u0001\n\u0001\n"+
		"\u0003\n\u00e9\b\n\u0001\u000b\u0001\u000b\u0001\u000b\u0001\u000b\u0005"+
		"\u000b\u00ef\b\u000b\n\u000b\f\u000b\u00f2\t\u000b\u0001\u000b\u0001\u000b"+
		"\u0001\u000b\u0001\u000b\u0001\u000b\u0003\u000b\u00f9\b\u000b\u0001\f"+
		"\u0001\f\u0001\f\u0003\f\u00fe\b\f\u0001\f\u0001\f\u0005\f\u0102\b\f\n"+
		"\f\f\f\u0105\t\f\u0001\f\u0001\f\u0001\r\u0001\r\u0001\r\u0001\r\u0001"+
		"\r\u0001\r\u0001\r\u0005\r\u0110\b\r\n\r\f\r\u0113\t\r\u0001\r\u0001\r"+
		"\u0001\u000e\u0001\u000e\u0001\u000e\u0001\u000e\u0005\u000e\u011b\b\u000e"+
		"\n\u000e\f\u000e\u011e\t\u000e\u0001\u000f\u0001\u000f\u0001\u000f\u0001"+
		"\u000f\u0001\u000f\u0001\u000f\u0001\u000f\u0001\u000f\u0001\u000f\u0001"+
		"\u000f\u0001\u000f\u0001\u000f\u0001\u000f\u0001\u000f\u0001\u000f\u0003"+
		"\u000f\u012f\b\u000f\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0001"+
		"\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0011\u0001\u0011\u0003"+
		"\u0011\u013b\b\u0011\u0001\u0012\u0001\u0012\u0001\u0012\u0001\u0012\u0001"+
		"\u0012\u0001\u0012\u0005\u0012\u0143\b\u0012\n\u0012\f\u0012\u0146\t\u0012"+
		"\u0001\u0013\u0001\u0013\u0001\u0013\u0001\u0013\u0001\u0013\u0001\u0013"+
		"\u0001\u0013\u0001\u0013\u0003\u0013\u0150\b\u0013\u0001\u0014\u0001\u0014"+
		"\u0001\u0014\u0001\u0014\u0001\u0014\u0003\u0014\u0157\b\u0014\u0001\u0015"+
		"\u0001\u0015\u0003\u0015\u015b\b\u0015\u0001\u0016\u0001\u0016\u0001\u0016"+
		"\u0001\u0016\u0001\u0016\u0001\u0017\u0001\u0017\u0001\u0017\u0003\u0017"+
		"\u0165\b\u0017\u0001\u0018\u0001\u0018\u0001\u0018\u0001\u0018\u0001\u0019"+
		"\u0001\u0019\u0001\u0019\u0001\u0019\u0001\u0019\u0001\u0019\u0003\u0019"+
		"\u0171\b\u0019\u0001\u001a\u0003\u001a\u0174\b\u001a\u0001\u001a\u0001"+
		"\u001a\u0001\u001a\u0001\u001a\u0001\u001a\u0005\u001a\u017b\b\u001a\n"+
		"\u001a\f\u001a\u017e\t\u001a\u0001\u001a\u0001\u001a\u0001\u001b\u0003"+
		"\u001b\u0183\b\u001b\u0001\u001b\u0001\u001b\u0001\u001b\u0001\u001b\u0001"+
		"\u001b\u0005\u001b\u018a\b\u001b\n\u001b\f\u001b\u018d\t\u001b\u0001\u001b"+
		"\u0001\u001b\u0001\u001c\u0001\u001c\u0003\u001c\u0193\b\u001c\u0001\u001c"+
		"\u0001\u001c\u0005\u001c\u0197\b\u001c\n\u001c\f\u001c\u019a\t\u001c\u0001"+
		"\u001c\u0001\u001c\u0001\u001d\u0001\u001d\u0001\u001d\u0001\u001d\u0001"+
		"\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0003\u001e\u01a6\b\u001e\u0001"+
		"\u001f\u0001\u001f\u0001\u001f\u0001\u001f\u0001\u001f\u0001\u001f\u0003"+
		"\u001f\u01ae\b\u001f\u0001 \u0001 \u0001 \u0001 \u0001!\u0001!\u0001!"+
		"\u0001!\u0001\"\u0001\"\u0001\"\u0005\"\u01bb\b\"\n\"\f\"\u01be\t\"\u0001"+
		"#\u0001#\u0001#\u0004#\u01c3\b#\u000b#\f#\u01c4\u0001$\u0001$\u0001$\u0001"+
		"$\u0001%\u0001%\u0001%\u0001%\u0001%\u0001%\u0001%\u0001%\u0001%\u0001"+
		"%\u0001%\u0001%\u0001%\u0001%\u0001%\u0001%\u0001%\u0001%\u0003%\u01dd"+
		"\b%\u0001&\u0001&\u0001&\u0004&\u01e2\b&\u000b&\f&\u01e3\u0001\'\u0001"+
		"\'\u0001\'\u0004\'\u01e9\b\'\u000b\'\f\'\u01ea\u0001(\u0001(\u0003(\u01ef"+
		"\b(\u0001(\u0001(\u0001(\u0001(\u0005(\u01f5\b(\n(\f(\u01f8\t(\u0003("+
		"\u01fa\b(\u0001(\u0001(\u0001)\u0001)\u0001)\u0001)\u0001)\u0001)\u0001"+
		")\u0001)\u0001)\u0001)\u0001)\u0001)\u0001)\u0001)\u0001)\u0001)\u0001"+
		")\u0001)\u0001)\u0001)\u0001)\u0001)\u0001)\u0001)\u0001)\u0001)\u0001"+
		")\u0001)\u0001)\u0001)\u0001)\u0001)\u0001)\u0001)\u0001)\u0001)\u0001"+
		")\u0001)\u0001)\u0001)\u0001)\u0001)\u0001)\u0001)\u0001)\u0001)\u0001"+
		")\u0001)\u0001)\u0004)\u022f\b)\u000b)\f)\u0230\u0001)\u0001)\u0001)\u0001"+
		")\u0001)\u0004)\u0238\b)\u000b)\f)\u0239\u0001)\u0001)\u0001)\u0001)\u0001"+
		")\u0001)\u0001)\u0001)\u0001)\u0001)\u0001)\u0001)\u0001)\u0001)\u0001"+
		")\u0001)\u0003)\u024c\b)\u0001)\u0001)\u0001)\u0001)\u0001)\u0001)\u0001"+
		")\u0001)\u0001)\u0001)\u0001)\u0001)\u0001)\u0001)\u0001)\u0001)\u0001"+
		")\u0001)\u0001)\u0001)\u0001)\u0001)\u0001)\u0001)\u0001)\u0001)\u0001"+
		")\u0001)\u0001)\u0001)\u0001)\u0001)\u0001)\u0001)\u0001)\u0001)\u0001"+
		")\u0001)\u0001)\u0001)\u0001)\u0001)\u0001)\u0001)\u0001)\u0001)\u0001"+
		")\u0001)\u0005)\u027e\b)\n)\f)\u0281\t)\u0001*\u0001*\u0001*\u0001*\u0001"+
		"*\u0001*\u0001*\u0003*\u028a\b*\u0001+\u0001+\u0001+\u0001+\u0001+\u0001"+
		"+\u0001+\u0003+\u0293\b+\u0001,\u0001,\u0003,\u0297\b,\u0001-\u0001-\u0001"+
		"-\u0001-\u0001-\u0001-\u0001-\u0003-\u02a0\b-\u0001-\u0001-\u0001-\u0005"+
		"-\u02a5\b-\n-\f-\u02a8\t-\u0001.\u0001.\u0001/\u0001/\u0001/\u0001/\u0001"+
		"/\u0001/\u0005/\u02b2\b/\n/\f/\u02b5\t/\u0001/\u0001/\u0001/\u0001/\u0001"+
		"/\u0001/\u0005/\u02bd\b/\n/\f/\u02c0\t/\u0001/\u0001/\u0003/\u02c4\b/"+
		"\u00010\u00010\u00010\u00010\u00011\u00011\u00011\u00011\u00051\u02ce"+
		"\b1\n1\f1\u02d1\t1\u00031\u02d3\b1\u00011\u00011\u00012\u00012\u00012"+
		"\u00012\u00012\u00012\u00012\u00012\u00012\u00032\u02e0\b2\u00012\u0001"+
		"2\u00012\u00012\u00012\u00012\u00012\u00012\u00012\u00012\u00012\u0001"+
		"2\u00032\u02ee\b2\u00012\u00012\u00012\u00012\u00012\u00012\u00052\u02f6"+
		"\b2\n2\f2\u02f9\t2\u00032\u02fb\b2\u00012\u00012\u00012\u00012\u00012"+
		"\u00012\u00012\u00012\u00012\u00032\u0306\b2\u00012\u00012\u00012\u0003"+
		"2\u030b\b2\u00012\u00012\u00012\u00012\u00012\u00012\u00012\u00012\u0001"+
		"2\u00012\u00012\u00012\u00052\u0319\b2\n2\f2\u031c\t2\u00032\u031e\b2"+
		"\u00012\u00012\u00012\u00012\u00052\u0324\b2\n2\f2\u0327\t2\u00013\u0001"+
		"3\u00013\u00053\u032c\b3\n3\f3\u032f\t3\u00014\u00014\u00014\u00014\u0001"+
		"4\u00034\u0336\b4\u00015\u00015\u00015\u00045\u033b\b5\u000b5\f5\u033c"+
		"\u00015\u00015\u00015\u00015\u00055\u0343\b5\n5\f5\u0346\t5\u00035\u0348"+
		"\b5\u00015\u00015\u00016\u00016\u00036\u034e\b6\u00016\u00016\u00016\u0001"+
		"6\u00056\u0354\b6\n6\f6\u0357\t6\u00036\u0359\b6\u00016\u00016\u00017"+
		"\u00017\u00037\u035f\b7\u00018\u00018\u00018\u00018\u00018\u00018\u0001"+
		"8\u00018\u00018\u00018\u00038\u036b\b8\u00019\u00019\u00019\u00019\u0001"+
		"9\u00059\u0372\b9\n9\f9\u0375\t9\u00019\u00019\u0001:\u0001:\u0001:\u0001"+
		":\u0001;\u0001;\u0001;\u0005;\u0380\b;\n;\f;\u0383\t;\u0001;\u0000\u0004"+
		"$RZd<\u0000\u0002\u0004\u0006\b\n\f\u000e\u0010\u0012\u0014\u0016\u0018"+
		"\u001a\u001c\u001e \"$&(*,.02468:<>@BDFHJLNPRTVXZ\\^`bdfhjlnprtv\u0000"+
		"\u0004\u0002\u000036AA\u0001\u0000/0\u0001\u000036\u0003\u0000\u000b\f"+
		"?@BB\u03d7\u0000|\u0001\u0000\u0000\u0000\u0002\u0081\u0001\u0000\u0000"+
		"\u0000\u0004\u0084\u0001\u0000\u0000\u0000\u0006\u008b\u0001\u0000\u0000"+
		"\u0000\b\u00aa\u0001\u0000\u0000\u0000\n\u00b4\u0001\u0000\u0000\u0000"+
		"\f\u00b6\u0001\u0000\u0000\u0000\u000e\u00c1\u0001\u0000\u0000\u0000\u0010"+
		"\u00cc\u0001\u0000\u0000\u0000\u0012\u00d7\u0001\u0000\u0000\u0000\u0014"+
		"\u00e8\u0001\u0000\u0000\u0000\u0016\u00f8\u0001\u0000\u0000\u0000\u0018"+
		"\u00fa\u0001\u0000\u0000\u0000\u001a\u0108\u0001\u0000\u0000\u0000\u001c"+
		"\u0116\u0001\u0000\u0000\u0000\u001e\u012e\u0001\u0000\u0000\u0000 \u0130"+
		"\u0001\u0000\u0000\u0000\"\u013a\u0001\u0000\u0000\u0000$\u013c\u0001"+
		"\u0000\u0000\u0000&\u014f\u0001\u0000\u0000\u0000(\u0156\u0001\u0000\u0000"+
		"\u0000*\u015a\u0001\u0000\u0000\u0000,\u015c\u0001\u0000\u0000\u0000."+
		"\u0164\u0001\u0000\u0000\u00000\u0166\u0001\u0000\u0000\u00002\u016a\u0001"+
		"\u0000\u0000\u00004\u0173\u0001\u0000\u0000\u00006\u0182\u0001\u0000\u0000"+
		"\u00008\u0190\u0001\u0000\u0000\u0000:\u019d\u0001\u0000\u0000\u0000<"+
		"\u01a5\u0001\u0000\u0000\u0000>\u01ad\u0001\u0000\u0000\u0000@\u01af\u0001"+
		"\u0000\u0000\u0000B\u01b3\u0001\u0000\u0000\u0000D\u01b7\u0001\u0000\u0000"+
		"\u0000F\u01bf\u0001\u0000\u0000\u0000H\u01c6\u0001\u0000\u0000\u0000J"+
		"\u01dc\u0001\u0000\u0000\u0000L\u01de\u0001\u0000\u0000\u0000N\u01e5\u0001"+
		"\u0000\u0000\u0000P\u01ec\u0001\u0000\u0000\u0000R\u024b\u0001\u0000\u0000"+
		"\u0000T\u0289\u0001\u0000\u0000\u0000V\u0292\u0001\u0000\u0000\u0000X"+
		"\u0296\u0001\u0000\u0000\u0000Z\u029f\u0001\u0000\u0000\u0000\\\u02a9"+
		"\u0001\u0000\u0000\u0000^\u02c3\u0001\u0000\u0000\u0000`\u02c5\u0001\u0000"+
		"\u0000\u0000b\u02c9\u0001\u0000\u0000\u0000d\u030a\u0001\u0000\u0000\u0000"+
		"f\u0328\u0001\u0000\u0000\u0000h\u0335\u0001\u0000\u0000\u0000j\u0337"+
		"\u0001\u0000\u0000\u0000l\u034b\u0001\u0000\u0000\u0000n\u035e\u0001\u0000"+
		"\u0000\u0000p\u036a\u0001\u0000\u0000\u0000r\u036c\u0001\u0000\u0000\u0000"+
		"t\u0378\u0001\u0000\u0000\u0000v\u037c\u0001\u0000\u0000\u0000x{\u0003"+
		"\u0002\u0001\u0000y{\u0003\b\u0004\u0000zx\u0001\u0000\u0000\u0000zy\u0001"+
		"\u0000\u0000\u0000{~\u0001\u0000\u0000\u0000|z\u0001\u0000\u0000\u0000"+
		"|}\u0001\u0000\u0000\u0000}\u007f\u0001\u0000\u0000\u0000~|\u0001\u0000"+
		"\u0000\u0000\u007f\u0080\u0005\u0000\u0000\u0001\u0080\u0001\u0001\u0000"+
		"\u0000\u0000\u0081\u0082\u0005%\u0000\u0000\u0082\u0083\u0003\u0004\u0002"+
		"\u0000\u0083\u0003\u0001\u0000\u0000\u0000\u0084\u0087\u0003\u0006\u0003"+
		"\u0000\u0085\u0086\u0005\u0002\u0000\u0000\u0086\u0088\u0003\u0006\u0003"+
		"\u0000\u0087\u0085\u0001\u0000\u0000\u0000\u0088\u0089\u0001\u0000\u0000"+
		"\u0000\u0089\u0087\u0001\u0000\u0000\u0000\u0089\u008a\u0001\u0000\u0000"+
		"\u0000\u008a\u0005\u0001\u0000\u0000\u0000\u008b\u008c\u0007\u0000\u0000"+
		"\u0000\u008c\u0007\u0001\u0000\u0000\u0000\u008d\u008f\u0005&\u0000\u0000"+
		"\u008e\u008d\u0001\u0000\u0000\u0000\u008e\u008f\u0001\u0000\u0000\u0000"+
		"\u008f\u0090\u0001\u0000\u0000\u0000\u0090\u00ab\u0003\u0016\u000b\u0000"+
		"\u0091\u0093\u0005&\u0000\u0000\u0092\u0091\u0001\u0000\u0000\u0000\u0092"+
		"\u0093\u0001\u0000\u0000\u0000\u0093\u0094\u0001\u0000\u0000\u0000\u0094"+
		"\u00ab\u0003\u0018\f\u0000\u0095\u0097\u0005&\u0000\u0000\u0096\u0095"+
		"\u0001\u0000\u0000\u0000\u0096\u0097\u0001\u0000\u0000\u0000\u0097\u0098"+
		"\u0001\u0000\u0000\u0000\u0098\u00ab\u0003\u001a\r\u0000\u0099\u00ab\u0003"+
		"\u001c\u000e\u0000\u009a\u009c\u0005&\u0000\u0000\u009b\u009a\u0001\u0000"+
		"\u0000\u0000\u009b\u009c\u0001\u0000\u0000\u0000\u009c\u009d\u0001\u0000"+
		"\u0000\u0000\u009d\u00ab\u0003\u001e\u000f\u0000\u009e\u00a0\u0005&\u0000"+
		"\u0000\u009f\u009e\u0001\u0000\u0000\u0000\u009f\u00a0\u0001\u0000\u0000"+
		"\u0000\u00a0\u00a1\u0001\u0000\u0000\u0000\u00a1\u00ab\u0003,\u0016\u0000"+
		"\u00a2\u00a4\u0005&\u0000\u0000\u00a3\u00a2\u0001\u0000\u0000\u0000\u00a3"+
		"\u00a4\u0001\u0000\u0000\u0000\u00a4\u00a5\u0001\u0000\u0000\u0000\u00a5"+
		"\u00ab\u0003\u0010\b\u0000\u00a6\u00a8\u0005&\u0000\u0000\u00a7\u00a6"+
		"\u0001\u0000\u0000\u0000\u00a7\u00a8\u0001\u0000\u0000\u0000\u00a8\u00a9"+
		"\u0001\u0000\u0000\u0000\u00a9\u00ab\u0003\u0012\t\u0000\u00aa\u008e\u0001"+
		"\u0000\u0000\u0000\u00aa\u0092\u0001\u0000\u0000\u0000\u00aa\u0096\u0001"+
		"\u0000\u0000\u0000\u00aa\u0099\u0001\u0000\u0000\u0000\u00aa\u009b\u0001"+
		"\u0000\u0000\u0000\u00aa\u009f\u0001\u0000\u0000\u0000\u00aa\u00a3\u0001"+
		"\u0000\u0000\u0000\u00aa\u00a7\u0001\u0000\u0000\u0000\u00ab\t\u0001\u0000"+
		"\u0000\u0000\u00ac\u00ae\u0005A\u0000\u0000\u00ad\u00af\u0003\f\u0006"+
		"\u0000\u00ae\u00ad\u0001\u0000\u0000\u0000\u00ae\u00af\u0001\u0000\u0000"+
		"\u0000\u00af\u00b5\u0001\u0000\u0000\u0000\u00b0\u00b1\u0005\u0004\u0000"+
		"\u0000\u00b1\u00b2\u0003\n\u0005\u0000\u00b2\u00b3\u0005\u0005\u0000\u0000"+
		"\u00b3\u00b5\u0001\u0000\u0000\u0000\u00b4\u00ac\u0001\u0000\u0000\u0000"+
		"\u00b4\u00b0\u0001\u0000\u0000\u0000\u00b5\u000b\u0001\u0000\u0000\u0000"+
		"\u00b6\u00b7\u0005\u0016\u0000\u0000\u00b7\u00bc\u0003\n\u0005\u0000\u00b8"+
		"\u00b9\u0005\u0001\u0000\u0000\u00b9\u00bb\u0003\n\u0005\u0000\u00ba\u00b8"+
		"\u0001\u0000\u0000\u0000\u00bb\u00be\u0001\u0000\u0000\u0000\u00bc\u00ba"+
		"\u0001\u0000\u0000\u0000\u00bc\u00bd\u0001\u0000\u0000\u0000\u00bd\u00bf"+
		"\u0001\u0000\u0000\u0000\u00be\u00bc\u0001\u0000\u0000\u0000\u00bf\u00c0"+
		"\u0005\u0018\u0000\u0000\u00c0\r\u0001\u0000\u0000\u0000\u00c1\u00c2\u0005"+
		"\u0016\u0000\u0000\u00c2\u00c7\u0005A\u0000\u0000\u00c3\u00c4\u0005\u0001"+
		"\u0000\u0000\u00c4\u00c6\u0005A\u0000\u0000\u00c5\u00c3\u0001\u0000\u0000"+
		"\u0000\u00c6\u00c9\u0001\u0000\u0000\u0000\u00c7\u00c5\u0001\u0000\u0000"+
		"\u0000\u00c7\u00c8\u0001\u0000\u0000\u0000\u00c8\u00ca\u0001\u0000\u0000"+
		"\u0000\u00c9\u00c7\u0001\u0000\u0000\u0000\u00ca\u00cb\u0005\u0018\u0000"+
		"\u0000\u00cb\u000f\u0001\u0000\u0000\u0000\u00cc\u00cd\u0005<\u0000\u0000"+
		"\u00cd\u00cf\u0005A\u0000\u0000\u00ce\u00d0\u0003\u000e\u0007\u0000\u00cf"+
		"\u00ce\u0001\u0000\u0000\u0000\u00cf\u00d0\u0001\u0000\u0000\u0000\u00d0"+
		"\u00d1\u0001\u0000\u0000\u0000\u00d1\u00d2\u00038\u001c\u0000\u00d2\u00d3"+
		"\u0005\u0003\u0000\u0000\u00d3\u00d4\u0003\n\u0005\u0000\u00d4\u00d5\u0005"+
		"\u001a\u0000\u0000\u00d5\u00d6\u0003R)\u0000\u00d6\u0011\u0001\u0000\u0000"+
		"\u0000\u00d7\u00d8\u0005=\u0000\u0000\u00d8\u00d9\u0005A\u0000\u0000\u00d9"+
		"\u00da\u00038\u001c\u0000\u00da\u00db\u0005\u0003\u0000\u0000\u00db\u00dc"+
		"\u0003\n\u0005\u0000\u00dc\u00e0\u0005\b\u0000\u0000\u00dd\u00df\u0003"+
		"\u0014\n\u0000\u00de\u00dd\u0001\u0000\u0000\u0000\u00df\u00e2\u0001\u0000"+
		"\u0000\u0000\u00e0\u00de\u0001\u0000\u0000\u0000\u00e0\u00e1\u0001\u0000"+
		"\u0000\u0000\u00e1\u00e3\u0001\u0000\u0000\u0000\u00e2\u00e0\u0001\u0000"+
		"\u0000\u0000\u00e3\u00e4\u0005\t\u0000\u0000\u00e4\u0013\u0001\u0000\u0000"+
		"\u0000\u00e5\u00e9\u00032\u0019\u0000\u00e6\u00e9\u00034\u001a\u0000\u00e7"+
		"\u00e9\u00036\u001b\u0000\u00e8\u00e5\u0001\u0000\u0000\u0000\u00e8\u00e6"+
		"\u0001\u0000\u0000\u0000\u00e8\u00e7\u0001\u0000\u0000\u0000\u00e9\u0015"+
		"\u0001\u0000\u0000\u0000\u00ea\u00eb\u0005)\u0000\u0000\u00eb\u00ec\u0005"+
		"A\u0000\u0000\u00ec\u00f0\u0005\b\u0000\u0000\u00ed\u00ef\u0003.\u0017"+
		"\u0000\u00ee\u00ed\u0001\u0000\u0000\u0000\u00ef\u00f2\u0001\u0000\u0000"+
		"\u0000\u00f0\u00ee\u0001\u0000\u0000\u0000\u00f0\u00f1\u0001\u0000\u0000"+
		"\u0000\u00f1\u00f3\u0001\u0000\u0000\u0000\u00f2\u00f0\u0001\u0000\u0000"+
		"\u0000\u00f3\u00f9\u0005\t\u0000\u0000\u00f4\u00f5\u0005)\u0000\u0000"+
		"\u00f5\u00f6\u0005A\u0000\u0000\u00f6\u00f7\u0005\u001c\u0000\u0000\u00f7"+
		"\u00f9\u0003Z-\u0000\u00f8\u00ea\u0001\u0000\u0000\u0000\u00f8\u00f4\u0001"+
		"\u0000\u0000\u0000\u00f9\u0017\u0001\u0000\u0000\u0000\u00fa\u00fb\u0005"+
		"\'\u0000\u0000\u00fb\u00fd\u0005A\u0000\u0000\u00fc\u00fe\u0003\u000e"+
		"\u0007\u0000\u00fd\u00fc\u0001\u0000\u0000\u0000\u00fd\u00fe\u0001\u0000"+
		"\u0000\u0000\u00fe\u00ff\u0001\u0000\u0000\u0000\u00ff\u0103\u0005\b\u0000"+
		"\u0000\u0100\u0102\u00030\u0018\u0000\u0101\u0100\u0001\u0000\u0000\u0000"+
		"\u0102\u0105\u0001\u0000\u0000\u0000\u0103\u0101\u0001\u0000\u0000\u0000"+
		"\u0103\u0104\u0001\u0000\u0000\u0000\u0104\u0106\u0001\u0000\u0000\u0000"+
		"\u0105\u0103\u0001\u0000\u0000\u0000\u0106\u0107\u0005\t\u0000\u0000\u0107"+
		"\u0019\u0001\u0000\u0000\u0000\u0108\u0109\u0005(\u0000\u0000\u0109\u010a"+
		"\u0005A\u0000\u0000\u010a\u010b\u0005\u001c\u0000\u0000\u010b\u010c\u0005"+
		"\b\u0000\u0000\u010c\u0111\u0003\\.\u0000\u010d\u010e\u0005\u0001\u0000"+
		"\u0000\u010e\u0110\u0003\\.\u0000\u010f\u010d\u0001\u0000\u0000\u0000"+
		"\u0110\u0113\u0001\u0000\u0000\u0000\u0111\u010f\u0001\u0000\u0000\u0000"+
		"\u0111\u0112\u0001\u0000\u0000\u0000\u0112\u0114\u0001\u0000\u0000\u0000"+
		"\u0113\u0111\u0001\u0000\u0000\u0000\u0114\u0115\u0005\t\u0000\u0000\u0115"+
		"\u001b\u0001\u0000\u0000\u0000\u0116\u0117\u0005*\u0000\u0000\u0117\u011c"+
		"\u0005A\u0000\u0000\u0118\u0119\u0005\u0001\u0000\u0000\u0119\u011b\u0005"+
		"A\u0000\u0000\u011a\u0118\u0001\u0000\u0000\u0000\u011b\u011e\u0001\u0000"+
		"\u0000\u0000\u011c\u011a\u0001\u0000\u0000\u0000\u011c\u011d\u0001\u0000"+
		"\u0000\u0000\u011d\u001d\u0001\u0000\u0000\u0000\u011e\u011c\u0001\u0000"+
		"\u0000\u0000\u011f\u0120\u0005+\u0000\u0000\u0120\u0121\u0005A\u0000\u0000"+
		"\u0121\u0122\u0005\u001c\u0000\u0000\u0122\u012f\u0003 \u0010\u0000\u0123"+
		"\u0124\u0005+\u0000\u0000\u0124\u0125\u0005A\u0000\u0000\u0125\u0126\u0005"+
		"\u001c\u0000\u0000\u0126\u0127\u0003$\u0012\u0000\u0127\u0128\u0005\u000e"+
		"\u0000\u0000\u0128\u0129\u0003R)\u0000\u0129\u012f\u0001\u0000\u0000\u0000"+
		"\u012a\u012b\u0005+\u0000\u0000\u012b\u012c\u0005A\u0000\u0000\u012c\u012d"+
		"\u0005\u001c\u0000\u0000\u012d\u012f\u0003$\u0012\u0000\u012e\u011f\u0001"+
		"\u0000\u0000\u0000\u012e\u0123\u0001\u0000\u0000\u0000\u012e\u012a\u0001"+
		"\u0000\u0000\u0000\u012f\u001f\u0001\u0000\u0000\u0000\u0130\u0131\u0005"+
		"\u0016\u0000\u0000\u0131\u0132\u0003\"\u0011\u0000\u0132\u0133\u0005\u0018"+
		"\u0000\u0000\u0133\u0134\u0003$\u0012\u0000\u0134\u0135\u0005\u0016\u0000"+
		"\u0000\u0135\u0136\u0003R)\u0000\u0136\u0137\u0005\u0018\u0000\u0000\u0137"+
		"!\u0001\u0000\u0000\u0000\u0138\u013b\u0005\u000b\u0000\u0000\u0139\u013b"+
		"\u0003$\u0012\u0000\u013a\u0138\u0001\u0000\u0000\u0000\u013a\u0139\u0001"+
		"\u0000\u0000\u0000\u013b#\u0001\u0000\u0000\u0000\u013c\u013d\u0006\u0012"+
		"\uffff\uffff\u0000\u013d\u013e\u0003&\u0013\u0000\u013e\u0144\u0001\u0000"+
		"\u0000\u0000\u013f\u0140\n\u0002\u0000\u0000\u0140\u0141\u0005\n\u0000"+
		"\u0000\u0141\u0143\u0003$\u0012\u0003\u0142\u013f\u0001\u0000\u0000\u0000"+
		"\u0143\u0146\u0001\u0000\u0000\u0000\u0144\u0142\u0001\u0000\u0000\u0000"+
		"\u0144\u0145\u0001\u0000\u0000\u0000\u0145%\u0001\u0000\u0000\u0000\u0146"+
		"\u0144\u0001\u0000\u0000\u0000\u0147\u0148\u0003(\u0014\u0000\u0148\u0149"+
		"\u0005\u0006\u0000\u0000\u0149\u014a\u0005A\u0000\u0000\u014a\u014b\u0005"+
		"\u0003\u0000\u0000\u014b\u014c\u0003\n\u0005\u0000\u014c\u014d\u0005\u0007"+
		"\u0000\u0000\u014d\u0150\u0001\u0000\u0000\u0000\u014e\u0150\u0003(\u0014"+
		"\u0000\u014f\u0147\u0001\u0000\u0000\u0000\u014f\u014e\u0001\u0000\u0000"+
		"\u0000\u0150\'\u0001\u0000\u0000\u0000\u0151\u0157\u0003*\u0015\u0000"+
		"\u0152\u0153\u0005\u0004\u0000\u0000\u0153\u0154\u0003$\u0012\u0000\u0154"+
		"\u0155\u0005\u0005\u0000\u0000\u0155\u0157\u0001\u0000\u0000\u0000\u0156"+
		"\u0151\u0001\u0000\u0000\u0000\u0156\u0152\u0001\u0000\u0000\u0000\u0157"+
		")\u0001\u0000\u0000\u0000\u0158\u015b\u0003\u0004\u0002\u0000\u0159\u015b"+
		"\u0005A\u0000\u0000\u015a\u0158\u0001\u0000\u0000\u0000\u015a\u0159\u0001"+
		"\u0000\u0000\u0000\u015b+\u0001\u0000\u0000\u0000\u015c\u015d\u0005,\u0000"+
		"\u0000\u015d\u015e\u0005A\u0000\u0000\u015e\u015f\u0005\u001c\u0000\u0000"+
		"\u015f\u0160\u0003R)\u0000\u0160-\u0001\u0000\u0000\u0000\u0161\u0165"+
		"\u00032\u0019\u0000\u0162\u0165\u00034\u001a\u0000\u0163\u0165\u00036"+
		"\u001b\u0000\u0164\u0161\u0001\u0000\u0000\u0000\u0164\u0162\u0001\u0000"+
		"\u0000\u0000\u0164\u0163\u0001\u0000\u0000\u0000\u0165/\u0001\u0000\u0000"+
		"\u0000\u0166\u0167\u0005A\u0000\u0000\u0167\u0168\u0005\u0003\u0000\u0000"+
		"\u0168\u0169\u0003\n\u0005\u0000\u01691\u0001\u0000\u0000\u0000\u016a"+
		"\u016b\u0007\u0001\u0000\u0000\u016b\u016c\u0005A\u0000\u0000\u016c\u016d"+
		"\u0005\u0003\u0000\u0000\u016d\u0170\u0003\n\u0005\u0000\u016e\u016f\u0005"+
		"\u001c\u0000\u0000\u016f\u0171\u0003R)\u0000\u0170\u016e\u0001\u0000\u0000"+
		"\u0000\u0170\u0171\u0001\u0000\u0000\u0000\u01713\u0001\u0000\u0000\u0000"+
		"\u0172\u0174\u00056\u0000\u0000\u0173\u0172\u0001\u0000\u0000\u0000\u0173"+
		"\u0174\u0001\u0000\u0000\u0000\u0174\u0175\u0001\u0000\u0000\u0000\u0175"+
		"\u0176\u00051\u0000\u0000\u0176\u0177\u0005A\u0000\u0000\u0177\u0178\u0003"+
		"8\u001c\u0000\u0178\u017c\u0005\b\u0000\u0000\u0179\u017b\u0003<\u001e"+
		"\u0000\u017a\u0179\u0001\u0000\u0000\u0000\u017b\u017e\u0001\u0000\u0000"+
		"\u0000\u017c\u017a\u0001\u0000\u0000\u0000\u017c\u017d\u0001\u0000\u0000"+
		"\u0000\u017d\u017f\u0001\u0000\u0000\u0000\u017e\u017c\u0001\u0000\u0000"+
		"\u0000\u017f\u0180\u0005\t\u0000\u0000\u01805\u0001\u0000\u0000\u0000"+
		"\u0181\u0183\u0007\u0002\u0000\u0000\u0182\u0181\u0001\u0000\u0000\u0000"+
		"\u0182\u0183\u0001\u0000\u0000\u0000\u0183\u0184\u0001\u0000\u0000\u0000"+
		"\u0184\u0185\u00052\u0000\u0000\u0185\u0186\u0005A\u0000\u0000\u0186\u0187"+
		"\u00038\u001c\u0000\u0187\u018b\u0005\b\u0000\u0000\u0188\u018a\u0003"+
		">\u001f\u0000\u0189\u0188\u0001\u0000\u0000\u0000\u018a\u018d\u0001\u0000"+
		"\u0000\u0000\u018b\u0189\u0001\u0000\u0000\u0000\u018b\u018c\u0001\u0000"+
		"\u0000\u0000\u018c\u018e\u0001\u0000\u0000\u0000\u018d\u018b\u0001\u0000"+
		"\u0000\u0000\u018e\u018f\u0005\t\u0000\u0000\u018f7\u0001\u0000\u0000"+
		"\u0000\u0190\u0192\u0005\u0004\u0000\u0000\u0191\u0193\u0003:\u001d\u0000"+
		"\u0192\u0191\u0001\u0000\u0000\u0000\u0192\u0193\u0001\u0000\u0000\u0000"+
		"\u0193\u0198\u0001\u0000\u0000\u0000\u0194\u0195\u0005\u0001\u0000\u0000"+
		"\u0195\u0197\u0003:\u001d\u0000\u0196\u0194\u0001\u0000\u0000\u0000\u0197"+
		"\u019a\u0001\u0000\u0000\u0000\u0198\u0196\u0001\u0000\u0000\u0000\u0198"+
		"\u0199\u0001\u0000\u0000\u0000\u0199\u019b\u0001\u0000\u0000\u0000\u019a"+
		"\u0198\u0001\u0000\u0000\u0000\u019b\u019c\u0005\u0005\u0000\u0000\u019c"+
		"9\u0001\u0000\u0000\u0000\u019d\u019e\u0005A\u0000\u0000\u019e\u019f\u0005"+
		"\u0003\u0000\u0000\u019f\u01a0\u0003\n\u0005\u0000\u01a0;\u0001\u0000"+
		"\u0000\u0000\u01a1\u01a6\u0003L&\u0000\u01a2\u01a6\u0003D\"\u0000\u01a3"+
		"\u01a6\u0003F#\u0000\u01a4\u01a6\u0003N\'\u0000\u01a5\u01a1\u0001\u0000"+
		"\u0000\u0000\u01a5\u01a2\u0001\u0000\u0000\u0000\u01a5\u01a3\u0001\u0000"+
		"\u0000\u0000\u01a5\u01a4\u0001\u0000\u0000\u0000\u01a6=\u0001\u0000\u0000"+
		"\u0000\u01a7\u01ae\u0003B!\u0000\u01a8\u01ae\u0003L&\u0000\u01a9\u01ae"+
		"\u0003D\"\u0000\u01aa\u01ae\u0003F#\u0000\u01ab\u01ae\u0003N\'\u0000\u01ac"+
		"\u01ae\u0003@ \u0000\u01ad\u01a7\u0001\u0000\u0000\u0000\u01ad\u01a8\u0001"+
		"\u0000\u0000\u0000\u01ad\u01a9\u0001\u0000\u0000\u0000\u01ad\u01aa\u0001"+
		"\u0000\u0000\u0000\u01ad\u01ab\u0001\u0000\u0000\u0000\u01ad\u01ac\u0001"+
		"\u0000\u0000\u0000\u01ae?\u0001\u0000\u0000\u0000\u01af\u01b0\u0005>\u0000"+
		"\u0000\u01b0\u01b1\u0005\u0003\u0000\u0000\u01b1\u01b2\u0003R)\u0000\u01b2"+
		"A\u0001\u0000\u0000\u0000\u01b3\u01b4\u00057\u0000\u0000\u01b4\u01b5\u0005"+
		"\u0003\u0000\u0000\u01b5\u01b6\u0003R)\u0000\u01b6C\u0001\u0000\u0000"+
		"\u0000\u01b7\u01b8\u00058\u0000\u0000\u01b8\u01bc\u0005\u0003\u0000\u0000"+
		"\u01b9\u01bb\u0003J%\u0000\u01ba\u01b9\u0001\u0000\u0000\u0000\u01bb\u01be"+
		"\u0001\u0000\u0000\u0000\u01bc\u01ba\u0001\u0000\u0000\u0000\u01bc\u01bd"+
		"\u0001\u0000\u0000\u0000\u01bdE\u0001\u0000\u0000\u0000\u01be\u01bc\u0001"+
		"\u0000\u0000\u0000\u01bf\u01c0\u00059\u0000\u0000\u01c0\u01c2\u0005\u0003"+
		"\u0000\u0000\u01c1\u01c3\u0003H$\u0000\u01c2\u01c1\u0001\u0000\u0000\u0000"+
		"\u01c3\u01c4\u0001\u0000\u0000\u0000\u01c4\u01c2\u0001\u0000\u0000\u0000"+
		"\u01c4\u01c5\u0001\u0000\u0000\u0000\u01c5G\u0001\u0000\u0000\u0000\u01c6"+
		"\u01c7\u0003R)\u0000\u01c7\u01c8\u0005$\u0000\u0000\u01c8\u01c9\u0003"+
		"R)\u0000\u01c9I\u0001\u0000\u0000\u0000\u01ca\u01cb\u0003v;\u0000\u01cb"+
		"\u01cc\u0005\u001c\u0000\u0000\u01cc\u01cd\u0003R)\u0000\u01cd\u01dd\u0001"+
		"\u0000\u0000\u0000\u01ce\u01cf\u0005A\u0000\u0000\u01cf\u01d0\u0005\u0006"+
		"\u0000\u0000\u01d0\u01d1\u0003R)\u0000\u01d1\u01d2\u0005\u0007\u0000\u0000"+
		"\u01d2\u01d3\u0005\u001c\u0000\u0000\u01d3\u01d4\u0003R)\u0000\u01d4\u01dd"+
		"\u0001\u0000\u0000\u0000\u01d5\u01d6\u0005!\u0000\u0000\u01d6\u01d7\u0005"+
		"A\u0000\u0000\u01d7\u01d8\u0005\u0003\u0000\u0000\u01d8\u01d9\u0003\n"+
		"\u0005\u0000\u01d9\u01da\u0005\u001c\u0000\u0000\u01da\u01db\u0003R)\u0000"+
		"\u01db\u01dd\u0001\u0000\u0000\u0000\u01dc\u01ca\u0001\u0000\u0000\u0000"+
		"\u01dc\u01ce\u0001\u0000\u0000\u0000\u01dc\u01d5\u0001\u0000\u0000\u0000"+
		"\u01ddK\u0001\u0000\u0000\u0000\u01de\u01df\u0005:\u0000\u0000\u01df\u01e1"+
		"\u0005\u0003\u0000\u0000\u01e0\u01e2\u0003P(\u0000\u01e1\u01e0\u0001\u0000"+
		"\u0000\u0000\u01e2\u01e3\u0001\u0000\u0000\u0000\u01e3\u01e1\u0001\u0000"+
		"\u0000\u0000\u01e3\u01e4\u0001\u0000\u0000\u0000\u01e4M\u0001\u0000\u0000"+
		"\u0000\u01e5\u01e6\u0005;\u0000\u0000\u01e6\u01e8\u0005\u0003\u0000\u0000"+
		"\u01e7\u01e9\u0003P(\u0000\u01e8\u01e7\u0001\u0000\u0000\u0000\u01e9\u01ea"+
		"\u0001\u0000\u0000\u0000\u01ea\u01e8\u0001\u0000\u0000\u0000\u01ea\u01eb"+
		"\u0001\u0000\u0000\u0000\u01ebO\u0001\u0000\u0000\u0000\u01ec\u01ee\u0005"+
		"A\u0000\u0000\u01ed\u01ef\u0003\f\u0006\u0000\u01ee\u01ed\u0001\u0000"+
		"\u0000\u0000\u01ee\u01ef\u0001\u0000\u0000\u0000\u01ef\u01f0\u0001\u0000"+
		"\u0000\u0000\u01f0\u01f9\u0005\u0004\u0000\u0000\u01f1\u01f6\u0003R)\u0000"+
		"\u01f2\u01f3\u0005\u0001\u0000\u0000\u01f3\u01f5\u0003R)\u0000\u01f4\u01f2"+
		"\u0001\u0000\u0000\u0000\u01f5\u01f8\u0001\u0000\u0000\u0000\u01f6\u01f4"+
		"\u0001\u0000\u0000\u0000\u01f6\u01f7\u0001\u0000\u0000\u0000\u01f7\u01fa"+
		"\u0001\u0000\u0000\u0000\u01f8\u01f6\u0001\u0000\u0000\u0000\u01f9\u01f1"+
		"\u0001\u0000\u0000\u0000\u01f9\u01fa\u0001\u0000\u0000\u0000\u01fa\u01fb"+
		"\u0001\u0000\u0000\u0000\u01fb\u01fc\u0005\u0005\u0000\u0000\u01fcQ\u0001"+
		"\u0000\u0000\u0000\u01fd\u01fe\u0006)\uffff\uffff\u0000\u01fe\u024c\u0003"+
		"\\.\u0000\u01ff\u0200\u0005\u0004\u0000\u0000\u0200\u0201\u0003R)\u0000"+
		"\u0201\u0202\u0005\u0005\u0000\u0000\u0202\u024c\u0001\u0000\u0000\u0000"+
		"\u0203\u024c\u0003^/\u0000\u0204\u024c\u0003b1\u0000\u0205\u024c\u0003"+
		"f3\u0000\u0206\u024c\u0003d2\u0000\u0207\u024c\u0003v;\u0000\u0208\u024c"+
		"\u0003r9\u0000\u0209\u024c\u0003l6\u0000\u020a\u020b\u0005\u0010\u0000"+
		"\u0000\u020b\u024c\u0003R)\u0019\u020c\u020d\u0005\r\u0000\u0000\u020d"+
		"\u024c\u0003R)\u0018\u020e\u020f\u0005\u000f\u0000\u0000\u020f\u024c\u0003"+
		"R)\u0017\u0210\u0211\u0005\u001f\u0000\u0000\u0211\u0212\u0005\u0004\u0000"+
		"\u0000\u0212\u0213\u0003R)\u0000\u0213\u0214\u0005\u0005\u0000\u0000\u0214"+
		"\u0215\u0005\b\u0000\u0000\u0215\u0216\u0003R)\u0000\u0216\u0217\u0005"+
		"\t\u0000\u0000\u0217\u0218\u0005 \u0000\u0000\u0218\u0219\u0005\b\u0000"+
		"\u0000\u0219\u021a\u0003R)\u0000\u021a\u021b\u0005\t\u0000\u0000\u021b"+
		"\u024c\u0001\u0000\u0000\u0000\u021c\u021d\u0005!\u0000\u0000\u021d\u021e"+
		"\u0005\u0004\u0000\u0000\u021e\u021f\u0005A\u0000\u0000\u021f\u0220\u0005"+
		"\u0003\u0000\u0000\u0220\u0221\u0003\n\u0005\u0000\u0221\u0222\u0005\u001c"+
		"\u0000\u0000\u0222\u0223\u0003R)\u0000\u0223\u0224\u0005\u0005\u0000\u0000"+
		"\u0224\u0225\u0005\b\u0000\u0000\u0225\u0226\u0003R)\u0000\u0226\u0227"+
		"\u0005\t\u0000\u0000\u0227\u024c\u0001\u0000\u0000\u0000\u0228\u0229\u0005"+
		"\"\u0000\u0000\u0229\u022a\u0005\u0004\u0000\u0000\u022a\u022b\u0003R"+
		")\u0000\u022b\u022c\u0005\u0005\u0000\u0000\u022c\u022e\u0005\b\u0000"+
		"\u0000\u022d\u022f\u0003T*\u0000\u022e\u022d\u0001\u0000\u0000\u0000\u022f"+
		"\u0230\u0001\u0000\u0000\u0000\u0230\u022e\u0001\u0000\u0000\u0000\u0230"+
		"\u0231\u0001\u0000\u0000\u0000\u0231\u0232\u0001\u0000\u0000\u0000\u0232"+
		"\u0233\u0005\t\u0000\u0000\u0233\u024c\u0001\u0000\u0000\u0000\u0234\u0235"+
		"\u0005\"\u0000\u0000\u0235\u0237\u0005\b\u0000\u0000\u0236\u0238\u0003"+
		"V+\u0000\u0237\u0236\u0001\u0000\u0000\u0000\u0238\u0239\u0001\u0000\u0000"+
		"\u0000\u0239\u0237\u0001\u0000\u0000\u0000\u0239\u023a\u0001\u0000\u0000"+
		"\u0000\u023a\u023b\u0001\u0000\u0000\u0000\u023b\u023c\u0005\t\u0000\u0000"+
		"\u023c\u024c\u0001\u0000\u0000\u0000\u023d\u023e\u0005-\u0000\u0000\u023e"+
		"\u023f\u0005A\u0000\u0000\u023f\u0240\u0005\u0003\u0000\u0000\u0240\u0241"+
		"\u0003\n\u0005\u0000\u0241\u0242\u0005\u0001\u0000\u0000\u0242\u0243\u0003"+
		"R)\u0002\u0243\u024c\u0001\u0000\u0000\u0000\u0244\u0245\u0005.\u0000"+
		"\u0000\u0245\u0246\u0005A\u0000\u0000\u0246\u0247\u0005\u0003\u0000\u0000"+
		"\u0247\u0248\u0003\n\u0005\u0000\u0248\u0249\u0005\u0001\u0000\u0000\u0249"+
		"\u024a\u0003R)\u0001\u024a\u024c\u0001\u0000\u0000\u0000\u024b\u01fd\u0001"+
		"\u0000\u0000\u0000\u024b\u01ff\u0001\u0000\u0000\u0000\u024b\u0203\u0001"+
		"\u0000\u0000\u0000\u024b\u0204\u0001\u0000\u0000\u0000\u024b\u0205\u0001"+
		"\u0000\u0000\u0000\u024b\u0206\u0001\u0000\u0000\u0000\u024b\u0207\u0001"+
		"\u0000\u0000\u0000\u024b\u0208\u0001\u0000\u0000\u0000\u024b\u0209\u0001"+
		"\u0000\u0000\u0000\u024b\u020a\u0001\u0000\u0000\u0000\u024b\u020c\u0001"+
		"\u0000\u0000\u0000\u024b\u020e\u0001\u0000\u0000\u0000\u024b\u0210\u0001"+
		"\u0000\u0000\u0000\u024b\u021c\u0001\u0000\u0000\u0000\u024b\u0228\u0001"+
		"\u0000\u0000\u0000\u024b\u0234\u0001\u0000\u0000\u0000\u024b\u023d\u0001"+
		"\u0000\u0000\u0000\u024b\u0244\u0001\u0000\u0000\u0000\u024c\u027f\u0001"+
		"\u0000\u0000\u0000\u024d\u024e\n\u0016\u0000\u0000\u024e\u024f\u0005\u0011"+
		"\u0000\u0000\u024f\u027e\u0003R)\u0017\u0250\u0251\n\u0015\u0000\u0000"+
		"\u0251\u0252\u0005\u0012\u0000\u0000\u0252\u027e\u0003R)\u0016\u0253\u0254"+
		"\n\u0014\u0000\u0000\u0254\u0255\u0005\u0013\u0000\u0000\u0255\u027e\u0003"+
		"R)\u0015\u0256\u0257\n\u0013\u0000\u0000\u0257\u0258\u0005\u0014\u0000"+
		"\u0000\u0258\u027e\u0003R)\u0014\u0259\u025a\n\u0012\u0000\u0000\u025a"+
		"\u025b\u0005\u0015\u0000\u0000\u025b\u027e\u0003R)\u0013\u025c\u025d\n"+
		"\u0011\u0000\u0000\u025d\u025e\u0005\u0016\u0000\u0000\u025e\u027e\u0003"+
		"R)\u0012\u025f\u0260\n\u0010\u0000\u0000\u0260\u0261\u0005\u0017\u0000"+
		"\u0000\u0261\u027e\u0003R)\u0011\u0262\u0263\n\u000f\u0000\u0000\u0263"+
		"\u0264\u0005\u0018\u0000\u0000\u0264\u027e\u0003R)\u0010\u0265\u0266\n"+
		"\u000e\u0000\u0000\u0266\u0267\u0005\u0019\u0000\u0000\u0267\u027e\u0003"+
		"R)\u000f\u0268\u0269\n\r\u0000\u0000\u0269\u026a\u0005#\u0000\u0000\u026a"+
		"\u027e\u0003R)\u000e\u026b\u026c\n\f\u0000\u0000\u026c\u026d\u0005\u001a"+
		"\u0000\u0000\u026d\u027e\u0003R)\r\u026e\u026f\n\u000b\u0000\u0000\u026f"+
		"\u0270\u0005\u001b\u0000\u0000\u0270\u027e\u0003R)\f\u0271\u0272\n\n\u0000"+
		"\u0000\u0272\u0273\u0005\r\u0000\u0000\u0273\u027e\u0003R)\u000b\u0274"+
		"\u0275\n\t\u0000\u0000\u0275\u0276\u0005\u000f\u0000\u0000\u0276\u027e"+
		"\u0003R)\n\u0277\u0278\n\b\u0000\u0000\u0278\u0279\u0005\u001d\u0000\u0000"+
		"\u0279\u027e\u0003R)\b\u027a\u027b\n\u0007\u0000\u0000\u027b\u027c\u0005"+
		"\u001e\u0000\u0000\u027c\u027e\u0003R)\b\u027d\u024d\u0001\u0000\u0000"+
		"\u0000\u027d\u0250\u0001\u0000\u0000\u0000\u027d\u0253\u0001\u0000\u0000"+
		"\u0000\u027d\u0256\u0001\u0000\u0000\u0000\u027d\u0259\u0001\u0000\u0000"+
		"\u0000\u027d\u025c\u0001\u0000\u0000\u0000\u027d\u025f\u0001\u0000\u0000"+
		"\u0000\u027d\u0262\u0001\u0000\u0000\u0000\u027d\u0265\u0001\u0000\u0000"+
		"\u0000\u027d\u0268\u0001\u0000\u0000\u0000\u027d\u026b\u0001\u0000\u0000"+
		"\u0000\u027d\u026e\u0001\u0000\u0000\u0000\u027d\u0271\u0001\u0000\u0000"+
		"\u0000\u027d\u0274\u0001\u0000\u0000\u0000\u027d\u0277\u0001\u0000\u0000"+
		"\u0000\u027d\u027a\u0001\u0000\u0000\u0000\u027e\u0281\u0001\u0000\u0000"+
		"\u0000\u027f\u027d\u0001\u0000\u0000\u0000\u027f\u0280\u0001\u0000\u0000"+
		"\u0000\u0280S\u0001\u0000\u0000\u0000\u0281\u027f\u0001\u0000\u0000\u0000"+
		"\u0282\u0283\u0003X,\u0000\u0283\u0284\u0005$\u0000\u0000\u0284\u0285"+
		"\u0003R)\u0000\u0285\u028a\u0001\u0000\u0000\u0000\u0286\u0287\u0005 "+
		"\u0000\u0000\u0287\u0288\u0005$\u0000\u0000\u0288\u028a\u0003R)\u0000"+
		"\u0289\u0282\u0001\u0000\u0000\u0000\u0289\u0286\u0001\u0000\u0000\u0000"+
		"\u028aU\u0001\u0000\u0000\u0000\u028b\u028c\u0003R)\u0000\u028c\u028d"+
		"\u0005$\u0000\u0000\u028d\u028e\u0003R)\u0000\u028e\u0293\u0001\u0000"+
		"\u0000\u0000\u028f\u0290\u0005 \u0000\u0000\u0290\u0291\u0005$\u0000\u0000"+
		"\u0291\u0293\u0003R)\u0000\u0292\u028b\u0001\u0000\u0000\u0000\u0292\u028f"+
		"\u0001\u0000\u0000\u0000\u0293W\u0001\u0000\u0000\u0000\u0294\u0297\u0003"+
		"\\.\u0000\u0295\u0297\u0003r9\u0000\u0296\u0294\u0001\u0000\u0000\u0000"+
		"\u0296\u0295\u0001\u0000\u0000\u0000\u0297Y\u0001\u0000\u0000\u0000\u0298"+
		"\u0299\u0006-\uffff\uffff\u0000\u0299\u02a0\u0003\u0004\u0002\u0000\u029a"+
		"\u02a0\u0005A\u0000\u0000\u029b\u029c\u0005\u0004\u0000\u0000\u029c\u029d"+
		"\u0003Z-\u0000\u029d\u029e\u0005\u0005\u0000\u0000\u029e\u02a0\u0001\u0000"+
		"\u0000\u0000\u029f\u0298\u0001\u0000\u0000\u0000\u029f\u029a\u0001\u0000"+
		"\u0000\u0000\u029f\u029b\u0001\u0000\u0000\u0000\u02a0\u02a6\u0001\u0000"+
		"\u0000\u0000\u02a1\u02a2\n\u0001\u0000\u0000\u02a2\u02a3\u0005\n\u0000"+
		"\u0000\u02a3\u02a5\u0003Z-\u0002\u02a4\u02a1\u0001\u0000\u0000\u0000\u02a5"+
		"\u02a8\u0001\u0000\u0000\u0000\u02a6\u02a4\u0001\u0000\u0000\u0000\u02a6"+
		"\u02a7\u0001\u0000\u0000\u0000\u02a7[\u0001\u0000\u0000\u0000\u02a8\u02a6"+
		"\u0001\u0000\u0000\u0000\u02a9\u02aa\u0007\u0003\u0000\u0000\u02aa]\u0001"+
		"\u0000\u0000\u0000\u02ab\u02ac\u0005\u0006\u0000\u0000\u02ac\u02c4\u0005"+
		"\u0007\u0000\u0000\u02ad\u02ae\u0005\u0006\u0000\u0000\u02ae\u02b3\u0003"+
		"`0\u0000\u02af\u02b0\u0005\u0001\u0000\u0000\u02b0\u02b2\u0003`0\u0000"+
		"\u02b1\u02af\u0001\u0000\u0000\u0000\u02b2\u02b5\u0001\u0000\u0000\u0000"+
		"\u02b3\u02b1\u0001\u0000\u0000\u0000\u02b3\u02b4\u0001\u0000\u0000\u0000"+
		"\u02b4\u02b6\u0001\u0000\u0000\u0000\u02b5\u02b3\u0001\u0000\u0000\u0000"+
		"\u02b6\u02b7\u0005\u0007\u0000\u0000\u02b7\u02c4\u0001\u0000\u0000\u0000"+
		"\u02b8\u02b9\u0005\u0006\u0000\u0000\u02b9\u02be\u0003R)\u0000\u02ba\u02bb"+
		"\u0005\u0001\u0000\u0000\u02bb\u02bd\u0003R)\u0000\u02bc\u02ba\u0001\u0000"+
		"\u0000\u0000\u02bd\u02c0\u0001\u0000\u0000\u0000\u02be\u02bc\u0001\u0000"+
		"\u0000\u0000\u02be\u02bf\u0001\u0000\u0000\u0000\u02bf\u02c1\u0001\u0000"+
		"\u0000\u0000\u02c0\u02be\u0001\u0000\u0000\u0000\u02c1\u02c2\u0005\u0007"+
		"\u0000\u0000\u02c2\u02c4\u0001\u0000\u0000\u0000\u02c3\u02ab\u0001\u0000"+
		"\u0000\u0000\u02c3\u02ad\u0001\u0000\u0000\u0000\u02c3\u02b8\u0001\u0000"+
		"\u0000\u0000\u02c4_\u0001\u0000\u0000\u0000\u02c5\u02c6\u0003R)\u0000"+
		"\u02c6\u02c7\u0005$\u0000\u0000\u02c7\u02c8\u0003R)\u0000\u02c8a\u0001"+
		"\u0000\u0000\u0000\u02c9\u02d2\u0005\b\u0000\u0000\u02ca\u02cf\u0003R"+
		")\u0000\u02cb\u02cc\u0005\u0001\u0000\u0000\u02cc\u02ce\u0003R)\u0000"+
		"\u02cd\u02cb\u0001\u0000\u0000\u0000\u02ce\u02d1\u0001\u0000\u0000\u0000"+
		"\u02cf\u02cd\u0001\u0000\u0000\u0000\u02cf\u02d0\u0001\u0000\u0000\u0000"+
		"\u02d0\u02d3\u0001\u0000\u0000\u0000\u02d1\u02cf\u0001\u0000\u0000\u0000"+
		"\u02d2\u02ca\u0001\u0000\u0000\u0000\u02d2\u02d3\u0001\u0000\u0000\u0000"+
		"\u02d3\u02d4\u0001\u0000\u0000\u0000\u02d4\u02d5\u0005\t\u0000\u0000\u02d5"+
		"c\u0001\u0000\u0000\u0000\u02d6\u02df\u00062\uffff\uffff\u0000\u02d7\u02e0"+
		"\u0003l6\u0000\u02d8\u02e0\u0003v;\u0000\u02d9\u02e0\u0003^/\u0000\u02da"+
		"\u02e0\u0003b1\u0000\u02db\u02dc\u0005\u0004\u0000\u0000\u02dc\u02dd\u0003"+
		"R)\u0000\u02dd\u02de\u0005\u0005\u0000\u0000\u02de\u02e0\u0001\u0000\u0000"+
		"\u0000\u02df\u02d7\u0001\u0000\u0000\u0000\u02df\u02d8\u0001\u0000\u0000"+
		"\u0000\u02df\u02d9\u0001\u0000\u0000\u0000\u02df\u02da\u0001\u0000\u0000"+
		"\u0000\u02df\u02db\u0001\u0000\u0000\u0000\u02e0\u02e1\u0001\u0000\u0000"+
		"\u0000\u02e1\u02e2\u0005\u0006\u0000\u0000\u02e2\u02e3\u0003h4\u0000\u02e3"+
		"\u02e4\u0005\u0007\u0000\u0000\u02e4\u030b\u0001\u0000\u0000\u0000\u02e5"+
		"\u02ee\u0003l6\u0000\u02e6\u02ee\u0003v;\u0000\u02e7\u02ee\u0003^/\u0000"+
		"\u02e8\u02ee\u0003b1\u0000\u02e9\u02ea\u0005\u0004\u0000\u0000\u02ea\u02eb"+
		"\u0003R)\u0000\u02eb\u02ec\u0005\u0005\u0000\u0000\u02ec\u02ee\u0001\u0000"+
		"\u0000\u0000\u02ed\u02e5\u0001\u0000\u0000\u0000\u02ed\u02e6\u0001\u0000"+
		"\u0000\u0000\u02ed\u02e7\u0001\u0000\u0000\u0000\u02ed\u02e8\u0001\u0000"+
		"\u0000\u0000\u02ed\u02e9\u0001\u0000\u0000\u0000\u02ee\u02ef\u0001\u0000"+
		"\u0000\u0000\u02ef\u02f0\u0005\u0002\u0000\u0000\u02f0\u02f1\u0005A\u0000"+
		"\u0000\u02f1\u02fa\u0005\u0004\u0000\u0000\u02f2\u02f7\u0003n7\u0000\u02f3"+
		"\u02f4\u0005\u0001\u0000\u0000\u02f4\u02f6\u0003n7\u0000\u02f5\u02f3\u0001"+
		"\u0000\u0000\u0000\u02f6\u02f9\u0001\u0000\u0000\u0000\u02f7\u02f5\u0001"+
		"\u0000\u0000\u0000\u02f7\u02f8\u0001\u0000\u0000\u0000\u02f8\u02fb\u0001"+
		"\u0000\u0000\u0000\u02f9\u02f7\u0001\u0000\u0000\u0000\u02fa\u02f2\u0001"+
		"\u0000\u0000\u0000\u02fa\u02fb\u0001\u0000\u0000\u0000\u02fb\u02fc\u0001"+
		"\u0000\u0000\u0000\u02fc\u02fd\u0005\u0005\u0000\u0000\u02fd\u030b\u0001"+
		"\u0000\u0000\u0000\u02fe\u0306\u0003l6\u0000\u02ff\u0306\u0003^/\u0000"+
		"\u0300\u0306\u0003b1\u0000\u0301\u0302\u0005\u0004\u0000\u0000\u0302\u0303"+
		"\u0003R)\u0000\u0303\u0304\u0005\u0005\u0000\u0000\u0304\u0306\u0001\u0000"+
		"\u0000\u0000\u0305\u02fe\u0001\u0000\u0000\u0000\u0305\u02ff\u0001\u0000"+
		"\u0000\u0000\u0305\u0300\u0001\u0000\u0000\u0000\u0305\u0301\u0001\u0000"+
		"\u0000\u0000\u0306\u0307\u0001\u0000\u0000\u0000\u0307\u0308\u0005\u0002"+
		"\u0000\u0000\u0308\u0309\u0005A\u0000\u0000\u0309\u030b\u0001\u0000\u0000"+
		"\u0000\u030a\u02d6\u0001\u0000\u0000\u0000\u030a\u02ed\u0001\u0000\u0000"+
		"\u0000\u030a\u0305\u0001\u0000\u0000\u0000\u030b\u0325\u0001\u0000\u0000"+
		"\u0000\u030c\u030d\n\u0006\u0000\u0000\u030d\u030e\u0005\u0006\u0000\u0000"+
		"\u030e\u030f\u0003h4\u0000\u030f\u0310\u0005\u0007\u0000\u0000\u0310\u0324"+
		"\u0001\u0000\u0000\u0000\u0311\u0312\n\u0005\u0000\u0000\u0312\u0313\u0005"+
		"\u0002\u0000\u0000\u0313\u0314\u0005A\u0000\u0000\u0314\u031d\u0005\u0004"+
		"\u0000\u0000\u0315\u031a\u0003n7\u0000\u0316\u0317\u0005\u0001\u0000\u0000"+
		"\u0317\u0319\u0003n7\u0000\u0318\u0316\u0001\u0000\u0000\u0000\u0319\u031c"+
		"\u0001\u0000\u0000\u0000\u031a\u0318\u0001\u0000\u0000\u0000\u031a\u031b"+
		"\u0001\u0000\u0000\u0000\u031b\u031e\u0001\u0000\u0000\u0000\u031c\u031a"+
		"\u0001\u0000\u0000\u0000\u031d\u0315\u0001\u0000\u0000\u0000\u031d\u031e"+
		"\u0001\u0000\u0000\u0000\u031e\u031f\u0001\u0000\u0000\u0000\u031f\u0324"+
		"\u0005\u0005\u0000\u0000\u0320\u0321\n\u0004\u0000\u0000\u0321\u0322\u0005"+
		"\u0002\u0000\u0000\u0322\u0324\u0005A\u0000\u0000\u0323\u030c\u0001\u0000"+
		"\u0000\u0000\u0323\u0311\u0001\u0000\u0000\u0000\u0323\u0320\u0001\u0000"+
		"\u0000\u0000\u0324\u0327\u0001\u0000\u0000\u0000\u0325\u0323\u0001\u0000"+
		"\u0000\u0000\u0325\u0326\u0001\u0000\u0000\u0000\u0326e\u0001\u0000\u0000"+
		"\u0000\u0327\u0325\u0001\u0000\u0000\u0000\u0328\u032d\u0003j5\u0000\u0329"+
		"\u032a\u0005\u0002\u0000\u0000\u032a\u032c\u0005A\u0000\u0000\u032b\u0329"+
		"\u0001\u0000\u0000\u0000\u032c\u032f\u0001\u0000\u0000\u0000\u032d\u032b"+
		"\u0001\u0000\u0000\u0000\u032d\u032e\u0001\u0000\u0000\u0000\u032eg\u0001"+
		"\u0000\u0000\u0000\u032f\u032d\u0001\u0000\u0000\u0000\u0330\u0331\u0003"+
		"R)\u0000\u0331\u0332\u0005\u0003\u0000\u0000\u0332\u0333\u0003R)\u0000"+
		"\u0333\u0336\u0001\u0000\u0000\u0000\u0334\u0336\u0003R)\u0000\u0335\u0330"+
		"\u0001\u0000\u0000\u0000\u0335\u0334\u0001\u0000\u0000\u0000\u0336i\u0001"+
		"\u0000\u0000\u0000\u0337\u033a\u0005A\u0000\u0000\u0338\u0339\u0005\u0002"+
		"\u0000\u0000\u0339\u033b\u0005A\u0000\u0000\u033a\u0338\u0001\u0000\u0000"+
		"\u0000\u033b\u033c\u0001\u0000\u0000\u0000\u033c\u033a\u0001\u0000\u0000"+
		"\u0000\u033c\u033d\u0001\u0000\u0000\u0000\u033d\u033e\u0001\u0000\u0000"+
		"\u0000\u033e\u0347\u0005\u0004\u0000\u0000\u033f\u0344\u0003n7\u0000\u0340"+
		"\u0341\u0005\u0001\u0000\u0000\u0341\u0343\u0003n7\u0000\u0342\u0340\u0001"+
		"\u0000\u0000\u0000\u0343\u0346\u0001\u0000\u0000\u0000\u0344\u0342\u0001"+
		"\u0000\u0000\u0000\u0344\u0345\u0001\u0000\u0000\u0000\u0345\u0348\u0001"+
		"\u0000\u0000\u0000\u0346\u0344\u0001\u0000\u0000\u0000\u0347\u033f\u0001"+
		"\u0000\u0000\u0000\u0347\u0348\u0001\u0000\u0000\u0000\u0348\u0349\u0001"+
		"\u0000\u0000\u0000\u0349\u034a\u0005\u0005\u0000\u0000\u034ak\u0001\u0000"+
		"\u0000\u0000\u034b\u034d\u0005A\u0000\u0000\u034c\u034e\u0003\f\u0006"+
		"\u0000\u034d\u034c\u0001\u0000\u0000\u0000\u034d\u034e\u0001\u0000\u0000"+
		"\u0000\u034e\u034f\u0001\u0000\u0000\u0000\u034f\u0358\u0005\u0004\u0000"+
		"\u0000\u0350\u0355\u0003n7\u0000\u0351\u0352\u0005\u0001\u0000\u0000\u0352"+
		"\u0354\u0003n7\u0000\u0353\u0351\u0001\u0000\u0000\u0000\u0354\u0357\u0001"+
		"\u0000\u0000\u0000\u0355\u0353\u0001\u0000\u0000\u0000\u0355\u0356\u0001"+
		"\u0000\u0000\u0000\u0356\u0359\u0001\u0000\u0000\u0000\u0357\u0355\u0001"+
		"\u0000\u0000\u0000\u0358\u0350\u0001\u0000\u0000\u0000\u0358\u0359\u0001"+
		"\u0000\u0000\u0000\u0359\u035a\u0001\u0000\u0000\u0000\u035a\u035b\u0005"+
		"\u0005\u0000\u0000\u035bm\u0001\u0000\u0000\u0000\u035c\u035f\u0003p8"+
		"\u0000\u035d\u035f\u0003R)\u0000\u035e\u035c\u0001\u0000\u0000\u0000\u035e"+
		"\u035d\u0001\u0000\u0000\u0000\u035fo\u0001\u0000\u0000\u0000\u0360\u0361"+
		"\u0005A\u0000\u0000\u0361\u0362\u0005$\u0000\u0000\u0362\u036b\u0003R"+
		")\u0000\u0363\u0364\u0005\u0004\u0000\u0000\u0364\u0365\u0005A\u0000\u0000"+
		"\u0365\u0366\u0005\u0001\u0000\u0000\u0366\u0367\u0005A\u0000\u0000\u0367"+
		"\u0368\u0005\u0005\u0000\u0000\u0368\u0369\u0005$\u0000\u0000\u0369\u036b"+
		"\u0003R)\u0000\u036a\u0360\u0001\u0000\u0000\u0000\u036a\u0363\u0001\u0000"+
		"\u0000\u0000\u036bq\u0001\u0000\u0000\u0000\u036c\u036d\u0003\n\u0005"+
		"\u0000\u036d\u036e\u0005\b\u0000\u0000\u036e\u0373\u0003t:\u0000\u036f"+
		"\u0370\u0005\u0001\u0000\u0000\u0370\u0372\u0003t:\u0000\u0371\u036f\u0001"+
		"\u0000\u0000\u0000\u0372\u0375\u0001\u0000\u0000\u0000\u0373\u0371\u0001"+
		"\u0000\u0000\u0000\u0373\u0374\u0001\u0000\u0000\u0000\u0374\u0376\u0001"+
		"\u0000\u0000\u0000\u0375\u0373\u0001\u0000\u0000\u0000\u0376\u0377\u0005"+
		"\t\u0000\u0000\u0377s\u0001\u0000\u0000\u0000\u0378\u0379\u0005A\u0000"+
		"\u0000\u0379\u037a\u0005\u001c\u0000\u0000\u037a\u037b\u0003R)\u0000\u037b"+
		"u\u0001\u0000\u0000\u0000\u037c\u0381\u0005A\u0000\u0000\u037d\u037e\u0005"+
		"\u0002\u0000\u0000\u037e\u0380\u0005A\u0000\u0000\u037f\u037d\u0001\u0000"+
		"\u0000\u0000\u0380\u0383\u0001\u0000\u0000\u0000\u0381\u037f\u0001\u0000"+
		"\u0000\u0000\u0381\u0382\u0001\u0000\u0000\u0000\u0382w\u0001\u0000\u0000"+
		"\u0000\u0383\u0381\u0001\u0000\u0000\u0000Uz|\u0089\u008e\u0092\u0096"+
		"\u009b\u009f\u00a3\u00a7\u00aa\u00ae\u00b4\u00bc\u00c7\u00cf\u00e0\u00e8"+
		"\u00f0\u00f8\u00fd\u0103\u0111\u011c\u012e\u013a\u0144\u014f\u0156\u015a"+
		"\u0164\u0170\u0173\u017c\u0182\u018b\u0192\u0198\u01a5\u01ad\u01bc\u01c4"+
		"\u01dc\u01e3\u01ea\u01ee\u01f6\u01f9\u0230\u0239\u024b\u027d\u027f\u0289"+
		"\u0292\u0296\u029f\u02a6\u02b3\u02be\u02c3\u02cf\u02d2\u02df\u02ed\u02f7"+
		"\u02fa\u0305\u030a\u031a\u031d\u0323\u0325\u032d\u0335\u033c\u0344\u0347"+
		"\u034d\u0355\u0358\u035e\u036a\u0373\u0381";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}