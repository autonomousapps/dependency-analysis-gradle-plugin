@file:Suppress("UnstableApiUsage")

package com.autonomousapps.internal.analyzer

import com.autonomousapps.internal.OutputPaths
import com.autonomousapps.internal.utils.capitalizeSafely
import com.autonomousapps.internal.utils.namedOrNull
import com.autonomousapps.services.InMemoryCache
import com.autonomousapps.shouldAnalyzeTests
import com.autonomousapps.tasks.*
import org.gradle.api.Project
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
  final override val variantName: String = mainSourceSet.name
  final override val variantNameCapitalized = variantName.capitalizeSafely()

  final override val compileConfigurationName = "compileClasspath"
  final override val testCompileConfigurationName = "testCompileClasspath"
  final override val kaptConfigurationName = "kapt"
  final override val annotationProcessorConfigurationName = "annotationProcessor"

  final override val attributeValueJar = "jar"

  final override val kotlinSourceFiles: FileTree = getKotlinSources()
  override val javaSourceFiles: FileTree? = getJavaSources()
  final override val javaAndKotlinSourceFiles: FileTree? = null

  final override val isDataBindingEnabled: Boolean = false
  final override val isViewBindingEnabled: Boolean = false

  protected val outputPaths = OutputPaths(project, variantName)

  override fun registerCreateVariantFilesTask(): TaskProvider<JvmCreateVariantFiles> {
    return project.tasks.register<JvmCreateVariantFiles>("createVariantFiles$variantNameCapitalized") {
      val mainFiles = project.files(mainSourceSet.sourceCode.sourceDirectories)
      val testFiles = testSourceSet?.let { project.files(it.sourceCode.sourceDirectories) }

      this.mainFiles.setFrom(mainFiles)
      testFiles?.let { this.testFiles.setFrom(it) }

      output.set(outputPaths.variantFilesPath)
    }
  }

  final override val testJavaCompileName: String = "compileTestJava"
  final override val testKotlinCompileName: String = "compileTestKotlin"

  final override fun registerClassAnalysisTask(
    createVariantFiles: TaskProvider<out CreateVariantFiles>
  ): TaskProvider<ClassListAnalysisTask> {
    return project.tasks.register<ClassListAnalysisTask>("analyzeClassUsage$variantNameCapitalized") {
      variantFiles.set(createVariantFiles.flatMap { it.output })

      javaCompileTask()?.let { javaClasses.from(it.get().outputs.files.asFileTree) }
      kotlinCompileTask()?.let { kotlinClasses.from(it.get().outputs.files.asFileTree) }

      if (project.shouldAnalyzeTests()) {
        testJavaCompile?.let { javaCompile ->
          testJavaClassesDir.set(javaCompile.flatMap { it.destinationDirectory })
        }
        testKotlinCompile?.let { kotlinCompile ->
          testKotlinClassesDir.set(kotlinCompile.flatMap { it.destinationDirectory })
        }
      }

      output.set(outputPaths.allUsedClassesPath)
      outputPretty.set(outputPaths.allUsedClassesPrettyPath)
    }
  }

  final override fun registerByteCodeSourceExploderTask(): TaskProvider<ClassListExploderTask> {
    return project.tasks.register<ClassListExploderTask>("explodeByteCodeSource$variantNameCapitalized") {
      javaCompileTask()?.let { javaClasses.from(it.get().outputs.files.asFileTree) }
      kotlinCompileTask()?.let { kotlinClasses.from(it.get().outputs.files.asFileTree) }
      output.set(outputPaths.explodingBytecodePath)
    }
  }

  final override fun registerFindDeclaredProcsTask(
    inMemoryCache: Provider<InMemoryCache>,
    locateDependenciesTask: TaskProvider<LocateDependenciesTask>
  ): TaskProvider<FindDeclaredProcsTask> {
    return project.tasks.register<FindDeclaredProcsTask>("findDeclaredProcs$variantNameCapitalized") {
      inMemoryCacheProvider.set(inMemoryCache)
      kaptConf()?.let {
        setKaptArtifacts(it.incoming.artifacts)
      }
      annotationProcessorConf()?.let {
        setAnnotationProcessorArtifacts(it.incoming.artifacts)
      }
      locations.set(locateDependenciesTask.flatMap { it.output })

      output.set(outputPaths.declaredProcPath)
      outputPretty.set(outputPaths.declaredProcPrettyPath)
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

  final override fun registerFindUnusedProcsTask(
    findDeclaredProcs: TaskProvider<FindDeclaredProcsTask>,
    importFinder: TaskProvider<ImportFinderTask>
  ): TaskProvider<FindUnusedProcsTask> {
    return project.tasks.register<FindUnusedProcsTask>("findUnusedProcs${variantNameCapitalized}") {
      javaCompileTask()?.let { javaClasses.from(it.get().outputs.files.asFileTree) }
      kotlinCompileTask()?.let { kotlinClasses.from(it.get().outputs.files.asFileTree) }
      imports.set(importFinder.flatMap { it.importsReport })
      annotationProcessorsProperty.set(findDeclaredProcs.flatMap { it.output })

      output.set(outputPaths.unusedProcPath)
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

  protected fun javaCompileTask() = project.tasks.namedOrNull("compileJava")

  protected fun kotlinCompileTask() = project.tasks.namedOrNull("compileKotlin")
    ?: project.tasks.namedOrNull("compileKotlinJvm") // for multiplatform projects

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
  project: Project, sourceSet: SourceSet, testSourceSet: SourceSet?
) : JvmAnalyzer(project, JavaSourceSet(sourceSet), testSourceSet?.let { JavaSourceSet(it) })

internal class JavaLibAnalyzer(
  project: Project, sourceSet: SourceSet, testSourceSet: SourceSet?
) : JvmAnalyzer(project, JavaSourceSet(sourceSet), testSourceSet?.let { JavaSourceSet(it) }) {

  override fun registerAbiAnalysisTask(
    analyzeJarTask: TaskProvider<AnalyzeJarTask>,
    abiExclusions: Provider<String>
  ): TaskProvider<AbiAnalysisTask> {
    return project.tasks.register<AbiAnalysisTask>("abiAnalysis$variantNameCapitalized") {
      javaCompileTask()?.let { javaClasses.from(it.get().outputs.files.asFileTree) }
      kotlinCompileTask()?.let { kotlinClasses.from(it.get().outputs.files.asFileTree) }
      dependencies.set(analyzeJarTask.flatMap { it.allComponentsReport })
      exclusions.set(abiExclusions)

      output.set(outputPaths.abiAnalysisPath)
      abiDump.set(outputPaths.abiDumpPath)
    }
  }

  override fun registerAbiAnalysisTask2(abiExclusions: Provider<String>): TaskProvider<AbiAnalysisTask2> {
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
  testSourceSet: JbKotlinSourceSet?
) : JvmAnalyzer(
  project = project,
  mainSourceSet = KotlinSourceSet(mainSourceSet),
  testSourceSet = testSourceSet?.let { KotlinSourceSet(it) }
) {
  final override val javaSourceFiles: FileTree? = null
}

internal class KotlinJvmAppAnalyzer(
  project: Project,
  sourceSet: JbKotlinSourceSet,
  testSourceSet: JbKotlinSourceSet?
) : KotlinJvmAnalyzer(
  project = project,
  mainSourceSet = sourceSet,
  testSourceSet = testSourceSet
)

internal class KotlinJvmLibAnalyzer(
  project: Project,
  mainSourceSet: JbKotlinSourceSet,
  testSourceSet: JbKotlinSourceSet?
) : KotlinJvmAnalyzer(
  project = project,
  mainSourceSet = mainSourceSet,
  testSourceSet = testSourceSet
) {

  override fun registerAbiAnalysisTask(
    analyzeJarTask: TaskProvider<AnalyzeJarTask>,
    abiExclusions: Provider<String>
  ): TaskProvider<AbiAnalysisTask> {
    return project.tasks.register<AbiAnalysisTask>("abiAnalysis$variantNameCapitalized") {
      javaCompileTask()?.let { javaClasses.from(it.get().outputs.files.asFileTree) }
      kotlinCompileTask()?.let { kotlinClasses.from(it.get().outputs.files.asFileTree) }
      dependencies.set(analyzeJarTask.flatMap { it.allComponentsReport })

      output.set(outputPaths.abiAnalysisPath)
      abiDump.set(outputPaths.abiDumpPath)
    }
  }

  override fun registerAbiAnalysisTask2(abiExclusions: Provider<String>): TaskProvider<AbiAnalysisTask2> {
    return project.tasks.register<AbiAnalysisTask2>("abiAnalysis$variantNameCapitalized") {
      javaCompileTask()?.let { javaClasses.from(it.get().outputs.files.asFileTree) }
      kotlinCompileTask()?.let { kotlinClasses.from(it.get().outputs.files.asFileTree) }
      exclusions.set(abiExclusions)
      output.set(outputPaths.abiAnalysisPath)
      abiDump.set(outputPaths.abiDumpPath)
    }
  }
}
