// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.analyzer

import com.autonomousapps.internal.ArtifactAttributes
import com.autonomousapps.internal.OutputPaths
import com.autonomousapps.internal.artifactsFor
import com.autonomousapps.internal.utils.capitalizeSafely
import com.autonomousapps.model.source.SourceKind
import com.autonomousapps.services.InMemoryCache
import com.autonomousapps.tasks.*
import org.gradle.api.Project
import org.gradle.api.file.FileTree
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import java.io.File

internal class KmpProjectAnalyzer(
  project: Project,
  private val sourceSet: JvmSourceSet,
  private val hasAbi: Boolean,
) : AbstractDependencyAnalyzer(project) {

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
    return project.tasks.register("explodeByteCodeSource$taskNameSuffix", ClassListExploderTask::class.java) {
      it.classes.setFrom(sourceSet.classesDirs)
      it.output.set(outputPaths.explodingBytecodePath)
    }
  }

  override fun registerCodeSourceExploderTask(): TaskProvider<out CodeSourceExploderTask> {
    return project.tasks.register("explodeCodeSource$taskNameSuffix", JvmCodeSourceExploderTask::class.java) {
      it.groovySource.setFrom(getGroovySources())
      it.javaSource.setFrom(getJavaSources())
      it.kotlinSource.setFrom(getKotlinSources())
      it.scalaSource.setFrom(getScalaSources())
      it.output.set(outputPaths.explodedSourcePath)
    }
  }

  override fun registerAbiAnalysisTask(abiExclusions: Provider<String>): TaskProvider<AbiAnalysisTask>? {
    if (!hasAbi) return null

    return project.tasks.register("abiAnalysis$taskNameSuffix", AbiAnalysisTask::class.java) {
      it.classes.setFrom(sourceSet.classesDirs)
      it.exclusions.set(abiExclusions)
      it.output.set(outputPaths.abiAnalysisPath)
      it.abiDump.set(outputPaths.abiDumpPath)
    }
  }

  override fun registerFindDeclaredProcsTask(): TaskProvider<FindDeclaredProcsTask> {
    return project.tasks.register("findDeclaredProcs$taskNameSuffix", FindDeclaredProcsTask::class.java) {
      it.inMemoryCacheProvider.set(InMemoryCache.register(project))
      kaptConf()?.let { configuration ->
        it.setKaptArtifacts(configuration.incoming.artifacts)
      }
      annotationProcessorConf()?.let { configuration ->
        it.setAnnotationProcessorArtifacts(configuration.incoming.artifacts)
      }

      it.output.set(outputPaths.declaredProcPath)
    }
  }

  override fun registerFindNativeLibsTask(): TaskProvider<FindNativeLibsTask> {
    return project.tasks.register("findNativeLibs$taskNameSuffix", FindNativeLibsTask::class.java) {
      it.setMacNativeLibs(
        project.configurations.getByName(compileConfigurationName).artifactsFor(ArtifactAttributes.DYLIB)
      )
      it.output.set(outputPaths.nativeDependenciesPath)
    }
  }

  private fun getGroovySources(): Provider<Iterable<File>> {
    return project.provider { getSourceDirectories().matching(Language.filterOf(Language.GROOVY)) }
  }

  private fun getJavaSources(): Provider<Iterable<File>> {
    return project.provider { getSourceDirectories().matching(Language.filterOf(Language.JAVA)) }
  }

  private fun getKotlinSources(): Provider<Iterable<File>> {
    return project.provider { getSourceDirectories().matching(Language.filterOf(Language.KOTLIN)) }
  }

  private fun getScalaSources(): Provider<Iterable<File>> {
    return project.provider { getSourceDirectories().matching(Language.filterOf(Language.SCALA)) }
  }

  private fun getSourceDirectories(): FileTree {
    val allSource = sourceSet.sourceCode.sourceDirectories
    return project.files(allSource).asFileTree
  }
}
