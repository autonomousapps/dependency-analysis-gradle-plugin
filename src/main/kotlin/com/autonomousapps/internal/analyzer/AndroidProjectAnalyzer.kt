// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("UnstableApiUsage")

package com.autonomousapps.internal.analyzer

import com.autonomousapps.internal.ArtifactAttributes
import com.autonomousapps.internal.OutputPaths
import com.autonomousapps.internal.android.AndroidGradlePluginFactory
import com.autonomousapps.internal.artifactsFor
import com.autonomousapps.internal.utils.capitalizeSafely
import com.autonomousapps.model.declaration.SourceSetKind
import com.autonomousapps.services.InMemoryCache
import com.autonomousapps.tasks.*
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.register
import java.io.File

/** Base class for analyzing an Android project (`com.android.application` or `com.android.library` only). */
internal abstract class AndroidAnalyzer(
  project: Project,
  protected val variant: AndroidVariant,
  protected val androidSources: AndroidSources,
  agpVersion: String,
) : AbstractDependencyAnalyzer(project) {

  protected val agp = AndroidGradlePluginFactory(project, agpVersion).newAdapter()

  final override val flavorName: String = variant.flavorName
  final override val variantName: String = variant.variantName
  final override val buildType: String = variant.buildType
  final override val kind: SourceSetKind = androidSources.variant.kind
  final override val variantNameCapitalized: String = variantName.capitalizeSafely()
  final override val taskNameSuffix: String = computeTaskNameSuffix()
  final override val compileConfigurationName = androidSources.compileClasspathConfigurationName
  final override val runtimeConfigurationName = androidSources.runtimeClasspathConfigurationName
  final override val kaptConfigurationName = kaptConfName()
  final override val annotationProcessorConfigurationName = "${variantName}AnnotationProcessorClasspath"
  final override val testInstrumentationRunner: Provider<String?> = variant.testInstrumentationRunner
  final override val kotlinSourceFiles: Provider<Iterable<File>> = androidSources.getKotlinSources()
  final override val javaSourceFiles: Provider<Iterable<File>> = androidSources.getJavaSources()
  final override val groovySourceFiles: Provider<Iterable<File>> = project.provider { project.files() }
  final override val scalaSourceFiles: Provider<Iterable<File>> = project.provider { project.files() }

  // TODO(2.0): verify this is the correct attribute.
  final override val attributeValueJar = ArtifactAttributes.ANDROID_CLASSES_JAR

  final override val isDataBindingEnabled: Provider<Boolean> = agp.isDataBindingEnabled()
  final override val isViewBindingEnabled: Provider<Boolean> = agp.isViewBindingEnabled()

  final override val outputPaths = OutputPaths(project, "$variantName${kind.taskNameSuffix}")

  final override fun registerByteCodeSourceExploderTask(): TaskProvider<ClassListExploderTask> {
    return project.tasks.register<ClassListExploderTask>("explodeByteCodeSource$taskNameSuffix") {
      classes.setFrom(project.files())
      output.set(outputPaths.explodingBytecodePath)
    }.also { provider ->
      androidSources.wireWithClassFiles(provider)
    }
  }

  final override fun registerManifestComponentsExtractionTask(): TaskProvider<ManifestComponentsExtractionTask> {
    return project.tasks.register<ManifestComponentsExtractionTask>(
      "extractPackageNameFromManifest$taskNameSuffix"
    ) {
      setArtifacts(project.configurations[compileConfigurationName].artifactsFor("android-manifest"))
      namespace.set(agp.namespace())
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
      androidLocalRes.setFrom(androidSources.getAndroidRes())
      layoutFiles.setFrom(androidSources.getLayoutFiles())
      manifestFiles.setFrom(androidSources.getManifestFiles())
      namespace.set(agp.namespace())
      output.set(outputPaths.androidResToResUsagePath)
    }
  }

  final override fun registerExplodeAssetSourceTask(): TaskProvider<AssetSourceExploderTask> {
    return project.tasks.register<AssetSourceExploderTask>("explodeAssetSource$taskNameSuffix") {
      androidLocalAssets.setFrom(androidSources.getAndroidAssets())
      output.set(outputPaths.androidAssetSourcePath)
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

  final override fun registerFindDeclaredProcsTask(): TaskProvider<FindDeclaredProcsTask> =
    project.tasks.register<FindDeclaredProcsTask>("findDeclaredProcs$taskNameSuffix") {
      inMemoryCacheProvider.set(InMemoryCache.register(project))
      kaptConf()?.let {
        setKaptArtifacts(it.incoming.artifacts)
      }
      annotationProcessorConf()?.let {
        setAnnotationProcessorArtifacts(it.incoming.artifacts)
      }

      output.set(outputPaths.declaredProcPath)
    }

  private fun kaptConfName(): String {
    return when (androidSources.variant.kind) {
      SourceSetKind.MAIN -> "kapt$variantNameCapitalized"
      SourceSetKind.TEST -> "kaptTest"
      SourceSetKind.ANDROID_TEST -> "kaptAndroidTest"
      SourceSetKind.CUSTOM_JVM -> error("Custom JVM source sets are not supported in Android context")
    }
  }

  private fun computeTaskNameSuffix(): String {
    return if (androidSources.variant.kind == SourceSetKind.MAIN) {
      // "flavorDebug" -> "FlavorDebug"
      variantName.capitalizeSafely()
    } else {
      // "flavorDebug" + "Test" -> "FlavorDebugTest"
      variantName.capitalizeSafely() + androidSources.variant.kind.taskNameSuffix
    }
  }
}

internal class AndroidAppAnalyzer(
  project: Project,
  variant: AndroidVariant,
  agpVersion: String,
  androidSources: AndroidSources,
) : AndroidAnalyzer(
  project = project,
  variant = variant,
  androidSources = androidSources,
  agpVersion = agpVersion
)

internal class AndroidLibAnalyzer(
  project: Project,
  variant: AndroidVariant,
  agpVersion: String,
  androidSources: AndroidSources,
  /** Tests and Android Tests don't have ABIs. */
  private val hasAbi: Boolean,
) : AndroidAnalyzer(
  project = project,
  variant = variant,
  androidSources = androidSources,
  agpVersion = agpVersion
) {

  override fun registerAbiAnalysisTask(abiExclusions: Provider<String>): TaskProvider<AbiAnalysisTask>? {
    if (!hasAbi) return null

    return project.tasks.register<AbiAnalysisTask>("abiAnalysis$taskNameSuffix") {
      exclusions.set(abiExclusions)
      output.set(outputPaths.abiAnalysisPath)
      abiDump.set(outputPaths.abiDumpPath)
    }.also { provider ->
      androidSources.wireWithClassFiles(provider)
    }
  }

  // We register this here because the concept of an "AndroidScore" for Android apps is useless. Android apps will never
  // be candidates for conversion to JVM libraries.
  override fun registerAndroidScoreTask(
    synthesizeDependenciesTask: TaskProvider<SynthesizeDependenciesTask>,
    synthesizeProjectViewTask: TaskProvider<SynthesizeProjectViewTask>,
  ): TaskProvider<AndroidScoreTask> {
    return project.tasks.register<AndroidScoreTask>("computeAndroidScore$taskNameSuffix") {
      dependencies.set(synthesizeDependenciesTask.flatMap { it.outputDir })
      dependenciesList.set(synthesizeDependenciesTask.flatMap { it.output })
      syntheticProject.set(synthesizeProjectViewTask.flatMap { it.output })
      output.set(outputPaths.androidScorePath)
    }
  }
}
