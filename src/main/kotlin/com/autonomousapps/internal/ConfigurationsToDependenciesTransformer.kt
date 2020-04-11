package com.autonomousapps.internal

import com.autonomousapps.internal.utils.capitalizeSafely
import com.autonomousapps.internal.utils.flatMapToSet
import com.autonomousapps.internal.utils.toIdentifiers
import org.gradle.api.Project

internal class ConfigurationsToDependenciesTransformer(
  private val variantName: String,
  private val project: Project
) {

  companion object {
    internal val DEFAULT_CONFS = listOf(
      "api", "implementation", "compile", "compileOnly", "runtimeOnly"
    )
    internal val DEFAULT_PROC_CONFS = listOf("kapt", "annotationProcessor")
  }

  fun dependencyConfigurations(): Set<DependencyConfiguration> {
    val candidateConfNames = DEFAULT_CONFS + DEFAULT_CONFS.map {
      // so, debugApi, etc.
      "${variantName}${it.capitalizeSafely()}"
    } + DEFAULT_PROC_CONFS + DEFAULT_PROC_CONFS.map {
      // so, kaptDebug, etc
      "${it}${variantName.capitalizeSafely()}"
    }

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
}
