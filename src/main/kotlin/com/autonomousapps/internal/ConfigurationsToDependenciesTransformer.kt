package com.autonomousapps.internal

import com.autonomousapps.internal.utils.capitalizeSafely
import com.autonomousapps.internal.utils.flatMapToSet
import com.autonomousapps.internal.utils.toIdentifiers
import org.gradle.api.Project

internal class ConfigurationsToDependenciesTransformer(
  private val flavorName: String?,
  private val variantName: String,
  private val project: Project
) {

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

    return interestingConfs.flatMapToSet { conf ->
      conf.dependencies.toIdentifiers().map { identifier ->
        DependencyConfiguration(identifier = identifier, configurationName = conf.name)
      }
    }
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
