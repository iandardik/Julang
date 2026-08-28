parser grammar JulayParser;
options { tokenVocab=JulayLexer; }

root
    : (import_stmt | decl)* EOF
    ;

import_stmt
    : IMPORT qualified_name
    ;

qualified_name
    : name_id (DOT name_id)+
    ;

/* Keywords allowed as module-path segments (e.g. import client.logic.Client). */
name_id
    : ID
    | CLIENT
    | PROVIDER
    | INTERNAL
    | SESSION
    | LISTOF
    | SETOF
    | MAPOF
    ;

decl
    : EXPORT? proc
    | EXPORT? api_decl
    | EXPORT? type_decl
    | compile_decl
    | EXPORT? spec
    | EXPORT? invariant_decl
    | EXPORT? fun_decl
    | EXPORT? procfun_decl
    ;

typeExpr
    : typeDomain PROCFUN_ARROW typeExpr
    | typeDomain
    ;

typeDomain
    : LPAREN typeExpr (COMMA typeExpr)+ RPAREN
    | typeAtom
    ;

typeAtom
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

procfun_decl
    : PROCFUN ID args COLON typeExpr LCURLY procfun_body* RCURLY
    ;

procfun_body
    : var
    | constructor
    | transition
    ;

api_decl
    : API ID LCURLY PROC COLON proc_expr (CALLS COLON api_call_list)? RCURLY
    ;

api_call_list
    : ID (COMMA ID)*
    ;

proc
    : PROC ID LCURLY proc_body* RCURLY
    | PROC ID ASGN_EQ proc_expr
    ;

type_decl
    : TYPE ID typeParams? LCURLY field* RCURLY
    | TYPE ID ASGN_EQ typeExpr
    | TYPE ID
    ;

type_model
    : ID ASGN_EQ LCURLY literal (COMMA literal)* RCURLY
    ;

compile_decl
    : COMPILE ID (COMMA ID)*
    ;

spec
    : SPEC ID (LBRACK ID COLON typeExpr RBRACK)? LCURLY leaf_spec_item* RCURLY
    | SPEC ID ASGN_EQ ag_spec
    | SPEC ID ASGN_EQ system_expr MODELS expr
    | SPEC ID ASGN_EQ system_expr
    ;

/* Leaf-spec body: state/actions plus delayed models (not top-level decls). */
leaf_spec_item
    : proc_body
    | type_model
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
    | with_expr
    | system_atom
    ;

with_expr
    : WITH LPAREN ID COLON typeExpr RPAREN LCURLY system_expr RCURLY
    ;

system_atom
    : system_primary LBRACK ID COLON typeExpr RBRACK (LCURLY create_index_item* RCURLY)?
    | system_primary LBRACK ID RBRACK
    | system_primary
    ;

create_index_item
    : global_decl
    | init_clause
    | type_model
    ;

global_decl
    : CONST? GLOBAL ID (COMMA ID)*
    ;

init_clause
    : INIT COLON expr
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

proc_body
    : var
    | constructor
    | transition
    ;

field
    : ID COLON typeExpr
    ;

var
    : (VAR | CONST) ID COLON typeExpr (ASGN_EQ expr)?
    ;

constructor
    : SESSION? CONSTRUCTOR ID args (ALSO args)? LCURLY constructor_body* RCURLY
    ;

transition
    : (INTERNAL | PROVIDER | CLIENT | SESSION)? TRANSITION ID args (ALSO args)? LCURLY action_body* RCURLY
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
    | return_clause
    ;

return_clause
    : RETURN COLON expr
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
    | THIS DOT ID LBRACK expr RBRACK ASGN_EQ expr
    | ID LBRACK expr RBRACK ASGN_EQ expr
    | LET ID COLON typeExpr ASGN_EQ expr
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
    | collection_literal
    | method_prop_expr
    | index_expr
    | field_access
    | record_literal
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
    | expr NIN expr
    | expr EQ expr
    | expr NEQ expr
    | expr AND expr
    | expr OR expr
    // Implication is right-associative: a => b => c ≡ a => (b => c)
    | <assoc=right> expr IMPLIES expr
    // Biconditional binds looser than implication
    | expr IFF expr
    | IF LPAREN expr RPAREN (LCURLY expr RCURLY | expr) ELSE (LCURLY expr RCURLY | expr)
    | LET LPAREN ID COLON typeExpr ASGN_EQ expr RPAREN (LCURLY expr RCURLY | expr)
    | WHEN LPAREN expr RPAREN LCURLY when_subject_arm+ RCURLY
    | WHEN LCURLY when_guard_arm+ RCURLY
    | FORALL ID COLON typeExpr COMMA expr
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
    | record_literal
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

collection_literal
    : list_literal
    | set_literal
    | map_literal
    ;

list_literal
    : LISTOF typeArgs? LPAREN (expr (COMMA expr)*)? RPAREN
    ;

set_literal
    : SETOF typeArgs? LPAREN (expr (COMMA expr)*)? RPAREN
    ;

map_literal
    : MAPOF typeArgs? LPAREN (map_entry (COMMA map_entry)*)? RPAREN
    ;

map_entry
    : expr TO expr
    ;

index_expr
    : index_expr LBRACK expr RBRACK
    | index_expr DOT ID LPAREN (call_arg (COMMA call_arg)*)? RPAREN
    | index_expr DOT ID
    | (fun_call | field_access | collection_literal | LPAREN expr RPAREN) LBRACK expr RBRACK
    | (fun_call | field_access | collection_literal | LPAREN expr RPAREN) DOT ID LPAREN (call_arg (COMMA call_arg)*)? RPAREN
    // Do NOT include field_access here: ID.DOT.ID must stay field_access (Leaf.var / obj fields).
    | (fun_call | collection_literal | LPAREN expr RPAREN) DOT ID
    ;

// method call (requires LPAREN) with optional trailing property access: xs.filter(...).length
method_prop_expr
    : method_call (DOT ID)*
    ;

// method_call requires at least one DOT so bare ID(...) stays fun_call; LPAREN is mandatory
method_call
    : THIS (DOT ID)+ LPAREN (call_arg (COMMA call_arg)*)? RPAREN
    | ID (DOT ID)+ LPAREN (call_arg (COMMA call_arg)*)? RPAREN
    ;

fun_call
    : ID typeArgs? LPAREN (call_arg (COMMA call_arg)*)? RPAREN
    ;

call_arg
    : lambda_expr
    | expr
    ;

lambda_expr
    : ID ARROW expr
    | LPAREN ID COMMA ID RPAREN ARROW expr
    ;

record_literal
    : typeExpr LCURLY record_field_assign (COMMA record_field_assign)* RCURLY
    ;

record_field_assign
    : ID ASGN_EQ expr
    ;

field_access
    : THIS DOT ID (DOT ID)*
    | ID (DOT ID)*
    ;
