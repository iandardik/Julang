lexer grammar JulayLexer;

COMMA : ',' ;
DOT : '.' ;
COLON : ':' ;
LPAREN : '(' ;
RPAREN : ')' ;
LBRACK : '[' ;
RBRACK : ']' ;
LCURLY : '{' ;
RCURLY : '}' ;
PARALLEL : '||' ;

TRUE : 'true' ;
FALSE : 'false' ;
AND : '&' ;
OR : '|' ;
NOT : '~' ;
TIMES : '*' ;
DIV : '/' ;
MOD : '%' ;
PLUS : '+' ;
MINUS : '-' ;
LT : '<' ;
LTE : '<=' ;
GT : '>' ;
GTE : '>=' ;
EQ : '=' ;
NEQ : '~=' ;
ASGN_EQ : ':=' ;
IMPLIES : '=>' ;
IF : 'if' ;
ELSE : 'else' ;
LET : 'let' ;
WHEN : 'when' ;
IN : 'in' ;
ARROW : '->' ;
IMPORT : 'import' ;


PCLASS : 'p-class' ;
OCLASS : 'o-class' ;
PROC : 'proc' ;
PROGRAM : 'program' ;
SPEC : 'spec' ;
VAR : 'var' ;
CONSTRUCTOR : 'constructor' ;
TRANSITION : 'transition' ;
SERVICE : 'p2p-service' ;
CONSUMER : 'p2p-consumer' ;
GUARD : 'guard' ;
TRANSIT : 'transit' ;
ERROR : 'error' ;
EFFECT : 'effect' ;
FUN : 'fun' ;

REAL : [-]?[0-9]+ '.' [0-9]+ ;
INT : [-]?[0-9]+ ;
ID: [a-zA-Z_][a-zA-Z_0-9]* ;
STRING: '"' ~["]* '"' ;

WS           : [ \t\r\n\u000C]+ -> channel(HIDDEN);
COMMENT      : '/*' .*? '*/'    -> channel(HIDDEN);
LINE_COMMENT : '//' ~[\r\n]*    -> channel(HIDDEN);
