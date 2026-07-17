parser grammar JulayParser;
options { tokenVocab=JulayLexer; }

root
    : (import_stmt | decl)* EOF
    ;

import_stmt
    : IMPORT qualified_name
    ;

qualified_name
    : ID (DOT qual_segment)+
    ;

qual_segment
    : ID
    | FUN
    ;

decl
    : pclass
    | oclass
    | proc
    | program
    | spec
    | fun_decl
    ;

typeExpr
    : ID typeArgs?
    | LPAREN typeExpr RPAREN
    ;

typeArgs
    : LT typeExpr (COMMA typeExpr)* GT
    ;

typeParams
    : LT ID (COMMA ID)* GT
    ;

fun_decl
    : FUN ID typeParams? args COLON typeExpr EQ expr
    ;

pclass
    : PCLASS ID LCURLY pclass_body* RCURLY
    ;

oclass
    : OCLASS ID typeParams? LCURLY field* RCURLY
    ;

proc
    : PROC ID ASGN_EQ proc_expr
    ;

program
    : PROGRAM ID ASGN_EQ proc_expr
    ;

spec
    : SPEC ID ASGN_EQ proc_expr
    ;

pclass_body
    : var
    | constructor
    | transition
    ;

field
    : ID COLON typeExpr
    ;

var
    : (VAR | CONST) ID COLON typeExpr
    ;

constructor
    : CONSTRUCTOR ID args LCURLY constructor_body* RCURLY
    ;

transition
    : (INTERNAL | SERVICE)? TRANSITION ID channel_bind? args LCURLY action_body* RCURLY
    ;

channel_bind
    : LT ID GT
    ;

args
    : LPAREN arg? (COMMA arg)* RPAREN
    ;

arg
    : ID COLON typeExpr
    ;

constructor_body
    : transit
    | error
    | effect
    ;

action_body
    : guard
    | transit
    | error
    | effect
    ;

guard
    : GUARD COLON expr
    ;

transit
    : TRANSIT COLON var_transit*
    ;

error
    : ERROR COLON error_arm+
    ;

error_arm
    : expr ARROW expr
    ;

var_transit
    : field_access ASGN_EQ expr
    | ID LBRACK expr RBRACK ASGN_EQ expr
    ;

effect
    : EFFECT COLON effect_stmt+
    ;

effect_stmt
    : effect_call
    | field_access ASGN_EQ effect_call
    ;

effect_call
    : ID typeArgs? LPAREN (expr (COMMA expr)*)? RPAREN
    ;

// order approximately according to the Java rules: https://introcs.cs.princeton.edu/java/11precedence/
expr
    : literal
    | LPAREN expr RPAREN
    | bracket_literal
    | set_literal
    | index_expr
    | field_access
    | oclass_literal
    | fun_call
    | NOT expr
    // Prefix & / | are no-ops (TLA+ style formatting); same precedence as ~
    | AND expr
    | OR expr
    | expr TIMES expr
    | expr DIV expr
    | expr MOD expr
    | expr PLUS expr
    | expr MINUS expr
    | expr LT expr
    | expr LTE expr
    | expr GT expr
    | expr GTE expr
    | expr IN expr
    | expr EQ expr
    | expr NEQ expr
    | expr AND expr
    | expr OR expr
    // Implication is right-associative: a => b => c ≡ a => (b => c)
    | <assoc=right> expr IMPLIES expr
    // Biconditional binds looser than implication
    | expr IFF expr
    | IF LPAREN expr RPAREN LCURLY expr RCURLY ELSE LCURLY expr RCURLY
    | LET LPAREN ID COLON typeExpr ASGN_EQ expr RPAREN LCURLY expr RCURLY
    | WHEN LPAREN expr RPAREN LCURLY when_subject_arm+ RCURLY
    | WHEN LCURLY when_guard_arm+ RCURLY
    ;

when_subject_arm
    : when_pattern ARROW expr
    | ELSE ARROW expr
    ;

when_guard_arm
    : expr ARROW expr
    | ELSE ARROW expr
    ;

when_pattern
    : literal
    | oclass_literal
    ;

proc_expr
    : qualified_name
    | ID
    | LPAREN proc_expr RPAREN
    | proc_expr PARALLEL proc_expr
    ;

literal
    : INT
    | REAL
    | TRUE
    | FALSE
    | STRING
    ;

bracket_literal
    : LBRACK RBRACK
    | LBRACK map_entry (COMMA map_entry)* RBRACK
    | LBRACK expr (COMMA expr)* RBRACK
    ;

map_entry
    : expr ARROW expr
    ;

set_literal
    : LCURLY (expr (COMMA expr)*)? RCURLY
    ;

index_expr
    : index_expr LBRACK index_or_slice RBRACK
    | (fun_call | field_access | bracket_literal | set_literal | LPAREN expr RPAREN) LBRACK index_or_slice RBRACK
    ;

index_or_slice
    : expr COLON expr
    | expr
    ;

fun_call
    : ID typeArgs? LPAREN (expr (COMMA expr)*)? RPAREN
    ;

oclass_literal
    : typeExpr LCURLY oclass_field_assign (COMMA oclass_field_assign)* RCURLY
    ;

oclass_field_assign
    : ID ASGN_EQ expr
    ;

field_access
    : ID (DOT ID)*
    ;
