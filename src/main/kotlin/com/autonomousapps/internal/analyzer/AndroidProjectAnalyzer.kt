@file:Suppress("UnstableApiUsage")

package com.autonomousapps.internal.analyzer

import com.android.build.gradle.api.BaseVariant
import com.autonomousapps.internal.ArtifactAttributes
import com.autonomousapps.internal.OutputPaths
import com.autonomousapps.internal.android.AndroidGradlePluginFactory
import com.autonomousapps.internal.artifactsFor
import com.autonomousapps.internal.utils.capitalizeSafely
import com.autonomousapps.internal.utils.namedOrNull
import com.autonomousapps.model.declaration.SourceSetKind
import com.autonomousapps.services.InMemoryCache
import com.autonomousapps.tasks.*
import org.gradle.api.Project
import org.gradle.api.Task
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
  final override val kind: SourceSetKind = variantSourceSet.variant.kind
  final override val variantNameCapitalized: String = variantName.capitalizeSafely()
  final override val taskNameSuffix: String = computeTaskNameSuffix()
  final override val compileConfigurationName = variantSourceSet.compileClasspathConfigurationName
  final override val runtimeConfigurationName = variantSourceSet.runtimeClasspathConfigurationName
  final override val kaptConfigurationName = "kapt$variantNameCapitalized"
  final override val annotationProcessorConfigurationName = "${variantName}AnnotationProcessorClasspath"
  final override val kotlinSourceFiles: FileTree = getKotlinSources()
  final override val javaSourceFiles: FileTree = getJavaSources()
  final override val groovySourceFiles: FileTree = getGroovySources()
  final override val scalaSourceFiles: FileTree = getScalaSources()

  // TODO looks like this will break with AGP >4. Seriously, check this against 7+
  final override val attributeValueJar =
    if (agpVersion.startsWith("4.")) ArtifactAttributes.ANDROID_CLASSES_JAR_4
    else ArtifactAttributes.ANDROID_CLASSES_JAR

  final override val isDataBindingEnabled: Boolean = dataBindingEnabled
  final override val isViewBindingEnabled: Boolean = viewBindingEnabled

  final override val outputPaths = OutputPaths(project, "$variantName${kind.taskNameSuffix}")

  final override val testJavaCompileName: String = "compile${variantNameCapitalized}UnitTestJavaWithJavac"
  final override val testKotlinCompileName: String = "compile${variantNameCapitalized}UnitTestKotlin"

  final override fun registerByteCodeSourceExploderTask(): TaskProvider<ClassListExploderTask> {
    return project.tasks.register<ClassListExploderTask>("explodeByteCodeSource$taskNameSuffix") {
      classes.setFrom(project.files())
      kotlinCompileTask()?.let { kotlinClasses.from(it.get().outputs.files.asFileTree) }
      javaClasses.from(javaCompileTask().get().outputs.files.asFileTree)

      output.set(outputPaths.explodingBytecodePath)
    }
  }

  final override fun registerManifestComponentsExtractionTask(): TaskProvider<ManifestComponentsExtractionTask> {
    return project.tasks.register<ManifestComponentsExtractionTask>(
      "extractPackageNameFromManifest$taskNameSuffix"
    ) {
      setArtifacts(project.configurations[compileConfigurationName].artifactsFor("android-manifest"))
      output.set(outputPaths.manifestPackagesPath)
    }
  }

  final override fun registerFindAndroidResTask(): TaskProvider<FindAndroidResTask> {
    return project.tasks.register<FindAndroidResTask>("findAndroidResImports$taskNameSuffix") {
      setAndroidSymbols(
        project.configurations[compileConfigurationName].artifactsFor("android-symbol-with-package-name")
      )
      setAndroidPublicRes(project.configurations[compileConfigurationName].artifactsFor("android-public-res"))
      output.set(outputPaths.androidResPath)
    }
  }

  final override fun registerExplodeXmlSourceTask(): TaskProvider<XmlSourceExploderTask> {
    return project.tasks.register<XmlSourceExploderTask>("explodeXmlSource$taskNameSuffix") {
      androidLocalRes.setFrom(getAndroidRes())
      layouts(variant.sourceSets.flatMap { it.resDirectories })
      manifestFiles.setFrom(variant.sourceSets.map { it.manifestFile })
      output.set(outputPaths.androidResToResUsagePath)
    }
  }

  final override fun registerFindNativeLibsTask(): TaskProvider<FindNativeLibsTask> {
    return project.tasks.register<FindNativeLibsTask>("findNativeLibs$taskNameSuffix") {
      setAndroidJni(project.configurations[compileConfigurationName].artifactsFor(ArtifactAttributes.ANDROID_JNI))
      output.set(outputPaths.nativeDependenciesPath)
    }
  }

  final override fun registerFindAndroidLintersTask(): TaskProvider<FindAndroidLinters> =
    project.tasks.register<FindAndroidLinters>("findAndroidLinters$taskNameSuffix") {
      setLintJars(project.configurations[compileConfigurationName].artifactsFor(ArtifactAttributes.ANDROID_LINT))
      output.set(outputPaths.androidLintersPath)
    }

  final override fun registerFindAndroidAssetProvidersTask(): TaskProvider<FindAndroidAssetProviders> =
    project.tasks.register<FindAndroidAssetProviders>("findAndroidAssetProviders$taskNameSuffix") {
      setAssets(project.configurations[runtimeConfigurationName].artifactsFor(ArtifactAttributes.ANDROID_ASSETS))
      output.set(outputPaths.androidAssetsPath)
    }

  final override fun registerFindDeclaredProcsTask(
    inMemoryCache: Provider<InMemoryCache>,
  ): TaskProvider<FindDeclaredProcsTask> =
    project.tasks.register<FindDeclaredProcsTask>("findDeclaredProcs$taskNameSuffix") {
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

  // Known to exist in Kotlin 1.3.61.
  private fun kotlinCompileTask(): TaskProvider<Task>? {
    return when (variantSourceSet.variant.kind) {
      SourceSetKind.MAIN -> project.tasks.namedOrNull("compile${variantNameCapitalized}Kotlin")
      SourceSetKind.TEST -> project.tasks.namedOrNull("compile${variantNameCapitalized}UnitTestKotlin")
    }
  }

  // Known to exist in AGP 3.5, 3.6, and 4.0, albeit with different backing classes (AndroidJavaCompile,
  // JavaCompile)
  private fun javaCompileTask(): TaskProvider<Task> {
    return when (variantSourceSet.variant.kind) {
      SourceSetKind.MAIN -> project.tasks.named("compile${variantNameCapitalized}JavaWithJavac")
      SourceSetKind.TEST -> project.tasks.named("compile${variantNameCapitalized}UnitTestJavaWithJavac")
    }
  }

  private fun computeTaskNameSuffix(): String {
    return if (variantSourceSet.variant.kind == SourceSetKind.MAIN) {
      // "flavorDebug" -> "FlavorDebug"
      variantName.capitalizeSafely()
    } else {
      // "flavorDebug" + "Test" -> "FlavorDebugTest"
      variantName.capitalizeSafely() + variantSourceSet.variant.kind.taskNameSuffix
    }
  }

  private fun getGroovySources(): FileTree = getSourceDirectories().matching(Language.filterOf(Language.GROOVY))
  private fun getJavaSources(): FileTree = getSourceDirectories().matching(Language.filterOf(Language.JAVA))
  private fun getKotlinSources(): FileTree = getSourceDirectories().matching(Language.filterOf(Language.KOTLIN))
  private fun getScalaSources(): FileTree = getSourceDirectories().matching(Language.filterOf(Language.SCALA))

  private fun getSourceDirectories(): FileTree {
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
    return project.files(javaDirs.filter { it.exists() } + kotlinDirs).asFileTree
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
)

internal class AndroidLibAnalyzer(
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

  override fun registerAbiAnalysisTask(abiExclusions: Provider<String>): TaskProvider<AbiAnalysisTask> {
    return project.tasks.register<AbiAnalysisTask>("abiAnalysis$taskNameSuffix") {
      jar.set(getBundleTaskOutput())
      exclusions.set(abiExclusions)
      output.set(outputPaths.abiAnalysisPath)
      abiDump.set(outputPaths.abiDumpPath)
    }
  }

  // TODO stop using bundleTask directly. Fragile.
  private fun getBundleTaskOutput(): Provider<RegularFile> = agp.getBundleTaskOutput(variantNameCapitalized)
}
