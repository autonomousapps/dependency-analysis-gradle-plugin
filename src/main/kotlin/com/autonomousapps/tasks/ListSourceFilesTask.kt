// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.tasks

import com.autonomousapps.internal.kotlin.multiplatform.FileCollectionMap
import com.autonomousapps.internal.kotlin.multiplatform.KotlinCommonSources
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * This task is currently unused but I think it could be interesting. It will list all the source files in the
 * `commonMain` and `commonTest` source sets. TODO
 */
@DisableCachingByDefault(because = "Writes to console")
public abstract class ListSourceFilesTask : DefaultTask() {

  internal companion object {
    fun register(
      project: Project,
      kotlin: KotlinMultiplatformExtension,
    ): TaskProvider<ListSourceFilesTask> {
      return project.tasks.register("listAllCommonSourceFiles", ListSourceFilesTask::class.java) { t ->
        t.names.set(project.provider(KotlinCommonSources.all(kotlin).names()))
        t.files.set(project.provider(KotlinCommonSources.all(kotlin).files()))
      }
    }
  }

  @get:Input
  public abstract val names: ListProperty<String>

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  public abstract val files: ListProperty<FileCollection>

  // TODO: generate code into commonMain and check what the path looks like
  //  files.files is a single file if there's nothing generated. What's it look like when there is generated source?
  @TaskAction public fun action() {
    FileCollectionMap.sequenced(names, files)
      // TODO: do I need the relative path of these files?
      .map { (names, files) -> names to files.asFileTree.files }
      .filterNot { (_, files) -> files.isEmpty() }
      .forEach { (name, files) ->
        val files = files.joinToString(System.lineSeparator()) { f -> "- $f" }
        logger.quiet("$name:\n$files")
      }
  }
}
