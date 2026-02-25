// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps

import com.autonomousapps.internal.kotlin.multiplatform.FileCollectionMap
import com.autonomousapps.internal.kotlin.multiplatform.KotlinCommonSources
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * This task is currently unused but I think it could be interesting. It will list all the source files in the
 * `commonMain` and `commonTest` source sets.
 */
@DisableCachingByDefault(because = "Writes to console")
public abstract class ListSourceFilesTask : DefaultTask() {

  internal companion object {
    fun register(
      project: Project,
      kotlin: KotlinMultiplatformExtension,
    ): TaskProvider<ListSourceFilesTask> {
      return project.tasks.register("listAllCommonSourceFiles", ListSourceFilesTask::class.java) { t ->
        t.files.set(project.provider(KotlinCommonSources.all(kotlin)))
      }
    }
  }

  @get:Nested
  public abstract val files: ListProperty<FileCollectionMap>

  @TaskAction public fun action() {
    files.get().forEach { (name, fileCollection) ->
      val files = fileCollection.asFileTree.files.joinToString(System.lineSeparator()) { f -> "- $f" }
      logger.quiet("$name:\n$files")
    }
  }
}
