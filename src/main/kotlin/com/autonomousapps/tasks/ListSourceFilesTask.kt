// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.tasks

import com.autonomousapps.internal.NoVariantOutputPaths
import com.autonomousapps.internal.kotlin.multiplatform.FileCollectionMap
import com.autonomousapps.internal.kotlin.multiplatform.KotlinCommonSources
import com.autonomousapps.internal.utils.bufferWriteJsonSet
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.model.internal.kmp.SourceMap
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import java.io.File

/**
 * TODO rename task, update kdoc
 *
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
        t.projectDir.set(project.layout.projectDirectory)
        t.names.set(project.provider(KotlinCommonSources.all(kotlin).names()))
        t.sourceDirectories.set(project.provider(KotlinCommonSources.all(kotlin).sourceDirectories()))
        t.generatedSourceDirectories.set(project.provider(KotlinCommonSources.all(kotlin).generatedSourceDirectories()))
        t.output.set(NoVariantOutputPaths(project).commonSourcesPath)
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
    val sources = buildSourceMaps(base)

    output.bufferWriteJsonSet(sources)
  }

  private fun buildSourceMaps(base: File): Set<SourceMap> {
    val sources = buildSourceMap(base, sourceDirectories)
    val generatedSources = buildSourceMap(base, generatedSourceDirectories)

    return sortedSetOf<SourceMap>().apply {
      addAll(sources)
      addAll(generatedSources)
    }
  }

  private fun buildSourceMap(base: File, files: ListProperty<FileCollection>): Set<SourceMap> {
    return FileCollectionMap.sequenced(names, files)
      // Ignore source directories with no regular files
      .filterNot { (_, dirs) -> dirs.asFileTree.files.isEmpty() }
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

          SourceMap(
            sourceSetName = name,
            sourceBase = dir.toRelativeString(base),
            sourceFiles = sourceFiles,
          )
        }
      }
      .toSortedSet()
  }
}
