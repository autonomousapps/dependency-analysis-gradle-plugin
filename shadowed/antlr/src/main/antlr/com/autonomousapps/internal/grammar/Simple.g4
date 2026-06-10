// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
grammar Simple;

file
    : fileAnnotation? .*? packageDeclaration? importList .*? EOF
    ;

fileAnnotation
    : '@file:' Identifier+ '("' Identifier+ '")'                  // @file:JvmName("Hello")
    | '@file:' Identifier+ '(' Identifier+ ':'+? Identifier+? ')' // @file:OptIn(ExperimentalRxStateApi::class)
    ;

packageDeclaration
    : 'package' qualifiedName ';'?
    ;

importList
    : importDeclaration*
    ;

importDeclaration
    : 'import' 'static'? qualifiedName ('.' '*')? ('as' Identifier)? ';'?
    ;

qualifiedName
    : Identifier ('.' Identifier)*
    ;

Identifier
    : Letter (Letter|JavaIDDigit)*
    ;

// https://unicodeplus.com/
fragment
Letter
    : '\u0024'            // $
    | '\u0041'..'\u005a'  // A-Z
    | '\u005f'            // _
    | '\u0061'..'\u007a'  // a-z
    | '\u00c0'..'\u00d6'  // z-Ö
    | '\u00d8'..'\u00f6'  // Ø-ö
    | '\u00f8'..'\u00ff'  // ø-ÿ
    | '\u0100'..'\u1fff'  // Ā-\
    | '\u3040'..'\u318f'  // \-\
    | '\u3300'..'\u337f'  // ㌀-㍿
    | '\u3400'..'\u3d2d'  // 㐀-
    | '\u4e00'..'\u9fff'  // 一-
    | '\uf900'..'\ufaff'  // 豈-
    ;

fragment
JavaIDDigit
    : '\u0030'..'\u0039' // 0-9
    | '\u0660'..'\u0669'
    | '\u06f0'..'\u06f9'
    | '\u0966'..'\u096f'
    | '\u09e6'..'\u09ef'
    | '\u0a66'..'\u0a6f'
    | '\u0ae6'..'\u0aef'
    | '\u0b66'..'\u0b6f'
    | '\u0be7'..'\u0bef'
    | '\u0c66'..'\u0c6f'
    | '\u0ce6'..'\u0cef'
    | '\u0d66'..'\u0d6f'
    | '\u0e50'..'\u0e59'
    | '\u0ed0'..'\u0ed9'
    | '\u1040'..'\u1049'
    ;

COMMENT
    : '/*' .*? '*/' -> skip
    ;

WS  : [ \r\t\u000C\n]+ -> skip
    ;

LINE_COMMENT
    : '//' ~[\r\n]* '\r'? '\n' -> skip
    ;

IGNORE
    : . -> skip
    ;
