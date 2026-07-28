// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP
import com.autonomousapps.internal.utils.VersionNumber
import com.autonomousapps.internal.utils.bufferWriteJsonMapSet
import com.autonomousapps.internal.utils.dependencyCoordinates
import com.autonomousapps.internal.utils.getAndDelete
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import java.util.*

@CacheableTask
public abstract class ComputeDuplicateDependenciesTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP
    description = "Computes 'duplicate' external dependencies across entire build."
  }

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  public abstract val resolvedDependenciesReports: ConfigurableFileCollection

  @get:OutputFile
  public abstract val output: RegularFileProperty

  @get:OutputFile
  public abstract val outputConsole: RegularFileProperty

  @TaskAction public fun action() {
    val output = output.getAndDelete()
    val outputConsole = outputConsole.getAndDelete()

    val map = sortedMapOf<String, SortedSet<String>>()

    resolvedDependenciesReports
      .dependencyCoordinates()
      .forEach {
        map.merge(it.identifier, sortedSetOf(it.resolvedVersion)) { acc, inc ->
          acc.apply { addAll(inc) }
        }
      }

    val consoleReport = buildConsoleReport(map)

    output.bufferWriteJsonMapSet(map)
    outputConsole.writeText(consoleReport)
  }

  private fun buildConsoleReport(report: Map<String, SortedSet<String>>): String {
    val total = report.size
    val sum = report.values.sumOf { it.size }
    val duplicates = report.filterTo(sortedMapOf()) { it.value.size > 1 }
    val duplicateCount = duplicates.size

    return buildString {
      append("Your build uses $sum dependencies, representing $total distinct 'libraries.' ")
      append("$duplicateCount libraries have multiple versions across the build.")
      if (duplicateCount == 0) {
        appendLine()
      } else {
        appendLine(" These are:")
        duplicates.forEach { (id, versions) ->
          appendLine("* $id:${versions.sortedVersions().joinToString(separator = ",", prefix = "{", postfix = "}")}")
        }
      }
    }
  }
}

// visible for testing
internal fun Iterable<String>.sortedVersions(): Iterable<String> = asSequence()
  .map { it to VersionNumber.parse(it) }
  .sortedBy { it.second }
  .map { it.first }
  .toList()
