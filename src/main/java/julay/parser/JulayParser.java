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
		RCURLY=9, PARALLEL=10, TRUE=11, FALSE=12, AND=13, OR=14, NOT=15, BANG=16, 
		TIMES=17, DIV=18, MOD=19, PLUS=20, MINUS=21, LT=22, LTE=23, GT=24, GTE=25, 
		EQ=26, BANG_NEQ=27, NEQ=28, ASGN_EQ=29, IMPLIES=30, IF=31, ELSE=32, LET=33, 
		WHEN=34, ARROW=35, IMPORT=36, PCLASS=37, OCLASS=38, PROC=39, PROGRAM=40, 
		SPEC=41, VAR=42, CONSTRUCTOR=43, TRANSITION=44, SERVICE=45, CONSUMER=46, 
		GUARD=47, TRANSIT=48, ERROR=49, EFFECT=50, FUN=51, INT=52, ID=53, STRING=54, 
		WS=55, COMMENT=56, LINE_COMMENT=57;
	public static final int
		RULE_root = 0, RULE_import_stmt = 1, RULE_qualified_name = 2, RULE_qual_segment = 3, 
		RULE_decl = 4, RULE_typeExpr = 5, RULE_typeParams = 6, RULE_fun_decl = 7, 
		RULE_pclass = 8, RULE_oclass = 9, RULE_proc = 10, RULE_program = 11, RULE_spec = 12, 
		RULE_pclass_body = 13, RULE_field = 14, RULE_var = 15, RULE_constructor = 16, 
		RULE_transition = 17, RULE_args = 18, RULE_arg = 19, RULE_action_body = 20, 
		RULE_guard = 21, RULE_transit = 22, RULE_error = 23, RULE_var_transit = 24, 
		RULE_effect = 25, RULE_effect_stmt = 26, RULE_effect_call = 27, RULE_expr = 28, 
		RULE_when_subject_arm = 29, RULE_when_guard_arm = 30, RULE_when_pattern = 31, 
		RULE_when_literal = 32, RULE_proc_expr = 33, RULE_value = 34, RULE_list_literal = 35, 
		RULE_index_expr = 36, RULE_index_or_slice = 37, RULE_fun_call = 38, RULE_oclass_literal = 39, 
		RULE_struct_field_assign = 40, RULE_field_access = 41;
	private static String[] makeRuleNames() {
		return new String[] {
			"root", "import_stmt", "qualified_name", "qual_segment", "decl", "typeExpr", 
			"typeParams", "fun_decl", "pclass", "oclass", "proc", "program", "spec", 
			"pclass_body", "field", "var", "constructor", "transition", "args", "arg", 
			"action_body", "guard", "transit", "error", "var_transit", "effect", 
			"effect_stmt", "effect_call", "expr", "when_subject_arm", "when_guard_arm", 
			"when_pattern", "when_literal", "proc_expr", "value", "list_literal", 
			"index_expr", "index_or_slice", "fun_call", "oclass_literal", "struct_field_assign", 
			"field_access"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "','", "'.'", "':'", "'('", "')'", "'['", "']'", "'{'", "'}'", 
			"'||'", "'true'", "'false'", "'&'", "'|'", "'~'", "'!'", "'*'", "'/'", 
			"'%'", "'+'", "'-'", "'<'", "'<='", "'>'", "'>='", "'='", "'!='", "'#'", 
			"':='", "'=>'", "'if'", "'else'", "'let'", "'when'", "'->'", "'import'", 
			"'p-class'", "'o-class'", "'proc'", "'program'", "'spec'", "'var'", "'constructor'", 
			"'transition'", "'p2p-service'", "'p2p-consumer'", "'guard'", "'transit'", 
			"'error'", "'effect'", "'fun'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "COMMA", "DOT", "COLON", "LPAREN", "RPAREN", "LBRACK", "RBRACK", 
			"LCURLY", "RCURLY", "PARALLEL", "TRUE", "FALSE", "AND", "OR", "NOT", 
			"BANG", "TIMES", "DIV", "MOD", "PLUS", "MINUS", "LT", "LTE", "GT", "GTE", 
			"EQ", "BANG_NEQ", "NEQ", "ASGN_EQ", "IMPLIES", "IF", "ELSE", "LET", "WHEN", 
			"ARROW", "IMPORT", "PCLASS", "OCLASS", "PROC", "PROGRAM", "SPEC", "VAR", 
			"CONSTRUCTOR", "TRANSITION", "SERVICE", "CONSUMER", "GUARD", "TRANSIT", 
			"ERROR", "EFFECT", "FUN", "INT", "ID", "STRING", "WS", "COMMENT", "LINE_COMMENT"
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
			setState(88);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 2256129140719616L) != 0)) {
				{
				setState(86);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case IMPORT:
					{
					setState(84);
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
					setState(85);
					decl();
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				}
				setState(90);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(91);
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
			setState(93);
			match(IMPORT);
			setState(94);
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
			setState(96);
			match(ID);
			setState(99); 
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					{
					setState(97);
					match(DOT);
					setState(98);
					qual_segment();
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(101); 
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
			setState(103);
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
			setState(111);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case PCLASS:
				enterOuterAlt(_localctx, 1);
				{
				setState(105);
				pclass();
				}
				break;
			case OCLASS:
				enterOuterAlt(_localctx, 2);
				{
				setState(106);
				oclass();
				}
				break;
			case PROC:
				enterOuterAlt(_localctx, 3);
				{
				setState(107);
				proc();
				}
				break;
			case PROGRAM:
				enterOuterAlt(_localctx, 4);
				{
				setState(108);
				program();
				}
				break;
			case SPEC:
				enterOuterAlt(_localctx, 5);
				{
				setState(109);
				spec();
				}
				break;
			case FUN:
				enterOuterAlt(_localctx, 6);
				{
				setState(110);
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
		public TypeExprContext typeExpr() {
			return getRuleContext(TypeExprContext.class,0);
		}
		public TerminalNode LPAREN() { return getToken(JulayParser.LPAREN, 0); }
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
		try {
			setState(120);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,4,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(113);
				match(ID);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(114);
				match(ID);
				setState(115);
				typeExpr();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(116);
				match(LPAREN);
				setState(117);
				typeExpr();
				setState(118);
				match(RPAREN);
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
		enterRule(_localctx, 12, RULE_typeParams);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(122);
			match(LT);
			setState(123);
			match(ID);
			setState(128);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(124);
				match(COMMA);
				setState(125);
				match(ID);
				}
				}
				setState(130);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(131);
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
		enterRule(_localctx, 14, RULE_fun_decl);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(133);
			match(FUN);
			setState(134);
			match(ID);
			setState(136);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LT) {
				{
				setState(135);
				typeParams();
				}
			}

			setState(138);
			args();
			setState(139);
			match(COLON);
			setState(140);
			typeExpr();
			setState(141);
			match(EQ);
			setState(142);
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
		enterRule(_localctx, 16, RULE_pclass);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(144);
			match(PCLASS);
			setState(145);
			match(ID);
			setState(146);
			match(LCURLY);
			setState(150);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 136339441844224L) != 0)) {
				{
				{
				setState(147);
				pclass_body();
				}
				}
				setState(152);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(153);
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
		enterRule(_localctx, 18, RULE_oclass);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(155);
			match(OCLASS);
			setState(156);
			match(ID);
			setState(158);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LT) {
				{
				setState(157);
				typeParams();
				}
			}

			setState(160);
			match(LCURLY);
			setState(164);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==ID) {
				{
				{
				setState(161);
				field();
				}
				}
				setState(166);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(167);
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
		enterRule(_localctx, 20, RULE_proc);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(169);
			match(PROC);
			setState(170);
			match(ID);
			setState(171);
			match(ASGN_EQ);
			setState(172);
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
		enterRule(_localctx, 22, RULE_program);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(174);
			match(PROGRAM);
			setState(175);
			match(ID);
			setState(176);
			match(ASGN_EQ);
			setState(177);
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
		enterRule(_localctx, 24, RULE_spec);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(179);
			match(SPEC);
			setState(180);
			match(ID);
			setState(181);
			match(ASGN_EQ);
			setState(182);
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
		enterRule(_localctx, 26, RULE_pclass_body);
		try {
			setState(187);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case VAR:
				enterOuterAlt(_localctx, 1);
				{
				setState(184);
				var();
				}
				break;
			case CONSTRUCTOR:
				enterOuterAlt(_localctx, 2);
				{
				setState(185);
				constructor();
				}
				break;
			case TRANSITION:
			case SERVICE:
			case CONSUMER:
				enterOuterAlt(_localctx, 3);
				{
				setState(186);
				transition();
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
		enterRule(_localctx, 28, RULE_field);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(189);
			match(ID);
			setState(190);
			match(COLON);
			setState(191);
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
		public TerminalNode VAR() { return getToken(JulayParser.VAR, 0); }
		public TerminalNode ID() { return getToken(JulayParser.ID, 0); }
		public TerminalNode COLON() { return getToken(JulayParser.COLON, 0); }
		public TypeExprContext typeExpr() {
			return getRuleContext(TypeExprContext.class,0);
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
		enterRule(_localctx, 30, RULE_var);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(193);
			match(VAR);
			setState(194);
			match(ID);
			setState(195);
			match(COLON);
			setState(196);
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
		enterRule(_localctx, 32, RULE_constructor);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(198);
			match(CONSTRUCTOR);
			setState(199);
			match(ID);
			setState(200);
			args();
			setState(201);
			match(LCURLY);
			setState(205);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 2111062325329920L) != 0)) {
				{
				{
				setState(202);
				action_body();
				}
				}
				setState(207);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(208);
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
		enterRule(_localctx, 34, RULE_transition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(211);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==SERVICE || _la==CONSUMER) {
				{
				setState(210);
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

			setState(213);
			match(TRANSITION);
			setState(214);
			match(ID);
			setState(215);
			args();
			setState(216);
			match(LCURLY);
			setState(220);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 2111062325329920L) != 0)) {
				{
				{
				setState(217);
				action_body();
				}
				}
				setState(222);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(223);
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
		enterRule(_localctx, 36, RULE_args);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(225);
			match(LPAREN);
			setState(227);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ID) {
				{
				setState(226);
				arg();
				}
			}

			setState(233);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(229);
				match(COMMA);
				setState(230);
				arg();
				}
				}
				setState(235);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(236);
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
		enterRule(_localctx, 38, RULE_arg);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(238);
			match(ID);
			setState(239);
			match(COLON);
			setState(240);
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
		enterRule(_localctx, 40, RULE_action_body);
		try {
			setState(246);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case GUARD:
				enterOuterAlt(_localctx, 1);
				{
				setState(242);
				guard();
				}
				break;
			case TRANSIT:
				enterOuterAlt(_localctx, 2);
				{
				setState(243);
				transit();
				}
				break;
			case ERROR:
				enterOuterAlt(_localctx, 3);
				{
				setState(244);
				error();
				}
				break;
			case EFFECT:
				enterOuterAlt(_localctx, 4);
				{
				setState(245);
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
		enterRule(_localctx, 42, RULE_guard);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(248);
			match(GUARD);
			setState(249);
			match(COLON);
			setState(250);
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
		enterRule(_localctx, 44, RULE_transit);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(252);
			match(TRANSIT);
			setState(253);
			match(COLON);
			setState(257);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==ID) {
				{
				{
				setState(254);
				var_transit();
				}
				}
				setState(259);
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
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
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
		enterRule(_localctx, 46, RULE_error);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(260);
			match(ERROR);
			setState(261);
			match(COLON);
			setState(262);
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
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
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
		enterRule(_localctx, 48, RULE_var_transit);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(264);
			field_access();
			setState(265);
			match(ASGN_EQ);
			setState(266);
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
		enterRule(_localctx, 50, RULE_effect);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(268);
			match(EFFECT);
			setState(269);
			match(COLON);
			setState(271); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(270);
				effect_stmt();
				}
				}
				setState(273); 
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
		enterRule(_localctx, 52, RULE_effect_stmt);
		try {
			setState(280);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,19,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(275);
				effect_call();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(276);
				field_access();
				setState(277);
				match(ASGN_EQ);
				setState(278);
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
		enterRule(_localctx, 54, RULE_effect_call);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(282);
			match(ID);
			setState(283);
			match(LPAREN);
			setState(292);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 31525225308985424L) != 0)) {
				{
				setState(284);
				expr(0);
				setState(289);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(285);
					match(COMMA);
					setState(286);
					expr(0);
					}
					}
					setState(291);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
			}

			setState(294);
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
		public ValueContext value() {
			return getRuleContext(ValueContext.class,0);
		}
		public TerminalNode LPAREN() { return getToken(JulayParser.LPAREN, 0); }
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public TerminalNode RPAREN() { return getToken(JulayParser.RPAREN, 0); }
		public TerminalNode NOT() { return getToken(JulayParser.NOT, 0); }
		public TerminalNode BANG() { return getToken(JulayParser.BANG, 0); }
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
		public TerminalNode EQ() { return getToken(JulayParser.EQ, 0); }
		public TerminalNode NEQ() { return getToken(JulayParser.NEQ, 0); }
		public TerminalNode BANG_NEQ() { return getToken(JulayParser.BANG_NEQ, 0); }
		public TerminalNode AND() { return getToken(JulayParser.AND, 0); }
		public TerminalNode OR() { return getToken(JulayParser.OR, 0); }
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
		int _startState = 56;
		enterRecursionRule(_localctx, 56, RULE_expr, _p);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(351);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,24,_ctx) ) {
			case 1:
				{
				setState(297);
				value();
				}
				break;
			case 2:
				{
				setState(298);
				match(LPAREN);
				setState(299);
				expr(0);
				setState(300);
				match(RPAREN);
				}
				break;
			case 3:
				{
				setState(302);
				match(NOT);
				setState(303);
				expr(21);
				}
				break;
			case 4:
				{
				setState(304);
				match(BANG);
				setState(305);
				expr(20);
				}
				break;
			case 5:
				{
				setState(306);
				match(IF);
				setState(307);
				match(LPAREN);
				setState(308);
				expr(0);
				setState(309);
				match(RPAREN);
				setState(310);
				match(LCURLY);
				setState(311);
				expr(0);
				setState(312);
				match(RCURLY);
				setState(313);
				match(ELSE);
				setState(314);
				match(LCURLY);
				setState(315);
				expr(0);
				setState(316);
				match(RCURLY);
				}
				break;
			case 6:
				{
				setState(318);
				match(LET);
				setState(319);
				match(LPAREN);
				setState(320);
				match(ID);
				setState(321);
				match(COLON);
				setState(322);
				typeExpr();
				setState(323);
				match(ASGN_EQ);
				setState(324);
				expr(0);
				setState(325);
				match(RPAREN);
				setState(326);
				match(LCURLY);
				setState(327);
				expr(0);
				setState(328);
				match(RCURLY);
				}
				break;
			case 7:
				{
				setState(330);
				match(WHEN);
				setState(331);
				match(LPAREN);
				setState(332);
				expr(0);
				setState(333);
				match(RPAREN);
				setState(334);
				match(LCURLY);
				setState(336); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(335);
					when_subject_arm();
					}
					}
					setState(338); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & 31525201686566928L) != 0) );
				setState(340);
				match(RCURLY);
				}
				break;
			case 8:
				{
				setState(342);
				match(WHEN);
				setState(343);
				match(LCURLY);
				setState(345); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(344);
					when_guard_arm();
					}
					}
					setState(347); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & 31525229603952720L) != 0) );
				setState(349);
				match(RCURLY);
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(400);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,26,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(398);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,25,_ctx) ) {
					case 1:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(353);
						if (!(precpred(_ctx, 19))) throw new FailedPredicateException(this, "precpred(_ctx, 19)");
						setState(354);
						match(TIMES);
						setState(355);
						expr(20);
						}
						break;
					case 2:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(356);
						if (!(precpred(_ctx, 18))) throw new FailedPredicateException(this, "precpred(_ctx, 18)");
						setState(357);
						match(DIV);
						setState(358);
						expr(19);
						}
						break;
					case 3:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(359);
						if (!(precpred(_ctx, 17))) throw new FailedPredicateException(this, "precpred(_ctx, 17)");
						setState(360);
						match(MOD);
						setState(361);
						expr(18);
						}
						break;
					case 4:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(362);
						if (!(precpred(_ctx, 16))) throw new FailedPredicateException(this, "precpred(_ctx, 16)");
						setState(363);
						match(PLUS);
						setState(364);
						expr(17);
						}
						break;
					case 5:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(365);
						if (!(precpred(_ctx, 15))) throw new FailedPredicateException(this, "precpred(_ctx, 15)");
						setState(366);
						match(MINUS);
						setState(367);
						expr(16);
						}
						break;
					case 6:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(368);
						if (!(precpred(_ctx, 14))) throw new FailedPredicateException(this, "precpred(_ctx, 14)");
						setState(369);
						match(LT);
						setState(370);
						expr(15);
						}
						break;
					case 7:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(371);
						if (!(precpred(_ctx, 13))) throw new FailedPredicateException(this, "precpred(_ctx, 13)");
						setState(372);
						match(LTE);
						setState(373);
						expr(14);
						}
						break;
					case 8:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(374);
						if (!(precpred(_ctx, 12))) throw new FailedPredicateException(this, "precpred(_ctx, 12)");
						setState(375);
						match(GT);
						setState(376);
						expr(13);
						}
						break;
					case 9:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(377);
						if (!(precpred(_ctx, 11))) throw new FailedPredicateException(this, "precpred(_ctx, 11)");
						setState(378);
						match(GTE);
						setState(379);
						expr(12);
						}
						break;
					case 10:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(380);
						if (!(precpred(_ctx, 10))) throw new FailedPredicateException(this, "precpred(_ctx, 10)");
						setState(381);
						match(EQ);
						setState(382);
						expr(11);
						}
						break;
					case 11:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(383);
						if (!(precpred(_ctx, 9))) throw new FailedPredicateException(this, "precpred(_ctx, 9)");
						setState(384);
						match(NEQ);
						setState(385);
						expr(10);
						}
						break;
					case 12:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(386);
						if (!(precpred(_ctx, 8))) throw new FailedPredicateException(this, "precpred(_ctx, 8)");
						setState(387);
						match(BANG_NEQ);
						setState(388);
						expr(9);
						}
						break;
					case 13:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(389);
						if (!(precpred(_ctx, 7))) throw new FailedPredicateException(this, "precpred(_ctx, 7)");
						setState(390);
						match(AND);
						setState(391);
						expr(8);
						}
						break;
					case 14:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(392);
						if (!(precpred(_ctx, 6))) throw new FailedPredicateException(this, "precpred(_ctx, 6)");
						setState(393);
						match(OR);
						setState(394);
						expr(7);
						}
						break;
					case 15:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(395);
						if (!(precpred(_ctx, 5))) throw new FailedPredicateException(this, "precpred(_ctx, 5)");
						setState(396);
						match(IMPLIES);
						setState(397);
						expr(6);
						}
						break;
					}
					} 
				}
				setState(402);
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
		enterRule(_localctx, 58, RULE_when_subject_arm);
		try {
			setState(410);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case LPAREN:
			case TRUE:
			case FALSE:
			case INT:
			case ID:
			case STRING:
				enterOuterAlt(_localctx, 1);
				{
				setState(403);
				when_pattern();
				setState(404);
				match(ARROW);
				setState(405);
				expr(0);
				}
				break;
			case ELSE:
				enterOuterAlt(_localctx, 2);
				{
				setState(407);
				match(ELSE);
				setState(408);
				match(ARROW);
				setState(409);
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
		enterRule(_localctx, 60, RULE_when_guard_arm);
		try {
			setState(419);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case LPAREN:
			case LBRACK:
			case TRUE:
			case FALSE:
			case NOT:
			case BANG:
			case IF:
			case LET:
			case WHEN:
			case INT:
			case ID:
			case STRING:
				enterOuterAlt(_localctx, 1);
				{
				setState(412);
				expr(0);
				setState(413);
				match(ARROW);
				setState(414);
				expr(0);
				}
				break;
			case ELSE:
				enterOuterAlt(_localctx, 2);
				{
				setState(416);
				match(ELSE);
				setState(417);
				match(ARROW);
				setState(418);
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
		public When_literalContext when_literal() {
			return getRuleContext(When_literalContext.class,0);
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
		enterRule(_localctx, 62, RULE_when_pattern);
		try {
			setState(423);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case TRUE:
			case FALSE:
			case INT:
			case STRING:
				enterOuterAlt(_localctx, 1);
				{
				setState(421);
				when_literal();
				}
				break;
			case LPAREN:
			case ID:
				enterOuterAlt(_localctx, 2);
				{
				setState(422);
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
	public static class When_literalContext extends ParserRuleContext {
		public TerminalNode INT() { return getToken(JulayParser.INT, 0); }
		public TerminalNode STRING() { return getToken(JulayParser.STRING, 0); }
		public TerminalNode TRUE() { return getToken(JulayParser.TRUE, 0); }
		public TerminalNode FALSE() { return getToken(JulayParser.FALSE, 0); }
		public When_literalContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_when_literal; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterWhen_literal(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitWhen_literal(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitWhen_literal(this);
			else return visitor.visitChildren(this);
		}
	}

	public final When_literalContext when_literal() throws RecognitionException {
		When_literalContext _localctx = new When_literalContext(_ctx, getState());
		enterRule(_localctx, 64, RULE_when_literal);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(425);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 22517998136858624L) != 0)) ) {
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
		int _startState = 66;
		enterRecursionRule(_localctx, 66, RULE_proc_expr, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(434);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,30,_ctx) ) {
			case 1:
				{
				setState(428);
				qualified_name();
				}
				break;
			case 2:
				{
				setState(429);
				match(ID);
				}
				break;
			case 3:
				{
				setState(430);
				match(LPAREN);
				setState(431);
				proc_expr(0);
				setState(432);
				match(RPAREN);
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(441);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,31,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					{
					_localctx = new Proc_exprContext(_parentctx, _parentState);
					pushNewRecursionContext(_localctx, _startState, RULE_proc_expr);
					setState(436);
					if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
					setState(437);
					match(PARALLEL);
					setState(438);
					proc_expr(2);
					}
					} 
				}
				setState(443);
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
	public static class ValueContext extends ParserRuleContext {
		public List_literalContext list_literal() {
			return getRuleContext(List_literalContext.class,0);
		}
		public Index_exprContext index_expr() {
			return getRuleContext(Index_exprContext.class,0);
		}
		public Oclass_literalContext oclass_literal() {
			return getRuleContext(Oclass_literalContext.class,0);
		}
		public Fun_callContext fun_call() {
			return getRuleContext(Fun_callContext.class,0);
		}
		public Field_accessContext field_access() {
			return getRuleContext(Field_accessContext.class,0);
		}
		public TerminalNode INT() { return getToken(JulayParser.INT, 0); }
		public TerminalNode TRUE() { return getToken(JulayParser.TRUE, 0); }
		public TerminalNode FALSE() { return getToken(JulayParser.FALSE, 0); }
		public TerminalNode STRING() { return getToken(JulayParser.STRING, 0); }
		public ValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_value; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitValue(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitValue(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ValueContext value() throws RecognitionException {
		ValueContext _localctx = new ValueContext(_ctx, getState());
		enterRule(_localctx, 68, RULE_value);
		try {
			setState(453);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,32,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(444);
				list_literal();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(445);
				index_expr(0);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(446);
				oclass_literal();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(447);
				fun_call();
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(448);
				field_access();
				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(449);
				match(INT);
				}
				break;
			case 7:
				enterOuterAlt(_localctx, 7);
				{
				setState(450);
				match(TRUE);
				}
				break;
			case 8:
				enterOuterAlt(_localctx, 8);
				{
				setState(451);
				match(FALSE);
				}
				break;
			case 9:
				enterOuterAlt(_localctx, 9);
				{
				setState(452);
				match(STRING);
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
	public static class List_literalContext extends ParserRuleContext {
		public TerminalNode LBRACK() { return getToken(JulayParser.LBRACK, 0); }
		public TerminalNode RBRACK() { return getToken(JulayParser.RBRACK, 0); }
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
		enterRule(_localctx, 70, RULE_list_literal);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(455);
			match(LBRACK);
			setState(464);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 31525225308985424L) != 0)) {
				{
				setState(456);
				expr(0);
				setState(461);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(457);
					match(COMMA);
					setState(458);
					expr(0);
					}
					}
					setState(463);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
			}

			setState(466);
			match(RBRACK);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
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
		public List_literalContext list_literal() {
			return getRuleContext(List_literalContext.class,0);
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
		int _startState = 72;
		enterRecursionRule(_localctx, 72, RULE_index_expr, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			{
			setState(476);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,35,_ctx) ) {
			case 1:
				{
				setState(469);
				fun_call();
				}
				break;
			case 2:
				{
				setState(470);
				field_access();
				}
				break;
			case 3:
				{
				setState(471);
				list_literal();
				}
				break;
			case 4:
				{
				setState(472);
				match(LPAREN);
				setState(473);
				expr(0);
				setState(474);
				match(RPAREN);
				}
				break;
			}
			setState(478);
			match(LBRACK);
			setState(479);
			index_or_slice();
			setState(480);
			match(RBRACK);
			}
			_ctx.stop = _input.LT(-1);
			setState(489);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,36,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					{
					_localctx = new Index_exprContext(_parentctx, _parentState);
					pushNewRecursionContext(_localctx, _startState, RULE_index_expr);
					setState(482);
					if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
					setState(483);
					match(LBRACK);
					setState(484);
					index_or_slice();
					setState(485);
					match(RBRACK);
					}
					} 
				}
				setState(491);
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
		enterRule(_localctx, 74, RULE_index_or_slice);
		try {
			setState(497);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,37,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(492);
				expr(0);
				setState(493);
				match(COLON);
				setState(494);
				expr(0);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(496);
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
		enterRule(_localctx, 76, RULE_fun_call);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(499);
			match(ID);
			setState(500);
			match(LPAREN);
			setState(509);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 31525225308985424L) != 0)) {
				{
				setState(501);
				expr(0);
				setState(506);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(502);
					match(COMMA);
					setState(503);
					expr(0);
					}
					}
					setState(508);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
			}

			setState(511);
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
		public List<Struct_field_assignContext> struct_field_assign() {
			return getRuleContexts(Struct_field_assignContext.class);
		}
		public Struct_field_assignContext struct_field_assign(int i) {
			return getRuleContext(Struct_field_assignContext.class,i);
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
		enterRule(_localctx, 78, RULE_oclass_literal);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(513);
			typeExpr();
			setState(514);
			match(LCURLY);
			setState(515);
			struct_field_assign();
			setState(520);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(516);
				match(COMMA);
				setState(517);
				struct_field_assign();
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
	public static class Struct_field_assignContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(JulayParser.ID, 0); }
		public TerminalNode ASGN_EQ() { return getToken(JulayParser.ASGN_EQ, 0); }
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public Struct_field_assignContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_struct_field_assign; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterStruct_field_assign(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitStruct_field_assign(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitStruct_field_assign(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Struct_field_assignContext struct_field_assign() throws RecognitionException {
		Struct_field_assignContext _localctx = new Struct_field_assignContext(_ctx, getState());
		enterRule(_localctx, 80, RULE_struct_field_assign);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(525);
			match(ID);
			setState(526);
			match(ASGN_EQ);
			setState(527);
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
		enterRule(_localctx, 82, RULE_field_access);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(529);
			match(ID);
			setState(534);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,41,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(530);
					match(DOT);
					setState(531);
					match(ID);
					}
					} 
				}
				setState(536);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,41,_ctx);
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
		case 28:
			return expr_sempred((ExprContext)_localctx, predIndex);
		case 33:
			return proc_expr_sempred((Proc_exprContext)_localctx, predIndex);
		case 36:
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
		"\u0004\u00019\u021a\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001\u0002"+
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
		"(\u0007(\u0002)\u0007)\u0001\u0000\u0001\u0000\u0005\u0000W\b\u0000\n"+
		"\u0000\f\u0000Z\t\u0000\u0001\u0000\u0001\u0000\u0001\u0001\u0001\u0001"+
		"\u0001\u0001\u0001\u0002\u0001\u0002\u0001\u0002\u0004\u0002d\b\u0002"+
		"\u000b\u0002\f\u0002e\u0001\u0003\u0001\u0003\u0001\u0004\u0001\u0004"+
		"\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0003\u0004p\b\u0004"+
		"\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005"+
		"\u0001\u0005\u0003\u0005y\b\u0005\u0001\u0006\u0001\u0006\u0001\u0006"+
		"\u0001\u0006\u0005\u0006\u007f\b\u0006\n\u0006\f\u0006\u0082\t\u0006\u0001"+
		"\u0006\u0001\u0006\u0001\u0007\u0001\u0007\u0001\u0007\u0003\u0007\u0089"+
		"\b\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001"+
		"\u0007\u0001\b\u0001\b\u0001\b\u0001\b\u0005\b\u0095\b\b\n\b\f\b\u0098"+
		"\t\b\u0001\b\u0001\b\u0001\t\u0001\t\u0001\t\u0003\t\u009f\b\t\u0001\t"+
		"\u0001\t\u0005\t\u00a3\b\t\n\t\f\t\u00a6\t\t\u0001\t\u0001\t\u0001\n\u0001"+
		"\n\u0001\n\u0001\n\u0001\n\u0001\u000b\u0001\u000b\u0001\u000b\u0001\u000b"+
		"\u0001\u000b\u0001\f\u0001\f\u0001\f\u0001\f\u0001\f\u0001\r\u0001\r\u0001"+
		"\r\u0003\r\u00bc\b\r\u0001\u000e\u0001\u000e\u0001\u000e\u0001\u000e\u0001"+
		"\u000f\u0001\u000f\u0001\u000f\u0001\u000f\u0001\u000f\u0001\u0010\u0001"+
		"\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0005\u0010\u00cc\b\u0010\n"+
		"\u0010\f\u0010\u00cf\t\u0010\u0001\u0010\u0001\u0010\u0001\u0011\u0003"+
		"\u0011\u00d4\b\u0011\u0001\u0011\u0001\u0011\u0001\u0011\u0001\u0011\u0001"+
		"\u0011\u0005\u0011\u00db\b\u0011\n\u0011\f\u0011\u00de\t\u0011\u0001\u0011"+
		"\u0001\u0011\u0001\u0012\u0001\u0012\u0003\u0012\u00e4\b\u0012\u0001\u0012"+
		"\u0001\u0012\u0005\u0012\u00e8\b\u0012\n\u0012\f\u0012\u00eb\t\u0012\u0001"+
		"\u0012\u0001\u0012\u0001\u0013\u0001\u0013\u0001\u0013\u0001\u0013\u0001"+
		"\u0014\u0001\u0014\u0001\u0014\u0001\u0014\u0003\u0014\u00f7\b\u0014\u0001"+
		"\u0015\u0001\u0015\u0001\u0015\u0001\u0015\u0001\u0016\u0001\u0016\u0001"+
		"\u0016\u0005\u0016\u0100\b\u0016\n\u0016\f\u0016\u0103\t\u0016\u0001\u0017"+
		"\u0001\u0017\u0001\u0017\u0001\u0017\u0001\u0018\u0001\u0018\u0001\u0018"+
		"\u0001\u0018\u0001\u0019\u0001\u0019\u0001\u0019\u0004\u0019\u0110\b\u0019"+
		"\u000b\u0019\f\u0019\u0111\u0001\u001a\u0001\u001a\u0001\u001a\u0001\u001a"+
		"\u0001\u001a\u0003\u001a\u0119\b\u001a\u0001\u001b\u0001\u001b\u0001\u001b"+
		"\u0001\u001b\u0001\u001b\u0005\u001b\u0120\b\u001b\n\u001b\f\u001b\u0123"+
		"\t\u001b\u0003\u001b\u0125\b\u001b\u0001\u001b\u0001\u001b\u0001\u001c"+
		"\u0001\u001c\u0001\u001c\u0001\u001c\u0001\u001c\u0001\u001c\u0001\u001c"+
		"\u0001\u001c\u0001\u001c\u0001\u001c\u0001\u001c\u0001\u001c\u0001\u001c"+
		"\u0001\u001c\u0001\u001c\u0001\u001c\u0001\u001c\u0001\u001c\u0001\u001c"+
		"\u0001\u001c\u0001\u001c\u0001\u001c\u0001\u001c\u0001\u001c\u0001\u001c"+
		"\u0001\u001c\u0001\u001c\u0001\u001c\u0001\u001c\u0001\u001c\u0001\u001c"+
		"\u0001\u001c\u0001\u001c\u0001\u001c\u0001\u001c\u0001\u001c\u0001\u001c"+
		"\u0001\u001c\u0001\u001c\u0001\u001c\u0004\u001c\u0151\b\u001c\u000b\u001c"+
		"\f\u001c\u0152\u0001\u001c\u0001\u001c\u0001\u001c\u0001\u001c\u0001\u001c"+
		"\u0004\u001c\u015a\b\u001c\u000b\u001c\f\u001c\u015b\u0001\u001c\u0001"+
		"\u001c\u0003\u001c\u0160\b\u001c\u0001\u001c\u0001\u001c\u0001\u001c\u0001"+
		"\u001c\u0001\u001c\u0001\u001c\u0001\u001c\u0001\u001c\u0001\u001c\u0001"+
		"\u001c\u0001\u001c\u0001\u001c\u0001\u001c\u0001\u001c\u0001\u001c\u0001"+
		"\u001c\u0001\u001c\u0001\u001c\u0001\u001c\u0001\u001c\u0001\u001c\u0001"+
		"\u001c\u0001\u001c\u0001\u001c\u0001\u001c\u0001\u001c\u0001\u001c\u0001"+
		"\u001c\u0001\u001c\u0001\u001c\u0001\u001c\u0001\u001c\u0001\u001c\u0001"+
		"\u001c\u0001\u001c\u0001\u001c\u0001\u001c\u0001\u001c\u0001\u001c\u0001"+
		"\u001c\u0001\u001c\u0001\u001c\u0001\u001c\u0001\u001c\u0001\u001c\u0005"+
		"\u001c\u018f\b\u001c\n\u001c\f\u001c\u0192\t\u001c\u0001\u001d\u0001\u001d"+
		"\u0001\u001d\u0001\u001d\u0001\u001d\u0001\u001d\u0001\u001d\u0003\u001d"+
		"\u019b\b\u001d\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e"+
		"\u0001\u001e\u0001\u001e\u0003\u001e\u01a4\b\u001e\u0001\u001f\u0001\u001f"+
		"\u0003\u001f\u01a8\b\u001f\u0001 \u0001 \u0001!\u0001!\u0001!\u0001!\u0001"+
		"!\u0001!\u0001!\u0003!\u01b3\b!\u0001!\u0001!\u0001!\u0005!\u01b8\b!\n"+
		"!\f!\u01bb\t!\u0001\"\u0001\"\u0001\"\u0001\"\u0001\"\u0001\"\u0001\""+
		"\u0001\"\u0001\"\u0003\"\u01c6\b\"\u0001#\u0001#\u0001#\u0001#\u0005#"+
		"\u01cc\b#\n#\f#\u01cf\t#\u0003#\u01d1\b#\u0001#\u0001#\u0001$\u0001$\u0001"+
		"$\u0001$\u0001$\u0001$\u0001$\u0001$\u0003$\u01dd\b$\u0001$\u0001$\u0001"+
		"$\u0001$\u0001$\u0001$\u0001$\u0001$\u0001$\u0005$\u01e8\b$\n$\f$\u01eb"+
		"\t$\u0001%\u0001%\u0001%\u0001%\u0001%\u0003%\u01f2\b%\u0001&\u0001&\u0001"+
		"&\u0001&\u0001&\u0005&\u01f9\b&\n&\f&\u01fc\t&\u0003&\u01fe\b&\u0001&"+
		"\u0001&\u0001\'\u0001\'\u0001\'\u0001\'\u0001\'\u0005\'\u0207\b\'\n\'"+
		"\f\'\u020a\t\'\u0001\'\u0001\'\u0001(\u0001(\u0001(\u0001(\u0001)\u0001"+
		")\u0001)\u0005)\u0215\b)\n)\f)\u0218\t)\u0001)\u0000\u00038BH*\u0000\u0002"+
		"\u0004\u0006\b\n\f\u000e\u0010\u0012\u0014\u0016\u0018\u001a\u001c\u001e"+
		" \"$&(*,.02468:<>@BDFHJLNPR\u0000\u0003\u0002\u00003355\u0001\u0000-."+
		"\u0003\u0000\u000b\f4466\u023e\u0000X\u0001\u0000\u0000\u0000\u0002]\u0001"+
		"\u0000\u0000\u0000\u0004`\u0001\u0000\u0000\u0000\u0006g\u0001\u0000\u0000"+
		"\u0000\bo\u0001\u0000\u0000\u0000\nx\u0001\u0000\u0000\u0000\fz\u0001"+
		"\u0000\u0000\u0000\u000e\u0085\u0001\u0000\u0000\u0000\u0010\u0090\u0001"+
		"\u0000\u0000\u0000\u0012\u009b\u0001\u0000\u0000\u0000\u0014\u00a9\u0001"+
		"\u0000\u0000\u0000\u0016\u00ae\u0001\u0000\u0000\u0000\u0018\u00b3\u0001"+
		"\u0000\u0000\u0000\u001a\u00bb\u0001\u0000\u0000\u0000\u001c\u00bd\u0001"+
		"\u0000\u0000\u0000\u001e\u00c1\u0001\u0000\u0000\u0000 \u00c6\u0001\u0000"+
		"\u0000\u0000\"\u00d3\u0001\u0000\u0000\u0000$\u00e1\u0001\u0000\u0000"+
		"\u0000&\u00ee\u0001\u0000\u0000\u0000(\u00f6\u0001\u0000\u0000\u0000*"+
		"\u00f8\u0001\u0000\u0000\u0000,\u00fc\u0001\u0000\u0000\u0000.\u0104\u0001"+
		"\u0000\u0000\u00000\u0108\u0001\u0000\u0000\u00002\u010c\u0001\u0000\u0000"+
		"\u00004\u0118\u0001\u0000\u0000\u00006\u011a\u0001\u0000\u0000\u00008"+
		"\u015f\u0001\u0000\u0000\u0000:\u019a\u0001\u0000\u0000\u0000<\u01a3\u0001"+
		"\u0000\u0000\u0000>\u01a7\u0001\u0000\u0000\u0000@\u01a9\u0001\u0000\u0000"+
		"\u0000B\u01b2\u0001\u0000\u0000\u0000D\u01c5\u0001\u0000\u0000\u0000F"+
		"\u01c7\u0001\u0000\u0000\u0000H\u01d4\u0001\u0000\u0000\u0000J\u01f1\u0001"+
		"\u0000\u0000\u0000L\u01f3\u0001\u0000\u0000\u0000N\u0201\u0001\u0000\u0000"+
		"\u0000P\u020d\u0001\u0000\u0000\u0000R\u0211\u0001\u0000\u0000\u0000T"+
		"W\u0003\u0002\u0001\u0000UW\u0003\b\u0004\u0000VT\u0001\u0000\u0000\u0000"+
		"VU\u0001\u0000\u0000\u0000WZ\u0001\u0000\u0000\u0000XV\u0001\u0000\u0000"+
		"\u0000XY\u0001\u0000\u0000\u0000Y[\u0001\u0000\u0000\u0000ZX\u0001\u0000"+
		"\u0000\u0000[\\\u0005\u0000\u0000\u0001\\\u0001\u0001\u0000\u0000\u0000"+
		"]^\u0005$\u0000\u0000^_\u0003\u0004\u0002\u0000_\u0003\u0001\u0000\u0000"+
		"\u0000`c\u00055\u0000\u0000ab\u0005\u0002\u0000\u0000bd\u0003\u0006\u0003"+
		"\u0000ca\u0001\u0000\u0000\u0000de\u0001\u0000\u0000\u0000ec\u0001\u0000"+
		"\u0000\u0000ef\u0001\u0000\u0000\u0000f\u0005\u0001\u0000\u0000\u0000"+
		"gh\u0007\u0000\u0000\u0000h\u0007\u0001\u0000\u0000\u0000ip\u0003\u0010"+
		"\b\u0000jp\u0003\u0012\t\u0000kp\u0003\u0014\n\u0000lp\u0003\u0016\u000b"+
		"\u0000mp\u0003\u0018\f\u0000np\u0003\u000e\u0007\u0000oi\u0001\u0000\u0000"+
		"\u0000oj\u0001\u0000\u0000\u0000ok\u0001\u0000\u0000\u0000ol\u0001\u0000"+
		"\u0000\u0000om\u0001\u0000\u0000\u0000on\u0001\u0000\u0000\u0000p\t\u0001"+
		"\u0000\u0000\u0000qy\u00055\u0000\u0000rs\u00055\u0000\u0000sy\u0003\n"+
		"\u0005\u0000tu\u0005\u0004\u0000\u0000uv\u0003\n\u0005\u0000vw\u0005\u0005"+
		"\u0000\u0000wy\u0001\u0000\u0000\u0000xq\u0001\u0000\u0000\u0000xr\u0001"+
		"\u0000\u0000\u0000xt\u0001\u0000\u0000\u0000y\u000b\u0001\u0000\u0000"+
		"\u0000z{\u0005\u0016\u0000\u0000{\u0080\u00055\u0000\u0000|}\u0005\u0001"+
		"\u0000\u0000}\u007f\u00055\u0000\u0000~|\u0001\u0000\u0000\u0000\u007f"+
		"\u0082\u0001\u0000\u0000\u0000\u0080~\u0001\u0000\u0000\u0000\u0080\u0081"+
		"\u0001\u0000\u0000\u0000\u0081\u0083\u0001\u0000\u0000\u0000\u0082\u0080"+
		"\u0001\u0000\u0000\u0000\u0083\u0084\u0005\u0018\u0000\u0000\u0084\r\u0001"+
		"\u0000\u0000\u0000\u0085\u0086\u00053\u0000\u0000\u0086\u0088\u00055\u0000"+
		"\u0000\u0087\u0089\u0003\f\u0006\u0000\u0088\u0087\u0001\u0000\u0000\u0000"+
		"\u0088\u0089\u0001\u0000\u0000\u0000\u0089\u008a\u0001\u0000\u0000\u0000"+
		"\u008a\u008b\u0003$\u0012\u0000\u008b\u008c\u0005\u0003\u0000\u0000\u008c"+
		"\u008d\u0003\n\u0005\u0000\u008d\u008e\u0005\u001a\u0000\u0000\u008e\u008f"+
		"\u00038\u001c\u0000\u008f\u000f\u0001\u0000\u0000\u0000\u0090\u0091\u0005"+
		"%\u0000\u0000\u0091\u0092\u00055\u0000\u0000\u0092\u0096\u0005\b\u0000"+
		"\u0000\u0093\u0095\u0003\u001a\r\u0000\u0094\u0093\u0001\u0000\u0000\u0000"+
		"\u0095\u0098\u0001\u0000\u0000\u0000\u0096\u0094\u0001\u0000\u0000\u0000"+
		"\u0096\u0097\u0001\u0000\u0000\u0000\u0097\u0099\u0001\u0000\u0000\u0000"+
		"\u0098\u0096\u0001\u0000\u0000\u0000\u0099\u009a\u0005\t\u0000\u0000\u009a"+
		"\u0011\u0001\u0000\u0000\u0000\u009b\u009c\u0005&\u0000\u0000\u009c\u009e"+
		"\u00055\u0000\u0000\u009d\u009f\u0003\f\u0006\u0000\u009e\u009d\u0001"+
		"\u0000\u0000\u0000\u009e\u009f\u0001\u0000\u0000\u0000\u009f\u00a0\u0001"+
		"\u0000\u0000\u0000\u00a0\u00a4\u0005\b\u0000\u0000\u00a1\u00a3\u0003\u001c"+
		"\u000e\u0000\u00a2\u00a1\u0001\u0000\u0000\u0000\u00a3\u00a6\u0001\u0000"+
		"\u0000\u0000\u00a4\u00a2\u0001\u0000\u0000\u0000\u00a4\u00a5\u0001\u0000"+
		"\u0000\u0000\u00a5\u00a7\u0001\u0000\u0000\u0000\u00a6\u00a4\u0001\u0000"+
		"\u0000\u0000\u00a7\u00a8\u0005\t\u0000\u0000\u00a8\u0013\u0001\u0000\u0000"+
		"\u0000\u00a9\u00aa\u0005\'\u0000\u0000\u00aa\u00ab\u00055\u0000\u0000"+
		"\u00ab\u00ac\u0005\u001d\u0000\u0000\u00ac\u00ad\u0003B!\u0000\u00ad\u0015"+
		"\u0001\u0000\u0000\u0000\u00ae\u00af\u0005(\u0000\u0000\u00af\u00b0\u0005"+
		"5\u0000\u0000\u00b0\u00b1\u0005\u001d\u0000\u0000\u00b1\u00b2\u0003B!"+
		"\u0000\u00b2\u0017\u0001\u0000\u0000\u0000\u00b3\u00b4\u0005)\u0000\u0000"+
		"\u00b4\u00b5\u00055\u0000\u0000\u00b5\u00b6\u0005\u001d\u0000\u0000\u00b6"+
		"\u00b7\u0003B!\u0000\u00b7\u0019\u0001\u0000\u0000\u0000\u00b8\u00bc\u0003"+
		"\u001e\u000f\u0000\u00b9\u00bc\u0003 \u0010\u0000\u00ba\u00bc\u0003\""+
		"\u0011\u0000\u00bb\u00b8\u0001\u0000\u0000\u0000\u00bb\u00b9\u0001\u0000"+
		"\u0000\u0000\u00bb\u00ba\u0001\u0000\u0000\u0000\u00bc\u001b\u0001\u0000"+
		"\u0000\u0000\u00bd\u00be\u00055\u0000\u0000\u00be\u00bf\u0005\u0003\u0000"+
		"\u0000\u00bf\u00c0\u0003\n\u0005\u0000\u00c0\u001d\u0001\u0000\u0000\u0000"+
		"\u00c1\u00c2\u0005*\u0000\u0000\u00c2\u00c3\u00055\u0000\u0000\u00c3\u00c4"+
		"\u0005\u0003\u0000\u0000\u00c4\u00c5\u0003\n\u0005\u0000\u00c5\u001f\u0001"+
		"\u0000\u0000\u0000\u00c6\u00c7\u0005+\u0000\u0000\u00c7\u00c8\u00055\u0000"+
		"\u0000\u00c8\u00c9\u0003$\u0012\u0000\u00c9\u00cd\u0005\b\u0000\u0000"+
		"\u00ca\u00cc\u0003(\u0014\u0000\u00cb\u00ca\u0001\u0000\u0000\u0000\u00cc"+
		"\u00cf\u0001\u0000\u0000\u0000\u00cd\u00cb\u0001\u0000\u0000\u0000\u00cd"+
		"\u00ce\u0001\u0000\u0000\u0000\u00ce\u00d0\u0001\u0000\u0000\u0000\u00cf"+
		"\u00cd\u0001\u0000\u0000\u0000\u00d0\u00d1\u0005\t\u0000\u0000\u00d1!"+
		"\u0001\u0000\u0000\u0000\u00d2\u00d4\u0007\u0001\u0000\u0000\u00d3\u00d2"+
		"\u0001\u0000\u0000\u0000\u00d3\u00d4\u0001\u0000\u0000\u0000\u00d4\u00d5"+
		"\u0001\u0000\u0000\u0000\u00d5\u00d6\u0005,\u0000\u0000\u00d6\u00d7\u0005"+
		"5\u0000\u0000\u00d7\u00d8\u0003$\u0012\u0000\u00d8\u00dc\u0005\b\u0000"+
		"\u0000\u00d9\u00db\u0003(\u0014\u0000\u00da\u00d9\u0001\u0000\u0000\u0000"+
		"\u00db\u00de\u0001\u0000\u0000\u0000\u00dc\u00da\u0001\u0000\u0000\u0000"+
		"\u00dc\u00dd\u0001\u0000\u0000\u0000\u00dd\u00df\u0001\u0000\u0000\u0000"+
		"\u00de\u00dc\u0001\u0000\u0000\u0000\u00df\u00e0\u0005\t\u0000\u0000\u00e0"+
		"#\u0001\u0000\u0000\u0000\u00e1\u00e3\u0005\u0004\u0000\u0000\u00e2\u00e4"+
		"\u0003&\u0013\u0000\u00e3\u00e2\u0001\u0000\u0000\u0000\u00e3\u00e4\u0001"+
		"\u0000\u0000\u0000\u00e4\u00e9\u0001\u0000\u0000\u0000\u00e5\u00e6\u0005"+
		"\u0001\u0000\u0000\u00e6\u00e8\u0003&\u0013\u0000\u00e7\u00e5\u0001\u0000"+
		"\u0000\u0000\u00e8\u00eb\u0001\u0000\u0000\u0000\u00e9\u00e7\u0001\u0000"+
		"\u0000\u0000\u00e9\u00ea\u0001\u0000\u0000\u0000\u00ea\u00ec\u0001\u0000"+
		"\u0000\u0000\u00eb\u00e9\u0001\u0000\u0000\u0000\u00ec\u00ed\u0005\u0005"+
		"\u0000\u0000\u00ed%\u0001\u0000\u0000\u0000\u00ee\u00ef\u00055\u0000\u0000"+
		"\u00ef\u00f0\u0005\u0003\u0000\u0000\u00f0\u00f1\u0003\n\u0005\u0000\u00f1"+
		"\'\u0001\u0000\u0000\u0000\u00f2\u00f7\u0003*\u0015\u0000\u00f3\u00f7"+
		"\u0003,\u0016\u0000\u00f4\u00f7\u0003.\u0017\u0000\u00f5\u00f7\u00032"+
		"\u0019\u0000\u00f6\u00f2\u0001\u0000\u0000\u0000\u00f6\u00f3\u0001\u0000"+
		"\u0000\u0000\u00f6\u00f4\u0001\u0000\u0000\u0000\u00f6\u00f5\u0001\u0000"+
		"\u0000\u0000\u00f7)\u0001\u0000\u0000\u0000\u00f8\u00f9\u0005/\u0000\u0000"+
		"\u00f9\u00fa\u0005\u0003\u0000\u0000\u00fa\u00fb\u00038\u001c\u0000\u00fb"+
		"+\u0001\u0000\u0000\u0000\u00fc\u00fd\u00050\u0000\u0000\u00fd\u0101\u0005"+
		"\u0003\u0000\u0000\u00fe\u0100\u00030\u0018\u0000\u00ff\u00fe\u0001\u0000"+
		"\u0000\u0000\u0100\u0103\u0001\u0000\u0000\u0000\u0101\u00ff\u0001\u0000"+
		"\u0000\u0000\u0101\u0102\u0001\u0000\u0000\u0000\u0102-\u0001\u0000\u0000"+
		"\u0000\u0103\u0101\u0001\u0000\u0000\u0000\u0104\u0105\u00051\u0000\u0000"+
		"\u0105\u0106\u0005\u0003\u0000\u0000\u0106\u0107\u00038\u001c\u0000\u0107"+
		"/\u0001\u0000\u0000\u0000\u0108\u0109\u0003R)\u0000\u0109\u010a\u0005"+
		"\u001d\u0000\u0000\u010a\u010b\u00038\u001c\u0000\u010b1\u0001\u0000\u0000"+
		"\u0000\u010c\u010d\u00052\u0000\u0000\u010d\u010f\u0005\u0003\u0000\u0000"+
		"\u010e\u0110\u00034\u001a\u0000\u010f\u010e\u0001\u0000\u0000\u0000\u0110"+
		"\u0111\u0001\u0000\u0000\u0000\u0111\u010f\u0001\u0000\u0000\u0000\u0111"+
		"\u0112\u0001\u0000\u0000\u0000\u01123\u0001\u0000\u0000\u0000\u0113\u0119"+
		"\u00036\u001b\u0000\u0114\u0115\u0003R)\u0000\u0115\u0116\u0005\u001d"+
		"\u0000\u0000\u0116\u0117\u00036\u001b\u0000\u0117\u0119\u0001\u0000\u0000"+
		"\u0000\u0118\u0113\u0001\u0000\u0000\u0000\u0118\u0114\u0001\u0000\u0000"+
		"\u0000\u01195\u0001\u0000\u0000\u0000\u011a\u011b\u00055\u0000\u0000\u011b"+
		"\u0124\u0005\u0004\u0000\u0000\u011c\u0121\u00038\u001c\u0000\u011d\u011e"+
		"\u0005\u0001\u0000\u0000\u011e\u0120\u00038\u001c\u0000\u011f\u011d\u0001"+
		"\u0000\u0000\u0000\u0120\u0123\u0001\u0000\u0000\u0000\u0121\u011f\u0001"+
		"\u0000\u0000\u0000\u0121\u0122\u0001\u0000\u0000\u0000\u0122\u0125\u0001"+
		"\u0000\u0000\u0000\u0123\u0121\u0001\u0000\u0000\u0000\u0124\u011c\u0001"+
		"\u0000\u0000\u0000\u0124\u0125\u0001\u0000\u0000\u0000\u0125\u0126\u0001"+
		"\u0000\u0000\u0000\u0126\u0127\u0005\u0005\u0000\u0000\u01277\u0001\u0000"+
		"\u0000\u0000\u0128\u0129\u0006\u001c\uffff\uffff\u0000\u0129\u0160\u0003"+
		"D\"\u0000\u012a\u012b\u0005\u0004\u0000\u0000\u012b\u012c\u00038\u001c"+
		"\u0000\u012c\u012d\u0005\u0005\u0000\u0000\u012d\u0160\u0001\u0000\u0000"+
		"\u0000\u012e\u012f\u0005\u000f\u0000\u0000\u012f\u0160\u00038\u001c\u0015"+
		"\u0130\u0131\u0005\u0010\u0000\u0000\u0131\u0160\u00038\u001c\u0014\u0132"+
		"\u0133\u0005\u001f\u0000\u0000\u0133\u0134\u0005\u0004\u0000\u0000\u0134"+
		"\u0135\u00038\u001c\u0000\u0135\u0136\u0005\u0005\u0000\u0000\u0136\u0137"+
		"\u0005\b\u0000\u0000\u0137\u0138\u00038\u001c\u0000\u0138\u0139\u0005"+
		"\t\u0000\u0000\u0139\u013a\u0005 \u0000\u0000\u013a\u013b\u0005\b\u0000"+
		"\u0000\u013b\u013c\u00038\u001c\u0000\u013c\u013d\u0005\t\u0000\u0000"+
		"\u013d\u0160\u0001\u0000\u0000\u0000\u013e\u013f\u0005!\u0000\u0000\u013f"+
		"\u0140\u0005\u0004\u0000\u0000\u0140\u0141\u00055\u0000\u0000\u0141\u0142"+
		"\u0005\u0003\u0000\u0000\u0142\u0143\u0003\n\u0005\u0000\u0143\u0144\u0005"+
		"\u001d\u0000\u0000\u0144\u0145\u00038\u001c\u0000\u0145\u0146\u0005\u0005"+
		"\u0000\u0000\u0146\u0147\u0005\b\u0000\u0000\u0147\u0148\u00038\u001c"+
		"\u0000\u0148\u0149\u0005\t\u0000\u0000\u0149\u0160\u0001\u0000\u0000\u0000"+
		"\u014a\u014b\u0005\"\u0000\u0000\u014b\u014c\u0005\u0004\u0000\u0000\u014c"+
		"\u014d\u00038\u001c\u0000\u014d\u014e\u0005\u0005\u0000\u0000\u014e\u0150"+
		"\u0005\b\u0000\u0000\u014f\u0151\u0003:\u001d\u0000\u0150\u014f\u0001"+
		"\u0000\u0000\u0000\u0151\u0152\u0001\u0000\u0000\u0000\u0152\u0150\u0001"+
		"\u0000\u0000\u0000\u0152\u0153\u0001\u0000\u0000\u0000\u0153\u0154\u0001"+
		"\u0000\u0000\u0000\u0154\u0155\u0005\t\u0000\u0000\u0155\u0160\u0001\u0000"+
		"\u0000\u0000\u0156\u0157\u0005\"\u0000\u0000\u0157\u0159\u0005\b\u0000"+
		"\u0000\u0158\u015a\u0003<\u001e\u0000\u0159\u0158\u0001\u0000\u0000\u0000"+
		"\u015a\u015b\u0001\u0000\u0000\u0000\u015b\u0159\u0001\u0000\u0000\u0000"+
		"\u015b\u015c\u0001\u0000\u0000\u0000\u015c\u015d\u0001\u0000\u0000\u0000"+
		"\u015d\u015e\u0005\t\u0000\u0000\u015e\u0160\u0001\u0000\u0000\u0000\u015f"+
		"\u0128\u0001\u0000\u0000\u0000\u015f\u012a\u0001\u0000\u0000\u0000\u015f"+
		"\u012e\u0001\u0000\u0000\u0000\u015f\u0130\u0001\u0000\u0000\u0000\u015f"+
		"\u0132\u0001\u0000\u0000\u0000\u015f\u013e\u0001\u0000\u0000\u0000\u015f"+
		"\u014a\u0001\u0000\u0000\u0000\u015f\u0156\u0001\u0000\u0000\u0000\u0160"+
		"\u0190\u0001\u0000\u0000\u0000\u0161\u0162\n\u0013\u0000\u0000\u0162\u0163"+
		"\u0005\u0011\u0000\u0000\u0163\u018f\u00038\u001c\u0014\u0164\u0165\n"+
		"\u0012\u0000\u0000\u0165\u0166\u0005\u0012\u0000\u0000\u0166\u018f\u0003"+
		"8\u001c\u0013\u0167\u0168\n\u0011\u0000\u0000\u0168\u0169\u0005\u0013"+
		"\u0000\u0000\u0169\u018f\u00038\u001c\u0012\u016a\u016b\n\u0010\u0000"+
		"\u0000\u016b\u016c\u0005\u0014\u0000\u0000\u016c\u018f\u00038\u001c\u0011"+
		"\u016d\u016e\n\u000f\u0000\u0000\u016e\u016f\u0005\u0015\u0000\u0000\u016f"+
		"\u018f\u00038\u001c\u0010\u0170\u0171\n\u000e\u0000\u0000\u0171\u0172"+
		"\u0005\u0016\u0000\u0000\u0172\u018f\u00038\u001c\u000f\u0173\u0174\n"+
		"\r\u0000\u0000\u0174\u0175\u0005\u0017\u0000\u0000\u0175\u018f\u00038"+
		"\u001c\u000e\u0176\u0177\n\f\u0000\u0000\u0177\u0178\u0005\u0018\u0000"+
		"\u0000\u0178\u018f\u00038\u001c\r\u0179\u017a\n\u000b\u0000\u0000\u017a"+
		"\u017b\u0005\u0019\u0000\u0000\u017b\u018f\u00038\u001c\f\u017c\u017d"+
		"\n\n\u0000\u0000\u017d\u017e\u0005\u001a\u0000\u0000\u017e\u018f\u0003"+
		"8\u001c\u000b\u017f\u0180\n\t\u0000\u0000\u0180\u0181\u0005\u001c\u0000"+
		"\u0000\u0181\u018f\u00038\u001c\n\u0182\u0183\n\b\u0000\u0000\u0183\u0184"+
		"\u0005\u001b\u0000\u0000\u0184\u018f\u00038\u001c\t\u0185\u0186\n\u0007"+
		"\u0000\u0000\u0186\u0187\u0005\r\u0000\u0000\u0187\u018f\u00038\u001c"+
		"\b\u0188\u0189\n\u0006\u0000\u0000\u0189\u018a\u0005\u000e\u0000\u0000"+
		"\u018a\u018f\u00038\u001c\u0007\u018b\u018c\n\u0005\u0000\u0000\u018c"+
		"\u018d\u0005\u001e\u0000\u0000\u018d\u018f\u00038\u001c\u0006\u018e\u0161"+
		"\u0001\u0000\u0000\u0000\u018e\u0164\u0001\u0000\u0000\u0000\u018e\u0167"+
		"\u0001\u0000\u0000\u0000\u018e\u016a\u0001\u0000\u0000\u0000\u018e\u016d"+
		"\u0001\u0000\u0000\u0000\u018e\u0170\u0001\u0000\u0000\u0000\u018e\u0173"+
		"\u0001\u0000\u0000\u0000\u018e\u0176\u0001\u0000\u0000\u0000\u018e\u0179"+
		"\u0001\u0000\u0000\u0000\u018e\u017c\u0001\u0000\u0000\u0000\u018e\u017f"+
		"\u0001\u0000\u0000\u0000\u018e\u0182\u0001\u0000\u0000\u0000\u018e\u0185"+
		"\u0001\u0000\u0000\u0000\u018e\u0188\u0001\u0000\u0000\u0000\u018e\u018b"+
		"\u0001\u0000\u0000\u0000\u018f\u0192\u0001\u0000\u0000\u0000\u0190\u018e"+
		"\u0001\u0000\u0000\u0000\u0190\u0191\u0001\u0000\u0000\u0000\u01919\u0001"+
		"\u0000\u0000\u0000\u0192\u0190\u0001\u0000\u0000\u0000\u0193\u0194\u0003"+
		">\u001f\u0000\u0194\u0195\u0005#\u0000\u0000\u0195\u0196\u00038\u001c"+
		"\u0000\u0196\u019b\u0001\u0000\u0000\u0000\u0197\u0198\u0005 \u0000\u0000"+
		"\u0198\u0199\u0005#\u0000\u0000\u0199\u019b\u00038\u001c\u0000\u019a\u0193"+
		"\u0001\u0000\u0000\u0000\u019a\u0197\u0001\u0000\u0000\u0000\u019b;\u0001"+
		"\u0000\u0000\u0000\u019c\u019d\u00038\u001c\u0000\u019d\u019e\u0005#\u0000"+
		"\u0000\u019e\u019f\u00038\u001c\u0000\u019f\u01a4\u0001\u0000\u0000\u0000"+
		"\u01a0\u01a1\u0005 \u0000\u0000\u01a1\u01a2\u0005#\u0000\u0000\u01a2\u01a4"+
		"\u00038\u001c\u0000\u01a3\u019c\u0001\u0000\u0000\u0000\u01a3\u01a0\u0001"+
		"\u0000\u0000\u0000\u01a4=\u0001\u0000\u0000\u0000\u01a5\u01a8\u0003@ "+
		"\u0000\u01a6\u01a8\u0003N\'\u0000\u01a7\u01a5\u0001\u0000\u0000\u0000"+
		"\u01a7\u01a6\u0001\u0000\u0000\u0000\u01a8?\u0001\u0000\u0000\u0000\u01a9"+
		"\u01aa\u0007\u0002\u0000\u0000\u01aaA\u0001\u0000\u0000\u0000\u01ab\u01ac"+
		"\u0006!\uffff\uffff\u0000\u01ac\u01b3\u0003\u0004\u0002\u0000\u01ad\u01b3"+
		"\u00055\u0000\u0000\u01ae\u01af\u0005\u0004\u0000\u0000\u01af\u01b0\u0003"+
		"B!\u0000\u01b0\u01b1\u0005\u0005\u0000\u0000\u01b1\u01b3\u0001\u0000\u0000"+
		"\u0000\u01b2\u01ab\u0001\u0000\u0000\u0000\u01b2\u01ad\u0001\u0000\u0000"+
		"\u0000\u01b2\u01ae\u0001\u0000\u0000\u0000\u01b3\u01b9\u0001\u0000\u0000"+
		"\u0000\u01b4\u01b5\n\u0001\u0000\u0000\u01b5\u01b6\u0005\n\u0000\u0000"+
		"\u01b6\u01b8\u0003B!\u0002\u01b7\u01b4\u0001\u0000\u0000\u0000\u01b8\u01bb"+
		"\u0001\u0000\u0000\u0000\u01b9\u01b7\u0001\u0000\u0000\u0000\u01b9\u01ba"+
		"\u0001\u0000\u0000\u0000\u01baC\u0001\u0000\u0000\u0000\u01bb\u01b9\u0001"+
		"\u0000\u0000\u0000\u01bc\u01c6\u0003F#\u0000\u01bd\u01c6\u0003H$\u0000"+
		"\u01be\u01c6\u0003N\'\u0000\u01bf\u01c6\u0003L&\u0000\u01c0\u01c6\u0003"+
		"R)\u0000\u01c1\u01c6\u00054\u0000\u0000\u01c2\u01c6\u0005\u000b\u0000"+
		"\u0000\u01c3\u01c6\u0005\f\u0000\u0000\u01c4\u01c6\u00056\u0000\u0000"+
		"\u01c5\u01bc\u0001\u0000\u0000\u0000\u01c5\u01bd\u0001\u0000\u0000\u0000"+
		"\u01c5\u01be\u0001\u0000\u0000\u0000\u01c5\u01bf\u0001\u0000\u0000\u0000"+
		"\u01c5\u01c0\u0001\u0000\u0000\u0000\u01c5\u01c1\u0001\u0000\u0000\u0000"+
		"\u01c5\u01c2\u0001\u0000\u0000\u0000\u01c5\u01c3\u0001\u0000\u0000\u0000"+
		"\u01c5\u01c4\u0001\u0000\u0000\u0000\u01c6E\u0001\u0000\u0000\u0000\u01c7"+
		"\u01d0\u0005\u0006\u0000\u0000\u01c8\u01cd\u00038\u001c\u0000\u01c9\u01ca"+
		"\u0005\u0001\u0000\u0000\u01ca\u01cc\u00038\u001c\u0000\u01cb\u01c9\u0001"+
		"\u0000\u0000\u0000\u01cc\u01cf\u0001\u0000\u0000\u0000\u01cd\u01cb\u0001"+
		"\u0000\u0000\u0000\u01cd\u01ce\u0001\u0000\u0000\u0000\u01ce\u01d1\u0001"+
		"\u0000\u0000\u0000\u01cf\u01cd\u0001\u0000\u0000\u0000\u01d0\u01c8\u0001"+
		"\u0000\u0000\u0000\u01d0\u01d1\u0001\u0000\u0000\u0000\u01d1\u01d2\u0001"+
		"\u0000\u0000\u0000\u01d2\u01d3\u0005\u0007\u0000\u0000\u01d3G\u0001\u0000"+
		"\u0000\u0000\u01d4\u01dc\u0006$\uffff\uffff\u0000\u01d5\u01dd\u0003L&"+
		"\u0000\u01d6\u01dd\u0003R)\u0000\u01d7\u01dd\u0003F#\u0000\u01d8\u01d9"+
		"\u0005\u0004\u0000\u0000\u01d9\u01da\u00038\u001c\u0000\u01da\u01db\u0005"+
		"\u0005\u0000\u0000\u01db\u01dd\u0001\u0000\u0000\u0000\u01dc\u01d5\u0001"+
		"\u0000\u0000\u0000\u01dc\u01d6\u0001\u0000\u0000\u0000\u01dc\u01d7\u0001"+
		"\u0000\u0000\u0000\u01dc\u01d8\u0001\u0000\u0000\u0000\u01dd\u01de\u0001"+
		"\u0000\u0000\u0000\u01de\u01df\u0005\u0006\u0000\u0000\u01df\u01e0\u0003"+
		"J%\u0000\u01e0\u01e1\u0005\u0007\u0000\u0000\u01e1\u01e9\u0001\u0000\u0000"+
		"\u0000\u01e2\u01e3\n\u0002\u0000\u0000\u01e3\u01e4\u0005\u0006\u0000\u0000"+
		"\u01e4\u01e5\u0003J%\u0000\u01e5\u01e6\u0005\u0007\u0000\u0000\u01e6\u01e8"+
		"\u0001\u0000\u0000\u0000\u01e7\u01e2\u0001\u0000\u0000\u0000\u01e8\u01eb"+
		"\u0001\u0000\u0000\u0000\u01e9\u01e7\u0001\u0000\u0000\u0000\u01e9\u01ea"+
		"\u0001\u0000\u0000\u0000\u01eaI\u0001\u0000\u0000\u0000\u01eb\u01e9\u0001"+
		"\u0000\u0000\u0000\u01ec\u01ed\u00038\u001c\u0000\u01ed\u01ee\u0005\u0003"+
		"\u0000\u0000\u01ee\u01ef\u00038\u001c\u0000\u01ef\u01f2\u0001\u0000\u0000"+
		"\u0000\u01f0\u01f2\u00038\u001c\u0000\u01f1\u01ec\u0001\u0000\u0000\u0000"+
		"\u01f1\u01f0\u0001\u0000\u0000\u0000\u01f2K\u0001\u0000\u0000\u0000\u01f3"+
		"\u01f4\u00055\u0000\u0000\u01f4\u01fd\u0005\u0004\u0000\u0000\u01f5\u01fa"+
		"\u00038\u001c\u0000\u01f6\u01f7\u0005\u0001\u0000\u0000\u01f7\u01f9\u0003"+
		"8\u001c\u0000\u01f8\u01f6\u0001\u0000\u0000\u0000\u01f9\u01fc\u0001\u0000"+
		"\u0000\u0000\u01fa\u01f8\u0001\u0000\u0000\u0000\u01fa\u01fb\u0001\u0000"+
		"\u0000\u0000\u01fb\u01fe\u0001\u0000\u0000\u0000\u01fc\u01fa\u0001\u0000"+
		"\u0000\u0000\u01fd\u01f5\u0001\u0000\u0000\u0000\u01fd\u01fe\u0001\u0000"+
		"\u0000\u0000\u01fe\u01ff\u0001\u0000\u0000\u0000\u01ff\u0200\u0005\u0005"+
		"\u0000\u0000\u0200M\u0001\u0000\u0000\u0000\u0201\u0202\u0003\n\u0005"+
		"\u0000\u0202\u0203\u0005\b\u0000\u0000\u0203\u0208\u0003P(\u0000\u0204"+
		"\u0205\u0005\u0001\u0000\u0000\u0205\u0207\u0003P(\u0000\u0206\u0204\u0001"+
		"\u0000\u0000\u0000\u0207\u020a\u0001\u0000\u0000\u0000\u0208\u0206\u0001"+
		"\u0000\u0000\u0000\u0208\u0209\u0001\u0000\u0000\u0000\u0209\u020b\u0001"+
		"\u0000\u0000\u0000\u020a\u0208\u0001\u0000\u0000\u0000\u020b\u020c\u0005"+
		"\t\u0000\u0000\u020cO\u0001\u0000\u0000\u0000\u020d\u020e\u00055\u0000"+
		"\u0000\u020e\u020f\u0005\u001d\u0000\u0000\u020f\u0210\u00038\u001c\u0000"+
		"\u0210Q\u0001\u0000\u0000\u0000\u0211\u0216\u00055\u0000\u0000\u0212\u0213"+
		"\u0005\u0002\u0000\u0000\u0213\u0215\u00055\u0000\u0000\u0214\u0212\u0001"+
		"\u0000\u0000\u0000\u0215\u0218\u0001\u0000\u0000\u0000\u0216\u0214\u0001"+
		"\u0000\u0000\u0000\u0216\u0217\u0001\u0000\u0000\u0000\u0217S\u0001\u0000"+
		"\u0000\u0000\u0218\u0216\u0001\u0000\u0000\u0000*VXeox\u0080\u0088\u0096"+
		"\u009e\u00a4\u00bb\u00cd\u00d3\u00dc\u00e3\u00e9\u00f6\u0101\u0111\u0118"+
		"\u0121\u0124\u0152\u015b\u015f\u018e\u0190\u019a\u01a3\u01a7\u01b2\u01b9"+
		"\u01c5\u01cd\u01d0\u01dc\u01e9\u01f1\u01fa\u01fd\u0208\u0216";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}