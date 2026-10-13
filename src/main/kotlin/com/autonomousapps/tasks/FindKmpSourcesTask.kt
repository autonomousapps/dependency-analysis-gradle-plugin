// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.tasks

import com.autonomousapps.internal.NoVariantOutputPaths
import com.autonomousapps.internal.kotlin.multiplatform.FileCollectionMap
import com.autonomousapps.internal.kotlin.multiplatform.KotlinSources
import com.autonomousapps.internal.utils.bufferWriteJsonSet
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.model.internal.kmp.SourceSetFiles
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import java.io.File

/**
 * Finds all KMP source files, including those in non-standard directories and generated source. This information is
 * used by [ComputeUsagesTask] to bridge the gap between the compilation-based analysis and the source set-based advice.
 */
@CacheableTask
public abstract class FindKmpSourcesTask : DefaultTask() {

  internal companion object {
    fun register(
      project: Project,
      kotlin: KotlinMultiplatformExtension,
    ): TaskProvider<FindKmpSourcesTask> {
      return project.tasks.register("findKmpSources", FindKmpSourcesTask::class.java) { t ->
        t.projectDir.set(project.layout.projectDirectory)

        val sources = KotlinSources.all(kotlin)
        t.names.set(project.provider(sources.names()))
        t.sourceDirectories.set(project.provider(sources.sourceDirectories()))
        // TODO(tsr): support for generated source dirs requires at least KGP 2.3 I think
        t.generatedSourceDirectories.set(project.provider(sources.generatedSourceDirectories()))
        t.output.set(NoVariantOutputPaths(project).findKmpSourcesPath)
      }
    }
  }

  init {
    description =
      "Write a report of all commonMain and commonTest source files in KMP projects, including generated files"
  }

  @get:Internal
  public abstract val projectDir: DirectoryProperty

  @get:Input
  public abstract val names: ListProperty<String>

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  public abstract val sourceDirectories: ListProperty<FileCollection>

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  public abstract val generatedSourceDirectories: ListProperty<FileCollection>

  @get:OutputFile
  public abstract val output: RegularFileProperty

  @TaskAction public fun action() {
    val output = output.getAndDelete()

    val base = projectDir.get().asFile
    val sources = buildSourceSetFiles(base)

    output.bufferWriteJsonSet(sources)
  }

  private fun buildSourceSetFiles(base: File): Set<SourceSetFiles> {
    val sources = buildSourceSetFiles(base, sourceDirectories)
    val generatedSources = buildSourceSetFiles(base, generatedSourceDirectories)

    return sortedSetOf<SourceSetFiles>().apply {
      addAll(sources)
      addAll(generatedSources)
    }
  }

  private fun buildSourceSetFiles(base: File, files: ListProperty<FileCollection>): Set<SourceSetFiles> {
    return FileCollectionMap.sequenced(names, files)
      // `dirs` is a FileCollection where each element is the root directory of a source tree, such as
      // `src/commonMain/kotlin`. We want to preserve the source root and, essentially, associate that root with the set
      // of regular source files contained within. Those regular source files will include their full package and file
      // extension, like `com/foo/Bar.kt`.
      .flatMap { (name, dirs) ->
        dirs.map { dir ->
          val sourceFiles = dir
            .walkTopDown()
            .filter { f -> f.isFile }
            .map { f -> f.toRelativeString(dir) }
            .toSortedSet()

          if (sourceFiles.isEmpty()) {
            // Ignore source directories with no regular files
            null
          } else {
            SourceSetFiles(
              sourceSetName = name,
              sourceBase = dir.toRelativeString(base),
              sourceFiles = sourceFiles,
            )
          }
        }
      }
      .filterNotNull()
      .toSortedSet()
  }
}
