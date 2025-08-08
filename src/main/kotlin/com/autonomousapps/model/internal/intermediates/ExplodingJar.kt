// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model.internal.intermediates

import com.autonomousapps.internal.AnalyzedClass
import com.autonomousapps.internal.ClassNames
import com.autonomousapps.internal.utils.efficient
import com.autonomousapps.internal.utils.reallyAll
import com.autonomousapps.model.internal.KtFile
import com.autonomousapps.model.internal.intermediates.producer.BinaryClass
import com.autonomousapps.model.internal.intermediates.producer.Constant

/**
 * Contains information about what features or capabilities a given "jar" provides. "Jar" is in
 * quotation marks because this really represents more of a meta-jar. It includes information about
 * the "real" jar (i.e. library with source code), as well as any associated lint jar that may be
 * present.
 *
 * The algorithm for [isCompileOnlyCandidate] is that the jar must _only_ contain:
 * 1. Annotation classes.
 * 2. The above, plus types that only exist to provide a sort of "namespace". For example,
 *    `org.jetbrains.annotations.ApiStatus` has no members. It only has inner classes which are
 *    themselves annotations.
 * 3. All of the above, plus types that are only used by the annotations in the jar.
 *    For example, the `@org.intellij.lang.annotations.PrintFormat` annotation uses a class (in
 *    this case defined in the same file), `org.intellij.lang.annotations.PrintFormatPattern`. The
 *    assumption here is that such classes (`PrintFormatPattern`) are only required during
 *    compilation, for their associated annotations.
 *
 * The algorithm for [isLintJar] is that the jar must meet these conditions:
 * 1. It must contain no classes (`analyzedClasses` is empty) AND
 * 2. It must contain an Android lint registry.
 */
internal class ExplodingJar(
  analyzedClasses: Set<AnalyzedClass>,
  val ktFiles: Set<KtFile>,
  val androidLintRegistry: String?
) {

  /**
   * The set of classes provided by this jar, including information about their superclass, interfaces, and public
   * members. May be empty.
   */
  val binaryClasses: Set<BinaryClass> = analyzedClasses.asSequence()
    .map { it.binaryClass }
    .toSortedSet()
    .efficient()

  /**
   * The set of security providers (classes that extend `java.security.Provider`) provided by this
   * jar. May be empty.
   */
  val securityProviders: Set<String> = analyzedClasses.asSequence()
    .filter { ClassNames.isSecurityProvider(it.superClassName) }
    .map { it.className }
    .toSortedSet()
    .efficient()

  /**
   * Map of class names to the public constants they declare. May be empty.
   */
  val constants: Map<String, Set<Constant>> = analyzedClasses.asSequence()
    .filterNot { it.constants.isEmpty() }
    // normalize class names to only contain '.' characters (in case of inner classes)
    .associate { it.className.replace('$', '.') to it.constants }
    .efficient()

  /**
   * A jar is a lint jar if it's _only_ for linting.
   *
   * nb: We're deliberately using `all` here because it is also true if the collection is empty,
   * which is what we want.
   */
  val isLintJar: Boolean = analyzedClasses.all { it.hasNoMembers } && androidLintRegistry != null

  /**
   * True if this jar is a candidate for the `compileOnly` configuration, and false otherwise. See
   * the class-level javadoc for an explanation of the algorithm.
   */
  val isCompileOnlyCandidate: Boolean =
    if (analyzedClasses.isEmpty()) {
      false
    } else if (analyzedClasses.none { isAnnotation(it) }) {
      false
    } else {
      var value = true
      for (analyzedClass in analyzedClasses) {
        if (isNotAnnotation(analyzedClass)) {
          // it is ok if it's not an annotation class, if it is a "namespace class".
          if (!isNamespaceClass(analyzedClass, analyzedClasses)) {
            // it's ok if it is not a namespace class, if it's private (non-public)
            if (isPublic(analyzedClass)) {
              // it's ok if it's public, if it's an enum which is an inner element of an Annotation
              if (!(isEnum(analyzedClass) && isOuterClassAnnotation(analyzedClass, analyzedClasses))) {
                value = false
                break
              }
            }
          }
        }
      }
      value
    }

  /*
   * compileOnly candidate helper functions
   */

  private fun isAnnotation(analyzedClass: AnalyzedClass): Boolean =
    analyzedClass.retentionPolicy != null

  private fun isNotAnnotation(analyzedClass: AnalyzedClass): Boolean =
    !isAnnotation(analyzedClass)

  private fun isNamespaceClass(analyzedClass: AnalyzedClass, analyzedClasses: Set<AnalyzedClass>): Boolean =
    analyzedClass.hasNoMembers && analyzedClasses
      .filter { analyzedClass.innerClasses.contains(it.className) }
      .reallyAll { isAnnotation(it) }

  private fun isOuterClassAnnotation(
    analyzedClass: AnalyzedClass,
    analyzedClasses: Set<AnalyzedClass>
  ): Boolean =
    analyzedClasses
      .filter { analyzedClass.outerClassName == it.className }
      .reallyAll { isAnnotation(it) }

  // should be named "isEffectivelyPublic"
  private fun isPublic(analyzedClass: AnalyzedClass): Boolean =
    analyzedClass.access.isPublic || analyzedClass.access.isProtected

  private fun isEnum(analyzedClass: AnalyzedClass): Boolean = ClassNames.isEnum(analyzedClass.superClassName)
}
