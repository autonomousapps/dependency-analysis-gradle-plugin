// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package com.autonomousapps.extension

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.newInstance
import org.gradle.kotlin.dsl.setProperty
import org.intellij.lang.annotations.Language
import javax.inject.Inject

/**
 * Configures various elements of ABI analysis, such as excluding source sets, packages, generated code, etc.
 *
 * ```
 * dependencyAnalysis {
 *   abi {
 *     exclusions {
 *       excludeSourceSets(/* source sets to exclude from ABI analysis */)
 *
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
public abstract class AbiHandler @Inject constructor(objects: ObjectFactory) {

  internal val exclusionsHandler: ExclusionsHandler = objects.newInstance()

  public fun exclusions(action: Action<ExclusionsHandler>) {
    action.execute(exclusionsHandler)
  }
}

/** @see [AbiHandler]. */
public abstract class ExclusionsHandler @Inject constructor(objects: ObjectFactory) {

  internal val classExclusions = objects.setProperty<String>().convention(emptySet())
  internal val annotationExclusions = objects.setProperty<String>().convention(emptySet())
  internal val pathExclusions = objects.setProperty<String>().convention(emptySet())
  internal val excludedSourceSets = objects.setProperty<String>().convention(emptySet())

  /**
   * Exclude the given [sourceSets] from ABI analysis, which means that regardless of the level of exposure of any given
   * symbol, all dependencies for these source sets are considered to be "implementation" details.
   */
  public fun excludeSourceSets(vararg sourceSets: String) {
    excludedSourceSets.addAll(*sourceSets)
  }

  /**
   * Exclude "internal" packages from ABI analysis, which means that any class in a package that contains ".internal."
   * will not be considered as part of the module's ABI.
   */
  public fun ignoreInternalPackages() {
    ignoreSubPackage("internal")
  }

  /**
   * Exclude given sub-packages from ABI analysis, which means that any class in a package that contains
   * ".[packageFragment]." will not be considered as part of the module's ABI.
   */
  public fun ignoreSubPackage(packageFragment: String) {
    excludeClasses("(.*\\.)?$packageFragment(\\..*)?")
  }

  /**
   * Best-effort attempts to ignore generated code by ignoring any bytecode in classes annotated
   * with an annotation ending in `Generated`. It's important to note that the standard
   * `javax.annotation.Generated` (or its JDK9+ successor) does _not_ work with this due to it
   * using `SOURCE` retention. It's recommended to use your own `Generated` annotation.
   */
  public fun ignoreGeneratedCode() {
    excludeAnnotations(".*\\.Generated")
  }

  /**
   * Exclude given classes from ABI analysis, which means that any class that matches the given [classRegexes] regex
   * will not be considered as part of the module's ABI.
   */
  public fun excludeClasses(@Language("RegExp") vararg classRegexes: String) {
    classExclusions.addAll(*classRegexes)
  }

  /**
   * Exclude any class from ABI analysis that is annotated with annotations that match [annotationRegexes], which means
   * those classes will not be considered as part of the module's ABI.
   */
  public fun excludeAnnotations(@Language("RegExp") vararg annotationRegexes: String) {
    annotationExclusions.addAll(*annotationRegexes)
  }

  // TODO Excluded for now but left as a toe-hold for future use
//  fun excludePaths(@Language("RegExp") vararg pathRegexes: String) {
//    pathExclusions.addAll(*pathRegexes)
//  }
}
