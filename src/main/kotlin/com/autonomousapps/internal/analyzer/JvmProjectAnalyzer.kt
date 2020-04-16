@file:Suppress("UnstableApiUsage")

package com.autonomousapps.internal.analyzer

import com.autonomousapps.internal.getAbiAnalysisPath
import com.autonomousapps.internal.getAbiDumpPath
import com.autonomousapps.internal.getAllUsedClassesPath
import com.autonomousapps.internal.getDeclaredProcPath
import com.autonomousapps.internal.getDeclaredProcPrettyPath
import com.autonomousapps.internal.getUnusedProcPath
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
  private val project: Project,
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

  final override fun registerClassAnalysisTask(): TaskProvider<JarAnalysisTask> =
    project.tasks.register<JarAnalysisTask>("analyzeClassUsage$variantNameCapitalized") {
      jar.set(getJarTask().flatMap { it.archiveFile })
      kaptJavaStubs.from(getKaptStubs(project, variantName))
      output.set(project.layout.buildDirectory.file(getAllUsedClassesPath(variantName)))
    }

  final override fun registerAbiAnalysisTask(dependencyReportTask: TaskProvider<DependencyReportTask>) =
    project.tasks.register<AbiAnalysisTask>("abiAnalysis$variantNameCapitalized") {
      jar.set(getJarTask().flatMap { it.archiveFile })
      dependencies.set(dependencyReportTask.flatMap { it.allComponentsReport })

      output.set(project.layout.buildDirectory.file(getAbiAnalysisPath(variantName)))
      abiDump.set(project.layout.buildDirectory.file(getAbiDumpPath(variantName)))
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

      output.set(project.layout.buildDirectory.file(getDeclaredProcPath(variantName)))
      outputPretty.set(project.layout.buildDirectory.file(getDeclaredProcPrettyPath(variantName)))

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

      output.set(project.layout.buildDirectory.file(getUnusedProcPath(variantName)))
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

  private fun getJarTask() = project.tasks.named(sourceSet.jarTaskName, Jar::class.java)

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

internal class JavaLibAnalyzer(project: Project, sourceSet: SourceSet)
  : JvmAnalyzer(project, JavaSourceSet(sourceSet))

internal class KotlinJvmAnalyzer(project: Project, sourceSet: JbKotlinSourceSet)
  : JvmAnalyzer(project, KotlinSourceSet(sourceSet)) {
  override val javaSourceFiles: FileTree? = null
}
