grammar GradleGroovyScript;

script
    :   (dependencies|buildscript|text)* EOF
    ;

dependencies
    :   'dependencies' '{' (fileDeclaration|externalDeclaration|localDeclaration)* '}'
    ;

buildscript
    :   'buildscript' '{' (dependencies|block|sea)* '}'
    ;

block
    :   ID '{' (block|sea)* '}'
    ;

fileDeclaration
    :   configuration '('? (FILE|FILES|TEST_FIXTURES) ('\'' | '"')? dependency ('\'' | '"')? ')' ')'?
    ;

externalDeclaration
    :   configuration '('? ('\'' | '"')? dependency ('\'' | '"')? ')'? closure?
    ;

localDeclaration
    :   configuration '('? 'project(' ('path:')? ('\'' | '"')? dependency ('\'' | '"')? ')' ')'? closure?
    ;

configuration
    :   ID
    ;

dependency
    :   ID
    ;

closure
    :   '{' .+? '}'
    ;

text
    : UNICODE_LATIN
    | ID
    | WS
    | DIGIT
    | FILE
    | FILES
    | '='
    | ';'
    | '\''
    | '"'
    | '{'
    | '}'
    | '('
    | ')'
    ;

// Sea of crap I don't care about
sea
    : ID
    | EQUALS
    | '\''
    | '"'
    | '('
    | ')'
    ;

// should include more unicode characters
//ID : [a-zA-Z_:]+ [a-zA-Z0-9_:+\-.]* ;
EQUALS: '=';
UNICODE_LATIN: '\u0021'..'\u007E';
ID: Letter LetterOrDigitEtc*;
NAME: Letter LetterOrDigit*;
DIGIT: [0-9];
FILE: 'file(';
FILES: 'files(';
TEST_FIXTURES: 'testFixtures(';

fragment Letter
    : [a-zA-Z$_:] // these are the "java letters" below 0x7F
    | ~[\u0000-\u007F\uD800-\uDBFF] // covers all characters above 0x7F which are not a surrogate
    | [\uD800-\uDBFF] [\uDC00-\uDFFF] // covers UTF-16 surrogate pairs encodings for U+10000 to U+10FFFF
    ;

fragment LetterOrDigit
    : Letter
    | [0-9]
    ;

fragment LetterOrDigitEtc
    : LetterOrDigit
    | [+\-./${}]
    ;

COMMENT : '/*' .*? '*/' -> channel(HIDDEN);
// \u000C is form-feed
WS : [ \r\t\u000C\n]+ -> channel(HIDDEN);
LINE_COMMENT : '//' ~[\r\n]* '\r'? '\n' -> channel(HIDDEN);
IGNORE : . -> channel(HIDDEN);
