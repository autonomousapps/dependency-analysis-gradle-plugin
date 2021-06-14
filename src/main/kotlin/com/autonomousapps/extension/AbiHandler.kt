@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package com.autonomousapps.extension

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.newInstance
import org.gradle.kotlin.dsl.setProperty
import org.intellij.lang.annotations.Language
import javax.inject.Inject

/**
 * ```
 * dependencyAnalysis {
 *   abi {
 *     exclusions {
 *       ignoreSubPackage("internal")
 *       ignoreInternalPackages()
 *       ignoreGeneratedCode()
 *       excludeAnnotations(".*\\.Generated")
 *       excludeClasses(".*\\.internal\\..*")
 *     }
 *   }
 * }
 * ```
 */
open class AbiHandler @Inject constructor(objects: ObjectFactory) {

  internal val exclusionsHandler: ExclusionsHandler = objects.newInstance(ExclusionsHandler::class)

  fun exclusions(action: Action<ExclusionsHandler>) {
    action.execute(exclusionsHandler)
  }
}

abstract class ExclusionsHandler @Inject constructor(objects: ObjectFactory) {

  internal val classExclusions = objects.setProperty<String>().convention(emptySet())
  internal val annotationExclusions = objects.setProperty<String>().convention(emptySet())
  internal val pathExclusions = objects.setProperty<String>().convention(emptySet())

  fun ignoreInternalPackages() {
    ignoreSubPackage("internal")
  }

  fun ignoreSubPackage(packageFragment: String) {
    excludeClasses("(.*\\.)?$packageFragment(\\..*)?")
  }

  /**
   * Best-effort attempts to ignore generated code by ignoring any bytecode in classes annotated
   * with an annotation ending in `Generated`. It's important to note that the standard
   * `javax.annotation.Generated` (or its JDK9+ successor) does _not_ work with this due to it
   * using `SOURCE` retention. It's recommended to use your own `Generated` annotation.
   */
  fun ignoreGeneratedCode() {
    excludeAnnotations(".*\\.Generated")
  }

  fun excludeClasses(@Language("RegExp") vararg classRegexes: String) {
    classExclusions.addAll(*classRegexes)
  }

  fun excludeAnnotations(@Language("RegExp") vararg annotationRegexes: String) {
    annotationExclusions.addAll(*annotationRegexes)
  }

  // TODO Excluded for now but left as a toe-hold for future use
//  fun excludePaths(@Language("RegExp") vararg pathRegexes: String) {
//    pathExclusions.addAll(*pathRegexes)
//  }
}