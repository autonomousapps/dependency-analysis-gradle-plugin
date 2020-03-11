package com.autonomousapps.internal

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
internal class AnalyzedJar(private val analyzedClasses: Set<AnalyzedClass>) {

  fun classNames(): Set<String> = analyzedClasses.map { it.className }.toSortedSet()

  fun isCompileOnlyCandidate(): Boolean {
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
            // it's ok if it's public, if it's self-referencing
//                        if (isNotSelfReferencing(analyzedClass)) {
//                            return false
//                        }
          }
        }
      }
    }
    return true
  }

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

  // A class is self-referenced if is used by the set of classes. An imperfect test, but I suspect there's no perfect
  // one.
  private fun isNotSelfReferencing(analyzedClass: AnalyzedClass): Boolean {
    val allTypes = analyzedClasses.flatMap { it.methods }
        .flatMap { it.types }
        .toSet()
    return !allTypes.contains(analyzedClass.className.replace(".", "/"))
  }
}