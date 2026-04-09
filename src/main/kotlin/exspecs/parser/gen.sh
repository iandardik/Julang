#!/bin/bash
java -jar ~/bin/antlr-4.13.2-complete.jar JulayLexer.g4 
java -jar ~/bin/antlr-4.13.2-complete.jar JulayParser.g4 -visitor

sed -i '' '1i\
package exspecs.parser;
' *.java
