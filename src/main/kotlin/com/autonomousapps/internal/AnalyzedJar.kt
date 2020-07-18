package com.autonomousapps.internal

import com.autonomousapps.internal.utils.flatMapToOrderedSet
import com.autonomousapps.internal.utils.mapToOrderedSet
import com.autonomousapps.internal.utils.reallyAll
import java.lang.annotation.RetentionPolicy

/**
 * Algorithm:
 *
 * Jar must _only_ contain:
 * 1. Annotation classes with `CLASS` or `SOURCE` retention policies (or no policy => `CLASS` is default).
 * 2. The above, plus types that only exist to provide a sort of "namespace". For example,
 *    `org.jetbrains.annotations.ApiStatus` has no members. It only has inner classes which are themselves
 *     annotations that comply with 1.
 * 3. All of the above, plus types that are only used by the annotations in the jar that comply with 1. For example, the
 *    `@org.intellij.lang.annotations.PrintFormat` annotation uses a class (in this case defined in the same file),
 *    `org.intellij.lang.annotations.PrintFormatPattern`. The assumption here is that such classes
 *    (`PrintFormatPattern`) are only required during compilation, for their associated compile-only annotations.
 *    // TODO unit tests for this class
 */
internal class AnalyzedJar(
  private val analyzedClasses: Set<AnalyzedClass>,
  val ktFiles: List<KtFile>
) {

  val classNames: Set<String> = analyzedClasses.mapToOrderedSet { it.className }

  val isCompileOnlyCandidate: Boolean
    get() {
      if (analyzedClasses.isEmpty()) {
        return false
      }

      for (analyzedClass in analyzedClasses) {
        if (isNotCompileOnlyAnnotation(analyzedClass)) {
          // it is ok if it's not an annotation class, if it is a "namespace class".
          if (!isNamespaceClass(analyzedClass)) {
            // it's ok if it is not a namespace class, if it's private (non-public)
            if (isPublic(analyzedClass)) {
              // it's ok if it's public, if it's an enum
              if (!isEnum(analyzedClass)) {
                return false
              }
            }
          }
        }
      }
      return true
    }

  val isSecurityProvider: Boolean = analyzedClasses.any { it.superClassName == "java/security/Provider" }

  /**
   * Map of class names to the public constants they declare.
   */
  val constants = analyzedClasses.map {
    it.className to it.constantFields
  }.toMap()

  private fun RetentionPolicy?.isCompileOnly() = this == RetentionPolicy.CLASS || this == RetentionPolicy.SOURCE

  private fun isCompileOnlyAnnotation(analyzedClass: AnalyzedClass): Boolean =
    analyzedClass.retentionPolicy.isCompileOnly()

  private fun isNotCompileOnlyAnnotation(analyzedClass: AnalyzedClass): Boolean =
    !isCompileOnlyAnnotation(analyzedClass)

  private fun isNamespaceClass(analyzedClass: AnalyzedClass): Boolean =
    analyzedClass.hasNoMembers && analyzedClasses
      .filter { analyzedClass.innerClasses.contains(it.className) }
      .reallyAll { isCompileOnlyAnnotation(it) }

  private fun isPublic(analyzedClass: AnalyzedClass): Boolean =
    analyzedClass.access == Access.PUBLIC || analyzedClass.access == Access.PROTECTED

  private fun isEnum(analyzedClass: AnalyzedClass): Boolean = analyzedClass.superClassName == "java/lang/Enum"
}
