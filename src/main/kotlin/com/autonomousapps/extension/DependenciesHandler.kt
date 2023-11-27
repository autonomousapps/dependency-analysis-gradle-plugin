@file:Suppress("UnstableApiUsage")

package com.autonomousapps.extension

import com.autonomousapps.internal.matches
import com.autonomousapps.model.Coordinates
import org.gradle.api.*
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Input
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.setProperty
import java.io.Serializable
import javax.inject.Inject

/**
 * ```
 * dependencyAnalysis {
 *   structure {
 *     // Set the given map. Used when printing advice and rewriting build scripts.
 *     map.set(/* map */)
 *
 *     // put a single key: value pair into the map. Used when printing advice and rewriting build scripts.
 *     map.put(key, value)
 *
 *     // put all entries from the given map into the map. Used when printing advice and rewriting build scripts.
 *     map.putAll(/* map */)
 *
 *     // Set to true to instruct the plugin to not suggest replacing -ktx dependencies with non-ktx dependencies.
 *     ignoreKtx(<true|false>) // default: false
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
abstract class DependenciesHandler @Inject constructor(
  private val project: Project,
  objects: ObjectFactory,
) {

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

    withVersionCatalogs()
  }

  internal companion object {
    /** Transform [map] into lambda function. Returns requested key as value if key isn't present. */
    fun Map<String, String>.toLambda(): (String) -> String? = { s ->
      get(s)
    }
  }

  internal val ignoreKtx = objects.property<Boolean>().also {
    it.convention(false)
  }

  fun ignoreKtx(ignore: Boolean) {
    ignoreKtx.set(ignore)
    ignoreKtx.disallowChanges()
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

  private fun withVersionCatalogs() {
    val catalogs = project.extensions.findByType(VersionCatalogsExtension::class.java) ?: return

    catalogs.catalogNames.forEach { catalogName ->
      val catalog = catalogs.named(catalogName)
      val identifierMap = catalog.libraryAliases.associateBy { alias ->
        catalog.findLibrary(alias).get().get().module.toString()
      }
      map.putAll(identifierMap.mapValues { (_, identifier) -> "${catalog.name}.$identifier" })
    }
  }

  private fun wrapException(e: GradleException) = if (e is InvalidUserDataException)
    GradleException("You must configure this project either at the root or the project level, not both", e)
  else e

  internal fun serializableBundles(): SerializableBundles = SerializableBundles.of(bundles)

  class SerializableBundles(
    @get:Input val rules: Map<String, Set<Regex>>,
    @get:Input val primaries: Map<String, String>,
  ) : Serializable {

    /** Returns the collection of bundle rules that [coordinates] is a member of. (May be 0 or more.) */
    internal fun matchingBundles(coordinates: Coordinates): Map<String, Set<Regex>> {
      if (rules.isEmpty()) return emptyMap()

      return rules.filter { (_, regexes) ->
        regexes.any { regex ->
          coordinates.matches(regex)
        }
      }
    }

    companion object {
      internal fun of(
        bundles: NamedDomainObjectContainer<BundleHandler>,
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
}
