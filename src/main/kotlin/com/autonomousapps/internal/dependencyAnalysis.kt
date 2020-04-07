@file:Suppress("UnstableApiUsage")

package com.autonomousapps.internal

import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.internal.publishing.AndroidArtifacts
import com.autonomousapps.internal.android.AndroidGradlePluginFactory
import com.autonomousapps.internal.utils.namedOrNull
import com.autonomousapps.tasks.*
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.ArtifactView
import org.gradle.api.attributes.Attribute
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.register
import java.io.File

/**
 * Abstraction for differentiating between android-app, android-lib, and java-lib projects.
 */
internal interface DependencyAnalyzer<T : ClassAnalysisTask> {
  /**
   * E.g., `flavorDebug`
   */
  val variantName: String

  /**
   * E.g., `FlavorDebug`
   */
  val variantNameCapitalized: String

  val compileConfigurationName: String
  val runtimeConfigurationName: String

  val attribute: Attribute<String>
  val attributeValue: String
  val attributeValueRes: String?

  val kotlinSourceFiles: FileTree
  val javaSourceFiles: FileTree?
  val javaAndKotlinSourceFiles: FileTree?

  val isDataBindingEnabled: Boolean
  val isViewBindingEnabled: Boolean

  /**
   * This produces a report that lists all of the used classes (FQCN) in the project.
   */
  fun registerClassAnalysisTask(): TaskProvider<out T>

  fun registerManifestPackageExtractionTask(): TaskProvider<ManifestPackageExtractionTask>? = null

  fun registerAndroidResToSourceAnalysisTask(manifestPackageExtractionTask: TaskProvider<ManifestPackageExtractionTask>):
    TaskProvider<AndroidResToSourceAnalysisTask>? = null

  fun registerAndroidResToResAnalysisTask(): TaskProvider<AndroidResToResToResAnalysisTask>? = null

  /**
   * This is a no-op for com.android.application projects, since they have no meaningful ABI.
   */
  fun registerAbiAnalysisTask(
    dependencyReportTask: TaskProvider<DependencyReportTask>
  ): TaskProvider<AbiAnalysisTask>? = null
}

/**
 * Base class for analyzing an Android project (com.android.application or com.android.library only).
 */
internal abstract class AndroidAnalyzer<T : ClassAnalysisTask>(
  protected val project: Project,
  protected val variant: BaseVariant,
  agpVersion: String
) : DependencyAnalyzer<T> {

  protected val agp = AndroidGradlePluginFactory(project, agpVersion).newAdapter()
  private val dataBindingEnabled = agp.isDataBindingEnabled()
  private val viewBindingEnabled = agp.isViewBindingEnabled()

  final override val variantName: String = variant.name
  final override val variantNameCapitalized: String = variantName.capitalize()
  final override val compileConfigurationName = "${variantName}CompileClasspath"
  final override val runtimeConfigurationName = "${variantName}RuntimeClasspath"
  final override val attribute: Attribute<String> = AndroidArtifacts.ARTIFACT_TYPE
  final override val kotlinSourceFiles: FileTree = getKotlinSources()
  final override val javaSourceFiles: FileTree = getJavaSources()
  final override val javaAndKotlinSourceFiles: FileTree = getJavaAndKotlinSources()
  final override val attributeValue = if (agpVersion.startsWith("4.")) {
    "android-classes-jar"
  } else {
    "android-classes"
  }
  final override val isDataBindingEnabled: Boolean = dataBindingEnabled
  final override val isViewBindingEnabled: Boolean = viewBindingEnabled

  // For AGP 3.5.3, this does not return any module dependencies
  override val attributeValueRes = "android-symbol-with-package-name"

  private val manifestArtifactView: Action<in ArtifactView.ViewConfiguration> =
    Action<ArtifactView.ViewConfiguration> {
      attributes.attribute(AndroidArtifacts.ARTIFACT_TYPE, "android-manifest")
    }

  protected fun getKaptStubs() = getKaptStubs(project, variantName)

  override fun registerManifestPackageExtractionTask(): TaskProvider<ManifestPackageExtractionTask> =
    project.tasks.register<ManifestPackageExtractionTask>("extractPackageNameFromManifest$variantNameCapitalized") {
      setArtifacts(project.configurations[compileConfigurationName].incoming.artifactView(manifestArtifactView).artifacts)
      manifestPackagesReport.set(project.layout.buildDirectory.file(getManifestPackagesPath(variantName)))
    }

  override fun registerAndroidResToSourceAnalysisTask(
    manifestPackageExtractionTask: TaskProvider<ManifestPackageExtractionTask>
  ): TaskProvider<AndroidResToSourceAnalysisTask> =
    project.tasks.register<AndroidResToSourceAnalysisTask>("findAndroidResBySourceUsage$variantNameCapitalized") {
      val resourceArtifacts = project.configurations[compileConfigurationName].incoming.artifactView {
        attributes.attribute(attribute, attributeValueRes)
      }.artifacts

      manifestPackages.set(manifestPackageExtractionTask.flatMap { it.manifestPackagesReport })
      setResources(resourceArtifacts)
      javaAndKotlinSourceFiles.setFrom(this@AndroidAnalyzer.javaAndKotlinSourceFiles)

      usedAndroidResDependencies.set(project.layout.buildDirectory.file(getAndroidResToSourceUsagePath(variantName)))
    }

  override fun registerAndroidResToResAnalysisTask(): TaskProvider<AndroidResToResToResAnalysisTask> {
    return project.tasks.register<AndroidResToResToResAnalysisTask>("findAndroidResByResUsage$variantNameCapitalized") {
      setAndroidPublicRes(project.configurations[compileConfigurationName].incoming.artifactView {
        attributes.attribute(attribute, "android-public-res")
      }.artifacts)
      androidLocalRes.setFrom(getAndroidRes())

      output.set(project.layout.buildDirectory.file(getAndroidResToResUsagePath(variantName)))
    }
  }

  private fun getKotlinSources(): FileTree = getSourceDirectories().asFileTree.matching {
    include("**/*.kt")
    exclude("**/*.java")
  }

  private fun getJavaSources(): FileTree = getSourceDirectories().asFileTree.matching {
    include("**/*.java")
    exclude("**/*.kt")
  }

  private fun getJavaAndKotlinSources(): FileTree = getSourceDirectories().asFileTree
    .matching {
      include("**/*.java")
      include("**/*.kt")
    }

  private fun getSourceDirectories(): ConfigurableFileCollection {
    val javaDirs = variant.sourceSets.flatMap {
      it.javaDirectories
    }.filter { it.exists() }

    val kotlinDirs = javaDirs
      .map { it.path }
      .map { it.removeSuffix("java") + "kotlin" }
      .map { File(it) }
      .filter { it.exists() }

    return project.files(javaDirs + kotlinDirs)
  }

  private fun getAndroidRes(): FileTree {
    val resDirs = variant.sourceSets.flatMap {
      it.resDirectories
    }.filter { it.exists() }

    return project.files(resDirs).asFileTree.matching {
      include("**/*.xml")
    }
  }
}

internal class AndroidAppAnalyzer(
  project: Project, variant: BaseVariant, agpVersion: String
) : AndroidAnalyzer<ClassListAnalysisTask>(project, variant, agpVersion) {

  override fun registerClassAnalysisTask(): TaskProvider<ClassListAnalysisTask> {
    // Known to exist in Kotlin 1.3.61.
    val kotlinCompileTask = project.tasks.namedOrNull("compile${variantNameCapitalized}Kotlin")
    // Known to exist in AGP 3.5, 3.6, and 4.0, albeit with different backing classes (AndroidJavaCompile,
    // JavaCompile)
    val javaCompileTask = project.tasks.named("compile${variantNameCapitalized}JavaWithJavac")

    return project.tasks.register<ClassListAnalysisTask>("analyzeClassUsage$variantNameCapitalized") {
      kotlinCompileTask?.let { kotlinClasses.from(it.get().outputs.files.asFileTree) }
      javaClasses.from(javaCompileTask.get().outputs.files.asFileTree)
      kaptJavaStubs.from(getKaptStubs())
      layouts(variant.sourceSets.flatMap { it.resDirectories })

      output.set(project.layout.buildDirectory.file(getAllUsedClassesPath(variantName)))
    }
  }
}

internal class AndroidLibAnalyzer(
  project: Project, variant: BaseVariant, agpVersion: String
) : AndroidAnalyzer<JarAnalysisTask>(project, variant, agpVersion) {

  override fun registerClassAnalysisTask(): TaskProvider<JarAnalysisTask> =
    project.tasks.register<JarAnalysisTask>("analyzeClassUsage$variantNameCapitalized") {
      jar.set(getBundleTaskOutput())
      kaptJavaStubs.from(getKaptStubs())
      layouts(variant.sourceSets.flatMap { it.resDirectories })

      output.set(project.layout.buildDirectory.file(getAllUsedClassesPath(variantName)))
    }

  override fun registerAbiAnalysisTask(
    dependencyReportTask: TaskProvider<DependencyReportTask>
  ): TaskProvider<AbiAnalysisTask> =
    project.tasks.register<AbiAnalysisTask>("abiAnalysis$variantNameCapitalized") {
      jar.set(getBundleTaskOutput())
      dependencies.set(dependencyReportTask.flatMap { it.allComponentsReport })

      output.set(project.layout.buildDirectory.file(getAbiAnalysisPath(variantName)))
      abiDump.set(project.layout.buildDirectory.file(getAbiDumpPath(variantName)))
    }

  private fun getBundleTaskOutput(): Provider<RegularFile> = agp.getBundleTaskOutput(variantNameCapitalized)
}

internal class JavaLibAnalyzer(
  private val project: Project,
  private val sourceSet: SourceSet
) : DependencyAnalyzer<JarAnalysisTask> {

  override val variantName: String = sourceSet.name
  override val variantNameCapitalized = variantName.capitalize()

  // Yes, these two are the same for this case
  override val compileConfigurationName = "compileClasspath"
  override val runtimeConfigurationName = compileConfigurationName
  override val attribute: Attribute<String> = Attribute.of("artifactType", String::class.java)
  override val attributeValue = "jar"
  override val attributeValueRes: String? = null

  override val kotlinSourceFiles: FileTree = getKotlinSources()
  override val javaSourceFiles: FileTree = getJavaSources()
  override val javaAndKotlinSourceFiles: FileTree? = null

  override val isDataBindingEnabled: Boolean = false
  override val isViewBindingEnabled: Boolean = false

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
    val javaAndKotlinSource = sourceSet.allJava.sourceDirectories
    return project.files(javaAndKotlinSource).asFileTree
  }

  override fun registerClassAnalysisTask(): TaskProvider<JarAnalysisTask> =
    project.tasks.register<JarAnalysisTask>("analyzeClassUsage$variantNameCapitalized") {
      jar.set(getJarTask().flatMap { it.archiveFile })
      kaptJavaStubs.from(getKaptStubs(project, variantName))
      output.set(project.layout.buildDirectory.file(getAllUsedClassesPath(variantName)))
    }

  override fun registerAbiAnalysisTask(dependencyReportTask: TaskProvider<DependencyReportTask>) =
    project.tasks.register<AbiAnalysisTask>("abiAnalysis$variantNameCapitalized") {
      jar.set(getJarTask().flatMap { it.archiveFile })
      dependencies.set(dependencyReportTask.flatMap { it.allComponentsReport })

      output.set(project.layout.buildDirectory.file(getAbiAnalysisPath(variantName)))
      abiDump.set(project.layout.buildDirectory.file(getAbiDumpPath(variantName)))
    }
}

// Best guess as to path to kapt-generated Java stubs
internal fun getKaptStubs(project: Project, variantName: String): FileTree =
  project.layout.buildDirectory.asFileTree.matching {
    include("**/kapt*/**/${variantName}/**/*.java")
  }
