@file:Suppress("UnstableApiUsage")

package com.autonomousapps.internal.analyzer

import com.android.build.gradle.api.BaseVariant
import com.autonomousapps.internal.ArtifactAttributes
import com.autonomousapps.internal.OutputPaths
import com.autonomousapps.internal.android.AndroidGradlePluginFactory
import com.autonomousapps.internal.artifactsFor
import com.autonomousapps.internal.utils.capitalizeSafely
import com.autonomousapps.internal.utils.namedOrNull
import com.autonomousapps.services.InMemoryCache
import com.autonomousapps.tasks.*
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.UnknownDomainObjectException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSet
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
  final override val sourceSetName: String = variantSourceSet.variant.sourceSetName
  final override val variantNameCapitalized: String = variantName.capitalizeSafely()
  final override val taskNameSuffix: String = computeTaskNameSuffix()
  final override val compileConfigurationName = variantSourceSet.compileClasspathConfigurationName
  final override val kaptConfigurationName = "kapt$variantNameCapitalized"
  final override val annotationProcessorConfigurationName = "${variantName}AnnotationProcessorClasspath"
  final override val kotlinSourceFiles: FileTree = getKotlinSources()//kotlinSource()//
  final override val javaSourceFiles: FileTree = getJavaSources()//javaSource()//
  final override val javaAndKotlinSourceFiles: FileTree = getJavaAndKotlinSources()

  // TODO looks like this will break with AGP >4. Seriously, check this against 7+
  final override val attributeValueJar =
    if (agpVersion.startsWith("4.")) ArtifactAttributes.ANDROID_CLASSES_JAR_4
    else ArtifactAttributes.ANDROID_CLASSES_JAR

  final override val isDataBindingEnabled: Boolean = dataBindingEnabled
  final override val isViewBindingEnabled: Boolean = viewBindingEnabled

  final override val outputPaths = OutputPaths(project, "$variantName${sourceSetName.capitalizeSafely()}")

  final override val testJavaCompileName: String = "compile${variantNameCapitalized}UnitTestJavaWithJavac"
  final override val testKotlinCompileName: String = "compile${variantNameCapitalized}UnitTestKotlin"

  override fun registerManifestComponentsExtractionTask(): TaskProvider<ManifestComponentsExtractionTask> {
    return project.tasks.register<ManifestComponentsExtractionTask>(
      "extractPackageNameFromManifest$taskNameSuffix"
    ) {
      setArtifacts(project.configurations[compileConfigurationName].artifactsFor("android-manifest"))
      output.set(outputPaths.manifestPackagesPath)
    }
  }

  override fun registerFindAndroidResTask(): TaskProvider<FindAndroidResTask> {
    return project.tasks.register<FindAndroidResTask>("findAndroidResImports$taskNameSuffix") {
      setAndroidSymbols(
        project.configurations[compileConfigurationName].artifactsFor("android-symbol-with-package-name")
      )
      setAndroidPublicRes(project.configurations[compileConfigurationName].artifactsFor("android-public-res"))
      output.set(outputPaths.androidResPath)
    }
  }

  override fun registerExplodeXmlSourceTask(): TaskProvider<XmlSourceExploderTask> {
    return project.tasks.register<XmlSourceExploderTask>("explodeXmlSource$taskNameSuffix") {
      androidLocalRes.setFrom(getAndroidRes())
      layouts(variant.sourceSets.flatMap { it.resDirectories })
      output.set(outputPaths.androidResToResUsagePath)
    }
  }

  override fun registerFindNativeLibsTask(): TaskProvider<FindNativeLibsTask> {
    return project.tasks.register<FindNativeLibsTask>("findNativeLibs$taskNameSuffix") {
      setAndroidJni(project.configurations[compileConfigurationName].artifactsFor(ArtifactAttributes.ANDROID_JNI))
      output.set(outputPaths.nativeDependenciesPath)
    }
  }

  override fun registerFindAndroidLintersTask(): TaskProvider<FindAndroidLinters> =
    project.tasks.register<FindAndroidLinters>("findAndroidLinters$taskNameSuffix") {
      setLintJars(project.configurations[compileConfigurationName].artifactsFor(ArtifactAttributes.ANDROID_LINT))
      output.set(outputPaths.androidLintersPath)
    }

  override fun registerFindDeclaredProcsTask(
    inMemoryCache: Provider<InMemoryCache>,
  ): TaskProvider<FindDeclaredProcsTask> {
    return project.tasks.register<FindDeclaredProcsTask>("findDeclaredProcs$taskNameSuffix") {
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

  // Known to exist in Kotlin 1.3.61.
  protected fun kotlinCompileTask(): TaskProvider<Task>? {
    if (variantSourceSet.variant.sourceSetName == SourceSet.TEST_SOURCE_SET_NAME) {
      return project.tasks.namedOrNull("compile${variantNameCapitalized}UnitTestKotlin")
    }
    return project.tasks.namedOrNull("compile${variantNameCapitalized}Kotlin")
  }

  // Known to exist in AGP 3.5, 3.6, and 4.0, albeit with different backing classes (AndroidJavaCompile,
  // JavaCompile)
  protected fun javaCompileTask(): TaskProvider<Task> {
    if (variantSourceSet.variant.sourceSetName == SourceSet.TEST_SOURCE_SET_NAME) {
      return project.tasks.named("compile${variantNameCapitalized}UnitTestJavaWithJavac")
    }
    return project.tasks.named("compile${variantNameCapitalized}JavaWithJavac")
  }

  private fun computeTaskNameSuffix(): String {
    return if (variantSourceSet.variant.sourceSetName == SourceSet.MAIN_SOURCE_SET_NAME) {
      // "flavorDebug" -> "FlavorDebug"
      variantName.capitalizeSafely()
    } else {
      // "flavorDebug" + "Test" -> "FlavorDebugTest"
      variantName.capitalizeSafely() + variantSourceSet.variant.sourceSetName.capitalizeSafely()
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

  private fun javaSource(): FileTree {
    return source().matching {
      include("**/*.java")
      exclude("**/*.kt")
    }
  }

  private fun kotlinSource(): FileTree = source().matching {
    include("**/*.kt")
    exclude("**/*.java")
  }

  private fun javaAndKotlinSource(): FileTree = source().matching {
    include("**/*.java")
    include("**/*.kt")
  }

  private fun source(): FileTree = variant.sourceSets
    .flatMap { it.javaDirectories }
    .map { project.fileTree(it) }
    .reduce(FileTree::plus)

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
    val javaDirs = variantSourceSet.androidSourceSets.flatMap { it.javaDirectories }

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
}

internal class AndroidAppAnalyzer(
  project: Project,
  variant: BaseVariant,
  agpVersion: String,
  variantSourceSet: VariantSourceSet
) : AndroidAnalyzer(
  project = project,
  variant = variant,
  variantSourceSet = variantSourceSet,
  agpVersion = agpVersion
) {

  override fun registerByteCodeSourceExploderTask(): TaskProvider<ClassListExploderTask> {
    return project.tasks.register<ClassListExploderTask>("explodeByteCodeSource$taskNameSuffix") {
      kotlinCompileTask()?.let { kotlinClasses.from(it.get().outputs.files.asFileTree) }
      javaClasses.from(javaCompileTask().get().outputs.files.asFileTree)
      output.set(outputPaths.explodingBytecodePath)
    }
  }
}

internal class AndroidLibAnalyzer(
  project: Project, variant: BaseVariant, agpVersion: String, variantSourceSet: VariantSourceSet
) : AndroidAnalyzer(
  project = project,
  variant = variant,
  variantSourceSet = variantSourceSet,
  agpVersion = agpVersion
) {

  override fun registerByteCodeSourceExploderTask(): TaskProvider<ClassListExploderTask> {
    return project.tasks.register<ClassListExploderTask>("explodeByteCodeSource$taskNameSuffix") {
      kotlinCompileTask()?.let { kotlinClasses.from(it.get().outputs.files.asFileTree) }
      javaClasses.from(javaCompileTask().get().outputs.files.asFileTree)
      output.set(outputPaths.explodingBytecodePath)
    }
  }

  override fun registerAbiAnalysisTask(abiExclusions: Provider<String>): TaskProvider<AbiAnalysisTask> {
    return project.tasks.register<AbiAnalysisTask>("abiAnalysis$taskNameSuffix") {
      jar.set(getBundleTaskOutput())
      exclusions.set(abiExclusions)
      output.set(outputPaths.abiAnalysisPath)
      abiDump.set(outputPaths.abiDumpPath)
    }
  }

  private fun getBundleTaskOutput(): Provider<RegularFile> = agp.getBundleTaskOutput(variantNameCapitalized)
}
