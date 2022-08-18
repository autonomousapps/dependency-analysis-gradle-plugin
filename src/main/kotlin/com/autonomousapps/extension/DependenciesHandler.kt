@file:Suppress("UnstableApiUsage")

package com.autonomousapps.extension

import com.autonomousapps.model.Coordinates
import org.gradle.api.*
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.kotlin.dsl.setProperty
import org.intellij.lang.annotations.Language
import java.io.Serializable
import javax.inject.Inject

/**
 * ```
 * dependencyAnalysis {
 *   dependencies {
 *     // Set the given map. Used when printing advice and rewriting build scripts.
 *     map.set(/* map */)
 *
 *     // put a single key: value pair into the map. Used when printing advice and rewriting build scripts.
 *     map.put(key, value)
 *
 *     // put all entries from the given map into the map. Used when printing advice and rewriting build scripts.
 *     map.putAll(/* map */)
 *
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
@Suppress("HasPlatformType")
open class DependenciesHandler @Inject constructor(objects: ObjectFactory) {

  val map = objects.mapProperty(String::class.java, String::class.java).convention(mutableMapOf())
  val bundles = objects.domainObjectContainer(BundleHandler::class.java)

  init {
    // With Kotlin plugin 1.4, the stdlib is now applied by default. It makes no sense to warn users
    // about this, even if it is "incorrect." So make all stdlib-related libraries a bundle and call
    // it a day.
    bundle("__kotlin-stdlib") {
      include(".*kotlin-stdlib.*")
    }
    // Firebase / Google services are tightly coupled
    bundle("__firebase") {
      includeGroup("com.google.firebase")
      includeGroup("com.google.android.gms")
    }
  }

  companion object {
    /** Transform [map] into lambda function. Returns requested key as value if key isn't present. */
    internal fun Map<String, String>.toLambda(): (String) -> String = { s ->
      getOrDefault(s, s)
    }
  }

  fun bundle(name: String, action: Action<BundleHandler>) {
    try {
      bundles.create(name) {
        action.execute(this)
      }
    } catch (e: GradleException) {
      throw wrapException(e)
    }
  }

  internal fun serializableBundles(): SerializableBundles = SerializableBundles.of(bundles)

  class SerializableBundles(
    @get:Input val rules: Map<String, Set<Regex>>,
    @get:Input val primaries: Map<String, String>
  ) : Serializable {

    /** Returns the collection of bundle rules that [coordinates] is a member of. (May be 0 or more.) */
    internal fun matchingBundles(coordinates: Coordinates): Map<String, Set<Regex>> {
      if (rules.isEmpty()) return emptyMap()

      return rules.filter { (_, regexes) ->
        regexes.any { regex ->
          regex.matches(coordinates.identifier)
        }
      }
    }

    companion object {
      internal fun of(
        bundles: NamedDomainObjectContainer<BundleHandler>
      ): SerializableBundles {
        val rules = mutableMapOf<String, Set<Regex>>()
        val primaries = mutableMapOf<String, String>()
        bundles.asMap.map { (name, groups) ->
          rules[name] = groups.includes.get()

          val primary = groups.primary.get()
          if (primary.isNotEmpty()) {
            primaries[name] = primary
          }
        }

        return SerializableBundles(rules, primaries)
      }
    }
  }

  private fun wrapException(e: GradleException) = if (e is InvalidUserDataException)
    GradleException("You must configure this project either at the root or the project level, not both", e)
  else e
}

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
open class BundleHandler @Inject constructor(
  private val name: String,
  objects: ObjectFactory
) : Named {

  override fun getName(): String = name

  val primary: Property<String> = objects.property(String::class.java).convention("")
  val includes: SetProperty<Regex> = objects.setProperty<Regex>().convention(emptySet())

  fun primary(identifier: String) {
    primary.set(identifier)
    primary.disallowChanges()
  }

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
