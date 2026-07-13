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
		RCURLY=9, PARALLEL=10, TRUE=11, FALSE=12, AND=13, OR=14, NOT=15, TIMES=16, 
		DIV=17, MOD=18, PLUS=19, MINUS=20, LT=21, LTE=22, GT=23, GTE=24, EQ=25, 
		NEQ=26, ASGN_EQ=27, IMPLIES=28, IF=29, ELSE=30, LET=31, WHEN=32, IN=33, 
		ARROW=34, IMPORT=35, PCLASS=36, OCLASS=37, PROC=38, PROGRAM=39, SPEC=40, 
		VAR=41, CONST=42, CONSTRUCTOR=43, TRANSITION=44, SERVICE=45, CONSUMER=46, 
		GUARD=47, TRANSIT=48, ERROR=49, EFFECT=50, FUN=51, REAL=52, INT=53, ID=54, 
		STRING=55, WS=56, COMMENT=57, LINE_COMMENT=58;
	public static final int
		RULE_root = 0, RULE_import_stmt = 1, RULE_qualified_name = 2, RULE_qual_segment = 3, 
		RULE_decl = 4, RULE_typeExpr = 5, RULE_typeArgs = 6, RULE_typeParams = 7, 
		RULE_fun_decl = 8, RULE_pclass = 9, RULE_oclass = 10, RULE_proc = 11, 
		RULE_program = 12, RULE_spec = 13, RULE_pclass_body = 14, RULE_field = 15, 
		RULE_var = 16, RULE_constructor = 17, RULE_transition = 18, RULE_args = 19, 
		RULE_arg = 20, RULE_action_body = 21, RULE_guard = 22, RULE_transit = 23, 
		RULE_error = 24, RULE_error_arm = 25, RULE_var_transit = 26, RULE_effect = 27, 
		RULE_effect_stmt = 28, RULE_effect_call = 29, RULE_expr = 30, RULE_when_subject_arm = 31, 
		RULE_when_guard_arm = 32, RULE_when_pattern = 33, RULE_proc_expr = 34, 
		RULE_literal = 35, RULE_bracket_literal = 36, RULE_map_entry = 37, RULE_set_literal = 38, 
		RULE_index_expr = 39, RULE_index_or_slice = 40, RULE_fun_call = 41, RULE_oclass_literal = 42, 
		RULE_oclass_field_assign = 43, RULE_field_access = 44;
	private static String[] makeRuleNames() {
		return new String[] {
			"root", "import_stmt", "qualified_name", "qual_segment", "decl", "typeExpr", 
			"typeArgs", "typeParams", "fun_decl", "pclass", "oclass", "proc", "program", 
			"spec", "pclass_body", "field", "var", "constructor", "transition", "args", 
			"arg", "action_body", "guard", "transit", "error", "error_arm", "var_transit", 
			"effect", "effect_stmt", "effect_call", "expr", "when_subject_arm", "when_guard_arm", 
			"when_pattern", "proc_expr", "literal", "bracket_literal", "map_entry", 
			"set_literal", "index_expr", "index_or_slice", "fun_call", "oclass_literal", 
			"oclass_field_assign", "field_access"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "','", "'.'", "':'", "'('", "')'", "'['", "']'", "'{'", "'}'", 
			"'||'", "'true'", "'false'", "'&'", "'|'", "'~'", "'*'", "'/'", "'%'", 
			"'+'", "'-'", "'<'", "'<='", "'>'", "'>='", "'='", "'~='", "':='", "'=>'", 
			"'if'", "'else'", "'let'", "'when'", "'in'", "'->'", "'import'", "'p-class'", 
			"'o-class'", "'proc'", "'program'", "'spec'", "'var'", "'const'", "'constructor'", 
			"'transition'", "'p2p-service'", "'p2p-consumer'", "'guard'", "'transit'", 
			"'error'", "'effect'", "'fun'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "COMMA", "DOT", "COLON", "LPAREN", "RPAREN", "LBRACK", "RBRACK", 
			"LCURLY", "RCURLY", "PARALLEL", "TRUE", "FALSE", "AND", "OR", "NOT", 
			"TIMES", "DIV", "MOD", "PLUS", "MINUS", "LT", "LTE", "GT", "GTE", "EQ", 
			"NEQ", "ASGN_EQ", "IMPLIES", "IF", "ELSE", "LET", "WHEN", "IN", "ARROW", 
			"IMPORT", "PCLASS", "OCLASS", "PROC", "PROGRAM", "SPEC", "VAR", "CONST", 
			"CONSTRUCTOR", "TRANSITION", "SERVICE", "CONSUMER", "GUARD", "TRANSIT", 
			"ERROR", "EFFECT", "FUN", "REAL", "INT", "ID", "STRING", "WS", "COMMENT", 
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
			setState(94);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 2253964477202432L) != 0)) {
				{
				setState(92);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case IMPORT:
					{
					setState(90);
					import_stmt();
					}
					break;
				case PCLASS:
				case OCLASS:
				case PROC:
				case PROGRAM:
				case SPEC:
				case FUN:
					{
					setState(91);
					decl();
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				}
				setState(96);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(97);
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
			setState(99);
			match(IMPORT);
			setState(100);
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
		public TerminalNode ID() { return getToken(JulayParser.ID, 0); }
		public List<TerminalNode> DOT() { return getTokens(JulayParser.DOT); }
		public TerminalNode DOT(int i) {
			return getToken(JulayParser.DOT, i);
		}
		public List<Qual_segmentContext> qual_segment() {
			return getRuleContexts(Qual_segmentContext.class);
		}
		public Qual_segmentContext qual_segment(int i) {
			return getRuleContext(Qual_segmentContext.class,i);
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
			setState(102);
			match(ID);
			setState(105); 
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					{
					setState(103);
					match(DOT);
					setState(104);
					qual_segment();
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(107); 
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
	public static class Qual_segmentContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(JulayParser.ID, 0); }
		public TerminalNode FUN() { return getToken(JulayParser.FUN, 0); }
		public Qual_segmentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_qual_segment; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterQual_segment(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitQual_segment(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitQual_segment(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Qual_segmentContext qual_segment() throws RecognitionException {
		Qual_segmentContext _localctx = new Qual_segmentContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_qual_segment);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(109);
			_la = _input.LA(1);
			if ( !(_la==FUN || _la==ID) ) {
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
		public PclassContext pclass() {
			return getRuleContext(PclassContext.class,0);
		}
		public OclassContext oclass() {
			return getRuleContext(OclassContext.class,0);
		}
		public ProcContext proc() {
			return getRuleContext(ProcContext.class,0);
		}
		public ProgramContext program() {
			return getRuleContext(ProgramContext.class,0);
		}
		public SpecContext spec() {
			return getRuleContext(SpecContext.class,0);
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
		try {
			setState(117);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case PCLASS:
				enterOuterAlt(_localctx, 1);
				{
				setState(111);
				pclass();
				}
				break;
			case OCLASS:
				enterOuterAlt(_localctx, 2);
				{
				setState(112);
				oclass();
				}
				break;
			case PROC:
				enterOuterAlt(_localctx, 3);
				{
				setState(113);
				proc();
				}
				break;
			case PROGRAM:
				enterOuterAlt(_localctx, 4);
				{
				setState(114);
				program();
				}
				break;
			case SPEC:
				enterOuterAlt(_localctx, 5);
				{
				setState(115);
				spec();
				}
				break;
			case FUN:
				enterOuterAlt(_localctx, 6);
				{
				setState(116);
				fun_decl();
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
			setState(127);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ID:
				enterOuterAlt(_localctx, 1);
				{
				setState(119);
				match(ID);
				setState(121);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==LT) {
					{
					setState(120);
					typeArgs();
					}
				}

				}
				break;
			case LPAREN:
				enterOuterAlt(_localctx, 2);
				{
				setState(123);
				match(LPAREN);
				setState(124);
				typeExpr();
				setState(125);
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
			setState(129);
			match(LT);
			setState(130);
			typeExpr();
			setState(135);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(131);
				match(COMMA);
				setState(132);
				typeExpr();
				}
				}
				setState(137);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(138);
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
			setState(140);
			match(LT);
			setState(141);
			match(ID);
			setState(146);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(142);
				match(COMMA);
				setState(143);
				match(ID);
				}
				}
				setState(148);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(149);
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
			setState(151);
			match(FUN);
			setState(152);
			match(ID);
			setState(154);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LT) {
				{
				setState(153);
				typeParams();
				}
			}

			setState(156);
			args();
			setState(157);
			match(COLON);
			setState(158);
			typeExpr();
			setState(159);
			match(EQ);
			setState(160);
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
	public static class PclassContext extends ParserRuleContext {
		public TerminalNode PCLASS() { return getToken(JulayParser.PCLASS, 0); }
		public TerminalNode ID() { return getToken(JulayParser.ID, 0); }
		public TerminalNode LCURLY() { return getToken(JulayParser.LCURLY, 0); }
		public TerminalNode RCURLY() { return getToken(JulayParser.RCURLY, 0); }
		public List<Pclass_bodyContext> pclass_body() {
			return getRuleContexts(Pclass_bodyContext.class);
		}
		public Pclass_bodyContext pclass_body(int i) {
			return getRuleContext(Pclass_bodyContext.class,i);
		}
		public PclassContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_pclass; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterPclass(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitPclass(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitPclass(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PclassContext pclass() throws RecognitionException {
		PclassContext _localctx = new PclassContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_pclass);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(162);
			match(PCLASS);
			setState(163);
			match(ID);
			setState(164);
			match(LCURLY);
			setState(168);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 138538465099776L) != 0)) {
				{
				{
				setState(165);
				pclass_body();
				}
				}
				setState(170);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(171);
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
	public static class OclassContext extends ParserRuleContext {
		public TerminalNode OCLASS() { return getToken(JulayParser.OCLASS, 0); }
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
		public OclassContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_oclass; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterOclass(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitOclass(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitOclass(this);
			else return visitor.visitChildren(this);
		}
	}

	public final OclassContext oclass() throws RecognitionException {
		OclassContext _localctx = new OclassContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_oclass);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(173);
			match(OCLASS);
			setState(174);
			match(ID);
			setState(176);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LT) {
				{
				setState(175);
				typeParams();
				}
			}

			setState(178);
			match(LCURLY);
			setState(182);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==ID) {
				{
				{
				setState(179);
				field();
				}
				}
				setState(184);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(185);
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
	public static class ProcContext extends ParserRuleContext {
		public TerminalNode PROC() { return getToken(JulayParser.PROC, 0); }
		public TerminalNode ID() { return getToken(JulayParser.ID, 0); }
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
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(187);
			match(PROC);
			setState(188);
			match(ID);
			setState(189);
			match(ASGN_EQ);
			setState(190);
			proc_expr(0);
			}
		}
		catch (RecognitionException re) {
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
	public static class ProgramContext extends ParserRuleContext {
		public TerminalNode PROGRAM() { return getToken(JulayParser.PROGRAM, 0); }
		public TerminalNode ID() { return getToken(JulayParser.ID, 0); }
		public TerminalNode ASGN_EQ() { return getToken(JulayParser.ASGN_EQ, 0); }
		public Proc_exprContext proc_expr() {
			return getRuleContext(Proc_exprContext.class,0);
		}
		public ProgramContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_program; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterProgram(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitProgram(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitProgram(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ProgramContext program() throws RecognitionException {
		ProgramContext _localctx = new ProgramContext(_ctx, getState());
		enterRule(_localctx, 24, RULE_program);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(192);
			match(PROGRAM);
			setState(193);
			match(ID);
			setState(194);
			match(ASGN_EQ);
			setState(195);
			proc_expr(0);
			}
		}
		catch (RecognitionException re) {
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
		public Proc_exprContext proc_expr() {
			return getRuleContext(Proc_exprContext.class,0);
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
		enterRule(_localctx, 26, RULE_spec);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(197);
			match(SPEC);
			setState(198);
			match(ID);
			setState(199);
			match(ASGN_EQ);
			setState(200);
			proc_expr(0);
			}
		}
		catch (RecognitionException re) {
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
		enterRule(_localctx, 28, RULE_pclass_body);
		try {
			setState(205);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,12,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(202);
				var();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(203);
				constructor();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(204);
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
		enterRule(_localctx, 30, RULE_field);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(207);
			match(ID);
			setState(208);
			match(COLON);
			setState(209);
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
		enterRule(_localctx, 32, RULE_var);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(211);
			_la = _input.LA(1);
			if ( !(_la==VAR || _la==CONST) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			setState(212);
			match(ID);
			setState(213);
			match(COLON);
			setState(214);
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
	public static class ConstructorContext extends ParserRuleContext {
		public TerminalNode CONSTRUCTOR() { return getToken(JulayParser.CONSTRUCTOR, 0); }
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
		public TerminalNode SERVICE() { return getToken(JulayParser.SERVICE, 0); }
		public TerminalNode CONSUMER() { return getToken(JulayParser.CONSUMER, 0); }
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
		enterRule(_localctx, 34, RULE_constructor);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(217);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==SERVICE || _la==CONSUMER) {
				{
				setState(216);
				_la = _input.LA(1);
				if ( !(_la==SERVICE || _la==CONSUMER) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
			}

			setState(219);
			match(CONSTRUCTOR);
			setState(220);
			match(ID);
			setState(221);
			args();
			setState(222);
			match(LCURLY);
			setState(226);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 2111062325329920L) != 0)) {
				{
				{
				setState(223);
				action_body();
				}
				}
				setState(228);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(229);
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
		public TerminalNode SERVICE() { return getToken(JulayParser.SERVICE, 0); }
		public TerminalNode CONSUMER() { return getToken(JulayParser.CONSUMER, 0); }
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
		enterRule(_localctx, 36, RULE_transition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(232);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==SERVICE || _la==CONSUMER) {
				{
				setState(231);
				_la = _input.LA(1);
				if ( !(_la==SERVICE || _la==CONSUMER) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
			}

			setState(234);
			match(TRANSITION);
			setState(235);
			match(ID);
			setState(236);
			args();
			setState(237);
			match(LCURLY);
			setState(241);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 2111062325329920L) != 0)) {
				{
				{
				setState(238);
				action_body();
				}
				}
				setState(243);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(244);
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
		enterRule(_localctx, 38, RULE_args);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(246);
			match(LPAREN);
			setState(248);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ID) {
				{
				setState(247);
				arg();
				}
			}

			setState(254);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(250);
				match(COMMA);
				setState(251);
				arg();
				}
				}
				setState(256);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(257);
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
		enterRule(_localctx, 40, RULE_arg);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(259);
			match(ID);
			setState(260);
			match(COLON);
			setState(261);
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
	public static class Action_bodyContext extends ParserRuleContext {
		public GuardContext guard() {
			return getRuleContext(GuardContext.class,0);
		}
		public TransitContext transit() {
			return getRuleContext(TransitContext.class,0);
		}
		public ErrorContext error() {
			return getRuleContext(ErrorContext.class,0);
		}
		public EffectContext effect() {
			return getRuleContext(EffectContext.class,0);
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
		enterRule(_localctx, 42, RULE_action_body);
		try {
			setState(267);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case GUARD:
				enterOuterAlt(_localctx, 1);
				{
				setState(263);
				guard();
				}
				break;
			case TRANSIT:
				enterOuterAlt(_localctx, 2);
				{
				setState(264);
				transit();
				}
				break;
			case ERROR:
				enterOuterAlt(_localctx, 3);
				{
				setState(265);
				error();
				}
				break;
			case EFFECT:
				enterOuterAlt(_localctx, 4);
				{
				setState(266);
				effect();
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
		enterRule(_localctx, 44, RULE_guard);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(269);
			match(GUARD);
			setState(270);
			match(COLON);
			setState(271);
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
		enterRule(_localctx, 46, RULE_transit);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(273);
			match(TRANSIT);
			setState(274);
			match(COLON);
			setState(278);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==ID) {
				{
				{
				setState(275);
				var_transit();
				}
				}
				setState(280);
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
		enterRule(_localctx, 48, RULE_error);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(281);
			match(ERROR);
			setState(282);
			match(COLON);
			setState(284); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(283);
				error_arm();
				}
				}
				setState(286); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & 67554001389943120L) != 0) );
			}
		}
		catch (RecognitionException re) {
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
		enterRule(_localctx, 50, RULE_error_arm);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(288);
			expr(0);
			setState(289);
			match(ARROW);
			setState(290);
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
		enterRule(_localctx, 52, RULE_var_transit);
		try {
			setState(303);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,22,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(292);
				field_access();
				setState(293);
				match(ASGN_EQ);
				setState(294);
				expr(0);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(296);
				match(ID);
				setState(297);
				match(LBRACK);
				setState(298);
				expr(0);
				setState(299);
				match(RBRACK);
				setState(300);
				match(ASGN_EQ);
				setState(301);
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
	public static class EffectContext extends ParserRuleContext {
		public TerminalNode EFFECT() { return getToken(JulayParser.EFFECT, 0); }
		public TerminalNode COLON() { return getToken(JulayParser.COLON, 0); }
		public List<Effect_stmtContext> effect_stmt() {
			return getRuleContexts(Effect_stmtContext.class);
		}
		public Effect_stmtContext effect_stmt(int i) {
			return getRuleContext(Effect_stmtContext.class,i);
		}
		public EffectContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_effect; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterEffect(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitEffect(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitEffect(this);
			else return visitor.visitChildren(this);
		}
	}

	public final EffectContext effect() throws RecognitionException {
		EffectContext _localctx = new EffectContext(_ctx, getState());
		enterRule(_localctx, 54, RULE_effect);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(305);
			match(EFFECT);
			setState(306);
			match(COLON);
			setState(308); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(307);
				effect_stmt();
				}
				}
				setState(310); 
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
	public static class Effect_stmtContext extends ParserRuleContext {
		public Effect_callContext effect_call() {
			return getRuleContext(Effect_callContext.class,0);
		}
		public Field_accessContext field_access() {
			return getRuleContext(Field_accessContext.class,0);
		}
		public TerminalNode ASGN_EQ() { return getToken(JulayParser.ASGN_EQ, 0); }
		public Effect_stmtContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_effect_stmt; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterEffect_stmt(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitEffect_stmt(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitEffect_stmt(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Effect_stmtContext effect_stmt() throws RecognitionException {
		Effect_stmtContext _localctx = new Effect_stmtContext(_ctx, getState());
		enterRule(_localctx, 56, RULE_effect_stmt);
		try {
			setState(317);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,24,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(312);
				effect_call();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(313);
				field_access();
				setState(314);
				match(ASGN_EQ);
				setState(315);
				effect_call();
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
	public static class Effect_callContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(JulayParser.ID, 0); }
		public TerminalNode LPAREN() { return getToken(JulayParser.LPAREN, 0); }
		public TerminalNode RPAREN() { return getToken(JulayParser.RPAREN, 0); }
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
		public Effect_callContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_effect_call; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterEffect_call(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitEffect_call(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitEffect_call(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Effect_callContext effect_call() throws RecognitionException {
		Effect_callContext _localctx = new Effect_callContext(_ctx, getState());
		enterRule(_localctx, 58, RULE_effect_call);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(319);
			match(ID);
			setState(320);
			match(LPAREN);
			setState(329);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 67554001389943120L) != 0)) {
				{
				setState(321);
				expr(0);
				setState(326);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(322);
					match(COMMA);
					setState(323);
					expr(0);
					}
					}
					setState(328);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
			}

			setState(331);
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
		int _startState = 60;
		enterRecursionRule(_localctx, 60, RULE_expr, _p);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(396);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,29,_ctx) ) {
			case 1:
				{
				setState(334);
				literal();
				}
				break;
			case 2:
				{
				setState(335);
				match(LPAREN);
				setState(336);
				expr(0);
				setState(337);
				match(RPAREN);
				}
				break;
			case 3:
				{
				setState(339);
				bracket_literal();
				}
				break;
			case 4:
				{
				setState(340);
				set_literal();
				}
				break;
			case 5:
				{
				setState(341);
				index_expr(0);
				}
				break;
			case 6:
				{
				setState(342);
				field_access();
				}
				break;
			case 7:
				{
				setState(343);
				oclass_literal();
				}
				break;
			case 8:
				{
				setState(344);
				fun_call();
				}
				break;
			case 9:
				{
				setState(345);
				match(NOT);
				setState(346);
				expr(22);
				}
				break;
			case 10:
				{
				setState(347);
				match(AND);
				setState(348);
				expr(21);
				}
				break;
			case 11:
				{
				setState(349);
				match(OR);
				setState(350);
				expr(20);
				}
				break;
			case 12:
				{
				setState(351);
				match(IF);
				setState(352);
				match(LPAREN);
				setState(353);
				expr(0);
				setState(354);
				match(RPAREN);
				setState(355);
				match(LCURLY);
				setState(356);
				expr(0);
				setState(357);
				match(RCURLY);
				setState(358);
				match(ELSE);
				setState(359);
				match(LCURLY);
				setState(360);
				expr(0);
				setState(361);
				match(RCURLY);
				}
				break;
			case 13:
				{
				setState(363);
				match(LET);
				setState(364);
				match(LPAREN);
				setState(365);
				match(ID);
				setState(366);
				match(COLON);
				setState(367);
				typeExpr();
				setState(368);
				match(ASGN_EQ);
				setState(369);
				expr(0);
				setState(370);
				match(RPAREN);
				setState(371);
				match(LCURLY);
				setState(372);
				expr(0);
				setState(373);
				match(RCURLY);
				}
				break;
			case 14:
				{
				setState(375);
				match(WHEN);
				setState(376);
				match(LPAREN);
				setState(377);
				expr(0);
				setState(378);
				match(RPAREN);
				setState(379);
				match(LCURLY);
				setState(381); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(380);
					when_subject_arm();
					}
					}
					setState(383); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & 67553995484305424L) != 0) );
				setState(385);
				match(RCURLY);
				}
				break;
			case 15:
				{
				setState(387);
				match(WHEN);
				setState(388);
				match(LCURLY);
				setState(390); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(389);
					when_guard_arm();
					}
					}
					setState(392); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & 67554002463684944L) != 0) );
				setState(394);
				match(RCURLY);
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(445);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,31,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(443);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,30,_ctx) ) {
					case 1:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(398);
						if (!(precpred(_ctx, 19))) throw new FailedPredicateException(this, "precpred(_ctx, 19)");
						setState(399);
						match(TIMES);
						setState(400);
						expr(20);
						}
						break;
					case 2:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(401);
						if (!(precpred(_ctx, 18))) throw new FailedPredicateException(this, "precpred(_ctx, 18)");
						setState(402);
						match(DIV);
						setState(403);
						expr(19);
						}
						break;
					case 3:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(404);
						if (!(precpred(_ctx, 17))) throw new FailedPredicateException(this, "precpred(_ctx, 17)");
						setState(405);
						match(MOD);
						setState(406);
						expr(18);
						}
						break;
					case 4:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(407);
						if (!(precpred(_ctx, 16))) throw new FailedPredicateException(this, "precpred(_ctx, 16)");
						setState(408);
						match(PLUS);
						setState(409);
						expr(17);
						}
						break;
					case 5:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(410);
						if (!(precpred(_ctx, 15))) throw new FailedPredicateException(this, "precpred(_ctx, 15)");
						setState(411);
						match(MINUS);
						setState(412);
						expr(16);
						}
						break;
					case 6:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(413);
						if (!(precpred(_ctx, 14))) throw new FailedPredicateException(this, "precpred(_ctx, 14)");
						setState(414);
						match(LT);
						setState(415);
						expr(15);
						}
						break;
					case 7:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(416);
						if (!(precpred(_ctx, 13))) throw new FailedPredicateException(this, "precpred(_ctx, 13)");
						setState(417);
						match(LTE);
						setState(418);
						expr(14);
						}
						break;
					case 8:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(419);
						if (!(precpred(_ctx, 12))) throw new FailedPredicateException(this, "precpred(_ctx, 12)");
						setState(420);
						match(GT);
						setState(421);
						expr(13);
						}
						break;
					case 9:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(422);
						if (!(precpred(_ctx, 11))) throw new FailedPredicateException(this, "precpred(_ctx, 11)");
						setState(423);
						match(GTE);
						setState(424);
						expr(12);
						}
						break;
					case 10:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(425);
						if (!(precpred(_ctx, 10))) throw new FailedPredicateException(this, "precpred(_ctx, 10)");
						setState(426);
						match(IN);
						setState(427);
						expr(11);
						}
						break;
					case 11:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(428);
						if (!(precpred(_ctx, 9))) throw new FailedPredicateException(this, "precpred(_ctx, 9)");
						setState(429);
						match(EQ);
						setState(430);
						expr(10);
						}
						break;
					case 12:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(431);
						if (!(precpred(_ctx, 8))) throw new FailedPredicateException(this, "precpred(_ctx, 8)");
						setState(432);
						match(NEQ);
						setState(433);
						expr(9);
						}
						break;
					case 13:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(434);
						if (!(precpred(_ctx, 7))) throw new FailedPredicateException(this, "precpred(_ctx, 7)");
						setState(435);
						match(AND);
						setState(436);
						expr(8);
						}
						break;
					case 14:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(437);
						if (!(precpred(_ctx, 6))) throw new FailedPredicateException(this, "precpred(_ctx, 6)");
						setState(438);
						match(OR);
						setState(439);
						expr(7);
						}
						break;
					case 15:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(440);
						if (!(precpred(_ctx, 5))) throw new FailedPredicateException(this, "precpred(_ctx, 5)");
						setState(441);
						match(IMPLIES);
						setState(442);
						expr(6);
						}
						break;
					}
					} 
				}
				setState(447);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,31,_ctx);
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
		enterRule(_localctx, 62, RULE_when_subject_arm);
		try {
			setState(455);
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
				setState(448);
				when_pattern();
				setState(449);
				match(ARROW);
				setState(450);
				expr(0);
				}
				break;
			case ELSE:
				enterOuterAlt(_localctx, 2);
				{
				setState(452);
				match(ELSE);
				setState(453);
				match(ARROW);
				setState(454);
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
		enterRule(_localctx, 64, RULE_when_guard_arm);
		try {
			setState(464);
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
			case REAL:
			case INT:
			case ID:
			case STRING:
				enterOuterAlt(_localctx, 1);
				{
				setState(457);
				expr(0);
				setState(458);
				match(ARROW);
				setState(459);
				expr(0);
				}
				break;
			case ELSE:
				enterOuterAlt(_localctx, 2);
				{
				setState(461);
				match(ELSE);
				setState(462);
				match(ARROW);
				setState(463);
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
		enterRule(_localctx, 66, RULE_when_pattern);
		try {
			setState(468);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case TRUE:
			case FALSE:
			case REAL:
			case INT:
			case STRING:
				enterOuterAlt(_localctx, 1);
				{
				setState(466);
				literal();
				}
				break;
			case LPAREN:
			case ID:
				enterOuterAlt(_localctx, 2);
				{
				setState(467);
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
		int _startState = 68;
		enterRecursionRule(_localctx, 68, RULE_proc_expr, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(477);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,35,_ctx) ) {
			case 1:
				{
				setState(471);
				qualified_name();
				}
				break;
			case 2:
				{
				setState(472);
				match(ID);
				}
				break;
			case 3:
				{
				setState(473);
				match(LPAREN);
				setState(474);
				proc_expr(0);
				setState(475);
				match(RPAREN);
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(484);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,36,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					{
					_localctx = new Proc_exprContext(_parentctx, _parentState);
					pushNewRecursionContext(_localctx, _startState, RULE_proc_expr);
					setState(479);
					if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
					setState(480);
					match(PARALLEL);
					setState(481);
					proc_expr(2);
					}
					} 
				}
				setState(486);
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
		enterRule(_localctx, 70, RULE_literal);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(487);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 49539595901081600L) != 0)) ) {
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
		enterRule(_localctx, 72, RULE_bracket_literal);
		int _la;
		try {
			setState(513);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,39,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(489);
				match(LBRACK);
				setState(490);
				match(RBRACK);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(491);
				match(LBRACK);
				setState(492);
				map_entry();
				setState(497);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(493);
					match(COMMA);
					setState(494);
					map_entry();
					}
					}
					setState(499);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(500);
				match(RBRACK);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(502);
				match(LBRACK);
				setState(503);
				expr(0);
				setState(508);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(504);
					match(COMMA);
					setState(505);
					expr(0);
					}
					}
					setState(510);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(511);
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
		enterRule(_localctx, 74, RULE_map_entry);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(515);
			expr(0);
			setState(516);
			match(ARROW);
			setState(517);
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
		enterRule(_localctx, 76, RULE_set_literal);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(519);
			match(LCURLY);
			setState(528);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 67554001389943120L) != 0)) {
				{
				setState(520);
				expr(0);
				setState(525);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(521);
					match(COMMA);
					setState(522);
					expr(0);
					}
					}
					setState(527);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
			}

			setState(530);
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
		public TerminalNode LPAREN() { return getToken(JulayParser.LPAREN, 0); }
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public TerminalNode RPAREN() { return getToken(JulayParser.RPAREN, 0); }
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
		int _startState = 78;
		enterRecursionRule(_localctx, 78, RULE_index_expr, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			{
			setState(541);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,42,_ctx) ) {
			case 1:
				{
				setState(533);
				fun_call();
				}
				break;
			case 2:
				{
				setState(534);
				field_access();
				}
				break;
			case 3:
				{
				setState(535);
				bracket_literal();
				}
				break;
			case 4:
				{
				setState(536);
				set_literal();
				}
				break;
			case 5:
				{
				setState(537);
				match(LPAREN);
				setState(538);
				expr(0);
				setState(539);
				match(RPAREN);
				}
				break;
			}
			setState(543);
			match(LBRACK);
			setState(544);
			index_or_slice();
			setState(545);
			match(RBRACK);
			}
			_ctx.stop = _input.LT(-1);
			setState(554);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,43,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					{
					_localctx = new Index_exprContext(_parentctx, _parentState);
					pushNewRecursionContext(_localctx, _startState, RULE_index_expr);
					setState(547);
					if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
					setState(548);
					match(LBRACK);
					setState(549);
					index_or_slice();
					setState(550);
					match(RBRACK);
					}
					} 
				}
				setState(556);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,43,_ctx);
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
		enterRule(_localctx, 80, RULE_index_or_slice);
		try {
			setState(562);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,44,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(557);
				expr(0);
				setState(558);
				match(COLON);
				setState(559);
				expr(0);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(561);
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
	public static class Fun_callContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(JulayParser.ID, 0); }
		public TerminalNode LPAREN() { return getToken(JulayParser.LPAREN, 0); }
		public TerminalNode RPAREN() { return getToken(JulayParser.RPAREN, 0); }
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
		enterRule(_localctx, 82, RULE_fun_call);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(564);
			match(ID);
			setState(565);
			match(LPAREN);
			setState(574);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 67554001389943120L) != 0)) {
				{
				setState(566);
				expr(0);
				setState(571);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(567);
					match(COMMA);
					setState(568);
					expr(0);
					}
					}
					setState(573);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
			}

			setState(576);
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
		enterRule(_localctx, 84, RULE_oclass_literal);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(578);
			typeExpr();
			setState(579);
			match(LCURLY);
			setState(580);
			oclass_field_assign();
			setState(585);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(581);
				match(COMMA);
				setState(582);
				oclass_field_assign();
				}
				}
				setState(587);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(588);
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
		enterRule(_localctx, 86, RULE_oclass_field_assign);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(590);
			match(ID);
			setState(591);
			match(ASGN_EQ);
			setState(592);
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
		enterRule(_localctx, 88, RULE_field_access);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(594);
			match(ID);
			setState(599);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,48,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(595);
					match(DOT);
					setState(596);
					match(ID);
					}
					} 
				}
				setState(601);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,48,_ctx);
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
		case 30:
			return expr_sempred((ExprContext)_localctx, predIndex);
		case 34:
			return proc_expr_sempred((Proc_exprContext)_localctx, predIndex);
		case 39:
			return index_expr_sempred((Index_exprContext)_localctx, predIndex);
		}
		return true;
	}
	private boolean expr_sempred(ExprContext _localctx, int predIndex) {
		switch (predIndex) {
		case 0:
			return precpred(_ctx, 19);
		case 1:
			return precpred(_ctx, 18);
		case 2:
			return precpred(_ctx, 17);
		case 3:
			return precpred(_ctx, 16);
		case 4:
			return precpred(_ctx, 15);
		case 5:
			return precpred(_ctx, 14);
		case 6:
			return precpred(_ctx, 13);
		case 7:
			return precpred(_ctx, 12);
		case 8:
			return precpred(_ctx, 11);
		case 9:
			return precpred(_ctx, 10);
		case 10:
			return precpred(_ctx, 9);
		case 11:
			return precpred(_ctx, 8);
		case 12:
			return precpred(_ctx, 7);
		case 13:
			return precpred(_ctx, 6);
		case 14:
			return precpred(_ctx, 5);
		}
		return true;
	}
	private boolean proc_expr_sempred(Proc_exprContext _localctx, int predIndex) {
		switch (predIndex) {
		case 15:
			return precpred(_ctx, 1);
		}
		return true;
	}
	private boolean index_expr_sempred(Index_exprContext _localctx, int predIndex) {
		switch (predIndex) {
		case 16:
			return precpred(_ctx, 2);
		}
		return true;
	}

	public static final String _serializedATN =
		"\u0004\u0001:\u025b\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001\u0002"+
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
		"(\u0007(\u0002)\u0007)\u0002*\u0007*\u0002+\u0007+\u0002,\u0007,\u0001"+
		"\u0000\u0001\u0000\u0005\u0000]\b\u0000\n\u0000\f\u0000`\t\u0000\u0001"+
		"\u0000\u0001\u0000\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0002\u0001"+
		"\u0002\u0001\u0002\u0004\u0002j\b\u0002\u000b\u0002\f\u0002k\u0001\u0003"+
		"\u0001\u0003\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004"+
		"\u0001\u0004\u0003\u0004v\b\u0004\u0001\u0005\u0001\u0005\u0003\u0005"+
		"z\b\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0003\u0005"+
		"\u0080\b\u0005\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0005\u0006"+
		"\u0086\b\u0006\n\u0006\f\u0006\u0089\t\u0006\u0001\u0006\u0001\u0006\u0001"+
		"\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0005\u0007\u0091\b\u0007\n"+
		"\u0007\f\u0007\u0094\t\u0007\u0001\u0007\u0001\u0007\u0001\b\u0001\b\u0001"+
		"\b\u0003\b\u009b\b\b\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b\u0001"+
		"\t\u0001\t\u0001\t\u0001\t\u0005\t\u00a7\b\t\n\t\f\t\u00aa\t\t\u0001\t"+
		"\u0001\t\u0001\n\u0001\n\u0001\n\u0003\n\u00b1\b\n\u0001\n\u0001\n\u0005"+
		"\n\u00b5\b\n\n\n\f\n\u00b8\t\n\u0001\n\u0001\n\u0001\u000b\u0001\u000b"+
		"\u0001\u000b\u0001\u000b\u0001\u000b\u0001\f\u0001\f\u0001\f\u0001\f\u0001"+
		"\f\u0001\r\u0001\r\u0001\r\u0001\r\u0001\r\u0001\u000e\u0001\u000e\u0001"+
		"\u000e\u0003\u000e\u00ce\b\u000e\u0001\u000f\u0001\u000f\u0001\u000f\u0001"+
		"\u000f\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0001"+
		"\u0011\u0003\u0011\u00da\b\u0011\u0001\u0011\u0001\u0011\u0001\u0011\u0001"+
		"\u0011\u0001\u0011\u0005\u0011\u00e1\b\u0011\n\u0011\f\u0011\u00e4\t\u0011"+
		"\u0001\u0011\u0001\u0011\u0001\u0012\u0003\u0012\u00e9\b\u0012\u0001\u0012"+
		"\u0001\u0012\u0001\u0012\u0001\u0012\u0001\u0012\u0005\u0012\u00f0\b\u0012"+
		"\n\u0012\f\u0012\u00f3\t\u0012\u0001\u0012\u0001\u0012\u0001\u0013\u0001"+
		"\u0013\u0003\u0013\u00f9\b\u0013\u0001\u0013\u0001\u0013\u0005\u0013\u00fd"+
		"\b\u0013\n\u0013\f\u0013\u0100\t\u0013\u0001\u0013\u0001\u0013\u0001\u0014"+
		"\u0001\u0014\u0001\u0014\u0001\u0014\u0001\u0015\u0001\u0015\u0001\u0015"+
		"\u0001\u0015\u0003\u0015\u010c\b\u0015\u0001\u0016\u0001\u0016\u0001\u0016"+
		"\u0001\u0016\u0001\u0017\u0001\u0017\u0001\u0017\u0005\u0017\u0115\b\u0017"+
		"\n\u0017\f\u0017\u0118\t\u0017\u0001\u0018\u0001\u0018\u0001\u0018\u0004"+
		"\u0018\u011d\b\u0018\u000b\u0018\f\u0018\u011e\u0001\u0019\u0001\u0019"+
		"\u0001\u0019\u0001\u0019\u0001\u001a\u0001\u001a\u0001\u001a\u0001\u001a"+
		"\u0001\u001a\u0001\u001a\u0001\u001a\u0001\u001a\u0001\u001a\u0001\u001a"+
		"\u0001\u001a\u0003\u001a\u0130\b\u001a\u0001\u001b\u0001\u001b\u0001\u001b"+
		"\u0004\u001b\u0135\b\u001b\u000b\u001b\f\u001b\u0136\u0001\u001c\u0001"+
		"\u001c\u0001\u001c\u0001\u001c\u0001\u001c\u0003\u001c\u013e\b\u001c\u0001"+
		"\u001d\u0001\u001d\u0001\u001d\u0001\u001d\u0001\u001d\u0005\u001d\u0145"+
		"\b\u001d\n\u001d\f\u001d\u0148\t\u001d\u0003\u001d\u014a\b\u001d\u0001"+
		"\u001d\u0001\u001d\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001"+
		"\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001"+
		"\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001"+
		"\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001"+
		"\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001"+
		"\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001"+
		"\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001"+
		"\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001"+
		"\u001e\u0001\u001e\u0004\u001e\u017e\b\u001e\u000b\u001e\f\u001e\u017f"+
		"\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0004\u001e"+
		"\u0187\b\u001e\u000b\u001e\f\u001e\u0188\u0001\u001e\u0001\u001e\u0003"+
		"\u001e\u018d\b\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001"+
		"\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001"+
		"\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001"+
		"\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001"+
		"\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001"+
		"\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001"+
		"\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001"+
		"\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0005\u001e\u01bc"+
		"\b\u001e\n\u001e\f\u001e\u01bf\t\u001e\u0001\u001f\u0001\u001f\u0001\u001f"+
		"\u0001\u001f\u0001\u001f\u0001\u001f\u0001\u001f\u0003\u001f\u01c8\b\u001f"+
		"\u0001 \u0001 \u0001 \u0001 \u0001 \u0001 \u0001 \u0003 \u01d1\b \u0001"+
		"!\u0001!\u0003!\u01d5\b!\u0001\"\u0001\"\u0001\"\u0001\"\u0001\"\u0001"+
		"\"\u0001\"\u0003\"\u01de\b\"\u0001\"\u0001\"\u0001\"\u0005\"\u01e3\b\""+
		"\n\"\f\"\u01e6\t\"\u0001#\u0001#\u0001$\u0001$\u0001$\u0001$\u0001$\u0001"+
		"$\u0005$\u01f0\b$\n$\f$\u01f3\t$\u0001$\u0001$\u0001$\u0001$\u0001$\u0001"+
		"$\u0005$\u01fb\b$\n$\f$\u01fe\t$\u0001$\u0001$\u0003$\u0202\b$\u0001%"+
		"\u0001%\u0001%\u0001%\u0001&\u0001&\u0001&\u0001&\u0005&\u020c\b&\n&\f"+
		"&\u020f\t&\u0003&\u0211\b&\u0001&\u0001&\u0001\'\u0001\'\u0001\'\u0001"+
		"\'\u0001\'\u0001\'\u0001\'\u0001\'\u0001\'\u0003\'\u021e\b\'\u0001\'\u0001"+
		"\'\u0001\'\u0001\'\u0001\'\u0001\'\u0001\'\u0001\'\u0001\'\u0005\'\u0229"+
		"\b\'\n\'\f\'\u022c\t\'\u0001(\u0001(\u0001(\u0001(\u0001(\u0003(\u0233"+
		"\b(\u0001)\u0001)\u0001)\u0001)\u0001)\u0005)\u023a\b)\n)\f)\u023d\t)"+
		"\u0003)\u023f\b)\u0001)\u0001)\u0001*\u0001*\u0001*\u0001*\u0001*\u0005"+
		"*\u0248\b*\n*\f*\u024b\t*\u0001*\u0001*\u0001+\u0001+\u0001+\u0001+\u0001"+
		",\u0001,\u0001,\u0005,\u0256\b,\n,\f,\u0259\t,\u0001,\u0000\u0003<DN-"+
		"\u0000\u0002\u0004\u0006\b\n\f\u000e\u0010\u0012\u0014\u0016\u0018\u001a"+
		"\u001c\u001e \"$&(*,.02468:<>@BDFHJLNPRTVX\u0000\u0004\u0002\u0000336"+
		"6\u0001\u0000)*\u0001\u0000-.\u0003\u0000\u000b\f4577\u0284\u0000^\u0001"+
		"\u0000\u0000\u0000\u0002c\u0001\u0000\u0000\u0000\u0004f\u0001\u0000\u0000"+
		"\u0000\u0006m\u0001\u0000\u0000\u0000\bu\u0001\u0000\u0000\u0000\n\u007f"+
		"\u0001\u0000\u0000\u0000\f\u0081\u0001\u0000\u0000\u0000\u000e\u008c\u0001"+
		"\u0000\u0000\u0000\u0010\u0097\u0001\u0000\u0000\u0000\u0012\u00a2\u0001"+
		"\u0000\u0000\u0000\u0014\u00ad\u0001\u0000\u0000\u0000\u0016\u00bb\u0001"+
		"\u0000\u0000\u0000\u0018\u00c0\u0001\u0000\u0000\u0000\u001a\u00c5\u0001"+
		"\u0000\u0000\u0000\u001c\u00cd\u0001\u0000\u0000\u0000\u001e\u00cf\u0001"+
		"\u0000\u0000\u0000 \u00d3\u0001\u0000\u0000\u0000\"\u00d9\u0001\u0000"+
		"\u0000\u0000$\u00e8\u0001\u0000\u0000\u0000&\u00f6\u0001\u0000\u0000\u0000"+
		"(\u0103\u0001\u0000\u0000\u0000*\u010b\u0001\u0000\u0000\u0000,\u010d"+
		"\u0001\u0000\u0000\u0000.\u0111\u0001\u0000\u0000\u00000\u0119\u0001\u0000"+
		"\u0000\u00002\u0120\u0001\u0000\u0000\u00004\u012f\u0001\u0000\u0000\u0000"+
		"6\u0131\u0001\u0000\u0000\u00008\u013d\u0001\u0000\u0000\u0000:\u013f"+
		"\u0001\u0000\u0000\u0000<\u018c\u0001\u0000\u0000\u0000>\u01c7\u0001\u0000"+
		"\u0000\u0000@\u01d0\u0001\u0000\u0000\u0000B\u01d4\u0001\u0000\u0000\u0000"+
		"D\u01dd\u0001\u0000\u0000\u0000F\u01e7\u0001\u0000\u0000\u0000H\u0201"+
		"\u0001\u0000\u0000\u0000J\u0203\u0001\u0000\u0000\u0000L\u0207\u0001\u0000"+
		"\u0000\u0000N\u0214\u0001\u0000\u0000\u0000P\u0232\u0001\u0000\u0000\u0000"+
		"R\u0234\u0001\u0000\u0000\u0000T\u0242\u0001\u0000\u0000\u0000V\u024e"+
		"\u0001\u0000\u0000\u0000X\u0252\u0001\u0000\u0000\u0000Z]\u0003\u0002"+
		"\u0001\u0000[]\u0003\b\u0004\u0000\\Z\u0001\u0000\u0000\u0000\\[\u0001"+
		"\u0000\u0000\u0000]`\u0001\u0000\u0000\u0000^\\\u0001\u0000\u0000\u0000"+
		"^_\u0001\u0000\u0000\u0000_a\u0001\u0000\u0000\u0000`^\u0001\u0000\u0000"+
		"\u0000ab\u0005\u0000\u0000\u0001b\u0001\u0001\u0000\u0000\u0000cd\u0005"+
		"#\u0000\u0000de\u0003\u0004\u0002\u0000e\u0003\u0001\u0000\u0000\u0000"+
		"fi\u00056\u0000\u0000gh\u0005\u0002\u0000\u0000hj\u0003\u0006\u0003\u0000"+
		"ig\u0001\u0000\u0000\u0000jk\u0001\u0000\u0000\u0000ki\u0001\u0000\u0000"+
		"\u0000kl\u0001\u0000\u0000\u0000l\u0005\u0001\u0000\u0000\u0000mn\u0007"+
		"\u0000\u0000\u0000n\u0007\u0001\u0000\u0000\u0000ov\u0003\u0012\t\u0000"+
		"pv\u0003\u0014\n\u0000qv\u0003\u0016\u000b\u0000rv\u0003\u0018\f\u0000"+
		"sv\u0003\u001a\r\u0000tv\u0003\u0010\b\u0000uo\u0001\u0000\u0000\u0000"+
		"up\u0001\u0000\u0000\u0000uq\u0001\u0000\u0000\u0000ur\u0001\u0000\u0000"+
		"\u0000us\u0001\u0000\u0000\u0000ut\u0001\u0000\u0000\u0000v\t\u0001\u0000"+
		"\u0000\u0000wy\u00056\u0000\u0000xz\u0003\f\u0006\u0000yx\u0001\u0000"+
		"\u0000\u0000yz\u0001\u0000\u0000\u0000z\u0080\u0001\u0000\u0000\u0000"+
		"{|\u0005\u0004\u0000\u0000|}\u0003\n\u0005\u0000}~\u0005\u0005\u0000\u0000"+
		"~\u0080\u0001\u0000\u0000\u0000\u007fw\u0001\u0000\u0000\u0000\u007f{"+
		"\u0001\u0000\u0000\u0000\u0080\u000b\u0001\u0000\u0000\u0000\u0081\u0082"+
		"\u0005\u0015\u0000\u0000\u0082\u0087\u0003\n\u0005\u0000\u0083\u0084\u0005"+
		"\u0001\u0000\u0000\u0084\u0086\u0003\n\u0005\u0000\u0085\u0083\u0001\u0000"+
		"\u0000\u0000\u0086\u0089\u0001\u0000\u0000\u0000\u0087\u0085\u0001\u0000"+
		"\u0000\u0000\u0087\u0088\u0001\u0000\u0000\u0000\u0088\u008a\u0001\u0000"+
		"\u0000\u0000\u0089\u0087\u0001\u0000\u0000\u0000\u008a\u008b\u0005\u0017"+
		"\u0000\u0000\u008b\r\u0001\u0000\u0000\u0000\u008c\u008d\u0005\u0015\u0000"+
		"\u0000\u008d\u0092\u00056\u0000\u0000\u008e\u008f\u0005\u0001\u0000\u0000"+
		"\u008f\u0091\u00056\u0000\u0000\u0090\u008e\u0001\u0000\u0000\u0000\u0091"+
		"\u0094\u0001\u0000\u0000\u0000\u0092\u0090\u0001\u0000\u0000\u0000\u0092"+
		"\u0093\u0001\u0000\u0000\u0000\u0093\u0095\u0001\u0000\u0000\u0000\u0094"+
		"\u0092\u0001\u0000\u0000\u0000\u0095\u0096\u0005\u0017\u0000\u0000\u0096"+
		"\u000f\u0001\u0000\u0000\u0000\u0097\u0098\u00053\u0000\u0000\u0098\u009a"+
		"\u00056\u0000\u0000\u0099\u009b\u0003\u000e\u0007\u0000\u009a\u0099\u0001"+
		"\u0000\u0000\u0000\u009a\u009b\u0001\u0000\u0000\u0000\u009b\u009c\u0001"+
		"\u0000\u0000\u0000\u009c\u009d\u0003&\u0013\u0000\u009d\u009e\u0005\u0003"+
		"\u0000\u0000\u009e\u009f\u0003\n\u0005\u0000\u009f\u00a0\u0005\u0019\u0000"+
		"\u0000\u00a0\u00a1\u0003<\u001e\u0000\u00a1\u0011\u0001\u0000\u0000\u0000"+
		"\u00a2\u00a3\u0005$\u0000\u0000\u00a3\u00a4\u00056\u0000\u0000\u00a4\u00a8"+
		"\u0005\b\u0000\u0000\u00a5\u00a7\u0003\u001c\u000e\u0000\u00a6\u00a5\u0001"+
		"\u0000\u0000\u0000\u00a7\u00aa\u0001\u0000\u0000\u0000\u00a8\u00a6\u0001"+
		"\u0000\u0000\u0000\u00a8\u00a9\u0001\u0000\u0000\u0000\u00a9\u00ab\u0001"+
		"\u0000\u0000\u0000\u00aa\u00a8\u0001\u0000\u0000\u0000\u00ab\u00ac\u0005"+
		"\t\u0000\u0000\u00ac\u0013\u0001\u0000\u0000\u0000\u00ad\u00ae\u0005%"+
		"\u0000\u0000\u00ae\u00b0\u00056\u0000\u0000\u00af\u00b1\u0003\u000e\u0007"+
		"\u0000\u00b0\u00af\u0001\u0000\u0000\u0000\u00b0\u00b1\u0001\u0000\u0000"+
		"\u0000\u00b1\u00b2\u0001\u0000\u0000\u0000\u00b2\u00b6\u0005\b\u0000\u0000"+
		"\u00b3\u00b5\u0003\u001e\u000f\u0000\u00b4\u00b3\u0001\u0000\u0000\u0000"+
		"\u00b5\u00b8\u0001\u0000\u0000\u0000\u00b6\u00b4\u0001\u0000\u0000\u0000"+
		"\u00b6\u00b7\u0001\u0000\u0000\u0000\u00b7\u00b9\u0001\u0000\u0000\u0000"+
		"\u00b8\u00b6\u0001\u0000\u0000\u0000\u00b9\u00ba\u0005\t\u0000\u0000\u00ba"+
		"\u0015\u0001\u0000\u0000\u0000\u00bb\u00bc\u0005&\u0000\u0000\u00bc\u00bd"+
		"\u00056\u0000\u0000\u00bd\u00be\u0005\u001b\u0000\u0000\u00be\u00bf\u0003"+
		"D\"\u0000\u00bf\u0017\u0001\u0000\u0000\u0000\u00c0\u00c1\u0005\'\u0000"+
		"\u0000\u00c1\u00c2\u00056\u0000\u0000\u00c2\u00c3\u0005\u001b\u0000\u0000"+
		"\u00c3\u00c4\u0003D\"\u0000\u00c4\u0019\u0001\u0000\u0000\u0000\u00c5"+
		"\u00c6\u0005(\u0000\u0000\u00c6\u00c7\u00056\u0000\u0000\u00c7\u00c8\u0005"+
		"\u001b\u0000\u0000\u00c8\u00c9\u0003D\"\u0000\u00c9\u001b\u0001\u0000"+
		"\u0000\u0000\u00ca\u00ce\u0003 \u0010\u0000\u00cb\u00ce\u0003\"\u0011"+
		"\u0000\u00cc\u00ce\u0003$\u0012\u0000\u00cd\u00ca\u0001\u0000\u0000\u0000"+
		"\u00cd\u00cb\u0001\u0000\u0000\u0000\u00cd\u00cc\u0001\u0000\u0000\u0000"+
		"\u00ce\u001d\u0001\u0000\u0000\u0000\u00cf\u00d0\u00056\u0000\u0000\u00d0"+
		"\u00d1\u0005\u0003\u0000\u0000\u00d1\u00d2\u0003\n\u0005\u0000\u00d2\u001f"+
		"\u0001\u0000\u0000\u0000\u00d3\u00d4\u0007\u0001\u0000\u0000\u00d4\u00d5"+
		"\u00056\u0000\u0000\u00d5\u00d6\u0005\u0003\u0000\u0000\u00d6\u00d7\u0003"+
		"\n\u0005\u0000\u00d7!\u0001\u0000\u0000\u0000\u00d8\u00da\u0007\u0002"+
		"\u0000\u0000\u00d9\u00d8\u0001\u0000\u0000\u0000\u00d9\u00da\u0001\u0000"+
		"\u0000\u0000\u00da\u00db\u0001\u0000\u0000\u0000\u00db\u00dc\u0005+\u0000"+
		"\u0000\u00dc\u00dd\u00056\u0000\u0000\u00dd\u00de\u0003&\u0013\u0000\u00de"+
		"\u00e2\u0005\b\u0000\u0000\u00df\u00e1\u0003*\u0015\u0000\u00e0\u00df"+
		"\u0001\u0000\u0000\u0000\u00e1\u00e4\u0001\u0000\u0000\u0000\u00e2\u00e0"+
		"\u0001\u0000\u0000\u0000\u00e2\u00e3\u0001\u0000\u0000\u0000\u00e3\u00e5"+
		"\u0001\u0000\u0000\u0000\u00e4\u00e2\u0001\u0000\u0000\u0000\u00e5\u00e6"+
		"\u0005\t\u0000\u0000\u00e6#\u0001\u0000\u0000\u0000\u00e7\u00e9\u0007"+
		"\u0002\u0000\u0000\u00e8\u00e7\u0001\u0000\u0000\u0000\u00e8\u00e9\u0001"+
		"\u0000\u0000\u0000\u00e9\u00ea\u0001\u0000\u0000\u0000\u00ea\u00eb\u0005"+
		",\u0000\u0000\u00eb\u00ec\u00056\u0000\u0000\u00ec\u00ed\u0003&\u0013"+
		"\u0000\u00ed\u00f1\u0005\b\u0000\u0000\u00ee\u00f0\u0003*\u0015\u0000"+
		"\u00ef\u00ee\u0001\u0000\u0000\u0000\u00f0\u00f3\u0001\u0000\u0000\u0000"+
		"\u00f1\u00ef\u0001\u0000\u0000\u0000\u00f1\u00f2\u0001\u0000\u0000\u0000"+
		"\u00f2\u00f4\u0001\u0000\u0000\u0000\u00f3\u00f1\u0001\u0000\u0000\u0000"+
		"\u00f4\u00f5\u0005\t\u0000\u0000\u00f5%\u0001\u0000\u0000\u0000\u00f6"+
		"\u00f8\u0005\u0004\u0000\u0000\u00f7\u00f9\u0003(\u0014\u0000\u00f8\u00f7"+
		"\u0001\u0000\u0000\u0000\u00f8\u00f9\u0001\u0000\u0000\u0000\u00f9\u00fe"+
		"\u0001\u0000\u0000\u0000\u00fa\u00fb\u0005\u0001\u0000\u0000\u00fb\u00fd"+
		"\u0003(\u0014\u0000\u00fc\u00fa\u0001\u0000\u0000\u0000\u00fd\u0100\u0001"+
		"\u0000\u0000\u0000\u00fe\u00fc\u0001\u0000\u0000\u0000\u00fe\u00ff\u0001"+
		"\u0000\u0000\u0000\u00ff\u0101\u0001\u0000\u0000\u0000\u0100\u00fe\u0001"+
		"\u0000\u0000\u0000\u0101\u0102\u0005\u0005\u0000\u0000\u0102\'\u0001\u0000"+
		"\u0000\u0000\u0103\u0104\u00056\u0000\u0000\u0104\u0105\u0005\u0003\u0000"+
		"\u0000\u0105\u0106\u0003\n\u0005\u0000\u0106)\u0001\u0000\u0000\u0000"+
		"\u0107\u010c\u0003,\u0016\u0000\u0108\u010c\u0003.\u0017\u0000\u0109\u010c"+
		"\u00030\u0018\u0000\u010a\u010c\u00036\u001b\u0000\u010b\u0107\u0001\u0000"+
		"\u0000\u0000\u010b\u0108\u0001\u0000\u0000\u0000\u010b\u0109\u0001\u0000"+
		"\u0000\u0000\u010b\u010a\u0001\u0000\u0000\u0000\u010c+\u0001\u0000\u0000"+
		"\u0000\u010d\u010e\u0005/\u0000\u0000\u010e\u010f\u0005\u0003\u0000\u0000"+
		"\u010f\u0110\u0003<\u001e\u0000\u0110-\u0001\u0000\u0000\u0000\u0111\u0112"+
		"\u00050\u0000\u0000\u0112\u0116\u0005\u0003\u0000\u0000\u0113\u0115\u0003"+
		"4\u001a\u0000\u0114\u0113\u0001\u0000\u0000\u0000\u0115\u0118\u0001\u0000"+
		"\u0000\u0000\u0116\u0114\u0001\u0000\u0000\u0000\u0116\u0117\u0001\u0000"+
		"\u0000\u0000\u0117/\u0001\u0000\u0000\u0000\u0118\u0116\u0001\u0000\u0000"+
		"\u0000\u0119\u011a\u00051\u0000\u0000\u011a\u011c\u0005\u0003\u0000\u0000"+
		"\u011b\u011d\u00032\u0019\u0000\u011c\u011b\u0001\u0000\u0000\u0000\u011d"+
		"\u011e\u0001\u0000\u0000\u0000\u011e\u011c\u0001\u0000\u0000\u0000\u011e"+
		"\u011f\u0001\u0000\u0000\u0000\u011f1\u0001\u0000\u0000\u0000\u0120\u0121"+
		"\u0003<\u001e\u0000\u0121\u0122\u0005\"\u0000\u0000\u0122\u0123\u0003"+
		"<\u001e\u0000\u01233\u0001\u0000\u0000\u0000\u0124\u0125\u0003X,\u0000"+
		"\u0125\u0126\u0005\u001b\u0000\u0000\u0126\u0127\u0003<\u001e\u0000\u0127"+
		"\u0130\u0001\u0000\u0000\u0000\u0128\u0129\u00056\u0000\u0000\u0129\u012a"+
		"\u0005\u0006\u0000\u0000\u012a\u012b\u0003<\u001e\u0000\u012b\u012c\u0005"+
		"\u0007\u0000\u0000\u012c\u012d\u0005\u001b\u0000\u0000\u012d\u012e\u0003"+
		"<\u001e\u0000\u012e\u0130\u0001\u0000\u0000\u0000\u012f\u0124\u0001\u0000"+
		"\u0000\u0000\u012f\u0128\u0001\u0000\u0000\u0000\u01305\u0001\u0000\u0000"+
		"\u0000\u0131\u0132\u00052\u0000\u0000\u0132\u0134\u0005\u0003\u0000\u0000"+
		"\u0133\u0135\u00038\u001c\u0000\u0134\u0133\u0001\u0000\u0000\u0000\u0135"+
		"\u0136\u0001\u0000\u0000\u0000\u0136\u0134\u0001\u0000\u0000\u0000\u0136"+
		"\u0137\u0001\u0000\u0000\u0000\u01377\u0001\u0000\u0000\u0000\u0138\u013e"+
		"\u0003:\u001d\u0000\u0139\u013a\u0003X,\u0000\u013a\u013b\u0005\u001b"+
		"\u0000\u0000\u013b\u013c\u0003:\u001d\u0000\u013c\u013e\u0001\u0000\u0000"+
		"\u0000\u013d\u0138\u0001\u0000\u0000\u0000\u013d\u0139\u0001\u0000\u0000"+
		"\u0000\u013e9\u0001\u0000\u0000\u0000\u013f\u0140\u00056\u0000\u0000\u0140"+
		"\u0149\u0005\u0004\u0000\u0000\u0141\u0146\u0003<\u001e\u0000\u0142\u0143"+
		"\u0005\u0001\u0000\u0000\u0143\u0145\u0003<\u001e\u0000\u0144\u0142\u0001"+
		"\u0000\u0000\u0000\u0145\u0148\u0001\u0000\u0000\u0000\u0146\u0144\u0001"+
		"\u0000\u0000\u0000\u0146\u0147\u0001\u0000\u0000\u0000\u0147\u014a\u0001"+
		"\u0000\u0000\u0000\u0148\u0146\u0001\u0000\u0000\u0000\u0149\u0141\u0001"+
		"\u0000\u0000\u0000\u0149\u014a\u0001\u0000\u0000\u0000\u014a\u014b\u0001"+
		"\u0000\u0000\u0000\u014b\u014c\u0005\u0005\u0000\u0000\u014c;\u0001\u0000"+
		"\u0000\u0000\u014d\u014e\u0006\u001e\uffff\uffff\u0000\u014e\u018d\u0003"+
		"F#\u0000\u014f\u0150\u0005\u0004\u0000\u0000\u0150\u0151\u0003<\u001e"+
		"\u0000\u0151\u0152\u0005\u0005\u0000\u0000\u0152\u018d\u0001\u0000\u0000"+
		"\u0000\u0153\u018d\u0003H$\u0000\u0154\u018d\u0003L&\u0000\u0155\u018d"+
		"\u0003N\'\u0000\u0156\u018d\u0003X,\u0000\u0157\u018d\u0003T*\u0000\u0158"+
		"\u018d\u0003R)\u0000\u0159\u015a\u0005\u000f\u0000\u0000\u015a\u018d\u0003"+
		"<\u001e\u0016\u015b\u015c\u0005\r\u0000\u0000\u015c\u018d\u0003<\u001e"+
		"\u0015\u015d\u015e\u0005\u000e\u0000\u0000\u015e\u018d\u0003<\u001e\u0014"+
		"\u015f\u0160\u0005\u001d\u0000\u0000\u0160\u0161\u0005\u0004\u0000\u0000"+
		"\u0161\u0162\u0003<\u001e\u0000\u0162\u0163\u0005\u0005\u0000\u0000\u0163"+
		"\u0164\u0005\b\u0000\u0000\u0164\u0165\u0003<\u001e\u0000\u0165\u0166"+
		"\u0005\t\u0000\u0000\u0166\u0167\u0005\u001e\u0000\u0000\u0167\u0168\u0005"+
		"\b\u0000\u0000\u0168\u0169\u0003<\u001e\u0000\u0169\u016a\u0005\t\u0000"+
		"\u0000\u016a\u018d\u0001\u0000\u0000\u0000\u016b\u016c\u0005\u001f\u0000"+
		"\u0000\u016c\u016d\u0005\u0004\u0000\u0000\u016d\u016e\u00056\u0000\u0000"+
		"\u016e\u016f\u0005\u0003\u0000\u0000\u016f\u0170\u0003\n\u0005\u0000\u0170"+
		"\u0171\u0005\u001b\u0000\u0000\u0171\u0172\u0003<\u001e\u0000\u0172\u0173"+
		"\u0005\u0005\u0000\u0000\u0173\u0174\u0005\b\u0000\u0000\u0174\u0175\u0003"+
		"<\u001e\u0000\u0175\u0176\u0005\t\u0000\u0000\u0176\u018d\u0001\u0000"+
		"\u0000\u0000\u0177\u0178\u0005 \u0000\u0000\u0178\u0179\u0005\u0004\u0000"+
		"\u0000\u0179\u017a\u0003<\u001e\u0000\u017a\u017b\u0005\u0005\u0000\u0000"+
		"\u017b\u017d\u0005\b\u0000\u0000\u017c\u017e\u0003>\u001f\u0000\u017d"+
		"\u017c\u0001\u0000\u0000\u0000\u017e\u017f\u0001\u0000\u0000\u0000\u017f"+
		"\u017d\u0001\u0000\u0000\u0000\u017f\u0180\u0001\u0000\u0000\u0000\u0180"+
		"\u0181\u0001\u0000\u0000\u0000\u0181\u0182\u0005\t\u0000\u0000\u0182\u018d"+
		"\u0001\u0000\u0000\u0000\u0183\u0184\u0005 \u0000\u0000\u0184\u0186\u0005"+
		"\b\u0000\u0000\u0185\u0187\u0003@ \u0000\u0186\u0185\u0001\u0000\u0000"+
		"\u0000\u0187\u0188\u0001\u0000\u0000\u0000\u0188\u0186\u0001\u0000\u0000"+
		"\u0000\u0188\u0189\u0001\u0000\u0000\u0000\u0189\u018a\u0001\u0000\u0000"+
		"\u0000\u018a\u018b\u0005\t\u0000\u0000\u018b\u018d\u0001\u0000\u0000\u0000"+
		"\u018c\u014d\u0001\u0000\u0000\u0000\u018c\u014f\u0001\u0000\u0000\u0000"+
		"\u018c\u0153\u0001\u0000\u0000\u0000\u018c\u0154\u0001\u0000\u0000\u0000"+
		"\u018c\u0155\u0001\u0000\u0000\u0000\u018c\u0156\u0001\u0000\u0000\u0000"+
		"\u018c\u0157\u0001\u0000\u0000\u0000\u018c\u0158\u0001\u0000\u0000\u0000"+
		"\u018c\u0159\u0001\u0000\u0000\u0000\u018c\u015b\u0001\u0000\u0000\u0000"+
		"\u018c\u015d\u0001\u0000\u0000\u0000\u018c\u015f\u0001\u0000\u0000\u0000"+
		"\u018c\u016b\u0001\u0000\u0000\u0000\u018c\u0177\u0001\u0000\u0000\u0000"+
		"\u018c\u0183\u0001\u0000\u0000\u0000\u018d\u01bd\u0001\u0000\u0000\u0000"+
		"\u018e\u018f\n\u0013\u0000\u0000\u018f\u0190\u0005\u0010\u0000\u0000\u0190"+
		"\u01bc\u0003<\u001e\u0014\u0191\u0192\n\u0012\u0000\u0000\u0192\u0193"+
		"\u0005\u0011\u0000\u0000\u0193\u01bc\u0003<\u001e\u0013\u0194\u0195\n"+
		"\u0011\u0000\u0000\u0195\u0196\u0005\u0012\u0000\u0000\u0196\u01bc\u0003"+
		"<\u001e\u0012\u0197\u0198\n\u0010\u0000\u0000\u0198\u0199\u0005\u0013"+
		"\u0000\u0000\u0199\u01bc\u0003<\u001e\u0011\u019a\u019b\n\u000f\u0000"+
		"\u0000\u019b\u019c\u0005\u0014\u0000\u0000\u019c\u01bc\u0003<\u001e\u0010"+
		"\u019d\u019e\n\u000e\u0000\u0000\u019e\u019f\u0005\u0015\u0000\u0000\u019f"+
		"\u01bc\u0003<\u001e\u000f\u01a0\u01a1\n\r\u0000\u0000\u01a1\u01a2\u0005"+
		"\u0016\u0000\u0000\u01a2\u01bc\u0003<\u001e\u000e\u01a3\u01a4\n\f\u0000"+
		"\u0000\u01a4\u01a5\u0005\u0017\u0000\u0000\u01a5\u01bc\u0003<\u001e\r"+
		"\u01a6\u01a7\n\u000b\u0000\u0000\u01a7\u01a8\u0005\u0018\u0000\u0000\u01a8"+
		"\u01bc\u0003<\u001e\f\u01a9\u01aa\n\n\u0000\u0000\u01aa\u01ab\u0005!\u0000"+
		"\u0000\u01ab\u01bc\u0003<\u001e\u000b\u01ac\u01ad\n\t\u0000\u0000\u01ad"+
		"\u01ae\u0005\u0019\u0000\u0000\u01ae\u01bc\u0003<\u001e\n\u01af\u01b0"+
		"\n\b\u0000\u0000\u01b0\u01b1\u0005\u001a\u0000\u0000\u01b1\u01bc\u0003"+
		"<\u001e\t\u01b2\u01b3\n\u0007\u0000\u0000\u01b3\u01b4\u0005\r\u0000\u0000"+
		"\u01b4\u01bc\u0003<\u001e\b\u01b5\u01b6\n\u0006\u0000\u0000\u01b6\u01b7"+
		"\u0005\u000e\u0000\u0000\u01b7\u01bc\u0003<\u001e\u0007\u01b8\u01b9\n"+
		"\u0005\u0000\u0000\u01b9\u01ba\u0005\u001c\u0000\u0000\u01ba\u01bc\u0003"+
		"<\u001e\u0006\u01bb\u018e\u0001\u0000\u0000\u0000\u01bb\u0191\u0001\u0000"+
		"\u0000\u0000\u01bb\u0194\u0001\u0000\u0000\u0000\u01bb\u0197\u0001\u0000"+
		"\u0000\u0000\u01bb\u019a\u0001\u0000\u0000\u0000\u01bb\u019d\u0001\u0000"+
		"\u0000\u0000\u01bb\u01a0\u0001\u0000\u0000\u0000\u01bb\u01a3\u0001\u0000"+
		"\u0000\u0000\u01bb\u01a6\u0001\u0000\u0000\u0000\u01bb\u01a9\u0001\u0000"+
		"\u0000\u0000\u01bb\u01ac\u0001\u0000\u0000\u0000\u01bb\u01af\u0001\u0000"+
		"\u0000\u0000\u01bb\u01b2\u0001\u0000\u0000\u0000\u01bb\u01b5\u0001\u0000"+
		"\u0000\u0000\u01bb\u01b8\u0001\u0000\u0000\u0000\u01bc\u01bf\u0001\u0000"+
		"\u0000\u0000\u01bd\u01bb\u0001\u0000\u0000\u0000\u01bd\u01be\u0001\u0000"+
		"\u0000\u0000\u01be=\u0001\u0000\u0000\u0000\u01bf\u01bd\u0001\u0000\u0000"+
		"\u0000\u01c0\u01c1\u0003B!\u0000\u01c1\u01c2\u0005\"\u0000\u0000\u01c2"+
		"\u01c3\u0003<\u001e\u0000\u01c3\u01c8\u0001\u0000\u0000\u0000\u01c4\u01c5"+
		"\u0005\u001e\u0000\u0000\u01c5\u01c6\u0005\"\u0000\u0000\u01c6\u01c8\u0003"+
		"<\u001e\u0000\u01c7\u01c0\u0001\u0000\u0000\u0000\u01c7\u01c4\u0001\u0000"+
		"\u0000\u0000\u01c8?\u0001\u0000\u0000\u0000\u01c9\u01ca\u0003<\u001e\u0000"+
		"\u01ca\u01cb\u0005\"\u0000\u0000\u01cb\u01cc\u0003<\u001e\u0000\u01cc"+
		"\u01d1\u0001\u0000\u0000\u0000\u01cd\u01ce\u0005\u001e\u0000\u0000\u01ce"+
		"\u01cf\u0005\"\u0000\u0000\u01cf\u01d1\u0003<\u001e\u0000\u01d0\u01c9"+
		"\u0001\u0000\u0000\u0000\u01d0\u01cd\u0001\u0000\u0000\u0000\u01d1A\u0001"+
		"\u0000\u0000\u0000\u01d2\u01d5\u0003F#\u0000\u01d3\u01d5\u0003T*\u0000"+
		"\u01d4\u01d2\u0001\u0000\u0000\u0000\u01d4\u01d3\u0001\u0000\u0000\u0000"+
		"\u01d5C\u0001\u0000\u0000\u0000\u01d6\u01d7\u0006\"\uffff\uffff\u0000"+
		"\u01d7\u01de\u0003\u0004\u0002\u0000\u01d8\u01de\u00056\u0000\u0000\u01d9"+
		"\u01da\u0005\u0004\u0000\u0000\u01da\u01db\u0003D\"\u0000\u01db\u01dc"+
		"\u0005\u0005\u0000\u0000\u01dc\u01de\u0001\u0000\u0000\u0000\u01dd\u01d6"+
		"\u0001\u0000\u0000\u0000\u01dd\u01d8\u0001\u0000\u0000\u0000\u01dd\u01d9"+
		"\u0001\u0000\u0000\u0000\u01de\u01e4\u0001\u0000\u0000\u0000\u01df\u01e0"+
		"\n\u0001\u0000\u0000\u01e0\u01e1\u0005\n\u0000\u0000\u01e1\u01e3\u0003"+
		"D\"\u0002\u01e2\u01df\u0001\u0000\u0000\u0000\u01e3\u01e6\u0001\u0000"+
		"\u0000\u0000\u01e4\u01e2\u0001\u0000\u0000\u0000\u01e4\u01e5\u0001\u0000"+
		"\u0000\u0000\u01e5E\u0001\u0000\u0000\u0000\u01e6\u01e4\u0001\u0000\u0000"+
		"\u0000\u01e7\u01e8\u0007\u0003\u0000\u0000\u01e8G\u0001\u0000\u0000\u0000"+
		"\u01e9\u01ea\u0005\u0006\u0000\u0000\u01ea\u0202\u0005\u0007\u0000\u0000"+
		"\u01eb\u01ec\u0005\u0006\u0000\u0000\u01ec\u01f1\u0003J%\u0000\u01ed\u01ee"+
		"\u0005\u0001\u0000\u0000\u01ee\u01f0\u0003J%\u0000\u01ef\u01ed\u0001\u0000"+
		"\u0000\u0000\u01f0\u01f3\u0001\u0000\u0000\u0000\u01f1\u01ef\u0001\u0000"+
		"\u0000\u0000\u01f1\u01f2\u0001\u0000\u0000\u0000\u01f2\u01f4\u0001\u0000"+
		"\u0000\u0000\u01f3\u01f1\u0001\u0000\u0000\u0000\u01f4\u01f5\u0005\u0007"+
		"\u0000\u0000\u01f5\u0202\u0001\u0000\u0000\u0000\u01f6\u01f7\u0005\u0006"+
		"\u0000\u0000\u01f7\u01fc\u0003<\u001e\u0000\u01f8\u01f9\u0005\u0001\u0000"+
		"\u0000\u01f9\u01fb\u0003<\u001e\u0000\u01fa\u01f8\u0001\u0000\u0000\u0000"+
		"\u01fb\u01fe\u0001\u0000\u0000\u0000\u01fc\u01fa\u0001\u0000\u0000\u0000"+
		"\u01fc\u01fd\u0001\u0000\u0000\u0000\u01fd\u01ff\u0001\u0000\u0000\u0000"+
		"\u01fe\u01fc\u0001\u0000\u0000\u0000\u01ff\u0200\u0005\u0007\u0000\u0000"+
		"\u0200\u0202\u0001\u0000\u0000\u0000\u0201\u01e9\u0001\u0000\u0000\u0000"+
		"\u0201\u01eb\u0001\u0000\u0000\u0000\u0201\u01f6\u0001\u0000\u0000\u0000"+
		"\u0202I\u0001\u0000\u0000\u0000\u0203\u0204\u0003<\u001e\u0000\u0204\u0205"+
		"\u0005\"\u0000\u0000\u0205\u0206\u0003<\u001e\u0000\u0206K\u0001\u0000"+
		"\u0000\u0000\u0207\u0210\u0005\b\u0000\u0000\u0208\u020d\u0003<\u001e"+
		"\u0000\u0209\u020a\u0005\u0001\u0000\u0000\u020a\u020c\u0003<\u001e\u0000"+
		"\u020b\u0209\u0001\u0000\u0000\u0000\u020c\u020f\u0001\u0000\u0000\u0000"+
		"\u020d\u020b\u0001\u0000\u0000\u0000\u020d\u020e\u0001\u0000\u0000\u0000"+
		"\u020e\u0211\u0001\u0000\u0000\u0000\u020f\u020d\u0001\u0000\u0000\u0000"+
		"\u0210\u0208\u0001\u0000\u0000\u0000\u0210\u0211\u0001\u0000\u0000\u0000"+
		"\u0211\u0212\u0001\u0000\u0000\u0000\u0212\u0213\u0005\t\u0000\u0000\u0213"+
		"M\u0001\u0000\u0000\u0000\u0214\u021d\u0006\'\uffff\uffff\u0000\u0215"+
		"\u021e\u0003R)\u0000\u0216\u021e\u0003X,\u0000\u0217\u021e\u0003H$\u0000"+
		"\u0218\u021e\u0003L&\u0000\u0219\u021a\u0005\u0004\u0000\u0000\u021a\u021b"+
		"\u0003<\u001e\u0000\u021b\u021c\u0005\u0005\u0000\u0000\u021c\u021e\u0001"+
		"\u0000\u0000\u0000\u021d\u0215\u0001\u0000\u0000\u0000\u021d\u0216\u0001"+
		"\u0000\u0000\u0000\u021d\u0217\u0001\u0000\u0000\u0000\u021d\u0218\u0001"+
		"\u0000\u0000\u0000\u021d\u0219\u0001\u0000\u0000\u0000\u021e\u021f\u0001"+
		"\u0000\u0000\u0000\u021f\u0220\u0005\u0006\u0000\u0000\u0220\u0221\u0003"+
		"P(\u0000\u0221\u0222\u0005\u0007\u0000\u0000\u0222\u022a\u0001\u0000\u0000"+
		"\u0000\u0223\u0224\n\u0002\u0000\u0000\u0224\u0225\u0005\u0006\u0000\u0000"+
		"\u0225\u0226\u0003P(\u0000\u0226\u0227\u0005\u0007\u0000\u0000\u0227\u0229"+
		"\u0001\u0000\u0000\u0000\u0228\u0223\u0001\u0000\u0000\u0000\u0229\u022c"+
		"\u0001\u0000\u0000\u0000\u022a\u0228\u0001\u0000\u0000\u0000\u022a\u022b"+
		"\u0001\u0000\u0000\u0000\u022bO\u0001\u0000\u0000\u0000\u022c\u022a\u0001"+
		"\u0000\u0000\u0000\u022d\u022e\u0003<\u001e\u0000\u022e\u022f\u0005\u0003"+
		"\u0000\u0000\u022f\u0230\u0003<\u001e\u0000\u0230\u0233\u0001\u0000\u0000"+
		"\u0000\u0231\u0233\u0003<\u001e\u0000\u0232\u022d\u0001\u0000\u0000\u0000"+
		"\u0232\u0231\u0001\u0000\u0000\u0000\u0233Q\u0001\u0000\u0000\u0000\u0234"+
		"\u0235\u00056\u0000\u0000\u0235\u023e\u0005\u0004\u0000\u0000\u0236\u023b"+
		"\u0003<\u001e\u0000\u0237\u0238\u0005\u0001\u0000\u0000\u0238\u023a\u0003"+
		"<\u001e\u0000\u0239\u0237\u0001\u0000\u0000\u0000\u023a\u023d\u0001\u0000"+
		"\u0000\u0000\u023b\u0239\u0001\u0000\u0000\u0000\u023b\u023c\u0001\u0000"+
		"\u0000\u0000\u023c\u023f\u0001\u0000\u0000\u0000\u023d\u023b\u0001\u0000"+
		"\u0000\u0000\u023e\u0236\u0001\u0000\u0000\u0000\u023e\u023f\u0001\u0000"+
		"\u0000\u0000\u023f\u0240\u0001\u0000\u0000\u0000\u0240\u0241\u0005\u0005"+
		"\u0000\u0000\u0241S\u0001\u0000\u0000\u0000\u0242\u0243\u0003\n\u0005"+
		"\u0000\u0243\u0244\u0005\b\u0000\u0000\u0244\u0249\u0003V+\u0000\u0245"+
		"\u0246\u0005\u0001\u0000\u0000\u0246\u0248\u0003V+\u0000\u0247\u0245\u0001"+
		"\u0000\u0000\u0000\u0248\u024b\u0001\u0000\u0000\u0000\u0249\u0247\u0001"+
		"\u0000\u0000\u0000\u0249\u024a\u0001\u0000\u0000\u0000\u024a\u024c\u0001"+
		"\u0000\u0000\u0000\u024b\u0249\u0001\u0000\u0000\u0000\u024c\u024d\u0005"+
		"\t\u0000\u0000\u024dU\u0001\u0000\u0000\u0000\u024e\u024f\u00056\u0000"+
		"\u0000\u024f\u0250\u0005\u001b\u0000\u0000\u0250\u0251\u0003<\u001e\u0000"+
		"\u0251W\u0001\u0000\u0000\u0000\u0252\u0257\u00056\u0000\u0000\u0253\u0254"+
		"\u0005\u0002\u0000\u0000\u0254\u0256\u00056\u0000\u0000\u0255\u0253\u0001"+
		"\u0000\u0000\u0000\u0256\u0259\u0001\u0000\u0000\u0000\u0257\u0255\u0001"+
		"\u0000\u0000\u0000\u0257\u0258\u0001\u0000\u0000\u0000\u0258Y\u0001\u0000"+
		"\u0000\u0000\u0259\u0257\u0001\u0000\u0000\u00001\\^kuy\u007f\u0087\u0092"+
		"\u009a\u00a8\u00b0\u00b6\u00cd\u00d9\u00e2\u00e8\u00f1\u00f8\u00fe\u010b"+
		"\u0116\u011e\u012f\u0136\u013d\u0146\u0149\u017f\u0188\u018c\u01bb\u01bd"+
		"\u01c7\u01d0\u01d4\u01dd\u01e4\u01f1\u01fc\u0201\u020d\u0210\u021d\u022a"+
		"\u0232\u023b\u023e\u0249\u0257";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}