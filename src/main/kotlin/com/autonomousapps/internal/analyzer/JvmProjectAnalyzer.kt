// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("UnstableApiUsage")

package com.autonomousapps.internal.analyzer

import com.autonomousapps.internal.ArtifactAttributes
import com.autonomousapps.internal.OutputPaths
import com.autonomousapps.internal.artifactsFor
import com.autonomousapps.internal.utils.capitalizeSafely
import com.autonomousapps.model.source.SourceKind
import com.autonomousapps.services.InMemoryCache
import com.autonomousapps.tasks.AbiAnalysisTask
import com.autonomousapps.tasks.ClassListExploderTask
import com.autonomousapps.tasks.FindDeclaredProcsTask
import com.autonomousapps.tasks.FindNativeLibsTask
import org.gradle.api.Project
import org.gradle.api.file.FileTree
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.register
import java.io.File

internal abstract class JvmAnalyzer(
  project: Project,
  private val sourceSet: JvmSourceSet,
  private val hasAbi: Boolean,
) : AbstractDependencyAnalyzer(project) {

  final override val flavorName: String? = null
  final override val buildType: String? = null
  final override val sourceKind: SourceKind = sourceSet.sourceKind
  final override val variantName: String = sourceSet.name
  final override val taskNameSuffix: String = variantName.capitalizeSafely()

  final override val compileConfigurationName = sourceSet.compileClasspathConfigurationName
  final override val runtimeConfigurationName = sourceSet.runtimeClasspathConfigurationName
  final override val kaptConfigurationName = "kapt"
  final override val annotationProcessorConfigurationName = "annotationProcessor"

  final override val attributeValueJar = "jar"

  final override val kotlinSourceFiles: Provider<Iterable<File>> = getKotlinSources()
  override val javaSourceFiles: Provider<Iterable<File>>? = getJavaSources()
  final override val groovySourceFiles: Provider<Iterable<File>> = getGroovySources()
  final override val scalaSourceFiles: Provider<Iterable<File>> = getScalaSources()

  final override val isDataBindingEnabled: Provider<Boolean> = project.provider { false }
  final override val isViewBindingEnabled: Provider<Boolean> = project.provider { false }

  override val outputPaths = OutputPaths(project, variantName)

  final override fun registerByteCodeSourceExploderTask(): TaskProvider<ClassListExploderTask> {
    return project.tasks.register<ClassListExploderTask>("explodeByteCodeSource$taskNameSuffix") {
      classes.setFrom(sourceSet.classesDirs)
      output.set(outputPaths.explodingBytecodePath)
    }
  }

  final override fun registerAbiAnalysisTask(abiExclusions: Provider<String>): TaskProvider<AbiAnalysisTask>? {
    if (!hasAbi) return null

    return project.tasks.register<AbiAnalysisTask>("abiAnalysis$taskNameSuffix") {
      sourceFiles.setFrom(sourceSet.sourceCode)
      classes.setFrom(sourceSet.classesDirs)
      exclusions.set(abiExclusions)
      output.set(outputPaths.abiAnalysisPath)
      abiDump.set(outputPaths.abiDumpPath)
    }
  }

  final override fun registerFindDeclaredProcsTask(): TaskProvider<FindDeclaredProcsTask> {
    return project.tasks.register<FindDeclaredProcsTask>("findDeclaredProcs$taskNameSuffix") {
      inMemoryCacheProvider.set(InMemoryCache.register(project))
      kaptConf()?.let {
        setKaptArtifacts(it.incoming.artifacts)
      }
      annotationProcessorConf()?.let {
        setAnnotationProcessorArtifacts(it.incoming.artifacts)
      }

      output.set(outputPaths.declaredProcPath)
    }
  }

  override fun registerFindNativeLibsTask(): TaskProvider<FindNativeLibsTask> {
    return project.tasks.register<FindNativeLibsTask>("findNativeLibs$taskNameSuffix") {
      setMacNativeLibs(project.configurations[compileConfigurationName].artifactsFor(ArtifactAttributes.DYLIB))
      output.set(outputPaths.nativeDependenciesPath)
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

internal class JavaWithoutAbiAnalyzer(
  project: Project,
  sourceSet: SourceSet,
  sourceKind: SourceKind,
) : JvmAnalyzer(
  project = project,
  sourceSet = JavaSourceSet(sourceSet, sourceKind),
  hasAbi = false
)

internal class JavaWithAbiAnalyzer(
  project: Project,
  sourceSet: SourceSet,
  sourceKind: SourceKind,
  hasAbi: Boolean,
) : JvmAnalyzer(
  project = project,
  sourceSet = JavaSourceSet(sourceSet, sourceKind),
  hasAbi = hasAbi
)

internal abstract class KotlinJvmAnalyzer(
  project: Project,
  sourceSet: SourceSet,
  sourceKind: SourceKind,
  hasAbi: Boolean,
) : JvmAnalyzer(
  project = project,
  sourceSet = KotlinSourceSet(sourceSet, sourceKind),
  hasAbi = hasAbi
)

internal class KotlinJvmAppAnalyzer(
  project: Project,
  sourceSet: SourceSet,
  sourceKind: SourceKind,
) : KotlinJvmAnalyzer(
  project = project,
  sourceSet = sourceSet,
  sourceKind = sourceKind,
  hasAbi = false
)

internal class KotlinJvmLibAnalyzer(
  project: Project,
  sourceSet: SourceSet,
  sourceKind: SourceKind,
  hasAbi: Boolean,
) : KotlinJvmAnalyzer(
  project = project,
  sourceSet = sourceSet,
  sourceKind = sourceKind,
  hasAbi = hasAbi
)
