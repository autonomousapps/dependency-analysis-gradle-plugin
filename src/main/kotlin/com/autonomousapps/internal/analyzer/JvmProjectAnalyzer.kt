@file:Suppress("UnstableApiUsage")

package com.autonomousapps.internal.analyzer

import com.autonomousapps.internal.OutputPaths
import com.autonomousapps.internal.utils.capitalizeSafely
import com.autonomousapps.services.InMemoryCache
import com.autonomousapps.tasks.*
import org.gradle.api.Project
import org.gradle.api.UnknownDomainObjectException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Attribute
import org.gradle.api.file.FileTree
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.register
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet as JbKotlinSourceSet

internal abstract class JvmAnalyzer(
  protected val project: Project,
  private val sourceSet: JvmSourceSet
) : DependencyAnalyzer<JarAnalysisTask> {
  final override val flavorName: String? = null
  final override val variantName: String = sourceSet.name
  final override val variantNameCapitalized = variantName.capitalizeSafely()

  // Yes, these two are the same for this case
  final override val compileConfigurationName = "compileClasspath"
  final override val runtimeConfigurationName = compileConfigurationName

  // Do NOT replace this with AndroidArtifacts.ARTIFACT_TYPE, as this will not be available in a
  // java lib project
  final override val attribute: Attribute<String> = Attribute.of("artifactType", String::class.java)
  final override val attributeValue = "jar"
  final override val attributeValueRes: String? = null

  final override val kotlinSourceFiles: FileTree = getKotlinSources()
  override val javaSourceFiles: FileTree? = getJavaSources()
  final override val javaAndKotlinSourceFiles: FileTree? = null

  final override val isDataBindingEnabled: Boolean = false
  final override val isViewBindingEnabled: Boolean = false

  protected val outputPaths = OutputPaths(project, variantName)

  final override fun registerClassAnalysisTask(): TaskProvider<JarAnalysisTask> =
    project.tasks.register<JarAnalysisTask>("analyzeClassUsage$variantNameCapitalized") {
      jar.set(getJarTask().flatMap { it.archiveFile })
      kaptJavaStubs.from(getKaptStubs(project, variantName))
      output.set(outputPaths.allUsedClassesPath)
    }

  final override fun registerFindDeclaredProcsTask(
    inMemoryCacheProvider: Provider<InMemoryCache>,
    locateDependenciesTask: TaskProvider<LocateDependenciesTask>
  ): TaskProvider<FindDeclaredProcsTask> {
    return project.tasks.register<FindDeclaredProcsTask>(
      "findDeclaredProcs$variantNameCapitalized"
    ) {
      kaptConf()?.let {
        setKaptArtifacts(it.incoming.artifacts)
      }
      annotationProcessorConf()?.let {
        setAnnotationProcessorArtifacts(it.incoming.artifacts)
      }
      dependencyConfigurations.set(locateDependenciesTask.flatMap { it.output })

      output.set(outputPaths.declaredProcPath)
      outputPretty.set(outputPaths.declaredProcPrettyPath)

      this.inMemoryCacheProvider.set(inMemoryCacheProvider)
    }
  }

  final override fun registerFindUnusedProcsTask(
    findDeclaredProcs: TaskProvider<FindDeclaredProcsTask>,
    importFinder: TaskProvider<ImportFinderTask>
  ): TaskProvider<FindUnusedProcsTask> {
    return project.tasks.register<FindUnusedProcsTask>(
      "findUnusedProcs${variantNameCapitalized}"
    ) {
      jar.set(getJarTask().flatMap { it.archiveFile })
      imports.set(importFinder.flatMap { it.importsReport })
      annotationProcessorsProperty.set(findDeclaredProcs.flatMap { it.output })

      output.set(outputPaths.unusedProcPath)
    }
  }

  private fun kaptConf(): Configuration? = try {
    project.configurations["kapt"]
  } catch (_: UnknownDomainObjectException) {
    null
  }

  private fun annotationProcessorConf(): Configuration? = try {
    project.configurations["annotationProcessor"]
  } catch (_: UnknownDomainObjectException) {
    null
  }

  protected fun getJarTask() = project.tasks.named(sourceSet.jarTaskName, Jar::class.java)

  private fun getKotlinSources(): FileTree = getSourceDirectories().matching {
    include("**/*.kt")
    exclude("**/*.java")
  }

  private fun getJavaSources(): FileTree = getSourceDirectories().matching {
    include("**/*.java")
    exclude("**/*.kt")
  }

  private fun getSourceDirectories(): FileTree {
    val javaAndKotlinSource = sourceSet.sourceCode.sourceDirectories
    return project.files(javaAndKotlinSource).asFileTree
  }
}

internal class JavaAppAnalyzer(project: Project, sourceSet: SourceSet)
  : JvmAnalyzer(project, JavaSourceSet(sourceSet))

internal class JavaLibAnalyzer(project: Project, sourceSet: SourceSet)
  : JvmAnalyzer(project, JavaSourceSet(sourceSet)) {

  override fun registerAbiAnalysisTask(
    dependencyReportTask: TaskProvider<DependencyReportTask>,
    abiExclusions: Provider<String>
  ): TaskProvider<AbiAnalysisTask>? =
    project.tasks.register<AbiAnalysisTask>("abiAnalysis$variantNameCapitalized") {
      jar.set(getJarTask().flatMap { it.archiveFile })
      dependencies.set(dependencyReportTask.flatMap { it.allComponentsReport })
      exclusions.set(abiExclusions)

      output.set(outputPaths.abiAnalysisPath)
      abiDump.set(outputPaths.abiDumpPath)
    }
}

internal abstract class KotlinJvmAnalyzer(project: Project, sourceSet: JbKotlinSourceSet)
  : JvmAnalyzer(project, KotlinSourceSet(sourceSet)) {
  final override val javaSourceFiles: FileTree? = null
}

internal class KotlinJvmAppAnalyzer(project: Project, sourceSet: JbKotlinSourceSet)
  : KotlinJvmAnalyzer(project, sourceSet)

internal class KotlinJvmLibAnalyzer(project: Project, sourceSet: JbKotlinSourceSet)
  : KotlinJvmAnalyzer(project, sourceSet) {
  override fun registerAbiAnalysisTask(
    dependencyReportTask: TaskProvider<DependencyReportTask>,
    abiExclusions: Provider<String>
  ) =
    project.tasks.register<AbiAnalysisTask>("abiAnalysis$variantNameCapitalized") {
      jar.set(getJarTask().flatMap { it.archiveFile })
      dependencies.set(dependencyReportTask.flatMap { it.allComponentsReport })

      output.set(outputPaths.abiAnalysisPath)
      abiDump.set(outputPaths.abiDumpPath)
    }
}
