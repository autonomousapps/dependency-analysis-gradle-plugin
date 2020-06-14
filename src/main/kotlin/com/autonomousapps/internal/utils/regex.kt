package com.autonomousapps.internal.utils

// Begins with an 'L'
// followed by at least one word character
// followed by one or more word char, /, or $, in any combination
// ends with a ';'
// Not perfect, but probably close enough
internal val METHOD_DESCRIPTOR_REGEX = """L\w[\w/$]+;""".toRegex()

// TODO sync with above. Note this has a capturing group.
internal val DESC_REGEX = """L(\w[\w/$]+);""".toRegex()

// This regex matches a Java FQCN.
// https://stackoverflow.com/questions/5205339/regular-expression-matching-fully-qualified-class-names#comment5855158_5205467
internal val JAVA_FQCN_REGEX =
  "(\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*\\.)+\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*".toRegex()
internal val JAVA_FQCN_REGEX_SLASHY =
  "(\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*/)+\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*".toRegex()

internal const val JAVA_SUB_PACKAGE = "(\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*)+"
