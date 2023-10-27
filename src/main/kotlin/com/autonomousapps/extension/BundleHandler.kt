package com.autonomousapps.extension

import org.gradle.api.Named
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
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
abstract class BundleHandler @Inject constructor(
  private val name: String,
  objects: ObjectFactory,
) : Named {

  override fun getName(): String = name

  val primary: Property<String> = objects.property(String::class.java).convention("")
  val includes: SetProperty<Regex> = objects.setProperty<Regex>().convention(emptySet())

  fun primary(identifier: String) {
    primary.set(identifier)
    primary.disallowChanges()
  }

  fun primary(module: Provider<MinimalExternalModuleDependency>) {
    primary(module.identifier())
  }

  fun includeGroup(group: String) {
    include("^$group:.*")
  }

  fun includeGroup(module: Provider<MinimalExternalModuleDependency>) {
    includeGroup(module.group())
  }

  fun includeDependency(identifier: String) {
    include("^$identifier\$")
  }

  fun includeDependency(module: Provider<MinimalExternalModuleDependency>) {
    includeDependency(module.identifier())
  }

  fun include(@Language("RegExp") regex: String) {
    include(regex.toRegex())
  }

  fun include(regex: Regex) {
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
