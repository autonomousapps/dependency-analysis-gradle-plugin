package com.autonomousapps.internal

import com.autonomousapps.internal.utils.mapToOrderedSet
import com.autonomousapps.internal.utils.reallyAll
import java.lang.annotation.RetentionPolicy

/**
 * Contains information about what features or capabilities a given "jar" provides. "Jar" is in
 * quotation marks because this really represents more of a meta-jar. It includes information about
 * the "real" jar (i.e. library with source code), as well as any associated lint jar that may be
 * present.
 *
 * The algorithm for [isCompileOnlyCandidate] is that the jar must _only_ contain:
 * 1. Annotation classes with `CLASS` or `SOURCE` retention policies (or no policy => `CLASS` is
 *    default).
 * 2. The above, plus types that only exist to provide a sort of "namespace". For example,
 *    `org.jetbrains.annotations.ApiStatus` has no members. It only has inner classes which are
 *    themselves annotations that comply with 1.
 * 3. All of the above, plus types that are only used by the annotations in the jar that comply with
 *    1. For example, the `@org.intellij.lang.annotations.PrintFormat` annotation uses a class (in
 *    this case defined in the same file), `org.intellij.lang.annotations.PrintFormatPattern`. The
 *    assumption here is that such classes (`PrintFormatPattern`) are only required during
 *    compilation, for their associated compile-only annotations.
 *
 * The algorithm for [isLintJar] is that the jar must meet these conditions:
 * 1. It must contain no classes (`analyzedClasses` is empty) AND
 * 2. It must contain an Android lint registry.
 * // TODO what about things like providing native libs, or only Android resources, or...?
 *
 * TODO unit tests for this class
 */
internal class AnalyzedJar(
  analyzedClasses: Set<AnalyzedClass>,
  val ktFiles: List<KtFile>,
  val androidLintRegistry: String?
) {

  /**
   * The set of classes provided by this jar. May be empty.
   */
  val classNames: Set<String> = analyzedClasses.mapToOrderedSet { it.className }

  /**
   * The set of security providers (classes that extend `java.security.Provider`) provided by this
   * jar. May be empty.
   */
  val securityProviders: Set<String> = analyzedClasses.filter {
    it.superClassName == "java/security/Provider"
  }.mapToOrderedSet { it.className }

  /**
   * Map of class names to the public constants they declare. May be empty.
   */
  val constants: Map<String, Set<String>> = analyzedClasses.map {
    it.className to it.constantFields
  }.toMap()

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
    } else {
      var value = true
      for (analyzedClass in analyzedClasses) {
        if (isNotCompileOnlyAnnotation(analyzedClass)) {
          // it is ok if it's not an annotation class, if it is a "namespace class".
          if (!isNamespaceClass(analyzedClass, analyzedClasses)) {
            // it's ok if it is not a namespace class, if it's private (non-public)
            if (isPublic(analyzedClass)) {
              // it's ok if it's public, if it's an enum
              if (!isEnum(analyzedClass)) {
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

  private fun RetentionPolicy?.isCompileOnly() = this == RetentionPolicy.CLASS || this == RetentionPolicy.SOURCE

  private fun isCompileOnlyAnnotation(analyzedClass: AnalyzedClass): Boolean =
    analyzedClass.retentionPolicy.isCompileOnly()

  private fun isNotCompileOnlyAnnotation(analyzedClass: AnalyzedClass): Boolean =
    !isCompileOnlyAnnotation(analyzedClass)

  private fun isNamespaceClass(analyzedClass: AnalyzedClass, analyzedClasses: Set<AnalyzedClass>): Boolean =
    analyzedClass.hasNoMembers && analyzedClasses
      .filter { analyzedClass.innerClasses.contains(it.className) }
      .reallyAll { isCompileOnlyAnnotation(it) }

  private fun isPublic(analyzedClass: AnalyzedClass): Boolean =
    analyzedClass.access == Access.PUBLIC || analyzedClass.access == Access.PROTECTED

  private fun isEnum(analyzedClass: AnalyzedClass): Boolean = analyzedClass.superClassName == "java/lang/Enum"
}
