package com.autonomousapps.internal

import org.gradle.api.Project

internal class ConfigurationsToDependenciesTransformer(
    private val variantName: String,
    private val project: Project
) {

  companion object {
    internal val DEFAULT_CONFS = listOf("api", "implementation", "compile", "compileOnly", "runtimeOnly")
  }

  fun dependencyConfigurations(): Set<DependencyConfiguration> {
    val candidateConfNames = DEFAULT_CONFS + DEFAULT_CONFS.map {
      "${variantName}${it.capitalize()}"
    }
    // Filter all configurations for those we care about
    val interestingConfs = project.configurations.asMap
        .filter { (name, _) -> candidateConfNames.contains(name) }
        .map { (_, conf) -> conf }

    return interestingConfs.flatMap { conf ->
      conf.dependencies.toIdentifiers().map { identifier ->
        DependencyConfiguration(identifier = identifier, configurationName = conf.name)
      }
    }.toSet()
  }
}
