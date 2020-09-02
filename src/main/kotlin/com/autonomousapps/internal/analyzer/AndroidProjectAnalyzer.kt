@file:Suppress("UnstableApiUsage")

package com.autonomousapps.internal.analyzer

import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.internal.publishing.AndroidArtifacts
import com.autonomousapps.advice.VariantFile
import com.autonomousapps.internal.OutputPaths
import com.autonomousapps.internal.android.AndroidGradlePluginFactory
import com.autonomousapps.internal.utils.capitalizeSafely
import com.autonomousapps.internal.utils.flatMapToSet
import com.autonomousapps.internal.utils.namedOrNull
import com.autonomousapps.services.InMemoryCache
import com.autonomousapps.tasks.*
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.UnknownDomainObjectException
import org.gradle.api.artifacts.ArtifactView
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Attribute
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.register
import java.io.File

/**
 * Base class for analyzing an Android project (com.android.application or com.android.library only).
 */
internal abstract class AndroidAnalyzer<T : ClassAnalysisTask>(
  project: Project,
  protected val variant: BaseVariant,
  protected val variantSourceSet: VariantSourceSet,
  agpVersion: String
) : AbstractDependencyAnalyzer<T>(project) {

  protected val agp = AndroidGradlePluginFactory(project, agpVersion).newAdapter()
  private val dataBindingEnabled = agp.isDataBindingEnabled()
  private val viewBindingEnabled = agp.isViewBindingEnabled()

  final override val flavorName: String = variant.flavorName
  final override val variantName: String = variant.name
  final override val variantNameCapitalized: String = variantName.capitalizeSafely()
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

  protected val outputPaths = OutputPaths(project, variantName)

  // For AGP 3.5.x, this does not return any module dependencies
  override val attributeValueRes = "android-symbol-with-package-name"

  private val manifestArtifactView: Action<in ArtifactView.ViewConfiguration> =
    Action<ArtifactView.ViewConfiguration> {
      attributes.attribute(AndroidArtifacts.ARTIFACT_TYPE, "android-manifest")
    }

  final override val testJavaCompileName: String = "compile${variantNameCapitalized}UnitTestJavaWithJavac"
  final override val testKotlinCompileName: String = "compile${variantNameCapitalized}UnitTestKotlin"

  protected fun getKaptStubs() = getKaptStubs(project, variantName)

  override fun registerManifestPackageExtractionTask(): TaskProvider<ManifestPackageExtractionTask> =
    project.tasks.register<ManifestPackageExtractionTask>("extractPackageNameFromManifest$variantNameCapitalized") {
      setArtifacts(project.configurations[compileConfigurationName].incoming.artifactView(manifestArtifactView).artifacts)
      manifestPackagesReport.set(outputPaths.manifestPackagesPath)
    }

  override fun registerAndroidResToSourceAnalysisTask(
    manifestPackageExtractionTask: TaskProvider<ManifestPackageExtractionTask>
  ): TaskProvider<AndroidResToSourceAnalysisTask> =
    project.tasks.register<AndroidResToSourceAnalysisTask>(
      "findAndroidResBySourceUsage$variantNameCapitalized"
    ) {
      val resourceArtifacts = project.configurations[compileConfigurationName]
        .incoming.artifactView {
          attributes.attribute(attribute, attributeValueRes)
        }.artifacts

      manifestPackages.set(manifestPackageExtractionTask.flatMap { it.manifestPackagesReport })
      setResources(resourceArtifacts)
      javaAndKotlinSourceFiles.setFrom(this@AndroidAnalyzer.javaAndKotlinSourceFiles)

      usedAndroidResDependencies.set(outputPaths.androidResToSourceUsagePath)
    }

  override fun registerAndroidResToResAnalysisTask(): TaskProvider<AndroidResToResToResAnalysisTask> {
    return project.tasks.register<AndroidResToResToResAnalysisTask>(
      "findAndroidResByResUsage$variantNameCapitalized"
    ) {
      setAndroidPublicRes(project.configurations[compileConfigurationName]
        .incoming.artifactView {
          attributes.attribute(attribute, "android-public-res")
        }.artifacts)
      androidLocalRes.setFrom(getAndroidRes())

      output.set(outputPaths.androidResToResUsagePath)
    }
  }

  override fun registerFindNativeLibsTask(
    locateDependenciesTask: TaskProvider<LocateDependenciesTask>
  ): TaskProvider<FindNativeLibsTask>? {
    return project.tasks.register<FindNativeLibsTask>("findNativeLibs$variantNameCapitalized") {
      val jni = project.configurations[compileConfigurationName].incoming.artifactView {
        attributes.attribute(attribute, "android-jni")
      }.artifacts
      setArtifacts(jni)
      dependencyConfigurations.set(locateDependenciesTask.flatMap { it.output })

      output.set(outputPaths.nativeDependenciesPath)
    }
  }

  override fun registerFindDeclaredProcsTask(
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

  private fun kaptConf(): Configuration? = try {
    project.configurations["kapt$variantNameCapitalized"]
  } catch (_: UnknownDomainObjectException) {
    null
  }

  private fun annotationProcessorConf(): Configuration? = try {
    project.configurations["${variantName}AnnotationProcessorClasspath"]
  } catch (_: UnknownDomainObjectException) {
    null
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
    // Java dirs regardless of whether they exist
    val javaDirs = variant.sourceSets.flatMap {
      it.javaDirectories
    }

    // Kotlin dirs, only if they exist. If we filtered the above for existence, and there was no
    // Java dir, then this would also be empty.
    val kotlinDirs = javaDirs
      .map { it.path }
      .map { it.removeSuffix("java") + "kotlin" }
      .map { File(it) }
      .filter { it.exists() }

    // Now finally filter Java dirs for existence
    return project.files(javaDirs.filter { it.exists() } + kotlinDirs)
  }

  private fun getAndroidRes(): FileTree {
    val resDirs = variant.sourceSets.flatMap {
      it.resDirectories
    }.filter { it.exists() }

    return project.files(resDirs).asFileTree.matching {
      include("**/*.xml")
    }
  }

  protected val variantFiles: Set<VariantFile> by lazy(mode = LazyThreadSafetyMode.NONE) {
    val androidSourceSets = variantSourceSet.androidSourceSets
    val kotlinSourceSets = variantSourceSet.kotlinSourceSets ?: emptySet()

    val javaVariantFiles = androidSourceSets.flatMapToSet { sourceSet ->
      project.files(sourceSet.javaDirectories).asFileTree.files.toVariantFiles(sourceSet.name)
    }

    val kotlinVariantFiles = kotlinSourceSets.flatMapToSet { sourceSet ->
      project.files(sourceSet.kotlin.srcDirs).asFileTree.files.toVariantFiles(sourceSet.name)
    }

    val xmlVariantFiles = androidSourceSets.flatMapToSet { sourceSet ->
      project.files(sourceSet.resDirectories).asFileTree.files.toVariantFiles(sourceSet.name)
    }

    // return
    javaVariantFiles + kotlinVariantFiles + xmlVariantFiles
  }

  private fun Set<File>.toVariantFiles(name: String): Set<VariantFile> {
    return asSequence().map { file ->
      project.relativePath(file)
    }.map { it.removePrefix("src/$name/") }
      // remove java/, kotlin/ and /res from start
      .map { it.substring(it.indexOf("/") + 1) }
      // remove file extension from end
      .mapNotNull {
        val index = it.lastIndexOf(".")
        if (index != -1) {
          it.substring(0, index)
        } else {
          // This could happen if the directory were empty, (eg `src/main/java/` with nothing in it)
          null
        }
      }
      .map { VariantFile(name, it) }
      .toSet()
  }
}

internal class AndroidAppAnalyzer(
  project: Project, variant: BaseVariant, agpVersion: String, variantSourceSet: VariantSourceSet
) : AndroidAnalyzer<ClassListAnalysisTask>(
  project = project,
  variant = variant,
  variantSourceSet = variantSourceSet,
  agpVersion = agpVersion
) {

  override fun registerClassAnalysisTask(): TaskProvider<ClassListAnalysisTask> {
    return project.tasks.register<ClassListAnalysisTask>("analyzeClassUsage$variantNameCapitalized") {
      kotlinCompileTask()?.let { kotlinClasses.from(it.get().outputs.files.asFileTree) }
      javaClasses.from(javaCompileTask().get().outputs.files.asFileTree)
      variantFiles.set(this@AndroidAppAnalyzer.variantFiles)
      kaptJavaStubs.from(getKaptStubs())
      testJavaCompile?.let { javaCompile ->
        testJavaClassesDir.set(javaCompile.flatMap { it.destinationDirectory })
      }
      testKotlinCompile?.let { kotlinCompile ->
        testKotlinClassesDir.set(kotlinCompile.flatMap { it.destinationDirectory })
      }
      layouts(variant.sourceSets.flatMap { it.resDirectories })

      output.set(outputPaths.allUsedClassesPath)
      outputPretty.set(outputPaths.allUsedClassesPrettyPath)
    }
  }

  override fun registerFindUnusedProcsTask(
    findDeclaredProcs: TaskProvider<FindDeclaredProcsTask>,
    importFinder: TaskProvider<ImportFinderTask>
  ): TaskProvider<FindUnusedProcsTask> {
    return project.tasks.register<FindUnusedProcsTask>(
      "findUnusedProcs$variantNameCapitalized"
    ) {
      kotlinCompileTask()?.let { kotlinClasses.from(it.get().outputs.files.asFileTree) }
      javaClasses.from(javaCompileTask().get().outputs.files.asFileTree)
      imports.set(importFinder.flatMap { it.importsReport })
      annotationProcessorsProperty.set(findDeclaredProcs.flatMap { it.output })

      output.set(outputPaths.unusedProcPath)
    }
  }

  // Known to exist in Kotlin 1.3.61.
  private fun kotlinCompileTask() =
    project.tasks.namedOrNull("compile${variantNameCapitalized}Kotlin")

  // Known to exist in AGP 3.5, 3.6, and 4.0, albeit with different backing classes (AndroidJavaCompile,
  // JavaCompile)
  private fun javaCompileTask() =
    project.tasks.named("compile${variantNameCapitalized}JavaWithJavac")
}

internal class AndroidLibAnalyzer(
  project: Project, variant: BaseVariant, agpVersion: String, variantSourceSet: VariantSourceSet
) : AndroidAnalyzer<JarAnalysisTask>(
  project = project,
  variant = variant,
  variantSourceSet = variantSourceSet,
  agpVersion = agpVersion
) {

  override fun registerClassAnalysisTask(): TaskProvider<JarAnalysisTask> =
    project.tasks.register<JarAnalysisTask>("analyzeClassUsage$variantNameCapitalized") {
      variantFiles.set(this@AndroidLibAnalyzer.variantFiles)
      jar.set(getBundleTaskOutput())
      testJavaCompile?.let { javaCompile ->
        testJavaClassesDir.set(javaCompile.flatMap { it.destinationDirectory })
      }
      testKotlinCompile?.let { kotlinCompile ->
        testKotlinClassesDir.set(kotlinCompile.flatMap { it.destinationDirectory })
      }
      kaptJavaStubs.from(getKaptStubs())
      layouts(variant.sourceSets.flatMap { it.resDirectories })

      output.set(outputPaths.allUsedClassesPath)
      outputPretty.set(outputPaths.allUsedClassesPrettyPath)
    }

  override fun registerAbiAnalysisTask(
    dependencyReportTask: TaskProvider<DependencyReportTask>,
    abiExclusions: Provider<String>
  ): TaskProvider<AbiAnalysisTask> =
    project.tasks.register<AbiAnalysisTask>("abiAnalysis$variantNameCapitalized") {
      jar.set(getBundleTaskOutput())
      dependencies.set(dependencyReportTask.flatMap { it.allComponentsReport })
      exclusions.set(abiExclusions)

      output.set(outputPaths.abiAnalysisPath)
      abiDump.set(outputPaths.abiDumpPath)
    }

  override fun registerFindUnusedProcsTask(
    findDeclaredProcs: TaskProvider<FindDeclaredProcsTask>,
    importFinder: TaskProvider<ImportFinderTask>
  ): TaskProvider<FindUnusedProcsTask> {
    return project.tasks.register<FindUnusedProcsTask>(
      "findUnusedProcs$variantNameCapitalized"
    ) {
      jar.set(getBundleTaskOutput())
      imports.set(importFinder.flatMap { it.importsReport })
      annotationProcessorsProperty.set(findDeclaredProcs.flatMap { it.output })

      output.set(outputPaths.unusedProcPath)
    }
  }

  private fun getBundleTaskOutput(): Provider<RegularFile> =
    agp.getBundleTaskOutput(variantNameCapitalized)
}
