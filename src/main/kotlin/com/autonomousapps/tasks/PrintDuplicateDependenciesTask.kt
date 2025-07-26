// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP
import com.autonomousapps.internal.utils.VersionNumber
import com.autonomousapps.internal.utils.fromJsonMapSet
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

public abstract class PrintDuplicateDependenciesTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP
    description = "Prints report of dependencies that have multiple versions across the build."
  }

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  public abstract val duplicateDependenciesReport: RegularFileProperty

  @TaskAction public fun action() {
    val report = duplicateDependenciesReport.fromJsonMapSet<String, String>()
    val total = report.size
    val sum = report.values.sumOf { it.size }
    val duplicates = report.filterTo(sortedMapOf()) { it.value.size > 1 }
    val duplicateCount = duplicates.size

    val output = buildString {
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

    logger.quiet(output)
  }
}

// visible for testing
internal fun Iterable<String>.sortedVersions(): Iterable<String> = asSequence()
  .map { it to VersionNumber.parse(it) }
  .sortedBy { it.second }
  .map { it.first }
  .toList()
