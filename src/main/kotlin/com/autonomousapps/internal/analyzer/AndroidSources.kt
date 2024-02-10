// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.analyzer

import com.android.build.api.artifact.ScopedArtifact
import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.ScopedArtifacts
import com.android.build.api.variant.Sources
import com.autonomousapps.model.declaration.Variant
import com.autonomousapps.tasks.ClassListExploderTask
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import java.io.File

/**
 * All the relevant sources for a given Android variant, including Java, Kotlin, assets, res, manifest files, and
 * layouts.
 */
internal interface AndroidSources {
  val variant: Variant

  /** E.g., `debugCompileClasspath` or `debugUnitTestCompileClasspath` */
  val compileClasspathConfigurationName: String

  /** E.g., `debugRuntimeClasspath` or `debugUnitTestRuntimeClasspath` */
  val runtimeClasspathConfigurationName: String

  fun getJavaSources(): Provider<Iterable<File>>
  fun getKotlinSources(): Provider<Iterable<File>>
  fun getAndroidAssets(): Provider<Iterable<File>>
  fun getAndroidRes(): Provider<Iterable<File>>
  fun getManifestFiles(): Provider<Iterable<File>>
  fun getLayoutFiles(): Provider<Iterable<File>>
  fun wireWithClassFiles(task: TaskProvider<ClassListExploderTask>)
}

@Suppress("UnstableApiUsage")
internal class DefaultAndroidSources(
  private val project: Project,
  private val agpVariant: com.android.build.api.variant.Variant,
  private val sources: Sources,
  override val variant: Variant,
  override val compileClasspathConfigurationName: String,
  override val runtimeClasspathConfigurationName: String,
) : AndroidSources {

  override fun getJavaSources(): Provider<Iterable<File>> {
    return sources.kotlin?.all
      ?.map { directories ->
        directories.map { directory -> directory.asFileTree.matching(Language.filterOf(Language.JAVA)) }
      }?.map { trees -> trees.flatten() }
      ?: project.provider { emptyList() }
  }

  override fun getKotlinSources(): Provider<Iterable<File>> {
    return sources.kotlin?.all
      ?.map { directories ->
        directories.map { directory -> directory.asFileTree.matching(Language.filterOf(Language.KOTLIN)) }
      }?.map { trees -> trees.flatten() }
      ?: project.provider { emptyList() }
  }

  override fun getAndroidAssets(): Provider<Iterable<File>> {
    return sources.assets?.all
      ?.map { layers -> layers.flatten() }
      ?.map { directories -> directories.map { directory -> directory.asFileTree } }
      ?.map { trees -> trees.flatten() }
      ?: project.provider { emptyList() }
  }

  override fun getAndroidRes(): Provider<Iterable<File>> {
    return sources.res?.all
      ?.map { layers -> layers.flatten() }
      ?.map { directories ->
        directories.map { directory -> directory.asFileTree.matching(Language.filterOf(Language.XML)) }
      }
      ?.map { trees -> trees.flatten() }
      ?: project.provider { emptyList() }
  }

  override fun getLayoutFiles(): Provider<Iterable<File>> {
    return sources.res?.all
      ?.map { layers -> layers.flatten() }
      ?.map { directories -> directories.map { directory -> directory.asFileTree } }
      ?.map { fileTrees ->
        fileTrees.map { fileTree ->
          fileTree.matching {
            include("**/layout/**/*.xml")
          }
        }.flatten()
      }
      ?: project.provider { emptyList() }
  }

  override fun getManifestFiles(): Provider<Iterable<File>> {
    return agpVariant.artifacts.get(SingleArtifact.MERGED_MANIFEST).map {
      listOf(it.asFile)
    }
  }

  override fun wireWithClassFiles(task: TaskProvider<ClassListExploderTask>) {
    agpVariant.artifacts.forScope(ScopedArtifacts.Scope.PROJECT)
      .use(task)
      .toGet(
        type = ScopedArtifact.CLASSES,
        inputJars = ClassListExploderTask::jars,
        inputDirectories = ClassListExploderTask::dirs,
      )
  }
}
