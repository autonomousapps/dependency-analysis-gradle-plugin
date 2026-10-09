// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.analyzer

import com.autonomousapps.AbstractExtension
import com.autonomousapps.internal.KotlinMetadataClasspath
import com.autonomousapps.internal.artifactsFor
import com.autonomousapps.internal.opaqueComponentArtifacts
import com.autonomousapps.internal.resolvedArtifactsFor
import com.autonomousapps.internal.resolvedOpaqueComponentArtifacts
import com.autonomousapps.internal.utils.project.buildPath
import com.autonomousapps.model.DuplicateClass
import com.autonomousapps.services.InMemoryCache
import com.autonomousapps.tasks.*
import org.gradle.api.Project
import org.gradle.api.UnknownDomainObjectException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider

internal abstract class AbstractDependencyAnalyzer(
  protected val project: Project,
) : DependencyAnalyzer {

  // Always null for JVM projects. May be null for Android projects.
  override val testInstrumentationRunner: Provider<String> = project.provider { null }

  final override fun registerArtifactsReportForCompileTask(): TaskProvider<ArtifactsReportTask> {
    return registerArtifactsReportTaskFor(
      "artifactsReport$taskNameSuffix",
      compileConfigurationName,
      output = outputPaths.compileArtifactsPath,
      excludedIdentifiersOutput = outputPaths.excludedIdentifiersPath,
    )
  }

  final override fun registerArtifactsReportForRuntimeTask(): TaskProvider<ArtifactsReportTask> {
    return registerArtifactsReportTaskFor(
      "artifactsReportRuntime$taskNameSuffix",
      runtimeConfigurationName,
      output = outputPaths.runtimeArtifactsPath,
      excludedIdentifiersOutput = outputPaths.excludedIdentifiersRuntimePath,
    )
  }

  private fun registerArtifactsReportTaskFor(
    taskName: String,
    configurationName: String,
    output: Provider<RegularFile>,
    excludedIdentifiersOutput: Provider<RegularFile>,
  ): TaskProvider<ArtifactsReportTask> {
    return project.tasks.register(taskName, ArtifactsReportTask::class.java) { t ->
      val classpath = project.configurations.named(configurationName)
      t.withJarArtifacts(classpath.resolvedArtifactsFor(attributeValueJar))
      t.withOpaqueJarArtifacts(classpath.resolvedOpaqueComponentArtifacts())

      t.buildPath.set(project.buildPath(configurationName))
      t.resolvedComponentResult.set(classpath.flatMap { it.incoming.resolutionResult.rootComponent })
      t.excludedIdentifiers.set(classpath.map { c -> c.excludeRules.map { "${it.group}:${it.module}".intern() } })

      t.output.set(output)
      t.excludedIdentifiersOutput.set(excludedIdentifiersOutput)
    }
  }

  final override fun registerComputeUsagesTask(
    checkSuperClasses: Provider<Boolean>,
    checkBinaryCompat: Provider<Boolean>,
    isKaptApplied: Provider<Boolean>,
    graphViewTask: TaskProvider<GraphViewTask>,
    findDeclarationsTask: TaskProvider<FindDeclarationsTask>,
    synthesizeProjectViewTask: TaskProvider<SynthesizeProjectViewTask>,
    explodeJarTask: TaskProvider<ExplodeJarTask>,
    synthesizeDependenciesTask: TaskProvider<SynthesizeDependenciesTask>,
    duplicateClassesCompile: TaskProvider<DiscoverClasspathDuplicationTask>,
    duplicateClassesRuntime: TaskProvider<DiscoverClasspathDuplicationTask>
  ): TaskProvider<ComputeUsagesTask> {
    return project.tasks.register("computeActualUsage$taskNameSuffix", ComputeUsagesTask::class.java) { t ->
      t.checkSuperClasses.set(checkSuperClasses)
      t.checkBinaryCompat.set(checkBinaryCompat)

      t.buildPath.set(project.buildPath(compileConfigurationName))
      t.graph.set(graphViewTask.flatMap { it.output })
      t.graphRuntime.set(graphViewTask.flatMap { it.outputRuntime })
      t.declarations.set(findDeclarationsTask.flatMap { it.output })
      t.dependencies.set(synthesizeDependenciesTask.flatMap { it.outputDir })
      t.syntheticProject.set(synthesizeProjectViewTask.flatMap { it.output })
      t.binaryClasses.set(explodeJarTask.flatMap { it.outputBinaryClasses })
      t.kapt.set(isKaptApplied)
      t.duplicateClassesReports.add(duplicateClassesCompile.flatMap { it.output })
      t.duplicateClassesReports.add(duplicateClassesRuntime.flatMap { it.output })
      t.output.set(outputPaths.dependencyTraceReportPath)
    }
  }

  final override fun registerComputeTypeUsageTask(
    synthesizeProjectViewTask: TaskProvider<SynthesizeProjectViewTask>,
    explodeJarTask: TaskProvider<ExplodeJarTask>,
    synthesizeDependenciesTask: TaskProvider<SynthesizeDependenciesTask>,
    dagpExtension: AbstractExtension,
  ): TaskProvider<ComputeTypeUsageTask> {
    return project.tasks.register("computeTypeUsage$taskNameSuffix", ComputeTypeUsageTask::class.java) { t ->
      t.buildPath.set(project.buildPath(compileConfigurationName))
      t.syntheticProject.set(synthesizeProjectViewTask.flatMap { it.output })
      t.explodedJars.set(explodeJarTask.flatMap { it.output })
      t.dependencies.set(synthesizeDependenciesTask.flatMap { it.outputDir })

      // Configuration from extension
      t.excludedPackages.set(dagpExtension.typeUsageHandler.excludedPackages)
      t.excludedTypes.set(dagpExtension.typeUsageHandler.excludedTypes)
      t.excludedRegexPatterns.set(dagpExtension.typeUsageHandler.excludedRegexPatterns)

      t.output.set(outputPaths.typeUsagePath)
    }
  }

  final override fun registerDominatorTreeTasks(
    artifactsReportCompile: TaskProvider<ArtifactsReportTask>,
    artifactsReportRuntime: TaskProvider<ArtifactsReportTask>,
    graphViewTask: TaskProvider<GraphViewTask>,
  ) {
    val computeDominatorCompile = registerComputeDominatorTreeForCompileTask(artifactsReportCompile, graphViewTask)
    val computeDominatorRuntime = registerComputeDominatorTreeForRuntimeTask(artifactsReportRuntime, graphViewTask)

    // a lifecycle task that computes the dominator tree for both compile and runtime classpaths
    project.tasks.register("computeDominatorTree$taskNameSuffix") { t ->
      t.dependsOn(computeDominatorCompile, computeDominatorRuntime)
    }

    project.tasks.register("printDominatorTreeCompile$taskNameSuffix", PrintDominatorTreeTask::class.java) { t ->
      t.consoleText.set(computeDominatorCompile.flatMap { it.outputTxt })
    }

    project.tasks.register("printDominatorTreeRuntime$taskNameSuffix", PrintDominatorTreeTask::class.java) { t ->
      t.consoleText.set(computeDominatorRuntime.flatMap { it.outputTxt })
    }
  }

  private fun registerComputeDominatorTreeForCompileTask(
    artifactsReportTask: TaskProvider<ArtifactsReportTask>,
    graphViewTask: TaskProvider<GraphViewTask>,
  ): TaskProvider<ComputeDominatorTreeTask> {
    return project.tasks.register(
      "computeDominatorTreeCompile$taskNameSuffix",
      ComputeDominatorTreeTask::class.java
    ) { t ->
      t.buildPath.set(project.buildPath(compileConfigurationName))
      t.projectPath.set(project.path)
      t.physicalArtifacts.set(artifactsReportTask.flatMap { it.output })
      t.graphView.set(graphViewTask.flatMap { it.output })

      t.outputTxt.set(outputPaths.compileDominatorConsolePath)
      t.outputDot.set(outputPaths.compileDominatorGraphPath)
      t.outputJson.set(outputPaths.compileDominatorJsonPath)
    }
  }

  private fun registerComputeDominatorTreeForRuntimeTask(
    artifactsReportTask: TaskProvider<ArtifactsReportTask>,
    graphViewTask: TaskProvider<GraphViewTask>
  ): TaskProvider<ComputeDominatorTreeTask> {
    return project.tasks.register(
      "computeDominatorTreeRuntime$taskNameSuffix",
      ComputeDominatorTreeTask::class.java
    ) { t ->
      t.buildPath.set(project.buildPath(runtimeConfigurationName))
      t.projectPath.set(project.path)
      t.physicalArtifacts.set(artifactsReportTask.flatMap { it.output })
      t.graphView.set(graphViewTask.flatMap { it.outputRuntime })

      t.outputTxt.set(outputPaths.runtimeDominatorConsolePath)
      t.outputDot.set(outputPaths.runtimeDominatorGraphPath)
      t.outputJson.set(outputPaths.runtimeDominatorJsonPath)
    }
  }

  final override fun registerDiscoverClasspathDuplicationForCompileTask(
    synthesizeProjectViewTask: TaskProvider<SynthesizeProjectViewTask>,
  ): TaskProvider<DiscoverClasspathDuplicationTask> {
    return registerDiscoverClasspathDuplicationTask(
      "discoverDuplicationForCompile$taskNameSuffix",
      DuplicateClass.COMPILE_CLASSPATH_NAME,
      compileConfigurationName,
      synthesizeProjectViewTask,
      outputPaths.duplicateCompileClasspathPath,
    )
  }

  final override fun registerDiscoverClasspathDuplicationForRuntimeTask(
    synthesizeProjectViewTask: TaskProvider<SynthesizeProjectViewTask>,
  ): TaskProvider<DiscoverClasspathDuplicationTask> {
    return registerDiscoverClasspathDuplicationTask(
      "discoverDuplicationForRuntime$taskNameSuffix",
      DuplicateClass.RUNTIME_CLASSPATH_NAME,
      runtimeConfigurationName,
      synthesizeProjectViewTask,
      outputPaths.duplicateCompileRuntimePath,
    )
  }

  private fun registerDiscoverClasspathDuplicationTask(
    taskName: String,
    classpathName: String,
    configurationName: String,
    synthesizeProjectViewTask: TaskProvider<SynthesizeProjectViewTask>,
    output: Provider<RegularFile>,
  ): TaskProvider<DiscoverClasspathDuplicationTask> {
    return project.tasks.register(taskName, DiscoverClasspathDuplicationTask::class.java) { t ->
      t.withClasspathName(classpathName)
      t.withClasspath(project.configurations.named(configurationName).resolvedArtifactsFor(attributeValueJar))
      t.syntheticProject.set(synthesizeProjectViewTask.flatMap { it.output })
      t.output.set(output)
    }
  }

  final override fun registerExplodeJarTask(
    artifactsReport: TaskProvider<ArtifactsReportTask>,
    androidLintTask: TaskProvider<FindAndroidLinters>?,
  ): TaskProvider<ExplodeJarTask> {
    return project.tasks.register("explodeJar$taskNameSuffix", ExplodeJarTask::class.java) { t ->
      InMemoryCache.register(t.inMemoryCache, project)
      t.compileClasspath.setFrom(
        project.configurations.named(compileConfigurationName).map { it.artifactsFor(attributeValueJar).artifactFiles }
      )
      t.physicalArtifacts.set(artifactsReport.flatMap { it.output })
      androidLintTask?.let { t2 -> t.androidLinters.set(t2.flatMap { it.output }) }
      t.kotlinMetadataClasspath.setFrom(KotlinMetadataClasspath.of(project))

      t.output.set(outputPaths.explodedJarsPath)
      t.outputBinaryClasses.set(outputPaths.binaryClassesPath)
    }
  }

  final override fun registerFindKotlinMagicTask(artifactsReport: TaskProvider<ArtifactsReportTask>): TaskProvider<FindKotlinMagicTask> {
    return project.tasks.register("findKotlinMagic$taskNameSuffix", FindKotlinMagicTask::class.java) { t ->
      InMemoryCache.register(t.inMemoryCacheProvider, project)
      t.compileClasspath.setFrom(
        project.configurations.named(compileConfigurationName).map { it.artifactsFor(attributeValueJar).artifactFiles }
      )
      t.artifacts.set(artifactsReport.flatMap { it.output })
      t.kotlinMetadataClasspath.setFrom(KotlinMetadataClasspath.of(project))
      t.outputInlineMembers.set(outputPaths.inlineUsagePath)
      t.outputTypealiases.set(outputPaths.typealiasUsagePath)
      t.outputErrors.set(outputPaths.inlineUsageErrorsPath)
    }
  }

  final override fun registerFindServiceLoadersTask(): TaskProvider<FindServiceLoadersTask> {
    return project.tasks.register("serviceLoader$taskNameSuffix", FindServiceLoadersTask::class.java) { t ->
      // TODO(tsr): consider this. Wouldn't the runtime classpath be more appropriate for this task? Separate PR to test.
      //  it.setCompileClasspath(configurations.getByName(dependencyAnalyzer.runtimeConfigurationName).artifactsFor(dependencyAnalyzer.attributeValueJar))
      t.setCompileClasspath(
        project.configurations
          .getByName(compileConfigurationName)
          .artifactsFor(attributeValueJar)
      )
      t.output.set(outputPaths.serviceLoaderDependenciesPath)
    }
  }

  final override fun registerGenerateProjectGraphTasks(mergeProjectGraphsTask: TaskProvider<MergeProjectGraphsTask>) {
    val generateProjectGraphTask =
      project.tasks.register("generateProjectGraph$taskNameSuffix", GenerateProjectGraphTask::class.java) { t ->
        t.buildPath.set(project.buildPath(compileConfigurationName))

        t.compileClasspath.set(
          project.configurations.named(compileConfigurationName).flatMap { it.incoming.resolutionResult.rootComponent }
        )
        t.runtimeClasspath.set(
          project.configurations.named(runtimeConfigurationName).flatMap { it.incoming.resolutionResult.rootComponent }
        )
        t.output.set(outputPaths.projectGraphDir)
      }

    // Prints some help text relating to generateProjectGraphTask. This is the "user-facing" task.
    project.tasks.register("projectGraph$taskNameSuffix", ProjectGraphTask::class.java) { t ->
      t.rootDir.set(project.rootDir)
      t.projectPath.set(project.path)
      t.graphsDir.set(generateProjectGraphTask.flatMap { it.output })
    }

    // Merges the graphs from generateProjectGraphTask into a single variant-agnostic output.
    mergeProjectGraphsTask.configure { t ->
      t.projectGraphs.add(generateProjectGraphTask.flatMap {
        it.output.file(GenerateProjectGraphTask.PROJECT_COMBINED_CLASSPATH_JSON)
      })
    }
  }

  final override fun registerGraphViewTask(findDeclarationsTask: TaskProvider<FindDeclarationsTask>): TaskProvider<GraphViewTask> {
    return project.tasks.register("graphView$taskNameSuffix", GraphViewTask::class.java) { t ->
      t.configureTask(
        compileClasspath = project.configurations.named(compileConfigurationName),
        runtimeClasspath = project.configurations.named(runtimeConfigurationName),
        jarAttr = attributeValueJar
      )
      t.buildPath.set(project.buildPath(compileConfigurationName))
      t.projectPath.set(project.path)
      t.sourceKind.set(sourceKind)
      t.declarations.set(findDeclarationsTask.flatMap { it.output })

      t.output.set(outputPaths.compileGraphPath)
      t.outputDot.set(outputPaths.compileGraphDotPath)
      t.outputNodes.set(outputPaths.compileNodesPath)
      t.outputRuntime.set(outputPaths.runtimeGraphPath)
      t.outputRuntimeDot.set(outputPaths.runtimeGraphDotPath)
    }
  }

  final override fun registerResolveExternalDependenciesTask(): TaskProvider<ResolveExternalDependenciesTask> {
    return project.tasks.register(
      "resolveExternalDependencies$taskNameSuffix",
      ResolveExternalDependenciesTask::class.java,
    ) { t ->
      t.configureTask(
        compileClasspath = project.configurations.named(compileConfigurationName),
        runtimeClasspath = project.configurations.named(runtimeConfigurationName),
        jarAttr = attributeValueJar,
      )
      t.output.set(outputPaths.externalDependenciesPath)
    }
  }

  final override fun registerSynthesizeDependenciesTask(
    graphViewTask: TaskProvider<GraphViewTask>,
    artifactsReport: TaskProvider<ArtifactsReportTask>,
    explodeJarTask: TaskProvider<ExplodeJarTask>,
    kotlinMagicTask: TaskProvider<FindKotlinMagicTask>,
    findServiceLoadersTask: TaskProvider<FindServiceLoadersTask>,
    declaredProcsTask: TaskProvider<FindDeclaredProcsTask>,
    findNativeLibsTask: TaskProvider<FindNativeLibsTask>,
    androidManifestTask: TaskProvider<ManifestComponentsExtractionTask>?,
    findAndroidResTask: TaskProvider<FindAndroidResTask>?,
    findAndroidAssetsTask: TaskProvider<FindAndroidAssetProviders>?,
  ): TaskProvider<SynthesizeDependenciesTask> {
    return project.tasks.register(
      "synthesizeDependencies$taskNameSuffix",
      SynthesizeDependenciesTask::class.java
    ) { t ->

      t.projectPath.set(project.path)
      t.compileDependencies.set(graphViewTask.flatMap { it.outputNodes })
      t.physicalArtifacts.set(artifactsReport.flatMap { it.output })
      t.explodedJars.set(explodeJarTask.flatMap { it.output })
      t.inlineMembers.set(kotlinMagicTask.flatMap { it.outputInlineMembers })
      t.typealiases.set(kotlinMagicTask.flatMap { it.outputTypealiases })
      t.serviceLoaders.set(findServiceLoadersTask.flatMap { it.output })
      t.annotationProcessors.set(declaredProcsTask.flatMap { it.output })
      t.nativeLibs.set(findNativeLibsTask.flatMap { it.output })
      // Optional Android-only inputs
      androidManifestTask?.let { t2 -> t.manifestComponents.set(t2.flatMap { it.output }) }
      findAndroidResTask?.let { t2 -> t.androidRes.set(t2.flatMap { it.output }) }
      findAndroidAssetsTask?.let { t2 -> t.androidAssets.set(t2.flatMap { it.output }) }

      t.outputDir.set(outputPaths.dependenciesDir)
    }
  }

  final override fun registerSynthesizeProjectViewTask(
    graphViewTask: TaskProvider<GraphViewTask>,
    declaredProcsTask: TaskProvider<FindDeclaredProcsTask>,
    explodeBytecodeTask: TaskProvider<ClassListExploderTask>,
    explodeCodeSourceTask: TaskProvider<out CodeSourceExploderTask>,
    usagesExclusionsProvider: Provider<String>,
    artifactsReport: TaskProvider<ArtifactsReportTask>,
    abiAnalysisTask: TaskProvider<AbiAnalysisTask>?,
    explodeXmlSourceTask: TaskProvider<XmlSourceExploderTask>?,
    explodeAssetSourceTask: TaskProvider<AssetSourceExploderTask>?
  ): TaskProvider<SynthesizeProjectViewTask> {
    return project.tasks.register("synthesizeProjectView$taskNameSuffix", SynthesizeProjectViewTask::class.java) { t ->
      t.projectPath.set(project.path)
      t.buildType.set(buildType)
      t.flavor.set(flavorName)
      t.variant.set(variantName)
      t.sourceKind.set(sourceKind)
      t.graph.set(graphViewTask.flatMap { it.output })
      t.annotationProcessors.set(declaredProcsTask.flatMap { it.output })
      t.explodedBytecode.set(explodeBytecodeTask.flatMap { it.output })
      t.explodedSourceCode.set(explodeCodeSourceTask.flatMap { it.output })
      t.usagesExclusions.set(usagesExclusionsProvider)
      t.excludedIdentifiers.set(artifactsReport.flatMap { it.excludedIdentifiersOutput })
      // Optional: only exists for libraries.
      abiAnalysisTask?.let { t2 -> t.explodingAbi.set(t2.flatMap { it.output }) }
      // Optional: only exists for Android libraries.
      explodeXmlSourceTask?.let { t2 ->
        t.androidResSource.set(t2.flatMap { it.output })
        t.androidResSourceRuntime.set(t2.flatMap { it.outputRuntime })
      }
      // Optional: only exists for Android libraries.
      explodeAssetSourceTask?.let { t2 -> t.androidAssetsSource.set(t2.flatMap { it.output }) }
      // Optional: only exists for Android projects.
      t.testInstrumentationRunner.set(testInstrumentationRunner)
      t.output.set(outputPaths.syntheticProjectPath)
    }
  }

  protected fun kaptConf(): Configuration? = try {
    project.configurations.getByName(kaptConfigurationName)
  } catch (_: UnknownDomainObjectException) {
    null
  }

  protected fun annotationProcessorConf(): Configuration? = try {
    project.configurations.getByName(annotationProcessorConfigurationName)
  } catch (_: UnknownDomainObjectException) {
    null
  }
}
