// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("UnstableApiUsage")

package com.autonomousapps.internal.analyzer

import com.autonomousapps.internal.ArtifactAttributes
import com.autonomousapps.internal.OutputPaths
import com.autonomousapps.internal.android.AndroidGradlePluginFactory
import com.autonomousapps.internal.artifactsFor
import com.autonomousapps.internal.utils.capitalizeSafely
import com.autonomousapps.model.source.SourceKind
import com.autonomousapps.services.InMemoryCache
import com.autonomousapps.tasks.*
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider

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

  final override val sourceKind: SourceKind = androidSources.sourceKind

  final override val taskNameSuffix: String = computeTaskNameSuffix()
  final override val compileConfigurationName = androidSources.compileClasspathConfigurationName
  final override val runtimeConfigurationName = androidSources.runtimeClasspathConfigurationName
  final override val kaptConfigurationName = kaptConfName()
  final override val annotationProcessorConfigurationName = "${variantName}AnnotationProcessorClasspath"
  final override val testInstrumentationRunner: Provider<String> = variant.testInstrumentationRunner

  // TODO(3.0): verify this is the correct attribute.
  final override val attributeValueJar = ArtifactAttributes.ANDROID_CLASSES_JAR

  final override val isDataBindingEnabled: Provider<Boolean> = agp.isDataBindingEnabled()
  final override val isViewBindingEnabled: Provider<Boolean> = agp.isViewBindingEnabled()

  private fun suffix() = when (sourceKind.kind) {
    SourceKind.MAIN_KIND -> "Main"
    SourceKind.TEST_KIND -> "Test"
    SourceKind.ANDROID_TEST_FIXTURES_KIND -> "TestFixtures"
    SourceKind.ANDROID_TEST_KIND -> "AndroidTest"
    else -> error("Unknown kind. Was '${sourceKind.kind}'")
  }

  final override val outputPaths = OutputPaths(project, "$variantName${suffix()}")

  final override fun registerByteCodeSourceExploderTask(): TaskProvider<ClassListExploderTask> {
    return project.tasks.register("explodeByteCodeSource$taskNameSuffix", ClassListExploderTask::class.java) {
      it.classes.setFrom(project.files())
      it.output.set(outputPaths.explodingBytecodePath)
    }.also { provider ->
      androidSources.wireWithClassFiles(provider)
    }
  }

  final override fun registerCodeSourceExploderTask(): TaskProvider<out CodeSourceExploderTask> {
    return project.tasks.register("explodeCodeSource$taskNameSuffix", AndroidCodeSourceExploderTask::class.java) {
      androidSources.sources.java?.all?.let { j -> it.javaSource.set(j) }
      androidSources.sources.kotlin?.all?.let { k -> it.kotlinSource.set(k) }
      it.output.set(outputPaths.explodedSourcePath)
    }
  }

  final override fun registerManifestComponentsExtractionTask(): TaskProvider<ManifestComponentsExtractionTask> {
    return project.tasks.register(
      "extractPackageNameFromManifest$taskNameSuffix",
      ManifestComponentsExtractionTask::class.java,
    ) {
      it.setArtifacts(project.configurations.getByName(compileConfigurationName).artifactsFor("android-manifest"))
      it.namespace.set(agp.namespace())
      it.output.set(outputPaths.manifestPackagesPath)
    }
  }

  final override fun registerFindAndroidResTask(): TaskProvider<FindAndroidResTask> {
    return project.tasks.register("findAndroidResImports$taskNameSuffix", FindAndroidResTask::class.java) {
      it.setAndroidSymbols(
        project.configurations.getByName(compileConfigurationName).artifactsFor("android-symbol-with-package-name")
      )
      it.setAndroidPublicRes(project.configurations.getByName(compileConfigurationName).artifactsFor("android-public-res"))
      it.output.set(outputPaths.androidResPath)
    }
  }

  final override fun registerExplodeXmlSourceTask(): TaskProvider<XmlSourceExploderTask> {
    return project.tasks.register("explodeXmlSource$taskNameSuffix", XmlSourceExploderTask::class.java) {
      androidSources.getAndroidRes()?.let { r -> it.androidLocalRes.set(r) }
      it.manifests.set(androidSources.getManifestFiles())
      it.mergedManifestFiles.setFrom(androidSources.getMergedManifest())
      it.namespace.set(agp.namespace())
      it.output.set(outputPaths.androidResToResUsagePath)
      it.outputRuntime.set(outputPaths.androidResToResUsageRuntimePath)
    }
  }

  final override fun registerExplodeAssetSourceTask(): TaskProvider<AssetSourceExploderTask> {
    return project.tasks.register("explodeAssetSource$taskNameSuffix", AssetSourceExploderTask::class.java) {
      it.androidLocalAssets.setFrom(androidSources.getAndroidAssets())
      it.output.set(outputPaths.androidAssetSourcePath)
    }
  }

  final override fun registerFindNativeLibsTask(): TaskProvider<FindNativeLibsTask> {
    return project.tasks.register("findNativeLibs$taskNameSuffix", FindNativeLibsTask::class.java) {
      it.setAndroidJni(project.configurations.getByName(compileConfigurationName).artifactsFor(ArtifactAttributes.ANDROID_JNI))
      it.output.set(outputPaths.nativeDependenciesPath)
    }
  }

  final override fun registerFindAndroidLintersTask(): TaskProvider<FindAndroidLinters> =
    project.tasks.register("findAndroidLinters$taskNameSuffix", FindAndroidLinters::class.java) {
      it.setLintJars(project.configurations.getByName(compileConfigurationName).artifactsFor(ArtifactAttributes.ANDROID_LINT))
      it.output.set(outputPaths.androidLintersPath)
    }

  final override fun registerFindAndroidAssetProvidersTask(): TaskProvider<FindAndroidAssetProviders> =
    project.tasks.register("findAndroidAssetProviders$taskNameSuffix", FindAndroidAssetProviders::class.java) {
      it.setAssets(project.configurations.getByName(runtimeConfigurationName).artifactsFor(ArtifactAttributes.ANDROID_ASSETS))
      it.output.set(outputPaths.androidAssetsPath)
    }

  final override fun registerFindDeclaredProcsTask(): TaskProvider<FindDeclaredProcsTask> =
    project.tasks.register("findDeclaredProcs$taskNameSuffix", FindDeclaredProcsTask::class.java) {
      it.inMemoryCacheProvider.set(InMemoryCache.register(project))
      kaptConf()?.let { configuration ->
        it.setKaptArtifacts(configuration.incoming.artifacts)
      }
      annotationProcessorConf()?.let { configuration ->
        it.setAnnotationProcessorArtifacts(configuration.incoming.artifacts)
      }

      it.output.set(outputPaths.declaredProcPath)
    }

  private fun kaptConfName(): String {
    return when (sourceKind.kind) {
      SourceKind.MAIN_KIND -> "kapt${variantName.capitalizeSafely()}"
      SourceKind.TEST_KIND -> "kaptTest"
      SourceKind.ANDROID_TEST_FIXTURES_KIND -> "kaptTestFixtures"
      SourceKind.ANDROID_TEST_KIND -> "kaptAndroidTest"
      SourceKind.CUSTOM_JVM_KIND -> error("Custom JVM source sets are not supported in Android context")
      else -> error("Unknown kind: '${sourceKind.kind}'")
    }
  }

  private fun computeTaskNameSuffix(): String {
    return if (sourceKind.kind == SourceKind.MAIN_KIND) {
      // "flavorDebug" -> "FlavorDebug"
      variantName.capitalizeSafely()
    } else {
      // "flavorDebug" + "Test" -> "FlavorDebugTest"
      variantName.capitalizeSafely() + suffix()
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

    return project.tasks.register("abiAnalysis$taskNameSuffix", AbiAnalysisTask::class.java) {
      it.sourceFiles.setFrom(androidSources.sources.java?.all)
      it.sourceFiles.setFrom(androidSources.sources.kotlin?.all)
      it.exclusions.set(abiExclusions)
      it.output.set(outputPaths.abiAnalysisPath)
      it.abiDump.set(outputPaths.abiDumpPath)
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
    return project.tasks.register("computeAndroidScore$taskNameSuffix", AndroidScoreTask::class.java) {
      it.dependencies.set(synthesizeDependenciesTask.flatMap { it.outputDir })
      it.syntheticProject.set(synthesizeProjectViewTask.flatMap { it.output })
      it.output.set(outputPaths.androidScorePath)
    }
  }
}

// nb: identical to AndroidAppAnalyzer
internal class AndroidTestAnalyzer(
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
