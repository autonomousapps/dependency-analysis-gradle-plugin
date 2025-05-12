// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.tasks

import com.autonomousapps.extension.DependenciesHandler
import com.autonomousapps.graph.Graphs.children
import com.autonomousapps.graph.Graphs.root
import com.autonomousapps.internal.Bundles
import com.autonomousapps.internal.utils.*
import com.autonomousapps.internal.utils.CoordinatesString.Companion.toStringCoordinates
import com.autonomousapps.model.*
import com.autonomousapps.model.declaration.internal.Bucket
import com.autonomousapps.model.declaration.internal.Configurations
import com.autonomousapps.model.declaration.internal.Declaration
import com.autonomousapps.model.internal.DependencyGraphView
import com.autonomousapps.model.internal.intermediates.*
import com.autonomousapps.model.source.SourceKind
import com.autonomousapps.transform.StandardTransform
import com.google.common.collect.SetMultimap
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.*
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

/**
 * Takes [usage][com.autonomousapps.model.internal.intermediates.Usage] information from [ComputeUsagesTask] and emits
 * the set of transforms a user should perform to have correct and simple dependency declarations. I.e., produces the
 * advice.
 */
@CacheableTask
abstract class ComputeAdviceTask @Inject constructor(
  private val workerExecutor: WorkerExecutor,
) : DefaultTask() {

  init {
    description = "Merges dependency usage reports from variant-specific computations"
  }

  @get:Input
  abstract val projectPath: Property<String>

  @get:Input
  abstract val buildPath: Property<String>

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  abstract val dependencyUsageReports: ListProperty<RegularFile>

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  abstract val dependencyGraphViews: ListProperty<RegularFile>

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  abstract val androidScoreReports: ListProperty<RegularFile>

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val declarations: RegularFileProperty

  @get:Nested
  abstract val bundles: Property<DependenciesHandler.SerializableBundles>

  @get:Input
  abstract val supportedSourceSets: SetProperty<String>

  @get:Input
  abstract val ignoreKtx: Property<Boolean>

  @get:Input
  abstract val explicitSourceSets: SetProperty<String>

  /** Android (true) or JVM (false). */
  @get:Input
  abstract val android: Property<Boolean>

  @get:Input
  abstract val kapt: Property<Boolean>

  @get:Optional
  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val redundantJvmPluginReport: RegularFileProperty

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  abstract val duplicateClassesReports: ListProperty<RegularFile>

  /*
   * Outputs.
   */

  @get:OutputFile
  abstract val output: RegularFileProperty

  @get:OutputFile
  abstract val dependencyUsages: RegularFileProperty

  @get:OutputFile
  abstract val annotationProcessorUsages: RegularFileProperty

  @get:OutputFile
  abstract val bundledTraces: RegularFileProperty

  @TaskAction fun action() {
    workerExecutor.noIsolation().submit(ComputeAdviceAction::class.java) {
      projectPath.set(this@ComputeAdviceTask.projectPath)
      buildPath.set(this@ComputeAdviceTask.buildPath)
      dependencyUsageReports.set(this@ComputeAdviceTask.dependencyUsageReports)
      dependencyGraphViews.set(this@ComputeAdviceTask.dependencyGraphViews)
      androidScoreReports.set(this@ComputeAdviceTask.androidScoreReports)
      declarations.set(this@ComputeAdviceTask.declarations)
      bundles.set(this@ComputeAdviceTask.bundles)
      supportedSourceSets.set(this@ComputeAdviceTask.supportedSourceSets)
      ignoreKtx.set(this@ComputeAdviceTask.ignoreKtx)
      explicitSourceSets.set(this@ComputeAdviceTask.explicitSourceSets)
      android.set(this@ComputeAdviceTask.android)
      kapt.set(this@ComputeAdviceTask.kapt)
      redundantPluginReport.set(this@ComputeAdviceTask.redundantJvmPluginReport)
      duplicateClassesReports.set(this@ComputeAdviceTask.duplicateClassesReports)

      output.set(this@ComputeAdviceTask.output)
      dependencyUsages.set(this@ComputeAdviceTask.dependencyUsages)
      annotationProcessorUsages.set(this@ComputeAdviceTask.annotationProcessorUsages)
      bundledTraces.set(this@ComputeAdviceTask.bundledTraces)
    }
  }

  interface ComputeAdviceParameters : WorkParameters {
    val projectPath: Property<String>
    val buildPath: Property<String>
    val dependencyUsageReports: ListProperty<RegularFile>
    val dependencyGraphViews: ListProperty<RegularFile>
    val androidScoreReports: ListProperty<RegularFile>
    val declarations: RegularFileProperty
    val bundles: Property<DependenciesHandler.SerializableBundles>
    val supportedSourceSets: SetProperty<String>
    val ignoreKtx: Property<Boolean>
    val explicitSourceSets: SetProperty<String>
    val android: Property<Boolean>
    val kapt: Property<Boolean>
    val redundantPluginReport: RegularFileProperty
    val duplicateClassesReports: ListProperty<RegularFile>

    val output: RegularFileProperty
    val dependencyUsages: RegularFileProperty
    val annotationProcessorUsages: RegularFileProperty
    val bundledTraces: RegularFileProperty
  }

  abstract class ComputeAdviceAction : WorkAction<ComputeAdviceParameters> {

    override fun execute() {
      val output = parameters.output.getAndDelete()
      val dependencyUsagesOut = parameters.dependencyUsages.getAndDelete()
      val annotationProcessorUsagesOut = parameters.annotationProcessorUsages.getAndDelete()
      val bundleTraces = parameters.bundledTraces.getAndDelete()

      val projectPath = parameters.projectPath.get()
      val buildPath = parameters.buildPath.get()
      val declarations = parameters.declarations.fromJsonSet<Declaration>()
      val dependencyGraph = parameters.dependencyGraphViews.get()
        .map { it.fromJson<DependencyGraphView>() }
        .associateBy { "${it.name},${it.configurationName}" }
      val androidScore = parameters.androidScoreReports.get()
        .map { it.fromJson<AndroidScoreVariant>() }
        .run { AndroidScore.ofVariants(this) }
        .toSetOrEmpty()
      val bundleRules = parameters.bundles.get()
      val traces = parameters.dependencyUsageReports.get().mapToSet { it.fromJson<DependencyTraceReport>() }
      val usageBuilder = UsageBuilder(
        traces = traces,
        // TODO(tsr): it would be clearer to get this from a SyntheticProject
        // TODO(tsr): this now includes the runtime graph. Maybe strip it if it's problematic here
        sourceKinds = dependencyGraph.values.map { it.sourceKind },
      )
      val dependencyUsages = usageBuilder.dependencyUsages
      val annotationProcessorUsages = usageBuilder.annotationProcessingUsages
      val supportedSourceSets = parameters.supportedSourceSets.get()
      val explicitSourceSets = parameters.explicitSourceSets.get()
      val isAndroidProject = parameters.android.get()
      val isKaptApplied = parameters.kapt.get()
      val directDependencies = computeDirectDependenciesMap(dependencyGraph)
      val dependenciesToClasspaths = computeDependenciesToClasspathsMap(dependencyGraph)

      val ignoreKtx = parameters.ignoreKtx.get()

      val bundles = Bundles.of(
        projectPath = projectPath,
        dependencyGraph = dependencyGraph,
        bundleRules = bundleRules,
        dependencyUsages = dependencyUsages,
        ignoreKtx = ignoreKtx,
      )

      val dependencyAdviceBuilder = DependencyAdviceBuilder(
        projectPath = projectPath,
        buildPath = buildPath,
        bundles = bundles,
        dependencyUsages = dependencyUsages,
        annotationProcessorUsages = annotationProcessorUsages,
        declarations = declarations,
        directDependencies = directDependencies,
        dependenciesToClasspaths = dependenciesToClasspaths,
        supportedSourceSets = supportedSourceSets,
        explicitSourceSets = explicitSourceSets,
        isAndroidProject = isAndroidProject,
        isKaptApplied = isKaptApplied,
      )

      val pluginAdviceBuilder = PluginAdviceBuilder(
        isKaptApplied = isKaptApplied,
        redundantPlugins = parameters.redundantPluginReport.fromNullableJsonSet<PluginAdvice>(),
        annotationProcessorUsages = annotationProcessorUsages,
      )

      val projectAdvice = ProjectAdvice(
        projectPath = projectPath,
        dependencyAdvice = dependencyAdviceBuilder.advice,
        pluginAdvice = pluginAdviceBuilder.getPluginAdvice(),
        moduleAdvice = androidScore,
        warning = buildWarning(),
      )

      output.bufferWriteJson(projectAdvice)
      // These must be transformed so that the Coordinates are Strings for serialization
      dependencyUsagesOut.bufferWriteJsonMap(toStringCoordinates(dependencyUsages, buildPath))
      annotationProcessorUsagesOut.bufferWriteJsonMap(toStringCoordinates(annotationProcessorUsages, buildPath))
      bundleTraces.bufferWriteJsonSet(dependencyAdviceBuilder.bundledTraces)
    }

    private fun buildWarning(): Warning {
      val duplicateClassesReports = parameters.duplicateClassesReports.get().asSequence()
        .map { it.fromJsonSet<DuplicateClass>() }
        .flatten()
        .toSortedSet()

      return Warning(duplicateClassesReports)
    }

    /**
     * Returns the set of direct (non-transitive) dependencies from [dependencyGraph], associated with the source sets
     * ([Variant.variant][com.autonomousapps.model.source.SourceKind]) they're related to.
     *
     * These are _direct_ dependencies that are not _declared_ because they're coming from associated classpaths. For
     * example, the `test` source set extends from the `main` source set (and also the compile and runtime classpaths).
     */
    private fun computeDirectDependenciesMap(
      dependencyGraph: Map<String, DependencyGraphView>,
    ): SetMultimap<String, SourceKind> {
      return newSetMultimap<String, SourceKind>().apply {
        dependencyGraph.values.map { graphView ->
          val root = graphView.graph.root()
          graphView.graph.children(root).forEach { directDependency ->
            // An attempt to normalize the identifier
            val identifier = if (directDependency is IncludedBuildCoordinates) {
              directDependency.resolvedProject.identifier
            } else {
              directDependency.identifier
            }

            put(identifier, graphView.sourceKind)
          }
        }
      }
    }

    /**
     * This results in a map like:
     * * "group:name:1.0" -> (compileClasspath, runtimeClasspath)
     * * ":project" -> (compileClasspath)
     *
     * etc.
     */
    private fun computeDependenciesToClasspathsMap(
      dependencyGraph: Map<String, DependencyGraphView>,
    ): SetMultimap<String, String> {
      return newSetMultimap<String, String>().apply {
        dependencyGraph.values.map { graphView ->
          graphView.graph.nodes().forEach { node ->
            // coordinate with `StandardTransform`
            // An attempt to normalize the identifier
            val identifier = if (node is IncludedBuildCoordinates) {
              node.resolvedProject.identifier
            } else {
              node.identifier
            }

            put(identifier, graphView.configurationName)
          }
        }
      }
    }
  }
}

internal class PluginAdviceBuilder(
  isKaptApplied: Boolean,
  redundantPlugins: Set<PluginAdvice>,
  annotationProcessorUsages: Map<Coordinates, Set<Usage>>,
) {

  private val pluginAdvice = mutableSetOf<PluginAdvice>()

  fun getPluginAdvice(): Set<PluginAdvice> = pluginAdvice

  init {
    pluginAdvice.addAll(redundantPlugins)

    if (isKaptApplied) {
      val usedProcs = annotationProcessorUsages.asSequence()
        .filter { (_, usages) -> usages.any { it.bucket == Bucket.ANNOTATION_PROCESSOR } }
        .map { it.key }
        .toSet()

      // kapt is unused
      if (usedProcs.isEmpty()) {
        pluginAdvice.add(PluginAdvice.redundantKapt())
      }
    }
  }
}

internal class DependencyAdviceBuilder(
  projectPath: String,
  private val buildPath: String,
  private val bundles: Bundles,
  private val dependencyUsages: Map<Coordinates, Set<Usage>>,
  private val annotationProcessorUsages: Map<Coordinates, Set<Usage>>,
  private val declarations: Set<Declaration>,
  private val directDependencies: SetMultimap<String, SourceKind>,
  private val dependenciesToClasspaths: SetMultimap<String, String>,
  private val supportedSourceSets: Set<String>,
  private val explicitSourceSets: Set<String>,
  private val isAndroidProject: Boolean,
  private val isKaptApplied: Boolean,
) {

  /** The unfiltered advice. */
  val advice: Set<Advice>

  /** Dependencies that are removed from [advice] because they match a bundle rule. Used by **Reason**. */
  val bundledTraces = mutableSetOf<BundleTrace>()

  init {
    advice = computeDependencyAdvice(projectPath)
      .plus(computeAnnotationProcessorAdvice())
      .toSortedSet()
  }

  private fun computeDependencyAdvice(projectPath: String): Sequence<Advice> {
    val declarations = declarations.filterToSet { Configurations.isForRegularDependency(it.configurationName) }

    fun Advice.isRemoveTestDependencyOnSelf(): Boolean {
      return coordinates.identifier == projectPath
        // https://github.com/gradle/gradle/blob/d9303339298e6206182fd1f5c7e51f11e4bdff30/subprojects/plugins/src/main/java/org/gradle/api/plugins/JavaTestFixturesPlugin.java#L68
        && (fromConfiguration?.equals("testFixturesApi") == true
        // https://github.com/gradle/gradle/blob/d9303339298e6206182fd1f5c7e51f11e4bdff30/subprojects/plugins/src/main/java/org/gradle/api/plugins/JavaTestFixturesPlugin.java#L70
        || fromConfiguration?.lowercase()?.endsWith("testimplementation") == true)
    }

    fun Advice.isAddTestDependencyOnSelf(): Boolean {
      return coordinates.identifier == projectPath
        && (fromConfiguration == null && toConfiguration?.equals("testImplementation") == true)
    }

    return dependencyUsages.asSequence()
      .flatMap { (coordinates, usages) ->
        StandardTransform(
          coordinates = coordinates,
          declarations = declarations,
          directDependencies = directDependencies,
          dependenciesToClasspaths = dependenciesToClasspaths,
          supportedSourceSets = supportedSourceSets,
          buildPath = buildPath,
          explicitSourceSets = explicitSourceSets,
          isAndroidProject = isAndroidProject,
        )
          .reduce(usages)
          .map { advice -> advice to coordinates }
      }
      // "null" removes the advice
      .mapNotNull { (advice, originalCoordinates) ->
        // Make sure to do all operations here based on the originalCoordinates used in the graph.
        // The 'advice.coordinates' may be reduced - e.g. contain less capabilities in the GradleVariantIdentifier.
        when {
          // The user cannot change these
          advice.isRemoveTestDependencyOnSelf() -> null

          // The user should not have to add a test dependency on self
          advice.isAddTestDependencyOnSelf() -> null

          advice.isAdd() && bundles.hasParentInBundle(originalCoordinates) -> {
            val parent = bundles.findParentInBundle(originalCoordinates)!!
            bundledTraces += BundleTrace.DeclaredParent(parent = parent, child = originalCoordinates)
            null
          }

          // Optionally map given advice to "primary" advice, if bundle has a primary
          advice.isAdd() -> {
            val p = bundles.maybePrimary(advice, originalCoordinates)
            if (p != advice) {
              bundledTraces += BundleTrace.PrimaryMap(primary = p.coordinates, subordinate = originalCoordinates)
            }
            p
          }

          advice.isRemove() && bundles.hasUsedChild(originalCoordinates) -> {
            val child = bundles.findUsedChild(originalCoordinates)!!
            bundledTraces += BundleTrace.UsedChild(parent = originalCoordinates, child = child)
            null
          }

          // If the advice has a used child, don't change it
          advice.isAnyChange() && bundles.hasUsedChild(originalCoordinates) -> {
            val child = bundles.findUsedChild(originalCoordinates)!!
            bundledTraces += BundleTrace.UsedChild(parent = originalCoordinates, child = child)
            null
          }

          else -> advice
        }
      }
  }

  // nb: no bundle support for annotation processors
  private fun computeAnnotationProcessorAdvice(): Sequence<Advice> {
    val declarations = declarations.filterToSet { Configurations.isForAnnotationProcessor(it.configurationName) }
    return annotationProcessorUsages.asSequence()
      .flatMap { (coordinates, usages) ->
        StandardTransform(
          coordinates = coordinates,
          declarations = declarations,
          directDependencies = emptySetMultimap(),
          dependenciesToClasspaths = emptySetMultimap(),
          supportedSourceSets = supportedSourceSets,
          buildPath = buildPath,
          explicitSourceSets = explicitSourceSets,
          isAndroidProject = isAndroidProject,
          isKaptApplied = isKaptApplied,
        ).reduce(usages)
      }
  }
}

/**
 * Equivalent to
 * ```
 * someBoolean.also { b ->
 *   if (b) block()
 * }
 * ```
 */
internal inline fun Boolean.andIfTrue(block: () -> Unit): Boolean {
  if (this) {
    block()
  }
  return this
}
