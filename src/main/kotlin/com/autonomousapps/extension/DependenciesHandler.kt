@file:Suppress("UnstableApiUsage")

package com.autonomousapps.extension

import org.gradle.api.*
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.SetProperty
import org.gradle.kotlin.dsl.setProperty
import org.intellij.lang.annotations.Language
import javax.inject.Inject

/**
 * ```
 * dependencyAnalysis {
 *   dependencies {
 *     bundle("kotlin-stdlib") {
 *       // 1: include all in group as a single logical dependency
 *       includeGroup("org.jetbrains.kotlin")
 *
 *       // 2: include all supplied dependencies as a single logical dependency
 *       includeDependency("org.jetbrains.kotlin:kotlin-stdlib")
 *       includeDependency("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
 *
 *       // 3: include all dependencies that match the regex as a single logical dependency
 *       include(".*kotlin-stdlib.*")
 *     }
 *   }
 * }
 * ```
 */
open class DependenciesHandler @Inject constructor(objects: ObjectFactory) {

  val bundles: NamedDomainObjectContainer<BundleHandler> = objects.domainObjectContainer(BundleHandler::class.java)

  fun bundle(name: String, action: Action<BundleHandler>) {
    try {
      bundles.create(name) {
        action.execute(this)
      }
    } catch (e: GradleException) {
      throw wrapException(e)
    }
  }

  private fun wrapException(e: GradleException) = if (e is InvalidUserDataException)
    GradleException("You must configure this project either at the root or the project level, not both", e)
  else e
}

/**
 * ```
 * bundle("kotlin-stdlib") {
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
open class BundleHandler @Inject constructor(
  private val name: String,
  objects: ObjectFactory
) : Named {

  override fun getName(): String = name

  val includes: SetProperty<Regex> = objects.setProperty<Regex>().convention(emptySet())

  fun includeGroup(group: String) {
    include("^$group:.*")
  }

  fun includeDependency(identifier: String) {
    include("^$identifier\$")
  }

  fun include(@Language("RegExp") regex: String) {
    include(regex.toRegex())
  }

  fun include(regex: Regex) {
    includes.add(regex)
  }
}
