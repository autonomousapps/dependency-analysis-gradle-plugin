package com.autonomousapps.internal

import com.autonomousapps.internal.utils.capitalizeSafely
import com.autonomousapps.internal.utils.flatMapToMutableSet
import com.autonomousapps.internal.utils.getLogger
import com.autonomousapps.internal.utils.toIdentifiers
import com.autonomousapps.tasks.LocateDependenciesTask
import org.gradle.api.Project

internal class ConfigurationsToDependenciesTransformer(
  private val flavorName: String?,
  private val variantName: String,
  private val project: Project
) {

  private val logger = getLogger<LocateDependenciesTask>()

  companion object {
    private val DEFAULT_CONFS = listOf(
      "api", "implementation", "compile", "compileOnly", "runtimeOnly"
    )
    private val DEFAULT_PROC_CONFS = listOf("kapt", "annotationProcessor")
  }

  fun dependencyConfigurations(): Set<DependencyConfiguration> {
    val candidateConfNames = buildConfNames() + buildAPConfNames()

    // Filter all configurations for those we care about
    val interestingConfs = project.configurations.asMap
      .filter { (name, _) -> candidateConfNames.contains(name) }
      .map { (_, conf) -> conf }

    val warnings = linkedMapOf<String, MutableSet<String>>()

    val metadataSink = mutableMapOf<String, Boolean>()
    val locations = interestingConfs.flatMapToMutableSet { conf ->
      conf.dependencies.toIdentifiers(metadataSink).map { identifier ->
        DependencyConfiguration(identifier = identifier, configurationName = conf.name).also {
          // Looking for dependencies stored on multiple configurations
          warnings.merge(it.identifier, mutableSetOf(it.configurationName)) { old, new ->
            old.apply { addAll(new) }
          }
        }
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

        // one of the declarations is for an api configuration. Prefer that one
        if (configurations.any { it.endsWith("api", true) }) {
          locations.removeIf {
            it.identifier == identifier && !it.configurationName.endsWith("api", true)
          }
        }
      }
    }

    return locations
  }

  private fun buildConfNames(): Set<String> {
    val confNames = (DEFAULT_CONFS + DEFAULT_CONFS.map {
      // so, flavorDebugApi, etc.
      "${variantName}${it.capitalizeSafely()}"
    }).toMutableSet()
    if (flavorName != null) {
      confNames.addAll(DEFAULT_CONFS.map {
        // so, flavorApi, etc.
        "${flavorName}${it.capitalizeSafely()}"
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
