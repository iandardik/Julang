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
		COMMA=1, DOT=2, COLON=3, LPAREN=4, RPAREN=5, LCURLY=6, RCURLY=7, PARALLEL=8, 
		TRUE=9, FALSE=10, AND=11, OR=12, NOT=13, BANG=14, TIMES=15, DIV=16, MOD=17, 
		PLUS=18, MINUS=19, LT=20, LTE=21, GT=22, GTE=23, EQ=24, BANG_NEQ=25, NEQ=26, 
		ASGN_EQ=27, IMPLIES=28, IF=29, ELSE=30, LET=31, WHEN=32, ARROW=33, IMPORT=34, 
		PCLASS=35, OCLASS=36, PROC=37, PROGRAM=38, SPEC=39, VAR=40, CONSTRUCTOR=41, 
		TRANSITION=42, SERVICE=43, CONSUMER=44, GUARD=45, TRANSIT=46, ERROR=47, 
		EFFECT=48, INT=49, ID=50, STRING=51, WS=52, COMMENT=53, LINE_COMMENT=54;
	public static final int
		RULE_root = 0, RULE_import_stmt = 1, RULE_qualified_name = 2, RULE_decl = 3, 
		RULE_pclass = 4, RULE_oclass = 5, RULE_proc = 6, RULE_program = 7, RULE_spec = 8, 
		RULE_pclass_body = 9, RULE_field = 10, RULE_var = 11, RULE_constructor = 12, 
		RULE_transition = 13, RULE_args = 14, RULE_arg = 15, RULE_action_body = 16, 
		RULE_guard = 17, RULE_transit = 18, RULE_error = 19, RULE_var_transit = 20, 
		RULE_effect = 21, RULE_effect_stmt = 22, RULE_effect_call = 23, RULE_expr = 24, 
		RULE_when_subject_arm = 25, RULE_when_guard_arm = 26, RULE_when_literal = 27, 
		RULE_proc_expr = 28, RULE_value = 29, RULE_struct_literal = 30, RULE_struct_field_assign = 31, 
		RULE_field_access = 32;
	private static String[] makeRuleNames() {
		return new String[] {
			"root", "import_stmt", "qualified_name", "decl", "pclass", "oclass", 
			"proc", "program", "spec", "pclass_body", "field", "var", "constructor", 
			"transition", "args", "arg", "action_body", "guard", "transit", "error", 
			"var_transit", "effect", "effect_stmt", "effect_call", "expr", "when_subject_arm", 
			"when_guard_arm", "when_literal", "proc_expr", "value", "struct_literal", 
			"struct_field_assign", "field_access"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "','", "'.'", "':'", "'('", "')'", "'{'", "'}'", "'||'", "'true'", 
			"'false'", "'&'", "'|'", "'~'", "'!'", "'*'", "'/'", "'%'", "'+'", "'-'", 
			"'<'", "'<='", "'>'", "'>='", "'='", "'!='", "'#'", "':='", "'=>'", "'if'", 
			"'else'", "'let'", "'when'", "'->'", "'import'", "'p-class'", "'o-class'", 
			"'proc'", "'program'", "'spec'", "'var'", "'constructor'", "'transition'", 
			"'p2p-service'", "'p2p-consumer'", "'guard'", "'transit'", "'error'", 
			"'effect'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "COMMA", "DOT", "COLON", "LPAREN", "RPAREN", "LCURLY", "RCURLY", 
			"PARALLEL", "TRUE", "FALSE", "AND", "OR", "NOT", "BANG", "TIMES", "DIV", 
			"MOD", "PLUS", "MINUS", "LT", "LTE", "GT", "GTE", "EQ", "BANG_NEQ", "NEQ", 
			"ASGN_EQ", "IMPLIES", "IF", "ELSE", "LET", "WHEN", "ARROW", "IMPORT", 
			"PCLASS", "OCLASS", "PROC", "PROGRAM", "SPEC", "VAR", "CONSTRUCTOR", 
			"TRANSITION", "SERVICE", "CONSUMER", "GUARD", "TRANSIT", "ERROR", "EFFECT", 
			"INT", "ID", "STRING", "WS", "COMMENT", "LINE_COMMENT"
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
			setState(70);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 1082331758592L) != 0)) {
				{
				setState(68);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case IMPORT:
					{
					setState(66);
					import_stmt();
					}
					break;
				case PCLASS:
				case OCLASS:
				case PROC:
				case PROGRAM:
				case SPEC:
					{
					setState(67);
					decl();
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				}
				setState(72);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(73);
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
			setState(75);
			match(IMPORT);
			setState(76);
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
		public List<TerminalNode> ID() { return getTokens(JulayParser.ID); }
		public TerminalNode ID(int i) {
			return getToken(JulayParser.ID, i);
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
			setState(78);
			match(ID);
			setState(81); 
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					{
					setState(79);
					match(DOT);
					setState(80);
					match(ID);
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(83); 
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
		enterRule(_localctx, 6, RULE_decl);
		try {
			setState(90);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case PCLASS:
				enterOuterAlt(_localctx, 1);
				{
				setState(85);
				pclass();
				}
				break;
			case OCLASS:
				enterOuterAlt(_localctx, 2);
				{
				setState(86);
				oclass();
				}
				break;
			case PROC:
				enterOuterAlt(_localctx, 3);
				{
				setState(87);
				proc();
				}
				break;
			case PROGRAM:
				enterOuterAlt(_localctx, 4);
				{
				setState(88);
				program();
				}
				break;
			case SPEC:
				enterOuterAlt(_localctx, 5);
				{
				setState(89);
				spec();
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
		enterRule(_localctx, 8, RULE_pclass);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(92);
			match(PCLASS);
			setState(93);
			match(ID);
			setState(94);
			match(LCURLY);
			setState(98);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 34084860461056L) != 0)) {
				{
				{
				setState(95);
				pclass_body();
				}
				}
				setState(100);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(101);
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
		enterRule(_localctx, 10, RULE_oclass);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(103);
			match(OCLASS);
			setState(104);
			match(ID);
			setState(105);
			match(LCURLY);
			setState(109);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==ID) {
				{
				{
				setState(106);
				field();
				}
				}
				setState(111);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(112);
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
		enterRule(_localctx, 12, RULE_proc);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(114);
			match(PROC);
			setState(115);
			match(ID);
			setState(116);
			match(ASGN_EQ);
			setState(117);
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
		enterRule(_localctx, 14, RULE_program);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(119);
			match(PROGRAM);
			setState(120);
			match(ID);
			setState(121);
			match(ASGN_EQ);
			setState(122);
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
		enterRule(_localctx, 16, RULE_spec);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(124);
			match(SPEC);
			setState(125);
			match(ID);
			setState(126);
			match(ASGN_EQ);
			setState(127);
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
		enterRule(_localctx, 18, RULE_pclass_body);
		try {
			setState(132);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case VAR:
				enterOuterAlt(_localctx, 1);
				{
				setState(129);
				var();
				}
				break;
			case CONSTRUCTOR:
				enterOuterAlt(_localctx, 2);
				{
				setState(130);
				constructor();
				}
				break;
			case TRANSITION:
			case SERVICE:
			case CONSUMER:
				enterOuterAlt(_localctx, 3);
				{
				setState(131);
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
		public List<TerminalNode> ID() { return getTokens(JulayParser.ID); }
		public TerminalNode ID(int i) {
			return getToken(JulayParser.ID, i);
		}
		public TerminalNode COLON() { return getToken(JulayParser.COLON, 0); }
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
		enterRule(_localctx, 20, RULE_field);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(134);
			match(ID);
			setState(135);
			match(COLON);
			setState(136);
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
	public static class VarContext extends ParserRuleContext {
		public TerminalNode VAR() { return getToken(JulayParser.VAR, 0); }
		public List<TerminalNode> ID() { return getTokens(JulayParser.ID); }
		public TerminalNode ID(int i) {
			return getToken(JulayParser.ID, i);
		}
		public TerminalNode COLON() { return getToken(JulayParser.COLON, 0); }
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
		enterRule(_localctx, 22, RULE_var);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(138);
			match(VAR);
			setState(139);
			match(ID);
			setState(140);
			match(COLON);
			setState(141);
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
		enterRule(_localctx, 24, RULE_constructor);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(143);
			match(CONSTRUCTOR);
			setState(144);
			match(ID);
			setState(145);
			args();
			setState(146);
			match(LCURLY);
			setState(150);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 527765581332480L) != 0)) {
				{
				{
				setState(147);
				action_body();
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
		enterRule(_localctx, 26, RULE_transition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(156);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==SERVICE || _la==CONSUMER) {
				{
				setState(155);
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

			setState(158);
			match(TRANSITION);
			setState(159);
			match(ID);
			setState(160);
			args();
			setState(161);
			match(LCURLY);
			setState(165);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 527765581332480L) != 0)) {
				{
				{
				setState(162);
				action_body();
				}
				}
				setState(167);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(168);
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
		enterRule(_localctx, 28, RULE_args);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(170);
			match(LPAREN);
			setState(172);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ID) {
				{
				setState(171);
				arg();
				}
			}

			setState(178);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(174);
				match(COMMA);
				setState(175);
				arg();
				}
				}
				setState(180);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(181);
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
		public List<TerminalNode> ID() { return getTokens(JulayParser.ID); }
		public TerminalNode ID(int i) {
			return getToken(JulayParser.ID, i);
		}
		public TerminalNode COLON() { return getToken(JulayParser.COLON, 0); }
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
		enterRule(_localctx, 30, RULE_arg);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(183);
			match(ID);
			setState(184);
			match(COLON);
			setState(185);
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
		enterRule(_localctx, 32, RULE_action_body);
		try {
			setState(191);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case GUARD:
				enterOuterAlt(_localctx, 1);
				{
				setState(187);
				guard();
				}
				break;
			case TRANSIT:
				enterOuterAlt(_localctx, 2);
				{
				setState(188);
				transit();
				}
				break;
			case ERROR:
				enterOuterAlt(_localctx, 3);
				{
				setState(189);
				error();
				}
				break;
			case EFFECT:
				enterOuterAlt(_localctx, 4);
				{
				setState(190);
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
		enterRule(_localctx, 34, RULE_guard);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(193);
			match(GUARD);
			setState(194);
			match(COLON);
			setState(195);
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
		enterRule(_localctx, 36, RULE_transit);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(197);
			match(TRANSIT);
			setState(198);
			match(COLON);
			setState(202);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==ID) {
				{
				{
				setState(199);
				var_transit();
				}
				}
				setState(204);
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
		enterRule(_localctx, 38, RULE_error);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(205);
			match(ERROR);
			setState(206);
			match(COLON);
			setState(207);
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
		enterRule(_localctx, 40, RULE_var_transit);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(209);
			field_access();
			setState(210);
			match(ASGN_EQ);
			setState(211);
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
		enterRule(_localctx, 42, RULE_effect);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(213);
			match(EFFECT);
			setState(214);
			match(COLON);
			setState(216); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(215);
				effect_stmt();
				}
				}
				setState(218); 
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
		enterRule(_localctx, 44, RULE_effect_stmt);
		try {
			setState(225);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,15,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(220);
				effect_call();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(221);
				field_access();
				setState(222);
				match(ASGN_EQ);
				setState(223);
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
		enterRule(_localctx, 46, RULE_effect_call);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(227);
			match(ID);
			setState(228);
			match(LPAREN);
			setState(237);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 3940656653297168L) != 0)) {
				{
				setState(229);
				expr(0);
				setState(234);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(230);
					match(COMMA);
					setState(231);
					expr(0);
					}
					}
					setState(236);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
			}

			setState(239);
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
		public List<TerminalNode> ID() { return getTokens(JulayParser.ID); }
		public TerminalNode ID(int i) {
			return getToken(JulayParser.ID, i);
		}
		public TerminalNode COLON() { return getToken(JulayParser.COLON, 0); }
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
		int _startState = 48;
		enterRecursionRule(_localctx, 48, RULE_expr, _p);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(296);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,20,_ctx) ) {
			case 1:
				{
				setState(242);
				value();
				}
				break;
			case 2:
				{
				setState(243);
				match(LPAREN);
				setState(244);
				expr(0);
				setState(245);
				match(RPAREN);
				}
				break;
			case 3:
				{
				setState(247);
				match(NOT);
				setState(248);
				expr(21);
				}
				break;
			case 4:
				{
				setState(249);
				match(BANG);
				setState(250);
				expr(20);
				}
				break;
			case 5:
				{
				setState(251);
				match(IF);
				setState(252);
				match(LPAREN);
				setState(253);
				expr(0);
				setState(254);
				match(RPAREN);
				setState(255);
				match(LCURLY);
				setState(256);
				expr(0);
				setState(257);
				match(RCURLY);
				setState(258);
				match(ELSE);
				setState(259);
				match(LCURLY);
				setState(260);
				expr(0);
				setState(261);
				match(RCURLY);
				}
				break;
			case 6:
				{
				setState(263);
				match(LET);
				setState(264);
				match(LPAREN);
				setState(265);
				match(ID);
				setState(266);
				match(COLON);
				setState(267);
				match(ID);
				setState(268);
				match(ASGN_EQ);
				setState(269);
				expr(0);
				setState(270);
				match(RPAREN);
				setState(271);
				match(LCURLY);
				setState(272);
				expr(0);
				setState(273);
				match(RCURLY);
				}
				break;
			case 7:
				{
				setState(275);
				match(WHEN);
				setState(276);
				match(LPAREN);
				setState(277);
				expr(0);
				setState(278);
				match(RPAREN);
				setState(279);
				match(LCURLY);
				setState(281); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(280);
					when_subject_arm();
					}
					}
					setState(283); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & 2814750840849920L) != 0) );
				setState(285);
				match(RCURLY);
				}
				break;
			case 8:
				{
				setState(287);
				match(WHEN);
				setState(288);
				match(LCURLY);
				setState(290); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(289);
					when_guard_arm();
					}
					}
					setState(292); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & 3940657727038992L) != 0) );
				setState(294);
				match(RCURLY);
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(345);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,22,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(343);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,21,_ctx) ) {
					case 1:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(298);
						if (!(precpred(_ctx, 19))) throw new FailedPredicateException(this, "precpred(_ctx, 19)");
						setState(299);
						match(TIMES);
						setState(300);
						expr(20);
						}
						break;
					case 2:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(301);
						if (!(precpred(_ctx, 18))) throw new FailedPredicateException(this, "precpred(_ctx, 18)");
						setState(302);
						match(DIV);
						setState(303);
						expr(19);
						}
						break;
					case 3:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(304);
						if (!(precpred(_ctx, 17))) throw new FailedPredicateException(this, "precpred(_ctx, 17)");
						setState(305);
						match(MOD);
						setState(306);
						expr(18);
						}
						break;
					case 4:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(307);
						if (!(precpred(_ctx, 16))) throw new FailedPredicateException(this, "precpred(_ctx, 16)");
						setState(308);
						match(PLUS);
						setState(309);
						expr(17);
						}
						break;
					case 5:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(310);
						if (!(precpred(_ctx, 15))) throw new FailedPredicateException(this, "precpred(_ctx, 15)");
						setState(311);
						match(MINUS);
						setState(312);
						expr(16);
						}
						break;
					case 6:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(313);
						if (!(precpred(_ctx, 14))) throw new FailedPredicateException(this, "precpred(_ctx, 14)");
						setState(314);
						match(LT);
						setState(315);
						expr(15);
						}
						break;
					case 7:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(316);
						if (!(precpred(_ctx, 13))) throw new FailedPredicateException(this, "precpred(_ctx, 13)");
						setState(317);
						match(LTE);
						setState(318);
						expr(14);
						}
						break;
					case 8:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(319);
						if (!(precpred(_ctx, 12))) throw new FailedPredicateException(this, "precpred(_ctx, 12)");
						setState(320);
						match(GT);
						setState(321);
						expr(13);
						}
						break;
					case 9:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(322);
						if (!(precpred(_ctx, 11))) throw new FailedPredicateException(this, "precpred(_ctx, 11)");
						setState(323);
						match(GTE);
						setState(324);
						expr(12);
						}
						break;
					case 10:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(325);
						if (!(precpred(_ctx, 10))) throw new FailedPredicateException(this, "precpred(_ctx, 10)");
						setState(326);
						match(EQ);
						setState(327);
						expr(11);
						}
						break;
					case 11:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(328);
						if (!(precpred(_ctx, 9))) throw new FailedPredicateException(this, "precpred(_ctx, 9)");
						setState(329);
						match(NEQ);
						setState(330);
						expr(10);
						}
						break;
					case 12:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(331);
						if (!(precpred(_ctx, 8))) throw new FailedPredicateException(this, "precpred(_ctx, 8)");
						setState(332);
						match(BANG_NEQ);
						setState(333);
						expr(9);
						}
						break;
					case 13:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(334);
						if (!(precpred(_ctx, 7))) throw new FailedPredicateException(this, "precpred(_ctx, 7)");
						setState(335);
						match(AND);
						setState(336);
						expr(8);
						}
						break;
					case 14:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(337);
						if (!(precpred(_ctx, 6))) throw new FailedPredicateException(this, "precpred(_ctx, 6)");
						setState(338);
						match(OR);
						setState(339);
						expr(7);
						}
						break;
					case 15:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(340);
						if (!(precpred(_ctx, 5))) throw new FailedPredicateException(this, "precpred(_ctx, 5)");
						setState(341);
						match(IMPLIES);
						setState(342);
						expr(6);
						}
						break;
					}
					} 
				}
				setState(347);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,22,_ctx);
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
		public When_literalContext when_literal() {
			return getRuleContext(When_literalContext.class,0);
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
		enterRule(_localctx, 50, RULE_when_subject_arm);
		try {
			setState(355);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case TRUE:
			case FALSE:
			case INT:
			case STRING:
				enterOuterAlt(_localctx, 1);
				{
				setState(348);
				when_literal();
				setState(349);
				match(ARROW);
				setState(350);
				expr(0);
				}
				break;
			case ELSE:
				enterOuterAlt(_localctx, 2);
				{
				setState(352);
				match(ELSE);
				setState(353);
				match(ARROW);
				setState(354);
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
		enterRule(_localctx, 52, RULE_when_guard_arm);
		try {
			setState(364);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case LPAREN:
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
				setState(357);
				expr(0);
				setState(358);
				match(ARROW);
				setState(359);
				expr(0);
				}
				break;
			case ELSE:
				enterOuterAlt(_localctx, 2);
				{
				setState(361);
				match(ELSE);
				setState(362);
				match(ARROW);
				setState(363);
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
		enterRule(_localctx, 54, RULE_when_literal);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(366);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 2814749767108096L) != 0)) ) {
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
		int _startState = 56;
		enterRecursionRule(_localctx, 56, RULE_proc_expr, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(375);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,25,_ctx) ) {
			case 1:
				{
				setState(369);
				qualified_name();
				}
				break;
			case 2:
				{
				setState(370);
				match(ID);
				}
				break;
			case 3:
				{
				setState(371);
				match(LPAREN);
				setState(372);
				proc_expr(0);
				setState(373);
				match(RPAREN);
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(382);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,26,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					{
					_localctx = new Proc_exprContext(_parentctx, _parentState);
					pushNewRecursionContext(_localctx, _startState, RULE_proc_expr);
					setState(377);
					if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
					setState(378);
					match(PARALLEL);
					setState(379);
					proc_expr(2);
					}
					} 
				}
				setState(384);
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
	public static class ValueContext extends ParserRuleContext {
		public Struct_literalContext struct_literal() {
			return getRuleContext(Struct_literalContext.class,0);
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
		enterRule(_localctx, 58, RULE_value);
		try {
			setState(391);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,27,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(385);
				struct_literal();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(386);
				field_access();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(387);
				match(INT);
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(388);
				match(TRUE);
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(389);
				match(FALSE);
				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(390);
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
	public static class Struct_literalContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(JulayParser.ID, 0); }
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
		public Struct_literalContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_struct_literal; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).enterStruct_literal(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JulayParserListener ) ((JulayParserListener)listener).exitStruct_literal(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JulayParserVisitor ) return ((JulayParserVisitor<? extends T>)visitor).visitStruct_literal(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Struct_literalContext struct_literal() throws RecognitionException {
		Struct_literalContext _localctx = new Struct_literalContext(_ctx, getState());
		enterRule(_localctx, 60, RULE_struct_literal);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(393);
			match(ID);
			setState(394);
			match(LCURLY);
			setState(395);
			struct_field_assign();
			setState(400);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(396);
				match(COMMA);
				setState(397);
				struct_field_assign();
				}
				}
				setState(402);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(403);
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
		enterRule(_localctx, 62, RULE_struct_field_assign);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(405);
			match(ID);
			setState(406);
			match(ASGN_EQ);
			setState(407);
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
		enterRule(_localctx, 64, RULE_field_access);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(409);
			match(ID);
			setState(414);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,29,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(410);
					match(DOT);
					setState(411);
					match(ID);
					}
					} 
				}
				setState(416);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,29,_ctx);
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
		case 24:
			return expr_sempred((ExprContext)_localctx, predIndex);
		case 28:
			return proc_expr_sempred((Proc_exprContext)_localctx, predIndex);
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

	public static final String _serializedATN =
		"\u0004\u00016\u01a2\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001\u0002"+
		"\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004\u0002"+
		"\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007\u0007\u0007\u0002"+
		"\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0002\u000b\u0007\u000b\u0002"+
		"\f\u0007\f\u0002\r\u0007\r\u0002\u000e\u0007\u000e\u0002\u000f\u0007\u000f"+
		"\u0002\u0010\u0007\u0010\u0002\u0011\u0007\u0011\u0002\u0012\u0007\u0012"+
		"\u0002\u0013\u0007\u0013\u0002\u0014\u0007\u0014\u0002\u0015\u0007\u0015"+
		"\u0002\u0016\u0007\u0016\u0002\u0017\u0007\u0017\u0002\u0018\u0007\u0018"+
		"\u0002\u0019\u0007\u0019\u0002\u001a\u0007\u001a\u0002\u001b\u0007\u001b"+
		"\u0002\u001c\u0007\u001c\u0002\u001d\u0007\u001d\u0002\u001e\u0007\u001e"+
		"\u0002\u001f\u0007\u001f\u0002 \u0007 \u0001\u0000\u0001\u0000\u0005\u0000"+
		"E\b\u0000\n\u0000\f\u0000H\t\u0000\u0001\u0000\u0001\u0000\u0001\u0001"+
		"\u0001\u0001\u0001\u0001\u0001\u0002\u0001\u0002\u0001\u0002\u0004\u0002"+
		"R\b\u0002\u000b\u0002\f\u0002S\u0001\u0003\u0001\u0003\u0001\u0003\u0001"+
		"\u0003\u0001\u0003\u0003\u0003[\b\u0003\u0001\u0004\u0001\u0004\u0001"+
		"\u0004\u0001\u0004\u0005\u0004a\b\u0004\n\u0004\f\u0004d\t\u0004\u0001"+
		"\u0004\u0001\u0004\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0005"+
		"\u0005l\b\u0005\n\u0005\f\u0005o\t\u0005\u0001\u0005\u0001\u0005\u0001"+
		"\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0007\u0001"+
		"\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\b\u0001\b\u0001\b\u0001"+
		"\b\u0001\b\u0001\t\u0001\t\u0001\t\u0003\t\u0085\b\t\u0001\n\u0001\n\u0001"+
		"\n\u0001\n\u0001\u000b\u0001\u000b\u0001\u000b\u0001\u000b\u0001\u000b"+
		"\u0001\f\u0001\f\u0001\f\u0001\f\u0001\f\u0005\f\u0095\b\f\n\f\f\f\u0098"+
		"\t\f\u0001\f\u0001\f\u0001\r\u0003\r\u009d\b\r\u0001\r\u0001\r\u0001\r"+
		"\u0001\r\u0001\r\u0005\r\u00a4\b\r\n\r\f\r\u00a7\t\r\u0001\r\u0001\r\u0001"+
		"\u000e\u0001\u000e\u0003\u000e\u00ad\b\u000e\u0001\u000e\u0001\u000e\u0005"+
		"\u000e\u00b1\b\u000e\n\u000e\f\u000e\u00b4\t\u000e\u0001\u000e\u0001\u000e"+
		"\u0001\u000f\u0001\u000f\u0001\u000f\u0001\u000f\u0001\u0010\u0001\u0010"+
		"\u0001\u0010\u0001\u0010\u0003\u0010\u00c0\b\u0010\u0001\u0011\u0001\u0011"+
		"\u0001\u0011\u0001\u0011\u0001\u0012\u0001\u0012\u0001\u0012\u0005\u0012"+
		"\u00c9\b\u0012\n\u0012\f\u0012\u00cc\t\u0012\u0001\u0013\u0001\u0013\u0001"+
		"\u0013\u0001\u0013\u0001\u0014\u0001\u0014\u0001\u0014\u0001\u0014\u0001"+
		"\u0015\u0001\u0015\u0001\u0015\u0004\u0015\u00d9\b\u0015\u000b\u0015\f"+
		"\u0015\u00da\u0001\u0016\u0001\u0016\u0001\u0016\u0001\u0016\u0001\u0016"+
		"\u0003\u0016\u00e2\b\u0016\u0001\u0017\u0001\u0017\u0001\u0017\u0001\u0017"+
		"\u0001\u0017\u0005\u0017\u00e9\b\u0017\n\u0017\f\u0017\u00ec\t\u0017\u0003"+
		"\u0017\u00ee\b\u0017\u0001\u0017\u0001\u0017\u0001\u0018\u0001\u0018\u0001"+
		"\u0018\u0001\u0018\u0001\u0018\u0001\u0018\u0001\u0018\u0001\u0018\u0001"+
		"\u0018\u0001\u0018\u0001\u0018\u0001\u0018\u0001\u0018\u0001\u0018\u0001"+
		"\u0018\u0001\u0018\u0001\u0018\u0001\u0018\u0001\u0018\u0001\u0018\u0001"+
		"\u0018\u0001\u0018\u0001\u0018\u0001\u0018\u0001\u0018\u0001\u0018\u0001"+
		"\u0018\u0001\u0018\u0001\u0018\u0001\u0018\u0001\u0018\u0001\u0018\u0001"+
		"\u0018\u0001\u0018\u0001\u0018\u0001\u0018\u0001\u0018\u0001\u0018\u0001"+
		"\u0018\u0001\u0018\u0004\u0018\u011a\b\u0018\u000b\u0018\f\u0018\u011b"+
		"\u0001\u0018\u0001\u0018\u0001\u0018\u0001\u0018\u0001\u0018\u0004\u0018"+
		"\u0123\b\u0018\u000b\u0018\f\u0018\u0124\u0001\u0018\u0001\u0018\u0003"+
		"\u0018\u0129\b\u0018\u0001\u0018\u0001\u0018\u0001\u0018\u0001\u0018\u0001"+
		"\u0018\u0001\u0018\u0001\u0018\u0001\u0018\u0001\u0018\u0001\u0018\u0001"+
		"\u0018\u0001\u0018\u0001\u0018\u0001\u0018\u0001\u0018\u0001\u0018\u0001"+
		"\u0018\u0001\u0018\u0001\u0018\u0001\u0018\u0001\u0018\u0001\u0018\u0001"+
		"\u0018\u0001\u0018\u0001\u0018\u0001\u0018\u0001\u0018\u0001\u0018\u0001"+
		"\u0018\u0001\u0018\u0001\u0018\u0001\u0018\u0001\u0018\u0001\u0018\u0001"+
		"\u0018\u0001\u0018\u0001\u0018\u0001\u0018\u0001\u0018\u0001\u0018\u0001"+
		"\u0018\u0001\u0018\u0001\u0018\u0001\u0018\u0001\u0018\u0005\u0018\u0158"+
		"\b\u0018\n\u0018\f\u0018\u015b\t\u0018\u0001\u0019\u0001\u0019\u0001\u0019"+
		"\u0001\u0019\u0001\u0019\u0001\u0019\u0001\u0019\u0003\u0019\u0164\b\u0019"+
		"\u0001\u001a\u0001\u001a\u0001\u001a\u0001\u001a\u0001\u001a\u0001\u001a"+
		"\u0001\u001a\u0003\u001a\u016d\b\u001a\u0001\u001b\u0001\u001b\u0001\u001c"+
		"\u0001\u001c\u0001\u001c\u0001\u001c\u0001\u001c\u0001\u001c\u0001\u001c"+
		"\u0003\u001c\u0178\b\u001c\u0001\u001c\u0001\u001c\u0001\u001c\u0005\u001c"+
		"\u017d\b\u001c\n\u001c\f\u001c\u0180\t\u001c\u0001\u001d\u0001\u001d\u0001"+
		"\u001d\u0001\u001d\u0001\u001d\u0001\u001d\u0003\u001d\u0188\b\u001d\u0001"+
		"\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0005\u001e\u018f"+
		"\b\u001e\n\u001e\f\u001e\u0192\t\u001e\u0001\u001e\u0001\u001e\u0001\u001f"+
		"\u0001\u001f\u0001\u001f\u0001\u001f\u0001 \u0001 \u0001 \u0005 \u019d"+
		"\b \n \f \u01a0\t \u0001 \u0000\u000208!\u0000\u0002\u0004\u0006\b\n\f"+
		"\u000e\u0010\u0012\u0014\u0016\u0018\u001a\u001c\u001e \"$&(*,.02468:"+
		"<>@\u0000\u0002\u0001\u0000+,\u0003\u0000\t\n1133\u01bc\u0000F\u0001\u0000"+
		"\u0000\u0000\u0002K\u0001\u0000\u0000\u0000\u0004N\u0001\u0000\u0000\u0000"+
		"\u0006Z\u0001\u0000\u0000\u0000\b\\\u0001\u0000\u0000\u0000\ng\u0001\u0000"+
		"\u0000\u0000\fr\u0001\u0000\u0000\u0000\u000ew\u0001\u0000\u0000\u0000"+
		"\u0010|\u0001\u0000\u0000\u0000\u0012\u0084\u0001\u0000\u0000\u0000\u0014"+
		"\u0086\u0001\u0000\u0000\u0000\u0016\u008a\u0001\u0000\u0000\u0000\u0018"+
		"\u008f\u0001\u0000\u0000\u0000\u001a\u009c\u0001\u0000\u0000\u0000\u001c"+
		"\u00aa\u0001\u0000\u0000\u0000\u001e\u00b7\u0001\u0000\u0000\u0000 \u00bf"+
		"\u0001\u0000\u0000\u0000\"\u00c1\u0001\u0000\u0000\u0000$\u00c5\u0001"+
		"\u0000\u0000\u0000&\u00cd\u0001\u0000\u0000\u0000(\u00d1\u0001\u0000\u0000"+
		"\u0000*\u00d5\u0001\u0000\u0000\u0000,\u00e1\u0001\u0000\u0000\u0000."+
		"\u00e3\u0001\u0000\u0000\u00000\u0128\u0001\u0000\u0000\u00002\u0163\u0001"+
		"\u0000\u0000\u00004\u016c\u0001\u0000\u0000\u00006\u016e\u0001\u0000\u0000"+
		"\u00008\u0177\u0001\u0000\u0000\u0000:\u0187\u0001\u0000\u0000\u0000<"+
		"\u0189\u0001\u0000\u0000\u0000>\u0195\u0001\u0000\u0000\u0000@\u0199\u0001"+
		"\u0000\u0000\u0000BE\u0003\u0002\u0001\u0000CE\u0003\u0006\u0003\u0000"+
		"DB\u0001\u0000\u0000\u0000DC\u0001\u0000\u0000\u0000EH\u0001\u0000\u0000"+
		"\u0000FD\u0001\u0000\u0000\u0000FG\u0001\u0000\u0000\u0000GI\u0001\u0000"+
		"\u0000\u0000HF\u0001\u0000\u0000\u0000IJ\u0005\u0000\u0000\u0001J\u0001"+
		"\u0001\u0000\u0000\u0000KL\u0005\"\u0000\u0000LM\u0003\u0004\u0002\u0000"+
		"M\u0003\u0001\u0000\u0000\u0000NQ\u00052\u0000\u0000OP\u0005\u0002\u0000"+
		"\u0000PR\u00052\u0000\u0000QO\u0001\u0000\u0000\u0000RS\u0001\u0000\u0000"+
		"\u0000SQ\u0001\u0000\u0000\u0000ST\u0001\u0000\u0000\u0000T\u0005\u0001"+
		"\u0000\u0000\u0000U[\u0003\b\u0004\u0000V[\u0003\n\u0005\u0000W[\u0003"+
		"\f\u0006\u0000X[\u0003\u000e\u0007\u0000Y[\u0003\u0010\b\u0000ZU\u0001"+
		"\u0000\u0000\u0000ZV\u0001\u0000\u0000\u0000ZW\u0001\u0000\u0000\u0000"+
		"ZX\u0001\u0000\u0000\u0000ZY\u0001\u0000\u0000\u0000[\u0007\u0001\u0000"+
		"\u0000\u0000\\]\u0005#\u0000\u0000]^\u00052\u0000\u0000^b\u0005\u0006"+
		"\u0000\u0000_a\u0003\u0012\t\u0000`_\u0001\u0000\u0000\u0000ad\u0001\u0000"+
		"\u0000\u0000b`\u0001\u0000\u0000\u0000bc\u0001\u0000\u0000\u0000ce\u0001"+
		"\u0000\u0000\u0000db\u0001\u0000\u0000\u0000ef\u0005\u0007\u0000\u0000"+
		"f\t\u0001\u0000\u0000\u0000gh\u0005$\u0000\u0000hi\u00052\u0000\u0000"+
		"im\u0005\u0006\u0000\u0000jl\u0003\u0014\n\u0000kj\u0001\u0000\u0000\u0000"+
		"lo\u0001\u0000\u0000\u0000mk\u0001\u0000\u0000\u0000mn\u0001\u0000\u0000"+
		"\u0000np\u0001\u0000\u0000\u0000om\u0001\u0000\u0000\u0000pq\u0005\u0007"+
		"\u0000\u0000q\u000b\u0001\u0000\u0000\u0000rs\u0005%\u0000\u0000st\u0005"+
		"2\u0000\u0000tu\u0005\u001b\u0000\u0000uv\u00038\u001c\u0000v\r\u0001"+
		"\u0000\u0000\u0000wx\u0005&\u0000\u0000xy\u00052\u0000\u0000yz\u0005\u001b"+
		"\u0000\u0000z{\u00038\u001c\u0000{\u000f\u0001\u0000\u0000\u0000|}\u0005"+
		"\'\u0000\u0000}~\u00052\u0000\u0000~\u007f\u0005\u001b\u0000\u0000\u007f"+
		"\u0080\u00038\u001c\u0000\u0080\u0011\u0001\u0000\u0000\u0000\u0081\u0085"+
		"\u0003\u0016\u000b\u0000\u0082\u0085\u0003\u0018\f\u0000\u0083\u0085\u0003"+
		"\u001a\r\u0000\u0084\u0081\u0001\u0000\u0000\u0000\u0084\u0082\u0001\u0000"+
		"\u0000\u0000\u0084\u0083\u0001\u0000\u0000\u0000\u0085\u0013\u0001\u0000"+
		"\u0000\u0000\u0086\u0087\u00052\u0000\u0000\u0087\u0088\u0005\u0003\u0000"+
		"\u0000\u0088\u0089\u00052\u0000\u0000\u0089\u0015\u0001\u0000\u0000\u0000"+
		"\u008a\u008b\u0005(\u0000\u0000\u008b\u008c\u00052\u0000\u0000\u008c\u008d"+
		"\u0005\u0003\u0000\u0000\u008d\u008e\u00052\u0000\u0000\u008e\u0017\u0001"+
		"\u0000\u0000\u0000\u008f\u0090\u0005)\u0000\u0000\u0090\u0091\u00052\u0000"+
		"\u0000\u0091\u0092\u0003\u001c\u000e\u0000\u0092\u0096\u0005\u0006\u0000"+
		"\u0000\u0093\u0095\u0003 \u0010\u0000\u0094\u0093\u0001\u0000\u0000\u0000"+
		"\u0095\u0098\u0001\u0000\u0000\u0000\u0096\u0094\u0001\u0000\u0000\u0000"+
		"\u0096\u0097\u0001\u0000\u0000\u0000\u0097\u0099\u0001\u0000\u0000\u0000"+
		"\u0098\u0096\u0001\u0000\u0000\u0000\u0099\u009a\u0005\u0007\u0000\u0000"+
		"\u009a\u0019\u0001\u0000\u0000\u0000\u009b\u009d\u0007\u0000\u0000\u0000"+
		"\u009c\u009b\u0001\u0000\u0000\u0000\u009c\u009d\u0001\u0000\u0000\u0000"+
		"\u009d\u009e\u0001\u0000\u0000\u0000\u009e\u009f\u0005*\u0000\u0000\u009f"+
		"\u00a0\u00052\u0000\u0000\u00a0\u00a1\u0003\u001c\u000e\u0000\u00a1\u00a5"+
		"\u0005\u0006\u0000\u0000\u00a2\u00a4\u0003 \u0010\u0000\u00a3\u00a2\u0001"+
		"\u0000\u0000\u0000\u00a4\u00a7\u0001\u0000\u0000\u0000\u00a5\u00a3\u0001"+
		"\u0000\u0000\u0000\u00a5\u00a6\u0001\u0000\u0000\u0000\u00a6\u00a8\u0001"+
		"\u0000\u0000\u0000\u00a7\u00a5\u0001\u0000\u0000\u0000\u00a8\u00a9\u0005"+
		"\u0007\u0000\u0000\u00a9\u001b\u0001\u0000\u0000\u0000\u00aa\u00ac\u0005"+
		"\u0004\u0000\u0000\u00ab\u00ad\u0003\u001e\u000f\u0000\u00ac\u00ab\u0001"+
		"\u0000\u0000\u0000\u00ac\u00ad\u0001\u0000\u0000\u0000\u00ad\u00b2\u0001"+
		"\u0000\u0000\u0000\u00ae\u00af\u0005\u0001\u0000\u0000\u00af\u00b1\u0003"+
		"\u001e\u000f\u0000\u00b0\u00ae\u0001\u0000\u0000\u0000\u00b1\u00b4\u0001"+
		"\u0000\u0000\u0000\u00b2\u00b0\u0001\u0000\u0000\u0000\u00b2\u00b3\u0001"+
		"\u0000\u0000\u0000\u00b3\u00b5\u0001\u0000\u0000\u0000\u00b4\u00b2\u0001"+
		"\u0000\u0000\u0000\u00b5\u00b6\u0005\u0005\u0000\u0000\u00b6\u001d\u0001"+
		"\u0000\u0000\u0000\u00b7\u00b8\u00052\u0000\u0000\u00b8\u00b9\u0005\u0003"+
		"\u0000\u0000\u00b9\u00ba\u00052\u0000\u0000\u00ba\u001f\u0001\u0000\u0000"+
		"\u0000\u00bb\u00c0\u0003\"\u0011\u0000\u00bc\u00c0\u0003$\u0012\u0000"+
		"\u00bd\u00c0\u0003&\u0013\u0000\u00be\u00c0\u0003*\u0015\u0000\u00bf\u00bb"+
		"\u0001\u0000\u0000\u0000\u00bf\u00bc\u0001\u0000\u0000\u0000\u00bf\u00bd"+
		"\u0001\u0000\u0000\u0000\u00bf\u00be\u0001\u0000\u0000\u0000\u00c0!\u0001"+
		"\u0000\u0000\u0000\u00c1\u00c2\u0005-\u0000\u0000\u00c2\u00c3\u0005\u0003"+
		"\u0000\u0000\u00c3\u00c4\u00030\u0018\u0000\u00c4#\u0001\u0000\u0000\u0000"+
		"\u00c5\u00c6\u0005.\u0000\u0000\u00c6\u00ca\u0005\u0003\u0000\u0000\u00c7"+
		"\u00c9\u0003(\u0014\u0000\u00c8\u00c7\u0001\u0000\u0000\u0000\u00c9\u00cc"+
		"\u0001\u0000\u0000\u0000\u00ca\u00c8\u0001\u0000\u0000\u0000\u00ca\u00cb"+
		"\u0001\u0000\u0000\u0000\u00cb%\u0001\u0000\u0000\u0000\u00cc\u00ca\u0001"+
		"\u0000\u0000\u0000\u00cd\u00ce\u0005/\u0000\u0000\u00ce\u00cf\u0005\u0003"+
		"\u0000\u0000\u00cf\u00d0\u00030\u0018\u0000\u00d0\'\u0001\u0000\u0000"+
		"\u0000\u00d1\u00d2\u0003@ \u0000\u00d2\u00d3\u0005\u001b\u0000\u0000\u00d3"+
		"\u00d4\u00030\u0018\u0000\u00d4)\u0001\u0000\u0000\u0000\u00d5\u00d6\u0005"+
		"0\u0000\u0000\u00d6\u00d8\u0005\u0003\u0000\u0000\u00d7\u00d9\u0003,\u0016"+
		"\u0000\u00d8\u00d7\u0001\u0000\u0000\u0000\u00d9\u00da\u0001\u0000\u0000"+
		"\u0000\u00da\u00d8\u0001\u0000\u0000\u0000\u00da\u00db\u0001\u0000\u0000"+
		"\u0000\u00db+\u0001\u0000\u0000\u0000\u00dc\u00e2\u0003.\u0017\u0000\u00dd"+
		"\u00de\u0003@ \u0000\u00de\u00df\u0005\u001b\u0000\u0000\u00df\u00e0\u0003"+
		".\u0017\u0000\u00e0\u00e2\u0001\u0000\u0000\u0000\u00e1\u00dc\u0001\u0000"+
		"\u0000\u0000\u00e1\u00dd\u0001\u0000\u0000\u0000\u00e2-\u0001\u0000\u0000"+
		"\u0000\u00e3\u00e4\u00052\u0000\u0000\u00e4\u00ed\u0005\u0004\u0000\u0000"+
		"\u00e5\u00ea\u00030\u0018\u0000\u00e6\u00e7\u0005\u0001\u0000\u0000\u00e7"+
		"\u00e9\u00030\u0018\u0000\u00e8\u00e6\u0001\u0000\u0000\u0000\u00e9\u00ec"+
		"\u0001\u0000\u0000\u0000\u00ea\u00e8\u0001\u0000\u0000\u0000\u00ea\u00eb"+
		"\u0001\u0000\u0000\u0000\u00eb\u00ee\u0001\u0000\u0000\u0000\u00ec\u00ea"+
		"\u0001\u0000\u0000\u0000\u00ed\u00e5\u0001\u0000\u0000\u0000\u00ed\u00ee"+
		"\u0001\u0000\u0000\u0000\u00ee\u00ef\u0001\u0000\u0000\u0000\u00ef\u00f0"+
		"\u0005\u0005\u0000\u0000\u00f0/\u0001\u0000\u0000\u0000\u00f1\u00f2\u0006"+
		"\u0018\uffff\uffff\u0000\u00f2\u0129\u0003:\u001d\u0000\u00f3\u00f4\u0005"+
		"\u0004\u0000\u0000\u00f4\u00f5\u00030\u0018\u0000\u00f5\u00f6\u0005\u0005"+
		"\u0000\u0000\u00f6\u0129\u0001\u0000\u0000\u0000\u00f7\u00f8\u0005\r\u0000"+
		"\u0000\u00f8\u0129\u00030\u0018\u0015\u00f9\u00fa\u0005\u000e\u0000\u0000"+
		"\u00fa\u0129\u00030\u0018\u0014\u00fb\u00fc\u0005\u001d\u0000\u0000\u00fc"+
		"\u00fd\u0005\u0004\u0000\u0000\u00fd\u00fe\u00030\u0018\u0000\u00fe\u00ff"+
		"\u0005\u0005\u0000\u0000\u00ff\u0100\u0005\u0006\u0000\u0000\u0100\u0101"+
		"\u00030\u0018\u0000\u0101\u0102\u0005\u0007\u0000\u0000\u0102\u0103\u0005"+
		"\u001e\u0000\u0000\u0103\u0104\u0005\u0006\u0000\u0000\u0104\u0105\u0003"+
		"0\u0018\u0000\u0105\u0106\u0005\u0007\u0000\u0000\u0106\u0129\u0001\u0000"+
		"\u0000\u0000\u0107\u0108\u0005\u001f\u0000\u0000\u0108\u0109\u0005\u0004"+
		"\u0000\u0000\u0109\u010a\u00052\u0000\u0000\u010a\u010b\u0005\u0003\u0000"+
		"\u0000\u010b\u010c\u00052\u0000\u0000\u010c\u010d\u0005\u001b\u0000\u0000"+
		"\u010d\u010e\u00030\u0018\u0000\u010e\u010f\u0005\u0005\u0000\u0000\u010f"+
		"\u0110\u0005\u0006\u0000\u0000\u0110\u0111\u00030\u0018\u0000\u0111\u0112"+
		"\u0005\u0007\u0000\u0000\u0112\u0129\u0001\u0000\u0000\u0000\u0113\u0114"+
		"\u0005 \u0000\u0000\u0114\u0115\u0005\u0004\u0000\u0000\u0115\u0116\u0003"+
		"0\u0018\u0000\u0116\u0117\u0005\u0005\u0000\u0000\u0117\u0119\u0005\u0006"+
		"\u0000\u0000\u0118\u011a\u00032\u0019\u0000\u0119\u0118\u0001\u0000\u0000"+
		"\u0000\u011a\u011b\u0001\u0000\u0000\u0000\u011b\u0119\u0001\u0000\u0000"+
		"\u0000\u011b\u011c\u0001\u0000\u0000\u0000\u011c\u011d\u0001\u0000\u0000"+
		"\u0000\u011d\u011e\u0005\u0007\u0000\u0000\u011e\u0129\u0001\u0000\u0000"+
		"\u0000\u011f\u0120\u0005 \u0000\u0000\u0120\u0122\u0005\u0006\u0000\u0000"+
		"\u0121\u0123\u00034\u001a\u0000\u0122\u0121\u0001\u0000\u0000\u0000\u0123"+
		"\u0124\u0001\u0000\u0000\u0000\u0124\u0122\u0001\u0000\u0000\u0000\u0124"+
		"\u0125\u0001\u0000\u0000\u0000\u0125\u0126\u0001\u0000\u0000\u0000\u0126"+
		"\u0127\u0005\u0007\u0000\u0000\u0127\u0129\u0001\u0000\u0000\u0000\u0128"+
		"\u00f1\u0001\u0000\u0000\u0000\u0128\u00f3\u0001\u0000\u0000\u0000\u0128"+
		"\u00f7\u0001\u0000\u0000\u0000\u0128\u00f9\u0001\u0000\u0000\u0000\u0128"+
		"\u00fb\u0001\u0000\u0000\u0000\u0128\u0107\u0001\u0000\u0000\u0000\u0128"+
		"\u0113\u0001\u0000\u0000\u0000\u0128\u011f\u0001\u0000\u0000\u0000\u0129"+
		"\u0159\u0001\u0000\u0000\u0000\u012a\u012b\n\u0013\u0000\u0000\u012b\u012c"+
		"\u0005\u000f\u0000\u0000\u012c\u0158\u00030\u0018\u0014\u012d\u012e\n"+
		"\u0012\u0000\u0000\u012e\u012f\u0005\u0010\u0000\u0000\u012f\u0158\u0003"+
		"0\u0018\u0013\u0130\u0131\n\u0011\u0000\u0000\u0131\u0132\u0005\u0011"+
		"\u0000\u0000\u0132\u0158\u00030\u0018\u0012\u0133\u0134\n\u0010\u0000"+
		"\u0000\u0134\u0135\u0005\u0012\u0000\u0000\u0135\u0158\u00030\u0018\u0011"+
		"\u0136\u0137\n\u000f\u0000\u0000\u0137\u0138\u0005\u0013\u0000\u0000\u0138"+
		"\u0158\u00030\u0018\u0010\u0139\u013a\n\u000e\u0000\u0000\u013a\u013b"+
		"\u0005\u0014\u0000\u0000\u013b\u0158\u00030\u0018\u000f\u013c\u013d\n"+
		"\r\u0000\u0000\u013d\u013e\u0005\u0015\u0000\u0000\u013e\u0158\u00030"+
		"\u0018\u000e\u013f\u0140\n\f\u0000\u0000\u0140\u0141\u0005\u0016\u0000"+
		"\u0000\u0141\u0158\u00030\u0018\r\u0142\u0143\n\u000b\u0000\u0000\u0143"+
		"\u0144\u0005\u0017\u0000\u0000\u0144\u0158\u00030\u0018\f\u0145\u0146"+
		"\n\n\u0000\u0000\u0146\u0147\u0005\u0018\u0000\u0000\u0147\u0158\u0003"+
		"0\u0018\u000b\u0148\u0149\n\t\u0000\u0000\u0149\u014a\u0005\u001a\u0000"+
		"\u0000\u014a\u0158\u00030\u0018\n\u014b\u014c\n\b\u0000\u0000\u014c\u014d"+
		"\u0005\u0019\u0000\u0000\u014d\u0158\u00030\u0018\t\u014e\u014f\n\u0007"+
		"\u0000\u0000\u014f\u0150\u0005\u000b\u0000\u0000\u0150\u0158\u00030\u0018"+
		"\b\u0151\u0152\n\u0006\u0000\u0000\u0152\u0153\u0005\f\u0000\u0000\u0153"+
		"\u0158\u00030\u0018\u0007\u0154\u0155\n\u0005\u0000\u0000\u0155\u0156"+
		"\u0005\u001c\u0000\u0000\u0156\u0158\u00030\u0018\u0006\u0157\u012a\u0001"+
		"\u0000\u0000\u0000\u0157\u012d\u0001\u0000\u0000\u0000\u0157\u0130\u0001"+
		"\u0000\u0000\u0000\u0157\u0133\u0001\u0000\u0000\u0000\u0157\u0136\u0001"+
		"\u0000\u0000\u0000\u0157\u0139\u0001\u0000\u0000\u0000\u0157\u013c\u0001"+
		"\u0000\u0000\u0000\u0157\u013f\u0001\u0000\u0000\u0000\u0157\u0142\u0001"+
		"\u0000\u0000\u0000\u0157\u0145\u0001\u0000\u0000\u0000\u0157\u0148\u0001"+
		"\u0000\u0000\u0000\u0157\u014b\u0001\u0000\u0000\u0000\u0157\u014e\u0001"+
		"\u0000\u0000\u0000\u0157\u0151\u0001\u0000\u0000\u0000\u0157\u0154\u0001"+
		"\u0000\u0000\u0000\u0158\u015b\u0001\u0000\u0000\u0000\u0159\u0157\u0001"+
		"\u0000\u0000\u0000\u0159\u015a\u0001\u0000\u0000\u0000\u015a1\u0001\u0000"+
		"\u0000\u0000\u015b\u0159\u0001\u0000\u0000\u0000\u015c\u015d\u00036\u001b"+
		"\u0000\u015d\u015e\u0005!\u0000\u0000\u015e\u015f\u00030\u0018\u0000\u015f"+
		"\u0164\u0001\u0000\u0000\u0000\u0160\u0161\u0005\u001e\u0000\u0000\u0161"+
		"\u0162\u0005!\u0000\u0000\u0162\u0164\u00030\u0018\u0000\u0163\u015c\u0001"+
		"\u0000\u0000\u0000\u0163\u0160\u0001\u0000\u0000\u0000\u01643\u0001\u0000"+
		"\u0000\u0000\u0165\u0166\u00030\u0018\u0000\u0166\u0167\u0005!\u0000\u0000"+
		"\u0167\u0168\u00030\u0018\u0000\u0168\u016d\u0001\u0000\u0000\u0000\u0169"+
		"\u016a\u0005\u001e\u0000\u0000\u016a\u016b\u0005!\u0000\u0000\u016b\u016d"+
		"\u00030\u0018\u0000\u016c\u0165\u0001\u0000\u0000\u0000\u016c\u0169\u0001"+
		"\u0000\u0000\u0000\u016d5\u0001\u0000\u0000\u0000\u016e\u016f\u0007\u0001"+
		"\u0000\u0000\u016f7\u0001\u0000\u0000\u0000\u0170\u0171\u0006\u001c\uffff"+
		"\uffff\u0000\u0171\u0178\u0003\u0004\u0002\u0000\u0172\u0178\u00052\u0000"+
		"\u0000\u0173\u0174\u0005\u0004\u0000\u0000\u0174\u0175\u00038\u001c\u0000"+
		"\u0175\u0176\u0005\u0005\u0000\u0000\u0176\u0178\u0001\u0000\u0000\u0000"+
		"\u0177\u0170\u0001\u0000\u0000\u0000\u0177\u0172\u0001\u0000\u0000\u0000"+
		"\u0177\u0173\u0001\u0000\u0000\u0000\u0178\u017e\u0001\u0000\u0000\u0000"+
		"\u0179\u017a\n\u0001\u0000\u0000\u017a\u017b\u0005\b\u0000\u0000\u017b"+
		"\u017d\u00038\u001c\u0002\u017c\u0179\u0001\u0000\u0000\u0000\u017d\u0180"+
		"\u0001\u0000\u0000\u0000\u017e\u017c\u0001\u0000\u0000\u0000\u017e\u017f"+
		"\u0001\u0000\u0000\u0000\u017f9\u0001\u0000\u0000\u0000\u0180\u017e\u0001"+
		"\u0000\u0000\u0000\u0181\u0188\u0003<\u001e\u0000\u0182\u0188\u0003@ "+
		"\u0000\u0183\u0188\u00051\u0000\u0000\u0184\u0188\u0005\t\u0000\u0000"+
		"\u0185\u0188\u0005\n\u0000\u0000\u0186\u0188\u00053\u0000\u0000\u0187"+
		"\u0181\u0001\u0000\u0000\u0000\u0187\u0182\u0001\u0000\u0000\u0000\u0187"+
		"\u0183\u0001\u0000\u0000\u0000\u0187\u0184\u0001\u0000\u0000\u0000\u0187"+
		"\u0185\u0001\u0000\u0000\u0000\u0187\u0186\u0001\u0000\u0000\u0000\u0188"+
		";\u0001\u0000\u0000\u0000\u0189\u018a\u00052\u0000\u0000\u018a\u018b\u0005"+
		"\u0006\u0000\u0000\u018b\u0190\u0003>\u001f\u0000\u018c\u018d\u0005\u0001"+
		"\u0000\u0000\u018d\u018f\u0003>\u001f\u0000\u018e\u018c\u0001\u0000\u0000"+
		"\u0000\u018f\u0192\u0001\u0000\u0000\u0000\u0190\u018e\u0001\u0000\u0000"+
		"\u0000\u0190\u0191\u0001\u0000\u0000\u0000\u0191\u0193\u0001\u0000\u0000"+
		"\u0000\u0192\u0190\u0001\u0000\u0000\u0000\u0193\u0194\u0005\u0007\u0000"+
		"\u0000\u0194=\u0001\u0000\u0000\u0000\u0195\u0196\u00052\u0000\u0000\u0196"+
		"\u0197\u0005\u001b\u0000\u0000\u0197\u0198\u00030\u0018\u0000\u0198?\u0001"+
		"\u0000\u0000\u0000\u0199\u019e\u00052\u0000\u0000\u019a\u019b\u0005\u0002"+
		"\u0000\u0000\u019b\u019d\u00052\u0000\u0000\u019c\u019a\u0001\u0000\u0000"+
		"\u0000\u019d\u01a0\u0001\u0000\u0000\u0000\u019e\u019c\u0001\u0000\u0000"+
		"\u0000\u019e\u019f\u0001\u0000\u0000\u0000\u019fA\u0001\u0000\u0000\u0000"+
		"\u01a0\u019e\u0001\u0000\u0000\u0000\u001eDFSZbm\u0084\u0096\u009c\u00a5"+
		"\u00ac\u00b2\u00bf\u00ca\u00da\u00e1\u00ea\u00ed\u011b\u0124\u0128\u0157"+
		"\u0159\u0163\u016c\u0177\u017e\u0187\u0190\u019e";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}