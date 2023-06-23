@file:Suppress("UnstableApiUsage")

package com.autonomousapps.internal.analyzer

import com.autonomousapps.internal.OutputPaths
import com.autonomousapps.internal.utils.capitalizeSafely
import com.autonomousapps.model.declaration.SourceSetKind
import com.autonomousapps.services.InMemoryCache
import com.autonomousapps.tasks.AbiAnalysisTask
import com.autonomousapps.tasks.ClassListExploderTask
import com.autonomousapps.tasks.FindDeclaredProcsTask
import org.gradle.api.Project
import org.gradle.api.file.FileTree
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.register
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet as JbKotlinSourceSet

internal abstract class JvmAnalyzer(
  project: Project,
  private val sourceSet: JvmSourceSet,
  private val hasAbi: Boolean
) : AbstractDependencyAnalyzer(project) {

  final override val flavorName: String? = null
  final override val buildType: String? = null
  final override val kind: SourceSetKind = sourceSet.kind
  final override val variantName: String = sourceSet.name
  final override val variantNameCapitalized: String = variantName.capitalizeSafely()
  final override val taskNameSuffix: String = variantNameCapitalized

  final override val compileConfigurationName = sourceSet.compileClasspathConfigurationName
  final override val runtimeConfigurationName = sourceSet.runtimeClasspathConfigurationName
  final override val kaptConfigurationName = "kapt"
  final override val annotationProcessorConfigurationName = "annotationProcessor"

  final override val attributeValueJar = "jar"

  final override val kotlinSourceFiles: FileTree = getKotlinSources()
  override val javaSourceFiles: FileTree? = getJavaSources()
  final override val groovySourceFiles: FileTree = getGroovySources()
  final override val scalaSourceFiles: FileTree = getScalaSources()

  final override val isDataBindingEnabled: Boolean = false
  final override val isViewBindingEnabled: Boolean = false

  override val outputPaths = OutputPaths(project, variantName)

  final override val testJavaCompileName: String = "compileTestJava"
  final override val testKotlinCompileName: String = "compileTestKotlin"

  final override fun registerByteCodeSourceExploderTask(): TaskProvider<ClassListExploderTask> {
    return project.tasks.register<ClassListExploderTask>("explodeByteCodeSource$variantNameCapitalized") {
      classes.setFrom(sourceSet.classesDirs)
      // These two are only used for Android projects (for now)
      javaClasses.setFrom(project.files())
      kotlinClasses.setFrom(project.files())

      output.set(outputPaths.explodingBytecodePath)
    }
  }

  final override fun registerAbiAnalysisTask(abiExclusions: Provider<String>): TaskProvider<AbiAnalysisTask>? {
    if (!hasAbi) return null

    return project.tasks.register<AbiAnalysisTask>("abiAnalysis$variantNameCapitalized") {
      classes.setFrom(sourceSet.classesDirs)
      // These two are only used for Android projects (for now)
      javaClasses.setFrom(project.files())
      kotlinClasses.setFrom(project.files())

      exclusions.set(abiExclusions)
      output.set(outputPaths.abiAnalysisPath)
      abiDump.set(outputPaths.abiDumpPath)
    }
  }

  final override fun registerFindDeclaredProcsTask(
    inMemoryCache: Provider<InMemoryCache>
  ): TaskProvider<FindDeclaredProcsTask> {
    return project.tasks.register<FindDeclaredProcsTask>("findDeclaredProcs$variantNameCapitalized") {
      inMemoryCacheProvider.set(inMemoryCache)
      kaptConf()?.let {
        setKaptArtifacts(it.incoming.artifacts)
      }
      annotationProcessorConf()?.let {
        setAnnotationProcessorArtifacts(it.incoming.artifacts)
      }

      output.set(outputPaths.declaredProcPath)
    }
  }

  private fun getGroovySources(): FileTree = getSourceDirectories().matching(Language.filterOf(Language.GROOVY))
  private fun getJavaSources(): FileTree = getSourceDirectories().matching(Language.filterOf(Language.JAVA))
  private fun getKotlinSources(): FileTree = getSourceDirectories().matching(Language.filterOf(Language.KOTLIN))
  private fun getScalaSources(): FileTree = getSourceDirectories().matching(Language.filterOf(Language.SCALA))

  private fun getSourceDirectories(): FileTree {
    val allSource = sourceSet.sourceCode.sourceDirectories
    return project.files(allSource).asFileTree
  }
}

internal class JavaWithoutAbiAnalyzer(
  project: Project,
  sourceSet: SourceSet,
  kind: SourceSetKind
) : JvmAnalyzer(
  project = project,
  sourceSet = JavaSourceSet(sourceSet, kind),
  hasAbi = false
)

internal class JavaWithAbiAnalyzer(
  project: Project,
  sourceSet: SourceSet,
  kind: SourceSetKind,
  hasAbi: Boolean
) : JvmAnalyzer(
  project = project,
  sourceSet = JavaSourceSet(sourceSet, kind),
  hasAbi = hasAbi
)

internal abstract class KotlinJvmAnalyzer(
  project: Project,
  sourceSet: SourceSet,
  kotlinSourceSet: JbKotlinSourceSet,
  kind: SourceSetKind,
  hasAbi: Boolean
) : JvmAnalyzer(
  project = project,
  sourceSet = KotlinSourceSet(sourceSet, kotlinSourceSet, kind),
  hasAbi = hasAbi
) {
  final override val javaSourceFiles: FileTree? = null
}

internal class KotlinJvmAppAnalyzer(
  project: Project,
  sourceSet: SourceSet,
  kotlinSourceSet: JbKotlinSourceSet,
  kind: SourceSetKind
) : KotlinJvmAnalyzer(
  project = project,
  sourceSet = sourceSet,
  kotlinSourceSet = kotlinSourceSet,
  kind = kind,
  hasAbi = false
)

internal class KotlinJvmLibAnalyzer(
  project: Project,
  sourceSet: SourceSet,
  kotlinSourceSet: JbKotlinSourceSet,
  kind: SourceSetKind,
  hasAbi: Boolean
) : KotlinJvmAnalyzer(
  project = project,
  sourceSet = sourceSet,
  kotlinSourceSet = kotlinSourceSet,
  kind = kind,
  hasAbi = hasAbi
)
