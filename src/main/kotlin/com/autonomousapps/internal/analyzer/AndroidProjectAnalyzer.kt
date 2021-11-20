@file:Suppress("UnstableApiUsage")

package com.autonomousapps.internal.analyzer

import com.android.build.gradle.api.BaseVariant
import com.autonomousapps.internal.OutputPaths
import com.autonomousapps.internal.android.AndroidGradlePluginFactory
import com.autonomousapps.internal.artifactViewFor
import com.autonomousapps.internal.utils.capitalizeSafely
import com.autonomousapps.internal.utils.namedOrNull
import com.autonomousapps.services.InMemoryCache
import com.autonomousapps.shouldAnalyzeTests
import com.autonomousapps.tasks.*
import org.gradle.api.Project
import org.gradle.api.UnknownDomainObjectException
import org.gradle.api.artifacts.Configuration
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
internal abstract class AndroidAnalyzer(
  project: Project,
  protected val variant: BaseVariant,
  protected val variantSourceSet: VariantSourceSet,
  agpVersion: String
) : AbstractDependencyAnalyzer(project) {

  protected val agp = AndroidGradlePluginFactory(project, agpVersion).newAdapter()
  private val dataBindingEnabled = agp.isDataBindingEnabled()
  private val viewBindingEnabled = agp.isViewBindingEnabled()

  final override val flavorName: String = variant.flavorName
  final override val variantName: String = variant.name
  final override val buildType: String = variant.buildType.name
  final override val variantNameCapitalized: String = variantName.capitalizeSafely()
  final override val compileConfigurationName = "${variantName}CompileClasspath"
  final override val testCompileConfigurationName = "${variantName}UnitTestCompileClasspath"
  final override val kotlinSourceFiles: FileTree = getKotlinSources()
  final override val javaSourceFiles: FileTree = getJavaSources()
  final override val javaAndKotlinSourceFiles: FileTree = getJavaAndKotlinSources()

  // TODO looks like this will break with AGP >4.
  final override val attributeValueJar =
    if (agpVersion.startsWith("4.")) "android-classes-jar"
    else "android-classes"

  final override val isDataBindingEnabled: Boolean = dataBindingEnabled
  final override val isViewBindingEnabled: Boolean = viewBindingEnabled

  protected val outputPaths = OutputPaths(project, variantName)

  final override val testJavaCompileName: String = "compile${variantNameCapitalized}UnitTestJavaWithJavac"
  final override val testKotlinCompileName: String = "compile${variantNameCapitalized}UnitTestKotlin"

  override fun registerManifestPackageExtractionTask(): TaskProvider<ManifestPackageExtractionTask> =
    project.tasks.register<ManifestPackageExtractionTask>(
      "extractPackageNameFromManifest$variantNameCapitalized"
    ) {
      setArtifacts(
        project.configurations[compileConfigurationName]
          .incoming
          .artifactViewFor("android-manifest")
          .artifacts
      )
      output.set(outputPaths.manifestPackagesPath)
    }

  override fun registerAndroidResToSourceAnalysisTask(
    manifestPackageExtractionTask: TaskProvider<ManifestPackageExtractionTask>
  ): TaskProvider<AndroidResToSourceAnalysisTask> =
    project.tasks.register<AndroidResToSourceAnalysisTask>(
      "findAndroidResBySourceUsage$variantNameCapitalized"
    ) {
      val resourceArtifacts = project.configurations[compileConfigurationName]
        .incoming
        // For AGP 3.5.x, this does not return any module dependencies
        .artifactViewFor("android-symbol-with-package-name")
        .artifacts

      manifestPackages.set(manifestPackageExtractionTask.flatMap { it.output })
      setResources(resourceArtifacts)
      javaAndKotlinSourceFiles.setFrom(this@AndroidAnalyzer.javaAndKotlinSourceFiles)

      output.set(outputPaths.androidResToSourceUsagePath)
    }

  override fun registerAndroidResToResAnalysisTask(): TaskProvider<AndroidResToResToResAnalysisTask> {
    return project.tasks.register<AndroidResToResToResAnalysisTask>(
      "findAndroidResByResUsage$variantNameCapitalized"
    ) {
      setAndroidPublicRes(
        project.configurations[compileConfigurationName]
          .incoming
          .artifactViewFor("android-public-res")
          .artifacts
      )
      setAndroidSymbols(
        project.configurations[compileConfigurationName]
          .incoming
          .artifactViewFor("android-symbol-with-package-name")
          .artifacts
      )

      androidLocalRes.setFrom(getAndroidRes())

      output.set(outputPaths.androidResToResUsagePath)
    }
  }

  override fun registerFindNativeLibsTask(
    locateDependenciesTask: TaskProvider<LocateDependenciesTask>
  ): TaskProvider<FindNativeLibsTask>? {
    return project.tasks.register<FindNativeLibsTask>("findNativeLibs$variantNameCapitalized") {
      val jni = project.configurations[compileConfigurationName]
        .incoming
        .artifactViewFor("android-jni")
        .artifacts
      setArtifacts(jni)
      locations.set(locateDependenciesTask.flatMap { it.output })

      output.set(outputPaths.nativeDependenciesPath)
    }
  }

  override fun registerFindAndroidLintersTask(
    locateDependenciesTask: TaskProvider<LocateDependenciesTask>
  ): TaskProvider<FindAndroidLinters>? =
    project.tasks.register<FindAndroidLinters>("findAndroidLinters$variantNameCapitalized") {
      locations.set(locateDependenciesTask.flatMap { it.output })
      setLintJars(
        project.configurations[compileConfigurationName]
          .incoming
          .artifactViewFor("android-lint")
          .artifacts
      )

      output.set(outputPaths.androidLintersPath)
    }

  override fun registerFindDeclaredProcsTask(
    inMemoryCacheProvider: Provider<InMemoryCache>,
    locateDependenciesTask: TaskProvider<LocateDependenciesTask>
  ): TaskProvider<FindDeclaredProcsTask> =
    project.tasks.register<FindDeclaredProcsTask>("findDeclaredProcs$variantNameCapitalized") {
      this.inMemoryCacheProvider.set(inMemoryCacheProvider)
      locations.set(locateDependenciesTask.flatMap { it.output })

      kaptConf()?.let {
        setKaptArtifacts(it.incoming.artifacts)
      }
      annotationProcessorConf()?.let {
        setAnnotationProcessorArtifacts(it.incoming.artifacts)
      }

      output.set(outputPaths.declaredProcPath)
      outputPretty.set(outputPaths.declaredProcPrettyPath)
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

  override fun registerCreateVariantFilesTask(): TaskProvider<AndroidCreateVariantFiles> {
    return project.tasks.register<AndroidCreateVariantFiles>("createVariantFiles$variantNameCapitalized") {
      val androidSourceSets = variantSourceSet.androidSourceSets
      val kotlinSourceSets = variantSourceSet.kotlinSourceSets ?: emptySet()

      val namedJavaDirs = mutableMapOf<String, CollectionHolder>()
      val namedXmlDirs = mutableMapOf<String, CollectionHolder>()
      androidSourceSets.forEach {
        namedJavaDirs[it.name] = CollectionHolder(project.files(it.javaDirectories))
        namedXmlDirs[it.name] = CollectionHolder(project.files(it.resDirectories))
      }
      val namedKotlinDirs = kotlinSourceSets.map {
        it.name to CollectionHolder(project.files(it.kotlin.srcDirs))
      }.toMap()

      this.namedJavaDirs.putAll(namedJavaDirs)
      this.namedKotlinDirs.putAll(namedKotlinDirs)
      this.namedXmlDirs.putAll(namedXmlDirs)

      output.set(outputPaths.variantFilesPath)
    }
  }
}

internal class AndroidAppAnalyzer(
  project: Project, variant: BaseVariant, agpVersion: String, variantSourceSet: VariantSourceSet
) : AndroidAnalyzer(
  project = project,
  variant = variant,
  variantSourceSet = variantSourceSet,
  agpVersion = agpVersion
) {

  override fun registerClassAnalysisTask(createVariantFiles: TaskProvider<out CreateVariantFiles>): TaskProvider<ClassListAnalysisTask> {
    return project.tasks.register<ClassListAnalysisTask>("analyzeClassUsage$variantNameCapitalized") {
      variantFiles.set(createVariantFiles.flatMap { it.output })

      kotlinCompileTask()?.let { kotlinClasses.from(it.get().outputs.files.asFileTree) }
      javaClasses.from(javaCompileTask().get().outputs.files.asFileTree)

      if (project.shouldAnalyzeTests()) {
        testJavaCompile?.let { javaCompile ->
          testJavaClassesDir.set(javaCompile.flatMap { it.destinationDirectory })
        }
        testKotlinCompile?.let { kotlinCompile ->
          testKotlinClassesDir.set(kotlinCompile.flatMap { it.destinationDirectory })
        }
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
) : AndroidAnalyzer(
  project = project,
  variant = variant,
  variantSourceSet = variantSourceSet,
  agpVersion = agpVersion
) {

  override fun registerClassAnalysisTask(createVariantFiles: TaskProvider<out CreateVariantFiles>): TaskProvider<JarAnalysisTask> =
    project.tasks.register<JarAnalysisTask>("analyzeClassUsage$variantNameCapitalized") {
      variantFiles.set(createVariantFiles.flatMap { it.output })

      jar.set(getBundleTaskOutput())

      if (project.shouldAnalyzeTests()) {
        testJavaCompile?.let { javaCompile ->
          testJavaClassesDir.set(javaCompile.flatMap { it.destinationDirectory })
        }
        testKotlinCompile?.let { kotlinCompile ->
          testKotlinClassesDir.set(kotlinCompile.flatMap { it.destinationDirectory })
        }
      }

      layouts(variant.sourceSets.flatMap { it.resDirectories })

      output.set(outputPaths.allUsedClassesPath)
      outputPretty.set(outputPaths.allUsedClassesPrettyPath)
    }

  override fun registerAbiAnalysisTask(
    analyzeJarTask: TaskProvider<AnalyzeJarTask>,
    abiExclusions: Provider<String>
  ): TaskProvider<AbiAnalysisTask> =
    project.tasks.register<AbiAnalysisTask>("abiAnalysis$variantNameCapitalized") {
      jar.set(getBundleTaskOutput())
      dependencies.set(analyzeJarTask.flatMap { it.allComponentsReport })
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

  private fun getBundleTaskOutput(): Provider<RegularFile> = agp.getBundleTaskOutput(variantNameCapitalized)
}
