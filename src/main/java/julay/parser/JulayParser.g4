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
    ;

decl
    : EXPORT? proc
    | EXPORT? obj
    | EXPORT? sort_decl
    | compile_decl
    | EXPORT? spec
    | EXPORT? invariant_decl
    | EXPORT? fun_decl
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

proc
    : PROC ID LCURLY pclass_body* RCURLY
    | PROC ID ASGN_EQ proc_expr
    ;

obj
    : OBJ ID typeParams? LCURLY field* RCURLY
    ;

sort_decl
    : SORT ID ASGN_EQ LCURLY literal (COMMA literal)* RCURLY
    ;

compile_decl
    : COMPILE ID (COMMA ID)*
    ;

spec
    : SPEC ID ASGN_EQ ag_spec
    | SPEC ID ASGN_EQ system_expr MODELS expr
    | SPEC ID ASGN_EQ system_expr
    ;

ag_spec
    : LT assume_expr GT system_expr LT expr GT
    ;

assume_expr
    : TRUE
    | system_expr
    ;

system_expr
    : system_expr PARALLEL system_expr
    | system_atom
    ;

system_atom
    : system_primary LBRACK ID COLON typeExpr RBRACK
    | system_primary
    ;

system_primary
    : system_leaf
    | LPAREN system_expr RPAREN
    ;

system_leaf
    : qualified_name
    | ID
    ;

invariant_decl
    : INVARIANT ID ASGN_EQ expr
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
    : SESSION? CONSTRUCTOR ID args LCURLY constructor_body* RCURLY
    ;

transition
    : (INTERNAL | SERVICE | SESSION)? TRANSITION ID args LCURLY action_body* RCURLY
    ;

args
    : LPAREN arg? (COMMA arg)* RPAREN
    ;

arg
    : ID COLON typeExpr
    ;

constructor_body
    : before
    | transit
    | error
    | after
    ;

action_body
    : guard
    | before
    | transit
    | error
    | after
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

before
    : BEFORE COLON call_stmt+
    ;

after
    : AFTER COLON call_stmt+
    ;

call_stmt
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
    | ALL ID COLON typeExpr COMMA expr
    | EXISTS ID COLON typeExpr COMMA expr
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
    | index_expr DOT ID
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
