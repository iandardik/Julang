parser grammar JulayParser;
options { tokenVocab=JulayLexer; }

root
    : decl* EOF
    ;

decl
    : pclass
    | proc
    | program
    | spec
    ;

pclass
    : PCLASS ID LCURLY pclass_body* RCURLY
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

var
    : VAR ID COLON ID
    ;

constructor
    : CONSTRUCTOR ID args LCURLY action_body* RCURLY
    ;

transition
    : TRANSITION ID args LCURLY action_body* RCURLY
    ;

args
    : LPAREN arg? (COMMA arg)* RPAREN
    ;

arg
    : ID COLON ID
    ;

action_body
    : guard
    | transit
    | error
    ;

guard
    : GUARD COLON expr
    ;

transit
    : TRANSIT COLON var_transit*
    ;

error
    : ERROR COLON expr
    ;

var_transit
    : ID ASGN_EQ expr
    ;

// order according to the Java rules: https://introcs.cs.princeton.edu/java/11precedence/
expr
    : value
    | LPAREN expr RPAREN
    | NOT expr
    | BANG expr
    | expr PLUS expr
    | expr MINUS expr
    | expr LT expr
    | expr LTE expr
    | expr GT expr
    | expr GTE expr
    | expr EQ expr
    | expr NEQ expr
    | expr BANG_NEQ expr
    | expr AND expr
    | expr OR expr
    ;

proc_expr
    : ID
    | LPAREN proc_expr RPAREN
    | proc_expr PARALLEL proc_expr
    ;

value
    : ID
    | INT
    | TRUE
    | FALSE
    | STRING
    ;