package com.autonomousapps.internal.utils

import com.autonomousapps.internal.asm.Type
import com.autonomousapps.internal.asm.tree.AnnotationNode

// Begins with an 'L'
// followed by at least one word character
// followed by one or more word char, /, or $, in any combination
// ends with a ';'
// Not perfect, but probably close enough
internal val METHOD_DESCRIPTOR_REGEX = """L\w[\w/$]+;""".toRegex()
internal val DESC_REGEX = """L(\w[\w/$]+);""".toRegex()
internal val TYPE_REGEX = """<(.+?)>""".toRegex()
private val FULL_DESC_REGEX = """(L\w[\w/$]+;)""".toRegex()

internal fun String.genericTypes(): Set<String> = TYPE_REGEX.findAll(this)
  .allItems()
  .flatMapToSet { FULL_DESC_REGEX.findAll(it).allItems() }

internal fun List<AnnotationNode>?.annotationTypes(): Set<String> {
  if (this == null) return emptySet()

  val types = mutableSetOf<String>()

  forEach { anno ->
    types.add(anno.desc)
    anno.values.orEmpty().filterIsInstance<Type>().forEach { type ->
      types.add(type.descriptor)
    }
  }

  return types
}

// This regex matches a Java FQCN.
// https://stackoverflow.com/questions/5205339/regular-expression-matching-fully-qualified-class-names#comment5855158_5205467
internal val JAVA_FQCN_REGEX_DOTTY =
  "(\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*\\.)+\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*".toRegex()
internal val JAVA_FQCN_REGEX_SLASHY =
  "(\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*/)+\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*".toRegex()

internal const val JAVA_SUB_PACKAGE = "(\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*)+"
