lexer grammar GradleGroovyScriptLexer;

channels { WHITESPACE, COMMENTS }

DEPENDENCIES: 'dependencies' WS* BRACE_OPEN;
FILE: 'file(';
FILES: 'files(';
TEST_FIXTURES: 'testFixtures(';
PROJECT: 'project';
BUILDSCRIPT: 'buildscript';

BRACE_OPEN: '{';
BRACE_CLOSE: '}';
PARENS_OPEN: '(';
PARENS_CLOSE: ')';
QUOTE_SINGLE: '\'';
QUOTE_DOUBLE: '"';
EQUALS: '=';
SEMI: ';';
BACKSLASH: '\\';

UNICODE_LATIN: '\u0021'..'\u007E';
ID: Letter LetterOrDigitEtc*;
NAME: Letter LetterOrDigit*;
DIGIT: [0-9];

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

COMMENT : '/*' .*? '*/' -> channel(COMMENTS);
LINE_COMMENT : '//' ~[\r\n]* '\r'? '\n' -> channel(COMMENTS);
// \u000C is form-feed
WS : [ \r\t\u000C\n]+ -> channel(WHITESPACE);
IGNORE : . -> channel(HIDDEN);
