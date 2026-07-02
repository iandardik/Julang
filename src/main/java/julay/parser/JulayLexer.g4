lexer grammar JulayLexer;

COMMA : ',' ;
DOT : '.' ;
COLON : ':' ;
LPAREN : '(' ;
RPAREN : ')' ;
LCURLY : '{' ;
RCURLY : '}' ;
PARALLEL : '||' ;

TRUE : 'true' ;
FALSE : 'false' ;
AND : '&' ;
OR : '|' ;
NOT : '~' ;
BANG : '!' ;
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
BANG_NEQ : '!=' ;
NEQ : '#' ;
ASGN_EQ : ':=' ;
IMPLIES : '=>' ;
IF : 'if' ;
ELSE : 'else' ;
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

INT : [-]?[0-9]+ ;
ID: [a-zA-Z_][a-zA-Z_0-9]* ;
STRING: '"' ~["]* '"' ;

WS           : [ \t\r\n\u000C]+ -> channel(HIDDEN);
COMMENT      : '/*' .*? '*/'    -> channel(HIDDEN);
LINE_COMMENT : '//' ~[\r\n]*    -> channel(HIDDEN);
