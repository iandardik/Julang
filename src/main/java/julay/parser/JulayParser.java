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
		SPEC=49, INVARIANT=50, FORALL=51, EXISTS=52, VAR=53, CONST=54, CONSTRUCTOR=55, 
		TRANSITION=56, INTERNAL=57, TIMER=58, SERVICE=59, CLIENT=60, GUARD=61, 
		TRANSIT=62, ERROR=63, BEFORE=64, AFTER=65, FUN=66, RETURN=67, ALSO=68, 
		WITH=69, THIS=70, GLOBAL=71, INIT=72, REAL=73, INT=74, ID=75, STRING=76, 
		WS=77, COMMENT=78, LINE_COMMENT=79;
	public static final int
		RULE_root = 0, RULE_import_stmt = 1, RULE_qualified_name = 2, RULE_name_id = 3, 
		RULE_decl = 4, RULE_typeExpr = 5, RULE_typeDomain = 6, RULE_typeAtom = 7, 
		RULE_typeArgs = 8, RULE_typeParams = 9, RULE_fun_decl = 10, RULE_proc = 11, 
		RULE_type_decl = 12, RULE_type_model = 13, RULE_fun_model = 14, RULE_compile_decl = 15, 
		RULE_spec = 16, RULE_leaf_spec_item = 17, RULE_ag_spec = 18, RULE_assume_expr = 19, 
		RULE_system_expr = 20, RULE_with_expr = 21, RULE_system_atom = 22, RULE_create_index_item = 23, 
		RULE_global_decl = 24, RULE_init_clause = 25, RULE_system_primary = 26, 
		RULE_system_leaf = 27, RULE_invariant_decl = 28, RULE_proc_body = 29, 
		RULE_field = 30, RULE_var = 31, RULE_constructor = 32, RULE_transition = 33, 
		RULE_return_action = 34, RULE_return_body = 35, RULE_args = 36, RULE_arg = 37, 
		RULE_constructor_body = 38, RULE_action_body = 39, RULE_guard = 40, RULE_transit = 41, 
		RULE_error = 42, RULE_error_arm = 43, RULE_var_transit = 44, RULE_before = 45, 
		RULE_after = 46, RULE_call_stmt = 47, RULE_expr = 48, RULE_when_subject_arm = 49, 
		RULE_when_guard_arm = 50, RULE_when_pattern = 51, RULE_proc_expr = 52, 
		RULE_handler_ref = 53, RULE_literal = 54, RULE_collection_literal = 55, 
		RULE_list_literal = 56, RULE_set_literal = 57, RULE_map_literal = 58, 
		RULE_map_entry = 59, RULE_index_expr = 60, RULE_method_prop_expr = 61, 
		RULE_method_call = 62, RULE_fun_call = 63, RULE_call_arg = 64, RULE_lambda_expr = 65, 
		RULE_record_literal = 66, RULE_record_field_assign = 67, RULE_field_access = 68;
	private static String[] makeRuleNames() {
		return new String[] {
			"root", "import_stmt", "qualified_name", "name_id", "decl", "typeExpr", 
			"typeDomain", "typeAtom", "typeArgs", "typeParams", "fun_decl", "proc", 
			"type_decl", "type_model", "fun_model", "compile_decl", "spec", "leaf_spec_item", 
			"ag_spec", "assume_expr", "system_expr", "with_expr", "system_atom", 
			"create_index_item", "global_decl", "init_clause", "system_primary", 
			"system_leaf", "invariant_decl", "proc_body", "field", "var", "constructor", 
			"transition", "return_action", "return_body", "args", "arg", "constructor_body", 
			"action_body", "guard", "transit", "error", "error_arm", "var_transit", 
			"before", "after", "call_stmt", "expr", "when_subject_arm", "when_guard_arm", 
			"when_pattern", "proc_expr", "handler_ref", "literal", "collection_literal", 
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
			"'mapOf'", "'type'", "'static'", "'proc'", "'compile'", "'spec'", "'invariant'", 
			"'forall'", "'exists'", "'var'", "'const'", "'constructor'", "'transition'", 
			"'internal'", "'timer'", "'service'", "'client'", "'guard'", "'transit'", 
			"'error'", "'before'", "'after'", "'fun'", "'return'", "'also'", "'with'", 
			"'this'", "'global'", "'init'"
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
			"INVARIANT", "FORALL", "EXISTS", "VAR", "CONST", "CONSTRUCTOR", "TRANSITION", 
			"INTERNAL", "TIMER", "SERVICE", "CLIENT", "GUARD", "TRANSIT", "ERROR", 
			"BEFORE", "AFTER", "FUN", "RETURN", "ALSO", "WITH", "THIS", "GLOBAL", 
			"INIT", "REAL", "INT", "ID", "STRING", "WS", "COMMENT", "LINE_COMMENT"
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
			setState(142);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (((((_la - 40)) & ~0x3f) == 0 && ((1L << (_la - 40)) & 67110883L) != 0)) {
				{
				setState(140);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case IMPORT:
					{
					setState(138);
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
				case FUN:
					{
					setState(139);
					decl();
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				}
				setState(144);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(145);
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
			setState(147);
			match(IMPORT);
			setState(148);
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
			setState(150);
			name_id();
			setState(153); 
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					{
					setState(151);
					match(DOT);
					setState(152);
					name_id();
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(155); 
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
			setState(157);
			_la = _input.LA(1);
			if ( !(((((_la - 42)) & ~0x3f) == 0 && ((1L << (_la - 42)) & 8590426135L) != 0)) ) {
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
			setState(180);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,8,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(160);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==EXPORT) {
					{
					setState(159);
					match(EXPORT);
					}
				}

				setState(162);
				proc();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(164);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==EXPORT) {
					{
					setState(163);
					match(EXPORT);
					}
				}

				setState(166);
				type_decl();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(167);
				compile_decl();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(169);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==EXPORT) {
					{
					setState(168);
					match(EXPORT);
					}
				}

				setState(171);
				spec();
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(173);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==EXPORT) {
					{
					setState(172);
					match(EXPORT);
					}
				}

				setState(175);
				invariant_decl();
				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(177);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==EXPORT) {
					{
					setState(176);
					match(EXPORT);
					}
				}

				setState(179);
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
			setState(182);
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
			setState(195);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,10,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(184);
				match(LPAREN);
				setState(185);
				typeExpr();
				setState(188); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(186);
					match(COMMA);
					setState(187);
					typeExpr();
					}
					}
					setState(190); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( _la==COMMA );
				setState(192);
				match(RPAREN);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(194);
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
			setState(205);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ID:
				enterOuterAlt(_localctx, 1);
				{
				setState(197);
				match(ID);
				setState(199);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==LT) {
					{
					setState(198);
					typeArgs();
					}
				}

				}
				break;
			case LPAREN:
				enterOuterAlt(_localctx, 2);
				{
				setState(201);
				match(LPAREN);
				setState(202);
				typeExpr();
				setState(203);
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
			setState(207);
			match(LT);
			setState(208);
			typeExpr();
			setState(213);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(209);
				match(COMMA);
				setState(210);
				typeExpr();
				}
				}
				setState(215);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(216);
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
			setState(218);
			match(LT);
			setState(219);
			match(ID);
			setState(224);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(220);
				match(COMMA);
				setState(221);
				match(ID);
				}
				}
				setState(226);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(227);
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
			setState(229);
			match(FUN);
			setState(230);
			match(ID);
			setState(232);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LT) {
				{
				setState(231);
				typeParams();
				}
			}

			setState(234);
			args();
			setState(235);
			match(COLON);
			setState(236);
			typeExpr();
			setState(237);
			match(EQ);
			setState(238);
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
			setState(257);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,18,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(241);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==STATIC) {
					{
					setState(240);
					match(STATIC);
					}
				}

				setState(243);
				match(PROC);
				setState(244);
				match(ID);
				setState(245);
				match(LCURLY);
				setState(249);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (((((_la - 53)) & ~0x3f) == 0 && ((1L << (_la - 53)) & 16639L) != 0)) {
					{
					{
					setState(246);
					proc_body();
					}
					}
					setState(251);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(252);
				match(RCURLY);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(253);
				match(PROC);
				setState(254);
				match(ID);
				setState(255);
				match(ASGN_EQ);
				setState(256);
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
			setState(278);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,21,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(259);
				match(TYPE);
				setState(260);
				match(ID);
				setState(262);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==LT) {
					{
					setState(261);
					typeParams();
					}
				}

				setState(264);
				match(LCURLY);
				setState(268);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==ID) {
					{
					{
					setState(265);
					field();
					}
					}
					setState(270);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(271);
				match(RCURLY);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(272);
				match(TYPE);
				setState(273);
				match(ID);
				setState(274);
				match(ASGN_EQ);
				setState(275);
				typeExpr();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(276);
				match(TYPE);
				setState(277);
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
			setState(280);
			match(ID);
			setState(281);
			match(ASGN_EQ);
			setState(282);
			match(LCURLY);
			setState(283);
			literal();
			setState(288);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(284);
				match(COMMA);
				setState(285);
				literal();
				}
				}
				setState(290);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(291);
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
			setState(293);
			match(ID);
			setState(294);
			match(ASGN_EQ);
			setState(295);
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
			setState(297);
			match(COMPILE);
			setState(298);
			match(ID);
			setState(303);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(299);
				match(COMMA);
				setState(300);
				match(ID);
				}
				}
				setState(305);
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
		enterRule(_localctx, 32, RULE_spec);
		int _la;
		try {
			setState(339);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,26,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(306);
				match(SPEC);
				setState(307);
				match(ID);
				setState(314);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==LBRACK) {
					{
					setState(308);
					match(LBRACK);
					setState(309);
					match(ID);
					setState(310);
					match(COLON);
					setState(311);
					typeExpr();
					setState(312);
					match(RBRACK);
					}
				}

				setState(316);
				match(LCURLY);
				setState(320);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (((((_la - 53)) & ~0x3f) == 0 && ((1L << (_la - 53)) & 4210943L) != 0)) {
					{
					{
					setState(317);
					leaf_spec_item();
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
				match(SPEC);
				setState(325);
				match(ID);
				setState(326);
				match(ASGN_EQ);
				setState(327);
				ag_spec();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(328);
				match(SPEC);
				setState(329);
				match(ID);
				setState(330);
				match(ASGN_EQ);
				setState(331);
				system_expr(0);
				setState(332);
				match(MODELS);
				setState(333);
				expr(0);
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(335);
				match(SPEC);
				setState(336);
				match(ID);
				setState(337);
				match(ASGN_EQ);
				setState(338);
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
			setState(344);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,27,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(341);
				proc_body();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(342);
				type_model();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(343);
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
		enterRule(_localctx, 36, RULE_ag_spec);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(346);
			match(LT);
			setState(347);
			assume_expr();
			setState(348);
			match(GT);
			setState(349);
			system_expr(0);
			setState(350);
			match(LT);
			setState(351);
			expr(0);
			setState(352);
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
		enterRule(_localctx, 38, RULE_assume_expr);
		try {
			setState(356);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case TRUE:
				enterOuterAlt(_localctx, 1);
				{
				setState(354);
				match(TRUE);
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
			case WITH:
			case ID:
				enterOuterAlt(_localctx, 2);
				{
				setState(355);
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
		int _startState = 40;
		enterRecursionRule(_localctx, 40, RULE_system_expr, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(361);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case WITH:
				{
				setState(359);
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
				setState(360);
				system_atom();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			_ctx.stop = _input.LT(-1);
			setState(368);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,30,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					{
					_localctx = new System_exprContext(_parentctx, _parentState);
					pushNewRecursionContext(_localctx, _startState, RULE_system_expr);
					setState(363);
					if (!(precpred(_ctx, 3))) throw new FailedPredicateException(this, "precpred(_ctx, 3)");
					setState(364);
					match(PARALLEL);
					setState(365);
					system_expr(4);
					}
					} 
				}
				setState(370);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,30,_ctx);
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
		enterRule(_localctx, 42, RULE_with_expr);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(371);
			match(WITH);
			setState(372);
			match(LPAREN);
			setState(373);
			match(ID);
			setState(374);
			match(COLON);
			setState(375);
			typeExpr();
			setState(376);
			match(RPAREN);
			setState(377);
			match(LCURLY);
			setState(378);
			system_expr(0);
			setState(379);
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
		enterRule(_localctx, 44, RULE_system_atom);
		int _la;
		try {
			setState(403);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,33,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(381);
				system_primary();
				setState(382);
				match(LBRACK);
				setState(383);
				match(ID);
				setState(384);
				match(COLON);
				setState(385);
				typeExpr();
				setState(386);
				match(RBRACK);
				setState(395);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,32,_ctx) ) {
				case 1:
					{
					setState(387);
					match(LCURLY);
					setState(391);
					_errHandler.sync(this);
					_la = _input.LA(1);
					while (((((_la - 54)) & ~0x3f) == 0 && ((1L << (_la - 54)) & 2490369L) != 0)) {
						{
						{
						setState(388);
						create_index_item();
						}
						}
						setState(393);
						_errHandler.sync(this);
						_la = _input.LA(1);
					}
					setState(394);
					match(RCURLY);
					}
					break;
				}
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(397);
				system_primary();
				setState(398);
				match(LBRACK);
				setState(399);
				match(ID);
				setState(400);
				match(RBRACK);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(402);
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
		enterRule(_localctx, 46, RULE_create_index_item);
		try {
			setState(409);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,34,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(405);
				global_decl();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(406);
				init_clause();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(407);
				type_model();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(408);
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
		enterRule(_localctx, 48, RULE_global_decl);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(412);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==CONST) {
				{
				setState(411);
				match(CONST);
				}
			}

			setState(414);
			match(GLOBAL);
			setState(415);
			match(ID);
			setState(420);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(416);
				match(COMMA);
				setState(417);
				match(ID);
				}
				}
				setState(422);
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
		enterRule(_localctx, 50, RULE_init_clause);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(423);
			match(INIT);
			setState(424);
			match(COLON);
			setState(425);
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
		enterRule(_localctx, 52, RULE_system_primary);
		try {
			setState(432);
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
				setState(427);
				system_leaf();
				}
				break;
			case LPAREN:
				enterOuterAlt(_localctx, 2);
				{
				setState(428);
				match(LPAREN);
				setState(429);
				system_expr(0);
				setState(430);
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
		public List<TerminalNode> ID() { return getTokens(JulayParser.ID); }
		public TerminalNode ID(int i) {
			return getToken(JulayParser.ID, i);
		}
		public TerminalNode LT() { return getToken(JulayParser.LT, 0); }
		public TerminalNode GT() { return getToken(JulayParser.GT, 0); }
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
		enterRule(_localctx, 54, RULE_system_leaf);
		try {
			setState(440);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,38,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(434);
				qualified_name();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(435);
				match(ID);
				setState(436);
				match(LT);
				setState(437);
				match(ID);
				setState(438);
				match(GT);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(439);
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
		enterRule(_localctx, 56, RULE_invariant_decl);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(442);
			match(INVARIANT);
			setState(443);
			match(ID);
			setState(444);
			match(ASGN_EQ);
			setState(445);
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
		enterRule(_localctx, 58, RULE_proc_body);
		try {
			setState(451);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,39,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(447);
				var();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(448);
				constructor();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(449);
				transition();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(450);
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
		enterRule(_localctx, 60, RULE_field);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(453);
			match(ID);
			setState(454);
			match(COLON);
			setState(455);
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
		enterRule(_localctx, 62, RULE_var);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(457);
			_la = _input.LA(1);
			if ( !(_la==VAR || _la==CONST) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			setState(458);
			match(ID);
			setState(459);
			match(COLON);
			setState(460);
			typeExpr();
			setState(463);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ASGN_EQ) {
				{
				setState(461);
				match(ASGN_EQ);
				setState(462);
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
		enterRule(_localctx, 64, RULE_constructor);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(465);
			match(CONSTRUCTOR);
			setState(466);
			match(ID);
			setState(467);
			args();
			setState(470);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ALSO) {
				{
				setState(468);
				match(ALSO);
				setState(469);
				args();
				}
			}

			setState(472);
			match(LCURLY);
			setState(476);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (((((_la - 62)) & ~0x3f) == 0 && ((1L << (_la - 62)) & 15L) != 0)) {
				{
				{
				setState(473);
				constructor_body();
				}
				}
				setState(478);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(479);
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
		enterRule(_localctx, 66, RULE_transition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(482);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 2161727821137838080L) != 0)) {
				{
				setState(481);
				_la = _input.LA(1);
				if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 2161727821137838080L) != 0)) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
			}

			setState(484);
			match(TRANSITION);
			setState(485);
			match(ID);
			setState(486);
			args();
			setState(489);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ALSO) {
				{
				setState(487);
				match(ALSO);
				setState(488);
				args();
				}
			}

			setState(491);
			match(LCURLY);
			setState(495);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (((((_la - 61)) & ~0x3f) == 0 && ((1L << (_la - 61)) & 31L) != 0)) {
				{
				{
				setState(492);
				action_body();
				}
				}
				setState(497);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(498);
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
		enterRule(_localctx, 68, RULE_return_action);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(501);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 1873497444986126336L) != 0)) {
				{
				setState(500);
				_la = _input.LA(1);
				if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 1873497444986126336L) != 0)) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
			}

			setState(503);
			match(RETURN);
			setState(504);
			match(ID);
			setState(505);
			args();
			setState(506);
			match(LCURLY);
			setState(510);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==GUARD || _la==BEFORE) {
				{
				{
				setState(507);
				return_body();
				}
				}
				setState(512);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(513);
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
		enterRule(_localctx, 70, RULE_return_body);
		try {
			setState(517);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case GUARD:
				enterOuterAlt(_localctx, 1);
				{
				setState(515);
				guard();
				}
				break;
			case BEFORE:
				enterOuterAlt(_localctx, 2);
				{
				setState(516);
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
		enterRule(_localctx, 72, RULE_args);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(519);
			match(LPAREN);
			setState(521);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ID) {
				{
				setState(520);
				arg();
				}
			}

			setState(527);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(523);
				match(COMMA);
				setState(524);
				arg();
				}
				}
				setState(529);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(530);
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
		enterRule(_localctx, 74, RULE_arg);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(532);
			match(ID);
			setState(533);
			match(COLON);
			setState(534);
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
		enterRule(_localctx, 76, RULE_constructor_body);
		try {
			setState(540);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case BEFORE:
				enterOuterAlt(_localctx, 1);
				{
				setState(536);
				before();
				}
				break;
			case TRANSIT:
				enterOuterAlt(_localctx, 2);
				{
				setState(537);
				transit();
				}
				break;
			case ERROR:
				enterOuterAlt(_localctx, 3);
				{
				setState(538);
				error();
				}
				break;
			case AFTER:
				enterOuterAlt(_localctx, 4);
				{
				setState(539);
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
		enterRule(_localctx, 78, RULE_action_body);
		try {
			setState(547);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case GUARD:
				enterOuterAlt(_localctx, 1);
				{
				setState(542);
				guard();
				}
				break;
			case BEFORE:
				enterOuterAlt(_localctx, 2);
				{
				setState(543);
				before();
				}
				break;
			case TRANSIT:
				enterOuterAlt(_localctx, 3);
				{
				setState(544);
				transit();
				}
				break;
			case ERROR:
				enterOuterAlt(_localctx, 4);
				{
				setState(545);
				error();
				}
				break;
			case AFTER:
				enterOuterAlt(_localctx, 5);
				{
				setState(546);
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
		enterRule(_localctx, 80, RULE_guard);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(549);
			match(GUARD);
			setState(550);
			match(COLON);
			setState(551);
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
		enterRule(_localctx, 82, RULE_transit);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(553);
			match(TRANSIT);
			setState(554);
			match(COLON);
			setState(558);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (((((_la - 35)) & ~0x3f) == 0 && ((1L << (_la - 35)) & 1133871366145L) != 0)) {
				{
				{
				setState(555);
				var_transit();
				}
				}
				setState(560);
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
		enterRule(_localctx, 84, RULE_error);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(561);
			match(ERROR);
			setState(562);
			match(COLON);
			setState(564); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(563);
				error_arm();
				}
				}
				setState(566); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & 6786297435961360L) != 0) || ((((_la - 70)) & ~0x3f) == 0 && ((1L << (_la - 70)) & 121L) != 0) );
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
		enterRule(_localctx, 86, RULE_error_arm);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(568);
			expr(0);
			setState(569);
			match(ARROW);
			setState(570);
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
		enterRule(_localctx, 88, RULE_var_transit);
		try {
			setState(599);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,55,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(572);
				field_access();
				setState(573);
				match(ASGN_EQ);
				setState(574);
				expr(0);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(576);
				match(THIS);
				setState(577);
				match(DOT);
				setState(578);
				match(ID);
				setState(579);
				match(LBRACK);
				setState(580);
				expr(0);
				setState(581);
				match(RBRACK);
				setState(582);
				match(ASGN_EQ);
				setState(583);
				expr(0);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(585);
				match(ID);
				setState(586);
				match(LBRACK);
				setState(587);
				expr(0);
				setState(588);
				match(RBRACK);
				setState(589);
				match(ASGN_EQ);
				setState(590);
				expr(0);
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(592);
				match(LET);
				setState(593);
				match(ID);
				setState(594);
				match(COLON);
				setState(595);
				typeExpr();
				setState(596);
				match(ASGN_EQ);
				setState(597);
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
		enterRule(_localctx, 90, RULE_before);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(601);
			match(BEFORE);
			setState(602);
			match(COLON);
			setState(604); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(603);
				call_stmt();
				}
				}
				setState(606); 
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
		enterRule(_localctx, 92, RULE_after);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(608);
			match(AFTER);
			setState(609);
			match(COLON);
			setState(611); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(610);
				call_stmt();
				}
				}
				setState(613); 
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
		enterRule(_localctx, 94, RULE_call_stmt);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(615);
			match(ID);
			setState(617);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LT) {
				{
				setState(616);
				typeArgs();
				}
			}

			setState(619);
			match(LPAREN);
			setState(628);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 6786297435961360L) != 0) || ((((_la - 70)) & ~0x3f) == 0 && ((1L << (_la - 70)) & 121L) != 0)) {
				{
				setState(620);
				expr(0);
				setState(625);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(621);
					match(COMMA);
					setState(622);
					expr(0);
					}
					}
					setState(627);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
			}

			setState(630);
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
		int _startState = 96;
		enterRecursionRule(_localctx, 96, RULE_expr, _p);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(720);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,66,_ctx) ) {
			case 1:
				{
				setState(633);
				literal();
				}
				break;
			case 2:
				{
				setState(634);
				match(LPAREN);
				setState(635);
				expr(0);
				setState(636);
				match(RPAREN);
				}
				break;
			case 3:
				{
				setState(638);
				collection_literal();
				}
				break;
			case 4:
				{
				setState(639);
				method_prop_expr();
				}
				break;
			case 5:
				{
				setState(640);
				index_expr(0);
				}
				break;
			case 6:
				{
				setState(641);
				field_access();
				}
				break;
			case 7:
				{
				setState(642);
				record_literal();
				}
				break;
			case 8:
				{
				setState(643);
				fun_call();
				}
				break;
			case 9:
				{
				setState(644);
				handler_ref();
				}
				break;
			case 10:
				{
				setState(645);
				match(NOT);
				setState(646);
				expr(26);
				}
				break;
			case 11:
				{
				setState(647);
				match(AND);
				setState(648);
				expr(25);
				}
				break;
			case 12:
				{
				setState(649);
				match(OR);
				setState(650);
				expr(24);
				}
				break;
			case 13:
				{
				setState(651);
				match(IF);
				setState(652);
				match(LPAREN);
				setState(653);
				expr(0);
				setState(654);
				match(RPAREN);
				setState(660);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case LCURLY:
					{
					setState(655);
					match(LCURLY);
					setState(656);
					expr(0);
					setState(657);
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
					setState(659);
					expr(0);
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(662);
				match(ELSE);
				setState(668);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case LCURLY:
					{
					setState(663);
					match(LCURLY);
					setState(664);
					expr(0);
					setState(665);
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
					setState(667);
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
				setState(670);
				match(LET);
				setState(671);
				match(LPAREN);
				setState(672);
				match(ID);
				setState(673);
				match(COLON);
				setState(674);
				typeExpr();
				setState(675);
				match(ASGN_EQ);
				setState(676);
				expr(0);
				setState(677);
				match(RPAREN);
				setState(683);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case LCURLY:
					{
					setState(678);
					match(LCURLY);
					setState(679);
					expr(0);
					setState(680);
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
					setState(682);
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
				setState(685);
				match(WHEN);
				setState(686);
				match(LPAREN);
				setState(687);
				expr(0);
				setState(688);
				match(RPAREN);
				setState(689);
				match(LCURLY);
				setState(691); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(690);
					when_subject_arm();
					}
					}
					setState(693); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & 17179875344L) != 0) || ((((_la - 73)) & ~0x3f) == 0 && ((1L << (_la - 73)) & 15L) != 0) );
				setState(695);
				match(RCURLY);
				}
				break;
			case 16:
				{
				setState(697);
				match(WHEN);
				setState(698);
				match(LCURLY);
				setState(700); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(699);
					when_guard_arm();
					}
					}
					setState(702); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & 6786314615830544L) != 0) || ((((_la - 70)) & ~0x3f) == 0 && ((1L << (_la - 70)) & 121L) != 0) );
				setState(704);
				match(RCURLY);
				}
				break;
			case 17:
				{
				setState(706);
				match(FORALL);
				setState(707);
				match(ID);
				setState(708);
				match(COLON);
				setState(709);
				typeExpr();
				setState(710);
				match(COMMA);
				setState(711);
				expr(2);
				}
				break;
			case 18:
				{
				setState(713);
				match(EXISTS);
				setState(714);
				match(ID);
				setState(715);
				match(COLON);
				setState(716);
				typeExpr();
				setState(717);
				match(COMMA);
				setState(718);
				expr(1);
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(775);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,68,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(773);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,67,_ctx) ) {
					case 1:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(722);
						if (!(precpred(_ctx, 23))) throw new FailedPredicateException(this, "precpred(_ctx, 23)");
						setState(723);
						match(TIMES);
						setState(724);
						expr(24);
						}
						break;
					case 2:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(725);
						if (!(precpred(_ctx, 22))) throw new FailedPredicateException(this, "precpred(_ctx, 22)");
						setState(726);
						match(DIV);
						setState(727);
						expr(23);
						}
						break;
					case 3:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(728);
						if (!(precpred(_ctx, 21))) throw new FailedPredicateException(this, "precpred(_ctx, 21)");
						setState(729);
						match(MOD);
						setState(730);
						expr(22);
						}
						break;
					case 4:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(731);
						if (!(precpred(_ctx, 20))) throw new FailedPredicateException(this, "precpred(_ctx, 20)");
						setState(732);
						match(PLUS);
						setState(733);
						expr(21);
						}
						break;
					case 5:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(734);
						if (!(precpred(_ctx, 19))) throw new FailedPredicateException(this, "precpred(_ctx, 19)");
						setState(735);
						match(MINUS);
						setState(736);
						expr(20);
						}
						break;
					case 6:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(737);
						if (!(precpred(_ctx, 18))) throw new FailedPredicateException(this, "precpred(_ctx, 18)");
						setState(738);
						match(LT);
						setState(739);
						expr(19);
						}
						break;
					case 7:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(740);
						if (!(precpred(_ctx, 17))) throw new FailedPredicateException(this, "precpred(_ctx, 17)");
						setState(741);
						match(LTE);
						setState(742);
						expr(18);
						}
						break;
					case 8:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(743);
						if (!(precpred(_ctx, 16))) throw new FailedPredicateException(this, "precpred(_ctx, 16)");
						setState(744);
						match(GT);
						setState(745);
						expr(17);
						}
						break;
					case 9:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(746);
						if (!(precpred(_ctx, 15))) throw new FailedPredicateException(this, "precpred(_ctx, 15)");
						setState(747);
						match(GTE);
						setState(748);
						expr(16);
						}
						break;
					case 10:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(749);
						if (!(precpred(_ctx, 14))) throw new FailedPredicateException(this, "precpred(_ctx, 14)");
						setState(750);
						match(IN);
						setState(751);
						expr(15);
						}
						break;
					case 11:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(752);
						if (!(precpred(_ctx, 13))) throw new FailedPredicateException(this, "precpred(_ctx, 13)");
						setState(753);
						match(NIN);
						setState(754);
						expr(14);
						}
						break;
					case 12:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(755);
						if (!(precpred(_ctx, 12))) throw new FailedPredicateException(this, "precpred(_ctx, 12)");
						setState(756);
						match(EQ);
						setState(757);
						expr(13);
						}
						break;
					case 13:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(758);
						if (!(precpred(_ctx, 11))) throw new FailedPredicateException(this, "precpred(_ctx, 11)");
						setState(759);
						match(NEQ);
						setState(760);
						expr(12);
						}
						break;
					case 14:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(761);
						if (!(precpred(_ctx, 10))) throw new FailedPredicateException(this, "precpred(_ctx, 10)");
						setState(762);
						match(AND);
						setState(763);
						expr(11);
						}
						break;
					case 15:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(764);
						if (!(precpred(_ctx, 9))) throw new FailedPredicateException(this, "precpred(_ctx, 9)");
						setState(765);
						match(OR);
						setState(766);
						expr(10);
						}
						break;
					case 16:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(767);
						if (!(precpred(_ctx, 8))) throw new FailedPredicateException(this, "precpred(_ctx, 8)");
						setState(768);
						match(IMPLIES);
						setState(769);
						expr(8);
						}
						break;
					case 17:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(770);
						if (!(precpred(_ctx, 7))) throw new FailedPredicateException(this, "precpred(_ctx, 7)");
						setState(771);
						match(IFF);
						setState(772);
						expr(8);
						}
						break;
					}
					} 
				}
				setState(777);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,68,_ctx);
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
		enterRule(_localctx, 98, RULE_when_subject_arm);
		try {
			setState(785);
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
				setState(778);
				when_pattern();
				setState(779);
				match(ARROW);
				setState(780);
				expr(0);
				}
				break;
			case ELSE:
				enterOuterAlt(_localctx, 2);
				{
				setState(782);
				match(ELSE);
				setState(783);
				match(ARROW);
				setState(784);
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
		enterRule(_localctx, 100, RULE_when_guard_arm);
		try {
			setState(794);
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
				setState(787);
				expr(0);
				setState(788);
				match(ARROW);
				setState(789);
				expr(0);
				}
				break;
			case ELSE:
				enterOuterAlt(_localctx, 2);
				{
				setState(791);
				match(ELSE);
				setState(792);
				match(ARROW);
				setState(793);
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
		enterRule(_localctx, 102, RULE_when_pattern);
		try {
			setState(798);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case TRUE:
			case FALSE:
			case REAL:
			case INT:
			case STRING:
				enterOuterAlt(_localctx, 1);
				{
				setState(796);
				literal();
				}
				break;
			case LPAREN:
			case ID:
				enterOuterAlt(_localctx, 2);
				{
				setState(797);
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
		public List<TerminalNode> ID() { return getTokens(JulayParser.ID); }
		public TerminalNode ID(int i) {
			return getToken(JulayParser.ID, i);
		}
		public TerminalNode LT() { return getToken(JulayParser.LT, 0); }
		public TerminalNode GT() { return getToken(JulayParser.GT, 0); }
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
		int _startState = 104;
		enterRecursionRule(_localctx, 104, RULE_proc_expr, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(811);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,72,_ctx) ) {
			case 1:
				{
				setState(801);
				qualified_name();
				}
				break;
			case 2:
				{
				setState(802);
				match(ID);
				setState(803);
				match(LT);
				setState(804);
				match(ID);
				setState(805);
				match(GT);
				}
				break;
			case 3:
				{
				setState(806);
				match(ID);
				}
				break;
			case 4:
				{
				setState(807);
				match(LPAREN);
				setState(808);
				proc_expr(0);
				setState(809);
				match(RPAREN);
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(818);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,73,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					{
					_localctx = new Proc_exprContext(_parentctx, _parentState);
					pushNewRecursionContext(_localctx, _startState, RULE_proc_expr);
					setState(813);
					if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
					setState(814);
					match(PARALLEL);
					setState(815);
					proc_expr(2);
					}
					} 
				}
				setState(820);
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
		enterRule(_localctx, 106, RULE_handler_ref);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(821);
			match(ID);
			setState(822);
			match(DOT);
			setState(823);
			match(ID);
			setState(824);
			match(PROCFUN_ARROW);
			setState(825);
			match(ID);
			setState(826);
			match(DOT);
			setState(827);
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
		enterRule(_localctx, 108, RULE_literal);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(829);
			_la = _input.LA(1);
			if ( !(_la==TRUE || _la==FALSE || ((((_la - 73)) & ~0x3f) == 0 && ((1L << (_la - 73)) & 11L) != 0)) ) {
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
		enterRule(_localctx, 110, RULE_collection_literal);
		try {
			setState(834);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case LISTOF:
				enterOuterAlt(_localctx, 1);
				{
				setState(831);
				list_literal();
				}
				break;
			case SETOF:
				enterOuterAlt(_localctx, 2);
				{
				setState(832);
				set_literal();
				}
				break;
			case MAPOF:
				enterOuterAlt(_localctx, 3);
				{
				setState(833);
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
		enterRule(_localctx, 112, RULE_list_literal);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(836);
			match(LISTOF);
			setState(838);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LT) {
				{
				setState(837);
				typeArgs();
				}
			}

			setState(840);
			match(LPAREN);
			setState(849);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 6786297435961360L) != 0) || ((((_la - 70)) & ~0x3f) == 0 && ((1L << (_la - 70)) & 121L) != 0)) {
				{
				setState(841);
				expr(0);
				setState(846);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(842);
					match(COMMA);
					setState(843);
					expr(0);
					}
					}
					setState(848);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
			}

			setState(851);
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
		enterRule(_localctx, 114, RULE_set_literal);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(853);
			match(SETOF);
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
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 6786297435961360L) != 0) || ((((_la - 70)) & ~0x3f) == 0 && ((1L << (_la - 70)) & 121L) != 0)) {
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
		enterRule(_localctx, 116, RULE_map_literal);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(870);
			match(MAPOF);
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
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 6786297435961360L) != 0) || ((((_la - 70)) & ~0x3f) == 0 && ((1L << (_la - 70)) & 121L) != 0)) {
				{
				setState(875);
				map_entry();
				setState(880);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(876);
					match(COMMA);
					setState(877);
					map_entry();
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
		enterRule(_localctx, 118, RULE_map_entry);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(887);
			expr(0);
			setState(888);
			match(TO);
			setState(889);
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
		int _startState = 120;
		enterRecursionRule(_localctx, 120, RULE_index_expr, _p);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(940);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,89,_ctx) ) {
			case 1:
				{
				setState(899);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,84,_ctx) ) {
				case 1:
					{
					setState(892);
					fun_call();
					}
					break;
				case 2:
					{
					setState(893);
					field_access();
					}
					break;
				case 3:
					{
					setState(894);
					collection_literal();
					}
					break;
				case 4:
					{
					setState(895);
					match(LPAREN);
					setState(896);
					expr(0);
					setState(897);
					match(RPAREN);
					}
					break;
				}
				setState(901);
				match(LBRACK);
				setState(902);
				expr(0);
				setState(903);
				match(RBRACK);
				}
				break;
			case 2:
				{
				setState(912);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,85,_ctx) ) {
				case 1:
					{
					setState(905);
					fun_call();
					}
					break;
				case 2:
					{
					setState(906);
					field_access();
					}
					break;
				case 3:
					{
					setState(907);
					collection_literal();
					}
					break;
				case 4:
					{
					setState(908);
					match(LPAREN);
					setState(909);
					expr(0);
					setState(910);
					match(RPAREN);
					}
					break;
				}
				setState(914);
				match(DOT);
				setState(915);
				match(ID);
				setState(916);
				match(LPAREN);
				setState(925);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 6786297435961360L) != 0) || ((((_la - 70)) & ~0x3f) == 0 && ((1L << (_la - 70)) & 121L) != 0)) {
					{
					setState(917);
					call_arg();
					setState(922);
					_errHandler.sync(this);
					_la = _input.LA(1);
					while (_la==COMMA) {
						{
						{
						setState(918);
						match(COMMA);
						setState(919);
						call_arg();
						}
						}
						setState(924);
						_errHandler.sync(this);
						_la = _input.LA(1);
					}
					}
				}

				setState(927);
				match(RPAREN);
				}
				break;
			case 3:
				{
				setState(935);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case ID:
					{
					setState(929);
					fun_call();
					}
					break;
				case LISTOF:
				case SETOF:
				case MAPOF:
					{
					setState(930);
					collection_literal();
					}
					break;
				case LPAREN:
					{
					setState(931);
					match(LPAREN);
					setState(932);
					expr(0);
					setState(933);
					match(RPAREN);
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(937);
				match(DOT);
				setState(938);
				match(ID);
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(967);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,93,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(965);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,92,_ctx) ) {
					case 1:
						{
						_localctx = new Index_exprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_index_expr);
						setState(942);
						if (!(precpred(_ctx, 6))) throw new FailedPredicateException(this, "precpred(_ctx, 6)");
						setState(943);
						match(LBRACK);
						setState(944);
						expr(0);
						setState(945);
						match(RBRACK);
						}
						break;
					case 2:
						{
						_localctx = new Index_exprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_index_expr);
						setState(947);
						if (!(precpred(_ctx, 5))) throw new FailedPredicateException(this, "precpred(_ctx, 5)");
						setState(948);
						match(DOT);
						setState(949);
						match(ID);
						setState(950);
						match(LPAREN);
						setState(959);
						_errHandler.sync(this);
						_la = _input.LA(1);
						if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 6786297435961360L) != 0) || ((((_la - 70)) & ~0x3f) == 0 && ((1L << (_la - 70)) & 121L) != 0)) {
							{
							setState(951);
							call_arg();
							setState(956);
							_errHandler.sync(this);
							_la = _input.LA(1);
							while (_la==COMMA) {
								{
								{
								setState(952);
								match(COMMA);
								setState(953);
								call_arg();
								}
								}
								setState(958);
								_errHandler.sync(this);
								_la = _input.LA(1);
							}
							}
						}

						setState(961);
						match(RPAREN);
						}
						break;
					case 3:
						{
						_localctx = new Index_exprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_index_expr);
						setState(962);
						if (!(precpred(_ctx, 4))) throw new FailedPredicateException(this, "precpred(_ctx, 4)");
						setState(963);
						match(DOT);
						setState(964);
						match(ID);
						}
						break;
					}
					} 
				}
				setState(969);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,93,_ctx);
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
		enterRule(_localctx, 122, RULE_method_prop_expr);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(970);
			method_call();
			setState(975);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,94,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(971);
					match(DOT);
					setState(972);
					match(ID);
					}
					} 
				}
				setState(977);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,94,_ctx);
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
		enterRule(_localctx, 124, RULE_method_call);
		int _la;
		try {
			setState(1016);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case THIS:
				enterOuterAlt(_localctx, 1);
				{
				setState(978);
				match(THIS);
				setState(981); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(979);
					match(DOT);
					setState(980);
					match(ID);
					}
					}
					setState(983); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( _la==DOT );
				setState(985);
				match(LPAREN);
				setState(994);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 6786297435961360L) != 0) || ((((_la - 70)) & ~0x3f) == 0 && ((1L << (_la - 70)) & 121L) != 0)) {
					{
					setState(986);
					call_arg();
					setState(991);
					_errHandler.sync(this);
					_la = _input.LA(1);
					while (_la==COMMA) {
						{
						{
						setState(987);
						match(COMMA);
						setState(988);
						call_arg();
						}
						}
						setState(993);
						_errHandler.sync(this);
						_la = _input.LA(1);
					}
					}
				}

				setState(996);
				match(RPAREN);
				}
				break;
			case ID:
				enterOuterAlt(_localctx, 2);
				{
				setState(997);
				match(ID);
				setState(1000); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(998);
					match(DOT);
					setState(999);
					match(ID);
					}
					}
					setState(1002); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( _la==DOT );
				setState(1004);
				match(LPAREN);
				setState(1013);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 6786297435961360L) != 0) || ((((_la - 70)) & ~0x3f) == 0 && ((1L << (_la - 70)) & 121L) != 0)) {
					{
					setState(1005);
					call_arg();
					setState(1010);
					_errHandler.sync(this);
					_la = _input.LA(1);
					while (_la==COMMA) {
						{
						{
						setState(1006);
						match(COMMA);
						setState(1007);
						call_arg();
						}
						}
						setState(1012);
						_errHandler.sync(this);
						_la = _input.LA(1);
					}
					}
				}

				setState(1015);
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
		enterRule(_localctx, 126, RULE_fun_call);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1018);
			match(ID);
			setState(1020);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LT) {
				{
				setState(1019);
				typeArgs();
				}
			}

			setState(1022);
			match(LPAREN);
			setState(1031);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 6786297435961360L) != 0) || ((((_la - 70)) & ~0x3f) == 0 && ((1L << (_la - 70)) & 121L) != 0)) {
				{
				setState(1023);
				call_arg();
				setState(1028);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(1024);
					match(COMMA);
					setState(1025);
					call_arg();
					}
					}
					setState(1030);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
			}

			setState(1033);
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
		enterRule(_localctx, 128, RULE_call_arg);
		try {
			setState(1037);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,105,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1035);
				lambda_expr();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1036);
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
		enterRule(_localctx, 130, RULE_lambda_expr);
		try {
			setState(1049);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ID:
				enterOuterAlt(_localctx, 1);
				{
				setState(1039);
				match(ID);
				setState(1040);
				match(ARROW);
				setState(1041);
				expr(0);
				}
				break;
			case LPAREN:
				enterOuterAlt(_localctx, 2);
				{
				setState(1042);
				match(LPAREN);
				setState(1043);
				match(ID);
				setState(1044);
				match(COMMA);
				setState(1045);
				match(ID);
				setState(1046);
				match(RPAREN);
				setState(1047);
				match(ARROW);
				setState(1048);
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
		enterRule(_localctx, 132, RULE_record_literal);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1051);
			typeExpr();
			setState(1052);
			match(LCURLY);
			setState(1053);
			record_field_assign();
			setState(1058);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(1054);
				match(COMMA);
				setState(1055);
				record_field_assign();
				}
				}
				setState(1060);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(1061);
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
		enterRule(_localctx, 134, RULE_record_field_assign);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1063);
			match(ID);
			setState(1064);
			match(ASGN_EQ);
			setState(1065);
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
		enterRule(_localctx, 136, RULE_field_access);
		try {
			int _alt;
			setState(1085);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case THIS:
				enterOuterAlt(_localctx, 1);
				{
				setState(1067);
				match(THIS);
				setState(1068);
				match(DOT);
				setState(1069);
				match(ID);
				setState(1074);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,108,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(1070);
						match(DOT);
						setState(1071);
						match(ID);
						}
						} 
					}
					setState(1076);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,108,_ctx);
				}
				}
				break;
			case ID:
				enterOuterAlt(_localctx, 2);
				{
				setState(1077);
				match(ID);
				setState(1082);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,109,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(1078);
						match(DOT);
						setState(1079);
						match(ID);
						}
						} 
					}
					setState(1084);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,109,_ctx);
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
		case 20:
			return system_expr_sempred((System_exprContext)_localctx, predIndex);
		case 48:
			return expr_sempred((ExprContext)_localctx, predIndex);
		case 52:
			return proc_expr_sempred((Proc_exprContext)_localctx, predIndex);
		case 60:
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
		"\u0004\u0001O\u0440\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001\u0002"+
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
		"A\u0007A\u0002B\u0007B\u0002C\u0007C\u0002D\u0007D\u0001\u0000\u0001\u0000"+
		"\u0005\u0000\u008d\b\u0000\n\u0000\f\u0000\u0090\t\u0000\u0001\u0000\u0001"+
		"\u0000\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0002\u0001\u0002\u0001"+
		"\u0002\u0004\u0002\u009a\b\u0002\u000b\u0002\f\u0002\u009b\u0001\u0003"+
		"\u0001\u0003\u0001\u0004\u0003\u0004\u00a1\b\u0004\u0001\u0004\u0001\u0004"+
		"\u0003\u0004\u00a5\b\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0003\u0004"+
		"\u00aa\b\u0004\u0001\u0004\u0001\u0004\u0003\u0004\u00ae\b\u0004\u0001"+
		"\u0004\u0001\u0004\u0003\u0004\u00b2\b\u0004\u0001\u0004\u0003\u0004\u00b5"+
		"\b\u0004\u0001\u0005\u0001\u0005\u0001\u0006\u0001\u0006\u0001\u0006\u0001"+
		"\u0006\u0004\u0006\u00bd\b\u0006\u000b\u0006\f\u0006\u00be\u0001\u0006"+
		"\u0001\u0006\u0001\u0006\u0003\u0006\u00c4\b\u0006\u0001\u0007\u0001\u0007"+
		"\u0003\u0007\u00c8\b\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007"+
		"\u0003\u0007\u00ce\b\u0007\u0001\b\u0001\b\u0001\b\u0001\b\u0005\b\u00d4"+
		"\b\b\n\b\f\b\u00d7\t\b\u0001\b\u0001\b\u0001\t\u0001\t\u0001\t\u0001\t"+
		"\u0005\t\u00df\b\t\n\t\f\t\u00e2\t\t\u0001\t\u0001\t\u0001\n\u0001\n\u0001"+
		"\n\u0003\n\u00e9\b\n\u0001\n\u0001\n\u0001\n\u0001\n\u0001\n\u0001\n\u0001"+
		"\u000b\u0003\u000b\u00f2\b\u000b\u0001\u000b\u0001\u000b\u0001\u000b\u0001"+
		"\u000b\u0005\u000b\u00f8\b\u000b\n\u000b\f\u000b\u00fb\t\u000b\u0001\u000b"+
		"\u0001\u000b\u0001\u000b\u0001\u000b\u0001\u000b\u0003\u000b\u0102\b\u000b"+
		"\u0001\f\u0001\f\u0001\f\u0003\f\u0107\b\f\u0001\f\u0001\f\u0005\f\u010b"+
		"\b\f\n\f\f\f\u010e\t\f\u0001\f\u0001\f\u0001\f\u0001\f\u0001\f\u0001\f"+
		"\u0001\f\u0003\f\u0117\b\f\u0001\r\u0001\r\u0001\r\u0001\r\u0001\r\u0001"+
		"\r\u0005\r\u011f\b\r\n\r\f\r\u0122\t\r\u0001\r\u0001\r\u0001\u000e\u0001"+
		"\u000e\u0001\u000e\u0001\u000e\u0001\u000f\u0001\u000f\u0001\u000f\u0001"+
		"\u000f\u0005\u000f\u012e\b\u000f\n\u000f\f\u000f\u0131\t\u000f\u0001\u0010"+
		"\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010"+
		"\u0001\u0010\u0003\u0010\u013b\b\u0010\u0001\u0010\u0001\u0010\u0005\u0010"+
		"\u013f\b\u0010\n\u0010\f\u0010\u0142\t\u0010\u0001\u0010\u0001\u0010\u0001"+
		"\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0001"+
		"\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0001"+
		"\u0010\u0001\u0010\u0003\u0010\u0154\b\u0010\u0001\u0011\u0001\u0011\u0001"+
		"\u0011\u0003\u0011\u0159\b\u0011\u0001\u0012\u0001\u0012\u0001\u0012\u0001"+
		"\u0012\u0001\u0012\u0001\u0012\u0001\u0012\u0001\u0012\u0001\u0013\u0001"+
		"\u0013\u0003\u0013\u0165\b\u0013\u0001\u0014\u0001\u0014\u0001\u0014\u0003"+
		"\u0014\u016a\b\u0014\u0001\u0014\u0001\u0014\u0001\u0014\u0005\u0014\u016f"+
		"\b\u0014\n\u0014\f\u0014\u0172\t\u0014\u0001\u0015\u0001\u0015\u0001\u0015"+
		"\u0001\u0015\u0001\u0015\u0001\u0015\u0001\u0015\u0001\u0015\u0001\u0015"+
		"\u0001\u0015\u0001\u0016\u0001\u0016\u0001\u0016\u0001\u0016\u0001\u0016"+
		"\u0001\u0016\u0001\u0016\u0001\u0016\u0005\u0016\u0186\b\u0016\n\u0016"+
		"\f\u0016\u0189\t\u0016\u0001\u0016\u0003\u0016\u018c\b\u0016\u0001\u0016"+
		"\u0001\u0016\u0001\u0016\u0001\u0016\u0001\u0016\u0001\u0016\u0003\u0016"+
		"\u0194\b\u0016\u0001\u0017\u0001\u0017\u0001\u0017\u0001\u0017\u0003\u0017"+
		"\u019a\b\u0017\u0001\u0018\u0003\u0018\u019d\b\u0018\u0001\u0018\u0001"+
		"\u0018\u0001\u0018\u0001\u0018\u0005\u0018\u01a3\b\u0018\n\u0018\f\u0018"+
		"\u01a6\t\u0018\u0001\u0019\u0001\u0019\u0001\u0019\u0001\u0019\u0001\u001a"+
		"\u0001\u001a\u0001\u001a\u0001\u001a\u0001\u001a\u0003\u001a\u01b1\b\u001a"+
		"\u0001\u001b\u0001\u001b\u0001\u001b\u0001\u001b\u0001\u001b\u0001\u001b"+
		"\u0003\u001b\u01b9\b\u001b\u0001\u001c\u0001\u001c\u0001\u001c\u0001\u001c"+
		"\u0001\u001c\u0001\u001d\u0001\u001d\u0001\u001d\u0001\u001d\u0003\u001d"+
		"\u01c4\b\u001d\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001f"+
		"\u0001\u001f\u0001\u001f\u0001\u001f\u0001\u001f\u0001\u001f\u0003\u001f"+
		"\u01d0\b\u001f\u0001 \u0001 \u0001 \u0001 \u0001 \u0003 \u01d7\b \u0001"+
		" \u0001 \u0005 \u01db\b \n \f \u01de\t \u0001 \u0001 \u0001!\u0003!\u01e3"+
		"\b!\u0001!\u0001!\u0001!\u0001!\u0001!\u0003!\u01ea\b!\u0001!\u0001!\u0005"+
		"!\u01ee\b!\n!\f!\u01f1\t!\u0001!\u0001!\u0001\"\u0003\"\u01f6\b\"\u0001"+
		"\"\u0001\"\u0001\"\u0001\"\u0001\"\u0005\"\u01fd\b\"\n\"\f\"\u0200\t\""+
		"\u0001\"\u0001\"\u0001#\u0001#\u0003#\u0206\b#\u0001$\u0001$\u0003$\u020a"+
		"\b$\u0001$\u0001$\u0005$\u020e\b$\n$\f$\u0211\t$\u0001$\u0001$\u0001%"+
		"\u0001%\u0001%\u0001%\u0001&\u0001&\u0001&\u0001&\u0003&\u021d\b&\u0001"+
		"\'\u0001\'\u0001\'\u0001\'\u0001\'\u0003\'\u0224\b\'\u0001(\u0001(\u0001"+
		"(\u0001(\u0001)\u0001)\u0001)\u0005)\u022d\b)\n)\f)\u0230\t)\u0001*\u0001"+
		"*\u0001*\u0004*\u0235\b*\u000b*\f*\u0236\u0001+\u0001+\u0001+\u0001+\u0001"+
		",\u0001,\u0001,\u0001,\u0001,\u0001,\u0001,\u0001,\u0001,\u0001,\u0001"+
		",\u0001,\u0001,\u0001,\u0001,\u0001,\u0001,\u0001,\u0001,\u0001,\u0001"+
		",\u0001,\u0001,\u0001,\u0001,\u0001,\u0001,\u0003,\u0258\b,\u0001-\u0001"+
		"-\u0001-\u0004-\u025d\b-\u000b-\f-\u025e\u0001.\u0001.\u0001.\u0004.\u0264"+
		"\b.\u000b.\f.\u0265\u0001/\u0001/\u0003/\u026a\b/\u0001/\u0001/\u0001"+
		"/\u0001/\u0005/\u0270\b/\n/\f/\u0273\t/\u0003/\u0275\b/\u0001/\u0001/"+
		"\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u0001"+
		"0\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u0001"+
		"0\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00030\u0295"+
		"\b0\u00010\u00010\u00010\u00010\u00010\u00010\u00030\u029d\b0\u00010\u0001"+
		"0\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u0001"+
		"0\u00010\u00030\u02ac\b0\u00010\u00010\u00010\u00010\u00010\u00010\u0004"+
		"0\u02b4\b0\u000b0\f0\u02b5\u00010\u00010\u00010\u00010\u00010\u00040\u02bd"+
		"\b0\u000b0\f0\u02be\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u0001"+
		"0\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00030\u02d1"+
		"\b0\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u0001"+
		"0\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u0001"+
		"0\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u0001"+
		"0\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u0001"+
		"0\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u0001"+
		"0\u00010\u00050\u0306\b0\n0\f0\u0309\t0\u00011\u00011\u00011\u00011\u0001"+
		"1\u00011\u00011\u00031\u0312\b1\u00012\u00012\u00012\u00012\u00012\u0001"+
		"2\u00012\u00032\u031b\b2\u00013\u00013\u00033\u031f\b3\u00014\u00014\u0001"+
		"4\u00014\u00014\u00014\u00014\u00014\u00014\u00014\u00014\u00034\u032c"+
		"\b4\u00014\u00014\u00014\u00054\u0331\b4\n4\f4\u0334\t4\u00015\u00015"+
		"\u00015\u00015\u00015\u00015\u00015\u00015\u00016\u00016\u00017\u0001"+
		"7\u00017\u00037\u0343\b7\u00018\u00018\u00038\u0347\b8\u00018\u00018\u0001"+
		"8\u00018\u00058\u034d\b8\n8\f8\u0350\t8\u00038\u0352\b8\u00018\u00018"+
		"\u00019\u00019\u00039\u0358\b9\u00019\u00019\u00019\u00019\u00059\u035e"+
		"\b9\n9\f9\u0361\t9\u00039\u0363\b9\u00019\u00019\u0001:\u0001:\u0003:"+
		"\u0369\b:\u0001:\u0001:\u0001:\u0001:\u0005:\u036f\b:\n:\f:\u0372\t:\u0003"+
		":\u0374\b:\u0001:\u0001:\u0001;\u0001;\u0001;\u0001;\u0001<\u0001<\u0001"+
		"<\u0001<\u0001<\u0001<\u0001<\u0001<\u0003<\u0384\b<\u0001<\u0001<\u0001"+
		"<\u0001<\u0001<\u0001<\u0001<\u0001<\u0001<\u0001<\u0001<\u0003<\u0391"+
		"\b<\u0001<\u0001<\u0001<\u0001<\u0001<\u0001<\u0005<\u0399\b<\n<\f<\u039c"+
		"\t<\u0003<\u039e\b<\u0001<\u0001<\u0001<\u0001<\u0001<\u0001<\u0001<\u0001"+
		"<\u0003<\u03a8\b<\u0001<\u0001<\u0001<\u0003<\u03ad\b<\u0001<\u0001<\u0001"+
		"<\u0001<\u0001<\u0001<\u0001<\u0001<\u0001<\u0001<\u0001<\u0001<\u0005"+
		"<\u03bb\b<\n<\f<\u03be\t<\u0003<\u03c0\b<\u0001<\u0001<\u0001<\u0001<"+
		"\u0005<\u03c6\b<\n<\f<\u03c9\t<\u0001=\u0001=\u0001=\u0005=\u03ce\b=\n"+
		"=\f=\u03d1\t=\u0001>\u0001>\u0001>\u0004>\u03d6\b>\u000b>\f>\u03d7\u0001"+
		">\u0001>\u0001>\u0001>\u0005>\u03de\b>\n>\f>\u03e1\t>\u0003>\u03e3\b>"+
		"\u0001>\u0001>\u0001>\u0001>\u0004>\u03e9\b>\u000b>\f>\u03ea\u0001>\u0001"+
		">\u0001>\u0001>\u0005>\u03f1\b>\n>\f>\u03f4\t>\u0003>\u03f6\b>\u0001>"+
		"\u0003>\u03f9\b>\u0001?\u0001?\u0003?\u03fd\b?\u0001?\u0001?\u0001?\u0001"+
		"?\u0005?\u0403\b?\n?\f?\u0406\t?\u0003?\u0408\b?\u0001?\u0001?\u0001@"+
		"\u0001@\u0003@\u040e\b@\u0001A\u0001A\u0001A\u0001A\u0001A\u0001A\u0001"+
		"A\u0001A\u0001A\u0001A\u0003A\u041a\bA\u0001B\u0001B\u0001B\u0001B\u0001"+
		"B\u0005B\u0421\bB\nB\fB\u0424\tB\u0001B\u0001B\u0001C\u0001C\u0001C\u0001"+
		"C\u0001D\u0001D\u0001D\u0001D\u0001D\u0005D\u0431\bD\nD\fD\u0434\tD\u0001"+
		"D\u0001D\u0001D\u0005D\u0439\bD\nD\fD\u043c\tD\u0003D\u043e\bD\u0001D"+
		"\u0000\u0004(`hxE\u0000\u0002\u0004\u0006\b\n\f\u000e\u0010\u0012\u0014"+
		"\u0016\u0018\u001a\u001c\u001e \"$&(*,.02468:<>@BDFHJLNPRTVXZ\\^`bdfh"+
		"jlnprtvxz|~\u0080\u0082\u0084\u0086\u0088\u0000\u0005\u0004\u0000*,.."+
		"9<KK\u0001\u000056\u0001\u00009<\u0002\u000099;<\u0003\u0000\u000b\fI"+
		"JLL\u04a7\u0000\u008e\u0001\u0000\u0000\u0000\u0002\u0093\u0001\u0000"+
		"\u0000\u0000\u0004\u0096\u0001\u0000\u0000\u0000\u0006\u009d\u0001\u0000"+
		"\u0000\u0000\b\u00b4\u0001\u0000\u0000\u0000\n\u00b6\u0001\u0000\u0000"+
		"\u0000\f\u00c3\u0001\u0000\u0000\u0000\u000e\u00cd\u0001\u0000\u0000\u0000"+
		"\u0010\u00cf\u0001\u0000\u0000\u0000\u0012\u00da\u0001\u0000\u0000\u0000"+
		"\u0014\u00e5\u0001\u0000\u0000\u0000\u0016\u0101\u0001\u0000\u0000\u0000"+
		"\u0018\u0116\u0001\u0000\u0000\u0000\u001a\u0118\u0001\u0000\u0000\u0000"+
		"\u001c\u0125\u0001\u0000\u0000\u0000\u001e\u0129\u0001\u0000\u0000\u0000"+
		" \u0153\u0001\u0000\u0000\u0000\"\u0158\u0001\u0000\u0000\u0000$\u015a"+
		"\u0001\u0000\u0000\u0000&\u0164\u0001\u0000\u0000\u0000(\u0169\u0001\u0000"+
		"\u0000\u0000*\u0173\u0001\u0000\u0000\u0000,\u0193\u0001\u0000\u0000\u0000"+
		".\u0199\u0001\u0000\u0000\u00000\u019c\u0001\u0000\u0000\u00002\u01a7"+
		"\u0001\u0000\u0000\u00004\u01b0\u0001\u0000\u0000\u00006\u01b8\u0001\u0000"+
		"\u0000\u00008\u01ba\u0001\u0000\u0000\u0000:\u01c3\u0001\u0000\u0000\u0000"+
		"<\u01c5\u0001\u0000\u0000\u0000>\u01c9\u0001\u0000\u0000\u0000@\u01d1"+
		"\u0001\u0000\u0000\u0000B\u01e2\u0001\u0000\u0000\u0000D\u01f5\u0001\u0000"+
		"\u0000\u0000F\u0205\u0001\u0000\u0000\u0000H\u0207\u0001\u0000\u0000\u0000"+
		"J\u0214\u0001\u0000\u0000\u0000L\u021c\u0001\u0000\u0000\u0000N\u0223"+
		"\u0001\u0000\u0000\u0000P\u0225\u0001\u0000\u0000\u0000R\u0229\u0001\u0000"+
		"\u0000\u0000T\u0231\u0001\u0000\u0000\u0000V\u0238\u0001\u0000\u0000\u0000"+
		"X\u0257\u0001\u0000\u0000\u0000Z\u0259\u0001\u0000\u0000\u0000\\\u0260"+
		"\u0001\u0000\u0000\u0000^\u0267\u0001\u0000\u0000\u0000`\u02d0\u0001\u0000"+
		"\u0000\u0000b\u0311\u0001\u0000\u0000\u0000d\u031a\u0001\u0000\u0000\u0000"+
		"f\u031e\u0001\u0000\u0000\u0000h\u032b\u0001\u0000\u0000\u0000j\u0335"+
		"\u0001\u0000\u0000\u0000l\u033d\u0001\u0000\u0000\u0000n\u0342\u0001\u0000"+
		"\u0000\u0000p\u0344\u0001\u0000\u0000\u0000r\u0355\u0001\u0000\u0000\u0000"+
		"t\u0366\u0001\u0000\u0000\u0000v\u0377\u0001\u0000\u0000\u0000x\u03ac"+
		"\u0001\u0000\u0000\u0000z\u03ca\u0001\u0000\u0000\u0000|\u03f8\u0001\u0000"+
		"\u0000\u0000~\u03fa\u0001\u0000\u0000\u0000\u0080\u040d\u0001\u0000\u0000"+
		"\u0000\u0082\u0419\u0001\u0000\u0000\u0000\u0084\u041b\u0001\u0000\u0000"+
		"\u0000\u0086\u0427\u0001\u0000\u0000\u0000\u0088\u043d\u0001\u0000\u0000"+
		"\u0000\u008a\u008d\u0003\u0002\u0001\u0000\u008b\u008d\u0003\b\u0004\u0000"+
		"\u008c\u008a\u0001\u0000\u0000\u0000\u008c\u008b\u0001\u0000\u0000\u0000"+
		"\u008d\u0090\u0001\u0000\u0000\u0000\u008e\u008c\u0001\u0000\u0000\u0000"+
		"\u008e\u008f\u0001\u0000\u0000\u0000\u008f\u0091\u0001\u0000\u0000\u0000"+
		"\u0090\u008e\u0001\u0000\u0000\u0000\u0091\u0092\u0005\u0000\u0000\u0001"+
		"\u0092\u0001\u0001\u0000\u0000\u0000\u0093\u0094\u0005(\u0000\u0000\u0094"+
		"\u0095\u0003\u0004\u0002\u0000\u0095\u0003\u0001\u0000\u0000\u0000\u0096"+
		"\u0099\u0003\u0006\u0003\u0000\u0097\u0098\u0005\u0002\u0000\u0000\u0098"+
		"\u009a\u0003\u0006\u0003\u0000\u0099\u0097\u0001\u0000\u0000\u0000\u009a"+
		"\u009b\u0001\u0000\u0000\u0000\u009b\u0099\u0001\u0000\u0000\u0000\u009b"+
		"\u009c\u0001\u0000\u0000\u0000\u009c\u0005\u0001\u0000\u0000\u0000\u009d"+
		"\u009e\u0007\u0000\u0000\u0000\u009e\u0007\u0001\u0000\u0000\u0000\u009f"+
		"\u00a1\u0005)\u0000\u0000\u00a0\u009f\u0001\u0000\u0000\u0000\u00a0\u00a1"+
		"\u0001\u0000\u0000\u0000\u00a1\u00a2\u0001\u0000\u0000\u0000\u00a2\u00b5"+
		"\u0003\u0016\u000b\u0000\u00a3\u00a5\u0005)\u0000\u0000\u00a4\u00a3\u0001"+
		"\u0000\u0000\u0000\u00a4\u00a5\u0001\u0000\u0000\u0000\u00a5\u00a6\u0001"+
		"\u0000\u0000\u0000\u00a6\u00b5\u0003\u0018\f\u0000\u00a7\u00b5\u0003\u001e"+
		"\u000f\u0000\u00a8\u00aa\u0005)\u0000\u0000\u00a9\u00a8\u0001\u0000\u0000"+
		"\u0000\u00a9\u00aa\u0001\u0000\u0000\u0000\u00aa\u00ab\u0001\u0000\u0000"+
		"\u0000\u00ab\u00b5\u0003 \u0010\u0000\u00ac\u00ae\u0005)\u0000\u0000\u00ad"+
		"\u00ac\u0001\u0000\u0000\u0000\u00ad\u00ae\u0001\u0000\u0000\u0000\u00ae"+
		"\u00af\u0001\u0000\u0000\u0000\u00af\u00b5\u00038\u001c\u0000\u00b0\u00b2"+
		"\u0005)\u0000\u0000\u00b1\u00b0\u0001\u0000\u0000\u0000\u00b1\u00b2\u0001"+
		"\u0000\u0000\u0000\u00b2\u00b3\u0001\u0000\u0000\u0000\u00b3\u00b5\u0003"+
		"\u0014\n\u0000\u00b4\u00a0\u0001\u0000\u0000\u0000\u00b4\u00a4\u0001\u0000"+
		"\u0000\u0000\u00b4\u00a7\u0001\u0000\u0000\u0000\u00b4\u00a9\u0001\u0000"+
		"\u0000\u0000\u00b4\u00ad\u0001\u0000\u0000\u0000\u00b4\u00b1\u0001\u0000"+
		"\u0000\u0000\u00b5\t\u0001\u0000\u0000\u0000\u00b6\u00b7\u0003\f\u0006"+
		"\u0000\u00b7\u000b\u0001\u0000\u0000\u0000\u00b8\u00b9\u0005\u0004\u0000"+
		"\u0000\u00b9\u00bc\u0003\n\u0005\u0000\u00ba\u00bb\u0005\u0001\u0000\u0000"+
		"\u00bb\u00bd\u0003\n\u0005\u0000\u00bc\u00ba\u0001\u0000\u0000\u0000\u00bd"+
		"\u00be\u0001\u0000\u0000\u0000\u00be\u00bc\u0001\u0000\u0000\u0000\u00be"+
		"\u00bf\u0001\u0000\u0000\u0000\u00bf\u00c0\u0001\u0000\u0000\u0000\u00c0"+
		"\u00c1\u0005\u0005\u0000\u0000\u00c1\u00c4\u0001\u0000\u0000\u0000\u00c2"+
		"\u00c4\u0003\u000e\u0007\u0000\u00c3\u00b8\u0001\u0000\u0000\u0000\u00c3"+
		"\u00c2\u0001\u0000\u0000\u0000\u00c4\r\u0001\u0000\u0000\u0000\u00c5\u00c7"+
		"\u0005K\u0000\u0000\u00c6\u00c8\u0003\u0010\b\u0000\u00c7\u00c6\u0001"+
		"\u0000\u0000\u0000\u00c7\u00c8\u0001\u0000\u0000\u0000\u00c8\u00ce\u0001"+
		"\u0000\u0000\u0000\u00c9\u00ca\u0005\u0004\u0000\u0000\u00ca\u00cb\u0003"+
		"\n\u0005\u0000\u00cb\u00cc\u0005\u0005\u0000\u0000\u00cc\u00ce\u0001\u0000"+
		"\u0000\u0000\u00cd\u00c5\u0001\u0000\u0000\u0000\u00cd\u00c9\u0001\u0000"+
		"\u0000\u0000\u00ce\u000f\u0001\u0000\u0000\u0000\u00cf\u00d0\u0005\u0017"+
		"\u0000\u0000\u00d0\u00d5\u0003\n\u0005\u0000\u00d1\u00d2\u0005\u0001\u0000"+
		"\u0000\u00d2\u00d4\u0003\n\u0005\u0000\u00d3\u00d1\u0001\u0000\u0000\u0000"+
		"\u00d4\u00d7\u0001\u0000\u0000\u0000\u00d5\u00d3\u0001\u0000\u0000\u0000"+
		"\u00d5\u00d6\u0001\u0000\u0000\u0000\u00d6\u00d8\u0001\u0000\u0000\u0000"+
		"\u00d7\u00d5\u0001\u0000\u0000\u0000\u00d8\u00d9\u0005\u0019\u0000\u0000"+
		"\u00d9\u0011\u0001\u0000\u0000\u0000\u00da\u00db\u0005\u0017\u0000\u0000"+
		"\u00db\u00e0\u0005K\u0000\u0000\u00dc\u00dd\u0005\u0001\u0000\u0000\u00dd"+
		"\u00df\u0005K\u0000\u0000\u00de\u00dc\u0001\u0000\u0000\u0000\u00df\u00e2"+
		"\u0001\u0000\u0000\u0000\u00e0\u00de\u0001\u0000\u0000\u0000\u00e0\u00e1"+
		"\u0001\u0000\u0000\u0000\u00e1\u00e3\u0001\u0000\u0000\u0000\u00e2\u00e0"+
		"\u0001\u0000\u0000\u0000\u00e3\u00e4\u0005\u0019\u0000\u0000\u00e4\u0013"+
		"\u0001\u0000\u0000\u0000\u00e5\u00e6\u0005B\u0000\u0000\u00e6\u00e8\u0005"+
		"K\u0000\u0000\u00e7\u00e9\u0003\u0012\t\u0000\u00e8\u00e7\u0001\u0000"+
		"\u0000\u0000\u00e8\u00e9\u0001\u0000\u0000\u0000\u00e9\u00ea\u0001\u0000"+
		"\u0000\u0000\u00ea\u00eb\u0003H$\u0000\u00eb\u00ec\u0005\u0003\u0000\u0000"+
		"\u00ec\u00ed\u0003\n\u0005\u0000\u00ed\u00ee\u0005\u001b\u0000\u0000\u00ee"+
		"\u00ef\u0003`0\u0000\u00ef\u0015\u0001\u0000\u0000\u0000\u00f0\u00f2\u0005"+
		".\u0000\u0000\u00f1\u00f0\u0001\u0000\u0000\u0000\u00f1\u00f2\u0001\u0000"+
		"\u0000\u0000\u00f2\u00f3\u0001\u0000\u0000\u0000\u00f3\u00f4\u0005/\u0000"+
		"\u0000\u00f4\u00f5\u0005K\u0000\u0000\u00f5\u00f9\u0005\b\u0000\u0000"+
		"\u00f6\u00f8\u0003:\u001d\u0000\u00f7\u00f6\u0001\u0000\u0000\u0000\u00f8"+
		"\u00fb\u0001\u0000\u0000\u0000\u00f9\u00f7\u0001\u0000\u0000\u0000\u00f9"+
		"\u00fa\u0001\u0000\u0000\u0000\u00fa\u00fc\u0001\u0000\u0000\u0000\u00fb"+
		"\u00f9\u0001\u0000\u0000\u0000\u00fc\u0102\u0005\t\u0000\u0000\u00fd\u00fe"+
		"\u0005/\u0000\u0000\u00fe\u00ff\u0005K\u0000\u0000\u00ff\u0100\u0005\u001e"+
		"\u0000\u0000\u0100\u0102\u0003h4\u0000\u0101\u00f1\u0001\u0000\u0000\u0000"+
		"\u0101\u00fd\u0001\u0000\u0000\u0000\u0102\u0017\u0001\u0000\u0000\u0000"+
		"\u0103\u0104\u0005-\u0000\u0000\u0104\u0106\u0005K\u0000\u0000\u0105\u0107"+
		"\u0003\u0012\t\u0000\u0106\u0105\u0001\u0000\u0000\u0000\u0106\u0107\u0001"+
		"\u0000\u0000\u0000\u0107\u0108\u0001\u0000\u0000\u0000\u0108\u010c\u0005"+
		"\b\u0000\u0000\u0109\u010b\u0003<\u001e\u0000\u010a\u0109\u0001\u0000"+
		"\u0000\u0000\u010b\u010e\u0001\u0000\u0000\u0000\u010c\u010a\u0001\u0000"+
		"\u0000\u0000\u010c\u010d\u0001\u0000\u0000\u0000\u010d\u010f\u0001\u0000"+
		"\u0000\u0000\u010e\u010c\u0001\u0000\u0000\u0000\u010f\u0117\u0005\t\u0000"+
		"\u0000\u0110\u0111\u0005-\u0000\u0000\u0111\u0112\u0005K\u0000\u0000\u0112"+
		"\u0113\u0005\u001e\u0000\u0000\u0113\u0117\u0003\n\u0005\u0000\u0114\u0115"+
		"\u0005-\u0000\u0000\u0115\u0117\u0005K\u0000\u0000\u0116\u0103\u0001\u0000"+
		"\u0000\u0000\u0116\u0110\u0001\u0000\u0000\u0000\u0116\u0114\u0001\u0000"+
		"\u0000\u0000\u0117\u0019\u0001\u0000\u0000\u0000\u0118\u0119\u0005K\u0000"+
		"\u0000\u0119\u011a\u0005\u001e\u0000\u0000\u011a\u011b\u0005\b\u0000\u0000"+
		"\u011b\u0120\u0003l6\u0000\u011c\u011d\u0005\u0001\u0000\u0000\u011d\u011f"+
		"\u0003l6\u0000\u011e\u011c\u0001\u0000\u0000\u0000\u011f\u0122\u0001\u0000"+
		"\u0000\u0000\u0120\u011e\u0001\u0000\u0000\u0000\u0120\u0121\u0001\u0000"+
		"\u0000\u0000\u0121\u0123\u0001\u0000\u0000\u0000\u0122\u0120\u0001\u0000"+
		"\u0000\u0000\u0123\u0124\u0005\t\u0000\u0000\u0124\u001b\u0001\u0000\u0000"+
		"\u0000\u0125\u0126\u0005K\u0000\u0000\u0126\u0127\u0005\u001e\u0000\u0000"+
		"\u0127\u0128\u0003`0\u0000\u0128\u001d\u0001\u0000\u0000\u0000\u0129\u012a"+
		"\u00050\u0000\u0000\u012a\u012f\u0005K\u0000\u0000\u012b\u012c\u0005\u0001"+
		"\u0000\u0000\u012c\u012e\u0005K\u0000\u0000\u012d\u012b\u0001\u0000\u0000"+
		"\u0000\u012e\u0131\u0001\u0000\u0000\u0000\u012f\u012d\u0001\u0000\u0000"+
		"\u0000\u012f\u0130\u0001\u0000\u0000\u0000\u0130\u001f\u0001\u0000\u0000"+
		"\u0000\u0131\u012f\u0001\u0000\u0000\u0000\u0132\u0133\u00051\u0000\u0000"+
		"\u0133\u013a\u0005K\u0000\u0000\u0134\u0135\u0005\u0006\u0000\u0000\u0135"+
		"\u0136\u0005K\u0000\u0000\u0136\u0137\u0005\u0003\u0000\u0000\u0137\u0138"+
		"\u0003\n\u0005\u0000\u0138\u0139\u0005\u0007\u0000\u0000\u0139\u013b\u0001"+
		"\u0000\u0000\u0000\u013a\u0134\u0001\u0000\u0000\u0000\u013a\u013b\u0001"+
		"\u0000\u0000\u0000\u013b\u013c\u0001\u0000\u0000\u0000\u013c\u0140\u0005"+
		"\b\u0000\u0000\u013d\u013f\u0003\"\u0011\u0000\u013e\u013d\u0001\u0000"+
		"\u0000\u0000\u013f\u0142\u0001\u0000\u0000\u0000\u0140\u013e\u0001\u0000"+
		"\u0000\u0000\u0140\u0141\u0001\u0000\u0000\u0000\u0141\u0143\u0001\u0000"+
		"\u0000\u0000\u0142\u0140\u0001\u0000\u0000\u0000\u0143\u0154\u0005\t\u0000"+
		"\u0000\u0144\u0145\u00051\u0000\u0000\u0145\u0146\u0005K\u0000\u0000\u0146"+
		"\u0147\u0005\u001e\u0000\u0000\u0147\u0154\u0003$\u0012\u0000\u0148\u0149"+
		"\u00051\u0000\u0000\u0149\u014a\u0005K\u0000\u0000\u014a\u014b\u0005\u001e"+
		"\u0000\u0000\u014b\u014c\u0003(\u0014\u0000\u014c\u014d\u0005\u000e\u0000"+
		"\u0000\u014d\u014e\u0003`0\u0000\u014e\u0154\u0001\u0000\u0000\u0000\u014f"+
		"\u0150\u00051\u0000\u0000\u0150\u0151\u0005K\u0000\u0000\u0151\u0152\u0005"+
		"\u001e\u0000\u0000\u0152\u0154\u0003(\u0014\u0000\u0153\u0132\u0001\u0000"+
		"\u0000\u0000\u0153\u0144\u0001\u0000\u0000\u0000\u0153\u0148\u0001\u0000"+
		"\u0000\u0000\u0153\u014f\u0001\u0000\u0000\u0000\u0154!\u0001\u0000\u0000"+
		"\u0000\u0155\u0159\u0003:\u001d\u0000\u0156\u0159\u0003\u001a\r\u0000"+
		"\u0157\u0159\u0003\u001c\u000e\u0000\u0158\u0155\u0001\u0000\u0000\u0000"+
		"\u0158\u0156\u0001\u0000\u0000\u0000\u0158\u0157\u0001\u0000\u0000\u0000"+
		"\u0159#\u0001\u0000\u0000\u0000\u015a\u015b\u0005\u0017\u0000\u0000\u015b"+
		"\u015c\u0003&\u0013\u0000\u015c\u015d\u0005\u0019\u0000\u0000\u015d\u015e"+
		"\u0003(\u0014\u0000\u015e\u015f\u0005\u0017\u0000\u0000\u015f\u0160\u0003"+
		"`0\u0000\u0160\u0161\u0005\u0019\u0000\u0000\u0161%\u0001\u0000\u0000"+
		"\u0000\u0162\u0165\u0005\u000b\u0000\u0000\u0163\u0165\u0003(\u0014\u0000"+
		"\u0164\u0162\u0001\u0000\u0000\u0000\u0164\u0163\u0001\u0000\u0000\u0000"+
		"\u0165\'\u0001\u0000\u0000\u0000\u0166\u0167\u0006\u0014\uffff\uffff\u0000"+
		"\u0167\u016a\u0003*\u0015\u0000\u0168\u016a\u0003,\u0016\u0000\u0169\u0166"+
		"\u0001\u0000\u0000\u0000\u0169\u0168\u0001\u0000\u0000\u0000\u016a\u0170"+
		"\u0001\u0000\u0000\u0000\u016b\u016c\n\u0003\u0000\u0000\u016c\u016d\u0005"+
		"\n\u0000\u0000\u016d\u016f\u0003(\u0014\u0004\u016e\u016b\u0001\u0000"+
		"\u0000\u0000\u016f\u0172\u0001\u0000\u0000\u0000\u0170\u016e\u0001\u0000"+
		"\u0000\u0000\u0170\u0171\u0001\u0000\u0000\u0000\u0171)\u0001\u0000\u0000"+
		"\u0000\u0172\u0170\u0001\u0000\u0000\u0000\u0173\u0174\u0005E\u0000\u0000"+
		"\u0174\u0175\u0005\u0004\u0000\u0000\u0175\u0176\u0005K\u0000\u0000\u0176"+
		"\u0177\u0005\u0003\u0000\u0000\u0177\u0178\u0003\n\u0005\u0000\u0178\u0179"+
		"\u0005\u0005\u0000\u0000\u0179\u017a\u0005\b\u0000\u0000\u017a\u017b\u0003"+
		"(\u0014\u0000\u017b\u017c\u0005\t\u0000\u0000\u017c+\u0001\u0000\u0000"+
		"\u0000\u017d\u017e\u00034\u001a\u0000\u017e\u017f\u0005\u0006\u0000\u0000"+
		"\u017f\u0180\u0005K\u0000\u0000\u0180\u0181\u0005\u0003\u0000\u0000\u0181"+
		"\u0182\u0003\n\u0005\u0000\u0182\u018b\u0005\u0007\u0000\u0000\u0183\u0187"+
		"\u0005\b\u0000\u0000\u0184\u0186\u0003.\u0017\u0000\u0185\u0184\u0001"+
		"\u0000\u0000\u0000\u0186\u0189\u0001\u0000\u0000\u0000\u0187\u0185\u0001"+
		"\u0000\u0000\u0000\u0187\u0188\u0001\u0000\u0000\u0000\u0188\u018a\u0001"+
		"\u0000\u0000\u0000\u0189\u0187\u0001\u0000\u0000\u0000\u018a\u018c\u0005"+
		"\t\u0000\u0000\u018b\u0183\u0001\u0000\u0000\u0000\u018b\u018c\u0001\u0000"+
		"\u0000\u0000\u018c\u0194\u0001\u0000\u0000\u0000\u018d\u018e\u00034\u001a"+
		"\u0000\u018e\u018f\u0005\u0006\u0000\u0000\u018f\u0190\u0005K\u0000\u0000"+
		"\u0190\u0191\u0005\u0007\u0000\u0000\u0191\u0194\u0001\u0000\u0000\u0000"+
		"\u0192\u0194\u00034\u001a\u0000\u0193\u017d\u0001\u0000\u0000\u0000\u0193"+
		"\u018d\u0001\u0000\u0000\u0000\u0193\u0192\u0001\u0000\u0000\u0000\u0194"+
		"-\u0001\u0000\u0000\u0000\u0195\u019a\u00030\u0018\u0000\u0196\u019a\u0003"+
		"2\u0019\u0000\u0197\u019a\u0003\u001a\r\u0000\u0198\u019a\u0003\u001c"+
		"\u000e\u0000\u0199\u0195\u0001\u0000\u0000\u0000\u0199\u0196\u0001\u0000"+
		"\u0000\u0000\u0199\u0197\u0001\u0000\u0000\u0000\u0199\u0198\u0001\u0000"+
		"\u0000\u0000\u019a/\u0001\u0000\u0000\u0000\u019b\u019d\u00056\u0000\u0000"+
		"\u019c\u019b\u0001\u0000\u0000\u0000\u019c\u019d\u0001\u0000\u0000\u0000"+
		"\u019d\u019e\u0001\u0000\u0000\u0000\u019e\u019f\u0005G\u0000\u0000\u019f"+
		"\u01a4\u0005K\u0000\u0000\u01a0\u01a1\u0005\u0001\u0000\u0000\u01a1\u01a3"+
		"\u0005K\u0000\u0000\u01a2\u01a0\u0001\u0000\u0000\u0000\u01a3\u01a6\u0001"+
		"\u0000\u0000\u0000\u01a4\u01a2\u0001\u0000\u0000\u0000\u01a4\u01a5\u0001"+
		"\u0000\u0000\u0000\u01a51\u0001\u0000\u0000\u0000\u01a6\u01a4\u0001\u0000"+
		"\u0000\u0000\u01a7\u01a8\u0005H\u0000\u0000\u01a8\u01a9\u0005\u0003\u0000"+
		"\u0000\u01a9\u01aa\u0003`0\u0000\u01aa3\u0001\u0000\u0000\u0000\u01ab"+
		"\u01b1\u00036\u001b\u0000\u01ac\u01ad\u0005\u0004\u0000\u0000\u01ad\u01ae"+
		"\u0003(\u0014\u0000\u01ae\u01af\u0005\u0005\u0000\u0000\u01af\u01b1\u0001"+
		"\u0000\u0000\u0000\u01b0\u01ab\u0001\u0000\u0000\u0000\u01b0\u01ac\u0001"+
		"\u0000\u0000\u0000\u01b15\u0001\u0000\u0000\u0000\u01b2\u01b9\u0003\u0004"+
		"\u0002\u0000\u01b3\u01b4\u0005K\u0000\u0000\u01b4\u01b5\u0005\u0017\u0000"+
		"\u0000\u01b5\u01b6\u0005K\u0000\u0000\u01b6\u01b9\u0005\u0019\u0000\u0000"+
		"\u01b7\u01b9\u0005K\u0000\u0000\u01b8\u01b2\u0001\u0000\u0000\u0000\u01b8"+
		"\u01b3\u0001\u0000\u0000\u0000\u01b8\u01b7\u0001\u0000\u0000\u0000\u01b9"+
		"7\u0001\u0000\u0000\u0000\u01ba\u01bb\u00052\u0000\u0000\u01bb\u01bc\u0005"+
		"K\u0000\u0000\u01bc\u01bd\u0005\u001e\u0000\u0000\u01bd\u01be\u0003`0"+
		"\u0000\u01be9\u0001\u0000\u0000\u0000\u01bf\u01c4\u0003>\u001f\u0000\u01c0"+
		"\u01c4\u0003@ \u0000\u01c1\u01c4\u0003B!\u0000\u01c2\u01c4\u0003D\"\u0000"+
		"\u01c3\u01bf\u0001\u0000\u0000\u0000\u01c3\u01c0\u0001\u0000\u0000\u0000"+
		"\u01c3\u01c1\u0001\u0000\u0000\u0000\u01c3\u01c2\u0001\u0000\u0000\u0000"+
		"\u01c4;\u0001\u0000\u0000\u0000\u01c5\u01c6\u0005K\u0000\u0000\u01c6\u01c7"+
		"\u0005\u0003\u0000\u0000\u01c7\u01c8\u0003\n\u0005\u0000\u01c8=\u0001"+
		"\u0000\u0000\u0000\u01c9\u01ca\u0007\u0001\u0000\u0000\u01ca\u01cb\u0005"+
		"K\u0000\u0000\u01cb\u01cc\u0005\u0003\u0000\u0000\u01cc\u01cf\u0003\n"+
		"\u0005\u0000\u01cd\u01ce\u0005\u001e\u0000\u0000\u01ce\u01d0\u0003`0\u0000"+
		"\u01cf\u01cd\u0001\u0000\u0000\u0000\u01cf\u01d0\u0001\u0000\u0000\u0000"+
		"\u01d0?\u0001\u0000\u0000\u0000\u01d1\u01d2\u00057\u0000\u0000\u01d2\u01d3"+
		"\u0005K\u0000\u0000\u01d3\u01d6\u0003H$\u0000\u01d4\u01d5\u0005D\u0000"+
		"\u0000\u01d5\u01d7\u0003H$\u0000\u01d6\u01d4\u0001\u0000\u0000\u0000\u01d6"+
		"\u01d7\u0001\u0000\u0000\u0000\u01d7\u01d8\u0001\u0000\u0000\u0000\u01d8"+
		"\u01dc\u0005\b\u0000\u0000\u01d9\u01db\u0003L&\u0000\u01da\u01d9\u0001"+
		"\u0000\u0000\u0000\u01db\u01de\u0001\u0000\u0000\u0000\u01dc\u01da\u0001"+
		"\u0000\u0000\u0000\u01dc\u01dd\u0001\u0000\u0000\u0000\u01dd\u01df\u0001"+
		"\u0000\u0000\u0000\u01de\u01dc\u0001\u0000\u0000\u0000\u01df\u01e0\u0005"+
		"\t\u0000\u0000\u01e0A\u0001\u0000\u0000\u0000\u01e1\u01e3\u0007\u0002"+
		"\u0000\u0000\u01e2\u01e1\u0001\u0000\u0000\u0000\u01e2\u01e3\u0001\u0000"+
		"\u0000\u0000\u01e3\u01e4\u0001\u0000\u0000\u0000\u01e4\u01e5\u00058\u0000"+
		"\u0000\u01e5\u01e6\u0005K\u0000\u0000\u01e6\u01e9\u0003H$\u0000\u01e7"+
		"\u01e8\u0005D\u0000\u0000\u01e8\u01ea\u0003H$\u0000\u01e9\u01e7\u0001"+
		"\u0000\u0000\u0000\u01e9\u01ea\u0001\u0000\u0000\u0000\u01ea\u01eb\u0001"+
		"\u0000\u0000\u0000\u01eb\u01ef\u0005\b\u0000\u0000\u01ec\u01ee\u0003N"+
		"\'\u0000\u01ed\u01ec\u0001\u0000\u0000\u0000\u01ee\u01f1\u0001\u0000\u0000"+
		"\u0000\u01ef\u01ed\u0001\u0000\u0000\u0000\u01ef\u01f0\u0001\u0000\u0000"+
		"\u0000\u01f0\u01f2\u0001\u0000\u0000\u0000\u01f1\u01ef\u0001\u0000\u0000"+
		"\u0000\u01f2\u01f3\u0005\t\u0000\u0000\u01f3C\u0001\u0000\u0000\u0000"+
		"\u01f4\u01f6\u0007\u0003\u0000\u0000\u01f5\u01f4\u0001\u0000\u0000\u0000"+
		"\u01f5\u01f6\u0001\u0000\u0000\u0000\u01f6\u01f7\u0001\u0000\u0000\u0000"+
		"\u01f7\u01f8\u0005C\u0000\u0000\u01f8\u01f9\u0005K\u0000\u0000\u01f9\u01fa"+
		"\u0003H$\u0000\u01fa\u01fe\u0005\b\u0000\u0000\u01fb\u01fd\u0003F#\u0000"+
		"\u01fc\u01fb\u0001\u0000\u0000\u0000\u01fd\u0200\u0001\u0000\u0000\u0000"+
		"\u01fe\u01fc\u0001\u0000\u0000\u0000\u01fe\u01ff\u0001\u0000\u0000\u0000"+
		"\u01ff\u0201\u0001\u0000\u0000\u0000\u0200\u01fe\u0001\u0000\u0000\u0000"+
		"\u0201\u0202\u0005\t\u0000\u0000\u0202E\u0001\u0000\u0000\u0000\u0203"+
		"\u0206\u0003P(\u0000\u0204\u0206\u0003Z-\u0000\u0205\u0203\u0001\u0000"+
		"\u0000\u0000\u0205\u0204\u0001\u0000\u0000\u0000\u0206G\u0001\u0000\u0000"+
		"\u0000\u0207\u0209\u0005\u0004\u0000\u0000\u0208\u020a\u0003J%\u0000\u0209"+
		"\u0208\u0001\u0000\u0000\u0000\u0209\u020a\u0001\u0000\u0000\u0000\u020a"+
		"\u020f\u0001\u0000\u0000\u0000\u020b\u020c\u0005\u0001\u0000\u0000\u020c"+
		"\u020e\u0003J%\u0000\u020d\u020b\u0001\u0000\u0000\u0000\u020e\u0211\u0001"+
		"\u0000\u0000\u0000\u020f\u020d\u0001\u0000\u0000\u0000\u020f\u0210\u0001"+
		"\u0000\u0000\u0000\u0210\u0212\u0001\u0000\u0000\u0000\u0211\u020f\u0001"+
		"\u0000\u0000\u0000\u0212\u0213\u0005\u0005\u0000\u0000\u0213I\u0001\u0000"+
		"\u0000\u0000\u0214\u0215\u0005K\u0000\u0000\u0215\u0216\u0005\u0003\u0000"+
		"\u0000\u0216\u0217\u0003\n\u0005\u0000\u0217K\u0001\u0000\u0000\u0000"+
		"\u0218\u021d\u0003Z-\u0000\u0219\u021d\u0003R)\u0000\u021a\u021d\u0003"+
		"T*\u0000\u021b\u021d\u0003\\.\u0000\u021c\u0218\u0001\u0000\u0000\u0000"+
		"\u021c\u0219\u0001\u0000\u0000\u0000\u021c\u021a\u0001\u0000\u0000\u0000"+
		"\u021c\u021b\u0001\u0000\u0000\u0000\u021dM\u0001\u0000\u0000\u0000\u021e"+
		"\u0224\u0003P(\u0000\u021f\u0224\u0003Z-\u0000\u0220\u0224\u0003R)\u0000"+
		"\u0221\u0224\u0003T*\u0000\u0222\u0224\u0003\\.\u0000\u0223\u021e\u0001"+
		"\u0000\u0000\u0000\u0223\u021f\u0001\u0000\u0000\u0000\u0223\u0220\u0001"+
		"\u0000\u0000\u0000\u0223\u0221\u0001\u0000\u0000\u0000\u0223\u0222\u0001"+
		"\u0000\u0000\u0000\u0224O\u0001\u0000\u0000\u0000\u0225\u0226\u0005=\u0000"+
		"\u0000\u0226\u0227\u0005\u0003\u0000\u0000\u0227\u0228\u0003`0\u0000\u0228"+
		"Q\u0001\u0000\u0000\u0000\u0229\u022a\u0005>\u0000\u0000\u022a\u022e\u0005"+
		"\u0003\u0000\u0000\u022b\u022d\u0003X,\u0000\u022c\u022b\u0001\u0000\u0000"+
		"\u0000\u022d\u0230\u0001\u0000\u0000\u0000\u022e\u022c\u0001\u0000\u0000"+
		"\u0000\u022e\u022f\u0001\u0000\u0000\u0000\u022fS\u0001\u0000\u0000\u0000"+
		"\u0230\u022e\u0001\u0000\u0000\u0000\u0231\u0232\u0005?\u0000\u0000\u0232"+
		"\u0234\u0005\u0003\u0000\u0000\u0233\u0235\u0003V+\u0000\u0234\u0233\u0001"+
		"\u0000\u0000\u0000\u0235\u0236\u0001\u0000\u0000\u0000\u0236\u0234\u0001"+
		"\u0000\u0000\u0000\u0236\u0237\u0001\u0000\u0000\u0000\u0237U\u0001\u0000"+
		"\u0000\u0000\u0238\u0239\u0003`0\u0000\u0239\u023a\u0005\'\u0000\u0000"+
		"\u023a\u023b\u0003`0\u0000\u023bW\u0001\u0000\u0000\u0000\u023c\u023d"+
		"\u0003\u0088D\u0000\u023d\u023e\u0005\u001e\u0000\u0000\u023e\u023f\u0003"+
		"`0\u0000\u023f\u0258\u0001\u0000\u0000\u0000\u0240\u0241\u0005F\u0000"+
		"\u0000\u0241\u0242\u0005\u0002\u0000\u0000\u0242\u0243\u0005K\u0000\u0000"+
		"\u0243\u0244\u0005\u0006\u0000\u0000\u0244\u0245\u0003`0\u0000\u0245\u0246"+
		"\u0005\u0007\u0000\u0000\u0246\u0247\u0005\u001e\u0000\u0000\u0247\u0248"+
		"\u0003`0\u0000\u0248\u0258\u0001\u0000\u0000\u0000\u0249\u024a\u0005K"+
		"\u0000\u0000\u024a\u024b\u0005\u0006\u0000\u0000\u024b\u024c\u0003`0\u0000"+
		"\u024c\u024d\u0005\u0007\u0000\u0000\u024d\u024e\u0005\u001e\u0000\u0000"+
		"\u024e\u024f\u0003`0\u0000\u024f\u0258\u0001\u0000\u0000\u0000\u0250\u0251"+
		"\u0005#\u0000\u0000\u0251\u0252\u0005K\u0000\u0000\u0252\u0253\u0005\u0003"+
		"\u0000\u0000\u0253\u0254\u0003\n\u0005\u0000\u0254\u0255\u0005\u001e\u0000"+
		"\u0000\u0255\u0256\u0003`0\u0000\u0256\u0258\u0001\u0000\u0000\u0000\u0257"+
		"\u023c\u0001\u0000\u0000\u0000\u0257\u0240\u0001\u0000\u0000\u0000\u0257"+
		"\u0249\u0001\u0000\u0000\u0000\u0257\u0250\u0001\u0000\u0000\u0000\u0258"+
		"Y\u0001\u0000\u0000\u0000\u0259\u025a\u0005@\u0000\u0000\u025a\u025c\u0005"+
		"\u0003\u0000\u0000\u025b\u025d\u0003^/\u0000\u025c\u025b\u0001\u0000\u0000"+
		"\u0000\u025d\u025e\u0001\u0000\u0000\u0000\u025e\u025c\u0001\u0000\u0000"+
		"\u0000\u025e\u025f\u0001\u0000\u0000\u0000\u025f[\u0001\u0000\u0000\u0000"+
		"\u0260\u0261\u0005A\u0000\u0000\u0261\u0263\u0005\u0003\u0000\u0000\u0262"+
		"\u0264\u0003^/\u0000\u0263\u0262\u0001\u0000\u0000\u0000\u0264\u0265\u0001"+
		"\u0000\u0000\u0000\u0265\u0263\u0001\u0000\u0000\u0000\u0265\u0266\u0001"+
		"\u0000\u0000\u0000\u0266]\u0001\u0000\u0000\u0000\u0267\u0269\u0005K\u0000"+
		"\u0000\u0268\u026a\u0003\u0010\b\u0000\u0269\u0268\u0001\u0000\u0000\u0000"+
		"\u0269\u026a\u0001\u0000\u0000\u0000\u026a\u026b\u0001\u0000\u0000\u0000"+
		"\u026b\u0274\u0005\u0004\u0000\u0000\u026c\u0271\u0003`0\u0000\u026d\u026e"+
		"\u0005\u0001\u0000\u0000\u026e\u0270\u0003`0\u0000\u026f\u026d\u0001\u0000"+
		"\u0000\u0000\u0270\u0273\u0001\u0000\u0000\u0000\u0271\u026f\u0001\u0000"+
		"\u0000\u0000\u0271\u0272\u0001\u0000\u0000\u0000\u0272\u0275\u0001\u0000"+
		"\u0000\u0000\u0273\u0271\u0001\u0000\u0000\u0000\u0274\u026c\u0001\u0000"+
		"\u0000\u0000\u0274\u0275\u0001\u0000\u0000\u0000\u0275\u0276\u0001\u0000"+
		"\u0000\u0000\u0276\u0277\u0005\u0005\u0000\u0000\u0277_\u0001\u0000\u0000"+
		"\u0000\u0278\u0279\u00060\uffff\uffff\u0000\u0279\u02d1\u0003l6\u0000"+
		"\u027a\u027b\u0005\u0004\u0000\u0000\u027b\u027c\u0003`0\u0000\u027c\u027d"+
		"\u0005\u0005\u0000\u0000\u027d\u02d1\u0001\u0000\u0000\u0000\u027e\u02d1"+
		"\u0003n7\u0000\u027f\u02d1\u0003z=\u0000\u0280\u02d1\u0003x<\u0000\u0281"+
		"\u02d1\u0003\u0088D\u0000\u0282\u02d1\u0003\u0084B\u0000\u0283\u02d1\u0003"+
		"~?\u0000\u0284\u02d1\u0003j5\u0000\u0285\u0286\u0005\u0011\u0000\u0000"+
		"\u0286\u02d1\u0003`0\u001a\u0287\u0288\u0005\r\u0000\u0000\u0288\u02d1"+
		"\u0003`0\u0019\u0289\u028a\u0005\u000f\u0000\u0000\u028a\u02d1\u0003`"+
		"0\u0018\u028b\u028c\u0005!\u0000\u0000\u028c\u028d\u0005\u0004\u0000\u0000"+
		"\u028d\u028e\u0003`0\u0000\u028e\u0294\u0005\u0005\u0000\u0000\u028f\u0290"+
		"\u0005\b\u0000\u0000\u0290\u0291\u0003`0\u0000\u0291\u0292\u0005\t\u0000"+
		"\u0000\u0292\u0295\u0001\u0000\u0000\u0000\u0293\u0295\u0003`0\u0000\u0294"+
		"\u028f\u0001\u0000\u0000\u0000\u0294\u0293\u0001\u0000\u0000\u0000\u0295"+
		"\u0296\u0001\u0000\u0000\u0000\u0296\u029c\u0005\"\u0000\u0000\u0297\u0298"+
		"\u0005\b\u0000\u0000\u0298\u0299\u0003`0\u0000\u0299\u029a\u0005\t\u0000"+
		"\u0000\u029a\u029d\u0001\u0000\u0000\u0000\u029b\u029d\u0003`0\u0000\u029c"+
		"\u0297\u0001\u0000\u0000\u0000\u029c\u029b\u0001\u0000\u0000\u0000\u029d"+
		"\u02d1\u0001\u0000\u0000\u0000\u029e\u029f\u0005#\u0000\u0000\u029f\u02a0"+
		"\u0005\u0004\u0000\u0000\u02a0\u02a1\u0005K\u0000\u0000\u02a1\u02a2\u0005"+
		"\u0003\u0000\u0000\u02a2\u02a3\u0003\n\u0005\u0000\u02a3\u02a4\u0005\u001e"+
		"\u0000\u0000\u02a4\u02a5\u0003`0\u0000\u02a5\u02ab\u0005\u0005\u0000\u0000"+
		"\u02a6\u02a7\u0005\b\u0000\u0000\u02a7\u02a8\u0003`0\u0000\u02a8\u02a9"+
		"\u0005\t\u0000\u0000\u02a9\u02ac\u0001\u0000\u0000\u0000\u02aa\u02ac\u0003"+
		"`0\u0000\u02ab\u02a6\u0001\u0000\u0000\u0000\u02ab\u02aa\u0001\u0000\u0000"+
		"\u0000\u02ac\u02d1\u0001\u0000\u0000\u0000\u02ad\u02ae\u0005$\u0000\u0000"+
		"\u02ae\u02af\u0005\u0004\u0000\u0000\u02af\u02b0\u0003`0\u0000\u02b0\u02b1"+
		"\u0005\u0005\u0000\u0000\u02b1\u02b3\u0005\b\u0000\u0000\u02b2\u02b4\u0003"+
		"b1\u0000\u02b3\u02b2\u0001\u0000\u0000\u0000\u02b4\u02b5\u0001\u0000\u0000"+
		"\u0000\u02b5\u02b3\u0001\u0000\u0000\u0000\u02b5\u02b6\u0001\u0000\u0000"+
		"\u0000\u02b6\u02b7\u0001\u0000\u0000\u0000\u02b7\u02b8\u0005\t\u0000\u0000"+
		"\u02b8\u02d1\u0001\u0000\u0000\u0000\u02b9\u02ba\u0005$\u0000\u0000\u02ba"+
		"\u02bc\u0005\b\u0000\u0000\u02bb\u02bd\u0003d2\u0000\u02bc\u02bb\u0001"+
		"\u0000\u0000\u0000\u02bd\u02be\u0001\u0000\u0000\u0000\u02be\u02bc\u0001"+
		"\u0000\u0000\u0000\u02be\u02bf\u0001\u0000\u0000\u0000\u02bf\u02c0\u0001"+
		"\u0000\u0000\u0000\u02c0\u02c1\u0005\t\u0000\u0000\u02c1\u02d1\u0001\u0000"+
		"\u0000\u0000\u02c2\u02c3\u00053\u0000\u0000\u02c3\u02c4\u0005K\u0000\u0000"+
		"\u02c4\u02c5\u0005\u0003\u0000\u0000\u02c5\u02c6\u0003\n\u0005\u0000\u02c6"+
		"\u02c7\u0005\u0001\u0000\u0000\u02c7\u02c8\u0003`0\u0002\u02c8\u02d1\u0001"+
		"\u0000\u0000\u0000\u02c9\u02ca\u00054\u0000\u0000\u02ca\u02cb\u0005K\u0000"+
		"\u0000\u02cb\u02cc\u0005\u0003\u0000\u0000\u02cc\u02cd\u0003\n\u0005\u0000"+
		"\u02cd\u02ce\u0005\u0001\u0000\u0000\u02ce\u02cf\u0003`0\u0001\u02cf\u02d1"+
		"\u0001\u0000\u0000\u0000\u02d0\u0278\u0001\u0000\u0000\u0000\u02d0\u027a"+
		"\u0001\u0000\u0000\u0000\u02d0\u027e\u0001\u0000\u0000\u0000\u02d0\u027f"+
		"\u0001\u0000\u0000\u0000\u02d0\u0280\u0001\u0000\u0000\u0000\u02d0\u0281"+
		"\u0001\u0000\u0000\u0000\u02d0\u0282\u0001\u0000\u0000\u0000\u02d0\u0283"+
		"\u0001\u0000\u0000\u0000\u02d0\u0284\u0001\u0000\u0000\u0000\u02d0\u0285"+
		"\u0001\u0000\u0000\u0000\u02d0\u0287\u0001\u0000\u0000\u0000\u02d0\u0289"+
		"\u0001\u0000\u0000\u0000\u02d0\u028b\u0001\u0000\u0000\u0000\u02d0\u029e"+
		"\u0001\u0000\u0000\u0000\u02d0\u02ad\u0001\u0000\u0000\u0000\u02d0\u02b9"+
		"\u0001\u0000\u0000\u0000\u02d0\u02c2\u0001\u0000\u0000\u0000\u02d0\u02c9"+
		"\u0001\u0000\u0000\u0000\u02d1\u0307\u0001\u0000\u0000\u0000\u02d2\u02d3"+
		"\n\u0017\u0000\u0000\u02d3\u02d4\u0005\u0012\u0000\u0000\u02d4\u0306\u0003"+
		"`0\u0018\u02d5\u02d6\n\u0016\u0000\u0000\u02d6\u02d7\u0005\u0013\u0000"+
		"\u0000\u02d7\u0306\u0003`0\u0017\u02d8\u02d9\n\u0015\u0000\u0000\u02d9"+
		"\u02da\u0005\u0014\u0000\u0000\u02da\u0306\u0003`0\u0016\u02db\u02dc\n"+
		"\u0014\u0000\u0000\u02dc\u02dd\u0005\u0015\u0000\u0000\u02dd\u0306\u0003"+
		"`0\u0015\u02de\u02df\n\u0013\u0000\u0000\u02df\u02e0\u0005\u0016\u0000"+
		"\u0000\u02e0\u0306\u0003`0\u0014\u02e1\u02e2\n\u0012\u0000\u0000\u02e2"+
		"\u02e3\u0005\u0017\u0000\u0000\u02e3\u0306\u0003`0\u0013\u02e4\u02e5\n"+
		"\u0011\u0000\u0000\u02e5\u02e6\u0005\u0018\u0000\u0000\u02e6\u0306\u0003"+
		"`0\u0012\u02e7\u02e8\n\u0010\u0000\u0000\u02e8\u02e9\u0005\u0019\u0000"+
		"\u0000\u02e9\u0306\u0003`0\u0011\u02ea\u02eb\n\u000f\u0000\u0000\u02eb"+
		"\u02ec\u0005\u001a\u0000\u0000\u02ec\u0306\u0003`0\u0010\u02ed\u02ee\n"+
		"\u000e\u0000\u0000\u02ee\u02ef\u0005%\u0000\u0000\u02ef\u0306\u0003`0"+
		"\u000f\u02f0\u02f1\n\r\u0000\u0000\u02f1\u02f2\u0005\u001d\u0000\u0000"+
		"\u02f2\u0306\u0003`0\u000e\u02f3\u02f4\n\f\u0000\u0000\u02f4\u02f5\u0005"+
		"\u001b\u0000\u0000\u02f5\u0306\u0003`0\r\u02f6\u02f7\n\u000b\u0000\u0000"+
		"\u02f7\u02f8\u0005\u001c\u0000\u0000\u02f8\u0306\u0003`0\f\u02f9\u02fa"+
		"\n\n\u0000\u0000\u02fa\u02fb\u0005\r\u0000\u0000\u02fb\u0306\u0003`0\u000b"+
		"\u02fc\u02fd\n\t\u0000\u0000\u02fd\u02fe\u0005\u000f\u0000\u0000\u02fe"+
		"\u0306\u0003`0\n\u02ff\u0300\n\b\u0000\u0000\u0300\u0301\u0005\u001f\u0000"+
		"\u0000\u0301\u0306\u0003`0\b\u0302\u0303\n\u0007\u0000\u0000\u0303\u0304"+
		"\u0005 \u0000\u0000\u0304\u0306\u0003`0\b\u0305\u02d2\u0001\u0000\u0000"+
		"\u0000\u0305\u02d5\u0001\u0000\u0000\u0000\u0305\u02d8\u0001\u0000\u0000"+
		"\u0000\u0305\u02db\u0001\u0000\u0000\u0000\u0305\u02de\u0001\u0000\u0000"+
		"\u0000\u0305\u02e1\u0001\u0000\u0000\u0000\u0305\u02e4\u0001\u0000\u0000"+
		"\u0000\u0305\u02e7\u0001\u0000\u0000\u0000\u0305\u02ea\u0001\u0000\u0000"+
		"\u0000\u0305\u02ed\u0001\u0000\u0000\u0000\u0305\u02f0\u0001\u0000\u0000"+
		"\u0000\u0305\u02f3\u0001\u0000\u0000\u0000\u0305\u02f6\u0001\u0000\u0000"+
		"\u0000\u0305\u02f9\u0001\u0000\u0000\u0000\u0305\u02fc\u0001\u0000\u0000"+
		"\u0000\u0305\u02ff\u0001\u0000\u0000\u0000\u0305\u0302\u0001\u0000\u0000"+
		"\u0000\u0306\u0309\u0001\u0000\u0000\u0000\u0307\u0305\u0001\u0000\u0000"+
		"\u0000\u0307\u0308\u0001\u0000\u0000\u0000\u0308a\u0001\u0000\u0000\u0000"+
		"\u0309\u0307\u0001\u0000\u0000\u0000\u030a\u030b\u0003f3\u0000\u030b\u030c"+
		"\u0005\'\u0000\u0000\u030c\u030d\u0003`0\u0000\u030d\u0312\u0001\u0000"+
		"\u0000\u0000\u030e\u030f\u0005\"\u0000\u0000\u030f\u0310\u0005\'\u0000"+
		"\u0000\u0310\u0312\u0003`0\u0000\u0311\u030a\u0001\u0000\u0000\u0000\u0311"+
		"\u030e\u0001\u0000\u0000\u0000\u0312c\u0001\u0000\u0000\u0000\u0313\u0314"+
		"\u0003`0\u0000\u0314\u0315\u0005\'\u0000\u0000\u0315\u0316\u0003`0\u0000"+
		"\u0316\u031b\u0001\u0000\u0000\u0000\u0317\u0318\u0005\"\u0000\u0000\u0318"+
		"\u0319\u0005\'\u0000\u0000\u0319\u031b\u0003`0\u0000\u031a\u0313\u0001"+
		"\u0000\u0000\u0000\u031a\u0317\u0001\u0000\u0000\u0000\u031be\u0001\u0000"+
		"\u0000\u0000\u031c\u031f\u0003l6\u0000\u031d\u031f\u0003\u0084B\u0000"+
		"\u031e\u031c\u0001\u0000\u0000\u0000\u031e\u031d\u0001\u0000\u0000\u0000"+
		"\u031fg\u0001\u0000\u0000\u0000\u0320\u0321\u00064\uffff\uffff\u0000\u0321"+
		"\u032c\u0003\u0004\u0002\u0000\u0322\u0323\u0005K\u0000\u0000\u0323\u0324"+
		"\u0005\u0017\u0000\u0000\u0324\u0325\u0005K\u0000\u0000\u0325\u032c\u0005"+
		"\u0019\u0000\u0000\u0326\u032c\u0005K\u0000\u0000\u0327\u0328\u0005\u0004"+
		"\u0000\u0000\u0328\u0329\u0003h4\u0000\u0329\u032a\u0005\u0005\u0000\u0000"+
		"\u032a\u032c\u0001\u0000\u0000\u0000\u032b\u0320\u0001\u0000\u0000\u0000"+
		"\u032b\u0322\u0001\u0000\u0000\u0000\u032b\u0326\u0001\u0000\u0000\u0000"+
		"\u032b\u0327\u0001\u0000\u0000\u0000\u032c\u0332\u0001\u0000\u0000\u0000"+
		"\u032d\u032e\n\u0001\u0000\u0000\u032e\u032f\u0005\n\u0000\u0000\u032f"+
		"\u0331\u0003h4\u0002\u0330\u032d\u0001\u0000\u0000\u0000\u0331\u0334\u0001"+
		"\u0000\u0000\u0000\u0332\u0330\u0001\u0000\u0000\u0000\u0332\u0333\u0001"+
		"\u0000\u0000\u0000\u0333i\u0001\u0000\u0000\u0000\u0334\u0332\u0001\u0000"+
		"\u0000\u0000\u0335\u0336\u0005K\u0000\u0000\u0336\u0337\u0005\u0002\u0000"+
		"\u0000\u0337\u0338\u0005K\u0000\u0000\u0338\u0339\u0005\u0010\u0000\u0000"+
		"\u0339\u033a\u0005K\u0000\u0000\u033a\u033b\u0005\u0002\u0000\u0000\u033b"+
		"\u033c\u0005K\u0000\u0000\u033ck\u0001\u0000\u0000\u0000\u033d\u033e\u0007"+
		"\u0004\u0000\u0000\u033em\u0001\u0000\u0000\u0000\u033f\u0343\u0003p8"+
		"\u0000\u0340\u0343\u0003r9\u0000\u0341\u0343\u0003t:\u0000\u0342\u033f"+
		"\u0001\u0000\u0000\u0000\u0342\u0340\u0001\u0000\u0000\u0000\u0342\u0341"+
		"\u0001\u0000\u0000\u0000\u0343o\u0001\u0000\u0000\u0000\u0344\u0346\u0005"+
		"*\u0000\u0000\u0345\u0347\u0003\u0010\b\u0000\u0346\u0345\u0001\u0000"+
		"\u0000\u0000\u0346\u0347\u0001\u0000\u0000\u0000\u0347\u0348\u0001\u0000"+
		"\u0000\u0000\u0348\u0351\u0005\u0004\u0000\u0000\u0349\u034e\u0003`0\u0000"+
		"\u034a\u034b\u0005\u0001\u0000\u0000\u034b\u034d\u0003`0\u0000\u034c\u034a"+
		"\u0001\u0000\u0000\u0000\u034d\u0350\u0001\u0000\u0000\u0000\u034e\u034c"+
		"\u0001\u0000\u0000\u0000\u034e\u034f\u0001\u0000\u0000\u0000\u034f\u0352"+
		"\u0001\u0000\u0000\u0000\u0350\u034e\u0001\u0000\u0000\u0000\u0351\u0349"+
		"\u0001\u0000\u0000\u0000\u0351\u0352\u0001\u0000\u0000\u0000\u0352\u0353"+
		"\u0001\u0000\u0000\u0000\u0353\u0354\u0005\u0005\u0000\u0000\u0354q\u0001"+
		"\u0000\u0000\u0000\u0355\u0357\u0005+\u0000\u0000\u0356\u0358\u0003\u0010"+
		"\b\u0000\u0357\u0356\u0001\u0000\u0000\u0000\u0357\u0358\u0001\u0000\u0000"+
		"\u0000\u0358\u0359\u0001\u0000\u0000\u0000\u0359\u0362\u0005\u0004\u0000"+
		"\u0000\u035a\u035f\u0003`0\u0000\u035b\u035c\u0005\u0001\u0000\u0000\u035c"+
		"\u035e\u0003`0\u0000\u035d\u035b\u0001\u0000\u0000\u0000\u035e\u0361\u0001"+
		"\u0000\u0000\u0000\u035f\u035d\u0001\u0000\u0000\u0000\u035f\u0360\u0001"+
		"\u0000\u0000\u0000\u0360\u0363\u0001\u0000\u0000\u0000\u0361\u035f\u0001"+
		"\u0000\u0000\u0000\u0362\u035a\u0001\u0000\u0000\u0000\u0362\u0363\u0001"+
		"\u0000\u0000\u0000\u0363\u0364\u0001\u0000\u0000\u0000\u0364\u0365\u0005"+
		"\u0005\u0000\u0000\u0365s\u0001\u0000\u0000\u0000\u0366\u0368\u0005,\u0000"+
		"\u0000\u0367\u0369\u0003\u0010\b\u0000\u0368\u0367\u0001\u0000\u0000\u0000"+
		"\u0368\u0369\u0001\u0000\u0000\u0000\u0369\u036a\u0001\u0000\u0000\u0000"+
		"\u036a\u0373\u0005\u0004\u0000\u0000\u036b\u0370\u0003v;\u0000\u036c\u036d"+
		"\u0005\u0001\u0000\u0000\u036d\u036f\u0003v;\u0000\u036e\u036c\u0001\u0000"+
		"\u0000\u0000\u036f\u0372\u0001\u0000\u0000\u0000\u0370\u036e\u0001\u0000"+
		"\u0000\u0000\u0370\u0371\u0001\u0000\u0000\u0000\u0371\u0374\u0001\u0000"+
		"\u0000\u0000\u0372\u0370\u0001\u0000\u0000\u0000\u0373\u036b\u0001\u0000"+
		"\u0000\u0000\u0373\u0374\u0001\u0000\u0000\u0000\u0374\u0375\u0001\u0000"+
		"\u0000\u0000\u0375\u0376\u0005\u0005\u0000\u0000\u0376u\u0001\u0000\u0000"+
		"\u0000\u0377\u0378\u0003`0\u0000\u0378\u0379\u0005&\u0000\u0000\u0379"+
		"\u037a\u0003`0\u0000\u037aw\u0001\u0000\u0000\u0000\u037b\u0383\u0006"+
		"<\uffff\uffff\u0000\u037c\u0384\u0003~?\u0000\u037d\u0384\u0003\u0088"+
		"D\u0000\u037e\u0384\u0003n7\u0000\u037f\u0380\u0005\u0004\u0000\u0000"+
		"\u0380\u0381\u0003`0\u0000\u0381\u0382\u0005\u0005\u0000\u0000\u0382\u0384"+
		"\u0001\u0000\u0000\u0000\u0383\u037c\u0001\u0000\u0000\u0000\u0383\u037d"+
		"\u0001\u0000\u0000\u0000\u0383\u037e\u0001\u0000\u0000\u0000\u0383\u037f"+
		"\u0001\u0000\u0000\u0000\u0384\u0385\u0001\u0000\u0000\u0000\u0385\u0386"+
		"\u0005\u0006\u0000\u0000\u0386\u0387\u0003`0\u0000\u0387\u0388\u0005\u0007"+
		"\u0000\u0000\u0388\u03ad\u0001\u0000\u0000\u0000\u0389\u0391\u0003~?\u0000"+
		"\u038a\u0391\u0003\u0088D\u0000\u038b\u0391\u0003n7\u0000\u038c\u038d"+
		"\u0005\u0004\u0000\u0000\u038d\u038e\u0003`0\u0000\u038e\u038f\u0005\u0005"+
		"\u0000\u0000\u038f\u0391\u0001\u0000\u0000\u0000\u0390\u0389\u0001\u0000"+
		"\u0000\u0000\u0390\u038a\u0001\u0000\u0000\u0000\u0390\u038b\u0001\u0000"+
		"\u0000\u0000\u0390\u038c\u0001\u0000\u0000\u0000\u0391\u0392\u0001\u0000"+
		"\u0000\u0000\u0392\u0393\u0005\u0002\u0000\u0000\u0393\u0394\u0005K\u0000"+
		"\u0000\u0394\u039d\u0005\u0004\u0000\u0000\u0395\u039a\u0003\u0080@\u0000"+
		"\u0396\u0397\u0005\u0001\u0000\u0000\u0397\u0399\u0003\u0080@\u0000\u0398"+
		"\u0396\u0001\u0000\u0000\u0000\u0399\u039c\u0001\u0000\u0000\u0000\u039a"+
		"\u0398\u0001\u0000\u0000\u0000\u039a\u039b\u0001\u0000\u0000\u0000\u039b"+
		"\u039e\u0001\u0000\u0000\u0000\u039c\u039a\u0001\u0000\u0000\u0000\u039d"+
		"\u0395\u0001\u0000\u0000\u0000\u039d\u039e\u0001\u0000\u0000\u0000\u039e"+
		"\u039f\u0001\u0000\u0000\u0000\u039f\u03a0\u0005\u0005\u0000\u0000\u03a0"+
		"\u03ad\u0001\u0000\u0000\u0000\u03a1\u03a8\u0003~?\u0000\u03a2\u03a8\u0003"+
		"n7\u0000\u03a3\u03a4\u0005\u0004\u0000\u0000\u03a4\u03a5\u0003`0\u0000"+
		"\u03a5\u03a6\u0005\u0005\u0000\u0000\u03a6\u03a8\u0001\u0000\u0000\u0000"+
		"\u03a7\u03a1\u0001\u0000\u0000\u0000\u03a7\u03a2\u0001\u0000\u0000\u0000"+
		"\u03a7\u03a3\u0001\u0000\u0000\u0000\u03a8\u03a9\u0001\u0000\u0000\u0000"+
		"\u03a9\u03aa\u0005\u0002\u0000\u0000\u03aa\u03ab\u0005K\u0000\u0000\u03ab"+
		"\u03ad\u0001\u0000\u0000\u0000\u03ac\u037b\u0001\u0000\u0000\u0000\u03ac"+
		"\u0390\u0001\u0000\u0000\u0000\u03ac\u03a7\u0001\u0000\u0000\u0000\u03ad"+
		"\u03c7\u0001\u0000\u0000\u0000\u03ae\u03af\n\u0006\u0000\u0000\u03af\u03b0"+
		"\u0005\u0006\u0000\u0000\u03b0\u03b1\u0003`0\u0000\u03b1\u03b2\u0005\u0007"+
		"\u0000\u0000\u03b2\u03c6\u0001\u0000\u0000\u0000\u03b3\u03b4\n\u0005\u0000"+
		"\u0000\u03b4\u03b5\u0005\u0002\u0000\u0000\u03b5\u03b6\u0005K\u0000\u0000"+
		"\u03b6\u03bf\u0005\u0004\u0000\u0000\u03b7\u03bc\u0003\u0080@\u0000\u03b8"+
		"\u03b9\u0005\u0001\u0000\u0000\u03b9\u03bb\u0003\u0080@\u0000\u03ba\u03b8"+
		"\u0001\u0000\u0000\u0000\u03bb\u03be\u0001\u0000\u0000\u0000\u03bc\u03ba"+
		"\u0001\u0000\u0000\u0000\u03bc\u03bd\u0001\u0000\u0000\u0000\u03bd\u03c0"+
		"\u0001\u0000\u0000\u0000\u03be\u03bc\u0001\u0000\u0000\u0000\u03bf\u03b7"+
		"\u0001\u0000\u0000\u0000\u03bf\u03c0\u0001\u0000\u0000\u0000\u03c0\u03c1"+
		"\u0001\u0000\u0000\u0000\u03c1\u03c6\u0005\u0005\u0000\u0000\u03c2\u03c3"+
		"\n\u0004\u0000\u0000\u03c3\u03c4\u0005\u0002\u0000\u0000\u03c4\u03c6\u0005"+
		"K\u0000\u0000\u03c5\u03ae\u0001\u0000\u0000\u0000\u03c5\u03b3\u0001\u0000"+
		"\u0000\u0000\u03c5\u03c2\u0001\u0000\u0000\u0000\u03c6\u03c9\u0001\u0000"+
		"\u0000\u0000\u03c7\u03c5\u0001\u0000\u0000\u0000\u03c7\u03c8\u0001\u0000"+
		"\u0000\u0000\u03c8y\u0001\u0000\u0000\u0000\u03c9\u03c7\u0001\u0000\u0000"+
		"\u0000\u03ca\u03cf\u0003|>\u0000\u03cb\u03cc\u0005\u0002\u0000\u0000\u03cc"+
		"\u03ce\u0005K\u0000\u0000\u03cd\u03cb\u0001\u0000\u0000\u0000\u03ce\u03d1"+
		"\u0001\u0000\u0000\u0000\u03cf\u03cd\u0001\u0000\u0000\u0000\u03cf\u03d0"+
		"\u0001\u0000\u0000\u0000\u03d0{\u0001\u0000\u0000\u0000\u03d1\u03cf\u0001"+
		"\u0000\u0000\u0000\u03d2\u03d5\u0005F\u0000\u0000\u03d3\u03d4\u0005\u0002"+
		"\u0000\u0000\u03d4\u03d6\u0005K\u0000\u0000\u03d5\u03d3\u0001\u0000\u0000"+
		"\u0000\u03d6\u03d7\u0001\u0000\u0000\u0000\u03d7\u03d5\u0001\u0000\u0000"+
		"\u0000\u03d7\u03d8\u0001\u0000\u0000\u0000\u03d8\u03d9\u0001\u0000\u0000"+
		"\u0000\u03d9\u03e2\u0005\u0004\u0000\u0000\u03da\u03df\u0003\u0080@\u0000"+
		"\u03db\u03dc\u0005\u0001\u0000\u0000\u03dc\u03de\u0003\u0080@\u0000\u03dd"+
		"\u03db\u0001\u0000\u0000\u0000\u03de\u03e1\u0001\u0000\u0000\u0000\u03df"+
		"\u03dd\u0001\u0000\u0000\u0000\u03df\u03e0\u0001\u0000\u0000\u0000\u03e0"+
		"\u03e3\u0001\u0000\u0000\u0000\u03e1\u03df\u0001\u0000\u0000\u0000\u03e2"+
		"\u03da\u0001\u0000\u0000\u0000\u03e2\u03e3\u0001\u0000\u0000\u0000\u03e3"+
		"\u03e4\u0001\u0000\u0000\u0000\u03e4\u03f9\u0005\u0005\u0000\u0000\u03e5"+
		"\u03e8\u0005K\u0000\u0000\u03e6\u03e7\u0005\u0002\u0000\u0000\u03e7\u03e9"+
		"\u0005K\u0000\u0000\u03e8\u03e6\u0001\u0000\u0000\u0000\u03e9\u03ea\u0001"+
		"\u0000\u0000\u0000\u03ea\u03e8\u0001\u0000\u0000\u0000\u03ea\u03eb\u0001"+
		"\u0000\u0000\u0000\u03eb\u03ec\u0001\u0000\u0000\u0000\u03ec\u03f5\u0005"+
		"\u0004\u0000\u0000\u03ed\u03f2\u0003\u0080@\u0000\u03ee\u03ef\u0005\u0001"+
		"\u0000\u0000\u03ef\u03f1\u0003\u0080@\u0000\u03f0\u03ee\u0001\u0000\u0000"+
		"\u0000\u03f1\u03f4\u0001\u0000\u0000\u0000\u03f2\u03f0\u0001\u0000\u0000"+
		"\u0000\u03f2\u03f3\u0001\u0000\u0000\u0000\u03f3\u03f6\u0001\u0000\u0000"+
		"\u0000\u03f4\u03f2\u0001\u0000\u0000\u0000\u03f5\u03ed\u0001\u0000\u0000"+
		"\u0000\u03f5\u03f6\u0001\u0000\u0000\u0000\u03f6\u03f7\u0001\u0000\u0000"+
		"\u0000\u03f7\u03f9\u0005\u0005\u0000\u0000\u03f8\u03d2\u0001\u0000\u0000"+
		"\u0000\u03f8\u03e5\u0001\u0000\u0000\u0000\u03f9}\u0001\u0000\u0000\u0000"+
		"\u03fa\u03fc\u0005K\u0000\u0000\u03fb\u03fd\u0003\u0010\b\u0000\u03fc"+
		"\u03fb\u0001\u0000\u0000\u0000\u03fc\u03fd\u0001\u0000\u0000\u0000\u03fd"+
		"\u03fe\u0001\u0000\u0000\u0000\u03fe\u0407\u0005\u0004\u0000\u0000\u03ff"+
		"\u0404\u0003\u0080@\u0000\u0400\u0401\u0005\u0001\u0000\u0000\u0401\u0403"+
		"\u0003\u0080@\u0000\u0402\u0400\u0001\u0000\u0000\u0000\u0403\u0406\u0001"+
		"\u0000\u0000\u0000\u0404\u0402\u0001\u0000\u0000\u0000\u0404\u0405\u0001"+
		"\u0000\u0000\u0000\u0405\u0408\u0001\u0000\u0000\u0000\u0406\u0404\u0001"+
		"\u0000\u0000\u0000\u0407\u03ff\u0001\u0000\u0000\u0000\u0407\u0408\u0001"+
		"\u0000\u0000\u0000\u0408\u0409\u0001\u0000\u0000\u0000\u0409\u040a\u0005"+
		"\u0005\u0000\u0000\u040a\u007f\u0001\u0000\u0000\u0000\u040b\u040e\u0003"+
		"\u0082A\u0000\u040c\u040e\u0003`0\u0000\u040d\u040b\u0001\u0000\u0000"+
		"\u0000\u040d\u040c\u0001\u0000\u0000\u0000\u040e\u0081\u0001\u0000\u0000"+
		"\u0000\u040f\u0410\u0005K\u0000\u0000\u0410\u0411\u0005\'\u0000\u0000"+
		"\u0411\u041a\u0003`0\u0000\u0412\u0413\u0005\u0004\u0000\u0000\u0413\u0414"+
		"\u0005K\u0000\u0000\u0414\u0415\u0005\u0001\u0000\u0000\u0415\u0416\u0005"+
		"K\u0000\u0000\u0416\u0417\u0005\u0005\u0000\u0000\u0417\u0418\u0005\'"+
		"\u0000\u0000\u0418\u041a\u0003`0\u0000\u0419\u040f\u0001\u0000\u0000\u0000"+
		"\u0419\u0412\u0001\u0000\u0000\u0000\u041a\u0083\u0001\u0000\u0000\u0000"+
		"\u041b\u041c\u0003\n\u0005\u0000\u041c\u041d\u0005\b\u0000\u0000\u041d"+
		"\u0422\u0003\u0086C\u0000\u041e\u041f\u0005\u0001\u0000\u0000\u041f\u0421"+
		"\u0003\u0086C\u0000\u0420\u041e\u0001\u0000\u0000\u0000\u0421\u0424\u0001"+
		"\u0000\u0000\u0000\u0422\u0420\u0001\u0000\u0000\u0000\u0422\u0423\u0001"+
		"\u0000\u0000\u0000\u0423\u0425\u0001\u0000\u0000\u0000\u0424\u0422\u0001"+
		"\u0000\u0000\u0000\u0425\u0426\u0005\t\u0000\u0000\u0426\u0085\u0001\u0000"+
		"\u0000\u0000\u0427\u0428\u0005K\u0000\u0000\u0428\u0429\u0005\u001e\u0000"+
		"\u0000\u0429\u042a\u0003`0\u0000\u042a\u0087\u0001\u0000\u0000\u0000\u042b"+
		"\u042c\u0005F\u0000\u0000\u042c\u042d\u0005\u0002\u0000\u0000\u042d\u0432"+
		"\u0005K\u0000\u0000\u042e\u042f\u0005\u0002\u0000\u0000\u042f\u0431\u0005"+
		"K\u0000\u0000\u0430\u042e\u0001\u0000\u0000\u0000\u0431\u0434\u0001\u0000"+
		"\u0000\u0000\u0432\u0430\u0001\u0000\u0000\u0000\u0432\u0433\u0001\u0000"+
		"\u0000\u0000\u0433\u043e\u0001\u0000\u0000\u0000\u0434\u0432\u0001\u0000"+
		"\u0000\u0000\u0435\u043a\u0005K\u0000\u0000\u0436\u0437\u0005\u0002\u0000"+
		"\u0000\u0437\u0439\u0005K\u0000\u0000\u0438\u0436\u0001\u0000\u0000\u0000"+
		"\u0439\u043c\u0001\u0000\u0000\u0000\u043a\u0438\u0001\u0000\u0000\u0000"+
		"\u043a\u043b\u0001\u0000\u0000\u0000\u043b\u043e\u0001\u0000\u0000\u0000"+
		"\u043c\u043a\u0001\u0000\u0000\u0000\u043d\u042b\u0001\u0000\u0000\u0000"+
		"\u043d\u0435\u0001\u0000\u0000\u0000\u043e\u0089\u0001\u0000\u0000\u0000"+
		"o\u008c\u008e\u009b\u00a0\u00a4\u00a9\u00ad\u00b1\u00b4\u00be\u00c3\u00c7"+
		"\u00cd\u00d5\u00e0\u00e8\u00f1\u00f9\u0101\u0106\u010c\u0116\u0120\u012f"+
		"\u013a\u0140\u0153\u0158\u0164\u0169\u0170\u0187\u018b\u0193\u0199\u019c"+
		"\u01a4\u01b0\u01b8\u01c3\u01cf\u01d6\u01dc\u01e2\u01e9\u01ef\u01f5\u01fe"+
		"\u0205\u0209\u020f\u021c\u0223\u022e\u0236\u0257\u025e\u0265\u0269\u0271"+
		"\u0274\u0294\u029c\u02ab\u02b5\u02be\u02d0\u0305\u0307\u0311\u031a\u031e"+
		"\u032b\u0332\u0342\u0346\u034e\u0351\u0357\u035f\u0362\u0368\u0370\u0373"+
		"\u0383\u0390\u039a\u039d\u03a7\u03ac\u03bc\u03bf\u03c5\u03c7\u03cf\u03d7"+
		"\u03df\u03e2\u03ea\u03f2\u03f5\u03f8\u03fc\u0404\u0407\u040d\u0419\u0422"+
		"\u0432\u043a\u043d";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}