// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.analyzer

import com.autonomousapps.internal.ArtifactAttributes
import com.autonomousapps.internal.KotlinMetadataClasspath
import com.autonomousapps.internal.OutputPaths
import com.autonomousapps.internal.resolvedArtifactsFor
import com.autonomousapps.internal.utils.capitalizeSafely
import com.autonomousapps.model.source.SourceKind
import com.autonomousapps.tasks.*
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation

internal class KmpProjectAnalyzer(
  project: Project,
  private val compilation: KotlinCompilation<*>,
  private val sourceSet: KmpSourceSet,
  private val findKmpSourcesTask: TaskProvider<FindKmpSourcesTask>,
  private val hasAbi: Boolean,
) : AbstractDependencyAnalyzer(project, findKmpSourcesTask) {

  override val flavorName: String? = null
  override val buildType: String? = null
  override val sourceKind: SourceKind = sourceSet.sourceKind
  override val variantName: String = sourceSet.name
  override val taskNameSuffix: String = variantName.capitalizeSafely()

  override val compileConfigurationName = sourceSet.compileClasspathConfigurationName
  override val runtimeConfigurationName = sourceSet.runtimeClasspathConfigurationName
  override val kaptConfigurationName = "kapt"
  override val annotationProcessorConfigurationName = "annotationProcessor"

  override val attributeValueJar = "jar"

  override val isDataBindingEnabled: Provider<Boolean> = project.provider { false }
  override val isViewBindingEnabled: Provider<Boolean> = project.provider { false }

  override val outputPaths = OutputPaths(project, variantName)

  override fun registerByteCodeSourceExploderTask(): TaskProvider<ClassListExploderTask> {
    return project.tasks.register("explodeByteCodeSource$taskNameSuffix", ClassListExploderTask::class.java) { t ->
      t.classes.setFrom(sourceSet.classesDirs)
      t.kmpSources.set(findKmpSourcesTask.flatMap { it.output })
      t.output.set(outputPaths.explodingBytecodePath)
    }
  }

  override fun registerCodeSourceExploderTask(): TaskProvider<out CodeSourceExploderTask> {
    return project.tasks.register("explodeCodeSource$taskNameSuffix", JvmCodeSourceExploderTask::class.java) { t ->
      t.kmpSources.set(findKmpSourcesTask.flatMap { it.output })

      // TODO(tsr): fix discovery of source files in non-KMP projects
      @OptIn(ExperimentalKotlinGradlePluginApi::class) // KGP 2.3+
      t.kotlinSource.setFrom(compilation.allKotlinSourceSets.map { sourceSet -> sourceSet.allKotlinSources })
      t.output.set(outputPaths.explodedSourcePath)
    }
  }

  override fun registerAbiAnalysisTask(abiExclusions: Provider<String>): TaskProvider<AbiAnalysisTask>? {
    if (!hasAbi) return null

    return project.tasks.register("abiAnalysis$taskNameSuffix", AbiAnalysisTask::class.java) { t ->
      t.classes.setFrom(sourceSet.classesDirs)
      t.kmpSources.set(findKmpSourcesTask.flatMap { it.output })
      t.exclusions.set(abiExclusions)
      t.kotlinMetadataClasspath.setFrom(KotlinMetadataClasspath.of(project))
      t.output.set(outputPaths.abiAnalysisPath)
      t.abiDump.set(outputPaths.abiDumpPath)
    }
  }

  override fun registerFindNativeLibsTask(): TaskProvider<FindNativeLibsTask> {
    return project.tasks.register("findNativeLibs$taskNameSuffix", FindNativeLibsTask::class.java) { t ->
      t.withDylibs(
        project.configurations.named(compileConfigurationName).resolvedArtifactsFor(ArtifactAttributes.DYLIB)
      )
      t.output.set(outputPaths.nativeDependenciesPath)
    }
  }
}
