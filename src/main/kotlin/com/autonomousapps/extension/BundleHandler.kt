// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.extension

import org.gradle.api.Named
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderConvertible
import org.gradle.api.provider.SetProperty
import org.gradle.kotlin.dsl.setProperty
import org.intellij.lang.annotations.Language
import javax.inject.Inject

/**
 * ```
 * bundle("kotlin-stdlib") {
 *   // 0 (Optional): Specify the primary entry point that the user is "supposed" to declare.
 *   primary("org.something:primary-entry-point")
 *
 *   // 1: include all in group as a single logical dependency
 *   includeGroup("org.jetbrains.kotlin")
 *
 *   // 2: include all supplied dependencies as a single logical dependency
 *   includeDependency("org.jetbrains.kotlin:kotlin-stdlib")
 *   includeDependency("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
 *
 *   // 3: include all dependencies that match the regex as a single logical dependency
 *   include(".*kotlin-stdlib.*")
 * }
 * ```
 */
public abstract class BundleHandler @Inject constructor(
  private val name: String,
  objects: ObjectFactory,
) : Named {

  /**
   * The unique name for this bundle.
   */
  override fun getName(): String = name

  /**
   * (Optional): Specify the primary entry point that the user is "supposed" to declare.
   */
  public val primary: Property<String> = objects.property(String::class.java).convention("")

  /**
   * Include all dependencies that match the regex as a single logical dependency.
   */
  public val includes: SetProperty<Regex> = objects.setProperty<Regex>().convention(emptySet())

  /**
   * (Optional): Specify the primary entry point that the user is "supposed" to declare.
   */
  public fun primary(identifier: String) {
    primary.set(identifier)
    primary.disallowChanges()

    includeDependency(identifier)
  }

  /**
   * (Optional): Specify the primary entry point that the user is "supposed" to declare.
   */
  public fun primary(module: Provider<MinimalExternalModuleDependency>) {
    primary(module.identifier())
  }

  /**
   * (Optional): Specify the primary entry point that the user is "supposed" to declare.
   */
  public fun primary(module: ProviderConvertible<MinimalExternalModuleDependency>) {
    primary(module.asProvider())
  }

  /**
   * Include all in group as a single logical dependency.
   */
  public fun includeGroup(group: String) {
    include("^$group:.*")
  }

  /**
   * Include all in group as a single logical dependency.
   */
  public fun includeGroup(module: Provider<MinimalExternalModuleDependency>) {
    includeGroup(module.group())
  }

  /**
   * Include all in group as a single logical dependency.
   */
  public fun includeGroup(module: ProviderConvertible<MinimalExternalModuleDependency>) {
    includeGroup(module.asProvider())
  }

  /**
   * Include all supplied dependencies as a single logical dependency.
   */
  public fun includeDependency(identifier: String) {
    include("^$identifier\$")

    // Hacky way to handle implicit KMP bundles. This is why `TestDependenciesSpec.bundles work for test dependencies`
    // passes.
    if (!identifier.endsWith("-jvm")) {
      include("^$identifier-jvm\$")
    }
    if (!identifier.endsWith("-android")) {
      include("^$identifier-android\$")
    }
  }

  /**
   * Include all supplied dependencies as a single logical dependency.
   */
  public fun includeDependency(module: Provider<MinimalExternalModuleDependency>) {
    includeDependency(module.identifier())
  }

  /**
   * Include all supplied dependencies as a single logical dependency.
   */
  public fun includeDependency(module: ProviderConvertible<MinimalExternalModuleDependency>) {
    includeDependency(module.asProvider())
  }

  /**
   * Include all dependencies that match the regex as a single logical dependency.
   */
  public fun include(@Language("RegExp") regex: String) {
    include(regex.toRegex())
  }

  /**
   * Include all dependencies that match the regex as a single logical dependency.
   */
  public fun include(regex: Regex) {
    includes.add(regex)
  }

  private fun Provider<MinimalExternalModuleDependency>.identifier(): String {
    return map { "${it.group}:${it.name}" }.get()
  }

  private fun Provider<MinimalExternalModuleDependency>.group(): String {
    return map {
      // group is in fact @Nullable
      @Suppress("USELESS_ELVIS")
      it.group ?: error("No group for $it")
    }.get()
  }
}
