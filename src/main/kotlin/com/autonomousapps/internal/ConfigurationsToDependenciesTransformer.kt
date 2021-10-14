package com.autonomousapps.internal

import com.autonomousapps.internal.utils.*
import com.autonomousapps.tasks.LocateDependenciesTask
import org.gradle.api.artifacts.ConfigurationContainer

internal class ConfigurationsToDependenciesTransformer(
  private val flavorName: String?,
  private val buildType: String?,
  private val variantName: String,
  private val configurations: ConfigurationContainer,
  private val includeTest: Boolean
) {

  private val logger = getLogger<LocateDependenciesTask>()

  companion object {
    private val DEFAULT_MAIN_CONFS = listOf(
      "api",
      "implementation",
      // Deprecated, removed in Gradle 7
      "compile",
      "compileOnly",
      //"compileOnlyApi", // TODO
      "runtimeOnly"
    )
    private val DEFAULT_TEST_CONFS = listOf(
      "testRuntimeOnly",
      "testImplementation",
      "testCompileOnly"
    )
    private val DEFAULT_PROC_CONFS = listOf("kapt", "annotationProcessor")
  }

  fun locations(): Set<Location> {
    val candidateConfNames = buildConfNames() + buildAPConfNames()

    // Partition all configurations into those we care about and those we don't
    val (interestingConfs, otherConfs) = configurations.partition { conf ->
      candidateConfNames.contains(conf.name)
    }

    // TODO combine these into one sink
    val warnings = linkedMapOf<String, MutableSet<String>>()
    val metadataSink = mutableMapOf<String, Boolean>()

    // Get all the interesting confs
    val interestingLocations = interestingConfs.flatMapToMutableSet { conf ->
      conf.dependencies.toIdentifiers(metadataSink).map { identifier ->
        Location(
          identifier = identifier,
          configurationName = conf.name,
          isInteresting = true
        ).also {
          // Looking for dependencies stored on multiple configurations
          warnings.merge(it.identifier, mutableSetOf(it.configurationName)) { old, new ->
            old.apply { addAll(new) }
          }
        }
      }
    }
    // Get all the non-interesting confs, too
    val boringLocations = otherConfs.flatMapToSet { conf ->
      conf.dependencies.toIdentifiers(metadataSink).map { identifier ->
        Location(
          identifier = identifier,
          configurationName = conf.name,
          isInteresting = false
        )
      }
    }.filterToSet { boring ->
      // if a dependency is in both sets, prefer interestingLocations over boringLocations
      interestingLocations.none { interesting ->
        boring.identifier == interesting.identifier
      }
    }

    // Warn if dependency is declared on multiple configurations
    warnings.entries.forEach { (identifier, configurations) ->
      if (configurations.size > 1) {
        // Don't emit a warning if it's for a java-platform project. These can be declared on
        // multiple configurations.
        if (metadataSink[identifier] != true) {
          logger.warn("Dependency $identifier has been declared multiple times: $configurations")
        }

        // if one of the declarations is for an api configuration. Prefer that one
        if (configurations.any { it.endsWith("api", true) }) {
          interestingLocations.removeIf {
            it.identifier == identifier && !it.configurationName.endsWith("api", true)
          }
        }
      }
    }

    return interestingLocations + boringLocations
  }

  private fun buildConfNames(): Set<String> {
    var defaultConfigurations = DEFAULT_MAIN_CONFS
    if (includeTest) {
      defaultConfigurations = defaultConfigurations + DEFAULT_TEST_CONFS
    }

    val confNames = (defaultConfigurations + defaultConfigurations.map {
      // so, flavorDebugApi, etc.
      "${variantName}${it.capitalizeSafely()}"
    }).toMutableSet()

    if (flavorName != null) {
      confNames.addAll(defaultConfigurations.map {
        // so, flavorApi, etc.
        "${flavorName}${it.capitalizeSafely()}"
      })
    }

    if (buildType != null) {
      confNames.addAll(defaultConfigurations.map {
        // so, buildTypeApi, etc.
        "${buildType}${it.capitalizeSafely()}"
      })
    }

    return confNames
  }

  private fun buildAPConfNames(): Set<String> {
    val procConfNames = (DEFAULT_PROC_CONFS + DEFAULT_PROC_CONFS.map {
      // so, kaptFlavorDebug, etc
      "${it}${variantName.capitalizeSafely()}"
    }).toMutableSet()
    if (flavorName != null) {
      procConfNames.addAll(DEFAULT_PROC_CONFS.map {
        // so, kaptFlavor, etc.
        "${it}${flavorName.capitalizeSafely()}"
      })
    }
    return procConfNames
  }
}
