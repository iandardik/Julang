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
		LISTOF=42, SETOF=43, MAPOF=44, TYPE=45, PROC=46, API=47, CALLS=48, COMPILE=49, 
		SPEC=50, INVARIANT=51, FORALL=52, EXISTS=53, VAR=54, CONST=55, CONSTRUCTOR=56, 
		TRANSITION=57, INTERNAL=58, PROVIDER=59, CLIENT=60, SESSION=61, GUARD=62, 
		TRANSIT=63, ERROR=64, BEFORE=65, AFTER=66, FUN=67, PROCFUN=68, RETURN=69, 
		ALSO=70, WITH=71, THIS=72, GLOBAL=73, INIT=74, REAL=75, INT=76, ID=77, 
		STRING=78, WS=79, COMMENT=80, LINE_COMMENT=81;
	public static final int
		RULE_root = 0, RULE_import_stmt = 1, RULE_qualified_name = 2, RULE_name_id = 3, 
		RULE_decl = 4, RULE_typeExpr = 5, RULE_typeDomain = 6, RULE_typeAtom = 7, 
		RULE_typeArgs = 8, RULE_typeParams = 9, RULE_fun_decl = 10, RULE_procfun_decl = 11, 
		RULE_procfun_body = 12, RULE_api_decl = 13, RULE_api_call_list = 14, RULE_proc = 15, 
		RULE_type_decl = 16, RULE_type_model = 17, RULE_compile_decl = 18, RULE_spec = 19, 
		RULE_leaf_spec_item = 20, RULE_ag_spec = 21, RULE_assume_expr = 22, RULE_system_expr = 23, 
		RULE_with_expr = 24, RULE_system_atom = 25, RULE_create_index_item = 26, 
		RULE_global_decl = 27, RULE_init_clause = 28, RULE_system_primary = 29, 
		RULE_system_leaf = 30, RULE_invariant_decl = 31, RULE_proc_body = 32, 
		RULE_field = 33, RULE_var = 34, RULE_constructor = 35, RULE_transition = 36, 
		RULE_args = 37, RULE_arg = 38, RULE_constructor_body = 39, RULE_action_body = 40, 
		RULE_return_clause = 41, RULE_guard = 42, RULE_transit = 43, RULE_error = 44, 
		RULE_error_arm = 45, RULE_var_transit = 46, RULE_before = 47, RULE_after = 48, 
		RULE_call_stmt = 49, RULE_expr = 50, RULE_when_subject_arm = 51, RULE_when_guard_arm = 52, 
		RULE_when_pattern = 53, RULE_proc_expr = 54, RULE_literal = 55, RULE_collection_literal = 56, 
		RULE_list_literal = 57, RULE_set_literal = 58, RULE_map_literal = 59, 
		RULE_map_entry = 60, RULE_index_expr = 61, RULE_method_prop_expr = 62, 
		RULE_method_call = 63, RULE_fun_call = 64, RULE_call_arg = 65, RULE_lambda_expr = 66, 
		RULE_record_literal = 67, RULE_record_field_assign = 68, RULE_field_access = 69;
	private static String[] makeRuleNames() {
		return new String[] {
			"root", "import_stmt", "qualified_name", "name_id", "decl", "typeExpr", 
			"typeDomain", "typeAtom", "typeArgs", "typeParams", "fun_decl", "procfun_decl", 
			"procfun_body", "api_decl", "api_call_list", "proc", "type_decl", "type_model", 
			"compile_decl", "spec", "leaf_spec_item", "ag_spec", "assume_expr", "system_expr", 
			"with_expr", "system_atom", "create_index_item", "global_decl", "init_clause", 
			"system_primary", "system_leaf", "invariant_decl", "proc_body", "field", 
			"var", "constructor", "transition", "args", "arg", "constructor_body", 
			"action_body", "return_clause", "guard", "transit", "error", "error_arm", 
			"var_transit", "before", "after", "call_stmt", "expr", "when_subject_arm", 
			"when_guard_arm", "when_pattern", "proc_expr", "literal", "collection_literal", 
			"list_literal", "set_literal", "map_literal", "map_entry", "index_expr", 
			"method_prop_expr", "method_call", "fun_call", "call_arg", "lambda_expr", 
			"record_literal", "record_field_assign", "field_access"
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
			"'mapOf'", "'type'", "'proc'", "'api'", "'calls'", "'compile'", "'spec'", 
			"'invariant'", "'forall'", "'exists'", "'var'", "'const'", "'constructor'", 
			"'transition'", "'internal'", "'provider'", "'client'", "'session'", 
			"'guard'", "'transit'", "'error'", "'before'", "'after'", "'fun'", "'procfun'", 
			"'return'", "'also'", "'with'", "'this'", "'global'", "'init'"
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
			"LISTOF", "SETOF", "MAPOF", "TYPE", "PROC", "API", "CALLS", "COMPILE", 
			"SPEC", "INVARIANT", "FORALL", "EXISTS", "VAR", "CONST", "CONSTRUCTOR", 
			"TRANSITION", "INTERNAL", "PROVIDER", "CLIENT", "SESSION", "GUARD", "TRANSIT", 
			"ERROR", "BEFORE", "AFTER", "FUN", "PROCFUN", "RETURN", "ALSO", "WITH", 
			"THIS", "GLOBAL", "INIT", "REAL", "INT", "ID", "STRING", "WS", "COMMENT", 
			"LINE_COMMENT"
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
			setState(144);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (((((_la - 40)) & ~0x3f) == 0 && ((1L << (_la - 40)) & 402656995L) != 0)) {
				{
				setState(142);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case IMPORT:
					{
					setState(140);
					import_stmt();
					}
					break;
				case EXPORT:
				case TYPE:
				case PROC:
				case API:
				case COMPILE:
				case SPEC:
				case INVARIANT:
				case FUN:
				case PROCFUN:
					{
					setState(141);
					decl();
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				}
				setState(146);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(147);
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
			setState(149);
			match(IMPORT);
			setState(150);
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
			setState(152);
			name_id();
			setState(155); 
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					{
					setState(153);
					match(DOT);
					setState(154);
					name_id();
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(157); 
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
			setState(159);
			_la = _input.LA(1);
			if ( !(((((_la - 42)) & ~0x3f) == 0 && ((1L << (_la - 42)) & 34360721415L) != 0)) ) {
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
		public Api_declContext api_decl() {
			return getRuleContext(Api_declContext.class,0);
		}
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
			setState(190);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,10,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(162);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==EXPORT) {
					{
					setState(161);
					match(EXPORT);
					}
				}

				setState(164);
				proc();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(166);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==EXPORT) {
					{
					setState(165);
					match(EXPORT);
					}
				}

				setState(168);
				api_decl();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(170);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==EXPORT) {
					{
					setState(169);
					match(EXPORT);
					}
				}

				setState(172);
				type_decl();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(173);
				compile_decl();
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(175);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==EXPORT) {
					{
					setState(174);
					match(EXPORT);
					}
				}

				setState(177);
				spec();
				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(179);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==EXPORT) {
					{
					setState(178);
					match(EXPORT);
					}
				}

				setState(181);
				invariant_decl();
				}
				break;
			case 7:
				enterOuterAlt(_localctx, 7);
				{
				setState(183);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==EXPORT) {
					{
					setState(182);
					match(EXPORT);
					}
				}

				setState(185);
				fun_decl();
				}
				break;
			case 8:
				enterOuterAlt(_localctx, 8);
				{
				setState(187);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==EXPORT) {
					{
					setState(186);
					match(EXPORT);
					}
				}

				setState(189);
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
		public TypeDomainContext typeDomain() {
			return getRuleContext(TypeDomainContext.class,0);
		}
		public TerminalNode PROCFUN_ARROW() { return getToken(JulayParser.PROCFUN_ARROW, 0); }
		public TypeExprContext typeExpr() {
			return getRuleContext(TypeExprContext.class,0);
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
			setState(197);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,11,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(192);
				typeDomain();
				setState(193);
				match(PROCFUN_ARROW);
				setState(194);
				typeExpr();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(196);
				typeDomain();
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
			setState(210);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,13,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(199);
				match(LPAREN);
				setState(200);
				typeExpr();
				setState(203); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(201);
					match(COMMA);
					setState(202);
					typeExpr();
					}
					}
					setState(205); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( _la==COMMA );
				setState(207);
				match(RPAREN);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(209);
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
			setState(220);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ID:
				enterOuterAlt(_localctx, 1);
				{
				setState(212);
				match(ID);
				setState(214);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==LT) {
					{
					setState(213);
					typeArgs();
					}
				}

				}
				break;
			case LPAREN:
				enterOuterAlt(_localctx, 2);
				{
				setState(216);
				match(LPAREN);
				setState(217);
				typeExpr();
				setState(218);
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
			setState(222);
			match(LT);
			setState(223);
			typeExpr();
			setState(228);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(224);
				match(COMMA);
				setState(225);
				typeExpr();
				}
				}
				setState(230);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(231);
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
			setState(233);
			match(LT);
			setState(234);
			match(ID);
			setState(239);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(235);
				match(COMMA);
				setState(236);
				match(ID);
				}
				}
				setState(241);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(242);
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
			setState(244);
			match(FUN);
			setState(245);
			match(ID);
			setState(247);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LT) {
				{
				setState(246);
				typeParams();
				}
			}

			setState(249);
			args();
			setState(250);
			match(COLON);
			setState(251);
			typeExpr();
			setState(252);
			match(EQ);
			setState(253);
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
		enterRule(_localctx, 22, RULE_procfun_decl);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(255);
			match(PROCFUN);
			setState(256);
			match(ID);
			setState(257);
			args();
			setState(258);
			match(COLON);
			setState(259);
			typeExpr();
			setState(260);
			match(LCURLY);
			setState(264);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 4593671619917905920L) != 0)) {
				{
				{
				setState(261);
				procfun_body();
				}
				}
				setState(266);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(267);
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
		enterRule(_localctx, 24, RULE_procfun_body);
		try {
			setState(272);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,20,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(269);
				var();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(270);
				constructor();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(271);
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
	public static class Api_declContext extends ParserRuleContext {
		public TerminalNode API() { return getToken(JulayParser.API, 0); }
		public TerminalNode ID() { return getToken(JulayParser.ID, 0); }
		public TerminalNode LCURLY() { return getToken(JulayParser.LCURLY, 0); }
		public TerminalNode PROC() { return getToken(JulayParser.PROC, 0); }
		public List<TerminalNode> COLON() { return getTokens(JulayParser.COLON); }
		public TerminalNode COLON(int i) {
			return getToken(JulayParser.COLON, i);
		}
		public Proc_exprContext proc_expr() {
			return getRuleContext(Proc_exprContext.class,0);
		}
		public TerminalNode RCURLY() { return getToken(JulayParser.RCURLY, 0); }
		public TerminalNode CALLS() { return getToken(JulayParser.CALLS, 0); }
		public Api_call_listContext api_call_list() {
			return getRuleContext(Api_call_listContext.class,0);
		}
		public Api_declContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_api_decl; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterApi_decl(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitApi_decl(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitApi_decl(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Api_declContext api_decl() throws RecognitionException {
		Api_declContext _localctx = new Api_declContext(_ctx, getState());
		enterRule(_localctx, 26, RULE_api_decl);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(274);
			match(API);
			setState(275);
			match(ID);
			setState(276);
			match(LCURLY);
			setState(277);
			match(PROC);
			setState(278);
			match(COLON);
			setState(279);
			proc_expr(0);
			setState(283);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==CALLS) {
				{
				setState(280);
				match(CALLS);
				setState(281);
				match(COLON);
				setState(282);
				api_call_list();
				}
			}

			setState(285);
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
	public static class Api_call_listContext extends ParserRuleContext {
		public List<TerminalNode> ID() { return getTokens(JulayParser.ID); }
		public TerminalNode ID(int i) {
			return getToken(JulayParser.ID, i);
		}
		public List<TerminalNode> COMMA() { return getTokens(JulayParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(JulayParser.COMMA, i);
		}
		public Api_call_listContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_api_call_list; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterApi_call_list(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitApi_call_list(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitApi_call_list(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Api_call_listContext api_call_list() throws RecognitionException {
		Api_call_listContext _localctx = new Api_call_listContext(_ctx, getState());
		enterRule(_localctx, 28, RULE_api_call_list);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(287);
			match(ID);
			setState(292);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(288);
				match(COMMA);
				setState(289);
				match(ID);
				}
				}
				setState(294);
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
	public static class ProcContext extends ParserRuleContext {
		public TerminalNode PROC() { return getToken(JulayParser.PROC, 0); }
		public TerminalNode ID() { return getToken(JulayParser.ID, 0); }
		public TerminalNode LCURLY() { return getToken(JulayParser.LCURLY, 0); }
		public TerminalNode RCURLY() { return getToken(JulayParser.RCURLY, 0); }
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
		enterRule(_localctx, 30, RULE_proc);
		int _la;
		try {
			setState(309);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,24,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(295);
				match(PROC);
				setState(296);
				match(ID);
				setState(297);
				match(LCURLY);
				setState(301);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 4593671619917905920L) != 0)) {
					{
					{
					setState(298);
					proc_body();
					}
					}
					setState(303);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(304);
				match(RCURLY);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(305);
				match(PROC);
				setState(306);
				match(ID);
				setState(307);
				match(ASGN_EQ);
				setState(308);
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
		enterRule(_localctx, 32, RULE_type_decl);
		int _la;
		try {
			setState(330);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,27,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(311);
				match(TYPE);
				setState(312);
				match(ID);
				setState(314);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==LT) {
					{
					setState(313);
					typeParams();
					}
				}

				setState(316);
				match(LCURLY);
				setState(320);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==ID) {
					{
					{
					setState(317);
					field();
					}
					}
					setState(322);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(323);
				match(RCURLY);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(324);
				match(TYPE);
				setState(325);
				match(ID);
				setState(326);
				match(ASGN_EQ);
				setState(327);
				typeExpr();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(328);
				match(TYPE);
				setState(329);
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
		enterRule(_localctx, 34, RULE_type_model);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(332);
			match(ID);
			setState(333);
			match(ASGN_EQ);
			setState(334);
			match(LCURLY);
			setState(335);
			literal();
			setState(340);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(336);
				match(COMMA);
				setState(337);
				literal();
				}
				}
				setState(342);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(343);
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
		enterRule(_localctx, 36, RULE_compile_decl);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(345);
			match(COMPILE);
			setState(346);
			match(ID);
			setState(351);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(347);
				match(COMMA);
				setState(348);
				match(ID);
				}
				}
				setState(353);
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
		enterRule(_localctx, 38, RULE_spec);
		int _la;
		try {
			setState(387);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,32,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(354);
				match(SPEC);
				setState(355);
				match(ID);
				setState(362);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==LBRACK) {
					{
					setState(356);
					match(LBRACK);
					setState(357);
					match(ID);
					setState(358);
					match(COLON);
					setState(359);
					typeExpr();
					setState(360);
					match(RBRACK);
					}
				}

				setState(364);
				match(LCURLY);
				setState(368);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (((((_la - 54)) & ~0x3f) == 0 && ((1L << (_la - 54)) & 8388863L) != 0)) {
					{
					{
					setState(365);
					leaf_spec_item();
					}
					}
					setState(370);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(371);
				match(RCURLY);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(372);
				match(SPEC);
				setState(373);
				match(ID);
				setState(374);
				match(ASGN_EQ);
				setState(375);
				ag_spec();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(376);
				match(SPEC);
				setState(377);
				match(ID);
				setState(378);
				match(ASGN_EQ);
				setState(379);
				system_expr(0);
				setState(380);
				match(MODELS);
				setState(381);
				expr(0);
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(383);
				match(SPEC);
				setState(384);
				match(ID);
				setState(385);
				match(ASGN_EQ);
				setState(386);
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
		enterRule(_localctx, 40, RULE_leaf_spec_item);
		try {
			setState(391);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case VAR:
			case CONST:
			case CONSTRUCTOR:
			case TRANSITION:
			case INTERNAL:
			case PROVIDER:
			case CLIENT:
			case SESSION:
				enterOuterAlt(_localctx, 1);
				{
				setState(389);
				proc_body();
				}
				break;
			case ID:
				enterOuterAlt(_localctx, 2);
				{
				setState(390);
				type_model();
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
		enterRule(_localctx, 42, RULE_ag_spec);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(393);
			match(LT);
			setState(394);
			assume_expr();
			setState(395);
			match(GT);
			setState(396);
			system_expr(0);
			setState(397);
			match(LT);
			setState(398);
			expr(0);
			setState(399);
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
		enterRule(_localctx, 44, RULE_assume_expr);
		try {
			setState(403);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case TRUE:
				enterOuterAlt(_localctx, 1);
				{
				setState(401);
				match(TRUE);
				}
				break;
			case LPAREN:
			case LISTOF:
			case SETOF:
			case MAPOF:
			case INTERNAL:
			case PROVIDER:
			case CLIENT:
			case SESSION:
			case WITH:
			case ID:
				enterOuterAlt(_localctx, 2);
				{
				setState(402);
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
		int _startState = 46;
		enterRecursionRule(_localctx, 46, RULE_system_expr, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(408);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case WITH:
				{
				setState(406);
				with_expr();
				}
				break;
			case LPAREN:
			case LISTOF:
			case SETOF:
			case MAPOF:
			case INTERNAL:
			case PROVIDER:
			case CLIENT:
			case SESSION:
			case ID:
				{
				setState(407);
				system_atom();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			_ctx.stop = _input.LT(-1);
			setState(415);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,36,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					{
					_localctx = new System_exprContext(_parentctx, _parentState);
					pushNewRecursionContext(_localctx, _startState, RULE_system_expr);
					setState(410);
					if (!(precpred(_ctx, 3))) throw new FailedPredicateException(this, "precpred(_ctx, 3)");
					setState(411);
					match(PARALLEL);
					setState(412);
					system_expr(4);
					}
					} 
				}
				setState(417);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,36,_ctx);
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
		enterRule(_localctx, 48, RULE_with_expr);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(418);
			match(WITH);
			setState(419);
			match(LPAREN);
			setState(420);
			match(ID);
			setState(421);
			match(COLON);
			setState(422);
			typeExpr();
			setState(423);
			match(RPAREN);
			setState(424);
			match(LCURLY);
			setState(425);
			system_expr(0);
			setState(426);
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
		enterRule(_localctx, 50, RULE_system_atom);
		int _la;
		try {
			setState(450);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,39,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(428);
				system_primary();
				setState(429);
				match(LBRACK);
				setState(430);
				match(ID);
				setState(431);
				match(COLON);
				setState(432);
				typeExpr();
				setState(433);
				match(RBRACK);
				setState(442);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,38,_ctx) ) {
				case 1:
					{
					setState(434);
					match(LCURLY);
					setState(438);
					_errHandler.sync(this);
					_la = _input.LA(1);
					while (((((_la - 55)) & ~0x3f) == 0 && ((1L << (_la - 55)) & 4980737L) != 0)) {
						{
						{
						setState(435);
						create_index_item();
						}
						}
						setState(440);
						_errHandler.sync(this);
						_la = _input.LA(1);
					}
					setState(441);
					match(RCURLY);
					}
					break;
				}
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(444);
				system_primary();
				setState(445);
				match(LBRACK);
				setState(446);
				match(ID);
				setState(447);
				match(RBRACK);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(449);
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
		enterRule(_localctx, 52, RULE_create_index_item);
		try {
			setState(455);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case CONST:
			case GLOBAL:
				enterOuterAlt(_localctx, 1);
				{
				setState(452);
				global_decl();
				}
				break;
			case INIT:
				enterOuterAlt(_localctx, 2);
				{
				setState(453);
				init_clause();
				}
				break;
			case ID:
				enterOuterAlt(_localctx, 3);
				{
				setState(454);
				type_model();
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
		enterRule(_localctx, 54, RULE_global_decl);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(458);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==CONST) {
				{
				setState(457);
				match(CONST);
				}
			}

			setState(460);
			match(GLOBAL);
			setState(461);
			match(ID);
			setState(466);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(462);
				match(COMMA);
				setState(463);
				match(ID);
				}
				}
				setState(468);
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
		enterRule(_localctx, 56, RULE_init_clause);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(469);
			match(INIT);
			setState(470);
			match(COLON);
			setState(471);
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
		enterRule(_localctx, 58, RULE_system_primary);
		try {
			setState(478);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case LISTOF:
			case SETOF:
			case MAPOF:
			case INTERNAL:
			case PROVIDER:
			case CLIENT:
			case SESSION:
			case ID:
				enterOuterAlt(_localctx, 1);
				{
				setState(473);
				system_leaf();
				}
				break;
			case LPAREN:
				enterOuterAlt(_localctx, 2);
				{
				setState(474);
				match(LPAREN);
				setState(475);
				system_expr(0);
				setState(476);
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
		enterRule(_localctx, 60, RULE_system_leaf);
		try {
			setState(482);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,44,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(480);
				qualified_name();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(481);
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
		enterRule(_localctx, 62, RULE_invariant_decl);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(484);
			match(INVARIANT);
			setState(485);
			match(ID);
			setState(486);
			match(ASGN_EQ);
			setState(487);
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
		enterRule(_localctx, 64, RULE_proc_body);
		try {
			setState(492);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,45,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(489);
				var();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(490);
				constructor();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(491);
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
		enterRule(_localctx, 66, RULE_field);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(494);
			match(ID);
			setState(495);
			match(COLON);
			setState(496);
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
		enterRule(_localctx, 68, RULE_var);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(498);
			_la = _input.LA(1);
			if ( !(_la==VAR || _la==CONST) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			setState(499);
			match(ID);
			setState(500);
			match(COLON);
			setState(501);
			typeExpr();
			setState(504);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ASGN_EQ) {
				{
				setState(502);
				match(ASGN_EQ);
				setState(503);
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
		public List<ArgsContext> args() {
			return getRuleContexts(ArgsContext.class);
		}
		public ArgsContext args(int i) {
			return getRuleContext(ArgsContext.class,i);
		}
		public TerminalNode LCURLY() { return getToken(JulayParser.LCURLY, 0); }
		public TerminalNode RCURLY() { return getToken(JulayParser.RCURLY, 0); }
		public TerminalNode SESSION() { return getToken(JulayParser.SESSION, 0); }
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
		enterRule(_localctx, 70, RULE_constructor);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(507);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==SESSION) {
				{
				setState(506);
				match(SESSION);
				}
			}

			setState(509);
			match(CONSTRUCTOR);
			setState(510);
			match(ID);
			setState(511);
			args();
			setState(514);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ALSO) {
				{
				setState(512);
				match(ALSO);
				setState(513);
				args();
				}
			}

			setState(516);
			match(LCURLY);
			setState(520);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (((((_la - 63)) & ~0x3f) == 0 && ((1L << (_la - 63)) & 15L) != 0)) {
				{
				{
				setState(517);
				constructor_body();
				}
				}
				setState(522);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(523);
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
		enterRule(_localctx, 72, RULE_transition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(526);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 4323455642275676160L) != 0)) {
				{
				setState(525);
				_la = _input.LA(1);
				if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 4323455642275676160L) != 0)) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
			}

			setState(528);
			match(TRANSITION);
			setState(529);
			match(ID);
			setState(530);
			args();
			setState(533);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ALSO) {
				{
				setState(531);
				match(ALSO);
				setState(532);
				args();
				}
			}

			setState(535);
			match(LCURLY);
			setState(539);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (((((_la - 62)) & ~0x3f) == 0 && ((1L << (_la - 62)) & 159L) != 0)) {
				{
				{
				setState(536);
				action_body();
				}
				}
				setState(541);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(542);
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
		enterRule(_localctx, 74, RULE_args);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(544);
			match(LPAREN);
			setState(546);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ID) {
				{
				setState(545);
				arg();
				}
			}

			setState(552);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(548);
				match(COMMA);
				setState(549);
				arg();
				}
				}
				setState(554);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(555);
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
		enterRule(_localctx, 76, RULE_arg);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(557);
			match(ID);
			setState(558);
			match(COLON);
			setState(559);
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
		enterRule(_localctx, 78, RULE_constructor_body);
		try {
			setState(565);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case BEFORE:
				enterOuterAlt(_localctx, 1);
				{
				setState(561);
				before();
				}
				break;
			case TRANSIT:
				enterOuterAlt(_localctx, 2);
				{
				setState(562);
				transit();
				}
				break;
			case ERROR:
				enterOuterAlt(_localctx, 3);
				{
				setState(563);
				error();
				}
				break;
			case AFTER:
				enterOuterAlt(_localctx, 4);
				{
				setState(564);
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
		enterRule(_localctx, 80, RULE_action_body);
		try {
			setState(573);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case GUARD:
				enterOuterAlt(_localctx, 1);
				{
				setState(567);
				guard();
				}
				break;
			case BEFORE:
				enterOuterAlt(_localctx, 2);
				{
				setState(568);
				before();
				}
				break;
			case TRANSIT:
				enterOuterAlt(_localctx, 3);
				{
				setState(569);
				transit();
				}
				break;
			case ERROR:
				enterOuterAlt(_localctx, 4);
				{
				setState(570);
				error();
				}
				break;
			case AFTER:
				enterOuterAlt(_localctx, 5);
				{
				setState(571);
				after();
				}
				break;
			case RETURN:
				enterOuterAlt(_localctx, 6);
				{
				setState(572);
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
		enterRule(_localctx, 82, RULE_return_clause);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(575);
			match(RETURN);
			setState(576);
			match(COLON);
			setState(577);
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
		enterRule(_localctx, 84, RULE_guard);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(579);
			match(GUARD);
			setState(580);
			match(COLON);
			setState(581);
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
		enterRule(_localctx, 86, RULE_transit);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(583);
			match(TRANSIT);
			setState(584);
			match(COLON);
			setState(588);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (((((_la - 35)) & ~0x3f) == 0 && ((1L << (_la - 35)) & 4535485464577L) != 0)) {
				{
				{
				setState(585);
				var_transit();
				}
				}
				setState(590);
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
		enterRule(_localctx, 88, RULE_error);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(591);
			match(ERROR);
			setState(592);
			match(COLON);
			setState(594); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(593);
				error_arm();
				}
				}
				setState(596); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & 13541696877017104L) != 0) || ((((_la - 72)) & ~0x3f) == 0 && ((1L << (_la - 72)) & 121L) != 0) );
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
		enterRule(_localctx, 90, RULE_error_arm);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(598);
			expr(0);
			setState(599);
			match(ARROW);
			setState(600);
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
		enterRule(_localctx, 92, RULE_var_transit);
		try {
			setState(629);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,59,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(602);
				field_access();
				setState(603);
				match(ASGN_EQ);
				setState(604);
				expr(0);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(606);
				match(THIS);
				setState(607);
				match(DOT);
				setState(608);
				match(ID);
				setState(609);
				match(LBRACK);
				setState(610);
				expr(0);
				setState(611);
				match(RBRACK);
				setState(612);
				match(ASGN_EQ);
				setState(613);
				expr(0);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(615);
				match(ID);
				setState(616);
				match(LBRACK);
				setState(617);
				expr(0);
				setState(618);
				match(RBRACK);
				setState(619);
				match(ASGN_EQ);
				setState(620);
				expr(0);
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(622);
				match(LET);
				setState(623);
				match(ID);
				setState(624);
				match(COLON);
				setState(625);
				typeExpr();
				setState(626);
				match(ASGN_EQ);
				setState(627);
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
		enterRule(_localctx, 94, RULE_before);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(631);
			match(BEFORE);
			setState(632);
			match(COLON);
			setState(634); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(633);
				call_stmt();
				}
				}
				setState(636); 
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
		enterRule(_localctx, 96, RULE_after);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(638);
			match(AFTER);
			setState(639);
			match(COLON);
			setState(641); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(640);
				call_stmt();
				}
				}
				setState(643); 
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
		enterRule(_localctx, 98, RULE_call_stmt);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(645);
			match(ID);
			setState(647);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LT) {
				{
				setState(646);
				typeArgs();
				}
			}

			setState(649);
			match(LPAREN);
			setState(658);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 13541696877017104L) != 0) || ((((_la - 72)) & ~0x3f) == 0 && ((1L << (_la - 72)) & 121L) != 0)) {
				{
				setState(650);
				expr(0);
				setState(655);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(651);
					match(COMMA);
					setState(652);
					expr(0);
					}
					}
					setState(657);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
			}

			setState(660);
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
		public TerminalNode FORALL() { return getToken(JulayParser.FORALL, 0); }
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
		int _startState = 100;
		enterRecursionRule(_localctx, 100, RULE_expr, _p);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(749);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,70,_ctx) ) {
			case 1:
				{
				setState(663);
				literal();
				}
				break;
			case 2:
				{
				setState(664);
				match(LPAREN);
				setState(665);
				expr(0);
				setState(666);
				match(RPAREN);
				}
				break;
			case 3:
				{
				setState(668);
				collection_literal();
				}
				break;
			case 4:
				{
				setState(669);
				method_prop_expr();
				}
				break;
			case 5:
				{
				setState(670);
				index_expr(0);
				}
				break;
			case 6:
				{
				setState(671);
				field_access();
				}
				break;
			case 7:
				{
				setState(672);
				record_literal();
				}
				break;
			case 8:
				{
				setState(673);
				fun_call();
				}
				break;
			case 9:
				{
				setState(674);
				match(NOT);
				setState(675);
				expr(26);
				}
				break;
			case 10:
				{
				setState(676);
				match(AND);
				setState(677);
				expr(25);
				}
				break;
			case 11:
				{
				setState(678);
				match(OR);
				setState(679);
				expr(24);
				}
				break;
			case 12:
				{
				setState(680);
				match(IF);
				setState(681);
				match(LPAREN);
				setState(682);
				expr(0);
				setState(683);
				match(RPAREN);
				setState(689);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case LCURLY:
					{
					setState(684);
					match(LCURLY);
					setState(685);
					expr(0);
					setState(686);
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
					setState(688);
					expr(0);
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(691);
				match(ELSE);
				setState(697);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case LCURLY:
					{
					setState(692);
					match(LCURLY);
					setState(693);
					expr(0);
					setState(694);
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
					setState(696);
					expr(0);
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				}
				break;
			case 13:
				{
				setState(699);
				match(LET);
				setState(700);
				match(LPAREN);
				setState(701);
				match(ID);
				setState(702);
				match(COLON);
				setState(703);
				typeExpr();
				setState(704);
				match(ASGN_EQ);
				setState(705);
				expr(0);
				setState(706);
				match(RPAREN);
				setState(712);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case LCURLY:
					{
					setState(707);
					match(LCURLY);
					setState(708);
					expr(0);
					setState(709);
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
					setState(711);
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
				setState(714);
				match(WHEN);
				setState(715);
				match(LPAREN);
				setState(716);
				expr(0);
				setState(717);
				match(RPAREN);
				setState(718);
				match(LCURLY);
				setState(720); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(719);
					when_subject_arm();
					}
					}
					setState(722); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & 17179875344L) != 0) || ((((_la - 75)) & ~0x3f) == 0 && ((1L << (_la - 75)) & 15L) != 0) );
				setState(724);
				match(RCURLY);
				}
				break;
			case 15:
				{
				setState(726);
				match(WHEN);
				setState(727);
				match(LCURLY);
				setState(729); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(728);
					when_guard_arm();
					}
					}
					setState(731); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & 13541714056886288L) != 0) || ((((_la - 72)) & ~0x3f) == 0 && ((1L << (_la - 72)) & 121L) != 0) );
				setState(733);
				match(RCURLY);
				}
				break;
			case 16:
				{
				setState(735);
				match(FORALL);
				setState(736);
				match(ID);
				setState(737);
				match(COLON);
				setState(738);
				typeExpr();
				setState(739);
				match(COMMA);
				setState(740);
				expr(2);
				}
				break;
			case 17:
				{
				setState(742);
				match(EXISTS);
				setState(743);
				match(ID);
				setState(744);
				match(COLON);
				setState(745);
				typeExpr();
				setState(746);
				match(COMMA);
				setState(747);
				expr(1);
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(804);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,72,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(802);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,71,_ctx) ) {
					case 1:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(751);
						if (!(precpred(_ctx, 23))) throw new FailedPredicateException(this, "precpred(_ctx, 23)");
						setState(752);
						match(TIMES);
						setState(753);
						expr(24);
						}
						break;
					case 2:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(754);
						if (!(precpred(_ctx, 22))) throw new FailedPredicateException(this, "precpred(_ctx, 22)");
						setState(755);
						match(DIV);
						setState(756);
						expr(23);
						}
						break;
					case 3:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(757);
						if (!(precpred(_ctx, 21))) throw new FailedPredicateException(this, "precpred(_ctx, 21)");
						setState(758);
						match(MOD);
						setState(759);
						expr(22);
						}
						break;
					case 4:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(760);
						if (!(precpred(_ctx, 20))) throw new FailedPredicateException(this, "precpred(_ctx, 20)");
						setState(761);
						match(PLUS);
						setState(762);
						expr(21);
						}
						break;
					case 5:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(763);
						if (!(precpred(_ctx, 19))) throw new FailedPredicateException(this, "precpred(_ctx, 19)");
						setState(764);
						match(MINUS);
						setState(765);
						expr(20);
						}
						break;
					case 6:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(766);
						if (!(precpred(_ctx, 18))) throw new FailedPredicateException(this, "precpred(_ctx, 18)");
						setState(767);
						match(LT);
						setState(768);
						expr(19);
						}
						break;
					case 7:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(769);
						if (!(precpred(_ctx, 17))) throw new FailedPredicateException(this, "precpred(_ctx, 17)");
						setState(770);
						match(LTE);
						setState(771);
						expr(18);
						}
						break;
					case 8:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(772);
						if (!(precpred(_ctx, 16))) throw new FailedPredicateException(this, "precpred(_ctx, 16)");
						setState(773);
						match(GT);
						setState(774);
						expr(17);
						}
						break;
					case 9:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(775);
						if (!(precpred(_ctx, 15))) throw new FailedPredicateException(this, "precpred(_ctx, 15)");
						setState(776);
						match(GTE);
						setState(777);
						expr(16);
						}
						break;
					case 10:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(778);
						if (!(precpred(_ctx, 14))) throw new FailedPredicateException(this, "precpred(_ctx, 14)");
						setState(779);
						match(IN);
						setState(780);
						expr(15);
						}
						break;
					case 11:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(781);
						if (!(precpred(_ctx, 13))) throw new FailedPredicateException(this, "precpred(_ctx, 13)");
						setState(782);
						match(NIN);
						setState(783);
						expr(14);
						}
						break;
					case 12:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(784);
						if (!(precpred(_ctx, 12))) throw new FailedPredicateException(this, "precpred(_ctx, 12)");
						setState(785);
						match(EQ);
						setState(786);
						expr(13);
						}
						break;
					case 13:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(787);
						if (!(precpred(_ctx, 11))) throw new FailedPredicateException(this, "precpred(_ctx, 11)");
						setState(788);
						match(NEQ);
						setState(789);
						expr(12);
						}
						break;
					case 14:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(790);
						if (!(precpred(_ctx, 10))) throw new FailedPredicateException(this, "precpred(_ctx, 10)");
						setState(791);
						match(AND);
						setState(792);
						expr(11);
						}
						break;
					case 15:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(793);
						if (!(precpred(_ctx, 9))) throw new FailedPredicateException(this, "precpred(_ctx, 9)");
						setState(794);
						match(OR);
						setState(795);
						expr(10);
						}
						break;
					case 16:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(796);
						if (!(precpred(_ctx, 8))) throw new FailedPredicateException(this, "precpred(_ctx, 8)");
						setState(797);
						match(IMPLIES);
						setState(798);
						expr(8);
						}
						break;
					case 17:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(799);
						if (!(precpred(_ctx, 7))) throw new FailedPredicateException(this, "precpred(_ctx, 7)");
						setState(800);
						match(IFF);
						setState(801);
						expr(8);
						}
						break;
					}
					} 
				}
				setState(806);
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
		enterRule(_localctx, 102, RULE_when_subject_arm);
		try {
			setState(814);
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
				setState(807);
				when_pattern();
				setState(808);
				match(ARROW);
				setState(809);
				expr(0);
				}
				break;
			case ELSE:
				enterOuterAlt(_localctx, 2);
				{
				setState(811);
				match(ELSE);
				setState(812);
				match(ARROW);
				setState(813);
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
		enterRule(_localctx, 104, RULE_when_guard_arm);
		try {
			setState(823);
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
				setState(816);
				expr(0);
				setState(817);
				match(ARROW);
				setState(818);
				expr(0);
				}
				break;
			case ELSE:
				enterOuterAlt(_localctx, 2);
				{
				setState(820);
				match(ELSE);
				setState(821);
				match(ARROW);
				setState(822);
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
		enterRule(_localctx, 106, RULE_when_pattern);
		try {
			setState(827);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case TRUE:
			case FALSE:
			case REAL:
			case INT:
			case STRING:
				enterOuterAlt(_localctx, 1);
				{
				setState(825);
				literal();
				}
				break;
			case LPAREN:
			case ID:
				enterOuterAlt(_localctx, 2);
				{
				setState(826);
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
		int _startState = 108;
		enterRecursionRule(_localctx, 108, RULE_proc_expr, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(836);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,76,_ctx) ) {
			case 1:
				{
				setState(830);
				qualified_name();
				}
				break;
			case 2:
				{
				setState(831);
				match(ID);
				}
				break;
			case 3:
				{
				setState(832);
				match(LPAREN);
				setState(833);
				proc_expr(0);
				setState(834);
				match(RPAREN);
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(843);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,77,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					{
					_localctx = new Proc_exprContext(_parentctx, _parentState);
					pushNewRecursionContext(_localctx, _startState, RULE_proc_expr);
					setState(838);
					if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
					setState(839);
					match(PARALLEL);
					setState(840);
					proc_expr(2);
					}
					} 
				}
				setState(845);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,77,_ctx);
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
		enterRule(_localctx, 110, RULE_literal);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(846);
			_la = _input.LA(1);
			if ( !(_la==TRUE || _la==FALSE || ((((_la - 75)) & ~0x3f) == 0 && ((1L << (_la - 75)) & 11L) != 0)) ) {
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
		enterRule(_localctx, 112, RULE_collection_literal);
		try {
			setState(851);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case LISTOF:
				enterOuterAlt(_localctx, 1);
				{
				setState(848);
				list_literal();
				}
				break;
			case SETOF:
				enterOuterAlt(_localctx, 2);
				{
				setState(849);
				set_literal();
				}
				break;
			case MAPOF:
				enterOuterAlt(_localctx, 3);
				{
				setState(850);
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
		enterRule(_localctx, 114, RULE_list_literal);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(853);
			match(LISTOF);
			setState(855);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LT) {
				{
				setState(854);
				typeArgs();
				}
			}

			setState(857);
			match(LPAREN);
			setState(866);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 13541696877017104L) != 0) || ((((_la - 72)) & ~0x3f) == 0 && ((1L << (_la - 72)) & 121L) != 0)) {
				{
				setState(858);
				expr(0);
				setState(863);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(859);
					match(COMMA);
					setState(860);
					expr(0);
					}
					}
					setState(865);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
			}

			setState(868);
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
		enterRule(_localctx, 116, RULE_set_literal);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(870);
			match(SETOF);
			setState(872);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LT) {
				{
				setState(871);
				typeArgs();
				}
			}

			setState(874);
			match(LPAREN);
			setState(883);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 13541696877017104L) != 0) || ((((_la - 72)) & ~0x3f) == 0 && ((1L << (_la - 72)) & 121L) != 0)) {
				{
				setState(875);
				expr(0);
				setState(880);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(876);
					match(COMMA);
					setState(877);
					expr(0);
					}
					}
					setState(882);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
			}

			setState(885);
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
		enterRule(_localctx, 118, RULE_map_literal);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(887);
			match(MAPOF);
			setState(889);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LT) {
				{
				setState(888);
				typeArgs();
				}
			}

			setState(891);
			match(LPAREN);
			setState(900);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 13541696877017104L) != 0) || ((((_la - 72)) & ~0x3f) == 0 && ((1L << (_la - 72)) & 121L) != 0)) {
				{
				setState(892);
				map_entry();
				setState(897);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(893);
					match(COMMA);
					setState(894);
					map_entry();
					}
					}
					setState(899);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
			}

			setState(902);
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
		enterRule(_localctx, 120, RULE_map_entry);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(904);
			expr(0);
			setState(905);
			match(TO);
			setState(906);
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
		int _startState = 122;
		enterRecursionRule(_localctx, 122, RULE_index_expr, _p);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(957);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,93,_ctx) ) {
			case 1:
				{
				setState(916);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,88,_ctx) ) {
				case 1:
					{
					setState(909);
					fun_call();
					}
					break;
				case 2:
					{
					setState(910);
					field_access();
					}
					break;
				case 3:
					{
					setState(911);
					collection_literal();
					}
					break;
				case 4:
					{
					setState(912);
					match(LPAREN);
					setState(913);
					expr(0);
					setState(914);
					match(RPAREN);
					}
					break;
				}
				setState(918);
				match(LBRACK);
				setState(919);
				expr(0);
				setState(920);
				match(RBRACK);
				}
				break;
			case 2:
				{
				setState(929);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,89,_ctx) ) {
				case 1:
					{
					setState(922);
					fun_call();
					}
					break;
				case 2:
					{
					setState(923);
					field_access();
					}
					break;
				case 3:
					{
					setState(924);
					collection_literal();
					}
					break;
				case 4:
					{
					setState(925);
					match(LPAREN);
					setState(926);
					expr(0);
					setState(927);
					match(RPAREN);
					}
					break;
				}
				setState(931);
				match(DOT);
				setState(932);
				match(ID);
				setState(933);
				match(LPAREN);
				setState(942);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 13541696877017104L) != 0) || ((((_la - 72)) & ~0x3f) == 0 && ((1L << (_la - 72)) & 121L) != 0)) {
					{
					setState(934);
					call_arg();
					setState(939);
					_errHandler.sync(this);
					_la = _input.LA(1);
					while (_la==COMMA) {
						{
						{
						setState(935);
						match(COMMA);
						setState(936);
						call_arg();
						}
						}
						setState(941);
						_errHandler.sync(this);
						_la = _input.LA(1);
					}
					}
				}

				setState(944);
				match(RPAREN);
				}
				break;
			case 3:
				{
				setState(952);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case ID:
					{
					setState(946);
					fun_call();
					}
					break;
				case LISTOF:
				case SETOF:
				case MAPOF:
					{
					setState(947);
					collection_literal();
					}
					break;
				case LPAREN:
					{
					setState(948);
					match(LPAREN);
					setState(949);
					expr(0);
					setState(950);
					match(RPAREN);
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(954);
				match(DOT);
				setState(955);
				match(ID);
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(984);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,97,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(982);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,96,_ctx) ) {
					case 1:
						{
						_localctx = new Index_exprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_index_expr);
						setState(959);
						if (!(precpred(_ctx, 6))) throw new FailedPredicateException(this, "precpred(_ctx, 6)");
						setState(960);
						match(LBRACK);
						setState(961);
						expr(0);
						setState(962);
						match(RBRACK);
						}
						break;
					case 2:
						{
						_localctx = new Index_exprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_index_expr);
						setState(964);
						if (!(precpred(_ctx, 5))) throw new FailedPredicateException(this, "precpred(_ctx, 5)");
						setState(965);
						match(DOT);
						setState(966);
						match(ID);
						setState(967);
						match(LPAREN);
						setState(976);
						_errHandler.sync(this);
						_la = _input.LA(1);
						if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 13541696877017104L) != 0) || ((((_la - 72)) & ~0x3f) == 0 && ((1L << (_la - 72)) & 121L) != 0)) {
							{
							setState(968);
							call_arg();
							setState(973);
							_errHandler.sync(this);
							_la = _input.LA(1);
							while (_la==COMMA) {
								{
								{
								setState(969);
								match(COMMA);
								setState(970);
								call_arg();
								}
								}
								setState(975);
								_errHandler.sync(this);
								_la = _input.LA(1);
							}
							}
						}

						setState(978);
						match(RPAREN);
						}
						break;
					case 3:
						{
						_localctx = new Index_exprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_index_expr);
						setState(979);
						if (!(precpred(_ctx, 4))) throw new FailedPredicateException(this, "precpred(_ctx, 4)");
						setState(980);
						match(DOT);
						setState(981);
						match(ID);
						}
						break;
					}
					} 
				}
				setState(986);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,97,_ctx);
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
		enterRule(_localctx, 124, RULE_method_prop_expr);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(987);
			method_call();
			setState(992);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,98,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(988);
					match(DOT);
					setState(989);
					match(ID);
					}
					} 
				}
				setState(994);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,98,_ctx);
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
		enterRule(_localctx, 126, RULE_method_call);
		int _la;
		try {
			setState(1033);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case THIS:
				enterOuterAlt(_localctx, 1);
				{
				setState(995);
				match(THIS);
				setState(998); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(996);
					match(DOT);
					setState(997);
					match(ID);
					}
					}
					setState(1000); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( _la==DOT );
				setState(1002);
				match(LPAREN);
				setState(1011);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 13541696877017104L) != 0) || ((((_la - 72)) & ~0x3f) == 0 && ((1L << (_la - 72)) & 121L) != 0)) {
					{
					setState(1003);
					call_arg();
					setState(1008);
					_errHandler.sync(this);
					_la = _input.LA(1);
					while (_la==COMMA) {
						{
						{
						setState(1004);
						match(COMMA);
						setState(1005);
						call_arg();
						}
						}
						setState(1010);
						_errHandler.sync(this);
						_la = _input.LA(1);
					}
					}
				}

				setState(1013);
				match(RPAREN);
				}
				break;
			case ID:
				enterOuterAlt(_localctx, 2);
				{
				setState(1014);
				match(ID);
				setState(1017); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(1015);
					match(DOT);
					setState(1016);
					match(ID);
					}
					}
					setState(1019); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( _la==DOT );
				setState(1021);
				match(LPAREN);
				setState(1030);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 13541696877017104L) != 0) || ((((_la - 72)) & ~0x3f) == 0 && ((1L << (_la - 72)) & 121L) != 0)) {
					{
					setState(1022);
					call_arg();
					setState(1027);
					_errHandler.sync(this);
					_la = _input.LA(1);
					while (_la==COMMA) {
						{
						{
						setState(1023);
						match(COMMA);
						setState(1024);
						call_arg();
						}
						}
						setState(1029);
						_errHandler.sync(this);
						_la = _input.LA(1);
					}
					}
				}

				setState(1032);
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
		enterRule(_localctx, 128, RULE_fun_call);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1035);
			match(ID);
			setState(1037);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LT) {
				{
				setState(1036);
				typeArgs();
				}
			}

			setState(1039);
			match(LPAREN);
			setState(1048);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 13541696877017104L) != 0) || ((((_la - 72)) & ~0x3f) == 0 && ((1L << (_la - 72)) & 121L) != 0)) {
				{
				setState(1040);
				call_arg();
				setState(1045);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(1041);
					match(COMMA);
					setState(1042);
					call_arg();
					}
					}
					setState(1047);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
			}

			setState(1050);
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
		enterRule(_localctx, 130, RULE_call_arg);
		try {
			setState(1054);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,109,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1052);
				lambda_expr();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1053);
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
		enterRule(_localctx, 132, RULE_lambda_expr);
		try {
			setState(1066);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ID:
				enterOuterAlt(_localctx, 1);
				{
				setState(1056);
				match(ID);
				setState(1057);
				match(ARROW);
				setState(1058);
				expr(0);
				}
				break;
			case LPAREN:
				enterOuterAlt(_localctx, 2);
				{
				setState(1059);
				match(LPAREN);
				setState(1060);
				match(ID);
				setState(1061);
				match(COMMA);
				setState(1062);
				match(ID);
				setState(1063);
				match(RPAREN);
				setState(1064);
				match(ARROW);
				setState(1065);
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
		enterRule(_localctx, 134, RULE_record_literal);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1068);
			typeExpr();
			setState(1069);
			match(LCURLY);
			setState(1070);
			record_field_assign();
			setState(1075);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(1071);
				match(COMMA);
				setState(1072);
				record_field_assign();
				}
				}
				setState(1077);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(1078);
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
		enterRule(_localctx, 136, RULE_record_field_assign);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1080);
			match(ID);
			setState(1081);
			match(ASGN_EQ);
			setState(1082);
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
		enterRule(_localctx, 138, RULE_field_access);
		try {
			int _alt;
			setState(1102);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case THIS:
				enterOuterAlt(_localctx, 1);
				{
				setState(1084);
				match(THIS);
				setState(1085);
				match(DOT);
				setState(1086);
				match(ID);
				setState(1091);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,112,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(1087);
						match(DOT);
						setState(1088);
						match(ID);
						}
						} 
					}
					setState(1093);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,112,_ctx);
				}
				}
				break;
			case ID:
				enterOuterAlt(_localctx, 2);
				{
				setState(1094);
				match(ID);
				setState(1099);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,113,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(1095);
						match(DOT);
						setState(1096);
						match(ID);
						}
						} 
					}
					setState(1101);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,113,_ctx);
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
		case 23:
			return system_expr_sempred((System_exprContext)_localctx, predIndex);
		case 50:
			return expr_sempred((ExprContext)_localctx, predIndex);
		case 54:
			return proc_expr_sempred((Proc_exprContext)_localctx, predIndex);
		case 61:
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
		"\u0004\u0001Q\u0451\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001\u0002"+
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
		"A\u0007A\u0002B\u0007B\u0002C\u0007C\u0002D\u0007D\u0002E\u0007E\u0001"+
		"\u0000\u0001\u0000\u0005\u0000\u008f\b\u0000\n\u0000\f\u0000\u0092\t\u0000"+
		"\u0001\u0000\u0001\u0000\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0002"+
		"\u0001\u0002\u0001\u0002\u0004\u0002\u009c\b\u0002\u000b\u0002\f\u0002"+
		"\u009d\u0001\u0003\u0001\u0003\u0001\u0004\u0003\u0004\u00a3\b\u0004\u0001"+
		"\u0004\u0001\u0004\u0003\u0004\u00a7\b\u0004\u0001\u0004\u0001\u0004\u0003"+
		"\u0004\u00ab\b\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0003\u0004\u00b0"+
		"\b\u0004\u0001\u0004\u0001\u0004\u0003\u0004\u00b4\b\u0004\u0001\u0004"+
		"\u0001\u0004\u0003\u0004\u00b8\b\u0004\u0001\u0004\u0001\u0004\u0003\u0004"+
		"\u00bc\b\u0004\u0001\u0004\u0003\u0004\u00bf\b\u0004\u0001\u0005\u0001"+
		"\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0003\u0005\u00c6\b\u0005\u0001"+
		"\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0004\u0006\u00cc\b\u0006\u000b"+
		"\u0006\f\u0006\u00cd\u0001\u0006\u0001\u0006\u0001\u0006\u0003\u0006\u00d3"+
		"\b\u0006\u0001\u0007\u0001\u0007\u0003\u0007\u00d7\b\u0007\u0001\u0007"+
		"\u0001\u0007\u0001\u0007\u0001\u0007\u0003\u0007\u00dd\b\u0007\u0001\b"+
		"\u0001\b\u0001\b\u0001\b\u0005\b\u00e3\b\b\n\b\f\b\u00e6\t\b\u0001\b\u0001"+
		"\b\u0001\t\u0001\t\u0001\t\u0001\t\u0005\t\u00ee\b\t\n\t\f\t\u00f1\t\t"+
		"\u0001\t\u0001\t\u0001\n\u0001\n\u0001\n\u0003\n\u00f8\b\n\u0001\n\u0001"+
		"\n\u0001\n\u0001\n\u0001\n\u0001\n\u0001\u000b\u0001\u000b\u0001\u000b"+
		"\u0001\u000b\u0001\u000b\u0001\u000b\u0001\u000b\u0005\u000b\u0107\b\u000b"+
		"\n\u000b\f\u000b\u010a\t\u000b\u0001\u000b\u0001\u000b\u0001\f\u0001\f"+
		"\u0001\f\u0003\f\u0111\b\f\u0001\r\u0001\r\u0001\r\u0001\r\u0001\r\u0001"+
		"\r\u0001\r\u0001\r\u0001\r\u0003\r\u011c\b\r\u0001\r\u0001\r\u0001\u000e"+
		"\u0001\u000e\u0001\u000e\u0005\u000e\u0123\b\u000e\n\u000e\f\u000e\u0126"+
		"\t\u000e\u0001\u000f\u0001\u000f\u0001\u000f\u0001\u000f\u0005\u000f\u012c"+
		"\b\u000f\n\u000f\f\u000f\u012f\t\u000f\u0001\u000f\u0001\u000f\u0001\u000f"+
		"\u0001\u000f\u0001\u000f\u0003\u000f\u0136\b\u000f\u0001\u0010\u0001\u0010"+
		"\u0001\u0010\u0003\u0010\u013b\b\u0010\u0001\u0010\u0001\u0010\u0005\u0010"+
		"\u013f\b\u0010\n\u0010\f\u0010\u0142\t\u0010\u0001\u0010\u0001\u0010\u0001"+
		"\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0003\u0010\u014b"+
		"\b\u0010\u0001\u0011\u0001\u0011\u0001\u0011\u0001\u0011\u0001\u0011\u0001"+
		"\u0011\u0005\u0011\u0153\b\u0011\n\u0011\f\u0011\u0156\t\u0011\u0001\u0011"+
		"\u0001\u0011\u0001\u0012\u0001\u0012\u0001\u0012\u0001\u0012\u0005\u0012"+
		"\u015e\b\u0012\n\u0012\f\u0012\u0161\t\u0012\u0001\u0013\u0001\u0013\u0001"+
		"\u0013\u0001\u0013\u0001\u0013\u0001\u0013\u0001\u0013\u0001\u0013\u0003"+
		"\u0013\u016b\b\u0013\u0001\u0013\u0001\u0013\u0005\u0013\u016f\b\u0013"+
		"\n\u0013\f\u0013\u0172\t\u0013\u0001\u0013\u0001\u0013\u0001\u0013\u0001"+
		"\u0013\u0001\u0013\u0001\u0013\u0001\u0013\u0001\u0013\u0001\u0013\u0001"+
		"\u0013\u0001\u0013\u0001\u0013\u0001\u0013\u0001\u0013\u0001\u0013\u0001"+
		"\u0013\u0003\u0013\u0184\b\u0013\u0001\u0014\u0001\u0014\u0003\u0014\u0188"+
		"\b\u0014\u0001\u0015\u0001\u0015\u0001\u0015\u0001\u0015\u0001\u0015\u0001"+
		"\u0015\u0001\u0015\u0001\u0015\u0001\u0016\u0001\u0016\u0003\u0016\u0194"+
		"\b\u0016\u0001\u0017\u0001\u0017\u0001\u0017\u0003\u0017\u0199\b\u0017"+
		"\u0001\u0017\u0001\u0017\u0001\u0017\u0005\u0017\u019e\b\u0017\n\u0017"+
		"\f\u0017\u01a1\t\u0017\u0001\u0018\u0001\u0018\u0001\u0018\u0001\u0018"+
		"\u0001\u0018\u0001\u0018\u0001\u0018\u0001\u0018\u0001\u0018\u0001\u0018"+
		"\u0001\u0019\u0001\u0019\u0001\u0019\u0001\u0019\u0001\u0019\u0001\u0019"+
		"\u0001\u0019\u0001\u0019\u0005\u0019\u01b5\b\u0019\n\u0019\f\u0019\u01b8"+
		"\t\u0019\u0001\u0019\u0003\u0019\u01bb\b\u0019\u0001\u0019\u0001\u0019"+
		"\u0001\u0019\u0001\u0019\u0001\u0019\u0001\u0019\u0003\u0019\u01c3\b\u0019"+
		"\u0001\u001a\u0001\u001a\u0001\u001a\u0003\u001a\u01c8\b\u001a\u0001\u001b"+
		"\u0003\u001b\u01cb\b\u001b\u0001\u001b\u0001\u001b\u0001\u001b\u0001\u001b"+
		"\u0005\u001b\u01d1\b\u001b\n\u001b\f\u001b\u01d4\t\u001b\u0001\u001c\u0001"+
		"\u001c\u0001\u001c\u0001\u001c\u0001\u001d\u0001\u001d\u0001\u001d\u0001"+
		"\u001d\u0001\u001d\u0003\u001d\u01df\b\u001d\u0001\u001e\u0001\u001e\u0003"+
		"\u001e\u01e3\b\u001e\u0001\u001f\u0001\u001f\u0001\u001f\u0001\u001f\u0001"+
		"\u001f\u0001 \u0001 \u0001 \u0003 \u01ed\b \u0001!\u0001!\u0001!\u0001"+
		"!\u0001\"\u0001\"\u0001\"\u0001\"\u0001\"\u0001\"\u0003\"\u01f9\b\"\u0001"+
		"#\u0003#\u01fc\b#\u0001#\u0001#\u0001#\u0001#\u0001#\u0003#\u0203\b#\u0001"+
		"#\u0001#\u0005#\u0207\b#\n#\f#\u020a\t#\u0001#\u0001#\u0001$\u0003$\u020f"+
		"\b$\u0001$\u0001$\u0001$\u0001$\u0001$\u0003$\u0216\b$\u0001$\u0001$\u0005"+
		"$\u021a\b$\n$\f$\u021d\t$\u0001$\u0001$\u0001%\u0001%\u0003%\u0223\b%"+
		"\u0001%\u0001%\u0005%\u0227\b%\n%\f%\u022a\t%\u0001%\u0001%\u0001&\u0001"+
		"&\u0001&\u0001&\u0001\'\u0001\'\u0001\'\u0001\'\u0003\'\u0236\b\'\u0001"+
		"(\u0001(\u0001(\u0001(\u0001(\u0001(\u0003(\u023e\b(\u0001)\u0001)\u0001"+
		")\u0001)\u0001*\u0001*\u0001*\u0001*\u0001+\u0001+\u0001+\u0005+\u024b"+
		"\b+\n+\f+\u024e\t+\u0001,\u0001,\u0001,\u0004,\u0253\b,\u000b,\f,\u0254"+
		"\u0001-\u0001-\u0001-\u0001-\u0001.\u0001.\u0001.\u0001.\u0001.\u0001"+
		".\u0001.\u0001.\u0001.\u0001.\u0001.\u0001.\u0001.\u0001.\u0001.\u0001"+
		".\u0001.\u0001.\u0001.\u0001.\u0001.\u0001.\u0001.\u0001.\u0001.\u0001"+
		".\u0001.\u0003.\u0276\b.\u0001/\u0001/\u0001/\u0004/\u027b\b/\u000b/\f"+
		"/\u027c\u00010\u00010\u00010\u00040\u0282\b0\u000b0\f0\u0283\u00011\u0001"+
		"1\u00031\u0288\b1\u00011\u00011\u00011\u00011\u00051\u028e\b1\n1\f1\u0291"+
		"\t1\u00031\u0293\b1\u00011\u00011\u00012\u00012\u00012\u00012\u00012\u0001"+
		"2\u00012\u00012\u00012\u00012\u00012\u00012\u00012\u00012\u00012\u0001"+
		"2\u00012\u00012\u00012\u00012\u00012\u00012\u00012\u00012\u00012\u0001"+
		"2\u00012\u00032\u02b2\b2\u00012\u00012\u00012\u00012\u00012\u00012\u0003"+
		"2\u02ba\b2\u00012\u00012\u00012\u00012\u00012\u00012\u00012\u00012\u0001"+
		"2\u00012\u00012\u00012\u00012\u00032\u02c9\b2\u00012\u00012\u00012\u0001"+
		"2\u00012\u00012\u00042\u02d1\b2\u000b2\f2\u02d2\u00012\u00012\u00012\u0001"+
		"2\u00012\u00042\u02da\b2\u000b2\f2\u02db\u00012\u00012\u00012\u00012\u0001"+
		"2\u00012\u00012\u00012\u00012\u00012\u00012\u00012\u00012\u00012\u0001"+
		"2\u00012\u00032\u02ee\b2\u00012\u00012\u00012\u00012\u00012\u00012\u0001"+
		"2\u00012\u00012\u00012\u00012\u00012\u00012\u00012\u00012\u00012\u0001"+
		"2\u00012\u00012\u00012\u00012\u00012\u00012\u00012\u00012\u00012\u0001"+
		"2\u00012\u00012\u00012\u00012\u00012\u00012\u00012\u00012\u00012\u0001"+
		"2\u00012\u00012\u00012\u00012\u00012\u00012\u00012\u00012\u00012\u0001"+
		"2\u00012\u00012\u00012\u00012\u00052\u0323\b2\n2\f2\u0326\t2\u00013\u0001"+
		"3\u00013\u00013\u00013\u00013\u00013\u00033\u032f\b3\u00014\u00014\u0001"+
		"4\u00014\u00014\u00014\u00014\u00034\u0338\b4\u00015\u00015\u00035\u033c"+
		"\b5\u00016\u00016\u00016\u00016\u00016\u00016\u00016\u00036\u0345\b6\u0001"+
		"6\u00016\u00016\u00056\u034a\b6\n6\f6\u034d\t6\u00017\u00017\u00018\u0001"+
		"8\u00018\u00038\u0354\b8\u00019\u00019\u00039\u0358\b9\u00019\u00019\u0001"+
		"9\u00019\u00059\u035e\b9\n9\f9\u0361\t9\u00039\u0363\b9\u00019\u00019"+
		"\u0001:\u0001:\u0003:\u0369\b:\u0001:\u0001:\u0001:\u0001:\u0005:\u036f"+
		"\b:\n:\f:\u0372\t:\u0003:\u0374\b:\u0001:\u0001:\u0001;\u0001;\u0003;"+
		"\u037a\b;\u0001;\u0001;\u0001;\u0001;\u0005;\u0380\b;\n;\f;\u0383\t;\u0003"+
		";\u0385\b;\u0001;\u0001;\u0001<\u0001<\u0001<\u0001<\u0001=\u0001=\u0001"+
		"=\u0001=\u0001=\u0001=\u0001=\u0001=\u0003=\u0395\b=\u0001=\u0001=\u0001"+
		"=\u0001=\u0001=\u0001=\u0001=\u0001=\u0001=\u0001=\u0001=\u0003=\u03a2"+
		"\b=\u0001=\u0001=\u0001=\u0001=\u0001=\u0001=\u0005=\u03aa\b=\n=\f=\u03ad"+
		"\t=\u0003=\u03af\b=\u0001=\u0001=\u0001=\u0001=\u0001=\u0001=\u0001=\u0001"+
		"=\u0003=\u03b9\b=\u0001=\u0001=\u0001=\u0003=\u03be\b=\u0001=\u0001=\u0001"+
		"=\u0001=\u0001=\u0001=\u0001=\u0001=\u0001=\u0001=\u0001=\u0001=\u0005"+
		"=\u03cc\b=\n=\f=\u03cf\t=\u0003=\u03d1\b=\u0001=\u0001=\u0001=\u0001="+
		"\u0005=\u03d7\b=\n=\f=\u03da\t=\u0001>\u0001>\u0001>\u0005>\u03df\b>\n"+
		">\f>\u03e2\t>\u0001?\u0001?\u0001?\u0004?\u03e7\b?\u000b?\f?\u03e8\u0001"+
		"?\u0001?\u0001?\u0001?\u0005?\u03ef\b?\n?\f?\u03f2\t?\u0003?\u03f4\b?"+
		"\u0001?\u0001?\u0001?\u0001?\u0004?\u03fa\b?\u000b?\f?\u03fb\u0001?\u0001"+
		"?\u0001?\u0001?\u0005?\u0402\b?\n?\f?\u0405\t?\u0003?\u0407\b?\u0001?"+
		"\u0003?\u040a\b?\u0001@\u0001@\u0003@\u040e\b@\u0001@\u0001@\u0001@\u0001"+
		"@\u0005@\u0414\b@\n@\f@\u0417\t@\u0003@\u0419\b@\u0001@\u0001@\u0001A"+
		"\u0001A\u0003A\u041f\bA\u0001B\u0001B\u0001B\u0001B\u0001B\u0001B\u0001"+
		"B\u0001B\u0001B\u0001B\u0003B\u042b\bB\u0001C\u0001C\u0001C\u0001C\u0001"+
		"C\u0005C\u0432\bC\nC\fC\u0435\tC\u0001C\u0001C\u0001D\u0001D\u0001D\u0001"+
		"D\u0001E\u0001E\u0001E\u0001E\u0001E\u0005E\u0442\bE\nE\fE\u0445\tE\u0001"+
		"E\u0001E\u0001E\u0005E\u044a\bE\nE\fE\u044d\tE\u0003E\u044f\bE\u0001E"+
		"\u0000\u0004.dlzF\u0000\u0002\u0004\u0006\b\n\f\u000e\u0010\u0012\u0014"+
		"\u0016\u0018\u001a\u001c\u001e \"$&(*,.02468:<>@BDFHJLNPRTVXZ\\^`bdfh"+
		"jlnprtvxz|~\u0080\u0082\u0084\u0086\u0088\u008a\u0000\u0004\u0003\u0000"+
		"*,:=MM\u0001\u000067\u0001\u0000:=\u0003\u0000\u000b\fKLNN\u04b9\u0000"+
		"\u0090\u0001\u0000\u0000\u0000\u0002\u0095\u0001\u0000\u0000\u0000\u0004"+
		"\u0098\u0001\u0000\u0000\u0000\u0006\u009f\u0001\u0000\u0000\u0000\b\u00be"+
		"\u0001\u0000\u0000\u0000\n\u00c5\u0001\u0000\u0000\u0000\f\u00d2\u0001"+
		"\u0000\u0000\u0000\u000e\u00dc\u0001\u0000\u0000\u0000\u0010\u00de\u0001"+
		"\u0000\u0000\u0000\u0012\u00e9\u0001\u0000\u0000\u0000\u0014\u00f4\u0001"+
		"\u0000\u0000\u0000\u0016\u00ff\u0001\u0000\u0000\u0000\u0018\u0110\u0001"+
		"\u0000\u0000\u0000\u001a\u0112\u0001\u0000\u0000\u0000\u001c\u011f\u0001"+
		"\u0000\u0000\u0000\u001e\u0135\u0001\u0000\u0000\u0000 \u014a\u0001\u0000"+
		"\u0000\u0000\"\u014c\u0001\u0000\u0000\u0000$\u0159\u0001\u0000\u0000"+
		"\u0000&\u0183\u0001\u0000\u0000\u0000(\u0187\u0001\u0000\u0000\u0000*"+
		"\u0189\u0001\u0000\u0000\u0000,\u0193\u0001\u0000\u0000\u0000.\u0198\u0001"+
		"\u0000\u0000\u00000\u01a2\u0001\u0000\u0000\u00002\u01c2\u0001\u0000\u0000"+
		"\u00004\u01c7\u0001\u0000\u0000\u00006\u01ca\u0001\u0000\u0000\u00008"+
		"\u01d5\u0001\u0000\u0000\u0000:\u01de\u0001\u0000\u0000\u0000<\u01e2\u0001"+
		"\u0000\u0000\u0000>\u01e4\u0001\u0000\u0000\u0000@\u01ec\u0001\u0000\u0000"+
		"\u0000B\u01ee\u0001\u0000\u0000\u0000D\u01f2\u0001\u0000\u0000\u0000F"+
		"\u01fb\u0001\u0000\u0000\u0000H\u020e\u0001\u0000\u0000\u0000J\u0220\u0001"+
		"\u0000\u0000\u0000L\u022d\u0001\u0000\u0000\u0000N\u0235\u0001\u0000\u0000"+
		"\u0000P\u023d\u0001\u0000\u0000\u0000R\u023f\u0001\u0000\u0000\u0000T"+
		"\u0243\u0001\u0000\u0000\u0000V\u0247\u0001\u0000\u0000\u0000X\u024f\u0001"+
		"\u0000\u0000\u0000Z\u0256\u0001\u0000\u0000\u0000\\\u0275\u0001\u0000"+
		"\u0000\u0000^\u0277\u0001\u0000\u0000\u0000`\u027e\u0001\u0000\u0000\u0000"+
		"b\u0285\u0001\u0000\u0000\u0000d\u02ed\u0001\u0000\u0000\u0000f\u032e"+
		"\u0001\u0000\u0000\u0000h\u0337\u0001\u0000\u0000\u0000j\u033b\u0001\u0000"+
		"\u0000\u0000l\u0344\u0001\u0000\u0000\u0000n\u034e\u0001\u0000\u0000\u0000"+
		"p\u0353\u0001\u0000\u0000\u0000r\u0355\u0001\u0000\u0000\u0000t\u0366"+
		"\u0001\u0000\u0000\u0000v\u0377\u0001\u0000\u0000\u0000x\u0388\u0001\u0000"+
		"\u0000\u0000z\u03bd\u0001\u0000\u0000\u0000|\u03db\u0001\u0000\u0000\u0000"+
		"~\u0409\u0001\u0000\u0000\u0000\u0080\u040b\u0001\u0000\u0000\u0000\u0082"+
		"\u041e\u0001\u0000\u0000\u0000\u0084\u042a\u0001\u0000\u0000\u0000\u0086"+
		"\u042c\u0001\u0000\u0000\u0000\u0088\u0438\u0001\u0000\u0000\u0000\u008a"+
		"\u044e\u0001\u0000\u0000\u0000\u008c\u008f\u0003\u0002\u0001\u0000\u008d"+
		"\u008f\u0003\b\u0004\u0000\u008e\u008c\u0001\u0000\u0000\u0000\u008e\u008d"+
		"\u0001\u0000\u0000\u0000\u008f\u0092\u0001\u0000\u0000\u0000\u0090\u008e"+
		"\u0001\u0000\u0000\u0000\u0090\u0091\u0001\u0000\u0000\u0000\u0091\u0093"+
		"\u0001\u0000\u0000\u0000\u0092\u0090\u0001\u0000\u0000\u0000\u0093\u0094"+
		"\u0005\u0000\u0000\u0001\u0094\u0001\u0001\u0000\u0000\u0000\u0095\u0096"+
		"\u0005(\u0000\u0000\u0096\u0097\u0003\u0004\u0002\u0000\u0097\u0003\u0001"+
		"\u0000\u0000\u0000\u0098\u009b\u0003\u0006\u0003\u0000\u0099\u009a\u0005"+
		"\u0002\u0000\u0000\u009a\u009c\u0003\u0006\u0003\u0000\u009b\u0099\u0001"+
		"\u0000\u0000\u0000\u009c\u009d\u0001\u0000\u0000\u0000\u009d\u009b\u0001"+
		"\u0000\u0000\u0000\u009d\u009e\u0001\u0000\u0000\u0000\u009e\u0005\u0001"+
		"\u0000\u0000\u0000\u009f\u00a0\u0007\u0000\u0000\u0000\u00a0\u0007\u0001"+
		"\u0000\u0000\u0000\u00a1\u00a3\u0005)\u0000\u0000\u00a2\u00a1\u0001\u0000"+
		"\u0000\u0000\u00a2\u00a3\u0001\u0000\u0000\u0000\u00a3\u00a4\u0001\u0000"+
		"\u0000\u0000\u00a4\u00bf\u0003\u001e\u000f\u0000\u00a5\u00a7\u0005)\u0000"+
		"\u0000\u00a6\u00a5\u0001\u0000\u0000\u0000\u00a6\u00a7\u0001\u0000\u0000"+
		"\u0000\u00a7\u00a8\u0001\u0000\u0000\u0000\u00a8\u00bf\u0003\u001a\r\u0000"+
		"\u00a9\u00ab\u0005)\u0000\u0000\u00aa\u00a9\u0001\u0000\u0000\u0000\u00aa"+
		"\u00ab\u0001\u0000\u0000\u0000\u00ab\u00ac\u0001\u0000\u0000\u0000\u00ac"+
		"\u00bf\u0003 \u0010\u0000\u00ad\u00bf\u0003$\u0012\u0000\u00ae\u00b0\u0005"+
		")\u0000\u0000\u00af\u00ae\u0001\u0000\u0000\u0000\u00af\u00b0\u0001\u0000"+
		"\u0000\u0000\u00b0\u00b1\u0001\u0000\u0000\u0000\u00b1\u00bf\u0003&\u0013"+
		"\u0000\u00b2\u00b4\u0005)\u0000\u0000\u00b3\u00b2\u0001\u0000\u0000\u0000"+
		"\u00b3\u00b4\u0001\u0000\u0000\u0000\u00b4\u00b5\u0001\u0000\u0000\u0000"+
		"\u00b5\u00bf\u0003>\u001f\u0000\u00b6\u00b8\u0005)\u0000\u0000\u00b7\u00b6"+
		"\u0001\u0000\u0000\u0000\u00b7\u00b8\u0001\u0000\u0000\u0000\u00b8\u00b9"+
		"\u0001\u0000\u0000\u0000\u00b9\u00bf\u0003\u0014\n\u0000\u00ba\u00bc\u0005"+
		")\u0000\u0000\u00bb\u00ba\u0001\u0000\u0000\u0000\u00bb\u00bc\u0001\u0000"+
		"\u0000\u0000\u00bc\u00bd\u0001\u0000\u0000\u0000\u00bd\u00bf\u0003\u0016"+
		"\u000b\u0000\u00be\u00a2\u0001\u0000\u0000\u0000\u00be\u00a6\u0001\u0000"+
		"\u0000\u0000\u00be\u00aa\u0001\u0000\u0000\u0000\u00be\u00ad\u0001\u0000"+
		"\u0000\u0000\u00be\u00af\u0001\u0000\u0000\u0000\u00be\u00b3\u0001\u0000"+
		"\u0000\u0000\u00be\u00b7\u0001\u0000\u0000\u0000\u00be\u00bb\u0001\u0000"+
		"\u0000\u0000\u00bf\t\u0001\u0000\u0000\u0000\u00c0\u00c1\u0003\f\u0006"+
		"\u0000\u00c1\u00c2\u0005\u0010\u0000\u0000\u00c2\u00c3\u0003\n\u0005\u0000"+
		"\u00c3\u00c6\u0001\u0000\u0000\u0000\u00c4\u00c6\u0003\f\u0006\u0000\u00c5"+
		"\u00c0\u0001\u0000\u0000\u0000\u00c5\u00c4\u0001\u0000\u0000\u0000\u00c6"+
		"\u000b\u0001\u0000\u0000\u0000\u00c7\u00c8\u0005\u0004\u0000\u0000\u00c8"+
		"\u00cb\u0003\n\u0005\u0000\u00c9\u00ca\u0005\u0001\u0000\u0000\u00ca\u00cc"+
		"\u0003\n\u0005\u0000\u00cb\u00c9\u0001\u0000\u0000\u0000\u00cc\u00cd\u0001"+
		"\u0000\u0000\u0000\u00cd\u00cb\u0001\u0000\u0000\u0000\u00cd\u00ce\u0001"+
		"\u0000\u0000\u0000\u00ce\u00cf\u0001\u0000\u0000\u0000\u00cf\u00d0\u0005"+
		"\u0005\u0000\u0000\u00d0\u00d3\u0001\u0000\u0000\u0000\u00d1\u00d3\u0003"+
		"\u000e\u0007\u0000\u00d2\u00c7\u0001\u0000\u0000\u0000\u00d2\u00d1\u0001"+
		"\u0000\u0000\u0000\u00d3\r\u0001\u0000\u0000\u0000\u00d4\u00d6\u0005M"+
		"\u0000\u0000\u00d5\u00d7\u0003\u0010\b\u0000\u00d6\u00d5\u0001\u0000\u0000"+
		"\u0000\u00d6\u00d7\u0001\u0000\u0000\u0000\u00d7\u00dd\u0001\u0000\u0000"+
		"\u0000\u00d8\u00d9\u0005\u0004\u0000\u0000\u00d9\u00da\u0003\n\u0005\u0000"+
		"\u00da\u00db\u0005\u0005\u0000\u0000\u00db\u00dd\u0001\u0000\u0000\u0000"+
		"\u00dc\u00d4\u0001\u0000\u0000\u0000\u00dc\u00d8\u0001\u0000\u0000\u0000"+
		"\u00dd\u000f\u0001\u0000\u0000\u0000\u00de\u00df\u0005\u0017\u0000\u0000"+
		"\u00df\u00e4\u0003\n\u0005\u0000\u00e0\u00e1\u0005\u0001\u0000\u0000\u00e1"+
		"\u00e3\u0003\n\u0005\u0000\u00e2\u00e0\u0001\u0000\u0000\u0000\u00e3\u00e6"+
		"\u0001\u0000\u0000\u0000\u00e4\u00e2\u0001\u0000\u0000\u0000\u00e4\u00e5"+
		"\u0001\u0000\u0000\u0000\u00e5\u00e7\u0001\u0000\u0000\u0000\u00e6\u00e4"+
		"\u0001\u0000\u0000\u0000\u00e7\u00e8\u0005\u0019\u0000\u0000\u00e8\u0011"+
		"\u0001\u0000\u0000\u0000\u00e9\u00ea\u0005\u0017\u0000\u0000\u00ea\u00ef"+
		"\u0005M\u0000\u0000\u00eb\u00ec\u0005\u0001\u0000\u0000\u00ec\u00ee\u0005"+
		"M\u0000\u0000\u00ed\u00eb\u0001\u0000\u0000\u0000\u00ee\u00f1\u0001\u0000"+
		"\u0000\u0000\u00ef\u00ed\u0001\u0000\u0000\u0000\u00ef\u00f0\u0001\u0000"+
		"\u0000\u0000\u00f0\u00f2\u0001\u0000\u0000\u0000\u00f1\u00ef\u0001\u0000"+
		"\u0000\u0000\u00f2\u00f3\u0005\u0019\u0000\u0000\u00f3\u0013\u0001\u0000"+
		"\u0000\u0000\u00f4\u00f5\u0005C\u0000\u0000\u00f5\u00f7\u0005M\u0000\u0000"+
		"\u00f6\u00f8\u0003\u0012\t\u0000\u00f7\u00f6\u0001\u0000\u0000\u0000\u00f7"+
		"\u00f8\u0001\u0000\u0000\u0000\u00f8\u00f9\u0001\u0000\u0000\u0000\u00f9"+
		"\u00fa\u0003J%\u0000\u00fa\u00fb\u0005\u0003\u0000\u0000\u00fb\u00fc\u0003"+
		"\n\u0005\u0000\u00fc\u00fd\u0005\u001b\u0000\u0000\u00fd\u00fe\u0003d"+
		"2\u0000\u00fe\u0015\u0001\u0000\u0000\u0000\u00ff\u0100\u0005D\u0000\u0000"+
		"\u0100\u0101\u0005M\u0000\u0000\u0101\u0102\u0003J%\u0000\u0102\u0103"+
		"\u0005\u0003\u0000\u0000\u0103\u0104\u0003\n\u0005\u0000\u0104\u0108\u0005"+
		"\b\u0000\u0000\u0105\u0107\u0003\u0018\f\u0000\u0106\u0105\u0001\u0000"+
		"\u0000\u0000\u0107\u010a\u0001\u0000\u0000\u0000\u0108\u0106\u0001\u0000"+
		"\u0000\u0000\u0108\u0109\u0001\u0000\u0000\u0000\u0109\u010b\u0001\u0000"+
		"\u0000\u0000\u010a\u0108\u0001\u0000\u0000\u0000\u010b\u010c\u0005\t\u0000"+
		"\u0000\u010c\u0017\u0001\u0000\u0000\u0000\u010d\u0111\u0003D\"\u0000"+
		"\u010e\u0111\u0003F#\u0000\u010f\u0111\u0003H$\u0000\u0110\u010d\u0001"+
		"\u0000\u0000\u0000\u0110\u010e\u0001\u0000\u0000\u0000\u0110\u010f\u0001"+
		"\u0000\u0000\u0000\u0111\u0019\u0001\u0000\u0000\u0000\u0112\u0113\u0005"+
		"/\u0000\u0000\u0113\u0114\u0005M\u0000\u0000\u0114\u0115\u0005\b\u0000"+
		"\u0000\u0115\u0116\u0005.\u0000\u0000\u0116\u0117\u0005\u0003\u0000\u0000"+
		"\u0117\u011b\u0003l6\u0000\u0118\u0119\u00050\u0000\u0000\u0119\u011a"+
		"\u0005\u0003\u0000\u0000\u011a\u011c\u0003\u001c\u000e\u0000\u011b\u0118"+
		"\u0001\u0000\u0000\u0000\u011b\u011c\u0001\u0000\u0000\u0000\u011c\u011d"+
		"\u0001\u0000\u0000\u0000\u011d\u011e\u0005\t\u0000\u0000\u011e\u001b\u0001"+
		"\u0000\u0000\u0000\u011f\u0124\u0005M\u0000\u0000\u0120\u0121\u0005\u0001"+
		"\u0000\u0000\u0121\u0123\u0005M\u0000\u0000\u0122\u0120\u0001\u0000\u0000"+
		"\u0000\u0123\u0126\u0001\u0000\u0000\u0000\u0124\u0122\u0001\u0000\u0000"+
		"\u0000\u0124\u0125\u0001\u0000\u0000\u0000\u0125\u001d\u0001\u0000\u0000"+
		"\u0000\u0126\u0124\u0001\u0000\u0000\u0000\u0127\u0128\u0005.\u0000\u0000"+
		"\u0128\u0129\u0005M\u0000\u0000\u0129\u012d\u0005\b\u0000\u0000\u012a"+
		"\u012c\u0003@ \u0000\u012b\u012a\u0001\u0000\u0000\u0000\u012c\u012f\u0001"+
		"\u0000\u0000\u0000\u012d\u012b\u0001\u0000\u0000\u0000\u012d\u012e\u0001"+
		"\u0000\u0000\u0000\u012e\u0130\u0001\u0000\u0000\u0000\u012f\u012d\u0001"+
		"\u0000\u0000\u0000\u0130\u0136\u0005\t\u0000\u0000\u0131\u0132\u0005."+
		"\u0000\u0000\u0132\u0133\u0005M\u0000\u0000\u0133\u0134\u0005\u001e\u0000"+
		"\u0000\u0134\u0136\u0003l6\u0000\u0135\u0127\u0001\u0000\u0000\u0000\u0135"+
		"\u0131\u0001\u0000\u0000\u0000\u0136\u001f\u0001\u0000\u0000\u0000\u0137"+
		"\u0138\u0005-\u0000\u0000\u0138\u013a\u0005M\u0000\u0000\u0139\u013b\u0003"+
		"\u0012\t\u0000\u013a\u0139\u0001\u0000\u0000\u0000\u013a\u013b\u0001\u0000"+
		"\u0000\u0000\u013b\u013c\u0001\u0000\u0000\u0000\u013c\u0140\u0005\b\u0000"+
		"\u0000\u013d\u013f\u0003B!\u0000\u013e\u013d\u0001\u0000\u0000\u0000\u013f"+
		"\u0142\u0001\u0000\u0000\u0000\u0140\u013e\u0001\u0000\u0000\u0000\u0140"+
		"\u0141\u0001\u0000\u0000\u0000\u0141\u0143\u0001\u0000\u0000\u0000\u0142"+
		"\u0140\u0001\u0000\u0000\u0000\u0143\u014b\u0005\t\u0000\u0000\u0144\u0145"+
		"\u0005-\u0000\u0000\u0145\u0146\u0005M\u0000\u0000\u0146\u0147\u0005\u001e"+
		"\u0000\u0000\u0147\u014b\u0003\n\u0005\u0000\u0148\u0149\u0005-\u0000"+
		"\u0000\u0149\u014b\u0005M\u0000\u0000\u014a\u0137\u0001\u0000\u0000\u0000"+
		"\u014a\u0144\u0001\u0000\u0000\u0000\u014a\u0148\u0001\u0000\u0000\u0000"+
		"\u014b!\u0001\u0000\u0000\u0000\u014c\u014d\u0005M\u0000\u0000\u014d\u014e"+
		"\u0005\u001e\u0000\u0000\u014e\u014f\u0005\b\u0000\u0000\u014f\u0154\u0003"+
		"n7\u0000\u0150\u0151\u0005\u0001\u0000\u0000\u0151\u0153\u0003n7\u0000"+
		"\u0152\u0150\u0001\u0000\u0000\u0000\u0153\u0156\u0001\u0000\u0000\u0000"+
		"\u0154\u0152\u0001\u0000\u0000\u0000\u0154\u0155\u0001\u0000\u0000\u0000"+
		"\u0155\u0157\u0001\u0000\u0000\u0000\u0156\u0154\u0001\u0000\u0000\u0000"+
		"\u0157\u0158\u0005\t\u0000\u0000\u0158#\u0001\u0000\u0000\u0000\u0159"+
		"\u015a\u00051\u0000\u0000\u015a\u015f\u0005M\u0000\u0000\u015b\u015c\u0005"+
		"\u0001\u0000\u0000\u015c\u015e\u0005M\u0000\u0000\u015d\u015b\u0001\u0000"+
		"\u0000\u0000\u015e\u0161\u0001\u0000\u0000\u0000\u015f\u015d\u0001\u0000"+
		"\u0000\u0000\u015f\u0160\u0001\u0000\u0000\u0000\u0160%\u0001\u0000\u0000"+
		"\u0000\u0161\u015f\u0001\u0000\u0000\u0000\u0162\u0163\u00052\u0000\u0000"+
		"\u0163\u016a\u0005M\u0000\u0000\u0164\u0165\u0005\u0006\u0000\u0000\u0165"+
		"\u0166\u0005M\u0000\u0000\u0166\u0167\u0005\u0003\u0000\u0000\u0167\u0168"+
		"\u0003\n\u0005\u0000\u0168\u0169\u0005\u0007\u0000\u0000\u0169\u016b\u0001"+
		"\u0000\u0000\u0000\u016a\u0164\u0001\u0000\u0000\u0000\u016a\u016b\u0001"+
		"\u0000\u0000\u0000\u016b\u016c\u0001\u0000\u0000\u0000\u016c\u0170\u0005"+
		"\b\u0000\u0000\u016d\u016f\u0003(\u0014\u0000\u016e\u016d\u0001\u0000"+
		"\u0000\u0000\u016f\u0172\u0001\u0000\u0000\u0000\u0170\u016e\u0001\u0000"+
		"\u0000\u0000\u0170\u0171\u0001\u0000\u0000\u0000\u0171\u0173\u0001\u0000"+
		"\u0000\u0000\u0172\u0170\u0001\u0000\u0000\u0000\u0173\u0184\u0005\t\u0000"+
		"\u0000\u0174\u0175\u00052\u0000\u0000\u0175\u0176\u0005M\u0000\u0000\u0176"+
		"\u0177\u0005\u001e\u0000\u0000\u0177\u0184\u0003*\u0015\u0000\u0178\u0179"+
		"\u00052\u0000\u0000\u0179\u017a\u0005M\u0000\u0000\u017a\u017b\u0005\u001e"+
		"\u0000\u0000\u017b\u017c\u0003.\u0017\u0000\u017c\u017d\u0005\u000e\u0000"+
		"\u0000\u017d\u017e\u0003d2\u0000\u017e\u0184\u0001\u0000\u0000\u0000\u017f"+
		"\u0180\u00052\u0000\u0000\u0180\u0181\u0005M\u0000\u0000\u0181\u0182\u0005"+
		"\u001e\u0000\u0000\u0182\u0184\u0003.\u0017\u0000\u0183\u0162\u0001\u0000"+
		"\u0000\u0000\u0183\u0174\u0001\u0000\u0000\u0000\u0183\u0178\u0001\u0000"+
		"\u0000\u0000\u0183\u017f\u0001\u0000\u0000\u0000\u0184\'\u0001\u0000\u0000"+
		"\u0000\u0185\u0188\u0003@ \u0000\u0186\u0188\u0003\"\u0011\u0000\u0187"+
		"\u0185\u0001\u0000\u0000\u0000\u0187\u0186\u0001\u0000\u0000\u0000\u0188"+
		")\u0001\u0000\u0000\u0000\u0189\u018a\u0005\u0017\u0000\u0000\u018a\u018b"+
		"\u0003,\u0016\u0000\u018b\u018c\u0005\u0019\u0000\u0000\u018c\u018d\u0003"+
		".\u0017\u0000\u018d\u018e\u0005\u0017\u0000\u0000\u018e\u018f\u0003d2"+
		"\u0000\u018f\u0190\u0005\u0019\u0000\u0000\u0190+\u0001\u0000\u0000\u0000"+
		"\u0191\u0194\u0005\u000b\u0000\u0000\u0192\u0194\u0003.\u0017\u0000\u0193"+
		"\u0191\u0001\u0000\u0000\u0000\u0193\u0192\u0001\u0000\u0000\u0000\u0194"+
		"-\u0001\u0000\u0000\u0000\u0195\u0196\u0006\u0017\uffff\uffff\u0000\u0196"+
		"\u0199\u00030\u0018\u0000\u0197\u0199\u00032\u0019\u0000\u0198\u0195\u0001"+
		"\u0000\u0000\u0000\u0198\u0197\u0001\u0000\u0000\u0000\u0199\u019f\u0001"+
		"\u0000\u0000\u0000\u019a\u019b\n\u0003\u0000\u0000\u019b\u019c\u0005\n"+
		"\u0000\u0000\u019c\u019e\u0003.\u0017\u0004\u019d\u019a\u0001\u0000\u0000"+
		"\u0000\u019e\u01a1\u0001\u0000\u0000\u0000\u019f\u019d\u0001\u0000\u0000"+
		"\u0000\u019f\u01a0\u0001\u0000\u0000\u0000\u01a0/\u0001\u0000\u0000\u0000"+
		"\u01a1\u019f\u0001\u0000\u0000\u0000\u01a2\u01a3\u0005G\u0000\u0000\u01a3"+
		"\u01a4\u0005\u0004\u0000\u0000\u01a4\u01a5\u0005M\u0000\u0000\u01a5\u01a6"+
		"\u0005\u0003\u0000\u0000\u01a6\u01a7\u0003\n\u0005\u0000\u01a7\u01a8\u0005"+
		"\u0005\u0000\u0000\u01a8\u01a9\u0005\b\u0000\u0000\u01a9\u01aa\u0003."+
		"\u0017\u0000\u01aa\u01ab\u0005\t\u0000\u0000\u01ab1\u0001\u0000\u0000"+
		"\u0000\u01ac\u01ad\u0003:\u001d\u0000\u01ad\u01ae\u0005\u0006\u0000\u0000"+
		"\u01ae\u01af\u0005M\u0000\u0000\u01af\u01b0\u0005\u0003\u0000\u0000\u01b0"+
		"\u01b1\u0003\n\u0005\u0000\u01b1\u01ba\u0005\u0007\u0000\u0000\u01b2\u01b6"+
		"\u0005\b\u0000\u0000\u01b3\u01b5\u00034\u001a\u0000\u01b4\u01b3\u0001"+
		"\u0000\u0000\u0000\u01b5\u01b8\u0001\u0000\u0000\u0000\u01b6\u01b4\u0001"+
		"\u0000\u0000\u0000\u01b6\u01b7\u0001\u0000\u0000\u0000\u01b7\u01b9\u0001"+
		"\u0000\u0000\u0000\u01b8\u01b6\u0001\u0000\u0000\u0000\u01b9\u01bb\u0005"+
		"\t\u0000\u0000\u01ba\u01b2\u0001\u0000\u0000\u0000\u01ba\u01bb\u0001\u0000"+
		"\u0000\u0000\u01bb\u01c3\u0001\u0000\u0000\u0000\u01bc\u01bd\u0003:\u001d"+
		"\u0000\u01bd\u01be\u0005\u0006\u0000\u0000\u01be\u01bf\u0005M\u0000\u0000"+
		"\u01bf\u01c0\u0005\u0007\u0000\u0000\u01c0\u01c3\u0001\u0000\u0000\u0000"+
		"\u01c1\u01c3\u0003:\u001d\u0000\u01c2\u01ac\u0001\u0000\u0000\u0000\u01c2"+
		"\u01bc\u0001\u0000\u0000\u0000\u01c2\u01c1\u0001\u0000\u0000\u0000\u01c3"+
		"3\u0001\u0000\u0000\u0000\u01c4\u01c8\u00036\u001b\u0000\u01c5\u01c8\u0003"+
		"8\u001c\u0000\u01c6\u01c8\u0003\"\u0011\u0000\u01c7\u01c4\u0001\u0000"+
		"\u0000\u0000\u01c7\u01c5\u0001\u0000\u0000\u0000\u01c7\u01c6\u0001\u0000"+
		"\u0000\u0000\u01c85\u0001\u0000\u0000\u0000\u01c9\u01cb\u00057\u0000\u0000"+
		"\u01ca\u01c9\u0001\u0000\u0000\u0000\u01ca\u01cb\u0001\u0000\u0000\u0000"+
		"\u01cb\u01cc\u0001\u0000\u0000\u0000\u01cc\u01cd\u0005I\u0000\u0000\u01cd"+
		"\u01d2\u0005M\u0000\u0000\u01ce\u01cf\u0005\u0001\u0000\u0000\u01cf\u01d1"+
		"\u0005M\u0000\u0000\u01d0\u01ce\u0001\u0000\u0000\u0000\u01d1\u01d4\u0001"+
		"\u0000\u0000\u0000\u01d2\u01d0\u0001\u0000\u0000\u0000\u01d2\u01d3\u0001"+
		"\u0000\u0000\u0000\u01d37\u0001\u0000\u0000\u0000\u01d4\u01d2\u0001\u0000"+
		"\u0000\u0000\u01d5\u01d6\u0005J\u0000\u0000\u01d6\u01d7\u0005\u0003\u0000"+
		"\u0000\u01d7\u01d8\u0003d2\u0000\u01d89\u0001\u0000\u0000\u0000\u01d9"+
		"\u01df\u0003<\u001e\u0000\u01da\u01db\u0005\u0004\u0000\u0000\u01db\u01dc"+
		"\u0003.\u0017\u0000\u01dc\u01dd\u0005\u0005\u0000\u0000\u01dd\u01df\u0001"+
		"\u0000\u0000\u0000\u01de\u01d9\u0001\u0000\u0000\u0000\u01de\u01da\u0001"+
		"\u0000\u0000\u0000\u01df;\u0001\u0000\u0000\u0000\u01e0\u01e3\u0003\u0004"+
		"\u0002\u0000\u01e1\u01e3\u0005M\u0000\u0000\u01e2\u01e0\u0001\u0000\u0000"+
		"\u0000\u01e2\u01e1\u0001\u0000\u0000\u0000\u01e3=\u0001\u0000\u0000\u0000"+
		"\u01e4\u01e5\u00053\u0000\u0000\u01e5\u01e6\u0005M\u0000\u0000\u01e6\u01e7"+
		"\u0005\u001e\u0000\u0000\u01e7\u01e8\u0003d2\u0000\u01e8?\u0001\u0000"+
		"\u0000\u0000\u01e9\u01ed\u0003D\"\u0000\u01ea\u01ed\u0003F#\u0000\u01eb"+
		"\u01ed\u0003H$\u0000\u01ec\u01e9\u0001\u0000\u0000\u0000\u01ec\u01ea\u0001"+
		"\u0000\u0000\u0000\u01ec\u01eb\u0001\u0000\u0000\u0000\u01edA\u0001\u0000"+
		"\u0000\u0000\u01ee\u01ef\u0005M\u0000\u0000\u01ef\u01f0\u0005\u0003\u0000"+
		"\u0000\u01f0\u01f1\u0003\n\u0005\u0000\u01f1C\u0001\u0000\u0000\u0000"+
		"\u01f2\u01f3\u0007\u0001\u0000\u0000\u01f3\u01f4\u0005M\u0000\u0000\u01f4"+
		"\u01f5\u0005\u0003\u0000\u0000\u01f5\u01f8\u0003\n\u0005\u0000\u01f6\u01f7"+
		"\u0005\u001e\u0000\u0000\u01f7\u01f9\u0003d2\u0000\u01f8\u01f6\u0001\u0000"+
		"\u0000\u0000\u01f8\u01f9\u0001\u0000\u0000\u0000\u01f9E\u0001\u0000\u0000"+
		"\u0000\u01fa\u01fc\u0005=\u0000\u0000\u01fb\u01fa\u0001\u0000\u0000\u0000"+
		"\u01fb\u01fc\u0001\u0000\u0000\u0000\u01fc\u01fd\u0001\u0000\u0000\u0000"+
		"\u01fd\u01fe\u00058\u0000\u0000\u01fe\u01ff\u0005M\u0000\u0000\u01ff\u0202"+
		"\u0003J%\u0000\u0200\u0201\u0005F\u0000\u0000\u0201\u0203\u0003J%\u0000"+
		"\u0202\u0200\u0001\u0000\u0000\u0000\u0202\u0203\u0001\u0000\u0000\u0000"+
		"\u0203\u0204\u0001\u0000\u0000\u0000\u0204\u0208\u0005\b\u0000\u0000\u0205"+
		"\u0207\u0003N\'\u0000\u0206\u0205\u0001\u0000\u0000\u0000\u0207\u020a"+
		"\u0001\u0000\u0000\u0000\u0208\u0206\u0001\u0000\u0000\u0000\u0208\u0209"+
		"\u0001\u0000\u0000\u0000\u0209\u020b\u0001\u0000\u0000\u0000\u020a\u0208"+
		"\u0001\u0000\u0000\u0000\u020b\u020c\u0005\t\u0000\u0000\u020cG\u0001"+
		"\u0000\u0000\u0000\u020d\u020f\u0007\u0002\u0000\u0000\u020e\u020d\u0001"+
		"\u0000\u0000\u0000\u020e\u020f\u0001\u0000\u0000\u0000\u020f\u0210\u0001"+
		"\u0000\u0000\u0000\u0210\u0211\u00059\u0000\u0000\u0211\u0212\u0005M\u0000"+
		"\u0000\u0212\u0215\u0003J%\u0000\u0213\u0214\u0005F\u0000\u0000\u0214"+
		"\u0216\u0003J%\u0000\u0215\u0213\u0001\u0000\u0000\u0000\u0215\u0216\u0001"+
		"\u0000\u0000\u0000\u0216\u0217\u0001\u0000\u0000\u0000\u0217\u021b\u0005"+
		"\b\u0000\u0000\u0218\u021a\u0003P(\u0000\u0219\u0218\u0001\u0000\u0000"+
		"\u0000\u021a\u021d\u0001\u0000\u0000\u0000\u021b\u0219\u0001\u0000\u0000"+
		"\u0000\u021b\u021c\u0001\u0000\u0000\u0000\u021c\u021e\u0001\u0000\u0000"+
		"\u0000\u021d\u021b\u0001\u0000\u0000\u0000\u021e\u021f\u0005\t\u0000\u0000"+
		"\u021fI\u0001\u0000\u0000\u0000\u0220\u0222\u0005\u0004\u0000\u0000\u0221"+
		"\u0223\u0003L&\u0000\u0222\u0221\u0001\u0000\u0000\u0000\u0222\u0223\u0001"+
		"\u0000\u0000\u0000\u0223\u0228\u0001\u0000\u0000\u0000\u0224\u0225\u0005"+
		"\u0001\u0000\u0000\u0225\u0227\u0003L&\u0000\u0226\u0224\u0001\u0000\u0000"+
		"\u0000\u0227\u022a\u0001\u0000\u0000\u0000\u0228\u0226\u0001\u0000\u0000"+
		"\u0000\u0228\u0229\u0001\u0000\u0000\u0000\u0229\u022b\u0001\u0000\u0000"+
		"\u0000\u022a\u0228\u0001\u0000\u0000\u0000\u022b\u022c\u0005\u0005\u0000"+
		"\u0000\u022cK\u0001\u0000\u0000\u0000\u022d\u022e\u0005M\u0000\u0000\u022e"+
		"\u022f\u0005\u0003\u0000\u0000\u022f\u0230\u0003\n\u0005\u0000\u0230M"+
		"\u0001\u0000\u0000\u0000\u0231\u0236\u0003^/\u0000\u0232\u0236\u0003V"+
		"+\u0000\u0233\u0236\u0003X,\u0000\u0234\u0236\u0003`0\u0000\u0235\u0231"+
		"\u0001\u0000\u0000\u0000\u0235\u0232\u0001\u0000\u0000\u0000\u0235\u0233"+
		"\u0001\u0000\u0000\u0000\u0235\u0234\u0001\u0000\u0000\u0000\u0236O\u0001"+
		"\u0000\u0000\u0000\u0237\u023e\u0003T*\u0000\u0238\u023e\u0003^/\u0000"+
		"\u0239\u023e\u0003V+\u0000\u023a\u023e\u0003X,\u0000\u023b\u023e\u0003"+
		"`0\u0000\u023c\u023e\u0003R)\u0000\u023d\u0237\u0001\u0000\u0000\u0000"+
		"\u023d\u0238\u0001\u0000\u0000\u0000\u023d\u0239\u0001\u0000\u0000\u0000"+
		"\u023d\u023a\u0001\u0000\u0000\u0000\u023d\u023b\u0001\u0000\u0000\u0000"+
		"\u023d\u023c\u0001\u0000\u0000\u0000\u023eQ\u0001\u0000\u0000\u0000\u023f"+
		"\u0240\u0005E\u0000\u0000\u0240\u0241\u0005\u0003\u0000\u0000\u0241\u0242"+
		"\u0003d2\u0000\u0242S\u0001\u0000\u0000\u0000\u0243\u0244\u0005>\u0000"+
		"\u0000\u0244\u0245\u0005\u0003\u0000\u0000\u0245\u0246\u0003d2\u0000\u0246"+
		"U\u0001\u0000\u0000\u0000\u0247\u0248\u0005?\u0000\u0000\u0248\u024c\u0005"+
		"\u0003\u0000\u0000\u0249\u024b\u0003\\.\u0000\u024a\u0249\u0001\u0000"+
		"\u0000\u0000\u024b\u024e\u0001\u0000\u0000\u0000\u024c\u024a\u0001\u0000"+
		"\u0000\u0000\u024c\u024d\u0001\u0000\u0000\u0000\u024dW\u0001\u0000\u0000"+
		"\u0000\u024e\u024c\u0001\u0000\u0000\u0000\u024f\u0250\u0005@\u0000\u0000"+
		"\u0250\u0252\u0005\u0003\u0000\u0000\u0251\u0253\u0003Z-\u0000\u0252\u0251"+
		"\u0001\u0000\u0000\u0000\u0253\u0254\u0001\u0000\u0000\u0000\u0254\u0252"+
		"\u0001\u0000\u0000\u0000\u0254\u0255\u0001\u0000\u0000\u0000\u0255Y\u0001"+
		"\u0000\u0000\u0000\u0256\u0257\u0003d2\u0000\u0257\u0258\u0005\'\u0000"+
		"\u0000\u0258\u0259\u0003d2\u0000\u0259[\u0001\u0000\u0000\u0000\u025a"+
		"\u025b\u0003\u008aE\u0000\u025b\u025c\u0005\u001e\u0000\u0000\u025c\u025d"+
		"\u0003d2\u0000\u025d\u0276\u0001\u0000\u0000\u0000\u025e\u025f\u0005H"+
		"\u0000\u0000\u025f\u0260\u0005\u0002\u0000\u0000\u0260\u0261\u0005M\u0000"+
		"\u0000\u0261\u0262\u0005\u0006\u0000\u0000\u0262\u0263\u0003d2\u0000\u0263"+
		"\u0264\u0005\u0007\u0000\u0000\u0264\u0265\u0005\u001e\u0000\u0000\u0265"+
		"\u0266\u0003d2\u0000\u0266\u0276\u0001\u0000\u0000\u0000\u0267\u0268\u0005"+
		"M\u0000\u0000\u0268\u0269\u0005\u0006\u0000\u0000\u0269\u026a\u0003d2"+
		"\u0000\u026a\u026b\u0005\u0007\u0000\u0000\u026b\u026c\u0005\u001e\u0000"+
		"\u0000\u026c\u026d\u0003d2\u0000\u026d\u0276\u0001\u0000\u0000\u0000\u026e"+
		"\u026f\u0005#\u0000\u0000\u026f\u0270\u0005M\u0000\u0000\u0270\u0271\u0005"+
		"\u0003\u0000\u0000\u0271\u0272\u0003\n\u0005\u0000\u0272\u0273\u0005\u001e"+
		"\u0000\u0000\u0273\u0274\u0003d2\u0000\u0274\u0276\u0001\u0000\u0000\u0000"+
		"\u0275\u025a\u0001\u0000\u0000\u0000\u0275\u025e\u0001\u0000\u0000\u0000"+
		"\u0275\u0267\u0001\u0000\u0000\u0000\u0275\u026e\u0001\u0000\u0000\u0000"+
		"\u0276]\u0001\u0000\u0000\u0000\u0277\u0278\u0005A\u0000\u0000\u0278\u027a"+
		"\u0005\u0003\u0000\u0000\u0279\u027b\u0003b1\u0000\u027a\u0279\u0001\u0000"+
		"\u0000\u0000\u027b\u027c\u0001\u0000\u0000\u0000\u027c\u027a\u0001\u0000"+
		"\u0000\u0000\u027c\u027d\u0001\u0000\u0000\u0000\u027d_\u0001\u0000\u0000"+
		"\u0000\u027e\u027f\u0005B\u0000\u0000\u027f\u0281\u0005\u0003\u0000\u0000"+
		"\u0280\u0282\u0003b1\u0000\u0281\u0280\u0001\u0000\u0000\u0000\u0282\u0283"+
		"\u0001\u0000\u0000\u0000\u0283\u0281\u0001\u0000\u0000\u0000\u0283\u0284"+
		"\u0001\u0000\u0000\u0000\u0284a\u0001\u0000\u0000\u0000\u0285\u0287\u0005"+
		"M\u0000\u0000\u0286\u0288\u0003\u0010\b\u0000\u0287\u0286\u0001\u0000"+
		"\u0000\u0000\u0287\u0288\u0001\u0000\u0000\u0000\u0288\u0289\u0001\u0000"+
		"\u0000\u0000\u0289\u0292\u0005\u0004\u0000\u0000\u028a\u028f\u0003d2\u0000"+
		"\u028b\u028c\u0005\u0001\u0000\u0000\u028c\u028e\u0003d2\u0000\u028d\u028b"+
		"\u0001\u0000\u0000\u0000\u028e\u0291\u0001\u0000\u0000\u0000\u028f\u028d"+
		"\u0001\u0000\u0000\u0000\u028f\u0290\u0001\u0000\u0000\u0000\u0290\u0293"+
		"\u0001\u0000\u0000\u0000\u0291\u028f\u0001\u0000\u0000\u0000\u0292\u028a"+
		"\u0001\u0000\u0000\u0000\u0292\u0293\u0001\u0000\u0000\u0000\u0293\u0294"+
		"\u0001\u0000\u0000\u0000\u0294\u0295\u0005\u0005\u0000\u0000\u0295c\u0001"+
		"\u0000\u0000\u0000\u0296\u0297\u00062\uffff\uffff\u0000\u0297\u02ee\u0003"+
		"n7\u0000\u0298\u0299\u0005\u0004\u0000\u0000\u0299\u029a\u0003d2\u0000"+
		"\u029a\u029b\u0005\u0005\u0000\u0000\u029b\u02ee\u0001\u0000\u0000\u0000"+
		"\u029c\u02ee\u0003p8\u0000\u029d\u02ee\u0003|>\u0000\u029e\u02ee\u0003"+
		"z=\u0000\u029f\u02ee\u0003\u008aE\u0000\u02a0\u02ee\u0003\u0086C\u0000"+
		"\u02a1\u02ee\u0003\u0080@\u0000\u02a2\u02a3\u0005\u0011\u0000\u0000\u02a3"+
		"\u02ee\u0003d2\u001a\u02a4\u02a5\u0005\r\u0000\u0000\u02a5\u02ee\u0003"+
		"d2\u0019\u02a6\u02a7\u0005\u000f\u0000\u0000\u02a7\u02ee\u0003d2\u0018"+
		"\u02a8\u02a9\u0005!\u0000\u0000\u02a9\u02aa\u0005\u0004\u0000\u0000\u02aa"+
		"\u02ab\u0003d2\u0000\u02ab\u02b1\u0005\u0005\u0000\u0000\u02ac\u02ad\u0005"+
		"\b\u0000\u0000\u02ad\u02ae\u0003d2\u0000\u02ae\u02af\u0005\t\u0000\u0000"+
		"\u02af\u02b2\u0001\u0000\u0000\u0000\u02b0\u02b2\u0003d2\u0000\u02b1\u02ac"+
		"\u0001\u0000\u0000\u0000\u02b1\u02b0\u0001\u0000\u0000\u0000\u02b2\u02b3"+
		"\u0001\u0000\u0000\u0000\u02b3\u02b9\u0005\"\u0000\u0000\u02b4\u02b5\u0005"+
		"\b\u0000\u0000\u02b5\u02b6\u0003d2\u0000\u02b6\u02b7\u0005\t\u0000\u0000"+
		"\u02b7\u02ba\u0001\u0000\u0000\u0000\u02b8\u02ba\u0003d2\u0000\u02b9\u02b4"+
		"\u0001\u0000\u0000\u0000\u02b9\u02b8\u0001\u0000\u0000\u0000\u02ba\u02ee"+
		"\u0001\u0000\u0000\u0000\u02bb\u02bc\u0005#\u0000\u0000\u02bc\u02bd\u0005"+
		"\u0004\u0000\u0000\u02bd\u02be\u0005M\u0000\u0000\u02be\u02bf\u0005\u0003"+
		"\u0000\u0000\u02bf\u02c0\u0003\n\u0005\u0000\u02c0\u02c1\u0005\u001e\u0000"+
		"\u0000\u02c1\u02c2\u0003d2\u0000\u02c2\u02c8\u0005\u0005\u0000\u0000\u02c3"+
		"\u02c4\u0005\b\u0000\u0000\u02c4\u02c5\u0003d2\u0000\u02c5\u02c6\u0005"+
		"\t\u0000\u0000\u02c6\u02c9\u0001\u0000\u0000\u0000\u02c7\u02c9\u0003d"+
		"2\u0000\u02c8\u02c3\u0001\u0000\u0000\u0000\u02c8\u02c7\u0001\u0000\u0000"+
		"\u0000\u02c9\u02ee\u0001\u0000\u0000\u0000\u02ca\u02cb\u0005$\u0000\u0000"+
		"\u02cb\u02cc\u0005\u0004\u0000\u0000\u02cc\u02cd\u0003d2\u0000\u02cd\u02ce"+
		"\u0005\u0005\u0000\u0000\u02ce\u02d0\u0005\b\u0000\u0000\u02cf\u02d1\u0003"+
		"f3\u0000\u02d0\u02cf\u0001\u0000\u0000\u0000\u02d1\u02d2\u0001\u0000\u0000"+
		"\u0000\u02d2\u02d0\u0001\u0000\u0000\u0000\u02d2\u02d3\u0001\u0000\u0000"+
		"\u0000\u02d3\u02d4\u0001\u0000\u0000\u0000\u02d4\u02d5\u0005\t\u0000\u0000"+
		"\u02d5\u02ee\u0001\u0000\u0000\u0000\u02d6\u02d7\u0005$\u0000\u0000\u02d7"+
		"\u02d9\u0005\b\u0000\u0000\u02d8\u02da\u0003h4\u0000\u02d9\u02d8\u0001"+
		"\u0000\u0000\u0000\u02da\u02db\u0001\u0000\u0000\u0000\u02db\u02d9\u0001"+
		"\u0000\u0000\u0000\u02db\u02dc\u0001\u0000\u0000\u0000\u02dc\u02dd\u0001"+
		"\u0000\u0000\u0000\u02dd\u02de\u0005\t\u0000\u0000\u02de\u02ee\u0001\u0000"+
		"\u0000\u0000\u02df\u02e0\u00054\u0000\u0000\u02e0\u02e1\u0005M\u0000\u0000"+
		"\u02e1\u02e2\u0005\u0003\u0000\u0000\u02e2\u02e3\u0003\n\u0005\u0000\u02e3"+
		"\u02e4\u0005\u0001\u0000\u0000\u02e4\u02e5\u0003d2\u0002\u02e5\u02ee\u0001"+
		"\u0000\u0000\u0000\u02e6\u02e7\u00055\u0000\u0000\u02e7\u02e8\u0005M\u0000"+
		"\u0000\u02e8\u02e9\u0005\u0003\u0000\u0000\u02e9\u02ea\u0003\n\u0005\u0000"+
		"\u02ea\u02eb\u0005\u0001\u0000\u0000\u02eb\u02ec\u0003d2\u0001\u02ec\u02ee"+
		"\u0001\u0000\u0000\u0000\u02ed\u0296\u0001\u0000\u0000\u0000\u02ed\u0298"+
		"\u0001\u0000\u0000\u0000\u02ed\u029c\u0001\u0000\u0000\u0000\u02ed\u029d"+
		"\u0001\u0000\u0000\u0000\u02ed\u029e\u0001\u0000\u0000\u0000\u02ed\u029f"+
		"\u0001\u0000\u0000\u0000\u02ed\u02a0\u0001\u0000\u0000\u0000\u02ed\u02a1"+
		"\u0001\u0000\u0000\u0000\u02ed\u02a2\u0001\u0000\u0000\u0000\u02ed\u02a4"+
		"\u0001\u0000\u0000\u0000\u02ed\u02a6\u0001\u0000\u0000\u0000\u02ed\u02a8"+
		"\u0001\u0000\u0000\u0000\u02ed\u02bb\u0001\u0000\u0000\u0000\u02ed\u02ca"+
		"\u0001\u0000\u0000\u0000\u02ed\u02d6\u0001\u0000\u0000\u0000\u02ed\u02df"+
		"\u0001\u0000\u0000\u0000\u02ed\u02e6\u0001\u0000\u0000\u0000\u02ee\u0324"+
		"\u0001\u0000\u0000\u0000\u02ef\u02f0\n\u0017\u0000\u0000\u02f0\u02f1\u0005"+
		"\u0012\u0000\u0000\u02f1\u0323\u0003d2\u0018\u02f2\u02f3\n\u0016\u0000"+
		"\u0000\u02f3\u02f4\u0005\u0013\u0000\u0000\u02f4\u0323\u0003d2\u0017\u02f5"+
		"\u02f6\n\u0015\u0000\u0000\u02f6\u02f7\u0005\u0014\u0000\u0000\u02f7\u0323"+
		"\u0003d2\u0016\u02f8\u02f9\n\u0014\u0000\u0000\u02f9\u02fa\u0005\u0015"+
		"\u0000\u0000\u02fa\u0323\u0003d2\u0015\u02fb\u02fc\n\u0013\u0000\u0000"+
		"\u02fc\u02fd\u0005\u0016\u0000\u0000\u02fd\u0323\u0003d2\u0014\u02fe\u02ff"+
		"\n\u0012\u0000\u0000\u02ff\u0300\u0005\u0017\u0000\u0000\u0300\u0323\u0003"+
		"d2\u0013\u0301\u0302\n\u0011\u0000\u0000\u0302\u0303\u0005\u0018\u0000"+
		"\u0000\u0303\u0323\u0003d2\u0012\u0304\u0305\n\u0010\u0000\u0000\u0305"+
		"\u0306\u0005\u0019\u0000\u0000\u0306\u0323\u0003d2\u0011\u0307\u0308\n"+
		"\u000f\u0000\u0000\u0308\u0309\u0005\u001a\u0000\u0000\u0309\u0323\u0003"+
		"d2\u0010\u030a\u030b\n\u000e\u0000\u0000\u030b\u030c\u0005%\u0000\u0000"+
		"\u030c\u0323\u0003d2\u000f\u030d\u030e\n\r\u0000\u0000\u030e\u030f\u0005"+
		"\u001d\u0000\u0000\u030f\u0323\u0003d2\u000e\u0310\u0311\n\f\u0000\u0000"+
		"\u0311\u0312\u0005\u001b\u0000\u0000\u0312\u0323\u0003d2\r\u0313\u0314"+
		"\n\u000b\u0000\u0000\u0314\u0315\u0005\u001c\u0000\u0000\u0315\u0323\u0003"+
		"d2\f\u0316\u0317\n\n\u0000\u0000\u0317\u0318\u0005\r\u0000\u0000\u0318"+
		"\u0323\u0003d2\u000b\u0319\u031a\n\t\u0000\u0000\u031a\u031b\u0005\u000f"+
		"\u0000\u0000\u031b\u0323\u0003d2\n\u031c\u031d\n\b\u0000\u0000\u031d\u031e"+
		"\u0005\u001f\u0000\u0000\u031e\u0323\u0003d2\b\u031f\u0320\n\u0007\u0000"+
		"\u0000\u0320\u0321\u0005 \u0000\u0000\u0321\u0323\u0003d2\b\u0322\u02ef"+
		"\u0001\u0000\u0000\u0000\u0322\u02f2\u0001\u0000\u0000\u0000\u0322\u02f5"+
		"\u0001\u0000\u0000\u0000\u0322\u02f8\u0001\u0000\u0000\u0000\u0322\u02fb"+
		"\u0001\u0000\u0000\u0000\u0322\u02fe\u0001\u0000\u0000\u0000\u0322\u0301"+
		"\u0001\u0000\u0000\u0000\u0322\u0304\u0001\u0000\u0000\u0000\u0322\u0307"+
		"\u0001\u0000\u0000\u0000\u0322\u030a\u0001\u0000\u0000\u0000\u0322\u030d"+
		"\u0001\u0000\u0000\u0000\u0322\u0310\u0001\u0000\u0000\u0000\u0322\u0313"+
		"\u0001\u0000\u0000\u0000\u0322\u0316\u0001\u0000\u0000\u0000\u0322\u0319"+
		"\u0001\u0000\u0000\u0000\u0322\u031c\u0001\u0000\u0000\u0000\u0322\u031f"+
		"\u0001\u0000\u0000\u0000\u0323\u0326\u0001\u0000\u0000\u0000\u0324\u0322"+
		"\u0001\u0000\u0000\u0000\u0324\u0325\u0001\u0000\u0000\u0000\u0325e\u0001"+
		"\u0000\u0000\u0000\u0326\u0324\u0001\u0000\u0000\u0000\u0327\u0328\u0003"+
		"j5\u0000\u0328\u0329\u0005\'\u0000\u0000\u0329\u032a\u0003d2\u0000\u032a"+
		"\u032f\u0001\u0000\u0000\u0000\u032b\u032c\u0005\"\u0000\u0000\u032c\u032d"+
		"\u0005\'\u0000\u0000\u032d\u032f\u0003d2\u0000\u032e\u0327\u0001\u0000"+
		"\u0000\u0000\u032e\u032b\u0001\u0000\u0000\u0000\u032fg\u0001\u0000\u0000"+
		"\u0000\u0330\u0331\u0003d2\u0000\u0331\u0332\u0005\'\u0000\u0000\u0332"+
		"\u0333\u0003d2\u0000\u0333\u0338\u0001\u0000\u0000\u0000\u0334\u0335\u0005"+
		"\"\u0000\u0000\u0335\u0336\u0005\'\u0000\u0000\u0336\u0338\u0003d2\u0000"+
		"\u0337\u0330\u0001\u0000\u0000\u0000\u0337\u0334\u0001\u0000\u0000\u0000"+
		"\u0338i\u0001\u0000\u0000\u0000\u0339\u033c\u0003n7\u0000\u033a\u033c"+
		"\u0003\u0086C\u0000\u033b\u0339\u0001\u0000\u0000\u0000\u033b\u033a\u0001"+
		"\u0000\u0000\u0000\u033ck\u0001\u0000\u0000\u0000\u033d\u033e\u00066\uffff"+
		"\uffff\u0000\u033e\u0345\u0003\u0004\u0002\u0000\u033f\u0345\u0005M\u0000"+
		"\u0000\u0340\u0341\u0005\u0004\u0000\u0000\u0341\u0342\u0003l6\u0000\u0342"+
		"\u0343\u0005\u0005\u0000\u0000\u0343\u0345\u0001\u0000\u0000\u0000\u0344"+
		"\u033d\u0001\u0000\u0000\u0000\u0344\u033f\u0001\u0000\u0000\u0000\u0344"+
		"\u0340\u0001\u0000\u0000\u0000\u0345\u034b\u0001\u0000\u0000\u0000\u0346"+
		"\u0347\n\u0001\u0000\u0000\u0347\u0348\u0005\n\u0000\u0000\u0348\u034a"+
		"\u0003l6\u0002\u0349\u0346\u0001\u0000\u0000\u0000\u034a\u034d\u0001\u0000"+
		"\u0000\u0000\u034b\u0349\u0001\u0000\u0000\u0000\u034b\u034c\u0001\u0000"+
		"\u0000\u0000\u034cm\u0001\u0000\u0000\u0000\u034d\u034b\u0001\u0000\u0000"+
		"\u0000\u034e\u034f\u0007\u0003\u0000\u0000\u034fo\u0001\u0000\u0000\u0000"+
		"\u0350\u0354\u0003r9\u0000\u0351\u0354\u0003t:\u0000\u0352\u0354\u0003"+
		"v;\u0000\u0353\u0350\u0001\u0000\u0000\u0000\u0353\u0351\u0001\u0000\u0000"+
		"\u0000\u0353\u0352\u0001\u0000\u0000\u0000\u0354q\u0001\u0000\u0000\u0000"+
		"\u0355\u0357\u0005*\u0000\u0000\u0356\u0358\u0003\u0010\b\u0000\u0357"+
		"\u0356\u0001\u0000\u0000\u0000\u0357\u0358\u0001\u0000\u0000\u0000\u0358"+
		"\u0359\u0001\u0000\u0000\u0000\u0359\u0362\u0005\u0004\u0000\u0000\u035a"+
		"\u035f\u0003d2\u0000\u035b\u035c\u0005\u0001\u0000\u0000\u035c\u035e\u0003"+
		"d2\u0000\u035d\u035b\u0001\u0000\u0000\u0000\u035e\u0361\u0001\u0000\u0000"+
		"\u0000\u035f\u035d\u0001\u0000\u0000\u0000\u035f\u0360\u0001\u0000\u0000"+
		"\u0000\u0360\u0363\u0001\u0000\u0000\u0000\u0361\u035f\u0001\u0000\u0000"+
		"\u0000\u0362\u035a\u0001\u0000\u0000\u0000\u0362\u0363\u0001\u0000\u0000"+
		"\u0000\u0363\u0364\u0001\u0000\u0000\u0000\u0364\u0365\u0005\u0005\u0000"+
		"\u0000\u0365s\u0001\u0000\u0000\u0000\u0366\u0368\u0005+\u0000\u0000\u0367"+
		"\u0369\u0003\u0010\b\u0000\u0368\u0367\u0001\u0000\u0000\u0000\u0368\u0369"+
		"\u0001\u0000\u0000\u0000\u0369\u036a\u0001\u0000\u0000\u0000\u036a\u0373"+
		"\u0005\u0004\u0000\u0000\u036b\u0370\u0003d2\u0000\u036c\u036d\u0005\u0001"+
		"\u0000\u0000\u036d\u036f\u0003d2\u0000\u036e\u036c\u0001\u0000\u0000\u0000"+
		"\u036f\u0372\u0001\u0000\u0000\u0000\u0370\u036e\u0001\u0000\u0000\u0000"+
		"\u0370\u0371\u0001\u0000\u0000\u0000\u0371\u0374\u0001\u0000\u0000\u0000"+
		"\u0372\u0370\u0001\u0000\u0000\u0000\u0373\u036b\u0001\u0000\u0000\u0000"+
		"\u0373\u0374\u0001\u0000\u0000\u0000\u0374\u0375\u0001\u0000\u0000\u0000"+
		"\u0375\u0376\u0005\u0005\u0000\u0000\u0376u\u0001\u0000\u0000\u0000\u0377"+
		"\u0379\u0005,\u0000\u0000\u0378\u037a\u0003\u0010\b\u0000\u0379\u0378"+
		"\u0001\u0000\u0000\u0000\u0379\u037a\u0001\u0000\u0000\u0000\u037a\u037b"+
		"\u0001\u0000\u0000\u0000\u037b\u0384\u0005\u0004\u0000\u0000\u037c\u0381"+
		"\u0003x<\u0000\u037d\u037e\u0005\u0001\u0000\u0000\u037e\u0380\u0003x"+
		"<\u0000\u037f\u037d\u0001\u0000\u0000\u0000\u0380\u0383\u0001\u0000\u0000"+
		"\u0000\u0381\u037f\u0001\u0000\u0000\u0000\u0381\u0382\u0001\u0000\u0000"+
		"\u0000\u0382\u0385\u0001\u0000\u0000\u0000\u0383\u0381\u0001\u0000\u0000"+
		"\u0000\u0384\u037c\u0001\u0000\u0000\u0000\u0384\u0385\u0001\u0000\u0000"+
		"\u0000\u0385\u0386\u0001\u0000\u0000\u0000\u0386\u0387\u0005\u0005\u0000"+
		"\u0000\u0387w\u0001\u0000\u0000\u0000\u0388\u0389\u0003d2\u0000\u0389"+
		"\u038a\u0005&\u0000\u0000\u038a\u038b\u0003d2\u0000\u038by\u0001\u0000"+
		"\u0000\u0000\u038c\u0394\u0006=\uffff\uffff\u0000\u038d\u0395\u0003\u0080"+
		"@\u0000\u038e\u0395\u0003\u008aE\u0000\u038f\u0395\u0003p8\u0000\u0390"+
		"\u0391\u0005\u0004\u0000\u0000\u0391\u0392\u0003d2\u0000\u0392\u0393\u0005"+
		"\u0005\u0000\u0000\u0393\u0395\u0001\u0000\u0000\u0000\u0394\u038d\u0001"+
		"\u0000\u0000\u0000\u0394\u038e\u0001\u0000\u0000\u0000\u0394\u038f\u0001"+
		"\u0000\u0000\u0000\u0394\u0390\u0001\u0000\u0000\u0000\u0395\u0396\u0001"+
		"\u0000\u0000\u0000\u0396\u0397\u0005\u0006\u0000\u0000\u0397\u0398\u0003"+
		"d2\u0000\u0398\u0399\u0005\u0007\u0000\u0000\u0399\u03be\u0001\u0000\u0000"+
		"\u0000\u039a\u03a2\u0003\u0080@\u0000\u039b\u03a2\u0003\u008aE\u0000\u039c"+
		"\u03a2\u0003p8\u0000\u039d\u039e\u0005\u0004\u0000\u0000\u039e\u039f\u0003"+
		"d2\u0000\u039f\u03a0\u0005\u0005\u0000\u0000\u03a0\u03a2\u0001\u0000\u0000"+
		"\u0000\u03a1\u039a\u0001\u0000\u0000\u0000\u03a1\u039b\u0001\u0000\u0000"+
		"\u0000\u03a1\u039c\u0001\u0000\u0000\u0000\u03a1\u039d\u0001\u0000\u0000"+
		"\u0000\u03a2\u03a3\u0001\u0000\u0000\u0000\u03a3\u03a4\u0005\u0002\u0000"+
		"\u0000\u03a4\u03a5\u0005M\u0000\u0000\u03a5\u03ae\u0005\u0004\u0000\u0000"+
		"\u03a6\u03ab\u0003\u0082A\u0000\u03a7\u03a8\u0005\u0001\u0000\u0000\u03a8"+
		"\u03aa\u0003\u0082A\u0000\u03a9\u03a7\u0001\u0000\u0000\u0000\u03aa\u03ad"+
		"\u0001\u0000\u0000\u0000\u03ab\u03a9\u0001\u0000\u0000\u0000\u03ab\u03ac"+
		"\u0001\u0000\u0000\u0000\u03ac\u03af\u0001\u0000\u0000\u0000\u03ad\u03ab"+
		"\u0001\u0000\u0000\u0000\u03ae\u03a6\u0001\u0000\u0000\u0000\u03ae\u03af"+
		"\u0001\u0000\u0000\u0000\u03af\u03b0\u0001\u0000\u0000\u0000\u03b0\u03b1"+
		"\u0005\u0005\u0000\u0000\u03b1\u03be\u0001\u0000\u0000\u0000\u03b2\u03b9"+
		"\u0003\u0080@\u0000\u03b3\u03b9\u0003p8\u0000\u03b4\u03b5\u0005\u0004"+
		"\u0000\u0000\u03b5\u03b6\u0003d2\u0000\u03b6\u03b7\u0005\u0005\u0000\u0000"+
		"\u03b7\u03b9\u0001\u0000\u0000\u0000\u03b8\u03b2\u0001\u0000\u0000\u0000"+
		"\u03b8\u03b3\u0001\u0000\u0000\u0000\u03b8\u03b4\u0001\u0000\u0000\u0000"+
		"\u03b9\u03ba\u0001\u0000\u0000\u0000\u03ba\u03bb\u0005\u0002\u0000\u0000"+
		"\u03bb\u03bc\u0005M\u0000\u0000\u03bc\u03be\u0001\u0000\u0000\u0000\u03bd"+
		"\u038c\u0001\u0000\u0000\u0000\u03bd\u03a1\u0001\u0000\u0000\u0000\u03bd"+
		"\u03b8\u0001\u0000\u0000\u0000\u03be\u03d8\u0001\u0000\u0000\u0000\u03bf"+
		"\u03c0\n\u0006\u0000\u0000\u03c0\u03c1\u0005\u0006\u0000\u0000\u03c1\u03c2"+
		"\u0003d2\u0000\u03c2\u03c3\u0005\u0007\u0000\u0000\u03c3\u03d7\u0001\u0000"+
		"\u0000\u0000\u03c4\u03c5\n\u0005\u0000\u0000\u03c5\u03c6\u0005\u0002\u0000"+
		"\u0000\u03c6\u03c7\u0005M\u0000\u0000\u03c7\u03d0\u0005\u0004\u0000\u0000"+
		"\u03c8\u03cd\u0003\u0082A\u0000\u03c9\u03ca\u0005\u0001\u0000\u0000\u03ca"+
		"\u03cc\u0003\u0082A\u0000\u03cb\u03c9\u0001\u0000\u0000\u0000\u03cc\u03cf"+
		"\u0001\u0000\u0000\u0000\u03cd\u03cb\u0001\u0000\u0000\u0000\u03cd\u03ce"+
		"\u0001\u0000\u0000\u0000\u03ce\u03d1\u0001\u0000\u0000\u0000\u03cf\u03cd"+
		"\u0001\u0000\u0000\u0000\u03d0\u03c8\u0001\u0000\u0000\u0000\u03d0\u03d1"+
		"\u0001\u0000\u0000\u0000\u03d1\u03d2\u0001\u0000\u0000\u0000\u03d2\u03d7"+
		"\u0005\u0005\u0000\u0000\u03d3\u03d4\n\u0004\u0000\u0000\u03d4\u03d5\u0005"+
		"\u0002\u0000\u0000\u03d5\u03d7\u0005M\u0000\u0000\u03d6\u03bf\u0001\u0000"+
		"\u0000\u0000\u03d6\u03c4\u0001\u0000\u0000\u0000\u03d6\u03d3\u0001\u0000"+
		"\u0000\u0000\u03d7\u03da\u0001\u0000\u0000\u0000\u03d8\u03d6\u0001\u0000"+
		"\u0000\u0000\u03d8\u03d9\u0001\u0000\u0000\u0000\u03d9{\u0001\u0000\u0000"+
		"\u0000\u03da\u03d8\u0001\u0000\u0000\u0000\u03db\u03e0\u0003~?\u0000\u03dc"+
		"\u03dd\u0005\u0002\u0000\u0000\u03dd\u03df\u0005M\u0000\u0000\u03de\u03dc"+
		"\u0001\u0000\u0000\u0000\u03df\u03e2\u0001\u0000\u0000\u0000\u03e0\u03de"+
		"\u0001\u0000\u0000\u0000\u03e0\u03e1\u0001\u0000\u0000\u0000\u03e1}\u0001"+
		"\u0000\u0000\u0000\u03e2\u03e0\u0001\u0000\u0000\u0000\u03e3\u03e6\u0005"+
		"H\u0000\u0000\u03e4\u03e5\u0005\u0002\u0000\u0000\u03e5\u03e7\u0005M\u0000"+
		"\u0000\u03e6\u03e4\u0001\u0000\u0000\u0000\u03e7\u03e8\u0001\u0000\u0000"+
		"\u0000\u03e8\u03e6\u0001\u0000\u0000\u0000\u03e8\u03e9\u0001\u0000\u0000"+
		"\u0000\u03e9\u03ea\u0001\u0000\u0000\u0000\u03ea\u03f3\u0005\u0004\u0000"+
		"\u0000\u03eb\u03f0\u0003\u0082A\u0000\u03ec\u03ed\u0005\u0001\u0000\u0000"+
		"\u03ed\u03ef\u0003\u0082A\u0000\u03ee\u03ec\u0001\u0000\u0000\u0000\u03ef"+
		"\u03f2\u0001\u0000\u0000\u0000\u03f0\u03ee\u0001\u0000\u0000\u0000\u03f0"+
		"\u03f1\u0001\u0000\u0000\u0000\u03f1\u03f4\u0001\u0000\u0000\u0000\u03f2"+
		"\u03f0\u0001\u0000\u0000\u0000\u03f3\u03eb\u0001\u0000\u0000\u0000\u03f3"+
		"\u03f4\u0001\u0000\u0000\u0000\u03f4\u03f5\u0001\u0000\u0000\u0000\u03f5"+
		"\u040a\u0005\u0005\u0000\u0000\u03f6\u03f9\u0005M\u0000\u0000\u03f7\u03f8"+
		"\u0005\u0002\u0000\u0000\u03f8\u03fa\u0005M\u0000\u0000\u03f9\u03f7\u0001"+
		"\u0000\u0000\u0000\u03fa\u03fb\u0001\u0000\u0000\u0000\u03fb\u03f9\u0001"+
		"\u0000\u0000\u0000\u03fb\u03fc\u0001\u0000\u0000\u0000\u03fc\u03fd\u0001"+
		"\u0000\u0000\u0000\u03fd\u0406\u0005\u0004\u0000\u0000\u03fe\u0403\u0003"+
		"\u0082A\u0000\u03ff\u0400\u0005\u0001\u0000\u0000\u0400\u0402\u0003\u0082"+
		"A\u0000\u0401\u03ff\u0001\u0000\u0000\u0000\u0402\u0405\u0001\u0000\u0000"+
		"\u0000\u0403\u0401\u0001\u0000\u0000\u0000\u0403\u0404\u0001\u0000\u0000"+
		"\u0000\u0404\u0407\u0001\u0000\u0000\u0000\u0405\u0403\u0001\u0000\u0000"+
		"\u0000\u0406\u03fe\u0001\u0000\u0000\u0000\u0406\u0407\u0001\u0000\u0000"+
		"\u0000\u0407\u0408\u0001\u0000\u0000\u0000\u0408\u040a\u0005\u0005\u0000"+
		"\u0000\u0409\u03e3\u0001\u0000\u0000\u0000\u0409\u03f6\u0001\u0000\u0000"+
		"\u0000\u040a\u007f\u0001\u0000\u0000\u0000\u040b\u040d\u0005M\u0000\u0000"+
		"\u040c\u040e\u0003\u0010\b\u0000\u040d\u040c\u0001\u0000\u0000\u0000\u040d"+
		"\u040e\u0001\u0000\u0000\u0000\u040e\u040f\u0001\u0000\u0000\u0000\u040f"+
		"\u0418\u0005\u0004\u0000\u0000\u0410\u0415\u0003\u0082A\u0000\u0411\u0412"+
		"\u0005\u0001\u0000\u0000\u0412\u0414\u0003\u0082A\u0000\u0413\u0411\u0001"+
		"\u0000\u0000\u0000\u0414\u0417\u0001\u0000\u0000\u0000\u0415\u0413\u0001"+
		"\u0000\u0000\u0000\u0415\u0416\u0001\u0000\u0000\u0000\u0416\u0419\u0001"+
		"\u0000\u0000\u0000\u0417\u0415\u0001\u0000\u0000\u0000\u0418\u0410\u0001"+
		"\u0000\u0000\u0000\u0418\u0419\u0001\u0000\u0000\u0000\u0419\u041a\u0001"+
		"\u0000\u0000\u0000\u041a\u041b\u0005\u0005\u0000\u0000\u041b\u0081\u0001"+
		"\u0000\u0000\u0000\u041c\u041f\u0003\u0084B\u0000\u041d\u041f\u0003d2"+
		"\u0000\u041e\u041c\u0001\u0000\u0000\u0000\u041e\u041d\u0001\u0000\u0000"+
		"\u0000\u041f\u0083\u0001\u0000\u0000\u0000\u0420\u0421\u0005M\u0000\u0000"+
		"\u0421\u0422\u0005\'\u0000\u0000\u0422\u042b\u0003d2\u0000\u0423\u0424"+
		"\u0005\u0004\u0000\u0000\u0424\u0425\u0005M\u0000\u0000\u0425\u0426\u0005"+
		"\u0001\u0000\u0000\u0426\u0427\u0005M\u0000\u0000\u0427\u0428\u0005\u0005"+
		"\u0000\u0000\u0428\u0429\u0005\'\u0000\u0000\u0429\u042b\u0003d2\u0000"+
		"\u042a\u0420\u0001\u0000\u0000\u0000\u042a\u0423\u0001\u0000\u0000\u0000"+
		"\u042b\u0085\u0001\u0000\u0000\u0000\u042c\u042d\u0003\n\u0005\u0000\u042d"+
		"\u042e\u0005\b\u0000\u0000\u042e\u0433\u0003\u0088D\u0000\u042f\u0430"+
		"\u0005\u0001\u0000\u0000\u0430\u0432\u0003\u0088D\u0000\u0431\u042f\u0001"+
		"\u0000\u0000\u0000\u0432\u0435\u0001\u0000\u0000\u0000\u0433\u0431\u0001"+
		"\u0000\u0000\u0000\u0433\u0434\u0001\u0000\u0000\u0000\u0434\u0436\u0001"+
		"\u0000\u0000\u0000\u0435\u0433\u0001\u0000\u0000\u0000\u0436\u0437\u0005"+
		"\t\u0000\u0000\u0437\u0087\u0001\u0000\u0000\u0000\u0438\u0439\u0005M"+
		"\u0000\u0000\u0439\u043a\u0005\u001e\u0000\u0000\u043a\u043b\u0003d2\u0000"+
		"\u043b\u0089\u0001\u0000\u0000\u0000\u043c\u043d\u0005H\u0000\u0000\u043d"+
		"\u043e\u0005\u0002\u0000\u0000\u043e\u0443\u0005M\u0000\u0000\u043f\u0440"+
		"\u0005\u0002\u0000\u0000\u0440\u0442\u0005M\u0000\u0000\u0441\u043f\u0001"+
		"\u0000\u0000\u0000\u0442\u0445\u0001\u0000\u0000\u0000\u0443\u0441\u0001"+
		"\u0000\u0000\u0000\u0443\u0444\u0001\u0000\u0000\u0000\u0444\u044f\u0001"+
		"\u0000\u0000\u0000\u0445\u0443\u0001\u0000\u0000\u0000\u0446\u044b\u0005"+
		"M\u0000\u0000\u0447\u0448\u0005\u0002\u0000\u0000\u0448\u044a\u0005M\u0000"+
		"\u0000\u0449\u0447\u0001\u0000\u0000\u0000\u044a\u044d\u0001\u0000\u0000"+
		"\u0000\u044b\u0449\u0001\u0000\u0000\u0000\u044b\u044c\u0001\u0000\u0000"+
		"\u0000\u044c\u044f\u0001\u0000\u0000\u0000\u044d\u044b\u0001\u0000\u0000"+
		"\u0000\u044e\u043c\u0001\u0000\u0000\u0000\u044e\u0446\u0001\u0000\u0000"+
		"\u0000\u044f\u008b\u0001\u0000\u0000\u0000s\u008e\u0090\u009d\u00a2\u00a6"+
		"\u00aa\u00af\u00b3\u00b7\u00bb\u00be\u00c5\u00cd\u00d2\u00d6\u00dc\u00e4"+
		"\u00ef\u00f7\u0108\u0110\u011b\u0124\u012d\u0135\u013a\u0140\u014a\u0154"+
		"\u015f\u016a\u0170\u0183\u0187\u0193\u0198\u019f\u01b6\u01ba\u01c2\u01c7"+
		"\u01ca\u01d2\u01de\u01e2\u01ec\u01f8\u01fb\u0202\u0208\u020e\u0215\u021b"+
		"\u0222\u0228\u0235\u023d\u024c\u0254\u0275\u027c\u0283\u0287\u028f\u0292"+
		"\u02b1\u02b9\u02c8\u02d2\u02db\u02ed\u0322\u0324\u032e\u0337\u033b\u0344"+
		"\u034b\u0353\u0357\u035f\u0362\u0368\u0370\u0373\u0379\u0381\u0384\u0394"+
		"\u03a1\u03ab\u03ae\u03b8\u03bd\u03cd\u03d0\u03d6\u03d8\u03e0\u03e8\u03f0"+
		"\u03f3\u03fb\u0403\u0406\u0409\u040d\u0415\u0418\u041e\u042a\u0433\u0443"+
		"\u044b\u044e";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}