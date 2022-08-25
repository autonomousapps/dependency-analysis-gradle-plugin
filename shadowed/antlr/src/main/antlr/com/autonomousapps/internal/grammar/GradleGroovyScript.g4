parser grammar GradleGroovyScript;

options { tokenVocab=GradleGroovyScriptLexer; }

script
    :   (dependencies|buildscript|text)* EOF
    ;

dependencies
    :   DEPENDENCIES (fileDeclaration|externalDeclaration|localDeclaration)* BRACE_CLOSE
    ;

buildscript
    :   BUILDSCRIPT BRACE_OPEN (dependencies|block|sea)* BRACE_CLOSE
    ;

block
    :   ID BRACE_OPEN (block|sea)* BRACE_CLOSE
    ;

// TODO testFixtures doesn't belong here
fileDeclaration
    :   configuration PARENS_OPEN? (FILE|FILES|TEST_FIXTURES) quote? dependency quote? PARENS_CLOSE PARENS_CLOSE? closure?
    ;

externalDeclaration
    :   configuration PARENS_OPEN? quote? dependency quote? PARENS_CLOSE? closure?
    ;

localDeclaration
    :   configuration PARENS_OPEN? PROJECT PARENS_OPEN PATH? quote? dependency quote? (COMMA CONFIGURATION quote text quote)? PARENS_CLOSE PARENS_CLOSE? closure?
    ;

configuration
    :   ID
    ;

dependency
    :   ID
    ;

closure
    :   BRACE_OPEN .+? BRACE_CLOSE
    ;

quote
    : QUOTE_SINGLE
    | QUOTE_DOUBLE
    ;

text
    : UNICODE_LATIN
    | ID
    | WS
    | DIGIT
    | FILE
    | FILES
    | EQUALS
    | SEMI
    | QUOTE_SINGLE
    | QUOTE_DOUBLE
    | BRACE_OPEN
    | BRACE_CLOSE
    | PARENS_OPEN
    | PARENS_CLOSE
    | COMMA
    ;

// Sea of crap I don't care about
sea
    : ID
    | EQUALS
    | QUOTE_SINGLE
    | QUOTE_DOUBLE
    | PARENS_OPEN
    | PARENS_CLOSE
    ;
