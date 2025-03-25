// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
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

    anno.values.orEmpty().filter {
      it is Type || it is List<*>
    }.forEach { value ->
      when (value) {
        is Type -> types.add(value.descriptor)
        is List<*> -> value.filterIsInstance<Type>().forEach { type -> types.add(type.descriptor) }
      }
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

/**
 * Matches Java fully-qualified class names as found in descriptors that are seen when analyzing bytecode with asm.
 * E.g.,
 *
 * ```
 * ()Lcom/example/producer/java/SamInterface;
 * ```
 *
 * The way this works is `\p{javaJavaIdentifierStart}` (etc) will map to a method in the `Character` class such as
 * `Character.isJavaIdentifierStart()`. So, drop the first `java`, and add the prefix `is`. I don't know why this isn't
 * clearly documented in the `Pattern` docs directly.
 *
 * @see <a href="https://docs.oracle.com/en/java/javase/24/docs/api/java.base/java/lang/Character.html">java.lang.Character</a>
 * @see <a href="https://docs.oracle.com/en/java/javase/24/docs/api/java.base/java/util/regex/Pattern.html#java">java.util.regex.Pattern</a>
 */
internal val JAVA_FQCN_REGEX_ASM =
  "L(\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*/)+\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*;".toRegex()

internal const val JAVA_SUB_PACKAGE = "(\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*)+"
