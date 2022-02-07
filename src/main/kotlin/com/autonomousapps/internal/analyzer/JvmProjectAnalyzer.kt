@file:Suppress("UnstableApiUsage")

package com.autonomousapps.internal.analyzer

import com.autonomousapps.internal.OutputPaths
import com.autonomousapps.internal.utils.capitalizeSafely
import com.autonomousapps.internal.utils.namedOrNull
import com.autonomousapps.model.SourceSetKind
import com.autonomousapps.services.InMemoryCache
import com.autonomousapps.tasks.AbiAnalysisTask2
import com.autonomousapps.tasks.ClassListExploderTask
import com.autonomousapps.tasks.FindDeclaredProcsTask2
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.UnknownDomainObjectException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileTree
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.register
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet as JbKotlinSourceSet

internal abstract class JvmAnalyzer(
  project: Project,
  private val mainSourceSet: JvmSourceSet,
  private val testSourceSet: JvmSourceSet?
) : AbstractDependencyAnalyzer(project) {

  final override val flavorName: String? = null
  final override val buildType: String? = null
  final override val kind: SourceSetKind = mainSourceSet.kind
  final override val variantName: String = mainSourceSet.name
  final override val variantNameCapitalized: String = variantName.capitalizeSafely()
  final override val taskNameSuffix: String = variantNameCapitalized

  final override val compileConfigurationName = mainSourceSet.compileClasspathConfigurationName
  final override val kaptConfigurationName = "kapt"
  final override val annotationProcessorConfigurationName = "annotationProcessor"

  final override val attributeValueJar = "jar"

  final override val kotlinSourceFiles: FileTree = getKotlinSources()
  override val javaSourceFiles: FileTree? = getJavaSources()
  final override val javaAndKotlinSourceFiles: FileTree? = null

  final override val isDataBindingEnabled: Boolean = false
  final override val isViewBindingEnabled: Boolean = false

  override val outputPaths = OutputPaths(project, variantName)

  final override val testJavaCompileName: String = "compileTestJava"
  final override val testKotlinCompileName: String = "compileTestKotlin"

  final override fun registerByteCodeSourceExploderTask(): TaskProvider<ClassListExploderTask> {
    return project.tasks.register<ClassListExploderTask>("explodeByteCodeSource$variantNameCapitalized") {
      javaCompileTask()?.let { javaClasses.from(it.get().outputs.files.asFileTree) }
      kotlinCompileTask()?.let { kotlinClasses.from(it.get().outputs.files.asFileTree) }
      output.set(outputPaths.explodingBytecodePath)
    }
  }

  final override fun registerFindDeclaredProcsTask(
    inMemoryCache: Provider<InMemoryCache>
  ): TaskProvider<FindDeclaredProcsTask2> {
    return project.tasks.register<FindDeclaredProcsTask2>("findDeclaredProcs$variantNameCapitalized") {
      inMemoryCacheProvider.set(inMemoryCache)
      kaptConf()?.let {
        setKaptArtifacts(it.incoming.artifacts)
      }
      annotationProcessorConf()?.let {
        setAnnotationProcessorArtifacts(it.incoming.artifacts)
      }

      output.set(outputPaths.declaredProcPath)
      outputPretty.set(outputPaths.declaredProcPrettyPath)
    }
  }

  private fun kaptConf(): Configuration? = try {
    project.configurations[kaptConfigurationName]
  } catch (_: UnknownDomainObjectException) {
    null
  }

  private fun annotationProcessorConf(): Configuration? = try {
    project.configurations[annotationProcessorConfigurationName]
  } catch (_: UnknownDomainObjectException) {
    null
  }

  protected fun javaCompileTask(): TaskProvider<Task>? {
    return project.tasks.namedOrNull(mainSourceSet.javaCompileTaskName)
  }

  protected fun kotlinCompileTask(): TaskProvider<Task>? {
    return project.tasks.namedOrNull(mainSourceSet.kotlinCompileTaskName)
    // TODO V2: multiplatform and test support
      ?: project.tasks.namedOrNull("compileKotlinJvm") // for multiplatform projects
  }

  protected fun getJarTask(): TaskProvider<Jar> = project.tasks.named(mainSourceSet.jarTaskName, Jar::class.java)

  private fun getKotlinSources(): FileTree = getSourceDirectories().matching {
    include("**/*.kt")
    exclude("**/*.java")
  }

  private fun getJavaSources(): FileTree = getSourceDirectories().matching {
    include("**/*.java")
    exclude("**/*.kt")
  }

  private fun getSourceDirectories(): FileTree {
    val javaAndKotlinSource = mainSourceSet.sourceCode.sourceDirectories
    return project.files(javaAndKotlinSource).asFileTree
  }
}

internal class JavaAppAnalyzer(
  project: Project,
  sourceSet: SourceSet,
  testSourceSet: SourceSet?,
  kind: SourceSetKind
) : JvmAnalyzer(
  project,
  JavaSourceSet(sourceSet, kind),
  testSourceSet?.let { JavaSourceSet(it, SourceSetKind.TEST) }
)

internal class JavaLibAnalyzer(
  project: Project,
  sourceSet: SourceSet,
  testSourceSet: SourceSet?,
  kind: SourceSetKind,
  private val hasAbi: Boolean
) : JvmAnalyzer(
  project,
  JavaSourceSet(sourceSet, kind),
  testSourceSet?.let { JavaSourceSet(it, SourceSetKind.TEST) }
) {

  override fun registerAbiAnalysisTask2(abiExclusions: Provider<String>): TaskProvider<AbiAnalysisTask2>? {
    if (!hasAbi) return null

    return project.tasks.register<AbiAnalysisTask2>("abiAnalysis$variantNameCapitalized") {
      javaCompileTask()?.let { javaClasses.from(it.get().outputs.files.asFileTree) }
      kotlinCompileTask()?.let { kotlinClasses.from(it.get().outputs.files.asFileTree) }
      exclusions.set(abiExclusions)
      output.set(outputPaths.abiAnalysisPath)
      abiDump.set(outputPaths.abiDumpPath)
    }
  }
}

internal abstract class KotlinJvmAnalyzer(
  project: Project,
  mainSourceSet: JbKotlinSourceSet,
  testSourceSet: JbKotlinSourceSet?,
  kind: SourceSetKind
) : JvmAnalyzer(
  project = project,
  mainSourceSet = KotlinSourceSet(mainSourceSet, kind),
  testSourceSet = testSourceSet?.let { KotlinSourceSet(it, SourceSetKind.TEST) }
) {
  final override val javaSourceFiles: FileTree? = null
}

internal class KotlinJvmAppAnalyzer(
  project: Project,
  sourceSet: JbKotlinSourceSet,
  testSourceSet: JbKotlinSourceSet?,
  kind: SourceSetKind
) : KotlinJvmAnalyzer(
  project = project,
  mainSourceSet = sourceSet,
  testSourceSet = testSourceSet,
  kind = kind
)

internal class KotlinJvmLibAnalyzer(
  project: Project,
  mainSourceSet: JbKotlinSourceSet,
  testSourceSet: JbKotlinSourceSet?,
  kind: SourceSetKind,
  private val hasAbi: Boolean
) : KotlinJvmAnalyzer(
  project = project,
  mainSourceSet = mainSourceSet,
  testSourceSet = testSourceSet,
  kind = kind
) {

  override fun registerAbiAnalysisTask2(abiExclusions: Provider<String>): TaskProvider<AbiAnalysisTask2>? {
    if (!hasAbi) return null

    return project.tasks.register<AbiAnalysisTask2>("abiAnalysis$variantNameCapitalized") {
      javaCompileTask()?.let { javaClasses.from(it.get().outputs.files.asFileTree) }
      kotlinCompileTask()?.let { kotlinClasses.from(it.get().outputs.files.asFileTree) }
      exclusions.set(abiExclusions)
      output.set(outputPaths.abiAnalysisPath)
      abiDump.set(outputPaths.abiDumpPath)
    }
  }
}
