// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.internal.utils.dependencyCoordinates
import com.autonomousapps.internal.utils.mapToOrderedSet
import com.autonomousapps.model.ModuleCoordinates
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class ComputeAllDependenciesTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP
    description = "Generates a version catalog file (allLibs.versions.toml) containing all dependencies in the project."
  }

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  abstract val resolvedDependenciesReports: ConfigurableFileCollection

  @get:OutputFile
  abstract val output: RegularFileProperty

  @TaskAction
  fun action() {
    val outputFile = output.getAndDelete()

    val libs: Set<String> = resolvedDependenciesReports
      .dependencyCoordinates()
      .mapToOrderedSet { "${it.toVersionCatalogAlias()} = { module = \"${it.identifier}\", version = \"${it.resolvedVersion}\" }" }

    val tomlContent = buildString {
      appendLine("[libraries]")
      libs.forEach { appendLine(it) }
    }

    outputFile.writeText(tomlContent)

    logger.quiet("Generated version catalog for all dependencies, containing ${libs.size} entries:\n${outputFile.absolutePath} ")
  }

  private fun ModuleCoordinates.toVersionCatalogAlias(): String {
    return "${identifier}-${resolvedVersion}"
      .split(':', '.')
      // replace reserved keywords with safe alternatives
      .joinToString(separator = "-") { tomlReservedKeywordMappings.getOrDefault(it, it) }
      .lowercase()
  }

  private companion object {
    private val tomlReservedKeywordMappings = mapOf(
      "extensions" to "extensionz",
      "class" to "clazz",
      "convention" to "convencion",
      "bundles" to "bundlez",
      "versions" to "versionz",
      "plugins" to "pluginz",
    )
  }
}
