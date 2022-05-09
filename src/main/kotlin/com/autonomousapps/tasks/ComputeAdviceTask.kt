package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP_INTERNAL
import com.autonomousapps.advice.PluginAdvice
import com.autonomousapps.extension.DependenciesHandler
import com.autonomousapps.internal.Bundles
import com.autonomousapps.internal.utils.*
import com.autonomousapps.model.*
import com.autonomousapps.model.declaration.Bucket
import com.autonomousapps.model.declaration.Configurations
import com.autonomousapps.model.declaration.Declaration
import com.autonomousapps.model.intermediates.DependencyTraceReport
import com.autonomousapps.model.intermediates.Usage
import com.autonomousapps.model.intermediates.UsageBuilder
import com.autonomousapps.transform.StandardTransform
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
 * Takes [usage][com.autonomousapps.model.intermediates.Usage] information from [ComputeUsagesTask] and emits the set of
 * transforms a user should perform to have correct and simple dependency declarations. I.e., produces the advice.
 */
@CacheableTask
abstract class ComputeAdviceTask @Inject constructor(
  private val workerExecutor: WorkerExecutor
) : DefaultTask() {

  init {
    group = TASK_GROUP_DEP_INTERNAL
    description = "Merges dependency usage reports from variant-specific computations"
  }

  @get:Input
  abstract val projectPath: Property<String>

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  abstract val dependencyUsageReports: ListProperty<RegularFile>

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  abstract val dependencyGraphViews: ListProperty<RegularFile>

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
  abstract val kapt: Property<Boolean>

  @get:Optional
  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val redundantJvmPluginReport: RegularFileProperty

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
      dependencyUsageReports.set(this@ComputeAdviceTask.dependencyUsageReports)
      dependencyGraphViews.set(this@ComputeAdviceTask.dependencyGraphViews)
      declarations.set(this@ComputeAdviceTask.declarations)
      bundles.set(this@ComputeAdviceTask.bundles)
      supportedSourceSets.set(this@ComputeAdviceTask.supportedSourceSets)
      ignoreKtx.set(this@ComputeAdviceTask.ignoreKtx)
      kapt.set(this@ComputeAdviceTask.kapt)
      redundantPluginReport.set(this@ComputeAdviceTask.redundantJvmPluginReport)
      output.set(this@ComputeAdviceTask.output)
      dependencyUsages.set(this@ComputeAdviceTask.dependencyUsages)
      annotationProcessorUsages.set(this@ComputeAdviceTask.annotationProcessorUsages)
      bundledTraces.set(this@ComputeAdviceTask.bundledTraces)
    }
  }

  interface ComputeAdviceParameters : WorkParameters {
    val projectPath: Property<String>
    val dependencyUsageReports: ListProperty<RegularFile>
    val dependencyGraphViews: ListProperty<RegularFile>
    val declarations: RegularFileProperty
    val bundles: Property<DependenciesHandler.SerializableBundles>
    val supportedSourceSets: SetProperty<String>
    val ignoreKtx: Property<Boolean>
    val kapt: Property<Boolean>
    val redundantPluginReport: RegularFileProperty
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
      val projectNode = ProjectCoordinates(projectPath)
      val declarations = parameters.declarations.fromJsonSet<Declaration>()
      val dependencyGraph = parameters.dependencyGraphViews.get()
        .map { it.fromJson<DependencyGraphView>() }
        .associateBy { it.name }
      val bundleRules = parameters.bundles.get()
      val reports = parameters.dependencyUsageReports.get().mapToSet { it.fromJson<DependencyTraceReport>() }
      val usageBuilder = UsageBuilder(
        reports = reports,
        // TODO V2: it would be clearer to get this from a SyntheticProject
        variants = dependencyGraph.values.map { it.variant }
      )
      val dependencyUsages = usageBuilder.dependencyUsages
      val annotationProcessorUsages = usageBuilder.annotationProcessingUsages
      val supportedSourceSets = parameters.supportedSourceSets.get()
      val isKaptApplied = parameters.kapt.get()

      val bundles = Bundles.of(
        projectNode = projectNode,
        dependencyGraph = dependencyGraph,
        bundleRules = bundleRules,
        dependencyUsages = dependencyUsages,
        ignoreKtx = parameters.ignoreKtx.get()
      )

      val dependencyAdviceBuilder = DependencyAdviceBuilder(
        bundles = bundles,
        dependencyUsages = dependencyUsages,
        annotationProcessorUsages = annotationProcessorUsages,
        declarations = declarations,
        supportedSourceSets = supportedSourceSets,
        isKaptApplied = isKaptApplied
      )

      val pluginAdviceBuilder = PluginAdviceBuilder(
        isKaptApplied = isKaptApplied,
        redundantPlugins = parameters.redundantPluginReport.fromNullableJsonSet<PluginAdvice>().orEmpty(),
        annotationProcessorUsages = annotationProcessorUsages
      )

      val projectAdvice = ProjectAdvice(
        projectPath = projectPath,
        dependencyAdvice = dependencyAdviceBuilder.advice,
        pluginAdvice = pluginAdviceBuilder.getPluginAdvice()
      )

      output.writeText(projectAdvice.toJson())
      // These must be transformed so that the Coordinates are Strings for serialization
      dependencyUsagesOut.writeText(dependencyUsages.toSimplifiedJson())
      annotationProcessorUsagesOut.writeText(annotationProcessorUsages.toSimplifiedJson())
      bundleTraces.writeText(dependencyAdviceBuilder.bundledTraces.toJson())
    }

    private fun Map<Coordinates, Set<Usage>>.toSimplifiedJson(): String = map { (key, value) ->
      key.gav() to value
    }.toMap().toJson()
  }
}

internal class PluginAdviceBuilder(
  isKaptApplied: Boolean,
  redundantPlugins: Set<PluginAdvice>,
  annotationProcessorUsages: Map<Coordinates, Set<Usage>>
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
  private val bundles: Bundles,
  private val dependencyUsages: Map<Coordinates, Set<Usage>>,
  private val annotationProcessorUsages: Map<Coordinates, Set<Usage>>,
  private val declarations: Set<Declaration>,
  private val supportedSourceSets: Set<String>,
  private val isKaptApplied: Boolean
) {

  /** The unfiltered advice. */
  val advice: Set<Advice>

  /** Dependencies that are removed from [advice] because they match a bundle rule. Used by **Reason**. */
  val bundledTraces = mutableSetOf<String>()

  init {
    advice = computeDependencyAdvice()
      .plus(computeAnnotationProcessorAdvice())
      .toSortedSet()
  }

  private fun computeDependencyAdvice(): Sequence<Advice> {
    val declarations = declarations.filterToSet { Configurations.isForRegularDependency(it.configurationName) }
    return dependencyUsages.asSequence()
      .flatMap { (coordinates, usages) ->
        StandardTransform(coordinates, declarations, supportedSourceSets).reduce(usages)
      }
      // "null" removes the advice
      .mapNotNull { advice ->
        when {
          advice.isAdd() && bundles.hasParentInBundle(advice.coordinates) -> {
            bundledTraces += advice.coordinates.gav()
            null
          }
          // Optionally map given advice to "primary" advice, if bundle has a primary
          advice.isAdd() -> bundles.primary(advice)
          advice.isRemove() && bundles.hasUsedChild(advice.coordinates) -> {
            bundledTraces += advice.coordinates.gav()
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
        StandardTransform(coordinates, declarations, supportedSourceSets, isKaptApplied).reduce(usages)
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
