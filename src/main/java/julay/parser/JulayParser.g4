parser grammar JulayParser;
options { tokenVocab=JulayLexer; }

root
    : decl* EOF
    ;

decl
    : pclass
    | oclass
    | proc
    | program
    | spec
    ;

pclass
    : PCLASS ID LCURLY pclass_body* RCURLY
    ;

oclass
    : OCLASS ID LCURLY field* RCURLY
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
    : ID COLON ID
    ;

var
    : VAR ID COLON ID
    ;

constructor
    : CONSTRUCTOR ID args LCURLY action_body* RCURLY
    ;

transition
    : (SERVICE | CONSUMER)? TRANSITION ID args LCURLY action_body* RCURLY
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
    : field_access ASGN_EQ expr
    ;

// order according to the Java rules: https://introcs.cs.princeton.edu/java/11precedence/
expr
    : value
    | LPAREN expr RPAREN
    | NOT expr
    | BANG expr
    | expr TIMES expr
    | expr DIV expr
    | expr MOD expr
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
    | expr IMPLIES expr
    | IF LPAREN expr RPAREN LCURLY expr RCURLY ELSE LCURLY expr RCURLY
    ;

proc_expr
    : ID
    | LPAREN proc_expr RPAREN
    | proc_expr PARALLEL proc_expr
    ;

value
    : struct_literal
    | field_access
    | INT
    | TRUE
    | FALSE
    | STRING
    ;

struct_literal
    : ID LCURLY struct_field_assign (COMMA struct_field_assign)* RCURLY
    ;

struct_field_assign
    : ID ASGN_EQ expr
    ;

field_access
    : ID (DOT ID)*
    ;